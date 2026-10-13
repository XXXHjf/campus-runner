const { loadAddressPage } = require('../../../utils/addressPager');
const feedback = require('../../../utils/feedback');
const subscriptions = require('../../../services/subscriptionService');
const secondHandService = require('../../../services/secondHandService');
const deliveryAddressService = require('../../../services/deliveryAddressService');
const mediaService = require('../../../services/mediaService');
const accountStorage = require('../../../utils/accountStorage');
const tokenManager = require('../../../utils/tokenManager');
const drafts = require('../../../utils/secondHandDraftManager');
const CONDITION_OPTIONS = ['全新', '几乎全新', '轻微使用', '明显使用'];

Page({
  retryFeedbackLoad() { return this._feedbackRetry?.(); },
  data: {
    id: null,
    isEdit: false,
    categories: [],
    categoryName: '',
    conditionOptions: CONDITION_OPTIONS,
    addressBook: [],
    hasLoadedAddresses: false,
    pickupAddressText: '',
    fileList: [],
    uploading: false,
    submitting: false,
    showCategorySheet: false,
    showPickupAddressSheet: false,
    form: {
      title: '',
      description: '',
      imageAssetIds: [],
      categoryId: null,
      conditionLevel: '轻微使用',
      price: '',
      pickupAddressId: null,
      pickupAddressSnapshot: '',
      pickupOnly: 1,
      negotiable: 1,
    },
  },

  async onLoad(options) {
    this._initialData = JSON.parse(JSON.stringify(this.data));
    this._unsubscribeSession = tokenManager.onSessionChange(() => {
      this._draftSession = null;
      this._savedDraftImageIds = new Set();
      this._protectedImageIds = new Set();
      this._dirty = false;
      this._draftRevision = null;
      this.submitted = false;
      this._draftImageCheckFailed = false;
      this.setData(JSON.parse(JSON.stringify(this._initialData)));
    });
    await tokenManager.waitForToken();
    if (this._unloaded) return;
    const session = this._draftSession = accountStorage.captureSession();
    const id = options && options.id ? options.id : null;
    this.setData({ id, isEdit: !!id });
    wx.setNavigationBarTitle({ title: id ? '编辑闲置' : '发布闲置' });
    await drafts.waitPending(id, session);
    if (this._unloaded || !accountStorage.isCurrent(session)) return;
    await Promise.all([this.loadCategories(), this.loadAddressBook()]);
    if (this._unloaded || !accountStorage.isCurrent(session)) return;
    if (id) {
      if (!await this.loadProduct(id)) return;
    }
    if (!this._unloaded && accountStorage.isCurrent(session)) await this.restoreDraft();
  },

  saveDraft(markDirty = false) {
    if (markDirty) this._dirty = true;
    if (!this._dirty && !this._draftRevision) return false;
    if (this.submitted || !accountStorage.isCurrent(this._draftSession)) return false;
    let revision;
    try {
      revision = drafts.save(this.data.id, { form: this.data.form, fileList: this.data.fileList,
        protectedImageIds: this._protectedImageIds }, this._draftSession);
    } catch (error) {
      if (!this._unloaded) feedback.showMessage(this, '内容暂未保存，请稍后重试', { theme: 'error', key: 'draft-save' });
      return false;
    }
    if (!revision) return false;
    this._draftRevision = revision;
    this._savedDraftImageIds = new Set(this.data.fileList.filter(file => file.status === 'done').map(file => file.mediaId));
    return true;
  },

  async restoreDraft() {
    if (this._unloaded) return;
    const session = this._draftSession;
    const draft = drafts.load(this.data.id, session);
    if (!draft) return;
    const result = await feedback.showModal(this, { title: '发现未完成的内容', content: '是否恢复上次填写的内容？',
      confirmText: '恢复', cancelText: '重新填写' });
    if (this._unloaded || !accountStorage.isCurrent(session)) return;
    if (!result.confirm && !result.cancel) return;
    if (!result.confirm) {
      drafts.clear(this.data.id, session, draft.revision);
      (draft.fileList || []).filter(file => file.temporary && !(draft.protectedImageIds || []).includes(file.mediaId))
        .forEach(file => mediaService.releaseTemporaryImage(file.mediaId).catch(() => {}));
      return;
    }
    const originals = new Map(this.data.fileList.filter(file => !file.temporary).map(file => [String(file.mediaId), file]));
    this._protectedImageIds = new Set(draft.protectedImageIds || []);
    this._dirty = true;
    this._draftRevision = draft.revision;
    const fileList = (draft.fileList || []).map(file => file.temporary ? { ...file, url: '' }
      : originals.get(String(file.mediaId))).filter(Boolean);
    const lostOriginal = fileList.length !== (draft.fileList || []).length;
    this.setData({ form: { ...this.data.form, ...draft.form }, fileList,
      pickupAddressText: draft.form.pickupAddressSnapshot || '' });
    this.syncCategoryName();
    this.syncImages();
    await this.refreshDraftImages();
    if (lostOriginal && !this._unloaded && accountStorage.isCurrent(session)) {
      feedback.showMessage(this, '部分图片已变化，请重新选择', { theme: 'warning', key: 'draft-images' });
    }
  },

  async refreshDraftImages() {
    const session = this._draftSession;
    const ids = this.data.fileList.filter(file => file.temporary && file.mediaId).map(file => file.mediaId);
    if (!ids.length) { this._draftImageCheckFailed = false; return true; }
    try {
      const usable = await mediaService.usableTemporaryImages(ids, 'SECOND_HAND_PRODUCT_IMAGE');
      if (this._unloaded || !accountStorage.isCurrent(session)) return false;
      const byId = new Map(usable.map(asset => [String(asset.mediaId), asset]));
      let expired = false;
      const fileList = this.data.fileList.filter(file => {
        if (!file.temporary || !ids.includes(file.mediaId) || byId.has(String(file.mediaId))) return true;
        expired = true; return false;
      }).map(file => byId.has(String(file.mediaId)) ? { ...file, url: byId.get(String(file.mediaId)).previewUrl } : file);
      this.setData({ fileList });
      this._draftImageCheckFailed = false;
      this.syncImages();
      if (expired) feedback.showMessage(this, '部分图片已失效，请重新上传', { theme: 'warning', key: 'draft-images' });
      else feedback.clearMessage(this, 'draft-images');
      return !expired;
    } catch (error) {
      if (!this._unloaded && accountStorage.isCurrent(session)) {
        this._draftImageCheckFailed = true;
        feedback.showMessage(this, '图片暂时无法确认，请稍后重试', { theme: 'error', key: 'draft-images',
          action: '重试', onAction: () => this.refreshDraftImages() });
      }
      return false;
    }
  },

  onHide() { this.saveDraft(); },

  async onShow() {
    if (this._draftSession === null) {
      const session = this._draftSession = accountStorage.captureSession();
      if (!accountStorage.isCurrent(session)) return;
      wx.setNavigationBarTitle({ title: '发布闲置' });
      await Promise.all([this.loadCategories(), this.loadAddressBook()]);
      if (accountStorage.isCurrent(session)) await this.restoreDraft();
      return;
    }
    if (this.data.hasLoadedAddresses) {
      await this.loadAddressBook(false);
    }
  },

  async loadCategories() {
    const session = this._draftSession;
    try {
      const categories = await secondHandService.listCategories();
      if (this._unloaded || (session && !accountStorage.isCurrent(session))) return;
      this.setData({ categories });
      this.syncCategoryName();
      feedback.clearMessage(this, 'category');
    } catch (error) {
      if (this._unloaded || (session && !accountStorage.isCurrent(session))) return;
      feedback.showMessage(this, '分类加载失败，请重试', { persistent: true, key: 'category', theme: 'error', action: '重试', onAction: () => this.loadCategories() });
    }
  },

  loadMoreAddresses() { return this.loadAddressBook(true, false); },

  async loadAddressBook(showError = true, reset = true) {
    const session = this._draftSession;
    try {
      const addressBook = await loadAddressPage(this, "addressBook", deliveryAddressService.getMyAddresses, reset);
      if (!addressBook) return;
      if (this._unloaded || (session && !accountStorage.isCurrent(session))) return;
      this.setData({
        addressBook: (addressBook || []).map((item) => ({
          ...item,
          addressText: this.formatAddress(item),
        })),
        hasLoadedAddresses: true,
      });
    } catch (error) {
      if (this._unloaded || (session && !accountStorage.isCurrent(session))) return;
      if (showError) {
        feedback.showMessage(this, '地址列表加载失败，请重试', { theme: 'error', action: '重试', onAction: () => this.loadAddressBook() });
      }
    }
  },

  async loadProduct(id) {
    const session = this._draftSession;
    feedback.loaded(this);
    try {
      const product = await secondHandService.getProduct(id);
      if (this._unloaded || (session && !accountStorage.isCurrent(session))) return false;
      const assetIds = product.imageAssetIds || [];
      const imageList = this.parseImages(product.images).map((image, index) => ({
        url: image,
        mediaId: assetIds[index],
        temporary: false,
        name: image.split('/').pop(),
        status: 'done',
      }));
      const pickupAddressText = product.pickupAddressSnapshot || '';
      const pickupOnly = Number(product.pickupOnly ?? 1);
      this.setData({
        form: {
          title: product.title || '',
          description: product.description || '',
          imageAssetIds: assetIds,
          categoryId: product.categoryId || null,
          conditionLevel: product.conditionLevel || '轻微使用',
          price: product.price || '',
          pickupAddressId: product.pickupAddressId || null,
          pickupAddressSnapshot: pickupAddressText,
          pickupOnly,
          negotiable: product.negotiable == null ? 1 : product.negotiable,
        },
        pickupAddressText,
        fileList: imageList,
      });
      this.syncCategoryName();
      return true;
    } catch (error) {
      if (this._unloaded || (session && !accountStorage.isCurrent(session))) return false;
      feedback.loadError(this, '商品加载失败，请重试', () => this.loadProduct(this.data.id));
      return false;
    }
  },

  syncCategoryName() {
    const category = this.data.categories.find((item) => Number(item.id) === Number(this.data.form.categoryId));
    this.setData({ categoryName: category ? category.name : '' });
  },

  setField(e) {
    if (this.data.submitting) return;
    const field = e.currentTarget.dataset.field;
    this.setData({ [`form.${field}`]: e.detail.value });
    this.saveDraft(true);
  },

  setSwitch(e) {
    if (this.data.submitting) return;
    const field = e.currentTarget.dataset.field;
    this.setData({ [`form.${field}`]: e.detail.value ? 1 : 0 });
    this.saveDraft(true);
  },

  setPickupOnly(e) {
    if (this.data.submitting) return;
    const pickupOnly = e.detail.value ? 1 : 0;
    this.setData({ 'form.pickupOnly': pickupOnly });
    this.saveDraft(true);
  },

  chooseCategory() {
    if (!this.data.categories.length) {
      feedback.showToast(this, { title: '暂无可选分类', theme: 'warning' });
      return;
    }
    this.setData({ showCategorySheet: true });
  },

  closeCategorySheet() {
    this.setData({ showCategorySheet: false });
  },

  selectCategory(e) {
    if (this.data.submitting) return;
    const category = this.data.categories.find((item) => Number(item.id) === Number(e.currentTarget.dataset.id));
    if (!category) return;
    this.setData({
      'form.categoryId': category.id,
      categoryName: category.name,
      showCategorySheet: false,
    });
    this.saveDraft(true);
  },

  choosePickupAddress() {
    this.setData({ showPickupAddressSheet: true });
  },

  closePickupAddressSheet() {
    this.setData({ showPickupAddressSheet: false });
  },

  selectPickupAddress(e) {
    if (this.data.submitting) return;
    const address = this.data.addressBook.find((item) => Number(item.id) === Number(e.currentTarget.dataset.id));
    if (!address) return;
    this.setData({
      'form.pickupAddressId': address.id,
      'form.pickupAddressSnapshot': address.addressText,
      pickupAddressText: address.addressText,
      showPickupAddressSheet: false,
    });
    this.saveDraft(true);
  },

  gotoAddPickupAddress() {
    this.setData({ showPickupAddressSheet: false });
    wx.navigateTo({ url: '/pages/address/addressAdd/add' });
  },

  chooseCondition() {
    wx.showActionSheet({
      itemList: CONDITION_OPTIONS,
      success: (res) => {
        if (this.data.submitting || this._unloaded) return;
        this.setData({ 'form.conditionLevel': CONDITION_OPTIONS[res.tapIndex] });
        this.saveDraft(true);
      },
    });
  },

  async handleAdd(e) {
    const files = e.detail.files || [];
    for (const file of files) {
      if (this._unloaded) break;
      await this.uploadImage(file);
    }
  },

  async uploadImage(file) {
    if (this._unloaded || this.data.submitting) return;
    const uploadId = this._nextUploadId = (this._nextUploadId || 0) + 1;
    const session = this._draftSession;
    this.setData({
      uploading: true,
      fileList: [...this.data.fileList, { ...file, uploadId, status: 'loading' }],
    });
    try {
      const uploaded = await mediaService.uploadImage(
        file.url,
        'SECOND_HAND_PRODUCT_IMAGE',
      );
      // Deletion can shift indices or replace a file while its upload is pending.
      const index = this.data.fileList.findIndex((item) => item.uploadId === uploadId);
      if (session && !accountStorage.isCurrent(session)) return;
      if (this._unloaded || index === -1) {
        mediaService.releaseTemporaryImage(uploaded.mediaId).catch(() => {});
        return;
      }
      this.setData({
        [`fileList[${index}].url`]: uploaded.previewUrl,
        [`fileList[${index}].mediaId`]: uploaded.mediaId,
        [`fileList[${index}].temporary`]: true,
        [`fileList[${index}].status`]: 'done',
      });
    } catch (error) {
      const index = this.data.fileList.findIndex((item) => item.uploadId === uploadId);
      if (session && !accountStorage.isCurrent(session)) return;
      if (this._unloaded || index === -1) return;
      this.setData({ [`fileList[${index}].status`]: 'failed' });
      feedback.showToast(this, { title: this.errorText(error, '图片上传失败'), theme: 'error' });
    } finally {
      if (!this._unloaded && (!session || accountStorage.isCurrent(session))) this.syncImages();
    }
  },

  handleRemove(e) {
    if (this.data.submitting) return;
    const index = e.detail.index;
    const removed = this.data.fileList[index];
    if (removed && removed.temporary && removed.mediaId && !this._protectedImageIds?.has(removed.mediaId)) {
      mediaService.releaseTemporaryImage(removed.mediaId).catch(() => {});
    }
    const fileList = this.data.fileList.filter((_, idx) => idx !== index);
    this.setData({ fileList });
    this.syncImages();
  },

  noop() {},

  syncImages() {
    const completed = this.data.fileList.filter((file) => file.status === 'done');
    const imageAssetIds = completed
      .filter((file) => file.mediaId)
      .map((file) => file.mediaId);
    this.setData({
      'form.imageAssetIds': imageAssetIds,
      uploading: this.data.fileList.some((file) => file.status === 'loading'),
    });
    this.saveDraft(true);
  },

  parseImages(images) {
    if (!images) return [];
    return String(images).split(',').filter(Boolean);
  },

  formatAddress(address) {
    return [
      address.compusName,
      address.buildCategoryName,
      address.buildingName,
      address.details,
    ].filter(Boolean).join(' ');
  },

  validateForm() {
    const form = this.data.form;
    const title = form.title.trim();
    const description = form.description.trim();
    const price = Number(form.price);
    if (title.length < 2) return '标题至少 2 个字';
    if (!form.categoryId) return '请选择商品分类';
    if (!price || price <= 0) return '请输入有效价格';
    if (price > 99999) return '价格不能超过 99999';
    if (!this.data.fileList.some((file) => file.status === 'done')) return '请至少上传 1 张图片';
    if (description.length < 4) return '描述至少 4 个字';
    if (!form.pickupAddressSnapshot) return '请选择自提点';
    return '';
  },

  async submit() {
    if (!await require('../../../utils/accessGuard').ensureAuthenticated()) return;
    if (!this.data.isEdit && secondHandService.getPendingPublication?.()) {
      const answer = await feedback.showModal(this, { title: '确认上次发布', content: '上次发布结果暂未确认。继续重试上次发布？', confirmText: '继续重试' });
      if (answer.confirm) await this.doSubmit();
      return;
    }
    const error = this.validateForm();
    if (error) {
      feedback.showToast(this, { title: error, theme: 'warning' });
      return;
    }
    const action = this.data.isEdit ? '保存修改' : '发布闲置';
    feedback.showModal(this, {
      title: action,
      content: this.data.form.pickupOnly === 1
        ? '确认后，买家将到你设置的地点取货。'
        : '确认后，买家可选择自提，也可以填写配送地址。',
      confirmText: action,
      success: async (res) => {
        if (!res.confirm) return;
        await this.doSubmit();
      },
    });
  },

  async doSubmit() {
    if (this._unloaded || this.data.submitting || this.data.uploading) return;
    if (this._draftSession !== undefined && !accountStorage.isCurrent(this._draftSession)) return;
    this.setData({ submitting: true });
    const pending = !this.data.isEdit && secondHandService.getPendingPublication?.();
    if (!pending && this._draftSession && !await this.refreshDraftImages()) {
      if (!this._unloaded) this.setData({ submitting: false });
      return;
    }
    const form = this.data.form;
    const payload = pending || {
      ...form,
      title: form.title.trim(),
      description: form.description.trim(),
      pickupAddressSnapshot: form.pickupAddressSnapshot.trim(),
      price: Number(form.price),
    };
    payload.imageAssetIds = [...payload.imageAssetIds];
    const previouslyProtected = new Set(this._protectedImageIds || []);
    this._protectedImageIds = new Set([...previouslyProtected, ...payload.imageAssetIds]);
    const session = this._draftSession;
    const finish = session ? drafts.beginSubmission(this.data.id, session) : () => {};
    this.saveDraft(true);
    try {
      if (this.data.isEdit) {
        await secondHandService.updateProduct(this.data.id, payload);
      } else {
        await secondHandService.publishProduct(payload);
      }
      if (session && !accountStorage.isCurrent(session)) return;
      this.submitted = true;
      if (session) drafts.clear(this.data.id, session, this._draftRevision);
      if (!this.data.isEdit && !this._unloaded) await subscriptions.requestSecondHandOrder();
      if (!this._unloaded) feedback.navigate(this, 'navigateBack', {}, this.data.isEdit ? '已保存' : '已发布');
    } catch (error) {
      if (session && !accountStorage.isCurrent(session)) return;
      if (!this.submitted && error.requestOutcome === 'rejected') {
        this._protectedImageIds = previouslyProtected;
      }
      this.saveDraft();
      if (!this._unloaded && (!session || accountStorage.isCurrent(session))) feedback.showToast(this, { title: this.errorText(error, '提交失败'), theme: 'error' });
    } finally {
      if (!session || accountStorage.isCurrent(session)) {
        if (this._unloaded) this.releaseUnusedImages();
        else this.setData({ submitting: false });
      }
      finish();
    }
  },

  onUnload() {
    this.saveDraft();
    this._unloaded = true;
    this._unsubscribeSession?.();
    this.releaseUnusedImages();
  },

  releaseUnusedImages() {
    if (this.submitted) return;
    this.data.fileList
      .filter((file) => file.temporary && file.mediaId && !this._protectedImageIds?.has(file.mediaId)
        && !this._savedDraftImageIds?.has(file.mediaId))
      .forEach((file) => mediaService.releaseTemporaryImage(file.mediaId).catch(() => {}));
  },

  errorText(error, fallback) {
    const message = error && error.message ? error.message : fallback;
    return message;
  },
});
