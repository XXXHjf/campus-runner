/**
 * 仪表盘页面
 */

import { useAuthContext } from '../../contexts/AuthContext'
import { useNavigate } from 'react-router-dom'
import './Dashboard.css'

export default function Dashboard() {
  const { userInfo, logout } = useAuthContext()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <div className="dashboard-container">
      <header className="dashboard-header">
        <div className="header-left">
          <h1>帮帮校园送 - 管理后台</h1>
        </div>
        <div className="header-right">
          <span className="user-info">👤 {userInfo?.username || '管理员'}</span>
          <button onClick={handleLogout} className="logout-button">
            退出登录
          </button>
        </div>
      </header>

      <main className="dashboard-main">
        <div className="welcome-card">
          <h2>🎉 欢迎使用中后台管理系统</h2>
          <p>您已成功登录！</p>
        </div>

        <div className="stats-grid">
          <div className="stat-card">
            <div className="stat-icon">📦</div>
            <div className="stat-content">
              <h3>订单管理</h3>
              <p>查看和管理所有订单</p>
            </div>
          </div>

          <div className="stat-card">
            <div className="stat-icon">👥</div>
            <div className="stat-content">
              <h3>用户管理</h3>
              <p>管理用户信息和权限</p>
            </div>
          </div>

          <div className="stat-card">
            <div className="stat-icon">📍</div>
            <div className="stat-content">
              <h3>地址管理</h3>
              <p>维护系统地址信息</p>
            </div>
          </div>

          <div className="stat-card">
            <div className="stat-icon">📂</div>
            <div className="stat-content">
              <h3>分类管理</h3>
              <p>管理跑腿服务分类</p>
            </div>
          </div>
        </div>

        <div className="info-section">
          <h3>📚 已集成功能</h3>
          <ul>
            <li>✅ 完整的 TypeScript 类型系统</li>
            <li>✅ Axios 请求封装（自动 Token、错误处理）</li>
            <li>✅ API 服务层（用户、订单、地址、接单、分类）</li>
            <li>✅ 认证系统（登录、Token 管理、权限控制）</li>
            <li>✅ 工具函数（验证、格式化、存储）</li>
            <li>✅ 自定义 Hooks（useRequest、usePagination、useAuth）</li>
          </ul>
        </div>

        <div className="info-section">
          <h3>🚀 下一步开发建议</h3>
          <ol>
            <li>添加侧边栏导航菜单</li>
            <li>创建订单管理页面（列表、详情、统计）</li>
            <li>创建用户管理页面</li>
            <li>添加数据可视化图表</li>
            <li>集成 UI 组件库（如 Ant Design）</li>
          </ol>
        </div>
      </main>
    </div>
  )
}
