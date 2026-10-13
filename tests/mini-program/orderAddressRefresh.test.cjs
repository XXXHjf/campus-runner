const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');

function harness(addresses, extraAddresses = []) {
  let page;
  vm.runInNewContext(fs.readFileSync(require.resolve(
    '../../apps/mini-program/pages/orders/myOrders/ordersAdd/add'
  ), 'utf8'), {
    console: { log() {}, error() {} },
    getApp: () => ({ globalData: {} }),
    Page(config) {
      page = { ...config, data: structuredClone(config.data),
        setData(update) { Object.assign(this.data, update); } };
    },
    require(id) {
      if (id.endsWith('addressPager')) return require('./helpers/addressPagerHarness.cjs')({});
      if (id.endsWith('accountStorage')) return { captureSession: () => 'a', isCurrent: () => true, read: () => null };
      if (id.endsWith('feedback')) return { loaded() {}, showMessage() {} };
      if (id.endsWith('orderConstants')) return require('../../apps/mini-program/utils/orderConstants');
      if (id.endsWith('tokenManager')) return { waitForToken: async () => {} };
      if (id.endsWith('addressService')) return { getAllAddresses: async () => addresses, getOptionalAddress: async id => [...addresses, ...extraAddresses].find(row => String(row.id) === String(id)) || null };
      return {};
    },
  });
  page._syncDeliveryTime = () => {};
  page._checkUserAuth = async () => ({ id: 1 });
  page._calculateFeePreview = () => {};
  page.setData({ feeConfigLoaded: true, addrList: [{ id: 1 }, { id: 2 }] });
  return page;
}

test('returning to the order page replaces both selected addresses with their latest content', async () => {
  const pickup = { id: 1, compusName: '新校区', buildCategoryName: '快递点', buildingName: '新取件楼', details: '北门', label: '新标签' };
  const delivery = { id: 2, compusName: '新校区', buildCategoryName: '宿舍', buildingName: '新送达楼', details: '' };
  const page = harness([pickup, delivery]);
  page.setData({ showPickUp: { ...pickup, details: '旧取件详情', label: '旧标签' },
    showRecive: { ...delivery, details: '旧送达详情' } });
  await page.onShow();
  assert.equal(page.data.showPickUp, pickup);
  assert.equal(page.data.showRecive, delivery);
  assert.equal(page.data.pickUpList[0], page.data.showPickUp);
  assert.equal(page.data.reciveList[1], page.data.showRecive);
});

test('editing an address selected for both ends updates both selections', async () => {
  const updated = { id: 1, buildingName: '新楼宇', details: '新详情' };
  const page = harness([updated]);
  page.setData({ showPickUp: { id: 1, details: '旧详情' }, showRecive: { id: 1, details: '旧详情' } });
  await page.onShow();
  assert.equal(page.data.showPickUp, updated);
  assert.equal(page.data.showRecive, updated);
});

test('selected historical addresses outside the first page are reloaded by ID and stay selectable', async () => {
  const first = Array.from({ length: 20 }, (_, i) => ({ id: i + 1 }));
  const older = { id: 61, details: '最新的旧地址内容' };
  const page = harness(first, [older]);page.setData({ showPickUp: { id: 61, details: '缓存内容' }, showRecive: { id: 1 } });
  await page.onShow();assert.equal(page.data.showPickUp, older);assert.equal(page.data.showRecive.id, 1);
  assert.equal(page.data.addrList.some(row => row.id === 61), true);
});

test('deleted selections clear independently and refreshing never selects an unchosen address', async () => {
  const remaining = { id: 2, details: '保留地址' };
  const page = harness([remaining]);
  page.setData({ showPickUp: { id: 1 }, showRecive: { id: 2 } });
  await page.onShow();
  assert.equal(page.data.showPickUp, null);
  assert.equal(page.data.showRecive, remaining);
  page.setData({ showPickUp: remaining, showRecive: null });
  await page.onShow();
  assert.equal(page.data.showPickUp, remaining);
  assert.equal(page.data.showRecive, null);
  page._getAddress = async () => [];
  await page.onShow();
  assert.equal(page.data.showPickUp, null);
  assert.equal(page.data.showRecive, null);
});
