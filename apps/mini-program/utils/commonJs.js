/**
 * 此JS文件用于封装公共函数，以减少代码冗余
 */
import Toast from 'tdesign-miniprogram/toast/index';

const url = getApp().globalData.API_URL;

/**
 * 判断输入的字符串中是否含有 emoji 表情
 * @param {*} str - 输入
 * @returns {boolean} - 如果字符串中包含 emoji，则返回 true；否则返回 false  
 */
function containsEmoji(str) {
  const emojiRegex = /(\ud83c[\udf00-\udfff])|(\ud83d[\udc00-\ude4f\ude80-\udeff])|[\u2600-\u2B55]/g;
  return emojiRegex.test(str);
};

/**
 * 一行的简易提示，图标为error-circle(一个感叹号！)
 * @param {context} context - 显式地传入当前上下文 
 * @param {*} title - 提示的文案
 */
function errorCilcleToast(context, title) {
  return Toast({
    context: context,
    selector: '#t-toast',
    message: title,
    icon: 'error-circle',
  })
};

/**
 * 一行的简易提示，图标为check-circle(一个对号√)
 * @param {context} context - 显式地传入当前上下文 
 * @param {*} title - 提示的文案
 */
function checkCilcleToast(context, title) {
  return Toast({
    context: context,
    selector: '#t-toast',
    message: title,
    icon: 'check-circle',
  })
};

/**
 * 两行（上图标下文案）的警告提示，有遮罩
 * @param {context} context - 显式地传入当前上下文 
 * @param {str} title - 提示的文案
 */
function showWarningToast(context, title) {
  Toast({
    context: context,
    selector: '#t-toast',
    message: title,
    theme: 'warning',
    direction: 'column',
    preventScrollThrough: true,
  });
};

/**
 * 两行（上图标下文案）的错误提示，有遮罩
 * @param {context} context - 显式地传入当前上下文 
 * @param {str} title - 提示的文案
 */
function showErrorToast(context, title) {
  Toast({
    context: context,
    selector: '#t-toast',
    message: title,
    theme: 'error',
    direction: 'column',
    preventScrollThrough: true,
  });
};

/**
 * 两行（上图标下文案）的成功提示，有遮罩
 * @param {context} context - 显式地传入当前上下文 
 * @param {str} title - 提示的文案
 */
function showSuccessToast(context, title) {
  Toast({
    context: context,
    selector: '#t-toast',
    message: title,
    theme: 'success',
    direction: 'column',
    preventScrollThrough: true,
  });
};

/**
 * 通过提供的 token 获取用户信息
 * @param {string} token - 用于身份验证的令牌。
 * @returns {Promise<Object, Error>} - 一个 Promise 对象，成功时解析为用户信息对象，失败时解析为 Error 对象。
 */
function _getUserInfo(token) {
  return new Promise((resolve, reject) => {
    wx.request({
      url: `${url}/api/user`,
      method: 'GET',
      header: {
        'Content-Type': 'application/json',
        'token': token,
      },
      success: (res) => {
        resolve(res.data.data);
      },
      fail: () => {
        reject(new Error("获取用户信息失败"));
      }
    });
  });
};

/**
 * 给数组中的每个元素加上 isLast 属性，判断是否为数组中的最后一个元素
 * @param {array} array - 对象数组 
 */
function _markLastItem(array) {
  return array.map((item, index, arrayCopy) => {
    return {
      ...item,
      isLast: index === arrayCopy.length - 1
    };
  });
}

/**
 * 前后端未沟通好的补充方法
 * 遍历数据，将取件和收件地址中的学校删除（删除第一个空格前的字符）
 * @param {*} array 
 */
function _delbefore1stBlank(array) {
  // 遍历数据，将取件和收件地址中的学校删除（删除第一个空格前的字符）
  array.forEach(item => {
    if (item.pickUpAddress) {
      item.pickUpAddress = item.pickUpAddress.split(' ').slice(1).join(' ');
    }
    if (item.reciveAddress) {
      item.reciveAddress = item.reciveAddress.split(' ').slice(1).join(' ');
    }
  });
  return array;
}

/**
 * 将对象的键和值转换为一个包含 value 和 label 属性的数组，并可选地应用一个过滤函数。
 * @param {Object} obj - 源对象，键将被用作 value，对应的值将被用作 label。
 * @param {Function} [filter] - 可选的过滤函数
 * @returns {Array} - 转换（过滤）后的数组
 */
function _getOptions(obj, filter) {
  const res = Object.keys(obj).map(function (key) {
    return {
      value: key,
      label: obj[key]
    };
  });
  // 可选地应用传入的过滤函数
  if (filter) {
    return res.filter(filter);
  }

  return res;
}

/**
 * 比较两个值转换为字符串后的前n个字符是否相同
 * @param {any} str1 - 第一个要转换为字符串并进行比较的值
 * @param {any} str2 - 第二个要转换为字符串并进行比较的值
 * @param {number} n - 要比较的字符数量
 * @returns {boolean} - 如果两个值的前n个字符相同，则返回true；否则返回false
 */
function _compareFirstNChars(str1, str2, n) {
  // n是一个有实际意义的数字
  if (n <= 0) return false;

  // 将两个值转换为字符串
  const string1 = str1.toString();
  const string2 = str2.toString();

  // 获取两个字符串的前n个字符（下标从0到n-1）
  const firstNCharsOfString1 = string1.slice(0, n);
  const firstNCharsOfString2 = string2.slice(0, n);

  return firstNCharsOfString1 === firstNCharsOfString2;
};

/**
 * 便捷打印出错信息，用于定位错误位置
 * @param {String} methodName 要打印错误的所在的函数 
 * @param {String} errMsg 报错信息
 */
function _logErrInfo(methodName, errMsg) {
  console.log("errCatch in " + methodName);
  console.log("errMsg is:" + errMsg);
};

/**
 * 将字符串形式(例:2025-04-18 22:13:34)的时间转换为Date
 * @param {string} dateTimeStr - 字符串格式时间
 * @returns {Date} - Date格式的时间
 */
function _parseStrDateTime(dateTimeStr) {
  const [datePart, timePart] = dateTimeStr.split(' ');
  const [year, month, day] = datePart.split('-').map(Number);
  const [hours, minutes, seconds] = timePart.split(':').map(Number);
  return new Date(year, month - 1, day, hours, minutes, seconds);
};

/**
 * 判断一个Date格式的日期是否在明天
 * @param {Date} newDate - 某个Date格式的日期
 * @returns {boolean} - 如果是明天返回true，否则返回false
 */
function _isTomorrow(newDate) {
  const now = new Date();

  // 获取当前时间的"日期部分"（当地时区）
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());

  // 获取新时间的"日期部分"（当地时区）
  const newDay = new Date(
    newDate.getFullYear(),
    newDate.getMonth(),
    newDate.getDate()
  );

  // 判断新日期是否是当前日期的下一天
  return newDay.getTime() - today.getTime() === 86400000; // 24*60*60*1000
};

/**
 * 获取Date的时分并返回字符串格式
 * @param {Date} date - Date类型时间
 * @returns {Sting} -字符串时分，例:19:30
 */
function _formatTime(date) {
  // 获取月份（getMonth() 返回 0-11，需要 +1）
  const month = date.getMonth() + 1;
  const day = date.getDate();
  const hours = date.getHours().toString().padStart(2, '0');
  const minutes = date.getMinutes().toString().padStart(2, '0');
  return `${month}月${day}日 ${hours}:${minutes}`;
};

/**
 * 根据字符串时间(例:2025-01-06 13:42:40)、该时间gap分钟后，获取字符串形式的时间(例:4月8日 15:30)
 * @param {String} timeString - 字符串形式的时间
 * @param {int} gap - 时间间隔
 * @returns {Sting} -字符串月日时分，例:4月8日 15:30
 */
function _getExpectTimeDisplay(timeString, gap) {
  const createTime = _parseStrDateTime(timeString);
  var expectTime = new Date(createTime.getTime() + gap * 60000);
  // 强制进位到下一分钟的起始点（如 15:30:30 → 15:31:00）
  if (expectTime.getSeconds() > 0 || expectTime.getMilliseconds() > 0) {
    expectTime.setMinutes(expectTime.getMinutes() + 1);
    expectTime.setSeconds(0);
    expectTime.setMilliseconds(0);
  }
  const timeStr = _formatTime(expectTime);
  return timeStr;
};

/**
 * 根据所有的school信息返回一个仅有学校名字的列表，新列表的下标对应每个学校的id
 * @param {*} addressList - 所有的school信息
 * @returns {List} - 仅有学校名字的列表
 */
function _schlsAll2schlNameOnly(addressList) {
  let List = {
    schools: {},
  };
  // 遍历addressList数组
  for (const item of addressList) {
    // 确保deleted属性为0，即未删除的学校
    if (item.deleted === 0) {
      // 使用学校的ID作为键，学校名作为值
      List.schools[item.id] = item.schoolName;
    }
  }
  return List;
}

module.exports = {
  containsEmoji: containsEmoji,
  errorCilcleToast: errorCilcleToast,
  checkCilcleToast: checkCilcleToast,
  showWarningToast: showWarningToast,
  showErrorToast: showErrorToast,
  showSuccessToast: showSuccessToast,
  _getUserInfo: _getUserInfo,
  _markLastItem: _markLastItem,
  _delbefore1stBlank: _delbefore1stBlank,
  _getOptions: _getOptions,
  _compareFirstNChars: _compareFirstNChars,
  _logErrInfo: _logErrInfo,
  _parseStrDateTime: _parseStrDateTime,
  _isTomorrow: _isTomorrow,
  _formatTime: _formatTime,
  _getExpectTimeDisplay: _getExpectTimeDisplay,
  _schlsAll2schlNameOnly: _schlsAll2schlNameOnly
}
