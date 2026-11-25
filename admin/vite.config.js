import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { createSvgIconsPlugin } from 'vite-plugin-svg-icons'
import path from 'path'

// 阶段2D-4a：webpack/vue-cli → Vite5（与 h5 同基线）
export default defineConfig({
  plugins: [
    vue(),
    createSvgIconsPlugin({
      iconDirs: [path.resolve(process.cwd(), 'src/icons/svg')],
      symbolId: 'icon-[name]'
    })
  ],
  resolve: {
    alias: {
      '@': path.resolve(process.cwd(), 'src'),
      '@tokens': path.resolve(process.cwd(), '../design/tokens.css')
    },
    extensions: ['.mjs', '.js', '.jsx', '.json', '.vue']
  },
  server: {
    port: 9527,
    proxy: {
      '/leomall/admin': {
        target: 'http://localhost:8080',
        // 同源转发：剥离浏览器 Origin，避免后端 CORS 白名单（P1-1 默认空）误判 403
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq) => proxyReq.removeHeader('origin'))
        }
      }
    }
  },
  build: {
    outDir: 'dist',
    chunkSizeWarningLimit: 2500
  }
})