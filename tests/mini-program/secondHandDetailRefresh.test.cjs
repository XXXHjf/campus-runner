const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const vm = require('node:vm');
const feedbackStub = require('./helpers/feedbackStub.cjs');
const source = fs.readFileSync(path.resolve(__dirname, '../../apps/mini-program/pages/second-hand/detail/detail.js'), 'utf8');
const deferred = () => {
  let resolve, reject;
  const promise = new Promise((yes, no) => { resolve = yes; reject = no; });
  return { promise, resolve, reject };
};
const product = (extra = {}) => ({ id: 1, sellerId: 9, price: 20, description: '原描述', status: 0, favoriteCount: 2, ...extra });
function harness(service = {}) {
  let page;
  const state = { token: 'a', product: product(), reads: 0, errors: [], routes: [] };
  const app = { globalData: { userInfo: { id: 7 } } };
  vm.runInNewContext(source, {
    Page(config) { page = { ...config, data: structuredClone(config.data), setData(update) { Object.assign(this.data, update); } }; },
    getApp: () => app,
    wx: { showShareMenu() {}, getStorageSync: () => null, setStorageSync() {} },
    require(id) {
      if (id.endsWith('feedback')) return feedbackStub({ navigateTo: (o) => state.routes.push(o.url) }, (e) => state.errors.push(e));
      if (id.endsWith('secondHandService')) return { getProduct: async () => { state.reads++; return state.product; }, listProductBargains: async () => [], ...service };
      if (id.endsWith('tokenManager')) return { getToken: () => state.token, hasToken: () => !!state.token };
      if (id.endsWith('accessGuard')) return { ensureAuthenticated: async () => true };
      if (id.endsWith('secondHandStatus')) return { friendlyError: (_, fallback) => fallback };
      if (id.endsWith('subscriptionService')) return { requestSecondHandOrder: async () => {} };
      if (id.endsWith('userService')) return { getUserInfo: async () => ({ id: 7 }) };
      if (id.endsWith('deliveryAddressService')) return { getMyAddresses: async () => [] };
      throw new Error(id);
    },
  });
  page.onLoad({ id: '1' });
  return { page, state, app };
}
test('initial show and simultaneous refresh share one request', async () => {
  const pending = deferred();
  let reads = 0;
  const { page, state } = harness({ getProduct: () => { reads++; return pending.promise; } });
  assert.equal(reads, 0);
  const showing = page.onShow();
  const retry = page.loadDetail();
  pending.resolve(product());
  await Promise.all([showing, retry]);
  assert.equal(reads, 1);
  assert.equal(page.data.product.canBuy, true);
});
test('return from edit or related page refreshes price, description, favorites and availability', async () => {
  const { page, state } = harness();
  await page.onShow();
  page.onHide();
  state.product = product({ price: 35, description: '新描述', favorited: true, status: 2 });
  await page.onShow();
  assert.equal(state.reads, 2);
  assert.equal(page.data.product.price, 35);
  assert.equal(page.data.product.description, '新描述');
  assert.equal(page.data.product.isFavorited, true);
  assert.equal(page.data.product.canBuy, false);
});
test('older result and finally cannot overwrite a newer return refresh', async () => {
  const old = deferred(), fresh = deferred();
  let reads = 0;
  const { page } = harness({ getProduct: () => ++reads === 1 ? old.promise : fresh.promise });
  const first = page.onShow();
  page.onHide();
  const second = page.onShow();
  old.resolve(product({ price: 1 }));
  await first;
  assert.equal(page.data.loading, true);
  assert.equal(page.data.product.id, undefined);
  fresh.resolve(product({ price: 40 }));
  await second;
  assert.equal(page.data.product.price, 40);
  assert.equal(page.data.loading, false);
});
test('stale failures after unload do not update the page or show an error', async () => {
  const pending = deferred();
  const { page, state } = harness({ getProduct: () => pending.promise });
  const showing = page.onShow();
  page.onUnload();
  const before = JSON.stringify(page.data);
  pending.reject(new Error('late'));
  await showing;
  assert.equal(JSON.stringify(page.data), before);
  assert.equal(state.errors.length, 0);
});
test('session switch invalidates old detail and clears sheets', async () => {
  const old = deferred();
  let reads = 0;
  const { page, state, app } = harness({ getProduct: () => ++reads === 1 ? old.promise : Promise.resolve(product({ sellerId: 8 })) });
  const first = page.onShow();
  page.setData({ showBuySheet: true, showBargain: true });
  state.token = 'b';
  app.globalData.userInfo = { id: 8 };
  await page.onShow();
  old.resolve(product());
  await first;
  assert.equal(page.data.product.isOwner, true);
  assert.equal(page.data.currentUserId, 8);
  assert.equal(page.data.showBuySheet, false);
});
test('favorite write does not restore an older product snapshot', async () => {
  const write = deferred();
  const { page, state } = harness({ favoriteProduct: () => write.promise });
  await page.onShow();
  const updating = page.toggleFavorite();
  await Promise.resolve();
  state.product = product({ price: 60, status: 2, favorited: true, favoriteCount: 3 });
  await page.loadDetail();
  write.resolve();
  await updating;
  assert.equal(page.data.product.price, 60);
  assert.equal(page.data.product.canBuy, false);
  assert.equal(page.data.product.isFavorited, true);
  assert.equal(page.data.product.favoriteCount, 3);
});
test('order creation refreshes availability and return reflects cancellation', async () => {
  const { page, state } = harness({ createOrder: async () => { state.product = product({ status: 2 }); return { id: 22 }; } });
  await page.onShow();
  await page.createOrder();
  assert.equal(page.data.product.canBuy, false);
  assert.equal(state.routes.length, 1);
  page.onHide();
  state.product = product({ status: 0 });
  await page.onShow();
  assert.equal(page.data.product.canBuy, true);
});

test('accepted quote opens existing sheet and submits quote id with chosen delivery', async () => {
  let payload;
  const { page } = harness({
    listProductBargains: async () => [{ id: 12, buyerId: 7, status: 1, offerPrice: 15 }],
    createOrder: async (data) => { payload = data; return { id: 30 }; },
  });
  page.onLoad({ id: '1', bargainId: '12' });
  await page.onShow();
  assert.equal(page.data.showBuySheet, true);
  assert.equal(page.data.orderPrice, 15);
  assert.equal(page.data.product.price, 20);
  page.setData({ selectedDeliveryMode: 1, selectedBuyerAddressId: 9, selectedBuyerAddressText: '3栋楼下' });
  await page.createOrder();
  assert.equal(payload.bargainId, 12);
  assert.equal(payload.deliveryMode, 1);
  assert.equal(payload.buyerDeliveryAddressId, 9);
  assert.equal(payload.offerPrice, undefined);
});

test('normal purchase uses own accepted quote while excluding historical orders and other buyers', async () => {
  const { page } = harness({ listProductBargains: async () => [
    { id: 1, buyerId: 7, status: 1, orderId: 90, offerPrice: 1 },
    { id: 2, buyerId: 8, status: 1, offerPrice: 2 },
    { id: 3, buyerId: 7, status: 1, offerPrice: 16 },
  ] });
  await page.onShow(); await page.buyNow();
  assert.equal(page.data.acceptedBargain.id, 3);
  assert.equal(page.data.orderPrice, 16);
});

test('direct purchase without accepted quote retains original price', async () => {
  const { page } = harness();
  await page.onShow(); await page.buyNow();
  assert.equal(page.data.acceptedBargain, null);
  assert.equal(page.data.orderPrice, 20);
});

test('expired or already ordered targeted quote never silently falls back to original price', async () => {
  const { page } = harness({ listProductBargains: async () => [{ id: 12, buyerId: 7, status: 3, offerPrice: 15 }] });
  page.onLoad({ id: '1', bargainId: '12' }); await page.onShow();
  assert.equal(page.data.showBuySheet, false);
});

test('quote lookup failure prevents opening checkout at an unconfirmed price', async () => {
  const { page } = harness({ listProductBargains: async () => { throw new Error('network'); } });
  await page.onShow(); await page.buyNow();
  assert.equal(page.data.showBuySheet, false);
});
