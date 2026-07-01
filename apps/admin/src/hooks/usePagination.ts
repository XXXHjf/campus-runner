/**
 * 分页 Hook
 */

import { useState, useCallback } from 'react'
import { DEFAULT_VALUES } from '../constants'

interface UsePaginationOptions {
  defaultPage?: number
  defaultPageSize?: number
}

interface UsePaginationResult {
  page: number
  pageSize: number
  setPage: (page: number) => void
  setPageSize: (pageSize: number) => void
  reset: () => void
  nextPage: () => void
  prevPage: () => void
}

export function usePagination(options: UsePaginationOptions = {}): UsePaginationResult {
  const { defaultPage = DEFAULT_VALUES.PAGE, defaultPageSize = DEFAULT_VALUES.PAGE_SIZE } = options

  const [page, setPage] = useState(defaultPage)
  const [pageSize, setPageSize] = useState(defaultPageSize)

  const reset = useCallback(() => {
    setPage(defaultPage)
    setPageSize(defaultPageSize)
  }, [defaultPage, defaultPageSize])

  const nextPage = useCallback(() => {
    setPage((prev) => prev + 1)
  }, [])

  const prevPage = useCallback(() => {
    setPage((prev) => Math.max(1, prev - 1))
  }, [])

  return {
    page,
    pageSize,
    setPage,
    setPageSize,
    reset,
    nextPage,
    prevPage,
  }
}

