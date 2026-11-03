<template>
  <div class="page-container">
    <el-card shadow="never">
      <!-- 搜索表单 -->
      <el-form :inline="true" class="search-form">
        <el-form-item label="赛事">
          <el-select v-model="queryParams.eventId" placeholder="请选择" clearable filterable @change="handleEventChange">
            <el-option v-for="item in eventOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="运动项目">
          <el-select v-model="queryParams.sportItemId" placeholder="请选择" clearable filterable>
            <el-option v-for="item in sportItemOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="请选择" clearable>
            <el-option v-for="item in STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 批量操作 -->
      <div class="mb-10">
        <el-button type="success" :disabled="selectedRows.length === 0" @click="handleBatchApprove">
          批量通过（{{ selectedRows.length }}）
        </el-button>
        <el-button type="danger" :disabled="selectedRows.length === 0" @click="handleBatchReject">
          批量拒绝
        </el-button>
      </div>

      <!-- 数据表格 -->
      <el-table :data="tableData" v-loading="loading" border stripe @selection-change="onSelectionChange">
        <el-table-column type="selection" width="50" :selectable="(row: any) => row.status === 1" />
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="eventName" label="赛事名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="sportItemName" label="运动项目" width="130" />
        <el-table-column prop="realName" label="报名人" width="100" />
        <el-table-column prop="studentNo" label="学号" width="120" />
        <el-table-column prop="phone" label="联系电话" width="130" />
        <el-table-column prop="statusName" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ row.statusName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="报名时间" width="160" />
        <el-table-column prop="remark" label="报名备注" min-width="140" show-overflow-tooltip />
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 1">
              <el-button type="success" link @click="handleApprove(row)">通过</el-button>
              <el-button type="danger" link @click="handleReject(row)">拒绝</el-button>
            </template>
            <el-button type="primary" link @click="handleView(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        class="mt-20"
        v-model:current-page="queryParams.current"
        v-model:page-size="queryParams.size"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadData"
        @current-change="loadData"
      />
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="报名详情" width="600px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="赛事名称">{{ currentRow.eventName }}</el-descriptions-item>
        <el-descriptions-item label="运动项目">{{ currentRow.sportItemName }}</el-descriptions-item>
        <el-descriptions-item label="报名人">{{ currentRow.realName }}</el-descriptions-item>
        <el-descriptions-item label="学号">{{ currentRow.studentNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ currentRow.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="学院">{{ currentRow.college || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentRow.status)">{{ currentRow.statusName }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="报名时间">{{ currentRow.createTime }}</el-descriptions-item>
        <el-descriptions-item label="审核时间">{{ currentRow.reviewTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审核人">{{ currentRow.reviewerName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审核意见" :span="2">{{ currentRow.reviewRemark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="报名备注" :span="2">{{ currentRow.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 拒绝原因弹窗 -->
    <el-dialog v-model="rejectDialogVisible" title="拒绝报名" width="420px">
      <el-form ref="rejectFormRef" :model="rejectForm" :rules="rejectRules" label-width="90px">
        <el-form-item label="拒绝原因" prop="reason">
          <el-input v-model="rejectForm.reason" type="textarea" :rows="3" placeholder="请输入拒绝原因（必填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="rejectLoading" @click="submitReject">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, FormInstance, FormRules } from 'element-plus'
import { get, post } from '@/utils/request'

const STATUS_OPTIONS = [
  { label: '待审核', value: 1 },
  { label: '已通过', value: 2 },
  { label: '已拒绝', value: 3 },
  { label: '已取消', value: 4 }
]

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const eventOptions = ref<any[]>([])
const sportItemOptions = ref<any[]>([])
const selectedRows = ref<any[]>([])

const queryParams = reactive({
  current: 1,
  size: 10,
  eventId: null as number | null,
  sportItemId: null as number | null,
  status: null as number | null
})

const detailDialogVisible = ref(false)
const currentRow = ref<any>({})

const rejectDialogVisible = ref(false)
const rejectLoading = ref(false)
const rejectFormRef = ref<FormInstance>()
const rejectForm = reactive({ reason: '' })
const rejectRules: FormRules = {
  reason: [{ required: true, message: '请输入拒绝原因', trigger: 'blur' }]
}
const rejectingId = ref<number | null>(null)

onMounted(() => {
  loadEventOptions()
  loadData()
})

async function loadEventOptions() {
  const res = await get('/events', { size: 100 })
  if (res.code === 200) {
    eventOptions.value = res.data.records || []
  }
}

async function handleEventChange(eventId: number | null) {
  queryParams.sportItemId = null
  sportItemOptions.value = []
  if (!eventId) return
  const res = await get('/sport-items', { eventId, size: 100 })
  if (res.code === 200) {
    sportItemOptions.value = res.data.records || []
  }
}

async function loadData() {
  loading.value = true
  try {
    const params: Record<string, any> = {
      current: queryParams.current,
      size: queryParams.size,
      eventId: queryParams.eventId || undefined,
      sportItemId: queryParams.sportItemId || undefined
    }
    if (queryParams.status !== null) {
      params.status = queryParams.status
    }
    const res = await get('/registrations', params)
    if (res.code === 200) {
      tableData.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  queryParams.current = 1
  loadData()
}

function resetQuery() {
  queryParams.eventId = null
  queryParams.sportItemId = null
  queryParams.status = null
  queryParams.current = 1
  sportItemOptions.value = []
  loadData()
}

type TagType = 'primary' | 'success' | 'warning' | 'info' | 'danger'

function getStatusType(status: number): TagType {
  const map: Record<number, TagType> = { 1: 'warning', 2: 'success', 3: 'danger', 4: 'info' }
  return map[status] || 'info'
}

function onSelectionChange(rows: any[]) {
  selectedRows.value = rows
}

function handleView(row: any) {
  currentRow.value = row
  detailDialogVisible.value = true
}

async function handleApprove(row: any) {
  await ElMessageBox.confirm(`确定要通过「${row.realName}」的报名申请吗？`, '审核通过', { type: 'warning' })
  await post(`/registrations/${row.id}/approve`)
  ElMessage.success('审核通过')
  loadData()
}

function handleReject(row: any) {
  rejectingId.value = row.id
  rejectForm.reason = ''
  rejectDialogVisible.value = true
}

async function submitReject() {
  if (!rejectFormRef.value) return
  await rejectFormRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    rejectLoading.value = true
    try {
      await post(`/registrations/${rejectingId.value}/reject`, { remark: rejectForm.reason })
      ElMessage.success('已拒绝')
      rejectDialogVisible.value = false
      loadData()
    } finally {
      rejectLoading.value = false
    }
  })
}

async function handleBatchApprove() {
  const ids = selectedRows.value.filter((r) => r.status === 1).map((r) => r.id)
  if (ids.length === 0) {
    ElMessage.warning('请选择待审核的报名记录')
    return
  }
  await ElMessageBox.confirm(`确定要批量通过 ${ids.length} 条报名吗？`, '批量审核', { type: 'warning' })
  const res = await post('/registrations/batch-review', { ids, status: 2 })
  if (res.code === 200) {
    const { success, failed } = res.data
    ElMessage.success(`成功 ${success} 条${failed?.length ? `，失败 ${failed.length} 条` : ''}`)
    loadData()
  }
}

async function handleBatchReject() {
  const ids = selectedRows.value.filter((r) => r.status === 1).map((r) => r.id)
  if (ids.length === 0) {
    ElMessage.warning('请选择待审核的报名记录')
    return
  }
  const { value } = await ElMessageBox.prompt('请输入批量拒绝原因', '批量拒绝', {
    inputPlaceholder: '拒绝原因（必填）',
    inputValidator: (v) => (!!v && v.trim().length > 0) || '拒绝原因不能为空'
  })
  const res = await post('/registrations/batch-review', { ids, status: 3, remark: value })
  if (res.code === 200) {
    ElMessage.success(`成功 ${res.data.success} 条`)
    loadData()
  }
}
</script>
