const secondHandService = require('../services/secondHandService');
const { friendlyError } = require('./secondHandStatus');

function maskPhone(phone) {
  return /^1\d{10}$/.test(phone) ? `${phone.slice(0, 3)}****${phone.slice(7)}` : phone;
}

async function showOrderContact(orderId, roleLabel) {
  try {
    const order = await secondHandService.getOrderDetail(orderId);
    const phone = String(order.counterpartyPhone || '').trim();
    if (!phone) {
      wx.showToast({ title: '对方暂未提供联系方式', icon: 'none' });
      return;
    }
    wx.showActionSheet({
      itemList: [`拨打${roleLabel}电话 ${maskPhone(phone)}`, '复制号码'],
      success: ({ tapIndex }) => {
        if (tapIndex === 0) wx.makePhoneCall({ phoneNumber: phone });
        if (tapIndex === 1) wx.setClipboardData({ data: phone });
      },
    });
  } catch (error) {
    wx.showToast({ title: friendlyError(error, '暂时无法获取联系方式'), icon: 'none' });
  }
}

module.exports = { showOrderContact };
