var app = getApp();
const url = getApp().globalData.API_URL;
const tokenManager = require('../../utils/tokenManager');
const {
  _getOptions,
  _compareFirstNChars,
  _parseStrDateTime,
  _isTomorrow,
  _formatTime
} = require('../../utils/commonJs');

// 引入新的服务层
const orderService = require('../../services/orderService');
const addressService = require('../../services/addressService');
const bannerService = require('../../services/bannerService');
const userService = require('../../services/userService');
const { showLoading, hideLoading, showError } = require('../../utils/transformers');
const {
  FILTER_TYPES,
  SWIPER_CONFIG,
  LOADING_MESSAGES,
  ERROR_MESSAGES,
  SUCCESS_MESSAGES
} = require('../../utils/constants');

// 轻提示
import Toast from 'tdesign-miniprogram/toast/index';

Page({
  data: {
    userInfo: {},
    takes: [], // 订单列表（数组类型）
    visible: false,
    isLoginChecking: false,
    sortMode: 'time',
    priceSort: 0,
    filterVisible: false,
    filterCount: 0,
    filterSummary: '',
    selectedCategoryId: null,
    selectedCategoryName: '',
    // 轮播图
    current: SWIPER_CONFIG.CURRENT,
    autoplay: SWIPER_CONFIG.AUTOPLAY,
    duration: SWIPER_CONFIG.DURATION,
    interval: SWIPER_CONFIG.INTERVAL,
    swiperList: [],
    // 选择器
    addressList: [], //用于存储地址allList
    areaVisible: false, //选择器判定
    areaValue: [], //选择器取值
    areaText: '',
    text: '',
    schools: [],
    compuses: [],
    buildings: [],
    upType: 0, //0取1收
    upAddress: {},
    upPickUp: null,
    upRecive: null,
    flagAddress: 0,
    flagPickUp: 0,
    flagRecive: 0,
    // 折叠面板
    activeValues: [0],
    // 跑腿分类
    category: [],
    showCategory: null,
    // 弹出层
    cur: {},
    visible: false,

    selectedCompus: '',
    selectedCompusNumberId: '',
    selectedCategory: '',
    selectedCategoryNumberId: '',
    selectedBuilding: '',
    selectedBuildingNumberId: '',

    pickerTag: '', // 标识触发选择器是取件/收件地址
  },

  onLoad() {
    this.loadBanners();
  },

  async loadBanners(schoolId) {
    const requestId = (this._bannerRequestId || 0) + 1;
    this._bannerRequestId = requestId;

    try {
      const banners = await bannerService.getHomepageBanners(schoolId);
      if (requestId !== this._bannerRequestId) return;
      this.setData({
        current: 0,
        swiperList: banners.map((banner) => ({
          ...banner,
          value: banner.imgUrl,
          ariaLabel: banner.title || '首页轮播图'
        }))
      });
    } catch (error) {
      console.error('获取首页轮播图失败:', error);
      if (requestId === this._bannerRequestId && this.data.swiperList.length === 0) {
        this.setData({ swiperList: [] });
      }
    }
  },

  // 详情跳转
  gotoTakesInfo(event) {
    const id = event.currentTarget.dataset.item.id;
    console.log(id)
    wx.navigateTo({
      url: `/pages/orders/takeOrders/takesInfo/info?id=${id}`,
    })
  },
  gotoPublishRunner() {
    wx.navigateTo({
      url: '/pages/orders/myOrders/ordersAdd/add',
    });
  },
  selectTimeSort() {
    if (!this._ensureLoggedIn()) return;
    this.setData({ sortMode: 'time' });
    this.applyFilters();
  },
  togglePriceSort() {
    if (!this._ensureLoggedIn()) return;
    const priceSort = this.data.sortMode === 'price' ? (this.data.priceSort + 1) % 2 : 0;
    this.setData({
      sortMode: 'price',
      priceSort,
    });
    this.applyFilters();
  },
  async openFilter() {
    if (!this._ensureLoggedIn()) return;
    this._filterSnapshot = {
      selectedCategoryId: this.data.selectedCategoryId,
      selectedCategoryName: this.data.selectedCategoryName,
      showCategory: this.data.showCategory,
      upPickUp: this.data.upPickUp,
      upRecive: this.data.upRecive,
    };
    this.setData({ filterVisible: true });
    if (!Array.isArray(this.data.category) || this.data.category.length === 0) {
      await this.getCategory();
    }
  },
  onFilterVisibleChange(e) {
    if (!e.detail.visible && this._filterSnapshot) {
      this.setData({
        ...this._filterSnapshot,
        filterVisible: false,
      });
      this._filterSnapshot = null;
      return;
    }
    this.setData({ filterVisible: e.detail.visible });
  },
  selectFilterCategory(e) {
    const item = e.currentTarget.dataset.item;
    this.setData({
      selectedCategoryId: item ? item.id : null,
      selectedCategoryName: item ? item.categoryName : '',
      showCategory: item ? item.categoryName : null,
    });
  },
  clearUpPickUp() {
    this.setData({ upPickUp: null });
  },
  clearUpRecive() {
    this.setData({ upRecive: null });
  },
  clearFilterSelection() {
    this.setData({
      selectedCategoryId: null,
      selectedCategoryName: '',
      showCategory: null,
      upPickUp: null,
      upRecive: null,
    });
  },
  applyFilterSelection() {
    this._filterSnapshot = null;
    this._updateFilterMeta();
    this.setData({ filterVisible: false });
    this.applyFilters();
  },
  resetFilters() {
    this.clearFilterSelection();
    this._updateFilterMeta();
    this.applyFilters();
  },
  _ensureLoggedIn() {
    if (tokenManager.hasToken()) return true;
    this.noToken();
    return false;
  },
  _updateFilterMeta() {
    const names = [];
    if (this.data.selectedCategoryId != null) names.push(this.data.selectedCategoryName);
    if (this.data.upPickUp) names.push(`取件：${this.data.upPickUp.label.slice(-1)[0]}`);
    if (this.data.upRecive) names.push(`收件：${this.data.upRecive.label.slice(-1)[0]}`);
    this.setData({
      filterCount: names.length,
      filterSummary: names.join(' · '),
    });
  },
  _decorateTakes(takes) {
    return (Array.isArray(takes) ? takes : []).map((item) => ({
      ...item,
      displayTitle: String(item.note || '').split(/\r?\n/)[0],
    }));
  },
  _sortTakes(takes) {
    if (this.data.sortMode !== 'price') return takes;
    const direction = this.data.priceSort === 0 ? -1 : 1;
    return [...takes].sort((a, b) => {
      const aPrice = Number(a.price) || 0;
      const bPrice = Number(b.price) || 0;
      return (aPrice - bPrice) * direction;
    });
  },
  async _loadFilteredTakes() {
    const { upPickUp, upRecive, selectedCategoryId, selectedCategoryName, userInfo } = this.data;
    let takes;

    if (upPickUp && upRecive) {
      takes = await orderService.getOrdersByDoubleAddress(upPickUp, upRecive, userInfo.schoolId);
    } else if (upPickUp) {
      takes = await orderService.getOrdersBySingleAddress(
        upPickUp,
        FILTER_TYPES.PICKUP_ADDRESS,
        userInfo.schoolId
      );
    } else if (upRecive) {
      takes = await orderService.getOrdersBySingleAddress(
        upRecive,
        FILTER_TYPES.RECEIVE_ADDRESS,
        userInfo.schoolId
      );
    } else if (selectedCategoryId != null) {
      takes = await orderService.getOrdersByCategory(selectedCategoryId);
    } else if (this.data.sortMode === 'price') {
      takes = await orderService.getOrdersByPrice(this.data.priceSort);
    } else {
      takes = await orderService.getOrdersByTime();
    }

    if ((upPickUp || upRecive) && selectedCategoryId != null) {
      takes = takes.filter((item) => (
        Number(item.categoryId) === Number(selectedCategoryId)
        || item.categoryName === selectedCategoryName
      ));
    }
    return this._sortTakes(this._decorateTakes(takes));
  },
  async applyFilters() {
    try {
      showLoading(LOADING_MESSAGES.GETTING_ORDERS);
      const takes = await this._loadFilteredTakes();
      this.setData({ takes });
    } catch (error) {
      console.error('获取订单失败:', error);
      showError(ERROR_MESSAGES.GET_ORDERS_FAILED);
    } finally {
      hideLoading();
    }
  },
  getTakeByTime() {
    this.setData({ sortMode: 'time' });
    return this.applyFilters();
  },
  getTakeByPrice() {
    this.setData({ sortMode: 'price' });
    return this.applyFilters();
  },
  getTakeByAddress() {
    return this.applyFilters();
  },
  // 修改当前地址-类型设置
  upPickUp() {
    this.setData({
      flagPickUp: 1,
      flagRecive: 0,
    });
    this.changeAddress();
  },
  upRecive() {
    this.setData({
      flagPickUp: 0,
      flagRecive: 1,
    });
    this.changeAddress();
  },
  upAddress() {
    this.setData({
      flagAddress: 1,
    })
    this.changeAddress();
  },
  // 修改当前地址-选择器
  changeAddress() {
    return new Promise((resolve, reject) => {
      if (this.data.addressList) {
        this.setData({
          areaVisible: true,
          areaValue: [], // 重置选择器的值
        });
        this.getAllAddress()
          .then(() => {
            const {
              schools
            } = this.data.addressList;
            this.setData({
              schools: _getOptions(schools),
            });
            this.lifetimes();
            resolve(); // 成功加载地址数据后 resolve
          })
          .catch((error) => {
            console.log('Failed to change address:', error);
            reject(error); // 加载地址数据失败时 reject
          });
      } else {
        console.log('Address list is undefined or null.');
        reject(new Error('Address list is undefined or null.')); // 地址数据为空时 reject
      }
    });
  },
  lifetimes() {
    const schools = this.data.schools;
    // 初次显示的校区与建筑
    const {
      compuses,
      buildings
    } = this.getCompus(schools[0].value);
    this.setData({
      compuses: compuses,
      buildings: buildings,
    });
  },
  onColumnChange(e) {
    console.log('pick:', e.detail);
    const {
      column,
      index
    } = e.detail;
    const {
      schools,
      compuses
    } = this.data;

    if (column === 0) {
      // 更改学校
      const {
        compuses,
        buildings
      } = this.getCompus(schools[index].value);
      this.setData({
        compuses,
        buildings,
      });
    }
    if (column === 1) {
      // 更改校区
      const buildings = this.getBuildings(compuses[index].value);
      this.setData({
        buildings
      });
    }
    if (column === 2) {
      // 更改建筑
    }
  },
  getCompus(schoolValue) {
    const compuses = _getOptions(this.data.addressList.compuses, (compus) => _compareFirstNChars(compus.value, schoolValue, 3));
    const buildings = this.getBuildings(compuses[0].value);
    return {
      compuses,
      buildings
    };
  },
  getBuildings(compusValue) {
    return _getOptions(this.data.addressList.buildings, (building) => _compareFirstNChars(building.value, compusValue, 6));
  },
  onPickerChange(e) {
    const {
      value,
      label
    } = e.detail;
    console.log('picker confirm:', e.detail);
    this.setData({
      areaVisible: false,
      areaValue: value,
      areaText: label.join(' '),
    });
    if (this.data.flagPickUp == 1) {
      this.setData({
        flagPickUp: 0,
        upPickUp: e.detail,
      })
    };
    if (this.data.flagRecive == 1) {
      this.setData({
        flagRecive: 0,
        upRecive: e.detail,
      })
    };
    if (this.data.flagAddress == 1) {
      this.setData({
        flagAddress: 0,
        upAddress: e.detail,
      })
    };
    this._updateFilterMeta();
  },
  onPickerCancel(e) {
    console.log('picker cancel', e.detail);
    this.setData({
      areaVisible: false,
    });
  },
  // 获取AddressList
  async getAllAddress() {
    try {
      const addressData = await addressService.getThreeLevelAddress();
      const addressList = addressService.convertAddressList(addressData);
      this.setData({ addressList });
    } catch (error) {
      console.error('Failed to get address data:', error);
      throw error;
    }
  },
  // 获取Category
  async getCategory() {
    try {
      showLoading(LOADING_MESSAGES.GETTING_CATEGORY);
      const category = await addressService.getCategories();
      this.setData({ category });
    } catch (error) {
      console.error('获取分类失败:', error);
      showError(ERROR_MESSAGES.GET_CATEGORY_FAILED);
    } finally {
      hideLoading();
    }
  },
  // 刷新缓存信息
  getGlobalData() {
    const that = this;
    return new Promise((resolve, reject) => {
      // *** 改进：等待 token 就绪 ***
      tokenManager.waitForToken().then(async () => {
        // 检查是否有token
        if (!tokenManager.hasToken()) {
          console.log('没有token，显示登录提示');
          reject(new Error('未登录'));
          return;
        }

        try {
          const userInfo = await userService.getUserInfo();
          const token = tokenManager.getToken();
          that.setData({
            userInfo: {
              ...userInfo,
              token
            }
          });
          console.log('系统返回用户数据', that.data.userInfo);
          resolve(userInfo);
        } catch (err) {
          console.log('系统返回用户数据失败', err);
          reject(err);
        }
      });
    });
  },
  // 首次使用（本地无缓存）提示
  onVisibleChange(e) {
    this.setData({
      visible: e.detail.visible,
    });
  },
  know() {
    this.setData({
      visible: false,
    });
    wx.setStorage({
      key: 'privacyAcknowledged',
      data: true
    });
    // this.onShow();
  },
  checkPrivacyAcknowledged() {
    try {
      const acknowledged = wx.getStorageSync('privacyAcknowledged');
      if (!acknowledged) {
        this.setData({ visible: true });
      }
    } catch (error) {
      console.error('读取用户协议状态失败:', error);
      this.setData({ visible: true });
    }
  },
  // 无token提示
  noToken() {
    wx.showToast({
      title: '请先登录',
      icon: 'error',
      mask: true,
      duration: 2000
    })
  },
  // 获取用户协议
  openPrivacyContract() {
    wx.openPrivacyContract({
      success: res => {
        console.log('openPrivacyContract success')
      },
      fail: res => {
        console.error('openPrivacyContract fail', res)
      }
    })
  },
  // 401错误，刷新token
  // 注意：此方法已基本被 services/request.js 的自动重试机制替代
  // 保留用于特殊场景的手动刷新
  updateToken() {
    wx.showLoading({ title: '加载中' });
    
    return new Promise((resolve, reject) => {
      wx.login({
        success: (loginRes) => {
          console.log("code is " + loginRes.code);
          wx.request({
            url: `${url}/api/user/login`,
            method: 'POST',
            data: { code: loginRes.code },
            header: { 'Content-Type': 'application/json' },
            success: (res) => {
              wx.hideLoading();
              
              if (res.statusCode === 200 && res.data.data) {
                const userData = res.data.data;
                // 使用 tokenManager 统一更新
                tokenManager.updateToken(userData.token, userData);
                this.setData({ userInfo: userData });
                resolve(userData);
              } else {
                wx.showToast({ title: '登录失败', icon: 'error' });
                reject(new Error('登录失败'));
              }
            },
            fail: (err) => {
              wx.hideLoading();
              wx.showToast({ title: '网络错误', icon: 'error' });
              reject(err);
            }
          });
        },
        fail: (err) => {
          wx.hideLoading();
          wx.showToast({ title: '登录失败', icon: 'error' });
          reject(err);
        }
      });
    });
  },
  // _getExpectTimeDisplay(timeString, gap) {
  //   const createTime = _parseStrDateTime(timeString);
  //   var expectTime = new Date(createTime.getTime() + gap * 60000);
  //   // 强制进位到下一分钟的起始点（如 15:30:30 → 15:31:00）
  //   if (expectTime.getSeconds() > 0 || expectTime.getMilliseconds() > 0) {
  //     expectTime.setMinutes(expectTime.getMinutes() + 1);
  //     expectTime.setSeconds(0);
  //     expectTime.setMilliseconds(0);
  //   }
  //   const timeStr = _formatTime(expectTime);
  //   return timeStr;
  // },
  onHide() {},
  // 生命周期函数--监听页面显示
  onShow() {
    this.checkPrivacyAcknowledged();
    getApp().refreshMineTabRedDot().catch(() => {});
    this.setData({ isLoginChecking: true });
    tokenManager.waitForToken().then(() => {
      if (!tokenManager.hasToken()) {
        const app = getApp();
        const silentLogin = app.globalData?.silentLoginPromise || app.silentLogin?.();
        Promise.resolve(silentLogin).then(() => {
          if (!tokenManager.hasToken()) {
            this.setData({
              userInfo: {},
              isLoginChecking: false
            });
            return;
          }

          const token = tokenManager.getToken();
          this.setData({
            userInfo: { token }
          });

          this.getGlobalData()
            .then(() => {
              this.applyFilters();
              this.loadBanners(this.data.userInfo.schoolId);
            })
            .catch(() => {})
            .finally(() => {
              this.setData({ isLoginChecking: false });
            });
        });
        return;
      }

      const token = tokenManager.getToken();
      this.setData({
        userInfo: { token }
      });

      this.getGlobalData()
        .then(() => {
          this.applyFilters();
          this.loadBanners(this.data.userInfo.schoolId);
        })
        .catch(() => {})
        .finally(() => {
          this.setData({ isLoginChecking: false });
        });
    });
  },
  // 下拉刷新事件
  onPullDownRefresh() {
    // 这里加上要刷新的逻辑
    this.onShow();
    // ------------
    this.showHorizontalText()
    wx.stopPullDownRefresh()
  },
  // 轻展示的方法
  showHorizontalText() {
    Toast({
      context: this,
      selector: '#t-toast',
      message: SUCCESS_MESSAGES.REFRESH_SUCCESS,
      icon: 'check-circle',
    });
  },
  // 右上角分享--好友、朋友圈
  onShareAppMessage() {
    return {
      title: '帮帮校园送',
      path: '/pages/index/index'
    }
  },
  onShareTimeline() {
    return {
      title: '帮帮校园送',
      path: '/pages/index/index'
    }
  },

  // 按地址类型筛选，点击收件/取件时弹出三级列表，并根据点击位置设置 pickerTag 为收件或取件类型
  handleShowPicker(e) {
    const triplePicker = this.selectComponent('#triplePicker');
    if (triplePicker) {
      triplePicker.showPicker();
    }
    // tag值为pickup表示取件，receive表示收件
    var tag = e.currentTarget.dataset.tag;
    this.setData({
      pickerTag: tag
    });
  },
  // 获取自定义组件 triple-address-pricker 传递的数据
  handleConfirmSelection(e) {
    const {
      upCompus,
      upCompusNumberId,
      upCategory,
      upCategoryNumberId,
      upBuilding,
      upBuildingNumberId
    } = e.detail;
    console.log(upCompus, upCategory, upBuilding);

    // 判断取件收件地址，给不同属性赋值
    if (this.data.pickerTag == "pickup") {
      // 设置取件地址信息
      this.setData({
        upPickUp: {
          label: [upCompus, upCategory, upBuilding],
          value: [upCompusNumberId, upCategoryNumberId, upBuildingNumberId]
        }
      });
      console.log('设置取件地址:', this.data.upPickUp);
    } else if (this.data.pickerTag == "recive") {
      // 设置收件地址信息
      this.setData({
        upRecive: {
          label: [upCompus, upCategory, upBuilding],
          value: [upCompusNumberId, upCategoryNumberId, upBuildingNumberId]
        }
      });
      console.log('设置收件地址:', this.data.upRecive);
    }
    
    // 更新页面显示信息
    this.setData({
      showPicker: false,
      selectedCompus: upCompus,
      selectedCompusNumberId: upCompusNumberId,
      selectedCategory: upCategory,
      selectedCategoryNumberId: upCategoryNumberId,
      selectedBuilding: upBuilding,
      selectedBuildingNumberId: upBuildingNumberId,
    });

  },

})
