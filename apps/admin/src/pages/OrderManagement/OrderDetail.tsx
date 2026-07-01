/**
 * 订单详情组件
 * 展示 4 个 Descriptions 区块：订单信息、接单人信息、支付信息、退款信息
 */

import { Descriptions, Tag, Image, Skeleton, Empty } from 'antd'
import type { AdminOrderDetailResponse } from '../../types/admin'
import { DOOR_ACCESS_LABELS } from '../../constants'
import { formatDateTime, formatPrice, formatPhone } from '../../utils/format'

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

interface OrderDetailProps {
  detail: AdminOrderDetailResponse | null
  loading: boolean
}

export default function OrderDetail({ detail, loading }: OrderDetailProps) {
  if (loading) {
    return (
      <div style={{ padding: '16px 0' }}>
        <Skeleton active paragraph={{ rows: 6 }} />
      </div>
    )
  }

  if (!detail) {
    return <Empty description="暂无订单数据" />
  }

  const { order, taker, payment, refund } = detail

  return (
    <div className="order-detail">
      {/* 1. 订单信息 */}
      <Descriptions
        title="订单信息"
        column={2}
        bordered
        size="small"
        className="detail-descriptions"
      >
        <Descriptions.Item label="订单编号" span={2}>
          {order.orderNumber}
        </Descriptions.Item>
        <Descriptions.Item label="订单状态">
          {getOrderStatusTag(order.status)}
        </Descriptions.Item>
        <Descriptions.Item label="分类">
          {order.categoryName || '-'}
        </Descriptions.Item>
        <Descriptions.Item label="订单金额">
          {formatPrice(order.price)}
        </Descriptions.Item>
        <Descriptions.Item label="服务费率">
          {order.serviceFeeRate != null ? `${(order.serviceFeeRate * 100).toFixed(1)}%` : '-'}
        </Descriptions.Item>
        <Descriptions.Item label="服务费">
          {formatPrice(order.serviceFee)}
        </Descriptions.Item>
        <Descriptions.Item label="支付总额">
          <span style={{ fontWeight: 600, color: '#52c41a' }}>{formatPrice(order.payAmount)}</span>
        </Descriptions.Item>
        <Descriptions.Item label="发单人" span={2}>
          {order.username}
        </Descriptions.Item>
        <Descriptions.Item label="联系电话" span={2}>
          {formatPhone(order.phone)}
        </Descriptions.Item>
        <Descriptions.Item label="取件地址" span={2}>
          {order.pickUpAddress || '-'}
        </Descriptions.Item>
        <Descriptions.Item label="收件地址" span={2}>
          {order.reciveAddress || '-'}
        </Descriptions.Item>
        <Descriptions.Item label="备注" span={2}>
          {order.note || '-'}
        </Descriptions.Item>
        <Descriptions.Item label="图片" span={2}>
          {order.image ? (
            <Image
              src={order.image}
              width={120}
              style={{ objectFit: 'contain', border: '1px solid #f0f0f0', borderRadius: 8 }}
              fallback="data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='120' height='120'%3E%3Crect width='120' height='120' fill='%23f0f0f0'/%3E%3Ctext x='50%25' y='50%25' text-anchor='middle' dy='.3em' fill='%23ccc' font-size='12'%3E暂无%3C/text%3E%3C/svg%3E"
            />
          ) : (
            '-'
          )}
        </Descriptions.Item>
        <Descriptions.Item label="门禁">
          {DOOR_ACCESS_LABELS[order.doorAccess as keyof typeof DOOR_ACCESS_LABELS] || '-'}
        </Descriptions.Item>
        <Descriptions.Item label="配送时限">
          {order.gap != null ? `${order.gap}分钟` : '-'}
        </Descriptions.Item>
        <Descriptions.Item label="最晚送达">
          {order.exceedTime ? formatDateTime(order.exceedTime) : '-'}
        </Descriptions.Item>
        <Descriptions.Item label="创建时间" span={2}>
          {formatDateTime(order.createTime)}
        </Descriptions.Item>
        {order.cancelTime && (
          <Descriptions.Item label="取消时间" span={2}>
            {formatDateTime(order.cancelTime)}
          </Descriptions.Item>
        )}
        {order.cancelReason && (
          <Descriptions.Item label="取消原因" span={2}>
            {order.cancelReason}
          </Descriptions.Item>
        )}
      </Descriptions>

      {/* 2. 接单人信息 */}
      {taker && (
        <Descriptions
          title="接单人信息"
          column={2}
          bordered
          size="small"
          className="detail-descriptions"
        >
          <Descriptions.Item label="接单人" span={2}>
            {taker.realname || '-'}
          </Descriptions.Item>
          <Descriptions.Item label="联系电话" span={2}>
            {formatPhone(taker.phone)}
          </Descriptions.Item>
          <Descriptions.Item label="接单时间" span={2}>
            {formatDateTime(taker.takeTime)}
          </Descriptions.Item>
          <Descriptions.Item label="送达时间" span={2}>
            {taker.deliveryTime ? formatDateTime(taker.deliveryTime) : '-'}
          </Descriptions.Item>
          <Descriptions.Item label="接单人图片" span={2}>
            {taker.image ? (
              <Image
                src={taker.image}
                width={120}
                style={{ objectFit: 'contain', border: '1px solid #f0f0f0', borderRadius: 8 }}
                fallback="data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='120' height='120'%3E%3Crect width='120' height='120' fill='%23f0f0f0'/%3E%3Ctext x='50%25' y='50%25' text-anchor='middle' dy='.3em' fill='%23ccc' font-size='12'%3E暂无%3C/text%3E%3C/svg%3E"
              />
            ) : (
              '-'
            )}
          </Descriptions.Item>
        </Descriptions>
      )}

      {/* 3. 支付信息 */}
      {payment && (
        <Descriptions
          title="支付信息"
          column={2}
          bordered
          size="small"
          className="detail-descriptions"
        >
          <Descriptions.Item label="交易单号" span={2}>
            {payment.transactionId || '-'}
          </Descriptions.Item>
          <Descriptions.Item label="交易状态">
            {payment.tradeState || '-'}
          </Descriptions.Item>
          <Descriptions.Item label="支付金额">
            <span style={{ fontWeight: 600, color: '#52c41a' }}>
              {formatPrice(payment.total)}
            </span>
          </Descriptions.Item>
          <Descriptions.Item label="支付服务费">
            {formatPrice(payment.serviceFee)}
          </Descriptions.Item>
          <Descriptions.Item label="支付用户" span={2}>
            {payment.payerOpenid || '-'}
          </Descriptions.Item>
          <Descriptions.Item label="支付时间" span={2}>
            {payment.successTime ? formatDateTime(payment.successTime) : '-'}
          </Descriptions.Item>
        </Descriptions>
      )}

      {/* 4. 退款信息 */}
      {refund && (
        <Descriptions
          title="退款信息"
          column={2}
          bordered
          size="small"
          className="detail-descriptions"
        >
          <Descriptions.Item label="退款单号" span={2}>
            {refund.refundNumber || '-'}
          </Descriptions.Item>
          <Descriptions.Item label="退款交易号" span={2}>
            {refund.refundId || '-'}
          </Descriptions.Item>
          <Descriptions.Item label="退款金额">
            <span style={{ fontWeight: 600, color: '#e34d59' }}>
              {formatPrice(refund.refundAmount)}
            </span>
          </Descriptions.Item>
          <Descriptions.Item label="退款状态">
            {refund.refundStatus || '-'}
          </Descriptions.Item>
          <Descriptions.Item label="退款原因" span={2}>
            {refund.reason || '-'}
          </Descriptions.Item>
          <Descriptions.Item label="退款时间" span={2}>
            {refund.createTime ? formatDateTime(refund.createTime) : '-'}
          </Descriptions.Item>
        </Descriptions>
      )}
    </div>
  )
}

export { getOrderStatusTag, orderStatusMap }
