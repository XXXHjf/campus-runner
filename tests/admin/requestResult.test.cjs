const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const ts = require('../../apps/admin/node_modules/typescript');
const source = fs.readFileSync(path.resolve(__dirname, '../../apps/admin/src/services/request.ts'), 'utf8');
function harness(body) {
  let onResponse;
  const instance = {
    interceptors: { request: { use() {} }, response: { use(fn) { onResponse = fn; } } },
    async request(config) { return onResponse({ status: 200, data: body, config }); },
  };
  const exports = {};
  vm.runInNewContext(ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, {
    exports, console,
    require(name) {
      if (name === 'axios') return { default: { create: () => instance } };
      if (name.includes('config')) return { default: { apiBaseUrl: '', requestTimeout: 15000, enableRequestLog: false } };
      if (name.includes('token')) return { tokenManager: {} };
      if (name.includes('constants')) return { HTTP_STATUS: { OK: 200 }, ERROR_MESSAGES: { UNKNOWN_ERROR: '未知错误' } };
      throw new Error(name);
    },
  });
  return exports;
}
for (const code of [0, '0']) {
  test(`业务失败 ${code} 必须拒绝并保留失败原因`, async () => {
    await assert.rejects(harness({ code, msg: '退款尚未确认', data: null }).post('/admin/api/orders/1/cancel', {}), err => err.message === '退款尚未确认');
  });
}
test('失败码不能因存在 data 而变成成功', async () => {
  await assert.rejects(harness({ code: 0, msg: '失败', data: { id: 1 } }).post('/orders', {}));
});
test('成功码 1 继续返回业务数据', async () => {
  assert.equal(await harness({ code: 1, data: 'ok' }).post('/orders', {}), 'ok');
});
