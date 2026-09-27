const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const vm = require('node:vm');
const root = path.resolve(__dirname, '../../apps/mini-program');

function harness() {
  const calls = [];
  let decision;
  let component;
  const dialog = { close() {} };
  const children = {
    '#feedback-toast': { hide() { calls.push(['hideToast']); } },
    '#feedback-message': { hide() {} },
    '#feedback-dialog': dialog,
  };
  const host = { active: true, feedbackReady: true, data: { top: 80 }, selectComponent: id => children[id] };
  const page = { route: 'current', data: {}, selectComponent: () => host,
    setData(value) { Object.assign(this.data, value); } };
  let current = page;
  const module = { exports: {} };
  const source = fs.readFileSync(path.join(root, 'utils/feedback.js'), 'utf8').replace(/^import .*;\n/gm, '');
  const Message = Object.fromEntries(['info', 'success', 'warning', 'error'].map(theme => [theme, options => calls.push(['message', theme, options])]));
  Message.hide = () => calls.push(['hideMessage']);
  const Dialog = Object.fromEntries(['alert', 'confirm'].map(method => [method, options => {
    calls.push(['dialog', method, options]);
    return new Promise((resolve, reject) => { decision = { resolve, reject }; });
  }]));
  const wx = { nextTick: callback => callback(), navigateBack: options => { calls.push(['navigate']); current = { ...page, route: 'destination' }; options.success({}); } };
  vm.runInNewContext(source, { module, Date, Promise, setTimeout, clearTimeout, Toast: options => calls.push(['toast', options]), Message, Dialog, wx, getCurrentPages: () => [current] });
  vm.runInNewContext(fs.readFileSync(path.join(root, 'components/ui-feedback/ui-feedback.js'), 'utf8'), {
    Component: value => { component = value; }, wx, Date, clearTimeout, require: () => module.exports,
  });
  Object.assign(host, component.methods);
  host.ownerPage = page;
  return { feedback: module.exports, page, host, calls, component, decision: () => decision, leave: () => { current = { ...page }; } };
}

test('short results use semantic icons, fixed durations and no blocking overlay', () => {
  const h = harness();
  h.feedback.showToast(h.page, { title: '已保存', icon: 'success', mask: true, duration: 99999 });
  let options = h.calls.at(-1)[1];
  assert.equal(options.duration, 2000);
  assert.equal(options.direction, 'row');
  assert.equal(options.icon, 'check-circle');
  assert.equal(options.showOverlay, false);
  assert.equal(options.preventScrollThrough, false);
  h.feedback.showToast(h.page, { title: '保存失败，请重试', theme: 'error' });
  options = h.calls.at(-1)[1];
  assert.equal(options.duration, 3000);
  assert.equal(options.icon, 'close-circle');
});

test('duplicates, hidden pages and modal background feedback are suppressed', async () => {
  const h = harness();
  h.feedback.showToast(h.page, { title: '已保存' });
  h.feedback.showToast(h.page, { title: '已保存' });
  assert.equal(h.calls.filter(call => call[0] === 'toast').length, 1);
  const modal = h.feedback.showModal(h.page, { title: '删除地址？' });
  h.feedback.showToast(h.page, { title: '异步操作成功' });
  assert.equal(h.calls.filter(call => call[0] === 'toast').length, 1);
  h.decision().reject();
  assert.equal((await modal).cancel, true);
  h.leave();
  h.feedback.showToast(h.page, { title: '保存失败' });
  assert.equal(h.calls.filter(call => call[0] === 'toast').length, 1);
});

test('persistent states retain priority, use no timer and clear only by their key', () => {
  const h = harness();
  h.feedback.showMessage(h.page, '支付已完成，订单状态待更新', { persistent: true, key: 'payment', action: '刷新' });
  const options = h.calls.at(-1)[2];
  assert.equal(options.duration, 0);
  assert.equal(options.closeBtn, false);
  assert.equal(options.offset[0], 80);
  h.feedback.showMessage(h.page, '普通状态');
  assert.equal(h.calls.filter(call => call[0] === 'message').length, 1);
  h.feedback.clearMessage(h.page, 'fee');
  assert.equal(h.host.notice.key, 'payment');
  h.feedback.clearMessage(h.page, 'payment');
  assert.equal(h.host.notice, null);
});

test('confirm and cancel preserve callback results; alert resolves as cancelled on leaving', async () => {
  const h = harness();
  const results = [];
  const pending = h.feedback.showModal(h.page, { title: '删除地址？', danger: true,
    confirmText: '删除', success: result => results.push(result.confirm) });
  const options = h.calls.find(call => call[0] === 'dialog')[2];
  assert.equal(options.confirmBtn.theme, 'danger');
  assert.equal(options.closeOnOverlayClick, false);
  assert.equal(options.showOverlay, true);
  h.decision().resolve();
  assert.equal((await pending).confirm, true);
  assert.deepEqual(results, [true]);
  const alert = h.feedback.showModal(h.page, { title: '重要告知', showCancel: false });
  h.host.cleanup();
  assert.equal((await alert).confirm, false);
});

test('technical and overlong errors never leak into feedback, long readable notices use Message', () => {
  const h = harness();
  assert.equal(h.feedback.userText('NO_TOKEN: missing token'), '操作失败，请重试');
  assert.equal(h.feedback.userText('数据库连接失败'), '操作失败，请重试');
  assert.equal(h.feedback.userText('内容'.repeat(30)), '操作失败，请重试');
  h.feedback.showToast(h.page, { title: '请选择合适的送达时间并检查订单中填写的地址信息是否完整' });
  assert.equal(h.calls.at(-1)[0], 'message');
});

test('initial load errors expose retry while failed refreshes preserve content', () => {
  const h = harness();
  let retried = 0;
  h.feedback.loadError(h.page, '订单加载失败，请重试', () => retried++);
  assert.equal(h.page.data.feedbackLoadError, '订单加载失败，请重试');
  h.page._feedbackRetry();
  assert.equal(retried, 1);
  h.feedback.loaded(h.page);
  h.feedback.loadError(h.page, '订单加载失败', () => retried++, true);
  assert.equal(h.page.data.feedbackLoadError, '');
  assert.equal(h.calls.at(-1)[2].link.content, '重试');
});

test('navigation feedback is shown on the destination without waiting for a toast timer', () => {
  const h = harness();
  h.feedback.navigate(h.page, 'navigateBack', {}, '已保存');
  assert.equal(h.calls[0][0], 'navigate');
  assert.equal(h.calls.at(-1)[0], 'toast');
  assert.equal(h.calls.at(-1)[1].message, '已保存');
});

test('a dismissed or duplicate dialog cannot execute the user cancel branch', async () => {
  const h = harness();
  let cancelEffects = 0;
  const pending = h.feedback.showModal(h.page, { title: '恢复草稿？', success: result => { if (!result.confirm) cancelEffects++; } });
  const duplicate = await h.feedback.showModal(h.page, { title: '恢复草稿？', success: () => cancelEffects++ });
  assert.equal(duplicate.dismissed, true);
  h.host.cleanup();
  assert.equal((await pending).dismissed, true);
  assert.equal(cancelEffects, 0);
});

test('feedback waits for destination children to be ready, and deferred dialogs dismiss safely', async () => {
  const h = harness();
  h.host.feedbackReady = false;
  h.feedback.showToast(h.page, { title: '已保存' });
  assert.equal(h.calls.length, 0);
  h.component.lifetimes.ready.call(h.host);
  assert.equal(h.calls.at(-1)[1].message, '已保存');
  h.host.feedbackReady = false;
  let cancelled = 0;
  const pending = h.feedback.showModal(h.page, { title: '恢复草稿？', success: () => cancelled++ });
  h.host.cleanup();
  assert.equal((await pending).dismissed, true);
  assert.equal(cancelled, 0);
});

test('failed confirmation keeps its context for retry and prevents repeated submissions', async () => {
  const h = harness();
  let attempts = 0;
  let finishRequest;
  let cancelled = 0;
  const pending = h.feedback.showModal(h.page, { title: '取消订单？', content: '取消后无法恢复。',
    success: async result => {
      if (!result.confirm) return;
      attempts++;
      await new Promise(resolve => { finishRequest = resolve; });
      h.feedback.showToast(h.page, { title: '取消失败，请重试', theme: 'error' });
    } });
  h.decision().resolve();
  await new Promise(resolve => setImmediate(resolve));
  const duplicate = await h.feedback.showModal(h.page, { title: '取消订单？', success: () => cancelled++ });
  assert.equal(duplicate.dismissed, true);
  finishRequest();
  await new Promise(resolve => setImmediate(resolve));
  const retry = h.calls.filter(call => call[0] === 'dialog').at(-1);
  assert.equal(retry[2].title, '取消订单？');
  assert.match(retry[2].content, /取消后无法恢复。\n\n取消失败，请重试/);
  assert.equal(h.calls.filter(call => call[0] === 'toast').length, 0);
  h.decision().reject();
  assert.equal((await pending).cancel, true);
  assert.equal(attempts, 1);
  assert.equal(cancelled, 0);
});

test('an old Message closing animation cannot swallow a replacement status', async () => {
  const h = harness();
  h.feedback.showMessage(h.page, '费用暂时无法获取', { persistent: true, key: 'fee' });
  h.feedback.clearMessage(h.page, 'fee');
  h.feedback.showMessage(h.page, '支付已完成，订单状态待更新', { persistent: true, key: 'payment' });
  assert.equal(h.calls.filter(call => call[0] === 'message').length, 1);
  await new Promise(resolve => setTimeout(resolve, 520));
  const messages = h.calls.filter(call => call[0] === 'message');
  assert.equal(messages.length, 2);
  assert.equal(messages.at(-1)[2].content, '支付已完成，订单状态待更新');
});

test('all page feedback hosts are registered and native feedback calls are absent', () => {
  const app = JSON.parse(fs.readFileSync(path.join(root, 'app.json')));
  assert.equal(app.usingComponents['ui-feedback'], '/components/ui-feedback/ui-feedback');
  for (const route of app.pages) {
    const wxml = fs.readFileSync(path.join(root, route + '.wxml'), 'utf8');
    const js = fs.readFileSync(path.join(root, route + '.js'), 'utf8');
    assert.equal((wxml.match(/<ui-feedback\b/g) || []).length, 1, route);
    assert.doesNotMatch(js, /wx\.(showToast|showModal)\s*\(/, route);
    assert.doesNotMatch(wxml, /<t-toast\b|<t-dialog\b/, route);
    if (/feedback\.loadError\(/.test(js)) assert.match(js, /retryFeedbackLoad\(\)/, route);
    for (const match of js.matchAll(/onAction:\s*\(\)\s*=>\s*this\.(\w+)\(/g)) {
      assert.match(js, new RegExp('\\b' + match[1] + '\\s*\\([^)]*\\)\\s*\\{'), `${route}: ${match[1]}`);
    }
  }
});
