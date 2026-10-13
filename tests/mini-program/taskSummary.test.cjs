const test=require('node:test');const assert=require('node:assert/strict');
const fs=require('node:fs');const path=require('node:path');const vm=require('node:vm');
const root=path.resolve(__dirname,'../../apps/mini-program');
const summary={runnerReceivableCount:75,runnerReceivableOrderId:null,runnerUnpaidCount:1,runnerUnpaidOrderId:10,secondHandBuyerCount:60,secondHandBuyerNoticeCount:45,secondHandSellerCount:50,bargainPendingCount:80};
const deferred=()=>{let resolve;const promise=new Promise(yes=>resolve=yes);return {promise,resolve};};
function moduleHarness(file,requireFn,extra={}) {const module={exports:{}};vm.runInNewContext(fs.readFileSync(path.join(root,file),'utf8'),{module,exports:module.exports,require:requireFn,getApp:()=>({globalData:{API_URL:'https://example.test'}}),...extra});return module.exports;}
test('summary validates complete counts instead of estimating from the first page',async()=>{
 let response=summary;const api=moduleHarness('services/taskSummaryService.js',name=>name.endsWith('tokenManager')?{getToken:()=> 'a'}:{request:async()=>response,requiredObject:r=>r});
 assert.equal((await api.getSummary()).runnerReceivableCount,75);
 for(const bad of [{...summary,runnerUnpaidOrderId:null},{...summary,secondHandBuyerCount:-1},{...summary,bargainPendingCount:'80'}]){response=bad;await assert.rejects(api.getSummary(),/待办加载失败/);}
});
test('summary rejects a response from a previous account session',async()=>{
 let token='a';const pending=deferred();const api=moduleHarness('services/taskSummaryService.js',name=>name.endsWith('tokenManager')?{getToken:()=>token}:{request:()=>pending.promise,requiredObject:r=>r});
 const loading=api.getSummary();token='b';pending.resolve(summary);await assert.rejects(loading,/登录状态已变化/);
});
function badgeHarness(){let token='a',response=summary,calls=0;const dots=[];
 const api=moduleHarness('services/mineTabBadgeService.js',name=>name.endsWith('tokenManager')?{getToken:()=>token,hasToken:()=>!!token}:name.endsWith('profileStatus')?{isProfileComplete:()=>true}:name.endsWith('taskSummaryService')?{getSummary:async()=>{calls++;return response;}}:{getUserInfo:async()=>({id:1,authentication:1})},
 {Date,wx:{showTabBarRedDot:()=>dots.push(true),hideTabBarRedDot:()=>dots.push(false)}});
 return {api,dots,setToken:t=>token=t,setResponse:r=>response=r,calls:()=>calls};}
test('badge uses full counts and retains the existing buyer-notice selection',async()=>{
 const h=badgeHarness();const result=await h.api.refreshMineTabRedDot();assert.equal(result.totalCount,251);await h.api.refreshMineTabRedDot();assert.equal(h.calls(),1);assert.equal(h.dots.at(-1),true);
});
test('badge cache and pending reads do not cross accounts',async()=>{
 const h=badgeHarness(),old=deferred();h.setResponse(old.promise);const first=h.api.refreshMineTabRedDot();
 h.setToken('b');h.setResponse({...summary,runnerReceivableCount:0,runnerUnpaidCount:0,secondHandBuyerNoticeCount:0,secondHandSellerCount:0,bargainPendingCount:0});
 await h.api.refreshMineTabRedDot();old.resolve(summary);await first;assert.equal(h.dots.at(-1),false);assert.equal(h.calls(),2);
});
function mineHarness(){let token='a',response=summary,page;const routes=[],queries=[],dots=[],messages=[];
 vm.runInNewContext(fs.readFileSync(path.join(root,'pages/mine/mine/mine.js'),'utf8'),{
  console,Date,getApp:()=>({globalData:{}}),require:name=>name.endsWith('tokenManager')?{getToken:()=>token,hasToken:()=>!!token}:name.endsWith('taskSummaryService')?{getSummary:async()=>response}:name.endsWith('feedback')?{showMessage:(_,text)=>messages.push(text)}:name.endsWith('userOrderService')?{getMyOrders:async q=>{queries.push(q);return [];}}:name.endsWith('mineTabBadgeService')?{setMineTabRedDot:n=>dots.push(n),clearMineTabRedDot:()=>dots.push(0)}:{},
  wx:{navigateTo:o=>routes.push(o.url)},Page:config=>{page={...config,data:structuredClone(config.data),setData:update=>Object.assign(page.data,update)};}
 });page._waitForStartupLogin=async()=>{};page.getGlobalData=async()=>({authentication:1});
 return {page,routes,queries,dots,messages,setToken:t=>token=t,setResponse:r=>response=r};}
test('mine counts remain complete and only a sole task enters detail directly',async()=>{
 const h=mineHarness();await h.page.onShow();assert.equal(h.page.data.notReceiveOrderCount,75);assert.equal(h.page.data.secondHandOrderCount,60);assert.equal(h.page.data.unpaidOrderCount,1);
 assert.equal(h.queries[0].group,'unpaid');assert.equal(h.queries[0].pageSize,20);
 h.page.tapToReceive();assert.match(h.routes.at(-1),/takesShow/);h.page.tapToUnpaid();assert.match(h.routes.at(-1),/info\?id=10/);
});
test('mine ignores late summary after leaving or switching account',async()=>{
 for(const kind of ['hide','account']) {const h=mineHarness(),pending=deferred();h.setResponse(pending.promise);const loading=h.page.onShow();await new Promise(resolve=>setImmediate(resolve));
  if(kind==='hide')h.page.onHide();else h.setToken('b');pending.resolve(summary);await loading;assert.equal(h.page.data.notReceiveOrderCount,0);assert.equal(h.dots.every(value=>value===0),true);
 }
});

test('summary failure preserves loaded profile and provides a retry message',async()=>{
 const h=mineHarness();h.page.setData({userInfo:{id:1,authentication:1}});const pending=deferred();h.setResponse(pending.promise.then(()=>{throw Error('network');}));
 const loading=h.page.onShow();await new Promise(resolve=>setImmediate(resolve));pending.resolve();await loading;assert.equal(h.page.data.userInfo.id,1);assert.equal(h.messages[0],'待办加载失败，请重试');
});
test('entering with another account clears old task counts before the new request finishes',async()=>{
 const h=mineHarness();await h.page.onShow();const pending=deferred();h.setToken('b');h.setResponse(pending.promise);
 const loading=h.page.onShow();await new Promise(resolve=>setImmediate(resolve));assert.equal(h.page.data.notReceiveOrderCount,0);assert.equal(h.page.data.unpaidOrderList.length,0);
 pending.resolve({...summary,runnerReceivableCount:1,runnerReceivableOrderId:222});await loading;h.page.tapToReceive();assert.match(h.routes.at(-1),/info\?id=222/);
});
