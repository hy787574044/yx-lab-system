<template>
  <div class="content-grid flow-config-page fixed-table-page" v-loading="loading">
    <section class="glass-panel section-block fixed-table-section">
      <div class="section-head">
        <div>
          <h3 class="section-title">流程配置</h3>
        </div>
      </div>

      <section class="stats-grid section-stats">
        <button
          v-for="item in stats"
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
            <el-button type="primary" class="toolbar-primary-button" @click="openFlowDialog()">新增流程</el-button>
            <div class="toolbar-fields">
              <label class="toolbar-field toolbar-field--medium">
                <span>流程名称</span>
                <el-input
                  v-model="query.keyword"
                  clearable
                  placeholder="请输入流程名称、适用范围或备注"
                  @keyup.enter="handleSearch"
                />
              </label>
              <label class="toolbar-field">
                <span>流程类型</span>
                <el-select v-model="query.flowType" clearable placeholder="请选择流程类型">
                  <el-option v-for="item in flowTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </label>
              <label class="toolbar-field">
                <span>启用状态</span>
                <el-select v-model="query.status" clearable placeholder="请选择状态">
                  <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </label>
            </div>
          </div>
          <div class="toolbar-actions">
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="resetQuery">重置</el-button>
          </div>
        </div>
      </div>

      <div class="table-card table-card--fixed-scroll">
        <div class="table-card__body">
          <el-table class="list-table" :data="flowRows" stripe height="100%" empty-text="暂无流程配置">
            <el-table-column prop="flowName" label="流程名称" min-width="180" />
            <el-table-column label="流程类型" width="120" header-cell-class-name="cell-center" class-name="cell-center">
              <template #default="{ row }">
                <span class="status-chip info">{{ getFlowTypeLabel(row.flowType) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="scopeName" label="适用范围" min-width="160" />
            <el-table-column label="节点数" width="90" header-cell-class-name="cell-center" class-name="cell-center">
              <template #default="{ row }">{{ row.nodes?.length || row.nodeCount || 0 }}</template>
            </el-table-column>
            <el-table-column label="默认流程" width="110" header-cell-class-name="cell-center" class-name="cell-center">
              <template #default="{ row }">
                <span :class="['status-chip', row.defaultFlag ? 'success' : 'info']">{{ row.defaultFlag ? '默认' : '否' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="启用状态" width="110" header-cell-class-name="cell-center" class-name="cell-center">
              <template #default="{ row }">
                <span :class="['status-chip', row.status === 1 ? 'success' : 'warning']">{{ row.status === 1 ? '启用' : '停用' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="updatedTime" label="更新时间" width="170" />
            <el-table-column prop="remark" label="备注" min-width="220" show-overflow-tooltip>
              <template #default="{ row }">{{ row.remark || '-' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="450" fixed="right" header-cell-class-name="cell-center" class-name="cell-center">
              <template #default="{ row }">
                <div class="table-action-row">
                  <el-button link type="primary" @click="openFlowDialog(row)">编辑</el-button>
                  <el-button link type="primary" @click="openNodeDialog(row)">配置节点</el-button>
                  <el-button link type="primary" @click="toggleStatus(row)">{{ row.status === 1 ? '停用' : '启用' }}</el-button>
                  <el-button link type="primary" :disabled="row.defaultFlag" @click="setDefaultFlow(row)">设为默认</el-button>
                  <el-button link type="danger" @click="removeFlow(row)">删除</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <TablePagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="flowTotal"
          @change="loadFlowConfigs"
        />
      </div>
    </section>

    <el-dialog v-model="flowDialogVisible" :title="flowForm.id ? '编辑流程' : '新增流程'" width="760px" destroy-on-close @closed="resetFlowForm">
      <el-form ref="flowFormRef" :model="flowForm" :rules="flowRules" label-width="100px">
        <div class="form-grid">
          <el-form-item label="流程名称" prop="flowName">
            <el-input v-model="flowForm.flowName" placeholder="请输入流程名称，例如：常规三级审核" />
          </el-form-item>
          <el-form-item label="流程类型" prop="flowType">
            <el-select v-model="flowForm.flowType" placeholder="请选择流程类型" style="width: 100%">
              <el-option v-for="item in flowTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="适用范围" prop="scopeName">
            <el-input v-model="flowForm.scopeName" placeholder="请输入适用范围，例如：全部样品" />
          </el-form-item>
          <el-form-item label="启用状态" prop="status">
            <el-radio-group v-model="flowForm.status">
              <el-radio-button :label="1">启用</el-radio-button>
              <el-radio-button :label="0">停用</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="默认流程">
            <el-radio-group v-model="flowForm.defaultFlag">
              <el-radio-button :label="true">是</el-radio-button>
              <el-radio-button :label="false">否</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item class="form-span-2" label="备注">
            <el-input v-model="flowForm.remark" type="textarea" :rows="3" placeholder="请输入流程用途、适用场景或维护说明" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="flowDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingFlow" @click="submitFlowForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="nodeDialogVisible" :title="nodeDialogTitle" width="920px" destroy-on-close>
      <div class="node-dialog">
        <div class="node-dialog__head">
          <div>
            <strong>节点配置</strong>
            <p>第一版先维护节点名称、审批角色、指定人员和驳回方式，后续接后端后可直接映射为流程节点。</p>
          </div>
          <el-button type="primary" plain @click="addNodeRow">新增节点</el-button>
        </div>

        <el-table class="list-table" :data="nodeDraftRows" stripe border max-height="420" empty-text="暂无节点">
          <el-table-column label="顺序" width="80" header-cell-class-name="cell-center" class-name="cell-center">
            <template #default="{ $index }">{{ $index + 1 }}</template>
          </el-table-column>
          <el-table-column label="节点名称" min-width="150">
            <template #default="{ row }">
              <el-input v-model="row.nodeName" placeholder="例如：初审" />
            </template>
          </el-table-column>
          <el-table-column label="审批角色" min-width="150">
            <template #default="{ row }">
              <el-select
                v-model="row.roleCode"
                filterable
                placeholder="请选择角色"
                @change="handleNodeRoleChange(row)"
              >
                <el-option
                  v-for="item in roleOptions"
                  :key="item.roleCode"
                  :label="item.roleName"
                  :value="item.roleCode"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="指定人员" min-width="150">
            <template #default="{ row }">
              <el-select
                v-model="row.assigneeId"
                clearable
                filterable
                :disabled="!row.roleCode"
                :loading="Boolean(userLoadingMap[row.roleCode])"
                placeholder="可选"
                @change="handleNodeAssigneeChange(row)"
              >
                <el-option
                  v-for="item in getRoleUsers(row.roleCode)"
                  :key="item.id"
                  :label="item.realName || item.username"
                  :value="item.id"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="必审" width="100" header-cell-class-name="cell-center" class-name="cell-center">
            <template #default="{ row }">
              <el-radio-group v-model="row.required">
                <el-radio-button :label="true">是</el-radio-button>
                <el-radio-button :label="false">否</el-radio-button>
              </el-radio-group>
            </template>
          </el-table-column>
          <el-table-column label="驳回方式" min-width="150">
            <template #default="{ row }">
              <el-select v-model="row.rejectMode" placeholder="请选择">
                <el-option v-for="item in rejectModeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="130" fixed="right" header-cell-class-name="cell-center" class-name="cell-center">
            <template #default="{ $index }">
              <div class="table-action-row">
                <el-button link type="primary" :disabled="$index === 0" @click="moveNode($index, -1)">上移</el-button>
                <el-button link type="danger" @click="removeNode($index)">删除</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <template #footer>
        <el-button @click="nodeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingNodes" @click="submitNodes">保存节点</el-button>
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
import { ElLoadingDirective } from 'element-plus/es/components/loading/index.mjs'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs'
import { ElOption, ElSelect } from 'element-plus/es/components/select/index.mjs'
import { ElRadioButton, ElRadioGroup } from 'element-plus/es/components/radio/index.mjs'
import { ElTable, ElTableColumn } from 'element-plus/es/components/table/index.mjs'
import TablePagination from '../components/common/TablePagination.vue'
import {
  createFlowConfigApi,
  deleteFlowConfigApi,
  fetchFlowConfigsApi,
  fetchSystemRoleOptionsApi,
  fetchSystemUsersApi,
  saveFlowConfigNodesApi,
  setDefaultFlowConfigApi,
  updateFlowConfigApi,
  updateFlowConfigStatusApi
} from '../api/lab'
import { DEFAULT_PAGE_SIZE } from '../utils/labEnums'

const FLOW_TYPE_REVIEW = 'REVIEW'
const FLOW_TYPE_PUBLISH = 'PUBLISH'

const vLoading = ElLoadingDirective
const loading = ref(false)
const savingFlow = ref(false)
const savingNodes = ref(false)
const activeStatKey = ref('all')
const flowDialogVisible = ref(false)
const nodeDialogVisible = ref(false)
const flowFormRef = ref()
const currentNodeFlowId = ref('')
const nodeDraftRows = ref([])
const flowRows = ref([])
const flowTotal = ref(0)
const summaryRows = ref([])
const roleOptions = ref([])
const roleUserMap = reactive({})
const userLoadingMap = reactive({})

const flowTypeOptions = [
  { label: '审核流程', value: FLOW_TYPE_REVIEW },
  { label: '发布流程', value: FLOW_TYPE_PUBLISH }
]

const statusOptions = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 }
]

const rejectModeOptions = [
  { label: '退回上一步', value: 'PREVIOUS' },
  { label: '退回检测', value: 'DETECTION' },
  { label: '流程终止', value: 'TERMINATE' }
]

const query = reactive({
  pageNum: 1,
  pageSize: DEFAULT_PAGE_SIZE,
  keyword: '',
  flowType: '',
  status: ''
})

const flowForm = reactive(createDefaultFlowForm())

const flowRules = {
  flowName: [{ required: true, message: '请输入流程名称', trigger: 'blur' }],
  flowType: [{ required: true, message: '请选择流程类型', trigger: 'change' }],
  scopeName: [{ required: true, message: '请输入适用范围', trigger: 'blur' }],
  status: [{ required: true, message: '请选择启用状态', trigger: 'change' }]
}

const stats = computed(() => [
  { key: 'all', label: '流程总数', value: summaryRows.value.length, desc: '当前已维护的审核与发布流程' },
  { key: 'review', label: '审核流程', value: summaryRows.value.filter((item) => item.flowType === FLOW_TYPE_REVIEW).length, desc: '样品登录选择的审核流程' },
  { key: 'publish', label: '发布流程', value: summaryRows.value.filter((item) => item.flowType === FLOW_TYPE_PUBLISH).length, desc: '报告发布前可选择的发布流程' },
  { key: 'enabled', label: '启用中', value: summaryRows.value.filter((item) => Number(item.status) === 1).length, desc: '当前可用的流程配置' }
])

const nodeDialogTitle = computed(() => {
  const flow = getFlowById(currentNodeFlowId.value)
  return flow ? `配置节点 - ${flow.flowName}` : '配置节点'
})

onMounted(async () => {
  await Promise.all([
    loadFlowConfigs(),
    loadFlowSummary()
  ])
})

function createDefaultFlowForm() {
  return {
    id: '',
    flowName: '',
    flowType: '',
    scopeName: '全部',
    defaultFlag: false,
    status: 1,
    remark: ''
  }
}

function createNode(
  nodeName = '',
  roleName = '',
  assigneeName = '',
  required = true,
  rejectMode = 'PREVIOUS',
  roleCode = '',
  assigneeId = null
) {
  return {
    id: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
    nodeName,
    roleName,
    roleCode,
    assigneeId,
    assigneeName,
    required,
    rejectMode
  }
}

function getDefaultNodes(flowType) {
  if (flowType === FLOW_TYPE_PUBLISH) {
    return [
      createNode('报告复核', '报告员', '', true, 'PREVIOUS', 'REPORTER'),
      createNode('发布确认', '报告员', '', true, 'TERMINATE', 'REPORTER')
    ]
  }
  return [
    createNode('初审', '审核员', '', true, 'DETECTION', 'REVIEWER'),
    createNode('复审', '审核员', '', true, 'PREVIOUS', 'REVIEWER')
  ]
}

function getFlowTypeLabel(value) {
  return flowTypeOptions.find((item) => item.value === value)?.label || '-'
}

function getFlowById(id) {
  return flowRows.value.find((item) => String(item.id) === String(id))
}

function getRoleUsers(roleCode) {
  return roleUserMap[roleCode] || []
}

function resolveRoleByCodeOrName(roleCode, roleName) {
  return roleOptions.value.find((item) => item.roleCode === roleCode)
    || roleOptions.value.find((item) => item.roleName === roleName)
}

function resolveUserById(roleCode, assigneeId) {
  return getRoleUsers(roleCode).find((item) => String(item.id) === String(assigneeId))
}

function normalizeFlow(row) {
  return {
    ...row,
    defaultFlag: Boolean(row.defaultFlag),
    nodes: Array.isArray(row.nodes) ? row.nodes.map(normalizeNode) : []
  }
}

function normalizeNode(node) {
  const role = resolveRoleByCodeOrName(node?.roleCode, node?.roleName)
  return {
    ...node,
    roleCode: node?.roleCode || role?.roleCode || '',
    roleName: node?.roleName || role?.roleName || '',
    assigneeId: node?.assigneeId || null,
    assigneeName: node?.assigneeName || ''
  }
}

function buildQueryParams(override = {}) {
  return {
    pageNum: query.pageNum,
    pageSize: query.pageSize,
    keyword: String(query.keyword || '').trim() || undefined,
    flowType: query.flowType || undefined,
    status: query.status === '' ? undefined : query.status,
    ...override
  }
}

async function loadFlowConfigs() {
  loading.value = true
  try {
    const result = await fetchFlowConfigsApi(buildQueryParams())
    flowRows.value = (Array.isArray(result.records) ? result.records : []).map(normalizeFlow)
    flowTotal.value = Number(result.total || 0)
  } finally {
    loading.value = false
  }
}

async function loadFlowSummary() {
  const result = await fetchFlowConfigsApi({ pageNum: 1, pageSize: 500 })
  summaryRows.value = (Array.isArray(result.records) ? result.records : []).map(normalizeFlow)
}

async function loadRoleOptions() {
  if (roleOptions.value.length) {
    return
  }
  roleOptions.value = await fetchSystemRoleOptionsApi()
}

async function loadUsersByRole(roleCode) {
  if (!roleCode || roleUserMap[roleCode]) {
    return
  }
  userLoadingMap[roleCode] = true
  try {
    const result = await fetchSystemUsersApi({ pageNum: 1, pageSize: 500, roleCode, status: 1 })
    roleUserMap[roleCode] = Array.isArray(result.records) ? result.records : []
  } finally {
    userLoadingMap[roleCode] = false
  }
}

async function refreshFlowData() {
  await Promise.all([
    loadFlowConfigs(),
    loadFlowSummary()
  ])
}

function handleStatClick(key) {
  activeStatKey.value = key
  query.pageNum = 1
  if (key === 'review') {
    query.flowType = FLOW_TYPE_REVIEW
    query.status = ''
  } else if (key === 'publish') {
    query.flowType = FLOW_TYPE_PUBLISH
    query.status = ''
  } else if (key === 'enabled') {
    query.flowType = ''
    query.status = 1
  } else {
    query.flowType = ''
    query.status = ''
  }
  loadFlowConfigs()
}

function handleSearch() {
  query.pageNum = 1
  syncActiveStatByQuery()
  loadFlowConfigs()
}

function resetQuery() {
  activeStatKey.value = 'all'
  query.pageNum = 1
  query.pageSize = DEFAULT_PAGE_SIZE
  query.keyword = ''
  query.flowType = ''
  query.status = ''
  loadFlowConfigs()
}

function syncActiveStatByQuery() {
  if (query.flowType === FLOW_TYPE_REVIEW && query.status === '') {
    activeStatKey.value = 'review'
  } else if (query.flowType === FLOW_TYPE_PUBLISH && query.status === '') {
    activeStatKey.value = 'publish'
  } else if (query.status === 1 || String(query.status) === '1') {
    activeStatKey.value = 'enabled'
  } else {
    activeStatKey.value = 'all'
  }
}

function openFlowDialog(row) {
  resetFlowForm()
  if (row) {
    Object.assign(flowForm, {
      id: row.id,
      flowName: row.flowName || '',
      flowType: row.flowType || '',
      scopeName: row.scopeName || '全部',
      defaultFlag: Boolean(row.defaultFlag),
      status: Number(row.status) === 0 ? 0 : 1,
      remark: row.remark || ''
    })
  }
  flowDialogVisible.value = true
}

function resetFlowForm() {
  Object.assign(flowForm, createDefaultFlowForm())
  flowFormRef.value?.clearValidate?.()
}

function buildFlowPayload() {
  const current = getFlowById(flowForm.id)
  return {
    flowName: String(flowForm.flowName || '').trim(),
    flowType: flowForm.flowType,
    scopeName: String(flowForm.scopeName || '').trim(),
    defaultFlag: Boolean(flowForm.defaultFlag),
    status: Number(flowForm.status) === 0 ? 0 : 1,
    remark: String(flowForm.remark || '').trim(),
    nodes: flowForm.id
      ? (current?.nodes || []).map(toNodePayload)
      : getDefaultNodes(flowForm.flowType).map(toNodePayload)
  }
}

function toNodePayload(node) {
  const role = resolveRoleByCodeOrName(node.roleCode, node.roleName)
  return {
    nodeName: String(node.nodeName || '').trim(),
    roleName: String(node.roleName || role?.roleName || '').trim(),
    roleCode: String(node.roleCode || role?.roleCode || '').trim(),
    assigneeId: node.assigneeId || null,
    assigneeName: String(node.assigneeName || '').trim(),
    required: Boolean(node.required),
    rejectMode: node.rejectMode || 'PREVIOUS'
  }
}

async function submitFlowForm() {
  await flowFormRef.value.validate()
  savingFlow.value = true
  try {
    await loadRoleOptions()
    const payload = buildFlowPayload()
    if (flowForm.id) {
      await updateFlowConfigApi(flowForm.id, payload)
      ElMessage.success('流程更新成功')
    } else {
      await createFlowConfigApi(payload)
      ElMessage.success('流程新增成功')
    }
    flowDialogVisible.value = false
    await refreshFlowData()
  } finally {
    savingFlow.value = false
  }
}

async function openNodeDialog(row) {
  await loadRoleOptions()
  currentNodeFlowId.value = row.id
  nodeDraftRows.value = (row.nodes || []).map(normalizeNode)
  await Promise.all([...new Set(nodeDraftRows.value.map((item) => item.roleCode).filter(Boolean))].map(loadUsersByRole))
  nodeDialogVisible.value = true
}

function addNodeRow() {
  nodeDraftRows.value.push(createNode(`节点${nodeDraftRows.value.length + 1}`, '', '', true, 'PREVIOUS'))
}

async function handleNodeRoleChange(row) {
  const role = resolveRoleByCodeOrName(row.roleCode, '')
  row.roleName = role?.roleName || ''
  row.assigneeId = null
  row.assigneeName = ''
  await loadUsersByRole(row.roleCode)
}

function handleNodeAssigneeChange(row) {
  const user = resolveUserById(row.roleCode, row.assigneeId)
  row.assigneeName = user ? (user.realName || user.username || '') : ''
}

function removeNode(index) {
  nodeDraftRows.value.splice(index, 1)
}

function moveNode(index, offset) {
  const targetIndex = index + offset
  if (targetIndex < 0 || targetIndex >= nodeDraftRows.value.length) {
    return
  }
  const rows = [...nodeDraftRows.value]
  const [item] = rows.splice(index, 1)
  rows.splice(targetIndex, 0, item)
  nodeDraftRows.value = rows
}

async function submitNodes() {
  if (!nodeDraftRows.value.length) {
    ElMessage.warning('请至少配置一个流程节点')
    return
  }
  const invalid = nodeDraftRows.value.some((item) => !String(item.nodeName || '').trim() || !String(item.roleCode || '').trim())
  if (invalid) {
    ElMessage.warning('请完整填写节点名称和审批角色')
    return
  }
  savingNodes.value = true
  try {
    await saveFlowConfigNodesApi(currentNodeFlowId.value, nodeDraftRows.value.map(toNodePayload))
    nodeDialogVisible.value = false
    ElMessage.success('节点配置已保存')
    await refreshFlowData()
  } finally {
    savingNodes.value = false
  }
}

async function toggleStatus(row) {
  const nextStatus = Number(row.status) === 1 ? 0 : 1
  await updateFlowConfigStatusApi(row.id, { status: nextStatus })
  ElMessage.success(`流程已${nextStatus === 1 ? '启用' : '停用'}`)
  await refreshFlowData()
}

async function setDefaultFlow(row) {
  await setDefaultFlowConfigApi(row.id)
  ElMessage.success('默认流程设置成功')
  await refreshFlowData()
}

async function removeFlow(row) {
  await ElMessageBox.confirm(`确定删除流程“${row.flowName}”吗？`, '删除确认', { type: 'warning' })
  await deleteFlowConfigApi(row.id)
  ElMessage.success('流程删除成功')
  if (flowRows.value.length === 1 && query.pageNum > 1) {
    query.pageNum -= 1
  }
  await refreshFlowData()
}
</script>

<style scoped>
.metric-card--action {
  width: 100%;
  border: none;
  cursor: pointer;
  text-align: left;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.metric-card--action:hover,
.metric-card--action:focus-visible,
.metric-card--action.is-active {
  transform: translateY(-2px);
  box-shadow: 0 18px 32px rgba(23, 67, 122, 0.12);
}

.metric-card p {
  margin: 8px 0 0;
  color: var(--text-sub);
  font-size: 13px;
  line-height: 1.7;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 4px 18px;
}

.form-span-2 {
  grid-column: span 2;
}

.node-dialog {
  display: grid;
  gap: 16px;
}

.node-dialog__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border: 1px solid var(--line-soft);
  border-radius: 16px;
  background: linear-gradient(180deg, #ffffff, #f7fbff);
}

.node-dialog__head strong {
  color: var(--text-main);
}

.node-dialog__head p {
  margin: 6px 0 0;
  color: var(--text-sub);
  line-height: 1.7;
}

.flow-config-page :deep(.el-select),
.flow-config-page :deep(.el-input) {
  width: 100%;
}

@media (max-width: 760px) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .form-span-2 {
    grid-column: span 1;
  }

  .node-dialog__head {
    flex-direction: column;
  }
}
</style>
