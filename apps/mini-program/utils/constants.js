/**
 * 常量定义
 * 统一管理页面中的魔法数字和状态值
 */

// Tab 标签页常量
const TAB_TYPES = {
  TIME_SORT: 0,      // 综合排序
  PRICE_SORT: 1,     // 价格排序
  QUICK_FILTER: 2    // 快捷筛选
};

// 订单状态常量
const ORDER_STATUS = {
  DISABLED: -1,      // 禁用状态
  CATEGORY_FILTER: 0, // 分类筛选
  ADDRESS_FILTER: 1,  // 地址筛选
  RESET_FILTER: 2     // 重置筛选
};

// 价格排序状态
const PRICE_SORT_STATUS = {
  HIGH_TO_LOW: 0,    // 从高到低
  LOW_TO_HIGH: 1,    // 从低到高
  PRICE_SORT: 2      // 价格排序
};

// 地址类型常量
const ADDRESS_TYPES = {
  PICKUP: 0,         // 取件
  RECEIVE: 1         // 收件
};

// 筛选类型常量
const FILTER_TYPES = {
  CATEGORY: 'showByCategory',
  PICKUP_ADDRESS: 'showByPickUpAdd',
  RECEIVE_ADDRESS: 'showByReciveAdd',
  DOUBLE_ADDRESS: 'showByDoubleAdd'
};

// 图标和标签配置
const TAB_CONFIG = {
  ICONS: ['caret-up', 'caret-down', 'currency-exchange'],
  LABELS: ['从高到低', '从低到高', '价格排序']
};

// 轮播图配置
const SWIPER_CONFIG = {
  CURRENT: 0,
  AUTOPLAY: true,
  DURATION: 500,
  INTERVAL: 5000
};

// 防抖延迟时间
const DEBOUNCE_DELAY = 300;

// 默认学校ID
const DEFAULT_SCHOOL_ID = '1030000000000';

// 加载提示文字
const LOADING_MESSAGES = {
  LOADING: '加载中',
  GETTING_ORDERS: '获取订单中',
  GETTING_ADDRESS: '获取地址中',
  GETTING_CATEGORY: '获取分类中'
};

// 错误提示文字
const ERROR_MESSAGES = {
  NETWORK_ERROR: '网络请求失败，请检查网络连接',
  GET_ORDERS_FAILED: '获取订单失败，请稍后再试',
  GET_ADDRESS_FAILED: '获取地址失败，请稍后再试',
  GET_CATEGORY_FAILED: '获取分类失败，请稍后再试',
  LOGIN_REQUIRED: '请先登录',
  LOGIN_FAILED: '登录失败，请重试'
};

// 成功提示文字
const SUCCESS_MESSAGES = {
  REFRESH_SUCCESS: '刷新成功',
  LOGIN_SUCCESS: '登录成功'
};

module.exports = {
  TAB_TYPES,
  ORDER_STATUS,
  PRICE_SORT_STATUS,
  ADDRESS_TYPES,
  FILTER_TYPES,
  TAB_CONFIG,
  SWIPER_CONFIG,
  DEBOUNCE_DELAY,
  DEFAULT_SCHOOL_ID,
  LOADING_MESSAGES,
  ERROR_MESSAGES,
  SUCCESS_MESSAGES
};
