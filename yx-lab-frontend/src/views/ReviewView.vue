<template>
  <div class="content-grid">
    <div class="glass-panel section-block">
      <div class="toolbar">
        <el-button type="primary" @click="loadData">刷新审核队列</el-button>
        <el-button @click="approveFirst" :disabled="!records.length">通过首条</el-button>
      </div>
      <el-table :data="records" stripe>
        <el-table-column prop="sampleNo" label="样品编号" />
        <el-table-column prop="reviewerName" label="审核人" />
        <el-table-column prop="reviewResult" label="审核结果" />
        <el-table-column prop="reviewTime" label="审核时间" />
        <el-table-column prop="rejectReason" label="驳回原因" />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchDetectionsApi, fetchReviewsApi, submitReviewApi } from '../api/lab'

const records = ref([])

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
