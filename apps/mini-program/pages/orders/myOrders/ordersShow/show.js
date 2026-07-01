// 引入服务和工具
const userOrderService = require('../../../../services/userOrderService');
const { showLoading, hideLoading, showError } = require('../../../../utils/transformers');
const { checkCilcleToast } = require('../../../../utils/commonJs');

Page({
  data: {
    // 状态标签颜色主题映射
    statusTheme: {
      '-4': 'danger',    // 退款异常
      '-3': 'warning',   // 退款成功
      '-2': 'warning',   // 退款中
      '-1': 'warning',   // 未支付
      '0': 'primary',    // 待接单
      '1': 'success',    // 已接单
      '2': 'success',    // 派送中
      '3': 'success',    // 已送达
      '4': 'default',    // 已取消
      '5': 'success',    // 已完成
      '6': 'success',    // 提现成功
      '7': 'danger'      // 提现失败
    },
    orderStatus: {
      '-4': "退款异常",
      '-3': "退款成功",
      '-2': "退款中",
      '-1': "待支付",
      '0': "待接单",
      '1': "已接单",
      '2': "派送中",
      '3': "已送达",
      '4': "已取消",
      '5': "已完成",
      '6': "提现成功",
      '7': "提现失败"
    },
    // 侧边栏标题映射
    tabTitles: {
      'all': '全部订单',
      '0': '待接单',
      'processing': '进行中',
      '5': '已完成',
      '4': '已取消',
      'refund': '退款/异常'
    },
    tabValue: 'all',
    visible: false,
    sidebar: [{
        title: '全部',
        value: 'all',
      },
      {
        title: '待接单',
        value: '0',
      },
      {
        title: '进行中',
        value: 'processing',
      },
      {
        title: '已完成',
        value: '5',
      },
      {
        title: '已取消',
        value: '4',
      },
      {
        title: '退款/异常',
        value: 'refund',
      },
    ],
    orders: [],
  },
  
  // 跳转订单详情
  gotoOrderInfo(event) {
    const id = event.currentTarget.dataset.item.id;
    wx.navigateTo({
      url: `/pages/orders/myOrders/ordersInfo/info?id=${id}`,
    })
  },
  
  // 根据状态(数字)获取状态描述(文字描述)
  _getStatusDescription(statusCode) {
    const { orderStatus } = this.data;
    return orderStatus[statusCode.toString()] || "未知状态";
  },
  
  // 根据状态获取标签颜色主题
  _getStatusTheme(statusCode) {
    const { statusTheme } = this.data;
    return statusTheme[statusCode.toString()] || 'default';
  },
  
  // 判断订单是否属于当前筛选分类
  _isOrderMatchTab(order, tabValue) {
    const status = order.status;
    
    // 全部
    if (tabValue === 'all') return true;
    
    // 待接单
    if (tabValue === '0') return status === 0;
    
    // 进行中（已接单、派送中、已送达）
    if (tabValue === 'processing') return status === 1 || status === 2 || status === 3;
    
    // 已完成
    if (tabValue === '5') return status === 5;
    
    // 已取消
    if (tabValue === '4') return status === 4;
    
    // 退款/异常（退款异常、退款成功、退款中、待支付）
    if (tabValue === 'refund') return status === -4 || status === -3 || status === -2 || status === -1;
    
    return false;
  },
  
  // 打开侧边栏
  openDrawer() {
    this.setData({
      visible: true,
    });
  },
  
  // 侧边栏项目点击
  itemClick(e) {
    console.log(e.detail);
    this.setData({
      visible: false,
      tabValue: e.detail.item.value,
    });
  },
  
  // 遮罩层点击
  overlayClick(e) {
    console.log(e.detail);
    this.setData({
      visible: false,
    })
  },
  
  // 获取全部订单信息（使用封装的 service）
  async _getAllOrder() {
    try {
      showLoading('加载中');
      const orders = await userOrderService.getMyOrders();
      
      // 添加状态描述和主题
      orders.forEach(order => {
        const payAmount = order.pay_amount ?? order.payAmount;
        order.displayPayAmount = (payAmount === null || payAmount === undefined) ? order.price : payAmount;
        order.statusDesc = this._getStatusDescription(order.status);
        order.statusTheme = this._getStatusTheme(order.status);
      });
      
      this.setData({ orders });
    } catch (error) {
      console.error('获取订单失败:', error);
      showError('获取订单失败');
    } finally {
      hideLoading();
    }
  },
  
  // 生命周期函数--监听页面加载
  onLoad(options) {
    this._getAllOrder();
  },
  
  // 生命周期函数--监听页面显示
  onShow() {
    this._getAllOrder();
  },
  
  // 下拉刷新事件
  onPullDownRefresh() {
    this._getAllOrder().then(() => {
      checkCilcleToast(this, '刷新成功');
      wx.stopPullDownRefresh();
    });
  },
})
