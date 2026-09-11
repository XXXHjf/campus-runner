const tokenManager = require('../utils/tokenManager');
const { request } = require('./request');

const url = getApp().globalData.API_URL;
const MAX_UPLOAD_BYTES = 10 * 1024 * 1024;
const MAX_LOCAL_SOURCE_BYTES = 25 * 1024 * 1024;

// Client processing only reduces network traffic. The server remains responsible
// for validating, resizing and normalizing every uploaded image.
const PREPROCESS_POLICIES = {
  ORDER_CATEGORY_ICON: { targetBytes: 1024 * 1024, qualities: [90, 82] },
  SECOND_HAND_CATEGORY_ICON: { targetBytes: 1024 * 1024, qualities: [90, 82] },
  SECOND_HAND_PRODUCT_IMAGE: { targetBytes: 2 * 1024 * 1024, qualities: [88, 80, 72] },
  ORDER_IMAGE: { targetBytes: 2 * 1024 * 1024, qualities: [88, 80, 72] },
  DELIVERY_PROOF: { targetBytes: 2 * 1024 * 1024, qualities: [90, 84, 76] },
  AVATAR: { targetBytes: 1024 * 1024, qualities: [88, 80] },
  STUDENT_CARD: { targetBytes: 4 * 1024 * 1024, qualities: [94, 90, 86] },
  BANNER: { targetBytes: 2 * 1024 * 1024, qualities: [90, 84] },
};

function getFileSize(filePath) {
  return new Promise((resolve, reject) => {
    wx.getFileInfo({
      filePath,
      success: (result) => resolve(result.size),
      fail: reject,
    });
  });
}

function compressImage(filePath, quality) {
  return new Promise((resolve, reject) => {
    wx.compressImage({
      src: filePath,
      quality,
      success: (result) => resolve(result.tempFilePath),
      fail: reject,
    });
  });
}

async function prepareImage(filePath, purpose) {
  const originalSize = await getFileSize(filePath);
  if (originalSize > MAX_LOCAL_SOURCE_BYTES) {
    throw new Error('图片过大，请选择较小的图片');
  }

  const policy = PREPROCESS_POLICIES[purpose];
  if (!policy || originalSize <= policy.targetBytes) {
    if (originalSize > MAX_UPLOAD_BYTES) {
      throw new Error('图片过大，请选择较小的图片');
    }
    return filePath;
  }

  let bestPath = filePath;
  let bestSize = originalSize;
  for (const quality of policy.qualities) {
    try {
      const candidatePath = await compressImage(filePath, quality);
      const candidateSize = await getFileSize(candidatePath);
      if (candidateSize < bestSize) {
        bestPath = candidatePath;
        bestSize = candidateSize;
      }
      if (candidateSize <= policy.targetBytes) {
        break;
      }
    } catch (error) {
      console.warn('[图片上传] 上传前优化失败，继续尝试上传原图', error);
      break;
    }
  }

  if (bestSize > MAX_UPLOAD_BYTES) {
    throw new Error('图片过大，请选择较小的图片');
  }
  return bestPath;
}

function parseUploadResponse(response) {
  // Nginx can reject an upload before it reaches the API and return HTML.
  // Check the HTTP status first; never expose that response body to users.
  if (response.statusCode !== 200) {
    console.error('[图片上传] 请求失败', response.statusCode);
    if (response.statusCode === 413) {
      throw new Error('图片过大，请压缩后重试');
    }
    if (response.statusCode === 401) {
      throw new Error('登录已失效，请重新登录');
    }
    throw new Error('图片上传失败，请稍后重试');
  }

  let body = response.data;
  if (typeof body === 'string') {
    try {
      body = JSON.parse(body);
    } catch (error) {
      console.error('[图片上传] 响应格式异常', response.statusCode);
      throw new Error('图片上传失败，请重试');
    }
  }
  if (!body || body.code !== 1 || !body.data) {
    console.error('[图片上传] 上传未成功', response.statusCode);
    const message = body && typeof body.msg === 'string' && body.msg.trim();
    throw new Error(message || '图片上传失败，请重试');
  }
  return body.data;
}

async function uploadImage(filePath, purpose, onProgress) {
  const uploadPath = await prepareImage(filePath, purpose);
  return new Promise((resolve, reject) => {
    const task = wx.uploadFile({
      url: `${url}/api/media/images`,
      filePath: uploadPath,
      name: 'file',
      formData: { purpose },
      header: {
        token: tokenManager.getToken(),
      },
      timeout: 60000,
      success: (response) => {
        try {
          resolve(parseUploadResponse(response));
        } catch (error) {
          reject(error);
        }
      },
      fail: (error) => {
        console.error('[图片上传] 网络请求失败', error);
        reject(new Error('图片上传失败，请检查网络后重试'));
      },
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
    throw new Error(response.data.msg || '图片删除失败，请重试');
  }
}

module.exports = {
  uploadImage,
  releaseTemporaryImage,
};
