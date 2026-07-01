/**
 * 通用类型定义
 */

// API 响应基础结构
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

// 分页请求参数
export interface PaginationParams {
  page: number
  pageSize: number
}

// 分页响应数据
export interface PaginationResponse<T> {
  list: T[]
  total: number
  page: number
  pageSize: number
}

// 排序方向
export enum SortOrder {
  ASC = 'asc',
  DESC = 'desc',
}

// 请求状态
export enum RequestStatus {
  IDLE = 'idle',
  LOADING = 'loading',
  SUCCESS = 'success',
  ERROR = 'error',
}

// 错误类型
export interface ApiError {
  code: number
  message: string
  errors?: Record<string, string[]>
}

