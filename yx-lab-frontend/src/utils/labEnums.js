export const DEFAULT_PAGE_SIZE = 30

export const PAGE_SIZE_OPTIONS = [10, 20, 30, 50, 100, 500]

export const instrumentStatusLabelMap = {
  NORMAL: '正常',
  DISABLED: '停用',
  MAINTENANCE: '维护中',
  CALIBRATING: '待校准'
}

export const pointStatusLabelMap = {
  ENABLED: '启用',
  DISABLED: '禁用'
}

export const pointTypeLabelMap = {
  FACTORY: '出厂水',
  RAW: '原水',
  TERMINAL: '管网末梢'
}

export const frequencyTypeLabelMap = {
  DAILY: '每日',
  WEEKLY: '每周',
  MONTHLY: '每月'
}

export const cycleTypeLabelMap = {
  ONCE: '单次',
  DAILY: '每日',
  WEEKLY: '每周',
  MONTHLY: '每月'
}

export const planStatusLabelMap = {
  ACTIVE: '启用中',
  PAUSED: '已暂停',
  DISPATCHED: '已派发',
  COMPLETED: '已完成',
  PENDING: '待执行',
  UNPUBLISHED: '待派发',
  PUBLISHED: '已派发'
}

export const taskStatusLabelMap = {
  PENDING: '待处理',
  IN_PROGRESS: '进行中',
  ABANDONED: '已废弃',
  COMPLETED: '已完成'
}

export const sampleTypeLabelMap = {
  FACTORY: '出厂水',
  RAW: '原水',
  TERMINAL: '管网末梢'
}

export const sampleStatusLabelMap = {
  LOGGED: '已登录',
  REVIEWING: '审核中',
  RETEST: '待重检',
  COMPLETED: '已完成'
}

export const detectionStatusLabelMap = {
  SUBMITTED: '待审核',
  APPROVED: '已通过',
  REJECTED: '已驳回'
}

export const detectionResultLabelMap = {
  NORMAL: '正常',
  ABNORMAL: '异常'
}

export const reviewResultLabelMap = {
  APPROVED: '审核通过',
  REJECTED: '审核驳回'
}

export const reportTypeLabelMap = {
  DAILY: '日报',
  WEEKLY: '周报',
  MONTHLY: '月报'
}

export const reportStatusLabelMap = {
  DRAFT: '草稿',
  GENERATED: '已生成',
  PUBLISHED: '已发布'
}

function buildOptions(labelMap) {
  return Object.entries(labelMap).map(([value, label]) => ({ label, value }))
}

export const instrumentStatusOptions = buildOptions(instrumentStatusLabelMap)

export const pointStatusOptions = buildOptions(pointStatusLabelMap)

export const enabledPointStatus = 'ENABLED'

export const disabledPointStatus = 'DISABLED'

export const pointTypeOptions = buildOptions(pointTypeLabelMap)

export const factoryPointType = 'FACTORY'

export const rawPointType = 'RAW'

export const terminalPointType = 'TERMINAL'

export const frequencyTypeOptions = buildOptions(frequencyTypeLabelMap)

export const dailyFrequencyType = 'DAILY'

export const weeklyFrequencyType = 'WEEKLY'

export const monthlyFrequencyType = 'MONTHLY'

export const cycleTypeOptions = buildOptions(cycleTypeLabelMap)

export const onceCycleType = 'ONCE'

export const sampleTypeOptions = buildOptions(sampleTypeLabelMap)

export const factorySampleType = 'FACTORY'

export const reportTypeOptions = buildOptions(reportTypeLabelMap)

export const dailyReportType = 'DAILY'

export const weeklyReportType = 'WEEKLY'

export const monthlyReportType = 'MONTHLY'

export const reportStatusOptions = buildOptions(reportStatusLabelMap)

export const draftReportStatus = 'DRAFT'

export const generatedReportStatus = 'GENERATED'

export const publishedReportStatus = 'PUBLISHED'

export const actionablePlanStatuses = ['ACTIVE', 'UNPUBLISHED']

export const pausedPlanStatus = 'PAUSED'

export const dispatchedPlanStatuses = ['DISPATCHED', 'PUBLISHED']

export const completedPlanStatus = 'COMPLETED'

export const pendingTaskStatus = 'PENDING'

export const abandonedTaskStatus = 'ABANDONED'

export const inProgressTaskStatus = 'IN_PROGRESS'

export const completedTaskStatus = 'COMPLETED'

export const completableTaskStatuses = [pendingTaskStatus, inProgressTaskStatus]

export const loggedSampleStatus = 'LOGGED'

export const reviewingSampleStatus = 'REVIEWING'

export const retestSampleStatus = 'RETEST'

export const completedSampleStatus = 'COMPLETED'

export const reviewPendingDetectionStatus = 'SUBMITTED'

export const rejectedDetectionStatus = 'REJECTED'

export const approvedReviewResult = 'APPROVED'

export const rejectedReviewResult = 'REJECTED'

export const abnormalDetectionResult = 'ABNORMAL'

export const availableDetectionSampleStatuses = ['LOGGED', 'RETEST']

export const routineSamplingType = 'ROUTINE'

const statusClassMaps = {
  instrumentStatus: {
    NORMAL: 'success',
    DISABLED: 'danger',
    MAINTENANCE: 'warning',
    CALIBRATING: 'info'
  },
  pointStatus: {
    ENABLED: 'success',
    DISABLED: 'warning'
  },
  planStatus: {
    ACTIVE: 'success',
    PAUSED: 'warning',
    DISPATCHED: 'info',
    COMPLETED: 'success',
    PENDING: 'warning',
    UNPUBLISHED: 'warning',
    PUBLISHED: 'info'
  },
  taskStatus: {
    PENDING: 'warning',
    IN_PROGRESS: 'info',
    ABANDONED: 'danger',
    COMPLETED: 'success'
  },
  sampleStatus: {
    LOGGED: 'info',
    REVIEWING: 'warning',
    RETEST: 'danger',
    COMPLETED: 'success'
  },
  detectionStatus: {
    SUBMITTED: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger'
  },
  detectionResult: {
    NORMAL: 'success',
    ABNORMAL: 'danger'
  },
  reviewResult: {
    APPROVED: 'success',
    REJECTED: 'danger'
  },
  reportStatus: {
    DRAFT: 'info',
    GENERATED: 'warning',
    PUBLISHED: 'success'
  }
}

export function getEnumLabel(map, value) {
  return map[value] || value || '-'
}

export function getStatusClass(type, value) {
  const classMap = statusClassMaps[type] || {}
  return classMap[value] || 'info'
}
