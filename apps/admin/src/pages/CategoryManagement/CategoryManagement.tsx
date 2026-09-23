/**
 * 分类管理
 * - 获取分类列表：GET /admin/api/categories
 * - 新增分类：POST /admin/api/categories
 * - 更新分类：PUT /admin/api/categories/{id}
 * - 删除分类：DELETE /admin/api/categories/{id}
 */

import { useEffect, useMemo, useState } from 'react'
import {
  Table,
  Button,
  Modal,
  Form,
  Input,
  App,
  Space,
  Image,
  Upload,
  InputNumber,
  Select,
  Switch,
  Tag,
} from 'antd'
import { UploadOutlined } from '@ant-design/icons'
import type { AdminCategory } from '../../types/admin'
import { mediaService } from '../../services'
import { get, post, put } from '../../services/request'
import {
  AdminContentCard,
  AdminFilterBar,
  AdminPage,
  AdminPageHeader,
  createAdminTableLocale,
} from '../../components/admin'
import './CategoryManagement.css'

export default function CategoryManagement() {
  const { message } = App.useApp()
  const [loading, setLoading] = useState(false)
  const [list, setList] = useState<AdminCategory[]>([])
  const [keyword, setKeyword] = useState('')
  const [modalOpen, setModalOpen] = useState(false)
  const [editingCategory, setEditingCategory] = useState<AdminCategory | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [temporaryMediaId, setTemporaryMediaId] = useState<number | null>(null)
  const [imagePreview, setImagePreview] = useState<string | null>(null)
  const [form] = Form.useForm()

  const loadList = async () => {
    setLoading(true)
    try {
      const data = await get<AdminCategory[]>('/admin/api/categories')
      setList(Array.isArray(data) ? data : [])
    } catch (err) {
      console.error('获取分类列表失败', err)
      message.error('获取分类列表失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadList()
  }, [])

  const filteredList = useMemo(() => {
    if (!keyword.trim()) return list
    const kw = keyword.trim().toLowerCase()
    return list.filter(
      (item) =>
        item.categoryName.toLowerCase().includes(kw) ||
        String(item.id).includes(kw),
    )
  }, [list, keyword])

  const openAddModal = () => {
    setEditingCategory(null)
    form.resetFields()
    setTemporaryMediaId(null)
    setImagePreview(null)
    setModalOpen(true)
  }

  const openEditModal = (record: AdminCategory) => {
    setEditingCategory(record)
    form.setFieldsValue({
      categoryName: record.categoryName,
      categoryCode: record.categoryCode,
      businessType: record.businessType,
      enabled: record.enabled === 1,
      sortOrder: record.sortOrder,
    })
    setTemporaryMediaId(null)
    setImagePreview(record.image || null)
    setModalOpen(true)
  }

  const closeModal = () => {
    if (temporaryMediaId) {
      void mediaService.releaseTemporaryImage(temporaryMediaId).catch((err) => {
        console.warn('释放临时分类图标失败，将由服务端定时清理', err)
      })
    }
    setTemporaryMediaId(null)
    setModalOpen(false)
  }

  const handleImageChange = async (file: File | null) => {
    if (!file) return
    setUploading(true)
    try {
      const uploaded = await mediaService.uploadImage(file, 'ORDER_CATEGORY_ICON')
      if (temporaryMediaId) {
        await mediaService.releaseTemporaryImage(temporaryMediaId).catch(() => {})
      }
      setTemporaryMediaId(uploaded.mediaId)
      setImagePreview(uploaded.previewUrl)
      form.setFieldValue('imageAssetId', uploaded.mediaId)
    } catch (err) {
      console.error('上传分类图标失败', err)
      message.error(err instanceof Error ? err.message : '上传分类图标失败，请重试')
    } finally {
      setUploading(false)
    }
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      const payload = { ...values, enabled: values.enabled ? 1 : 0 }
      setSubmitting(true)
      if (editingCategory) {
        await put(`/admin/api/categories/${editingCategory.id}`, payload)
        message.success('更新成功')
      } else {
        await post('/admin/api/categories', payload)
        message.success('新增成功')
      }
      setTemporaryMediaId(null)
      setModalOpen(false)
      await loadList()
    } catch (err: unknown) {
      const error = err as Record<string, unknown>
      if (error?.errorFields) return // form validation error, antd shows inline messages
      console.error('操作失败', err)
      message.error('操作失败')
    } finally {
      setSubmitting(false)
    }
  }

  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 80,
    },
    {
      title: '分类名称',
      dataIndex: 'categoryName',
      key: 'categoryName',
    },
    {
      title: '业务编码',
      dataIndex: 'categoryCode',
      key: 'categoryCode',
    },
    {
      title: '业务类型',
      dataIndex: 'businessType',
      key: 'businessType',
      render: (value: AdminCategory['businessType']) => (
        <Tag color={value === 'PURCHASE' ? 'orange' : 'blue'}>
          {value === 'PURCHASE' ? '代买' : '普通跑腿'}
        </Tag>
      ),
    },
    {
      title: '状态',
      dataIndex: 'enabled',
      key: 'enabled',
      width: 90,
      render: (value: number) => <Tag color={value === 1 ? 'green' : 'default'}>{value === 1 ? '启用' : '停用'}</Tag>,
    },
    {
      title: '排序',
      dataIndex: 'sortOrder',
      key: 'sortOrder',
      width: 80,
    },
    {
      title: '图标',
      dataIndex: 'image',
      key: 'image',
      width: 120,
      render: (image: string | undefined) =>
        image ? (
          <Image
            src={image}
            width={60}
            height={60}
            style={{ objectFit: 'cover', borderRadius: 8 }}
            preview={{ mask: '查看' }}
          />
        ) : (
          '-'
        ),
    },
    {
      title: '操作',
      key: 'action',
      width: 100,
      render: (_: unknown, record: AdminCategory) => (
        <Space>
          <Button type="link" onClick={() => openEditModal(record)}>
            编辑
          </Button>
        </Space>
      ),
    },
  ]

  return (
    <AdminPage className="category-management">
      <AdminPageHeader
        title="分类管理"
        description="管理跑腿订单的分类"
        actions={
          <Button type="primary" onClick={openAddModal}>
            新增分类
          </Button>
        }
      />

      <AdminFilterBar>
        <Input.Search
          placeholder="搜索分类名称"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          onSearch={(value) => setKeyword(value)}
          style={{ width: 320 }}
          allowClear
        />
      </AdminFilterBar>

      <AdminContentCard flush>
        <Table
          dataSource={filteredList}
          columns={columns}
          rowKey="id"
          loading={loading}
          locale={createAdminTableLocale('暂无分类数据')}
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showTotal: (total: number) => `共 ${total} 条`,
          }}
        />
      </AdminContentCard>

      <Modal
        title={editingCategory ? '编辑分类' : '新增分类'}
        open={modalOpen}
        onOk={handleSubmit}
        onCancel={closeModal}
        confirmLoading={submitting}
        destroyOnHidden
      >
        <Form form={form} layout="vertical" preserve={false}>
          <Form.Item
            name="categoryName"
            label="分类名称"
            rules={[{ required: true, message: '请输入分类名称' }]}
          >
            <Input placeholder="请输入分类名称" />
          </Form.Item>
          <Form.Item
            name="categoryCode"
            label="业务编码"
            rules={[{ required: true, pattern: /^[A-Z][A-Z0-9_]{1,31}$/, message: '请输入2至32位大写字母、数字或下划线' }]}
          >
            <Input disabled={Boolean(editingCategory)} placeholder="例如 PURCHASE" />
          </Form.Item>
          <Form.Item name="businessType" label="业务类型" initialValue="NORMAL" rules={[{ required: true }]}>
            <Select disabled={Boolean(editingCategory)} options={[{ label: '普通跑腿', value: 'NORMAL' }, { label: '代买', value: 'PURCHASE' }]} />
          </Form.Item>
          <Form.Item name="enabled" label="允许发布" valuePropName="checked" initialValue>
            <Switch checkedChildren="启用" unCheckedChildren="停用" />
          </Form.Item>
          <Form.Item name="sortOrder" label="展示顺序" initialValue={50} rules={[{ required: true, message: '请输入展示顺序' }]}>
            <InputNumber min={0} precision={0} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="imageAssetId" hidden>
            <Input />
          </Form.Item>
          <Form.Item label="分类图标">
            <Space direction="vertical">
              <Upload
                showUploadList={false}
                accept="image/jpeg,image/png,image/webp"
                disabled={uploading}
                beforeUpload={(file) => {
                  void handleImageChange(file)
                  return false
                }}
              >
                <Button icon={<UploadOutlined />} loading={uploading}>
                  选择图片
                </Button>
              </Upload>
              {imagePreview && (
                <Image
                  src={imagePreview}
                  width={80}
                  height={80}
                  style={{ objectFit: 'cover', borderRadius: 8 }}
                />
              )}
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </AdminPage>
  )
}
