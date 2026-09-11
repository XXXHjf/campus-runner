const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const vm = require('node:vm');

const source = fs.readFileSync(path.resolve(
  __dirname, '../../apps/mini-program/pages/second-hand/index/index.js',
), 'utf8');
const tick = () => new Promise((resolve) => setImmediate(resolve));
function deferred() {
  let resolve;
  const promise = new Promise((yes) => { resolve = yes; });
  return { promise, resolve };
}

function createHarness({ token = 'session-a', getUserInfo, getSchools, listProducts } = {}) {
  const state = {
    user: { id: 7, schoolId: 101 },
    schools: [
      { id: '101', schoolName: ' 测试大学 ' },
      { id: 102, schoolName: '另一所大学' },
      { id: 103, schoolName: '  ' },
    ],
    userReads: 0,
    schoolReads: 0,
  };
  const toasts = [];
  const app = {
    globalData: { userInfo: token ? { id: 7, token } : null, silentLoginPromise: null },
    refreshMineTabRedDot: async () => {},
  };
  let page;
  vm.runInNewContext(source, {
    Page(config) {
      page = {
        ...config,
        data: structuredClone(config.data),
        setData(update) { Object.assign(this.data, update); },
      };
    },
    getApp: () => app,
    require(id) {
      if (id.endsWith('tokenManager')) {
        return { getToken: () => app.globalData.userInfo?.token || null };
      }
      if (id.endsWith('userService')) {
        return {
          getUserInfo: async () => {
            state.userReads += 1;
            return getUserInfo ? getUserInfo() : state.user;
          },
          getSchools: async () => {
            state.schoolReads += 1;
            return getSchools ? getSchools() : state.schools;
          },
        };
      }
      if (id.endsWith('secondHandService')) {
        return {
          listCategories: async () => [],
          listProducts: listProducts || (async () => [{ id: 1, title: '测试商品', images: 'one.jpg' }]),
        };
      }
      throw new Error(`Unexpected dependency: ${id}`);
    },
    wx: {
      getWindowInfo: () => ({ windowWidth: 375, statusBarHeight: 20 }),
      getMenuButtonBoundingClientRect: () => ({ top: 24, left: 281, width: 87, height: 32 }),
      showToast: (options) => toasts.push(options.title),
    },
    console: { warn() {}, error() {} },
  });
  page.onLoad();
  return { page, app, state, toasts };
}

test('anonymous and registered users without a saved school do not show a school', async () => {
  const { page, app, state } = createHarness({ token: null });
  await page.onShow();
  assert.equal(state.userReads, 0);
  assert.equal(page.data.schoolName, '');
  assert.equal(page.data.products.length, 1);

  app.globalData.userInfo = { id: 7, token: 'registered-session' };
  for (const school of [
    { schoolId: null, schoolName: null },
    { schoolId: null, schoolName: '未绑定学校' },
    { schoolId: 999 },
    { schoolId: 103 },
  ]) {
    state.user = { id: 7, profileCompleted: true, ...school };
    await page.onShow();
    assert.equal(page.data.schoolName, '');
  }
});

test('returning after school submission and pull-to-refresh update the school; logout clears it', async () => {
  const { page, app, state } = createHarness();
  state.user = { id: 7, schoolId: null, schoolName: null };
  await page.onShow();
  page.onHide();
  state.user = { id: 7, schoolId: 101, studentIdCardReview: 1 };
  await page.onShow();
  assert.equal(page.data.schoolName, '测试大学');

  state.user = { id: 7, schoolId: 102 };
  await page.onRefresh();
  assert.equal(page.data.schoolName, '另一所大学');
  assert.equal(page.data.refreshing, false);

  page.onHide();
  app.globalData.userInfo = null;
  const readsBeforeLogout = state.userReads;
  const showing = page.onShow();
  assert.equal(page.data.schoolName, '');
  await showing;
  assert.equal(state.userReads, readsBeforeLogout);
});

test('cold startup waits for silent login and then displays the school without reopening', async () => {
  const gate = deferred();
  const { page, app, state } = createHarness({ token: null });
  app.globalData.silentLoginPromise = gate.promise;
  const showing = page.onShow();
  await tick();
  assert.equal(state.userReads, 0);
  assert.equal(page.data.products.length, 1);
  app.globalData.userInfo = { id: 7, token: 'restored-session' };
  gate.resolve(true);
  await showing;
  assert.equal(page.data.schoolName, '测试大学');
  assert.equal(state.schoolReads, 1);
});

test('unsuccessful silent login leaves the header empty without fetching user details', async () => {
  const { page, app, state, toasts } = createHarness({ token: null });
  app.globalData.silentLoginPromise = Promise.resolve(false);
  await page.onShow();
  assert.equal(page.data.schoolName, '');
  assert.equal(state.userReads, 0);
  assert.deepEqual(toasts, []);
});

test('school fetch failure hides the school without blocking products or showing a toast', async () => {
  const { page, toasts } = createHarness({ getSchools: async () => {
    throw new Error('network failure');
  } });
  page.data.schoolName = '旧学校';
  await page.onShow();
  assert.equal(page.data.schoolName, '');
  assert.equal(page.data.products.length, 1);
  assert.deepEqual(toasts, []);
});

test('an older response cannot overwrite the newest school refresh', async () => {
  const gate = deferred();
  let reads = 0;
  const { page } = createHarness({ getUserInfo: () => ++reads === 1
    ? gate.promise
    : { id: 7, schoolId: 102 } });
  const initial = page.onShow();
  await tick();
  await page.onRefresh();
  gate.resolve({ id: 7, schoolId: 101 });
  await initial;
  assert.equal(page.data.schoolName, '另一所大学');
});

test('late school results are ignored after hiding or unloading the page', async () => {
  for (const lifecycle of ['onHide', 'onUnload']) {
    const gate = deferred();
    const { page } = createHarness({ getUserInfo: () => gate.promise });
    const loading = page.loadSchoolName();
    page[lifecycle]();
    gate.resolve({ id: 7, schoolId: 101 });
    await loading;
    assert.equal(page.data.schoolName, '');
  }
});

test('token renewal for the same user is accepted but logout or an account change is not', async () => {
  for (const userId of [7, 8, null]) {
    const gate = deferred();
    const { page, app } = createHarness({ getUserInfo: () => gate.promise });
    const loading = page.loadSchoolName();
    app.globalData.userInfo = userId == null ? null : { id: userId, token: 'new-session' };
    gate.resolve({ id: 7, schoolId: 101 });
    await loading;
    assert.equal(page.data.schoolName, userId === 7 ? '测试大学' : '');
  }
});

test('product failures show a user-facing message and do not prevent school loading', async () => {
  const { page, toasts } = createHarness({ listProducts: async () => {
    throw new Error('BadSqlGrammarException: internal query');
  } });
  await page.onShow();
  assert.equal(page.data.schoolName, '测试大学');
  assert.deepEqual(toasts, ['商品加载失败，请稍后重试']);
  assert.equal(page.data.loading, false);
});
