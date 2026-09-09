<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'
import { useClusterStore } from '@/stores/cluster'
import BrandMark from '@/components/BrandMark.vue'
import { getCaptcha, getPublicKey } from '@/api/auth'

/** 验证码原图基准尺寸（须与后端 CaptchaService 的 WIDTH / HEIGHT 保持一致） */
const CAPTCHA_W = 320
const CAPTCHA_H = 150

const auth = useAuthStore()
const ui = useUiStore()
const router = useRouter()

const form = reactive({ username: '', password: '' })
/** 登录身份：管理员 / 商户（选身份后走各自独立的登录接口与令牌体系） */
const identity = ref('admin')
const loading = ref(false)
const captcha = ref(null)
const clicks = ref([])
const publicKey = ref('')
const imgEl = ref(null)

/** 验证码面板开关（仅在右侧卡片内局部切换） */
const captchaOpen = ref(false)
/** 明确业务报错 */
const formError = ref('')

/* =========================================================
 *  吉祥物「译译」物理/眼球追踪与高情商对白状态机
 * ========================================================= */
const owlRef = ref(null)
const isUsernameFocused = ref(false)
const isPasswordFocused = ref(false)
const isBlinking = ref(false)
const isTilting = ref(false)
const loginStatus = ref('idle') // 'idle' | 'failed' | 'success'
/** 校验中轮换的「工作台词」：让等待时段的译译有事可做，避免一动不动的呆滞感 */
const VERIFY_LINES = ['正在解密身份凭证…', '正在核对账户口令…', '正在签发会话令牌…']
const verifyLine = ref(VERIFY_LINES[0])
let verifyTicker = null
/** 依依不舍哭泣态：离开登录页 / 登出后回到本页 */
const leaving = ref(false)
const farewell = ref(false)
const route = useRoute()

/* ---- 交互手势：对每个按钮 / 点选做出即时小动作（A/B 交替以便连续触发重放动画） ---- */
const gesture = ref('') // 'popA' | 'popB' | 'oopsA' | 'oopsB' | 'focus' | ''
let gestureTimer = null
let gestureFlip = false
const playGesture = (name, ms = 520) => {
  gestureFlip = !gestureFlip
  gesture.value = `${name}${gestureFlip ? 'A' : 'B'}`
  clearTimeout(gestureTimer)
  gestureTimer = setTimeout(() => {
    gesture.value = ''
  }, ms)
}
const clearGesture = () => {
  clearTimeout(gestureTimer)
  gesture.value = ''
}

const pupilOffset = reactive({ x: 0, y: 0 })
const headTilt = reactive({ x: 0, y: 0, rotate: 0 })

const mascotMood = computed(() => {
  if (loginStatus.value === 'success') return 'success'
  if (loginStatus.value === 'failed') return 'dizzy'
  if (leaving.value || farewell.value) return 'cry'
  if (gesture.value.startsWith('pop')) return 'happy'
  if (gesture.value.startsWith('oops')) return 'oops'
  if (gesture.value === 'focus') return 'focus'
  if (isBlinking.value) return 'blink'
  if (isPasswordFocused.value && !captchaOpen.value) return 'shy'
  if (isUsernameFocused.value && !captchaOpen.value) return 'curious'
  return 'normal'
})

/**
 * 漫画对话框动态对白内容
 */
const mascotSpeech = computed(() => {
  if (loginStatus.value === 'success') {
    return {
      tag: '验证通过',
      type: 'success',
      text: '太棒啦！安全验证通过，视界之门已开启 🎉',
    }
  }

  // 依依不舍：离开登录页 / 登出后回到本页
  if (leaving.value) {
    return {
      tag: '依依不舍',
      type: 'warning',
      text: '不要走嘛…译译会一直在这里等你的 😢',
    }
  }
  if (farewell.value) {
    return {
      tag: '好想你',
      type: 'warning',
      text: '您离开啦，译译好舍不得，快回来登录吧 😭',
    }
  }

  // 校验中：台词轮换 + 眼珠扫视 + 身体轻摇，把"系统正在干活"演出来
  if (gesture.value === 'focus') {
    return {
      tag: '校验中',
      type: 'normal',
      text: verifyLine.value,
    }
  }

  // 遇到具体业务报错
  if (formError.value) {
    if (formError.value.includes('验证码') || formError.value.includes('重新验证')) {
      return {
        tag: '重新校验',
        type: 'error',
        text: '验证码没对上，译译帮你换了一张，再试一次！💦',
      }
    }
    if (formError.value.includes('顺序') || formError.value.includes('点击')) {
      return {
        tag: '顺序提醒',
        type: 'warning',
        text: '还没点完哦，请按提示文字顺序在图中点击 👆',
      }
    }
    if (formError.value.includes('账号') || formError.value.includes('口令') || formError.value.includes('密码')) {
      return {
        tag: '核对提醒',
        type: 'error',
        text: '账号或口令好像不正确，再仔细检查一下呢 🧐',
      }
    }
    return {
      tag: '请注意',
      type: 'error',
      text: `${formError.value}，请重新尝试 💦`,
    }
  }

  // 验证码阶段步进指引
  if (captchaOpen.value) {
    if (clicks.value.length === 0) {
      return {
        tag: '安全挑战',
        type: 'guide',
        text: `请在右侧图中帮我依次点击「${captcha.value?.hint || '...'}」✨`,
      }
    }
    // 注意：是否点对由后端校验，前端无权判定，因此文案只描述"已找到/已标记"
    if (clicks.value.length === 1) {
      return {
        tag: '已找到 1/3',
        type: 'guide',
        text: '已找到第 1 个字，继续找第 2 个字 🔍',
      }
    }
    if (clicks.value.length === 2) {
      return {
        tag: '已找到 2/3',
        type: 'guide',
        text: '已找到 2 个字，只剩最后 1 个啦 🎯',
      }
    }
    if (clicks.value.length >= 3) {
      return {
        tag: '已就绪',
        type: 'success',
        text: '3 个字都标记好啦，点击「确认登录」提交吧 👍',
      }
    }
  }

  // 基础输入阶段
  if (isPasswordFocused.value) {
    return {
      tag: '加密保护',
      type: 'shy',
      text: '非礼勿视！口令加密保护中，译译不偷看 🙈',
    }
  }
  if (isUsernameFocused.value) {
    return {
      tag: '认真观察',
      type: 'curious',
      text: identity.value === 'merchant' ? '请在右侧输入您的商户授权账号 ✍️' : '请在右侧输入您的管理员授权账号 ✍️',
    }
  }
  if (isBlinking.value) {
    return {
      tag: '刷新中',
      type: 'guide',
      text: '收到！正在为您刷新安全挑战 ✨',
    }
  }

  return {
    tag: '在线向导',
    type: 'normal',
    text: '你好！我是视界译向导，今天由我协助你登录 🦉',
  }
})

/** 全局鼠标移动视差追踪 */
const handleMouseMove = (e) => {
  if (['shy', 'dizzy', 'success', 'focus', 'cry'].includes(mascotMood.value) || !owlRef.value) return

  const rect = owlRef.value.getBoundingClientRect()
  const centerX = rect.left + rect.width / 2
  const centerY = rect.top + rect.height * 0.42

  const dx = e.clientX - centerX
  const dy = e.clientY - centerY
  const distance = Math.hypot(dx, dy)
  const angle = Math.atan2(dy, dx)

  const maxPupilRadius = mascotMood.value === 'curious' ? 8 : 10
  const clampedDist = Math.min(distance / 22, maxPupilRadius)

  pupilOffset.x = Math.cos(angle) * clampedDist
  pupilOffset.y = Math.sin(angle) * clampedDist

  headTilt.x = pupilOffset.x * 0.45
  headTilt.y = pupilOffset.y * 0.45
  headTilt.rotate = (pupilOffset.x / maxPupilRadius) * 3.2
}

const triggerBlinkAndTilt = () => {
  isTilting.value = true
  isBlinking.value = true
  setTimeout(() => {
    isBlinking.value = false
  }, 220)
  setTimeout(() => {
    isTilting.value = false
  }, 650)
}

/* =========================================================
 *  业务逻辑
 * ========================================================= */
const loadCaptcha = async (isManual = false) => {
  clicks.value = []
  if (isManual) triggerBlinkAndTilt()
  try {
    captcha.value = null
    const data = await getCaptcha()
    captcha.value = data
    if (!publicKey.value) {
      publicKey.value = await getPublicKey()
    }
  } catch {
    formError.value = '验证码加载失败，请点击"换一张"重试'
  }
}

const openCaptcha = () => {
  formError.value = ''
  if (!form.username || !form.password) {
    formError.value = '请输入账号与口令'
    return
  }
  captchaOpen.value = true
  playGesture('oops', 650) // 进入安全挑战，睁大眼睛盯住
  loadCaptcha()
}

const closeCaptcha = () => {
  captchaOpen.value = false
  formError.value = ''
}

const onCaptchaClick = (e) => {
  if (!captcha.value || clicks.value.length >= 3 || !imgEl.value) return
  const rect = imgEl.value.getBoundingClientRect()
  clicks.value.push({
    x: Math.round(((e.clientX - rect.left) / rect.width) * CAPTCHA_W),
    y: Math.round(((e.clientY - rect.top) / rect.height) * CAPTCHA_H),
  })
  playGesture('pop') // 点对一个字，眯眼开心一下
  if (formError.value.includes('顺序') || formError.value.includes('点击')) {
    formError.value = ''
  }
}

const undoLastClick = () => {
  if (clicks.value.length > 0) {
    clicks.value.pop()
    playGesture('oops') // 撤销时睁大眼表示"咦？"
  }
}

const confirmLogin = async () => {
  if (clicks.value.length < 3) {
    formError.value = '请按提示顺序点击验证码中的文字'
    return
  }

  loading.value = true
  formError.value = ''
  clearGesture()
  gesture.value = 'focus' // 校验中凝神屏息
  // 台词轮换：每 0.9s 切一条「工作台词」，与眼珠扫视动画同节奏
  let step = 0
  verifyLine.value = VERIFY_LINES[0]
  verifyTicker = setInterval(() => {
    step = (step + 1) % VERIFY_LINES.length
    verifyLine.value = VERIFY_LINES[step]
  }, 900)
  try {
    // 按所选身份走各自独立的登录接口：商户签发商户令牌，管理员签发后台令牌
    const submit = identity.value === 'merchant'
      ? await auth.loginMerchant(form.username, form.password, publicKey.value, captcha.value.id, clicks.value)
      : await auth.login(form.username, form.password, publicKey.value, captcha.value.id, clicks.value)
    loginStatus.value = 'success'
    ui.success(`欢迎回来，${submit.profile.name}`)
    const target = submit.profile.roleCode === 'MERCHANT' ? '/merchant' : '/'
    // 跳转动画期间并行预取大盘数据：原来要等 900ms 跳过去后 TopBar 挂载才发请求，
    // 落地后白屏等待；现在请求与动画同时进行，且成功动画从 900ms 缩短到 300ms。
    if (target === '/') useClusterStore().load()
    setTimeout(() => {
      router.replace(target)
    }, 300)
  } catch (error) {
    loginStatus.value = 'failed'
    const errMsg = error?.message || '登录失败，请稍后重试'
    if (errMsg.includes('验证码') || errMsg.includes('captcha')) {
      formError.value = '验证码校验失败，请重新验证'
    } else {
      formError.value = errMsg
    }
    ui.error(formError.value)
    loadCaptcha()
    setTimeout(() => {
      if (loginStatus.value === 'failed') loginStatus.value = 'idle'
    }, 2800)
  } finally {
    clearInterval(verifyTicker)
    verifyTicker = null
    if (gesture.value === 'focus') gesture.value = ''
    loading.value = false
  }
}

onMounted(() => {
  window.addEventListener('mousemove', handleMouseMove, { passive: true })
  // 由登出跳转而来（/login?bye=1）：短暂表达不舍后恢复常态
  if (route.query.bye) {
    farewell.value = true
    setTimeout(() => {
      farewell.value = false
    }, 2800)
  }
})

/** 离开登录页时先哭一下再放行（登录成功跳转除外） */
onBeforeRouteLeave(async () => {
  if (loginStatus.value === 'success') return true
  leaving.value = true
  await new Promise((resolve) => setTimeout(resolve, 1200))
  return true
})

onBeforeUnmount(() => {
  window.removeEventListener('mousemove', handleMouseMove)
  clearGesture()
})
</script>

<template>
  <div class="relative flex min-h-screen w-full flex-col overflow-hidden bg-titan lg:flex-row">
    <!-- ==================== 左：品牌展示区（与应用侧栏同为浅色，登录页与控制台同源） ==================== -->
    <div
      class="sidebar-bg relative flex flex-col justify-between border-b border-line p-6 lg:w-[54%] lg:border-b-0 lg:border-r lg:p-12 xl:w-[56%]"
    >
      <!-- 与侧栏一致的网格纹理 -->
      <div class="sidebar-grid pointer-events-none absolute inset-0"></div>
      <!-- 顶部淡青渐变，给浅色面板一点层次，避免大面积留白 -->
      <div class="pointer-events-none absolute inset-x-0 top-0 h-40 bg-gradient-to-b from-brand-50 to-transparent"></div>

      <!-- 品牌标识 -->
      <div class="relative z-10 flex items-center gap-3">
        <div
          class="grid h-11 w-11 place-items-center rounded-xl text-white shadow-lift"
          style="background: linear-gradient(135deg, #0d9488, #06b6d4)"
        >
          <BrandMark class="h-[26px] w-[26px]" />
        </div>
        <div>
          <div class="text-[17px] font-bold tracking-tight text-ink">视界译 VisionTrans</div>
          <div class="text-[11.5px] text-sub">VR 空间级多模态实时解析系统</div>
        </div>
      </div>

      <!-- 中央舞台：猫头鹰 + 右上肩侧漫画气泡（同一坐标系，尖角精准指向喙部） -->
      <div class="relative z-10 my-auto flex flex-col items-center justify-center py-6">
        <!-- 舞台宽度即猫头鹰宽度；气泡宽度取 80%，尖角偏移 37% ⇒ 尖端恰好落在喙部中轴 -->
        <div class="relative mx-auto w-[240px] pt-[104px] sm:w-[290px] sm:pt-[112px] lg:w-[320px] lg:pt-[118px]">
          <!-- ================= 漫画对话框（右下肩侧，暖白底 + 品牌冰蓝描边） ================= -->
          <div
            class="comic-bubble absolute right-0 top-0 z-30 w-4/5 rounded-2xl border bg-[#FFFDF8] px-3.5 py-3 shadow-2xl transition-all duration-300"
            :class="{
              'border-rose-200': mascotSpeech.type === 'error',
              'border-emerald-200': mascotSpeech.type === 'success',
              'border-amber-200': mascotSpeech.type === 'warning',
              'border-brand-100': ['normal', 'guide', 'shy', 'curious'].includes(mascotSpeech.type)
            }"
          >
            <!-- 气泡头：徽章 + 身份 -->
            <div class="mb-1.5 flex items-center justify-between gap-2">
              <span
                class="inline-flex items-center gap-1.5 rounded-full px-2 py-0.5 text-[10.5px] font-bold"
                :class="{
                  'bg-rose-50 text-rose-700': mascotSpeech.type === 'error',
                  'bg-emerald-50 text-emerald-700': mascotSpeech.type === 'success',
                  'bg-amber-50 text-amber-700': mascotSpeech.type === 'warning',
                  'bg-brand-50 text-brand-700': ['normal', 'guide', 'shy', 'curious'].includes(mascotSpeech.type)
                }"
              >
                <span
                  class="h-1.5 w-1.5 rounded-full"
                  :class="{
                    'bg-rose-500 animate-ping': mascotSpeech.type === 'error',
                    'bg-emerald-500 animate-bounce': mascotSpeech.type === 'success',
                    'bg-amber-500 animate-pulse': mascotSpeech.type === 'warning',
                    'bg-brand-500 animate-pulse': ['normal', 'guide', 'shy', 'curious'].includes(mascotSpeech.type)
                  }"
                ></span>
                {{ mascotSpeech.tag }}
              </span>
              <span class="shrink-0 text-[10px] font-semibold text-slate-400">译译</span>
            </div>

            <!-- 正文：品牌墨色，暖白底上清晰可读 -->
            <p class="text-[12.5px] font-bold leading-snug text-ink sm:text-[13.5px]">
              {{ mascotSpeech.text }}
            </p>

            <!-- 漫画尖角：从气泡左下延伸，精准指向猫头鹰喙部 -->
            <svg
              class="pointer-events-none absolute -bottom-[13px] left-[37%] h-4 w-6 text-[#FFFDF8]"
              viewBox="0 0 24 16"
              fill="currentColor"
            >
              <path d="M0 16 C8 12 14 6 18 0 L4 0 C2 5 1 11 0 16 Z" />
            </svg>
          </div>

          <!-- 纯 SVG 猫头鹰「译译」：暖沙奶油色主体，在深空蓝上形成强对撞 -->
          <div
            ref="owlRef"
            class="relative transition-transform duration-500 ease-out"
            :class="{
              'animate-shake': mascotMood === 'dizzy',
              'animate-bounce-short': mascotMood === 'success',
              'animate-think': mascotMood === 'focus',
              'anim-pop-a': gesture === 'popA',
              'anim-pop-b': gesture === 'popB',
            }"
            :style="{
              transform: `translate3d(${headTilt.x}px, ${headTilt.y}px, 0) rotate(${
                isTilting ? 12 : headTilt.rotate
              }deg)`,
            }"
          >
            <!-- 眩晕冒汗 -->
            <svg
              v-if="mascotMood === 'dizzy'"
              class="animate-sweat pointer-events-none absolute -right-1 top-10 h-7 w-7 text-ice drop-shadow-md"
              viewBox="0 0 24 24"
              fill="currentColor"
            >
              <path d="M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z" />
            </svg>

            <svg viewBox="0 0 280 280" class="w-full drop-shadow-2xl">
              <defs>
                <!-- 身体：暖沙奶油 -->
                <linearGradient id="owlBody" x1="0%" y1="0%" x2="0%" y2="100%">
                  <stop offset="0%" stop-color="#FDF8EC" />
                  <stop offset="55%" stop-color="#F5E7CB" />
                  <stop offset="100%" stop-color="#E4CDA4" />
                </linearGradient>
                <!-- 肚腹 -->
                <linearGradient id="owlBelly" x1="0%" y1="0%" x2="0%" y2="100%">
                  <stop offset="0%" stop-color="#FFFDF8" />
                  <stop offset="100%" stop-color="#FAF1DE" />
                </linearGradient>
                <!-- 脸盘 -->
                <linearGradient id="owlFace" x1="0%" y1="0%" x2="0%" y2="100%">
                  <stop offset="0%" stop-color="#FFFDF7" />
                  <stop offset="100%" stop-color="#F6E9D0" />
                </linearGradient>
                <!-- 双翼 -->
                <linearGradient id="owlWing" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stop-color="#F0DDB8" />
                  <stop offset="55%" stop-color="#E2C99F" />
                  <stop offset="100%" stop-color="#CFB389" />
                </linearGradient>
                <!-- 围巾：品牌 冰蓝 → 电蓝，与控制台主色同源 -->
                <linearGradient id="owlScarf" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stop-color="#38BDF8" />
                  <stop offset="100%" stop-color="#0D9488" />
                </linearGradient>

                <radialGradient id="owlGround" cx="50%" cy="50%" r="50%">
                  <stop offset="0%" stop-color="#0f172a" stop-opacity="0.5" />
                  <stop offset="100%" stop-color="#0f172a" stop-opacity="0" />
                </radialGradient>

                <filter id="wingShadow" x="-20%" y="-20%" width="140%" height="140%">
                  <feDropShadow dx="0" dy="6" stdDeviation="5" flood-color="#0f172a" flood-opacity="0.45" />
                </filter>
              </defs>

              <!-- 落地投影 -->
              <ellipse cx="140" cy="266" rx="76" ry="12" fill="url(#owlGround)" />

              <!-- 爪子（琥珀，与暖色身体同源） -->
              <path d="M 98 254 C 98 262 108 262 110 254 C 110 262 120 262 122 254" fill="none" stroke="#F59E0B" stroke-width="6" stroke-linecap="round" />
              <path d="M 158 254 C 158 262 168 262 170 254 C 170 262 180 262 182 254" fill="none" stroke="#F59E0B" stroke-width="6" stroke-linecap="round" />

              <!-- 耳羽 -->
              <polygon points="76,78 60,34 94,56" fill="#E2C99F" stroke="#C9AB7E" stroke-width="2.5" />
              <polygon points="204,78 220,34 186,56" fill="#E2C99F" stroke="#C9AB7E" stroke-width="2.5" />

              <!-- 身体（浅色主题下加暖褐描边，避免奶油色身体糊在白底上） -->
              <rect x="52" y="48" width="176" height="204" rx="88" fill="url(#owlBody)" stroke="#D3B78C" stroke-width="3" />

              <!-- 肚腹与绒羽纹 -->
              <path d="M 85 160 Q 140 185 195 160 Q 185 240 140 242 Q 95 240 85 160 Z" fill="url(#owlBelly)" />
              <path d="M 125 186 Q 140 194 155 186" fill="none" stroke="#DCC49B" stroke-width="2.5" stroke-linecap="round" opacity="0.9" />
              <path d="M 118 206 Q 140 216 162 206" fill="none" stroke="#DCC49B" stroke-width="2.5" stroke-linecap="round" opacity="0.7" />

              <!-- 脸盘 -->
              <circle cx="102" cy="120" r="38" fill="url(#owlFace)" stroke="#E8D3AC" stroke-width="1.5" />
              <circle cx="178" cy="120" r="38" fill="url(#owlFace)" stroke="#E8D3AC" stroke-width="1.5" />

              <!-- 眼白 -->
              <circle cx="102" cy="120" r="30" fill="#FFFFFF" />
              <circle cx="178" cy="120" r="30" fill="#FFFFFF" />

              <!-- 眼部情绪（暖褐瞳，比纯黑更贴合奶油主体） -->
              <g v-if="mascotMood === 'normal' || mascotMood === 'curious'">
                <g class="transition-transform duration-75 ease-out" :style="{ transform: `translate(${pupilOffset.x}px, ${pupilOffset.y}px)` }">
                  <circle cx="102" cy="120" :r="mascotMood === 'curious' ? 14.5 : 13" fill="#4B3A1E" />
                  <circle cx="102" cy="120" r="13" fill="none" stroke="#06B6D4" stroke-width="1.8" opacity="0.55" />
                  <circle cx="98" cy="116" r="4.2" fill="#FFFFFF" />
                  <circle cx="106" cy="123" r="2.2" fill="#FFFFFF" />
                </g>
                <g class="transition-transform duration-75 ease-out" :style="{ transform: `translate(${pupilOffset.x}px, ${pupilOffset.y}px)` }">
                  <circle cx="178" cy="120" :r="mascotMood === 'curious' ? 14.5 : 13" fill="#4B3A1E" />
                  <circle cx="178" cy="120" r="13" fill="none" stroke="#06B6D4" stroke-width="1.8" opacity="0.55" />
                  <circle cx="174" cy="116" r="4.2" fill="#FFFFFF" />
                  <circle cx="182" cy="123" r="2.2" fill="#FFFFFF" />
                </g>
              </g>

              <g v-else-if="mascotMood === 'blink'">
                <line x1="80" y1="120" x2="124" y2="120" stroke="#4B3A1E" stroke-width="4.5" stroke-linecap="round" />
                <line x1="156" y1="120" x2="200" y2="120" stroke="#4B3A1E" stroke-width="4.5" stroke-linecap="round" />
              </g>

              <g v-else-if="mascotMood === 'shy'">
                <path d="M 88 122 Q 102 136 116 122" fill="none" stroke="#4B3A1E" stroke-width="5" stroke-linecap="round" />
                <path d="M 164 122 Q 178 136 192 122" fill="none" stroke="#4B3A1E" stroke-width="5" stroke-linecap="round" />
              </g>

              <!-- 哭泣：八字愁眉 + 闭眼 + 持续滑落的眼泪 -->
              <g v-else-if="mascotMood === 'cry'">
                <path d="M 84 100 L 116 108" fill="none" stroke="#4B3A1E" stroke-width="4" stroke-linecap="round" />
                <path d="M 196 100 L 164 108" fill="none" stroke="#4B3A1E" stroke-width="4" stroke-linecap="round" />
                <path d="M 88 122 Q 102 134 116 122" fill="none" stroke="#4B3A1E" stroke-width="4.5" stroke-linecap="round" />
                <path d="M 164 122 Q 178 134 192 122" fill="none" stroke="#4B3A1E" stroke-width="4.5" stroke-linecap="round" />
              </g>

              <!-- 开心眯眼：点对字 / 悬停登录按钮 -->
              <g v-else-if="mascotMood === 'happy'">
                <path d="M 88 125 Q 102 109 116 125" fill="none" stroke="#4B3A1E" stroke-width="5" stroke-linecap="round" />
                <path d="M 164 125 Q 178 109 192 125" fill="none" stroke="#4B3A1E" stroke-width="5" stroke-linecap="round" />
              </g>

              <!-- 惊讶注视：进入验证 / 撤销点选 -->
              <g v-else-if="mascotMood === 'oops'">
                <circle cx="102" cy="120" r="16" fill="#4B3A1E" />
                <circle cx="97" cy="114" r="4.5" fill="#FFFFFF" />
                <path d="M 164 121 Q 178 129 192 121" fill="none" stroke="#4B3A1E" stroke-width="5" stroke-linecap="round" />
              </g>

              <!-- 凝神屏息：提交校验中（整组眼珠来回扫视，像在逐项核对凭证） -->
              <g v-else-if="mascotMood === 'focus'" class="animate-eye-scan">
                <circle cx="102" cy="120" r="9" fill="#4B3A1E" />
                <circle cx="102" cy="120" r="9" fill="none" stroke="#06B6D4" stroke-width="1.8" opacity="0.6" />
                <circle cx="99.5" cy="117" r="2.6" fill="#FFFFFF" />
                <circle cx="178" cy="120" r="9" fill="#4B3A1E" />
                <circle cx="178" cy="120" r="9" fill="none" stroke="#06B6D4" stroke-width="1.8" opacity="0.6" />
                <circle cx="175.5" cy="117" r="2.6" fill="#FFFFFF" />
              </g>

              <g v-else-if="mascotMood === 'dizzy'">
                <path d="M 102 120 m -16 0 a 16 16 0 1 0 32 0 a 11 11 0 1 0 -22 0 a 6 6 0 1 0 12 0" fill="none" stroke="#06B6D4" stroke-width="3.5" stroke-linecap="round" />
                <path d="M 178 120 m -16 0 a 16 16 0 1 0 32 0 a 11 11 0 1 0 -22 0 a 6 6 0 1 0 12 0" fill="none" stroke="#06B6D4" stroke-width="3.5" stroke-linecap="round" />
              </g>

              <g v-else-if="mascotMood === 'success'">
                <path d="M 102 105 L 106 116 L 117 120 L 106 124 L 102 135 L 98 124 L 87 120 L 98 116 Z" fill="#F59E0B" />
                <path d="M 178 105 L 182 116 L 193 120 L 182 124 L 178 135 L 174 124 L 163 120 L 174 116 Z" fill="#F59E0B" />
              </g>

              <!-- 腮红（低饱和，与暖色主体协调） -->
              <ellipse cx="76" cy="146" rx="11" ry="6" fill="#FCA5A5" class="transition-opacity duration-300" :style="{ opacity: mascotMood === 'shy' ? 0.85 : 0.2 }" />
              <ellipse cx="204" cy="146" rx="11" ry="6" fill="#FCA5A5" class="transition-opacity duration-300" :style="{ opacity: mascotMood === 'shy' ? 0.85 : 0.2 }" />

              <!-- 鸟喙 -->
              <polygon points="140,128 149,146 131,146" fill="#F59E0B" stroke="#D97706" stroke-width="1.5" />

              <!-- 围巾（品牌蓝，点睛一笔把吉祥物与产品主色绑定） -->
              <path d="M 76 168 Q 140 188 204 168 Q 206 186 198 194 Q 140 208 82 194 Q 74 184 76 168 Z" fill="url(#owlScarf)" />
              <path d="M 166 182 L 192 182 L 186 230 L 164 226 Z" fill="url(#owlScarf)" />
              <line x1="165" y1="228" x2="186" y2="231" stroke="#7DD3FC" stroke-width="2.5" stroke-dasharray="2 3" />

              <!-- 哭泣：😭 式两道持续流淌的泪痕（绘制在围巾之后，泪水可流过围巾） -->
              <g v-if="mascotMood === 'cry'">
                <path
                  class="tear-stream"
                  fill="#38BDF8"
                  d="M 98 130 C 95 156 94 182 96 202 C 97 210 107 210 108 202 C 110 182 109 156 106 130 Z"
                />
                <path
                  class="tear-stream"
                  fill="#38BDF8"
                  d="M 174 130 C 171 156 170 182 172 202 C 173 210 183 210 184 202 C 186 182 185 156 182 130 Z"
                />
                <g fill="#06B6D4">
                  <g class="tear-drop drop-a">
                    <path transform="translate(102,132)" d="M 0 0 C 3.2 4.6 5 7.6 5 9.8 A 5 5 0 0 1 -5 9.8 C -5 7.6 -3.2 4.6 0 0 Z" />
                  </g>
                  <g class="tear-drop drop-b">
                    <path transform="translate(102,132)" d="M 0 0 C 3.2 4.6 5 7.6 5 9.8 A 5 5 0 0 1 -5 9.8 C -5 7.6 -3.2 4.6 0 0 Z" />
                  </g>
                  <g class="tear-drop drop-c">
                    <path transform="translate(178,132)" d="M 0 0 C 3.2 4.6 5 7.6 5 9.8 A 5 5 0 0 1 -5 9.8 C -5 7.6 -3.2 4.6 0 0 Z" />
                  </g>
                  <g class="tear-drop drop-d">
                    <path transform="translate(178,132)" d="M 0 0 C 3.2 4.6 5 7.6 5 9.8 A 5 5 0 0 1 -5 9.8 C -5 7.6 -3.2 4.6 0 0 Z" />
                  </g>
                </g>
              </g>

              <!-- 双翼 -->
              <g
                class="owl-wing transition-all duration-500 ease-out"
                :style="{
                  transformOrigin: '64px 150px',
                  transform: mascotMood === 'shy'
                    ? 'translate(28px, -24px) rotate(58deg) scale(0.84, 0.80)'
                    : 'translate(0px, 0px) rotate(0deg) scale(1, 1)',
                  filter: mascotMood === 'shy' ? 'url(#wingShadow)' : 'none'
                }"
              >
                <path
                  d="M 64 148 C 42 165 38 196 46 226 C 52 232 58 231 63 223 C 67 217 71 226 77 220 C 82 214 82 204 84 195 C 87 180 82 164 74 150 Z"
                  fill="url(#owlWing)"
                />
                <path d="M 48 185 C 44 205 52 222 56 223" fill="none" stroke="#C9AB7E" stroke-width="2" stroke-linecap="round" opacity="0.6" />
              </g>

              <g
                class="owl-wing transition-all duration-500 ease-out"
                :style="{
                  transformOrigin: '216px 150px',
                  transform: mascotMood === 'shy'
                    ? 'translate(-28px, -24px) rotate(-58deg) scale(0.84, 0.80)'
                    : 'translate(0px, 0px) rotate(0deg) scale(1, 1)',
                  filter: mascotMood === 'shy' ? 'url(#wingShadow)' : 'none'
                }"
              >
                <path
                  d="M 216 148 C 238 165 242 196 234 226 C 228 232 222 231 217 223 C 213 217 209 226 203 220 C 198 214 198 204 196 195 C 193 180 198 164 206 150 Z"
                  fill="url(#owlWing)"
                />
                <path d="M 232 185 C 236 205 228 222 224 223" fill="none" stroke="#C9AB7E" stroke-width="2" stroke-linecap="round" opacity="0.6" />
              </g>
            </svg>
          </div>
        </div>

        <!-- 能力条：浅色胶囊，与浅色品牌面板统一 -->
        <div class="mt-6 flex flex-wrap items-center justify-center gap-x-4 gap-y-2 rounded-full border border-line bg-white/80 px-4 py-2 backdrop-blur-md">
          <span class="inline-flex items-center gap-1.5 text-[11.5px] font-medium text-slate-600">
            <i class="fa-solid fa-language text-ice"></i>多语种实时互译
          </span>
          <span class="hidden h-3 w-px bg-line sm:block"></span>
          <span class="inline-flex items-center gap-1.5 text-[11.5px] font-medium text-slate-600">
            <i class="fa-solid fa-bolt text-ice"></i>4K 60FPS · 延迟 &lt; 18ms
          </span>
          <span class="hidden h-3 w-px bg-line sm:block"></span>
          <span class="inline-flex items-center gap-1.5 text-[11.5px] font-medium text-slate-600">
            <i class="fa-solid fa-cube text-ice"></i>空间级双目渲染
          </span>
        </div>
      </div>

      <div class="relative z-10 text-[11px] text-slate-400">
        © 2026 视界译 VisionTrans · 毫秒级双目注视点空间翻译渲染内核
      </div>
    </div>

    <!-- ==================== 右：登录卡片（与应用内容区同为钛白底，视觉连续） ==================== -->
    <div class="relative flex flex-1 items-center justify-center px-4 py-10 lg:p-12">
      <!-- 内容区微弱蓝光，与左侧深色形成柔和过渡 -->
      <div class="pointer-events-none absolute inset-0 bg-[radial-gradient(600px_400px_at_50%_0%,rgba(13,148,136,0.06),transparent_70%)]"></div>

      <div class="relative w-full max-w-[420px]">
        <div class="relative overflow-hidden rounded-2xl border border-line bg-white p-7 shadow-card">
          <!-- 卡片顶部品牌细线，呼应侧栏渐变 -->
          <div class="absolute inset-x-0 top-0 h-[3px]" style="background: linear-gradient(90deg, #0f766e, #0d9488 55%, #06b6d4)"></div>

          <div class="mb-5">
            <!-- 身份选择：管理员 / 商户，分别走独立的登录接口与令牌体系 -->
            <div class="mb-4 inline-flex rounded-xl border border-line bg-slate-50 p-1">
              <button
                type="button"
                class="rounded-lg px-3 py-1.5 text-[12.5px] font-medium transition"
                :class="identity === 'admin' ? 'bg-white text-electric shadow-sm' : 'text-sub hover:text-ink'"
                @click="identity = 'admin'"
              >
                管理员
              </button>
              <button
                type="button"
                class="rounded-lg px-3 py-1.5 text-[12.5px] font-medium transition"
                :class="identity === 'merchant' ? 'bg-white text-electric shadow-sm' : 'text-sub hover:text-ink'"
                @click="identity = 'merchant'"
              >
                商户
              </button>
            </div>
            <h2 class="text-[17px] font-bold tracking-tight text-ink">
              {{ identity === 'merchant' ? '商户登录' : '管理员登录' }}
            </h2>
            <p class="mt-1 text-[12px] text-sub">
              {{
                identity === 'merchant'
                  ? '请输入商户授权凭证以进入独立工作台'
                  : '请输入系统授权凭证以访问控制台'
              }}
            </p>
          </div>

          <!-- 账号 -->
          <label class="lbl">登录账号</label>
          <div class="relative mb-4">
            <i class="fa-regular fa-user absolute left-3.5 top-1/2 -translate-y-1/2 text-[13px] text-slate-400"></i>
            <input
              v-model="form.username"
              class="field pl-10"
              placeholder="请输入账号"
              autocomplete="username"
              @focus="isUsernameFocused = true"
              @blur="isUsernameFocused = false"
            />
          </div>

          <!-- 口令 -->
          <label class="lbl">登录口令</label>
          <div class="relative mb-4">
            <i class="fa-solid fa-lock absolute left-3.5 top-1/2 -translate-y-1/2 text-[13px] text-slate-400"></i>
            <input
              v-model="form.password"
              type="password"
              class="field pl-10 pr-9"
              placeholder="请输入口令"
              autocomplete="current-password"
              @focus="isPasswordFocused = true"
              @blur="isPasswordFocused = false"
            />
            <i
              class="fa-solid fa-key absolute right-3.5 top-1/2 -translate-y-1/2 text-[11px] text-emerald-500"
              title="口令经 RSA-2048 公钥加密后传输"
            ></i>
          </div>

          <!-- 主界面红色错误 -->
          <div
            v-if="formError && !captchaOpen"
            class="mb-4 flex items-start gap-2 rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-[12.5px] font-medium text-rose-600"
          >
            <i class="fa-solid fa-circle-exclamation mt-0.5 shrink-0"></i>
            <span>{{ formError }}</span>
          </div>

          <button
            type="button"
            class="btn btn-primary w-full justify-center py-2.5 text-[13.5px]"
            @click="openCaptcha"
            @mouseenter="playGesture('pop', 600)"
          >
            <span>登 录</span>
          </button>

          <div class="mt-4 rounded-lg border border-brand-100 bg-brand-50 p-2.5 text-center text-[11.5px] text-sub">
            <template v-if="identity === 'merchant'">
              演示账号 <code class="rounded bg-brand-50 px-1 py-0.5 font-mono text-ink">merchant</code> / 口令
              <code class="rounded bg-brand-50 px-1 py-0.5 font-mono text-ink">merchant123</code> · RSA 保护
            </template>
            <template v-else>
              演示账号 <code class="rounded bg-brand-50 px-1 py-0.5 font-mono text-ink">admin</code> / 口令
              <code class="rounded bg-brand-50 px-1 py-0.5 font-mono text-ink">admin123</code> · RSA 保护
            </template>
          </div>

          <!-- 卡片内验证抽屉 -->
          <transition name="card-drawer">
            <div
              v-if="captchaOpen"
              class="absolute inset-0 z-30 flex flex-col justify-between bg-white p-7"
            >
              <div>
                <div class="mb-3 flex items-center justify-between">
                  <button
                    type="button"
                    class="inline-flex items-center gap-1.5 text-[12px] font-medium text-sub transition hover:text-electric"
                    @click="closeCaptcha"
                  >
                    <i class="fa-solid fa-arrow-left text-[11px]"></i>
                    <span>返回修改账号口令</span>
                  </button>
                  <span class="rounded bg-brand-50 px-2 py-0.5 text-[11px] font-semibold text-electric">安全挑战</span>
                </div>

                <div class="mb-2 flex items-center justify-between text-[12px]">
                  <div>
                    <span class="text-sub">请依次点击：</span>
                    <span class="font-bold text-electric">{{ captcha?.hint || '…' }}</span>
                  </div>
                  <div class="flex items-center gap-2">
                    <button
                      v-if="clicks.length > 0"
                      type="button"
                      class="text-[11px] font-medium text-amber-600 transition hover:text-amber-700"
                      @click="undoLastClick"
                    >
                      <i class="fa-solid fa-arrow-rotate-left mr-0.5"></i>撤销
                    </button>
                    <button
                      type="button"
                      class="text-[11px] text-sub transition hover:text-electric"
                      @click="loadCaptcha(true)"
                    >
                      <i class="fa-solid fa-rotate mr-0.5"></i>换一张
                    </button>
                  </div>
                </div>

                <!-- 定比验证码图片 -->
                <div
                  class="relative mb-3 overflow-hidden rounded-lg border bg-slate-100 transition-all duration-300"
                  :class="clicks.length === 3 ? 'border-emerald-500 ring-2 ring-emerald-400/20' : 'border-line'"
                  style="aspect-ratio: 320 / 150;"
                >
                  <img
                    v-if="captcha"
                    ref="imgEl"
                    :src="captcha.image"
                    class="block h-full w-full cursor-crosshair select-none object-cover"
                    alt="点击验证码"
                    draggable="false"
                    @click="onCaptchaClick"
                  />
                  <div v-else class="flex h-full w-full flex-col items-center justify-center gap-2 bg-slate-100 text-slate-400">
                    <div class="h-4 w-28 animate-pulse rounded bg-slate-200"></div>
                    <span class="inline-flex items-center gap-1.5 text-[11.5px]">
                      <i class="fa-solid fa-circle-notch fa-spin text-[10px]"></i>验证码载入中…
                    </span>
                  </div>

                  <!-- 序数光标 -->
                  <div
                    v-for="(c, i) in clicks"
                    :key="i"
                    class="pointer-events-none absolute -translate-x-1/2 -translate-y-1/2"
                    :style="{ left: `${(c.x / CAPTCHA_W) * 100}%`, top: `${(c.y / CAPTCHA_H) * 100}%` }"
                  >
                    <span class="relative flex h-5 w-5 items-center justify-center">
                      <span class="absolute inline-flex h-full w-full animate-ping rounded-full bg-electric opacity-40"></span>
                      <span class="relative grid h-5 w-5 place-items-center rounded-full bg-electric text-[10px] font-bold text-white shadow ring-2 ring-white">
                        {{ i + 1 }}
                      </span>
                    </span>
                  </div>
                </div>

                <!-- 明确精准的红色报错 -->
                <div
                  v-if="formError"
                  class="mb-3 flex items-start gap-2 rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-[12px] font-medium text-rose-600"
                >
                  <i class="fa-solid fa-circle-exclamation mt-0.5 shrink-0"></i>
                  <span>{{ formError }}</span>
                </div>
              </div>

              <button
                type="button"
                class="btn btn-primary w-full justify-center py-2.5 text-[13.5px]"
                :class="{ 'ring-2 ring-emerald-500/30': clicks.length === 3 }"
                :disabled="loading || clicks.length < 3"
                @click="confirmLogin"
                @mouseenter="clicks.length === 3 && playGesture('pop', 600)"
              >
                <i v-if="loading" class="fa-solid fa-circle-notch fa-spin mr-1.5"></i>
                <span>{{ loading ? '身份凭证校验中…' : clicks.length === 3 ? '确认并登录' : `请点击剩余 ${3 - clicks.length} 处文字` }}</span>
              </button>
            </div>
          </transition>
        </div>

        <div class="mt-5 text-center text-[11px] text-slate-400">
          所有管理审计会话均受不可篡改日志系统监控
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 漫画气泡：柔和悬浮 + 深空投影，与暖白底协调 */
.comic-bubble {
  box-shadow: 0 18px 40px -12px rgba(4, 18, 47, 0.55), 0 2px 6px rgba(4, 18, 47, 0.12);
  animation: bubbleSway 4s ease-in-out infinite alternate;
}
@keyframes bubbleSway {
  0% {
    transform: translateY(0px) rotate(0deg);
  }
  100% {
    transform: translateY(-5px) rotate(0.5deg);
  }
}

/* 翅膀铰链：显式使用 view-box，保证 transformOrigin 落在肩关节用户坐标上 */
.owl-wing {
  transform-box: view-box;
}

/* 卡片内部抽屉平滑滑入动效 */
.card-drawer-enter-active,
.card-drawer-leave-active {
  transition: transform 0.32s cubic-bezier(0.16, 1, 0.3, 1), opacity 0.28s ease;
}
.card-drawer-enter-from {
  transform: translateY(100%);
  opacity: 0;
}
.card-drawer-leave-to {
  transform: translateY(100%);
  opacity: 0;
}

/* 失败摇晃 */
.animate-shake {
  animation: shakeBody 0.4s ease-in-out 3;
}
@keyframes shakeBody {
  0%,
  100% {
    transform: translateX(0);
  }
  25% {
    transform: translateX(-6px) rotate(-3deg);
  }
  75% {
    transform: translateX(6px) rotate(3deg);
  }
}

/* 成功跳跃 */
.animate-bounce-short {
  animation: jumpHappy 0.6s cubic-bezier(0.175, 0.885, 0.32, 1.275) 2;
}
@keyframes jumpHappy {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-22px) scale(1.05);
  }
}

/* 流汗 */
.animate-sweat {
  animation: sweatDrop 1s cubic-bezier(0.4, 0, 0.2, 1) infinite;
}
@keyframes sweatDrop {
  0% {
    opacity: 0;
    transform: translateY(-8px) scale(0.7);
  }
  30% {
    opacity: 1;
  }
  80% {
    opacity: 0.9;
    transform: translateY(12px) scale(1);
  }
  100% {
    opacity: 0;
    transform: translateY(18px) scale(0.6);
  }
}

/* 哭泣：😭 式双行眼泪 —— 两道泪痕 + 沿泪痕不断滚落的泪珠 */
.tear-stream {
  transform-box: fill-box;
  transform-origin: center top;
  animation: tearFlow 1.6s ease-in-out infinite;
}
@keyframes tearFlow {
  0%,
  100% {
    opacity: 0.4;
    transform: scaleY(0.92);
  }
  50% {
    opacity: 0.72;
    transform: scaleY(1);
  }
}

.tear-drop {
  transform-box: view-box;
  animation: tearFall 1.15s ease-in infinite;
}
.drop-b {
  animation-delay: 0.55s;
}
.drop-c {
  animation-delay: 0.28s;
}
.drop-d {
  animation-delay: 0.83s;
}
@keyframes tearFall {
  0% {
    opacity: 0;
    transform: translateY(0) scale(0.6);
  }
  15% {
    opacity: 1;
    transform: translateY(6px) scale(1);
  }
  85% {
    opacity: 0.9;
  }
  100% {
    opacity: 0;
    transform: translateY(84px) scale(0.75);
  }
}

/* 点选/悬停小脉冲（A/B 交替以支持连续触发重放） */
.anim-pop-a,
.anim-pop-b {
  animation: popBeat 0.35s ease-out;
}
@keyframes popBeat {
  0% {
    transform: scale(1);
  }
  40% {
    transform: scale(1.045);
  }
  100% {
    transform: scale(1);
  }
}

/* 校验中思考：身体小幅摇摆 + 眼珠来回扫视，把「正在核对」演出来（消除一动不动的诡异感） */
.animate-think {
  animation: owlThink 1.8s ease-in-out infinite;
  transform-origin: 50% 92%;
}
@keyframes owlThink {
  0%,
  100% {
    transform: rotate(-2.2deg) translateY(0);
  }
  50% {
    transform: rotate(2.2deg) translateY(-5px);
  }
}
.animate-eye-scan {
  animation: eyeScan 1.8s ease-in-out infinite;
}
@keyframes eyeScan {
  0%,
  100% {
    transform: translateX(-6px);
  }
  50% {
    transform: translateX(6px);
  }
}

@media (prefers-reduced-motion: reduce) {
  .animate-shake,
  .animate-bounce-short,
  .animate-sweat,
  .animate-think,
  .animate-eye-scan,
  .comic-bubble,
  .anim-pop-a,
  .anim-pop-b,
  .tear-stream,
  .tear-drop {
    animation: none;
  }
}
</style>
