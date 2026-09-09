<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import ConfirmDialog from '@/components/ConfirmDialog.vue'
import CrudDialog from '@/components/CrudDialog.vue'
import KpiCard from '@/components/KpiCard.vue'
import PageHeader from '@/components/PageHeader.vue'
import ProgressBar from '@/components/ProgressBar.vue'
import StatusPill from '@/components/StatusPill.vue'
import ToggleSwitch from '@/components/ToggleSwitch.vue'
import ChinaMap from '@/components/ChinaMap.vue'
import { exportCsv, stamp } from '@/utils/csv'
import { useAppStore } from '@/stores/app'
import { useSecurityStore } from '@/stores/security'
import { useConfirm } from '@/composables/useConfirm'

const router = useRouter()
const app = useAppStore()
const store = useSecurityStore()
const { confirmState, askConfirm, resolveConfirm } = useConfirm()

const data = computed(() => store.data)

onMounted(() => {
  if (!data.value) store.load(1)
  // 系统配置（参数 / 功能开关）独立于大盘数据，进入页面即顺带拉取
  if (!store.sysConfig) store.loadSysConfig()
})

const STATE_ORDER = ['GRANTED', 'PARTIAL', 'NONE']
const stateMeta = {
  GRANTED: { icon: 'fa-check', cls: 'bg-blue-50 text-electric', text: 'text-ink' },
  PARTIAL: { icon: 'fa-minus', cls: 'bg-amber-50 text-amber-500', text: 'text-ink' },
  NONE: { icon: '', cls: 'bg-slate-100 text-slate-400', text: 'text-slate-400' },
}

/** 待保存的三态授权变更 */
const pending = reactive({})

const currentState = (role, group, permission) => {
  const key = `${role.code}|${group.name}|${permission.name}`
  return key in pending ? pending[key] : permission.state
}

const cycleState = (role, group, permission) => {
  const key = `${role.code}|${group.name}|${permission.name}`
  pending[key] = STATE_ORDER[(STATE_ORDER.indexOf(currentState(role, group, permission)) + 1) % 3]
}

const pendingCount = computed(() => Object.keys(pending).length)

const savePermissions = async () => {
  const payloads = Object.entries(pending).map(([key, state]) => {
    const [roleCode, groupName, permissionName] = key.split('|')
    return { roleCode, groupName, permissionName, state }
  })
  Object.keys(pending).forEach((key) => delete pending[key])
  if (payloads.length) {
    await store.savePermissionBatch(payloads).catch(() => {})
  }
}

/* ------------------------------ 弹窗 ------------------------------ */

const DEVICE_FIELDS = [
  { key: 'fingerprint', label: '设备指纹', type: 'text', required: true, placeholder: 'DEV-1A2B-3C4D' },
  { key: 'region', label: '地域 · 运营商', type: 'text', placeholder: '北京 · 联通' },
  { key: 'ip', label: 'IP 地址', type: 'text', kind: 'ip', placeholder: '61.135.169.105' },
  { key: 'sessions', label: '会话数', type: 'text', kind: 'int', placeholder: '1286' },
  { key: 'risk', label: '风险分 (0-100)', type: 'number', min: 0, max: 100 },
  { key: 'verdict', label: '判定', type: 'select', options: ['正常', '可疑', '异常'] },
  { key: 'banned', label: '已封禁', type: 'switch' },
]

const PLAN_FIELDS = [
  { key: 'name', label: '套餐名称', type: 'text', required: true, placeholder: '会员季卡' },
  { key: 'price', label: '价格', type: 'text', kind: 'money', prefix: '¥', decimals: 1, placeholder: '88' },
  { key: 'desc', label: '售卖说明', type: 'text', placeholder: '按季度订阅 · 自动续费' },
  { key: 'quota', label: '额度说明', type: 'text', placeholder: '每日 800 次 · 含云端大模型' },
  { key: 'subscribers', label: '订阅人数', type: 'text', kind: 'int', placeholder: '12000' },
  { key: 'usage', label: '本月额度消耗', type: 'text', kind: 'percent', suffix: '%', decimals: 1, min: 0, max: 100, placeholder: '68' },
]

const ADMIN_FIELDS = [
  { key: 'name', label: '姓名', type: 'text', required: true, placeholder: 'Danny' },
  { key: 'role', label: '角色', type: 'select', options: ['超级管理员', '运营管理员', '只读审计员'] },
  { key: 'group', label: '所属权限组', type: 'select', options: ['集群运维组', '模型治理组', '安全审计组', '内容审核组', '广告运营组', '商户与结算组', '合规审计组'] },
  { key: 'phone', label: '手机号', type: 'text', kind: 'phone', maxlength: 11, placeholder: '13812342043' },
  { key: 'status', label: '状态', type: 'select', options: ['启用', '停用'] },
  { key: 'lastLogin', label: '最近登录', type: 'datetime-local' },
]

/** C 端用户调整：仅会员状态与账号状态可改，其余只读展示 */
const APP_USER_FIELDS = [
  { key: 'account', label: '账号', type: 'text', disabled: true },
  { key: 'regSource', label: '注册方式', type: 'text', disabled: true },
  { key: 'membership', label: '会员状态', type: 'select', options: ['免费体验', '会员月卡', '会员年卡', '会员过期'] },
  { key: 'registered', label: '注册时间', type: 'text', disabled: true },
  { key: 'lastActive', label: '最近活跃', type: 'text', disabled: true },
  { key: 'status', label: '账号状态', type: 'select', options: ['正常', '停用'] },
]

const dialog = reactive({ open: false, title: '', fields: [], record: {}, mode: 'device-create' })

const openDeviceCreate = () => {
  dialog.open = true
  dialog.mode = 'device-create'
  dialog.title = '新增设备台账'
  dialog.fields = DEVICE_FIELDS.map((f) => ({ ...f, disabled: false }))
  dialog.record = { fingerprint: '', region: '', ip: '', sessions: '0', risk: 50, verdict: '正常', banned: false }
}

const openDeviceEdit = (row) => {
  dialog.open = true
  dialog.mode = 'device-edit'
  dialog.title = `编辑设备 · ${row.fingerprint}`
  dialog.fields = DEVICE_FIELDS.map((f) => ({ ...f, disabled: f.key === 'fingerprint' }))
  dialog.record = { ...row }
}

const openPlanCreate = () => {
  dialog.open = true
  dialog.mode = 'plan-create'
  dialog.title = '新增会员套餐'
  dialog.fields = PLAN_FIELDS.map((f) => ({ ...f, disabled: false }))
  dialog.record = { name: '', price: '¥ 0', desc: '', quota: '', subscribers: '0', usage: '0%' }
}

const openPlanEdit = (row) => {
  dialog.open = true
  dialog.mode = 'plan-edit'
  dialog.title = `编辑套餐 · ${row.name}`
  dialog.fields = PLAN_FIELDS.map((f) => ({ ...f, disabled: f.key === 'name' }))
  dialog.record = { ...row }
}

const openAdminCreate = () => {
  dialog.open = true
  dialog.mode = 'admin-create'
  dialog.title = '新增管理员'
  dialog.fields = ADMIN_FIELDS
  dialog.record = { name: '', role: '运营管理员', group: '内容审核组', phone: '', status: '启用', lastLogin: '—' }
}

const openAdminEdit = (row) => {
  dialog.open = true
  dialog.mode = 'admin-edit'
  dialog.title = `编辑管理员 · ${row.name}`
  dialog.fields = ADMIN_FIELDS
  dialog.record = { ...row }
}

const openAppUserEdit = (row) => {
  dialog.open = true
  dialog.mode = 'appuser-edit'
  dialog.title = `调整 C 端用户 · ${row.account}`
  dialog.fields = APP_USER_FIELDS
  dialog.record = { ...row }
}

const submitDialog = async (form) => {
  const mode = dialog.mode
  dialog.open = false
  try {
    if (mode === 'device-create') await store.createDevice(form)
    else if (mode === 'device-edit') await store.updateDevice(form)
    else if (mode === 'plan-create') await store.createPlan(form)
    else if (mode === 'plan-edit') await store.updatePlan(form)
    else if (mode === 'admin-create') await store.createAdmin(form)
    else if (mode === 'admin-edit') await store.updateAdmin(form)
    else if (mode === 'appuser-edit') await store.updateUser({ ...dialog.record, ...form })
  } catch {
    /* 提示已在 store 统一处理 */
  }
}

const removeDevice = async (row) => {
  if (await askConfirm(`确定删除设备「${row.fingerprint}」？`, '删除设备台账')) {
    await store.removeDevice(row.fingerprint).catch(() => {})
  }
}

const removePlan = async (row) => {
  if (await askConfirm(`确定删除套餐「${row.name}」？`, '删除会员套餐')) {
    await store.removePlan(row.name).catch(() => {})
  }
}

const removeAdmin = async (row) => {
  if (await askConfirm(`确定删除管理员「${row.name}」？`, '删除管理员')) {
    await store.removeAdmin(row.id).catch(() => {})
  }
}

const banOne = (device) => {
  if (!device.banned) store.ban(device.fingerprint).catch(() => {})
}

/* ------------------------------ 安全策略弹窗 ------------------------------ */

const policyOpen = ref(false)
const togglePolicy = (item) => store.setPolicy(item.name, !item.enabled).catch(() => {})

/* ------------------------------ 系统配置 ------------------------------ */

/** 参数编辑本地值（name → 数字字符串），保存后回读覆盖 */
const paramValues = reactive({})
watch(
  () => store.sysConfig?.params,
  (list) => {
    if (!list) return
    list.forEach((param) => {
      if (!(param.name in paramValues)) paramValues[param.name] = param.value
    })
  },
  { immediate: true },
)

const saveParam = (param) => {
  const value = String(paramValues[param.name] ?? '').trim()
  if (value === '' || value === String(param.value)) return
  store.setSysParam(param.name, value).catch(() => {})
}

const toggleFeature = (feature) => store.setFeature(feature.name, !feature.enabled).catch(() => {})

/* ------------------------------ 列表 / 搜索 ------------------------------ */

const deviceTone = { 正常: 'green', 可疑: 'amber', 异常: 'red', 已封禁: 'slate' }
const resultTone = { 成功: 'green', 已拦截: 'red' }
const riskTone = (risk) => (risk > 85 ? 'danger' : risk > 70 ? 'warn' : 'ok')

const matchKeyword = (...values) => {
  const keyword = app.keyword.trim().toLowerCase()
  if (!keyword) return true
  return values.some((v) => String(v ?? '').toLowerCase().includes(keyword))
}

const filteredDevices = computed(() => (data.value?.devices || [])
  .filter((d) => matchKeyword(d.region, d.ip, d.fingerprint, d.verdict)))
const filteredAdmins = computed(() => (data.value?.admins || [])
  .filter((u) => matchKeyword(u.name, u.role, u.group, u.phone, u.status)))
const filteredAppUsers = computed(() => (data.value?.appUsers || [])
  .filter((u) => matchKeyword(u.account, u.regSource, u.membership, u.status)))
const filteredLogs = computed(() => (data.value?.auditLogs || [])
  .filter((l) => matchKeyword(l.time, l.operator, l.action, l.detail, l.source, l.result)))

const exportLogs = () => {
  exportCsv(`操作日志-${stamp()}`,
    ['操作时间', '操作人', '角色', '权限组', '操作类型', '操作详情', '来源 IP / 地域', '结果', '存证哈希'],
    filteredLogs.value.map((l) => [l.time, l.operator, l.role, l.group, l.action, l.detail, l.source, l.result, l.hash]))
}

const gotoModeration = () => router.push({ name: 'a8' })
</script>

<template>
  <div class="p-5">
    <template v-if="data">
      <PageHeader
        title="安全风控、设备审计与 RBAC 权限"
        desc="管理员分级授权、异常设备地理监控与不可篡改的审计日志 · 日志留存 180 天"
      >
        <template #actions>
          <button class="btn btn-ghost btn-sm" @click="policyOpen = true">
            <i class="fa-solid fa-shield-halved"></i>安全策略
          </button>
          <button class="btn btn-primary btn-sm" @click="openAdminCreate">
            <i class="fa-solid fa-user-plus"></i>新增管理员
          </button>
        </template>
      </PageHeader>

      <!-- KPI -->
      <div class="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
        <KpiCard v-for="kpi in data.kpis" :key="kpi.label" :kpi="kpi" />
      </div>

      <!-- 多级角色权限树 -->
      <div class="card anim mt-4">
        <div class="card-h">
          <div>
            <div class="card-t">多级角色权限树状分配</div>
            <div class="card-s">3 个角色域 · 6 个权限组 · 共 24 项细粒度权限（✓ 已授权 · − 部分授权 · 空 未授权）</div>
          </div>
          <StatusPill text="三态授权" tone="blue" />
        </div>
        <div class="card-b grid grid-cols-1 gap-4 lg:grid-cols-3">
          <div v-for="role in data.roleTree" :key="role.code" class="overflow-hidden rounded-xl border border-line">
            <div class="flex items-center gap-2.5 px-3 py-2.5" style="background: linear-gradient(90deg, #f0f7ff, #f8fafc)">
              <i class="fa-solid fa-chevron-down text-[9px] text-sub"></i>
              <i class="fa-solid text-[12px] text-navy" :class="role.icon"></i>
              <span class="text-[13px] font-semibold text-ink">{{ role.name }}</span>
              <span class="pill pill-blue ml-auto !text-[10.5px]">{{ role.members }} 人</span>
            </div>
            <div class="divide-y divide-[#F1F5F9]">
              <div v-for="group in role.groups" :key="group.name" class="px-3 py-2.5">
                <div class="flex items-center gap-2">
                  <i class="fa-solid fa-users-gear text-[11px] text-ice"></i>
                  <span class="text-[12.5px] font-medium text-ink">{{ group.name }}</span>
                  <span class="ml-auto text-[11px] text-sub">{{ group.members }} 人</span>
                </div>
                <div class="mt-2 space-y-1.5">
                  <div
                    v-for="permission in group.permissions"
                    :key="permission.name"
                    class="flex cursor-pointer items-center gap-1.5 text-[12px]"
                    :class="stateMeta[currentState(role, group, permission)].text"
                    title="点击切换授权状态"
                    @click="cycleState(role, group, permission)"
                  >
                    <span
                      class="grid h-4 w-4 flex-none place-items-center rounded text-[8px]"
                      :class="stateMeta[currentState(role, group, permission)].cls"
                    >
                      <i
                        v-if="stateMeta[currentState(role, group, permission)].icon"
                        class="fa-solid"
                        :class="stateMeta[currentState(role, group, permission)].icon"
                      ></i>
                    </span>
                    <span>{{ permission.name }}</span>
                    <i
                      v-if="currentState(role, group, permission) !== permission.state"
                      class="fa-solid fa-pen text-[8px] text-ice"
                    ></i>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="card-h flex-wrap !border-b-0 !border-t border-line">
          <span class="text-[11.5px] text-sub">
            <i class="fa-solid fa-circle-info mr-1 text-ice"></i>
            点击权限项可切换三态；修改后需点击「保存权限配置」生效
            <span v-if="pendingCount" class="ml-1 font-semibold text-electric">（{{ pendingCount }} 项待保存）</span>
          </span>
          <button class="btn btn-primary btn-sm" :disabled="store.acting || !pendingCount" @click="savePermissions">
            <i class="fa-solid fa-floppy-disk"></i>保存权限配置
          </button>
        </div>
      </div>

      <!-- 登录监控地图 + 设备明细 -->
      <div class="card anim mt-4">
        <div class="card-h">
          <div>
            <div class="card-t">异常设备登录监控地图</div>
            <div class="card-s">全球实时登录热力 · 气泡大小为会话数，红色为已判定异常</div>
          </div>
          <div class="flex items-center gap-3 text-[11px] text-sub">
            <span class="inline-flex items-center gap-1.5"><i class="h-2.5 w-2.5 rounded-full bg-emerald-500"></i>正常</span>
            <span class="inline-flex items-center gap-1.5"><i class="h-2.5 w-2.5 rounded-full bg-amber-500"></i>可疑</span>
            <span class="inline-flex items-center gap-1.5"><i class="h-2.5 w-2.5 rounded-full bg-rose-500"></i>异常</span>
            <button class="btn btn-primary btn-sm" @click="openDeviceCreate">
              <i class="fa-solid fa-plus"></i>新增设备
            </button>
          </div>
        </div>
        <div class="card-b">
          <ChinaMap :points="data.map.points" :height="420" />
        </div>
        <div class="overflow-x-auto">
          <table class="tb">
            <thead>
              <tr>
                <th>地域 · 运营商</th>
                <th>IP 地址</th>
                <th>设备指纹</th>
                <th>会话数</th>
                <th>风险分</th>
                <th>判定</th>
                <th class="text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="device in filteredDevices" :key="device.fingerprint">
                <td class="text-[12.5px] font-medium">{{ device.region }}</td>
                <td class="num text-[12px]">{{ device.ip }}</td>
                <td class="num text-[11.5px] text-sub">{{ device.fingerprint }}</td>
                <td class="num text-[12px]">{{ device.sessions }}</td>
                <td><ProgressBar :value="device.risk" :tone="riskTone(device.risk)" show-text /></td>
                <td><StatusPill :text="device.verdict" :tone="deviceTone[device.verdict] || 'slate'" /></td>
                <td class="text-right">
                  <button
                    class="btn btn-ghost btn-sm !px-2 !py-1"
                    title="封禁设备"
                    :disabled="device.banned || store.acting"
                    @click="banOne(device)"
                  >
                    <i class="fa-solid fa-ban" :class="device.banned ? 'text-slate-300' : 'text-rose-500'"></i>
                  </button>
                  <button class="btn btn-ghost btn-sm !px-2 !py-1" title="编辑" @click="openDeviceEdit(device)">
                    <i class="fa-solid fa-pen"></i>
                  </button>
                  <button class="btn btn-ghost btn-sm !px-2 !py-1" title="删除" @click="removeDevice(device)">
                    <i class="fa-solid fa-trash text-rose-500"></i>
                  </button>
                </td>
              </tr>
              <tr v-if="!filteredDevices.length">
                <td colspan="7" class="py-8 text-center text-[12px] text-sub">没有匹配的设备</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 会员套餐与额度 -->
      <div class="card anim mt-4">
        <div class="card-h">
          <div>
            <div class="card-t">会员套餐与翻译额度配置</div>
            <div class="card-s">额度按自然月重置，超额自动降级为按次计费 · 商户与结算组可编辑</div>
          </div>
          <div class="flex items-center gap-1.5">
            <button class="btn btn-ghost btn-sm" @click="gotoModeration">
              <i class="fa-solid fa-gavel"></i>退款仲裁工单
            </button>
            <button class="btn btn-primary btn-sm" @click="openPlanCreate">
              <i class="fa-solid fa-plus"></i>新增套餐
            </button>
          </div>
        </div>
        <div class="card-b grid grid-cols-1 gap-4 md:grid-cols-3">
          <div v-for="plan in data.plans" :key="plan.name" class="overflow-hidden rounded-xl border border-line">
            <div class="px-3.5 py-3" style="background: linear-gradient(135deg, #f0f7ff, #f8fafc)">
              <div class="flex items-center justify-between">
                <span class="text-[13.5px] font-semibold text-ink">{{ plan.name }}</span>
                <span class="num text-[16px] font-bold text-navy">{{ plan.price }}</span>
              </div>
              <div class="mt-0.5 text-[11.5px] text-sub">{{ plan.desc }}</div>
            </div>
            <div class="px-3.5 py-3">
              <div class="text-[12px]">{{ plan.quota }}</div>
              <div class="mt-2.5">
                <div class="mb-1 flex items-center justify-between text-[11.5px]">
                  <span class="text-sub">本月额度消耗</span>
                  <span class="num font-semibold" :class="parseInt(plan.usage) >= 85 ? 'text-amber-600' : 'text-navy'">
                    {{ plan.usage }}
                  </span>
                </div>
                <ProgressBar :value="parseFloat(plan.usage)" :tone="parseInt(plan.usage) >= 85 ? 'warn' : ''" width="w-full" />
              </div>
              <div class="mt-2.5 flex items-center justify-between">
                <span class="text-[11.5px] text-sub">订阅人数 <b class="num text-ink">{{ plan.subscribers }}</b></span>
                <div class="flex gap-1.5">
                  <button class="btn btn-ghost btn-sm !px-2 !py-1" title="编辑" @click="openPlanEdit(plan)">
                    <i class="fa-solid fa-pen"></i>
                  </button>
                  <button class="btn btn-primary btn-sm !px-2 !py-1" title="调整额度" @click="openPlanEdit(plan)">
                    <i class="fa-solid fa-sliders"></i>额度
                  </button>
                  <button class="btn btn-ghost btn-sm !px-2 !py-1" title="删除" @click="removePlan(plan)">
                    <i class="fa-solid fa-trash text-rose-500"></i>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 管理员账号 -->
      <div class="card anim mt-4">
        <div class="card-h">
          <div>
            <div class="card-t">管理员账号</div>
            <div class="card-s">支持新增 / 编辑 / 停用管理员，账号数量与上方 KPI 实时联动</div>
          </div>
          <StatusPill :text="`${filteredAdmins.length} 个账号`" tone="blue" />
        </div>
        <div class="overflow-x-auto">
          <table class="tb">
            <thead>
              <tr>
                <th>姓名</th>
                <th>角色</th>
                <th>所属权限组</th>
                <th>手机号</th>
                <th>状态</th>
                <th>最近登录</th>
                <th class="text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="admin in filteredAdmins" :key="admin.id">
                <td class="text-[12.5px] font-medium">
                  <span
                    class="mr-2 inline-grid h-6 w-6 place-items-center rounded-lg text-[9.5px] font-bold text-white"
                    style="background: linear-gradient(135deg, #1E3A8A, #2563EB)"
                    >{{ admin.name.slice(0, 1) }}</span
                  >{{ admin.name }}
                </td>
                <td><StatusPill :text="admin.role" :tone="admin.role === '超级管理员' ? 'blue' : admin.role === '只读审计员' ? 'slate' : 'amber'" /></td>
                <td class="text-[12px]">{{ admin.group }}</td>
                <td class="num text-[12px]">{{ admin.phone }}</td>
                <td><StatusPill :text="admin.status" :tone="admin.status === '启用' ? 'green' : 'slate'" /></td>
                <td class="num text-[11.5px] text-sub">{{ admin.lastLogin }}</td>
                <td class="text-right">
                  <button class="btn btn-ghost btn-sm !px-2 !py-1" title="编辑" @click="openAdminEdit(admin)">
                    <i class="fa-solid fa-pen"></i>
                  </button>
                  <button class="btn btn-ghost btn-sm !px-2 !py-1" title="删除" @click="removeAdmin(admin)">
                    <i class="fa-solid fa-trash text-rose-500"></i>
                  </button>
                </td>
              </tr>
              <tr v-if="!filteredAdmins.length">
                <td colspan="7" class="py-8 text-center text-[12px] text-sub">没有匹配的管理员</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- C 端用户管理 -->
      <div class="card anim mt-4">
        <div class="card-h">
          <div>
            <div class="card-t">C 端用户管理</div>
            <div class="card-s">会员状态与账号状态在线调整 · 注册来源与活跃度一览</div>
          </div>
          <StatusPill :text="`${filteredAppUsers.length} 个用户`" tone="blue" />
        </div>
        <div class="overflow-x-auto">
          <table class="tb">
            <thead>
              <tr>
                <th>账号</th>
                <th>注册方式</th>
                <th>会员状态</th>
                <th>注册时间</th>
                <th>最近活跃</th>
                <th>状态</th>
                <th class="text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="user in filteredAppUsers" :key="user.id">
                <td class="text-[12.5px] font-medium">{{ user.account }}</td>
                <td class="text-[12px]">{{ user.regSource }}</td>
                <td>
                  <StatusPill
                    :text="user.membership"
                    :tone="user.membership === '会员过期' ? 'slate' : 'blue'"
                  />
                </td>
                <td class="num text-[11.5px] text-sub">{{ user.registered }}</td>
                <td class="num text-[11.5px] text-sub">{{ user.lastActive }}</td>
                <td><StatusPill :text="user.status" :tone="user.status === '正常' ? 'green' : 'slate'" /></td>
                <td class="text-right">
                  <button class="btn btn-ghost btn-sm !px-2 !py-1" title="调整" @click="openAppUserEdit(user)">
                    <i class="fa-solid fa-sliders"></i>调整
                  </button>
                </td>
              </tr>
              <tr v-if="!filteredAppUsers.length">
                <td colspan="7" class="py-8 text-center text-[12px] text-sub">没有匹配的用户</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 系统配置 -->
      <div class="card anim mt-4">
        <div class="card-h">
          <div>
            <div class="card-t">系统参数与功能开关</div>
            <div class="card-s">运行参数在线调优 · 功能开关保存后即时生效</div>
          </div>
          <StatusPill text="热更新" tone="amber" />
        </div>
        <div class="card-b grid grid-cols-1 gap-4 lg:grid-cols-2">
          <!-- 运行参数 -->
          <div class="space-y-2">
            <div
              v-for="param in store.sysConfig?.params || []"
              :key="param.name"
              class="flex items-center gap-2 rounded-lg border border-line px-3 py-2"
            >
              <span class="text-[12.5px] text-ink">{{ param.label }}</span>
              <input
                v-model="paramValues[param.name]"
                type="number"
                min="0"
                class="field num ml-auto !w-24 !py-1 text-right text-[12px]"
                @blur="saveParam(param)"
              />
              <span v-if="param.unit" class="w-8 text-[11px] text-sub">{{ param.unit }}</span>
              <button
                class="btn btn-primary btn-sm !px-2 !py-1"
                :disabled="store.acting || String(paramValues[param.name] ?? '').trim() === String(param.value)"
                @click="saveParam(param)"
              >
                保存
              </button>
            </div>
            <div v-if="!store.sysConfig?.params?.length" class="py-4 text-center text-[12px] text-sub">
              参数加载中…
            </div>
          </div>
          <!-- 功能开关 -->
          <div class="space-y-2">
            <div
              v-for="feature in store.sysConfig?.features || []"
              :key="feature.name"
              class="flex items-center justify-between rounded-lg border border-line px-3 py-2 text-[12.5px]"
            >
              <span class="text-ink">{{ feature.name }}</span>
              <ToggleSwitch
                :model-value="feature.enabled"
                :disabled="store.acting"
                @update:model-value="() => toggleFeature(feature)"
              />
            </div>
            <div v-if="!store.sysConfig?.features?.length" class="py-4 text-center text-[12px] text-sub">
              开关加载中…
            </div>
          </div>
        </div>
      </div>

      <!-- 操作日志 -->
      <div class="card anim mt-4">
        <div class="card-h">
          <div>
            <div class="card-t">管理员操作日志（{{ data.page.retentionDays }} 天不可篡改）</div>
            <div class="card-s">追加写入 + 区块链哈希链式存证，任何管理员（含超管）均无法修改或删除</div>
          </div>
          <div class="flex items-center gap-2">
            <StatusPill text="哈希链已校验" tone="green" />
            <button class="btn btn-ghost btn-sm" @click="exportLogs">
              <i class="fa-solid fa-file-export"></i>导出存证
            </button>
          </div>
        </div>
        <div class="overflow-x-auto">
          <table class="tb">
            <thead>
              <tr>
                <th>操作时间</th>
                <th>操作人</th>
                <th>操作类型</th>
                <th>操作详情</th>
                <th>来源 IP / 地域</th>
                <th>结果</th>
                <th>存证哈希</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="log in filteredLogs" :key="log.id">
                <td class="num text-[12.5px]">{{ log.time }}</td>
                <td>
                  <div class="flex items-center gap-2">
                    <span
                      class="grid h-6 w-6 flex-none place-items-center rounded-lg text-[9.5px] font-bold text-white"
                      style="background: linear-gradient(135deg, #1E3A8A, #2563EB)"
                      >{{ log.operator.slice(0, 1) }}</span
                    >
                    <div class="leading-tight">
                      <div class="text-[12.5px] font-medium">{{ log.operator }}</div>
                      <div class="text-[10px] text-sub">{{ log.role }} · {{ log.group }}</div>
                    </div>
                  </div>
                </td>
                <td><StatusPill :text="log.action" tone="slate" /></td>
                <td class="text-[12.5px]">{{ log.detail }}</td>
                <td class="num text-[11.5px] text-sub">{{ log.source }}</td>
                <td><StatusPill :text="log.result" :tone="resultTone[log.result] || 'amber'" /></td>
                <td class="num text-[11px] text-sub">
                  <i class="fa-solid fa-link mr-1 text-[9px] text-ice"></i>{{ log.hash }}
                </td>
              </tr>
              <tr v-if="!filteredLogs.length">
                <td colspan="7" class="py-8 text-center text-[12px] text-sub">没有匹配的日志</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="card-h !border-b-0 !border-t border-line">
          <span class="text-[11.5px] text-sub">
            {{ data.page.rangeText }} · 留存期 {{ data.page.retentionDays }} 天
          </span>
          <div class="flex items-center gap-1">
            <button class="btn btn-ghost btn-sm" :disabled="data.page.page <= 1" @click="store.load(data.page.page - 1)">
              上一页
            </button>
            <span class="num px-2 text-[12px] font-semibold text-navy">
              {{ data.page.page }} / {{ data.page.totalPages }}
            </span>
            <button
              class="btn btn-ghost btn-sm"
              :disabled="data.page.page >= data.page.totalPages"
              @click="store.load(data.page.page + 1)"
            >
              下一页
            </button>
          </div>
        </div>
      </div>
    </template>

    <div v-else class="grid h-64 place-items-center text-[13px] text-sub">
      <i class="fa-solid fa-circle-notch fa-spin mr-2"></i>加载安全风控数据…
    </div>

    <!-- 安全策略弹窗 -->
    <div
      v-if="policyOpen"
      class="fixed inset-0 z-[70] grid place-items-center bg-ink/30 p-4 backdrop-blur-sm"
      @click.self="policyOpen = false"
    >
      <div class="w-full max-w-md rounded-2xl bg-white p-5 shadow-lift">
        <div class="flex items-center justify-between">
          <div class="text-[14.5px] font-semibold text-ink">安全策略</div>
          <button class="text-sub transition hover:text-ink" @click="policyOpen = false">
            <i class="fa-solid fa-xmark"></i>
          </button>
        </div>
        <div class="mt-3 space-y-1.5">
          <div
            v-for="policy in data?.policies || []"
            :key="policy.name"
            class="flex items-center justify-between rounded-lg border border-line px-3 py-2 text-[12.5px]"
          >
            <span class="text-ink">{{ policy.name }}</span>
            <ToggleSwitch
              :model-value="policy.enabled"
              :disabled="store.acting"
              @update:model-value="() => togglePolicy(policy)"
            />
          </div>
        </div>
        <div class="mt-4 flex justify-end">
          <button class="btn btn-primary btn-sm" @click="policyOpen = false">完成</button>
        </div>
      </div>
    </div>

    <CrudDialog
      :open="dialog.open"
      :title="dialog.title"
      :fields="dialog.fields"
      :model-value="dialog.record"
      :loading="store.acting"
      @close="dialog.open = false"
      @submit="submitDialog"
    />
    <ConfirmDialog v-bind="confirmState" @resolve="resolveConfirm" />
  </div>
</template>
