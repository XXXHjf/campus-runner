const secondHandService = require('../../../services/secondHandService');
const userService = require('../../../services/userService');
const tokenManager = require('../../../utils/tokenManager');

Page({
  data: {
    schoolName: '',
    keyword: '',
    categories: [],
    products: [],
    activeCategoryId: null,
    pickupAddressFilter: null,
    filterVisible: false,
    filterCount: 0,
    filterSummary: '',
    draftCategoryId: null,
    draftPickupAddress: null,
    loading: false,
    refreshing: false,
    statusBarHeight: 0,
    navigationBarHeight: 44,
    navigationRightPadding: 96,
    stats: {
      onSale: 0,
      negotiable: 0,
    },
  },

  onLoad() {
    this.schoolLoadVersion = 0;
    this.schoolToken = null;
    this.updateNavigationMetrics();
  },

  onShow() {
    getApp().refreshMineTabRedDot().catch(() => {});
    return Promise.all([this.loadSchoolName(), this.loadData()]);
  },

  onHide() {
    this.schoolLoadVersion += 1;
  },

  onUnload() {
    this.onHide();
  },

  async loadSchoolName() {
    const version = ++this.schoolLoadVersion;
    const app = getApp();
    let token = tokenManager.getToken();
    if (!token || token !== this.schoolToken) {
      this.setData({ schoolName: '' });
    }

    try {
      // 首次启动的静默登录可能晚于页面 onShow，完成后再读取学校。
      if (!token && app.globalData.silentLoginPromise) {
        await app.globalData.silentLoginPromise;
      }
      if (version !== this.schoolLoadVersion) return;
      token = tokenManager.getToken();
      if (!token) return;

      const userInfo = await userService.getUserInfo();
      if (version !== this.schoolLoadVersion) return;
      // 当前用户查询只返回 schoolId，沿用校园认证页的学校列表解析名称。
      const schools = userInfo && userInfo.schoolId ? await userService.getSchools() : [];
      if (version !== this.schoolLoadVersion) return;
      const currentToken = tokenManager.getToken();
      // 请求层可能续签同一账号的 token，但不能展示已退出或其他账号的校名。
      const sameUser = userInfo && userInfo.id != null
        && String(userInfo.id) === String(app.globalData.userInfo?.id);
      if (!currentToken || (currentToken !== token && !sameUser)) {
        this.schoolToken = null;
        this.setData({ schoolName: '' });
        return;
      }

      this.schoolToken = currentToken;
      const school = schools.find((item) => String(item.id) === String(userInfo.schoolId));
      this.setData({
        schoolName: school && typeof school.schoolName === 'string'
          ? school.schoolName.trim()
          : '',
      });
    } catch (error) {
      if (version !== this.schoolLoadVersion) return;
      this.schoolToken = null;
      this.setData({ schoolName: '' });
      console.warn('二手首页学校信息加载失败', error);
    }
  },

  updateNavigationMetrics() {
    const windowInfo = typeof wx.getWindowInfo === 'function'
      ? wx.getWindowInfo()
      : wx.getSystemInfoSync();
    const menuButton = wx.getMenuButtonBoundingClientRect();
    const statusBarHeight = windowInfo.statusBarHeight || 0;
    const hasMenuButton = menuButton && menuButton.width > 0 && menuButton.left > 0;
    const menuGap = hasMenuButton ? Math.max(0, menuButton.top - statusBarHeight) : 0;

    this.setData({
      statusBarHeight,
      navigationBarHeight: hasMenuButton
        ? Math.max(44, menuButton.height + menuGap * 2)
        : 44,
      navigationRightPadding: hasMenuButton
        ? Math.max(96, windowInfo.windowWidth - menuButton.left + 4)
        : 96,
    });
  },

  async loadData() {
    this.setData({ loading: true });
    try {
      const [categories, products] = await Promise.all([
        secondHandService.listCategories(),
        secondHandService.listProducts({
          keyword: this.data.keyword,
          categoryId: this.data.activeCategoryId,
          pickupAddressPrefix: this.data.pickupAddressFilter
            ? this.data.pickupAddressFilter.prefix
            : null,
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
      console.error('二手首页商品加载失败', error);
      wx.showToast({ title: '商品加载失败，请稍后重试', icon: 'none' });
    } finally {
      this.setData({
        loading: false,
        refreshing: false,
      });
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
    this.setData({
      activeCategoryId: id,
      ...this.buildFilterMeta(id, this.data.pickupAddressFilter),
    }, () => this.loadData());
  },

  openFilter() {
    this.setData({
      draftCategoryId: this.data.activeCategoryId,
      draftPickupAddress: this.data.pickupAddressFilter,
      filterVisible: true,
    });
  },

  onFilterVisibleChange(e) {
    this.setData({ filterVisible: !!e.detail.visible });
  },

  selectFilterCategory(e) {
    this.setData({ draftCategoryId: e.currentTarget.dataset.id || null });
  },

  chooseFilterAddress() {
    const picker = this.selectComponent('#secondHandAddressPicker');
    if (picker) picker.showPicker();
  },

  handleFilterAddressConfirm(e) {
    const detail = e.detail || {};
    const prefix = [detail.upCompus, detail.upCategory, detail.upBuilding]
      .filter(Boolean)
      .join(' ');
    if (!prefix) return;
    this.setData({
      draftPickupAddress: {
        ...detail,
        prefix,
        displayText: prefix,
      },
    });
  },

  clearDraftPickupAddress() {
    this.setData({ draftPickupAddress: null });
  },

  resetFilterDraft() {
    this.setData({
      draftCategoryId: null,
      draftPickupAddress: null,
    });
  },

  applyFilterSelection() {
    const activeCategoryId = this.data.draftCategoryId;
    const pickupAddressFilter = this.data.draftPickupAddress;
    this.setData({
      activeCategoryId,
      pickupAddressFilter,
      filterVisible: false,
      ...this.buildFilterMeta(activeCategoryId, pickupAddressFilter),
    }, () => this.loadData());
  },

  clearFilters() {
    this.setData({
      activeCategoryId: null,
      pickupAddressFilter: null,
      draftCategoryId: null,
      draftPickupAddress: null,
      filterCount: 0,
      filterSummary: '',
    }, () => this.loadData());
  },

  buildFilterMeta(categoryId, pickupAddress) {
    const names = [];
    const category = this.data.categories.find((item) => Number(item.id) === Number(categoryId));
    if (category) names.push(category.name);
    if (pickupAddress) names.push(`自提：${pickupAddress.upBuilding || pickupAddress.displayText}`);
    return {
      filterCount: names.length,
      filterSummary: names.join(' · '),
    };
  },

  handleEmptyAction() {
    if (this.data.filterCount > 0) {
      this.clearFilters();
      return;
    }
    this.gotoPublish();
  },

  gotoPublish() {
    wx.navigateTo({ url: '/pages/second-hand/publish/publish' });
  },

  gotoDetail(e) {
    wx.navigateTo({ url: `/pages/second-hand/detail/detail?id=${e.currentTarget.dataset.id}` });
  },

  onRefresh() {
    if (this.data.loading) return;
    this.setData({ refreshing: true });
    return Promise.all([this.loadSchoolName(), this.loadData()]);
  },

  // 右上角分享--好友、朋友圈
  onShareAppMessage() {
    return {
      title: '帮帮校园送 · 校园二手好物',
      path: '/pages/second-hand/index/index',
    };
  },
  onShareTimeline() {
    return {
      title: '帮帮校园送 · 校园二手好物',
    };
  },
});
