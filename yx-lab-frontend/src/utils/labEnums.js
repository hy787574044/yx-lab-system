export const instrumentStatusLabelMap = {
  NORMAL: '正常',
  DISABLED: '停用',
  MAINTENANCE: '维修中',
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

export const planStatusLabelMap = {
  PENDING: '待执行',
  DISPATCHED: '已派发',
  COMPLETED: '已完成',
  UNPUBLISHED: '未派发',
  PUBLISHED: '已派发'
}

export const taskStatusLabelMap = {
  PENDING: '待处理',
  IN_PROGRESS: '进行中',
  COMPLETED: '已完成'
}

export const sampleTypeLabelMap = {
  FACTORY: '出厂水',
  RAW: '原水',
  TERMINAL: '管网末梢'
}

export const sampleStatusLabelMap = {
  LOGGED: '已登录',
  SUBMITTED: '已登记',
  IN_TEST: '检测中',
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
    PENDING: 'warning',
    DISPATCHED: 'info',
    COMPLETED: 'success',
    UNPUBLISHED: 'warning',
    PUBLISHED: 'success'
  },
  taskStatus: {
    PENDING: 'warning',
    IN_PROGRESS: 'info',
    COMPLETED: 'success'
  },
  sampleStatus: {
    LOGGED: 'info',
    SUBMITTED: 'info',
    IN_TEST: 'warning',
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
