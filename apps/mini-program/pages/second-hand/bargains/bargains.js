const feedback = require('../../../utils/feedback');
const secondHandService = require('../../../services/secondHandService');
const userService = require('../../../services/userService');
const tokenManager = require('../../../utils/tokenManager');
const { bargainStatus, friendlyError } = require('../../../utils/secondHandStatus');

Page({
  retryFeedbackLoad() { return this._feedbackRetry?.(); },
  data: {
    refreshing: false,
    loading: false,
    hasMore: true,
    tab: 'received',
    issued: [],
    received: [],
    summary: {
      pending: 0,
      accepted: 0,
      rejected: 0,
    },
    actionLoadingId: null,
    actionsReady: true,
    showReject: false,
    rejectBargainId: null,
    rejectPrice: '',
    rejectReasonCode: '',
    rejectNote: '',
    rejectSubmitting: false,
  },

  onLoad(options = {}) {
    this._notificationBargainId = /^[1-9]\d*$/.test(String(options.bargainId || '')) ? options.bargainId : null;
    this.setData({ tab: options.tab === 'issued' ? 'issued' : 'received' });
  },

  onShow() { this._hidden = false; return this.loadList(); },
  onHide() {
    this._hidden = true;
    this._rejectVersion = (this._rejectVersion || 0) + 1;
    this._listVersion = (this._listVersion || 0) + 1;
    this.setData({ loading: false, refreshing: false, showReject: false, rejectSubmitting: false, actionLoadingId: null });
  },
  onUnload() { this._disposed = true; this.onHide(); },
  loadMore() { return this.loadList(true); },

  async loadList(append = false) {
    if (this._hidden || this._disposed || (append && (this.data.loading || !this.data.hasMore))) return false;
    if (append && !this.data.actionsReady) append = false;
    const token = tokenManager.getToken();
    const session = tokenManager.getSessionVersion?.();
    const tab = this.data.tab;
    const scope = JSON.stringify([token, session, tab]);
    if (append && scope !== this._listScope) append = false;
    const changed = scope !== this._listScope;
    this._listScope = scope;
    const version = this._listVersion = (this._listVersion || 0) + 1;
    const page = append ? (this._page || 1) + 1 : 1;
    const current = () => !this._hidden && !this._disposed && version === this._listVersion
      && token === tokenManager.getToken() && session === tokenManager.getSessionVersion?.() && tab === this.data.tab;
    if (changed) {
      this._rejectVersion = (this._rejectVersion || 0) + 1;
      this.setData({ issued: [], received: [], hasMore: true, actionLoadingId: null, showReject: false, rejectSubmitting: false });
    }
    feedback.loaded(this);
    this.setData({ loading: true, ...(!append ? { actionsReady: false } : {}) });
    try {
      const user = await this.getCurrentUser();
      if (!current()) return false;
      const list = await secondHandService.listMyBargains({ page, pageSize: 20, role: tab });
      if (!current()) return false;
      let notificationRows = [];
      if (!append && tab === 'issued' && this._notificationBargainId
          && !list.some(item => String(item.id) === String(this._notificationBargainId))) {
        notificationRows = await secondHandService.listMyBargains({ page: 1, pageSize: 1, role: tab, bargainId: this._notificationBargainId });
        if (!current()) return false;
      }
      const mapped = [...notificationRows, ...list].map(item => ({
        ...item, coverImage: item.productCoverImage || '',
        statusText: Number(item.status) === 1 && !item.orderId ? '等待买家下单' : bargainStatus(item.status).text,
        statusTheme: bargainStatus(item.status).theme,
        isBuyer: user.id != null && Number(item.buyerId) === Number(user.id),
        isSeller: user.id != null && Number(item.sellerId) === Number(user.id),
        rejectReasonText: Number(item.status) === 2 ? (item.rejectReasonText || '卖家未填写原因') : '',
      }));
      const rows = mapped.filter(item => tab === 'issued' ? item.isBuyer : item.isSeller);
      const combined = append ? [...this.data[tab]] : [];
      const seen = new Set(combined.map(item => String(item.id)));
      for (const row of rows) {
        if (seen.has(String(row.id))) continue;
        seen.add(String(row.id)); combined.push(row);
      }
      this._page = page;
      this.setData({ [tab]: combined, hasMore: list.length === 20, actionsReady: true });
      if (this.data.showReject) {
        const quote = this.findBargain(this.data.rejectBargainId);
        if (!quote || Number(quote.status) !== 0) this.setData({ showReject: false });
      }
      return true;
    } catch (error) {
      if (current()) feedback.loadError(this, '议价记录加载失败，请重试', () => this.loadList(append), !!(this.data.received.length + this.data.issued.length));
      return false;
    } finally {
      if (current()) this.setData({ loading: false });
    }
  },

  async getCurrentUser() {
    const token = tokenManager.getToken();
    const session = tokenManager.getSessionVersion?.();
    const app = getApp();
    const cached = app.globalData.userInfo || wx.getStorageSync('userInfo') || {};
    if (cached.id != null && cached.token === token) return cached;
    const fresh = await userService.getUserInfo();
    if (token !== tokenManager.getToken() || session !== tokenManager.getSessionVersion?.()) throw new Error('登录状态已变化，请刷新');
    const userInfo = { ...fresh, token };
    app.globalData.userInfo = userInfo;
    wx.setStorageSync('userInfo', userInfo);
    return userInfo;
  },

  onTabsClick(e) {
    this.setData({ tab: e.detail.value });
    return this.loadList();
  },

  accept(e) {
    if (this.data.actionLoadingId || !this.data.actionsReady || this.data.loading) return;
    const bargain = this.findBargain(e.currentTarget.dataset.id);
    if (!bargain || !bargain.isSeller || Number(bargain.status) !== 0) return;
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
    if (this.data.actionLoadingId || !this.data.actionsReady || this.data.loading) return;
    const bargain = this.findBargain(e.currentTarget.dataset.id);
    if (!bargain || !bargain.isSeller || Number(bargain.status) !== 0) return;
    this._rejectVersion = (this._rejectVersion || 0) + 1;
    this.setData({
      showReject: true, rejectBargainId: bargain.id, rejectPrice: bargain.offerPrice,
      rejectReasonCode: '', rejectNote: '',
    });
  },

  closeReject() {
    if (!this.data.rejectSubmitting) this.setData({ showReject: false });
  },

  selectRejectReason(e) {
    if (this.data.rejectSubmitting) return;
    this.setData({ rejectReasonCode: e.detail.value, rejectNote: '' });
  },

  changeRejectNote(e) {
    if (!this.data.rejectSubmitting) this.setData({ rejectNote: e.detail.value });
  },

  async submitReject() {
    if (this.data.rejectSubmitting || this.data.actionLoadingId != null || !this.data.actionsReady
        || !this.data.showReject || !this.data.rejectReasonCode) return;
    const bargain = this.findBargain(this.data.rejectBargainId);
    if (!bargain || !bargain.isSeller || Number(bargain.status) !== 0) {
      this.setData({ showReject: false });
      return;
    }
    const reason = { reasonCode: this.data.rejectReasonCode,
      note: this.data.rejectReasonCode === 'CUSTOM' ? this.data.rejectNote.trim() : null };
    const token = tokenManager.getToken();
    const session = tokenManager.getSessionVersion?.();
    const version = this._rejectVersion;
    const current = () => !this._hidden && !this._disposed && token === tokenManager.getToken()
      && session === tokenManager.getSessionVersion?.() && version === this._rejectVersion;
    this.setData({ actionLoadingId: bargain.id, rejectSubmitting: true });
    try {
      await secondHandService.rejectBargain(bargain.id, reason);
      if (!current()) return;
      const reasonText = reason.reasonCode === 'CUSTOM' ? (reason.note || '卖家未填写原因')
        : reason.reasonCode === 'NOT_SELLING_NOW' ? '暂时不打算出售' : '暂不考虑这个价格';
      this.setData({ showReject: false, received: this.data.received.map(item => Number(item.id) === Number(bargain.id)
        ? { ...item, status: 2, statusText: '已拒绝', statusTheme: bargainStatus(2).theme, rejectReasonText: reasonText } : item) });
      feedback.showToast(this, { title: '已拒绝', icon: 'success' });
      await this.loadList();
    } catch (error) {
      if (!current()) return;
      feedback.showToast(this, { title: this.errorText(error, '拒绝失败，请重试'), theme: 'error' });
      await this.loadList();
    } finally {
      if (version === this._rejectVersion) this.setData({ actionLoadingId: null, rejectSubmitting: false });
    }
  },

  async withdraw(e) {
    if (this.data.actionLoadingId != null || !this.data.actionsReady || this.data.loading) return;
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
    if (!this.data.actionsReady || this.data.loading) return;
    const bargain = this.findBargain(e.currentTarget.dataset.id);
    if (!bargain || !bargain.isBuyer || Number(bargain.status) !== 1 || bargain.orderId) return;
    wx.navigateTo({ url: `/pages/second-hand/detail/detail?id=${bargain.productId}&bargainId=${bargain.id}` });
  },

  gotoProduct(e) {
    const bargain = this.findBargain(e.currentTarget.dataset.id);
    if (bargain && bargain.productDeleted) return;
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
