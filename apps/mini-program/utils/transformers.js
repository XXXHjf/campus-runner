/**
 * 数据处理工具
 * 统一处理各种数据转换和格式化
 */

const {
  _delbefore1stBlank,
  _getExpectTimeDisplay
} = require('../utils/commonJs');

/**
 * 为订单列表添加预期时间显示
 * @param {Array} orders - 订单列表
 * @returns {Array} 处理后的订单列表
 */
function addExpectTime(orders) {
  if (!Array.isArray(orders)) {
    return [];
  }
  
  return orders.map(item => {
    item.expectTime = _getExpectTimeDisplay(item.createTime, item.gap);
    return item;
  });
}

/**
 * 安全处理订单数据
 * @param {Array} orders - 原始订单数据
 * @returns {Array} 处理后的订单数据
 */
function processOrders(orders) {
  if (!Array.isArray(orders)) {
    return [];
  }
  
  const cleanedOrders = _delbefore1stBlank(orders);
  return addExpectTime(cleanedOrders);
}

/**
 * 防抖函数
 * @param {Function} func - 要防抖的函数
 * @param {number} delay - 延迟时间(ms)
 * @returns {Function} 防抖后的函数
 */
function debounce(func, delay) {
  let timeoutId;
  return function (...args) {
    clearTimeout(timeoutId);
    timeoutId = setTimeout(() => func.apply(this, args), delay);
  };
}

/**
 * 节流函数
 * @param {Function} func - 要节流的函数
 * @param {number} delay - 延迟时间(ms)
 * @returns {Function} 节流后的函数
 */
function throttle(func, delay) {
  let lastCall = 0;
  return function (...args) {
    const now = Date.now();
    if (now - lastCall >= delay) {
      lastCall = now;
      return func.apply(this, args);
    }
  };
}

/**
 * 显示加载提示
 * @param {string} title - 提示文字
 */
function showLoading(title = '加载中') {
  wx.showLoading({ title });
}

/**
 * 隐藏加载提示
 */
function hideLoading() {
  wx.hideLoading();
}

/**
 * 显示成功提示
 * @param {string} title - 提示文字
 */
function showSuccess(title) {
  wx.showToast({
    title,
    icon: 'success',
    duration: 2000
  });
}

/**
 * 显示错误提示
 * @param {string} title - 提示文字
 */
function showError(title) {
  wx.showToast({
    title,
    icon: 'error',
    duration: 2000
  });
}

module.exports = {
  addExpectTime,
  processOrders,
  debounce,
  throttle,
  showLoading,
  hideLoading,
  showSuccess,
  showError
};
