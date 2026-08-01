/**
 * 轮播图管理
 * - 获取轮播图列表：GET /admin/api/banner/getList/{schoolId}
 * - 新增轮播图：POST /admin/api/banner/add
 * - 删除轮播图：DELETE /admin/api/banner/delete/{id}
 * - 上传临时图片：POST /admin/api/media/images
 */

import { useEffect, useMemo, useRef, useState } from 'react'
import {
  Alert,
  Button,
  Form,
  Image,
  Input,
  Modal,
  Progress,
  Select,
  Slider,
  Space,
  Table,
  Tag,
  Upload,
} from 'antd'
import { PlusOutlined, ReloadOutlined, UploadOutlined } from '@ant-design/icons'
import Cropper, { type Area, type Point } from 'react-easy-crop'
import { bannerService, addressService, mediaService } from '../../services'
import type { AdminSchool, Banner, BannerCreateRequest, BannerJumpType } from '../../types'
import { BANNER_ASPECT_RATIO, createBannerCropFile } from '../../utils/cropImage'
import { formatDateTime } from '../../utils/format'
import {
  AdminContentCard,
  AdminCount,
  AdminFilterBar,
  AdminPage,
  AdminPageHeader,
  createAdminTableLocale,
} from '../../components/admin'
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
  const [cropFile, setCropFile] = useState<File | null>(null)
  const [cropSource, setCropSource] = useState<string | null>(null)
  const [crop, setCrop] = useState<Point>({ x: 0, y: 0 })
  const [zoom, setZoom] = useState(1)
  const [croppedArea, setCroppedArea] = useState<Area | null>(null)
  const [cropping, setCropping] = useState(false)
  const [addSubmitting, setAddSubmitting] = useState(false)
  const [addMessage, setAddMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(
    null,
  )
  const uploadRequestIdRef = useRef(0)
  const temporaryMediaIdRef = useRef<number | null>(null)

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
    if (!cropFile) {
      setCropSource(null)
      return
    }
    const source = URL.createObjectURL(cropFile)
    setCropSource(source)
    return () => URL.revokeObjectURL(source)
  }, [cropFile])

  useEffect(() => {
    const hasModal = addModalOpen || deleteModal.open || preview.open
    if (!hasModal) return undefined
    const originalOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => {
      document.body.style.overflow = originalOverflow
    }
  }, [addModalOpen, deleteModal.open, preview.open])

  useEffect(
    () => () => {
      uploadRequestIdRef.current += 1
      const temporaryMediaId = temporaryMediaIdRef.current
      temporaryMediaIdRef.current = null
      if (temporaryMediaId) {
        void mediaService.releaseTemporaryImage(temporaryMediaId).catch((err) => {
          console.warn('释放未使用的轮播图失败，将由服务端定时清理', err)
        })
      }
    },
    [],
  )

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
    uploadRequestIdRef.current += 1
    temporaryMediaIdRef.current = null
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
    setCropFile(null)
    setCropping(false)
    setAddSubmitting(false)
    setAddMessage(null)
    setAddModalOpen(true)
  }

  const closeAddModal = () => {
    if (addSubmitting || uploading || cropping) return
    uploadRequestIdRef.current += 1
    const temporaryMediaId = temporaryMediaIdRef.current
    temporaryMediaIdRef.current = null
    if (temporaryMediaId) {
      void mediaService.releaseTemporaryImage(temporaryMediaId).catch((err) => {
        console.warn('释放临时轮播图失败，将由服务端定时清理', err)
      })
    }
    setAddModalOpen(false)
    setCropFile(null)
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

  const openCropper = (file: File) => {
    setCrop({ x: 0, y: 0 })
    setZoom(1)
    setCroppedArea(null)
    setCropFile(file)
    setAddMessage(null)
  }

  const closeCropper = () => {
    if (cropping) return
    setCropFile(null)
  }

  const confirmCrop = async () => {
    if (!cropFile || !cropSource || !croppedArea) {
      setAddMessage({ type: 'error', text: '图片尚未准备好，请稍后再试' })
      return
    }

    setCropping(true)
    try {
      const croppedFile = await createBannerCropFile(cropSource, croppedArea, cropFile.name)
      setCropFile(null)
      await handleFileSelection(croppedFile)
    } catch (err) {
      console.error('裁切轮播图失败', err)
      setAddMessage({ type: 'error', text: getErrorMessage(err, '图片处理失败，请重新选择') })
    } finally {
      setCropping(false)
    }
  }

  const handleFileSelection = async (file: File | null) => {
    const requestId = uploadRequestIdRef.current + 1
    uploadRequestIdRef.current = requestId
    const previousMediaId = temporaryMediaIdRef.current
    temporaryMediaIdRef.current = null
    if (previousMediaId) {
      void mediaService.releaseTemporaryImage(previousMediaId).catch((err) => {
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
    if (!file) {
      return
    }

    setUploading(true)
    try {
      const result = await mediaService.uploadImage(file, 'BANNER', (progress) => {
        if (uploadRequestIdRef.current === requestId) {
          setUploadProgress(progress)
        }
      })
      if (uploadRequestIdRef.current !== requestId) {
        await mediaService.releaseTemporaryImage(result.mediaId).catch((err) => {
          console.warn('释放已取消的轮播图失败，将由服务端定时清理', err)
        })
        return
      }
      temporaryMediaIdRef.current = result.mediaId
      setFormState((prev) => ({
        ...prev,
        imgUrl: result.previewUrl,
        imageAssetId: result.mediaId,
      }))
    } catch (err) {
      if (uploadRequestIdRef.current !== requestId) return
      console.error('上传图片失败', err)
      setAddMessage({ type: 'error', text: getErrorMessage(err, '上传失败，请稍后重试') })
    } finally {
      if (uploadRequestIdRef.current === requestId) {
        setUploading(false)
      }
    }
  }

  const handleCreate = async () => {
    if (!formState.title.trim()) {
      setAddMessage({ type: 'error', text: '请填写轮播图标题' })
      return
    }
    if (uploading) {
      setAddMessage({ type: 'error', text: '图片正在上传，请稍候' })
      return
    }
    if (!formState.imageAssetId) {
      setAddMessage({ type: 'error', text: '请选择轮播图图片' })
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
      temporaryMediaIdRef.current = null
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

  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      width: 72,
    },
    {
      title: '图片',
      dataIndex: 'imgUrl',
      width: 150,
      render: (value: string | undefined, record: Banner) =>
        value ? (
          <button
            type="button"
            className="banner-image-button"
            onClick={() => openPreview(record)}
          >
            <img src={value} alt={record.title || '轮播图'} />
          </button>
        ) : (
          '-'
        ),
    },
    {
      title: '标题',
      dataIndex: 'title',
      width: 180,
      render: (value: string | undefined) => value || '-',
    },
    {
      title: '学校',
      width: 160,
      render: (_: unknown, record: Banner) => schoolLabel(record),
    },
    {
      title: '跳转类型',
      dataIndex: 'jumpType',
      width: 120,
      render: (value: number) => <Tag color="blue">{jumpTypeLabels[value] || '-'}</Tag>,
    },
    {
      title: '跳转目标',
      dataIndex: 'jumpTarget',
      width: 220,
      ellipsis: true,
      render: (value: string | undefined) => value || '-',
    },
    {
      title: '备注',
      dataIndex: 'remark',
      width: 180,
      ellipsis: true,
      render: (value: string | undefined) => value || '-',
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      width: 180,
      render: formatCreateTime,
    },
    {
      title: '操作',
      fixed: 'right' as const,
      width: 100,
      render: (_: unknown, record: Banner) => (
        <Button
          type="link"
          danger
          loading={actionId === record.id}
          onClick={() => setDeleteModal({ open: true, banner: record })}
        >
          删除
        </Button>
      ),
    },
  ]

  return (
    <AdminPage className="banner-management">
      <AdminPageHeader
        title="轮播图管理"
        description="管理小程序首页轮播图及跳转设置"
        actions={
          <>
            <Button
              icon={<ReloadOutlined />}
              onClick={() => loadList(selectedSchoolId)}
              loading={loading}
            >
              刷新
            </Button>
            <Button type="primary" icon={<PlusOutlined />} onClick={openAddModal}>
              新增轮播图
            </Button>
          </>
        }
      />

      <AdminFilterBar>
        <Select
          aria-label="查看学校"
          value={selectedSchoolId}
          style={{ width: 190 }}
          onChange={setSelectedSchoolId}
          options={[
            { value: 0, label: '通用轮播图' },
            ...schools.map((school) => ({ value: school.id, label: school.schoolName })),
          ]}
        />
        <Input.Search
          allowClear
          placeholder="搜索标题、跳转目标、备注或学校"
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
          onSearch={setKeyword}
          style={{ width: 360 }}
        />
      </AdminFilterBar>

      {error && (
        <Alert type="error" showIcon title={error} />
      )}

      <AdminContentCard
        title={listTitle}
        description="展示当前筛选条件下的轮播图"
        extra={
          <AdminCount>{loading ? '加载中' : `共 ${filteredList.length} 条`}</AdminCount>
        }
        flush
      >
        <Table
          rowKey="id"
          loading={loading}
          dataSource={filteredList}
          columns={columns}
          locale={createAdminTableLocale('暂无轮播图数据')}
          scroll={{ x: 1320 }}
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 条`,
          }}
        />
      </AdminContentCard>

      <Modal
        title="新增轮播图"
        open={addModalOpen}
        onCancel={closeAddModal}
        onOk={handleCreate}
        okText="保存"
        cancelText="取消"
        confirmLoading={addSubmitting}
        okButtonProps={{ disabled: uploading || cropping }}
        mask={{ closable: !addSubmitting && !uploading && !cropping }}
        destroyOnHidden
        width={720}
      >
        {addMessage && (
          <Alert
            className="banner-form-alert"
            type={addMessage.type}
            showIcon
            title={addMessage.text}
          />
        )}
        <Form layout="vertical">
          <div className="admin-form-grid">
            <Form.Item label="标题" required>
              <Input
                value={formState.title}
                onChange={(event) =>
                  setFormState((prev) => ({ ...prev, title: event.target.value }))
                }
                placeholder="请输入轮播图标题"
              />
            </Form.Item>

            <Form.Item label="归属学校" required>
              <Select
                value={formState.schoolId}
                onChange={handleSchoolChange}
                options={[
                  { value: 0, label: '通用轮播图' },
                  ...schools.map((school) => ({
                    value: school.id,
                    label: school.schoolName,
                  })),
                ]}
              />
              {formState.schoolId !== 0 && !selectedSchoolName && (
                <span className="banner-form-hint">学校列表加载中，请稍后再上传</span>
              )}
            </Form.Item>

            <Form.Item className="admin-form-grid__full" label="轮播图图片" required>
              <Space direction="vertical" size={12}>
                <Upload
                  accept="image/jpeg,image/png,image/webp"
                  showUploadList={false}
                  disabled={uploading}
                  beforeUpload={(file) => {
                    openCropper(file)
                    return false
                  }}
                >
                  <Button icon={<UploadOutlined />} loading={uploading}>
                    选择图片
                  </Button>
                </Upload>
                {uploading && (
                  <Progress percent={uploadProgress} size="small" style={{ width: 280 }} />
                )}
                {!uploading && formState.imageAssetId && (
                  <span className="banner-upload-ready">图片已就绪</span>
                )}
                <span className="banner-form-hint">
                  选择图片后可拖动和缩放，显示范围与小程序首页一致
                </span>
                {(formState.imgUrl || localPreview) && (
                  <Image
                    src={formState.imgUrl || localPreview || ''}
                    alt="轮播图预览"
                    width={320}
                    className="banner-form-preview"
                  />
                )}
              </Space>
            </Form.Item>

            <Form.Item label="跳转类型">
              <Select
                value={formState.jumpType}
                onChange={handleJumpTypeChange}
                options={jumpTypeOptions}
              />
            </Form.Item>

            <Form.Item label="跳转目标">
              <Input
                value={formState.jumpTarget}
                onChange={(event) =>
                  setFormState((prev) => ({ ...prev, jumpTarget: event.target.value }))
                }
                placeholder="请输入所选跳转类型对应的地址"
                disabled={formState.jumpType === 0}
              />
            </Form.Item>

            <Form.Item className="admin-form-grid__full" label="备注说明">
              <Input.TextArea
                rows={3}
                value={formState.remark}
                onChange={(event) =>
                  setFormState((prev) => ({ ...prev, remark: event.target.value }))
                }
                placeholder="可选，补充说明"
              />
            </Form.Item>
          </div>
        </Form>
      </Modal>

      <Modal
        title="调整轮播图显示范围"
        open={Boolean(cropFile)}
        onCancel={closeCropper}
        onOk={() => void confirmCrop()}
        okText="确认并上传"
        cancelText="取消"
        confirmLoading={cropping}
        mask={{ closable: !cropping }}
        width={860}
        centered
        destroyOnHidden
      >
        <p className="banner-crop-description">
          拖动图片选择首页要显示的部分，使用下方滑块调整大小。
        </p>
        <div className="banner-crop-stage">
          {cropSource && (
            <Cropper
              image={cropSource}
              crop={crop}
              zoom={zoom}
              aspect={BANNER_ASPECT_RATIO}
              onCropChange={setCrop}
              onZoomChange={setZoom}
              onCropComplete={(_, areaPixels) => setCroppedArea(areaPixels)}
              showGrid
            />
          )}
        </div>
        <div className="banner-crop-zoom">
          <span>图片大小</span>
          <Slider
            min={1}
            max={3}
            step={0.01}
            value={zoom}
            onChange={setZoom}
            aria-label="调整图片大小"
          />
        </div>
      </Modal>

      <Modal
        title="删除轮播图"
        open={deleteModal.open}
        onCancel={() => setDeleteModal({ open: false })}
        onOk={() => deleteModal.banner && handleDelete(deleteModal.banner)}
        okText="确认删除"
        cancelText="取消"
        okButtonProps={{ danger: true }}
        confirmLoading={actionId === deleteModal.banner?.id}
      >
        <p>确认删除该轮播图吗？删除后不可恢复。</p>
      </Modal>

      <Modal
        title={preview.title || '轮播图预览'}
        open={preview.open}
        onCancel={() => setPreview({ open: false })}
        footer={null}
        width={900}
        centered
      >
        {preview.url && (
          <Image
            src={preview.url}
            alt={preview.title || '轮播图预览'}
            width="100%"
            preview={false}
          />
        )}
      </Modal>
    </AdminPage>
  )
}
