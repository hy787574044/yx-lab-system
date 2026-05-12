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
          <h3 class="section-title">报告台账</h3>
          <p class="page-subtitle">统一查看正式报告产物、发布状态、推送结果与留痕信息，并支持在线预览正式报告。</p>
        </div>
      </div>

      <div class="toolbar-panel">
        <div class="toolbar-row">
          <div class="toolbar-fields">
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

          <div class="toolbar-actions">
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="resetQuery">重置</el-button>
            <el-button @click="loadReports" :loading="loading">刷新报告</el-button>
            <el-button @click="handleExport" :loading="loading">导出</el-button>
            <el-button type="primary" plain @click="createTemplate">新增模板</el-button>
          </div>
        </div>
      </div>

      <div class="table-card">
        <el-table
          class="list-table"
          :data="visibleReports"
          stripe
          max-height="460"
          v-loading="loading"
          empty-text="暂无报告台账数据"
        >
          <el-table-column prop="reportName" label="报告名称" min-width="200" />
          <el-table-column prop="sampleNo" label="样品编号" width="150" />
          <el-table-column prop="sealNo" label="封签编号" width="160" />
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
          <el-table-column label="推送状态" width="120">
            <template #default="{ row }">
              <span class="status-chip" :class="getPushStatusClass(row.pushStatus)">
                {{ getPushStatusLabel(row.pushStatus) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="lastPushTime" label="最近推送时间" width="170" />
          <el-table-column prop="lastPushMessage" label="推送结果" min-width="240" show-overflow-tooltip>
            <template #default="{ row }">
              {{ row.lastPushMessage || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="内容摘要" min-width="220" show-overflow-tooltip>
            <template #default="{ row }">
              {{ translateWorkflowText(row.contentSnapshot) || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="220" fixed="right">
            <template #default="{ row }">
              <div class="action-row">
                <el-button size="small" @click="previewReport(row)">预览</el-button>
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
              </div>
            </template>
          </el-table-column>
        </el-table>

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
        <el-button v-if="previewData" type="primary" plain @click="printPreview">打印</el-button>
        <el-button @click="closePreviewDialog">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElButton } from 'element-plus/es/components/button/index.mjs'
import { ElDialog } from 'element-plus/es/components/dialog/index.mjs'
import { ElLoadingDirective } from 'element-plus/es/components/loading/index.mjs'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElOption, ElSelect } from 'element-plus/es/components/select/index.mjs'
import { ElTable, ElTableColumn } from 'element-plus/es/components/table/index.mjs'
import {
  createTemplateApi,
  exportReportsApi,
  fetchReportsApi,
  fetchReportPreviewDataApi,
  publishReportApi,
  unpublishReportApi
} from '../api/lab'
import TablePagination from '../components/common/TablePagination.vue'
import ReportPrintDocument from '../components/report/ReportPrintDocument.vue'
import {
  DEFAULT_PAGE_SIZE,
  generatedReportStatus,
  getEnumLabel,
  getStatusClass,
  monthlyReportType,
  pushStatusLabelMap,
  publishedReportStatus,
  reportStatusLabelMap,
  reportStatusOptions,
  reportTypeLabelMap,
  reportTypeOptions,
  successPushStatus,
  translateWorkflowText
} from '../utils/labEnums'

const query = reactive({
  pageNum: 1,
  pageSize: DEFAULT_PAGE_SIZE,
  reportType: '',
  reportStatus: ''
})

const loading = ref(false)
const reports = ref([])
const total = ref(0)
const activeStatKey = ref('all')
const vLoading = ElLoadingDirective

const previewDialogVisible = ref(false)
const previewData = ref(null)
const previewTitle = ref('')
const previewError = ref('')
const reportPrintRef = ref(null)

const stats = computed(() => [
  { key: 'all', label: '报告总数', value: total.value, desc: '报告台账记录总量' },
  { key: 'page', label: '本页记录', value: reports.value.length, desc: '当前分页加载的报告数量' },
  {
    key: 'generated',
    label: '待发布',
    value: reports.value.filter((item) => item.reportStatus === generatedReportStatus).length,
    desc: '当前页已生成但尚未正式发布的报告'
  },
  {
    key: 'published',
    label: '已发布',
    value: reports.value.filter((item) => item.reportStatus === publishedReportStatus).length,
    desc: '当前页已进入正式发布状态的报告'
  },
  {
    key: 'pushed',
    label: '已推送',
    value: reports.value.filter((item) => item.pushStatus === successPushStatus).length,
    desc: '当前页已完成推送留痕的报告'
  }
])

const visibleReports = computed(() => {
  if (activeStatKey.value === 'generated') {
    return reports.value.filter((item) => item.reportStatus === generatedReportStatus)
  }
  if (activeStatKey.value === 'published') {
    return reports.value.filter((item) => item.reportStatus === publishedReportStatus)
  }
  if (activeStatKey.value === 'pushed') {
    return reports.value.filter((item) => item.pushStatus === successPushStatus)
  }
  return reports.value
})

function handleStatClick(key) {
  activeStatKey.value = key === activeStatKey.value ? 'all' : key
}

function getPushStatusLabel(status) {
  return getEnumLabel(pushStatusLabelMap, status)
}

function getPushStatusClass(status) {
  return getStatusClass('pushStatus', status)
}

function handleSearch() {
  query.pageNum = 1
  activeStatKey.value = 'all'
  loadReports()
}

function resetQuery() {
  query.pageNum = 1
  query.pageSize = DEFAULT_PAGE_SIZE
  query.reportType = ''
  query.reportStatus = ''
  activeStatKey.value = 'all'
  loadReports()
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
    templateContent: '月报模板内容：${sampleNo} / ${sealNo} - ${detectionResult}'
  })
  ElMessage.success('模板已创建。')
}

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

function closePreviewDialog() {
  previewDialogVisible.value = false
  previewData.value = null
  previewTitle.value = ''
  previewError.value = ''
}

onMounted(loadReports)
onBeforeUnmount(() => {
  previewData.value = null
})
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

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
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
