const secondHandService = require('../../../services/secondHandService');

Page({
  data: {
    keyword: '',
    categories: [],
    products: [],
    activeCategoryId: null,
    loading: false,
    stats: {
      onSale: 0,
      negotiable: 0,
    },
  },

  onShow() {
    this.loadData();
  },

  async loadData() {
    this.setData({ loading: true });
    try {
      const [categories, products] = await Promise.all([
        secondHandService.listCategories(),
        secondHandService.listProducts({
          keyword: this.data.keyword,
          categoryId: this.data.activeCategoryId,
        }),
      ]);
      this.setData({
        categories,
        products: this.decorateProducts(products),
        stats: {
          onSale: products.length,
          negotiable: products.filter((item) => Number(item.negotiable) === 1).length,
        },
      });
    } catch (error) {
      wx.showToast({ title: this.errorText(error, '二手首页加载失败'), icon: 'none' });
    } finally {
      this.setData({ loading: false });
      wx.stopPullDownRefresh();
    }
  },

  decorateProducts(products) {
    return products.map((item) => ({
      ...item,
      coverImage: item.coverImage || this.firstImage(item.images),
      conditionText: item.conditionLevel || '成色良好',
    }));
  },

  firstImage(images) {
    if (!images) return '';
    return String(images).split(',').filter(Boolean)[0] || '';
  },

  onSearchChange(e) {
    this.setData({ keyword: e.detail.value });
  },

  onSearch() {
    this.loadData();
  },

  clearSearch() {
    this.setData({ keyword: '' });
    this.loadData();
  },

  selectCategory(e) {
    const id = e.currentTarget.dataset.id || null;
    this.setData({ activeCategoryId: id });
    this.loadData();
  },

  gotoPublish() {
    wx.navigateTo({ url: '/pages/second-hand/publish/publish' });
  },

  gotoOrders() {
    wx.navigateTo({ url: '/pages/second-hand/orders/orders' });
  },

  gotoBargains() {
    wx.navigateTo({ url: '/pages/second-hand/bargains/bargains' });
  },

  gotoMyProducts() {
    wx.navigateTo({ url: '/pages/second-hand/my-products/my-products' });
  },

  gotoDetail(e) {
    wx.navigateTo({ url: `/pages/second-hand/detail/detail?id=${e.currentTarget.dataset.id}` });
  },

  errorText(error, fallback) {
    const message = error && error.message ? error.message : fallback;
    return message.length > 18 ? message.slice(0, 18) : message;
  },

  onPullDownRefresh() {
    this.loadData();
  },
});
