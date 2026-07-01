/**
 * 接单服务
 * 封装"我的接单"相关的 API 调用
 */

const { request, safeList } = require('./request');
const { _delbefore1stBlank } = require('../utils/commonJs');

const url = getApp().globalData.API_URL;

/**
 * 获取我接的所有订单
 * @returns {Promise<Array>} 接单列表
 */
function getMyTakeOrders() {
  return request({
    url: `${url}/api/takeOrders`,
    method: 'GET'
  }).then(res => {
    const rawData = safeList(res);
    return _delbefore1stBlank(rawData);
  });
}

/**
 * 根据状态查询接单
 * @param {number} status - 订单状态
 * @returns {Promise<Array>} 接单列表
 */
function getTakeOrdersByStatus(status) {
  return request({
    url: `${url}/api/takeOrders/query`,
    method: 'GET',
    data: { status }
  }).then(res => {
    const rawData = safeList(res);
    return _delbefore1stBlank(rawData);
  });
}

/**
 * 根据orderId查询,返回接单人用户信息
 * @param {string|number} orderId - 订单ID
 * @returns {Promise<Object>} 接单详情
 */
function getTakeOrderDetail(orderId) {
  return request({
    url: `${url}/api/takeOrders/${orderId}`,
    method: 'GET'
  }).then(res => {
    return res.data.data || {};
  });
}

/**
 * 接单
 * @param {string|number} orderId - 订单ID
 * @returns {Promise<Object>} 接单结果
 */
function acceptOrder(orderId) {
  return request({
    url: `${url}/api/takeOrders`,
    method: 'POST',
    data: { orderId }
  }).then(res => {
    return res.data;
  });
}

/**
 * 更新接单状态
 * @param {Object} statusData - 状态数据（必须包含id字段）
 * @returns {Promise<Object>} 更新结果
 */
function updateTakeOrderStatus(statusData) {
  return request({
    url: `${url}/api/takeOrders`,
    method: 'PUT',
    data: statusData
  }).then(res => {
    return res.data;
  });
}

/**
 * 取消接单
 * @param {string|number} takeOrderId - 接单ID
 * @returns {Promise<Object>} 取消结果
 */
function cancelTakeOrder(takeOrderId) {
  return request({
    url: `${url}/api/takeOrders/cancel/${takeOrderId}`,
    method: 'PUT'
  }).then(res => {
    return res.data;
  });
}

/**
 * 获取送达图片
 * @param {string|number} orderId - 订单ID
 * @returns {Promise<string>} 图片URL
 */
function getDeliveryImage(orderId) {
  return request({
    url: `${url}/api/takeOrders/image/${orderId}`,
    method: 'GET'
  }).then(res => {
    return res.data.data || null;
  });
}

/**
 * 接单（通过订单ID）
 * @param {string|number} orderId - 订单ID
 * @returns {Promise<Object>} 接单结果
 */
function acceptOrderById(orderId) {
  return request({
    url: `${url}/api/takeOrders/${orderId}`,
    method: 'POST'
  }).then(res => {
    return res.data;
  });
}

/**
 * 发送接单订阅消息
 * @param {string|number} orderId - 订单ID
 * @param {string|number} userId - 用户ID
 * @returns {Promise<Object>} 发送结果
 */
function sendTakeOrderMessage(orderId, userId) {
  return request({
    url: `${url}/api/message/alreadyTakeOrder`,
    method: 'POST',
    data: { orderId, takeOrderUserId: userId }
  }).then(res => {
    return res.data;
  });
}

/**
 * 发送取件订阅消息
 * @param {string|number} orderId - 订单ID
 * @returns {Promise<Object>} 发送结果
 */
function sendPickupMessage(orderId) {
  return request({
    url: `${url}/api/message/pickUp?orderId=${orderId}`,
    method: 'POST'
  }).then(res => {
    return res.data;
  });
}

/**
 * 发送送达订阅消息
 * @param {string|number} orderId - 订单ID
 * @param {string|number} userId - 用户ID
 * @returns {Promise<Object>} 发送结果
 */
function sendDeliveredMessage(orderId, userId) {
  return request({
    url: `${url}/api/message/delivered`,
    method: 'POST',
    data: { orderId, takeOrderUserId: userId }
  }).then(res => {
    return res.data;
  });
}

/**
 * 获取已完成但未收款的订单
 * @returns {Promise<Array>} 未收款订单列表
 */
function getNotWithdrawnOrders() {
  return request({
    url: `${url}/api/takeOrders/notWithdrawn`,
    method: 'GET'
  }).then(res => {
    return res.data.data || [];
  });
}

module.exports = {
  getMyTakeOrders,
  getTakeOrdersByStatus,
  getTakeOrderDetail,
  acceptOrder,
  updateTakeOrderStatus,
  cancelTakeOrder,
  getDeliveryImage,
  acceptOrderById,
  sendTakeOrderMessage,
  sendPickupMessage,
  sendDeliveredMessage,
  getNotWithdrawnOrders
};

