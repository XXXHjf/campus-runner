const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const test = require('node:test');

const source = fs.readFileSync(path.resolve(__dirname, '../../apps/mini-program/pages/orders/takeOrders/takesInfo/info.js'), 'utf8');

function createPage(wxOverrides = {}) {
  let page;
  const wx = {
    getAccountInfoSync: () => ({ miniProgram: { appId: 'test-app' } }),
    showToast: () => {},
    requestMerchantTransfer: ({ success }) => success({}),
    ...wxOverrides,
  };
  vm.runInNewContext(source, {
    console,
    wx,
    getApp: () => ({ globalData: { API_URL: 'https://example.test', MOCK_PAYMENT: false } }),
    require: (name) => name.includes('tokenManager') ? { getToken: () => 'test-token' }
      : name.includes('transformers') ? { showLoading: () => {}, hideLoading: () => {}, showError: () => {} }
        : name.includes('commonJs') ? { errorCilcleToast: () => {} }
          : {},
    Page(config) {
      page = {
        ...config,
        data: structuredClone(config.data),
        setData(value) { Object.assign(this.data, value); },
      };
    },
  });
  return page;
}

test('微信弹窗成功后等待服务端订单状态，不提前标记收款成功', async () => {
  let refreshed = 0;
  const page = createPage();
  page.data.id = 42;
  page.data.orderInfo = { status: 5 };
  page._apiTransfer = async () => ({ mchId: 'merchant', packageInfo: 'package' });
  page._loadOrderInfo = async () => { refreshed += 1; };
  await page._requestMerchantTransfer();
  assert.equal(page.data.orderInfo.status, 5);
  assert.equal(page.data.transferBusy, false);
  assert.equal(refreshed, 1);
});

test('收款接口失败响应不会打开微信收款弹窗', async () => {
  let opened = false;
  const page = createPage({
    request: ({ success }) => success({ statusCode: 200, data: { code: 0, msg: '失败' } }),
    requestMerchantTransfer: () => { opened = true; },
  });
  await assert.rejects(page._apiTransfer(42), /发起收款失败/);
  assert.equal(opened, false);
});
