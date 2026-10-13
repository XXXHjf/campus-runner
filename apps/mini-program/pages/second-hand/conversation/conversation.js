const feedback = require('../../../utils/feedback');
const subscriptions = require('../../../services/subscriptionService');
const secondHandService = require('../../../services/secondHandService');
const userService = require('../../../services/userService');
const { friendlyError } = require('../../../utils/secondHandStatus');
const tokenManager = require('../../../utils/tokenManager');

Page({
  retryFeedbackLoad() { return this._feedbackRetry?.(); },
  data: {
    productId: null,
    counterpartyId: null,
    orderId: null,
    currentUserId: null,
    product: {},
    messages: [],
    messageContent: '',
    counterpartyPhone: '',
    counterpartyPhoneMasked: '',
    sending: false,
    loading: false,
    loadingHistory: false,
    hasOlder: true,
    lastMessageAnchor: '',
  },

  onLoad(options) {
    this.setData({
      productId: Number(options.productId),
      counterpartyId: Number(options.counterpartyId),
      orderId: options.orderId ? Number(options.orderId) : null,
    });
    this._unsubscribeSession = tokenManager.onSessionChange?.(() => this.syncScope());
  },

  onShow() {
    this._hidden = false;
    this.loadConversation();
    this.startPolling();
  },

  onHide() {
    this._hidden = true;
    this._generation = (this._generation || 0) + 1;
    this._pending = {};
    this.setData({ loading: false, loadingHistory: false, sending: false });
    this.stopPolling();
  },

  onUnload() {
    this.onHide();
    this._unsubscribeSession?.();
  },

  syncScope() {
    const token = tokenManager.getToken();
    const scope = JSON.stringify([token, tokenManager.getSessionVersion?.()]);
    if (scope !== this._scope) {
      this._scope = scope;
      this._generation = (this._generation || 0) + 1;
      this._pending = {};
      this._initialized = false;
      this._beforeId = this._afterId = null;
      this.setData({ messages: [], product: {}, currentUserId: null, messageContent: '',
        counterpartyPhone: '', counterpartyPhoneMasked: '', lastMessageAnchor: '',
        loading: false, loadingHistory: false, sending: false, hasOlder: true });
    }
    return token && !this._hidden;
  },

  currentRequest(generation, scope) {
    return !this._hidden && generation === this._generation && scope === this._scope
      && scope === JSON.stringify([tokenManager.getToken(), tokenManager.getSessionVersion?.()]);
  },

  loadConversation(showLoading = true) {
    if (!this.syncScope() || !this.data.productId || !this.data.counterpartyId) return Promise.resolve(false);
    return this.loadMessages(showLoading || !this._initialized ? 'initial' : 'newer');
  },

  loadOlderMessages() {
    if (!this.syncScope() || !this._initialized || !this.data.hasOlder || !this._beforeId) return Promise.resolve(false);
    return this.loadMessages('older');
  },

  loadMessages(direction) {
    this._pending = this._pending || {};
    if (this._pending[direction]) return this._pending[direction];
    if (direction !== 'initial' && this._pending.initial) return this._pending.initial;
    if (direction === 'initial') {
      this._generation = (this._generation || 0) + 1;
      this._pending = {};
      feedback.loaded(this);
      this.setData({ loading: true, loadingHistory: false });
    } else if (direction === 'older') this.setData({ loadingHistory: true });
    const generation = this._generation;
    const scope = this._scope;
    const isCurrent = () => this.currentRequest(generation, scope);
    const query = { pageSize: 20 };
    if (direction === 'older') query.beforeId = this._beforeId;
    if (direction === 'newer' && this._afterId) query.afterId = this._afterId;
    const promise = (async () => {
      try {
        const requests = [secondHandService.listConversationMessages(this.data.productId, this.data.counterpartyId, query)];
        if (direction === 'initial') {
          requests.push(secondHandService.getProduct(this.data.productId), this.getCurrentUser(isCurrent));
          if (this.data.orderId) requests.push(secondHandService.getOrderDetail(this.data.orderId));
        }
        const [rows, product, user, order] = await Promise.all(requests);
        if (!isCurrent()) return false;
        if (direction === 'initial') {
          const phone = order?.counterpartyPhone ? String(order.counterpartyPhone) : '';
          this.setData({ product: { ...product, coverImage: this.firstImage(product.images) },
            currentUserId: user.id || null, counterpartyPhone: phone, counterpartyPhoneMasked: this.maskPhone(phone) });
          const peerMessage = rows.find(item => Number(item.senderId) === Number(this.data.counterpartyId)
            || Number(item.receiverId) === Number(this.data.counterpartyId));
          const name = peerMessage ? (Number(peerMessage.senderId) === Number(this.data.counterpartyId)
            ? peerMessage.senderName : peerMessage.receiverName) : product.sellerName;
          wx.setNavigationBarTitle({ title: name || '私信' });
          this._initialized = true;
          this._beforeId = rows[0]?.id || null;
          this._afterId = rows[rows.length - 1]?.id || null;
        } else if (direction === 'older' && rows.length) this._beforeId = rows[0].id;
        else if (direction === 'newer' && rows.length) {
          this._afterId = rows[rows.length - 1].id;
          if (!this._beforeId) this._beforeId = rows[0].id;
        }
        const oldFirst = this.data.messages[0];
        const messages = this.mergeMessages(direction === 'initial' ? [] : this.data.messages, rows);
        const update = { messages };
        if (direction !== 'newer' || !query.afterId) update.hasOlder = rows.length === 20;
        if (direction === 'initial' || (direction === 'newer' && rows.length)) {
          update.lastMessageAnchor = messages.length ? `message-${messages[messages.length - 1].id}` : '';
        } else if (direction === 'older' && oldFirst) update.lastMessageAnchor = `message-${oldFirst.id}`;
        this.setData(update);
        if (direction === 'initial' || rows.some(row => Number(row.receiverId) === Number(this.data.currentUserId))) {
          getApp().refreshMineTabRedDot({ force: true }).catch(() => {});
        }
        return true;
      } catch (error) {
        if (isCurrent() && direction !== 'newer') {
          feedback.loadError(this, '私信加载失败，请重试', () => direction === 'older'
            ? this.loadOlderMessages() : this.loadConversation(), !!this.data.product.id);
        }
        return false;
      } finally {
        if (isCurrent()) {
          delete this._pending[direction];
          if (direction === 'initial') { this.setData({ loading: false }); wx.stopPullDownRefresh(); }
          if (direction === 'older') this.setData({ loadingHistory: false });
        }
      }
    })();
    this._pending[direction] = promise;
    return promise;
  },

  mergeMessages(existing, rows) {
    const byId = new Map(existing.map(row => [String(row.id), row]));
    rows.forEach(item => byId.set(String(item.id), { ...item,
      isMine: Number(item.senderId) === Number(this.data.currentUserId), timeText: this.formatTime(item.createTime) }));
    return [...byId.values()].sort((a, b) => {
      const timeA = a.createTime ? new Date(a.createTime).getTime() : -Infinity;
      const timeB = b.createTime ? new Date(b.createTime).getTime() : -Infinity;
      return (timeA === timeB ? 0 : timeA - timeB) || Number(a.id) - Number(b.id);
    });
  },

  async getCurrentUser(isCurrent = () => true) {
    const app = getApp();
    const cached = app.globalData.userInfo || wx.getStorageSync('userInfo') || {};
    if (cached.id != null) return cached;
    const fresh = await userService.getUserInfo();
    if (!isCurrent()) return fresh;
    app.globalData.userInfo = { ...cached, ...fresh };
    wx.setStorageSync('userInfo', app.globalData.userInfo);
    return app.globalData.userInfo;
  },

  setMessage(e) {
    this.setData({ messageContent: e.detail.value });
  },

  async sendMessage() {
    if (!this.syncScope() || !this._initialized || this.data.loading) return;
    if (this.data.sending) return;
    const content = this.data.messageContent.trim();
    if (!content) {
      feedback.showToast(this, { title: '请输入消息', theme: 'warning' });
      return;
    }
    const generation = this._generation;
    const scope = this._scope;
    const isCurrent = () => this.currentRequest(generation, scope);
    try {
      this.setData({ sending: true });
      const created = await secondHandService.sendMessage({
        productId: this.data.productId,
        orderId: this.data.orderId,
        receiverId: this.data.counterpartyId,
        content,
      });
      if (!isCurrent()) return;
      this.setData({ messages: this.mergeMessages(this.data.messages, [created]),
        lastMessageAnchor: `message-${created.id}`,
        messageContent: this.data.messageContent.trim() === content ? '' : this.data.messageContent });
      // The read cursor only advances on contiguous fetched pages, never on a sent message.
      await this.loadConversation(false);
      if (isCurrent()) await subscriptions.requestSecondHandMessage();
    } catch (error) {
      if (isCurrent()) feedback.showToast(this, { title: friendlyError(error, '发送失败'), theme: 'error' });
    } finally {
      if (isCurrent()) this.setData({ sending: false });
    }
  },

  callCounterparty() {
    if (!this.data.counterpartyPhone) return;
    wx.makePhoneCall({ phoneNumber: this.data.counterpartyPhone });
  },

  gotoProduct() {
    wx.navigateTo({ url: `/pages/second-hand/detail/detail?id=${this.data.productId}` });
  },

  firstImage(images) {
    if (!images) return '';
    return String(images).split(',').filter(Boolean)[0] || '';
  },

  maskPhone(phone) {
    return /^1\d{10}$/.test(phone) ? `${phone.slice(0, 3)}****${phone.slice(7)}` : phone;
  },

  formatTime(value) {
    if (!value) return '';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return '';
    return `${date.getMonth() + 1}-${date.getDate()} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
  },

  startPolling() {
    this.stopPolling();
    this.pollTimer = setInterval(() => this.loadConversation(false), 15000);
  },

  stopPolling() {
    if (this.pollTimer) {
      clearInterval(this.pollTimer);
      this.pollTimer = null;
    }
  },

  onPullDownRefresh() {
    if (this.data.sending) { wx.stopPullDownRefresh(); return Promise.resolve(false); }
    return this.loadConversation();
  },
});
