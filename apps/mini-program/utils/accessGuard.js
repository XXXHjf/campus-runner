const tokenManager = require('./tokenManager');
const { PROFILE_PAGE, CAMPUS_AUTH_PAGE, isProfileComplete } = require('./profileStatus');
let loginPrompt = null;
let guidanceVisible = false;

function confirm(options) {
  return new Promise((resolve) => wx.showModal({ ...options,
    success: (res) => resolve(!!res.confirm), fail: () => resolve(false) }));
}

async function ensureLogin() {
  if (tokenManager.hasToken()) return true;
  if (loginPrompt) return loginPrompt;
  loginPrompt = (async () => {
    if (!await confirm({ title: '登录后继续',
      content: '此操作需要登录。取消后仍可浏览商品和跑腿服务。',
      confirmText: '去登录', cancelText: '取消' })) return false;
    const ok = await getApp().silentLogin();
    if (!ok) wx.showToast({ title: '登录失败，请稍后重试', icon: 'none' });
    return !!ok;
  })();
  try { return await loginPrompt; } finally { loginPrompt = null; }
}

async function guideAuthentication(user = {}) {
  if (guidanceVisible) return;
  guidanceVisible = true;
  try {
    const pending = Number(user.studentIdCardReview) === 1;
    const rejected = Number(user.studentIdCardReview) === 3;
    const accepted = await confirm({
      title: pending ? '校园认证审核中' : (rejected ? '校园认证未通过' : '需要校园认证'),
      content: pending ? '认证材料正在审核，通过后即可操作。您可以继续浏览，或查看审核进度。'
        : (rejected ? '请查看未通过原因，修改材料后重新提交。审核通过前仍可继续浏览。'
          : '完成校园认证后才能操作。请先完善个人资料并提交认证材料，您也可以暂不认证、继续浏览。'),
      confirmText: pending ? '查看进度' : '去认证',
      cancelText: pending ? '继续浏览' : '暂不认证',
    });
    if (accepted) wx.navigateTo({ url: !isProfileComplete(user) && !pending ? PROFILE_PAGE : CAMPUS_AUTH_PAGE });
  } finally { guidanceVisible = false; }
}

async function ensureAuthenticated() {
  if (!await ensureLogin()) return false;
  try {
    const user = await require('../services/userService').getUserInfo();
    if (Number(user.authentication) === 1) return true;
    await guideAuthentication(user);
  } catch (error) {
    wx.showToast({ title: '认证状态加载失败，请稍后重试', icon: 'none' });
  }
  return false;
}

function returnToBrowse() {
  wx.navigateBack({ fail: () => wx.switchTab({ url: '/pages/second-hand/index/index' }) });
}
module.exports = { ensureLogin, ensureAuthenticated, guideAuthentication, returnToBrowse };
