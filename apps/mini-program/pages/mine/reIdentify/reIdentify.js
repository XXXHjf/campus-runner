const feedback = require('../../../utils/feedback');
const { toast: Toast } = require('../../../utils/feedback');

// 引入服务和工具
const userService = require('../../../services/userService');
const mediaService = require('../../../services/mediaService');
const tokenManager = require('../../../utils/tokenManager');
const { showLoading, hideLoading, showError } = require('../../../utils/transformers');
const {
  _getUserInfo,
  _schlsAll2schlNameOnly,
  _getOptions,
  errorCilcleToast,
  showSuccessToast
} = require('../../../utils/commonJs');
const {
  STUDENT_ID_CARD_REVIEW_STATUS: AUTH_STATUS,
  getStatusText,
  getStatusTheme,
  getStatusIcon,
  getStatusTitle,
  getStatusDesc
} = require('../../../utils/authStatus');

Page({
  retryFeedbackLoad() { return this._feedbackRetry?.(); },

  /**
   * 页面的初始数据
   */
  data: {
    userInfo: null,
    upSchoolId: null, //选择的学校的ID
    upSID: null, //输入的学号
    upName: null, //输入的真实姓名
    pickerVisible: false, //选择器可见性
    onPickValue: null, //选择器取值
    schools: null,
    
    // 校园认证相关
    studentIdCardUrl: null, // 学生证照片本地路径
    uploadedStudentIdCardAssetId: null,
    
    // 认证须知弹窗
    showAuthNotice: false, // 是否显示认证须知弹窗
    hasAgreedNotice: false, // 是否已同意认证须知

    // 认证状态展示（派生数据）
    authStatusText: '',
    authStatusTheme: 'default',
    authStatusIcon: 'info-circle',
    authStatusTitle: '',
    authStatusDesc: '',
    isRejected: false,
    submitting: false,
    canSubmit: true,
  },

  // 用户同意认证须知
  onAgreeNotice() {
    this.setData({
      showAuthNotice: false,
      hasAgreedNotice: true
    });
    console.log('用户已同意认证须知');
  },
  
  // 用户不同意认证须知
  onDisagreeNotice() {
    this.setData({
      showAuthNotice: false,
      hasAgreedNotice: false
    });
    wx.navigateBack();
  },
  
  // 点击"选择学校"，跳出选择器
  changeSchoolNew() {
    if (this.data.submitting) return;
    if (!this.data.canSubmit) {
      feedback.showMessage(this, '认证材料审核中，暂不可修改', { theme: 'info' });
      return;
    }
    if (this.data.schools) {
      this.setData({
        pickerVisible: true
      });
    } else {
      console.log("schools null In changeSchoolNew");
    }
  },
  // picker选择器选中变化时候触发，即确认变化时触发
  onPickerChange(e) {
    const {
      value,
      label
    } = e.detail;
    console.log("01", label);
    console.log('picker confirm:', e.detail);
    this.setData({
      pickerVisible: false,
      onPickValue: label[0],
      upSchoolId: e.detail.value[0],
    });
  },
  // picker选择器点击取消按钮时触发
  onPickerCancel(e) {
    this.setData({
      pickerVisible: false,
    });
  },
  // 获取所有学校，用于选择列表（使用封装的 service）
  async _getAllSchool() {
    try {
      return await userService.getSchools();
    } catch (error) {
      console.error('获取学校列表失败:', error);
      feedback.showMessage(this, '学校列表加载失败，请重试', { theme: 'error', action: '重试', onAction: () => this.onShow() });
      throw error;
    }
  },
  // 输入姓名
  handleName(e) {
    this.setData({
      upName: e.detail.value.replace(/\s+/g, '')
    })
  },
  // 输入学号
  handleStudentId(e) {
    this.setData({
      upSID: e.detail.value.replace(/\s+/g, '')
    })
  },
  // 选择学生证照片
  async chooseStudentIdCard() {
    if (!this.data.canSubmit || this.data.submitting) return;
    try {
      const res = await new Promise((resolve, reject) => {
        wx.chooseMedia({
          count: 1,
          mediaType: ['image'],
          sourceType: ['album', 'camera'],
          success: resolve,
          fail: reject
        });
      });
      
      const tempFilePath = res.tempFiles[0].tempFilePath;
      
      if (this.data.uploadedStudentIdCardAssetId) {
        mediaService.releaseTemporaryImage(this.data.uploadedStudentIdCardAssetId).catch(() => {});
      }
      this.setData({ studentIdCardUrl: tempFilePath, uploadedStudentIdCardAssetId: null });
      console.log('选择学生证照片成功');
    } catch (err) {
      if (/cancel/i.test(err?.errMsg || err?.message || '')) return;
      console.error('选择图片失败:', err);
      feedback.showToast(this, {
        title: '选择图片失败',
        theme: 'error',
      });
    }
  },
  // 删除学生证照片
  deleteStudentIdCard() {
    if (!this.data.canSubmit) {
      feedback.showMessage(this, '认证材料审核中，暂不可修改', { theme: 'info' });
      return;
    }
    if (this.data.uploadedStudentIdCardAssetId) {
      mediaService.releaseTemporaryImage(this.data.uploadedStudentIdCardAssetId).catch(() => {});
    }
    this.setData({
      studentIdCardUrl: null,
      uploadedStudentIdCardAssetId: null
    });
  },
  previewStudentIdCard() {
    const imageUrl = this.data.studentIdCardUrl || this.data.userInfo?.studentIdCard;
    if (!imageUrl) return;
    wx.previewImage({
      current: imageUrl,
      urls: [imageUrl]
    });
  },
  // 上传学生证照片到服务器
  async uploadStudentIdCard() {
    if (!this.data.studentIdCardUrl) {
      throw new Error('未选择学生证照片');
    }
    const uploaded = await mediaService.uploadImage(
      this.data.studentIdCardUrl,
      'STUDENT_CARD',
    );
    this.setData({ uploadedStudentIdCardAssetId: uploaded.mediaId });
    return uploaded.mediaId;
  },
  // 确认修改（使用封装的 service）
  async identify() {
    if (this.data.submitting) return;
    const status = Number(this.data.userInfo?.studentIdCardReview ?? AUTH_STATUS.UNREVIEWED);
    if (status === AUTH_STATUS.PENDING) {
      feedback.showMessage(this, '认证材料审核中，请勿重复提交', { theme: 'info' });
      return;
    }

    if (!this.data.hasAgreedNotice) {
      this.setData({ showAuthNotice: true });
      return;
    }

    const effectiveSchoolId = this.data.upSchoolId || this.data.userInfo?.schoolId;
    const effectiveName = this.data.upName ?? this.data.userInfo?.realname;
    const effectiveStuId = this.data.upSID ?? this.data.userInfo?.stuId;

    if (effectiveSchoolId == null || effectiveName == null || effectiveStuId == null) {
      errorCilcleToast(this, "请填写完整信息");
      return;
    }
    
    if (effectiveSchoolId === '' || effectiveName === '' || effectiveStuId === '') {
      errorCilcleToast(this, "请填写完整信息");
      return;
    }
    
    // 证件材料：可复用已提交材料，或重新上传
    const hasNewLocalImage = !!this.data.studentIdCardUrl;
    const existingAssetId = this.data.userInfo?.studentIdCardAssetId || null;
    if (!hasNewLocalImage && !existingAssetId) {
      errorCilcleToast(this, "请上传证件照片");
      return;
    }

    if (this.data.submitting) return;
    try {
      this.setData({ submitting: true });
      showLoading('提交中');
      
      // 有新图则上传，否则复用已绑定的媒体资源。
      let studentIdCardAssetId = this.data.uploadedStudentIdCardAssetId
        || existingAssetId
        || null;
      if (hasNewLocalImage && !this.data.uploadedStudentIdCardAssetId) {
        studentIdCardAssetId = await this.uploadStudentIdCard();
      }
      
      const authData = {
        schoolId: effectiveSchoolId,
        realname: effectiveName,
        stuId: effectiveStuId,
        studentIdCardAssetId
      };
      
      const submittedUser = { ...this.data.userInfo, ...authData,
        studentIdCard: this.data.studentIdCardUrl || this.data.userInfo?.studentIdCard };
      await userService.authenticate(authData);
      this.setData({ studentIdCardUrl: null, uploadedStudentIdCardAssetId: null,
        userInfo: { ...submittedUser, studentIdCardReview: AUTH_STATUS.PENDING,
          studentIdCardRejectReason: null } });
      this._refreshAuthViewState();
      feedback.showToast(this, { title: '提交成功，等待审核', theme: 'success' });
      await this.refreshStatus();
    } catch (error) {
      console.error('重新认证失败:', error);
      showError(this, error.message || '修改失败');
    } finally {
      this.setData({ submitting: false });
      hideLoading();
    }
  },

  async refreshStatus() {
    try {
      const userInfo = await userService.getUserInfo();
      this.setData({ userInfo });
      this._syncFormFromUserInfo();
      this._refreshAuthViewState();
    } catch (_) {
      feedback.showMessage(this, '状态刷新失败，请重试', {
        theme: 'error', action: '刷新', onAction: () => this.refreshStatus(),
      });
    }
  },

  _refreshAuthViewState() {
    const status = Number(this.data.userInfo?.studentIdCardReview ?? AUTH_STATUS.UNREVIEWED);
    this.setData({
      authStatusText: getStatusText(status),
      authStatusTheme: getStatusTheme(status),
      authStatusIcon: getStatusIcon(status),
      authStatusTitle: getStatusTitle(status),
      authStatusDesc: getStatusDesc(status, this.data.userInfo?.studentIdCardRejectReason),
      isRejected: status === AUTH_STATUS.REJECTED,
      canSubmit: status !== AUTH_STATUS.PENDING,
    });
  },

  _syncFormFromUserInfo() {
    const userInfo = this.data.userInfo;
    if (!userInfo) return;

    const nextData = {};
    if (!this.data.upSchoolId && userInfo.schoolId) nextData.upSchoolId = userInfo.schoolId;
    if (this.data.upName == null && userInfo.realname) nextData.upName = userInfo.realname;
    if (this.data.upSID == null && userInfo.stuId) nextData.upSID = userInfo.stuId;
    if (Object.keys(nextData).length > 0) this.setData(nextData);
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {

  },

  /**
   * 生命周期函数--监听页面初次渲染完成
   */
  onReady() {

  },

  /**
   * 生命周期函数--监听页面显示
   */
  async onShow() {
    feedback.loaded(this);
    try {
      // 等待 token 就绪
      await tokenManager.waitForToken();
      
      // 获取用户信息
      const userInfo = await userService.getUserInfo();
      userInfo.token = tokenManager.getToken();
      this.setData({ userInfo });
      this._syncFormFromUserInfo();
      this._refreshAuthViewState();
      
      // 获取学校信息并调整为选择器易用的形式
      const schoolsInfo = await this._getAllSchool();
      const schoolsName = _schlsAll2schlNameOnly(schoolsInfo);
      let { schools } = schoolsName;
      schools = _getOptions(schools);
      
      // 把处理完成的数据放到data里
      this.setData({ schools });

      // 如果已有学校信息，回显学校名称
      const schoolId = this.data.upSchoolId || userInfo.schoolId;
      if (schoolId) {
        const matched = schools.find(item => item.value == schoolId);
        if (matched) this.setData({ onPickValue: matched.label });
      }
      
      // 显示认证须知（重新认证也需要同意）
      if (this.data.canSubmit && !this.data.hasAgreedNotice) {
        this.setData({ showAuthNotice: true });
      }
    } catch (error) {
      console.error("页面加载失败:", error);
      feedback.loadError(this, '资料加载失败，请重试', () => this.onShow(), !!this.data.userInfo?.id);
    }
  },

  /**
   * 生命周期函数--监听页面隐藏
   */
  onHide() {

  },

  /**
   * 生命周期函数--监听页面卸载
   */
  onUnload() {

  },

  /**
   * 页面相关事件处理函数--监听用户下拉动作
   */
  onPullDownRefresh() {

  },

  /**
   * 页面上拉触底事件的处理函数
   */
  onReachBottom() {

  },

  /**
   * 用户点击右上角分享
   */
  onShareAppMessage() {

  }
})
