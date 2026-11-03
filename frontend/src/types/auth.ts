// 登录请求
export interface LoginDTO {
  username: string
  password: string
}

// 注册请求
export interface RegisterDTO {
  username: string
  password: string
  confirmPassword: string
  realName: string
  studentNo?: string
  college?: string
  className?: string
  phone?: string
  email?: string
  gender?: string
}

// 登录响应
export interface LoginVO {
  token: string
  tokenType: string
  expiresIn: number
  userId: number
  username: string
  realName: string
  role: string
  roleName: string
  avatar?: string
  studentNo?: string
  className?: string
  department?: string
  phone?: string
  email?: string
  gender?: number
  status?: number
  createTime?: string
}

// 通用响应
export interface Result<T = any> {
  code: number
  message: string
  data: T
  timestamp: number
}

// 分页结果
export interface PageResult<T = any> {
  current: number
  size: number
  total: number
  pages: number
  records: T[]
}