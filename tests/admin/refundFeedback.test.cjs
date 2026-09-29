const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const ts = require('../../apps/admin/node_modules/typescript');
const source = fs.readFileSync(path.resolve(__dirname, '../../apps/admin/src/utils/format.ts'), 'utf8');
const moduleExports = {};
vm.runInNewContext(ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText,
  { exports: moduleExports });

test('refund feedback distinguishes intent, processing, success and failure', () => {
  const action = (refundStatus) => moduleExports.formatOrderActionResult({ orderStatus: -2, refundStatus });
  assert.equal(action('REQUESTED').content, '申请已受理，请稍后查看退款状态');
  assert.equal(action('PROCESSING').type, 'info');
  assert.equal(action('SUCCESS').content, '退款已到账');
  assert.equal(action('REQUEST_FAILED').type, 'error');
  assert.equal(action('ABNORMAL').type, 'error');
  assert.equal(moduleExports.formatOrderActionResult({ orderStatus: 4, refundStatus: null }).content, '订单已取消');
  assert.equal(moduleExports.formatOrderActionResult('ok').type, 'error');
});
