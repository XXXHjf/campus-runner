import Toast from 'tdesign-miniprogram/toast/index';

// 引入服务和工具
const userService = require('../../../services/userService');
const tokenManager = require('../../../utils/tokenManager');
const { showLoading, hideLoading, showError } = require('../../../utils/transformers');
const {
  containsEmoji,
  errorCilcleToast,
  showSuccessToast,
  compressImageSmart
} = require('../../../utils/commonJs');

const url = getApp().globalData.API_URL;
const defaultAvatarUrl = 'https://mmbiz.qpic.cn/mmbiz/icTdbqWNOwNRna42FI242Lcia07jQodd2FJGIYQfG0LAJGFxM4FbnQP6yfMxBgJ0F3YRqJCJ1aPAK2dQagdusBZg/0';

Page({
  data: {
    text: 'Copyright © 2024 campus-runner.All Rights Reserved.',
    userInfo: {
      avatarUrl: defaultAvatarUrl,
      nickName: '',
    },
    token: null,
    nickName: null,
    phone: '', // 用户手机号
    radio: false,
    hasUserInfo: false,
    canIUseGetUserProfile: wx.canIUse('getUserProfile'),
    canIUseNicknameComp: wx.canIUse('input.type.nickname'),
  },
  
  // 生命周期 - 初始化 token
  onLoad() {
    this.setData({
      token: tokenManager.getToken()
    });
  },
  // 确定注册
  async confirmRegister() {
    // 验证输入
    if (!this.validateInput()) {
      return;
    }
    
    try {
      showLoading('注册中');
      
      // 如果用户上传了自定义头像，需要先上传到服务器
      if (this.data.userInfo.avatarUrl !== defaultAvatarUrl) {
        const uploadedAvatarUrl = await this.uploadAvatar(this.data.userInfo.avatarUrl);
        this.setData({
          "userInfo.avatarUrl": uploadedAvatarUrl,
        });
      }
      
      // 更新用户信息到后端
      await this.updateUser();
      
      hideLoading();
      showSuccessToast(this, "注册成功");
      
      setTimeout(() => {
        wx.navigateBack();
      }, 1500);
    } catch (error) {
      hideLoading();
      console.error('[注册失败]:', error);
      errorCilcleToast(this, error.message || "注册失败，请重试");
    }
  },
  
  // 验证用户输入
  validateInput() {
    const { nickName } = this.data.userInfo;
    const { radio, phone } = this.data;
    
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
    const phoneReg = /^1[3-9]\d{9}$/;
    if (!phoneReg.test(phone)) {
      errorCilcleToast(this, "请输入正确的手机号");
      return false;
    }
    
    if (!radio.checked) {
      errorCilcleToast(this, "请先阅读并同意用户协议");
      return false;
    }
    
    return true;
  },
  
  // 上传头像到服务器
  uploadAvatar(filePath) {
    return new Promise((resolve, reject) => {
      wx.uploadFile({
        url: `${url}/api/upload`,
        filePath: filePath,
        name: 'img',
        header: {
          'token': tokenManager.getToken(),
          'Content-Type': 'multipart/form-data'
        },
        formData: {
          'dirName': 'avatar',
        },
        success: (res) => {
          try {
            const data = JSON.parse(res.data);
            if (data.code === 1) {
              console.log('[上传头像] 成功:', data.data);
              resolve(data.data);
            } else {
              console.error('[上传头像] 失败:', data);
              reject(new Error(data.msg || '上传头像失败'));
            }
          } catch (e) {
            console.error('[上传头像] 解析响应失败:', e);
            reject(new Error('上传头像失败'));
          }
        },
        fail: (err) => {
          console.error('[上传头像] 请求失败:', err);
          reject(new Error('上传头像失败'));
        }
      });
    });
  },
  // 更新用户数据（使用封装的 service）
  async updateUser() {
    let nickName = this.data.userInfo.nickName;
    if (!nickName || nickName === '') {
      nickName = '微信用户';
      this.setData({
        "userInfo.nickName": nickName,
      });
    }

    const userData = {
      username: nickName,
      headImg: this.data.userInfo.avatarUrl,
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
  async onChooseAvatar(e) {
    const { avatarUrl } = e.detail;
    const { nickName } = this.data.userInfo;
    
    try {
      // 显示加载提示
      wx.showLoading({ 
        title: '处理图片中...', 
        mask: true 
      });
      
      // 智能压缩图片
      const compressedPath = await compressImageSmart(avatarUrl);
      
      wx.hideLoading();
      
      this.setData({
        "userInfo.avatarUrl": compressedPath,
        hasUserInfo: nickName && compressedPath && compressedPath !== defaultAvatarUrl,
      });
      
      console.log('[头像选择] 图片处理成功');
    } catch (error) {
      wx.hideLoading();
      console.error('[头像选择] 图片处理失败:', error);
      errorCilcleToast(this, '图片处理失败，请重试');
    }
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
  
  getUserProfile(e) {
    // 推荐使用wx.getUserProfile获取用户信息，开发者每次通过该接口获取用户个人信息均需用户确认，开发者妥善保管用户快速填写的头像昵称，避免重复弹窗
    wx.getUserProfile({
      desc: '展示用户信息', // 声明获取用户个人信息后的用途，后续会展示在弹窗中，请谨慎填写
      success: (res) => {
        console.log(res)
        this.setData({
          userInfo: res.userInfo,
          hasUserInfo: true
        })
      }
    })
  },
})