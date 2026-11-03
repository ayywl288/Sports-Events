<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <span>我的报名</span>
      </template>

      <!-- 搜索表单 -->
      <el-form :inline="true" class="search-form">
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

      <!-- 数据表格 -->
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="eventName" label="赛事名称" min-width="160" />
        <el-table-column prop="sportItemName" label="运动项目" width="140" />
        <el-table-column prop="statusName" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ row.statusName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="报名时间" width="170" />
        <el-table-column prop="reviewTime" label="审核时间" width="170">
          <template #default="{ row }">{{ row.reviewTime || '-' }}</template>
        </el-table-column>
        <el-table-column prop="reviewRemark" label="审核意见" min-width="160">
          <template #default="{ row }">{{ row.reviewRemark || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleViewEvent(row)">查看赛事</el-button>
            <el-button
              v-if="row.status === 1"
              type="danger"
              link
              @click="handleCancel(row)"
            >取消报名</el-button>
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { get, post } from '@/utils/request'

const STATUS_OPTIONS = [
  { label: '待审核', value: 1 },
  { label: '已通过', value: 2 },
  { label: '已拒绝', value: 3 },
  { label: '已取消', value: 4 }
]

const router = useRouter()

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)

const queryParams = reactive({
  current: 1,
  size: 10,
  status: null as number | null
})

onMounted(() => {
  loadData()
})

async function loadData() {
  loading.value = true
  try {
    const params: Record<string, any> = {
      current: queryParams.current,
      size: queryParams.size
    }
    if (queryParams.status !== null) {
      params.status = queryParams.status
    }
    const res = await get('/registrations/my', params)
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
  queryParams.status = null
  queryParams.current = 1
  loadData()
}

type TagType = 'primary' | 'success' | 'warning' | 'info' | 'danger'

function getStatusType(status: number): TagType {
  const map: Record<number, TagType> = { 1: 'warning', 2: 'success', 3: 'danger', 4: 'info' }
  return map[status] || 'info'
}

function handleViewEvent(row: any) {
  router.push(`/events/${row.eventId}`)
}

async function handleCancel(row: any) {
  await ElMessageBox.confirm('确定要取消该报名吗？取消后可在报名截止前重新报名', '提示', { type: 'warning' })
  await post(`/registrations/${row.id}/cancel`)
  ElMessage.success('取消成功')
  loadData()
}
</script>
