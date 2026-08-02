/**
 * Admin-specific type definitions for management backend APIs
 */

// Generic paginated response matching backend PageResult<T>
export interface PageResponse<T> {
  total: number
  page: number
  pageSize: number
  list: T[]
}

// ----- Order Management -----

export interface AdminOrderItem {
  id: number
  orderNumber: string
  status: number
  categoryName: string
  price: number
  serviceFee: number
  payAmount: number
  username: string
  phone: string
  pickUpAddress: string
  reciveAddress: string
  note: string
  doorAccess: number
  createTime: string
  // in-progress / completed only
  takerName?: string
  takerPhone?: string
  takeOrderTime?: string
  deliveryTime?: string
  completeTime?: string
  withdrawalStatus?: number | null
  // canceled only
  cancelTime?: string
  cancelReason?: string
  refundAmount?: number
  refundStatus?: string
}

export interface AdminOrderDetailResponse {
  order: {
    id: number
    orderNumber: string
    status: number
    categoryName: string
    price: number
    serviceFeeRate: number
    serviceFee: number
    payAmount: number
    username: string
    phone: string
    pickUpAddress: string
    reciveAddress: string
    note: string
    image: string | null
    doorAccess: number
    gap: number
    exceedTime: string
    createTime: string
    cancelTime?: string
    cancelReason?: string
  }
  taker: {
    userId: number
    realname: string
    phone: string
    takeTime: string
    deliveryTime: string | null
    image: string | null
  } | null
  payment: {
    transactionId: string
    tradeState: string
    total: number
    serviceFee: number
    payerOpenid: string
    successTime: string
  } | null
  refund: {
    refundNumber: string
    refundId: string
    refundAmount: number
    refundStatus: string
    reason: string
    createTime: string
  } | null
}

export interface AdminOrderStatistics {
  totalCount: number
  waitingCount: number
  inProgressCount: number
  completedCount: number
  canceledCount: number
  todayNewCount: number
  todayTotalAmount: number
  todayServiceFee: number
}

// ----- User Management -----

export interface AdminUserItem {
  id: number
  username: string
  realname: string
  headImg: string
  sex: number
  phone: string
  schoolName: string
  stuId: string
  authentication: number
  studentIdCardReview: number
  score: number
  orderCount: number
  takeOrderCount: number
  createTime: string
  studentIdCard?: string
}

export interface AdminUserDetail {
  id: number
  username: string
  realname: string
  headImg: string
  sex: number
  phone: string
  schoolName: string
  stuId: string
  authentication: number
  studentIdCard: string
  studentIdCardReview: number
  score: number
  isManager: number
  orderCount: number
  takeOrderCount: number
  totalEarned: number
  createTime: string
  updateTime: string
}

export interface AdminUserStatistics {
  totalCount: number
  authenticatedCount: number
  unauthenticatedCount: number
  pendingReviewCount: number
  todayNewCount: number
  managerCount: number
}

// ----- Take Order Management -----

export interface AdminTakeOrderItem {
  id: number
  orderId: number
  orderNumber: string
  orderStatus: number
  takeOrderStatus: number
  categoryName: string
  price: number
  orderNote: string
  pickUpAddress: string
  reciveAddress: string
  publisherName: string
  publisherPhone: string
  takerName: string
  takerPhone: string
  takeOrderTime: string
  deliveryTime: string | null
  takeOrderImage: string | null
  // unpaid only
  serviceFee?: number
  payAmount?: number
  completeTime?: string
  withdrawalStatus?: number | null
}

export interface AdminTakeOrderStatistics {
  totalCount: number
  todayNewCount: number
  unpaidCount: number
  unpaidTotalAmount: number
  todayCompletedCount: number
  todayCompletedAmount: number
}

// ----- Category -----

export interface AdminCategory {
    id: number
    categoryName: string
    image?: string
    imageAssetId?: number
  }
