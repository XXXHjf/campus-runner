/**
 * 主布局组件
 * 包含侧边栏、顶部导航栏、主内容区域
 */

import { useState } from 'react'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import { useAuthContext } from '../../contexts/AuthContext'
import './MainLayout.css'

type MenuLeaf = {
  key: string
  label: string
  path: string
}

type MenuItem =
  | {
      key: string
      label: string
      icon: string
      path: string
      children?: undefined
    }
  | {
      key: string
      label: string
      icon: string
      children: MenuLeaf[]
      path?: undefined
    }

// 菜单配置
const menuItems: MenuItem[] = [
  {
    key: 'dashboard',
    label: '数据概览',
    icon: '📊',
    path: '/dashboard',
  },
  {
    key: 'orders',
    label: '订单管理',
    icon: '📦',
    children: [
      { key: 'orders-all', label: '全部订单', path: '/orders/all' },
      { key: 'orders-pending', label: '待接单', path: '/orders/pending' },
      { key: 'orders-progress', label: '进行中', path: '/orders/progress' },
      { key: 'orders-completed', label: '已完成', path: '/orders/completed' },
      { key: 'orders-stats', label: '订单统计', path: '/orders/stats' },
    ],
  },
  {
    key: 'take-orders',
    label: '接单管理',
    icon: '🏃',
    children: [
      { key: 'takes-all', label: '全部接单', path: '/takes/all' },
      { key: 'takes-withdrawn', label: '未收款订单', path: '/takes/withdrawn' },
      { key: 'takes-stats', label: '接单统计', path: '/takes/stats' },
    ],
  },
  {
    key: 'users',
    label: '用户管理',
    icon: '👥',
    children: [
      { key: 'users-all', label: '全部用户', path: '/users/all' },
      { key: 'users-authenticated', label: '已认证用户', path: '/users/authenticated' },
      { key: 'users-pending-auth', label: '待审核认证', path: '/users/pending-auth' },
      { key: 'users-stats', label: '用户统计', path: '/users/stats' },
    ],
  },
  {
    key: 'auth',
    label: '审核管理',
    icon: '🧾',
    path: '/auth',
  },
  {
    key: 'address',
    label: '地址管理',
    icon: '📍',
    children: [
      { key: 'address-school', label: '学校管理', path: '/address/school' },
    ],
  },
  {
    key: 'category',
    label: '分类管理',
    icon: '📂',
    path: '/category',
  },
  {
    key: 'second-hand',
    label: '二手交易',
    icon: '♻️',
    path: '/second-hand',
  },
  {
    key: 'finance',
    label: '财务管理',
    icon: '💰',
    children: [
      { key: 'finance-overview', label: '财务概览', path: '/finance/overview' },
      { key: 'finance-income', label: '收益统计', path: '/finance/income' },
      { key: 'finance-withdraw', label: '提现记录', path: '/finance/withdraw' },
    ],
  },
  {
    key: 'message',
    label: '消息管理',
    icon: '📨',
    children: [
      { key: 'message-subscribe', label: '订阅消息', path: '/message/subscribe' },
      { key: 'message-template', label: '消息模板', path: '/message/template' },
    ],
  },
  {
    key: 'banners',
    label: '轮播图管理',
    icon: '🖼️',
    path: '/banners',
  },
  {
    key: 'system',
    label: '系统设置',
    icon: '⚙️',
    children: [
      { key: 'system-config', label: '系统配置', path: '/system/config' },
      { key: 'system-admin', label: '管理员管理', path: '/system/admin' },
      { key: 'system-logs', label: '操作日志', path: '/system/logs' },
    ],
  },
]

export default function MainLayout() {
  const navigate = useNavigate()
  const location = useLocation()
  const { userInfo, logout } = useAuthContext()
  const [collapsed, setCollapsed] = useState(false)
  const [expandedKeys, setExpandedKeys] = useState<string[]>(['orders'])

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const handleMenuClick = (item: MenuItem) => {
    if (collapsed && item.children && item.children.length > 0) {
      // 收起状态下，点击有子菜单的项目，导航到第一个子项
      navigate(item.children[0].path)
    } else if (!collapsed) {
      // 展开状态下，切换展开/收起（手风琴效果：自动收起其他分类）
      setExpandedKeys((prev) => {
        if (prev.includes(item.key)) {
          // 如果当前已展开，则收起
          return prev.filter((k) => k !== item.key)
        } else {
          // 如果当前未展开，则只展开当前项（收起其他所有项）
          return [item.key]
        }
      })
    }
  }

  const isActive = (path?: string) => {
    if (!path) return false
    return location.pathname === path
  }

  const isParentActive = (children?: MenuLeaf[]) => {
    if (!children) return false
    return children.some((child) => location.pathname === child.path)
  }

  return (
    <div className="main-layout">
      {/* 侧边栏 */}
      <aside className={`sidebar ${collapsed ? 'collapsed' : ''}`}>
        <div className="sidebar-header">
          <div className="logo">{collapsed ? '🏃' : ' 帮帮校园送'}</div>
          <button className="collapse-btn" onClick={() => setCollapsed(!collapsed)}>
            {collapsed ? '→' : '←'}
          </button>
        </div>

        <nav className="sidebar-menu">
          {menuItems.map((item) => (
            <div key={item.key} className="menu-item-wrapper">
              {item.children ? (
                <>
                  <div
                    className={`menu-item menu-parent ${isParentActive(item.children) ? 'active' : ''}`}
                    onClick={() => handleMenuClick(item)}
                    title={collapsed ? item.label : ''}
                  >
                    <span className="menu-icon">{item.icon}</span>
                    {!collapsed && (
                      <>
                        <span className="menu-label">{item.label}</span>
                        <span
                          className="menu-arrow"
                          style={{
                            transform: expandedKeys.includes(item.key)
                              ? 'rotate(90deg)'
                              : 'rotate(0deg)',
                          }}
                        >
                          ▶
                        </span>
                      </>
                    )}
                  </div>
                  {!collapsed && expandedKeys.includes(item.key) && (
                    <div className="submenu">
                      {item.children.map((child) => (
                        <div
                          key={child.key}
                          className={`menu-item submenu-item ${isActive(child.path) ? 'active' : ''}`}
                          onClick={() => navigate(child.path)}
                        >
                          <span className="menu-label">{child.label}</span>
                        </div>
                      ))}
                    </div>
                  )}
                </>
              ) : (
                <div
                  className={`menu-item ${isActive(item.path) ? 'active' : ''}`}
                  onClick={() => item.path && navigate(item.path)}
                  title={collapsed ? item.label : ''}
                >
                  <span className="menu-icon">{item.icon}</span>
                  {!collapsed && <span className="menu-label">{item.label}</span>}
                </div>
              )}
            </div>
          ))}
        </nav>
      </aside>

      {/* 主内容区 */}
      <div className="main-content">
        {/* 顶部导航栏 */}
        <header className="top-navbar">
          <div className="navbar-left">
            <h2 className="page-title">中后台管理系统</h2>
          </div>
          <div className="navbar-right">
            <div className="user-info">
              <img
                src={userInfo?.headImg || 'https://api.dicebear.com/7.x/avataaars/svg?seed=admin'}
                alt="头像"
                className="user-avatar"
              />
              <span className="user-name">{userInfo?.username || '管理员'}</span>
            </div>
            <button onClick={handleLogout} className="logout-btn">
              退出登录
            </button>
          </div>
        </header>

        {/* 内容区域 */}
        <main className="content-area">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
