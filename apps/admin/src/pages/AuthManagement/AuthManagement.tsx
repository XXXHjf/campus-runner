/**
 * 审核管理：学生认证审核
 * - 获取待审核列表：GET /admin/api/auth/pendingList
 * - 更新审核状态：PUT /admin/api/auth/review
 */

import { useEffect, useMemo, useState } from 'react'
import { Alert, App, Avatar, Button, Image, Input, Modal, Space, Table, Tag } from 'antd'
import { authService } from '../../services'
import type { PendingAuthUser, AuthReviewStatus } from '../../types'
import {
  AdminContentCard,
  AdminCount,
  AdminFilterBar,
  AdminPage,
  AdminPageHeader,
  createAdminTableLocale,
} from '../../components/admin'
import './AuthManagement.css'

const statusLabels: Record<number, string> = {
  0: '未审核',
  1: '审核中',
  2: '审核通过',
  3: '审核不通过',
}

const statusColors: Record<number, string> = {
  0: '#6b7280',
  1: '#f59e0b',
  2: '#10b981',
  3: '#ef4444',
}

function guessImageUrl(value?: string) {
  if (!value) return null
  const trimmed = value.trim()
  if (!trimmed) return null
  if (trimmed.startsWith('http://') || trimmed.startsWith('https://')) return trimmed
  if (trimmed.startsWith('data:image/')) return trimmed
  return null
}

function getErrorMessage(err: unknown, fallback: string) {
  if (err instanceof Error) return err.message
  if (err && typeof err === 'object' && 'message' in err) {
    const record = err as Record<string, unknown>
    if (typeof record.message === 'string') return record.message
  }
  return fallback
}

type ConfirmModalState =
  | {
      open: false
    }
  | {
      open: true
      user: PendingAuthUser
      review: AuthReviewStatus
    }

export default function AuthManagement() {
  const { message } = App.useApp()
  const [loading, setLoading] = useState(false)
  const [list, setList] = useState<PendingAuthUser[]>([])
  const [keyword, setKeyword] = useState('')
  const [actionUserId, setActionUserId] = useState<number | null>(null)
  const [confirmModal, setConfirmModal] = useState<ConfirmModalState>({ open: false })
  const [preview, setPreview] = useState<{ open: boolean; url?: string; title?: string }>({
    open: false,
  })
  const [error, setError] = useState<string | null>(null)

  const loadList = async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await authService.getPendingList()
      setList(Array.isArray(data) ? data : [])
    } catch (err: unknown) {
      console.error('获取待审核列表失败', err)
      setError(getErrorMessage(err, '获取待审核列表失败，请稍后重试'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadList()
  }, [])

  const filteredList = useMemo(() => {
    if (!keyword.trim()) return list
    const kw = keyword.trim()
    return list.filter((u) => {
      const fields = [
        String(u.id ?? ''),
        u.username ?? '',
        u.realname ?? '',
        u.phone ?? '',
        u.schoolName ?? '',
        u.stuId ?? '',
      ]
      return fields.some((f) => f.includes(kw))
    })
  }, [list, keyword])

  const updateStatus = async (user: PendingAuthUser, review: AuthReviewStatus) => {
    setActionUserId(user.id)
    try {
      await authService.reviewAuth({ userID: user.id, review })
      setList((prev) => prev.filter((u) => u.id !== user.id))
      setConfirmModal({ open: false })
    } catch (err: unknown) {
      console.error('更新审核状态失败', err)
      message.error(getErrorMessage(err, '更新失败，请稍后重试'))
    } finally {
      setActionUserId(null)
    }
  }

  const openPreview = (user: PendingAuthUser) => {
    const url = guessImageUrl(user.studentIdCard)
    if (!url) return
    setPreview({
      open: true,
      url,
      title: `${user.realname || user.username || user.id} - 证明材料`,
    })
  }

  const columns = [
    {
      title: '用户ID',
      dataIndex: 'id',
      width: 90,
    },
    {
      title: '用户',
      width: 180,
      render: (_: unknown, user: PendingAuthUser) => (
        <div className="auth-user-cell">
          <Avatar src={user.headImg || undefined}>
            {!user.headImg ? (user.username || '?').slice(0, 1).toUpperCase() : undefined}
          </Avatar>
          <span>{user.username || '-'}</span>
        </div>
      ),
    },
    {
      title: '姓名',
      dataIndex: 'realname',
      width: 120,
      render: (value: string | undefined) => value || '-',
    },
    {
      title: '学校',
      dataIndex: 'schoolName',
      width: 180,
      render: (value: string | undefined) => value || '-',
    },
    {
      title: '学号',
      dataIndex: 'stuId',
      width: 130,
      render: (value: string | undefined) => value || '-',
    },
    {
      title: '手机号',
      dataIndex: 'phone',
      width: 140,
      render: (value: string | undefined) => value || '-',
    },
    {
      title: '状态',
      dataIndex: 'studentIdCardReview',
      width: 120,
      render: (value: number | undefined) => {
        const review = value ?? 0
        return <Tag color={statusColors[review]}>{statusLabels[review] || '未知'}</Tag>
      },
    },
    {
      title: '材料',
      width: 100,
      render: (_: unknown, user: PendingAuthUser) => {
        const url = guessImageUrl(user.studentIdCard)
        return url ? (
          <Button type="link" onClick={() => openPreview(user)}>
            查看材料
          </Button>
        ) : (
          '无'
        )
      },
    },
    {
      title: '操作',
      fixed: 'right' as const,
      width: 230,
      render: (_: unknown, user: PendingAuthUser) => {
        const disabled = actionUserId === user.id
        return (
          <Space size={4}>
            <Button
              type="link"
              size="small"
              disabled={disabled}
              onClick={() => setConfirmModal({ open: true, user, review: 1 })}
            >
              标记审核中
            </Button>
            <Button
              type="link"
              size="small"
              disabled={disabled}
              onClick={() => setConfirmModal({ open: true, user, review: 2 })}
            >
              通过
            </Button>
            <Button
              type="link"
              size="small"
              danger
              disabled={disabled}
              onClick={() => setConfirmModal({ open: true, user, review: 3 })}
            >
              不通过
            </Button>
          </Space>
        )
      },
    },
  ]

  return (
    <AdminPage className="auth-management">
      <AdminPageHeader
        title="审核管理"
        description="查看学生认证材料并处理审核申请"
        actions={
          <Button onClick={loadList} loading={loading}>
            刷新
          </Button>
        }
      />

      <AdminFilterBar
        extra={
          <AdminCount>{loading ? '加载中' : `待处理 ${filteredList.length} 条`}</AdminCount>
        }
      >
        <Input.Search
          allowClear
          placeholder="搜索 ID、昵称、姓名、手机号、学校或学号"
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
          onSearch={setKeyword}
          style={{ width: 420 }}
        />
      </AdminFilterBar>

      {error && (
        <Alert type="error" showIcon title={error} />
      )}

      <AdminContentCard flush>
        <Table
          rowKey="id"
          loading={loading}
          dataSource={filteredList}
          columns={columns}
          locale={createAdminTableLocale('暂无待审核数据')}
          scroll={{ x: 1320 }}
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 条`,
          }}
        />
      </AdminContentCard>

      <Modal
        title="确认审核操作"
        open={confirmModal.open}
        onCancel={() => setConfirmModal({ open: false })}
        onOk={() =>
          confirmModal.open && void updateStatus(confirmModal.user, confirmModal.review)
        }
        okText="确认提交"
        cancelText="取消"
        confirmLoading={confirmModal.open && actionUserId === confirmModal.user.id}
      >
        {confirmModal.open && (
          <Space direction="vertical" size={12}>
            <span>
              用户：
              {confirmModal.user.realname ||
                confirmModal.user.username ||
                String(confirmModal.user.id)}
            </span>
            <span>
              设置状态：
              <Tag color={statusColors[confirmModal.review]}>
                {statusLabels[confirmModal.review] || '未知'}
              </Tag>
            </span>
            <span className="auth-modal-note">提交后将更新该用户的学生认证审核状态。</span>
          </Space>
        )}
      </Modal>

      <Modal
        title={preview.title || '材料预览'}
        open={preview.open}
        onCancel={() => setPreview({ open: false })}
        footer={null}
        width={860}
        centered
      >
        {preview.url && <Image src={preview.url} alt="学生认证材料" width="100%" />}
      </Modal>
    </AdminPage>
  )
}
