import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  base: '/spotify-frontend/',
  host: '0.0.0.0', // 允许所有 IPv4 地址访问
  build: {
    rollupOptions: {
      input: {
        index: fileURLToPath(new URL('./index.html', import.meta.url)),
        main: fileURLToPath(new URL('./main.html', import.meta.url)),
      },
    },
  },
  server: {
    host: true, // 允许局域网内其他人访问你的本地网页（方便手机测试）
    port: 8000, // 用户端前端端口
    strictPort: true, // 端口被占用时直接报错，不自动递增
    proxy: {
      '/api': {
        target: 'http://localhost:5000',
        changeOrigin: true, // 改变源，解决跨域问题
        rewrite: path => path.replace(/^\/api/, ''),
      },
      '/spotify/': {
        target: 'http://localhost:5000',
        changeOrigin: true,
      },
      '/stream': {
        target: 'http://localhost:5000',
        changeOrigin: true,
      },
      '/captcha': {
        target: 'http://localhost:5000',
        changeOrigin: true,
      },
      '/uploads': {
        target: 'http://localhost:5000',
        changeOrigin: true,
      },
      '/songs': {
        target: 'http://localhost:5000',
        changeOrigin: true,
      },
      '/static': {
        target: 'http://localhost:5000',
        changeOrigin: true,
      },
    },
  },
})
