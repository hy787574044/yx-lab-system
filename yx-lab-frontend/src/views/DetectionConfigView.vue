<template>
  <div class="content-grid detection-config-page">
    <section class="glass-panel section-block page-hero">
      <div>
        <h2 class="page-title">{{ currentScene.title }}</h2>
        <p class="page-subtitle">{{ currentScene.subtitle }}</p>
      </div>
      <div class="hero-tags">
        <span
          v-for="tag in currentTags"
          :key="tag.label"
          :class="['status-chip', tag.type || 'info']"
        >
          {{ tag.label }} {{ tag.value }}
        </span>
      </div>
    </section>

    <section class="stats-grid">
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

    <section class="glass-panel section-block">
      <div class="section-head">
        <div>
          <h3 class="section-title">{{ currentScene.tableTitle }}</h3>
          <p class="page-subtitle">{{ currentScene.tableSubtitle }}</p>
        </div>
      </div>

      <template v-if="isParameterScene">
        <div class="toolbar-panel">
          <div class="toolbar-row">
            <div class="toolbar-fields">
              <label class="toolbar-field toolbar-field--medium">
                <span>关键字</span>
                <el-input
                  v-model="parameterQuery.keyword"
                  clearable
                  placeholder="请输入参数名称、单位、标准或备注"
                  @keyup.enter="handleParameterSearch"
                />
              </label>
            </div>

            <div class="toolbar-actions">
              <el-button type="primary" @click="handleParameterSearch">查询</el-button>
              <el-button @click="resetParameterQuery">重置</el-button>
              <el-button @click="reloadData">刷新</el-button>
              <el-button type="primary" plain @click="openParameterDialog()">新增检测参数</el-button>
            </div>
          </div>
          <div class="panel-note">
            这里维护的是最基础的检测参数，例如 pH、浊度、余氯、氨氮等。后续检测项目组只能从这里选择多个不重复参数进行组合。
          </div>
        </div>

        <div class="table-card">
          <el-table
            class="list-table"
            :data="visibleParameterRows"
            stripe
            max-height="480"
            empty-text="暂无检测参数数据"
          >
            <el-table-column prop="parameterName" label="检测参数" min-width="160" />
            <el-table-column prop="unit" label="单位" min-width="100">
              <template #default="{ row }">{{ row.unit || '-' }}</template>
            </el-table-column>
            <el-table-column label="标准范围" min-width="180">
              <template #default="{ row }">
                {{ formatStandardRange(row.standardMin, row.standardMax, row.unit) }}
              </template>
            </el-table-column>
            <el-table-column prop="referenceStandard" label="参考标准" min-width="180" show-overflow-tooltip>
              <template #default="{ row }">{{ row.referenceStandard || '-' }}</template>
            </el-table-column>
            <el-table-column prop="exceedRule" label="判定规则" min-width="220" show-overflow-tooltip>
              <template #default="{ row }">{{ row.exceedRule || '-' }}</template>
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
            <el-table-column label="操作" width="170" fixed="right" class-name="cell-center">
              <template #default="{ row }">
                <el-button link type="primary" @click="openParameterDialog(row)">编辑</el-button>
                <el-button link type="danger" @click="removeParameter(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <TablePagination
            v-model:current-page="parameterQuery.pageNum"
            v-model:page-size="parameterQuery.pageSize"
            :total="parameterTotal"
            @change="loadParameters"
          />
        </div>
      </template>

      <template v-else>
        <div class="toolbar-panel">
          <div class="toolbar-row">
            <div class="toolbar-fields">
              <label class="toolbar-field toolbar-field--medium">
                <span>关键字</span>
                <el-input
                  v-model="groupQuery.keyword"
                  clearable
                  placeholder="请输入项目组名称、参数名称或备注"
                  @keyup.enter="handleGroupSearch"
                />
              </label>
            </div>

            <div class="toolbar-actions">
              <el-button type="primary" @click="handleGroupSearch">查询</el-button>
              <el-button @click="resetGroupQuery">重置</el-button>
              <el-button @click="reloadData">刷新</el-button>
              <el-button type="primary" plain @click="openGroupDialog()">新增检测套餐</el-button>
            </div>
          </div>
          <div class="panel-note">
            检测套餐由多个检测参数组成，组内参数自动去重。样品登录时只能单选一个检测套餐，后续检测时会按该套餐自动带出参数清单。
          </div>
        </div>

        <div class="table-card">
          <el-table
            class="list-table"
            :data="visibleGroupRows"
            stripe
            max-height="480"
            empty-text="暂无检测套餐数据"
          >
            <el-table-column prop="typeName" label="套餐名称" min-width="180" />
            <el-table-column prop="parameterNames" label="组内参数" min-width="280" show-overflow-tooltip>
              <template #default="{ row }">{{ row.parameterNames || '-' }}</template>
            </el-table-column>
            <el-table-column label="参数数量" width="110" class-name="cell-center" header-cell-class-name="cell-center">
              <template #default="{ row }">{{ countParameterIds(row.parameterIds) }}</template>
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
            <el-table-column label="操作" width="170" fixed="right" class-name="cell-center">
              <template #default="{ row }">
                <el-button link type="primary" @click="openGroupDialog(row)">编辑</el-button>
                <el-button link type="danger" @click="removeGroup(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <TablePagination
            v-model:current-page="groupQuery.pageNum"
            v-model:page-size="groupQuery.pageSize"
            :total="groupTotal"
            @change="loadGroups"
          />
        </div>
      </template>
    </section>

    <section class="scene-grid">
      <div class="glass-panel section-block">
        <div class="section-head">
          <h3 class="section-title">页面说明</h3>
        </div>
        <div class="scene-copy">
          <p>{{ currentScene.guide }}</p>
          <p>{{ currentScene.constraint }}</p>
        </div>
      </div>

      <div class="glass-panel section-block">
        <div class="section-head">
          <h3 class="section-title">关联入口</h3>
        </div>
        <div class="quick-links">
          <button
            v-for="item in currentScene.quickLinks"
            :key="item.path"
            type="button"
            class="quick-link"
            @click="goRoute(item.path)"
          >
            <strong>{{ item.label }}</strong>
            <span>{{ item.desc }}</span>
          </button>
        </div>
      </div>
    </section>

    <el-dialog
      v-model="parameterDialogVisible"
      :title="parameterForm.id ? '编辑检测参数' : '新增检测参数'"
      width="840px"
      destroy-on-close
      @closed="resetParameterForm"
    >
      <el-form label-width="100px">
        <div class="form-grid">
          <el-form-item label="检测参数">
            <el-input
              v-model="parameterForm.parameterName"
              placeholder="请输入检测参数名称，例如 pH、浊度、余氯"
            />
          </el-form-item>
          <el-form-item label="单位">
            <el-input
              v-model="parameterForm.unit"
              placeholder="请输入参数单位，例如 NTU、mg/L"
            />
          </el-form-item>
          <el-form-item label="标准下限">
            <el-input v-model="parameterForm.standardMin" placeholder="可为空" />
          </el-form-item>
          <el-form-item label="标准上限">
            <el-input v-model="parameterForm.standardMax" placeholder="可为空" />
          </el-form-item>
          <el-form-item label="参考标准">
            <el-input v-model="parameterForm.referenceStandard" placeholder="请输入参考标准" />
          </el-form-item>
          <el-form-item label="状态">
            <el-radio-group v-model="parameterForm.enabled">
              <el-radio-button :label="1">启用</el-radio-button>
              <el-radio-button :label="0">停用</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item class="form-span-2" label="判定规则">
            <el-input
              v-model="parameterForm.exceedRule"
              type="textarea"
              :rows="3"
              placeholder="请输入超标或异常判定规则"
            />
          </el-form-item>
          <el-form-item class="form-span-2" label="备注">
            <el-input
              v-model="parameterForm.remark"
              type="textarea"
              :rows="3"
              placeholder="请输入维护备注"
            />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="parameterDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingParameter" @click="submitParameterForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="groupDialogVisible"
      :title="groupForm.id ? '编辑检测套餐' : '新增检测套餐'"
      width="840px"
      destroy-on-close
      @closed="resetGroupForm"
    >
      <el-form label-width="100px">
        <div class="form-grid">
          <el-form-item label="项目组名称">
            <el-input v-model="groupForm.typeName" placeholder="请输入检测套餐名称" />
          </el-form-item>
          <el-form-item label="状态">
            <el-radio-group v-model="groupForm.enabled">
              <el-radio-button :label="1">启用</el-radio-button>
              <el-radio-button :label="0">停用</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item class="form-span-2" label="组内参数">
            <el-select
              v-model="groupForm.parameterIdList"
              multiple
              filterable
              collapse-tags
              collapse-tags-tooltip
              placeholder="请选择组内检测参数"
              style="width: 100%"
            >
              <el-option
                v-for="item in enabledParameterOptions"
                :key="item.id"
                :label="item.parameterName"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item class="form-span-2" label="备注">
            <el-input
              v-model="groupForm.remark"
              type="textarea"
              :rows="3"
              placeholder="请输入检测套餐说明"
            />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="groupDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingGroup" @click="submitGroupForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElButton } from 'element-plus/es/components/button/index.mjs'
import { ElDialog } from 'element-plus/es/components/dialog/index.mjs'
import { ElForm, ElFormItem } from 'element-plus/es/components/form/index.mjs'
import { ElInput } from 'element-plus/es/components/input/index.mjs'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs'
import { ElOption, ElSelect } from 'element-plus/es/components/select/index.mjs'
import { ElRadioButton, ElRadioGroup } from 'element-plus/es/components/radio/index.mjs'
import { ElTable, ElTableColumn } from 'element-plus/es/components/table/index.mjs'
import TablePagination from '../components/common/TablePagination.vue'
import {
  createDetectionParameterApi,
  createDetectionTypeApi,
  deleteDetectionParameterApi,
  deleteDetectionTypeApi,
  fetchDetectionParametersApi,
  fetchDetectionTypesApi,
  updateDetectionParameterApi,
  updateDetectionTypeApi
} from '../api/lab'
import { DEFAULT_PAGE_SIZE } from '../utils/labEnums'

const route = useRoute()
const router = useRouter()

const activeStatKey = ref('all')
const parameterRows = ref([])
const parameterTotal = ref(0)
const allParameters = ref([])
const groupRows = ref([])
const groupTotal = ref(0)

const parameterDialogVisible = ref(false)
const groupDialogVisible = ref(false)
const savingParameter = ref(false)
const savingGroup = ref(false)

const parameterQuery = reactive({
  keyword: '',
  pageNum: 1,
  pageSize: DEFAULT_PAGE_SIZE
})

const groupQuery = reactive({
  keyword: '',
  pageNum: 1,
  pageSize: DEFAULT_PAGE_SIZE
})

const parameterForm = reactive({
  id: null,
  parameterName: '',
  standardMin: '',
  standardMax: '',
  unit: '',
  exceedRule: '',
  referenceStandard: '',
  enabled: 1,
  remark: ''
})

const groupForm = reactive({
  id: null,
  typeName: '',
  parameterIdList: [],
  enabled: 1,
  remark: ''
})

const sceneMap = {
  '/detection-projects': {
    title: '检测参数',
    subtitle: '先维护单个检测参数，再为项目组提供参数来源。',
    tableTitle: '检测参数列表',
    tableSubtitle: '这里维护的是 pH、浊度、余氯、氨氮等单个检测参数。',
    guide: '检测参数是最底层配置，样品检测实际录入的也是这些参数结果。',
    constraint: '检测套餐只能选择这里已经维护好的参数，建议停用参数不要再加入新的套餐。',
    quickLinks: [
      { path: '/detection-project-groups', label: '检测套餐', desc: '把多个检测参数组合成一个正式检测套餐' },
      { path: '/sample-login', label: '样品登录', desc: '登录样品时单选一个检测套餐' },
      { path: '/detection-analysis', label: '检测分析', desc: '按样品选择的项目组自动装载参数清单' }
    ]
  },
  '/detection-project-groups': {
    title: '检测套餐',
    subtitle: '从检测参数台账中选择多个不重复参数，组成一个检测套餐。',
    tableTitle: '检测套餐列表',
    tableSubtitle: '样品登录时只能单选一个检测套餐，后续检测结果按套餐内参数录入。',
    guide: '检测套餐相当于样品登录时选择的检测方案，一个样品对应一个检测套餐。',
    constraint: '保存检测套餐时会自动去重组内参数，检测流程中的历史样品不建议频繁改动正在使用的套餐结构。',
    quickLinks: [
      { path: '/detection-projects', label: '检测参数', desc: '先维护单个参数，再回来组合检测套餐' },
      { path: '/sample-login', label: '样品登录', desc: '查看检测套餐是否已出现在样品登录下拉中' },
      { path: '/detection-analysis', label: '检测分析', desc: '查看检测套餐参数是否能正确带入检测页面' }
    ]
  }
}

const isParameterScene = computed(() => route.path === '/detection-projects')
const currentScene = computed(() => sceneMap[route.path] || sceneMap['/detection-projects'])
const enabledParameterOptions = computed(() => allParameters.value.filter((item) => item.enabled === 1))

const visibleParameterRows = computed(() => {
  if (activeStatKey.value === 'enabled') {
    return parameterRows.value.filter((item) => item.enabled === 1)
  }
  if (activeStatKey.value === 'disabled') {
    return parameterRows.value.filter((item) => item.enabled === 0)
  }
  return parameterRows.value
})

const visibleGroupRows = computed(() => {
  if (activeStatKey.value === 'enabled') {
    return groupRows.value.filter((item) => item.enabled === 1)
  }
  if (activeStatKey.value === 'disabled') {
    return groupRows.value.filter((item) => item.enabled === 0)
  }
  if (activeStatKey.value === 'multi') {
    return groupRows.value.filter((item) => countParameterIds(item.parameterIds) >= 2)
  }
  return groupRows.value
})

const currentTags = computed(() => (
  isParameterScene.value
    ? [
      { label: '参数总数', value: parameterTotal.value, type: 'info' },
      { label: '启用参数', value: allParameters.value.filter((item) => item.enabled === 1).length, type: 'success' },
      { label: '已建项目组', value: groupRows.value.length, type: 'warning' }
    ]
    : [
      { label: '项目组总数', value: groupTotal.value, type: 'info' },
      { label: '启用项目组', value: groupRows.value.filter((item) => item.enabled === 1).length, type: 'success' },
      { label: '多参数组', value: groupRows.value.filter((item) => countParameterIds(item.parameterIds) >= 2).length, type: 'warning' }
    ]
))

const currentStats = computed(() => (
  isParameterScene.value
    ? [
      { key: 'all', label: '全部参数', value: parameterTotal.value, desc: '检测参数全量台账' },
      { key: 'enabled', label: '启用参数', value: allParameters.value.filter((item) => item.enabled === 1).length, desc: '可用于新建项目组的参数' },
      { key: 'disabled', label: '停用参数', value: allParameters.value.filter((item) => item.enabled === 0).length, desc: '暂不建议继续使用的参数' }
    ]
    : [
      { key: 'all', label: '全部套餐', value: groupTotal.value, desc: '检测套餐全量台账' },
      { key: 'enabled', label: '启用套餐', value: groupRows.value.filter((item) => item.enabled === 1).length, desc: '样品登录可选检测套餐' },
      { key: 'disabled', label: '停用套餐', value: groupRows.value.filter((item) => item.enabled === 0).length, desc: '已停用检测套餐' },
      { key: 'multi', label: '多参数组', value: groupRows.value.filter((item) => countParameterIds(item.parameterIds) >= 2).length, desc: '组内包含多个检测参数' }
    ]
))

function handleStatClick(key) {
  activeStatKey.value = activeStatKey.value === key ? 'all' : key
}

function goRoute(path) {
  if (path && route.path !== path) {
    router.push(path)
  }
}

function parseParameterIds(value) {
  return String(value || '')
    .split(',')
    .map((item) => item.trim())
    .filter((item) => item !== '')
    .map((item) => Number(item))
    .filter((item) => Number.isFinite(item))
}

function countParameterIds(value) {
  return parseParameterIds(value).length
}

function toNullableNumber(value) {
  const text = String(value ?? '').trim()
  if (!text) {
    return null
  }
  const num = Number(text)
  return Number.isFinite(num) ? num : null
}

function formatStandardRange(min, max, unit) {
  const suffix = unit ? ` ${unit}` : ''
  if (min != null && max != null) {
    return `${min} ~ ${max}${suffix}`
  }
  if (min != null) {
    return `>= ${min}${suffix}`
  }
  if (max != null) {
    return `<= ${max}${suffix}`
  }
  return '-'
}

function resetParameterForm() {
  parameterForm.id = null
  parameterForm.parameterName = ''
  parameterForm.standardMin = ''
  parameterForm.standardMax = ''
  parameterForm.unit = ''
  parameterForm.exceedRule = ''
  parameterForm.referenceStandard = ''
  parameterForm.enabled = 1
  parameterForm.remark = ''
}

function resetGroupForm() {
  groupForm.id = null
  groupForm.typeName = ''
  groupForm.parameterIdList = []
  groupForm.enabled = 1
  groupForm.remark = ''
}

function openParameterDialog(row) {
  resetParameterForm()
  if (row) {
    parameterForm.id = row.id
    parameterForm.parameterName = row.parameterName || ''
    parameterForm.standardMin = row.standardMin ?? ''
    parameterForm.standardMax = row.standardMax ?? ''
    parameterForm.unit = row.unit || ''
    parameterForm.exceedRule = row.exceedRule || ''
    parameterForm.referenceStandard = row.referenceStandard || ''
    parameterForm.enabled = row.enabled ?? 1
    parameterForm.remark = row.remark || ''
  }
  parameterDialogVisible.value = true
}

function openGroupDialog(row) {
  resetGroupForm()
  if (row) {
    groupForm.id = row.id
    groupForm.typeName = row.typeName || ''
    groupForm.parameterIdList = parseParameterIds(row.parameterIds)
    groupForm.enabled = row.enabled ?? 1
    groupForm.remark = row.remark || ''
  }
  groupDialogVisible.value = true
}

async function submitParameterForm() {
  if (!parameterForm.parameterName.trim()) {
    ElMessage.warning('请填写检测参数名称')
    return
  }

  const payload = {
    parameterName: parameterForm.parameterName.trim(),
    standardMin: toNullableNumber(parameterForm.standardMin),
    standardMax: toNullableNumber(parameterForm.standardMax),
    unit: parameterForm.unit.trim(),
    exceedRule: parameterForm.exceedRule.trim(),
    referenceStandard: parameterForm.referenceStandard.trim(),
    enabled: parameterForm.enabled,
    remark: parameterForm.remark.trim()
  }

  savingParameter.value = true
  try {
    if (parameterForm.id) {
      await updateDetectionParameterApi(parameterForm.id, payload)
    } else {
      await createDetectionParameterApi(payload)
    }
    ElMessage.success(parameterForm.id ? '检测参数已更新' : '检测参数已新增')
    parameterDialogVisible.value = false
    await refreshAll()
  } finally {
    savingParameter.value = false
  }
}

async function submitGroupForm() {
  if (!groupForm.typeName.trim()) {
    ElMessage.warning('请填写项目组名称')
    return
  }
  if (!groupForm.parameterIdList.length) {
    ElMessage.warning('请至少选择一个检测参数')
    return
  }

  const uniqueIds = Array.from(new Set(groupForm.parameterIdList))
  const parameterMap = new Map(allParameters.value.map((item) => [item.id, item]))
  const selectedParameters = uniqueIds
    .map((id) => parameterMap.get(id))
    .filter(Boolean)

  if (!selectedParameters.length) {
    ElMessage.warning('当前未找到可用的检测参数，请刷新后重试')
    return
  }

  const payload = {
    typeName: groupForm.typeName.trim(),
    parameterIds: uniqueIds.join(','),
    parameterNames: selectedParameters.map((item) => item.parameterName).join('、'),
    enabled: groupForm.enabled,
    remark: groupForm.remark.trim()
  }

  savingGroup.value = true
  try {
    if (groupForm.id) {
      await updateDetectionTypeApi(groupForm.id, payload)
    } else {
      await createDetectionTypeApi(payload)
    }
    ElMessage.success(groupForm.id ? '检测套餐已更新' : '检测套餐已新增')
    groupDialogVisible.value = false
    await refreshAll()
  } finally {
    savingGroup.value = false
  }
}

async function removeParameter(row) {
  await ElMessageBox.confirm(`确定删除检测参数“${row.parameterName}”吗？`, '删除确认', {
    type: 'warning'
  })
  await deleteDetectionParameterApi(row.id)
  ElMessage.success('检测参数已删除')
  await refreshAll()
}

async function removeGroup(row) {
  await ElMessageBox.confirm(`确定删除检测套餐“${row.typeName}”吗？`, '删除确认', {
    type: 'warning'
  })
  await deleteDetectionTypeApi(row.id)
  ElMessage.success('检测套餐已删除')
  await refreshAll()
}

function handleParameterSearch() {
  parameterQuery.pageNum = 1
  loadParameters()
}

function handleGroupSearch() {
  groupQuery.pageNum = 1
  loadGroups()
}

function resetParameterQuery() {
  parameterQuery.keyword = ''
  parameterQuery.pageNum = 1
  loadParameters()
}

function resetGroupQuery() {
  groupQuery.keyword = ''
  groupQuery.pageNum = 1
  loadGroups()
}

async function loadParameterOptions() {
  const result = await fetchDetectionParametersApi({ pageNum: 1, pageSize: 500 })
  allParameters.value = result.records || []
}

async function loadParameters() {
  const result = await fetchDetectionParametersApi({ ...parameterQuery })
  parameterRows.value = result.records || []
  parameterTotal.value = Number(result.total || 0)
}

async function loadGroups() {
  const result = await fetchDetectionTypesApi({ ...groupQuery })
  groupRows.value = result.records || []
  groupTotal.value = Number(result.total || 0)
}

async function refreshAll() {
  await loadParameterOptions()
  await Promise.all([loadParameters(), loadGroups()])
}

async function reloadData() {
  await refreshAll()
}

function syncRouteState() {
  activeStatKey.value = 'all'
}

onMounted(async () => {
  syncRouteState()
  await refreshAll()
})

watch(() => route.fullPath, async () => {
  syncRouteState()
  await refreshAll()
})
</script>

<style scoped>
.detection-config-page {
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

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.form-span-2 {
  grid-column: 1 / -1;
}

@media (max-width: 900px) {
  .page-hero,
  .scene-grid,
  .form-grid {
    grid-template-columns: 1fr;
  }

  .hero-tags {
    justify-content: flex-start;
  }
}
</style>
