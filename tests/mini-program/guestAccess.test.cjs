const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const test = require('node:test');
const root = path.resolve(__dirname, '../../apps/mini-program');

function load(file, overrides) {
  const module = { exports: {} };
  vm.runInNewContext(fs.readFileSync(path.join(root, file), 'utf8'), { module, console, ...overrides });
  return module.exports;
}

function guardHarness({ token = null, confirm = false, user = {}, profileError = false } = {}) {
  const state = { token, logins: 0, routes: [], modals: [] };
  const guard = load('utils/accessGuard.js', {
    require(id) {
      if (id.includes('tokenManager')) return { hasToken: () => !!state.token };
      if (id.includes('profileStatus')) return { PROFILE_PAGE: '/profile', CAMPUS_AUTH_PAGE: '/auth', isProfileComplete: (u) => !!u.profileCompleted };
      if (id.includes('userService')) return { getUserInfo: async () => {
        if (profileError) throw new Error('network');
        return user;
      } };
    },
    getApp: () => ({ silentLogin: async () => { state.logins++; state.token = 'new'; return true; } }),
    wx: {
      showModal(options) { state.modals.push(options); options.success({ confirm }); },
      navigateTo({ url }) { state.routes.push(url); },
      navigateBack({ fail }) { fail(); },
      switchTab({ url }) { state.routes.push(url); },
      showToast() {},
    },
  });
  return { guard, state };
}

test('取消登录不调用微信登录、不跳转、不继续身份操作', async () => {
  const { guard, state } = guardHarness();
  assert.equal(await guard.ensureAuthenticated(), false);
  assert.equal(state.logins, 0);
  assert.equal(state.routes.length, 0);
});

test('确认登录后才能进入已认证操作，重复点击合并登录弹窗', async () => {
  const { guard, state } = guardHarness({ confirm: true, user: { authentication: 1, profileCompleted: true } });
  assert.deepEqual(await Promise.all([guard.ensureLogin(), guard.ensureLogin()]), [true, true]);
  assert.equal(state.logins, 1);
  assert.equal(state.modals.length, 1);
  assert.equal(await guard.ensureAuthenticated(), true);
});

test('未认证、审核中、驳回分别指引，取消后无跳转', async () => {
  for (const [review, title] of [[0, '需要校园认证'], [1, '校园认证审核中'], [3, '校园认证未通过']]) {
    const { guard, state } = guardHarness({ token: 'a', user: { authentication: 0, studentIdCardReview: review, profileCompleted: true } });
    assert.equal(await guard.ensureAuthenticated(), false);
    assert.equal(state.modals[0].title, title);
    assert.equal(state.routes.length, 0);
  }
});

test('去认证按资料完整度分流，单页打开时返回浏览首页', async () => {
  for (const [profileCompleted, route] of [[false, '/profile?after=login'], [true, '/auth']]) {
    const { guard, state } = guardHarness({ token: 'a', confirm: true, user: { profileCompleted } });
    await guard.ensureAuthenticated();
    assert.equal(state.routes[0], route);
    guard.returnToBrowse();
    assert.equal(state.routes[1], '/pages/second-hand/index/index');
  }
});

test('新登录和已有凭证的缺资料账号均进入填写页，不继续原操作', async () => {
  for (const token of [null, 'existing']) {
    const user = { profileCompleted: false, authentication: 1 };
    const { guard, state } = guardHarness({ token, confirm: true, user });
    assert.deepEqual(await Promise.all([guard.ensureLogin(), guard.ensureLogin()]), [false, false]);
    assert.equal(state.routes.length, 1);
    assert.equal(state.routes[0], '/profile?after=login');
    assert.equal(state.logins, token ? 0 : 1);
    // 取消填写后，再次主动操作仍不可绕过；补齐资料后才允许继续。
    assert.equal(await guard.ensureAuthenticated(), false);
    user.profileCompleted = true;
    assert.equal(await guard.ensureAuthenticated(), true);
  }
});

test('资料加载失败不放行也不误跳注册页', async () => {
  const { guard, state } = guardHarness({ token: 'existing', profileError: true });
  assert.equal(await guard.ensureAuthenticated(), false);
  assert.equal(state.routes.length, 0);
});

test('我的主动登录按资料完整度分流', async () => {
  for (const profileCompleted of [false, true]) {
    let page;
    const routes = [];
    const toasts = [];
    const app = { globalData: {}, refreshMineTabRedDot: () => Promise.resolve() };
    const source = fs.readFileSync(path.join(root, 'pages/mine/mine/mine.js'), 'utf8')
      .replace(/^import .+;.*$/gm, '');
    vm.runInNewContext(source, {
      console, getApp: () => app,
      require(id) {
        if (id.endsWith('userService')) return { getUserInfo: async () => ({ profileCompleted }) };
        if (id.endsWith('tokenManager')) return { updateToken() {} };
        if (id.endsWith('privacy')) return { maskPhone: () => '' };
        if (id.endsWith('profileStatus')) return require(path.join(root, 'utils/profileStatus'));
        return {};
      },
      wx: {
        login: ({ success }) => success({ code: 'test' }),
        request: ({ success }) => success({ statusCode: 200, data: { code: 1, data: { token: 'test' } } }),
        setStorage() {}, navigateTo: ({ url }) => routes.push(url),
        showToast: ({ title }) => toasts.push(title),
      },
      Page(config) { page = { ...config, data: structuredClone(config.data), setData(data) { Object.assign(this.data, data); } }; },
    });
    await page._login();
    assert.deepEqual(routes, profileCompleted ? [] : ['/pages/mine/newUser/index?after=login']);
    assert.deepEqual(toasts, profileCompleted ? ['登录成功'] : []);
    assert.equal(page.data.loginLoadShow, false);
  }
});

test('注册保存经确认后，登录入口返回来源页、认证入口继续认证', async () => {
  for (const after of ['login', undefined]) {
    let page;
    const routes = [];
    load('pages/mine/newUser/index.js', {
      require(id) {
        if (id.endsWith('profileStatus')) return require(path.join(root, 'utils/profileStatus'));
        if (id.endsWith('userService')) return { getUserInfo: async () => ({ profileCompleted: true }) };
        if (id.endsWith('tokenManager')) return { waitForToken: async () => {}, getToken: () => 'test' };
        if (id.endsWith('transformers')) return { showLoading() {}, hideLoading() {} };
        if (id.endsWith('commonJs')) return { showSuccessToast() {}, errorCilcleToast() { assert.fail('保存不应失败'); } };
        if (id.endsWith('accessGuard')) return { returnToBrowse: () => routes.push('back') };
        return {};
      },
      getApp: () => ({ onUserInfoUpdated() {} }),
      setTimeout: (callback) => callback(),
      wx: { redirectTo: ({ url }) => routes.push(url) },
      Page(config) { page = { ...config, data: structuredClone(config.data), setData(data) { Object.assign(this.data, data); } }; },
    });
    await page.onLoad({ after });
    page.validateInput = () => true;
    page.updateUser = async () => {};
    await page.confirmRegister();
    assert.deepEqual(routes, after === 'login' ? ['back'] : ['/pages/mine/identify/identify']);
  }
});

test('游客公开读取无登录副作用，私有读取与写入不放行', async () => {
  let calls = 0;
  const { request, isPublicRead } = load('services/request.js', {
    require: () => ({ getToken: () => null }),
    wx: { request(options) { calls++; options.success({ statusCode: 200, data: { code: 1, data: [] } }); } },
  });
  for (const route of ['/api/second-hand/categories', '/api/second-hand/products', '/api/second-hand/products/12', '/api/order/public', '/api/address/three']) {
    await request({ url: `https://example.test${route}` });
  }
  assert.equal(calls, 5);
  for (const route of ['/api/second-hand/products/my', '/api/user', '/api/second-hand/products/12/messages']) {
    await assert.rejects(request({ url: `https://example.test${route}` }), /请先登录/);
  }
  assert.equal(isPublicRead({ url: 'https://example.test/api/second-hand/products', method: 'POST' }), false);
  assert.equal(calls, 5);
});

test('401清理过期登录，不刷新令牌或重放订单', async () => {
  let calls = 0;
  let cleared = 0;
  const { request } = load('services/request.js', {
    require: () => ({ getToken: () => 'old', clearToken() { cleared++; } }),
    wx: { request(options) { calls++; options.success({ statusCode: 401 }); } },
  });
  await assert.rejects(request({ url: 'https://example.test/api/second-hand/orders', method: 'POST' }), /登录已失效/);
  assert.equal(calls, 1);
  assert.equal(cleared, 1);
});

test('首次启动和无缓存启动只恢复缓存，不自动微信登录', () => {
  let app;
  let initialized = 0;
  load('app.js', {
    require: () => ({ initTokenSync() { initialized++; } }),
    App(config) { app = config; },
    wx: { login() { throw new Error('不应自动登录'); } },
  });
  app.onLaunch();
  assert.equal(initialized, 1);
});

test('未完善资料用户返回我的页，不再次跳转注册或加载认证待办', async () => {
  let page;
  let jumps = 0;
  const source = fs.readFileSync(path.join(root, 'pages/mine/mine/mine.js'), 'utf8')
    .replace(/^import .+;.*$/gm, '');
  vm.runInNewContext(source, {
    console,
    getApp: () => ({ globalData: {} }),
    require(id) {
      if (id.endsWith('tokenManager')) return { hasToken: () => true };
      if (id.endsWith('mineTabBadgeService')) return { clearMineTabRedDot() {} };
      return {};
    },
    wx: { navigateTo() { jumps++; } },
    Page(config) { page = { ...config, data: structuredClone(config.data), setData(data) { Object.assign(this.data, data); } }; },
  });
  page._waitForStartupLogin = async () => {};
  page.getGlobalData = async () => ({ authentication: 0, profileCompleted: false });
  page._getSecondHandTaskCounts = () => { throw new Error('不应加载需认证的待办'); };
  await page.onShow();
  await page.onShow();
  assert.equal(jumps, 0);
});

test('后台读取被拒绝时只返回中文错误，不弹认证引导', async () => {
  let imports = 0;
  const { request } = load('services/request.js', {
    require() { imports++; return { getToken: () => 'a' }; },
    wx: { request(options) { options.success({ statusCode: 200, data: { code: 0, msg: 'User not authenticated' } }); } },
  });
  await assert.rejects(request({ url: 'https://example.test/api/second-hand/orders/buyer' }), /完成校园认证/);
  assert.equal(imports, 1);
});
