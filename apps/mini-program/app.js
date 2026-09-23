// app.js
const tokenManager = require('./utils/tokenManager');

App({
  async refreshMineTabRedDot(options = {}) {
    if (!tokenManager.hasToken() && this.globalData.silentLoginPromise) {
      try {
        await this.globalData.silentLoginPromise;
      } catch (error) {
        // 红点刷新失败不影响主流程
      }
    }
    const mineTabBadgeService = require('./services/mineTabBadgeService');
    return mineTabBadgeService.refreshMineTabRedDot(options);
  },

  // 认证成功后更新全局 userInfo
  onUserInfoUpdated(userInfo) {
    // 使用 tokenManager 统一管理
    if (userInfo && userInfo.token) {
      tokenManager.updateToken(userInfo.token, userInfo);
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
    this.refreshMineTabRedDot({ force: true, userInfo }).catch(() => {});
  },

  onLaunch() {
    console.log('=== 小程序启动 ===');

    // *** 使用同步方法初始化 token ***
    // 这样可以确保在页面/组件加载前 token 已经准备好
    tokenManager.initTokenSync();
    // Only restore a session after the user has previously chosen to log in.
    this.restoreLogin().catch(() => {});
  },

  onShow() {
    this.restoreLogin()
      .then(() => this.refreshMineTabRedDot())
      .catch(() => {});
  },
  restoreLogin({ force = false } = {}) {
    if (!tokenManager.hasLoginIntent()) return Promise.resolve(false);
    if (!force && !tokenManager.needsRefresh()) {
      return this.globalData.silentLoginPromise || Promise.resolve(true);
    }
    return this.silentLogin();
  },
  // Manual login and recovery of a previously chosen login share one request.
  silentLogin() {
    if (this.globalData.silentLoginPromise) return this.globalData.silentLoginPromise;
    console.log('尝试静默登录');
    const url = this.globalData.API_URL;
    const sessionVersion = tokenManager.getSessionVersion();

    const loginPromise = new Promise((resolve) => {
      wx.login({
        success: (loginRes) => {
          if (!loginRes.code) { resolve(false); return; }
          wx.request({
            url: `${url}/api/user/login`,
            method: 'POST',
            data: { code: loginRes.code },
            header: { 'Content-Type': 'application/json' },
            timeout: 10000,
            success: (res) => {
              const token = res.data?.data?.token;
              if (res.statusCode === 200 && res.data?.code === 1 && token) {
                if (sessionVersion !== tokenManager.getSessionVersion()) {
                  resolve(false);
                  return;
                }
                try { tokenManager.updateToken(token, { id: res.data.data.id }); }
                catch (error) { resolve(false); return; }
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
    this.globalData.silentLoginPromise = loginPromise;
    const clearPending = () => {
      if (this.globalData.silentLoginPromise === loginPromise) {
        this.globalData.silentLoginPromise = null;
      }
    };
    loginPromise.then(clearPending, clearPending);
    return loginPromise;
  },

  globalData: {
    userInfo: null,
    silentLoginPromise: null,
    // Local development only. Turn on together with backend mock-payment-enabled when testing without real WeChat Pay.
    MOCK_PAYMENT: true,
    //后端访问地址
    // API_URL: 'http://localhost:8080',
    // API_URL : 'http://47.99.105.120:8080',
    API_URL : 'https://www.campusrunner.top:443',
  }
})
