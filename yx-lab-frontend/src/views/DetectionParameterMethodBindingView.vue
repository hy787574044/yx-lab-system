<template>
  <div class="content-grid binding-page">
    <section class="glass-panel section-block page-hero">
      <div>
        <h2 class="page-title">参数方法绑定</h2>
        <p class="page-subtitle">
          以检测参数为主维度统一配置检测方法。一个检测参数可绑定多个检测方法，但一个检测方法同一时间只能归属一个检测参数。
        </p>
      </div>
      <div class="hero-tags">
        <span class="status-chip info">参数总数 {{ total }}</span>
        <span class="status-chip success">已绑定参数 {{ boundParameterCount }}</span>
        <span class="status-chip warning">已分配方法 {{ assignedMethodCount }}</span>
      </div>
    </section>

    <section class="stats-grid">
      <button
        v-for="item in statCards"
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
          <h3 class="section-title">绑定配置列表</h3>
          <p class="page-subtitle">按“检测参数 → 检测方法”的层级关系展示当前绑定情况。</p>
        </div>
      </div>

      <div class="toolbar-panel">
        <div class="toolbar-row">
          <div class="toolbar-fields">
            <label class="toolbar-field toolbar-field--medium">
              <span>关键字</span>
              <el-input
                v-model="query.keyword"
                clearable
                placeholder="请输入检测参数名称、单位、标准或备注"
                @keyup.enter="handleSearch"
              />
            </label>
          </div>

          <div class="toolbar-actions">
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="resetQuery">重置</el-button>
            <el-button @click="reloadData">刷新</el-button>
          </div>
        </div>
        <div class="panel-note">
          绑定保存时会自动覆盖当前参数原有方法；已绑定到其他参数的方法会锁定不可选，必须先解除原绑定后才能重新分配。
        </div>
      </div>

      <div class="table-card">
        <el-table
          class="list-table"
          :data="visibleRows"
          stripe
          max-height="520"
          empty-text="暂无参数方法绑定数据"
        >
          <el-table-column label="参数方法关系" min-width="460">
            <template #default="{ row }">
              <div class="binding-tree">
                <div class="binding-tree__parameter">
                  <span class="binding-tree__dash">-</span>
                  <span class="binding-tree__parameter-name">{{ row.parameterName || '-' }}</span>
                </div>
                <div class="binding-tree__children">
                  <div
                    v-for="method in getBoundMethods(row)"
                    :key="`${row.id}-${method.id || method.methodName}`"
                    class="binding-tree__method"
                  >
                    <span class="binding-tree__dash">-</span>
                    <span>{{ method.methodName }}</span>
                    <el-button
                      v-if="method.id"
                      link
                      type="danger"
                      class="binding-tree__action"
                      :loading="unbindingMethodId === String(method.id)"
                      @click="removeSingleBinding(row, method)"
                    >
                      解除绑定
                    </el-button>
                  </div>
                  <div v-if="!getBoundMethods(row).length" class="binding-tree__method binding-tree__method--empty">
                    <span class="binding-tree__dash">-</span>
                    <span>当前未绑定检测方法</span>
                  </div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="unit" label="单位" width="100">
            <template #default="{ row }">{{ row.unit || '-' }}</template>
          </el-table-column>
          <el-table-column label="标准范围" min-width="170">
            <template #default="{ row }">
              {{ formatStandardRange(row.standardMin, row.standardMax, row.unit) }}
            </template>
          </el-table-column>
          <el-table-column label="已绑定方法数" width="120" class-name="cell-center" header-cell-class-name="cell-center">
            <template #default="{ row }">{{ Number(row.methodCount || 0) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100" class-name="cell-center" header-cell-class-name="cell-center">
            <template #default="{ row }">
              <span :class="['status-chip', row.enabled === 1 ? 'success' : 'warning']">
                {{ row.enabled === 1 ? '启用' : '停用' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="updatedTime" label="更新时间" min-width="170">
            <template #default="{ row }">{{ row.updatedTime || '-' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="190" fixed="right" class-name="cell-center">
            <template #default="{ row }">
              <el-button link type="primary" @click="openBindDialog(row)">配置绑定</el-button>
              <el-button
                link
                type="danger"
                :disabled="Number(row.methodCount || 0) === 0"
                @click="clearBindings(row)"
              >
                清空绑定
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <TablePagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          @change="loadRows"
        />
      </div>
    </section>

    <section class="scene-grid">
      <div class="glass-panel section-block">
        <div class="section-head">
          <h3 class="section-title">配置说明</h3>
        </div>
        <div class="scene-copy">
          <p>检测参数用于定义结果录入项，检测方法用于定义执行标准，两者拆开维护后再做正式绑定。</p>
          <p>如果后续需要改绑，请先在本页解除旧绑定，再重新绑定到新的检测参数，避免一个方法被多个参数同时占用。</p>
        </div>
      </div>

      <div class="glass-panel section-block">
        <div class="section-head">
          <h3 class="section-title">关联入口</h3>
        </div>
        <div class="quick-links">
          <button type="button" class="quick-link" @click="goRoute('/detection-projects')">
            <strong>检测参数</strong>
            <span>维护 pH、浊度、余氯、氨氮等基础检测参数。</span>
          </button>
          <button type="button" class="quick-link" @click="goRoute('/detection-methods')">
            <strong>检测方法</strong>
            <span>维护标准方法台账，并查看每个方法当前绑定到哪个参数。</span>
          </button>
          <button type="button" class="quick-link" @click="goRoute('/detection-project-groups')">
            <strong>检测套餐</strong>
            <span>将多个检测参数组合为样品登录时可选的检测套餐。</span>
          </button>
        </div>
      </div>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="920px"
      destroy-on-close
      @closed="resetDialogState"
    >
      <div class="dialog-summary">
        <div class="summary-chip">
          <span>当前参数</span>
          <strong>{{ currentParameter?.parameterName || '-' }}</strong>
        </div>
        <button
          type="button"
          :class="['summary-chip', 'summary-chip--action', { 'is-active': dialogFilter === 'selected' }]"
          @click="switchDialogFilter('selected')"
        >
          <span>已选方法</span>
          <strong>{{ selectedMethodCount }}</strong>
        </button>
        <button
          type="button"
          :class="['summary-chip', 'summary-chip--action', { 'is-active': dialogFilter === 'pending' }]"
          @click="switchDialogFilter('pending')"
        >
          <span>待绑定方法</span>
          <strong>{{ pendingMethodCount }}</strong>
        </button>
      </div>

      <div class="toolbar-panel binding-dialog-toolbar">
        <div class="toolbar-row">
          <div class="toolbar-fields">
            <label class="toolbar-field toolbar-field--medium">
              <span>方法检索</span>
              <el-input
                v-model="methodKeyword"
                clearable
                placeholder="请输入方法名称、编码、标准编号或已绑定参数"
              />
            </label>
          </div>
          <div class="toolbar-actions">
            <el-button :type="dialogFilter === 'all' ? 'primary' : 'default'" @click="switchDialogFilter('all')">
              全部方法
            </el-button>
          </div>
        </div>
      </div>

      <div class="method-option-grid">
        <div
          v-for="item in filteredMethodOptions"
          :key="item.id"
          :class="[
            'method-option-card',
            {
              'is-disabled': isMethodDisabled(item),
              'is-selected': isMethodSelected(item.id),
              'is-locked': isCurrentBoundMethod(item)
            }
          ]"
        >
          <div class="method-option-head">
            <el-checkbox
              :model-value="isMethodSelected(item.id)"
              :disabled="isMethodDisabled(item) || isCurrentBoundMethod(item)"
              @change="(checked) => handleMethodToggle(item, checked)"
            >
              {{ item.methodName || '未命名方法' }}
            </el-checkbox>
            <span :class="['status-chip', item.enabled === 1 ? 'success' : 'warning']">
              {{ item.enabled === 1 ? '启用' : '停用' }}
            </span>
          </div>
          <div class="method-option-meta">
            <span>编码：{{ item.methodCode || '-' }}</span>
            <span>标准：{{ item.standardCode || '-' }}</span>
          </div>
          <div class="method-option-meta">
            <span>依据：{{ item.methodBasis || '-' }}</span>
          </div>
          <div class="method-option-footer">
            <span v-if="item.parameterId && item.parameterId !== currentParameter?.id" class="binding-tip binding-tip--locked">
              已绑定到参数：{{ item.parameterName || item.parameterId }}
            </span>
            <span v-else-if="item.parameterId === currentParameter?.id" class="binding-tip binding-tip--self">
              当前已绑定
            </span>
            <span v-else class="binding-tip">当前未绑定，可直接选用</span>
          </div>
        </div>
      </div>

      <div v-if="!filteredMethodOptions.length" class="empty-block">
        暂无符合条件的检测方法
      </div>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitBindings">保存绑定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElButton } from 'element-plus/es/components/button/index.mjs'
import { ElCheckbox } from 'element-plus/es/components/checkbox/index.mjs'
import { ElDialog } from 'element-plus/es/components/dialog/index.mjs'
import { ElInput } from 'element-plus/es/components/input/index.mjs'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs'
import { ElTable, ElTableColumn } from 'element-plus/es/components/table/index.mjs'
import TablePagination from '../components/common/TablePagination.vue'
import {
  fetchDetectionMethodOptionsApi,
  fetchDetectionParameterMethodBindingsApi,
  saveDetectionParameterMethodBindingsApi
} from '../api/lab'
import { DEFAULT_PAGE_SIZE } from '../utils/labEnums'

const router = useRouter()

const activeStatKey = ref('all')
const rows = ref([])
const total = ref(0)
const methodOptions = ref([])
const dialogVisible = ref(false)
const saving = ref(false)
const unbindingMethodId = ref('')
const currentParameter = ref(null)
const methodKeyword = ref('')
const dialogFilter = ref('all')

const query = reactive({
  keyword: '',
  pageNum: 1,
  pageSize: DEFAULT_PAGE_SIZE
})

const bindingForm = reactive({
  methodIds: []
})

const visibleRows = computed(() => {
  if (activeStatKey.value === 'bound') {
    return rows.value.filter((item) => Number(item.methodCount || 0) > 0)
  }
  if (activeStatKey.value === 'unbound') {
    return rows.value.filter((item) => Number(item.methodCount || 0) === 0)
  }
  if (activeStatKey.value === 'disabled') {
    return rows.value.filter((item) => item.enabled !== 1)
  }
  return rows.value
})

const selectedMethodIds = computed(() => new Set(bindingForm.methodIds.map((item) => String(item))))

const boundParameterCount = computed(() => {
  const parameterIds = new Set(
    methodOptions.value
      .map((item) => item.parameterId)
      .filter((item) => item != null)
  )
  return parameterIds.size
})

const assignedMethodCount = computed(() => methodOptions.value.filter((item) => item.parameterId != null).length)
const selectedMethodCount = computed(() => bindingForm.methodIds.length)
const pendingMethodCount = computed(() => methodOptions.value.filter((item) => isPendingMethod(item)).length)

const statCards = computed(() => [
  { key: 'all', label: '全部参数', value: total.value, desc: '检测参数绑定总览' },
  { key: 'bound', label: '已绑定参数', value: rows.value.filter((item) => Number(item.methodCount || 0) > 0).length, desc: '当前页已有方法绑定的参数' },
  { key: 'unbound', label: '未绑定参数', value: rows.value.filter((item) => Number(item.methodCount || 0) === 0).length, desc: '当前页尚未配置方法的参数' },
  { key: 'disabled', label: '停用参数', value: rows.value.filter((item) => item.enabled !== 1).length, desc: '当前页停用状态的参数' }
])

const dialogTitle = computed(() => {
  const name = currentParameter.value?.parameterName || ''
  return name ? `配置检测方法 - ${name}` : '配置检测方法'
})

const filteredMethodOptions = computed(() => {
  const keyword = methodKeyword.value.trim().toLowerCase()
  let list = methodOptions.value.filter((item) => {
    const text = [
      item.methodName,
      item.methodCode,
      item.standardCode,
      item.methodBasis,
      item.parameterName
    ]
      .filter(Boolean)
      .join(' ')
      .toLowerCase()
    return !keyword || text.includes(keyword)
  })

  if (dialogFilter.value === 'selected') {
    list = list.filter((item) => isMethodSelected(item.id))
  } else if (dialogFilter.value === 'pending') {
    list = list.filter((item) => isPendingMethod(item))
  }

  return [...list].sort(compareMethodOption)
})

function handleStatClick(key) {
  activeStatKey.value = activeStatKey.value === key ? 'all' : key
}

function switchDialogFilter(key) {
  dialogFilter.value = dialogFilter.value === key ? 'all' : key
}

function parseIdList(value) {
  return String(value || '')
    .split(',')
    .map((item) => item.trim())
    .filter((item) => item !== '')
}

function toMethodNameList(value) {
  return String(value || '')
    .split(/[、,，]/)
    .map((item) => item.trim())
    .filter((item) => item !== '')
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

function isMethodSelected(methodId) {
  return selectedMethodIds.value.has(String(methodId))
}

function isCurrentBoundMethod(item) {
  return currentParameter.value != null && String(item.parameterId) === String(currentParameter.value.id)
}

function isMethodDisabled(item) {
  if (!currentParameter.value) {
    return true
  }
  return item.parameterId != null && String(item.parameterId) !== String(currentParameter.value.id)
}

function isPendingMethod(item) {
  return !isMethodDisabled(item) && !isMethodSelected(item.id)
}

function getBoundMethods(row) {
  const methods = methodOptions.value
    .filter((item) => String(item.parameterId) === String(row?.id))
    .map((item) => ({
      id: String(item.id || '').trim(),
      methodName: item.methodName || '未命名方法'
    }))

  if (methods.length) {
    return methods.sort((left, right) => left.methodName.localeCompare(right.methodName, 'zh-CN'))
  }

  return toMethodNameList(row?.methodNames).map((methodName) => ({
    id: '',
    methodName
  }))
}

function handleMethodToggle(item, checked) {
  const methodId = String(item?.id || '').trim()
  if (!methodId) {
    return
  }
  if (isMethodDisabled(item) || isCurrentBoundMethod(item)) {
    return
  }

  const nextIds = new Set(bindingForm.methodIds.map((id) => String(id)).filter((id) => id))
  if (checked) {
    nextIds.add(methodId)
  } else {
    nextIds.delete(methodId)
  }
  bindingForm.methodIds = Array.from(nextIds)
}

function compareMethodOption(left, right) {
  const leftRank = optionRank(left)
  const rightRank = optionRank(right)
  if (leftRank !== rightRank) {
    return leftRank - rightRank
  }
  return String(left.methodName || '').localeCompare(String(right.methodName || ''), 'zh-CN')
}

function optionRank(item) {
  if (isMethodSelected(item.id)) {
    return 0
  }
  if (!isMethodDisabled(item)) {
    return 1
  }
  return 2
}

async function openBindDialog(row) {
  currentParameter.value = row
  dialogFilter.value = 'all'
  methodKeyword.value = ''
  await loadMethodOptions()

  const boundIdsFromOptions = methodOptions.value
    .filter((item) => String(item.parameterId) === String(row.id))
    .map((item) => String(item.id || '').trim())
    .filter((item) => item)

  bindingForm.methodIds = boundIdsFromOptions.length ? boundIdsFromOptions : parseIdList(row.methodIds)
  dialogVisible.value = true
}

function resetDialogState() {
  currentParameter.value = null
  bindingForm.methodIds = []
  methodKeyword.value = ''
  dialogFilter.value = 'all'
}

function handleSearch() {
  query.pageNum = 1
  loadRows()
}

function resetQuery() {
  query.keyword = ''
  query.pageNum = 1
  activeStatKey.value = 'all'
  loadRows()
}

function goRoute(path) {
  if (path) {
    router.push(path)
  }
}

async function loadRows() {
  const result = await fetchDetectionParameterMethodBindingsApi({ ...query })
  rows.value = result.records || []
  total.value = Number(result.total || 0)
}

async function loadMethodOptions() {
  const result = await fetchDetectionMethodOptionsApi()
  methodOptions.value = Array.isArray(result) ? result : []
}

async function reloadData() {
  await Promise.all([loadRows(), loadMethodOptions()])
}

async function clearBindings(row) {
  await ElMessageBox.confirm(`确认清空检测参数“${row.parameterName}”已绑定的检测方法吗？`, '清空确认', {
    type: 'warning'
  })
  await saveDetectionParameterMethodBindingsApi(row.id, { methodIds: [] })
  ElMessage.success('已清空当前参数的检测方法绑定')
  await reloadData()
}

async function removeSingleBinding(row, method) {
  const methodId = String(method?.id || '').trim()
  if (!methodId) {
    return
  }

  await ElMessageBox.confirm(
    `确认解除检测参数“${row.parameterName}”下的检测方法“${method.methodName}”吗？`,
    '解除绑定确认',
    { type: 'warning' }
  )

  const remainingMethodIds = getBoundMethods(row)
    .map((item) => String(item.id || '').trim())
    .filter((item) => item && item !== methodId)

  unbindingMethodId.value = methodId
  try {
    await saveDetectionParameterMethodBindingsApi(row.id, { methodIds: remainingMethodIds })
    ElMessage.success('已解除当前检测方法绑定')
    await reloadData()
  } finally {
    unbindingMethodId.value = ''
  }
}

async function submitBindings() {
  if (!currentParameter.value?.id) {
    return
  }
  saving.value = true
  try {
    const validMethodIds = new Set(
      methodOptions.value
        .map((item) => String(item.id || '').trim())
        .filter((item) => item)
    )
    const methodIds = Array.from(
      new Set(
        bindingForm.methodIds
          .map((item) => String(item || '').trim())
          .filter((item) => item && validMethodIds.has(item))
      )
    )

    await saveDetectionParameterMethodBindingsApi(currentParameter.value.id, {
      methodIds
    })
    ElMessage.success('参数方法绑定已保存')
    dialogVisible.value = false
    await reloadData()
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await reloadData()
})
</script>

<style scoped>
.binding-page {
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

.quick-link span,
.text-placeholder {
  color: var(--text-sub);
  font-size: 13px;
  line-height: 1.6;
}

.binding-tree {
  display: grid;
  gap: 6px;
}

.binding-tree__parameter,
.binding-tree__method {
  display: flex;
  align-items: center;
  gap: 6px;
  line-height: 1.7;
}

.binding-tree__children {
  padding-left: 18px;
  border-left: 1px dashed rgba(var(--theme-primary-rgb), 0.2);
  margin-left: 6px;
  display: grid;
  gap: 4px;
}

.binding-tree__dash {
  color: var(--text-sub);
  width: 10px;
  flex: 0 0 10px;
}

.binding-tree__parameter-name {
  color: var(--text-main);
  font-weight: 700;
}

.binding-tree__method {
  color: var(--text-secondary);
}

.binding-tree__action {
  margin-left: 6px;
}

.binding-tree__method--empty {
  color: var(--text-sub);
}

.dialog-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.summary-chip {
  padding: 14px 16px;
  border-radius: 14px;
  border: 1px solid var(--line-soft);
  background: var(--bg-panel-soft);
  display: grid;
  gap: 4px;
}

.summary-chip--action {
  text-align: left;
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.summary-chip--action:hover,
.summary-chip--action.is-active {
  border-color: rgba(var(--theme-primary-rgb), 0.35);
  box-shadow: 0 12px 24px rgba(var(--theme-primary-rgb), 0.12);
  transform: translateY(-1px);
}

.summary-chip span {
  color: var(--text-sub);
  font-size: 12px;
}

.summary-chip strong {
  color: var(--text-main);
  font-size: 16px;
}

.binding-dialog-toolbar {
  margin-bottom: 16px;
}

.method-option-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  max-height: 420px;
  overflow-y: auto;
  padding-right: 6px;
}

.method-option-card {
  display: grid;
  gap: 10px;
  padding: 14px;
  border-radius: 14px;
  border: 1px solid var(--line-soft);
  background: rgba(255, 255, 255, 0.88);
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.method-option-card:hover {
  border-color: rgba(var(--theme-primary-rgb), 0.28);
  box-shadow: 0 12px 24px rgba(var(--theme-primary-rgb), 0.1);
  transform: translateY(-1px);
}

.method-option-card.is-disabled {
  background: rgba(245, 247, 250, 0.88);
}

.method-option-card.is-selected {
  border-color: rgba(var(--theme-primary-rgb), 0.5);
  box-shadow: 0 14px 28px rgba(var(--theme-primary-rgb), 0.14);
}

.method-option-card.is-locked {
  background: rgba(236, 247, 240, 0.9);
}

.method-option-head,
.method-option-meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.method-option-head {
  align-items: center;
}

.method-option-meta,
.method-option-footer {
  color: var(--text-sub);
  font-size: 12px;
  line-height: 1.6;
}

.binding-tip {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
}

.binding-tip--locked {
  color: #c45656;
}

.binding-tip--self {
  color: #3e8b52;
}

.empty-block {
  margin-top: 16px;
  padding: 18px 0;
  text-align: center;
  color: var(--text-sub);
  border-top: 1px dashed var(--line-soft);
}

@media (max-width: 1100px) {
  .page-hero,
  .scene-grid,
  .dialog-summary,
  .method-option-grid {
    grid-template-columns: 1fr;
  }

  .hero-tags {
    justify-content: flex-start;
  }
}
</style>
