/**
 * 用户订单服务
 * 封装"我发布的"订单相关的 API 调用
 */

const { request, safeList } = require('./request');
const { _delbefore1stBlank } = require('../utils/commonJs');

const url = getApp().globalData.API_URL;

/**
 * 获取我发布的所有订单
 * @returns {Promise<Array>} 订单列表
 */
function getMyOrders() {
  return request({
    url: `${url}/api/order/my`,
    method: 'GET'
  }).then(res => {
    const rawData = safeList(res);
    return _delbefore1stBlank(rawData);
  });
}

/**
 * 获取我发布的订单详情 
 * @param {string|number} orderId - 订单ID
 * @returns {Promise<Object>} 订单详情
 */
function getMyOrderDetail(orderId) {
  console.log('userOrderService - 请求订单详情, ID:', orderId);
  return request({
    url: `${url}/api/order/detail/${orderId}`,
    method: 'GET'
  }).then(res => {
    console.log('userOrderService - 订单详情响应:', res.data);
    return res.data.data || {};
  });
}

/**
 * 创建订单
 * @param {Object} orderData - 订单数据
 * @returns {Promise<Object>} 创建结果
 */
function createOrder(orderData) {
  return request({
    url: `${url}/api/order`,
    method: 'POST',
    data: orderData
  }).then(res => {
    return res.data;
  });
}

/**
 * 更新订单
 * @param {string|number} orderId - 订单ID  
 * @param {Object} orderData - 订单数据
 * @returns {Promise<Object>} 更新结果
 */
function updateOrder(orderId, orderData) {
  return request({
    url: `${url}/api/order/${orderId}`,
    method: 'PUT',
    data: orderData
  }).then(res => {
    return res.data;
  });
}

/**
 * 删除订单
 * @param {string|number} orderId - 订单ID
 * @returns {Promise<Object>} 删除结果
 */
function deleteOrder(orderId) {
  return request({
    url: `${url}/api/order/${orderId}`,
    method: 'DELETE'
  }).then(res => {
    return res.data;
  });
}

/**
 * 取消订单
 * @param {string|number} orderId - 订单ID
 * @param {string} cancelReason - 取消原因
 * @param {string} orderNumber - 订单号（用于后端幂等校验）
 * @returns {Promise<Object>} 取消结果
 */
function cancelOrder(orderId, cancelReason, orderNumber) {
  return request({
    url: `${url}/api/order/cancel`,
    method: 'PUT',
    data: { id: orderId, cancelReason, orderNumber }
  }).then(res => {
    return res.data;
  });
}

/**
 * 发起微信退款
 * @param {string} orderNumber - 商户订单号
 * @param {string} reason - 退款原因
 * @returns {Promise<Object>} 退款结果
 */
function refundOrder(orderNumber, reason) {
  return request({
    url: `${url}/api/wx-pay/refunds`,
    method: 'POST',
    data: { orderNumber, reason }
  }).then(res => {
    return res.data;
  });
}

/**
 * 确认订单
 * @param {string|number} orderId - 订单ID
 * @returns {Promise<Object>} 确认结果
 */
function confirmOrder(orderId) {
  return request({
    url: `${url}/api/order/confirm/${orderId}`,
    method: 'PUT'
  }).then(res => {
    return res.data;
  });
}

/**
 * 获取接单人支付码
 * @param {string|number} orderId - 订单ID
 * @returns {Promise<Object>} 支付码信息
 */
function getTakerPaymentCode(orderId) {
  return request({
    url: `${url}/api/takeOrders/paymentCode/${orderId}`,
    method: 'GET'
  }).then(res => {
    return res.data.data || {};
  });
}

module.exports = {
  getMyOrders,
  getMyOrderDetail,
  createOrder,
  updateOrder,
  deleteOrder,
  cancelOrder,
  refundOrder,
  confirmOrder,
  getTakerPaymentCode
};

