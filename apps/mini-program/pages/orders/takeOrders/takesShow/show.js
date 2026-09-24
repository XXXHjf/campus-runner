const takeOrderService = require('../../../../services/takeOrderService');
const userOrderService = require('../../../../services/userOrderService');
const { showError } = require('../../../../utils/transformers');
const { checkCilcleToast } = require('../../../../utils/commonJs');

function normalizeSearchText(value) {
  return String(value == null ? '' : value).replace(/\s+/g, '').toLocaleLowerCase();
}

function formatMoney(value) {
  const amount = Number(value);
  return Number.isFinite(amount) ? amount.toFixed(2) : '0.00';
}

Page({
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
      { label: '待确认', value: 'confirming' },
      { label: '待收款', value: 'settling' },
      { label: '已完成', value: 'completed' },
      { label: '已取消', value: 'canceled' },
      { label: '异常', value: 'exception' },
    ],
    orders: [],
    filteredOrders: [],
    loading: false,
    actionSheetVisible: false,
    actionOrder: {},
    actionBusy: false,
  },

  updateNavigationMetrics() {
    const windowInfo = typeof wx.getWindowInfo === 'function'
      ? wx.getWindowInfo()
      : wx.getSystemInfoSync();
    const menuButton = wx.getMenuButtonBoundingClientRect();
    const statusBarHeight = windowInfo.statusBarHeight || 0;
    const hasMenuButton = menuButton && menuButton.width > 0 && menuButton.left > 0;
    const menuGap = hasMenuButton ? Math.max(0, menuButton.top - statusBarHeight) : 0;
    const navigationBarHeight = hasMenuButton ? Math.max(44, menuButton.height + menuGap * 2) : 44;
    this.setData({
      statusBarHeight,
      navigationBarHeight,
      navigationRightPadding: hasMenuButton
        ? Math.max(96, windowInfo.windowWidth - menuButton.left + 4)
        : 96,
      controlsHeight: statusBarHeight + navigationBarHeight + windowInfo.windowWidth * 84 / 750,
    });
  },

  goBack() {
    if (getCurrentPages().length > 1) {
      wx.navigateBack({ delta: 1 });
    } else {
      wx.switchTab({ url: '/pages/mine/mine/mine' });
    }
  },

  gotoOrderInfo(event) {
    const orderId = event.currentTarget.dataset.id;
    if (orderId) wx.navigateTo({ url: `/pages/orders/takeOrders/takesInfo/info?id=${orderId}` });
  },

  getStatusDescription(order) {
    const status = Number(order.status);
    if (status === 1) return order.businessType === 'PURCHASE' ? '待购买' : '已接单';
    if (status === 2) return order.businessType === 'PURCHASE' ? '配送中' : '派送中';
    if (status === 5) return order.price == null ? '已完成' : '待收款';
    const descriptions = {
      '-4': '退款异常', '-3': '退款成功', '-2': '退款中', '-1': '待支付',
      0: '待接单', 3: '已送达', 4: '已取消', 6: '已完成', 7: '收款失败',
    };
    return descriptions[status] || '订单状态';
  },

  getStatusGroup(order) {
    const status = Number(order.status);
    if (status === 1 || status === 2) return 'processing';
    if (status === 3) return 'confirming';
    if (status === 5) return order.price == null ? 'completed' : 'settling';
    if (status === 6) return 'completed';
    if (status === 4) return 'canceled';
    return 'exception';
  },

  getStatusKind(order) {
    const status = Number(order.status);
    if (status === 1 && order.businessType === 'PURCHASE') return 'pending';
    if (status === 1 || status === 2 || status === 3) return 'active';
    if (status === 5 && order.price != null) return 'pending';
    if (status === 5 || status === 6) return 'done';
    if (status === 4 || status === -3) return 'neutral';
    return 'warning';
  },

  getPrimaryAction(order) {
    const status = Number(order.status);
    if (status === 1) return order.businessType === 'PURCHASE' ? '查看任务' : '取件完成';
    if (status === 2) return '上传送达图';
    if (status === 5 && order.price != null) return '确认收款';
    if (status === 7 && order.price != null) return '处理收款';
    return '';
  },

  prepareOrder(order) {
    const isPurchase = order.businessType === 'PURCHASE';
    const receivable = isPurchase
      ? (order.runnerReceivable ?? order.runner_receivable ?? Number(order.productAmount || 0) + Number(order.price || 0))
      : order.price;
    const prepared = {
      ...order,
      displayName: String(order.username || '').trim() || '微信用户',
      displayNote: String(order.note || '').replace(/\s+/g, ' ').trim() || order.categoryName || '订单',
      displayAmount: formatMoney(receivable),
      showAmount: receivable != null && Number(receivable) > 0,
      statusDesc: this.getStatusDescription(order),
      statusGroup: this.getStatusGroup(order),
      statusKind: this.getStatusKind(order),
      primaryAction: this.getPrimaryAction(order),
    };
    prepared.searchText = normalizeSearchText([
      prepared.displayName, prepared.categoryName, prepared.note,
      prepared.pickUpAddress, prepared.reciveAddress, prepared.statusDesc,
      prepared.orderNumber, prepared.createTime, prepared.takeOrderCreateTime,
    ].join(' '));
    return prepared;
  },

  applyFilters() {
    const keyword = normalizeSearchText(this.data.keyword);
    const filteredOrders = this.data.orders.filter(order =>
      (this.data.tabValue === 'all' || order.statusGroup === this.data.tabValue)
      && (!keyword || order.searchText.includes(keyword))
    );
    this.setData({ filteredOrders });
  },

  onSearchChange(event) {
    this.setData({ keyword: event.detail.value }, () => this.applyFilters());
  },

  onSearchSubmit() {
    this.applyFilters();
    wx.hideKeyboard();
  },

  clearSearch() {
    this.setData({ keyword: '' }, () => this.applyFilters());
  },

  selectTab(event) {
    this.setData({ tabValue: event.currentTarget.dataset.value }, () => this.applyFilters());
  },

  async onPrimaryAction(event) {
    if (this.data.actionBusy) return;
    const order = this.data.orders.find(item => String(item.orderId) === String(event.currentTarget.dataset.id));
    if (!order || !order.primaryAction) return;
    if (Number(order.status) !== 1 || order.businessType === 'PURCHASE') {
      this.gotoOrderInfo(event);
      return;
    }
    this.setData({ actionBusy: true });
    let shouldReload = false;
    try {
      const latest = await userOrderService.getMyOrderDetail(order.orderId);
      if (Number(latest.status) !== 1 || latest.businessType === 'PURCHASE') {
        wx.showToast({ title: '订单状态已变化，请刷新', icon: 'none' });
        shouldReload = true;
        return;
      }
      const confirmed = await new Promise(resolve => wx.showModal({
        title: '取件完成',
        content: '确认已取到物品？',
        success: ({ confirm }) => resolve(confirm),
        fail: () => resolve(false),
      }));
      if (!confirmed) return;
      shouldReload = true;
      const result = await takeOrderService.updateTakeOrderStatus({ id: order.id, status: 1 });
      if (result.code !== 1) throw new Error(result.msg || '操作失败');
      wx.showToast({ title: '已开始派送', icon: 'success' });
    } catch (error) {
      showError(shouldReload ? '操作失败，请刷新订单后重试' : '获取订单失败，请重试');
    } finally {
      if (shouldReload) await this.loadOrders();
      this.setData({ actionBusy: false });
    }
  },

  openActionSheet(event) {
    const order = this.data.orders.find(item => String(item.orderId) === String(event.currentTarget.dataset.id));
    if (order) this.setData({ actionOrder: order, actionSheetVisible: true });
  },

  onActionSheetChange(event) {
    this.setData({ actionSheetVisible: event.detail.visible });
  },

  closeActionSheet() {
    this.setData({ actionSheetVisible: false });
  },

  showActionOrderDetail() {
    const orderId = this.data.actionOrder.orderId;
    this.closeActionSheet();
    if (orderId) wx.navigateTo({ url: `/pages/orders/takeOrders/takesInfo/info?id=${orderId}` });
  },

  callSender() {
    const phoneNumber = this.data.actionOrder.phone;
    this.closeActionSheet();
    if (phoneNumber) wx.makePhoneCall({ phoneNumber: String(phoneNumber) });
  },

  async loadOrders() {
    this.setData({ loading: true });
    try {
      const result = await takeOrderService.getMyTakeOrders();
      const orders = (Array.isArray(result) ? result : []).map(order => this.prepareOrder(order));
      this.setData({ orders }, () => this.applyFilters());
      return true;
    } catch (error) {
      console.error('获取接单失败:', error);
      showError('获取接单失败，请重试');
      return false;
    } finally {
      this.setData({ loading: false });
    }
  },

  onLoad() {
    this.updateNavigationMetrics();
  },

  onResize() {
    this.updateNavigationMetrics();
  },

  onShow() {
    this.loadOrders();
  },

  async onPullDownRefresh() {
    const success = await this.loadOrders();
    wx.stopPullDownRefresh();
    if (success) checkCilcleToast(this, '刷新成功');
  },
});
