/**
 * Token 管理工具
 * 职责：token 的存储、读取、状态管理
 * 注意：不包含网络请求逻辑，登录逻辑由 services/request.js 处理
 */

// Token 就绪状态
let tokenReady = false;
let tokenReadyCallbacks = [];

const TOKEN_TTL_MS = 20 * 60 * 60 * 1000; // 20h
const TOKEN_REFRESH_AHEAD_MS = 30 * 60 * 1000;
const TOKEN_UPDATED_AT_KEY = 'tokenUpdatedAt';
const LOGIN_INTENT_KEY = 'hasLoggedIn';
let sessionVersion = 0;

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
  wx.setStorageSync(TOKEN_UPDATED_AT_KEY, timestamp);
}

function clearTokenMeta() {
  wx.removeStorageSync(TOKEN_UPDATED_AT_KEY);
}

function isTokenExpired(updatedAt) {
  if (!updatedAt) return false;
  return Date.now() - updatedAt >= TOKEN_TTL_MS;
}

function hasLoginIntent() {
  return wx.getStorageSync(LOGIN_INTENT_KEY) === true;
}

function needsRefresh() {
  if (!hasLoginIntent()) return false;
  if (!getToken()) return true;
  const updatedAt = getTokenUpdatedAt();
  return !updatedAt || Date.now() - updatedAt >= TOKEN_TTL_MS - TOKEN_REFRESH_AHEAD_MS;
}

function getSessionVersion() {
  return sessionVersion;
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
  }).then(async () => {
    const pending = getAppSafe()?.globalData?.silentLoginPromise;
    if (pending) await pending;
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
      // Existing users must retain their login choice across this upgrade.
      wx.setStorageSync(LOGIN_INTENT_KEY, true);
      const updatedAt = getTokenUpdatedAt();
      if (isTokenExpired(updatedAt)) {
        console.log('Token 已过期，清理缓存');
        const app = getAppSafe();
        if (app && app.globalData) {
          app.globalData.userInfo = null;
        }
        clearToken({ preserveLoginIntent: true });
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
  const previous = app.globalData.userInfo;
  app.globalData.userInfo = {
    ...(previous?.token === token ? previous : {}),
    ...userInfo,
    token: token
  };
  
  // 更新本地存储
  wx.setStorageSync('userInfo', app.globalData.userInfo);
  setTokenUpdatedAt(Date.now());
  wx.setStorageSync(LOGIN_INTENT_KEY, true);
  
  markTokenReady();
}

/**
 * 清除 token
 */
function clearToken({ preserveLoginIntent = false } = {}) {
  sessionVersion++;
  const app = getAppSafe();
  if (app && app.globalData) {
    app.globalData.userInfo = null;
  }
  wx.removeStorageSync('userInfo');
  clearTokenMeta();
  if (!preserveLoginIntent) wx.removeStorageSync(LOGIN_INTENT_KEY);
  markTokenReady();
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
  getTokenUpdatedAt,
  hasLoginIntent,
  needsRefresh,
  getSessionVersion
};

