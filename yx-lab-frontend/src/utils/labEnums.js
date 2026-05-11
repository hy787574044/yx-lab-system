export const DEFAULT_PAGE_SIZE = 30

export const PAGE_SIZE_OPTIONS = [10, 20, 30, 50, 100, 500]

export const instrumentNormalStatus = 'NORMAL'
export const instrumentDisabledStatus = 'DISABLED'
export const instrumentMaintenanceStatus = 'MAINTENANCE'
export const instrumentCalibratingStatus = 'CALIBRATING'

export const enabledPointStatus = 'ENABLED'
export const disabledPointStatus = 'DISABLED'

export const factoryPointType = 'FACTORY'
export const rawPointType = 'RAW'
export const terminalPointType = 'TERMINAL'

export const dailyFrequencyType = 'DAILY'
export const weeklyFrequencyType = 'WEEKLY'
export const monthlyFrequencyType = 'MONTHLY'

export const onceCycleType = 'ONCE'
export const dailyCycleType = 'DAILY'
export const weeklyCycleType = 'WEEKLY'
export const monthlyCycleType = 'MONTHLY'

export const activePlanStatus = 'ACTIVE'
export const pausedPlanStatus = 'PAUSED'
export const dispatchedPlanStatus = 'DISPATCHED'
export const completedPlanStatus = 'COMPLETED'
export const unpublishedPlanStatus = 'UNPUBLISHED'

export const pendingTaskStatus = 'PENDING'
export const inProgressTaskStatus = 'IN_PROGRESS'
export const abandonedTaskStatus = 'ABANDONED'
export const completedTaskStatus = 'COMPLETED'
export const unregisteredSampleRegisterStatus = 'UNREGISTERED'
export const registeredSampleRegisterStatus = 'REGISTERED'

export const factorySampleType = 'FACTORY'
export const rawSampleType = 'RAW'
export const terminalSampleType = 'TERMINAL'

export const routineSamplingType = 'ROUTINE'

export const loggedSampleStatus = 'LOGGED'
export const reviewingSampleStatus = 'REVIEWING'
export const retestSampleStatus = 'RETEST'
export const completedSampleStatus = 'COMPLETED'

export const waitAssignDetectionStatus = 'WAIT_ASSIGN'
export const waitDetectDetectionStatus = 'WAIT_DETECT'
export const reviewPendingDetectionStatus = 'SUBMITTED'
export const approvedDetectionStatus = 'APPROVED'
export const rejectedDetectionStatus = 'REJECTED'

export const normalDetectionResult = 'NORMAL'
export const abnormalDetectionResult = 'ABNORMAL'

export const approvedReviewResult = 'APPROVED'
export const rejectedReviewResult = 'REJECTED'

export const dailyReportType = 'DAILY'
export const weeklyReportType = 'WEEKLY'
export const monthlyReportType = 'MONTHLY'

export const draftReportStatus = 'DRAFT'
export const generatedReportStatus = 'GENERATED'
export const publishedReportStatus = 'PUBLISHED'

export const pendingPushStatus = 'PENDING'
export const successPushStatus = 'SUCCESS'
export const cancelledPushStatus = 'CANCELLED'
export const failedPushStatus = 'FAILED'

export const instrumentStatusLabelMap = {
  [instrumentNormalStatus]: '正常',
  [instrumentDisabledStatus]: '停用',
  [instrumentMaintenanceStatus]: '维护中',
  [instrumentCalibratingStatus]: '校准中'
}

export const pointStatusLabelMap = {
  [enabledPointStatus]: '启用',
  [disabledPointStatus]: '禁用'
}

export const pointTypeLabelMap = {
  [factoryPointType]: '出厂水',
  [rawPointType]: '原水',
  [terminalPointType]: '管网末梢'
}

export const frequencyTypeLabelMap = {
  [dailyFrequencyType]: '每日',
  [weeklyFrequencyType]: '每周',
  [monthlyFrequencyType]: '每月'
}

export const cycleTypeLabelMap = {
  [onceCycleType]: '单次',
  [dailyCycleType]: '每日',
  [weeklyCycleType]: '每周',
  [monthlyCycleType]: '每月'
}

export const planStatusLabelMap = {
  [activePlanStatus]: '启用中',
  [pausedPlanStatus]: '已暂停',
  [dispatchedPlanStatus]: '已派发',
  [completedPlanStatus]: '已完成',
  [unpublishedPlanStatus]: '待派发'
}

export const taskStatusLabelMap = {
  [pendingTaskStatus]: '待处理',
  [inProgressTaskStatus]: '进行中',
  [abandonedTaskStatus]: '已废弃',
  [completedTaskStatus]: '已完成'
}

export const sampleRegisterStatusLabelMap = {
  [unregisteredSampleRegisterStatus]: '未登记',
  [registeredSampleRegisterStatus]: '已登记'
}

export const sampleTypeLabelMap = {
  [factorySampleType]: '出厂水',
  [rawSampleType]: '原水',
  [terminalSampleType]: '管网末梢'
}

export const sampleStatusLabelMap = {
  [loggedSampleStatus]: '已登录',
  [reviewingSampleStatus]: '审核中',
  [retestSampleStatus]: '待重检',
  [completedSampleStatus]: '已完成'
}

export const detectionStatusLabelMap = {
  [waitAssignDetectionStatus]: '待分配',
  [waitDetectDetectionStatus]: '待检测',
  [reviewPendingDetectionStatus]: '待审核',
  [approvedDetectionStatus]: '已通过',
  [rejectedDetectionStatus]: '已驳回'
}

export const detectionResultLabelMap = {
  [normalDetectionResult]: '正常',
  [abnormalDetectionResult]: '异常'
}

export const reviewResultLabelMap = {
  [approvedReviewResult]: '审核通过',
  [rejectedReviewResult]: '审核驳回'
}

export const reportTypeLabelMap = {
  [dailyReportType]: '日报',
  [weeklyReportType]: '周报',
  [monthlyReportType]: '月报'
}

export const reportStatusLabelMap = {
  [draftReportStatus]: '草稿',
  [generatedReportStatus]: '已生成',
  [publishedReportStatus]: '已发布'
}

export const pushStatusLabelMap = {
  [pendingPushStatus]: '待推送',
  [successPushStatus]: '已推送',
  [cancelledPushStatus]: '已撤回',
  [failedPushStatus]: '推送失败'
}

function buildOptions(labelMap) {
  return Object.entries(labelMap).map(([value, label]) => ({ label, value }))
}

export const instrumentStatusOptions = buildOptions(instrumentStatusLabelMap)
export const pointStatusOptions = buildOptions(pointStatusLabelMap)
export const pointTypeOptions = buildOptions(pointTypeLabelMap)
export const frequencyTypeOptions = buildOptions(frequencyTypeLabelMap)
export const cycleTypeOptions = buildOptions(cycleTypeLabelMap)
export const sampleTypeOptions = buildOptions(sampleTypeLabelMap)
export const reportTypeOptions = buildOptions(reportTypeLabelMap)
export const reportStatusOptions = buildOptions(reportStatusLabelMap)

export const actionablePlanStatuses = [activePlanStatus, unpublishedPlanStatus]
export const dispatchedPlanStatuses = [dispatchedPlanStatus]
export const abandonableTaskStatuses = [pendingTaskStatus, inProgressTaskStatus]
export const completableTaskStatuses = [inProgressTaskStatus]
export const availableDetectionSampleStatuses = [loggedSampleStatus, retestSampleStatus]
export const inactiveInstrumentStatuses = [instrumentDisabledStatus, instrumentMaintenanceStatus]

const statusClassMaps = {
  instrumentStatus: {
    [instrumentNormalStatus]: 'success',
    [instrumentDisabledStatus]: 'danger',
    [instrumentMaintenanceStatus]: 'warning',
    [instrumentCalibratingStatus]: 'info'
  },
  pointStatus: {
    [enabledPointStatus]: 'success',
    [disabledPointStatus]: 'warning'
  },
  planStatus: {
    [activePlanStatus]: 'success',
    [pausedPlanStatus]: 'warning',
    [dispatchedPlanStatus]: 'info',
    [completedPlanStatus]: 'success',
    [unpublishedPlanStatus]: 'warning'
  },
  taskStatus: {
    [pendingTaskStatus]: 'warning',
    [inProgressTaskStatus]: 'info',
    [abandonedTaskStatus]: 'danger',
    [completedTaskStatus]: 'success'
  },
  sampleStatus: {
    [loggedSampleStatus]: 'info',
    [reviewingSampleStatus]: 'warning',
    [retestSampleStatus]: 'danger',
    [completedSampleStatus]: 'success'
  },
  detectionStatus: {
    [waitAssignDetectionStatus]: 'warning',
    [waitDetectDetectionStatus]: 'info',
    [reviewPendingDetectionStatus]: 'warning',
    [approvedDetectionStatus]: 'success',
    [rejectedDetectionStatus]: 'danger'
  },
  detectionResult: {
    [normalDetectionResult]: 'success',
    [abnormalDetectionResult]: 'danger'
  },
  reviewResult: {
    [approvedReviewResult]: 'success',
    [rejectedReviewResult]: 'danger'
  },
  reportStatus: {
    [draftReportStatus]: 'info',
    [generatedReportStatus]: 'warning',
    [publishedReportStatus]: 'success'
  },
  pushStatus: {
    [pendingPushStatus]: 'warning',
    [successPushStatus]: 'success',
    [cancelledPushStatus]: 'info',
    [failedPushStatus]: 'danger'
  }
}

export function getEnumLabel(map, value) {
  return map[value] || value || '-'
}

export function getStatusClass(type, value) {
  const classMap = statusClassMaps[type] || {}
  return classMap[value] || 'info'
}

const workflowTextLabelMap = {
  FACTORY: '出厂水',
  RAW: '原水',
  TERMINAL: '管网末梢',
  ENABLED: '启用',
  DISABLED: '停用',
  ACTIVE: '启用中',
  PAUSED: '已暂停',
  DISPATCHED: '已派发',
  UNPUBLISHED: '待派发',
  PENDING: '待处理',
  IN_PROGRESS: '进行中',
  ABANDONED: '已废弃',
  UNREGISTERED: '未登记',
  REGISTERED: '已登记',
  LOGGED: '已登录',
  REVIEWING: '审核中',
  RETEST: '待重检',
  COMPLETED: '已完成',
  WAIT_ASSIGN: '待分配',
  WAIT_DETECT: '待检测',
  SUBMITTED: '待审核',
  APPROVED: '审核通过',
  REJECTED: '审核驳回',
  NORMAL: '正常',
  ABNORMAL: '异常',
  DRAFT: '草稿',
  GENERATED: '已生成',
  PUBLISHED: '已发布',
  SUCCESS: '已推送',
  FAILED: '推送失败',
  CANCELLED: '已撤回'
}

function escapeRegExp(value) {
  return String(value).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

export function translateWorkflowText(value) {
  const text = String(value ?? '')
  if (!text.trim()) {
    return ''
  }
  return Object.entries(workflowTextLabelMap)
    .sort((left, right) => right[0].length - left[0].length)
    .reduce((result, [code, label]) => {
      const pattern = new RegExp(`\\b${escapeRegExp(code)}\\b`, 'g')
      return result.replace(pattern, label)
    }, text)
}
