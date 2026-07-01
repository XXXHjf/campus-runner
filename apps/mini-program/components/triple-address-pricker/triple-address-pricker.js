import Toast from 'tdesign-miniprogram/toast/index';

const tokenManager = require('../../utils/tokenManager');
const addressService = require('../../services/addressService');
const {
  errorCilcleToast,
  _getOptions,
  _compareFirstNChars
} = require('../../utils/commonJs');

Component({

  /**
   * 组件的属性列表
   */
  properties: {

  },

  /**
   * 组件的初始数据
   */
  data: {
    isVisible: false,
    areaValue: [],
    addressList: [], // 
    compuses: [],
    category: [],
    buildings: [],
  },

  /**
   * 组件的方法列表
   */
  methods: {
    // 显示三级选择器
    async showPicker() {
      // *** 关键修复：显示选择器前先检查地址数据是否已加载 ***
      if (!this.data.addressList || Object.keys(this.data.addressList.compuses || {}).length === 0) {
        console.log('地址数据未加载，尝试重新加载...');
        try {
          await this.changeAddressNew();
        } catch (error) {
          console.error('加载地址数据失败:', error);
          wx.showToast({
            title: '地址数据加载失败',
            icon: 'error',
            duration: 2000
          });
          return; // 加载失败则不显示选择器
        }
      }
      
      this.setData({
        isVisible: true
      });
    },
    // 隐藏三级选择器
    hidePicker() {
      this.setData({
        isVisible: false
      });
    },
    // 三级列表滑动任意一列触发的事件
    onColumnChange(e) {
      console.log('三级列表滑动:', e.detail);
      const {
        column,
        index
      } = e.detail;
      const {
        compuses,
        category,
      } = this.data;

      if (column === 0) {
        // 滑动校区列（第一列）变动类型和楼宇两列
        const {
          category,
          buildings
        } = this._getCategory(compuses[index].value);
        this.setData({
          category,
          buildings,
        });
      }
      if (column === 1) {
        // 滑动类型列（第二列）变动楼宇列
        const buildings = this._getBuildings(category[index].value);
        this.setData({
          buildings
        });
      }
      if (column === 2) {
        // 滑动楼宇列，三级列表不变化
      }
    },
    // 三级列表取消时触发的事件
    onPickerCancel() {
      this.hidePicker();
    },
    // 三级列表确定时触发的事件
    onPickerChange(e) {
      const {
        value,
        label
      } = e.detail;
      console.log('picker confirm:', e.detail);
      this.setData({
        areaValue: value
      });
      // 触发自定义事件，传递选择的三级列表
      this.triggerEvent('confirmSelection', {
        upCompus: label[0],
        upCompusNumberId: value[0],
        upCategory: label[1],
        upCategoryNumberId: value[1],
        upBuilding: label[2],
        upBuildingNumberId: value[2],
      });
      this.hidePicker();
    },
    // 转换AddressList格式-选择器
    _convertToListNew(rawAddressList) {
      let addressMap = {
        compuses: {},
        category: {},
        buildings: {}
      };
      
      // 添加数据验证
      if (!rawAddressList) {
        console.warn('原始地址数据为空');
        return addressMap;
      }
      
      if (rawAddressList.building && Array.isArray(rawAddressList.building)) {
        for (const building of rawAddressList.building) {
          addressMap.buildings[building.numberId] = building.buildingName;
        }
      }
      
      if (rawAddressList.buildCategory && Array.isArray(rawAddressList.buildCategory)) {
        for (const category of rawAddressList.buildCategory) {
          addressMap.category[category.numberId] = category.buildCategoryName;
        }
      }
      
      if (rawAddressList.compus && Array.isArray(rawAddressList.compus)) {
        for (const compus of rawAddressList.compus) {
          addressMap.compuses[compus.numberId] = compus.compusName;
        }
      }
      
      return addressMap;
    },
    // 修改当前地址-选择器
    async changeAddressNew() {
      try {
        // *** 关键改进：等待 token 就绪 ***
        await tokenManager.waitForToken();
        
        // 检查是否有 token
        if (!tokenManager.hasToken()) {
          console.warn('组件初始化时未登录，跳过地址加载');
          return;
        }
        
        const addressList = await this._getAllAddress();
        if (!addressList) {
          throw new Error("地址列表为空");
        }
        
        const { compuses } = addressList;
        this.setData({
          addressList: addressList,
          areaValue: [], // 重置选中值
          compuses: _getOptions(compuses),
        });
        this._pickerOnFirstShow();
      } catch (err) {
        console.error("组件地址加载失败:", err.message);
        // 只在用户操作时显示错误提示
        if (this.data.isVisible) {
          wx.showToast({
            title: '地址加载失败',
            icon: 'error'
          });
        }
      }
    },
    // 获取所有地址 resolve(addressList)
    async _getAllAddress() {
      try {
        // *** 改进：使用 addressService，统一走 request 层 ***
        const rawData = await addressService.getThreeLevelAddress();
        const addressList = this._convertToListNew(rawData);
        return addressList;
      } catch (err) {
        console.error("_getAllAddress failed:", err);
        throw new Error("获取地址数据失败: " + err.message);
      }
    },
    // 设置三级列表刚弹出时显示的地址
    _pickerOnFirstShow() {
      const compuses = this.data.compuses;
      const {
        category,
        buildings
      } = this._getCategory(compuses[0].value);
      this.setData({
        category: category,
        buildings: buildings,
      });
    },
    // 根据校区编号获取类型，如传入1010010000000，此时前6位固定
    _getCategory(campusValue) {
      const category = _getOptions(this.data.addressList.category, (category) => _compareFirstNChars(category.value, campusValue, 6));
      const buildings = this._getBuildings(category[0].value);
      return {
        category,
        buildings
      };
    },
    // 从 this.data.addressList.buildings 中筛选出所有 value 属性与给定校区Value 前 9 个字符相匹配的建筑物，并返回这些建筑物的 value 和 label 作为对象的数组
    _getBuildings(categoryValue) {
      return _getOptions(this.data.addressList.buildings, (building) => _compareFirstNChars(building.value, categoryValue, 9));
    },
  },

  lifetimes: {
    attached: function () {
      // 在组件实例进入页面节点树时执行
      console.log('[triple-address-pricker] 组件 attached');
      this.changeAddressNew();
    },
    detached: function () {
      // 在组件实例被从页面节点树移除时执行
    },
  },
  
  // 页面生命周期
  pageLifetimes: {
    show: function() {
      // *** 修复：页面显示时也尝试加载地址数据 ***
      console.log('[triple-address-pricker] 页面 show');
      // 如果数据为空，尝试重新加载
      if (!this.data.addressList || Object.keys(this.data.addressList.compuses || {}).length === 0) {
        console.log('[triple-address-pricker] 检测到地址数据为空，尝试加载');
        // 延迟500ms再加载，确保 token 已更新
        setTimeout(() => {
          this.changeAddressNew();
        }, 500);
      }
    },
  },
})