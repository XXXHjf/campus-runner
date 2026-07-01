/**
 * Axios 请求封装
 * 处理 token、请求/响应拦截、错误处理等通用逻辑
 */

import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse, AxiosError } from 'axios'
import config from '../config/config'
import { tokenManager } from '../utils/token'
import type { ApiResponse, ApiError } from '../types'
import { HTTP_STATUS, ERROR_MESSAGES } from '../constants'

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function hasDataPayload<T>(value: unknown): value is { data: T } {
  return isRecord(value) && 'data' in value
}

// 创建 axios 实例
const instance: AxiosInstance = axios.create({
  baseURL: config.apiBaseUrl,
  timeout: config.requestTimeout,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 请求拦截器
instance.interceptors.request.use(
  (reqConfig) => {
    // 添加 token
    const token = tokenManager.getToken()
    if (token && reqConfig.headers) {
      reqConfig.headers.token = token
    }

    // 开发环境打印请求日志
    if (config.enableRequestLog) {
      console.log('📤 Request:', {
        url: reqConfig.url,
        method: reqConfig.method,
        params: reqConfig.params,
        data: reqConfig.data,
      })
    }

    return reqConfig
  },
  (error: AxiosError) => {
    console.error('❌ Request Error:', error)
    return Promise.reject(error)
  },
)

// 响应拦截器
instance.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const respData: unknown = response.data
    const respObj = isRecord(respData) ? respData : null
    const codeRaw = respObj?.code
    const codeNum = typeof codeRaw === 'string' ? Number(codeRaw) : codeRaw
    const normalizedCode = typeof codeNum === 'number' ? codeNum : -1

    // 开发环境打印响应日志
    if (config.enableRequestLog) {
      console.log('📥 Response:', {
        url: response.config.url,
        data: respData,
      })
    }

    // 统一按后端 code 判断
    if (respObj && 'code' in respObj) {
      const success =
        codeNum === 0 ||
        codeNum === 1 ||
        codeRaw === '0' ||
        codeRaw === '1' ||
        codeNum === HTTP_STATUS.OK

      // 后端部分接口可能返回非 0/200 但 data 内含有效数据，允许通过
      if (!success) {
        const data = respObj.data
        const hasPayload =
          Array.isArray(data) || (isRecord(data) && Object.keys(data).length > 0)
        const hasToken = isRecord(data) && ('adminToken' in data || 'token' in data)
        if (hasToken || hasPayload) {
          return response
        }

        const message =
          (typeof respObj.message === 'string' && respObj.message) ||
          (typeof respObj.msg === 'string' && respObj.msg) ||
          ERROR_MESSAGES.UNKNOWN_ERROR
        return Promise.reject({
          code: normalizedCode,
          message,
          errors: isRecord(respObj.errors) ? (respObj.errors as Record<string, string[]>) : undefined,
        } as ApiError)
      }
    }

    // 正常响应
    if (response.status === HTTP_STATUS.OK) {
      return response
    }

    // 其他状态码
    const message =
      (respObj && typeof respObj.message === 'string' && respObj.message) || ERROR_MESSAGES.UNKNOWN_ERROR
    return Promise.reject(new Error(message))
  },
  async (error: AxiosError<ApiResponse>) => {
    console.error('❌ Response Error:', error)

    // 处理 401 未授权
    if (error.response?.status === HTTP_STATUS.UNAUTHORIZED) {
      console.log('收到 401 响应，需要重新登录')
      tokenManager.clearAuth()

      // 跳转到登录页
      window.location.href = '/login'

      return Promise.reject({
        code: HTTP_STATUS.UNAUTHORIZED,
        message: ERROR_MESSAGES.UNAUTHORIZED,
      } as ApiError)
    }

    // 处理 403 禁止访问
    if (error.response?.status === HTTP_STATUS.FORBIDDEN) {
      return Promise.reject({
        code: HTTP_STATUS.FORBIDDEN,
        message: ERROR_MESSAGES.FORBIDDEN,
      } as ApiError)
    }

    // 处理 404 未找到
    if (error.response?.status === HTTP_STATUS.NOT_FOUND) {
      return Promise.reject({
        code: HTTP_STATUS.NOT_FOUND,
        message: ERROR_MESSAGES.NOT_FOUND,
      } as ApiError)
    }

    // 处理 500 服务器错误
    if (error.response?.status === HTTP_STATUS.INTERNAL_SERVER_ERROR) {
      return Promise.reject({
        code: HTTP_STATUS.INTERNAL_SERVER_ERROR,
        message: ERROR_MESSAGES.SERVER_ERROR,
      } as ApiError)
    }

    // 处理超时
    if (error.code === 'ECONNABORTED') {
      return Promise.reject({
        code: -1,
        message: ERROR_MESSAGES.TIMEOUT_ERROR,
      } as ApiError)
    }

    // 处理网络错误
    if (!error.response) {
      return Promise.reject({
        code: -1,
        message: ERROR_MESSAGES.NETWORK_ERROR,
      } as ApiError)
    }

    // 其他错误
    return Promise.reject({
      code: error.response?.status || -1,
      message: error.response?.data?.message || ERROR_MESSAGES.UNKNOWN_ERROR,
    } as ApiError)
  },
)

/**
 * 通用请求方法
 */
export async function request<T = unknown>(reqConfig: AxiosRequestConfig): Promise<T> {
  const response = await instance.request<ApiResponse<T>>(reqConfig)
  const respData: unknown = response.data
  if (hasDataPayload<T>(respData)) {
    return respData.data
  }
  return respData as T
}

/**
 * GET 请求
 */
export async function get<T = unknown>(
  url: string,
  params?: Record<string, unknown> | unknown,
  reqConfig?: AxiosRequestConfig,
): Promise<T> {
  return request<T>({
    url,
    method: 'GET',
    params,
    ...reqConfig,
  })
}

/**
 * POST 请求
 */
export async function post<T = unknown>(
  url: string,
  data?: Record<string, unknown> | unknown,
  reqConfig?: AxiosRequestConfig,
): Promise<T> {
  return request<T>({
    url,
    method: 'POST',
    data,
    ...reqConfig,
  })
}

/**
 * PUT 请求
 */
export async function put<T = unknown>(
  url: string,
  data?: Record<string, unknown> | unknown,
  reqConfig?: AxiosRequestConfig,
): Promise<T> {
  return request<T>({
    url,
    method: 'PUT',
    data,
    ...reqConfig,
  })
}

/**
 * DELETE 请求
 */
export async function del<T = unknown>(
  url: string,
  params?: Record<string, unknown> | unknown,
  reqConfig?: AxiosRequestConfig,
): Promise<T> {
  return request<T>({
    url,
    method: 'DELETE',
    params,
    ...reqConfig,
  })
}

/**
 * 上传文件
 */
export async function upload<T = unknown>(
  url: string,
  file: File,
  onProgress?: (progress: number) => void,
): Promise<T> {
  const formData = new FormData()
  formData.append('file', file)

  return request<T>({
    url,
    method: 'POST',
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

export default instance
