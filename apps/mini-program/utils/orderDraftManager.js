/**
 * 订单草稿管理工具
 * 提供订单草稿的保存、读取、清除功能
 * 防止用户填写过程中意外退出导致数据丢失
 */

const { CACHE_KEYS } = require('./orderConstants');

/**
 * 保存订单草稿
 * @param {Object} orderData - 订单数据
 * @returns {boolean} 是否保存成功
 */
function saveDraft(orderData) {
  try {
    const draft = {
      ...orderData,
      timestamp: Date.now(),
      version: '1.0' // 用于未来版本兼容
    };
    
    wx.setStorageSync(CACHE_KEYS.ORDER_DRAFT, draft);
    console.log('[订单草稿] 保存成功');
    return true;
  } catch (error) {
    console.error('[订单草稿] 保存失败:', error);
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
    const draft = wx.getStorageSync(CACHE_KEYS.ORDER_DRAFT);
    
    if (!draft) {
      console.log('[订单草稿] 无草稿记录');
      return null;
    }
    
    // 检查草稿是否过期
    const age = Date.now() - draft.timestamp;
    if (age > maxAge) {
      console.log('[订单草稿] 草稿已过期，自动清除');
      clearDraft();
      return null;
    }
    
    console.log('[订单草稿] 读取成功');
    return draft;
  } catch (error) {
    console.error('[订单草稿] 读取失败:', error);
    return null;
  }
}

/**
 * 清除订单草稿
 */
function clearDraft() {
  try {
    wx.removeStorageSync(CACHE_KEYS.ORDER_DRAFT);
    console.log('[订单草稿] 清除成功');
  } catch (error) {
    console.error('[订单草稿] 清除失败:', error);
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
  if (!draft) return {};
  
  // 不恢复的字段（用于控制UI状态，不应该恢复）
  const excludeFields = ['timestamp', 'version', 'userInfo', 'orderID', 'wxpayPrepayID'];
  
  const restored = {};
  Object.keys(draft).forEach(key => {
    if (!excludeFields.includes(key)) {
      restored[key] = draft[key];
    }
  });
  
  return restored;
}

/**
 * 创建防抖保存函数
 * @param {number} delay - 防抖延迟（毫秒），默认1000ms
 * @returns {Function} 防抖后的保存函数
 */
function createDebouncedSave(delay = 1000) {
  let timeoutId = null;
  
  return function(orderData) {
    if (timeoutId) {
      clearTimeout(timeoutId);
    }
    
    timeoutId = setTimeout(() => {
      saveDraft(orderData);
    }, delay);
  };
}

module.exports = {
  saveDraft,
  loadDraft,
  clearDraft,
  hasDraft,
  restoreFromDraft,
  createDebouncedSave
};

