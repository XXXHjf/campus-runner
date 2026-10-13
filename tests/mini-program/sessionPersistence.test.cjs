const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const test = require('node:test');

const root = path.resolve(__dirname, '../../apps/mini-program');

function load(file, globals) {
  const module = { exports: {} };
  vm.runInNewContext(fs.readFileSync(path.join(root, file), 'utf8'), {
    module, console, Promise, Date, ...globals,
  });
  return module.exports;
}

function harness() {
  const cache = new Map();
  let app;
  let loginCalls = 0;
  let apiCalls = 0;
  let finishLogin;
  const wx = {
    getStorageSync: (key) => cache.get(key),
    setStorageSync: (key, data) => cache.set(key, data),
    removeStorageSync: (key) => cache.delete(key),
    login({ success }) { loginCalls++; success({ code: 'fresh-code' }); },
    request({ url, success }) {
      if (url.endsWith('/api/user/login')) {
        apiCalls++;
        finishLogin = () => success({ statusCode: 200, data: { code: 1, data: { token: 'renewed' } } });
      }
    },
  };
  const getApp = () => app;
  const tokenManager = load('utils/tokenManager.js', { wx, getApp });
  load('app.js', {
    wx,
    require: () => tokenManager,
    App(config) { app = config; },
  });
  return { app, cache, tokenManager, get loginCalls() { return loginCalls; },
    get apiCalls() { return apiCalls; }, finishLogin: () => finishLogin() };
}

test('旧登录缓存迁移后重编译可恢复，过期时保留自动恢复意图', () => {
  const h = harness();
  h.cache.set('userInfo', { token: 'old' });
  h.cache.set('tokenUpdatedAt', Date.now() - 21 * 60 * 60 * 1000);
  h.tokenManager.initTokenSync();
  assert.equal(h.tokenManager.hasToken(), false);
  assert.equal(h.tokenManager.hasLoginIntent(), true);
  const pending = h.app.restoreLogin();
  assert.equal(h.loginCalls, 1);
  h.finishLogin();
  return pending.then(() => {
    assert.equal(h.tokenManager.getToken(), 'renewed');
    assert.equal(h.cache.get('userInfo').token, 'renewed');
  });
});

test('同一批恢复只换一次凭证，退出后迟到结果不能重新登录', async () => {
  const h = harness();
  h.cache.set('hasLoggedIn', true);
  h.tokenManager.initTokenSync();
  const first = h.app.restoreLogin();
  const second = h.app.restoreLogin();
  assert.equal(first, second);
  assert.equal(h.apiCalls, 1);
  h.tokenManager.clearToken();
  h.finishLogin();
  assert.equal(await first, false);
  assert.equal(h.tokenManager.hasToken(), false);
  assert.equal(h.tokenManager.hasLoginIntent(), false);
});

test('新账号凭证不继承旧账号资料', () => {
  const h = harness();
  h.tokenManager.updateToken('first', { id: 1, schoolId: 8 });
  h.tokenManager.updateToken('second', { id: 2 });
  assert.equal(h.cache.get('userInfo').id, 2);
  assert.equal(h.cache.get('userInfo').schoolId, undefined);
});

test('401 续签后只重试读取，不重发写操作', async () => {
  for (const method of ['GET', 'POST']) {
    let current = 'old';
    let calls = 0;
    let renewals = 0;
    const request = load('services/request.js', {
      require: () => ({ getToken: () => current, hasLoginIntent: () => true }),
      getApp: () => ({ restoreLogin: async ({ force } = {}) => {
        if (force) { renewals++; current = 'new'; }
        return true;
      } }),
      wx: { request(options) { calls++; options.success({ statusCode: options.header.token === 'old' ? 401 : 200, data: { code: 1 } }); } },
    }).request;
    const operation = request({ url: 'https://example.test/api/user', method });
    if (method === 'GET') await operation;
    else await assert.rejects(operation, /请重试操作/);
    assert.equal(renewals, 1);
    assert.equal(calls, method === 'GET' ? 2 : 1);
  }
});

test('换号后的旧响应被拒绝，迟到 401 不清除新账号登录', async () => {
  for (const statusCode of [200, 401]) {
    let version = 1, token = 'A', finish, cleared = false;
    const request = load('services/request.js', {
      require: () => ({ getToken: () => token, getSessionVersion: () => version,
        hasLoginIntent: () => false, clearToken: () => { cleared = true; } }),
      getApp: () => ({ globalData: {} }),
      wx: { request(options) { finish = () => options.success({ statusCode, data: { code: 1, data: { phone: 'A_PRIVATE' } } }); } },
    }).request;
    const pending = request({ url: 'https://example.test/api/user' });
    version = 2; token = 'B'; finish();
    await assert.rejects(pending, /登录状态已变化/);
    assert.equal(cleared, false);
  }
});

test('等待恢复登录时换号不能把原账号写入请求发送为新账号', async () => {
  let version = 1, resume, calls = 0;
  const request = load('services/request.js', {
    require: () => ({ getToken: () => 'B', getSessionVersion: () => version, hasLoginIntent: () => true }),
    getApp: () => ({ restoreLogin: () => new Promise(resolve => { resume = resolve; }) }),
    wx: { request() { calls++; } },
  }).request;
  const pending = request({ url: 'https://example.test/api/order', method: 'POST', data: { phone: 'A_PRIVATE' } });
  version = 2; resume(true);
  await assert.rejects(pending, /登录状态已变化/); assert.equal(calls, 0);
});

test('401 续签期间换号不得用新账号重试原读取', async () => {
  let version = 1, resume, calls = 0;
  const request = load('services/request.js', {
    require: () => ({ getToken: () => 'login', getSessionVersion: () => version, hasLoginIntent: () => true }),
    getApp: () => ({ restoreLogin: ({ force } = {}) => force ? new Promise(resolve => { resume = resolve; }) : Promise.resolve(true) }),
    wx: { request(options) { calls++; options.success({ statusCode: 401 }); } },
  }).request;
  const pending = request({ url: 'https://example.test/api/user' });
  await new Promise(setImmediate); version = 2; resume(true);
  await assert.rejects(pending, /登录状态已变化/); assert.equal(calls, 1);
});
