/**
 * 轮播图管理
 * - 获取轮播图列表：GET /admin/api/banner/getList/{schoolId}
 * - 新增轮播图：POST /admin/api/banner/add
 * - 删除轮播图：DELETE /admin/api/banner/delete/{id}
 * - 上传临时图片：POST /admin/api/media/images
 */

import { useEffect, useMemo, useState } from 'react'
import { bannerService, addressService, mediaService } from '../../services'
import type { AdminSchool, Banner, BannerCreateRequest, BannerJumpType } from '../../types'
import { formatDateTime } from '../../utils/format'
import './BannerManagement.css'

const jumpTypeLabels: Record<number, string> = {
  0: '无跳转',
  1: '网页链接',
  2: '站内页面',
  3: '小程序页面',
}

const jumpTypeOptions: { value: BannerJumpType; label: string }[] = [
  { value: 0, label: '无跳转' },
  { value: 1, label: '网页链接' },
  { value: 2, label: '站内页面' },
  { value: 3, label: '小程序页面' },
]

function getErrorMessage(err: unknown, fallback: string) {
  if (err instanceof Error) return err.message
  if (err && typeof err === 'object' && 'message' in err) {
    const record = err as Record<string, unknown>
    if (typeof record.message === 'string') return record.message
  }
  return fallback
}

function formatCreateTime(value?: string) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return formatDateTime(date)
}

export default function BannerManagement() {
  const [schools, setSchools] = useState<AdminSchool[]>([])
  const [selectedSchoolId, setSelectedSchoolId] = useState<number>(0)
  const [loading, setLoading] = useState(false)
  const [list, setList] = useState<Banner[]>([])
  const [keyword, setKeyword] = useState('')
  const [error, setError] = useState<string | null>(null)

  const [addModalOpen, setAddModalOpen] = useState(false)
  const [deleteModal, setDeleteModal] = useState<{ open: boolean; banner?: Banner }>({
    open: false,
  })
  const [preview, setPreview] = useState<{ open: boolean; url?: string; title?: string }>({
    open: false,
  })
  const [actionId, setActionId] = useState<number | null>(null)

  const [formState, setFormState] = useState<BannerCreateRequest>({
    title: '',
    imgUrl: '',
    schoolId: 0,
    jumpType: 0,
    jumpTarget: '',
    remark: '',
  })
  const [uploadFile, setUploadFile] = useState<File | null>(null)
  const [localPreview, setLocalPreview] = useState<string | null>(null)
  const [uploading, setUploading] = useState(false)
  const [uploadProgress, setUploadProgress] = useState(0)
  const [addSubmitting, setAddSubmitting] = useState(false)
  const [addMessage, setAddMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(
    null,
  )

  useEffect(() => {
    void loadSchools()
  }, [])

  useEffect(() => {
    void loadList(selectedSchoolId)
  }, [selectedSchoolId])

  useEffect(() => {
    if (!uploadFile) {
      setLocalPreview(null)
      return
    }
    const previewUrl = URL.createObjectURL(uploadFile)
    setLocalPreview(previewUrl)
    return () => URL.revokeObjectURL(previewUrl)
  }, [uploadFile])

  useEffect(() => {
    const hasModal = addModalOpen || deleteModal.open || preview.open
    if (!hasModal) return undefined
    const originalOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => {
      document.body.style.overflow = originalOverflow
    }
  }, [addModalOpen, deleteModal.open, preview.open])

  const loadSchools = async () => {
    try {
      const data = await addressService.getSchools()
      setSchools(Array.isArray(data) ? data : [])
    } catch (err) {
      console.error('获取学校列表失败', err)
    }
  }

  const loadList = async (schoolId: number) => {
    setLoading(true)
    setError(null)
    try {
      const data = await bannerService.getBannerList(schoolId)
      setList(Array.isArray(data) ? data : [])
    } catch (err) {
      console.error('获取轮播图列表失败', err)
      setError(getErrorMessage(err, '获取轮播图列表失败，请稍后重试'))
    } finally {
      setLoading(false)
    }
  }

  const filteredList = useMemo(() => {
    if (!keyword.trim()) return list
    const kw = keyword.trim()
    return list.filter((item) => {
      const fields = [
        String(item.id),
        item.title ?? '',
        item.jumpTarget ?? '',
        item.remark ?? '',
        item.schoolName ?? '',
      ]
      return fields.some((field) => field.includes(kw))
    })
  }, [list, keyword])

  const openAddModal = () => {
    const nextSchoolId = selectedSchoolId ?? 0
    setFormState({
      title: '',
      imgUrl: '',
      imageAssetId: undefined,
      schoolId: nextSchoolId,
      jumpType: 0,
      jumpTarget: '',
      remark: '',
    })
    setUploadFile(null)
    setLocalPreview(null)
    setUploadProgress(0)
    setUploading(false)
    setAddSubmitting(false)
    setAddMessage(null)
    setAddModalOpen(true)
  }

  const closeAddModal = () => {
    if (formState.imageAssetId) {
      void mediaService.releaseTemporaryImage(formState.imageAssetId).catch((err) => {
        console.warn('释放临时轮播图失败，将由服务端定时清理', err)
      })
    }
    setAddModalOpen(false)
  }

  const handleSchoolChange = (value: number) => {
    setFormState((prev) => ({ ...prev, schoolId: value }))
  }

  const handleJumpTypeChange = (value: BannerJumpType) => {
    setFormState((prev) => ({
      ...prev,
      jumpType: value,
      jumpTarget: value === 0 ? '' : prev.jumpTarget,
    }))
  }

  const handleFileSelection = (file: File | null) => {
    if (formState.imageAssetId) {
      void mediaService.releaseTemporaryImage(formState.imageAssetId).catch((err) => {
        console.warn('释放已替换的临时轮播图失败，将由服务端定时清理', err)
      })
    }
    setFormState((prev) => ({
      ...prev,
      imgUrl: '',
      imageAssetId: undefined,
    }))
    setUploadFile(file)
    setUploadProgress(0)
    setAddMessage(null)
  }

  const handleUpload = async () => {
    if (!uploadFile) {
      setAddMessage({ type: 'error', text: '请先选择需要上传的图片' })
      return
    }
    setUploading(true)
    setUploadProgress(0)
    setAddMessage(null)
    try {
      const result = await mediaService.uploadImage(uploadFile, 'BANNER', setUploadProgress)
      if (formState.imageAssetId) {
        await mediaService.releaseTemporaryImage(formState.imageAssetId).catch((err) => {
          console.warn('释放已替换的临时轮播图失败，将由服务端定时清理', err)
        })
      }
      setFormState((prev) => ({
        ...prev,
        imgUrl: result.previewUrl,
        imageAssetId: result.mediaId,
      }))
      setAddMessage({ type: 'success', text: '图片上传成功，请点击“保存”完成新增' })
    } catch (err) {
      console.error('上传图片失败', err)
      setAddMessage({ type: 'error', text: getErrorMessage(err, '上传失败，请稍后重试') })
    } finally {
      setUploading(false)
    }
  }

  const handleCreate = async () => {
    if (!formState.title.trim()) {
      setAddMessage({ type: 'error', text: '请填写轮播图标题' })
      return
    }
    if (!formState.imageAssetId) {
      setAddMessage({ type: 'error', text: '请先上传图片' })
      return
    }
    if (formState.jumpType !== 0 && !formState.jumpTarget.trim()) {
      setAddMessage({ type: 'error', text: '请填写跳转目标' })
      return
    }
    setAddSubmitting(true)
    setAddMessage(null)
    try {
      await bannerService.addBanner({
        ...formState,
        title: formState.title.trim(),
        jumpTarget: formState.jumpTarget.trim(),
        remark: formState.remark.trim(),
        imgUrl: '',
      })
      await loadList(selectedSchoolId)
      setFormState((prev) => ({ ...prev, imageAssetId: undefined }))
      setAddModalOpen(false)
    } catch (err) {
      console.error('新增轮播图失败', err)
      setAddMessage({ type: 'error', text: getErrorMessage(err, '新增失败，请稍后重试') })
    } finally {
      setAddSubmitting(false)
    }
  }

  const handleDelete = async (banner: Banner) => {
    setActionId(banner.id)
    try {
      await bannerService.deleteBanner(banner.id)
      await loadList(selectedSchoolId)
      setDeleteModal({ open: false })
    } catch (err) {
      console.error('删除轮播图失败', err)
      alert(getErrorMessage(err, '删除失败，请稍后再试'))
    } finally {
      setActionId(null)
    }
  }

  const schoolLabel = (item: Banner) => {
    if (item.schoolId === 0) return '通用'
    return item.schoolName || `学校ID ${item.schoolId}`
  }

  const openPreview = (item: Banner) => {
    if (!item.imgUrl) return
    setPreview({
      open: true,
      url: item.imgUrl,
      title: item.title ? `${item.title} - 轮播图预览` : '轮播图预览',
    })
  }

  const selectedSchoolName = schools.find((school) => school.id === formState.schoolId)?.schoolName
  const listSchoolName = schools.find((school) => school.id === selectedSchoolId)?.schoolName
  const listTitle =
    selectedSchoolId === 0
      ? '通用轮播图列表'
      : `${listSchoolName || `学校ID ${selectedSchoolId}`} 轮播图列表`

  return (
    <div className="banner-management">
      <div className="page-header">
        <div className="header-left">
          <h1>轮播图管理</h1>
          <p>管理小程序首页轮播图（支持上传图片并配置跳转）</p>
        </div>
        <div className="header-right">
          <button className="btn-secondary" onClick={() => loadList(selectedSchoolId)} disabled={loading}>
            刷新
          </button>
          <button className="btn-primary" onClick={openAddModal}>
            <span className="btn-icon">＋</span>
            新增轮播图
          </button>
        </div>
      </div>

      <div className="toolbar">
        <div className="toolbar-left">
          <div className="filter-group">
            <label>查看学校</label>
            <select
              value={selectedSchoolId}
              onChange={(e) => setSelectedSchoolId(Number(e.target.value))}
            >
              <option value={0}>通用轮播图</option>
              {schools.map((school) => (
                <option key={school.id} value={school.id}>
                  {school.schoolName}
                </option>
              ))}
            </select>
          </div>
          <div className="search-box">
            <span className="search-icon">🔍</span>
            <input
              type="text"
              placeholder="搜索：标题/跳转/备注/学校"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
            />
          </div>
        </div>
      </div>

      {error && (
        <div className="alert alert-error">
          <span>⚠️ {error}</span>
        </div>
      )}

      <div className="table-container">
        <div className="table-header">
          <div>
            <h3 className="table-title">{listTitle}</h3>
            <p className="table-subtitle">表格展示当前筛选条件下的轮播图</p>
          </div>
          <span className="data-count">{loading ? '加载中...' : `共 ${filteredList.length} 条`}</span>
        </div>
        <table className="data-table">
          <thead>
            <tr>
              <th style={{ width: 80 }}>ID</th>
              <th style={{ width: 130 }}>图片</th>
              <th style={{ width: 160 }}>标题</th>
              <th style={{ width: 160 }}>学校</th>
              <th style={{ width: 120 }}>跳转类型</th>
              <th>跳转目标</th>
              <th style={{ width: 180 }}>备注</th>
              <th style={{ width: 180 }}>创建时间</th>
              <th style={{ width: 120 }}>操作</th>
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
                    <p>暂无轮播图数据</p>
                    <small>可点击右上角新增轮播图</small>
                  </div>
                </td>
              </tr>
            ) : (
              filteredList.map((item) => (
                <tr key={item.id}>
                  <td>{item.id}</td>
                  <td>
                    {item.imgUrl ? (
                      <button
                        type="button"
                        className="banner-thumb-button"
                        onClick={() => openPreview(item)}
                      >
                        <img
                          className="banner-thumb"
                          src={item.imgUrl}
                          alt={item.title || 'banner'}
                        />
                      </button>
                    ) : (
                      <span className="text-muted">暂无图片</span>
                    )}
                  </td>
                  <td>{item.title || '-'}</td>
                  <td>{schoolLabel(item)}</td>
                  <td>
                    <span className="tag">{jumpTypeLabels[item.jumpType] || '-'}</span>
                  </td>
                  <td className="text-ellipsis">{item.jumpTarget || '-'}</td>
                  <td className="text-ellipsis">{item.remark || '-'}</td>
                  <td>{formatCreateTime(item.createTime)}</td>
                  <td>
                    <button
                      className="btn-action btn-delete"
                      onClick={() => setDeleteModal({ open: true, banner: item })}
                      disabled={actionId === item.id}
                    >
                      删除
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {addModalOpen && (
        <div className="modal-overlay" onClick={closeAddModal}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>新增轮播图</h2>
              <button className="modal-close" onClick={closeAddModal}>
                ✕
              </button>
            </div>
            <div className="modal-body">
              {addMessage && (
                <div className={`alert ${addMessage.type === 'success' ? 'alert-success' : 'alert-error'}`}>
                  {addMessage.text}
                </div>
              )}

              <div className="form-group">
                <label>
                  标题<span className="required">*</span>
                </label>
                <input
                  type="text"
                  value={formState.title}
                  onChange={(e) => setFormState((prev) => ({ ...prev, title: e.target.value }))}
                  placeholder="请输入轮播图标题"
                />
              </div>

              <div className="form-group">
                <label>
                  归属学校<span className="required">*</span>
                </label>
                <select
                  value={formState.schoolId}
                  onChange={(e) => handleSchoolChange(Number(e.target.value))}
                >
                  <option value={0}>通用轮播图</option>
                  {schools.map((school) => (
                    <option key={school.id} value={school.id}>
                      {school.schoolName}
                    </option>
                  ))}
                </select>
                {formState.schoolId !== 0 && !selectedSchoolName && (
                  <p className="form-hint">学校列表加载中，请稍后再上传</p>
                )}
              </div>

              <div className="form-group">
                <label>
                  图片上传<span className="required">*</span>
                </label>
                <div className="upload-row">
                  <input
                    type="file"
                    accept="image/*"
                    onChange={(e) => handleFileSelection(e.target.files?.[0] || null)}
                  />
                  <button
                    className="btn-secondary"
                    onClick={handleUpload}
                    disabled={uploading}
                  >
                    {uploading ? `上传中 ${uploadProgress}%` : '上传图片'}
                  </button>
                </div>
                {(formState.imgUrl || localPreview) && (
                  <div className="image-preview">
                    <img
                      src={formState.imgUrl || localPreview || ''}
                      alt="预览"
                      className="preview-img"
                    />
                  </div>
                )}
              </div>

              <div className="form-group">
                <label>跳转类型</label>
                <select
                  value={formState.jumpType}
                  onChange={(e) => handleJumpTypeChange(Number(e.target.value) as BannerJumpType)}
                >
                  {jumpTypeOptions.map((item) => (
                    <option key={item.value} value={item.value}>
                      {item.label}
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label>跳转目标</label>
                <input
                  type="text"
                  value={formState.jumpTarget}
                  onChange={(e) => setFormState((prev) => ({ ...prev, jumpTarget: e.target.value }))}
                  placeholder="请输入所选跳转类型对应的地址"
                  disabled={formState.jumpType === 0}
                />
              </div>

              <div className="form-group">
                <label>备注说明</label>
                <input
                  type="text"
                  value={formState.remark}
                  onChange={(e) => setFormState((prev) => ({ ...prev, remark: e.target.value }))}
                  placeholder="可选，补充说明"
                />
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn-secondary" onClick={closeAddModal}>
                取消
              </button>
              <button className="btn-primary" onClick={handleCreate} disabled={addSubmitting}>
                保存
              </button>
            </div>
          </div>
        </div>
      )}

      {deleteModal.open && (
        <div className="modal-overlay" onClick={() => setDeleteModal({ open: false })}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>删除确认</h2>
              <button className="modal-close" onClick={() => setDeleteModal({ open: false })}>
                ✕
              </button>
            </div>
            <div className="modal-body">
              <p>确认删除该轮播图吗？删除后不可恢复。</p>
            </div>
            <div className="modal-footer">
              <button className="btn-secondary" onClick={() => setDeleteModal({ open: false })}>
                取消
              </button>
              <button
                className="btn-primary"
                onClick={() => deleteModal.banner && handleDelete(deleteModal.banner)}
                disabled={actionId === deleteModal.banner?.id}
              >
                确认删除
              </button>
            </div>
          </div>
        </div>
      )}

      {preview.open && (
        <div className="modal-overlay" onClick={() => setPreview({ open: false })}>
          <div className="modal-content modal-preview" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>{preview.title || '轮播图预览'}</h2>
              <button className="modal-close" onClick={() => setPreview({ open: false })}>
                ✕
              </button>
            </div>
            <div className="modal-body">
              {preview.url ? (
                <img className="preview-image" src={preview.url} alt="轮播图" />
              ) : (
                <div className="text-muted">暂无可预览图片</div>
              )}
            </div>
            <div className="modal-footer">
              {preview.url && (
                <a className="btn-secondary" href={preview.url} download>
                  下载
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
