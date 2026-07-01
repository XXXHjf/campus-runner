/**
 * 订单参数构造器
 * 统一处理各种筛选条件的参数构造
 */

/**
 * 标准化学校ID
 * 从用户绑定的学校ID获取标准格式的学校编号
 * @param {string|number} userSchoolId - 用户学校ID
 * @returns {string} 标准化的学校编号
 */
function normalizeSchoolId(userSchoolId) {
  if (!userSchoolId) {
    return '1030000000000'; // 默认学校ID
  }
  const schoolIdStr = userSchoolId.toString();
  return schoolIdStr.slice(0, 3) + '0000000000';
}

/**
 * 构造单向地址筛选参数
 * @param {Object} addressData - 地址数据 { value: [campusId, categoryId, buildingId] }
 * @param {string} type - 筛选类型 ('showByPickUpAdd' | 'showByReciveAdd')
 * @param {string|number} userSchoolId - 用户学校ID
 * @returns {Object} 请求参数
 */
function buildSingleParams(addressData, type, userSchoolId) {
  const schoolNumberId = normalizeSchoolId(userSchoolId);
  const campusNumberId = addressData.value[0]; // 校区编号
  const buildCategoryNumberId = addressData.value[1]; // 类型编号
  const buildingNumberId = addressData.value[2]; // 楼宇编号
  
  return {
    schoolNumberId: schoolNumberId,
    campusNumberId: campusNumberId,
    buildCategoryNumberId: buildCategoryNumberId,
    buildingNumberId: buildingNumberId
  };
}

/**
 * 构造双向地址筛选参数
 * @param {Object} pickupData - 取件地址数据 { value: [campusId, categoryId, buildingId] }
 * @param {Object} receiveData - 收件地址数据 { value: [campusId, categoryId, buildingId] }
 * @param {string|number} userSchoolId - 用户学校ID
 * @returns {Object} 请求参数
 */
function buildDoubleParams(pickupData, receiveData, userSchoolId) {
  const schoolNumberId = normalizeSchoolId(userSchoolId);
  
  // 取件地址参数
  const pickCampusNumberId = pickupData.value[0]; // 取件校区编号
  const pickBuildCategoryNumberId = pickupData.value[1]; // 取件类型编号
  const pickBuildingNumberId = pickupData.value[2]; // 取件楼宇编号
  
  // 收件地址参数
  const reciveCampusNumberId = receiveData.value[0]; // 收件校区编号
  const reciveBuildCategoryNumberId = receiveData.value[1]; // 收件类型编号
  const reciveBuildingNumberId = receiveData.value[2]; // 收件楼宇编号
  
  return {
    pickSchoolNumberId: schoolNumberId,
    pickCampusNumberId: pickCampusNumberId,
    pickBuildCategoryNumberId: pickBuildCategoryNumberId,
    pickBuildingNumberId: pickBuildingNumberId,
    reciveSchoolNumberId: schoolNumberId,
    reciveCampusNumberId: reciveCampusNumberId,
    reciveBuildCategoryNumberId: reciveBuildCategoryNumberId,
    reciveBuildingNumberId: reciveBuildingNumberId
  };
}

module.exports = {
  normalizeSchoolId,
  buildSingleParams,
  buildDoubleParams
};
