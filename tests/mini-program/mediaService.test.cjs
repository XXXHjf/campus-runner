const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const vm = require('node:vm');

const filename = path.resolve(__dirname, '../../apps/mini-program/services/mediaService.js');
const source = fs.readFileSync(filename, 'utf8');
const purpose = 'SECOND_HAND_PRODUCT_IMAGE';
const uploaded = { mediaId: 123, previewUrl: 'https://example.test/image.jpg' };

function createHarness(response, { sizes = {}, networkError = false } = {}) {
  const uploads = [];
  const compressions = [];
  const logs = [];
  const module = { exports: {} };
  vm.runInNewContext(source, {
    module,
    require(id) {
      if (id === '../utils/tokenManager') return { getToken: () => 'test-token' };
      if (id === './request') return { request: async () => ({ data: { code: 1 } }) };
      throw new Error(`Unexpected dependency: ${id}`);
    },
    getApp: () => ({ globalData: { API_URL: 'https://example.test' } }),
    console: {
      error: (...args) => logs.push(args),
      warn: (...args) => logs.push(args),
    },
    wx: {
      getFileInfo({ filePath, success }) {
        success({ size: sizes[filePath] ?? 1024 });
      },
      compressImage({ src, quality, success }) {
        compressions.push({ src, quality });
        success({ tempFilePath: `compressed-${quality}.jpg` });
      },
      uploadFile(options) {
        uploads.push(options);
        if (networkError) {
          options.fail({ errMsg: 'uploadFile:fail timeout' });
        } else {
          options.success(response);
        }
        return { onProgressUpdate: (callback) => callback({ progress: 100 }) };
      },
    },
  }, { filename });
  return { service: module.exports, uploads, compressions, logs };
}

for (const data of [JSON.stringify({ code: 1, data: uploaded }), { code: 1, data: uploaded }]) {
  test(`successful upload accepts ${typeof data} responses and reports progress`, async () => {
    const { service, uploads } = createHarness({ statusCode: 200, data });
    const progress = [];
    const result = await service.uploadImage('small.jpg', purpose, (value) => progress.push(value));
    assert.equal(result.mediaId, uploaded.mediaId);
    assert.equal(result.previewUrl, uploaded.previewUrl);
    assert.equal(uploads[0].formData.purpose, purpose);
    assert.deepEqual(progress, [100]);
  });
}

for (const { statusCode, data, message } of [
  { statusCode: 413, data: '<html>413 Request Entity Too Large</html>', message: '图片过大，请压缩后重试' },
  { statusCode: 413, data: '{"code":1,"data":{"mediaId":123}}', message: '图片过大，请压缩后重试' },
  { statusCode: 401, data: '<html>Unauthorized</html>', message: '登录已失效，请重新登录' },
  { statusCode: 502, data: '<html>Bad Gateway</html>', message: '图片上传失败，请稍后重试' },
  { statusCode: 500, data: '{"msg":"internal implementation details"}', message: '图片上传失败，请稍后重试' },
]) {
  test(`HTTP ${statusCode} is handled before parsing its body: ${data}`, async () => {
    const { service, logs } = createHarness({ statusCode, data });
    await assert.rejects(service.uploadImage('small.jpg', purpose), { message });
    assert.deepEqual(logs, [['[图片上传] 请求失败', statusCode]]);
  });
}

test('malformed success responses produce a readable error without logging HTML', async () => {
  const { service, logs } = createHarness({ statusCode: 200, data: '<html>unexpected page</html>' });
  await assert.rejects(service.uploadImage('small.jpg', purpose), { message: '图片上传失败，请重试' });
  assert.deepEqual(logs, [['[图片上传] 响应格式异常', 200]]);
});

test('API business errors preserve the actionable message', async () => {
  const message = '图片文件过大，请选择不超过 10MB 的图片';
  const { service } = createHarness({ statusCode: 200, data: JSON.stringify({ code: 0, msg: message }) });
  await assert.rejects(service.uploadImage('small.jpg', purpose), { message });
});

test('invalid or missing response payloads do not become successful uploads', async () => {
  for (const data of [null, '', 'null', '[]', '{"code":1}', { code: 0, msg: { internal: 'details' } }]) {
    const { service } = createHarness({ statusCode: 200, data });
    await assert.rejects(service.uploadImage('small.jpg', purpose), { message: '图片上传失败，请重试' });
  }
});

test('an 11MB source uses the compressed file when it is under the 2MB target', async () => {
  const { service, uploads, compressions } = createHarness(
    { statusCode: 200, data: { code: 1, data: uploaded } },
    { sizes: { 'original.jpg': 11466018, 'compressed-88.jpg': 1727000 } },
  );
  await service.uploadImage('original.jpg', purpose);
  assert.deepEqual(compressions, [{ src: 'original.jpg', quality: 88 }]);
  assert.equal(uploads[0].filePath, 'compressed-88.jpg');
});

test('images still exceeding 10MB after compression are rejected before upload', async () => {
  const sizes = Object.fromEntries(
    ['original.jpg', 'compressed-88.jpg', 'compressed-80.jpg', 'compressed-72.jpg']
      .map((name) => [name, 11466018]),
  );
  const { service, uploads } = createHarness(null, { sizes });
  await assert.rejects(service.uploadImage('original.jpg', purpose), { message: '图片过大，请选择较小的图片' });
  assert.equal(uploads.length, 0);
});

test('network failures do not expose platform error messages', async () => {
  const { service } = createHarness(null, { networkError: true });
  await assert.rejects(service.uploadImage('small.jpg', purpose), { message: '图片上传失败，请检查网络后重试' });
});
