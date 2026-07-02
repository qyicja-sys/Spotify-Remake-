<script setup>
import { onMounted, onBeforeUnmount, ref } from 'vue'

const canvasRef = ref(null)

const vertexShaderSource = `
  attribute vec2 position;
  void main() {
    gl_Position = vec4(position, 0.0, 1.0);
  }
`

const fragmentShaderSource = `
  precision mediump float;
  uniform float uTime;
  uniform vec2 uResolution;

  vec3 mod289(vec3 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
  vec4 mod289(vec4 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
  vec4 permute(vec4 x) { return mod289(((x * 34.0) + 1.0) * x); }
  vec4 taylorInvSqrt(vec4 r) { return 1.79284291400159 - 0.85373472095314 * r; }

  float snoise(vec3 v) {
    const vec2 C = vec2(1.0/6.0, 1.0/3.0);
    const vec4 D = vec4(0.0, 0.5, 1.0, 2.0);
    vec3 i = floor(v + dot(v, C.yyy));
    vec3 x0 = v - i + dot(i, C.xxx);
    vec3 g = step(x0.yzx, x0.xyz);
    vec3 l = 1.0 - g;
    vec3 i1 = min(g.xyz, l.zxy);
    vec3 i2 = max(g.xyz, l.zxy);
    vec3 x1 = x0 - i1 + C.xxx;
    vec3 x2 = x0 - i2 + C.yyy;
    vec3 x3 = x0 - D.yyy;
    i = mod289(i);
    vec4 p = permute(permute(permute(i.z + vec4(0.0, i1.z, i2.z, 1.0)) + i.y + vec4(0.0, i1.y, i2.y, 1.0)) + i.x + vec4(0.0, i1.x, i2.x, 1.0));
    float n_ = 0.142857142857;
    vec3 ns = n_ * D.wyz - D.xzx;
    vec4 j = p - 49.0 * floor(p * ns.z * ns.z);
    vec4 x_ = floor(j * ns.z);
    vec4 y_ = floor(j - 7.0 * x_);
    vec4 x = x_ * ns.x + ns.yyyy;
    vec4 y = y_ * ns.x + ns.yyyy;
    vec4 h = 1.0 - abs(x) - abs(y);
    vec4 b0 = vec4(x.xy, y.xy);
    vec4 b1 = vec4(x.zw, y.zw);
    vec4 s0 = floor(b0) * 2.0 + 1.0;
    vec4 s1 = floor(b1) * 2.0 + 1.0;
    vec4 sh = -step(h, vec4(0.0));
    vec4 a0 = b0.xzyw + s0.xzyw * sh.xxyy;
    vec4 a1 = b1.xzyw + s1.xzyw * sh.zzww;
    vec3 p0 = vec3(a0.xy, h.x);
    vec3 p1 = vec3(a0.zw, h.y);
    vec3 p2 = vec3(a1.xy, h.z);
    vec3 p3 = vec3(a1.zw, h.w);
    vec4 norm = taylorInvSqrt(vec4(dot(p0,p0), dot(p1,p1), dot(p2,p2), dot(p3,p3)));
    p0 *= norm.x; p1 *= norm.y; p2 *= norm.z; p3 *= norm.w;
    vec4 m = max(0.6 - vec4(dot(x0,x0), dot(x1,x1), dot(x2,x2), dot(x3,x3)), 0.0);
    m = m * m;
    return 42.0 * dot(m*m, vec4(dot(p0,x0), dot(p1,x1), dot(p2,x2), dot(p3,x3)));
  }

  void main() {
    vec2 uv = gl_FragCoord.xy / uResolution;
    float t = uTime * 0.06;

    float ar = uResolution.x / uResolution.y;
    vec2 p = (uv - 0.5) * vec2(ar, 1.0);
    p *= 2.2;

    float n1 = snoise(vec3(p * 0.8, t));
    float n2 = snoise(vec3(p * 1.5 + 3.7, t * 0.7 + 1.3));
    float n3 = snoise(vec3(p * 0.4 + 7.1, t * 0.5 - 2.1));
    float n4 = snoise(vec3(p * 2.0, t * 1.1 + 5.0));

    float fluid = n1 * 0.5 + n2 * 0.3 + n3 * 0.4 + n4 * 0.06;

    float light = fluid * 0.5 + 0.5;

    float sharp = smoothstep(0.08, 0.92, light);

    float highlight = smoothstep(0.55, 0.95, sharp);
    float shadow = smoothstep(0.15, 0.0, sharp);

    float val = sharp * 0.78 + 0.14 + highlight * 0.14 - shadow * 0.06;

    vec3 col = vec3(val);

    gl_FragColor = vec4(col, 1.0);
  }
`

let gl = null
let program = null
let animationId = 0
let startTime = 0
let resizeHandler = null

function createShader(type, source) {
  if (!gl) return null
  const shader = gl.createShader(type)
  if (!shader) return null
  gl.shaderSource(shader, source)
  gl.compileShader(shader)
  if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) {
    console.error(gl.getShaderInfoLog(shader))
    gl.deleteShader(shader)
    return null
  }
  return shader
}

function resize() {
  if (!canvasRef.value || !gl) return
  const dpr = Math.min(window.devicePixelRatio || 1, 2)
  const width = canvasRef.value.clientWidth * dpr
  const height = canvasRef.value.clientHeight * dpr
  if (width > 0 && height > 0 && (canvasRef.value.width !== width || canvasRef.value.height !== height)) {
    canvasRef.value.width = width
    canvasRef.value.height = height
    gl.viewport(0, 0, width, height)
  }
}

function draw(time) {
  if (!gl || !program || !canvasRef.value) return
  animationId = requestAnimationFrame(draw)
  const t = (time - startTime) / 1000
  resize()
  if (canvasRef.value.width === 0 || canvasRef.value.height === 0) return
  gl.uniform1f(gl.getUniformLocation(program, 'uTime'), t)
  gl.uniform2f(gl.getUniformLocation(program, 'uResolution'), canvasRef.value.width, canvasRef.value.height)
  gl.drawArrays(gl.TRIANGLE_STRIP, 0, 4)
}

function init() {
  const canvas = canvasRef.value
  if (!canvas) return
  gl = canvas.getContext('webgl', { alpha: false, antialias: false, preserveDrawingBuffer: false })
  if (!gl) return
  const vs = createShader(gl.VERTEX_SHADER, vertexShaderSource)
  const fs = createShader(gl.FRAGMENT_SHADER, fragmentShaderSource)
  if (!vs || !fs) return
  program = gl.createProgram()
  if (!program) return
  gl.attachShader(program, vs)
  gl.attachShader(program, fs)
  gl.linkProgram(program)
  if (!gl.getProgramParameter(program, gl.LINK_STATUS)) {
    console.error(gl.getProgramInfoLog(program))
    return
  }
  gl.useProgram(program)
  const buffer = gl.createBuffer()
  gl.bindBuffer(gl.ARRAY_BUFFER, buffer)
  gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([-1, -1, 1, -1, -1, 1, 1, 1]), gl.STATIC_DRAW)
  const loc = gl.getAttribLocation(program, 'position')
  gl.enableVertexAttribArray(loc)
  gl.vertexAttribPointer(loc, 2, gl.FLOAT, false, 0, 0)
  startTime = performance.now()
  resizeHandler = () => resize()
  window.addEventListener('resize', resizeHandler)
  resize()
  animationId = requestAnimationFrame(draw)
}

function destroy() {
  cancelAnimationFrame(animationId)
  animationId = 0
  if (resizeHandler) window.removeEventListener('resize', resizeHandler)
  if (gl && program) gl.deleteProgram(program)
  gl = null
  program = null
}

onMounted(() => {
  requestAnimationFrame(() => {
    requestAnimationFrame(() => {
      init()
    })
  })
})
onBeforeUnmount(destroy)
</script>

<template>
  <canvas ref="canvasRef" style="position:absolute;inset:0;width:100%;height:100%;z-index:0;pointer-events:none;" />
</template>
