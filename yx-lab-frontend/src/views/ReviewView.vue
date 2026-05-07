<template>
  <div class="content-grid">
    <section class="stats-grid">
      <article class="metric-card" v-for="item in stats" :key="item.label">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <p>{{ item.desc }}</p>
      </article>
    </section>

    <section class="glass-panel section-block">
      <div class="section-head">
        <div>
          <h3 class="section-title">结果审核</h3>
          <p class="page-subtitle">统一查看审核记录、审核结果和驳回原因，支持检测结果闭环流转。</p>
        </div>
      </div>

      <div class="toolbar">
        <el-button type="primary" @click="loadData">刷新审核队列</el-button>
        <el-button @click="approveFirst">通过首条</el-button>
        <el-button type="danger" plain @click="rejectFirst">驳回首条</el-button>
      </div>

      <div class="table-card">
        <el-table class="list-table" :data="records" stripe max-height="420" empty-text="暂无审核记录数据">
          <el-table-column prop="sampleNo" label="样品编号" min-width="180" />
          <el-table-column prop="sealNo" label="封签编号" min-width="180" />
          <el-table-column prop="reviewerName" label="审核人" width="120" />
          <el-table-column label="审核结果" width="120">
            <template #default="{ row }">
              <span class="status-chip" :class="getStatusClass('reviewResult', row.reviewResult)">
                {{ row.reviewResult ? getEnumLabel(reviewResultLabelMap, row.reviewResult) : '待审核' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="reviewTime" label="审核时间" width="170" />
          <el-table-column prop="reviewRemark" label="审核意见" min-width="180" show-overflow-tooltip />
          <el-table-column prop="rejectReason" label="驳回原因" min-width="180" show-overflow-tooltip />
        </el-table>

        <TablePagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          @change="loadData"
        />
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
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

const query = reactive({ pageNum: 1, pageSize: DEFAULT_PAGE_SIZE })
const records = ref([])
const total = ref(0)

const stats = computed(() => [
  { label: '审核总数', value: total.value, desc: '审核记录总量' },
  { label: '本页记录', value: records.value.length, desc: '当前分页加载的审核记录' },
  { label: '审核通过', value: records.value.filter((item) => item.reviewResult === approvedReviewResult).length, desc: '当前页审核通过记录' },
  { label: '审核驳回', value: records.value.filter((item) => item.reviewResult === rejectedReviewResult).length, desc: '当前页审核驳回记录' }
])

async function loadData() {
  const result = await fetchReviewsApi(query)
  records.value = result.records || []
  total.value = result.total || 0
}

async function approveFirst() {
  const detectionResult = await fetchDetectionsApi({ pageNum: 1, pageSize: DEFAULT_PAGE_SIZE })
  const record = detectionResult.records?.find((item) => item.detectionStatus === reviewPendingDetectionStatus)
  if (!record) {
    ElMessage.warning('当前没有待审核的检测记录')
    return
  }

  await submitReviewApi({
    detectionRecordId: record.id,
    reviewResult: approvedReviewResult,
    reviewRemark: '数据合格，允许出具报告'
  })

  ElMessage.success('审核通过')
  query.pageNum = 1
  await loadData()
}

async function rejectFirst() {
  const detectionResult = await fetchDetectionsApi({ pageNum: 1, pageSize: DEFAULT_PAGE_SIZE })
  const record = detectionResult.records?.find((item) => item.detectionStatus === reviewPendingDetectionStatus)
  if (!record) {
    ElMessage.warning('当前没有待审核的检测记录')
    return
  }

  await submitReviewApi({
    detectionRecordId: record.id,
    reviewResult: rejectedReviewResult,
    rejectReason: '原始记录不完整，退回重检',
    reviewRemark: '请补充记录后重新提交'
  })

  ElMessage.success('已驳回并退回重检')
  query.pageNum = 1
  await loadData()
}

onMounted(loadData)
</script>

<style scoped>
.section-head {
  margin-bottom: 16px;
}

.section-title {
  margin: 0;
  font-size: 18px;
  line-height: 1.4;
}

.metric-card p {
  margin: 10px 0 0;
  color: var(--text-sub);
  font-size: 14px;
  line-height: 1.6;
}
</style>
