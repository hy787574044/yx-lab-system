<template>
  <div class="content-grid dashboard-page" v-loading="loading">
    <section class="glass-panel section-block overview-hero">
      <div class="hero-copy">
        <h2 class="section-title">运行总览</h2>
        <p class="page-subtitle">统一查看样品、检测、审核与报告发布状态，便于快速识别当前业务处理进度。</p>
      </div>
      <div class="hero-status">
        <span class="status-chip success">正常 {{ normalCount }}</span>
        <span class="status-chip danger">异常 {{ abnormalCount }}</span>
      </div>
    </section>

    <section class="stats-grid">
      <article class="metric-card metric-strong" v-for="item in cards" :key="item.label">
        <div class="metric-head">
          <span>{{ item.label }}</span>
          <el-icon :class="['metric-icon', item.tone]">
            <component :is="item.icon" />
          </el-icon>
        </div>
        <strong>{{ item.value }}</strong>
        <p>{{ item.desc }}</p>
      </article>
    </section>

    <section class="dashboard-lower">
      <div class="glass-panel section-block summary-panel">
        <div class="panel-head">
          <h3 class="section-title">结果分布</h3>
          <span class="panel-note">按当前检测结果统计</span>
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
      </div>

      <div class="glass-panel section-block action-panel">
        <div class="panel-head">
          <h3 class="section-title">快捷操作</h3>
          <span class="panel-note">保留清晰的业务入口指引</span>
        </div>
        <div class="action-list">
          <div class="action-item" v-for="(action, index) in actionItems" :key="action.label">
            <div class="action-index">{{ String(index + 1).padStart(2, '0') }}</div>
            <div class="action-copy">
              <strong>{{ action.label }}</strong>
              <p>{{ action.desc }}</p>
            </div>
            <el-icon class="action-icon">
              <component :is="action.icon" />
            </el-icon>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import {
  CircleCheckFilled,
  DocumentChecked,
  Files,
  Opportunity,
  WarningFilled
} from '@element-plus/icons-vue'
import { dashboardApi } from '../api/lab'

const loading = ref(false)
const overview = ref({})

const normalCount = computed(() => overview.value.resultSummary?.['正常'] || 0)
const abnormalCount = computed(() => overview.value.resultSummary?.['异常'] || 0)
const resultTotal = computed(() => normalCount.value + abnormalCount.value)
const normalPercent = computed(() => (resultTotal.value ? Math.round((normalCount.value / resultTotal.value) * 100) : 0))
const abnormalPercent = computed(() => (resultTotal.value ? Math.round((abnormalCount.value / resultTotal.value) * 100) : 0))

const cards = computed(() => [
  {
    label: '样品总数',
    value: overview.value.sampleTotal || 0,
    desc: '当前系统已登记样品总量',
    icon: Files,
    tone: 'brand'
  },
  {
    label: '待审核数',
    value: overview.value.pendingReviewTotal || 0,
    desc: '待复核、待流转的检测记录',
    icon: WarningFilled,
    tone: 'warning'
  },
  {
    label: '已通过检测',
    value: overview.value.approvedTotal || 0,
    desc: '检测流程已完成审批的数据',
    icon: CircleCheckFilled,
    tone: 'success'
  },
  {
    label: '已发布报告',
    value: overview.value.publishedReportTotal || 0,
    desc: '已输出正式报告数量',
    icon: DocumentChecked,
    tone: 'brand'
  }
])

const defaultActionItems = [
  {
    label: '样品采样',
    desc: '快速进入采样计划、采样任务和样品登录环节',
    icon: Opportunity
  },
  {
    label: '检测分析',
    desc: '进入检测数据录入与结果处理流程',
    icon: Files
  },
  {
    label: '结果审核',
    desc: '进入审核审批与复核处理流程',
    icon: DocumentChecked
  },
  {
    label: '报告台账',
    desc: '进入报告生成与发布环节',
    icon: CircleCheckFilled
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
    desc: defaultActionItems[index]?.desc || '按既定流程完成当前业务操作'
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
