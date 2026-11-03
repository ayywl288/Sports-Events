<template>
  <div class="dashboard-container">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-cards">
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card stat-card--blue">
          <div class="stat-decor"></div>
          <div class="stat-icon">
            <el-icon><Trophy /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.totalEvents }}</div>
            <div class="stat-label">赛事总数</div>
          </div>
          <div class="stat-trend">
            <span class="trend-dot"></span>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card stat-card--green">
          <div class="stat-decor"></div>
          <div class="stat-icon">
            <el-icon><VideoPlay /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.ongoingEvents }}</div>
            <div class="stat-label">进行中赛事</div>
          </div>
          <div class="stat-trend">
            <span class="trend-dot"></span>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card stat-card--amber">
          <div class="stat-decor"></div>
          <div class="stat-icon">
            <el-icon><Document /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.totalRegistrations }}</div>
            <div class="stat-label">报名总数</div>
          </div>
          <div class="stat-trend">
            <span class="trend-dot"></span>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card stat-card--rose">
          <div class="stat-decor"></div>
          <div class="stat-icon">
            <el-icon><Clock /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.pendingRegistrations }}</div>
            <div class="stat-label">待审核报名</div>
          </div>
          <div class="stat-trend">
            <span class="trend-dot"></span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :xs="24" :lg="12">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <div class="card-header">
              <span class="header-icon">📊</span>
              <span>赛事报名人数 Top10</span>
            </div>
          </template>
          <div ref="registrationChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <div class="card-header">
              <span class="header-icon">🥧</span>
              <span>赛事状态分布</span>
            </div>
          </template>
          <div ref="statusChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷入口 -->
    <el-card shadow="hover" class="quick-card">
      <template #header>
        <div class="card-header">
          <span class="header-icon">⚡</span>
          <span>快捷入口</span>
        </div>
      </template>
      <div class="entry-list">
        <div class="entry-item" @click="router.push('/events')">
          <div class="entry-icon entry-icon--blue">
            <el-icon><Trophy /></el-icon>
          </div>
          <span class="entry-label">赛事管理</span>
        </div>
        <div class="entry-item" @click="router.push('/registrations')">
          <div class="entry-icon entry-icon--green">
            <el-icon><Document /></el-icon>
          </div>
          <span class="entry-label">报名管理</span>
        </div>
        <div class="entry-item" @click="router.push('/schedules')">
          <div class="entry-icon entry-icon--purple">
            <el-icon><Calendar /></el-icon>
          </div>
          <span class="entry-label">赛程安排</span>
        </div>
        <div class="entry-item" @click="router.push('/results')">
          <div class="entry-icon entry-icon--amber">
            <el-icon><Medal /></el-icon>
          </div>
          <span class="entry-label">成绩管理</span>
        </div>
        <div class="entry-item" @click="router.push('/statistics')">
          <div class="entry-icon entry-icon--rose">
            <el-icon><DataAnalysis /></el-icon>
          </div>
          <span class="entry-label">统计分析</span>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { get } from '@/utils/request'

const router = useRouter()

const stats = ref({
  totalEvents: 0,
  ongoingEvents: 0,
  totalRegistrations: 0,
  pendingRegistrations: 0,
  totalResults: 0
})

const registrationChartRef = ref<HTMLElement>()
const statusChartRef = ref<HTMLElement>()
let registrationChart: echarts.ECharts | null = null
let statusChart: echarts.ECharts | null = null

onMounted(async () => {
  await loadStats()
  await loadCharts()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  registrationChart?.dispose()
  statusChart?.dispose()
})

function handleResize() {
  registrationChart?.resize()
  statusChart?.resize()
}

async function loadStats() {
  try {
    const res = await get('/statistics/dashboard')
    if (res.code === 200) {
      stats.value = res.data
    }
  } catch (error) {
    console.error('加载统计数据失败', error)
  }
}

async function loadCharts() {
  // 报名人数 Top10 图表
  if (registrationChartRef.value) {
    registrationChart = echarts.init(registrationChartRef.value)
    try {
      const res = await get('/statistics/registration-top')
      if (res.code === 200) {
        const data = res.data || []
        registrationChart.setOption({
          tooltip: { trigger: 'axis' },
          xAxis: {
            type: 'category',
            data: data.map((item: any) => item.eventName),
            axisLabel: {
              rotate: 30,
              interval: 0
            }
          },
          yAxis: { type: 'value' },
          series: [{
            type: 'bar',
            data: data.map((item: any) => item.count),
            itemStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: '#409eff' },
                { offset: 1, color: '#67c23a' }
              ])
            }
          }]
        })
      }
    } catch (error) {
      console.error('加载报名统计失败', error)
    }
  }

  // 赛事状态分布图表
  if (statusChartRef.value) {
    statusChart = echarts.init(statusChartRef.value)
    try {
      const res = await get('/statistics/event-status')
      if (res.code === 200) {
        const data = res.data || []
        statusChart.setOption({
          tooltip: { trigger: 'item' },
          legend: { bottom: 0 },
          series: [{
            type: 'pie',
            radius: ['40%', '70%'],
            data: data.map((item: any) => ({
              name: item.statusName,
              value: item.count
            })),
            emphasis: {
              itemStyle: {
                shadowBlur: 10,
                shadowOffsetX: 0,
                shadowColor: 'rgba(0, 0, 0, 0.5)'
              }
            }
          }]
        })
      }
    } catch (error) {
      console.error('加载状态统计失败', error)
    }
  }
}
</script>

<style lang="scss" scoped>
.dashboard-container {
  .stat-cards {
    margin-bottom: 24px;
  }

  .stat-card {
    position: relative;
    overflow: hidden;
    
    :deep(.el-card__body) {
      display: flex;
      align-items: center;
      padding: 24px !important;
      position: relative;
    }
    
    .stat-decor {
      position: absolute;
      top: -20px;
      right: -20px;
      width: 100px;
      height: 100px;
      border-radius: 50%;
      opacity: 0.12;
    }

    .stat-icon {
      width: 56px;
      height: 56px;
      border-radius: 16px;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-right: 16px;
      flex-shrink: 0;

      .el-icon {
        font-size: 26px;
        color: #fff;
      }
    }

    .stat-info {
      flex: 1;

      .stat-value {
        font-size: 28px;
        font-weight: 700;
        color: var(--text-primary);
        line-height: 1.2;
      }

      .stat-label {
        font-size: 13px;
        color: var(--text-muted);
        margin-top: 4px;
        font-weight: 500;
      }
    }
    
    .stat-trend {
      position: absolute;
      top: 16px;
      right: 16px;
      
      .trend-dot {
        width: 8px;
        height: 8px;
        border-radius: 50%;
        background: var(--success);
        box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.2);
      }
    }
    
    // 蓝色主题
    &.stat-card--blue {
      .stat-icon {
        background: linear-gradient(135deg, #2563EB 0%, #3B82F6 100%);
        box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
      }
      .stat-decor { background: #2563EB; }
    }
    
    // 绿色主题
    &.stat-card--green {
      .stat-icon {
        background: linear-gradient(135deg, #059669 0%, #10B981 100%);
        box-shadow: 0 4px 12px rgba(5, 150, 105, 0.3);
      }
      .stat-decor { background: #10B981; }
    }
    
    // 琥珀色主题
    &.stat-card--amber {
      .stat-icon {
        background: linear-gradient(135deg, #D97706 0%, #F59E0B 100%);
        box-shadow: 0 4px 12px rgba(217, 119, 6, 0.3);
      }
      .stat-decor { background: #F59E0B; }
    }
    
    // 玫瑰色主题
    &.stat-card--rose {
      .stat-icon {
        background: linear-gradient(135deg, #DC2626 0%, #EF4444 100%);
        box-shadow: 0 4px 12px rgba(220, 38, 38, 0.3);
      }
      .stat-decor { background: #EF4444; }
    }
  }

  .chart-row {
    margin-bottom: 24px;
  }
  
  .chart-card, .quick-card {
    :deep(.el-card__header) {
      padding: 16px 24px !important;
    }
    
    :deep(.el-card__body) {
      padding: 20px 24px !important;
    }
  }
  
  .card-header {
    display: flex;
    align-items: center;
    gap: 8px;
    font-weight: 600;
    font-size: 15px;
    
    .header-icon {
      font-size: 18px;
    }
  }

  .chart-container {
    height: 320px;
  }

  .entry-list {
    display: flex;
    flex-wrap: wrap;
    gap: 16px;
    justify-content: flex-start;
  }

  .entry-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 20px 24px;
    cursor: pointer;
    border-radius: 16px;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    min-width: 110px;
    
    &:hover {
      transform: translateY(-4px);
      background: var(--bg-hover);
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
    }

    .entry-icon {
      width: 52px;
      height: 52px;
      border-radius: 14px;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-bottom: 12px;
      
      .el-icon {
        font-size: 26px;
        color: #fff;
      }
    }
    
    .entry-icon--blue {
      background: linear-gradient(135deg, #2563EB 0%, #3B82F6 100%);
      box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
    }
    
    .entry-icon--green {
      background: linear-gradient(135deg, #059669 0%, #10B981 100%);
      box-shadow: 0 4px 12px rgba(5, 150, 105, 0.3);
    }
    
    .entry-icon--purple {
      background: linear-gradient(135deg, #7C3AED 0%, #8B5CF6 100%);
      box-shadow: 0 4px 12px rgba(124, 58, 237, 0.3);
    }
    
    .entry-icon--amber {
      background: linear-gradient(135deg, #D97706 0%, #F59E0B 100%);
      box-shadow: 0 4px 12px rgba(217, 119, 6, 0.3);
    }
    
    .entry-icon--rose {
      background: linear-gradient(135deg, #DB2777 0%, #EC4899 100%);
      box-shadow: 0 4px 12px rgba(219, 39, 119, 0.3);
    }

    .entry-label {
      font-size: 14px;
      color: var(--text-secondary);
      font-weight: 500;
    }
  }
}

// 响应式适配
@media (max-width: 768px) {
  .dashboard-container {
    .stat-card {
      margin-bottom: 16px;
    }
    
    .entry-item {
      min-width: 90px;
      padding: 16px;
      
      .entry-icon {
        width: 44px;
        height: 44px;
        
        .el-icon {
          font-size: 22px;
        }
      }
    }
  }
}
</style>