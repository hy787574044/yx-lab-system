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
            检测套餐由多个检测参数组成，并要求为每个参数选定可用检测方法。样品登录时只能单选一个检测套餐，后续检测时会按该套餐自动带出参数清单。
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
            <el-table-column prop="parameterNames" label="组内参数" min-width="220" show-overflow-tooltip>
              <template #default="{ row }">{{ row.parameterNames || '-' }}</template>
            </el-table-column>
            <el-table-column prop="parameterMethodNames" label="参数检测方法" min-width="320" show-overflow-tooltip>
              <template #default="{ row }">{{ row.parameterMethodNames || '-' }}</template>
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
          <el-form-item label="套餐名称">
            <el-input v-model="groupForm.typeName" placeholder="请输入检测套餐名称" />
          </el-form-item>
          <el-form-item label="状态">
            <el-radio-group v-model="groupForm.enabled">
              <el-radio-button :label="1">启用</el-radio-button>
              <el-radio-button :label="0">停用</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item class="form-span-2" label="参数方法绑定">
            <div class="binding-editor">
              <div class="binding-editor__select">
                <div class="binding-editor__summary">
                  <span class="binding-editor__chip">
                    已选参数 <strong>{{ selectedGroupParameterCount }}</strong>
                  </span>
                  <span class="binding-editor__chip">
                    已选方法 <strong>{{ selectedGroupMethodCount }}</strong>
                  </span>
                </div>
                <div class="binding-workbench">
                  <div class="binding-workbench__parameters">
                    <button
                      v-for="item in groupParameterMethodOptions"
                      :key="item.parameterId"
                      type="button"
                      :class="['binding-parameter-card', { 'is-active': currentBindingParameterId === item.parameterId }]"
                      @click="switchBindingParameter(item.parameterId)"
                    >
                      <div class="binding-parameter-card__head">
                        <strong>{{ item.label }}</strong>
                        <span>{{ item.children.length }} 项方法</span>
                      </div>
                      <div class="binding-parameter-card__meta">
                        已选 {{ getSelectedMethodCountByParameter(item.parameterId) }} 项
                      </div>
                    </button>
                  </div>

                  <div class="binding-workbench__methods">
                    <div class="binding-workbench__methods-head">
                      <div>
                        <strong>{{ currentBindingParameterLabel || '请选择检测参数' }}</strong>
                        <span>在右侧为当前检测参数选择 1 个检测方法</span>
                      </div>
                      <div v-if="currentBindingParameterId" class="binding-workbench__methods-actions">
                        <el-button link @click="clearCurrentParameterMethods">清空</el-button>
                      </div>
                    </div>

                    <div v-if="currentBindingMethods.length" class="binding-method-list">
                      <label
                        v-for="method in currentBindingMethods"
                        :key="method.methodId"
                        :class="['binding-method-item', { 'is-checked': isMethodSelected(method.methodId) }]"
                      >
                        <el-checkbox
                          :model-value="isMethodSelected(method.methodId)"
                          @change="(checked) => handleMethodSelectionChange(method.methodId, checked)"
                        >
                          <span class="binding-method-item__name">{{ method.label }}</span>
                        </el-checkbox>
                        <span class="binding-method-item__code">{{ method.methodCode || '未填写方法编码' }}</span>
                      </label>
                    </div>
                    <div v-else class="binding-method-empty">
                      当前检测参数下暂无可选检测方法
                    </div>
                  </div>
                </div>
                <div class="form-helper">
                  仅展示已启用且已绑定检测方法的检测参数。每个检测参数在当前检测套餐内只能选择 1 个检测方法。
                </div>
              </div>
              <div class="binding-editor__result">
                <div class="binding-editor__result-head">
                  <strong>已选套餐参数</strong>
                  <span>仅展示已纳入当前检测套餐的参数。</span>
                </div>
                <div v-if="selectedGroupBindings.length" class="group-binding-preview">
                  <div
                    v-for="item in selectedGroupBindings"
                    :key="item.parameterId"
                    class="group-binding-preview__item"
                  >
                    <div class="group-binding-preview__label">
                      {{ item.parameterName }}
                    </div>
                    <div class="group-binding-preview__meta">
                      已选 {{ item.methodIds.length }} 个检测方法
                    </div>
                  </div>
                </div>
                <div v-else class="group-binding-preview group-binding-preview--empty">
                  尚未选择套餐参数
                </div>
              </div>
            </div>
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
import { ElCheckbox } from 'element-plus/es/components/checkbox/index.mjs'
import { ElDialog } from 'element-plus/es/components/dialog/index.mjs'
import { ElForm, ElFormItem } from 'element-plus/es/components/form/index.mjs'
import { ElInput } from 'element-plus/es/components/input/index.mjs'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs'
import { ElRadioButton, ElRadioGroup } from 'element-plus/es/components/radio/index.mjs'
import { ElTable, ElTableColumn } from 'element-plus/es/components/table/index.mjs'
import TablePagination from '../components/common/TablePagination.vue'
import {
  createDetectionParameterApi,
  createDetectionTypeApi,
  deleteDetectionParameterApi,
  deleteDetectionTypeApi,
  fetchDetectionMethodOptionsApi,
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
const detectionMethodOptions = ref([])
const groupRows = ref([])
const groupTotal = ref(0)

const parameterDialogVisible = ref(false)
const groupDialogVisible = ref(false)
const savingParameter = ref(false)
const savingGroup = ref(false)
const currentBindingParameterId = ref('')

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
  parameterMethodSelection: [],
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
    subtitle: '从检测参数台账中选择参数，并绑定对应检测方法，组成一个检测套餐。',
    tableTitle: '检测套餐列表',
    tableSubtitle: '样品登录时只能单选一个检测套餐，后续检测结果按套餐内参数录入。',
    guide: '检测套餐相当于样品登录时选择的检测方案，一个样品对应一个检测套餐。',
    constraint: '保存检测套餐时会自动去重组内参数，并校验检测方法必须归属对应参数，检测流程中的历史样品不建议频繁改动正在使用的套餐结构。',
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
const parameterOptionMap = computed(() => new Map(
  allParameters.value.map((item) => [String(item.id), item])
))
const methodOptionMap = computed(() => new Map(
  detectionMethodOptions.value.map((item) => [String(item.id), item])
))
const groupParameterMethodOptions = computed(() => {
  const methodMap = new Map()
  detectionMethodOptions.value.forEach((item) => {
    if (item.enabled !== 1 || !item.parameterId) {
      return
    }
    const parameterId = String(item.parameterId)
    const children = methodMap.get(parameterId) || []
    children.push({
      value: String(item.id),
      label: item.methodName || `检测方法-${item.id}`,
      methodId: String(item.id),
      methodCode: item.methodCode || ''
    })
    methodMap.set(parameterId, children)
  })

  return enabledParameterOptions.value
    .map((item) => {
      const parameterId = String(item.id)
      const children = (methodMap.get(parameterId) || []).sort((left, right) => left.label.localeCompare(right.label, 'zh-CN'))
      return {
        value: `parameter:${parameterId}`,
        label: item.parameterName,
        parameterId,
        children
      }
    })
    .filter((item) => item.children.length > 0)
})
const selectedGroupBindings = computed(() => buildGroupBindings())
const selectedGroupParameterCount = computed(() => selectedGroupBindings.value.length)
const selectedGroupMethodCount = computed(() => selectedGroupBindings.value.reduce(
  (total, item) => total + item.methodIds.length,
  0
))
const currentBindingParameterOption = computed(() => {
  if (!currentBindingParameterId.value) {
    return null
  }
  return groupParameterMethodOptions.value.find((item) => item.parameterId === currentBindingParameterId.value) || null
})
const currentBindingParameterLabel = computed(() => currentBindingParameterOption.value?.label || '')
const currentBindingMethods = computed(() => {
  const children = currentBindingParameterOption.value?.children || []
  return children.map((item) => ({
    methodId: item.methodId,
    label: item.label,
    methodCode: item.methodCode
  }))
})

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

function parseBindingJson(value) {
  const text = String(value || '').trim()
  if (!text) {
    return []
  }
  try {
    const list = JSON.parse(text)
    return Array.isArray(list) ? list : []
  } catch (error) {
    return []
  }
}

function buildGroupBindings(selectedMethodIds = groupForm.parameterMethodSelection) {
  const parameterOrderMap = new Map(
    enabledParameterOptions.value.map((item, index) => [String(item.id), index])
  )
  const bindingMap = new Map()
  Array.from(new Set(normalizeSingleMethodSelection(selectedMethodIds)))
    .forEach((methodId) => {
      const method = methodOptionMap.value.get(methodId)
      if (!method || !method.parameterId) {
        return
      }
      const parameterId = String(method.parameterId)
      const parameter = parameterOptionMap.value.get(parameterId)
      if (!bindingMap.has(parameterId)) {
        bindingMap.set(parameterId, {
          parameterId,
          parameterName: parameter?.parameterName || method.parameterName || `参数-${parameterId}`,
          methodIds: [],
          methodNames: []
        })
      }
      const item = bindingMap.get(parameterId)
      if (!item.methodIds.includes(methodId)) {
        item.methodIds.push(methodId)
        item.methodNames.push(method.methodName || `方法-${methodId}`)
      }
    })

  return Array.from(bindingMap.values()).sort((left, right) => {
    const leftOrder = parameterOrderMap.get(left.parameterId) ?? Number.MAX_SAFE_INTEGER
    const rightOrder = parameterOrderMap.get(right.parameterId) ?? Number.MAX_SAFE_INTEGER
    return leftOrder - rightOrder
  })
}

function normalizeSingleMethodSelection(selectionIds) {
  const parameterMethodMap = new Map()
  ;(selectionIds || [])
    .map((item) => String(item || '').trim())
    .filter(Boolean)
    .forEach((methodId) => {
      const method = methodOptionMap.value.get(methodId)
      if (!method || !method.parameterId) {
        return
      }
      const parameterId = String(method.parameterId)
      if (!parameterMethodMap.has(parameterId)) {
        parameterMethodMap.set(parameterId, methodId)
      }
    })
  return Array.from(parameterMethodMap.values())
}

function ensureBindingParameterReady(preferredParameterId = '') {
  const availableParameterIds = groupParameterMethodOptions.value.map((item) => item.parameterId)
  if (!availableParameterIds.length) {
    currentBindingParameterId.value = ''
    return
  }
  if (preferredParameterId && availableParameterIds.includes(preferredParameterId)) {
    currentBindingParameterId.value = preferredParameterId
    return
  }
  if (currentBindingParameterId.value && availableParameterIds.includes(currentBindingParameterId.value)) {
    return
  }
  const selectedParameterIds = selectedGroupBindings.value.map((item) => item.parameterId)
  currentBindingParameterId.value = selectedParameterIds.find((item) => availableParameterIds.includes(item)) || availableParameterIds[0]
}

function switchBindingParameter(parameterId) {
  currentBindingParameterId.value = String(parameterId || '').trim()
}

function isMethodSelected(methodId) {
  return groupForm.parameterMethodSelection.includes(String(methodId || '').trim())
}

function handleMethodSelectionChange(methodId, checked) {
  const normalizedMethodId = String(methodId || '').trim()
  if (!normalizedMethodId) {
    return
  }
  const currentMethodIds = new Set(currentBindingMethods.value.map((item) => item.methodId))
  const nextSelection = new Set(
    groupForm.parameterMethodSelection
      .map((item) => String(item || '').trim())
      .filter((item) => item && !currentMethodIds.has(item))
  )
  if (checked) {
    nextSelection.add(normalizedMethodId)
  }
  groupForm.parameterMethodSelection = normalizeSingleMethodSelection(Array.from(nextSelection))
}

function getSelectedMethodCountByParameter(parameterId) {
  return buildGroupBindings()
    .find((item) => item.parameterId === String(parameterId || '').trim())
    ?.methodIds.length || 0
}

function clearCurrentParameterMethods() {
  if (!currentBindingParameterId.value) {
    return
  }
  const currentMethodIds = new Set(currentBindingMethods.value.map((item) => item.methodId))
  groupForm.parameterMethodSelection = groupForm.parameterMethodSelection
    .map((item) => String(item || '').trim())
    .filter((item) => item && !currentMethodIds.has(item))
}

function resolveGroupMethodSelection(row) {
  const bindings = parseBindingJson(row?.parameterMethodBindings)
  const bindingMethodIds = bindings
    .flatMap((item) => Array.isArray(item?.methodIds) ? item.methodIds : [])
    .map((item) => String(item || '').trim())
    .filter(Boolean)

  if (bindingMethodIds.length) {
    return normalizeSingleMethodSelection(Array.from(new Set(bindingMethodIds)))
  }

  const selectedParameterIds = new Set(parseParameterIds(row?.parameterIds))
  if (!selectedParameterIds.size) {
    return []
  }

  return detectionMethodOptions.value
    .filter((item) => item.enabled === 1 && item.parameterId && selectedParameterIds.has(String(item.parameterId)))
    .map((item) => String(item.id))
    .filter((item, index, list) => index === list.findIndex((methodId) => {
      const method = methodOptionMap.value.get(methodId)
      const current = methodOptionMap.value.get(item)
      return method && current && String(method.parameterId) === String(current.parameterId)
    }))
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
  groupForm.parameterMethodSelection = []
  groupForm.enabled = 1
  groupForm.remark = ''
  currentBindingParameterId.value = ''
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

async function openGroupDialog(row) {
  resetGroupForm()
  if (!detectionMethodOptions.value.length) {
    await loadMethodOptions()
  }
  if (row) {
    groupForm.id = row.id
    groupForm.typeName = row.typeName || ''
    groupForm.parameterIdList = parseParameterIds(row.parameterIds)
    groupForm.parameterMethodSelection = resolveGroupMethodSelection(row)
    groupForm.enabled = row.enabled ?? 1
    groupForm.remark = row.remark || ''
  }
  ensureBindingParameterReady(row ? parseParameterIds(row.parameterIds)[0] : '')
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
    ElMessage.warning('请填写检测套餐名称')
    return
  }
  const bindings = buildGroupBindings()
  if (!bindings.length) {
    ElMessage.warning('请至少选择一个检测参数及其对应检测方法')
    return
  }

  const uniqueIds = bindings.map((item) => item.parameterId)
  const selectedParameters = bindings
    .map((item) => parameterOptionMap.value.get(item.parameterId))
    .filter(Boolean)

  if (!selectedParameters.length) {
    ElMessage.warning('当前未找到可用的检测参数，请刷新后重试')
    return
  }

  groupForm.parameterIdList = [...uniqueIds]

  const payload = {
    typeName: groupForm.typeName.trim(),
    parameterIds: uniqueIds.join(','),
    parameterNames: selectedParameters.map((item) => item.parameterName).join('、'),
    parameterMethodBindings: bindings.map((item) => ({
      parameterId: item.parameterId,
      methodIds: item.methodIds
    })),
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

async function loadMethodOptions() {
  const result = await fetchDetectionMethodOptionsApi()
  detectionMethodOptions.value = Array.isArray(result) ? result : []
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
  await Promise.all([loadParameterOptions(), loadMethodOptions()])
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

watch(groupParameterMethodOptions, () => {
  if (groupDialogVisible.value) {
    ensureBindingParameterReady()
  }
}, { deep: true })

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

.binding-editor {
  display: grid;
  grid-template-columns: 1fr;
  gap: 14px;
  width: 100%;
}

.binding-editor__select,
.binding-editor__result {
  padding: 16px;
  border: 1px solid var(--line-soft);
  border-radius: 16px;
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--brand) 4%, #ffffff 96%) 0%, #ffffff 100%);
  box-shadow: var(--shadow-sm);
}

.binding-editor__summary {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 12px;
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

.binding-editor__chip strong {
  color: var(--brand);
  font-size: 15px;
}

.binding-editor__result {
  display: grid;
  align-content: start;
  gap: 12px;
  width: 100%;
}

.binding-editor__result-head {
  display: grid;
  gap: 4px;
}

.binding-editor__result-head strong {
  color: var(--text-main);
  font-size: 14px;
}

.binding-editor__result-head span {
  color: var(--text-light);
  font-size: 12px;
  line-height: 1.6;
}

.binding-workbench {
  display: grid;
  grid-template-columns: minmax(240px, 280px) minmax(0, 1fr);
  gap: 16px;
}

.binding-workbench__parameters {
  display: grid;
  gap: 10px;
  max-height: 332px;
  overflow-y: auto;
  padding-right: 4px;
}

.binding-parameter-card {
  display: grid;
  gap: 6px;
  width: 100%;
  padding: 14px 16px;
  text-align: left;
  border: 1px solid var(--line-soft);
  border-radius: 14px;
  background: #ffffff;
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.binding-parameter-card:hover,
.binding-parameter-card:focus-visible {
  border-color: color-mix(in srgb, var(--brand) 36%, #ffffff 64%);
  box-shadow: var(--shadow-sm);
  transform: translateY(-1px);
  outline: none;
}

.binding-parameter-card.is-active {
  border-color: var(--brand);
  background: color-mix(in srgb, var(--brand) 8%, #ffffff 92%);
  box-shadow: 0 10px 22px rgba(17, 54, 99, 0.08);
}

.binding-parameter-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.binding-parameter-card__head strong {
  color: var(--text-main);
  font-size: 14px;
  line-height: 1.5;
}

.binding-parameter-card__head span,
.binding-parameter-card__meta {
  color: var(--text-light);
  font-size: 12px;
  line-height: 1.5;
}

.binding-workbench__methods {
  min-height: 332px;
  padding: 16px;
  border-radius: 14px;
  border: 1px solid color-mix(in srgb, var(--brand) 12%, #ffffff 88%);
  background: color-mix(in srgb, var(--brand) 3%, #ffffff 97%);
  min-width: 0;
}

.binding-workbench__methods-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 14px;
}

.binding-workbench__methods-head > div:first-child {
  flex: 1 1 320px;
  min-width: 0;
}

.binding-workbench__methods-head strong {
  display: block;
  color: var(--text-main);
  font-size: 15px;
  line-height: 1.5;
}

.binding-workbench__methods-head span {
  display: block;
  margin-top: 4px;
  color: var(--text-light);
  font-size: 12px;
  line-height: 1.6;
}

.binding-workbench__methods-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.binding-method-list {
  display: grid;
  gap: 10px;
}

.binding-method-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 52px;
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px solid var(--line-soft);
  background: #ffffff;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, background 0.2s ease;
}

.binding-method-item:hover {
  border-color: color-mix(in srgb, var(--brand) 30%, #ffffff 70%);
  box-shadow: var(--shadow-sm);
}

.binding-method-item.is-checked {
  border-color: color-mix(in srgb, var(--brand) 55%, #ffffff 45%);
  background: color-mix(in srgb, var(--brand) 7%, #ffffff 93%);
}

.binding-method-item__name {
  color: var(--text-main);
  font-size: 14px;
}

.binding-method-item__code {
  color: var(--text-light);
  font-size: 12px;
  white-space: nowrap;
}

.binding-method-empty {
  display: grid;
  place-items: center;
  min-height: 210px;
  border: 1px dashed var(--line-strong);
  border-radius: 12px;
  background: #ffffff;
  color: var(--text-light);
  font-size: 13px;
}

.form-helper {
  margin-top: 10px;
  color: var(--text-light);
  font-size: 13px;
  line-height: 1.6;
}

.group-binding-preview {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
}

.group-binding-preview__item {
  display: grid;
  gap: 10px;
  padding: 14px;
  border-radius: 14px;
  border: 1px solid color-mix(in srgb, var(--brand) 12%, #ffffff 88%);
  background: var(--bg-panel-soft);
  box-shadow: 0 6px 16px rgba(17, 54, 99, 0.05);
}

.group-binding-preview__label {
  color: var(--text-main);
  font-size: 14px;
  font-weight: 700;
  line-height: 1.5;
}

.group-binding-preview__meta {
  color: var(--text-light);
  font-size: 12px;
}

.group-binding-preview--empty {
  padding: 20px 16px;
  border: 1px dashed var(--line-strong);
  border-radius: 14px;
  background: color-mix(in srgb, var(--brand) 3%, #ffffff 97%);
  color: var(--text-light);
  font-size: 13px;
  text-align: center;
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

  .binding-workbench {
    grid-template-columns: 1fr;
  }

  .binding-workbench__methods-head,
  .binding-method-item {
    flex-direction: column;
  }

  .binding-method-item {
    align-items: flex-start;
  }

  .binding-method-item__code {
    white-space: normal;
  }

  .group-binding-preview__item {
    padding: 12px;
  }
}
</style>
