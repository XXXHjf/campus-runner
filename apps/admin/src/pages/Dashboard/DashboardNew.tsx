/**
 * 数据概览页面（新版）
 */

import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { kpiService } from '../../services'
import './DashboardNew.css'

export default function DashboardNew() {
  const navigate = useNavigate()
  const [loading, setLoading] = useState(true)
  const [stats, setStats] = useState({
    orderTotal: 0,
    orderPending: 0,
    orderCompleted: 0,
    orderToday: 0,
    userTotal: 0,
    takeTotal: 0,
  })

  useEffect(() => {
    loadStatistics()
  }, [])

  const loadStatistics = async () => {
    setLoading(true)
    try {
      const [
        orderTotal,
        orderPending,
        orderCompleted,
        orderToday,
        userTotal,
        takeTotal,
      ] = await Promise.all([
        kpiService.getTotalOrders(),
        kpiService.getPendingOrders(),
        kpiService.getCompletedOrders(),
        kpiService.getTodayOrders(),
        kpiService.getTotalUsers(),
        kpiService.getAcceptedOrders(),
      ])

      setStats({
        orderTotal: orderTotal ?? 0,
        orderPending: orderPending ?? 0,
        orderCompleted: orderCompleted ?? 0,
        orderToday: orderToday ?? 0,
        userTotal: userTotal ?? 0,
        takeTotal: takeTotal ?? 0,
      })
    } catch (error) {
      console.error('加载统计数据失败:', error)
    } finally {
      setLoading(false)
    }
  }

  const statCards = [
    {
      title: '订单总数',
      value: stats.orderTotal,
      icon: '📦',
      color: '#667eea',
      path: '/orders/all',
    },
    {
      title: '待接单',
      value: stats.orderPending,
      icon: '⏳',
      color: '#f59e0b',
      path: '/orders/pending',
    },
    {
      title: '已完成',
      value: stats.orderCompleted,
      icon: '✅',
      color: '#10b981',
      path: '/orders/completed',
    },
    {
      title: '今日订单',
      value: stats.orderToday,
      icon: '📊',
      color: '#3b82f6',
      path: '/orders/stats',
    },
    {
      title: '用户总数',
      value: stats.userTotal,
      icon: '👥',
      color: '#8b5cf6',
      path: '/users/all',
    },
    {
      title: '接单总数',
      value: stats.takeTotal,
      icon: '🏃',
      color: '#ec4899',
      path: '/takes/all',
    },
  ]

  const quickActions = [
    { label: '查看全部订单', icon: '📦', path: '/orders/all' },
    { label: '用户管理', icon: '👥', path: '/users/all' },
    { label: '地址管理', icon: '📍', path: '/address/system' },
    { label: '分类管理', icon: '📂', path: '/category' },
  ]

  if (loading) {
    return (
      <div className="dashboard-loading">
        <div className="loading-spinner"></div>
        <p>加载中...</p>
      </div>
    )
  }

  return (
    <div className="dashboard-new">
      <div className="dashboard-header">
        <h1>数据概览</h1>
        <p>欢迎使用帮帮校园送中后台管理系统</p>
      </div>

      {/* 统计卡片 */}
      <div className="stats-grid">
        {statCards.map((card, index) => (
          <div
            key={index}
            className="stat-card"
            style={{ '--card-color': card.color } as React.CSSProperties}
            onClick={() => navigate(card.path)}
          >
            <div className="stat-icon">{card.icon}</div>
            <div className="stat-content">
              <h3>{card.title}</h3>
              <p className="stat-value">{card.value.toLocaleString()}</p>
            </div>
          </div>
        ))}
      </div>

      {/* 快捷操作 */}
      <div className="quick-actions">
        <h2>快捷操作</h2>
        <div className="actions-grid">
          {quickActions.map((action, index) => (
            <button key={index} className="action-btn" onClick={() => navigate(action.path)}>
              <span className="action-icon">{action.icon}</span>
              <span className="action-label">{action.label}</span>
            </button>
          ))}
        </div>
      </div>

      {/* 系统信息 */}
      <div className="system-info">
        <h2>系统信息</h2>
        <div className="info-grid">
          <div className="info-item">
            <span className="info-label">系统版本</span>
            <span className="info-value">v1.0.0</span>
          </div>
          <div className="info-item">
            <span className="info-label">React 版本</span>
            <span className="info-value">19.1.1</span>
          </div>
          <div className="info-item">
            <span className="info-label">构建工具</span>
            <span className="info-value">Vite 7.1.12</span>
          </div>
          <div className="info-item">
            <span className="info-label">API 地址</span>
            <span className="info-value">campusrunner.top</span>
          </div>
        </div>
      </div>
    </div>
  )
}
