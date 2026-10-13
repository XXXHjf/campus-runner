const tokenManager = require('./tokenManager');

// Page state belongs to a view and a login session, never to the process-wide service cache.
async function loadAddressPage(owner, field, fetchPage, reset = true, transform = item => item) {
  owner._addressPages ||= {};
  const token = tokenManager.getToken?.();
  const version = tokenManager.getSessionVersion?.();
  if (typeof tokenManager.getToken === 'function' && !token) {
    owner._addressPages[field] = null;
    owner.setData({ [field]: [] });
    return null;
  }
  let state = owner._addressPages[field];
  if (reset || !state || state.token !== token || state.version !== version) {
    state = { token, version, page: 1, more: true, busy: false };
    owner._addressPages[field] = state;
  }
  if (state.busy || !state.more) return null;
  state.busy = true;
  try {
    const rows = await fetchPage({ page: state.page, pageSize: 20 });
    if (token !== tokenManager.getToken?.() || version !== tokenManager.getSessionVersion?.() || owner._addressPages[field] !== state
        || owner._unloaded || owner._detailDisposed || owner._detailHidden) return null;
    const old = reset ? [] : owner.data[field] || [];
    const merged = [...old];
    for (const row of rows.map(transform)) {
      const index = merged.findIndex(item => String(item.id) === String(row.id));
      if (index < 0) merged.push(row); else merged[index] = row;
    }
    state.page++; state.more = rows.length === 20;
    owner.setData({ [field]: merged });
    return merged;
  } finally { state.busy = false; }
}

module.exports = { loadAddressPage };
