<template>
  <div class="page-container">
    <el-row :gutter="20">
      <!-- 个人信息卡片 -->
      <el-col :span="8">
        <el-card shadow="never">
          <template #header>
            <span>个人信息</span>
          </template>
          <div class="profile-card">
            <el-avatar :size="100" :src="userInfo.avatar || defaultAvatar" />
            <h3 class="profile-name">{{ userInfo.realName || userInfo.username }}</h3>
            <el-tag :type="getRoleType(userInfo.role)">{{ userInfo.roleName }}</el-tag>
            <div class="profile-info">
              <p><el-icon><User /></el-icon> 用户名：{{ userInfo.username }}</p>
              <p v-if="userInfo.studentNo"><el-icon><Postcard /></el-icon> 学号：{{ userInfo.studentNo }}</p>
              <p v-if="userInfo.phone"><el-icon><Phone /></el-icon> 电话：{{ userInfo.phone }}</p>
              <p v-if="userInfo.email"><el-icon><Message /></el-icon> 邮箱：{{ userInfo.email }}</p>
              <p v-if="userInfo.department"><el-icon><OfficeBuilding /></el-icon> 院系：{{ userInfo.department }}</p>
              <p><el-icon><Calendar /></el-icon> 注册时间：{{ userInfo.createTime }}</p>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 编辑信息 -->
      <el-col :span="16">
        <el-card shadow="never">
          <template #header>
            <span>编辑资料</span>
          </template>
          <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px" style="max-width: 500px">
            <el-form-item label="真实姓名" prop="realName">
              <el-input v-model="formData.realName" placeholder="请输入真实姓名" />
            </el-form-item>
            <el-form-item label="学号" prop="studentNo" v-if="userInfo.role === 'STUDENT'">
              <el-input v-model="formData.studentNo" placeholder="请输入学号" />
            </el-form-item>
            <el-form-item label="手机号码" prop="phone">
              <el-input v-model="formData.phone" placeholder="请输入手机号码" />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="formData.email" placeholder="请输入邮箱" />
            </el-form-item>
            <el-form-item label="院系" prop="department">
              <el-input v-model="formData.department" placeholder="请输入院系" />
            </el-form-item>
            <el-form-item label="性别" prop="gender">
              <el-radio-group v-model="formData.gender">
                <el-radio :value="1">男</el-radio>
                <el-radio :value="2">女</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="submitLoading" @click="handleSubmit">保存修改</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 修改密码 -->
        <el-card shadow="never" class="mt-20">
          <template #header>
            <span>修改密码</span>
          </template>
          <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="100px" style="max-width: 500px">
            <el-form-item label="原密码" prop="oldPassword">
              <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入新密码" />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="passwordLoading" @click="handleChangePassword">修改密码</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { get, put, post } from '@/utils/request'

const userStore = useUserStore()
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const userInfo = computed<any>(() => userStore.userInfo || {})

const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const formData = reactive({
  realName: '',
  studentNo: '',
  phone: '',
  email: '',
  department: '',
  gender: 0
})

const rules: FormRules = {
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ]
}

const passwordFormRef = ref<FormInstance>()
const passwordLoading = ref(false)
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirmPassword = (_rule: any, value: string, callback: Function) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const passwordRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

onMounted(() => {
  loadUserInfo()
})

async function loadUserInfo() {
  const res = await get('/auth/info')
  if (res.code === 200) {
    userStore.setUserInfo(res.data)
    Object.assign(formData, {
      realName: res.data.realName || '',
      studentNo: res.data.studentNo || '',
      phone: res.data.phone || '',
      email: res.data.email || '',
      department: res.data.department || '',
      gender: res.data.gender ?? 0
    })
  }
}

type TagType = 'primary' | 'success' | 'warning' | 'info' | 'danger'

function getRoleType(role: string): TagType {
  const map: Record<string, TagType> = {
    ADMIN: 'danger',
    TEACHER: 'warning',
    STUDENT: 'primary'
  }
  return map[role] || 'info'
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    submitLoading.value = true
    try {
      const res = await put('/auth/profile', formData)
      if (res.code === 200 && res.data) {
        userStore.setUserInfo({ ...userStore.userInfo, ...res.data })
      }
      ElMessage.success('保存成功')
      loadUserInfo()
    } finally {
      submitLoading.value = false
    }
  })
}

async function handleChangePassword() {
  if (!passwordFormRef.value) return
  await passwordFormRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    passwordLoading.value = true
    try {
      await post('/auth/change-password', {
        oldPassword: passwordForm.oldPassword,
        newPassword: passwordForm.newPassword
      })
      ElMessage.success('密码修改成功，请重新登录')
      passwordFormRef.value?.resetFields()
      // 可选：退出登录
      // userStore.logout()
    } finally {
      passwordLoading.value = false
    }
  })
}
</script>

<style scoped>
.profile-card {
  text-align: center;
  padding: 20px;
}

.profile-name {
  margin: 16px 0 8px;
  font-size: 20px;
}

.profile-info {
  text-align: left;
  margin-top: 20px;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
}

.profile-info p {
  margin: 12px 0;
  color: #606266;
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>