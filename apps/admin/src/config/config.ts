/**
 * 应用配置文件
 */

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || ''

export const config = {
  // API 基础地址（开发环境走 Vite 代理，保持相对路径）
  apiBaseUrl,

  // 请求超时时间（毫秒）
  requestTimeout: 15000,

  // Token 存储键名
  tokenKey: 'admin_token',

  // 用户信息存储键名
  userInfoKey: 'admin_user_info',

  // 是否开启请求日志
  enableRequestLog: import.meta.env.DEV,

  // 是否开启 mock 数据
  enableMock: false,

  // 分页默认配置
  pagination: {
    defaultPageSize: 10,
    pageSizeOptions: [10, 20, 50, 100],
  },

  // 上传配置
  upload: {
    maxSize: 5 * 1024 * 1024, // 5MB
    acceptTypes: ['image/jpeg', 'image/png', 'image/gif'],
  },
}

export default config

