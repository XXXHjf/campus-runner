const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const test = require('node:test');
const source = fs.readFileSync(path.resolve(__dirname, '../../apps/mini-program/pages/index/index.js'), 'utf8');
const tick = () => new Promise(resolve => setImmediate(resolve));
function deferred() {
  let resolve;
  const promise = new Promise(done => { resolve = done; });
  return { promise, resolve };
}
function harness() {
  const state = { token: 'a', loading: 0, stopped: 0, errors: [] };
  let page;
  vm.runInNewContext(source, {
    getApp: () => ({ globalData: {}, refreshMineTabRedDot: async () => {} }),
    console: { log() {}, error() {} },
    wx: { stopPullDownRefresh() { state.stopped++; } },
    require(id) {
      if (id.endsWith('tokenManager')) return {
        getToken: () => state.token, hasToken: () => !!state.token, waitForToken: async () => {},
      };
      if (id.endsWith('userService')) return { getUserInfo: async () => ({ id: 1, schoolId: 1, authentication: 1 }) };
      if (id.endsWith('orderService')) return { getPublicOrders: async () => [{ id: 9, categoryId: 2, categoryName: '取件', price: 3 }] };
      if (id.endsWith('transformers')) return {
        showLoading() { state.loading++; }, hideLoading() {}, showError(message) { state.errors.push(message); },
      };
      if (id.endsWith('constants')) return { SWIPER_CONFIG: {}, ERROR_MESSAGES: { GET_ORDERS_FAILED: '获取失败' } };
      return {};
    },
    Page(config) { page = { ...config, data: structuredClone(config.data), setData(value) { Object.assign(this.data, value); } }; },
  });
  page.checkPrivacyAcknowledged = () => {};
  page.loadBanners = async () => {};
  return { page, state };
}
test('切页保留已有订单，合并刷新且下拉等待数据，不显示全局加载', async () => {
  const { page, state } = harness();
  page.data.userInfo = { id: 1, schoolId: 1, authentication: 1, token: 'a' };
  page.data.takes = [{ id: 'cached' }];
  const pending = deferred();
  let reads = 0;
  page._loadFilteredTakes = () => { reads++; return pending.promise; };
  const shown = page.onShow();
  const pulled = page.onPullDownRefresh();
  await tick();
  assert.equal(reads, 1);
  assert.equal(page.data.isLoginChecking, false);
  assert.equal(page.data.takes[0].id, 'cached');
  assert.equal(state.stopped, 0);
  pending.resolve([{ id: 'fresh' }]);
  await Promise.all([shown, pulled]);
  assert.equal(page.data.takes[0].id, 'fresh');
  assert.equal(state.stopped, 1);
  assert.equal(state.loading, 0);
});
test('不同筛选的迟到结果不能覆盖新结果，离开页面后不更新', async () => {
  const { page } = harness();
  page.data.userInfo = { authentication: 1 };
  const first = deferred();
  const second = deferred();
  page._loadFilteredTakes = () => first.promise;
  const old = page.applyFilters();
  assert.equal(page.applyFilters(), old);
  page.data.selectedCategoryId = 2;
  page._loadFilteredTakes = () => second.promise;
  const latest = page.applyFilters();
  second.resolve([{ id: 2 }]);
  await latest;
  first.resolve([{ id: 1 }]);
  await old;
  assert.equal(page.data.takes[0].id, 2);
  const hidden = deferred();
  page._loadFilteredTakes = () => hidden.promise;
  const request = page.applyFilters();
  page.onHide();
  hidden.resolve([{ id: 3 }]);
  await request;
  assert.equal(page.data.takes[0].id, 2);
});
test('首次加载与失败均结束加载状态，退出登录清空旧订单', async () => {
  const { page, state } = harness();
  const pending = deferred();
  page._loadFilteredTakes = () => pending.promise;
  const refresh = page.refreshRunner();
  assert.equal(page.data.isLoginChecking, true);
  await tick();
  assert.equal(page.data.ordersLoading, true);
  pending.resolve([{ id: 1 }]);
  await refresh;
  assert.equal(page.data.isLoginChecking, false);
  page._loadFilteredTakes = async () => { throw new Error('network'); };
  await page.onPullDownRefresh();
  assert.equal(state.errors.length, 1);
  assert.equal(state.stopped, 1);
  assert.equal(page.data.ordersLoading, false);
  state.token = null;
  await page.refreshRunner();
  assert.equal(page.data.takes.length, 1);
  assert.equal(page.data.takes[0].id, 9);
  assert.equal(page.data.userInfo.token, undefined);
});
