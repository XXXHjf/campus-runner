const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const test = require('node:test');

const source = fs.readFileSync(path.resolve(__dirname, '../../apps/mini-program/services/userOrderService.js'), 'utf8');

test('编辑、确认、取消和退款按业务码判断结果', async () => {
  let response = { data: { code: 1 } };
  const module = { exports: {} };
  vm.runInNewContext(source, {
    module,
    console,
    getApp: () => ({ globalData: { API_URL: 'https://example.com' } }),
    require: (name) => name === './request'
      ? { request: async () => response }
      : { _delbefore1stBlank: (items) => items },
  });
  const actions = [
    () => module.exports.updateOrderContent(1, { note: '订单说明', imageAssetId: 2 }),
    () => module.exports.confirmOrder(1),
    () => module.exports.cancelOrder(1, '不需要了', 'order-1'),
    () => module.exports.refundOrder('order-1', '不需要了'),
  ];
  for (const action of actions) {
    await assert.doesNotReject(action());
    response = { data: { code: 0, msg: '操作失败' } };
    await assert.rejects(action(), /操作失败/);
    response = { data: { code: 1 } };
  }
});
