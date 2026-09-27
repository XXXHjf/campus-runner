const feedback = require('../../../utils/feedback');
const subscriptions = require('../../../services/subscriptionService');
const secondHandService = require('../../../services/secondHandService');
const deliveryAddressService = require('../../../services/deliveryAddressService');
const userService = require('../../../services/userService');
const tokenManager = require('../../../utils/tokenManager');
const { ensureAuthenticated } = require('../../../utils/accessGuard');
const { friendlyError } = require('../../../utils/secondHandStatus');

Page({
  retryFeedbackLoad() { return this._feedbackRetry?.(); },
  data: {
    id: null,
    product: {},
    images: [],
    galleryIndex: 0,
    currentUserId: null,
    addressBook: [],
    showBargain: false,
    showBuySheet: false,
    acceptedBargain: null,
    orderPrice: '',
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
    wx.showShareMenu({ menus: ['shareAppMessage', 'shareTimeline'] });
    this.setData({ id: options.id });
    this._requestedBargainId = options.bargainId || null;
    this._openBargainCheckout = !!options.bargainId;
  },

  async onShow() {
    this._detailHidden = false;
    if (this._detailToken !== undefined && this._detailToken !== tokenManager.getToken()) {
      this.invalidateDetail();
      this.setData({ product: {}, images: [], showBuySheet: false, showBargain: false, addressBook: [], currentUserId: null, acceptedBargain: null, orderPrice: '' });
      this._openBargainCheckout = false;
    }
    await this.loadDetail();
    if (this._openBargainCheckout && !this._detailHidden && this.data.product.id) {
      this._openBargainCheckout = false;
      await this.buyNow();
    }
    if (!this._detailHidden && this.data.showBuySheet) {
      await this.loadAddressBook(false);
    }
  },

  onHide() {
    this._detailHidden = true;
    this.invalidateDetail();
  },

  onUnload() {
    this._detailDisposed = true;
    this.invalidateDetail();
  },

  invalidateDetail() {
    this._detailVersion = (this._detailVersion || 0) + 1;
    this._detailRequest = null;
  },

  loadDetail() {
    if (this._detailDisposed || this._detailHidden) return Promise.resolve();
    const token = tokenManager.getToken();
    if (this._detailRequest && this._detailToken === token) return this._detailRequest;
    this._detailToken = token;
    const version = this._detailVersion = (this._detailVersion || 0) + 1;
    const isCurrent = () => !this._detailDisposed && !this._detailHidden
      && version === this._detailVersion && token === tokenManager.getToken();
    feedback.loaded(this);
    this.setData({ loading: true });
    this._detailRequest = (async () => {
      try {
        const [product, user] = await Promise.all([
          secondHandService.getProduct(this.data.id),
          this.getCurrentUser(),
        ]);
        if (!isCurrent()) return;
        const decorated = this.decorateProduct(product, user);
        this.setData({
          product: decorated,
          images: this.parseImages(product.images),
          galleryIndex: 0,
          currentUserId: user.id || null,
          showBuySheet: this.data.showBuySheet && decorated.canBuy,
          showBargain: this.data.showBargain && decorated.canBargain,
        });
      } catch (error) {
        if (!isCurrent()) return;
        feedback.loadError(this, '详情加载失败，请重试', () => this.loadDetail(), !!this.data.product.id);
      } finally {
        if (isCurrent()) {
          this._detailRequest = null;
          this.setData({ loading: false });
        }
      }
    })();
    return this._detailRequest;
  },

  decorateProduct(product, user = {}) {
    const pickupAddressText = product.pickupAddressSnapshot || '';
    const pickupOnly = Number(product.pickupOnly ?? 1);
    const isOwner = user.id != null && Number(user.id) === Number(product.sellerId);
    return {
      ...product,
      pickupOnly,
      pickupAddressText,
      deliveryText: pickupOnly === 1 ? '仅自提' : '支持配送',
      statusText: this.statusText(product.status),
      isOwner,
      isFavorited: product.favorited === true || Number(product.favorited) === 1,
      favoriteCount: Math.max(0, Number(product.favoriteCount) || 0),
      canBuy: !isOwner && Number(product.status) === 0,
      canBargain: !isOwner && Number(product.status) === 0 && Number(product.negotiable) === 1,
    };
  },

  async getCurrentUser() {
    if (!tokenManager.hasToken()) return {};
    const token = tokenManager.getToken();
    const app = getApp();
    const cached = app.globalData.userInfo || wx.getStorageSync('userInfo') || {};
    if (cached.id != null) return cached;
    try {
      const fresh = await userService.getUserInfo();
      if (token !== tokenManager.getToken()) return {};
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

  onGalleryChange(e) {
    this.setData({ galleryIndex: e.detail.current });
  },

  previewImage(e) {
    wx.previewImage({
      current: e.currentTarget.dataset.url,
      urls: this.data.images,
    });
  },

  async buyNow() {
    if (this.data.buySubmitting) return;
    if (!await ensureAuthenticated()) return;
    const product = this.data.product;
    if (!product.id) return;
    if (!product.canBuy) {
      feedback.showToast(this, { title: '商品当前不可下单', theme: 'warning' });
      return;
    }
    const token = tokenManager.getToken();
    let acceptedBargain;
    try {
      const bargains = await secondHandService.listProductBargains(product.id);
      if (this._detailDisposed || this._detailHidden || token !== tokenManager.getToken()) return;
      const user = await this.getCurrentUser();
      if (this._detailDisposed || this._detailHidden || token !== tokenManager.getToken()) return;
      const eligible = bargains.filter((item) => Number(item.status) === 1 && !item.orderId
        && Number(item.buyerId) === Number(user.id));
      acceptedBargain = this._requestedBargainId
        ? eligible.find((item) => Number(item.id) === Number(this._requestedBargainId))
        : eligible[0];
      if (this._requestedBargainId && !acceptedBargain) {
        feedback.showToast(this, { title: '该报价当前不可下单，请查看议价记录', theme: 'warning' });
        return;
      }
    } catch (error) {
      feedback.showToast(this, { title: this.errorText(error, '报价加载失败，请重试'), theme: 'error' });
      return;
    }
    if (!this.data.product.canBuy) return;
    this.setData({
      showBuySheet: true,
      acceptedBargain: acceptedBargain || null,
      orderPrice: acceptedBargain ? acceptedBargain.offerPrice : this.data.product.price,
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
      feedback.showToast(this, { title: '该商品仅支持自提', theme: 'warning' });
      return;
    }
    this.setData({ selectedDeliveryMode: mode });
    if (mode === 1 && !this.data.addressBook.length) {
      this.loadAddressBook();
    }
  },

  async loadAddressBook(showError = true) {
    const token = tokenManager.getToken();
    try {
      const addressBook = await deliveryAddressService.getMyAddresses();
      if (this._detailDisposed || this._detailHidden || token !== tokenManager.getToken()) return;
      const addresses = (addressBook || []).map((item) => ({ ...item, addressText: this.formatAddress(item) }));
      const selected = addresses.find((item) => Number(item.id) === Number(this.data.selectedBuyerAddressId));
      this.setData({
        addressBook: addresses,
        selectedBuyerAddressId: selected ? selected.id : null,
        selectedBuyerAddressText: selected ? selected.addressText : '',
      });
    } catch (error) {
      if (this._detailDisposed || this._detailHidden || token !== tokenManager.getToken()) return;
      if (showError) {
        feedback.showMessage(this, '地址列表加载失败，请重试', { theme: 'error', action: '重试', onAction: () => this.loadAddressBook() });
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
      feedback.showToast(this, { title: '请选择配送地址', theme: 'warning' });
      return;
    }
    const addressText = deliveryMode === 1 ? this.data.selectedBuyerAddressText : product.pickupAddressText;
    feedback.showModal(this, {
      title: '确认下单',
      content: `${deliveryMode === 1 ? '卖家配送到' : '买家自提于'}：${addressText}\n约定价格 ¥${this.data.orderPrice || product.price}。下单后商品将进入交易中，请与卖家自行协商付款和交付。`,
      confirmText: '创建订单',
      cancelText: '再看看',
      success: async (res) => {
        if (!res.confirm) return;
        await this.createOrder();
      },
    });
  },

  async createOrder() {
    if (this.data.buySubmitting || !this.data.product.canBuy) return;
    const product = this.data.product;
    const deliveryMode = Number(this.data.selectedDeliveryMode);
    const payload = {
      productId: product.id,
      bargainId: this.data.acceptedBargain ? this.data.acceptedBargain.id : null,
      deliveryMode,
      deliveryRemark: deliveryMode === 1 ? this.data.selectedBuyerAddressText : product.pickupAddressText,
      buyerDeliveryAddressId: deliveryMode === 1 ? this.data.selectedBuyerAddressId : null,
      buyerDeliveryAddressSnapshot: deliveryMode === 1 ? this.data.selectedBuyerAddressText : null,
    };
    this.setData({ buySubmitting: true });
    try {
      const order = await secondHandService.createOrder(payload);
      this.invalidateDetail();
      this.setData({ product: { ...this.data.product, canBuy: false, canBargain: false }, showBuySheet: false });
      await this.loadDetail();
      await subscriptions.requestSecondHandOrder();
      this.setData({ showBuySheet: false });
      feedback.navigate(this, 'navigateTo', { url: `/pages/second-hand/order-detail/order-detail?id=${order.id}` }, '下单成功');
    } catch (error) {
      feedback.showToast(this, { title: this.errorText(error, '下单失败'), theme: 'error' });
      this.invalidateDetail();
      await this.loadDetail();
    } finally {
      this.setData({ buySubmitting: false });
    }
  },

  async openBargain() {
    if (!await ensureAuthenticated()) return;
    if (!this.data.product.canBargain) {
      feedback.showToast(this, { title: '当前不可议价', theme: 'warning' });
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
      feedback.showToast(this, { title: '请输入有效报价', theme: 'warning' });
      return;
    }
    if (price >= Number(this.data.product.price)) {
      feedback.showToast(this, { title: '报价需低于售价', theme: 'warning' });
      return;
    }
    feedback.showModal(this, {
      title: '发送议价',
      content: `向卖家报价 ¥${price}，请确认后发送。`,
      confirmText: '发送',
      cancelText: '暂不发送',
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
          feedback.showToast(this, { title: '已发送议价', icon: 'success' });
        } catch (error) {
          feedback.showToast(this, { title: this.errorText(error, '议价失败'), theme: 'error' });
        } finally {
          this.setData({ bargainSubmitting: false });
        }
      },
    });
  },

  noop() {},

  async toggleFavorite() {
    if (!await ensureAuthenticated()) return;
    const product = this.data.product;
    if (!product.id || product.isOwner || this.data.favoriteSubmitting) return;
    const nextFavorited = !product.isFavorited;
    const token = tokenManager.getToken();
    this.invalidateDetail();
    this.setData({ favoriteSubmitting: true });
    try {
      if (nextFavorited) {
        await secondHandService.favoriteProduct(product.id);
      } else {
        await secondHandService.unfavoriteProduct(product.id);
      }
      if (this._detailDisposed || token !== tokenManager.getToken()) return;
      this.invalidateDetail();
    } catch (error) {
      feedback.showToast(this, { title: this.errorText(error, '操作失败'), theme: 'error' });
    } finally {
      if (!this._detailDisposed) {
        this.setData({ favoriteSubmitting: false });
        await this.loadDetail();
      }
    }
  },

  editProduct() {
    wx.navigateTo({ url: `/pages/second-hand/publish/publish?id=${this.data.product.id}` });
  },

  toggleProductStatus() {
    const product = this.data.product;
    if (!product.isOwner || ![0, 4].includes(Number(product.status))) return;
    const nextStatus = Number(product.status) === 4 ? 0 : 4;
    const actionText = nextStatus === 0 ? '上架' : '下架';
    feedback.showModal(this, {
      title: `${actionText}商品`,
      content: `${actionText}后将${nextStatus === 0 ? '重新对同校买家展示' : '暂停买家下单'}，确认继续？`,
      confirmText: actionText,
      cancelText: '保持不变',
      success: async (res) => {
        if (!res.confirm) return;
        try {
          await secondHandService.updateProductStatus(product.id, nextStatus);
          feedback.showToast(this, { title: `已${actionText}`, icon: 'success' });
          this.invalidateDetail();
          this.loadDetail();
        } catch (error) {
          feedback.showToast(this, { title: this.errorText(error, '操作失败'), theme: 'error' });
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
