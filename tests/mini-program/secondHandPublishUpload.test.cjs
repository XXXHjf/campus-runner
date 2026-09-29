const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const vm = require('node:vm');
const feedbackStub = require('./helpers/feedbackStub.cjs');
const source = fs.readFileSync(path.resolve(__dirname, '../../apps/mini-program/pages/second-hand/publish/publish.js'), 'utf8');

function deferred() {
  let resolve, reject;
  const promise = new Promise((yes, no) => { resolve = yes; reject = no; });
  return { promise, resolve, reject };
}

function harness() {
  let page;
  const state = { uploads: [], released: [], toasts: [], updates: 0, saved: [] };
  vm.runInNewContext(source, {
    Page(config) {
      page = {
        ...config,
        data: structuredClone(config.data),
        setData(update) {
          state.updates++;
          for (const [key, value] of Object.entries(update)) {
            const keys = key.replace(/\[(\d+)\]/g, '.$1').split('.');
            let target = this.data;
            for (const part of keys.slice(0, -1)) target = target[part] ||= {};
            target[keys.at(-1)] = value;
          }
        },
      };
    },
    require(id) {
      if (id.endsWith('feedback')) return feedbackStub({ showToast: (o) => state.toasts.push(o.title) });
      if (id.endsWith('mediaService')) return {
        uploadImage(url, purpose) {
          const pending = deferred();
          state.uploads.push({ url, purpose, ...pending });
          return pending.promise;
        },
        releaseTemporaryImage: async (id) => { state.released.push(id); },
      };
      if (id.endsWith('secondHandService')) return {
        publishProduct: async (payload) => { state.saved.push(payload); },
        updateProduct: async (_, payload) => { state.saved.push(payload); },
      };
      if (id.endsWith('subscriptionService')) return { requestSecondHandOrder: async () => {} };
      if (id.endsWith('deliveryAddressService')) return {};
      throw new Error(id);
    },
  });
  return { page, state };
}

const uploaded = (mediaId) => ({ mediaId, previewUrl: `https://images/${mediaId}` });
const remove = (page, index) => page.handleRemove({ detail: { index } });
const assetIds = (page) => Array.from(page.data.form.imageAssetIds);

test('deleting a pending image releases its late result without restoring it', async () => {
  const { page, state } = harness();
  const pending = page.handleAdd({ detail: { files: [{ url: 'local-a' }] } });
  assert.equal(page.data.uploading, true);
  remove(page, 0);
  state.uploads[0].resolve(uploaded(11));
  await pending;
  assert.equal(page.data.fileList.length, 0);
  assert.deepEqual(assetIds(page), []);
  assert.deepEqual(state.released, [11]);
  assert.equal(page.data.uploading, false);
});

test('reselecting the same local path is a new upload and cannot receive an old result', async () => {
  const { page, state } = harness();
  const old = page.uploadImage({ url: 'local-a' });
  remove(page, 0);
  const fresh = page.uploadImage({ url: 'local-a' });
  state.uploads[0].resolve(uploaded(11));
  await old;
  assert.equal(page.data.fileList[0].url, 'local-a');
  assert.equal(page.data.fileList[0].status, 'loading');
  assert.equal(page.data.uploading, true);
  state.uploads[1].resolve(uploaded(22));
  await fresh;
  assert.deepEqual(assetIds(page), [22]);
  assert.deepEqual(state.released, [11]);
});

for (const fails of [false, true]) {
  test(`a shifted pending image receives its own ${fails ? 'failure' : 'success'} without overwriting another image`, async () => {
    const { page, state } = harness();
    page.setData({ fileList: [{ url: 'existing', mediaId: 5, temporary: false, status: 'done' }] });
    const first = page.uploadImage({ url: 'local-a' });
    const second = page.uploadImage({ url: 'local-b' });
    remove(page, 0);
    if (fails) state.uploads[0].reject(new Error('图片上传失败'));
    else state.uploads[0].resolve(uploaded(11));
    await first;
    assert.equal(page.data.fileList.length, 2);
    assert.equal(page.data.fileList[0].status, fails ? 'failed' : 'done');
    assert.equal(page.data.fileList[1].url, 'local-b');
    assert.equal(page.data.fileList[1].status, 'loading');
    assert.equal(page.data.uploading, true);
    state.uploads[1].resolve(uploaded(22));
    await second;
    assert.deepEqual(assetIds(page), fails ? [22] : [11, 22]);
    assert.equal(page.data.uploading, false);
    assert.deepEqual(state.released, []);
  });
}

test('concurrent uploads completing out of order preserve image and asset order', async () => {
  const { page, state } = harness();
  const first = page.uploadImage({ url: 'local-a' });
  const second = page.uploadImage({ url: 'local-b' });
  state.uploads[1].resolve(uploaded(22));
  await second;
  assert.equal(page.data.uploading, true);
  assert.equal(page.data.fileList[0].status, 'loading');
  state.uploads[0].resolve(uploaded(11));
  await first;
  assert.deepEqual(assetIds(page), [11, 22]);
  assert.equal(page.data.uploading, false);
});

test('a deleted upload failing late does not mark its replacement failed or show an error', async () => {
  const { page, state } = harness();
  const old = page.uploadImage({ url: 'local-a' });
  remove(page, 0);
  const fresh = page.uploadImage({ url: 'local-b' });
  state.uploads[0].reject(new Error('late failure'));
  await old;
  assert.equal(page.data.fileList[0].status, 'loading');
  assert.equal(page.data.uploading, true);
  assert.deepEqual(state.toasts, []);
  state.uploads[1].resolve(uploaded(22));
  await fresh;
});

for (const fails of [false, true]) {
  test(`unloading during a batch ignores late ${fails ? 'failure' : 'success'} and stops remaining uploads`, async () => {
    const { page, state } = harness();
    const pending = page.handleAdd({ detail: { files: [{ url: 'local-a' }, { url: 'local-b' }] } });
    page.onUnload();
    const updates = state.updates;
    if (fails) state.uploads[0].reject(new Error('late failure'));
    else state.uploads[0].resolve(uploaded(11));
    await pending;
    assert.equal(state.updates, updates);
    assert.equal(state.uploads.length, 1);
    assert.deepEqual(state.released, fails ? [] : [11]);
    assert.deepEqual(state.toasts, []);
  });
}

test('removal and unsaved exit release temporary images but preserve original product images', async () => {
  const { page, state } = harness();
  page.setData({ fileList: [{ url: 'existing', mediaId: 5, temporary: false, status: 'done' }] });
  const first = page.uploadImage({ url: 'local-a' });
  state.uploads[0].resolve(uploaded(11));
  await first;
  const second = page.uploadImage({ url: 'local-b' });
  state.uploads[1].resolve(uploaded(22));
  await second;
  remove(page, 1);
  assert.deepEqual(assetIds(page), [5, 22]);
  page.onUnload();
  assert.deepEqual(state.released, [11, 22]);
});

for (const isEdit of [false, true]) {
  test(`successful ${isEdit ? 'editing' : 'publishing'} preserves submitted images on exit`, async () => {
    const { page, state } = harness();
    page.setData({ isEdit, id: isEdit ? 1 : null });
    const pending = page.uploadImage({ url: 'local-a' });
    state.uploads[0].resolve(uploaded(11));
    await pending;
    await page.doSubmit();
    page.onUnload();
    assert.deepEqual(Array.from(state.saved[0].imageAssetIds), [11]);
    assert.deepEqual(state.released, []);
  });
}
