/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{vue,js}'],
  theme: {
    extend: {
      colors: {
        navy: '#1E3A8A',     // 海军蓝 · 主色
        abyss: '#0B1E4D',    // 深空蓝 · 侧栏底色
        electric: '#2563EB', // 星际电蓝 · 交互主色
        ice: '#0EA5E9',      // 高光冰青 · 数据高亮
        titan: '#F8FAFC',    // 冷钛白 · 空间底色
        line: '#E2E8F0',     // 浅蓝灰 · 微边框
        ink: '#0F172A',      // 主文本
        sub: '#64748B',      // 次级文本
      },
      boxShadow: {
        card: '0 1px 2px rgba(15,23,42,.04), 0 10px 30px -14px rgba(30,58,138,.18)',
        lift: '0 8px 30px -10px rgba(37,99,235,.35)',
      },
      fontFamily: {
        sans: ['Inter', '"Segoe UI"', '"Microsoft YaHei"', 'system-ui', 'sans-serif'],
        mono: ['"JetBrains Mono"', 'Consolas', '"SFMono-Regular"', 'monospace'],
      },
    },
  },
  plugins: [],
}
