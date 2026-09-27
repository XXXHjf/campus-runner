const feedback = require('../../../utils/feedback');
const secondHandService = require('../../../services/secondHandService');
const userService = require('../../../services/userService');
const tokenManager = require('../../../utils/tokenManager');
const { bargainStatus, friendlyError } = require('../../../utils/secondHandStatus');

Page({
  retryFeedbackLoad() { return this._feedbackRetry?.(); },
  data: {
    refreshing: false,
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
    feedback.loaded(this);
    try {
      const user = await this.getCurrentUser();
      const list = await secondHandService.listMyBargains();
      const mapped = list.map((item) => ({
        ...item,
        coverImage: item.productCoverImage || '',
        statusText: Number(item.status) === 1 && !item.orderId ? '待买家下单' : bargainStatus(item.status).text,
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
      feedback.loadError(this, '议价记录加载失败，请重试', () => this.loadList(), !!(this.data.received.length + this.data.issued.length));
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
    feedback.showModal(this, {
      title: '接受议价',
      content: `同意报价 ¥${bargain.offerPrice}，买家将从商品支持的交付方式中选择并下单。下单前商品仍可购买。`,
      confirmText: '接受',
      success: async (res) => {
        if (!res.confirm) return;
        try {
          this.setData({ actionLoadingId: bargain.id });
          await secondHandService.acceptBargain(bargain.id);
          feedback.showToast(this, { title: '已接受', icon: 'success' });
          await this.loadList();
        } catch (error) {
          feedback.showToast(this, { title: this.errorText(error, '接受失败'), theme: 'error' });
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
    feedback.showModal(this, {
      title: '拒绝议价',
      content: '确认拒绝这次报价？',
      confirmText: '拒绝',
      confirmColor: '#d54941',
      success: async (res) => {
        if (!res.confirm) return;
        try {
          this.setData({ actionLoadingId: bargain.id });
          await secondHandService.rejectBargain(bargain.id);
          feedback.showToast(this, { title: '已拒绝', icon: 'success' });
          this.loadList();
        } catch (error) {
          feedback.showToast(this, { title: this.errorText(error, '拒绝失败'), theme: 'error' });
          this.loadList();
        } finally {
          this.setData({ actionLoadingId: null });
        }
      },
    });
  },

  async withdraw(e) {
    if (this.data.actionLoadingId != null) return;
    const bargain = this.findBargain(e.currentTarget.dataset.id);
    if (!bargain || !bargain.isBuyer || Number(bargain.status) !== 0) return;
    const res = await feedback.showModal(this, {
      title: '撤回报价？',
      content: '撤回后，卖家将无法接受这次报价。',
      cancelText: '继续保留',
      confirmText: '确认撤回',
    });
    if (!res.confirm || this.data.actionLoadingId != null) return;
    try {
      this.setData({ actionLoadingId: bargain.id });
      await secondHandService.withdrawBargain(bargain.id);
      // 先反映已确认的结果，即使随后刷新失败，也不再显示可撤回按钮。
      this.setData({
        issued: this.data.issued.map((item) => Number(item.id) === Number(bargain.id)
          ? { ...item, status: 4, statusText: bargainStatus(4).text, statusTheme: bargainStatus(4).theme }
          : item),
      });
      feedback.showToast(this, { title: '已撤回', icon: 'success' });
      await this.loadList();
    } catch (error) {
      feedback.showToast(this, { title: this.errorText(error, '撤回失败，请重试'), theme: 'error' });
      await this.loadList();
    } finally {
      this.setData({ actionLoadingId: null });
    }
  },

  noop() {},

  gotoRecord(e) {
    if (e.currentTarget.dataset.orderId) {
      this.gotoOrder(e);
      return;
    }
    const bargain = this.findBargain(e.currentTarget.dataset.id);
    if (bargain && bargain.isBuyer && Number(bargain.status) === 1) {
      this.buyBargain(e);
      return;
    }
    this.gotoProduct(e);
  },

  buyBargain(e) {
    const bargain = this.findBargain(e.currentTarget.dataset.id);
    if (!bargain || !bargain.isBuyer || Number(bargain.status) !== 1 || bargain.orderId) return;
    wx.navigateTo({ url: `/pages/second-hand/detail/detail?id=${bargain.productId}&bargainId=${bargain.id}` });
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
    feedback.showToast(this, { title: '订单尚未生成', theme: 'warning' });
  },

  findBargain(id) {
    return [...this.data.issued, ...this.data.received].find((item) => Number(item.id) === Number(id));
  },

  errorText(error, fallback) {
    return friendlyError(error, fallback);
  },

  async onPullDownRefresh() {
    if (this.data.refreshing) return;
    this.setData({ refreshing: true });
    try {
      await this.loadList();
    } finally {
      this.setData({ refreshing: false });
    }
  },
});
