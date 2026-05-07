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
          <h3 class="section-title">报告台账</h3>
          <p class="page-subtitle">统一查看报告状态、来源样品及生成时间，并管理报告发布流转。</p>
        </div>
      </div>

      <div class="toolbar">
        <el-select v-model="query.reportType" placeholder="报告类型" clearable style="width: 180px">
          <el-option
            v-for="option in reportTypeOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
        <el-select v-model="query.reportStatus" placeholder="报告状态" clearable style="width: 180px">
          <el-option
            v-for="option in reportStatusOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
        <el-button type="primary" @click="loadReports">刷新报告</el-button>
        <el-button type="primary" plain @click="createTemplate">新增模板</el-button>
      </div>

      <div class="table-card">
        <el-table class="list-table" :data="reports" stripe max-height="420" empty-text="暂无报告台账数据">
          <el-table-column prop="reportName" label="报告名称" min-width="180" />
          <el-table-column prop="sampleNo" label="样品编号" width="150" />
          <el-table-column prop="sealNo" label="封签编号" width="170" />
          <el-table-column label="报告类型" width="120">
            <template #default="{ row }">
              {{ getEnumLabel(reportTypeLabelMap, row.reportType) }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <span class="status-chip" :class="getStatusClass('reportStatus', row.reportStatus)">
                {{ getEnumLabel(reportStatusLabelMap, row.reportStatus) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="generatedTime" label="生成时间" width="170" />
          <el-table-column prop="contentSnapshot" label="内容摘要" min-width="220" show-overflow-tooltip />
          <el-table-column label="操作" min-width="180" fixed="right">
            <template #default="{ row }">
              <div class="action-row">
                <el-button
                  size="small"
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
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createTemplateApi, fetchReportsApi, publishReportApi, unpublishReportApi } from '../api/lab'
import TablePagination from '../components/common/TablePagination.vue'
import {
  DEFAULT_PAGE_SIZE,
  generatedReportStatus,
  getEnumLabel,
  getStatusClass,
  monthlyReportType,
  publishedReportStatus,
  reportStatusLabelMap,
  reportStatusOptions,
  reportTypeOptions,
  reportTypeLabelMap
} from '../utils/labEnums'

const query = reactive({
  pageNum: 1,
  pageSize: DEFAULT_PAGE_SIZE,
  reportType: '',
  reportStatus: ''
})
const reports = ref([])
const total = ref(0)

const stats = computed(() => [
  { label: '报告总数', value: total.value, desc: '报告记录总量' },
  { label: '本页记录', value: reports.value.length, desc: '当前分页加载的报告记录' },
  { label: '已生成', value: reports.value.filter((item) => item.reportStatus === generatedReportStatus).length, desc: '当前页已生成但未发布的报告' },
  { label: '已发布', value: reports.value.filter((item) => item.reportStatus === publishedReportStatus).length, desc: '当前页已发布报告' },
  { label: '月报数量', value: reports.value.filter((item) => item.reportType === monthlyReportType).length, desc: '当前页月报类报告数量' }
])

function handleSearch() {
  query.pageNum = 1
  loadReports()
}

function resetQuery() {
  query.pageNum = 1
  query.pageSize = DEFAULT_PAGE_SIZE
  query.reportType = ''
  query.reportStatus = ''
  loadReports()
}

async function loadReports() {
    const result = await fetchReportsApi(query)
  reports.value = result.records || []
  total.value = result.total || 0
}

async function createTemplate() {
  await createTemplateApi({
    reportType: monthlyReportType,
    templateName: '月报模板',
    defaultTemplate: 0,
    templateContent: '月报模板内容：${sampleNo} / ${sealNo} - ${detectionResult}'
  })
  ElMessage.success('模板已创建')
}

async function publish(id) {
  await publishReportApi(id)
  ElMessage.success('报告已发布')
  query.pageNum = 1
  await loadReports()
}

async function unpublish(id) {
  await unpublishReportApi(id)
  ElMessage.success('报告已取消发布')
  query.pageNum = 1
  await loadReports()
}

onMounted(loadReports)
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

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
