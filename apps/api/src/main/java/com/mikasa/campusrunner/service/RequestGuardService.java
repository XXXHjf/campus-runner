package com.mikasa.campusrunner.service;

import com.mikasa.campusrunner.common.exception.RequestLimitException;
import com.mikasa.campusrunner.common.properties.RequestGuardProperties;
import com.mikasa.campusrunner.mapper.RequestGuardMapper;
import lombok.Getter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class RequestGuardService {
    @Getter private final RequestGuardProperties properties;
    private final RequestGuardMapper mapper;
    private final TransactionTemplate tx;
    // Local registry only renews live work; admission always reads shared MySQL leases.
    private final java.util.concurrent.ConcurrentMap<String,Long> liveUploads = new java.util.concurrent.ConcurrentHashMap<>();
    private final ThreadLocal<String> requestLease = new ThreadLocal<>();
    private record InputReservation(String actor, String purpose, String key, long window, long bytes) {}
    private final ThreadLocal<InputReservation> requestInput = new ThreadLocal<>();
    public RequestGuardService(RequestGuardProperties properties, RequestGuardMapper mapper, PlatformTransactionManager manager) {
        this.properties=properties; this.mapper=mapper; this.tx=new TransactionTemplate(manager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        tx.setTimeout(5);
    }
    public <T> T atomic(Supplier<T> action) {
        try { return tx.execute(status -> action.get()); }
        catch (org.springframework.dao.DataAccessException | org.springframework.transaction.TransactionException error) {
            throw new com.mikasa.campusrunner.common.exception.RequestGuardUnavailableException();
        }
    }
    public void check(String key, int seconds, int limit) {
        if (!properties.isEnabled()) return;
        atomic(() -> { spend(key,seconds,limit,0,Long.MAX_VALUE);return null; });
    }
    // Caller must be inside atomic(); store clock and conditional updates are shared across instances.
    public void spend(String key, int seconds, int limit, long bytes, long budget) {
        if (seconds<1 || limit<1 || bytes<0 || budget<0) throw new IllegalStateException("Invalid guard configuration");
        long now=mapper.nowSeconds(), window=seconds==86400 ? (now+28800)/86400*86400-28800 : now/seconds*seconds;
        mapper.ensure(key,window,0,now);
        if (mapper.spend(key,window,limit,bytes,budget,now)!=1) throw new RequestLimitException((int)(window+seconds-now));
    }
    public void reserveInput(String actor, com.mikasa.campusrunner.common.constant.MediaPurpose purpose, long contentLength) {
        var p=properties;
        boolean admin=actor.startsWith("ADMIN:"), evidence=purpose==com.mikasa.campusrunner.common.constant.MediaPurpose.DELIVERY_PROOF
                || purpose==com.mikasa.campusrunner.common.constant.MediaPurpose.PURCHASE_PROOF;
        String group=admin?"ADMIN":evidence?"EVIDENCE":"CONTENT";
        int count=admin?p.getAdminDailyUploads():evidence?p.getEvidenceDailyUploads():p.getDailyUploads();
        long budget=admin?p.getAdminDailyInputBytes():evidence?p.getEvidenceDailyInputBytes():p.getDailyInputBytes();
        // Content-Length is an upper bound on a file only when valid; chunked requests reserve the file maximum.
        long bytes=contentLength>0?Math.min(contentLength,purpose.getMaxInputBytes()):purpose.getMaxInputBytes();
        InputReservation reserved=atomic(() -> {
            long now=mapper.nowSeconds(), window=(now+28800)/86400*86400-28800;
            String key=actor+":DAILY:"+group;mapper.ensure(key,window,0,now);
            if(mapper.spend(key,window,count,bytes,budget,now)!=1) throw new RequestLimitException((int)(window+86400-now));
            return new InputReservation(actor,purpose.name(),key,window,bytes);
        });
        requestInput.set(reserved);
    }
    /** Settle once in the admission day, including requests that cross midnight. Counts are never refunded. */
    public boolean settleInput(String actor, String purpose, long bytes) {
        InputReservation reserved=requestInput.get();
        if(reserved==null) return false;
        if(!reserved.actor().equals(actor) || !reserved.purpose().equals(purpose) || bytes<0 || bytes>reserved.bytes()) {
            throw new com.mikasa.campusrunner.common.exception.UploadException("图片上传失败，请重试");
        }
        requestInput.remove();
        long unused=reserved.bytes()-bytes;
        if(unused>0) atomic(() -> { if(mapper.settleInput(reserved.key(),reserved.window(),unused)!=1)
            throw new com.mikasa.campusrunner.common.exception.RequestGuardUnavailableException();return null; });
        return true;
    }
    public void clearInputReservation() { requestInput.remove(); }
    public void checkIp(String key) {
        if (!properties.isEnabled()) return;
        atomic(() -> {
            long now=mapper.nowSeconds();mapper.ensure(key,-2,properties.getIpBurst(),now);
            if (mapper.takeToken(key,properties.getIpBurst(),properties.getIpPerSecond(),now)!=1) throw new RequestLimitException(1);
            return null;
        });
    }
    public String acquireUpload(String owner) {
        if (!properties.isEnabled()) return null;
        String lease = atomic(() -> {
            long now=mapper.nowSeconds();mapper.ensure(owner+":LEASE",-1,0,now);mapper.lock(owner+":LEASE",-1);
            if (mapper.activeLeases(owner,now)>=properties.getUploadConcurrency()) throw new RequestLimitException(5);
            String id=UUID.randomUUID().toString();mapper.insertLease(id,owner,now+3600);return id;
        });
        liveUploads.put(lease, 0L); requestLease.set(lease); return lease;
    }
    public void releaseUpload(String id) {
        requestLease.remove();
        if(id!=null) {liveUploads.remove(id);atomic(() -> {mapper.releaseLease(id);return null;});}
    }
    public void trackUpload(long mediaId) {
        String id=requestLease.get();if(id!=null) liveUploads.replace(id,mediaId);
    }
    public void checkUploadLease() {
        String id=requestLease.get();
        if(id!=null && atomic(() -> mapper.activeLease(id))!=1) throw new RequestLimitException(5);
    }
    @org.springframework.scheduling.annotation.Scheduled(fixedDelay=30000)
    public void renewLiveUploads() {
        for(var entry:liveUploads.entrySet()) {
            try {
                atomic(() -> {
                    long now=mapper.nowSeconds();
                    if(mapper.renewLease(entry.getKey(),now+3600,now)==1 && entry.getValue()>0) mapper.renewUploading(entry.getValue(),now+3600);
                    return null;
                });
            } catch(com.mikasa.campusrunner.common.exception.RequestGuardUnavailableException error) {
                // Admission fails closed; a long outage expires the shared lease instead of creating an unbounded local allowance.
                break;
            }
        }
    }
    public void lockOwner(String owner) {
        long now=mapper.nowSeconds();mapper.ensure(owner+":MEDIA",-1,0,now);mapper.lock(owner+":MEDIA",-1);
    }
    public java.util.Map<String,Object> unsettled(String type,long id,java.util.List<String> purposes) {
        return mapper.unsettled(type,id,purposes);
    }
    @org.springframework.scheduling.annotation.Scheduled(fixedDelay=60000)
    public void cleanup() {
        if (!properties.isEnabled()) return;
        atomic(() -> { long now=mapper.nowSeconds();mapper.cleanLeases(now);mapper.cleanWindows(now-172800);return null; });
    }
}
