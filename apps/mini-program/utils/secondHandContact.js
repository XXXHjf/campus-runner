const feedback = require('./feedback');
const secondHandService = require('../services/secondHandService');
const { friendlyError } = require('./secondHandStatus');

function maskPhone(phone) {
  return /^1\d{10}$/.test(phone) ? `${phone.slice(0, 3)}****${phone.slice(7)}` : phone;
}

async function showOrderContact(orderId, roleLabel) {
  const context = feedback.currentPage();
  try {
    const order = await secondHandService.getOrderDetail(orderId);
    const phone = String(order.counterpartyPhone || '').trim();
    if (!phone) {
      feedback.showToast(context, { title: '对方暂未提供联系方式', icon: 'none' });
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
    feedback.showToast(context, { title: friendlyError(error, '暂时无法获取联系方式'), theme: 'error' });
  }
}

module.exports = { showOrderContact };
