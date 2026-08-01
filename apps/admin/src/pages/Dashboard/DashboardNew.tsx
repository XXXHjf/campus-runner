/**
 * 数据概览页面（新版）
 */

import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, Card, Col, Descriptions, Row, Spin, Statistic } from 'antd'
import {
  AppstoreOutlined,
  CalendarOutlined,
  CarOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  EnvironmentOutlined,
  ShoppingOutlined,
  TeamOutlined,
} from '@ant-design/icons'
import { kpiService } from '../../services'
import { AdminContentCard, AdminPage, AdminPageHeader } from '../../components/admin'
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
      icon: <ShoppingOutlined />,
      color: '#667eea',
      path: '/orders/all',
    },
    {
      title: '待接单',
      value: stats.orderPending,
      icon: <ClockCircleOutlined />,
      color: '#f59e0b',
      path: '/orders/pending',
    },
    {
      title: '已完成',
      value: stats.orderCompleted,
      icon: <CheckCircleOutlined />,
      color: '#10b981',
      path: '/orders/completed',
    },
    {
      title: '今日订单',
      value: stats.orderToday,
      icon: <CalendarOutlined />,
      color: '#3b82f6',
      path: '/orders/stats',
    },
    {
      title: '用户总数',
      value: stats.userTotal,
      icon: <TeamOutlined />,
      color: '#8b5cf6',
      path: '/users/all',
    },
    {
      title: '接单总数',
      value: stats.takeTotal,
      icon: <CarOutlined />,
      color: '#ec4899',
      path: '/takes/all',
    },
  ]

  const quickActions = [
    { label: '查看全部订单', icon: <ShoppingOutlined />, path: '/orders/all' },
    { label: '用户管理', icon: <TeamOutlined />, path: '/users/all' },
    { label: '学校管理', icon: <EnvironmentOutlined />, path: '/address/school' },
    { label: '分类管理', icon: <AppstoreOutlined />, path: '/category' },
  ]

  if (loading) {
    return (
      <div className="dashboard-loading">
        <Spin size="large" description="加载数据" />
      </div>
    )
  }

  return (
    <AdminPage className="dashboard-new">
      <AdminPageHeader
        title="数据概览"
        description="查看平台核心数据并快速进入常用管理功能"
      />

      <Row gutter={[16, 16]}>
        {statCards.map((card) => (
          <Col key={card.title} xs={24} sm={12} xl={8}>
            <Card
              hoverable
              className="dashboard-stat-card"
              onClick={() => navigate(card.path)}
            >
              <div className="dashboard-stat-card__icon" style={{ color: card.color }}>
                {card.icon}
              </div>
              <Statistic title={card.title} value={card.value} />
            </Card>
          </Col>
        ))}
      </Row>

      <AdminContentCard title="快捷操作">
        <div className="dashboard-actions">
          {quickActions.map((action) => (
            <Button
              key={action.label}
              icon={action.icon}
              onClick={() => navigate(action.path)}
            >
              {action.label}
            </Button>
          ))}
        </div>
      </AdminContentCard>

      <AdminContentCard title="系统信息">
        <Descriptions
          column={{ xs: 1, sm: 2, lg: 4 }}
          items={[
            { key: 'version', label: '系统版本', children: 'v1.0.0' },
            { key: 'react', label: 'React 版本', children: '19.1.1' },
            { key: 'vite', label: '构建工具', children: 'Vite 7.1.12' },
            { key: 'api', label: '服务域名', children: 'campusrunner.top' },
          ]}
        />
      </AdminContentCard>
    </AdminPage>
  )
}
