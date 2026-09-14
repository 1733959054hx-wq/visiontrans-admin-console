import { fileURLToPath, URL } from 'node:url'
import fs from 'node:fs'
import path from 'node:path'

import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

/**
 * 把 extension/ 目录（manifest.json、background.js、icons/*）原样打进扩展产物根目录。
 * 只在 --mode extension 构建时启用；普通网页构建完全不受影响。
 */
function bundleExtensionAssets() {
  const extensionDir = fileURLToPath(new URL('./extension', import.meta.url))
  return {
    name: 'bundle-extension-assets',
    generateBundle() {
      const walk = (dir, base = '') => {
        for (const name of fs.readdirSync(dir)) {
          const full = path.join(dir, name)
          const rel = base ? `${base}/${name}` : name
          if (fs.statSync(full).isDirectory()) {
            walk(full, rel)
          } else {
            this.emitFile({ type: 'asset', fileName: rel, source: fs.readFileSync(full) })
          }
        }
      }
      walk(extensionDir)
    },
  }
}

// 开发期把 /api 代理到 Spring Boot（后端地址来自 .env.development 的 VITE_PROXY_TARGET）
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const target = env.VITE_PROXY_TARGET || 'http://localhost:8080'
  // 扩展构建：chrome-extension 协议要求相对资源路径 + hash 路由，产物单独输出到 dist-extension
  const extension = mode === 'extension'

  return {
    // 扩展内 index.html 位于 chrome-extension://<id>/index.html，必须用相对路径加载资源
    base: extension ? './' : '/',
    plugins: [vue(), extension && bundleExtensionAssets()].filter(Boolean),
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    server: {
      port: 5173,
      open: false,
      proxy: {
        '/api': {
          target,
          changeOrigin: true,
        },
      },
    },
    build: {
      outDir: extension ? 'dist-extension' : 'dist',
      sourcemap: false,
      rollupOptions: {
        output: {
          // 把体积较大的第三方库单独分包，利于浏览器缓存
          manualChunks: {
            echarts: ['echarts'],
            vendor: ['vue', 'vue-router', 'pinia', 'axios'],
          },
        },
      },
    },
  }
})
