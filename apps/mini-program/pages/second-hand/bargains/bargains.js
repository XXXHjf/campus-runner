const secondHandService = require('../../../services/secondHandService');
const userService = require('../../../services/userService');
const tokenManager = require('../../../utils/tokenManager');
const { bargainStatus, friendlyError } = require('../../../utils/secondHandStatus');

Page({
  data: {
    tab: 'received',
    issued: [],
    received: [],
    summary: {
      pending: 0,
      accepted: 0,
      rejected: 0,
    },
    actionLoadingId: null,
  },

  onShow() {
    this.loadList();
  },

  async loadList() {
    try {
      const user = await this.getCurrentUser();
      const list = await secondHandService.listMyBargains();
      const mapped = list.map((item) => ({
        ...item,
        statusText: bargainStatus(item.status).text,
        statusTheme: bargainStatus(item.status).theme,
        isBuyer: user.id != null && Number(item.buyerId) === Number(user.id),
        isSeller: user.id != null && Number(item.sellerId) === Number(user.id),
      }));
      this.setData({
        issued: mapped.filter((item) => item.isBuyer),
        received: mapped.filter((item) => item.isSeller),
        summary: {
          pending: mapped.filter((item) => Number(item.status) === 0).length,
          accepted: mapped.filter((item) => Number(item.status) === 1).length,
          rejected: mapped.filter((item) => Number(item.status) === 2).length,
        },
      });
    } catch (error) {
      wx.showToast({ title: this.errorText(error, '加载失败'), icon: 'none' });
    } finally {
      wx.stopPullDownRefresh();
    }
  },

  async getCurrentUser() {
    const app = getApp();
    const cached = app.globalData.userInfo || wx.getStorageSync('userInfo') || {};
    if (cached.id != null) return cached;
    try {
      const fresh = await userService.getUserInfo();
      const userInfo = {
        ...cached,
        ...fresh,
        token: cached.token || tokenManager.getToken(),
      };
      app.globalData.userInfo = userInfo;
      wx.setStorageSync('userInfo', userInfo);
      return userInfo;
    } catch (error) {
      return cached;
    }
  },

  onTabsClick(e) {
    this.setData({ tab: e.detail.value });
  },

  accept(e) {
    if (this.data.actionLoadingId) return;
    const bargain = this.findBargain(e.currentTarget.dataset.id);
    if (!bargain) return;
    wx.showModal({
      title: '接受议价',
      content: `接受 ¥${bargain.offerPrice} 后，买家即可按该价格下单支付。`,
      confirmText: '接受',
      success: async (res) => {
        if (!res.confirm) return;
        try {
          this.setData({ actionLoadingId: bargain.id });
          const order = await secondHandService.acceptBargain(bargain.id, {
            productId: bargain.productId,
            bargainId: bargain.id,
          });
          wx.showToast({ title: '已接受', icon: 'success' });
          wx.navigateTo({ url: `/pages/second-hand/order-detail/order-detail?id=${order.id}` });
        } catch (error) {
          wx.showToast({ title: this.errorText(error, '接受失败'), icon: 'none' });
          this.loadList();
        } finally {
          this.setData({ actionLoadingId: null });
        }
      },
    });
  },

  reject(e) {
    if (this.data.actionLoadingId) return;
    const bargain = this.findBargain(e.currentTarget.dataset.id);
    if (!bargain) return;
    wx.showModal({
      title: '拒绝议价',
      content: '确认拒绝这次报价？',
      confirmText: '拒绝',
      confirmColor: '#d54941',
      success: async (res) => {
        if (!res.confirm) return;
        try {
          this.setData({ actionLoadingId: bargain.id });
          await secondHandService.rejectBargain(bargain.id);
          wx.showToast({ title: '已拒绝', icon: 'success' });
          this.loadList();
        } catch (error) {
          wx.showToast({ title: this.errorText(error, '拒绝失败'), icon: 'none' });
          this.loadList();
        } finally {
          this.setData({ actionLoadingId: null });
        }
      },
    });
  },

  gotoProduct(e) {
    wx.navigateTo({ url: `/pages/second-hand/detail/detail?id=${e.currentTarget.dataset.productId}` });
  },

  gotoOrder(e) {
    const orderId = e.currentTarget.dataset.orderId;
    if (orderId) {
      wx.navigateTo({ url: `/pages/second-hand/order-detail/order-detail?id=${orderId}` });
      return;
    }
    wx.showToast({ title: '订单尚未生成', icon: 'none' });
  },

  findBargain(id) {
    return [...this.data.issued, ...this.data.received].find((item) => Number(item.id) === Number(id));
  },

  errorText(error, fallback) {
    return friendlyError(error, fallback);
  },

  onPullDownRefresh() {
    this.loadList();
  },
});
