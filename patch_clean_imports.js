const { readFile, writeFile } = require('node:fs/promises');
const path = require('node:path');
const file = path.join(__dirname, 'src', 'main', 'resources', 'static', 'spotify-frontend', 'src', 'MainApp.vue');
(async () => {
  const original = await readFile(file, 'utf8');
  await writeFile(path.join(__dirname, 'MainApp.vue.bak'), original, 'utf8');
  let content = original;
  content = content.replace(
    "import { getLastSong, setLastSong as saveLastSong, getLastQueue, setLastQueue, clearLastQueue, getLastShuffle, setLastShuffle } from './utils/storage'\nimport { restoreLastQueue } from './utils/playback'",
    "import { getLastSong, setLastSong as saveLastSong } from './utils/storage'\nimport { persistCurrentQueue, restoreLastQueue } from './utils/playback'"
  );
  await writeFile(file, content, 'utf8');
  console.log('cleaned imports')
})().catch(err => { console.error(err); process.exit(1); });
