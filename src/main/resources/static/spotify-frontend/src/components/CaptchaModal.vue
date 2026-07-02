<script setup>
import { computed, ref, watch } from 'vue'
import { getCaptcha, checkCaptcha } from '../api/auth'

const props = defineProps({
  visible: { type: Boolean, default: false },
})

const emit = defineEmits(['close', 'success', 'error'])

// ── Constants (match AJ-Captcha 1.3.0 image dimensions) ──────────────────────
const CAPTCHA_IMAGE_WIDTH = 310
const CAPTCHA_SLIDER_SIZE = 45
const CAPTCHA_TRACK_BUTTON_SIZE = 48
const CAPTCHA_MAX_OFFSET = CAPTCHA_IMAGE_WIDTH - CAPTCHA_SLIDER_SIZE // 265
const FIXED_Y = 5

// ── Reactive state ───────────────────────────────────────────────────────────
const backgroundImage = ref('')
const sliderImage = ref('')
const token = ref('')
const secretKey = ref('')
const sliderLeft = ref(0)
const sliderButtonLeft = ref(0)
const isDragging = ref(false)
const checking = ref(false)
const verified = ref(false)
const hintText = ref('')
const loading = ref(false)

// ── Computed ─────────────────────────────────────────────────────────────────
const sliderProgressWidth = computed(() => sliderButtonLeft.value + CAPTCHA_TRACK_BUTTON_SIZE)

// ── Helpers ──────────────────────────────────────────────────────────────────
function extractClientX(event) {
  if (event.touches && event.touches.length) return event.touches[0].clientX
  if (event.changedTouches && event.changedTouches.length) return event.changedTouches[0].clientX
  return event.clientX
}

function toDataUrl(base64, fallbackType) {
  if (!base64) return ''
  if (base64.startsWith('data:')) return base64
  return 'data:image/' + fallbackType + ';base64,' + base64
}

function getTrackMaxButtonLeft(track) {
  return track.getBoundingClientRect().width - CAPTCHA_TRACK_BUTTON_SIZE
}

function updateSliderPosition(clientX, trackEl) {
  if (!trackEl) return
  const rect = trackEl.getBoundingClientRect()
  const maxBtnLeft = getTrackMaxButtonLeft(trackEl)
  const rawLeft = clientX - rect.left - CAPTCHA_TRACK_BUTTON_SIZE / 2
  const nextBtnLeft = Math.min(Math.max(rawLeft, 0), maxBtnLeft)
  const ratio = maxBtnLeft > 0 ? nextBtnLeft / maxBtnLeft : 0

  sliderButtonLeft.value = nextBtnLeft
  sliderLeft.value = Math.round(ratio * CAPTCHA_MAX_OFFSET)
}

// ── Event handlers ───────────────────────────────────────────────────────────
function handleSliderStart(event) {
  if (checking.value || verified.value || !token.value) return
  isDragging.value = true
  hintText.value = ''
  updateSliderPosition(extractClientX(event), event.currentTarget.parentElement)
}

function handleSliderMove(event) {
  if (!isDragging.value || checking.value || verified.value) return
  updateSliderPosition(extractClientX(event), event.currentTarget)
}

async function handleSliderEnd() {
  if (!isDragging.value || checking.value || verified.value) return
  isDragging.value = false
  checking.value = true
  hintText.value = 'Verifying...'

  try {
    const x = Math.round(sliderLeft.value)
    const pointJsonRaw = JSON.stringify({ x, y: FIXED_Y })

    if (typeof window.AesUtil === 'undefined') {
      hintText.value = 'Captcha encryption not available. Please refresh the page.'
      emit('error')
      checking.value = false
      return
    }

    const encryptedPoint = window.AesUtil.encrypt(pointJsonRaw, secretKey.value)

    const response = await checkCaptcha({
      captchaType: 'blockPuzzle',
      token: token.value,
      pointJson: encryptedPoint,
    })

    const { repCode, repData } = response.data

    if (repCode === '0000' && repData && repData.result) {
      verified.value = true
      const captchaVerification = window.AesUtil.encrypt(
        token.value + '---' + pointJsonRaw,
        secretKey.value,
      )
      hintText.value = 'Verified'
      emit('success', { captchaVerification })
      setTimeout(() => { emit('close') }, 600)
    } else {
      sliderLeft.value = 0
      sliderButtonLeft.value = 0
      hintText.value = 'Verification failed, please try again.'
      emit('error')
    }
  } catch (err) {
    console.error('Captcha check failed:', err)
    sliderLeft.value = 0
    sliderButtonLeft.value = 0
    hintText.value = 'Verification failed, please try again.'
    emit('error')
  } finally {
    checking.value = false
  }
}

// ── Fetch captcha ────────────────────────────────────────────────────────────
async function fetchCaptcha() {
  loading.value = true
  hintText.value = 'Loading slider captcha...'

  try {
    const response = await getCaptcha()
    const { repCode, repMsg, repData } = response.data

    if (repCode !== '0000' || !repData) {
      hintText.value = repMsg || 'Failed to load captcha.'
      emit('error')
      return
    }

    backgroundImage.value = toDataUrl(repData.originalImageBase64, 'png')
    sliderImage.value = toDataUrl(repData.jigsawImageBase64, 'png')
    token.value = repData.token || ''
    secretKey.value = repData.secretKey || ''

    if (!backgroundImage.value || !sliderImage.value || !token.value || !secretKey.value) {
      hintText.value = 'Failed to load captcha.'
      emit('error')
      return
    }

    hintText.value = ''
  } catch (err) {
    console.error('Captcha fetch failed:', err)
    hintText.value = 'Failed to load captcha. Please try again.'
    emit('error')
  } finally {
    loading.value = false
  }
}

function refreshCaptcha() {
  if (loading.value || checking.value) return
  sliderLeft.value = 0
  sliderButtonLeft.value = 0
  verified.value = false
  fetchCaptcha()
}

function closeModal() {
  emit('close')
}

// ── Watch visibility ─────────────────────────────────────────────────────────
watch(
  () => props.visible,
  (isVisible) => {
    if (isVisible) {
      backgroundImage.value = ''
      sliderImage.value = ''
      token.value = ''
      secretKey.value = ''
      sliderLeft.value = 0
      sliderButtonLeft.value = 0
      isDragging.value = false
      checking.value = false
      verified.value = false
      hintText.value = ''
      fetchCaptcha()
    }
  },
)
</script>

<template>
  <div v-if="visible" class="captcha-modal-mask" @click.self="closeModal">
    <div class="captcha-modal">
      <!-- Header -->
      <div class="captcha-panel-header">
        <div>
          <h3>Security Verification</h3>
          <p>Drag the slider to fit the puzzle piece.</p>
        </div>
        <button type="button" class="captcha-close" aria-label="Close captcha" @click="closeModal">x</button>
      </div>

      <!-- Image stage -->
      <div class="captcha-stage">
        <div class="captcha-image-box" :class="{ 'is-verified': verified }">
          <div v-if="backgroundImage && sliderImage && !verified" class="captcha-target-hint"></div>
          <img class="captcha-background-image" :src="backgroundImage" alt="Captcha background" />
          <img
            v-if="sliderImage"
            class="captcha-slider-image"
            :src="sliderImage"
            :style="{ left: sliderLeft + 'px', top: FIXED_Y + 'px' }"
            alt="Captcha slider"
          />
        </div>
        <p v-if="!verified && !loading" class="captcha-stage-hint">
          <span class="captcha-hint-arrow">&darr;</span>
          Match the puzzle piece
        </p>
      </div>

      <!-- Status -->
      <div class="captcha-status" :class="{ success: verified }">{{ hintText }}</div>

      <!-- Drag track -->
      <div
        class="captcha-drag-track"
        @mousemove="handleSliderMove"
        @mouseup="handleSliderEnd"
        @mouseleave="handleSliderEnd"
        @touchmove.prevent="handleSliderMove"
        @touchend="handleSliderEnd"
      >
        <div class="captcha-drag-fill" :style="{ width: sliderProgressWidth + 'px' }"></div>
        <div class="captcha-drag-text">
          {{ verified ? 'Verified' : checking ? 'Verifying...' : 'Slide to complete the puzzle' }}
        </div>
        <button
          type="button"
          class="captcha-drag-thumb"
          :style="{ left: sliderButtonLeft + 'px' }"
          :disabled="checking || verified"
          @mousedown.prevent="handleSliderStart"
          @touchstart.prevent="handleSliderStart"
        >
          <span>&gt;&gt;</span>
        </button>
      </div>

      <!-- Actions -->
      <div class="captcha-panel-actions">
        <button
          type="button"
          class="captcha-secondary-btn"
          :disabled="loading || checking"
          @click="refreshCaptcha"
        >Refresh</button>
      </div>
    </div>
  </div>
</template>
