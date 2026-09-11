const PROFILE_PAGE = '/pages/mine/newUser/index';
const CAMPUS_AUTH_PAGE = '/pages/mine/identify/identify';
const PLACEHOLDER_USERNAMES = new Set(['微信用户']);
const PHONE_PATTERN = /^1[3-9]\d{9}$/;

function normalizedText(value) {
  return typeof value === 'string' ? value.trim() : '';
}

function hasValidUsername(userInfo) {
  const username = normalizedText(userInfo?.username);
  return username !== '' && !PLACEHOLDER_USERNAMES.has(username);
}

function hasValidPhone(userInfo) {
  return PHONE_PATTERN.test(normalizedText(userInfo?.phone));
}

function hasAvatar(userInfo) {
  return Boolean(userInfo?.headImgAssetId || normalizedText(userInfo?.headImg));
}

function hasSelectedAvatarFile(filePath) {
  return normalizedText(filePath) !== '';
}

function getMissingProfileFields(userInfo) {
  const missing = [];
  if (!hasAvatar(userInfo)) missing.push('avatar');
  if (!hasValidUsername(userInfo)) missing.push('username');
  if (!hasValidPhone(userInfo)) missing.push('phone');
  return missing;
}

function isProfileComplete(userInfo) {
  if (!userInfo) return false;
  if (typeof userInfo.profileCompleted === 'boolean') {
    return userInfo.profileCompleted;
  }
  return getMissingProfileFields(userInfo).length === 0;
}

function getRequiredOnboardingRoute(userInfo) {
  if (!isProfileComplete(userInfo)) return PROFILE_PAGE;

  const authentication = Number(userInfo?.authentication);
  const reviewStatus = Number(userInfo?.studentIdCardReview ?? 0);
  if (authentication !== 1 && (reviewStatus === 0 || reviewStatus === 3)) {
    return CAMPUS_AUTH_PAGE;
  }
  return null;
}

module.exports = {
  PROFILE_PAGE,
  CAMPUS_AUTH_PAGE,
  PHONE_PATTERN,
  hasSelectedAvatarFile,
  getMissingProfileFields,
  isProfileComplete,
  getRequiredOnboardingRoute,
};
