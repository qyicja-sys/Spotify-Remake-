/**
 * 最近播放 Store（Vue 3 reactive 模式，效果等同 Pinia）
 *
 * 用户播放歌曲时：
 * 1. 乐观更新本地列表（立即反映到 UI）
 * 2. fire-and-forget 异步 POST /spotify/playback/record
 * 3. 后端写入 Redis ZSET → 异步落库 MySQL
 */
import { reactive } from 'vue'

function getToken() {
  return sessionStorage.getItem('jwt') || ''
}

/** 标准化歌曲对象 */
function normalizeSong(song) {
  return {
    id: song.id || song.externalId,   // 外部歌曲用 externalId 兜底
    title: song.title || '未知歌曲',
    artistName: song.artistName || song.artist || '未知艺人',
    coverUrl: song.coverUrl || song.coverNetworkUrl || '',
    coverNetworkUrl: song.coverNetworkUrl || song.coverUrl || '',
    duration: song.duration || 0,
    externalSource: song.externalSource || song.source || '',
    externalId: song.externalId || '',
    picId: song.picId || '',
  }
}

/** 标准化艺人对象 */
function normalizeArtist(artist) {
  return {
    id: artist.id,
    name: artist.name || artist.artistName || '',
    avatarUrl: artist.avatarUrl || '',
    avatarNetworkUrl: artist.avatarNetworkUrl || '',
  }
}

export const recentlyPlayedStore = reactive({
  /** 最近播放歌曲列表（最多 50 首，最新的在前） */
  recentSongs: [],
  /** 最近播放艺人列表（最多 5 个，最新的在前） */
  recentArtists: [],

  /**
   * 记录播放一首歌（乐观更新 + 异步后端同步）
   * @param {Object} song - 歌曲对象
   */
  recordPlay(song) {
    console.log('[recentlyPlayed] recordPlay called — id:', song?.id, 'externalId:', song?.externalId, 'title:', song?.title, 'externalSource:', song?.externalSource, 'isExternal:', song?.isExternal)
    if (!song) {
      console.warn('[recentlyPlayed] recordPlay skipped — no song object')
      return
    }
    const normalized = normalizeSong(song)
    if (!normalized.id) {
      console.warn('[recentlyPlayed] recordPlay skipped — no id or externalId')
      return
    }

    // 去重：移除旧条目
    this.recentSongs = this.recentSongs.filter(s => s.id !== normalized.id)
    // 插入到最前面
    this.recentSongs.unshift(normalized)
    // 保留最近 50 首
    if (this.recentSongs.length > 50) {
      this.recentSongs = this.recentSongs.slice(0, 50)
    }
    console.log('[recentlyPlayed] store updated — recentSongs count:', this.recentSongs.length)

    // 异步发送到后端（外部歌曲由 recordExternalPlay 处理，不重复发送）
    if (!song.externalSource && !song.isExternal) {
      this._syncToBackend(song.id)
    }
  },

  /**
   * 记录播放一个艺人（乐观更新）
   * @param {Object} artist - 艺人对象
   */
  recordArtist(artist) {
    if (!artist || !artist.id) return
    const normalized = normalizeArtist(artist)

    this.recentArtists = this.recentArtists.filter(a => a.id !== normalized.id)
    this.recentArtists.unshift(normalized)
    if (this.recentArtists.length > 5) {
      this.recentArtists = this.recentArtists.slice(0, 5)
    }
  },

  /** 从 Profile API 响应中合并数据（保留本地乐观更新的数据，不被服务端空数据覆盖） */
  setFromProfile(profileData) {
    console.log('[recentlyPlayed] setFromProfile called — server songs:', profileData.recentSongs?.length, 'server artists:', profileData.recentArtists?.length, 'local songs:', this.recentSongs.length, 'local artists:', this.recentArtists.length)
    if (profileData.recentSongs && profileData.recentSongs.length > 0) {
      const serverIds = new Set(profileData.recentSongs.map(s => s.id))
      // 保留本地已播放但服务端尚未返回的歌曲（可能因 Redis 延迟）
      const localOnly = this.recentSongs.filter(s => !serverIds.has(s.id))
      // 服务端数据为基底，本地新增置顶
      this.recentSongs = [...localOnly, ...profileData.recentSongs.map(normalizeSong)]
      if (this.recentSongs.length > 50) {
        this.recentSongs = this.recentSongs.slice(0, 50)
      }
    }
    // 服务端返回空数组时不覆盖本地数据（可能 Redis 暂未同步）
    if (profileData.recentArtists && profileData.recentArtists.length > 0) {
      const serverIds = new Set(profileData.recentArtists.map(a => a.id))
      const localOnly = this.recentArtists.filter(a => !serverIds.has(a.id))
      this.recentArtists = [...localOnly, ...profileData.recentArtists.map(normalizeArtist)]
      if (this.recentArtists.length > 5) {
        this.recentArtists = this.recentArtists.slice(0, 5)
      }
    }
  },

  /** 异步同步到后端（fire-and-forget） */
  async _syncToBackend(songId) {
    try {
      const token = getToken()
      if (!token) {
        console.warn('[recentlyPlayed] _syncToBackend skipped — no token')
        return
      }
      console.log('[recentlyPlayed] _syncToBackend sending — songId:', songId)
      const resp = await fetch('/spotify/playback/record', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'token': token,
        },
        body: JSON.stringify({ songId }),
      })
      console.log('[recentlyPlayed] _syncToBackend response — status:', resp.status)
    } catch (e) {
      console.error('[recentlyPlayed] _syncToBackend failed:', e.message || e)
    }
  },
})
