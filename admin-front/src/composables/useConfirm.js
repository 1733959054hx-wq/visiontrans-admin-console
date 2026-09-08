import { ref } from 'vue'

/**
 * 轻量确认框状态管理：页面调用 askConfirm(message) 拿到 Promise<boolean>。
 *
 * 用法：
 *   const { confirmState, askConfirm, resolveConfirm } = useConfirm()
 *   <ConfirmDialog v-bind="confirmState" @resolve="resolveConfirm" />
 */
export function useConfirm() {
  const confirmState = ref({ open: false, title: '操作确认', message: '' })
  let resolver = null

  const askConfirm = (message, title = '操作确认') => {
    confirmState.value = { open: true, title, message }
    return new Promise((resolve) => {
      resolver = resolve
    })
  }

  const resolveConfirm = (confirmed) => {
    confirmState.value = { ...confirmState.value, open: false }
    if (resolver) {
      resolver(confirmed)
      resolver = null
    }
  }

  return { confirmState, askConfirm, resolveConfirm }
}
