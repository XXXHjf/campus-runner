const { test } = require('node:test');
const assert = require('node:assert/strict');
const service = require('../../apps/mini-program/services/subscriptionService');

test('subscription rejection, unsupported API and synchronous failure never reject the business action', async () => {
  global.wx = {};
  assert.deepEqual(await service.requestSecondHandOrder(), {});
  global.wx = { requestSubscribeMessage: ({ fail }) => fail({ errMsg: 'cancel' }) };
  assert.deepEqual(await service.requestSecondHandOrder(), {});
  global.wx = { requestSubscribeMessage: () => { throw new Error('unsupported'); } };
  assert.deepEqual(await service.requestSecondHandMessage(), {});
});

test('each user action requests the relevant single template without caching authorization', async () => {
  const requested = [];
  global.wx = { requestSubscribeMessage: ({ tmplIds, success }) => { requested.push(tmplIds); success({}); } };
  await service.requestSecondHandOrder();
  await service.requestSecondHandMessage();
  await service.requestSecondHandMessage();
  assert.equal(requested.length, 3);
  assert.equal(requested[0].length, 1);
  assert.notEqual(requested[0][0], requested[1][0]);
  assert.deepEqual(requested[1], requested[2]);
});
