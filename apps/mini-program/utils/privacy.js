function maskPhone(phone) {
  const value = String(phone || '').trim();
  if (!value) return '';
  if (value.length < 7) return value;
  return `${value.slice(0, 3)}****${value.slice(-4)}`;
}

module.exports = {
  maskPhone,
};
