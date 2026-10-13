/**
 * 订单草稿管理工具
 * 提供订单草稿的保存、读取、清除功能
 * 防止用户填写过程中意外退出导致数据丢失
 */

const { CACHE_KEYS } = require('./orderConstants');
const accountStorage = require('./accountStorage');

// Only an absolute timestamp can reproduce the deadline used for submission.
function normalizeSelectedTime(value) {
  if (typeof value !== 'number' && typeof value !== 'string') return null;
  if (typeof value === 'string' && !value.trim()) return null;
  const time = Number(value);
  return time > 0 && Number.isFinite(new Date(time).getTime()) ? time : null;
}

/**
 * 保存订单草稿
 * @param {Object} orderData - 订单数据
 * @returns {boolean} 是否保存成功
 */
function saveDraft(orderData, session = accountStorage.captureSession()) {
  try {
    const draft = {
      ...orderData,
      selectedTime: normalizeSelectedTime(orderData.selectedTime),
      timestamp: Date.now(),
      version: '1.2'
    };
    
    if (!accountStorage.write(CACHE_KEYS.ORDER_DRAFT, draft, session)) return false;
    console.log('[订单草稿] 保存成功');
    return true;
  } catch (error) {
    console.error("[订单草稿] 保存失败:");
    return false;
  }
}

/**
 * 读取订单草稿
 * @param {number} maxAge - 草稿最大有效期（毫秒），默认7天
 * @returns {Object|null} 订单草稿数据或null
 */
function loadDraft(maxAge = 7 * 24 * 60 * 60 * 1000) {
  try {
    const draft = accountStorage.read(CACHE_KEYS.ORDER_DRAFT);
    
    if (!draft) {
      console.log('[订单草稿] 无草稿记录');
      return null;
    }
    
    // 检查草稿是否过期
    const age = Date.now() - draft.timestamp;
    if (!Number.isFinite(age) || age < 0 || age > maxAge) {
      console.log('[订单草稿] 草稿已过期，自动清除');
      clearDraft();
      return null;
    }
    
    console.log('[订单草稿] 读取成功');
    return draft;
  } catch (error) {
    console.error("[订单草稿] 读取失败:");
    return null;
  }
}

/**
 * 清除订单草稿
 */
function clearDraft() {
  try {
    accountStorage.remove(CACHE_KEYS.ORDER_DRAFT);
    console.log('[订单草稿] 清除成功');
  } catch (error) {
    console.error("[订单草稿] 清除失败:");
  }
}

/**
 * 检查是否有草稿
 * @returns {boolean} 是否存在有效草稿
 */
function hasDraft() {
  return loadDraft() !== null;
}

/**
 * 从草稿恢复订单数据（过滤掉不需要恢复的字段）
 * @param {Object} draft - 草稿数据
 * @returns {Object} 处理后的订单数据
 */
function restoreFromDraft(draft) {
  const session = accountStorage.captureSession();
  if (!accountStorage.isCurrent(session) || !draft || draft.ownerId !== session.ownerId) return {};
  
  // 不恢复的字段（用于控制UI状态，不应该恢复）
  const excludeFields = [
    'timestamp', 'version', 'ownerId', 'userInfo', 'orderID', 'wxpayPrepayID',
    'showReachTime', 'gapReach', 'activeTab', 'todayTimes', 'tomorrowTimes',
    'isReachTimeVisiable', 'showOrderConfirm'
  ];
  
  const restored = {};
  Object.keys(draft).forEach(key => {
    if (!excludeFields.includes(key)) {
      restored[key] = draft[key];
    }
  });
  
  // Older drafts without a timestamp must require a new selection. Relative
  // durations and labels cannot identify the user's original deadline.
  const selectedTime = normalizeSelectedTime(draft.selectedTime);
  restored.selectedTime = selectedTime !== null && selectedTime > Date.now() ? selectedTime : null;
  return restored;
}

/**
 * 创建防抖保存函数
 * @param {number} delay - 防抖延迟（毫秒），默认1000ms
 * @returns {Function} 防抖后的保存函数
 */
function createDebouncedSave(delay = 1000) {
  let timeoutId = null;
  let pendingData = null;
  let pendingSession = null;
  
  const debouncedSave = function(orderData) {
    if (timeoutId) {
      clearTimeout(timeoutId);
    }
    
    pendingData = orderData;
    pendingSession = accountStorage.captureSession();
    timeoutId = setTimeout(() => {
      timeoutId = null;
      const saved = saveDraft(pendingData, pendingSession);
      pendingData = null;
      pendingSession = null;
      return saved;
    }, delay);
  };

  debouncedSave.cancel = function() {
    if (timeoutId) clearTimeout(timeoutId);
    timeoutId = null;
    pendingData = null;
    pendingSession = null;
  };

  debouncedSave.flush = function() {
    if (!pendingData) return false;
    const saved = saveDraft(pendingData, pendingSession);
    debouncedSave.cancel();
    return saved;
  };

  return debouncedSave;
}

module.exports = {
  saveDraft,
  loadDraft,
  clearDraft,
  hasDraft,
  restoreFromDraft,
  createDebouncedSave
};

