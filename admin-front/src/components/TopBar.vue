<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'

import ConfirmDialog from '@/components/ConfirmDialog.vue'
import StatusPill from '@/components/StatusPill.vue'
import MerchantNavDrawer from '@/jingchen/components/MerchantNavDrawer.vue'
import { useAppStore } from '@/stores/app'
import { useClusterStore } from '@/stores/cluster'
import { useAuthStore } from '@/stores/auth'
import { useConfirm } from '@/composables/useConfirm'

const app = useAppStore()
const cluster = useClusterStore()
const auth = useAuthStore()
const route = useRoute()

const { confirmState, askConfirm, resolveConfirm } = useConfirm()

/** 商户没有后台侧栏，汉堡按钮改为呼出商户端导航抽屉（jingchen 模块组件） */
const merchantNavOpen = ref(false)
const onToggleNav = () => {
  if (auth.isMerchant) merchantNavOpen.value = true
  else app.toggleSide()
}

const logout = async () => {
  const ok = await askConfirm('退出后将结束当前会话并返回登录页，需要重新验证身份后才能再次进入。', '确认退出登录')
  if (!ok) return
  await auth.logout()
  // bye=1 让登录页的「译译」以哭泣相迎
  location.href = '/login?bye=1'
}

/** 确认框中醒目展示当前登录身份，一眼看清"谁要退出" */
const logoutSubject = computed(() =>
  auth.user
    ? { avatar: auth.user.avatar || '管', name: auth.user.name || '—', role: auth.user.role || '系统管理员' }
    : null
)

const meta = computed(() => app.metaOf(String(route.name || '')))
const title = computed(() => meta.value?.title || route.meta.title || '平台管理后台')
const crumb = computed(() => {
  if (auth.isMerchant) {
    // 商户页面不在后台菜单 meta 里，直接用页面标题组装面包屑
    const t = route.meta.title || ''
    return t === '商户工作台' ? '商户工作台' : `商户工作台 / ${t}`
  }
  const m = meta.value
  if (!m) return '平台管理后台'
  return `平台管理后台 / ${m.title} / ${m.sub}`
})

/* ------------------------------ 全局搜索 ------------------------------ */

const searchInput = ref(null)
/**
 * 窄屏（< md）下搜索框默认隐藏以让位给其它操作区；
 * 若此时按 /，直接 focus 一个 display:none 的元素是无效的（什么都不显示），
 * 因此快捷键会先强制展开搜索框，再在下一个 tick 聚焦。
 */
const searchForced = ref(false)

const focusSearch = async () => {
  searchForced.value = true
  await nextTick()
  searchInput.value?.focus()
}
defineExpose({ focusSearch })

/** 失焦且无关键字时收起，避免窄屏下长期占位 */
const onSearchBlur = () => {
  if (!app.keyword) searchForced.value = false
}

/**
 * 搜索框内按 / 不录入字符：/ 是全局聚焦快捷键，
 * 若被当普通字符存进关键字（如 "/深圳"），会导致整表匹配不到任何数据。
 */
const onSearchKeydown = (e) => {
  if (e.key === '/') {
    e.preventDefault()
  } else if (e.key === 'Escape') {
    app.setKeyword('')
  }
}

/* ------------------------------ 通知铃铛 ------------------------------ */

const bellOpen = ref(false)

/** 顶部铃铛展示由后端依据 metric_sample 真实采样推导的告警（随日期范围变化） */
const alerts = computed(() => cluster.data?.alerts || [])
const alarms = computed(() => alerts.value.slice(0, 5))
const badge = computed(() => alerts.value.length || 0)

// 应用启动即拉取一次，保证各页面铃铛角标与内容都是真实数据；
// 仅当身份已由 /auth/me 校验通过（auth.user 非空）时才请求，避免无效令牌触发 401 提示
onMounted(() => {
  // 商户无权访问集群接口（后端 @RequireRole 会 403），不拉取以免产生无意义的报错提示
  if (auth.user && !auth.isMerchant && !cluster.data && !cluster.loading) cluster.load()
})

const toggleBell = async () => {
  bellOpen.value = !bellOpen.value
  if (bellOpen.value && !cluster.data && !cluster.loading) {
    await cluster.load()
  }
}
</script>

<template>
  <header class="glass relative z-30 flex h-[62px] flex-none items-center gap-4 border-b border-line px-6">
    <div class="flex min-w-0 items-center gap-3">
      <button
        class="grid h-8 w-8 place-items-center rounded-lg border border-line bg-white text-sub transition hover:border-brand-200 hover:text-electric"
        :title="auth.isMerchant ? '打开商户导航' : '折叠 / 展开侧栏（H）'"
        @click="onToggleNav"
      >
        <i class="fa-solid fa-bars text-[12px]"></i>
      </button>
      <div class="min-w-0">
        <div class="text-[15px] font-semibold leading-tight">{{ title }}</div>
        <div class="text-[11px] leading-tight text-sub">{{ crumb }}</div>
      </div>
    </div>

    <div class="flex-1"></div>

    <div
      class="inline-flex items-center gap-2 rounded-lg px-3 py-1.5 text-[12.5px] font-semibold"
      style="
        background: linear-gradient(135deg, #115E59, #0F766E 55%, #06B6D4);
        color: #fff;
        box-shadow: 0 6px 16px -8px rgba(6, 182, 212, 0.9);
      "
    >
      <i class="fa-solid fa-server"></i>{{ auth.isMerchant ? '商户工作台 · Merchant Console' : app.system?.consoleLabel || '平台管理后台 · Admin Console' }}
    </div>

    <div
      class="hidden items-center gap-1.5 rounded-lg border border-line bg-white px-2.5 py-1 text-[12px] text-sub xl:flex"
      title="选择日期范围，各页面大盘数据将按此过滤"
    >
      <i class="fa-regular fa-calendar text-ice"></i>
      <input
        v-model="app.dateStart"
        type="date"
        class="num rounded-md border-none bg-transparent px-1 py-0.5 text-[12px] text-sub outline-none transition hover:bg-slate-50 focus:bg-slate-50 focus:ring-2 focus:ring-electric/20"
        @change="app.setRange(app.dateStart, app.dateEnd, 'start')"
      />
      <span class="text-slate-300">~</span>
      <input
        v-model="app.dateEnd"
        type="date"
        class="num rounded-md border-none bg-transparent px-1 py-0.5 text-[12px] text-sub outline-none transition hover:bg-slate-50 focus:bg-slate-50 focus:ring-2 focus:ring-electric/20"
        @change="app.setRange(app.dateStart, app.dateEnd, 'end')"
      />
    </div>

    <!-- md 起常驻；更窄时由 / 快捷键强制展开（商户页面无表格数据，不展示） -->
    <div v-if="!auth.isMerchant" class="relative" :class="searchForced ? 'block' : 'hidden md:block'">
      <i class="fa-solid fa-magnifying-glass absolute left-3 top-1/2 -translate-y-1/2 text-[11px] text-slate-400"></i>
      <input
        ref="searchInput"
        v-model="app.keyword"
        class="w-52 rounded-lg border border-line bg-white py-1.5 pl-8 pr-7 text-[12.5px] transition-all duration-150 focus:border-electric focus:outline-none focus:ring-2 focus:ring-electric/15"
        placeholder="输入关键字过滤本页数据，快捷键 /"
        @blur="onSearchBlur"
        @keydown="onSearchKeydown"
      />
      <button
        v-if="app.keyword"
        class="absolute right-2 top-1/2 -translate-y-1/2 text-[10px] text-slate-400 transition hover:text-ink"
        title="清空"
        @click="app.setKeyword('')"
      >
        <i class="fa-solid fa-xmark"></i>
      </button>
    </div>

    <!-- 通知（集群告警属后台管理数据，商户不展示） -->
    <div v-if="!auth.isMerchant" class="relative">
      <button
        class="relative h-9 w-9 rounded-lg border border-line bg-white text-sub transition-all duration-150 hover:border-brand-200 hover:text-electric"
        title="最新告警"
        @click="toggleBell"
      >
        <i class="fa-regular fa-bell"></i>
        <span
          v-if="badge"
          class="absolute -right-1 -top-1 grid h-4 min-w-4 place-items-center rounded-full bg-rose-500 px-1 text-[9px] font-bold text-white"
          >{{ badge }}</span
        >
      </button>

      <div
        v-if="bellOpen"
        class="absolute right-0 top-11 z-40 w-80 rounded-xl border border-line bg-white p-3 shadow-lift"
      >
        <div class="mb-2 flex items-center justify-between">
          <span class="text-[12.5px] font-semibold text-ink">最新告警</span>
          <router-link
            class="text-[11px] text-electric transition hover:underline"
            :to="{ name: 'a6' }"
            @click="bellOpen = false"
            >查看全部</router-link
          >
        </div>
        <div v-if="alarms.length" class="space-y-1.5">
          <div v-for="alarm in alarms" :key="alarm.id" class="rounded-lg border border-line p-2">
            <div class="flex items-center gap-2">
              <StatusPill :text="alarm.level" :tone="alarm.level === 'P1' ? 'red' : alarm.level === 'P2' ? 'amber' : 'blue'" small />
              <span class="num text-[10.5px] text-sub">{{ alarm.time }}</span>
            </div>
            <div class="mt-1 line-clamp-2 text-[11.5px] leading-snug text-ink">{{ alarm.message }}</div>
          </div>
        </div>
        <div v-else class="py-4 text-center text-[11.5px] text-sub">
          <i v-if="cluster.loading" class="fa-solid fa-circle-notch fa-spin"></i>
          <span v-else>暂无告警</span>
        </div>
      </div>
    </div>

    <button
      class="h-9 w-9 rounded-lg border border-line bg-white text-sub transition-all duration-150 hover:border-brand-200 hover:text-electric"
      title="使用说明与快捷键（?）"
      @click="app.toggleHelp(true)"
    >
      <i class="fa-regular fa-circle-question"></i>
    </button>

    <!-- 当前登录用户 + 退出 -->
    <div class="flex items-center gap-2 rounded-lg border border-line bg-white py-1 pl-1 pr-2.5">
      <div
        class="grid h-7 w-7 place-items-center rounded-full text-[12px] font-semibold text-white"
        style="background: linear-gradient(135deg, #115E59, #06B6D4)"
      >
        {{ auth.user?.avatar || '管' }}
      </div>
      <div class="hidden leading-tight sm:block">
        <div class="text-[12px] font-medium text-ink">{{ auth.user?.name || '—' }}</div>
        <div class="text-[10px] text-sub">{{ auth.user?.role || '' }}</div>
      </div>
      <button
        class="ml-1 h-7 w-7 rounded-md text-sub transition hover:bg-rose-50 hover:text-rose-500"
        title="退出登录"
        @click="logout"
      >
        <i class="fa-solid fa-right-from-bracket text-[12px]"></i>
      </button>
    </div>

    <!-- 退出确认：Teleport 到 body，避免毛玻璃顶栏的 backdrop-filter 形成包含块导致浮层错位 -->
    <Teleport to="body">
      <ConfirmDialog
        v-bind="confirmState"
        :subject="logoutSubject"
        danger
        confirm-text="退出登录"
        @resolve="resolveConfirm"
      />
      <MerchantNavDrawer :open="merchantNavOpen" @close="merchantNavOpen = false" />
    </Teleport>
  </header>
</template>
