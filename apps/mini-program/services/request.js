/**
 * 统一请求处理服务
 * 处理 token、401 重试、错误处理等通用逻辑
 */

const tokenManager = require('../utils/tokenManager');
const url = getApp().globalData.API_URL;

/**
 * 安全获取列表数据，避免 null 错误
 * @param {Object} res - 响应对象
 * @returns {Array} 安全的数据数组
 */
function safeList(res) {
  if (res && res.data && Array.isArray(res.data.data)) {
    return res.data.data;
  }
  return [];
}

/**
 * 统一请求方法
 * @param {Object} options - 请求配置
 * @param {string} options.url - 请求地址
 * @param {string} options.method - 请求方法
 * @param {Object} options.data - 请求数据
 * @param {Object} options.header - 请求头
 * @param {boolean} options.skipTokenCheck - 是否跳过token检查（默认false）
 * @returns {Promise} 请求 Promise
 */
function request(options) {
  return new Promise((resolve, reject) => {
    // *** 改进：使用 tokenManager 统一获取 token ***
    const token = tokenManager.getToken();
    
    wx.request({
      url: options.url,
      method: options.method || 'GET',
      data: options.data,
      header: {
        'Content-Type': 'application/json',
        'token': token,
        ...options.header
      },
      success: (res) => {
        if (res.statusCode === 200) {
          resolve(res);
        } else if (res.statusCode === 401) {
          // 401 错误，尝试刷新 token
          console.log('收到401响应，尝试刷新token');
          refreshTokenAndRetry(options)
            .then(resolve)
            .catch(reject);
        } else {
          reject(new Error(`请求失败: ${res.statusCode}`));
        }
      },
      fail: (error) => {
        console.error('网络请求失败:', error);
        reject(error);
      }
    });
  });
}

/**
 * 刷新 token 并重试请求
 * @param {Object} originalOptions - 原始请求配置
 * @returns {Promise} 重试后的请求结果
 */
function refreshTokenAndRetry(originalOptions) {
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
            if (res.statusCode === 200 && res.data.data) {
              console.log('获取新token', res.data.data.token);
              
              // *** 改进：使用 tokenManager 统一更新 token ***
              tokenManager.updateToken(res.data.data.token, res.data.data);
              
              // 重试原始请求
              const retryOptions = {
                ...originalOptions,
                header: {
                  ...originalOptions.header,
                  'token': res.data.data.token
                }
              };
              
              wx.request({
                ...retryOptions,
                success: resolve,
                fail: reject
              });
            } else {
              console.error('登录响应异常:', res);
              reject(new Error('登录响应异常'));
            }
          },
          fail: (res) => {
            console.log('token刷新失败', res);
            wx.showToast({
              title: '验证失效，请重新登录',
              duration: 2000
            });
            wx.switchTab({
              url: '/pages/mine/mine/mine'
            });
            reject(new Error('登录请求失败'));
          }
        });
      },
      fail: (loginError) => {
        console.log('微信登录失败', loginError);
        wx.showToast({
          title: '登录失败',
          duration: 2000
        });
        reject(new Error('微信登录失败'));
      }
    });
  });
}

module.exports = {
  request,
  safeList
};
