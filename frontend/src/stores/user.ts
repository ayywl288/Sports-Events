import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login, getUserInfo, register } from '@/api/auth'
import type { LoginDTO, RegisterDTO, LoginVO } from '@/types/auth'

const TOKEN_KEY = 'token'
const USER_INFO_KEY = 'userInfo'

function readCachedUser(): LoginVO | null {
  try {
    const raw = localStorage.getItem(USER_INFO_KEY)
    return raw ? (JSON.parse(raw) as LoginVO) : null
  } catch {
    return null
  }
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem(TOKEN_KEY) || '')
  const userInfo = ref<LoginVO | null>(readCachedUser())

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value?.role === 'ADMIN')
  const isTeacher = computed(() => userInfo.value?.role === 'TEACHER')
  const isStudent = computed(() => userInfo.value?.role === 'STUDENT')
  const isManager = computed(() => isAdmin.value || isTeacher.value)

  function persistUser(info: LoginVO | null) {
    userInfo.value = info
    if (info) {
      localStorage.setItem(USER_INFO_KEY, JSON.stringify(info))
    } else {
      localStorage.removeItem(USER_INFO_KEY)
    }
  }

  // 登录
  async function doLogin(loginDTO: LoginDTO) {
    const res = await login(loginDTO)
    if (res.code === 200 && res.data) {
      token.value = res.data.token
      localStorage.setItem(TOKEN_KEY, res.data.token)
      persistUser(res.data)
      return res.data
    }
    throw new Error(res.message || '登录失败')
  }

  // 注册
  async function doRegister(registerDTO: RegisterDTO) {
    const res = await register(registerDTO)
    if (res.code === 200) {
      return res.data
    }
    throw new Error(res.message || '注册失败')
  }

  // 获取用户信息
  async function fetchUserInfo() {
    if (!token.value) return null
    const res = await getUserInfo()
    if (res.code === 200 && res.data) {
      persistUser(res.data)
      return res.data
    }
    return null
  }

  // 设置用户信息
  function setUserInfo(info: LoginVO | null) {
    persistUser(info)
  }

  // 登出
  function logout() {
    token.value = ''
    persistUser(null)
    localStorage.removeItem(TOKEN_KEY)
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    isAdmin,
    isTeacher,
    isStudent,
    isManager,
    doLogin,
    doRegister,
    fetchUserInfo,
    setUserInfo,
    logout
  }
})
