<template>
  <div class="content-grid dashboard-page" v-loading="loading">
    <section class="glass-panel section-block overview-hero">
      <div class="hero-copy">
        <h2 class="section-title">运行总览</h2>
        <p class="page-subtitle">
          统一查看样品、检测、审查与报告发布状态，便于快速识别当前业务处理进度。
        </p>
      </div>
      <div class="hero-status">
        <span class="status-chip success">正常 {{ normalCount }}</span>
        <span class="status-chip danger">异常 {{ abnormalCount }}</span>
      </div>
    </section>

    <section class="stats-grid">
      <button
        v-for="item in cards"
        :key="item.label"
        type="button"
        class="metric-card metric-strong metric-card--link"
        @click="goRoute(item.path)"
      >
        <div class="metric-head">
          <span>{{ item.label }}</span>
          <el-icon :class="['metric-icon', item.tone]">
            <component :is="item.icon" />
          </el-icon>
        </div>
        <strong>{{ item.value }}</strong>
        <p>{{ item.desc }}</p>
      </button>
    </section>

    <section class="dashboard-lower">
      <button
        type="button"
        class="glass-panel section-block summary-panel summary-panel--link"
        @click="goRoute('/detection-ledger')"
      >
        <div class="panel-head">
          <h3 class="section-title">结果分布</h3>
          <span class="panel-note">按当前检测结果统计，点击进入检测台账</span>
        </div>
        <div class="result-stack">
          <div class="result-row">
            <div class="result-meta">
              <span>正常</span>
              <strong>{{ normalCount }}</strong>
            </div>
            <el-progress :percentage="normalPercent" color="#16a34a" :stroke-width="12" />
          </div>
          <div class="result-row">
            <div class="result-meta">
              <span>异常</span>
              <strong>{{ abnormalCount }}</strong>
            </div>
            <el-progress :percentage="abnormalPercent" color="#dc2626" :stroke-width="12" />
          </div>
        </div>
      </button>

      <div class="glass-panel section-block action-panel">
        <div class="panel-head">
          <h3 class="section-title">快捷操作</h3>
          <span class="panel-note">保留清晰的业务入口指引，点击即可进入对应页面</span>
        </div>
        <div class="action-list">
          <button
            v-for="(action, index) in actionItems"
            :key="action.label"
            type="button"
            class="action-item"
            @click="goRoute(action.path)"
          >
            <div class="action-index">{{ String(index + 1).padStart(2, '0') }}</div>
            <div class="action-copy">
              <strong>{{ action.label }}</strong>
              <p>{{ action.desc }}</p>
            </div>
            <el-icon class="action-icon">
              <component :is="action.icon" />
            </el-icon>
          </button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  CircleCheckFilled,
  DocumentChecked,
  Files,
  Opportunity,
  WarningFilled
} from '@element-plus/icons-vue'
import { dashboardApi } from '../api/lab'

const router = useRouter()
const loading = ref(false)
const overview = ref({})

const actionRouteMap = {
  样品采样: '/sample-login',
  检测分析: '/detection-analysis',
  结果审核: '/review-result',
  报告台账: '/report-ledger'
}

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

const normalCount = computed(() => toSafeNumber(overview.value.resultSummary?.正常))
const abnormalCount = computed(() => toSafeNumber(overview.value.resultSummary?.异常))
const resultTotal = computed(() => normalCount.value + abnormalCount.value)
const normalPercent = computed(() => calcPercent(normalCount.value, resultTotal.value))
const abnormalPercent = computed(() => calcPercent(abnormalCount.value, resultTotal.value))

const cards = computed(() => [
  {
    label: '样品总数',
    value: toSafeNumber(overview.value.sampleTotal),
    desc: '当前系统已登记样品总量',
    icon: Files,
    tone: 'brand',
    path: '/sample-ledger'
  },
  {
    label: '待审核数',
    value: toSafeNumber(overview.value.pendingReviewTotal),
    desc: '待复核、待流转的检测记录',
    icon: WarningFilled,
    tone: 'warning',
    path: '/review-result'
  },
  {
    label: '已通过检测',
    value: toSafeNumber(overview.value.approvedTotal),
    desc: '检测流程已完成审批的数据',
    icon: CircleCheckFilled,
    tone: 'success',
    path: '/detection-ledger'
  },
  {
    label: '已发布报告',
    value: toSafeNumber(overview.value.publishedReportTotal),
    desc: '已输出正式报告数量',
    icon: DocumentChecked,
    tone: 'brand',
    path: '/report-ledger'
  }
])

const defaultActionItems = [
  {
    label: '样品采样',
    desc: '快速进入采样任务与样品登录环节',
    icon: Opportunity,
    path: '/sample-login'
  },
  {
    label: '检测分析',
    desc: '进入检测数据录入与结果处理流程',
    icon: Files,
    path: '/detection-analysis'
  },
  {
    label: '结果审核',
    desc: '进入审核审批与复核处理流程',
    icon: DocumentChecked,
    path: '/review-result'
  },
  {
    label: '报告台账',
    desc: '进入报告生成与发布环节',
    icon: CircleCheckFilled,
    path: '/report-ledger'
  }
]

const actionItems = computed(() => {
  const source = overview.value.quickActions || []
  if (!source.length) {
    return defaultActionItems
  }
  return source.map((label, index) => ({
    label,
    icon: defaultActionItems[index]?.icon || Opportunity,
    desc: defaultActionItems[index]?.desc || '按既定流程完成当前业务操作。',
    path: actionRouteMap[label] || defaultActionItems[index]?.path || '/dashboard'
  }))
})

onMounted(async () => {
  loading.value = true
  try {
    overview.value = await dashboardApi()
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.dashboard-page {
  gap: 16px;
}

.overview-hero,
.dashboard-lower {
  display: grid;
  gap: 16px;
}

.overview-hero {
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
}

.section-title {
  margin: 0;
  font-size: 18px;
  line-height: 1.4;
}

.hero-status {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.metric-strong {
  min-height: 138px;
}

.metric-card--link,
.summary-panel--link,
.action-item {
  width: 100%;
  text-align: left;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.metric-card--link:hover,
.metric-card--link:focus-visible,
.summary-panel--link:hover,
.summary-panel--link:focus-visible,
.action-item:hover,
.action-item:focus-visible {
  border-color: color-mix(in srgb, var(--brand) 48%, #ffffff 52%);
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
  outline: none;
}

.metric-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.metric-icon {
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  border-radius: 12px;
  font-size: 18px;
}

.metric-icon.brand {
  color: var(--brand);
  background: var(--brand-soft);
}

.metric-icon.success {
  color: var(--success);
  background: rgba(22, 163, 74, 0.1);
}

.metric-icon.warning {
  color: var(--warning);
  background: rgba(217, 119, 6, 0.12);
}

.metric-card p {
  margin: 10px 0 0;
  color: var(--text-sub);
  font-size: 14px;
  line-height: 1.6;
}

.dashboard-lower {
  grid-template-columns: minmax(0, 1.2fr) minmax(320px, 0.8fr);
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.panel-note {
  color: var(--text-light);
  font-size: 14px;
}

.result-stack {
  display: grid;
  gap: 16px;
}

.result-row {
  display: grid;
  gap: 10px;
}

.result-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-size: 14px;
}

.result-meta span {
  color: var(--text-sub);
}

.result-meta strong {
  font-size: 18px;
}

.action-list {
  display: grid;
  gap: 12px;
}

.action-item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 14px;
  align-items: center;
  padding: 14px;
  border: 1px solid var(--line-soft);
  border-radius: 12px;
  background: var(--bg-panel-soft);
}

.action-index {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  border-radius: 10px;
  background: var(--brand-soft);
  color: var(--brand);
  font-size: 14px;
  font-weight: 700;
}

.action-copy strong {
  display: block;
  font-size: 14px;
  line-height: 1.5;
}

.action-copy p {
  margin: 4px 0 0;
  color: var(--text-sub);
  font-size: 14px;
  line-height: 1.5;
}

.action-icon {
  color: var(--text-light);
  font-size: 18px;
}

@media (max-width: 900px) {
  .overview-hero,
  .dashboard-lower {
    grid-template-columns: 1fr;
  }

  .panel-head {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
