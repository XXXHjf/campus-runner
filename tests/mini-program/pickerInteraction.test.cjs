const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const vm = require('node:vm');

const directory = path.resolve(__dirname, '../../apps/mini-program/miniprogram_npm/tdesign-miniprogram/picker-item');
const source = fs.readFileSync(path.join(directory, 'picker-item.js'), 'utf8')
  .replace(/^import .*;\r?\n/gm, '')
  .replace(/PickerItem = __decorate\([\s\S]*$/, 'globalThis.PickerItem = PickerItem;');

function createColumn() {
  class SuperComponent {
    setData(values) { Object.assign(this.data, values); }
  }
  const context = { SuperComponent, config: { prefix: 't' }, props: {}, wx: {
    getSystemInfoSync: () => ({ windowWidth: 375 }),
  } };
  vm.runInNewContext(source, context);
  const column = new context.PickerItem();
  Object.assign(column, column.methods);
  column.created();
  column.data.options = ['学生公寓', '教师公寓', '公共设施'].map((label, value) => ({ label, value }));
  column.data.columnIndex = 1;
  column.update();
  const changes = [];
  column.$parent = { triggerColumnChange: (detail) => changes.push({ ...detail, value: column._selectedValue }) };
  return { column, changes };
}
const touch = (clientY) => ({ touches: [{ clientY }] });

test('tap selects a visible row and immediately notifies cascading columns', () => {
  const { column, changes } = createColumn();
  column.onTouchStart(touch(100));
  column.onTouchEnd({ type: 'touchend' });
  column.onItemTap({ currentTarget: { dataset: { index: 2 } } });
  assert.equal(column._selectedLabel, '公共设施');
  assert.equal(column.data.offset, -80);
  assert.deepEqual(changes, [{ index: 2, column: 1, value: 2 }]);
  assert.match(fs.readFileSync(path.join(directory, 'picker-item.wxml'), 'utf8'), /bind:tap="onItemTap"/);
});

test('drag follows the finger without animation and snaps one row on release', () => {
  const { column, changes } = createColumn();
  column.onTouchStart(touch(100));
  column.onTouchMove(touch(76));
  assert.equal(column.data.offset, -24);
  assert.equal(column.data.duration, 0);
  column.onTouchEnd({ type: 'touchend' });
  assert.equal(column.data.offset, -40);
  assert.equal(column.data.duration, 240);
  column.onItemTap({ currentTarget: { dataset: { index: 2 } } });
  assert.equal(column._selectedValue, 1);
  assert.equal(changes.length, 1);
});

test('tiny drags return to alignment; boundaries do not overshoot the last row', () => {
  const { column } = createColumn();
  column.onTouchStart(touch(100));
  column.onTouchMove(touch(97));
  column.onTouchEnd({ type: 'touchend' });
  assert.ok(column.data.offset === 0);
  column.onTouchStart(touch(100));
  column.onTouchMove(touch(-200));
  assert.equal(column.data.offset, -80);
  column.onTouchEnd({ type: 'touchend' });
  assert.equal(column._selectedValue, 2);
  column.data.options = [];
  column.update();
  column.onTouchStart(touch(100));
  column.onTouchMove(touch(0));
  column.onTouchEnd({ type: 'touchend' });
  assert.equal(column._selectedValue, undefined);
});
