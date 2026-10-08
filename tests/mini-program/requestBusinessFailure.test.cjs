const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const test = require('node:test');

function service(file, response) {
  const root = path.resolve(__dirname, '../../apps/mini-program');
  const app = { globalData: { API_URL: 'https://example.test' } };
  const load = (file, require) => {
    const module = { exports: {} };
    vm.runInNewContext(fs.readFileSync(path.join(root, file), 'utf8'), {
      module, require, getApp: () => app,
      wx: { request: ({ success }) => success(response) },
    });
    return module.exports;
  };
  const request = load('services/request.js', () => ({ getToken: () => 'test', hasLoginIntent: () => false }));
  return load(`services/${file}.js`, id => id === './request' ? request : {});
}

for (const [file, action, payload] of [
  ['userService', 'updateUserInfo', { username: '同学' }],
  ['takeOrderService', 'updateTakeOrderStatus', { id: 1, status: 1 }],
  ['takeOrderService', 'acceptOrderById', 1],
]) {
  test(`${action}: HTTP 200 的业务失败不能被当成保存成功`, async () => {
    const api = service(file, { statusCode: 200, data: { code: 0, msg: '订单状态已变化，请刷新后重试' } });
    await assert.rejects(api[action](payload), /订单状态已变化/);
  });
  test(`${action}: 成功响应保留服务返回格式`, async () => {
    const body = { code: 1, data: { id: 1 } };
    const api = service(file, { statusCode: 200, data: body });
    assert.equal(await api[action](payload), body);
  });
}
