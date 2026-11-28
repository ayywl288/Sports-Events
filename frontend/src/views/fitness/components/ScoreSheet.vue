<template>
  <article class="score-sheet">
    <header class="sheet-head">
      <div class="student">
        <strong>{{ score.studentName || '未命名学生' }}</strong>
        <el-tag :type="gradeType" effect="light" round>{{ score.grade }}</el-tag>
      </div>
      <p class="meta">
        <span>学号 {{ score.studentNo || '-' }}</span>
        <i>·</i>
        <span>{{ score.academicYear }}</span>
        <i>·</i>
        <span>{{ score.genderLabel }}</span>
        <i>·</i>
        <span>{{ score.groupTypeLabel === '大一大二' ? '大一、大二' : score.groupTypeLabel === '大三大四' ? '大三、大四' : score.groupTypeLabel }}</span>
        <i>·</i>
        <span>测试日期 {{ score.testDate }}</span>
      </p>
    </header>

    <section class="metric-grid">
      <div class="metric">
        <label>标准分</label>
        <strong>{{ format(score.standardScore) }}<small>/100</small></strong>
      </div>
      <div class="metric">
        <label>附加分</label>
        <strong>{{ score.bonusScore }}<small>/20</small></strong>
      </div>
      <div class="metric">
        <label>学年总分</label>
        <strong>{{ format(score.totalScore) }}<small>/120</small></strong>
      </div>
      <div class="metric">
        <label>等级</label>
        <strong>{{ score.grade }}</strong>
      </div>
    </section>

    <el-table :data="score.items" stripe class="item-table" empty-text="暂无可展示的单项成绩">
      <el-table-column prop="itemName" label="项目" min-width="100" />
      <el-table-column label="原始成绩" min-width="102" align="right">
        <template #default="{ row }">
          {{ format(row.rawValue) }} <small class="unit">{{ row.unit }}</small>
        </template>
      </el-table-column>
      <el-table-column prop="score" label="单项得分" min-width="72" align="right" />
      <el-table-column v-if="!compact" label="权重(%)" min-width="64" align="right">
        <template #default="{ row }">{{ format(row.weight) }}</template>
      </el-table-column>
      <el-table-column v-if="!compact" label="加权得分" min-width="76" align="right">
        <template #default="{ row }">{{ format(row.weightedScore) }}</template>
      </el-table-column>
      <el-table-column v-if="!compact" prop="bonus" label="附加分" min-width="58" align="right" />
    </el-table>

    <p v-if="missingLabels.length" class="missing">
      <el-icon><WarningFilled /></el-icon>
      缺测项目：{{ missingLabels.join('、') }}（不计入加权，不计 0 分）
    </p>
    <p v-else class="missing ok">7 项全部已测，无缺测项目。</p>

    <footer v-if="score.auditOperator" class="audit">
      审核：{{ score.auditOperator }}<span v-if="score.auditTime"> · {{ formatTime(score.auditTime) }}</span>
    </footer>

    <div v-if="$slots.actions" class="sheet-actions">
      <slot name="actions" />
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import type { FitnessScore } from '@/api/fitnessScore'

const props = defineProps<{ score: FitnessScore }>()

const compact = ref(false)
function updateCompact() {
  compact.value = window.innerWidth < 900
}
onMounted(() => {
  updateCompact()
  window.addEventListener('resize', updateCompact)
})
onUnmounted(() => window.removeEventListener('resize', updateCompact))

const gradeType = computed<'success' | 'primary' | 'warning' | 'danger'>(() => {
  switch (props.score.grade) {
    case '优秀': return 'success'
    case '良好': return 'primary'
    case '及格': return 'warning'
    default: return 'danger'
  }
})

const itemNames: Record<string, string> = {
  BMI: '体重指数(BMI)',
  VITAL_CAPACITY: '肺活量',
  RUN_50M: '50米跑',
  SIT_REACH: '坐位体前屈',
  LONG_JUMP: '立定跳远',
  PULL_UP: '引体向上',
  SIT_UP: '1分钟仰卧起坐',
  RUN_1000M: '1000米跑',
  RUN_800M: '800米跑'
}
const missingLabels = computed(() =>
  (props.score.missingItemCodes || []).map(code => itemNames[code] || code)
)

function format(value: number | string | null | undefined) {
  if (value === null || value === undefined || value === '') return '-'
  const n = Number(value)
  return Number.isFinite(n) ? n.toFixed(2).replace(/\.00$/, '') : String(value)
}

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-'
}
</script>

<style scoped>
.score-sheet {
  border: 1px solid #e4e9f2;
  border-radius: 16px;
  background: #fff;
  padding: 22px clamp(16px, 3vw, 30px);
  box-shadow: 0 10px 26px rgb(23 43 77 / 6%);
}
.sheet-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 14px; flex-wrap: wrap; }
.student { display: flex; align-items: center; gap: 10px; color: var(--text-primary, #18263a); font-size: 20px; }
.meta { width: 100%; margin: 8px 0 0; color: var(--text-secondary, #62748c); font-size: 13px; line-height: 1.9; }
.meta i { margin: 0 7px; font-style: normal; color: #b6c2d2; }
.metric-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin: 20px 0; }
.metric { padding: 14px; border-radius: 12px; background: #f7f9fd; border: 1px solid #eef2f8; }
.metric label { display: block; color: #7b8aa0; font-size: 12px; }
.metric strong { display: block; margin-top: 6px; color: #1d2b44; font-size: 21px; font-variant-numeric: tabular-nums; }
.metric small { margin-left: 2px; color: #8b98aa; font-size: 12px; font-weight: 400; }
.item-table { margin-top: 4px; font-variant-numeric: tabular-nums; }
.unit { color: #8a98aa; font-size: 11px; font-weight: 400; }
.missing { display: flex; align-items: center; gap: 5px; margin: 14px 0 0; color: #b26a1f; font-size: 13px; }
.missing.ok { color: #6b7c91; }
.audit { margin-top: 12px; color: #8a98aa; font-size: 12px; }
.sheet-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 16px; }
@media (max-width: 900px) {
  .metric-grid { grid-template-columns: repeat(2, 1fr); }
  .metric strong { font-size: 18px; }
}
</style>
