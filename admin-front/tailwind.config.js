/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{vue,js}'],
  theme: {
    extend: {
      colors: {
        // ---- 语义色（内容区点缀）----
        // 保持原始蓝色系：侧栏/顶栏等"外壳"用青绿品牌色（写死在组件里），
        // 内容区数字 / 图标 / 状态点缀回归蓝色，与后端图表数据色（#2563EB 系）一致，
        // 避免大面积青绿铺满内容区。
        navy: '#1E3A8A', // 海军蓝 · 主色深色端
        abyss: '#0B1E4D', // 深空蓝 · 需要深色时的底色
        electric: '#2563EB', // 电蓝 · 交互主色
        ice: '#0EA5E9', // 冰青 · 数据高亮
        titan: '#F8FAFC', // 冷钛白 · 空间底色
        line: '#E2E8F0', // 浅蓝灰 · 微边框
        ink: '#0F172A', // 主文本
        sub: '#64748B', // 次级文本

        // ---- 品牌青绿色阶（teal，登录页 / 商户工作台的品牌点缀）----
        brand: {
          50: '#F0FDFA',
          100: '#CCFBF1',
          200: '#99F6E4',
          300: '#5EEAD4',
          400: '#2DD4BF',
          500: '#14B8A6',
          600: '#0D9488',
          700: '#0F766E',
          800: '#115E59',
          900: '#134E4A',
        },

        // ---- 浅色侧栏专用 ----
        sidebar: {
          DEFAULT: '#FFFFFF',
          border: '#E5EAF0',
          text: '#334155',
          sub: '#94A3B8',
          hover: '#F1F5F9',
          active: '#F0FDFA',
          activeText: '#0F766E',
        },
      },
      boxShadow: {
        card: '0 1px 2px rgba(15,23,42,.04), 0 10px 30px -14px rgba(30,58,138,.16)',
        lift: '0 8px 30px -10px rgba(30,58,138,.30)',
      },
      fontFamily: {
        sans: ['Inter', '"Segoe UI"', '"Microsoft YaHei"', 'system-ui', 'sans-serif'],
        mono: ['"JetBrains Mono"', 'Consolas', '"SFMono-Regular"', 'monospace'],
      },
    },
  },
  plugins: [],
}
