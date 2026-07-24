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
const userService = require('../../services/userService');
const { debounce, showLoading, hideLoading, showSuccess, showError } = require('../../utils/transformers');
const {
  TAB_TYPES,
  ORDER_STATUS,
  PRICE_SORT_STATUS,
  ADDRESS_TYPES,
  FILTER_TYPES,
  TAB_CONFIG,
  SWIPER_CONFIG,
  DEBOUNCE_DELAY,
  LOADING_MESSAGES,
  ERROR_MESSAGES,
  SUCCESS_MESSAGES
} = require('../../utils/constants');

// 动作面板，用于筛选选择
import ActionSheet, {
  ActionSheetTheme
} from 'tdesign-miniprogram/action-sheet/index';
// 轻提示
import Toast from 'tdesign-miniprogram/toast/index';

// 轮播图
const imageCdn = 'https://tdesign.gtimg.com/mobile/demos';
const swiperList = [{
  value: `https://campus-runner.oss-cn-hangzhou.aliyuncs.com/other/swiper/b51e16fc-2e83-4261-8640-8730dc29ac64.jpg?Expires=1813539269&OSSAccessKeyId=LTAI5tF5ytFN5eSVf3cjR92a&Signature=xZIQk816u8To%2FuXS0qnvGiyh2v0%3D`,
  ariaLabel: '图片0',
}, {
  value: `${imageCdn}/swiper1.png`,
  ariaLabel: '图片1',
}, {
  value: `${imageCdn}/swiper2.png`,
  ariaLabel: '图片2',
}];

Page({
  data: {
    userInfo: {},
    takes: [], // 订单列表（数组类型）
    tab: TAB_TYPES.TIME_SORT, // tab标签
    visible: false,
    isLoginChecking: false,

    orderStatus: ORDER_STATUS.DISABLED, // 订单状态
    icon1: TAB_CONFIG.ICONS,
    label1: TAB_CONFIG.LABELS,
    // 轮播图
    current: SWIPER_CONFIG.CURRENT,
    autoplay: SWIPER_CONFIG.AUTOPLAY,
    duration: SWIPER_CONFIG.DURATION,
    interval: SWIPER_CONFIG.INTERVAL,
    swiperList,
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
    upPickUp: {},
    upRecive: {},
    flagAddress: 0,
    flagPickUp: 0,
    flagRecive: 0,
    // 折叠面板
    activeValues: [0],
    // 跑腿分类
    category: {},
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

  // 初始化防抖方法
  onLoad() {
    // 创建防抖的地址筛选方法
    this.debouncedGetTakeByAddress = debounce(this.getTakeByAddress.bind(this), DEBOUNCE_DELAY);
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
  //tabs值更新
  onTabsClick(event) {
    this.setData({
      tab: event.detail.value,
    });
    // 功能实现
    try {
      // *** 改进：使用 tokenManager 检查 token ***
      if (tokenManager.hasToken()) {
        console.log('token不为空，tabs正常', this.data.tab);
        if (this.data.tab != TAB_TYPES.PRICE_SORT) {
          this.setData({
            orderStatus: ORDER_STATUS.RESET_FILTER,
          });
        } else {
          this.setData({
            orderStatus: (this.data.orderStatus + 1) % 2
          })
        }
        console.log('orderStatus正常', this.data.orderStatus);
        if (this.data.tab == TAB_TYPES.TIME_SORT) {
          this.getTakeByTime();
        };
        if (this.data.tab == TAB_TYPES.PRICE_SORT) {
          this.getTakeByPrice();
        };
        if (this.data.tab == TAB_TYPES.QUICK_FILTER) {
          this.setData({
            upPickUp: null,
            upRecive: null,
            showCategory: null,
          });
          this.searchActio();
        };
      } else {
        console.log('token为空，tabs禁用');
        this.noToken();
        this.setData({
          orderStatus: ORDER_STATUS.RESET_FILTER
        })
        console.log('orderStatus禁用', this.data.orderStatus);
      }
    } catch (error) {
      console.error('错误抛出：', error);
      this.setData({
        orderStatus: ORDER_STATUS.RESET_FILTER
      })
      this.noToken();
    }
  },
  // 筛选选择
  searchActio() {
    // 动作面板
    ActionSheet.show({
      theme: ActionSheetTheme.List,
      selector: '#t-action-sheet',
      context: this,
      items: [{
          label: '按照跑腿分类筛选',
          icon: 'task-visible',
        },
        {
          label: '按照地址类型筛选',
          icon: 'map-search',
        },
      ],
    });
  },
  handleSelected(e) {
    // console.log(e.detail);
    this.setData({
      orderStatus: e.detail.index,
    });
    console.log('筛选种类，0分类1地址', this.data.orderStatus)
    if (this.data.tab == TAB_TYPES.QUICK_FILTER && this.data.orderStatus == ORDER_STATUS.CATEGORY_FILTER) {
      this.getCategory();
    };
  },
  clearUpPickUp() {
    this.setData({
      upPickUp: null
    })
    this.getTakeByAddress();
  },
  clearUpRecive() {
    this.setData({
      upRecive: null
    })
    this.getTakeByAddress();
  },
  // 获取订单信息-综合排序
  async getTakeByTime() {
    try {
      showLoading(LOADING_MESSAGES.GETTING_ORDERS);
      const takes = await orderService.getOrdersByTime();
      this.setData({ takes });
    } catch (error) {
      console.error('获取订单失败:', error);
      showError(ERROR_MESSAGES.GET_ORDERS_FAILED);
    } finally {
      hideLoading();
    }
  },
  // 获取订单信息-价格排序
  async getTakeByPrice() {
    try {
      showLoading(LOADING_MESSAGES.GETTING_ORDERS);
      const status = this.data.orderStatus;
      const takes = await orderService.getOrdersByPrice(status);
      this.setData({ takes });
    } catch (error) {
      console.error('获取订单失败:', error);
      showError(ERROR_MESSAGES.GET_ORDERS_FAILED);
    } finally {
      hideLoading();
    }
  },
  // 获取订单信息-分类筛选
  async getTakeByCategory(event) {
    try {
      showLoading(LOADING_MESSAGES.GETTING_ORDERS);
      const item = event.currentTarget.dataset.item;
      this.setData({
        showCategory: item.categoryName,
      });
      const takes = await orderService.getOrdersByCategory(item.id);
      this.setData({ takes });
    } catch (error) {
      console.error('获取订单失败:', error);
      showError(ERROR_MESSAGES.GET_ORDERS_FAILED);
    } finally {
      hideLoading();
    }
  },
  // 获取订单信息-地址筛选
  async getTakeByAddress() {
    try {
      showLoading(LOADING_MESSAGES.GETTING_ORDERS);
      let takes;
      
      if (this.data.upPickUp == null || this.data.upRecive == null) {
        // 单向筛选
        if (this.data.upPickUp != null) {
          this.setData({
            upType: ADDRESS_TYPES.PICKUP,
            upAddress: this.data.upPickUp,
          });
          takes = await orderService.getOrdersBySingleAddress(
            this.data.upPickUp, 
            FILTER_TYPES.PICKUP_ADDRESS,
            this.data.userInfo.schoolId
          );
        } else if (this.data.upRecive != null) {
          this.setData({
            upType: ADDRESS_TYPES.RECEIVE,
            upAddress: this.data.upRecive,
          });
          takes = await orderService.getOrdersBySingleAddress(
            this.data.upRecive, 
            FILTER_TYPES.RECEIVE_ADDRESS,
            this.data.userInfo.schoolId
          );
        } else {
          // 都没有选择，返回综合排序
          takes = await orderService.getOrdersByTime();
        }
      } else {
        // 双向筛选
        takes = await orderService.getOrdersByDoubleAddress(
          this.data.upPickUp,
          this.data.upRecive,
          this.data.userInfo.schoolId
        );
      }
      
      this.setData({ takes });
    } catch (error) {
      console.error('获取订单失败:', error);
      showError(ERROR_MESSAGES.GET_ORDERS_FAILED);
    } finally {
      hideLoading();
    }
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
    this.getTakeByAddress();
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
  // 生命周期函数--监听离开显示
  onHide() {
    this.setData({
      tab: TAB_TYPES.TIME_SORT
    })
  },
  // 生命周期函数--监听页面显示
  onShow() {
    this.checkPrivacyAcknowledged();
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
              // 在获取到userInfo之后调用getTakeByTime
              this.getTakeByTime();
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
          // 在获取到userInfo之后调用getTakeByTime
          this.getTakeByTime();
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

    // 根据选择的地址刷新订单列表（防抖处理）
    this.debouncedGetTakeByAddress();
  },

})
