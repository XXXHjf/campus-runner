/**
 * 订单相关服务
 * 封装所有订单相关的 API 调用
 */

const { request, safeList } = require('./request');
const { buildSingleParams, buildDoubleParams } = require('../utils/orderParamBuilder');
const { addExpectTime } = require('../utils/transformers');

const url = getApp().globalData.API_URL;

/**
 * 获取订单信息 - 综合排序
 * @returns {Promise<Array>} 订单列表
 */
function getOrdersByTime() {
  return request({
    url: `${url}/api/order/showByTime/0`,
    method: 'GET'
  }).then(res => {
    const rawData = safeList(res);
    return addExpectTime(rawData);
  });
}

/**
 * 获取订单信息 - 价格排序
 * @param {number} status - 排序状态 (0: 从高到低, 1: 从低到高)
 * @returns {Promise<Array>} 订单列表
 */
function getOrdersByPrice(status) {
  return request({
    url: `${url}/api/order/showByPrice/${status}`,
    method: 'GET'
  }).then(res => {
    const rawData = safeList(res);
    return addExpectTime(rawData);
  });
}

/**
 * 获取订单信息 - 分类筛选
 * @param {number} categoryId - 分类ID
 * @returns {Promise<Array>} 订单列表
 */
function getOrdersByCategory(categoryId) {
  return request({
    url: `${url}/api/order/showByCategory/${categoryId}`,
    method: 'GET'
  }).then(res => {
    const rawData = safeList(res);
    return addExpectTime(rawData);
  });
}

/**
 * 获取订单信息 - 单向地址筛选
 * @param {Object} addressData - 地址数据
 * @param {string} type - 筛选类型 ('showByPickUpAdd' | 'showByReciveAdd')
 * @param {string|number} userSchoolId - 用户学校ID
 * @returns {Promise<Array>} 订单列表
 */
function getOrdersBySingleAddress(addressData, type, userSchoolId) {
  const params = buildSingleParams(addressData, type, userSchoolId);
  
  return request({
    url: `${url}/api/order/${type}`,
    method: 'GET',
    data: params
  }).then(res => {
    const rawData = safeList(res);
    return addExpectTime(rawData);
  });
}

/**
 * 获取订单信息 - 双向地址筛选
 * @param {Object} pickupData - 取件地址数据
 * @param {Object} receiveData - 收件地址数据
 * @param {string|number} userSchoolId - 用户学校ID
 * @returns {Promise<Array>} 订单列表
 */
function getOrdersByDoubleAddress(pickupData, receiveData, userSchoolId) {
  const params = buildDoubleParams(pickupData, receiveData, userSchoolId);
  
  return request({
    url: `${url}/api/order/showByDoubleAdd`,
    method: 'GET',
    data: params
  }).then(res => {
    const rawData = safeList(res);
    return addExpectTime(rawData);
  });
}

module.exports = {
  getOrdersByTime,
  getOrdersByPrice,
  getOrdersByCategory,
  getOrdersBySingleAddress,
  getOrdersByDoubleAddress
};
