const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
const root = path.resolve(__dirname, '../../apps/mini-program');
const pager = require('./helpers/addressPagerHarness.cjs');
const view = () => ({ data: { addresses: [] }, setData(update) { Object.assign(this.data, update); } });
test('all 65 addresses remain reachable and duplicate loads share the page state', async () => {
  const { loadAddressPage } = pager({ getToken: () => 'a' }); const owner = view();
  const rows = Array.from({ length: 65 }, (_, i) => ({ id: i + 1 })); const calls = [];
  const fetch = async query => { calls.push(query.page); return rows.slice((query.page-1)*20, query.page*20); };
  await loadAddressPage(owner, 'addresses', fetch);
  await Promise.all([loadAddressPage(owner, 'addresses', fetch, false), loadAddressPage(owner, 'addresses', fetch, false)]);
  await loadAddressPage(owner, 'addresses', fetch, false); await loadAddressPage(owner, 'addresses', fetch, false);
  await loadAddressPage(owner, 'addresses', fetch, false);
  assert.equal(owner.data.addresses.length, 65); assert.deepEqual(calls, [1,2,3,4]);
});
test('failed continuation preserves records and retries the same page', async () => {
  const { loadAddressPage } = pager({ getToken: () => 'a' }); const owner = view(); let fail = false; const calls = [];
  const fetch = async query => { calls.push(query.page); if (fail) throw Error('offline'); return Array.from({length:20},(_,i)=>({id:(query.page-1)*20+i+1})); };
  await loadAddressPage(owner,'addresses',fetch);fail=true;
  await assert.rejects(loadAddressPage(owner,'addresses',fetch,false));assert.equal(owner.data.addresses.length,20);
  fail=false;await loadAddressPage(owner,'addresses',fetch,false);assert.deepEqual(calls,[1,2,2]);assert.equal(owner.data.addresses.length,40);
});
test('refresh and login change reject late private-address results', async () => {
  let token = 'a', version = 0, resolve;
  const {loadAddressPage}=pager({getToken:()=>token,getSessionVersion:()=>version});const owner=view();
  const pending=loadAddressPage(owner,'addresses',()=>new Promise(yes=>{resolve=yes;}));
  await loadAddressPage(owner,'addresses',async()=>[{id:99}]);resolve([{id:1}]);await pending;
  assert.equal(owner.data.addresses[0].id,99);
  const old=loadAddressPage(owner,'addresses',()=>new Promise(yes=>{resolve=yes;}));version++;owner.setData({addresses:[]});resolve([{id:1}]);await old;assert.equal(owner.data.addresses.length,0);
  token=null;let calls=0;await loadAddressPage(owner,'addresses',async()=>{calls++;return [];});assert.equal(calls,0);
});
test('reference reads use bounded pages and request buildings only for the selected campus', async () => {
  const calls=[]; const mod={exports:{}};
  vm.runInNewContext(fs.readFileSync(path.join(root,'services/addressService.js'),'utf8'),{module:mod,getApp:()=>({globalData:{API_URL:''}}),require:id=>id.endsWith('tokenManager')?{getToken:()=> 'a'}:{
    request:async o=>{calls.push(o.data);const first=o.data.page===1;return {data:{code:1,data:{school:[],compus:[],buildCategory:[],building:o.data.compusNumberId?Array.from({length:first?50:7},(_,i)=>({numberId:i+o.data.page*100,buildingName:'楼'})):[]}}};},
    requiredObject:res=>res.data.data,
  }});
  const base=await mod.exports.getThreeLevelAddress();assert.equal(base.building.length,0);assert.equal(calls[0].compusNumberId,undefined);
  const campus=await mod.exports.getThreeLevelAddress(1010010000000);assert.equal(campus.building.length,57);assert.equal(calls[1].compusNumberId,1010010000000);assert.equal(calls[2].page,2);assert.equal(calls[2].pageSize,50);
});

test('configuration paging never exposes a partial set or loops on an incompatible old server', async () => {
  const mod={exports:{}};vm.runInNewContext(fs.readFileSync(path.join(root,'services/request.js'),'utf8'),{module:mod,require:()=>({getToken:()=> 'a'})});
  const {collectListPages}=mod.exports;const first=Array.from({length:50},(_,i)=>({id:i}));
  const rows=await collectListPages(async query=>query.page===1?first:[{id:51}]);assert.equal(rows.length,51);
  let calls=0;await assert.rejects(collectListPages(async()=>{calls++;return first;}));assert.equal(calls,2);
  await assert.rejects(collectListPages(async query=>{if(query.page===2)throw Error('offline');return first;}));
});
