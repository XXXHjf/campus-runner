/**
 * 验证工具函数
 */

import { VALIDATION_MESSAGES, DEFAULT_VALUES } from '../constants'

/**
 * 验证手机号
 */
export function validatePhone(phone: string): { valid: boolean; message?: string } {
  const phoneRegex = /^1[3-9]\d{9}$/
  if (!phone) {
    return { valid: false, message: VALIDATION_MESSAGES.REQUIRED }
  }
  if (!phoneRegex.test(phone)) {
    return { valid: false, message: VALIDATION_MESSAGES.INVALID_PHONE }
  }
  return { valid: true }
}

/**
 * 验证用户名
 */
export function validateUsername(username: string): { valid: boolean; message?: string } {
  if (!username) {
    return { valid: false, message: VALIDATION_MESSAGES.REQUIRED }
  }
  if (username.length > DEFAULT_VALUES.MAX_USERNAME_LENGTH) {
    return { valid: false, message: VALIDATION_MESSAGES.USERNAME_TOO_LONG }
  }
  // 检查是否包含 emoji 或特殊字符
  const emojiRegex = /[\uD800-\uDBFF][\uDC00-\uDFFF]|[\u2600-\u27FF]/
  if (emojiRegex.test(username)) {
    return { valid: false, message: VALIDATION_MESSAGES.INVALID_USERNAME }
  }
  return { valid: true }
}

/**
 * 验证订单说明
 */
export function validateNote(note: string): { valid: boolean; message?: string } {
  if (!note) {
    return { valid: false, message: VALIDATION_MESSAGES.NO_NOTE }
  }
  if (note.length > DEFAULT_VALUES.MAX_NOTE_LENGTH) {
    return { valid: false, message: VALIDATION_MESSAGES.NOTE_TOO_LONG }
  }
  // 检查是否包含 emoji
  const emojiRegex = /[\uD800-\uDBFF][\uDC00-\uDFFF]|[\u2600-\u27FF]/
  if (emojiRegex.test(note)) {
    return { valid: false, message: VALIDATION_MESSAGES.INVALID_NOTE }
  }
  return { valid: true }
}

/**
 * 验证任务时间
 */
export function validateGapMinutes(gapMinutes: number): { valid: boolean; message?: string } {
  if (!gapMinutes || gapMinutes < DEFAULT_VALUES.MIN_GAP_MINUTES) {
    return { valid: false, message: VALIDATION_MESSAGES.GAP_TOO_SHORT }
  }
  if (gapMinutes > DEFAULT_VALUES.MAX_GAP_MINUTES) {
    return { valid: false, message: VALIDATION_MESSAGES.GAP_TOO_LONG }
  }
  return { valid: true }
}

/**
 * 验证价格
 */
export function validatePrice(price: number): { valid: boolean; message?: string } {
  if (price < DEFAULT_VALUES.MIN_PRICE) {
    return { valid: false, message: VALIDATION_MESSAGES.PRICE_TOO_LOW }
  }
  return { valid: true }
}

/**
 * 验证邮箱
 */
export function validateEmail(email: string): { valid: boolean; message?: string } {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!email) {
    return { valid: false, message: VALIDATION_MESSAGES.REQUIRED }
  }
  if (!emailRegex.test(email)) {
    return { valid: false, message: '请输入正确的邮箱地址' }
  }
  return { valid: true }
}

/**
 * 验证必填项
 */
export function validateRequired(value: unknown): { valid: boolean; message?: string } {
  if (value === null || value === undefined || value === '') {
    return { valid: false, message: VALIDATION_MESSAGES.REQUIRED }
  }
  return { valid: true }
}

