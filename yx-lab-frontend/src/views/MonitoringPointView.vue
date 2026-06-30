<template>
  <div class="content-grid fixed-table-page">
    <section class="glass-panel section-block fixed-table-section">
      <div class="section-head">
        <div>
          <h3 class="section-title">监测点位</h3>
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
              <el-button type="primary" class="toolbar-primary-button" @click="openCreateDialog">新增点位</el-button>
              <div class="toolbar-fields">
                <label class="toolbar-field">
                  <span>所属机构</span>
                  <el-select v-model="query.orgId" placeholder="请选择所属机构" clearable filterable>
                    <el-option
                      v-for="option in orgOptions"
                    :key="option.value"
                    :label="option.label"
                    :value="option.value"
                  />
                </el-select>
                </label>
                <label class="toolbar-field">
                  <span>点位名称</span>
                  <el-input v-model="query.keyword" placeholder="请输入点位名称查询" clearable />
                </label>
                <label class="toolbar-field">
                  <span>点位类型</span>
                  <el-select v-model="query.pointType" placeholder="请选择点位类型" clearable>
                    <el-option
                      v-for="option in pointTypeOptions"
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
            <el-button @click="handleExport">导出</el-button>
          </div>
        </div>
      </div>

      <div class="table-card table-card--fixed-scroll">
        <div class="table-card__body">
        <el-table class="list-table" :data="visibleRecords" stripe height="100%" empty-text="暂无监测点位数据">
          <el-table-column prop="pointName" label="点位名称" min-width="180" />
          <el-table-column prop="address" label="地图位置" min-width="220" />
          <el-table-column prop="orgName" label="所属机构" min-width="160">
            <template #default="{ row }">{{ row.orgName || '-' }}</template>
          </el-table-column>
          <el-table-column label="点位类型" width="120">
            <template #default="{ row }">
              {{ getEnumLabel(pointTypeLabelMap, row.pointType) }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <span class="status-chip" :class="getStatusClass('pointStatus', row.pointStatus)">
                {{ getEnumLabel(pointStatusLabelMap, row.pointStatus) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="220" fixed="right" header-cell-class-name="cell-center" class-name="cell-center">
            <template #default="{ row }">
              <div class="table-action-row">
                <el-button link type="primary" @click="openEditDialog(row)">编辑资料</el-button>
                <el-button
                  link
                  :type="row.pointStatus === enabledPointStatus ? 'warning' : 'success'"
                  :loading="statusUpdatingId === row.id"
                  @click="togglePointStatus(row)"
                >
                  {{ row.pointStatus === enabledPointStatus ? '禁用' : '启用' }}
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
      v-model="dialogVisible"
      :title="dialogTitle"
      width="760px"
      class="monitoring-point-dialog"
      align-center
      @closed="resetForm"
    >
      <el-form :model="form" label-width="100px">
        <div class="form-grid">
          <el-form-item label="点位名称" required>
            <el-input v-model="form.pointName" placeholder="请输入点位名称" />
          </el-form-item>
          <el-form-item label="所属机构" required>
            <el-select v-model="form.orgId" filterable style="width: 100%" placeholder="请选择所属机构">
              <el-option
                v-for="option in orgOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="点位坐标" required>
            <div class="location-picker">
              <el-input :model-value="formatCoordinateText(form)" readonly placeholder="请从地图选择点位坐标" />
              <el-button @click="openMapSelector">
                {{ form.longitude && form.latitude ? '重新选点' : '地图选点' }}
              </el-button>
            </div>
          </el-form-item>
          <el-form-item label="地图位置">
            <el-input v-model="form.address" placeholder="地图选点后自动回填，可再次编辑" />
          </el-form-item>
          <el-form-item label="点位类型" required>
            <el-select v-model="form.pointType" style="width: 100%">
              <el-option
                v-for="option in pointTypeOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="点位状态">
            <el-select v-model="form.pointStatus" style="width: 100%">
              <el-option
                v-for="option in pointStatusOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="mapSelectorVisible"
      title="选择点位位置"
      width="880px"
      append-to-body
      destroy-on-close
    >
      <TiandituPointSelector v-model="mapSelectorValue" />
      <template #footer>
        <el-button @click="mapSelectorVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmMapSelection">确认位置</el-button>
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
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs'
import { ElOption, ElSelect } from 'element-plus/es/components/select/index.mjs'
import { ElTable, ElTableColumn } from 'element-plus/es/components/table/index.mjs'
import { createMonitoringPointApi, exportMonitoringPointsApi, fetchMonitoringPointOrgOptionsApi, fetchMonitoringPointsApi, updateMonitoringPointApi } from '../api/lab'
import TablePagination from '../components/common/TablePagination.vue'
import TiandituPointSelector from '../components/TiandituPointSelector.vue'
import {
  DEFAULT_PAGE_SIZE,
  disabledPointStatus,
  enabledPointStatus,
  factoryPointType,
  getEnumLabel,
  getStatusClass,
  pointStatusOptions,
  pointStatusLabelMap,
  sampleTypeOptions,
  sampleTypeLabelMap
} from '../utils/labEnums'

const pointTypeOptions = sampleTypeOptions
const pointTypeLabelMap = sampleTypeLabelMap
const SUMMARY_PAGE_SIZE = 10000

const query = reactive({ pageNum: 1, pageSize: DEFAULT_PAGE_SIZE, keyword: '', pointType: '', orgId: '', pointStatus: '' })
const records = ref([])
const total = ref(0)
const summaryRecords = ref([])
const summaryTotal = ref(0)
const dialogVisible = ref(false)
const activeStatKey = ref('all')
const editingId = ref(null)
const statusUpdatingId = ref(null)
const submitting = ref(false)
const mapSelectorVisible = ref(false)
const mapSelectorValue = reactive({ pointName: '', address: '', latitude: '', longitude: '' })
const orgOptions = ref([])

const defaultForm = () => ({
  pointName: '',
  address: '',
  longitude: '',
  latitude: '',
  orgId: '',
  pointType: factoryPointType,
  pointStatus: enabledPointStatus,
})

const form = reactive(defaultForm())

const dialogTitle = computed(() => (editingId.value ? '编辑监测点位' : '新增监测点位'))

const stats = computed(() => [
  { key: 'all', label: '点位总数', value: summaryTotal.value, desc: '当前监测点位总量' },
  {
    key: 'enabled',
    label: '启用点位',
    value: summaryRecords.value.filter((item) => item.pointStatus === enabledPointStatus).length,
    desc: '状态为启用的点位'
  },
  {
    key: 'disabled',
    label: '停用点位',
    value: summaryRecords.value.filter((item) => item.pointStatus === disabledPointStatus).length,
    desc: '状态为停用的点位'
  },
  {
    key: 'factory',
    label: '出厂水点位',
    value: summaryRecords.value.filter((item) => item.pointType === factoryPointType).length,
    desc: '出厂水监测点位数量'
  }
])

const visibleRecords = computed(() => {
  if (activeStatKey.value === 'enabled') {
    return records.value.filter((item) => item.pointStatus === enabledPointStatus)
  }
  if (activeStatKey.value === 'disabled') {
    return records.value.filter((item) => item.pointStatus === disabledPointStatus)
  }
  if (activeStatKey.value === 'factory') {
    return records.value.filter((item) => item.pointType === factoryPointType)
  }
  return records.value
})

function handleStatClick(key) {
  const nextKey = key === activeStatKey.value ? 'all' : key
  activeStatKey.value = nextKey
  query.pointStatus = nextKey === 'enabled' ? enabledPointStatus : nextKey === 'disabled' ? disabledPointStatus : ''
  query.pointType = nextKey === 'factory' ? factoryPointType : ''
  query.pageNum = 1
  loadData()
}

function openCreateDialog() {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row) {
  editingId.value = row.id
  Object.assign(form, {
    pointName: row.pointName || '',
    address: row.address || row.pointName || '',
    longitude: row.longitude || '',
    latitude: row.latitude || '',
    orgId: row.orgId ? String(row.orgId) : '',  // 使用字符串避免精度丢失
    pointType: row.pointType || factoryPointType,
    pointStatus: row.pointStatus || enabledPointStatus
  })
  dialogVisible.value = true
}

function handleSearch() {
  query.pageNum = 1
  syncActiveStatByQuery()
  loadData()
}

function resetQuery() {
  query.pageNum = 1
  query.pageSize = DEFAULT_PAGE_SIZE
  query.keyword = ''
  query.pointType = ''
  query.orgId = ''
  query.pointStatus = ''
  activeStatKey.value = 'all'
  loadData()
}

function syncActiveStatByQuery() {
  if (query.pointStatus === enabledPointStatus && !query.pointType) {
    activeStatKey.value = 'enabled'
  } else if (query.pointStatus === disabledPointStatus && !query.pointType) {
    activeStatKey.value = 'disabled'
  } else if (query.pointType === factoryPointType && !query.pointStatus) {
    activeStatKey.value = 'factory'
  } else {
    activeStatKey.value = 'all'
  }
}

function resetForm() {
  editingId.value = null
  Object.assign(form, defaultForm())
}

function buildPayload(source) {
  return {
    pointName: source.pointName?.trim() || '',
    address: source.address?.trim() || '',
    longitude: source.longitude?.trim() || '',
    latitude: source.latitude?.trim() || '',
    orgId: source.orgId ? String(source.orgId).trim() : null,  // 使用字符串避免精度丢失
    pointType: source.pointType || factoryPointType,
    pointStatus: source.pointStatus || enabledPointStatus
  }
}

function formatCoordinateText(source) {
  if (!source?.longitude || !source?.latitude) {
    return ''
  }
  return `${source.longitude}, ${source.latitude}`
}

function syncMapSelectorValue(source) {
  mapSelectorValue.pointName = source?.pointName || ''
  mapSelectorValue.address = source?.address || source?.pointName || ''
  mapSelectorValue.latitude = source?.latitude || ''
  mapSelectorValue.longitude = source?.longitude || ''
}

function openMapSelector() {
  syncMapSelectorValue(form)
  mapSelectorVisible.value = true
}

function confirmMapSelection() {
  if (!mapSelectorValue.latitude || !mapSelectorValue.longitude) {
    ElMessage.warning('请先在地图上选择点位')
    return
  }
  form.address = mapSelectorValue.address || mapSelectorValue.pointName || form.address
  form.latitude = mapSelectorValue.latitude
  form.longitude = mapSelectorValue.longitude
  if (!form.pointName) {
    form.pointName = mapSelectorValue.pointName || mapSelectorValue.address || ''
  }
  mapSelectorVisible.value = false
}

async function loadData() {
  const result = await fetchMonitoringPointsApi(query)
  records.value = result.records || []
  total.value = result.total || 0
}

async function loadSummary() {
  const result = await fetchMonitoringPointsApi({
    pageNum: 1,
    pageSize: SUMMARY_PAGE_SIZE
  })
  summaryRecords.value = result.records || []
  summaryTotal.value = Number(result.total || 0)
}

async function loadOrgOptions() {
  const result = await fetchMonitoringPointOrgOptionsApi()
  orgOptions.value = Array.isArray(result)
    ? result.map((item) => ({
        label: item.orgName,
        value: String(item.id)  // 使用字符串避免精度丢失
      }))
    : []
}

async function handleExport() {
  try {
    await exportMonitoringPointsApi(query)
    ElMessage.success('监测点位导出成功')
  } catch (error) {
    ElMessage.error(error.message || '监测点位导出失败')
  }
}

async function submit() {
  const payload = buildPayload(form)
  if (!payload.pointName) {
    ElMessage.warning('请先填写点位名称')
    return
  }
  if (!payload.orgId) {
    ElMessage.warning('请选择所属机构')
    return
  }
  if (!payload.pointType) {
    ElMessage.warning('请选择点位类型')
    return
  }
  if (payload.pointStatus === enabledPointStatus && (!payload.longitude || !payload.latitude)) {
    ElMessage.warning('启用的监测点位请先选择地图坐标')
    return
  }

  submitting.value = true
  try {
    if (editingId.value) {
      await updateMonitoringPointApi(editingId.value, payload)
      ElMessage.success('点位资料已更新')
    } else {
      await createMonitoringPointApi(payload)
      ElMessage.success('监测点位新增成功')
      query.pageNum = 1
    }
    dialogVisible.value = false
    resetForm()
    await Promise.all([loadData(), loadSummary()])
  } finally {
    submitting.value = false
  }
}

async function togglePointStatus(row) {
  const nextStatus = row.pointStatus === enabledPointStatus ? disabledPointStatus : enabledPointStatus
  const actionText = nextStatus === enabledPointStatus ? '启用' : '禁用'

  await ElMessageBox.confirm(`确认${actionText}点位“${row.pointName || '-'}”吗？`, `${actionText}确认`, {
    type: 'warning',
    confirmButtonText: '确认',
    cancelButtonText: '取消'
  })

  statusUpdatingId.value = row.id
  try {
    await updateMonitoringPointApi(row.id, buildPayload({ ...row, pointStatus: nextStatus }))
    ElMessage.success(`点位已${actionText}`)
    await Promise.all([loadData(), loadSummary()])
  } finally {
    statusUpdatingId.value = null
  }
}

onMounted(() => {
  loadData()
  loadSummary()
  loadOrgOptions()
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

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 14px;
}

:deep(.monitoring-point-dialog .el-dialog__body) {
  padding-bottom: 4px;
}

:deep(.el-dialog.monitoring-point-dialog) {
  min-height: auto !important;
  height: auto !important;
}

:deep(.monitoring-point-dialog .el-dialog__footer) {
  padding-top: 8px;
}

:deep(.monitoring-point-dialog .el-form-item) {
  margin-bottom: 10px;
}

.location-picker {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  width: 100%;
}

.table-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

@media (max-width: 860px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>

