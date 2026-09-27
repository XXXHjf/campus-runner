const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const test = require('node:test');
const statusTools = require('../../apps/mini-program/utils/secondHandStatus');

function loadPage(role) {
  const file = role === 'buyer' ? 'orders/orders.js' : 'seller-orders/seller-orders.js';
  let page;
  vm.runInNewContext(fs.readFileSync(path.resolve(__dirname,
    '../../apps/mini-program/pages/second-hand', file), 'utf8'), {
    require: (name) => name.endsWith('secondHandStatus') ? statusTools : {},
    Page: (config) => { page = config; },
  });
  return page;
}

const onlineGroups = [
  'processing', 'processing', 'processing', 'completed', 'canceled',
  'refund', 'refund', 'refund', 'processing', 'completed', 'processing', 'processing',
];
const offlineGroups = new Map([[1, 'processing'], [2, 'processing'],
  [3, 'completed'], [4, 'canceled'], [11, 'processing']]);

for (const role of ['buyer', 'seller']) {
  test(`${role}: 所有状态按交易模式分组，历史退款保持可见`, () => {
    const page = loadPage(role);
    for (const tradeMode of ['OFFLINE', 'offline', 'ONLINE', 'online', undefined, '']) {
      const offline = String(tradeMode || '').toUpperCase() === 'OFFLINE';
      for (let status = 0; status <= 11; status += 1) {
        for (const value of [status, String(status)]) {
          const expected = offline ? offlineGroups.get(status) || 'all' : onlineGroups[status];
          const order = page.decorateOrder({ id: 1, status: value, tradeMode });
          assert.equal(order.statusGroup, expected, `${tradeMode}/${value}`);
          assert.equal(order.isRefund, !offline && [5, 6, 7].includes(status));
          assert.ok(page.data.filterOptions.some((option) => option.value === expected));
          assert.equal(page.filterOrders([order], 'all', '').length, 1);
          for (const group of ['processing', 'completed', 'canceled', 'refund']) {
            assert.equal(page.filterOrders([order], group, '').length,
              expected === group ? 1 : 0, `${tradeMode}/${value}/${group}`);
          }
        }
      }
    }
    for (const status of [-1, 12, 'unknown', undefined]) {
      const order = page.decorateOrder({ status, tradeMode: 'ONLINE' });
      assert.equal(order.statusGroup, 'all');
      assert.equal(page.filterOrders([order], 'all', '').length, 1);
      assert.equal(page.filterOrders([order], 'refund', '').length, 0);
    }
  });

  test(`${role}: 协商中可在进行中结合关键词找到，保留警示且不开放取消`, () => {
    const page = loadPage(role);
    const orders = [
      page.decorateOrder({ id: 1, status: '11', tradeMode: 'OFFLINE', productTitle: '旧书' }),
      page.decorateOrder({ id: 2, status: 5, tradeMode: 'ONLINE', productTitle: '旧书' }),
    ];
    assert.deepEqual([...page.filterOrders(orders, 'processing', '旧 书').map((order) => order.id)], [1]);
    assert.deepEqual([...page.filterOrders(orders, 'refund', '旧书').map((order) => order.id)], [2]);
    assert.equal(orders[0].statusText, '协商中');
    assert.equal(orders[0].statusKind, 'warning');
    assert.equal(orders[0].canCancel, false);
    assert.equal(orders[0].canContact, true);
  });
}
