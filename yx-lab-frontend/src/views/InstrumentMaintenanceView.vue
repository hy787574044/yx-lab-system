<template>
  <div class="content-grid maintenance-page">
    <section class="glass-panel section-block page-hero">
      <div>
        <h2 class="page-title">设备维修</h2>
        <p class="page-subtitle">
          统一登记仪器设备维修时间、维修原因、维修结果、维修费用与留痕说明，形成正式维修台账。
        </p>
      </div>
      <div class="hero-tags">
        <span class="status-chip info">当前设备 {{ instrumentOptions.length }}</span>
        <span :class="['status-chip', recentCount ? 'warning' : 'success']">近 7 天 {{ recentCount }}</span>
        <span :class="['status-chip', highCostCount ? 'danger' : 'success']">高成本 {{ highCostCount }}</span>
      </div>
    </section>

    <section class="stats-grid">
      <button
        v-for="item in stats"
        :key="item.label"
        type="button"
        :class="['metric-card', 'metric-card--action', { 'is-active': activeStatKey === item.label }]"
        @click="handleStatClick(item.label)"
      >
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <p>{{ item.desc }}</p>
      </button>
    </section>

    <section class="glass-panel section-block">
      <div class="section-head">
        <div>
          <h3 class="section-title">维修台账</h3>
          <p class="page-subtitle">支持按设备查看维修记录，并对当前分页数据按时间、费用和维修类型做快速筛选。</p>
        </div>
      </div>

      <div class="toolbar-panel">
        <div class="toolbar-row">
          <div class="toolbar-fields">
            <label class="toolbar-field">
              <span>设备名称</span>
              <el-select
                v-model="query.instrumentId"
                clearable
                filterable
                placeholder="请选择设备"
              >
                <el-option
                  v-for="item in instrumentOptions"
                  :key="item.id"
                  :label="item.instrumentName"
                  :value="item.id"
                />
              </el-select>
            </label>
            <label class="toolbar-field toolbar-field--wide">
              <span>关键词</span>
              <el-input
                v-model="query.keyword"
                clearable
                placeholder="可按维修原因、维修人、维修公司、维修结果或备注筛选当前页"
              />
            </label>
          </div>

          <div class="toolbar-actions">
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="resetQuery">重置</el-button>
            <el-button @click="goRoute('/instrument-ledger')">查看设备台账</el-button>
            <el-button type="primary" plain @click="openDialog()">新增维修</el-button>
          </div>
        </div>
      </div>

      <div class="table-card">
        <el-table
          class="list-table"
          :data="visibleRecords"
          stripe
          max-height="460"
          v-loading="loading"
          empty-text="暂无设备维修记录数据"
        >
          <el-table-column prop="instrumentName" label="设备名称" min-width="180" />
          <el-table-column prop="maintenanceTime" label="维修时间" width="180" />
          <el-table-column prop="maintenanceReason" label="维修原因" min-width="180" show-overflow-tooltip />
          <el-table-column prop="maintainerName" label="维修人" width="120" />
          <el-table-column prop="maintenanceCompany" label="维修公司" min-width="160" show-overflow-tooltip />
          <el-table-column prop="maintenanceResult" label="维修结果" min-width="180" show-overflow-tooltip />
          <el-table-column label="维修费用" width="120" header-cell-class-name="cell-center" class-name="cell-center">
            <template #default="{ row }">
              {{ formatMoney(row.maintenanceCost) }}
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
          <el-table-column label="操作" width="170" fixed="right" class-name="cell-center">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
              <el-button link type="danger" @click="removeRow(row)">删除</el-button>
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

    <section class="scene-grid">
      <div class="glass-panel section-block">
        <div class="section-head">
          <h3 class="section-title">页面说明</h3>
        </div>
        <div class="scene-copy">
          <p>设备维修页已从占位页升级为正式业务页，直接接入后端维修接口。</p>
          <p>当前支持维修记录分页、设备筛选、新增、编辑、删除，能够与设备台账形成闭环。</p>
        </div>
      </div>

      <div class="glass-panel section-block">
        <div class="section-head">
          <h3 class="section-title">关联入口</h3>
        </div>
        <div class="quick-links">
          <button type="button" class="quick-link" @click="goRoute('/instrument-ledger')">
            <strong>设备台账</strong>
            <span>查看设备基础档案、设备状态与校准周期</span>
          </button>
          <button type="button" class="quick-link" @click="goRoute('/document-ledger')">
            <strong>文档台账</strong>
            <span>管理维修报告、说明附件与制度文档</span>
          </button>
          <button type="button" class="quick-link" @click="goRoute('/dashboard')">
            <strong>运行总览</strong>
            <span>回到主看板继续查看整体实验室运行情况</span>
          </button>
        </div>
      </div>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑维修记录' : '新增维修记录'"
      width="760px"
      destroy-on-close
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <div class="form-grid">
          <el-form-item label="设备名称" prop="instrumentId">
            <el-select
              v-model="form.instrumentId"
              filterable
              placeholder="请选择设备"
              style="width: 100%"
              @change="syncInstrumentName"
            >
              <el-option
                v-for="item in instrumentOptions"
                :key="item.id"
                :label="item.instrumentName"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="维修时间" prop="maintenanceTime">
            <el-date-picker
              v-model="form.maintenanceTime"
              type="datetime"
              format="YYYY-MM-DD HH:mm:ss"
              value-format="YYYY-MM-DD HH:mm:ss"
              placeholder="请选择维修时间"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item class="form-span-2" label="维修原因" prop="maintenanceReason">
            <el-input
              v-model="form.maintenanceReason"
              type="textarea"
              :rows="2"
              placeholder="请输入维修原因"
            />
          </el-form-item>
          <el-form-item label="维修人" prop="maintainerName">
            <el-input v-model="form.maintainerName" placeholder="请输入维修人" />
          </el-form-item>
          <el-form-item label="维修公司" prop="maintenanceCompany">
            <el-input v-model="form.maintenanceCompany" placeholder="请输入维修公司" />
          </el-form-item>
          <el-form-item class="form-span-2" label="维修结果" prop="maintenanceResult">
            <el-input
              v-model="form.maintenanceResult"
              type="textarea"
              :rows="2"
              placeholder="请输入维修结果"
            />
          </el-form-item>
          <el-form-item label="维修费用" prop="maintenanceCost">
            <el-input-number
              v-model="form.maintenanceCost"
              :min="0"
              :precision="2"
              :step="100"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="设备名称回填" prop="instrumentName">
            <el-input v-model="form.instrumentName" disabled placeholder="选择设备后自动回填" />
          </el-form-item>
          <el-form-item class="form-span-2" label="备注" prop="remark">
            <el-input
              v-model="form.remark"
              type="textarea"
              :rows="3"
              placeholder="可填写维修留痕、附件说明或后续处理建议"
            />
          </el-form-item>
        </div>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import dayjs from 'dayjs'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import TablePagination from '../components/common/TablePagination.vue'
import {
  createInstrumentMaintenanceApi,
  deleteInstrumentMaintenanceApi,
  fetchInstrumentMaintenancesApi,
  fetchInstrumentsApi,
  updateInstrumentMaintenanceApi
} from '../api/lab'
import { DEFAULT_PAGE_SIZE } from '../utils/labEnums'

const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const formRef = ref()

const query = reactive({
  pageNum: 1,
  pageSize: DEFAULT_PAGE_SIZE,
  instrumentId: '',
  keyword: ''
})

const records = ref([])
const total = ref(0)
const activeStatKey = ref('全部记录')
const instrumentOptions = ref([])

function emptyForm() {
  return {
    id: null,
    instrumentId: '',
    instrumentName: '',
    maintenanceTime: dayjs().format('YYYY-MM-DD HH:mm:ss'),
    maintenanceReason: '',
    maintainerName: '',
    maintenanceCompany: '',
    maintenanceResult: '',
    maintenanceCost: null,
    remark: ''
  }
}

const form = reactive(emptyForm())

const rules = {
  instrumentId: [{ required: true, message: '请选择设备', trigger: 'change' }],
  maintenanceTime: [{ required: true, message: '请选择维修时间', trigger: 'change' }],
  maintenanceReason: [{ required: true, message: '请输入维修原因', trigger: 'blur' }]
}

function toSafeNumber(value) {
  const num = typeof value === 'number' ? value : Number.parseFloat(String(value ?? '').replace(/,/g, '').trim())
  return Number.isFinite(num) ? num : 0
}

function formatMoney(value) {
  const amount = toSafeNumber(value)
  return amount ? `¥${amount.toFixed(2)}` : '¥0.00'
}

function goRoute(path) {
  if (!path) {
    return
  }
  router.push(path)
}

const keywordFilteredRecords = computed(() => {
  const keyword = String(query.keyword || '').trim().toLowerCase()
  if (!keyword) {
    return records.value
  }

  return records.value.filter((item) => (
    `${item.instrumentName || ''} ${item.maintenanceReason || ''} ${item.maintainerName || ''} ${item.maintenanceCompany || ''} ${item.maintenanceResult || ''} ${item.remark || ''}`
      .toLowerCase()
      .includes(keyword)
  ))
})

const recentCount = computed(() =>
  keywordFilteredRecords.value.filter((item) => dayjs(item.maintenanceTime).isAfter(dayjs().subtract(7, 'day'))).length
)

const currentMonthCount = computed(() =>
  keywordFilteredRecords.value.filter((item) => dayjs(item.maintenanceTime).isSame(dayjs(), 'month')).length
)

const externalCount = computed(() =>
  keywordFilteredRecords.value.filter((item) => String(item.maintenanceCompany || '').trim()).length
)

const highCostCount = computed(() =>
  keywordFilteredRecords.value.filter((item) => toSafeNumber(item.maintenanceCost) >= 1000).length
)

const stats = computed(() => [
  {
    label: '全部记录',
    value: toSafeNumber(total.value),
    desc: '设备维修分页总量'
  },
  {
    label: '本页记录',
    value: keywordFilteredRecords.value.length,
    desc: '当前分页已加载并筛选后的记录数'
  },
  {
    label: '最近7天',
    value: recentCount.value,
    desc: '当前页近 7 天内的维修记录'
  },
  {
    label: '本月维修',
    value: currentMonthCount.value,
    desc: '当前页本月发生的维修记录'
  },
  {
    label: '外部维修',
    value: externalCount.value,
    desc: '当前页由外部维修公司处理的记录'
  },
  {
    label: '高成本维修',
    value: highCostCount.value,
    desc: '当前页维修费用不低于 1000 元的记录'
  }
])

const visibleRecords = computed(() => {
  if (activeStatKey.value === '最近7天') {
    return keywordFilteredRecords.value.filter((item) => dayjs(item.maintenanceTime).isAfter(dayjs().subtract(7, 'day')))
  }
  if (activeStatKey.value === '本月维修') {
    return keywordFilteredRecords.value.filter((item) => dayjs(item.maintenanceTime).isSame(dayjs(), 'month'))
  }
  if (activeStatKey.value === '外部维修') {
    return keywordFilteredRecords.value.filter((item) => String(item.maintenanceCompany || '').trim())
  }
  if (activeStatKey.value === '高成本维修') {
    return keywordFilteredRecords.value.filter((item) => toSafeNumber(item.maintenanceCost) >= 1000)
  }
  return keywordFilteredRecords.value
})

function handleStatClick(label) {
  activeStatKey.value = activeStatKey.value === label ? '全部记录' : label
}

function resetForm() {
  Object.assign(form, emptyForm())
}

function syncInstrumentName() {
  const target = instrumentOptions.value.find((item) => item.id === form.instrumentId)
  form.instrumentName = target?.instrumentName || ''
}

function handleSearch() {
  query.pageNum = 1
  activeStatKey.value = '全部记录'
  loadData()
}

function resetQuery() {
  query.pageNum = 1
  query.pageSize = DEFAULT_PAGE_SIZE
  query.instrumentId = ''
  query.keyword = ''
  activeStatKey.value = '全部记录'
  loadData()
}

async function loadInstrumentOptions() {
  const result = await fetchInstrumentsApi({
    pageNum: 1,
    pageSize: 500
  })
  instrumentOptions.value = result.records || []
}

async function loadData() {
  loading.value = true
  try {
    const result = await fetchInstrumentMaintenancesApi({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      instrumentId: query.instrumentId || undefined
    })
    records.value = result.records || []
    total.value = toSafeNumber(result.total)
  } finally {
    loading.value = false
  }
}

function openDialog(row) {
  resetForm()
  if (row) {
    Object.assign(form, {
      id: row.id,
      instrumentId: row.instrumentId,
      instrumentName: row.instrumentName || '',
      maintenanceTime: row.maintenanceTime || '',
      maintenanceReason: row.maintenanceReason || '',
      maintainerName: row.maintainerName || '',
      maintenanceCompany: row.maintenanceCompany || '',
      maintenanceResult: row.maintenanceResult || '',
      maintenanceCost: row.maintenanceCost == null ? null : Number(row.maintenanceCost),
      remark: row.remark || ''
    })
  }
  dialogVisible.value = true
}

async function submitForm() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = {
      instrumentId: Number(form.instrumentId),
      instrumentName: form.instrumentName,
      maintenanceTime: form.maintenanceTime,
      maintenanceReason: form.maintenanceReason,
      maintainerName: form.maintainerName || '',
      maintenanceCompany: form.maintenanceCompany || '',
      maintenanceResult: form.maintenanceResult || '',
      maintenanceCost: form.maintenanceCost == null ? null : Number(form.maintenanceCost),
      remark: form.remark || ''
    }

    if (form.id) {
      await updateInstrumentMaintenanceApi(form.id, payload)
      ElMessage.success('维修记录已更新。')
    } else {
      await createInstrumentMaintenanceApi(payload)
      ElMessage.success('维修记录已新增。')
    }

    dialogVisible.value = false
    await loadData()
  } finally {
    saving.value = false
  }
}

async function removeRow(row) {
  try {
    await ElMessageBox.confirm(`确认删除设备“${row.instrumentName}”的这条维修记录吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await deleteInstrumentMaintenanceApi(row.id)
    ElMessage.success('维修记录已删除。')
    if (records.value.length === 1 && query.pageNum > 1) {
      query.pageNum -= 1
    }
    await loadData()
  } catch {
    // 用户取消删除时不处理
  }
}

onMounted(async () => {
  await Promise.all([loadInstrumentOptions(), loadData()])
})
</script>

<style scoped>
.maintenance-page {
  gap: 16px;
}

.page-hero,
.scene-grid {
  display: grid;
  gap: 16px;
}

.page-hero {
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
}

.hero-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: flex-end;
}

.metric-card--action,
.quick-link {
  width: 100%;
  text-align: left;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.metric-card--action:hover,
.metric-card--action:focus-visible,
.metric-card--action.is-active,
.quick-link:hover,
.quick-link:focus-visible {
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
  gap: 4px 16px;
}

.form-span-2 {
  grid-column: 1 / -1;
}

.scene-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.scene-copy {
  display: grid;
  gap: 10px;
  color: var(--text-sub);
  font-size: 14px;
  line-height: 1.7;
}

.scene-copy p {
  margin: 0;
}

.quick-links {
  display: grid;
  gap: 12px;
}

.quick-link {
  display: grid;
  gap: 4px;
  padding: 14px;
  border: 1px solid var(--line-soft);
  border-radius: 12px;
  background: var(--bg-panel-soft);
}

.quick-link strong {
  color: var(--text-main);
  font-size: 14px;
  line-height: 1.5;
}

.quick-link span {
  color: var(--text-sub);
  font-size: 13px;
  line-height: 1.6;
}

@media (max-width: 900px) {
  .page-hero,
  .scene-grid,
  .form-grid {
    grid-template-columns: 1fr;
  }

  .form-span-2 {
    grid-column: auto;
  }

  .hero-tags {
    justify-content: flex-start;
  }
}
</style>
