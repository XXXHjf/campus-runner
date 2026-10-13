const { request, requiredObject } = require('./request');
const tokenManager = require('../utils/tokenManager');
const url = getApp().globalData.API_URL;
const countKeys = ['runnerReceivableCount', 'runnerUnpaidCount', 'secondHandBuyerCount',
  'secondHandBuyerNoticeCount', 'secondHandSellerCount', 'bargainPendingCount', 'bargainAcceptedCount'];
async function getSummary() {
  const token = tokenManager.getToken();
  const session = tokenManager.getSessionVersion?.();
  const result = requiredObject(await request({ url: `${url}/api/user/tasks`, method: 'GET' }));
  // Allow the mini program to run during a coordinated API rollout.
  if (result.bargainAcceptedCount === undefined) result.bargainAcceptedCount = 0;
  if (token !== tokenManager.getToken() || session !== tokenManager.getSessionVersion?.()) throw new Error('登录状态已变化，请刷新');
  if (countKeys.some(key => !Number.isSafeInteger(result[key]) || result[key] < 0)) throw new Error('待办加载失败，请重试');
  for (const [count, id] of [['runnerReceivableCount', 'runnerReceivableOrderId'], ['runnerUnpaidCount', 'runnerUnpaidOrderId']]) {
    if (result[count] === 1 && (!Number.isSafeInteger(result[id]) || result[id] <= 0)) throw new Error('待办加载失败，请重试');
  }
  return result;
}
module.exports = { getSummary };
