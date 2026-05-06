import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'
import { clearToken, getToken } from '../utils/auth'

const request = axios.create({
  baseURL: '/',
  timeout: 15000
})

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
      if (payload.message?.includes('登录')) {
        clearToken()
        router.push('/login')
      }
      return Promise.reject(payload)
    }
    return payload.data
  },
  (error) => {
    ElMessage.error(error.message || '网络异常')
    return Promise.reject(error)
  }
)

export default request
