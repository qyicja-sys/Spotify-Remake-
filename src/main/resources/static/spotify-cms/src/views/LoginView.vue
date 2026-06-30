<script setup>
import { ref } from 'vue'
import { useApi } from '../composables/useApi'
import Button from '../components/ui/Button.vue'
import Input from '../components/ui/Input.vue'
import Label from '../components/ui/Label.vue'
import { Music, Loader2 } from 'lucide-vue-next'

const emit = defineEmits(['login'])
const { login, loading, error } = useApi()

const username = ref('')
const password = ref('')

async function handleLogin() {
  if (!username.value || !password.value) return
  const result = await login(username.value, password.value)
  if (result) {
    window.dispatchEvent(new Event('auth:login'))
    emit('login')
  }
}
</script>

<template>
  <div class="min-h-screen flex items-center justify-center bg-background">
    <!-- Background gradient -->
    <div class="fixed inset-0 bg-[radial-gradient(ellipse_at_top,_var(--color-primary)/8%,_transparent_60%)]" />

    <div class="relative w-full max-w-sm mx-4 animate-fade-in">
      <!-- Logo -->
      <div class="text-center mb-8">
        <div class="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-primary/10 mb-4">
          <Music class="w-8 h-8 text-primary" />
        </div>
        <h1 class="text-2xl font-bold text-foreground">Spotify Admin</h1>
        <p class="text-sm text-muted mt-1">管理后台登录</p>
      </div>

      <!-- Form -->
      <form @submit.prevent="handleLogin" class="space-y-4">
        <div class="space-y-2">
          <Label for="username">用户名</Label>
          <Input
            id="username"
            v-model="username"
            placeholder="输入管理员用户名"
            autocomplete="username"
          />
        </div>

        <div class="space-y-2">
          <Label for="password">密码</Label>
          <Input
            id="password"
            v-model="password"
            type="password"
            placeholder="输入密码"
            autocomplete="current-password"
          />
        </div>

        <!-- Error -->
        <div v-if="error" class="flex items-center gap-2 text-sm text-destructive bg-destructive/10 rounded-lg px-3 py-2">
          {{ error }}
        </div>

        <Button type="submit" class="w-full" :disabled="loading || !username || !password">
          <Loader2 v-if="loading" class="w-4 h-4 animate-spin mr-2" />
          {{ loading ? '登录中...' : '登 录' }}
        </Button>
      </form>
    </div>
  </div>
</template>
