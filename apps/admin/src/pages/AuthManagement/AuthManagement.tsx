/**
 * 审核管理：学生认证审核
 * - 获取待审核列表：GET /admin/api/auth/pendingList
 * - 更新审核状态：PUT /admin/api/auth/review
 */

import { useEffect, useMemo, useState } from 'react'
import { authService } from '../../services'
import type { PendingAuthUser, AuthReviewStatus } from '../../types'
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
      alert(getErrorMessage(err, '更新失败，请稍后重试'))
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

  return (
    <div className="auth-management">
      <div className="page-header">
        <div className="header-left">
          <h1>审核管理</h1>
          <p>处理学生认证申请（查看材料并审核通过/不通过）</p>
        </div>
        <div className="header-right">
          <button className="btn-secondary" onClick={loadList} disabled={loading}>
            刷新
          </button>
        </div>
      </div>

      <div className="toolbar">
        <div className="toolbar-left">
          <div className="search-box">
            <span className="search-icon">🔍</span>
            <input
              type="text"
              placeholder="搜索：ID/昵称/姓名/手机号/学校/学号"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
            />
          </div>
        </div>
        <div className="toolbar-right">
          <span className="data-count">
            {loading ? '加载中...' : `待处理 ${filteredList.length} 条`}
          </span>
        </div>
      </div>

      {error && (
        <div className="alert alert-error">
          <span>⚠️ {error}</span>
        </div>
      )}

      <div className="table-container">
        <table className="data-table">
          <thead>
            <tr>
              <th style={{ width: 90 }}>用户ID</th>
              <th style={{ width: 140 }}>昵称</th>
              <th style={{ width: 120 }}>姓名</th>
              <th style={{ width: 160 }}>学校</th>
              <th style={{ width: 120 }}>学号</th>
              <th style={{ width: 140 }}>手机号</th>
              <th style={{ width: 120 }}>状态</th>
              <th>材料</th>
              <th style={{ width: 210 }}>操作</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={9} className="no-data">
                  <div className="no-data-content">
                    <span className="no-data-icon">⏳</span>
                    <p>加载中...</p>
                  </div>
                </td>
              </tr>
            ) : filteredList.length === 0 ? (
              <tr>
                <td colSpan={9} className="no-data">
                  <div className="no-data-content">
                    <span className="no-data-icon">📭</span>
                    <p>暂无待审核数据</p>
                    <small>可点击右上角刷新</small>
                  </div>
                </td>
              </tr>
            ) : (
              filteredList.map((user) => {
                const review = user.studentIdCardReview ?? 0
                const url = guessImageUrl(user.studentIdCard)
                const disabled = actionUserId === user.id

                return (
                  <tr key={user.id}>
                    <td>{user.id}</td>
                    <td>
                      <div className="user-cell">
                        <img
                          className="user-avatar"
                          src={
                            user.headImg ||
                            `https://api.dicebear.com/7.x/avataaars/svg?seed=${encodeURIComponent(
                              String(user.id),
                            )}`
                          }
                          alt="头像"
                        />
                        <span className="user-name">{user.username || '-'}</span>
                      </div>
                    </td>
                    <td>{user.realname || '-'}</td>
                    <td>{user.schoolName || '-'}</td>
                    <td>{user.stuId || '-'}</td>
                    <td>{user.phone || '-'}</td>
                    <td>
                      <span
                        className="status-badge"
                        style={{
                          backgroundColor: statusColors[review] || '#6b7280',
                        }}
                      >
                        {statusLabels[review] || '未知'}
                      </span>
                    </td>
                    <td>
                      {url ? (
                        <button className="btn-link" onClick={() => openPreview(user)}>
                          查看材料
                        </button>
                      ) : user.studentIdCard ? (
                        <span className="text-muted">{user.studentIdCard}</span>
                      ) : (
                        <span className="text-muted">无</span>
                      )}
                    </td>
                    <td>
                      <div className="table-actions">
                        <button
                          className="btn-action btn-reviewing"
                          onClick={() => setConfirmModal({ open: true, user, review: 1 })}
                          disabled={disabled}
                          title="标记为审核中"
                        >
                          审核中
                        </button>
                        <button
                          className="btn-action btn-approve"
                          onClick={() => setConfirmModal({ open: true, user, review: 2 })}
                          disabled={disabled}
                        >
                          通过
                        </button>
                        <button
                          className="btn-action btn-reject"
                          onClick={() => setConfirmModal({ open: true, user, review: 3 })}
                          disabled={disabled}
                        >
                          不通过
                        </button>
                      </div>
                    </td>
                  </tr>
                )
              })
            )}
          </tbody>
        </table>
      </div>

      {confirmModal.open && (
        <div className="modal-overlay" onClick={() => setConfirmModal({ open: false })}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>确认审核操作</h2>
              <button className="modal-close" onClick={() => setConfirmModal({ open: false })}>
                ✕
              </button>
            </div>
            <div className="modal-body">
              <div className="confirm-summary">
                <div className="confirm-row">
                  <span className="confirm-label">用户</span>
                  <span className="confirm-value">
                    {confirmModal.user.realname ||
                      confirmModal.user.username ||
                      String(confirmModal.user.id)}
                  </span>
                </div>
                <div className="confirm-row">
                  <span className="confirm-label">设置状态</span>
                  <span
                    className="status-badge"
                    style={{
                      backgroundColor: statusColors[confirmModal.review] || '#6b7280',
                    }}
                  >
                    {statusLabels[confirmModal.review] || '未知'}
                  </span>
                </div>
              </div>
              <p className="text-muted" style={{ marginTop: 12 }}>
                提交后将更新该用户的学生认证审核状态。
              </p>
            </div>
            <div className="modal-footer">
              <button
                className="btn-secondary"
                onClick={() => setConfirmModal({ open: false })}
                disabled={actionUserId === confirmModal.user.id}
              >
                取消
              </button>
              <button
                className="btn-primary"
                onClick={() => void updateStatus(confirmModal.user, confirmModal.review)}
                disabled={actionUserId === confirmModal.user.id}
              >
                确认提交
              </button>
            </div>
          </div>
        </div>
      )}

      {preview.open && (
        <div className="modal-overlay" onClick={() => setPreview({ open: false })}>
          <div className="modal-content modal-preview" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>{preview.title || '材料预览'}</h2>
              <button className="modal-close" onClick={() => setPreview({ open: false })}>
                ✕
              </button>
            </div>
            <div className="modal-body">
              {preview.url ? (
                <img className="preview-image" src={preview.url} alt="材料" />
              ) : (
                <div className="text-muted">无可预览内容</div>
              )}
            </div>
            <div className="modal-footer">
              {preview.url && (
                <a className="btn-secondary" href={preview.url} target="_blank" rel="noreferrer">
                  在新标签页打开
                </a>
              )}
              <button className="btn-primary" onClick={() => setPreview({ open: false })}>
                关闭
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
