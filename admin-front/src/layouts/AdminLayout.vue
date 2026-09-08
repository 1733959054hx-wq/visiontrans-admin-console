<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import SideBar from '@/components/SideBar.vue'
import ToastHost from '@/components/ToastHost.vue' // Toast 已上移至 App.vue 全局挂载
import TopBar from '@/components/TopBar.vue'
import { useAppStore } from '@/stores/app'
import { useAuthStore } from '@/stores/auth'

const app = useAppStore()
const auth = useAuthStore()
const router = useRouter()

const menus = computed(() => app.nav.items)
const topBar = ref(null)
/** 商户只有顶部导航栏，不展示后台管理侧栏 */
const showSideBar = computed(() => !auth.isMerchant)

onMounted(() => {
  app.loadMeta()
  window.addEventListener('keydown', onKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', onKeydown)
})

/** 原型快捷键：F 全屏、H 折叠侧栏、1~5 切换模块、? 打开说明 */
function onKeydown(event) {
  const tag = event.target?.tagName
  if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT') return
  const key = event.key.toLowerCase()
  if (key === 'f') {
    if (!document.fullscreenElement) document.documentElement.requestFullscreen?.()
    else document.exitFullscreen?.()
  } else if (key === 'h') {
    app.toggleSide()
  } else if (key === '/' ) {
    event.preventDefault()
    topBar.value?.focusSearch()
  } else if (key === '?' || (key === '/' && event.shiftKey)) {
    app.toggleHelp()
  } else if (!auth.isMerchant && ['1', '2', '3', '4', '5'].includes(key)) {
    // 数字键切换的是后台管理模块，商户无此菜单，直接忽略
    const target = menus.value[Number(key) - 1]
    if (target) router.push({ name: target.id })
  }
}
</script>

<template>
  <div class="flex h-screen overflow-hidden">
    <SideBar v-if="showSideBar" />

    <div class="flex min-w-0 flex-1 flex-col">
      <TopBar ref="topBar" />
      <main class="flex-1 overflow-y-auto bg-titan">
        <router-view v-slot="{ Component }">
          <component :is="Component" />
        </router-view>
      </main>
    </div>

    <!-- 帮助弹窗 -->
    <div
      v-if="app.helpOpen"
      class="fixed inset-0 z-[75] grid place-items-center bg-ink/30 p-4 backdrop-blur-sm"
      @click.self="app.toggleHelp(false)"
    >
      <div class="w-full max-w-lg rounded-2xl bg-white p-6 shadow-lift">
        <div class="flex items-center justify-between">
          <div class="text-[15px] font-semibold text-ink">使用说明</div>
          <button class="text-sub transition hover:text-ink" @click="app.toggleHelp(false)">
            <i class="fa-solid fa-xmark"></i>
          </button>
        </div>
        <div class="mt-4 space-y-2 text-[12.5px] leading-relaxed text-sub">
          <p>本后台 5 个模块均支持真实增删改查，所有写操作会落库并写入不可篡改的操作日志。</p>

          <div class="mt-3 rounded-xl border border-line p-3">
            <div class="mb-1.5 text-[12.5px] font-semibold text-ink">顶栏功能</div>
            <ul class="mt-2 space-y-1.5">
              <li>
                <b class="text-ink">搜索</b>：按 <kbd class="kbd">/</kbd> 聚焦后输入关键字，实时过滤<b class="text-ink">本页</b>表格
                （如集群页输入「深圳」「P1」「已自愈」）；切换页面自动清空，框内按
                <kbd class="kbd">/</kbd> 不会录入字符，<kbd class="kbd">Esc</kbd> 一键清空。结果为空说明关键字没有命中任何字段。
              </li>
              <li>
                <b class="text-ink">日期范围</b>：右上角起止日期过滤<b class="text-ink">所有页面</b>的大盘数据，
                起始晚于结束会自动纠正为合法区间。
              </li>
              <li>
                <b class="text-ink">铃铛</b>：展示由真实指标采样推导的实时告警 —— 可用率、数据时延、越权拦截、
                模型回滚、广告填充率任一越阈值时出现，条目与数值随所选日期范围变化；
                「查看全部」跳转集群大盘的告警台账。
              </li>
              <li><b class="text-ink">导出</b>：列表右上角导出按钮生成 CSV（Excel 可直接打开），仅导出当前过滤结果。</li>
              <li><b class="text-ink">权限树</b>：点击权限项在「已授权 / 部分授权 / 未授权」之间循环，点保存生效。</li>
              <li><b class="text-ink">退出登录</b>：需二次确认，并醒目展示当前登录身份（头像 / 姓名 / 角色），防止误退。</li>
            </ul>
          </div>

          <div class="rounded-xl border border-line p-3">
            <div class="mb-1.5 text-[12.5px] font-semibold text-ink">登录与安全</div>
            <ul class="mt-2 space-y-1.5">
              <li>口令经 <b class="text-ink">RSA-2048</b> 公钥加密后才传输，服务端私钥解密。</li>
              <li>
                登录需完成<b class="text-ink">点击式验证码</b>：按提示在图中依次点击 3 个汉字（目标字互不重复，
                可「撤销」上一步或「换一张」），是否点对由后端按坐标校验，前端不做判定。
              </li>
              <li>未登录访问任何页面会被重定向回登录页；令牌失效后自动踢回并清理本地状态。</li>
            </ul>
          </div>

          <div class="mt-4 rounded-xl border border-line p-3">
            <div class="mb-1.5 text-[12.5px] font-semibold text-ink">快捷键</div>
            <div class="grid grid-cols-2 gap-1.5 text-[12px]">
              <span><kbd class="kbd">F</kbd> 全屏</span>
              <span><kbd class="kbd">H</kbd> 折叠 / 展开侧栏</span>
              <span><kbd class="kbd">1</kbd> ~ <kbd class="kbd">5</kbd> 切换模块</span>
              <span><kbd class="kbd">/</kbd> 聚焦搜索（任意窗口宽度可用）</span>
              <span><kbd class="kbd">Esc</kbd> 清空搜索关键字</span>
              <span><kbd class="kbd">?</kbd> 打开 / 关闭本说明</span>
            </div>
          </div>
        </div>
        <div class="mt-5 flex justify-end">
          <button class="btn btn-primary btn-sm" @click="app.toggleHelp(false)">知道了</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.kbd {
  display: inline-block;
  min-width: 18px;
  padding: 1px 5px;
  margin-right: 4px;
  border: 1px solid #e2e8f0;
  border-bottom-width: 2px;
  border-radius: 5px;
  background: #f8fafc;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 11px;
  text-align: center;
  color: #334155;
}
</style>
