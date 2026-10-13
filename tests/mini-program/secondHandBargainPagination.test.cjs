const test=require('node:test'),assert=require('node:assert/strict'),vm=require('node:vm'),fs=require('node:fs'),path=require('node:path');
const feedbackStub=require('./helpers/feedbackStub.cjs');
const source=fs.readFileSync(path.resolve(__dirname,'../../apps/mini-program/pages/second-hand/bargains/bargains.js'),'utf8');
const defer=()=>{let resolve;const promise=new Promise(yes=>resolve=yes);return {promise,resolve};};
function harness(){let page;const state={token:'a',queries:[],failure:false,pending:null,summary:{bargainPendingCount:80,bargainAcceptedCount:12}};const app={globalData:{userInfo:{id:7,token:'a'}}};
 const records=Array.from({length:45},(_,i)=>({id:i+1,buyerId:8,sellerId:7,status:i%5,productId:i+100,offerPrice:10}));records.push({id:90,buyerId:7,sellerId:8,status:1});
 vm.runInNewContext(source,{getApp:()=>app,wx:{getStorageSync:()=>null,setStorageSync(){}},
 require:id=>id.endsWith('taskSummaryService')?{getSummary:async()=>{if(state.summary instanceof Error)throw state.summary;return state.summary;}}:id.endsWith('tokenManager')?{getToken:()=>state.token}:id.endsWith('feedback')?feedbackStub():id.endsWith('secondHandStatus')?require('../../apps/mini-program/utils/secondHandStatus'):id.endsWith('userService')?{getUserInfo:async()=>({id:state.token==='a'?7:9})}:id.endsWith('secondHandService')?{listMyBargains:async query=>{
  state.queries.push({...query});if(state.failure)throw Error('network');if(state.pending&&query.page===2)return state.pending.promise;
  return records.filter(item=>query.role==='received'?item.sellerId===7:item.buyerId===7).slice((query.page-1)*20,query.page*20);
 }}:{},Page:config=>{page={...config,data:structuredClone(config.data),setData:update=>Object.assign(page.data,update)};}});
 return {state,page};}
test('received records read all pages and stop at the end',async()=>{const {page,state}=harness();await page.loadList();await page.loadMore();await page.loadMore();await page.loadMore();assert.equal(page.data.received.length,45);assert.equal(state.queries.length,3);assert.equal(page.data.hasMore,false);});
test('issued tab filters full history on the server from page one',async()=>{const {page,state}=harness();await page.loadList();await page.loadMore();await page.onTabsClick({detail:{value:'issued'}});assert.equal(page.data.issued[0].id,90);assert.equal(page.data.received.length,0);assert.equal(state.queries.at(-1).page,1);assert.equal(state.queries.at(-1).role,'issued');});
test('append failure retains cursor and confirmed record states',async()=>{const {page,state}=harness();await page.loadList();state.failure=true;await page.loadMore();assert.equal(page.data.received.length,20);state.failure=false;await page.loadMore();assert.equal(state.queries.at(-1).page,2);assert.equal(page.data.received.length,40);});
test('late next page cannot overwrite another tab, account or hidden page',async()=>{
 for(const action of ['tab','account','hide']){const {page,state}=harness();await page.loadList();state.pending=defer();const loading=page.loadMore();await Promise.resolve();await Promise.resolve();
  if(action==='tab')await page.onTabsClick({detail:{value:'issued'}});else if(action==='account')state.token='b';else page.onHide();
  state.pending.resolve([{id:999,buyerId:8,sellerId:7,status:0}]);await loading;assert.equal(page.findBargain(999),undefined);
 }
});
test('failed refresh after account change clears previous owner rows',async()=>{const {page,state}=harness();await page.loadList();state.token='b';state.failure=true;await page.loadList();assert.equal(page.data.received.length,0);assert.equal(page.data.issued.length,0);});

test('notification tab is applied before first load and survives returning to the page',async()=>{
 for(const tab of ['received','issued']){const {page,state}=harness();page.onLoad({tab});await page.onShow();
  assert.equal(page.data.tab,tab);assert.equal(state.queries[0].role,tab);assert.equal(state.queries[0].page,1);
  assert.ok(page.findBargain(tab==='issued'?90:1));
  page.onHide();await page.onShow();assert.equal(page.data.tab,tab);assert.equal(state.queries.at(-1).role,tab);
 }
});

test('ordinary entry and invalid tab preserve the received default',async()=>{
 for(const options of [undefined,{}, {tab:'invalid'}]){const {page,state}=harness();page.onLoad(options);await page.onShow();
  assert.equal(page.data.tab,'received');assert.equal(state.queries[0].role,'received');
 }
});

test('tab badges use full task counts and opening records does not acknowledge them',async()=>{
 const {page,state}=harness();await page.onShow();assert.equal(page.data.receivedBadge.count,80);assert.equal(page.data.issuedBadge.count,12);
 await page.onTabsClick({detail:{value:'issued'}});assert.equal(page.data.issued.length,1);assert.equal(page.data.issuedBadge.count,12);
 state.summary={bargainPendingCount:0,bargainAcceptedCount:0};await page.loadList();assert.equal(page.data.issuedBadge.count,0);
});

test('failed task refresh hides stale badge without hiding records',async()=>{
 const {page,state}=harness();await page.onShow();state.summary=Error('network');await page.loadList();
 assert.equal(page.data.issuedBadge.count,0);assert.equal(page.data.receivedBadge.count,0);assert.equal(page.data.received.length,20);
});
