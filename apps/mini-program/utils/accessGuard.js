const feedback = require('./feedback');
const tokenManager = require('./tokenManager');
const { PROFILE_PAGE, CAMPUS_AUTH_PAGE, isProfileComplete } = require('./profileStatus');
let loginPrompt = null;
let guidanceVisible = false;

function confirm(context, options) {
  return new Promise((resolve) => feedback.showModal(context, { ...options, allowDuringAction: true,
    success: (res) => resolve(!!res.confirm), fail: () => resolve(false) }));
}

async function ensureLogin() {
  const context = feedback.currentPage();
  if (loginPrompt) return loginPrompt;
  loginPrompt = (async () => {
    if (!tokenManager.hasToken() && tokenManager.hasLoginIntent?.()) {
      await getApp().restoreLogin();
    }
    if (!tokenManager.hasToken()) {
      if (!await confirm(context, { title: '登录后继续',
        content: '此操作需要登录。取消后仍可浏览商品和跑腿服务。',
        confirmText: '去登录', cancelText: '取消' })) return false;
      const ok = await getApp().silentLogin();
      if (!ok) {
        feedback.showToast(context, { title: '登录失败，请稍后重试', theme: 'error' });
        return false;
      }
    }
    // 微信身份凭证不代表资料注册完成；历史未完成账号也必须检查。
    try {
      const user = await require('../services/userService').getUserInfo();
      if (isProfileComplete(user)) return true;
      wx.navigateTo({ url: `${PROFILE_PAGE}?after=login` });
    } catch (error) {
      feedback.showToast(context, { title: '资料加载失败，请稍后重试', theme: 'error' });
    }
    return false;
  })();
  try { return await loginPrompt; } finally { loginPrompt = null; }
}

async function guideAuthentication(user = {}) {
  const context = feedback.currentPage();
  if (guidanceVisible) return;
  guidanceVisible = true;
  try {
    const pending = Number(user.studentIdCardReview) === 1;
    const rejected = Number(user.studentIdCardReview) === 3;
    const accepted = await confirm(context, {
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
  const context = feedback.currentPage();
  if (!await ensureLogin()) return false;
  try {
    const user = await require('../services/userService').getUserInfo();
    if (Number(user.authentication) === 1) return true;
    await guideAuthentication(user);
  } catch (error) {
    feedback.showToast(context, { title: '认证状态加载失败，请稍后重试', theme: 'error' });
  }
  return false;
}

function returnToBrowse() {
  wx.navigateBack({ fail: () => wx.switchTab({ url: '/pages/second-hand/index/index' }) });
}
module.exports = { ensureLogin, ensureAuthenticated, guideAuthentication, returnToBrowse };
