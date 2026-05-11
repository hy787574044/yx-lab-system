<template>
  <div class="content-grid detection-page">
    <section class="glass-panel section-block page-hero">
      <div>
        <h2 class="page-title">{{ currentScene.title }}</h2>
        <p class="page-subtitle">{{ currentScene.subtitle }}</p>
      </div>
      <div class="hero-tags">
        <span
          v-for="tag in currentScene.tags"
          :key="tag.label"
          :class="['status-chip', tag.type]"
        >
          {{ tag.label }} {{ tag.value }}
        </span>
      </div>
    </section>

    <section class="stats-grid">
      <button
        v-for="item in currentStats"
        :key="item.key"
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
          <h3 class="section-title">{{ baseScene.tableTitle }}</h3>
          <p class="page-subtitle">{{ baseScene.tableSubtitle }}</p>
        </div>
      </div>

      <div class="toolbar-panel">
        <div class="toolbar-row">
          <div class="panel-note">{{ baseScene.note }}</div>
          <div class="toolbar-actions">
            <el-button type="primary" @click="loadData">刷新检测记录</el-button>
            <el-button v-if="baseScene.allowSubmit" @click="submitDemo">提交演示检测</el-button>
            <el-button
              v-if="baseScene.key === 'detection-history'"
              type="primary"
              plain
              @click="goRoute('/review-history')"
            >
              查看历史审查
            </el-button>
            <el-button
              v-if="baseScene.key === 'detection-ledger'"
              type="primary"
              plain
              @click="goRoute('/review-result')"
            >
              前往结果审查
            </el-button>
          </div>
        </div>
      </div>

      <div class="table-card">
        <el-table class="list-table" :data="pagedRecords" stripe max-height="460" :empty-text="baseScene.emptyText">
          <el-table-column prop="sampleNo" label="样品编号" min-width="180" />
          <el-table-column prop="sealNo" label="封签编号" min-width="180" />
          <el-table-column prop="detectionTypeName" label="检测类别" min-width="180" />
          <el-table-column prop="detectorName" label="检测人" width="120" />
          <el-table-column label="检测结果" width="120" header-cell-class-name="cell-center" class-name="cell-center">
            <template #default="{ row }">
              <span class="status-chip" :class="getDetectionResultClass(row.detectionResult)">
                {{ getDetectionResultLabel(row.detectionResult) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="检测状态" width="120" header-cell-class-name="cell-center" class-name="cell-center">
            <template #default="{ row }">
              <span class="status-chip" :class="getDetectionStatusClass(row.detectionStatus)">
                {{ getDetectionStatusLabel(row.detectionStatus) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="detectionTime" label="检测时间" width="170" />
          <el-table-column prop="abnormalRemark" label="异常说明" min-width="180" show-overflow-tooltip />
        </el-table>

        <TablePagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="displayTotal"
          @change="loadData"
        />
      </div>
    </section>

    <section class="scene-grid">
      <div class="glass-panel section-block">
        <div class="section-head">
          <h3 class="section-title">页面说明</h3>
        </div>
        <div class="scene-copy">
          <p>{{ baseScene.guide }}</p>
          <p>当前检测页已经区分“当前处理、历史追溯、全量台账”三种使用意图。</p>
        </div>
      </div>

      <div class="glass-panel section-block">
        <div class="section-head">
          <h3 class="section-title">关联入口</h3>
        </div>
        <div class="quick-links">
          <button
            v-for="item in baseScene.quickLinks"
            :key="item.path"
            type="button"
            class="quick-link"
            @click="goRoute(item.path)"
          >
            <strong>{{ item.label }}</strong>
            <span>{{ item.desc }}</span>
          </button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElButton } from 'element-plus/es/components/button/index.mjs'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElTable, ElTableColumn } from 'element-plus/es/components/table/index.mjs'
import TablePagination from '../components/common/TablePagination.vue'
import {
  fetchDetectionParametersApi,
  fetchDetectionsApi,
  fetchDetectionTypesApi,
  fetchSamplesApi,
  submitDetectionApi
} from '../api/lab'
import {
  abnormalDetectionResult,
  approvedDetectionStatus,
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

const route = useRoute()
const router = useRouter()

const WAITING_DETECTION_STATUS = 'WAITING'
const WAITING_DETECTION_LABEL = '待检测'
const MAX_LOAD_SIZE = 500

const query = reactive({ pageNum: 1, pageSize: DEFAULT_PAGE_SIZE })
const records = ref([])
const samplePool = ref([])
const activeStatKey = ref('all')

function toSafeNumber(value) {
  const num = typeof value === 'number' ? value : Number.parseFloat(String(value ?? '').replace(/,/g, '').trim())
  return Number.isFinite(num) ? num : 0
}

function parseDetectionItemsText(value) {
  return String(value || '')
    .split(/[，,、]/)
    .map((item) => item.trim())
    .filter(Boolean)
}

function buildPendingSampleRecord(sample) {
  return {
    id: `pending-${sample.id}`,
    sampleId: sample.id,
    sampleNo: sample.sampleNo,
    sealNo: sample.sealNo,
    detectionTypeName: sample.detectionItems || '待分配检测项目',
    detectorName: '待分配',
    detectionResult: null,
    detectionStatus: WAITING_DETECTION_STATUS,
    detectionTime: sample.sealTime || sample.samplingTime || '',
    abnormalRemark: sample.remark || '',
    __pendingSample: true
  }
}

function compareByTimeDesc(left, right) {
  return String(right.detectionTime || '').localeCompare(String(left.detectionTime || ''))
}

const sceneMap = {
  '/detection-analysis': {
    key: 'detection-analysis',
    title: '检测分析',
    subtitle: '承接样品登录后的待检测样品与已提交检测记录，支持快速完成结果录入。',
    tableTitle: '检测处理清单',
    tableSubtitle: '样品登录完成后会直接进入这里，检测人员可继续提交检测结果。',
    note: '当前页会同时展示待检测样品和已提交检测记录，避免样品登录后“有数量无列表”。',
    guide: '如果没有可提交样品，请先在样品登录页完成样品登记；检测提交后可直接流转到结果审查。',
    defaultStatKey: 'all',
    allowSubmit: true,
    includePendingSamples: true,
    emptyText: '暂无检测分析数据',
    recordFilter: () => true,
    quickLinks: [
      { path: '/sample-login', label: '样品登录', desc: '先完成样品登记，再进入检测分析' },
      { path: '/review-result', label: '结果审查', desc: '检测提交后进入审核闭环' },
      { path: '/detection-history', label: '历史检测', desc: '查看已处理检测记录与异常回函' }
    ]
  },
  '/detection-history': {
    key: 'detection-history',
    title: '历史检测',
    subtitle: '回看已经完成提交的检测记录，重点查看已通过、已驳回与异常结果。',
    tableTitle: '历史检测记录',
    tableSubtitle: '本页只展示已经进入检测流程的历史记录，不再承接新的待检样品。',
    note: '历史检测页强调回溯，不承担当前处理动作，避免把历史页做成重复入口。',
    guide: '如果需要继续追踪驳回样品，可跳往历史审查页；若要看全量记录，请进入检测台账。',
    defaultStatKey: 'approved',
    allowSubmit: false,
    includePendingSamples: false,
    emptyText: '暂无历史检测数据',
    recordFilter: (item) =>
      [approvedDetectionStatus, rejectedDetectionStatus].includes(item.detectionStatus)
      || item.detectionResult === abnormalDetectionResult,
    quickLinks: [
      { path: '/review-history', label: '历史审查', desc: '继续查看检测结果的审核处理情况' },
      { path: '/detection-ledger', label: '检测台账', desc: '切换到全量检测清单视角' },
      { path: '/report-ledger', label: '报告台账', desc: '核对检测结果是否进入正式报告' }
    ]
  },
  '/detection-ledger': {
    key: 'detection-ledger',
    title: '检测台账',
    subtitle: '集中查看全部检测记录，同时纳入已登录未检测样品，便于统一盘点。',
    tableTitle: '检测全量台账',
    tableSubtitle: '保留全量检测记录，并补充待检测样品视角，更适合管理与盘点。',
    note: '检测台账页会把已登录但尚未提交检测的样品一并展示，避免流程断点。',
    guide: '如需继续处理待检测样品，可回到检测分析页；如需继续审查，可前往结果审查。',
    defaultStatKey: 'all',
    allowSubmit: false,
    includePendingSamples: true,
    emptyText: '暂无检测台账数据',
    recordFilter: () => true,
    quickLinks: [
      { path: '/detection-analysis', label: '检测分析', desc: '返回当前检测处理入口' },
      { path: '/review-result', label: '结果审查', desc: '处理待审核检测结果' },
      { path: '/sample-ledger', label: '样品台账', desc: '回看检测记录对应的样品来源' }
    ]
  }
}

const baseScene = computed(() => sceneMap[route.path] || sceneMap['/detection-analysis'])
const pendingSampleRecords = computed(() => (
  samplePool.value
    .filter((item) => availableDetectionSampleStatuses.includes(item.sampleStatus))
    .map(buildPendingSampleRecord)
    .sort(compareByTimeDesc)
))
const sourceRecords = computed(() => {
  const detectionRecords = [...records.value].sort(compareByTimeDesc)
  if (!baseScene.value.includePendingSamples) {
    return detectionRecords
  }
  return [...pendingSampleRecords.value, ...detectionRecords].sort(compareByTimeDesc)
})
const sceneRecords = computed(() => sourceRecords.value.filter((item) => baseScene.value.recordFilter(item)))
const submittableSamples = computed(() => (
  samplePool.value.filter((item) => availableDetectionSampleStatuses.includes(item.sampleStatus))
))

const currentScene = computed(() => ({
  ...baseScene.value,
  tags: [
    {
      label: '待审查',
      value: sceneRecords.value.filter((item) => item.detectionStatus === reviewPendingDetectionStatus).length,
      type: 'warning'
    },
    {
      label: '已驳回',
      value: sceneRecords.value.filter((item) => item.detectionStatus === rejectedDetectionStatus).length,
      type: sceneRecords.value.some((item) => item.detectionStatus === rejectedDetectionStatus) ? 'danger' : 'success'
    },
    {
      label: '待检测样品',
      value: pendingSampleRecords.value.length,
      type: pendingSampleRecords.value.length ? 'info' : 'success'
    }
  ]
}))

const currentStats = computed(() => [
  {
    key: 'all',
    label: baseScene.value.key === 'detection-ledger' ? '台账总量' : '检测总览',
    value: sceneRecords.value.length,
    desc: baseScene.value.key === 'detection-ledger' ? '检测台账总量' : '当前场景已加载的检测清单'
  },
  {
    key: 'pendingReview',
    label: '待审查',
    value: sceneRecords.value.filter((item) => item.detectionStatus === reviewPendingDetectionStatus).length,
    desc: '已提交但尚未审核的检测记录'
  },
  {
    key: 'approved',
    label: '已通过',
    value: sceneRecords.value.filter((item) => item.detectionStatus !== reviewPendingDetectionStatus && item.detectionStatus !== rejectedDetectionStatus && item.detectionStatus !== WAITING_DETECTION_STATUS).length,
    desc: '已审核通过的检测记录'
  },
  {
    key: 'rejected',
    label: '已驳回',
    value: sceneRecords.value.filter((item) => item.detectionStatus === rejectedDetectionStatus).length,
    desc: '已被驳回并等待补检的检测记录'
  },
  {
    key: 'abnormal',
    label: '异常结果',
    value: sceneRecords.value.filter((item) => item.detectionResult === abnormalDetectionResult).length,
    desc: '检测结果判定为异常的记录'
  },
  {
    key: 'todo-sample',
    label: '待检测样品',
    value: pendingSampleRecords.value.length,
    desc: '样品已登录但尚未提交检测的待处理样品'
  }
])

const filteredRecords = computed(() => {
  if (activeStatKey.value === 'pendingReview') {
    return sceneRecords.value.filter((item) => item.detectionStatus === reviewPendingDetectionStatus)
  }
  if (activeStatKey.value === 'approved') {
    return sceneRecords.value.filter((item) => (
      item.detectionStatus !== reviewPendingDetectionStatus
      && item.detectionStatus !== rejectedDetectionStatus
      && item.detectionStatus !== WAITING_DETECTION_STATUS
    ))
  }
  if (activeStatKey.value === 'rejected') {
    return sceneRecords.value.filter((item) => item.detectionStatus === rejectedDetectionStatus)
  }
  if (activeStatKey.value === 'abnormal') {
    return sceneRecords.value.filter((item) => item.detectionResult === abnormalDetectionResult)
  }
  if (activeStatKey.value === 'todo-sample') {
    return sceneRecords.value.filter((item) => item.detectionStatus === WAITING_DETECTION_STATUS)
  }
  return sceneRecords.value
})

const displayTotal = computed(() => filteredRecords.value.length)
const pagedRecords = computed(() => {
  const start = (query.pageNum - 1) * query.pageSize
  return filteredRecords.value.slice(start, start + query.pageSize)
})

function getDetectionStatusLabel(status) {
  return status === WAITING_DETECTION_STATUS
    ? WAITING_DETECTION_LABEL
    : getEnumLabel(detectionStatusLabelMap, status)
}

function getDetectionStatusClass(status) {
  return status === WAITING_DETECTION_STATUS ? 'info' : getStatusClass('detectionStatus', status)
}

function getDetectionResultLabel(result) {
  return result ? getEnumLabel(detectionResultLabelMap, result) : '-'
}

function getDetectionResultClass(result) {
  return result ? getStatusClass('detectionResult', result) : 'info'
}

function handleStatClick(key) {
  activeStatKey.value = activeStatKey.value === key ? baseScene.value.defaultStatKey : key
  query.pageNum = 1
}

function syncRouteState() {
  activeStatKey.value = baseScene.value.defaultStatKey
  query.pageNum = 1
}

function goRoute(path) {
  if (!path) {
    return
  }
  router.push(path)
}

async function loadData() {
  const [detectionResult, sampleResult] = await Promise.all([
    fetchDetectionsApi({ pageNum: 1, pageSize: MAX_LOAD_SIZE }),
    fetchSamplesApi({ pageNum: 1, pageSize: MAX_LOAD_SIZE })
  ])
  records.value = detectionResult.records || []
  samplePool.value = sampleResult.records || []

  const maxPage = Math.max(1, Math.ceil(displayTotal.value / query.pageSize))
  if (query.pageNum > maxPage) {
    query.pageNum = 1
  }
}

async function submitDemo() {
  const availableSamples = submittableSamples.value
  const sample = availableSamples.find((item) => item.sampleStatus === retestSampleStatus)
    || availableSamples.find((item) => item.sampleStatus === loggedSampleStatus)

  if (!sample) {
    ElMessage.warning('请先准备一条已登录或待重检的样品。')
    return
  }

  const typeResult = await fetchDetectionTypesApi({ pageNum: 1, pageSize: 100 })
  const enabledTypes = (typeResult.records || []).filter((item) => item.enabled === 1)
  const expectedTypeNames = parseDetectionItemsText(sample.detectionItems)
  const detectionType = enabledTypes.find((item) => expectedTypeNames.includes(item.typeName)) || enabledTypes[0]
  if (!detectionType) {
    ElMessage.warning('请先启用一项检测类别配置。')
    return
  }

  const parameterIdList = String(detectionType.parameterIds || '')
    .split(',')
    .map((item) => Number(item.trim()))
    .filter((item) => Number.isFinite(item))

  if (!parameterIdList.length) {
    ElMessage.warning('当前检测类别尚未配置检测参数。')
    return
  }

  const parameterResult = await fetchDetectionParametersApi({ pageNum: 1, pageSize: 200 })
  const parameterMap = new Map((parameterResult.records || []).map((item) => [item.id, item]))
  const configuredParameters = parameterIdList
    .map((id) => parameterMap.get(id))
    .filter((item) => item && item.enabled === 1)

  if (configuredParameters.length !== parameterIdList.length) {
    ElMessage.warning('当前检测类别存在未启用或缺失的检测参数，请先修正配置。')
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

  ElMessage.success(
    sample.sampleStatus === retestSampleStatus
      ? '重检记录已提交，样品重新进入审核流程。'
      : '检测记录已提交。'
  )
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

onMounted(async () => {
  syncRouteState()
  await loadData()
})

watch(() => route.fullPath, () => {
  syncRouteState()
})
</script>

<style scoped>
.detection-page {
  gap: 16px;
}

.page-hero,
.scene-grid {
  display: grid;
  gap: 16px;
}

.page-hero {
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
}

.hero-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: flex-end;
}

.metric-card--action,
.quick-link {
  width: 100%;
  text-align: left;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.metric-card--action:hover,
.metric-card--action:focus-visible,
.metric-card--action.is-active,
.quick-link:hover,
.quick-link:focus-visible {
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

.scene-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.scene-copy {
  display: grid;
  gap: 10px;
  color: var(--text-sub);
  font-size: 14px;
  line-height: 1.7;
}

.scene-copy p {
  margin: 0;
}

.quick-links {
  display: grid;
  gap: 12px;
}

.quick-link {
  display: grid;
  gap: 4px;
  padding: 14px;
  border: 1px solid var(--line-soft);
  border-radius: 12px;
  background: var(--bg-panel-soft);
}

.quick-link strong {
  color: var(--text-main);
  font-size: 14px;
  line-height: 1.5;
}

.quick-link span {
  color: var(--text-sub);
  font-size: 13px;
  line-height: 1.6;
}

@media (max-width: 900px) {
  .page-hero,
  .scene-grid {
    grid-template-columns: 1fr;
  }

  .hero-tags {
    justify-content: flex-start;
  }
}
</style>
