/**
 * 地址相关类型定义
 */

// 地址类型
export enum AddressType {
  PICKUP = 0, // 取件地址
  RECEIVE = 1, // 收件地址
}

// 地址层级
export enum AddressLevel {
  SCHOOL = 1, // 学校级别
  CAMPUS = 2, // 校区级别
  BUILDING = 3, // 楼宇级别
}

// 地址信息
export interface Address {
  id: string
  numberId: string
  schoolId: string
  schoolName?: string
  campusId?: string
  campusName?: string
  buildingId?: string
  buildingName?: string
  detailAddress?: string
  level: AddressLevel
  isCustom: boolean
  userId?: string
  createdAt?: string
  updatedAt?: string
}

// 学校地址
export interface SchoolAddress {
  numberId: string
  schoolName: string
}

// 校区地址
export interface CampusAddress {
  numberId: string
  compusName: string
  schoolId: string
}

// 楼宇地址
export interface BuildingAddress {
  numberId: string
  buildingName: string
  compusId: string
  schoolId: string
}

// 三级地址数据
export interface ThreeLevelAddress {
  school: SchoolAddress[]
  compus: CampusAddress[]
  building: BuildingAddress[]
}

// 创建地址请求
export interface CreateAddressRequest {
  schoolId: string
  campusId?: string
  buildingId?: string
  detailAddress?: string
  level: AddressLevel
}

// 更新地址请求
export interface UpdateAddressRequest extends Partial<CreateAddressRequest> {
  id: string
}

// 地址查询参数
export interface AddressQueryParams {
  type?: AddressType
  schoolId?: string
  isCustom?: boolean
}

// 管理端：预设地址创建请求
export interface AdminSchoolReserve {
  schoolName: string
  compusName: string
  buildCategoryName: string
  buildingName: string
}

// 管理端：学校基础信息
export interface AdminSchool {
  id: number
  schoolName: string
  numberId: number
  deleted: number
}

// 管理端：楼宇信息
export interface AdminAddressBuilding {
  id: number
  schoolId: number
  schoolName: string
  compusId: number
  compusName: string
  buildCategoryId: number
  buildCategoryName: string
  numberId: number
  buildingName: string
}

// 管理端：更新楼宇名称
export interface AdminUpdateBuildingRequest {
  buildingID: number
  buildingName: string
}

