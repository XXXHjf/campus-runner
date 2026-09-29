/**
 * 审核管理服务
 * 封装学生认证审核相关的 API 调用
 */

import { put } from './request'
import { AUTH_REVIEW_CHANGED } from '../hooks/usePendingAuthCount'
import type { ReviewAuthRequest } from '../types'

/**
 * 更新学生认证审核状态
 * 仅提交通过或驳回结果
 */
export async function reviewAuth(data: ReviewAuthRequest): Promise<Record<string, never>> {
  const result = await put<Record<string, never>>('/admin/api/auth/review', data)
  window.dispatchEvent(new Event(AUTH_REVIEW_CHANGED))
  return result
}

