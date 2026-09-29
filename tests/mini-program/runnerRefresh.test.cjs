const feedbackStub = require('./helpers/feedbackStub.cjs');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const test = require('node:test');
const source = fs.readFileSync(path.resolve(__dirname, '../../apps/mini-program/pages/index/index.js'), 'utf8');
const commonContext = { module: { exports: {} }, require: () => ({}), getApp: () => ({ globalData: {} }) };
vm.runInNewContext(fs.readFileSync(path.resolve(__dirname, '../../apps/mini-program/utils/commonJs.js'), 'utf8'), commonContext);
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
      if (id.endsWith('feedback')) return feedbackStub({}, message => state.errors.push(message));
      if (id.endsWith('commonJs')) return commonContext.module.exports;
      if (id.endsWith('tokenManager')) return {
        getToken: () => state.token, hasToken: () => !!state.token, waitForToken: async () => {},
      };
      if (id.endsWith('userService')) return {
        getUserInfo: async () => ({ id: 1, schoolId: 1, authentication: 1 }),
        getSchools: async () => [{ id: 1, schoolName: '测试大学' }],
      };
      if (id.endsWith('orderService')) return { getPublicOrders: async () => [{ id: 9, categoryId: 2, categoryName: '取件', price: 3 }] };
      if (id.endsWith('transformers')) return {
        showLoading() { state.loading++; }, hideLoading() {}, showError(_, message) { state.errors.push(message); },
      };
      if (id.endsWith('constants')) return { SWIPER_CONFIG: {}, ERROR_MESSAGES: { GET_ORDERS_FAILED: '获取失败' } };
      return {};
    },
    Page(config) { page = { ...config, data: structuredClone(config.data), setData(value, callback) {
      Object.assign(this.data, value);
      if (callback) callback();
    } }; },
  });
  page.checkPrivacyAcknowledged = () => {};
  page.loadBanners = async () => {};
  return { page, state };
}
test('首页送达时间区分今天和次日，无偿兼容零金额与旧空金额', () => {
  const { page } = harness();
  const now = new Date();
  const timestamp = (offset) => {
    const date = new Date(now.getFullYear(), now.getMonth(), now.getDate() + offset);
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} 09:00:00`;
  };
  const items = page._decorateTakes([
    { expectedDeliveryTime: timestamp(0), price: '0.00' },
    { expectedDeliveryTime: timestamp(1), price: null },
    { expectedDeliveryTime: timestamp(0), price: 3 },
    { expectedDeliveryTime: timestamp(0), price: 0, businessType: 'PURCHASE', productAmount: 20 },
    { createTime: timestamp(1).replace('09:00:00', '08:30:30'), gap: 29, price: 0 },
  ]);
  assert.equal(items[0].displayDeliveryTime, '9:00');
  assert.equal(items[1].displayDeliveryTime, '次日9:00');
  assert.equal(items[4].displayDeliveryTime, '次日9:00');
  assert.deepEqual(Array.from(items, item => item.isFree), [true, true, false, false, true]);
});
test('首页地址只展示楼宇和具体位置，保留完整地址供搜索与详情使用', () => {
  const { page } = harness();
  const order = {
    pickUpAddress: '浙大城市学院 南校区 寝室楼 弘毅楼 寝室楼下 靠东门',
    reciveAddress: '浙大城市学院 北校区 教学楼 理工楼 302室',
  };
  const [item] = page._decorateTakes([order]);
  assert.equal(item.displayPickUpAddress, '弘毅楼 寝室楼下 靠东门');
  assert.equal(item.displayReciveAddress, '理工楼 302室');
  assert.equal(item.pickUpAddress, order.pickUpAddress);
  assert.equal(item.reciveAddress, order.reciveAddress);
  assert.equal(page._formatCardAddress('浙大城市学院 南校区 寝室楼 弘毅楼'), '弘毅楼');
  assert.equal(page._formatCardAddress('弘毅楼 楼下'), '弘毅楼 楼下');
  assert.equal(page._formatCardAddress(null), '');
});
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
  assert.equal(page.data.schoolName, '');
});

test('搜索与分类一起筛选订单，学校名称随登录状态更新', async () => {
  const { page, state } = harness();
  page.data.userInfo = { id: 1, schoolId: 1, authentication: 1, token: 'a' };
  page.data.category = [{ id: 2, categoryName: '外卖' }];
  page._loadFilteredTakes = async () => [
    { id: 1, categoryId: 2, categoryName: '外卖', note: '送到图书馆' },
    { id: 2, categoryId: 2, categoryName: '外卖', note: '送到宿舍' },
  ];
  await page.refreshRunner();
  assert.equal(page.data.schoolName, '测试大学');
  page.selectCategory({ currentTarget: { dataset: { id: 2 } } });
  page.onSearchChange({ detail: { value: '图书馆' } });
  page.onSearch();
  await tick();
  assert.deepEqual(Array.from(page.data.takes, item => item.id), [1]);
  assert.equal(page.data.filterSummary, '外卖');
  state.token = null;
  await page.refreshRunner();
  assert.equal(page.data.schoolName, '');
});
