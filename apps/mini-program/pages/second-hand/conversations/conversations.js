const feedback = require('../../../utils/feedback');
const secondHandService = require('../../../services/secondHandService');
const tokenManager = require('../../../utils/tokenManager');

Page({
  retryFeedbackLoad() { return this._feedbackRetry?.(); },
  data: {
    conversations: [],
    loading: false,
    hasMore: true,
  },

  onShow() {
    this._hidden = false;
    return this.loadConversations();
  },

  onHide() {
    this._hidden = true;
    this._loadVersion = (this._loadVersion || 0) + 1;
    this._pending = null;
    this.setData({ loading: false });
  },
  onUnload() { this.onHide(); },

  loadConversations(append = false) {
    const token = tokenManager.getToken();
    const session = tokenManager.getSessionVersion?.();
    const scope = JSON.stringify([token, session]);
    if (this._scope !== scope) {
      this._scope = scope;
      this._loadVersion = (this._loadVersion || 0) + 1;
      this._pending = null;
      this._page = 0;
      this.setData({ conversations: [], hasMore: true, loading: false });
    }
    if (!token) {
      this._loadVersion = (this._loadVersion || 0) + 1;
      this._pending = null;
      this.setData({ conversations: [], loading: false, hasMore: false });
      wx.stopPullDownRefresh();
      return Promise.resolve(false);
    }
    if (append && (this._hidden || this.data.loading || !this.data.hasMore)) return Promise.resolve(false);
    const page = append && this._page ? this._page + 1 : 1;
    const key = JSON.stringify([scope, page]);
    if (this._pending?.key === key) return this._pending.promise;
    const version = (this._loadVersion || 0) + 1;
    this._loadVersion = version;
    const isCurrent = () => version === this._loadVersion && !this._hidden
      && token === tokenManager.getToken() && session === tokenManager.getSessionVersion?.();
    feedback.loaded(this);
    this.setData({ loading: true });
    const promise = (async () => {
      try {
        const rows = await secondHandService.listConversations({ page, pageSize: 20 });
        if (!isCurrent()) return false;
        const fresh = rows.map((item) => this.decorateConversation(item));
        let conversations = fresh;
        if (append && this._page) {
          const seen = new Set(this.data.conversations.map(item => item.key));
          conversations = [...this.data.conversations, ...fresh.filter(item => !seen.has(item.key))];
        }
        this._page = page;
        this.setData({ conversations, hasMore: rows.length === 20 });
        return true;
      } catch (error) {
        if (isCurrent()) feedback.loadError(this, '加载失败，请重试', () => this.loadConversations(append), !!this.data.conversations.length);
        return false;
      } finally {
        if (version === this._loadVersion) {
          this._pending = null;
          this.setData({ loading: false });
          wx.stopPullDownRefresh();
        }
      }
    })();
    this._pending = { key, promise };
    return promise;
  },

  onReachBottom() { return this._hidden ? Promise.resolve(false) : this.loadConversations(true); },

  decorateConversation(item) {
    return {
      ...item,
      key: `${item.productId}-${item.counterpartyId}`,
      coverImage: this.firstImage(item.productImages),
      timeText: this.formatTime(item.lastMessageTime),
    };
  },

  firstImage(images) {
    if (!images) return '';
    return String(images).split(',').filter(Boolean)[0] || '';
  },

  formatTime(value) {
    if (!value) return '';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return '';
    const now = new Date();
    if (date.toDateString() === now.toDateString()) {
      return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
    }
    return `${date.getMonth() + 1}-${date.getDate()}`;
  },

  gotoConversation(e) {
    const item = this.data.conversations[e.currentTarget.dataset.index];
    if (!item) return;
    const orderPart = item.orderId ? `&orderId=${item.orderId}` : '';
    wx.navigateTo({
      url: `/pages/second-hand/conversation/conversation?productId=${item.productId}&counterpartyId=${item.counterpartyId}${orderPart}`,
    });
  },

  onPullDownRefresh() {
    return this.loadConversations();
  },
});
