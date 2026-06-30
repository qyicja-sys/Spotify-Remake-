<script setup>
defineProps({
  visible: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['close', 'success', 'error'])

function onSuccess(params) {
  emit('success', params)
}

function onError() {
  emit('error')
}
</script>

<template>
  <div v-if="visible" class="captcha-verify-overlay" @click.self="emit('close')">
    <div class="captcha-verify-box">
      <div class="captcha-verify-header">
        <h3>Slider Verification</h3>
        <p>Drag the slider to fit the puzzle piece</p>
        <button type="button" class="captcha-verify-close" @click="emit('close')">&times;</button>
      </div>
      <Verify
        :mode="'pop'"
        :captchaType="'blockPuzzle'"
        :imgSize="{ width: '310px', height: '155px' }"
        :barSize="{ width: '310px', height: '40px' }"
        @success="onSuccess"
        @error="onError"
      />
    </div>
  </div>
</template>

<style scoped>
.captcha-verify-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.captcha-verify-box {
  position: relative;
  background: #282828;
  border-radius: 8px;
  padding: 24px;
  min-width: 360px;
  box-shadow: 0 4px 60px rgba(0, 0, 0, 0.5);
}

.captcha-verify-header {
  margin-bottom: 16px;
}

.captcha-verify-header h3 {
  margin: 0 0 4px;
  font-size: 18px;
  color: #fff;
}

.captcha-verify-header p {
  margin: 0;
  font-size: 13px;
  color: #b3b3b3;
}

.captcha-verify-close {
  position: absolute;
  top: 12px;
  right: 16px;
  background: none;
  border: none;
  color: #b3b3b3;
  font-size: 24px;
  cursor: pointer;
}

.captcha-verify-close:hover {
  color: #fff;
}
</style>
