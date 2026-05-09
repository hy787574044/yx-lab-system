import axios from 'axios'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import router from '../router'
import { clearToken, getToken } from '../utils/auth'

const request = axios.create({
  baseURL: '/',
  timeout: 15000
})

function shouldRedirectToLogin(message) {
  if (!message) {
    return false
  }
  return message.includes('请先登录')
    || message.includes('登录已失效')
    || message.includes('登录信息已失效')
    || message.includes('请重新登录')
}

request.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const payload = response.data
    if (payload.code !== 0) {
      ElMessage.error(payload.message || '请求失败')
      if (shouldRedirectToLogin(payload.message)) {
        clearToken()
        router.push('/login')
      }
      return Promise.reject(payload)
    }
    return payload.data
  },
  (error) => {
    if (error.response?.status === 401) {
      clearToken()
      router.push('/login')
    }
    ElMessage.error(error.message || '网络异常')
    return Promise.reject(error)
  }
)

export default request
