const { request, safeList } = require('./request');

const app = getApp();
const url = app.globalData.API_URL;

function unwrapData(res, fallback = {}) {
  if (res && res.data && res.data.code !== undefined && res.data.code !== 1) {
    throw new Error(res.data.msg || '接口返回异常');
  }
  if (res && res.data && res.data.data !== undefined && res.data.data !== null) {
    return res.data.data;
  }
  return fallback;
}

function cleanQuery(query = {}) {
  const data = {};
  Object.keys(query).forEach((key) => {
    const value = query[key];
    if (value !== undefined && value !== null && value !== '' && value !== 'null' && value !== 'undefined') {
      data[key] = value;
    }
  });
  return data;
}

function listCategories() {
  return request({
    url: `${url}/api/second-hand/categories`,
    method: 'GET',
  }).then(safeList);
}

function listProducts(query = {}) {
  return request({
    url: `${url}/api/second-hand/products`,
    method: 'GET',
    data: cleanQuery(query),
  }).then(safeList);
}

function getProduct(id) {
  return request({
    url: `${url}/api/second-hand/products/${id}`,
    method: 'GET',
  }).then((res) => unwrapData(res));
}

function publishProduct(data) {
  return request({
    url: `${url}/api/second-hand/products`,
    method: 'POST',
    data,
  }).then((res) => unwrapData(res));
}

function updateProduct(id, data) {
  return request({
    url: `${url}/api/second-hand/products/${id}`,
    method: 'PUT',
    data,
  }).then((res) => unwrapData(res));
}

function updateProductStatus(id, status) {
  return request({
    url: `${url}/api/second-hand/products/${id}/status/${status}`,
    method: 'PUT',
  }).then((res) => unwrapData(res));
}

function listMyProducts() {
  return request({
    url: `${url}/api/second-hand/products/my`,
    method: 'GET',
  }).then(safeList);
}

function createOrder(data) {
  return request({
    url: `${url}/api/second-hand/orders`,
    method: 'POST',
    data,
  }).then((res) => unwrapData(res));
}

function listBuyerOrders() {
  return request({
    url: `${url}/api/second-hand/orders/buyer`,
    method: 'GET',
  }).then(safeList);
}

function listSellerOrders() {
  return request({
    url: `${url}/api/second-hand/orders/seller`,
    method: 'GET',
  }).then(safeList);
}

function getOrderDetail(orderId) {
  return request({
    url: `${url}/api/second-hand/orders/${orderId}`,
    method: 'GET',
  }).then((res) => unwrapData(res));
}

function payOrder(orderId) {
  return request({
    url: `${url}/api/second-hand/pay/jsapi/${orderId}`,
    method: 'POST',
  }).then((res) => unwrapData(res));
}

function cancelOrder(orderId, reason) {
  return request({
    url: `${url}/api/second-hand/orders/${orderId}/cancel`,
    method: 'POST',
    data: { reason },
  }).then((res) => unwrapData(res));
}

function markDelivered(orderId) {
  return request({
    url: `${url}/api/second-hand/orders/${orderId}/delivered`,
    method: 'POST',
  }).then((res) => unwrapData(res));
}

function confirmOrder(orderId) {
  return request({
    url: `${url}/api/second-hand/orders/${orderId}/confirm`,
    method: 'POST',
  }).then((res) => unwrapData(res));
}

function mockPaySuccess(orderId) {
  return request({
    url: `${url}/api/dev-payment/second-hand/orders/${orderId}/mock-pay-success`,
    method: 'POST',
  }).then((res) => unwrapData(res));
}

function mockReceiveSuccess(orderId) {
  return request({
    url: `${url}/api/dev-payment/second-hand/orders/${orderId}/mock-receive-success`,
    method: 'POST',
  }).then((res) => unwrapData(res));
}

function createBargain(data) {
  return request({
    url: `${url}/api/second-hand/bargains`,
    method: 'POST',
    data,
  }).then((res) => unwrapData(res));
}

function listMyBargains() {
  return request({
    url: `${url}/api/second-hand/bargains/my`,
    method: 'GET',
  }).then(safeList);
}

function listProductBargains(productId) {
  return request({
    url: `${url}/api/second-hand/products/${productId}/bargains`,
    method: 'GET',
  }).then(safeList);
}

function acceptBargain(bargainId, data = {}) {
  return request({
    url: `${url}/api/second-hand/bargains/${bargainId}/accept`,
    method: 'POST',
    data,
  }).then((res) => unwrapData(res));
}

function rejectBargain(bargainId) {
  return request({
    url: `${url}/api/second-hand/bargains/${bargainId}/reject`,
    method: 'POST',
  }).then((res) => unwrapData(res));
}

function sendMessage(data) {
  return request({
    url: `${url}/api/second-hand/messages`,
    method: 'POST',
    data,
  }).then((res) => unwrapData(res));
}

function listProductMessages(productId) {
  return request({
    url: `${url}/api/second-hand/products/${productId}/messages`,
    method: 'GET',
  }).then(safeList);
}

module.exports = {
  listCategories,
  listProducts,
  getProduct,
  publishProduct,
  updateProduct,
  updateProductStatus,
  listMyProducts,
  createOrder,
  listBuyerOrders,
  listSellerOrders,
  getOrderDetail,
  payOrder,
  cancelOrder,
  markDelivered,
  confirmOrder,
  mockPaySuccess,
  mockReceiveSuccess,
  createBargain,
  listMyBargains,
  listProductBargains,
  acceptBargain,
  rejectBargain,
  sendMessage,
  listProductMessages,
};
