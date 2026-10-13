package com.mikasa.campusrunner.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.common.exception.ParamException;
import com.mikasa.campusrunner.common.exception.SubmissionPendingException;
import com.mikasa.campusrunner.common.exception.UserException;
import com.mikasa.campusrunner.mapper.SubmissionRecordMapper;
import com.mikasa.campusrunner.mapper.UserMapper;
import com.mikasa.campusrunner.pojo.entity.SubmissionRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class SubmissionService {
    public enum Operation { RUNNER_ORDER, SECOND_HAND_PRODUCT, SECOND_HAND_ORDER }
    private final SubmissionRecordMapper mapper;
    private final UserMapper userMapper;
    private final ObjectMapper json;

    @Transactional(rollbackFor = Exception.class)
    public <T> T execute(Operation operation, String key, Object payload, Class<T> resultType, Supplier<T> create) {
        Long ownerId = BaseContext.getCurrentId();
        if (ownerId == null || ownerId <= 0) throw new UserException("请先登录后再操作");
        var user = userMapper.getById(ownerId);
        if (user == null || Integer.valueOf(1).equals(user.getDeleted())) throw new UserException("请重新登录后再操作");
        if (!Integer.valueOf(1).equals(user.getAuthentication())) throw new UserException("完成校园认证后才能操作");
        if (key == null || !key.matches("[A-Za-z0-9_-]{16,64}")) throw new ParamException("请更新版本后再提交");
        var claim = new SubmissionRecord();
        claim.setOwnerId(ownerId);
        claim.setOperation(operation.name());
        claim.setRequestKey(key);
        claim.setRequestHash(hash(payload));
        claim.setClaimToken(UUID.randomUUID().toString().replace("-", ""));
        mapper.claim(claim);
        var existing = mapper.lock(claim);
        if (existing == null) throw new SubmissionPendingException();
        if (!claim.getRequestHash().equals(existing.getRequestHash())) {
            throw new SubmissionPendingException("提交内容已变化，请先确认上次提交结果");
        }
        if (existing.getResultJson() != null) {
            try {
                T saved = json.readValue(existing.getResultJson(), resultType);
                if (saved == null) throw new SubmissionPendingException();
                return saved;
            }
            catch (JsonProcessingException error) { throw new SubmissionPendingException(); }
        }
        if (!claim.getClaimToken().equals(existing.getClaimToken())) throw new SubmissionPendingException();
        T result = create.get();
        if (result == null) throw new SubmissionPendingException();
        try { claim.setResultJson(json.writeValueAsString(result)); }
        catch (JsonProcessingException error) { throw new ParamException("提交未完成，请稍后重试"); }
        if (mapper.complete(claim) != 1) throw new SubmissionPendingException();
        return result;
    }

    private String hash(Object payload) {
        JsonNode tree = json.valueToTree(payload);
        if (tree instanceof ObjectNode object) object.remove("clientRequestId");
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsBytes(sorted(tree))));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        } catch (JsonProcessingException error) {
            throw new ParamException("提交内容有误，请重新填写");
        }
    }

    private JsonNode sorted(JsonNode node) {
        if (node.isObject()) {
            ObjectNode result = json.createObjectNode();
            var keys = new TreeSet<String>(); node.fieldNames().forEachRemaining(keys::add);
            keys.forEach(key -> result.set(key, sorted(node.get(key))));
            return result;
        }
        if (node.isArray()) {
            var result = json.createArrayNode(); node.forEach(value -> result.add(sorted(value))); return result;
        }
        return node;
    }
}
