<script setup>
import { ref, onMounted } from 'vue'
import { useApi } from '../composables/useApi'
import Button from '../components/ui/Button.vue'
import Input from '../components/ui/Input.vue'
import Label from '../components/ui/Label.vue'
import Textarea from '../components/ui/Textarea.vue'
import Dialog from '../components/ui/Dialog.vue'
import { Mic2, Plus, Pencil, Trash2, Loader2, Users, Image, X } from 'lucide-vue-next'

const { apiFetch, loading } = useApi()

const artists = ref([])
const dialogOpen = ref(false)
const editingId = ref(null)

const form = ref({
  name: '',
  fansCount: '',
  monthlyListeners: '',
  avatarUrl: '',
  avatarNetworkUrl: '',
  biography: ''
})
const avatarFile = ref(null)
const avatarPreview = ref('')
const uploading = ref(false)

async function loadArtists() {
  const result = await apiFetch('/artists')
  artists.value = result?.code === 200 ? result.data : []
}

function openAdd() {
  editingId.value = null
  form.value = { name: '', fansCount: '', monthlyListeners: '', avatarUrl: '', avatarNetworkUrl: '', biography: '' }
  avatarFile.value = null
  avatarPreview.value = ''
  dialogOpen.value = true
}

async function openEdit(id) {
  const result = await apiFetch('/artists/' + id)
  if (result?.code !== 200) return
  const a = result.data
  editingId.value = id
  form.value = {
    name: a.name || '',
    fansCount: a.fansCount ?? '',
    monthlyListeners: a.monthlyListeners ?? '',
    avatarUrl: a.avatarUrl || '',
    avatarNetworkUrl: a.avatarNetworkUrl || '',
    biography: a.biography || ''
  }
  avatarFile.value = null
  avatarPreview.value = a.avatarUrl || ''
  dialogOpen.value = true
}

function onAvatarChange(e) {
  const file = e.target.files[0]
  if (!file) return
  avatarFile.value = file
  const reader = new FileReader()
  reader.onload = (ev) => { avatarPreview.value = ev.target.result }
  reader.readAsDataURL(file)
}

function clearAvatar() {
  avatarFile.value = null
  avatarPreview.value = form.value.avatarUrl || ''
}

async function uploadAvatar() {
  if (!avatarFile.value || !form.value.name.trim()) return null
  const formData = new FormData()
  formData.append('avatar', avatarFile.value)
  formData.append('artistName', form.value.name.trim())

  const token = localStorage.getItem('adminToken')
  const response = await fetch('/admin/spotify/artists/upload-avatar', {
    method: 'POST',
    headers: { 'token': token },
    body: formData
  })
  const result = await response.json()
  if (result.code !== 200) throw new Error(result.message || '头像上传失败')
  return result.data
}

async function saveArtist() {
  if (!form.value.name.trim()) return

  uploading.value = true
  try {
    // 有新头像则先上传
    let avatarUrl = form.value.avatarUrl
    if (avatarFile.value) {
      const urls = await uploadAvatar()
      if (urls) avatarUrl = urls.artistAvatarUrl
    }

    const data = {
      name: form.value.name.trim(),
      fansCount: form.value.fansCount !== '' ? parseInt(form.value.fansCount) : null,
      monthlyListeners: form.value.monthlyListeners !== '' ? parseInt(form.value.monthlyListeners) : null,
      avatarUrl: avatarUrl || '',
      avatarNetworkUrl: form.value.avatarNetworkUrl.trim() || null,
      biography: form.value.biography.trim()
    }

    const url = editingId.value ? '/artists/' + editingId.value : '/artists'
    const method = editingId.value ? 'PUT' : 'POST'
    const result = await apiFetch(url, { method, body: JSON.stringify(data) })
    if (result?.code === 200) {
      dialogOpen.value = false
      loadArtists()
    }
  } catch (e) {
    alert('保存失败: ' + e.message)
  } finally {
    uploading.value = false
  }
}

async function deleteArtist(id) {
  if (!confirm('确认删除该艺术家？关联的歌曲可能受影响。')) return
  const result = await apiFetch('/artists/' + id, { method: 'DELETE' })
  if (result?.code === 200) loadArtists()
}

function formatNumber(n) {
  if (!n) return '-'
  return n.toLocaleString()
}

onMounted(loadArtists)
</script>

<template>
  <div class="animate-fade-in">
    <!-- Header -->
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-bold text-foreground">艺术家管理</h1>
        <p class="text-sm text-muted mt-1">管理平台上的所有艺术家</p>
      </div>
      <Button @click="openAdd">
        <Plus class="w-4 h-4 mr-2" />
        添加艺术家
      </Button>
    </div>

    <!-- Table -->
    <div v-if="artists.length" class="border border-border rounded-xl overflow-hidden">
      <table class="w-full text-sm">
        <thead>
          <tr class="border-b border-border bg-surface-hover/50">
            <th class="text-left px-4 py-3 font-medium text-muted">ID</th>
            <th class="text-left px-4 py-3 font-medium text-muted">头像</th>
            <th class="text-left px-4 py-3 font-medium text-muted">名称</th>
            <th class="text-left px-4 py-3 font-medium text-muted">粉丝数</th>
            <th class="text-left px-4 py-3 font-medium text-muted">月听众</th>
            <th class="text-right px-4 py-3 font-medium text-muted">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="artist in artists"
            :key="artist.id"
            class="border-b border-border/50 last:border-0 hover:bg-surface-hover/30 transition-colors"
          >
            <td class="px-4 py-3 text-muted tabular-nums">{{ artist.id }}</td>
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
            <td class="px-4 py-3 text-muted tabular-nums">{{ formatNumber(artist.fansCount) }}</td>
            <td class="px-4 py-3 text-muted tabular-nums">{{ formatNumber(artist.monthlyListeners) }}</td>
            <td class="px-4 py-3">
              <div class="flex items-center justify-end gap-1">
                <button
                  @click="openEdit(artist.id)"
                  class="p-2 rounded-lg text-muted hover:text-foreground hover:bg-surface-hover transition-colors cursor-pointer"
                  aria-label="Edit artist"
                >
                  <Pencil class="w-4 h-4" />
                </button>
                <button
                  @click="deleteArtist(artist.id)"
                  class="p-2 rounded-lg text-muted hover:text-destructive hover:bg-destructive/10 transition-colors cursor-pointer"
                  aria-label="Delete artist"
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
        <Users class="w-6 h-6 text-muted" />
      </div>
      <h3 class="text-base font-semibold text-foreground mb-1">暂无艺术家</h3>
      <p class="text-sm text-muted mb-4">添加你的第一位艺术家</p>
      <Button @click="openAdd" variant="secondary">
        <Plus class="w-4 h-4 mr-2" />
        添加艺术家
      </Button>
    </div>

    <!-- Dialog -->
    <Dialog
      v-model:open="dialogOpen"
      :title="editingId ? '编辑艺术家' : '添加艺术家'"
      :description="editingId ? '修改艺术家资料信息' : '添加新的艺术家到平台'"
    >
      <form @submit.prevent="saveArtist" class="space-y-4">
        <div class="space-y-2">
          <Label for="artist-name">名称 <span class="text-destructive">*</span></Label>
          <Input id="artist-name" v-model="form.name" placeholder="输入艺术家名称" />
        </div>
        <div class="grid grid-cols-2 gap-4">
          <div class="space-y-2">
            <Label for="artist-fans">粉丝数</Label>
            <Input id="artist-fans" v-model="form.fansCount" type="number" placeholder="0" />
          </div>
          <div class="space-y-2">
            <Label for="artist-listeners">月听众</Label>
            <Input id="artist-listeners" v-model="form.monthlyListeners" type="number" placeholder="0" />
          </div>
        </div>

        <!-- 头像上传 -->
        <div class="space-y-2">
          <Label>头像</Label>
          <div
            v-if="!avatarPreview"
            class="border-2 border-dashed border-border rounded-lg p-6 text-center hover:border-primary/50 transition-colors cursor-pointer"
            @click="$refs.avatarInput.click()"
          >
            <Image class="w-8 h-8 mx-auto text-muted mb-2" />
            <p class="text-sm text-muted">点击上传头像图片</p>
            <p class="text-xs text-muted mt-1">支持 JPG、PNG 格式</p>
          </div>
          <div v-else class="flex items-center gap-4">
            <div class="relative w-20 h-20 rounded-full overflow-hidden border border-border flex-shrink-0">
              <img :src="avatarPreview" class="w-full h-full object-cover" />
              <button
                type="button"
                @click="clearAvatar"
                class="absolute top-0.5 right-0.5 p-0.5 rounded-full bg-black/60 text-white hover:bg-black/80 cursor-pointer"
              >
                <X class="w-3 h-3" />
              </button>
            </div>
            <div class="text-sm text-muted">
              <p v-if="avatarFile">{{ avatarFile.name }}</p>
              <p v-else>当前头像</p>
              <button type="button" @click="$refs.avatarInput.click()" class="text-primary text-xs hover:underline mt-1 cursor-pointer">
                更换图片
              </button>
            </div>
          </div>
          <input
            ref="avatarInput"
            type="file"
            accept="image/*"
            class="hidden"
            @change="onAvatarChange"
          />
        </div>

        <div class="space-y-2">
          <Label for="avatar-network-url">头像网络URL</Label>
          <Input id="avatar-network-url" v-model="form.avatarNetworkUrl" placeholder="https://example.com/avatar.jpg" />
          <p class="text-xs text-muted">填写后优先使用网络图片</p>
        </div>

        <div class="space-y-2">
          <Label for="artist-bio">简介</Label>
          <Textarea id="artist-bio" v-model="form.biography" placeholder="输入艺术家简介" :rows="3" />
        </div>
        <div class="flex justify-end gap-3 pt-2">
          <Button type="button" variant="ghost" @click="dialogOpen = false">取消</Button>
          <Button type="submit" :disabled="uploading || loading || !form.name.trim()">
            <Loader2 v-if="uploading" class="w-4 h-4 animate-spin mr-2" />
            {{ editingId ? '保存修改' : '添加艺术家' }}
          </Button>
        </div>
      </form>
    </Dialog>
  </div>
</template>
