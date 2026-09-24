const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const test = require('node:test');

const root = path.resolve(__dirname, '../../apps/mini-program/pages/orders/takeOrders');
const source = fs.readFileSync(path.join(root, 'takesShow/show.js'), 'utf8');
const listTemplate = fs.readFileSync(path.join(root, 'takesShow/show.wxml'), 'utf8');
const detailTemplate = fs.readFileSync(path.join(root, 'takesInfo/info.wxml'), 'utf8');

function createPage({ takeService = {}, orderService = {}, wxOverrides = {} } = {}) {
  let page;
  const wx = {
    getWindowInfo: () => ({ statusBarHeight: 47, windowWidth: 375 }),
    getMenuButtonBoundingClientRect: () => ({ top: 54, height: 32, left: 286, width: 87 }),
    ...wxOverrides,
  };
  vm.runInNewContext(source, {
    console,
    wx,
    require: (name) => name.includes('takeOrderService') ? takeService
      : name.includes('userOrderService') ? orderService
        : name.includes('transformers') ? { showError: () => {} }
          : {},
    Page(config) {
      page = {
        ...config,
        data: structuredClone(config.data),
        setData(value, callback) {
          Object.assign(this.data, value);
          if (callback) callback();
        },
      };
    },
  });
  return page;
}

test('接单状态各归一类，代买和无偿单有正确文案及金额', () => {
  const page = createPage();
  const expected = new Map([
    [-4, 'exception'], [-3, 'exception'], [-2, 'exception'], [-1, 'exception'],
    [0, 'exception'], [1, 'processing'], [2, 'processing'], [3, 'confirming'],
    [4, 'canceled'], [5, 'settling'], [6, 'completed'], [7, 'exception'],
  ]);
  const visible = new Set(page.data.filterOptions.map(item => item.value));
  for (const [status, group] of expected) {
    const order = page.prepareOrder({ status, price: 5, categoryName: '外卖' });
    assert.equal(order.statusGroup, group, `status ${status}`);
    assert.ok(visible.has(group));
  }
  const free = page.prepareOrder({ status: 5, price: null, categoryName: '外卖' });
  assert.equal(free.statusGroup, 'completed');
  assert.equal(free.showAmount, false);
  const purchase = page.prepareOrder({ status: 1, price: 5, productAmount: 13, businessType: 'PURCHASE', categoryName: '代买' });
  assert.equal(purchase.statusDesc, '待购买');
  assert.equal(purchase.displayAmount, '18.00');
  assert.equal(purchase.primaryAction, '查看任务');
});

test('分类与关键词组合筛选，昵称、任务和地址均可搜索', () => {
  const page = createPage();
  page.data.orders = [
    page.prepareOrder({ id: 1, orderId: 11, status: 1, price: 5, username: '林同学', note: '晚餐', pickUpAddress: '竹苑餐厅' }),
    page.prepareOrder({ id: 2, orderId: 12, status: 4, price: 5, username: '周同学', note: '午餐', pickUpAddress: '菊苑餐厅' }),
  ];
  page.data.tabValue = 'processing';
  page.data.keyword = '林 同 学';
  page.applyFilters();
  assert.equal(page.data.filteredOrders.length, 1);
  assert.equal(page.data.filteredOrders[0].orderId, 11);
  page.data.keyword = '菊苑';
  page.applyFilters();
  assert.equal(page.data.filteredOrders.length, 0);
  page.data.tabValue = 'all';
  page.applyFilters();
  assert.equal(page.data.filteredOrders[0].orderId, 12);
});

test('普通单取件前重新核对状态，按钮不会越过详情执行代买操作', async () => {
  let updates = 0;
  let navigated = '';
  const page = createPage({
    takeService: { updateTakeOrderStatus: async () => { updates += 1; return { code: 1 }; }, getMyTakeOrders: async () => [] },
    orderService: { getMyOrderDetail: async () => ({ status: 2, businessType: 'NORMAL' }) },
    wxOverrides: {
      showToast: () => {},
      navigateTo: ({ url }) => { navigated = url; },
    },
  });
  page.data.orders = [page.prepareOrder({ id: 7, orderId: 11, status: 1, businessType: 'NORMAL' })];
  await page.onPrimaryAction({ currentTarget: { dataset: { id: 11 } } });
  assert.equal(updates, 0);
  assert.equal(page.data.actionBusy, false);
  const purchase = page.prepareOrder({ id: 8, orderId: 12, status: 1, businessType: 'PURCHASE' });
  page.data.orders = [purchase];
  await page.onPrimaryAction({ currentTarget: { dataset: { id: 12 } } });
  assert.equal(navigated, '/pages/orders/takeOrders/takesInfo/info?id=12');
  assert.equal(updates, 0);
});

test('普通单确认取件后使用接单记录 ID 更新状态并刷新列表', async () => {
  let payload;
  const page = createPage({
    takeService: {
      updateTakeOrderStatus: async (value) => { payload = value; return { code: 1 }; },
      getMyTakeOrders: async () => [],
    },
    orderService: { getMyOrderDetail: async () => ({ status: 1, businessType: 'NORMAL' }) },
    wxOverrides: {
      showModal: ({ success }) => success({ confirm: true }),
      showToast: () => {},
    },
  });
  page.data.orders = [page.prepareOrder({ id: 7, orderId: 11, status: 1, businessType: 'NORMAL' })];
  await page.onPrimaryAction({ currentTarget: { dataset: { id: 11 } } });
  assert.equal(JSON.stringify(payload), JSON.stringify({ id: 7, status: 1 }));
  assert.equal(page.data.orders.length, 0);
  assert.equal(page.data.actionBusy, false);
});

test('列表和代买详情没有接单人取消入口', () => {
  assert.doesNotMatch(listTemplate, /取消接单/);
  assert.doesNotMatch(detailTemplate, /取消接单/);
  assert.match(listTemplate, /order.senderAvatar/);
  assert.match(listTemplate, /take-orders__controls/);
});
