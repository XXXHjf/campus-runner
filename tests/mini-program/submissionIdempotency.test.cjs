const test = require('node:test');
const assert = require('node:assert/strict');
const vm = require('node:vm');
const fs = require('node:fs');
const path = require('node:path');
const source = fs.readFileSync(path.resolve(__dirname, '../../apps/mini-program/utils/submissionManager.js'), 'utf8');
function harness() {
  const state = { owner: '1', version: 1, storage: new Map(), failStorage: false };
  const accounts = {
    captureSession: () => ({ ownerId: state.owner, version: state.version }),
    isCurrent: s => !!s.ownerId && s.ownerId === state.owner && s.version === state.version,
    read: (key, s) => accounts.isCurrent(s) ? structuredClone(state.storage.get(`${s.ownerId}:${key}`)) : null,
    write: (key, value, s) => {
      if (state.failStorage) throw new Error('storage full');
      if (!accounts.isCurrent(s)) return false;
      state.storage.set(`${s.ownerId}:${key}`, structuredClone(value)); return true;
    },
  };
  const reload = () => {
    const module = { exports: {} };
    vm.runInNewContext(source, { module, require: () => accounts });
    return module.exports;
  };
  return { state, reload, manager: reload() };
}
test('lost response keeps the original payload and key across application restart', async () => {
  const { manager, reload } = harness();
  let sent;
  await assert.rejects(manager.submit('RUNNER_ORDER', { gap: 10, images: [1] }, async p => { sent = p; throw new Error('timeout'); }));
  assert.match(sent.clientRequestId, /^[A-Za-z0-9_-]{16,64}$/);
  const restarted = reload();
  const pending = restarted.getPending('RUNNER_ORDER');
  assert.equal(pending.clientRequestId, sent.clientRequestId);
  await restarted.submit('RUNNER_ORDER', pending, async p => {
    assert.equal(JSON.stringify(p), JSON.stringify(sent)); return { id: 1 };
  });
  assert.equal(restarted.getPending('RUNNER_ORDER'), null);
});
test('uncertain response retains the key; only explicit rejection permits a fresh attempt', async () => {
  const { manager } = harness();
  await assert.rejects(manager.submit('SECOND_HAND_PRODUCT', { title: 'book' }, async () => { throw new Error('missing data'); }));
  const first = manager.getPending('SECOND_HAND_PRODUCT').clientRequestId;
  const rejected = new Error('rejected'); rejected.requestOutcome = 'rejected';
  await assert.rejects(manager.submit('SECOND_HAND_PRODUCT', { title: 'book' }, async p => {
    assert.equal(p.clientRequestId, first); throw rejected;
  }));
  assert.equal(manager.getPending('SECOND_HAND_PRODUCT'), null);
  await manager.submit('SECOND_HAND_PRODUCT', { title: 'new' }, async p => {
    assert.notEqual(p.clientRequestId, first); return { id: 2 };
  });
});
test('changed fields cannot replace an uncertain submission', async () => {
  const { manager } = harness();
  await assert.rejects(manager.submit('RUNNER_ORDER', { gap: 10 }, async () => { throw new Error('timeout'); }));
  await assert.rejects(manager.submit('RUNNER_ORDER', { gap: 9 }, async () => assert.fail('must not send')), /上次提交/);
  assert.equal(manager.getPending('RUNNER_ORDER').gap, 10);
});
test('simultaneous identical calls share the same write', async () => {
  const { manager } = harness(); let finish, writes = 0;
  const send = async () => { writes++; return new Promise(resolve => { finish = resolve; }); };
  const first = manager.submit('RUNNER_ORDER', { title: 'book' }, send);
  const second = manager.submit('RUNNER_ORDER', { title: 'book' }, send);
  await Promise.resolve(); finish({ id: 1 }); await Promise.all([first, second]);
  assert.equal(writes, 1);
});
test('storage failure fails before sending a write', async () => {
  const { manager, state } = harness(); state.failStorage = true;
  await assert.rejects(manager.submit('RUNNER_ORDER', {}, async () => assert.fail('must not send')), /storage/);
});
test('owner changes hide pending payloads and stale success cannot clear them', async () => {
  const { manager, state } = harness(); let finish;
  const pending = manager.submit('RUNNER_ORDER', { phone: 'private' }, async () => new Promise(resolve => { finish = resolve; }));
  await Promise.resolve(); state.owner = '2'; state.version++;
  assert.equal(manager.getPending('RUNNER_ORDER'), null);
  finish({ id: 1 }); await pending;
  state.owner = '1'; state.version++;
  assert.equal(manager.getPending('RUNNER_ORDER').phone, 'private');
});
test('logout before dispatch prevents a write with the next account token', async () => {
  const { manager, state } = harness();
  const pending = manager.submit('RUNNER_ORDER', {}, async () => assert.fail('must not send'));
  state.owner = null; state.version++;
  await assert.rejects(pending, /登录已变化/);
});
