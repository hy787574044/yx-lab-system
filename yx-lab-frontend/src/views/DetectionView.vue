<template>
  <div class="content-grid detection-page fixed-table-page">
    <section class="glass-panel section-block fixed-table-section">
      <div class="section-head">
        <div>
          <h3 class="section-title">{{ baseScene.tableTitle }}</h3>
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
            <div class="toolbar-fields">
              <label class="toolbar-field toolbar-field--medium">
                <span>关键字</span>
                <el-input
                  v-model="query.keyword"
                  clearable
                  placeholder="请输入样品编号或检测套餐"
                  @keyup.enter="handleSearch"
                />
              </label>
              <label class="toolbar-field">
                <span>流程状态</span>
                <el-select v-model="query.detectionStatus" clearable placeholder="请选择流程状态">
                  <el-option
                    v-for="option in detectionStatusOptions"
                    :key="option.value"
                    :label="option.label"
                    :value="option.value"
                  />
                </el-select>
              </label>
              <label class="toolbar-field">
                <span>数据范围</span>
                <el-select v-model="query.mine" clearable placeholder="请选择数据范围">
                  <el-option
                    v-for="option in mineOptions"
                    :key="String(option.value)"
                    :label="option.label"
                    :value="option.value"
                  />
                </el-select>
              </label>
            </div>
          </div>
          <div class="toolbar-actions">
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="resetQuery">重置</el-button>
            <el-button @click="handleExport">导出</el-button>
          </div>
        </div>
      </div>

      <div class="table-card table-card--fixed-scroll">
        <div class="table-card__body">
        <el-table
          class="list-table"
          :data="records"
          stripe
          row-key="id"
          height="100%"
          :empty-text="baseScene.emptyText"
        >
          <el-table-column prop="sampleNo" label="样品编号" min-width="170" />
          <el-table-column prop="detectionTypeName" label="检测套餐" min-width="180" />
          <el-table-column label="参数进度" min-width="150">
            <template #default="{ row }">
              {{ formatProgress(row) }}
            </template>
          </el-table-column>
          <el-table-column label="检测人员" min-width="120">
            <template #default="{ row }">
              {{ row.detectorName || (row.detectionStatus === WAIT_ASSIGN_STATUS ? '待分配' : '-') }}
            </template>
          </el-table-column>
          <el-table-column label="检测结果" width="120" header-cell-class-name="cell-center result-field-header" class-name="cell-center">
            <template #default="{ row }">
              <span class="status-chip" :class="getDetectionResultClass(row)">
                {{ getDetectionResultLabel(row) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="流程状态" width="120" header-cell-class-name="cell-center" class-name="cell-center">
            <template #default="{ row }">
              <span class="status-chip" :class="getDetectionStatusClass(row.detectionStatus)">
                {{ getDetectionStatusLabel(row.detectionStatus) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="detectionTime" label="流程时间" width="170" />
          <el-table-column label="说明" min-width="260" show-overflow-tooltip>
            <template #default="{ row }">
              {{ getRecordRemark(row) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="220" fixed="right" header-cell-class-name="cell-center" class-name="cell-center">
            <template #default="{ row }">
              <div class="table-action-row">
                <el-button
                  type="primary"
                  link
                  @click="openSubflowDialog(row)"
                >
                  {{ getSubflowActionLabel() }}
                </el-button>
                <el-button
                  v-if="canViewRecordResults(row)"
                  type="primary"
                  link
                  @click="openRecordResultDialog(row)"
                >
                  查看结果
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
        </div>

        <TablePagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          @change="loadData"
        />
      </div>
    </section>

    <el-dialog
      v-model="subflowDialogVisible"
      :class="['detection-subflow-dialog', { 'detection-subflow-dialog--assign': currentSubflowRecord && canAssignRow(currentSubflowRecord) }]"
      title="检测项明细"
      :width="currentSubflowRecord && canAssignRow(currentSubflowRecord) ? '1280px' : '1080px'"
      destroy-on-close
      @closed="resetSubflowDialog"
    >
      <div v-if="currentSubflowRecord" class="subflow-panel subflow-panel--dialog">
        <div class="result-dialog__summary">
          <span class="binding-editor__chip">样品编号<strong>{{ currentSubflowRecord.sampleNo || '-' }}</strong></span>
          <span class="binding-editor__chip">检测套餐<strong>{{ currentSubflowRecord.detectionTypeName || '-' }}</strong></span>
          <span class="binding-editor__chip">检测项数<strong>{{ currentSubflowItems.length }}</strong></span>
        </div>

        <div v-if="currentSubflowRecord.__sampleOnly" class="subflow-empty">
          <strong>该样品为历史旧数据</strong>
          <p>当前只保留了样品主信息，尚未生成正式的参数子流程分配数据。建议重新登录该样品后再进行参数级分配。</p>
        </div>

        <div v-else-if="currentSubflowLoading" class="subflow-empty">
          正在加载参数子流程...
        </div>

        <div v-else-if="currentSubflowItems.length" class="subflow-content">
          <div class="subflow-head">
            <div class="subflow-summary">
              <button
                v-for="item in currentSubflowSummary"
                :key="item.key"
                type="button"
                :class="['status-chip', item.type, 'subflow-summary__chip', { 'is-active': subflowItemFilter === item.key }]"
                @click="setSubflowItemFilter(item.key)"
              >
                {{ item.label }} {{ item.value }}
              </button>
            </div>
          </div>

          <div v-if="canAssignRow(currentSubflowRecord)" class="subflow-assign-layout">
            <div class="subflow-assign-main">
              <div class="subflow-batch-panel">
                <div class="subflow-batch-panel__head">
                  <div>
                    <strong>检测参数列表</strong>
                    <p>左侧勾选一个或多个检测参数，右侧选择检测人员后点击确认即可批量设置。</p>
                  </div>
                  <div class="subflow-batch-panel__actions">
                    <span v-if="selectedAssignableItems.length" class="subflow-batch-panel__count">
                      已选 {{ selectedAssignableItems.length }} 项
                    </span>
                    <el-button link type="primary" @click="selectAllAssignableSubflowItems">全选可分配</el-button>
                    <el-button link @click="clearSelectedSubflowItems">清空已选</el-button>
                  </div>
                </div>

                <div class="subflow-pick-list">
                  <label
                    v-for="item in assignableSubflowItems"
                    :key="`pick-${item.id}`"
                    :class="['subflow-pick-item', { 'is-selected': isSubflowItemSelected(item.id) }]"
                  >
                    <el-checkbox
                      :model-value="isSubflowItemSelected(item.id)"
                      @change="toggleSubflowItemSelected(item.id, $event)"
                    />
                    <div class="subflow-pick-item__body">
                      <strong>{{ item.parameterName }}</strong>
                      <p>{{ item.methodName || '未绑定检测方法' }}</p>
                      <span>当前检测人员：{{ getAssignedDetectorName(currentSubflowRecord.id, item) }}</span>
                    </div>
                    <span class="status-chip" :class="getSubflowItemStatusClass(currentSubflowRecord.id, item)">
                      {{ getSubflowItemStatusLabel(currentSubflowRecord.id, item) }}
                    </span>
                  </label>
                </div>
              </div>
            </div>

            <aside class="detector-side-panel detector-side-panel--inline">
              <div class="detector-side-panel__header">
                <strong>选择检测人员</strong>
                <el-input
                  v-model="detectorKeyword"
                  clearable
                  class="detector-side-panel__search"
                  placeholder="输入人员名称筛选"
                />
              </div>

              <div class="detector-assign-dialog">
                <div class="detector-assign-dialog__summary">
                  <span class="binding-editor__chip">已选参数<strong>{{ selectedAssignableItems.length }}</strong></span>
                  <span class="binding-editor__chip">可选人员<strong>{{ filteredDetectorOptions.length }}</strong></span>
                </div>

                <section class="detector-assign-dialog__list">
                  <button
                    v-for="option in filteredDetectorOptions"
                    :key="option.id"
                    type="button"
                    :class="['subflow-detector-card', { 'is-selected': isPendingDetectorSelected(option.id) }]"
                    @click="selectPendingDetector(option.id)"
                  >
                    <span v-if="isPendingDetectorSelected(option.id)" class="detector-check-mark">✓</span>
                    <strong>{{ getDetectorOptionLabel(option) }}</strong>
                    <span>{{ isPendingDetectorSelected(option.id) ? '已选择，已回填到左侧待提交参数' : '点击选择该检测人员' }}</span>
                  </button>
                  <div v-if="!filteredDetectorOptions.length" class="detector-empty">
                    未找到匹配的检测人员
                  </div>
                </section>
              </div>
            </aside>

            <div class="subflow-submit-footer">
              <el-button
                type="primary"
                @click="saveAssignments(currentSubflowRecord)"
              >
                提交
              </el-button>
            </div>
          </div>

          <div v-else class="subflow-readonly-panel">
            <el-table
              class="list-table subflow-readonly-table"
              :data="visibleSubflowItems"
              stripe
              border
              max-height="460"
            >
              <el-table-column prop="parameterName" label="检测参数" min-width="120" show-overflow-tooltip />
              <el-table-column prop="methodName" label="检测方法" min-width="130" show-overflow-tooltip>
                <template #default="{ row }">{{ row.methodName || '-' }}</template>
              </el-table-column>
              <el-table-column label="检测步骤" min-width="240" class-name="cell-multiline">
                <template #default="{ row }">
                  <span class="method-basis-text">{{ getMethodBasis(row) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="检测人员" width="110">
                <template #default="{ row }">{{ row.detectorName || '-' }}</template>
              </el-table-column>
              <el-table-column label="判定结果" width="100" header-cell-class-name="cell-center" class-name="cell-center">
                <template #default="{ row }">
                  <span class="status-chip" :class="getItemResultClass(row)">
                    {{ getItemResultLabel(row) }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column label="子流程状态" width="112" header-cell-class-name="cell-center" class-name="cell-center">
                <template #default="{ row }">
                  <span class="status-chip" :class="getSubflowItemStatusClass(currentSubflowRecord.id, row)">
                    {{ getSubflowItemStatusLabel(currentSubflowRecord.id, row) }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column label="标准范围" min-width="120">
                <template #default="{ row }">
                  {{ formatStandardRange(row.standardMin, row.standardMax, null, row.optionValues) }}
                </template>
              </el-table-column>
              <el-table-column prop="unit" label="单位" width="72">
                <template #default="{ row }">{{ row.unit || '-' }}</template>
              </el-table-column>
              <el-table-column prop="referenceStandard" label="检测标准" min-width="130" show-overflow-tooltip>
                <template #default="{ row }">{{ row.referenceStandard || '-' }}</template>
              </el-table-column>
              <el-table-column label="检测结果" width="100" header-cell-class-name="result-field-header">
                <template #default="{ row }">
                  {{ getResultDisplayValue(row) }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="100" fixed="right" header-cell-class-name="cell-center" class-name="cell-center">
                <template #default="{ row }">
                  <el-button
                    v-if="canOpenResultDialogItem(row)"
                    type="primary"
                    link
                    @click="openResultDialog(currentSubflowRecord, row)"
                  >
                    查看结果
                  </el-button>
                  <span v-else class="table-action-placeholder">-</span>
                </template>
              </el-table-column>
            </el-table>
          </div>

        </div>

        <div v-else class="subflow-empty">
          当前主流程下暂无参数子流程数据。
        </div>

      </div>
    </el-dialog>

    <el-dialog
      v-model="resultDialogVisible"
      class="detection-result-dialog"
      :title="resultDialogTitle"
      width="1080px"
      destroy-on-close
      @closed="resetResultForm"
    >
      <div class="result-dialog">
        <div class="result-dialog__summary">
          <span class="binding-editor__chip">样品编号<strong>{{ resultForm.sampleNo || '-' }}</strong></span>
          <span class="binding-editor__chip">检测套餐<strong>{{ resultForm.detectionTypeName || '-' }}</strong></span>
          <span class="binding-editor__chip">{{ resultDialogScopeLabel }}<strong>{{ resultForm.parameterName || '-' }}</strong></span>
          <span class="binding-editor__chip">参数数<strong>{{ resultForm.items.length }}</strong></span>
          <span class="binding-editor__chip">正常<strong>{{ resultNormalCount }}</strong></span>
          <span class="binding-editor__chip">异常<strong>{{ resultAbnormalCount }}</strong></span>
        </div>

        <div class="panel-note result-dialog__note">
          {{ resultDialogNote }}
        </div>

        <div v-if="!resultDialogReadonly" class="result-dialog__toolbar">
          <el-button class="ocr-trigger-btn" @click="handleOcrTrigger">OCR识别</el-button>
        </div>

        <el-table
          class="list-table result-dialog__table"
          :data="resultForm.items"
          stripe
          border
          max-height="460"
        >
          <el-table-column prop="parameterName" label="检测参数" min-width="120" show-overflow-tooltip />
          <el-table-column prop="methodName" label="检测方法" min-width="150" show-overflow-tooltip />
          <el-table-column label="标准范围" min-width="120">
            <template #default="{ row }">
              {{ formatStandardRange(row.standardMin, row.standardMax, row.unit, row.optionValues) }}
            </template>
          </el-table-column>
          <el-table-column prop="unit" label="单位" width="72">
            <template #default="{ row }">{{ row.unit || '-' }}</template>
          </el-table-column>
          <el-table-column prop="referenceStandard" label="检测标准" min-width="130" show-overflow-tooltip>
            <template #default="{ row }">{{ row.referenceStandard || '-' }}</template>
          </el-table-column>
          <el-table-column label="检测结果" width="120" header-cell-class-name="result-field-header">
            <template #default="{ row }">
              <span v-if="resultDialogReadonly">{{ getResultDisplayValue(row) }}</span>
              <el-select
                v-else-if="row.optionValues"
                v-model="row.resultValue"
                style="width: 100%"
                placeholder="请选择"
              >
                <el-option
                  v-for="(label, index) in parseOptionValuesArray(row.optionValues)"
                  :key="index"
                  :label="label"
                  :value="String(index)"
                />
              </el-select>
              <el-input-number
                v-else
                v-model="row.resultValue"
                :step="0.01"
                controls-position="right"
                style="width: 100%"
              />
            </template>
          </el-table-column>
        </el-table>

        <el-form label-position="top" class="result-dialog__form">
          <el-form-item label="异常说明">
            <el-input
              v-model="resultForm.abnormalRemark"
              type="textarea"
              :rows="3"
              :readonly="resultDialogReadonly"
              placeholder="如有异常项，可补充现场化验情况、复核说明或异常原因"
            />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <el-button @click="resultDialogVisible = false">{{ resultDialogReadonly ? '关闭' : '取消' }}</el-button>
        <el-button
          v-if="!resultDialogReadonly"
          type="primary"
          :loading="resultSubmitting"
          @click="submitDetectionResult"
        >
          提交
        </el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElButton } from 'element-plus/es/components/button/index.mjs'
import { ElCheckbox } from 'element-plus/es/components/checkbox/index.mjs'
import { ElDialog } from 'element-plus/es/components/dialog/index.mjs'
import { ElForm, ElFormItem } from 'element-plus/es/components/form/index.mjs'
import { ElInput } from 'element-plus/es/components/input/index.mjs'
import { ElInputNumber } from 'element-plus/es/components/input-number/index.mjs'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElOption, ElSelect } from 'element-plus/es/components/select/index.mjs'
import { ElTable, ElTableColumn } from 'element-plus/es/components/table/index.mjs'
import TablePagination from '../components/common/TablePagination.vue'
import {
  assignDetectionDetectorsApi,
  exportDetectionsApi,
  fetchDetectionDetailApi,
  fetchDetectionDetectorsApi,
  fetchDetectionRecordSummaryApi,
  fetchDetectionsApi,
  submitDetectionApi
} from '../api/lab'
import {
  abnormalDetectionResult,
  approvedDetectionStatus,
  DEFAULT_PAGE_SIZE,
  detectionResultLabelMap,
  detectionStatusLabelMap,
  getEnumLabel,
  getStatusClass,
  rejectedDetectionStatus,
  reviewPendingDetectionStatus,
  translateWorkflowText,
  waitAssignDetectionStatus,
  waitDetectDetectionStatus
} from '../utils/labEnums'
import { hasPermission } from '../utils/permission'

const props = defineProps({
  forcedScenePath: {
    type: String,
    default: ''
  }
})

const route = useRoute()

const WAIT_ASSIGN_STATUS = waitAssignDetectionStatus
const WAIT_DETECT_STATUS = waitDetectDetectionStatus

const query = reactive({
  keyword: '',
  detectionStatus: '',
  mine: '',
  pageNum: 1,
  pageSize: DEFAULT_PAGE_SIZE
})
const records = ref([])
const total = ref(0)
let loadDataVersion = 0
const detectorOptions = ref([])
const detectorOptionsLoaded = ref(false)
const detectorKeyword = ref('')
const activeStatKey = ref('all')
const summary = reactive({
  total: 0,
  waitAssignCount: 0,
  waitDetectCount: 0,
  pendingReviewCount: 0,
  approvedCount: 0,
  rejectedCount: 0
})
const detailMap = reactive({})
const detailLoadingMap = reactive({})
const assignmentMap = reactive({})
const subflowDialogVisible = ref(false)
const currentSubflowRecord = ref(null)
const selectedSubflowItemIds = ref([])
const subflowItemFilter = ref('all')
const pendingDetectorId = ref(undefined)
const resultDialogVisible = ref(false)
const resultSubmitting = ref(false)
const resultDialogItemId = ref(null)
const resultDialogItemStatus = ref('')
const resultDialogMode = ref('item')

const resultForm = reactive({
  recordId: null,
  sampleId: null,
  sampleNo: '',
  detectionTypeId: null,
  detectionTypeName: '',
  parameterName: '',
  abnormalRemark: '',
  items: []
})

function parseSampleConfigSnapshot(snapshotText) {
  if (!snapshotText) {
    return []
  }
  try {
    const parsed = JSON.parse(snapshotText)
    return Array.isArray(parsed) ? parsed.filter((item) => item && item.parameterId) : []
  } catch {
    return []
  }
}

function buildPendingSampleRecord(sample) {
  const configItems = parseSampleConfigSnapshot(sample.detectionConfigSnapshot)
  return {
    id: `pending-sample-${sample.id}`,
    sampleId: sample.id,
    sampleNo: sample.sampleNo,
    detectionTypeName: sample.detectionTypeName || sample.detectionItems || '待分配检测套餐',
    detectorName: '',
    detectionResult: null,
    detectionStatus: WAIT_ASSIGN_STATUS,
    detectionTime: sample.samplingTime || '',
    abnormalRemark: '历史样品未自动生成参数子流程',
    parameterCount: configItems.length,
    assignedCount: 0,
    completedCount: 0,
    __sampleOnly: true
  }
}

function compareByTimeDesc(left, right) {
  return String(right.detectionTime || '').localeCompare(String(left.detectionTime || ''))
}

const sceneMap = {
  '/detection-split': {
    key: 'detection-split',
    title: '检测分样',
    subtitle: '样品登录后按套餐参数生成检测子流程，默认由采样员自采自检；特殊情况可在本页调整人员。',
    tableTitle: '检测流程队列',
    tableSubtitle: '点击每条主流程后的”查看检测项”，即可查看套餐参数列表；特殊情况可重新分配检测人员。',
    note: '当前页面重点处理需要人工调整检测人员的特殊检测流程，调整完成后即可继续检测执行。',
    guide: '常规样品会默认分配采样员检测，仅在需要协同或改派时进入本页调整。',
    defaultStatKey: 'all',
    emptyText: '暂无检测分样数据',
    allowAssign: true,
    includePendingFallback: false,
    statKeys: ['all', 'waitAssign', 'waitDetect', 'pendingReview'],
    recordFilter: (item) => [WAIT_ASSIGN_STATUS, WAIT_DETECT_STATUS].includes(item.detectionStatus),
    quickLinks: [
      { path: '/sample-login', label: '样品登录', desc: '样品登录后默认自采自检，特殊情况再进入检测分样调整人员' },
      { path: '/review-result', label: '结果审查', desc: '检测完成提交后进入结果审查闭环' },
      { path: '/detection-history', label: '历史检测', desc: '查看已完成检测与驳回重检记录' }
    ]
  },
  '/detection-analysis': {
    key: 'detection-analysis',
    title: '检测分析',
    subtitle: '样品登录后按套餐参数生成检测子流程，默认由采样员自采自检并直接进入待检测。',
    tableTitle: '检测流程队列',
    tableSubtitle: '点击每条主流程后的“查看检测项”，即可查看套餐参数列表并录入检测结果。',
    note: '当前页重点处理待检测、已提交和驳回重检流程；待分配一般只用于特殊改派场景。',
    guide: '员工可直接处理分配给自己的检测子流程，主任可按需进入检测分样调整人员。',
    defaultStatKey: 'all',
    emptyText: '暂无检测分析数据',
    allowAssign: true,
    includePendingFallback: true,
    recordFilter: (item) => [WAIT_ASSIGN_STATUS, WAIT_DETECT_STATUS, reviewPendingDetectionStatus].includes(item.detectionStatus),
    quickLinks: [
      { path: '/sample-login', label: '样品登录', desc: '样品登录后自动进入检测分析，默认由采样员自采自检' },
      { path: '/review-result', label: '结果审查', desc: '检测完成提交后进入结果审查闭环' },
      { path: '/detection-history', label: '历史检测', desc: '查看已完成检测与驳回重检记录' }
    ]
  },
  '/detection-history': {
    key: 'detection-history',
    title: '历史检测',
    subtitle: '回看已进入审查或已完成闭环的检测记录，重点核对异常结果与驳回重检情况。',
    tableTitle: '历史检测记录',
    tableSubtitle: '本页只保留历史追溯视角，不再承担当前待办分配动作。',
    note: '历史页仅用于追溯检测链路，不再展示当前需要人员分配的流程。',
    guide: '如需处理新的样品检测，请返回检测分析页；如需继续追踪审查结论，请前往历史审查。',
    defaultStatKey: 'approved',
    emptyText: '暂无历史检测数据',
    allowAssign: false,
    includePendingFallback: false,
    statKeys: ['all', 'approved', 'rejected'],
    recordFilter: (item) => [approvedDetectionStatus, rejectedDetectionStatus].includes(item.detectionStatus),
    quickLinks: [
      { path: '/review-history', label: '历史审查', desc: '继续查看检测结果的审查处理结论' },
      { path: '/detection-ledger', label: '检测台账', desc: '切换到全量检测主流程台账视角' },
      { path: '/report-ledger', label: '报告台账', desc: '查看检测结果是否已沉淀为正式报告' }
    ]
  },
  '/detection-ledger': {
    key: 'detection-ledger',
    title: '检测台账',
    subtitle: '统一查看全部检测主流程，同时保留参数子流程与人员分配的完整留痕。',
    tableTitle: '检测全量台账',
    tableSubtitle: '适合管理端统一核对各状态主流程及其参数拆分情况。',
    note: '台账页保留全部状态，既能看当前待办，也能看已提交、已审查与已驳回记录。',
    guide: '如需实际执行分配动作，建议返回检测分析页操作；台账页更适合总览与核对。',
    defaultStatKey: 'all',
    emptyText: '暂无检测台账数据',
    allowAssign: false,
    includePendingFallback: true,
    recordFilter: () => true,
    quickLinks: [
      { path: '/detection-analysis', label: '检测分析', desc: '回到当前待分配和待检测主流程' },
      { path: '/review-result', label: '结果审查', desc: '处理已经提交完成的检测结果' },
      { path: '/sample-ledger', label: '样品台账', desc: '回看检测流程对应的样品来源信息' }
    ]
  }
}

const scenePath = computed(() => props.forcedScenePath || route.path)

const baseScene = computed(() => sceneMap[scenePath.value] || sceneMap['/detection-analysis'])

const sceneRecords = computed(() => records.value)

const currentScene = computed(() => ({
  ...baseScene.value,
  tags: [
    {
      label: '待分配',
      value: summary.waitAssignCount,
      type: summary.waitAssignCount > 0 ? 'warning' : 'success'
    },
    {
      label: '待检测',
      value: summary.waitDetectCount,
      type: summary.waitDetectCount > 0 ? 'info' : 'success'
    },
    {
      label: '待审查',
      value: summary.pendingReviewCount,
      type: summary.pendingReviewCount > 0 ? 'danger' : 'success'
    }
  ]
}))

const detectionStatusOptions = computed(() => Object.entries(detectionStatusLabelMap).map(([value, label]) => ({ value, label })))
const mineOptions = [
  { value: true, label: '仅看我的' },
  { value: false, label: '查看全部' }
]

const allStats = computed(() => [
  {
    key: 'all',
    label: baseScene.value.key === 'detection-ledger' ? '台账总量' : '流程总量',
    value: summary.total,
    desc: baseScene.value.key === 'detection-ledger' ? '当前检测台账主流程总数' : '当前场景下可见的检测主流程总数'
  },
  {
    key: 'waitAssign',
    label: '待分配',
    value: summary.waitAssignCount,
    desc: '样品已进入检测分析，但参数子流程尚未完成检测员分配'
  },
  {
    key: 'waitDetect',
    label: '待检测',
    value: summary.waitDetectCount,
    desc: '参数子流程已完成检测员分配，等待检测执行'
  },
  {
    key: 'pendingReview',
    label: '待审查',
    value: summary.pendingReviewCount,
    desc: '检测结果已提交，等待进入审查闭环'
  },
  {
    key: 'approved',
    label: '已通过',
    value: summary.approvedCount,
    desc: '检测审查已通过，可继续进入报告正式产物环节'
  },
  {
    key: 'rejected',
    label: '已驳回',
    value: summary.rejectedCount,
    desc: '检测审查已驳回，样品重新回到重检待办链路'
  }
])

const currentStats = computed(() => {
  if (!baseScene.value.statKeys) {
    return allStats.value
  }
  const allowedKeys = new Set(baseScene.value.statKeys)
  return allStats.value.filter((item) => allowedKeys.has(item.key))
})

const currentSubflowItems = computed(() => {
  if (!currentSubflowRecord.value || currentSubflowRecord.value.__sampleOnly) {
    return []
  }
  return getDetailItems(currentSubflowRecord.value.id)
})

const currentSubflowLoading = computed(() => {
  if (!currentSubflowRecord.value || currentSubflowRecord.value.__sampleOnly) {
    return false
  }
  return !!detailLoadingMap[currentSubflowRecord.value.id]
})

const currentSubflowSummary = computed(() => {
  const recordId = currentSubflowRecord.value?.id
  const total = currentSubflowItems.value.length
  const assigned = recordId ? countAssignedItems(recordId) : 0
  const submitted = recordId ? countSubmittedItems(recordId) : 0
  return [
    { key: 'all', label: '参数', value: total, type: 'info' },
    { key: 'assigned', label: '已分配', value: assigned, type: 'success' },
    { key: 'waitAssign', label: '待分配', value: Math.max(total - assigned, 0), type: 'warning' },
    { key: 'submitted', label: '已提交', value: submitted, type: 'success' },
    { key: 'waitSubmit', label: '待提交', value: Math.max(total - submitted, 0), type: 'info' }
  ]
})

const visibleSubflowItems = computed(() => {
  const recordId = currentSubflowRecord.value?.id
  if (!recordId || subflowItemFilter.value === 'all') {
    return currentSubflowItems.value
  }
  if (subflowItemFilter.value === 'assigned') {
    return currentSubflowItems.value.filter((item) => isSubflowItemAssigned(recordId, item))
  }
  if (subflowItemFilter.value === 'waitAssign') {
    return currentSubflowItems.value.filter((item) => !isSubflowItemAssigned(recordId, item))
  }
  if (subflowItemFilter.value === 'submitted') {
    return currentSubflowItems.value.filter((item) => isSubflowItemSubmitted(item))
  }
  if (subflowItemFilter.value === 'waitSubmit') {
    return currentSubflowItems.value.filter((item) => !isSubflowItemSubmitted(item))
  }
  return currentSubflowItems.value
})

const assignableSubflowItems = computed(() => visibleSubflowItems.value.filter((item) => isAssignableSubflowItem(item)))

const selectedAssignableItems = computed(() => {
  const selectedSet = new Set(selectedSubflowItemIds.value)
  return currentSubflowItems.value.filter((item) => isAssignableSubflowItem(item) && selectedSet.has(item.id))
})

const filteredDetectorOptions = computed(() => {
  const keyword = detectorKeyword.value.trim().toLowerCase()
  if (!keyword) {
    return detectorOptions.value
  }
  return detectorOptions.value.filter((option) => getDetectorSearchText(option).includes(keyword))
})

const selectedCommonDetectorId = computed(() => {
  const recordId = currentSubflowRecord.value?.id
  if (!recordId || !selectedAssignableItems.value.length) {
    return undefined
  }
  const detectorIds = selectedAssignableItems.value.map((item) => getAssignedDetectorId(recordId, item.id))
  const firstDetectorId = detectorIds[0]
  if (firstDetectorId === null || firstDetectorId === undefined || firstDetectorId === '') {
    return undefined
  }
  return detectorIds.every((detectorId) => String(detectorId) === String(firstDetectorId))
    ? firstDetectorId
    : undefined
})

const activeDetectorId = computed(() => {
  if (selectedAssignableItems.value.length) {
    return selectedCommonDetectorId.value
  }
  return pendingDetectorId.value
})

const resultDialogReadonly = computed(() => {
  if (resultDialogMode.value === 'record') {
    return true
  }
  return ![WAIT_DETECT_STATUS, rejectedDetectionStatus].includes(resultDialogItemStatus.value)
})

const resultDialogTitle = computed(() => {
  const action = resultDialogMode.value === 'record'
    ? '检测结果汇总'
    : (resultDialogReadonly.value ? '检测结果明细' : '检测结果录入')
  return resultForm.parameterName ? `${action} - ${resultForm.parameterName}` : action
})

const resultDialogScopeLabel = computed(() => (resultDialogMode.value === 'record' ? '汇总范围' : '当前参数'))

const resultNormalCount = computed(() => resultForm.items.filter((item) => getResultValueStatusLabel(item) === '正常').length)

const resultAbnormalCount = computed(() => resultForm.items.filter((item) => getResultValueStatusLabel(item) === '异常').length)

const resultDialogNote = computed(() => (
  resultDialogMode.value === 'record'
    ? '当前窗口用于汇总查看该主流程下全部参数的检测方法、结果值与异常判定。'
    : (resultDialogReadonly.value
        ? '当前窗口用于查看该参数子流程已保存的检测方法、结果值与异常判定。'
        : '请按当前参数子流程录入化验结果，系统会根据标准范围自动判定正常或异常。')
))

function getDetectionStatusLabel(status) {
  return getEnumLabel(detectionStatusLabelMap, status)
}

function getDetectionStatusClass(status) {
  return getStatusClass('detectionStatus', status)
}

function getItemStatusLabel(status) {
  return getDetectionStatusLabel(status)
}

function getItemStatusClass(status) {
  return getDetectionStatusClass(status)
}

function getSubflowItemStatusLabel(recordId, item) {
  if (item?.itemStatus === WAIT_ASSIGN_STATUS && isSubflowItemAssigned(recordId, item)) {
    return '待提交'
  }
  return getItemStatusLabel(item?.itemStatus)
}

function getSubflowItemStatusClass(recordId, item) {
  if (item?.itemStatus === WAIT_ASSIGN_STATUS && isSubflowItemAssigned(recordId, item)) {
    return 'info'
  }
  return getItemStatusClass(item?.itemStatus)
}

function getDetectionResultLabel(row) {
  const total = Number(row?.parameterCount || 0)
  const completed = Number(row?.completedCount || 0)
  if (!total) {
    return '-'
  }
  if (completed <= 0) {
    return '未录入'
  }
  if (completed < total) {
    return '部分提交'
  }
  return row?.detectionResult ? getEnumLabel(detectionResultLabelMap, row.detectionResult) : '已提交'
}

function getDetectionResultClass(row) {
  const total = Number(row?.parameterCount || 0)
  const completed = Number(row?.completedCount || 0)
  if (!total || completed <= 0) {
    return 'info'
  }
  if (completed < total) {
    return 'warning'
  }
  return row?.detectionResult ? getStatusClass('detectionResult', row.detectionResult) : 'success'
}

function handleStatClick(key) {
  const nextKey = activeStatKey.value === key ? 'all' : key
  activeStatKey.value = nextKey
  query.detectionStatus = getDetectionStatusByStatKey(nextKey) || ''
  query.pageNum = 1
  loadData()
}

function handleSearch() {
  query.pageNum = 1
  syncActiveStatByQuery()
  loadData()
}

function resetQuery() {
  query.keyword = ''
  query.detectionStatus = ''
  query.mine = ''
  query.pageNum = 1
  syncRouteState()
  loadData()
}

function syncRouteState() {
  activeStatKey.value = 'all'
  query.detectionStatus = ''
  query.pageNum = 1
}

function parseOptionValuesArray(json) {
  if (!json) return []
  try {
    const arr = JSON.parse(json)
    return Array.isArray(arr) ? arr : []
  } catch {
    return []
  }
}

function formatStandardRange(min, max, unit, optionValues) {
  if (optionValues) {
    const options = parseOptionValuesArray(optionValues)
    if (options.length) return options.join(' / ')
  }
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
  return `未设置${suffix}`
}

function formatProgress(row) {
  const total = Number(row.parameterCount || 0)
  const assigned = Number(row.assignedCount || 0)
  const completed = Number(row.completedCount || 0)
  if (!total) {
    return '-'
  }
  if (completed > 0 && completed < total) {
    return `${assigned}/${total} 已分配，${completed}/${total} 已提交，检测进行中`
  }
  if (completed === total) {
    return `${assigned}/${total} 已分配，${completed}/${total} 已提交，已全部完成`
  }
  return `${assigned}/${total} 已分配，${completed}/${total} 已提交`
}

function getMethodBasis(item) {
  if (item?.methodBasis) {
    return item.methodBasis
  }
  return '-'
}

function canAssignRow(row) {
  return !row.__sampleOnly
    && baseScene.value.allowAssign
    && hasPermission('detection:assign')
    && [WAIT_ASSIGN_STATUS, WAIT_DETECT_STATUS].includes(row.detectionStatus)
}

function isAssignableSubflowItem(item) {
  return !!item && [WAIT_ASSIGN_STATUS, WAIT_DETECT_STATUS, rejectedDetectionStatus].includes(item.itemStatus)
}

function canViewRecordResults(row) {
  if (!row || row.__sampleOnly) {
    return false
  }
  const total = Number(row.parameterCount || 0)
  const completed = Number(row.completedCount || 0)
  return total > 0 && completed === total
}

function canOpenResultDialogItem(item) {
  return !!item
    && [WAIT_DETECT_STATUS, reviewPendingDetectionStatus, approvedDetectionStatus, rejectedDetectionStatus].includes(item.itemStatus)
}

function isResultEditable(item) {
  return !!item && [WAIT_DETECT_STATUS, rejectedDetectionStatus].includes(item.itemStatus)
}

function getResultActionLabel(item) {
  return isResultEditable(item) ? '录入结果' : '查看结果'
}

function getSubflowActionLabel() {
  return baseScene.value.key === 'detection-split' ? '分配检测人员' : '查看检测项'
}

function getDetailItems(recordId) {
  return detailMap[recordId]?.items || []
}

function countSubmittedItems(recordId) {
  return getDetailItems(recordId).filter((item) => isSubflowItemSubmitted(item)).length
}

function countAssignedItems(recordId) {
  return getDetailItems(recordId).filter((item) => isSubflowItemAssigned(recordId, item)).length
}

function isSubflowItemAssigned(recordId, item) {
  if (!recordId || !item) {
    return false
  }
  const currentValue = assignmentMap[recordId]?.[item.id]
  return currentValue !== null && currentValue !== undefined && currentValue !== ''
}

function isSubflowItemSubmitted(item) {
  return item?.itemStatus === reviewPendingDetectionStatus
}

function setSubflowItemFilter(key) {
  subflowItemFilter.value = key
  selectedSubflowItemIds.value = []
  pendingDetectorId.value = undefined
}

function getItemResultLabel(item) {
  if (!item) {
    return '-'
  }
  if (item.itemStatus === WAIT_ASSIGN_STATUS) {
    return '待分配人员'
  }
  if (item.itemStatus === WAIT_DETECT_STATUS) {
    return '待录入结果'
  }
  if (item.itemStatus === rejectedDetectionStatus) {
    return '审核驳回，待重检'
  }
  if (item.itemStatus === reviewPendingDetectionStatus) {
    return getResultValueStatusLabel(item)
  }
  return getResultValueStatusLabel(item)
}

function getItemResultClass(item) {
  if (!item) {
    return 'info'
  }
  if (item.itemStatus === WAIT_ASSIGN_STATUS) {
    return 'warning'
  }
  if (item.itemStatus === WAIT_DETECT_STATUS) {
    return 'info'
  }
  if (item.itemStatus === rejectedDetectionStatus) {
    return 'danger'
  }
  return getResultValueStatusClass(item)
}

function getRecordRemark(row) {
  if (!row) {
    return '-'
  }
  if (row.__sampleOnly) {
    return row.abnormalRemark || '历史样品未自动生成参数子流程'
  }
  const total = Number(row.parameterCount || 0)
  const assigned = Number(row.assignedCount || 0)
  const completed = Number(row.completedCount || 0)
  if (row.abnormalRemark) {
    return translateWorkflowText(row.abnormalRemark)
  }
  if (!total) {
    return '当前主流程下暂无参数子流程数据'
  }
  if (assigned < total) {
    return `已完成 ${assigned}/${total} 个参数分配，仍有 ${total - assigned} 个参数待分配检测员`
  }
  if (completed <= 0) {
    return '参数子流程已全部分配完成，等待检测员录入结果'
  }
  if (completed < total) {
    return `已有 ${completed}/${total} 个参数提交结果，剩余 ${total - completed} 个参数待检测`
  }
  if (row.detectionResult === abnormalDetectionResult) {
    return '全部参数已提交完成，存在异常项，等待结果审查'
  }
  return '全部参数已提交完成，等待结果审查'
}

function ensureAssignmentState(recordId, items) {
  assignmentMap[recordId] = items.reduce((result, item) => {
    result[item.id] = item.detectorId ?? null
    return result
  }, {})
}

function getAssignedDetectorId(recordId, itemId) {
  return assignmentMap[recordId]?.[itemId] ?? null
}

function updateAssignedDetectorId(recordId, itemId, value) {
  if (!assignmentMap[recordId]) {
    assignmentMap[recordId] = {}
  }
  assignmentMap[recordId][itemId] = value ?? null
}

function getDetectorOptionLabel(option) {
  if (!option) {
    return '-'
  }
  return option.displayName || option.realName || option.username || '-'
}

function getDetectorSearchText(option) {
  return [
    option?.displayName,
    option?.realName,
    option?.username,
    option?.nickname,
    option?.phone,
    option?.id
  ]
    .filter((value) => value !== null && value !== undefined && value !== '')
    .join(' ')
    .toLowerCase()
}

function getAssignedDetectorName(recordId, item) {
  const detectorId = getAssignedDetectorId(recordId, item.id)
  if (detectorId == null || detectorId === '') {
    return '待分配'
  }
  const option = detectorOptions.value.find((candidate) => String(candidate.id) === String(detectorId))
  return getDetectorOptionLabel(option)
}

function isSubflowItemSelected(itemId) {
  return selectedSubflowItemIds.value.includes(itemId)
}

async function showDetectorAssignPanel() {
  await loadDetectorOptions()
}

async function loadDetectorOptions(force = false) {
  if (!force && detectorOptionsLoaded.value) {
    return
  }
  const result = await fetchDetectionDetectorsApi()
  detectorOptions.value = (result || []).map((item) => ({
    ...item,
    id: item.userId ?? item.id
  }))
  detectorOptionsLoaded.value = true
}

function toggleSubflowItemSelected(itemId, checked) {
  if (checked) {
    if (!selectedSubflowItemIds.value.includes(itemId)) {
      selectedSubflowItemIds.value = [...selectedSubflowItemIds.value, itemId]
    }
    showDetectorAssignPanel()
    return
  }
  selectedSubflowItemIds.value = selectedSubflowItemIds.value.filter((id) => id !== itemId)
}

function selectAllAssignableSubflowItems() {
  selectedSubflowItemIds.value = assignableSubflowItems.value.map((item) => item.id)
  if (selectedSubflowItemIds.value.length) {
    showDetectorAssignPanel()
  }
}

function clearSelectedSubflowItems() {
  selectedSubflowItemIds.value = []
  pendingDetectorId.value = undefined
}

function isPendingDetectorSelected(detectorId) {
  return activeDetectorId.value !== undefined && String(activeDetectorId.value) === String(detectorId)
}

function selectPendingDetector(detectorId) {
  const recordId = currentSubflowRecord.value?.id
  if (!recordId || !selectedAssignableItems.value.length) {
    ElMessage.warning('请先勾选需要分配的检测参数')
    return
  }
  const selectedItems = [...selectedAssignableItems.value]
  selectedItems.forEach((item) => {
    updateAssignedDetectorId(recordId, item.id, detectorId)
  })
  pendingDetectorId.value = detectorId
  selectedSubflowItemIds.value = []
  const detectorLabel = getDetectorOptionLabel(detectorOptions.value.find((option) => String(option.id) === String(detectorId)))
  ElMessage.success(`已将 ${selectedItems.length} 个参数分配给${detectorLabel}，请点击提交保存`)
}

function resetSubflowDialog() {
  currentSubflowRecord.value = null
  selectedSubflowItemIds.value = []
  subflowItemFilter.value = 'all'
  pendingDetectorId.value = undefined
}

function resetResultForm() {
  resultForm.recordId = null
  resultForm.sampleId = null
  resultForm.sampleNo = ''
  resultForm.detectionTypeId = null
  resultForm.detectionTypeName = ''
  resultForm.parameterName = ''
  resultForm.abnormalRemark = ''
  resultForm.items = []
  resultDialogItemId.value = null
  resultDialogItemStatus.value = ''
  resultDialogMode.value = 'item'
}

function isResultValueAbnormal(item) {
  if (!item || item.resultValue == null || item.resultValue === '') {
    return false
  }
  if (item.optionValues) {
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

function getResultRangeError(item) {
  if (!isResultValueAbnormal(item)) {
    return ''
  }
  const parameterName = item.parameterName || '当前检测参数'
  return `检测结果超出标准范围：${parameterName}，标准范围 ${formatStandardRange(item.standardMin, item.standardMax, item.unit, item.optionValues)}，请检查结果值`
}

function getResultDisplayValue(item) {
  if (!item || item.resultValue == null || item.resultValue === '') return '-'
  if (item.optionValues) {
    const options = parseOptionValuesArray(item.optionValues)
    const index = Number(item.resultValue)
    return options[index] ?? item.resultValue
  }
  return item.resultValue
}

function getResultValueStatusLabel(item) {
  if (!item || item.resultValue == null || item.resultValue === '') {
    return '待录入'
  }
  return isResultValueAbnormal(item) ? '异常' : '正常'
}

function getResultValueStatusClass(item) {
  const label = getResultValueStatusLabel(item)
  if (label === '异常') {
    return 'danger'
  }
  if (label === '正常') {
    return 'success'
  }
  return 'info'
}

async function loadRecordDetail(recordId, force = false) {
  if (!recordId || recordId.toString().startsWith('pending-sample-')) {
    return
  }
  if (!force && detailMap[recordId]) {
    return
  }
  detailLoadingMap[recordId] = true
  try {
    const detail = await fetchDetectionDetailApi(recordId)
    detailMap[recordId] = detail || { items: [] }
    ensureAssignmentState(recordId, detailMap[recordId].items || [])
  } finally {
    detailLoadingMap[recordId] = false
  }
}

async function openResultDialog(row, item) {
  if (!row || row.__sampleOnly) {
    return
  }
  await loadRecordDetail(row.id)
  const detail = detailMap[row.id]
  const currentItem = (detail?.items || []).find((detailItem) => detailItem.id === item?.id)
  if (!currentItem) {
    ElMessage.warning('当前子流程明细不存在，请刷新后重试')
    return
  }
  resetResultForm()
  resultForm.recordId = row.id
  resultForm.sampleId = row.sampleId || detail?.record?.sampleId || null
  resultForm.sampleNo = row.sampleNo || detail?.record?.sampleNo || ''
  resultForm.detectionTypeId = row.detectionTypeId || detail?.record?.detectionTypeId || null
  resultForm.detectionTypeName = row.detectionTypeName || detail?.record?.detectionTypeName || ''
  resultForm.parameterName = currentItem.parameterName || ''
  resultForm.abnormalRemark = detail?.record?.abnormalRemark || ''
  resultDialogMode.value = 'item'
  resultDialogItemId.value = currentItem.id
  resultDialogItemStatus.value = currentItem.itemStatus || ''
  resultForm.items = [{
    id: currentItem.id,
    parameterId: currentItem.parameterId,
    parameterName: currentItem.parameterName || '',
    methodName: currentItem.methodName || '',
    standardMin: currentItem.standardMin,
    standardMax: currentItem.standardMax,
    optionValues: currentItem.optionValues || '',
    referenceStandard: currentItem.referenceStandard || '',
    unit: currentItem.unit || '',
    resultValue: currentItem.resultValue == null ? null : Number(currentItem.resultValue)
  }]
  resultDialogVisible.value = true
}

async function openRecordResultDialog(row) {
  if (!canViewRecordResults(row)) {
    return
  }
  await loadRecordDetail(row.id)
  const detail = detailMap[row.id]
  const items = detail?.items || []
  if (!items.length) {
    ElMessage.warning('当前主流程下暂无可查看的结果明细')
    return
  }
  resetResultForm()
  resultForm.recordId = row.id
  resultForm.sampleId = row.sampleId || detail?.record?.sampleId || null
  resultForm.sampleNo = row.sampleNo || detail?.record?.sampleNo || ''
  resultForm.detectionTypeId = row.detectionTypeId || detail?.record?.detectionTypeId || null
  resultForm.detectionTypeName = row.detectionTypeName || detail?.record?.detectionTypeName || ''
  resultForm.parameterName = '全部参数'
  resultForm.abnormalRemark = detail?.record?.abnormalRemark || ''
  resultDialogMode.value = 'record'
  resultDialogItemId.value = null
  resultDialogItemStatus.value = reviewPendingDetectionStatus
  resultForm.items = items.map((currentItem) => ({
    id: currentItem.id,
    parameterId: currentItem.parameterId,
    parameterName: currentItem.parameterName || '',
    methodName: currentItem.methodName || '',
    standardMin: currentItem.standardMin,
    standardMax: currentItem.standardMax,
    optionValues: currentItem.optionValues || '',
    referenceStandard: currentItem.referenceStandard || '',
    unit: currentItem.unit || '',
    resultValue: currentItem.resultValue == null ? null : Number(currentItem.resultValue)
  }))
  resultDialogVisible.value = true
}

async function openSubflowDialog(row) {
  currentSubflowRecord.value = row
  selectedSubflowItemIds.value = []
  pendingDetectorId.value = undefined
  subflowDialogVisible.value = true
  if (row?.__sampleOnly) {
    return
  }
  await loadRecordDetail(row.id)
  if (canAssignRow(row)) {
    await loadDetectorOptions()
  }
}

async function saveAssignments(row) {
  const detailItems = getDetailItems(row.id)
  if (!detailItems.length) {
    ElMessage.warning('当前主流程下没有可分配的参数子流程')
    return
  }
  await assignDetectionDetectorsApi(row.id, {
    items: detailItems.map((item) => ({
      itemId: item.id,
      detectorId: getAssignedDetectorId(row.id, item.id)
    }))
  })
  ElMessage.success('检测员分配已提交')
  await Promise.all([
    loadRecordDetail(row.id, true),
    loadData()
  ])
  subflowDialogVisible.value = false
}

function handleOcrTrigger() {
  ElMessage.info('未检测到可适配的OCR设备')
}

async function submitDetectionResult() {
  if (!resultForm.sampleId || !resultForm.detectionTypeId) {
    ElMessage.warning('当前检测主流程缺少样品或检测套餐信息，暂不能提交')
    return
  }
  if (!resultForm.items.length || resultForm.items.some((item) => item.resultValue == null || item.resultValue === '')) {
    ElMessage.warning('请完整填写全部检测参数的结果值')
    return
  }
  const abnormalItem = resultForm.items.find((item) => isResultValueAbnormal(item))
  if (abnormalItem) {
    ElMessage.warning(getResultRangeError(abnormalItem))
    return
  }

  resultSubmitting.value = true
  try {
    const recordId = resultForm.recordId
    await submitDetectionApi({
      recordId: resultForm.recordId,
      itemId: resultDialogItemId.value,
      sampleId: resultForm.sampleId,
      detectionTypeId: resultForm.detectionTypeId,
      detectionTypeName: resultForm.detectionTypeName,
      abnormalRemark: resultForm.abnormalRemark,
      items: resultForm.items.map((item) => ({
        parameterId: item.parameterId,
        parameterName: item.parameterName,
        standardMin: item.standardMin,
        standardMax: item.standardMax,
        resultValue: item.resultValue,
        unit: item.unit,
        optionValues: item.optionValues || ''
      }))
    })
    resultDialogVisible.value = false
    ElMessage.success('检测结果已提交')
    await Promise.all([
      loadRecordDetail(recordId, true),
      loadData()
    ])
  } finally {
    resultSubmitting.value = false
  }
}

function getActiveDetectionStatusFilter() {
  return query.detectionStatus || undefined
}

function getDetectionStatusByStatKey(key) {
  const statStatusMap = {
    waitAssign: WAIT_ASSIGN_STATUS,
    waitDetect: WAIT_DETECT_STATUS,
    pendingReview: reviewPendingDetectionStatus,
    approved: approvedDetectionStatus,
    rejected: rejectedDetectionStatus
  }
  return statStatusMap[key]
}

function syncActiveStatByQuery() {
  const matchedEntry = Object.entries({
    waitAssign: WAIT_ASSIGN_STATUS,
    waitDetect: WAIT_DETECT_STATUS,
    pendingReview: reviewPendingDetectionStatus,
    approved: approvedDetectionStatus,
    rejected: rejectedDetectionStatus
  }).find(([, status]) => status === query.detectionStatus)
  activeStatKey.value = matchedEntry?.[0] || 'all'
}

async function loadData() {
  const currentVersion = ++loadDataVersion
  const detectionQuery = {
    ...query,
    scope: baseScene.value.key,
    detectionStatus: getActiveDetectionStatusFilter(),
    mine: query.mine === '' ? undefined : query.mine
  }
  const summaryQuery = {
    scope: baseScene.value.key,
    mine: query.mine === '' ? undefined : query.mine
  }
  const [detectionResult, summaryResult] = await Promise.all([
    fetchDetectionsApi(detectionQuery),
    fetchDetectionRecordSummaryApi(summaryQuery)
  ])
  if (currentVersion !== loadDataVersion) {
    return
  }

  records.value = detectionResult.records || []
  total.value = Number(detectionResult.total || 0)
  if (!records.value.length && total.value > 0 && query.pageNum > 1) {
    query.pageNum = 1
    await loadData()
    return
  }

  summary.total = Number(summaryResult.total || 0)
  summary.waitAssignCount = Number(summaryResult.waitAssignCount || 0)
  summary.waitDetectCount = Number(summaryResult.waitDetectCount || 0)
  summary.pendingReviewCount = Number(summaryResult.pendingReviewCount || 0)
  summary.approvedCount = Number(summaryResult.approvedCount || 0)
  summary.rejectedCount = Number(summaryResult.rejectedCount || 0)
}

async function handleExport() {
  try {
    await exportDetectionsApi({
      ...query,
      scope: baseScene.value.key,
      detectionStatus: getActiveDetectionStatusFilter(),
      mine: query.mine === '' ? undefined : query.mine
    })
    ElMessage.success('检测流程导出成功')
  } catch (error) {
    ElMessage.error(error.message || '检测流程导出失败')
  }
}

onMounted(async () => {
  syncRouteState()
  await loadData()
})

watch(() => route.fullPath, () => {
  syncRouteState()
  loadData()
})

</script>

<style scoped>
.detection-page {
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

.table-action-group {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  justify-content: center;
  flex-wrap: nowrap;
  white-space: nowrap;
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

.subflow-panel {
  display: grid;
  gap: 12px;
  padding: 8px 6px;
}

.subflow-panel--dialog {
  position: relative;
  overflow: visible;
}

.subflow-content {
  display: grid;
  gap: 12px;
  min-height: 0;
}

.subflow-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.subflow-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.subflow-summary__chip {
  border: none;
  cursor: pointer;
}

.subflow-summary__chip.is-active {
  outline: 2px solid color-mix(in srgb, var(--brand) 42%, #ffffff 58%);
  outline-offset: 2px;
}

.subflow-readonly-panel {
  display: grid;
  gap: 12px;
  padding: 14px;
  border: 1px solid color-mix(in srgb, var(--brand) 10%, #ffffff 90%);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.9);
}

.subflow-readonly-table {
  width: 100%;
}

.subflow-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.subflow-list--single {
  grid-template-columns: 1fr;
}

.subflow-card {
  display: grid;
  gap: 10px;
  padding: 14px;
  border: 1px solid var(--line-soft);
  border-radius: 16px;
  background: linear-gradient(180deg, rgba(250, 252, 255, 0.98), rgba(244, 248, 255, 0.92));
}

.subflow-card__head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.subflow-card__head strong {
  display: block;
  color: var(--text-main);
  font-size: 15px;
  line-height: 1.5;
}

.subflow-card__head p {
  margin: 6px 0 0;
  color: var(--text-sub);
  font-size: 13px;
  line-height: 1.6;
}

.subflow-meta {
  display: grid;
  gap: 8px;
  color: var(--text-sub);
  font-size: 13px;
  line-height: 1.6;
}

.subflow-result {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 14px;
  background: color-mix(in srgb, var(--brand) 5%, #ffffff 95%);
}

.subflow-result__label {
  color: var(--text-sub);
  font-size: 13px;
}

.subflow-batch-panel {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  min-height: 0;
  gap: 14px;
  padding: 16px;
  border: 1px solid color-mix(in srgb, var(--brand) 12%, #ffffff 88%);
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(248, 251, 255, 0.98), rgba(242, 247, 255, 0.94));
}

.subflow-batch-panel__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.subflow-batch-panel__head strong {
  display: block;
  color: var(--text-main);
  font-size: 15px;
  line-height: 1.5;
}

.subflow-batch-panel__head p {
  margin: 6px 0 0;
  color: var(--text-sub);
  font-size: 13px;
  line-height: 1.7;
}

.subflow-batch-panel__actions {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  flex-wrap: nowrap;
  white-space: nowrap;
}

.subflow-batch-panel__count {
  color: var(--text-sub);
  font-size: 13px;
  line-height: 1.6;
}

.subflow-pick-list {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  align-content: start;
  gap: 12px;
  min-height: 0;
  overflow: auto;
  padding-right: 4px;
}

.subflow-submit-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  grid-column: 1 / -1;
  padding-top: 14px;
  border-top: 1px solid color-mix(in srgb, var(--brand) 12%, #ffffff 88%);
}

.subflow-pick-item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid var(--line-soft);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.92);
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.subflow-pick-item:hover,
.subflow-pick-item.is-selected {
  border-color: color-mix(in srgb, var(--brand) 34%, #ffffff 66%);
  box-shadow: var(--shadow-sm);
}

.subflow-pick-item.is-selected {
  transform: translateY(-1px);
}

.subflow-pick-item__body {
  min-width: 0;
}

.subflow-pick-item__body strong {
  display: block;
  color: var(--text-main);
  font-size: 14px;
  line-height: 1.5;
}

.subflow-pick-item__body p,
.subflow-pick-item__body span {
  display: block;
  margin: 4px 0 0;
  color: var(--text-sub);
  font-size: 13px;
  line-height: 1.6;
  word-break: break-word;
}

.detector-assign-dialog {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  min-height: 0;
  gap: 12px;
  overflow: hidden;
}

.subflow-assign-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  grid-template-rows: minmax(0, 1fr) auto;
  gap: 10px 16px;
  height: 100%;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.subflow-assign-main {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.detector-side-panel {
  display: flex;
  flex-direction: column;
  min-height: 0;
  margin-top: 16px;
  padding: 18px;
  border: 1px solid color-mix(in srgb, var(--brand) 12%, #ffffff 88%);
  border-radius: 22px;
  background: #ffffff;
  box-shadow: var(--shadow-lg);
  width: 620px;
}

.detector-side-panel--inline {
  width: auto;
  height: 100%;
  min-height: 0;
  margin-top: 0;
  padding: 16px;
  box-shadow: none;
  overflow: hidden;
}

.detector-side-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
  padding-bottom: 14px;
  border-bottom: 1px solid color-mix(in srgb, var(--brand) 14%, #ffffff 86%);
}

.detector-side-panel__header strong {
  color: var(--text-main);
  font-size: 16px;
  line-height: 1.5;
}

.detector-side-panel__search {
  width: 240px;
  max-width: 58%;
}

.detector-side-panel__close {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 999px;
  background: transparent;
  color: var(--text-sub);
  font-size: 24px;
  line-height: 1;
  cursor: pointer;
}

.detector-side-panel__close:hover {
  background: color-mix(in srgb, var(--brand) 8%, #ffffff 92%);
  color: var(--brand);
}

.detector-assign-dialog__summary {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.detector-assign-dialog__list {
  display: grid;
  align-content: start;
  gap: 10px;
  min-height: 0;
  overflow: auto;
  padding-right: 4px;
}

.detector-empty {
  padding: 20px 12px;
  color: var(--text-sub);
  font-size: 13px;
  text-align: center;
}

.subflow-detector-card {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid var(--line-soft);
  border-radius: 14px;
  background: #ffffff;
  display: grid;
  gap: 2px;
  position: relative;
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.subflow-detector-card strong {
  display: block;
  color: var(--text-main);
  font-size: 14px;
  line-height: 1.5;
  text-align: left;
}

.subflow-detector-card span {
  margin: 4px 0 0;
  color: var(--text-sub);
  font-size: 12px;
  line-height: 1.6;
  text-align: left;
}


.subflow-detector-card:hover {
  border-color: color-mix(in srgb, var(--brand) 34%, #ffffff 66%);
  box-shadow: var(--shadow-sm);
  transform: translateY(-1px);
}

.subflow-detector-card.is-selected {
  border-color: color-mix(in srgb, var(--brand) 48%, #ffffff 52%);
  box-shadow: 0 10px 24px rgba(99, 102, 241, 0.14);
}

.detector-check-mark {
  position: absolute;
  top: 10px;
  right: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  margin: 0;
  border-radius: 50%;
  background: #22c55e;
  color: #ffffff !important;
  font-size: 13px !important;
  font-weight: 800;
  line-height: 1;
}

.subflow-assign {
  display: grid;
  gap: 8px;
}

.subflow-assign__value {
  min-height: 40px;
  padding: 9px 12px;
  border: 1px solid var(--line-soft);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.88);
  color: var(--text-main);
  font-size: 13px;
  line-height: 1.6;
}

.subflow-card__action {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  min-height: 40px;
}

.subflow-assign label {
  color: var(--text-main);
  font-size: 13px;
  font-weight: 600;
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

.result-dialog {
  display: grid;
  gap: 12px;
}

.result-dialog__summary {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.result-dialog__note {
  margin-bottom: 2px;
}

.result-dialog__table {
  width: 100%;
}

:deep(.result-field-header .cell) {
  color: #d4380d;
  font-weight: 700;
}

.result-dialog__form {
  margin-top: 4px;
}

:deep(.el-dialog.detection-subflow-dialog),
:deep(.detection-subflow-dialog .el-dialog) {
  max-width: min(960px, calc(100vw - 64px));
  overflow: visible;
  transition: width 0.22s ease, left 0.22s ease, transform 0.22s ease;
}

:deep(.el-dialog.detection-subflow-dialog--assign),
:deep(.detection-subflow-dialog--assign .el-dialog) {
  display: flex;
  flex-direction: column;
  height: min(780px, calc(100vh - 40px));
  margin-top: 0 !important;
  margin-bottom: 0 !important;
  max-width: min(1280px, calc(100vw - 64px));
  overflow: hidden;
}

:global(.el-overlay-dialog:has(.detection-subflow-dialog--assign)) {
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

:deep(.el-dialog.detection-subflow-dialog .el-dialog__body),
:deep(.detection-subflow-dialog .el-dialog__body) {
  padding-top: 18px;
  padding-left: 20px;
  padding-right: 20px;
  overflow: visible;
}

:deep(.el-dialog.detection-subflow-dialog--assign .el-dialog__header),
:deep(.detection-subflow-dialog--assign .el-dialog__header) {
  flex: none;
  padding: 14px 24px 8px;
  margin-right: 0;
  border-bottom: 1px solid var(--line-soft);
}

:deep(.el-dialog.detection-subflow-dialog--assign .el-dialog__title),
:deep(.detection-subflow-dialog--assign .el-dialog__title) {
  font-size: 16px;
  font-weight: 700;
  line-height: 22px;
}

:deep(.el-dialog.detection-subflow-dialog--assign .el-dialog__headerbtn),
:deep(.detection-subflow-dialog--assign .el-dialog__headerbtn) {
  top: 8px;
  right: 16px;
  width: 30px;
  height: 30px;
}

:deep(.el-dialog.detection-subflow-dialog--assign .el-dialog__body),
:deep(.detection-subflow-dialog--assign .el-dialog__body) {
  flex: 1;
  min-height: 0;
  padding: 10px 24px 16px;
  overflow: hidden;
}

:deep(.detection-subflow-dialog--assign) .result-dialog__summary {
  gap: 6px;
  margin-bottom: 6px;
}

:deep(.detection-subflow-dialog--assign) .binding-editor__chip {
  min-height: 28px;
  padding: 0 10px;
}

:deep(.detection-subflow-dialog--assign) .subflow-head {
  margin-bottom: 6px;
}

:deep(.detection-subflow-dialog--assign) .subflow-summary {
  gap: 6px;
}

:deep(.detection-subflow-dialog--assign) .subflow-panel--dialog,
:deep(.detection-subflow-dialog--assign) .subflow-content {
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

:deep(.detection-subflow-dialog--assign) .subflow-panel--dialog,
:deep(.detection-subflow-dialog--assign) .subflow-content,
:deep(.detection-subflow-dialog--assign) .subflow-assign-main,
:deep(.detection-subflow-dialog--assign) .detector-side-panel--inline,
:deep(.detection-subflow-dialog--assign) .detector-assign-dialog {
  display: flex;
  flex-direction: column;
}

:deep(.detection-subflow-dialog--assign) .subflow-content {
  gap: 6px;
}

:deep(.detection-subflow-dialog--assign) .subflow-batch-panel,
:deep(.detection-subflow-dialog--assign) .detector-side-panel--inline {
  padding: 12px 14px;
}

:deep(.detection-subflow-dialog--assign) .subflow-batch-panel {
  gap: 10px;
}

:deep(.detection-subflow-dialog--assign) .detector-side-panel__header {
  margin-bottom: 10px;
  padding-bottom: 10px;
}

:deep(.detection-subflow-dialog--assign) .detector-assign-dialog {
  gap: 8px;
}

:deep(.detection-subflow-dialog--assign) .subflow-submit-footer {
  padding-top: 8px;
}

:deep(.detection-subflow-dialog--assign) .result-dialog__summary,
:deep(.detection-subflow-dialog--assign) .subflow-head,
:deep(.detection-subflow-dialog--assign) .subflow-submit-footer,
:deep(.detection-subflow-dialog--assign) .detector-side-panel__header,
:deep(.detection-subflow-dialog--assign) .detector-assign-dialog__summary {
  flex: none;
}

:deep(.detection-subflow-dialog--assign) .subflow-batch-panel,
:deep(.detection-subflow-dialog--assign) .detector-assign-dialog {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

:deep(.detection-subflow-dialog--assign) .subflow-pick-list,
:deep(.detection-subflow-dialog--assign) .detector-assign-dialog__list {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding-right: 4px;
}

@media (min-width: 1320px) {
  :deep(.el-dialog.detection-subflow-dialog--assign .el-dialog__body),
  :deep(.detection-subflow-dialog--assign .el-dialog__body) {
    overflow: hidden;
  }

  .subflow-panel--dialog {
    height: 100%;
    min-height: 0;
    overflow: hidden;
    display: flex;
    flex-direction: column;
  }

  .subflow-panel--dialog .result-dialog__summary,
  .subflow-panel--dialog .subflow-head {
    flex: none;
  }

  .subflow-content {
    flex: 1;
    min-height: 0;
    display: flex;
    flex-direction: column;
  }

  .subflow-batch-panel {
    flex: 1;
    min-height: 0;
    overflow: hidden;
  }

  .subflow-pick-list {
    flex: 1;
    overflow: auto;
    padding-right: 4px;
  }

  .subflow-submit-footer {
    flex: none;
  }

  .detector-assign-dialog {
    flex: 1;
    min-height: 0;
    display: flex;
    flex-direction: column;
  }

  .detector-assign-dialog__summary {
    flex: none;
  }

  .detector-assign-dialog__list {
    flex: 1;
    overflow: auto;
    padding-right: 4px;
  }
}

.subflow-empty {
  padding: 20px 14px;
  color: var(--text-sub);
  line-height: 1.8;
}

.subflow-empty strong {
  display: block;
  color: var(--text-main);
  margin-bottom: 6px;
}

@media (max-width: 1100px) {
  .subflow-assign-layout {
    grid-template-columns: 1fr;
  }

  .subflow-list,
  .subflow-pick-list {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .page-hero,
  .scene-grid {
    grid-template-columns: 1fr;
  }

  .hero-tags {
    justify-content: flex-start;
  }

  .subflow-head {
    align-items: stretch;
    flex-direction: column;
  }

  .subflow-batch-panel__head {
    flex-direction: column;
  }

  .subflow-batch-panel__actions {
    white-space: normal;
  }

  .subflow-pick-item {
    grid-template-columns: auto minmax(0, 1fr);
  }

  .result-dialog__summary {
    gap: 8px;
  }
}
</style>
