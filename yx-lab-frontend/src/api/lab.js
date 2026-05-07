import axios from 'axios'
import request from './http'
import { getToken } from '../utils/auth'

// 认证与首页总览
export const loginApi = (data) => request.post('/api/auth/login', data)
export const mobileLoginApi = (data) => request.post('/api/auth/mobile-login', data)
export const getMeApi = () => request.get('/api/auth/me')
export const dashboardApi = () => request.get('/api/dashboard/overview')

// 移动端闭环看板接口
export const fetchMobileSamplingTodoApi = () => request.get('/api/mobile/sampling/todo')
export const fetchMobileDetectionTodoApi = () => request.get('/api/mobile/detection/todo')
export const fetchMobileDetectionHistoryApi = () => request.get('/api/mobile/detection/history')
export const fetchMobileReviewTodoApi = () => request.get('/api/mobile/review/todo')
export const fetchMobileReviewHistoryApi = () => request.get('/api/mobile/review/history')
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
export const fetchSamplingTasksApi = (params) => request.get('/api/sampling-tasks', { params })
export const startSamplingTaskApi = (id, data = {}) => request.post(`/api/sampling-tasks/${id}/start`, data)
export const abandonSamplingTaskApi = (id, data = {}) => request.post(`/api/sampling-tasks/${id}/abandon`, data)
export const resumeSamplingTaskApi = (id, data = {}) => request.post(`/api/sampling-tasks/${id}/resume`, data)
export const completeSamplingTaskApi = (data) => request.post('/api/sampling-tasks/complete', data)

// 样品登录
export const fetchSamplesApi = (params) => request.get('/api/samples', { params })
export const loginSampleApi = (data) => request.post('/api/samples/login', data)

// 检测配置与检测提交
export const fetchDetectionTypesApi = (params) => request.get('/api/detection-config/types', { params })
export const fetchDetectionParametersApi = (params) => request.get('/api/detection-config/parameters', { params })
export const fetchDetectionsApi = (params) => request.get('/api/detections', { params })
export const submitDetectionApi = (data) => request.post('/api/detections/submit', data)

// 审核流程
export const fetchReviewsApi = (params) => request.get('/api/reviews', { params })
export const submitReviewApi = (data) => request.post('/api/reviews', data)

// 报告产物与预览
export const fetchReportsApi = (params) => request.get('/api/reports', { params })
export const fetchTemplatesApi = (params) => request.get('/api/reports/templates', { params })
export const createTemplateApi = (data) => request.post('/api/reports/templates', data)
export const publishReportApi = (id) => request.post(`/api/reports/${id}/publish`)
export const unpublishReportApi = (id) => request.post(`/api/reports/${id}/unpublish`)
// 预览类接口返回二进制流，需要单独保留鉴权头和 blob 处理方式。
export const previewReportApi = (id) => axios.get(`/api/reports/${id}/preview`, {
  responseType: 'blob',
  headers: getToken() ? { Authorization: `Bearer ${getToken()}` } : {}
})

// 设备与文档资产
export const fetchInstrumentsApi = (params) => request.get('/api/assets/instruments', { params })
export const getInstrumentDetailApi = (id) => request.get(`/api/assets/instruments/${id}`)
export const downloadInstrumentTemplateApi = () => axios.get('/api/assets/instruments/import-template', {
  responseType: 'blob',
  headers: getToken() ? { Authorization: `Bearer ${getToken()}` } : {}
})
export const importInstrumentsApi = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/api/assets/instruments/import', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
export const createInstrumentApi = (data) => request.post('/api/assets/instruments', data)
export const updateInstrumentApi = (id, data) => request.put(`/api/assets/instruments/${id}`, data)
export const deleteInstrumentApi = (id) => request.delete(`/api/assets/instruments/${id}`)
export const fetchDocumentsApi = (params) => request.get('/api/assets/documents', { params })
export const getDocumentDetailApi = (id) => request.get(`/api/assets/documents/${id}`)
export const fetchDocumentUsersApi = () => request.get('/api/assets/document-users')
export const uploadStorageFileApi = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/api/storage/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
export const previewDocumentApi = (id) => axios.get(`/api/assets/documents/${id}/preview`, {
  responseType: 'blob',
  headers: getToken() ? { Authorization: `Bearer ${getToken()}` } : {}
})
export const createDocumentApi = (data) => request.post('/api/assets/documents', data)
export const updateDocumentApi = (id, data) => request.put(`/api/assets/documents/${id}`, data)
export const deleteDocumentApi = (id) => request.delete(`/api/assets/documents/${id}`)

// 统计汇总
export const statisticsApi = () => request.get('/api/statistics/summary')
