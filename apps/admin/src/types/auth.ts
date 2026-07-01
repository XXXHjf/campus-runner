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

// 后端 UserVO（摘取审核管理所需字段）
export interface PendingAuthUser {
  id: number
  username: string
  realname: string
  openid?: string
  headImg?: string
  sex?: number
  phone?: string
  authentication?: number
  schoolId?: number
  schoolName?: string
  stuId?: string
  studentIdCard?: string
  studentIdCardReview?: AuthReviewStatus
  score?: number
  money?: number
  alipayPaymentCode?: string
  weChatPaymentCode?: string
  isManager?: number
  deleted?: number
  createTime?: string
  updateTime?: string
}

// 审核请求
export interface ReviewAuthRequest {
  userID: number
  review: AuthReviewStatus
}

