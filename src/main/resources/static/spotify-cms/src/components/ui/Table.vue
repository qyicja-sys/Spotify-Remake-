<script setup>
import { cn } from '@/lib/utils'

const props = defineProps({ class: String })
</script>

<template>
  <div class="relative w-full overflow-auto rounded-lg border border-border">
    <table :class="cn('w-full caption-bottom text-sm', props.class)">
      <slot />
    </table>
  </div>
</template>
