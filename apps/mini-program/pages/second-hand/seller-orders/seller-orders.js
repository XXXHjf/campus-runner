const feedback = require('../../../utils/feedback');
const secondHandService = require('../../../services/secondHandService');
const tokenManager = require('../../../utils/tokenManager');
const { orderStatus, orderStatusGroup, friendlyError } = require('../../../utils/secondHandStatus');
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
    hasMoreOrders: false,
    actionSheetVisible: false,
    actionOrder: {},
    actionBusy: false,
  },

  onLoad() {
    this.updateNavigationMetrics();
  },

  onShow() {
    this._hidden = false;
    return this.loadOrders();
  },

  onHide() { this._hidden = true; this._listVersion = (this._listVersion || 0) + 1; clearTimeout(this._searchTimer); },
  onUnload() { this._disposed = true; this.onHide(); },
  onReachBottom() { return this.loadOrders(true); },

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

  async loadOrders(append = false) {
    if (this._hidden || this._disposed || (append && (this.data.loading || !this.data.hasMoreOrders))) return;
    const token = tokenManager.getToken();
    const group = this.data.tabValue;
    const keyword = normalizeSearchText(this.data.keyword);
    const key = JSON.stringify([token, group, keyword]);
    if (append && key !== this._listKey) return this.loadOrders();
    if (token !== this._ownerToken) {
      this.setData({ actionSheetVisible: false, actionOrder: {} }); this._ownerToken = token;
    }
    if (key !== this._listKey) this.setData({ orders: [], filteredOrders: [], hasMoreOrders: false });
    this._listKey = key;
    const version = this._listVersion = (this._listVersion || 0) + 1;
    const page = append ? (this._page || 1) + 1 : 1;
    const current = () => !this._hidden && !this._disposed && version === this._listVersion
      && token === tokenManager.getToken() && group === this.data.tabValue && keyword === normalizeSearchText(this.data.keyword);
    feedback.loaded(this);
    this.setData({ loading: true });
    try {
      const rows = await secondHandService.listSellerOrders({ page, pageSize: 20, group, keyword });
      if (!current()) return;
      const decorated = rows.map(order => this.decorateOrder(order));
      const orders = append ? [...new Map([...this.data.orders, ...decorated].map(item => [String(item.id), item])).values()] : decorated;
      this._page = page;
      feedback.clearMessage(this, 'order-pagination');
      this.setData({ orders, filteredOrders: this.filterOrders(orders, group, keyword), hasMoreOrders: rows.length === 20 });
    } catch (error) {
      if (!current()) return;
      if (append) feedback.showMessage(this, '后续订单加载失败，请重试', { key: 'order-pagination', theme: 'error', action: '重试', onAction: () => this.loadOrders(true) });
      else feedback.loadError(this, '订单加载失败，请重试', () => this.loadOrders(), !!this.data.orders.length);
    } finally {
      if (current()) { this.setData({ loading: false }); wx.stopPullDownRefresh(); }
    }
  },

  decorateOrder(order) {
    const status = Number(order.status);
    const isOffline = String(order.tradeMode || '').toUpperCase() === 'OFFLINE';
    const isRefund = !isOffline && [5, 6, 7].includes(status);
    return {
      ...order,
      isOffline,
      isRefund,
      statusGroup: orderStatusGroup(status, order.tradeMode),
      statusText: orderStatus(status, order.tradeMode).text,
      statusKind: [3, 6, 9].includes(status) ? 'done'
        : status === 4 ? 'neutral'
          : [7, 10, 11].includes(status) ? 'warning'
            : [0, 5, 8].includes(status) ? 'pending' : 'active',
      coverImage: String(order.productImages || '').split(',').filter(Boolean)[0] || '',
      canCancel: isOffline && status === 1,
      canContact: isOffline || !!order.payTime,
      primaryAction: status === 1 ? '去交付'
        : status === 8 && !isOffline && order.transferState === 'WAIT_USER_CONFIRM' ? '确认收款' : '',
    };
  },

  filterOrders(orders, tabValue, keyword) {
    const needle = normalizeSearchText(keyword);
    return orders.filter((order) => (tabValue === 'all' || order.statusGroup === tabValue)
      && (!needle || normalizeSearchText([order.productTitle, order.buyerName, order.orderNumber].join(' ')).includes(needle)));
  },

  applyFilters(tabValue = this.data.tabValue, keyword = this.data.keyword) {
    clearTimeout(this._searchTimer);
    this.setData({ tabValue, keyword, filteredOrders: this.filterOrders(this.data.orders, tabValue, keyword) });
    return this.loadOrders();
  },

  selectTab(e) { return this.applyFilters(e.currentTarget.dataset.value); },
  onSearchChange(e) {
    const keyword = e.detail.value;
    this.setData({ keyword, filteredOrders: this.filterOrders(this.data.orders, this.data.tabValue, keyword) });
    clearTimeout(this._searchTimer);
    this._searchTimer = setTimeout(() => this.loadOrders(), 300);
  },
  onSearchSubmit(e) { return this.applyFilters(this.data.tabValue, e.detail.value); },
  clearSearch() { return this.applyFilters(this.data.tabValue, ''); },

  gotoDetail(e) {
    const id = e.currentTarget.dataset.id;
    if (id) wx.navigateTo({ url: `/pages/second-hand/order-detail/order-detail?id=${id}` });
  },

  onPrimaryAction(e) { this.gotoDetail(e); },

  contactCounterparty(e) {
    const id = e.currentTarget.dataset.id;
    if (id) showOrderContact(id, '买家');
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
      content: '取消后商品将重新展示，双方无需继续本次交易。确认取消？',
      confirmText: '取消订单',
      cancelText: '保留订单',
      confirmColor: '#d54941',
      success: async (res) => {
        if (!res.confirm || this.data.actionBusy) return;
        this.setData({ actionBusy: true });
        try {
          await secondHandService.cancelOrder(order.id, '卖家取消线下交易');
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

  onPullDownRefresh() { clearTimeout(this._searchTimer); return this.loadOrders(); },
});
