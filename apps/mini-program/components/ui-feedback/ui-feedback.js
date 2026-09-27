Component({
  properties: { customNavigation: Boolean },
  data: { top: 12 },
  lifetimes: {
    attached() {
      this.active = true;
      this.ownerPage = require('../../utils/feedback').currentPage();
      if (this.properties.customNavigation) {
        const capsule = wx.getMenuButtonBoundingClientRect();
        const windowInfo = wx.getWindowInfo ? wx.getWindowInfo() : wx.getSystemInfoSync();
        const statusBarHeight = windowInfo.statusBarHeight || 0;
        const height = Math.max(44, (capsule?.height || 32) + Math.max(0, (capsule?.top || statusBarHeight) - statusBarHeight) * 2);
        this.setData({ top: statusBarHeight + height + 12 });
      }
    },
    ready() {
      this.feedbackReady = true;
      require('../../utils/feedback').flushPending(this.ownerPage);
    },
    detached() { this.cleanup(); },
  },
  pageLifetimes: {
    hide() { this.cleanup(); },
    show() {
      this.active = true;
      if (this.notice?.persistent) {
        const notice = this.notice;
        wx.nextTick(() => {
          if (!this.active || notice !== this.notice) return;
          const feedback = require('../../utils/feedback');
          feedback.showMessage(this.ownerPage, notice.content, notice);
        });
      }
    },
  },
  methods: {
    cleanup() {
      this.active = false;
      clearTimeout(this.messageTimer);
      this.messageResetUntil = Date.now() + 500;
      if (this.ownerPage?.selectComponent('#ui-feedback') === this) {
        require('../../utils/feedback').flushPending(this.ownerPage, true);
      }
      this.lastText = '';
      this.selectComponent('#feedback-toast')?.hide();
      this.selectComponent('#feedback-message')?.hide();
      const dialog = this.selectComponent('#feedback-dialog');
      this.cancelDialog?.();
      dialog?.close();
      if (dialog) { dialog._onConfirm = null; dialog._onCancel = null; }
      this.dialogOpen = false;
      if (!this.notice?.persistent) this.notice = null;
    },
    onAction() { this.notice?.onAction?.(); },
  },
});
