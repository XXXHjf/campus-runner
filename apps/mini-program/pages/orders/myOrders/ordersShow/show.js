const userOrderService = require('../../../../services/userOrderService');
const { showError } = require('../../../../utils/transformers');
const { checkCilcleToast } = require('../../../../utils/commonJs');
const mediaService = require('../../../../services/mediaService');
const { validateNote } = require('../../../../utils/validators');

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
      { label: '待支付', value: 'unpaid' },
      { label: '待接单', value: 'waiting' },
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
    editVisible: false,
    editOrder: {},
    editNote: '',
    editFiles: [],
    editImageAssetId: null,
    editSaving: false,
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
    const id = event.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/orders/myOrders/ordersInfo/info?id=${id}` });
  },

  getPrimaryAction(status) {
    if (status === -1) return '去支付';
    if (status === 0) return '编辑';
    if (status === 3) return '确认订单';
    return '';
  },

  onPrimaryAction(event) {
    const id = event.currentTarget.dataset.id;
    const order = this.data.orders.find(item => String(item.id) === String(id));
    if (!order) return;
    if (Number(order.status) === 0) {
      this.openEdit(event);
      return;
    }
    if (Number(order.status) !== 3) {
      this.gotoOrderInfo(event);
      return;
    }
    wx.showModal({
      title: '确认订单',
      content: '确认已收到物品并完成订单？',
      success: async (result) => {
        if (!result.confirm) return;
        try {
          await userOrderService.confirmOrder(id);
          wx.showToast({ title: '确认成功', icon: 'success' });
          this.loadOrders();
        } catch (error) {
          console.error('确认订单失败:', error);
          showError('确认失败，请重试');
        }
      },
    });
  },

  openActionSheet(event) {
    const id = event.currentTarget.dataset.id;
    const order = this.data.orders.find(item => String(item.id) === String(id));
    if (order) this.setData({ actionOrder: order, actionSheetVisible: true });
  },

  onActionSheetChange(event) {
    this.setData({ actionSheetVisible: event.detail.visible });
  },

  closeActionSheet() {
    this.setData({ actionSheetVisible: false });
  },

  showActionOrderDetail() {
    const id = this.data.actionOrder.id;
    this.closeActionSheet();
    if (id) wx.navigateTo({ url: `/pages/orders/myOrders/ordersInfo/info?id=${id}` });
  },

  cancelActionOrder() {
    const order = this.data.actionOrder;
    this.closeActionSheet();
    if (!order || ![-1, 0].includes(Number(order.status))) return;
    wx.showModal({
      title: '取消订单',
      content: Number(order.status) === -1 ? '取消后订单无法恢复，确定取消吗？' : '取消后订单无法恢复，已支付的金额将申请退款。',
      confirmText: '取消订单',
      confirmColor: '#d54941',
      success: async (result) => {
        if (!result.confirm) return;
        try {
          await userOrderService.cancelOrder(order.id, '发单人取消订单', order.orderNumber);
          const needsRefund = Number(order.status) === 0 && Number(order.pay_amount ?? order.payAmount ?? 0) > 0 && !!order.orderNumber;
          if (needsRefund) {
            try {
              await userOrderService.refundOrder(order.orderNumber, '发单人取消订单');
            } catch (error) {
              console.error('退款申请失败:', error);
              showError('订单已取消，退款申请未成功，请联系客服');
              this.loadOrders();
              return;
            }
          }
          wx.showToast({ title: needsRefund ? '已发起退款' : '已取消', icon: 'none' });
          this.loadOrders();
        } catch (error) {
          console.error('取消订单失败:', error);
          showError('取消失败，请重试');
          this.loadOrders();
        }
      },
    });
  },

  async openEdit(event) {
    const id = event.currentTarget.dataset.id;
    try {
      const order = await userOrderService.getMyOrderDetail(id);
      if (Number(order.status) !== 0) {
        wx.showToast({ title: '订单状态已变化，请刷新', icon: 'none' });
        this.loadOrders();
        return;
      }
      this.setData({
        editOrder: order,
        editNote: order.note || '',
        editImageAssetId: order.imageAssetId || null,
        editFiles: order.imageAssetId && order.image ? [{ url: order.image, mediaId: order.imageAssetId, status: 'done' }] : [],
        editVisible: true,
      });
    } catch (error) {
      showError('打开编辑失败，请重试');
    }
  },

  onEditNoteChange(event) {
    this.setData({ editNote: event.detail.value });
  },

  async onEditImageAdd(event) {
    const file = event.detail.files && event.detail.files[0];
    if (!file) return;
    this.setData({ editFiles: [{ ...file, status: 'loading' }], editImageAssetId: null });
    try {
      const uploaded = await mediaService.uploadImage(file.url, 'ORDER_IMAGE');
      if (!this.data.editVisible) {
        await mediaService.releaseTemporaryImage(uploaded.mediaId).catch(() => {});
        return;
      }
      this._editTempMediaId = uploaded.mediaId;
      this.setData({ editFiles: [{ url: uploaded.previewUrl, mediaId: uploaded.mediaId, status: 'done' }], editImageAssetId: uploaded.mediaId });
    } catch (error) {
      this.setData({ editFiles: [], editImageAssetId: null });
      showError('图片上传失败，请重试');
    }
  },

  onEditImageRemove() {
    if (this._editTempMediaId) {
      mediaService.releaseTemporaryImage(this._editTempMediaId).catch(() => {});
      this._editTempMediaId = null;
    }
    this.setData({ editFiles: [], editImageAssetId: null });
  },

  onEditVisibleChange(event) {
    if (!event.detail.visible) this.closeEdit();
  },

  closeEdit() {
    if (this.data.editSaving) return;
    if (this._editTempMediaId) {
      mediaService.releaseTemporaryImage(this._editTempMediaId).catch(() => {});
      this._editTempMediaId = null;
    }
    this.setData({ editVisible: false });
  },

  async saveEdit() {
    if (this.data.editSaving) return;
    const validation = validateNote(this.data.editNote);
    if (!validation.valid) {
      showError(validation.message);
      return;
    }
    if (!this.data.editImageAssetId || !this.data.editFiles.some(file => file.status === 'done')) {
      showError('请上传一张说明图片');
      return;
    }
    this.setData({ editSaving: true });
    try {
      await userOrderService.updateOrderContent(this.data.editOrder.id, {
        note: this.data.editNote.trim(), imageAssetId: this.data.editImageAssetId,
      });
      this._editTempMediaId = null;
      this.setData({ editVisible: false });
      wx.showToast({ title: '保存成功', icon: 'success' });
      this.loadOrders();
    } catch (error) {
      showError('保存失败，请刷新订单后重试');
      this.loadOrders();
    } finally {
      this.setData({ editSaving: false });
    }
  },

  onShareAppMessage() {
    const order = this.data.actionOrder;
    this.closeActionSheet();
    if (!order || Number(order.status) !== 0) {
      return { title: '帮帮校园送', path: '/pages/index/index' };
    }
    return {
      title: order.displayNote || '帮帮校园送',
      path: `/pages/orders/takeOrders/takesInfo/info?id=${order.id}`,
      ...(order.image ? { imageUrl: order.image } : {}),
    };
  },

  getStatusDescription(order) {
    const status = Number(order.status);
    if (order.businessType === 'PURCHASE' && status === 1) return '待购买';
    if (order.businessType === 'PURCHASE' && status === 2) return '配送中';
    const descriptions = {
      '-4': '退款异常', '-3': '退款成功', '-2': '退款中', '-1': '待支付',
      0: '待接单', 1: '已接单', 2: '派送中', 3: '已送达',
      4: '已取消', 5: '已完成', 6: '提现成功', 7: '提现失败',
    };
    return descriptions[status] || '订单状态';
  },

  getStatusKind(order) {
    const status = Number(order.status);
    if (status === -1 || status === 0 || (order.businessType === 'PURCHASE' && status === 1)) return 'pending';
    if (status === 1 || status === 2 || status === 3) return 'active';
    if (status === 5 || status === 6 || status === -3) return 'done';
    if (status === -4 || status === 7) return 'warning';
    return 'neutral';
  },

  getStatusGroup(order) {
    const status = Number(order.status);
    if (status === -1) return 'unpaid';
    if (status === 0) return 'waiting';
    if (status === 1 || status === 2 || status === 3) return 'processing';
    if (status === 5 || status === 6 || status === 7) return 'completed';
    if (status === 4) return 'canceled';
    if (status === -2 || status === -3 || status === -4) return 'refund';
    return 'all';
  },

  prepareOrder(order) {
    const payAmount = order.pay_amount ?? order.payAmount ?? 0;
    const prepared = {
      ...order,
      displayNote: String(order.note || '').replace(/\s+/g, ' ').trim() || order.categoryName,
      displayPayAmount: formatMoney(payAmount),
      showPayAmount: Number(payAmount) > 0,
      primaryAction: this.getPrimaryAction(Number(order.status)),
      statusDesc: this.getStatusDescription(order),
      statusKind: this.getStatusKind(order),
      statusGroup: this.getStatusGroup(order),
    };
    prepared.searchText = normalizeSearchText([
      prepared.categoryName, prepared.note, prepared.pickUpAddress,
      prepared.reciveAddress, prepared.statusDesc, prepared.orderNumber,
      prepared.createTime, prepared.displayPayAmount,
    ].join(' '));
    return prepared;
  },

  matchesTab(order, tabValue) {
    return tabValue === 'all' || order.statusGroup === tabValue;
  },

  applyFilters() {
    const keyword = normalizeSearchText(this.data.keyword);
    const filteredOrders = this.data.orders.filter(order =>
      this.matchesTab(order, this.data.tabValue) && (!keyword || order.searchText.includes(keyword))
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

  async loadOrders() {
    this.setData({ loading: true });
    try {
      const result = await userOrderService.getMyOrders();
      const orders = (Array.isArray(result) ? result : []).map(order => this.prepareOrder(order));
      this.setData({ orders }, () => this.applyFilters());
      return true;
    } catch (error) {
      console.error('获取订单失败:', error);
      showError('获取订单失败，请重试');
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
