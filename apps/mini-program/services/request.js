const tokenManager = require('../utils/tokenManager');

function safeList(res) {
  if (res?.data?.code !== 1) {
    throw new Error(res.data.msg || '请求失败，请稍后重试');
  }
  if (!Array.isArray(res.data.data)) throw new Error('加载失败，请稍后重试');
  return res.data.data;
}

function requiredObject(res) {
  if (res?.data?.code !== 1) throw new Error(res?.data?.msg || '加载失败，请稍后重试');
  const value = res.data.data;
  if (!value || typeof value !== 'object' || Array.isArray(value) || !Object.keys(value).length) {
    throw new Error('加载失败，请稍后重试');
  }
  return value;
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
  const initialSessionVersion = tokenManager.getSessionVersion?.();
  const publicRead = isPublicRead(options);
  const app = typeof getApp === 'function' ? getApp() : null;
  if (!options.skipTokenCheck && !publicRead && tokenManager.hasLoginIntent?.()) {
    await app?.restoreLogin?.();
  }
  if (initialSessionVersion !== tokenManager.getSessionVersion?.()) {
    throw new Error('登录状态已变化，请重试操作');
  }
  const token = options.skipTokenCheck ? null : tokenManager.getToken();
  // Background reads never create accounts or open a login prompt.
  if (!token && !publicRead && !options.skipTokenCheck) {
    throw new Error('请先登录后再操作');
  }
  return send(options, token, app, false);
}

function send(options, token, app, retried) {
  const sessionVersion = tokenManager.getSessionVersion?.();
  return new Promise((resolve, reject) => wx.request({
    ...options,
    method: options.method || 'GET',
    header: { 'Content-Type': 'application/json', ...(token ? { token } : {}), ...options.header },
    timeout: options.timeout || 10000,
    success: (res) => {
      if (token && sessionVersion !== tokenManager.getSessionVersion?.()) {
        reject(new Error('登录状态已变化，请重试操作'));
        return;
      }
      if (res.statusCode === 401) {
        // Refresh identity once. Only reads may be retried automatically.
        if (!token || retried || !tokenManager.hasLoginIntent?.()) {
          if (token && token === tokenManager.getToken()) tokenManager.clearToken?.({ preserveLoginIntent: true });
          reject(new Error('登录已失效，请重新登录'));
          return;
        }
        Promise.resolve(app?.restoreLogin?.({ force: true }))
          .then((restored) => {
            if (sessionVersion !== tokenManager.getSessionVersion?.()) {
              reject(new Error('登录状态已变化，请重试操作'));
              return;
            }
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
        const error = new Error(res.statusCode === 429 ? '操作较频繁，请稍后再试' : '请求失败，请稍后重试');
        error.statusCode = res.statusCode;
        // A rejected transport response does not confirm the result of an older
        // write attempt. Preserve its identifier and never retry writes here.
        reject(error);
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
      if (res.data?.code !== 1) {
        const error = new Error(res.data?.msg || '操作失败，请稍后重试');
        // Transport errors and malformed responses can hide a committed write.
        if (res.data?.code === 0) error.requestOutcome = 'rejected';
        reject(error);
        return;
      }
      resolve(res);
    },
    fail: () => reject(new Error('网络连接失败，请稍后重试')),
  }));
}
async function collectListPages(fetchPage) {
  const token = tokenManager.getToken?.();
  const version = tokenManager.getSessionVersion?.();
  const result = [];
  const seenPages = new Set();
  for (let page = 1; ; page++) {
    const rows = await fetchPage({ page, pageSize: 50 });
    if (token !== tokenManager.getToken?.() || version !== tokenManager.getSessionVersion?.()) throw new Error('登录状态已变化，请重试');
    if (!Array.isArray(rows)) throw new Error('加载失败，请稍后重试');
    const signature = JSON.stringify(rows.map(row => row.id ?? row.numberId));
    if (rows.length > 50 || (rows.length && seenPages.has(signature))) throw new Error('加载失败，请稍后重试');
    seenPages.add(signature);
    result.push(...rows);
    if (rows.length < 50) return result;
  }
}
module.exports = { request, safeList, requiredObject, isPublicRead, collectListPages };
