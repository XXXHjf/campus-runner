/**
 * 主布局组件
 * 包含侧边栏、顶部导航栏、主内容区域
 */

import { useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import { Avatar, Badge, Button } from 'antd'
import {
  AppstoreOutlined,
  AuditOutlined,
  CarOutlined,
  DashboardOutlined,
  DownOutlined,
  EnvironmentOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  PictureOutlined,
  SettingOutlined,
  ShoppingOutlined,
  SwapOutlined,
  TeamOutlined,
  ThunderboltOutlined,
  UserOutlined,
} from '@ant-design/icons'
import { useAuthContext } from '../../contexts/AuthContext'
import { usePendingAuthCount } from '../../hooks/usePendingAuthCount'
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
      icon: ReactNode
      path: string
      children?: undefined
    }
  | {
      key: string
      label: string
      icon: ReactNode
      children: MenuLeaf[]
      path?: undefined
    }

// 菜单配置
const menuItems: MenuItem[] = [
  {
    key: 'dashboard',
    label: '数据概览',
    icon: <DashboardOutlined />,
    path: '/dashboard',
  },
  {
    key: 'orders',
    label: '订单管理',
    icon: <ShoppingOutlined />,
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
    icon: <CarOutlined />,
    children: [
      { key: 'takes-all', label: '全部接单', path: '/takes/all' },
      { key: 'takes-withdrawn', label: '未收款订单', path: '/takes/withdrawn' },
      { key: 'takes-stats', label: '接单统计', path: '/takes/stats' },
    ],
  },
  {
    key: 'users',
    label: '用户管理',
    icon: <TeamOutlined />,
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
    icon: <AuditOutlined />,
    path: '/auth',
  },
  {
    key: 'address',
    label: '地址管理',
    icon: <EnvironmentOutlined />,
    children: [
      { key: 'address-school', label: '学校管理', path: '/address/school' },
    ],
  },
  {
    key: 'category',
    label: '分类管理',
    icon: <AppstoreOutlined />,
    path: '/category',
  },
  {
    key: 'second-hand',
    label: '二手交易',
    icon: <SwapOutlined />,
    path: '/second-hand',
  },
  {
    key: 'banners',
    label: '轮播图管理',
    icon: <PictureOutlined />,
    path: '/banners',
  },
  {
    key: 'system',
    label: '系统设置',
    icon: <SettingOutlined />,
    children: [
      { key: 'system-config', label: '系统配置', path: '/system/config' },
    ],
  },
]

export default function MainLayout() {
  const pendingAuthCount = usePendingAuthCount()
  const navigate = useNavigate()
  const location = useLocation()
  const { userInfo, logout } = useAuthContext()
  const [collapsed, setCollapsed] = useState(false)
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const [expandedKeys, setExpandedKeys] = useState<string[]>(['orders'])

  useEffect(() => {
    setMobileMenuOpen(false)
  }, [location.pathname])

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
      {mobileMenuOpen && (
        <button
          type="button"
          className="mobile-sidebar-mask"
          aria-label="关闭侧边栏"
          onClick={() => setMobileMenuOpen(false)}
        />
      )}
      <aside
        className={`sidebar ${collapsed ? 'collapsed' : ''} ${mobileMenuOpen ? 'mobile-open' : ''}`}
      >
        <div className="sidebar-header">
          <div className="logo">
            <ThunderboltOutlined />
            {!collapsed && <span>帮帮校园送</span>}
          </div>
          <button
            type="button"
            className="collapse-btn"
            onClick={() => setCollapsed(!collapsed)}
            aria-label={collapsed ? '展开侧边栏' : '收起侧边栏'}
          >
            {collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
          </button>
        </div>

        <nav className="sidebar-menu">
          {menuItems.map((item) => (
            <div key={item.key} className="menu-item-wrapper">
              {item.children ? (
                <>
                  <button
                    type="button"
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
                          <DownOutlined />
                        </span>
                      </>
                    )}
                  </button>
                  {!collapsed && expandedKeys.includes(item.key) && (
                    <div className="submenu">
                      {item.children.map((child) => (
                        <button
                          type="button"
                          key={child.key}
                          className={`menu-item submenu-item ${isActive(child.path) ? 'active' : ''}`}
                          onClick={() => navigate(child.path)}
                        >
                          <span className="menu-label">{child.label}</span>
                          {child.key === 'users-pending-auth' && <Badge count={pendingAuthCount} overflowCount={99} />}
                        </button>
                      ))}
                    </div>
                  )}
                </>
              ) : (
                <button
                  type="button"
                  className={`menu-item ${isActive(item.path) ? 'active' : ''}`}
                  onClick={() => item.path && navigate(item.path)}
                  title={collapsed ? item.label : ''}
                >
                  <span className="menu-icon">{item.icon}</span>
                  {!collapsed && <span className="menu-label">{item.label}</span>}
                </button>
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
            <button
              type="button"
              className="mobile-menu-btn"
              aria-label="打开侧边栏"
              onClick={() => setMobileMenuOpen(true)}
            >
              <MenuUnfoldOutlined />
            </button>
            <h2 className="page-title">中后台管理系统</h2>
          </div>
          <div className="navbar-right">
            <Button icon={<AuditOutlined />} onClick={() => navigate('/users/pending-auth')}>
              待审核认证 <Badge count={pendingAuthCount} overflowCount={99} />
            </Button>
            <div className="user-info">
              <Avatar
                src={userInfo?.headImg || undefined}
                icon={!userInfo?.headImg ? <UserOutlined /> : undefined}
                className="user-avatar"
              />
              <span className="user-name">{userInfo?.username || '管理员'}</span>
            </div>
            <Button onClick={handleLogout} className="logout-btn">
              退出登录
            </Button>
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
