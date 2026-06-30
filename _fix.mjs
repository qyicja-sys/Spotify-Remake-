const fs = require('fs');
const p = String.raw`D:\javaedit\project\spotify\Spotify_remake\Spotify_Remake\src\main\resources\static\spotify-frontend\src\MainApp.vue`;
let c = fs.readFileSync(p, 'utf-8');

const oldWatch = `watch(expandActiveLyricIndex, (idx) => {\r\n  if (idx >= 0) {\r\n    nextTick(() => scrollLyricToCenter(idx))\r\n  }\r\n})`;

const newCode = `function clampLyricsScroll() {\r\n  const el = lyricsScrollRef.value\r\n  if (!el) return\r\n  const lines = el.querySelectorAll('.lyric-line')\r\n  if (!lines.length) return\r\n  const firstTop = lines[0].offsetTop\r\n  const lastEl = lines[lines.length - 1]\r\n  const lastBottom = lastEl.offsetTop + lastEl.clientHeight\r\n  const viewH = el.clientHeight\r\n  const minScroll = Math.max(0, firstTop - viewH / 2 + lines[0].clientHeight / 2)\r\n  const maxScroll = Math.max(0, lastBottom - viewH / 2 - lines[lines.length - 1].clientHeight / 2)\r\n  if (el.scrollTop < minScroll) el.scrollTop = minScroll\r\n  if (el.scrollTop > maxScroll) el.scrollTop = maxScroll\r\n}\r\n\r\nwatch(expandActiveLyricIndex, (idx) => {\r\n  if (idx >= 0) {\r\n    nextTick(() => scrollLyricToCenter(idx))\r\n  }\r\n})\r\n\r\nwatch(expandOverlayVisible, (visible) => {\r\n  nextTick(() => {\r\n    const el = lyricsScrollRef.value\r\n    if (!el) return\r\n    if (visible) {\r\n      el.addEventListener('scroll', clampLyricsScroll)\r\n    } else {\r\n      el.removeEventListener('scroll', clampLyricsScroll)\r\n    }\r\n  })\r\n})`;

if (c.includes(oldWatch)) {
  c = c.replace(oldWatch, newCode);
  fs.writeFileSync(p, c, 'utf-8');
  console.log('replaced ok');
} else {
  console.log('NOT FOUND');
}
