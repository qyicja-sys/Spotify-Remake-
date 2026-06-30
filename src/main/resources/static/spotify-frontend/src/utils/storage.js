const STORAGE_KEYS = {
  LANGUAGE: 'spotify_language',
  LAST_SONG: 'spotify_last_song',
}

export function getLanguage() {
  return localStorage.getItem(STORAGE_KEYS.LANGUAGE) || 'en'
}

export function setLanguage(lang) {
  localStorage.setItem(STORAGE_KEYS.LANGUAGE, lang)
}

export function getLastSong() {
  try {
    const raw = localStorage.getItem(STORAGE_KEYS.LAST_SONG)
    return raw ? JSON.parse(raw) : null
  } catch { return null }
}

export function setLastSong(song) {
  localStorage.setItem(STORAGE_KEYS.LAST_SONG, JSON.stringify(song))
}

export function clearLastSong() {
  localStorage.removeItem(STORAGE_KEYS.LAST_SONG)
}
