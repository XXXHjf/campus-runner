const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
function harness(check) {
 let page;
 const notices = [];
 let current = true;
 vm.runInNewContext(fs.readFileSync(require.resolve('../../apps/mini-program/pages/orders/myOrders/ordersAdd/add.js'),'utf8'), {
  console, getApp: () => ({globalData:{}}), Page(config) { page={...config,data:structuredClone(config.data),setData(update){Object.assign(this.data,update);}}; },
  require(id) {
   if(id.endsWith('orderConstants')) return require('../../apps/mini-program/utils/orderConstants');
   if(id.endsWith('accountStorage')) return {captureSession:()=>({ownerId:'1'}),isCurrent:()=>current};
   if(id.endsWith('mediaService')) return {usableTemporaryImages:check};
   if(id.endsWith('feedback')) return {showMessage:(_,text)=>notices.push(text),clearMessage(){}};
   return {};
  }
 });
 page.buttonColor=()=>{}; page._saveDraftData=()=>{};
 page.setData({note:'KEEP_NOTE',showPhone:'KEEP_PHONE',fileList:[{mediaId:1,status:'done',url:'old-1'},{mediaId:2,status:'done',url:'old-2'}]});
 return {page,notices,switchAccount(){current=false;}};
}
test('draft check removes only expired assets and refreshes usable previews without losing form content',async()=>{
 const h=harness(async(ids,purpose)=>{assert.equal(purpose,'ORDER_IMAGE');assert.deepEqual(Array.from(ids),[1,2]);return [{mediaId:2,previewUrl:'fresh-2',expiresAt:'future'}];});
 assert.equal(await h.page._refreshDraftImages(),false);
 assert.equal(h.page.data.fileList.length,1); assert.equal(h.page.data.fileList[0].mediaId,2); assert.equal(h.page.data.fileList[0].url,'fresh-2');
 assert.equal(h.page.data.note,'KEEP_NOTE'); assert.equal(h.page.data.showPhone,'KEEP_PHONE'); assert.equal(h.notices[0],'部分图片已失效，请重新上传');
});
test('failed checks preserve images and form content and do not allow submission',async()=>{
 const h=harness(async()=>{throw Error('network');});
 assert.equal(await h.page._refreshDraftImages(),false);assert.equal(h.page.data.fileList.length,2);assert.equal(h.page.data.note,'KEEP_NOTE');
});
for(const leave of ['account','page']) test(`late image checks cannot mutate a changed ${leave}`,async()=>{
 let resolve;const h=harness(()=>new Promise(done=>resolve=done));const pending=h.page._refreshDraftImages();
 if(leave==='account')h.switchAccount();else h.page._unloaded=true;
 resolve([]);assert.equal(await pending,false);assert.equal(h.page.data.fileList.length,2);assert.deepEqual(h.notices,[]);
});
test('an empty draft does not require a media request',async()=>{
 const h=harness(async()=>{throw Error('unexpected request');});h.page.data.fileList=[];assert.equal(await h.page._refreshDraftImages(),true);
});

test('legacy or unfinished cached images require reupload without losing text',async()=>{
 const h=harness(async()=>{throw Error('unexpected request');});
 h.page.data.fileList=[{url:'old-signed-url',status:'done'},{url:'old-local-url',status:'loading'}];
 assert.equal(await h.page._refreshDraftImages(true),false); assert.equal(h.page.data.fileList.length,0);assert.equal(h.page.data.note,'KEEP_NOTE');
 assert.equal(h.notices[0],'部分图片已失效，请重新上传');
});
