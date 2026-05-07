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
          <h3 class="section-title">监测点位</h3>
          <p class="page-subtitle">统一维护监测点位基础信息，便于采样计划和样品流转时快速选择。</p>
        </div>
      </div>

      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="输入点位名称查询" clearable style="max-width: 280px" />
        <el-select v-model="query.pointStatus" placeholder="状态" clearable style="width: 180px">
          <el-option label="启用" value="ENABLED" />
          <el-option label="禁用" value="DISABLED" />
        </el-select>
        <el-button type="primary" @click="loadData">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
        <el-button type="primary" plain @click="openDialog">新增点位</el-button>
      </div>

      <div class="table-card">
        <el-table :data="records" stripe empty-text="暂无监测点位数据">
          <el-table-column prop="pointName" label="点位名称" min-width="180" />
          <el-table-column prop="regionName" label="所属区域" min-width="160" />
          <el-table-column label="点位类型" width="120">
            <template #default="{ row }">
              {{ getEnumLabel(pointTypeLabelMap, row.pointType) }}
            </template>
          </el-table-column>
          <el-table-column label="监测频次" width="120">
            <template #default="{ row }">
              {{ getEnumLabel(frequencyTypeLabelMap, row.frequencyType) }}
            </template>
          </el-table-column>
          <el-table-column prop="ownerName" label="负责人" width="120" />
          <el-table-column prop="contactPhone" label="联系电话" width="140" />
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <span class="status-chip" :class="getStatusClass('pointStatus', row.pointStatus)">
                {{ getEnumLabel(pointStatusLabelMap, row.pointStatus) }}
              </span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </section>

    <el-dialog v-model="dialogVisible" title="新增监测点位" width="640px">
      <el-form :model="form" label-width="100px">
        <div class="form-grid">
          <el-form-item label="点位名称"><el-input v-model="form.pointName" /></el-form-item>
          <el-form-item label="所属区域"><el-input v-model="form.regionName" /></el-form-item>
          <el-form-item label="负责人"><el-input v-model="form.ownerName" /></el-form-item>
          <el-form-item label="联系电话"><el-input v-model="form.contactPhone" /></el-form-item>
          <el-form-item label="点位类型">
            <el-select v-model="form.pointType" style="width: 100%">
              <el-option label="出厂水" value="FACTORY" />
              <el-option label="原水" value="RAW" />
              <el-option label="管网末梢" value="TERMINAL" />
            </el-select>
          </el-form-item>
          <el-form-item label="监测频次">
            <el-select v-model="form.frequencyType" style="width: 100%">
              <el-option label="每日" value="DAILY" />
              <el-option label="每周" value="WEEKLY" />
              <el-option label="每月" value="MONTHLY" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="form.pointStatus" style="width: 100%">
              <el-option label="启用" value="ENABLED" />
              <el-option label="禁用" value="DISABLED" />
            </el-select>
          </el-form-item>
          <el-form-item label="服务人口">
            <el-input-number v-model="form.servicePopulation" :min="0" style="width: 100%" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createMonitoringPointApi, fetchMonitoringPointsApi } from '../api/lab'
import {
  frequencyTypeLabelMap,
  getEnumLabel,
  getStatusClass,
  pointStatusLabelMap,
  pointTypeLabelMap
} from '../utils/labEnums'

const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', pointStatus: '' })
const records = ref([])
const dialogVisible = ref(false)
const form = reactive({
  pointName: '',
  regionName: '',
  ownerName: '',
  contactPhone: '',
  pointType: 'FACTORY',
  frequencyType: 'DAILY',
  pointStatus: 'ENABLED',
  servicePopulation: 0
})

const stats = computed(() => [
  { label: '点位总数', value: records.value.length, desc: '当前页已加载监测点位数量' },
  { label: '启用点位', value: records.value.filter((item) => item.pointStatus === 'ENABLED').length, desc: '当前状态为启用的点位' },
  { label: '禁用点位', value: records.value.filter((item) => item.pointStatus === 'DISABLED').length, desc: '已暂停使用的点位数量' },
  { label: '出厂水点位', value: records.value.filter((item) => item.pointType === 'FACTORY').length, desc: '用于出厂水检测的点位' }
])

function openDialog() {
  dialogVisible.value = true
}

function resetQuery() {
  query.keyword = ''
  query.pointStatus = ''
  loadData()
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

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

@media (max-width: 860px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
