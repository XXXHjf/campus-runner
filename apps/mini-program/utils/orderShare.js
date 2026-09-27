const title = '帮帮校园送 · 校园跑腿';
const imageUrl = '/images/tabBar/tab-runner-selected.png';

// 使用固定封面，避免微信默认截图包含地址、电话或凭证。
function shareOrder(id, order = {}) {
  const canShareOrder = id && Number(order.status) === 0;
  return {
    title,
    imageUrl,
    path: canShareOrder
      ? `/pages/orders/takeOrders/takesInfo/info?id=${encodeURIComponent(id)}`
      : '/pages/index/index',
  };
}

module.exports = { shareOrder, title, imageUrl };
