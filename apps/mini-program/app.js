// app.js
const tokenManager = require('./utils/tokenManager');

App({
  // 认证成功后更新全局 userInfo
  onUserInfoUpdated(userInfo) {
    // 使用 tokenManager 统一管理
    if (userInfo && userInfo.token) {
      tokenManager.updateToken(userInfo.token, userInfo);
      console.log('用户信息已更新', userInfo);
    } else {
      this.globalData.userInfo = userInfo;
      // 写入缓存
      wx.setStorage({
        key: "userInfo",
        data: userInfo,
        success: function () {
          console.log('写入userInfo成功', userInfo)
        },
        fail: function () {
          console.log('写入userInfo发生错误', userInfo)
        }
      });
    }
  },

  onLaunch() {
    console.log('=== 小程序启动 ===');
    
    // 展示本地存储能力
    const logs = wx.getStorageSync('logs') || []
    logs.unshift(Date.now())
    wx.setStorageSync('logs', logs)

    // *** 使用同步方法初始化 token ***
    // 这样可以确保在页面/组件加载前 token 已经准备好
    tokenManager.initTokenSync();
    
    console.log('Token 初始化完成, 当前 token:', tokenManager.getToken());

    if (!tokenManager.hasToken()) {
      this.globalData.silentLoginPromise = this.silentLogin();
    }
  },
  // 静默登录，用于启动时无感刷新 token
  silentLogin() {
    console.log('尝试静默登录');
    const url = this.globalData.API_URL;

    return new Promise((resolve) => {
      wx.login({
        success: (loginRes) => {
          wx.request({
            url: `${url}/api/user/login`,
            method: 'POST',
            data: { code: loginRes.code },
            header: { 'Content-Type': 'application/json' },
            timeout: 10000,
            success: (res) => {
              const token = res.data?.data?.token;
              if (res.statusCode === 200 && token) {
                tokenManager.updateToken(token);
                console.log('静默登录成功');
                resolve(true);
              } else {
                console.log('静默登录失败', res);
                resolve(false);
              }
            },
            fail: (err) => {
              console.log('静默登录请求失败', err);
              resolve(false);
            }
          });
        },
        fail: (err) => {
          console.log('静默登录 wx.login 失败', err);
          resolve(false);
        }
      });
    });
  },

  globalData: {
    userInfo: null,
    silentLoginPromise: null,
    // Local development only. Turn on together with backend mock-payment-enabled when testing without real WeChat Pay.
    MOCK_PAYMENT: false,
    //后端访问地址
    // API_URL: 'http://localhost:8080',
    // API_URL : 'http://47.99.105.120:8080',
    API_URL : 'https://www.campusrunner.top:443',
  }
})
