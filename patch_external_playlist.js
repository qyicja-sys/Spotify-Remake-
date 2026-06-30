const { readFile, writeFile } = require('node:fs/promises');
const path = require('node:path');
const file = path.join(__dirname, 'src', 'main', 'resources', 'static', 'spotify-frontend', 'src', 'MainApp.vue');
(async () => {
  const original = await readFile(file, 'utf8');
  await writeFile(path.join(__dirname, 'MainApp.vue.bak'), original, 'utf8');
  let content = original;
  content = content.replace(
    "function playExternalSongFromPlaylist(song, queue) {",
    "function playExternalSongFromPlaylist(song, queue, options = {}) {"
  );
  content = content.replace(
    /  saveLastSong\(\{ \.\.\.currentSong\.value, currentTime: 0 \}\)\n\n  \/\/ 璁板綍澶栭儴姝屾洸鎾斁鍘嗗彶/,
    "  saveLastSong({ ...currentSong.value, currentTime: 0 })\n  persistCurrentQueue(playQueue.value, { shuffle: shuffleActive.value, ...options })\n\n  // 璁板綍澶栭儴姝屾洸鎾斁鍘嗗彶"
  );
  await writeFile(file, content, 'utf8');
  console.log('patched playExternalSongFromPlaylist')
})().catch(err => { console.error(err); process.exit(1); });
