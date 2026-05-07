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
          <p class="page-subtitle">统一查看检测记录及检测状态，并支持快速生成演示检测数据。</p>
        </div>
      </div>

      <div class="toolbar">
        <el-button type="primary" @click="loadData">刷新检测记录</el-button>
        <el-button @click="submitDemo">提交演示检测</el-button>
      </div>

      <div class="table-card">
        <el-table class="list-table" :data="records" stripe max-height="420" empty-text="暂无检测记录数据">
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

        <TablePagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          @change="loadData"
        />
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchDetectionsApi, fetchSamplesApi, submitDetectionApi } from '../api/lab'
import TablePagination from '../components/common/TablePagination.vue'
import {
  abnormalDetectionResult,
  availableDetectionSampleStatuses,
  DEFAULT_PAGE_SIZE,
  detectionResultLabelMap,
  detectionStatusLabelMap,
  getEnumLabel,
  getStatusClass,
  loggedSampleStatus,
  rejectedDetectionStatus,
  retestSampleStatus,
  reviewPendingDetectionStatus
} from '../utils/labEnums'

const query = reactive({ pageNum: 1, pageSize: DEFAULT_PAGE_SIZE })
const records = ref([])
const total = ref(0)

const stats = computed(() => [
  { label: '检测总数', value: total.value, desc: '检测记录总量' },
  { label: '本页记录', value: records.value.length, desc: '当前分页加载的检测记录' },
  { label: '待审核', value: records.value.filter((item) => item.detectionStatus === reviewPendingDetectionStatus).length, desc: '当前页待审核的检测记录' },
  { label: '已驳回', value: records.value.filter((item) => item.detectionStatus === rejectedDetectionStatus).length, desc: '当前页已驳回的检测记录' },
  { label: '异常结果', value: records.value.filter((item) => item.detectionResult === abnormalDetectionResult).length, desc: '当前页异常检测结果' }
])

async function loadData() {
  const result = await fetchDetectionsApi(query)
  records.value = result.records || []
  total.value = result.total || 0
}

async function submitDemo() {
  const sampleResult = await fetchSamplesApi({ pageNum: 1, pageSize: DEFAULT_PAGE_SIZE })
  const availableSamples = sampleResult.records?.filter((item) => availableDetectionSampleStatuses.includes(item.sampleStatus)) || []
  const sample = availableSamples.find((item) => item.sampleStatus === retestSampleStatus)
    || availableSamples.find((item) => item.sampleStatus === loggedSampleStatus)
  if (!sample) {
    ElMessage.warning('请先准备一条已登录或待重检的样品')
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

  ElMessage.success(sample.sampleStatus === retestSampleStatus ? '重检记录已提交，样品重新进入审核流程' : '检测记录已提交')
  query.pageNum = 1
  await loadData()
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
