<template>
  <div class="content-grid">
    <div class="glass-panel section-block">
      <div class="toolbar">
        <el-button type="primary" @click="loadReports">刷新报告</el-button>
        <el-button @click="createTemplate">新增模板</el-button>
      </div>
      <el-table :data="reports" stripe>
        <el-table-column prop="reportName" label="报告名称" />
        <el-table-column prop="sampleNo" label="样品编号" />
        <el-table-column prop="reportStatus" label="状态" />
        <el-table-column prop="generatedTime" label="生成时间" />
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button size="small" @click="publish(row.id)">发布</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createTemplateApi, fetchReportsApi, publishReportApi } from '../api/lab'

const reports = ref([])

async function loadReports() {
  reports.value = (await fetchReportsApi({ pageNum: 1, pageSize: 10 })).records || []
}

async function createTemplate() {
  await createTemplateApi({
    reportType: 'MONTHLY',
    templateName: '月报模板',
    defaultTemplate: 0,
    templateContent: '月报模板内容：${sampleNo} - ${detectionResult}'
  })
  ElMessage.success('模板已创建')
}

async function publish(id) {
  await publishReportApi(id)
  ElMessage.success('报告已发布')
  loadReports()
}

onMounted(loadReports)
</script>
