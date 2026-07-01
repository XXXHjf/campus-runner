/**
 * 用户服务
 * 封装用户相关的 API 调用
 */

import { get, post } from './request'
import type { AdminLoginRequest, AdminLoginResponse } from '../types'
import type { PageResponse, AdminUserItem, AdminUserDetail, AdminUserStatistics } from '../types/admin'

/**
 * 用户登录（管理后台使用账号密码登录）
 */
export async function login(data: AdminLoginRequest): Promise<AdminLoginResponse> {
  return post<AdminLoginResponse>('/admin/api/login', data)
}

export async function listAllUsers(page: number, pageSize: number): Promise<PageResponse<AdminUserItem>> {
  return get<PageResponse<AdminUserItem>>('/admin/api/users/all', { page, pageSize })
}

export async function listAuthenticated(page: number, pageSize: number): Promise<PageResponse<AdminUserItem>> {
  return get<PageResponse<AdminUserItem>>('/admin/api/users/authenticated', { page, pageSize })
}

export async function listPendingReview(page: number, pageSize: number): Promise<PageResponse<AdminUserItem>> {
  return get<PageResponse<AdminUserItem>>('/admin/api/users/pending-review', { page, pageSize })
}

export async function userDetail(id: number): Promise<AdminUserDetail> {
  return get<AdminUserDetail>(`/admin/api/users/${id}`)
}

export async function userStatistics(): Promise<AdminUserStatistics> {
  return get<AdminUserStatistics>('/admin/api/users/statistics')
}
