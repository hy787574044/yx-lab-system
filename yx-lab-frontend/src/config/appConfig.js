const DEV_API_BASE_URL = '/'
const PROD_API_BASE_URL = 'https://yangxin.yunhexx.com:8443'

export const API_BASE_URL = import.meta.env.MODE === 'production' ? PROD_API_BASE_URL : DEV_API_BASE_URL

function buildStorageFileUrl(filePath) {
  const normalizedPath = String(filePath || '').trim().replace(/^\/+/, '')
  if (!normalizedPath) {
    return ''
  }
  const requestPath = `/api/storage/file?path=${encodeURIComponent(normalizedPath)}`
  return API_BASE_URL === '/'
    ? requestPath
    : `${API_BASE_URL.replace(/\/$/, '')}${requestPath}`
}

function extractStorageFilePath(value) {
  if (value.startsWith('/api/storage/file?path=') || value.startsWith('api/storage/file?path=')) {
    const queryText = value.slice(value.indexOf('?') + 1)
    return new URLSearchParams(queryText).get('path') || ''
  }
  if (/^https?:\/\//i.test(value)) {
    try {
      const url = new URL(value)
      if (url.pathname === '/api/storage/file') {
        return url.searchParams.get('path') || ''
      }
    } catch {
      return ''
    }
  }
  return ''
}

export function getPublicFileUrl(path) {
  const value = String(path || '').trim()
  if (!value) {
    return ''
  }
  if (value.startsWith('blob:') || value.startsWith('data:')) {
    return value
  }
  const storageFilePath = extractStorageFilePath(value)
  if (storageFilePath) {
    return buildStorageFileUrl(storageFilePath)
  }
  if (/^https?:\/\//i.test(value)) {
    return value
  }
  return buildStorageFileUrl(value)
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
