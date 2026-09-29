const { shareOrder, title: shareTitle, imageUrl: shareImageUrl } = require('../../../../utils/orderShare');
const feedback = require('../../../../utils/feedback');
const detailRefresh = require('../../../../utils/detailRefresh');
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
  _getExpectedDeliveryDate,
  _parseStrDateTime
} = require('../../../../utils/commonJs');

const url = getApp().globalData.API_URL;

Page({
  retryFeedbackLoad() { return this._feedbackRetry?.(); },
  data: {
    userInfo: null,
    // 步骤条
    title: ['待接单', '待取件', '派送中', '已送达', '已完成'],
    titleB: ['接单', '取件完成', '派送完成', '更改送达图片', '已确认'],
    // 订单内容
    id: null,
    orderInfo: {},
    purchaseStatusText: '',
    orderStatusText: '',
    progressStep: 0,
    isMyTaken: false,
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
    proofPurpose: 'DELIVERY_PROOF',
    fileList: [],
    image: null,
    imageAssetId: null,
    proofUploading: false,
    proofSubmitting: false,
    transferBusy: false,
  },

  // 按钮根据状态跳转方法
  button(e) {
    const status = this.data.orderInfo.status;
    if (status == 0) {
      this.showDialog();
    } else if (status == 1) {
      if (this.data.orderInfo.businessType === 'PURCHASE') {
        this.showDialogWithImage('PURCHASE_PROOF');
      } else {
        this.statusTo2();
      }
    } else {
      this.showDialogWithImage();
    }
  },

  showDialog(e) {
    feedback.showModal(this, { title: '确认接单？', content: this.data.orderInfo.businessType === 'PURCHASE' ? '请先与发单方确认商品、规格和预算。接单后按要求购买并提交购买凭证，再开始配送；如遇问题，请联系平台协商处理。' : '接单后不可取消订单。平台仅提供交易渠道，服务细节请与发单方确认清楚。', confirmText: '接单', cancelText: '暂不接单', success: result => { if (result.confirm) return this.statusTo1(); } });
  },

  closeConfirm() {
    this.setData({ showConfirm: false });
    this.statusTo1();
  },

  closeDialog() {
    this.setData({ showConfirm: false });
  },

  // 待接单0->接单1（使用封装的 service）
  async statusTo1() {
    if (!await require('../../../../utils/accessGuard').ensureAuthenticated()) return;
    const userInfo = this.data.userInfo;

    // 用户未认证的提示

    // 用户手机号未填的提示
    if (!userInfo.phone || userInfo.phone === '') {
      feedback.navigate(this, 'navigateTo', { url: '/pages/mine/userInfo/info' }, '请先完善个人资料', 'warning');
      return;
    }

    // 接单逻辑
    try {
      // 检查是否已被接单
      const existingTaker = await takeOrderService.getTakeOrderDetail(this.data.id);

      // 修复：正确判断是否已被接单（检查对象是否有id属性，而不是仅判断对象存在）
      if (existingTaker && existingTaker.id) {
        feedback.navigate(this, 'switchTab', { url: '/pages/index/index' }, '订单已被接单', 'warning');
        return;
      }

      // 执行接单
      await takeOrderService.acceptOrderById(this.data.id);
      checkCilcleToast(this, "接单成功");


      await this._loadOrderInfo();
    } catch (error) {
      console.error("接单失败，请重试:", error);
      feedback.navigate(this, 'switchTab', { url: '/pages/index/index' }, '接单失败，请重试', 'error');
    }
  },

  // 接单1->已取件2（使用封装的 service）
  async statusTo2() {
    if (this.data.orderInfo.businessType === 'PURCHASE' && !this.data.imageAssetId) {
      errorCilcleToast(this, '请先上传购买凭证或商品照片');
      return;
    }
    try {
      showLoading(this.data.orderInfo.businessType === 'PURCHASE' ? '提交购买信息' : '取件中');
      await takeOrderService.updateTakeOrderStatus({ 
        id: this.data.taker.id, 
        status: 1,
        imageAssetId: this.data.orderInfo.businessType === 'PURCHASE' ? this.data.imageAssetId : null,
      });
      this.setData({ imageAssetId: null, fileList: [] });
      checkCilcleToast(this, this.data.orderInfo.businessType === 'PURCHASE' ? '购买信息已提交' : '取件成功');


      await this._loadOrderInfo();
      return true;
    } catch (error) {
      console.error('取件失败:', error);
      showError(this, this.data.orderInfo.businessType === 'PURCHASE' ? '提交购买信息失败' : '取件失败');
      return false;
    } finally {
      hideLoading();
    }
  },

  // 图片上传提示
  showDialogWithImage(purpose = 'DELIVERY_PROOF') {
    if (this._detailRequest) {
      this._resumeRefreshPending = true;
      detailRefresh.hide(this);
      this._detailHidden = false;
    }
    this.setData({ showWithImage: true, proofPurpose: purpose });
  },

  async closeConfirmWithImage() {
    if (this.data.proofSubmitting) return;
    if (this.data.proofUploading) {
      errorCilcleToast(this, '图片正在上传，请稍候');
      return;
    }
    if (!this.data.imageAssetId) {
      errorCilcleToast(this, this.data.proofPurpose === 'PURCHASE_PROOF'
        ? '请先上传购买凭证或商品照片' : '请先上传送达图片');
      return;
    }
    this.setData({ proofSubmitting: true });
    try {
      const submitted = this.data.proofPurpose === 'PURCHASE_PROOF'
        ? await this.statusTo2() : await this.statusTo3();
      if (submitted) this.setData({ showWithImage: false, proofPurpose: 'DELIVERY_PROOF' });
    } finally {
      this.setData({ proofSubmitting: false });
      this._resumeOrderRefresh();
    }
  },

  closeWithImage(e) {
    if (e?.detail?.visible === true || this.data.proofSubmitting) return;
    this._proofUploadVersion = (this._proofUploadVersion || 0) + 1;
    if (this.data.imageAssetId) {
      mediaService.releaseTemporaryImage(this.data.imageAssetId).catch(() => {});
    }
    this.setData({
      showWithImage: false,
      proofPurpose: 'DELIVERY_PROOF',
      imageAssetId: null,
      fileList: [],
      proofUploading: false,
    });
    this._resumeOrderRefresh();
  },

  // 图片上传
  handleAdd(e) {
    const { files } = e.detail;
    if (files.length) {
      this.onUpload(files[files.length - 1]);
    }
  },

  async onUpload(file) {
    if (!this.data.showWithImage || this.data.proofSubmitting || this.data.proofUploading) return;
    const version = this._proofUploadVersion = (this._proofUploadVersion || 0) + 1;
    const index = 0;
    this.setData({
      proofUploading: true,
      fileList: [{ ...file, status: 'loading' }],
    });

    try {
      const purpose = this.data.proofPurpose || 'DELIVERY_PROOF';
      const uploaded = await mediaService.uploadImage(
        file.url,
        purpose,
        (progress) => {
          if (version === this._proofUploadVersion) {
            this.setData({ [`fileList[${index}].percent`]: progress });
          }
        },
      );
      if (version !== this._proofUploadVersion) {
        await mediaService.releaseTemporaryImage(uploaded.mediaId).catch(() => {});
        return;
      }
      if (this.data.imageAssetId) {
        mediaService.releaseTemporaryImage(this.data.imageAssetId).catch(() => {});
      }
      this.setData({
        [`fileList[${index}].status`]: 'done',
        [`fileList[${index}].url`]: uploaded.previewUrl,
        [`fileList[${index}].mediaId`]: uploaded.mediaId,
        imageAssetId: uploaded.mediaId,
      });
    } catch (error) {
      if (version !== this._proofUploadVersion) return;
      this.setData({ [`fileList[${index}].status`]: 'failed' });
      showErrorToast(this, '图片上传失败');
    } finally {
      if (version === this._proofUploadVersion) this.setData({ proofUploading: false });
    }
  },

  handleRemove(e) {
    if (this.data.proofSubmitting) return;
    this._proofUploadVersion = (this._proofUploadVersion || 0) + 1;
    const { index } = e.detail;
    const { fileList } = this.data;
    const removed = fileList[index];
    if (removed && removed.mediaId) {
      mediaService.releaseTemporaryImage(removed.mediaId).catch(() => {});
    }
    fileList.splice(index, 1);
    this.setData({ fileList, imageAssetId: null, proofUploading: false });
  },

  // 已取件2->已派送3（使用封装的 service）
  async statusTo3() {
    if (!this.data.imageAssetId) {
      errorCilcleToast(this, '请先上传送达图片');
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


      await this._loadOrderInfo();
      return true;
    } catch (error) {
      console.error('派送失败:', error);
      showError(this, '派送失败');
      return false;
    } finally {
      hideLoading();
    }
  },

  // 拉起微信确定收款（保留原逻辑，使用 tokenManager）
  async _requestMerchantTransfer() {
    if (this.data.transferBusy) return;
    this.setData({ transferBusy: true });
    try {
      if (getApp().globalData.MOCK_PAYMENT) {
        showLoading('模拟收款中');
        const mockResult = await takeOrderService.mockReceiveSuccess(this.data.id);
        if (mockResult.code !== 1) throw new Error('模拟收款失败');
        hideLoading();
        feedback.showToast(this, {
          title: '模拟收款成功',
          icon: 'success'
        });
        await this._loadOrderInfo();
        return;
      }

      const result = await this._apiTransfer(this.data.id);
      const accountInfo = wx.getAccountInfoSync();
      await new Promise((resolve, reject) => wx.requestMerchantTransfer({
        mchId: result.mchId,
        appId: accountInfo.miniProgram.appId,
        package: result.packageInfo,
        success: resolve,
        fail: reject,
      }));
      feedback.showMessage(this, '收款申请已提交，请查看订单状态', { theme: 'info' });
    } catch (err) {
      hideLoading();
      if (!/cancel|取消/i.test(err?.errMsg || err?.message || '')) {
        feedback.showToast(this, { title: feedback.userText(err?.message, '收款未完成，请重试'), theme: 'error' });
      }
    } finally {
      await this._loadOrderInfo();
      this.setData({ transferBusy: false });
    }
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
          const transfer = res.data?.data;
          if (res.statusCode !== 200 || res.data?.code !== 1 || !transfer?.mchId || !transfer?.packageInfo) {
            reject(new Error('发起收款失败，请稍后重试'));
            return;
          }
          resolve(transfer);
        },
        fail: () => {
          reject(new Error('网络连接失败，请稍后重试'));
        }
      });
    });
  },

  // 集中统一获取、加载、更新订单相关的信息
  _loadOrderInfo(fresh = true) {
    if (fresh && !this._detailHidden && !this._detailDisposed) this._resumeRefreshPending = false;
    return detailRefresh.refresh(this, current => this._refreshOrderInfo(current), fresh);
  },

  async _refreshOrderInfo(current) {
    feedback.loaded(this);
    try {
      await tokenManager.waitForToken();
      if (!current()) return;
      await this.getGlobalData(current);
      if (!current()) return;
      await this._getOrderInfo(current);
      if (!current()) return;

      const status = this.data.orderInfo.status;
      if (status > 0 && status != 4) {
        await this._getMyTakeInfo(current);
        if (!current()) return;
        if (this.data.isMyTaken) {
          await this._getTaker(current);
          if (!current()) return;
          await this._getTakeImage(current);
        } else {
          this.setData({ taker: {}, image: null });
        }
      } else {
        this.setData({ taker: {}, image: null, isMyTaken: false });
      }
    } catch (err) {
      if (!current()) return;
      feedback.loadError(this, '加载失败，请重试', () => this._loadOrderInfo(), !!this.data.orderInfo.id);
      _logErrInfo("_loadOrderInfo", err.message);
    }
  },

  async _getMyTakeInfo(current = () => true) {
    try {
      const orders = await takeOrderService.getMyTakeOrders();
      if (!current()) return;
      const myOrder = orders.find(order => String(order.orderId) === String(this.data.id));
      this.setData({ isMyTaken: Boolean(myOrder) });
    } catch (error) {
      console.warn('获取接单身份失败:', error);
    }
  },

  // 获取订单详情（使用封装的 service）
  async _getOrderInfo(current = () => true) {
    try {
      const orderInfo = await userOrderService.getMyOrderDetail(this.data.id);
      if (!current()) return;
      console.log("请求的orderInfo: ", orderInfo);

      // 检查订单数据是否有效
      if (!orderInfo || !orderInfo.id) {
        throw new Error('订单不存在或已被删除');
      }
      orderInfo.images = orderInfo.images?.length ? orderInfo.images : (orderInfo.image ? [orderInfo.image] : []);

      // 安全处理地址分割（去掉第一个空格前的内容）
      const addressParts1 = orderInfo.pickUpAddress ? orderInfo.pickUpAddress.split(' ').slice(1) : [];
      const addressParts2 = orderInfo.reciveAddress ? orderInfo.reciveAddress.split(" ").slice(1) : [];
      orderInfo.expectTime = _getExpectTimeDisplay(orderInfo.createTime, orderInfo.gap, orderInfo.expectedDeliveryTime);
      const title = orderInfo.businessType === 'PURCHASE'
        ? ['待接单', '待购买', '配送中', '已送达', '已完成']
        : ['待接单', '待取件', '派送中', '已送达', '已完成'];
      const purchaseStatusText = ({ '-4': '退款异常', '-3': '退款成功', '-2': '退款中', '-1': '待支付', 0: '待接单', 1: '待购买', 2: '配送中', 3: '已送达', 4: '已取消', 5: '已完成', 6: '收款成功', 7: '收款失败' })[orderInfo.status] || '订单状态';
      const runnerStatusText = ({ 0: '待接单', 1: '待取件', 2: '派送中', 3: '已送达', 4: '已取消', 5: '已完成', 6: '收款成功', 7: '收款失败' })[orderInfo.status] || '订单状态';

      this.setData({
        orderInfo,
        purchaseStatusText,
        orderStatusText: orderInfo.businessType === 'PURCHASE' ? purchaseStatusText : runnerStatusText,
        progressStep: Math.min(Math.max(Number(orderInfo.status) || 0, 0), 4),
        title,
        pickUpAddress: addressParts1,
        reciveAddress: addressParts2
      });
      wx.showShareMenu({
        menus: Number(orderInfo.status) === 0
          ? ['shareAppMessage', 'shareTimeline'] : ['shareAppMessage'],
      });
      if (Number(orderInfo.status) !== 0) {
        wx.hideShareMenu({ menus: ['shareTimeline'] });
      }
    } catch (error) {
      throw new Error(`获取订单失败：${error.message}`);
    }
  },

  // 获取送达图片（使用封装的 service）
  async _getTakeImage(current = () => true) {
    try {
      const image = await takeOrderService.getDeliveryImage(this.data.id);
      if (!current()) return;
      this.setData({ image });
    } catch (error) {
      console.warn('获取送达图片失败:', error);
    }
  },

  // 获取接单人信息（使用封装的 service）
  async _getTaker(current = () => true) {
    try {
      const taker = await takeOrderService.getTakeOrderDetail(this.data.id);
      if (!current()) return;
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
    // 使用固定送达时间；旧订单使用兼容计算。
    const { createTime, gap, expectedDeliveryTime } = this.data.orderInfo;
    const expectTime = _getExpectedDeliveryDate(createTime, gap, expectedDeliveryTime);

    // 计算倒计时 = 预期送达时间 - 当前时间
    const nowTime = new Date();
    const timeDifference = Math.floor(expectTime - nowTime);

    this.setData({ time: timeDifference });
  },

  // 获取用户数据（使用封装的 service）
  async getGlobalData(current = () => true) {
    try {
      await tokenManager.waitForToken();
      if (!current()) return;
      const userInfo = await userService.getUserInfo();
      if (!current()) return;
      userInfo.token = tokenManager.getToken();
      this.setData({ userInfo });
    } catch (err) {
      console.error('获取用户数据失败:', err);
    }
  },

  onShareAppMessage() {
    return shareOrder(this.data.id, this.data.orderInfo);
  },

  onShareTimeline() {
    return {
      title: shareTitle,
      imageUrl: shareImageUrl,
      query: `id=${encodeURIComponent(this.data.id || '')}`,
    };
  },

  // 生命周期函数
  async onLoad(options) {
    this._skipInitialShow = true;
    wx.showShareMenu({ menus: ['shareAppMessage'] });
    console.log('接单详情页 - 接收到的参数:', options);
    console.log('接单详情页 - 订单ID:', options.id);
    this.setData({ id: options.id });
    try {
      await this._loadOrderInfo();
    } catch (err) {
      console.error('页面加载失败', err);
    }
  },

  onShow() {
    this._detailHidden = false;
    if (this._skipInitialShow) {
      this._skipInitialShow = false;
      return;
    }
    this._resumeRefreshPending = true;
    return this._resumeOrderRefresh();
  },

  _resumeOrderRefresh() {
    if (!this._resumeRefreshPending || this._detailHidden || this._detailDisposed
      || this.data.showWithImage || this.data.proofUploading || this.data.proofSubmitting) return;
    this._resumeRefreshPending = false;
    return this._loadOrderInfo(false);
  },

  onHide() {
    detailRefresh.hide(this);
  },

  onUnload() {
    detailRefresh.hide(this, true);
    this._proofUploadVersion = (this._proofUploadVersion || 0) + 1;
  },

  // 下拉刷新
  onPullDownRefresh() {
    if (this.data.showWithImage || this.data.proofUploading || this.data.proofSubmitting) {
      this._resumeRefreshPending = true;
      wx.stopPullDownRefresh();
      return;
    }
    return this._loadOrderInfo(false).finally(() => wx.stopPullDownRefresh());
  },

  // 预览图片
  tapOnImageToPreview(res) {
    const imageUrl = res.currentTarget.dataset.src;
    const ifClickable = res.currentTarget.dataset.flag;

    if (ifClickable == 0) {
      return;
    }

    const orderImages = this.data.orderInfo.images || [];
    wx.previewImage({
      current: imageUrl,
      urls: orderImages.includes(imageUrl) ? orderImages : [imageUrl],
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
