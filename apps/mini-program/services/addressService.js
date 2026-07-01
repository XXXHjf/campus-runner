/**
 * 地址相关服务
 * 封装所有地址相关的 API 调用
 */

const { request } = require('./request');

const url = getApp().globalData.API_URL;

/**
 * 获取三级地址数据
 * @returns {Promise<Object>} 地址数据
 */
function getThreeLevelAddress() {
  return request({
    url: `${url}/api/address/three`,
    method: 'GET'
  }).then(res => {
    return res.data.data;
  });
}

/**
 * 获取分类数据
 * @returns {Promise<Array>} 分类列表
 */
function getCategories() {
  return request({
    url: `${url}/api/category`,
    method: 'GET'
  }).then(res => {
    return res.data.data;
  });
}

/**
 * 获取所有地址（用于选择器）
 * @returns {Promise<Array>} 地址列表
 */
function getAllAddresses() {
  return request({
    url: `${url}/api/address/show`,
    method: 'GET'
  }).then(res => {
    return res.data.data || [];
  });
}

/**
 * 获取指定类型的地址
 * @param {number} type - 地址类型 (0: 取件, 1: 收件)
 * @returns {Promise<Object>} 地址数据
 */
function getAddressByType(type) {
  return request({
    url: `${url}/api/address`,
    method: 'GET',
    data: { type }
  }).then(res => {
    return res.data.data || {};
  });
}

/**
 * 创建用户自定义地址
 * @param {Object} addressData - 地址数据
 * @returns {Promise<Object>} 创建结果
 */
function createUserAddress(addressData) {
  return request({
    url: `${url}/api/address`,
    method: 'POST',
    data: addressData
  }).then(res => {
    return res.data;
  });
}

/**
 * 更新用户自定义地址
 * @param {Object} addressData - 地址数据
 * @returns {Promise<Object>} 更新结果
 */
function updateUserAddress(addressData) {
  return request({
    url: `${url}/api/address`,
    method: 'PUT',
    data: addressData
  }).then(res => {
    return res.data;
  });
}

/**
 * 更新用户自定义地址（详情页）
 * @param {Object} addressData - 地址数据
 * @returns {Promise<Object>} 更新结果
 */
function updateUserAddressDetail(addressData) {
  return request({
    url: `${url}/api/address/update`,
    method: 'PUT',
    data: addressData
  }).then(res => {
    return res.data;
  });
}

/**
 * 删除用户自定义地址
 * @param {string|number} addressId - 地址ID
 * @returns {Promise<Object>} 删除结果
 */
function deleteUserAddress(addressId) {
  return request({
    url: `${url}/api/address/${addressId}`,
    method: 'DELETE'
  }).then(res => {
    return res.data;
  });
}

/**
 * 获取用户自定义地址详情
 * @param {string|number} addressId - 地址ID
 * @returns {Promise<Object>} 地址详情
 */
function getUserAddressDetail(addressId) {
  return request({
    url: `${url}/api/address`,
    method: 'GET',
    data: { id: addressId }
  }).then(res => {
    return res.data.data?.[0] || {};
  });
}

/**
 * 转换地址数据格式 - 选择器
 * @param {Object} addressList - 原始地址数据
 * @returns {Object} 转换后的地址数据
 */
function convertAddressList(addressList) {
  let List = {
    schools: {},
    compuses: {},
    buildings: {}
  };
  
  for (const building of addressList.building) {
    // 建筑名，数字 ID 作为 ID
    List.buildings[building.numberId] = building.buildingName;
  }
  
  for (const compus of addressList.compus) {
    // 校区名，数字 ID 作为 ID
    List.compuses[compus.numberId] = compus.compusName;
    List.buildings[compus.numberId] = '全部楼宇';
  }
  
  for (const school of addressList.school) {
    // 学校名，数字 ID 作为 ID
    List.schools[school.numberId] = school.schoolName;
    List.compuses[school.numberId] = '全校范围';
    List.buildings[school.numberId] = '全部楼宇';
  }
  
  return List;
}

module.exports = {
  getThreeLevelAddress,
  getCategories,
  getAllAddresses,
  getAddressByType,
  createUserAddress,
  updateUserAddress,
  updateUserAddressDetail,
  deleteUserAddress,
  getUserAddressDetail,
  convertAddressList
};
