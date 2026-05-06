<template>
  <div class="content-grid">
    <div class="stats-grid">
      <div class="metric-card">
        <span>样品总数</span>
        <strong>{{ data.sampleTotal || 0 }}</strong>
      </div>
      <div class="metric-card">
        <span>正常数</span>
        <strong>{{ data.normalTotal || 0 }}</strong>
      </div>
      <div class="metric-card">
        <span>异常数</span>
        <strong>{{ data.abnormalTotal || 0 }}</strong>
      </div>
      <div class="metric-card">
        <span>审核通过率</span>
        <strong>{{ data.approvalRate || 0 }}%</strong>
      </div>
    </div>
    <div class="glass-panel section-block">
      <h3>统计摘要</h3>
      <p class="page-subtitle">
        当前样品合格率为 {{ data.passRate || 0 }}%，总审核 {{ data.reviewTotal || 0 }} 次，
        通过 {{ data.approvedTotal || 0 }} 次，驳回 {{ data.rejectedTotal || 0 }} 次。
      </p>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { statisticsApi } from '../api/lab'

const data = ref({})

onMounted(async () => {
  data.value = await statisticsApi()
})
</script>
