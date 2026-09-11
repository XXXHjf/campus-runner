// 引入服务和工具
const userService = require('../../../services/userService');
const mediaService = require('../../../services/mediaService');
const tokenManager = require('../../../utils/tokenManager');
const { showLoading, hideLoading } = require('../../../utils/transformers');
const {
  PHONE_PATTERN,
  CAMPUS_AUTH_PAGE,
  getMissingProfileFields,
  hasSelectedAvatarFile,
  isProfileComplete,
} = require('../../../utils/profileStatus');
const {
  containsEmoji,
  errorCilcleToast,
  showSuccessToast
} = require('../../../utils/commonJs');

const defaultAvatarUrl = 'https://mmbiz.qpic.cn/mmbiz/icTdbqWNOwNRna42FI242Lcia07jQodd2FJGIYQfG0LAJGFxM4FbnQP6yfMxBgJ0F3YRqJCJ1aPAK2dQagdusBZg/0';

Page({
  data: {
    userInfo: {
      avatarUrl: defaultAvatarUrl,
      nickName: '',
    },
    token: null,
    nickName: null,
    phone: '', // 用户手机号
    selectedAvatarFilePath: null,
    headImgAssetId: null,
    isSubmitting: false,
    radio: false,
    hasUserInfo: false,
  },
  
  // 生命周期 - 初始化 token
  async onLoad() {
    try {
      await tokenManager.waitForToken();
      const token = tokenManager.getToken();
      const currentUser = await userService.getUserInfo();
      this.setData({
        token,
        userInfo: {
          avatarUrl: currentUser.headImg || defaultAvatarUrl,
          nickName: currentUser.username === '微信用户' ? '' : (currentUser.username || ''),
        },
        phone: currentUser.phone || '',
      });
    } catch (error) {
      console.error('[注册资料加载失败]:', error);
      errorCilcleToast(this, '资料加载失败，请重试');
    }
  },
  // 确定注册
  async confirmRegister() {
    if (this.data.isSubmitting) {
      return;
    }

    // 验证输入
    if (!this.validateInput()) {
      return;
    }

    this.setData({ isSubmitting: true });
    try {
      showLoading('注册中');

      // chooseAvatar 返回的是本地临时文件，部分环境下也可能以 http:// 开头。
      // 是否需要上传只能依据“本次是否选择了头像”，不能用 URL 前缀推断。
      if (hasSelectedAvatarFile(this.data.selectedAvatarFilePath)) {
        const uploaded = await mediaService.uploadImage(
          this.data.selectedAvatarFilePath,
          'AVATAR',
        );
        this.setData({
          headImgAssetId: uploaded.mediaId,
        });
      }
      
      // 更新用户信息到后端
      await this.updateUser();

      const currentUser = await userService.getUserInfo();
      if (!isProfileComplete(currentUser)) {
        const missingFields = getMissingProfileFields(currentUser);
        if (missingFields.length === 1 && missingFields[0] === 'avatar') {
          throw new Error('头像保存失败，请重新选择后再试');
        }
        throw new Error('资料保存未完成，请检查后重试');
      }
      this.setData({
        selectedAvatarFilePath: null,
        headImgAssetId: null,
      });
      getApp().onUserInfoUpdated({
        ...currentUser,
        token: this.data.token,
      });
      
      showSuccessToast(this, "注册成功");
      
      setTimeout(() => {
        wx.redirectTo({ url: CAMPUS_AUTH_PAGE });
      }, 800);
    } catch (error) {
      if (this.data.headImgAssetId) {
        mediaService.releaseTemporaryImage(this.data.headImgAssetId).catch(() => {});
        this.setData({ headImgAssetId: null });
      }
      console.error('[注册失败]:', error);
      errorCilcleToast(this, error.message || "注册失败，请重试");
    } finally {
      hideLoading();
      this.setData({ isSubmitting: false });
    }
  },
  
  // 验证用户输入
  validateInput() {
    const { nickName } = this.data.userInfo;
    const { radio, phone } = this.data;

    if (!this.data.userInfo.avatarUrl || this.data.userInfo.avatarUrl === defaultAvatarUrl) {
      errorCilcleToast(this, "请选择头像");
      return false;
    }
    
    if (!nickName || nickName === '') {
      errorCilcleToast(this, "请输入昵称");
      return false;
    }
    
    if (containsEmoji(nickName)) {
      errorCilcleToast(this, "昵称中不能有表情");
      return false;
    }
    
    if (nickName.length > 50) {
      errorCilcleToast(this, "昵称过长，请控制在50字以内");
      return false;
    }
    
    if (!phone || phone === '') {
      errorCilcleToast(this, "请输入手机号");
      return false;
    }
    
    // 验证手机号格式
    if (!PHONE_PATTERN.test(phone)) {
      errorCilcleToast(this, "请输入正确的手机号");
      return false;
    }
    
    const agreementAccepted = radio === true || radio?.checked === true;
    if (!agreementAccepted) {
      errorCilcleToast(this, "请先阅读并同意用户协议");
      return false;
    }
    
    return true;
  },
  
  // 更新用户数据（使用封装的 service）
  async updateUser() {
    const userData = {
      username: this.data.userInfo.nickName,
      headImgAssetId: this.data.headImgAssetId,
      phone: this.data.phone, // 添加手机号
    };

    await userService.updateUserInfo(userData);
    console.log('[更新用户信息] 成功');
  },
  // 用户协议勾选
  onChange(event) {
    // console.log('radio', event.detail);
    this.setData({
      radio: event.detail
    })
  },
  // 获取用户协议
  openPrivacyContract() {
    wx.openPrivacyContract({
      success: res => {
        console.log('openPrivacyContract success')
      },
      fail: res => {
        console.error('openPrivacyContract fail', res)
      }
    })
  },
  // 头像与昵称获取
  onChooseAvatar(e) {
    const { avatarUrl } = e.detail;
    const { nickName } = this.data.userInfo;

    this.setData({
      "userInfo.avatarUrl": avatarUrl,
      selectedAvatarFilePath: avatarUrl,
      hasUserInfo: nickName && avatarUrl && avatarUrl !== defaultAvatarUrl,
    });
  },
  // input表单的input事件--昵称修改
  iptInputNickName(e) {
    const nickName = e.detail.value.replace(/\s+/g, '');
    console.log(nickName);
    this.setData({
      "userInfo.nickName": nickName,
    });
  },
  // 手机号输入
  iptInputPhone(e) {
    const phone = e.detail.value.replace(/\s+/g, '');
    console.log('[手机号输入]:', phone);
    this.setData({
      phone: phone,
    });
  },
})
