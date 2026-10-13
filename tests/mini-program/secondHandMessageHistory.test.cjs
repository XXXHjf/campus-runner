const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const source = fs.readFileSync(path.resolve(__dirname, '../../apps/mini-program/pages/second-hand/conversation/conversation.js'), 'utf8');
const feedback = require('./helpers/feedbackStub.cjs');
const defer = () => { let resolve; const promise = new Promise(yes => { resolve = yes; }); return { promise, resolve }; };
const row = id => ({ id, productId: 30, senderId: id % 2 ? 20 : 10, receiverId: id % 2 ? 10 : 20, createTime: '2026-10-11 12:00:00', content: `message ${id}` });

function harness() {
  let page;
  const state = { token: 'a', session: 1, count: 65, queries: [], productReads: 0, orderReads: 0, failed: false, pending: null, sendPending: null, notifications: 0 };
  const app = { globalData: { userInfo: { id: 10 } }, refreshMineTabRedDot: async () => {} };
  const service = {
    getProduct: async () => { state.productReads++; return { id: 30, title: 'item', sellerId: 20 }; },
    getOrderDetail: async () => { state.orderReads++; return { counterpartyPhone: '13800000000' }; },
    listConversationMessages: async (_, __, q) => {
      state.queries.push({ ...q });
      if (state.pending) return state.pending.promise;
      if (state.failed) throw Error('network');
      let rows = Array.from({ length: state.count }, (_, i) => row(i + 1));
      if (q.beforeId) rows = rows.filter(r => r.id < q.beforeId);
      if (q.afterId) return rows.filter(r => r.id > q.afterId).slice(0, q.pageSize);
      return rows.slice(-q.pageSize);
    },
    sendMessage: async () => state.sendPending ? state.sendPending.promise : row(++state.count),
  };
  vm.runInNewContext(source, {
    console, setInterval: () => 1, clearInterval() {}, getApp: () => app,
    wx: { stopPullDownRefresh() {}, setNavigationBarTitle() {}, getStorageSync: () => ({}), setStorageSync() {} },
    require: id => id.endsWith('feedback') ? feedback() : id.endsWith('secondHandService') ? service
      : id.endsWith('tokenManager') ? { getToken: () => state.token, getSessionVersion: () => state.session,
        onSessionChange: fn => { state.sessionChange = fn; return () => { state.sessionChange = null; }; } }
        : id.endsWith('subscriptionService') ? { requestSecondHandMessage: async () => { state.notifications++; } }
          : id.endsWith('userService') ? { getUserInfo: async () => ({ id: 10 }) } : { friendlyError: (_, fallback) => fallback },
    Page: value => { page = { ...value, data: structuredClone(value.data), setData: update => Object.assign(page.data, update) }; },
  });
  page.onLoad({ productId: '30', counterpartyId: '20', orderId: '50' });
  return { page, state };
}

test('latest page and upward pagination reach all 65 messages in chronological order', async () => {
  const { page, state } = harness(); await page.loadConversation();
  assert.deepEqual(Array.from(page.data.messages, r => r.id), Array.from({ length: 20 }, (_, i) => i + 46));
  for (let i = 0; i < 5; i++) await page.loadOlderMessages();
  assert.equal(page.data.messages.length, 65); assert.equal(page.data.hasOlder, false);
  assert.equal(state.queries.length, 4); assert.equal(state.productReads, 1); assert.equal(state.orderReads, 1);
  assert.equal(page.data.lastMessageAnchor, 'message-6');
});

test('incremental polling catches a multi-page backlog without gaps or metadata reads', async () => {
  const { page, state } = harness(); await page.loadConversation(); state.count = 110;
  for (let i = 0; i < 3; i++) await page.loadConversation(false);
  assert.deepEqual(Array.from(page.data.messages, r => r.id), Array.from({ length: 65 }, (_, i) => i + 46));
  assert.deepEqual(state.queries.slice(1).map(q => q.afterId), [65,85,105]);
  assert.equal(state.productReads, 1); assert.equal(state.orderReads, 1);
  assert.equal(page.data.hasOlder, true);
});

test('an initially empty conversation can reach an entire backlog arriving before the next poll', async () => {
  const { page, state } = harness(); state.count = 0; await page.loadConversation();
  assert.equal(page.data.hasOlder,false); state.count = 60; await page.loadConversation(false);
  assert.equal(page.data.hasOlder,true);
  for (let i=0;i<4;i++) await page.loadOlderMessages();
  assert.equal(page.data.messages.length,60);
});

test('failed history fetch preserves cursor and records, retry uses the same cursor', async () => {
  const { page, state } = harness(); await page.loadConversation(); state.failed = true;
  assert.equal(await page.loadOlderMessages(), false); assert.equal(page.data.messages.length, 20);
  state.failed = false; await page.loadOlderMessages(); assert.equal(page.data.messages.length, 40);
  assert.deepEqual(state.queries.slice(1).map(q => q.beforeId), [46,46]);
});

test('simultaneous history and poll merge safely and duplicate requests are shared', async () => {
  const { page, state } = harness(); await page.loadConversation(); state.pending = defer();
  const older = page.loadOlderMessages(); const duplicate = page.loadOlderMessages();
  assert.equal(older, duplicate); const poll = page.loadConversation(false);
  state.pending.resolve([row(45), row(65), row(66)]); await Promise.all([older, poll]);
  assert.equal(new Set(page.data.messages.map(r => r.id)).size, page.data.messages.length);
  assert.equal(page.data.messages.at(-1).id, 66);
});

test('refresh supersedes pending history and preserves the fresh latest page', async () => {
  const { page, state } = harness(); await page.loadConversation(); const deferred = defer(); state.pending = deferred;
  const older = page.loadOlderMessages(); state.pending = null; await page.onPullDownRefresh();
  deferred.resolve([row(1)]); await older; assert.equal(page.data.messages.length,20); assert.equal(page.data.messages[0].id,46);
});

test('session change clears private state immediately and rejects old responses', async () => {
  const { page, state } = harness(); await page.loadConversation(); const deferred = defer(); state.pending = deferred;
  const older = page.loadOlderMessages(); page.setMessage({ detail: { value: 'private draft' } });
  state.token = 'b'; state.session++; state.sessionChange();
  assert.equal(page.data.messages.length,0); assert.equal(page.data.messageContent,''); assert.equal(page.data.counterpartyPhone,'');
  deferred.resolve([row(1)]); await older; assert.equal(page.data.messages.length,0);
});

test('hide and unload reject late responses, logout performs no history request', async () => {
  for (const action of ['onHide','onUnload']) {
    const { page, state } = harness(); await page.loadConversation(); state.pending = defer();
    const poll = page.loadConversation(false); page[action](); state.pending.resolve([row(999)]); await poll;
    assert.equal(page.data.messages.some(r => r.id===999),false);
  }
  const { page, state } = harness(); state.token = null; await page.loadConversation(); assert.equal(state.queries.length,0);
});

test('sent message is visible immediately without advancing past an unfetched backlog', async () => {
  const { page, state } = harness(); await page.loadConversation(); state.count = 95;
  page.setMessage({ detail: { value: 'hello' } }); await page.sendMessage();
  assert.equal(page.data.messages.at(-1).id,96); assert.equal(page._afterId,85); assert.equal(page.data.messageContent,'');
  await page.loadConversation(false);
  assert.deepEqual(Array.from(page.data.messages,r=>r.id),Array.from({length:51},(_,i)=>i+46));
});

test('late send cannot clear a new session draft or request a subscription', async () => {
  const { page, state } = harness(); await page.loadConversation(); state.sendPending = defer();
  page.setMessage({ detail: { value: 'hello' } }); const send = page.sendMessage();
  state.token = 'b'; state.session++; state.sessionChange(); page.setMessage({ detail: { value: 'new draft' } });
  state.sendPending.resolve(row(66)); await send;
  assert.equal(page.data.messageContent,'new draft'); assert.equal(page.data.messages.length,0); assert.equal(state.notifications,0);
});

test('legacy product and conversation service functions forward their cursor parameters', async () => {
  const calls=[]; let api;
  const serviceSource=fs.readFileSync(path.resolve(__dirname,'../../apps/mini-program/services/secondHandService.js'),'utf8');
  const module={exports:{}};
  vm.runInNewContext(serviceSource,{module,getApp:()=>({globalData:{API_URL:'https://example.test'}}),require:id=>id==='./request'
    ?{request:async q=>{calls.push(q);return q;},safeList:r=>r,requiredObject:r=>r}:{} }); api=module.exports;
  await api.listProductMessages(30,{beforeId:21,pageSize:50}); await api.listConversationMessages(30,20,{afterId:22});
  assert.equal(calls[0].data.beforeId,21); assert.equal(calls[0].data.pageSize,50); assert.equal(calls[1].data.afterId,22);
  assert.match(fs.readFileSync(path.resolve(__dirname,'../../apps/mini-program/pages/second-hand/conversation/conversation.wxml'),'utf8'),/bindscrolltoupper="loadOlderMessages"/);
});
