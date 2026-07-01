/**
 * 订单相关常量定义
 * 统一管理订单流程中的魔法数字和状态值
 */

// 步骤条步骤常量
const ORDER_STEPS = {
  BASIC_INFO: 1,    // 基础信息（地址、联系方式、跑腿类型）
  CONTENT: 2,       // 跑腿内容（文字说明、图片说明）
  SERVICE: 3        // 服务信息（任务时间、付费信息）
};

// 步骤标题
const STEP_TITLES = ['基础信息', '跑腿内容', '服务信息'];

// 门禁状态常量
const DOOR_ACCESS = {
  NO_GUARD: 0,      // 无门禁
  HAS_GUARD: 1      // 有门禁
};

// 门禁选项文字
const DOOR_OPTIONS = ['取收均无门禁', '有门禁'];

// 价格设置状态常量
const PRICE_MODE = {
  FREE: 0,          // 无偿（免费）
  PAID: 1           // 有偿（付费）
};

// 地址类型常量
const ADDRESS_TYPE = {
  PICKUP: 0,        // 取件地址
  RECEIVE: 1        // 收件地址
};

// 默认值常量
const DEFAULT_VALUES = {
  PRICE: 5,                    // 默认价格
  GAP_MINUTES: 60,             // 默认任务时间（分钟）
  AUTO_CANCEL_HOURS: 24,       // 默认自动取消时间（小时）
  MIN_GAP_MINUTES: 10,         // 最小任务时间（分钟）
  MAX_GAP_MINUTES: 120,        // 最大任务时间（分钟）
  MIN_PRICE: 0.1,              // 最小价格（元）
  MAX_NOTE_LENGTH: 100,        // 最大说明长度
  MAX_USERNAME_LENGTH: 50      // 最大昵称长度
};

// 表单验证错误消息
const VALIDATION_MESSAGES = {
  // 步骤1 - 基础信息
  NO_ADDRESS: '未选择地址',
  NO_CATEGORY: '未选择跑腿类型',
  NO_CONTACT: '未填写联系方式',
  INVALID_USERNAME: '昵称中不能有表情',
  INVALID_PHONE: '请输入正确的手机号',
  
  // 步骤2 - 跑腿内容
  NO_NOTE: '未填写跑腿说明',
  INVALID_NOTE: '说明中不能有表情',
  
  // 步骤3 - 服务信息
  INVALID_GAP: '任务时间错误',
  GAP_TOO_SHORT: '任务时间过短',
  INVALID_PRICE: '付费价格错误',
  PRICE_TOO_LOW: '最低价格为0.1'
};

// 本地缓存键名
const CACHE_KEYS = {
  LAST_ADDRESS: 'lastUsedOrderAddress',       // 上次使用的地址
  ORDER_DRAFT: 'orderDraft'                   // 订单草稿
};

// 按钮主题样式
const BUTTON_THEME = {
  DEFAULT: { theme: 'default', variant: 'base' },      // 默认样式（不可用）
  PRIMARY: { theme: 'primary', variant: 'outline' }    // 主色样式（可用）
};

// 订阅消息模板ID
const SUBSCRIBE_TEMPLATE_IDS = [
  'qkAvz4m0wgT36hFtijv4KqidigRyzEZDteR2RdZJMNU',
  'Pwxi3Ae73tJdYq8llzpG_9tQCFiCX95eqg2xG5iEbCc',
  'wXbpHxae19WhKHfzJaFcDTq19rVbAYxlKjBO6-TTj3w'
];

module.exports = {
  ORDER_STEPS,
  STEP_TITLES,
  DOOR_ACCESS,
  DOOR_OPTIONS,
  PRICE_MODE,
  ADDRESS_TYPE,
  DEFAULT_VALUES,
  VALIDATION_MESSAGES,
  CACHE_KEYS,
  BUTTON_THEME,
  SUBSCRIBE_TEMPLATE_IDS
};

