const subscriptions = require('../../../services/subscriptionService');
const secondHandService = require('../../../services/secondHandService');
const deliveryAddressService = require('../../../services/deliveryAddressService');
const userService = require('../../../services/userService');
const { friendlyError } = require('../../../utils/secondHandStatus');

Page({
  data: {
    id: null,
    product: {},
    images: [],
    currentUserId: null,
    addressBook: [],
    showBargain: false,
    showBuySheet: false,
    selectedDeliveryMode: 0,
    selectedBuyerAddressId: null,
    selectedBuyerAddressText: '',
    bargainPrice: '',
    bargainMessage: '',
    loading: false,
    favoriteSubmitting: false,
    buySubmitting: false,
    bargainSubmitting: false,
  },

  onLoad(options) {
    this.setData({ id: options.id });
    this.loadDetail();
  },

  async onShow() {
    if (this.data.showBuySheet) {
      await this.loadAddressBook(false);
    }
  },

  async loadDetail() {
    this.setData({ loading: true });
    try {
      const [product, user] = await Promise.all([
        secondHandService.getProduct(this.data.id),
        this.getCurrentUser(),
      ]);
      this.setData({
        product: this.decorateProduct(product, user),
        images: this.parseImages(product.images),
        currentUserId: user.id || null,
      });
    } catch (error) {
      wx.showToast({ title: this.errorText(error, '详情加载失败'), icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  decorateProduct(product, user = {}) {
    const pickupAddressText = product.pickupAddressSnapshot || '';
    const pickupOnly = Number(product.pickupOnly ?? 1);
    const isOwner = user.id != null && Number(user.id) === Number(product.sellerId);
    return {
      ...product,
      pickupOnly,
      pickupAddressText,
      deliveryText: pickupOnly === 1 ? '仅支持买家自提' : '可选自提或卖家配送',
      statusText: this.statusText(product.status),
      isOwner,
      isFavorited: product.favorited === true || Number(product.favorited) === 1,
      favoriteCount: Math.max(0, Number(product.favoriteCount) || 0),
      canBuy: !isOwner && Number(product.status) === 0,
      canBargain: !isOwner && Number(product.status) === 0 && Number(product.negotiable) === 1,
      canContact: !isOwner,
    };
  },

  async getCurrentUser() {
    const app = getApp();
    const cached = app.globalData.userInfo || wx.getStorageSync('userInfo') || {};
    if (cached.id != null) return cached;
    try {
      const fresh = await userService.getUserInfo();
      app.globalData.userInfo = { ...cached, ...fresh };
      wx.setStorageSync('userInfo', app.globalData.userInfo);
      return app.globalData.userInfo;
    } catch (error) {
      return cached;
    }
  },

  parseImages(images) {
    if (!images) return [];
    return String(images).split(',').filter(Boolean);
  },

  previewImage(e) {
    wx.previewImage({
      current: e.currentTarget.dataset.url,
      urls: this.data.images,
    });
  },

  async buyNow() {
    if (this.data.buySubmitting) return;
    const product = this.data.product;
    if (!product.id) return;
    if (!product.canBuy) {
      wx.showToast({ title: '商品当前不可下单', icon: 'none' });
      return;
    }
    this.setData({
      showBuySheet: true,
      selectedDeliveryMode: 0,
      selectedBuyerAddressId: null,
      selectedBuyerAddressText: '',
    });
    if (product.pickupOnly !== 1) {
      await this.loadAddressBook(false);
    }
  },

  closeBuySheet() {
    if (this.data.buySubmitting) return;
    this.setData({ showBuySheet: false });
  },

  chooseDeliveryMode(e) {
    const mode = Number(e.currentTarget.dataset.mode);
    if (mode === 1 && this.data.product.pickupOnly === 1) {
      wx.showToast({ title: '该商品仅支持自提', icon: 'none' });
      return;
    }
    this.setData({ selectedDeliveryMode: mode });
    if (mode === 1 && !this.data.addressBook.length) {
      this.loadAddressBook();
    }
  },

  async loadAddressBook(showError = true) {
    try {
      const addressBook = await deliveryAddressService.getMyAddresses();
      this.setData({
        addressBook: (addressBook || []).map((item) => ({
          ...item,
          addressText: this.formatAddress(item),
        })),
      });
    } catch (error) {
      if (showError) {
        wx.showToast({ title: this.errorText(error, '地址加载失败'), icon: 'none' });
      }
    }
  },

  selectBuyerAddress(e) {
    const address = this.data.addressBook.find((item) => Number(item.id) === Number(e.currentTarget.dataset.id));
    if (!address) return;
    this.setData({
      selectedBuyerAddressId: address.id,
      selectedBuyerAddressText: address.addressText,
    });
  },

  gotoAddDeliveryAddress() {
    wx.navigateTo({ url: '/pages/address/addressAdd/add' });
  },

  confirmBuy() {
    const product = this.data.product;
    const deliveryMode = Number(this.data.selectedDeliveryMode);
    if (deliveryMode === 1 && !this.data.selectedBuyerAddressText) {
      wx.showToast({ title: '请选择配送地址', icon: 'none' });
      return;
    }
    const addressText = deliveryMode === 1 ? this.data.selectedBuyerAddressText : product.pickupAddressText;
    wx.showModal({
      title: '确认下单',
      content: `${deliveryMode === 1 ? '卖家配送到' : '买家自提于'}：${addressText}\n约定价格 ¥${product.price}。下单后商品将进入交易中，请与卖家自行协商付款和交付。`,
      confirmText: '创建订单',
      success: async (res) => {
        if (!res.confirm) return;
        await this.createOrder();
      },
    });
  },

  async createOrder() {
    const product = this.data.product;
    const deliveryMode = Number(this.data.selectedDeliveryMode);
    const payload = {
      productId: product.id,
      deliveryMode,
      deliveryRemark: deliveryMode === 1 ? this.data.selectedBuyerAddressText : product.pickupAddressText,
      buyerDeliveryAddressId: deliveryMode === 1 ? this.data.selectedBuyerAddressId : null,
      buyerDeliveryAddressSnapshot: deliveryMode === 1 ? this.data.selectedBuyerAddressText : null,
    };
    this.setData({ buySubmitting: true });
    try {
      const order = await secondHandService.createOrder(payload);
      await subscriptions.requestSecondHandOrder();
      this.setData({ showBuySheet: false });
      wx.showToast({ title: '下单成功', icon: 'success' });
      wx.navigateTo({ url: `/pages/second-hand/order-detail/order-detail?id=${order.id}` });
    } catch (error) {
      wx.showToast({ title: this.errorText(error, '下单失败'), icon: 'none' });
    } finally {
      this.setData({ buySubmitting: false });
    }
  },

  openBargain() {
    if (!this.data.product.canBargain) {
      wx.showToast({ title: '当前不可议价', icon: 'none' });
      return;
    }
    this.setData({ showBargain: true });
  },

  closeBargain() {
    this.setData({ showBargain: false });
  },

  setBargainPrice(e) {
    this.setData({ bargainPrice: e.detail.value });
  },

  setBargainMessage(e) {
    this.setData({ bargainMessage: e.detail.value });
  },

  submitBargain() {
    if (this.data.bargainSubmitting) return;
    const price = Number(this.data.bargainPrice);
    if (!price || price <= 0) {
      wx.showToast({ title: '请输入有效报价', icon: 'none' });
      return;
    }
    if (price >= Number(this.data.product.price)) {
      wx.showToast({ title: '报价需低于售价', icon: 'none' });
      return;
    }
    wx.showModal({
      title: '发送议价',
      content: `向卖家报价 ¥${price}，请确认后发送。`,
      confirmText: '发送',
      success: async (res) => {
        if (!res.confirm) return;
        try {
          this.setData({ bargainSubmitting: true });
          await secondHandService.createBargain({
            productId: this.data.product.id,
            offerPrice: price,
            message: this.data.bargainMessage,
          });
          await subscriptions.requestSecondHandOrder();
          this.setData({ showBargain: false, bargainPrice: '', bargainMessage: '' });
          wx.showToast({ title: '已发送议价', icon: 'success' });
        } catch (error) {
          wx.showToast({ title: this.errorText(error, '议价失败'), icon: 'none' });
        } finally {
          this.setData({ bargainSubmitting: false });
        }
      },
    });
  },

  noop() {},

  async toggleFavorite() {
    const product = this.data.product;
    if (!product.id || product.isOwner || this.data.favoriteSubmitting) return;
    const nextFavorited = !product.isFavorited;
    this.setData({ favoriteSubmitting: true });
    try {
      if (nextFavorited) {
        await secondHandService.favoriteProduct(product.id);
      } else {
        await secondHandService.unfavoriteProduct(product.id);
      }
      this.setData({
        product: {
          ...product,
          isFavorited: nextFavorited,
          favoriteCount: Math.max(0, product.favoriteCount + (nextFavorited ? 1 : -1)),
        },
      });
      wx.showToast({ title: nextFavorited ? '已收藏' : '已取消收藏', icon: 'success' });
    } catch (error) {
      wx.showToast({ title: this.errorText(error, '操作失败'), icon: 'none' });
    } finally {
      this.setData({ favoriteSubmitting: false });
    }
  },

  gotoConversation() {
    const product = this.data.product;
    if (!product.id || !product.sellerId || product.isOwner) return;
    wx.navigateTo({
      url: `/pages/second-hand/conversation/conversation?productId=${product.id}&counterpartyId=${product.sellerId}`,
    });
  },

  gotoConversations() {
    wx.navigateTo({ url: '/pages/second-hand/conversations/conversations' });
  },

  editProduct() {
    wx.navigateTo({ url: `/pages/second-hand/publish/publish?id=${this.data.product.id}` });
  },

  toggleProductStatus() {
    const product = this.data.product;
    if (!product.isOwner || ![0, 4].includes(Number(product.status))) return;
    const nextStatus = Number(product.status) === 4 ? 0 : 4;
    const actionText = nextStatus === 0 ? '上架' : '下架';
    wx.showModal({
      title: `${actionText}商品`,
      content: `${actionText}后将${nextStatus === 0 ? '重新对同校买家展示' : '暂停买家下单'}，确认继续？`,
      confirmText: actionText,
      success: async (res) => {
        if (!res.confirm) return;
        try {
          await secondHandService.updateProductStatus(product.id, nextStatus);
          wx.showToast({ title: `已${actionText}`, icon: 'success' });
          this.loadDetail();
        } catch (error) {
          wx.showToast({ title: this.errorText(error, '操作失败'), icon: 'none' });
        }
      },
    });
  },

  formatAddress(address) {
    return [
      address.compusName,
      address.buildCategoryName,
      address.buildingName,
      address.details,
    ].filter(Boolean).join(' ');
  },

  statusText(status) {
    return {
      0: '在售',
      1: '已锁定',
      2: '交易中',
      3: '已售出',
      4: '已下架',
    }[status] || '未知状态';
  },

  errorText(error, fallback) {
    return friendlyError(error, fallback);
  },

  // 右上角分享--好友、朋友圈
  onShareAppMessage() {
    const product = this.data.product || {};
    const id = this.data.id || product.id;
    const title = product.title
      ? `${product.title} · ¥${product.price}`
      : '帮帮校园送 · 校园二手好物';
    const cover = this.data.images && this.data.images[0];
    if (!id) {
      return { title, path: '/pages/second-hand/index/index' };
    }
    return {
      title,
      path: `/pages/second-hand/detail/detail?id=${id}`,
      ...(cover ? { imageUrl: cover } : {}),
    };
  },
  onShareTimeline() {
    const product = this.data.product || {};
    const id = this.data.id || product.id;
    const title = product.title
      ? `${product.title} · ¥${product.price}`
      : '帮帮校园送 · 校园二手好物';
    const cover = this.data.images && this.data.images[0];
    return {
      title,
      query: id ? `id=${id}` : '',
      ...(cover ? { imageUrl: cover } : {}),
    };
  },
});
