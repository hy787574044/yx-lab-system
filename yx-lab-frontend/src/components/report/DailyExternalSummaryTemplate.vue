<template>
  <div class="daily-template template-external">
    <div class="template-title">{{ reportUnit }}水质日报表</div>

    <div class="meta-row meta-row--top">
      <div class="meta-cell meta-left">报告单位：{{ reportUnit }}</div>
      <div class="meta-cell meta-right">采样日期：{{ sampleDateLabel }}</div>
    </div>

    <table class="report-table">
      <thead>
        <tr>
          <th rowspan="2" class="th-sample">水样</th>
          <th rowspan="2" class="th-point">采样地点</th>
          <th colspan="5">检测项目</th>
        </tr>
        <tr>
          <th>浑浊度(NTU)</th>
          <th>色度(度)</th>
          <th>臭和味</th>
          <th>肉眼可见物</th>
          <th>游离余氯(mg/L)</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in rows" :key="row.rowKey">
          <td>{{ row.sampleTypeLabel || '-' }}</td>
          <td>{{ row.pointName || '-' }}</td>
          <td>{{ cell(row, '浑浊度(NTU)') }}</td>
          <td>{{ cell(row, '色度(度)') }}</td>
          <td>{{ cell(row, '臭和味') }}</td>
          <td>{{ cell(row, '肉眼可见物') }}</td>
          <td>{{ cell(row, '游离余氯(mg/L)') }}</td>
        </tr>
      </tbody>
    </table>

    <div class="meta-row meta-row--bottom">
      <div class="meta-cell meta-left">报告时间：{{ sampleDateLabel }}</div>
      <div class="meta-cell">负责人：{{ previewData?.principalName || '-' }}</div>
      <div class="meta-cell meta-right">报告人：{{ previewData?.reporterName || '-' }}</div>
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

const sampleDateLabel = computed(() => {
  const value = String(props.previewData?.periodStart || '')
  if (!value) {
    return '-'
  }
  const [year = '', month = '', day = ''] = value.split('-')
  return `${year}年${Number(month || 0)}月${Number(day || 0)}日`
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

.template-title {
  margin-bottom: 18px;
  text-align: center;
  font-family: SimSun, "Microsoft YaHei", sans-serif;
  font-size: 28px;
  line-height: 1.6;
}

.meta-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 12px;
  font-size: 15px;
}

.meta-row--bottom {
  grid-template-columns: 1fr 1fr 1fr;
  margin-top: 14px;
  margin-bottom: 0;
}

.meta-cell {
  display: flex;
  align-items: center;
}

.meta-left {
  justify-content: flex-start;
}

.meta-right {
  justify-content: flex-end;
}

.report-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}

.report-table th,
.report-table td {
  border: 1px solid #111827;
  padding: 10px 6px;
  text-align: center;
  font-size: 13px;
  line-height: 1.5;
  word-break: break-word;
}

.th-sample {
  width: 120px;
}

.th-point {
  width: 180px;
}
</style>
