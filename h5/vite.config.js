import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

function resolve(dir = '') {
  return path.join(__dirname, './src', dir);
}

// 阶段2D-1：vue-cli3/webpack4 → Vite5（Vue3 + Vant4）
export default defineConfig({
  base: './',
  plugins: [vue()],
  resolve: {
    // 路由/组件存在无扩展名 import（webpack 时代习惯），补 .vue 解析
    extensions: ['.mjs', '.js', '.ts', '.jsx', '.tsx', '.json', '.vue'],
    alias: {
      '@': resolve(),
      core: resolve('core'),
      // tokens 单源：仓库根 design/tokens.css（DESIGN-2D §四.1）
      '@tokens': path.join(__dirname, '../design/tokens.css')
    }
  },
  server: {
    // 九键输入法的 「mall」= 「6255」
    port: 6255,
    proxy: {
      '/leomall/wx': {
        target: 'http://localhost:8080',
        // 同源转发：剥离浏览器 Origin，避免后端 CORS 白名单（P1-1 默认空）
        // 把 dev 代理误判为跨域而 403 Invalid CORS request（随 2fce919 修复随迁）
        configure(proxy) {
          proxy.on('proxyReq', proxyReq => proxyReq.removeHeader('origin'));
        }
      }
    }
  },
  css: {
    preprocessorOptions: {
      scss: {
        additionalData(source, filename) {
          const f = filename.replace(/\\/g, '/');
          if (f.endsWith('_var.scss') || f.endsWith('_mixin.scss')) return source;
          return '@use "@/assets/scss/_var.scss" as *;\n@use "@/assets/scss/_mixin.scss" as *;\n' + source;
        }
      }
    }
  },
  build: {
    outDir: 'dist',
    assetsDir: 'static',
    sourcemap: false
  }
});