package com.mikasa.campusrunner.pojo.vo;
/** Counts are independent from paginated lists; IDs are only supplied for a sole task. */
public record UserTaskSummaryVO(Long runnerReceivableCount, Long runnerReceivableOrderId,
        Long runnerUnpaidCount, Long runnerUnpaidOrderId, Long secondHandBuyerCount,
        Long secondHandBuyerNoticeCount, Long secondHandSellerCount, Long bargainPendingCount, Long bargainAcceptedCount) {
    public static UserTaskSummaryVO empty() { return new UserTaskSummaryVO(0L,null,0L,null,0L,0L,0L,0L,0L); }
}
