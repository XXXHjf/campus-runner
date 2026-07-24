const secondHandService = require('../../../services/secondHandService');
const { orderStatus, formatRemain, friendlyError } = require('../../../utils/secondHandStatus');

Page({
  data: {
    tab: 'buy',
    buyerOrders: [],
    sellerOrders: [],
    summary: {
      waitingPay: 0,
      waitingDelivery: 0,
      waitingConfirm: 0,
    },
  },

  onShow() {
    this.loadOrders();
  },

  async loadOrders() {
    try {
      const [buyerOrders, sellerOrders] = await Promise.all([
        secondHandService.listBuyerOrders(),
        secondHandService.listSellerOrders(),
      ]);
      const decoratedBuyer = buyerOrders.map((item) => this.decorateOrder(item));
      const decoratedSeller = sellerOrders.map((item) => this.decorateOrder(item));
      const all = [...decoratedBuyer, ...decoratedSeller];
      this.setData({
        buyerOrders: decoratedBuyer,
        sellerOrders: decoratedSeller,
        summary: {
          waitingPay: all.filter((item) => Number(item.status) === 0).length,
          waitingDelivery: all.filter((item) => Number(item.status) === 1).length,
          waitingConfirm: all.filter((item) => Number(item.status) === 2).length,
        },
      });
    } catch (error) {
      wx.showToast({ title: this.errorText(error, '订单加载失败'), icon: 'none' });
    } finally {
      wx.stopPullDownRefresh();
    }
  },

  decorateOrder(order) {
    return {
      ...order,
      coverImage: this.firstImage(order.productImages),
      statusText: orderStatus(order.status).text,
      statusTheme: orderStatus(order.status).theme,
      payRemainText: Number(order.status) === 0 ? formatRemain(order.payRemainSeconds) : '',
    };
  },

  firstImage(images) {
    if (!images) return '';
    return String(images).split(',').filter(Boolean)[0] || '';
  },

  onTabsClick(e) {
    this.setData({ tab: e.detail.value });
  },

  gotoDetail(e) {
    wx.navigateTo({ url: `/pages/second-hand/order-detail/order-detail?id=${e.currentTarget.dataset.id}` });
  },

  errorText(error, fallback) {
    return friendlyError(error, fallback);
  },

  onPullDownRefresh() {
    this.loadOrders();
  },
});
