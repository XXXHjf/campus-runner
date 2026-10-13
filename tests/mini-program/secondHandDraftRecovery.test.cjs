const assert = require('node:assert/strict');
const test = require('node:test');
const fs = require('node:fs');
const vm = require('node:vm');
const root = require('node:path').resolve(__dirname, '../../apps/mini-program');
const read = file => fs.readFileSync(require('node:path').join(root, file), 'utf8');
const tick = () => new Promise(resolve => setImmediate(resolve));
function deferred() { let resolve, reject; const promise = new Promise((yes, no) => {resolve=yes;reject=no;});return {promise,resolve,reject}; }
function harness() {
 const state = { owner:'1', version:0, cache:new Map(), notices:[], released:[], modalCount:0, modal:{confirm:true}, writes:[], write:null, networkFailure:false, originalId:9, navigated:0 };
 const listeners = new Set();
 const tokens = { getAccountId:()=>state.owner, getSessionVersion:()=>state.version, waitForToken:async()=>{}, onSessionChange:fn=>{listeners.add(fn);return()=>listeners.delete(fn);} };
 const wx = { getStorageSync:key=>state.cache.get(key), setStorageSync:(key,value)=>state.cache.set(key,structuredClone(value)),removeStorageSync:key=>state.cache.delete(key),setNavigationBarTitle(){} };
 function module(file, require) { const mod={exports:{}};vm.runInNewContext(read(file),{module:mod,require,wx,Date,console});return mod.exports; }
 const storage=module('utils/accountStorage.js',id=>id.endsWith('tokenManager')?tokens:require('../../apps/mini-program/utils/orderConstants'));
 const drafts=module('utils/secondHandDraftManager.js',()=>storage);
 const media={releaseTemporaryImage:async id=>state.released.push(id),usableTemporaryImages:async ids=>{
  if(state.networkFailure)throw Error('network');
  return ids.filter(id=>Number(id)!==3).map(mediaId=>({mediaId,previewUrl:`fresh-${mediaId}`}));
 }};
 const save=async payload=>{state.writes.push(payload);return state.write?state.write.promise:{id:11};};
 function page() {let page;vm.runInNewContext(read('pages/second-hand/publish/publish.js'),{
  wx,console,Page(config){page={...config,data:structuredClone(config.data),setData(update){for(const [key,value]of Object.entries(update)){const parts=key.replace(/\[(\d+)\]/g,'.$1').split('.');let target=this.data;for(const part of parts.slice(0,-1))target=target[part]||={};target[parts.at(-1)]=value;}}};},
  require(id){
   if(id.endsWith('addressPager'))return module('utils/addressPager.js',()=>tokens);
   if(id.endsWith('accountStorage'))return storage;if(id.endsWith('tokenManager'))return tokens;if(id.endsWith('secondHandDraftManager'))return drafts;if(id.endsWith('mediaService'))return media;
   if(id.endsWith('deliveryAddressService'))return {getMyAddresses:async()=>[]};
   if(id.endsWith('subscriptionService'))return {requestSecondHandOrder:async()=>{}};
   if(id.endsWith('feedback'))return {showModal:async()=>{state.modalCount++;return state.modal.promise || state.modal;},clearMessage(){},loaded(){},showMessage:(_,text)=>state.notices.push(text),showToast(){},loadError(){},navigate(){state.navigated++;}};
   if(id.endsWith('secondHandService'))return {listCategories:async()=>[{id:1,name:'分类'}],publishProduct:save,getPendingPublication:()=>state.pending || null,updateProduct:async(_,payload)=>save(payload),getProduct:async id=>({id,title:'SERVER',description:'SERVER DESCRIPTION',categoryId:1,price:10,pickupAddressSnapshot:'取货点',images:`original-${state.originalId}`,imageAssetIds:[state.originalId]})};
   throw Error(id);
  }
 });return page;}
 function changeOwner(owner,reason='accountChanged'){const previousAccountId=state.owner;state.owner=owner;state.version++;[...listeners].forEach(fn=>fn({previousAccountId,reason}));}
 return {state,storage,drafts,page,changeOwner};
}
function field(page,name,value){page.setField({currentTarget:{dataset:{field:name}},detail:{value}});}
function images(page,list){page.setData({fileList:list.map(item=>({status:'done',url:'old-preview',temporary:true,...item}))});page.syncImages();}

test('new draft survives leaving with text and image identity; valid previews are refreshed',async()=>{
 const h=harness();const first=h.page();await first.onLoad({});field(first,'title','DRAFT TITLE');field(first,'description','DRAFT DESCRIPTION');images(first,[{mediaId:2}]);first.onHide();first.onUnload();
 assert.deepEqual(h.state.released,[]);const second=h.page();await second.onLoad({});
 assert.equal(second.data.form.title,'DRAFT TITLE');assert.equal(second.data.form.description,'DRAFT DESCRIPTION');assert.equal(second.data.fileList[0].url,'fresh-2');assert.equal(second.data.form.imageAssetIds[0],2);
 const cached=h.drafts.load(null,h.storage.captureSession());assert.equal(cached.fileList[0].url,undefined);
 second.onUnload();
});
test('new and per-product editing drafts are separate; removed originals are not restored',async()=>{
 const h=harness();const fresh=h.page();await fresh.onLoad({});field(fresh,'title','NEW TITLE');fresh.onUnload();
 const edit=h.page();await edit.onLoad({id:'8'});field(edit,'title','EDIT TITLE');images(edit,[{mediaId:9,temporary:false},{mediaId:2},{mediaId:3}]);edit.onUnload();
 h.state.originalId=10;const restored=h.page();await restored.onLoad({id:'8'});
 assert.equal(restored.data.form.title,'EDIT TITLE');assert.deepEqual(Array.from(restored.data.form.imageAssetIds),[2]);assert.equal(h.drafts.load(null,h.storage.captureSession()).form.title,'NEW TITLE');assert.ok(h.state.notices.some(text=>text.includes('图片')));restored.onUnload();
});
test('image check failure preserves draft and blocks publishing',async()=>{
 const h=harness();const first=h.page();await first.onLoad({});field(first,'title','KEEP');images(first,[{mediaId:2}]);first.onUnload();h.state.networkFailure=true;
 const second=h.page();await second.onLoad({});assert.equal(second.data.form.title,'KEEP');assert.equal(second.data.fileList.length,1);await second.doSubmit();assert.equal(h.state.writes.length,0);second.onUnload();
});
test('reopening waits for an in-flight publish and successful completion clears its draft',async()=>{
 const h=harness();const first=h.page();await first.onLoad({});field(first,'title','PENDING');images(first,[{mediaId:2}]);h.state.write=deferred();const submitted=first.doSubmit();await tick();first.onUnload();
 const next=h.page();const loading=next.onLoad({});await tick();assert.equal(h.state.modalCount,0);h.state.write.resolve({id:11});await submitted;await loading;
 assert.equal(h.drafts.load(null,h.storage.captureSession()),null);assert.equal(next.data.form.title,'');assert.deepEqual(h.state.released,[]);next.onUnload();
});
for(const rejected of [false,true])test(`discarding a ${rejected?'rejected':'uncertain'} submission respects protected images`,async()=>{
 const h=harness();const first=h.page();await first.onLoad({});field(first,'title','KEEP');images(first,[{mediaId:2}]);h.state.write=deferred();const submitted=first.doSubmit();await tick();first.onUnload();h.state.write.reject(Object.assign(Error('failure'),rejected?{requestOutcome:'rejected'}:{}));await submitted;
 h.state.modal={confirm:false,cancel:true};const next=h.page();await next.onLoad({});assert.deepEqual(h.state.released,rejected?[2]:[]);assert.equal(h.drafts.load(null,h.storage.captureSession()),null);next.onUnload();
});
test('account changes clear page state, isolate caches and invalidate delayed restoration',async()=>{
 const h=harness();const first=h.page();await first.onLoad({});field(first,'title','ACCOUNT A');first.onUnload();
 h.state.modal=deferred();const second=h.page();const loading=second.onLoad({});await tick();h.changeOwner('2');h.state.modal.resolve({confirm:true});await loading;
 assert.equal(second.data.form.title,'');assert.equal(h.drafts.load(null,h.storage.captureSession()),null);
 h.changeOwner('1');h.changeOwner(null,'logout');assert.equal(h.state.cache.has('secondHandDrafts:1'),false);second.onUnload();
});
test('draft expiry and storage limits preserve the newest editable entry',()=>{
 const h=harness();const session=h.storage.captureSession();const data={form:{title:'TEXT'},fileList:[]};
 for(let id=1;id<=25;id++)h.drafts.save(id,data,session);
 assert.ok(h.drafts.load(25,session));assert.equal(Object.keys(h.storage.read('secondHandDrafts',session).entries).length,20);
 const cache=h.state.cache.get('secondHandDrafts:1');cache.entries['edit:25'].timestamp=Date.now()-8*24*3600000;assert.equal(h.drafts.load(25,session),null);
});

test('a delayed successful write after switching accounts cannot clear the new account draft or navigate',async()=>{
 const h=harness();const page=h.page();await page.onLoad({});field(page,'title','OLD');images(page,[{mediaId:2}]);h.state.write=deferred();const submitted=page.doSubmit();await tick();h.changeOwner('2');await page.onShow();field(page,'title','NEW');h.state.write.resolve({id:11});await submitted;
 assert.equal(page.data.form.title,'NEW');assert.equal(h.drafts.load(null,h.storage.captureSession()).form.title,'NEW');assert.equal(h.state.navigated,0);page.onUnload();
});
test('clearing a submitted revision cannot erase a newer saved revision',()=>{
 const h=harness();const session=h.storage.captureSession();const first=h.drafts.save(null,{form:{title:'FIRST'},fileList:[]},session);h.drafts.save(null,{form:{title:'SECOND'},fileList:[]},session);h.drafts.clear(null,session,first);assert.equal(h.drafts.load(null,session).form.title,'SECOND');
});

test('a stale rejected write cannot copy its protected images into a new account draft',async()=>{
 const h=harness();const page=h.page();await page.onLoad({});field(page,'title','A');images(page,[{mediaId:2}]);h.state.write=deferred();const submitted=page.doSubmit();await tick();h.changeOwner('2');await page.onShow();field(page,'title','B');h.state.write.reject(Object.assign(Error('rejected'),{requestOutcome:'rejected'}));await submitted;
 const draft=h.drafts.load(null,h.storage.captureSession());assert.equal(draft.form.title,'B');assert.equal(draft.protectedImageIds.length,0);page.onUnload();
});


test('uncertain publication replays original content even when bound images cannot be refreshed',async()=>{
 const h=harness();const page=h.page();await page.onLoad({});
 h.state.pending={clientRequestId:'original-request-0001',title:'ORIGINAL',description:'original description',imageAssetIds:[2],price:10};
 h.state.networkFailure=true;
 page.refreshDraftImages=async()=>assert.fail('must not revalidate bound images');
 await page.doSubmit();
 assert.equal(h.state.writes[0].title,'ORIGINAL');
 assert.equal(h.state.writes[0].clientRequestId,'original-request-0001');
 assert.equal(h.state.writes[0].imageAssetIds[0],2);
});
