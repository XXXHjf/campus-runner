/**
 * Order management service
 * Corresponds to backend AdminOrderController
 */

import { get, post } from './request'
import type {
  PageResponse,
  AdminOrderItem,
  AdminOrderDetailResponse,
  AdminOrderStatistics,
} from '../types/admin'

export async function listAll(
  page: number,
  pageSize: number,
): Promise<PageResponse<AdminOrderItem>> {
  return get<PageResponse<AdminOrderItem>>('/admin/api/orders/all', { page, pageSize })
}

export async function listWaiting(
  page: number,
  pageSize: number,
): Promise<PageResponse<AdminOrderItem>> {
  return get<PageResponse<AdminOrderItem>>('/admin/api/orders/waiting', { page, pageSize })
}

export async function listInProgress(
  page: number,
  pageSize: number,
): Promise<PageResponse<AdminOrderItem>> {
  return get<PageResponse<AdminOrderItem>>('/admin/api/orders/in-progress', { page, pageSize })
}

export async function listCompleted(
  page: number,
  pageSize: number,
): Promise<PageResponse<AdminOrderItem>> {
  return get<PageResponse<AdminOrderItem>>('/admin/api/orders/completed', { page, pageSize })
}

export async function listCanceled(
  page: number,
  pageSize: number,
): Promise<PageResponse<AdminOrderItem>> {
  return get<PageResponse<AdminOrderItem>>('/admin/api/orders/canceled', { page, pageSize })
}

export async function detail(id: number): Promise<AdminOrderDetailResponse> {
  // Backend returns a flat VO; remap to nested structure expected by OrderDetail
  const raw: Record<string, unknown> = await get(`/admin/api/orders/${id}`)

  // Helper: treat value as null if null/undefined/empty string
  const n = (v: unknown) => (v == null || v === '' ? null : v)

  // Helper: convert Integer cents from WeChat Pay to yuan (e.g. 23400 → 234.00)
  const fenToYuan = (v: unknown) => (v != null ? Number(v) / 100 : 0)

  return {
    order: {
      id: raw['id'] as number,
      orderNumber: raw['orderNumber'] as string,
      status: raw['status'] as number,
      categoryName: raw['categoryName'] as string,
      price: raw['price'] as number,
      serviceFeeRate: raw['serviceFeeRate'] as number,
      serviceFee: raw['serviceFee'] as number,
      payAmount: raw['payAmount'] as number,
      username: raw['username'] as string,
      phone: raw['phone'] as string,
      pickUpAddress: raw['pickUpAddress'] as string,
      reciveAddress: raw['reciveAddress'] as string,
      note: raw['note'] as string,
      image: n(raw['image']) as string | null,
      doorAccess: raw['doorAccess'] as number,
      gap: raw['gap'] as number,
      exceedTime: raw['exceedTime'] as string,
      createTime: raw['createTime'] as string,
      cancelTime: n(raw['cancelTime']) as string | undefined,
      cancelReason: n(raw['cancelReason']) as string | undefined,
    },
    taker: raw['takerUserId']
      ? {
          userId: raw['takerUserId'] as number,
          realname: raw['takerRealname'] as string,
          phone: raw['takerPhone'] as string,
          takeTime: raw['takerTakeTime'] as string,
          deliveryTime: n(raw['takerDeliveryTime']) as string | null,
          image: n(raw['takerImage']) as string | null,
        }
      : null,
    payment: raw['paymentTransactionId']
      ? {
          transactionId: raw['paymentTransactionId'] as string,
          tradeState: raw['paymentTradeState'] as string,
          total: fenToYuan(raw['paymentTotal']),
          serviceFee: fenToYuan(raw['paymentServiceFee']),
          payerOpenid: raw['paymentPayerOpenid'] as string,
          successTime: raw['paymentSuccessTime'] as string,
        }
      : null,
    refund: raw['refundNumber']
      ? {
          refundNumber: raw['refundNumber'] as string,
          refundId: raw['refundId'] as string,
          refundAmount: fenToYuan(raw['refundAmount']),
          refundStatus: raw['refundStatus'] as string,
          reason: raw['refundReason'] as string,
          createTime: raw['refundCreateTime'] as string,
        }
      : null,
  }
}

export async function statistics(): Promise<AdminOrderStatistics> {
  return get<AdminOrderStatistics>('/admin/api/orders/statistics')
}

export async function cancelOrder(id: number, reason: string): Promise<string> {
  return post<string>(`/admin/api/orders/${id}/cancel`, { reason })
}

export async function refundOrder(id: number, reason: string): Promise<string> {
  return post<string>(`/admin/api/orders/${id}/refund`, { reason })
}
