/**
 * 地址服务
 * 封装地址相关的 API 调用
 */

import { get, post, put, del } from './request'
import type { AdminSchoolReserve, AdminSchool, AdminAddressBuilding, AdminUpdateBuildingRequest } from '../types'

/**
 * 管理端：查询所有学校
 */
export async function getSchools(): Promise<AdminSchool[]> {
  return get<AdminSchool[]>('/admin/api/address/schools')
}

/**
 * 管理端：按学校获取楼宇列表
 */
export async function getBuildingsBySchool(
  schoolId: number | string,
): Promise<AdminAddressBuilding[]> {
  return get<AdminAddressBuilding[]>(`/admin/api/address/getListBySchool/${schoolId}`)
}

/**
 * 管理端：新增预设地址
 */
export async function createPresetAddress(
  data: AdminSchoolReserve,
): Promise<Record<string, never>> {
  return post('/admin/api/address', data)
}

/**
 * 管理端：修改楼宇名称
 */
export async function updateBuilding(
  data: AdminUpdateBuildingRequest,
): Promise<Record<string, never>> {
  return put('/admin/api/address/update', data)
}

/**
 * 管理端：删除楼宇
 */
export async function deleteBuilding(buildingID: number | string): Promise<Record<string, never>> {
  return del(`/admin/api/address/delete/${buildingID}`)
}
