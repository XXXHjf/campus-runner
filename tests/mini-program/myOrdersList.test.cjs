const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const test = require('node:test');

const source = fs.readFileSync(path.resolve(__dirname, '../../apps/mini-program/pages/orders/myOrders/ordersShow/show.js'), 'utf8');

function createPage() {
  let page;
  const wx = {
    getWindowInfo: () => ({ statusBarHeight: 47, windowWidth: 375 }),
    getMenuButtonBoundingClientRect: () => ({ top: 54, height: 32, left: 286, width: 87 }),
  };
  vm.runInNewContext(source, {
    console,
    Date,
    wx,
    require: () => ({}),
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

test('每个已知订单状态都有唯一且可见的分类，代买文案按业务显示', () => {
  const page = createPage();
  const expected = new Map([
    [-4, 'refund'], [-3, 'refund'], [-2, 'refund'], [-1, 'unpaid'],
    [0, 'waiting'], [1, 'processing'], [2, 'processing'], [3, 'processing'],
    [4, 'canceled'], [5, 'completed'], [6, 'completed'], [7, 'completed'],
  ]);
  const visibleGroups = new Set(page.data.filterOptions.map(option => option.value));
  assert.deepEqual([...page.data.filterOptions.map(option => option.label)],
    ['全部', '待支付', '待接单', '进行中', '已完成', '已取消', '退款']);
  for (const [status, group] of expected) {
    const order = page.prepareOrder({ status, businessType: 'NORMAL', categoryName: '外卖' });
    assert.equal(order.statusGroup, group, `status ${status}`);
    assert.ok(visibleGroups.has(group));
  }
  assert.equal(page.getStatusDescription({ status: 1, businessType: 'PURCHASE' }), '待购买');
  assert.equal(page.getStatusDescription({ status: 2, businessType: 'PURCHASE' }), '配送中');
});

test('关键词与状态分类同时筛选，取消和退款订单仍可在全部中看到', () => {
  const page = createPage();
  page.data.orders = [
    page.prepareOrder({ id: 1, status: 1, businessType: 'PURCHASE', categoryName: '代买', note: '早餐', pickUpAddress: '便利店', reciveAddress: '弘毅楼', payAmount: 35.5 }),
    page.prepareOrder({ id: 2, status: 4, businessType: 'NORMAL', categoryName: '外卖', note: '午餐', pickUpAddress: '食堂', reciveAddress: '菊苑', payAmount: 4 }),
    page.prepareOrder({ id: 3, status: -3, businessType: 'NORMAL', categoryName: '外卖', note: '晚餐', pickUpAddress: '食堂', reciveAddress: '菊苑', payAmount: 4 }),
  ];
  page.data.tabValue = 'processing';
  page.data.keyword = '早 餐';
  page.applyFilters();
  assert.deepEqual([...page.data.filteredOrders.map(order => order.id)], [1]);
  page.data.tabValue = 'all';
  page.data.keyword = '';
  page.applyFilters();
  assert.deepEqual([...page.data.filteredOrders.map(order => order.id)], [1, 2, 3]);
  assert.equal(page.data.orders[0].displayPayAmount, '35.50');
});

test('自定义导航为状态栏、搜索栏和分类栏都预留位置', () => {
  const page = createPage();
  page.updateNavigationMetrics();
  assert.equal(page.data.statusBarHeight, 47);
  assert.equal(page.data.navigationBarHeight, 46);
  assert.equal(page.data.controlsHeight, 47 + 46 + 375 * 84 / 750);
  assert.ok(page.data.navigationRightPadding >= 96);
});

test('卡片使用单行描述和订单图片，零金额不显示，搜索仍可匹配完整地址', () => {
  const page = createPage();
  const order = page.prepareOrder({
    status: 0,
    businessType: 'NORMAL',
    categoryName: '外卖',
    note: '第一句\n第二句',
    image: 'https://example.com/order.jpg',
    pickUpAddress: '大学城校区 食堂 竹苑餐厅 杭州面馆',
    reciveAddress: '大学城校区 学生公寓 梅苑1栋 1-201  ',
    payAmount: 0,
  });
  assert.equal(order.displayNote, '第一句 第二句');
  assert.equal(order.image, 'https://example.com/order.jpg');
  assert.equal(order.showPayAmount, false);
  assert.ok(order.searchText.includes('大学城校区'));
  assert.equal(page.prepareOrder({ payAmount: 0.1 }).showPayAmount, true);
  assert.equal(page.prepareOrder({ price: 5 }).showPayAmount, false);
  assert.equal(page.prepareOrder({ status: -1 }).primaryAction, '去支付');
  assert.equal(page.prepareOrder({ status: 0 }).primaryAction, '编辑');
  assert.equal(page.prepareOrder({ status: 3 }).primaryAction, '确认订单');
  assert.equal(page.prepareOrder({ status: 1, businessType: 'PURCHASE' }).primaryAction, '');
  assert.equal(page.prepareOrder({ status: 5 }).primaryAction, '');
});

test('更多操作绑定当前订单，待接单订单分享跳转接单详情', () => {
  const page = createPage();
  const order = page.prepareOrder({ id: 42, status: 0, categoryName: '外卖', note: '一份晚饭', image: 'https://example.com/order.jpg' });
  page.data.orders = [order];
  page.openActionSheet({ currentTarget: { dataset: { id: 42 } } });
  assert.equal(page.data.actionSheetVisible, true);
  assert.equal(page.data.actionOrder.id, 42);
  const share = page.onShareAppMessage();
  assert.equal(share.path, '/pages/orders/takeOrders/takesInfo/info?id=42');
  assert.equal(share.title, '一份晚饭');
  assert.equal(page.data.actionSheetVisible, false);
  page.data.orders = [page.prepareOrder({ id: 43, status: 5, categoryName: '外卖', note: '已完成订单' })];
  page.openActionSheet({ currentTarget: { dataset: { id: 43 } } });
  assert.equal(page.data.actionSheetVisible, true);
  assert.equal(page.data.actionOrder.id, 43);
});
