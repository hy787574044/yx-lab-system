import axios from 'axios'
import request from './http'
import { getToken } from '../utils/auth'

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
export const mobileLoginApi = (data) => request.post('/api/auth/mobile-login', data)
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
export const fetchMonitoringPointsApi = (params) => request.get('/api/monitoring-points', { params })
export const createMonitoringPointApi = (data) => request.post('/api/monitoring-points', data)

// 采样计划
export const fetchSamplingPlansApi = (params) => request.get('/api/sampling-plans', { params })
export const createSamplingPlanApi = (data) => request.post('/api/sampling-plans', data)
export const dispatchSamplingPlanApi = (data) => request.post('/api/sampling-plans/dispatch', data)
export const pauseSamplingPlanApi = (id) => request.post(`/api/sampling-plans/${id}/pause`)
export const resumeSamplingPlanApi = (id) => request.post(`/api/sampling-plans/${id}/resume`)

// 采样任务执行
/**
 * 获取采样任务分页。
 *
 * @param {Object} params 查询条件。
 * @returns {Promise<any>} 采样任务分页结果。
 */
export const fetchSamplingTasksApi = (params) => request.get('/api/sampling-tasks', { params })
/**
 * 开始采样任务。
 *
 * @param {number} id 采样任务主键。
 * @param {Object} [data={}] 操作附加信息。
 * @returns {Promise<any>} 操作结果。
 */
export const startSamplingTaskApi = (id, data = {}) => request.post(`/api/sampling-tasks/${id}/start`, data)
/**
 * 废弃采样任务。
 *
 * @param {number} id 采样任务主键。
 * @param {Object} [data={}] 废弃原因与备注。
 * @returns {Promise<any>} 操作结果。
 */
export const abandonSamplingTaskApi = (id, data = {}) => request.post(`/api/sampling-tasks/${id}/abandon`, data)
/**
 * 恢复采样任务。
 *
 * @param {number} id 采样任务主键。
 * @param {Object} [data={}] 操作附加信息。
 * @returns {Promise<any>} 操作结果。
 */
export const resumeSamplingTaskApi = (id, data = {}) => request.post(`/api/sampling-tasks/${id}/resume`, data)
/**
 * 提交采样完成信息。
 *
 * @param {Object} data 采样完成表单。
 * @returns {Promise<any>} 操作结果。
 */
export const completeSamplingTaskApi = (data) => request.post('/api/sampling-tasks/complete', data)

// 样品登录
/**
 * 获取样品分页。
 *
 * @param {Object} params 查询条件。
 * @returns {Promise<any>} 样品分页结果。
 */
export const fetchSamplesApi = (params) => request.get('/api/samples', { params })
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
export const fetchDetectionTypesApi = (params) => request.get('/api/detection-config/types', { params })
/**
 * 获取检测参数分页。
 *
 * @param {Object} params 查询条件。
 * @returns {Promise<any>} 检测参数分页结果。
 */
export const fetchDetectionParametersApi = (params) => request.get('/api/detection-config/parameters', { params })
/**
 * 获取检测记录分页。
 *
 * @param {Object} params 查询条件。
 * @returns {Promise<any>} 检测记录分页结果。
 */
export const fetchDetectionsApi = (params) => request.get('/api/detections', { params })
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
/**
 * 获取仪器详情。
 *
 * @param {number} id 仪器主键。
 * @returns {Promise<any>} 仪器详情。
 */
export const getInstrumentDetailApi = (id) => request.get(`/api/assets/instruments/${id}`)
/**
 * 下载仪器导入模板。
 *
 * @returns {Promise<import('axios').AxiosResponse<Blob>>} 模板文件响应。
 */
export const downloadInstrumentTemplateApi = () => axios.get('/api/assets/instruments/import-template', {
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
export const updateInstrumentApi = (id, data) => request.put(`/api/assets/instruments/${id}`, data)
/**
 * 删除仪器台账。
 *
 * @param {number} id 仪器主键。
 * @returns {Promise<any>} 删除结果。
 */
export const deleteInstrumentApi = (id) => request.delete(`/api/assets/instruments/${id}`)
/**
 * 获取实验室文档分页。
 *
 * @param {Object} params 查询条件。
 * @returns {Promise<any>} 文档分页结果。
 */
export const fetchDocumentsApi = (params) => request.get('/api/assets/documents', { params })
/**
 * 获取实验室文档详情。
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
export const fetchDocumentUsersApi = () => request.get('/api/assets/document-users')
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
 * 预览实验室文档文件流。
 *
 * @param {number} id 文档主键。
 * @returns {Promise<import('axios').AxiosResponse<Blob>>} 文档预览响应。
 */
export const previewDocumentApi = (id) => axios.get(`/api/assets/documents/${id}/preview`, {
  responseType: 'blob',
  headers: getToken() ? { Authorization: `Bearer ${getToken()}` } : {}
})
/**
 * 新增实验室文档。
 *
 * @param {Object} data 文档表单。
 * @returns {Promise<any>} 保存结果。
 */
export const createDocumentApi = (data) => request.post('/api/assets/documents', data)
/**
 * 更新实验室文档。
 *
 * @param {number} id 文档主键。
 * @param {Object} data 文档表单。
 * @returns {Promise<any>} 更新结果。
 */
export const updateDocumentApi = (id, data) => request.put(`/api/assets/documents/${id}`, data)
/**
 * 删除实验室文档。
 *
 * @param {number} id 文档主键。
 * @returns {Promise<any>} 删除结果。
 */
export const deleteDocumentApi = (id) => request.delete(`/api/assets/documents/${id}`)

// 统计汇总
/**
 * 获取统计汇总。
 *
 * @returns {Promise<any>} 统计汇总结果。
 */
export const statisticsApi = () => request.get('/api/statistics/summary')
