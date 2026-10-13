/**
 * 地址相关服务
 * 封装所有地址相关的 API 调用
 */

const { request, collectListPages, safeList, requiredObject } = require('./request');

function checkResult(res) {
  if (res.data?.code !== 1) throw new Error(res.data?.msg || '操作失败，请重试');
  return res;
}

const url = getApp().globalData.API_URL;

/**
 * 获取三级地址数据
 * @returns {Promise<Object>} 地址数据
 */
async function getThreeLevelAddress(compusNumberId) {
  const result = { school: [], compus: [], buildCategory: [], building: [] };
  const token = require('../utils/tokenManager').getToken();
  const seenPages = new Set();
  for (let page = 1; ; page++) {
    const res = await request({ url: `${url}/api/address/three`, method: 'GET',
      data: { page, pageSize: 50, ...(compusNumberId ? { compusNumberId } : {}) } });
    checkResult(res);
    if (token !== require('../utils/tokenManager').getToken()) throw new Error('登录状态已变化，请重试');
    const data = requiredObject(res);
    const keys = compusNumberId ? ['building'] : ['school', 'compus', 'buildCategory'];
    const signature = JSON.stringify(keys.map(key => (data[key] || []).map(row => row.numberId)));
    if (keys.some(key => (data[key] || []).length > 50) || seenPages.has(signature)) throw new Error('地址加载失败，请重试');
    seenPages.add(signature);
    for (const key of keys) {
      if (!Array.isArray(data[key])) throw new Error('地址加载失败，请重试');
      result[key].push(...data[key]);
    }
    if (keys.every(key => data[key].length < 50)) return result;
  }
}

/**
 * 获取分类数据
 * @returns {Promise<Array>} 分类列表
 */
function getCategories() {
  return collectListPages(query => request({
    url: `${url}/api/category`,
    method: 'GET', data: query
  }).then(checkResult).then(res => {
    return safeList(res);
  }));
}

/**
 * 获取所有地址（用于选择器）
 * @returns {Promise<Array>} 地址列表
 */
function getAllAddresses(query = {}) {
  return request({
    url: `${url}/api/address/show`,
    method: 'GET',
    data: query
  }).then(checkResult).then(res => {
    return safeList(res);
  });
}

function getAddressSummary() {
  return request({ url: `${url}/api/address/summary`, method: 'GET' }).then(checkResult).then(requiredObject);
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
  }).then(checkResult).then(res => {
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
  }).then(checkResult).then(res => {
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
  }).then(checkResult).then(res => {
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
  }).then(checkResult).then(res => {
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
  }).then(checkResult).then(res => {
    const address = safeList(res)[0];
    if (!address || typeof address !== 'object' || !address.id) throw new Error('地址已不存在，请重新选择');
    return address;
  });
}

function getOptionalAddress(id) {
  return request({ url: `${url}/api/address`, method: 'GET', data: { id, pageSize: 1 } })
    .then(checkResult).then(res => safeList(res)[0] || null);
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
  getAddressSummary,
  createUserAddress,
  updateUserAddress,
  updateUserAddressDetail,
  deleteUserAddress,
  getUserAddressDetail,
  getOptionalAddress,
  convertAddressList
};
