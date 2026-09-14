/**
 * 生成浏览器扩展所需的 PNG 图标（16 / 48 / 128）。
 *
 * 纯 Node 实现（仅用内置 zlib），不引入任何第三方依赖：
 * 品牌冰蓝→青色的圆角方块底 + 白色 V 形标识（呼应 VisionTrans），
 * 每次 build:ext 前自动重新生成到 extension/icons/。
 */
import zlib from 'node:zlib'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const OUT_DIR = fileURLToPath(new URL('../extension/icons', import.meta.url))
const SIZES = [16, 48, 128]

/* ---------------- PNG 编码（RGBA → 真彩色带透明通道） ---------------- */

const CRC_TABLE = (() => {
  const table = new Uint32Array(256)
  for (let n = 0; n < 256; n++) {
    let c = n
    for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1
    table[n] = c >>> 0
  }
  return table
})()

function crc32(buf) {
  let c = 0xffffffff
  for (const b of buf) c = CRC_TABLE[(c ^ b) & 0xff] ^ (c >>> 8)
  return (c ^ 0xffffffff) >>> 0
}

function chunk(type, data) {
  const len = Buffer.alloc(4)
  len.writeUInt32BE(data.length, 0)
  const typeBuf = Buffer.from(type, 'ascii')
  const crc = Buffer.alloc(4)
  crc.writeUInt32BE(crc32(Buffer.concat([typeBuf, data])), 0)
  return Buffer.concat([len, typeBuf, data, crc])
}

function encodePng(width, height, rgba) {
  const ihdr = Buffer.alloc(13)
  ihdr.writeUInt32BE(width, 0)
  ihdr.writeUInt32BE(height, 4)
  ihdr[8] = 8 // bit depth
  ihdr[9] = 6 // color type RGBA
  const stride = width * 4
  const raw = Buffer.alloc((stride + 1) * height)
  for (let y = 0; y < height; y++) {
    const rowStart = y * (stride + 1)
    raw[rowStart] = 0 // filter: none
    rgba.copy(raw, rowStart + 1, y * stride, (y + 1) * stride)
  }
  return Buffer.concat([
    Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
    chunk('IHDR', ihdr),
    chunk('IDAT', zlib.deflateSync(raw, { level: 9 })),
    chunk('IEND', Buffer.alloc(0)),
  ])
}

/* ---------------- 图案绘制 ---------------- */

function distToSegment(px, py, ax, ay, bx, by) {
  const dx = bx - ax
  const dy = by - ay
  const len2 = dx * dx + dy * dy
  let t = len2 === 0 ? 0 : ((px - ax) * dx + (py - ay) * dy) / len2
  t = Math.max(0, Math.min(1, t))
  const cx = ax + t * dx
  const cy = ay + t * dy
  return Math.hypot(px - cx, py - cy)
}

function insideRoundedRect(x, y, n, r) {
  if (x < 0 || y < 0 || x >= n || y >= n) return false
  const cx = Math.min(Math.max(x, r), n - 1 - r)
  const cy = Math.min(Math.max(y, r), n - 1 - r)
  return Math.hypot(x - cx, y - cy) <= r
}

/** 白色 V 形折线：左上 → 中下 → 右上，到折线的距离小于描边半宽即为标识区域。 */
function insideVee(x, y, n) {
  const half = n * 0.085
  const p1 = [n * 0.30, n * 0.34]
  const p2 = [n * 0.50, n * 0.70]
  const p3 = [n * 0.70, n * 0.34]
  return (
    Math.min(distToSegment(x, y, ...p1, ...p2), distToSegment(x, y, ...p2, ...p3)) <= half
  )
}

function render(size) {
  const n = size
  const buf = Buffer.alloc(n * n * 4)
  const radius = n * 0.22
  // 品牌渐变：#0d9488（teal-600）→ #06b6d4（cyan-500），自上而下
  const top = [0x0d, 0x94, 0x88]
  const bottom = [0x06, 0xb6, 0xd4]
  for (let y = 0; y < n; y++) {
    for (let x = 0; x < n; x++) {
      const i = (y * n + x) * 4
      if (!insideRoundedRect(x + 0.5, y + 0.5, n, radius)) {
        buf[i + 3] = 0
        continue
      }
      const t = y / (n - 1)
      let r = Math.round(top[0] + (bottom[0] - top[0]) * t)
      let g = Math.round(top[1] + (bottom[1] - top[1]) * t)
      let b = Math.round(top[2] + (bottom[2] - top[2]) * t)
      let a = 255
      if (insideVee(x + 0.5, y + 0.5, n)) {
        r = 0xff
        g = 0xff
        b = 0xff
      }
      buf[i] = r
      buf[i + 1] = g
      buf[i + 2] = b
      buf[i + 3] = a
    }
  }
  return encodePng(n, n, buf)
}

fs.mkdirSync(OUT_DIR, { recursive: true })
for (const size of SIZES) {
  const file = path.join(OUT_DIR, `icon${size}.png`)
  fs.writeFileSync(file, render(size))
  console.log(`generated ${path.relative(process.cwd(), file)}`)
}
