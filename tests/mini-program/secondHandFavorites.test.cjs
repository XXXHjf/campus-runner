const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const vm = require('node:vm');

const root = path.resolve(__dirname, '../../apps/mini-program');
const read = (file) => fs.readFileSync(path.join(root, file), 'utf8');
const event = (id) => ({ currentTarget: { dataset: { id } } });
const tick = () => new Promise((resolve) => setImmediate(resolve));
function deferred() {
  let resolve;
  let reject;
  const promise = new Promise((yes, no) => { resolve = yes; reject = no; });
  return { promise, resolve, reject };
}

function createHarness({ beforeRequest = async () => {} } = {}) {
  const records = [
    { id: 1, title: '测试商品一', images: 'one.jpg,two.jpg', status: 0 },
    { id: 2, title: '测试商品二', images: '', status: 3 },
  ];
  const favorites = new Set([1, 2]);
  const calls = [];
  const toasts = [];
  const navigation = [];
  async function request(options) {
    const pathname = new URL(options.url).pathname;
    const call = { method: options.method, path: pathname };
    calls.push(call);
    await beforeRequest(call);
    const match = pathname.match(/\/products\/(\d+)(\/favorite)?$/);
    if (match && match[2]) {
      if (options.method === 'DELETE') favorites.delete(Number(match[1]));
      if (options.method === 'POST') favorites.add(Number(match[1]));
      return { data: { code: 1 } };
    }
    const data = match
      ? { ...records.find((item) => item.id === Number(match[1])), favorited: favorites.has(Number(match[1])) }
      : records.filter((item) => favorites.has(item.id)).map((item) => ({ ...item, favorited: true }));
    return { data: { code: 1, data } };
  }
  const serviceModule = { exports: {} };
  vm.runInNewContext(read('services/secondHandService.js'), {
    module: serviceModule,
    getApp: () => ({ globalData: { API_URL: 'https://example.test' } }),
    require: () => ({ request, safeList: (response) => response.data.data }),
  });
  const service = serviceModule.exports;
  function createPage() {
    let page;
    vm.runInNewContext(read('pages/second-hand/favorites/favorites.js'), {
      Page(config) {
        page = {
          ...config,
          data: structuredClone(config.data),
          renderCount: 0,
          setData(update) { Object.assign(this.data, update); this.renderCount += 1; },
        };
      },
      require(id) {
        if (id.endsWith('secondHandService')) return service;
        return { friendlyError: (error, fallback) => error.message || fallback };
      },
      wx: {
        showToast: (options) => toasts.push(options.title),
        stopPullDownRefresh() {},
        navigateTo: (options) => navigation.push(options.url),
        switchTab: (options) => navigation.push(options.url),
      },
    });
    page.onLoad();
    return page;
  }
  return { createPage, service, calls, favorites, toasts, navigation };
}

test('cancel keeps the card, changes local state, and can be undone without requests', async () => {
  const h = createHarness();
  const page = h.createPage();
  await page.onShow();
  page.toggleFavorite(event(1));
  assert.equal(page.data.products.length, 2);
  assert.equal(page.data.products[0].isFavorited, false);
  assert.equal(page.data.favoriteCount, 1);
  assert.equal(h.favorites.has(1), true);
  page.toggleFavorite(event('1'));
  assert.equal(page.data.products[0].isFavorited, true);
  assert.equal(page.data.favoriteCount, 2);
  await page.onUnload();
  assert.equal(h.calls.filter((call) => call.method !== 'GET').length, 0);
  assert.deepEqual(h.toasts, []);
});

test('refresh and returning from details preserve pending choices until unload', async () => {
  const h = createHarness();
  const page = h.createPage();
  await page.onShow();
  page.toggleFavorite(event(1));
  await page.onPullDownRefresh();
  page.gotoDetail(event(1));
  await page.onShow();
  assert.equal(page.data.products.length, 2);
  assert.equal(page.data.products[0].isFavorited, false);
  assert.equal(h.calls.filter((call) => call.method === 'DELETE').length, 0);
  await page.onUnload();
  assert.equal(h.favorites.has(1), false);
  assert.equal(h.favorites.has(2), true);
  const reopened = h.createPage();
  await reopened.onShow();
  assert.deepEqual(Array.from(reopened.data.products, (item) => item.id), [2]);
});

test('cancelling every card does not show an empty list before leaving', async () => {
  const h = createHarness();
  const page = h.createPage();
  await page.onShow();
  page.toggleFavorite(event(1));
  page.toggleFavorite(event(2));
  assert.equal(page.data.favoriteCount, 0);
  assert.equal(page.data.products.length, 2);
  await page.onUnload();
  await page.onUnload();
  assert.equal(h.calls.filter((call) => call.method === 'DELETE').length, 2);
  assert.equal(h.favorites.size, 0);
});

test('rapid reopening waits for the previous page cancellation to finish', async () => {
  const gate = deferred();
  const h = createHarness({ beforeRequest: (call) => call.method === 'DELETE' ? gate.promise : undefined });
  const page = h.createPage();
  await page.onShow();
  page.toggleFavorite(event(1));
  const commit = page.onUnload();
  const reopened = h.createPage();
  const loading = reopened.onShow();
  await tick();
  assert.equal(h.calls.filter((call) => call.method === 'GET').length, 1);
  gate.resolve();
  await Promise.all([commit, loading]);
  assert.deepEqual(Array.from(reopened.data.products, (item) => item.id), [2]);
});

test('a failed cancellation is reported, stays saved, and does not block other changes', async () => {
  const h = createHarness({ beforeRequest(call) {
    if (call.method === 'DELETE' && call.path.includes('/1/')) throw new Error('network failure');
  } });
  const page = h.createPage();
  await page.onShow();
  page.toggleFavorite(event(1));
  page.toggleFavorite(event(2));
  await page.onUnload();
  assert.deepEqual(h.toasts, ['部分收藏取消失败，请返回重试']);
  const reopened = h.createPage();
  await reopened.onShow();
  assert.deepEqual(Array.from(reopened.data.products, (item) => item.id), [1]);
  assert.equal(reopened.data.products[0].isFavorited, true);
});

test('a later favorite action is not overwritten by an earlier pending cancellation', async () => {
  const gate = deferred();
  const h = createHarness({ beforeRequest: (call) => call.method === 'DELETE' ? gate.promise : undefined });
  const cancellation = h.service.unfavoriteProduct(1);
  const restoration = h.service.favoriteProduct(1);
  const detail = h.service.getProduct(1);
  await tick();
  assert.equal(h.calls.length, 1);
  gate.resolve();
  await Promise.all([cancellation, restoration]);
  assert.equal((await detail).favorited, true);
  assert.deepEqual(h.calls.map((call) => call.method), ['DELETE', 'POST', 'GET']);
});

test('late list responses cannot render after the page has unloaded', async () => {
  const gate = deferred();
  const h = createHarness({ beforeRequest: () => gate.promise });
  const page = h.createPage();
  const loading = page.onShow();
  await page.onUnload();
  const renderCount = page.renderCount;
  gate.resolve();
  await loading;
  assert.equal(page.renderCount, renderCount);
});

test('the newest refresh wins when list responses arrive out of order', async () => {
  const first = deferred();
  let reads = 0;
  const h = createHarness({ beforeRequest() { if (++reads === 1) return first.promise; } });
  const page = h.createPage();
  const initial = page.onShow();
  await tick();
  await page.onPullDownRefresh();
  page.toggleFavorite(event(1));
  const renderCount = page.renderCount;
  first.resolve();
  await initial;
  assert.equal(page.renderCount, renderCount);
  assert.equal(page.data.products[0].isFavorited, false);
});
