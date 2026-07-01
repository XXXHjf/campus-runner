/**
 * KPI 服务：数据概览面板
 */

import { get } from './request'

export async function getTotalOrders(): Promise<number> {
  return get('/admin/api/kpi/orders')
}

export async function getPendingOrders(): Promise<number> {
  return get('/admin/api/kpi/pending')
}

export async function getCompletedOrders(): Promise<number> {
  return get('/admin/api/kpi/completed')
}

export async function getAcceptedOrders(): Promise<number> {
  return get('/admin/api/kpi/accepted')
}

export async function getTodayOrders(): Promise<number> {
  return get('/admin/api/kpi/today')
}

export async function getTotalUsers(): Promise<number> {
  return get('/admin/api/kpi/users')
}

export default {
  getTotalOrders,
  getPendingOrders,
  getCompletedOrders,
  getAcceptedOrders,
  getTodayOrders,
  getTotalUsers,
}
