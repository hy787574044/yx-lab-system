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
        <el-table class="list-table" :data="visibleRecords" stripe max-height="460" :empty-text="baseScene.emptyText">
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
          <el-table-column label="检测状态" width="120" header-cell-class-name="cell-center" class-name="cell-center">
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

    <section class="scene-grid">
      <div class="glass-panel section-block">
        <div class="section-head">
          <h3 class="section-title">页面说明</h3>
        </div>
        <div class="scene-copy">
          <p>{{ baseScene.guide }}</p>
          <p>当前检测页面已经区分“当前处理、历史追溯、全量台账”三种使用意图。</p>
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
import { ElMessage } from 'element-plus'
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

const query = reactive({ pageNum: 1, pageSize: DEFAULT_PAGE_SIZE })
const records = ref([])
const samplePool = ref([])
const total = ref(0)
const activeStatKey = ref('all')

function toSafeNumber(value) {
  const num = typeof value === 'number' ? value : Number.parseFloat(String(value ?? '').replace(/,/g, '').trim())
  return Number.isFinite(num) ? num : 0
}

const sceneMap = {
  '/detection-analysis': {
    key: 'detection-analysis',
    title: '检测分析',
    subtitle: '聚焦待检测与待审核样品，支持快速提交检测结果，承接样品登录后的实验室分析环节。',
    tableTitle: '当前检测记录',
    tableSubtitle: '默认聚焦待审核与异常结果，可快速验证检测配置约束与检测提交流程。',
    note: '检测分析页保留“提交演示检测”动作，适合作为当前处理入口。',
    guide: '如果没有可提交样品，请先在样品登录页完成样品登记；提交后可直接流转到结果审查。',
    defaultStatKey: 'pendingReview',
    allowSubmit: true,
    emptyText: '暂无检测分析数据',
    recordFilter: () => true,
    quickLinks: [
      { path: '/sample-login', label: '样品登录', desc: '先完成样品登记，再进入检测分析' },
      { path: '/review-result', label: '结果审查', desc: '检测提交后进入审核闭环' },
      { path: '/detection-history', label: '历史检测', desc: '查看已处理检测记录与异常回溯' }
    ]
  },
  '/detection-history': {
    key: 'detection-history',
    title: '历史检测',
    subtitle: '回看已经完成提交的检测记录，重点查看已通过、已驳回与异常结果，服务复盘与追溯。',
    tableTitle: '历史检测记录',
    tableSubtitle: '本页自动聚焦已处理历史记录，不再提供新的检测提交动作。',
    note: '历史检测页强调回溯，不承担当前处理动作，避免把历史页做成重复入口。',
    guide: '如果需要继续追踪驳回样品，可跳往历史审查页；若要看全量记录，请进入检测台账。',
    defaultStatKey: 'approved',
    allowSubmit: false,
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
    subtitle: '集中查看全部检测记录，适合全量核对检测状态、异常结果与审核流转关系。',
    tableTitle: '检测全量台账',
    tableSubtitle: '保留全量检测记录，不强调当前提交动作，更适合管理与盘点。',
    note: '检测台账页用于全量盘点，可按状态和结果快速切换视角。',
    guide: '如需继续处理待审核检测，可前往结果审查；如需补做检测，可回到检测分析页。',
    defaultStatKey: 'all',
    allowSubmit: false,
    emptyText: '暂无检测台账数据',
    recordFilter: () => true,
    quickLinks: [
      { path: '/detection-analysis', label: '检测分析', desc: '返回当前检测处理入口' },
      { path: '/review-result', label: '结果审查', desc: '处理待审核检测结果' },
      { path: '/sample-ledger', label: '样品台账', desc: '回溯检测记录对应的样品来源' }
    ]
  }
}

const baseScene = computed(() => sceneMap[route.path] || sceneMap['/detection-analysis'])
const sceneRecords = computed(() => records.value.filter((item) => baseScene.value.recordFilter(item)))
const submittableSamples = computed(() =>
  samplePool.value.filter((item) => availableDetectionSampleStatuses.includes(item.sampleStatus))
)

const currentScene = computed(() => ({
  ...baseScene.value,
  tags: [
    {
      label: '待审核',
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
      value: submittableSamples.value.length,
      type: submittableSamples.value.length ? 'info' : 'success'
    }
  ]
}))

const currentStats = computed(() => [
  {
    key: 'all',
    label: baseScene.value.key === 'detection-ledger' ? '台账总量' : '检测总览',
    value: baseScene.value.key === 'detection-ledger' ? toSafeNumber(total.value) : sceneRecords.value.length,
    desc: baseScene.value.key === 'detection-ledger' ? '检测台账总量' : '当前场景已加载的检测记录'
  },
  {
    key: 'pendingReview',
    label: '待审核',
    value: sceneRecords.value.filter((item) => item.detectionStatus === reviewPendingDetectionStatus).length,
    desc: '已提交但尚未审核的检测记录'
  },
  {
    key: 'approved',
    label: '已通过',
    value: sceneRecords.value.filter((item) => item.detectionStatus !== reviewPendingDetectionStatus && item.detectionStatus !== rejectedDetectionStatus).length,
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
    value: submittableSamples.value.length,
    desc: '样品台账中仍可发起检测提交的样品'
  }
])

const visibleRecords = computed(() => {
  if (activeStatKey.value === 'pendingReview') {
    return sceneRecords.value.filter((item) => item.detectionStatus === reviewPendingDetectionStatus)
  }
  if (activeStatKey.value === 'approved') {
    return sceneRecords.value.filter((item) => item.detectionStatus !== reviewPendingDetectionStatus && item.detectionStatus !== rejectedDetectionStatus)
  }
  if (activeStatKey.value === 'rejected') {
    return sceneRecords.value.filter((item) => item.detectionStatus === rejectedDetectionStatus)
  }
  if (activeStatKey.value === 'abnormal') {
    return sceneRecords.value.filter((item) => item.detectionResult === abnormalDetectionResult)
  }
  return sceneRecords.value
})

function handleStatClick(key) {
  activeStatKey.value = activeStatKey.value === key ? baseScene.value.defaultStatKey : key
}

function syncRouteState() {
  activeStatKey.value = baseScene.value.defaultStatKey
}

function goRoute(path) {
  if (!path) {
    return
  }
  router.push(path)
}

async function loadData() {
  const [detectionResult, sampleResult] = await Promise.all([
    fetchDetectionsApi(query),
    fetchSamplesApi({ pageNum: 1, pageSize: 200 })
  ])
  records.value = detectionResult.records || []
  total.value = toSafeNumber(detectionResult.total)
  samplePool.value = sampleResult.records || []
}

async function submitDemo() {
  const availableSamples = submittableSamples.value
  const sample = availableSamples.find((item) => item.sampleStatus === retestSampleStatus)
    || availableSamples.find((item) => item.sampleStatus === loggedSampleStatus)

  if (!sample) {
    ElMessage.warning('请先准备一条已登记或待重检的样品。')
    return
  }

  const typeResult = await fetchDetectionTypesApi({ pageNum: 1, pageSize: 100 })
  const detectionType = typeResult.records?.find((item) => item.enabled === 1)
  if (!detectionType) {
    ElMessage.warning('请先启用一项检测类型配置。')
    return
  }

  const parameterIdList = String(detectionType.parameterIds || '')
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
