const fs = require('node:fs');
const path = require('node:path');

// TDesign 1.4.1 picker interaction patch. Keep installed and DevTools copies in sync.
const root = path.resolve(__dirname, '..');
const marker = '// campus-runner: picker interaction patch';
const methods = `            ${marker}
            onTouchStart(event) {
                this.StartY = event.touches[0].clientY;
                this.StartOffset = this.data.offset;
                this._touchMoved = false;
                this.setData({ duration: 0 });
            },
            onTouchMove(event) {
                const deltaY = event.touches[0].clientY - this.StartY;
                if (Math.abs(deltaY) > 5) this._touchMoved = true;
                this.setData({
                    offset: range(this.StartOffset + deltaY, -Math.max(0, this.getCount() - 1) * this.itemHeight, 0),
                    duration: 0,
                });
            },
            onTouchEnd(event) {
                // A stationary touch is handled by onItemTap. Cancel only restores alignment.
                if (!this._touchMoved && this.data.offset === this.StartOffset && event.type !== 'touchcancel') return;
                const index = Math.round(-this.data.offset / this.itemHeight);
                this.selectIndex(index);
            },
            onItemTap(event) {
                // Some runtimes emit tap after a drag; do not select the touched row then.
                if (this._touchMoved) return;
                this.selectIndex(Number(event.currentTarget.dataset.index));
            },
            selectIndex(targetIndex) {
                const { options, labelAlias, valueAlias, columnIndex } = this.data;
                if (!options.length || !Number.isInteger(targetIndex)) return;
                const index = range(targetIndex, 0, options.length - 1);
                this.setData({
                    curIndex: index,
                    offset: -index * this.itemHeight,
                    duration: DefaultDuration,
                });
                if (index === this._selectedIndex) return;
                // Update before confirm or a cascading column change can read the selection.
                this._selectedIndex = index;
                this._selectedValue = options[index][valueAlias];
                this._selectedLabel = options[index][labelAlias];
                if (this.$parent) this.$parent.triggerColumnChange({ index, column: columnIndex });
            },
`;

function patch(directory) {
  const jsPath = path.join(directory, 'picker-item/picker-item.js');
  const wxmlPath = path.join(directory, 'picker-item/picker-item.wxml');
  if (!fs.existsSync(jsPath)) return false;
  let js = fs.readFileSync(jsPath, 'utf8');
  let wxml = fs.readFileSync(wxmlPath, 'utf8');
  if (!js.includes(marker)) {
    // Fail explicitly on dependency changes instead of silently applying a stale patch.
    if (!js.includes('duration: DefaultDuration,') ||
        !js.includes('return Math.abs(touchDeltaY) > itemHeight ? 1.2 * touchDeltaY : touchDeltaY;')) {
      throw new Error('Picker source changed; review the interaction patch before rebuilding npm.');
    }
    const start = js.indexOf('            onTouchStart(event) {');
    const end = js.indexOf('            update() {', start);
    if (start < 0 || end < 0) throw new Error('Picker methods not found.');
    js = js.slice(0, start) + methods + js.slice(end);
    js = js.replace('return Math.abs(touchDeltaY) > itemHeight ? 1.2 * touchDeltaY : touchDeltaY;', 'return touchDeltaY;');
  }
  if (!wxml.includes('bind:tap="onItemTap"')) {
    const anchor = 'data-index="{{ index }}"';
    if (!wxml.includes(anchor)) throw new Error('Picker item template changed.');
    wxml = wxml.replace(anchor, anchor + '\n      bind:tap="onItemTap"');
  }
  fs.writeFileSync(jsPath, js);
  fs.writeFileSync(wxmlPath, wxml);
  return true;
}

const directories = [
  'node_modules/tdesign-miniprogram',
  'node_modules/tdesign-miniprogram/miniprogram_dist',
  'miniprogram_npm/tdesign-miniprogram',
];
let count = 0;
for (const directory of directories) {
  if (patch(path.join(root, directory))) count++;
}
if (!count) throw new Error('TDesign picker not found. Install dependencies first.');
console.log('Picker interaction patch applied to ' + count + ' copy/copies.');
