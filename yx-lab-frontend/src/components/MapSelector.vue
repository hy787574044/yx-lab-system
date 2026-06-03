<template>
  <TiandituPointSelector :model-value="normalizedValue" @update:model-value="emitLegacyValue" />
</template>

<script setup>
import { computed } from 'vue'
import TiandituPointSelector from './TiandituPointSelector.vue'

const props = defineProps({
  modelValue: {
    type: Object,
    default: () => ({ address: '', latitude: '', longitude: '' })
  }
})

const emit = defineEmits(['update:modelValue'])

const normalizedValue = computed(() => ({
  pointName: props.modelValue.pointName || props.modelValue.address || '',
  address: props.modelValue.address || '',
  latitude: props.modelValue.latitude || '',
  longitude: props.modelValue.longitude || ''
}))

function emitLegacyValue(value) {
  emit('update:modelValue', {
    address: value.address || value.pointName || '',
    latitude: value.latitude || '',
    longitude: value.longitude || ''
  })
}
</script>
