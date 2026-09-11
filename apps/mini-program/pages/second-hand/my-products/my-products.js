const secondHandService = require('../../../services/secondHandService');
const { friendlyError } = require('../../../utils/secondHandStatus');

Page({
  data: {
    products: [],
    loading: false,
    pricePopupVisible: false,
    priceProduct: null,
    currentPriceText: '',
    priceInput: '',
    priceError: '',
    priceSubmitting: false,
    priceKeyboardHeight: 0,
  },

  onShow() {
    this.loadProducts();
  },

  async loadProducts() {
    this.setData({ loading: true });
    try {
      const products = await secondHandService.listMyProducts();
      this.setData({ products: products.map((item) => this.decorateProduct(item)) });
    } catch (error) {
      wx.showToast({ title: this.errorText(error, '加载失败'), icon: 'none' });
    } finally {
      this.setData({ loading: false });
      wx.stopPullDownRefresh();
    }
  },

  decorateProduct(product) {
    return {
      ...product,
      coverImage: this.firstImage(product.images),
      statusText: this.statusText(product.status),
      statusTheme: this.statusTheme(product.status),
      canChangePrice: this.canChangePrice(product),
    };
  },

  firstImage(images) {
    if (!images) return '';
    return String(images).split(',').filter(Boolean)[0] || '';
  },

  gotoPublish() {
    wx.navigateTo({ url: '/pages/second-hand/publish/publish' });
  },

  gotoDetail(e) {
    wx.navigateTo({ url: `/pages/second-hand/detail/detail?id=${e.currentTarget.dataset.id}` });
  },

  editProduct(e) {
    wx.navigateTo({ url: `/pages/second-hand/publish/publish?id=${e.currentTarget.dataset.id}` });
  },

  canChangePrice(product) {
    return !!product && [0, 4].includes(Number(product.status));
  },

  openPriceSheet(e) {
    if (this.data.priceSubmitting) return;
    const product = this.data.products.find((item) => Number(item.id) === Number(e.currentTarget.dataset.id));
    if (!this.canChangePrice(product)) {
      wx.showToast({ title: '当前商品暂不能改价', icon: 'none' });
      return;
    }
    if (!Number.isFinite(Number(product.price)) || Number(product.price) <= 0) {
      wx.showToast({ title: '请刷新后重试', icon: 'none' });
      return;
    }
    this.setData({
      pricePopupVisible: true,
      priceProduct: product,
      currentPriceText: Number(product.price).toFixed(2),
      priceInput: '',
      priceError: '',
      priceKeyboardHeight: 0,
    });
  },

  closePriceSheet() {
    if (this.data.priceSubmitting) return;
    this.setData({ pricePopupVisible: false, priceKeyboardHeight: 0 });
    wx.hideKeyboard();
  },

  onPricePopupChange(e) {
    if (!e.detail.visible) this.closePriceSheet();
  },

  onPriceInput(e) {
    if (this.data.priceSubmitting) return;
    this.setData({ priceInput: String(e.detail.value || ''), priceError: '' });
  },

  onPriceKeyboardHeightChange(e) {
    if (!this.data.pricePopupVisible) return;
    // 原生键盘高度单位为 px；只抬升弹层，关闭输入框的自动顶页以免重复位移。
    const height = Number(e.detail.height);
    this.setData({ priceKeyboardHeight: Number.isFinite(height) ? Math.max(0, height) : 0 });
  },

  priceChangeError(value) {
    const input = String(value).trim();
    if (!input) return '请输入新价格';
    if (!/^(?:\d+(?:\.\d{0,2})?|\.\d{1,2})$/.test(input)) {
      return '请输入有效金额，最多两位小数';
    }
    const price = Number(input);
    if (!Number.isFinite(price) || price <= 0) return '价格需大于 0';
    if (price > 99999) return '价格不能超过 99999 元';
    return '';
  },

  async submitPriceChange() {
    if (this.data.priceSubmitting || !this.data.pricePopupVisible || !this.data.priceProduct) return;
    const product = this.data.products.find((item) => Number(item.id) === Number(this.data.priceProduct.id));
    if (!this.canChangePrice(product)) {
      this.setData({ priceError: '商品状态已变化，请刷新后重试' });
      return;
    }
    const error = this.priceChangeError(this.data.priceInput);
    if (error) {
      this.setData({ priceError: error });
      return;
    }
    const price = Number(this.data.priceInput.trim());
    this.setData({ priceSubmitting: true, priceError: '' });
    wx.hideKeyboard();
    try {
      // 仅更新售价，不回传旧商品资料，避免覆盖图片、描述、自提点或上下架状态。
      await secondHandService.updateProduct(product.id, { price });
      this.setData({
        products: this.data.products.map((item) => Number(item.id) === Number(product.id)
          ? this.decorateProduct({ ...item, price }) : item),
        pricePopupVisible: false,
        priceKeyboardHeight: 0,
      });
      wx.showToast({ title: '已改价', icon: 'success' });
    } catch (error) {
      this.setData({ priceError: this.errorText(error, '改价失败，请重试') });
    } finally {
      this.setData({ priceSubmitting: false });
    }
  },

  toggleStatus(e) {
    const product = this.data.products.find((item) => Number(item.id) === Number(e.currentTarget.dataset.id));
    if (!product) return;
    const isOffShelf = Number(product.status) === 4;
    const nextStatus = isOffShelf ? 0 : 4;
    wx.showModal({
      title: isOffShelf ? '重新上架' : '下架商品',
      content: isOffShelf ? '重新上架后买家可以继续下单和议价。' : '下架后买家将无法继续下单或议价。',
      confirmText: isOffShelf ? '上架' : '下架',
      confirmColor: isOffShelf ? '#19a66a' : '#d54941',
      success: async (res) => {
        if (!res.confirm) return;
        try {
          await secondHandService.updateProductStatus(product.id, nextStatus);
          wx.showToast({ title: isOffShelf ? '已上架' : '已下架', icon: 'success' });
          this.loadProducts();
        } catch (error) {
          wx.showToast({ title: this.errorText(error, '操作失败'), icon: 'none' });
        }
      },
    });
  },

  statusText(status) {
    return {
      0: '在售',
      1: '已锁定',
      2: '交易中',
      3: '已售出',
      4: '已下架',
    }[status] || '未知';
  },

  statusTheme(status) {
    if (Number(status) === 0) return 'success';
    if (Number(status) === 4) return 'default';
    if (Number(status) === 3) return 'warning';
    return 'primary';
  },

  errorText(error, fallback) {
    return friendlyError(error, fallback);
  },

  onPullDownRefresh() {
    this.loadProducts();
  },
});
