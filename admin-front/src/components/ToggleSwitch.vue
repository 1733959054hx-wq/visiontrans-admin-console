<script setup>
defineProps({
  modelValue: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue'])
</script>

<template>
  <span
    class="sw"
    :class="{ on: modelValue }"
    @click="!disabled && emit('update:modelValue', !modelValue)"
  ></span>
</template>
