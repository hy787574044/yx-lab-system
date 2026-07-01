<template>
  <div class="content-grid dashboard-page" v-loading="loading">
    <section class="glass-panel workbench-hero">
      <div class="workbench-hero__title">
        <h3>化验室工作台</h3>
      </div>
      <div class="workbench-hero__flow">
        <span>流程概览</span>
        <div class="workbench-hero__flow-chips">
          <button
            v-for="item in workflowOverviewItems"
            :key="item.key"
            type="button"
            @click="openWorkbenchAction(item.action)"
          >
            {{ item.label }}
          </button>
        </div>
      </div>
      <div class="workbench-hero__stats">
        <span>
          <strong>{{ warningTotal }}</strong>
          <em>待处理预警</em>
        </span>
        <span>
          <strong>{{ latestPassRate }}%</strong>
          <em>近况合格率</em>
        </span>
      </div>
    </section>

    <section class="todo-detail-grid">
      <article
        v-for="section in workbenchSections"
        :key="section.key"
        class="glass-panel todo-detail-panel"
      >
        <div class="todo-detail-panel__head">
          <div>
            <h3>{{ section.title }}</h3>
          </div>
          <strong>{{ section.rows.length }}</strong>
        </div>
        <div v-if="section.rows.length" class="todo-detail-list">
          <div
            v-for="row in section.rows"
            :key="`${section.key}-${getActionRowKey(row)}`"
            role="button"
            tabindex="0"
            :class="['todo-detail-card', `todo-detail-card--${section.key}`]"
            @click="handlePreviewCardClick(section.key, row)"
            @keydown.enter.prevent="handlePreviewCardClick(section.key, row)"
            @keydown.space.prevent="handlePreviewCardClick(section.key, row)"
          >
            <div class="todo-detail-card__main">
              <span>{{ getPreviewRowTitle(section.key, row) }}</span>
              <p v-if="getPreviewDisplayRowMeta(section.key, row)">{{ getPreviewDisplayRowMeta(section.key, row) }}</p>
            </div>
            <div class="todo-detail-card__fields">
              <span v-for="field in getVisiblePreviewFields(section.key, row)" :key="field.label">
                <em>{{ field.label }}</em>
                <strong
                  v-if="field.statusType || field.statusClass"
                  class="status-chip"
                  :class="field.statusClass || getStatusClass(field.statusType, field.statusValue)"
                >
                  {{ field.value || '-' }}
                </strong>
                <strong v-else>{{ field.value || '-' }}</strong>
              </span>
            </div>
            <el-button
              v-if="section.key === 'samplingPlan'"
              v-permission="'samplingPlan:write'"
              class="todo-detail-card__action"
              type="primary"
              size="small"
              :loading="dispatchingPlanId === row.id"
              :disabled="dispatchSubmitting || !actionablePlanStatuses.includes(row.planStatus)"
              @click.stop="dispatchSamplingPlanFromWorkbench(row)"
              @keydown.stop
            >
              派发
            </el-button>
            <el-button
              v-if="section.key === 'sampling'"
              v-permission="'sample:write'"
              class="todo-detail-card__action todo-detail-card__action--center"
              type="primary"
              size="small"
              @click.stop="openWorkbenchAction('sampleLogin', row)"
              @keydown.stop
            >
              样品登录
            </el-button>
          </div>
        </div>
        <div v-else class="todo-detail-empty">暂无待处理数据</div>
      </article>
    </section>

    <el-dialog
      v-model="samplingPlanDetailVisible"
      class="sampling-plan-detail-dialog"
      title="采样计划详情"
      width="760px"
      align-center
    >
      <div v-if="selectedSamplingPlan" class="plan-detail">
        <div class="plan-detail__title">
          <strong>{{ selectedSamplingPlan.planName || '-' }}</strong>
          <span
            class="status-chip"
            :class="getStatusClass('planStatus', selectedSamplingPlan.planStatus)"
          >
            {{ getEnumLabel(planStatusLabelMap, selectedSamplingPlan.planStatus) }}
          </span>
        </div>
        <div class="plan-detail__grid">
          <div v-for="field in samplingPlanDetailFields" :key="field.label">
            <span>{{ field.label }}</span>
            <strong>{{ field.value || '-' }}</strong>
          </div>
        </div>
        <div class="plan-detail__remark">
          <span>备注</span>
          <p>{{ selectedSamplingPlan.remark || '-' }}</p>
        </div>
      </div>
      <template #footer>
        <el-button @click="samplingPlanDetailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="dispatchDialogVisible"
      class="dispatch-plan-dialog"
      title="派发采样计划"
      width="560px"
      align-center
      destroy-on-close
      @closed="resetDispatchForm"
    >
      <el-form label-width="96px">
        <el-form-item label="采样人员" required>
          <el-select
            v-model="dispatchForm.samplerIds"
            multiple
            collapse-tags
            collapse-tags-tooltip
            clearable
            filterable
            style="width: 100%"
            placeholder="请选择采样员，可多选"
            :loading="samplerLoading"
            @visible-change="handleSamplerDropdownVisible"
            @change="handleDispatchSamplerChange"
          >
            <el-option
              v-for="item in samplerOptions"
              :key="item.id"
              :label="getSamplerDisplayName(item)"
              :value="getSamplerOptionId(item)"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="执行时间">
          <el-date-picker
            v-model="dispatchForm.samplingTime"
            type="datetime"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :disabled="dispatchSubmitting" @click="dispatchDialogVisible = false">取消</el-button>
        <el-button
          v-permission="'samplingPlan:write'"
          type="primary"
          :loading="dispatchSubmitting"
          :disabled="dispatchSubmitting"
          @click="submitDispatchForm"
        >
          确认派发
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="workbenchDialogVisible"
      class="workbench-action-dialog"
      :title="activeActionTitle"
      :width="activeAction === 'sampling' ? '1140px' : (activeAction === 'detection' || activeAction === 'detectionSplit') ? '1080px' : activeAction === 'samplingPlan' ? '900px' : '1180px'"
      align-center
      destroy-on-close
      @closed="resetWorkbenchDialog"
    >
      <div
        :class="[
          'workbench-dialog',
          {
            'workbench-dialog--sampling': activeAction === 'sampling',
            'workbench-dialog--sample-login': activeAction === 'sampleLogin',
            'workbench-dialog--detection': activeAction === 'detection',
            'workbench-dialog--report': activeAction === 'report',
            'workbench-dialog--report-half-month': activeAction === 'report' && previewData?.previewMode === 'HALF_MONTHLY_TERMINAL_TEMPLATE'
          }
        ]"
        v-loading="actionLoading"
      >
        <aside class="workbench-dialog__side">
          <div class="workbench-dialog__side-head">
            <span>待处理列表</span>
            <strong>{{ actionRows.length }}</strong>
          </div>
          <div v-if="actionRows.length" class="workbench-row-list">
            <button
              v-for="row in actionRows"
              :key="getActionRowKey(row)"
              type="button"
              :class="['workbench-row-card', { 'is-active': isActionRowActive(row) }]"
              @click="selectActionRow(row)"
            >
              <strong>{{ getActionRowTitle(row) }}</strong>
              <span>{{ getActionRowMeta(row) }}</span>
            </button>
          </div>
          <div v-else class="empty-block">当前没有可处理数据</div>
        </aside>

        <main class="workbench-dialog__main">
          <template v-if="activeAction === 'sampling'">
            <el-form label-width="96px">
              <div class="form-grid sampling-entry-grid">
                <el-form-item label="任务编号">
                  <el-input :model-value="samplingTask?.taskNo || '-'" readonly />
                </el-form-item>
                <el-form-item label="样品编号">
                  <el-input :model-value="samplingTask?.sampleNo || '-'" readonly />
                </el-form-item>
                <el-form-item label="点位名称">
                  <el-input :model-value="samplingTask?.pointName || '-'" readonly />
                </el-form-item>
                <el-form-item label="采样人员">
                  <el-input :model-value="samplingTask?.samplerName || '-'" readonly />
                </el-form-item>
                <el-form-item label="样品类型">
                  <el-input :model-value="getEnumLabel(sampleTypeLabelMap, samplingTask?.sampleType) || '-'" readonly />
                </el-form-item>
                <el-form-item label="任务状态">
                  <el-input :model-value="getEnumLabel(taskStatusLabelMap, samplingTask?.taskStatus) || '-'" readonly />
                </el-form-item>
                <el-form-item label="计划时间">
                  <el-input :model-value="formatDate(samplingTask?.samplingTime) || '-'" readonly />
                </el-form-item>
                <el-form-item label="登记状态">
                  <el-input :model-value="getEnumLabel(sampleRegisterStatusLabelMap, samplingTask?.sampleRegisterStatus || unregisteredSampleRegisterStatus)" readonly />
                </el-form-item>
                <el-form-item class="form-span-2" label="备注">
                  <el-input :model-value="samplingTask?.remark || '-'" type="textarea" :rows="3" readonly />
                </el-form-item>
              </div>
            </el-form>
          </template>

          <template v-else-if="activeAction === 'sampleLogin'">
            <el-form label-width="96px">
              <div class="form-grid sample-login-grid">
                <el-form-item label="待登录任务">
                  <el-input :model-value="formatTaskLabel(loginTask)" readonly />
                </el-form-item>
                <el-form-item label="样品编号">
                  <el-input :model-value="loginForm.sampleNo || '-'" readonly />
                </el-form-item>
                <el-form-item label="点位名称">
                  <el-input :model-value="loginForm.pointName || '-'" readonly />
                </el-form-item>
                <el-form-item label="样品类型">
                  <el-input :model-value="getEnumLabel(sampleTypeLabelMap, loginForm.sampleType)" readonly />
                </el-form-item>
                <el-form-item label="样品来源">
                  <el-select v-model="loginForm.sampleSourceMethod" style="width: 100%">
                    <el-option v-for="option in sampleSourceMethodOptions" :key="option.value" :label="option.label" :value="option.value" />
                  </el-select>
                </el-form-item>
                <el-form-item label="采样人员">
                  <el-input v-model="loginForm.samplerName" readonly />
                </el-form-item>
                <el-form-item class="form-span-2" label="检测套餐" required>
                  <el-select v-model="loginForm.detectionTypeId" filterable style="width: 100%" @change="handleLoginDetectionTypeChange">
                    <el-option
                      v-for="item in loginDetectionTypeOptions"
                      :key="item.id"
                      :label="formatDetectionTypeLabel(item)"
                      :value="item.id"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="审核流程" required>
                  <el-select v-model="loginForm.reviewFlowId" filterable style="width: 100%" @change="handleReviewFlowChange">
                    <el-option v-for="item in reviewFlowOptions" :key="item.id" :label="item.flowName" :value="item.id" />
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
                  <el-select v-model="loginForm.weather" allow-create clearable filterable default-first-option style="width: 100%">
                    <el-option v-for="option in weatherOptions" :key="option.value" :label="option.label" :value="option.value" />
                  </el-select>
                </el-form-item>
                <el-form-item label="保存条件">
                  <el-select v-model="loginForm.storageCondition" allow-create clearable filterable default-first-option style="width: 100%">
                    <el-option v-for="option in storageConditionOptions" :key="option.value" :label="option.label" :value="option.value" />
                  </el-select>
                </el-form-item>
              </div>
            </el-form>
            <el-table
              v-if="loginForm.detectionConfigItems.length"
              class="compact-table sample-login-config-table"
              :data="loginForm.detectionConfigItems"
              size="small"
              border
              max-height="190"
            >
              <el-table-column prop="parameterName" label="检测参数" min-width="140" />
              <el-table-column label="标准范围" min-width="120">
                <template #default="{ row }">{{ formatStandardRange(row.standardMin, row.standardMax, row.unit) }}</template>
              </el-table-column>
              <el-table-column prop="methodName" label="检测方法" min-width="160" />
              <el-table-column prop="sampleVolume" label="取样体积" width="100" />
            </el-table>
          </template>

          <template v-else-if="activeAction === 'detection'">
            <div class="summary-chips">
              <span>样品编号<strong>{{ resultForm.sampleNo || '-' }}</strong></span>
              <span>检测参数<strong>{{ resultForm.parameterName || '-' }}</strong></span>
              <span>检测人员<strong>{{ resultForm.detectorName || '-' }}</strong></span>
            </div>
            <div class="meta-grid">
              <div><span>检测方法</span><strong>{{ resultForm.methodName || '-' }}</strong></div>
              <div><span>检测标准</span><strong>{{ resultForm.referenceStandard || '-' }}</strong></div>
              <div><span>标准范围</span><strong>{{ formatStandardRange(resultForm.standardMin, resultForm.standardMax, resultForm.unit) }}</strong></div>
              <div><span>单位</span><strong>{{ resultForm.unit || '-' }}</strong></div>
            </div>
            <div class="detection-step-row">
              <span>检测步骤</span>
              <p>{{ resultForm.methodBasis || '-' }}</p>
            </div>
            <el-form label-position="top">
              <el-form-item label="检测结果" required>
                <div class="result-value-field">
                  <el-input
                    v-model="resultForm.resultValue"
                    inputmode="decimal"
                    class="result-value-input"
                    @input="handleDetectionResultInput"
                  />
                  <span v-if="resultForm.unit" class="result-value-unit">{{ resultForm.unit }}</span>
                </div>
              </el-form-item>
              <el-form-item class="detection-textarea-item" label="异常说明">
                <el-input v-model="resultForm.abnormalRemark" type="textarea" :rows="3" />
              </el-form-item>
              <el-form-item class="detection-textarea-item" label="备注">
                <el-input v-model="resultForm.remark" type="textarea" :rows="3" />
              </el-form-item>
            </el-form>
          </template>

          <template v-else-if="activeAction === 'review'">
            <div class="summary-chips">
              <span>样品编号<strong>{{ reviewForm.sampleNo || '-' }}</strong></span>
              <span>检测套餐<strong>{{ reviewForm.detectionTypeName || '-' }}</strong></span>
              <span>待审核<strong>{{ pendingReviewItemCount }}</strong></span>
            </div>
            <div class="review-toolbar">
              <el-button type="primary" @click="setAllReviewResult(approvedReviewResult)">一键通过</el-button>
              <el-button type="danger" plain @click="setAllReviewResult(rejectedReviewResult)">一键驳回</el-button>
            </div>
            <el-table class="compact-table" :data="reviewForm.items" size="small" border max-height="430">
              <el-table-column prop="parameterName" label="检测参数" min-width="130" />
              <el-table-column prop="methodName" label="检测方法" min-width="150" />
              <el-table-column label="标准范围" min-width="130">
                <template #default="{ row }">{{ formatStandardRange(row.standardMin, row.standardMax, row.unit) }}</template>
              </el-table-column>
              <el-table-column prop="unit" label="单位" width="90" />
              <el-table-column prop="resultValue" label="检测值" width="100" />
              <el-table-column label="审核操作" min-width="180">
                <template #default="{ row }">
                  <el-button size="small" :type="row.reviewResultDraft === approvedReviewResult ? 'primary' : 'default'" @click="row.reviewResultDraft = approvedReviewResult">通过</el-button>
                  <el-button size="small" :type="row.reviewResultDraft === rejectedReviewResult ? 'danger' : 'default'" @click="row.reviewResultDraft = rejectedReviewResult">驳回</el-button>
                </template>
              </el-table-column>
              <el-table-column label="驳回原因" min-width="220">
                <template #default="{ row }">
                  <el-input v-model="row.rejectReasonDraft" :disabled="row.reviewResultDraft !== rejectedReviewResult" placeholder="驳回时填写原因" />
                </template>
              </el-table-column>
            </el-table>
            <el-form label-position="top" class="review-remark-form">
              <el-form-item label="整单审核意见">
                <el-input v-model="reviewForm.reviewRemark" type="textarea" :rows="3" />
              </el-form-item>
            </el-form>
          </template>

          <template v-else-if="activeAction === 'report'">
            <div v-if="previewError" class="empty-block">{{ previewError }}</div>
            <div v-else-if="previewData && previewComponent" class="workbench-report-preview">
              <div class="workbench-report-preview__scale">
                <component
                  :is="previewComponent"
                  :preview-data="previewData"
                />
              </div>
            </div>
            <div v-else-if="previewData" class="empty-block">暂无可预览模板</div>
            <div v-else class="empty-block">请选择左侧报告进行预览</div>
          </template>
        </main>
      </div>

      <template #footer>
        <el-button v-if="activeAction === 'sampling'" type="primary" @click="openWorkbenchAction('sampleLogin', activeRow)">样品登录</el-button>
        <el-button @click="workbenchDialogVisible = false">关闭</el-button>
        <el-button v-if="activeAction === 'sampleLogin'" type="primary" :loading="submitting" @click="submitSampleLogin">保存</el-button>
        <el-button v-if="activeAction === 'detection'" type="primary" :loading="submitting" @click="submitDetectionResult">提交</el-button>
        <el-button v-if="activeAction === 'review'" type="primary" :loading="submitting" @click="submitReviewDecision">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { ElButton } from 'element-plus/es/components/button/index.mjs'
import { ElDatePicker } from 'element-plus/es/components/date-picker/index.mjs'
import { ElDialog } from 'element-plus/es/components/dialog/index.mjs'
import { ElForm, ElFormItem } from 'element-plus/es/components/form/index.mjs'
import { ElInput } from 'element-plus/es/components/input/index.mjs'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElOption, ElSelect } from 'element-plus/es/components/select/index.mjs'
import { ElTable, ElTableColumn } from 'element-plus/es/components/table/index.mjs'
import { ElLoadingDirective } from 'element-plus/es/components/loading/index.mjs'
import {
  dispatchSamplingPlanApi,
  fetchDetectionDetailApi,
  fetchDetectionItemsApi,
  fetchDetectionMethodOptionsApi,
  fetchDetectionParametersApi,
  fetchDetectionsApi,
  fetchDetectionTypesApi,
  fetchDictItemsApi,
  fetchFlowConfigOptionsApi,
  fetchSamplingPlansApi,
  fetchSamplingTaskDetailApi,
  fetchSamplingTasksApi,
  fetchSummaryReportsApi,
  fetchSystemUsersApi,
  leaderDashboardApi,
  loginSampleApi,
  previewSummaryReportApi,
  submitDetectionApi,
  submitReviewApi
} from '../api/lab'
import DailyExternalSummaryTemplate from '../components/report/DailyExternalSummaryTemplate.vue'
import DailyInternalSummaryTemplate from '../components/report/DailyInternalSummaryTemplate.vue'
import HalfMonthlyTerminalSummaryTemplate from '../components/report/HalfMonthlyTerminalSummaryTemplate.vue'
import WeeklyFactorySummaryTemplate from '../components/report/WeeklyFactorySummaryTemplate.vue'
import {
  approvedDetectionStatus,
  approvedReviewResult,
  activePlanStatus,
  actionablePlanStatuses,
  cycleTypeLabelMap,
  detectionStatusLabelMap,
  getEnumLabel,
  getStatusClass,
  planStatusLabelMap,
  rejectedDetectionStatus,
  rejectedReviewResult,
  reviewPendingDetectionStatus,
  sampleRegisterStatusLabelMap,
  sampleSourceMethodOptions,
  sampleTypeLabelMap,
  samplingTypeLabelMap,
  samplingSampleSourceMethod,
  taskStatusLabelMap,
  unregisteredSampleRegisterStatus,
  waitDetectDetectionStatus
} from '../utils/labEnums'
import { getUser } from '../utils/auth'
import { hasPermission } from '../utils/permission'
import { isStaffRole } from '../utils/menuPermission'

const FLOW_TYPE_REVIEW = 'REVIEW'
const SUMMARY_TYPE_DAILY = 'DAILY'
const SUMMARY_TYPE_WEEKLY = 'WEEKLY'
const SUMMARY_TYPE_HALF_MONTHLY = 'HALF_MONTHLY'
const vLoading = ElLoadingDirective
const loading = ref(false)
const actionLoading = ref(false)
const submitting = ref(false)
const dispatchSubmitting = ref(false)
const dispatchingPlanId = ref(null)
const currentUser = ref(getUser() || {})
const dispatchDialogVisible = ref(false)
const dashboard = ref({})
const activeAction = ref('')
const workbenchDialogVisible = ref(false)
const samplingPlanDetailVisible = ref(false)
const actionRows = ref([])
const activeRow = ref(null)
const selectedSamplingPlan = ref(null)
const samplerOptions = ref([])
const samplerLoading = ref(false)
const weatherOptions = ref([])
const storageConditionOptions = ref([])
const detectionTypes = ref([])
const detectionParameters = ref([])
const detectionMethods = ref([])
const reviewFlowOptions = ref([])
const previewData = ref(null)
const previewError = ref('')
const previewRows = reactive({
  samplingPlan: [],
  sampling: [],
  sampleLogin: [],
  detection: [],
  review: [],
  report: []
})

const samplingTask = ref(null)
const loginTask = ref(null)

const dispatchForm = reactive({
  planId: null,
  samplingTime: '',
  samplerIds: [],
  samplerId: null,
  samplerName: ''
})

const loginForm = reactive({
  taskId: null,
  sampleNo: '',
  pointId: null,
  pointName: '',
  sampleType: '',
  sampleSourceMethod: samplingSampleSourceMethod,
  detectionItems: '',
  detectionTypeId: null,
  detectionTypeName: '',
  detectionConfigItems: [],
  reviewFlowId: null,
  reviewFlowName: '',
  samplingTime: '',
  samplerId: null,
  samplerName: '',
  weather: '',
  storageCondition: '',
  remark: ''
})

const resultForm = reactive({
  id: null,
  recordId: null,
  sampleId: null,
  sampleNo: '',
  detectionTypeId: null,
  detectionTypeName: '',
  parameterId: null,
  parameterName: '',
  methodName: '',
  methodBasis: '',
  standardMin: null,
  standardMax: null,
  unit: '',
  referenceStandard: '',
  detectorName: '',
  resultValue: null,
  abnormalRemark: '',
  remark: '',
  itemStatus: ''
})

const reviewForm = reactive({
  detectionRecordId: null,
  sampleNo: '',
  detectionTypeName: '',
  reviewRemark: '',
  items: []
})

const staffWorkbenchSectionKeys = new Set(['sampling', 'detection', 'report'])
const hiddenAdminWorkbenchSectionKeys = new Set(['samplingPlan', 'sampleLogin'])
const workbenchSections = computed(() => {
  const sections = [
    {
      key: 'samplingPlan',
      title: '采样计划',
      rows: previewRows.samplingPlan
    },
    {
      key: 'sampling',
      title: '采样任务',
      rows: previewRows.sampling
    },
    {
      key: 'sampleLogin',
      title: '样品登录',
      rows: previewRows.sampleLogin
    },
    {
      key: 'detection',
      title: '检测录入',
      rows: previewRows.detection
    },
    {
      key: 'review',
      title: '结果审核',
      rows: previewRows.review
    },
    {
      key: 'report',
      title: '报告处理',
      rows: previewRows.report
    }
  ]
  if (isStaffRole(currentUser.value)) {
    return sections.filter((section) => staffWorkbenchSectionKeys.has(section.key))
  }
  return sections.filter((section) => !hiddenAdminWorkbenchSectionKeys.has(section.key))
})
const workflowOverviewItems = computed(() => {
  const defaultItems = [
    { key: 'samplingPlan', label: '采样计划', action: 'samplingPlan', permission: 'samplingPlan:view' },
    { key: 'sampling', label: '采样任务', action: 'sampling', permission: 'samplingTask:view' },
    { key: 'sampleLogin', label: '样品登录', action: 'sampleLogin', permission: 'sample:view' },
    { key: 'detectionSplit', label: '检测分样', action: 'detectionSplit', permission: 'detection:view' },
    { key: 'detection', label: '化验检测', action: 'detection', permission: 'detection:view' },
    { key: 'review', label: '结果审查', action: 'review', permission: 'review:view' },
    { key: 'report', label: '生成报告', action: 'report', permission: 'report:view' }
  ]
  // 根据权限过滤
  const filteredItems = defaultItems.filter((item) => hasPermission(item.permission))
  const processNodes = dashboard.value.processNodes || []
  if (!processNodes.length) {
    return filteredItems
  }
  return filteredItems.map((item) => {
    const matched = processNodes.find((node) => node.label === item.label)
    return matched ? { ...item, label: matched.label || item.label } : item
  })
})
const warningTotal = computed(() => (dashboard.value.warnings || []).reduce((sum, item) => sum + toSafeNumber(item.count), 0))
const latestPassRate = computed(() => {
  const trendItems = dashboard.value.passRateTrend || []
  const latest = trendItems[trendItems.length - 1]
  return toSafeNumber(latest?.value).toFixed(0)
})
const activeActionTitle = computed(() => ({
  samplingPlan: '采样计划',
  sampling: '采样任务',
  sampleLogin: '样品登录',
  detectionSplit: '检测分样',
  detection: '检测结果录入',
  review: '结果审核',
  report: '报告处理'
}[activeAction.value] || '快捷处理'))
const loginDetectionTypeOptions = computed(() => {
  const sampleType = String(loginForm.sampleType || '').trim()
  if (!sampleType) {
    return detectionTypes.value
  }
  return detectionTypes.value.filter((item) => !item.sampleType || String(item.sampleType) === sampleType)
})
const pendingReviewItemCount = computed(() => reviewForm.items.filter((item) => item.itemStatus === reviewPendingDetectionStatus).length)
const previewComponent = computed(() => resolveSummaryPreviewComponent(previewData.value?.previewMode))
const samplingPlanDetailFields = computed(() => {
  const plan = selectedSamplingPlan.value || {}
  return [
    { label: '点位名称', value: plan.pointName },
    { label: '所属地址', value: plan.address },
    { label: '采样人员', value: plan.samplerName },
    { label: '样品类型', value: getEnumLabel(sampleTypeLabelMap, plan.sampleType) },
    { label: '检测套餐', value: plan.detectionTypeName },
    { label: '采样周期', value: getEnumLabel(cycleTypeLabelMap, plan.cycleType) },
    { label: '开始时间', value: formatDate(plan.startTime) },
    { label: '结束时间', value: formatDate(plan.endTime) },
    { label: '坐标', value: formatPlanCoordinate(plan) },
    { label: '采样方式', value: getEnumLabel(samplingTypeLabelMap, plan.samplingType) }
  ]
})

function toSafeNumber(value) {
  const num = typeof value === 'number' ? value : Number.parseFloat(String(value ?? '').replace(/,/g, '').trim())
  return Number.isFinite(num) ? num : 0
}

function normalizeDictOptions(items) {
  return (items || []).map((item) => ({
    label: item.itemName || item.label || item.value || item.itemValue,
    value: item.itemValue || item.value || item.itemName || item.label
  })).filter((item) => item.value)
}

function formatPlanCoordinate(plan) {
  if (!plan?.longitude && !plan?.latitude) {
    return ''
  }
  return `${plan.longitude || '-'}, ${plan.latitude || '-'}`
}

function normalizeSamplerIdList(value) {
  const rawItems = Array.isArray(value)
    ? value
    : String(value || '')
      .split(',')
      .map((item) => item.trim())
  return rawItems
    .map((item) => Number(item))
    .filter((item) => Number.isFinite(item) && item > 0)
    .filter((item, index, source) => source.indexOf(item) === index)
}

function getSamplerOptionId(item) {
  const id = Number(item?.id ?? item?.userId ?? item?.user_id)
  return Number.isFinite(id) && id > 0 ? id : item?.id
}

function getSamplerDisplayName(item) {
  return String(
    item?.realName
    || item?.real_name
    || item?.name
    || item?.nickName
    || item?.nickname
    || item?.displayName
    || item?.employeeName
    || item?.label
    || item?.username
    || ''
  ).trim()
}

function splitSamplerNames(value) {
  return String(value || '')
    .split(/[、,，]/)
    .map((item) => item.trim())
    .filter(Boolean)
}

function normalizeSamplerOption(item) {
  const id = getSamplerOptionId(item)
  return {
    ...item,
    id,
    label: getSamplerDisplayName(item) || String(id || '')
  }
}

function dedupeSamplerOptions(options) {
  const idSet = new Set()
  const nameSet = new Set()
  const result = []
  ;(options || []).forEach((item) => {
    const id = Number(getSamplerOptionId(item))
    const name = getSamplerDisplayName(item)
    const idKey = Number.isFinite(id) && id > 0 ? String(id) : ''
    const nameKey = name || ''
    if ((idKey && idSet.has(idKey)) || (nameKey && nameSet.has(nameKey))) {
      return
    }
    if (idKey) {
      idSet.add(idKey)
    }
    if (nameKey) {
      nameSet.add(nameKey)
    }
    result.push(item)
  })
  return result
}

function ensureSelectedSamplerOptions(ids, samplerNameText) {
  const selectedIds = normalizeSamplerIdList(ids)
  if (!selectedIds.length) {
    return
  }
  const names = splitSamplerNames(samplerNameText)
  const existingIds = new Set(samplerOptions.value.map((item) => Number(getSamplerOptionId(item))))
  const existingNames = new Set(samplerOptions.value.map((item) => getSamplerDisplayName(item)).filter(Boolean))
  const additions = selectedIds
    .filter((id, index) => !existingIds.has(id) && !existingNames.has(names[index]))
    .map((id, index) => ({
      id,
      realName: names[index] || `采样员${id}`,
      username: String(id),
      label: names[index] || `采样员${id}`
    }))
  if (additions.length) {
    samplerOptions.value = dedupeSamplerOptions([...samplerOptions.value, ...additions])
  }
}

function resolveSamplerNames(ids) {
  const selectedIds = normalizeSamplerIdList(ids)
  return selectedIds
    .map((id) => samplerOptions.value.find((item) => Number(getSamplerOptionId(item)) === id))
    .filter(Boolean)
    .map((item) => getSamplerDisplayName(item))
    .filter(Boolean)
    .join('、')
}

function getRowSamplerIds(row) {
  const ids = normalizeSamplerIdList(row?.samplerIds || row?.sampler_ids)
  if (ids.length) {
    return ids
  }
  return normalizeSamplerIdList(row?.samplerId || row?.sampler_id)
}

function resolveRowSamplerOptionIds(row) {
  const names = splitSamplerNames(row?.samplerName || row?.sampler_name)
  const ids = getRowSamplerIds(row)
  const resolved = ids.map((id, index) => {
    const idMatched = samplerOptions.value.find((item) => Number(getSamplerOptionId(item)) === id)
    if (idMatched) {
      return Number(getSamplerOptionId(idMatched))
    }
    const name = names[index]
    if (!name) {
      return id
    }
    const nameMatched = samplerOptions.value.find((item) => getSamplerDisplayName(item) === name)
    return nameMatched ? Number(getSamplerOptionId(nameMatched)) : id
  })
  return normalizeSamplerIdList(resolved)
}

function resetDispatchForm() {
  dispatchForm.planId = null
  dispatchForm.samplingTime = ''
  dispatchForm.samplerIds = []
  dispatchForm.samplerId = null
  dispatchForm.samplerName = ''
}

function handleDispatchSamplerChange(userIds) {
  const ids = normalizeSamplerIdList(userIds)
  dispatchForm.samplerIds = ids
  dispatchForm.samplerId = ids[0] || null
  dispatchForm.samplerName = resolveSamplerNames(ids)
}

async function loadSamplers(force = false) {
  if (!force && samplerOptions.value.length) {
    return
  }
  samplerLoading.value = true
  try {
    const result = await fetchSystemUsersApi({
      pageNum: 1,
      pageSize: 500,
      roleCode: 'STAFF',
      status: 1
    })
    const records = Array.isArray(result.records) ? result.records : []
    samplerOptions.value = dedupeSamplerOptions(records.map(normalizeSamplerOption).filter((item) => item.id))
  } finally {
    samplerLoading.value = false
  }
}

function handleSamplerDropdownVisible(visible) {
  if (visible) {
    loadSamplers(true)
  }
}

function nowDateTimeText() {
  const date = new Date()
  const pad = (value) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

async function refreshDashboard() {
  dashboard.value = await leaderDashboardApi() || {}
}

async function loadWorkbenchPreviewRows() {
  const previewPageSize = 12
  const [planResult, samplingResult, loginResult, detectionResult, reviewResult, summaryReportRows] = await Promise.all([
    fetchSamplingPlansApi({ pageNum: 1, pageSize: previewPageSize, planStatus: activePlanStatus }),
    fetchSamplingTasksApi({ pageNum: 1, pageSize: previewPageSize, sampleRegisterStatus: 'UNREGISTERED' }),
    fetchSamplingTasksApi({ pageNum: 1, pageSize: previewPageSize, sampleRegisterStatus: 'UNREGISTERED' }),
    fetchDetectionItemsApi({ pageNum: 1, pageSize: previewPageSize, itemStatus: waitDetectDetectionStatus }),
    fetchDetectionsApi({ pageNum: 1, pageSize: previewPageSize, detectionStatus: reviewPendingDetectionStatus }),
    loadWorkbenchSummaryReportRows(previewPageSize)
  ])
  previewRows.samplingPlan = planResult.records || []
  previewRows.sampling = samplingResult.records || []
  previewRows.sampleLogin = loginResult.records || []
  previewRows.detection = detectionResult.records || []
  previewRows.review = reviewResult.records || []
  previewRows.report = summaryReportRows
}

function getWorkbenchSummaryTypes() {
  if (isStaffRole(currentUser.value)) {
    return [SUMMARY_TYPE_DAILY]
  }
  return [SUMMARY_TYPE_DAILY, SUMMARY_TYPE_WEEKLY, SUMMARY_TYPE_HALF_MONTHLY]
}

async function loadWorkbenchSummaryReportRows(pageSize) {
  const size = Number(pageSize || 12)
  const results = await Promise.all(
    getWorkbenchSummaryTypes().map((summaryType) => fetchSummaryReportsApi({
      pageNum: 1,
      pageSize: size,
      summaryType
    }))
  )
  return results
    .flatMap((result) => Array.isArray(result.records) ? result.records : [])
    .sort(compareSummaryReportRows)
    .slice(0, size)
}

function compareSummaryReportRows(left, right) {
  const rightTime = Date.parse(right?.periodStart || right?.latestSamplingTime || '')
  const leftTime = Date.parse(left?.periodStart || left?.latestSamplingTime || '')
  const safeRightTime = Number.isFinite(rightTime) ? rightTime : 0
  const safeLeftTime = Number.isFinite(leftTime) ? leftTime : 0
  if (safeRightTime !== safeLeftTime) {
    return safeRightTime - safeLeftTime
  }
  return String(left?.reportName || '').localeCompare(String(right?.reportName || ''))
}

async function loadDictOptions() {
  const [weatherItems, storageItems] = await Promise.all([
    fetchDictItemsApi('weather_condition'),
    fetchDictItemsApi('storage_condition')
  ])
  weatherOptions.value = normalizeDictOptions(weatherItems)
  storageConditionOptions.value = normalizeDictOptions(storageItems)
}

async function loadDetectionConfigOptions() {
  if (detectionTypes.value.length && detectionParameters.value.length && detectionMethods.value.length) {
    return
  }
  const [typeResult, parameterResult, methodResult] = await Promise.all([
    fetchDetectionTypesApi({ pageNum: 1, pageSize: 500, enabled: 1 }),
    fetchDetectionParametersApi({ pageNum: 1, pageSize: 500 }),
    fetchDetectionMethodOptionsApi()
  ])
  detectionTypes.value = typeResult.records || []
  detectionParameters.value = parameterResult.records || []
  detectionMethods.value = Array.isArray(methodResult) ? methodResult : []
}

async function loadFlowOptions() {
  if (reviewFlowOptions.value.length) {
    return
  }
  const result = await fetchFlowConfigOptionsApi({ flowType: FLOW_TYPE_REVIEW })
  reviewFlowOptions.value = Array.isArray(result) ? result : []
}

function handlePreviewCardClick(key, row) {
  if (key === 'samplingPlan') {
    selectedSamplingPlan.value = row
    samplingPlanDetailVisible.value = true
    return
  }
  openWorkbenchAction(key, row)
}

async function dispatchSamplingPlanFromWorkbench(row) {
  if (!row || dispatchSubmitting.value) {
    return
  }
  await loadSamplers(true)
  resetDispatchForm()
  const samplerIds = resolveRowSamplerOptionIds(row)
  dispatchForm.planId = row.id
  dispatchForm.samplingTime = row.startTime || nowDateTimeText()
  ensureSelectedSamplerOptions(samplerIds, row.samplerName || row.sampler_name)
  handleDispatchSamplerChange(samplerIds)
  dispatchDialogVisible.value = true
}

async function submitDispatchForm() {
  if (dispatchSubmitting.value) {
    return
  }
  if (!dispatchForm.planId || !dispatchForm.samplerIds.length || !dispatchForm.samplerId || !dispatchForm.samplerName) {
    ElMessage.warning('派发任务前必须指定采样员')
    return
  }
  dispatchSubmitting.value = true
  dispatchingPlanId.value = dispatchForm.planId
  try {
    await dispatchSamplingPlanApi({
      planId: dispatchForm.planId,
      samplingTime: dispatchForm.samplingTime || nowDateTimeText(),
      samplerIds: normalizeSamplerIdList(dispatchForm.samplerIds),
      samplerId: dispatchForm.samplerId,
      samplerName: dispatchForm.samplerName
    })
    dispatchDialogVisible.value = false
    ElMessage.success('采样计划已派发，并已同步生成采样任务。')
    samplingPlanDetailVisible.value = false
    await Promise.all([refreshDashboard(), loadWorkbenchPreviewRows()])
  } finally {
    dispatchSubmitting.value = false
    dispatchingPlanId.value = null
  }
}

async function openWorkbenchAction(key, preferredRow = null) {
  activeAction.value = key
  workbenchDialogVisible.value = true
  actionLoading.value = true
  try {
    await loadActionRows(key)
    const selected = preferredRow
      ? actionRows.value.find((row) => String(getActionRowKey(row)) === String(getActionRowKey(preferredRow))) || preferredRow
      : null
    if (selected) {
      await selectActionRow(selected)
    } else if (actionRows.value.length) {
      await selectActionRow(actionRows.value[0])
    } else {
      activeRow.value = null
    }
  } finally {
    actionLoading.value = false
  }
}

async function loadActionRows(key) {
  previewData.value = null
  previewError.value = ''
  if (key === 'samplingPlan') {
    const result = await fetchSamplingPlansApi({ pageNum: 1, pageSize: 30, planStatus: 'ACTIVE' })
    actionRows.value = result.records || []
    return
  }
  if (key === 'sampling') {
    const result = await fetchSamplingTasksApi({ pageNum: 1, pageSize: 30, taskStatus: 'PENDING' })
    actionRows.value = result.records || []
    return
  }
  if (key === 'sampleLogin') {
    await Promise.all([loadDetectionConfigOptions(), loadFlowOptions()])
    const result = await fetchSamplingTasksApi({
      pageNum: 1,
      pageSize: 30,
      sampleRegisterStatus: 'UNREGISTERED'
    })
    actionRows.value = result.records || []
    return
  }
  if (key === 'detectionSplit' || key === 'detection') {
    const result = await fetchDetectionItemsApi({ pageNum: 1, pageSize: 30, itemStatus: waitDetectDetectionStatus })
    actionRows.value = result.records || []
    return
  }
  if (key === 'review') {
    const result = await fetchDetectionsApi({ pageNum: 1, pageSize: 30, detectionStatus: reviewPendingDetectionStatus })
    actionRows.value = result.records || []
    return
  }
  if (key === 'report') {
    actionRows.value = await loadWorkbenchSummaryReportRows(30)
  }
}

async function selectActionRow(row) {
  activeRow.value = row
  if (activeAction.value === 'samplingPlan') {
    // 采样计划暂无详情表单
  } else if (activeAction.value === 'sampling') {
    await openSamplingForm(row)
  } else if (activeAction.value === 'sampleLogin') {
    await openSampleLoginForm(row)
  } else if (activeAction.value === 'detectionSplit' || activeAction.value === 'detection') {
    openResultForm(row)
  } else if (activeAction.value === 'review') {
    await openReviewForm(row)
  } else if (activeAction.value === 'report') {
    await openReportPreview(row)
  }
}

function getActionRowKey(row) {
  return row?.id || row?.reportKey || row?.taskId || row?.recordId || row?.sampleNo || JSON.stringify(row)
}

function isActionRowActive(row) {
  return String(getActionRowKey(row)) === String(getActionRowKey(activeRow.value))
}

function getActionRowTitle(row) {
  if (activeAction.value === 'samplingPlan') {
    return row?.planName || row?.pointName || '-'
  }
  if (activeAction.value === 'sampling' || activeAction.value === 'sampleLogin') {
    return formatPlanNameWithDate(row)
  }
  if (activeAction.value === 'detectionSplit' || activeAction.value === 'detection' || activeAction.value === 'review') {
    return row?.sampleNo || row?.parameterName || '-'
  }
  if (activeAction.value === 'report') {
    return row?.reportName || row?.sampleNo || '-'
  }
  return '-'
}

function getActionRowMeta(row) {
  if (activeAction.value === 'samplingPlan') {
    return `${row?.pointName || '-'} / ${row?.cycleType || '-'}`
  }
  if (activeAction.value === 'sampling' || activeAction.value === 'sampleLogin') {
    return `${row?.pointName || '-'}`
  }
  if (activeAction.value === 'detectionSplit' || activeAction.value === 'detection') {
    return `${row?.parameterName || '-'} / ${row?.methodName || '-'}`
  }
  if (activeAction.value === 'review') {
    return `${row?.detectionTypeName || '-'} / ${row?.detectorName || '-'}`
  }
  if (activeAction.value === 'report') {
    return `${row?.summaryTypeLabel || row?.summaryType || '-'} / ${row?.periodLabel || '-'}`
  }
  return ''
}

function getPreviewRowTitle(key, row) {
  if (key === 'samplingPlan') {
    return row?.planName || row?.pointName || '-'
  }
  if (key === 'sampleLogin') {
    return formatPlanNameWithDate(row)
  }
  if (key === 'sampling') {
    return formatPlanNameWithDate(row)
  }
  if (key === 'detection') {
    return buildSampleContextTitle(row, row?.parameterName)
  }
  if (key === 'review') {
    return buildSampleContextTitle(row, row?.detectionTypeName)
  }
  if (key === 'report') {
    return row?.reportName || row?.summaryTypeLabel || '-'
  }
  return '-'
}

function buildSampleContextTitle(row, suffix = '') {
  const planName = firstNonBlank(row?.planName, row?.pointName, row?.monitoringPointName)
  const sampleType = firstNonBlank(row?.sampleTypeLabel, getEnumLabel(sampleTypeLabelMap, row?.sampleType), row?.sampleType)
  return `计划名称：${planName || '未填'}      样品类型：${sampleType || '未填'}`
}

function firstNonBlank(...values) {
  return values
    .map((value) => String(value ?? '').trim())
    .find((value) => value && value !== '-') || ''
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  if (isNaN(date.getTime())) return dateStr
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function formatSamplingTimeShort(dateStr) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  if (isNaN(date.getTime())) return ''
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${month}-${day} ${hour}:${minute}`
}

function formatPlanNameWithDate(row) {
  const planName = row?.planName || row?.pointName || '-'
  const dateLabel = formatSamplingTimeShort(row?.samplingTime)
  return dateLabel ? `${planName}  ${dateLabel}` : planName
}

function getPreviewRowMeta(key, row) {
  if (key === 'samplingPlan') {
    return `${row?.pointName || '-'} / ${row?.samplerName || '-'}`
  }
  if (key === 'sampleLogin') {
    return row?.samplerName || ''
  }
  if (key === 'sampling') {
    return ''
  }
  if (key === 'detection') {
    return ''
  }
  if (key === 'review') {
    return firstNonBlank(row?.detectorName) ? `检测人员：${row.detectorName}` : ''
  }
  if (key === 'report') {
    return `${row?.summaryTypeLabel || row?.summaryType || '-'} / ${row?.periodLabel || '-'}`
  }
  return ''
}

function getPreviewFields(key, row) {
  if (key === 'samplingPlan') {
    return [
      { label: '计划状态', value: getEnumLabel(planStatusLabelMap, row?.planStatus) },
      { label: '周期类型', value: getEnumLabel(cycleTypeLabelMap, row?.cycleType) },
      { label: '开始时间', value: formatDate(row?.startTime) }
    ]
  }
  if (key === 'report') {
    return [
      { label: '报表类型', value: row?.summaryTypeLabel || row?.summaryType },
      { label: '周期范围', value: row?.periodLabel },
      { label: '报表状态', value: row?.reportStatusLabel || row?.reportStatus }
    ]
  }
  if (key === 'sampling') {
    return [
      { label: '任务状态', value: row?.taskStatus },
      { label: '计划时间', value: formatDate(row?.samplingTime) }
    ]
  }
  if (key === 'sampleLogin') {
    return [
      { label: '任务编号', value: row?.taskNo },
      { label: '计划时间', value: formatDate(row?.samplingTime) }
    ]
  }
  if (key === 'detection') {
    return [
      { label: '检测方法', value: row?.methodName },
      { label: '检测人员', value: row?.detectorName },
      { label: '更新时间', value: row?.updatedTime }
    ]
  }
  if (key === 'review') {
    return [
      { label: '检测套餐', value: row?.detectionTypeName },
      { label: '参数进度', value: `${row?.completedCount ?? 0}/${row?.parameterCount ?? 0}` },
      { label: '更新时间', value: row?.updatedTime }
    ]
  }
  if (key === 'report') {
    return [
      { label: '报告状态', value: row?.reportStatus },
      { label: '生成时间', value: row?.generatedTime }
    ]
  }
  return []
}

function getPreviewDisplayRowMeta(key, row) {
  return getPreviewRowMeta(key, row)
}

function getPreviewDisplayFields(key, row) {
  if (key === 'report') {
    return [
      {
        label: '报表状态',
        value: row?.reportStatusLabel || row?.reportStatus,
        statusClass: resolveSummaryReportStatusClass(row?.reportStatus)
      },
      { label: '报表类型', value: row?.summaryTypeLabel || row?.summaryType },
      { label: '周期范围', value: row?.periodLabel }
    ]
  }
  if (key === 'samplingPlan') {
    return [
      {
        label: '计划状态',
        value: getEnumLabel(planStatusLabelMap, row?.planStatus),
        statusType: 'planStatus',
        statusValue: row?.planStatus
      },
      { label: '周期', value: getEnumLabel(cycleTypeLabelMap, row?.cycleType) },
      { label: '开始时间', value: formatDate(row?.startTime) }
    ]
  }
  if (key === 'sampling') {
    return [
      {
        label: '任务状态',
        value: getEnumLabel(taskStatusLabelMap, row?.taskStatus),
        statusType: 'taskStatus',
        statusValue: row?.taskStatus
      },
      { label: '计划时间', value: formatDate(row?.samplingTime) },
      { label: '采样人员', value: row?.samplerName }
    ]
  }
  if (key === 'sampleLogin') {
    return [
      { label: '任务编号', value: row?.taskNo },
      {
        label: '登记状态',
        value: getEnumLabel(sampleRegisterStatusLabelMap, row?.sampleRegisterStatus || unregisteredSampleRegisterStatus),
        statusClass: row?.sampleRegisterStatus && row.sampleRegisterStatus !== unregisteredSampleRegisterStatus ? 'success' : 'warning'
      },
      { label: '计划时间', value: formatDate(row?.samplingTime) }
    ]
  }
  if (key === 'detection') {
    return [
      {
        label: '检测状态',
        value: getEnumLabel(detectionStatusLabelMap, row?.itemStatus),
        statusType: 'detectionStatus',
        statusValue: row?.itemStatus
      },
      { label: '检测方法', value: row?.methodName },
      { label: '检测人员', value: row?.detectorName }
    ]
  }
  if (key === 'review') {
    return [
      {
        label: '审核状态',
        value: getEnumLabel(detectionStatusLabelMap, reviewPendingDetectionStatus),
        statusType: 'detectionStatus',
        statusValue: reviewPendingDetectionStatus
      },
      { label: '参数进度', value: `${row?.completedCount ?? 0}/${row?.parameterCount ?? 0}` },
      { label: '更新时间', value: row?.updatedTime }
    ]
  }
  if (key === 'report') {
    return [
      {
        label: '报告状态',
        value: getEnumLabel(reportStatusLabelMap, row?.reportStatus),
        statusType: 'reportStatus',
        statusValue: row?.reportStatus
      },
      { label: '生成时间', value: row?.generatedTime }
    ]
  }
  return getPreviewFields(key, row)
}

function getVisiblePreviewFields(key, row) {
  return getPreviewDisplayFields(key, row).filter((field) => field?.statusType || field?.statusClass || firstNonBlank(field?.value))
}

async function openSamplingForm(row) {
  const detail = await fetchSamplingTaskDetailApi(row.id)
  const task = detail || row
  samplingTask.value = task
}

async function openSampleLoginForm(task) {
  loginTask.value = task
  loginForm.taskId = task.id
  loginForm.sampleNo = task.sampleNo || ''
  loginForm.pointId = task.pointId || null
  loginForm.pointName = task.pointName || ''
  loginForm.sampleType = task.sampleType || ''
  loginForm.sampleSourceMethod = samplingSampleSourceMethod
  loginForm.samplingTime = task.samplingTime || ''
  loginForm.samplerId = task.samplerId || null
  loginForm.samplerName = task.samplerName || ''
  loginForm.weather = task.weather || ''
  loginForm.storageCondition = ''
  loginForm.remark = task.remark || ''
  const flow = reviewFlowOptions.value.find((item) => item.defaultFlag) || reviewFlowOptions.value[0]
  loginForm.reviewFlowId = flow?.id || null
  loginForm.reviewFlowName = flow?.flowName || ''
  const type = loginDetectionTypeOptions.value[0]
  handleLoginDetectionTypeChange(type?.id || null)
}

function handleReviewFlowChange(flowId) {
  const flow = reviewFlowOptions.value.find((item) => String(item.id) === String(flowId))
  loginForm.reviewFlowName = flow?.flowName || ''
}

function formatDetectionTypeLabel(item) {
  const sampleTypeLabel = item?.sampleType ? getEnumLabel(sampleTypeLabelMap, item.sampleType) : '未绑定样品类型'
  return `${item?.typeName || '-'} / ${sampleTypeLabel}`
}

function handleLoginDetectionTypeChange(typeId) {
  const type = detectionTypes.value.find((item) => String(item.id) === String(typeId || ''))
  loginForm.detectionTypeId = type?.id || null
  loginForm.detectionTypeName = type?.typeName || ''
  loginForm.detectionItems = type?.typeName || ''
  loginForm.detectionConfigItems = buildLoginDetectionConfigItems(type)
}

function parseIdList(value) {
  return String(value || '').split(',').map((item) => item.trim()).filter(Boolean)
}

function parseBindingJson(value) {
  try {
    const list = JSON.parse(String(value || '[]'))
    return Array.isArray(list) ? list : []
  } catch {
    return []
  }
}

function buildLoginDetectionConfigItems(type) {
  if (!type) {
    return []
  }
  const parameterMap = new Map(detectionParameters.value.map((item) => [String(item.id), item]))
  const bindingMap = new Map()
  parseBindingJson(type.parameterMethodBindings).forEach((item) => {
    const parameterId = String(item?.parameterId || '')
    const methodId = Array.isArray(item?.methodIds) ? item.methodIds[0] : null
    if (parameterId) {
      bindingMap.set(parameterId, methodId == null ? '' : String(methodId))
    }
  })
  return parseIdList(type.parameterIds).map((parameterId) => {
    const parameter = parameterMap.get(String(parameterId))
    if (!parameter) {
      return null
    }
    const methodOptions = detectionMethods.value.filter((item) => String(item.parameterId) === String(parameterId) && item.enabled === 1)
    const defaultMethodId = bindingMap.get(parameterId) || methodOptions[0]?.id || ''
    const method = methodOptions.find((item) => String(item.id) === String(defaultMethodId)) || methodOptions[0]
    return {
      parameterId: String(parameter.id),
      parameterName: parameter.parameterName || '',
      unit: parameter.unit || '',
      standardMin: parameter.standardMin,
      standardMax: parameter.standardMax,
      referenceStandard: parameter.referenceStandard || '',
      methodId: method?.id ? String(method.id) : '',
      methodName: method?.methodName || '',
      sampleVolume: method?.sampleVolume || method?.sample_volume || ''
    }
  }).filter(Boolean)
}

async function submitSampleLogin() {
  if (!loginForm.taskId || !loginForm.pointName || !loginForm.sampleType || !loginForm.detectionTypeId || !loginForm.reviewFlowId || !loginForm.samplingTime) {
    ElMessage.warning('请完整填写样品登录信息')
    return
  }
  if (!loginForm.detectionConfigItems.length || loginForm.detectionConfigItems.some((item) => !item.methodId)) {
    ElMessage.warning('检测套餐缺少检测参数或检测方法')
    return
  }
  submitting.value = true
  try {
    // 验证任务状态，防止长时间未刷新导致重复登录
    const taskDetail = await fetchSamplingTaskDetailApi(loginForm.taskId)
    if (taskDetail.sampleRegisterStatus === 'REGISTERED') {
      ElMessage.warning('该任务已被其他人完成样品登录，请刷新页面')
      await reloadActiveAction()
      return
    }
    if (taskDetail.taskStatus === 'ABANDONED') {
      ElMessage.warning('该任务已被废弃，请刷新页面')
      await reloadActiveAction()
      return
    }
    const sample = await loginSampleApi({
      ...loginForm,
      detectionConfigItems: loginForm.detectionConfigItems
    })
    ElMessage.success(`样品登录完成：${sample?.sampleNo || loginForm.sampleNo || '-'}`)
    await reloadActiveAction()
  } finally {
    submitting.value = false
  }
}

function formatTaskLabel(task) {
  if (!task) {
    return '-'
  }
  return task.planName || task.pointName || '-'
}

function openResultForm(row) {
  resultForm.id = row.id
  resultForm.recordId = row.recordId
  resultForm.sampleId = row.sampleId
  resultForm.sampleNo = row.sampleNo || ''
  resultForm.detectionTypeId = row.detectionTypeId
  resultForm.detectionTypeName = row.detectionTypeName || ''
  resultForm.parameterId = row.parameterId
  resultForm.parameterName = row.parameterName || ''
  resultForm.methodName = row.methodName || ''
  resultForm.methodBasis = row.methodBasis || ''
  resultForm.standardMin = row.standardMin
  resultForm.standardMax = row.standardMax
  resultForm.unit = row.unit || ''
  resultForm.referenceStandard = row.referenceStandard || ''
  resultForm.detectorName = row.detectorName || ''
  resultForm.resultValue = row.resultValue == null ? null : String(row.resultValue)
  resultForm.abnormalRemark = row.abnormalRemark || ''
  resultForm.remark = row.remark || ''
  resultForm.itemStatus = row.itemStatus || ''
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

function isResultValueAbnormal(item) {
  if (!item || item.resultValue == null || item.resultValue === '') {
    return false
  }
  const value = Number(item.resultValue)
  if (!Number.isFinite(value)) {
    return false
  }
  if (item.standardMin != null && value < Number(item.standardMin)) {
    return true
  }
  return item.standardMax != null && value > Number(item.standardMax)
}

function handleDetectionResultInput(value) {
  resultForm.resultValue = normalizeDetectionResultInput(value)
}

function normalizeDetectionResultInput(value) {
  const text = String(value ?? '').replace(/[^\d.]/g, '')
  const [integerPart, ...decimalParts] = text.split('.')
  if (!decimalParts.length) {
    return integerPart
  }
  return `${integerPart}.${decimalParts.join('')}`
}

async function submitDetectionResult() {
  if (!resultForm.sampleId || !resultForm.detectionTypeId || !resultForm.recordId || !resultForm.id) {
    ElMessage.warning('当前检测子流程缺少必要信息')
    return
  }
  if (resultForm.resultValue == null || resultForm.resultValue === '') {
    ElMessage.warning('请填写检测结果')
    return
  }
  if (isResultValueAbnormal(resultForm)) {
    ElMessage.warning(`检测结果存在异常，禁止录入：${formatStandardRange(resultForm.standardMin, resultForm.standardMax, resultForm.unit)}`)
    return
  }
  submitting.value = true
  try {
    await submitDetectionApi({
      recordId: resultForm.recordId,
      itemId: resultForm.id,
      sampleId: resultForm.sampleId,
      detectionTypeId: resultForm.detectionTypeId,
      detectionTypeName: resultForm.detectionTypeName,
      abnormalRemark: resultForm.abnormalRemark,
      remark: resultForm.remark,
      items: [{
        parameterId: resultForm.parameterId,
        parameterName: resultForm.parameterName,
        standardMin: resultForm.standardMin,
        standardMax: resultForm.standardMax,
        resultValue: Number(resultForm.resultValue),
        unit: resultForm.unit
      }]
    })
    ElMessage.success('检测结果已提交')
    await reloadActiveAction()
  } finally {
    submitting.value = false
  }
}

async function openReviewForm(row) {
  const detail = await fetchDetectionDetailApi(row.id || row.detectionRecordId)
  const items = detail?.items || []
  reviewForm.detectionRecordId = row.id || row.detectionRecordId
  reviewForm.sampleNo = row.sampleNo || detail?.record?.sampleNo || ''
  reviewForm.detectionTypeName = row.detectionTypeName || detail?.record?.detectionTypeName || ''
  reviewForm.reviewRemark = ''
  reviewForm.items = items.map((item) => ({
    id: item.id,
    parameterName: item.parameterName || '',
    methodName: item.methodName || '',
    standardMin: item.standardMin,
    standardMax: item.standardMax,
    unit: item.unit || '',
    resultValue: item.resultValue == null ? null : Number(item.resultValue),
    itemStatus: item.itemStatus || '',
    reviewResultDraft: item.itemStatus === approvedDetectionStatus ? approvedReviewResult : '',
    rejectReasonDraft: '',
    reviewRemarkDraft: ''
  }))
}

function setAllReviewResult(result) {
  reviewForm.items.forEach((item) => {
    if (item.itemStatus === reviewPendingDetectionStatus) {
      item.reviewResultDraft = result
      if (result === rejectedReviewResult && !item.rejectReasonDraft) {
        item.rejectReasonDraft = '审核不通过，请重新检测并提交结果'
      }
    }
  })
}

async function submitReviewDecision() {
  const pendingItems = reviewForm.items.filter((item) => item.itemStatus === reviewPendingDetectionStatus)
  if (!pendingItems.length) {
    ElMessage.warning('当前主流程下没有待审核子流程')
    return
  }
  const undecidedItem = pendingItems.find((item) => !item.reviewResultDraft)
  if (undecidedItem) {
    ElMessage.warning(`请先选择 ${undecidedItem.parameterName || '检测参数'} 的审核结果`)
    return
  }
  const missingRejectReason = pendingItems.find((item) => item.reviewResultDraft === rejectedReviewResult && !item.rejectReasonDraft)
  if (missingRejectReason) {
    ElMessage.warning(`请填写 ${missingRejectReason.parameterName || '检测参数'} 的驳回原因`)
    return
  }
  submitting.value = true
  try {
    const anyRejected = pendingItems.some((item) => item.reviewResultDraft === rejectedReviewResult)
    await submitReviewApi({
      detectionRecordId: reviewForm.detectionRecordId,
      reviewResult: anyRejected ? rejectedReviewResult : approvedReviewResult,
      reviewRemark: reviewForm.reviewRemark,
      items: pendingItems.map((item) => ({
        itemId: item.id,
        reviewResult: item.reviewResultDraft,
        rejectReason: item.rejectReasonDraft,
        reviewRemark: item.reviewRemarkDraft
      }))
    })
    ElMessage.success(anyRejected ? '审核已提交，主流程已退回' : '审核已提交，主流程全部通过')
    await reloadActiveAction()
  } finally {
    submitting.value = false
  }
}

async function openReportPreview(row) {
  previewData.value = null
  previewError.value = ''
  try {
    previewData.value = await previewSummaryReportApi(buildSummaryPreviewParams(row))
  } catch (error) {
    previewError.value = error?.message || '报告预览失败'
  }
}

function buildSummaryPreviewParams(row) {
  const params = {
    summaryType: row?.summaryType || SUMMARY_TYPE_DAILY,
    regionName: row?.regionName || '',
    periodStart: row?.periodStart,
    periodEnd: row?.periodEnd
  }
  if (params.summaryType === SUMMARY_TYPE_DAILY) {
    params.dailyReportType = row?.dailyReportType || ''
  }
  return params
}

function resolveSummaryPreviewComponent(previewMode) {
  if (previewMode === 'DAILY_INTERNAL_TEMPLATE') {
    return DailyInternalSummaryTemplate
  }
  if (previewMode === 'DAILY_EXTERNAL_TEMPLATE') {
    return DailyExternalSummaryTemplate
  }
  if (previewMode === 'WEEKLY_FACTORY_TEMPLATE') {
    return WeeklyFactorySummaryTemplate
  }
  if (previewMode === 'HALF_MONTHLY_TERMINAL_TEMPLATE') {
    return HalfMonthlyTerminalSummaryTemplate
  }
  return null
}

function resolveSummaryReportStatusClass(status) {
  if (status === 'COMPLETE') {
    return 'success'
  }
  if (status === 'PARTIAL') {
    return 'warning'
  }
  return 'info'
}

async function reloadActiveAction() {
  await Promise.all([refreshDashboard(), loadWorkbenchPreviewRows(), loadActionRows(activeAction.value)])
  if (actionRows.value.length) {
    await selectActionRow(actionRows.value[0])
  } else {
    activeRow.value = null
  }
}

function resetWorkbenchDialog() {
  activeAction.value = ''
  actionRows.value = []
  activeRow.value = null
  previewData.value = null
  previewError.value = ''
}

function syncCurrentUser(event) {
  currentUser.value = event?.detail || getUser() || {}
}

onMounted(async () => {
  currentUser.value = getUser() || {}
  window.addEventListener('yx-lab-user-updated', syncCurrentUser)
  loading.value = true
  try {
    await Promise.all([refreshDashboard(), loadWorkbenchPreviewRows(), loadDictOptions()])
  } catch (error) {
    dashboard.value = {}
    console.warn('工作台数据加载失败，已使用空数据兜底。', error)
  } finally {
    loading.value = false
  }
})

onUnmounted(() => {
  window.removeEventListener('yx-lab-user-updated', syncCurrentUser)
})
</script>

<style scoped>
.dashboard-page {
  min-height: 100%;
  gap: 16px;
}

.workbench-hero {
  display: grid;
  grid-template-columns: minmax(180px, 260px) minmax(360px, 1fr) auto;
  gap: 22px;
  align-items: center;
  padding: 18px 22px;
  border: 1px solid rgba(47, 111, 159, 0.18);
  background:
    radial-gradient(circle at 5% 0%, rgba(47, 111, 159, 0.16), transparent 34%),
    linear-gradient(135deg, #ffffff 0%, #f6fbff 55%, #edf6ff 100%);
}

.workbench-hero h3 {
  margin: 0;
  color: var(--text-main);
  font-size: 28px;
}

.workbench-hero__flow {
  min-width: 0;
}

.workbench-hero__flow > span {
  display: block;
  margin-bottom: 10px;
  color: var(--text-sub);
  font-size: 14px;
}

.workbench-hero__flow-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.workbench-hero__flow-chips button {
  min-height: 34px;
  padding: 0 14px;
  border: 1px solid rgba(22, 119, 255, 0.18);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.74);
  color: #173b67;
  font: inherit;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: border-color 0.18s ease, color 0.18s ease, box-shadow 0.18s ease;
}

.workbench-hero__flow-chips button:hover {
  border-color: rgba(22, 119, 255, 0.34);
  color: var(--brand);
  box-shadow: 0 8px 18px rgba(17, 54, 99, 0.08);
}

.workbench-hero__flow-chips button:disabled {
  cursor: default;
  opacity: 1;
}

.workbench-hero__stats {
  display: grid;
  grid-template-columns: repeat(2, 128px);
  gap: 12px;
}

.workbench-hero__stats span {
  display: grid;
  gap: 8px;
  min-height: 92px;
  place-items: center;
  padding: 14px 12px;
  border: 1px solid rgba(214, 225, 241, 0.92);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.76);
  box-shadow: 0 12px 24px rgba(17, 54, 99, 0.05);
}

.workbench-hero__stats strong {
  color: #1769ff;
  font-size: 30px;
  line-height: 1;
}

.workbench-hero__stats em {
  color: #3d5878;
  font-size: 13px;
  font-style: normal;
}

.workbench-guide button,
.workbench-row-card,
.todo-detail-card {
  border: 0;
  font: inherit;
  cursor: pointer;
}

.todo-preview-panel {
  padding: 18px;
}

.panel-head {
  margin-bottom: 14px;
}

.section-title {
  margin: 0;
  font-size: 18px;
}

.panel-head p {
  margin: 4px 0 0;
  color: var(--text-sub);
  font-size: 13px;
}

.workbench-guide {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.workbench-guide button {
  min-width: 128px;
  padding: 12px 18px;
  border: 1px solid rgba(22, 119, 255, 0.18);
  border-radius: 12px;
  background: var(--brand-soft);
  color: var(--brand);
  font-weight: 800;
}

.todo-detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.todo-detail-panel {
  display: flex;
  flex-direction: column;
  height: 460px;
  min-height: 290px;
  overflow: hidden;
  padding: 16px;
  border: 1px solid rgba(214, 225, 241, 0.86);
}

.todo-detail-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0 2px 12px;
  border-bottom: 1px solid #edf2f7;
}

.todo-detail-panel__head h3 {
  margin: 0;
  color: var(--text-main);
  font-size: 16px;
}

.todo-detail-panel__head > strong {
  min-width: 38px;
  padding: 6px 10px;
  border-radius: 999px;
  background: var(--brand-soft);
  color: var(--brand);
  font-size: 18px;
  line-height: 1;
  text-align: center;
}

.todo-detail-list {
  display: grid;
  align-content: start;
  grid-auto-rows: max-content;
  gap: 10px;
  flex: 1;
  min-height: 0;
  margin-top: 12px;
  overflow-y: auto;
  padding-right: 4px;
  scrollbar-width: thin;
  scrollbar-color: rgba(100, 116, 139, 0.32) transparent;
}

.todo-detail-list::-webkit-scrollbar {
  width: 6px;
}

.todo-detail-list::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(100, 116, 139, 0.28);
}

.todo-detail-list::-webkit-scrollbar-track {
  background: transparent;
}

.todo-detail-card {
  position: relative;
  display: grid;
  align-content: center;
  gap: 9px;
  height: 112px;
  padding: 13px 14px 13px 18px;
  border: 1px solid rgba(214, 225, 241, 0.92);
  border-radius: 12px;
  background: #ffffff;
  text-align: left;
  cursor: pointer;
  box-shadow: 0 8px 18px rgba(17, 54, 99, 0.035);
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.todo-detail-card--samplingPlan,
.todo-detail-card--sampling {
  padding-right: 88px;
}

.todo-detail-card::before {
  content: "";
  position: absolute;
  left: 0;
  top: 12px;
  bottom: 12px;
  width: 4px;
  border-radius: 0 999px 999px 0;
  background: var(--brand);
}

.todo-detail-card--sampleLogin::before {
  background: var(--success);
}

.todo-detail-card--samplingPlan::before {
  background: var(--brand);
}

.todo-detail-card--detection::before,
.todo-detail-card--review::before {
  background: var(--warning);
}

.todo-detail-card--report::before {
  background: #64748b;
}

.todo-detail-card:hover {
  border-color: rgba(22, 119, 255, 0.35);
  box-shadow: 0 12px 24px rgba(17, 54, 99, 0.08);
  transform: translateY(-1px);
}

.todo-detail-card:focus-visible {
  outline: 2px solid rgba(22, 119, 255, 0.38);
  outline-offset: 2px;
}

.todo-detail-card__action {
  position: absolute;
  right: 12px;
  bottom: 12px;
}

.todo-detail-card__action--center {
  top: 50%;
  bottom: auto;
  transform: translateY(-50%);
}

.todo-detail-card__main {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 4px;
}

.todo-detail-card__main span {
  overflow: hidden;
  color: var(--text-main);
  font-size: 15px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.todo-detail-card__main p {
  overflow: hidden;
  margin: 0;
  color: var(--text-sub);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.todo-detail-card__fields {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  min-width: 0;
}

.todo-detail-card__fields span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  min-width: 0;
  max-width: 100%;
  padding: 0;
}

.todo-detail-card--sampling .todo-detail-card__fields,
.todo-detail-card--detection .todo-detail-card__fields,
.todo-detail-card--review .todo-detail-card__fields {
  gap: 8px 20px;
}

.todo-detail-card--sampling .todo-detail-card__fields span,
.todo-detail-card--detection .todo-detail-card__fields span,
.todo-detail-card--review .todo-detail-card__fields span {
  gap: 6px;
}

.todo-detail-card__fields em {
  color: var(--text-sub);
  font-size: 12px;
  font-style: normal;
  white-space: nowrap;
}

.todo-detail-card__fields strong {
  overflow: hidden;
  color: var(--text-main);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.todo-detail-card__fields .status-chip {
  min-width: 0;
  min-height: 22px;
  padding: 0 9px;
  font-size: 12px;
  line-height: 22px;
}

.todo-detail-empty {
  display: grid;
  flex: 1;
  min-height: 160px;
  place-items: center;
  color: var(--text-sub);
}

.plan-detail {
  display: grid;
  gap: 16px;
}

.plan-detail__title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 14px;
  border-bottom: 1px solid #edf2f7;
}

.plan-detail__title > strong {
  min-width: 0;
  overflow: hidden;
  color: var(--text-main);
  font-size: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.plan-detail__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.plan-detail__grid > div,
.plan-detail__remark {
  display: grid;
  gap: 6px;
  min-width: 0;
  padding: 12px;
  border: 1px solid #edf2f7;
  border-radius: 8px;
  background: #f8fafc;
}

.plan-detail__grid span,
.plan-detail__remark span {
  color: var(--text-sub);
  font-size: 12px;
}

.plan-detail__grid strong,
.plan-detail__remark p {
  min-width: 0;
  overflow-wrap: anywhere;
  margin: 0;
  color: var(--text-main);
  font-size: 14px;
  font-weight: 700;
}

:deep(.dispatch-plan-dialog .el-dialog__body) {
  padding-bottom: 0;
}

:deep(.dispatch-plan-dialog .el-dialog__footer) {
  padding-top: 8px;
  padding-bottom: 16px;
}

:deep(.el-dialog.dispatch-plan-dialog),
:deep(.dispatch-plan-dialog) {
  min-height: auto;
}

.workbench-action-dialog :deep(.el-dialog__body) {
  padding-top: 10px;
}

:deep(.el-dialog.workbench-action-dialog) {
  max-width: calc(100vw - 32px);
}

.workbench-dialog {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 16px;
  min-height: 520px;
}

.workbench-dialog--sampling {
  grid-template-columns: 322px minmax(0, 1fr);
  height: 430px;
  min-height: 0;
}

.workbench-dialog--detection {
  grid-template-columns: 320px minmax(0, 1fr);
  width: 100%;
  height: 650px;
  min-height: 0;
}

.workbench-dialog--sample-login {
  grid-template-columns: 320px minmax(0, 1fr);
  width: 100%;
  height: 560px;
  min-height: 0;
}

.workbench-dialog--report {
  grid-template-columns: 320px minmax(0, 1fr);
  width: 100%;
  height: 650px;
  min-height: 0;
}

.workbench-dialog__side {
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 12px;
  border: 1px solid rgba(214, 225, 241, 0.9);
  border-radius: 14px;
  background: #f8fbff;
}

.workbench-dialog__side-head {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
  color: var(--text-main);
  font-weight: 800;
}

.workbench-row-list {
  display: grid;
  align-content: start;
  gap: 8px;
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: rgba(100, 116, 139, 0.32) transparent;
}

.workbench-row-list::-webkit-scrollbar {
  width: 6px;
}

.workbench-row-list::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(100, 116, 139, 0.28);
}

.workbench-row-list::-webkit-scrollbar-track {
  background: transparent;
}

.workbench-dialog--detection .workbench-row-list {
  max-height: 582px;
  padding-right: 4px;
}

.workbench-dialog--sample-login .workbench-row-list {
  max-height: 492px;
  padding-right: 4px;
}

.workbench-dialog--report .workbench-row-list {
  max-height: 582px;
  padding-right: 4px;
}

.workbench-dialog--detection .workbench-row-card {
  min-height: 68px;
  padding: 10px 12px;
}

.workbench-dialog--sample-login .workbench-row-card {
  min-height: 68px;
  padding: 10px 12px;
}

.workbench-dialog--report .workbench-row-card {
  min-height: 68px;
  padding: 10px 12px;
}

.workbench-row-card {
  display: grid;
  gap: 6px;
  padding: 12px;
  border: 1px solid rgba(214, 225, 241, 0.9);
  border-radius: 12px;
  background: #ffffff;
  text-align: left;
}

.workbench-row-card.is-active {
  border-color: rgba(22, 119, 255, 0.42);
  background: var(--brand-soft);
}

.workbench-row-card strong {
  color: var(--text-main);
  font-size: 14px;
}

.workbench-row-card span {
  color: var(--text-sub);
  font-size: 12px;
  line-height: 1.5;
}

.workbench-dialog__main {
  min-width: 0;
  overflow-y: auto;
  padding-right: 4px;
}

.workbench-dialog--sampling .workbench-dialog__main,
.workbench-dialog--detection .workbench-dialog__main {
  display: block;
  overflow: hidden;
}

.workbench-dialog--sample-login .workbench-dialog__main {
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
  padding-right: 0;
}

.workbench-dialog--report .workbench-dialog__main {
  min-height: 0;
  overflow: hidden;
  padding-right: 0;
}

.workbench-report-preview {
  width: 100%;
  height: 100%;
  min-width: 0;
  overflow: auto;
  padding: 0 4px 4px 0;
  scrollbar-width: thin;
  scrollbar-color: rgba(100, 116, 139, 0.32) transparent;
}

.workbench-report-preview::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.workbench-report-preview::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(100, 116, 139, 0.28);
}

.workbench-report-preview::-webkit-scrollbar-track {
  background: transparent;
}

.workbench-report-preview__scale {
  min-width: 820px;
}

.workbench-dialog--report-half-month .workbench-report-preview__scale {
  min-width: 1180px;
  zoom: 0.78;
}

.workbench-dialog--sampling :deep(.el-form) {
  height: 100%;
}

.workbench-dialog--detection .workbench-dialog__main {
  display: grid;
  align-content: start;
  gap: 14px;
  padding-right: 0;
}

.workbench-dialog--detection .summary-chips {
  margin-bottom: 0;
}

.workbench-dialog--detection .summary-chips span {
  min-height: 36px;
  padding: 8px 12px;
}

.workbench-dialog--detection .meta-grid {
  margin-bottom: 0;
}

.workbench-dialog--detection .meta-grid div {
  min-height: 64px;
  padding: 10px 12px;
}

.detection-step-row {
  display: grid;
  gap: 8px;
  min-height: 120px;
  padding: 12px 14px;
  border: 1px solid rgba(214, 225, 241, 0.9);
  border-radius: 12px;
  background: #ffffff;
}

.detection-step-row span {
  color: var(--text-sub);
  font-size: 12px;
}

.detection-step-row p {
  min-height: 68px;
  max-height: 92px;
  overflow-y: auto;
  margin: 0;
  color: var(--text-main);
  font-size: 13px;
  line-height: 1.6;
  overflow-wrap: anywhere;
}

.workbench-dialog--detection :deep(.el-form) {
  display: grid;
  grid-template-rows: 64px 112px 112px;
  gap: 14px;
}

.workbench-dialog--detection :deep(.el-form-item) {
  margin-bottom: 0;
}

.workbench-dialog--detection :deep(.el-form-item__label) {
  line-height: 26px;
  margin-bottom: 8px;
}

.workbench-dialog--detection :deep(.el-form-item__content) {
  min-height: 36px;
}

.workbench-dialog--detection .detection-textarea-item :deep(.el-textarea__inner) {
  height: 76px;
  min-height: 76px !important;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 4px 14px;
}

.sample-login-grid {
  gap: 2px 14px;
}

.workbench-dialog--sample-login :deep(.el-form-item) {
  margin-bottom: 12px;
}

.sample-login-config-table {
  flex: 0 1 auto;
  min-height: 0;
}

.sampling-entry-grid {
  height: 100%;
  align-content: stretch;
  grid-template-rows: repeat(4, 48px) minmax(132px, 1fr);
  gap: 10px 22px;
  padding: 2px 0;
}

.sampling-entry-grid :deep(.el-form-item) {
  align-items: center;
  margin-bottom: 0;
  min-height: 48px;
}

.sampling-entry-grid :deep(.el-form-item__label) {
  line-height: 36px;
  padding-right: 10px;
  white-space: nowrap;
}

.sampling-entry-grid :deep(.el-form-item__content) {
  min-height: 36px;
  line-height: 36px;
  min-width: 0;
}

.sampling-entry-grid .form-span-2 {
  align-items: stretch;
  margin-top: 0;
  min-height: 132px;
}

.sampling-entry-grid .form-span-2 :deep(.el-form-item__label) {
  line-height: 36px;
  padding-top: 0;
}

.sampling-entry-grid .form-span-2 :deep(.el-form-item__content) {
  display: flex;
  min-height: 132px;
  line-height: 1.5;
}

.sampling-entry-grid .form-span-2 :deep(.el-textarea) {
  flex: 1;
}

.sampling-entry-grid .form-span-2 :deep(.el-textarea__inner) {
  height: 100%;
  min-height: 132px !important;
}

.form-span-2 {
  grid-column: span 2;
}

.summary-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 14px;
}

.summary-chips span {
  padding: 8px 12px;
  border-radius: 999px;
  background: #f6f9fc;
  color: var(--text-sub);
}

.summary-chips strong {
  margin-left: 6px;
  color: var(--text-main);
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 14px;
}

.meta-grid div {
  display: grid;
  gap: 6px;
  padding: 12px;
  border: 1px solid rgba(214, 225, 241, 0.9);
  border-radius: 12px;
  background: #ffffff;
}

.meta-grid span {
  color: var(--text-sub);
  font-size: 12px;
}

.meta-grid strong {
  color: var(--text-main);
  font-size: 13px;
}

.result-value-field {
  display: flex;
  align-items: center;
  gap: 10px;
}

.result-value-input {
  width: 220px;
}

.result-value-unit {
  color: var(--text-sub);
}

.compact-table {
  margin-top: 12px;
}

.review-toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}

.review-remark-form {
  margin-top: 12px;
}

.review-remark-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.review-remark-form :deep(.el-textarea__inner) {
  min-height: 116px !important;
}

.empty-block {
  display: grid;
  min-height: 160px;
  place-items: center;
  color: var(--text-sub);
}

@media (max-width: 1280px) {
  .workbench-hero {
    grid-template-columns: 1fr;
  }

  .workbench-hero__stats {
    grid-template-columns: repeat(2, minmax(120px, 1fr));
  }

  .todo-detail-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .workbench-dialog {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .workbench-hero__stats {
    grid-template-columns: 1fr;
  }

  .todo-detail-grid {
    grid-template-columns: 1fr;
  }

  .todo-detail-card {
    height: 132px;
  }
}
</style>
