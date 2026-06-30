const fs = require('fs');
const p = 'D:/javaedit/project/spotify/Spotify_remake/Spotify_Remake/src/main/resources/static/spotify-frontend/src/MainApp.vue';
let c = fs.readFileSync(p, 'utf8');

const old = '>{{ line.text }}</div>';
const rep = '>\n' +
  '                  <span class="lyric-base">{{ line.text }}</span>\n' +
  '                  <span v-if="i === expandActiveLyricIndex" class="lyric-fill" :style="{ width: (expandCharProgress * 100) + \x27%\x27 }">{{ line.text }}</span>\n' +
  '                </div>';

const idx = c.lastIndexOf(old);
if (idx === -1) { console.log('NOT FOUND'); process.exit(1); }
c = c.slice(0, idx) + rep + c.slice(idx + old.length);
fs.writeFileSync(p, c, 'utf8');
console.log('Done, idx:', idx);