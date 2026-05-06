<template>
  <div class="content-grid">
    <div class="stats-grid">
      <div class="metric-card" v-for="item in cards" :key="item.label">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </div>
    </div>
    <div class="glass-panel section-block">
      <h3>结果概览</h3>
      <div class="toolbar">
        <span class="status-chip success">正常 {{ overview.resultSummary?.['正常'] || 0 }}</span>
        <span class="status-chip danger">异常 {{ overview.resultSummary?.['异常'] || 0 }}</span>
      </div>
      <el-timeline>
        <el-timeline-item v-for="action in overview.quickActions || []" :key="action" :timestamp="action" />
      </el-timeline>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { dashboardApi } from '../api/lab'

const overview = ref({})
const cards = computed(() => [
  { label: '样品总数', value: overview.value.sampleTotal || 0 },
  { label: '待审核数', value: overview.value.pendingReviewTotal || 0 },
  { label: '已通过检测', value: overview.value.approvedTotal || 0 },
  { label: '已发布报告', value: overview.value.publishedReportTotal || 0 }
])

onMounted(async () => {
  overview.value = await dashboardApi()
})
</script>
