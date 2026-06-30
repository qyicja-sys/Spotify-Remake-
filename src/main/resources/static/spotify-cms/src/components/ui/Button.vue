<script setup>
import { cn } from '@/lib/utils'

const props = defineProps({
  variant: { type: String, default: 'default' },
  size: { type: String, default: 'default' },
  disabled: Boolean,
  type: { type: String, default: 'button' },
  class: String
})

const variants = {
  default: 'bg-primary text-primary-foreground hover:bg-primary-hover shadow-sm',
  secondary: 'bg-surface-hover text-foreground border border-border hover:border-border-hover hover:bg-surface-active',
  destructive: 'bg-destructive text-destructive-foreground hover:bg-destructive-hover',
  ghost: 'text-muted hover:text-foreground hover:bg-surface-hover',
  outline: 'border border-border text-foreground hover:bg-surface-hover hover:border-border-hover'
}

const sizes = {
  default: 'h-10 px-4 py-2.5 text-sm',
  sm: 'h-8 px-3 text-xs',
  lg: 'h-12 px-6 text-base',
  icon: 'h-10 w-10'
}
</script>

<template>
  <button
    :type="type"
    :disabled="disabled"
    :class="cn(
      'inline-flex items-center justify-center whitespace-nowrap rounded-full font-semibold',
      'transition-all duration-150 cursor-pointer',
      'focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring',
      'disabled:pointer-events-none disabled:opacity-50',
      'active:scale-[0.97]',
      variants[variant],
      sizes[size],
      props.class
    )"
  >
    <slot />
  </button>
</template>
