<template>
  <div class="content-grid dashboard-page" v-loading="loading">
    <section class="glass-panel workbench-hero">
      <div class="workbench-hero__title">
        <h3>化验室工作台</h3>
      </div>
      <div class="workbench-hero__flow">
        <span>流程概览</span>
        <div class="workbench-hero__flow-chips">
          <button
            v-for="item in workflowOverviewItems"
            :key="item.key"
            type="button"
            :disabled="!item.action"
            @click="item.action && openWorkbenchAction(item.action)"
          >
            {{ item.label }}
          </button>
        </div>
      </div>
      <div class="workbench-hero__stats">
        <span>
          <strong>{{ warningTotal }}</strong>
          <em>待处理预警</em>
        </span>
        <span>
          <strong>{{ latestPassRate }}%</strong>
          <em>近况合格率</em>
        </span>
      </div>
    </section>

    <section class="todo-detail-grid">
      <article
        v-for="section in workbenchSections"
        :key="section.key"
        class="glass-panel todo-detail-panel"
      >
        <div class="todo-detail-panel__head">
          <div>
            <h3>{{ section.title }}</h3>
          </div>
          <strong>{{ section.rows.length }}</strong>
        </div>
        <div v-if="section.rows.length" class="todo-detail-list">
          <button
            v-for="row in section.rows"
            :key="`${section.key}-${getActionRowKey(row)}`"
            type="button"
            :class="['todo-detail-card', `todo-detail-card--${section.key}`]"
            @click="handlePreviewCardClick(section.key, row)"
          >
            <div class="todo-detail-card__main">
              <span>{{ getPreviewRowTitle(section.key, row) }}</span>
              <p>{{ getPreviewDisplayRowMeta(section.key, row) }}</p>
            </div>
            <div class="todo-detail-card__fields">
              <span v-for="field in getPreviewDisplayFields(section.key, row)" :key="field.label">
                <em>{{ field.label }}</em>
                <strong
                  v-if="field.statusType || field.statusClass"
                  class="status-chip"
                  :class="field.statusClass || getStatusClass(field.statusType, field.statusValue)"
                >
                  {{ field.value || '-' }}
                </strong>
                <strong v-else>{{ field.value || '-' }}</strong>
              </span>
            </div>
          </button>
        </div>
        <div v-else class="todo-detail-empty">暂无待处理数据</div>
      </article>
    </section>

    <el-dialog
      v-model="workbenchDialogVisible"
      class="workbench-action-dialog"
      :title="activeActionTitle"
      width="1180px"
      align-center
      destroy-on-close
      @closed="resetWorkbenchDialog"
    >
      <div class="workbench-dialog" v-loading="actionLoading">
        <aside class="workbench-dialog__side">
          <div class="workbench-dialog__side-head">
            <span>待处理列表</span>
            <strong>{{ actionRows.length }}</strong>
          </div>
          <div v-if="actionRows.length" class="workbench-row-list">
            <button
              v-for="row in actionRows"
              :key="getActionRowKey(row)"
              type="button"
              :class="['workbench-row-card', { 'is-active': isActionRowActive(row) }]"
              @click="selectActionRow(row)"
            >
              <strong>{{ getActionRowTitle(row) }}</strong>
              <span>{{ getActionRowMeta(row) }}</span>
            </button>
          </div>
          <div v-else class="empty-block">当前没有可处理数据</div>
        </aside>

        <main class="workbench-dialog__main">
          <template v-if="activeAction === 'sampling'">
            <el-form label-width="96px">
              <div class="form-grid">
                <el-form-item label="任务编号">
                  <el-input :model-value="samplingTask?.taskNo || '-'" readonly />
                </el-form-item>
                <el-form-item label="样品编号">
                  <el-input :model-value="samplingTask?.sampleNo || '-'" readonly />
                </el-form-item>
                <el-form-item label="点位名称">
                  <el-input :model-value="samplingTask?.pointName || '-'" readonly />
                </el-form-item>
                <el-form-item label="采样人员">
                  <el-input :model-value="samplingTask?.samplerName || '-'" readonly />
                </el-form-item>
                <el-form-item label="天气">
                  <el-select v-model="samplingForm.weather" allow-create clearable filterable default-first-option style="width: 100%">
                    <el-option v-for="option in weatherOptions" :key="option.value" :label="option.label" :value="option.value" />
                  </el-select>
                </el-form-item>
                <el-form-item label="温度">
                  <el-input v-model="samplingForm.temperature" placeholder="例如：26℃" />
                </el-form-item>
                <el-form-item label="采样总容量">
                  <el-input v-model="samplingForm.sampleTotalVolume" placeholder="请输入采样总容量">
                    <template #append>mL</template>
                  </el-input>
                </el-form-item>
                <el-form-item label="采样瓶数">
                  <el-input-number v-model="samplingForm.sampleBottleCount" :min="0" :precision="0" style="width: 100%" />
                </el-form-item>
                <el-form-item class="form-span-2" label="备注">
                  <el-input v-model="samplingForm.remark" type="textarea" :rows="3" placeholder="可补充采样过程、异常情况等说明" />
                </el-form-item>
              </div>
            </el-form>
          </template>

          <template v-else-if="activeAction === 'sampleLogin'">
            <el-form label-width="96px">
              <div class="form-grid">
                <el-form-item label="待登录任务">
                  <el-input :model-value="formatTaskLabel(loginTask)" readonly />
                </el-form-item>
                <el-form-item label="样品编号">
                  <el-input :model-value="loginForm.sampleNo || '-'" readonly />
                </el-form-item>
                <el-form-item label="点位名称">
                  <el-input :model-value="loginForm.pointName || '-'" readonly />
                </el-form-item>
                <el-form-item label="样品类型">
                  <el-input :model-value="getEnumLabel(sampleTypeLabelMap, loginForm.sampleType)" readonly />
                </el-form-item>
                <el-form-item label="样品来源">
                  <el-select v-model="loginForm.sampleSourceMethod" style="width: 100%">
                    <el-option v-for="option in sampleSourceMethodOptions" :key="option.value" :label="option.label" :value="option.value" />
                  </el-select>
                </el-form-item>
                <el-form-item label="采样人员">
                  <el-input v-model="loginForm.samplerName" readonly />
                </el-form-item>
                <el-form-item class="form-span-2" label="检测套餐" required>
                  <el-select v-model="loginForm.detectionTypeId" filterable style="width: 100%" @change="handleLoginDetectionTypeChange">
                    <el-option
                      v-for="item in loginDetectionTypeOptions"
                      :key="item.id"
                      :label="formatDetectionTypeLabel(item)"
                      :value="item.id"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="审核流程" required>
                  <el-select v-model="loginForm.reviewFlowId" filterable style="width: 100%" @change="handleReviewFlowChange">
                    <el-option v-for="item in reviewFlowOptions" :key="item.id" :label="item.flowName" :value="item.id" />
                  </el-select>
                </el-form-item>
                <el-form-item label="采样时间" required>
                  <el-date-picker
                    v-model="loginForm.samplingTime"
                    type="datetime"
                    format="YYYY-MM-DD HH:mm:ss"
                    value-format="YYYY-MM-DD HH:mm:ss"
                    style="width: 100%"
                  />
                </el-form-item>
                <el-form-item label="天气">
                  <el-select v-model="loginForm.weather" allow-create clearable filterable default-first-option style="width: 100%">
                    <el-option v-for="option in weatherOptions" :key="option.value" :label="option.label" :value="option.value" />
                  </el-select>
                </el-form-item>
                <el-form-item label="保存条件">
                  <el-select v-model="loginForm.storageCondition" allow-create clearable filterable default-first-option style="width: 100%">
                    <el-option v-for="option in storageConditionOptions" :key="option.value" :label="option.label" :value="option.value" />
                  </el-select>
                </el-form-item>
              </div>
            </el-form>
            <el-table v-if="loginForm.detectionConfigItems.length" class="compact-table" :data="loginForm.detectionConfigItems" size="small" border max-height="260">
              <el-table-column prop="parameterName" label="检测参数" min-width="140" />
              <el-table-column label="标准范围" min-width="120">
                <template #default="{ row }">{{ formatStandardRange(row.standardMin, row.standardMax, row.unit) }}</template>
              </el-table-column>
              <el-table-column prop="methodName" label="检测方法" min-width="160" />
              <el-table-column prop="sampleVolume" label="取样体积" width="100" />
            </el-table>
          </template>

          <template v-else-if="activeAction === 'detection'">
            <div class="summary-chips">
              <span>样品编号<strong>{{ resultForm.sampleNo || '-' }}</strong></span>
              <span>检测参数<strong>{{ resultForm.parameterName || '-' }}</strong></span>
              <span>检测人员<strong>{{ resultForm.detectorName || '-' }}</strong></span>
            </div>
            <div class="meta-grid">
              <div><span>检测方法</span><strong>{{ resultForm.methodName || '-' }}</strong></div>
              <div><span>检测标准</span><strong>{{ resultForm.referenceStandard || '-' }}</strong></div>
              <div><span>标准范围</span><strong>{{ formatStandardRange(resultForm.standardMin, resultForm.standardMax, resultForm.unit) }}</strong></div>
              <div><span>单位</span><strong>{{ resultForm.unit || '-' }}</strong></div>
            </div>
            <el-form label-position="top">
              <el-form-item label="检测结果" required>
                <div class="result-value-field">
                  <el-input-number v-model="resultForm.resultValue" :precision="4" :step="0.01" controls-position="right" class="result-value-input" />
                  <span v-if="resultForm.unit" class="result-value-unit">{{ resultForm.unit }}</span>
                </div>
              </el-form-item>
              <el-form-item label="异常说明">
                <el-input v-model="resultForm.abnormalRemark" type="textarea" :rows="2" />
              </el-form-item>
              <el-form-item label="备注">
                <el-input v-model="resultForm.remark" type="textarea" :rows="2" />
              </el-form-item>
            </el-form>
          </template>

          <template v-else-if="activeAction === 'review'">
            <div class="summary-chips">
              <span>样品编号<strong>{{ reviewForm.sampleNo || '-' }}</strong></span>
              <span>检测套餐<strong>{{ reviewForm.detectionTypeName || '-' }}</strong></span>
              <span>待审核<strong>{{ pendingReviewItemCount }}</strong></span>
            </div>
            <div class="review-toolbar">
              <el-button type="primary" @click="setAllReviewResult(approvedReviewResult)">一键通过</el-button>
              <el-button type="danger" plain @click="setAllReviewResult(rejectedReviewResult)">一键驳回</el-button>
            </div>
            <el-table class="compact-table" :data="reviewForm.items" size="small" border max-height="430">
              <el-table-column prop="parameterName" label="检测参数" min-width="130" />
              <el-table-column prop="methodName" label="检测方法" min-width="150" />
              <el-table-column prop="resultValue" label="检测值" width="100" />
              <el-table-column label="审核操作" min-width="180">
                <template #default="{ row }">
                  <el-button size="small" :type="row.reviewResultDraft === approvedReviewResult ? 'primary' : 'default'" @click="row.reviewResultDraft = approvedReviewResult">通过</el-button>
                  <el-button size="small" :type="row.reviewResultDraft === rejectedReviewResult ? 'danger' : 'default'" @click="row.reviewResultDraft = rejectedReviewResult">驳回</el-button>
                </template>
              </el-table-column>
              <el-table-column label="驳回原因" min-width="220">
                <template #default="{ row }">
                  <el-input v-model="row.rejectReasonDraft" :disabled="row.reviewResultDraft !== rejectedReviewResult" placeholder="驳回时填写原因" />
                </template>
              </el-table-column>
            </el-table>
            <el-form label-position="top" class="review-remark-form">
              <el-form-item label="整单审核意见">
                <el-input v-model="reviewForm.reviewRemark" type="textarea" :rows="3" />
              </el-form-item>
            </el-form>
          </template>

          <template v-else-if="activeAction === 'report'">
            <div v-if="previewError" class="empty-block">{{ previewError }}</div>
            <component
              v-else-if="previewData"
              :is="previewComponent"
              :preview-data="previewData"
            />
            <div v-else class="empty-block">请选择左侧报告进行预览</div>
          </template>
        </main>
      </div>

      <template #footer>
        <el-button @click="workbenchDialogVisible = false">关闭</el-button>
        <el-button v-if="activeAction === 'sampling'" type="primary" :loading="submitting" @click="submitSampling">保存采样录入</el-button>
        <el-button v-if="activeAction === 'sampleLogin'" type="primary" :loading="submitting" @click="submitSampleLogin">保存样品登录</el-button>
        <el-button v-if="activeAction === 'detection'" type="primary" :loading="submitting" @click="submitDetectionResult">提交检测结果</el-button>
        <el-button v-if="activeAction === 'review'" type="primary" :loading="submitting" @click="submitReviewDecision">提交审核</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElButton } from 'element-plus/es/components/button/index.mjs'
import { ElDatePicker } from 'element-plus/es/components/date-picker/index.mjs'
import { ElDialog } from 'element-plus/es/components/dialog/index.mjs'
import { ElForm, ElFormItem } from 'element-plus/es/components/form/index.mjs'
import { ElInput } from 'element-plus/es/components/input/index.mjs'
import { ElInputNumber } from 'element-plus/es/components/input-number/index.mjs'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElOption, ElSelect } from 'element-plus/es/components/select/index.mjs'
import { ElTable, ElTableColumn } from 'element-plus/es/components/table/index.mjs'
import { ElLoadingDirective } from 'element-plus/es/components/loading/index.mjs'
import {
  completeSamplingTaskApi,
  fetchDetectionDetailApi,
  fetchDetectionItemsApi,
  fetchDetectionMethodOptionsApi,
  fetchDetectionParametersApi,
  fetchDetectionsApi,
  fetchDetectionTypesApi,
  fetchDictItemsApi,
  fetchFlowConfigOptionsApi,
  fetchReportPreviewDataApi,
  fetchReportsApi,
  fetchSamplingPlansApi,
  fetchSamplingTaskDetailApi,
  fetchSamplingTasksApi,
  leaderDashboardApi,
  loginSampleApi,
  startSamplingTaskApi,
  submitDetectionApi,
  submitReviewApi
} from '../api/lab'
import ReportPrintDocument from '../components/report/ReportPrintDocument.vue'
import RawRecordPrintDocument from '../components/report/RawRecordPrintDocument.vue'
import {
  approvedDetectionStatus,
  approvedReviewResult,
  activePlanStatus,
  completedTaskStatus,
  cycleTypeLabelMap,
  draftReportStatus,
  detectionStatusLabelMap,
  generatedReportStatus,
  getEnumLabel,
  getStatusClass,
  pendingTaskStatus,
  planStatusLabelMap,
  rawRecordReportCategory,
  reportCategoryLabelMap,
  reportStatusLabelMap,
  rejectedDetectionStatus,
  rejectedReviewResult,
  reviewPendingDetectionStatus,
  sampleRegisterStatusLabelMap,
  sampleSourceMethodOptions,
  sampleTypeLabelMap,
  samplingSampleSourceMethod,
  taskStatusLabelMap,
  unregisteredSampleRegisterStatus,
  waitDetectDetectionStatus
} from '../utils/labEnums'

const FLOW_TYPE_REVIEW = 'REVIEW'
const vLoading = ElLoadingDirective
const router = useRouter()
const loading = ref(false)
const actionLoading = ref(false)
const submitting = ref(false)
const dashboard = ref({})
const activeAction = ref('')
const workbenchDialogVisible = ref(false)
const actionRows = ref([])
const activeRow = ref(null)
const weatherOptions = ref([])
const storageConditionOptions = ref([])
const detectionTypes = ref([])
const detectionParameters = ref([])
const detectionMethods = ref([])
const reviewFlowOptions = ref([])
const previewData = ref(null)
const previewError = ref('')
const previewRows = reactive({
  samplingPlan: [],
  sampling: [],
  sampleLogin: [],
  detection: [],
  review: [],
  report: []
})

const samplingTask = ref(null)
const loginTask = ref(null)

const samplingForm = reactive({
  taskId: null,
  weather: '',
  temperature: '',
  sampleTotalVolume: '',
  sampleBottleCount: null,
  remark: ''
})

const loginForm = reactive({
  taskId: null,
  sampleNo: '',
  pointId: null,
  pointName: '',
  sampleType: '',
  sampleSourceMethod: samplingSampleSourceMethod,
  detectionItems: '',
  detectionTypeId: null,
  detectionTypeName: '',
  detectionConfigItems: [],
  reviewFlowId: null,
  reviewFlowName: '',
  samplingTime: '',
  samplerId: null,
  samplerName: '',
  weather: '',
  storageCondition: '',
  remark: ''
})

const resultForm = reactive({
  id: null,
  recordId: null,
  sampleId: null,
  sampleNo: '',
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
  remark: '',
  itemStatus: ''
})

const reviewForm = reactive({
  detectionRecordId: null,
  sampleNo: '',
  detectionTypeName: '',
  reviewRemark: '',
  items: []
})

const workbenchSections = computed(() => [
  {
    key: 'samplingPlan',
    title: '采样计划',
    rows: previewRows.samplingPlan
  },
  {
    key: 'sampling',
    title: '采样任务',
    rows: previewRows.sampling
  },
  {
    key: 'sampleLogin',
    title: '样品登录',
    rows: previewRows.sampleLogin
  },
  {
    key: 'detection',
    title: '检测录入',
    rows: previewRows.detection
  },
  {
    key: 'review',
    title: '结果审核',
    rows: previewRows.review
  },
  {
    key: 'report',
    title: '报告处理',
    rows: previewRows.report
  }
])
const workflowOverviewItems = computed(() => {
  const defaultItems = [
    { key: 'sampleLogin', label: '样品登录', action: 'sampleLogin' },
    { key: 'detectionSplit', label: '检测分样', action: '' },
    { key: 'detection', label: '化验检测', action: 'detection' },
    { key: 'review', label: '结果审查', action: 'review' },
    { key: 'report', label: '生成报告', action: 'report' }
  ]
  const processNodes = dashboard.value.processNodes || []
  if (!processNodes.length) {
    return defaultItems
  }
  return defaultItems.map((item) => {
    const matched = processNodes.find((node) => node.label === item.label)
    return matched ? { ...item, label: matched.label || item.label } : item
  })
})
const warningTotal = computed(() => (dashboard.value.warnings || []).reduce((sum, item) => sum + toSafeNumber(item.count), 0))
const latestPassRate = computed(() => {
  const trendItems = dashboard.value.passRateTrend || []
  const latest = trendItems[trendItems.length - 1]
  return toSafeNumber(latest?.value).toFixed(0)
})
const activeActionTitle = computed(() => ({
  sampling: '采样录入',
  sampleLogin: '样品登录',
  detection: '检测结果录入',
  review: '结果审核',
  report: '报告处理'
}[activeAction.value] || '快捷处理'))
const loginDetectionTypeOptions = computed(() => {
  const sampleType = String(loginForm.sampleType || '').trim()
  if (!sampleType) {
    return detectionTypes.value
  }
  return detectionTypes.value.filter((item) => !item.sampleType || String(item.sampleType) === sampleType)
})
const pendingReviewItemCount = computed(() => reviewForm.items.filter((item) => item.itemStatus === reviewPendingDetectionStatus).length)
const previewComponent = computed(() => (
  previewData.value?.reportCategory === rawRecordReportCategory ? RawRecordPrintDocument : ReportPrintDocument
))

function toSafeNumber(value) {
  const num = typeof value === 'number' ? value : Number.parseFloat(String(value ?? '').replace(/,/g, '').trim())
  return Number.isFinite(num) ? num : 0
}

function normalizeDictOptions(items) {
  return (items || []).map((item) => ({
    label: item.itemName || item.label || item.value || item.itemValue,
    value: item.itemValue || item.value || item.itemName || item.label
  })).filter((item) => item.value)
}

async function refreshDashboard() {
  dashboard.value = await leaderDashboardApi() || {}
}

async function loadWorkbenchPreviewRows() {
  const previewPageSize = 12
  const [planResult, samplingResult, loginResult, detectionResult, reviewResult, draftReportResult] = await Promise.all([
    fetchSamplingPlansApi({ pageNum: 1, pageSize: previewPageSize, planStatus: activePlanStatus }),
    fetchSamplingTasksApi({ pageNum: 1, pageSize: previewPageSize, taskStatus: `${pendingTaskStatus},IN_PROGRESS` }),
    fetchSamplingTasksApi({ pageNum: 1, pageSize: previewPageSize, taskStatus: completedTaskStatus, sampleRegisterStatus: 'UNREGISTERED' }),
    fetchDetectionItemsApi({ pageNum: 1, pageSize: previewPageSize, itemStatus: waitDetectDetectionStatus }),
    fetchDetectionsApi({ pageNum: 1, pageSize: previewPageSize, detectionStatus: reviewPendingDetectionStatus }),
    fetchReportsApi({ pageNum: 1, pageSize: previewPageSize, reportStatus: draftReportStatus })
  ])
  previewRows.samplingPlan = planResult.records || []
  previewRows.sampling = samplingResult.records || []
  previewRows.sampleLogin = loginResult.records || []
  previewRows.detection = detectionResult.records || []
  previewRows.review = reviewResult.records || []
  if (draftReportResult.records?.length) {
    previewRows.report = draftReportResult.records
    return
  }
  const generatedReportResult = await fetchReportsApi({ pageNum: 1, pageSize: previewPageSize, reportStatus: generatedReportStatus })
  previewRows.report = generatedReportResult.records || []
}

async function loadDictOptions() {
  const [weatherItems, storageItems] = await Promise.all([
    fetchDictItemsApi('weather_condition'),
    fetchDictItemsApi('storage_condition')
  ])
  weatherOptions.value = normalizeDictOptions(weatherItems)
  storageConditionOptions.value = normalizeDictOptions(storageItems)
}

async function loadDetectionConfigOptions() {
  if (detectionTypes.value.length && detectionParameters.value.length && detectionMethods.value.length) {
    return
  }
  const [typeResult, parameterResult, methodResult] = await Promise.all([
    fetchDetectionTypesApi({ pageNum: 1, pageSize: 500, enabled: 1 }),
    fetchDetectionParametersApi({ pageNum: 1, pageSize: 500 }),
    fetchDetectionMethodOptionsApi()
  ])
  detectionTypes.value = typeResult.records || []
  detectionParameters.value = parameterResult.records || []
  detectionMethods.value = Array.isArray(methodResult) ? methodResult : []
}

async function loadFlowOptions() {
  if (reviewFlowOptions.value.length) {
    return
  }
  const result = await fetchFlowConfigOptionsApi({ flowType: FLOW_TYPE_REVIEW })
  reviewFlowOptions.value = Array.isArray(result) ? result : []
}

function handlePreviewCardClick(key, row) {
  if (key === 'samplingPlan') {
    router.push('/sampling-plan')
    return
  }
  openWorkbenchAction(key, row)
}

async function openWorkbenchAction(key, preferredRow = null) {
  activeAction.value = key
  workbenchDialogVisible.value = true
  actionLoading.value = true
  try {
    await loadActionRows(key)
    const selected = preferredRow
      ? actionRows.value.find((row) => String(getActionRowKey(row)) === String(getActionRowKey(preferredRow))) || preferredRow
      : null
    if (selected) {
      await selectActionRow(selected)
    } else {
      activeRow.value = null
    }
  } finally {
    actionLoading.value = false
  }
}

async function loadActionRows(key) {
  previewData.value = null
  previewError.value = ''
  if (key === 'sampling') {
    const result = await fetchSamplingTasksApi({ pageNum: 1, pageSize: 30, taskStatus: `${pendingTaskStatus},IN_PROGRESS` })
    actionRows.value = result.records || []
    return
  }
  if (key === 'sampleLogin') {
    await Promise.all([loadDetectionConfigOptions(), loadFlowOptions()])
    const result = await fetchSamplingTasksApi({
      pageNum: 1,
      pageSize: 30,
      taskStatus: completedTaskStatus,
      sampleRegisterStatus: 'UNREGISTERED'
    })
    actionRows.value = result.records || []
    return
  }
  if (key === 'detection') {
    const result = await fetchDetectionItemsApi({ pageNum: 1, pageSize: 30, itemStatus: waitDetectDetectionStatus })
    actionRows.value = result.records || []
    return
  }
  if (key === 'review') {
    const result = await fetchDetectionsApi({ pageNum: 1, pageSize: 30, detectionStatus: reviewPendingDetectionStatus })
    actionRows.value = result.records || []
    return
  }
  if (key === 'report') {
    const result = await fetchReportsApi({ pageNum: 1, pageSize: 30, reportStatus: draftReportStatus })
    actionRows.value = result.records?.length ? result.records : (await fetchReportsApi({ pageNum: 1, pageSize: 30, reportStatus: generatedReportStatus })).records || []
  }
}

async function selectActionRow(row) {
  activeRow.value = row
  if (activeAction.value === 'sampling') {
    await openSamplingForm(row)
  } else if (activeAction.value === 'sampleLogin') {
    await openSampleLoginForm(row)
  } else if (activeAction.value === 'detection') {
    openResultForm(row)
  } else if (activeAction.value === 'review') {
    await openReviewForm(row)
  } else if (activeAction.value === 'report') {
    await openReportPreview(row)
  }
}

function getActionRowKey(row) {
  return row?.id || row?.taskId || row?.recordId || row?.sampleNo || JSON.stringify(row)
}

function isActionRowActive(row) {
  return String(getActionRowKey(row)) === String(getActionRowKey(activeRow.value))
}

function getActionRowTitle(row) {
  if (activeAction.value === 'sampling' || activeAction.value === 'sampleLogin') {
    return row?.taskNo || row?.sampleNo || '-'
  }
  if (activeAction.value === 'detection' || activeAction.value === 'review') {
    return row?.sampleNo || row?.parameterName || '-'
  }
  if (activeAction.value === 'report') {
    return row?.reportName || row?.sampleNo || '-'
  }
  return '-'
}

function getActionRowMeta(row) {
  if (activeAction.value === 'sampling' || activeAction.value === 'sampleLogin') {
    return `${row?.pointName || '-'} / ${row?.samplerName || '-'}`
  }
  if (activeAction.value === 'detection') {
    return `${row?.parameterName || '-'} / ${row?.methodName || '-'}`
  }
  if (activeAction.value === 'review') {
    return `${row?.detectionTypeName || '-'} / ${row?.detectorName || '-'}`
  }
  if (activeAction.value === 'report') {
    return `${row?.reportStatus || '-'} / ${row?.generatedTime || '-'}`
  }
  return ''
}

function getPreviewRowTitle(key, row) {
  if (key === 'samplingPlan') {
    return row?.planName || row?.pointName || '-'
  }
  if (key === 'sampling' || key === 'sampleLogin') {
    return row?.sampleNo || row?.taskNo || '未生成编号'
  }
  if (key === 'detection') {
    return `${row?.sampleNo || '-'} / ${row?.parameterName || '-'}`
  }
  if (key === 'review') {
    return row?.sampleNo || row?.detectionTypeName || '-'
  }
  if (key === 'report') {
    return row?.reportName || row?.sampleNo || '-'
  }
  return '-'
}

function getPreviewRowMeta(key, row) {
  if (key === 'samplingPlan') {
    return `${row?.pointName || '-'} / ${row?.samplerName || '-'}`
  }
  if (key === 'sampling' || key === 'sampleLogin') {
    return `${row?.pointName || '-'} / ${row?.samplerName || '-'}`
  }
  if (key === 'detection') {
    return `${row?.methodName || '-'} / ${row?.detectorName || '-'}`
  }
  if (key === 'review') {
    return `${row?.detectionTypeName || '-'} / ${row?.detectorName || '-'}`
  }
  if (key === 'report') {
    return `${row?.reportStatus || '-'} / ${row?.reportCategory || '-'}`
  }
  return ''
}

function getPreviewFields(key, row) {
  if (key === 'sampling') {
    return [
      { label: '任务编号', value: row?.taskNo },
      { label: '任务状态', value: row?.taskStatus },
      { label: '计划时间', value: row?.samplingTime }
    ]
  }
  if (key === 'sampleLogin') {
    return [
      { label: '任务编号', value: row?.taskNo },
      { label: '样品类型', value: getEnumLabel(sampleTypeLabelMap, row?.sampleType) },
      { label: '完成时间', value: row?.finishedTime }
    ]
  }
  if (key === 'detection') {
    return [
      { label: '检测参数', value: row?.parameterName },
      { label: '检测方法', value: row?.methodName },
      { label: '检测人员', value: row?.detectorName }
    ]
  }
  if (key === 'review') {
    return [
      { label: '检测套餐', value: row?.detectionTypeName },
      { label: '参数进度', value: `${row?.completedCount ?? 0}/${row?.parameterCount ?? 0}` },
      { label: '更新时间', value: row?.updatedTime }
    ]
  }
  if (key === 'report') {
    return [
      { label: '报告状态', value: row?.reportStatus },
      { label: '生成时间', value: row?.generatedTime },
      { label: '样品编号', value: row?.sampleNo }
    ]
  }
  return []
}

function getPreviewDisplayRowMeta(key, row) {
  if (key === 'report') {
    return `${getEnumLabel(reportStatusLabelMap, row?.reportStatus)} / ${getEnumLabel(reportCategoryLabelMap, row?.reportCategory)}`
  }
  return getPreviewRowMeta(key, row)
}

function getPreviewDisplayFields(key, row) {
  if (key === 'samplingPlan') {
    return [
      {
        label: '计划状态',
        value: getEnumLabel(planStatusLabelMap, row?.planStatus),
        statusType: 'planStatus',
        statusValue: row?.planStatus
      },
      { label: '周期', value: getEnumLabel(cycleTypeLabelMap, row?.cycleType) },
      { label: '开始时间', value: row?.startTime }
    ]
  }
  if (key === 'sampling') {
    return [
      { label: '任务编号', value: row?.taskNo },
      {
        label: '任务状态',
        value: getEnumLabel(taskStatusLabelMap, row?.taskStatus),
        statusType: 'taskStatus',
        statusValue: row?.taskStatus
      },
      { label: '计划时间', value: row?.samplingTime }
    ]
  }
  if (key === 'sampleLogin') {
    return [
      { label: '任务编号', value: row?.taskNo },
      {
        label: '登记状态',
        value: getEnumLabel(sampleRegisterStatusLabelMap, row?.sampleRegisterStatus || unregisteredSampleRegisterStatus),
        statusClass: row?.sampleRegisterStatus && row.sampleRegisterStatus !== unregisteredSampleRegisterStatus ? 'success' : 'warning'
      },
      { label: '完成时间', value: row?.finishedTime }
    ]
  }
  if (key === 'detection') {
    return [
      { label: '检测参数', value: row?.parameterName },
      {
        label: '检测状态',
        value: getEnumLabel(detectionStatusLabelMap, row?.itemStatus),
        statusType: 'detectionStatus',
        statusValue: row?.itemStatus
      },
      { label: '检测人员', value: row?.detectorName }
    ]
  }
  if (key === 'review') {
    return [
      { label: '检测套餐', value: row?.detectionTypeName },
      {
        label: '审核状态',
        value: getEnumLabel(detectionStatusLabelMap, reviewPendingDetectionStatus),
        statusType: 'detectionStatus',
        statusValue: reviewPendingDetectionStatus
      },
      { label: '更新时间', value: row?.updatedTime }
    ]
  }
  if (key === 'report') {
    return [
      {
        label: '报告状态',
        value: getEnumLabel(reportStatusLabelMap, row?.reportStatus),
        statusType: 'reportStatus',
        statusValue: row?.reportStatus
      },
      { label: '生成时间', value: row?.generatedTime },
      { label: '样品编号', value: row?.sampleNo }
    ]
  }
  return getPreviewFields(key, row)
}

async function openSamplingForm(row) {
  const detail = await fetchSamplingTaskDetailApi(row.id)
  const task = detail || row
  samplingTask.value = task
  samplingForm.taskId = task.id
  samplingForm.weather = task.weather || ''
  samplingForm.temperature = task.temperature || ''
  samplingForm.sampleTotalVolume = extractSampleVolumeNumber(task.sampleTotalVolume || '')
  samplingForm.sampleBottleCount = task.sampleBottleCount ?? null
  samplingForm.remark = task.remark || ''
}

function extractSampleVolumeNumber(value) {
  const match = String(value || '').match(/[\d.]+/)
  return match ? match[0] : ''
}

function buildSampleVolumePayload(value) {
  const text = String(value || '').trim()
  return text ? `${text}mL` : ''
}

async function submitSampling() {
  if (!samplingForm.taskId) {
    ElMessage.warning('请选择采样任务')
    return
  }
  submitting.value = true
  try {
    if (samplingTask.value?.taskStatus === pendingTaskStatus) {
      await startSamplingTaskApi(samplingTask.value.id, { remark: '工作台采样录入时自动开始任务' })
    }
    await completeSamplingTaskApi({
      taskId: samplingForm.taskId,
      weather: samplingForm.weather,
      temperature: samplingForm.temperature,
      sampleTotalVolume: buildSampleVolumePayload(samplingForm.sampleTotalVolume),
      sampleBottleCount: samplingForm.sampleBottleCount == null ? '' : String(samplingForm.sampleBottleCount),
      remark: samplingForm.remark
    })
    ElMessage.success('采样录入已保存')
    await reloadActiveAction()
  } finally {
    submitting.value = false
  }
}

async function openSampleLoginForm(task) {
  loginTask.value = task
  loginForm.taskId = task.id
  loginForm.sampleNo = task.sampleNo || ''
  loginForm.pointId = task.pointId || null
  loginForm.pointName = task.pointName || ''
  loginForm.sampleType = task.sampleType || ''
  loginForm.sampleSourceMethod = samplingSampleSourceMethod
  loginForm.samplingTime = task.samplingTime || ''
  loginForm.samplerId = task.samplerId || null
  loginForm.samplerName = task.samplerName || ''
  loginForm.weather = task.weather || ''
  loginForm.storageCondition = ''
  loginForm.remark = task.remark || ''
  const flow = reviewFlowOptions.value.find((item) => item.defaultFlag) || reviewFlowOptions.value[0]
  loginForm.reviewFlowId = flow?.id || null
  loginForm.reviewFlowName = flow?.flowName || ''
  const type = loginDetectionTypeOptions.value[0]
  handleLoginDetectionTypeChange(type?.id || null)
}

function handleReviewFlowChange(flowId) {
  const flow = reviewFlowOptions.value.find((item) => String(item.id) === String(flowId))
  loginForm.reviewFlowName = flow?.flowName || ''
}

function formatDetectionTypeLabel(item) {
  const sampleTypeLabel = item?.sampleType ? getEnumLabel(sampleTypeLabelMap, item.sampleType) : '未绑定样品类型'
  return `${item?.typeName || '-'} / ${sampleTypeLabel}`
}

function handleLoginDetectionTypeChange(typeId) {
  const type = detectionTypes.value.find((item) => String(item.id) === String(typeId || ''))
  loginForm.detectionTypeId = type?.id || null
  loginForm.detectionTypeName = type?.typeName || ''
  loginForm.detectionItems = type?.typeName || ''
  loginForm.detectionConfigItems = buildLoginDetectionConfigItems(type)
}

function parseIdList(value) {
  return String(value || '').split(',').map((item) => item.trim()).filter(Boolean)
}

function parseBindingJson(value) {
  try {
    const list = JSON.parse(String(value || '[]'))
    return Array.isArray(list) ? list : []
  } catch {
    return []
  }
}

function buildLoginDetectionConfigItems(type) {
  if (!type) {
    return []
  }
  const parameterMap = new Map(detectionParameters.value.map((item) => [String(item.id), item]))
  const bindingMap = new Map()
  parseBindingJson(type.parameterMethodBindings).forEach((item) => {
    const parameterId = String(item?.parameterId || '')
    const methodId = Array.isArray(item?.methodIds) ? item.methodIds[0] : null
    if (parameterId) {
      bindingMap.set(parameterId, methodId == null ? '' : String(methodId))
    }
  })
  return parseIdList(type.parameterIds).map((parameterId) => {
    const parameter = parameterMap.get(String(parameterId))
    if (!parameter) {
      return null
    }
    const methodOptions = detectionMethods.value.filter((item) => String(item.parameterId) === String(parameterId) && item.enabled === 1)
    const defaultMethodId = bindingMap.get(parameterId) || methodOptions[0]?.id || ''
    const method = methodOptions.find((item) => String(item.id) === String(defaultMethodId)) || methodOptions[0]
    return {
      parameterId: String(parameter.id),
      parameterName: parameter.parameterName || '',
      unit: parameter.unit || '',
      standardMin: parameter.standardMin,
      standardMax: parameter.standardMax,
      referenceStandard: parameter.referenceStandard || '',
      methodId: method?.id ? String(method.id) : '',
      methodName: method?.methodName || '',
      sampleVolume: method?.sampleVolume || method?.sample_volume || ''
    }
  }).filter(Boolean)
}

async function submitSampleLogin() {
  if (!loginForm.taskId || !loginForm.pointName || !loginForm.sampleType || !loginForm.detectionTypeId || !loginForm.reviewFlowId || !loginForm.samplingTime) {
    ElMessage.warning('请完整填写样品登录信息')
    return
  }
  if (!loginForm.detectionConfigItems.length || loginForm.detectionConfigItems.some((item) => !item.methodId)) {
    ElMessage.warning('检测套餐缺少检测参数或检测方法')
    return
  }
  submitting.value = true
  try {
    const sample = await loginSampleApi({
      ...loginForm,
      detectionConfigItems: loginForm.detectionConfigItems
    })
    ElMessage.success(`样品登录完成：${sample?.sampleNo || loginForm.sampleNo || '-'}`)
    await reloadActiveAction()
  } finally {
    submitting.value = false
  }
}

function formatTaskLabel(task) {
  if (!task) {
    return '-'
  }
  return `${task.taskNo || '-'} / ${task.sampleNo || '-'} / ${task.pointName || '-'}`
}

function openResultForm(row) {
  resultForm.id = row.id
  resultForm.recordId = row.recordId
  resultForm.sampleId = row.sampleId
  resultForm.sampleNo = row.sampleNo || ''
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
  resultForm.remark = row.remark || ''
  resultForm.itemStatus = row.itemStatus || ''
}

function formatStandardRange(min, max, unit) {
  const suffix = unit ? ` ${unit}` : ''
  if (min != null && max != null) {
    return `${min} ~ ${max}${suffix}`
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

async function submitDetectionResult() {
  if (!resultForm.sampleId || !resultForm.detectionTypeId || !resultForm.recordId || !resultForm.id) {
    ElMessage.warning('当前检测子流程缺少必要信息')
    return
  }
  if (resultForm.resultValue == null || resultForm.resultValue === '') {
    ElMessage.warning('请填写检测结果')
    return
  }
  if (isResultValueAbnormal(resultForm)) {
    ElMessage.warning(`检测结果存在异常，禁止录入：${formatStandardRange(resultForm.standardMin, resultForm.standardMax, resultForm.unit)}`)
    return
  }
  submitting.value = true
  try {
    await submitDetectionApi({
      recordId: resultForm.recordId,
      itemId: resultForm.id,
      sampleId: resultForm.sampleId,
      detectionTypeId: resultForm.detectionTypeId,
      detectionTypeName: resultForm.detectionTypeName,
      abnormalRemark: resultForm.abnormalRemark,
      remark: resultForm.remark,
      items: [{
        parameterId: resultForm.parameterId,
        parameterName: resultForm.parameterName,
        standardMin: resultForm.standardMin,
        standardMax: resultForm.standardMax,
        resultValue: resultForm.resultValue,
        unit: resultForm.unit
      }]
    })
    ElMessage.success('检测结果已提交')
    await reloadActiveAction()
  } finally {
    submitting.value = false
  }
}

async function openReviewForm(row) {
  const detail = await fetchDetectionDetailApi(row.id || row.detectionRecordId)
  const items = detail?.items || []
  reviewForm.detectionRecordId = row.id || row.detectionRecordId
  reviewForm.sampleNo = row.sampleNo || detail?.record?.sampleNo || ''
  reviewForm.detectionTypeName = row.detectionTypeName || detail?.record?.detectionTypeName || ''
  reviewForm.reviewRemark = ''
  reviewForm.items = items.map((item) => ({
    id: item.id,
    parameterName: item.parameterName || '',
    methodName: item.methodName || '',
    resultValue: item.resultValue == null ? null : Number(item.resultValue),
    itemStatus: item.itemStatus || '',
    reviewResultDraft: item.itemStatus === approvedDetectionStatus ? approvedReviewResult : '',
    rejectReasonDraft: '',
    reviewRemarkDraft: ''
  }))
}

function setAllReviewResult(result) {
  reviewForm.items.forEach((item) => {
    if (item.itemStatus === reviewPendingDetectionStatus) {
      item.reviewResultDraft = result
      if (result === rejectedReviewResult && !item.rejectReasonDraft) {
        item.rejectReasonDraft = '审核不通过，请重新检测并提交结果'
      }
    }
  })
}

async function submitReviewDecision() {
  const pendingItems = reviewForm.items.filter((item) => item.itemStatus === reviewPendingDetectionStatus)
  if (!pendingItems.length) {
    ElMessage.warning('当前主流程下没有待审核子流程')
    return
  }
  const undecidedItem = pendingItems.find((item) => !item.reviewResultDraft)
  if (undecidedItem) {
    ElMessage.warning(`请先选择 ${undecidedItem.parameterName || '检测参数'} 的审核结果`)
    return
  }
  const missingRejectReason = pendingItems.find((item) => item.reviewResultDraft === rejectedReviewResult && !item.rejectReasonDraft)
  if (missingRejectReason) {
    ElMessage.warning(`请填写 ${missingRejectReason.parameterName || '检测参数'} 的驳回原因`)
    return
  }
  submitting.value = true
  try {
    const anyRejected = pendingItems.some((item) => item.reviewResultDraft === rejectedReviewResult)
    await submitReviewApi({
      detectionRecordId: reviewForm.detectionRecordId,
      reviewResult: anyRejected ? rejectedReviewResult : approvedReviewResult,
      reviewRemark: reviewForm.reviewRemark,
      items: pendingItems.map((item) => ({
        itemId: item.id,
        reviewResult: item.reviewResultDraft,
        rejectReason: item.rejectReasonDraft,
        reviewRemark: item.reviewRemarkDraft
      }))
    })
    ElMessage.success(anyRejected ? '审核已提交，主流程已退回' : '审核已提交，主流程全部通过')
    await reloadActiveAction()
  } finally {
    submitting.value = false
  }
}

async function openReportPreview(row) {
  previewData.value = null
  previewError.value = ''
  try {
    previewData.value = await fetchReportPreviewDataApi(row.id)
  } catch (error) {
    previewError.value = error?.message || '报告预览失败'
  }
}

async function reloadActiveAction() {
  await Promise.all([refreshDashboard(), loadWorkbenchPreviewRows(), loadActionRows(activeAction.value)])
  if (actionRows.value.length) {
    await selectActionRow(actionRows.value[0])
  } else {
    activeRow.value = null
  }
}

function resetWorkbenchDialog() {
  activeAction.value = ''
  actionRows.value = []
  activeRow.value = null
  previewData.value = null
  previewError.value = ''
}

onMounted(async () => {
  loading.value = true
  try {
    await Promise.all([refreshDashboard(), loadWorkbenchPreviewRows(), loadDictOptions()])
  } catch (error) {
    dashboard.value = {}
    console.warn('工作台数据加载失败，已使用空数据兜底。', error)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.dashboard-page {
  min-height: 100%;
  gap: 16px;
}

.workbench-hero {
  display: grid;
  grid-template-columns: minmax(180px, 260px) minmax(360px, 1fr) auto;
  gap: 22px;
  align-items: center;
  padding: 18px 22px;
  border: 1px solid rgba(47, 111, 159, 0.18);
  background:
    radial-gradient(circle at 5% 0%, rgba(47, 111, 159, 0.16), transparent 34%),
    linear-gradient(135deg, #ffffff 0%, #f6fbff 55%, #edf6ff 100%);
}

.workbench-hero h3 {
  margin: 0;
  color: var(--text-main);
  font-size: 28px;
}

.workbench-hero__flow {
  min-width: 0;
}

.workbench-hero__flow > span {
  display: block;
  margin-bottom: 10px;
  color: var(--text-sub);
  font-size: 14px;
}

.workbench-hero__flow-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.workbench-hero__flow-chips button {
  min-height: 34px;
  padding: 0 14px;
  border: 1px solid rgba(22, 119, 255, 0.18);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.74);
  color: #173b67;
  font: inherit;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: border-color 0.18s ease, color 0.18s ease, box-shadow 0.18s ease;
}

.workbench-hero__flow-chips button:hover {
  border-color: rgba(22, 119, 255, 0.34);
  color: var(--brand);
  box-shadow: 0 8px 18px rgba(17, 54, 99, 0.08);
}

.workbench-hero__flow-chips button:disabled {
  cursor: default;
  opacity: 1;
}

.workbench-hero__stats {
  display: grid;
  grid-template-columns: repeat(2, 128px);
  gap: 12px;
}

.workbench-hero__stats span {
  display: grid;
  gap: 8px;
  min-height: 92px;
  place-items: center;
  padding: 14px 12px;
  border: 1px solid rgba(214, 225, 241, 0.92);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.76);
  box-shadow: 0 12px 24px rgba(17, 54, 99, 0.05);
}

.workbench-hero__stats strong {
  color: #1769ff;
  font-size: 30px;
  line-height: 1;
}

.workbench-hero__stats em {
  color: #3d5878;
  font-size: 13px;
  font-style: normal;
}

.workbench-guide button,
.workbench-row-card,
.todo-detail-card {
  border: 0;
  font: inherit;
  cursor: pointer;
}

.todo-preview-panel {
  padding: 18px;
}

.panel-head {
  margin-bottom: 14px;
}

.section-title {
  margin: 0;
  font-size: 18px;
}

.panel-head p {
  margin: 4px 0 0;
  color: var(--text-sub);
  font-size: 13px;
}

.workbench-guide {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.workbench-guide button {
  min-width: 128px;
  padding: 12px 18px;
  border: 1px solid rgba(22, 119, 255, 0.18);
  border-radius: 12px;
  background: var(--brand-soft);
  color: var(--brand);
  font-weight: 800;
}

.todo-detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.todo-detail-panel {
  display: flex;
  flex-direction: column;
  height: 460px;
  min-height: 290px;
  overflow: hidden;
  padding: 16px;
  border: 1px solid rgba(214, 225, 241, 0.86);
}

.todo-detail-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0 2px 12px;
  border-bottom: 1px solid #edf2f7;
}

.todo-detail-panel__head h3 {
  margin: 0;
  color: var(--text-main);
  font-size: 16px;
}

.todo-detail-panel__head > strong {
  min-width: 38px;
  padding: 6px 10px;
  border-radius: 999px;
  background: var(--brand-soft);
  color: var(--brand);
  font-size: 18px;
  line-height: 1;
  text-align: center;
}

.todo-detail-list {
  display: grid;
  gap: 10px;
  flex: 1;
  min-height: 0;
  margin-top: 12px;
  overflow-y: auto;
  padding-right: 4px;
  scrollbar-width: thin;
  scrollbar-color: rgba(100, 116, 139, 0.32) transparent;
}

.todo-detail-list::-webkit-scrollbar {
  width: 6px;
}

.todo-detail-list::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(100, 116, 139, 0.28);
}

.todo-detail-list::-webkit-scrollbar-track {
  background: transparent;
}

.todo-detail-card {
  position: relative;
  display: grid;
  align-content: center;
  gap: 9px;
  height: 112px;
  padding: 13px 14px 13px 18px;
  border: 1px solid rgba(214, 225, 241, 0.92);
  border-radius: 12px;
  background: #ffffff;
  text-align: left;
  box-shadow: 0 8px 18px rgba(17, 54, 99, 0.035);
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.todo-detail-card::before {
  content: "";
  position: absolute;
  left: 0;
  top: 12px;
  bottom: 12px;
  width: 4px;
  border-radius: 0 999px 999px 0;
  background: var(--brand);
}

.todo-detail-card--sampleLogin::before {
  background: var(--success);
}

.todo-detail-card--samplingPlan::before {
  background: var(--brand);
}

.todo-detail-card--detection::before,
.todo-detail-card--review::before {
  background: var(--warning);
}

.todo-detail-card--report::before {
  background: #64748b;
}

.todo-detail-card:hover {
  border-color: rgba(22, 119, 255, 0.35);
  box-shadow: 0 12px 24px rgba(17, 54, 99, 0.08);
  transform: translateY(-1px);
}

.todo-detail-card__main {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 4px;
}

.todo-detail-card__main span {
  overflow: hidden;
  color: var(--text-main);
  font-size: 15px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.todo-detail-card__main p {
  overflow: hidden;
  margin: 0;
  color: var(--text-sub);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.todo-detail-card__fields {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  min-width: 0;
}

.todo-detail-card__fields span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  min-width: 0;
  max-width: 100%;
  padding: 0;
}

.todo-detail-card__fields em {
  color: var(--text-sub);
  font-size: 12px;
  font-style: normal;
  white-space: nowrap;
}

.todo-detail-card__fields strong {
  overflow: hidden;
  color: var(--text-main);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.todo-detail-card__fields .status-chip {
  min-width: 0;
  min-height: 22px;
  padding: 0 9px;
  font-size: 12px;
  line-height: 22px;
}

.todo-detail-empty {
  display: grid;
  flex: 1;
  min-height: 160px;
  place-items: center;
  color: var(--text-sub);
}

.workbench-action-dialog :deep(.el-dialog__body) {
  padding-top: 10px;
}

.workbench-dialog {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 16px;
  min-height: 520px;
}

.workbench-dialog__side {
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 12px;
  border: 1px solid rgba(214, 225, 241, 0.9);
  border-radius: 14px;
  background: #f8fbff;
}

.workbench-dialog__side-head {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
  color: var(--text-main);
  font-weight: 800;
}

.workbench-row-list {
  display: grid;
  gap: 8px;
  overflow-y: auto;
}

.workbench-row-card {
  display: grid;
  gap: 6px;
  padding: 12px;
  border: 1px solid rgba(214, 225, 241, 0.9);
  border-radius: 12px;
  background: #ffffff;
  text-align: left;
}

.workbench-row-card.is-active {
  border-color: rgba(22, 119, 255, 0.42);
  background: var(--brand-soft);
}

.workbench-row-card strong {
  color: var(--text-main);
  font-size: 14px;
}

.workbench-row-card span {
  color: var(--text-sub);
  font-size: 12px;
  line-height: 1.5;
}

.workbench-dialog__main {
  min-width: 0;
  overflow-y: auto;
  padding-right: 4px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 4px 14px;
}

.form-span-2 {
  grid-column: span 2;
}

.summary-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 14px;
}

.summary-chips span {
  padding: 8px 12px;
  border-radius: 999px;
  background: #f6f9fc;
  color: var(--text-sub);
}

.summary-chips strong {
  margin-left: 6px;
  color: var(--text-main);
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 14px;
}

.meta-grid div {
  display: grid;
  gap: 6px;
  padding: 12px;
  border: 1px solid rgba(214, 225, 241, 0.9);
  border-radius: 12px;
  background: #ffffff;
}

.meta-grid span {
  color: var(--text-sub);
  font-size: 12px;
}

.meta-grid strong {
  color: var(--text-main);
  font-size: 13px;
}

.result-value-field {
  display: flex;
  align-items: center;
  gap: 10px;
}

.result-value-input {
  width: 220px;
}

.result-value-unit {
  color: var(--text-sub);
}

.compact-table {
  margin-top: 12px;
}

.review-toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}

.review-remark-form {
  margin-top: 12px;
}

.empty-block {
  display: grid;
  min-height: 160px;
  place-items: center;
  color: var(--text-sub);
}

@media (max-width: 1280px) {
  .workbench-hero {
    grid-template-columns: 1fr;
  }

  .workbench-hero__stats {
    grid-template-columns: repeat(2, minmax(120px, 1fr));
  }

  .todo-detail-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .workbench-dialog {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .workbench-hero__stats {
    grid-template-columns: 1fr;
  }

  .todo-detail-grid {
    grid-template-columns: 1fr;
  }

  .todo-detail-card {
    height: 132px;
  }
}
</style>
