const secondHandService = require('../../../services/secondHandService');
const { friendlyError } = require('../../../utils/secondHandStatus');

Page({
  data: {
    conversations: [],
    loading: false,
  },

  onShow() {
    this.loadConversations();
  },

  async loadConversations() {
    this.setData({ loading: true });
    try {
      const conversations = await secondHandService.listConversations();
      this.setData({ conversations: conversations.map((item) => this.decorateConversation(item)) });
    } catch (error) {
      wx.showToast({ title: friendlyError(error, '私信加载失败'), icon: 'none' });
    } finally {
      this.setData({ loading: false });
      wx.stopPullDownRefresh();
    }
  },

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
    this.loadConversations();
  },
});
