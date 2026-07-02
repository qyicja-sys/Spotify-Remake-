const STORAGE_KEYS = {
  LANGUAGE: 'spotify_language',
  LAST_SONG: 'spotify_last_song',
  LAST_QUEUE: 'spotify_last_queue',
  LAST_SHUFFLE: 'spotify_last_shuffle',
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

export function getLastQueue() {
  try {
    const raw = localStorage.getItem(STORAGE_KEYS.LAST_QUEUE)
    if (!raw) return []
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

export function setLastQueue(queue) {
  const sanitized = Array.isArray(queue)
    ? queue.map((song) => ({
        id: song.id,
        title: song.title,
        artist: song.artist || song.artistName || '未知艺人',
        artistName: song.artistName || song.artist || '未知艺人',
        coverUrl: song.coverUrl,
        coverNetworkUrl: song.coverNetworkUrl,
        duration: song.duration,
        isExternal: !!song.isExternal,
        externalSource: song.externalSource,
        externalId: song.externalId,
        streamUrl: song.streamUrl,
      }))
    : []
  localStorage.setItem(STORAGE_KEYS.LAST_QUEUE, JSON.stringify(sanitized.slice(0, 200)))
}

export function clearLastQueue() {
  localStorage.removeItem(STORAGE_KEYS.LAST_QUEUE)
}

export function getLastShuffle() {
  return localStorage.getItem(STORAGE_KEYS.LAST_SHUFFLE) === 'true'
}

export function setLastShuffle(active) {
  localStorage.setItem(STORAGE_KEYS.LAST_SHUFFLE, active ? 'true' : 'false')
}
