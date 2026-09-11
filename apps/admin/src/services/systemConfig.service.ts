/**
 * 系统配置服务
 * 封装系统级业务配置读取与更新能力
 */

import { get, put } from './request'
import type { SystemConfigKey } from '../types'

interface ConfigEndpoint {
  getPath: string
  putPath: string
  payloadKey: SystemConfigKey
}

const configEndpointMap: Record<SystemConfigKey, ConfigEndpoint> = {
  serviceFeeRate: {
    getPath: '/admin/api/config/service_fee_rate',
    putPath: '/admin/api/config/service_fee_rate',
    payloadKey: 'serviceFeeRate',
  },
  serviceFeeMin: {
    getPath: '/admin/api/config/service_fee_min',
    putPath: '/admin/api/config/service_fee_min',
    payloadKey: 'serviceFeeMin',
  },
  secondHandServiceFeeRate: {
    getPath: '/admin/api/config/second_hand_service_fee_rate',
    putPath: '/admin/api/config/second_hand_service_fee_rate',
    payloadKey: 'secondHandServiceFeeRate',
  },
}

function normalizeConfigValue(value: unknown): string {
  if (value === null || value === undefined) return ''
  if (typeof value === 'string' || typeof value === 'number') return String(value)
  return ''
}

/**
 * 获取单个系统配置项
 */
export async function getConfigValue(key: SystemConfigKey): Promise<string> {
  const endpoint = configEndpointMap[key]
  const data = await get<string | number>(endpoint.getPath)
  return normalizeConfigValue(data)
}

/**
 * 更新单个系统配置项
 */
export async function updateConfigValue(
  key: SystemConfigKey,
  value: number,
): Promise<Record<string, never>> {
  const endpoint = configEndpointMap[key]
  return put(endpoint.putPath, {
    [endpoint.payloadKey]: value,
  })
}

/**
 * 获取服务费率
 */
export async function getServiceFeeRate(): Promise<string> {
  return getConfigValue('serviceFeeRate')
}

/**
 * 更新服务费率
 */
export async function updateServiceFeeRate(value: number): Promise<Record<string, never>> {
  return updateConfigValue('serviceFeeRate', value)
}

/**
 * 获取最低服务费
 */
export async function getServiceFeeMin(): Promise<string> {
  return getConfigValue('serviceFeeMin')
}

/**
 * 更新最低服务费
 */
export async function updateServiceFeeMin(value: number): Promise<Record<string, never>> {
  return updateConfigValue('serviceFeeMin', value)
}

export async function getSecondHandServiceFeeRate(): Promise<string> {
  return getConfigValue('secondHandServiceFeeRate')
}

export async function updateSecondHandServiceFeeRate(
  value: number,
): Promise<Record<string, never>> {
  return updateConfigValue('secondHandServiceFeeRate', value)
}

export default {
  getConfigValue,
  updateConfigValue,
  getServiceFeeRate,
  updateServiceFeeRate,
  getServiceFeeMin,
  updateServiceFeeMin,
  getSecondHandServiceFeeRate,
  updateSecondHandServiceFeeRate,
}
