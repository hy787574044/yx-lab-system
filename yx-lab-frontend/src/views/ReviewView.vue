<template>
  <div class="content-grid">
    <section class="stats-grid">
      <article class="metric-card" v-for="item in stats" :key="item.label">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <p>{{ item.desc }}</p>
      </article>
    </section>

    <section class="glass-panel section-block">
      <div class="section-head">
        <div>
          <h3 class="section-title">结果审核</h3>
          <p class="page-subtitle">统一查看审核记录、审核结果及驳回原因，支撑检测结果复核流程。</p>
        </div>
      </div>

      <div class="toolbar">
        <el-button type="primary" @click="loadData">刷新审核队列</el-button>
        <el-button @click="approveFirst" :disabled="!records.length">通过首条</el-button>
      </div>

      <el-table :data="records" stripe empty-text="暂无审核记录数据">
        <el-table-column prop="sampleNo" label="样品编号" min-width="180" />
        <el-table-column prop="reviewerName" label="审核人" width="120" />
        <el-table-column label="审核结果" width="120">
          <template #default="{ row }">
            <span class="status-chip" :class="getStatusClass('reviewResult', row.reviewResult)">
              {{ row.reviewResult ? getEnumLabel(reviewResultLabelMap, row.reviewResult) : '待审核' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="reviewTime" label="审核时间" width="170" />
        <el-table-column prop="reviewRemark" label="审核意见" min-width="180" show-overflow-tooltip />
        <el-table-column prop="rejectReason" label="驳回原因" min-width="180" show-overflow-tooltip />
      </el-table>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchDetectionsApi, fetchReviewsApi, submitReviewApi } from '../api/lab'
import { getEnumLabel, getStatusClass, reviewResultLabelMap } from '../utils/labEnums'

const records = ref([])

const stats = computed(() => [
  { label: '审核记录', value: records.value.length, desc: '当前页已加载审核记录数' },
  { label: '审核通过', value: records.value.filter((item) => item.reviewResult === 'APPROVED').length, desc: '已通过审核的数据记录' },
  { label: '审核驳回', value: records.value.filter((item) => item.reviewResult === 'REJECTED').length, desc: '已被驳回的数据记录' },
  { label: '待处理检测', value: records.value.filter((item) => !item.reviewResult).length, desc: '尚未形成审核结果的记录' }
])

async function loadData() {
  records.value = (await fetchReviewsApi({ pageNum: 1, pageSize: 10 })).records || []
}

async function approveFirst() {
  const detectionResult = await fetchDetectionsApi({ pageNum: 1, pageSize: 10 })
  const record = detectionResult.records?.find((item) => item.detectionStatus === 'SUBMITTED')
  if (!record) {
    ElMessage.warning('当前没有待审核检测记录')
    return
  }
  await submitReviewApi({
    detectionRecordId: record.id,
    reviewResult: 'APPROVED',
    reviewRemark: '数据合格，允许出具报告'
  })
  ElMessage.success('审核通过')
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.section-head {
  margin-bottom: 16px;
}

.section-title {
  margin: 0;
  font-size: 18px;
  line-height: 1.4;
}

.metric-card p {
  margin: 10px 0 0;
  color: var(--text-sub);
  font-size: 14px;
  line-height: 1.6;
}
</style>
