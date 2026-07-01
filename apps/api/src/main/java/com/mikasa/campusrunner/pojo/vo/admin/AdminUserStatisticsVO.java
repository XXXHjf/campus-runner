package com.mikasa.campusrunner.pojo.vo.admin;

import lombok.Data;

@Data
public class AdminUserStatisticsVO {
    private Integer totalCount;
    private Integer authenticatedCount;
    private Integer unauthenticatedCount;
    private Integer pendingReviewCount;
    private Integer todayNewCount;
    private Integer managerCount;
}
