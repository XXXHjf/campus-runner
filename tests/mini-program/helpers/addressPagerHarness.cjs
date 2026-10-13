const vm = require('node:vm');
const fs = require('node:fs');
module.exports = tokens => {
  const mod = { exports: {} };
  vm.runInNewContext(fs.readFileSync(require.resolve('../../../apps/mini-program/utils/addressPager.js'), 'utf8'),
    { module: mod, require: () => tokens });
  return mod.exports;
};
