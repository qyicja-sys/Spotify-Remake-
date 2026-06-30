const { readFile, writeFile } = require('node:fs/promises');
const path = require('node:path');

const dir = __dirname;
const file = path.join(dir, 'src', 'main', 'resources', 'static', 'spotify-frontend', 'src', 'MainApp.vue');

(async () => {
  const original = await readFile(file, 'utf8');
  await writeFile(path.join(dir, 'MainApp.vue.bak'), original, 'utf8');
  const needle = "import { getLastSong, setLastSong as saveLastSong, getLastQueue, setLastQueue, clearLastQueue, getLastShuffle, setLastShuffle } from './utils/storage'"
  if (!original.includes(needle)) {
    throw new Error('未找到 storage import')
  }
  const replacement = needle + "\nimport { restoreLastQueue } from './utils/playback'"
  await writeFile(file, original.replace(needle, replacement), 'utf8');
  console.log('added playback import')
})().catch(err => { console.error(err); process.exit(1); });
