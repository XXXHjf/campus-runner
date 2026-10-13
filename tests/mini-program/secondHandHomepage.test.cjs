const feedbackStub = require('./helpers/feedbackStub.cjs');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const vm = require('node:vm');

const source = fs.readFileSync(path.resolve(
  __dirname, '../../apps/mini-program/pages/second-hand/index/index.js',
), 'utf8');
const tick = () => new Promise((resolve) => setImmediate(resolve));
function deferred() {
  let resolve, reject;
  const promise = new Promise((yes, no) => { resolve = yes; reject = no; });
  return { promise, resolve, reject };
}

function createHarness({ token = 'session-a', getUserInfo, getSchools, listProducts, listCategories } = {}) {
  const state = {
    user: { id: 7, schoolId: 101 },
    schools: [
      { id: '101', schoolName: ' 测试大学 ' },
      { id: 102, schoolName: '另一所大学' },
      { id: 103, schoolName: '  ' },
    ],
    userReads: 0,
    schoolReads: 0,
    loadErrors: [],
  };
  const toasts = [];
  const app = {
    globalData: { userInfo: token ? { id: 7, token } : null, silentLoginPromise: null },
    refreshMineTabRedDot: async () => {},
  };
  let page;
  vm.runInNewContext(source, {
    Page(config) {
      page = {
        ...config,
        data: structuredClone(config.data),
        setData(update, callback) { Object.assign(this.data, update); callback?.(); },
      };
    },
    getApp: () => app,
    require(id) {
      if (id.endsWith('feedback')) return feedbackStub({ showToast: ({ title }) => toasts.push(title) }, (message) => state.loadErrors.push(message));
      if (id.endsWith('tokenManager')) {
        return { getToken: () => app.globalData.userInfo?.token || null };
      }
      if (id.endsWith('userService')) {
        return {
          getUserInfo: async () => {
            state.userReads += 1;
            return getUserInfo ? getUserInfo() : state.user;
          },
          getSchools: async () => {
            state.schoolReads += 1;
            return getSchools ? getSchools() : state.schools;
          },
        };
      }
      if (id.endsWith('secondHandService')) {
        return {
          listCategories: listCategories || (async () => []),
          listProducts: listProducts || (async () => [{ id: 1, title: '测试商品', images: 'one.jpg' }]),
        };
      }
      throw new Error(`Unexpected dependency: ${id}`);
    },
    wx: {
      showShareMenu() {},
      getWindowInfo: () => ({ windowWidth: 375, statusBarHeight: 20 }),
      getMenuButtonBoundingClientRect: () => ({ top: 24, left: 281, width: 87, height: 32 }),
      showToast: (options) => toasts.push(options.title),
    },
    console: { warn() {}, error() {} },
  });
  page.onLoad();
  return { page, app, state, toasts };
}

test('anonymous and registered users without a saved school do not show a school', async () => {
  const { page, app, state } = createHarness({ token: null });
  await page.onShow();
  assert.equal(state.userReads, 0);
  assert.equal(page.data.schoolName, '');
  assert.equal(page.data.products.length, 1);

  app.globalData.userInfo = { id: 7, token: 'registered-session' };
  for (const school of [
    { schoolId: null, schoolName: null },
    { schoolId: null, schoolName: '未绑定学校' },
    { schoolId: 999 },
    { schoolId: 103 },
  ]) {
    state.user = { id: 7, profileCompleted: true, ...school };
    await page.onShow();
    assert.equal(page.data.schoolName, '');
  }
});

test('returning after school submission and pull-to-refresh update the school; logout clears it', async () => {
  const { page, app, state } = createHarness();
  state.user = { id: 7, schoolId: null, schoolName: null };
  await page.onShow();
  page.onHide();
  state.user = { id: 7, schoolId: 101, studentIdCardReview: 1 };
  await page.onShow();
  assert.equal(page.data.schoolName, '测试大学');

  state.user = { id: 7, schoolId: 102 };
  await page.onRefresh();
  assert.equal(page.data.schoolName, '另一所大学');
  assert.equal(page.data.refreshing, false);

  page.onHide();
  app.globalData.userInfo = null;
  const readsBeforeLogout = state.userReads;
  const showing = page.onShow();
  assert.equal(page.data.schoolName, '');
  await showing;
  assert.equal(state.userReads, readsBeforeLogout);
});

test('cold startup waits for silent login and then displays the school without reopening', async () => {
  const gate = deferred();
  const { page, app, state } = createHarness({ token: null });
  app.globalData.silentLoginPromise = gate.promise;
  const showing = page.onShow();
  await tick();
  assert.equal(state.userReads, 0);
  assert.equal(page.data.products.length, 1);
  app.globalData.userInfo = { id: 7, token: 'restored-session' };
  gate.resolve(true);
  await showing;
  assert.equal(page.data.schoolName, '测试大学');
  assert.equal(state.schoolReads, 1);
});

test('unsuccessful silent login leaves the header empty without fetching user details', async () => {
  const { page, app, state, toasts } = createHarness({ token: null });
  app.globalData.silentLoginPromise = Promise.resolve(false);
  await page.onShow();
  assert.equal(page.data.schoolName, '');
  assert.equal(state.userReads, 0);
  assert.deepEqual(toasts, []);
});

test('school fetch failure hides the school without blocking products or showing a toast', async () => {
  const { page, toasts } = createHarness({ getSchools: async () => {
    throw new Error('network failure');
  } });
  page.data.schoolName = '旧学校';
  await page.onShow();
  assert.equal(page.data.schoolName, '');
  assert.equal(page.data.products.length, 1);
  assert.deepEqual(toasts, []);
});

test('an older response cannot overwrite the newest school refresh', async () => {
  const gate = deferred();
  let reads = 0;
  const { page } = createHarness({ getUserInfo: () => ++reads === 1
    ? gate.promise
    : { id: 7, schoolId: 102 } });
  const initial = page.onShow();
  await tick();
  await page.onRefresh();
  gate.resolve({ id: 7, schoolId: 101 });
  await initial;
  assert.equal(page.data.schoolName, '另一所大学');
});

test('late school results are ignored after hiding or unloading the page', async () => {
  for (const lifecycle of ['onHide', 'onUnload']) {
    const gate = deferred();
    const { page } = createHarness({ getUserInfo: () => gate.promise });
    const loading = page.loadSchoolName();
    page[lifecycle]();
    gate.resolve({ id: 7, schoolId: 101 });
    await loading;
    assert.equal(page.data.schoolName, '');
  }
});

test('token renewal for the same user is accepted but logout or an account change is not', async () => {
  for (const userId of [7, 8, null]) {
    const gate = deferred();
    const { page, app } = createHarness({ getUserInfo: () => gate.promise });
    const loading = page.loadSchoolName();
    app.globalData.userInfo = userId == null ? null : { id: userId, token: 'new-session' };
    gate.resolve({ id: 7, schoolId: 101 });
    await loading;
    assert.equal(page.data.schoolName, userId === 7 ? '测试大学' : '');
  }
});

test('product failures show a user-facing message and do not prevent school loading', async () => {
  const { page, toasts } = createHarness({ listProducts: async () => {
    throw new Error('BadSqlGrammarException: internal query');
  } });
  await page.onShow();
  assert.equal(page.data.schoolName, '测试大学');
  assert.deepEqual(toasts, []);
  assert.equal(page.data.feedbackLoadError, '商品加载失败，请重试');
  assert.equal(page.data.loading, false);
});

for (const action of ['search', 'category', 'filter']) {
  test(`${action}: newer results and stats survive an older late response`, async () => {
    const requests = [];
    const { page } = createHarness({ listProducts: (query) => {
      const pending = deferred();
      requests.push({ ...pending, query });
      return pending.promise;
    } });
    const oldLoad = page.loadData();
    if (action === 'search') {
      page.onSearchChange({ detail: { value: '书' } });
      page.onSearch();
    } else if (action === 'category') {
      page.selectCategory({ currentTarget: { dataset: { id: 3 } } });
    } else {
      page.setData({ draftCategoryId: 4, draftPickupAddress: { prefix: '北校区 一栋', displayText: '北校区 一栋' } });
      page.applyFilterSelection();
    }
    assert.equal(requests.length, 2);
    assert.equal(requests[0].query.keyword, '');
    if (action === 'search') assert.equal(requests[1].query.keyword, '书');
    if (action === 'category') assert.equal(requests[1].query.categoryId, 3);
    if (action === 'filter') {
      assert.equal(requests[1].query.categoryId, 4);
      assert.equal(requests[1].query.pickupAddressPrefix, '北校区 一栋');
    }
    requests[1].resolve([{ id: 2, negotiable: 1 }]);
    await tick();
    assert.equal(page.data.loading, false);
    requests[0].resolve([{ id: 1 }, { id: 3 }]);
    await oldLoad;
    assert.equal(page.data.products.length, 1);
    assert.equal(page.data.products[0].id, 2);
    assert.equal(page.data.stats.onSale, 1);
    assert.equal(page.data.stats.negotiable, 1);
  });
}

test('older failure cannot stop the latest loading or pull-to-refresh indicator', async () => {
  const requests = [];
  const { page, state } = createHarness({ listProducts: () => {
    const pending = deferred(); requests.push(pending); return pending.promise;
  } });
  page.setData({ refreshing: true });
  const first = page.loadData();
  page.setData({ keyword: '新查询' });
  const latest = page.loadData();
  requests[0].reject(new Error('old failure'));
  await first;
  assert.equal(page.data.loading, true);
  assert.equal(page.data.refreshing, true);
  assert.equal(state.loadErrors.length, 0);
  requests[1].resolve([]);
  await latest;
  assert.equal(page.data.loading, false);
  assert.equal(page.data.refreshing, false);
  assert.equal(page.data.products.length, 0);
  assert.equal(page.data.feedbackLoadError, '');
});

test('latest empty results survive an older late failure without showing an error', async () => {
  const old = deferred();
  let reads = 0;
  const { page, state } = createHarness({ listProducts: () => ++reads === 1 ? old.promise : [] });
  const first = page.loadData();
  page.setData({ keyword: '无匹配' });
  await page.loadData();
  old.reject(new Error('old failure'));
  await first;
  assert.equal(page.data.products.length, 0);
  assert.equal(page.data.stats.onSale, 0);
  assert.equal(page.data.loading, false);
  assert.equal(page.data.feedbackLoadError, '');
  assert.equal(state.loadErrors.length, 0);
});

test('latest query failure keeps its error after an older success and clears unrelated products', async () => {
  const old = deferred();
  let reads = 0;
  const { page, state } = createHarness({ listProducts: () => ++reads === 1 ? old.promise : Promise.reject(new Error('latest failure')) });
  page.setData({ products: [{ id: 99 }], stats: { onSale: 1, negotiable: 1 } });
  const first = page.loadData();
  page.setData({ keyword: '新查询' });
  const latest = page.loadData();
  assert.equal(page.data.products.length, 0);
  assert.equal(page.data.loading, true);
  await latest;
  assert.equal(page.data.feedbackLoadError, '商品加载失败，请重试');
  old.resolve([{ id: 1 }]);
  await first;
  assert.equal(page.data.products.length, 0);
  assert.equal(page.data.stats.onSale, 0);
  assert.equal(page.data.feedbackLoadError, '商品加载失败，请重试');
  assert.equal(state.loadErrors.length, 1);
});

test('hidden or unloaded list requests cannot change results, errors or loading flags', async () => {
  for (const lifecycle of ['onHide', 'onUnload']) {
    for (const failure of [false, true]) {
      const pending = deferred();
      const { page, state } = createHarness({ listProducts: () => pending.promise });
      const loading = page.loadData();
      page[lifecycle]();
      const snapshot = JSON.stringify(page.data);
      if (failure) pending.reject(new Error('late failure'));
      else pending.resolve([{ id: 5 }]);
      await loading;
      assert.equal(JSON.stringify(page.data), snapshot);
      assert.equal(state.loadErrors.length, 0);
    }
  }
});


const fullPage = (start = 1) => Array.from({length:20},(_,i)=>({id:start+i,title:`item-${start+i}`}));
test('homepage loads subsequent pages and stops after a short final page',async()=>{
 const queries=[];const {page}=createHarness({listProducts:async q=>{queries.push(q);return q.page===1?fullPage():[{id:21}];}});
 await page.loadData();await page.loadMoreProducts();await page.loadMoreProducts();
 assert.equal(queries.length,2);assert.equal(queries[0].page,1);assert.equal(queries[1].page,2);
 assert.equal(queries[0].pageSize,20);assert.equal(page.data.products.length,21);assert.equal(page.data.hasMoreProducts,false);
});
test('homepage refresh replaces appended data and resets pagination',async()=>{
 let fresh=false;const queries=[];const {page}=createHarness({listProducts:async q=>{queries.push(q.page);return fresh?[{id:99}]:fullPage((q.page-1)*20+1);}});
 await page.loadData();await page.loadMoreProducts();fresh=true;await page.loadData();
 assert.equal(page.data.products.length,1);assert.equal(page.data.products[0].id,99);assert.equal(page.productPage,1);assert.equal(queries.at(-1),1);
});
test('repeated scroll events share a page load and deduplicate overlapping items',async()=>{
 const pending=deferred();let reads=0;const {page}=createHarness({listProducts:async q=>{reads++;return q.page===1?fullPage():pending.promise;}});
 await page.loadData();const first=page.loadMoreProducts();const duplicate=page.loadMoreProducts();
 pending.resolve([{id:20,title:'updated'},{id:21}]);await Promise.all([first,duplicate]);
 assert.equal(reads,2);assert.equal(page.data.products.length,21);assert.equal(page.data.products[19].title,'updated');
});
test('failed subsequent page keeps existing items and retries the same page',async()=>{
 let failed=true;const queries=[];const {page}=createHarness({listProducts:async q=>{queries.push(q.page);if(q.page===1)return fullPage();if(failed)throw Error('network');return [{id:21}];}});
 await page.loadData();await page.loadMoreProducts();assert.equal(page.data.products.length,20);assert.equal(page.productPage,1);
 failed=false;await page.loadMoreProducts();assert.equal(queries.at(-1),2);assert.equal(page.data.products.length,21);
});
test('late appended page cannot restore old products after a filter change',async()=>{
 const pending=deferred();const {page}=createHarness({listProducts:async q=>q.keyword?[{id:99}]:q.page===1?fullPage():pending.promise});
 await page.loadData();const more=page.loadMoreProducts();page.setData({keyword:'new'});await page.loadData();
 pending.resolve([{id:21}]);await more;assert.equal(page.data.products.length,1);assert.equal(page.data.products[0].id,99);assert.equal(page.productPage,1);
});
test('account changes discard old page data even when the next request fails',async()=>{
 let failed=false;const {page,app}=createHarness({listProducts:async()=>{if(failed)throw Error('network');return fullPage();}});
 await page.loadData();app.globalData.userInfo={id:8,token:'session-b'};failed=true;await page.loadData();
 assert.equal(page.data.products.length,0);assert.equal(page.data.hasMoreProducts,false);
});
