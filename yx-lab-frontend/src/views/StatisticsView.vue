<template>
  <div class="content-grid statistics-page" v-loading="loading">
    <section class="glass-panel section-block">
      <div class="section-head">
        <div>
          <h3 class="section-title">{{ currentScene.title }}</h3>
        </div>
      </div>

      <section class="stats-grid section-stats">
        <button
          v-for="item in currentCards"
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
    </section>

    <section class="statistics-lower">
      <section
        v-for="panel in currentPanels"
        :key="panel.title"
        class="glass-panel section-block statistics-panel"
      >
        <div class="panel-head">
          <div>
            <h3 class="section-title">{{ panel.title }}</h3>
            <p>{{ panel.note }}</p>
          </div>
          <el-button v-if="panel.path" link type="primary" @click="goRoute(panel.path)">查看明细</el-button>
        </div>
        <MiniCharts v-if="panel.chart" :type="panel.chart" :items="panel.items" :colors="panel.colors" />
        <div v-else class="progress-stack">
          <div v-for="row in panel.rows" :key="row.label" class="progress-row">
            <div class="progress-meta">
              <span>{{ row.label }}</span>
              <strong>{{ row.value }}</strong>
            </div>
            <el-progress :percentage="row.percent" :color="row.color" :stroke-width="12" />
          </div>
        </div>
      </section>
    </section>

  </div>
</template>

<script setup>
import { computed, onMounted, ref, unref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElLoadingDirective } from 'element-plus/es/components/loading/index.mjs'
import { ElProgress } from 'element-plus/es/components/progress/index.mjs'
import { ElButton } from 'element-plus/es/components/button/index.mjs'
import { dashboardApi, statisticsApi } from '../api/lab'
import MiniCharts from '../components/common/MiniCharts.vue'
import {
  detectionResultLabelMap,
  detectionStatusLabelMap,
  getEnumLabel,
  reportStatusLabelMap,
  reviewResultLabelMap,
  sampleStatusLabelMap,
  sampleTypeLabelMap
} from '../utils/labEnums'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const summary = ref({})
const overview = ref({})
const vLoading = ElLoadingDirective

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

function formatPercent(value) {
  return `${toSafeNumber(value)}%`
}

function goRoute(path) {
  if (!path) {
    return
  }
  router.push(path)
}

const sampleTotal = computed(() => toSafeNumber(summary.value.sampleTotal || overview.value.sampleTotal))
const normalTotal = computed(() => {
  const direct = toSafeNumber(summary.value.normalTotal)
  if (direct) {
    return direct
  }
  return toSafeNumber(overview.value.resultSummary?.正常)
})
const abnormalTotal = computed(() => {
  const direct = toSafeNumber(summary.value.abnormalTotal)
  if (direct) {
    return direct
  }
  return toSafeNumber(overview.value.resultSummary?.异常)
})
const approvedTotal = computed(() => toSafeNumber(summary.value.approvedTotal || overview.value.approvedTotal))
const rejectedTotal = computed(() => toSafeNumber(summary.value.rejectedTotal))
const pendingReviewTotal = computed(() => toSafeNumber(overview.value.pendingReviewTotal))
const publishedReportTotal = computed(() => toSafeNumber(overview.value.publishedReportTotal))
const passRate = computed(() => formatPercent(summary.value.passRate))
const approvalRate = computed(() => formatPercent(summary.value.approvalRate))

const resultTotal = computed(() => normalTotal.value + abnormalTotal.value)
const reviewTotal = computed(() => approvedTotal.value + rejectedTotal.value)
const timeBucketItems = computed(() => (summary.value.timeBuckets || []).map((item) => ({
  label: item.label,
  value: toSafeNumber(item.sampleTotal) + toSafeNumber(item.detectionTotal) + toSafeNumber(item.reviewTotal) + toSafeNumber(item.reportTotal)
})))
const sampleTypeItems = computed(() => normalizeDimension(summary.value.sampleTypeDistribution, sampleTypeLabelMap))
const sampleStatusItems = computed(() => normalizeDimension(summary.value.sampleStatusDistribution, sampleStatusLabelMap))
const detectionStatusItems = computed(() => normalizeDimension(summary.value.detectionStatusDistribution, detectionStatusLabelMap))
const detectionResultItems = computed(() => normalizeDimension(summary.value.detectionResultDistribution, detectionResultLabelMap))
const reviewResultItems = computed(() => normalizeDimension(summary.value.reviewResultDistribution, reviewResultLabelMap))
const reportStatusItems = computed(() => normalizeDimension(summary.value.reportStatusDistribution, reportStatusLabelMap))
const detectionTypeItems = computed(() => normalizeDimension(summary.value.detectionTypeRanking))

const sceneMap = {
  '/statistics-count': {
    title: '化验数量统计',
    subtitle: '聚焦样品数量、审核处理量与报告产出量，适合看业务规模与处理进度。',
    heroValue: sampleTotal,
    heroLabel: '样品总量',
    guide: '数量统计用于看业务“做了多少”，更适合管理者盘点任务量、处理量和产物量。',
    quickLinks: [
      { path: '/sample-ledger', label: '样品台账', desc: '查看样品全量数据来源' },
      { path: '/review-ledger', label: '审查台账', desc: '核对审核处理总量与状态' },
      { path: '/report-ledger', label: '报告台账', desc: '查看正式报告产物数量' }
    ]
  },
  '/statistics-result': {
    title: '化验结果统计',
    subtitle: '聚焦正常、异常、驳回等结果分布，适合看检测结果结构和异常占比。',
    heroValue: passRate,
    heroLabel: '样品合格率',
    guide: '结果统计用于看业务“结果怎么样”，尤其适合识别异常样品与驳回分布。',
    quickLinks: [
      { path: '/detection-ledger', label: '检测台账', desc: '查看结果分布对应的检测明细' },
      { path: '/review-history', label: '历史审查', desc: '查看结果对应的审核结论' },
      { path: '/sample-ledger', label: '样品台账', desc: '回溯异常结果对应的样品来源' }
    ]
  },
  '/statistics-quality': {
    title: '化验质量统计',
    subtitle: '聚焦合格率、审核通过率、待审核积压与报告发布，适合看质量闭环表现。',
    heroValue: approvalRate,
    heroLabel: '审核通过率',
    guide: '质量统计用于看业务“做得怎么样”，重点观察审核效率、驳回情况和闭环质量。',
    quickLinks: [
      { path: '/review-result', label: '结果审查', desc: '优先处理待审核积压数据' },
      { path: '/detection-history', label: '历史检测', desc: '回看异常与驳回的检测背景' },
      { path: '/report-ledger', label: '报告台账', desc: '核对最终发布产物与闭环结果' }
    ]
  }
}

const baseScene = computed(() => sceneMap[route.path] || sceneMap['/statistics-count'])
const currentScene = computed(() => ({
  ...baseScene.value,
  heroValue: unref(baseScene.value.heroValue),
  heroLabel: baseScene.value.heroLabel
}))

const currentCards = computed(() => {
  if (route.path === '/statistics-result') {
    return [
      { label: '正常样品', value: normalTotal.value, desc: '检测结果为正常的样品数量', path: '/sample-ledger' },
      { label: '异常样品', value: abnormalTotal.value, desc: '检测结果为异常的样品数量', path: '/detection-ledger' },
      { label: '样品合格率', value: passRate.value, desc: '正常样品在全部结果中的占比', path: '/sample-ledger' },
      { label: '审核驳回量', value: rejectedTotal.value, desc: '审核驳回并退回重检的记录数量', path: '/review-history' }
    ]
  }

  if (route.path === '/statistics-quality') {
    return [
      { label: '样品合格率', value: passRate.value, desc: '从检测结果角度衡量质量稳定性', path: '/detection-ledger' },
      { label: '审核通过率', value: approvalRate.value, desc: '从审核结果角度衡量质量闭环情况', path: '/review-ledger' },
      { label: '待审核量', value: pendingReviewTotal.value, desc: '当前仍待审查的检测结果数量', path: '/review-result' },
      { label: '已发布报告', value: publishedReportTotal.value, desc: '正式发布的报告产物数量', path: '/report-ledger' }
    ]
  }

  return [
    { label: '样品总量', value: sampleTotal.value, desc: '系统内已登记样品总量', path: '/sample-ledger' },
    { label: '检测结果量', value: resultTotal.value, desc: '已形成检测结果的样品数量', path: '/detection-ledger' },
    { label: '审核处理量', value: reviewTotal.value, desc: '已形成审核结论的记录数量', path: '/review-ledger' },
    { label: '已发布报告', value: publishedReportTotal.value, desc: '正式报告发布产物数量', path: '/report-ledger' }
  ]
})

const currentPanels = computed(() => {
  if (route.path === '/statistics-result') {
    return [
      {
        title: '检测结果占比',
        note: '用饼图查看正常、异常结果结构',
        path: '/sample-ledger',
        chart: 'pie',
        colors: ['#16a34a', '#dc2626'],
        items: detectionResultItems.value
      },
      {
        title: '审核结果结构',
        note: '审核通过与驳回情况对比',
        path: '/review-ledger',
        chart: 'bar',
        colors: ['#1677ff', '#d97706'],
        items: reviewResultItems.value
      },
      {
        title: '检测状态分布',
        note: '观察结果在流程中的流转状态',
        path: '/detection-ledger',
        chart: 'bar',
        items: detectionStatusItems.value
      },
      {
        title: '检测套餐结果量排行',
        note: '定位产生结果最多的检测套餐',
        path: '/detection-ledger',
        chart: 'bar',
        colors: ['#0f766e', '#1677ff', '#d97706'],
        items: detectionTypeItems.value
      }
    ]
  }

  if (route.path === '/statistics-quality') {
    return [
      {
        title: '质量核心占比',
        note: '合格率与异常风险占比',
        path: '/detection-ledger',
        chart: 'pie',
        colors: ['#16a34a', '#dc2626'],
        items: [
          { label: '正常', value: normalTotal.value },
          { label: '异常', value: abnormalTotal.value }
        ]
      },
      {
        title: '审核闭环效率',
        note: '通过、驳回、待审核多维对比',
        path: '/review-result',
        chart: 'bar',
        colors: ['#1677ff', '#d97706', '#dc2626'],
        items: [
          { label: '审核通过', value: approvedTotal.value },
          { label: '审核驳回', value: rejectedTotal.value },
          { label: '待审核', value: pendingReviewTotal.value }
        ]
      },
      {
        title: '报告发布质量',
        note: '正式报告各状态分布',
        path: '/report-ledger',
        chart: 'pie',
        items: reportStatusItems.value
      },
      {
        title: '质量时间趋势',
        note: '不同时间跨度的业务处理量变化',
        chart: 'line',
        items: timeBucketItems.value
      }
    ]
  }

  return [
    {
      title: '多时间跨度数量趋势',
      note: '今日、近7日、近30日、全部的处理量变化',
      chart: 'line',
      items: timeBucketItems.value
    },
    {
      title: '样品类型结构',
      note: '按样品类型展示数量结构',
      path: '/sample-ledger',
      chart: 'pie',
      items: sampleTypeItems.value
    },
    {
      title: '样品状态分布',
      note: '样品在登录、审核、重检、完成中的分布',
      path: '/sample-ledger',
      chart: 'bar',
      items: sampleStatusItems.value
    },
    {
      title: '业务流转数量',
      note: '检测、审核、报告产物关键节点数量',
      path: '/review-ledger',
      chart: 'bar',
      colors: ['#1677ff', '#d97706', '#16a34a', '#0f766e'],
      items: [
        { label: '检测结果', value: resultTotal.value },
        { label: '待审核', value: pendingReviewTotal.value },
        { label: '已审核', value: reviewTotal.value },
        { label: '已发布', value: publishedReportTotal.value }
      ]
    }
  ]
})

function normalizeDimension(items, labelMap = {}) {
  return (items || []).map((item) => ({
    label: getEnumLabel(labelMap, item.name),
    value: item.value
  }))
}

async function loadData() {
  loading.value = true
  try {
    const [summaryResult, overviewResult] = await Promise.all([statisticsApi(), dashboardApi()])
    summary.value = summaryResult || {}
    overview.value = overviewResult || {}
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadData()
})

watch(() => route.fullPath, () => {
  void loadData()
})
</script>

<style scoped>
.statistics-page,
.scene-grid {
  gap: 12px;
}

.statistics-hero,
.statistics-lower,
.scene-grid {
  display: grid;
  gap: 12px;
}

.statistics-hero {
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
}

.hero-value {
  min-width: 180px;
  padding: 14px;
  border-radius: 14px;
  background: var(--brand-soft);
  text-align: center;
}

.hero-value strong {
  display: block;
  color: var(--brand);
  font-size: 32px;
  line-height: 1.2;
}

.hero-value span,
.panel-note,
.metric-card p {
  color: var(--text-sub);
  font-size: 14px;
}

.metric-card--link,
.quick-link {
  width: 100%;
  text-align: left;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.metric-card--link:hover,
.metric-card--link:focus-visible,
.quick-link:hover,
.quick-link:focus-visible {
  border-color: color-mix(in srgb, var(--brand) 48%, #ffffff 52%);
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
  outline: none;
}

.metric-card p {
  margin: 8px 0 0;
  line-height: 1.6;
}

.statistics-lower,
.scene-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.statistics-panel {
  min-height: 260px;
}

.statistics-panel .panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.statistics-panel .panel-head p {
  margin: 4px 0 0;
  color: var(--text-sub);
  font-size: 13px;
}

.progress-stack,
.scene-copy,
.quick-links {
  display: grid;
  gap: 10px;
}

.progress-row {
  display: grid;
  gap: 8px;
}

.progress-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.progress-meta strong {
  font-size: 18px;
}

.scene-copy {
  color: var(--text-sub);
  font-size: 14px;
  line-height: 1.7;
}

.scene-copy p {
  margin: 0;
}

.quick-link {
  display: grid;
  gap: 4px;
  padding: 12px;
  border: 1px solid var(--line-soft);
  border-radius: 12px;
  background: var(--bg-panel-soft);
}

.quick-link strong {
  color: var(--text-main);
  font-size: 14px;
  line-height: 1.5;
}

.quick-link span {
  color: var(--text-sub);
  font-size: 13px;
  line-height: 1.6;
}

@media (max-width: 900px) {
  .statistics-hero,
  .statistics-lower,
  .scene-grid {
    grid-template-columns: 1fr;
  }
}
</style>
