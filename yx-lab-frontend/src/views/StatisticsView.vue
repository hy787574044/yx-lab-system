<template>
  <div class="content-grid statistics-page" v-loading="loading">
    <button type="button" class="glass-panel section-block statistics-hero statistics-hero--link" @click="goRoute('/samples')">
      <div>
        <h2 class="section-title">分析统计</h2>
        <p class="page-subtitle">从样品合格率、审核通过率等维度汇总展示当前实验室业务运行情况。</p>
      </div>
      <div class="hero-rate">
        <strong>{{ passRateText }}%</strong>
        <span>样品合格率</span>
      </div>
    </button>

    <section class="stats-grid">
      <button
        v-for="item in cards"
        :key="item.label"
        type="button"
        class="metric-card metric-card--link"
        @click="goRoute(item.path)"
      >
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <p>{{ item.desc }}</p>
      </button>
    </section>

    <section class="statistics-lower">
      <button type="button" class="glass-panel section-block statistics-panel statistics-panel--link" @click="goRoute('/samples')">
        <div class="panel-head">
          <h3 class="section-title">样品结果统计</h3>
          <span class="panel-note">依据样品检测结果自动汇总，点击进入样品采样</span>
        </div>
        <div class="progress-stack">
          <div class="progress-row">
            <div class="progress-meta">
              <span>正常</span>
              <strong>{{ toSafeNumber(data.normalTotal) }}</strong>
            </div>
            <el-progress :percentage="sampleNormalPercent" color="#16a34a" :stroke-width="12" />
          </div>
          <div class="progress-row">
            <div class="progress-meta">
              <span>异常</span>
              <strong>{{ toSafeNumber(data.abnormalTotal) }}</strong>
            </div>
            <el-progress :percentage="sampleAbnormalPercent" color="#dc2626" :stroke-width="12" />
          </div>
        </div>
      </button>

      <button type="button" class="glass-panel section-block statistics-panel statistics-panel--link" @click="goRoute('/reviews')">
        <div class="panel-head">
          <h3 class="section-title">审核结果统计</h3>
          <span class="panel-note">依据审核记录自动汇总，点击进入结果审核</span>
        </div>
        <div class="progress-stack">
          <div class="progress-row">
            <div class="progress-meta">
              <span>审核通过</span>
              <strong>{{ toSafeNumber(data.approvedTotal) }}</strong>
            </div>
            <el-progress :percentage="reviewApprovedPercent" color="#1677ff" :stroke-width="12" />
          </div>
          <div class="progress-row">
            <div class="progress-meta">
              <span>审核驳回</span>
              <strong>{{ toSafeNumber(data.rejectedTotal) }}</strong>
            </div>
            <el-progress :percentage="reviewRejectedPercent" color="#d97706" :stroke-width="12" />
          </div>
        </div>
      </button>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { statisticsApi } from '../api/lab'

const router = useRouter()
const loading = ref(false)
const data = ref({})

function toSafeNumber(value) {
  const num = typeof value === 'number' ? value : Number.parseFloat(String(value ?? '').replace(/,/g, '').trim())
  return Number.isFinite(num) ? num : 0
}

function calcPercent(part, total) {
  const safePart = toSafeNumber(part)
  const safeTotal = toSafeNumber(total)
  if (!safeTotal) {
    return 0
  }
  return Math.max(0, Math.min(100, Math.round((safePart / safeTotal) * 100)))
}

function goRoute(path) {
  if (!path) {
    return
  }
  router.push(path)
}

const cards = computed(() => [
  { label: '样品总数', value: toSafeNumber(data.value.sampleTotal), desc: '系统内已登记样品总量', path: '/samples' },
  { label: '正常样品', value: toSafeNumber(data.value.normalTotal), desc: '判定结果为正常的样品数', path: '/samples' },
  { label: '异常样品', value: toSafeNumber(data.value.abnormalTotal), desc: '判定结果为异常的样品数', path: '/detections' },
  { label: '审核通过率', value: `${approvalRateText.value}%`, desc: '审核记录通过比例', path: '/reviews' }
])

const passRateText = computed(() => toSafeNumber(data.value.passRate))
const approvalRateText = computed(() => toSafeNumber(data.value.approvalRate))

const sampleNormalPercent = computed(() => {
  const total = toSafeNumber(data.value.normalTotal) + toSafeNumber(data.value.abnormalTotal)
  return calcPercent(data.value.normalTotal, total)
})

const sampleAbnormalPercent = computed(() => {
  const total = toSafeNumber(data.value.normalTotal) + toSafeNumber(data.value.abnormalTotal)
  return calcPercent(data.value.abnormalTotal, total)
})

const reviewApprovedPercent = computed(() => {
  const total = toSafeNumber(data.value.approvedTotal) + toSafeNumber(data.value.rejectedTotal)
  return calcPercent(data.value.approvedTotal, total)
})

const reviewRejectedPercent = computed(() => {
  const total = toSafeNumber(data.value.approvedTotal) + toSafeNumber(data.value.rejectedTotal)
  return calcPercent(data.value.rejectedTotal, total)
})

onMounted(async () => {
  loading.value = true
  try {
    data.value = await statisticsApi() || {}
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.statistics-page,
.statistics-lower {
  gap: 16px;
}

.statistics-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.statistics-hero--link,
.metric-card--link,
.statistics-panel--link {
  width: 100%;
  text-align: left;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.statistics-hero--link:hover,
.statistics-hero--link:focus-visible,
.metric-card--link:hover,
.metric-card--link:focus-visible,
.statistics-panel--link:hover,
.statistics-panel--link:focus-visible {
  border-color: color-mix(in srgb, var(--brand) 48%, #ffffff 52%);
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
  outline: none;
}

.section-title {
  margin: 0;
  font-size: 18px;
  line-height: 1.4;
}

.hero-rate {
  min-width: 180px;
  padding: 16px;
  border-radius: 14px;
  background: var(--brand-soft);
  text-align: center;
}

.hero-rate strong {
  display: block;
  color: var(--brand);
  font-size: 32px;
  line-height: 1.2;
}

.hero-rate span,
.panel-note,
.metric-card p {
  color: var(--text-sub);
  font-size: 14px;
}

.metric-card p {
  margin: 10px 0 0;
  line-height: 1.6;
}

.statistics-lower {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.progress-stack {
  display: grid;
  gap: 16px;
}

.progress-row {
  display: grid;
  gap: 10px;
}

.progress-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.progress-meta strong {
  font-size: 18px;
}

@media (max-width: 900px) {
  .statistics-hero,
  .statistics-lower {
    display: grid;
    grid-template-columns: 1fr;
  }

  .panel-head {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
