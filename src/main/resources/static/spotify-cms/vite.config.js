import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'
import path from 'path'

export default defineConfig({
  plugins: [vue(), tailwindcss()],
  base: '/',
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src')
    }
  },
  build: {
    outDir: '../spotify-cms-dist',
    emptyOutDir: true
  },
  server: {
    port: 4000,
    proxy: {
      '/admin': 'http://localhost:8080',
      '/static': 'http://localhost:8080'
    }
  }
})
