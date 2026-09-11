const ORDER_STATUS = {
  0: { text: '待支付', theme: 'warning' },
  1: { text: '待卖家交付', theme: 'warning' },
  2: { text: '待确认收货', theme: 'warning' },
  3: { text: '已完成', theme: 'success' },
  4: { text: '已取消', theme: 'default' },
  5: { text: '退款中', theme: 'warning' },
  6: { text: '退款成功', theme: 'success' },
  7: { text: '退款异常', theme: 'danger' },
  8: { text: '收款中', theme: 'warning' },
  9: { text: '收款成功', theme: 'success' },
  10: { text: '收款异常', theme: 'danger' },
  11: { text: '协商中', theme: 'danger' },
};

const OFFLINE_ORDER_STATUS = {
  1: { text: '待交付', theme: 'warning' },
  2: { text: '待确认完成', theme: 'warning' },
  3: { text: '已完成', theme: 'success' },
  4: { text: '已取消', theme: 'default' },
  11: { text: '协商中', theme: 'danger' },
};

// 当前版本关闭买家线上付款入口；保留旧订单代码用于历史订单善后和未来重新评估。
const SECOND_HAND_ONLINE_PAYMENT_ENABLED = false;

const BARGAIN_STATUS = {
  0: { text: '待回复', theme: 'warning' },
  1: { text: '已接受', theme: 'success' },
  2: { text: '已拒绝', theme: 'danger' },
  3: { text: '已失效', theme: 'default' },
};

function orderStatus(status, tradeMode) {
  const statusMap = String(tradeMode || '').toUpperCase() === 'OFFLINE'
    ? OFFLINE_ORDER_STATUS
    : ORDER_STATUS;
  return statusMap[Number(status)] || { text: '订单状态', theme: 'default' };
}

function bargainStatus(status) {
  return BARGAIN_STATUS[Number(status)] || { text: '未知', theme: 'default' };
}

function formatRemain(seconds) {
  const value = Math.max(0, Number(seconds) || 0);
  if (value <= 0) return '已超时';
  const minutes = Math.floor(value / 60);
  const secs = value % 60;
  if (minutes >= 60) {
    const hours = Math.floor(minutes / 60);
    const restMinutes = minutes % 60;
    return `${hours}小时${String(restMinutes).padStart(2, '0')}分钟`;
  }
  return `${String(minutes).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
}

function friendlyError(error, fallback) {
  const raw = error && error.message ? String(error.message) : '';
  if (!raw) return fallback;
  if (raw.includes('BadSqlGrammarException') || raw.includes('Exception:') || raw.includes('###')) {
    return fallback;
  }
  if (raw.includes('不能给自己发布的商品下单') || raw.includes('不能购买自己的商品')) return '不能给自己发布的商品下单';
  if (raw.includes('不能向自己的商品议价')) return '不能给自己的商品议价';
  if (raw.includes('议价次数已用完')) return '议价次数已用完';
  if (raw.includes('订单已超时关闭')) return '订单已超时，请重新购买';
  if (raw.includes('订单状态已更新')) return '订单状态已更新，请刷新查看';
  return raw.length > 18 ? raw.slice(0, 18) : raw;
}

module.exports = {
  SECOND_HAND_ONLINE_PAYMENT_ENABLED,
  orderStatus,
  bargainStatus,
  formatRemain,
  friendlyError,
};
