// 引入服务和工具
const deliveryAddressService = require('../../../services/deliveryAddressService');
const userService = require('../../../services/userService');
const tokenManager = require('../../../utils/tokenManager');
const { showLoading, hideLoading, showError } = require('../../../utils/transformers');
const { _markLastItem } = require('../../../utils/commonJs');

Page({
  data: {
    userInfo: null,
    tabValue: 0, //0为取件，1为收件
    addressBook: [],
  },
  
  // 跳转新增地址界面
  gotoAdd() {
    const type = this.data.tabValue;
    wx.navigateTo({
      url: `/pages/address/addressAdd/add?type=${type}`,
    })
  },
  
  // 跳转地址详情
  gotoAI(event) {
    const id = event.currentTarget.dataset.item.id;
    wx.navigateTo({
      url: `/pages/address/addressInfo/info?id=${id}`,
    })
  },
  
  // 获取用户地址簿（使用封装的 service）
  async _getAddressBook() {
    try {
      showLoading('加载中');
      const addressBook = await deliveryAddressService.getMyAddresses();
      // 加上 isLast 属性
      const addressWithLast = _markLastItem(addressBook);
      this.setData({
        addressBook: addressWithLast
      });
    } catch (error) {
      console.error('获取地址簿失败:', error);
      showError('获取地址失败');
    } finally {
      hideLoading();
    }
  },
  
  // tabs值更新
  onTabsChange(event) {
    this.setData({
      tabValue: event.detail.value,
    });
  },
  
  onTabsClick(event) {
    this.setData({
      tabValue: event.detail.value,
    });
    // 更新展示卡片
    this._getAddressBook();
  },
  
  // 获取用户数据（使用封装的 service）
  async getGlobalData() {
    try {
      // 等待 token 就绪
      await tokenManager.waitForToken();
      
      if (!tokenManager.hasToken()) {
        console.warn('未登录');
        return;
      }
      
      const userInfo = await userService.getUserInfo();
      this.setData({
        userInfo: {
          ...userInfo,
          token: tokenManager.getToken()
        }
      });
      console.log('系统返回用户数据', userInfo);
    } catch (error) {
      console.error('获取用户数据失败', error);
    }
  },
  
  // 生命周期函数--监听页面显示
  async onShow() {
    try {
      await this.getGlobalData();
      console.log('getGlobalData执行成功');
      await this._getAddressBook();
    } catch (error) {
      console.error('页面初始化失败', error);
    }
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
      message: '刷新成功',
      icon: 'check-circle',
    });
  },
})