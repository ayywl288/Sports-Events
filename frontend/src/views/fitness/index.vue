<template>
  <main class="fitness-page">
    <header class="fitness-heading">
      <h1>{{ user.isManager ? '体测管理' : '我的体测' }}</h1>
      <p>{{ user.isManager ? '查询体测记录，按测试日期分析学生表现分布。' : '按测试日期查看本人的体测记录。' }}</p>
    </header>

    <el-tabs v-if="user.isManager" v-model="activeTab" aria-label="体测功能" class="fitness-tabs">
      <el-tab-pane name="records" label="体测记录" />
      <el-tab-pane name="clusters" label="K-Means 聚类" />
    </el-tabs>

    <el-card v-if="activeTab === 'records'" class="fitness-panel" shadow="never">
      <div class="fitness-toolbar">
        <form class="fitness-filters" @submit.prevent="loadPage(1)">
          <label v-if="user.isManager" class="fitness-field">
            <span>学生</span>
            <el-select
              v-model="filter.userId"
              aria-label="按学生筛选"
              filterable remote clearable
              placeholder="姓名或学号"
              :remote-method="searchStudents"
              :loading="studentsLoading"
              @visible-change="openStudentOptions"
            >
              <el-option
                v-for="student in students" :key="student.id"
                :label="studentLabel(student)" :value="student.id"
              />
            </el-select>
          </label>
          <label class="fitness-field">
            <span>测试日期</span>
            <el-date-picker
              v-model="filter.testDate" type="date" value-format="YYYY-MM-DD"
              aria-label="按测试日期筛选" placeholder="全部日期"
            />
          </label>
          <el-button native-type="submit" :loading="loading">查询</el-button>
          <el-button text @click="resetFilter">重置</el-button>
        </form>

        <el-button v-if="user.isManager" type="primary" @click="openForm()">
          录入体测
        </el-button>
      </div>

      <el-table
        v-loading="loading" :data="records" row-key="id" stripe
        :empty-text="loadError ? '加载失败，请重新查询' : '暂无匹配的体测记录'"
      >
        <el-table-column v-if="user.isManager" label="学生" min-width="145">
          <template #default="{ row }">
            <span class="fitness-student">{{ row.studentName || '学生 #' + row.userId }}</span>
            <small v-if="row.studentNo" class="fitness-student-no">{{ row.studentNo }}</small>
          </template>
        </el-table-column>
        <el-table-column prop="testDate" label="测试日期" min-width="114" />
        <el-table-column prop="heightCm" label="身高 (cm)" min-width="94" align="right" />
        <el-table-column prop="weightKg" label="体重 (kg)" min-width="94" align="right" />
        <el-table-column label="BMI" min-width="70" align="right">
          <template #default="{ row }">{{ calculateBmi(row) }}</template>
        </el-table-column>
        <el-table-column prop="vitalCapacity" label="肺活量 (mL)" min-width="110" align="right" />
        <el-table-column prop="run50Seconds" label="50 米 (秒)" min-width="102" align="right" />
        <el-table-column prop="jumpCm" label="立定跳远 (cm)" min-width="120" align="right" />
        <el-table-column v-if="user.isManager" label="操作" width="70" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openForm(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="total > pageSize"
        class="fitness-pagination"
        layout="total, prev, pager, next"
        :current-page="page"
        :page-size="pageSize"
        :total="total"
        @current-change="loadPage"
      />
    </el-card>

    <FitnessClusterPanel v-else-if="user.isManager" :suggested-date="latestDate" />

    <el-dialog
      v-if="user.isManager"
      v-model="formVisible" :title="editId === undefined ? '录入体测' : '编辑体测'"
      width="min(560px, calc(100vw - 28px))" :close-on-click-modal="!saving"
      :close-on-press-escape="!saving" :show-close="!saving"
    >
      <el-form label-position="top" @submit.prevent="submit">
        <el-form-item label="学生" required>
          <el-input v-if="editId !== undefined" :model-value="editingStudent" readonly />
          <el-select
            v-else v-model="draft.userId" filterable remote
            placeholder="选择学生或搜索姓名、学号" aria-label="选择学生"
            :remote-method="searchStudents" :loading="studentsLoading"
            @visible-change="openStudentOptions"
          >
            <el-option
              v-for="student in students" :key="student.id"
              :label="studentLabel(student)" :value="student.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="测试日期" required>
          <el-date-picker
            v-model="draft.testDate" type="date" value-format="YYYY-MM-DD"
            placeholder="选择测试日期" aria-label="测试日期"
          />
        </el-form-item>
        <div class="fitness-form-grid">
          <el-form-item v-for="measure in measures" :key="measure.key" :label="measure.label" required>
            <el-input-number
              v-model="draft[measure.key]" :min="measure.min" :max="measure.max"
              :step="measure.step" :controls="false" :aria-label="measure.label"
            />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button :disabled="saving" @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存记录</el-button>
      </template>
    </el-dialog>
  </main>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { findFitnessStudents, listFitness, saveFitness } from '@/api/fitness'
import type { FitnessInput, FitnessRecord, FitnessStudent } from '@/api/fitness'
import { useUserStore } from '@/stores/user'
import FitnessClusterPanel from './FitnessClusterPanel.vue'

const user = useUserStore()
const activeTab = ref('records')
const pageSize = 10
const page = ref(1)
const total = ref(0)
const records = ref<FitnessRecord[]>([])
const filter = reactive<{ userId?: number; testDate: string }>({ testDate: '' })
const loading = ref(false)
const loadError = ref(false)
const latestDate = ref('')
let pageRequest = 0

const students = ref<FitnessStudent[]>([])
const studentsLoading = ref(false)
let studentRequest = 0

const formVisible = ref(false)
const saving = ref(false)
const editId = ref<number>()
const editingStudent = ref('')
const measures = [
  { key: 'heightCm', label: '身高 (cm)', min: 50, max: 250, step: 0.1 },
  { key: 'weightKg', label: '体重 (kg)', min: 10, max: 400, step: 0.1 },
  { key: 'vitalCapacity', label: '肺活量 (mL)', min: 100, max: 10000, step: 1 },
  { key: 'run50Seconds', label: '50 米用时 (秒)', min: 4, max: 60, step: 0.01 },
  { key: 'jumpCm', label: '立定跳远 (cm)', min: 10, max: 400, step: 0.1 }
] as const

type Measure = (typeof measures)[number]['key']
type Draft = { userId?: number; testDate: string } & Record<Measure, number | null | undefined>

function blankDraft(): Draft {
  return {
    userId: undefined, testDate: dayjs().format('YYYY-MM-DD'),
    heightCm: null, weightKg: null, vitalCapacity: null, run50Seconds: null, jumpCm: null
  }
}
const draft = reactive<Draft>(blankDraft())

async function loadPage(target = 1) {
  const request = ++pageRequest
  const userId = user.isManager && filter.userId ? filter.userId : undefined
  const testDate = filter.testDate || undefined
  loading.value = true
  loadError.value = false
  try {
    const { data } = await listFitness({ current: target, size: pageSize, userId, testDate })
    if (request !== pageRequest) return
    page.value = data.current
    total.value = data.total
    records.value = data.records
    if (target === 1 && userId == null && !testDate) {
      latestDate.value = data.records[0]?.testDate ?? ''
    }
  } catch {
    if (request !== pageRequest) return
    records.value = []
    total.value = 0
    loadError.value = true
  } finally {
    if (request === pageRequest) loading.value = false
  }
}

function resetFilter() {
  filter.userId = undefined
  filter.testDate = ''
  void loadPage(1)
}

async function searchStudents(query = '') {
  const request = ++studentRequest
  studentsLoading.value = true
  try {
    const { data } = await findFitnessStudents(query.trim().slice(0, 80))
    if (request === studentRequest) students.value = data
  } catch {
    if (request === studentRequest) students.value = []
  } finally {
    if (request === studentRequest) studentsLoading.value = false
  }
}

function openStudentOptions(open: boolean) {
  if (open) void searchStudents()
}

function studentLabel(student: FitnessStudent) {
  return student.studentNo ? student.name + ' · ' + student.studentNo : student.name
}

function calculateBmi(record: FitnessRecord) {
  return (record.weightKg / (record.heightCm / 100) ** 2).toFixed(1)
}

function openForm(record?: FitnessRecord) {
  editId.value = record?.id
  Object.assign(draft, record ?? blankDraft())
  editingStudent.value = record
    ? studentLabel({ id: record.userId, name: record.studentName || '学生 #' + record.userId, studentNo: record.studentNo })
    : ''
  formVisible.value = true
}

async function submit() {
  if (saving.value) return
  if (!draft.userId || !draft.testDate) {
    ElMessage.warning('请选择学生和测试日期')
    return
  }
  for (const measure of measures) {
    const value = draft[measure.key]
    if (value == null || !Number.isFinite(value) || value < measure.min || value > measure.max) {
      ElMessage.warning('请填写有效的' + measure.label)
      return
    }
  }
  const input: FitnessInput = {
    userId: draft.userId, testDate: draft.testDate,
    heightCm: draft.heightCm!, weightKg: draft.weightKg!, vitalCapacity: draft.vitalCapacity!,
    run50Seconds: draft.run50Seconds!, jumpCm: draft.jumpCm!
  }
  saving.value = true
  try {
    await saveFitness(input, editId.value)
    ElMessage.success(editId.value === undefined ? '体测记录已保存' : '体测记录已更新')
    formVisible.value = false
    if (!latestDate.value || input.testDate > latestDate.value) latestDate.value = input.testDate
    filter.userId = undefined
    filter.testDate = ''
    await loadPage(1)
  } catch {
    // The shared request handler displays the server's validation error.
  } finally {
    saving.value = false
  }
}

onMounted(() => { void loadPage() })
</script>

<style scoped>
.fitness-page { min-width: 0; padding: 2px 0 24px; }
.fitness-heading { margin-bottom: 20px; }
.fitness-heading h1 { margin: 0 0 6px; color: var(--text-primary, #18263a); font-size: 26px; font-weight: 700; }
.fitness-heading p { margin: 0; color: var(--text-secondary, #50627a); font-size: 14px; }
.fitness-tabs { margin-bottom: 4px; }
.fitness-tabs :deep(.el-tabs__header) { margin-bottom: 14px; }
.fitness-toolbar { display: flex; justify-content: space-between; align-items: end; flex-wrap: wrap; gap: 14px; margin-bottom: 18px; }
.fitness-filters { display: flex; flex-wrap: wrap; align-items: end; gap: 12px; }
.fitness-field { display: grid; gap: 5px; min-width: 160px; margin: 0; color: var(--text-primary, #18263a); font-size: 13px; font-weight: 600; }
.fitness-field :deep(.el-input), .fitness-field :deep(.el-select) { width: 188px; max-width: 100%; }
.fitness-filters :deep(.el-button + .el-button) { margin-left: 0; }
.fitness-student, .fitness-student-no { display: block; }
.fitness-student { font-weight: 600; }
.fitness-student-no { color: var(--text-secondary, #50627a); font-size: 12px; }
.fitness-panel :deep(.el-table) { font-variant-numeric: tabular-nums; }
.fitness-pagination { justify-content: flex-end; margin-top: 14px; }
.fitness-form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); column-gap: 14px; }
.fitness-form-grid :deep(.el-input-number), :deep(.el-dialog .el-select) { width: 100%; }
@media (max-width: 620px) {
  .fitness-field { min-width: 140px; flex: 1; }
  .fitness-field :deep(.el-input), .fitness-field :deep(.el-select) { width: 100%; }
}
@media (max-width: 390px) {
  .fitness-form-grid { grid-template-columns: 1fr; }
}
</style>
