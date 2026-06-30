<script setup>
import { ref, onMounted, onUnmounted, markRaw } from 'vue'
import LoginView from './views/LoginView.vue'
import DashboardView from './views/DashboardView.vue'
import SongsView from './views/SongsView.vue'
import ArtistsView from './views/ArtistsView.vue'
import PlaylistsView from './views/PlaylistsView.vue'
import AlbumsView from './views/AlbumsView.vue'
import { useApi } from './composables/useApi'
import { LayoutDashboard, Music, Mic2, ListMusic, LogOut, Disc3 } from 'lucide-vue-next'

const { logout } = useApi()

const isAuthenticated = ref(false)
const currentView = ref('dashboard')

const views = {
  dashboard: markRaw(DashboardView),
  songs: markRaw(SongsView),
  artists: markRaw(ArtistsView),
  playlists: markRaw(PlaylistsView),
  albums: markRaw(AlbumsView)
}

const navItems = [
  { id: 'dashboard', label: '仪表盘', icon: LayoutDashboard },
  { id: 'songs', label: '歌曲管理', icon: Music },
  { id: 'artists', label: '艺术家', icon: Mic2 },
  { id: 'playlists', label: '系统歌单', icon: ListMusic },
  { id: 'albums', label: '专辑管理', icon: Disc3 }
]

function onLogout() {
  logout()
  isAuthenticated.value = false
}

function checkAuth() {
  isAuthenticated.value = !!localStorage.getItem('adminToken')
}

onMounted(() => {
  checkAuth()
  window.addEventListener('auth:logout', onLogout)
  window.addEventListener('auth:login', checkAuth)
})

onUnmounted(() => {
  window.removeEventListener('auth:logout', onLogout)
  window.removeEventListener('auth:login', checkAuth)
})
</script>

<template>
  <!-- Login -->
  <LoginView v-if="!isAuthenticated" @login="isAuthenticated = true; currentView = 'dashboard'" />

  <!-- Main App -->
  <div v-else class="flex h-screen bg-background">
    <!-- Sidebar -->
    <aside class="w-64 flex-shrink-0 bg-sidebar flex flex-col border-r border-border/50">
      <!-- Logo -->
      <div class="px-6 py-5 flex items-center gap-3">
        <div class="w-8 h-8 rounded-full bg-primary flex items-center justify-center">
          <Music class="w-4 h-4 text-primary-foreground" />
        </div>
        <span class="text-lg font-bold text-foreground tracking-tight">Spotify CMS</span>
      </div>

      <!-- Navigation -->
      <nav class="flex-1 px-3 py-2 space-y-1">
        <button
          v-for="item in navItems"
          :key="item.id"
          @click="currentView = item.id"
          :class="[
            'w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-all duration-150 cursor-pointer',
            currentView === item.id
              ? 'bg-sidebar-accent text-sidebar-active'
              : 'text-sidebar-foreground hover:text-sidebar-active hover:bg-sidebar-accent/50'
          ]"
        >
          <component :is="item.icon" class="w-5 h-5" />
          {{ item.label }}
        </button>
      </nav>

      <!-- Footer -->
      <div class="px-3 py-4 border-t border-border/50">
        <button
          @click="onLogout"
          class="w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium text-sidebar-foreground hover:text-destructive hover:bg-destructive/10 transition-colors duration-150 cursor-pointer"
        >
          <LogOut class="w-5 h-5" />
          退出登录
        </button>
      </div>
    </aside>

    <!-- Main Content -->
    <main class="flex-1 overflow-y-auto">
      <div class="p-8 max-w-7xl mx-auto">
        <Transition name="page" mode="out-in">
          <component :is="views[currentView]" :key="currentView" />
        </Transition>
      </div>
    </main>
  </div>
</template>

<style scoped>
.page-enter-active {
  transition: opacity 200ms ease, transform 200ms ease;
}
.page-leave-active {
  transition: opacity 150ms ease;
}
.page-enter-from {
  opacity: 0;
  transform: translateY(4px);
}
.page-leave-to {
  opacity: 0;
}
</style>
