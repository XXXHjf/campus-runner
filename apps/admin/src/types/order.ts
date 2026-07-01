/**
 * 订单相关类型定义
 */

// 订单状态枚举
export enum OrderStatus {
  PENDING = 0, // 待接单
  ACCEPTED = 1, // 已接单
  IN_PROGRESS = 2, // 进行中
  COMPLETED = 3, // 已完成
  CANCELLED = 4, // 已取消
}

// 价格模式
export enum PriceMode {
  FREE = 0, // 无偿
  PAID = 1, // 有偿
}

// 门禁状态
export enum DoorAccess {
  NO_GUARD = 0, // 无门禁
  HAS_GUARD = 1, // 有门禁
}

// 订单信息
export interface Order {
  id: string
  userId: string
  categoryId: string
  categoryName?: string
  pickupAddressId: string
  pickupAddressName?: string
  receiveAddressId: string
  receiveAddressName?: string
  contactName: string
  contactPhone: string
  note: string
  imageUrls?: string[]
  priceMode: PriceMode
  price: number
  doorAccess: DoorAccess
  gapMinutes: number
  autoCancelHours: number
  status: OrderStatus
  cancelReason?: string
  createdAt: string
  updatedAt: string
  expectTime?: string
  // 发布者信息
  publisher?: {
    username: string
    headImg: string
    phone: string
  }
  // 接单者信息
  taker?: {
    id: string
    username: string
    headImg: string
    phone: string
  }
}

// 创建订单请求
export interface CreateOrderRequest {
  categoryId: string
  pickupAddressId: string
  receiveAddressId: string
  contactName: string
  contactPhone: string
  note: string
  imageUrls?: string[]
  priceMode: PriceMode
  price: number
  doorAccess: DoorAccess
  gapMinutes: number
  autoCancelHours: number
}

// 更新订单请求
export interface UpdateOrderRequest extends Partial<CreateOrderRequest> {
  id: string
}

// 取消订单请求
export interface CancelOrderRequest {
  id: string
  cancelReason: string
}

// 订单查询参数
export interface OrderQueryParams {
  status?: OrderStatus
  categoryId?: string
  keyword?: string
  startDate?: string
  endDate?: string
  page?: number
  pageSize?: number
}

// 订单排序类型
export enum OrderSortType {
  TIME = 'time', // 时间排序
  PRICE_HIGH = 'price_high', // 价格从高到低
  PRICE_LOW = 'price_low', // 价格从低到高
}

