// 引入服务和工具
const addressService = require('../../../services/addressService');
const tokenManager = require('../../../utils/tokenManager');
const { showLoading, hideLoading, showError, showSuccess } = require('../../../utils/transformers');
// 轻提示
import Toast from 'tdesign-miniprogram/toast/index';

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
    try {
      showLoading('删除中');
      const id = this.data.addressInfo.id;
      await addressService.deleteUserAddress(id);
      showSuccess('删除成功');
      setTimeout(() => {
        wx.navigateBack();
      }, 1500);
    } catch (error) {
      console.error('删除地址失败:', error);
      showError('删除失败');
    } finally {
      hideLoading();
    }
  },
  // 保存（使用封装的 service）
  async b2() {
    try {
      showLoading('保存中');

      if (this.data.upDefault == 1) {
        await this.saveAsDefault();
      }

      const id = this.data.addressInfo.id;
      const requestedDetails = this.data.upDetails;
      const requestedLabel = this.data.upLabel;

      // 动态构建数据对象
      let data = {
        'id': id,
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

      if (requestedDetails === '' || requestedLabel === '') {
        const savedAddress = await addressService.getUserAddressDetail(id);
        const detailsNotCleared = requestedDetails === '' && (savedAddress.details || '') !== '';
        const labelNotCleared = requestedLabel === '' && (savedAddress.label || '') !== '';

        if (detailsNotCleared || labelNotCleared) {
          const error = new Error('地址修改未生效');
          error.code = ADDRESS_SAVE_NOT_APPLIED;
          throw error;
        }
      }

      showSuccess('更新成功');

      setTimeout(() => {
        wx.navigateBack();
      }, 1500);
    } catch (error) {
      console.error('更新地址失败:', error);
      showError(error && error.code === ADDRESS_SAVE_NOT_APPLIED ? '保存未生效，请稍后重试' : '更新失败');
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
        selector: '#t-toast',
        message: '详细地址最多30个字',
        theme: 'warning',
        direction: 'column',
      });
      
      // 更新为截断后的值
      this.setData({
        upDetails: value
      });
    }
    
    console.log('details值： ' + value);
    
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
        selector: '#t-toast',
        message: '标签最多3个字',
        theme: 'warning',
        direction: 'column',
      });
      
      // 更新为截断后的值
      this.setData({
        upLabel: value
      });
    }
    
    console.log('label值： ' + value);
    
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
    const oldDefault = this.data.upDefault;
    this.setData({
      upDefault: (oldDefault + 1) % 2,
    })
  },
  // 保存为默认地址（使用封装的 service）
  async saveAsDefault() {
    try {
      const id = this.data.addressInfo.id;
      await addressService.updateUserAddress({ id });
      console.log('设置默认地址成功');
    } catch (error) {
      console.error('设置默认地址失败:', error);
      throw error;
    }
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
    
    console.log('选择的地址:', upCompus, upCategory, upBuilding);
    
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
    try {
      const id = this.data.id;
      const addressInfo = await addressService.getUserAddressDetail(id);
      console.log('地址详情:', addressInfo);
      this.setData({
        addressInfo,
        upSchool: addressInfo.schoolName,
        upCompus: addressInfo.compusName,
        upCategory: addressInfo.buildCategoryName,
        upBuilding: addressInfo.buildingName,
        upDetails: addressInfo.details || '',
        upDefault: addressInfo.isDefault,
        upLabel: addressInfo.label || '',
        showLabelTip: false,
        showDetailsTip: false,
      });
    } catch (error) {
      console.error('获取地址详情失败:', error);
      showError('加载失败');
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
        this.lifetimes();
      } else {
        console.log('Address list is undefined or null.');
        throw new Error('Address list is undefined or null.');
      }
    } catch (error) {
      console.error('修改地址失败:', error);
      throw error;
    }
  },
  lifetimes() {
    const schools = this.data.schools;
    // 初次显示的校区与建筑
    const {
      compuses,
      buildings
    } = this.getCompus(schools[0].value);
    this.setData({
      compuses: compuses,
      buildings: buildings,
    });
  },
  onColumnChange(e) {
    console.log('pick:', e.detail);
    const {
      column,
      index
    } = e.detail;
    const {
      schools,
      compuses
    } = this.data;

    if (column === 0) {
      // 更改学校
      const {
        compuses,
        buildings
      } = this.getCompus(schools[index].value);
      this.setData({
        compuses,
        buildings,
      });
    }
    if (column === 1) {
      // 更改校区
      const buildings = this.getBuildings(compuses[index].value);
      this.setData({
        buildings
      });
    }
    if (column === 2) {
      // 更改建筑
    }
  },
  getCompus(schoolValue) {
    const compuses = getOptions(this.data.addressList.compuses, (compus) => match(compus.value, schoolValue, 3));
    const buildings = this.getBuildings(compuses[0].value);
    return {
      compuses,
      buildings
    };
  },
  getBuildings(compusValue) {
    return getOptions(this.data.addressList.buildings, (building) => match(building.value, compusValue, 6));
  },
  onPickerChange(e) {
    const {
      value,
      label
    } = e.detail;
    console.log('picker confirm:', e.detail);
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
    console.log('picker cancel', e.detail);
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
      console.error('获取地址列表失败:', error);
      showError('加载地址失败');
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
      console.error('页面加载失败:', error);
      showError('加载失败');
    }
  },
  // 下拉刷新事件
  onPullDownRefresh() {
    // 这里加上要刷新的逻辑
    this.getAddressInfo()
    // ------------
    this.showHorizontalText()
    wx.stopPullDownRefresh()
  },
  // 轻展示的方法
  showHorizontalText() {
    Toast({
      context: this,
      selector: '#t-toast',
      message: '刷新成功',
      icon: 'check-circle',
    });
  },
})
