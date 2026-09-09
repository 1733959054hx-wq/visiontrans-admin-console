/**
 * 纯前端 CSV 导出工具（不依赖后端，适合列表"导出"按钮）。
 *
 * @param {string} filename 文件名（不含扩展名）
 * @param {string[]} headers 表头
 * @param {Array<Array<string|number>>} rows 数据行
 */
export function exportCsv(filename, headers, rows) {
  // 危险前缀（= + - @ Tab CR）会被 Excel / WPS 当作公式执行（CSV 公式注入 · CWE-1236），
  // 导出的 UGC 内容、用户名等字段由外部用户可控，统一前置单引号中和
  const RISKY = /^[=+\-@\t\r]/
  const escape = (value) => {
    const raw = value === null || value === undefined ? '' : String(value)
    const text = RISKY.test(raw) ? `'${raw}` : raw
    return /[",\n\r]/.test(text) ? `"${text.replace(/"/g, '""')}"` : text
  }
  // BOM 保证 Excel 打开中文不乱码
  const bom = '\uFEFF'
  const csv = [headers, ...rows].map((row) => row.map(escape).join(',')).join('\r\n')
  const blob = new Blob([bom + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `${filename}.csv`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

/** 当前时间戳，用于导出文件名。 */
export function stamp() {
  const d = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}${pad(d.getMonth() + 1)}${pad(d.getDate())}-${pad(d.getHours())}${pad(d.getMinutes())}`
}
