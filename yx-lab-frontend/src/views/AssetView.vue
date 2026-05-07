<template>
  <div class="content-grid asset-page">
    <section class="stats-grid asset-stats">
      <button
        v-for="item in currentStats"
        :key="item.label"
        type="button"
        :class="['metric-card', 'asset-metric', 'metric-card--action', { 'is-active': activeStatKey === item.key }]"
        @click="handleStatClick(item)"
      >
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <p>{{ item.desc }}</p>
      </button>
    </section>

    <section class="glass-panel section-block">
      <el-tabs v-model="active">
        <el-tab-pane label="设备台账" name="inst">
          <div class="section-head">
            <div>
              <h3 class="section-title">设备台账</h3>
              <p class="page-subtitle">
                统一维护实验室设备基础档案、状态信息和校准周期，支持模板下载与批量导入。
              </p>
            </div>
          </div>

          <div class="toolbar-panel">
            <div class="toolbar-row">
              <div class="toolbar-fields">
                <label class="toolbar-field toolbar-field--wide">
                  <span>关键字</span>
                  <el-input
                    v-model="instrumentQuery.keyword"
                    placeholder="请输入设备名称、型号、厂家或存放位置"
                    clearable
                    @keyup.enter="handleInstrumentSearch"
                  />
                </label>
                <label class="toolbar-field">
                  <span>设备状态</span>
                  <el-select v-model="instrumentQuery.instrumentStatus" placeholder="请选择设备状态" clearable>
                    <el-option
                      v-for="option in instrumentStatusOptions"
                      :key="option.value"
                      :label="option.label"
                      :value="option.value"
                    />
                  </el-select>
                </label>
              </div>

              <div class="toolbar-actions">
                <el-button type="primary" @click="handleInstrumentSearch">查询</el-button>
                <el-button @click="resetInstrumentQuery">重置</el-button>
                <el-button @click="handleDownloadTemplate" :loading="templateDownloading">下载导入模板</el-button>
                <el-button type="warning" plain @click="openImportDialog">导入设备</el-button>
                <el-button type="primary" plain @click="openInstrumentDialog()">新增设备</el-button>
              </div>
            </div>
          </div>

          <div class="table-card">
            <el-table
              class="list-table"
              :data="visibleInstruments"
              stripe
              max-height="420"
              v-loading="instrumentLoading"
              empty-text="暂无设备台账数据"
            >
              <el-table-column prop="instrumentName" label="设备名称" min-width="180" />
              <el-table-column prop="instrumentModel" label="设备型号" min-width="140" />
              <el-table-column prop="manufacturer" label="生产厂家" min-width="180" />
              <el-table-column prop="ownerName" label="负责人" min-width="110" />
              <el-table-column prop="storageLocation" label="存放位置" min-width="140" />
              <el-table-column prop="purchaseDate" label="购置日期" width="120" />
              <el-table-column label="状态" width="110" header-cell-class-name="cell-center" class-name="cell-center">
                <template #default="{ row }">
                  <span class="status-chip" :class="getStatusClass('instrumentStatus', row.instrumentStatus)">
                    {{ getEnumLabel(instrumentStatusLabelMap, row.instrumentStatus) }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column label="备注" min-width="180" show-overflow-tooltip>
                <template #default="{ row }">
                  {{ row.remark || '-' }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="170" fixed="right" class-name="cell-center">
                <template #default="{ row }">
                  <el-button link type="primary" @click="openInstrumentDialog(row.id)">编辑</el-button>
                  <el-button link type="danger" @click="removeInstrument(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>

            <TablePagination
              v-model:current-page="instrumentQuery.pageNum"
              v-model:page-size="instrumentQuery.pageSize"
              :total="instrumentTotal"
              @change="loadInstruments"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="文档台账" name="doc">
          <div class="section-head">
            <div>
              <h3 class="section-title">文档台账</h3>
              <p class="page-subtitle">
                管理制度文件、规范文档和附件资料，支持查看权限控制、上传替换和在线预览。
              </p>
            </div>
          </div>

          <div class="toolbar-panel">
            <div class="toolbar-row">
              <div class="toolbar-fields">
                <label class="toolbar-field toolbar-field--wide">
                  <span>关键字</span>
                  <el-input
                    v-model="documentQuery.keyword"
                    placeholder="请输入文档名称"
                    clearable
                    @keyup.enter="handleDocumentSearch"
                  />
                </label>
                <label class="toolbar-field">
                  <span>文档分类</span>
                  <el-input v-model="documentQuery.documentCategory" placeholder="请输入文档分类" clearable />
                </label>
              </div>

              <div class="toolbar-actions">
                <el-button type="primary" @click="handleDocumentSearch">查询</el-button>
                <el-button @click="resetDocumentQuery">重置</el-button>
                <el-button type="primary" plain @click="openDocumentDialog()">新增文档</el-button>
              </div>
            </div>
          </div>

          <div class="table-card">
            <el-table
              class="list-table"
              :data="visibleDocuments"
              stripe
              max-height="420"
              v-loading="documentLoading"
              empty-text="暂无文档台账数据"
            >
              <el-table-column prop="documentName" label="文档名称" min-width="180" />
              <el-table-column prop="documentCategory" label="分类" min-width="120" />
              <el-table-column prop="fileType" label="文件类型" width="100" class-name="cell-center" />
              <el-table-column prop="createdName" label="上传人" min-width="120" />
              <el-table-column label="可查看人员" min-width="200" show-overflow-tooltip>
                <template #default="{ row }">
                  {{ row.viewerNames?.length ? row.viewerNames.join('、') : '仅上传人和管理员可查看' }}
                </template>
              </el-table-column>
              <el-table-column prop="createdTime" label="上传时间" width="170" />
              <el-table-column label="备注" min-width="160" show-overflow-tooltip>
                <template #default="{ row }">
                  {{ row.remark || '-' }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="220" fixed="right" class-name="cell-center">
                <template #default="{ row }">
                  <el-button link type="primary" @click="previewDocument(row)">查看</el-button>
                  <el-button v-if="row.canManage" link type="primary" @click="openDocumentDialog(row.id)">编辑</el-button>
                  <el-button v-if="row.canManage" link type="danger" @click="removeDocument(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>

            <TablePagination
              v-model:current-page="documentQuery.pageNum"
              v-model:page-size="documentQuery.pageSize"
              :total="documentTotal"
              @change="loadDocuments"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </section>

    <el-dialog
      v-model="instrumentDialogVisible"
      :title="instrumentForm.id ? '编辑设备' : '新增设备'"
      width="760px"
      destroy-on-close
      @closed="resetInstrumentForm"
    >
      <el-form ref="instrumentFormRef" :model="instrumentForm" :rules="instrumentRules" label-width="100px">
        <div class="form-grid">
          <el-form-item label="设备名称" prop="instrumentName">
            <el-input v-model="instrumentForm.instrumentName" placeholder="请输入设备名称" />
          </el-form-item>
          <el-form-item label="设备型号" prop="instrumentModel">
            <el-input v-model="instrumentForm.instrumentModel" placeholder="请输入设备型号" />
          </el-form-item>
          <el-form-item label="生产厂家" prop="manufacturer">
            <el-input v-model="instrumentForm.manufacturer" placeholder="请输入生产厂家" />
          </el-form-item>
          <el-form-item label="负责人" prop="ownerName">
            <el-input v-model="instrumentForm.ownerName" placeholder="请输入负责人" />
          </el-form-item>
          <el-form-item label="设备状态" prop="instrumentStatus">
            <el-select v-model="instrumentForm.instrumentStatus" placeholder="请选择设备状态" style="width: 100%">
              <el-option
                v-for="option in instrumentStatusOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="存放位置" prop="storageLocation">
            <el-input v-model="instrumentForm.storageLocation" placeholder="请输入存放位置" />
          </el-form-item>
          <el-form-item label="购置日期" prop="purchaseDate">
            <el-date-picker
              v-model="instrumentForm.purchaseDate"
              type="date"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              placeholder="请选择购置日期"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="使用年限" prop="serviceLifeYears">
            <el-input-number v-model="instrumentForm.serviceLifeYears" :min="0" :max="100" style="width: 100%" />
          </el-form-item>
          <el-form-item label="校准周期" prop="calibrationCycle">
            <el-input v-model="instrumentForm.calibrationCycle" placeholder="例如：12个月" />
          </el-form-item>
          <el-form-item label="证书地址" prop="certificateUrl">
            <el-input v-model="instrumentForm.certificateUrl" placeholder="请输入证书文件地址" />
          </el-form-item>
          <el-form-item class="form-span-2" label="备注" prop="remark">
            <el-input v-model="instrumentForm.remark" type="textarea" :rows="3" placeholder="可填写设备补充说明" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="instrumentDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingInstrument" @click="submitInstrument">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="importDialogVisible"
      title="导入设备台账"
      width="760px"
      destroy-on-close
      @closed="closeImportDialog"
    >
      <div class="import-guide">
        <div class="import-card">
          <strong>导入前请先下载模板</strong>
          <p>请严格按照模板列名填写，系统会校验必填项、状态值、日期格式、使用年限以及重复设备。</p>
        </div>
        <div class="import-card warm">
          <strong>校验规则</strong>
          <p>只要存在一行错误，本次导入就不会入库。你可以根据下方错误明细修正后重新导入。</p>
        </div>
      </div>

      <div class="toolbar-panel import-toolbar">
        <div class="toolbar-row">
          <div class="panel-note">支持 Excel 批量导入，仅新增设备，不覆盖已有台账。</div>
          <div class="toolbar-actions">
            <el-button @click="handleDownloadTemplate" :loading="templateDownloading">下载模板</el-button>
          </div>
        </div>
      </div>

      <el-upload
        class="import-upload"
        drag
        :auto-upload="false"
        :limit="1"
        accept=".xlsx,.xls"
        :file-list="importFileList"
        :on-change="handleImportFileChange"
        :on-remove="handleImportFileRemove"
      >
        <div class="upload-box">
          <div class="upload-title">将填写好的 Excel 文件拖到这里，或点击上传</div>
          <div class="upload-subtitle">仅支持 `.xlsx` / `.xls`，且仅导入新增设备数据。</div>
        </div>
      </el-upload>

      <el-alert
        v-if="importResult"
        :type="importResult.allPassed ? 'success' : 'error'"
        :closable="false"
        class="import-result"
        :title="importResult.allPassed
          ? `导入成功，本次新增 ${importResult.successCount} 条设备记录`
          : `校验未通过，共检测 ${importResult.totalRows} 行数据，错误 ${importResult.errors.length} 条`"
      />

      <el-table
        v-if="importResult && importResult.errors?.length"
        :data="importResult.errors"
        stripe
        max-height="260"
        empty-text="暂无校验错误"
        class="import-error-table"
      >
        <el-table-column prop="rowNum" label="行号" width="90" class-name="cell-center" />
        <el-table-column prop="deviceName" label="设备名称" min-width="160" />
        <el-table-column prop="message" label="错误原因" min-width="280" show-overflow-tooltip />
      </el-table>

      <template #footer>
        <el-button @click="closeImportDialog">取消</el-button>
        <el-button type="primary" :loading="importSubmitting" @click="submitImport">开始导入</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="documentDialogVisible"
      :title="documentForm.id ? '编辑文档' : '新增文档'"
      width="820px"
      destroy-on-close
      @closed="resetDocumentForm"
    >
      <el-form ref="documentFormRef" :model="documentForm" :rules="documentRules" label-width="110px">
        <div class="form-grid">
          <el-form-item label="文档名称" prop="documentName">
            <el-input v-model="documentForm.documentName" placeholder="请输入文档名称" />
          </el-form-item>
          <el-form-item label="文档分类" prop="documentCategory">
            <el-input v-model="documentForm.documentCategory" placeholder="请输入文档分类" />
          </el-form-item>
          <el-form-item class="form-span-2" label="查看人员">
            <el-select
              v-model="documentForm.viewerUserIds"
              multiple
              collapse-tags
              collapse-tags-tooltip
              filterable
              placeholder="请选择可查看人员"
              style="width: 100%"
            >
              <el-option
                v-for="user in documentUsers"
                :key="user.userId"
                :label="`${user.realName || user.username}（${user.username}）`"
                :value="String(user.userId)"
              />
            </el-select>
          </el-form-item>
          <el-form-item class="form-span-2" label="上传文件" prop="fileUrl">
            <el-upload
              :auto-upload="false"
              :limit="1"
              :file-list="documentFileList"
              :on-change="handleDocumentFileChange"
              :on-remove="handleDocumentFileRemove"
            >
              <template #trigger>
                <el-button type="primary" plain>选择本地文档</el-button>
              </template>
              <template #tip>
                <div class="upload-tip">
                  {{ documentForm.fileUrl ? '已存在文档文件；如重新选择文件，将覆盖原文件。' : '请选择本地文档后保存。' }}
                </div>
              </template>
            </el-upload>
          </el-form-item>
          <el-form-item class="form-span-2" label="备注" prop="remark">
            <el-input v-model="documentForm.remark" type="textarea" :rows="3" placeholder="可填写文档用途或说明" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="documentDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingDocument" @click="submitDocument">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="previewDialogVisible"
      :title="previewTitle"
      width="900px"
      destroy-on-close
      @closed="closePreviewDialog"
    >
      <div v-if="previewError" class="preview-empty">{{ previewError }}</div>
      <img v-else-if="previewMode === 'image'" :src="previewUrl" alt="文档预览" class="preview-image" />
      <iframe v-else-if="previewUrl" :src="previewUrl" class="preview-frame" />
      <div v-else class="preview-empty">暂无可预览内容</div>
      <template #footer>
        <el-button @click="closePreviewDialog">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import TablePagination from '../components/common/TablePagination.vue'
import {
  createDocumentApi,
  createInstrumentApi,
  deleteDocumentApi,
  deleteInstrumentApi,
  downloadInstrumentTemplateApi,
  fetchDocumentsApi,
  fetchDocumentUsersApi,
  fetchInstrumentsApi,
  getDocumentDetailApi,
  getInstrumentDetailApi,
  importInstrumentsApi,
  previewDocumentApi,
  updateDocumentApi,
  updateInstrumentApi,
  uploadStorageFileApi
} from '../api/lab'
import {
  DEFAULT_PAGE_SIZE,
  getEnumLabel,
  getStatusClass,
  inactiveInstrumentStatuses,
  instrumentCalibratingStatus,
  instrumentNormalStatus,
  instrumentStatusLabelMap,
  instrumentStatusOptions
} from '../utils/labEnums'

const active = ref('inst')
const instrumentLoading = ref(false)
const savingInstrument = ref(false)
const templateDownloading = ref(false)
const importSubmitting = ref(false)
const instrumentDialogVisible = ref(false)
const importDialogVisible = ref(false)
const instrumentFormRef = ref()
const instruments = ref([])
const instrumentTotal = ref(0)
const importFileList = ref([])
const selectedImportFile = ref(null)
const importResult = ref(null)

const documentLoading = ref(false)
const savingDocument = ref(false)
const documentDialogVisible = ref(false)
const documentFormRef = ref()
const documents = ref([])
const documentTotal = ref(0)
const documentUsers = ref([])
const documentFileList = ref([])
const selectedDocumentFile = ref(null)

const previewDialogVisible = ref(false)
const previewUrl = ref('')
const previewMode = ref('frame')
const previewTitle = ref('')
const previewError = ref('')
const activeStatKey = ref('设备总数')

const currentStats = computed(() => (active.value === 'inst' ? instrumentStats.value : documentStats.value))

const visibleInstruments = computed(() => {
  if (active.value !== 'inst') {
    return instruments.value
  }
  if (activeStatKey.value === '正常设备') {
    return instruments.value.filter((item) => item.instrumentStatus === instrumentNormalStatus)
  }
  if (activeStatKey.value === '停用/维护') {
    return instruments.value.filter((item) => inactiveInstrumentStatuses.includes(item.instrumentStatus))
  }
  if (activeStatKey.value === '待校准') {
    return instruments.value.filter((item) => item.instrumentStatus === instrumentCalibratingStatus)
  }
  return instruments.value
})

const visibleDocuments = computed(() => {
  if (active.value !== 'doc') {
    return documents.value
  }
  if (activeStatKey.value === '可管理文档') {
    return documents.value.filter((item) => item.canManage)
  }
  if (activeStatKey.value === '共享文档') {
    return documents.value.filter((item) => (item.viewerNames || []).length > 0)
  }
  return documents.value
})

const instrumentStats = computed(() => [
  {
    label: '设备总数',
    value: instrumentTotal.value || 0,
    desc: '设备台账内已登记设备数量'
  },
  {
    label: '正常设备',
    value: countInstrumentByStatus(instrumentNormalStatus),
    desc: '当前状态为正常的设备'
  },
  {
    label: '停用/维护',
    value: countInstrumentByStatuses(inactiveInstrumentStatuses),
    desc: '处于停用或维护状态的设备'
  },
  {
    label: '待校准',
    value: countInstrumentByStatus(instrumentCalibratingStatus),
    desc: '待校准或需要重点关注的设备'
  }
])

const documentStats = computed(() => [
  {
    label: '文档总数',
    value: documentTotal.value || 0,
    desc: '当前文档台账收录的文件数量'
  },
  {
    label: '可管理文档',
    value: documents.value.filter((item) => item.canManage).length,
    desc: '当前账号可编辑或删除的文档'
  },
  {
    label: '共享文档',
    value: documents.value.filter((item) => (item.viewerNames || []).length > 0).length,
    desc: '已配置查看人员的文档'
  },
  {
    label: '本页记录',
    value: documents.value.length,
    desc: '当前分页加载的文档条数'
  }
])

const instrumentQuery = reactive({
  pageNum: 1,
  pageSize: DEFAULT_PAGE_SIZE,
  keyword: '',
  instrumentStatus: ''
})

const documentQuery = reactive({
  pageNum: 1,
  pageSize: DEFAULT_PAGE_SIZE,
  keyword: '',
  documentCategory: ''
})

const emptyInstrumentForm = () => ({
  id: null,
  instrumentName: '',
  instrumentModel: '',
  manufacturer: '',
  purchaseDate: '',
  serviceLifeYears: null,
  calibrationCycle: '',
  ownerName: '',
  instrumentStatus: instrumentNormalStatus,
  storageLocation: '',
  certificateUrl: '',
  remark: ''
})

const emptyDocumentForm = () => ({
  id: null,
  documentName: '',
  documentCategory: '',
  fileType: '',
  fileSize: null,
  fileUrl: '',
  remark: '',
  viewerUserIds: []
})

const instrumentForm = reactive(emptyInstrumentForm())
const documentForm = reactive(emptyDocumentForm())

const instrumentRules = {
  instrumentName: [{ required: true, message: '请输入设备名称', trigger: 'blur' }],
  instrumentStatus: [{ required: true, message: '请选择设备状态', trigger: 'change' }]
}

const documentRules = {
  documentName: [{ required: true, message: '请输入文档名称', trigger: 'blur' }]
}

function resetInstrumentForm() {
  Object.assign(instrumentForm, emptyInstrumentForm())
}

function resetDocumentForm() {
  Object.assign(documentForm, emptyDocumentForm())
  documentFileList.value = []
  selectedDocumentFile.value = null
}

function resetInstrumentQuery() {
  instrumentQuery.pageNum = 1
  instrumentQuery.pageSize = DEFAULT_PAGE_SIZE
  instrumentQuery.keyword = ''
  instrumentQuery.instrumentStatus = ''
  activeStatKey.value = '设备总数'
  loadInstruments()
}

function resetDocumentQuery() {
  documentQuery.pageNum = 1
  documentQuery.pageSize = DEFAULT_PAGE_SIZE
  documentQuery.keyword = ''
  documentQuery.documentCategory = ''
  activeStatKey.value = '文档总数'
  loadDocuments()
}

function handleInstrumentSearch() {
  instrumentQuery.pageNum = 1
  activeStatKey.value = '设备总数'
  loadInstruments()
}

function handleDocumentSearch() {
  documentQuery.pageNum = 1
  activeStatKey.value = '文档总数'
  loadDocuments()
}

function handleStatClick(item) {
  if (!item?.label) {
    return
  }

  if (['设备总数', '正常设备', '停用/维护', '待校准'].includes(item.label)) {
    active.value = 'inst'
    activeStatKey.value = activeStatKey.value === item.label ? '设备总数' : item.label
    return
  }

  active.value = 'doc'
  activeStatKey.value = activeStatKey.value === item.label ? '文档总数' : item.label
}

watch(active, (value) => {
  if (value === 'inst' && !['设备总数', '正常设备', '停用/维护', '待校准'].includes(activeStatKey.value)) {
    activeStatKey.value = '设备总数'
  }
  if (value === 'doc' && !['文档总数', '可管理文档', '共享文档', '本页记录'].includes(activeStatKey.value)) {
    activeStatKey.value = '文档总数'
  }
})

function openImportDialog() {
  importDialogVisible.value = true
}

function closeImportDialog() {
  importDialogVisible.value = false
  importFileList.value = []
  selectedImportFile.value = null
  importResult.value = null
}

function handleImportFileChange(file, files) {
  importFileList.value = files.slice(-1)
  selectedImportFile.value = file.raw || null
  importResult.value = null
}

function handleImportFileRemove() {
  importFileList.value = []
  selectedImportFile.value = null
  importResult.value = null
}

function handleDocumentFileChange(file, files) {
  documentFileList.value = files.slice(-1)
  selectedDocumentFile.value = file.raw || null
}

function handleDocumentFileRemove() {
  documentFileList.value = []
  selectedDocumentFile.value = null
}

function revokePreviewUrl() {
  if (previewUrl.value) {
    window.URL.revokeObjectURL(previewUrl.value)
    previewUrl.value = ''
  }
}

function closePreviewDialog() {
  revokePreviewUrl()
  previewDialogVisible.value = false
  previewUrl.value = ''
  previewMode.value = 'frame'
  previewError.value = ''
  previewTitle.value = ''
}

function countInstrumentByStatus(status) {
  return instruments.value.filter((item) => item.instrumentStatus === status).length
}

function countInstrumentByStatuses(statuses) {
  return instruments.value.filter((item) => statuses.includes(item.instrumentStatus)).length
}

async function handleDownloadTemplate() {
  templateDownloading.value = true
  try {
    const response = await downloadInstrumentTemplateApi()
    const blob = new Blob([response.data], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = '设备台账导入模板.xlsx'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  } finally {
    templateDownloading.value = false
  }
}

async function submitImport() {
  if (!selectedImportFile.value) {
    ElMessage.warning('请先选择导入文件。')
    return
  }
  importSubmitting.value = true
  try {
    const result = await importInstrumentsApi(selectedImportFile.value)
    importResult.value = result
    if (result.allPassed) {
      ElMessage.success(`导入成功，新增 ${result.successCount} 条设备记录。`)
      closeImportDialog()
      await loadInstruments()
    } else {
      ElMessage.warning(`导入校验未通过，请处理 ${result.errors.length} 条错误后重试。`)
    }
  } finally {
    importSubmitting.value = false
  }
}

async function loadInstruments() {
  instrumentLoading.value = true
  try {
    const result = await fetchInstrumentsApi(instrumentQuery)
    instruments.value = result.records || []
    instrumentTotal.value = result.total || 0
  } finally {
    instrumentLoading.value = false
  }
}

async function loadDocuments() {
  documentLoading.value = true
  try {
    const result = await fetchDocumentsApi(documentQuery)
    documents.value = result.records || []
    documentTotal.value = result.total || 0
  } finally {
    documentLoading.value = false
  }
}

async function loadDocumentUsers() {
  documentUsers.value = await fetchDocumentUsersApi()
}

async function openInstrumentDialog(id) {
  resetInstrumentForm()
  instrumentDialogVisible.value = true
  if (id) {
    const detail = await getInstrumentDetailApi(id)
    Object.assign(instrumentForm, detail)
  }
}

async function openDocumentDialog(id) {
  resetDocumentForm()
  documentDialogVisible.value = true
  if (id) {
    const detail = await getDocumentDetailApi(id)
    Object.assign(documentForm, {
      id: detail.id,
      documentName: detail.documentName,
      documentCategory: detail.documentCategory,
      fileType: detail.fileType,
      fileSize: detail.fileSize,
      fileUrl: detail.fileUrl,
      remark: detail.remark,
      viewerUserIds: (detail.viewerUserIds || []).map((item) => String(item))
    })
  }
}

async function submitInstrument() {
  await instrumentFormRef.value.validate()
  savingInstrument.value = true
  try {
    const payload = { ...instrumentForm }
    if (!payload.purchaseDate) {
      payload.purchaseDate = null
    }
    if (payload.id) {
      await updateInstrumentApi(payload.id, payload)
      ElMessage.success('设备信息已更新。')
    } else {
      await createInstrumentApi(payload)
      ElMessage.success('设备已新增。')
    }
    instrumentDialogVisible.value = false
    await loadInstruments()
  } finally {
    savingInstrument.value = false
  }
}

async function submitDocument() {
  await documentFormRef.value.validate()
  if (!documentForm.id && !selectedDocumentFile.value) {
    ElMessage.warning('请先选择本地文档文件。')
    return
  }
  savingDocument.value = true
  try {
    const payload = { ...documentForm, viewerUserIds: [...documentForm.viewerUserIds] }
    if (selectedDocumentFile.value) {
      const uploadResult = await uploadStorageFileApi(selectedDocumentFile.value)
      payload.fileUrl = uploadResult.filePath
      payload.fileSize = selectedDocumentFile.value.size
      payload.fileType = getFileExtension(selectedDocumentFile.value.name)
    }
    if (payload.id) {
      await updateDocumentApi(payload.id, payload)
      ElMessage.success('文档信息已更新。')
    } else {
      await createDocumentApi(payload)
      ElMessage.success('文档已新增。')
    }
    documentDialogVisible.value = false
    await loadDocuments()
  } finally {
    savingDocument.value = false
  }
}

async function previewDocument(row) {
  revokePreviewUrl()
  previewTitle.value = row.documentName
  previewError.value = ''
  previewMode.value = 'frame'
  const fileType = normalizeFileType(row.fileType, row.documentName)
  if (!isInlinePreviewable(fileType)) {
    previewError.value = fileType
      ? `当前格式（${fileType}）暂不支持浏览器在线预览，建议上传 PDF 或图片文件。`
      : '当前文件暂不支持浏览器在线预览，建议上传 PDF 或图片文件。'
    previewDialogVisible.value = true
    return
  }
  try {
    const response = await previewDocumentApi(row.id)
    const contentType = response.headers['content-type'] || 'application/octet-stream'
    if (contentType.includes('application/json')) {
      const message = await parsePreviewError(response.data)
      previewError.value = message || '文档预览失败。'
      previewDialogVisible.value = true
      return
    }
    const blob = new Blob([response.data], { type: contentType })
    previewUrl.value = window.URL.createObjectURL(blob)
    previewMode.value = contentType.startsWith('image/') ? 'image' : 'frame'
    previewDialogVisible.value = true
  } catch (error) {
    previewError.value = error?.message || '文档预览失败。'
    previewDialogVisible.value = true
  }
}

async function removeInstrument(row) {
  try {
    await ElMessageBox.confirm(`确认删除设备“${row.instrumentName}”吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await deleteInstrumentApi(row.id)
    ElMessage.success('设备已删除。')
    if (instruments.value.length === 1 && instrumentQuery.pageNum > 1) {
      instrumentQuery.pageNum -= 1
    }
    await loadInstruments()
  } catch {
    // 用户取消删除时不做处理。
  }
}

async function removeDocument(row) {
  try {
    await ElMessageBox.confirm(`确认删除文档“${row.documentName}”吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await deleteDocumentApi(row.id)
    ElMessage.success('文档已删除。')
    if (documents.value.length === 1 && documentQuery.pageNum > 1) {
      documentQuery.pageNum -= 1
    }
    await loadDocuments()
  } catch {
    // 用户取消删除时不做处理。
  }
}

function getFileExtension(fileName) {
  const index = fileName.lastIndexOf('.')
  return index >= 0 ? fileName.slice(index + 1).toLowerCase() : ''
}

function normalizeFileType(fileType, fileName) {
  if (fileType) {
    return String(fileType).trim().toLowerCase()
  }
  return getFileExtension(fileName || '')
}

function isInlinePreviewable(fileType) {
  return ['pdf', 'png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp', 'txt', 'html', 'htm'].includes(fileType)
}

async function parsePreviewError(blob) {
  try {
    const text = await blob.text()
    const payload = JSON.parse(text)
    return payload.message || ''
  } catch {
    return ''
  }
}

onMounted(() => {
  loadInstruments()
  loadDocuments()
  loadDocumentUsers()
})

onBeforeUnmount(revokePreviewUrl)
</script>

<style scoped>
.asset-page {
  gap: 16px;
}

.asset-stats {
  margin-bottom: 0;
}

.asset-metric p {
  margin: 10px 0 0;
  color: var(--text-sub);
  font-size: 14px;
  line-height: 1.6;
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

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 4px 16px;
}

.form-span-2 {
  grid-column: 1 / -1;
}

.import-guide {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}

.import-card {
  padding: 14px 16px;
  border-radius: 14px;
  background: rgba(15, 109, 122, 0.08);
  border: 1px solid rgba(15, 109, 122, 0.12);
}

.import-card.warm {
  background: rgba(240, 139, 87, 0.1);
  border-color: rgba(240, 139, 87, 0.18);
}

.import-card strong {
  display: block;
  margin-bottom: 6px;
}

.import-card p {
  margin: 0;
  line-height: 1.6;
  color: var(--text-sub);
}

.import-toolbar {
  margin-bottom: 14px;
}

.upload-box {
  padding: 12px;
}

.upload-title {
  font-size: 15px;
  font-weight: 600;
}

.upload-subtitle,
.upload-tip {
  margin-top: 8px;
  color: var(--text-sub);
  font-size: 13px;
}

.import-result,
.import-error-table {
  margin-top: 18px;
}

.preview-frame {
  width: 100%;
  height: 70vh;
  border: none;
  border-radius: 12px;
  background: #f5f7fa;
}

.preview-image {
  display: block;
  max-width: 100%;
  max-height: 70vh;
  margin: 0 auto;
}

.preview-empty {
  min-height: 160px;
  display: grid;
  place-items: center;
  color: var(--text-sub);
  text-align: center;
  line-height: 1.8;
}

@media (max-width: 860px) {
  .form-grid,
  .import-guide {
    grid-template-columns: 1fr;
  }

  .form-span-2 {
    grid-column: auto;
  }
}
</style>
