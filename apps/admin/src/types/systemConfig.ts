/**
 * 系统配置相关类型定义
 */

export interface AdminSystemConfigDTO {
  serviceFeeRate: number
  serviceFeeMin: number
}

export type SystemConfigKey = keyof AdminSystemConfigDTO

export interface SystemConfigFieldDefinition {
  key: SystemConfigKey
  label: string
  description: string
  placeholder: string
  unit?: string
  min?: number
  step?: string
}
