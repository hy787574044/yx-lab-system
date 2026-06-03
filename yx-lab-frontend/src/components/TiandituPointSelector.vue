<template>
  <div class="tdt-selector">
    <div v-if="!readonly" class="tdt-selector__search">
      <el-input v-model="keyword" clearable placeholder="搜索点位、地址或直接在地图上点击选点" @keyup.enter="searchKeyword" />
      <el-button type="primary" :loading="searching" @click="searchKeyword">搜索</el-button>
    </div>
    <div :id="mapId" ref="mapContainer" class="tdt-selector__map"></div>
    <div class="tdt-selector__info">
      <span>位置：{{ selectedAddress || '-' }}</span>
      <span>经度：{{ selectedLongitude || '-' }}</span>
      <span>纬度：{{ selectedLatitude || '-' }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElButton } from 'element-plus/es/components/button/index.mjs'
import { ElInput } from 'element-plus/es/components/input/index.mjs'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'

const DEFAULT_CENTER = { longitude: '115.2153', latitude: '29.8305' }
let tiandituLoader = null

const props = defineProps({
  modelValue: {
    type: Object,
    default: () => ({ pointName: '', address: '', latitude: '', longitude: '' })
  },
  readonly: {
    type: Boolean,
    default: false
  },
  zoom: {
    type: Number,
    default: 15
  }
})

const emit = defineEmits(['update:modelValue', 'picked'])

const mapId = `tdt-map-${Math.random().toString(36).slice(2)}`
const mapContainer = ref(null)
const keyword = ref('')
const searching = ref(false)
const selectedAddress = ref('')
const selectedLatitude = ref('')
const selectedLongitude = ref('')

let map = null
let marker = null

const token = computed(() => String(import.meta.env.VITE_TIANDITU_TK || '').trim())

watch(() => props.modelValue, (value) => {
  selectedAddress.value = value?.address || value?.pointName || ''
  selectedLatitude.value = value?.latitude || ''
  selectedLongitude.value = value?.longitude || ''
  syncMarkerFromValue(false)
}, { deep: true, immediate: true })

onMounted(async () => {
  await nextTick()
  await initMap()
})

onBeforeUnmount(() => {
  if (map?.clearOverLays) {
    map.clearOverLays()
  }
  map = null
  marker = null
})

async function initMap() {
  if (!token.value) {
    ElMessage.warning('请先配置天地图密钥 VITE_TIANDITU_TK')
    return
  }
  try {
    await loadTianditu(token.value)
    if (!window.T || !mapContainer.value) {
      return
    }
    map = new window.T.Map(mapId)
    const center = buildLngLat(
      selectedLongitude.value || DEFAULT_CENTER.longitude,
      selectedLatitude.value || DEFAULT_CENTER.latitude
    )
    map.centerAndZoom(center, props.zoom)
    if (map.enableScrollWheelZoom) {
      map.enableScrollWheelZoom()
    }
    if (!props.readonly) {
      map.addEventListener('click', handleMapClick)
    }
    syncMarkerFromValue(true)
  } catch (error) {
    console.error('Tianditu load failed:', error)
    ElMessage.error('天地图加载失败，请检查网络或密钥配置')
  }
}

function loadTianditu(tk) {
  if (window.T?.Map) {
    return Promise.resolve()
  }
  if (tiandituLoader) {
    return tiandituLoader
  }
  tiandituLoader = new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = `https://api.tianditu.gov.cn/api?v=4.0&tk=${encodeURIComponent(tk)}`
    script.async = true
    script.onload = () => resolve()
    script.onerror = reject
    document.head.appendChild(script)
  })
  return tiandituLoader
}

function buildLngLat(longitude, latitude) {
  return new window.T.LngLat(Number(longitude), Number(latitude))
}

function syncMarkerFromValue(recenter) {
  if (!map || !selectedLongitude.value || !selectedLatitude.value || !window.T) {
    return
  }
  const point = buildLngLat(selectedLongitude.value, selectedLatitude.value)
  setMarker(point)
  if (recenter && map.centerAndZoom) {
    map.centerAndZoom(point, props.zoom)
  }
}

function setMarker(point) {
  if (!map || !window.T) {
    return
  }
  if (marker && map.removeOverLay) {
    map.removeOverLay(marker)
  }
  marker = new window.T.Marker(point)
  map.addOverLay(marker)
}

async function handleMapClick(event) {
  const point = event.lnglat || event.lngLat || event.lngLatLng
  if (!point) {
    return
  }
  const longitude = String(point.getLng ? point.getLng() : point.lng)
  const latitude = String(point.getLat ? point.getLat() : point.lat)
  const address = await reverseGeocode(point)
  updateSelection({
    pointName: address,
    address,
    latitude,
    longitude
  })
  setMarker(point)
}

async function reverseGeocode(point) {
  if (!window.T?.Geocoder) {
    return ''
  }
  return new Promise((resolve) => {
    try {
      const geocoder = new window.T.Geocoder()
      geocoder.getLocation(point, (result) => {
        resolve(extractAddress(result))
      })
    } catch {
      resolve('')
    }
  })
}

function extractAddress(result) {
  if (!result) {
    return ''
  }
  if (typeof result.getAddress === 'function') {
    return result.getAddress() || ''
  }
  return result.formatted_address
    || result.formattedAddress
    || result.address
    || result.result?.formatted_address
    || result.result?.formattedAddress
    || result.result?.addressComponent?.address
    || ''
}

async function searchKeyword() {
  const text = keyword.value.trim()
  if (!text || !token.value) {
    return
  }
  searching.value = true
  try {
    const url = `https://api.tianditu.gov.cn/geocoder?ds=${encodeURIComponent(JSON.stringify({ keyWord: text }))}&tk=${encodeURIComponent(token.value)}`
    const response = await fetch(url)
    const payload = await response.json()
    const location = extractSearchLocation(payload)
    if (!location) {
      ElMessage.warning('未找到匹配点位')
      return
    }
    const point = buildLngLat(location.longitude, location.latitude)
    const address = location.address || text
    updateSelection({
      pointName: address,
      address,
      latitude: String(location.latitude),
      longitude: String(location.longitude)
    })
    setMarker(point)
    map.centerAndZoom(point, props.zoom)
  } catch (error) {
    console.error('Tianditu search failed:', error)
    ElMessage.error('天地图搜索失败')
  } finally {
    searching.value = false
  }
}

function extractSearchLocation(payload) {
  const result = payload?.result || payload
  const location = result?.location || result?.lonlat || result?.position
  if (location) {
    const longitude = location.lon || location.lng || location.longitude
    const latitude = location.lat || location.latitude
    if (longitude && latitude) {
      return { longitude, latitude, address: result.formatted_address || result.formattedAddress || result.address }
    }
  }
  const firstPoi = Array.isArray(result?.pois) ? result.pois[0] : null
  if (firstPoi?.lonlat) {
    const [longitude, latitude] = String(firstPoi.lonlat).split(',')
    if (longitude && latitude) {
      return { longitude, latitude, address: firstPoi.name || firstPoi.address }
    }
  }
  return null
}

function updateSelection(nextValue) {
  selectedAddress.value = nextValue.address || ''
  selectedLatitude.value = nextValue.latitude || ''
  selectedLongitude.value = nextValue.longitude || ''
  emit('update:modelValue', nextValue)
  emit('picked', nextValue)
}
</script>

<style scoped>
.tdt-selector {
  display: grid;
  gap: 10px;
}

.tdt-selector__search {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
}

.tdt-selector__map {
  height: 420px;
  min-height: 320px;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  overflow: hidden;
  background: var(--bg-panel-soft);
}

.tdt-selector__info {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  color: var(--text-sub);
  font-size: 13px;
}
</style>
