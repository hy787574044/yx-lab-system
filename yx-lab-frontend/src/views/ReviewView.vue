<template>
  <div class="content-grid review-page">
    <section class="glass-panel section-block page-hero">
      <div>
        <h2 class="page-title">{{ currentScene.title }}</h2>
        <p class="page-subtitle">{{ currentScene.subtitle }}</p>
      </div>
      <div class="hero-tags">
        <span
          v-for="tag in currentScene.tags"
          :key="tag.label"
          :class="['status-chip', tag.type]"
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
          <h3 class="section-title">{{ baseScene.tableTitle }}</h3>
          <p class="page-subtitle">{{ baseScene.tableSubtitle }}</p>
        </div>
      </div>

      <div class="toolbar-panel">
        <div class="toolbar-row">
          <div class="panel-note">{{ baseScene.note }}</div>
          <div class="toolbar-actions">
            <el-button type="primary" @click="loadData">刷新审查队列</el-button>
            <el-button
              v-if="baseScene.key === 'review-history'"
              type="primary"
              plain
              @click="goRoute('/report-ledger')"
            >
              查看报告台账
            </el-button>
          </div>
        </div>
      </div>

      <div class="table-card">
        <el-table class="list-table" :data="visibleRecords" stripe max-height="520" :empty-text="baseScene.emptyText">
          <el-table-column prop="sampleNo" label="样品编号" min-width="160" />
          <el-table-column prop="sealNo" label="封签编号" min-width="160" />
          <el-table-column prop="detectionTypeName" label="检测套餐" min-width="160" />
          <el-table-column label="参数进度" min-width="150">
            <template #default="{ row }">
              {{ formatItemProgress(row) }}
            </template>
          </el-table-column>
          <el-table-column prop="reviewerName" label="审核人" width="120" />
          <el-table-column label="审查状态" width="120" header-cell-class-name="cell-center" class-name="cell-center">
            <template #default="{ row }">
              <span :class="['status-chip', getRowReviewStatusClass(row)]">
                {{ getRowReviewStatusLabel(row) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="reviewTime" label="审查时间" width="170" />
          <el-table-column label="审查意见" min-width="220" show-overflow-tooltip>
            <template #default="{ row }">
              {{ row.reviewRemark || (isPendingRow(row) ? '等待审核处理' : '-') }}
            </template>
          </el-table-column>
          <el-table-column label="驳回原因" min-width="220" show-overflow-tooltip>
            <template #default="{ row }">
              {{ row.rejectReason || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" header-cell-class-name="cell-center" class-name="cell-center">
            <template #default="{ row }">
              <el-button
                v-if="isPendingRow(row)"
                type="primary"
                link
                @click="openReviewDialog(row)"
              >
                审核
              </el-button>
              <el-button
                v-else
                type="primary"
                link
                @click="openReviewDialog(row, true)"
              >
                查看明细
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <TablePagination
          v-if="baseScene.key !== 'review-result'"
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          @change="loadData"
        />
      </div>
    </section>

    <el-dialog
      v-model="reviewDialogVisible"
      class="review-detail-dialog"
      :title="reviewDialogTitle"
      width="1220px"
      destroy-on-close
      @closed="resetReviewDialog"
    >
      <div class="review-dialog">
        <div class="review-dialog__summary">
          <span class="binding-editor__chip">样品编号<strong>{{ reviewDialog.sampleNo || '-' }}</strong></span>
          <span class="binding-editor__chip">封签编号<strong>{{ reviewDialog.sealNo || '-' }}</strong></span>
          <span class="binding-editor__chip">检测套餐<strong>{{ reviewDialog.detectionTypeName || '-' }}</strong></span>
          <span class="binding-editor__chip">子流程<strong>{{ reviewDialog.items.length }}</strong></span>
          <span class="binding-editor__chip">待审核<strong>{{ pendingReviewItemCount }}</strong></span>
          <span class="binding-editor__chip">已通过<strong>{{ approvedReviewItemCount }}</strong></span>
          <span class="binding-editor__chip">已驳回<strong>{{ rejectedReviewItemCount }}</strong></span>
        </div>

        <div class="review-dialog__toolbar" v-if="!reviewDialogReadonly">
          <el-button type="primary" @click="approveAllPendingItems">一键审核通过</el-button>
          <el-button type="danger" plain @click="rejectAllPendingItems">一键审核不通过</el-button>
        </div>

        <div class="panel-note review-dialog__note">
          {{ reviewDialogNote }}
        </div>

        <el-table
          class="list-table review-dialog__table"
          :data="reviewDialog.items"
          stripe
          border
          max-height="500"
        >
          <el-table-column prop="parameterName" label="检测参数" min-width="120" />
          <el-table-column prop="methodName" label="检测方法" min-width="180" show-overflow-tooltip />
          <el-table-column label="标准范围" min-width="130">
            <template #default="{ row }">
              {{ formatStandardRange(row.standardMin, row.standardMax, row.unit) }}
            </template>
          </el-table-column>
          <el-table-column prop="referenceStandard" label="参考范围" min-width="150" show-overflow-tooltip>
            <template #default="{ row }">{{ row.referenceStandard || '-' }}</template>
          </el-table-column>
          <el-table-column prop="resultValue" label="检测值" min-width="110">
            <template #default="{ row }">{{ row.resultValue ?? '-' }}</template>
          </el-table-column>
          <el-table-column label="检测判定" width="110" header-cell-class-name="cell-center" class-name="cell-center">
            <template #default="{ row }">
              <span class="status-chip" :class="getResultValueStatusClass(row)">
                {{ getResultValueStatusLabel(row) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="子流程状态" width="110" header-cell-class-name="cell-center" class-name="cell-center">
            <template #default="{ row }">
              <span class="status-chip" :class="getItemStatusClass(row.itemStatus)">
                {{ getItemStatusLabel(row.itemStatus) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="审核操作" min-width="220">
            <template #default="{ row }">
              <div v-if="canReviewItem(row)" class="review-item-actions">
                <el-button
                  :type="row.reviewResultDraft === approvedReviewResult ? 'primary' : 'default'"
                  plain
                  @click="setItemReviewResult(row, approvedReviewResult)"
                >
                  通过
                </el-button>
                <el-button
                  :type="row.reviewResultDraft === rejectedReviewResult ? 'danger' : 'default'"
                  plain
                  @click="setItemReviewResult(row, rejectedReviewResult)"
                >
                  驳回
                </el-button>
              </div>
              <span v-else class="review-item-fixed">
                {{ getFinalReviewText(row) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="驳回原因" min-width="220">
            <template #default="{ row }">
              <span v-if="!canReviewItem(row)">{{ row.rejectReasonDraft || '-' }}</span>
              <el-input
                v-else
                v-model="row.rejectReasonDraft"
                :disabled="row.reviewResultDraft !== rejectedReviewResult"
                placeholder="子流程审核不通过时填写原因"
              />
            </template>
          </el-table-column>
          <el-table-column label="审核说明" min-width="220">
            <template #default="{ row }">
              <span v-if="reviewDialogReadonly">{{ row.reviewRemarkDraft || '-' }}</span>
              <el-input
                v-else
                v-model="row.reviewRemarkDraft"
                placeholder="可补充审核说明"
              />
            </template>
          </el-table-column>
        </el-table>

        <el-form label-position="top" class="review-dialog__form">
          <el-form-item label="整单审核意见">
            <el-input
              v-model="reviewDialog.reviewRemark"
              type="textarea"
              :rows="3"
              :readonly="reviewDialogReadonly"
              placeholder="用于记录本次整单审核的总体结论或补充说明"
            />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <el-button @click="reviewDialogVisible = false">{{ reviewDialogReadonly ? '关闭' : '取消' }}</el-button>
        <el-button
          v-if="!reviewDialogReadonly"
          type="primary"
          :loading="reviewSubmitting"
          @click="submitReviewDecision"
        >
          提交审核
        </el-button>
      </template>
    </el-dialog>

    <section class="scene-grid">
      <div class="glass-panel section-block">
        <div class="section-head">
          <h3 class="section-title">页面说明</h3>
        </div>
        <div class="scene-copy">
          <p>{{ baseScene.guide }}</p>
          <p>当前审查页面已经支持按主流程展开全部化验结果，并对子流程逐条审核、批量通过或批量驳回。</p>
        </div>
      </div>

      <div class="glass-panel section-block">
        <div class="section-head">
          <h3 class="section-title">关联入口</h3>
        </div>
        <div class="quick-links">
          <button
            v-for="item in baseScene.quickLinks"
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
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElButton } from 'element-plus/es/components/button/index.mjs'
import { ElDialog } from 'element-plus/es/components/dialog/index.mjs'
import { ElForm, ElFormItem } from 'element-plus/es/components/form/index.mjs'
import { ElInput } from 'element-plus/es/components/input/index.mjs'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElTable, ElTableColumn } from 'element-plus/es/components/table/index.mjs'
import {
  fetchDetectionDetailApi,
  fetchDetectionsApi,
  fetchReviewsApi,
  submitReviewApi
} from '../api/lab'
import TablePagination from '../components/common/TablePagination.vue'
import {
  approvedDetectionStatus,
  approvedReviewResult,
  DEFAULT_PAGE_SIZE,
  getEnumLabel,
  getStatusClass,
  rejectedDetectionStatus,
  rejectedReviewResult,
  reviewPendingDetectionStatus,
  reviewResultLabelMap
} from '../utils/labEnums'

const route = useRoute()
const router = useRouter()

const WAIT_ASSIGN_STATUS = 'WAIT_ASSIGN'
const WAIT_DETECT_STATUS = 'WAIT_DETECT'

const query = reactive({ pageNum: 1, pageSize: DEFAULT_PAGE_SIZE })
const reviewRecords = ref([])
const pendingDetections = ref([])
const total = ref(0)
const activeStatKey = ref('pending')
const reviewDialogVisible = ref(false)
const reviewSubmitting = ref(false)

const reviewDialog = reactive({
  detectionRecordId: null,
  sampleNo: '',
  sealNo: '',
  detectionTypeName: '',
  reviewRemark: '',
  items: []
})

const reviewDialogReadonly = ref(false)

function toSafeNumber(value) {
  const num = typeof value === 'number' ? value : Number.parseFloat(String(value ?? '').replace(/,/g, '').trim())
  return Number.isFinite(num) ? num : 0
}

const sceneMap = {
  '/review-result': {
    key: 'review-result',
    title: '结果审查',
    subtitle: '按检测主流程审核全部化验结果，并对子流程逐条通过或驳回，形成真正的审查闭环。',
    tableTitle: '待审核队列',
    tableSubtitle: '打开每条主流程即可查看全部化验结果，并按子流程做通过/驳回判定。',
    note: '结果审查页现在以“逐条主流程审核”模式运行，不再保留首条演示审核按钮。',
    guide: '全部子流程审核通过后，该主流程才算审核通过；只要存在驳回子流程，就会退回流程分析重新化验。',
    defaultStatKey: 'pending',
    emptyText: '暂无待审核审查数据',
    quickLinks: [
      { path: '/detection-analysis', label: '流程分析', desc: '驳回后回到流程分析，重新录入对应子流程结果' },
      { path: '/review-history', label: '历史审查', desc: '查看已完成的整单审核处理记录' },
      { path: '/report-ledger', label: '报告台账', desc: '整单审核通过后进入报告产物管理' }
    ]
  },
  '/review-history': {
    key: 'review-history',
    title: '历史审查',
    subtitle: '回看已经完成的审核处理记录，重点追溯各主流程的通过、驳回与重检情况。',
    tableTitle: '历史审查记录',
    tableSubtitle: '可查看每条主流程对应的化验结果明细与最终审核结论。',
    note: '历史审查页仅承担追溯用途，不再在列表中直接处理待审核动作。',
    guide: '如果要继续处理待办，请回结果审查页；如果要看最终产物，请转到报告台账。',
    defaultStatKey: 'approved',
    emptyText: '暂无历史审查数据',
    quickLinks: [
      { path: '/review-result', label: '结果审查', desc: '返回待审核处理入口' },
      { path: '/report-ledger', label: '报告台账', desc: '查看审核通过后是否已生成报告' },
      { path: '/detection-history', label: '历史检测', desc: '回溯检测提交与异常处理背景' }
    ]
  },
  '/review-ledger': {
    key: 'review-ledger',
    title: '审查台账',
    subtitle: '集中查看全部审核记录，适合全量核对审核结果、驳回原因与后续闭环衔接。',
    tableTitle: '审查全量台账',
    tableSubtitle: '台账页保留全量审核清单，也支持查看每条主流程的子流程审核明细。',
    note: '审查台账页用于全量盘点，可按状态快速切换查看不同审核结果。',
    guide: '如需处理待办，请前往结果审查；如需查看最终产物，请进入报告台账。',
    defaultStatKey: 'all',
    emptyText: '暂无审查台账数据',
    quickLinks: [
      { path: '/review-result', label: '结果审查', desc: '处理待审核检测结果' },
      { path: '/report-ledger', label: '报告台账', desc: '核对正式报告发布情况' },
      { path: '/sample-ledger', label: '样品台账', desc: '回溯审查记录对应的样品信息' }
    ]
  }
}

const baseScene = computed(() => sceneMap[route.path] || sceneMap['/review-result'])

const mappedPendingRows = computed(() =>
  pendingDetections.value.map((item) => ({
    id: `pending-${item.id}`,
    detectionRecordId: item.id,
    sampleNo: item.sampleNo,
    sealNo: item.sealNo,
    detectionTypeName: item.detectionTypeName || '-',
    reviewerName: '待审核',
    reviewResult: null,
    reviewTime: item.detectionTime,
    reviewRemark: '',
    rejectReason: item.abnormalRemark || '',
    parameterCount: item.parameterCount || 0,
    completedCount: item.completedCount || 0
  }))
)

const approvedRows = computed(() =>
  reviewRecords.value.filter((item) => item.reviewResult === approvedReviewResult)
)

const rejectedRows = computed(() =>
  reviewRecords.value.filter((item) => item.reviewResult === rejectedReviewResult)
)

const currentScene = computed(() => ({
  ...baseScene.value,
  tags: [
    {
      label: '待审核',
      value: mappedPendingRows.value.length,
      type: mappedPendingRows.value.length ? 'warning' : 'success'
    },
    {
      label: '审核通过',
      value: approvedRows.value.length,
      type: 'success'
    },
    {
      label: '审核驳回',
      value: rejectedRows.value.length,
      type: rejectedRows.value.length ? 'danger' : 'info'
    }
  ]
}))

const currentStats = computed(() => [
  {
    key: 'all',
    label: baseScene.value.key === 'review-ledger' ? '台账总量' : '审查总览',
    value: baseScene.value.key === 'review-ledger'
      ? toSafeNumber(total.value)
      : mappedPendingRows.value.length + reviewRecords.value.length,
    desc: baseScene.value.key === 'review-ledger' ? '审查台账总量' : '当前场景已加载的审查记录'
  },
  {
    key: 'pending',
    label: '待审核',
    value: mappedPendingRows.value.length,
    desc: '检测结果已提交，等待审查人员完成整单审核'
  },
  {
    key: 'approved',
    label: '审核通过',
    value: approvedRows.value.length,
    desc: '全部子流程均已审核通过，可继续进入报告环节'
  },
  {
    key: 'rejected',
    label: '审核驳回',
    value: rejectedRows.value.length,
    desc: '存在审核不通过子流程，主流程已退回流程分析重检'
  }
])

const visibleRecords = computed(() => {
  if (baseScene.value.key === 'review-result') {
    if (activeStatKey.value === 'approved') {
      return approvedRows.value
    }
    if (activeStatKey.value === 'rejected') {
      return rejectedRows.value
    }
    if (activeStatKey.value === 'all') {
      return [...mappedPendingRows.value, ...reviewRecords.value]
    }
    return mappedPendingRows.value
  }

  if (activeStatKey.value === 'approved') {
    return approvedRows.value
  }
  if (activeStatKey.value === 'rejected') {
    return rejectedRows.value
  }
  return reviewRecords.value
})

const reviewDialogTitle = computed(() => reviewDialogReadonly.value ? '审查明细查看' : '审查处理')

const pendingReviewItemCount = computed(() => reviewDialog.items.filter((item) => item.itemStatus === reviewPendingDetectionStatus).length)
const approvedReviewItemCount = computed(() => reviewDialog.items.filter((item) => item.itemStatus === approvedDetectionStatus).length)
const rejectedReviewItemCount = computed(() => reviewDialog.items.filter((item) => item.itemStatus === rejectedDetectionStatus).length)

const reviewDialogNote = computed(() => (
  reviewDialogReadonly.value
    ? '当前窗口用于查看该主流程下全部子流程的化验结果与历史审核结论。'
    : '请对当前主流程下全部待审核子流程逐条做通过/驳回判定；只要有一条驳回，该主流程就会退回流程分析重检。'
))

function handleStatClick(key) {
  activeStatKey.value = activeStatKey.value === key ? baseScene.value.defaultStatKey : key
}

function syncRouteState() {
  activeStatKey.value = baseScene.value.defaultStatKey
}

function goRoute(path) {
  if (!path) {
    return
  }
  router.push(path)
}

function isPendingRow(row) {
  return !row?.reviewResult
}

function getRowReviewStatusLabel(row) {
  return row?.reviewResult ? getEnumLabel(reviewResultLabelMap, row.reviewResult) : '待审核'
}

function getRowReviewStatusClass(row) {
  return row?.reviewResult ? getStatusClass('reviewResult', row.reviewResult) : 'warning'
}

function formatItemProgress(row) {
  const total = Number(row?.parameterCount || 0)
  const completed = Number(row?.completedCount || 0)
  if (!total) {
    return '-'
  }
  return `${completed}/${total} 已完成检测`
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

function getItemStatusLabel(status) {
  if (status === WAIT_ASSIGN_STATUS) {
    return '待分配'
  }
  if (status === WAIT_DETECT_STATUS) {
    return '待检测'
  }
  if (status === reviewPendingDetectionStatus) {
    return '待审核'
  }
  if (status === approvedDetectionStatus) {
    return '审核通过'
  }
  if (status === rejectedDetectionStatus) {
    return '审核驳回'
  }
  return status || '-'
}

function getItemStatusClass(status) {
  if (status === WAIT_ASSIGN_STATUS) {
    return 'warning'
  }
  if (status === WAIT_DETECT_STATUS) {
    return 'info'
  }
  if (status === reviewPendingDetectionStatus) {
    return 'warning'
  }
  if (status === approvedDetectionStatus) {
    return 'success'
  }
  if (status === rejectedDetectionStatus) {
    return 'danger'
  }
  return 'info'
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
    return '未录入'
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

function canReviewItem(item) {
  return !reviewDialogReadonly.value && item.itemStatus === reviewPendingDetectionStatus
}

function setItemReviewResult(item, reviewResult) {
  if (!canReviewItem(item)) {
    return
  }
  item.reviewResultDraft = reviewResult
  if (reviewResult === approvedReviewResult) {
    item.rejectReasonDraft = ''
  } else if (!item.rejectReasonDraft) {
    item.rejectReasonDraft = '审核不通过，请重新化验并提交结果'
  }
}

function approveAllPendingItems() {
  reviewDialog.items.forEach((item) => {
    if (canReviewItem(item)) {
      item.reviewResultDraft = approvedReviewResult
      item.rejectReasonDraft = ''
    }
  })
}

function rejectAllPendingItems() {
  reviewDialog.items.forEach((item) => {
    if (canReviewItem(item)) {
      item.reviewResultDraft = rejectedReviewResult
      if (!item.rejectReasonDraft) {
        item.rejectReasonDraft = '审核不通过，请重新化验并提交结果'
      }
    }
  })
}

function getFinalReviewText(item) {
  if (item.itemStatus === approvedDetectionStatus) {
    return '已通过'
  }
  if (item.itemStatus === rejectedDetectionStatus) {
    return '已驳回'
  }
  return '只读'
}

function resetReviewDialog() {
  reviewDialog.detectionRecordId = null
  reviewDialog.sampleNo = ''
  reviewDialog.sealNo = ''
  reviewDialog.detectionTypeName = ''
  reviewDialog.reviewRemark = ''
  reviewDialog.items = []
  reviewDialogReadonly.value = false
}

async function openReviewDialog(row, readonly = false) {
  const detectionRecordId = row?.detectionRecordId
  if (!detectionRecordId) {
    ElMessage.warning('当前主流程标识不存在，请刷新后重试')
    return
  }
  const detail = await fetchDetectionDetailApi(detectionRecordId)
  const items = detail?.items || []
  if (!items.length) {
    ElMessage.warning('当前主流程下暂无可审查的化验结果明细')
    return
  }
  resetReviewDialog()
  reviewDialog.detectionRecordId = detectionRecordId
  reviewDialog.sampleNo = row.sampleNo || detail?.record?.sampleNo || ''
  reviewDialog.sealNo = row.sealNo || detail?.record?.sealNo || ''
  reviewDialog.detectionTypeName = row.detectionTypeName || detail?.record?.detectionTypeName || ''
  reviewDialog.reviewRemark = row.reviewRemark || ''
  reviewDialog.items = items.map((item) => ({
    id: item.id,
    parameterName: item.parameterName || '',
    methodName: item.methodName || '',
    standardMin: item.standardMin,
    standardMax: item.standardMax,
    referenceStandard: item.referenceStandard || '',
    unit: item.unit || '',
    resultValue: item.resultValue == null ? null : Number(item.resultValue),
    itemStatus: item.itemStatus || '',
    reviewResultDraft: item.itemStatus === approvedDetectionStatus
      ? approvedReviewResult
      : (item.itemStatus === rejectedDetectionStatus ? rejectedReviewResult : ''),
    rejectReasonDraft: item.itemStatus === rejectedDetectionStatus ? (row.rejectReason || '') : '',
    reviewRemarkDraft: ''
  }))
  reviewDialogReadonly.value = readonly || !isPendingRow(row)
  reviewDialogVisible.value = true
}

async function submitReviewDecision() {
  const pendingItems = reviewDialog.items.filter((item) => item.itemStatus === reviewPendingDetectionStatus)
  if (!pendingItems.length) {
    ElMessage.warning('当前主流程下没有待审核子流程')
    return
  }
  const undecidedItem = pendingItems.find((item) => !item.reviewResultDraft)
  if (undecidedItem) {
    ElMessage.warning(`请先完成子流程“${undecidedItem.parameterName}”的审核判定`)
    return
  }
  const rejectedItem = pendingItems.find((item) =>
    item.reviewResultDraft === rejectedReviewResult && !String(item.rejectReasonDraft || '').trim())
  if (rejectedItem) {
    ElMessage.warning(`请填写子流程“${rejectedItem.parameterName}”的驳回原因`)
    return
  }

  reviewSubmitting.value = true
  try {
    const anyRejected = pendingItems.some((item) => item.reviewResultDraft === rejectedReviewResult)
    await submitReviewApi({
      detectionRecordId: reviewDialog.detectionRecordId,
      reviewResult: anyRejected ? rejectedReviewResult : approvedReviewResult,
      reviewRemark: reviewDialog.reviewRemark,
      items: pendingItems.map((item) => ({
        itemId: item.id,
        reviewResult: item.reviewResultDraft,
        rejectReason: item.reviewResultDraft === rejectedReviewResult ? item.rejectReasonDraft : '',
        reviewRemark: item.reviewRemarkDraft
      }))
    })
    reviewDialogVisible.value = false
    ElMessage.success(anyRejected ? '审核已提交，主流程已退回流程分析' : '审核已提交，主流程全部通过')
    await loadData()
  } finally {
    reviewSubmitting.value = false
  }
}

async function loadData() {
  const [reviewResult, detectionResult] = await Promise.all([
    fetchReviewsApi(query),
    fetchDetectionsApi({ pageNum: 1, pageSize: 500 })
  ])
  const detectionRecords = detectionResult.records || []
  const detectionMap = detectionRecords.reduce((result, item) => {
    result[item.id] = item
    return result
  }, {})
  reviewRecords.value = (reviewResult.records || []).map((item) => {
    const related = detectionMap[item.detectionRecordId] || {}
    return {
      ...item,
      detectionTypeName: related.detectionTypeName || item.detectionTypeName || '-',
      parameterCount: related.parameterCount || 0,
      completedCount: related.completedCount || 0
    }
  })
  total.value = toSafeNumber(reviewResult.total)
  pendingDetections.value = detectionRecords.filter(
    (item) => item.detectionStatus === reviewPendingDetectionStatus
  )
}

onMounted(async () => {
  syncRouteState()
  await loadData()
})

watch(() => route.fullPath, () => {
  syncRouteState()
})
</script>

<style scoped>
.review-page {
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
  margin: 10px 0 0;
  color: var(--text-sub);
  font-size: 14px;
  line-height: 1.6;
}

.scene-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.scene-copy {
  display: grid;
  gap: 10px;
  color: var(--text-sub);
  font-size: 14px;
  line-height: 1.7;
}

.scene-copy p {
  margin: 0;
}

.quick-links {
  display: grid;
  gap: 12px;
}

.quick-link {
  display: grid;
  gap: 4px;
  padding: 14px;
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

.review-dialog {
  display: grid;
  gap: 14px;
}

.review-dialog__summary {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.review-dialog__toolbar {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.review-dialog__note {
  margin-bottom: 2px;
}

.review-dialog__table {
  width: 100%;
}

.review-dialog__form {
  margin-top: 4px;
}

.review-item-actions {
  display: flex;
  gap: 8px;
}

.review-item-fixed {
  color: var(--text-sub);
  font-size: 13px;
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

@media (max-width: 900px) {
  .page-hero,
  .scene-grid {
    grid-template-columns: 1fr;
  }

  .hero-tags {
    justify-content: flex-start;
  }

  .review-dialog__toolbar {
    justify-content: flex-start;
    flex-wrap: wrap;
  }
}
</style>
