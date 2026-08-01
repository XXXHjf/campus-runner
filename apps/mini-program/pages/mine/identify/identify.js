import Toast from 'tdesign-miniprogram/toast/index';

// 引入服务和工具
const userService = require('../../../services/userService');
const mediaService = require('../../../services/mediaService');
const tokenManager = require('../../../utils/tokenManager');
const { showLoading, hideLoading, showError } = require('../../../utils/transformers');
const {
  showSuccessToast,
  compressImageSmart
} = require('../../../utils/commonJs');
const {
  AUTH_STATUS,
  isApproved,
  getStatusText,
  getStatusTheme,
  getStatusIcon,
  getStatusTitle,
  getStatusDesc
} = require('../../../utils/authStatus');

const url = getApp().globalData.API_URL;

function normalizeRemoteImageUrl(rawUrl) {
  if (!rawUrl) return '';
  const value = rawUrl.toString().trim();
  if (!value) return '';
  if (value.startsWith('wxfile://')) return value;
  if (/^https?:\/\//i.test(value)) return value;
  if (value.startsWith('//')) return `https:${value}`;
  if (value.startsWith('/')) return `${url}${value}`;
  return `${url}/${value}`;
}

// 选择器方法
const getOptions = (obj, filter) => {
  const res = Object.keys(obj).map((key) => ({
    value: key,
    label: obj[key]
  }));
  if (filter) {
    return res.filter(filter);
  }
  return res;
};
const match = (v1, v2, size) => v1.toString().slice(0, size) === v2.toString().slice(0, size);

Page({
  data: {
    userInfo: null,
    defaultAvatarUrl: 'https://mmbiz.qpic.cn/mmbiz/icTdbqWNOwNRna42FI242Lcia07jQodd2FJGIYQfG0LAJGFxM4FbnQP6yfMxBgJ0F3YRqJCJ1aPAK2dQagdusBZg/0',

    phoneError: false,
    phoneNumber: null,

    upName: null,
    upSID: null,
    // 联级/选择器
    addressList: [], //用于存储地址allList
    visible: false, //联级选择判定
    value: [], //联级选择器值
    upNumberid: null, //联级
    areaVisible: false, //选择器判定
    areaValue: [], //选择器取值
    upSchool: null,
    upSchoolNumberId: null, //选择器
    text: '',
    schools: [],

    dialogVisable: false,
    
    // 校园认证相关
    studentIdCardUrl: null, // 学生证照片本地路径
    uploadedStudentIdCardAssetId: null,
    refreshTime: Date.now(), // 用于刷新图片缓存的时间戳
    studentIdCardWithTimestamp: '', // 带时间戳的完整图片URL
    
    // 认证须知弹窗
    showAuthNotice: false, // 是否显示认证须知弹窗
    hasAgreedNotice: false, // 是否已同意认证须知

    // 认证状态展示（派生数据）
    authStatusText: '',
    authStatusTheme: 'default',
    authStatusIcon: 'info-circle',
    authStatusTitle: '',
    authStatusDesc: '',
    isFormReadOnly: false,
    showSubmitButton: false,
    submitButtonText: '提交认证',
    showChangeIdentify: false,
  },

  // 点击更改认证，修改对话框状态
  tapChangeIdnetify() {
    this.setData({ dialogVisable: true })
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
  closeDialog() {
    this.setData({ dialogVisable: false })
  },
  goToChange() {
    wx.navigateTo({
      url: '/pages/mine/reIdentify/reIdentify',
    })
    this.setData({ dialogVisable: false })
  },
  // 号码验证
  onPhoneInput(e) {
    const {
      phoneError
    } = this.data;
    const isPhoneNumber = /^[1][3,4,5,7,8,9][0-9]{9}$/.test(e.detail.value);
    if (phoneError === isPhoneNumber) {
      this.setData({
        phoneError: !isPhoneNumber,
      });
    }
  },
  // 获取学校列表（使用封装的 service）
  async getAllAddress() {
    try {
      const schools = await userService.getSchools();
      const addressList = this.convertToListNew(schools);
      this.setData({ addressList });
      
      // 认证成功返回校名
      if (this.data.userInfo?.schoolId) {
        this.setData({
          upSchool: this.getSchoolNameByID(this.data.userInfo.schoolId),
        });
      }
    } catch (error) {
      console.error('获取学校列表失败:', error);
      showError('加载学校列表失败');
      throw error;
    }
  },
  // 获取学校名称的函数
  getSchoolNameByID(id) {
    const schools = this.data.addressList?.schools;
    if (!schools) return null;

    // eslint-disable-next-line no-prototype-builtins
    if (schools.hasOwnProperty(id)) {
      return schools[id];
    }
    return null;
  },
  // 转换AddressList格式-选择器
  convertToListNew(addressList) {
    let List = {
      schools: {},
    };
    // 遍历addressList数组
    for (const item of addressList) {
      // 确保deleted属性为0，即未删除的学校
      if (item.deleted === 0) {
        // 使用学校的ID作为键，学校名作为值
        List.schools[item.id] = item.schoolName;
      }
    }
    return List;
  },
  // 修改当前地址-选择器
  async changeAddressNew() {
    try {
      if (this.data.addressList) {
        this.setData({
          areaVisible: true,
          areaValue: [], // 重置选择器的值
        });
        
        await this.getAllAddress();
        const { schools } = this.data.addressList;
        this.setData({
          schools: getOptions(schools),
        });
      } else {
        console.log('Address list is undefined or null.');
        throw new Error('Address list is undefined or null.');
      }
    } catch (error) {
      console.error('修改地址失败:', error);
      throw error;
    }
  },
  onTapSelectSchool() {
    const authStatus = Number(this.data.userInfo?.authentication);
    const reviewStatus = Number(this.data.userInfo?.studentIdCardReview ?? AUTH_STATUS.UNREVIEWED);
    if (this.data.isFormReadOnly) {
      if (reviewStatus === AUTH_STATUS.PENDING) {
        wx.showToast({ title: '审核中，暂不可修改', icon: 'none' });
      } else if (authStatus === 1) {
        wx.showToast({ title: '已认证，如需修改请点更改认证', icon: 'none' });
      } else if (reviewStatus === AUTH_STATUS.APPROVED) {
        wx.showToast({ title: '材料已通过审核，暂不可修改', icon: 'none' });
      }
      return;
    }
    this.changeAddressNew();
  },
  onColumnChange(e) {
    console.log('pick:', e.detail);
  },
  onPickerChange(e) {
    const {
      value,
      label
    } = e.detail;
    console.log('picker confirm:', e.detail);
    this.setData({
      areaVisible: false,
      areaValue: value,
      areaText: label.join(' '),
      upSchool: e.detail.label[0],
      upSchoolNumberId: e.detail.value[0],
    });
  },
  onPickerCancel(e) {
    console.log('picker cancel', e.detail);
    this.setData({
      areaVisible: false,
    });
  },
  // 姓名
  handleName(e) {
    console.log('name值： ' + e.detail.value)
    this.setData({
      upName: e.detail.value.replace(/\s+/g, '')
    })
  },
  // 学号
  handleStudentId(e) {
    console.log('sid值： ' + e.detail.value)
    this.setData({
      upSID: e.detail.value.replace(/\s+/g, '')
    })
  },
  // 选择学生证照片
  async chooseStudentIdCard() {
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
      
      let tempFilePath = res.tempFiles[0].tempFilePath;
      
      // 使用智能压缩（保留质量优先，确保不超过2MB）
      try {
        tempFilePath = await compressImageSmart(tempFilePath);
      } catch (compressErr) {
        console.error('图片压缩失败:', compressErr);
        wx.showToast({
          title: '图片压缩失败，请重试',
          icon: 'none',
          duration: 2000
        });
        return;
      }
      
      this.setData({
        studentIdCardUrl: tempFilePath
      });
      console.log('选择学生证照片成功');
    } catch (err) {
      console.error('选择图片失败:', err);
      wx.showToast({
        title: '选择图片失败',
        icon: 'error',
        duration: 2000
      });
    }
  },
  // 删除学生证照片
  deleteStudentIdCard() {
    if (this.data.uploadedStudentIdCardAssetId) {
      mediaService.releaseTemporaryImage(this.data.uploadedStudentIdCardAssetId).catch(() => {});
    }
    this.setData({
      studentIdCardUrl: null,
      uploadedStudentIdCardAssetId: null
    });
  },
  // 预览学生证照片（已认证状态）
  previewStudentIdCard() {
    if (this.data.userInfo.studentIdCard) {
      // 使用不带时间戳的原始 URL 进行预览
      const imageUrl = normalizeRemoteImageUrl(this.data.userInfo.studentIdCard).split('?')[0];
      wx.previewImage({
        current: imageUrl,
        urls: [imageUrl]
      });
    }
  },
  // 图片加载成功
  onImageLoad(e) {
    console.log('[identify] 证件照片加载成功:', this.data.userInfo.studentIdCard);
  },
  // 图片加载失败
  onImageError(e) {
    console.error('[identify] 证件照片加载失败:', e.detail);
    console.error('[identify] 图片URL:', this.data.userInfo.studentIdCard);
    wx.showToast({
      title: '证件照片加载失败',
      icon: 'none',
      duration: 2000
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

  _syncFormFromUserInfo() {
    const userInfo = this.data.userInfo;
    if (!userInfo) return;

    const nextData = {};

    if (!this.data.upName && userInfo.realname) nextData.upName = userInfo.realname;
    if (!this.data.upSID && userInfo.stuId) nextData.upSID = userInfo.stuId;
    if (!this.data.upSchoolNumberId && userInfo.schoolId) nextData.upSchoolNumberId = userInfo.schoolId;
    // 学校名称依赖 schools 列表加载完成（getAllAddress 之后）
    if (!this.data.upSchool && userInfo.schoolId && this.data.addressList?.schools) {
      nextData.upSchool = this.getSchoolNameByID(userInfo.schoolId);
    }

    if (Object.keys(nextData).length > 0) {
      this.setData(nextData);
    }
  },

  _refreshAuthViewState() {
    const reviewStatus = Number(this.data.userInfo?.studentIdCardReview ?? AUTH_STATUS.UNREVIEWED);
    const status = reviewStatus;
    const approved = isApproved(status);
    const pending = status === AUTH_STATUS.PENDING;
    const rejected = status === AUTH_STATUS.REJECTED;
    const authStatus = Number(this.data.userInfo?.authentication);

    // 审核不通过时，需要允许用户修改“信息 + 材料”重新提交（即使 authentication==1）
    const isFormReadOnly = pending || approved || (authStatus === 1 && !rejected);
    const showSubmitButton = status === AUTH_STATUS.UNREVIEWED || rejected;
    const submitButtonText = rejected ? '重新审核' : '提交认证';

    this.setData({
      authStatusText: getStatusText(status),
      authStatusTheme: getStatusTheme(status),
      authStatusIcon: getStatusIcon(status),
      authStatusTitle: getStatusTitle(status),
      authStatusDesc: getStatusDesc(status),
      isFormReadOnly,
      showSubmitButton,
      submitButtonText,
      showChangeIdentify: authStatus === 1 && !showSubmitButton,
    });
  },

  async refreshStatus() {
    try {
      showLoading('刷新中');
      await this.getGlobalData();
      showSuccessToast(this, '已刷新');
    } catch (error) {
      console.error('刷新认证状态失败:', error);
      showError('刷新失败');
    } finally {
      hideLoading();
    }
  },
  // 确定认证（使用封装的 service）
  async identify() {
    const reviewStatus = Number(this.data.userInfo?.studentIdCardReview ?? AUTH_STATUS.UNREVIEWED);

    // 审核中/已通过时，不允许重复提交
    if (reviewStatus === AUTH_STATUS.PENDING) {
      wx.showToast({
        title: '审核中，请耐心等待',
        icon: 'none',
        duration: 2000,
      });
      return;
    }
    if (reviewStatus === AUTH_STATUS.APPROVED) {
      wx.showToast({
        title: '材料已通过审核',
        icon: 'none',
        duration: 2000,
      });
      return;
    }

    // 审核不通过：二次确认，避免误触直接提交
    if (reviewStatus === AUTH_STATUS.REJECTED) {
      const shouldContinue = await new Promise((resolve) => {
        wx.showModal({
          title: '重新提交审核',
          content: '将重新提交认证信息与材料进行审核，是否继续？',
          confirmText: '重新审核',
          cancelText: '取消',
          success: (res) => resolve(!!res.confirm),
          fail: () => resolve(false),
        });
      });

      if (!shouldContinue) return;
    }

    // 未同意须知则先弹窗
    if (!this.data.hasAgreedNotice) {
      this.setData({ showAuthNotice: true });
      return;
    }

    // 验证基本信息
    const effectiveSchoolId = this.data.upSchoolNumberId || this.data.userInfo?.schoolId;
    const effectiveName = this.data.upName || this.data.userInfo?.realname;
    const effectiveStuId = this.data.upSID || this.data.userInfo?.stuId;
    if (effectiveSchoolId == null || effectiveName == null || effectiveStuId == null) {
      wx.showToast({
        title: '未填写完全',
        icon: "error",
        duration: 2000,
      });
      return;
    } 
    
    if (effectiveSchoolId === '' || effectiveName === '' || effectiveStuId === '') {
      wx.showToast({
        title: '填写内容为空',
        icon: "error",
        duration: 2000,
      });
      return;
    }
    
    // 验证是否有证件材料：首次提交必须上传；驳回可沿用原证件或重新上传
    const hasNewLocalImage = !!this.data.studentIdCardUrl;
    const existingRemoteImage = this.data.userInfo?.studentIdCard || null;
    if (!hasNewLocalImage && !existingRemoteImage) {
      wx.showToast({
        title: '请上传证件照片',
        icon: "error",
        duration: 2000,
      });
      return;
    }

    try {
      showLoading('提交中');
      
      // 先处理证件材料：有新图则上传，否则复用已提交的URL
      let studentIdCardAssetId = this.data.uploadedStudentIdCardAssetId
        || this.data.userInfo?.studentIdCardAssetId
        || null;
      if (hasNewLocalImage && !this.data.uploadedStudentIdCardAssetId) {
        studentIdCardAssetId = await this.uploadStudentIdCard();
      }
      
      // 准备认证数据，调用 /api/user 接口进行认证
      const authData = {
        schoolId: effectiveSchoolId,
        realname: effectiveName,
        stuId: effectiveStuId,
        studentIdCard: studentIdCardAssetId ? null : existingRemoteImage,
        studentIdCardAssetId
      };
      
      await userService.authenticate(authData);
      console.log('认证信息已提交');
      showSuccessToast(this, "提交成功，等待审核");

      // 清理本地选择的图片（后续以服务端数据为准）
      this.setData({
        studentIdCardUrl: null,
        uploadedStudentIdCardAssetId: null
      });

      // 刷新缓存信息
      await this.getGlobalData();
    } catch (error) {
      console.error('认证失败:', error);
      showError(error.message || '认证失败');
      throw error;
    } finally {
      hideLoading();
    }
  },
  // 获取用户数据（使用封装的 service）
  async getGlobalData() {
    try {
      const userInfo = await userService.getUserInfo();
      const app = getApp();
      const userInfoWithToken = {
        ...userInfo,
        token: app.globalData.userInfo?.token || tokenManager.getToken()
      };
      
      this.setData({ userInfo: userInfoWithToken });
      
      // 更新带时间戳的图片URL
      this.updateStudentIdCardUrl();
      // 同步表单默认值与状态展示
      this._syncFormFromUserInfo();
      this._refreshAuthViewState();
      
      console.log('获取用户数据成功:', userInfo);
      return userInfo;
    } catch (error) {
      console.error('获取用户数据失败:', error);
      throw error;
    }
  },
  // 更新学生证图片URL（添加时间戳避免缓存）
  updateStudentIdCardUrl() {
    if (this.data.userInfo?.studentIdCard) {
      const baseUrl = normalizeRemoteImageUrl(this.data.userInfo.studentIdCard);
      // 检查URL中是否已经有查询参数
      const separator = baseUrl.includes('?') ? '&' : '?';
      const urlWithTimestamp = `${baseUrl}${separator}t=${this.data.refreshTime}`;
      
      this.setData({
        studentIdCardWithTimestamp: urlWithTimestamp
      });
      
      console.log('[identify] 更新图片URL:', urlWithTimestamp);
    } else {
      this.setData({ studentIdCardWithTimestamp: '' });
    }
  },
  // 生命周期函数--监听页面显示
  async onShow() {
    try {
      // 等待 token 就绪
      await tokenManager.waitForToken();
      
      // 更新时间戳，强制刷新图片（避免缓存）
      const newRefreshTime = Date.now();
      this.setData({
        refreshTime: newRefreshTime
      });
      console.log('[identify] 更新刷新时间戳:', newRefreshTime);
      
      // 强制刷新用户数据（避免从 reIdentify 返回后显示旧数据）
      console.log('[identify] 页面显示，刷新用户数据...');
      await this.getGlobalData();
      console.log('[identify] 用户数据刷新成功');
      console.log('[identify] authentication:', this.data.userInfo?.authentication);
      console.log('[identify] studentIdCard:', this.data.userInfo?.studentIdCard);
      console.log('[identify] 完整图片URL:', this.data.studentIdCardWithTimestamp);
      
      // 获取学校列表
      await this.getAllAddress();
      
      // 如果是未认证/驳回状态，显示认证须知
      const reviewStatus = Number(this.data.userInfo?.studentIdCardReview ?? AUTH_STATUS.UNREVIEWED);
      if ((reviewStatus === AUTH_STATUS.UNREVIEWED || reviewStatus === AUTH_STATUS.REJECTED) && !this.data.hasAgreedNotice) {
        this.setData({ showAuthNotice: true });
      }
    } catch (error) {
      console.error('页面加载失败:', error);
      showError('加载失败');
    }
  },
})
