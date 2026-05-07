import axios from 'axios'
import request from './http'
import { getToken } from '../utils/auth'

export const loginApi = (data) => request.post('/api/auth/login', data)
export const getMeApi = () => request.get('/api/auth/me')
export const dashboardApi = () => request.get('/api/dashboard/overview')

export const fetchMonitoringPointsApi = (params) => request.get('/api/monitoring-points', { params })
export const createMonitoringPointApi = (data) => request.post('/api/monitoring-points', data)

export const fetchSamplingPlansApi = (params) => request.get('/api/sampling-plans', { params })
export const createSamplingPlanApi = (data) => request.post('/api/sampling-plans', data)
export const dispatchSamplingPlanApi = (data) => request.post('/api/sampling-plans/dispatch', data)
export const pauseSamplingPlanApi = (id) => request.post(`/api/sampling-plans/${id}/pause`)
export const resumeSamplingPlanApi = (id) => request.post(`/api/sampling-plans/${id}/resume`)

export const fetchSamplingTasksApi = (params) => request.get('/api/sampling-tasks', { params })
export const startSamplingTaskApi = (id, data = {}) => request.post(`/api/sampling-tasks/${id}/start`, data)
export const abandonSamplingTaskApi = (id, data = {}) => request.post(`/api/sampling-tasks/${id}/abandon`, data)
export const resumeSamplingTaskApi = (id, data = {}) => request.post(`/api/sampling-tasks/${id}/resume`, data)
export const completeSamplingTaskApi = (data) => request.post('/api/sampling-tasks/complete', data)

export const fetchSamplesApi = (params) => request.get('/api/samples', { params })
export const loginSampleApi = (data) => request.post('/api/samples/login', data)

export const fetchDetectionTypesApi = (params) => request.get('/api/detection-config/types', { params })
export const fetchDetectionParametersApi = (params) => request.get('/api/detection-config/parameters', { params })
export const fetchDetectionsApi = (params) => request.get('/api/detections', { params })
export const submitDetectionApi = (data) => request.post('/api/detections/submit', data)

export const fetchReviewsApi = (params) => request.get('/api/reviews', { params })
export const submitReviewApi = (data) => request.post('/api/reviews', data)

export const fetchReportsApi = (params) => request.get('/api/reports', { params })
export const fetchTemplatesApi = (params) => request.get('/api/reports/templates', { params })
export const createTemplateApi = (data) => request.post('/api/reports/templates', data)
export const publishReportApi = (id) => request.post(`/api/reports/${id}/publish`)
export const unpublishReportApi = (id) => request.post(`/api/reports/${id}/unpublish`)

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

export const statisticsApi = () => request.get('/api/statistics/summary')
