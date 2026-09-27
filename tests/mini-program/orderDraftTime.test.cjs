const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const constants = require('../../apps/mini-program/utils/orderConstants');
const feedbackStub = require('./helpers/feedbackStub.cjs');
const at = (day, hour, minute = 0) => +new Date(2026, 8, day, hour, minute);

function harness(initialNow) {
  const state = { now: initialNow, storage: {}, requests: [], messages: [], modal: null };
  class Clock extends Date {
    constructor(...args) { super(...(args.length ? args : [state.now])); }
    static now() { return state.now; }
  }
  const wx = {
    getStorageSync: key => state.storage[key],
    setStorageSync: (key, value) => { state.storage[key] = structuredClone(value); },
    removeStorageSync: key => { delete state.storage[key]; },
    showModal: options => { state.modal = options; },
  };
  const quietConsole = { log() {}, error() {} };
  const managerContext = { Date: Clock, wx, console: quietConsole, module: { exports: {} }, require: () => constants, setTimeout, clearTimeout };
  vm.runInNewContext(fs.readFileSync(require.resolve('../../apps/mini-program/utils/orderDraftManager'), 'utf8'), managerContext);
  const manager = managerContext.module.exports;
  const commonContext = { module: { exports: {} }, require: () => feedbackStub(wx), getApp: () => ({ globalData: {} }) };
  vm.runInNewContext(fs.readFileSync(require.resolve('../../apps/mini-program/utils/commonJs'), 'utf8'), commonContext);
  const validationContext = { module: { exports: {} }, require: () => commonContext.module.exports };
  vm.runInNewContext(fs.readFileSync(require.resolve('../../apps/mini-program/utils/validators'), 'utf8'), validationContext);
  let page;
  vm.runInNewContext(fs.readFileSync(require.resolve('../../apps/mini-program/pages/orders/myOrders/ordersAdd/add'), 'utf8'), {
    Date: Clock, wx, console: quietConsole, getApp: () => ({ globalData: {} }),
    Page(config) { page = { ...config, data: structuredClone(config.data), setData(update, cb) { Object.assign(this.data, update); cb?.(); } }; },
    require(id) {
      if (id.endsWith('orderDraftManager')) return manager;
      if (id.endsWith('orderConstants')) return constants;
      if (id.endsWith('feedback')) return feedbackStub(wx);
      if (id.endsWith('privacy')) return { maskPhone: value => value };
      if (id.endsWith('commonJs')) return {
        errorCilcleToast: (_, message) => state.messages.push(message),
        checkCilcleToast() {}, showErrorToast: (_, message) => state.messages.push(message),
      };
      if (id.endsWith('accessGuard')) return { ensureAuthenticated: async () => true };
      if (id.endsWith('validators')) return validationContext.module.exports;
      if (id.endsWith('transformers')) return { showLoading() {}, hideLoading() {} };
      if (id.endsWith('userOrderService')) return { createOrder: async data => { state.requests.push(data); return { data: { id: 1 } }; } };
      return {};
    },
  });
  page.buttonColor = () => {};
  page.saveLastUsedAddress = () => {};
  page._redirectToHome = () => {};
  const autosave = data => manager.saveDraft(data);
  autosave.cancel = () => {};
  page.debouncedSaveDraft = autosave;
  page.setData({ category: [{ id: 3 }], showCategory: { id: 3 }, showPickUp: { id: 1 }, showRecive: { id: 2 }, showUser: '测试用户', showPhone: '13800138000', note: '取快递', fileList: [{ mediaId: 1, status: 'done' }] });
  const restore = draft => {
    state.storage[constants.CACHE_KEYS.ORDER_DRAFT] = { timestamp: state.now, ...draft };
    assert.equal(page._tryRestoreDraft(), true);
    state.modal.success({ confirm: true });
  };
  return { page, state, manager, restore };
}

test('selected absolute deadline is saved, restored and submitted unchanged', async () => {
  const { page, state, restore } = harness(at(27, 15));
  page.selectTime({ currentTarget: { dataset: { time: { time: at(27, 16, 30), displayTime: '16:30' } } } });
  const saved = state.storage[constants.CACHE_KEYS.ORDER_DRAFT];
  assert.equal(saved.selectedTime, at(27, 16, 30));
  assert.equal(saved.version, '1.1');
  state.now = at(27, 15, 40);
  page.setData({ selectedTime: at(27, 16) });
  restore(saved);
  assert.equal(page.data.selectedTime, at(27, 16, 30));
  assert.equal(page.data.showReachTime, '今天 16:30');
  assert.equal(page.data.gapReach, 50);
  await page._apiPostOrder();
  assert.equal(state.requests[0].expectedDeliveryTime, '2026-09-27 16:30:00');
});

test('expired and legacy drafts never inherit the default deadline or infer one from relative fields', async () => {
  for (const draft of [
    { selectedTime: at(27, 14) },
    { selectedTime: at(27, 15) },
    { gapReach: 30, showReachTime: '明天 16:30' },
    { selectedTime: 'invalid' },
    { selectedTime: Infinity },
    { selectedTime: 1e20 },
  ]) {
    const { page, state, restore } = harness(at(27, 15));
    await page._generateTimeSlots(true);
    assert.equal(page.data.selectedTime, at(27, 15, 30));
    restore({ version: '1.0', note: '旧草稿内容', showCategory: { id: 3 }, ...draft });
    assert.equal(page.data.note, '旧草稿内容');
    assert.equal(page.data.selectedTime, null);
    assert.equal(page.data.showReachTime, '');
    await page.tapOnReachTime();
    assert.equal(page.data.selectedTime, null);
    await page.order();
    assert.equal(state.messages.at(-1), '请重新选择送达时间');
    assert.equal(page.data.showOrderConfirm, false);
    assert.equal(state.requests.length, 0);
  }
});

test('legacy absolute timestamp is accepted but stored labels and popup state are ignored', () => {
  const { page, restore } = harness(at(27, 15));
  restore({ version: '1.0', selectedTime: String(at(28, 6)), showReachTime: '今天 6:00', gapReach: 20, showOrderConfirm: true });
  assert.equal(page.data.selectedTime, at(28, 6));
  assert.equal(page.data.showReachTime, '明天 6:00');
  assert.equal(page.data.activeTab, 'tomorrow');
  assert.equal(page.data.showOrderConfirm, false);
});

test('cross-day recovery derives today from the original timestamp and preserves submitted date', async () => {
  const { page, state, restore } = harness(at(28, 0, 10));
  restore({ selectedTime: at(28, 6), showReachTime: '明天 6:00', showCategory: { id: 3 } });
  await page.tapOnReachTime();
  assert.equal(page.data.showReachTime, '今天 6:00');
  assert.equal(page.data.selectedTime, at(28, 6));
  assert.equal(page.data.activeTab, 'today');
  await page._apiPostOrder();
  assert.equal(state.requests[0].expectedDeliveryTime, '2026-09-28 06:00:00');
});

test('waiting in confirmation preserves a valid deadline and rejects one expired during authorization', async () => {
  const { page, state, restore } = harness(at(27, 15));
  restore({ selectedTime: at(27, 15, 30), showCategory: { id: 3 }, note: '帮我取快递', fileList: [{ mediaId: 1, status: 'done' }] });
  await page.order();
  assert.equal(page.data.showOrderConfirm, true);
  state.now = at(27, 15, 10);
  assert.equal(page.data.selectedTime, at(27, 15, 30));
  page._getSubscribeMessage = async () => { state.now = at(27, 15, 31); };
  await page.confirmOrder();
  assert.equal(page.data.selectedTime, null);
  assert.equal(page.data.showOrderConfirm, false);
  assert.equal(state.requests.length, 0);
  assert.match(state.messages.at(-1), /请重新选择/);
});

test('late initial slot generation cannot replace a restored missing or valid deadline', async () => {
  for (const selectedTime of [undefined, at(27, 16)]) {
    const { page, restore } = harness(at(27, 15));
    let resolve;
    page._generateTodayTimes = () => new Promise(yes => { resolve = yes; });
    const initializing = page._generateTimeSlots(true);
    restore({ selectedTime, showCategory: { id: 3 } });
    resolve([{ time: at(27, 15, 30), displayTime: '15:30' }]);
    await initializing;
    assert.equal(page.data.selectedTime, selectedTime ?? null);
  }
});

test('opening the picker after a selection expires does not silently choose a replacement', async () => {
  const { page, state, restore } = harness(at(27, 15));
  restore({ selectedTime: at(27, 15, 30) });
  state.now = at(27, 15, 31);
  await page.tapOnReachTime();
  assert.equal(page.data.selectedTime, null);
  assert.equal(page.data.showReachTime, '');
  page.setData({ isReachTimeVisiable: false });
  await assert.rejects(page._apiPostOrder(), /请重新选择/);
  assert.equal(state.requests.length, 0);
});

test('confirmation delay before expiry submits the restored original deadline', async () => {
  const { page, state, restore } = harness(at(27, 15));
  restore({ selectedTime: at(27, 15, 30), showCategory: { id: 3 }, note: '帮我取快递', fileList: [{ mediaId: 1, status: 'done' }] });
  await page.order();
  state.now = at(27, 15, 10);
  page._getSubscribeMessage = async () => {};
  await page.confirmOrder();
  assert.equal(state.requests.length, 1);
  assert.equal(state.requests[0].expectedDeliveryTime, '2026-09-27 15:30:00');
  assert.equal(state.requests[0].gap, 20);
});
