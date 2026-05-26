<template>
  <div ref="printRootRef" class="report-print-root">
    <div :class="['report-preview-shell', { 'report-preview-shell--measure': singlePage }]">
      <section
        v-for="page in pages"
        :key="page.pageNo"
        :class="[
          'report-paper',
          {
            'report-paper--last': page.pageNo === totalPages,
            'report-paper--measure': singlePage
          }
        ]"
      >
        <header class="report-header">
          <h1>{{ template.title }}</h1>
          <div class="report-meta">
            <span>报告编号：{{ reportNo }}</span>
            <span>生成日期：{{ generatedDate }}</span>
            <span v-if="totalPages > 1">第 {{ page.pageNo }} / {{ totalPages }} 页</span>
          </div>
        </header>

        <template v-if="page.pageNo === 1">
          <section class="report-section">
            <h2>一、样品信息</h2>
            <table class="report-table info-table">
              <tbody>
                <tr>
                  <th>样品编号</th>
                  <td>{{ text(previewData.sampleNo) }}</td>
                  <th>样品类型</th>
                  <td>{{ text(previewData.sampleTypeLabel) }}</td>
                </tr>
                <tr>
                  <th>采样点位</th>
                  <td>{{ text(previewData.pointName) }}</td>
                  <th>采样时间</th>
                  <td>{{ text(previewData.samplingTime) }}</td>
                </tr>
                <tr>
                  <th>采样人员</th>
                  <td>{{ text(previewData.samplerName) }}</td>
                  <th>质控品类</th>
                  <td>{{ text(previewData.qualityControlTypeLabel) }}</td>
                </tr>
              </tbody>
            </table>
          </section>
        </template>

        <section class="report-section">
          <h2>{{ page.pageNo === 1 ? '二、参数信息' : '二、参数信息（续页）' }}</h2>
          <table v-if="page.items.length" class="report-table parameter-table">
            <thead>
              <tr>
                <th class="col-index">序号</th>
                <th>检测参数</th>
                <th>检测方法</th>
                <th>检测标准</th>
                <th>标准范围</th>
                <th>检测值</th>
                <th>检测结果</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(item, index) in page.items" :key="`${page.pageNo}-${index}`">
                <td>{{ page.startIndex + index + 1 }}</td>
                <td>{{ text(item.parameterName) }}</td>
                <td>{{ text(item.methodName) }}</td>
                <td>{{ text(item.referenceStandard) }}</td>
                <td>{{ formatRange(item) }}</td>
                <td>{{ text(item.resultValue) }}</td>
                <td :class="['judge-cell', isAbnormal(item) ? 'is-danger' : 'is-success']">
                  {{ formatJudgment(item) }}
                </td>
              </tr>
            </tbody>
          </table>
          <div v-else class="empty-box">当前报告暂无化验结果明细。</div>

          <template v-if="page.pageNo === totalPages">
            <p class="basis-line">判定依据：{{ judgmentBasis }}</p>
            <p class="overall-line">整体评价：{{ overallEvaluation }}</p>
          </template>
        </section>

        <section v-if="page.pageNo === totalPages" class="report-section">
          <h2>三、检测信息</h2>
          <table class="report-table detection-table">
            <thead>
              <tr>
                <th>检测参数</th>
                <th>检测人员</th>
                <th>检测开始时间</th>
                <th>检测完成时间</th>
                <th>耗时</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, index) in detectionRows" :key="index">
                <td class="merged-parameters">{{ row.parameters }}</td>
                <td>{{ row.detectorName }}</td>
                <td>{{ row.startTime }}</td>
                <td>{{ row.endTime }}</td>
                <td>{{ row.duration }}</td>
              </tr>
            </tbody>
          </table>
        </section>

        <section v-if="page.pageNo === totalPages" class="report-section">
          <h2>四、签审信息</h2>
          <div class="sign-box">
            <table class="report-table sign-table">
              <tbody>
                <tr>
                  <th>编制人员</th>
                  <td>{{ compilerName }}</td>
                  <th>审核人员</th>
                  <td>{{ text(previewData.reviewerName) }}</td>
                </tr>
                <tr>
                  <th>批准人员</th>
                  <td>{{ text(previewData.publishedByName) }}</td>
                  <th>签发日期</th>
                  <td>{{ signDate }}</td>
                </tr>
                <tr>
                  <th>检测单位</th>
                  <td colspan="3">{{ template.organizationName }}</td>
                </tr>
              </tbody>
            </table>
            <div class="report-stamp">
              <span>云河化验报告</span>
              <strong>化验室</strong>
            </div>
          </div>
        </section>

        <section v-if="page.pageNo === totalPages" class="report-section">
          <h2>五、备注声明</h2>
          <div class="note-box">
            <ol class="note-list">
              <li v-for="item in template.notes" :key="item">{{ item }}</li>
            </ol>
          </div>
        </section>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, ref } from 'vue'
import {
  FIRST_PAGE_ITEM_COUNT,
  OTHER_PAGE_ITEM_COUNT,
  REPORT_TEMPLATE,
  buildOverallEvaluation,
  buildReportNo,
  normalizeDisplay,
  toDateText
} from './reportPrintTemplate'

const props = defineProps({
  previewData: {
    type: Object,
    default: () => ({})
  },
  singlePage: {
    type: Boolean,
    default: false
  }
})

const printRootRef = ref(null)

const template = REPORT_TEMPLATE

const previewItems = computed(() => Array.isArray(props.previewData?.items) ? props.previewData.items : [])

const pages = computed(() => {
  const items = previewItems.value
  if (props.singlePage) {
    return [{ pageNo: 1, startIndex: 0, items }]
  }
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
const reportNo = computed(() => buildReportNo(props.previewData))
const generatedDate = computed(() => toDateText(props.previewData?.generatedTime))
const signDate = computed(() => toDateText(props.previewData?.publishedTime || props.previewData?.reviewTime || props.previewData?.generatedTime))
const compilerName = computed(() => text(props.previewData?.publishedByName || normalizeDetectorName(props.previewData?.detectorName)))
const judgmentBasis = computed(() => normalizeDisplay(props.previewData?.judgmentBasis, template.judgmentBasis))
const overallEvaluation = computed(() => buildOverallEvaluation(props.previewData))
const detectionRows = computed(() => {
  const rows = new Map()
  previewItems.value.forEach((item) => {
    const detectorName = normalizeDetectorName(item.detectorName)
    const detectorKey = detectorName || '未指定'
    const itemStartTime = normalizeDisplay(item.startTime || props.previewData?.detectionTime, '')
    const itemEndTime = normalizeDisplay(item.endTime || props.previewData?.reviewTime || props.previewData?.detectionTime, '')
    if (!rows.has(detectorKey)) {
      rows.set(detectorKey, {
        parameters: [],
        detectorName: detectorKey,
        startTime: itemStartTime,
        endTime: itemEndTime
      })
    }
    const row = rows.get(detectorKey)
    row.startTime = pickEarlierTime(row.startTime, itemStartTime)
    row.endTime = pickLaterTime(row.endTime, itemEndTime)
    const parameterName = normalizeDisplay(item.parameterName, '')
    if (parameterName && !row.parameters.includes(parameterName)) {
      row.parameters.push(parameterName)
    }
  })

  if (!rows.size) {
    const detectorName = normalizeDetectorName(props.previewData?.detectorName) || '未指定'
    const startTime = normalizeDisplay(props.previewData?.detectionTime, '')
    const endTime = normalizeDisplay(props.previewData?.reviewTime || props.previewData?.detectionTime, '')
    return [{
      parameters: '-',
      detectorName,
      startTime: normalizeDisplay(startTime),
      endTime: normalizeDisplay(endTime),
      duration: calculateDurationText(startTime, endTime)
    }]
  }

  return Array.from(rows.values()).map((row) => ({
    ...row,
    parameters: row.parameters.length ? row.parameters.join('、') : '-',
    startTime: normalizeDisplay(row.startTime),
    endTime: normalizeDisplay(row.endTime),
    duration: calculateDurationText(row.startTime, row.endTime)
  })).sort((left, right) => left.detectorName.localeCompare(right.detectorName, 'zh-CN'))
})

function text(value) {
  return normalizeDisplay(value)
}

function normalizeDetectorName(value) {
  const name = normalizeDisplay(value, '')
  return name && name !== '协同检测' ? name : ''
}

function parseTime(value) {
  const text = normalizeDisplay(value, '')
  if (!text) {
    return null
  }
  const timestamp = Date.parse(text.replace(' ', 'T'))
  return Number.isNaN(timestamp) ? null : timestamp
}

function pickEarlierTime(left, right) {
  const leftTime = parseTime(left)
  const rightTime = parseTime(right)
  if (leftTime == null) {
    return right || left
  }
  if (rightTime == null) {
    return left
  }
  return rightTime < leftTime ? right : left
}

function pickLaterTime(left, right) {
  const leftTime = parseTime(left)
  const rightTime = parseTime(right)
  if (leftTime == null) {
    return right || left
  }
  if (rightTime == null) {
    return left
  }
  return rightTime > leftTime ? right : left
}

function calculateDurationText(startTime, endTime) {
  const start = parseTime(startTime)
  const end = parseTime(endTime)
  if (start == null || end == null || end < start) {
    return '-'
  }
  return `${Math.ceil((end - start) / 60000)} min`
}

function isAbnormal(item) {
  const label = normalizeDisplay(item?.judgmentLabel, '')
  return label.includes('异常') || label.includes('超标') || label.includes('不合格')
}

function formatJudgment(item) {
  if (!normalizeDisplay(item?.judgmentLabel, '')) {
    return '-'
  }
  return isAbnormal(item) ? '超标' : '合格'
}

function formatRange(item) {
  const range = normalizeDisplay(item?.standardRange, '')
  const unit = normalizeDisplay(item?.unit, '')
  if (!range) {
    return '-'
  }
  if (!unit || unit === '-') {
    return range
  }
  return range.includes(unit) ? range : `${range} ${unit}`
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
  const paper = printRootRef.value?.querySelector('.report-paper')
  return paper?.scrollHeight || paper?.offsetHeight || printRootRef.value?.scrollHeight || 0
}

defineExpose({
  printDocument,
  measureHeightPx
})
</script>

<style scoped>
.report-preview-shell {
  padding: 18px 0 10px;
  background: linear-gradient(180deg, #edf3fb 0%, #e8eef8 100%);
}

.report-preview-shell--measure {
  padding: 0;
  background: transparent;
}

.report-paper {
  position: relative;
  width: 210mm;
  min-height: 297mm;
  margin: 0 auto 18px;
  padding: 8mm 10mm 10mm;
  box-sizing: border-box;
  border: 1px solid #6b7280;
  background: #ffffff;
  color: #202733;
  font-family: "Microsoft YaHei", "SimSun", "SimHei", sans-serif;
  box-shadow: 0 14px 36px rgba(15, 23, 42, 0.14);
  break-after: page;
  page-break-after: always;
}

.report-paper--last {
  break-after: auto;
  page-break-after: auto;
}

.report-paper--measure {
  min-height: 0;
  margin: 0;
  box-shadow: none;
}

.report-header {
  margin: -8mm -10mm 0;
  padding: 5mm 8mm 4.5mm;
  border-bottom: 1px solid #9ca3af;
  text-align: center;
  background: linear-gradient(180deg, #ffffff 0%, #fbfdff 100%);
}

.report-header h1 {
  margin: 0 0 5.5mm;
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 1px;
  color: #172033;
}

.report-meta {
  display: grid;
  grid-template-columns: 1fr 1fr minmax(0, auto);
  gap: 10mm;
  text-align: left;
  font-size: 14px;
  color: #202733;
}

.report-meta span {
  white-space: nowrap;
}

.report-section {
  margin-top: 7.2mm;
}

.report-section h2 {
  display: flex;
  align-items: center;
  gap: 2.5mm;
  margin: 0 0 4mm;
  font-size: 15px;
  font-weight: 600;
  color: #111827;
}

.report-section h2::before {
  content: "";
  width: 2.5mm;
  height: 4mm;
  border-radius: 1px;
  background: #2f6f9f;
}

.sign-box {
  position: relative;
  border: 1px solid #8d96a3;
  border-radius: 4px;
  padding: 4mm;
  background: #ffffff;
}

.report-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
  border: 1px solid #8d96a3;
}

.report-table th,
.report-table td {
  border: 1px solid #9aa3af;
  padding: 3.1mm 2.6mm;
  text-align: center;
  vertical-align: middle;
  font-size: 13px;
  line-height: 1.32;
  word-break: break-word;
}

.report-table th {
  font-weight: 600;
  color: #172033;
  background: #f3f7fb;
}

.info-table {
  overflow: hidden;
  border: 0;
  border-radius: 0;
}

.info-table th {
  width: 18%;
  color: #1f344d;
  background: #f2f6fb;
}

.info-table td {
  width: 32%;
  text-align: left;
  padding-left: 5mm;
  color: #202733;
  background: #ffffff;
}

.col-index {
  width: 11%;
}

.judge-cell {
  font-weight: 600;
}

.judge-cell.is-success {
  color: #00a63e;
}

.judge-cell.is-danger {
  color: #e60012;
}

.basis-line {
  margin: 6mm 0 0;
  font-size: 14px;
}

.overall-line {
  margin: 3.2mm 0 0;
  font-size: 14px;
  font-weight: 600;
}

.detection-table th,
.detection-table td {
  padding: 3.2mm 2.4mm;
}

.detection-table th:first-child,
.detection-table td:first-child {
  width: 22%;
}

.detection-table th:nth-child(2),
.detection-table td:nth-child(2) {
  width: 16%;
}

.detection-table th:nth-child(5),
.detection-table td:nth-child(5) {
  width: 11%;
}

.detection-table .merged-parameters {
  text-align: center;
  line-height: 1.45;
  color: #172033;
}

.sign-box {
  position: relative;
  min-height: 0;
  overflow: hidden;
  padding: 0;
}

.sign-table {
  width: 100%;
}

.sign-table th {
  width: 22%;
}

.sign-table td {
  text-align: left;
  padding-left: 5mm;
}

.report-stamp {
  position: absolute;
  right: 8mm;
  top: 50%;
  width: 34mm;
  height: 34mm;
  display: grid;
  place-items: center;
  border: 2.4px solid rgba(230, 0, 18, 0.88);
  border-radius: 50%;
  color: rgba(230, 0, 18, 0.92);
  transform: translateY(-50%) rotate(-12deg);
  opacity: 0.9;
  pointer-events: none;
}

.report-stamp::before {
  content: "★";
  position: absolute;
  top: 10.5mm;
  left: 0;
  right: 0;
  text-align: center;
  font-size: 14mm;
  line-height: 1;
}

.report-stamp span {
  position: absolute;
  top: 5.5mm;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 1.4px;
}

.report-stamp strong {
  position: absolute;
  bottom: 4mm;
  font-size: 17px;
  letter-spacing: 2px;
}

.note-list {
  margin: 0;
  padding-left: 18px;
  font-size: 13px;
  line-height: 2;
}

.note-box {
  border: 1px solid #8d96a3;
  border-radius: 4px;
  padding: 3.5mm 5mm;
  background: #fbfdff;
}

.empty-box {
  min-height: 36mm;
  display: grid;
  place-items: center;
  border: 1px dashed #9ca3af;
  color: #64748b;
  font-size: 13px;
}

@media print {
  .report-preview-shell {
    padding: 0;
    background: transparent;
  }

  .report-paper {
    width: 210mm;
    min-height: 297mm;
    margin: 0;
    box-shadow: none;
  }
}
</style>