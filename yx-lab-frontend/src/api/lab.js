import axios from 'axios'
import request from './http'
import { getToken } from '../utils/auth'

function resolveDownloadFilename(contentDisposition, fallbackName = '数据导出.xlsx') {
  if (!contentDisposition) {
    return fallbackName
  }
  const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (utf8Match?.[1]) {
    return decodeURIComponent(utf8Match[1])
  }
  const normalMatch = contentDisposition.match(/filename="?([^";]+)"?/i)
  if (normalMatch?.[1]) {
    return decodeURIComponent(normalMatch[1])
  }
  return fallbackName
}

async function downloadExcel(url, params, fallbackName) {
  const token = getToken()
  const response = await axios.get(url, {
    baseURL: '/',
    params,
    responseType: 'blob',
    timeout: 30000,
    headers: token ? { Authorization: `Bearer ${token}` } : {}
  })
  const contentType = response.headers['content-type'] || ''
  if (contentType.includes('application/json')) {
    const text = await response.data.text()
    const payload = JSON.parse(text || '{}')
    throw new Error(payload.message || '导出失败')
  }
  const fileName = resolveDownloadFilename(response.headers['content-disposition'], fallbackName)
  const blobUrl = window.URL.createObjectURL(response.data)
  const link = document.createElement('a')
  link.href = blobUrl
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(blobUrl)
}

// 认证与首页总览
/**
 * 桌面端登录。
 *
 * @param {Object} data 登录请求体。
 * @returns {Promise<any>} 登录结果。
 */
export const loginApi = (data) => request.post('/api/auth/login', data)
/**
 * 移动端登录。
 *
 * @param {Object} data 登录请求体。
 * @returns {Promise<any>} 登录结果。
 */
export const mobileLoginApi = (data) => request.post('/api/auth/mobileLogin', data)
/**
 * 获取当前登录人信息。
 *
 * @returns {Promise<any>} 当前用户信息。
 */
export const getMeApi = () => request.get('/api/auth/me')
/**
 * 获取驾驶舱首页总览。
 *
 * @returns {Promise<any>} 驾驶舱总览数据。
 */
export const dashboardApi = () => request.get('/api/dashboard/overview')

// 移动端闭环看板接口
/**
 * 获取移动端采样待办。
 *
 * @returns {Promise<any>} 采样待办列表。
 */
export const fetchMobileSamplingTodoApi = () => request.get('/api/mobile/sampling/todo')
/**
 * 获取移动端检测待办。
 *
 * @returns {Promise<any>} 检测待办列表。
 */
export const fetchMobileDetectionTodoApi = () => request.get('/api/mobile/detection/todo')
/**
 * 获取移动端检测历史。
 *
 * @returns {Promise<any>} 检测历史列表。
 */
export const fetchMobileDetectionHistoryApi = () => request.get('/api/mobile/detection/history')
/**
 * 获取移动端审核待办。
 *
 * @returns {Promise<any>} 审核待办列表。
 */
export const fetchMobileReviewTodoApi = () => request.get('/api/mobile/review/todo')
/**
 * 获取移动端审核历史。
 *
 * @returns {Promise<any>} 审核历史列表。
 */
export const fetchMobileReviewHistoryApi = () => request.get('/api/mobile/review/history')
/**
 * 获取移动端我的报告。
 *
 * @returns {Promise<any>} 报告列表。
 */
export const fetchMobileReportsApi = () => request.get('/api/mobile/reports/mine')

// 监测点位
export const fetchMonitoringPointsApi = (params) => request.get('/api/monitoringPoints', { params })
export const exportMonitoringPointsApi = (params) => downloadExcel('/api/monitoringPoints/export', params, '监测点位.xlsx')
export const createMonitoringPointApi = (data) => request.post('/api/monitoringPoints', data)
export const updateMonitoringPointApi = (id, data) => request.post(`/api/monitoringPoints/${id}`, data)

// 采样计划
export const fetchSamplingPlansApi = (params) => request.get('/api/samplingPlans', { params })
export const exportSamplingPlansApi = (params) => downloadExcel('/api/samplingPlans/export', params, '采样计划.xlsx')
export const createSamplingPlanApi = (data) => request.post('/api/samplingPlans', data)
export const updateSamplingPlanApi = (id, data) => request.post(`/api/samplingPlans/${id}`, data)
export const dispatchSamplingPlanApi = (data) => request.post('/api/samplingPlans/dispatch', data)
export const pauseSamplingPlanApi = (id) => request.post(`/api/samplingPlans/${id}/pause`)
export const resumeSamplingPlanApi = (id) => request.post(`/api/samplingPlans/${id}/resume`)

// 采样任务执行
/**
 * 获取采样任务分页。
 *
 * @param {Object} params 查询条件。
 * @returns {Promise<any>} 采样任务分页结果。
 */
export const fetchSamplingTasksApi = (params) => request.get('/api/samplingTasks', { params })
export const exportSamplingTasksApi = (params) => downloadExcel('/api/samplingTasks/export', params, '采样任务.xlsx')
export const updateSamplingTaskSealNoApi = (id, data) => request.post(`/api/samplingTasks/${id}/sealNo`, data)
/**
 * 开始采样任务。
 *
 * @param {number} id 采样任务主键。
 * @param {Object} [data={}] 操作附加信息。
 * @returns {Promise<any>} 操作结果。
 */
export const startSamplingTaskApi = (id, data = {}) => request.post(`/api/samplingTasks/${id}/start`, data)
/**
 * 废弃采样任务。
 *
 * @param {number} id 采样任务主键。
 * @param {Object} [data={}] 废弃原因与备注。
 * @returns {Promise<any>} 操作结果。
 */
export const abandonSamplingTaskApi = (id, data = {}) => request.post(`/api/samplingTasks/${id}/abandon`, data)
/**
 * 恢复采样任务。
 *
 * @param {number} id 采样任务主键。
 * @param {Object} [data={}] 操作附加信息。
 * @returns {Promise<any>} 操作结果。
 */
export const resumeSamplingTaskApi = (id, data = {}) => request.post(`/api/samplingTasks/${id}/resume`, data)
/**
 * 提交采样完成信息。
 *
 * @param {Object} data 采样完成表单。
 * @returns {Promise<any>} 操作结果。
 */
export const completeSamplingTaskApi = (data) => request.post('/api/samplingTasks/complete', data)

// 样品登录
/**
 * 获取样品分页。
 *
 * @param {Object} params 查询条件。
 * @returns {Promise<any>} 样品分页结果。
 */
export const fetchSamplesApi = (params) => request.get('/api/samples', { params })
export const exportSamplesApi = (params) => downloadExcel('/api/samples/export', params, '样品台账.xlsx')
/**
 * 提交样品登录。
 *
 * @param {Object} data 样品登录表单。
 * @returns {Promise<any>} 登录后的样品信息。
 */
export const loginSampleApi = (data) => request.post('/api/samples/login', data)

// 检测配置与检测提交
/**
 * 获取检测类型分页。
 *
 * @param {Object} params 查询条件。
 * @returns {Promise<any>} 检测类型分页结果。
 */
export const fetchDetectionTypesApi = (params) => request.get('/api/detectionConfig/types', { params })
export const exportDetectionTypesApi = (params) => downloadExcel('/api/detectionConfig/types/export', params, '检测套餐.xlsx')
/**
 * 新增检测项目。
 *
 * @param {Object} data 检测项目表单。
 * @returns {Promise<any>} 保存结果。
 */
export const createDetectionTypeApi = (data) => request.post('/api/detectionConfig/types', data)
/**
 * 更新检测项目。
 *
 * @param {number} id 检测项目主键。
 * @param {Object} data 检测项目表单。
 * @returns {Promise<any>} 更新结果。
 */
export const updateDetectionTypeApi = (id, data) => request.post(`/api/detectionConfig/types/${id}`, data)
/**
 * 删除检测项目。
 *
 * @param {number} id 检测项目主键。
 * @returns {Promise<any>} 删除结果。
 */
export const deleteDetectionTypeApi = (id) => request.post(`/api/detectionConfig/types/${id}/delete`)
/**
 * 新增检测参数。
 *
 * @param {Object} data 检测参数表单。
 * @returns {Promise<any>} 保存结果。
 */
export const createDetectionParameterApi = (data) => request.post('/api/detectionConfig/parameters', data)
/**
 * 更新检测参数。
 *
 * @param {number} id 检测参数主键。
 * @param {Object} data 检测参数表单。
 * @returns {Promise<any>} 更新结果。
 */
export const updateDetectionParameterApi = (id, data) => request.post(`/api/detectionConfig/parameters/${id}`, data)
/**
 * 删除检测参数。
 *
 * @param {number} id 检测参数主键。
 * @returns {Promise<any>} 删除结果。
 */
export const deleteDetectionParameterApi = (id) => request.post(`/api/detectionConfig/parameters/${id}/delete`)
/**
 * 获取检测员下拉选项。
 *
 * @returns {Promise<any>} 检测员列表。
 */
export const fetchDetectionDetectorsApi = () => request.get('/api/detectionConfig/detectors')
/**
 * 获取检测项目组分页。
 *
 * @param {Object} params 查询条件。
 * @returns {Promise<any>} 检测项目组分页结果。
 */
export const fetchDetectionProjectGroupsApi = (params) => request.get('/api/detectionConfig/projectGroups', { params })
/**
 * 新增检测项目组。
 *
 * @param {Object} data 检测项目组表单。
 * @returns {Promise<any>} 保存结果。
 */
export const createDetectionProjectGroupApi = (data) => request.post('/api/detectionConfig/projectGroups', data)
/**
 * 更新检测项目组。
 *
 * @param {number} id 检测项目组主键。
 * @param {Object} data 检测项目组表单。
 * @returns {Promise<any>} 更新结果。
 */
export const updateDetectionProjectGroupApi = (id, data) => request.post(`/api/detectionConfig/projectGroups/${id}`, data)
/**
 * 删除检测项目组。
 *
 * @param {number} id 检测项目组主键。
 * @returns {Promise<any>} 删除结果。
 */
export const deleteDetectionProjectGroupApi = (id) => request.post(`/api/detectionConfig/projectGroups/${id}/delete`)
/**
 * 获取检测参数分页。
 *
 * @param {Object} params 查询条件。
 * @returns {Promise<any>} 检测参数分页结果。
 */
export const fetchDetectionParametersApi = (params) => request.get('/api/detectionConfig/parameters', { params })
export const exportDetectionParametersApi = (params) => downloadExcel('/api/detectionConfig/parameters/export', params, '检测参数.xlsx')
/**
 * 获取检测记录分页。
 *
 * @param {Object} params 查询条件。
 * @returns {Promise<any>} 检测记录分页结果。
 */
/**
 * 获取检测方法分页。
 * @param {Object} params 查询条件。
 * @returns {Promise<any>} 检测方法分页结果。
 */
export const fetchDetectionMethodsApi = (params) => request.get('/api/detectionConfig/methods', { params })
export const exportDetectionMethodsApi = (params) => downloadExcel('/api/detectionConfig/methods/export', params, '检测方法.xlsx')
/**
 * 新增检测方法。
 * @param {Object} data 检测方法表单。
 * @returns {Promise<any>} 保存结果。
 */
export const createDetectionMethodApi = (data) => request.post('/api/detectionConfig/methods', data)
/**
 * 更新检测方法。
 * @param {number} id 检测方法主键。
 * @param {Object} data 检测方法表单。
 * @returns {Promise<any>} 更新结果。
 */
export const updateDetectionMethodApi = (id, data) => request.post(`/api/detectionConfig/methods/${id}`, data)
/**
 * 删除检测方法。
 * @param {number} id 检测方法主键。
 * @returns {Promise<any>} 删除结果。
 */
export const deleteDetectionMethodApi = (id) => request.post(`/api/detectionConfig/methods/${id}/delete`)
/**
 * 获取检测参数与检测方法绑定列表。
 * @param {Object} params 查询条件。
 * @returns {Promise<any>} 绑定列表分页结果。
 */
export const fetchDetectionParameterMethodBindingsApi = (params) => request.get('/api/detectionConfig/parameterMethodBindings', { params })
/**
 * 保存单个检测参数的检测方法绑定关系。
 * @param {number} parameterId 检测参数主键。
 * @param {Object} data 绑定表单。
 * @returns {Promise<any>} 保存结果。
 */
export const saveDetectionParameterMethodBindingsApi = (parameterId, data) => request.post(`/api/detectionConfig/parameterMethodBindings/${parameterId}`, data)
/**
 * 获取检测方法下拉选项。
 * @returns {Promise<any>} 检测方法列表。
 */
export const fetchDetectionMethodOptionsApi = () => request.get('/api/detectionConfig/methods/options')
export const fetchDetectionsApi = (params) => request.get('/api/detections', { params })
export const exportDetectionsApi = (params) => downloadExcel('/api/detections/export', params, '检测流程.xlsx')
export const fetchDetectionDetailApi = (id) => request.get(`/api/detections/${id}`)
export const assignDetectionDetectorsApi = (id, data) => request.post(`/api/detections/${id}/assignDetectors`, data)
/**
 * 提交检测结果。
 *
 * @param {Object} data 检测提交表单。
 * @returns {Promise<any>} 提交结果。
 */
export const submitDetectionApi = (data) => request.post('/api/detections/submit', data)

// 审核流程
/**
 * 获取审核记录分页。
 *
 * @param {Object} params 查询条件。
 * @returns {Promise<any>} 审核记录分页结果。
 */
export const fetchReviewsApi = (params) => request.get('/api/reviews', { params })
export const exportReviewsApi = (params) => downloadExcel('/api/reviews/export', params, '结果审查.xlsx')
/**
 * 提交审核结果。
 *
 * @param {Object} data 审核表单。
 * @returns {Promise<any>} 提交结果。
 */
export const submitReviewApi = (data) => request.post('/api/reviews', data)

// 报告产物与预览
/**
 * 获取报告分页。
 *
 * @param {Object} params 查询条件。
 * @returns {Promise<any>} 报告分页结果。
 */
export const fetchReportsApi = (params) => request.get('/api/reports', { params })
export const exportReportsApi = (params) => downloadExcel('/api/reports/export', params, '报告台账.xlsx')
/**
 * 获取报告模板分页。
 *
 * @param {Object} params 查询条件。
 * @returns {Promise<any>} 模板分页结果。
 */
export const fetchTemplatesApi = (params) => request.get('/api/reports/templates', { params })
/**
 * 新增报告模板。
 *
 * @param {Object} data 模板表单。
 * @returns {Promise<any>} 保存结果。
 */
export const createTemplateApi = (data) => request.post('/api/reports/templates', data)
/**
 * 发布正式报告。
 *
 * @param {number} id 报告主键。
 * @returns {Promise<any>} 发布结果。
 */
export const publishReportApi = (id) => request.post(`/api/reports/${id}/publish`)
/**
 * 取消发布正式报告。
 *
 * @param {number} id 报告主键。
 * @returns {Promise<any>} 取消发布结果。
 */
export const unpublishReportApi = (id) => request.post(`/api/reports/${id}/unpublish`)
/**
 * 获取报告预览结构化数据。
 *
 * @param {number} id 报告主键。
 * @returns {Promise<any>} 报告预览结构化数据。
 */
export const fetchReportPreviewDataApi = (id) => request.get(`/api/reports/${id}/previewData`)
// 预览类接口返回二进制流，需要单独保留鉴权头和 blob 处理方式。
/**
 * 预览正式报告文件流。
 *
 * @param {number} id 报告主键。
 * @returns {Promise<import('axios').AxiosResponse<Blob>>} 报告预览响应。
 */
export const previewReportApi = (id) => axios.get(`/api/reports/${id}/preview`, {
  responseType: 'blob',
  headers: getToken() ? { Authorization: `Bearer ${getToken()}` } : {}
})

// 设备与文档资产
/**
 * 获取仪器台账分页。
 *
 * @param {Object} params 查询条件。
 * @returns {Promise<any>} 仪器台账分页结果。
 */
export const fetchInstrumentsApi = (params) => request.get('/api/assets/instruments', { params })
export const exportInstrumentsApi = (params) => downloadExcel('/api/assets/instruments/export', params, '设备台账.xlsx')
/**
 * 获取仪器详情。
 *
 * @param {number} id 仪器主键。
 * @returns {Promise<any>} 仪器详情。
 */
export const getInstrumentDetailApi = (id) => request.get(`/api/assets/instruments/${id}`)
/**
 * 获取设备维修分页。
 *
 * @param {Object} params 查询条件。
 * @returns {Promise<any>} 设备维修分页结果。
 */
export const fetchInstrumentMaintenancesApi = (params) => request.get('/api/assets/maintenances', { params })
export const exportInstrumentMaintenancesApi = (params) => downloadExcel('/api/assets/maintenances/export', params, '设备维修.xlsx')
/**
 * 新增设备维修记录。
 *
 * @param {Object} data 设备维修表单。
 * @returns {Promise<any>} 保存结果。
 */
export const createInstrumentMaintenanceApi = (data) => request.post('/api/assets/maintenances', data)
/**
 * 更新设备维修记录。
 *
 * @param {number} id 维修主键。
 * @param {Object} data 设备维修表单。
 * @returns {Promise<any>} 更新结果。
 */
export const updateInstrumentMaintenanceApi = (id, data) => request.post(`/api/assets/maintenances/${id}`, data)
/**
 * 删除设备维修记录。
 *
 * @param {number} id 维修主键。
 * @returns {Promise<any>} 删除结果。
 */
export const deleteInstrumentMaintenanceApi = (id) => request.post(`/api/assets/maintenances/${id}/delete`)
/**
 * 下载仪器导入模板。
 *
 * @returns {Promise<import('axios').AxiosResponse<Blob>>} 模板文件响应。
 */
export const downloadInstrumentTemplateApi = () => axios.get('/api/assets/instruments/importTemplate', {
  responseType: 'blob',
  headers: getToken() ? { Authorization: `Bearer ${getToken()}` } : {}
})
/**
 * 导入仪器台账文件。
 *
 * @param {File} file 上传文件。
 * @returns {Promise<any>} 导入结果。
 */
export const importInstrumentsApi = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/api/assets/instruments/import', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
/**
 * 新增仪器台账。
 *
 * @param {Object} data 仪器表单。
 * @returns {Promise<any>} 保存结果。
 */
export const createInstrumentApi = (data) => request.post('/api/assets/instruments', data)
/**
 * 更新仪器台账。
 *
 * @param {number} id 仪器主键。
 * @param {Object} data 仪器表单。
 * @returns {Promise<any>} 更新结果。
 */
export const updateInstrumentApi = (id, data) => request.post(`/api/assets/instruments/${id}`, data)
/**
 * 删除仪器台账。
 *
 * @param {number} id 仪器主键。
 * @returns {Promise<any>} 删除结果。
 */
export const deleteInstrumentApi = (id) => request.post(`/api/assets/instruments/${id}/delete`)
/**
 * 获取化验室文档分页。
 *
 * @param {Object} params 查询条件。
 * @returns {Promise<any>} 文档分页结果。
 */
export const fetchDocumentsApi = (params) => request.get('/api/assets/documents', { params })
export const exportDocumentsApi = (params) => downloadExcel('/api/assets/documents/export', params, '文档台账.xlsx')
/**
 * 获取化验室文档详情。
 *
 * @param {number} id 文档主键。
 * @returns {Promise<any>} 文档详情。
 */
export const getDocumentDetailApi = (id) => request.get(`/api/assets/documents/${id}`)
/**
 * 获取文档可见人员下拉选项。
 *
 * @returns {Promise<any>} 用户选项列表。
 */
export const fetchDocumentUsersApi = () => request.get('/api/assets/documentUsers')
/**
 * 获取系统用户分页。
 *
 * @param {Object} params 查询条件。
 * @returns {Promise<any>} 用户分页结果。
 */
export const fetchSystemUsersApi = (params) => request.get('/api/system/users', { params })
export const exportSystemUsersApi = (params) => downloadExcel('/api/system/users/export', params, '用户管理.xlsx')
/**
 * 获取系统用户详情。
 *
 * @param {number} id 用户主键。
 * @returns {Promise<any>} 用户详情。
 */
export const getSystemUserDetailApi = (id) => request.get(`/api/system/users/${id}`)
/**
 * 新增系统用户。
 *
 * @param {Object} data 用户表单。
 * @returns {Promise<any>} 保存结果。
 */
export const createSystemUserApi = (data) => request.post('/api/system/users', data)
/**
 * 更新系统用户。
 *
 * @param {number} id 用户主键。
 * @param {Object} data 用户表单。
 * @returns {Promise<any>} 更新结果。
 */
export const updateSystemUserApi = (id, data) => request.post(`/api/system/users/${id}`, data)
/**
 * 删除系统用户。
 *
 * @param {number} id 用户主键。
 * @returns {Promise<any>} 删除结果。
 */
export const deleteSystemUserApi = (id) => request.post(`/api/system/users/${id}/delete`)
/**
 * 获取系统角色分页。
 *
 * @param {Object} params 查询条件。
 * @returns {Promise<any>} 角色分页结果。
 */
export const fetchSystemRolesApi = (params) => request.get('/api/system/roles', { params })
export const exportSystemRolesApi = (params) => downloadExcel('/api/system/roles/export', params, '角色管理.xlsx')
/**
 * 获取系统角色详情。
 *
 * @param {number} id 角色主键。
 * @returns {Promise<any>} 角色详情。
 */
export const getSystemRoleDetailApi = (id) => request.get(`/api/system/roles/${id}`)
/**
 * 获取启用角色选项。
 *
 * @returns {Promise<any>} 角色选项列表。
 */
export const fetchSystemRoleOptionsApi = () => request.get('/api/system/roles/options')
export const fetchSystemOrgsApi = (params) => request.get('/api/system/orgs', { params })
export const exportSystemOrgsApi = (params) => downloadExcel('/api/system/orgs/export', params, '机构管理.xlsx')
export const getSystemOrgDetailApi = (id) => request.get(`/api/system/orgs/${id}`)
export const fetchSystemOrgOptionsApi = () => request.get('/api/system/orgs/options')
/**
 * 新增系统角色。
 *
 * @param {Object} data 角色表单。
 * @returns {Promise<any>} 保存结果。
 */
export const createSystemRoleApi = (data) => request.post('/api/system/roles', data)
/**
 * 更新系统角色。
 *
 * @param {number} id 角色主键。
 * @param {Object} data 角色表单。
 * @returns {Promise<any>} 更新结果。
 */
export const updateSystemRoleApi = (id, data) => request.post(`/api/system/roles/${id}`, data)
/**
 * 删除系统角色。
 *
 * @param {number} id 角色主键。
 * @returns {Promise<any>} 删除结果。
 */
export const deleteSystemRoleApi = (id) => request.post(`/api/system/roles/${id}/delete`)
export const createSystemOrgApi = (data) => request.post('/api/system/orgs', data)
export const updateSystemOrgApi = (id, data) => request.post(`/api/system/orgs/${id}`, data)
export const deleteSystemOrgApi = (id) => request.post(`/api/system/orgs/${id}/delete`)
export const fetchSystemDictsApi = (params) => request.get('/api/system/dicts', { params })
export const exportSystemDictsApi = (params) => downloadExcel('/api/system/dicts/export', params, '数据字典.xlsx')
export const getSystemDictDetailApi = (id) => request.get(`/api/system/dicts/${id}`)
export const createSystemDictApi = (data) => request.post('/api/system/dicts', data)
export const updateSystemDictApi = (id, data) => request.post(`/api/system/dicts/${id}`, data)
export const deleteSystemDictApi = (id) => request.post(`/api/system/dicts/${id}/delete`)
export const fetchSystemLogsApi = (params) => request.get('/api/system/logs', { params })
export const exportSystemLogsApi = (params) => downloadExcel('/api/system/logs/export', params, '系统日志.xlsx')
/**
 * 上传附件到统一存储。
 *
 * @param {File} file 上传文件。
 * @returns {Promise<any>} 上传结果，包含文件路径。
 */
export const uploadStorageFileApi = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/api/storage/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
/**
 * 预览化验室文档文件流。
 *
 * @param {number} id 文档主键。
 * @returns {Promise<import('axios').AxiosResponse<Blob>>} 文档预览响应。
 */
export const previewDocumentApi = (id) => axios.get(`/api/assets/documents/${id}/preview`, {
  responseType: 'blob',
  headers: getToken() ? { Authorization: `Bearer ${getToken()}` } : {}
})
/**
 * 新增化验室文档。
 *
 * @param {Object} data 文档表单。
 * @returns {Promise<any>} 保存结果。
 */
export const createDocumentApi = (data) => request.post('/api/assets/documents', data)
/**
 * 更新化验室文档。
 *
 * @param {number} id 文档主键。
 * @param {Object} data 文档表单。
 * @returns {Promise<any>} 更新结果。
 */
export const updateDocumentApi = (id, data) => request.post(`/api/assets/documents/${id}`, data)
/**
 * 删除化验室文档。
 *
 * @param {number} id 文档主键。
 * @returns {Promise<any>} 删除结果。
 */
export const deleteDocumentApi = (id) => request.post(`/api/assets/documents/${id}/delete`)

// 统计汇总
/**
 * 获取统计汇总。
 *
 * @returns {Promise<any>} 统计汇总结果。
 */
export const statisticsApi = () => request.get('/api/statistics/summary')
