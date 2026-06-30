<script setup>
import { ref, computed, onMounted } from 'vue'
import { useApi } from '../composables/useApi'
import Button from '../components/ui/Button.vue'
import Input from '../components/ui/Input.vue'
import Label from '../components/ui/Label.vue'
import Dialog from '../components/ui/Dialog.vue'
import { Music, Plus, Pencil, Trash2, Loader2, Disc3, X, Image, AudioLines, Search, UserPlus, Check } from 'lucide-vue-next'

const { apiFetch, loading } = useApi()

const songs = ref([])
const artists = ref([])
const dialogOpen = ref(false)
const editingId = ref(null)

const form = ref({
  title: '',
  artistId: null,
  duration: '',
  coverNetworkUrl: ''
})
const featuredArtistIds = ref([])
const coverFile = ref(null)
const musicFile = ref(null)
const coverPreview = ref('')
const musicFileName = ref('')
const uploading = ref(false)

// 副艺术家搜索
const featuredSearch = ref('')

// 过滤后的艺术家列表（排除已选主艺术家）
const filteredArtists = computed(() => {
  const keyword = featuredSearch.value.trim().toLowerCase()
  return artists.value.filter(a => {
    if (a.id === form.value.artistId) return false
    if (keyword && !a.name.toLowerCase().includes(keyword)) return false
    return true
  })
})

async function loadSongs() {
  const result = await apiFetch('/songs')
  songs.value = result?.code === 200 ? result.data : []
}

async function loadArtists() {
  const result = await apiFetch('/artists')
  artists.value = result?.code === 200 ? result.data : []
}

function openAdd() {
  editingId.value = null
  form.value = { title: '', artistId: null, duration: '', coverNetworkUrl: '' }
  featuredArtistIds.value = []
  coverFile.value = null
  musicFile.value = null
  coverPreview.value = ''
  musicFileName.value = ''
  featuredSearch.value = ''
  dialogOpen.value = true
}

async function openEdit(id) {
  const result = await apiFetch('/songs/' + id)
  if (result?.code !== 200) return
  const { song, featuredArtistIds: featIds } = result.data
  editingId.value = id
  form.value = {
    title: song.title || '',
    artistId: song.artistId || null,
    duration: song.duration ?? '',
    coverNetworkUrl: song.coverNetworkUrl || ''
  }
  featuredArtistIds.value = featIds || []
  coverFile.value = null
  musicFile.value = null
  coverPreview.value = song.coverUrl || ''
  musicFileName.value = song.fileUrl ? song.fileUrl.split('/').pop() : ''
  featuredSearch.value = ''
  dialogOpen.value = true
}

function onCoverChange(e) {
  const file = e.target.files[0]
  if (!file) return
  coverFile.value = file
  const reader = new FileReader()
  reader.onload = (ev) => { coverPreview.value = ev.target.result }
  reader.readAsDataURL(file)
}

function onMusicChange(e) {
  const file = e.target.files[0]
  if (!file) return
  musicFile.value = file
  musicFileName.value = file.name
}

function clearCover() {
  coverFile.value = null
  coverPreview.value = ''
}

function clearMusic() {
  musicFile.value = null
  musicFileName.value = ''
}

function toggleFeatured(artistId) {
  const idx = featuredArtistIds.value.indexOf(artistId)
  if (idx === -1) {
    featuredArtistIds.value.push(artistId)
  } else {
    featuredArtistIds.value.splice(idx, 1)
  }
}

function removeFeatured(artistId) {
  featuredArtistIds.value = featuredArtistIds.value.filter(id => id !== artistId)
}

function getArtistName(id) {
  if (!id) return ''
  const a = artists.value.find(a => a.id === id)
  return a?.name || ''
}

function getArtistNamesFromIds(ids) {
  if (!ids || !ids.length) return []
  return ids.map(id => getArtistName(id)).filter(Boolean)
}

async function uploadFiles() {
  if (!coverFile.value && !musicFile.value) return { coverUrl: '', fileUrl: '' }

  const formData = new FormData()
  formData.append('title', form.value.title.trim())
  // 传递主艺术家名称用于文件路径
  const mainArtistName = getArtistName(form.value.artistId)
  if (mainArtistName) formData.append('artistName', mainArtistName)
  if (coverFile.value) formData.append('cover', coverFile.value)
  if (musicFile.value) formData.append('music', musicFile.value)

  const token = localStorage.getItem('adminToken')
  const response = await fetch('/admin/spotify/songs/upload', {
    method: 'POST',
    headers: { 'token': token },
    body: formData
  })
  const result = await response.json()
  if (result.code !== 200) throw new Error(result.message || '上传失败')
  return result.data
}

async function saveSong() {
  if (!form.value.title.trim()) return

  uploading.value = true
  try {
    // 新建歌曲时必须有文件
    if (!editingId.value && (!coverFile.value || !musicFile.value)) {
      alert('请上传封面图片和音频文件')
      return
    }

    // 先上传文件
    const urls = await uploadFiles()

    const data = {
      title: form.value.title.trim(),
      artistId: form.value.artistId || null,
      duration: form.value.duration !== '' ? parseInt(form.value.duration) : null,
      featuredArtistIds: featuredArtistIds.value,
      coverNetworkUrl: form.value.coverNetworkUrl.trim() || null
    }
    if (urls.coverUrl) data.coverUrl = urls.coverUrl
    if (urls.fileUrl) data.fileUrl = urls.fileUrl

    const url = editingId.value ? '/songs/' + editingId.value : '/songs'
    const method = editingId.value ? 'PUT' : 'POST'
    const result = await apiFetch(url, { method, body: JSON.stringify(data) })
    if (result?.code === 200) {
      dialogOpen.value = false
      loadSongs()
    }
  } catch (e) {
    alert('保存失败: ' + e.message)
  } finally {
    uploading.value = false
  }
}

async function deleteSong(id) {
  if (!confirm('确认删除这首歌曲？')) return
  const result = await apiFetch('/songs/' + id, { method: 'DELETE' })
  if (result?.code === 200) loadSongs()
}

function formatDuration(seconds) {
  if (!seconds) return '-'
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${m}:${String(s).padStart(2, '0')}`
}

onMounted(() => {
  loadSongs()
  loadArtists()
})
</script>

<template>
  <div class="animate-fade-in">
    <!-- Header -->
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-bold text-foreground">歌曲管理</h1>
        <p class="text-sm text-muted mt-1">管理平台上的所有音乐曲目</p>
      </div>
      <Button @click="openAdd">
        <Plus class="w-4 h-4 mr-2" />
        添加歌曲
      </Button>
    </div>

    <!-- Table -->
    <div v-if="songs.length" class="border border-border rounded-xl overflow-hidden">
      <table class="w-full text-sm">
        <thead>
          <tr class="border-b border-border bg-surface-hover/50">
            <th class="text-left px-4 py-3 font-medium text-muted">ID</th>
            <th class="text-left px-4 py-3 font-medium text-muted">封面</th>
            <th class="text-left px-4 py-3 font-medium text-muted">歌曲名</th>
            <th class="text-left px-4 py-3 font-medium text-muted">艺术家</th>
            <th class="text-left px-4 py-3 font-medium text-muted">时长</th>
            <th class="text-right px-4 py-3 font-medium text-muted">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="song in songs"
            :key="song.id"
            class="border-b border-border/50 last:border-0 hover:bg-surface-hover/30 transition-colors"
          >
            <td class="px-4 py-3 text-muted tabular-nums">{{ song.id }}</td>
            <td class="px-4 py-3">
              <div class="w-10 h-10 rounded-md bg-surface-hover overflow-hidden flex items-center justify-center">
                <img
                  v-if="song.coverNetworkUrl || song.coverUrl"
                  :src="song.coverNetworkUrl || song.coverUrl"
                  :alt="song.title"
                  class="w-full h-full object-cover"
                  @error="$event.target.style.display='none'"
                />
                <Music v-else class="w-4 h-4 text-muted" />
              </div>
            </td>
            <td class="px-4 py-3 font-medium text-foreground">{{ song.title }}</td>
            <td class="px-4 py-3 text-muted">
              <span>{{ song.artistName || getArtistName(song.artistId) || '-' }}</span>
              <span v-if="song.featuredArtistNames && song.featuredArtistNames.length" class="text-xs text-muted/70">
                feat. {{ song.featuredArtistNames.join(', ') }}
              </span>
            </td>
            <td class="px-4 py-3 text-muted tabular-nums">{{ formatDuration(song.duration) }}</td>
            <td class="px-4 py-3">
              <div class="flex items-center justify-end gap-1">
                <button
                  @click="openEdit(song.id)"
                  class="p-2 rounded-lg text-muted hover:text-foreground hover:bg-surface-hover transition-colors cursor-pointer"
                  aria-label="Edit song"
                >
                  <Pencil class="w-4 h-4" />
                </button>
                <button
                  @click="deleteSong(song.id)"
                  class="p-2 rounded-lg text-muted hover:text-destructive hover:bg-destructive/10 transition-colors cursor-pointer"
                  aria-label="Delete song"
                >
                  <Trash2 class="w-4 h-4" />
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Empty State -->
    <div
      v-else
      class="flex flex-col items-center justify-center py-20 border border-border rounded-xl bg-card"
    >
      <div class="w-14 h-14 rounded-2xl bg-surface-hover flex items-center justify-center mb-4">
        <Disc3 class="w-6 h-6 text-muted" />
      </div>
      <h3 class="text-base font-semibold text-foreground mb-1">暂无歌曲</h3>
      <p class="text-sm text-muted mb-4">添加你的第一首歌曲</p>
      <Button @click="openAdd" variant="secondary">
        <Plus class="w-4 h-4 mr-2" />
        添加歌曲
      </Button>
    </div>

    <!-- Dialog -->
    <Dialog
      v-model:open="dialogOpen"
      :title="editingId ? '编辑歌曲' : '添加歌曲'"
      :description="editingId ? '修改歌曲信息' : '添加新歌曲到曲库'"
    >
      <form @submit.prevent="saveSong" class="space-y-4">
        <div class="space-y-2">
          <Label for="song-title">歌曲名称 <span class="text-destructive">*</span></Label>
          <Input id="song-title" v-model="form.title" placeholder="输入歌曲名称" />
        </div>

        <!-- 主艺术家选择 -->
        <div class="space-y-2">
          <Label for="song-artist">主艺术家</Label>
          <select
            id="song-artist"
            v-model="form.artistId"
            class="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm text-foreground ring-offset-background focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
          >
            <option :value="null" class="text-muted">-- 选择艺术家 --</option>
            <option v-for="artist in artists" :key="artist.id" :value="artist.id">
              {{ artist.name }}
            </option>
          </select>
        </div>

        <!-- 副艺术家选择 -->
        <div class="space-y-2">
          <Label>
            <UserPlus class="w-3.5 h-3.5 inline mr-1" />
            副艺术家 (feat.)
          </Label>

          <!-- 已选副艺术家标签 -->
          <div v-if="featuredArtistIds.length" class="flex flex-wrap gap-1.5 mb-2">
            <span
              v-for="fid in featuredArtistIds"
              :key="fid"
              class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs bg-primary/10 text-primary border border-primary/20"
            >
              {{ getArtistName(fid) }}
              <button type="button" @click="removeFeatured(fid)" class="hover:text-destructive cursor-pointer">
                <X class="w-3 h-3" />
              </button>
            </span>
          </div>

          <!-- 搜索 + 列表 -->
          <div class="border border-border rounded-lg overflow-hidden">
            <div class="flex items-center gap-2 px-3 py-2 border-b border-border bg-surface-hover/30">
              <Search class="w-4 h-4 text-muted flex-shrink-0" />
              <input
                v-model="featuredSearch"
                type="text"
                placeholder="搜索艺术家..."
                class="flex-1 bg-transparent text-sm outline-none text-foreground placeholder:text-muted"
              />
            </div>
            <div class="max-h-40 overflow-y-auto">
              <div v-if="filteredArtists.length === 0" class="px-3 py-4 text-center text-sm text-muted">
                没有匹配的艺术家
              </div>
              <label
                v-for="artist in filteredArtists"
                :key="artist.id"
                class="flex items-center gap-2.5 px-3 py-2 hover:bg-surface-hover/50 cursor-pointer transition-colors"
                @click.prevent="toggleFeatured(artist.id)"
              >
                <div
                  class="w-4 h-4 rounded border flex items-center justify-center flex-shrink-0 transition-colors"
                  :class="featuredArtistIds.includes(artist.id) ? 'bg-primary border-primary' : 'border-input'"
                >
                  <Check v-if="featuredArtistIds.includes(artist.id)" class="w-3 h-3 text-primary-foreground" />
                </div>
                <span class="text-sm text-foreground">{{ artist.name }}</span>
              </label>
            </div>
          </div>
        </div>

        <!-- Cover Upload -->
        <div class="space-y-2">
          <Label>封面图片 <span v-if="!editingId" class="text-destructive">*</span></Label>
          <div
            v-if="!coverPreview"
            class="border-2 border-dashed border-border rounded-lg p-6 text-center hover:border-primary/50 transition-colors cursor-pointer"
            @click="$refs.coverInput.click()"
          >
            <Image class="w-8 h-8 mx-auto text-muted mb-2" />
            <p class="text-sm text-muted">点击上传封面图片</p>
            <p class="text-xs text-muted mt-1">支持 JPG、PNG 格式</p>
          </div>
          <div v-else class="relative w-32 h-32 rounded-lg overflow-hidden border border-border">
            <img :src="coverPreview" class="w-full h-full object-cover" />
            <button
              type="button"
              @click="clearCover"
              class="absolute top-1 right-1 p-1 rounded-full bg-black/60 text-white hover:bg-black/80 cursor-pointer"
            >
              <X class="w-3 h-3" />
            </button>
          </div>
          <input
            ref="coverInput"
            type="file"
            accept="image/*"
            class="hidden"
            @change="onCoverChange"
          />
        </div>

        <div class="space-y-2">
          <Label for="cover-network-url">封面网络URL</Label>
          <Input id="cover-network-url" v-model="form.coverNetworkUrl" placeholder="https://example.com/cover.jpg" />
          <p class="text-xs text-muted">填写后优先使用网络图片</p>
        </div>

        <!-- Music Upload -->
        <div class="space-y-2">
          <Label>音频文件 <span v-if="!editingId" class="text-destructive">*</span></Label>
          <div
            v-if="!musicFileName"
            class="border-2 border-dashed border-border rounded-lg p-6 text-center hover:border-primary/50 transition-colors cursor-pointer"
            @click="$refs.musicInput.click()"
          >
            <AudioLines class="w-8 h-8 mx-auto text-muted mb-2" />
            <p class="text-sm text-muted">点击上传音频文件</p>
            <p class="text-xs text-muted mt-1">支持 MP3、WAV、FLAC 格式</p>
          </div>
          <div v-else class="flex items-center gap-3 p-3 bg-surface-hover rounded-lg border border-border">
            <AudioLines class="w-5 h-5 text-primary flex-shrink-0" />
            <span class="text-sm text-foreground truncate flex-1">{{ musicFileName }}</span>
            <button
              type="button"
              @click="clearMusic"
              class="p-1 rounded-full text-muted hover:text-destructive cursor-pointer"
            >
              <X class="w-4 h-4" />
            </button>
          </div>
          <input
            ref="musicInput"
            type="file"
            accept="audio/*"
            class="hidden"
            @change="onMusicChange"
          />
        </div>

        <div class="space-y-2">
          <Label for="song-duration">时长（秒）</Label>
          <Input id="song-duration" v-model="form.duration" type="number" placeholder="例如: 210" />
        </div>
        <div class="flex justify-end gap-3 pt-2">
          <Button type="button" variant="ghost" @click="dialogOpen = false">取消</Button>
          <Button type="submit" :disabled="uploading || !form.title.trim()">
            <Loader2 v-if="uploading" class="w-4 h-4 animate-spin mr-2" />
            {{ editingId ? '保存修改' : '添加歌曲' }}
          </Button>
        </div>
      </form>
    </Dialog>
  </div>
</template>
