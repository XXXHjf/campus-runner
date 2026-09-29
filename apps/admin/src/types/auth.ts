/**
 * 审核管理相关类型定义
 */

// 学生认证审核状态
// 0未审核 1审核中 2审核通过 3审核不通过
export enum AuthReviewStatus {
  UNREVIEWED = 0,
  REVIEWING = 1,
  APPROVED = 2,
  REJECTED = 3,
}

// 审核请求
export interface ReviewAuthRequest {
  userID: number
  review: AuthReviewStatus
  authReviewVersion: number
  studentIdCardRejectReason?: string
}

