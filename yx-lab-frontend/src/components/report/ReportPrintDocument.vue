<template>
  <div ref="printRootRef" class="report-print-root">
    <div class="report-preview-shell">
      <section
        v-for="page in pages"
        :key="page.pageNo"
        class="report-paper"
        :class="{ 'report-paper--last': page.pageNo === totalPages }"
      >
        <header class="paper-header">
          <div class="paper-header__side">
            <span>样品编号：{{ previewData.sampleNo || '-' }}</span>
            <span>封签编号：{{ previewData.sealNo || '-' }}</span>
          </div>
          <div class="paper-header__title">
            <h1>{{ previewData.reportName || '化验报告' }}</h1>
            <p>{{ page.pageNo === 1 ? 'A4 通用打印模板' : 'A4 通用打印模板（续页）' }}</p>
          </div>
          <div class="paper-header__side paper-header__side--right">
            <span>第 {{ page.pageNo }} / {{ totalPages }} 页</span>
            <span>生成时间：{{ previewData.generatedTime || '-' }}</span>
          </div>
        </header>

        <section v-if="page.pageNo === 1" class="paper-section">
          <h2>一、样品基础信息</h2>
          <table class="info-table">
            <tbody>
              <tr>
                <td class="label">报告名称</td>
                <td>{{ previewData.reportName || '-' }}</td>
                <td class="label">报告类型</td>
                <td>{{ previewData.reportTypeLabel || '-' }}</td>
                <td class="label">报告状态</td>
                <td>{{ previewData.reportStatusLabel || '-' }}</td>
              </tr>
              <tr>
                <td class="label">样品编号</td>
                <td>{{ previewData.sampleNo || '-' }}</td>
                <td class="label">封签编号</td>
                <td>{{ previewData.sealNo || '-' }}</td>
                <td class="label">点位名称</td>
                <td>{{ previewData.pointName || '-' }}</td>
              </tr>
              <tr>
                <td class="label">样品类型</td>
                <td>{{ previewData.sampleTypeLabel || '-' }}</td>
                <td class="label">样品状态</td>
                <td>{{ previewData.sampleStatusLabel || '-' }}</td>
                <td class="label">结果摘要</td>
                <td>{{ previewData.resultSummary || '-' }}</td>
              </tr>
              <tr>
                <td class="label">采样时间</td>
                <td>{{ previewData.samplingTime || '-' }}</td>
                <td class="label">封签时间</td>
                <td>{{ previewData.sealTime || '-' }}</td>
                <td class="label">采样人员</td>
                <td>{{ previewData.samplerName || '-' }}</td>
              </tr>
              <tr>
                <td class="label">天气情况</td>
                <td>{{ previewData.weather || '-' }}</td>
                <td class="label">保存条件</td>
                <td>{{ previewData.storageCondition || '-' }}</td>
                <td class="label">样品备注</td>
                <td>{{ previewData.sampleRemark || '-' }}</td>
              </tr>
            </tbody>
          </table>
        </section>

        <section v-if="page.pageNo === 1" class="paper-section">
          <h2>二、流程与审查信息</h2>
          <table class="info-table">
            <tbody>
              <tr>
                <td class="label">化验时间</td>
                <td>{{ previewData.detectionTime || '-' }}</td>
                <td class="label">化验人员</td>
                <td>{{ previewData.detectorName || '-' }}</td>
                <td class="label">化验结论</td>
                <td>{{ previewData.detectionResultLabel || '-' }}</td>
              </tr>
              <tr>
                <td class="label">审核时间</td>
                <td>{{ previewData.reviewTime || '-' }}</td>
                <td class="label">审核人员</td>
                <td>{{ previewData.reviewerName || '-' }}</td>
                <td class="label">审核结论</td>
                <td>{{ previewData.reviewResultLabel || '-' }}</td>
              </tr>
              <tr>
                <td class="label">发布时间</td>
                <td>{{ previewData.publishedTime || '-' }}</td>
                <td class="label">发布人</td>
                <td>{{ previewData.publishedByName || '-' }}</td>
                <td class="label">推送状态</td>
                <td>{{ previewData.pushStatusLabel || '-' }}</td>
              </tr>
              <tr>
                <td class="label">最近推送时间</td>
                <td>{{ previewData.lastPushTime || '-' }}</td>
                <td class="label label--wide">推送结果说明</td>
                <td colspan="3">{{ previewData.lastPushMessage || '-' }}</td>
              </tr>
            </tbody>
          </table>
        </section>

        <section v-if="page.pageNo === 1" class="paper-section paper-summary">
          <article class="summary-card">
            <span>参数总数</span>
            <strong>{{ previewData.parameterCount || 0 }}</strong>
          </article>
          <article class="summary-card">
            <span>正常项</span>
            <strong class="is-success">{{ previewData.normalCount || 0 }}</strong>
          </article>
          <article class="summary-card">
            <span>异常项</span>
            <strong class="is-danger">{{ previewData.abnormalCount || 0 }}</strong>
          </article>
          <article class="summary-card">
            <span>流程说明</span>
            <strong class="is-text">{{ previewData.recordRemark || '-' }}</strong>
          </article>
        </section>

        <section class="paper-section">
          <h2>{{ page.pageNo === 1 ? '三、化验结果明细' : '化验结果明细（续页）' }}</h2>
          <table v-if="page.items.length" class="result-table">
            <thead>
              <tr>
                <th class="col-index">序号</th>
                <th class="col-parameter">检测参数</th>
                <th class="col-unit">单位</th>
                <th class="col-method">检测方法</th>
                <th class="col-standard">标准范围</th>
                <th class="col-reference">参考范围</th>
                <th class="col-value">化验值</th>
                <th class="col-judge">单项判定</th>
                <th class="col-status">子流程状态</th>
              </tr>
            </thead>
            <tbody>
              <template v-for="(item, index) in page.items" :key="`${page.pageNo}-${index}`">
                <tr class="result-main-row">
                  <td class="cell-center">{{ page.startIndex + index + 1 }}</td>
                  <td>{{ item.parameterName || '-' }}</td>
                  <td class="cell-center">{{ item.unit || '-' }}</td>
                  <td>{{ item.methodName || '-' }}</td>
                  <td>{{ item.standardRange || '-' }}</td>
                  <td>{{ item.referenceStandard || '-' }}</td>
                  <td class="cell-center">{{ item.resultValue || '-' }}</td>
                  <td class="cell-center">
                    <span
                      :class="[
                        'state-tag',
                        item.judgmentLabel === '异常' ? 'state-tag--danger' : 'state-tag--success'
                      ]"
                    >
                      {{ item.judgmentLabel || '-' }}
                    </span>
                  </td>
                  <td class="cell-center">
                    <span :class="['state-tag', resolveStatusClass(item.itemStatusLabel)]">
                      {{ item.itemStatusLabel || '-' }}
                    </span>
                  </td>
                </tr>
                <tr class="result-compare-row">
                  <td class="compare-label">结果对比</td>
                  <td colspan="8" :class="item.judgmentLabel === '异常' ? 'text-danger' : 'text-success'">
                    {{ item.compareText || '-' }}
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
          <div v-else class="empty-box">当前报告暂无化验结果明细。</div>
        </section>

        <section v-if="page.pageNo === totalPages" class="paper-section">
          <h2>四、补充说明与签字</h2>
          <table class="info-table">
            <tbody>
              <tr>
                <td class="label label--wide">审核意见</td>
                <td colspan="5">{{ previewData.reviewRemark || '-' }}</td>
              </tr>
              <tr>
                <td class="label label--wide">驳回原因</td>
                <td colspan="5">{{ previewData.rejectReason || '-' }}</td>
              </tr>
              <tr>
                <td class="label label--wide">流程留痕</td>
                <td colspan="5" class="trace-text">{{ previewData.traceLog || '当前样品暂无额外流程留痕。' }}</td>
              </tr>
              <tr>
                <td class="label">化验人员</td>
                <td>{{ previewData.detectorName || '________________' }}</td>
                <td class="label">审核人员</td>
                <td>{{ previewData.reviewerName || '________________' }}</td>
                <td class="label">签字日期</td>
                <td>________________</td>
              </tr>
            </tbody>
          </table>
        </section>

        <footer class="paper-footer">
          <span>阳新化验室管理系统通用报告模板</span>
          <span>适配 A4 纵向打印，内容超出自动续页</span>
        </footer>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, ref } from 'vue'

const props = defineProps({
  previewData: {
    type: Object,
    default: () => ({})
  }
})

const FIRST_PAGE_ITEM_COUNT = 4
const OTHER_PAGE_ITEM_COUNT = 6

const printRootRef = ref(null)

const previewItems = computed(() => Array.isArray(props.previewData?.items) ? props.previewData.items : [])

const pages = computed(() => {
  const items = previewItems.value
  if (!items.length) {
    return [{ pageNo: 1, startIndex: 0, items: [] }]
  }
  const result = []
  let pageNo = 1
  let cursor = 0
  while (cursor < items.length) {
    const pageSize = pageNo === 1 ? FIRST_PAGE_ITEM_COUNT : OTHER_PAGE_ITEM_COUNT
    result.push({
      pageNo,
      startIndex: cursor,
      items: items.slice(cursor, cursor + pageSize)
    })
    cursor += pageSize
    pageNo += 1
  }
  return result
})

const totalPages = computed(() => pages.value.length)

function resolveStatusClass(statusLabel) {
  if (!statusLabel) {
    return 'state-tag--plain'
  }
  if (statusLabel.includes('驳回') || statusLabel.includes('异常')) {
    return 'state-tag--danger'
  }
  if (statusLabel.includes('待') || statusLabel.includes('提交')) {
    return 'state-tag--warning'
  }
  if (statusLabel.includes('完成') || statusLabel.includes('通过') || statusLabel.includes('正常')) {
    return 'state-tag--success'
  }
  return 'state-tag--plain'
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

defineExpose({
  printDocument
})
</script>

<style scoped>
.report-preview-shell {
  padding: 18px 0 10px;
  background: linear-gradient(180deg, #edf3fb 0%, #e8eef8 100%);
}

.report-paper {
  width: 210mm;
  min-height: 297mm;
  margin: 0 auto 18px;
  padding: 10mm 11mm 10mm;
  box-sizing: border-box;
  background: #ffffff;
  color: #0f172a;
  box-shadow: 0 14px 36px rgba(15, 23, 42, 0.14);
  break-after: page;
  page-break-after: always;
}

.report-paper--last {
  break-after: auto;
  page-break-after: auto;
}

.paper-header {
  display: grid;
  grid-template-columns: 1fr 1.3fr 1fr;
  gap: 10px;
  align-items: start;
  padding-bottom: 10px;
  border-bottom: 2px solid #1d4ed8;
}

.paper-header__title {
  text-align: center;
}

.paper-header__title h1 {
  margin: 0;
  font-size: 24px;
  line-height: 1.25;
  letter-spacing: 1px;
}

.paper-header__title p {
  margin: 6px 0 0;
  font-size: 12px;
  color: #64748b;
}

.paper-header__side {
  display: grid;
  gap: 6px;
  font-size: 12px;
  color: #475569;
}

.paper-header__side--right {
  text-align: right;
}

.paper-section {
  margin-top: 12px;
}

.paper-section h2 {
  margin: 0 0 8px;
  font-size: 15px;
  color: #0f172a;
}

.paper-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.summary-card {
  min-height: 76px;
  padding: 10px 12px;
  border: 1px solid #d8e2f1;
  border-radius: 10px;
  background: linear-gradient(180deg, #f9fbff 0%, #f4f8ff 100%);
}

.summary-card span {
  display: block;
  font-size: 12px;
  color: #64748b;
}

.summary-card strong {
  display: block;
  margin-top: 10px;
  font-size: 22px;
  color: #0f172a;
}

.summary-card .is-success {
  color: #047857;
}

.summary-card .is-danger {
  color: #dc2626;
}

.summary-card .is-text {
  font-size: 13px;
  line-height: 1.55;
}

.info-table,
.result-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}

.info-table td,
.result-table th,
.result-table td {
  border: 1px solid #d9e2f1;
  padding: 7px 8px;
  font-size: 11px;
  line-height: 1.5;
  vertical-align: top;
  word-break: break-word;
  overflow-wrap: anywhere;
}

.info-table td {
  vertical-align: middle;
}

.info-table td.label {
  width: 11%;
  background: #f5f9ff;
  text-align: center;
  font-weight: 700;
  color: #334155;
}

.label--wide {
  width: 12%;
}

.result-table th {
  background: #eef4ff;
  text-align: center;
  font-weight: 700;
  color: #1e3a5f;
  vertical-align: middle;
}

.result-table td {
  background: #ffffff;
}

.result-main-row:nth-of-type(4n + 1) td,
.result-main-row:nth-of-type(4n + 2) td {
  background: #fbfdff;
}

.result-compare-row td {
  background: #f8fbff;
}

.col-index {
  width: 6%;
}

.col-parameter {
  width: 13%;
}

.col-unit {
  width: 8%;
}

.col-method {
  width: 18%;
}

.col-standard {
  width: 13%;
}

.col-reference {
  width: 14%;
}

.col-value {
  width: 8%;
}

.col-judge {
  width: 10%;
}

.col-status {
  width: 10%;
}

.cell-center {
  text-align: center;
  vertical-align: middle !important;
}

.compare-label {
  text-align: center;
  color: #475569;
  font-weight: 700;
  vertical-align: middle !important;
}

.state-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
}

.state-tag--success {
  color: #047857;
  background: rgba(4, 120, 87, 0.12);
}

.state-tag--danger {
  color: #dc2626;
  background: rgba(220, 38, 38, 0.12);
}

.state-tag--warning {
  color: #b45309;
  background: rgba(245, 158, 11, 0.16);
}

.state-tag--plain {
  color: #475569;
  background: #e2e8f0;
}

.text-success {
  color: #047857;
  font-weight: 700;
}

.text-danger {
  color: #dc2626;
  font-weight: 700;
}

.trace-text {
  white-space: pre-wrap;
}

.empty-box {
  min-height: 120px;
  display: grid;
  place-items: center;
  border: 1px dashed #cbd5e1;
  border-radius: 12px;
  color: #64748b;
  font-size: 13px;
}

.paper-footer {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-top: 12px;
  font-size: 11px;
  color: #64748b;
}

@media (max-width: 1200px) {
  .report-preview-shell {
    overflow-x: auto;
    padding-left: 12px;
    padding-right: 12px;
  }

  .report-paper {
    min-width: 210mm;
  }
}
</style>

<style>
@page {
  size: A4 portrait;
  margin: 8mm;
}

@media print {
  body.report-printing {
    background: #ffffff !important;
  }

  body.report-printing > * {
    visibility: hidden !important;
  }

  body.report-printing .report-print-root,
  body.report-printing .report-print-root * {
    visibility: visible !important;
  }

  body.report-printing .report-print-root {
    position: absolute !important;
    inset: 0 !important;
    width: 100% !important;
    background: #ffffff !important;
  }

  body.report-printing .report-preview-shell {
    padding: 0 !important;
    background: #ffffff !important;
  }

  body.report-printing .report-paper {
    margin: 0 auto !important;
    box-shadow: none !important;
  }
}
</style>
