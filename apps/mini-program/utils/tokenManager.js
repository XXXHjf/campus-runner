/**
 * Token 管理工具
 * 职责：token 的存储、读取、状态管理
 * 注意：不包含网络请求逻辑，登录逻辑由 services/request.js 处理
 */

// Token 就绪状态
let tokenReady = false;
let tokenReadyCallbacks = [];

const TOKEN_TTL_MS = 20 * 60 * 60 * 1000; // 20h
const TOKEN_UPDATED_AT_KEY = 'tokenUpdatedAt';

function getAppSafe() {
  try {
    return getApp();
  } catch (e) {
    return null;
  }
}

function getTokenUpdatedAt() {
  const stored = wx.getStorageSync(TOKEN_UPDATED_AT_KEY);
  return typeof stored === 'number' ? stored : null;
}

function setTokenUpdatedAt(timestamp) {
  wx.setStorage({
    key: TOKEN_UPDATED_AT_KEY,
    data: timestamp
  });
}

function clearTokenMeta() {
  wx.removeStorageSync(TOKEN_UPDATED_AT_KEY);
}

function isTokenExpired(updatedAt) {
  if (!updatedAt) return false;
  return Date.now() - updatedAt >= TOKEN_TTL_MS;
}

/**
 * 标记 token 已就绪
 */
function markTokenReady() {
  tokenReady = true;
  // 执行所有等待的回调
  tokenReadyCallbacks.forEach(callback => callback());
  tokenReadyCallbacks = [];
}

/**
 * 等待 token 就绪
 * @returns {Promise} 当 token 就绪时 resolve
 */
function waitForToken() {
  return new Promise((resolve) => {
    if (tokenReady) {
      resolve();
    } else {
      tokenReadyCallbacks.push(resolve);
    }
  });
}

/**
 * 安全获取 token
 * @returns {string|null} token 或 null
 */
function getToken() {
  const app = getAppSafe();
  return app?.globalData?.userInfo?.token || null;
}

/**
 * 同步初始化 token（从本地存储）
 * 在 app.js onLaunch 中调用
 */
function initTokenSync() {
  try {
    const userInfo = wx.getStorageSync('userInfo');
    if (userInfo && userInfo.token) {
      const updatedAt = getTokenUpdatedAt();
      if (isTokenExpired(updatedAt)) {
        console.log('Token 已过期，清理缓存');
        const app = getAppSafe();
        if (app && app.globalData) {
          app.globalData.userInfo = null;
        }
        wx.removeStorageSync('userInfo');
        clearTokenMeta();
        markTokenReady();
        return false;
      }

      if (!updatedAt) {
        setTokenUpdatedAt(Date.now());
      }

      const app = getAppSafe();
      if (app && app.globalData) {
        app.globalData.userInfo = userInfo;
      }
      console.log('Token 同步初始化成功');
      markTokenReady();
      return true;
    } else {
      console.log('本地无 token 缓存');
      markTokenReady(); // 即使没有token也标记为就绪
      return false;
    }
  } catch (e) {
    console.error('Token 初始化失败:', e);
    markTokenReady(); // 即使失败也标记为就绪
    return false;
  }
}

/**
 * 更新 token
 * @param {string} token - 新的 token
 * @param {Object} userInfo - 用户信息（可选）
 */
function updateToken(token, userInfo = {}) {
  const app = getAppSafe();
  if (!app || !app.globalData) {
    console.error('Token 更新失败：App 未就绪');
    return;
  }
  
  // 更新全局数据
  app.globalData.userInfo = {
    ...app.globalData.userInfo,
    ...userInfo,
    token: token
  };
  
  // 更新本地存储
  wx.setStorage({
    key: 'userInfo',
    data: app.globalData.userInfo,
    success: () => {
      console.log('Token 更新成功');
    },
    fail: (err) => {
      console.error('Token 存储失败:', err);
    }
  });

  setTokenUpdatedAt(Date.now());
  
  markTokenReady();
}

/**
 * 清除 token
 */
function clearToken() {
  const app = getAppSafe();
  if (app && app.globalData) {
    app.globalData.userInfo = null;
  }
  wx.removeStorageSync('userInfo');
  clearTokenMeta();
  tokenReady = false;
  console.log('Token 已清除');
}

/**
 * 检查 token 是否有效
 * @returns {boolean} token 是否存在
 */
function hasToken() {
  return !!getToken();
}

module.exports = {
  initTokenSync,
  waitForToken,
  getToken,
  updateToken,
  clearToken,
  hasToken,
  markTokenReady,
  getTokenUpdatedAt
};

