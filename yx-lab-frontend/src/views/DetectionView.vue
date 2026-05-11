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
            <el-button type="primary" @click="loadData">刷新检测流程</el-button>
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
        <el-table
          class="list-table"
          :data="pagedRecords"
          stripe
          row-key="id"
          max-height="560"
          :empty-text="baseScene.emptyText"
          @expand-change="handleExpandChange"
        >
          <el-table-column type="expand" width="56">
            <template #default="{ row }">
              <div v-if="row.__sampleOnly" class="subflow-empty">
                <strong>该样品为历史旧数据</strong>
                <p>当前只保留了样品主信息，尚未生成正式的参数子流程分配数据。建议重新登录该样品后再进行参数级分配。</p>
              </div>

              <div v-else-if="detailLoadingMap[row.id]" class="subflow-empty">
                正在加载参数子流程...
              </div>

              <div v-else-if="getDetailItems(row.id).length" class="subflow-panel">
                <div class="subflow-head">
                  <div class="subflow-summary">
                    <span class="status-chip info">参数 {{ getDetailItems(row.id).length }}</span>
                    <span class="status-chip success">已分配 {{ countAssignedItems(row.id) }}</span>
                    <span class="status-chip warning">待分配 {{ getDetailItems(row.id).length - countAssignedItems(row.id) }}</span>
                  </div>
                  <el-button
                    v-if="canAssignRow(row)"
                    type="primary"
                    @click="saveAssignments(row)"
                  >
                    保存人员分配
                  </el-button>
                </div>

                <div class="subflow-list">
                  <article
                    v-for="item in getDetailItems(row.id)"
                    :key="item.id"
                    class="subflow-card"
                  >
                    <div class="subflow-card__head">
                      <div>
                        <strong>{{ item.parameterName }}</strong>
                        <p>{{ item.methodName || '未绑定检测方法' }}</p>
                      </div>
                      <span class="status-chip" :class="getItemStatusClass(item.itemStatus)">
                        {{ getItemStatusLabel(item.itemStatus) }}
                      </span>
                    </div>

                    <div class="subflow-meta">
                      <span>标准范围：{{ formatStandardRange(item.standardMin, item.standardMax, item.unit) }}</span>
                      <span>参考范围：{{ item.referenceStandard || '-' }}</span>
                    </div>

                    <div class="subflow-assign">
                      <label>检测员分配</label>
                      <el-select
                        :model-value="getAssignedDetectorId(row.id, item.id)"
                        :disabled="!canAssignRow(row)"
                        clearable
                        filterable
                        placeholder="请选择检测员"
                        @update:model-value="updateAssignedDetectorId(row.id, item.id, $event)"
                      >
                        <el-option
                          v-for="option in detectorOptions"
                          :key="option.id"
                          :label="option.displayName || option.realName || option.username"
                          :value="option.id"
                        />
                      </el-select>
                    </div>
                  </article>
                </div>
              </div>

              <div v-else class="subflow-empty">
                当前主流程下暂无参数子流程数据。
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="sampleNo" label="样品编号" min-width="170" />
          <el-table-column prop="sealNo" label="封签编号" min-width="170" />
          <el-table-column prop="detectionTypeName" label="检测套餐" min-width="180" />
          <el-table-column label="参数进度" min-width="150">
            <template #default="{ row }">
              {{ formatProgress(row) }}
            </template>
          </el-table-column>
          <el-table-column label="检测人员" min-width="120">
            <template #default="{ row }">
              {{ row.detectorName || (row.detectionStatus === WAIT_ASSIGN_STATUS ? '待分配' : '-') }}
            </template>
          </el-table-column>
          <el-table-column label="检测结果" width="120" header-cell-class-name="cell-center" class-name="cell-center">
            <template #default="{ row }">
              <span class="status-chip" :class="getDetectionResultClass(row.detectionResult)">
                {{ getDetectionResultLabel(row.detectionResult) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="流程状态" width="120" header-cell-class-name="cell-center" class-name="cell-center">
            <template #default="{ row }">
              <span class="status-chip" :class="getDetectionStatusClass(row.detectionStatus)">
                {{ getDetectionStatusLabel(row.detectionStatus) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="detectionTime" label="流程时间" width="170" />
          <el-table-column prop="abnormalRemark" label="说明" min-width="220" show-overflow-tooltip />
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
          <p>当前页面已经按“检测主流程 + 参数子流程”展开，样品登录后会先进入待分配，再按参数逐条分配检测员。</p>
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
import { ElOption, ElSelect } from 'element-plus/es/components/select/index.mjs'
import { ElTable, ElTableColumn } from 'element-plus/es/components/table/index.mjs'
import TablePagination from '../components/common/TablePagination.vue'
import {
  assignDetectionDetectorsApi,
  fetchDetectionDetailApi,
  fetchDetectionDetectorsApi,
  fetchDetectionsApi,
  fetchSamplesApi
} from '../api/lab'
import {
  abnormalDetectionResult,
  approvedDetectionStatus,
  DEFAULT_PAGE_SIZE,
  detectionResultLabelMap,
  detectionStatusLabelMap,
  getEnumLabel,
  getStatusClass,
  rejectedDetectionStatus,
  reviewPendingDetectionStatus,
  availableDetectionSampleStatuses
} from '../utils/labEnums'

const route = useRoute()
const router = useRouter()

const WAIT_ASSIGN_STATUS = 'WAIT_ASSIGN'
const WAIT_DETECT_STATUS = 'WAIT_DETECT'
const MAX_LOAD_SIZE = 500

const query = reactive({ pageNum: 1, pageSize: DEFAULT_PAGE_SIZE })
const records = ref([])
const samplePool = ref([])
const detectorOptions = ref([])
const activeStatKey = ref('all')
const detailMap = reactive({})
const detailLoadingMap = reactive({})
const assignmentMap = reactive({})

function parseSampleConfigSnapshot(snapshotText) {
  if (!snapshotText) {
    return []
  }
  try {
    const parsed = JSON.parse(snapshotText)
    return Array.isArray(parsed) ? parsed.filter((item) => item && item.parameterId) : []
  } catch {
    return []
  }
}

function buildPendingSampleRecord(sample) {
  const configItems = parseSampleConfigSnapshot(sample.detectionConfigSnapshot)
  return {
    id: `pending-sample-${sample.id}`,
    sampleId: sample.id,
    sampleNo: sample.sampleNo,
    sealNo: sample.sealNo,
    detectionTypeName: sample.detectionTypeName || sample.detectionItems || '待分配检测套餐',
    detectorName: '',
    detectionResult: null,
    detectionStatus: WAIT_ASSIGN_STATUS,
    detectionTime: sample.sealTime || sample.samplingTime || '',
    abnormalRemark: '历史样品未自动生成参数子流程',
    parameterCount: configItems.length,
    assignedCount: 0,
    completedCount: 0,
    __sampleOnly: true
  }
}

function compareByTimeDesc(left, right) {
  return String(right.detectionTime || '').localeCompare(String(left.detectionTime || ''))
}

const sceneMap = {
  '/detection-analysis': {
    key: 'detection-analysis',
    title: '检测分析',
    subtitle: '样品登录后先进入待分配检测流程，再按套餐参数拆分为多个待分配的检测子流程。',
    tableTitle: '检测流程队列',
    tableSubtitle: '展开每条主流程，即可查看套餐参数列表并逐条分配检测员。',
    note: '当前页重点处理“待分配”和“待检测”两个阶段，分配完成后即可继续进入检测执行。',
    guide: '建议先在本页完成参数级人员分配，再由检测员按各自分工执行后续检测录入。',
    defaultStatKey: 'all',
    emptyText: '暂无检测分析数据',
    allowAssign: true,
    includePendingFallback: true,
    recordFilter: (item) => [WAIT_ASSIGN_STATUS, WAIT_DETECT_STATUS, reviewPendingDetectionStatus].includes(item.detectionStatus),
    quickLinks: [
      { path: '/sample-login', label: '样品登录', desc: '先完成样品登录，再进入检测分析分配检测员' },
      { path: '/review-result', label: '结果审查', desc: '检测完成提交后进入结果审查闭环' },
      { path: '/detection-history', label: '历史检测', desc: '查看已完成检测与驳回重检记录' }
    ]
  },
  '/detection-history': {
    key: 'detection-history',
    title: '历史检测',
    subtitle: '回看已进入审查或已完成闭环的检测记录，重点核对异常结果与驳回重检情况。',
    tableTitle: '历史检测记录',
    tableSubtitle: '本页只保留历史追溯视角，不再承担当前待办分配动作。',
    note: '历史页仅用于追溯检测链路，不再展示当前需要人员分配的流程。',
    guide: '如需处理新的样品检测，请返回检测分析页；如需继续追踪审查结论，请前往历史审查。',
    defaultStatKey: 'approved',
    emptyText: '暂无历史检测数据',
    allowAssign: false,
    includePendingFallback: false,
    recordFilter: (item) =>
      [approvedDetectionStatus, rejectedDetectionStatus].includes(item.detectionStatus)
      || item.detectionResult === abnormalDetectionResult,
    quickLinks: [
      { path: '/review-history', label: '历史审查', desc: '继续查看检测结果的审查处理结论' },
      { path: '/detection-ledger', label: '检测台账', desc: '切换到全量检测主流程台账视角' },
      { path: '/report-ledger', label: '报告台账', desc: '查看检测结果是否已沉淀为正式报告' }
    ]
  },
  '/detection-ledger': {
    key: 'detection-ledger',
    title: '检测台账',
    subtitle: '统一查看全部检测主流程，同时保留参数子流程与人员分配的完整留痕。',
    tableTitle: '检测全量台账',
    tableSubtitle: '适合管理端统一核对各状态主流程及其参数拆分情况。',
    note: '台账页保留全部状态，既能看当前待办，也能看已提交、已审查与已驳回记录。',
    guide: '如需实际执行分配动作，建议返回检测分析页操作；台账页更适合总览与核对。',
    defaultStatKey: 'all',
    emptyText: '暂无检测台账数据',
    allowAssign: false,
    includePendingFallback: true,
    recordFilter: () => true,
    quickLinks: [
      { path: '/detection-analysis', label: '检测分析', desc: '回到当前待分配和待检测主流程' },
      { path: '/review-result', label: '结果审查', desc: '处理已经提交完成的检测结果' },
      { path: '/sample-ledger', label: '样品台账', desc: '回看检测流程对应的样品来源信息' }
    ]
  }
}

const baseScene = computed(() => sceneMap[route.path] || sceneMap['/detection-analysis'])

const pendingSampleRecords = computed(() => {
  if (!baseScene.value.includePendingFallback) {
    return []
  }
  const existingSampleIds = new Set(records.value.map((item) => item.sampleId))
  return samplePool.value
    .filter((item) => availableDetectionSampleStatuses.includes(item.sampleStatus))
    .filter((item) => !existingSampleIds.has(item.id))
    .map(buildPendingSampleRecord)
    .sort(compareByTimeDesc)
})

const sourceRecords = computed(() => {
  const actualRecords = [...records.value].sort(compareByTimeDesc)
  if (!baseScene.value.includePendingFallback) {
    return actualRecords
  }
  return [...actualRecords, ...pendingSampleRecords.value].sort(compareByTimeDesc)
})

const sceneRecords = computed(() => sourceRecords.value.filter((item) => baseScene.value.recordFilter(item)))

const currentScene = computed(() => ({
  ...baseScene.value,
  tags: [
    {
      label: '待分配',
      value: sceneRecords.value.filter((item) => item.detectionStatus === WAIT_ASSIGN_STATUS).length,
      type: sceneRecords.value.some((item) => item.detectionStatus === WAIT_ASSIGN_STATUS) ? 'warning' : 'success'
    },
    {
      label: '待检测',
      value: sceneRecords.value.filter((item) => item.detectionStatus === WAIT_DETECT_STATUS).length,
      type: sceneRecords.value.some((item) => item.detectionStatus === WAIT_DETECT_STATUS) ? 'info' : 'success'
    },
    {
      label: '待审查',
      value: sceneRecords.value.filter((item) => item.detectionStatus === reviewPendingDetectionStatus).length,
      type: sceneRecords.value.some((item) => item.detectionStatus === reviewPendingDetectionStatus) ? 'danger' : 'success'
    }
  ]
}))

const currentStats = computed(() => [
  {
    key: 'all',
    label: baseScene.value.key === 'detection-ledger' ? '台账总量' : '流程总量',
    value: sceneRecords.value.length,
    desc: baseScene.value.key === 'detection-ledger' ? '当前检测台账主流程总数' : '当前场景下可见的检测主流程总数'
  },
  {
    key: 'waitAssign',
    label: '待分配',
    value: sceneRecords.value.filter((item) => item.detectionStatus === WAIT_ASSIGN_STATUS).length,
    desc: '样品已进入检测分析，但参数子流程尚未完成检测员分配'
  },
  {
    key: 'waitDetect',
    label: '待检测',
    value: sceneRecords.value.filter((item) => item.detectionStatus === WAIT_DETECT_STATUS).length,
    desc: '参数子流程已完成检测员分配，等待检测执行'
  },
  {
    key: 'pendingReview',
    label: '待审查',
    value: sceneRecords.value.filter((item) => item.detectionStatus === reviewPendingDetectionStatus).length,
    desc: '检测结果已提交，等待进入审查闭环'
  },
  {
    key: 'approved',
    label: '已通过',
    value: sceneRecords.value.filter((item) => item.detectionStatus === approvedDetectionStatus).length,
    desc: '检测审查已通过，可继续进入报告正式产物环节'
  },
  {
    key: 'rejected',
    label: '已驳回',
    value: sceneRecords.value.filter((item) => item.detectionStatus === rejectedDetectionStatus).length,
    desc: '检测审查已驳回，样品重新回到重检待办链路'
  }
])

const filteredRecords = computed(() => {
  if (activeStatKey.value === 'waitAssign') {
    return sceneRecords.value.filter((item) => item.detectionStatus === WAIT_ASSIGN_STATUS)
  }
  if (activeStatKey.value === 'waitDetect') {
    return sceneRecords.value.filter((item) => item.detectionStatus === WAIT_DETECT_STATUS)
  }
  if (activeStatKey.value === 'pendingReview') {
    return sceneRecords.value.filter((item) => item.detectionStatus === reviewPendingDetectionStatus)
  }
  if (activeStatKey.value === 'approved') {
    return sceneRecords.value.filter((item) => item.detectionStatus === approvedDetectionStatus)
  }
  if (activeStatKey.value === 'rejected') {
    return sceneRecords.value.filter((item) => item.detectionStatus === rejectedDetectionStatus)
  }
  return sceneRecords.value
})

const displayTotal = computed(() => filteredRecords.value.length)

const pagedRecords = computed(() => {
  const start = (query.pageNum - 1) * query.pageSize
  return filteredRecords.value.slice(start, start + query.pageSize)
})

function getDetectionStatusLabel(status) {
  if (status === WAIT_ASSIGN_STATUS) {
    return '待分配'
  }
  if (status === WAIT_DETECT_STATUS) {
    return '待检测'
  }
  return getEnumLabel(detectionStatusLabelMap, status)
}

function getDetectionStatusClass(status) {
  if (status === WAIT_ASSIGN_STATUS) {
    return 'warning'
  }
  if (status === WAIT_DETECT_STATUS) {
    return 'info'
  }
  return getStatusClass('detectionStatus', status)
}

function getItemStatusLabel(status) {
  return getDetectionStatusLabel(status)
}

function getItemStatusClass(status) {
  return getDetectionStatusClass(status)
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

function formatStandardRange(min, max, unit) {
  const suffix = unit ? ` ${unit}` : ''
  if (min != null && max != null) {
    return `${min} - ${max}${suffix}`
  }
  if (min != null) {
    return `>= ${min}${suffix}`
  }
  if (max != null) {
    return `<= ${max}${suffix}`
  }
  return `未设置${suffix}`
}

function formatProgress(row) {
  const total = Number(row.parameterCount || 0)
  const assigned = Number(row.assignedCount || 0)
  const completed = Number(row.completedCount || 0)
  if (!total) {
    return '-'
  }
  return `${assigned}/${total} 已分配，${completed}/${total} 已提交`
}

function canAssignRow(row) {
  return !row.__sampleOnly
    && baseScene.value.allowAssign
    && [WAIT_ASSIGN_STATUS, WAIT_DETECT_STATUS].includes(row.detectionStatus)
}

function getDetailItems(recordId) {
  return detailMap[recordId]?.items || []
}

function countAssignedItems(recordId) {
  return getDetailItems(recordId).filter((item) => {
    const currentValue = assignmentMap[recordId]?.[item.id]
    return currentValue !== null && currentValue !== undefined && currentValue !== ''
  }).length
}

function ensureAssignmentState(recordId, items) {
  assignmentMap[recordId] = items.reduce((result, item) => {
    result[item.id] = item.detectorId ?? null
    return result
  }, {})
}

function getAssignedDetectorId(recordId, itemId) {
  return assignmentMap[recordId]?.[itemId] ?? null
}

function updateAssignedDetectorId(recordId, itemId, value) {
  if (!assignmentMap[recordId]) {
    assignmentMap[recordId] = {}
  }
  assignmentMap[recordId][itemId] = value ?? null
}

async function loadRecordDetail(recordId, force = false) {
  if (!recordId || recordId.toString().startsWith('pending-sample-')) {
    return
  }
  if (!force && detailMap[recordId]) {
    return
  }
  detailLoadingMap[recordId] = true
  try {
    const detail = await fetchDetectionDetailApi(recordId)
    detailMap[recordId] = detail || { items: [] }
    ensureAssignmentState(recordId, detailMap[recordId].items || [])
  } finally {
    detailLoadingMap[recordId] = false
  }
}

async function handleExpandChange(row, expandedRows) {
  if (row.__sampleOnly) {
    return
  }
  const expanded = expandedRows.some((item) => item.id === row.id)
  if (expanded) {
    await loadRecordDetail(row.id)
  }
}

async function saveAssignments(row) {
  const detailItems = getDetailItems(row.id)
  if (!detailItems.length) {
    ElMessage.warning('当前主流程下没有可分配的参数子流程')
    return
  }
  await assignDetectionDetectorsApi(row.id, {
    items: detailItems.map((item) => ({
      itemId: item.id,
      detectorId: getAssignedDetectorId(row.id, item.id)
    }))
  })
  ElMessage.success('检测员分配已保存')
  await Promise.all([
    loadRecordDetail(row.id, true),
    loadData()
  ])
}

async function loadData() {
  const [detectionResult, sampleResult, detectorResult] = await Promise.all([
    fetchDetectionsApi({ pageNum: 1, pageSize: MAX_LOAD_SIZE }),
    fetchSamplesApi({ pageNum: 1, pageSize: MAX_LOAD_SIZE }),
    fetchDetectionDetectorsApi()
  ])

  records.value = detectionResult.records || []
  samplePool.value = sampleResult.records || []
  detectorOptions.value = (detectorResult || []).map((item) => ({
    ...item,
    id: item.userId ?? item.id
  }))

  const maxPage = Math.max(1, Math.ceil(displayTotal.value / query.pageSize))
  if (query.pageNum > maxPage) {
    query.pageNum = 1
  }
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

.subflow-panel {
  display: grid;
  gap: 16px;
  padding: 8px 6px;
}

.subflow-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.subflow-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.subflow-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.subflow-card {
  display: grid;
  gap: 12px;
  padding: 16px;
  border: 1px solid var(--line-soft);
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(250, 252, 255, 0.98), rgba(244, 248, 255, 0.92));
}

.subflow-card__head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.subflow-card__head strong {
  display: block;
  color: var(--text-main);
  font-size: 15px;
  line-height: 1.5;
}

.subflow-card__head p {
  margin: 6px 0 0;
  color: var(--text-sub);
  font-size: 13px;
  line-height: 1.6;
}

.subflow-meta {
  display: grid;
  gap: 8px;
  color: var(--text-sub);
  font-size: 13px;
  line-height: 1.6;
}

.subflow-assign {
  display: grid;
  gap: 8px;
}

.subflow-assign label {
  color: var(--text-main);
  font-size: 13px;
  font-weight: 600;
}

.subflow-empty {
  padding: 20px 14px;
  color: var(--text-sub);
  line-height: 1.8;
}

.subflow-empty strong {
  display: block;
  color: var(--text-main);
  margin-bottom: 6px;
}

@media (max-width: 1100px) {
  .subflow-list {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .page-hero,
  .scene-grid {
    grid-template-columns: 1fr;
  }

  .hero-tags {
    justify-content: flex-start;
  }

  .subflow-head {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
