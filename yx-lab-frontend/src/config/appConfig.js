const DEV_API_BASE_URL = '/'
const PROD_API_BASE_URL = 'https://yangxin.yunhexx.com:8443'

export const API_BASE_URL = import.meta.env.MODE === 'production' ? PROD_API_BASE_URL : DEV_API_BASE_URL

export function getPublicFileUrl(path) {
  const value = String(path || '').trim()
  if (!value) {
    return ''
  }
  if (/^https?:\/\//i.test(value) || value.startsWith('blob:') || value.startsWith('data:')) {
    return value
  }
  if (value.startsWith('/api/storage/file?path=')) {
    return API_BASE_URL === '/'
      ? value
      : `${API_BASE_URL.replace(/\/$/, '')}${value}`
  }
  const normalizedPath = value.startsWith('/') ? value.slice(1) : value
  return API_BASE_URL === '/'
    ? `/api/storage/file?path=${encodeURIComponent(normalizedPath)}`
    : `${API_BASE_URL.replace(/\/$/, '')}/api/storage/file?path=${encodeURIComponent(normalizedPath)}`
}

export function buildApiUrl(path) {
  const value = String(path || '').trim()
  if (!value) {
    return ''
  }
  if (/^https?:\/\//i.test(value) || value.startsWith('blob:') || value.startsWith('data:')) {
    return value
  }
  if (API_BASE_URL === '/') {
    return value.startsWith('/') ? value : `/${value}`
  }
  const origin = API_BASE_URL.replace(/\/$/, '')
  const normalizedPath = value.startsWith('/') ? value : `/${value}`
  return `${origin}${normalizedPath}`
}
