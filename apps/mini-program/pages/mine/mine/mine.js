// 引入服务和工具
const userService = require('../../../services/userService');
const takeOrderService = require('../../../services/takeOrderService');
const userOrderService = require('../../../services/userOrderService');
const tokenManager = require('../../../utils/tokenManager');
const { showLoading, hideLoading, showError } = require('../../../utils/transformers');

import Dialog from 'tdesign-miniprogram/dialog/index'; //对话框
import Toast from 'tdesign-miniprogram/toast/index'; // 轻提示

const app = getApp();
const url = getApp().globalData.API_URL;
const UNPAID_TIMEOUT_MS = 30 * 60 * 1000;

Page({
  data: {
    wxInfo: null,
    userInfo: null,
    loginLoadShow: false,
    defaultAvatarUrl: 'https://mmbiz.qpic.cn/mmbiz/icTdbqWNOwNRna42FI242Lcia07jQodd2FJGIYQfG0LAJGFxM4FbnQP6yfMxBgJ0F3YRqJCJ1aPAK2dQagdusBZg/0',
    shareFlag: false,
    feedbackFlag: false,
    showCompleteInfoDialog: false, // 控制信息完善弹窗

    menuSections: [{
        title: '跑腿服务',
        items: [{
            value: 'label_1',
            label: '我发布的',
            icon: 'send',
          },
          {
            value: 'label_2',
            label: '我的接单',
            icon: 'task',
          },
        ],
      },
      {
        title: '二手交易',
        items: [{
            value: 'label_7',
            label: '我的闲置',
            icon: 'shop',
          },
          {
            value: 'label_8',
            label: '二手订单',
            icon: 'order-ascending',
          },
          {
            value: 'label_9',
            label: '议价记录',
            icon: 'chat-message',
          },
        ],
      },
      {
        title: '账户资料',
        items: [{
            value: 'label_3',
            label: '地址管理',
            icon: 'location',
          },
          {
            value: 'label_4',
            label: '个人信息',
            icon: 'user-list',
          },
        ],
      },
    ],
    notReceiveOrderList: [],
    unpaidOrderList: [],
    hasShownCompleteInfoDialog: false, // 标记是否已显示过弹窗（本次会话）
  },

  onMenuTap(e) {
    if (this.data.userInfo == null) {
      this.loginTap();
      return;
    }
    const action = e.currentTarget.dataset.value;
    if (this[action]) {
      this[action]();
    }
  },
  // 跳转tabBar对应界面
  label_1() {
    wx.navigateTo({
      url: '/pages/orders/myOrders/ordersShow/show',
    })
  },
  label_2() {
    wx.navigateTo({
      url: '/pages/orders/takeOrders/takesShow/show',
    })
  },
  label_3() {
    wx.navigateTo({
      url: '/pages/address/addressShow/show',
    })
  },
  label_4() {
    wx.navigateTo({
      url: '/pages/mine/userInfo/info',
    })
  },
  label_7() {
    wx.navigateTo({
      url: '/pages/second-hand/my-products/my-products',
    })
  },
  label_8() {
    wx.navigateTo({
      url: '/pages/second-hand/orders/orders',
    })
  },
  label_9() {
    wx.navigateTo({
      url: '/pages/second-hand/bargains/bargains',
    })
  },
  // 点击头像登陆，读取缓存里的用户信息
  loginTap() {
    wx.getStorage({
      key: "userInfo",
      success: (res) => {
        if (res.data == null) {
          // 有缓存，但是null：弹出微信授权登陆
          const dialogConfig = {
            context: this,
            title: '登录',
            content: '使用微信授权登录',
            confirmBtn: '确定',
            cancelBtn: '取消',
          };
          Dialog.confirm(dialogConfig)
            .then(async () => {
              this.setData({
                loginLoadShow: true
              })
              const userInfo = await this._getUserProfile()
              this.setData({
                wxInfo: userInfo
              })
              this._login()
              console.log('点击了确定')
            })
            .catch(() => console.log('点击了取消'))
            .finally(() => Dialog.close());
        } else {
          // 有缓存，不为null：跳转认证
          console.log('已登录', res);
          wx.navigateTo({
            url: '/pages/mine/identify/identify',
          });
        }
      },
      fail: () => {
        // 没有缓存：弹出微信授权登陆
        const dialogConfig = {
          context: this,
          title: '登录',
          content: '使用微信授权登录',
          confirmBtn: '确定',
          cancelBtn: '取消',
        };
        Dialog.confirm(dialogConfig)
          .then(async () => {
            this.setData({
              loginLoadShow: true
            })
            const userInfo = await this._getUserProfile()
            this.setData({
              wxInfo: userInfo
            })
            this._login()
            console.log('点击了确定')
          })
          .catch(() => console.log('点击了取消'))
          .finally(() => Dialog.close());
      }
    })
  },
  // 微信提供的getUserProfile接口，获取微信用户的信息，将此方法promise化
  _getUserProfile() {
    return new Promise((resolve, reject) => {
      wx.getUserProfile({
        desc: '微信授权登陆',
        success: (res) => {
          resolve(res.userInfo)
        }
      })
    })
  },
  // 调用用户登陆的接口
  async _login() {
    try {
      // 获取微信登录 code
      const loginRes = await new Promise((resolve, reject) => {
        wx.login({
          success: resolve,
          fail: reject
        });
      });

      console.log("code is " + loginRes.code);

      // 调用登录接口获取 token（这个接口比较特殊，直接使用 wx.request）
      const loginResult = await new Promise((resolve, reject) => {
        wx.request({
          url: `${url}/api/user/login`,
          method: 'POST',
          data: {
            code: loginRes.code
          },
          header: {
            'Content-Type': 'application/json'
          },
          success: resolve,
          fail: reject
        });
      });

      console.log('登录成功', loginResult.data.data);
      
      // token存入缓存与全局变量userInfo中
      const token = {
        token: loginResult.data.data.token
      };
      
      wx.setStorage({
        key: "userInfo",
        data: token,
      });
      app.globalData.userInfo = token;
      
      // 更新 tokenManager
      tokenManager.updateToken(token.token);

      // 获取用户信息（使用封装的 service）
      try {
        const userInfo = await userService.getUserInfo();
        console.log('获取用户信息:', userInfo);
        
        this.setData({
          userInfo: {
            ...userInfo,
            token: token.token
          }
        });

        // 判断是否为新用户并进行初始化
        if (userInfo.createTime == userInfo.updateTime) {
          wx.navigateTo({
            url: '/pages/mine/newUser/index',
          });
        } else {
          wx.showToast({
            title: '登录成功',
            icon: 'success',
            duration: 2000
          });
        }

        // 关闭加载中的遮罩层
        this.setData({
          loginLoadShow: false
        });
      } catch (userInfoError) {
        console.error('获取用户信息失败:', userInfoError);
        this.setData({
          loginLoadShow: false
        });
        this.showLoginFail();
      }
    } catch (error) {
      // 如果登陆失败，关闭加载中的遮罩层，弹出登陆失败
      console.error('登录失败:', error);
      this.setData({
        loginLoadShow: false
      });
      
      // 根据错误类型显示不同的提示
      if (error.errMsg && error.errMsg.includes('timeout')) {
        wx.showToast({
          title: '网络超时，请检查网络连接',
          icon: 'none',
          duration: 3000
        });
      } else {
        this.showLoginFail();
      }
    }
  },
  // 更新或新建用户信息（使用封装的 service）
  async _updateUserInfo() {
    try {
      const userData = {
        phone: this.data.userInfo.phone,
        sex: this.data.userInfo.sex,
      };
      await userService.updateUserInfo(userData);
      console.log('更新用户信息成功');
    } catch (error) {
      console.error('更新用户信息失败:', error);
      showError('更新失败');
    }
  },
  // 退出登录
  logoutBtnClick() {
    const dialogConfig = {
      context: this,
      title: '退出登录',
      content: '您确定要退出登陆吗？',
      confirmBtn: '确定',
      cancelBtn: '取消',
    };
    Dialog.confirm(dialogConfig)
      .then(async () => {
        // 清除页面数据
        this.setData({
          userInfo: null,
          notReceiveOrderList: [],
          unpaidOrderList: []
        });
        
        // 清除全局数据
        app.globalData.userInfo = null;
        
        // 清除 tokenManager
        tokenManager.clearToken();
        
        // 清除缓存
        wx.setStorage({
          key: 'userInfo',
          data: null,
          success() {
            console.log("清除 userInfo 缓存成功");
            // 重新加载页面，销毁其他页面数据
            wx.reLaunch({
              url: '/pages/mine/mine/mine'
            });
          }
        });
      })
      .catch(() => console.log('点击了取消'))
      .finally(() => Dialog.close());
  },
  // 未收款订单跳转 - 优化：跳转到我的接单页面
  tapToReceive() {
    const orderCount = this.data.notReceiveOrderList.length;
    
    if (orderCount === 1) {
      // 只有一个订单时，直接跳转到订单详情
      const id = this.data.notReceiveOrderList[0].orderId;
      wx.navigateTo({
        url: `/pages/orders/takeOrders/takesInfo/info?id=${id}`,
      });
    } else {
      // 多个订单时，跳转到"我的接单"页面让用户选择
      wx.navigateTo({
        url: '/pages/orders/takeOrders/takesShow/show',
      });
    }
  },
  // 查询已经完成但是未收款的订单（使用封装的 service）
  async _getOrderWithNotReceive() {
    try {
      return await takeOrderService.getNotWithdrawnOrders();
    } catch (error) {
      console.error('获取未收款订单失败:', error);
      return [];
    }
  },
  // 邀请好友（button属性open-type="share"，该方法用于cell背景透明）
  share: function () {
    // 设置 shareFlag 为 true
    this.setData({
      shareFlag: true
    });
    // 超时后设置 shareFlag 为 false
    setTimeout(() => {
      this.setData({
        shareFlag: false
      });
    }, 100);
  },
  // 意见反馈（button属性open-type="feedback"，该方法用于cell背景透明）
  feedback: function () {
    // 设置 shareFlag 为 true
    this.setData({
      feedbackFlag: true
    });
    // 超时后设置 shareFlag 为 false
    setTimeout(() => {
      this.setData({
        feedbackFlag: false
      });
    }, 100);
  },
  // 获取用户数据（使用封装的 service）
  async getGlobalData() {
    try {
      const userInfo = await userService.getUserInfo();
      const app = getApp();
      const userInfoWithToken = {
        ...userInfo,
        token: app.globalData.userInfo?.token || tokenManager.getToken()
      };
      
      this.setData({ userInfo: userInfoWithToken });
      console.log('获取用户数据成功:', userInfo);
      return userInfo;
    } catch (error) {
      console.error('获取用户数据失败:', error);
      throw error;
    }
  },
  
  // 检查用户信息是否完整
  checkUserInfoComplete(userInfo) {
    if (!userInfo) return false;
    
    // 检查是否有手机号
    const hasPhone = userInfo.phone && userInfo.phone.trim() !== '';
    
    // 检查是否已认证（authentication === 1 表示已认证）
    const isAuthenticated = userInfo.authentication === 1;
    
    return hasPhone && isAuthenticated;
  },

  _parseDateTime(dateStr) {
    if (!dateStr) return null;
    const normalized = String(dateStr).replace(/-/g, '/');
    const parsed = new Date(normalized);
    return isNaN(parsed.getTime()) ? null : parsed;
  },

  _getRemainPayTime(createTime) {
    const createdAt = this._parseDateTime(createTime);
    if (!createdAt) return 0;
    return createdAt.getTime() + UNPAID_TIMEOUT_MS - Date.now();
  },

  // 查询未支付订单，超时自动删除
  async _getUnpaidOrders() {
    try {
      const orders = await userOrderService.getMyOrders();
      const unpaidOrders = orders.filter(order => Number(order.status) === -1);

      const validOrders = [];
      const expiredOrderIds = [];

      unpaidOrders.forEach(order => {
        const remainMs = this._getRemainPayTime(order.createTime);
        if (remainMs <= 0) {
          expiredOrderIds.push(order.id);
        } else {
          validOrders.push({ ...order, remainMs });
        }
      });

      if (expiredOrderIds.length > 0) {
        await Promise.all(
          expiredOrderIds.map(id =>
            userOrderService.deleteOrder(id).catch(err => {
              console.warn('自动删除未支付订单失败:', id, err);
            })
          )
        );
      }

      return validOrders;
    } catch (error) {
      console.error('获取未支付订单失败:', error);
      return [];
    }
  },

  // 未支付订单跳转
  tapToUnpaid() {
    const orderCount = this.data.unpaidOrderList.length;
    if (orderCount <= 0) return;

    if (orderCount === 1) {
      const id = this.data.unpaidOrderList[0].id;
      wx.navigateTo({
        url: `/pages/orders/myOrders/ordersInfo/info?id=${id}`,
      });
      return;
    }

    wx.navigateTo({
      url: '/pages/orders/myOrders/ordersShow/show',
    });
  },
  
  // 显示信息完善引导弹窗
  showCompleteInfoDialog() {
    // 如果本次会话已经显示过，不再重复显示
    if (this.data.hasShownCompleteInfoDialog) {
      return;
    }
    
    const userInfo = this.data.userInfo;
    if (!userInfo) return;
    
    // 检查是否是新用户刚注册（昵称已填，但信息未完善）
    const hasUsername = userInfo.username && userInfo.username.trim() !== '' && userInfo.username !== '微信用户';
    const isInfoIncomplete = !this.checkUserInfoComplete(userInfo);
    
    // 如果有昵称但信息不完整，显示弹窗
    if (hasUsername && isInfoIncomplete) {
      this.setData({ 
        showCompleteInfoDialog: true,
        hasShownCompleteInfoDialog: true // 标记已显示
      });
      console.log('[信息完善提示] 显示引导弹窗');
    }
  },
  
  // 点击"去完善"按钮
  handleCompleteInfo() {
    this.setData({ showCompleteInfoDialog: false });
    // 跳转到个人信息页面
    wx.navigateTo({
      url: '/pages/mine/userInfo/info',
    });
  },
  
  // 点击"稍后再说"按钮
  handleCancelCompleteInfo() {
    this.setData({ showCompleteInfoDialog: false });
    console.log('[信息完善提示] 用户选择稍后再说');
  },
  // 生命周期函数--监听页面显示
  async onShow() {
    // 检查是否已登录（有 token）
    const hasToken = tokenManager.hasToken();
    
    if (!hasToken) {
      // 未登录状态，清空数据
      console.log('未登录，跳过数据加载');
      this.setData({
        userInfo: null,
        notReceiveOrderList: [],
        unpaidOrderList: []
      });
      return;
    }

    // 已登录状态，加载用户数据
    try {
      await this.getGlobalData();
      
      // 获取未收款订单
      const notReceiveOrders = await this._getOrderWithNotReceive();
      const unpaidOrders = await this._getUnpaidOrders();
      this.setData({
        notReceiveOrderList: notReceiveOrders,
        unpaidOrderList: unpaidOrders
      });
      
      // 检查是否需要显示信息完善引导弹窗
      this.showCompleteInfoDialog();
    } catch (err) {
      console.error('页面数据加载失败:', err);
      // 如果是 401 错误，说明 token 已失效，清空登录状态
      this.setData({
        userInfo: null,
        notReceiveOrderList: [],
        unpaidOrderList: []
      });
    }
  },
  // 轻展示的方法
  showLoginFail() {
    Toast({
      context: this,
      selector: '#t-toast',
      message: '登陆失败，请稍后重试',
      icon: 'close-circle',
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
  }
})
