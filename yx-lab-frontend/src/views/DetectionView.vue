<template>
  <div class="content-grid">
    <section class="stats-grid">
      <button
        v-for="item in stats"
        :key="item.label"
        type="button"
        :class="['metric-card', 'metric-card--action', { 'is-active': activeStatKey === item.key }]"
        @click="handleStatClick(item.key)"
      >
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <p>{{ item.desc }}</p>
      </button>
    </section>

    <section class="glass-panel section-block">
      <div class="section-head">
        <div>
          <h3 class="section-title">检测分析</h3>
          <p class="page-subtitle">
            统一查看检测记录、检测结果与检测状态，并支持快速生成一条演示检测数据进行流程验证。
          </p>
        </div>
      </div>

      <div class="toolbar-panel">
        <div class="toolbar-row">
          <div class="panel-note">点击上方统计卡片可筛选当前页记录；再次点击“检测总数”或“本页记录”可恢复全部。</div>
          <div class="toolbar-actions">
            <el-button type="primary" @click="loadData">刷新检测记录</el-button>
            <el-button @click="submitDemo">提交演示检测</el-button>
          </div>
        </div>
      </div>

      <div class="table-card">
        <el-table class="list-table" :data="visibleRecords" stripe max-height="420" empty-text="暂无检测记录数据">
          <el-table-column prop="sampleNo" label="样品编号" min-width="180" />
          <el-table-column prop="sealNo" label="封签编号" min-width="180" />
          <el-table-column prop="detectionTypeName" label="检测类别" min-width="160" />
          <el-table-column prop="detectorName" label="检测人" width="120" />
          <el-table-column label="检测结果" width="120" header-cell-class-name="cell-center" class-name="cell-center">
            <template #default="{ row }">
              <span class="status-chip" :class="getStatusClass('detectionResult', row.detectionResult)">
                {{ getEnumLabel(detectionResultLabelMap, row.detectionResult) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="120" header-cell-class-name="cell-center" class-name="cell-center">
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
import {
  fetchDetectionParametersApi,
  fetchDetectionsApi,
  fetchDetectionTypesApi,
  fetchSamplesApi,
  submitDetectionApi
} from '../api/lab'
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
const activeStatKey = ref('all')

const stats = computed(() => [
  { key: 'all', label: '检测总数', value: total.value, desc: '检测记录总量' },
  { key: 'page', label: '本页记录', value: records.value.length, desc: '当前分页加载的检测记录条数' },
  {
    key: 'pendingReview',
    label: '待审核',
    value: records.value.filter((item) => item.detectionStatus === reviewPendingDetectionStatus).length,
    desc: '当前页已提交、待审核的检测记录'
  },
  {
    key: 'rejected',
    label: '已驳回',
    value: records.value.filter((item) => item.detectionStatus === rejectedDetectionStatus).length,
    desc: '当前页已驳回并待重检的检测记录'
  },
  {
    key: 'abnormal',
    label: '异常结果',
    value: records.value.filter((item) => item.detectionResult === abnormalDetectionResult).length,
    desc: '当前页判定为异常的检测结果'
  }
])

const visibleRecords = computed(() => {
  if (activeStatKey.value === 'pendingReview') {
    return records.value.filter((item) => item.detectionStatus === reviewPendingDetectionStatus)
  }
  if (activeStatKey.value === 'rejected') {
    return records.value.filter((item) => item.detectionStatus === rejectedDetectionStatus)
  }
  if (activeStatKey.value === 'abnormal') {
    return records.value.filter((item) => item.detectionResult === abnormalDetectionResult)
  }
  return records.value
})

function handleStatClick(key) {
  activeStatKey.value = key === activeStatKey.value ? 'all' : key
}

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
    ElMessage.warning('请先准备一条已登录或待重检的样品。')
    return
  }

  const typeResult = await fetchDetectionTypesApi({ pageNum: 1, pageSize: 100 })
  const detectionType = typeResult.records?.find((item) => item.enabled === 1)
  if (!detectionType) {
    ElMessage.warning('请先启用一项检测类型配置。')
    return
  }

  const parameterIdList = (detectionType.parameterIds || '')
    .split(',')
    .map((item) => Number(item.trim()))
    .filter((item) => Number.isFinite(item))

  if (!parameterIdList.length) {
    ElMessage.warning('当前检测类型尚未配置检测参数。')
    return
  }

  const parameterResult = await fetchDetectionParametersApi({ pageNum: 1, pageSize: 200 })
  const parameterMap = new Map((parameterResult.records || []).map((item) => [item.id, item]))
  const configuredParameters = parameterIdList
    .map((id) => parameterMap.get(id))
    .filter((item) => item && item.enabled === 1)

  if (configuredParameters.length !== parameterIdList.length) {
    ElMessage.warning('当前检测类型存在未启用或缺失的检测参数，请先修正配置。')
    return
  }

  await submitDetectionApi({
    sampleId: sample.id,
    detectionTypeId: detectionType.id,
    detectionTypeName: detectionType.typeName,
    items: configuredParameters.map((parameter, index) => ({
      parameterId: parameter.id,
      parameterName: parameter.parameterName,
      standardMin: parameter.standardMin,
      standardMax: parameter.standardMax,
      resultValue: buildDemoResult(parameter, index),
      unit: parameter.unit
    }))
  })

  ElMessage.success(sample.sampleStatus === retestSampleStatus
    ? '重检记录已提交，样品重新进入审核流程。'
    : '检测记录已提交。')
  query.pageNum = 1
  await loadData()
}

function buildDemoResult(parameter, index) {
  if (parameter.standardMin != null && parameter.standardMax != null) {
    return Number(((Number(parameter.standardMin) + Number(parameter.standardMax)) / 2).toFixed(2))
  }
  if (parameter.standardMax != null) {
    return Number((Math.max(Number(parameter.standardMax) - 0.1, 0)).toFixed(2))
  }
  if (parameter.standardMin != null) {
    return Number((Number(parameter.standardMin) + 0.1).toFixed(2))
  }
  return Number((0.5 + index * 0.1).toFixed(2))
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

.metric-card--action {
  width: 100%;
  text-align: left;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.metric-card--action:hover,
.metric-card--action:focus-visible,
.metric-card--action.is-active {
  border-color: color-mix(in srgb, var(--brand) 48%, #ffffff 52%);
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
  outline: none;
}

.metric-card p {
  margin: 10px 0 0;
  color: var(--text-sub);
  font-size: 14px;
  line-height: 1.6;
}
</style>
