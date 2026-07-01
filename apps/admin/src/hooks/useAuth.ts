/**
 * 认证 Hook
 */

import { useState, useEffect, useCallback } from 'react'
import { tokenManager } from '../utils/token'
import { storage } from '../utils/storage'
import config from '../config/config'
import type { UserInfo } from '../types'

interface UseAuthResult {
  isAuthenticated: boolean
  userInfo: UserInfo | null
  setUserInfo: (userInfo: UserInfo | null) => void
  logout: () => void
}

export function useAuth(): UseAuthResult {
  const [isAuthenticated, setIsAuthenticated] = useState(tokenManager.hasToken())
  const [userInfo, setUserInfoState] = useState<UserInfo | null>(
    storage.get<UserInfo>(config.userInfoKey),
  )

  useEffect(() => {
    // 监听 token 变化
    const hasToken = tokenManager.hasToken()
    setIsAuthenticated(hasToken)

    if (!hasToken) {
      setUserInfoState(null)
    }
  }, [])

  const setUserInfo = useCallback((info: UserInfo | null) => {
    setUserInfoState(info)
    if (info) {
      storage.set(config.userInfoKey, info)
    } else {
      storage.remove(config.userInfoKey)
    }
  }, [])

  const logout = useCallback(() => {
    tokenManager.clearAuth()
    setUserInfoState(null)
    setIsAuthenticated(false)
  }, [])

  return {
    isAuthenticated,
    userInfo,
    setUserInfo,
    logout,
  }
}

