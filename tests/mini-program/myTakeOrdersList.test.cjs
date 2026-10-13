const feedbackStub = require('./helpers/feedbackStub.cjs');
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
    getStorageSync: () => 'owner-a',
    getWindowInfo: () => ({ statusBarHeight: 47, windowWidth: 375 }),
    getMenuButtonBoundingClientRect: () => ({ top: 54, height: 32, left: 286, width: 87 }),
    ...wxOverrides,
  };
  vm.runInNewContext(source, {
    console,
    setTimeout, clearTimeout,
    wx,
    require: (name) => name.endsWith('tokenManager') ? { getToken: () => wx.getStorageSync('token') } : name.endsWith('runnerAmount') ? require('../../apps/mini-program/utils/runnerAmount') : name.endsWith('feedback') ? feedbackStub(wx) : name.includes('takeOrderService') ? takeService
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
  assert.equal(free.showAmount, true);
  assert.equal(free.displayAmount, '0.00');
  for (const price of [null, 0, '0.00']) {
    const completed = page.prepareOrder({ status: 5, price, businessType: 'NORMAL' });
    assert.equal(completed.statusDesc, '已完成');
    assert.equal(completed.statusGroup, 'completed');
    assert.equal(completed.statusKind, 'done');
    assert.equal(completed.primaryAction, '');
  }
  const reimbursed = page.prepareOrder({ status: 5, price: 0, productAmount: 12, businessType: 'PURCHASE' });
  assert.equal(reimbursed.statusGroup, 'settling');
  assert.equal(reimbursed.primaryAction, '确认收款');
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

function pagedHarness() {
 let token='a',failure=false,pending;const queries=[];
 const records=Array.from({length:45},(_,i)=>({id:i+1,orderId:i+100,status:1,price:2,note:i===43?'target':'book',username:'sender'}));
 records.push({id:90,orderId:190,status:5,price:0,note:'free'}, {id:91,orderId:191,status:5,price:0,businessType:'PURCHASE',productAmount:5}, {id:92,orderId:192,status:99});
 const page=createPage({wxOverrides:{getStorageSync:()=>token},takeService:{getMyTakeOrders:async q=>{
  queries.push({...q});if(failure)throw Error('network');if(pending&&q.page===2)return pending;
  const rows=records.filter(row=>q.group==='all'||page.getStatusGroup(row)===q.group)
    .filter(row=>!q.keyword||page.prepareOrder(row).searchText.includes(q.keyword));
  return rows.slice((q.page-1)*20,q.page*20);
 }}});
 return {page,queries,setToken:value=>token=value,setFailure:value=>failure=value,setPending:value=>pending=value};
}
test('taken list reads all pages and keeps unknown historical states',async()=>{
 const h=pagedHarness();await h.page.loadOrders();await h.page.onReachBottom();await h.page.onReachBottom();await h.page.onReachBottom();
 assert.equal(h.page.data.orders.length,48);assert.equal(h.queries.length,3);assert.equal(h.page.data.hasMoreOrders,false);
});
test('taken status filters include free completion and purchase reimbursement before paging',async()=>{
 const h=pagedHarness();await h.page.loadOrders();await h.page.selectTab({currentTarget:{dataset:{value:'completed'}}});
 assert.equal(h.page.data.filteredOrders[0].id,90);await h.page.selectTab({currentTarget:{dataset:{value:'settling'}}});
 assert.equal(h.page.data.filteredOrders[0].id,91);assert.equal(h.queries.at(-1).page,1);
});
test('taken search finds later-page record from page one',async()=>{
 const h=pagedHarness();await h.page.loadOrders();h.page.setData({keyword:'target'});await h.page.loadOrders();
 assert.equal(h.page.data.filteredOrders[0].id,44);assert.equal(h.queries.at(-1).page,1);
});
test('taken failed next page retains rows and retries the same cursor',async()=>{
 const h=pagedHarness();await h.page.loadOrders();h.setFailure(true);await h.page.onReachBottom();
 assert.equal(h.page.data.orders.length,20);h.setFailure(false);await h.page.onReachBottom();assert.equal(h.page.data.orders.length,40);assert.equal(h.queries.at(-1).page,2);
});
test('taken late responses cannot cross account, filter or page lifetime',async()=>{
 for(const action of ['account','filter','hide']) {
  const h=pagedHarness();await h.page.loadOrders();let resolve;h.setPending(new Promise(yes=>resolve=yes));const loading=h.page.onReachBottom();
  if(action==='account')h.setToken('b');else if(action==='filter')await h.page.selectTab({currentTarget:{dataset:{value:'completed'}}});else h.page.onHide();
  resolve([{id:999,orderId:1999,status:1}]);await loading;assert.equal(h.page.data.orders.some(row=>row.id===999),false);
 }
});
