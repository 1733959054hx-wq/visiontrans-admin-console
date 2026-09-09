/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{vue,js}'],
  theme: {
    extend: {
      colors: {
        // ---- 语义色（全站统一青绿单色系）----
        // 外壳（侧栏/顶栏/登录页）与内容区（数字/图标/按钮/图表）共用同一青绿色阶，
        // 仅以明度区分层次：navy/abyss 为深色端，electric 为交互主色，ice 为数据高亮。
        // 状态色仅保留 成功(绿)/警告(琥珀)/危险(红) 三种语义色，不再引入其他色相。
        navy: '#115E59', // 深青 · 主色深色端
        abyss: '#134E4A', // 玄青 · 需要深色时的底色
        electric: '#0D9488', // 青绿 · 交互主色
        ice: '#14B8A6', // 亮青 · 数据高亮
        titan: '#F8FAFC', // 冷灰白 · 空间底色
        line: '#E5EAF0', // 浅灰 · 全站统一边框色
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
        card: '0 1px 2px rgba(15,23,42,.04), 0 10px 30px -14px rgba(17,94,89,.14)',
        lift: '0 8px 30px -10px rgba(17,94,89,.26)',
      },
      fontFamily: {
        sans: ['Inter', '"Segoe UI"', '"Microsoft YaHei"', 'system-ui', 'sans-serif'],
        mono: ['"JetBrains Mono"', 'Consolas', '"SFMono-Regular"', 'monospace'],
      },
    },
  },
  plugins: [],
}
