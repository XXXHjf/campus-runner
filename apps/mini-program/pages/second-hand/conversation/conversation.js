const subscriptions = require('../../../services/subscriptionService');
const secondHandService = require('../../../services/secondHandService');
const userService = require('../../../services/userService');
const { friendlyError } = require('../../../utils/secondHandStatus');

Page({
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
    lastMessageAnchor: '',
  },

  onLoad(options) {
    this.setData({
      productId: Number(options.productId),
      counterpartyId: Number(options.counterpartyId),
      orderId: options.orderId ? Number(options.orderId) : null,
    });
    this.loadConversation();
  },

  onShow() {
    this.startPolling();
  },

  onHide() {
    this.stopPolling();
  },

  onUnload() {
    this.stopPolling();
  },

  async loadConversation(showLoading = true) {
    if (!this.data.productId || !this.data.counterpartyId) return;
    if (showLoading) this.setData({ loading: true });
    try {
      const requests = [
        secondHandService.getProduct(this.data.productId),
        secondHandService.listConversationMessages(this.data.productId, this.data.counterpartyId),
        this.getCurrentUser(),
      ];
      if (this.data.orderId) requests.push(secondHandService.getOrderDetail(this.data.orderId));
      const [product, messages, user, order] = await Promise.all(requests);
      const lastMessage = messages[messages.length - 1];
      const phone = order && order.counterpartyPhone ? String(order.counterpartyPhone) : '';
      const counterpartyMessage = messages.find((item) => (
        Number(item.senderId) === Number(this.data.counterpartyId) ||
        Number(item.receiverId) === Number(this.data.counterpartyId)
      ));
      const counterpartyName = counterpartyMessage
        ? (Number(counterpartyMessage.senderId) === Number(this.data.counterpartyId)
          ? counterpartyMessage.senderName
          : counterpartyMessage.receiverName)
        : (Number(product.sellerId) === Number(this.data.counterpartyId) ? product.sellerName : '同学');
      this.setData({
        product: { ...product, coverImage: this.firstImage(product.images) },
        currentUserId: user.id || null,
        messages: messages.map((item) => ({
          ...item,
          isMine: Number(item.senderId) === Number(user.id),
          timeText: this.formatTime(item.createTime),
        })),
        counterpartyPhone: phone,
        counterpartyPhoneMasked: this.maskPhone(phone),
        lastMessageAnchor: lastMessage ? `message-${lastMessage.id}` : '',
      });
      wx.setNavigationBarTitle({ title: counterpartyName || '私信' });
      if (showLoading) {
        getApp().refreshMineTabRedDot({ force: true, userInfo: user }).catch(() => {});
      }
    } catch (error) {
      if (showLoading) {
        wx.showToast({ title: friendlyError(error, '私信加载失败'), icon: 'none' });
      }
    } finally {
      this.setData({ loading: false });
      wx.stopPullDownRefresh();
    }
  },

  async getCurrentUser() {
    const app = getApp();
    const cached = app.globalData.userInfo || wx.getStorageSync('userInfo') || {};
    if (cached.id != null) return cached;
    const fresh = await userService.getUserInfo();
    app.globalData.userInfo = { ...cached, ...fresh };
    wx.setStorageSync('userInfo', app.globalData.userInfo);
    return app.globalData.userInfo;
  },

  setMessage(e) {
    this.setData({ messageContent: e.detail.value });
  },

  async sendMessage() {
    if (this.data.sending) return;
    const content = this.data.messageContent.trim();
    if (!content) {
      wx.showToast({ title: '请输入消息', icon: 'none' });
      return;
    }
    try {
      this.setData({ sending: true });
      await secondHandService.sendMessage({
        productId: this.data.productId,
        orderId: this.data.orderId,
        receiverId: this.data.counterpartyId,
        content,
      });
      this.setData({ messageContent: '' });
      await subscriptions.requestSecondHandMessage();
      await this.loadConversation(false);
    } catch (error) {
      wx.showToast({ title: friendlyError(error, '发送失败'), icon: 'none' });
    } finally {
      this.setData({ sending: false });
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
    this.loadConversation();
  },
});
