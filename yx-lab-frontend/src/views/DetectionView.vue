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
          <h3 class="section-title">检测分析</h3>
          <p class="page-subtitle">统一查看检测记录及检测状态，并可快速生成演示检测数据。</p>
        </div>
      </div>

      <div class="toolbar">
        <el-button type="primary" @click="loadData">刷新检测记录</el-button>
        <el-button @click="submitDemo">提交演示检测</el-button>
      </div>

      <el-table :data="records" stripe empty-text="暂无检测记录数据">
        <el-table-column prop="sampleNo" label="样品编号" min-width="180" />
        <el-table-column prop="detectionTypeName" label="检测类别" min-width="160" />
        <el-table-column prop="detectorName" label="检测人" width="120" />
        <el-table-column label="检测结果" width="120">
          <template #default="{ row }">
            <span class="status-chip" :class="getStatusClass('detectionResult', row.detectionResult)">
              {{ getEnumLabel(detectionResultLabelMap, row.detectionResult) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <span class="status-chip" :class="getStatusClass('detectionStatus', row.detectionStatus)">
              {{ getEnumLabel(detectionStatusLabelMap, row.detectionStatus) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="detectionTime" label="检测时间" width="170" />
        <el-table-column prop="abnormalRemark" label="异常说明" min-width="180" show-overflow-tooltip />
      </el-table>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchDetectionsApi, fetchSamplesApi, submitDetectionApi } from '../api/lab'
import {
  detectionResultLabelMap,
  detectionStatusLabelMap,
  getEnumLabel,
  getStatusClass
} from '../utils/labEnums'

const records = ref([])

const stats = computed(() => [
  { label: '检测记录', value: records.value.length, desc: '当前页已加载检测记录数' },
  { label: '已提交', value: records.value.filter((item) => item.detectionStatus === 'SUBMITTED').length, desc: '待审核的检测记录' },
  { label: '已通过', value: records.value.filter((item) => item.detectionStatus === 'APPROVED').length, desc: '已审核通过的记录' },
  { label: '异常结果', value: records.value.filter((item) => item.detectionResult === 'ABNORMAL').length, desc: '检测结果判定为异常的记录' }
])

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
