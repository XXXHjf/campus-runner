// 引入服务和工具
const takeOrderService = require('../../../../services/takeOrderService');
const { showLoading, hideLoading, showError } = require('../../../../utils/transformers');
const { checkCilcleToast } = require('../../../../utils/commonJs');

Page({
  data: {
    categoryImages: {},
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
      '5': 'warning',    // 有偿：结算中 / 无偿：已完成
      '6': 'success',    // 已完成（提现成功）
      '7': 'danger'      // 提现失败
    },
    orderStatus: {
      '-4': "退款异常",
      '-3': "退款成功",
      '-2': "退款中",
      '-1': "未支付",
      '0': "待接单",
      '1': "已接单",
      '2': "派送中",
      '3': "已送达",
      '4': "已取消",
      '5': "结算中",  // 默认显示，会根据是否有偿动态修改
      '6': "已完成",
      '7': "提现失败"
    },
    // 侧边栏标题映射
    tabTitles: {
      'all': '全部接单',
      'processing': '进行中',
      'settling': '结算中',
      'completed': '已完成',
      'exception': '异常'
    },
    tabValue: 'all',
    visible: false,
    sidebar: [{
        title: '全部',
        value: 'all',
      },
      {
        title: '进行中',
        value: 'processing',
      },
      {
        title: '结算中',
        value: 'settling',
      },
      {
        title: '已完成',
        value: 'completed',
      },
      {
        title: '异常',
        value: 'exception',
      },
    ],
    takes: [],
  },
  
  // 接单详情跳转
  gotoTakesInfo(event) {
    const id = event.currentTarget.dataset.item.orderId;
    wx.navigateTo({
      url: `/pages/orders/takeOrders/takesInfo/info?id=${id}`,
    })
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
  
  // 根据状态(数字)和订单价格获取状态描述(文字描述)
  _getStatusDescription(statusCode, price) {
    const { orderStatus } = this.data;
    const baseStatus = orderStatus[statusCode.toString()] || "未知状态";
    
    // 状态5需要根据是否有偿来区分显示
    if (statusCode === 5) {
      return (price === null || price === undefined) ? "已完成" : "结算中";
    }
    
    return baseStatus;
  },
  
  // 根据状态和订单价格获取标签颜色主题
  _getStatusTheme(statusCode, price) {
    const { statusTheme } = this.data;
    const baseTheme = statusTheme[statusCode.toString()] || 'default';
    
    // 状态5需要根据是否有偿来区分主题色
    if (statusCode === 5) {
      return (price === null || price === undefined) ? 'success' : 'warning';
    }
    
    return baseTheme;
  },
  
  // 判断订单是否属于当前筛选分类
  _isOrderMatchTab(order, tabValue) {
    const status = order.status;
    const hasPrice = order.price !== null && order.price !== undefined;
    
    // 全部
    if (tabValue === 'all') return true;
    
    // 进行中（已接单、派送中、已送达）
    if (tabValue === 'processing') return status === 1 || status === 2 || status === 3;
    
    // 结算中（状态5且有偿订单）
    if (tabValue === 'settling') return status === 5 && hasPrice;
    
    // 已完成（状态5的无偿订单 + 状态6）
    if (tabValue === 'completed') return (status === 5 && !hasPrice) || status === 6;
    
    // 异常（已取消、提现失败、退款异常、退款成功、退款中）
    if (tabValue === 'exception') return status === 4 || status === 7 || status === -4 || status === -3 || status === -2;
    
    return false;
  },
  
  // 获取全部接单信息（使用封装的 service）
  async getAllTake() {
    try {
      showLoading('加载中');
      const takes = await takeOrderService.getMyTakeOrders();
      
      // 添加状态描述和主题（根据订单价格动态设置）
      takes.forEach(item => {
        item.statusDesc = this._getStatusDescription(item.status, item.price);
        item.statusTheme = this._getStatusTheme(item.status, item.price);
      });
      
      this.setData({ takes });
    } catch (error) {
      console.error('获取接单失败:', error);
      showError('获取接单失败');
    } finally {
      hideLoading();
    }
  },
  
  // 生命周期函数--监听页面加载
  onLoad(options) {
    this.getAllTake();
  },
  
  // 生命周期函数--监听页面显示
  onShow() {
    this.getAllTake();
  },
  
  // 下拉刷新事件
  onPullDownRefresh() {
    this.getAllTake().then(() => {
      checkCilcleToast(this, '刷新成功');
      wx.stopPullDownRefresh();
    });
  },
})