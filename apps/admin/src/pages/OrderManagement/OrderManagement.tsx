/**
 * 订单管理页面
 * 5 个 Tab：全部 / 待接单 / 进行中 / 已完成 / 已取消退款 + 订单统计
 * 基于 URL path 切换 Tab
 */

import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import {
  Tabs,
  Table,
  Input,
  Button,
  Modal,
  Descriptions,
  Tag,
  Card,
  Statistic,
  Row,
  Col,
  message,
} from 'antd'
import type { AdminOrderItem, AdminOrderDetailResponse, AdminOrderStatistics } from '../../types/admin'
import { orderService } from '../../services'
import { DOOR_ACCESS_LABELS } from '../../constants'
import { formatDateTime, formatPrice, formatPhone } from '../../utils/format'
import OrderDetail from './OrderDetail'
import './OrderManagement.css'

/** 订单状态映射（覆盖后端全部状态码 -4 ~ 7） */
const orderStatusMap: Record<number, { label: string; color: string }> = {
  [-4]: { label: '退款异常', color: '#dc2626' },
  [-3]: { label: '退款成功', color: '#6b7280' },
  [-2]: { label: '退款中', color: '#f59e0b' },
  [-1]: { label: '未支付', color: '#9ca3af' },
  0: { label: '待接单', color: '#0052d9' },
  1: { label: '已接单', color: '#029cd4' },
  2: { label: '派送中', color: '#0ba360' },
  3: { label: '已送达', color: '#2ba471' },
  4: { label: '已取消', color: '#e34d59' },
  5: { label: '已完成', color: '#10b981' },
  6: { label: '提现成功', color: '#10b981' },
  7: { label: '提现失败', color: '#dc2626' },
}

function getOrderStatusTag(status: number) {
  const entry = orderStatusMap[status]
  if (entry) {
    return <Tag color={entry.color}>{entry.label}</Tag>
  }
  return <Tag color="default">未知({status})</Tag>
}

/** Tab 配置映射 */
const TAB_CONFIG: Record<string, { label: string; api: (page: number, pageSize: number) => Promise<{ total: number; list: AdminOrderItem[] }> }> = {
  all: {
    label: '全部',
    api: (page, pageSize) => orderService.listAll(page, pageSize),
  },
  pending: {
    label: '待接单',
    api: (page, pageSize) => orderService.listWaiting(page, pageSize),
  },
  progress: {
    label: '进行中',
    api: (page, pageSize) => orderService.listInProgress(page, pageSize),
  },
  completed: {
    label: '已完成',
    api: (page, pageSize) => orderService.listCompleted(page, pageSize),
  },
  canceled: {
    label: '已取消/退款',
    api: (page, pageSize) => orderService.listCanceled(page, pageSize),
  },
}

const TAB_KEYS = ['all', 'pending', 'progress', 'completed', 'canceled', 'stats'] as const
type TabKey = (typeof TAB_KEYS)[number]

const PAGE_SIZE = 20

export default function OrderManagement() {
  const navigate = useNavigate()
  const location = useLocation()

  // Derive active tab key from URL path
  const pathSegments = location.pathname.split('/').filter(Boolean)
  const tabKey: TabKey = (pathSegments[pathSegments.length - 1] as TabKey) || 'all'

  // List tab state
  const [loading, setLoading] = useState(false)
  const [list, setList] = useState<AdminOrderItem[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1)
  const [keyword, setKeyword] = useState('')

  // Detail modal state
  const [detailModal, setDetailModal] = useState<{
    open: boolean
    loading: boolean
    data: AdminOrderDetailResponse | null
  }>({ open: false, loading: false, data: null })

  // Action (cancel/refund) modal state
  const [actionModal, setActionModal] = useState<{
    open: boolean
    type: 'cancel' | 'refund'
    id: number | null
    reason: string
    submitting: boolean
    error: string
  }>({ open: false, type: 'cancel', id: null, reason: '', submitting: false, error: '' })

  // Stats state
  const [statsLoading, setStatsLoading] = useState(false)
  const [stats, setStats] = useState<AdminOrderStatistics | null>(null)

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
      console.error('获取订单列表失败', err)
      message.error('获取订单列表失败')
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
      const data = await orderService.statistics()
      setStats(data)
    } catch (err) {
      console.error('获取订单统计失败', err)
      message.error('获取订单统计失败')
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

  // Reset pagination and keyword when switching tabs
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
        item.orderNumber.toLowerCase().includes(kw) ||
        item.username.toLowerCase().includes(kw) ||
        item.phone.includes(kw),
    )
  }, [list, keyword])

  // Open detail modal
  const openDetail = async (id: number) => {
    setDetailModal({ open: true, loading: true, data: null })
    try {
      const data = await orderService.detail(id)
      setDetailModal({ open: true, loading: false, data })
    } catch (err) {
      console.error('获取订单详情失败', err)
      message.error('获取订单详情失败')
      setDetailModal({ open: false, loading: false, data: null })
    }
  }

  // Open action modal (cancel / refund)
  const openActionModal = (type: 'cancel' | 'refund', id: number) => {
    setActionModal({ open: true, type, id, reason: '', submitting: false, error: '' })
  }

  // Confirm action (cancel / refund)
  const confirmAction = async () => {
    const { type, id, reason } = actionModal
    if (!id) return
    if (!reason.trim()) {
      setActionModal((prev) => ({ ...prev, error: '请输入原因' }))
      return
    }
    if (reason.trim().length < 2) {
      setActionModal((prev) => ({ ...prev, error: '原因至少需要2个字符' }))
      return
    }

    setActionModal((prev) => ({ ...prev, submitting: true, error: '' }))
    try {
      if (type === 'cancel') {
        await orderService.cancelOrder(id, reason.trim())
      } else {
        await orderService.refundOrder(id, reason.trim())
      }
      message.success(type === 'cancel' ? '订单已取消' : '退款已处理')
      setActionModal({ open: false, type: 'cancel', id: null, reason: '', submitting: false, error: '' })
      // Refresh current list
      loadList(tabKey)
    } catch (err) {
      console.error(`操作失败`, err)
      setActionModal((prev) => ({ ...prev, error: '操作失败，请稍后重试', submitting: false }))
    }
  }

  // Handle tab change
  const onTabChange = (key: string) => {
    navigate(`/orders/${key}`, { replace: true })
  }

  // Handle pagination change
  const onPageChange = (newPage: number) => {
    setPage(newPage)
  }

  // Determine if a row can be canceled
  const canCancel = (status: number) => status === 0 || status === -1

  // Determine if a row can be refunded
  const canRefund = (status: number) => status > 0 && status !== 6 && status !== 7

  // Base columns for all list tabs
  const baseColumns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 70,
    },
    {
      title: '订单编号',
      dataIndex: 'orderNumber',
      key: 'orderNumber',
      width: 180,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: number) => getOrderStatusTag(status),
    },
    {
      title: '分类',
      dataIndex: 'categoryName',
      key: 'categoryName',
      width: 100,
      render: (categoryName: string) => categoryName || '-',
    },
    {
      title: '金额',
      dataIndex: 'price',
      key: 'price',
      width: 90,
      render: (price: number) => formatPrice(price),
    },
    {
      title: '服务费',
      dataIndex: 'serviceFee',
      key: 'serviceFee',
      width: 90,
      render: (serviceFee: number) => formatPrice(serviceFee),
    },
    {
      title: '支付总额',
      dataIndex: 'payAmount',
      key: 'payAmount',
      width: 100,
      render: (payAmount: number) => formatPrice(payAmount),
    },
    {
      title: '发单人',
      dataIndex: 'username',
      key: 'username',
      width: 100,
    },
    {
      title: '联系电话',
      dataIndex: 'phone',
      key: 'phone',
      width: 130,
      render: (phone: string) => formatPhone(phone),
    },
    {
      title: '取件地址',
      dataIndex: 'pickUpAddress',
      key: 'pickUpAddress',
      width: 180,
      ellipsis: true,
    },
    {
      title: '收件地址',
      dataIndex: 'reciveAddress',
      key: 'reciveAddress',
      width: 180,
      ellipsis: true,
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 170,
      render: (createTime: string) => formatDateTime(createTime),
    },
    {
      title: '操作',
      key: 'action',
      width: 180,
      fixed: 'right' as const,
      render: (_: unknown, record: AdminOrderItem) => (
        <div className="action-buttons">
          <Button type="link" size="small" onClick={() => openDetail(record.id)}>
            详情
          </Button>
          {canCancel(record.status) && (
            <Button type="link" size="small" danger onClick={() => openActionModal('cancel', record.id)}>
              取消订单
            </Button>
          )}
          {canRefund(record.status) && (
            <Button type="link" size="small" danger onClick={() => openActionModal('refund', record.id)}>
              退款
            </Button>
          )}
        </div>
      ),
    },
  ]

  // Extra columns for in-progress / completed tabs
  const progressExtraColumns = [
    {
      title: '接单人',
      dataIndex: 'takerName',
      key: 'takerName',
      width: 100,
      render: (takerName: string | undefined) => takerName || '-',
    },
    {
      title: '接单电话',
      dataIndex: 'takerPhone',
      key: 'takerPhone',
      width: 130,
      render: (takerPhone: string | undefined) => (takerPhone ? formatPhone(takerPhone) : '-'),
    },
    {
      title: '接单时间',
      dataIndex: 'takeOrderTime',
      key: 'takeOrderTime',
      width: 170,
      render: (takeOrderTime: string | undefined) => (takeOrderTime ? formatDateTime(takeOrderTime) : '-'),
    },
    {
      title: '送达时间',
      dataIndex: 'deliveryTime',
      key: 'deliveryTime',
      width: 170,
      render: (deliveryTime: string | undefined) => (deliveryTime ? formatDateTime(deliveryTime) : '-'),
    },
  ]

  // Extra columns for canceled tab
  const canceledExtraColumns = [
    {
      title: '取消时间',
      dataIndex: 'cancelTime',
      key: 'cancelTime',
      width: 170,
      render: (cancelTime: string | undefined) => (cancelTime ? formatDateTime(cancelTime) : '-'),
    },
    {
      title: '取消原因',
      dataIndex: 'cancelReason',
      key: 'cancelReason',
      width: 150,
      ellipsis: true,
      render: (cancelReason: string | undefined) => cancelReason || '-',
    },
    {
      title: '退款金额',
      dataIndex: 'refundAmount',
      key: 'refundAmount',
      width: 100,
      render: (refundAmount: number | undefined) =>
        refundAmount !== undefined && refundAmount !== null ? formatPrice(refundAmount) : '-',
    },
    {
      title: '退款状态',
      dataIndex: 'refundStatus',
      key: 'refundStatus',
      width: 100,
      render: (refundStatus: string | undefined) => refundStatus || '-',
    },
  ]

  const columns = useMemo(() => {
    if (tabKey === 'progress' || tabKey === 'completed') {
      return [
        ...baseColumns.slice(0, 12),
        ...progressExtraColumns,
        baseColumns[baseColumns.length - 1], // action column
      ]
    }
    if (tabKey === 'canceled') {
      return [
        ...baseColumns.slice(0, 12),
        ...canceledExtraColumns,
        baseColumns[baseColumns.length - 1], // action column
      ]
    }
    return baseColumns
  }, [tabKey])

  const actionModalTitle = actionModal.type === 'cancel' ? '取消订单' : '订单退款'

  return (
    <div className="order-management">
      <div className="page-header">
        <div className="header-left">
          <h1>订单管理</h1>
          <p>管理平台订单信息与状态</p>
        </div>
      </div>

      <Tabs
        activeKey={tabKey}
        onChange={onTabChange}
        className="order-tabs"
        items={[
          { key: 'all', label: '全部' },
          { key: 'pending', label: '待接单' },
          { key: 'progress', label: '进行中' },
          { key: 'completed', label: '已完成' },
          { key: 'canceled', label: '已取消/退款' },
          { key: 'stats', label: '订单统计' },
        ]}
      />

      {isListTab ? (
        <>
          <div className="toolbar">
            <Input.Search
              placeholder="搜索订单编号/发单人/联系电话..."
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
              locale={{ emptyText: '暂无订单数据' }}
              scroll={{ x: 1800 }}
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
                <Statistic title="订单总数" value={stats?.totalCount ?? '-'} />
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={8}>
              <Card loading={statsLoading} className="stats-card stats-card-waiting">
                <Statistic title="待接单" value={stats?.waitingCount ?? '-'} />
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={8}>
              <Card loading={statsLoading} className="stats-card stats-card-progress">
                <Statistic title="进行中" value={stats?.inProgressCount ?? '-'} />
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={8}>
              <Card loading={statsLoading} className="stats-card stats-card-completed">
                <Statistic title="已完成" value={stats?.completedCount ?? '-'} />
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={8}>
              <Card loading={statsLoading} className="stats-card stats-card-canceled">
                <Statistic title="已取消" value={stats?.canceledCount ?? '-'} />
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={8}>
              <Card loading={statsLoading} className="stats-card stats-card-new">
                <Statistic title="今日新增" value={stats?.todayNewCount ?? '-'} />
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={8}>
              <Card loading={statsLoading} className="stats-card stats-card-amount">
                <Statistic
                  title="今日支付总额"
                  value={stats?.todayTotalAmount ? formatPrice(stats.todayTotalAmount) : '-'}
                />
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={8}>
              <Card loading={statsLoading} className="stats-card stats-card-fee">
                <Statistic
                  title="今日服务费"
                  value={stats?.todayServiceFee ? formatPrice(stats.todayServiceFee) : '-'}
                />
              </Card>
            </Col>
          </Row>
        </div>
      )}

      {/* Order Detail Modal */}
      <Modal
        title="订单详情"
        open={detailModal.open}
        onCancel={() => setDetailModal({ open: false, loading: false, data: null })}
        footer={[
          <Button key="close" onClick={() => setDetailModal({ open: false, loading: false, data: null })}>
            关闭
          </Button>,
        ]}
        width={900}
        destroyOnClose
      >
        <OrderDetail detail={detailModal.data} loading={detailModal.loading} />
      </Modal>

      {/* Cancel / Refund Modal */}
      <Modal
        title={actionModalTitle}
        open={actionModal.open}
        onCancel={() => setActionModal({ open: false, type: 'cancel', id: null, reason: '', submitting: false, error: '' })}
        onOk={confirmAction}
        confirmLoading={actionModal.submitting}
        okText="确认"
        cancelText="取消"
        destroyOnClose
      >
        <div className="action-modal-form">
          {actionModal.error && (
            <div className="action-modal-error">{actionModal.error}</div>
          )}
          <div className="action-modal-field">
            <label>
              {actionModal.type === 'cancel' ? '取消原因' : '退款原因'}
              <span className="required-mark"> *</span>
            </label>
            <Input.TextArea
              rows={4}
              placeholder={actionModal.type === 'cancel' ? '请输入取消订单的原因' : '请输入退款处理的原因'}
              value={actionModal.reason}
              onChange={(e) =>
                setActionModal((prev) => ({ ...prev, reason: e.target.value, error: '' }))
              }
              disabled={actionModal.submitting}
              maxLength={500}
              showCount
            />
          </div>
        </div>
      </Modal>
    </div>
  )
}

export { getOrderStatusTag, orderStatusMap }
