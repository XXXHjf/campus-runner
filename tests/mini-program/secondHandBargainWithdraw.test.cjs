const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const test = require('node:test');
const statusTools = require('../../apps/mini-program/utils/secondHandStatus');

function loadPage({ fail = false, refreshFail = false } = {}) {
  let page;
  let modal;
  let decide;
  const calls = [];
  vm.runInNewContext(fs.readFileSync(path.resolve(__dirname,
    '../../apps/mini-program/pages/second-hand/bargains/bargains.js'), 'utf8'), {
    require(name) {
      if (name.endsWith('secondHandStatus')) return statusTools;
      if (name.endsWith('feedback')) return {
        showModal: (_, options) => { modal = options; return new Promise(resolve => { decide = resolve; }); },
        showToast: (_, options) => calls.push(options.title),
      };
      if (name.endsWith('secondHandService')) return {
        withdrawBargain: async (id) => {
          calls.push(id);
          if (fail) throw new Error('卖家已接受，请查看订单');
        },
      };
      return {};
    },
    Page(config) { page = config; },
  });
  page.data.issued = [{ id: 7, isBuyer: true, status: 0 }];
  page.setData = (patch) => Object.assign(page.data, patch);
  page.loadList = async () => {
    calls.push('refresh');
    if (fail && !refreshFail) page.data.issued[0].status = 1;
  };
  return { page, calls, modal: () => modal, confirm: (result) => decide(result) };
}

const event = { currentTarget: { dataset: { id: 7 } } };

test('withdraw requires confirmation, keeps record and removes pending state', async () => {
  const { page, calls, modal, confirm } = loadPage({ refreshFail: true });
  let action = page.withdraw(event);
  assert.equal(modal().confirmText, '确认撤回');
  confirm({ confirm: false });
  await action;
  assert.equal(calls.length, 0);
  action = page.withdraw(event);
  confirm({ confirm: true });
  await action;
  assert.equal(page.data.issued.length, 1);
  assert.equal(page.data.issued[0].status, 4);
  assert.equal(page.data.issued[0].statusText, '已撤回');
  assert.deepEqual(calls, [7, '已撤回', 'refresh']);
  assert.equal(page.data.actionLoadingId, null);
});

test('accepted race reports failure and refreshes actual record instead of claiming withdrawal', async () => {
  const { page, calls, confirm } = loadPage({ fail: true });
  const action = page.withdraw(event);
  confirm({ confirm: true });
  await action;
  assert.deepEqual(calls, [7, '卖家已接受，请查看订单', 'refresh']);
  assert.equal(page.data.issued[0].status, 1);
  assert.equal(page.data.actionLoadingId, null);
});

test('other users, terminal records and busy submissions cannot open withdrawal', () => {
  for (const record of [{ isBuyer: false, status: 0 }, { isBuyer: true, status: 1 },
    { isBuyer: true, status: 2 }, { isBuyer: true, status: 3 }, { isBuyer: true, status: 4 }]) {
    const { page, modal } = loadPage();
    Object.assign(page.data.issued[0], record);
    page.withdraw(event);
    assert.equal(modal(), undefined);
  }
  const { page, modal } = loadPage();
  page.data.actionLoadingId = 7;
  page.withdraw(event);
  assert.equal(modal(), undefined);
});

test('withdraw service rejects business failures returned with HTTP success', async () => {
  let service;
  let sent;
  const context = {
    getApp: () => ({ globalData: { API_URL: 'https://example.invalid' } }),
    module: { exports: {} },
    require: () => ({ request: async (options) => {
      sent = options;
      return { data: { code: 0, msg: '卖家已接受，请查看订单' } };
    } }),
  };
  vm.runInNewContext(fs.readFileSync(path.resolve(__dirname,
    '../../apps/mini-program/services/secondHandService.js'), 'utf8'), context);
  service = context.module.exports;
  await assert.rejects(service.withdrawBargain(7), /卖家已接受/);
  assert.equal(sent.method, 'POST');
  assert.ok(sent.url.endsWith('/bargains/7/withdraw'));
});
