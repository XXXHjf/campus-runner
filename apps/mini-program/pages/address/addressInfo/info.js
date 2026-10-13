const feedback = require('../../../utils/feedback');
// 引入服务和工具
const addressService = require('../../../services/addressService');
const tokenManager = require('../../../utils/tokenManager');
const { showLoading, hideLoading, showError, showSuccess } = require('../../../utils/transformers');
// 轻提示
const { toast: Toast } = require('../../../utils/feedback');

const url = getApp().globalData.API_URL;
const ADDRESS_SAVE_NOT_APPLIED = 'ADDRESS_SAVE_NOT_APPLIED';

// 选择器方法
const getOptions = (obj, filter) => {
  const res = Object.keys(obj).map((key) => ({
    value: key,
    label: obj[key]
  }));
  if (filter) {
    return res.filter(filter);
  }
  return res;
};
const match = (v1, v2, size) => v1.toString().slice(0, size) === v2.toString().slice(0, size);

Page({
  retryFeedbackLoad() { return this._feedbackRetry?.(); },
  data: {
    userInfo: null,
    id: null,
    addressInfo: {},
    upSchool: '',
    upCompus: '',
    upCategory: '',
    upBuilding: '',
    upSchoolNum: '',
    upCompusNum: '',
    upCategoryNum: '',
    upBuildingNum: '',
    upDetails: '',
    upDefault: 0,
    upLabel: '',
    note: '请选择地址',
    showLabelTip: false,
    showDetailsTip: false,
  },
  // 删除（使用封装的 service）
  async b1() {
    let addresses;
    try { addresses = await addressService.getAddressSummary(); }
    catch (error) { showError(this, '地址加载失败，请重试'); return; }
    const result = await feedback.showModal(this, { title: '删除地址？', content: this.data.addressInfo.isDefault == 1 && addresses.total > 1 ? '删除后，最近添加的其他地址将设为默认地址。' : '删除后需要重新添加该地址。', confirmText: '删除', cancelText: '保留', danger: true });
    if (!result.confirm) return;
    try {
      showLoading('删除中');
      const id = this.data.addressInfo.id;
      await addressService.deleteUserAddress(id);
      feedback.navigate(this, 'navigateBack', {}, '删除成功');
    } catch (error) {
      console.error("删除地址失败:");
      showError(this, '删除失败');
    } finally {
      hideLoading();
    }
  },
  // 保存（使用封装的 service）
  async b2() {
    try {
      showLoading('保存中');

      const id = this.data.addressInfo.id;
      const requestedDetails = this.data.upDetails;
      const requestedLabel = this.data.upLabel;

      // 动态构建数据对象
      let data = {
        'id': id,
        'isDefault': Number(this.data.upDefault),
        'details': requestedDetails,
        'label': requestedLabel,
      };

      if (this.data.upCompusNum != null) {
        data.compusNumberId = this.data.upCompusNum;
      }
      if (this.data.upCategoryNum != null) {
        data.buildCategoryNumberId = this.data.upCategoryNum;
      }
      if (this.data.upBuildingNum != null) {
        data.buildingNumberId = this.data.upBuildingNum;
      }

      await addressService.updateUserAddressDetail(data);

      const savedAddress = await addressService.getUserAddressDetail(id);
      let defaultMatches = Number(savedAddress.isDefault) === data.isDefault;
      // 历史地址可能没有默认地址；服务端会在本次写入时补齐。
      if (!defaultMatches && data.isDefault === 0 && Number(savedAddress.isDefault) === 1
          && this.data.addressInfo.isDefault != 1) {
        const summary = await addressService.getAddressSummary();
        defaultMatches = Number(summary.defaultCount) === 1 && String(summary.defaultAddress?.id) === String(id);
      }
      if (!defaultMatches
          || (savedAddress.details || '') !== requestedDetails
          || (savedAddress.label || '') !== requestedLabel) {
        const error = new Error('地址修改未生效');
        error.code = ADDRESS_SAVE_NOT_APPLIED;
        throw error;
      }

      feedback.navigate(this, 'navigateBack', {}, '更新成功');
    } catch (error) {
      console.error("更新地址失败:");
      showError(this, error && error.code === ADDRESS_SAVE_NOT_APPLIED ? '保存未生效，请稍后重试' : '更新失败');
    } finally {
      hideLoading();
    }
  },
  // 详细地址修改（实时输入）
  handleDetails(e) {
    let value = e.detail.value;
    
    // 显示提示
    this.setData({
      showDetailsTip: value.length > 0
    });
    
    // 实时更新值（不在这里限制长度，避免干扰中文输入法）
    this.setData({
      upDetails: value
    });
  },
  
  // 详细地址输入框失去焦点（在这里限制长度）
  handleDetailsBlur(e) {
    const maxLength = 30;
    let value = e.detail && typeof e.detail.value === 'string'
      ? e.detail.value
      : this.data.upDetails;
    
    // 限制字数
    if (value.length > maxLength) {
      value = value.substring(0, maxLength);
      Toast({
        context: this,
        message: '详细地址最多30个字',
        theme: 'warning',
      });
      
      // 更新为截断后的值
      this.setData({
        upDetails: value
      });
    }
    
    console.log("details值： ");
    
    this.setData({
      showDetailsTip: false
    });
  },
  // 地址标签修改（实时输入）
  handleLabel(e) {
    let value = e.detail.value;
    
    // 显示提示
    this.setData({
      showLabelTip: value.length > 0
    });
    
    // 实时更新值（不在这里限制长度，避免干扰中文输入法）
    this.setData({
      upLabel: value
    });
  },
  
  // 标签输入框失去焦点（在这里限制长度）
  handleLabelBlur(e) {
    let value = e.detail && typeof e.detail.value === 'string'
      ? e.detail.value
      : this.data.upLabel;
    
    // 限制最多3个字符
    if (value.length > 3) {
      value = value.substring(0, 3);
      Toast({
        context: this,
        message: '标签最多3个字',
        theme: 'warning',
      });
      
      // 更新为截断后的值
      this.setData({
        upLabel: value
      });
    }
    
    console.log("label值： ");
    
    this.setData({
      showLabelTip: false
    });
  },
  clearDetails() {
    this.setData({
      upDetails: '',
      showDetailsTip: false,
    });
  },
  clearLabel() {
    this.setData({
      upLabel: '',
      showLabelTip: false,
    });
  },
  // 设置为默认地址
  changeDefault(event) {
    if (this.data.addressInfo.isDefault == 1) return;
    const oldDefault = Number(this.data.upDefault);
    this.setData({
      upDefault: (oldDefault + 1) % 2,
    })
  },
  // 显示地址选择器
  showAddressPicker() {
    const triplePicker = this.selectComponent('#triplePicker');
    if (triplePicker) {
      triplePicker.showPicker();
    }
  },
  
  // 处理地址选择器确认
  handleConfirmSelection(e) {
    const {
      upCompus,
      upCompusNumberId,
      upCategory,
      upCategoryNumberId,
      upBuilding,
      upBuildingNumberId
    } = e.detail;
    
    console.log("选择的地址:");
    
    this.setData({
      upCompus: upCompus,
      upCompusNum: upCompusNumberId,
      upCategory: upCategory,
      upCategoryNum: upCategoryNumberId,
      upBuilding: upBuilding,
      upBuildingNum: upBuildingNumberId
    });
  },
  // 获取传入的地址（使用封装的 service）
  async getAddressInfo() {
    feedback.loaded(this);
    try {
      const id = this.data.id;
      const addressInfo = await addressService.getUserAddressDetail(id);
      console.log("地址详情:");
      this.setData({
        addressInfo,
        upSchool: addressInfo.schoolName,
        upCompus: addressInfo.compusName,
        upCategory: addressInfo.buildCategoryName,
        upBuilding: addressInfo.buildingName,
        upDetails: addressInfo.details || '',
        upDefault: addressInfo.isDefault == 1 ? 1 : 0,
        upLabel: addressInfo.label || '',
        showLabelTip: false,
        showDetailsTip: false,
      });
    } catch (error) {
      console.error("获取地址详情失败:");
      feedback.loadError(this, '资料加载失败，请重试', () => this.getAddressInfo(), !!this.data.addressInfo.id);
    }
  },
  // 修改当前地址-选择器
  async changeAddressNew() {
    try {
      if (this.data.addressList) {
        this.setData({
          areaVisible: true,
          areaValue: [], // 重置选择器的值
        });
        
        await this.getAllAddress();
        const { schools } = this.data.addressList;
        this.setData({
          schools: getOptions(schools),
        });
        await this.lifetimes();
      } else {
        console.log('Address list is undefined or null.');
        throw new Error('Address list is undefined or null.');
      }
    } catch (error) {
      console.error("修改地址失败:");
      throw error;
    }
  },
  async lifetimes() {
    const version = (this._addressColumnVersion || 0) + 1;
    this._addressColumnVersion = version;
    const schools = this.data.schools;
    // 初次显示的校区与建筑
    const {
      compuses,
      buildings
    } = await this.getCompus(schools[0]?.value);
    if (version !== this._addressColumnVersion) return;
    this.setData({
      compuses: compuses,
      buildings: buildings,
    });
  },
  async onColumnChange(e) {
    console.log("pick:");
    const {
      column,
      index
    } = e.detail;
    const {
      schools,
      compuses
    } = this.data;

    if (column > 1) return;
    const columnVersion = (this._addressColumnVersion || 0) + 1;
    this._addressColumnVersion = columnVersion;
    if (column === 0) {
      // 更改学校
      const {
        compuses,
        buildings
      } = await this.getCompus(schools[index]?.value);
      if (columnVersion !== this._addressColumnVersion) return;
      this.setData({
        compuses,
        buildings,
      });
    }
    if (column === 1) {
      // 更改校区
      const buildings = await this.getBuildings(compuses[index]?.value);
      if (columnVersion !== this._addressColumnVersion) return;
      this.setData({
        buildings
      });
    }
    if (column === 2) {
      // 更改建筑
    }
  },
  async getCompus(schoolValue) {
    if (!schoolValue) return { compuses: [], buildings: [] };
    const compuses = getOptions(this.data.addressList.compuses, (compus) => match(compus.value, schoolValue, 3));
    this.setData({ compuses });
    const buildings = await this.getBuildings(compuses[0]?.value);
    return {
      compuses,
      buildings
    };
  },
  async getBuildings(compusValue) {
    const version = (this._buildingVersion || 0) + 1;
    this._buildingVersion = version;
    this.setData({ buildings: [], addressLoading: true });
    const token = tokenManager.getToken();
    try {
      if (!compusValue) return [];
      const raw = await addressService.getThreeLevelAddress(compusValue);
      if (version !== this._buildingVersion || token !== tokenManager.getToken()) return [];
      const map = { ...(this.data.addressList.buildings || {}) };
      for (const item of raw.building) map[item.numberId] = item.buildingName;
      this.setData({ 'addressList.buildings': map });
      return getOptions(map, building => match(building.value, compusValue, 6));
    } catch (error) {
      if (version === this._buildingVersion) feedback.showToast(this, { title: '地址加载失败，请重试', icon: 'error' });
      return [];
    } finally { if (version === this._buildingVersion) this.setData({ addressLoading: false }); }
  },
  onPickerChange(e) {
    if (this.data.addressLoading || !this.data.buildings?.some(item => String(item.value) === String(e.detail.value?.[2]))) return;
    const {
      value,
      label
    } = e.detail;
    console.log("picker confirm:");
    this.setData({
      areaVisible: false,
      areaValue: value,
      areaText: label.join(' '),
      upSchool: e.detail.label[0],
      upCompus: e.detail.label[1],
      upBuilding: e.detail.label[2],
      upSchoolNum: e.detail.value[0],
      upCompusNum: e.detail.value[1],
      upBuildingNum: e.detail.value[2],
    });
  },
  onPickerCancel(e) {
    console.log("picker cancel");
    this.setData({
      areaVisible: false,
    });
  },
  // 获取AddressList（使用封装的 service）
  async getAllAddress() {
    try {
      const addressData = await addressService.getThreeLevelAddress();
      const addressList = this.convertToListNew(addressData);
      this.setData({ addressList });
    } catch (error) {
      console.error("获取地址列表失败:");
      feedback.showMessage(this, '地址列表加载失败，请重试', { theme: 'error', action: '重试', onAction: () => this.getAllAddress() });
      throw error;
    }
  },
  // 转换AddressList格式-选择器
  convertToListNew(addressList) {
    let List = {
      schools: {},
      compuses: {},
      buildings: {}
    };
    for (const building of addressList.building) {
      // 建筑名，数字 ID 作为 ID
      List.buildings[building.numberId] = building.buildingName;
    }
    for (const compus of addressList.compus) {
      // 校区名，数字 ID 作为 ID
      List.compuses[compus.numberId] = compus.compusName;
      // List.buildings[compus.numberId] = '全部楼宇';
    }
    for (const school of addressList.school) {
      // 学校名，数字 ID 作为 ID
      List.schools[school.numberId] = school.schoolName;
      // List.compuses[school.numberId] = '全校范围';
      // List.buildings[school.numberId] = '全部楼宇';
    }
    return List;
  },
  // 生命周期函数--监听页面加载
  async onLoad(options) {
    try {
      // 等待 token 就绪
      await tokenManager.waitForToken();
      
      // 初始化用户信息
      const app = getApp();
      this.setData({
        id: options.id,
        userInfo: {
          token: app.globalData.userInfo?.token || tokenManager.getToken()
        }
      });
      
      // 获取地址详情
      await this.getAddressInfo();
    } catch (error) {
      console.error("页面加载失败:");
      feedback.loadError(this, '资料加载失败，请重试', () => this.getAddressInfo(), !!this.data.addressInfo.id);
    }
  },
  // 下拉刷新事件
  async onPullDownRefresh() {
    try {
      await this.getAddressInfo();
    } finally {
      wx.stopPullDownRefresh();
    }
  },

})
