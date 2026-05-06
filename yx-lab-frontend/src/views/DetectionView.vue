<template>
  <div class="content-grid">
    <div class="glass-panel section-block">
      <div class="toolbar">
        <el-button type="primary" @click="loadData">刷新检测记录</el-button>
        <el-button @click="submitDemo">提交演示检测</el-button>
      </div>
      <el-table :data="records" stripe>
        <el-table-column prop="sampleNo" label="样品编号" />
        <el-table-column prop="detectionTypeName" label="检测类型" />
        <el-table-column prop="detectorName" label="检测人" />
        <el-table-column prop="detectionResult" label="检测结果" />
        <el-table-column prop="detectionStatus" label="状态" />
        <el-table-column prop="detectionTime" label="检测时间" />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchDetectionsApi, fetchSamplesApi, submitDetectionApi } from '../api/lab'

const records = ref([])

async function loadData() {
  records.value = (await fetchDetectionsApi({ pageNum: 1, pageSize: 10 })).records || []
}

async function submitDemo() {
  const sampleResult = await fetchSamplesApi({ pageNum: 1, pageSize: 10 })
  const sample = sampleResult.records?.[0]
  if (!sample) {
    ElMessage.warning('请先完成样品登录')
    return
  }
  await submitDetectionApi({
    sampleId: sample.id,
    detectionTypeId: 3101,
    detectionTypeName: '日检九项',
    items: [
      { parameterId: 3001, parameterName: 'pH', standardMin: 6.5, standardMax: 8.5, resultValue: 7.12, unit: '' },
      { parameterId: 3002, parameterName: '浊度', standardMin: 0, standardMax: 1, resultValue: 0.66, unit: 'NTU' },
      { parameterId: 3003, parameterName: '余氯', standardMin: 0.05, standardMax: 2, resultValue: 0.31, unit: 'mg/L' },
      { parameterId: 3004, parameterName: '氨氮', standardMin: 0, standardMax: 0.5, resultValue: 0.18, unit: 'mg/L' }
    ]
  })
  ElMessage.success('检测记录已提交')
  loadData()
}

onMounted(loadData)
</script>
