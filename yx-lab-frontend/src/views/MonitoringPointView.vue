<template>
  <div class="content-grid">
    <div class="glass-panel section-block">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="输入点位名称查询" clearable style="max-width: 260px" />
        <el-select v-model="query.pointStatus" placeholder="状态" clearable style="width: 160px">
          <el-option label="启用" value="ENABLED" />
          <el-option label="禁用" value="DISABLED" />
        </el-select>
        <el-button type="primary" @click="loadData">查询</el-button>
        <el-button @click="openDialog">新增点位</el-button>
      </div>
      <div class="table-card">
        <el-table :data="records" stripe>
          <el-table-column prop="pointName" label="点位名称" />
          <el-table-column prop="regionName" label="所属区域" />
          <el-table-column prop="pointType" label="点位类型" />
          <el-table-column prop="frequencyType" label="监测频次" />
          <el-table-column prop="ownerName" label="负责人" />
          <el-table-column label="状态">
            <template #default="{ row }">
              <span class="status-chip" :class="row.pointStatus === 'ENABLED' ? 'success' : 'warning'">
                {{ row.pointStatus }}
              </span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
    <el-dialog v-model="dialogVisible" title="新增监测点位" width="560px">
      <el-form :model="form" label-width="96px">
        <el-form-item label="点位名称"><el-input v-model="form.pointName" /></el-form-item>
        <el-form-item label="区域"><el-input v-model="form.regionName" /></el-form-item>
        <el-form-item label="负责人"><el-input v-model="form.ownerName" /></el-form-item>
        <el-form-item label="点位类型"><el-input v-model="form.pointType" placeholder="RAW / FACTORY / TERMINAL" /></el-form-item>
        <el-form-item label="状态"><el-input v-model="form.pointStatus" placeholder="ENABLED" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createMonitoringPointApi, fetchMonitoringPointsApi } from '../api/lab'

const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', pointStatus: '' })
const records = ref([])
const dialogVisible = ref(false)
const form = reactive({ pointName: '', regionName: '', ownerName: '', pointType: 'FACTORY', pointStatus: 'ENABLED' })

function openDialog() {
  dialogVisible.value = true
}

async function loadData() {
  const result = await fetchMonitoringPointsApi(query)
  records.value = result.records || []
}

async function submit() {
  await createMonitoringPointApi(form)
  ElMessage.success('保存成功')
  dialogVisible.value = false
  loadData()
}

onMounted(loadData)
</script>
