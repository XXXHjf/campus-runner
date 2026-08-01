/**
 * 接单管理页面
 * 3 个 Tab：全部接单 / 未收款订单 / 接单统计
 * 基于 URL path 切换 Tab
 */

import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import {
  Tabs,
  Table,
  Input,
  Button,
  Tag,
  Card,
  Statistic,
  Row,
  Col,
  Modal,
  Image,
  App,
} from 'antd'
import type { AdminTakeOrderItem, AdminTakeOrderStatistics, AdminOrderDetailResponse } from '../../types/admin'
import { takeOrderService, orderService } from '../../services'
import OrderDetail from '../OrderManagement/OrderDetail'
import { TAKE_ORDER_STATUS_LABELS, TAKE_ORDER_STATUS_COLORS } from '../../constants'
import { formatDateTime, formatPrice, formatPhone } from '../../utils/format'
import {
  AdminContentCard,
  AdminCount,
  AdminFilterBar,
  AdminPage,
  AdminPageHeader,
  createAdminTableLocale,
} from '../../components/admin'
import './TakeOrderManagement.css'

/** 接单状态标签渲染 */
function getTakeOrderStatusTag(status: number) {
  const label = TAKE_ORDER_STATUS_LABELS[status as keyof typeof TAKE_ORDER_STATUS_LABELS]
  const color = TAKE_ORDER_STATUS_COLORS[status as keyof typeof TAKE_ORDER_STATUS_COLORS]
  if (label && color) {
    return <Tag color={color}>{label}</Tag>
  }
  return <Tag color="default">未知({status})</Tag>
}

/** 提现状态映射 */
const WITHDRAWAL_STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '未提现', color: '#9ca3af' },
  1: { label: '已提现', color: '#10b981' },
  2: { label: '提现失败', color: '#dc2626' },
}

function getWithdrawalStatusTag(status: number | null | undefined) {
  if (status === null || status === undefined) {
    return <Tag color="default">-</Tag>
  }
  const entry = WITHDRAWAL_STATUS_MAP[status]
  if (entry) {
    return <Tag color={entry.color}>{entry.label}</Tag>
  }
  return <Tag color="default">未知({status})</Tag>
}

/** Tab 配置映射 */
const TAB_CONFIG: Record<
  string,
  {
    label: string
    api: (page: number, pageSize: number) => Promise<{ total: number; list: AdminTakeOrderItem[] }>
  }
> = {
  all: {
    label: '全部接单',
    api: (page, pageSize) => takeOrderService.listAll(page, pageSize),
  },
  withdrawn: {
    label: '未收款订单',
    api: (page, pageSize) => takeOrderService.listUnpaid(page, pageSize),
  },
}

type TabKey = 'all' | 'withdrawn' | 'stats'

const PAGE_SIZE = 20

export default function TakeOrderManagement() {
  const { message } = App.useApp()
  const navigate = useNavigate()
  const location = useLocation()

  // Derive active tab key from URL path
  const pathSegments = location.pathname.split('/').filter(Boolean)
  const tabKey: TabKey = (pathSegments[pathSegments.length - 1] as TabKey) || 'all'

  // List tab state
  const [loading, setLoading] = useState(false)
  const [list, setList] = useState<AdminTakeOrderItem[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1)
  const [keyword, setKeyword] = useState('')

  // Order detail modal state
  const [detailModal, setDetailModal] = useState<{
    open: boolean
    loading: boolean
    data: AdminOrderDetailResponse | null
  }>({ open: false, loading: false, data: null })

  // Image preview modal state
  const [imagePreview, setImagePreview] = useState<{ open: boolean; url: string }>({
    open: false,
    url: '',
  })

  // Stats state
  const [statsLoading, setStatsLoading] = useState(false)
  const [stats, setStats] = useState<AdminTakeOrderStatistics | null>(null)

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
      console.error('获取接单列表失败', err)
      message.error('获取接单列表失败')
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
      const data = await takeOrderService.statistics()
      setStats(data)
    } catch (err) {
      console.error('获取接单统计失败', err)
      message.error('获取接单统计失败')
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
        item.publisherName.toLowerCase().includes(kw) ||
        item.takerName.toLowerCase().includes(kw) ||
        item.takerPhone.includes(kw),
    )
  }, [list, keyword])

  // Handle tab change
  const onTabChange = (key: string) => {
    navigate(`/takes/${key}`, { replace: true })
  }

  // Handle pagination change
  const onPageChange = (newPage: number) => {
    setPage(newPage)
  }

  // Open order detail modal
  const openOrderDetail = async (orderId: number) => {
    setDetailModal({ open: true, loading: true, data: null })
    try {
      const data = await orderService.detail(orderId)
      setDetailModal({ open: true, loading: false, data })
    } catch (err) {
      console.error('获取订单详情失败', err)
      message.error('获取订单详情失败')
      setDetailModal({ open: false, loading: false, data: null })
    }
  }

  // Base columns for all list tabs ("all" and "withdrawn")
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
      title: '接单状态',
      dataIndex: 'takeOrderStatus',
      key: 'takeOrderStatus',
      width: 100,
      render: (status: number) => getTakeOrderStatusTag(status),
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
      title: '订单说明',
      dataIndex: 'orderNote',
      key: 'orderNote',
      width: 150,
      ellipsis: true,
      render: (orderNote: string) => orderNote || '-',
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
      title: '发单人',
      dataIndex: 'publisherName',
      key: 'publisherName',
      width: 100,
    },
    {
      title: '发单电话',
      dataIndex: 'publisherPhone',
      key: 'publisherPhone',
      width: 130,
      render: (phone: string) => formatPhone(phone),
    },
    {
      title: '接单人',
      dataIndex: 'takerName',
      key: 'takerName',
      width: 100,
      render: (takerName: string) => takerName || '-',
    },
    {
      title: '接单电话',
      dataIndex: 'takerPhone',
      key: 'takerPhone',
      width: 130,
      render: (phone: string) => (phone ? formatPhone(phone) : '-'),
    },
    {
      title: '接单时间',
      dataIndex: 'takeOrderTime',
      key: 'takeOrderTime',
      width: 170,
      render: (time: string) => formatDateTime(time),
    },
    {
      title: '送达时间',
      dataIndex: 'deliveryTime',
      key: 'deliveryTime',
      width: 170,
      render: (time: string | null) => (time ? formatDateTime(time) : '-'),
    },
    {
      title: '操作',
      key: 'action',
      width: 120,
      fixed: 'right' as const,
      render: (_: unknown, record: AdminTakeOrderItem) => (
        <div className="action-buttons">
          <Button type="link" size="small" onClick={() => openOrderDetail(record.orderId)}>
            查看订单
          </Button>
          {record.takeOrderImage && (
            <Button type="link" size="small" onClick={() => setImagePreview({ open: true, url: record.takeOrderImage! })}>
              查看图片
            </Button>
          )}
        </div>
      ),
    },
  ]

  // Extra columns for the "withdrawn" (unpaid) tab
  const unpaidExtraColumns = [
    {
      title: '服务费',
      dataIndex: 'serviceFee',
      key: 'serviceFee',
      width: 90,
      render: (fee: number | undefined) => (fee !== undefined ? formatPrice(fee) : '-'),
    },
    {
      title: '支付金额',
      dataIndex: 'payAmount',
      key: 'payAmount',
      width: 100,
      render: (amount: number | undefined) => (amount !== undefined ? formatPrice(amount) : '-'),
    },
    {
      title: '完成时间',
      dataIndex: 'completeTime',
      key: 'completeTime',
      width: 170,
      render: (time: string | undefined) => (time ? formatDateTime(time) : '-'),
    },
    {
      title: '提现状态',
      dataIndex: 'withdrawalStatus',
      key: 'withdrawalStatus',
      width: 100,
      render: (status: number | null | undefined) => getWithdrawalStatusTag(status),
    },
  ]

  const columns = useMemo(() => {
    if (tabKey === 'withdrawn') {
      // Insert unpaid extra columns before the action column
      const actionCol = baseColumns[baseColumns.length - 1]
      return [...baseColumns.slice(0, -1), ...unpaidExtraColumns, actionCol]
    }
    return baseColumns
  }, [tabKey])

  return (
    <AdminPage className="take-order-management">
      <AdminPageHeader title="接单管理" description="管理平台接单信息与状态" />

      <Tabs
        activeKey={tabKey}
        onChange={onTabChange}
        className="take-order-tabs"
        items={[
          { key: 'all', label: '全部接单' },
          { key: 'withdrawn', label: '未收款订单' },
          { key: 'stats', label: '接单统计' },
        ]}
      />

      {isListTab ? (
        <>
          <AdminFilterBar extra={<AdminCount>共 {total} 条</AdminCount>}>
            <Input.Search
              placeholder="搜索订单编号、发单人、接单人或联系电话"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              onSearch={(value) => setKeyword(value)}
              style={{ width: 400 }}
              allowClear
            />
          </AdminFilterBar>

          <AdminContentCard flush>
            <Table
              dataSource={filteredList}
              columns={columns}
              rowKey="id"
              loading={loading}
              locale={createAdminTableLocale('暂无接单数据')}
              scroll={{ x: 2200 }}
              pagination={{
                current: page,
                pageSize: PAGE_SIZE,
                total,
                showSizeChanger: false,
                showTotal: (t: number) => `共 ${t} 条`,
                onChange: onPageChange,
              }}
            />
          </AdminContentCard>
        </>
      ) : (
        <div className="stats-container">
          <Row gutter={[24, 24]}>
            <Col xs={24} sm={12} lg={8}>
              <Card loading={statsLoading} className="stats-card">
                <Statistic title="接单总数" value={stats?.totalCount ?? '-'} />
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={8}>
              <Card loading={statsLoading} className="stats-card stats-card-new">
                <Statistic title="今日新增" value={stats?.todayNewCount ?? '-'} />
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={8}>
              <Card loading={statsLoading} className="stats-card stats-card-unpaid">
                <Statistic title="未收款" value={stats?.unpaidCount ?? '-'} />
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={8}>
              <Card loading={statsLoading} className="stats-card stats-card-amount">
                <Statistic
                  title="未收款总额"
                  value={stats?.unpaidTotalAmount ? formatPrice(stats.unpaidTotalAmount) : '-'}
                />
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={8}>
              <Card loading={statsLoading} className="stats-card stats-card-completed">
                <Statistic title="今日送达" value={stats?.todayCompletedCount ?? '-'} />
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={8}>
              <Card loading={statsLoading} className="stats-card stats-card-revenue">
                <Statistic
                  title="今日送达金额"
                  value={stats?.todayCompletedAmount ? formatPrice(stats.todayCompletedAmount) : '-'}
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
        footer={<Button onClick={() => setDetailModal({ open: false, loading: false, data: null })}>关闭</Button>}
        width={900}
        destroyOnHidden
      >
        <OrderDetail detail={detailModal.data} loading={detailModal.loading} />
      </Modal>

      {/* Image Preview Modal */}
      <Modal
        title="图片预览"
        open={imagePreview.open}
        onCancel={() => setImagePreview({ open: false, url: '' })}
        footer={null}
        width={600}
        destroyOnHidden
        centered
      >
        <div style={{ textAlign: 'center', padding: 16 }}>
          <Image
            src={imagePreview.url}
            style={{ maxWidth: '100%' }}
          />
        </div>
      </Modal>
    </AdminPage>
  )
}
