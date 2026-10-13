package com.mikasa.campusrunner.mapper;

import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface RequestGuardMapper {
    @Select("select unix_timestamp()") long nowSeconds();
    // A no-op duplicate-key UPDATE takes an exclusive lock. INSERT IGNORE takes a shared
    // duplicate lock and concurrent lock upgrades can deadlock on the same counter.
    @Insert("insert into tb_request_guard(guard_key, window_start, used_count, used_bytes, tokens, touched_at) values(#{key},#{window},0,0,#{capacity},#{now}) on duplicate key update guard_key=values(guard_key)")
    void ensure(@Param("key") String key, @Param("window") long window, @Param("capacity") int capacity, @Param("now") long now);
    @Select("select used_count from tb_request_guard where guard_key=#{key} and window_start=#{window} for update")
    long lock(@Param("key") String key, @Param("window") long window);
    @Update("update tb_request_guard set used_count=used_count+1, used_bytes=used_bytes+#{bytes}, touched_at=#{now} where guard_key=#{key} and window_start=#{window} and used_count < #{count} and used_bytes <= #{budget}-#{bytes}")
    int spend(@Param("key") String key, @Param("window") long window, @Param("count") int count,
              @Param("bytes") long bytes, @Param("budget") long budget, @Param("now") long now);
    @Update("update tb_request_guard set used_bytes=used_bytes-#{unused} where guard_key=#{key} and window_start=#{window} and used_bytes>=#{unused}")
    int settleInput(@Param("key") String key, @Param("window") long window, @Param("unused") long unused);
    @Update("update tb_request_guard set tokens=least(#{capacity},tokens+greatest(0,#{now}-touched_at)*#{rate})-1, touched_at=#{now} where guard_key=#{key} and window_start=-2 and least(#{capacity},tokens+greatest(0,#{now}-touched_at)*#{rate}) >= 1")
    int takeToken(@Param("key") String key, @Param("capacity") int capacity, @Param("rate") int rate, @Param("now") long now);
    @Select("select count(*) from tb_upload_lease where guard_key=#{key} and expires_at>#{now}")
    int activeLeases(@Param("key") String key, @Param("now") long now);
    @Insert("insert into tb_upload_lease(lease_id,guard_key,expires_at) values(#{id},#{key},#{expiry})")
    void insertLease(@Param("id") String id, @Param("key") String key, @Param("expiry") long expiry);
    @Delete("delete from tb_upload_lease where lease_id=#{id}") void releaseLease(String id);
    @Update("update tb_upload_lease set expires_at=#{expiry} where lease_id=#{id} and expires_at>#{now}")
    int renewLease(@Param("id") String id, @Param("expiry") long expiry, @Param("now") long now);
    @Select("select count(*) from tb_upload_lease where lease_id=#{id} and expires_at>unix_timestamp()")
    int activeLease(String id);
    @Update("update tb_media_asset set delete_after=from_unixtime(#{expiry}) where id=#{id} and status='UPLOADING' and delete_after>now()")
    int renewUploading(@Param("id") long id, @Param("expiry") long expiry);
    @Delete("delete from tb_upload_lease where expires_at < #{now} limit 200") int cleanLeases(long now);
    @Delete("delete from tb_request_guard where (window_start >= 0 or window_start=-2) and touched_at < #{cutoff} limit 200") int cleanWindows(long cutoff);
    @Select("<script>select count(*) as resource_count, coalesce(sum(file_size),0) as resource_bytes from tb_media_asset where owner_type=#{ownerType} and owner_id=#{ownerId} and status in ('UPLOADING','TEMP','FAILED','PENDING_DELETE') and purpose in <foreach collection='purposes' item='purpose' open='(' separator=',' close=')'>#{purpose}</foreach></script>")
    java.util.Map<String,Object> unsettled(@Param("ownerType") String ownerType, @Param("ownerId") long ownerId, @Param("purposes") List<String> purposes);
}
