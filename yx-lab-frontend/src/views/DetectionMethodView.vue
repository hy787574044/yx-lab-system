<template>
  <div class="content-grid detection-method-page">
    <section class="glass-panel section-block">
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
              <label class="toolbar-field toolbar-field--medium">
                <span>关键字</span>
                <el-input
                  v-model="query.keyword"
                  clearable
                  placeholder="请输入检测方法名称、编码、标准编号、检测依据或备注"
                  @keyup.enter="handleSearch"
                />
              </label>
            </div>
          </div>

          <div class="toolbar-actions">
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="resetQuery">重置</el-button>
            <el-button @click="reloadData">刷新</el-button>
            <el-button @click="handleExport">导出</el-button>
          </div>
        </div>
      </div>

      <div class="table-card">
        <el-table
          class="list-table"
          :data="visibleRows"
          stripe
          max-height="480"
          empty-text="暂无检测方法数据"
        >
          <el-table-column prop="methodName" label="检测方法名称" min-width="180" />
          <el-table-column prop="methodCode" label="方法编码" min-width="140">
            <template #default="{ row }">{{ row.methodCode || '-' }}</template>
          </el-table-column>
          <el-table-column prop="standardCode" label="标准编号" min-width="160">
            <template #default="{ row }">{{ row.standardCode || '-' }}</template>
          </el-table-column>
          <el-table-column prop="parameterName" label="已绑定参数" min-width="150">
            <template #default="{ row }">{{ row.parameterName || '未绑定' }}</template>
          </el-table-column>
          <el-table-column prop="methodBasis" label="检测依据" min-width="220" show-overflow-tooltip>
            <template #default="{ row }">{{ row.methodBasis || '-' }}</template>
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
          <el-form-item label="检测方法">
            <el-input v-model="form.methodName" placeholder="请输入检测方法名称" />
          </el-form-item>
          <el-form-item label="方法编码">
            <el-input v-model="form.methodCode" placeholder="请输入方法编码，可不填" />
          </el-form-item>
          <el-form-item label="标准编号">
            <el-input v-model="form.standardCode" placeholder="请输入标准编号，如 GB/T 5750.4" />
          </el-form-item>
          <el-form-item label="状态">
            <el-radio-group v-model="form.enabled">
              <el-radio-button :label="1">启用</el-radio-button>
              <el-radio-button :label="0">停用</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item class="form-span-2" label="检测依据">
            <el-input
              v-model="form.methodBasis"
              type="textarea"
              :rows="3"
              placeholder="请输入检测依据、方法说明或执行标准"
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
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElButton } from 'element-plus/es/components/button/index.mjs'
import { ElDialog } from 'element-plus/es/components/dialog/index.mjs'
import { ElForm, ElFormItem } from 'element-plus/es/components/form/index.mjs'
import { ElInput } from 'element-plus/es/components/input/index.mjs'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs'
import { ElRadioButton, ElRadioGroup } from 'element-plus/es/components/radio/index.mjs'
import { ElOption, ElSelect } from 'element-plus/es/components/select/index.mjs'
import { ElTable, ElTableColumn } from 'element-plus/es/components/table/index.mjs'
import TablePagination from '../components/common/TablePagination.vue'
import {
  createDetectionMethodApi,
  deleteDetectionMethodApi,
  exportDetectionMethodsApi,
  fetchDetectionParametersApi,
  fetchDetectionMethodsApi,
  updateDetectionMethodApi
} from '../api/lab'
import { DEFAULT_PAGE_SIZE } from '../utils/labEnums'

const router = useRouter()

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
const parameterOptions = ref([])
const dialogVisible = ref(false)
const saving = ref(false)

const query = reactive({
  parameterId: '',
  enabled: '',
  keyword: '',
  pageNum: 1,
  pageSize: DEFAULT_PAGE_SIZE
})

const form = reactive({
  id: null,
  methodName: '',
  methodCode: '',
  standardCode: '',
  methodBasis: '',
  applyScope: '',
  enabled: 1,
  remark: ''
})

const visibleRows = computed(() => {
  if (activeStatKey.value === 'enabled') {
    return rows.value.filter((item) => item.enabled === 1)
  }
  if (activeStatKey.value === 'disabled') {
    return rows.value.filter((item) => item.enabled === 0)
  }
  return rows.value
})

const currentTags = computed(() => [
  { label: '方法总数', value: total.value, type: 'info' },
  { label: '启用方法', value: rows.value.filter((item) => item.enabled === 1).length, type: 'success' },
  { label: '停用方法', value: rows.value.filter((item) => item.enabled === 0).length, type: 'warning' }
])

const currentStats = computed(() => [
  { key: 'all', label: '全部方法', value: total.value, desc: '检测方法全量台账' },
  { key: 'enabled', label: '启用方法', value: rows.value.filter((item) => item.enabled === 1).length, desc: '当前可正式使用的方法' },
  { key: 'disabled', label: '停用方法', value: rows.value.filter((item) => item.enabled === 0).length, desc: '已停用或暂不使用的方法' }
])

function handleStatClick(key) {
  activeStatKey.value = activeStatKey.value === key ? 'all' : key
}

function goRoute(path) {
  if (path) {
    router.push(path)
  }
}

function resetForm() {
  form.id = null
  form.methodName = ''
  form.methodCode = ''
  form.standardCode = ''
  form.methodBasis = ''
  form.applyScope = ''
  form.enabled = 1
  form.remark = ''
}

function openDialog(row) {
  resetForm()
  if (row) {
    form.id = row.id
    form.methodName = row.methodName || ''
    form.methodCode = row.methodCode || ''
    form.standardCode = row.standardCode || ''
    form.methodBasis = row.methodBasis || ''
    form.applyScope = row.applyScope || ''
    form.enabled = row.enabled ?? 1
    form.remark = row.remark || ''
  }
  dialogVisible.value = true
}

async function submitForm() {
  if (!form.methodName.trim()) {
    ElMessage.warning('请填写检测方法名称')
    return
  }

  const payload = {
    methodName: form.methodName.trim(),
    methodCode: form.methodCode.trim(),
    standardCode: form.standardCode.trim(),
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
    await loadRows()
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
  await loadRows()
}

function handleSearch() {
  query.pageNum = 1
  loadRows()
}

function resetQuery() {
  query.parameterId = ''
  query.enabled = ''
  query.keyword = ''
  query.pageNum = 1
  loadRows()
}

async function loadParameterOptions() {
  const result = await fetchDetectionParametersApi({ pageNum: 1, pageSize: 500, enabled: 1 })
  parameterOptions.value = result.records || []
}

async function loadRows() {
  const result = await fetchDetectionMethodsApi({ ...query })
  rows.value = result.records || []
  total.value = Number(result.total || 0)
}

async function handleExport() {
  try {
    await exportDetectionMethodsApi({ ...query })
    ElMessage.success('检测方法导出成功')
  } catch (error) {
    ElMessage.error(error.message || '检测方法导出失败')
  }
}

async function reloadData() {
  activeStatKey.value = 'all'
  await loadRows()
}

onMounted(async () => {
  await Promise.all([loadRows(), loadParameterOptions()])
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

@media (max-width: 1080px) {
  .page-hero,
  .scene-grid {
    grid-template-columns: 1fr;
  }

  .hero-tags {
    justify-content: flex-start;
  }
}
</style>
