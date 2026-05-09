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
            <el-button v-if="baseScene.key === 'review-result'" @click="approveFirst">通过首条</el-button>
            <el-button
              v-if="baseScene.key === 'review-result'"
              type="danger"
              plain
              @click="rejectFirst"
            >
              驳回首条
            </el-button>
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
        <el-table class="list-table" :data="visibleRecords" stripe max-height="460" :empty-text="baseScene.emptyText">
          <el-table-column prop="sampleNo" label="样品编号" min-width="180" />
          <el-table-column prop="sealNo" label="封签编号" min-width="180" />
          <el-table-column prop="reviewerName" label="审核人" width="120" />
          <el-table-column label="审查状态" width="120" header-cell-class-name="cell-center" class-name="cell-center">
            <template #default="{ row }">
              <span :class="['status-chip', row.reviewResult ? getStatusClass('reviewResult', row.reviewResult) : 'warning']">
                {{ row.reviewResult ? getEnumLabel(reviewResultLabelMap, row.reviewResult) : '待审核' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="reviewTime" label="审查时间" width="170" />
          <el-table-column prop="reviewRemark" label="审查意见" min-width="180" show-overflow-tooltip />
          <el-table-column prop="rejectReason" label="驳回原因" min-width="180" show-overflow-tooltip />
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

    <section class="scene-grid">
      <div class="glass-panel section-block">
        <div class="section-head">
          <h3 class="section-title">页面说明</h3>
        </div>
        <div class="scene-copy">
          <p>{{ baseScene.guide }}</p>
          <p>当前审查页面已经把待审核队列和审核历史拆开，避免结果审查页沦为纯展示页。</p>
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
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElTable, ElTableColumn } from 'element-plus/es/components/table/index.mjs'
import { fetchDetectionsApi, fetchReviewsApi, submitReviewApi } from '../api/lab'
import TablePagination from '../components/common/TablePagination.vue'
import {
  approvedReviewResult,
  DEFAULT_PAGE_SIZE,
  getEnumLabel,
  getStatusClass,
  rejectedReviewResult,
  reviewPendingDetectionStatus,
  reviewResultLabelMap
} from '../utils/labEnums'

const route = useRoute()
const router = useRouter()

const query = reactive({ pageNum: 1, pageSize: DEFAULT_PAGE_SIZE })
const reviewRecords = ref([])
const pendingDetections = ref([])
const total = ref(0)
const activeStatKey = ref('pending')

function toSafeNumber(value) {
  const num = typeof value === 'number' ? value : Number.parseFloat(String(value ?? '').replace(/,/g, '').trim())
  return Number.isFinite(num) ? num : 0
}

const sceneMap = {
  '/review-result': {
    key: 'review-result',
    title: '结果审查',
    subtitle: '聚焦待审核检测结果，支持直接通过或驳回首条，真正承接检测后的当前处理环节。',
    tableTitle: '待审核队列',
    tableSubtitle: '默认展示尚未审核的检测结果，也支持切换查看已通过和已驳回记录。',
    note: '结果审查页以“待处理”为主，不再把历史记录当成默认视图。',
    guide: '通过后即可进入报告环节；如驳回，会将样品退回重检链路继续处理。',
    defaultStatKey: 'pending',
    emptyText: '暂无待审核审查数据',
    quickLinks: [
      { path: '/detection-analysis', label: '检测分析', desc: '回到检测处理页面继续补录或重检' },
      { path: '/review-history', label: '历史审查', desc: '查看已完成的审核处理记录' },
      { path: '/report-ledger', label: '报告台账', desc: '审核通过后进入报告产物管理' }
    ]
  },
  '/review-history': {
    key: 'review-history',
    title: '历史审查',
    subtitle: '回看已经完成的审核处理记录，适合复盘审核结论、驳回原因与后续闭环情况。',
    tableTitle: '历史审查记录',
    tableSubtitle: '本页聚焦已通过与已驳回记录，默认展示审核通过历史。',
    note: '历史审查页仅承担追溯用途，不再保留通过与驳回动作按钮。',
    guide: '如果要继续追踪最终产物，可跳往报告台账；如需看待处理队列，请回结果审查页。',
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
    subtitle: '集中查看全部审核记录，适合全量核对审核结果、驳回原因与报告闭环衔接。',
    tableTitle: '审查全量台账',
    tableSubtitle: '台账页保留全量审核清单，便于管理人员做整体盘点。',
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
    reviewerName: item.reviewerName || '待审核',
    reviewResult: null,
    reviewTime: item.detectionTime,
    reviewRemark: '等待审核处理',
    rejectReason: item.abnormalRemark || '-'
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
    desc: '检测完成后尚未审核的结果'
  },
  {
    key: 'approved',
    label: '审核通过',
    value: approvedRows.value.length,
    desc: '已经审核通过并可进入报告流程的记录'
  },
  {
    key: 'rejected',
    label: '审核驳回',
    value: rejectedRows.value.length,
    desc: '已驳回并退回重检链路的记录'
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

async function loadData() {
  const [reviewResult, detectionResult] = await Promise.all([
    fetchReviewsApi(query),
    fetchDetectionsApi({ pageNum: 1, pageSize: 200 })
  ])
  reviewRecords.value = reviewResult.records || []
  total.value = toSafeNumber(reviewResult.total)
  pendingDetections.value = (detectionResult.records || []).filter(
    (item) => item.detectionStatus === reviewPendingDetectionStatus
  )
}

async function approveFirst() {
  const record = pendingDetections.value[0]
  if (!record) {
    ElMessage.warning('当前没有待审核的检测记录。')
    return
  }

  await submitReviewApi({
    detectionRecordId: record.id,
    reviewResult: approvedReviewResult,
    reviewRemark: '数据合格，允许出具报告。'
  })

  ElMessage.success('审核通过。')
  await loadData()
}

async function rejectFirst() {
  const record = pendingDetections.value[0]
  if (!record) {
    ElMessage.warning('当前没有待审核的检测记录。')
    return
  }

  await submitReviewApi({
    detectionRecordId: record.id,
    reviewResult: rejectedReviewResult,
    rejectReason: '原始记录不完整，退回重检',
    reviewRemark: '请补全记录后重新提交'
  })

  ElMessage.success('已驳回并退回重检。')
  await loadData()
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

@media (max-width: 900px) {
  .page-hero,
  .scene-grid {
    grid-template-columns: 1fr;
  }

  .hero-tags {
    justify-content: flex-start;
  }
}
</style>
