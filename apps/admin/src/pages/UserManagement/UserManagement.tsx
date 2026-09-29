/**
 * 用户管理页面
 * 4 个 Tab：全部用户 / 已认证 / 待审核 / 用户统计
 * 基于 URL path 切换 Tab
 */

import { useEffect, useRef, useState } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import {
  Tabs,
  Table,
  Input,
  Button,
  Drawer,
  Modal,
  Descriptions,
  Tag,
  Avatar,
  Image,
  Empty,
  Alert,
  Card,
  Statistic,
  Row,
  Col,
  App,
} from 'antd'
import type { AdminUserItem, AdminUserDetail, AdminUserStatistics } from '../../types/admin'
import { AuthReviewStatus } from '../../types/auth'
import { authService, userService } from '../../services'
import { GENDER_LABELS } from '../../constants'
import { formatDateTime, formatPhone, formatPrice } from '../../utils/format'
import {
  AdminContentCard,
  AdminCount,
  AdminFilterBar,
  AdminPage,
  AdminPageHeader,
  createAdminTableLocale,
} from '../../components/admin'
import './UserManagement.css'

/** Tab 配置映射 */
const TAB_CONFIG: Record<string, { label: string; api: (page: number, pageSize: number, keyword: string) => Promise<{ total: number; list: AdminUserItem[] }> }> = {
  all: {
    label: '全部用户',
    api: (page, pageSize, keyword) => userService.listAllUsers(page, pageSize, keyword),
  },
  authenticated: {
    label: '已认证',
    api: (page, pageSize, keyword) => userService.listAuthenticated(page, pageSize, keyword),
  },
  'pending-auth': {
    label: '待审核',
    api: (page, pageSize, keyword) => userService.listPendingReview(page, pageSize, keyword),
  },
}

type TabKey = 'all' | 'authenticated' | 'pending-auth' | 'stats'

const PAGE_SIZE = 20

const REVIEW_STATUS = {
  [AuthReviewStatus.UNREVIEWED]: { label: '未审核', color: 'default' },
  [AuthReviewStatus.REVIEWING]: { label: '审核中', color: 'processing' },
  [AuthReviewStatus.APPROVED]: { label: '已通过', color: 'success' },
  [AuthReviewStatus.REJECTED]: { label: '未通过', color: 'error' },
}

export default function UserManagement() {
  const { message } = App.useApp()
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
  const [searchKeyword, setSearchKeyword] = useState('')
  const listRequestId = useRef(0)

  // Detail drawer state
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [detailLoading, setDetailLoading] = useState(false)
  const [detail, setDetail] = useState<AdminUserDetail | null>(null)
  const detailRequestId = useRef(0)
  const [studentCardError, setStudentCardError] = useState(false)
  const [reviewAction, setReviewAction] = useState<'approve' | 'reject' | null>(null)
  const [rejectReason, setRejectReason] = useState('')
  const [reviewError, setReviewError] = useState('')
  const [reviewSubmitting, setReviewSubmitting] = useState(false)

  // Stats state
  const [statsLoading, setStatsLoading] = useState(false)
  const [stats, setStats] = useState<AdminUserStatistics | null>(null)

  const isListTab = tabKey !== 'stats'

  // Load list data
  const loadList = async (currentTab: string) => {
    const config = TAB_CONFIG[currentTab]
    if (!config) return
    const requestId = ++listRequestId.current
    setLoading(true)
    try {
      const res = await config.api(page, PAGE_SIZE, searchKeyword)
      if (requestId !== listRequestId.current) return
      setList(res.list)
      setTotal(res.total)
    } catch (err) {
      if (requestId !== listRequestId.current) return
      console.error('获取用户列表失败', err)
      message.error('获取用户列表失败')
      setList([])
      setTotal(0)
    } finally {
      if (requestId === listRequestId.current) setLoading(false)
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
  }, [page, tabKey, searchKeyword])

  useEffect(() => {
    const timer = window.setTimeout(() => {
      setPage(1)
      setSearchKeyword(keyword.trim())
    }, 300)
    return () => window.clearTimeout(timer)
  }, [keyword])

  useEffect(() => {
    if (!isListTab) {
      loadStats()
    }
  }, [tabKey])

  // Reset pagination when switching tabs
  useEffect(() => {
    setPage(1)
    setKeyword('')
    setSearchKeyword('')
  }, [tabKey])

  // Open detail drawer
  const openDetail = async (id: number) => {
    const requestId = ++detailRequestId.current
    setDrawerOpen(true)
    setDetailLoading(true)
    setDetail(null)
    setStudentCardError(false)
    setReviewAction(null)
    try {
      const data = await userService.userDetail(id)
      if (requestId === detailRequestId.current) setDetail(data)
    } catch (err) {
      if (requestId !== detailRequestId.current) return
      console.error('获取用户详情失败', err)
      message.error('获取用户详情失败')
    } finally {
      if (requestId === detailRequestId.current) setDetailLoading(false)
    }
  }

  const submitReview = async () => {
    if (!detail || !reviewAction) return
    const reason = rejectReason.trim()
    if (reviewAction === 'reject' && (!reason || Array.from(reason).length > 100)) {
      setReviewError('请填写1至100字的驳回原因')
      return
    }
    if (detail.authReviewVersion == null) {
      message.error('申请信息未加载完整，请刷新后重试')
      return
    }
    setReviewSubmitting(true)
    try {
      await authService.reviewAuth({
        userID: detail.id,
        authReviewVersion: detail.authReviewVersion,
        review: reviewAction === 'approve' ? AuthReviewStatus.APPROVED : AuthReviewStatus.REJECTED,
        ...(reviewAction === 'reject' ? { studentIdCardRejectReason: reason } : {}),
      })
      message.success(reviewAction === 'approve' ? '已通过认证' : '已驳回申请')
      setReviewAction(null)
      setDrawerOpen(false)
      if (page > 1 && list.length === 1) setPage(page - 1)
      else void loadList(tabKey)
    } catch (err) {
      const errorMessage = err && typeof err === 'object' && 'message' in err
        && typeof err.message === 'string' ? err.message : '审核失败，请稍后重试'
      message.error(errorMessage)
      if (errorMessage.includes('状态已更新')) {
        setReviewAction(null)
        void openDetail(detail.id)
        void loadList(tabKey)
      }
    } finally {
      setReviewSubmitting(false)
    }
  }

  // Handle tab change
  const onTabChange = (key: string) => {
    navigate(`/users/${key}`, { replace: true })
  }

  // Handle pagination change
  const onPageChange = (newPage: number) => {
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

  // Keep the review queue compact; supporting details and actions stay in the drawer.
  const pendingColumns = [
    {
      title: '用户',
      key: 'user',
      render: (_: unknown, user: AdminUserItem) => (
        <div className="review-user-cell">
          <Avatar src={user.headImg || undefined} size={32} alt="头像" />
          <div><div>{user.username || '-'}</div><span>ID: {user.id}</span></div>
        </div>
      ),
    },
    { title: '姓名', dataIndex: 'realname', key: 'realname', render: (name: string) => name || '-' },
    { title: '学校', dataIndex: 'schoolName', key: 'schoolName', render: (name: string) => name || '-' },
    {
      title: '最近更新', dataIndex: 'updateTime', key: 'updateTime', width: 170,
      render: (value: string) => value ? formatDateTime(value) : '-',
    },
    {
      title: '操作', key: 'review', width: 110,
      render: (_: unknown, user: AdminUserItem) => (
        <Button type="link" onClick={() => openDetail(user.id)}>查看资料</Button>
      ),
    },
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
    <AdminPage className="user-management">
      <AdminPageHeader title="用户管理" description="管理平台用户信息与认证状态" />

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
          <AdminFilterBar extra={<AdminCount>共 {total} 条</AdminCount>}>
            <Input.Search
              placeholder="搜索昵称、姓名、手机号、学校或 ID"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              onSearch={(value) => {
                setPage(1)
                setSearchKeyword(value.trim())
              }}
              style={{ width: 360 }}
              allowClear
            />
          </AdminFilterBar>

          <AdminContentCard flush>
            <Table
              dataSource={list}
              columns={columns}
              rowKey="id"
              loading={loading}
              locale={createAdminTableLocale(tabKey === 'pending-auth'
                ? (searchKeyword ? <span>没有符合条件的申请 <Button type="link" onClick={() => setKeyword('')}>清空搜索</Button></span> : '暂无待审核申请')
                : '暂无用户数据')}
              scroll={{ x: tabKey === 'pending-auth' ? 760 : 1400 }}
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
        rootClassName="user-management"
        title={tabKey === 'pending-auth' ? '认证资料' : '用户详情'}
        placement="right"
        size={560}
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        loading={detailLoading}
        footer={tabKey === 'pending-auth' && detail?.studentIdCardReview === AuthReviewStatus.REVIEWING ? (
          <div className="review-drawer-actions">
            <Button onClick={() => { setRejectReason(''); setReviewError(''); setReviewAction('reject') }}>驳回</Button>
            <Button type="primary" onClick={() => setReviewAction('approve')}>通过</Button>
          </div>
        ) : undefined}
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
                <Tag color={REVIEW_STATUS[detail.studentIdCardReview as AuthReviewStatus]?.color ?? 'default'}>
                  {REVIEW_STATUS[detail.studentIdCardReview as AuthReviewStatus]?.label ?? '未知状态'}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="学生证照片" span={2}>
                {detail.studentIdCard ? (
                  studentCardError ? (
                    <Alert
                      type="warning"
                      showIcon
                      title="材料加载失败，请刷新后重试"
                      action={<Button size="small" onClick={() => openDetail(detail.id)}>刷新</Button>}
                    />
                  ) : (
                    <Image
                      src={detail.studentIdCard}
                      alt="学生证照片"
                      width="100%"
                      styles={{ image: { maxHeight: 240, objectFit: 'contain' } }}
                      preview={{ mask: '查看大图' }}
                      onError={() => setStudentCardError(true)}
                    />
                  )
                ) : (
                  <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无认证材料" />
                )}
              </Descriptions.Item>
            </Descriptions>

            {tabKey !== 'pending-auth' && <Descriptions title="数据统计" column={2} bordered size="small" className="detail-descriptions">
              <Descriptions.Item label="发单数">{detail.orderCount}</Descriptions.Item>
              <Descriptions.Item label="接单数">{detail.takeOrderCount}</Descriptions.Item>
              <Descriptions.Item label="累计收入" span={2}>
                <span style={{ fontWeight: 600, color: '#52c41a' }}>
                  {formatPrice(detail.totalEarned)}
                </span>
              </Descriptions.Item>
            </Descriptions>}
          </>
        )}
      </Drawer>

      <Modal
        rootClassName="user-management"
        title={reviewAction === 'reject' ? '驳回认证申请' : '确认通过认证'}
        open={reviewAction !== null}
        onCancel={() => { if (!reviewSubmitting) setReviewAction(null) }}
        onOk={() => void submitReview()}
        okText={reviewAction === 'reject' ? '确认驳回' : '确认通过'}
        okButtonProps={{ danger: reviewAction === 'reject' }}
        confirmLoading={reviewSubmitting}
        cancelButtonProps={{ disabled: reviewSubmitting }}
        closable={!reviewSubmitting}
        maskClosable={!reviewSubmitting}
      >
        {reviewAction === 'reject' ? (
          <div>
            <label htmlFor="review-reject-reason">驳回原因（将展示给申请人）</label>
            <Input.TextArea id="review-reject-reason" rows={3} value={rejectReason}
              placeholder="请说明需要修改的内容"
              count={{ show: true, max: 100, strategy: (value) => Array.from(value).length }}
              status={reviewError ? 'error' : undefined}
              onChange={(event) => { setRejectReason(event.target.value); setReviewError('') }} />
            {reviewError && <div className="review-reason-error" role="alert">{reviewError}</div>}
          </div>
        ) : <p>确认通过{detail?.realname || detail?.username || '该用户'}的认证申请？</p>}
      </Modal>
    </AdminPage>
  )
}
