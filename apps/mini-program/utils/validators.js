/**
 * 表单验证工具函数
 * 统一管理各种输入验证逻辑
 */

const { containsEmoji } = require('./commonJs');

/**
 * 验证手机号格式
 * @param {string} phone - 手机号
 * @returns {boolean} 是否有效
 */
function isValidPhone(phone) {
  if (!phone) return false;
  // 中国大陆手机号：1开头，第二位为3-9，共11位
  const phoneRegex = /^1[3-9]\d{9}$/;
  return phoneRegex.test(phone.toString().trim());
}

/**
 * 验证价格格式
 * @param {string|number} price - 价格
 * @returns {boolean} 是否有效
 */
function isValidPrice(price) {
  if (price === null || price === undefined || price === '') return false;
  // 正则表达式：禁止前导零 + 最多两位小数
  const priceRegex = /^(0|(?!0)\d+)(\.\d{1,2})?$/;
  return priceRegex.test(price.toString());
}

/**
 * 验证用户名（昵称）
 * @param {string} username - 用户名
 * @returns {Object} { valid: boolean, message: string }
 */
function validateUsername(username) {
  if (!username || username.trim() === '') {
    return { valid: false, message: '昵称不能为空' };
  }
  
  if (containsEmoji(username)) {
    return { valid: false, message: '昵称中不能有表情' };
  }
  
  if (username.length > 50) {
    return { valid: false, message: '昵称长度不能超过50个字符' };
  }
  
  return { valid: true, message: '' };
}

/**
 * 验证跑腿说明
 * @param {string} note - 跑腿说明
 * @returns {Object} { valid: boolean, message: string }
 */
function validateNote(note) {
  if (!note || note.trim() === '') {
    return { valid: false, message: '未填写跑腿说明' };
  }
  
  if (containsEmoji(note)) {
    return { valid: false, message: '说明中不能有表情' };
  }
  
  if (note.length > 100) {
    return { valid: false, message: '说明长度不能超过100个字符' };
  }
  
  return { valid: true, message: '' };
}

/**
 * 验证任务时间（gap）
 * @param {number} gap - 任务时间（分钟）
 * @param {number} minGap - 最小任务时间（默认10）
 * @param {number} maxGap - 最大任务时间（默认120）
 * @returns {Object} { valid: boolean, message: string }
 */
function validateGap(gap, minGap = 10, maxGap = 120) {
  const numGap = Number(gap);
  
  if (isNaN(numGap)) {
    return { valid: false, message: '请输入正确的时间' };
  }
  
  if (numGap < minGap) {
    return { valid: false, message: `任务时间不能少于${minGap}分钟` };
  }
  
  if (numGap > maxGap) {
    return { valid: false, message: `任务时间不能超过${maxGap}分钟` };
  }
  
  return { valid: true, message: '' };
}

/**
 * 验证价格
 * @param {string|number} price - 价格
 * @param {number} minPrice - 最小价格（默认0.1）
 * @returns {Object} { valid: boolean, message: string }
 */
function validatePrice(price, minPrice = 0.1) {
  if (!isValidPrice(price)) {
    return { valid: false, message: '请输入正确的价格（最多两位小数）' };
  }
  
  const numPrice = Number(price);
  if (numPrice < minPrice) {
    return { valid: false, message: `最低价格为${minPrice}元` };
  }
  
  return { valid: true, message: '' };
}

/**
 * 验证订单基础信息（步骤1）
 * @param {Object} data - 订单数据
 * @returns {Object} { valid: boolean, message: string }
 */
function validateBasicInfo(data) {
  // 检查地址
  if (!data.showPickUp || !data.showRecive) {
    return { valid: false, message: '未选择地址' };
  }
  
  // 检查跑腿类型
  if (!data.showCategory || !data.showCategory.id) {
    return { valid: false, message: '未选择跑腿类型' };
  }
  
  // 检查联系方式
  if (!data.showUser || !data.showPhone) {
    return { valid: false, message: '未填写联系方式' };
  }
  
  // 验证昵称
  const usernameValidation = validateUsername(data.showUser);
  if (!usernameValidation.valid) {
    return usernameValidation;
  }
  
  // 验证手机号
  if (!isValidPhone(data.showPhone)) {
    return { valid: false, message: '请输入正确的手机号' };
  }
  
  return { valid: true, message: '' };
}

/**
 * 验证跑腿内容（步骤2）
 * @param {Object} data - 订单数据
 * @returns {Object} { valid: boolean, message: string }
 */
function validateContent(data) {
  return validateNote(data.note);
}

/**
 * 验证服务信息（步骤3）
 * @param {Object} data - 订单数据
 * @returns {Object} { valid: boolean, message: string }
 */
function validateServiceInfo(data) {
  // 验证任务时间
  if (data.gapError) {
    return { valid: false, message: '任务时间错误' };
  }
  
  // 如果选择了有偿，验证价格
  if (data.priceAccess === 1) {
    if (data.priceError) {
      return { valid: false, message: '付费价格错误' };
    }
    
    const priceValidation = validatePrice(data.price);
    if (!priceValidation.valid) {
      return priceValidation;
    }
  }
  
  return { valid: true, message: '' };
}

module.exports = {
  isValidPhone,
  isValidPrice,
  validateUsername,
  validateNote,
  validateGap,
  validatePrice,
  validateBasicInfo,
  validateContent,
  validateServiceInfo
};

