<script setup>
import { ref, onMounted } from 'vue'
import { useApi } from '../composables/useApi'
import Button from '../components/ui/Button.vue'
import Input from '../components/ui/Input.vue'
import Label from '../components/ui/Label.vue'
import Dialog from '../components/ui/Dialog.vue'
import { ListMusic, Plus, Pencil, Trash2, Loader2, Music, X, GripVertical } from 'lucide-vue-next'

const { apiFetch, loading } = useApi()

const playlists = ref([])
const dialogOpen = ref(false)
const songsDialogOpen = ref(false)
const editingId = ref(null)
const managingPlaylistId = ref(null)
const managingPlaylistTitle = ref('')

// 歌曲列表状态（浏览 + 搜索共用）
const searchQuery = ref('')
const songList = ref([])
const songTotal = ref(0)
const songPage = ref(1)
const songTotalPages = ref(0)
const songLoading = ref(false)
let searchTimeout = null

const form = ref({
  title: '',
  coverUrl: '',
  coverNetworkUrl: '',
  type: '1',
  profile: '',
  coverFile: null,
  coverFilename: ''
})

const typeLabels = { 0: '用户自建', 1: '系统推荐', 2: '官方精选' }
const typeBadgeClass = {
  0: 'bg-primary/15 text-primary',
  1: 'bg-purple-500/15 text-purple-400',
  2: 'bg-surface-hover text-muted'
}

// 当前管理歌单中已有的歌曲
const playlistSongs = ref([])

async function loadPlaylists() {
  const result = await apiFetch('/playlists?systemOnly=true')
  playlists.value = result?.code === 200 ? result.data : []
}

async function loadPlaylistSongs(id) {
  const result = await apiFetch('/playlists/' + id)
  if (result?.code === 200 && result.data?.songList) {
    playlistSongs.value = result.data.songList
  } else {
    playlistSongs.value = []
  }
}

function openAdd() {
  editingId.value = null
  form.value = { title: '', coverUrl: '', coverNetworkUrl: '', type: '1', profile: '', coverFile: null, coverFilename: '' }
  dialogOpen.value = true
}

async function openEdit(id) {
  const result = await apiFetch('/playlists/' + id)
  if (result?.code !== 200) return
  const p = result.data
  editingId.value = id
  form.value = {
    title: p.title || '',
    coverUrl: p.coverUrl || '',
    coverNetworkUrl: p.coverNetworkUrl || '',
    type: String(p.type ?? 0),
    profile: p.profile || '',
    coverFile: null,
    coverFilename: ''
  }
  dialogOpen.value = true
}

async function savePlaylist() {
  if (!form.value.title.trim()) return

  let coverUrl = form.value.coverUrl.trim()

  // 如果有文件需要上传
  if (form.value.coverFile && form.value.coverFilename.trim()) {
    const formData = new FormData()
    formData.append('cover', form.value.coverFile)
    formData.append('filename', form.value.coverFilename.trim())

    const token = localStorage.getItem('adminToken')
    const uploadResult = await fetch('/admin/spotify/playlists/upload-cover', {
      method: 'POST',
      headers: { 'token': token },
      body: formData
    }).then(r => r.json())

    if (uploadResult?.code === 200) {
      coverUrl = uploadResult.data.coverUrl
    } else {
      alert('封面上传失败: ' + (uploadResult?.msg || '未知错误'))
      return
    }
  }

  const data = {
    title: form.value.title.trim(),
    coverUrl: coverUrl,
    coverNetworkUrl: form.value.coverNetworkUrl.trim() || null,
    type: parseInt(form.value.type),
    profile: form.value.profile.trim() || ''
  }

  const url = editingId.value ? '/playlists/' + editingId.value : '/playlists'
  const method = editingId.value ? 'PUT' : 'POST'
  const result = await apiFetch(url, { method, body: JSON.stringify(data) })
  if (result?.code === 200) {
    dialogOpen.value = false
    loadPlaylists()
  }
}

async function deletePlaylist(id) {
  if (!confirm('确认删除该歌单？歌曲关联也会被移除。')) return
  const result = await apiFetch('/playlists/' + id, { method: 'DELETE' })
  if (result?.code === 200) loadPlaylists()
}

async function openSongsManager(id, title) {
  managingPlaylistId.value = id
  managingPlaylistTitle.value = title
  searchQuery.value = ''
  songPage.value = 1
  await loadPlaylistSongs(id)
  await fetchSongList()
  songsDialogOpen.value = true
}

async function fetchSongList() {
  songLoading.value = true
  try {
    const keyword = searchQuery.value.trim()
    let url = '/songs/search?page=' + songPage.value + '&pageSize=10'
    if (keyword.length >= 2) {
      url += '&keyword=' + encodeURIComponent(keyword)
    }
    const result = await apiFetch(url)
    if (result?.code === 200 && result.data) {
      songList.value = result.data.records || []
      songTotal.value = result.data.total || 0
      songTotalPages.value = result.data.totalPages || 0
    } else {
      songList.value = []
      songTotal.value = 0
      songTotalPages.value = 0
    }
  } catch {
    songList.value = []
    songTotal.value = 0
    songTotalPages.value = 0
  } finally {
    songLoading.value = false
  }
}

function onSearchInput() {
  if (searchTimeout) clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => {
    songPage.value = 1
    fetchSongList()
  }, 300)
}

function goToSongPage(page) {
  if (page < 1 || page > songTotalPages.value) return
  songPage.value = page
  fetchSongList()
}

function isSongInPlaylist(songId) {
  return playlistSongs.value.some(s => s.id === songId)
}

function getArtistDisplay(song) {
  const parts = []
  if (song.artistName) parts.push(song.artistName)
  if (song.featuredArtistNames?.length) parts.push(...song.featuredArtistNames)
  return parts.length ? parts.join(', ') : '未知艺人'
}

async function addSongToPlaylist(songId) {
  if (!songId) return
  const result = await apiFetch('/playlists/' + managingPlaylistId.value + '/songs', {
    method: 'POST',
    body: JSON.stringify({ songId: songId })
  })
  if (result?.code === 200) {
    await loadPlaylistSongs(managingPlaylistId.value)
  }
}

async function removeSongFromPlaylist(songId) {
  const result = await apiFetch('/playlists/' + managingPlaylistId.value + '/songs/' + songId, {
    method: 'DELETE'
  })
  if (result?.code === 200) {
    await loadPlaylistSongs(managingPlaylistId.value)
  }
}

function formatType(t) {
  return typeLabels[t] || '未知'
}

onMounted(() => {
  loadPlaylists()
})
</script>

<template>
  <div class="animate-fade-in">
    <!-- Header -->
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-bold text-foreground">系统歌单</h1>
        <p class="text-sm text-muted mt-1">管理系统推荐和官方精选歌单</p>
      </div>
      <Button @click="openAdd">
        <Plus class="w-4 h-4 mr-2" />
        新建歌单
      </Button>
    </div>

    <!-- Grid -->
    <div v-if="playlists.length" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
      <div
        v-for="p in playlists"
        :key="p.id"
        class="group bg-card border border-border rounded-xl overflow-hidden hover:border-border-hover hover:bg-card-hover transition-all duration-200"
      >
        <!-- Cover -->
        <div class="aspect-square bg-surface-hover relative">
          <img
            v-if="p.coverNetworkUrl || p.coverUrl"
            :src="p.coverNetworkUrl || p.coverUrl"
            :alt="p.title"
            class="w-full h-full object-cover"
            @error="$event.target.style.display='none'"
          />
          <div v-if="!p.coverNetworkUrl && !p.coverUrl" class="absolute inset-0 flex items-center justify-center">
            <ListMusic class="w-12 h-12 text-muted" />
          </div>
          <!-- Hover actions -->
          <div class="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity duration-200 flex items-center justify-center gap-2">
            <button
              @click="openSongsManager(p.id, p.title)"
              class="p-2.5 rounded-full bg-primary text-primary-foreground hover:bg-primary-hover transition-colors cursor-pointer"
              aria-label="Manage songs"
            >
              <Music class="w-4 h-4" />
            </button>
            <button
              @click="openEdit(p.id)"
              class="p-2.5 rounded-full bg-surface-hover text-foreground hover:bg-surface-active transition-colors cursor-pointer"
              aria-label="Edit playlist"
            >
              <Pencil class="w-4 h-4" />
            </button>
            <button
              @click="deletePlaylist(p.id)"
              class="p-2.5 rounded-full bg-surface-hover text-destructive hover:bg-destructive hover:text-destructive-foreground transition-colors cursor-pointer"
              aria-label="Delete playlist"
            >
              <Trash2 class="w-4 h-4" />
            </button>
          </div>
        </div>
        <!-- Info -->
        <div class="p-4">
          <div class="font-semibold text-foreground truncate">{{ p.title }}</div>
          <div class="flex items-center gap-2 mt-2">
            <span :class="['text-xs px-2 py-0.5 rounded-full font-medium', typeBadgeClass[p.type] || typeBadgeClass[0]]">
              {{ formatType(p.type) }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div
      v-else
      class="flex flex-col items-center justify-center py-20 border border-border rounded-xl bg-card"
    >
      <div class="w-14 h-14 rounded-2xl bg-surface-hover flex items-center justify-center mb-4">
        <ListMusic class="w-6 h-6 text-muted" />
      </div>
      <h3 class="text-base font-semibold text-foreground mb-1">暂无系统歌单</h3>
      <p class="text-sm text-muted mb-4">创建你的第一个系统歌单</p>
      <Button @click="openAdd" variant="secondary">
        <Plus class="w-4 h-4 mr-2" />
        新建歌单
      </Button>
    </div>

    <!-- Create/Edit Dialog -->
    <Dialog
      v-model:open="dialogOpen"
      :title="editingId ? '编辑歌单' : '新建系统歌单'"
      :description="editingId ? '修改歌单信息' : '创建新的系统推荐或官方精选歌单'"
    >
      <form @submit.prevent="savePlaylist" class="space-y-4">
        <div class="space-y-2">
          <Label for="pl-title">歌单名称 <span class="text-destructive">*</span></Label>
          <Input id="pl-title" v-model="form.title" placeholder="例如：每日推荐、热门精选" />
        </div>
        <div class="space-y-2">
          <Label for="pl-profile">简介</Label>
          <Input id="pl-profile" v-model="form.profile" placeholder="歌单简介（可选）" />
        </div>
        <div class="space-y-2">
          <Label for="pl-cover-file">封面图片</Label>
          <input
            id="pl-cover-file"
            type="file"
            accept="image/*"
            @change="form.coverFile = $event.target.files[0]"
            class="flex h-10 w-full rounded-lg border border-border bg-surface px-3 py-2.5 text-sm text-foreground file:mr-3 file:rounded file:border-0 file:bg-surface-hover file:px-3 file:py-1 file:text-sm file:text-foreground cursor-pointer"
          />
        </div>
        <div class="space-y-2">
          <Label for="pl-cover-name">文件名 <span class="text-destructive">*</span></Label>
          <Input id="pl-cover-name" v-model="form.coverFilename" placeholder="输入保存的文件名（不含扩展名）" />
        </div>
        <div class="space-y-2">
          <Label for="pl-cover-network">封面网络URL</Label>
          <Input id="pl-cover-network" v-model="form.coverNetworkUrl" placeholder="https://example.com/cover.jpg" />
          <p class="text-xs text-muted">填写后优先使用网络图片</p>
        </div>
        <div class="space-y-2">
          <Label for="pl-type">歌单类型</Label>
          <select
            id="pl-type"
            v-model="form.type"
            class="flex h-10 w-full rounded-lg border border-border bg-surface px-3 py-2.5 text-sm text-foreground transition-colors duration-150 hover:border-border-hover focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary/30 cursor-pointer"
          >
            <option value="1">系统推荐</option>
            <option value="2">官方精选</option>
          </select>
        </div>
        <div class="flex justify-end gap-3 pt-2">
          <Button type="button" variant="ghost" @click="dialogOpen = false">取消</Button>
          <Button type="submit" :disabled="loading || !form.title.trim()">
            <Loader2 v-if="loading" class="w-4 h-4 animate-spin mr-2" />
            {{ editingId ? '保存修改' : '创建歌单' }}
          </Button>
        </div>
      </form>
    </Dialog>

    <!-- Manage Songs Dialog -->
    <Dialog
      v-model:open="songsDialogOpen"
      :title="'管理歌曲 — ' + managingPlaylistTitle"
      description="浏览或搜索歌曲，点击添加到歌单"
      class="max-w-2xl"
    >
      <div class="space-y-4">
        <!-- Search input -->
        <div class="relative">
          <Input
            v-model="searchQuery"
            placeholder="搜索歌曲名或艺术家名（留空则浏览全部）..."
            @input="onSearchInput"
            class="pr-8"
          />
          <div v-if="songLoading" class="absolute right-3 top-1/2 -translate-y-1/2">
            <Loader2 class="w-4 h-4 animate-spin text-muted" />
          </div>
        </div>

        <!-- Song list (browse / search results) -->
        <div>
          <div class="text-xs text-muted mb-2">
            <span v-if="searchQuery.trim().length >= 2">搜索结果：共 {{ songTotal }} 首</span>
            <span v-else>全部歌曲：共 {{ songTotal }} 首</span>
          </div>

          <div v-if="songList.length" class="border border-border rounded-lg overflow-hidden">
            <div
              v-for="song in songList"
              :key="song.id"
              class="flex items-center gap-3 px-4 py-3 border-b border-border/50 last:border-0 hover:bg-surface-hover/30 transition-colors"
            >
              <div class="w-8 h-8 rounded bg-surface-hover flex items-center justify-center flex-shrink-0 overflow-hidden">
                <img
                  v-if="song.coverNetworkUrl || song.coverUrl"
                  :src="song.coverNetworkUrl || song.coverUrl"
                  class="w-full h-full object-cover"
                  @error="$event.target.style.display='none'"
                />
                <Music v-else class="w-3 h-3 text-muted" />
              </div>
              <div class="flex-1 min-w-0">
                <div class="text-sm font-medium text-foreground truncate">{{ song.title }}</div>
                <div class="text-xs text-muted truncate">{{ getArtistDisplay(song) }}</div>
              </div>
              <Button
                v-if="!isSongInPlaylist(song.id)"
                size="sm"
                variant="ghost"
                @click="addSongToPlaylist(song.id)"
                :disabled="loading"
              >
                <Plus class="w-3 h-3 mr-1" />
                添加
              </Button>
              <span v-else class="text-xs text-muted px-2 py-1 rounded bg-surface-hover">
                已添加
              </span>
            </div>
          </div>
          <div v-else-if="!songLoading" class="text-center py-6 text-sm text-muted">
            <span v-if="searchQuery.trim().length >= 2">未找到匹配的歌曲</span>
            <span v-else>暂无歌曲</span>
          </div>

          <!-- Pagination -->
          <div v-if="songTotalPages > 1" class="flex items-center justify-center gap-2 mt-3">
            <button
              @click="goToSongPage(songPage - 1)"
              :disabled="songPage <= 1"
              class="px-3 py-1 text-xs rounded border border-border hover:bg-surface-hover disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
            >
              上一页
            </button>
            <span class="text-xs text-muted">{{ songPage }} / {{ songTotalPages }}</span>
            <button
              @click="goToSongPage(songPage + 1)"
              :disabled="songPage >= songTotalPages"
              class="px-3 py-1 text-xs rounded border border-border hover:bg-surface-hover disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
            >
              下一页
            </button>
          </div>
        </div>

        <!-- Current songs -->
        <div>
          <div class="text-sm font-medium text-foreground mb-2">已添加的歌曲 ({{ playlistSongs.length }})</div>
          <div v-if="playlistSongs.length" class="border border-border rounded-lg overflow-hidden max-h-60 overflow-y-auto">
            <div
              v-for="(song, index) in playlistSongs"
              :key="song.id"
              class="flex items-center gap-3 px-4 py-3 border-b border-border/50 last:border-0 hover:bg-surface-hover/30 transition-colors"
            >
              <span class="text-xs text-muted-foreground w-5 text-center tabular-nums">{{ index + 1 }}</span>
              <div class="w-8 h-8 rounded bg-surface-hover flex items-center justify-center flex-shrink-0 overflow-hidden">
                <img
                  v-if="song.coverNetworkUrl || song.coverUrl"
                  :src="song.coverNetworkUrl || song.coverUrl"
                  class="w-full h-full object-cover"
                  @error="$event.target.style.display='none'"
                />
                <Music v-else class="w-3 h-3 text-muted" />
              </div>
              <div class="flex-1 min-w-0">
                <div class="text-sm font-medium text-foreground truncate">{{ song.title }}</div>
                <div class="text-xs text-muted truncate">{{ getArtistDisplay(song) }}</div>
              </div>
              <button
                @click="removeSongFromPlaylist(song.id)"
                class="p-1.5 rounded-lg text-muted hover:text-destructive hover:bg-destructive/10 transition-colors cursor-pointer flex-shrink-0"
                aria-label="Remove song"
              >
                <X class="w-4 h-4" />
              </button>
            </div>
          </div>
          <div v-else class="text-center py-6 text-sm text-muted">
            歌单暂无歌曲，请从上方添加
          </div>
        </div>
      </div>
    </Dialog>
  </div>
</template>
