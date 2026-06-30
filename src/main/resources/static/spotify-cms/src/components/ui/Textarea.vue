<script setup>
import { cn } from '@/lib/utils'

const props = defineProps({
  class: String,
  modelValue: [String, Number],
  placeholder: String,
  rows: { type: Number, default: 4 }
})

const emit = defineEmits(['update:modelValue'])
</script>

<template>
  <textarea
    :rows="rows"
    :value="modelValue"
    :placeholder="placeholder"
    @input="emit('update:modelValue', $event.target.value)"
    :class="cn(
      'flex min-h-[80px] w-full rounded-lg border border-border bg-surface px-3 py-2.5 text-sm text-foreground placeholder:text-muted-foreground',
      'transition-colors duration-150',
      'hover:border-border-hover',
      'focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary/30',
      'disabled:cursor-not-allowed disabled:opacity-50',
      'resize-y',
      props.class
    )"
  />
</template>
