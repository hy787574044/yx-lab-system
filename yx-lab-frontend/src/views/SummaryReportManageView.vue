<template>
  <div class="content-grid summary-report-page fixed-table-page">
    <section class="glass-panel section-block fixed-table-section">
      <div class="section-head">
        <div>
          <h3 class="section-title">{{ pageConfig.title }}</h3>
          <p class="section-desc">{{ pageConfig.desc }}</p>
        </div>
      </div>

      <section class="stats-grid section-stats">
        <article
          v-for="item in summaryCards"
          :key="item.key"
          class="metric-card metric-card--static"
        >
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </article>
      </section>

      <div class="toolbar-panel">
        <div class="toolbar-row">
          <div class="toolbar-main">
            <div class="toolbar-fields">
              <label class="toolbar-field toolbar-field--plant">
                <span>所属机构</span>
                <el-select
                  v-model="query.orgId"
                  clearable
                  filterable
                  placeholder="请选择所属机构"
                >
                  <el-option
                    v-for="option in orgOptions"
                    :key="option.value"
                    :label="option.label"
                    :value="option.value"
                  />
                </el-select>
              </label>
              <label v-if="showDailyReportType" class="toolbar-field toolbar-field--daily-type">
                <span>日报类型</span>
                <el-select v-model="query.dailyReportType" placeholder="请选择日报类型" clearable>
                  <el-option
                    v-for="option in dailyReportTypeOptions"
                    :key="option.value"
                    :label="option.label"
                    :value="option.value"
                  />
                </el-select>
              </label>
              <label class="toolbar-field toolbar-field--date">
                <span>采样日期</span>
                <el-date-picker
                  v-model="query.dateRange"
                  class="summary-date-range"
                  type="daterange"
                  value-format="YYYY-MM-DD"
                  range-separator="至"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                  popper-class="summary-date-range-popper"
                  unlink-panels
                />
              </label>
              <label class="toolbar-field toolbar-field--status">
                <span>报表状态</span>
                <el-select v-model="query.reportStatus" placeholder="请选择状态" clearable>
                  <el-option
                    v-for="option in reportStatusOptions"
                    :key="option.value"
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
            <el-button :loading="exporting" @click="handleExportList">导出列表</el-button>
          </div>
        </div>
      </div>

      <div class="table-card table-card--fixed-scroll ledger-table-card">
        <div class="ledger-table-card__body">
          <el-table
            v-loading="loading"
            class="list-table"
            :data="records"
            stripe
            height="100%"
            empty-text="暂无汇总报表数据"
          >
            <el-table-column prop="reportName" label="报表名称" min-width="230" />
            <el-table-column prop="regionName" label="所属机构" min-width="150" />
            <el-table-column
              v-if="showDailyReportType"
              prop="dailyReportTypeLabel"
              label="日报类型"
              width="120"
              header-cell-class-name="cell-center"
              class-name="cell-center"
            />
            <el-table-column prop="periodLabel" label="周期范围" min-width="180" />
            <el-table-column prop="pointCount" label="检测点数" width="100" header-cell-class-name="cell-center" class-name="cell-center" />
            <el-table-column prop="taskCount" label="任务数" width="90" header-cell-class-name="cell-center" class-name="cell-center" />
            <el-table-column prop="sampleCount" label="样品数" width="90" header-cell-class-name="cell-center" class-name="cell-center" />
            <el-table-column prop="detectedTaskCount" label="已出结果" width="100" header-cell-class-name="cell-center" class-name="cell-center" />
            <el-table-column prop="resultItemCount" label="结果明细数" width="110" header-cell-class-name="cell-center" class-name="cell-center" />
            <el-table-column label="报表状态" width="120" header-cell-class-name="cell-center" class-name="cell-center">
              <template #default="{ row }">
                <el-tag :type="resolveReportStatusTag(row.reportStatus)">{{ row.reportStatusLabel || '-' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="latestSamplingTime" label="最新采样时间" width="170" />
            <el-table-column prop="latestDetectionTime" label="最新检测时间" width="170">
              <template #default="{ row }">
                {{ row.latestDetectionTime || '-' }}
              </template>
            </el-table-column>
            <el-table-column label="操作" min-width="180" fixed="right" header-cell-class-name="cell-center" class-name="cell-center">
              <template #default="{ row }">
                <div class="action-row">
                  <el-button size="small" @click="openPreview(row)">预览</el-button>
                  <el-button
                    size="small"
                    :loading="String(detailExportingKey) === String(row.reportKey)"
                    @click="exportDetail(row)"
                  >
                    导出明细
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
          @change="loadRecords"
        />
      </div>
    </section>

    <el-dialog
      v-model="previewDialogVisible"
      :title="previewTitle"
      width="1400px"
      align-center
      destroy-on-close
      class="summary-preview-dialog"
      @closed="closePreview"
    >
      <div v-if="previewError" class="preview-empty">{{ previewError }}</div>
      <div v-else-if="previewLoading" class="preview-empty">正在加载汇总报表...</div>
      <div v-else-if="previewData" class="preview-shell">
        <div v-if="isInternalRecordPreview" class="template-card">
          <DailyInternalSummaryTemplate :preview-data="previewData" />
        </div>

        <div v-else-if="isWeeklyFactoryPreview" class="template-card">
          <WeeklyFactorySummaryTemplate :preview-data="previewData" />
        </div>

        <div v-else-if="isHalfMonthlyTerminalPreview" class="template-card">
          <HalfMonthlyTerminalSummaryTemplate :preview-data="previewData" />
        </div>

        <div v-else-if="isDailyExternalPreview" class="template-card">
          <DailyExternalSummaryTemplate :preview-data="previewData" />
        </div>

        <div v-else class="table-card table-card--preview">
          <el-table
            class="list-table"
            :data="previewData.rows || []"
            stripe
            max-height="520"
            empty-text="该周期下暂无可展示的汇总明细"
          >
            <el-table-column
              v-if="currentSummaryType === summaryTypes.daily"
              prop="timeBucket"
              label="时间段"
              width="100"
              header-cell-class-name="cell-center"
              class-name="cell-center"
            />
            <el-table-column prop="samplingTimeLabel" label="采样时间" width="170" />
            <el-table-column prop="pointName" label="采样地点" min-width="150" />
            <el-table-column prop="sampleTypeLabel" label="水样类型" width="120" />
            <el-table-column prop="sampleNo" label="样品编号" width="150" />
            <el-table-column prop="sourceTaskCount" label="来源任务数" width="100" header-cell-class-name="cell-center" class-name="cell-center" />
            <el-table-column
              v-for="column in previewColumns"
              :key="column"
              :label="column"
              min-width="120"
            >
              <template #default="{ row }">
                {{ row.parameterValues?.[column] || '-' }}
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
      <template #footer>
        <el-button
          v-if="previewData"
          :loading="previewExporting"
          type="primary"
          plain
          @click="exportPreviewDetail"
        >
          导出当前明细
        </el-button>
        <el-button @click="closePreview">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElButton } from 'element-plus/es/components/button/index.mjs'
import { ElDatePicker } from 'element-plus/es/components/date-picker/index.mjs'
import { ElDialog } from 'element-plus/es/components/dialog/index.mjs'
import { ElLoadingDirective } from 'element-plus/es/components/loading/index.mjs'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElOption, ElSelect } from 'element-plus/es/components/select/index.mjs'
import { ElTable, ElTableColumn } from 'element-plus/es/components/table/index.mjs'
import { ElTag } from 'element-plus/es/components/tag/index.mjs'
import {
  exportSummaryReportDetailApi,
  exportSummaryReportsApi,
  fetchDictItemsApi,
  fetchMonitoringPointOrgOptionsApi,
  fetchSummaryReportsApi,
  previewSummaryReportApi
} from '../api/lab'
import TablePagination from '../components/common/TablePagination.vue'
import DailyExternalSummaryTemplate from '../components/report/DailyExternalSummaryTemplate.vue'
import DailyInternalSummaryTemplate from '../components/report/DailyInternalSummaryTemplate.vue'
import HalfMonthlyTerminalSummaryTemplate from '../components/report/HalfMonthlyTerminalSummaryTemplate.vue'
import WeeklyFactorySummaryTemplate from '../components/report/WeeklyFactorySummaryTemplate.vue'
import { DEFAULT_PAGE_SIZE } from '../utils/labEnums'

const summaryTypes = {
  daily: 'DAILY',
  weekly: 'WEEKLY',
  halfMonthly: 'HALF_MONTHLY'
}

const routeConfigMap = {
  '/report-daily-manage': {
    title: '日报管理',
    shortTitle: '日报',
    desc: '日报按固定模板生成，区分对内日报与对外日报，并填充当日所属水厂、计划时间和检测结果。'
  },
  '/report-weekly-manage': {
    title: '周报管理',
    shortTitle: '周报',
    desc: '按周汇总各水厂每周采样计划，并按固定水质检验记录表展示检测结果。'
  },
  '/report-half-month-manage': {
    title: '半月报管理',
    shortTitle: '半月报',
    desc: '按每月上半月、下半月汇总半月周期计划，并按管网水检测报告模板展示结果。'
  }
}

const reportStatusOptions = [
  { label: '已完成', value: 'COMPLETE' },
  { label: '部分完成', value: 'PARTIAL' },
  { label: '待补全', value: 'PENDING' }
]

const dailyReportTypeOptions = [
  { label: '对内日报', value: 'INTERNAL' },
  { label: '对外日报', value: 'EXTERNAL' }
]

const route = useRoute()
const vLoading = ElLoadingDirective

const query = reactive({
  pageNum: 1,
  pageSize: DEFAULT_PAGE_SIZE,
  orgId: '',
  reportStatus: '',
  dailyReportType: '',
  dateRange: []
})

const currentSummaryType = ref(summaryTypes.daily)
const loading = ref(false)
const exporting = ref(false)
const detailExportingKey = ref('')
const records = ref([])
const total = ref(0)
const orgOptions = ref([])

const previewDialogVisible = ref(false)
const previewLoading = ref(false)
const previewExporting = ref(false)
const previewError = ref('')
const previewTitle = ref('')
const previewData = ref(null)
const currentPreviewParams = ref(null)

const pageConfig = computed(() => routeConfigMap[route.path] || routeConfigMap['/report-daily-manage'])
const previewColumns = computed(() => previewData.value?.parameterColumns || [])
const showDailyReportType = computed(() => currentSummaryType.value === summaryTypes.daily)
const isInternalRecordPreview = computed(() => previewData.value?.previewMode === 'DAILY_INTERNAL_TEMPLATE')
const isDailyExternalPreview = computed(() => previewData.value?.previewMode === 'DAILY_EXTERNAL_TEMPLATE')
const isWeeklyFactoryPreview = computed(() => previewData.value?.previewMode === 'WEEKLY_FACTORY_TEMPLATE')
const isHalfMonthlyTerminalPreview = computed(() => previewData.value?.previewMode === 'HALF_MONTHLY_TERMINAL_TEMPLATE')

const summaryMetrics = computed(() => {
  return (records.value || []).reduce((result, item) => {
    result.taskCount += Number(item.taskCount || 0)
    result.resultItemCount += Number(item.resultItemCount || 0)
    return result
  }, {
    taskCount: 0,
    resultItemCount: 0
  })
})

const summaryCards = computed(() => [
  { key: 'total', label: '报表数量', value: total.value },
  { key: 'taskCount', label: '任务总数', value: summaryMetrics.value.taskCount },
  { key: 'resultItemCount', label: '结果明细数', value: summaryMetrics.value.resultItemCount }
])

function resolveSummaryTypeByPath(path) {
  if (path === '/report-weekly-manage') {
    return summaryTypes.weekly
  }
  if (path === '/report-half-month-manage') {
    return summaryTypes.halfMonthly
  }
  return summaryTypes.daily
}

function buildDefaultDateRange(summaryType) {
  const now = new Date()
  const end = formatDate(now)
  const start = new Date(now)
  if (summaryType === summaryTypes.daily) {
    start.setDate(start.getDate() - 30)
  } else {
    start.setDate(start.getDate() - 180)
  }
  return [formatDate(start), end]
}

function formatDate(date) {
  const year = date.getFullYear()
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${year}-${month}-${day}`
}

function applyRouteState(resetPage = true) {
  currentSummaryType.value = resolveSummaryTypeByPath(route.path)
  query.pageNum = resetPage ? 1 : query.pageNum
  query.dateRange = buildDefaultDateRange(currentSummaryType.value)
  query.orgId = ''
  query.reportStatus = ''
  query.dailyReportType = ''
}

function buildListParams() {
  const params = {
    pageNum: query.pageNum,
    pageSize: query.pageSize,
    summaryType: currentSummaryType.value,
    orgId: query.orgId?.trim() || '',
    reportStatus: query.reportStatus || ''
  }
  if (showDailyReportType.value) {
    params.dailyReportType = query.dailyReportType || ''
  }
  if (Array.isArray(query.dateRange) && query.dateRange.length === 2) {
    params.dateFrom = query.dateRange[0] || ''
    params.dateTo = query.dateRange[1] || ''
  }
  return params
}

function buildPreviewParams(row) {
  const params = {
    summaryType: currentSummaryType.value,
    regionName: row.regionName || '',
    periodStart: row.periodStart,
    periodEnd: row.periodEnd
  }
  if (showDailyReportType.value) {
    params.dailyReportType = row.dailyReportType || query.dailyReportType || ''
  }
  return params
}

function resolveReportStatusTag(status) {
  if (status === 'COMPLETE') {
    return 'success'
  }
  if (status === 'PARTIAL') {
    return 'warning'
  }
  return 'info'
}

async function loadRecords() {
  loading.value = true
  try {
    const result = await fetchSummaryReportsApi(buildListParams())
    records.value = result.records || []
    total.value = Number(result.total || 0)
  } finally {
    loading.value = false
  }
}

function normalizeDictOptions(items) {
  if (!Array.isArray(items)) {
    return []
  }
  return items
    .map((item) => ({
      label: item.label || item.value || '',
      value: item.value || item.label || ''
    }))
    .filter((item) => item.label && item.value)
}

async function loadOrgOptions() {
  try {
    const result = await fetchMonitoringPointOrgOptionsApi()
    orgOptions.value = Array.isArray(result)
      ? result.map((item) => ({ label: item.orgName, value: String(item.id) }))
      : []
  } catch (error) {
    orgOptions.value = []
  }
}

function handleSearch() {
  query.pageNum = 1
  loadRecords()
}

function resetQuery() {
  applyRouteState()
  loadRecords()
}

async function handleExportList() {
  exporting.value = true
  try {
    await exportSummaryReportsApi(buildListParams())
    ElMessage.success(`${pageConfig.value.shortTitle}列表导出成功`)
  } catch (error) {
    ElMessage.error(error?.message || `${pageConfig.value.shortTitle}列表导出失败`)
  } finally {
    exporting.value = false
  }
}

async function openPreview(row) {
  previewDialogVisible.value = true
  previewLoading.value = true
  previewError.value = ''
  previewData.value = null
  previewTitle.value = row.reportName || `${pageConfig.value.shortTitle}预览`
  currentPreviewParams.value = buildPreviewParams(row)
  try {
    previewData.value = await previewSummaryReportApi(currentPreviewParams.value)
  } catch (error) {
    previewError.value = error?.message || '汇总报表预览失败'
  } finally {
    previewLoading.value = false
  }
}

async function exportDetail(row) {
  const params = buildPreviewParams(row)
  detailExportingKey.value = row.reportKey
  try {
    await exportSummaryReportDetailApi(params)
    ElMessage.success('汇总明细导出成功')
  } catch (error) {
    ElMessage.error(error?.message || '汇总明细导出失败')
  } finally {
    detailExportingKey.value = ''
  }
}

async function exportPreviewDetail() {
  if (!currentPreviewParams.value) {
    return
  }
  previewExporting.value = true
  try {
    await exportSummaryReportDetailApi(currentPreviewParams.value)
    ElMessage.success('当前明细导出成功')
  } catch (error) {
    ElMessage.error(error?.message || '当前明细导出失败')
  } finally {
    previewExporting.value = false
  }
}

function closePreview() {
  previewDialogVisible.value = false
  previewLoading.value = false
  previewExporting.value = false
  previewError.value = ''
  previewTitle.value = ''
  previewData.value = null
  currentPreviewParams.value = null
}

onMounted(() => {
  loadOrgOptions()
  applyRouteState()
  loadRecords()
})

watch(() => route.path, () => {
  applyRouteState()
  closePreview()
  loadRecords()
})
</script>

<style scoped>
.summary-report-page {
  height: 100%;
  min-height: 0;
}

.summary-report-page > .section-block {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.section-desc {
  margin: 8px 0 0;
  color: rgba(255, 255, 255, 0.72);
  font-size: 13px;
  line-height: 1.6;
}

.metric-card--static {
  width: 100%;
}

.metric-card--static strong {
  justify-content: flex-end;
}

.summary-report-page .toolbar-row {
  align-items: flex-end;
  flex-wrap: wrap;
}

.summary-report-page .toolbar-main {
  width: auto;
  flex-grow: 1;
  flex-shrink: 0;
  flex-basis: min(100%, 1058px);
  min-width: 0;
}

.summary-report-page .toolbar-fields {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px 12px;
  width: 100%;
  min-width: 0;
}

.summary-report-page .toolbar-field {
  flex: 0 0 auto;
  width: auto;
  min-width: auto;
  justify-content: flex-start;
}

.summary-report-page .toolbar-field > span {
  flex: 0 0 auto;
  width: 52px;
}

.toolbar-field--plant {
  width: 228px;
}

.toolbar-field--plant :deep(.el-select) {
  width: 168px;
}

.toolbar-field--daily-type {
  width: 212px;
}

.toolbar-field--daily-type :deep(.el-select) {
  width: 152px;
}

.toolbar-field--date {
  width: 360px;
  flex: 0 0 360px;
  min-width: auto;
}

.toolbar-field--status {
  width: 204px;
  flex: 0 0 204px;
}

.toolbar-field--status :deep(.el-select) {
  width: 144px;
}

.summary-report-page .toolbar-actions {
  justify-content: flex-end;
  margin-left: auto;
  flex: 0 0 auto;
}

.preview-empty {
  padding: 48px 16px;
  text-align: center;
  color: #6b7280;
}

.preview-shell {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.table-card--preview,
.template-card {
  padding: 12px;
  background: #fff;
  border-radius: 18px;
  border: 1px solid #dce8f6;
}

.toolbar-field--date :deep(.summary-date-range.el-date-editor) {
  flex: 0 0 300px;
  width: 300px;
  max-width: 300px;
  height: 34px;
  min-height: 34px;
  padding: 0 8px;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 0 0 1px #cfd9e6 inset !important;
}

.toolbar-field--date :deep(.summary-date-range.el-date-editor:hover) {
  box-shadow: 0 0 0 1px #69b6fb inset !important;
}

.toolbar-field--date :deep(.summary-date-range.el-date-editor.is-active),
.toolbar-field--date :deep(.summary-date-range.el-date-editor.is-focus) {
  box-shadow: 0 0 0 1px #4da7ff inset !important;
}

.toolbar-field--date :deep(.summary-date-range .el-range__icon) {
  margin-right: 4px;
  color: #7890aa;
}

.toolbar-field--date :deep(.summary-date-range .el-range-input) {
  min-width: 0;
  height: 30px;
  line-height: 30px;
  border: 0;
  outline: 0;
  box-shadow: none;
  background: transparent;
  color: #1f2937;
  font-size: 13px;
  text-align: center;
}

.toolbar-field--date :deep(.summary-date-range .el-range-input:focus) {
  border: 0;
  outline: 0;
  box-shadow: none;
}

.toolbar-field--date :deep(.summary-date-range .el-range-separator) {
  flex: 0 0 20px;
  width: 20px;
  height: 30px;
  padding: 0;
  line-height: 30px;
  color: #64748b;
  font-size: 12px;
}

.toolbar-field--date :deep(.summary-date-range .el-range__close-icon) {
  flex: 0 0 16px;
  width: 16px;
  margin-left: 2px;
  color: #7890aa;
}

:global(.summary-date-range-popper.el-picker__popper) {
  width: auto !important;
  max-width: calc(100vw - 24px);
  border-radius: 8px !important;
  box-shadow: 0 12px 32px rgba(31, 45, 61, 0.14) !important;
}

:global(.summary-date-range-popper .el-date-range-picker) {
  width: 620px !important;
  min-width: 620px !important;
  max-width: calc(100vw - 24px);
}

:global(.summary-date-range-popper .el-picker-panel__body) {
  display: grid !important;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  align-items: stretch;
  min-width: 0 !important;
  width: 100%;
}

:global(.summary-date-range-popper .el-date-range-picker__content) {
  float: none !important;
  width: auto !important;
  min-width: 0;
  padding: 12px 16px 14px;
}

:global(.summary-date-range-popper .el-date-range-picker__content.is-left) {
  border-right: 1px solid #eef2f6;
}

:global(.summary-date-range-popper .el-date-range-picker__content.is-right) {
  border-left: 0;
}

:global(.summary-date-range-popper .el-date-range-picker__header) {
  margin-bottom: 8px;
  padding-bottom: 8px;
}

:global(.summary-date-range-popper .el-date-range-picker__header-label) {
  font-size: 13px;
  font-weight: 700;
  line-height: 20px;
}

:global(.summary-date-range-popper .el-date-table th) {
  padding: 5px 0 7px;
  color: #65758b;
}

:global(.summary-date-range-popper .el-date-table td) {
  padding: 2px 0;
}

:global(.summary-date-range-popper .el-date-table td .el-date-table-cell) {
  height: 28px;
}

:global(.summary-date-range-popper .el-date-table td .el-date-table-cell__text) {
  min-width: 22px;
  height: 22px;
  line-height: 22px;
  font-size: 12px;
  border-radius: 4px;
}

:global(.summary-date-range-popper .el-date-table td.in-range .el-date-table-cell) {
  background: #eaf3ff;
}

:global(.summary-date-range-popper .el-date-table td.start-date .el-date-table-cell__text),
:global(.summary-date-range-popper .el-date-table td.end-date .el-date-table-cell__text) {
  background: #1677ff;
  color: #ffffff;
  box-shadow: 0 4px 10px rgba(22, 119, 255, 0.22);
}

@media (max-width: 1280px) {
  .summary-report-page .toolbar-main,
  .summary-report-page .toolbar-actions {
    flex-basis: 100%;
  }

  .summary-report-page .toolbar-actions {
    justify-content: flex-start;
    margin-left: 0;
  }
}

@media (max-width: 768px) {
  .summary-report-page .toolbar-field,
  .toolbar-field--plant,
  .toolbar-field--daily-type,
  .toolbar-field--date,
  .toolbar-field--status {
    width: 100%;
    flex: 1 1 100%;
  }

  .toolbar-field--plant :deep(.el-select),
  .toolbar-field--daily-type :deep(.el-select),
  .toolbar-field--status :deep(.el-select),
  .toolbar-field--date :deep(.summary-date-range.el-date-editor) {
    width: 100%;
    min-width: 0;
    flex: 1 1 0;
    max-width: none;
  }
}
</style>
