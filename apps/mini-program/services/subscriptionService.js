const SECOND_HAND_ORDER_TEMPLATE_ID = '9t_UqwTYTDOrPkVU24Z_heEtj4MyVqNCXQN0ntLnvCY';
const SECOND_HAND_MESSAGE_TEMPLATE_ID = 'stU3liGSHNl_cpUPucv4ZtJqbkIYNjVny-aIsLv85Oc';

// A declined or unavailable subscription must never turn a successful action into a failure.
function requestTemplates(tmplIds) {
  return new Promise((resolve) => {
    if (typeof wx.requestSubscribeMessage !== 'function') return resolve({});
    try {
      wx.requestSubscribeMessage({ tmplIds, success: resolve, fail: () => resolve({}) });
    } catch (_) { resolve({}); }
  });
}

module.exports = {
  requestTemplates,
  requestSecondHandOrder: () => requestTemplates([SECOND_HAND_ORDER_TEMPLATE_ID]),
  requestSecondHandMessage: () => requestTemplates([SECOND_HAND_MESSAGE_TEMPLATE_ID]),
};
