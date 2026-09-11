const secondHandService = require('../../../services/secondHandService');
const userService = require('../../../services/userService');
const tokenManager = require('../../../utils/tokenManager');
const {
  SECOND_HAND_ONLINE_PAYMENT_ENABLED,
  orderStatus,
  formatRemain,
  friendlyError,
} = require('../../../utils/secondHandStatus');

Page({
  data: {
    id: null,
    order: {},
    role: 'buyer',
    counterpartyLabel: '卖家',
    counterpartyName: '同学',
    counterpartyPhone: '',
    counterpartyPhoneMasked: '',
    steps: [],
    canPay: false,
    canCancel: false,
    canDeliver: false,
    canConfirm: false,
    canReceive: false,
    actionLoading: '',
    payRemainSeconds: 0,
    payRemainText: '',
    loading: false,
  },

  onLoad(options) {
    this.setData({ id: options.id });
    this.loadDetail();
  },

  onUnload() {
    this.clearPayTimer();
  },

  async loadDetail() {
    this.setData({ loading: true });
    try {
      const order = await secondHandService.getOrderDetail(this.data.id);
      const user = await this.getCurrentUser();
      const role = this.resolveRole(order, user);
      const counterpartyLabel = role === 'buyer' ? '卖家' : '买家';
      const counterpartyName = role === 'buyer' ? order.sellerName : order.buyerName;
      const counterpartyPhone = order.counterpartyPhone ? String(order.counterpartyPhone) : '';
      const isOffline = String(order.tradeMode || '').toUpperCase() === 'OFFLINE';
      const status = Number(order.status);
      this.setData({
        order: this.decorateOrder(order, role),
        role,
        counterpartyLabel,
        counterpartyName: counterpartyName || '同学',
        counterpartyPhone,
        counterpartyPhoneMasked: this.maskPhone(counterpartyPhone),
        steps: this.buildSteps(order),
        canPay: SECOND_HAND_ONLINE_PAYMENT_ENABLED && !isOffline && role === 'buyer' && status === 0,
        canCancel: isOffline
          ? (role === 'buyer' || role === 'seller') && status === 1
          : role === 'buyer' && (status === 0 || status === 1),
        canDeliver: role === 'seller' && status === 1,
        canConfirm: role === 'buyer' && status === 2,
        canReceive: !isOffline && role === 'seller' && status === 8,
        payRemainSeconds: Math.max(0, Number(order.payRemainSeconds) || 0),
        payRemainText: !isOffline && status === 0 ? formatRemain(order.payRemainSeconds) : '',
      });
      this.startPayTimer();
    } catch (error) {
      wx.showToast({ title: this.errorText(error, '加载失败'), icon: 'none' });
    } finally {
      this.setData({ loading: false });
      wx.stopPullDownRefresh();
    }
  },

  decorateOrder(order, role) {
    const deliveryMode = Number(order.deliveryMode);
    const pickupAddress = order.pickupAddressSnapshot || order.deliveryRemark || '';
    const deliveryAddress = order.buyerDeliveryAddressSnapshot || order.deliveryRemark || '';
    return {
      ...order,
      isOffline: String(order.tradeMode || '').toUpperCase() === 'OFFLINE',
      coverImage: this.firstImage(order.productImages),
      statusText: orderStatus(order.status, order.tradeMode).text,
      statusTheme: orderStatus(order.status, order.tradeMode).theme,
      statusDesc: this.statusDesc(order, role),
      deliveryText: deliveryMode === 1 ? '卖家配送' : '买家自提',
      deliveryAddressTitle: deliveryMode === 1 ? '配送到' : '自提点',
      deliveryAddressText: deliveryMode === 1 ? deliveryAddress : pickupAddress,
      pickupAddressText: pickupAddress,
    };
  },

  firstImage(images) {
    if (!images) return '';
    return String(images).split(',').filter(Boolean)[0] || '';
  },

  async getCurrentUser() {
    const app = getApp();
    const cached = app.globalData.userInfo || wx.getStorageSync('userInfo') || {};
    if (cached.id != null) return cached;
    try {
      const fresh = await userService.getUserInfo();
      const userInfo = {
        ...cached,
        ...fresh,
        token: cached.token || tokenManager.getToken(),
      };
      app.globalData.userInfo = userInfo;
      wx.setStorageSync('userInfo', userInfo);
      return userInfo;
    } catch (error) {
      return cached;
    }
  },

  resolveRole(order, user) {
    const isBuyer = user.id != null && Number(user.id) === Number(order.buyerId);
    const isSeller = user.id != null && Number(user.id) === Number(order.sellerId);
    if (isBuyer && isSeller) {
      return Number(order.status) === 1 ? 'seller' : 'buyer';
    }
    if (isSeller) return 'seller';
    return 'buyer';
  },

  buildSteps(order) {
    const current = Number(order.status);
    if (String(order.tradeMode || '').toUpperCase() === 'OFFLINE') {
      if (current === 4) {
        return [{ title: '订单已取消', desc: '商品已恢复展示', active: true, danger: true }];
      }
      if (current === 11) {
        return [{ title: '双方协商中', desc: '请通过私信与对方沟通后续处理', active: true, danger: true }];
      }
      return [
        { key: 1, title: '买家已下单', desc: '请双方联系并协商付款、交付方式' },
        { key: 2, title: '卖家已交付', desc: '等待买家确认交易完成' },
        { key: 3, title: '交易已完成', desc: '双方已完成线下付款和商品交接' },
      ].map((item) => ({
        ...item,
        active: current >= item.key,
      }));
    }
    if (current === 4) {
      return [{ title: '订单已取消', desc: '如已付款，将按交易进度处理退款', active: true, danger: true }];
    }
    if ([5, 6, 7].includes(current)) {
      return [
        { title: '买家已支付', desc: '款项已由平台保障', active: true },
        { title: current === 6 ? '退款成功' : current === 7 ? '退款异常' : '退款处理中', desc: current === 7 ? '请联系客服处理' : '退款正在处理，请稍候', active: true, danger: current === 7 },
      ];
    }
    if ([10, 11].includes(current)) {
      return [
        { title: '交易已确认', desc: '买家已确认收货', active: true },
        { title: current === 10 ? '收款异常' : '协商处理中', desc: current === 10 ? '卖家收款失败，请联系客服处理' : '请留意后续处理结果', active: true, danger: true },
      ];
    }
    const base = [
      { key: 1, title: '买家已支付', desc: '款项已由平台保障' },
      { key: 2, title: '卖家已交付', desc: '等待买家检查商品' },
      {
        key: 8,
        title: order.transferState === 'WAIT_USER_CONFIRM' ? '等待卖家确认收款' : '收款处理中',
        desc: order.transferState === 'WAIT_USER_CONFIRM' ? '卖家需打开收款页面并按提示确认' : '正在处理卖家收款',
      },
      { key: 9, title: '交易完成', desc: '卖家收款成功' },
    ];
    if (current === 0) {
      return [{ title: '待支付', desc: '支付后商品会进入交易中', active: true }, ...base];
    }
    return base.map((item) => ({
      ...item,
      active: current >= item.key,
    }));
  },

  pay() {
    if (!SECOND_HAND_ONLINE_PAYMENT_ENABLED || this.data.actionLoading || !this.data.canPay) return;
    if (this.data.payRemainSeconds <= 0) {
      wx.showToast({ title: '订单已超时，请刷新查看', icon: 'none' });
      this.loadDetail();
      return;
    }
    wx.showModal({
      title: '确认支付',
      content: `将通过平台担保支付 ¥${this.data.order.payAmount}，确认收货后卖家收款。`,
      confirmText: '去支付',
      success: async (res) => {
        if (!res.confirm) return;
        try {
          this.setData({ actionLoading: 'pay' });
          if (getApp().globalData.MOCK_PAYMENT) {
            await secondHandService.mockPaySuccess(this.data.id);
            wx.showToast({ title: '支付成功', icon: 'success' });
            this.loadDetail();
            return;
          }
          const pay = await secondHandService.payOrder(this.data.id);
          wx.requestPayment({
            timeStamp: pay.timeStamp,
            nonceStr: pay.nonceStr,
            package: `prepay_id=${pay.prepayId}`,
            signType: pay.signType,
            paySign: pay.paySign,
            complete: () => this.loadDetail(),
          });
        } catch (error) {
          wx.showToast({ title: this.errorText(error, '支付失败'), icon: 'none' });
          this.loadDetail();
        } finally {
          this.setData({ actionLoading: '' });
        }
      },
    });
  },

  cancelOrder() {
    if (this.data.actionLoading || !this.data.canCancel) return;
    const isOffline = Boolean(this.data.order.isOffline);
    wx.showModal({
      title: '取消订单',
      content: isOffline
        ? '取消后商品将重新展示，双方无需继续本次交易。确认取消？'
        : (Number(this.data.order.status) === 1 ? '卖家交付前取消会发起退款，确认继续？' : '取消后商品将重新上架，确认继续？'),
      confirmText: '确认取消',
      confirmColor: '#d54941',
      success: async (res) => {
        if (!res.confirm) return;
        try {
          this.setData({ actionLoading: 'cancel' });
          await secondHandService.cancelOrder(
            this.data.id,
            isOffline
              ? `${this.data.role === 'seller' ? '卖家' : '买家'}取消线下交易`
              : '买家取消订单',
          );
          wx.showToast({ title: '已取消', icon: 'success' });
          this.loadDetail();
        } catch (error) {
          wx.showToast({ title: this.errorText(error, '取消失败'), icon: 'none' });
          this.loadDetail();
        } finally {
          this.setData({ actionLoading: '' });
        }
      },
    });
  },

  markDelivered() {
    if (this.data.actionLoading || !this.data.canDeliver) return;
    const order = this.data.order;
    wx.showModal({
      title: '标记已交付',
      content: order.isOffline
        ? `请确认已在${Number(order.deliveryMode) === 1 ? '配送地址' : '约定地点'}完成商品交接。标记后将等待买家确认交易完成。`
        : (Number(order.deliveryMode) === 1
          ? `请确认已配送到：${order.deliveryAddressText || '买家填写地址'}。标记后买家可确认收货。`
          : `请确认已在自提点完成交接：${order.deliveryAddressText || '约定地点'}。标记后买家可确认收货。`),
      confirmText: '已交付',
      success: async (res) => {
        if (!res.confirm) return;
        try {
          this.setData({ actionLoading: 'deliver' });
          await secondHandService.markDelivered(this.data.id);
          wx.showToast({ title: '已标记交付', icon: 'success' });
          this.loadDetail();
        } catch (error) {
          wx.showToast({ title: this.errorText(error, '操作失败'), icon: 'none' });
          this.loadDetail();
        } finally {
          this.setData({ actionLoading: '' });
        }
      },
    });
  },

  confirmReceipt() {
    if (this.data.actionLoading || !this.data.canConfirm) return;
    const isOffline = Boolean(this.data.order.isOffline);
    wx.showModal({
      title: isOffline ? '确认交易完成' : '确认收货',
      content: isOffline
        ? '请确认你已与卖家完成商品和款项交接。平台不会核验双方的付款情况。'
        : '确认后平台会结算给卖家。如商品有问题，请先和卖家沟通。',
      confirmText: isOffline ? '确认完成' : '确认收货',
      success: async (res) => {
        if (!res.confirm) return;
        try {
          this.setData({ actionLoading: 'confirm' });
          await secondHandService.confirmOrder(this.data.id);
          if (isOffline) {
            wx.showToast({ title: '交易已完成', icon: 'success' });
          } else if (getApp().globalData.MOCK_PAYMENT) {
            await secondHandService.mockReceiveSuccess(this.data.id);
            wx.showToast({ title: '交易已完成', icon: 'success' });
          } else {
            wx.showToast({ title: '已确认收货，卖家收款处理中', icon: 'none' });
          }
          this.loadDetail();
        } catch (error) {
          wx.showToast({ title: this.errorText(error, '确认失败'), icon: 'none' });
          this.loadDetail();
        } finally {
          this.setData({ actionLoading: '' });
        }
      },
    });
  },

  async requestTransferClaim() {
    if (this.data.actionLoading || !this.data.canReceive) return;
    try {
      this.setData({ actionLoading: 'receive' });
      const claim = await secondHandService.getTransferClaim(this.data.id);
      if (claim.state === 'SUCCESS') {
        wx.showToast({ title: '收款已到账', icon: 'success' });
        this.loadDetail();
        return;
      }
      if (!wx.canIUse('requestMerchantTransfer')) {
        wx.showModal({
          title: '微信版本过低',
          content: '请升级微信后重新打开订单确认收款。',
          showCancel: false,
        });
        return;
      }
      await new Promise((resolve, reject) => {
        wx.requestMerchantTransfer({
          mchId: claim.mchId,
          appId: claim.appId,
          package: claim.packageInfo,
          success: resolve,
          fail: reject,
        });
      });
      wx.showToast({ title: '已打开收款页面，请按提示确认', icon: 'none' });
      this.loadDetail();
    } catch (error) {
      const message = error && (error.errMsg || error.message);
      if (!message || !String(message).includes('cancel')) {
        wx.showToast({ title: this.errorText(error, '暂时无法确认收款'), icon: 'none' });
      }
      this.loadDetail();
    } finally {
      this.setData({ actionLoading: '' });
    }
  },

  gotoConversation() {
    const order = this.data.order;
    const counterpartyId = this.data.role === 'buyer' ? order.sellerId : order.buyerId;
    if (!order.productId || !counterpartyId) return;
    wx.navigateTo({
      url: `/pages/second-hand/conversation/conversation?productId=${order.productId}&counterpartyId=${counterpartyId}&orderId=${order.id}`,
    });
  },

  callCounterparty() {
    if (!this.data.counterpartyPhone) return;
    wx.makePhoneCall({ phoneNumber: this.data.counterpartyPhone });
  },

  maskPhone(phone) {
    return /^1\d{10}$/.test(phone) ? `${phone.slice(0, 3)}****${phone.slice(7)}` : phone;
  },

  gotoProduct() {
    wx.navigateTo({ url: `/pages/second-hand/detail/detail?id=${this.data.order.productId}` });
  },

  statusDesc(order, role) {
    const status = Number(order.status);
    if (String(order.tradeMode || '').toUpperCase() === 'OFFLINE') {
      if (status === 1) return '请联系对方，自行协商付款和交付';
      if (status === 2) return role === 'buyer' ? '确认双方完成交接后结束交易' : '等待买家确认交易完成';
      if (status === 3) return '双方已完成线下交易';
      if (status === 4) return '本次交易已取消';
      if (status === 11) return '请通过私信与对方协商处理';
      return '交易状态已更新';
    }
    if (status === 0) return '请在倒计时结束前完成支付';
    if (status === 1) return role === 'seller' ? '请与买家确认交付后再标记' : '等待卖家交付';
    if (status === 2) return role === 'buyer' ? '检查商品无误后确认收货' : '等待买家确认收货';
    if (status === 5) return '退款正在处理，请稍候';
    if (status === 7) return '退款遇到异常，请联系客服';
    if (status === 8 && role === 'seller' && order.transferState === 'WAIT_USER_CONFIRM') return '请打开收款页面并按提示确认';
    if (status === 8) return role === 'buyer' ? '卖家正在确认收款' : '收款正在处理中';
    if (status === 10) return '卖家收款失败，请联系客服';
    return '交易状态已更新';
  },

  errorText(error, fallback) {
    return friendlyError(error, fallback);
  },

  startPayTimer() {
    this.clearPayTimer();
    if (this.data.order.isOffline || Number(this.data.order.status) !== 0 || this.data.payRemainSeconds <= 0) return;
    this.payTimer = setInterval(() => {
      const next = Math.max(0, this.data.payRemainSeconds - 1);
      this.setData({
        payRemainSeconds: next,
        payRemainText: formatRemain(next),
      });
      if (next <= 0) {
        this.clearPayTimer();
        this.loadDetail();
      }
    }, 1000);
  },

  clearPayTimer() {
    if (this.payTimer) {
      clearInterval(this.payTimer);
      this.payTimer = null;
    }
  },

  onPullDownRefresh() {
    this.loadDetail();
  },
});
