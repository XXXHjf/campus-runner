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
      if (name.endsWith('runnerAmount')) return require('../../apps/mini-program/utils/runnerAmount');
      if (name.endsWith('detailRefresh')) return detailRefresh;
      if (name.endsWith('feedback')) return feedbackStub(wx, message => state.errors.push(message));
      if (name.endsWith('secondHandStatus')) return statusTools;
      if (name.endsWith('userOrderService')) return {
        getMyOrderDetail: getOrder,
        async cancelOrder(...args) { state.cancelled = args; },
        async deleteOrder(...args) { state.hidden = args; },
      };
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

test('接单详情以服务端身份展示图片，不依赖我的接单第一页', async () => {
  const h = harness('taker');
  h.setRead(async () => ({ id: 42, status: 1, viewerIsTaker: true, images: ['clear.jpg'] }));
  h.page._getMyTakeInfo = async () => { throw new Error('不得读取接单列表来判断身份'); };
  await h.load();
  assert.equal(h.page.data.isMyTaken, true);
  assert.deepEqual(Array.from(h.page.data.orderInfo.images), ['clear.jpg']);
  assert.equal(h.page.data.taker.id, 7);
});

test('接单详情刷新为浏览身份时清除旧接单权限和凭证', async () => {
  const h = harness('taker');
  h.page.setData({ isMyTaken: true, taker: { id: 7 }, image: 'old-proof.jpg' });
  h.setRead(async () => ({ id: 42, status: 0, viewerIsTaker: false, images: ['blurred.jpg'] }));
  await h.load();
  assert.equal(h.page.data.isMyTaken, false);
  assert.equal(h.page.data.taker.id, undefined);
  assert.equal(h.page.data.image, null);
  assert.deepEqual(Array.from(h.page.data.orderInfo.images), ['blurred.jpg']);
});

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

test('发单详情：仅终态开放隐藏，待收款和失败重试不开放', () => {
  const h = harness('publisher');
  for (const status of [-3, 4, 6]) {
    h.page.data.orderInfo = { status };
    h.page.buttonColor();
    assert.equal(h.page.data.buttonText, '隐藏订单');
    assert.equal(h.page.data.buttonDisabled, false);
  }
  for (const status of [-2, 1, 2, 5, 7]) {
    h.page.data.orderInfo = { status };
    h.page.buttonColor();
    assert.equal(h.page.data.buttonDisabled, true);
  }
  h.page.data.orderInfo = { status: -1 };
  h.page.buttonColor();
  assert.equal(h.page.data.buttonText, '取消订单');
});

test('发单详情：未支付超时走取消并刷新，不调用隐藏', async () => {
  const h = harness('publisher');
  h.page.data.id = 42;
  h.page.data.orderInfo = { orderNumber: 'O42', status: -1 };
  await h.page._autoCancelUnpaidOrder();
  assert.deepEqual(h.state.cancelled, [42, '支付超时', 'O42']);
  assert.equal(h.state.hidden, undefined);
  assert.equal(h.state.reads, 1);
  h.page.onUnload();
});

for (const kind of ['publisher', 'taker']) {
  test(`${kind}: address snapshots preserve spaces, long details and line breaks`, async () => {
    const h = harness(kind);
    const pickup = '学校 北 校区 快递点 A 楼 123 室\n侧门入口 ' + '详细指引 '.repeat(30);
    const delivery = '学校 南校区 宿舍 B 楼 456 室\n请敲门';
    h.setRead(async () => ({ id: 42, status: 1, pickUpAddress: pickup, reciveAddress: delivery }));
    await h.page.onLoad({ id: 42 });
    assert.equal(h.page.data.pickUpAddress, pickup);
    assert.equal(h.page.data.reciveAddress, delivery);
    const template = fs.readFileSync(path.resolve(__dirname,
      '../../apps/mini-program/pages', files[kind].replace('.js', '.wxml')), 'utf8');
    assert.match(template, /white-space: pre-wrap;">\{\{pickUpAddress\}\}/);
    assert.match(template, /white-space: pre-wrap;">\{\{reciveAddress\}\}/);
    assert.doesNotMatch(template, /(?:pickUpAddress|reciveAddress)\[\d\]/);
    h.page.onUnload();
  });
}

test('publisher payment section follows actual payable amount and business type', () => {
  const h = harness('publisher');
  for (const [input, amount, visible] of [
    [{ price: 0, payAmount: 0, businessType: 'NORMAL' }, 0, false],
    [{ price: '0.00', pay_amount: '0.00', businessType: 'NORMAL' }, 0, false],
    [{ price: 10, payAmount: 0, businessType: 'NORMAL' }, 0, false],
    [{ price: 10, payAmount: 10.5, businessType: 'NORMAL' }, 10.5, true],
    [{ price: 0, productAmount: 50, payAmount: 51, businessType: 'PURCHASE' }, 51, true],
    [{ price: 0, productAmount: 50, payAmount: 0, businessType: 'PURCHASE' }, 0, false],
    [{ price: 0, productAmount: 999, businessType: 'NORMAL' }, 0, false],
    [{ price: 10, serviceFee: 0.5, businessType: 'NORMAL' }, 10.5, true],
    [{ price: 0, productAmount: 50, serviceFee: 1, businessType: 'PURCHASE' }, 51, true],
    [{ price: 10, payAmount: 'invalid', businessType: 'NORMAL' }, 0, false],
  ]) {
    const normalized = h.page._normalizeAmountFields(input);
    assert.equal(normalized.paymentDisplayAmount, amount);
    assert.equal(normalized.hasPaymentAmount, visible);
  }
  const template = fs.readFileSync(path.resolve(__dirname,
    '../../apps/mini-program/pages', files.publisher.replace('.js', '.wxml')), 'utf8');
  assert.match(template, /wx:if="\{\{orderInfo.hasPaymentAmount\}\}" class="publisher-detail-payment"/);
});
