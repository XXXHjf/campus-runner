const tokenManager = require('./tokenManager');
const { CACHE_KEYS } = require('./orderConstants');
const privateKeys = [CACHE_KEYS.ORDER_DRAFT, CACHE_KEYS.LAST_ADDRESS, 'secondHandDrafts'];

function captureSession() {
  return { ownerId: tokenManager.getAccountId(), version: tokenManager.getSessionVersion() };
}

function isCurrent(session) {
  return !!session?.ownerId && session.ownerId === tokenManager.getAccountId()
    && session.version === tokenManager.getSessionVersion();
}

function read(key, session = captureSession()) {
  // Old shared records cannot safely be attributed to an account.
  wx.removeStorageSync(key);
  if (!isCurrent(session)) return null;
  const value = wx.getStorageSync(`${key}:${session.ownerId}`);
  if (!value || value.ownerId !== session.ownerId) return null;
  return value;
}

function write(key, value, session = captureSession()) {
  wx.removeStorageSync(key);
  if (!isCurrent(session)) return false;
  wx.setStorageSync(`${key}:${session.ownerId}`, { ...value, ownerId: session.ownerId });
  return true;
}

function remove(key, session = captureSession()) {
  wx.removeStorageSync(key);
  if (isCurrent(session)) wx.removeStorageSync(`${key}:${session.ownerId}`);
}

tokenManager.onSessionChange(({ reason, previousAccountId }) => {
  privateKeys.forEach(key => {
    wx.removeStorageSync(key);
    if (reason === 'logout' && previousAccountId) wx.removeStorageSync(`${key}:${previousAccountId}`);
  });
});

module.exports = { captureSession, isCurrent, read, write, remove };
