import Toast from 'tdesign-miniprogram/toast/index';
import Message from 'tdesign-miniprogram/message/index';
import Dialog from 'tdesign-miniprogram/dialog/index';

function currentPage() {
  const pages = getCurrentPages();
  return pages[pages.length - 1];
}

function host(context) {
  const page = currentPage();
  if (!page || (context && context.route && context !== page)) return null;
  return (context || page).selectComponent?.('#ui-feedback') || null;
}

// A navigation success callback can run before the destination's children are
// ready. Keep feedback on its originating page until its host is ready.
function deferUntilReady(context, run, dismiss = () => {}) {
  const page = currentPage();
  if (!page || (context && context !== page) || host(page)?.feedbackReady) return false;
  if (!page._feedbackPending) page._feedbackPending = [];
  page._feedbackPending.push({ run, dismiss });
  return true;
}

function flushPending(page, dismissed = false) {
  const pending = page?._feedbackPending || [];
  if (page) page._feedbackPending = [];
  pending.forEach(item => (dismissed ? item.dismiss() : item.run()));
}

function userText(value, fallback = '操作失败，请重试', allowLong = false) {
  const text = String(value || '').trim();
  if (!text || (!allowLong && text.length > 36) || !/[\u4e00-\u9fff]/.test(text) || /token|接口|数据库|字段|路由|后端|exception|request:fail|https?:\/\//i.test(text) || /\b[A-Z]+(?:_[A-Z]+)+\b/.test(text)) return fallback;
  return text;
}

function showToast(context, options = {}) {
  if (deferUntilReady(context, () => showToast(context, options))) return;
  const instance = host(context);
  if (!instance || !instance.active || instance.dialogOpen) return;
  const message = userText(options.title || options.message);
  const theme = options.theme || (options.icon === 'success' || options.icon === 'check-circle' ? 'success'
    : options.icon === 'error' || options.icon === 'close-circle' ? 'error'
      : options.icon === 'error-circle' ? 'warning' : 'info');
  const actions = instance.modalActions;
  const action = actions && actions[actions.length - 1];
  if (action && theme === 'error') {
    action.error = message;
    return;
  }
  if (message.length > 24) return showMessage(context, message, { theme });
  const icons = { success: 'check-circle', error: 'close-circle', warning: 'error-circle', info: '' };
  if (instance.lastText === message && Date.now() - instance.lastTime < 1500) return;
  instance.lastText = message;
  instance.lastTime = Date.now();
  Toast({ context: instance, selector: '#feedback-toast', message, icon: icons[theme] || '',
    direction: 'row', placement: 'middle', duration: ['error', 'warning'].includes(theme) ? 3000 : 2000,
    showOverlay: false, preventScrollThrough: false });
}

function toast(options = {}) {
  return showToast(options.context, options);
}

function showMessage(context, content, options = {}) {
  if (deferUntilReady(context, () => showMessage(context, content, options))) return;
  const instance = host(context);
  if (!instance || !instance.active || instance.dialogOpen) return;
  if (instance.notice?.persistent && !options.persistent && !options.replace) return;
  const theme = options.theme || 'warning';
  instance.notice = { content: userText(content), ...options, theme };
  // TDesign removes a hidden single message after its closing animation.
  // Wait for that removal before reusing the same message id.
  const remaining = (instance.messageResetUntil || 0) - Date.now();
  if (remaining > 0) {
    const notice = instance.notice;
    clearTimeout(instance.messageTimer);
    instance.messageTimer = setTimeout(() => {
      if (instance.active && instance.notice === notice) showMessage(context, content, options);
    }, remaining);
    return;
  }
  instance.selectComponent('#feedback-toast')?.hide();
  Message[theme]({ context: instance, selector: '#feedback-message', content: instance.notice.content,
    duration: options.persistent ? 0 : 4000, icon: true, closeBtn: !options.persistent,
    single: true, marquee: false, offset: [instance.data.top, 12],
    link: options.action ? { content: options.action } : undefined });
}

function clearMessage(context, key) {
  const instance = host(context);
  if (!instance || (key && instance.notice?.key !== key)) return;
  instance.notice = null;
  instance.messageResetUntil = Date.now() + 500;
  Message.hide({ context: instance, selector: '#feedback-message' });
}

function loadError(context, content, retry, hasContent = false) {
  if (context !== currentPage()) return;
  context.setData({ feedbackLoadError: hasContent ? '' : userText(content, '加载失败，请重试') });
  context._feedbackRetry = retry;
  if (hasContent) showMessage(context, '刷新失败，请重试', { theme: 'error', key: 'load', action: '重试', onAction: retry });
}

function loaded(context) {
  context.setData({ feedbackLoadError: '' });
  clearMessage(context, 'load');
}

function navigate(context, method, options, message, theme = 'success') {
  if (context !== currentPage()) return;
  const onSuccess = options.success;
  wx[method]({ ...options, success: result => {
    onSuccess?.(result);
    if (message) wx.nextTick(() => showToast(currentPage(), { title: message, theme }));
  } });
}

async function showModal(context, options = {}) {
  const page = currentPage();
  if ((!context || context === page) && page && !host(page)?.feedbackReady) {
    return new Promise(resolve => {
      deferUntilReady(context, () => resolve(showModal(context, options)), () => {
        const result = { confirm: false, cancel: false, dismissed: true };
        options.fail?.(result);
        options.complete?.(result);
        resolve(result);
      });
    });
  }
  const instance = host(context);
  const dialog = instance?.selectComponent('#feedback-dialog');
  if (!instance || !dialog || !instance.active || instance.dialogOpen || (instance.modalActions?.length && !options.allowDuringAction)) {
    const result = { confirm: false, cancel: false, dismissed: true };
    options.fail?.(result);
    options.complete?.(result);
    return result;
  }
  instance.dialogOpen = true;
  instance.selectComponent('#feedback-toast')?.hide();
  Message.hide({ context: instance, selector: '#feedback-message' });
  instance.messageResetUntil = Date.now() + 500;
  const config = { context: instance, selector: '#feedback-dialog', title: options.title || '请确认',
    content: String(options.content || ''), confirmBtn: options.confirmText || (options.showCancel === false ? '知道了' : '确认'),
    cancelBtn: options.cancelText || '取消', showOverlay: true, preventScrollThrough: true,
    closeOnOverlayClick: false, zIndex: 16000 };
  if (['#e34d59', '#d54941'].includes(options.confirmColor) || options.danger) {
    config.confirmBtn = { content: options.confirmText || '确认', theme: 'danger' };
  }
  let confirmed = false;
  try {
    dialog._onConfirm = null;
    dialog._onCancel = null;
    const decision = options.showCancel === false ? Dialog.alert(config) : Dialog.confirm(config);
    confirmed = await Promise.race([
      decision.then(() => true, () => false),
      new Promise(resolve => { instance.cancelDialog = () => resolve(false); }),
    ]);
  } catch (_) {
    // Dialog.confirm rejects on cancel. Missing hosts never fall back to native UI.
  }
  instance.dialogOpen = false;
  instance.cancelDialog = null;
  const result = { confirm: confirmed && instance.active, cancel: !confirmed && instance.active, dismissed: !instance.active };
  const action = { error: '' };
  if (!instance.modalActions) instance.modalActions = [];
  instance.modalActions.push(action);
  try {
    if (result.dismissed) options.fail?.(result);
    else await options.success?.(result);
  } finally { instance.modalActions.pop(); }
  if (result.confirm && action.error && instance.active) {
    const originalContent = options.originalContent ?? options.content ?? '';
    const retryResult = await showModal(context, { ...options, originalContent,
      content: `${originalContent}\n\n${action.error}`, complete: undefined });
    options.complete?.(retryResult);
    return retryResult;
  }
  options.complete?.(result);
  if (instance.active && instance.notice?.persistent) {
    showMessage(context, instance.notice.content, instance.notice);
  }
  return result;
}

module.exports = { currentPage, userText, showToast, toast, showMessage, clearMessage, showModal, loadError, loaded, navigate, flushPending };
