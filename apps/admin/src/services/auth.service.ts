/**
 * 审核管理服务
 * 封装学生认证审核相关的 API 调用
 */

import { get, put } from './request'
import { AUTH_REVIEW_CHANGED } from '../hooks/usePendingAuthCount'
import type { PendingAuthUser, ReviewAuthRequest } from '../types'

/**
 * 获取待审核列表
 */
export async function getPendingList(): Promise<PendingAuthUser[]> {
  return get<PendingAuthUser[]>('/admin/api/auth/pendingList')
}

/**
 * 更新学生认证审核状态
 * 0未审核 1审核中 2审核通过 3审核不通过
 */
export async function reviewAuth(data: ReviewAuthRequest): Promise<Record<string, never>> {
  const result = await put<Record<string, never>>('/admin/api/auth/review', data)
  window.dispatchEvent(new Event(AUTH_REVIEW_CHANGED))
  return result
}

