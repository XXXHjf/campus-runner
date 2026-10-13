const test = require('node:test');
const assert = require('node:assert/strict');
const vm = require('node:vm');
const fs = require('node:fs');
const path = require('node:path');
const feedbackStub = require('./helpers/feedbackStub.cjs');
const deferred = () => { let resolve; const promise = new Promise(yes => { resolve = yes; }); return {promise,resolve}; };
function harness(kind) {
 const state={token:'owner-a',queries:[],failure:false,pending:null,writes:[]}; let page;
 const records=Array.from({length:45},(_,i)=>({id:i+1,title:i===44?'target':'book',status:0}));records.push({id:99,title:'off shelf',status:4});
 const fetch=async query=>{
  state.queries.push({...query});
  if(state.failure)throw Error('network');
  if(state.pending && query.page===2)return state.pending.promise;
  const filtered=records.filter(p=>query.status===undefined||p.status===query.status).filter(p=>!query.keyword||p.title.includes(query.keyword));
  return filtered.slice((query.page-1)*20,query.page*20);
 };
 vm.runInNewContext(fs.readFileSync(path.resolve(__dirname,`../../apps/mini-program/pages/second-hand/${kind}/${kind}.js`),'utf8'),{
  Page:config=>{page={...config,data:structuredClone(config.data),setData:value=>Object.assign(page.data,value)};},
  require:id=>id.endsWith('tokenManager')?{getToken:()=>state.token}:id.endsWith('secondHandService')?
   {listMyProducts:fetch,listFavoriteProducts:fetch,unfavoriteProduct:async id=>state.writes.push(id)}:
   id.endsWith('feedback')?feedbackStub():{friendlyError:(_,fallback)=>fallback},
  wx:{getWindowInfo:()=>({windowWidth:375,statusBarHeight:20}),getMenuButtonBoundingClientRect:()=>({width:87,left:281,top:24,height:32}),stopPullDownRefresh(){}},
 });page.onLoad();return {page,state,load:()=>kind==='favorites'?page.loadFavorites():page.loadProducts()};
}
for(const kind of ['my-products','favorites']){
 test(`${kind}: loads every page and stops after the final short page`,async()=>{
  const h=harness(kind);await h.load();await h.page.onReachBottom();await h.page.onReachBottom();await h.page.onReachBottom();
  assert.equal(h.page.data.products.length,kind==='favorites'?46:45);assert.equal(h.state.queries.length,3);
  assert.equal(h.state.queries[2].page,3);assert.equal(h.page.data.hasMoreProducts,false);
 });
 test(`${kind}: failed page retries without discarding existing records`,async()=>{
  const h=harness(kind);await h.load();h.state.failure=true;await h.page.onReachBottom();
  assert.equal(h.page.data.products.length,20);h.state.failure=false;await h.page.onReachBottom();
  assert.equal(h.state.queries.at(-1).page,2);assert.equal(h.page.data.products.length,40);
 });
 test(`${kind}: account change clears records even if the replacement request fails`,async()=>{
  const h=harness(kind);await h.load();h.state.token='owner-b';h.state.failure=true;await h.load();
  assert.equal(h.page.data.products.length,0);
 });
 test(`${kind}: late subsequent page cannot overwrite a newer refresh`,async()=>{
  const h=harness(kind);await h.load();h.state.pending=deferred();const more=h.page.onReachBottom();
  await h.load();h.state.pending.resolve([{id:500,status:0}]);await more;
  assert.equal(h.page.data.products.length,20);assert.equal(h.page.data.products.some(p=>p.id===500),false);
 });
}
test('personal publishing switches tabs using a server status query from page one',async()=>{
 const h=harness('my-products');await h.load();await h.page.onReachBottom();
 await h.page.selectTab({currentTarget:{dataset:{tab:'offShelf'}}});
 assert.equal(h.state.queries.at(-1).status,4);assert.equal(h.state.queries.at(-1).page,1);
 assert.equal(h.page.data.filteredProducts.length,1);assert.equal(h.page.data.filteredProducts[0].id,99);
});
test('favorite search reaches an item outside the first page and preserves pending choices',async()=>{
 const h=harness('favorites');await h.load();h.page.toggleFavorite({currentTarget:{dataset:{id:1}}});
 await h.page.onSearchChange({detail:{value:'target'}});
 assert.equal(h.page.data.filteredProducts[0].id,45);assert.equal(h.state.queries.at(-1).page,1);
 await h.page.clearSearch();assert.equal(h.page.data.products[0].isFavorited,false);
 await h.page.onUnload();assert.deepEqual(h.state.writes,['1']);
});
test('favorite choices from the previous account are not committed on unload',async()=>{
 const h=harness('favorites');await h.load();h.page.toggleFavorite({currentTarget:{dataset:{id:1}}});h.state.token='owner-b';
 await h.page.onUnload();assert.deepEqual(h.state.writes,[]);
});
