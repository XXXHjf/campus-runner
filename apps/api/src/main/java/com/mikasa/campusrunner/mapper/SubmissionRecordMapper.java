package com.mikasa.campusrunner.mapper;

import com.mikasa.campusrunner.pojo.entity.SubmissionRecord;
import org.apache.ibatis.annotations.*;

@Mapper
public interface SubmissionRecordMapper {
    @Insert("""
        INSERT INTO tb_submission_record(owner_id, operation, request_key, request_hash, claim_token)
        VALUES(#{ownerId}, #{operation}, #{requestKey}, #{requestHash}, #{claimToken})
        ON DUPLICATE KEY UPDATE request_key = request_key
        """)
    void claim(SubmissionRecord record);

    @Select("""
        SELECT owner_id, operation, request_key, request_hash, claim_token, result_json
        FROM tb_submission_record
        WHERE owner_id = #{ownerId} AND operation = #{operation} AND request_key = #{requestKey}
        FOR UPDATE
        """)
    SubmissionRecord lock(SubmissionRecord record);

    @Update("""
        UPDATE tb_submission_record SET result_json = #{resultJson}
        WHERE owner_id = #{ownerId} AND operation = #{operation} AND request_key = #{requestKey}
          AND claim_token = #{claimToken} AND request_hash = #{requestHash} AND result_json IS NULL
        """)
    int complete(SubmissionRecord record);
}
