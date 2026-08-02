// 引入服务和工具
const userOrderService = require('../../../../services/userOrderService');
const takeOrderService = require('../../../../services/takeOrderService');
const userService = require('../../../../services/userService');
const mediaService = require('../../../../services/mediaService');
const tokenManager = require('../../../../utils/tokenManager');
const { showLoading, hideLoading, showError } = require('../../../../utils/transformers');
const { 
  checkCilcleToast, 
  errorCilcleToast, 
  showErrorToast, 
  showSuccessToast,
  _logErrInfo,
  _getExpectTimeDisplay,
  _parseStrDateTime
} = require('../../../../utils/commonJs');

const url = getApp().globalData.API_URL;

Page({
  data: {
    userInfo: null,
    // 步骤条
    title: ['待接单', '待取件', '派送中', '已送达', '已完成'],
    titleB: ['接单', '取件完成', '派送完成', '更改送达图片', '已确认'],
    // 订单内容
    id: null,
    orderInfo: {},
    takeInfo: {},
    pickUpAddress: [],
    reciveAddress: [],
    door: ['无门禁', '有门禁', '暂不清楚'],
    // 显示组件
    theme: ['primary', 'warning', 'warning', 'success', 'danger', 'success', 'danger'],
    status: ['待接单', '待取件', '派送中', '已完成', '已取消', '已确认', '错误状态'],
    cancelReason: '暂无信息',
    taker: {},
    time: 0,
    showConfirm: false,
    showWithImage: false,
    fileList: [],
    image: null,
    imageAssetId: null,
  },

  // 按钮根据状态跳转方法
  button(e) {
    const status = this.data.orderInfo.status;
    if (status == 0) {
      this.showDialog();
    } else if (status == 1) {
      this.statusTo2();
    } else {
      this.showDialogWithImage();
    }
  },

  showDialog(e) {
    this.setData({ showConfirm: true });
  },

  closeConfirm() {
    this.setData({ showConfirm: false });
    this.statusTo1();
  },

  closeDialog() {
    this.setData({ showConfirm: false });
    wx.showToast({
      title: '取消接单',
      icon: 'none',
      mask: 'true',
      duration: 2000
    });
  },

  // 待接单0->接单1（使用封装的 service）
  async statusTo1() {
    const userInfo = this.data.userInfo;
    
    // 用户未认证的提示
    if (userInfo.authentication == 0) {
      errorCilcleToast(this, "未校园认证");
      setTimeout(() => {
        wx.navigateTo({ url: '/pages/mine/userInfo/info' });
      }, 1500);
      return;
    }
    
    // 用户手机号未填的提示
    if (!userInfo.phone || userInfo.phone === '') {
      errorCilcleToast(this, "个人信息不完全");
      setTimeout(() => {
        wx.navigateTo({ url: '/pages/mine/userInfo/info' });
      }, 1500);
      return;
    }
    
    // 接单逻辑
    try {
      // 检查是否已被接单
      const existingTaker = await takeOrderService.getTakeOrderDetail(this.data.id);
      
      // 修复：正确判断是否已被接单（检查对象是否有id属性，而不是仅判断对象存在）
      if (existingTaker && existingTaker.id) {
        checkCilcleToast(this, "已被接单");
        setTimeout(() => {
          wx.switchTab({ url: '/pages/index/index' });
        }, 1500);
        return;
      }
      
      // 执行接单
      await takeOrderService.acceptOrderById(this.data.id);
      checkCilcleToast(this, "接单成功");
      
      // 发送订阅消息
      try {
        await takeOrderService.sendTakeOrderMessage(
          this.data.orderInfo.id, 
          userInfo.id
        );
        console.log("成功调用发送接单的订阅消息");
      } catch (err) {
        console.log("发送订阅消息失败:", err.message);
      }
      
      await this._loadOrderInfo();
    } catch (error) {
      console.error("接单错误:", error);
      errorCilcleToast(this, "接单错误");
      setTimeout(() => {
        wx.switchTab({ url: '/pages/index/index' });
      }, 1500);
    }
  },

  // 接单1->已取件2（使用封装的 service）
  async statusTo2() {
    try {
      showLoading('取件中');
      await takeOrderService.updateTakeOrderStatus({ 
        id: this.data.taker.id, 
        status: 1 
      });
      checkCilcleToast(this, "取件成功");
      
      // 发送订阅消息
      try {
        await takeOrderService.sendPickupMessage(this.data.orderInfo.id);
      } catch (err) {
        console.log("发送取件订阅消息失败:", err.message);
      }
      
      await this._loadOrderInfo();
    } catch (error) {
      console.error('取件失败:', error);
      showError('取件失败');
    } finally {
      hideLoading();
    }
  },

  // 图片上传提示
  showDialogWithImage(e) {
    this.setData({ showWithImage: true });
  },

  closeConfirmWithImage() {
    this.setData({ showWithImage: false });
    this.statusTo3();
  },

  closeWithImage() {
    if (this.data.imageAssetId) {
      mediaService.releaseTemporaryImage(this.data.imageAssetId).catch(() => {});
    }
    this.setData({
      showWithImage: false,
      imageAssetId: null,
      fileList: [],
    });
    wx.showToast({
      title: '取消上传',
      icon: 'none',
      mask: 'true',
      duration: 2000
    });
    this._loadOrderInfo();
  },

  // 图片上传
  handleAdd(e) {
    const { files } = e.detail;
    if (files.length) {
      this.onUpload(files[files.length - 1]);
    }
  },

  async onUpload(file) {
    const index = 0;
    this.setData({
      fileList: [{ ...file, status: 'loading' }],
    });

    try {
      const uploaded = await mediaService.uploadImage(
        file.url,
        'DELIVERY_PROOF',
        (progress) => {
          this.setData({ [`fileList[${index}].percent`]: progress });
        },
      );
      if (this.data.imageAssetId) {
        await mediaService.releaseTemporaryImage(this.data.imageAssetId).catch(() => {});
      }
      this.setData({
        [`fileList[${index}].status`]: 'done',
        [`fileList[${index}].url`]: uploaded.previewUrl,
        [`fileList[${index}].mediaId`]: uploaded.mediaId,
        image: uploaded.previewUrl,
        imageAssetId: uploaded.mediaId,
      });
    } catch (error) {
      this.setData({ [`fileList[${index}].status`]: 'failed' });
      errorCilcleToast(this, '图片上传失败');
    }
  },

  handleRemove(e) {
    const { index } = e.detail;
    const { fileList } = this.data;
    const removed = fileList[index];
    if (removed && removed.mediaId) {
      mediaService.releaseTemporaryImage(removed.mediaId).catch(() => {});
    }
    fileList.splice(index, 1);
    this.setData({ fileList, image: null, imageAssetId: null });
  },

  // 已取件2->已派送3（使用封装的 service）
  async statusTo3() {
    if (!this.data.imageAssetId) {
      errorCilcleToast(this, "未上传图片");
      this._loadOrderInfo();
      return;
    }
    
    try {
      showLoading('派送中');
      await takeOrderService.updateTakeOrderStatus({
        id: this.data.taker.id,
        status: 2,
        imageAssetId: this.data.imageAssetId
      });
      this.setData({ imageAssetId: null, fileList: [] });
      checkCilcleToast(this, "派送成功");
      
      // 发送订阅消息
      try {
        await takeOrderService.sendDeliveredMessage(
          this.data.orderInfo.id, 
          this.data.userInfo.id
        );
      } catch (err) {
        console.log("发送派送订阅消息失败:", err.message);
      }
      
      await this._loadOrderInfo();
    } catch (error) {
      console.error('派送失败:', error);
      showError('派送失败');
    } finally {
      hideLoading();
    }
  },

  // 取消接单（使用封装的 service）
  async cancelTake() {
    try {
      showLoading('取消中');
      await takeOrderService.updateTakeOrderStatus({ 
        id: this.data.taker.id, 
        status: 3 
      });
      wx.showToast({
        title: '接单已取消',
        icon: 'success'
      });
      await this._loadOrderInfo();
    } catch (error) {
      console.error('取消接单失败:', error);
      showError('取消失败');
    } finally {
      hideLoading();
    }
  },

  // 拉起微信确定收款（保留原逻辑，使用 tokenManager）
  async _requestMerchantTransfer() {
    try {
      if (getApp().globalData.MOCK_PAYMENT) {
        showLoading('模拟收款中');
        await takeOrderService.mockReceiveSuccess(this.data.id);
        hideLoading();
        wx.showToast({
          title: '模拟收款成功',
          icon: 'success'
        });
        await this._loadOrderInfo();
        return;
      }

      const result = await this._apiTransfer(this.data.id);
      const accountInfo = wx.getAccountInfoSync();
      wx.requestMerchantTransfer({
        mchId: result.mchId,
        appId: accountInfo.miniProgram.appId,
        package: result.packageInfo,
        success: (res) => {
          const newOrderInfo = this.data.orderInfo;
          newOrderInfo.status = '6';
          this.setData({ orderInfo: newOrderInfo });
          console.log(res);
        },
        fail: (err) => {
          console.log(err);
        }
      });
    } catch (err) {
      hideLoading();
      errorCilcleToast(this, err.message);
    }
    
    const pages = getCurrentPages();
    const currentPage = pages[pages.length - 1];
    currentPage.onLoad(currentPage.options);
  },

  // 调用微信收款的后端接口
  _apiTransfer(orderId) {
    return new Promise((resolve, reject) => {
      wx.request({
        url: `${url}/api/wx-transfer/transfer/${orderId}`,
        method: 'POST',
        header: {
          'token': tokenManager.getToken()
        },
        success: (res) => {
          console.log("res.data", res.data);
          resolve(res.data.data);
        },
        fail: (err) => {
          reject(new Error("发起转账失败"));
        }
      });
    });
  },

  // 集中统一获取、加载、更新订单相关的信息
  async _loadOrderInfo() {
    try {
      await tokenManager.waitForToken();
      await this._getOrderInfo();
      
      const status = this.data.orderInfo.status;
      if (status > 0 && status < 6 && status != 4) {
        await this._getTaker();
      }
      await this._getTakeImage();
    } catch (err) {
      showErrorToast(this, "加载失败");
      _logErrInfo("_loadOrderInfo", err.message);
    }
  },

  // 获取订单详情（使用封装的 service）
  async _getOrderInfo() {
    try {
      const orderInfo = await userOrderService.getMyOrderDetail(this.data.id);
      console.log("请求的orderInfo: ", orderInfo);
      
      // 检查订单数据是否有效
      if (!orderInfo || !orderInfo.id) {
        throw new Error('订单不存在或已被删除');
      }
      
      // 安全处理地址分割（去掉第一个空格前的内容）
      const addressParts1 = orderInfo.pickUpAddress ? orderInfo.pickUpAddress.split(' ').slice(1) : [];
      const addressParts2 = orderInfo.reciveAddress ? orderInfo.reciveAddress.split(" ").slice(1) : [];
      orderInfo.expectTime = _getExpectTimeDisplay(orderInfo.createTime, orderInfo.gap);
      
      this.setData({
        orderInfo,
        pickUpAddress: addressParts1,
        reciveAddress: addressParts2
      });
    } catch (error) {
      throw new Error(`获取订单失败：${error.message}`);
    }
  },

  // 获取送达图片（使用封装的 service）
  async _getTakeImage() {
    try {
      const image = await takeOrderService.getDeliveryImage(this.data.id);
      this.setData({ image });
    } catch (error) {
      console.warn('获取送达图片失败:', error);
    }
  },

  // 获取接单人信息（使用封装的 service）
  async _getTaker() {
    try {
      const taker = await takeOrderService.getTakeOrderDetail(this.data.id);
      // 只有当获取到有效的接单人信息时才设置数据
      if (taker && taker.id) {
        this.setData({ taker });
        this.time();
      }
    } catch (error) {
      console.warn('获取接单人信息失败:', error);
    }
  },

  // 倒计时计算 - 修复：应该显示预期送达时间和当前时间的差值
  time() {
    // 计算预期送达时间 = 创建时间 + gap（分钟）
    const createTime = _parseStrDateTime(this.data.orderInfo.createTime);
    const expectTime = new Date(createTime.getTime() + this.data.orderInfo.gap * 60000);
    
    // 计算倒计时 = 预期送达时间 - 当前时间
    const nowTime = new Date();
    const timeDifference = Math.floor(expectTime - nowTime);
    
    this.setData({ time: timeDifference });
  },

  // 获取用户数据（使用封装的 service）
  async getGlobalData() {
    try {
      await tokenManager.waitForToken();
      const userInfo = await userService.getUserInfo();
      userInfo.token = tokenManager.getToken();
      this.setData({ userInfo });
    } catch (err) {
      console.error('获取用户数据失败:', err);
    }
  },

  // 生命周期函数
  async onLoad(options) {
    console.log('接单详情页 - 接收到的参数:', options);
    console.log('接单详情页 - 订单ID:', options.id);
    this.setData({ id: options.id });
    try {
      await this.getGlobalData();
      console.log('getGlobalData执行成功');
      await this._loadOrderInfo();
    } catch (err) {
      console.error('页面加载失败', err);
    }
  },

  onShow() {
    this.getGlobalData();
  },

  // 下拉刷新
  onPullDownRefresh() {
    this._loadOrderInfo().then(() => {
      checkCilcleToast(this, '刷新成功');
      wx.stopPullDownRefresh();
    });
  },

  // 预览图片
  tapOnImageToPreview(res) {
    const imageUrl = res.target.dataset.src;
    const ifClickable = res.target.dataset.flag;
    
    if (ifClickable == 0) {
      return;
    }
    
    wx.previewImage({
      urls: [imageUrl],
      showmenu: true
    });
  },

  // 联系方式
  tapOnPhoneToDo(res) {
    const type = res.currentTarget.dataset.phone;
    const phoneObject = this.data[type];
    const number = phoneObject.phone;
    
    wx.showActionSheet({
      alertText: number + "可能是微信或电话，你可以",
      itemList: ["呼叫", "复制到剪切板"],
      success: (res) => {
        if (res.tapIndex == 0) {
          wx.makePhoneCall({ phoneNumber: number });
        } else if (res.tapIndex == 1) {
          wx.setClipboardData({ data: number });
        }
      }
    });
  }
});
