const storage = require('./accountStorage');
const KEY = 'secondHandDrafts';
const LIFETIME = 7 * 24 * 60 * 60 * 1000;
const pending = new Map();
const fields = ['title', 'description', 'categoryId', 'conditionLevel', 'price', 'pickupAddressId',
  'pickupAddressSnapshot', 'pickupOnly', 'negotiable'];
let revision = 0;
const slot = id => id ? `edit:${id}` : 'new';
const pendingKey = (id, session) => `${session?.ownerId}:${slot(id)}`;

function load(id, session) {
  const entry = storage.read(KEY, session)?.entries?.[slot(id)];
  const age = Date.now() - Number(entry?.timestamp);
  return entry && Number.isFinite(age) && age >= 0 && age <= LIFETIME ? entry : null;
}

function save(id, data, session) {
  if (!storage.isCurrent(session)) return null;
  const existing = storage.read(KEY, session)?.entries || {};
  const form = Object.fromEntries(fields.map(field => [field, data.form[field]]));
  const files = (data.fileList || []).filter(file => file.status === 'done' && file.mediaId)
    .map(file => ({ mediaId: file.mediaId, temporary: !!file.temporary, status: 'done' }));
  const entry = { form, fileList: files, protectedImageIds: [...(data.protectedImageIds || [])],
    timestamp: Date.now(), revision: `${Date.now()}:${++revision}` };
  const entries = Object.fromEntries(Object.entries(existing).filter(([, value]) =>
    value && Date.now() - value.timestamp <= LIFETIME));
  delete entries[slot(id)];
  entries[slot(id)] = entry;
  const limited = Object.fromEntries(Object.entries(entries).reverse().sort((a, b) => b[1].timestamp - a[1].timestamp).slice(0, 20));
  return storage.write(KEY, { entries: limited }, session) ? entry.revision : null;
}

function clear(id, session, expectedRevision) {
  const entries = storage.read(KEY, session)?.entries;
  if (!entries || (expectedRevision && entries[slot(id)]?.revision !== expectedRevision)) return;
  delete entries[slot(id)];
  storage.write(KEY, { entries }, session);
}

function waitPending(id, session) {
  return pending.get(pendingKey(id, session)) || Promise.resolve();
}

function beginSubmission(id, session) {
  const key = pendingKey(id, session);
  let done;
  const promise = new Promise(resolve => { done = resolve; });
  pending.set(key, promise);
  return () => { if (pending.get(key) === promise) pending.delete(key); done(); };
}

module.exports = { load, save, clear, waitPending, beginSubmission };
