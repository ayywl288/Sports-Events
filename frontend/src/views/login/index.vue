<template>
  <div class="login-container">
    <!-- 背景装饰 -->
    <div class="bg-decoration">
      <div class="bg-circle bg-circle-1"></div>
      <div class="bg-circle bg-circle-2"></div>
      <div class="bg-circle bg-circle-3"></div>
      <div class="bg-grid"></div>
    </div>
    
    <div class="login-box">
      <div class="login-header">
        <div class="logo-wrapper">
          <img src="@/assets/logo.svg" alt="logo" class="login-logo" />
        </div>
        <h1 class="login-title">校园体育赛事管理系统</h1>
        <p class="login-subtitle">Campus Sports Events Management System</p>
      </div>
      <el-form
        ref="formRef"
        :model="loginForm"
        :rules="rules"
        class="login-form"
        @keyup.enter="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            prefix-icon="User"
            size="large"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            size="large"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="login-btn"
            @click="handleLogin"
          >
            <span class="btn-text">登 录</span>
          </el-button>
        </el-form-item>
        <div class="login-footer">
          <span>还没有账号？</span>
          <router-link to="/register">立即注册</router-link>
        </div>
      </el-form>
      <div class="demo-accounts">
        <div class="demo-divider"><span>演示账号</span></div>
        <div class="demo-tags">
          <el-tag @click="fillDemo('admin', '123456')" class="demo-tag">管理员: admin</el-tag>
          <el-tag type="success" @click="fillDemo('teacher1', '123456')" class="demo-tag">教师: teacher1</el-tag>
          <el-tag type="warning" @click="fillDemo('student1', '123456')" class="demo-tag">学生: student1</el-tag>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage, FormInstance, FormRules } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
}

function fillDemo(username: string, password: string) {
  loginForm.username = username
  loginForm.password = password
}

async function handleLogin() {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      await userStore.doLogin(loginForm)
      ElMessage.success('登录成功')

      // 跳转到之前的页面或首页
      const redirect = route.query.redirect as string
      router.push(redirect || '/')
    } catch (error: any) {
      ElMessage.error(error.message || '登录失败')
    } finally {
      loading.value = false
    }
  })
}
</script>

<style lang="scss" scoped>
.login-container {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #0F172A 0%, #1E1B4B 40%, #581C87 100%);
}

// 背景装饰
.bg-decoration {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.bg-circle {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.6;
}

.bg-circle-1 {
  width: 600px;
  height: 600px;
  background: radial-gradient(circle, #3B82F6 0%, transparent 70%);
  top: -200px;
  left: -200px;
  animation: float 20s ease-in-out infinite;
}

.bg-circle-2 {
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, #8B5CF6 0%, transparent 70%);
  bottom: -150px;
  right: -150px;
  animation: float 25s ease-in-out infinite reverse;
}

.bg-circle-3 {
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, #EC4899 0%, transparent 70%);
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  animation: pulse 15s ease-in-out infinite;
}

.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255,255,255,0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255,255,255,0.03) 1px, transparent 1px);
  background-size: 50px 50px;
  mask-image: radial-gradient(circle at center, black 30%, transparent 70%);
}

@keyframes float {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(30px, -30px); }
}

@keyframes pulse {
  0%, 100% { opacity: 0.3; transform: translate(-50%, -50%) scale(1); }
  50% { opacity: 0.5; transform: translate(-50%, -50%) scale(1.1); }
}

.login-box {
  width: 440px;
  padding: 48px 40px 36px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border-radius: 24px;
  box-shadow: 
    0 25px 50px -12px rgba(0, 0, 0, 0.4),
    0 0 0 1px rgba(255, 255, 255, 0.1);
  position: relative;
  z-index: 10;
  animation: slideUp 0.6s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.login-header {
  text-align: center;
  margin-bottom: 36px;

  .logo-wrapper {
    width: 72px;
    height: 72px;
    margin: 0 auto 20px;
    border-radius: 20px;
    background: linear-gradient(135deg, #2563EB 0%, #7C3AED 50%, #EC4899 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 8px 24px rgba(37, 99, 235, 0.35);
    animation: logoFloat 4s ease-in-out infinite;
    
    .login-logo {
      width: 52px;
      height: 52px;
      filter: drop-shadow(0 2px 4px rgba(0,0,0,0.2));
    }
  }
  
  @keyframes logoFloat {
    0%, 100% { transform: translateY(0); }
    50% { transform: translateY(-4px); }
  }

  .login-title {
    font-size: 24px;
    color: #1E293B;
    margin-bottom: 6px;
    font-weight: 700;
    letter-spacing: 0.5px;
  }

  .login-subtitle {
    font-size: 12px;
    color: #94A3B8;
    letter-spacing: 1px;
  }
}

.login-form {
  :deep(.el-input__wrapper) {
    padding: 4px 16px;
    border-radius: 12px !important;
    box-shadow: 0 0 0 1px #E2E8F0 inset !important;
    background: #F8FAFC;
    transition: all 0.25s ease;
    
    &:hover {
      box-shadow: 0 0 0 1px #3B82F6 inset !important;
      background: #fff;
    }
    
    &.is-focus {
      box-shadow: 0 0 0 2px #2563EB inset, 0 0 0 4px rgba(37, 99, 235, 0.12) !important;
      background: #fff;
    }
  }
  
  :deep(.el-input__inner) {
    height: 44px;
    font-size: 15px;
  }
  
  :deep(.el-input__prefix) {
    color: #94A3B8;
  }
  
  :deep(.el-form-item) {
    margin-bottom: 20px;
  }
  
  .login-btn {
    width: 100%;
    height: 48px;
    border-radius: 12px;
    font-size: 16px;
    font-weight: 600;
    letter-spacing: 4px;
    background: linear-gradient(135deg, #2563EB 0%, #7C3AED 50%, #EC4899 100%);
    border: none;
    box-shadow: 0 6px 20px rgba(37, 99, 235, 0.4);
    transition: all 0.3s ease;
    
    .btn-text {
      position: relative;
      z-index: 1;
    }
    
    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 28px rgba(37, 99, 235, 0.5);
    }
    
    &:active {
      transform: translateY(0);
    }
  }
}

.login-footer {
  text-align: center;
  color: #64748B;
  font-size: 14px;

  a {
    color: #2563EB;
    text-decoration: none;
    font-weight: 500;
    transition: all 0.2s ease;

    &:hover {
      color: #7C3AED;
    }
  }
}

.demo-accounts {
  margin-top: 28px;
  
  .demo-divider {
    position: relative;
    text-align: center;
    margin-bottom: 16px;
    
    &::before {
      content: '';
      position: absolute;
      top: 50%;
      left: 0;
      right: 0;
      height: 1px;
      background: linear-gradient(90deg, transparent 0%, #E2E8F0 50%, transparent 100%);
    }
    
    span {
      position: relative;
      padding: 0 16px;
      background: rgba(255, 255, 255, 0.95);
      font-size: 12px;
      color: #94A3B8;
      letter-spacing: 2px;
    }
  }
  
  .demo-tags {
    display: flex;
    flex-wrap: wrap;
    justify-content: center;
    gap: 8px;
  }
  
  .demo-tag {
    cursor: pointer;
    transition: all 0.25s ease;
    padding: 6px 14px !important;
    font-size: 13px;
    
    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    }
  }
}
</style>