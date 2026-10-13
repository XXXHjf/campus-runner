const test=require('node:test');
const assert=require('node:assert/strict');
const vm=require('node:vm');
const fs=require('node:fs');
const path=require('node:path');
const feedbackStub=require('./helpers/feedbackStub.cjs');
const root=path.resolve(__dirname,'../../apps/mini-program');
const statusModule={exports:{}};
vm.runInNewContext(fs.readFileSync(path.join(root,'utils/secondHandStatus.js'),'utf8'),{module:statusModule,require:()=>({})});
const tools=statusModule.exports;
const deferred=()=>{let resolve;const promise=new Promise(yes=>{resolve=yes;});return {promise,resolve};};
function harness(role){
 const state={token:'owner-a',queries:[],failure:false,pending:null,timers:new Map(),timerId:0};let page;
 const records=Array.from({length:60},(_,i)=>({id:i+1,status:1,tradeMode:'OFFLINE',productTitle:i===44?'target':'BOOK',buyerName:'Other',sellerName:'Other',orderNumber:`SH${i+1}`}));
 records.push({id:99,status:3,tradeMode:'OFFLINE',productTitle:'completed'},{id:100,status:6,tradeMode:'ONLINE',productTitle:'refund'},{id:101,status:8,tradeMode:'OFFLINE',productTitle:'unknown'});
 const fetch=async q=>{
  state.queries.push({...q});if(state.failure)throw Error('network');if(state.pending&&q.page===2)return state.pending.promise;
  const filtered=records.filter(item=>q.group==='all'||tools.orderStatusGroup(item.status,item.tradeMode)===q.group)
   .filter(item=>!q.keyword||[item.productTitle,item.buyerName,item.orderNumber].join('').replace(/\s+/g,'').toLowerCase().includes(q.keyword));
  return filtered.slice((q.page-1)*20,q.page*20);
 };
 const kind=role==='buyer'?'orders':'seller-orders';
 vm.runInNewContext(fs.readFileSync(path.join(root,`pages/second-hand/${kind}/${kind}.js`),'utf8'),{
  Page:config=>{page={...config,data:structuredClone(config.data),setData:update=>Object.assign(page.data,update)};},
  require:id=>id.endsWith('tokenManager')?{getToken:()=>state.token}:id.endsWith('secondHandService')?{listBuyerOrders:fetch,listSellerOrders:fetch}:
   id.endsWith('secondHandStatus')?tools:id.endsWith('feedback')?feedbackStub():{},
  setTimeout:fn=>{const id=++state.timerId;state.timers.set(id,fn);return id;},clearTimeout:id=>state.timers.delete(id),
  wx:{getWindowInfo:()=>({windowWidth:375,statusBarHeight:20}),getMenuButtonBoundingClientRect:()=>({width:87,left:281,top:24,height:32}),stopPullDownRefresh(){}},
 });page.onLoad();state.flush=async()=>{const callbacks=[...state.timers.values()];state.timers.clear();await Promise.all(callbacks.map(fn=>fn()));};
 return {state,page};
}
for(const role of ['buyer','seller']){
 test(`${role}: loads all pages including unknown historical statuses`,async()=>{
  const {state,page}=harness(role);await page.loadOrders();for(let i=0;i<4;i++)await page.onReachBottom();
  assert.equal(page.data.orders.length,63);assert.equal(page.data.filteredOrders.length,63);
  assert.equal(state.queries.length,4);assert.equal(page.data.hasMoreOrders,false);
 });
 test(`${role}: status tabs query the complete history from page one`,async()=>{
  const {state,page}=harness(role);await page.loadOrders();await page.onReachBottom();
  await page.selectTab({currentTarget:{dataset:{value:'completed'}}});assert.equal(page.data.filteredOrders[0].id,99);
  await page.selectTab({currentTarget:{dataset:{value:'refund'}}});assert.equal(page.data.filteredOrders[0].id,100);
  assert.equal(state.queries.at(-1).page,1);assert.equal(state.queries.at(-1).group,'refund');
 });
 test(`${role}: delayed search uses only the latest text and finds a later-page order`,async()=>{
  const {state,page}=harness(role);await page.loadOrders();
  page.onSearchChange({detail:{value:'missing'}});page.onSearchChange({detail:{value:'ta \u00a0 rget'}});
  assert.equal(state.timers.size,1);await state.flush();assert.equal(state.queries.at(-1).keyword,'target');
  assert.equal(page.data.filteredOrders.length,1);assert.equal(page.data.filteredOrders[0].id,45);
 });
 test(`${role}: failed next page preserves loaded rows and retries the same page`,async()=>{
  const {state,page}=harness(role);await page.loadOrders();state.failure=true;await page.onReachBottom();
  assert.equal(page.data.orders.length,20);state.failure=false;await page.onReachBottom();
  assert.equal(state.queries.at(-1).page,2);assert.equal(page.data.orders.length,40);
 });
 test(`${role}: stale next page cannot overwrite a new filter`,async()=>{
  const {state,page}=harness(role);await page.loadOrders();state.pending=deferred();const more=page.onReachBottom();
  await page.selectTab({currentTarget:{dataset:{value:'completed'}}});state.pending.resolve([{id:500,status:1,tradeMode:'OFFLINE'}]);await more;
  assert.equal(page.data.orders.length,1);assert.equal(page.data.orders[0].id,99);assert.equal(page._page,1);
 });
 test(`${role}: account changes clear previous rows even on request failure`,async()=>{
  const {state,page}=harness(role);await page.loadOrders();state.token='owner-b';state.failure=true;await page.loadOrders();
  assert.equal(page.data.orders.length,0);assert.equal(page.data.hasMoreOrders,false);
 });
 test(`${role}: hiding cancels queued search and ignores late page results`,async()=>{
  const {state,page}=harness(role);await page.loadOrders();state.pending=deferred();const more=page.onReachBottom();
  page.onSearchChange({detail:{value:'target'}});page.onHide();const snapshot=JSON.stringify(page.data);
  state.pending.resolve([{id:500,status:1,tradeMode:'OFFLINE'}]);await more;await state.flush();
  assert.equal(JSON.stringify(page.data),snapshot);assert.equal(state.queries.length,2);
 });
}
