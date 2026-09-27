const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const context = { module: { exports: {} }, require: () => ({}), getApp: () => ({ globalData: {} }) };
vm.runInNewContext(fs.readFileSync(require.resolve('../../apps/mini-program/utils/commonJs'), 'utf8'), context);
const { _getExpectedDeliveryDate, _getExpectTimeDisplay } = context.module.exports;

test('选定时间不随创建时间、分钟间隔或确认延迟改变', () => {
  for (const createTime of ['2026-09-27 15:00:45', '2026-09-27 15:01:45', '2026-09-27 15:10:00']) {
    const deadline = _getExpectedDeliveryDate(createTime, 29, '2026-09-27 15:30:00');
    assert.equal(deadline.getHours(), 15);
    assert.equal(deadline.getMinutes(), 30);
    assert.equal(deadline.getSeconds(), 0);
    assert.equal(_getExpectTimeDisplay(createTime, 29, '2026-09-27 15:30:00'), '9月27日 15:30');
    assert.equal(deadline - new Date(2026, 8, 27, 15, 29, 30), 30000);
  }
});

test('跨日和跨年保持选中的日期', () => {
  assert.equal(_getExpectTimeDisplay('2026-12-31 23:50:40', 9, '2027-01-01 00:00:00'), '1月1日 00:00');
});

test('旧订单保留原有进位显示，倒计时与显示共用截止时间', () => {
  assert.equal(_getExpectTimeDisplay('2026-09-27 15:00:45', 29), '9月27日 15:30');
  assert.equal(_getExpectedDeliveryDate('2026-09-27 15:00:45', 29).getSeconds(), 0);
  assert.equal(_getExpectTimeDisplay('2026-09-27 15:00:00', 30), '9月27日 15:30');
});

function loadPublishPage(now, selectedTime) {
  let page;
  const requests = [];
  class Clock extends Date { static now() { return now; } }
  vm.runInNewContext(fs.readFileSync(require.resolve('../../apps/mini-program/pages/orders/myOrders/ordersAdd/add.js'), 'utf8'), {
    Date: Clock,
    console: { log() {}, error() {} },
    getApp: () => ({ globalData: {} }),
    require(id) {
      if (id.endsWith('userOrderService')) return { createOrder: async data => { requests.push(data); return { data: { id: 1 } }; } };
      if (id.endsWith('transformers')) return { showLoading() {}, hideLoading() {} };
      if (id.endsWith('orderDraftManager')) return { clearDraft() {} };
      if (id.endsWith('orderConstants')) return require('../../apps/mini-program/utils/orderConstants');
      return {};
    },
    Page(config) { page = config; },
  });
  Object.assign(page, {
    data: { selectedTime, showOrderConfirm: true, priceAccess: 'free', showPickUp: { id: 1 }, showRecive: { id: 2 }, showCategory: { id: 3 }, fileList: [] },
    setData(value) { Object.assign(this.data, value); },
    saveLastUsedAddress() {}, _redirectToHome() {},
  });
  return { page, requests };
}

test('发布页跨一分钟提交仍发送原选定时间，而非反推时间', async () => {
  const { page, requests } = loadPublishPage(+new Date(2026, 8, 27, 15, 1, 45), +new Date(2026, 8, 27, 15, 30));
  await page._apiPostOrder();
  assert.equal(requests[0].expectedDeliveryTime, '2026-09-27 15:30:00');
  assert.equal(requests[0].gap, 29);
});

test('授权等待后时间已过期时不发送创建请求', async () => {
  const deadline = +new Date(2026, 8, 27, 15, 30);
  const { page, requests } = loadPublishPage(deadline + 1, deadline);
  await assert.rejects(page._apiPostOrder(), /预期送达时间已过/);
  assert.equal(requests.length, 0);
  assert.equal(page.data.showOrderConfirm, false);
});
