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
          <h3 class="section-title">监测点位</h3>
          <p class="page-subtitle">统一维护监测点位基础信息，便于采样计划和样品流转时快速选择。</p>
        </div>
      </div>

      <div class="toolbar-panel">
        <div class="toolbar-row">
          <div class="toolbar-fields">
            <label class="toolbar-field">
              <span>点位名称</span>
              <el-input v-model="query.keyword" placeholder="请输入点位名称查询" clearable />
            </label>
            <label class="toolbar-field">
              <span>点位状态</span>
              <el-select v-model="query.pointStatus" placeholder="请选择点位状态" clearable>
                <el-option
                  v-for="option in pointStatusOptions"
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
            <el-button @click="loadData">刷新</el-button>
            <el-button type="primary" plain @click="openDialog">新增点位</el-button>
          </div>
        </div>
      </div>

      <div class="table-card">
        <el-table class="list-table" :data="visibleRecords" stripe max-height="420" empty-text="暂无监测点位数据">
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

        <TablePagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          @change="loadData"
        />
      </div>
    </section>

    <el-dialog v-model="dialogVisible" title="新增监测点位" width="640px" @closed="resetForm">
      <el-form :model="form" label-width="100px">
        <div class="form-grid">
          <el-form-item label="点位名称">
            <el-input v-model="form.pointName" />
          </el-form-item>
          <el-form-item label="所属区域">
            <el-input v-model="form.regionName" />
          </el-form-item>
          <el-form-item label="负责人">
            <el-input v-model="form.ownerName" />
          </el-form-item>
          <el-form-item label="联系电话">
            <el-input v-model="form.contactPhone" />
          </el-form-item>
          <el-form-item label="点位类型">
            <el-select v-model="form.pointType" style="width: 100%">
              <el-option
                v-for="option in pointTypeOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="监测频次">
            <el-select v-model="form.frequencyType" style="width: 100%">
              <el-option
                v-for="option in frequencyTypeOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="form.pointStatus" style="width: 100%">
              <el-option
                v-for="option in pointStatusOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
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
import TablePagination from '../components/common/TablePagination.vue'
import {
  DEFAULT_PAGE_SIZE,
  dailyFrequencyType,
  enabledPointStatus,
  factoryPointType,
  frequencyTypeOptions,
  frequencyTypeLabelMap,
  getEnumLabel,
  getStatusClass,
  pointStatusOptions,
  pointStatusLabelMap,
  pointTypeOptions,
  pointTypeLabelMap
} from '../utils/labEnums'

const query = reactive({ pageNum: 1, pageSize: DEFAULT_PAGE_SIZE, keyword: '', pointStatus: '' })
const records = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const activeStatKey = ref('all')

const defaultForm = () => ({
  pointName: '',
  regionName: '',
  ownerName: '',
  contactPhone: '',
  pointType: factoryPointType,
  frequencyType: dailyFrequencyType,
  pointStatus: enabledPointStatus,
  servicePopulation: 0
})

const form = reactive(defaultForm())

const stats = computed(() => [
  { key: 'all', label: '点位总数', value: total.value, desc: '当前监测点位总量' },
  { key: 'page', label: '本页记录', value: records.value.length, desc: '当前分页加载的点位数量' },
  {
    key: 'enabled',
    label: '启用点位',
    value: records.value.filter((item) => item.pointStatus === enabledPointStatus).length,
    desc: '当前页状态为启用的点位'
  },
  {
    key: 'factory',
    label: '出厂水点位',
    value: records.value.filter((item) => item.pointType === factoryPointType).length,
    desc: '当前页出厂水监测点位数量'
  }
])

const visibleRecords = computed(() => {
  if (activeStatKey.value === 'enabled') {
    return records.value.filter((item) => item.pointStatus === enabledPointStatus)
  }
  if (activeStatKey.value === 'factory') {
    return records.value.filter((item) => item.pointType === factoryPointType)
  }
  return records.value
})

function handleStatClick(key) {
  activeStatKey.value = key === activeStatKey.value ? 'all' : key
}

function openDialog() {
  dialogVisible.value = true
}

function handleSearch() {
  query.pageNum = 1
  loadData()
}

function resetQuery() {
  query.pageNum = 1
  query.pageSize = DEFAULT_PAGE_SIZE
  query.keyword = ''
  query.pointStatus = ''
  activeStatKey.value = 'all'
  loadData()
}

function resetForm() {
  Object.assign(form, defaultForm())
}

async function loadData() {
  const result = await fetchMonitoringPointsApi(query)
  records.value = result.records || []
  total.value = result.total || 0
}

async function submit() {
  await createMonitoringPointApi(form)
  ElMessage.success('保存成功')
  dialogVisible.value = false
  resetForm()
  query.pageNum = 1
  await loadData()
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
