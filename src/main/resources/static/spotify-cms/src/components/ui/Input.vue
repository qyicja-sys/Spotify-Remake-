<script setup>
import { cn } from '@/lib/utils'

const props = defineProps({
  class: String,
  modelValue: [String, Number],
  type: { type: String, default: 'text' },
  placeholder: String,
  disabled: Boolean,
  id: String,
  autocomplete: String
})

const emit = defineEmits(['update:modelValue'])
</script>

<template>
  <input
    :id="id"
    :type="type"
    :value="modelValue"
    :placeholder="placeholder"
    :disabled="disabled"
    :autocomplete="autocomplete"
    @input="emit('update:modelValue', $event.target.value)"
    :class="cn(
      'flex h-10 w-full rounded-lg border border-border bg-surface px-3 py-2.5 text-sm text-foreground placeholder:text-muted-foreground',
      'transition-colors duration-150',
      'hover:border-border-hover',
      'focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary/30',
      'disabled:cursor-not-allowed disabled:opacity-50',
      props.class
    )"
  />
</template>
