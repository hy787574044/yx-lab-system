<template>
  <div class="mobile-shell">
    <header class="mobile-hero">
      <div class="hero-copy">
        <span class="hero-tag">移动端工作台</span>
        <h1>从采样到报告，一部手机完成流转</h1>
        <p>聚焦待办、快速提单、即时回看，面向现场人员与审核人员的轻量工作台。</p>
      </div>

      <div class="hero-user">
        <div class="hero-user__main">
          <div class="hero-avatar">
            <img v-if="currentUserAvatarSrc" :src="currentUserAvatarSrc" alt="用户头像" />
            <span v-else>{{ currentUserInitial }}</span>
          </div>
          <div>
            <strong>{{ currentUser.realName || currentUser.username || '未登录用户' }}</strong>
            <p>{{ currentUser.roleCode || 'LAB_USER' }}</p>
          </div>
        </div>
        <div class="hero-actions">
          <el-button size="small" plain @click="openProfileDialog">修改资料</el-button>
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
          <p>已登录样品和待重检样品都在这里</p>
        </article>
        <article class="overview-card" @click="activeTab = 'review'">
          <span>审核待办</span>
          <strong>{{ reviewTodos.length }}</strong>
          <p>支持审核通过或驳回重检</p>
        </article>
        <article class="overview-card" @click="activeTab = 'report'">
          <span>正式报告</span>
          <strong>{{ reports.length }}</strong>
          <p>支持正式报告在线预览</p>
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
              <strong>{{ task.pointName || '未命名点位' }}</strong>
              <span class="status-chip" :class="getStatusClass('taskStatus', task.taskStatus)">
                {{ getEnumLabel(taskStatusLabelMap, task.taskStatus) }}
              </span>
            </div>
            <p>任务编号：{{ task.taskNo || '-' }}</p>
            <p>封签编号：{{ task.sealNo || '待录入' }}</p>
            <p>计划时间：{{ task.samplingTime || '-' }}</p>
            <p>样品类型：{{ getEnumLabel(sampleTypeLabelMap, task.sampleType) }}</p>
            <p>检测项目组：{{ task.detectionItems || '-' }}</p>
            <p v-if="task.sampleLogged">已登录样品：{{ task.sampleNo || '-' }}</p>
            <p v-else-if="task.taskStatus === completedTaskStatus" class="warn-text">采样已完成，请尽快进行样品登录</p>
            <div class="card-actions">
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
            <p>采样人员：{{ sample.samplerName || '-' }}</p>
            <p>检测项目组：{{ sample.detectionItems || '-' }}</p>
            <p v-if="sample.resultSummary">当前摘要：{{ translateWorkflowText(sample.resultSummary) }}</p>
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
            <p>检测项目组：{{ row.detectionTypeName || '-' }}</p>
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
            <p>检测项目组：{{ row.detectionTypeName || '-' }}</p>
            <p>检测人员：{{ row.detectorName || '-' }}</p>
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
          <span class="section-tip">支持正式报告在线预览</span>
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
            <p>发布时间：{{ row.publishedTime || row.generatedTime || '-' }}</p>
            <p>发布人员：{{ row.publishedByName || '-' }}</p>
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
        <el-form-item label="采样封签号" required>
          <el-input v-model="completeForm.sealNo" placeholder="请输入或粘贴 OCR 识别的封签号" />
        </el-form-item>
        <el-form-item label="天气">
          <el-input v-model="completeForm.weather" placeholder="例如：晴、多云、小雨" />
        </el-form-item>
        <el-form-item label="温度">
          <el-input v-model="completeForm.temperature" placeholder="例如：26℃" />
        </el-form-item>
        <el-form-item label="现场指标">
          <el-input
            v-model="completeForm.onsiteMetrics"
            type="textarea"
            :rows="3"
            placeholder="例如余氯、浊度、温度等现场指标"
          />
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
        <el-button type="primary" :loading="submitting" @click="submitComplete">保存</el-button>
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
        <el-form-item label="点位名称" required>
          <el-input v-model="loginForm.pointName" />
        </el-form-item>
        <el-form-item label="样品类型" required>
          <el-select v-model="loginForm.sampleType" style="width: 100%">
            <el-option
              v-for="option in sampleTypeOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="质控类型">
          <el-select v-model="loginForm.qualityControlType" clearable placeholder="请选择质控类型" style="width: 100%">
            <el-option
              v-for="option in qualityControlTypeOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="检测项目组" required>
          <el-select
            v-model="loginForm.detectionItems"
            clearable
            filterable
            placeholder="请选择检测项目组"
            style="width: 100%"
          >
            <el-option
              v-for="item in enabledDetectionTypes"
              :key="item.id"
              :label="item.typeName"
              :value="item.typeName"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="采样时间" required>
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
        <el-button type="primary" :loading="submitting" @click="submitSampleLogin">保存</el-button>
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
        <el-form-item label="检测项目组" required>
          <el-select v-model="detectionForm.detectionTypeId" style="width: 100%" @change="handleDetectionTypeChange">
            <el-option
              v-for="item in currentDetectionTypeOptions"
              :key="item.id"
              :label="item.typeName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <div v-if="detectionForm.items.length" class="parameter-list">
          <div v-for="item in detectionForm.items" :key="item.parameterId" class="parameter-card">
            <div class="parameter-title">
              <strong>{{ item.parameterName }}</strong>
              <span>{{ formatStandardRange(item.standardMin, item.standardMax, item.unit) }}</span>
            </div>
            <el-input-number
              v-model="item.resultValue"
              :precision="2"
              :step="0.1"
              controls-position="right"
              style="width: 100%"
            />
          </div>
        </div>
        <el-form-item label="异常说明">
          <el-input v-model="detectionForm.abnormalRemark" type="textarea" :rows="3" placeholder="检测结果异常时建议填写" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="detectionDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitDetection">提交</el-button>
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
          <el-input v-model="reviewForm.rejectReason" type="textarea" :rows="3" placeholder="请填写驳回重检原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitReview">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="previewDialogVisible"
      :title="previewTitle"
      class="mobile-dialog mobile-preview-dialog"
      width="1360px"
      align-center
      destroy-on-close
      @closed="closePreviewDialog"
    >
      <div v-if="previewError" class="preview-empty">{{ previewError }}</div>
      <ReportPrintDocument
        v-else-if="previewData"
        ref="reportPrintRef"
        :preview-data="previewData"
      />
      <div v-else class="preview-empty">正在加载报告文档...</div>
      <template #footer>
        <el-button v-if="previewData" type="primary" plain @click="printPreview">打印</el-button>
        <el-button @click="closePreviewDialog">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="profileDialogVisible"
      title="修改资料"
      width="92%"
      class="mobile-dialog"
      destroy-on-close
      @closed="resetProfileForm"
    >
      <el-form ref="profileFormRef" :model="profileForm" :rules="profileRules" label-position="top">
        <el-form-item label="头像">
          <div class="mobile-avatar-editor">
            <div class="mobile-avatar-preview">
              <img v-if="profileAvatarSrc" :src="profileAvatarSrc" alt="头像预览" />
              <span v-else>{{ currentUserInitial }}</span>
            </div>
            <div class="mobile-avatar-actions">
              <el-upload
                :auto-upload="false"
                :show-file-list="false"
                accept="image/png,image/jpeg,image/jpg,image/webp"
                :on-change="handleProfileAvatarChange"
              >
                <el-button>选择头像</el-button>
              </el-upload>
              <el-button v-if="profileForm.avatarUrl || profileAvatarPreviewUrl" text @click="clearProfileAvatar">移除头像</el-button>
              <p>支持 JPG、PNG、WEBP，图片不超过 2MB。</p>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="用户名">
          <el-input :model-value="currentUser.username || '-'" disabled />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="profileForm.realName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="手机号码" prop="phone">
          <el-input v-model="profileForm.phone" placeholder="请输入手机号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="profileDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingProfile" @click="submitProfileForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import dayjs from 'dayjs'
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElButton } from 'element-plus/es/components/button/index.mjs'
import { ElDatePicker } from 'element-plus/es/components/date-picker/index.mjs'
import { ElDialog } from 'element-plus/es/components/dialog/index.mjs'
import { ElEmpty } from 'element-plus/es/components/empty/index.mjs'
import { ElForm, ElFormItem } from 'element-plus/es/components/form/index.mjs'
import { ElInput } from 'element-plus/es/components/input/index.mjs'
import { ElInputNumber } from 'element-plus/es/components/input-number/index.mjs'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs'
import { ElOption, ElSelect } from 'element-plus/es/components/select/index.mjs'
import { ElUpload } from 'element-plus/es/components/upload/index.mjs'
import {
  fetchDetectionParametersApi,
  fetchDetectionTypesApi,
  fetchMobileDetectionHistoryApi,
  fetchMobileDetectionTodoApi,
  fetchMobileReportsApi,
  fetchMobileReviewHistoryApi,
  fetchMobileReviewTodoApi,
  fetchMobileSamplingTodoApi,
  fetchMobileProfileApi,
  fetchReportPreviewDataApi,
  loginSampleApi,
  mobileLogoutApi,
  previewStorageFileApi,
  startSamplingTaskApi,
  abandonSamplingTaskApi,
  completeSamplingTaskApi,
  submitDetectionApi,
  submitReviewApi,
  updateMobileProfileApi,
  uploadStorageFileApi
} from '../api/lab'
import ReportPrintDocument from '../components/report/ReportPrintDocument.vue'
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
  qualityControlTypeOptions,
  sampleStatusLabelMap,
  sampleTypeLabelMap,
  sampleTypeOptions,
  taskStatusLabelMap,
  translateWorkflowText
} from '../utils/labEnums'

const router = useRouter()

const activeTab = ref('overview')
const refreshing = ref(false)
const submitting = ref(false)
const savingProfile = ref(false)

const currentUser = ref(getUser())
const currentUserAvatarSrc = ref('')
const samplingTodos = ref([])
const detectionTodos = ref([])
const detectionHistory = ref([])
const reviewTodos = ref([])
const reviewHistory = ref([])
const reports = ref([])
const samplingTodoTotal = ref(0)
const detectionTodoTotal = ref(0)
const detectionHistoryTotal = ref(0)
const reviewTodoTotal = ref(0)
const reviewHistoryTotal = ref(0)
const reportTotal = ref(0)

const detectionTypes = ref([])
const detectionParameters = ref([])
const currentDetectionTypeOptions = ref([])

const completeDialogVisible = ref(false)
const loginDialogVisible = ref(false)
const detectionDialogVisible = ref(false)
const reviewDialogVisible = ref(false)
const previewDialogVisible = ref(false)
const profileDialogVisible = ref(false)
const profileFormRef = ref()
const selectedProfileAvatarFile = ref(null)
const profileAvatarPreviewUrl = ref('')

const previewData = ref(null)
const previewTitle = ref('')
const previewError = ref('')
const reportPrintRef = ref(null)

const completeForm = reactive({
  taskId: null,
  sealNo: '',
  onsiteMetrics: '',
  weather: '',
  temperature: '',
  photoUrls: '',
  remark: ''
})

const loginForm = reactive({
  taskId: null,
  pointId: null,
  pointName: '',
  sampleType: '',
  qualityControlType: '',
  detectionItems: '',
  samplingTime: '',
  samplerId: null,
  samplerName: '',
  weather: '',
  storageCondition: '',
  remark: ''
})

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

const profileForm = reactive({
  realName: '',
  phone: '',
  avatarUrl: ''
})

const profileRules = {
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [{ max: 32, message: '手机号长度不能超过32个字符', trigger: 'blur' }]
}

const enabledDetectionTypes = computed(() => detectionTypes.value.filter((item) => item.enabled === 1))
const currentUserInitial = computed(() => (currentUser.value.realName || currentUser.value.username || '用').slice(0, 1))
const profileAvatarSrc = computed(() => profileAvatarPreviewUrl.value || (profileForm.avatarUrl ? currentUserAvatarSrc.value : ''))

const stats = computed(() => [
  { label: '采样待办', value: samplingTodoTotal.value, desc: '待执行和待样品登录任务' },
  { label: '检测待办', value: detectionTodoTotal.value, desc: '待检样品和待重检样品' },
  { label: '审核待办', value: reviewTodoTotal.value, desc: '待审核检测记录' },
  { label: '我的报告', value: reportTotal.value, desc: '正式报告在线预览' }
])

const tabOptions = computed(() => [
  { value: 'overview', label: '总览', count: samplingTodoTotal.value + detectionTodoTotal.value + reviewTodoTotal.value },
  { value: 'sampling', label: '采样', count: samplingTodoTotal.value },
  { value: 'detection', label: '检测', count: detectionTodoTotal.value },
  { value: 'review', label: '审核', count: reviewTodoTotal.value },
  { value: 'report', label: '报告', count: reportTotal.value }
])

function parseDetectionItemsText(value) {
  return String(value || '')
    .split(/[,，、;；\n\r]+/)
    .map((item) => item.trim())
    .filter(Boolean)
}

function matchDetectionTypesForSample(sample) {
  const expectedNames = parseDetectionItemsText(sample?.detectionItems)
  if (!expectedNames.length) {
    return enabledDetectionTypes.value
  }
  const matched = enabledDetectionTypes.value.filter((item) => expectedNames.includes(item.typeName))
  return matched.length ? matched : enabledDetectionTypes.value
}

async function refreshCurrentUser() {
  const user = await fetchMobileProfileApi()
  setUser(user)
  currentUser.value = user
}

function openProfileDialog() {
  profileForm.realName = currentUser.value.realName || ''
  profileForm.phone = currentUser.value.phone || ''
  profileForm.avatarUrl = currentUser.value.avatarUrl || ''
  selectedProfileAvatarFile.value = null
  clearProfileAvatarPreview()
  profileDialogVisible.value = true
}

function resetProfileForm() {
  profileForm.realName = ''
  profileForm.phone = ''
  profileForm.avatarUrl = ''
  selectedProfileAvatarFile.value = null
  clearProfileAvatarPreview()
  profileFormRef.value?.clearValidate?.()
}

async function submitProfileForm() {
  await profileFormRef.value.validate()
  savingProfile.value = true
  try {
    let avatarUrl = String(profileForm.avatarUrl || '').trim()
    if (selectedProfileAvatarFile.value) {
      const uploadResult = await uploadStorageFileApi(selectedProfileAvatarFile.value)
      avatarUrl = uploadResult.filePath || ''
    }
    const nextUser = await updateMobileProfileApi({
      realName: String(profileForm.realName || '').trim(),
      phone: String(profileForm.phone || '').trim(),
      avatarUrl
    })
    setUser(nextUser)
    currentUser.value = nextUser
    profileDialogVisible.value = false
    ElMessage.success('资料修改成功')
  } finally {
    savingProfile.value = false
  }
}

function handleProfileAvatarChange(file) {
  const rawFile = file.raw
  if (!rawFile) {
    return
  }
  if (!rawFile.type?.startsWith('image/')) {
    ElMessage.warning('请选择图片文件作为头像')
    return
  }
  if (rawFile.size > 2 * 1024 * 1024) {
    ElMessage.warning('头像图片不能超过 2MB')
    return
  }
  selectedProfileAvatarFile.value = rawFile
  clearProfileAvatarPreview()
  profileAvatarPreviewUrl.value = URL.createObjectURL(rawFile)
}

function clearProfileAvatar() {
  profileForm.avatarUrl = ''
  selectedProfileAvatarFile.value = null
  clearProfileAvatarPreview()
}

function clearProfileAvatarPreview() {
  if (profileAvatarPreviewUrl.value) {
    URL.revokeObjectURL(profileAvatarPreviewUrl.value)
    profileAvatarPreviewUrl.value = ''
  }
}

function clearCurrentUserAvatarSrc() {
  if (currentUserAvatarSrc.value) {
    URL.revokeObjectURL(currentUserAvatarSrc.value)
    currentUserAvatarSrc.value = ''
  }
}

async function loadCurrentUserAvatar() {
  clearCurrentUserAvatarSrc()
  if (!currentUser.value?.avatarUrl) {
    return
  }
  try {
    const response = await previewStorageFileApi(currentUser.value.avatarUrl)
    currentUserAvatarSrc.value = URL.createObjectURL(response.data)
  } catch {
    currentUserAvatarSrc.value = ''
  }
}

async function refreshAll() {
  refreshing.value = true
  try {
    if (!currentUser.value?.userId && !currentUser.value?.id) {
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
      fetchMobileSamplingTodoApi({ pageNum: 1, pageSize: 200 }),
      fetchMobileDetectionTodoApi({ pageNum: 1, pageSize: 200 }),
      fetchMobileDetectionHistoryApi({ pageNum: 1, pageSize: 200 }),
      fetchMobileReviewTodoApi({ pageNum: 1, pageSize: 200 }),
      fetchMobileReviewHistoryApi({ pageNum: 1, pageSize: 200 }),
      fetchMobileReportsApi({ pageNum: 1, pageSize: 200 })
    ])
    samplingTodos.value = samplingData?.records || []
    detectionTodos.value = detectionTodoData?.records || []
    detectionHistory.value = detectionHistoryData?.records || []
    reviewTodos.value = reviewTodoData?.records || []
    reviewHistory.value = reviewHistoryData?.records || []
    reports.value = reportData?.records || []
    samplingTodoTotal.value = Number(samplingData?.total || 0)
    detectionTodoTotal.value = Number(detectionTodoData?.total || 0)
    detectionHistoryTotal.value = Number(detectionHistoryData?.total || 0)
    reviewTodoTotal.value = Number(reviewTodoData?.total || 0)
    reviewHistoryTotal.value = Number(reviewHistoryData?.total || 0)
    reportTotal.value = Number(reportData?.total || 0)
  } finally {
    refreshing.value = false
  }
}

async function ensureDetectionConfig() {
  if (detectionTypes.value.length && detectionParameters.value.length) {
    return
  }
  const [typeResult, parameterResult] = await Promise.all([
    fetchDetectionTypesApi({ pageNum: 1, pageSize: 200, enabled: 1 }),
    fetchDetectionParametersApi({ pageNum: 1, pageSize: 500 })
  ])
  detectionTypes.value = typeResult.records || []
  detectionParameters.value = parameterResult.records || []
}

async function handleStartTask(task) {
  let sealNo = String(task?.sealNo || '').trim()
  if (!sealNo) {
    try {
      const { value } = await ElMessageBox.prompt(
        '请输入采样封签号，支持手工录入或粘贴 OCR 识别结果。',
        '开始任务前请先录入封签号',
        {
          confirmButtonText: '录入并开始',
          cancelButtonText: '取消',
          inputPlaceholder: '请输入采样封签号',
          inputValidator: (inputValue) => String(inputValue || '').trim() ? true : '封签号不能为空'
        }
      )
      sealNo = String(value || '').trim()
    } catch {
      return
    }
  }
  await startSamplingTaskApi(task.id, { sealNo, remark: '移动端开始采样' })
  ElMessage.success('采样任务已开始。')
  await refreshAll()
}

async function abandonTask(task) {
  try {
    const { value } = await ElMessageBox.prompt('请填写废弃原因', '废弃采样任务', {
      confirmButtonText: '确认废弃',
      cancelButtonText: '取消',
      inputPlaceholder: '例如：现场条件不满足，需改日执行'
    })
    await abandonSamplingTaskApi(task.id, {
      reason: String(value || '').trim(),
      remark: '移动端废弃采样任务'
    })
    ElMessage.success('采样任务已废弃。')
    await refreshAll()
  } catch {
    return
  }
}

function openCompleteDialog(task) {
  completeForm.taskId = task.id
  completeForm.sealNo = task.sealNo || ''
  completeForm.onsiteMetrics = ''
  completeForm.weather = task.weather || ''
  completeForm.temperature = task.temperature || ''
  completeForm.photoUrls = ''
  completeForm.remark = task.remark || ''
  completeDialogVisible.value = true
}

function resetCompleteForm() {
  completeForm.taskId = null
  completeForm.sealNo = ''
  completeForm.onsiteMetrics = ''
  completeForm.weather = ''
  completeForm.temperature = ''
  completeForm.photoUrls = ''
  completeForm.remark = ''
}

async function submitComplete() {
  if (!completeForm.taskId) {
    ElMessage.warning('请选择要完成的采样任务')
    return
  }
  if (!String(completeForm.sealNo || '').trim()) {
    ElMessage.warning('请先录入采样封签号')
    return
  }
  submitting.value = true
  try {
    await completeSamplingTaskApi({
      taskId: completeForm.taskId,
      sealNo: String(completeForm.sealNo || '').trim(),
      onsiteMetrics: completeForm.onsiteMetrics,
      weather: completeForm.weather,
      temperature: completeForm.temperature,
      photoUrls: completeForm.photoUrls,
      remark: completeForm.remark
    })
    completeDialogVisible.value = false
    ElMessage.success('采样任务已完成。')
    await refreshAll()
  } finally {
    submitting.value = false
  }
}

async function openLoginDialog(task) {
  await ensureDetectionConfig()
  loginForm.taskId = task.id
  loginForm.pointId = task.pointId || null
  loginForm.pointName = task.pointName || ''
  loginForm.sampleType = task.sampleType || ''
  loginForm.qualityControlType = ''
  loginForm.detectionItems = String(task.detectionItems || '').trim()
  loginForm.samplingTime = task.samplingTime || dayjs().format('YYYY-MM-DD HH:mm:ss')
  loginForm.samplerId = task.samplerId || currentUser.value.userId || currentUser.value.id || null
  loginForm.samplerName = task.samplerName || currentUser.value.realName || currentUser.value.username || ''
  loginForm.weather = ''
  loginForm.storageCondition = ''
  loginForm.remark = task.remark || ''
  loginDialogVisible.value = true
}

function resetLoginForm() {
  loginForm.taskId = null
  loginForm.pointId = null
  loginForm.pointName = ''
  loginForm.sampleType = ''
  loginForm.qualityControlType = ''
  loginForm.detectionItems = ''
  loginForm.samplingTime = ''
  loginForm.samplerId = null
  loginForm.samplerName = ''
  loginForm.weather = ''
  loginForm.storageCondition = ''
  loginForm.remark = ''
}

async function submitSampleLogin() {
  if (!loginForm.taskId || !loginForm.pointId || !loginForm.pointName || !loginForm.sampleType || !loginForm.detectionItems || !loginForm.samplingTime) {
    ElMessage.warning('请完整填写样品登录信息')
    return
  }
  submitting.value = true
  try {
    const sample = await loginSampleApi({
      ...loginForm,
      detectionItems: String(loginForm.detectionItems || '').trim()
    })
    loginDialogVisible.value = false
    ElMessage.success(`样品登录完成，封签编号：${sample?.sealNo || '-'}`)
    await refreshAll()
    activeTab.value = 'detection'
  } finally {
    submitting.value = false
  }
}

async function openDetectionDialog(sample) {
  await ensureDetectionConfig()
  if (!enabledDetectionTypes.value.length) {
    ElMessage.warning('当前没有可用的检测项目组配置')
    return
  }

  currentDetectionTypeOptions.value = matchDetectionTypesForSample(sample)
  if (!currentDetectionTypeOptions.value.length) {
    ElMessage.warning('当前样品未匹配到可用检测项目组，请先检查样品登录中的项目组配置')
    return
  }

  detectionForm.sampleId = sample.sampleId
  detectionForm.abnormalRemark = ''
  detectionForm.detectionTypeId = currentDetectionTypeOptions.value[0].id
  handleDetectionTypeChange(detectionForm.detectionTypeId)

  if (!detectionForm.items.length) {
    ElMessage.warning('当前检测项目组没有可用参数，请先检查检测配置')
    return
  }
  detectionDialogVisible.value = true
}

function handleDetectionTypeChange(typeId) {
  const type = currentDetectionTypeOptions.value.find((item) => item.id === typeId)
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

function resetDetectionForm() {
  detectionForm.sampleId = null
  detectionForm.detectionTypeId = null
  detectionForm.detectionTypeName = ''
  detectionForm.abnormalRemark = ''
  detectionForm.items = []
  currentDetectionTypeOptions.value = []
}

async function submitDetection() {
  if (!detectionForm.sampleId || !detectionForm.detectionTypeId) {
    ElMessage.warning('请选择检测项目组')
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

function openReviewDialog(row, reviewResult) {
  reviewForm.detectionRecordId = row.id
  reviewForm.reviewResult = reviewResult
  reviewForm.rejectReason = ''
  reviewForm.reviewRemark = reviewResult === approvedReviewResult ? '移动端审核通过' : ''
  reviewDialogVisible.value = true
}

function resetReviewForm() {
  reviewForm.detectionRecordId = null
  reviewForm.reviewResult = approvedReviewResult
  reviewForm.rejectReason = ''
  reviewForm.reviewRemark = ''
}

async function submitReview() {
  if (!reviewForm.detectionRecordId) {
    ElMessage.warning('请选择待审核记录')
    return
  }
  if (reviewForm.reviewResult === rejectedReviewResult && !String(reviewForm.rejectReason || '').trim()) {
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

async function previewReport(row) {
  previewData.value = null
  previewTitle.value = row.reportName || '报告预览'
  previewError.value = ''
  previewDialogVisible.value = true
  try {
    previewData.value = await fetchReportPreviewDataApi(row.id)
  } catch (error) {
    previewError.value = error?.message || '报告预览失败'
  }
}

function printPreview() {
  reportPrintRef.value?.printDocument?.()
}

function closePreviewDialog() {
  previewDialogVisible.value = false
  previewData.value = null
  previewTitle.value = ''
  previewError.value = ''
}

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

async function logout() {
  try {
    await mobileLogoutApi()
  } catch {
    // 令牌过期时也需要清理本地状态。
  }
  clearToken()
  router.push('/mobile/login')
}

watch(
  () => currentUser.value?.avatarUrl,
  () => {
    loadCurrentUserAvatar()
  },
  { immediate: true }
)

onMounted(async () => {
  await refreshAll()
})

onBeforeUnmount(() => {
  previewData.value = null
  clearProfileAvatarPreview()
  clearCurrentUserAvatarSrc()
})
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

.hero-user__main {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.hero-avatar {
  width: 42px;
  height: 42px;
  flex: none;
  display: grid;
  place-items: center;
  border-radius: 50%;
  overflow: hidden;
  color: #ffffff;
  font-weight: 800;
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.34);
}

.hero-avatar img,
.mobile-avatar-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
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
  justify-content: flex-end;
}

.mobile-avatar-editor {
  display: flex;
  align-items: center;
  gap: 16px;
}

.mobile-avatar-preview {
  width: 72px;
  height: 72px;
  flex: none;
  display: grid;
  place-items: center;
  border-radius: 50%;
  overflow: hidden;
  color: #ffffff;
  font-size: 24px;
  font-weight: 800;
  background: linear-gradient(135deg, #1677ff, #36a3ff);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.34);
}

.mobile-avatar-actions {
  display: grid;
  gap: 8px;
}

.mobile-avatar-actions p {
  margin: 0;
  color: var(--text-sub);
  font-size: 12px;
  line-height: 1.6;
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
  max-width: min(1360px, calc(100vw - 24px));
}

:deep(.mobile-preview-dialog .el-dialog__body) {
  padding: 10px 18px 18px;
  background: #eef3fb;
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

  .hero-actions {
    justify-content: flex-start;
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
