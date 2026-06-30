// ── URL helpers ──────────────────────────────────────────────────────────────

function isLocalHost(hostname) {
  return hostname === 'localhost' || hostname === '127.0.0.1'
}

function isProxyMode() {
  const { hostname, port } = window.location
  if (!isLocalHost(hostname)) return false
  if (port === '90') return true
  return import.meta.env.DEV && (port === '5173' || port === '3000' || port === '5000' || port === '8000')
}

function getApiBase() {
  const envBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim()
  if (envBaseUrl) return envBaseUrl.replace(/\/$/, '')
  if (isProxyMode()) return '/api'
  return ''
}

function withBase(path) {
  return `${getApiBase()}${path}`
}

export function resolveUrl(path) {
  if (!path) return ''
  if (path.startsWith('http')) return path
  const normalized = path.startsWith('/') ? path : `/${path}`
  return normalized
}

// 优先返回网络URL，没有则解析本地URL
export function imgUrl(localUrl, networkUrl) {
  if (networkUrl) return networkUrl
  return resolveUrl(localUrl)
}

// ── Error & auth helpers ─────────────────────────────────────────────────────

function createError(message, responseData, status) {
  const error = new Error(message)
  error.response = { data: responseData, status }
  return error
}

function redirectToLogin(kickedOut = false) {
  sessionStorage.clear()
  localStorage.removeItem('jwt')
  localStorage.removeItem('refreshToken')
  if (kickedOut) {
    sessionStorage.setItem('kickedOut', '1')
  }
  window.location.href = '/spotify-frontend/'
}

function getTokenHeader() {
  const token = sessionStorage.getItem('jwt')
  return token ? { token } : {}
}

async function parseResponseBody(response) {
  const text = await response.text()
  if (!text) return {}
  try {
    return JSON.parse(text)
  } catch {
    throw createError(`Unexpected response from ${response.url}`, { message: text }, response.status)
  }
}

// ── Token 刷新 ───────────────────────────────────────────────────────────────

let isRefreshing = false
let refreshPromise = null

/** 尝试用 refreshToken 换取新的 access token，成功则更新 sessionStorage */
async function tryRefreshToken() {
  const refreshToken = sessionStorage.getItem('refreshToken') || localStorage.getItem('refreshToken')
  if (!refreshToken) return false

  // 避免并发刷新
  if (isRefreshing) {
    await refreshPromise
    return !!sessionStorage.getItem('jwt')
  }

  isRefreshing = true
  refreshPromise = (async () => {
    try {
      const resp = await fetch('/spotify/token/refresh', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken }),
      })
      const data = await resp.json()
      if (resp.ok && data.code === 200 && data.data) {
        const { jwt, refreshToken: newRefreshToken } = data.data
        if (jwt) {
          sessionStorage.setItem('jwt', jwt)
          localStorage.setItem('jwt', jwt)
        }
        if (newRefreshToken) {
          sessionStorage.setItem('refreshToken', newRefreshToken)
          localStorage.setItem('refreshToken', newRefreshToken)
        }
        return true
      }
      return false
    } catch {
      return false
    } finally {
      isRefreshing = false
      refreshPromise = null
    }
  })()

  return refreshPromise
}

function throwIfNotOk(response, data) {
  if (response.ok) return
  if (response.status === 401) {
    const kickedOut = data?.msg === 'ACCOUNT_LOGGED_IN_ELSEWHERE'
    // ACCOUNT_LOGGED_IN_ELSEWHERE 不走刷新，直接踢出
    if (kickedOut) {
      redirectToLogin(true)
      throw createError('Account logged in elsewhere', data, 401)
    }
    // 普通 401：抛出一个可被 request() 捕获并尝试刷新的错误
    const err = createError('TOKEN_EXPIRED', data, 401)
    err.needsRefresh = true
    throw err
  }
  const message = data?.message || data?.msg || `Request failed: ${response.status}`
  throw createError(message, data, response.status)
}

// ── Core request ─────────────────────────────────────────────────────────────

async function request(method, path, payload, { timeout = 10000 } = {}) {
  const url = withBase(path)
  const controller = new AbortController()
  let timer = setTimeout(() => controller.abort(), timeout)

  function resetTimer() {
    clearTimeout(timer)
    timer = setTimeout(() => controller.abort(), timeout)
  }

  async function doFetch() {
    const options = {
      method,
      headers: {
        'Content-Type': 'application/json',
        ...getTokenHeader(),
      },
      credentials: 'include',
      cache: 'no-store',
      signal: controller.signal,
    }

    if (payload != null) {
      options.body = JSON.stringify(payload)
    }

    let response
    try {
      response = await fetch(url, options)
    } catch (error) {
      if (error.name === 'AbortError') throw createError('请求超时，请稍后重试', null, 0)
      throw createError('Network error, please try again', null, 0)
    }

    const data = await parseResponseBody(response)
    throwIfNotOk(response, data)
    return { data, status: response.status }
  }

  try {
    const result = await doFetch()
    clearTimeout(timer)
    return result
  } catch (error) {
    clearTimeout(timer)
    // Token 过期 → 尝试刷新后重试一次
    if (error.needsRefresh) {
      const refreshed = await tryRefreshToken()
      if (refreshed) {
        resetTimer()
        try {
          const retryResult = await doFetch()
          clearTimeout(timer)
          return retryResult
        } catch (retryError) {
          clearTimeout(timer)
          if (retryError.needsRefresh) {
            redirectToLogin(false)
            throw createError('登录已过期，请重新登录', null, 401)
          }
          throw retryError
        }
      }
      // 刷新失败 → 跳登录
      redirectToLogin(false)
      throw createError('登录已过期，请重新登录', null, 401)
    }
    throw error
  }
}

// ── Upload helper (shared by uploadAvatar / uploadSong) ──────────────────────

async function uploadFile(path, formData) {
  const url = withBase(path)
  const response = await fetch(url, {
    method: 'POST',
    headers: getTokenHeader(),
    body: formData,
    credentials: 'include',
    cache: 'no-store',
  })

  const data = await parseResponseBody(response)
  throwIfNotOk(response, data)
  return { data, status: response.status }
}

// ── Public API ───────────────────────────────────────────────────────────────

export function login(payload) {
  return request('POST', '/spotify/login', payload)
}

export function signUp(payload) {
  return request('POST', '/spotify/signup', payload)
}

export function resetPassword(payload) {
  return request('POST', '/spotify/login/forgetPassword', payload)
}

export function getPlaylistDetail(id) {
  return request('GET', `/spotify/playlist/${id}`)
}

export function deletePlaylist(playlistId) {
  return request('DELETE', `/spotify/playlist/${playlistId}`)
}

export function getHome() {
  return request('GET', '/spotify/home')
}

export function getStreamUrl(id) {
  return request('GET', `/stream/songs/${id}/stream-url`)
}

export function getExternalStreamUrl(source, externalId) {
  return request('GET', `/stream/songs/external/${source}/${externalId}/stream-url`)
}

export function search(keyword) {
  return request('GET', `/spotify/search?title=${encodeURIComponent(keyword)}`)
}

export function searchExternal(keyword) {
  return request('GET', `/spotify/external/search?keyword=${encodeURIComponent(keyword)}`, null, { timeout: 5000 })
}

export function searchExternalByArtist(artistName) {
  return request('GET', `/spotify/external/artist-search?artistName=${encodeURIComponent(artistName)}`, null, { timeout: 8000 })
}

export function createPlaylist() {
  return request('POST', '/spotify/playlist/create')
}

export function getProfile() {
  return request('GET', '/spotify/profile')
}

export function updateNickName(nickName) {
  return request('PUT', '/spotify/profile/nickname', { nickName })
}

export function uploadAvatar(file) {
  const formData = new FormData()
  formData.append('file', file)
  return uploadFile('/spotify/profile/avatar', formData)
}

export function registerArtist(nickName) {
  return request('POST', '/spotify/profile/register-artist', { nickName })
}

export function uploadSong(title, coverFile, musicFile, duration, featuredArtistIds) {
  const formData = new FormData()
  formData.append('title', title)
  formData.append('cover', coverFile)
  formData.append('music', musicFile)
  if (duration != null) formData.append('duration', String(duration))
  if (featuredArtistIds && featuredArtistIds.length > 0) {
    featuredArtistIds.forEach(id => formData.append('featuredArtistIds', String(id)))
  }
  return uploadFile('/spotify/profile/upload-song', formData)
}

export function searchArtists(keyword) {
  return request('GET', `/spotify/artists/search?keyword=${encodeURIComponent(keyword)}`)
}

export function getArtistDetail(id) {
  return request('GET', `/spotify/artist/${id}`)
}

export function followArtist(id) {
  return request('POST', `/spotify/artist/${id}/follow`)
}

export function unfollowArtist(id) {
  return request('DELETE', `/spotify/artist/${id}/follow`)
}

export function checkArtistFollowed(id) {
  return request('GET', `/spotify/artist/${id}/follow/check`)
}

export function likeSong(songId) {
  return request('POST', '/spotify/playlist/like', { songId })
}

export function likeExternalSong(track) {
  return request('POST', '/spotify/playlist/like-external', track)
}

export function unlikeSong(songId) {
  return request('DELETE', `/spotify/playlist/like/${songId}`)
}

export function checkLiked(songId) {
  return request('GET', `/spotify/playlist/like/check/${songId}`)
}

export function addSongToPlaylist(playlistId, songId) {
  return request('POST', `/spotify/playlist/${playlistId}/add-song`, { songId })
}

export function removeSongFromPlaylist(playlistId, songId) {
  return request('DELETE', `/spotify/playlist/${playlistId}/songs/${songId}`)
}

export function editPlaylistDetail(playlistId, title, profile, coverFile) {
  const formData = new FormData()
  formData.append('title', title)
  if (profile != null) formData.append('profile', profile)
  if (coverFile) formData.append('cover', coverFile)
  return uploadFile(`/spotify/playlist/${playlistId}/edit`, formData)
}

export function collectPlaylist(playlistId) {
  return request('POST', `/spotify/playlist/${playlistId}/collect`)
}

export function uncollectPlaylist(playlistId) {
  return request('DELETE', `/spotify/playlist/${playlistId}/collect`)
}

export function checkPlaylistCollected(playlistId) {
  return request('GET', `/spotify/playlist/${playlistId}/collect/check`)
}

export function togglePlaylistPrivacy(playlistId) {
  return request('PUT', `/spotify/playlist/${playlistId}/privacy`)
}

// ── Album API ────────────────────────────────────────────────────────────

export function createAlbum(formData) {
  return uploadFile('/spotify/profile/create-album', formData)
}

export function searchMySongs(keyword) {
  return request('GET', `/spotify/profile/my-songs/search?keyword=${encodeURIComponent(keyword || '')}`)
}

export function getAlbumDetail(id) {
  return request('GET', `/spotify/album/${id}`)
}

export function getArtistAlbums(artistId) {
  return request('GET', `/spotify/artist/${artistId}/albums`)
}

// ── External playback history ──────────────────────────────────────────────

/** 记录外部歌曲播放历史（fire-and-forget） */
export function recordExternalPlay(metadata) {
  return request('POST', '/stream/songs/external/record-play', metadata)
}

// ── Lyrics API ────────────────────────────────────────────────────────────

/** 获取本地歌曲歌词 */
export function getLocalLyrics(title) {
  return request('GET', `/spotify/lyrics?title=${encodeURIComponent(title)}`)
}

/** 获取外部歌曲歌词 */
export function getExternalLyrics(source, lyricId) {
  return request('GET', `/spotify/lyrics/external?source=${encodeURIComponent(source)}&lyricId=${encodeURIComponent(lyricId)}`)
}
