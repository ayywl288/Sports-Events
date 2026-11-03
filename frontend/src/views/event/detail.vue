<template>
  <div class="page-container">
    <el-card shadow="never" v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>赛事详情</span>
          <el-button @click="goBack">返回列表</el-button>
        </div>
      </template>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="赛事名称">{{ eventData.name }}</el-descriptions-item>
        <el-descriptions-item label="赛事类型">{{ eventData.typeName }}</el-descriptions-item>
        <el-descriptions-item label="开始日期">{{ eventData.startDate }}</el-descriptions-item>
        <el-descriptions-item label="结束日期">{{ eventData.endDate }}</el-descriptions-item>
        <el-descriptions-item label="报名开始">{{ eventData.regStartDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="报名截止">{{ eventData.regEndDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="举办地点">{{ eventData.location }}</el-descriptions-item>
        <el-descriptions-item label="主办单位">{{ eventData.organizer }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ eventData.contactPerson || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ eventData.contactPhone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="项目数量">{{ eventData.sportItemCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="报名人数">{{ eventData.registrationCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(eventData.status)">{{ eventData.statusName }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="赛事描述" :span="2">{{ eventData.description }}</el-descriptions-item>
      </el-descriptions>

      <!-- 报名入口 -->
      <div class="mt-20" v-if="isStudent">
        <el-alert
          v-if="!eventData.regOpen"
          :title="regTip"
          type="info"
          :closable="false"
          show-icon
        />
        <el-button v-else type="primary" size="large" @click="handleRegister">立即报名</el-button>
      </div>
    </el-card>

    <!-- 运动项目列表 -->
    <el-card shadow="never" class="mt-20">
      <template #header>
        <span>运动项目</span>
      </template>
      <el-table :data="sportItems" border stripe>
        <el-table-column prop="name" label="项目名称" min-width="140" />
        <el-table-column label="类别" width="110">
          <template #default="{ row }">{{ itemTypeName(row.type) }}</template>
        </el-table-column>
        <el-table-column label="性别要求" width="110">
          <template #default="{ row }">{{ genderName(row.genderLimit) }}</template>
        </el-table-column>
        <el-table-column prop="maxParticipants" label="人数上限" width="100">
          <template #default="{ row }">{{ row.maxParticipants || '不限' }}</template>
        </el-table-column>
        <el-table-column prop="currentParticipants" label="已通过" width="100" />
        <el-table-column prop="rules" label="比赛规则" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="120" v-if="isStudent">
          <template #default="{ row }">
            <el-tag v-if="registeredItemIds.has(row.id)" type="success">已报名</el-tag>
            <el-tag v-else-if="isItemFull(row)" type="info">已满</el-tag>
            <el-button
              v-else
              type="primary"
              link
              :disabled="!eventData.regOpen"
              @click="handleRegisterItem(row)"
            >报名</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="sportItems.length === 0" description="暂无比赛项目" />
    </el-card>

    <!-- 赛程安排 -->
    <el-card shadow="never" class="mt-20">
      <template #header>
        <span>赛程安排</span>
      </template>
      <el-timeline v-if="schedules.length > 0">
        <el-timeline-item
          v-for="item in schedules"
          :key="item.id"
          :timestamp="`${item.matchDate} ${item.startTime || ''}`"
          placement="top"
        >
          <el-card shadow="hover">
            <h4>{{ item.sportItemName }} - {{ item.scheduleName || '赛程' }}</h4>
            <p>地点：{{ item.location || '待定' }}</p>
            <p v-if="item.groupNo">分组：{{ item.groupNo }}</p>
            <p v-if="item.referee">裁判：{{ item.referee }}</p>
            <p>状态：<el-tag :type="getScheduleStatusType(item.status)">{{ item.statusName }}</el-tag></p>
          </el-card>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无赛程安排" />
    </el-card>

    <!-- 报名弹窗 -->
    <el-dialog v-model="registerDialogVisible" title="赛事报名" width="500px">
      <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" label-width="100px">
        <el-form-item label="运动项目" prop="sportItemId">
          <el-select v-model="registerForm.sportItemId" placeholder="请选择">
            <el-option
              v-for="item in availableItems"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="registerForm.remark" type="textarea" :rows="3" placeholder="请输入备注信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="registerDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="registerLoading" @click="submitRegister">确定报名</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { get, post } from '@/utils/request'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isStudent = computed(() => userStore.isStudent)

const loading = ref(false)
const eventData = ref<any>({})
const sportItems = ref<any[]>([])
const schedules = ref<any[]>([])
const registeredItemIds = ref<Set<number>>(new Set())

const registerDialogVisible = ref(false)
const registerLoading = ref(false)
const registerFormRef = ref<FormInstance>()
const registerForm = reactive({
  sportItemId: null as number | null,
  remark: ''
})

const registerRules: FormRules = {
  sportItemId: [{ required: true, message: '请选择运动项目', trigger: 'change' }]
}

const regTip = computed(() => {
  if (eventData.value.status !== 2) {
    return '当前赛事不在报名阶段'
  }
  if (!eventData.value.regStartDate || !eventData.value.regEndDate) {
    return '该赛事未设置报名时间窗口'
  }
  const now = new Date()
  if (now < new Date(eventData.value.regStartDate)) {
    return `报名将于 ${eventData.value.regStartDate} 开始`
  }
  if (now > new Date(`${eventData.value.regEndDate} 23:59:59`)) {
    return '报名已截止'
  }
  return '当前不可报名'
})

const availableItems = computed(() =>
  sportItems.value.filter((item) => !registeredItemIds.value.has(item.id) && !isItemFull(item))
)

onMounted(() => {
  loadEventDetail()
})

async function loadEventDetail() {
  const eventId = route.params.id as string
  loading.value = true
  try {
    const res = await get(`/events/${eventId}`)
    if (res.code === 200) {
      eventData.value = res.data || {}
    }

    const itemsRes = await get('/sport-items', { eventId, size: 100 })
    if (itemsRes.code === 200) {
      sportItems.value = itemsRes.data.records || []
    }

    const schedulesRes = await get('/schedules', { eventId, size: 100 })
    if (schedulesRes.code === 200) {
      schedules.value = schedulesRes.data.records || []
    }

    if (isStudent.value) {
      await loadMyRegistrations(eventId)
    }
  } finally {
    loading.value = false
  }
}

/**
 * 加载当前用户在该赛事下仍然有效的报名（待审核/已通过），用于禁用重复报名
 */
async function loadMyRegistrations(eventId: string) {
  const res = await get('/registrations/my', { eventId, size: 100 })
  if (res.code === 200) {
    const records = res.data?.records || []
    registeredItemIds.value = new Set(
      records.filter((r: any) => r.status === 1 || r.status === 2).map((r: any) => r.sportItemId)
    )
  }
}

function isItemFull(item: any) {
  if (!item.maxParticipants || item.maxParticipants <= 0) return false
  return (item.currentParticipants || 0) >= item.maxParticipants
}

function goBack() {
  router.push('/events')
}

type TagType = 'primary' | 'success' | 'warning' | 'info' | 'danger'

function getStatusType(status: number): TagType {
  const map: Record<number, TagType> = { 1: 'info', 2: 'primary', 3: 'success', 4: 'warning', 5: 'danger' }
  return map[status] || 'info'
}

function getScheduleStatusType(status: number): TagType {
  const map: Record<number, TagType> = { 1: 'info', 2: 'primary', 3: 'success' }
  return map[status] || 'info'
}

function itemTypeName(type: string) {
  const map: Record<string, string> = {
    TRACK: '径赛',
    FIELD: '田赛',
    BALL: '球类',
    WATER: '水上项目',
    SWIMMING: '游泳',
    OTHER: '其他'
  }
  return map[type] || type || '-'
}

function genderName(limit: number) {
  const map: Record<number, string> = { 0: '不限', 1: '仅男', 2: '仅女' }
  return map[limit] ?? '不限'
}

function handleRegister() {
  registerForm.sportItemId = null
  registerForm.remark = ''
  registerDialogVisible.value = true
}

function handleRegisterItem(item: any) {
  registerForm.sportItemId = item.id
  registerForm.remark = ''
  registerDialogVisible.value = true
}

async function submitRegister() {
  if (!registerFormRef.value) return
  await registerFormRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    registerLoading.value = true
    try {
      await post('/registrations', {
        eventId: eventData.value.id,
        sportItemId: registerForm.sportItemId,
        remark: registerForm.remark
      })
      ElMessage.success('报名成功，请等待审核')
      registerDialogVisible.value = false
      await loadMyRegistrations(String(eventData.value.id))
    } finally {
      registerLoading.value = false
    }
  })
}
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
