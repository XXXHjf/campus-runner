/**
 * 系统配置服务
 * 封装服务费率和最低服务费相关接口
 */

const { request } = require('./request');

const url = getApp().globalData.API_URL;

/**
 * 获取服务费率
 * @returns {Promise<number>} 服务费率（如 0.05）
 */
function getServiceFeeRate() {
  return request({
    url: `${url}/admin/api/config/service_fee_rate`,
    method: 'GET'
  }).then((res) => Number(res.data?.data));
}

/**
 * 获取最低服务费
 * @returns {Promise<number>} 最低服务费（如 0.5）
 */
function getServiceFeeMin() {
  return request({
    url: `${url}/admin/api/config/service_fee_min`,
    method: 'GET'
  }).then((res) => Number(res.data?.data));
}

module.exports = {
  getServiceFeeRate,
  getServiceFeeMin
};

