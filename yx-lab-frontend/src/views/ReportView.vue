<template>
  <div class="content-grid report-page">
    <section class="glass-panel section-block">
      <div class="section-head">
        <div>
          <h3 class="section-title">报告台账</h3>
        </div>
      </div>

      <section class="stats-grid section-stats">
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

      <div class="toolbar-panel">
        <div class="toolbar-row">
          <div class="toolbar-main">
            <el-button v-permission="'report:write'" type="primary" class="toolbar-primary-button" @click="createTemplate">新增模板</el-button>
            <div class="toolbar-fields">
              <label class="toolbar-field toolbar-field--wide">
                <span>关键字</span>
                <el-input
                  v-model="query.keyword"
                  clearable
                  placeholder="请输入报告名称或样品编号"
                  @keyup.enter="handleSearch"
                />
              </label>
              <label class="toolbar-field">
                <span>报告类型</span>
                <el-select v-model="query.reportType" placeholder="请选择报告类型" clearable>
                  <el-option
                    v-for="option in reportTypeOptions"
                    :key="option.value"
                    :label="option.label"
                    :value="option.value"
                  />
                </el-select>
              </label>
              <label class="toolbar-field">
                <span>报告状态</span>
                <el-select v-model="query.reportStatus" placeholder="请选择报告状态" clearable>
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
            <el-button @click="handleExport" :loading="loading">导出</el-button>
          </div>
        </div>
      </div>

      <div class="table-card table-card--fixed-scroll ledger-table-card">
        <div class="ledger-table-card__body">
          <el-table
            class="list-table"
            :data="visibleReports"
            stripe
            height="100%"
            v-loading="loading"
            empty-text="暂无报告台账数据"
          >
          <el-table-column prop="reportName" label="报告名称" min-width="200" />
          <el-table-column prop="sampleNo" label="样品编号" width="150" />
          <el-table-column label="报告类型" width="110">
            <template #default="{ row }">
              {{ getEnumLabel(reportTypeLabelMap, row.reportType) }}
            </template>
          </el-table-column>
          <el-table-column label="报告状态" width="110">
            <template #default="{ row }">
              <span class="status-chip" :class="getStatusClass('reportStatus', row.reportStatus)">
                {{ getEnumLabel(reportStatusLabelMap, row.reportStatus) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="generatedTime" label="生成时间" width="170" />
          <el-table-column prop="publishedByName" label="发布人" width="120">
            <template #default="{ row }">
              {{ row.publishedByName || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="publishedTime" label="发布时间" width="170">
            <template #default="{ row }">
              {{ row.publishedTime || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="内容摘要" min-width="220" show-overflow-tooltip>
            <template #default="{ row }">
              {{ translateWorkflowText(row.contentSnapshot) || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="240" fixed="right" header-cell-class-name="cell-center" class-name="cell-center">
            <template #default="{ row }">
              <div class="action-row">
                <el-button v-permission="'report:view'" size="small" @click="previewReport(row)">预览</el-button>
                <el-button
                  size="small"
                  v-permission="'report:view'"
                  :loading="String(downloadingReportId) === String(row.id)"
                  @click="downloadPdf(row)"
                >
                  下载PDF
                </el-button>
                <!-- 报告发布、取消发布功能暂时停用
                <el-button
                  size="small"
                  type="primary"
                  @click="publish(row.id)"
                  :disabled="row.reportStatus === publishedReportStatus"
                >
                  发布
                </el-button>
                <el-button
                  size="small"
                  @click="unpublish(row.id)"
                  :disabled="row.reportStatus !== publishedReportStatus"
                >
                  取消发布
                </el-button>
                -->
              </div>
            </template>
          </el-table-column>
          </el-table>
        </div>

        <TablePagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          @change="loadReports"
        />
      </div>
    </section>

    <el-dialog
      v-model="previewDialogVisible"
      :title="previewTitle"
      width="1360px"
      align-center
      class="report-preview-dialog"
      destroy-on-close
      @closed="closePreviewDialog"
    >
      <div v-if="previewError" class="preview-empty">{{ previewError }}</div>
      <ReportPrintDocument
        v-else-if="previewData"
        ref="reportPrintRef"
        :preview-data="previewData"
      />
      <div v-else class="preview-empty">正在加载报告文档...</div>
      <template #footer>
        <el-button v-if="previewData" v-permission="'report:view'" type="primary" plain @click="printPreview">打印</el-button>
        <el-button @click="closePreviewDialog">关闭</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElButton } from 'element-plus/es/components/button/index.mjs'
import { ElDialog } from 'element-plus/es/components/dialog/index.mjs'
import { ElInput } from 'element-plus/es/components/input/index.mjs'
import { ElLoadingDirective } from 'element-plus/es/components/loading/index.mjs'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElOption, ElSelect } from 'element-plus/es/components/select/index.mjs'
import { ElTable, ElTableColumn } from 'element-plus/es/components/table/index.mjs'
import {
  createTemplateApi,
  downloadReportPdfApi,
  exportReportsApi,
  fetchReportsApi,
  fetchReportPreviewDataApi,
  fetchReportStatsApi
} from '../api/lab'
import TablePagination from '../components/common/TablePagination.vue'
import ReportPrintDocument from '../components/report/ReportPrintDocument.vue'
import {
  DEFAULT_PAGE_SIZE,
  generatedReportStatus,
  getEnumLabel,
  getStatusClass,
  monthlyReportType,
  publishedReportStatus,
  reportStatusLabelMap,
  reportStatusOptions,
  reportTypeLabelMap,
  reportTypeOptions,
  translateWorkflowText
} from '../utils/labEnums'

const query = reactive({
  pageNum: 1,
  pageSize: DEFAULT_PAGE_SIZE,
  keyword: '',
  reportType: '',
  reportStatus: ''
})

const loading = ref(false)
const reports = ref([])
const total = ref(0)
const statCounts = ref({})
const activeStatKey = ref('all')
const vLoading = ElLoadingDirective

const previewDialogVisible = ref(false)
const previewData = ref(null)
const previewTitle = ref('')
const previewError = ref('')
const reportPrintRef = ref(null)
const downloadingReportId = ref('')

function toSafeNumber(value) {
  const num = typeof value === 'number' ? value : Number.parseFloat(String(value ?? '').replace(/,/g, '').trim())
  return Number.isFinite(num) ? num : 0
}

function buildCountMap(items) {
  return (items || []).reduce((result, item) => {
    result[String(item.status || '')] = toSafeNumber(item.count)
    return result
  }, {})
}

function getCount(key) {
  return toSafeNumber(statCounts.value[key])
}

const stats = computed(() => [
  { key: 'all', label: '报告总数', value: getCount('ALL'), desc: '报告台账记录总量' },
  {
    key: 'generated',
    label: '待发布',
    value: getCount(generatedReportStatus),
    desc: '已生成但尚未正式发布的报告'
  },
  {
    key: 'published',
    label: '已发布',
    value: getCount(publishedReportStatus),
    desc: '已进入正式发布状态的报告'
  }
])

const visibleReports = computed(() => {
  if (activeStatKey.value === 'generated') {
    return reports.value.filter((item) => item.reportStatus === generatedReportStatus)
  }
  if (activeStatKey.value === 'published') {
    return reports.value.filter((item) => item.reportStatus === publishedReportStatus)
  }
  return reports.value
})

function handleStatClick(key) {
  const nextKey = key === activeStatKey.value ? 'all' : key
  activeStatKey.value = nextKey
  query.reportStatus = nextKey === 'generated'
    ? generatedReportStatus
    : nextKey === 'published'
      ? publishedReportStatus
      : ''
  query.pageNum = 1
  loadReports()
}

function handleSearch() {
  query.pageNum = 1
  syncActiveStatByQuery()
  loadReports()
}

function resetQuery() {
  query.pageNum = 1
  query.pageSize = DEFAULT_PAGE_SIZE
  query.keyword = ''
  query.reportType = ''
  query.reportStatus = ''
  activeStatKey.value = 'all'
  loadReports()
}

function syncActiveStatByQuery() {
  if (query.reportStatus === generatedReportStatus) {
    activeStatKey.value = 'generated'
  } else if (query.reportStatus === publishedReportStatus) {
    activeStatKey.value = 'published'
  } else {
    activeStatKey.value = 'all'
  }
}

async function loadReports() {
  loading.value = true
  try {
    const result = await fetchReportsApi(query)
    reports.value = result.records || []
    total.value = result.total || 0
  } finally {
    loading.value = false
  }
}

async function loadReportStats() {
  statCounts.value = buildCountMap(await fetchReportStatsApi())
}

async function handleExport() {
  try {
    await exportReportsApi(query)
    ElMessage.success('报告台账导出成功')
  } catch (error) {
    ElMessage.error(error.message || '报告台账导出失败')
  }
}

async function createTemplate() {
  await createTemplateApi({
    reportType: monthlyReportType,
    templateName: '月报模板',
    defaultTemplate: 0,
    templateContent: '月报模板内容：${sampleNo} - ${detectionResult}'
  })
  ElMessage.success('模板已创建。')
  await loadReportStats()
}

/*
async function publish(id) {
  await publishReportApi(id)
  ElMessage.success('报告已发布。')
  query.pageNum = 1
  await loadReports()
}

async function unpublish(id) {
  await unpublishReportApi(id)
  ElMessage.success('报告已取消发布。')
  query.pageNum = 1
  await loadReports()
}
*/

async function previewReport(row) {
  previewData.value = null
  previewTitle.value = row.reportName || '报告预览'
  previewError.value = ''
  previewDialogVisible.value = true
  try {
    previewData.value = await fetchReportPreviewDataApi(row.id)
  } catch (error) {
    previewError.value = error?.message || '报告预览失败'
  }
}

function printPreview() {
  reportPrintRef.value?.printDocument?.()
}

async function downloadPdf(row) {
  downloadingReportId.value = row.id
  try {
    const response = await downloadReportPdfApi(row.id)
    const blobUrl = window.URL.createObjectURL(response.data)
    const disposition = response.headers['content-disposition'] || ''
    const utf8Match = disposition.match(/filename\*=UTF-8''([^;]+)/i)
    const normalMatch = disposition.match(/filename="?([^";]+)"?/i)
    const fileName = utf8Match?.[1]
      ? decodeURIComponent(utf8Match[1])
      : normalMatch?.[1]
        ? decodeURIComponent(normalMatch[1])
        : `${row.reportName || '报告'}.pdf`
    const link = document.createElement('a')
    link.href = blobUrl
    link.download = fileName
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(blobUrl)
    ElMessage.success('PDF 已开始下载')
  } catch (error) {
    ElMessage.error(error?.message || '下载 PDF 失败')
  } finally {
    downloadingReportId.value = ''
  }
}

function closePreviewDialog() {
  previewDialogVisible.value = false
  previewData.value = null
  previewTitle.value = ''
  previewError.value = ''
}

onMounted(() => {
  Promise.all([loadReports(), loadReportStats()])
})
onBeforeUnmount(() => {
  previewData.value = null
})
</script>

<style scoped>
.report-page {
  height: 100%;
  min-height: 0;
}

.report-page > .section-block {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

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

.action-row {
  display: flex;
  flex-wrap: nowrap;
  gap: 8px;
  white-space: nowrap;
}

.ledger-table-card {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.ledger-table-card__body {
  flex: 1 1 auto;
  min-height: 0;
}

.ledger-table-card__body :deep(.el-table) {
  height: 100%;
}

.report-preview-dialog :deep(.el-dialog__body) {
  max-height: calc(100vh - 170px);
  overflow: auto;
  padding: 10px 18px 18px;
  background: #eef3fb;
}

.report-preview-dialog :deep(.el-dialog) {
  max-width: calc(100vw - 32px);
  margin: 0 auto;
}

.preview-empty {
  min-height: 240px;
  display: grid;
  place-items: center;
  color: var(--text-sub);
  text-align: center;
  line-height: 1.8;
}
</style>

