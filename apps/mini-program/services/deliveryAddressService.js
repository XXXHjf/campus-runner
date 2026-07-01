/**
 * 收货地址服务
 * 封装收货地址管理相关的 API 调用
 */

const { request, safeList } = require('./request');

const url = getApp().globalData.API_URL;

/**
 * 获取我的所有地址
 * @returns {Promise<Array>} 地址列表
 */
function getMyAddresses() {
  return request({
    url: `${url}/api/address/show`,
    method: 'GET'
  }).then(res => {
    return safeList(res);
  });
}

/**
 * 获取地址详情
 * @param {string|number} addressId - 地址ID
 * @returns {Promise<Object>} 地址详情
 */
function getAddressDetail(addressId) {
  return request({
    url: `${url}/api/deliveryAddress/${addressId}`,
    method: 'GET'
  }).then(res => {
    return res.data.data || {};
  });
}

/**
 * 创建收货地址
 * @param {Object} addressData - 地址数据
 * @returns {Promise<Object>} 创建结果
 */
function createAddress(addressData) {
  return request({
    url: `${url}/api/deliveryAddress`,
    method: 'POST',
    data: addressData
  }).then(res => {
    return res.data;
  });
}

/**
 * 更新收货地址
 * @param {string|number} addressId - 地址ID
 * @param {Object} addressData - 地址数据
 * @returns {Promise<Object>} 更新结果
 */
function updateAddress(addressId, addressData) {
  return request({
    url: `${url}/api/deliveryAddress/${addressId}`,
    method: 'PUT',
    data: addressData
  }).then(res => {
    return res.data;
  });
}

/**
 * 删除收货地址
 * @param {string|number} addressId - 地址ID
 * @returns {Promise<Object>} 删除结果
 */
function deleteAddress(addressId) {
  return request({
    url: `${url}/api/deliveryAddress/${addressId}`,
    method: 'DELETE'
  }).then(res => {
    return res.data;
  });
}

module.exports = {
  getMyAddresses,
  getAddressDetail,
  createAddress,
  updateAddress,
  deleteAddress
};

