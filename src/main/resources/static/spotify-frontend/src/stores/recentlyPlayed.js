/**
 * 最近播放 Store（Vue 3 reactive 模式，效果等同 Pinia）
 *
 * 用户播放歌曲时：
 * 1. 乐观更新本地列表（立即反映到 UI）
 * 2. fire-and-forget 异步 POST /spotify/playback/record
 * 3. 后端写入 Redis ZSET → 异步落库 MySQL
 */
import { reactive } from 'vue'

const TOKEN_KEY = 'spotify_token'

function getToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

/** 标准化歌曲对象 */
function normalizeSong(song) {
  return {
    id: song.id,
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
    if (!song || !song.id) return
    const normalized = normalizeSong(song)

    // 去重：移除旧条目
    this.recentSongs = this.recentSongs.filter(s => s.id !== normalized.id)
    // 插入到最前面
    this.recentSongs.unshift(normalized)
    // 保留最近 50 首
    if (this.recentSongs.length > 50) {
      this.recentSongs = this.recentSongs.slice(0, 50)
    }

    // 异步发送到后端
    this._syncToBackend(song.id)
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

  /** 从 Profile API 响应中设置完整数据 */
  setFromProfile(profileData) {
    if (profileData.recentSongs) {
      this.recentSongs = profileData.recentSongs.map(normalizeSong)
    }
    if (profileData.recentArtists) {
      this.recentArtists = profileData.recentArtists.map(normalizeArtist)
    }
  },

  /** 异步同步到后端（fire-and-forget） */
  async _syncToBackend(songId) {
    try {
      const token = getToken()
      if (!token) return
      await fetch('/spotify/playback/record', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'token': token,
        },
        body: JSON.stringify({ songId }),
      })
    } catch {
      // 静默失败，不影响播放体验
    }
  },
})
