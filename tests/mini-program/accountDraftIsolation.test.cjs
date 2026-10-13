const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const constants = require('../../apps/mini-program/utils/orderConstants');
const feedbackStub = require('./helpers/feedbackStub.cjs');
const root = path.resolve(__dirname, '../../apps/mini-program');

function harness() {
  const state = { now: Date.now(), storage: new Map(), timers: new Map(), nextTimer: 0, modal: null };
  const app = { globalData: {} };
  const modules = new Map();
  class Clock extends Date {
    constructor(...args) { super(...(args.length ? args : [state.now])); }
    static now() { return state.now; }
  }
  const wx = {
    getStorageSync: key => state.storage.get(key),
    setStorageSync: (key, value) => state.storage.set(key, structuredClone(value)),
    removeStorageSync: key => state.storage.delete(key),
    showModal: options => { state.modal = options; },
  };
  function load(file) {
    if (modules.has(file)) return modules.get(file);
    const module = { exports: {} }; modules.set(file, module.exports);
    vm.runInNewContext(fs.readFileSync(path.join(root, file), 'utf8'), {
      module, wx, Date: Clock, getApp: () => app, console: { log() {}, warn() {}, error() {} },
      setTimeout(fn) { const id = ++state.nextTimer; state.timers.set(id, fn); return id; },
      clearTimeout(id) { state.timers.delete(id); },
      require(id) { return load(path.posix.normalize(path.posix.join(path.posix.dirname(file), `${id}.js`))); },
    });
    modules.set(file, module.exports); return module.exports;
  }
  const tokens = load('utils/tokenManager.js');
  const cache = load('utils/accountStorage.js');
  const drafts = load('utils/orderDraftManager.js');
  let page;
  vm.runInNewContext(fs.readFileSync(path.join(root, 'pages/orders/myOrders/ordersAdd/add.js'), 'utf8'), {
    wx, Date: Clock, getApp: () => app, console: { log() {}, warn() {}, error() {} },
    Page(config) { page = { ...config, data: structuredClone(config.data), setData(values, cb) { Object.assign(this.data, values); cb?.(); } }; },
    require(id) {
      if (id.endsWith('tokenManager')) return tokens;
      if (id.endsWith('accountStorage')) return cache;
      if (id.endsWith('orderDraftManager')) return drafts;
      if (id.endsWith('orderConstants')) return constants;
      if (id.endsWith('feedback')) return feedbackStub(wx);
      if (id.endsWith('privacy')) return { maskPhone: value => value };
      if (id.endsWith('addressService')) return { getOptionalAddress: async () => null };
      if (id.endsWith('commonJs')) return { checkCilcleToast() {} };
      return {};
    },
  });
  page._setDateLabels = () => {}; page._generateTimeSlots = () => {}; page.buttonColor = () => {};
  const login = id => { tokens.updateToken(`token-${id}`, { id }); page._draftSession = cache.captureSession(); };
  const runTimers = () => { for (const [id, fn] of [...state.timers]) { state.timers.delete(id); fn(); } };
  return { state, app, tokens, cache, drafts, page, login, runTimers };
}

test('账号 B 不读取 A 草稿、最近地址或旧的无归属缓存', () => {
  const h = harness(); h.login(1);
  assert.equal(h.drafts.saveDraft({ showPhone: 'A_PHONE', note: 'A_NOTE' }), true);
  h.page.saveLastUsedAddress(11, 12);
  assert.equal(h.page.getLastUsedAddress().pickUpId, 11);
  h.state.storage.set(constants.CACHE_KEYS.ORDER_DRAFT, { note: 'OLD_PRIVATE_NOTE' });
  h.login(2);
  assert.equal(h.drafts.loadDraft(), null); assert.equal(h.page.getLastUsedAddress(), null);
  assert.equal(h.state.storage.has(constants.CACHE_KEYS.ORDER_DRAFT), false);
  h.state.storage.set('orderDraft:2', { ownerId: '1', timestamp: h.state.now, note: 'A_NOTE' });
  assert.equal(h.drafts.loadDraft(), null);
  h.login(1); assert.equal(h.drafts.loadDraft().note, 'A_NOTE');
});

test('换号和退出后延迟保存不写进新账号或重新登录的账号', () => {
  const h = harness(); h.login(1);
  const save = h.drafts.createDebouncedSave(); save({ note: 'A_NOTE' });
  h.login(2); h.runTimers();
  assert.equal(h.state.storage.has('orderDraft:2'), false); assert.equal(h.state.storage.has('orderDraft:1'), false);
  h.login(1); save({ note: 'STALE_A_NOTE' });
  h.tokens.clearToken(); h.login(1); h.runTimers();
  assert.equal(h.drafts.loadDraft(), null);
});

test('主动退出清理账号私有缓存；过期保留隔离草稿且拒绝恢复或写入', () => {
  const h = harness(); h.login(1); h.drafts.saveDraft({ note: 'A_NOTE' }); h.page.saveLastUsedAddress(11, 12);
  h.state.now += 21 * 60 * 60 * 1000;
  assert.equal(h.drafts.loadDraft(), null); assert.equal(h.drafts.saveDraft({ note: 'EXPIRED' }), false);
  h.tokens.clearToken({ preserveLoginIntent: true });
  h.login(2); assert.equal(h.drafts.loadDraft(), null);
  h.login(1); assert.equal(h.drafts.loadDraft().note, 'A_NOTE');
  h.tokens.clearToken();
  assert.equal(h.state.storage.has('orderDraft:1'), false); assert.equal(h.state.storage.has('lastUsedOrderAddress:1'), false);
});

test('同账号续签不丢草稿；页面换号立即清空私有字段并取消保存', () => {
  const h = harness(); h.login(1); h.page.onLoad();
  h.page.setData({ showPhone: 'A_PHONE', showUser: 'A_NAME', note: 'A_NOTE', showPickUp: { id: 11 }, fileList: [{ mediaId: 3 }] });
  h.page._saveDraftData();
  const version = h.tokens.getSessionVersion();
  h.tokens.updateToken('renewed-token', { id: 1 });
  assert.equal(h.tokens.getSessionVersion(), version); assert.equal(h.page.data.note, 'A_NOTE');
  h.login(2); h.runTimers();
  assert.equal(h.page.data.note, ''); assert.equal(h.page.data.showPhone, null);
  assert.equal(h.page.data.showPickUp, null); assert.equal(h.page.data.fileList.length, 0);
  assert.equal(h.drafts.loadDraft(), null);
});

test('恢复弹窗迟到确认不能恢复其他账号资料，地址仅从当前账号地址簿解析', async () => {
  const h = harness(); h.login(1);
  h.drafts.saveDraft({ note: 'A_NOTE', showPickUp: { id: 11, details: 'OLD_ADDRESS' }, showRecive: { id: 12 } });
  h.page.setData({ addrList: [{ id: 11, details: 'CURRENT_ADDRESS' }], category: [] });
  assert.equal(h.page._tryRestoreDraft(), true); await h.state.modal.success({ confirm: true });
  assert.equal(h.page.data.showPickUp.details, 'CURRENT_ADDRESS'); assert.equal(h.page.data.showRecive, undefined);
  h.page.setData({ hasLoadedDraft: false }); h.page._tryRestoreDraft();
  const pending = h.state.modal; h.login(2); h.page.setData({ note: 'B_NOTE' }); await pending.success({ confirm: true });
  assert.equal(h.page.data.note, 'B_NOTE');
});

test('离页刷新最后一次草稿，注销监听后不再修改已卸载页面', () => {
  const h = harness(); h.login(1); h.page.onLoad(); h.page.setData({ note: 'LATEST_NOTE' });
  h.page._saveDraftData(); h.page.onUnload();
  assert.equal(h.drafts.loadDraft().note, 'LATEST_NOTE');
  h.login(2); assert.equal(h.page.data.note, 'LATEST_NOTE');
});
