import Toast from 'tdesign-miniprogram/toast/index';

// 引入服务和工具
const userService = require('../../../services/userService');
const mediaService = require('../../../services/mediaService');
const tokenManager = require('../../../utils/tokenManager');
const { showLoading, hideLoading, showError } = require('../../../utils/transformers');
const {
  _getUserInfo,
  checkCilcleToast,
  errorCilcleToast,
  showErrorToast,
  showSuccessToast,
  compressImageSmart
} = require('../../../utils/commonJs');

// pages/mine/paycode/paycode.js
Page({

  /**
   * 页面的初始数据
   */
  data: {
    wxCodeSrc: null,
    ifWxCode: true,
    aliCodeSrc: null,
    ifAliCode: true,
  },

  // 保存收款码
  tgrdClkSaveQR() {
    let secureUrl = this.data.wxCodeSrc.replace(/^http:/, 'https:');
    // 下载网络路径的图片为临时文件
    wx.downloadFile({
      url: secureUrl,
      success: (res) => {
        // 将downloadFile回调的临时路径下载到本地
        wx.saveImageToPhotosAlbum({
          filePath: res.tempFilePath,
          success: () => {
            checkCilcleToast(this, "下载成功")
          },
          fail: () => {
            errorCilcleToast(this, "下载失败");
          }
        })
      },
      fail: () => {
        errorCilcleToast(this, "下载失败");
      }
    })
  },

  // 更换收款码
  async tgrdClkChangeQR(e) {
    try {
      // 打开相册时 await 等待选择图片完成
      const chooseResult = await this._chooseImage();
      let localPath = chooseResult.tempFiles[0].tempFilePath;
      
      // 压缩图片
      try {
        localPath = await compressImageSmart(localPath);
      } catch (compressErr) {
        console.error('图片压缩失败:', compressErr);
      }
      
      // 等待上传完成  
      const uploaded = await mediaService.uploadImage(localPath, 'PAYMENT_QR');
      let updateResult;
      try {
        updateResult = await this._updateCode(uploaded.mediaId);
      } catch (error) {
        await mediaService.releaseTemporaryImage(uploaded.mediaId).catch(() => {});
        throw error;
      }
      showSuccessToast(this, updateResult)
      this.setData({
        wxCodeSrc: localPath,
        ifWxCode: true
      })
    } catch (err) {
      if (err.type === "NOT_CHOOSE") {
        errorCilcleToast(this, err.message);
        return;
      }
      showErrorToast(this, err.message);
    }
  },

  // 收款小账本 跳转
  toLedger(e) {
    wx.navigateToMiniProgram({
      shortLink: '#小程序://收款小账本/bTfYWvu9amPhjWh'
    })
    success: (e) => {
      console.log(e);
    }
  },

  async _updateCode(weChatPaymentCodeAssetId) {
    try {
      await userService.updatePaymentCode({ weChatPaymentCodeAssetId });
      return "成功更换二维码";
    } catch (error) {
      console.error('更新支付码失败:', error);
      throw new Error("更新失败");
    }
  },

  // 选择图片，直接打开相册
  _chooseImage() {
    return new Promise((resolve, reject) => {
      wx.chooseMedia({
        count: 1,
        mediaType: ['image'],
        sourceType: ['album'],
        success: (res) => {
          resolve(res)
        },
        fail: () => {
          const err = new Error("未选择图片");
          err.type = "NOT_CHOOSE";
          reject(err);
        }
      })
    })
  },


  /**
   * 生命周期函数--监听页面加载
   */
  async onLoad(options) {
    try {
      // 调整当前页面的背景颜色
      wx.setBackgroundColor({
        backgroundColor: '#E6B840',
      });
      
      // 调整当前页面的导航栏颜色
      wx.setNavigationBarColor({
        frontColor: '#ffffff',
        backgroundColor: '#E6B840',
      });

      // 等待 token 就绪
      await tokenManager.waitForToken();
      
      // 获取用户的收款码
      const userInfo = await userService.getUserInfo();
      const aliCodeSrc = userInfo.alipayPaymentCode;
      const wxCodeSrc = userInfo.weChatPaymentCode;
      
      this.setData({
        aliCodeSrc,
        wxCodeSrc,
      });
      
      if (wxCodeSrc == null || wxCodeSrc === '') {
        this.setData({
          ifWxCode: false
        });
      }
    } catch (error) {
      console.error('页面加载失败:', error);
      showErrorToast(this, '加载失败');
    }
  },

  /**
   * 生命周期函数--监听页面初次渲染完成
   */
  onReady() {


  },

  /**
   * 生命周期函数--监听页面显示
   */
  onShow() {


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
