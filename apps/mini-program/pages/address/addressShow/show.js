const feedback = require('../../../utils/feedback');
// 引入服务和工具
const deliveryAddressService = require('../../../services/deliveryAddressService');
const userService = require('../../../services/userService');
const tokenManager = require('../../../utils/tokenManager');
const { showLoading, hideLoading, showError } = require('../../../utils/transformers');

Page({
  retryFeedbackLoad() { return this._feedbackRetry?.(); },
  data: {
    userInfo: null,
    addressBook: [],
  },

  // 跳转新增地址界面
  gotoAdd() {
    wx.navigateTo({
      url: '/pages/address/addressAdd/add',
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
    feedback.loaded(this);
    try {
      showLoading('加载中');
      const addressBook = await deliveryAddressService.getMyAddresses();
      this.setData({
        addressBook
      });
    } catch (error) {
      console.error('获取地址簿失败:', error);
      feedback.loadError(this, '加载失败，请重试', () => this._getAddressBook(), !!this.data.addressBook.length);
    } finally {
      hideLoading();
    }
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
  async onPullDownRefresh() {
    try {
      await this._getAddressBook();

    } finally {
      wx.stopPullDownRefresh();
    }
  },
})
