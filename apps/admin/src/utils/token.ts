/**
 * Token 管理工具
 */

import { storage } from './storage'
import config from '../config/config'

class TokenManager {
  private tokenKey = config.tokenKey

  /**
   * 获取 token
   */
  getToken(): string | null {
    return storage.get<string>(this.tokenKey)
  }

  /**
   * 设置 token
   */
  setToken(token: string): void {
    storage.set(this.tokenKey, token)
  }

  /**
   * 移除 token
   */
  removeToken(): void {
    storage.remove(this.tokenKey)
  }

  /**
   * 检查是否有 token
   */
  hasToken(): boolean {
    return storage.has(this.tokenKey)
  }

  /**
   * 清除所有认证信息
   */
  clearAuth(): void {
    this.removeToken()
    storage.remove(config.userInfoKey)
  }
}

export const tokenManager = new TokenManager()
export default tokenManager

