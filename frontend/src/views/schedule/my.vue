<template>
  <div class="page-container my-schedule">
    <section class="ms-hero">
      <div>
        <div class="ms-hero__title">我的日程</div>
        <div class="ms-hero__desc">
          根据已通过审核的报名自动生成；同一时段参加多个项目会被标记为时间冲突
        </div>
      </div>
      <div class="ms-hero__actions">
        <el-button round @click="loadData">刷新</el-button>
        <el-button round type="primary" @click="router.push('/events')">赛事大厅</el-button>
      </div>
    </section>

    <div v-if="!loading && list.length" class="ms-stats">
      <div v-for="item in statCards" :key="item.label" class="ms-stat">
        <div class="ms-stat__icon" :class="item.color">
          <el-icon><component :is="item.icon" /></el-icon>
        </div>
        <div>
          <div class="ms-stat__value">{{ item.value }}</div>
          <div class="ms-stat__label">{{ item.label }}</div>
        </div>
      </div>
    </div>

    <div v-loading="loading" class="ms-body">
      <el-empty v-if="!loading && !list.length" description="暂无比赛安排，先去报名参加项目吧">
        <el-button type="primary" @click="router.push('/events')">去报名参赛</el-button>
      </el-empty>

      <template v-else>
        <div v-for="group in groups" :key="group.date" class="ms-day">
          <div class="ms-day__head">
            <div class="ms-day__date">
              <span class="ms-day__md">{{ group.md }}</span>
              <span class="ms-day__week">{{ group.week }}</span>
              <el-tag v-if="group.isToday" size="small" effect="dark">今天</el-tag>
            </div>
            <span class="ms-day__count">{{ group.items.length }} 场比赛</span>
          </div>

          <div class="ms-timeline">
            <div
              v-for="item in group.items"
              :key="item.id"
              class="ms-item"
              :class="{ 'is-conflict': item.conflict, 'is-done': item.status === 3 }"
            >
              <div class="ms-item__time">
                <span class="ms-item__start">{{ item.startTime }}</span>
                <span v-if="item.endTime" class="ms-item__end">- {{ item.endTime }}</span>
              </div>
              <div class="ms-item__dot" />
              <div class="ms-item__card">
                <div class="ms-item__head">
                  <div class="ms-item__title">{{ item.sportItemName }}</div>
                  <div class="ms-item__tags">
                    <el-tag v-if="item.conflict" type="danger" size="small" effect="dark">
                      <el-icon><WarningFilled /></el-icon> 时间冲突
                    </el-tag>
                    <el-tag :type="statusType(item.status)" size="small">
                      {{ item.statusName || statusName(item.status) }}
                    </el-tag>
                  </div>
                </div>
                <div class="ms-item__sub">{{ item.eventName }}</div>
                <div class="ms-item__meta">
                  <span v-if="item.scheduleName">
                    <el-icon><Flag /></el-icon>{{ item.scheduleName }}
                  </span>
                  <span v-if="item.roundNo">
                    <el-icon><RefreshRight /></el-icon>第 {{ item.roundNo }} 轮
                  </span>
                  <span v-if="item.groupNo">
                    <el-icon><Grid /></el-icon>{{ item.groupNo }}
                  </span>
                  <span>
                    <el-icon><Location /></el-icon>{{ item.location || '场地待定' }}
                  </span>
                </div>
                <div v-if="item.conflict" class="ms-item__conflict">
                  <el-icon><WarningFilled /></el-icon>
                  <span>{{ item.conflictRemark }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { get } from '@/utils/request'

interface MySchedule {
  id: number
  eventId: number
  eventName: string
  sportItemId: number
  sportItemName: string
  scheduleName?: string
  roundNo?: number
  groupNo?: string
  matchDate: string
  startTime: string
  endTime?: string
  location?: string
  status: number
  statusName?: string
  conflict?: boolean
  conflictRemark?: string
}

const router = useRouter()
const loading = ref(false)
const list = ref<MySchedule[]>([])

const groups = computed(() => {
  const map = new Map<string, MySchedule[]>()
  for (const item of list.value) {
    const key = item.matchDate || '日期待定'
    if (!map.has(key)) {
      map.set(key, [])
    }
    map.get(key)!.push(item)
  }
  const today = todayText()
  return Array.from(map.entries())
    .sort((a, b) => a[0].localeCompare(b[0]))
    .map(([date, items]) => ({
      date,
      items,
      md: formatMonthDay(date),
      week: weekdayText(date),
      isToday: date === today
    }))
})

const statCards = computed(() => [
  { label: '全部场次', value: list.value.length, icon: 'Calendar', color: 'is-blue' },
  { label: '待开始', value: list.value.filter((item) => item.status === 1).length, icon: 'Clock', color: 'is-amber' },
  { label: '已结束', value: list.value.filter((item) => item.status === 3).length, icon: 'CircleCheck', color: 'is-green' },
  { label: '时间冲突', value: list.value.filter((item) => item.conflict).length, icon: 'WarningFilled', color: 'is-red' }
])

onMounted(loadData)

async function loadData() {
  loading.value = true
  try {
    const res = await get<MySchedule[]>('/schedules/my')
    if (res.code === 200) {
      list.value = res.data || []
    }
  } finally {
    loading.value = false
  }
}

type TagType = 'primary' | 'success' | 'warning' | 'info' | 'danger'

function statusType(status: number): TagType {
  const map: Record<number, TagType> = { 1: 'info', 2: 'primary', 3: 'success' }
  return map[status] || 'info'
}

function statusName(status: number) {
  const map: Record<number, string> = { 1: '未开始', 2: '进行中', 3: '已结束' }
  return map[status] || '未知'
}

function todayText() {
  const now = new Date()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${now.getFullYear()}-${month}-${day}`
}

function formatMonthDay(date: string) {
  const parts = date.split('-')
  if (parts.length !== 3) return date
  return `${Number(parts[1])}月${Number(parts[2])}日`
}

function weekdayText(date: string) {
  const parts = date.split('-').map(Number)
  if (parts.length !== 3 || parts.some(Number.isNaN)) return ''
  const week = new Date(parts[0], parts[1] - 1, parts[2]).getDay()
  return `周${'日一二三四五六'[week]}`
}
</script>

<style scoped lang="scss">
.my-schedule {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.ms-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 24px 28px;
  border-radius: var(--radius-lg);
  background: linear-gradient(135deg, #1e3a8a 0%, #312e81 55%, #4c1d95 100%);
  color: #fff;
  box-shadow: var(--shadow-lg);

  &__title {
    font-size: 22px;
    font-weight: 700;
    letter-spacing: 1px;
  }

  &__desc {
    margin-top: 8px;
    font-size: 13px;
    color: rgba(255, 255, 255, 0.78);
  }

  &__actions {
    display: flex;
    gap: 10px;
    flex-shrink: 0;
  }
}

.ms-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.ms-stat {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);

  &__icon {
    width: 44px;
    height: 44px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 20px;
    color: #fff;

    &.is-blue { background: linear-gradient(135deg, #2563eb, #3b82f6); }
    &.is-amber { background: linear-gradient(135deg, #f59e0b, #d97706); }
    &.is-green { background: linear-gradient(135deg, #10b981, #059669); }
    &.is-red { background: linear-gradient(135deg, #ef4444, #dc2626); }
  }

  &__value {
    font-size: 24px;
    font-weight: 700;
    line-height: 1.1;
    color: var(--text-primary);
  }

  &__label {
    margin-top: 4px;
    font-size: 13px;
    color: var(--text-secondary);
  }
}

.ms-body {
  min-height: 200px;
}

.ms-day {
  margin-bottom: 26px;

  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 14px;
    padding-left: 4px;
  }

  &__date {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  &__md {
    font-size: 18px;
    font-weight: 700;
    color: var(--text-primary);
  }

  &__week {
    font-size: 13px;
    color: var(--text-secondary);
  }

  &__count {
    font-size: 13px;
    color: var(--text-muted);
  }
}

.ms-timeline {
  position: relative;
  padding-left: 8px;

  &::before {
    content: '';
    position: absolute;
    left: 92px;
    top: 8px;
    bottom: 8px;
    width: 2px;
    background: linear-gradient(180deg, rgba(37, 99, 235, 0.35), rgba(124, 58, 237, 0.15));
  }
}

.ms-item {
  position: relative;
  display: flex;
  align-items: stretch;
  gap: 18px;
  padding: 10px 0;

  &__time {
    width: 72px;
    flex-shrink: 0;
    text-align: right;
    padding-top: 12px;
    display: flex;
    flex-direction: column;
    line-height: 1.2;
  }

  &__start {
    font-size: 17px;
    font-weight: 700;
    color: var(--text-primary);
  }

  &__end {
    font-size: 12px;
    color: var(--text-muted);
    margin-top: 4px;
  }

  &__dot {
    position: relative;
    z-index: 1;
    width: 12px;
    height: 12px;
    margin-top: 18px;
    border-radius: 50%;
    background: #fff;
    border: 3px solid var(--primary);
    box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.12);
  }

  &__card {
    flex: 1;
    background: var(--bg-card);
    border: 1px solid var(--border-light);
    border-left: 4px solid var(--primary);
    border-radius: var(--radius-lg);
    padding: 16px 20px;
    box-shadow: var(--shadow-sm);
    transition: transform 0.2s ease, box-shadow 0.2s ease;

    &:hover {
      transform: translateY(-2px);
      box-shadow: var(--shadow-md);
    }
  }

  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
  }

  &__title {
    font-size: 16px;
    font-weight: 600;
    color: var(--text-primary);
  }

  &__tags {
    display: flex;
    align-items: center;
    gap: 8px;

    .el-tag {
      display: inline-flex;
      align-items: center;
      gap: 4px;
    }
  }

  &__sub {
    margin-top: 4px;
    font-size: 13px;
    color: var(--text-secondary);
  }

  &__meta {
    display: flex;
    flex-wrap: wrap;
    gap: 8px 18px;
    margin-top: 10px;
    font-size: 13px;
    color: var(--text-secondary);

    span {
      display: inline-flex;
      align-items: center;
      gap: 4px;
    }
  }

  &__conflict {
    display: flex;
    align-items: flex-start;
    gap: 6px;
    margin-top: 12px;
    padding: 8px 12px;
    border-radius: var(--radius-md);
    background: rgba(239, 68, 68, 0.08);
    color: var(--danger);
    font-size: 12px;
    line-height: 1.5;
  }

  &.is-conflict &__card {
    border-left-color: var(--danger);
    background: linear-gradient(180deg, #fff, #fff5f5);
  }

  &.is-conflict &__dot {
    border-color: var(--danger);
    box-shadow: 0 0 0 4px rgba(239, 68, 68, 0.14);
  }

  &.is-done &__card {
    opacity: 0.72;
  }

  &.is-done &__dot {
    border-color: var(--text-muted);
    box-shadow: none;
  }
}

@media (max-width: 900px) {
  .ms-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .ms-hero {
    flex-direction: column;
    align-items: flex-start;
  }

  .ms-timeline::before {
    left: 78px;
  }

  .ms-item__time {
    width: 58px;
  }
}
</style>
