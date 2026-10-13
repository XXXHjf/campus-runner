const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
const source = fs.readFileSync(path.resolve(__dirname,
  '../../apps/mini-program/pages/second-hand/bargains/bargains.js'), 'utf8');

function harness({ fail = false, latestStatus = 0 } = {}) {
  let page;
  const calls = [];
  const app = { globalData: { userInfo: { id: 7, token: 'a' } } };
  vm.runInNewContext(source, {
    getApp: () => app,
    wx: { getStorageSync() {}, setStorageSync() {}, navigateTo: options => calls.push(options.url) },
    require: name => name.endsWith('tokenManager') ? { getToken: () => 'a' }
      : name.endsWith('secondHandStatus') ? require('../../apps/mini-program/utils/secondHandStatus')
      : name.endsWith('feedback') ? require('./helpers/feedbackStub.cjs')({ showToast: options => calls.push(options.title) })
      : name.endsWith('secondHandService') ? {
        rejectBargain: async (id, reason) => {
          calls.push({ id, ...reason });
          if (fail) throw Error('network');
        },
        listMyBargains: async query => {
          calls.push({ query: { ...query } });
          if (query.bargainId) return [{ id: 90, buyerId: 7, sellerId: 8, status: 3 }];
          return [{ id: 1, buyerId: 7, sellerId: 8, status: 0 }];
        },
      } : {},
    Page: config => { page = { ...config, data: structuredClone(config.data) }; },
  });
  page.setData = patch => Object.assign(page.data, patch);
  page.data.received = [{ id: 7, isSeller: true, status: 0, offerPrice: 20 }];
  page._listScope = JSON.stringify(['a', undefined, 'received']);
  return { page, calls, latestStatus };
}

const event = { currentTarget: { dataset: { id: 7 } } };

test('third option owns optional explanation and presets send no extra explanation', async () => {
  const { page, calls } = harness();
  page.loadList = async () => true;
  page.reject(event);
  assert.equal(page.data.rejectReasonCode, '');
  page.selectRejectReason({ detail: { value: 'CUSTOM' } });
  page.changeRejectNote({ detail: { value: '  暂时不方便出售  ' } });
  page.selectRejectReason({ detail: { value: 'PRICE_NOT_SUITABLE' } });
  assert.equal(page.data.rejectNote, '');
  await page.submitReject();
  assert.deepEqual(calls[0], { id: 7, reasonCode: 'PRICE_NOT_SUITABLE', note: null });
  assert.equal(page.data.received[0].rejectReasonText, '暂不考虑这个价格');
  assert.equal(page.data.showReject, false);
});

test('custom option can be submitted blank and shows fallback without duplicating feedback', async () => {
  const { page, calls } = harness();
  page.loadList = async () => true;
  page.reject(event);
  page.selectRejectReason({ detail: { value: 'CUSTOM' } });
  await page.submitReject();
  assert.deepEqual(calls[0], { id: 7, reasonCode: 'CUSTOM', note: '' });
  assert.equal(page.data.received[0].rejectReasonText, '卖家未填写原因');
});

test('notification loads old quote by identity filter even when it is beyond first page', async () => {
  const { page, calls } = harness();
  page.onLoad({ tab: 'issued', bargainId: '90' });
  await page.onShow();
  assert.equal(page.data.issued[0].id, 90);
  assert.equal(page.data.issued[0].status, 3);
  assert.equal(calls[1].query.bargainId, '90');
  page.buyBargain({ currentTarget: { dataset: { id: 90 } } });
  assert.equal(calls.some(item => typeof item === 'string' && item.includes('bargainId=')), false);
});

test('rejection failure preserves custom text for retry unless refreshed quote is terminal', async () => {
  const { page } = harness({ fail: true });
  page.loadList = async () => true;
  page.reject(event);
  page.selectRejectReason({ detail: { value: 'CUSTOM' } });
  page.changeRejectNote({ detail: { value: '谢谢理解' } });
  await page.submitReject();
  assert.equal(page.data.showReject, true);
  assert.equal(page.data.rejectNote, '谢谢理解');
  assert.equal(page.data.received[0].status, 0);
});

test('unverified state and deleted product cannot start stale actions', () => {
  const { page, calls } = harness();
  page.data.actionsReady = false;
  page.reject(event);
  assert.equal(page.data.showReject, false);
  page.data.received[0].productDeleted = true;
  page.gotoProduct({ currentTarget: { dataset: { id: 7, productId: 10 } } });
  assert.equal(calls.length, 0);
});
