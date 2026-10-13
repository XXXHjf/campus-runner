const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
const source = fs.readFileSync(path.resolve(__dirname,
  '../../apps/mini-program/pages/second-hand/detail/detail.js'), 'utf8');

function harness({ pending = [], accepted = [], confirm = true, failRead = false, failSend = false } = {}) {
  let page;
  const state = { session: 'a', dialogs: [], sent: [], notices: [], buying: 0, queries: [] };
  vm.runInNewContext(source, {
    Page(config) { page = { ...config, data: structuredClone(config.data) }; },
    require(name) {
      if (name.endsWith('accountStorage')) return {
        captureSession: () => state.session, isCurrent: session => state.session === session,
      };
      if (name.endsWith('feedback')) return {
        showToast: (_, options) => state.notices.push(options.title),
        showModal: async (_, options) => { state.dialogs.push(options); return { confirm }; },
      };
      if (name.endsWith('secondHandService')) return {
        listProductBargains: async (_, query) => {
          state.queries.push(query);
          if (failRead) throw Error('network');
          return query.status === 1 ? accepted : pending;
        },
        createBargain: async data => {
          state.sent.push(data);
          if (failSend) throw Error('network');
        },
      };
      if (name.endsWith('subscriptionService')) return { requestSecondHandOrder: async () => {} };
      return {};
    },
  });
  page.setData = patch => Object.assign(page.data, patch);
  page.getCurrentUser = async () => ({ id: 7 });
  page.errorText = (_, fallback) => fallback;
  page.buyNow = async () => { state.buying++; };
  Object.assign(page.data, { product: { id: 10, price: 100 }, bargainPrice: '80', bargainMessage: '自提', showBargain: true });
  return { page, state };
}

const previous = { id: 1, buyerId: 7, status: 0, offerPrice: 70, message: '自提' };

test('replacement warns before sending and clears draft only after success', async () => {
  const { page, state } = harness({ pending: [previous] });
  await page.submitBargain();
  assert.match(state.dialogs[0].content, /发送后，之前的议价将失效/);
  assert.equal(state.dialogs[0].cancelText, '取消');
  assert.equal(state.sent.length, 1);
  assert.equal(page.data.showBargain, false);
  assert.equal(page.data.bargainSubmitting, false);
  assert.deepEqual(state.queries.map(query => query.status), [1, 0]);
});

test('cancel keeps draft and sends nothing', async () => {
  const { page, state } = harness({ pending: [previous], confirm: false });
  await page.submitBargain();
  assert.equal(state.sent.length, 0);
  assert.equal(page.data.bargainPrice, '80');
  assert.equal(page.data.showBargain, true);
  assert.equal(page.data.bargainSubmitting, false);
});

test('initial quote retains existing confirmation without replacement warning', async () => {
  const { page, state } = harness();
  await page.submitBargain();
  assert.doesNotMatch(state.dialogs[0].content, /失效/);
  assert.equal(state.sent.length, 1);
});

test('accepted quote guides checkout without sending or invalidating it', async () => {
  const { page, state } = harness({ accepted: [{ buyerId: 7, status: 1 }] });
  await page.submitBargain();
  assert.equal(state.buying, 1);
  assert.equal(state.sent.length, 0);
  assert.equal(state.dialogs.length, 0);
});

test('query failure stops sending and send failure preserves draft', async () => {
  for (const options of [{ failRead: true }, { failSend: true, pending: [previous] }]) {
    const { page, state } = harness(options);
    await page.submitBargain();
    assert.equal(page.data.showBargain, true);
    assert.equal(page.data.bargainMessage, '自提');
    assert.equal(page.data.bargainSubmitting, false);
    if (options.failRead) assert.equal(state.sent.length, 0);
  }
});

test('double click and account switch during confirmation cannot send a second or stale quote', async () => {
  const { page, state } = harness({ pending: [previous] });
  const first = page.submitBargain();
  await page.submitBargain();
  await first;
  assert.equal(state.sent.length, 1);
  const other = harness();
  other.page.getCurrentUser = async () => { other.state.session = 'b'; return { id: 8 }; };
  await other.page.submitBargain();
  assert.equal(other.state.sent.length, 0);
  assert.equal(other.state.dialogs.length, 0);
});
