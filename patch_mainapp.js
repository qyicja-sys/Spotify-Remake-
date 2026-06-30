const { readFile, writeFile, mkdir } = require('node:fs/promises');
const path = require('node:path');

const dir = __dirname;
const file = path.join(dir, 'src', 'main', 'resources', 'static', 'spotify-frontend', 'src', 'MainApp.vue');

(async () => {
  const original = await readFile(file, 'utf8');
  await writeFile(path.join(dir, 'MainApp.vue.bak'), original, 'utf8');
  const lines = original.split(/\r?\n/);
  lines.splice(359, 0, '    persistCurrentQueue(playQueue.value, { shuffle: shuffleActive.value })');
  await writeFile(file, lines.join('\n') + '\n', 'utf8');
  console.log('Patched', file);
})().catch(err => { console.error(err); process.exit(1); });
