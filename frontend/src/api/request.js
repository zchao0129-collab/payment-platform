import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getActiveToken, getActiveUserInfo } from '@/stores/user'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
})

// Request interceptor — attach role-appropriate JWT token
request.interceptors.request.use(
  (config) => {
    const token = getActiveToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// Response interceptor — unwrap Result<T> and handle errors
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 200) {
      return res.data
    }
    if (res.code === 401) {
      const userInfo = getActiveUserInfo()
      const keys = getActiveStorageKeys()
      localStorage.removeItem(keys.token)
      localStorage.removeItem(keys.refresh)
      localStorage.removeItem(keys.user)
      window.location.href = userInfo?.role === 1 ? '/admin/login' : '/login'
      return Promise.reject(new Error(res.msg || '未登录'))
    }
    ElMessage.error(res.msg || '请求失败')
    return Promise.reject(new Error(res.msg || '请求失败'))
  },
  (error) => {
    if (error.response?.status === 401) {
      const userInfo = getActiveUserInfo()
      const keys = getActiveStorageKeys()
      localStorage.removeItem(keys.token)
      localStorage.removeItem(keys.refresh)
      localStorage.removeItem(keys.user)
      window.location.href = userInfo?.role === 1 ? '/admin/login' : '/login'
    }
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

function getActiveStorageKeys() {
  const isAdmin = window.location.pathname.startsWith('/admin')
  return isAdmin
    ? { token: 'admin_access_token', refresh: 'admin_refresh_token', user: 'admin_user_info' }
    : { token: 'mch_access_token', refresh: 'mch_refresh_token', user: 'mch_user_info' }
}

export default request
