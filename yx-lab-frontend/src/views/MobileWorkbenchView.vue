<template>
  <div class="mobile-shell">
    <header class="mobile-hero">
      <div class="hero-copy">
        <span class="hero-tag">移动端闭环</span>
        <h1>从采样到报告，一部手机完成流转</h1>
        <p>聚焦待办、快速提交、即时回看，面向现场人员和审核人员的轻量工作台。</p>
      </div>

      <div class="hero-user">
        <div>
          <strong>{{ currentUser.realName || currentUser.username || '未登录用户' }}</strong>
          <p>{{ currentUser.roleCode || 'LAB_USER' }}</p>
        </div>
        <div class="hero-actions">
          <el-button size="small" @click="refreshAll" :loading="refreshing">刷新</el-button>
          <el-button size="small" type="danger" plain @click="logout">退出</el-button>
        </div>
      </div>
    </header>

    <section class="mobile-stats">
      <article class="mobile-stat-card" v-for="item in stats" :key="item.label">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <p>{{ item.desc }}</p>
      </article>
    </section>

    <nav class="mobile-tabs">
      <button
        v-for="item in tabOptions"
        :key="item.value"
        type="button"
        class="mobile-tab"
        :class="{ active: activeTab === item.value }"
        @click="activeTab = item.value"
      >
        <span>{{ item.label }}</span>
        <b>{{ item.count }}</b>
      </button>
    </nav>

    <section v-if="activeTab === 'overview'" class="mobile-panel">
      <div class="overview-grid">
        <article class="overview-card" @click="activeTab = 'sampling'">
          <span>采样任务</span>
          <strong>{{ samplingTodos.length }}</strong>
          <p>包含待执行和待样品登录任务</p>
        </article>
        <article class="overview-card" @click="activeTab = 'detection'">
          <span>检测待办</span>
          <strong>{{ detectionTodos.length }}</strong>
          <p>已登录样品和退回重检样品都在这里</p>
        </article>
        <article class="overview-card" @click="activeTab = 'review'">
          <span>审核待办</span>
          <strong>{{ reviewTodos.length }}</strong>
          <p>可直接通过或驳回重检</p>
        </article>
        <article class="overview-card" @click="activeTab = 'report'">
          <span>正式报告</span>
          <strong>{{ reports.length }}</strong>
          <p>支持在线预览和推送结果回看</p>
        </article>
      </div>

      <div class="mobile-section">
        <div class="section-headline">
          <h3>最近报告</h3>
          <el-button link type="primary" @click="activeTab = 'report'">查看全部</el-button>
        </div>
        <div v-if="reports.length" class="card-stack">
          <article class="report-card" v-for="row in reports.slice(0, 3)" :key="row.id">
            <div class="card-title-row">
              <strong>{{ row.reportName }}</strong>
              <span class="status-chip" :class="getStatusClass('reportStatus', row.reportStatus)">
                {{ getEnumLabel(reportStatusLabelMap, row.reportStatus) }}
              </span>
            </div>
            <p>样品编号：{{ row.sampleNo || '-' }}</p>
            <p>发布时间：{{ row.publishedTime || row.generatedTime || '-' }}</p>
            <p>推送状态：{{ getPushStatusLabel(row.pushStatus) }}</p>
            <el-button size="small" @click="previewReport(row)">预览正式报告</el-button>
          </article>
        </div>
        <el-empty v-else description="暂无相关报告" />
      </div>
    </section>

    <section v-else-if="activeTab === 'sampling'" class="mobile-panel">
      <div class="mobile-section">
        <div class="section-headline">
          <h3>采样待办</h3>
          <span class="section-tip">已完成但未登录样品的任务也会出现在这里</span>
        </div>
        <div v-if="samplingTodos.length" class="card-stack">
          <article class="task-card" v-for="task in samplingTodos" :key="task.id">
            <div class="card-title-row">
              <strong>{{ task.pointName }}</strong>
              <span class="status-chip" :class="getStatusClass('taskStatus', task.taskStatus)">
                {{ getEnumLabel(taskStatusLabelMap, task.taskStatus) }}
              </span>
            </div>
            <p>计划时间：{{ task.samplingTime || '-' }}</p>
            <p>样品类型：{{ getEnumLabel(sampleTypeLabelMap, task.sampleType) }}</p>
            <p>检测项目：{{ task.detectionItems || '-' }}</p>
            <p v-if="task.sampleLogged">已登录样品：{{ task.sampleNo }} / {{ task.sealNo }}</p>
            <p v-else-if="task.taskStatus === completedTaskStatus" class="warn-text">采样已完成，请立即做样品登录</p>
            <div class="card-actions">
              <el-button
                v-if="task.taskStatus === pendingTaskStatus"
                size="small"
                @click="startTask(task)"
              >
                开始采样
              </el-button>
              <el-button
                v-if="task.taskStatus === pendingTaskStatus || task.taskStatus === inProgressTaskStatus"
                size="small"
                type="primary"
                @click="openCompleteDialog(task)"
              >
                完成采样
              </el-button>
              <el-button
                v-if="task.taskStatus === pendingTaskStatus || task.taskStatus === inProgressTaskStatus"
                size="small"
                type="danger"
                plain
                @click="abandonTask(task)"
              >
                废弃任务
              </el-button>
              <el-button
                v-if="task.taskStatus === completedTaskStatus && !task.sampleLogged"
                size="small"
                type="primary"
                @click="openLoginDialog(task)"
              >
                样品登录
              </el-button>
            </div>
          </article>
        </div>
        <el-empty v-else description="当前没有采样待办" />
      </div>
    </section>

    <section v-else-if="activeTab === 'detection'" class="mobile-panel">
      <div class="mobile-section">
        <div class="section-headline">
          <h3>检测待办</h3>
          <span class="section-tip">覆盖待检样品和退回重检样品</span>
        </div>
        <div v-if="detectionTodos.length" class="card-stack">
          <article class="task-card" v-for="sample in detectionTodos" :key="sample.sampleId">
            <div class="card-title-row">
              <strong>{{ sample.sampleNo }}</strong>
              <span class="status-chip" :class="getStatusClass('sampleStatus', sample.sampleStatus)">
                {{ getEnumLabel(sampleStatusLabelMap, sample.sampleStatus) }}
              </span>
            </div>
            <p>封签编号：{{ sample.sealNo || '-' }}</p>
            <p>点位名称：{{ sample.pointName || '-' }}</p>
            <p>采样人：{{ sample.samplerName || '-' }}</p>
            <p>检测项目：{{ sample.detectionItems || '-' }}</p>
            <p v-if="sample.resultSummary">当前摘要：{{ sample.resultSummary }}</p>
            <div class="card-actions">
              <el-button size="small" type="primary" @click="openDetectionDialog(sample)">提交检测</el-button>
            </div>
          </article>
        </div>
        <el-empty v-else description="当前没有检测待办" />
      </div>

      <div class="mobile-section">
        <div class="section-headline">
          <h3>我的检测历史</h3>
        </div>
        <div v-if="detectionHistory.length" class="card-stack">
          <article class="history-card" v-for="row in detectionHistory" :key="row.id">
            <div class="card-title-row">
              <strong>{{ row.sampleNo }}</strong>
              <span class="status-chip" :class="getStatusClass('detectionStatus', row.detectionStatus)">
                {{ getEnumLabel(detectionStatusLabelMap, row.detectionStatus) }}
              </span>
            </div>
            <p>封签编号：{{ row.sealNo || '-' }}</p>
            <p>检测类型：{{ row.detectionTypeName || '-' }}</p>
            <p>检测结果：{{ getEnumLabel(detectionResultLabelMap, row.detectionResult) }}</p>
            <p>提交时间：{{ row.detectionTime || '-' }}</p>
          </article>
        </div>
        <el-empty v-else description="暂无检测历史" />
      </div>
    </section>

    <section v-else-if="activeTab === 'review'" class="mobile-panel">
      <div class="mobile-section">
        <div class="section-headline">
          <h3>审核待办</h3>
        </div>
        <div v-if="reviewTodos.length" class="card-stack">
          <article class="task-card" v-for="row in reviewTodos" :key="row.id">
            <div class="card-title-row">
              <strong>{{ row.sampleNo }}</strong>
              <span class="status-chip" :class="getStatusClass('detectionResult', row.detectionResult)">
                {{ getEnumLabel(detectionResultLabelMap, row.detectionResult) }}
              </span>
            </div>
            <p>检测类型：{{ row.detectionTypeName || '-' }}</p>
            <p>检测人：{{ row.detectorName || '-' }}</p>
            <p>提交时间：{{ row.detectionTime || '-' }}</p>
            <p v-if="row.abnormalRemark">异常说明：{{ row.abnormalRemark }}</p>
            <div class="card-actions">
              <el-button size="small" type="primary" @click="openReviewDialog(row, approvedReviewResult)">审核通过</el-button>
              <el-button size="small" type="danger" plain @click="openReviewDialog(row, rejectedReviewResult)">驳回重检</el-button>
            </div>
          </article>
        </div>
        <el-empty v-else description="当前没有审核待办" />
      </div>

      <div class="mobile-section">
        <div class="section-headline">
          <h3>我的审核记录</h3>
        </div>
        <div v-if="reviewHistory.length" class="card-stack">
          <article class="history-card" v-for="row in reviewHistory" :key="row.id">
            <div class="card-title-row">
              <strong>{{ row.sampleNo }}</strong>
              <span class="status-chip" :class="getStatusClass('reviewResult', row.reviewResult)">
                {{ getEnumLabel(reviewResultLabelMap, row.reviewResult) }}
              </span>
            </div>
            <p>审核时间：{{ row.reviewTime || '-' }}</p>
            <p v-if="row.reviewRemark">审核意见：{{ row.reviewRemark }}</p>
            <p v-if="row.rejectReason">驳回原因：{{ row.rejectReason }}</p>
          </article>
        </div>
        <el-empty v-else description="暂无审核记录" />
      </div>
    </section>

    <section v-else class="mobile-panel">
      <div class="mobile-section">
        <div class="section-headline">
          <h3>我的报告</h3>
          <span class="section-tip">支持正式产物在线预览和推送留痕回看</span>
        </div>
        <div v-if="reports.length" class="card-stack">
          <article class="report-card" v-for="row in reports" :key="row.id">
            <div class="card-title-row">
              <strong>{{ row.reportName }}</strong>
              <span class="status-chip" :class="getStatusClass('reportStatus', row.reportStatus)">
                {{ getEnumLabel(reportStatusLabelMap, row.reportStatus) }}
              </span>
            </div>
            <p>样品编号：{{ row.sampleNo || '-' }}</p>
            <p>封签编号：{{ row.sealNo || '-' }}</p>
            <p>发布时间：{{ row.publishedTime || '-' }}</p>
            <p>发布人：{{ row.publishedByName || '-' }}</p>
            <p>推送状态：{{ getPushStatusLabel(row.pushStatus) }}</p>
            <p v-if="row.lastPushMessage">推送结果：{{ row.lastPushMessage }}</p>
            <div class="card-actions">
              <el-button size="small" @click="previewReport(row)">预览正式报告</el-button>
            </div>
          </article>
        </div>
        <el-empty v-else description="暂无相关报告" />
      </div>
    </section>

    <el-dialog
      v-model="completeDialogVisible"
      title="完成采样"
      class="mobile-dialog"
      width="560px"
      destroy-on-close
      @closed="resetCompleteForm"
    >
      <el-form label-position="top">
        <el-form-item label="现场指标">
          <el-input v-model="completeForm.onsiteMetrics" type="textarea" :rows="3" placeholder="例如余氯、浊度、温度等现场指标" />
        </el-form-item>
        <el-form-item label="照片地址">
          <el-input v-model="completeForm.photoUrls" placeholder="多张图片可用英文逗号分隔" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="completeForm.remark" type="textarea" :rows="3" placeholder="补充说明现场情况" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="completeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitComplete">确认完成</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="loginDialogVisible"
      title="样品登录"
      class="mobile-dialog"
      width="560px"
      destroy-on-close
      @closed="resetLoginForm"
    >
      <el-form label-position="top">
        <el-form-item label="点位名称">
          <el-input v-model="loginForm.pointName" />
        </el-form-item>
        <el-form-item label="样品类型">
          <el-select v-model="loginForm.sampleType" style="width: 100%">
            <el-option v-for="option in sampleTypeOptions" :key="option.value" :label="option.label" :value="option.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="检测项目">
          <el-input v-model="loginForm.detectionItems" />
        </el-form-item>
        <el-form-item label="采样时间">
          <el-date-picker
            v-model="loginForm.samplingTime"
            type="datetime"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="天气">
          <el-input v-model="loginForm.weather" />
        </el-form-item>
        <el-form-item label="保存条件">
          <el-input v-model="loginForm.storageCondition" placeholder="例如冷藏避光、常温送检" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="loginForm.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="loginDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitSampleLogin">确认登录</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="detectionDialogVisible"
      title="提交检测"
      class="mobile-dialog"
      width="560px"
      destroy-on-close
      @closed="resetDetectionForm"
    >
      <el-form label-position="top">
        <el-form-item label="检测类型">
          <el-select v-model="detectionForm.detectionTypeId" style="width: 100%" @change="handleDetectionTypeChange">
            <el-option v-for="item in enabledDetectionTypes" :key="item.id" :label="item.typeName" :value="item.id" />
          </el-select>
        </el-form-item>
        <div v-if="detectionForm.items.length" class="parameter-list">
          <div v-for="item in detectionForm.items" :key="item.parameterId" class="parameter-card">
            <div class="parameter-title">
              <strong>{{ item.parameterName }}</strong>
              <span>{{ formatStandardRange(item.standardMin, item.standardMax, item.unit) }}</span>
            </div>
            <el-input-number v-model="item.resultValue" :precision="2" :step="0.1" style="width: 100%" />
          </div>
        </div>
        <el-form-item label="异常说明">
          <el-input v-model="detectionForm.abnormalRemark" type="textarea" :rows="3" placeholder="检测结果异常时建议填写" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="detectionDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitDetection">确认提交</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="reviewDialogVisible"
      :title="reviewForm.reviewResult === rejectedReviewResult ? '驳回重检' : '审核通过'"
      class="mobile-dialog"
      width="560px"
      destroy-on-close
      @closed="resetReviewForm"
    >
      <el-form label-position="top">
        <el-form-item label="审核意见">
          <el-input v-model="reviewForm.reviewRemark" type="textarea" :rows="3" placeholder="填写审核意见" />
        </el-form-item>
        <el-form-item v-if="reviewForm.reviewResult === rejectedReviewResult" label="驳回原因">
          <el-input v-model="reviewForm.rejectReason" type="textarea" :rows="3" placeholder="请填写退回重检原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitReview">确认提交</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="previewDialogVisible"
      :title="previewTitle"
      class="mobile-dialog mobile-preview-dialog"
      width="980px"
      destroy-on-close
      @closed="closePreviewDialog"
    >
      <div v-if="previewError" class="preview-empty">{{ previewError }}</div>
      <iframe v-else-if="previewUrl" :src="previewUrl" class="preview-frame" />
      <div v-else class="preview-empty">暂无可预览内容</div>
      <template #footer>
        <el-button @click="closePreviewDialog">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import {
  abandonSamplingTaskApi,
  completeSamplingTaskApi,
  fetchDetectionParametersApi,
  fetchDetectionTypesApi,
  fetchMobileDetectionHistoryApi,
  fetchMobileDetectionTodoApi,
  fetchMobileReportsApi,
  fetchMobileReviewHistoryApi,
  fetchMobileReviewTodoApi,
  fetchMobileSamplingTodoApi,
  getMeApi,
  loginSampleApi,
  previewReportApi,
  startSamplingTaskApi,
  submitDetectionApi,
  submitReviewApi
} from '../api/lab'
import { clearToken, getUser, setUser } from '../utils/auth'
import {
  approvedReviewResult,
  completedTaskStatus,
  detectionResultLabelMap,
  detectionStatusLabelMap,
  getEnumLabel,
  getStatusClass,
  inProgressTaskStatus,
  pendingTaskStatus,
  rejectedReviewResult,
  reportStatusLabelMap,
  reviewResultLabelMap,
  sampleStatusLabelMap,
  sampleTypeLabelMap,
  sampleTypeOptions,
  taskStatusLabelMap
} from '../utils/labEnums'

// 移动端工作台负责把采样、样品登录、检测、审核、报告预览串成一个闭环页面。
const router = useRouter()

const activeTab = ref('overview')
const refreshing = ref(false)
const submitting = ref(false)

const currentUser = ref(getUser())

// 各业务阶段的数据源按标签页拆分，刷新时统一并行拉取。
const samplingTodos = ref([])
const detectionTodos = ref([])
const detectionHistory = ref([])
const reviewTodos = ref([])
const reviewHistory = ref([])
const reports = ref([])

const detectionTypes = ref([])
const detectionParameters = ref([])

const completeDialogVisible = ref(false)
const loginDialogVisible = ref(false)
const detectionDialogVisible = ref(false)
const reviewDialogVisible = ref(false)
const previewDialogVisible = ref(false)

const previewUrl = ref('')
const previewTitle = ref('')
const previewError = ref('')

// 采样完成弹窗表单
const completeForm = reactive({
  taskId: null,
  onsiteMetrics: '',
  photoUrls: '',
  remark: ''
})

// 样品登录表单，补齐点位、时间、样品类型和留样信息。
const loginForm = reactive({
  taskId: null,
  pointId: null,
  pointName: '',
  sampleType: '',
  detectionItems: '',
  samplingTime: '',
  samplerId: null,
  samplerName: '',
  weather: '',
  storageCondition: '',
  remark: ''
})

// 检测提交表单，items 会根据检测类型自动带出参数清单。
const detectionForm = reactive({
  sampleId: null,
  detectionTypeId: null,
  detectionTypeName: '',
  abnormalRemark: '',
  items: []
})

const reviewForm = reactive({
  detectionRecordId: null,
  reviewResult: approvedReviewResult,
  rejectReason: '',
  reviewRemark: ''
})

// 只允许启用状态的检测类型进入移动端闭环。
const enabledDetectionTypes = computed(() => detectionTypes.value.filter((item) => item.enabled === 1))

const stats = computed(() => [
  { label: '采样待办', value: samplingTodos.value.length, desc: '待执行和待样品登录任务' },
  { label: '检测待办', value: detectionTodos.value.length, desc: '待检样品和退回重检样品' },
  { label: '审核待办', value: reviewTodos.value.length, desc: '待审核检测记录' },
  { label: '我的报告', value: reports.value.length, desc: '正式报告与推送留痕' }
])

const tabOptions = computed(() => [
  { value: 'overview', label: '总览', count: samplingTodos.value.length + detectionTodos.value.length + reviewTodos.value.length },
  { value: 'sampling', label: '采样', count: samplingTodos.value.length },
  { value: 'detection', label: '检测', count: detectionTodos.value.length },
  { value: 'review', label: '审核', count: reviewTodos.value.length },
  { value: 'report', label: '报告', count: reports.value.length }
])

/**
 * 将报告推送状态码转换为前端可展示的中文文案。
 *
 * @param {string} status 推送状态编码。
 * @returns {string} 推送状态中文说明。
 */
function getPushStatusLabel(status) {
  if (status === 'SUCCESS') {
    return '已推送'
  }
  if (status === 'PENDING') {
    return '待推送'
  }
  if (status === 'CANCELLED') {
    return '已撤回'
  }
  return status || '-'
}

/**
 * 重新获取当前登录人信息，并同步更新本地缓存与页面状态。
 *
 * @returns {Promise<void>} 用户信息刷新完成。
 */
async function refreshCurrentUser() {
  const user = await getMeApi()
  setUser(user)
  currentUser.value = user
}

/**
 * 并行刷新移动端工作台所需的全部业务数据。
 *
 * @returns {Promise<void>} 采样、检测、审核和报告数据刷新完成。
 */
async function refreshAll() {
  refreshing.value = true
  try {
    if (!currentUser.value.userId) {
      await refreshCurrentUser()
    }
    const [
      samplingData,
      detectionTodoData,
      detectionHistoryData,
      reviewTodoData,
      reviewHistoryData,
      reportData
    ] = await Promise.all([
      fetchMobileSamplingTodoApi(),
      fetchMobileDetectionTodoApi(),
      fetchMobileDetectionHistoryApi(),
      fetchMobileReviewTodoApi(),
      fetchMobileReviewHistoryApi(),
      fetchMobileReportsApi()
    ])
    samplingTodos.value = samplingData || []
    detectionTodos.value = detectionTodoData || []
    detectionHistory.value = detectionHistoryData || []
    reviewTodos.value = reviewTodoData || []
    reviewHistory.value = reviewHistoryData || []
    reports.value = reportData || []
  } finally {
    refreshing.value = false
  }
}

/**
 * 将指定采样任务推进到“执行中”状态。
 *
 * @param {Object} task 当前选中的采样任务。
 * @returns {Promise<void>} 任务开始后刷新工作台数据。
 */
async function startTask(task) {
  await startSamplingTaskApi(task.id, { remark: '移动端开始采样' })
  ElMessage.success('采样任务已开始')
  await refreshAll()
}

/**
 * 废弃采样任务，并要求填写废弃原因用于业务追溯。
 *
 * @param {Object} task 当前选中的采样任务。
 * @returns {Promise<void>} 废弃完成后刷新工作台数据。
 */
async function abandonTask(task) {
  try {
    const { value } = await ElMessageBox.prompt('请填写废弃原因', '废弃采样任务', {
      confirmButtonText: '确认废弃',
      cancelButtonText: '取消',
      inputPlaceholder: '例如：现场条件不满足，需要改日执行'
    })
    await abandonSamplingTaskApi(task.id, {
      reason: value,
      remark: '移动端废弃采样任务'
    })
    ElMessage.success('采样任务已废弃')
    await refreshAll()
  } catch {
    // 用户取消废弃操作时不做处理。
  }
}

/**
 * 打开采样完成弹窗，并将当前任务信息写入表单上下文。
 *
 * @param {Object} task 当前选中的采样任务。
 * @returns {void} 无返回值。
 */
function openCompleteDialog(task) {
  completeForm.taskId = task.id
  completeForm.onsiteMetrics = ''
  completeForm.photoUrls = ''
  completeForm.remark = task.remark || ''
  completeDialogVisible.value = true
}

/**
 * 重置采样完成表单，避免不同任务间数据串用。
 *
 * @returns {void} 无返回值。
 */
function resetCompleteForm() {
  completeForm.taskId = null
  completeForm.onsiteMetrics = ''
  completeForm.photoUrls = ''
  completeForm.remark = ''
}

/**
 * 提交采样完成结果，并推动流程进入样品登录阶段。
 *
 * @returns {Promise<void>} 提交完成后刷新工作台数据。
 */
async function submitComplete() {
  if (!completeForm.taskId) {
    ElMessage.warning('请选择要完成的采样任务')
    return
  }
  submitting.value = true
  try {
    await completeSamplingTaskApi({
      taskId: completeForm.taskId,
      onsiteMetrics: completeForm.onsiteMetrics,
      photoUrls: completeForm.photoUrls,
      remark: completeForm.remark
    })
    completeDialogVisible.value = false
    ElMessage.success('采样任务已完成')
    await refreshAll()
  } finally {
    submitting.value = false
  }
}

/**
 * 打开样品登录弹窗，并根据采样任务预填核心字段。
 *
 * @param {Object} task 当前选中的采样任务。
 * @returns {void} 无返回值。
 */
function openLoginDialog(task) {
  loginForm.taskId = task.id
  loginForm.pointId = task.pointId
  loginForm.pointName = task.pointName || ''
  loginForm.sampleType = task.sampleType || ''
  loginForm.detectionItems = task.detectionItems || ''
  loginForm.samplingTime = task.samplingTime || ''
  loginForm.samplerId = task.samplerId || currentUser.value.userId || null
  loginForm.samplerName = task.samplerName || currentUser.value.realName || currentUser.value.username || ''
  loginForm.weather = ''
  loginForm.storageCondition = ''
  loginForm.remark = task.remark || ''
  loginDialogVisible.value = true
}

/**
 * 重置样品登录表单。
 *
 * @returns {void} 无返回值。
 */
function resetLoginForm() {
  loginForm.taskId = null
  loginForm.pointId = null
  loginForm.pointName = ''
  loginForm.sampleType = ''
  loginForm.detectionItems = ''
  loginForm.samplingTime = ''
  loginForm.samplerId = null
  loginForm.samplerName = ''
  loginForm.weather = ''
  loginForm.storageCondition = ''
  loginForm.remark = ''
}

/**
 * 提交样品登录信息，生成实验室样品并切换到检测标签页。
 *
 * @returns {Promise<void>} 登录完成后刷新工作台数据。
 */
async function submitSampleLogin() {
  if (!loginForm.pointId || !loginForm.pointName || !loginForm.sampleType || !loginForm.detectionItems || !loginForm.samplingTime) {
    ElMessage.warning('请完整填写样品登录信息')
    return
  }
  submitting.value = true
  try {
    const sample = await loginSampleApi({ ...loginForm })
    loginDialogVisible.value = false
    ElMessage.success(`样品登录完成，封签号：${sample.sealNo}`)
    await refreshAll()
    activeTab.value = 'detection'
  } finally {
    submitting.value = false
  }
}

/**
 * 加载检测类型与检测参数配置，并在已存在缓存时直接复用。
 *
 * @returns {Promise<void>} 检测配置加载完成。
 */
async function ensureDetectionConfig() {
  if (detectionTypes.value.length && detectionParameters.value.length) {
    return
  }
  const [typeResult, parameterResult] = await Promise.all([
    fetchDetectionTypesApi({ pageNum: 1, pageSize: 200 }),
    fetchDetectionParametersApi({ pageNum: 1, pageSize: 500 })
  ])
  detectionTypes.value = typeResult.records || []
  detectionParameters.value = parameterResult.records || []
}

/**
 * 打开检测提交弹窗，并根据样品信息初始化检测表单。
 *
 * @param {Object} sample 当前选中的样品待检记录。
 * @returns {Promise<void>} 弹窗初始化完成。
 */
async function openDetectionDialog(sample) {
  await ensureDetectionConfig()
  if (!enabledDetectionTypes.value.length) {
    ElMessage.warning('当前没有可用的检测类型配置')
    return
  }
  detectionForm.sampleId = sample.sampleId
  detectionForm.abnormalRemark = ''
  detectionForm.detectionTypeId = enabledDetectionTypes.value[0].id
  handleDetectionTypeChange(detectionForm.detectionTypeId)
  if (!detectionForm.items.length) {
    ElMessage.warning('当前检测类型没有可用的参数配置，请先检查检测配置')
    return
  }
  detectionDialogVisible.value = true
}

/**
 * 根据检测类型生成检测参数录入项。
 *
 * @param {number} typeId 检测类型主键。
 * @returns {void} 无返回值。
 */
function handleDetectionTypeChange(typeId) {
  const type = enabledDetectionTypes.value.find((item) => item.id === typeId)
  detectionForm.detectionTypeId = typeId
  detectionForm.detectionTypeName = type ? type.typeName : ''
  detectionForm.items = []
  if (!type) {
    return
  }
  const parameterIds = String(type.parameterIds || '')
    .split(',')
    .map((item) => Number(item.trim()))
    .filter((item) => Number.isFinite(item))
  const parameterMap = new Map(detectionParameters.value.map((item) => [item.id, item]))
  detectionForm.items = parameterIds
    .map((id) => parameterMap.get(id))
    .filter((item) => item && item.enabled === 1)
    .map((item) => ({
      parameterId: item.id,
      parameterName: item.parameterName,
      standardMin: item.standardMin,
      standardMax: item.standardMax,
      resultValue: null,
      unit: item.unit
    }))
}

/**
 * 重置检测提交表单。
 *
 * @returns {void} 无返回值。
 */
function resetDetectionForm() {
  detectionForm.sampleId = null
  detectionForm.detectionTypeId = null
  detectionForm.detectionTypeName = ''
  detectionForm.abnormalRemark = ''
  detectionForm.items = []
}

/**
 * 提交检测结果，并校验所有必填检测参数均已录入。
 *
 * @returns {Promise<void>} 检测提交完成后刷新工作台数据。
 */
async function submitDetection() {
  if (!detectionForm.sampleId || !detectionForm.detectionTypeId) {
    ElMessage.warning('请选择检测类型')
    return
  }
  if (!detectionForm.items.length || detectionForm.items.some((item) => item.resultValue == null)) {
    ElMessage.warning('请完整填写所有检测参数结果')
    return
  }
  submitting.value = true
  try {
    await submitDetectionApi({
      sampleId: detectionForm.sampleId,
      detectionTypeId: detectionForm.detectionTypeId,
      detectionTypeName: detectionForm.detectionTypeName,
      abnormalRemark: detectionForm.abnormalRemark,
      items: detectionForm.items.map((item) => ({
        parameterId: item.parameterId,
        parameterName: item.parameterName,
        standardMin: item.standardMin,
        standardMax: item.standardMax,
        resultValue: item.resultValue,
        unit: item.unit
      }))
    })
    detectionDialogVisible.value = false
    ElMessage.success('检测已提交')
    await refreshAll()
    activeTab.value = 'review'
  } finally {
    submitting.value = false
  }
}

/**
 * 打开审核弹窗，并初始化通过或驳回重检所需的数据。
 *
 * @param {Object} row 当前选中的检测记录。
 * @param {string} reviewResult 审核结果编码。
 * @returns {void} 无返回值。
 */
function openReviewDialog(row, reviewResult) {
  reviewForm.detectionRecordId = row.id
  reviewForm.reviewResult = reviewResult
  reviewForm.rejectReason = ''
  reviewForm.reviewRemark = reviewResult === approvedReviewResult ? '移动端审核通过' : ''
  reviewDialogVisible.value = true
}

/**
 * 重置审核表单。
 *
 * @returns {void} 无返回值。
 */
function resetReviewForm() {
  reviewForm.detectionRecordId = null
  reviewForm.reviewResult = approvedReviewResult
  reviewForm.rejectReason = ''
  reviewForm.reviewRemark = ''
}

/**
 * 提交审核结果；若驳回，则必须附带重检原因。
 *
 * @returns {Promise<void>} 审核完成后刷新工作台数据。
 */
async function submitReview() {
  if (!reviewForm.detectionRecordId) {
    ElMessage.warning('请选择待审核记录')
    return
  }
  if (reviewForm.reviewResult === rejectedReviewResult && !reviewForm.rejectReason) {
    ElMessage.warning('请填写驳回原因')
    return
  }
  submitting.value = true
  try {
    await submitReviewApi({ ...reviewForm })
    reviewDialogVisible.value = false
    ElMessage.success(reviewForm.reviewResult === rejectedReviewResult ? '已驳回并退回重检' : '审核通过')
    await refreshAll()
    activeTab.value = reviewForm.reviewResult === rejectedReviewResult ? 'detection' : 'report'
  } finally {
    submitting.value = false
  }
}

/**
 * 预览正式报告产物，并在失败时展示后端返回的错误信息。
 *
 * @param {Object} row 当前选中的报告记录。
 * @returns {Promise<void>} 预览弹窗打开完成。
 */
async function previewReport(row) {
  revokePreviewUrl()
  previewTitle.value = row.reportName || '报告预览'
  previewError.value = ''
  try {
    const response = await previewReportApi(row.id)
    const contentType = response.headers['content-type'] || 'text/html'
    if (contentType.includes('application/json')) {
      previewError.value = (await parsePreviewError(response.data)) || '报告预览失败'
      previewDialogVisible.value = true
      return
    }
    const blob = new Blob([response.data], { type: contentType })
    previewUrl.value = window.URL.createObjectURL(blob)
    previewDialogVisible.value = true
  } catch (error) {
    previewError.value = error?.message || '报告预览失败'
    previewDialogVisible.value = true
  }
}

/**
 * 释放报告预览产生的临时 Blob 地址。
 *
 * @returns {void} 无返回值。
 */
function revokePreviewUrl() {
  if (previewUrl.value) {
    window.URL.revokeObjectURL(previewUrl.value)
    previewUrl.value = ''
  }
}

/**
 * 关闭报告预览弹窗，并清理相关的临时状态。
 *
 * @returns {void} 无返回值。
 */
function closePreviewDialog() {
  revokePreviewUrl()
  previewDialogVisible.value = false
  previewTitle.value = ''
  previewError.value = ''
}

/**
 * 将预览接口返回的 Blob 错误内容解析为后端统一错误消息。
 *
 * @param {Blob} blob 预览接口返回的错误二进制内容。
 * @returns {Promise<string>} 解析后的错误消息。
 */
async function parsePreviewError(blob) {
  try {
    const text = await blob.text()
    const payload = JSON.parse(text)
    return payload.message || ''
  } catch {
    return ''
  }
}

/**
 * 格式化检测参数的标准范围显示文本。
 *
 * @param {number | null} min 最小标准值。
 * @param {number | null} max 最大标准值。
 * @param {string} unit 检测单位。
 * @returns {string} 标准范围展示文案。
 */
function formatStandardRange(min, max, unit) {
  const suffix = unit ? ` ${unit}` : ''
  if (min != null && max != null) {
    return `${min} - ${max}${suffix}`
  }
  if (min != null) {
    return `>= ${min}${suffix}`
  }
  if (max != null) {
    return `<= ${max}${suffix}`
  }
  return `无范围${suffix}`
}

/**
 * 退出移动端登录态，并跳转到移动端登录页。
 *
 * @returns {void} 无返回值。
 */
function logout() {
  clearToken()
  router.push('/mobile/login')
}

onMounted(async () => {
  await refreshAll()
})

onBeforeUnmount(revokePreviewUrl)
</script>

<style scoped>
.mobile-shell {
  min-height: 100vh;
  padding: 16px 14px 28px;
  background:
    radial-gradient(circle at top, rgba(21, 111, 255, 0.18), transparent 28%),
    linear-gradient(180deg, #f1f7ff 0%, #f7fbff 48%, #ffffff 100%);
}

.mobile-hero {
  padding: 22px 18px;
  border-radius: 24px;
  background:
    linear-gradient(135deg, rgba(16, 104, 219, 0.98), rgba(56, 154, 255, 0.92)),
    linear-gradient(180deg, #1677ff 0%, #0f5fd1 100%);
  color: #ffffff;
  box-shadow: 0 20px 46px rgba(18, 94, 190, 0.22);
}

.hero-tag {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.16);
  font-size: 13px;
  font-weight: 700;
}

.hero-copy h1 {
  margin: 14px 0 10px;
  font-size: 30px;
  line-height: 1.22;
}

.hero-copy p {
  margin: 0;
  color: rgba(255, 255, 255, 0.88);
  line-height: 1.7;
}

.hero-user {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.18);
}

.hero-user strong {
  display: block;
  font-size: 16px;
}

.hero-user p {
  margin: 6px 0 0;
  color: rgba(255, 255, 255, 0.78);
  font-size: 13px;
}

.hero-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.mobile-stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.mobile-stat-card,
.overview-card,
.mobile-panel,
.task-card,
.history-card,
.report-card {
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(214, 227, 246, 0.92);
  box-shadow: 0 12px 28px rgba(38, 68, 108, 0.08);
}

.mobile-stat-card {
  padding: 16px;
}

.mobile-stat-card span {
  display: block;
  color: #5f7388;
}

.mobile-stat-card strong {
  display: block;
  margin-top: 10px;
  font-size: 30px;
  line-height: 1.1;
  color: #22384d;
}

.mobile-stat-card p {
  margin: 10px 0 0;
  color: #70859b;
  line-height: 1.6;
}

.mobile-tabs {
  position: sticky;
  top: 8px;
  z-index: 5;
  display: grid;
  grid-auto-flow: column;
  grid-auto-columns: minmax(78px, 1fr);
  gap: 8px;
  margin-top: 16px;
  padding: 10px 2px;
  overflow-x: auto;
  scroll-padding-inline: 12px;
  scrollbar-width: none;
  backdrop-filter: blur(10px);
}

.mobile-tabs::-webkit-scrollbar {
  display: none;
}

.mobile-tab {
  padding: 12px 6px 10px;
  border: 1px solid #d6e4f6;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.92);
  color: #2f4763;
  min-width: 78px;
}

.mobile-tab span,
.mobile-tab b {
  display: block;
}

.mobile-tab span {
  font-size: 12px;
}

.mobile-tab b {
  margin-top: 6px;
  font-size: 18px;
}

.mobile-tab.active {
  background: linear-gradient(180deg, #1677ff 0%, #0f65de 100%);
  border-color: #1677ff;
  color: #ffffff;
  box-shadow: 0 12px 24px rgba(22, 119, 255, 0.24);
}

.mobile-panel {
  margin-top: 8px;
  padding: 16px;
  overflow: hidden;
}

.mobile-section + .mobile-section {
  margin-top: 20px;
}

.section-headline {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 12px;
}

.section-headline h3 {
  margin: 0;
  font-size: 18px;
  line-height: 1.35;
}

.section-tip {
  color: #70859b;
  font-size: 12px;
  line-height: 1.6;
  text-align: right;
}

.overview-grid,
.card-stack {
  display: grid;
  gap: 12px;
}

.overview-card {
  padding: 16px;
  cursor: pointer;
}

.overview-card span {
  color: #5b7288;
}

.overview-card strong {
  display: block;
  margin-top: 10px;
  font-size: 28px;
}

.overview-card p {
  margin: 8px 0 0;
  color: #70859b;
}

.task-card,
.history-card,
.report-card {
  padding: 16px;
}

.card-title-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 10px;
}

.card-title-row strong {
  font-size: 16px;
  line-height: 1.4;
  color: #24384d;
}

.task-card p,
.history-card p,
.report-card p {
  margin: 8px 0 0;
  color: #546a81;
  line-height: 1.65;
  word-break: break-word;
}

.card-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.warn-text {
  color: #d97706 !important;
  font-weight: 600;
}

.parameter-list {
  display: grid;
  gap: 12px;
  margin-bottom: 16px;
}

.parameter-card {
  padding: 14px;
  border-radius: 16px;
  background: #f8fbff;
  border: 1px solid #d9e6f4;
}

.parameter-title {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 12px;
}

.parameter-title strong {
  color: #24384d;
}

.parameter-title span {
  color: #71859a;
  font-size: 12px;
  text-align: right;
}

.preview-frame {
  width: 100%;
  height: min(75vh, 820px);
  border: none;
  border-radius: 14px;
  background: #f5f7fa;
}

.preview-empty {
  min-height: 160px;
  display: grid;
  place-items: center;
  color: #70859b;
  text-align: center;
  line-height: 1.8;
}

:deep(.mobile-dialog) {
  max-width: calc(100vw - 24px);
  margin: 24px auto !important;
}

:deep(.mobile-dialog .el-dialog__body) {
  max-height: calc(100vh - 220px);
  overflow: auto;
}

:deep(.mobile-preview-dialog) {
  max-width: min(980px, calc(100vw - 24px));
}

:deep(.mobile-preview-dialog .el-dialog__body) {
  padding-top: 12px;
  padding-bottom: 12px;
}

@media (max-width: 560px) {
  .mobile-shell {
    padding: 12px 12px 24px;
  }

  .mobile-hero {
    padding: 18px 16px;
    border-radius: 22px;
  }

  .hero-copy h1 {
    font-size: 24px;
  }

  .mobile-stats {
    grid-template-columns: 1fr;
  }

  .hero-user,
  .section-headline {
    flex-direction: column;
    align-items: stretch;
  }

  .section-tip {
    text-align: left;
  }

  .mobile-tab {
    min-width: 74px;
  }

  :deep(.mobile-dialog) {
    margin: 12px auto !important;
  }

  :deep(.mobile-dialog .el-dialog__body) {
    max-height: calc(100vh - 180px);
  }
}
</style>
