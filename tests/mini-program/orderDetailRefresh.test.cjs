const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const test = require('node:test');
const feedbackStub = require('./helpers/feedbackStub.cjs');
const detailRefresh = require('../../apps/mini-program/utils/detailRefresh');
const statusTools = require('../../apps/mini-program/utils/secondHandStatus');

const tick = () => new Promise(resolve => setImmediate(resolve));
function deferred() {
  let resolve;
  const promise = new Promise(done => { resolve = done; });
  return { promise, resolve };
}

const files = {
  publisher: 'orders/myOrders/ordersInfo/info.js',
  taker: 'orders/takeOrders/takesInfo/info.js',
  secondHand: 'second-hand/order-detail/order-detail.js',
};
function harness(kind) {
  let page;
  const state = { reads: 0, stopped: 0, timers: new Set(), errors: [] };
  let read = async () => ({ id: 42, status: 1, tradeMode: 'OFFLINE', buyerId: 1 });
  const getOrder = () => { state.reads++; return read(); };
  const wx = { showShareMenu() {}, hideShareMenu() {},
    stopPullDownRefresh() { state.stopped++; } };
  vm.runInNewContext(fs.readFileSync(path.resolve(__dirname,
    '../../apps/mini-program/pages', files[kind]), 'utf8'), {
    console: { log() {}, warn() {}, error() {} }, wx,
    getApp: () => ({ globalData: { API_URL: 'https://example.test', userInfo: { id: 1 } } }),
    setInterval(fn) { state.timers.add(fn); return fn; },
    clearInterval(fn) { state.timers.delete(fn); },
    require(name) {
      if (name.endsWith('detailRefresh')) return detailRefresh;
      if (name.endsWith('feedback')) return feedbackStub(wx, message => state.errors.push(message));
      if (name.endsWith('secondHandStatus')) return statusTools;
      if (name.endsWith('userOrderService')) return { getMyOrderDetail: getOrder };
      if (name.endsWith('secondHandService')) return { getOrderDetail: getOrder };
      if (name.endsWith('userService')) return { getUserInfo: async () => ({ id: 1 }) };
      if (name.endsWith('takeOrderService')) return {
        getMyTakeOrders: async () => [{ orderId: 42 }],
        getTakeOrderDetail: async () => ({ id: 7 }),
        getDeliveryImage: async () => 'saved.jpg',
      };
      if (name.endsWith('tokenManager')) return { waitForToken: async () => {}, getToken: () => 'token' };
      if (name.endsWith('commonJs')) return {
        _getExpectTimeDisplay: () => '', _getExpectedDeliveryDate: () => new Date(), _logErrInfo() {},
        _parseStrDateTime: value => new Date(value),
      };
      return {};
    },
    Page(config) { page = { ...config, data: structuredClone(config.data),
      setData(value) { Object.assign(this.data, value); } }; },
  });
  const load = fresh => kind === 'secondHand' ? page.loadDetail(fresh) : page._loadOrderInfo(fresh);
  const order = () => kind === 'secondHand' ? page.data.order : page.data.orderInfo;
  return { page, state, load, order, setRead(fn) { read = fn; } };
}

for (const kind of Object.keys(files)) {
  test(`${kind}: 首次进入只加载一次，返回页面/前台后更新进度及按钮`, async () => {
    const h = harness(kind);
    const initial = h.page.onLoad({ id: 42 });
    h.page.onShow();
    await initial;
    assert.equal(h.state.reads, 1);
    h.page.onHide();
    h.setRead(async () => ({ id: 42, status: 3, tradeMode: 'OFFLINE', buyerId: 1 }));
    await h.page.onShow();
    assert.equal(h.state.reads, 2);
    assert.equal(h.order().status, 3);
    if (kind === 'publisher') assert.equal(h.page.data.buttonText, '确认收货');
    if (kind === 'taker') {
      assert.equal(h.page.data.progressStep, 3);
      assert.equal(h.page.data.isMyTaken, true);
    }
    if (kind === 'secondHand') {
      assert.equal(h.page.data.progressComplete, true);
      assert.equal(h.page.data.canCancel, false);
      assert.equal(h.page.data.canConfirm, false);
    }
    h.page.onUnload();
  });

  test(`${kind}: 返回刷新与下拉合并，等待时保留内容，失败后仍可重试`, async () => {
    const h = harness(kind);
    await h.page.onLoad({ id: 42 });
    h.page.onShow();
    const before = JSON.stringify(h.order());
    const pending = deferred();
    h.setRead(() => pending.promise);
    h.page.onHide();
    const shown = h.page.onShow();
    const pulled = h.page.onPullDownRefresh();
    await tick();
    assert.equal(h.state.reads, 2);
    assert.equal(h.state.stopped, 0);
    assert.equal(JSON.stringify(h.order()), before);
    assert.notEqual(h.page.data.loading, true);
    pending.resolve({ id: 42, status: 2, tradeMode: 'OFFLINE', buyerId: 1 });
    await Promise.all([shown, pulled]);
    assert.equal(h.state.stopped, 1);
    h.setRead(async () => { throw new Error('network'); });
    await h.load(false);
    assert.equal(h.order().status, 2);
    assert.equal(h.page.data.feedbackLoadError, '');
    h.page.onUnload();
  });

  test(`${kind}: 隐藏前请求迟到不回写，返回补一次新请求，卸载后不回写`, async () => {
    const h = harness(kind);
    await h.page.onLoad({ id: 42 });
    h.page.onShow();
    const old = deferred();
    const fresh = deferred();
    h.setRead(() => old.promise);
    const loading = h.load(false);
    await tick();
    h.page.onHide();
    h.setRead(() => fresh.promise);
    const shown = h.page.onShow();
    old.resolve({ id: 42, status: 0 });
    await tick();
    assert.equal(h.order().status, 1);
    assert.equal(h.state.reads, 3);
    fresh.resolve({ id: 42, status: 2, tradeMode: 'OFFLINE', buyerId: 1 });
    await Promise.all([loading, shown]);
    assert.equal(h.order().status, 2);
    const late = deferred();
    h.setRead(() => late.promise);
    const unloading = h.load(false);
    await tick();
    h.page.onUnload();
    late.resolve({ id: 42, status: 0 });
    await unloading;
    assert.equal(h.order().status, 2);
    assert.equal(h.state.timers.size, 0);
  });

  test(`${kind}: 操作后的刷新使在途旧响应失效，并串行读取最新状态`, async () => {
    const h = harness(kind);
    h.page.setData({ id: 42 });
    const pending = deferred();
    h.setRead(() => pending.promise);
    const old = h.load(false);
    await tick();
    h.setRead(async () => ({ id: 42, status: 3, tradeMode: 'OFFLINE', buyerId: 1 }));
    const action = h.load(true);
    pending.resolve({ id: 42, status: 0 });
    await Promise.all([old, action]);
    assert.equal(h.state.reads, 2);
    assert.equal(h.order().status, 3);
    h.page.onUnload();
  });
}

test('接单：图片选择/上传期间返回不发刷新，关闭后补刷且不重置图片', async () => {
  const h = harness('taker');
  await h.page.onLoad({ id: 42 });
  h.page.onShow();
  h.page.showDialogWithImage('PURCHASE_PROOF');
  h.page.setData({ fileList: [{ url: 'local.jpg', status: 'loading' }], proofUploading: true });
  h.page.onHide();
  await h.page.onShow();
  await h.page.onPullDownRefresh();
  assert.equal(h.state.reads, 1);
  assert.equal(h.page.data.fileList[0].url, 'local.jpg');
  assert.equal(h.page.data.proofUploading, true);
  h.page.closeWithImage();
  await tick();
  assert.equal(h.state.reads, 2);
  assert.equal(h.page.data.image, 'saved.jpg');
  h.page.onUnload();
});
