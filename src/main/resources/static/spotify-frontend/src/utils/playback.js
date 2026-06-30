import { getLastSong, getLastQueue, setLastQueue, clearLastQueue, getLastShuffle, setLastShuffle } from './storage'

const STORAGE_KEYS = {
  LAST_QUEUE_CONTEXT: 'spotify_last_queue_context',
}

function normalizeQueueItem(song) {
  return {
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
    picId: song.picId,
    source: song.source,
  }
}

function readQueueContext() {
  try {
    const raw = localStorage.getItem(STORAGE_KEYS.LAST_QUEUE_CONTEXT)
    if (!raw) return {}
    const parsed = JSON.parse(raw)
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch {
    return {}
  }
}

function writeQueueContext(context) {
  localStorage.setItem(STORAGE_KEYS.LAST_QUEUE_CONTEXT, JSON.stringify(context || {}))
}

export function persistCurrentQueue(queue, options = {}) {
  const source = Array.isArray(queue) ? queue : []
  if (!source.length) {
    clearLastQueue()
    writeQueueContext({})
    return
  }
  const sanitized = source.map(normalizeQueueItem).slice(0, 200)
  setLastQueue(sanitized)
  if (options.shuffle != null) {
    setLastShuffle(!!options.shuffle)
  }
  writeQueueContext({
    origin: options.origin || readQueueContext().origin || 'unknown',
    playlistId: options.playlistId,
    artistId: options.artistId,
    albumId: options.albumId,
  })
}

export function restoreLastQueue() {
  const song = getLastSong()
  if (!song) return null
  const queue = getLastQueue()
  return {
    currentSong: {
      id: song.id,
      title: song.title,
      artist: song.artist || song.artistName || '未知艺人',
      artistName: song.artistName || song.artist || '未知艺人',
      coverUrl: song.coverUrl,
      coverNetworkUrl: song.coverNetworkUrl,
      duration: song.duration,
      currentTime: song.currentTime || 0,
      isExternal: !!song.isExternal,
      externalSource: song.externalSource,
      externalId: song.externalId,
      streamUrl: song.streamUrl,
      picId: song.picId,
      source: song.source,
    },
    queue: queue || [],
    shuffle: getLastShuffle(),
    context: readQueueContext(),
  }
}
