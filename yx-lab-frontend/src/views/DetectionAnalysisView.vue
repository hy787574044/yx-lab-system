<template>
  <div class="content-grid detection-analysis-page fixed-table-page">
    <section class="glass-panel section-block fixed-table-section">
      <div class="section-head">
        <div>
          <h3 class="section-title">检测分析队列</h3>
        </div>
      </div>

      <section class="stats-grid section-stats">
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

      <div class="toolbar-panel">
        <div class="toolbar-row">
          <div class="toolbar-main">
            <div class="toolbar-fields">
              <label class="toolbar-field toolbar-field--medium">
                <span>关键字</span>
                <el-input
                  v-model="query.keyword"
                  clearable
                  placeholder="请输入样品编号、封签编号、检测套餐、检测参数、检测方法或检测人员"
                  @keyup.enter="handleSearch"
                />
              </label>
              <label class="toolbar-field">
                <span>子流程状态</span>
                <el-select v-model="query.itemStatus" clearable placeholder="请选择子流程状态">
                  <el-option
                    v-for="option in itemStatusOptions"
                    :key="option.value"
                    :label="option.label"
                    :value="option.value"
                  />
                </el-select>
              </label>
              <label class="toolbar-field">
                <span>数据范围</span>
                <el-select v-model="query.mine" clearable placeholder="请选择数据范围">
                  <el-option
                    v-for="option in mineOptions"
                    :key="String(option.value)"
                    :label="option.label"
                    :value="option.value"
                  />
                </el-select>
              </label>
            </div>
          </div>
          <div class="toolbar-actions">
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="resetQuery">重置</el-button>
            <el-button @click="handleExport">导出</el-button>
          </div>
        </div>
      </div>

      <div class="table-card table-card--fixed-scroll">
        <div class="table-card__body">
          <el-table
            class="list-table"
            :data="records"
            stripe
            height="100%"
            empty-text="暂无检测分析子流程数据"
          >
            <el-table-column prop="sampleNo" label="样品编号" min-width="150" />
            <el-table-column prop="sealNo" label="封签编号" min-width="150" />
            <el-table-column prop="detectionTypeName" label="检测套餐" min-width="170" show-overflow-tooltip />
            <el-table-column prop="parameterName" label="检测参数" min-width="150" show-overflow-tooltip />
            <el-table-column prop="methodName" label="检测方法" min-width="180" show-overflow-tooltip>
              <template #default="{ row }">{{ row.methodName || '-' }}</template>
            </el-table-column>
            <el-table-column label="检测步骤" min-width="220" show-overflow-tooltip>
              <template #default="{ row }">{{ row.methodBasis || '-' }}</template>
            </el-table-column>
            <el-table-column label="检测人员" min-width="120">
              <template #default="{ row }">{{ row.detectorName || '-' }}</template>
            </el-table-column>
            <el-table-column label="标准范围" min-width="150">
              <template #default="{ row }">{{ formatStandardRange(row.standardMin, row.standardMax) }}</template>
            </el-table-column>
            <el-table-column prop="unit" label="单位" width="90">
              <template #default="{ row }">{{ row.unit || '-' }}</template>
            </el-table-column>
            <el-table-column prop="referenceStandard" label="检测标准" min-width="160" show-overflow-tooltip>
              <template #default="{ row }">{{ row.referenceStandard || '-' }}</template>
            </el-table-column>
            <el-table-column label="检测结果" min-width="120" header-cell-class-name="result-field-header">
              <template #default="{ row }">
                {{ row.resultValue ?? '-' }}
              </template>
            </el-table-column>
            <el-table-column label="判定结果" width="110" header-cell-class-name="cell-center" class-name="cell-center">
              <template #default="{ row }">
                <span class="status-chip" :class="getResultValueStatusClass(row)">
                  {{ getResultValueStatusLabel(row) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="子流程状态" width="120" header-cell-class-name="cell-center" class-name="cell-center">
              <template #default="{ row }">
                <span class="status-chip" :class="getItemStatusClass(row.itemStatus)">
                  {{ getItemStatusLabel(row.itemStatus) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="updatedTime" label="更新时间" width="170" />
            <el-table-column label="操作" width="120" fixed="right" header-cell-class-name="cell-center" class-name="cell-center">
              <template #default="{ row }">
                <div class="table-action-row">
                  <span v-if="row.itemStatus === WAIT_ASSIGN_STATUS" class="table-action-placeholder">待分配</span>
                  <el-button
                    v-else
                    type="primary"
                    link
                    @click="openResultDialog(row)"
                  >
                    {{ getResultActionLabel(row) }}
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <TablePagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          @change="loadData"
        />
      </div>
    </section>

    <el-dialog
      v-model="resultDialogVisible"
      class="detection-result-dialog"
      :title="resultDialogTitle"
      width="760px"
      destroy-on-close
      @closed="resetResultForm"
    >
      <div class="result-dialog">
        <div class="result-dialog__summary">
          <span class="binding-editor__chip">样品编号<strong>{{ resultForm.sampleNo || '-' }}</strong></span>
          <span class="binding-editor__chip">检测套餐<strong>{{ resultForm.detectionTypeName || '-' }}</strong></span>
          <span class="binding-editor__chip">检测参数<strong>{{ resultForm.parameterName || '-' }}</strong></span>
          <span class="binding-editor__chip">检测人员<strong>{{ resultForm.detectorName || '-' }}</strong></span>
        </div>

        <div class="result-meta-grid">
          <div class="result-meta-card">
            <span>检测方法</span>
            <strong>{{ resultForm.methodName || '-' }}</strong>
          </div>
          <div class="result-meta-card">
            <span>检测标准</span>
            <strong>{{ resultForm.referenceStandard || '-' }}</strong>
          </div>
          <div class="result-meta-card">
            <span>标准范围</span>
            <strong>{{ formatStandardRange(resultForm.standardMin, resultForm.standardMax, resultForm.unit) }}</strong>
          </div>
          <div class="result-meta-card">
            <span>单位</span>
            <strong>{{ resultForm.unit || '-' }}</strong>
          </div>
        </div>

        <el-form label-position="top" class="result-dialog__form">
          <div class="result-step-panel">
            <span>检测步骤</span>
            <strong>{{ resultForm.methodBasis || '-' }}</strong>
          </div>
          <el-form-item label="检测结果" :required="!resultDialogReadonly" class="result-field-form-item">
            <div class="result-value-field">
              <span v-if="resultDialogReadonly" class="result-fixed-value">{{ resultForm.resultValue ?? '-' }}</span>
              <el-input-number
                v-else
                v-model="resultForm.resultValue"
                :precision="4"
                :step="0.01"
                controls-position="right"
                class="result-value-input"
              />
              <span v-if="resultUnitVisible" class="result-value-unit">{{ resultForm.unit }}</span>
            </div>
          </el-form-item>
          <el-form-item label="异常说明" class="result-remark-item">
            <el-input
              v-model="resultForm.abnormalRemark"
              type="textarea"
              :rows="1"
              :readonly="resultDialogReadonly"
              placeholder="如有异常项，可补充现场化验情况、复核说明或异常原因"
            />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <el-button @click="resultDialogVisible = false">{{ resultDialogReadonly ? '关闭' : '取消' }}</el-button>
        <el-button
          v-if="!resultDialogReadonly"
          type="primary"
          :loading="resultSubmitting"
          @click="submitDetectionResult"
        >
          提交
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElButton } from 'element-plus/es/components/button/index.mjs'
import { ElDialog } from 'element-plus/es/components/dialog/index.mjs'
import { ElForm, ElFormItem } from 'element-plus/es/components/form/index.mjs'
import { ElInput } from 'element-plus/es/components/input/index.mjs'
import { ElInputNumber } from 'element-plus/es/components/input-number/index.mjs'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElOption, ElSelect } from 'element-plus/es/components/select/index.mjs'
import { ElTable, ElTableColumn } from 'element-plus/es/components/table/index.mjs'
import TablePagination from '../components/common/TablePagination.vue'
import {
  exportDetectionItemsApi,
  fetchDetectionItemsApi,
  fetchDetectionItemSummaryApi,
  submitDetectionApi
} from '../api/lab'
import {
  approvedDetectionStatus,
  DEFAULT_PAGE_SIZE,
  detectionStatusLabelMap,
  getEnumLabel,
  getStatusClass,
  rejectedDetectionStatus,
  reviewPendingDetectionStatus,
  waitAssignDetectionStatus,
  waitDetectDetectionStatus
} from '../utils/labEnums'

const WAIT_ASSIGN_STATUS = waitAssignDetectionStatus
const WAIT_DETECT_STATUS = waitDetectDetectionStatus

const query = reactive({
  keyword: '',
  itemStatus: '',
  mine: '',
  pageNum: 1,
  pageSize: DEFAULT_PAGE_SIZE
})

const records = ref([])
const total = ref(0)
let loadDataVersion = 0
const summary = reactive({
  total: 0,
  waitAssignCount: 0,
  waitDetectCount: 0,
  pendingReviewCount: 0,
  approvedCount: 0,
  rejectedCount: 0
})
const resultDialogVisible = ref(false)
const resultSubmitting = ref(false)

const resultForm = reactive({
  id: null,
  recordId: null,
  sampleId: null,
  sampleNo: '',
  sealNo: '',
  detectionTypeId: null,
  detectionTypeName: '',
  parameterId: null,
  parameterName: '',
  methodName: '',
  methodBasis: '',
  standardMin: null,
  standardMax: null,
  unit: '',
  referenceStandard: '',
  detectorName: '',
  resultValue: null,
  abnormalRemark: '',
  itemStatus: ''
})

const mineOptions = [
  { value: true, label: '仅看我的' },
  { value: false, label: '查看全部' }
]

const statusKeyMap = {
  all: '',
  waitAssign: WAIT_ASSIGN_STATUS,
  waitDetect: WAIT_DETECT_STATUS,
  pendingReview: reviewPendingDetectionStatus,
  approved: approvedDetectionStatus,
  rejected: rejectedDetectionStatus
}

const activeStatKey = computed(() => (
  Object.entries(statusKeyMap).find(([, value]) => value === query.itemStatus)?.[0] || 'all'
))

const currentStats = computed(() => [
  {
    key: 'all',
    label: '子流程总量',
    value: summary.total,
    desc: '当前筛选范围内的全部检测子流程'
  },
  {
    key: 'waitAssign',
    label: '待分配',
    value: summary.waitAssignCount,
    desc: '尚未分配检测人员的检测子流程'
  },
  {
    key: 'waitDetect',
    label: '待检测',
    value: summary.waitDetectCount,
    desc: '已分配检测人员，等待录入检测结果'
  },
  {
    key: 'pendingReview',
    label: '待审核',
    value: summary.pendingReviewCount,
    desc: '检测结果已提交，等待进入结果审核'
  },
  {
    key: 'approved',
    label: '已通过',
    value: summary.approvedCount,
    desc: '检测子流程已审核通过'
  },
  {
    key: 'rejected',
    label: '已驳回',
    value: summary.rejectedCount,
    desc: '检测子流程被驳回，待重新检测'
  }
])

const itemStatusOptions = computed(() => Object.entries(detectionStatusLabelMap).map(([value, label]) => ({ value, label })))

const resultDialogReadonly = computed(() => ![WAIT_DETECT_STATUS, rejectedDetectionStatus].includes(resultForm.itemStatus))

const resultDialogTitle = computed(() => resultDialogReadonly.value ? '检测结果查看' : '检测结果录入')

const resultUnitVisible = computed(() => {
  const unit = String(resultForm.unit || '').trim()
  return Boolean(unit && unit !== '-')
})

function handleStatClick(key) {
  const nextStatus = statusKeyMap[key] ?? ''
  query.itemStatus = query.itemStatus === nextStatus ? '' : nextStatus
  query.pageNum = 1
  loadData()
}

function handleSearch() {
  query.pageNum = 1
  loadData()
}

function resetQuery() {
  query.keyword = ''
  query.itemStatus = ''
  query.mine = ''
  query.pageNum = 1
  loadData()
}

function getItemStatusLabel(status) {
  return getEnumLabel(detectionStatusLabelMap, status)
}

function getItemStatusClass(status) {
  return getStatusClass('detectionStatus', status)
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
  return '-'
}

function isResultValueAbnormal(item) {
  if (!item || item.resultValue == null || item.resultValue === '') {
    return false
  }
  const value = Number(item.resultValue)
  if (!Number.isFinite(value)) {
    return false
  }
  if (item.standardMin != null && value < Number(item.standardMin)) {
    return true
  }
  return item.standardMax != null && value > Number(item.standardMax)
}

function getResultValueStatusLabel(item) {
  if (!item || item.resultValue == null || item.resultValue === '') {
    return '待录入'
  }
  return isResultValueAbnormal(item) ? '异常' : '正常'
}

function getResultValueStatusClass(item) {
  const label = getResultValueStatusLabel(item)
  if (label === '异常') {
    return 'danger'
  }
  if (label === '正常') {
    return 'success'
  }
  return 'info'
}

function isResultEditable(row) {
  return !!row && [WAIT_DETECT_STATUS, rejectedDetectionStatus].includes(row.itemStatus)
}

function getResultActionLabel(row) {
  return isResultEditable(row) ? '录入结果' : '查看结果'
}

function resetResultForm() {
  resultForm.id = null
  resultForm.recordId = null
  resultForm.sampleId = null
  resultForm.sampleNo = ''
  resultForm.sealNo = ''
  resultForm.detectionTypeId = null
  resultForm.detectionTypeName = ''
  resultForm.parameterId = null
  resultForm.parameterName = ''
  resultForm.methodName = ''
  resultForm.methodBasis = ''
  resultForm.standardMin = null
  resultForm.standardMax = null
  resultForm.unit = ''
  resultForm.referenceStandard = ''
  resultForm.detectorName = ''
  resultForm.resultValue = null
  resultForm.abnormalRemark = ''
  resultForm.itemStatus = ''
}

function openResultDialog(row) {
  resetResultForm()
  resultForm.id = row.id
  resultForm.recordId = row.recordId
  resultForm.sampleId = row.sampleId
  resultForm.sampleNo = row.sampleNo || ''
  resultForm.sealNo = row.sealNo || ''
  resultForm.detectionTypeId = row.detectionTypeId
  resultForm.detectionTypeName = row.detectionTypeName || ''
  resultForm.parameterId = row.parameterId
  resultForm.parameterName = row.parameterName || ''
  resultForm.methodName = row.methodName || ''
  resultForm.methodBasis = row.methodBasis || ''
  resultForm.standardMin = row.standardMin
  resultForm.standardMax = row.standardMax
  resultForm.unit = row.unit || ''
  resultForm.referenceStandard = row.referenceStandard || ''
  resultForm.detectorName = row.detectorName || ''
  resultForm.resultValue = row.resultValue == null ? null : Number(row.resultValue)
  resultForm.abnormalRemark = row.abnormalRemark || ''
  resultForm.itemStatus = row.itemStatus || ''
  resultDialogVisible.value = true
}

async function submitDetectionResult() {
  if (!resultForm.sampleId || !resultForm.detectionTypeId || !resultForm.recordId || !resultForm.id) {
    ElMessage.warning('当前检测子流程缺少必要信息，暂不能提交')
    return
  }
  if (resultForm.resultValue == null || resultForm.resultValue === '') {
    ElMessage.warning('请先填写检测结果')
    return
  }

  resultSubmitting.value = true
  try {
    await submitDetectionApi({
      recordId: resultForm.recordId,
      itemId: resultForm.id,
      sampleId: resultForm.sampleId,
      detectionTypeId: resultForm.detectionTypeId,
      detectionTypeName: resultForm.detectionTypeName,
      abnormalRemark: resultForm.abnormalRemark,
      items: [{
        parameterId: resultForm.parameterId,
        parameterName: resultForm.parameterName,
        standardMin: resultForm.standardMin,
        standardMax: resultForm.standardMax,
        resultValue: resultForm.resultValue,
        unit: resultForm.unit
      }]
    })
    resultDialogVisible.value = false
    ElMessage.success('检测结果已提交')
    await loadData()
  } finally {
    resultSubmitting.value = false
  }
}

async function loadData() {
  const currentVersion = ++loadDataVersion
  const requestQuery = {
    ...query,
    mine: query.mine === '' ? undefined : query.mine
  }
  const summaryQuery = {
    keyword: requestQuery.keyword,
    mine: requestQuery.mine
  }
  const [pageResult, summaryResult] = await Promise.all([
    fetchDetectionItemsApi(requestQuery),
    fetchDetectionItemSummaryApi(summaryQuery)
  ])
  if (currentVersion !== loadDataVersion) {
    return
  }
  records.value = pageResult.records || []
  total.value = Number(pageResult.total || 0)
  if (!records.value.length && total.value > 0 && query.pageNum > 1) {
    query.pageNum = 1
    await loadData()
    return
  }
  summary.total = Number(summaryResult.total || 0)
  summary.waitAssignCount = Number(summaryResult.waitAssignCount || 0)
  summary.waitDetectCount = Number(summaryResult.waitDetectCount || 0)
  summary.pendingReviewCount = Number(summaryResult.pendingReviewCount || 0)
  summary.approvedCount = Number(summaryResult.approvedCount || 0)
  summary.rejectedCount = Number(summaryResult.rejectedCount || 0)
}

async function handleExport() {
  try {
    await exportDetectionItemsApi({
      ...query,
      mine: query.mine === '' ? undefined : query.mine
    })
    ElMessage.success('检测分析导出成功')
  } catch (error) {
    ElMessage.error(error.message || '检测分析导出失败')
  }
}

onMounted(async () => {
  await loadData()
})
</script>

<style scoped>
.detection-analysis-page {
  gap: 12px;
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
  margin: 8px 0 0;
  color: var(--text-sub);
  font-size: 14px;
  line-height: 1.6;
}

.table-action-placeholder {
  color: var(--text-sub);
  font-size: 13px;
}

.result-dialog {
  display: grid;
  gap: 12px;
}

.result-dialog__summary {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.result-meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.result-meta-card {
  display: grid;
  gap: 4px;
  padding: 12px 14px;
  border: 1px solid var(--line-soft);
  border-radius: 12px;
  background: var(--bg-panel-soft);
}

.result-meta-card span {
  color: var(--text-sub);
  font-size: 13px;
}

.result-meta-card strong {
  color: var(--text-main);
  font-size: 14px;
  line-height: 1.6;
  font-weight: 600;
}

.result-step-panel {
  display: grid;
  gap: 6px;
  margin-bottom: 12px;
  padding: 14px 16px;
  border: 1px solid;
  border-radius: 12px;
  border-color: color-mix(in srgb, var(--brand) 28%, #ffffff 72%);
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--brand) 10%, #ffffff 90%), #ffffff 58%),
    var(--bg-panel-soft);
}

.result-step-panel span {
  color: color-mix(in srgb, var(--brand) 70%, var(--text-sub) 30%);
  font-size: 13px;
}

.result-step-panel strong {
  color: var(--text-main);
  font-size: 15px;
  line-height: 1.75;
  font-weight: 600;
}

.result-dialog__form {
  margin-top: 2px;
}

.result-value-field {
  display: flex;
  align-items: center;
  gap: 10px;
}

.result-value-input {
  width: 220px;
  max-width: 100%;
}

:deep(.result-field-header .cell),
:deep(.result-field-form-item .el-form-item__label) {
  color: #d4380d;
  font-weight: 700;
}

.result-value-unit {
  color: var(--text-main);
  font-size: 14px;
  font-weight: 600;
  line-height: 32px;
  white-space: nowrap;
}

.result-remark-item :deep(.el-textarea__inner) {
  min-height: 34px !important;
  padding-top: 6px;
  padding-bottom: 6px;
  resize: none;
}

.result-fixed-value {
  color: var(--text-main);
  line-height: 32px;
}

.binding-editor__chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid color-mix(in srgb, var(--brand) 16%, #ffffff 84%);
  background: color-mix(in srgb, var(--brand) 7%, #ffffff 93%);
  color: var(--text-sub);
  font-size: 13px;
}

.binding-editor__chip strong {
  color: var(--brand);
  font-size: 15px;
}

@media (max-width: 900px) {
  .result-meta-grid {
    grid-template-columns: 1fr;
  }
}
</style>
