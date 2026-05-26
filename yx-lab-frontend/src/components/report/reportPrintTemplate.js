export const REPORT_TEMPLATE = {
  title: '水质检测报告',
  reportNoPrefix: 'BG',
  organizationName: '云河水质化验室',
  judgmentBasis: 'GB 5749-2022《生活饮用水卫生标准》',
  notes: [
    '本报告基于 GB/T 5750-2023 系列标准检测，判定依据为 GB 5749-2022。',
    '本报告经编制人、审核人、批准人签字，并加盖化验室印章后生效。',
    '未经本化验室书面批准，不得部分复制本报告。',
    '免责声明：本报告仅对本次送检样品及报告所列检测项目的检测结果负责。',
    '对本报告如有异议，请于收到报告之日起 7 日内向本化验室提出。'
  ]
}

export const FIRST_PAGE_ITEM_COUNT = 9

export const OTHER_PAGE_ITEM_COUNT = 18

export function normalizeDisplay(value, fallback = '-') {
  const text = String(value ?? '').trim()
  return text || fallback
}

export function toDateText(value) {
  const text = normalizeDisplay(value, '')
  return text ? text.slice(0, 10) : '-'
}

export function buildReportNo(previewData) {
  const existing = normalizeDisplay(previewData?.reportNo || previewData?.reportCode, '')
  if (existing) {
    return existing
  }
  const dateText = toDateText(previewData?.generatedTime).replaceAll('-', '')
  const idText = String(previewData?.reportId || '001').padStart(3, '0')
  return `${REPORT_TEMPLATE.reportNoPrefix}-${dateText || '00000000'}-${idText}`
}

export function buildOverallEvaluation(previewData) {
  const abnormalItems = (previewData?.items || [])
    .filter((item) => normalizeDisplay(item?.judgmentLabel, '').includes('异常'))
    .map((item) => normalizeDisplay(item?.parameterName, '未知参数'))
  if (!abnormalItems.length) {
    return '本次检测项目均符合标准要求。'
  }
  return `存在 ${abnormalItems.length} 项不合格指标（${abnormalItems.join('、')}）`
}
