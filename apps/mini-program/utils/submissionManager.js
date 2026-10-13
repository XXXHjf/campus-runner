const accounts = require('./accountStorage');
const BUCKET = 'pendingSubmissions';
const flights = new Map();

function snapshot(value) { return JSON.parse(JSON.stringify(value)); }
function canonical(value) {
  if (Array.isArray(value)) return value.map(canonical);
  if (value && typeof value === 'object') {
    const result = {};
    Object.keys(value).sort().forEach(key => {
      if (key !== 'clientRequestId' && value[key] !== undefined) result[key] = canonical(value[key]);
    });
    return result;
  }
  return value;
}
function getPending(slot, session = accounts.captureSession()) {
  const value = accounts.read(BUCKET, session)?.items?.[slot];
  return value ? snapshot(value.payload) : null;
}
function save(slot, payload, session) {
  const record = accounts.read(BUCKET, session) || { items: {} };
  record.items = { ...record.items, [slot]: { payload } };
  if (!accounts.write(BUCKET, record, session)) throw new Error('登录已变化，请重新提交');
}
function clear(slot, key, session) {
  if (!accounts.isCurrent(session)) return;
  const record = accounts.read(BUCKET, session);
  if (record?.items?.[slot]?.payload?.clientRequestId !== key) return;
  delete record.items[slot];
  accounts.write(BUCKET, record, session);
}
function createKey() {
  // This is a uniqueness identifier, not a credential. The server also binds it
  // to the owner, operation and content and refuses accidental collisions.
  return `cr_${Date.now().toString(36)}_${Math.random().toString(36).slice(2)}_${Math.random().toString(36).slice(2)}`;
}
function submit(slot, data, send) {
  const session = accounts.captureSession();
  if (!accounts.isCurrent(session)) return Promise.reject(new Error('请先登录后再提交'));
  let payload;
  try {
    const pending = getPending(slot, session);
    if (pending && JSON.stringify(canonical(pending)) !== JSON.stringify(canonical(data))) {
      throw new Error('上次提交结果暂未确认，请先重试上次提交');
    }
    payload = pending || { ...snapshot(data), clientRequestId: createKey() };
    // Fail closed if storage cannot persist the identifier before the write.
    save(slot, payload, session);
  } catch (error) { return Promise.reject(error); }
  const flightKey = `${session.ownerId}:${slot}:${payload.clientRequestId}`;
  if (flights.has(flightKey)) return flights.get(flightKey);
  const promise = Promise.resolve().then(() => {
    if (!accounts.isCurrent(session)) throw new Error('登录已变化，请重新提交');
    return send(snapshot(payload));
  }).then(result => {
    clear(slot, payload.clientRequestId, session);
    return result;
  }).catch(error => {
    if (error?.requestOutcome === 'rejected') clear(slot, payload.clientRequestId, session);
    throw error;
  }).finally(() => flights.delete(flightKey));
  flights.set(flightKey, promise);
  return promise;
}

// Pending writes intentionally survive logout: a completed but unacknowledged
// write cannot be treated as cancelled. Reads are still restricted to its owner.
module.exports = { getPending, submit };
