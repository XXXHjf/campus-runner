import { useEffect, useMemo, useRef, useState } from 'react'
import {
  Button,
  Card,
  Col,
  Descriptions,
  Drawer,
  Empty,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Row,
  Select,
  Space,
  Statistic,
  Table,
  Tabs,
  Tag,
  Upload,
  message,
} from 'antd'
import type { UploadFile, UploadProps } from 'antd'
import { del, get, post, put } from '../../services/request'
import {
  releaseTemporaryImage,
  uploadImage,
  type MediaUploadResult,
} from '../../services/media.service'
import './SecondHandManagement.css'

type Category = {
  id: number
  name: string
  image?: string
  imageAssetId?: number
  sort?: number
}

type Product = {
  id: number
  sellerId?: number
  sellerName?: string
  schoolName?: string
  compusName?: string
  categoryId?: number
  categoryName?: string
  title: string
  description?: string
  images?: string
  conditionLevel?: string
  price: number
  pickupLocation?: string
  pickupAddressSnapshot?: string
  pickupOnly?: number
  supportDelivery?: number
  negotiable?: number
  status: number
  viewCount?: number
  favoriteCount?: number
  createTime?: string
  updateTime?: string
}

type Order = {
  id: number
  orderNumber: string
  productId: number
  bargainId?: number
  productTitle: string
  productImages?: string
  buyerName?: string
  sellerName?: string
  productAmount?: number
  payAmount: number
  serviceFeeRate?: number
  serviceFee?: number
  sellerIncome?: number
  deliveryMode?: number
  pickupAddressSnapshot?: string
  buyerDeliveryAddressSnapshot?: string
  deliveryRemark?: string
  status: number
  payTime?: string
  cancelTime?: string
  cancelReason?: string
  deliveredTime?: string
  confirmDeadline?: string
  finishTime?: string
  transferTime?: string
  transferFailReason?: string
  payDeadline?: string
  payRemainSeconds?: number
  createTime?: string
  updateTime?: string
}

type Bargain = {
  id: number
  productId: number
  orderId?: number
  productTitle: string
  buyerName?: string
  sellerName?: string
  offerPrice: number
  message?: string
  status: number
  attemptNo: number
  createTime?: string
  updateTime?: string
}

type MessageRecord = {
  id: number
  productId: number
  orderId?: number
  senderName?: string
  receiverName?: string
  content: string
  createTime?: string
}

type DetailState =
  | { open: false; type: null; data: null; loading: false }
  | { open: true; type: 'product'; data: Product | null; loading: boolean }
  | { open: true; type: 'order'; data: Order | null; loading: boolean }

const productStatus: Record<number, { label: string; color: string }> = {
  0: { label: '在售', color: 'green' },
  1: { label: '待支付', color: 'gold' },
  2: { label: '交易中', color: 'blue' },
  3: { label: '已售出', color: 'default' },
  4: { label: '已下架', color: 'red' },
}

const orderStatus: Record<number, { label: string; color: string }> = {
  0: { label: '待支付', color: 'gold' },
  1: { label: '待交付', color: 'blue' },
  2: { label: '待确认', color: 'cyan' },
  3: { label: '已完成', color: 'green' },
  4: { label: '已取消', color: 'default' },
  5: { label: '退款中', color: 'orange' },
  6: { label: '退款成功', color: 'green' },
  7: { label: '退款异常', color: 'red' },
  8: { label: '收款中', color: 'blue' },
  9: { label: '收款成功', color: 'green' },
  10: { label: '收款异常', color: 'red' },
  11: { label: '协商中', color: 'purple' },
}

const bargainStatus: Record<number, { label: string; color: string }> = {
  0: { label: '待回复', color: 'gold' },
  1: { label: '已接受', color: 'green' },
  2: { label: '已拒绝', color: 'red' },
  3: { label: '已失效', color: 'default' },
}

const productStatusOptions = Object.entries(productStatus).map(([value, item]) => ({
  label: item.label,
  value: Number(value),
}))

const orderStatusOptions = Object.entries(orderStatus).map(([value, item]) => ({
  label: item.label,
  value: Number(value),
}))

const bargainStatusOptions = Object.entries(bargainStatus).map(([value, item]) => ({
  label: item.label,
  value: Number(value),
}))

function statusTag(map: Record<number, { label: string; color: string }>, status: number) {
  const item = map[status]
  return item ? <Tag color={item.color}>{item.label}</Tag> : <Tag>未知({status})</Tag>
}

function money(value?: number) {
  return `¥${Number(value || 0).toFixed(2)}`
}

function dateTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}

function compactText(value?: string | number | null) {
  return value === undefined || value === null || value === '' ? '-' : value
}

function splitImages(images?: string) {
  if (!images) return []
  return images
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
}

function getErrorText(error: unknown, fallback: string) {
  if (error && typeof error === 'object' && 'message' in error) {
    const text = String((error as { message?: unknown }).message || '')
    if (text && !text.includes('org.springframework') && !text.includes('java.')) {
      return text
    }
  }
  return fallback
}

export default function SecondHandManagement() {
  const [categories, setCategories] = useState<Category[]>([])
  const [products, setProducts] = useState<Product[]>([])
  const [orders, setOrders] = useState<Order[]>([])
  const [bargains, setBargains] = useState<Bargain[]>([])
  const [messages, setMessages] = useState<MessageRecord[]>([])
  const [loading, setLoading] = useState(false)
  const [actionLoading, setActionLoading] = useState(false)
  const [categoryModalOpen, setCategoryModalOpen] = useState(false)
  const [editingCategory, setEditingCategory] = useState<Category | null>(null)
  const [categoryFileList, setCategoryFileList] = useState<UploadFile[]>([])
  const [categoryUploading, setCategoryUploading] = useState(false)
  const [categoryPreviewUrl, setCategoryPreviewUrl] = useState<string | null>(null)
  const [categoryImageError, setCategoryImageError] = useState<string | null>(null)
  const [detail, setDetail] = useState<DetailState>({ open: false, type: null, data: null, loading: false })
  const [orderModal, setOrderModal] = useState<{ open: boolean; order: Order | null }>({ open: false, order: null })
  const [productFilter, setProductFilter] = useState<{ keyword?: string; categoryId?: number; status?: number }>({})
  const [orderFilter, setOrderFilter] = useState<{ status?: number }>({})
  const [messageProductId, setMessageProductId] = useState<string>('')
  const [categoryForm] = Form.useForm()
  const [orderForm] = Form.useForm()
  const categoryUploadPromiseRef = useRef<Promise<MediaUploadResult> | null>(null)
  const categoryTemporaryMediaIdRef = useRef<number | null>(null)
  const categoryModalSessionRef = useRef(0)

  const stats = useMemo(() => {
    const activeProducts = products.filter((item) => item.status === 0).length
    const abnormalOrders = orders.filter((item) => [7, 10, 11].includes(item.status)).length
    const pendingBargains = bargains.filter((item) => item.status === 0).length
    const turnover = orders
      .filter((item) => [3, 8, 9, 10].includes(item.status))
      .reduce((sum, item) => sum + Number(item.payAmount || 0), 0)
    return { activeProducts, abnormalOrders, pendingBargains, turnover }
  }, [products, orders, bargains])

  const loadAll = async () => {
    setLoading(true)
    try {
      const messageParams = messageProductId ? { productId: Number(messageProductId) } : undefined
      const [categoryData, productData, orderData, bargainData, messageData] = await Promise.all([
        get<Category[]>('/admin/api/second-hand/categories'),
        get<Product[]>('/admin/api/second-hand/products', productFilter),
        get<Order[]>('/admin/api/second-hand/orders', orderFilter),
        get<Bargain[]>('/admin/api/second-hand/bargains'),
        get<MessageRecord[]>('/admin/api/second-hand/messages', messageParams),
      ])
      setCategories(Array.isArray(categoryData) ? categoryData : [])
      setProducts(Array.isArray(productData) ? productData : [])
      setOrders(Array.isArray(orderData) ? orderData : [])
      setBargains(Array.isArray(bargainData) ? bargainData : [])
      setMessages(Array.isArray(messageData) ? messageData : [])
    } catch (error) {
      console.error('加载二手交易数据失败', error)
      message.error(getErrorText(error, '加载二手交易数据失败'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadAll()
  }, [])

  const openCategoryModal = (record?: Category) => {
    categoryModalSessionRef.current += 1
    categoryUploadPromiseRef.current = null
    categoryTemporaryMediaIdRef.current = null
    setCategoryUploading(false)
    setCategoryPreviewUrl(null)
    setCategoryImageError(null)
    setEditingCategory(record || null)
    categoryForm.resetFields()
    categoryForm.setFieldsValue({
      name: record?.name,
      imageAssetId: record?.imageAssetId,
      sort: record?.sort ?? 0,
    })
    setCategoryFileList(
      record?.image
        ? [
            {
              uid: `existing-${record.id}`,
              name: `${record.name}-图标`,
              status: 'done',
              url: record.image,
            },
          ]
        : [],
    )
    setCategoryModalOpen(true)
  }

  const resetCategoryModal = () => {
    categoryUploadPromiseRef.current = null
    categoryTemporaryMediaIdRef.current = null
    setCategoryUploading(false)
    setCategoryPreviewUrl(null)
    setCategoryImageError(null)
    setCategoryFileList([])
    setEditingCategory(null)
    categoryForm.resetFields()
    setCategoryModalOpen(false)
  }

  const cancelCategoryModal = () => {
    categoryModalSessionRef.current += 1
    const temporaryMediaId = categoryTemporaryMediaIdRef.current
    resetCategoryModal()

    if (temporaryMediaId) {
      void releaseTemporaryImage(temporaryMediaId).catch(() => undefined)
    }
  }

  const uploadCategoryImage: UploadProps['customRequest'] = async (options) => {
    const file = options.file as File
    const session = categoryModalSessionRef.current
    setCategoryUploading(true)
    const task = uploadImage(file, 'SECOND_HAND_CATEGORY_ICON', (progress) => {
      options.onProgress?.({ percent: progress })
    })
    categoryUploadPromiseRef.current = task

    try {
      const result = await task
      if (session !== categoryModalSessionRef.current) {
        void releaseTemporaryImage(result.mediaId).catch(() => undefined)
        return
      }
      categoryTemporaryMediaIdRef.current = result.mediaId
      categoryForm.setFieldValue('imageAssetId', result.mediaId)
      setCategoryImageError(null)
      options.onSuccess?.(result)
    } catch (error) {
      if (session === categoryModalSessionRef.current) {
        options.onError?.(error instanceof Error ? error : new Error('图片上传失败'))
        message.error(getErrorText(error, '图片上传失败，请重试'))
      }
    } finally {
      if (session === categoryModalSessionRef.current) {
        categoryUploadPromiseRef.current = null
        setCategoryUploading(false)
      }
    }
  }

  const beforeCategoryUpload: UploadProps['beforeUpload'] = (file) => {
    const supported = ['image/jpeg', 'image/png', 'image/webp'].includes(file.type)
    if (!supported) {
      message.error('仅支持 JPEG、PNG 或 WebP 图片')
      return Upload.LIST_IGNORE
    }
    if (file.size > 2 * 1024 * 1024) {
      message.error('图片不能超过 2MB')
      return Upload.LIST_IGNORE
    }
    return true
  }

  const removeCategoryImage: UploadProps['onRemove'] = () => {
    categoryModalSessionRef.current += 1
    const temporaryMediaId = categoryTemporaryMediaIdRef.current
    categoryUploadPromiseRef.current = null
    categoryTemporaryMediaIdRef.current = null
    setCategoryUploading(false)
    setCategoryFileList([])
    categoryForm.setFieldValue('imageAssetId', undefined)
    setCategoryImageError('请选择分类图标')
    if (temporaryMediaId) {
      void releaseTemporaryImage(temporaryMediaId).catch(() => undefined)
    }
    return true
  }

  const changeCategoryUpload: UploadProps['onChange'] = ({ fileList }) => {
    const normalized = fileList.map((file) => {
      const response = file.response as MediaUploadResult | undefined
      return response?.previewUrl ? { ...file, url: response.previewUrl } : file
    })
    setCategoryFileList(normalized)
  }

  const saveCategory = async () => {
    setActionLoading(true)
    try {
      const pendingUpload = categoryUploadPromiseRef.current
      if (pendingUpload) {
        await pendingUpload
      }
      const selectedMediaId = categoryForm.getFieldValue('imageAssetId')
      const keepsLegacyImage =
        Boolean(editingCategory?.image) &&
        categoryFileList.some((file) => file.uid.startsWith('existing-'))
      const validatedValues = categoryForm.validateFields()
      if (!selectedMediaId && !keepsLegacyImage) {
        setCategoryImageError('请选择分类图标')
        await validatedValues.catch(() => undefined)
        return
      }
      const values = await validatedValues
      if (editingCategory) {
        await put(`/admin/api/second-hand/categories/${editingCategory.id}`, values)
        message.success('分类已更新')
      } else {
        await post('/admin/api/second-hand/categories', values)
        message.success('分类已新增')
      }
      categoryModalSessionRef.current += 1
      resetCategoryModal()
      await loadAll()
    } catch (error) {
      message.error(getErrorText(error, '保存分类失败'))
    } finally {
      setActionLoading(false)
    }
  }

  const deleteCategory = async (id: number) => {
    setActionLoading(true)
    try {
      await del(`/admin/api/second-hand/categories/${id}`)
      message.success('分类已删除')
      loadAll()
    } catch (error) {
      message.error(getErrorText(error, '删除分类失败'))
    } finally {
      setActionLoading(false)
    }
  }

  const updateProductStatus = async (id: number, status: number) => {
    setActionLoading(true)
    try {
      await put(`/admin/api/second-hand/products/${id}/status`, { status })
      message.success('商品状态已更新')
      loadAll()
    } catch (error) {
      message.error(getErrorText(error, '更新商品状态失败'))
    } finally {
      setActionLoading(false)
    }
  }

  const deleteProduct = async (id: number) => {
    setActionLoading(true)
    try {
      await del(`/admin/api/second-hand/products/${id}`)
      message.success('商品已删除')
      loadAll()
    } catch (error) {
      message.error(getErrorText(error, '删除商品失败'))
    } finally {
      setActionLoading(false)
    }
  }

  const openProductDetail = async (id: number) => {
    setDetail({ open: true, type: 'product', data: null, loading: true })
    try {
      const data = await get<Product>(`/admin/api/second-hand/products/${id}`)
      setDetail({ open: true, type: 'product', data, loading: false })
    } catch (error) {
      message.error(getErrorText(error, '获取商品详情失败'))
      setDetail({ open: false, type: null, data: null, loading: false })
    }
  }

  const openOrderDetail = async (id: number) => {
    setDetail({ open: true, type: 'order', data: null, loading: true })
    try {
      const data = await get<Order>(`/admin/api/second-hand/orders/${id}`)
      setDetail({ open: true, type: 'order', data, loading: false })
    } catch (error) {
      message.error(getErrorText(error, '获取订单详情失败'))
      setDetail({ open: false, type: null, data: null, loading: false })
    }
  }

  const openOrderModal = (order: Order) => {
    setOrderModal({ open: true, order })
    orderForm.setFieldsValue({ status: order.status, reason: '' })
  }

  const saveOrderStatus = async () => {
    if (!orderModal.order) return
    const values = await orderForm.validateFields()
    setActionLoading(true)
    try {
      await put(`/admin/api/second-hand/orders/${orderModal.order.id}/status`, values)
      message.success('订单已处理')
      setOrderModal({ open: false, order: null })
      loadAll()
    } catch (error) {
      message.error(getErrorText(error, '处理订单失败'))
    } finally {
      setActionLoading(false)
    }
  }

  const retryTransfer = async (id: number) => {
    setActionLoading(true)
    try {
      await post(`/admin/api/second-hand/orders/${id}/retry-transfer`)
      message.success('已发起重试')
      loadAll()
    } catch (error) {
      message.error(getErrorText(error, '重试收款失败'))
    } finally {
      setActionLoading(false)
    }
  }

  const updateBargainStatus = async (id: number, status: number) => {
    setActionLoading(true)
    try {
      await put(`/admin/api/second-hand/bargains/${id}/status`, { status })
      message.success('议价状态已更新')
      loadAll()
    } catch (error) {
      message.error(getErrorText(error, '更新议价状态失败'))
    } finally {
      setActionLoading(false)
    }
  }

  const renderProductDetail = (data: Product | null) => {
    if (!data) return <Empty />
    const images = splitImages(data.images)
    return (
      <Space direction="vertical" size={20} className="detail-stack">
        {images.length > 0 && (
          <div className="image-list">
            {images.map((src) => (
              <img key={src} src={src} alt={data.title} />
            ))}
          </div>
        )}
        <Descriptions column={1} bordered size="small">
          <Descriptions.Item label="商品名称">{data.title}</Descriptions.Item>
          <Descriptions.Item label="状态">{statusTag(productStatus, data.status)}</Descriptions.Item>
          <Descriptions.Item label="价格">{money(data.price)}</Descriptions.Item>
          <Descriptions.Item label="分类">{compactText(data.categoryName)}</Descriptions.Item>
          <Descriptions.Item label="成色">{compactText(data.conditionLevel)}</Descriptions.Item>
          <Descriptions.Item label="卖家">{compactText(data.sellerName)}</Descriptions.Item>
          <Descriptions.Item label="学校/校区">
            {[data.schoolName, data.compusName].filter(Boolean).join(' / ') || '-'}
          </Descriptions.Item>
          <Descriptions.Item label="交付方式">
            {data.pickupOnly === 1 ? '仅自提' : data.supportDelivery === 1 ? '自提或配送' : '自提'}
          </Descriptions.Item>
          <Descriptions.Item label="自提地点">{compactText(data.pickupLocation || data.pickupAddressSnapshot)}</Descriptions.Item>
          <Descriptions.Item label="是否可议价">{data.negotiable === 1 ? '可议价' : '不可议价'}</Descriptions.Item>
          <Descriptions.Item label="浏览/收藏">
            {data.viewCount || 0} / {data.favoriteCount || 0}
          </Descriptions.Item>
          <Descriptions.Item label="发布时间">{dateTime(data.createTime)}</Descriptions.Item>
          <Descriptions.Item label="商品描述">{compactText(data.description)}</Descriptions.Item>
        </Descriptions>
      </Space>
    )
  }

  const renderOrderDetail = (data: Order | null) => {
    if (!data) return <Empty />
    return (
      <Descriptions column={1} bordered size="small">
        <Descriptions.Item label="订单号">{data.orderNumber}</Descriptions.Item>
        <Descriptions.Item label="状态">{statusTag(orderStatus, data.status)}</Descriptions.Item>
        <Descriptions.Item label="商品">{data.productTitle}</Descriptions.Item>
        <Descriptions.Item label="买家">{compactText(data.buyerName)}</Descriptions.Item>
        <Descriptions.Item label="卖家">{compactText(data.sellerName)}</Descriptions.Item>
        <Descriptions.Item label="成交金额">{money(data.payAmount)}</Descriptions.Item>
        <Descriptions.Item label="服务费">{money(data.serviceFee)}</Descriptions.Item>
        <Descriptions.Item label="卖家收入">{money(data.sellerIncome)}</Descriptions.Item>
        <Descriptions.Item label="交付方式">{data.deliveryMode === 1 ? '配送' : '自提'}</Descriptions.Item>
        <Descriptions.Item label="自提地点">{compactText(data.pickupAddressSnapshot)}</Descriptions.Item>
        <Descriptions.Item label="配送地址">{compactText(data.buyerDeliveryAddressSnapshot)}</Descriptions.Item>
        <Descriptions.Item label="交付备注">{compactText(data.deliveryRemark)}</Descriptions.Item>
        <Descriptions.Item label="取消/处理原因">{compactText(data.cancelReason)}</Descriptions.Item>
        <Descriptions.Item label="收款异常原因">{compactText(data.transferFailReason)}</Descriptions.Item>
        <Descriptions.Item label="创建时间">{dateTime(data.createTime)}</Descriptions.Item>
        <Descriptions.Item label="支付时间">{dateTime(data.payTime)}</Descriptions.Item>
        <Descriptions.Item label="交付时间">{dateTime(data.deliveredTime)}</Descriptions.Item>
        <Descriptions.Item label="完成时间">{dateTime(data.finishTime)}</Descriptions.Item>
        <Descriptions.Item label="收款时间">{dateTime(data.transferTime)}</Descriptions.Item>
      </Descriptions>
    )
  }

  return (
    <div className="second-hand-management">
      <div className="page-header">
        <div>
          <h1>二手交易管理</h1>
          <p>处理商品、订单、分类、议价与留言记录</p>
        </div>
        <Button onClick={loadAll} loading={loading}>
          刷新
        </Button>
      </div>

      <Row gutter={16} className="stat-row">
        <Col xs={24} md={6}>
          <Card>
            <Statistic title="在售商品" value={stats.activeProducts} />
          </Card>
        </Col>
        <Col xs={24} md={6}>
          <Card>
            <Statistic title="待回复议价" value={stats.pendingBargains} />
          </Card>
        </Col>
        <Col xs={24} md={6}>
          <Card>
            <Statistic title="异常/协商订单" value={stats.abnormalOrders} />
          </Card>
        </Col>
        <Col xs={24} md={6}>
          <Card>
            <Statistic title="已成交金额" value={stats.turnover} precision={2} prefix="¥" />
          </Card>
        </Col>
      </Row>

      <Tabs
        className="management-tabs"
        items={[
          {
            key: 'products',
            label: '商品',
            children: (
              <>
                <div className="toolbar">
                  <Space wrap>
                    <Input
                      allowClear
                      placeholder="搜索商品标题或描述"
                      value={productFilter.keyword}
                      onChange={(event) => setProductFilter((prev) => ({ ...prev, keyword: event.target.value || undefined }))}
                      onPressEnter={loadAll}
                    />
                    <Select
                      allowClear
                      className="filter-select"
                      placeholder="分类"
                      value={productFilter.categoryId}
                      options={categories.map((item) => ({ label: item.name, value: item.id }))}
                      onChange={(value) => setProductFilter((prev) => ({ ...prev, categoryId: value }))}
                    />
                    <Select
                      allowClear
                      className="filter-select"
                      placeholder="商品状态"
                      value={productFilter.status}
                      options={productStatusOptions}
                      onChange={(value) => setProductFilter((prev) => ({ ...prev, status: value }))}
                    />
                    <Button type="primary" onClick={loadAll}>
                      查询
                    </Button>
                  </Space>
                </div>
                <Table
                  loading={loading}
                  rowKey="id"
                  dataSource={products}
                  scroll={{ x: 1180 }}
                  columns={[
                    { title: 'ID', dataIndex: 'id', width: 80 },
                    { title: '商品', dataIndex: 'title', width: 220 },
                    { title: '卖家', dataIndex: 'sellerName', width: 120, render: compactText },
                    { title: '分类', dataIndex: 'categoryName', width: 120, render: compactText },
                    { title: '校区', dataIndex: 'compusName', width: 120, render: compactText },
                    { title: '价格', dataIndex: 'price', width: 110, render: money },
                    { title: '交付', width: 120, render: (_, record) => (record.pickupOnly === 1 ? '仅自提' : '自提或配送') },
                    { title: '状态', dataIndex: 'status', width: 110, render: (value: number) => statusTag(productStatus, value) },
                    { title: '发布时间', dataIndex: 'createTime', width: 170, render: dateTime },
                    {
                      title: '操作',
                      fixed: 'right',
                      width: 210,
                      render: (_, record) => (
                        <Space>
                          <Button type="link" onClick={() => openProductDetail(record.id)}>
                            详情
                          </Button>
                          <Button
                            type="link"
                            loading={actionLoading}
                            onClick={() => updateProductStatus(record.id, record.status === 4 ? 0 : 4)}
                          >
                            {record.status === 4 ? '恢复' : '下架'}
                          </Button>
                          <Popconfirm title="确认删除该商品？" onConfirm={() => deleteProduct(record.id)}>
                            <Button type="link" danger loading={actionLoading}>
                              删除
                            </Button>
                          </Popconfirm>
                        </Space>
                      ),
                    },
                  ]}
                />
              </>
            ),
          },
          {
            key: 'orders',
            label: '订单',
            children: (
              <>
                <div className="toolbar">
                  <Space wrap>
                    <Select
                      allowClear
                      className="filter-select"
                      placeholder="订单状态"
                      value={orderFilter.status}
                      options={orderStatusOptions}
                      onChange={(value) => setOrderFilter({ status: value })}
                    />
                    <Button type="primary" onClick={loadAll}>
                      查询
                    </Button>
                  </Space>
                </div>
                <Table
                  loading={loading}
                  rowKey="id"
                  dataSource={orders}
                  scroll={{ x: 1280 }}
                  columns={[
                    { title: '订单号', dataIndex: 'orderNumber', width: 180 },
                    { title: '商品', dataIndex: 'productTitle', width: 220 },
                    { title: '买家', dataIndex: 'buyerName', width: 120, render: compactText },
                    { title: '卖家', dataIndex: 'sellerName', width: 120, render: compactText },
                    { title: '成交金额', dataIndex: 'payAmount', width: 120, render: money },
                    { title: '服务费', dataIndex: 'serviceFee', width: 110, render: money },
                    { title: '卖家收入', dataIndex: 'sellerIncome', width: 120, render: money },
                    { title: '状态', dataIndex: 'status', width: 120, render: (value: number) => statusTag(orderStatus, value) },
                    { title: '创建时间', dataIndex: 'createTime', width: 170, render: dateTime },
                    {
                      title: '操作',
                      fixed: 'right',
                      width: 220,
                      render: (_, record) => (
                        <Space>
                          <Button type="link" onClick={() => openOrderDetail(record.id)}>
                            详情
                          </Button>
                          <Button type="link" onClick={() => openOrderModal(record)}>
                            处理
                          </Button>
                          {[8, 10].includes(record.status) && (
                            <Popconfirm title="确认重试卖家收款？" onConfirm={() => retryTransfer(record.id)}>
                              <Button type="link" loading={actionLoading}>
                                重试收款
                              </Button>
                            </Popconfirm>
                          )}
                        </Space>
                      ),
                    },
                  ]}
                />
              </>
            ),
          },
          {
            key: 'categories',
            label: '分类',
            children: (
              <>
                <div className="toolbar toolbar-right">
                  <Button type="primary" onClick={() => openCategoryModal()}>
                    新增分类
                  </Button>
                </div>
                <Table
                  loading={loading}
                  rowKey="id"
                  dataSource={categories}
                  columns={[
                    { title: 'ID', dataIndex: 'id', width: 80 },
                    { title: '名称', dataIndex: 'name' },
                    {
                      title: '图标',
                      dataIndex: 'image',
                      width: 100,
                      render: (value: string | undefined, record: Category) =>
                        value ? (
                          <img
                            className="category-icon-thumbnail"
                            src={value}
                            alt={`${record.name}图标`}
                          />
                        ) : (
                          '-'
                        ),
                    },
                    { title: '排序', dataIndex: 'sort', width: 120 },
                    {
                      title: '操作',
                      width: 180,
                      render: (_, record) => (
                        <Space>
                          <Button type="link" onClick={() => openCategoryModal(record)}>
                            编辑
                          </Button>
                          <Popconfirm title="确认删除该分类？" onConfirm={() => deleteCategory(record.id)}>
                            <Button type="link" danger loading={actionLoading}>
                              删除
                            </Button>
                          </Popconfirm>
                        </Space>
                      ),
                    },
                  ]}
                />
              </>
            ),
          },
          {
            key: 'bargains',
            label: '议价',
            children: (
              <Table
                loading={loading}
                rowKey="id"
                dataSource={bargains}
                scroll={{ x: 980 }}
                columns={[
                  { title: 'ID', dataIndex: 'id', width: 80 },
                  { title: '商品', dataIndex: 'productTitle', width: 220 },
                  { title: '买家', dataIndex: 'buyerName', width: 120, render: compactText },
                  { title: '卖家', dataIndex: 'sellerName', width: 120, render: compactText },
                  { title: '报价', dataIndex: 'offerPrice', width: 110, render: money },
                  { title: '次数', dataIndex: 'attemptNo', width: 90 },
                  { title: '留言', dataIndex: 'message', ellipsis: true, render: compactText },
                  { title: '状态', dataIndex: 'status', width: 110, render: (value: number) => statusTag(bargainStatus, value) },
                  { title: '时间', dataIndex: 'createTime', width: 170, render: dateTime },
                  {
                    title: '操作',
                    fixed: 'right',
                    width: 150,
                    render: (_, record) => (
                      <Select
                        size="small"
                        value={record.status}
                        options={bargainStatusOptions}
                        onChange={(value) => updateBargainStatus(record.id, value)}
                      />
                    ),
                  },
                ]}
              />
            ),
          },
          {
            key: 'messages',
            label: '留言',
            children: (
              <>
                <div className="toolbar">
                  <Space wrap>
                    <Input
                      allowClear
                      placeholder="按商品 ID 筛选"
                      value={messageProductId}
                      onChange={(event) => setMessageProductId(event.target.value.replace(/\D/g, ''))}
                      onPressEnter={loadAll}
                    />
                    <Button type="primary" onClick={loadAll}>
                      查询
                    </Button>
                  </Space>
                </div>
                <Table
                  loading={loading}
                  rowKey="id"
                  dataSource={messages}
                  columns={[
                    { title: 'ID', dataIndex: 'id', width: 80 },
                    { title: '商品ID', dataIndex: 'productId', width: 100 },
                    { title: '订单ID', dataIndex: 'orderId', width: 100, render: compactText },
                    { title: '发送人', dataIndex: 'senderName', width: 120, render: compactText },
                    { title: '接收人', dataIndex: 'receiverName', width: 120, render: compactText },
                    { title: '内容', dataIndex: 'content' },
                    { title: '时间', dataIndex: 'createTime', width: 170, render: dateTime },
                  ]}
                />
              </>
            ),
          },
        ]}
      />

      <Modal
        title={editingCategory ? '编辑二手分类' : '新增二手分类'}
        open={categoryModalOpen}
        onOk={saveCategory}
        confirmLoading={actionLoading}
        okText={editingCategory ? '保存' : '新增'}
        cancelText="取消"
        forceRender
        mask={{ closable: !actionLoading && !categoryUploading }}
        onCancel={cancelCategoryModal}
      >
        <Form form={categoryForm} layout="vertical">
          <Form.Item name="imageAssetId" hidden>
            <Input />
          </Form.Item>
          <Form.Item name="name" label="分类名称" rules={[{ required: true, message: '请输入分类名称' }]}>
            <Input />
          </Form.Item>
          <Form.Item
            label="分类图标"
            extra="支持 JPEG、PNG、WebP，最大 2MB，建议使用正方形图片"
            required
            validateStatus={categoryImageError ? 'error' : undefined}
            help={categoryImageError || undefined}
          >
            <Upload
              accept="image/jpeg,image/png,image/webp"
              beforeUpload={beforeCategoryUpload}
              customRequest={uploadCategoryImage}
              disabled={actionLoading}
              fileList={categoryFileList}
              listType="picture-card"
              maxCount={1}
              onChange={changeCategoryUpload}
              onPreview={(file) => setCategoryPreviewUrl(file.url || file.thumbUrl || null)}
              onRemove={removeCategoryImage}
            >
              {categoryFileList.length === 0 && (
                <div className="category-upload-trigger">
                  <span>选择图片</span>
                </div>
              )}
            </Upload>
          </Form.Item>
          <Form.Item name="sort" label="排序">
            <InputNumber min={0} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="分类图标预览"
        open={Boolean(categoryPreviewUrl)}
        footer={null}
        onCancel={() => setCategoryPreviewUrl(null)}
        destroyOnHidden
      >
        {categoryPreviewUrl && (
          <img className="category-icon-preview" src={categoryPreviewUrl} alt="分类图标预览" />
        )}
      </Modal>

      <Modal
        title="处理二手订单"
        open={orderModal.open}
        onOk={saveOrderStatus}
        confirmLoading={actionLoading}
        onCancel={() => setOrderModal({ open: false, order: null })}
        destroyOnHidden
      >
        <Form form={orderForm} layout="vertical">
          <Form.Item name="status" label="订单状态" rules={[{ required: true, message: '请选择订单状态' }]}>
            <Select options={orderStatusOptions} />
          </Form.Item>
          <Form.Item name="reason" label="处理备注">
            <Input.TextArea rows={4} placeholder="需要人工说明时填写" />
          </Form.Item>
        </Form>
      </Modal>

      <Drawer
        title={detail.type === 'product' ? '商品详情' : '订单详情'}
        open={detail.open}
        size={560}
        onClose={() => setDetail({ open: false, type: null, data: null, loading: false })}
      >
        {detail.loading ? (
          <div className="drawer-loading">加载中...</div>
        ) : detail.type === 'product' ? (
          renderProductDetail(detail.data)
        ) : (
          renderOrderDetail(detail.data)
        )}
      </Drawer>
    </div>
  )
}
