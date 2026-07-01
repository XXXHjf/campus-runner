// pages/help-info/help-info.js
Page({

  /**
   * 页面的初始数据
   */
  data: {
    tag: 'default',
    allHelpData: {
      'default' : {
        title: '默认说明'
      },
      'entranceGuard' : {
        title: '配送过程门禁说明'
      },
    }
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {
    const tag = options.tag || 'default'; // 获取跳转时传递的 tag
    this.setData({
      tag: tag
    })
    const helpType = this.data.allHelpData[tag] || this.data.allHelpData['default'];
    wx.setNavigationBarTitle({
      title: helpType.title
    })
    
  },

  /**
   * 生命周期函数--监听页面初次渲染完成
   */
  onReady() {

  },

  /**
   * 生命周期函数--监听页面显示
   */
  onShow() {

  },

  /**
   * 生命周期函数--监听页面隐藏
   */
  onHide() {

  },

  /**
   * 生命周期函数--监听页面卸载
   */
  onUnload() {

  },

  /**
   * 页面相关事件处理函数--监听用户下拉动作
   */
  onPullDownRefresh() {

  },

  /**
   * 页面上拉触底事件的处理函数
   */
  onReachBottom() {

  },

  /**
   * 用户点击右上角分享
   */
  onShareAppMessage() {

  }
})