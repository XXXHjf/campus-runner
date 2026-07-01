/**
 * 常量定义
 */

// HTTP 状态码
export const HTTP_STATUS = {
  OK: 200,
  CREATED: 201,
  NO_CONTENT: 204,
  BAD_REQUEST: 400,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  INTERNAL_SERVER_ERROR: 500,
} as const

// 订单状态标签
export const ORDER_STATUS_LABELS = {
  0: '待接单',
  1: '已接单',
  2: '进行中',
  3: '已完成',
  4: '已取消',
} as const

// 订单状态颜色
export const ORDER_STATUS_COLORS = {
  0: '#0052d9',
  1: '#029cd4',
  2: '#0ba360',
  3: '#2ba471',
  4: '#e34d59',
} as const

// 接单状态标签
export const TAKE_ORDER_STATUS_LABELS = {
  0: '已接单',
  1: '已取件',
  2: '已送达',
  3: '已确认',
  4: '已取消',
} as const

// 接单状态颜色
export const TAKE_ORDER_STATUS_COLORS = {
  0: '#0052d9',
  1: '#029cd4',
  2: '#0ba360',
  3: '#2ba471',
  4: '#e34d59',
} as const

// 价格模式标签
export const PRICE_MODE_LABELS = {
  0: '无偿',
  1: '有偿',
} as const

// 门禁状态标签
export const DOOR_ACCESS_LABELS = {
  0: '无门禁',
  1: '有门禁',
} as const

// 性别标签
export const GENDER_LABELS = {
  0: '未知',
  1: '男',
  2: '女',
} as const

// 地址类型标签
export const ADDRESS_TYPE_LABELS = {
  0: '取件地址',
  1: '收件地址',
} as const

// 地址层级标签
export const ADDRESS_LEVEL_LABELS = {
  1: '学校',
  2: '校区',
  3: '楼宇',
} as const

// 默认值
export const DEFAULT_VALUES = {
  PAGE: 1,
  PAGE_SIZE: 10,
  PRICE: 5,
  GAP_MINUTES: 60,
  AUTO_CANCEL_HOURS: 24,
  MIN_GAP_MINUTES: 10,
  MAX_GAP_MINUTES: 120,
  MIN_PRICE: 0.1,
  MAX_NOTE_LENGTH: 100,
  MAX_USERNAME_LENGTH: 50,
} as const

// 验证消息
export const VALIDATION_MESSAGES = {
  // 通用
  REQUIRED: '此字段为必填项',
  INVALID_FORMAT: '格式不正确',

  // 用户
  INVALID_PHONE: '请输入正确的手机号',
  INVALID_USERNAME: '昵称中不能包含特殊字符',
  USERNAME_TOO_LONG: `昵称不能超过${DEFAULT_VALUES.MAX_USERNAME_LENGTH}个字符`,

  // 订单
  NO_ADDRESS: '请选择地址',
  NO_CATEGORY: '请选择跑腿类型',
  NO_CONTACT: '请填写联系方式',
  NO_NOTE: '请填写跑腿说明',
  INVALID_NOTE: '说明中不能包含特殊字符',
  NOTE_TOO_LONG: `说明不能超过${DEFAULT_VALUES.MAX_NOTE_LENGTH}个字符`,
  INVALID_GAP: '任务时间错误',
  GAP_TOO_SHORT: `任务时间不能少于${DEFAULT_VALUES.MIN_GAP_MINUTES}分钟`,
  GAP_TOO_LONG: `任务时间不能超过${DEFAULT_VALUES.MAX_GAP_MINUTES}分钟`,
  INVALID_PRICE: '价格错误',
  PRICE_TOO_LOW: `最低价格为${DEFAULT_VALUES.MIN_PRICE}元`,
} as const

// 成功消息
export const SUCCESS_MESSAGES = {
  LOGIN_SUCCESS: '登录成功',
  LOGOUT_SUCCESS: '退出登录成功',
  CREATE_SUCCESS: '创建成功',
  UPDATE_SUCCESS: '更新成功',
  DELETE_SUCCESS: '删除成功',
  SAVE_SUCCESS: '保存成功',
  SUBMIT_SUCCESS: '提交成功',
  CANCEL_SUCCESS: '取消成功',
  CONFIRM_SUCCESS: '确认成功',
} as const

// 错误消息
export const ERROR_MESSAGES = {
  NETWORK_ERROR: '网络请求失败，请检查网络连接',
  UNKNOWN_ERROR: '未知错误，请稍后重试',
  TIMEOUT_ERROR: '请求超时，请稍后重试',
  SERVER_ERROR: '服务器错误，请稍后重试',
  UNAUTHORIZED: '未授权，请先登录',
  FORBIDDEN: '无权限访问',
  NOT_FOUND: '资源不存在',
  VALIDATION_ERROR: '数据验证失败',
} as const

// 加载消息
export const LOADING_MESSAGES = {
  LOADING: '加载中...',
  SUBMITTING: '提交中...',
  SAVING: '保存中...',
  DELETING: '删除中...',
  UPLOADING: '上传中...',
} as const

