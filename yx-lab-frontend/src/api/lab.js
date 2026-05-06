import request from './http'

export const loginApi = (data) => request.post('/api/auth/login', data)
export const getMeApi = () => request.get('/api/auth/me')
export const dashboardApi = () => request.get('/api/dashboard/overview')

export const fetchMonitoringPointsApi = (params) => request.get('/api/monitoring-points', { params })
export const createMonitoringPointApi = (data) => request.post('/api/monitoring-points', data)

export const fetchSamplingPlansApi = (params) => request.get('/api/sampling-plans', { params })
export const createSamplingPlanApi = (data) => request.post('/api/sampling-plans', data)
export const dispatchSamplingPlanApi = (data) => request.post('/api/sampling-plans/dispatch', data)
export const fetchSamplingTasksApi = (params) => request.get('/api/sampling-tasks', { params })
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

export const fetchInstrumentsApi = (params) => request.get('/api/assets/instruments', { params })
export const createInstrumentApi = (data) => request.post('/api/assets/instruments', data)
export const fetchDocumentsApi = (params) => request.get('/api/assets/documents', { params })
export const createDocumentApi = (data) => request.post('/api/assets/documents', data)

export const statisticsApi = () => request.get('/api/statistics/summary')
