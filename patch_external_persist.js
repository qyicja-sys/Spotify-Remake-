const { readFile, writeFile } = require('node:fs/promises');
const path = require('node:path');
const file = path.join(__dirname, 'src', 'main', 'resources', 'static', 'spotify-frontend', 'src', 'MainApp.vue');
(async () => {
  const original = await readFile(file, 'utf8');
  await writeFile(path.join(__dirname, 'MainApp.vue.bak'), original, 'utf8');
  const lines = original.split(/\r?\n/);
  const targets = [2095, 2170];
  for (const idx of targets) {
    lines.splice(idx, 0, '  persistCurrentQueue(playQueue.value, { shuffle: shuffleActive.value })');
  }
  await writeFile(file, lines.join('\n') + '\n', 'utf8');
  console.log('patched external persist points')
})().catch(err => { console.error(err); process.exit(1); });
