<template>
  <div ref="printRootRef" class="raw-record-root">
    <section class="raw-record-paper">
      <header class="raw-record-header">
        <div>
          <h1>{{ previewData.reportName || '全流程原始记录' }}</h1>
          <p>
            <span>报告编号：{{ reportNo }}</span>
            <span>生成时间：{{ text(previewData.generatedTime) }}</span>
          </p>
        </div>
        <span class="raw-record-badge">{{ text(previewData.reportCategoryLabel || '全流程原始记录') }}</span>
      </header>

      <section class="summary-grid">
        <article class="summary-card">
          <span>样品编号</span>
          <strong>{{ text(previewData.sampleNo) }}</strong>
        </article>
        <article class="summary-card">
          <span>点位名称</span>
          <strong>{{ text(previewData.pointName) }}</strong>
        </article>
        <article class="summary-card">
          <span>检测结果</span>
          <strong>{{ text(previewData.detectionResultLabel) }}</strong>
        </article>
        <article class="summary-card">
          <span>审核结论</span>
          <strong>{{ text(previewData.reviewResultLabel) }}</strong>
        </article>
      </section>

      <section class="record-section">
        <h2>一、样品采样信息</h2>
        <table class="record-table info-table">
          <tbody>
            <tr>
              <th>样品编号</th>
              <td>{{ text(previewData.sampleNo) }}</td>
              <th>点位名称</th>
              <td>{{ text(previewData.pointName) }}</td>
            </tr>
            <tr>
              <th>样品类型</th>
              <td colspan="3">{{ text(previewData.sampleTypeLabel) }}</td>
            </tr>
            <tr>
              <th>采样时间</th>
              <td>{{ text(previewData.samplingTime) }}</td>
              <th>采样人员</th>
              <td colspan="3">{{ text(previewData.samplerName) }}</td>
            </tr>
            <tr>
              <th>天气情况</th>
              <td>{{ text(previewData.weather) }}</td>
              <th>保存条件</th>
              <td>{{ text(previewData.storageCondition) }}</td>
            </tr>
            <tr>
              <th>样品状态</th>
              <td>{{ text(previewData.sampleStatusLabel) }}</td>
              <th>结果摘要</th>
              <td>{{ text(previewData.resultSummary) }}</td>
            </tr>
            <tr>
              <th>样品备注</th>
              <td colspan="3">{{ text(previewData.sampleRemark) }}</td>
            </tr>
          </tbody>
        </table>
      </section>

      <section class="record-section">
        <h2>二、检测与审核流程</h2>
        <table class="record-table info-table">
          <tbody>
            <tr>
              <th>检测时间</th>
              <td>{{ text(previewData.detectionTime) }}</td>
              <th>检测人员</th>
              <td>{{ text(previewData.detectorName) }}</td>
            </tr>
            <tr>
              <th>检测结果</th>
              <td>{{ text(previewData.detectionResultLabel) }}</td>
              <th>流程状态</th>
              <td>{{ text(previewData.detectionStatusLabel) }}</td>
            </tr>
            <tr>
              <th>流程备注</th>
              <td>{{ text(previewData.recordRemark) }}</td>
              <th>审核时间</th>
              <td>{{ text(previewData.reviewTime) }}</td>
            </tr>
            <tr>
              <th>审核人员</th>
              <td>{{ text(previewData.reviewerName) }}</td>
              <th>审核结论</th>
              <td>{{ text(previewData.reviewResultLabel) }}</td>
            </tr>
            <tr>
              <th>审核意见</th>
              <td>{{ text(previewData.reviewRemark) }}</td>
              <th>驳回原因</th>
              <td>{{ text(previewData.rejectReason) }}</td>
            </tr>
            <tr>
              <th>发布时间</th>
              <td>{{ text(previewData.publishedTime) }}</td>
              <th>发布人</th>
              <td>{{ text(previewData.publishedByName) }}</td>
            </tr>
          </tbody>
        </table>
      </section>

      <section class="record-section">
        <h2>三、检测参数原始明细</h2>
        <table class="record-table detail-table">
          <thead>
            <tr>
              <th class="col-index">序号</th>
              <th>检测参数</th>
              <th>检测方法</th>
              <th>标准范围</th>
              <th>单位</th>
              <th>检测标准</th>
              <th>检测值</th>
              <th>结果对比</th>
              <th>判定</th>
              <th>状态</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="!items.length">
              <td colspan="10" class="empty-cell">当前报告暂无检测参数明细</td>
            </tr>
            <tr v-for="(item, index) in items" :key="`${index}-${item.parameterName || ''}`">
              <td class="cell-center">{{ index + 1 }}</td>
              <td>{{ text(item.parameterName) }}</td>
              <td>{{ text(item.methodName) }}</td>
              <td>{{ formatRange(item) }}</td>
              <td>{{ text(item.unit) }}</td>
              <td>{{ text(item.referenceStandard) }}</td>
              <td>{{ text(item.resultValue) }}</td>
              <td>{{ text(item.compareText) }}</td>
              <td :class="['cell-center', isAbnormal(item) ? 'is-danger' : 'is-success']">{{ text(item.judgmentLabel) }}</td>
              <td class="cell-center">{{ text(item.itemStatusLabel) }}</td>
            </tr>
          </tbody>
        </table>
      </section>

      <section class="record-section">
        <h2>四、流程轨迹与留痕</h2>
        <div class="trace-box">{{ text(previewData.traceLog, '当前样品暂无流程轨迹记录') }}</div>
      </section>

      <section class="record-section">
        <h2>五、签字留存</h2>
        <div class="sign-grid">
          <article class="sign-card">
            <span>检测人员</span>
            <strong>{{ text(previewData.detectorName, '________________') }}</strong>
          </article>
          <article class="sign-card">
            <span>审核人员</span>
            <strong>{{ text(previewData.reviewerName, '________________') }}</strong>
          </article>
          <article class="sign-card">
            <span>发布人员</span>
            <strong>{{ text(previewData.publishedByName, '________________') }}</strong>
          </article>
        </div>
      </section>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, ref } from 'vue'
import { buildReportNo, normalizeDisplay } from './reportPrintTemplate'

const props = defineProps({
  previewData: {
    type: Object,
    default: () => ({})
  }
})

const printRootRef = ref(null)
const items = computed(() => Array.isArray(props.previewData?.items) ? props.previewData.items : [])
const reportNo = computed(() => buildReportNo(props.previewData))

function text(value, fallback = '-') {
  return normalizeDisplay(value, fallback)
}

function isAbnormal(item) {
  return text(item?.judgmentLabel, '').includes('异常')
}

function formatRange(item) {
  const range = text(item?.standardRange, '')
  const unit = text(item?.unit, '')
  if (!range) {
    return '-'
  }
  if (!unit || unit === '-' || range.includes(unit)) {
    return range
  }
  return `${range} ${unit}`
}

async function printDocument() {
  if (!printRootRef.value) {
    return
  }
  document.body.classList.add('report-printing')
  await nextTick()
  window.print()
  window.setTimeout(() => {
    document.body.classList.remove('report-printing')
  }, 300)
}

function measureHeightPx() {
  return printRootRef.value?.scrollHeight || 0
}

defineExpose({
  printDocument,
  measureHeightPx
})
</script>

<style scoped>
.raw-record-root {
  padding: 18px 0 10px;
  background: linear-gradient(180deg, #edf3fb 0%, #e8eef8 100%);
}

.raw-record-paper {
  max-width: 1160px;
  margin: 0 auto;
  padding: 28px 30px 36px;
  border-radius: 18px;
  background: #ffffff;
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.08);
}

.raw-record-header {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
  padding-bottom: 18px;
  border-bottom: 2px solid #3b82f6;
}

.raw-record-header h1 {
  margin: 0;
  font-size: 30px;
  line-height: 1.3;
  color: #111827;
}

.raw-record-header p {
  margin: 10px 0 0;
  display: flex;
  gap: 18px;
  flex-wrap: wrap;
  font-size: 13px;
  color: #64748b;
}

.raw-record-badge {
  display: inline-flex;
  align-items: center;
  min-height: 36px;
  padding: 0 14px;
  border-radius: 999px;
  background: #dbeafe;
  color: #1d4ed8;
  font-size: 13px;
  font-weight: 700;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-top: 20px;
}

.summary-card {
  padding: 14px 16px;
  border: 1px solid #dbeafe;
  border-radius: 14px;
  background: linear-gradient(180deg, #f8fbff 0%, #eff6ff 100%);
}

.summary-card span {
  display: block;
  font-size: 12px;
  color: #64748b;
}

.summary-card strong {
  display: block;
  margin-top: 10px;
  font-size: 18px;
  line-height: 1.45;
  color: #0f172a;
}

.record-section {
  margin-top: 26px;
}

.record-section h2 {
  margin: 0 0 12px;
  padding-left: 10px;
  border-left: 4px solid #3b82f6;
  font-size: 18px;
  color: #111827;
}

.record-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}

.record-table th,
.record-table td {
  border: 1px solid #d8e2f0;
  padding: 10px 12px;
  line-height: 1.65;
  word-break: break-word;
}

.info-table th {
  width: 15%;
  background: #f8fbff;
  color: #475569;
}

.info-table td {
  width: 35%;
}

.detail-table th {
  background: #eff6ff;
  color: #1e3a5f;
  font-size: 13px;
}

.col-index {
  width: 58px;
}

.cell-center {
  text-align: center;
}

.empty-cell {
  text-align: center;
  color: #64748b;
}

.trace-box {
  min-height: 120px;
  padding: 14px 16px;
  border: 1px solid #d8e2f0;
  border-radius: 14px;
  background: #f8fbff;
  color: #334155;
  line-height: 1.8;
  white-space: pre-wrap;
}

.sign-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.sign-card {
  min-height: 96px;
  padding: 14px 16px;
  border: 1px solid #d8e2f0;
  border-radius: 14px;
  background: #ffffff;
}

.sign-card span {
  display: block;
  font-size: 12px;
  color: #64748b;
}

.sign-card strong {
  display: block;
  margin-top: 12px;
  font-size: 16px;
  color: #0f172a;
}

.is-success {
  color: #047857;
  font-weight: 700;
}

.is-danger {
  color: #dc2626;
  font-weight: 700;
}

@media print {
  .raw-record-root {
    padding: 0;
    background: #ffffff;
  }

  .raw-record-paper {
    max-width: none;
    padding: 0;
    border-radius: 0;
    box-shadow: none;
  }
}
</style>
