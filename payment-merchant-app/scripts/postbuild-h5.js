// H5 导出后处理脚本：node scripts/postbuild-h5.js
// 作用：Expo 会把所有 Web 静态资源放进 dist/assets/node_modules/ 下，
//       而「node_modules」是几乎所有上传工具 / .gitignore / rsync / CI 默认排除的目录，
//       导致部署后图标字体、返回箭头等 404。
//       本脚本把 assets/node_modules 重命名为 assets/vendor，并同步改写 bundle 里的资源路径引用，
//       从根上规避该问题。
const fs = require('fs');
const path = require('path');

const dist = path.join(__dirname, '..', 'dist');
const assetsDir = path.join(dist, 'assets');
const oldDir = path.join(assetsDir, 'node_modules');
const newDir = path.join(assetsDir, 'vendor');

// 1) 重命名 assets/node_modules -> assets/vendor
if (fs.existsSync(oldDir)) {
  fs.renameSync(oldDir, newDir);
  console.log('[postbuild-h5] renamed: assets/node_modules -> assets/vendor');
} else if (!fs.existsSync(newDir)) {
  console.log('[postbuild-h5] warning: assets/node_modules not found');
}

// 2) 改写 JS bundle 中的资源路径：assets/node_modules/ -> assets/vendor/
const jsDir = path.join(dist, '_expo', 'static', 'js', 'web');
if (fs.existsSync(jsDir)) {
  for (const file of fs.readdirSync(jsDir)) {
    if (!file.endsWith('.js')) continue;
    const fp = path.join(jsDir, file);
    const code = fs.readFileSync(fp, 'utf8');
    const next = code.split('assets/node_modules/').join('assets/vendor/');
    if (next !== code) {
      fs.writeFileSync(fp, next);
      console.log('[postbuild-h5] rewritten paths in:', file);
    }
  }
}
console.log('[postbuild-h5] done');
