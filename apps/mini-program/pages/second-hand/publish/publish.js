const secondHandService = require('../../../services/secondHandService');
const deliveryAddressService = require('../../../services/deliveryAddressService');
const mediaService = require('../../../services/mediaService');
const CONDITION_OPTIONS = ['全新', '几乎全新', '轻微使用', '明显使用'];

Page({
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
      images: '',
      imageAssetIds: [],
      categoryId: null,
      conditionLevel: '轻微使用',
      price: '',
      pickupAddressId: null,
      pickupAddressSnapshot: '',
      pickupLocation: '',
      pickupOnly: 1,
      supportDelivery: 0,
      negotiable: 1,
    },
  },

  async onLoad(options) {
    const id = options && options.id ? options.id : null;
    this.setData({ id, isEdit: !!id });
    wx.setNavigationBarTitle({ title: id ? '编辑闲置' : '发布闲置' });
    await Promise.all([this.loadCategories(), this.loadAddressBook()]);
    if (id) {
      await this.loadProduct(id);
    }
  },

  async onShow() {
    if (this.data.hasLoadedAddresses) {
      await this.loadAddressBook(false);
    }
  },

  async loadCategories() {
    try {
      const categories = await secondHandService.listCategories();
      this.setData({ categories });
      this.syncCategoryName();
    } catch (error) {
      wx.showToast({ title: this.errorText(error, '分类加载失败'), icon: 'none' });
    }
  },

  async loadAddressBook(showError = true) {
    try {
      const addressBook = await deliveryAddressService.getMyAddresses();
      this.setData({
        addressBook: (addressBook || []).map((item) => ({
          ...item,
          addressText: this.formatAddress(item),
          typeText: Number(item.type) === 1 ? '收件' : '取件',
        })),
        hasLoadedAddresses: true,
      });
    } catch (error) {
      if (showError) {
        wx.showToast({ title: this.errorText(error, '地址加载失败'), icon: 'none' });
      }
    }
  },

  async loadProduct(id) {
    try {
      const product = await secondHandService.getProduct(id);
      const assetIds = product.imageAssetIds || [];
      const imageList = this.parseImages(product.images).map((image, index) => ({
        url: image,
        mediaId: assetIds[index],
        temporary: false,
        name: image.split('/').pop(),
        status: 'done',
      }));
      const pickupAddressText = product.pickupAddressSnapshot || product.pickupLocation || '';
      const pickupOnly = product.pickupOnly == null
        ? (Number(product.supportDelivery) === 1 ? 0 : 1)
        : Number(product.pickupOnly);
      this.setData({
        form: {
          title: product.title || '',
          description: product.description || '',
          images: product.images || '',
          imageAssetIds: assetIds,
          categoryId: product.categoryId || null,
          conditionLevel: product.conditionLevel || '轻微使用',
          price: product.price || '',
          pickupAddressId: product.pickupAddressId || null,
          pickupAddressSnapshot: pickupAddressText,
          pickupLocation: pickupAddressText,
          pickupOnly,
          supportDelivery: pickupOnly === 1 ? 0 : 1,
          negotiable: product.negotiable == null ? 1 : product.negotiable,
        },
        pickupAddressText,
        fileList: imageList,
      });
      this.syncCategoryName();
    } catch (error) {
      wx.showToast({ title: this.errorText(error, '商品加载失败'), icon: 'none' });
    }
  },

  syncCategoryName() {
    const category = this.data.categories.find((item) => Number(item.id) === Number(this.data.form.categoryId));
    this.setData({ categoryName: category ? category.name : '' });
  },

  setField(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({ [`form.${field}`]: e.detail.value });
  },

  setSwitch(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({ [`form.${field}`]: e.detail.value ? 1 : 0 });
  },

  setPickupOnly(e) {
    const pickupOnly = e.detail.value ? 1 : 0;
    this.setData({
      'form.pickupOnly': pickupOnly,
      'form.supportDelivery': pickupOnly === 1 ? 0 : 1,
    });
  },

  chooseCategory() {
    if (!this.data.categories.length) {
      wx.showToast({ title: '暂无可选分类', icon: 'none' });
      return;
    }
    this.setData({ showCategorySheet: true });
  },

  closeCategorySheet() {
    this.setData({ showCategorySheet: false });
  },

  selectCategory(e) {
    const category = this.data.categories.find((item) => Number(item.id) === Number(e.currentTarget.dataset.id));
    if (!category) return;
    this.setData({
      'form.categoryId': category.id,
      categoryName: category.name,
      showCategorySheet: false,
    });
  },

  choosePickupAddress() {
    this.setData({ showPickupAddressSheet: true });
  },

  closePickupAddressSheet() {
    this.setData({ showPickupAddressSheet: false });
  },

  selectPickupAddress(e) {
    const address = this.data.addressBook.find((item) => Number(item.id) === Number(e.currentTarget.dataset.id));
    if (!address) return;
    this.setData({
      'form.pickupAddressId': address.id,
      'form.pickupAddressSnapshot': address.addressText,
      'form.pickupLocation': address.addressText,
      pickupAddressText: address.addressText,
      showPickupAddressSheet: false,
    });
  },

  gotoAddPickupAddress() {
    this.setData({ showPickupAddressSheet: false });
    wx.navigateTo({ url: '/pages/address/addressAdd/add?type=0' });
  },

  chooseCondition() {
    wx.showActionSheet({
      itemList: CONDITION_OPTIONS,
      success: (res) => {
        this.setData({ 'form.conditionLevel': CONDITION_OPTIONS[res.tapIndex] });
      },
    });
  },

  async handleAdd(e) {
    const files = e.detail.files || [];
    for (const file of files) {
      await this.uploadImage(file);
    }
  },

  async uploadImage(file) {
    const index = this.data.fileList.length;
    this.setData({
      uploading: true,
      fileList: [...this.data.fileList, { ...file, status: 'loading' }],
    });
    try {
      const uploaded = await mediaService.uploadImage(
        file.url,
        'SECOND_HAND_PRODUCT_IMAGE',
      );
      this.setData({
        [`fileList[${index}].url`]: uploaded.previewUrl,
        [`fileList[${index}].mediaId`]: uploaded.mediaId,
        [`fileList[${index}].temporary`]: true,
        [`fileList[${index}].status`]: 'done',
      });
      this.syncImages();
    } catch (error) {
      this.setData({ [`fileList[${index}].status`]: 'failed' });
      wx.showToast({ title: this.errorText(error, '图片上传失败'), icon: 'none' });
    } finally {
      this.setData({ uploading: false });
    }
  },

  handleRemove(e) {
    const index = e.detail.index;
    const removed = this.data.fileList[index];
    if (removed && removed.temporary && removed.mediaId) {
      mediaService.releaseTemporaryImage(removed.mediaId).catch(() => {});
    }
    const fileList = this.data.fileList.filter((_, idx) => idx !== index);
    this.setData({ fileList });
    this.syncImages();
  },

  noop() {},

  syncImages() {
    const completed = this.data.fileList.filter((file) => file.status === 'done');
    const images = completed
      .filter((file) => file.url && !file.mediaId)
      .map((file) => file.url)
      .join(',');
    const imageAssetIds = completed
      .filter((file) => file.mediaId)
      .map((file) => file.mediaId);
    this.setData({
      'form.images': images,
      'form.imageAssetIds': imageAssetIds,
    });
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

  submit() {
    const error = this.validateForm();
    if (error) {
      wx.showToast({ title: error, icon: 'none' });
      return;
    }
    const action = this.data.isEdit ? '保存修改' : '发布闲置';
    wx.showModal({
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
    this.setData({ submitting: true });
    const form = this.data.form;
    const payload = {
      ...form,
      title: form.title.trim(),
      description: form.description.trim(),
      pickupAddressSnapshot: form.pickupAddressSnapshot.trim(),
      pickupLocation: form.pickupAddressSnapshot.trim(),
      price: Number(form.price),
    };
    try {
      if (this.data.isEdit) {
        await secondHandService.updateProduct(this.data.id, payload);
      } else {
        await secondHandService.publishProduct(payload);
      }
      this.submitted = true;
      wx.showToast({ title: this.data.isEdit ? '已保存' : '已发布', icon: 'success' });
      setTimeout(() => wx.navigateBack(), 600);
    } catch (error) {
      wx.showToast({ title: this.errorText(error, '提交失败'), icon: 'none' });
    } finally {
      this.setData({ submitting: false });
    }
  },

  onUnload() {
    if (this.submitted) return;
    this.data.fileList
      .filter((file) => file.temporary && file.mediaId)
      .forEach((file) => mediaService.releaseTemporaryImage(file.mediaId).catch(() => {}));
  },

  errorText(error, fallback) {
    const message = error && error.message ? error.message : fallback;
    return message.length > 18 ? message.slice(0, 18) : message;
  },
});
