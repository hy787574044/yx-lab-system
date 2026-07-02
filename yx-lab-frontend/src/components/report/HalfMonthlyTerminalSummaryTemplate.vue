<template>
  <div class="half-month-template">
    <div class="template-title">{{ reportUnit }}管网水检测报告</div>

    <div class="date-row">
      <span>采样日期：{{ sampleDateLabel }}</span>
      <span>检测日期：{{ reportDateLabel }}</span>
    </div>

    <table class="half-month-table">
      <colgroup>
        <col class="col-address" />
        <col class="col-color" />
        <col class="col-turbidity" />
        <col class="col-ph" />
        <col class="col-smell" />
        <col class="col-chlorine" />
        <col class="col-bacteria" />
        <col class="col-coliform" />
        <col class="col-index" />
      </colgroup>
      <thead>
        <tr>
          <th class="corner-cell">
            <span class="corner-project">检测项目</span>
            <span class="corner-address">地址</span>
          </th>
          <th>色度<br />（度）</th>
          <th>浑浊度<br />（NTU）</th>
          <th>PH</th>
          <th>臭和味</th>
          <th>游离氯<br />（mg/L）</th>
          <th>菌落总数<br />（CFU/ml）</th>
          <th>总大肠菌群<br />（MPN/100ml）</th>
          <th>高锰酸盐指数<br />（mg/L）</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in paddedRows" :key="row.key">
          <td class="address-cell">{{ row.pointName || '' }}</td>
          <td>{{ cell(row, '色度(度)') }}</td>
          <td>{{ cell(row, '浑浊度(NTU)') }}</td>
          <td>{{ cell(row, 'PH') }}</td>
          <td>{{ cell(row, '臭和味') }}</td>
          <td>{{ cell(row, '游离氯(mg/L)') }}</td>
          <td>{{ cell(row, '菌落总数(CFU/ml)') }}</td>
          <td>{{ cell(row, '总大肠菌群(MPN/100ml)') }}</td>
          <td>{{ cell(row, '高锰酸盐指数(mg/L)') }}</td>
        </tr>
      </tbody>
    </table>

    <div class="signature-row">
      <span>检测人员：{{ previewData?.inspectorName || '' }}</span>
      <span>负责人：{{ previewData?.principalName || '' }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  previewData: {
    type: Object,
    default: null
  }
})

const reportUnit = computed(() => props.previewData?.reportUnit || '阳新县城发水务有限公司')
const rows = computed(() => props.previewData?.rows || [])
const minRows = 16

const paddedRows = computed(() => {
  const list = rows.value.map((row, index) => ({
    ...row,
    key: row.rowKey || row.pointName || `row-${index}`
  }))
  while (list.length < minRows) {
    list.push({ key: `empty-${list.length}`, parameterValues: {} })
  }
  return list
})

const sampleDateLabel = computed(() => formatDate(props.previewData?.periodStart))
const reportDateLabel = computed(() => formatDate(new Date()))

function formatDate(value) {
  const date = value ? new Date(value) : new Date()
  if (Number.isNaN(date.getTime())) {
    return ''
  }
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function cell(row, key) {
  return row?.parameterValues?.[key] || ''
}
</script>

<style scoped>
.half-month-template {
  padding: 18px 18px 14px;
  color: #111827;
  font-family: SimSun, "Microsoft YaHei", sans-serif;
}

.template-title {
  margin-bottom: 16px;
  text-align: center;
  font-size: 24px;
  font-weight: 700;
  line-height: 1.5;
  letter-spacing: 0.04em;
}

.date-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  margin: 0 40px 14px;
  font-size: 16px;
}

.date-row span:first-child {
  text-align: left;
}

.date-row span:last-child {
  text-align: center;
}

.half-month-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}

.half-month-table th,
.half-month-table td {
  border: 1px solid #111827;
  padding: 6px 5px;
  text-align: center;
  font-size: 14px;
  line-height: 1.35;
  word-break: break-word;
}

.half-month-table thead th {
  height: 78px;
  font-weight: 600;
}

.half-month-table tbody td {
  height: 42px;
}

.corner-cell {
  position: relative;
  padding: 0 !important;
  overflow: hidden;
}

.corner-cell::before {
  content: "";
  position: absolute;
  inset: -1px;
  background: linear-gradient(
    to top right,
    transparent calc(50% - 0.75px),
    #111827 calc(50% - 0.75px),
    #111827 calc(50% + 0.75px),
    transparent calc(50% + 0.75px)
  );
  pointer-events: none;
}

.corner-project,
.corner-address {
  position: absolute;
  z-index: 1;
  font-size: 14px;
  font-weight: 600;
}

.corner-project {
  right: 22px;
  top: 9px;
}

.corner-address {
  left: 16px;
  bottom: 18px;
}

.address-cell {
  text-align: left;
  padding-left: 10px !important;
}

.col-address {
  width: 180px;
}

.col-color {
  width: 86px;
}

.col-turbidity {
  width: 116px;
}

.col-ph {
  width: 104px;
}

.col-smell {
  width: 104px;
}

.col-chlorine {
  width: 132px;
}

.col-bacteria {
  width: 172px;
}

.col-coliform {
  width: 184px;
}

.col-index {
  width: 180px;
}

.signature-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  margin-top: 12px;
  padding: 0 120px;
  font-size: 16px;
}
</style>
