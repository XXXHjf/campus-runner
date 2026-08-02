// 引入服务和工具
const userOrderService = require('../../../../services/userOrderService');
const takeOrderService = require('../../../../services/takeOrderService');
const tokenManager = require('../../../../utils/tokenManager');
const { showLoading, hideLoading, showError } = require('../../../../utils/transformers');
const { 
  checkCilcleToast, 
  errorCilcleToast, 
  showErrorToast, 
  showSuccessToast,
  _getExpectTimeDisplay,
  _parseStrDateTime
} = require('../../../../utils/commonJs');

const url = getApp().globalData.API_URL;
const UNPAID_TIMEOUT_MS = 30 * 60 * 1000;

Page({
  data: {
    // 步骤条
    title: ['待接单', '待取件', '派送中', '已送达', '已完成'],
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
    fileList: [],
    image: null,
    cur: {},
    current: null,
    note: '',
    radio: ["自食其力，暂不需要", "信息有误，重新下单", "不知道为什么，但是不用了"],
    buttonText: '',
    buttonDisabled: false,
    dltDialogVisable: false,
    visible: false,
    unpaidRemainTime: 0,
    unpaidDeleteProcessing: false,
  },

  _normalizeAmountFields(orderInfo) {
    const normalized = { ...orderInfo };
    const serviceFeeRate = normalized.service_fee_rate ?? normalized.serviceFeeRate;
    const serviceFee = normalized.service_fee ?? normalized.serviceFee;
    const payAmount = normalized.pay_amount ?? normalized.payAmount;

    normalized.service_fee_rate = serviceFeeRate ?? null;
    normalized.service_fee = serviceFee ?? null;
    normalized.pay_amount = payAmount ?? null;

    return normalized;
  },

  _shouldTriggerRefund(orderInfo) {
    if (!orderInfo) return false;
    const status = Number(orderInfo.status);
    const payAmount = Number(orderInfo.pay_amount ?? orderInfo.payAmount ?? 0);
    return status === 0 && payAmount > 0 && !!orderInfo.orderNumber;
  },

  // 按钮根据状态跳转方法
  button(e) {
    if (this.data.buttonDisabled) return;

    const orderInfo = this.data.orderInfo;
    const status = Number(orderInfo.status);
    if (status === 0)
      this.handlePopup(e);
    else if (status > 0 && status < 3)
      errorCilcleToast(this, "订单仍未完成")
    else if (status === 3)
      this.tbtnTapConfirm();
    else if (status === -4)
      this.gotoFeedback();
    else
      this.setData({ dltDialogVisable: true });
  },

  // 按钮样式
  buttonColor() {
    const status = Number(this.data.orderInfo.status);
    if (status === 0) {
      this.setData({ theme: 'primary', buttonText: '取消订单', buttonDisabled: false });
    } else if (status === 3) {
      this.setData({ theme: 'primary', buttonText: '确认订单', buttonDisabled: false });
    } else if (status === 1 || status === 2) {
      this.setData({ theme: 'default', buttonText: '订单进行中', buttonDisabled: true });
    } else if (status === -2) {
      this.setData({ theme: 'default', buttonText: '退款处理中', buttonDisabled: true });
    } else if (status === -4) {
      this.setData({ theme: 'primary', buttonText: '反馈异常', buttonDisabled: false });
    } else {
      this.setData({ theme: 'danger', buttonText: '删除订单', buttonDisabled: false });
    }
  },

  gotoFeedback() {
    wx.switchTab({
      url: '/pages/mine/mine/mine',
      success: () => {
        wx.showToast({
          title: '请在“我的-意见反馈”提交异常',
          icon: 'none',
          duration: 2200
        });
      }
    });
  },

  // 取消订单（使用封装的 service）
  async cancelOrder() {
    const orderInfo = this.data.orderInfo || {};
    const cancelReason = this.data.cancelReason || '发单人取消订单';
    const shouldRefund = this._shouldTriggerRefund(orderInfo);
    let refundError = null;

    try {
      showLoading('取消中');
      await userOrderService.cancelOrder(this.data.id, cancelReason, orderInfo.orderNumber);

      if (shouldRefund) {
        try {
          await userOrderService.refundOrder(orderInfo.orderNumber, cancelReason);
        } catch (error) {
          refundError = error;
          console.error('退款申请失败:', error);
        }
      }

      await this._loadOrderInfo();
      if (refundError) {
        const msg = refundError && refundError.message ? refundError.message : '请稍后在订单列表查看退款状态';
        showErrorToast(this, `取消成功，但退款申请失败：${msg}`);
      } else {
        checkCilcleToast(this, shouldRefund ? "取消成功，退款申请已提交" : "取消成功");
      }
    } catch (error) {
      console.error('取消订单失败:', error);
      showError('取消失败');
    } finally {
      hideLoading();
    }
  },

  // 取消按钮 弹出层
  handlePopup(e) {
    const { item } = e.currentTarget.dataset;
    this.setData({ cur: item, visible: true });
  },

  onVisibleChange(e) {
    this.setData({ visible: e.detail.visible });
  },

  onChange(event) {
    this.setData({ current: event.detail.value });
  },

  confirm() {
    const current = this.data.current;
    if (!current) {
      errorCilcleToast(this, "未选择取消原因");
      return;
    }
    const cancelReason = current == 3 ? 
      (this.data.note || '未填写说明') : 
      this.data.radio[current];
    
    this.setData({ cancelReason, visible: false });
    this.cancelOrder();
  },

  cancel() {
    this.setData({ visible: false });
    this._loadOrderInfo();
  },

  handleInput(e) {
    this.setData({ note: e.detail.value });
  },

  // 确认收货
  async tbtnTapConfirm() {
    try {
      showLoading('确认中');
      await userOrderService.confirmOrder(this.data.id);
      await this._loadOrderInfo();
      showSuccessToast(this, "确认成功");
    } catch (err) {
      showErrorToast(this, "确认失败");
    } finally {
      hideLoading();
    }
  },

  // 待支付订单去支付
  async payOrder() {
    try {
      showLoading('发起支付');
      if (getApp().globalData.MOCK_PAYMENT) {
        await userOrderService.mockPaySuccess(this.data.id);
        showSuccessToast(this, "模拟支付成功");
        await this._loadOrderInfo();
        return;
      }
      const paymentParams = await this._apiPostTransaction(this.data.id);
      await this._requestRegister(paymentParams);
      let synced = true;
      try {
        await this._syncPayStatus();
      } catch (syncError) {
        synced = false;
        console.error('支付状态同步失败:', syncError);
      }
      showSuccessToast(this, "支付成功");
      await this._loadOrderInfo();
      if (!synced) {
        showErrorToast(this, "支付成功，状态同步稍后刷新");
      }
    } catch (error) {
      console.error('订单支付失败:', error);
      if (error && error.message && error.message.includes('ORDERPAID')) {
        try {
          await this._syncPayStatus();
          await this._loadOrderInfo();
          showSuccessToast(this, "支付已完成，状态已同步");
          return;
        } catch (syncError) {
          console.error('支付状态同步失败:', syncError);
        }
      }
      const message = error && error.message ? error.message : '支付失败';
      showErrorToast(this, message);
    } finally {
      hideLoading();
    }
  },

  async _syncPayStatus(retryTimes = 3) {
    let lastError = null;
    for (let index = 0; index < retryTimes; index += 1) {
      try {
        return await userOrderService.syncPayStatus(this.data.id);
      } catch (error) {
        lastError = error;
        if (index < retryTimes - 1) {
          await new Promise((resolve) => setTimeout(resolve, 1000));
        }
      }
    }
    throw lastError;
  },

  // 获取支付参数
  _apiPostTransaction(orderID) {
    return new Promise((resolve, reject) => {
      wx.request({
        url: `${url}/api/wx-pay/jspai/${orderID}`,
        method: 'POST',
        header: {
          'token': tokenManager.getToken()
        },
        success: (res) => {
          const paymentData = res?.data?.data;
          if (!paymentData) {
            const message = res?.data?.msg || '支付参数获取失败';
            reject(new Error(message));
            return;
          }
          resolve(paymentData);
        },
        fail: (err) => {
          reject(new Error("支付请求失败"));
        }
      });
    });
  },

  // 拉起微信支付
  _requestRegister(res) {
    return new Promise((resolve, reject) => {
      wx.requestPayment({
        timeStamp: res.timeStamp,
        nonceStr: res.nonceStr,
        package: "prepay_id=" + res.prepayId,
        signType: res.signType,
        paySign: res.paySign,
        success: (paymentRes) => {
          resolve(paymentRes);
        },
        fail: (err) => {
          if (err && err.errMsg && err.errMsg.includes('cancel')) {
            reject(new Error('已取消支付'));
            return;
          }
          reject(new Error('支付未完成，请稍后重试'));
        }
      })
    })
  },

  // 删除订单（使用封装的 service）
  async delOrder() {
    try {
      showLoading('删除中');
      await userOrderService.deleteOrder(this.data.id);
      this.setData({ dltDialogVisable: false });
      checkCilcleToast(this, "删除成功");
      setTimeout(() => wx.navigateBack(), 1500);
    } catch (error) {
      console.error('删除订单失败:', error);
      showError('删除失败');
    } finally {
      hideLoading();
    }
  },

  // 集中统一获取、加载、更新订单相关的信息
  async _loadOrderInfo() {
    try {
      await tokenManager.waitForToken();
      await this._getOrderInfo();
      this._updateUnpaidCountdown();
      
      const status = this.data.orderInfo.status;
      if (status > 0 && status < 6 && status != 4) {
        await this._getTaker();
      }
      await this._getTakeImage();
      this.buttonColor();
    } catch (err) {
      showErrorToast(this, "加载失败");
    }
  },

  // 获取订单详情（使用封装的 service）
  async _getOrderInfo() {
    try {
      const orderInfoRaw = await userOrderService.getMyOrderDetail(this.data.id);
      const orderInfo = this._normalizeAmountFields(orderInfoRaw);
      
      // 检查订单数据是否有效
      if (!orderInfo || !orderInfo.id) {
        throw new Error('订单不存在或已被删除');
      }
      
      // 安全处理地址分割
      const addressParts1 = orderInfo.pickUpAddress ? orderInfo.pickUpAddress.split(" ") : [];
      const addressParts2 = orderInfo.reciveAddress ? orderInfo.reciveAddress.split(" ") : [];
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

  // 生命周期函数
  onLoad(options) {
    console.log('订单详情页 - 接收到的参数:', options);
    console.log('订单详情页 - 订单ID:', options.id);
    this.setData({ id: options.id });
    this._loadOrderInfo();
  },

  _clearUnpaidTimer() {
    if (this._unpaidTimer) {
      clearInterval(this._unpaidTimer);
      this._unpaidTimer = null;
    }
  },

  _updateUnpaidCountdown() {
    this._clearUnpaidTimer();
    const { orderInfo } = this.data;

    if (!orderInfo || Number(orderInfo.status) !== -1 || !orderInfo.createTime) {
      this.setData({ unpaidRemainTime: 0 });
      return;
    }

    const createTime = _parseStrDateTime(orderInfo.createTime);
    if (!createTime || isNaN(createTime.getTime())) {
      this.setData({ unpaidRemainTime: 0 });
      return;
    }
    const expireAt = createTime.getTime() + UNPAID_TIMEOUT_MS;

    const tick = () => {
      const remain = expireAt - Date.now();
      if (remain <= 0) {
        this.setData({ unpaidRemainTime: 0 });
        this._clearUnpaidTimer();
        this._autoDeleteUnpaidOrder();
        return;
      }
      this.setData({ unpaidRemainTime: remain });
    };

    tick();
    this._unpaidTimer = setInterval(tick, 1000);
  },

  async _autoDeleteUnpaidOrder() {
    if (this.data.unpaidDeleteProcessing) return;
    this.setData({ unpaidDeleteProcessing: true });

    try {
      await userOrderService.deleteOrder(this.data.id);
      showSuccessToast(this, '订单超时未支付，已自动删除');
      setTimeout(() => {
        wx.navigateBack();
      }, 1000);
    } catch (error) {
      console.error('自动删除未支付订单失败:', error);
      showErrorToast(this, '订单超时，请手动刷新');
      this.setData({ unpaidDeleteProcessing: false });
    }
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
    wx.previewImage({ urls: [imageUrl], showmenu: true });
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
  },

  closeDialog() {
    this.setData({ dltDialogVisable: false });
  },

  continueDlt() {
    this.delOrder();
  },

  onUnload() {
    this._clearUnpaidTimer();
  }
});
