const tokenManager = require('../utils/tokenManager');
const { request } = require('./request');

const url = getApp().globalData.API_URL;

function parseUploadResponse(response) {
  let body = response.data;
  if (typeof body === 'string') {
    try {
      body = JSON.parse(body);
    } catch (error) {
      throw new Error('图片上传响应格式异常');
    }
  }
  if (response.statusCode !== 200 || !body || body.code !== 1 || !body.data) {
    throw new Error((body && body.msg) || `图片上传失败: ${response.statusCode}`);
  }
  return body.data;
}

function uploadImage(filePath, purpose, onProgress) {
  return new Promise((resolve, reject) => {
    const task = wx.uploadFile({
      url: `${url}/api/media/images`,
      filePath,
      name: 'file',
      formData: { purpose },
      header: {
        token: tokenManager.getToken(),
      },
      timeout: 30000,
      success: (response) => {
        try {
          resolve(parseUploadResponse(response));
        } catch (error) {
          reject(error);
        }
      },
      fail: reject,
    });

    if (onProgress && task && typeof task.onProgressUpdate === 'function') {
      task.onProgressUpdate((event) => onProgress(event.progress));
    }
  });
}

async function releaseTemporaryImage(mediaId) {
  if (!mediaId) {
    return;
  }
  const response = await request({
    url: `${url}/api/media/images/${mediaId}`,
    method: 'DELETE',
  });
  if (response.data && response.data.code !== undefined && response.data.code !== 1) {
    throw new Error(response.data.msg || '释放临时图片失败');
  }
}

module.exports = {
  uploadImage,
  releaseTemporaryImage,
};
