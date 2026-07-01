/**
 * 校园认证须知组件
 * 用于展示认证相关的法律声明和隐私政策
 */
Component({
  /**
   * 组件的属性列表
   */
  properties: {
    // 控制组件显示/隐藏
    show: {
      type: Boolean,
      value: false
    }
  },

  /**
   * 组件的初始数据
   */
  data: {
    // 组件内部状态
  },

  /**
   * 组件的方法列表
   */
  methods: {
    /**
     * 用户点击"同意，下一步"按钮
     */
    onAgree() {
      // 触发同意事件，通知父组件
      this.triggerEvent('agree');
    },

    /**
     * 用户点击"不同意，返回"按钮
     */
    onDisagree() {
      // 触发不同意事件，通知父组件
      this.triggerEvent('disagree');
    },

    /**
     * 打开隐私保护政策
     */
    openPrivacyContract() {
      wx.openPrivacyContract({
        success: res => {
          console.log('打开隐私协议成功');
        },
        fail: res => {
          console.error('打开隐私协议失败', res);
          wx.showToast({
            title: '打开隐私协议失败',
            icon: 'none',
            duration: 2000
          });
        }
      });
    },

    /**
     * 阻止遮罩层点击事件冒泡
     */
    preventTap() {
      // 空函数，用于阻止事件冒泡
    }
  }
})

