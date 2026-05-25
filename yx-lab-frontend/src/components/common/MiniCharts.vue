<template>
  <div class="mini-chart" :class="`mini-chart--${type}`">
    <template v-if="type === 'pie'">
      <div class="pie-wrap">
        <div class="pie-visual" :style="{ background: pieBackground }">
          <span>{{ total }}</span>
        </div>
        <div class="chart-legend">
          <div v-for="item in normalizedItems" :key="item.label" class="legend-row">
            <i :style="{ background: item.color }"></i>
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </div>
        </div>
      </div>
    </template>

    <template v-else-if="type === 'line'">
      <svg class="line-svg" viewBox="0 0 320 150" preserveAspectRatio="none" role="img">
        <defs>
          <linearGradient :id="gradientId" x1="0" x2="0" y1="0" y2="1">
            <stop offset="0%" :stop-color="lineColor" stop-opacity="0.24" />
            <stop offset="100%" :stop-color="lineColor" stop-opacity="0" />
          </linearGradient>
        </defs>
        <path :d="areaPath" :fill="`url(#${gradientId})`" />
        <path :d="linePath" fill="none" :stroke="lineColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" />
        <g
          v-for="(point, index) in linePoints"
          :key="`${point.x}-${point.y}`"
          class="line-point"
          @mouseenter="hoverIndex = index"
          @mouseleave="hoverIndex = null"
        >
          <circle :cx="point.x" :cy="point.y" r="10" fill="transparent" />
          <circle :cx="point.x" :cy="point.y" r="4.5" :fill="lineColor" />
        </g>
      </svg>
      <div class="axis-labels">
        <span v-for="item in normalizedItems" :key="item.label">{{ item.label }}</span>
      </div>
      <div
        v-if="hoveredLineItem"
        class="line-tooltip"
        :style="{ left: `${hoveredTooltipLeft}%` }"
      >
        <strong>{{ hoveredLineItem.label }}</strong>
        <span>合格率：{{ hoveredLineItem.value }}%</span>
        <span v-if="hasCountDetail(hoveredLineItem)">合格数量：{{ hoveredLineItem.normalCount }}</span>
        <span v-if="hasCountDetail(hoveredLineItem)">不合格数量：{{ hoveredLineItem.abnormalCount }}</span>
      </div>
    </template>

    <template v-else>
      <div class="bar-stack">
        <div v-for="item in normalizedItems" :key="item.label" class="bar-row">
          <div class="bar-meta">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </div>
          <div class="bar-track">
            <i :style="{ width: `${item.percent}%`, background: item.color }"></i>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  type: {
    type: String,
    default: 'bar'
  },
  items: {
    type: Array,
    default: () => []
  },
  colors: {
    type: Array,
    default: () => ['#1677ff', '#16a34a', '#d97706', '#dc2626', '#0f766e', '#7c3aed']
  }
})

const gradientId = `mini-line-${Math.random().toString(36).slice(2)}`
const hoverIndex = ref(null)
const lineColor = computed(() => props.colors[0] || '#1677ff')
const normalizedItems = computed(() => {
  const items = (props.items || []).map((item, index) => ({
    label: item.label || item.name || '-',
    value: toSafeNumber(item.value),
    normalCount: toSafeNumber(item.normalCount),
    abnormalCount: toSafeNumber(item.abnormalCount),
    color: item.color || props.colors[index % props.colors.length]
  }))
  const max = Math.max(...items.map((item) => item.value), 0)
  return items.map((item) => ({
    ...item,
    percent: max ? Math.round((item.value / max) * 100) : 0
  }))
})
const total = computed(() => normalizedItems.value.reduce((sum, item) => sum + item.value, 0))
const pieBackground = computed(() => {
  if (!total.value) {
    return 'conic-gradient(#e8edf5 0deg 360deg)'
  }
  let start = 0
  const segments = normalizedItems.value.map((item) => {
    const end = start + (item.value / total.value) * 360
    const segment = `${item.color} ${start}deg ${end}deg`
    start = end
    return segment
  })
  return `conic-gradient(${segments.join(', ')})`
})
const linePoints = computed(() => {
  const items = normalizedItems.value
  if (!items.length) {
    return []
  }
  const max = Math.max(...items.map((item) => item.value), 1)
  const step = items.length > 1 ? 280 / (items.length - 1) : 0
  return items.map((item, index) => ({
    x: 20 + step * index,
    y: 130 - (item.value / max) * 105
  }))
})
const linePath = computed(() => {
  if (!linePoints.value.length) {
    return ''
  }
  return linePoints.value.map((point, index) => `${index ? 'L' : 'M'} ${point.x} ${point.y}`).join(' ')
})
const areaPath = computed(() => {
  if (!linePoints.value.length) {
    return ''
  }
  const first = linePoints.value[0]
  const last = linePoints.value[linePoints.value.length - 1]
  return `${linePath.value} L ${last.x} 142 L ${first.x} 142 Z`
})
const hoveredLineItem = computed(() => {
  if (hoverIndex.value == null) {
    return null
  }
  return normalizedItems.value[hoverIndex.value] || null
})
const hoveredTooltipLeft = computed(() => {
  const total = Math.max(normalizedItems.value.length - 1, 1)
  const index = hoverIndex.value == null ? 0 : hoverIndex.value
  return Math.min(92, Math.max(8, (index / total) * 100))
})

function toSafeNumber(value) {
  const num = typeof value === 'number' ? value : Number.parseFloat(String(value ?? '').replace(/,/g, '').trim())
  return Number.isFinite(num) ? num : 0
}

function hasCountDetail(item) {
  return toSafeNumber(item?.normalCount) > 0 || toSafeNumber(item?.abnormalCount) > 0
}
</script>

<style scoped>
.mini-chart {
  position: relative;
  min-height: 160px;
}

.pie-wrap {
  display: grid;
  grid-template-columns: 154px minmax(0, 1fr);
  gap: 18px;
  align-items: center;
}

.pie-visual {
  width: 148px;
  height: 148px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  position: relative;
  box-shadow: inset 0 0 0 1px rgba(214, 225, 241, 0.8);
}

.pie-visual::after {
  content: "";
  position: absolute;
  inset: 32px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 0 0 1px rgba(226, 232, 240, 0.92);
}

.pie-visual span {
  position: relative;
  z-index: 1;
  color: var(--text-main);
  font-size: 24px;
  font-weight: 800;
}

.chart-legend,
.bar-stack {
  display: grid;
  gap: 12px;
}

.legend-row,
.bar-meta,
.axis-labels {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.legend-row i {
  width: 10px;
  height: 10px;
  flex: none;
  border-radius: 999px;
}

.legend-row span,
.bar-meta span,
.axis-labels span {
  min-width: 0;
  flex: 1;
  color: var(--text-sub);
  font-size: 13px;
}

.legend-row strong,
.bar-meta strong {
  color: var(--text-main);
  font-size: 15px;
}

.bar-row {
  display: grid;
  gap: 8px;
}

.bar-track {
  height: 12px;
  overflow: hidden;
  border-radius: 999px;
  background: #e9eef6;
}

.bar-track i {
  display: block;
  height: 100%;
  min-width: 4px;
  border-radius: inherit;
}

.line-svg {
  width: 100%;
  height: 150px;
  display: block;
}

.line-point {
  cursor: pointer;
}

.line-tooltip {
  position: absolute;
  top: 6px;
  z-index: 2;
  display: grid;
  gap: 4px;
  min-width: 132px;
  padding: 9px 10px;
  border: 1px solid rgba(47, 111, 159, 0.22);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: var(--shadow-md);
  transform: translateX(-50%);
  pointer-events: none;
}

.line-tooltip strong {
  color: var(--text-main);
  font-size: 13px;
}

.line-tooltip span {
  color: var(--text-sub);
  font-size: 12px;
  white-space: nowrap;
}

.axis-labels {
  margin-top: 4px;
}

.axis-labels span {
  flex: 0 1 auto;
  text-align: center;
}

@media (max-width: 760px) {
  .pie-wrap {
    grid-template-columns: 1fr;
  }

  .pie-visual {
    margin: 0 auto;
  }
}
</style>
