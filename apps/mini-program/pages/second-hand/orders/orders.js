const feedback = require('../../../utils/feedback');
const secondHandService = require('../../../services/secondHandService');
const { orderStatus, friendlyError } = require('../../../utils/secondHandStatus');
const { showOrderContact } = require('../../../utils/secondHandContact');

function normalizeSearchText(value) {
  return String(value == null ? '' : value).replace(/\s+/g, '').toLocaleLowerCase();
}

Page({
  retryFeedbackLoad() { return this._feedbackRetry?.(); },
  data: {
    statusBarHeight: 0,
    navigationBarHeight: 44,
    navigationRightPadding: 96,
    controlsHeight: 88,
    keyword: '',
    tabValue: 'all',
    filterOptions: [
      { label: '全部', value: 'all' },
      { label: '进行中', value: 'processing' },
      { label: '已完成', value: 'completed' },
      { label: '已取消', value: 'canceled' },
      { label: '退款', value: 'refund' },
    ],
    orders: [],
    filteredOrders: [],
    loading: false,
    actionSheetVisible: false,
    actionOrder: {},
    actionBusy: false,
  },

  onLoad() {
    this.updateNavigationMetrics();
  },

  onShow() {
    this.loadOrders();
  },

  updateNavigationMetrics() {
    const windowInfo = typeof wx.getWindowInfo === 'function'
      ? wx.getWindowInfo() : wx.getSystemInfoSync();
    const menuButton = wx.getMenuButtonBoundingClientRect();
    const statusBarHeight = windowInfo.statusBarHeight || 0;
    const hasMenuButton = menuButton && menuButton.width > 0 && menuButton.left > 0;
    const menuGap = hasMenuButton ? Math.max(0, menuButton.top - statusBarHeight) : 0;
    const navigationBarHeight = hasMenuButton ? Math.max(44, menuButton.height + menuGap * 2) : 44;
    this.setData({
      statusBarHeight,
      navigationBarHeight,
      navigationRightPadding: hasMenuButton
        ? Math.max(96, windowInfo.windowWidth - menuButton.left + 4) : 96,
      controlsHeight: statusBarHeight + navigationBarHeight + windowInfo.windowWidth * 84 / 750,
    });
  },

  goBack() {
    if (getCurrentPages().length > 1) wx.navigateBack({ delta: 1 });
    else wx.switchTab({ url: '/pages/mine/mine/mine' });
  },

  async loadOrders() {
    feedback.loaded(this);
    this.setData({ loading: true });
    try {
      const orders = (await secondHandService.listBuyerOrders()).map((order) => this.decorateOrder(order));
      this.setData({ orders, filteredOrders: this.filterOrders(orders, this.data.tabValue, this.data.keyword) });
    } catch (error) {
      feedback.loadError(this, '订单加载失败，请重试', () => this.loadOrders(), !!this.data.orders.length);
    } finally {
      this.setData({ loading: false });
      wx.stopPullDownRefresh();
    }
  },

  decorateOrder(order) {
    const status = Number(order.status);
    const isOffline = String(order.tradeMode || '').toUpperCase() === 'OFFLINE';
    const isRefund = !isOffline && [5, 6, 7].includes(status);
    let statusGroup = 'refund';
    if ([0, 1, 2, 8].includes(status)) statusGroup = 'processing';
    else if ([3, 9].includes(status)) statusGroup = 'completed';
    else if (status === 4) statusGroup = 'canceled';
    else if (isRefund) statusGroup = 'refund';
    return {
      ...order,
      isOffline,
      isRefund,
      statusGroup,
      statusText: orderStatus(status, order.tradeMode).text,
      statusKind: [3, 6, 9].includes(status) ? 'done'
        : status === 4 ? 'neutral'
          : [7, 10, 11].includes(status) ? 'warning'
            : [0, 5, 8].includes(status) ? 'pending' : 'active',
      coverImage: String(order.productImages || '').split(',').filter(Boolean)[0] || '',
      canCancel: isOffline ? status === 1 : [0, 1].includes(status),
      canContact: isOffline || !!order.payTime,
      primaryAction: status === 2 ? '去确认' : '',
    };
  },

  filterOrders(orders, tabValue, keyword) {
    const needle = normalizeSearchText(keyword);
    return orders.filter((order) => (tabValue === 'all' || order.statusGroup === tabValue)
      && (!needle || normalizeSearchText([order.productTitle, order.sellerName, order.orderNumber].join(' ')).includes(needle)));
  },

  applyFilters(tabValue = this.data.tabValue, keyword = this.data.keyword) {
    this.setData({ tabValue, keyword, filteredOrders: this.filterOrders(this.data.orders, tabValue, keyword) });
  },

  selectTab(e) { this.applyFilters(e.currentTarget.dataset.value); },
  onSearchChange(e) { this.applyFilters(this.data.tabValue, e.detail.value); },
  onSearchSubmit(e) { this.applyFilters(this.data.tabValue, e.detail.value); },
  clearSearch() { this.applyFilters(this.data.tabValue, ''); },

  gotoDetail(e) {
    const id = e.currentTarget.dataset.id;
    if (id) wx.navigateTo({ url: `/pages/second-hand/order-detail/order-detail?id=${id}` });
  },

  onPrimaryAction(e) { this.gotoDetail(e); },

  contactCounterparty(e) {
    const id = e.currentTarget.dataset.id;
    if (id) showOrderContact(id, '卖家');
  },

  openActionSheet(e) {
    const order = this.data.orders.find((item) => Number(item.id) === Number(e.currentTarget.dataset.id));
    if (order) this.setData({ actionSheetVisible: true, actionOrder: order });
  },

  closeActionSheet() { this.setData({ actionSheetVisible: false }); },
  onActionSheetChange(e) { if (!e.detail.visible) this.closeActionSheet(); },

  showActionOrderDetail() {
    const id = this.data.actionOrder.id;
    this.closeActionSheet();
    if (id) wx.navigateTo({ url: `/pages/second-hand/order-detail/order-detail?id=${id}` });
  },

  cancelActionOrder() {
    const order = this.data.actionOrder;
    this.closeActionSheet();
    if (!order.canCancel || this.data.actionBusy) return;
    feedback.showModal(this, {
      title: '取消订单？',
      content: order.isOffline ? '取消后双方无需继续本次交易。确认取消？'
        : (Number(order.status) === 1 ? '卖家交付前取消会发起退款，确认继续？' : '确认取消订单？'),
      confirmText: '取消订单',
      cancelText: '保留订单',
      confirmColor: '#d54941',
      success: async (res) => {
        if (!res.confirm || this.data.actionBusy) return;
        this.setData({ actionBusy: true });
        try {
          await secondHandService.cancelOrder(order.id, order.isOffline ? '买家取消线下交易' : '买家取消订单');
          feedback.showToast(this, { title: '已取消', icon: 'success' });
        } catch (error) {
          feedback.showToast(this, { title: friendlyError(error, '取消失败'), theme: 'error' });
        } finally {
          this.setData({ actionBusy: false });
          this.loadOrders();
        }
      },
    });
  },

  onPullDownRefresh() { this.loadOrders(); },
});
