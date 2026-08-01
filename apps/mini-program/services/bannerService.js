/**
 * 首页轮播图服务
 */

const { request, safeList } = require('./request');

const url = getApp().globalData.API_URL;

function getBannersBySchool(schoolId) {
  return request({
    url: `${url}/admin/api/banner/getList/${schoolId}`,
    method: 'GET',
    skipTokenCheck: true
  }).then(safeList);
}

async function getHomepageBanners(schoolId) {
  const normalizedSchoolId = Number(schoolId) || 0;
  const commonPromise = getBannersBySchool(0).catch((error) => {
    console.error('获取通用轮播图失败:', error);
    return [];
  });

  const schoolPromise = normalizedSchoolId === 0
    ? Promise.resolve([])
    : getBannersBySchool(normalizedSchoolId).catch((error) => {
      console.error('获取学校轮播图失败:', error);
      return [];
    });

  const [commonBanners, schoolBanners] = await Promise.all([
    commonPromise,
    schoolPromise
  ]);
  const uniqueBanners = new Map();

  [...schoolBanners, ...commonBanners].forEach((banner) => {
    if (!banner || !banner.imgUrl) return;
    const key = banner.id == null ? banner.imgUrl : String(banner.id);
    if (!uniqueBanners.has(key)) {
      uniqueBanners.set(key, banner);
    }
  });

  return Array.from(uniqueBanners.values());
}

module.exports = {
  getHomepageBanners
};
