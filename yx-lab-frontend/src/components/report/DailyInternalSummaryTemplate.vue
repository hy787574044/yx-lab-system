<template>
  <div class="daily-template template-internal">
    <div class="template-title">{{ reportUnit }}</div>
    <div class="template-subtitle">{{ plantTitle }}</div>

    <div class="meta-row">
      <div class="meta-cell meta-date">
        <span>{{ dateParts.year }}</span>
        <span>年</span>
        <span>{{ dateParts.month }}</span>
        <span>月</span>
        <span>{{ dateParts.day }}</span>
        <span>日</span>
      </div>
      <div class="meta-cell">{{ previewData?.weekdayLabel || '-' }}</div>
      <div class="meta-cell">天气：{{ previewData?.weather || '-' }}</div>
    </div>

    <table class="report-table">
      <colgroup>
        <col class="col-time" />
        <col v-for="column in 15" :key="column" />
      </colgroup>
      <thead>
        <tr>
          <th rowspan="2" class="th-corner">
            <span class="corner-time">时间</span>
            <span class="corner-item">项目</span>
          </th>
          <th colspan="4">水源水</th>
          <th colspan="1">滤前水</th>
          <th colspan="10">出厂水</th>
        </tr>
        <tr>
          <th>水温℃</th>
          <th>PH值</th>
          <th>浑浊度NTU</th>
          <th>氨氮(mg/L)</th>
          <th>浑浊度NTU</th>
          <th>菌落总数CFU/ml</th>
          <th>总大肠菌群MPN/100ml</th>
          <th>大肠埃希氏菌CFU/ml</th>
          <th>浑浊度NTU</th>
          <th>PH值</th>
          <th>色度(度)</th>
          <th>臭和味</th>
          <th>肉眼可见物</th>
          <th>余氯(mg/L)</th>
          <th>高锰酸盐指数(mg/L)</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in rows" :key="row.timeBucket || row.rowKey">
          <td class="td-time">{{ row.timeBucket || '-' }}</td>
          <td>{{ cell(row, '水源水-水温℃') }}</td>
          <td>{{ cell(row, '水源水-PH值') }}</td>
          <td>{{ cell(row, '水源水-浑浊度NTU') }}</td>
          <td>{{ cell(row, '水源水-氨氮(mg/L)') }}</td>
          <td>{{ cell(row, '滤前水-浑浊度NTU') }}</td>
          <td>{{ cell(row, '出厂水-菌落总数CFU/ml') }}</td>
          <td>{{ cell(row, '出厂水-总大肠菌群MPN/100ml') }}</td>
          <td>{{ cell(row, '出厂水-大肠埃希氏菌CFU/ml') }}</td>
          <td>{{ cell(row, '出厂水-浑浊度NTU') }}</td>
          <td>{{ cell(row, '出厂水-PH值') }}</td>
          <td>{{ cell(row, '出厂水-色度(度)') }}</td>
          <td>{{ cell(row, '出厂水-臭和味') }}</td>
          <td>{{ cell(row, '出厂水-肉眼可见物') }}</td>
          <td>{{ cell(row, '出厂水-余氯(mg/L)') }}</td>
          <td>{{ cell(row, '出厂水-高锰酸盐指数(mg/L)') }}</td>
        </tr>
        <tr class="remark-row">
          <td class="remark-label">备注</td>
          <td colspan="9" class="remark-cell"></td>
          <td colspan="2" class="inspector-label">检验员</td>
          <td colspan="4" class="inspector-cell">{{ previewData?.inspectorName || '' }}</td>
        </tr>
      </tbody>
    </table>
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
const plantTitle = computed(() => `${props.previewData?.regionName || '-'}水质检验记录表`)
const rows = computed(() => props.previewData?.rows || [])

const dateParts = computed(() => {
  const date = String(props.previewData?.periodStart || '')
  const [year = '', month = '', day = ''] = date.split('-')
  return { year, month, day }
})

function cell(row, key) {
  const values = row?.parameterValues || {}
  return values[key] || ''
}
</script>

<style scoped>
.daily-template {
  padding: 8px 6px 12px;
  color: #111827;
}

.template-title,
.template-subtitle {
  text-align: center;
  font-family: SimSun, "Microsoft YaHei", sans-serif;
}

.template-title {
  font-size: 26px;
  line-height: 1.5;
  letter-spacing: 0.18em;
}

.template-subtitle {
  margin-top: 4px;
  font-size: 20px;
  line-height: 1.5;
  letter-spacing: 0.08em;
}

.meta-row {
  display: grid;
  grid-template-columns: 1.4fr 1fr 1fr;
  margin: 16px 0 8px;
  font-size: 14px;
}

.meta-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.meta-date {
  justify-content: flex-start;
  padding-left: 18px;
}

.report-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}

.col-time {
  width: 88px;
}

.report-table th,
.report-table td {
  border: 1px solid #111827;
  padding: 4px 3px;
  text-align: center;
  font-size: 12px;
  line-height: 1.25;
  word-break: break-word;
}

.report-table thead tr:first-child th {
  height: 32px;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.18em;
}

.report-table thead tr:nth-child(2) th {
  height: 54px;
  font-size: 11px;
  font-weight: 600;
}

.th-corner {
  position: relative;
  width: 88px;
  min-width: 88px;
  padding: 0;
  overflow: hidden;
}

.th-corner::before {
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

.corner-time,
.corner-item {
  position: absolute;
  z-index: 1;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.08em;
}

.corner-time {
  left: 9px;
  bottom: 16px;
}

.corner-item {
  right: 8px;
  top: 16px;
}

.td-time {
  width: 88px;
  height: 44px;
  font-size: 13px;
}

.remark-row td {
  height: 96px;
}

.remark-label,
.inspector-label {
  font-size: 14px;
  font-weight: 500;
}

.remark-cell,
.inspector-cell {
  text-align: left;
  vertical-align: top;
}

.inspector-cell {
  text-align: center;
  vertical-align: middle;
}
</style>
