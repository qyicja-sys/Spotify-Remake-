<script setup>
import { ref, onMounted, computed } from 'vue'
import { useApi } from '../composables/useApi'
import Button from '../components/ui/Button.vue'
import Input from '../components/ui/Input.vue'
import Label from '../components/ui/Label.vue'
import Textarea from '../components/ui/Textarea.vue'
import Dialog from '../components/ui/Dialog.vue'
import { Disc3, Mic2, Plus, Pencil, Trash2, Loader2, ArrowLeft, Image, X, Music } from 'lucide-vue-next'

const { apiFetch, loading } = useApi()

// 视图状态：'artists' 或 'albums'
const view = ref('artists')
const artists = ref([])
const selectedArtist = ref(null)
const albums = ref([])

// 专辑详情
const detailAlbum = ref(null)
const detailSongs = ref([])
const detailDialogOpen = ref(false)

// 编辑/新增专辑
const dialogOpen = ref(false)
const editingId = ref(null)
const form = ref({
  name: '',
  description: '',
  type: 2,
  coverUrl: '',
  coverNetworkUrl: ''
})
const coverFile = ref(null)
const coverPreview = ref('')
const uploading = ref(false)

const albumTypes = [
  { value: 0, label: 'Single' },
  { value: 1, label: 'EP' },
  { value: 2, label: 'Album' },
  { value: 3, label: 'Compilation' }
]

const albumTypeLabel = computed(() => {
  const t = albumTypes.find(t => t.value === form.value.type)
  return t ? t.label : 'Album'
})

function formatDuration(seconds) {
  if (!seconds) return '0:00'
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${m}:${String(s).padStart(2, '0')}`
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  return dateStr.substring(0, 10)
}

// ── 艺术家列表 ──────────────────────────────────────────────────────

async function loadArtists() {
  const result = await apiFetch('/albums/artists')
  artists.value = result?.code === 200 ? result.data : []
}

function selectArtist(artist) {
  selectedArtist.value = artist
  view.value = 'albums'
  loadAlbums(artist.id)
}

function backToArtists() {
  view.value = 'artists'
  selectedArtist.value = null
  albums.value = []
}

// ── 专辑列表 ──────────────────────────────────────────────────────

async function loadAlbums(artistId) {
  const result = await apiFetch('/albums/artist/' + artistId)
  if (result?.code === 200) {
    albums.value = result.data.albums || []
    if (result.data.artist) {
      selectedArtist.value = result.data.artist
    }
  }
}

function openAdd() {
  editingId.value = null
  form.value = { name: '', description: '', type: 2, coverUrl: '', coverNetworkUrl: '' }
  coverFile.value = null
  coverPreview.value = ''
  dialogOpen.value = true
}

async function openEdit(id) {
  const result = await apiFetch('/albums/' + id)
  if (result?.code !== 200) return
  const a = result.data.album
  editingId.value = id
  form.value = {
    name: a.name || '',
    description: a.description || '',
    type: a.type ?? 2,
    coverUrl: a.coverUrl || '',
    coverNetworkUrl: a.coverNetworkUrl || ''
  }
  coverFile.value = null
  coverPreview.value = a.coverUrl || ''
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

function clearCover() {
  coverFile.value = null
  coverPreview.value = form.value.coverUrl || ''
}

async function uploadCover() {
  if (!coverFile.value || !form.value.name.trim()) return null
  const formData = new FormData()
  formData.append('cover', coverFile.value)
  formData.append('albumName', form.value.name.trim())

  const token = localStorage.getItem('adminToken')
  const response = await fetch('/admin/spotify/albums/upload-cover', {
    method: 'POST',
    headers: { 'token': token },
    body: formData
  })
  const result = await response.json()
  if (result.code !== 200) throw new Error(result.message || '封面上传失败')
  return result.data.coverUrl
}

async function saveAlbum() {
  if (!form.value.name.trim()) return
  if (!selectedArtist.value) return

  uploading.value = true
  try {
    let coverUrl = form.value.coverUrl
    if (coverFile.value) {
      coverUrl = await uploadCover()
    }

    const data = {
      name: form.value.name.trim(),
      description: form.value.description.trim() || null,
      type: form.value.type,
      artistId: selectedArtist.value.id,
      coverUrl: coverUrl || '',
      coverNetworkUrl: form.value.coverNetworkUrl.trim() || null
    }

    const url = editingId.value ? '/albums/' + editingId.value : '/albums'
    const method = editingId.value ? 'PUT' : 'POST'
    const result = await apiFetch(url, { method, body: JSON.stringify(data) })
    if (result?.code === 200) {
      dialogOpen.value = false
      loadAlbums(selectedArtist.value.id)
    }
  } catch (e) {
    alert('保存失败: ' + e.message)
  } finally {
    uploading.value = false
  }
}

async function deleteAlbum(id) {
  if (!confirm('确认删除该专辑？专辑内的歌曲将变为单曲。')) return
  const result = await apiFetch('/albums/' + id, { method: 'DELETE' })
  if (result?.code === 200) loadAlbums(selectedArtist.value.id)
}

async function openDetail(id) {
  const result = await apiFetch('/albums/' + id)
  if (result?.code !== 200) return
  detailAlbum.value = result.data.album
  detailSongs.value = result.data.songs || []
  detailDialogOpen.value = true
}

onMounted(loadArtists)
</script>

<template>
  <div class="animate-fade-in">
    <!-- 艺术家列表视图 -->
    <template v-if="view === 'artists'">
      <div class="flex items-center justify-between mb-6">
        <div>
          <h1 class="text-2xl font-bold text-foreground">专辑管理</h1>
          <p class="text-sm text-muted mt-1">按艺术家浏览和管理专辑</p>
        </div>
      </div>

      <div v-if="artists.length" class="border border-border rounded-xl overflow-hidden">
        <table class="w-full text-sm">
          <thead>
            <tr class="border-b border-border bg-surface-hover/50">
              <th class="text-left px-4 py-3 font-medium text-muted">头像</th>
              <th class="text-left px-4 py-3 font-medium text-muted">艺术家</th>
              <th class="text-left px-4 py-3 font-medium text-muted">专辑数</th>
              <th class="text-right px-4 py-3 font-medium text-muted">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="artist in artists"
              :key="artist.id"
              class="border-b border-border/50 last:border-0 hover:bg-surface-hover/30 transition-colors cursor-pointer"
              @click="selectArtist(artist)"
            >
              <td class="px-4 py-3">
                <div class="w-10 h-10 rounded-full bg-surface-hover overflow-hidden flex items-center justify-center">
                  <img
                    v-if="artist.avatarNetworkUrl || artist.avatarUrl"
                    :src="artist.avatarNetworkUrl || artist.avatarUrl"
                    :alt="artist.name"
                    class="w-full h-full object-cover"
                    @error="$event.target.style.display='none'"
                  />
                  <Mic2 v-else class="w-4 h-4 text-muted" />
                </div>
              </td>
              <td class="px-4 py-3 font-medium text-foreground">{{ artist.name }}</td>
              <td class="px-4 py-3 text-muted tabular-nums">{{ artist.albumCount }}</td>
              <td class="px-4 py-3 text-right">
                <Button variant="ghost" size="sm" @click.stop="selectArtist(artist)">
                  查看专辑
                </Button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-else class="flex flex-col items-center justify-center py-20 border border-border rounded-xl bg-card">
        <div class="w-14 h-14 rounded-2xl bg-surface-hover flex items-center justify-center mb-4">
          <Disc3 class="w-6 h-6 text-muted" />
        </div>
        <h3 class="text-base font-semibold text-foreground mb-1">暂无专辑</h3>
        <p class="text-sm text-muted">还没有艺术家创建专辑</p>
      </div>
    </template>

    <!-- 专辑列表视图 -->
    <template v-if="view === 'albums'">
      <div class="flex items-center gap-3 mb-6">
        <button
          @click="backToArtists"
          class="p-2 rounded-lg text-muted hover:text-foreground hover:bg-surface-hover transition-colors cursor-pointer"
        >
          <ArrowLeft class="w-5 h-5" />
        </button>
        <div class="flex-1">
          <h1 class="text-2xl font-bold text-foreground">
            {{ selectedArtist?.name }} 的专辑
          </h1>
          <p class="text-sm text-muted mt-1">共 {{ albums.length }} 张专辑</p>
        </div>
        <Button @click="openAdd">
          <Plus class="w-4 h-4 mr-2" />
          新建专辑
        </Button>
      </div>

      <div v-if="albums.length" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        <div
          v-for="album in albums"
          :key="album.id"
          class="group bg-card border border-border rounded-xl overflow-hidden hover:border-border-hover hover:bg-card-hover transition-all duration-200 cursor-pointer"
          @click="openDetail(album.id)"
        >
          <div class="aspect-square bg-surface-hover relative overflow-hidden">
            <img
              v-if="album.coverNetworkUrl || album.coverUrl"
              :src="album.coverNetworkUrl || album.coverUrl"
              :alt="album.name"
              class="w-full h-full object-cover"
              @error="$event.target.style.display='none'"
            />
            <Disc3 v-else class="w-12 h-12 text-muted absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2" />
          </div>
          <div class="p-4">
            <h3 class="font-semibold text-foreground truncate">{{ album.name }}</h3>
            <div class="flex items-center gap-2 mt-1">
              <span class="text-xs text-muted bg-surface-hover px-2 py-0.5 rounded">
                {{ albumTypes.find(t => t.value === album.type)?.label || 'Album' }}
              </span>
              <span class="text-xs text-muted">{{ formatDate(album.releaseDate) }}</span>
            </div>
            <p v-if="album.description" class="text-xs text-muted mt-2 line-clamp-2">{{ album.description }}</p>
            <div class="flex items-center justify-end gap-1 mt-3 opacity-0 group-hover:opacity-100 transition-opacity">
              <button
                @click.stop="openEdit(album.id)"
                class="p-2 rounded-lg text-muted hover:text-foreground hover:bg-surface-hover transition-colors cursor-pointer"
              >
                <Pencil class="w-4 h-4" />
              </button>
              <button
                @click.stop="deleteAlbum(album.id)"
                class="p-2 rounded-lg text-muted hover:text-destructive hover:bg-destructive/10 transition-colors cursor-pointer"
              >
                <Trash2 class="w-4 h-4" />
              </button>
            </div>
          </div>
        </div>
      </div>

      <div v-else class="flex flex-col items-center justify-center py-20 border border-border rounded-xl bg-card">
        <div class="w-14 h-14 rounded-2xl bg-surface-hover flex items-center justify-center mb-4">
          <Disc3 class="w-6 h-6 text-muted" />
        </div>
        <h3 class="text-base font-semibold text-foreground mb-1">暂无专辑</h3>
        <p class="text-sm text-muted mb-4">该艺术家还没有专辑</p>
        <Button @click="openAdd" variant="secondary">
          <Plus class="w-4 h-4 mr-2" />
          新建专辑
        </Button>
      </div>
    </template>

    <!-- 编辑/新增专辑对话框 -->
    <Dialog
      v-model:open="dialogOpen"
      :title="editingId ? '编辑专辑' : '新建专辑'"
      :description="editingId ? '修改专辑信息' : '为 ' + (selectedArtist?.name || '') + ' 创建新专辑'"
    >
      <form @submit.prevent="saveAlbum" class="space-y-4">
        <!-- 封面上传 -->
        <div class="space-y-2">
          <Label>封面</Label>
          <div
            v-if="!coverPreview"
            class="border-2 border-dashed border-border rounded-lg p-6 text-center hover:border-primary/50 transition-colors cursor-pointer"
            @click="$refs.coverInput.click()"
          >
            <Image class="w-8 h-8 mx-auto text-muted mb-2" />
            <p class="text-sm text-muted">点击上传封面图片</p>
          </div>
          <div v-else class="flex items-center gap-4">
            <div class="relative w-20 h-20 rounded-lg overflow-hidden border border-border flex-shrink-0">
              <img :src="coverPreview" class="w-full h-full object-cover" />
              <button
                type="button"
                @click="clearCover"
                class="absolute top-0.5 right-0.5 p-0.5 rounded-full bg-black/60 text-white hover:bg-black/80 cursor-pointer"
              >
                <X class="w-3 h-3" />
              </button>
            </div>
            <div class="text-sm text-muted">
              <p v-if="coverFile">{{ coverFile.name }}</p>
              <p v-else>当前封面</p>
              <button type="button" @click="$refs.coverInput.click()" class="text-primary text-xs hover:underline mt-1 cursor-pointer">
                更换图片
              </button>
            </div>
          </div>
          <input ref="coverInput" type="file" accept="image/*" class="hidden" @change="onCoverChange" />
        </div>

        <div class="space-y-2">
          <Label for="album-name">专辑名 <span class="text-destructive">*</span></Label>
          <Input id="album-name" v-model="form.name" placeholder="输入专辑名" />
        </div>

        <div class="grid grid-cols-2 gap-4">
          <div class="space-y-2">
            <Label for="album-type">类型</Label>
            <select
              id="album-type"
              v-model="form.type"
              class="w-full h-10 rounded-lg border border-border bg-surface px-3 py-2 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-primary/50"
            >
              <option v-for="t in albumTypes" :key="t.value" :value="t.value">{{ t.label }}</option>
            </select>
          </div>
          <div class="space-y-2">
            <Label for="cover-network-url">封面网络URL</Label>
            <Input id="cover-network-url" v-model="form.coverNetworkUrl" placeholder="https://..." />
          </div>
        </div>

        <div class="space-y-2">
          <Label for="album-desc">简介</Label>
          <Textarea id="album-desc" v-model="form.description" placeholder="专辑简介（可选）" :rows="3" />
        </div>

        <div class="flex justify-end gap-3 pt-2">
          <Button type="button" variant="ghost" @click="dialogOpen = false">取消</Button>
          <Button type="submit" :disabled="uploading || loading || !form.name.trim()">
            <Loader2 v-if="uploading" class="w-4 h-4 animate-spin mr-2" />
            {{ editingId ? '保存修改' : '创建专辑' }}
          </Button>
        </div>
      </form>
    </Dialog>

    <!-- 专辑详情对话框 -->
    <Dialog
      v-model:open="detailDialogOpen"
      :title="detailAlbum?.name || '专辑详情'"
      :description="detailAlbum?.description || ''"
    >
      <div v-if="detailAlbum" class="space-y-4">
        <div class="flex items-center gap-4">
          <div class="w-24 h-24 rounded-lg overflow-hidden bg-surface-hover flex-shrink-0">
            <img
              v-if="detailAlbum.coverNetworkUrl || detailAlbum.coverUrl"
              :src="detailAlbum.coverNetworkUrl || detailAlbum.coverUrl"
              class="w-full h-full object-cover"
            />
            <Disc3 v-else class="w-8 h-8 text-muted m-auto mt-8" />
          </div>
          <div>
            <h3 class="text-lg font-bold text-foreground">{{ detailAlbum.name }}</h3>
            <div class="flex items-center gap-2 mt-1">
              <span class="text-xs text-muted bg-surface-hover px-2 py-0.5 rounded">
                {{ albumTypes.find(t => t.value === detailAlbum.type)?.label || 'Album' }}
              </span>
              <span class="text-xs text-muted">{{ formatDate(detailAlbum.releaseDate) }}</span>
            </div>
          </div>
        </div>

        <!-- 歌曲列表 -->
        <div>
          <h4 class="text-sm font-semibold text-foreground mb-2">歌曲 ({{ detailSongs.length }})</h4>
          <div v-if="detailSongs.length" class="border border-border rounded-lg overflow-hidden">
            <table class="w-full text-sm">
              <thead>
                <tr class="border-b border-border bg-surface-hover/50">
                  <th class="text-left px-3 py-2 font-medium text-muted w-10">#</th>
                  <th class="text-left px-3 py-2 font-medium text-muted">标题</th>
                  <th class="text-right px-3 py-2 font-medium text-muted">时长</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="(song, idx) in detailSongs"
                  :key="song.id"
                  class="border-b border-border/50 last:border-0"
                >
                  <td class="px-3 py-2 text-muted tabular-nums">{{ idx + 1 }}</td>
                  <td class="px-3 py-2">
                    <div class="flex items-center gap-2">
                      <div class="w-8 h-8 rounded overflow-hidden bg-surface-hover flex-shrink-0">
                        <img
                          v-if="song.coverUrl"
                          :src="song.coverUrl"
                          class="w-full h-full object-cover"
                          @error="$event.target.style.display='none'"
                        />
                        <Music v-else class="w-3 h-3 text-muted m-auto mt-2.5" />
                      </div>
                      <span class="font-medium text-foreground truncate">{{ song.title }}</span>
                    </div>
                  </td>
                  <td class="px-3 py-2 text-muted text-right tabular-nums">{{ formatDuration(song.duration) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <p v-else class="text-sm text-muted">专辑内暂无歌曲</p>
        </div>
      </div>
    </Dialog>
  </div>
</template>
