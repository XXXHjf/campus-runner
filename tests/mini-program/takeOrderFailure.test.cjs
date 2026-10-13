const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
const feedbackStub = require('./helpers/feedbackStub.cjs');
const source = fs.readFileSync(path.resolve(__dirname, '../../apps/mini-program/pages/orders/takeOrders/takesInfo/info.js'), 'utf8');

function harness(error) {
  let page;
  const state = { calls: 0, privateReads: 0, refreshed: 0, messages: [], navigated: [] };
  const presentation = feedbackStub({}, text => state.messages.push(text));
  presentation.navigate = (_, method, options, message) => { state.messages.push(message); state.navigated.push(options.url); };
  vm.runInNewContext(source, {
    console: { log() {}, error() {} }, wx: {}, getApp: () => ({ globalData: { API_URL: 'https://example.test' } }),
    require(name) {
      if (name.endsWith('feedback')) return presentation;
      if (name.endsWith('accessGuard')) return { ensureAuthenticated: async () => true };
      if (name.endsWith('takeOrderService')) return {
        getTakeOrderDetail: async () => { state.privateReads++; throw Error('资料不可访问'); },
        acceptOrderById: async id => { assert.equal(id, 42); state.calls++; if (error) throw error; },
      };
      if (name.endsWith('commonJs')) return { checkCilcleToast: (_, text) => state.messages.push(text) };
      return {};
    },
    Page(config) { page = { ...config, data: structuredClone(config.data), setData(value) { Object.assign(this.data, value); } }; },
  });
  page.data.id = 42; page.data.userInfo = { phone: '12345678900' };
  page._loadOrderInfo = async () => { state.refreshed++; };
  return { page, state };
}

test('confirmed other taker keeps the precise server message and returns to the order hall', async () => {
  const error = Object.assign(new Error('该订单已被接单，请选择其他订单'), { requestOutcome: 'rejected' });
  const { page, state } = harness(error);
  await page.statusTo1();
  assert.deepEqual(state.messages, [error.message]);
  assert.deepEqual(state.navigated, ['/pages/index/index']);
  assert.equal(state.privateReads, 0);
});

test('temporary failure, unreadable outcome and legacy internal errors are never reported as taken', async () => {
  for (const text of ['接单暂未成功，请刷新后重试', '系统数据异常，请稍后重试', '订单错误', '死锁', '锁等待']) {
    const error = Object.assign(new Error(text), { requestOutcome: 'rejected' });
    const { page, state } = harness(error);
    await page.statusTo1();
    assert.deepEqual(state.messages, ['接单暂未成功，请刷新后重试']);
    assert.equal(state.navigated.length, 0);
  }
  const { page, state } = harness(new Error('网络连接失败，请稍后重试'));
  await page.statusTo1();
  assert.deepEqual(state.messages, ['接单暂未成功，请刷新后重试']);
});

test('specific business failures such as cancellation and authentication are preserved', async () => {
  for (const text of ['该订单已取消，请选择其他订单', '不能接自己发布的订单', '完成校园认证后才能操作']) {
    const { page, state } = harness(Object.assign(new Error(text), { requestOutcome: 'rejected' }));
    await page.statusTo1(); assert.deepEqual(state.messages, [text]); assert.equal(state.calls, 1);
  }
});

test('successful take goes directly through authoritative server validation without a private preflight', async () => {
  const { page, state } = harness();
  await page.statusTo1();
  assert.equal(state.calls, 1); assert.equal(state.privateReads, 0); assert.equal(state.refreshed, 1);
  assert.deepEqual(state.messages, ['接单成功']);
});
