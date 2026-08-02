/**
 * 用户相关类型定义
 */

// 性别枚举
export enum Gender {
  UNKNOWN = 0,
  MALE = 1,
  FEMALE = 2,
}

// 用户信息
export interface UserInfo {
  id: string
  openid?: string
  phone: string
  username: string
  headImg: string
  sex: Gender
  schoolId: string
  schoolName?: string
  realname?: string
  stuId?: string
  studentIdCard?: string
  isAuthenticated: boolean
  createdAt?: string
  updatedAt?: string
}

// 登录请求
export interface LoginRequest {
  code: string
}

// 管理后台登录请求
export interface AdminLoginRequest {
  username: string
  password: string
}

// 登录响应
export interface LoginResponse {
  token: string
  userInfo: UserInfo
}

// 管理后台登录响应
export interface AdminLoginResponse {
  id?: number
  username: string
  passwordHash?: string
  nickname?: string
  avatar?: string
  status?: number
  school?: number
  lastLoginIp?: string
  lastLoginTime?: string
  createTime?: string
  updateTime?: string
  deleted?: number
  adminToken: string
}

// 更新用户信息请求
export interface UpdateUserInfoRequest {
  phone?: string
  username?: string
  headImg?: string
  sex?: Gender
}

// 用户认证请求
export interface AuthenticateRequest {
  schoolId: string
  realname: string
  stuId: string
  studentIdCard: string
}

// 学校信息
export interface School {
  id: string
  schoolName: string
  numberId: string
  createdAt?: string
  updatedAt?: string
}

