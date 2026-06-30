<script setup>
defineProps({
  visible: Boolean,
  backgroundImage: {
    type: String,
    default: '',
  },
  sliderImage: {
    type: String,
    default: '',
  },
  sliderLeft: {
    type: Number,
    default: 0,
  },
  sliderTop: {
    type: Number,
    default: 40,
  },
  sliderProgressWidth: {
    type: Number,
    default: 48,
  },
  sliderButtonLeft: {
    type: Number,
    default: 0,
  },
  hintText: {
    type: String,
    default: '',
  },
  loading: Boolean,
  checking: Boolean,
  verified: Boolean,
})

defineEmits([
  'close',
  'refresh',
  'slider-start',
  'slider-move',
  'slider-end',
])
</script>

<template>
  <div v-if="visible" class="captcha-modal-mask" @click.self="$emit('close')">
    <div class="captcha-modal">
      <div class="captcha-panel-header">
        <div>
          <h3>Slider Verification</h3>
          <p>Drag the slider to fit the puzzle piece.</p>
        </div>
        <button type="button" class="captcha-close" aria-label="Close captcha" @click="$emit('close')">
          x
        </button>
      </div>

      <div class="captcha-stage">
        <div class="captcha-image-box" :class="{ 'is-verified': verified }">
          <img class="captcha-background-image" :src="backgroundImage" alt="Captcha background" />
          <!-- 缺块提示：未验证时显示脉冲边框 -->
          <div v-if="!verified" class="captcha-target-hint"></div>
          <img
            class="captcha-slider-image"
            :src="sliderImage"
            :style="{ left: `${sliderLeft}px`, top: `${sliderTop}px` }"
            alt="Captcha slider"
          />
        </div>
        <p v-if="!verified" class="captcha-stage-hint">
          <span class="captcha-hint-arrow">&#8595;</span>
          Find the dark cutout and drag the puzzle piece into it
        </p>
      </div>

      <div class="captcha-status" :class="{ success: verified }">
        {{ hintText }}
      </div>

      <div
        class="captcha-drag-track"
        @mousemove="$emit('slider-move', $event)"
        @mouseup="$emit('slider-end')"
        @mouseleave="$emit('slider-end')"
        @touchmove.prevent="$emit('slider-move', $event)"
        @touchend="$emit('slider-end')"
      >
        <div class="captcha-drag-fill" :style="{ width: `${sliderProgressWidth}px` }"></div>
        <div class="captcha-drag-text">
          {{ verified ? 'Verification completed' : 'Slide to complete the puzzle' }}
        </div>
        <button
          type="button"
          class="captcha-drag-thumb"
          :style="{ left: `${sliderButtonLeft}px` }"
          :disabled="checking || verified"
          @mousedown.prevent="$emit('slider-start', $event)"
          @touchstart.prevent="$emit('slider-start', $event)"
        >
          <span>&gt;&gt;</span>
        </button>
      </div>

      <div class="captcha-panel-actions">
        <button
          type="button"
          class="captcha-secondary-btn"
          :disabled="loading || checking"
          @click="$emit('refresh')"
        >
          Refresh
        </button>
      </div>
    </div>
  </div>
</template>
