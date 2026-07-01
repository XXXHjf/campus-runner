/**
 * 分类管理
 * - 获取分类列表：GET /admin/api/categories
 * - 新增分类：POST /admin/api/categories
 * - 更新分类：PUT /admin/api/categories/{id}
 * - 删除分类：DELETE /admin/api/categories/{id}
 */

import { useEffect, useMemo, useState } from 'react'
import { Table, Button, Modal, Form, Input, Popconfirm, message, Space, Image } from 'antd'
import type { AdminCategory } from '../../types/admin'
import { get, post, put, del } from '../../services/request'
import './CategoryManagement.css'

export default function CategoryManagement() {
  const [loading, setLoading] = useState(false)
  const [list, setList] = useState<AdminCategory[]>([])
  const [keyword, setKeyword] = useState('')
  const [modalOpen, setModalOpen] = useState(false)
  const [editingCategory, setEditingCategory] = useState<AdminCategory | null>(null)
  const [submitting, setSubmitting] = useState(false)
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
    setModalOpen(true)
  }

  const openEditModal = (record: AdminCategory) => {
    setEditingCategory(record)
    form.setFieldsValue(record)
    setModalOpen(true)
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      setSubmitting(true)
      if (editingCategory) {
        await put(`/admin/api/categories/${editingCategory.id}`, values)
        message.success('更新成功')
      } else {
        await post('/admin/api/categories', values)
        message.success('新增成功')
      }
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

  const handleDelete = async (id: number) => {
    try {
      await del(`/admin/api/categories/${id}`)
      message.success('删除成功')
      await loadList()
    } catch (err) {
      console.error('删除分类失败', err)
      message.error('删除失败')
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
            fallback="data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='60' height='60'%3E%3Crect width='60' height='60' fill='%23f0f0f0'/%3E%3Ctext x='50%25' y='50%25' text-anchor='middle' dy='.3em' fill='%23ccc' font-size='10'%3E暂无%3C/text%3E%3C/svg%3E"
            preview={{ mask: '查看' }}
          />
        ) : (
          '-'
        ),
    },
    {
      title: '操作',
      key: 'action',
      width: 160,
      render: (_: unknown, record: AdminCategory) => (
        <Space>
          <Button type="link" onClick={() => openEditModal(record)}>
            编辑
          </Button>
          <Popconfirm
            title="确认删除?"
            description="删除后不可恢复"
            onConfirm={() => handleDelete(record.id)}
          >
            <Button type="link" danger>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div className="category-management">
      <div className="page-header">
        <div className="header-left">
          <h1>分类管理</h1>
          <p>管理跑腿订单的分类</p>
        </div>
        <div className="header-right">
          <Button type="primary" onClick={openAddModal}>
            新增分类
          </Button>
        </div>
      </div>

      <div className="toolbar">
        <Input.Search
          placeholder="搜索分类名称..."
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          onSearch={(value) => setKeyword(value)}
          style={{ width: 320 }}
          allowClear
        />
      </div>

      <div className="table-container">
        <Table
          dataSource={filteredList}
          columns={columns}
          rowKey="id"
          loading={loading}
          locale={{ emptyText: '暂无分类数据' }}
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showTotal: (total: number) => `共 ${total} 条`,
          }}
        />
      </div>

      <Modal
        title={editingCategory ? '编辑分类' : '新增分类'}
        open={modalOpen}
        onOk={handleSubmit}
        onCancel={() => setModalOpen(false)}
        confirmLoading={submitting}
        destroyOnClose
      >
        <Form form={form} layout="vertical" preserve={false}>
          <Form.Item
            name="categoryName"
            label="分类名称"
            rules={[{ required: true, message: '请输入分类名称' }]}
          >
            <Input placeholder="请输入分类名称" />
          </Form.Item>
          <Form.Item name="image" label="图标链接">
            <Input placeholder="可选，输入图片URL" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
