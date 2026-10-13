const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const test = require('node:test');

function service(file, response, calls = []) {
  const root = path.resolve(__dirname, '../../apps/mini-program');
  const app = { globalData: { API_URL: 'https://example.test' } };
  const load = (file, require) => {
    const module = { exports: {} };
    vm.runInNewContext(fs.readFileSync(path.join(root, file), 'utf8'), {
      module, require, getApp: () => app,
      wx: { request: options => { calls.push(options); options.success(response); } },
    });
    return module.exports;
  };
  const request = load('services/request.js', () => ({ getToken: () => 'test', hasLoginIntent: () => false }));
  return load(`services/${file}.js`, id => id === './request' ? request
    : id.endsWith('/submissionManager') ? { submit: (slot, data, send) => send(data) } : {});
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

test('only an explicit business rejection proves a write failed', async () => {
  for (const [response, outcome] of [
    [{ statusCode: 200, data: { code: 0, msg: '操作未完成' } }, 'rejected'],
    [{ statusCode: 200, data: {} }, undefined],
    [{ statusCode: 503, data: { code: 0 } }, undefined],
  ]) {
    const api = service('userService', response);
    await assert.rejects(api.updateUserInfo({ username: '同学' }), error => {
      assert.equal(error.requestOutcome, outcome);
      return true;
    });
  }
});

for (const [file, action, payload] of [
  ['userService', 'getUserInfo'],
  ['userOrderService', 'getMyOrderDetail', 1],
  ['userOrderService', 'previewOrderAmount', {}],
  ['secondHandService', 'getProduct', 1],
  ['secondHandService', 'getOrderDetail', 1],
  ['secondHandService', 'publishProduct', {}],
  ['secondHandService', 'createOrder', {}],
  ['deliveryAddressService', 'getAddressDetail', 1],
  ['addressService', 'getThreeLevelAddress'],
  ['addressService', 'getUserAddressDetail', 1],
]) {
  test(`${file}.${action}: missing data is rejected instead of an empty successful result`, async () => {
    for (const data of [undefined, null, {}, [], 'unexpected']) {
      const api = service(file, { statusCode: 200, data: { code: 1, data } });
      await assert.rejects(api[action](payload));
    }
  });
}

test('list endpoints reject missing data while retaining legitimate empty lists', async () => {
  for (const [file, action] of [['secondHandService', 'listProducts'], ['userService', 'getSchools'],
    ['addressService', 'getAllAddresses'], ['addressService', 'getCategories'], ['deliveryAddressService', 'getMyAddresses']]) {
    const broken = service(file, { statusCode: 200, data: { code: 1 } });
    await assert.rejects(broken[action]());
    const valid = service(file, { statusCode: 200, data: { code: 1, data: [] } });
    assert.equal((await valid[action]()).length, 0);
  }
});

test('successful void updates and absent optional taker data remain supported', async () => {
  const api = service('secondHandService', { statusCode: 200, data: { code: 1 } });
  await api.updateProduct(1, {});
  const taker = service('takeOrderService', { statusCode: 200, data: { code: 1, data: null } });
  assert.equal(Object.keys(await taker.getTakeOrderDetail(1)).length, 0);
  assert.equal(await taker.getDeliveryImage(1), null);
});

test('required creation, payment and conversation responses cannot succeed with no result', async () => {
  for (const [file, action, payload] of [
    ['userOrderService', 'createOrder', {}], ['secondHandService', 'payOrder', 1],
    ['secondHandService', 'createBargain', {}], ['secondHandService', 'sendMessage', {}],
    ['secondHandService', 'getTransferClaim', 1],
  ]) {
    const api = service(file, { statusCode: 200, data: { code: 1, data: null } });
    await assert.rejects(api[action](payload));
  }
});

test('missing configuration is not silently converted to zero', async () => {
  for (const action of ['getServiceFeeRate', 'getServiceFeeMin', 'getRunnerTransferSingleMax']) {
    for (const data of [undefined, null, '', ' ', false, {}, [], 'invalid', -1]) {
      const api = service('configService', { statusCode: 200, data: { code: 1, data } });
      await assert.rejects(api[action]());
    }
    for (const data of [0, '0', '0.5', 2000]) {
      const api = service('configService', { statusCode: 200, data: { code: 1, data } });
      assert.equal(await api[action](), Number(data));
    }
  }
});


test('HTTP 429 shows a readable message and does not confirm or automatically retry a write', async () => {
 const calls=[];const api=service('userService',{statusCode:429,data:{code:0,msg:'TOO_MANY_REQUESTS'}},calls);
 await assert.rejects(api.updateUserInfo({username:'test'}),error=>{
  assert.equal(error.message,'操作较频繁，请稍后再试');assert.equal(error.statusCode,429);
  assert.equal(error.requestOutcome,undefined);return true;
 });assert.equal(calls.length,1);
});
