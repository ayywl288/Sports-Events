<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <span>我的成绩</span>
      </template>

      <!-- 搜索表单 -->
      <el-form :inline="true" class="search-form">
        <el-form-item label="赛事">
          <el-select v-model="queryParams.eventId" placeholder="请选择" clearable filterable>
            <el-option v-for="item in eventOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 成绩统计卡片 -->
      <el-row :gutter="20" class="mb-20">
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value">{{ statistics.totalEvents }}</div>
            <div class="stat-label">参赛赛事数</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card gold">
            <div class="stat-value">{{ statistics.goldMedals }}</div>
            <div class="stat-label">🥇 金牌</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card silver">
            <div class="stat-value">{{ statistics.silverMedals }}</div>
            <div class="stat-label">🥈 银牌</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card bronze">
            <div class="stat-value">{{ statistics.bronzeMedals }}</div>
            <div class="stat-label">🥉 铜牌</div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 数据表格 -->
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="eventName" label="赛事名称" min-width="160" />
        <el-table-column prop="sportItemName" label="运动项目" width="140" />
        <el-table-column label="成绩" width="120">
          <template #default="{ row }">{{ row.scoreText || row.score || '-' }}</template>
        </el-table-column>
        <el-table-column prop="unit" label="单位" width="80" />
        <el-table-column prop="ranking" label="名次" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.ranking === 1" type="warning" effect="dark">🥇 第1名</el-tag>
            <el-tag v-else-if="row.ranking === 2" effect="dark">🥈 第2名</el-tag>
            <el-tag v-else-if="row.ranking === 3" type="danger" effect="dark">🥉 第3名</el-tag>
            <span v-else-if="row.ranking">第{{ row.ranking }}名</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="points" label="积分" width="80" />
        <el-table-column prop="recordTime" label="记录时间" width="170">
          <template #default="{ row }">{{ row.recordTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleViewEvent(row)">查看赛事</el-button>
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
import { get } from '@/utils/request'

const router = useRouter()

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const eventOptions = ref<any[]>([])

const queryParams = reactive({
  current: 1,
  size: 10,
  eventId: null as number | null
})

const statistics = reactive({
  totalEvents: 0,
  goldMedals: 0,
  silverMedals: 0,
  bronzeMedals: 0
})

onMounted(() => {
  loadEventOptions()
  loadData()
  loadStatistics()
})

async function loadEventOptions() {
  const res = await get('/events', { size: 100 })
  if (res.code === 200) {
    eventOptions.value = res.data.records || []
  }
}

async function loadData() {
  loading.value = true
  try {
    const res = await get('/results/my', {
      current: queryParams.current,
      size: queryParams.size,
      eventId: queryParams.eventId || undefined
    })
    if (res.code === 200) {
      tableData.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } finally {
    loading.value = false
  }
}

async function loadStatistics() {
  const res = await get('/results/my/statistics')
  if (res.code === 200 && res.data) {
    Object.assign(statistics, res.data)
  }
}

function handleSearch() {
  queryParams.current = 1
  loadData()
}

function resetQuery() {
  queryParams.eventId = null
  queryParams.current = 1
  loadData()
}

function handleViewEvent(row: any) {
  router.push(`/events/${row.eventId}`)
}
</script>

<style scoped>
.stat-card {
  text-align: center;
  padding: 20px;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #409eff;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 8px;
}

.stat-card.gold .stat-value {
  color: #e6a23c;
}

.stat-card.silver .stat-value {
  color: #909399;
}

.stat-card.bronze .stat-value {
  color: #cd7f32;
}
</style>
