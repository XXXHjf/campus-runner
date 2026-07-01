/**
 * 登录页面
 */

import { useState, FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuthContext } from '../../contexts/AuthContext'
import { userService } from '../../services'
import { tokenManager } from '../../utils/token'
import { Gender } from '../../types'
import type { UserInfo } from '../../types'
import './Login.css'

export default function Login() {
  const navigate = useNavigate()
  const { setUserInfo } = useAuthContext()

  const [formData, setFormData] = useState({
    username: '',
    password: '',
  })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const getErrorMessage = (err: unknown, fallback: string) => {
    if (err instanceof Error) return err.message
    if (err && typeof err === 'object' && 'message' in err && typeof (err as Record<string, unknown>).message === 'string') {
      return String((err as Record<string, unknown>).message)
    }
    return fallback
  }

  const getTokenFromLoginResponse = (data: unknown) => {
    if (!data || typeof data !== 'object') return null
    const record = data as Record<string, unknown>
    const adminToken = record.adminToken
    if (typeof adminToken === 'string' && adminToken) return adminToken
    const token = record.token
    if (typeof token === 'string' && token) return token
    if (record.data && typeof record.data === 'object') {
      const nested = record.data as Record<string, unknown>
      const nestedAdminToken = nested.adminToken
      if (typeof nestedAdminToken === 'string' && nestedAdminToken) return nestedAdminToken
      const nestedToken = nested.token
      if (typeof nestedToken === 'string' && nestedToken) return nestedToken
    }
    return null
  }

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError('')

    if (!formData.username || !formData.password) {
      setError('请输入用户名和密码')
      return
    }

    setLoading(true)
    try {
      const data = await userService.login({
        username: formData.username,
        password: formData.password,
      })

      console.log('登录响应', data)
      const token = getTokenFromLoginResponse(data)
      if (!token) {
        throw new Error('登录失败：后端未返回 token，请检查账号或接口')
      }

      // 保存 token
      tokenManager.setToken(token)

      // 构造前端展示所需的用户信息
      const adminInfo: UserInfo = {
        id: String(data.id ?? 'admin'),
        phone: 'N/A',
        username: data.nickname || data.username || '管理员',
        headImg: data.avatar || 'https://api.dicebear.com/7.x/avataaars/svg?seed=admin',
        sex: Gender.UNKNOWN,
        schoolId: data.school !== undefined ? String(data.school) : '',
        schoolName: data.school !== undefined ? String(data.school) : undefined,
        realname: data.nickname || data.username || '管理员',
        stuId: 'ADMIN',
        isAuthenticated: true,
      }

      setUserInfo(adminInfo)
      navigate('/dashboard')
    } catch (err: unknown) {
      console.error('登录失败:', err)
      setError(getErrorMessage(err, '登录失败，请重试'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-container">
        <div className="login-box">
          <div className="login-header">
            <h1>帮帮校园送</h1>
            <p>中后台管理系统</p>
          </div>

        <form className="login-form" onSubmit={handleSubmit}>
          {error && (
            <div className="error-message">
              <span>⚠️ {error}</span>
            </div>
          )}

          <div className="form-group">
            <label htmlFor="username">用户名</label>
            <input
              id="username"
              type="text"
              placeholder="请输入用户名"
              value={formData.username}
              onChange={(e) => setFormData({ ...formData, username: e.target.value })}
              disabled={loading}
              autoComplete="username"
            />
          </div>

          <div className="form-group">
            <label htmlFor="password">密码</label>
            <input
              id="password"
              type="password"
              placeholder="请输入密码"
              value={formData.password}
              onChange={(e) => setFormData({ ...formData, password: e.target.value })}
              disabled={loading}
              autoComplete="current-password"
            />
          </div>

          <button type="submit" className="login-button" disabled={loading}>
            {loading ? '登录中...' : '登录'}
          </button>


        </form>
      </div>

      <div className="login-footer">
        <p>© 2025 帮帮校园送 - 中后台管理系统</p>
      </div>
    </div>
  )
}
