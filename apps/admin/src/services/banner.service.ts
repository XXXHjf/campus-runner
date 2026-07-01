/**
 * 轮播图服务
 * 封装轮播图相关的 API 调用
 */

import { get, post, del, request } from './request'
import type { Banner, BannerCreateRequest } from '../types'

/**
 * 管理端：获取指定学校轮播图
 */
export async function getBannerList(schoolId: number | string): Promise<Banner[]> {
  return get<Banner[]>(`/admin/api/banner/getList/${schoolId}`)
}

/**
 * 管理端：新增轮播图
 */
export async function addBanner(data: BannerCreateRequest): Promise<Record<string, never>> {
  return post('/admin/api/banner/add', data)
}

/**
 * 管理端：删除轮播图
 */
export async function deleteBanner(id: number | string): Promise<Record<string, never>> {
  return del(`/admin/api/banner/delete/${id}`)
}

/**
 * 上传轮播图图片
 */
export async function uploadBannerImage(
  file: File,
  dirName: string,
  onProgress?: (progress: number) => void,
): Promise<string> {
  const formData = new FormData()
  formData.append('img', file)

  return request<string>({
    url: '/api/upload',
    method: 'POST',
    params: dirName ? { dirName } : undefined,
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data',
    },
    onUploadProgress: (progressEvent: { loaded: number; total?: number }) => {
      if (onProgress && progressEvent.total) {
        const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total)
        onProgress(progress)
      }
    },
  })
}
