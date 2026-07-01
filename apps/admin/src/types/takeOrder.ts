/**
 * 接单相关类型定义
 */

// 接单状态
export enum TakeOrderStatus {
  ACCEPTED = 0, // 已接单
  PICKED_UP = 1, // 已取件
  DELIVERED = 2, // 已送达
  CONFIRMED = 3, // 已确认（完成）
  CANCELLED = 4, // 已取消
}

// 接单信息
export interface TakeOrder {
  id: string
  orderId: string
  userId: string
  status: TakeOrderStatus
  deliveryImageUrl?: string
  createdAt: string
  updatedAt: string
  // 关联的订单信息
  order?: {
    id: string
    categoryName: string
    pickupAddressName: string
    receiveAddressName: string
    contactName: string
    contactPhone: string
    note: string
    price: number
    priceMode: number
  }
  // 接单者信息
  taker?: {
    username: string
    headImg: string
    phone: string
  }
  // 发布者信息
  publisher?: {
    username: string
    headImg: string
    phone: string
  }
}

// 接单请求
export interface AcceptOrderRequest {
  orderId: string
}

// 更新接单状态请求
export interface UpdateTakeOrderStatusRequest {
  id: string
  status: TakeOrderStatus
  deliveryImageUrl?: string
}

// 接单查询参数
export interface TakeOrderQueryParams {
  status?: TakeOrderStatus
  userId?: string
  orderId?: string
  page?: number
  pageSize?: number
}

