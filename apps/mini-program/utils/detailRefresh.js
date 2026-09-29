// 合并同一轮刷新；操作成功或页面返回后需要的新数据排在当前请求之后。
function refresh(page, load, fresh = false) {
  if (page._detailDisposed || page._detailHidden) return Promise.resolve();
  if (page._detailRequest) {
    if (fresh || page._detailInvalid) {
      page._detailVersion = (page._detailVersion || 0) + 1;
      page._detailQueued = true;
    }
    return page._detailRequest;
  }
  page._detailRequest = Promise.resolve().then(async () => {
    do {
      if (page._detailHidden || page._detailDisposed) return;
      page._detailQueued = false;
      page._detailInvalid = false;
      const version = page._detailVersion = (page._detailVersion || 0) + 1;
      const id = page.data.id;
      const current = () => !page._detailDisposed && !page._detailHidden
        && page._detailVersion === version && page.data.id === id;
      await load(current);
    } while (page._detailQueued && !page._detailHidden && !page._detailDisposed);
  }).finally(() => { page._detailRequest = null; });
  return page._detailRequest;
}

function hide(page, disposed = false) {
  page._detailHidden = true;
  page._detailDisposed = disposed;
  page._detailInvalid = true;
  page._detailVersion = (page._detailVersion || 0) + 1;
  page._detailQueued = false;
}

module.exports = { refresh, hide };
