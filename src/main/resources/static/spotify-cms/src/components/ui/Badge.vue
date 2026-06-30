<script setup>
import { cn } from '@/lib/utils'

const props = defineProps({
  variant: { type: String, default: 'default' },
  class: String
})

const variants = {
  default: 'bg-primary/15 text-primary border-primary/20',
  secondary: 'bg-surface-hover text-foreground border-border',
  destructive: 'bg-destructive/15 text-destructive border-destructive/20',
  outline: 'border-border text-foreground'
}
</script>

<template>
  <span
    :class="cn(
      'inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-medium transition-colors',
      variants[variant],
      props.class
    )"
  >
    <slot />
  </span>
</template>
