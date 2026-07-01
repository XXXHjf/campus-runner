/**
 * 分类相关类型定义
 */

// 分类信息
export interface Category {
  id: string
  categoryName: string
  icon?: string
  sort?: number
  isActive: boolean
  createdAt?: string
  updatedAt?: string
}

// 创建分类请求
export interface CreateCategoryRequest {
  categoryName: string
  icon?: string
  sort?: number
  isActive?: boolean
}

// 更新分类请求
export interface UpdateCategoryRequest extends Partial<CreateCategoryRequest> {
  id: string
}

