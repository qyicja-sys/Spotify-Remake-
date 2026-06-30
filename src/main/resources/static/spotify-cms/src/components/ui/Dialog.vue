<script setup>
import { watch, onMounted, onUnmounted } from 'vue'
import { cn } from '@/lib/utils'
import { X } from 'lucide-vue-next'

const props = defineProps({
  open: Boolean,
  title: String,
  description: String,
  class: String
})

const emit = defineEmits(['update:open', 'close'])

function close() {
  emit('update:open', false)
  emit('close')
}

function onKeydown(e) {
  if (e.key === 'Escape') close()
}

watch(() => props.open, (val) => {
  if (val) {
    document.addEventListener('keydown', onKeydown)
    document.body.style.overflow = 'hidden'
  } else {
    document.removeEventListener('keydown', onKeydown)
    document.body.style.overflow = ''
  }
})

onUnmounted(() => {
  document.removeEventListener('keydown', onKeydown)
  document.body.style.overflow = ''
})
</script>

<template>
  <Teleport to="body">
    <Transition name="dialog">
      <div
        v-if="open"
        class="fixed inset-0 z-50 flex items-center justify-center"
        @click.self="close"
      >
        <!-- Backdrop -->
        <div class="absolute inset-0 bg-black/60 backdrop-blur-sm" />

        <!-- Content -->
        <div
          :class="cn(
            'relative z-50 w-full max-w-lg mx-4 bg-surface border border-border rounded-xl shadow-2xl shadow-black/40',
            'animate-scale-in',
            props.class
          )"
        >
          <!-- Header -->
          <div class="flex items-start justify-between p-6 pb-4">
            <div>
              <h2 v-if="title" class="text-lg font-semibold text-foreground">{{ title }}</h2>
              <p v-if="description" class="mt-1 text-sm text-muted">{{ description }}</p>
            </div>
            <button
              @click="close"
              class="rounded-lg p-1.5 text-muted hover:text-foreground hover:bg-surface-hover transition-colors cursor-pointer"
              aria-label="Close dialog"
            >
              <X class="w-4 h-4" />
            </button>
          </div>

          <!-- Body -->
          <div class="px-6 pb-6">
            <slot />
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.dialog-enter-active,
.dialog-leave-active {
  transition: opacity 200ms ease;
}
.dialog-enter-from,
.dialog-leave-to {
  opacity: 0;
}
</style>
