<template>
  <div class="content-grid">
    <div class="glass-panel section-block">
      <el-tabs v-model="active">
        <el-tab-pane label="设备台账" name="inst">
          <div class="toolbar">
            <el-input
              v-model="instrumentQuery.keyword"
              placeholder="搜索设备名称、型号、厂家、存放位置"
              clearable
              style="max-width: 320px"
              @keyup.enter="handleInstrumentSearch"
            />
            <el-select
              v-model="instrumentQuery.instrumentStatus"
              placeholder="设备状态"
              clearable
              style="width: 180px"
            >
              <el-option label="正常" value="NORMAL" />
              <el-option label="停用" value="DISABLED" />
              <el-option label="维修中" value="MAINTENANCE" />
              <el-option label="待校准" value="CALIBRATING" />
            </el-select>
            <el-button type="primary" @click="handleInstrumentSearch">查询</el-button>
            <el-button @click="resetInstrumentQuery">重置</el-button>
            <el-button @click="handleDownloadTemplate" :loading="templateDownloading">下载导入模板</el-button>
            <el-button type="warning" plain @click="openImportDialog">导入设备</el-button>
            <el-button type="primary" plain @click="openInstrumentDialog()">新增设备</el-button>
          </div>

          <div class="table-card">
            <el-table :data="instruments" stripe v-loading="instrumentLoading">
              <el-table-column prop="instrumentName" label="设备名称" min-width="180" />
              <el-table-column prop="instrumentModel" label="设备型号" min-width="140" />
              <el-table-column prop="manufacturer" label="生产厂家" min-width="180" />
              <el-table-column prop="ownerName" label="负责人" min-width="110" />
              <el-table-column prop="storageLocation" label="存放位置" min-width="140" />
              <el-table-column prop="purchaseDate" label="购置日期" width="120" />
              <el-table-column prop="instrumentStatus" label="状态" width="110">
                <template #default="{ row }">
                  <span class="status-chip" :class="statusClassMap[row.instrumentStatus] || 'info'">
                    {{ statusLabelMap[row.instrumentStatus] || row.instrumentStatus || '-' }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column label="备注" min-width="180" show-overflow-tooltip>
                <template #default="{ row }">
                  {{ row.remark || '-' }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="170" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" @click="openInstrumentDialog(row.id)">编辑</el-button>
                  <el-button link type="danger" @click="removeInstrument(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>

            <div class="pager-wrap">
              <el-pagination
                v-model:current-page="instrumentQuery.pageNum"
                v-model:page-size="instrumentQuery.pageSize"
                :page-sizes="[10, 20, 50]"
                :total="instrumentTotal"
                layout="total, sizes, prev, pager, next"
                @size-change="loadInstruments"
                @current-change="loadInstruments"
              />
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="文档台账" name="doc">
          <div class="toolbar">
            <el-button type="primary" @click="loadDocuments">刷新</el-button>
            <el-button @click="createDocument">新增文档</el-button>
          </div>
          <el-table :data="documents" stripe>
            <el-table-column prop="documentName" label="文档名称" />
            <el-table-column prop="documentCategory" label="分类" />
            <el-table-column prop="fileType" label="文件格式" />
            <el-table-column prop="fileUrl" label="文件路径" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>

    <el-dialog
      v-model="instrumentDialogVisible"
      :title="instrumentForm.id ? '编辑设备' : '新增设备'"
      width="760px"
      destroy-on-close
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
            <el-select v-model="instrumentForm.instrumentStatus" placeholder="请选择状态" style="width: 100%">
              <el-option label="正常" value="NORMAL" />
              <el-option label="停用" value="DISABLED" />
              <el-option label="维修中" value="MAINTENANCE" />
              <el-option label="待校准" value="CALIBRATING" />
            </el-select>
          </el-form-item>
          <el-form-item label="存放位置" prop="storageLocation">
            <el-input v-model="instrumentForm.storageLocation" placeholder="请输入存放位置" />
          </el-form-item>
          <el-form-item label="购置日期" prop="purchaseDate">
            <el-date-picker
              v-model="instrumentForm.purchaseDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="请选择购置日期"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="使用年限" prop="serviceLifeYears">
            <el-input-number v-model="instrumentForm.serviceLifeYears" :min="0" :max="100" style="width: 100%" />
          </el-form-item>
          <el-form-item label="校准周期" prop="calibrationCycle">
            <el-input v-model="instrumentForm.calibrationCycle" placeholder="如：12个月" />
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

    <el-dialog v-model="importDialogVisible" title="导入设备台账" width="760px" destroy-on-close>
      <div class="import-guide">
        <div class="import-card">
          <strong>导入前请先下载模板</strong>
          <p>请严格按模板列名填写，系统会校验必填项、状态值、日期格式、使用年限以及重复设备。</p>
        </div>
        <div class="import-card warm">
          <strong>校验规则</strong>
          <p>只要存在一行错误，本次导入不会入库。你可以根据下方错误明细修正后重新导入。</p>
        </div>
      </div>

      <div class="toolbar import-toolbar">
        <el-button @click="handleDownloadTemplate" :loading="templateDownloading">下载模板</el-button>
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
          <div class="upload-subtitle">仅支持 `.xlsx` / `.xls`，且仅导入新增设备数据</div>
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
        class="import-error-table"
      >
        <el-table-column prop="rowNum" label="行号" width="90" />
        <el-table-column prop="deviceName" label="设备名称" min-width="160" />
        <el-table-column prop="message" label="错误原因" min-width="280" show-overflow-tooltip />
      </el-table>

      <template #footer>
        <el-button @click="closeImportDialog">取消</el-button>
        <el-button type="primary" :loading="importSubmitting" @click="submitImport">开始导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createDocumentApi,
  createInstrumentApi,
  deleteInstrumentApi,
  downloadInstrumentTemplateApi,
  fetchDocumentsApi,
  fetchInstrumentsApi,
  getInstrumentDetailApi,
  importInstrumentsApi,
  updateInstrumentApi
} from '../api/lab'

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
const documents = ref([])
const importFileList = ref([])
const selectedImportFile = ref(null)
const importResult = ref(null)

const instrumentQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  instrumentStatus: ''
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
  instrumentStatus: 'NORMAL',
  storageLocation: '',
  certificateUrl: '',
  remark: ''
})

const instrumentForm = reactive(emptyInstrumentForm())

const instrumentRules = {
  instrumentName: [{ required: true, message: '请输入设备名称', trigger: 'blur' }],
  instrumentStatus: [{ required: true, message: '请选择设备状态', trigger: 'change' }]
}

const statusLabelMap = {
  NORMAL: '正常',
  DISABLED: '停用',
  MAINTENANCE: '维修中',
  CALIBRATING: '待校准'
}

const statusClassMap = {
  NORMAL: 'success',
  DISABLED: 'danger',
  MAINTENANCE: 'warning',
  CALIBRATING: 'info'
}

function resetInstrumentForm() {
  Object.assign(instrumentForm, emptyInstrumentForm())
}

function resetInstrumentQuery() {
  instrumentQuery.pageNum = 1
  instrumentQuery.pageSize = 10
  instrumentQuery.keyword = ''
  instrumentQuery.instrumentStatus = ''
  loadInstruments()
}

function handleInstrumentSearch() {
  instrumentQuery.pageNum = 1
  loadInstruments()
}

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
    ElMessage.warning('请先选择导入文件')
    return
  }
  importSubmitting.value = true
  try {
    const result = await importInstrumentsApi(selectedImportFile.value)
    importResult.value = result
    if (result.allPassed) {
      ElMessage.success(`导入成功，新增 ${result.successCount} 条设备记录`)
      closeImportDialog()
      loadInstruments()
    } else {
      ElMessage.warning(`导入校验未通过，请处理 ${result.errors.length} 条错误后重试`)
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
  documents.value = (await fetchDocumentsApi({ pageNum: 1, pageSize: 10 })).records || []
}

async function openInstrumentDialog(id) {
  resetInstrumentForm()
  instrumentDialogVisible.value = true
  if (id) {
    const detail = await getInstrumentDetailApi(id)
    Object.assign(instrumentForm, detail)
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
      ElMessage.success('设备信息已更新')
    } else {
      await createInstrumentApi(payload)
      ElMessage.success('设备已新增')
    }
    instrumentDialogVisible.value = false
    loadInstruments()
  } finally {
    savingInstrument.value = false
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
    ElMessage.success('设备已删除')
    if (instruments.value.length === 1 && instrumentQuery.pageNum > 1) {
      instrumentQuery.pageNum -= 1
    }
    loadInstruments()
  } catch {
    // User canceled deletion.
  }
}

async function createDocument() {
  await createDocumentApi({
    documentName: 'GB5749标准摘要',
    documentCategory: '标准规范',
    fileType: 'pdf',
    fileSize: 102400,
    fileUrl: '/uploads/gb5749.pdf'
  })
  ElMessage.success('文档已新增')
  loadDocuments()
}

onMounted(() => {
  loadInstruments()
  loadDocuments()
})
</script>

<style scoped>
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 4px 16px;
}

.form-span-2 {
  grid-column: 1 / -1;
}

.pager-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
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

.upload-subtitle {
  margin-top: 8px;
  color: var(--text-sub);
  font-size: 13px;
}

.import-result {
  margin-top: 18px;
}

.import-error-table {
  margin-top: 14px;
}

@media (max-width: 860px) {
  .form-grid,
  .import-guide {
    grid-template-columns: 1fr;
  }

  .form-span-2 {
    grid-column: auto;
  }

  .pager-wrap {
    justify-content: flex-start;
  }
}
</style>
