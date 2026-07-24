const secondHandService = require('../../../services/secondHandService');
const userService = require('../../../services/userService');
const tokenManager = require('../../../utils/tokenManager');
const { orderStatus, formatRemain, friendlyError } = require('../../../utils/secondHandStatus');

Page({
  data: {
    id: null,
    order: {},
    messages: [],
    messageContent: '',
    role: 'buyer',
    counterpartyLabel: '卖家',
    counterpartyName: '同学',
    steps: [],
    canPay: false,
    canCancel: false,
    canDeliver: false,
    canConfirm: false,
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
      const messages = order.productId ? await secondHandService.listProductMessages(order.productId) : [];
      const counterpartyLabel = role === 'buyer' ? '卖家' : '买家';
      const counterpartyName = role === 'buyer' ? order.sellerName : order.buyerName;
      this.setData({
        order: this.decorateOrder(order, role),
        role,
        counterpartyLabel,
        counterpartyName: counterpartyName || '同学',
        messages,
        steps: this.buildSteps(order.status),
        canPay: role === 'buyer' && Number(order.status) === 0,
        canCancel: role === 'buyer' && (Number(order.status) === 0 || Number(order.status) === 1),
        canDeliver: role === 'seller' && Number(order.status) === 1,
        canConfirm: role === 'buyer' && Number(order.status) === 2,
        payRemainSeconds: Math.max(0, Number(order.payRemainSeconds) || 0),
        payRemainText: Number(order.status) === 0 ? formatRemain(order.payRemainSeconds) : '',
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
      coverImage: this.firstImage(order.productImages),
      statusText: orderStatus(order.status).text,
      statusTheme: orderStatus(order.status).theme,
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

  buildSteps(status) {
    const current = Number(status);
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
      { key: 8, title: '收款处理中', desc: '正在处理卖家收款' },
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
    if (this.data.actionLoading || !this.data.canPay) return;
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
    wx.showModal({
      title: '取消订单',
      content: Number(this.data.order.status) === 1 ? '卖家交付前取消会发起退款，确认继续？' : '取消后商品会释放回市场，确认继续？',
      confirmText: '确认取消',
      confirmColor: '#d54941',
      success: async (res) => {
        if (!res.confirm) return;
        try {
          this.setData({ actionLoading: 'cancel' });
          await secondHandService.cancelOrder(this.data.id, '买家取消订单');
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
      content: Number(order.deliveryMode) === 1
        ? `请确认已配送到：${order.deliveryAddressText || '买家填写地址'}。标记后买家可确认收货。`
        : `请确认已在自提点完成交接：${order.deliveryAddressText || '约定地点'}。标记后买家可确认收货。`,
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
    wx.showModal({
      title: '确认收货',
      content: '确认后平台会结算给卖家。如商品有问题，请先和卖家沟通。',
      confirmText: '确认收货',
      success: async (res) => {
        if (!res.confirm) return;
        try {
          this.setData({ actionLoading: 'confirm' });
          await secondHandService.confirmOrder(this.data.id);
          if (getApp().globalData.MOCK_PAYMENT) {
            await secondHandService.mockReceiveSuccess(this.data.id);
          }
          wx.showToast({ title: '交易已完成', icon: 'success' });
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

  setMessage(e) {
    this.setData({ messageContent: e.detail.value });
  },

  async sendMessage() {
    if (this.data.actionLoading === 'message') return;
    const content = this.data.messageContent.trim();
    if (!content) {
      wx.showToast({ title: '请输入留言', icon: 'none' });
      return;
    }
    const receiverId = this.data.role === 'buyer' ? this.data.order.sellerId : this.data.order.buyerId;
    const user = await this.getCurrentUser();
    if (user.id != null && Number(receiverId) === Number(user.id)) {
      wx.showToast({ title: '无需给自己留言', icon: 'none' });
      return;
    }
    try {
      this.setData({ actionLoading: 'message' });
      await secondHandService.sendMessage({
        productId: this.data.order.productId,
        orderId: this.data.order.id,
        receiverId,
        content,
      });
      this.setData({ messageContent: '' });
      wx.showToast({ title: '已发送', icon: 'success' });
      this.loadDetail();
    } catch (error) {
      wx.showToast({ title: this.errorText(error, '发送失败'), icon: 'none' });
    } finally {
      this.setData({ actionLoading: '' });
    }
  },

  gotoProduct() {
    wx.navigateTo({ url: `/pages/second-hand/detail/detail?id=${this.data.order.productId}` });
  },

  statusDesc(order, role) {
    const status = Number(order.status);
    if (status === 0) return '请在倒计时结束前完成支付';
    if (status === 1) return role === 'seller' ? '请与买家确认交付后再标记' : '等待卖家交付';
    if (status === 2) return role === 'buyer' ? '检查商品无误后确认收货' : '等待买家确认收货';
    if (status === 5) return '退款正在处理，请稍候';
    if (status === 7) return '退款遇到异常，请联系客服';
    if (status === 8) return '确认收货后，正在处理卖家收款';
    if (status === 10) return '卖家收款失败，请联系客服';
    return '交易状态已更新';
  },

  errorText(error, fallback) {
    return friendlyError(error, fallback);
  },

  startPayTimer() {
    this.clearPayTimer();
    if (Number(this.data.order.status) !== 0 || this.data.payRemainSeconds <= 0) return;
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
