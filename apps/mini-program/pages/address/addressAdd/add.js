import Toast from 'tdesign-miniprogram/toast/index';

// 引入服务和工具
const addressService = require('../../../services/addressService');
const userService = require('../../../services/userService');
const tokenManager = require('../../../utils/tokenManager');
const { showLoading, hideLoading, showError } = require('../../../utils/transformers');
const {
  containsEmoji,
  checkCilcleToast,
  errorCilcleToast,
  _getOptions,
  _compareFirstNChars,
} = require('../../../utils/commonJs');

const url = getApp().globalData.API_URL;

Page({
  data: {
    userInfo: null,
    upSchool: '',
    upCompus: '',
    upCategory: '',
    upBuilding: '',
    upDetails: '',
    upDefault: 0,
    uptype: 0,
    upLabel: '',
    type: ['取件', '收件'],
    note: '请选择地址',

    // 联级/选择器
    addressList: [], //用于存储地址allList
    visible: false, //联级选择判定
    value: [], //联级选择器值
    upNumberid: null, //联级
    areaVisible: false, //选择器判定
    areaValue: [], //选择器取值
    upSchoolNumberId: null, //选择器
    upCompusNumberId: null, //选择器
    upCategoryNumberId: null, //选择器
    upBuildingNumberId: null, //选择器
    areaText: '',
    text: '',
    schools: [],
    compuses: [],
    category: [],
    buildings: [],

    showPicker: false,

    selectedCompus: '',
    selectedCompusNumberId: '',
    selectedCategory: '',
    selectedCategoryNumberId: '',
    selectedBuilding: '',
    selectedBuildingNumberId: '',
  },
  // 保存地址（使用封装的 service）
  async save() {
    // 地址中有emoji表情，弹出提示
    if (containsEmoji(this.data.upLabel) || containsEmoji(this.data.upDetails)) {
      errorCilcleToast(this, "地址中不能有表情");
      return;
    }

    const upCompusNumberId = this.data.selectedCompusNumberId;
    const upCategoryNumberId = this.data.selectedCategoryNumberId;
    const upBuildingNumberId = this.data.selectedBuildingNumberId;
    const type = this.data.uptype;
    const label = this.data.upLabel;
    const details = this.data.upDetails;

    if (!upCompusNumberId) {
      errorCilcleToast(this, "空地址");
      return;
    }

    try {
      showLoading('保存中');
      
      const addressData = {
        compusNumberId: upCompusNumberId,
        buildCategoryNumberId: upCategoryNumberId,
        buildingNumberId: upBuildingNumberId,
        type: type,
        label: label,
        details: details,
      };

      await addressService.createUserAddress(addressData);
      checkCilcleToast(this, "添加成功");
      
      setTimeout(() => {
        wx.navigateBack();
      }, 1500);
    } catch (error) {
      console.error('保存地址失败:', error);
      showError('保存失败');
    } finally {
      hideLoading();
    }
  },
  // 未选择地址，弹出提醒
  saveOnRecord() {
    errorCilcleToast(this, "请填写收货地址");
  },
  // 点击地址，弹出三级列表
  handleShowPicker() {
    const triplePicker = this.selectComponent('#triplePicker');
    if (triplePicker) {
      triplePicker.showPicker();
    }
  },
  // 获取自定义组件 triple-address-pricker 传递的数据
  handleConfirmSelection(e) {
    const {
      upCompus,
      upCompusNumberId,
      upCategory,
      upCategoryNumberId,
      upBuilding,
      upBuildingNumberId
    } = e.detail;

    this.setData({
      showPicker: false,
      selectedCompus: upCompus,
      selectedCompusNumberId: upCompusNumberId,
      selectedCategory: upCategory,
      selectedCategoryNumberId: upCategoryNumberId,
      selectedBuilding: upBuilding,
      selectedBuildingNumberId: upBuildingNumberId,
    });
  },
  // 详细地址修改（实时输入）
  tiptChangeDetail(e) {
    console.log('details值： ' + e.detail.value);
    this.setData({
      upDetails: e.detail.value
    });
  },
  
  // 详细地址输入框失去焦点（限制长度）
  tiptChangeDetailBlur(e) {
    const maxLength = 20;
    let value = e.detail.value || this.data.upDetails;
    
    // 限制字数
    if (value.length > maxLength) {
      value = value.substring(0, maxLength);
      Toast({
        context: this,
        selector: '#t-toast',
        message: '详细地址最多20个字',
        theme: 'warning',
        direction: 'column',
      });
      
      // 更新为截断后的值
      this.setData({
        upDetails: value
      });
    }
    
    console.log('details最终值： ' + value);
  },
  
  // 地址标签修改（实时输入）
  tiptChangeLabel(e) {
    console.log('label值： ' + e.detail.value);
    this.setData({
      upLabel: e.detail.value
    });
  },
  
  // 地址标签输入框失去焦点（限制长度）
  tiptChangeLabelBlur(e) {
    const maxLength = 20;
    let value = e.detail.value || this.data.upLabel;
    
    // 限制字数
    if (value.length > maxLength) {
      value = value.substring(0, maxLength);
      Toast({
        context: this,
        selector: '#t-toast',
        message: '标签最多20个字',
        theme: 'warning',
        direction: 'column',
      });
      
      // 更新为截断后的值
      this.setData({
        upLabel: value
      });
    }
    
    console.log('label最终值： ' + value);
  },
  // 地址类型切换
  changeType() {
    const oldtype = this.data.uptype;
    this.setData({
      uptype: (oldtype + 1) % 2,
    })
  },
  // 设置为默认地址
  changeDefault(event) {
    const oldDefault = this.data.upDefault;
    this.setData({
      upDefault: (oldDefault + 1) % 2,
    })
  },
  // 保存为默认地址（使用封装的 service）
  async saveAsDefault() {
    try {
      const id = this.data.addressInfo.id;
      const type = this.data.addressInfo.type;
      
      await addressService.updateUserAddress({ id, type });
      console.log('设置默认地址成功');
    } catch (error) {
      console.error('设置默认地址失败:', error);
      showError('操作失败');
    }
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





  // 生命周期函数--监听页面显示
  async onShow() {
    try {
      // 等待 token 就绪
      await tokenManager.waitForToken();
      
      // 获取用户数据
      await this.getGlobalData();
      console.log('getGlobalData执行成功');
    } catch (error) {
      console.error('页面加载失败:', error);
      showError('加载失败');
    }
  },
  // 生命周期函数--监听页面加载
  onLoad(options) {
    const uptype = options.type;
    // console.log("address id：",id);
    this.setData({
      uptype: uptype,
    })
  }
})