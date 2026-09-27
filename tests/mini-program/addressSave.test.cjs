const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
const root = path.resolve(__dirname, '../../apps/mini-program');
function harness() {
  let saved = { id: 2, isDefault: 0, details: 'old', label: 'old' };
  let responseCode = 1, navigated = false, page;
  const writes = [];
  const serviceModule = { exports: {} };
  vm.runInNewContext(fs.readFileSync(path.join(root, 'services/addressService.js'), 'utf8'), {
    module: serviceModule, getApp: () => ({ globalData: { API_URL: '' } }),
    require: () => ({ request: async o => {
      if (o.method === 'PUT') { writes.push(o); if (responseCode === 1) saved = { ...saved, ...o.data }; }
      return { data: { code: responseCode, msg: '保存失败', data: [saved] } };
    } }),
  });
  vm.runInNewContext(fs.readFileSync(path.join(root, 'pages/address/addressInfo/info.js'), 'utf8'), {
    Page: p => { page = p; }, getApp: () => ({ globalData: {} }), console: { log() {}, error() {} },
    require: name => name.includes('addressService') ? serviceModule.exports : name.includes('transformers')
      ? { showLoading() {}, hideLoading() {}, showError() {} }
      : { loaded() {}, navigate() { navigated = true; } },
  });
  page.data = { ...page.data, addressInfo: { ...saved }, upDetails: '', upLabel: 'new', upDefault: 1,
    upCompusNum: null, upCategoryNum: null, upBuildingNum: null, id: 2 };
  page.setData = data => Object.assign(page.data, data);
  return { page, writes, saved: () => saved, navigated: () => navigated, fail: () => { responseCode = 0; } };
}
test('one write saves default and content; reloading restores saved values', async () => {
  const h = harness(); await h.page.b2();
  assert.equal(h.writes.length, 1); assert.equal(h.writes[0].url, '/api/address/update');
  assert.equal(h.navigated(), true);
  h.page.data.upDefault = 0; h.page.data.upDetails = 'stale';
  await h.page.getAddressInfo();
  assert.equal(h.page.data.upDefault, 1); assert.equal(h.page.data.upDetails, ''); assert.equal(h.page.data.upLabel, 'new');
  h.page.changeDefault(); assert.equal(h.page.data.upDefault, 1);
});
test('business failure does not navigate or change saved values', async () => {
  const h = harness(); h.fail(); await h.page.b2();
  assert.equal(h.navigated(), false); assert.equal(h.saved().isDefault, 0); assert.equal(h.saved().details, 'old');
});
