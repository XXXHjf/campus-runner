import Toast from 'tdesign-miniprogram/toast/index';

// 引入服务和工具
const userOrderService = require('../../../../services/userOrderService');
const addressService = require('../../../../services/addressService');
const userService = require('../../../../services/userService');
const configService = require('../../../../services/configService');
const mediaService = require('../../../../services/mediaService');
const tokenManager = require('../../../../utils/tokenManager');
const { maskPhone } = require('../../../../utils/privacy');
const { showLoading, hideLoading, showError } = require('../../../../utils/transformers');
const {
  containsEmoji,
  checkCilcleToast,
  errorCilcleToast,
  showWarningToast,
  showErrorToast
} = require('../../../../utils/commonJs');

// 引入新的常量和工具
const {
  ORDER_STEPS,
  STEP_TITLES,
  DOOR_ACCESS,
  DOOR_OPTIONS,
  PRICE_MODE,
  ADDRESS_TYPE,
  DEFAULT_VALUES,
  VALIDATION_MESSAGES,
  CACHE_KEYS,
  BUTTON_THEME,
  SUBSCRIBE_TEMPLATE_IDS
} = require('../../../../utils/orderConstants');
const {
  isValidPhone,
  validateBasicInfo,
  validateContent,
  validateServiceInfo
} = require('../../../../utils/validators');
const {
  saveDraft,
  loadDraft,
  clearDraft,
  hasDraft,
  restoreFromDraft,
  createDebouncedSave
} = require('../../../../utils/orderDraftManager');

const url = getApp().globalData.API_URL;

Page({
  data: {
    showConfirm: true,
    confirmBtn: {
      content: '我已知晓',
      variant: 'base'
    },
    userInfo: null,
    // 步骤条
    count: ORDER_STEPS.BASIC_INFO,
    title: STEP_TITLES,
    // 地址
    pickUpList: {},
    reciveList: {},
    showPickUp: null,
    showRecive: null,
    // 联系方式
    showUser: null,
    showPhone: null,
    maskedShowPhone: '',
    // 订单类型
    category: {},
    showCategory: {},
    // 门禁情况
    door: DOOR_OPTIONS,
    doorAccess: DOOR_ACCESS.NO_GUARD,
    // 文字说明
    note: null,
    noteTitle: '',
    noteDetail: '',
    // 图片说明
    fileList: [],
    image: null,
    imageAssetId: null,
    // 自动取消
    showCancel: '',
    value: '12:00:00',
    cancelTime: null,
    visible: false,
    // 任务时间
    gapError: false,
    gap: DEFAULT_VALUES.GAP_MINUTES, // 单位：分钟
    showGap: DEFAULT_VALUES.GAP_MINUTES, // 单位：分钟
    // 跑腿价格
    price: DEFAULT_VALUES.PRICE,
    priceError: false,
    priceAccess: PRICE_MODE.FREE,
    serviceFeeRate: null,
    serviceFeeMin: null,
    serviceFeeRatePercent: '--',
    serviceFee: null,
    payAmount: null,
    feeConfigLoaded: false,
    // 按钮样式
    theme: BUTTON_THEME.DEFAULT.theme,
    variant: BUTTON_THEME.DEFAULT.variant,
    addrList: [],
    // 成功发布后的订单ID
    orderID: null,
    // 预支付ID
    wxpayPrepayID: null,

    showReachTime: "",
    isReachTimeVisiable: false, // 控制"选择送达时间"弹出层的可见与否

    gapReach: null, // 预期送达时间(原来的任务时间)

    activeTab: 'today', // 当前选中tab
    selectedTime: null, // 选中的时间戳
    todayLabel: '', // 今日日期显示（如"周一"）
    tomorrowLabel: '', // 明日日期显示
    todayTimes: [], // 今日可选时间列表
    tomorrowTimes: [], // 明日可选时间列表
    
    // 发布确认弹窗
    showOrderConfirm: false,
    
    // 草稿自动保存
    debouncedSaveDraft: null, // 防抖保存函数
    hasLoadedDraft: false // 是否已加载过草稿
  },

  _roundMoney(value) {
    return Math.round((Number(value) + Number.EPSILON) * 100) / 100;
  },

  _formatPercent(value) {
    return `${(Number(value) * 100).toFixed(2)}%`;
  },

  _calculateFeePreview() {
    const { priceAccess, price, serviceFeeRate, serviceFeeMin } = this.data;

    if (priceAccess !== PRICE_MODE.PAID) {
      this.setData({
        serviceFee: null,
        payAmount: null
      });
      return;
    }

    const basePrice = Number(price);
    const rate = Number(serviceFeeRate);
    const minFee = Number(serviceFeeMin);

    if (!basePrice || isNaN(basePrice) || isNaN(rate) || isNaN(minFee)) {
      this.setData({
        serviceFee: null,
        payAmount: null
      });
      return;
    }

    const serviceFee = this._roundMoney(Math.max(basePrice * rate, minFee));
    const payAmount = this._roundMoney(basePrice + serviceFee);

    this.setData({
      serviceFee,
      payAmount
    });
  },

  async _loadFeeConfig() {
    try {
      const [serviceFeeRate, serviceFeeMin] = await Promise.all([
        configService.getServiceFeeRate(),
        configService.getServiceFeeMin()
      ]);

      if (isNaN(serviceFeeRate) || isNaN(serviceFeeMin)) {
        throw new Error('服务费配置解析失败');
      }

      this.setData({
        serviceFeeRate,
        serviceFeeMin,
        serviceFeeRatePercent: this._formatPercent(serviceFeeRate),
        feeConfigLoaded: true
      });

      this._calculateFeePreview();
    } catch (error) {
      console.error('[服务费配置] 加载失败:', error);
      this.setData({ feeConfigLoaded: false });
      showWarningToast(this, '服务费配置加载失败，请稍后重试');
    }
  },
  
  // 初始化防抖保存函数
  _initDraftAutoSave() {
    this.debouncedSaveDraft = createDebouncedSave(1000);
  },
  
  // 保存当前表单数据为草稿
  _saveDraftData() {
    if (!this.debouncedSaveDraft) return;
    
    const draftData = {
      showPickUp: this.data.showPickUp,
      showRecive: this.data.showRecive,
      showUser: this.data.showUser,
      showPhone: this.data.showPhone,
      noteTitle: this.data.noteTitle,
      noteDetail: this.data.noteDetail,
      showCategory: this.data.showCategory,
      doorAccess: this.data.doorAccess,
      note: this.data.note,
      fileList: this.data.fileList,
      image: this.data.image,
      imageAssetId: this.data.imageAssetId,
      price: this.data.price,
      priceAccess: this.data.priceAccess,
      count: this.data.count
    };
    
    this.debouncedSaveDraft(draftData);
  },
  
  // 尝试从草稿恢复数据
  _tryRestoreDraft() {
    if (this.data.hasLoadedDraft) return false;
    
    const draft = loadDraft();
    if (!draft) return false;
    
    // 询问用户是否恢复草稿
    wx.showModal({
      title: '发现未完成的订单',
      content: '是否恢复上次填写的内容？',
      confirmText: '恢复',
      cancelText: '重新填写',
      success: (res) => {
        if (res.confirm) {
          const restored = restoreFromDraft(draft);
          const noteParts = String(restored.note || '').split(/\r?\n/);
          let noteTitle = restored.noteTitle || noteParts.shift() || '';
          let noteDetail = restored.noteDetail || noteParts.join('\n');
          if (!restored.noteTitle && noteTitle.length > 30) {
            noteDetail = `${noteTitle.slice(30)}${noteDetail}`.slice(0, 69);
            noteTitle = noteTitle.slice(0, 30);
          }
          noteTitle = String(noteTitle).slice(0, 30);
          noteDetail = String(noteDetail).slice(0, 69);
          this.setData({
            ...restored,
            noteTitle,
            noteDetail,
            note: this._composeNote(noteTitle, noteDetail),
            maskedShowPhone: maskPhone(restored.showPhone),
            hasLoadedDraft: true
          }, () => {
            this.buttonColor();
          });
          checkCilcleToast(this, '已恢复草稿');
        } else {
          if (draft.imageAssetId) {
            mediaService.releaseTemporaryImage(draft.imageAssetId).catch(() => {});
          }
          clearDraft();
          this.setData({ hasLoadedDraft: true });
        }
      }
    });
    
    return true;
  },
  _setDateLabels() {
    const days = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
    const today = new Date();
    const tomorrow = new Date(today.getTime() + 86400000); // +1天

    this.setData({
      todayLabel: days[today.getDay()],
      tomorrowLabel: days[tomorrow.getDay()]
    });
  },
  // 生成时间列表
  async _generateTimeSlots() {
    // 生成今日时间（仅当前时间之后的准点）
    const todayTimes = await this._generateTodayTimes();

    // 生成明日时间（全天准点）
    const tomorrowTimes = await this._generateTomorrowTimes();

    // 标记推荐时间（最早的3个时间段标记为"推荐"）
    const markRecommended = (times) => {
      return times.map((time, index) => ({
        ...time,
        isRecommended: index < 3,
        recommendLabel: index === 0 ? '最早' : (index < 3 ? '推荐' : '')
      }));
    };
    
    const todayTimesWithLabel = markRecommended(todayTimes);
    const tomorrowTimesWithLabel = markRecommended(tomorrowTimes);

    // 如果今天已经没有可选时间，则只显示明天
    var defaultTimeList = todayTimesWithLabel.length > 0 ? todayTimesWithLabel : tomorrowTimesWithLabel;
    var day = todayTimesWithLabel.length > 0 ? "今天 " : "明天 ";
    
    if (todayTimesWithLabel.length <= 0) {
      this.setData({
        activeTab: 'tomorrow'
      });
    }
    
    // 生成后将默认最近的整点作为显示时间
    var timeDifference = defaultTimeList[0].time - Date.now();
    var minutesDifference = Math.floor(timeDifference / (1000 * 60));
    this.setData({
      showReachTime: day + defaultTimeList[0].displayTime,
      gapReach: minutesDifference
    });

    this.setData({
      todayTimes: todayTimesWithLabel,
      tomorrowTimes: tomorrowTimesWithLabel
    });
  },
  // 生成今日时间
  _generateTodayTimes() {
    return new Promise((resolve, reject) => {
      try {
        const now = new Date();
        const currentHour = now.getHours();
        const currentMinute = now.getMinutes();
        const today = now.getDate(); // 记录今天的日期

        // 计算起始时间（下一个半小时刻度）
        let startHour = currentHour;
        let startMinute = currentMinute < 30 ? 30 : 0;

        // 若当前分钟超过30分钟，跳至下一小时
        if (currentMinute >= 30) {
          startHour += 1;
        }

        // 从当前时间开始（不设置最早限制）
        const baseTime = new Date(now);
        baseTime.setHours(startHour, startMinute, 0, 0);

        const times = [];
        while (true) {
          const timeValue = baseTime.getTime();
          const displayHour = baseTime.getHours();
          const displayMinute = baseTime.getMinutes().toString().padStart(2, '0');

          // 如果日期变化了（到了第二天）或超过23:30，停止生成
          if (baseTime.getDate() !== today) break;
          if (displayHour === 23 && displayMinute > '30') break;

          times.push({
            time: timeValue,
            displayTime: `${displayHour}:${displayMinute}`
          });

          // 每次增加30分钟
          baseTime.setTime(timeValue + 1800000); // 30分钟 = 30*60*1000
        }

        resolve(times);
      } catch {
        reject(new Error('今日时间获取失败'))
      }
    })
  },
  // 生成明日时间
  _generateTomorrowTimes() {
    return new Promise((resolve, reject) => {
      try {
        // 创建基准时间为明日0点
        const baseDate = new Date();
        baseDate.setDate(baseDate.getDate() + 1);
        baseDate.setHours(0, 0, 0, 0);

        // 起始时间为明日6:00
        const startTime = new Date(baseDate);
        startTime.setHours(6, 0, 0);

        // 截止时间为明日23:30（24点前最后一个时间段）
        const endTime = new Date(baseDate);
        endTime.setHours(23, 30, 0);

        const times = [];
        let currentTime = new Date(startTime);

        // 循环生成每半小时段
        while (currentTime <= endTime) {
          const hour = currentTime.getHours();
          const minute = currentTime.getMinutes().toString().padStart(2, '0');

          times.push({
            time: currentTime.getTime(),
            displayTime: `${hour}:${minute}`
          });

          // 增加30分钟
          currentTime.setTime(currentTime.getTime() + 1800000); // 30分钟
        }
        resolve(times);
      } catch {
        reject(new Error('明日时间获取失败'))
      }
    })
  },
  // 切换选项卡
  switchTab(e) {
    const tab = e.currentTarget.dataset.tab;
    this.setData({
      activeTab: tab
    });
  },
  // 选择送达时间
  selectTime(e) {
    const selectedTime = e.currentTarget.dataset.time.time;
    var date = "今天"
    if (this.data.activeTab == "today")
      date = "今天 "
    else
      date = "明天 "
    var timeDifference = selectedTime - Date.now();
    var minutesDifference = Math.floor(timeDifference / (1000 * 60));
    console.log(minutesDifference);
    this.setData({
      selectedTime: selectedTime,
      gapReach: minutesDifference,
      isReachTimeVisiable: false,
      showReachTime: date + e.currentTarget.dataset.time.displayTime
    });
  },
  // 接单提示
  closeConfirm() {
    this.setData({
      showConfirm: false,
    })
  },
  // 步骤条 - 下一步
  toNext() {
    const { count } = this.data;
    let validation;
    
    // 根据当前步骤进行验证
    if (count === ORDER_STEPS.BASIC_INFO) {
      validation = validateBasicInfo(this.data);
    } else if (count === ORDER_STEPS.CONTENT) {
      validation = validateContent(this.data);
    }
    
    // 如果验证失败，显示错误消息
    if (!validation.valid) {
      errorCilcleToast(this, validation.message);
      return;
    }
    
    // 验证通过，进入下一步
    const nextStep = count + 1;
    const buttonStyle = nextStep === ORDER_STEPS.SERVICE ? BUTTON_THEME.PRIMARY : BUTTON_THEME.DEFAULT;
    
    this.setData({
      count: nextStep,
      theme: buttonStyle.theme,
      variant: buttonStyle.variant
    });
    
    // 保存草稿
    this._saveDraftData();
  },
  // 步骤条 - 上一步
  toPrev() {
    const { count } = this.data;
    if (count <= ORDER_STEPS.BASIC_INFO) return;

    this.setData({
      count: count - 1
    });
    this.buttonColor();
    this._saveDraftData();
  },
  onCascader(e) {
    const {
      current
    } = e.detail;
    this.setData({
      count: current + 1,
    });
    this.buttonColor();
  },
  // 按钮样式 - 根据表单验证状态动态更新
  buttonColor() {
    const { count } = this.data;
    let isValid = false;
    
    // 根据当前步骤判断表单是否有效
    if (count === ORDER_STEPS.BASIC_INFO) {
      isValid = validateBasicInfo(this.data).valid;
    } else if (count === ORDER_STEPS.CONTENT) {
      isValid = validateContent(this.data).valid;
    }
    
    // 设置按钮样式
    const buttonStyle = isValid ? BUTTON_THEME.PRIMARY : BUTTON_THEME.DEFAULT;
    this.setData({
      theme: buttonStyle.theme,
      variant: buttonStyle.variant
    });
  },
  // 获取我的所有地址（使用封装的 service）
  async _getAddress() {
    try {
      return await addressService.getAllAddresses();
    } catch (error) {
      console.error('获取地址失败:', error);
      throw error;
    }
  },

  // ============ 地址缓存管理 ============
  
  // 保存上次使用的地址到本地缓存
  saveLastUsedAddress(pickUpId, reciveId) {
    try {
      const data = {
        pickUpId: pickUpId,
        reciveId: reciveId,
        timestamp: Date.now()
      };
      wx.setStorageSync(CACHE_KEYS.LAST_ADDRESS, data);
      console.log('[地址缓存] 保存成功:', data);
    } catch (error) {
      console.error('[地址缓存] 保存失败:', error);
    }
  },
  
  // 从本地缓存读取上次使用的地址
  getLastUsedAddress() {
    try {
      const data = wx.getStorageSync(CACHE_KEYS.LAST_ADDRESS);
      if (data) {
        console.log('[地址缓存] 读取成功:', data);
        return data;
      }
      console.log('[地址缓存] 无缓存记录');
      return null;
    } catch (error) {
      console.error('[地址缓存] 读取失败:', error);
      return null;
    }
  },
  
  // 根据 ID 在地址列表中查找地址
  findAddressById(addressList, id) {
    if (!addressList || !Array.isArray(addressList)) {
      return null;
    }
    return addressList.find(addr => addr.id === id);
  },
  
  // 设置默认地址（优先使用上次地址，否则为空）
  setDefaultAddress(addressList) {
    // 读取上次使用的地址
    const lastUsed = this.getLastUsedAddress();
    
    if (!lastUsed) {
      // 无缓存记录 → 显示空白，用户需要主动选择
      console.log('[设置默认地址] 无缓存记录，显示空白');
      this.setData({
        showPickUp: null,
        showRecive: null
      });
      return;
    }
    
    // 查找上次使用的地址
    const lastPickUp = this.findAddressById(addressList, lastUsed.pickUpId);
    const lastRecive = this.findAddressById(addressList, lastUsed.reciveId);
    
    if (lastPickUp && lastRecive) {
      // 找到了上次使用的地址 → 自动填充
      console.log('[设置默认地址] 使用上次地址');
      this.setData({
        showPickUp: lastPickUp,
        showRecive: lastRecive
      });
    } else {
      // 地址已被删除 → 显示空白，提示用户重新选择
      console.log('[设置默认地址] 上次地址已删除，显示空白');
      this.setData({
        showPickUp: null,
        showRecive: null
      });
    }
  },
  // 取件地址修改
  tapOnPickAddr(e) {
    this.setData({
      visiblePickUp: true
    });
  },
  onVisibleChangePickUp(e) {
    this.setData({
      visiblePickUp: e.detail.visible,
    });
  },
  cancelAndConfirmPickUp() {
    this.setData({
      visiblePickUp: false
    });
    this.buttonColor();
  },
  onChangePickUp(event) {
    const pickUp = event.currentTarget.dataset.item;
    this.setData({
      showPickUp: pickUp,
      visiblePickUp: false
    });
    this._saveDraftData(); // 保存草稿
  },
  // 收件地址修改
  upRecive(e) {
    this.setData({
      visibleRecive: true
    });
  },
  onVisibleChangeRecive(e) {
    this.setData({
      visibleRecive: e.detail.visible,
    });
  },
  cancelAndConfirmRecive() {
    this.setData({
      visibleRecive: false
    });
    this.buttonColor();
  },
  onChangeRecive(event) {
    const Recive = event.currentTarget.dataset.item;
    this.setData({
      showRecive: Recive,
      visibleRecive: false
    });
    this._saveDraftData(); // 保存草稿
  },
  // 增加新地址
  button0() {
    wx.navigateTo({
      url: '/pages/address/addressAdd/add',
    })
  },
  button1() {
    wx.navigateTo({
      url: '/pages/address/addressAdd/add',
    })
  },
  // 编辑地址
  editAddress(e) {
    const { id } = e.currentTarget.dataset;
    if (!id) {
      showErrorToast('地址已失效，请重新选择');
      return;
    }
    wx.navigateTo({
      url: `/pages/address/addressInfo/info?id=${id}`,
    })
  },
  // 下单人
  getUser() {
    const app = getApp();
    const userInfo = app.globalData.userInfo;
    if (userInfo) {
      this.setData({
        showUser: userInfo.username || this.data.userInfo?.username,
        showPhone: userInfo.phone || this.data.userInfo?.phone,
        maskedShowPhone: maskPhone(userInfo.phone || this.data.userInfo?.phone),
      })
    }
  },
  // 下单人信息修改
  upUser(e) {
    this.setData({
      visibleUser: true
    });
  },
  onVisibleChangeUser(e) {
    this.setData({
      visibleUser: e.detail.visible,
    });
  },
  cancelAndConfirmUser() {
    this.setData({
      visibleUser: false
    });
    this.buttonColor();
    this._saveDraftData();
  },
  tiptChangeShowuser(e) {
    console.log('Name值： ' + e.detail.value)
    this.setData({
      showUser: e.detail.value
    })
  },
  tiptChangePhone(e) {
    console.log('phone值： ' + e.detail.value)
    this.setData({
      showPhone: e.detail.value,
      maskedShowPhone: maskPhone(e.detail.value),
    })
  },
  // 订单类别category（使用封装的 service）
  async getCategory() {
    try {
      const category = await addressService.getCategories();
      this.setData({ category });
    } catch (error) {
      console.error('获取分类失败:', error);
      showError('加载分类失败');
    }
  },
  // 订单类别category修改
  upCategory(e) {
    this.setData({
      visibleCategory: true
    });
  },
  onVisibleChangeCategory(e) {
    this.setData({
      visibleCategory: e.detail.visible,
    });
  },
  cancelAndConfirmCategory() {
    this.setData({
      visibleCategory: false
    });
    this.buttonColor();
  },
  onChangeCategory(event) {
    const Category = event.currentTarget.dataset.item;
    this.setData({
      showCategory: Category
    });
    this._saveDraftData(); // 保存草稿
  },
  // 门禁修改
  handleChangeDoor(e) {
    const old = this.data.doorAccess;
    this.setData({
      doorAccess: (old + 1) % 2
    });
  },
  _composeNote(title = this.data.noteTitle, detail = this.data.noteDetail) {
    return [String(title || '').trim(), String(detail || '').trim()]
      .filter(Boolean)
      .join('\n');
  },
  onNoteTitleInput(e) {
    const noteTitle = e.detail.value;
    this.setData({
      noteTitle,
      note: this._composeNote(noteTitle, this.data.noteDetail),
    });
    this.buttonColor();
    this._saveDraftData();
  },
  onNoteDetailInput(e) {
    const noteDetail = e.detail.value;
    this.setData({
      noteDetail,
      note: this._composeNote(this.data.noteTitle, noteDetail),
    });
    this.buttonColor();
    this._saveDraftData();
  },
  // 图片说明
  // 添加图片
  handleAdd(e) {
    const { files } = e.detail;
    files.forEach(file => this.onUpload(file));
  },
  
  // 上传图片 - 重构版（消除代码重复）
  async onUpload(file) {
    const { fileList } = this.data;
    const fileIndex = fileList.length;
    
    // 添加加载状态的文件项
    this.setData({
      fileList: [...fileList, { ...file, status: 'loading' }]
    });
    
    try {
      // 上传图片
      const uploadResult = await mediaService.uploadImage(
        file.url,
        'ORDER_IMAGE',
        (progress) => {
          this.setData({ [`fileList[${fileIndex}].percent`]: progress });
        },
      );
      
      // 更新状态为完成
      this.setData({
        [`fileList[${fileIndex}].status`]: 'done',
        [`fileList[${fileIndex}].url`]: uploadResult.previewUrl,
        [`fileList[${fileIndex}].mediaId`]: uploadResult.mediaId,
        image: null,
        imageAssetId: uploadResult.mediaId
      });
      
      console.log('[图片上传] 上传成功');
    } catch (error) {
      console.error('[图片上传] 上传失败:', error);
      // 更新状态为失败
      this.setData({
        [`fileList[${fileIndex}].status`]: 'failed'
      });
      errorCilcleToast(this, '图片上传失败');
    }
  },
  
  handleRemove(e) {
    const {
      index
    } = e.detail;
    const {
      fileList
    } = this.data;
    const removed = fileList[index];
    if (removed && removed.mediaId) {
      mediaService.releaseTemporaryImage(removed.mediaId).catch(() => {});
    }
    fileList.splice(index, 1);
    this.setData({
      fileList,
      image: null,
      imageAssetId: null,
    });
  },
  // 自动取消时间
  showPicker() {
    this.setData({
      visible: true,
    });
  },
  hidePicker() {
    this.setData({
      visible: false,
    });
  },
  onConfirm(e) {
    const {
      value
    } = e.detail;
    console.log('确定', value);
    this.setData({
      value: value
    })
    const nowTime = new Date(); // 当前时间
    const timeParts = value.split(':'); // value获取时、分、秒
    if (timeParts.length === 3) {
      const hoursToAdd = parseInt(timeParts[0], 10);
      const minutesToAdd = parseInt(timeParts[1], 10);
      const secondsToAdd = parseInt(timeParts[2], 10);
      // 创建一个新的日期对象
      const updatedTime = new Date(nowTime.getTime());
      updatedTime.setHours(nowTime.getHours() + hoursToAdd);
      updatedTime.setMinutes(nowTime.getMinutes() + minutesToAdd);
      updatedTime.setSeconds(nowTime.getSeconds() + secondsToAdd);
      this.setData({
        cancelTime: this.formatDateTime(updatedTime),
      })
    } else {
      console.log('Invalid time format');
    }
    this.hidePicker();
  },
  onColumnChange(e) {
    console.log('选择', e.detail.value);
  },
  formatDateTime(date) {
    const year = date.getFullYear();
    const month = (date.getMonth() + 1).toString().padStart(2, '0');
    const day = date.getDate().toString().padStart(2, '0');
    const hours = date.getHours().toString().padStart(2, '0');
    const minutes = date.getMinutes().toString().padStart(2, '0');
    const seconds = date.getSeconds().toString().padStart(2, '0');
    this.setData({
      showCancel: `将于 ${year}-${month}-${day} ${hours}时${minutes}分 取消`,
    })
    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
  },
  // 设置默认的取消时间为24h
  _setDefaultCancelTime() {
    const value = '24:00:00'
    const nowTime = new Date(); // 当前时间
    const timeParts = value.split(':'); // value获取时、分、秒
    if (timeParts.length === 3) {
      const hoursToAdd = parseInt(timeParts[0], 10);
      const minutesToAdd = parseInt(timeParts[1], 10);
      const secondsToAdd = parseInt(timeParts[2], 10);
      // 创建一个新的日期对象
      const updatedTime = new Date(nowTime.getTime());
      updatedTime.setHours(nowTime.getHours() + hoursToAdd);
      updatedTime.setMinutes(nowTime.getMinutes() + minutesToAdd);
      updatedTime.setSeconds(nowTime.getSeconds() + secondsToAdd);
      // console.log('Current Time:', nowTime);
      // console.log('Updated Time:', updatedTime);
      this.setData({
        cancelTime: this.formatDateTime(updatedTime),
      })
    }
  },
  // 任务时间
  onGapInput(e) {
    const {
      gapError
    } = this.data;
    const isNumber = /^\d+(\.\d+)?$/.test(e.detail.value);
    if (gapError === isNumber) {
      this.setData({
        gapError: !isNumber,
      });
    }
    if (e.detail.value > 120) {
      this.setData({
        gapError: true,
      });
    }
    console.log('gap值： ' + e.detail.value)
    this.setData({
      showGap: e.detail.value
    })
  },
  // 价格获取与检查
  handleChangePrice(e) {
    const old = this.data.priceAccess;
    const nextPriceAccess = (old + 1) % 2;
    this.setData({
      priceAccess: nextPriceAccess
    });
    if (nextPriceAccess == PRICE_MODE.FREE) {
      this.setData({
        price: 5,
        priceError: false,
      });
    }
    this._calculateFeePreview();
  },
  onPriceInput(e) {
    const value = e.detail.value;
    // 正则表达式判断价格是否正确，禁止前导零 + 最多两位小数
    const isNumber = /^(0|(?!0)\d+)(\.\d{1,2})?$/.test(e.detail.value);
    // 实时更新错误状态
    this.setData({
      priceError: !isNumber,
      price: value
    });
    this._calculateFeePreview();
    console.log('￥输入的有偿值: ', e.detail.value)
  },
  // 发布订单 - 验证并显示确认弹窗
  async order() {
    // 数据预处理
    this._prepareOrderData();

    if (this.data.priceAccess === PRICE_MODE.PAID && !this.data.feeConfigLoaded) {
      errorCilcleToast(this, '服务费配置加载失败，请稍后重试');
      return;
    }
    
    // 验证所有步骤的数据
    const validations = [
      validateBasicInfo(this.data),
      validateContent(this.data),
      validateServiceInfo(this.data)
    ];
    
    // 查找第一个验证失败的项
    for (const validation of validations) {
      if (!validation.valid) {
        errorCilcleToast(this, validation.message);
        return;
      }
    }
    
    // 所有验证通过，显示确认弹窗
    this.setData({
      showOrderConfirm: true
    });
  },
  
  // 订单数据预处理
  _prepareOrderData() {
    this.setData({
      note: this._composeNote(),
    });

    // 处理价格：免费模式下价格设为 null
    if (this.data.priceAccess === PRICE_MODE.FREE || this.data.price == 0) {
      this.setData({
        price: null,
        serviceFee: null,
        payAmount: null
      });
    } else {
      this._calculateFeePreview();
    }
    
    // 处理任务时间
    if (this.data.showGap != null && !this.data.gapError) {
      this.setData({ gap: this.data.showGap });
    }
    
    // 处理自动取消时间
    if (this.data.cancelTime == null) {
      this._setDefaultCancelTime();
    }
  },
  // 取消发布订单
  cancelOrder() {
    this.setData({
      showOrderConfirm: false
    });
  },
  // 确认发布订单
  async confirmOrder() {
    this.setData({
      showOrderConfirm: false
    });
    
    try {
      // 请求订阅消息权限
      await this._getSubscribeMessage();
      console.log('[订单发布] 订阅消息权限获取成功');
      
      // 发布订单
      await this._apiPostOrder();
    } catch (err) {
      console.error('[订单发布] 失败:', err);
      errorCilcleToast(this, err.message || '发布订单失败');
    }
  },
  // 调用后端接口 发布订单（使用封装的 service）
  async _apiPostOrder() {
    showLoading('发布中');
    
    try {
      const isPaidOrder = this.data.price !== null && this.data.price !== undefined;

      // 构建订单数据
      const orderData = {
        pickUpAddress: this.data.showPickUp.id,
        reciveAddress: this.data.showRecive.id,
        categoryId: this.data.showCategory.id,
        username: this.data.showUser,
        phone: this.data.showPhone,
        doorAccess: this.data.doorAccess,
        note: this.data.note,
        image: this.data.image,
        imageAssetId: this.data.imageAssetId,
        cancelTime: this.data.cancelTime,
        gap: this.data.gapReach,
        price: isPaidOrder ? Number(this.data.price) : null,
        serviceFeeRate: isPaidOrder ? Number(this.data.serviceFeeRate) : 0,
        serviceFee: isPaidOrder ? Number(this.data.serviceFee) : 0,
        payAmount: isPaidOrder ? Number(this.data.payAmount) : 0
      };

      console.log("此次发布订单的信息", orderData);

      // 创建订单
      const result = await userOrderService.createOrder(orderData);
      const orderID = result.data.id;
      
      hideLoading();
      checkCilcleToast(this, '下单成功');
      
      // 保存本次使用的地址到缓存（用于下次自动填充）
      this.saveLastUsedAddress(this.data.showPickUp.id, this.data.showRecive.id);
      
      // 清除订单草稿（发布成功后）
      clearDraft();

      // 如选择有偿，处理微信支付
      if (this.data.price) {
        await this._handlePayment(orderID);
      } else {
        // 免费订单，直接跳转
        this._redirectToHome();
      }
    } catch (error) {
      hideLoading();
      console.error('[订单发布] 创建订单失败:', error);
      showError('创建订单失败');
      throw error;
    }
  },
  
  // 处理支付流程（提取为独立函数）
  async _handlePayment(orderID) {
    try {
      showLoading('发起支付');
      if (getApp().globalData.MOCK_PAYMENT) {
        await userOrderService.mockPaySuccess(orderID);
        hideLoading();
        checkCilcleToast(this, '模拟支付成功');
        this._redirectToHome();
        return;
      }
      const transactionRes = await this._apiPostTransaction(orderID);
      await this._requestRegister(transactionRes);
      let synced = true;
      try {
        await this._syncPayStatus(orderID);
      } catch (syncError) {
        synced = false;
        console.error('[订单支付] 支付状态同步失败:', syncError);
      }
      
      hideLoading();
      checkCilcleToast(this, '发布成功');
      if (!synced) {
        showWarningToast(this, '支付成功，状态同步稍后刷新');
      }
      this._redirectToHome();
    } catch (error) {
      hideLoading();
      console.error('[订单支付] 支付失败:', error);
      showError('支付失败，订单已创建');
      // 支付失败但订单已创建，延迟跳转到订单列表
      setTimeout(() => {
        wx.reLaunch({ url: '/pages/orders/myOrders/ordersShow/show' });
      }, 2000);
    }
  },

  async _syncPayStatus(orderID, retryTimes = 3) {
    let lastError = null;
    for (let index = 0; index < retryTimes; index += 1) {
      try {
        return await userOrderService.syncPayStatus(orderID);
      } catch (error) {
        lastError = error;
        if (index < retryTimes - 1) {
          await new Promise((resolve) => setTimeout(resolve, 1000));
        }
      }
    }
    throw lastError;
  },
  
  // 跳转到首页
  _redirectToHome() {
    setTimeout(() => {
      wx.reLaunch({ url: '/pages/index/index' });
    }, 1500);
  },
  // 微信支付发单，返回prepayID、签名值等
  _apiPostTransaction(orderID) {
    return new Promise((resolve, reject) => {
      wx.request({
        url: `${url}/api/wx-pay/jspai/${orderID}`,
        method: 'POST',
        header: {
          'token': tokenManager.getToken()
        },
        success: (res) => {
          resolve(res.data.data);
        },
        fail: (err) => {
          reject(new Error("_apiPostTransaction failed: " + err.message));
        }
      });
    })
  },
  // 调起微信支付收银台
  _requestRegister(res) {
    return new Promise((resolve, reject) => {
      wx.requestPayment({
        timeStamp: res.timeStamp,
        nonceStr: res.nonceStr,
        package: "prepay_id=" + res.prepayId,
        signType: res.signType,
        paySign: res.paySign,
        success: (res) => {
          resolve(res);
        },
        fail: (err) => {
          reject(new Error("requestPayment failed" + err.message));
        }
      })
    })
  },
  // wx.requestSubscribeMessage 获取订阅的消息通知权限
  _getSubscribeMessage() {
    return new Promise((resolve, reject) => {
      wx.requestSubscribeMessage({
        tmplIds: SUBSCRIBE_TEMPLATE_IDS,
        success: (res) => {
          console.log('[订阅消息] 用户订阅结果:', res);
          resolve(res);
        },
        fail: (err) => {
          console.warn('[订阅消息] 用户拒绝订阅:', err);
          // 用户拒绝订阅不应该影响订单发布流程
          resolve({ errMsg: 'requestSubscribeMessage:ok' });
        }
      });
    });
  },
  // 获取用户数据（使用封装的 service）
  async getGlobalData() {
    try {
      await tokenManager.waitForToken();
      
      const app = getApp();
      const isLogin = (app.globalData.userInfo != null);
      console.log("登陆状态: " + isLogin);
      
      if (!isLogin) {
        const notLoginError = new Error("未登录账号");
        notLoginError.type = 'NOT_LOGIN';
        throw notLoginError;
      }

      const userInfo = await userService.getUserInfo();
      
      if (userInfo.authentication == 0) {
        const notIdentifyError = new Error("个人信息未认证");
        notIdentifyError.type = 'NOT_IDENTIFY';
        throw notIdentifyError;
      }
      
      console.log('获取用户数据:', userInfo);
      return userInfo;
    } catch (error) {
      console.error('获取用户数据失败:', error);
      throw error;
    }
  },
  // 清除所有页面数据
  onUnload: function () {
    let pages = getCurrentPages().length - 1;
    console.log('需要销毁的页面：' + pages);
    wx.navigateBack({
      delta: pages
    })
  },

  // 生命周期函数--监听页面加载
  onLoad() {
    console.log('[生命周期] onLoad - 页面加载');
    
    // 初始化日期标签
    this._setDateLabels();
    // 生成时间列表
    this._generateTimeSlots();
    // 初始化草稿自动保存
    this._initDraftAutoSave();
  },
  
  // 生命周期函数--监听页面显示
  async onShow() {
    console.log('[生命周期] onShow - 页面显示');
    
    try {
      // 等待 token 就绪
      await tokenManager.waitForToken();
      
      // 获取并验证用户信息
      const userInfo = await this._checkUserAuth();
      if (!userInfo) return; // 未登录或未认证，已处理跳转

      if (!this.data.feeConfigLoaded) {
        await this._loadFeeConfig();
      }
      
      // 只在首次加载时获取地址和分类（性能优化）
      const isFirstLoad = !this.data.addrList || this.data.addrList.length === 0;
      
      if (isFirstLoad) {
        await this._loadInitialData();
        // 尝试恢复草稿（仅首次加载时）
        this._tryRestoreDraft();
        this._calculateFeePreview();
      } else {
        // 非首次加载，只刷新地址列表（可能从地址管理页返回）
        await this._refreshAddressList();
        this._calculateFeePreview();
      }
      
    } catch (error) {
      console.error('[生命周期] onShow 错误:', error);
      showWarningToast(this, error.message || '页面加载失败');
    }
  },
  
  // 检查用户认证状态
  async _checkUserAuth() {
    try {
      const res = await this.getGlobalData();
      const app = getApp();
      const userInfo = {
        ...res,
        token: app.globalData.userInfo.token
      };
      
      this.setData({ userInfo });
      return userInfo;
    } catch (error) {
      // 未登录或者未认证状态，提示并转到"我的"
      if (error.type === 'NOT_LOGIN' || error.type === 'NOT_IDENTIFY') {
        showWarningToast(this, error.message);
        setTimeout(() => {
          wx.switchTab({ url: '/pages/mine/mine/mine' });
        }, 1500);
      }
      return null;
    }
  },
  
  // 加载初始数据（首次加载时）
  async _loadInitialData() {
    console.log('[数据加载] 加载初始数据');
    
    // 并行加载地址和分类（提升性能）
    const [addrList, category] = await Promise.all([
      this._getAddress(),
      this.getCategory()
    ]);
    
    // 设置地址列表
    this.setData({
      reciveList: addrList,
      pickUpList: addrList,
      addrList: addrList
    });
    
    // 设置默认地址
    if (!this.data.showPickUp && !this.data.showRecive) {
      this.setDefaultAddress(addrList);
    }
    
    // 设置用户联系方式
    if (!this.data.showUser) {
      this.getUser();
    }
  },
  
  // 刷新地址列表（从其他页面返回时）
  async _refreshAddressList() {
    console.log('[数据刷新] 刷新地址列表');
    
    try {
      const addrList = await this._getAddress();
      this.setData({
        reciveList: addrList,
        pickUpList: addrList,
        addrList: addrList
      });
      
      // 验证当前选中的地址是否仍然有效
      if (this.data.showPickUp) {
        const pickUpExists = addrList.some(addr => addr.id === this.data.showPickUp.id);
        if (!pickUpExists) {
          this.setData({ showPickUp: null });
        }
      }
      
      if (this.data.showRecive) {
        const reciveExists = addrList.some(addr => addr.id === this.data.showRecive.id);
        if (!reciveExists) {
          this.setData({ showRecive: null });
        }
      }
    } catch (error) {
      console.error('[数据刷新] 刷新地址失败:', error);
    }
  },
  // 右上角分享--好友、朋友圈
  onShareAppMessage() {
    return {
      title: '帮帮校园送',
      path: '/pages/index/index'
    }
  },
  onShareTimeline() {
    return {
      title: '帮帮校园送',
      path: '/pages/index/index'
    }
  },
  ticonTapToExplain() {
    wx.navigateTo({
      url: '/pages/help-info/help-info?tag=entranceGuard'
    });
  },

  // 预期送达时间对应事件：选择送达时间
  tapOnReachTime() {
    this.setData({
      isReachTimeVisiable: true
    })
  }
})
