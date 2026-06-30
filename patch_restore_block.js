const { readFile, writeFile } = require('node:fs/promises');
const path = require('node:path');
const file = path.join(__dirname, 'src', 'main', 'resources', 'static', 'spotify-frontend', 'src', 'MainApp.vue');
(async () => {
  const original = await readFile(file, 'utf8');
  await writeFile(path.join(__dirname, 'MainApp.vue.bak'), original, 'utf8');
  const needle = '  const lastSong = getLastSong()\n  if (lastSong) {\n    restoredSongId.value = lastSong.id\n    restoredTime.value = lastSong.currentTime || 0\n    delete lastSong.currentTime\n    currentSong.value = lastSong\n    playerVisible.value = true\n  }';
  if (!original.includes(needle)) throw new Error('未找到恢复歌曲代码块');
  const replacement = [
    '  const lastState = restoreLastQueue()',
    '  if (lastState) {',
    '    restoredSongId.value = lastState.currentSong.id',
    '    restoredTime.value = lastState.currentSong.currentTime || 0',
    '    currentSong.value = lastState.currentSong',
    '    playQueue.value = lastState.queue || []',
    '    currentIndex.value = playQueue.value.findIndex((item) => item.id === currentSong.value?.id)',
    '    shuffleActive.value = !!lastState.shuffle',
    '    playerVisible.value = true',
    '  }'
  ].join('\n');
  await writeFile(file, original.replace(needle, replacement), 'utf8');
  console.log('patched restore block')
})().catch(err => { console.error(err); process.exit(1); });
