/**
 * 通用请求 Hook
 * 处理请求状态、加载、错误等
 */

import { useState, useCallback } from 'react'
import { RequestStatus } from '../types'
import type { ApiError } from '../types'

interface UseRequestOptions<T> {
  onSuccess?: (data: T) => void
  onError?: (error: ApiError) => void
  manual?: boolean // 是否手动触发
}

interface UseRequestResult<T, P extends unknown[]> {
  data: T | null
  loading: boolean
  error: ApiError | null
  status: RequestStatus
  run: (...params: P) => Promise<T | void>
  reset: () => void
}

export function useRequest<T, P extends unknown[]>(
  requestFn: (...params: P) => Promise<T>,
  options: UseRequestOptions<T> = {},
): UseRequestResult<T, P> {
  const { onSuccess, onError, manual = false } = options

  const [data, setData] = useState<T | null>(null)
  const [loading, setLoading] = useState(!manual)
  const [error, setError] = useState<ApiError | null>(null)
  const [status, setStatus] = useState<RequestStatus>(
    manual ? RequestStatus.IDLE : RequestStatus.LOADING,
  )

  const run = useCallback(
    async (...params: P) => {
      setLoading(true)
      setError(null)
      setStatus(RequestStatus.LOADING)

      try {
        const result = await requestFn(...params)
        setData(result)
        setStatus(RequestStatus.SUCCESS)
        onSuccess?.(result)
        return result
      } catch (err) {
        const apiError = err as ApiError
        setError(apiError)
        setStatus(RequestStatus.ERROR)
        onError?.(apiError)
      } finally {
        setLoading(false)
      }
    },
    [requestFn, onSuccess, onError],
  )

  const reset = useCallback(() => {
    setData(null)
    setError(null)
    setLoading(false)
    setStatus(RequestStatus.IDLE)
  }, [])

  return {
    data,
    loading,
    error,
    status,
    run,
    reset,
  }
}
