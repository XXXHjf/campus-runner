const feedback = require('../../../utils/feedback');
const secondHandService = require('../../../services/secondHandService');
const { friendlyError } = require('../../../utils/secondHandStatus');

Page({
  retryFeedbackLoad() { return this._feedbackRetry?.(); },
  data: {
    statusBarHeight: 0,
    navigationBarHeight: 44,
    navigationRightPadding: 96,
    controlsHeight: 44,
    keyword: '',
    products: [],
    filteredProducts: [],
    loading: false,
  },

  onLoad() {
    this.updateNavigationMetrics();
    this.pendingRemovalIds = new Set();
    this.loadVersion = 0;
    this.disposed = false;
  },

  onShow() {
    return this.loadFavorites();
  },

  updateNavigationMetrics() {
    const windowInfo = typeof wx.getWindowInfo === 'function'
      ? wx.getWindowInfo() : wx.getSystemInfoSync();
    const menuButton = wx.getMenuButtonBoundingClientRect();
    const statusBarHeight = windowInfo.statusBarHeight || 0;
    const hasMenuButton = menuButton && menuButton.width > 0 && menuButton.left > 0;
    const menuGap = hasMenuButton ? Math.max(0, menuButton.top - statusBarHeight) : 0;
    const navigationBarHeight = hasMenuButton ? Math.max(44, menuButton.height + menuGap * 2) : 44;
    this.setData({
      statusBarHeight,
      navigationBarHeight,
      navigationRightPadding: hasMenuButton
        ? Math.max(96, windowInfo.windowWidth - menuButton.left + 4) : 96,
      controlsHeight: statusBarHeight + navigationBarHeight,
    });
  },

  goBack() {
    if (getCurrentPages().length > 1) wx.navigateBack({ delta: 1 });
    else wx.switchTab({ url: '/pages/mine/mine/mine' });
  },

  onSearchChange(e) {
    const keyword = String(e.detail.value || '');
    this.setData({ keyword, filteredProducts: this.filterProducts(this.data.products, keyword) });
  },

  clearSearch() {
    this.setData({ keyword: '', filteredProducts: this.data.products });
  },

  filterProducts(products, keyword) {
    const normalized = String(keyword || '').trim().toLowerCase();
    if (!normalized) return products;
    return products.filter((item) => [item.title, item.categoryName, item.conditionLevel]
      .some((value) => String(value || '').toLowerCase().includes(normalized)));
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
      // The save happens on leaving. Report its failure on the active page.
      feedback.showMessage(feedback.currentPage(), failedCount === ids.length
        ? '取消收藏失败，请返回重试' : '部分收藏取消失败，请返回重试', {
        theme: 'error', action: '查看收藏',
        onAction: () => wx.navigateTo({ url: '/pages/second-hand/favorites/favorites' }),
      });
    });
    return this.commitPromise;
  },

  async loadFavorites() {
    feedback.loaded(this);
    if (this.disposed) return;
    const version = ++this.loadVersion;
    this.setData({ loading: true });
    try {
      const products = await secondHandService.listFavoriteProducts();
      if (this.disposed || version !== this.loadVersion) return;
      this.updateProducts(products.map((item) => this.decorateProduct(item)));
    } catch (error) {
      if (this.disposed || version !== this.loadVersion) return;
      feedback.loadError(this, '收藏加载失败，请重试', () => this.loadFavorites(), !!this.data.products.length);
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
      filteredProducts: this.filterProducts(products, this.data.keyword),
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
