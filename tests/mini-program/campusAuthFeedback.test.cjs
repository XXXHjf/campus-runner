const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const test = require('node:test');
const root = path.resolve(__dirname, '../../apps/mini-program');
const status = require(path.join(root, 'utils/authStatus'));

function harness(name, { submitError = false, refreshError = false } = {}) {
  let definition;
  const state = { submissions: [], errors: [], messages: [], uploads: 0 };
  const feedback = {
    showModal: (_, options) => options.success({ confirm: true }),
    showToast: (_, options) => state.messages.push(options.title),
    showMessage: (_, message) => state.messages.push(message),
  };
  const services = {
    authenticate: async data => {
      state.submissions.push(data);
      if (submitError) throw new Error('提交失败');
    },
    getUserInfo: async () => {
      if (refreshError) throw new Error('读取失败');
      return { ...page.data.userInfo, studentIdCardReview: 1, studentIdCardRejectReason: null };
    },
  };
  vm.runInNewContext(fs.readFileSync(path.join(root, `pages/mine/${name}/${name}.js`), 'utf8'), {
    Page: value => { definition = value; }, console: { log() {}, error() {} },
    getApp: () => ({ globalData: { API_URL: 'https://example.test', userInfo: {} } }),
    require: id => {
      if (id.includes('authStatus')) return status;
      if (id.includes('userService')) return services;
      if (id.includes('mediaService')) return { uploadImage: async () => { state.uploads++; return { mediaId: 42 }; } };
      if (id.includes('tokenManager')) return { getToken: () => 'test' };
      if (id.includes('profileStatus')) return { isProfileComplete: () => true };
      if (id.includes('feedback')) return feedback;
      if (id.includes('transformers')) return { showLoading() {}, hideLoading() {}, showError: (_, message) => state.errors.push(message) };
      if (id.includes('commonJs')) return { showSuccessToast() {}, errorCilcleToast: (_, message) => state.errors.push(message) };
      throw new Error(id);
    },
  });
  const page = { ...definition, data: JSON.parse(JSON.stringify(definition.data)),
    setData(value) { Object.assign(this.data, value); } };
  page.data.userInfo = { id: 9, schoolId: 1, stuId: 'old-id', realname: '旧姓名',
    authentication: 0, studentIdCardReview: 3, studentIdCardRejectReason: '请更正学号', studentIdCardAssetId: 31 };
  page.data.hasAgreedNotice = true;
  return { page, state };
}

test('原因仅在未通过时显示；历史原因缺失明确提示', () => {
  assert.equal(status.getStatusDesc(3, '  更正学号  '), '更正学号');
  assert.match(status.getStatusDesc(3, null), /未提供具体原因/);
  assert.equal(status.getStatusDesc(1, '旧原因'), '已提交，等待审核');
  assert.equal(status.getStatusDesc(2, '旧原因'), '');
});

for (const name of ['identify', 'reIdentify']) {
  test(`${name}: 主动清空姓名不会回退旧值或被同步覆盖`, async () => {
    const { page, state } = harness(name);
    page.data.upName = '';
    page._syncFormFromUserInfo();
    assert.equal(page.data.upName, '');
    await page.identify();
    assert.equal(state.submissions.length, 0);
  });
  test(`${name}: 只改学号可复用原材料；提交成功清原因，即使刷新失败`, async () => {
    const { page, state } = harness(name, { refreshError: true });
    page.data.upSID = 'new-id';
    await page.identify();
    assert.equal(state.submissions[0].stuId, 'new-id');
    assert.equal(state.submissions[0].studentIdCardAssetId, 31);
    assert.equal(state.uploads, 0);
    assert.equal(page.data.userInfo.studentIdCardReview, 1);
    assert.equal(page.data.userInfo.studentIdCardRejectReason, null);
    assert.equal(page.data.isRejected, false);
    assert.equal(state.errors.length, 0);
    assert.match(state.messages.at(-1), /刷新失败/);
  });
  test(`${name}: 提交失败保留原因和修改；审核中重复提交被阻止`, async () => {
    const { page, state } = harness(name, { submitError: true });
    page.data.upSID = 'new-id';
    await page.identify();
    assert.equal(page.data.userInfo.studentIdCardRejectReason, '请更正学号');
    assert.equal(page.data.upSID, 'new-id');
    assert.equal(page.data.submitting, false);
    page.data.userInfo.studentIdCardReview = 1;
    await page.identify();
    assert.equal(state.submissions.length, 1);
  });
}

test('认证服务拒绝业务失败，不把失败响应当作提交成功', async () => {
  let service;
  const module = { exports: {} };
  vm.runInNewContext(fs.readFileSync(path.join(root, 'services/userService.js'), 'utf8'), {
    module, getApp: () => ({ globalData: { API_URL: '' } }),
    require: () => ({ request: async () => ({ data: { code: 0, msg: '材料不可用' } }) }),
  });
  service = module.exports;
  await assert.rejects(service.authenticate({}), /材料不可用/);
});
