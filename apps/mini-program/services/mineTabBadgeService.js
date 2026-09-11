const userService = require('./userService');
const takeOrderService = require('./takeOrderService');
const userOrderService = require('./userOrderService');
const secondHandService = require('./secondHandService');
const tokenManager = require('../utils/tokenManager');
const { isProfileComplete } = require('../utils/profileStatus');

const MINE_TAB_INDEX = 2;
const UNPAID_TIMEOUT_MS = 30 * 60 * 1000;
const REFRESH_INTERVAL_MS = 15000;

let lastRefreshAt = 0;
let lastSummary = null;
let refreshPromise = null;

function setMineTabRedDot(totalCount) {
  const hasNotice = Number(totalCount) > 0;
  const method = hasNotice ? wx.showTabBarRedDot : wx.hideTabBarRedDot;
  if (typeof method !== 'function') return;
  method({
    index: MINE_TAB_INDEX,
    fail: () => {},
  });
}

function activeRunnerUnpaidCount(orders) {
  const now = Date.now();
  return orders.filter((item) => {
    if (Number(item.status) !== -1 || !item.createTime) return false;
    const createdAt = new Date(String(item.createTime).replace(/-/g, '/'));
    return !Number.isNaN(createdAt.getTime())
      && createdAt.getTime() + UNPAID_TIMEOUT_MS > now;
  }).length;
}

function fulfilledValue(result) {
  return result.status === 'fulfilled' && Array.isArray(result.value) ? result.value : [];
}

async function loadSummary(userInfo) {
  const currentUser = userInfo && userInfo.id != null
    ? userInfo
    : await userService.getUserInfo();
  if (!isProfileComplete(currentUser)) {
    return { totalCount: 0 };
  }

  const results = await Promise.allSettled([
    takeOrderService.getNotWithdrawnOrders(),
    userOrderService.getMyOrders(),
    secondHandService.listBuyerOrders(),
    secondHandService.listSellerOrders(),
    secondHandService.listMyBargains(),
    secondHandService.listConversations(),
  ]);
  const hasPartialFailure = results.some((result) => result.status === 'rejected');
  const [notReceivedResult, runnerOrdersResult, buyerOrdersResult, sellerOrdersResult, bargainsResult, conversationsResult] = results;
  const notReceivedOrders = fulfilledValue(notReceivedResult);
  const runnerOrders = fulfilledValue(runnerOrdersResult);
  const buyerOrders = fulfilledValue(buyerOrdersResult);
  const sellerOrders = fulfilledValue(sellerOrdersResult);
  const bargains = fulfilledValue(bargainsResult);
  const conversations = fulfilledValue(conversationsResult);

  const runnerUnpaidCount = activeRunnerUnpaidCount(runnerOrders);
  const secondHandBuyerCount = buyerOrders.filter((item) => Number(item.status) === 2).length;
  const secondHandSellerCount = sellerOrders.filter((item) => (
    Number(item.status) === 1
    || (Number(item.status) === 8 && item.transferState === 'WAIT_USER_CONFIRM')
  )).length;
  const bargainPendingCount = bargains.filter((item) => (
    Number(item.status) === 0
    && Number(item.sellerId) === Number(currentUser.id)
  )).length;
  const privateUnreadCount = conversations.reduce(
    (sum, item) => sum + Math.max(0, Number(item.unreadCount) || 0),
    0,
  );
  const totalCount = notReceivedOrders.length
    + runnerUnpaidCount
    + secondHandBuyerCount
    + secondHandSellerCount
    + bargainPendingCount
    + privateUnreadCount;
  if (totalCount === 0 && hasPartialFailure) {
    throw new Error('待办数据暂时不可用');
  }

  return {
    totalCount,
    privateUnreadCount,
  };
}

async function refreshMineTabRedDot(options = {}) {
  if (!tokenManager.hasToken()) {
    lastSummary = { totalCount: 0 };
    setMineTabRedDot(0);
    return lastSummary;
  }
  const now = Date.now();
  if (!options.force && lastSummary && now - lastRefreshAt < REFRESH_INTERVAL_MS) {
    setMineTabRedDot(lastSummary.totalCount);
    return lastSummary;
  }
  if (refreshPromise) return refreshPromise;

  refreshPromise = loadSummary(options.userInfo)
    .then((summary) => {
      lastSummary = summary;
      lastRefreshAt = Date.now();
      setMineTabRedDot(summary.totalCount);
      return summary;
    })
    .catch(() => lastSummary || { totalCount: 0 })
    .finally(() => {
      refreshPromise = null;
    });
  return refreshPromise;
}

function clearMineTabRedDot() {
  lastSummary = { totalCount: 0 };
  lastRefreshAt = 0;
  setMineTabRedDot(0);
}

module.exports = {
  setMineTabRedDot,
  refreshMineTabRedDot,
  clearMineTabRedDot,
};
