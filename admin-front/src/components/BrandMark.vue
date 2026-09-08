<script setup>
/**
 * 品牌标识：几何化猫头鹰「译译」。
 *
 * 单 path + evenodd 嵌套：身体(实) → 眼窝(镂空) → 瞳孔(实) → 高光(镂空) → 喙(镂空)，
 * 镂空处透出底色，放在品牌蓝渐变徽章上自动呈现渐变眼窝与高光，无需多套图。
 * fill 用 currentColor，颜色由父级文字色决定。纯矢量、无外部资源。
 */
defineOptions({ name: 'BrandMark' })
</script>

<template>
  <svg
    viewBox="0 0 48 48"
    xmlns="http://www.w3.org/2000/svg"
    role="img"
    aria-label="视界译 VisionTrans"
  >
    <path
      fill="currentColor"
      fill-rule="evenodd"
      clip-rule="evenodd"
      d="M10 8 L12.5 14.5 C7.5 17 6 23 6 28 C6 36 14 42 24 42
         C34 42 42 36 42 28 C42 23 40.5 17 35.5 14.5 L38 8 L31 12.5
         C29 11.6 26.5 11 24 11 C21.5 11 19 11.6 17 12.5 Z
         M11 24 a6 6 0 1 0 12 0 a6 6 0 1 0 -12 0 Z
         M25 24 a6 6 0 1 0 12 0 a6 6 0 1 0 -12 0 Z
         M14.5 24 a2.5 2.5 0 1 0 5 0 a2.5 2.5 0 1 0 -5 0 Z
         M28.5 24 a2.5 2.5 0 1 0 5 0 a2.5 2.5 0 1 0 -5 0 Z
         M15.1 22.8 a0.9 0.9 0 1 0 1.8 0 a0.9 0.9 0 1 0 -1.8 0 Z
         M29.1 22.8 a0.9 0.9 0 1 0 1.8 0 a0.9 0.9 0 1 0 -1.8 0 Z
         M21.5 31 L26.5 31 L24 36 Z"
    />
  </svg>
</template>
