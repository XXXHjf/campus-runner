/**
 * 用户管理页面
 * 4 个 Tab：全部用户 / 已认证 / 待审核 / 用户统计
 * 基于 URL path 切换 Tab
 */

import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import {
  Tabs,
  Table,
  Input,
  Button,
  Drawer,
  Descriptions,
  Tag,
  Avatar,
  Card,
  Statistic,
  Row,
  Col,
  message,
} from 'antd'
import type { AdminUserItem, AdminUserDetail, AdminUserStatistics } from '../../types/admin'
import { userService } from '../../services'
import { GENDER_LABELS } from '../../constants'
import { formatDateTime, formatPhone, formatPrice } from '../../utils/format'
import './UserManagement.css'

/** Tab 配置映射 */
const TAB_CONFIG: Record<string, { label: string; api: (page: number, pageSize: number) => Promise<{ total: number; list: AdminUserItem[] }> }> = {
  all: {
    label: '全部用户',
    api: (page, pageSize) => userService.listAllUsers(page, pageSize),
  },
  authenticated: {
    label: '已认证',
    api: (page, pageSize) => userService.listAuthenticated(page, pageSize),
  },
  'pending-auth': {
    label: '待审核',
    api: (page, pageSize) => userService.listPendingReview(page, pageSize),
  },
}

const TAB_KEYS = ['all', 'authenticated', 'pending-auth', 'stats'] as const
type TabKey = (typeof TAB_KEYS)[number]

const PAGE_SIZE = 20

export default function UserManagement() {
  const navigate = useNavigate()
  const location = useLocation()

  // Derive active tab key from URL path
  const pathSegments = location.pathname.split('/').filter(Boolean)
  const tabKey: TabKey = (pathSegments[pathSegments.length - 1] as TabKey) || 'all'

  // List tab state
  const [loading, setLoading] = useState(false)
  const [list, setList] = useState<AdminUserItem[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1)
  const [keyword, setKeyword] = useState('')

  // Detail drawer state
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [detailLoading, setDetailLoading] = useState(false)
  const [detail, setDetail] = useState<AdminUserDetail | null>(null)

  // Stats state
  const [statsLoading, setStatsLoading] = useState(false)
  const [stats, setStats] = useState<AdminUserStatistics | null>(null)

  const isListTab = tabKey !== 'stats'

  // Load list data
  const loadList = async (currentTab: string) => {
    const config = TAB_CONFIG[currentTab]
    if (!config) return
    setLoading(true)
    try {
      const res = await config.api(page, PAGE_SIZE)
      setList(res.list)
      setTotal(res.total)
    } catch (err) {
      console.error('获取用户列表失败', err)
      message.error('获取用户列表失败')
      setList([])
      setTotal(0)
    } finally {
      setLoading(false)
    }
  }

  // Load statistics
  const loadStats = async () => {
    setStatsLoading(true)
    try {
      const data = await userService.userStatistics()
      setStats(data)
    } catch (err) {
      console.error('获取用户统计失败', err)
      message.error('获取用户统计失败')
    } finally {
      setStatsLoading(false)
    }
  }

  useEffect(() => {
    if (isListTab) {
      loadList(tabKey)
    }
  }, [page, tabKey])

  useEffect(() => {
    if (!isListTab) {
      loadStats()
    }
  }, [tabKey])

  // Reset pagination when switching tabs
  useEffect(() => {
    setPage(1)
    setKeyword('')
  }, [tabKey])

  // Frontend keyword filtering
  const filteredList = useMemo(() => {
    if (!keyword.trim()) return list
    const kw = keyword.trim().toLowerCase()
    return list.filter(
      (item) =>
        item.username.toLowerCase().includes(kw) ||
        item.realname.toLowerCase().includes(kw) ||
        item.phone.includes(kw) ||
        item.schoolName.toLowerCase().includes(kw) ||
        String(item.id).includes(kw),
    )
  }, [list, keyword])

  // Open detail drawer
  const openDetail = async (id: number) => {
    setDrawerOpen(true)
    setDetailLoading(true)
    setDetail(null)
    try {
      const data = await userService.userDetail(id)
      setDetail(data)
    } catch (err) {
      console.error('获取用户详情失败', err)
      message.error('获取用户详情失败')
    } finally {
      setDetailLoading(false)
    }
  }

  // Handle tab change
  const onTabChange = (key: string) => {
    navigate(`/users/${key}`, { replace: true })
  }

  // Handle pagination change
  const onPageChange = (newPage: number, newPageSize: number) => {
    setPage(newPage)
  }

  // Columns for list tabs
  const baseColumns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 70,
    },
    {
      title: '头像',
      dataIndex: 'headImg',
      key: 'headImg',
      width: 60,
      render: (headImg: string) => (
        <Avatar src={headImg || undefined} size={36} alt="头像">
          {!headImg ? '?' : undefined}
        </Avatar>
      ),
    },
    {
      title: '昵称',
      dataIndex: 'username',
      key: 'username',
      width: 120,
    },
    {
      title: '真实姓名',
      dataIndex: 'realname',
      key: 'realname',
      width: 100,
      render: (realname: string) => realname || '-',
    },
    {
      title: '性别',
      dataIndex: 'sex',
      key: 'sex',
      width: 60,
      render: (sex: number) => GENDER_LABELS[sex as keyof typeof GENDER_LABELS] || '-',
    },
    {
      title: '手机号',
      dataIndex: 'phone',
      key: 'phone',
      width: 130,
      render: (phone: string) => formatPhone(phone),
    },
    {
      title: '学校',
      dataIndex: 'schoolName',
      key: 'schoolName',
      width: 150,
      render: (schoolName: string) => schoolName || '-',
    },
    {
      title: '学号',
      dataIndex: 'stuId',
      key: 'stuId',
      width: 120,
      render: (stuId: string) => stuId || '-',
    },
    {
      title: '发单数',
      dataIndex: 'orderCount',
      key: 'orderCount',
      width: 70,
    },
    {
      title: '接单数',
      dataIndex: 'takeOrderCount',
      key: 'takeOrderCount',
      width: 70,
    },
    {
      title: '注册时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 170,
      render: (createTime: string) => formatDateTime(createTime),
    },
    {
      title: '操作',
      key: 'action',
      width: 100,
      fixed: 'right' as const,
      render: (_: unknown, record: AdminUserItem) => (
        <Button type="link" onClick={() => openDetail(record.id)}>
          查看详情
        </Button>
      ),
    },
  ]

  // Additional column for pending-auth tab
  const pendingColumns = [
    ...baseColumns.slice(0, 3),
    {
      title: '实名材料',
      dataIndex: 'studentIdCard',
      key: 'studentIdCard',
      width: 100,
      render: (studentIdCard: string | undefined) =>
        studentIdCard ? (
          <Button
            type="link"
            onClick={() => window.open(studentIdCard, '_blank')}
          >
            查看材料
          </Button>
        ) : (
          '-'
        ),
    },
    ...baseColumns.slice(3),
  ]

  const columns = tabKey === 'pending-auth' ? pendingColumns : baseColumns

  // Render auth status tag
  const renderAuthTag = (value: number, trueLabel: string, falseLabel: string) => {
    return value === 1 ? (
      <Tag color="green">{trueLabel}</Tag>
    ) : (
      <Tag color="default">{falseLabel}</Tag>
    )
  }

  return (
    <div className="user-management">
      <div className="page-header">
        <div className="header-left">
          <h1>用户管理</h1>
          <p>管理平台用户信息与认证状态</p>
        </div>
      </div>

      <Tabs
        activeKey={tabKey}
        onChange={onTabChange}
        className="user-tabs"
        items={[
          { key: 'all', label: '全部用户' },
          { key: 'authenticated', label: '已认证' },
          { key: 'pending-auth', label: '待审核' },
          { key: 'stats', label: '用户统计' },
        ]}
      />

      {isListTab ? (
        <>
          <div className="toolbar">
            <Input.Search
              placeholder="搜索用户昵称/姓名/手机号/学校/ID..."
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              onSearch={(value) => setKeyword(value)}
              style={{ width: 360 }}
              allowClear
            />
            <span className="data-count">共 {total} 条</span>
          </div>

          <div className="table-container">
            <Table
              dataSource={filteredList}
              columns={columns}
              rowKey="id"
              loading={loading}
              locale={{ emptyText: '暂无用户数据' }}
              scroll={{ x: 1400 }}
              pagination={{
                current: page,
                pageSize: PAGE_SIZE,
                total,
                showSizeChanger: false,
                showTotal: (t: number) => `共 ${t} 条`,
                onChange: onPageChange,
              }}
            />
          </div>
        </>
      ) : (
        <div className="stats-container">
          <Row gutter={[24, 24]}>
            <Col xs={24} sm={12} lg={8}>
              <Card loading={statsLoading} className="stats-card">
                <Statistic title="用户总数" value={stats?.totalCount ?? '-'} />
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={8}>
              <Card loading={statsLoading} className="stats-card stats-card-auth">
                <Statistic title="已认证" value={stats?.authenticatedCount ?? '-'} />
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={8}>
              <Card loading={statsLoading} className="stats-card stats-card-unauth">
                <Statistic title="未认证" value={stats?.unauthenticatedCount ?? '-'} />
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={8}>
              <Card loading={statsLoading} className="stats-card stats-card-pending">
                <Statistic title="待审核" value={stats?.pendingReviewCount ?? '-'} />
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={8}>
              <Card loading={statsLoading} className="stats-card stats-card-new">
                <Statistic title="今日新增" value={stats?.todayNewCount ?? '-'} />
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={8}>
              <Card loading={statsLoading} className="stats-card stats-card-manager">
                <Statistic title="管理员" value={stats?.managerCount ?? '-'} />
              </Card>
            </Col>
          </Row>
        </div>
      )}

      <Drawer
        title="用户详情"
        placement="right"
        width={560}
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        loading={detailLoading}
      >
        {detail && (
          <>
            <div className="detail-avatar-section">
              <Avatar src={detail.headImg || undefined} size={64} alt="头像">
                {!detail.headImg ? (detail.username?.charAt(0).toUpperCase() ?? '?') : undefined}
              </Avatar>
              <div className="detail-avatar-info">
                <span className="detail-username">{detail.username}</span>
                <span className="detail-id">ID: {detail.id}</span>
              </div>
            </div>

            <Descriptions title="基本信息" column={2} bordered size="small" className="detail-descriptions">
              <Descriptions.Item label="真实姓名" span={2}>
                {detail.realname || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="手机号">{formatPhone(detail.phone)}</Descriptions.Item>
              <Descriptions.Item label="学校" span={2}>
                {detail.schoolName || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="学号" span={2}>
                {detail.stuId || '-'}
              </Descriptions.Item>
            </Descriptions>

            <Descriptions title="认证信息" column={2} bordered size="small" className="detail-descriptions">
              <Descriptions.Item label="认证状态">
                {renderAuthTag(detail.authentication, '已认证', '未认证')}
              </Descriptions.Item>
              <Descriptions.Item label="学生证审核">
                {renderAuthTag(detail.studentIdCardReview, '已通过', '未审核')}
              </Descriptions.Item>
            </Descriptions>

            <Descriptions title="数据统计" column={2} bordered size="small" className="detail-descriptions">
              <Descriptions.Item label="发单数">{detail.orderCount}</Descriptions.Item>
              <Descriptions.Item label="接单数">{detail.takeOrderCount}</Descriptions.Item>
              <Descriptions.Item label="累计收入" span={2}>
                <span style={{ fontWeight: 600, color: '#52c41a' }}>
                  {formatPrice(detail.totalEarned)}
                </span>
              </Descriptions.Item>
            </Descriptions>
          </>
        )}
      </Drawer>
    </div>
  )
}
