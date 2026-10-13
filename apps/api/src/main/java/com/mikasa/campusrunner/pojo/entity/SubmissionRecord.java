package com.mikasa.campusrunner.pojo.entity;

import lombok.Data;

@Data
public class SubmissionRecord {
    private Long ownerId;
    private String operation;
    private String requestKey;
    private String requestHash;
    private String claimToken;
    private String resultJson;
}
