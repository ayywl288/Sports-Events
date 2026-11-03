import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'
import type { Result } from '@/types/auth'

// 创建 axios 实例
const service: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  (error) => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse<Result>) => {
    // 二进制响应（文件下载）直接返回，不做业务码校验
    if (response.config?.responseType === 'blob') {
      return response.data as any
    }

    const res = response.data

    // 业务错误
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')

      // 401 未登录
      if (res.code === 401) {
        const userStore = useUserStore()
        userStore.logout()
        router.push('/login')
      }

      return Promise.reject(new Error(res.message || '请求失败'))
    }

    return response.data
  },
  (error) => {
    console.error('响应错误:', error)

    // 网络错误
    if (error.response) {
      const status = error.response.status
      switch (status) {
        case 401:
          ElMessage.error('登录已过期，请重新登录')
          const userStore = useUserStore()
          userStore.logout()
          router.push('/login')
          break
        case 403:
          ElMessage.error('没有权限访问')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          ElMessage.error('服务器内部错误')
          break
        default:
          ElMessage.error(error.response.data?.message || '请求失败')
      }
    } else {
      ElMessage.error('网络连接失败，请检查网络')
    }

    return Promise.reject(error)
  }
)

// 封装请求方法
export function get<T = any>(url: string, params?: any, config?: AxiosRequestConfig): Promise<Result<T>> {
  return service.get(url, { params, ...config })
}

export function post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<Result<T>> {
  return service.post(url, data, config)
}

export function put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<Result<T>> {
  return service.put(url, data, config)
}

export function del<T = any>(url: string, config?: AxiosRequestConfig): Promise<Result<T>> {
  return service.delete(url, config)
}

/**
 * 带登录态的二进制下载（导出 Excel 等）
 */
export async function download(url: string, params?: any, filename = 'download.xlsx'): Promise<void> {
  const response = await service.get(url, { params, responseType: 'blob' })
  // 后端异常时会返回 JSON，需要单独处理
  const blob = response as unknown as Blob
  if (blob.type && blob.type.includes('application/json')) {
    const text = await blob.text()
    try {
      const json = JSON.parse(text)
      ElMessage.error(json.message || '下载失败')
    } catch {
      ElMessage.error('下载失败')
    }
    return
  }

  const link = document.createElement('a')
  const href = URL.createObjectURL(blob)
  link.href = href
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(href)
}

/**
 * 文件上传（multipart/form-data）
 */
export function upload<T = any>(url: string, formData: FormData): Promise<Result<T>> {
  return service.post(url, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export default service