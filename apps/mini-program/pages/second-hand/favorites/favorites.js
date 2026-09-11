const secondHandService = require('../../../services/secondHandService');
const { friendlyError } = require('../../../utils/secondHandStatus');

Page({
  data: {
    products: [],
    loading: false,
    favoriteCount: 0,
  },

  onLoad() {
    this.pendingRemovalIds = new Set();
    this.loadVersion = 0;
    this.disposed = false;
  },

  onShow() {
    return this.loadFavorites();
  },

  onUnload() {
    if (this.disposed) return this.commitPromise;
    this.disposed = true;
    this.loadVersion += 1;
    const ids = Array.from(this.pendingRemovalIds);
    this.pendingRemovalIds.clear();
    // Page lifecycle callbacks are not awaited. The service retains each write
    // so a new favorites page cannot read before these requests have settled.
    this.commitPromise = Promise.all(ids.map((id) => (
      secondHandService.unfavoriteProduct(id).then(() => false, () => true)
    ))).then((failures) => {
      const failedCount = failures.filter(Boolean).length;
      if (!failedCount) return;
      wx.showToast({
        title: failedCount === ids.length
          ? '取消收藏失败，请返回重试'
          : '部分收藏取消失败，请返回重试',
        icon: 'none',
      });
    });
    return this.commitPromise;
  },

  async loadFavorites() {
    if (this.disposed) return;
    const version = ++this.loadVersion;
    this.setData({ loading: true });
    try {
      const products = await secondHandService.listFavoriteProducts();
      if (this.disposed || version !== this.loadVersion) return;
      this.updateProducts(products.map((item) => this.decorateProduct(item)));
    } catch (error) {
      if (this.disposed || version !== this.loadVersion) return;
      wx.showToast({ title: friendlyError(error, '收藏加载失败'), icon: 'none' });
    } finally {
      if (!this.disposed && version === this.loadVersion) {
        this.setData({ loading: false });
        wx.stopPullDownRefresh();
      }
    }
  },

  updateProducts(products) {
    this.setData({
      products,
      favoriteCount: products.filter((item) => item.isFavorited).length,
    });
  },

  decorateProduct(product) {
    return {
      ...product,
      isFavorited: !this.pendingRemovalIds.has(String(product.id)),
      coverImage: this.firstImage(product.images),
      statusText: this.statusText(product.status),
      statusTheme: this.statusTheme(product.status),
    };
  },

  firstImage(images) {
    if (!images) return '';
    return String(images).split(',').filter(Boolean)[0] || '';
  },

  gotoDetail(e) {
    wx.navigateTo({ url: `/pages/second-hand/detail/detail?id=${e.currentTarget.dataset.id}` });
  },

  toggleFavorite(e) {
    const id = String(e.currentTarget.dataset.id || '');
    if (!id || this.disposed || this.data.loading) return;
    const product = this.data.products.find((item) => String(item.id) === id);
    if (!product) return;
    const isFavorited = !product.isFavorited;
    if (isFavorited) {
      this.pendingRemovalIds.delete(id);
    } else {
      this.pendingRemovalIds.add(id);
    }
    this.updateProducts(this.data.products.map((item) => (
      String(item.id) === id ? { ...item, isFavorited } : item
    )));
  },

  gotoMarket() {
    wx.switchTab({ url: '/pages/second-hand/index/index' });
  },

  statusText(status) {
    return {
      0: '在售',
      1: '已锁定',
      2: '交易中',
      3: '已售出',
      4: '已下架',
    }[status] || '状态未知';
  },

  statusTheme(status) {
    if (Number(status) === 0) return 'success';
    if (Number(status) === 3) return 'warning';
    if (Number(status) === 4) return 'default';
    return 'primary';
  },

  onPullDownRefresh() {
    return this.loadFavorites();
  },
});
