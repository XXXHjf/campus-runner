import Toast from 'tdesign-miniprogram/toast/index';

// 引入服务和工具
const userService = require('../../../services/userService');
const mediaService = require('../../../services/mediaService');
const tokenManager = require('../../../utils/tokenManager');
const { showLoading, hideLoading, showError } = require('../../../utils/transformers');
const {
  containsEmoji,
  _getUserInfo,
  checkCilcleToast,
  errorCilcleToast,
  showErrorToast,
  showSuccessToast
} = require('../../../utils/commonJs');

Page({
  data: {
    userInfo: null,
    defaultAvatarUrl: 'https://mmbiz.qpic.cn/mmbiz/icTdbqWNOwNRna42FI242Lcia07jQodd2FJGIYQfG0LAJGFxM4FbnQP6yfMxBgJ0F3YRqJCJ1aPAK2dQagdusBZg/0',
    phoneError: false,
    phoneNumber: null,

    newAvatar: null,
    newAvatarAssetId: null,
    newNickName: null,
  },
  // 头像
  onChooseAvatar(e) {
    const { avatarUrl } = e.detail;
    const { nickName } = this.data.userInfo;

    this.setData({
      newAvatar: avatarUrl,
      hasUserInfo: nickName && avatarUrl && avatarUrl !== this.data.defaultAvatarUrl,
    });
  },
  // 昵称
  tiptChangeUsername(e) {
    const nickName = e.detail.value.replace(/\s+/g, ''); // 移除所有空格
    console.log(nickName);
    this.setData({
      newNickName: nickName,
    });
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
    this.setData({
      phoneNumber: e.detail.value,
    })
  },
  // 保存
  async save() {
    // 验证输入
    if (!this.validateInput()) {
      return;
    }
    
    try {
      showLoading('保存中');
      
      // 如果用户更换了头像，需要先上传到服务器
      if (this.data.newAvatar) {
        const uploaded = await mediaService.uploadImage(this.data.newAvatar, 'AVATAR');
        this.setData({
          newAvatarAssetId: uploaded.mediaId,
        });
      }
      
      // 更新用户信息到后端
      await this.updateUser();
      this.setData({ newAvatarAssetId: null });
      
      hideLoading();
      checkCilcleToast(this, "保存成功");
      
      setTimeout(() => {
        wx.navigateBack();
      }, 1500);
    } catch (error) {
      if (this.data.newAvatarAssetId) {
        mediaService.releaseTemporaryImage(this.data.newAvatarAssetId).catch(() => {});
        this.setData({ newAvatarAssetId: null });
      }
      hideLoading();
      console.error('[保存失败]:', error);
      errorCilcleToast(this, error.message || "保存失败，请重试");
    }
  },
  
  // 验证用户输入
  validateInput() {
    const { newNickName, phoneNumber, phoneError, userInfo } = this.data;
    
    // 获取最终的昵称和手机号（优先使用新输入的，否则使用原有的）
    const finalNickName = newNickName || userInfo.username;
    const finalPhone = phoneNumber || userInfo.phone;
    
    // 检查昵称（必填）
    if (!finalNickName || finalNickName.trim() === '') {
      errorCilcleToast(this, "请输入昵称");
      return false;
    }
    
    if (containsEmoji(finalNickName)) {
      errorCilcleToast(this, "昵称中不能有表情");
      return false;
    }
    
    // 检查手机号（必填）
    if (!finalPhone || finalPhone.trim() === '') {
      errorCilcleToast(this, "请输入手机号");
      return false;
    }
    
    if (phoneError === true) {
      errorCilcleToast(this, "手机号格式不正确");
      return false;
    }
    
    return true;
  },
  
  // 更新用户数据（使用封装的 service）
  async updateUser() {
    if (this.data.phoneError == true) {
      this.setData({ phoneNumber: null });
    }

    // 准备更新数据
    const updatedUserInfo = {
      username: this.data.newNickName || this.data.userInfo.username,
      headImgAssetId: this.data.newAvatarAssetId,
      phone: this.data.phoneNumber || this.data.userInfo.phone,
    };

    // 更新到数据库
    await userService.updateUserInfo(updatedUserInfo);
    console.log('[更新用户信息] 成功');

    // 更新本地数据
    this.setData({
      userInfo: {
        ...this.data.userInfo,
        ...updatedUserInfo
      }
    });

    // 刷新用户数据
    await this.getGlobalData();
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
      console.log('获取用户数据成功:', userInfo);
      return userInfo;
    } catch (error) {
      console.error('获取用户数据失败:', error);
      throw error;
    }
  },
  // 界面跳转
  toIdentify() {
    wx.navigateTo({
      url: '/pages/mine/identify/identify',
    })
  },
  // 生命周期函数--监听页面显示
  async onShow() {
    try {
      // 等待 token 就绪
      await tokenManager.waitForToken();
      
      // 获取用户数据
      await this.getGlobalData();
      console.log('页面显示时获取用户数据成功');
    } catch (error) {
      console.error('页面显示时获取用户数据失败:', error);
      showError('加载失败');
    }
  },
})
