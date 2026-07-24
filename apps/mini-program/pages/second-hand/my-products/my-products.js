const secondHandService = require('../../../services/secondHandService');

Page({
  data: {
    products: [],
    loading: false,
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

  toggleStatus(e) {
    const product = this.data.products.find((item) => Number(item.id) === Number(e.currentTarget.dataset.id));
    if (!product) return;
    const isOffShelf = Number(product.status) === 4;
    const nextStatus = isOffShelf ? 0 : 4;
    wx.showModal({
      title: isOffShelf ? '重新上架' : '下架商品',
      content: isOffShelf ? '重新上架后买家可以继续购买和议价。' : '下架后买家将无法继续购买或议价。',
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
      1: '待支付',
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
    const message = error && error.message ? error.message : fallback;
    return message.length > 18 ? message.slice(0, 18) : message;
  },

  onPullDownRefresh() {
    this.loadProducts();
  },
});
