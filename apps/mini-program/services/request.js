const tokenManager = require('../utils/tokenManager');

function safeList(res) {
  if (res && res.data && res.data.code !== undefined && res.data.code !== 1) {
    throw new Error(res.data.msg || '请求失败，请稍后重试');
  }
  return Array.isArray(res?.data?.data) ? res.data.data : [];
}

function isPublicRead(options) {
  if ((options.method || 'GET').toUpperCase() !== 'GET') return false;
  const path = options.url.replace(/^https?:\/\/[^/]+/, '').split('?')[0];
  return /^\/api\/second-hand\/(categories|products(?:\/[0-9]+)?)$/.test(path)
    || /^\/api\/(school|category)(\/[0-9]+)?$/.test(path)
    || path === '/api/address/three' || path === '/api/order/public'
    || /^\/admin\/api\/banner\/getList\/[^/]+$/.test(path);
}

async function request(options) {
  const publicRead = isPublicRead(options);
  const app = typeof getApp === 'function' ? getApp() : null;
  if (!options.skipTokenCheck && !publicRead && tokenManager.hasLoginIntent?.()) {
    await app?.restoreLogin?.();
  }
  const token = options.skipTokenCheck ? null : tokenManager.getToken();
  // Background reads never create accounts or open a login prompt.
  if (!token && !publicRead && !options.skipTokenCheck) {
    throw new Error('请先登录后再操作');
  }
  return send(options, token, app, false);
}

function send(options, token, app, retried) {
  return new Promise((resolve, reject) => wx.request({
    ...options,
    method: options.method || 'GET',
    header: { 'Content-Type': 'application/json', ...(token ? { token } : {}), ...options.header },
    timeout: options.timeout || 10000,
    success: (res) => {
      if (res.statusCode === 401) {
        // Refresh identity once. Only reads may be retried automatically.
        if (!token || retried || !tokenManager.hasLoginIntent?.()) {
          if (token && token === tokenManager.getToken()) tokenManager.clearToken?.({ preserveLoginIntent: true });
          reject(new Error('登录已失效，请重新登录'));
          return;
        }
        Promise.resolve(app?.restoreLogin?.({ force: true }))
          .then((restored) => {
            const freshToken = tokenManager.getToken();
            if (!restored || !freshToken) {
              if (token === tokenManager.getToken()) tokenManager.clearToken?.({ preserveLoginIntent: true });
              reject(new Error('登录已失效，请重新登录'));
            } else if ((options.method || 'GET').toUpperCase() === 'GET') {
              send(options, freshToken, app, true).then(resolve, reject);
            } else {
              reject(new Error('登录状态已恢复，请重试操作'));
            }
          })
          .catch(() => reject(new Error('登录已失效，请重新登录')));
        return;
      }
      if (res.statusCode !== 200) {
        reject(new Error('请求失败，请稍后重试'));
        return;
      }
      const msg = String(res.data?.msg || '');
      if (res.data?.code !== 1 && (/User not authentic/i.test(msg) || msg.includes('完成校园认证后才能操作'))) {
        // A background badge/list refresh must never reopen onboarding.
        if ((options.method || 'GET').toUpperCase() !== 'GET') {
          const guard = require('../utils/accessGuard');
          require('./userService').getUserInfo()
            .then((user) => guard.guideAuthentication(user))
            .catch(() => guard.guideAuthentication());
        }
        reject(new Error('完成校园认证后才能操作'));
        return;
      }
      resolve(res);
    },
    fail: () => reject(new Error('网络连接失败，请稍后重试')),
  }));
}
module.exports = { request, safeList, isPublicRead };
