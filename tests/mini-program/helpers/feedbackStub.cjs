// Page business tests replace only the presentation layer; feedback.test.cjs
// exercises the real policy and lifecycle implementation separately.
module.exports = function feedbackStub(wx = {}, report = () => {}) {
  return {
    currentPage: () => ({}),
    loaded: page => page.setData({ feedbackLoadError: '' }),
    loadError(page, content, retry, hasContent) {
      page.setData({ feedbackLoadError: hasContent ? '' : content });
      page._feedbackRetry = retry;
      report(content);
    },
    showModal: (_, options) => wx.showModal?.(options),
    showToast: (_, options) => wx.showToast?.(options),
    toast: options => wx.showToast?.({ title: options.message }),
    showMessage: (_, content) => { wx.showToast?.({ title: content }); report(content); },
    clearMessage() {},
    userText: (text, fallback) => text || fallback,
    navigate: (_, method, options) => wx[method]?.(options),
  };
};
