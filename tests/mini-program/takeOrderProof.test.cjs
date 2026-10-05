const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const test = require('node:test');
const feedbackStub = require('./helpers/feedbackStub.cjs');

const source = fs.readFileSync(path.resolve(__dirname,
  '../../apps/mini-program/pages/orders/takeOrders/takesInfo/info.js'), 'utf8');

function deferred() {
  let resolve;
  const promise = new Promise(done => { resolve = done; });
  return { promise, resolve };
}

function harness(purpose, overrides = {}) {
  const state = { reads: 0, updates: [], released: [], messages: [] };
  const order = { id: 42, status: purpose === 'PURCHASE_PROOF' ? 1 : 2,
    businessType: purpose === 'PURCHASE_PROOF' ? 'PURCHASE' : 'NORMAL' };
  const takeService = {
    getMyTakeOrders: async () => [{ orderId: 42 }],
    getTakeOrderDetail: async () => ({ id: 7 }),
    getDeliveryImage: async () => null,
    updateTakeOrderStatus: async payload => {
      state.updates.push(payload);
      order.status += 1;
    },
    ...overrides.takeService,
  };
  const mediaService = {
    uploadImage: async () => ({ mediaId: 99, previewUrl: 'proof.jpg' }),
    releaseTemporaryImage: async id => { state.released.push(id); },
    ...overrides.mediaService,
  };
  let page;
  vm.runInNewContext(source, {
    console: { log() {}, warn() {}, error() {} },
    getApp: () => ({ globalData: { API_URL: 'https://example.test' } }),
    wx: { showShareMenu() {}, hideShareMenu() {} },
    require(name) {
      if (name.endsWith('runnerAmount')) return require('../../apps/mini-program/utils/runnerAmount');
      if (name.endsWith('detailRefresh')) return require('../../apps/mini-program/utils/detailRefresh');
      if (name.endsWith('feedback')) return feedbackStub();
      if (name.endsWith('takeOrderService')) return takeService;
      if (name.endsWith('mediaService')) return mediaService;
      if (name.endsWith('userOrderService')) return {
        getMyOrderDetail: async () => { state.reads++; return { ...order }; },
      };
      if (name.endsWith('tokenManager')) return { waitForToken: async () => {} };
      if (name.endsWith('transformers')) return { showLoading() {}, hideLoading() {},
        showError(_, text) { state.messages.push(text); } };
      if (name.endsWith('commonJs')) return {
        checkCilcleToast() {},
        errorCilcleToast(_, text) { state.messages.push(text); },
        showErrorToast(_, text) { state.messages.push(text); },
        _getExpectTimeDisplay: () => '',
        _getExpectedDeliveryDate: () => new Date(),
        _logErrInfo() {},
      };
      return {};
    },
    Page(config) {
      page = { ...config, data: structuredClone(config.data), setData(values) {
        for (const [key, value] of Object.entries(values)) {
          const match = key.match(/^fileList\[(\d+)\]\.(.+)$/);
          if (match) this.data.fileList[Number(match[1])][match[2]] = value;
          else this.data[key] = value;
        }
      } };
    },
  });
  page.setData({ id: 42, orderInfo: { ...order }, isMyTaken: true, taker: { id: 7 },
    image: 'saved.jpg' });
  page.showDialogWithImage(purpose);
  return { page, state };
}

for (const purpose of ['DELIVERY_PROOF', 'PURCHASE_PROOF']) {
  test(`${purpose}：空白关闭和取消均保留订单信息，不请求刷新`, () => {
    for (const event of [{ detail: { visible: false, trigger: 'overlay' } }, { detail: {} }]) {
      const { page, state } = harness(purpose);
      const before = JSON.stringify([page.data.orderInfo, page.data.taker, page.data.image]);
      page.closeWithImage(event);
      assert.equal(page.data.showWithImage, false);
      assert.equal(page.data.isMyTaken, true);
      assert.equal(JSON.stringify([page.data.orderInfo, page.data.taker, page.data.image]), before);
      assert.equal(state.reads, 0);
      assert.equal(state.updates.length, 0);
    }
  });

  test(`${purpose}：未上传点击确定保留弹窗，上传后才更新对应状态`, async () => {
    const { page, state } = harness(purpose);
    await page.closeConfirmWithImage();
    assert.equal(page.data.showWithImage, true);
    assert.equal(page.data.isMyTaken, true);
    assert.equal(state.reads, 0);
    assert.equal(state.updates.length, 0);
    assert.match(state.messages[0], /请先上传/);
    await page.onUpload({ url: 'local.jpg' });
    assert.equal(page.data.image, 'saved.jpg');
    await page.closeConfirmWithImage();
    assert.equal(page.data.showWithImage, false);
    assert.equal(page.data.isMyTaken, true);
    assert.equal(page.data.proofSubmitting, false);
    assert.equal(page.data.imageAssetId, null);
    assert.equal(JSON.stringify(state.updates), JSON.stringify([
      { id: 7, status: purpose === 'PURCHASE_PROOF' ? 1 : 2, imageAssetId: 99 },
    ]));
    assert.equal(state.released.length, 0);
  });

  test(`${purpose}：提交失败保留图片可重试，提交中取消和重复确定无效`, async () => {
    const pending = deferred();
    let attempts = 0;
    const { page, state } = harness(purpose, { takeService: {
      updateTakeOrderStatus: async () => {
        attempts++;
        await pending.promise;
        throw new Error('network');
      },
    } });
    await page.onUpload({ url: 'local.jpg' });
    const confirm = page.closeConfirmWithImage();
    page.closeWithImage();
    await page.closeConfirmWithImage();
    assert.equal(attempts, 1);
    assert.equal(page.data.showWithImage, true);
    assert.equal(state.released.length, 0);
    pending.resolve();
    await confirm;
    assert.equal(page.data.proofSubmitting, false);
    assert.equal(page.data.imageAssetId, 99);
    assert.equal(page.data.fileList[0].status, 'done');
    assert.equal(page.data.isMyTaken, true);
    await page.closeConfirmWithImage();
    assert.equal(attempts, 2);
    page.closeWithImage();
    assert.deepEqual(state.released, [99]);
    assert.equal(page.data.image, 'saved.jpg');
  });
}

test('上传中确定不提交；退出后迟到上传释放图片，不污染重新打开的弹窗', async () => {
  const pending = deferred();
  let progress;
  const { page, state } = harness('PURCHASE_PROOF', { mediaService: {
    uploadImage: (_, __, callback) => { progress = callback; return pending.promise; },
  } });
  const uploading = page.onUpload({ url: 'local.jpg' });
  await page.closeConfirmWithImage();
  assert.equal(page.data.showWithImage, true);
  assert.equal(state.updates.length, 0);
  page.closeWithImage();
  page.showDialogWithImage('DELIVERY_PROOF');
  progress(80);
  pending.resolve({ mediaId: 99, previewUrl: 'late.jpg' });
  await uploading;
  assert.deepEqual(state.released, [99]);
  assert.equal(page.data.fileList.length, 0);
  assert.equal(page.data.imageAssetId, null);
  assert.equal(page.data.image, 'saved.jpg');
  assert.equal(page.data.proofUploading, false);
});

test('身份查询失败时刷新不清空已显示的接单信息，成功查明非本人时才清空', async () => {
  let fail = true;
  const { page } = harness('DELIVERY_PROOF', { takeService: {
    getMyTakeOrders: async () => {
      if (fail) throw new Error('network');
      return [];
    },
  } });
  await page._loadOrderInfo();
  assert.equal(page.data.isMyTaken, true);
  assert.equal(page.data.taker.id, 7);
  fail = false;
  await page._loadOrderInfo();
  assert.equal(page.data.isMyTaken, false);
  assert.equal(page.data.taker.id, undefined);
  assert.equal(page.data.image, null);
});

test('图片选择返回后提交凭证，提交后的刷新同时满足延后刷新，不重复请求', async () => {
  const { page, state } = harness('DELIVERY_PROOF');
  page.onHide();
  await page.onShow();
  assert.equal(state.reads, 0);
  await page.onUpload({ url: 'local.jpg' });
  await page.closeConfirmWithImage();
  await new Promise(resolve => setImmediate(resolve));
  assert.equal(state.reads, 1);
  assert.equal(page._resumeRefreshPending, false);
});
