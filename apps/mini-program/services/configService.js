/**
 * 系统配置服务
 * 封装服务费率和最低服务费相关接口
 */

const { request } = require('./request');

const url = getApp().globalData.API_URL;

function configNumber(res) {
  const value = res.data?.data;
  if ((typeof value !== 'number' && typeof value !== 'string') || String(value).trim() === ''
      || !Number.isFinite(Number(value)) || Number(value) < 0) {
    throw new Error('服务信息加载失败，请稍后重试');
  }
  return Number(value);
}


/**
 * 获取服务费率
 * @returns {Promise<number>} 服务费率（如 0.05）
 */
function getServiceFeeRate() {
  return request({
    url: `${url}/admin/api/config/service_fee_rate`,
    method: 'GET'
  }).then(configNumber);
}

/**
 * 获取最低服务费
 * @returns {Promise<number>} 最低服务费（如 0.5）
 */
function getServiceFeeMin() {
  return request({
    url: `${url}/admin/api/config/service_fee_min`,
    method: 'GET'
  }).then(configNumber);
}

function getRunnerTransferSingleMax() {
  return request({
    url: `${url}/admin/api/config/runner_transfer_single_max`,
    method: 'GET'
  }).then(configNumber);
}

module.exports = {
  getServiceFeeRate,
  getServiceFeeMin,
  getRunnerTransferSingleMax
};

