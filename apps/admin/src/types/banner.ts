/**
 * 轮播图相关类型定义
 */

export enum BannerJumpType {
  NONE = 0,
  WEB = 1,
  INNER = 2,
  MINI = 3,
}

export interface Banner {
  id: number
  imgUrl: string
  title: string
  schoolId: number
  schoolName?: string | null
  jumpType: BannerJumpType
  jumpTarget: string
  remark: string
  createBy?: number
  createTime?: string
  deleted?: number
}

export interface BannerCreateRequest {
  imgUrl: string
  title: string
  schoolId: number
  jumpType: BannerJumpType
  jumpTarget: string
  remark: string
}
