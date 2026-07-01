<template>
  <div class="content-grid detection-method-page fixed-table-page">
    <section class="glass-panel section-block fixed-table-section">
      <div class="section-head">
        <div>
          <h3 class="section-title">{{ currentScene.tableTitle }}</h3>
        </div>
      </div>

      <section class="stats-grid section-stats">
        <button
          v-for="item in currentStats"
          :key="item.key"
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
            <el-button
              type="primary"
              class="toolbar-primary-button"
              @click="openDialog()"
            >
              新增检测方法
            </el-button>
            <div class="toolbar-fields">
              <label class="toolbar-field">
                <span>绑定参数</span>
                <el-select
                  v-model="query.parameterId"
                  clearable
                  filterable
                  placeholder="请选择检测参数"
                >
                  <el-option
                    v-for="item in parameterOptions"
                    :key="item.id"
                    :label="item.parameterName"
                    :value="item.id"
                  />
                </el-select>
              </label>
              <label class="toolbar-field">
                <span>状态</span>
                <el-select
                  v-model="query.enabled"
                  clearable
                  placeholder="请选择状态"
                >
                  <el-option label="启用" :value="1" />
                  <el-option label="停用" :value="0" />
                </el-select>
              </label>
              <label class="toolbar-field">
                <span>绑定状态</span>
                <el-select
                  v-model="query.bindingStatus"
                  clearable
                  placeholder="请选择绑定状态"
                >
                  <el-option label="已绑定型号" value="BOUND" />
                  <el-option label="未绑定型号" value="UNBOUND" />
                </el-select>
              </label>
              <label class="toolbar-field toolbar-field--medium">
                <span>关键字</span>
                <el-input
                  v-model="query.keyword"
                  clearable
                  placeholder="请输入检测方法名称、编码、标准编号、取样体积、检测步骤或备注"
                  @keyup.enter="handleSearch"
                />
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
        <el-table
          class="list-table"
          :data="visibleRows"
          stripe
          height="100%"
          empty-text="暂无检测方法数据"
        >
          <el-table-column label="方法设备关系" min-width="360">
            <template #default="{ row }">
              <div class="binding-tree">
                <div class="binding-tree__parameter">
                  <span class="binding-tree__dash">-</span>
                  <span class="binding-tree__parameter-name">{{ row.methodName || '-' }}</span>
                </div>
                <div v-if="getMethodBoundModels(row).length" class="binding-tree__children">
                  <div
                    v-for="model in getMethodBoundModels(row)"
                    :key="`${row.id}-${buildInstrumentModelKey(model)}`"
                    class="binding-tree__method"
                  >
                    <span class="binding-tree__dash binding-tree__dash--child">-</span>
                    <span class="binding-tree__method-name">{{ formatInstrumentModelLabel(model) }}</span>
                    <span class="binding-tree__count">在库 {{ model.instrumentCount || 0 }} 台</span>
                    <el-button
                      v-permission="'detectionConfig:write'"
                      link
                      type="danger"
                      class="binding-tree__action"
                      @click="removeSingleMethodModelBinding(row, model)"
                    >
                      解除绑定
                    </el-button>
                  </div>
                </div>
                <div v-else class="binding-tree__method binding-tree__method--empty">
                  <span class="binding-tree__dash">-</span>
                  <span>当前未绑定设备型号</span>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="methodCode" label="方法编码" min-width="140">
            <template #default="{ row }">{{ row.methodCode || '-' }}</template>
          </el-table-column>
          <el-table-column prop="standardCode" label="标准编号" min-width="160">
            <template #default="{ row }">{{ row.standardCode || '-' }}</template>
          </el-table-column>
          <el-table-column prop="sampleVolume" label="取样体积" min-width="90">
            <template #default="{ row }">{{ row.sampleVolume || '-' }}</template>
          </el-table-column>
          <el-table-column prop="parameterName" label="已绑定参数" min-width="150">
            <template #default="{ row }">{{ row.parameterName || '未绑定' }}</template>
          </el-table-column>
          <el-table-column prop="methodBasis" label="检测步骤" min-width="220">
            <template #default="{ row }">
              <el-tooltip
                effect="dark"
                placement="top"
                popper-class="method-basis-tooltip"
                :disabled="!row.methodBasis"
              >
                <template #content>
                  <span class="preserve-line-breaks">{{ row.methodBasis || '-' }}</span>
                </template>
                <span class="method-basis-ellipsis">{{ row.methodBasis || '-' }}</span>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column prop="applyScope" label="适用范围" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">{{ row.applyScope || '-' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="110" class-name="cell-center" header-cell-class-name="cell-center">
            <template #default="{ row }">
              <span :class="['status-chip', row.enabled === 1 ? 'success' : 'warning']">
                {{ row.enabled === 1 ? '启用' : '停用' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="220" show-overflow-tooltip>
            <template #default="{ row }">{{ row.remark || '-' }}</template>
          </el-table-column>
          <el-table-column prop="updatedTime" label="更新时间" min-width="170">
            <template #default="{ row }">{{ row.updatedTime || '-' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="380" fixed="right" header-cell-class-name="cell-center" class-name="cell-center">
            <template #default="{ row }">
              <div class="table-action-row detection-method-action-row">
                <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
                <el-button link type="primary" @click="openModelBindingDialog(row)">配置绑定</el-button>
                <el-button
                  link
                  :disabled="!getMethodBoundModels(row).length"
                  @click="clearMethodModelBindings(row)"
                >
                  清空绑定
                </el-button>
                <el-button link type="danger" @click="removeRow(row)">删除</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
        </div>

        <TablePagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          @change="loadRows"
        />
      </div>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑检测方法' : '新增检测方法'"
      width="840px"
      destroy-on-close
      @closed="resetForm"
    >
      <el-form label-width="100px">
        <div class="form-grid">
          <el-form-item label="检测方法" required>
            <el-input v-model="form.methodName" placeholder="请输入检测方法名称" />
          </el-form-item>
          <el-form-item label="方法编码">
            <el-input v-model="form.methodCode" placeholder="请输入方法编码，可不填" />
          </el-form-item>
          <el-form-item label="标准编号">
            <el-input v-model="form.standardCode" placeholder="请输入标准编号，如 GB/T 5750.4" />
          </el-form-item>
          <el-form-item label="取样体积">
            <el-input
              v-model="form.sampleVolume"
              inputmode="decimal"
              placeholder="请输入数字，如 100"
              @input="handleSampleVolumeInput"
            >
              <template #append>mL</template>
            </el-input>
          </el-form-item>
          <el-form-item label="状态">
            <el-radio-group v-model="form.enabled">
              <el-radio-button :label="1">启用</el-radio-button>
              <el-radio-button :label="0">停用</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item class="form-span-2" label="检测步骤">
            <el-input
              v-model="form.methodBasis"
              type="textarea"
              :autosize="{ minRows: 4, maxRows: 10 }"
              placeholder="请输入检测步骤、方法说明或执行标准，按 Enter 可手动换行"
            />
          </el-form-item>
          <el-form-item class="form-span-2" label="适用范围">
            <el-input
              v-model="form.applyScope"
              type="textarea"
              :rows="3"
              placeholder="请输入适用品类、样品类型或适用条件"
            />
          </el-form-item>
          <el-form-item class="form-span-2" label="备注">
            <el-input
              v-model="form.remark"
              type="textarea"
              :rows="3"
              placeholder="请输入补充说明"
            />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="modelBindingDialogVisible"
      :title="modelBindingDialogTitle"
      width="960px"
      destroy-on-close
      @closed="resetModelBindingForm"
    >
      <div class="binding-editor">
        <div class="binding-editor__summary">
          <span class="binding-editor__chip">
            检测方法 <strong>{{ modelBindingForm.methodName || '-' }}</strong>
          </span>
          <button
            type="button"
            :class="['binding-editor__chip', 'binding-editor__chip--action', { 'is-active': modelBindingDialogFilter === 'selected' }]"
            @click="switchModelBindingFilter('selected')"
          >
            已选型号 <strong>{{ selectedInstrumentModelCount }}</strong>
          </button>
          <button
            type="button"
            :class="['binding-editor__chip', 'binding-editor__chip--action', { 'is-active': modelBindingDialogFilter === 'pending' }]"
            @click="switchModelBindingFilter('pending')"
          >
            待绑定型号 <strong>{{ pendingInstrumentModelCount }}</strong>
          </button>
        </div>
        <div class="toolbar-panel binding-editor__toolbar">
          <div class="toolbar-row">
            <div class="toolbar-main">
              <div class="toolbar-fields">
                <label class="toolbar-field toolbar-field--medium">
                  <span>型号筛选</span>
                  <el-input
                    v-model="modelBindingForm.keyword"
                    clearable
                    placeholder="请输入设备名称、设备型号或生产厂家"
                  />
                </label>
              </div>
            </div>
            <div class="toolbar-actions">
              <el-button
                :type="modelBindingDialogFilter === 'all' ? 'primary' : 'default'"
                @click="switchModelBindingFilter('all')"
              >
                全部型号
              </el-button>
            </div>
          </div>
        </div>
        <div v-if="filteredInstrumentModelOptions.length" class="instrument-model-grid">
          <div
            v-for="item in filteredInstrumentModelOptions"
            :key="buildInstrumentModelKey(item)"
            :class="['instrument-model-card', { 'is-selected': isInstrumentModelSelected(item) }]"
            @click="toggleInstrumentModel(item)"
          >
            <div class="instrument-model-card__head">
              <el-checkbox
                :model-value="isInstrumentModelSelected(item)"
                @change="(checked) => handleInstrumentModelToggle(item, checked)"
                @click.stop
              >
                {{ item.instrumentModel || '-' }}
              </el-checkbox>
              <span class="status-chip info">在库 {{ item.instrumentCount || 0 }} 台</span>
            </div>
            <div class="instrument-model-card__meta">
              设备名称：{{ formatInstrumentNameText(item) }}
            </div>
            <div class="instrument-model-card__meta">
              生产厂家：{{ item.manufacturer || '-' }}
            </div>
          </div>
        </div>
        <div v-else class="empty-box">暂无可绑定的设备型号，请先在仪器台账维护设备型号。</div>
      </div>
      <template #footer>
        <el-button @click="modelBindingDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingModelBinding" @click="submitModelBindings">保存绑定</el-button>
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
import { ElCheckbox } from 'element-plus/es/components/checkbox/index.mjs'
import { ElRadioButton, ElRadioGroup } from 'element-plus/es/components/radio/index.mjs'
import { ElOption, ElSelect } from 'element-plus/es/components/select/index.mjs'
import { ElTable, ElTableColumn } from 'element-plus/es/components/table/index.mjs'
import { ElTooltip } from 'element-plus/es/components/tooltip/index.mjs'
import TablePagination from '../components/common/TablePagination.vue'
import {
  createDetectionMethodApi,
  deleteDetectionMethodApi,
  exportDetectionMethodsApi,
  fetchInstrumentModelOptionsApi,
  fetchDetectionParametersApi,
  fetchDetectionMethodsApi,
  saveDetectionMethodInstrumentModelBindingsApi,
  updateDetectionMethodApi
} from '../api/lab'
import { DEFAULT_PAGE_SIZE } from '../utils/labEnums'

const SUMMARY_PAGE_SIZE = 10000
const currentScene = {
  title: '检测方法',
  subtitle: '维护化验室检测方法基础台账，为后续检测配置和引用提供统一口径。',
  tableTitle: '检测方法列表',
  tableSubtitle: '支持对检测方法进行新增、编辑、删除与状态维护。',
  guide: '检测方法用于沉淀化验室标准方法信息，后续若要把方法绑定到参数或检测流程，可以直接基于本台账扩展。',
  constraint: '检测方法名称建议保持唯一，方法编码和标准编号尽量按正式标准填写，便于后续对接报告、模板和质量追溯。',
  quickLinks: [
    { path: '/detection-projects', label: '检测参数', desc: '查看当前检测参数基础台账' },
    { path: '/detection-project-groups', label: '检测套餐', desc: '查看参数组合后的检测套餐配置' },
    { path: '/detection-analysis', label: '检测分析', desc: '回到正式检测业务页面继续流转' }
  ]
}

const activeStatKey = ref('all')
const rows = ref([])
const total = ref(0)
const summaryRows = ref([])
const summaryTotal = ref(0)
const parameterOptions = ref([])
const instrumentModelOptions = ref([])
const dialogVisible = ref(false)
const modelBindingDialogVisible = ref(false)
const saving = ref(false)
const savingModelBinding = ref(false)
const modelBindingDialogFilter = ref('all')

const query = reactive({
  parameterId: '',
  enabled: '',
  bindingStatus: '',
  keyword: '',
  pageNum: 1,
  pageSize: DEFAULT_PAGE_SIZE
})

const form = reactive({
  id: null,
  methodName: '',
  methodCode: '',
  standardCode: '',
  sampleVolume: '',
  methodBasis: '',
  applyScope: '',
  enabled: 1,
  remark: ''
})

const modelBindingForm = reactive({
  methodId: null,
  methodName: '',
  selectedKeys: [],
  keyword: ''
})

const visibleRows = computed(() => {
  if (activeStatKey.value === 'enabled') {
    return rows.value.filter((item) => item.enabled === 1)
  }
  if (activeStatKey.value === 'disabled') {
    return rows.value.filter((item) => item.enabled === 0)
  }
  if (activeStatKey.value === 'bound') {
    return rows.value.filter((item) => Number(item.instrumentModelCount || 0) > 0)
  }
  if (activeStatKey.value === 'unbound') {
    return rows.value.filter((item) => Number(item.instrumentModelCount || 0) === 0)
  }
  return rows.value
})

const currentStats = computed(() => [
  { key: 'all', label: '全部方法', value: summaryTotal.value, desc: '检测方法全量台账' },
  { key: 'bound', label: '已绑定型号', value: summaryRows.value.filter((item) => Number(item.instrumentModelCount || 0) > 0).length, desc: '已配置设备型号的方法' },
  { key: 'unbound', label: '未绑定型号', value: summaryRows.value.filter((item) => Number(item.instrumentModelCount || 0) === 0).length, desc: '尚未配置设备型号的方法' },
  { key: 'disabled', label: '停用方法', value: summaryRows.value.filter((item) => item.enabled === 0).length, desc: '已停用或暂不使用的方法' }
])

const modelBindingDialogTitle = computed(() => (
  modelBindingForm.methodName ? `配置设备型号 - ${modelBindingForm.methodName}` : '配置设备型号'
))

const selectedInstrumentModelKeySet = computed(() => new Set(modelBindingForm.selectedKeys))
const selectedInstrumentModelCount = computed(() => modelBindingForm.selectedKeys.length)
const pendingInstrumentModelCount = computed(() =>
  instrumentModelOptions.value.filter((item) => !isInstrumentModelSelected(item)).length
)

const filteredInstrumentModelOptions = computed(() => {
  const keyword = modelBindingForm.keyword.trim().toLowerCase()
  let options = instrumentModelOptions.value || []
  if (keyword) {
    options = options.filter((item) =>
      String(item.instrumentName || '').toLowerCase().includes(keyword)
        || String(item.instrumentDisplayNames || '').toLowerCase().includes(keyword)
        || String(item.label || '').toLowerCase().includes(keyword)
        || String(item.instrumentModel || '').toLowerCase().includes(keyword)
        || String(item.manufacturer || '').toLowerCase().includes(keyword)
    )
  }
  if (modelBindingDialogFilter.value === 'selected') {
    options = options.filter((item) => isInstrumentModelSelected(item))
  } else if (modelBindingDialogFilter.value === 'pending') {
    options = options.filter((item) => !isInstrumentModelSelected(item))
  }
  return [...options].sort(compareInstrumentModelOption)
})

function handleStatClick(key) {
  const nextKey = activeStatKey.value === key ? 'all' : key
  activeStatKey.value = nextKey
  query.enabled = nextKey === 'disabled' ? 0 : ''
  query.bindingStatus = nextKey === 'bound' ? 'BOUND' : nextKey === 'unbound' ? 'UNBOUND' : ''
  query.pageNum = 1
  loadRows()
}

function resetForm() {
  form.id = null
  form.methodName = ''
  form.methodCode = ''
  form.standardCode = ''
  form.sampleVolume = ''
  form.methodBasis = ''
  form.applyScope = ''
  form.enabled = 1
  form.remark = ''
}

function resetModelBindingForm() {
  modelBindingForm.methodId = null
  modelBindingForm.methodName = ''
  modelBindingForm.selectedKeys = []
  modelBindingForm.keyword = ''
  modelBindingDialogFilter.value = 'all'
}

function openDialog(row) {
  resetForm()
  if (row) {
    form.id = row.id
    form.methodName = row.methodName || ''
    form.methodCode = row.methodCode || ''
    form.standardCode = row.standardCode || ''
    form.sampleVolume = extractSampleVolumeNumber(row.sampleVolume)
    form.methodBasis = row.methodBasis || ''
    form.applyScope = row.applyScope || ''
    form.enabled = row.enabled ?? 1
    form.remark = row.remark || ''
  }
  dialogVisible.value = true
}

function extractSampleVolumeNumber(value) {
  const text = String(value || '').trim().replace(/\s+/g, '')
  return text.replace(/(?:ml|毫升)$/i, '')
}

function sanitizeSampleVolumeNumber(value) {
  const text = extractSampleVolumeNumber(value).replace(/[^\d.]/g, '')
  const parts = text.split('.')
  if (parts.length <= 1) {
    return parts[0]
  }
  return `${parts[0]}.${parts.slice(1).join('')}`
}

function handleSampleVolumeInput(value) {
  form.sampleVolume = sanitizeSampleVolumeNumber(value)
}

function buildSampleVolumePayload(value) {
  const numberText = sanitizeSampleVolumeNumber(value)
  return numberText ? `${numberText}mL` : ''
}

function isValidSampleVolumeNumber(value) {
  const numberText = sanitizeSampleVolumeNumber(value)
  return !numberText || /^\d+(\.\d+)?$/.test(numberText)
}

function getMethodBoundModels(row) {
  return Array.isArray(row?.instrumentModelBindings) ? row.instrumentModelBindings : []
}

function buildInstrumentModelKey(itemOrModel, manufacturer) {
  if (typeof itemOrModel === 'string') {
    return `${itemOrModel.trim()}||${String(manufacturer || '').trim()}`
  }
  return `${String(itemOrModel?.instrumentModel || '').trim()}||${String(itemOrModel?.manufacturer || '').trim()}`
}

function formatInstrumentModelLabel(item) {
  const displayNames = String(item?.instrumentDisplayNames || item?.instrumentDisplayName || item?.label || '').trim()
  if (displayNames) {
    return displayNames
  }
  const name = String(item?.instrumentName || '').trim()
  const model = item?.instrumentModel || '-'
  return name ? `${name}/${model}` : model
}

function formatInstrumentNameText(item) {
  return String(item?.instrumentName || item?.instrumentDisplayNames || item?.instrumentDisplayName || '').trim() || '-'
}

function isInstrumentModelSelected(item) {
  return selectedInstrumentModelKeySet.value.has(buildInstrumentModelKey(item))
}

function toggleInstrumentModel(item) {
  handleInstrumentModelToggle(item, !isInstrumentModelSelected(item))
}

function handleInstrumentModelToggle(item, checked) {
  const key = buildInstrumentModelKey(item)
  const nextSelected = new Set(modelBindingForm.selectedKeys)
  if (checked) {
    nextSelected.add(key)
  } else {
    nextSelected.delete(key)
  }
  modelBindingForm.selectedKeys = Array.from(nextSelected)
}

function switchModelBindingFilter(key) {
  modelBindingDialogFilter.value = modelBindingDialogFilter.value === key ? 'all' : key
}

function compareInstrumentModelOption(left, right) {
  const leftSelected = isInstrumentModelSelected(left) ? 0 : 1
  const rightSelected = isInstrumentModelSelected(right) ? 0 : 1
  if (leftSelected !== rightSelected) {
    return leftSelected - rightSelected
  }
  return formatInstrumentModelLabel(left).localeCompare(formatInstrumentModelLabel(right), 'zh-CN')
}

async function openModelBindingDialog(row) {
  if (!instrumentModelOptions.value.length) {
    await loadInstrumentModelOptions()
  }
  resetModelBindingForm()
  modelBindingForm.methodId = row.id
  modelBindingForm.methodName = row.methodName || ''
  modelBindingForm.selectedKeys = getMethodBoundModels(row).map((item) => buildInstrumentModelKey(item))
  modelBindingDialogVisible.value = true
}

function buildModelBindingPayload(selectedKeys = modelBindingForm.selectedKeys) {
  const optionMap = new Map(instrumentModelOptions.value.map((item) => [buildInstrumentModelKey(item), item]))
  return {
    items: selectedKeys
      .map((key) => optionMap.get(key))
      .filter(Boolean)
      .map((item) => ({
        instrumentModel: item.instrumentModel,
        manufacturer: item.manufacturer || ''
      }))
  }
}

async function submitModelBindings() {
  if (!modelBindingForm.methodId) {
    ElMessage.warning('当前检测方法不存在，请刷新后重试')
    return
  }
  savingModelBinding.value = true
  try {
    await saveDetectionMethodInstrumentModelBindingsApi(modelBindingForm.methodId, buildModelBindingPayload())
    ElMessage.success('设备型号绑定已保存')
    modelBindingDialogVisible.value = false
    await Promise.all([loadRows(), loadSummaryRows()])
  } finally {
    savingModelBinding.value = false
  }
}

async function clearMethodModelBindings(row) {
  await ElMessageBox.confirm(`确认清空检测方法“${row.methodName}”已绑定的设备型号吗？`, '清空确认', {
    type: 'warning'
  })
  await saveDetectionMethodInstrumentModelBindingsApi(row.id, { items: [] })
  ElMessage.success('已清空当前检测方法的设备型号绑定')
  await Promise.all([loadRows(), loadSummaryRows()])
}

async function removeSingleMethodModelBinding(row, model) {
  const modelLabel = formatInstrumentModelLabel(model)
  await ElMessageBox.confirm(`确认解除检测方法“${row.methodName}”下的设备型号“${modelLabel}”吗？`, '解除确认', {
    type: 'warning'
  })
  const removedKey = buildInstrumentModelKey(model)
  const remainingKeys = getMethodBoundModels(row)
    .map((item) => buildInstrumentModelKey(item))
    .filter((key) => key !== removedKey)
  if (!instrumentModelOptions.value.length) {
    await loadInstrumentModelOptions()
  }
  await saveDetectionMethodInstrumentModelBindingsApi(row.id, buildModelBindingPayload(remainingKeys))
  ElMessage.success('已解除当前设备型号绑定')
  await Promise.all([loadRows(), loadSummaryRows()])
}

async function submitForm() {
  if (!form.methodName.trim()) {
    ElMessage.warning('请填写检测方法名称')
    return
  }
  if (!isValidSampleVolumeNumber(form.sampleVolume)) {
    ElMessage.warning('取样体积只能填写数字')
    return
  }

  const payload = {
    methodName: form.methodName.trim(),
    methodCode: form.methodCode.trim(),
    standardCode: form.standardCode.trim(),
    sampleVolume: buildSampleVolumePayload(form.sampleVolume),
    methodBasis: form.methodBasis.trim(),
    applyScope: form.applyScope.trim(),
    enabled: form.enabled,
    remark: form.remark.trim()
  }

  saving.value = true
  try {
    if (form.id) {
      await updateDetectionMethodApi(form.id, payload)
    } else {
      await createDetectionMethodApi(payload)
    }
    ElMessage.success(form.id ? '检测方法已更新' : '检测方法已新增')
    dialogVisible.value = false
    await Promise.all([loadRows(), loadSummaryRows()])
  } finally {
    saving.value = false
  }
}

async function removeRow(row) {
  await ElMessageBox.confirm(`确定删除检测方法“${row.methodName}”吗？`, '删除确认', {
    type: 'warning'
  })
  await deleteDetectionMethodApi(row.id)
  ElMessage.success('检测方法已删除')
  await Promise.all([loadRows(), loadSummaryRows()])
}

function handleSearch() {
  query.pageNum = 1
  syncActiveStatByQuery()
  loadRows()
}

function resetQuery() {
  query.parameterId = ''
  query.enabled = ''
  query.bindingStatus = ''
  query.keyword = ''
  query.pageNum = 1
  activeStatKey.value = 'all'
  loadRows()
}

function syncActiveStatByQuery() {
  if (query.bindingStatus === 'BOUND') {
    activeStatKey.value = 'bound'
  } else if (query.bindingStatus === 'UNBOUND') {
    activeStatKey.value = 'unbound'
  } else if (String(query.enabled) === '0') {
    activeStatKey.value = 'disabled'
  } else {
    activeStatKey.value = 'all'
  }
}

async function loadParameterOptions() {
  const result = await fetchDetectionParametersApi({ pageNum: 1, pageSize: 500, enabled: 1 })
  parameterOptions.value = result.records || []
}

async function loadInstrumentModelOptions() {
  instrumentModelOptions.value = await fetchInstrumentModelOptionsApi()
}

async function loadRows() {
  const result = await fetchDetectionMethodsApi({ ...query })
  rows.value = result.records || []
  total.value = Number(result.total || 0)
}

async function loadSummaryRows() {
  const result = await fetchDetectionMethodsApi({
    pageNum: 1,
    pageSize: SUMMARY_PAGE_SIZE
  })
  summaryRows.value = result.records || []
  summaryTotal.value = Number(result.total || 0)
}

async function handleExport() {
  try {
    await exportDetectionMethodsApi({ ...query })
    ElMessage.success('检测方法导出成功')
  } catch (error) {
    ElMessage.error(error.message || '检测方法导出失败')
  }
}

onMounted(async () => {
  await Promise.all([loadRows(), loadSummaryRows(), loadParameterOptions(), loadInstrumentModelOptions()])
})
</script>

<style scoped>
.detection-method-page {
  gap: 12px;
}

.page-hero,
.scene-grid {
  display: grid;
  gap: 12px;
}

.page-hero {
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
}

.hero-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
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
  margin: 8px 0 0;
  color: var(--text-sub);
  font-size: 14px;
  line-height: 1.6;
}

.scene-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.scene-copy {
  display: grid;
  gap: 8px;
  color: var(--text-sub);
  font-size: 14px;
  line-height: 1.7;
}

.scene-copy p {
  margin: 0;
}

.quick-links {
  display: grid;
  gap: 10px;
}

.quick-link {
  display: grid;
  gap: 4px;
  padding: 12px;
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

.detection-method-action-row {
  flex-wrap: wrap;
  gap: 6px;
  min-height: 68px;
  padding: 4px 0;
}

.binding-tree {
  display: grid;
  gap: 8px;
  padding: 4px 0;
  line-height: 1.5;
}

.binding-tree__parameter,
.binding-tree__method {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.binding-tree__children {
  display: grid;
  gap: 6px;
  margin-left: 18px;
}

.binding-tree__dash {
  color: var(--brand);
  font-weight: 800;
}

.binding-tree__dash--child {
  color: var(--text-sub);
}

.binding-tree__parameter-name {
  color: var(--text-main);
  font-weight: 700;
}

.binding-tree__method-name {
  color: var(--text-main);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.binding-tree__count {
  flex: 0 0 auto;
  color: var(--text-sub);
  font-size: 12px;
}

.binding-tree__action {
  flex: 0 0 auto;
  margin-left: auto;
}

.binding-tree__method--empty {
  margin-left: 18px;
  color: var(--text-sub);
}

.binding-editor {
  display: grid;
  gap: 14px;
}

.binding-editor__summary {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.binding-editor__chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid color-mix(in srgb, var(--brand) 16%, #ffffff 84%);
  background: color-mix(in srgb, var(--brand) 7%, #ffffff 93%);
  color: var(--text-sub);
  font-size: 13px;
}

.binding-editor__chip--action {
  appearance: none;
  font-family: inherit;
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.binding-editor__chip--action:hover,
.binding-editor__chip--action:focus-visible,
.binding-editor__chip--action.is-active {
  border-color: color-mix(in srgb, var(--brand) 42%, #ffffff 58%);
  box-shadow: var(--shadow-sm);
  transform: translateY(-1px);
  outline: none;
}

.binding-editor__chip strong {
  color: var(--brand);
  font-size: 15px;
}

.binding-editor__toolbar {
  padding: 12px;
}

.instrument-model-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  max-height: 430px;
  overflow: auto;
  padding-right: 4px;
}

.instrument-model-card {
  display: grid;
  gap: 8px;
  padding: 12px;
  border: 1px solid var(--line-soft);
  border-radius: 12px;
  background: var(--bg-panel-soft);
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.instrument-model-card:hover,
.instrument-model-card.is-selected {
  border-color: color-mix(in srgb, var(--brand) 45%, #ffffff 55%);
  box-shadow: var(--shadow-sm);
  transform: translateY(-1px);
}

.instrument-model-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.instrument-model-card__meta {
  color: var(--text-sub);
  font-size: 13px;
  line-height: 1.5;
}

.empty-box {
  min-height: 180px;
  display: grid;
  place-items: center;
  border: 1px dashed var(--line-soft);
  border-radius: 12px;
  color: var(--text-sub);
}

@media (max-width: 1080px) {
  .page-hero,
  .scene-grid {
    grid-template-columns: 1fr;
  }

  .hero-tags {
    justify-content: flex-start;
  }

  .instrument-model-grid {
    grid-template-columns: 1fr;
  }
}
</style>
