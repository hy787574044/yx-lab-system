<template>
  <div class="content-grid statistics-page" v-loading="loading">
    <section class="glass-panel section-block statistics-hero">
      <div>
        <h2 class="section-title">分析统计</h2>
        <p class="page-subtitle">从样品合格率、审核通过率等维度汇总展示当前实验室业务运行情况。</p>
      </div>
      <div class="hero-rate">
        <strong>{{ data.passRate || 0 }}%</strong>
        <span>样品合格率</span>
      </div>
    </section>

    <section class="stats-grid">
      <article class="metric-card" v-for="item in cards" :key="item.label">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <p>{{ item.desc }}</p>
      </article>
    </section>

    <section class="statistics-lower">
      <div class="glass-panel section-block">
        <div class="panel-head">
          <h3 class="section-title">样品结果统计</h3>
          <span class="panel-note">依据样品检测结果自动汇总</span>
        </div>
        <div class="progress-stack">
          <div class="progress-row">
            <div class="progress-meta">
              <span>正常</span>
              <strong>{{ data.normalTotal || 0 }}</strong>
            </div>
            <el-progress :percentage="sampleNormalPercent" color="#16a34a" :stroke-width="12" />
          </div>
          <div class="progress-row">
            <div class="progress-meta">
              <span>异常</span>
              <strong>{{ data.abnormalTotal || 0 }}</strong>
            </div>
            <el-progress :percentage="sampleAbnormalPercent" color="#dc2626" :stroke-width="12" />
          </div>
        </div>
      </div>

      <div class="glass-panel section-block">
        <div class="panel-head">
          <h3 class="section-title">审核结果统计</h3>
          <span class="panel-note">依据审核记录自动汇总</span>
        </div>
        <div class="progress-stack">
          <div class="progress-row">
            <div class="progress-meta">
              <span>审核通过</span>
              <strong>{{ data.approvedTotal || 0 }}</strong>
            </div>
            <el-progress :percentage="reviewApprovedPercent" color="#1677ff" :stroke-width="12" />
          </div>
          <div class="progress-row">
            <div class="progress-meta">
              <span>审核驳回</span>
              <strong>{{ data.rejectedTotal || 0 }}</strong>
            </div>
            <el-progress :percentage="reviewRejectedPercent" color="#d97706" :stroke-width="12" />
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { statisticsApi } from '../api/lab'

const loading = ref(false)
const data = ref({})

const cards = computed(() => [
  { label: '样品总数', value: data.value.sampleTotal || 0, desc: '系统内已登记样品总量' },
  { label: '正常样品', value: data.value.normalTotal || 0, desc: '判定结果为正常的样品数' },
  { label: '异常样品', value: data.value.abnormalTotal || 0, desc: '判定结果为异常的样品数' },
  { label: '审核通过率', value: `${data.value.approvalRate || 0}%`, desc: '审核记录通过比例' }
])

const sampleNormalPercent = computed(() => {
  const total = (data.value.normalTotal || 0) + (data.value.abnormalTotal || 0)
  return total ? Math.round(((data.value.normalTotal || 0) / total) * 100) : 0
})

const sampleAbnormalPercent = computed(() => {
  const total = (data.value.normalTotal || 0) + (data.value.abnormalTotal || 0)
  return total ? Math.round(((data.value.abnormalTotal || 0) / total) * 100) : 0
})

const reviewApprovedPercent = computed(() => {
  const total = (data.value.approvedTotal || 0) + (data.value.rejectedTotal || 0)
  return total ? Math.round(((data.value.approvedTotal || 0) / total) * 100) : 0
})

const reviewRejectedPercent = computed(() => {
  const total = (data.value.approvedTotal || 0) + (data.value.rejectedTotal || 0)
  return total ? Math.round(((data.value.rejectedTotal || 0) / total) * 100) : 0
})

onMounted(async () => {
  loading.value = true
  try {
    data.value = await statisticsApi()
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
