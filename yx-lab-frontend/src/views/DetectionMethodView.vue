<template>
  <div class="content-grid detection-method-page">
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

      <div class="toolbar-panel">
        <div class="toolbar-row">
          <div class="toolbar-fields">
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

          <div class="toolbar-actions">
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="resetQuery">重置</el-button>
            <el-button @click="reloadData">刷新</el-button>
            <el-button type="primary" plain @click="openDialog()">新增检测方法</el-button>
          </div>
        </div>
        <div class="panel-note">
          检测方法用于维护化验室采用的标准方法台账，可按名称、编码和标准编号管理，为后续检测流程留出正式引用入口。
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
import { ElTable, ElTableColumn } from 'element-plus/es/components/table/index.mjs'
import TablePagination from '../components/common/TablePagination.vue'
import {
  createDetectionMethodApi,
  deleteDetectionMethodApi,
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
const dialogVisible = ref(false)
const saving = ref(false)

const query = reactive({
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
  query.keyword = ''
  query.pageNum = 1
  loadRows()
}

async function loadRows() {
  const result = await fetchDetectionMethodsApi({ ...query })
  rows.value = result.records || []
  total.value = Number(result.total || 0)
}

async function reloadData() {
  activeStatKey.value = 'all'
  await loadRows()
}

onMounted(async () => {
  await loadRows()
})
</script>

<style scoped>
.detection-method-page {
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

.scene-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.scene-copy {
  display: grid;
  gap: 10px;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.7;
}

.quick-links {
  display: grid;
  gap: 12px;
}

.quick-link {
  border: 1px solid var(--panel-border);
  border-radius: 16px;
  padding: 14px 16px;
  background: rgba(255, 255, 255, 0.78);
  display: grid;
  gap: 6px;
  text-align: left;
  color: var(--text-primary);
  transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
}

.quick-link:hover {
  transform: translateY(-1px);
  border-color: rgba(var(--theme-primary-rgb), 0.4);
  box-shadow: 0 14px 28px rgba(var(--theme-primary-rgb), 0.12);
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
