/**
 * 用户服务
 * 封装用户相关的 API 调用
 */

const { request } = require('./request');

const url = getApp().globalData.API_URL;

/**
 * 获取用户信息
 * @returns {Promise<Object>} 用户信息
 */
function getUserInfo() {
  return request({
    url: `${url}/api/user`,
    method: 'GET'
  }).then(res => {
    return res.data.data || {};
  });
}

/**
 * 更新用户信息
 * @param {Object} userInfo - 用户信息 { phone, username, headImg, sex }
 * @returns {Promise<Object>} 更新结果
 */
function updateUserInfo(userInfo) {
  return request({
    url: `${url}/api/user/update`,
    method: 'PUT',
    data: userInfo
  }).then(res => {
    return res.data;
  });
}

/**
 * 用户认证
 * @param {Object} authData - 认证数据 { schoolId, realname, stuId, studentIdCard }
 * @returns {Promise<Object>} 认证结果
 */
function authenticate(authData) {
  return request({
    url: `${url}/api/user`,
    method: 'PUT',
    data: authData
  }).then(res => {
    return res.data;
  });
}

/**
 * 获取学校列表
 * @returns {Promise<Array>} 学校列表
 */
function getSchools() {
  return request({
    url: `${url}/api/school`,
    method: 'GET'
  }).then(res => {
    return res.data.data || [];
  });
}

module.exports = {
  getUserInfo,
  updateUserInfo,
  authenticate,
  getSchools
};

