const secondHandService = require('../../../services/secondHandService');
const deliveryAddressService = require('../../../services/deliveryAddressService');
const { friendlyError } = require('../../../utils/secondHandStatus');

Page({
  data: {
    id: null,
    product: {},
    images: [],
    messages: [],
    addressBook: [],
    showBargain: false,
    showMessage: false,
    showBuySheet: false,
    selectedDeliveryMode: 0,
    selectedBuyerAddressId: null,
    selectedBuyerAddressText: '',
    bargainPrice: '',
    bargainMessage: '',
    messageContent: '',
    loading: false,
    buySubmitting: false,
    bargainSubmitting: false,
    messageSubmitting: false,
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
      const product = await secondHandService.getProduct(this.data.id);
      const messages = await secondHandService.listProductMessages(this.data.id);
      this.setData({
        product: this.decorateProduct(product),
        images: this.parseImages(product.images),
        messages,
      });
    } catch (error) {
      wx.showToast({ title: this.errorText(error, '详情加载失败'), icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  decorateProduct(product) {
    const pickupAddressText = product.pickupAddressSnapshot || '';
    const pickupOnly = Number(product.pickupOnly ?? 1);
    return {
      ...product,
      pickupOnly,
      pickupAddressText,
      deliveryText: pickupOnly === 1 ? '仅支持买家自提' : '可选自提或卖家配送',
      statusText: this.statusText(product.status),
      canBuy: Number(product.status) === 0,
      canBargain: Number(product.status) === 0 && Number(product.negotiable) === 1,
    };
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
      wx.showToast({ title: '商品当前不可购买', icon: 'none' });
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
      content: `${deliveryMode === 1 ? '卖家配送到' : '买家自提于'}：${addressText}\n成交价 ¥${product.price}，支付后将为你保留商品。`,
      confirmText: '创建订单',
      success: async (res) => {
        if (!res.confirm) return;
        await this.createOrderAndPay();
      },
    });
  },

  async createOrderAndPay() {
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
      if (getApp().globalData.MOCK_PAYMENT) {
        await secondHandService.mockPaySuccess(order.id);
        wx.showToast({ title: '支付成功', icon: 'success' });
      }
      this.setData({ showBuySheet: false });
      wx.navigateTo({ url: `/pages/second-hand/order-detail/order-detail?id=${order.id}` });
    } catch (error) {
      wx.showToast({ title: this.errorText(error, '购买失败'), icon: 'none' });
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

  openMessage() {
    this.setData({ showMessage: true });
  },

  closeMessage() {
    this.setData({ showMessage: false });
  },

  noop() {},

  setMessage(e) {
    this.setData({ messageContent: e.detail.value });
  },

  async submitMessage() {
    if (this.data.messageSubmitting) return;
    const content = this.data.messageContent.trim();
    if (!content) {
      wx.showToast({ title: '请输入留言', icon: 'none' });
      return;
    }
    try {
      this.setData({ messageSubmitting: true });
      await secondHandService.sendMessage({
        productId: this.data.product.id,
        receiverId: this.data.product.sellerId,
        content,
      });
      this.setData({ showMessage: false, messageContent: '' });
      wx.showToast({ title: '已留言', icon: 'success' });
      this.loadDetail();
    } catch (error) {
      wx.showToast({ title: this.errorText(error, '留言失败'), icon: 'none' });
    } finally {
      this.setData({ messageSubmitting: false });
    }
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
      1: '待支付',
      2: '交易中',
      3: '已售出',
      4: '已下架',
    }[status] || '未知状态';
  },

  errorText(error, fallback) {
    return friendlyError(error, fallback);
  },
});
