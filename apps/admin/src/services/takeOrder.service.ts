/**
 * Take order management service
 * Corresponds to backend AdminTakeOrderController
 */

import { get } from './request'
import type {
  PageResponse,
  AdminTakeOrderItem,
  AdminTakeOrderStatistics,
} from '../types/admin'

export async function listAll(
  page: number,
  pageSize: number,
  keyword = '',
): Promise<PageResponse<AdminTakeOrderItem>> {
  return get<PageResponse<AdminTakeOrderItem>>('/admin/api/take-orders/all', { page, pageSize, keyword })
}

export async function listUnpaid(
  page: number,
  pageSize: number,
  keyword = '',
): Promise<PageResponse<AdminTakeOrderItem>> {
  return get<PageResponse<AdminTakeOrderItem>>('/admin/api/take-orders/unpaid', { page, pageSize, keyword })
}

export async function statistics(): Promise<AdminTakeOrderStatistics> {
  return get<AdminTakeOrderStatistics>('/admin/api/take-orders/statistics')
}
