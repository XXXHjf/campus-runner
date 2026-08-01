/**
 * 登录页面
 */

import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Alert, Button, Card, Form, Input } from 'antd'
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

  const handleSubmit = async () => {
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

      const token = getTokenFromLoginResponse(data)
      if (!token) {
        throw new Error('登录失败，请确认账号信息后重试')
      }

      // 保存 token
      tokenManager.setToken(token)

      // 构造前端展示所需的用户信息
      const adminInfo: UserInfo = {
        id: String(data.id ?? 'admin'),
        phone: 'N/A',
        username: data.nickname || data.username || '管理员',
        headImg: data.avatar || '',
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
      <Card className="login-box" bordered={false}>
        <div className="login-header">
          <h1>帮帮校园送</h1>
          <p>中后台管理系统</p>
        </div>

        <Form className="login-form" layout="vertical" onFinish={handleSubmit}>
          {error && <Alert type="error" showIcon title={error} />}

          <Form.Item label="用户名" required>
            <Input
              placeholder="请输入用户名"
              value={formData.username}
              onChange={(event) =>
                setFormData((prev) => ({ ...prev, username: event.target.value }))
              }
              disabled={loading}
              autoComplete="username"
            />
          </Form.Item>

          <Form.Item label="密码" required>
            <Input.Password
              placeholder="请输入密码"
              value={formData.password}
              onChange={(event) =>
                setFormData((prev) => ({ ...prev, password: event.target.value }))
              }
              disabled={loading}
              autoComplete="current-password"
            />
          </Form.Item>

          <Button type="primary" htmlType="submit" block loading={loading}>
            登录
          </Button>
        </Form>
      </Card>

      <div className="login-footer">
        <p>© 2025 帮帮校园送 - 中后台管理系统</p>
      </div>
    </div>
  )
}
