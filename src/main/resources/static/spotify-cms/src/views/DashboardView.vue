<script setup>
import { ref, onMounted } from 'vue'
import { useApi } from '../composables/useApi'
import { Music, Mic2, TrendingUp } from 'lucide-vue-next'

const { apiFetch } = useApi()

const stats = ref({ songs: 0, artists: 0 })

onMounted(async () => {
  const [songsRes, artistsRes] = await Promise.all([
    apiFetch('/songs'),
    apiFetch('/artists')
  ])
  stats.value.songs = songsRes?.code === 200 ? songsRes.data.length : 0
  stats.value.artists = artistsRes?.code === 200 ? artistsRes.data.length : 0
})

const cards = [
  { key: 'songs', label: '歌曲总数', icon: Music, color: 'text-primary' },
  { key: 'artists', label: '艺术家', icon: Mic2, color: 'text-purple-400' }
]
</script>

<template>
  <div class="animate-fade-in">
    <div class="mb-8">
      <h1 class="text-2xl font-bold text-foreground">仪表盘</h1>
      <p class="text-sm text-muted mt-1">管理你的音乐内容</p>
    </div>

    <!-- Stats Grid -->
    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 mb-8">
      <div
        v-for="card in cards"
        :key="card.key"
        class="group bg-card border border-border rounded-xl p-6 hover:border-border-hover hover:bg-card-hover transition-all duration-200"
      >
        <div class="flex items-center justify-between mb-4">
          <div :class="['p-2 rounded-lg bg-surface-hover', card.color]">
            <component :is="card.icon" class="w-5 h-5" />
          </div>
          <TrendingUp class="w-4 h-4 text-muted-foreground group-hover:text-primary transition-colors" />
        </div>
        <div class="text-3xl font-bold text-foreground tabular-nums">
          {{ stats[card.key] }}
        </div>
        <div class="text-sm text-muted mt-1">{{ card.label }}</div>
      </div>
    </div>

    <!-- Quick Actions -->
    <div class="bg-card border border-border rounded-xl p-6">
      <h2 class="text-base font-semibold text-foreground mb-4">快速操作</h2>
      <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <div class="flex items-center gap-3 p-3 rounded-lg bg-surface-hover/50 border border-border/50">
          <Music class="w-5 h-5 text-primary" />
          <div>
            <div class="text-sm font-medium text-foreground">添加新歌曲</div>
            <div class="text-xs text-muted">上传并管理音乐曲库</div>
          </div>
        </div>
        <div class="flex items-center gap-3 p-3 rounded-lg bg-surface-hover/50 border border-border/50">
          <Mic2 class="w-5 h-5 text-purple-400" />
          <div>
            <div class="text-sm font-medium text-foreground">添加艺术家</div>
            <div class="text-xs text-muted">管理艺术家资料信息</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
