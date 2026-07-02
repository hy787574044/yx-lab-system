<template>
  <div class="weekly-template">
    <div class="template-title">{{ reportUnit }}出厂水常规九项检验报告单</div>
    <div class="report-date">{{ reportDateLabel }}</div>

    <table class="weekly-table">
      <colgroup>
        <col class="col-result" />
        <col class="col-small" />
        <col class="col-small" />
        <col class="col-medium" />
        <col class="col-medium" />
        <col class="col-small" />
        <col class="col-micro" />
        <col class="col-micro" />
        <col class="col-micro" />
        <col class="col-index" />
        <col class="col-remark" />
      </colgroup>
      <thead>
        <tr>
          <th class="corner-cell">
            <span class="corner-project">检验项目</span>
            <span class="corner-result">检验结果</span>
          </th>
          <th>色度<br />（度）</th>
          <th>浑浊度<br />（NTU）</th>
          <th>臭和味</th>
          <th>肉眼可见物</th>
          <th>余氯<br />（mg/L）</th>
          <th>菌落总数<br />（CFU/ml）</th>
          <th>总大肠菌群<br />（MPN/100ml）</th>
          <th>大肠埃希氏菌<br />（CFU/ml）</th>
          <th>高锰酸盐指数<br />（mg/L）</th>
          <th>备注</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in rows" :key="row.rowKey || row.pointName">
          <td class="plant-cell">{{ row.pointName || '-' }}</td>
          <td>{{ cell(row, '色度(度)') }}</td>
          <td>{{ cell(row, '浑浊度(NTU)') }}</td>
          <td>{{ cell(row, '臭和味') }}</td>
          <td>{{ cell(row, '肉眼可见物') }}</td>
          <td>{{ cell(row, '余氯(mg/L)') }}</td>
          <td>{{ cell(row, '菌落总数(CFU/ml)') }}</td>
          <td>{{ cell(row, '总大肠菌群(MPN/100ml)') }}</td>
          <td>{{ cell(row, '大肠埃希氏菌(CFU/ml)') }}</td>
          <td>{{ cell(row, '高锰酸盐指数(mg/L)') }}</td>
          <td>{{ cell(row, '备注') }}</td>
        </tr>
      </tbody>
    </table>

    <div class="signature-row">
      <span>检验人员：{{ previewData?.inspectorName || '' }}</span>
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

const reportDateLabel = computed(() => {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${year}/${month}/${day}`
})

function cell(row, key) {
  const value = row?.parameterValues?.[key]
  return value || ''
}
</script>

<style scoped>
.weekly-template {
  padding: 10px 8px 14px;
  color: #111827;
  font-family: SimSun, "Microsoft YaHei", sans-serif;
}

.template-title {
  margin-bottom: 8px;
  text-align: center;
  font-size: 24px;
  font-weight: 700;
  line-height: 1.5;
  letter-spacing: 0.04em;
}

.report-date {
  margin: 0 118px 2px 0;
  text-align: right;
  font-size: 13px;
  line-height: 20px;
}

.weekly-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}

.weekly-table th,
.weekly-table td {
  border: 1px solid #111827;
  padding: 8px 5px;
  text-align: center;
  font-size: 13px;
  line-height: 1.35;
  word-break: break-word;
}

.weekly-table thead th {
  height: 54px;
  font-weight: 600;
}

.weekly-table tbody td {
  height: 54px;
  font-size: 16px;
}

.corner-cell {
  position: relative;
  height: 78px !important;
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
.corner-result {
  position: absolute;
  z-index: 1;
  font-size: 13px;
  font-weight: 600;
}

.corner-project {
  right: 13px;
  top: 11px;
}

.corner-result {
  left: 18px;
  bottom: 17px;
}

.plant-cell {
  font-weight: 600;
}

.col-result {
  width: 140px;
}

.col-small {
  width: 66px;
}

.col-medium {
  width: 138px;
}

.col-micro {
  width: 92px;
}

.col-index {
  width: 100px;
}

.col-remark {
  width: 140px;
}

.signature-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  margin-top: 18px;
  padding: 0 120px 0 160px;
  font-size: 16px;
}
</style>
