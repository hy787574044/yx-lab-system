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
                  placeholder="请输入样品编号、封签号或检测套餐"
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
          :data="pagedRecords"
          stripe
          row-key="id"
          height="100%"
          :empty-text="baseScene.emptyText"
        >
          <el-table-column prop="sampleNo" label="样品编号" min-width="170" />
          <el-table-column prop="sealNo" label="封签编号" min-width="170" />
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
          <el-table-column label="检测结果" width="120" header-cell-class-name="cell-center" class-name="cell-center">
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
          :total="displayTotal"
          @change="loadData"
        />
      </div>
    </section>

    <el-dialog
      v-model="subflowDialogVisible"
      :class="['detection-subflow-dialog', { 'detection-subflow-dialog--shifted': detectorAssignDialogVisible }]"
      title="检测项明细"
      :style="subflowDialogStyle"
      :width="detectorAssignDialogVisible ? '760px' : '960px'"
      destroy-on-close
      @closed="resetSubflowDialog"
    >
      <div v-if="currentSubflowRecord" class="subflow-panel subflow-panel--dialog">
        <div class="result-dialog__summary">
          <span class="binding-editor__chip">样品编号<strong>{{ currentSubflowRecord.sampleNo || '-' }}</strong></span>
          <span class="binding-editor__chip">封签编号<strong>{{ currentSubflowRecord.sealNo || '-' }}</strong></span>
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

        <div v-else-if="currentSubflowItems.length">
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
            <el-button
              v-if="canAssignRow(currentSubflowRecord)"
              type="primary"
              @click="saveAssignments(currentSubflowRecord)"
            >
              保存人员分配
            </el-button>
          </div>

          <div v-if="canAssignRow(currentSubflowRecord)" class="subflow-batch-panel">
            <div class="subflow-batch-panel__head">
              <div>
                <strong>检测参数列表</strong>
                <p>可单选或多选检测参数，勾选后右侧会自动弹出检测人员列表。</p>
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
                <span class="status-chip" :class="getItemStatusClass(item.itemStatus)">
                  {{ getItemStatusLabel(item.itemStatus) }}
                </span>
              </label>
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
              <el-table-column prop="parameterName" label="检测参数" min-width="150" show-overflow-tooltip />
              <el-table-column prop="methodName" label="检测方法" min-width="180" show-overflow-tooltip>
                <template #default="{ row }">{{ row.methodName || '-' }}</template>
              </el-table-column>
              <el-table-column label="检测步骤" min-width="220" show-overflow-tooltip>
                <template #default="{ row }">{{ getMethodBasis(row) }}</template>
              </el-table-column>
              <el-table-column label="检测人员" min-width="120">
                <template #default="{ row }">{{ row.detectorName || '-' }}</template>
              </el-table-column>
              <el-table-column label="标准范围" min-width="140">
                <template #default="{ row }">
                  {{ formatStandardRange(row.standardMin, row.standardMax, row.unit) }}
                </template>
              </el-table-column>
              <el-table-column prop="referenceStandard" label="参考范围" min-width="150" show-overflow-tooltip>
                <template #default="{ row }">{{ row.referenceStandard || '-' }}</template>
              </el-table-column>
              <el-table-column label="检测值" min-width="110">
                <template #default="{ row }">{{ row.resultValue ?? '-' }}</template>
              </el-table-column>
              <el-table-column label="判定结果" width="110" header-cell-class-name="cell-center" class-name="cell-center">
                <template #default="{ row }">
                  <span class="status-chip" :class="getItemResultClass(row)">
                    {{ getItemResultLabel(row) }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column label="子流程状态" width="120" header-cell-class-name="cell-center" class-name="cell-center">
                <template #default="{ row }">
                  <span class="status-chip" :class="getItemStatusClass(row.itemStatus)">
                    {{ getItemStatusLabel(row.itemStatus) }}
                  </span>
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

        <Teleport to="body">
          <aside
            v-if="detectorAssignDialogVisible && currentSubflowRecord && canAssignRow(currentSubflowRecord)"
            class="detector-side-panel"
            :style="detectorPanelStyle"
          >
          <div class="detector-side-panel__header">
            <strong>选择检测人员</strong>
            <button
              type="button"
              class="detector-side-panel__close"
              @click="detectorAssignDialogVisible = false"
            >
              ×
            </button>
          </div>

          <div class="detector-assign-dialog">
            <div class="detector-assign-dialog__summary">
              <span class="binding-editor__chip">已选参数<strong>{{ selectedAssignableItems.length }}</strong></span>
              <span class="binding-editor__chip">可选人员<strong>{{ detectorOptions.length }}</strong></span>
            </div>

            <section class="detector-assign-dialog__picked">
              <div class="detector-assign-dialog__head">
                <strong>待分配检测参数</strong>
                <span>选择人员后点击确认，才会回填到左侧列表</span>
              </div>
              <div class="detector-assign-dialog__tags">
                <span
                  v-for="item in selectedAssignableItems"
                  :key="`assign-${item.id}`"
                  class="detector-assign-dialog__tag"
                >
                  {{ item.parameterName }}
                </span>
              </div>
            </section>

            <section class="detector-assign-dialog__list">
              <button
                type="button"
                :class="['subflow-detector-card', 'subflow-detector-card--clear', { 'is-selected': pendingDetectorId === null }]"
                @click="selectPendingDetector(null)"
              >
                <strong>清空分配</strong>
                <span>将当前已选参数恢复为待分配</span>
              </button>
              <button
                v-for="option in detectorOptions"
                :key="option.id"
                type="button"
                :class="['subflow-detector-card', { 'is-selected': isPendingDetectorSelected(option.id) }]"
                @click="selectPendingDetector(option.id)"
              >
                <strong>{{ getDetectorOptionLabel(option) }}</strong>
                <span>{{ isPendingDetectorSelected(option.id) ? '已选择，点击下方确认后生效' : '点击选择该检测人员' }}</span>
              </button>
            </section>

            <div class="detector-assign-dialog__footer">
              <span>{{ getPendingDetectorLabel() }}</span>
              <el-button type="primary" :disabled="!selectedAssignableItems.length" @click="confirmSelectedDetector">
                确认
              </el-button>
            </div>
            </div>
          </aside>
        </Teleport>
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
          <span class="binding-editor__chip">封签编号<strong>{{ resultForm.sealNo || '-' }}</strong></span>
          <span class="binding-editor__chip">检测套餐<strong>{{ resultForm.detectionTypeName || '-' }}</strong></span>
          <span class="binding-editor__chip">{{ resultDialogScopeLabel }}<strong>{{ resultForm.parameterName || '-' }}</strong></span>
          <span class="binding-editor__chip">参数数<strong>{{ resultForm.items.length }}</strong></span>
          <span class="binding-editor__chip">正常<strong>{{ resultNormalCount }}</strong></span>
          <span class="binding-editor__chip">异常<strong>{{ resultAbnormalCount }}</strong></span>
        </div>

        <div class="panel-note result-dialog__note">
          {{ resultDialogNote }}
        </div>

        <el-table
          class="list-table result-dialog__table"
          :data="resultForm.items"
          stripe
          border
          max-height="460"
        >
          <el-table-column prop="parameterName" label="检测参数" min-width="150" />
          <el-table-column prop="methodName" label="检测方法" min-width="220" show-overflow-tooltip />
          <el-table-column label="标准范围" min-width="140">
            <template #default="{ row }">
              {{ formatStandardRange(row.standardMin, row.standardMax, row.unit) }}
            </template>
          </el-table-column>
          <el-table-column prop="referenceStandard" label="参考范围" min-width="160" show-overflow-tooltip>
            <template #default="{ row }">{{ row.referenceStandard || '-' }}</template>
          </el-table-column>
          <el-table-column label="检测值" min-width="180">
            <template #default="{ row }">
              <span v-if="resultDialogReadonly">{{ row.resultValue ?? '-' }}</span>
              <el-input-number
                v-else
                v-model="row.resultValue"
                :precision="4"
                :step="0.01"
                controls-position="right"
                style="width: 100%"
              />
            </template>
          </el-table-column>
          <el-table-column label="判定结果" width="120" header-cell-class-name="cell-center" class-name="cell-center">
            <template #default="{ row }">
              <span class="status-chip" :class="getResultValueStatusClass(row)">
                {{ getResultValueStatusLabel(row) }}
              </span>
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
          提交检测结果
        </el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
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
  fetchDetectionMethodsApi,
  fetchDetectionsApi,
  fetchSamplesApi,
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
  availableDetectionSampleStatuses,
  translateWorkflowText,
  waitAssignDetectionStatus,
  waitDetectDetectionStatus
} from '../utils/labEnums'

const props = defineProps({
  forcedScenePath: {
    type: String,
    default: ''
  }
})

const route = useRoute()

const WAIT_ASSIGN_STATUS = waitAssignDetectionStatus
const WAIT_DETECT_STATUS = waitDetectDetectionStatus
const MAX_LOAD_SIZE = 500

const query = reactive({
  keyword: '',
  detectionStatus: '',
  mine: '',
  pageNum: 1,
  pageSize: DEFAULT_PAGE_SIZE
})
const records = ref([])
const samplePool = ref([])
const detectorOptions = ref([])
const detectionMethodOptions = ref([])
const activeStatKey = ref('all')
const detailMap = reactive({})
const detailLoadingMap = reactive({})
const assignmentMap = reactive({})
const subflowDialogVisible = ref(false)
const currentSubflowRecord = ref(null)
const selectedSubflowItemIds = ref([])
const subflowItemFilter = ref('all')
const pendingDetectorId = ref(undefined)
const detectorAssignDialogVisible = ref(false)
const subflowDialogMetrics = reactive({
  top: null,
  height: null
})
const resultDialogVisible = ref(false)
const resultSubmitting = ref(false)
const resultDialogItemId = ref(null)
const resultDialogItemStatus = ref('')
const resultDialogMode = ref('item')

const resultForm = reactive({
  recordId: null,
  sampleId: null,
  sampleNo: '',
  sealNo: '',
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
    sealNo: sample.sealNo,
    detectionTypeName: sample.detectionTypeName || sample.detectionItems || '待分配检测套餐',
    detectorName: '',
    detectionResult: null,
    detectionStatus: WAIT_ASSIGN_STATUS,
    detectionTime: sample.sealTime || sample.samplingTime || '',
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
    subtitle: '样品登录后先进入待分配检测流程，再按套餐参数拆分为多个待分配的检测子流程。',
    tableTitle: '检测流程队列',
    tableSubtitle: '点击每条主流程后的“查看检测项”，即可查看套餐参数列表并逐条分配检测员。',
    note: '当前页面重点处理“待分配”和“待检测”两个阶段，分配完成后即可继续进入检测执行。',
    guide: '建议先在本页完成参数级人员分配，再由检测员按各自分工执行后续检测录入。',
    defaultStatKey: 'all',
    emptyText: '暂无检测分样数据',
    allowAssign: true,
    includePendingFallback: false,
    statKeys: ['all', 'waitAssign', 'waitDetect', 'pendingReview'],
    recordFilter: (item) => item.detectionStatus === WAIT_ASSIGN_STATUS,
    quickLinks: [
      { path: '/sample-login', label: '样品登录', desc: '先完成样品登录，再进入检测分样分配检测员' },
      { path: '/review-result', label: '结果审查', desc: '检测完成提交后进入结果审查闭环' },
      { path: '/detection-history', label: '历史检测', desc: '查看已完成检测与驳回重检记录' }
    ]
  },
  '/detection-analysis': {
    key: 'detection-analysis',
    title: '检测分析',
    subtitle: '样品登录后先进入待分配检测流程，再按套餐参数拆分为多个待分配的检测子流程。',
    tableTitle: '检测流程队列',
    tableSubtitle: '点击每条主流程后的“查看检测项”，即可查看套餐参数列表并逐条分配检测员。',
    note: '当前页重点处理“待分配”和“待检测”两个阶段，分配完成后即可继续进入检测执行。',
    guide: '建议先在本页完成参数级人员分配，再由检测员按各自分工执行后续检测录入。',
    defaultStatKey: 'all',
    emptyText: '暂无检测分析数据',
    allowAssign: true,
    includePendingFallback: true,
    recordFilter: (item) => [WAIT_ASSIGN_STATUS, WAIT_DETECT_STATUS, reviewPendingDetectionStatus].includes(item.detectionStatus),
    quickLinks: [
      { path: '/sample-login', label: '样品登录', desc: '先完成样品登录，再进入检测分析分配检测员' },
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
    recordFilter: (item) =>
      [approvedDetectionStatus, rejectedDetectionStatus].includes(item.detectionStatus)
      || item.detectionResult === abnormalDetectionResult,
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

const pendingSampleRecords = computed(() => {
  if (!baseScene.value.includePendingFallback) {
    return []
  }
  if (query.mine === true) {
    return []
  }
  if (query.detectionStatus && query.detectionStatus !== WAIT_ASSIGN_STATUS) {
    return []
  }
  const existingSampleIds = new Set(records.value.map((item) => item.sampleId))
  return samplePool.value
    .filter((item) => availableDetectionSampleStatuses.includes(item.sampleStatus))
    .filter((item) => !existingSampleIds.has(item.id))
    .map(buildPendingSampleRecord)
    .sort(compareByTimeDesc)
})

const sourceRecords = computed(() => {
  const actualRecords = [...records.value].sort(compareByTimeDesc)
  if (!baseScene.value.includePendingFallback) {
    return actualRecords
  }
  return [...actualRecords, ...pendingSampleRecords.value].sort(compareByTimeDesc)
})

const sceneRecords = computed(() => sourceRecords.value.filter((item) => baseScene.value.recordFilter(item)))

const currentScene = computed(() => ({
  ...baseScene.value,
  tags: [
    {
      label: '待分配',
      value: sceneRecords.value.filter((item) => item.detectionStatus === WAIT_ASSIGN_STATUS).length,
      type: sceneRecords.value.some((item) => item.detectionStatus === WAIT_ASSIGN_STATUS) ? 'warning' : 'success'
    },
    {
      label: '待检测',
      value: sceneRecords.value.filter((item) => item.detectionStatus === WAIT_DETECT_STATUS).length,
      type: sceneRecords.value.some((item) => item.detectionStatus === WAIT_DETECT_STATUS) ? 'info' : 'success'
    },
    {
      label: '待审查',
      value: sceneRecords.value.filter((item) => item.detectionStatus === reviewPendingDetectionStatus).length,
      type: sceneRecords.value.some((item) => item.detectionStatus === reviewPendingDetectionStatus) ? 'danger' : 'success'
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
    value: sceneRecords.value.length,
    desc: baseScene.value.key === 'detection-ledger' ? '当前检测台账主流程总数' : '当前场景下可见的检测主流程总数'
  },
  {
    key: 'waitAssign',
    label: '待分配',
    value: sceneRecords.value.filter((item) => item.detectionStatus === WAIT_ASSIGN_STATUS).length,
    desc: '样品已进入检测分析，但参数子流程尚未完成检测员分配'
  },
  {
    key: 'waitDetect',
    label: '待检测',
    value: sceneRecords.value.filter((item) => item.detectionStatus === WAIT_DETECT_STATUS).length,
    desc: '参数子流程已完成检测员分配，等待检测执行'
  },
  {
    key: 'pendingReview',
    label: '待审查',
    value: sceneRecords.value.filter((item) => item.detectionStatus === reviewPendingDetectionStatus).length,
    desc: '检测结果已提交，等待进入审查闭环'
  },
  {
    key: 'approved',
    label: '已通过',
    value: sceneRecords.value.filter((item) => item.detectionStatus === approvedDetectionStatus).length,
    desc: '检测审查已通过，可继续进入报告正式产物环节'
  },
  {
    key: 'rejected',
    label: '已驳回',
    value: sceneRecords.value.filter((item) => item.detectionStatus === rejectedDetectionStatus).length,
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

const filteredRecords = computed(() => {
  if (activeStatKey.value === 'waitAssign') {
    return sceneRecords.value.filter((item) => item.detectionStatus === WAIT_ASSIGN_STATUS)
  }
  if (activeStatKey.value === 'waitDetect') {
    return sceneRecords.value.filter((item) => item.detectionStatus === WAIT_DETECT_STATUS)
  }
  if (activeStatKey.value === 'pendingReview') {
    return sceneRecords.value.filter((item) => item.detectionStatus === reviewPendingDetectionStatus)
  }
  if (activeStatKey.value === 'approved') {
    return sceneRecords.value.filter((item) => item.detectionStatus === approvedDetectionStatus)
  }
  if (activeStatKey.value === 'rejected') {
    return sceneRecords.value.filter((item) => item.detectionStatus === rejectedDetectionStatus)
  }
  return sceneRecords.value
})

const displayTotal = computed(() => filteredRecords.value.length)

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

const splitDialogGap = 12
const splitSubflowDialogWidth = 760
const splitDetectorPanelWidth = 420

const subflowDialogStyle = computed(() => {
  if (!detectorAssignDialogVisible.value) {
    return {}
  }
  const style = {
    left: `calc(50% - ${splitDialogGap}px - ${splitSubflowDialogWidth}px)`,
    width: `${splitSubflowDialogWidth}px`
  }
  if (subflowDialogMetrics.top != null) {
    style.top = `${subflowDialogMetrics.top}px`
  }
  if (subflowDialogMetrics.height != null) {
    style.height = `${subflowDialogMetrics.height}px`
  }
  return style
})

const detectorPanelStyle = computed(() => {
  if (!detectorAssignDialogVisible.value) {
    return {}
  }
  const style = {
    left: `calc(50% + ${splitDialogGap}px)`,
    width: `${splitDetectorPanelWidth}px`
  }
  if (subflowDialogMetrics.top != null) {
    style.top = `${subflowDialogMetrics.top}px`
  }
  if (subflowDialogMetrics.height != null) {
    style.height = `${subflowDialogMetrics.height}px`
  }
  return style
})

const pagedRecords = computed(() => {
  const start = (query.pageNum - 1) * query.pageSize
  return filteredRecords.value.slice(start, start + query.pageSize)
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
  activeStatKey.value = activeStatKey.value === key ? baseScene.value.defaultStatKey : key
  query.pageNum = 1
}

function handleSearch() {
  query.pageNum = 1
  loadData()
}

function resetQuery() {
  query.keyword = ''
  query.detectionStatus = ''
  query.mine = ''
  query.pageNum = 1
  loadData()
}

function syncRouteState() {
  activeStatKey.value = baseScene.value.defaultStatKey
  query.pageNum = 1
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
  const methodId = item?.methodId == null ? '' : String(item.methodId)
  if (!methodId) {
    return '-'
  }
  const method = detectionMethodOptions.value.find((option) => String(option.id) === methodId)
  return method?.methodBasis || '-'
}

function canAssignRow(row) {
  return !row.__sampleOnly
    && baseScene.value.allowAssign
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
  detectorAssignDialogVisible.value = false
  resetSubflowDialogMetrics()
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

function getSubflowDialogElement() {
  return document.querySelector('.el-dialog.detection-subflow-dialog')
    || document.querySelector('.detection-subflow-dialog .el-dialog')
}

function syncSubflowDialogMetrics() {
  if (typeof window === 'undefined') {
    return
  }
  const dialog = getSubflowDialogElement()
  if (!dialog) {
    return
  }
  const rect = dialog.getBoundingClientRect()
  subflowDialogMetrics.top = rect.top
  subflowDialogMetrics.height = rect.height
}

async function showDetectorAssignPanel() {
  syncSubflowDialogMetrics()
  await nextTick()
  syncSubflowDialogMetrics()
  detectorAssignDialogVisible.value = true
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
  if (!selectedSubflowItemIds.value.length) {
    detectorAssignDialogVisible.value = false
  }
}

function selectAllAssignableSubflowItems() {
  selectedSubflowItemIds.value = assignableSubflowItems.value.map((item) => item.id)
  if (selectedSubflowItemIds.value.length) {
    showDetectorAssignPanel()
  } else {
    detectorAssignDialogVisible.value = false
  }
}

function clearSelectedSubflowItems() {
  selectedSubflowItemIds.value = []
  pendingDetectorId.value = undefined
  detectorAssignDialogVisible.value = false
  resetSubflowDialogMetrics()
}

function resetSubflowDialogMetrics() {
  subflowDialogMetrics.top = null
  subflowDialogMetrics.height = null
}

function selectPendingDetector(detectorId) {
  pendingDetectorId.value = detectorId
}

function isPendingDetectorSelected(detectorId) {
  return pendingDetectorId.value !== undefined && String(pendingDetectorId.value) === String(detectorId)
}

function getPendingDetectorLabel() {
  if (pendingDetectorId.value === undefined) {
    return '请选择检测人员'
  }
  if (pendingDetectorId.value === null) {
    return '已选择：清空分配'
  }
  const option = detectorOptions.value.find((candidate) => String(candidate.id) === String(pendingDetectorId.value))
  return `已选择：${getDetectorOptionLabel(option)}`
}

function confirmSelectedDetector() {
  const recordId = currentSubflowRecord.value?.id
  if (!recordId || !selectedAssignableItems.value.length) {
    ElMessage.warning('请先勾选需要分配的检测参数')
    return
  }
  if (pendingDetectorId.value === undefined) {
    ElMessage.warning('请先选择检测人员')
    return
  }
  selectedAssignableItems.value.forEach((item) => {
    updateAssignedDetectorId(recordId, item.id, pendingDetectorId.value)
  })
  const detectorLabel = pendingDetectorId.value == null
    ? '未分配'
    : getDetectorOptionLabel(detectorOptions.value.find((option) => String(option.id) === String(pendingDetectorId.value)))
  detectorAssignDialogVisible.value = false
  pendingDetectorId.value = undefined
  selectedSubflowItemIds.value = []
  resetSubflowDialogMetrics()
  ElMessage.success(`已将所选参数批量设置为${detectorLabel}`)
}

function resetSubflowDialog() {
  currentSubflowRecord.value = null
  selectedSubflowItemIds.value = []
  subflowItemFilter.value = 'all'
  pendingDetectorId.value = undefined
  detectorAssignDialogVisible.value = false
  resetSubflowDialogMetrics()
}

function resetResultForm() {
  resultForm.recordId = null
  resultForm.sampleId = null
  resultForm.sampleNo = ''
  resultForm.sealNo = ''
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
  const value = Number(item.resultValue)
  if (!Number.isFinite(value)) {
    return false
  }
  if (item.standardMin != null && value < Number(item.standardMin)) {
    return true
  }
  return item.standardMax != null && value > Number(item.standardMax)
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
  resultForm.sealNo = row.sealNo || detail?.record?.sealNo || ''
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
  resultForm.sealNo = row.sealNo || detail?.record?.sealNo || ''
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
    referenceStandard: currentItem.referenceStandard || '',
    unit: currentItem.unit || '',
    resultValue: currentItem.resultValue == null ? null : Number(currentItem.resultValue)
  }))
  resultDialogVisible.value = true
}

async function openSubflowDialog(row) {
  currentSubflowRecord.value = row
  selectedSubflowItemIds.value = []
  subflowDialogVisible.value = true
  if (row?.__sampleOnly) {
    return
  }
  await loadRecordDetail(row.id)
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
  ElMessage.success('检测员分配已保存')
  await Promise.all([
    loadRecordDetail(row.id, true),
    loadData()
  ])
  subflowDialogVisible.value = false
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
        unit: item.unit
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

async function loadData() {
  const detectionQuery = {
    ...query,
    mine: query.mine === '' ? undefined : query.mine
  }
  const sampleQuery = {
    pageNum: 1,
    pageSize: MAX_LOAD_SIZE,
    keyword: query.keyword
  }
  const [detectionResult, sampleResult, detectorResult, detectionMethodResult] = await Promise.all([
    fetchDetectionsApi({ ...detectionQuery, pageNum: 1, pageSize: MAX_LOAD_SIZE }),
    fetchSamplesApi(sampleQuery),
    fetchDetectionDetectorsApi(),
    fetchDetectionMethodsApi({ pageNum: 1, pageSize: MAX_LOAD_SIZE })
  ])

  records.value = detectionResult.records || []
  samplePool.value = sampleResult.records || []
  detectorOptions.value = (detectorResult || []).map((item) => ({
    ...item,
    id: item.userId ?? item.id
  }))
  detectionMethodOptions.value = detectionMethodResult.records || []

  const maxPage = Math.max(1, Math.ceil(displayTotal.value / query.pageSize))
  if (query.pageNum > maxPage) {
    query.pageNum = 1
  }
}

async function handleExport() {
  try {
    await exportDetectionsApi({
      ...query,
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
  window.addEventListener('resize', syncSubflowDialogMetrics)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', syncSubflowDialogMetrics)
})

watch(() => route.fullPath, () => {
  syncRouteState()
})

watch(detectorAssignDialogVisible, async (visible) => {
  if (!visible) {
    pendingDetectorId.value = undefined
    return
  }
  await nextTick()
  syncSubflowDialogMetrics()
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
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
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
  gap: 12px;
}

.detector-side-panel {
  margin-top: 16px;
  padding: 18px;
  border: 1px solid color-mix(in srgb, var(--brand) 12%, #ffffff 88%);
  border-radius: 22px;
  background: #ffffff;
  box-shadow: var(--shadow-lg);
  width: 420px;
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

.detector-assign-dialog__picked {
  display: grid;
  gap: 12px;
  padding: 14px;
  border: 1px solid var(--line-soft);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.92);
}

.detector-assign-dialog__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.detector-assign-dialog__head strong {
  color: var(--text-main);
  font-size: 14px;
  line-height: 1.5;
}

.detector-assign-dialog__head span {
  color: var(--text-sub);
  font-size: 12px;
  line-height: 1.6;
  text-align: right;
}

.detector-assign-dialog__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.detector-assign-dialog__tag {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--brand) 8%, #ffffff 92%);
  border: 1px solid color-mix(in srgb, var(--brand) 16%, #ffffff 84%);
  color: var(--text-main);
  font-size: 13px;
  line-height: 1.5;
}

.detector-assign-dialog__list {
  display: grid;
  gap: 10px;
}

.subflow-detector-card {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid var(--line-soft);
  border-radius: 14px;
  background: #ffffff;
  display: grid;
  gap: 2px;
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

.subflow-detector-card--clear {
  background: color-mix(in srgb, #f59e0b 8%, #ffffff 92%);
}

.detector-assign-dialog__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 12px;
  border-top: 1px solid color-mix(in srgb, var(--brand) 12%, #ffffff 88%);
}

.detector-assign-dialog__footer span {
  color: var(--text-sub);
  font-size: 13px;
  line-height: 1.6;
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

.result-dialog__form {
  margin-top: 4px;
}

:deep(.el-dialog.detection-subflow-dialog),
:deep(.detection-subflow-dialog .el-dialog) {
  max-width: min(960px, calc(100vw - 64px));
  overflow: visible;
  transition: width 0.22s ease, left 0.22s ease, transform 0.22s ease;
}

:deep(.el-dialog.detection-subflow-dialog .el-dialog__body),
:deep(.detection-subflow-dialog .el-dialog__body) {
  padding-top: 18px;
  padding-left: 20px;
  padding-right: 20px;
  overflow: visible;
}

@media (min-width: 960px) {
  :deep(.el-dialog.detection-subflow-dialog--shifted),
  :deep(.detection-subflow-dialog--shifted .el-dialog) {
    position: fixed;
    left: calc(50% - 12px - 760px);
    right: auto !important;
    transform: none !important;
    width: 760px !important;
    max-width: none !important;
    margin: 0;
  }

  .detector-side-panel {
    position: fixed;
    left: calc(50% + 12px);
    right: auto;
    width: 420px;
    margin-top: 0;
    z-index: 2300;
    overflow: auto;
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

  .subflow-batch-panel__head,
  .detector-assign-dialog__head {
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
