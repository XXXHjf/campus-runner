/**
 * 学生证审核状态（studentIdCardReview）定义与展示辅助
 * 状态含义（后端约定）：
 * 0 未审核/未提交
 * 1 审核中
 * 2 审核通过
 * 3 审核不通过
 *
 * 注意：authentication 仍为最终认证开关（0/1），决定是否可发单/接单；
 * studentIdCardReview 仅表示“学生证审核”流程状态。
 */

const STUDENT_ID_CARD_REVIEW_STATUS = {
  UNREVIEWED: 0,
  PENDING: 1,
  APPROVED: 2,
  REJECTED: 3,
};

function isApproved(status) {
  return Number(status) === STUDENT_ID_CARD_REVIEW_STATUS.APPROVED;
}

function getStatusText(status) {
  const s = Number(status);
  if (s === STUDENT_ID_CARD_REVIEW_STATUS.APPROVED) return '审核通过';
  if (s === STUDENT_ID_CARD_REVIEW_STATUS.PENDING) return '审核中';
  if (s === STUDENT_ID_CARD_REVIEW_STATUS.REJECTED) return '未通过';
  return '未审核';
}

function getStatusTheme(status) {
  const s = Number(status);
  if (s === STUDENT_ID_CARD_REVIEW_STATUS.APPROVED) return 'primary';
  if (s === STUDENT_ID_CARD_REVIEW_STATUS.PENDING) return 'warning';
  if (s === STUDENT_ID_CARD_REVIEW_STATUS.REJECTED) return 'danger';
  return 'default';
}

function getStatusIcon(status) {
  const s = Number(status);
  if (s === STUDENT_ID_CARD_REVIEW_STATUS.APPROVED) return 'check-circle-filled';
  if (s === STUDENT_ID_CARD_REVIEW_STATUS.PENDING) return 'time';
  if (s === STUDENT_ID_CARD_REVIEW_STATUS.REJECTED) return 'error-circle-filled';
  return 'info-circle';
}

function getStatusTitle(status) {
  const s = Number(status);
  if (s === STUDENT_ID_CARD_REVIEW_STATUS.APPROVED) return '学生证审核已通过';
  if (s === STUDENT_ID_CARD_REVIEW_STATUS.PENDING) return '学生证审核中';
  if (s === STUDENT_ID_CARD_REVIEW_STATUS.REJECTED) return '学生证审核未通过';
  return '学生证审核未开始';
}

function getStatusDesc(status) {
  const s = Number(status);
  if (s === STUDENT_ID_CARD_REVIEW_STATUS.APPROVED) {
    return '若仍无法发单/接单，请稍后刷新或重新登录';
  }
  if (s === STUDENT_ID_CARD_REVIEW_STATUS.PENDING) {
    return '正在审核中。审核通过后将自动生效';
  }
  if (s === STUDENT_ID_CARD_REVIEW_STATUS.REJECTED) {
    return '审核未通过，请核对信息并重新提交材料';
  }
  return '请上传学生证/校园卡等材料，提交后等待审核';
}

module.exports = {
  STUDENT_ID_CARD_REVIEW_STATUS,
  isApproved,
  getStatusText,
  getStatusTheme,
  getStatusIcon,
  getStatusTitle,
  getStatusDesc,
};
