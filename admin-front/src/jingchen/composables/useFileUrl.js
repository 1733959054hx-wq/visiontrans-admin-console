import { fetchFileBlob } from '@/jingchen/api/merchant'

/**
 * 鉴权文件取回（jingchen 模块）。
 *
 * 上传接口返回的 /merchant/files/... 地址需要携带 X-Auth-Token，
 * <img>/<video> 标签无法直接引用，这里统一取回 blob 转 objectURL；
 * 同一地址进程内缓存，页面生命周期内复用不重复请求。
 */
const cache = new Map()

/** 把后端文件地址解析为可直接用于媒体标签的 objectURL（失败返回空串） */
export async function fileObjectUrl(url) {
  if (!url) return ''
  if (cache.has(url)) return cache.get(url)
  try {
    const blob = await fetchFileBlob(url)
    const objectUrl = URL.createObjectURL(blob)
    cache.set(url, objectUrl)
    return objectUrl
  } catch {
    return ''
  }
}

/** 触发浏览器下载（鉴权 blob → 临时 <a>），name 为保存文件名 */
export async function downloadFile(url, name) {
  const objectUrl = await fileObjectUrl(url)
  if (!objectUrl) return
  const a = document.createElement('a')
  a.href = objectUrl
  a.download = name || 'merchant-file'
  document.body.appendChild(a)
  a.click()
  a.remove()
}

/** 按扩展名判断是否可直接内联预览 */
export function isPreviewable(ext) {
  return ['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp', 'mp4', 'webm', 'mov', 'srt', 'vtt'].includes(
    String(ext || '').toLowerCase(),
  )
}
