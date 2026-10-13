const userService = require('./userService');
const taskSummaryService = require('./taskSummaryService');
const tokenManager = require('../utils/tokenManager');
const { isProfileComplete } = require('../utils/profileStatus');

const MINE_TAB_INDEX = 2;
const REFRESH_INTERVAL_MS = 15000;

let lastRefreshAt = 0;
let lastSummary = null;
let refreshPromise = null;
let refreshScope = null;

function setMineTabRedDot(totalCount) {
  const hasNotice = Number(totalCount) > 0;
  const method = hasNotice ? wx.showTabBarRedDot : wx.hideTabBarRedDot;
  if (typeof method !== 'function') return;
  method({
    index: MINE_TAB_INDEX,
    fail: () => {},
  });
}

async function loadSummary(userInfo) {
  const currentUser = userInfo && userInfo.id != null ? userInfo : await userService.getUserInfo();
  if (!isProfileComplete(currentUser) || Number(currentUser.authentication) !== 1) return { totalCount: 0 };
  const summary = await taskSummaryService.getSummary();
  return { totalCount: summary.runnerReceivableCount + summary.runnerUnpaidCount
    + summary.secondHandBuyerNoticeCount + summary.secondHandSellerCount + summary.bargainPendingCount };
}

async function refreshMineTabRedDot(options = {}) {
  if (!tokenManager.hasToken()) {
    clearMineTabRedDot();
    return lastSummary;
  }
  const scope = JSON.stringify([tokenManager.getToken(), tokenManager.getSessionVersion?.()]);
  if (refreshScope !== scope) {
    lastSummary = null; lastRefreshAt = 0; refreshPromise = null; refreshScope = scope;
    setMineTabRedDot(0);
  }
  const now = Date.now();
  if (!options.force && lastSummary && now - lastRefreshAt < REFRESH_INTERVAL_MS) {
    setMineTabRedDot(lastSummary.totalCount);
    return lastSummary;
  }
  if (refreshPromise) return refreshPromise;

  const pending = loadSummary(options.userInfo)
    .then((summary) => {
      if (refreshScope !== scope || scope !== JSON.stringify([tokenManager.getToken(), tokenManager.getSessionVersion?.()])) return { totalCount: 0 };
      lastSummary = summary;
      lastRefreshAt = Date.now();
      setMineTabRedDot(summary.totalCount);
      return summary;
    })
    .catch(() => refreshScope === scope && scope === JSON.stringify([tokenManager.getToken(), tokenManager.getSessionVersion?.()])
      ? lastSummary || { totalCount: 0 } : { totalCount: 0 })
    .finally(() => {
      if (refreshPromise === pending) refreshPromise = null;
    });
  refreshPromise = pending;
  return pending;
}

function clearMineTabRedDot() {
  refreshScope = null; refreshPromise = null;
  lastSummary = { totalCount: 0 };
  lastRefreshAt = 0;
  setMineTabRedDot(0);
}

module.exports = {
  setMineTabRedDot,
  refreshMineTabRedDot,
  clearMineTabRedDot,
};
