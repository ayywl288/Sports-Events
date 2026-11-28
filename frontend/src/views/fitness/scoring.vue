<template>
  <main class="score-admin-page">
    <header class="page-head">
      <div>
        <h1>国标体测评分</h1>
        <p>按《国家学生体质健康标准（2014 年修订）》大学组评分；支持缺测项、附加分与教师审核。</p>
      </div>
      <el-button type="primary" @click="openCreate">录入评分</el-button>
    </header>

    <el-card shadow="never" class="panel">
      <el-form :inline="true" class="filters" @submit.prevent="loadPage(1)">
        <el-form-item label="学生">
          <el-select
            v-model="query.studentId" filterable remote clearable placeholder="姓名或学号"
            :remote-method="searchStudents" :loading="studentsLoading" style="width: 210px"
          >
            <el-option
              v-for="s in students" :key="s.id"
              :label="studentLabel(s)" :value="s.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="测试日期">
          <el-date-picker
            v-model="query.testDate" type="date" value-format="YYYY-MM-DD"
            placeholder="全部日期" clearable
          />
        </el-form-item>
        <el-form-item>
          <el-button native-type="submit" :loading="loading">查询</el-button>
          <el-button text @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="records" stripe row-key="id" empty-text="暂无国标评分记录">
        <el-table-column label="学生" min-width="106">
          <template #default="{ row }">
            <span class="student-name">{{ row.studentName || '学生 #' + row.userId }}</span>
            <small class="student-no">{{ row.studentNo || '-' }}</small>
          </template>
        </el-table-column>
        <el-table-column label="测试信息" min-width="122">
          <template #default="{ row }">
            <span class="student-name">{{ row.testDate }}</span>
            <small class="student-no">{{ row.academicYear }}</small>
          </template>
        </el-table-column>
        <el-table-column label="成绩" min-width="106" align="right">
          <template #default="{ row }">
            <span class="score-value">{{ formatScore(row.totalScore) }}</span>
            <small class="score-sub" :class="{ invalid: row.status === 'INVALID' }">
              {{ row.grade }} · {{ row.statusLabel }}
            </small>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="104" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row.id)">详情</el-button>
            <el-button link type="primary" :disabled="row.status === 'INVALID'" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="total > query.size" class="pager"
        layout="total, prev, pager, next" :current-page="query.current"
        :page-size="query.size" :total="total" @current-change="loadPage"
      />
    </el-card>

    <!-- 录入 / 编辑 -->
    <el-dialog
      v-model="formVisible" :title="editingId === null ? '录入国标评分' : '编辑国标评分'"
      width="min(860px, calc(100vw - 24px))" class="entry-dialog"
      :close-on-click-modal="!saving"
      :close-on-press-escape="!saving" :show-close="!saving"
    >
      <el-form label-position="top" class="entry-form" @submit.prevent="save">
        <div class="form-grid">
          <el-form-item label="学生" required>
            <el-select
              v-model="form.userId" filterable remote placeholder="选择学生"
              :remote-method="searchStudents" :loading="studentsLoading"
              :disabled="editingId !== null" @change="onStudentChange"
            >
              <el-option v-for="s in students" :key="s.id" :label="studentLabel(s)" :value="s.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="测试日期" required>
            <el-date-picker
              v-model="form.testDate" type="date" value-format="YYYY-MM-DD"
              :disabled-date="disableFuture"
            />
          </el-form-item>
          <el-form-item label="性别" required>
            <el-radio-group v-model="form.gender">
              <el-radio value="MALE">男</el-radio>
              <el-radio value="FEMALE">女</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="年级组别" required>
            <el-select v-model="form.groupType">
              <el-option label="大一、大二" value="UNIV_1_2" />
              <el-option label="大三、大四" value="UNIV_3_4" />
            </el-select>
          </el-form-item>
          <el-form-item label="学年">
            <el-input v-model="form.academicYear" placeholder="留空按测试日期推导，如 2026-2027学年" />
          </el-form-item>
        </div>

        <div class="measure-grid">
          <el-form-item label="身高 (cm)" required>
            <el-input-number v-model="form.heightCm" :min="50" :max="250" :step="0.1" :controls="false" />
          </el-form-item>
          <el-form-item label="体重 (kg)" required>
            <el-input-number v-model="form.weightKg" :min="10" :max="400" :step="0.1" :controls="false" />
          </el-form-item>
          <el-form-item label="肺活量 (mL)">
            <el-input-number v-model="form.vitalCapacity" :min="100" :max="10000" :controls="false" />
          </el-form-item>
          <el-form-item label="50 米跑 (秒)">
            <el-input-number v-model="form.run50Seconds" :min="3" :max="30" :step="0.01" :controls="false" />
          </el-form-item>
          <el-form-item label="坐位体前屈 (cm)">
            <el-input-number v-model="form.sitReachCm" :min="-30" :max="50" :step="0.1" :controls="false" />
          </el-form-item>
          <el-form-item label="立定跳远 (cm)">
            <el-input-number v-model="form.jumpCm" :min="20" :max="400" :step="0.1" :controls="false" />
          </el-form-item>
          <el-form-item :label="strengthLabel">
            <el-input-number v-model="form.strengthCount" :min="0" :max="120" :controls="false" />
          </el-form-item>
          <el-form-item :label="enduranceLabel">
            <el-input-number v-model="form.enduranceSeconds" :min="90" :max="1200" :step="0.1" :controls="false" />
          </el-form-item>
        </div>

        <p class="hint">BMI 由身高、体重自动计算；缺测项可留空，评分时不计入加权，并在成绩单中标注缺测。</p>

        <ScoreSheet v-if="preview" :score="preview" class="preview-sheet" />
      </el-form>

      <template #footer>
        <el-button :disabled="saving" @click="formVisible = false">取消</el-button>
        <el-button :loading="previewing" :disabled="saving" @click="doPreview">试算</el-button>
        <el-button type="primary" :loading="saving" :disabled="previewing" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 详情 -->
    <el-dialog v-model="detailVisible" title="体测成绩单" width="min(900px, calc(100vw - 20px))">
      <ScoreSheet v-if="detail" :score="detail" />
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button
          v-if="detail" :type="detail.status === 'VALID' ? 'danger' : 'success'"
          plain @click="toggleStatus(detail)"
        >{{ detail.status === 'VALID' ? '判定无效' : '恢复有效' }}</el-button>
      </template>
    </el-dialog>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import {
  changeFitnessScoreStatus, createFitnessScore, findFitnessScoreStudents,
  getFitnessScore, pageFitnessScores, previewFitnessScore, updateFitnessScore
} from '@/api/fitnessScore'
import type {
  FitnessScore, FitnessScorePayload, FitnessScoreStudent
} from '@/api/fitnessScore'
import ScoreSheet from './components/ScoreSheet.vue'

const records = ref<FitnessScore[]>([])
const total = ref(0)
const loading = ref(false)
const query = reactive({
  current: 1,
  size: 10,
  studentId: null as number | null,
  testDate: null as string | null
})

const students = ref<FitnessScoreStudent[]>([])
const studentsLoading = ref(false)
let studentRequest = 0

const formVisible = ref(false)
const saving = ref(false)
const previewing = ref(false)
const editingId = ref<number | null>(null)
const preview = ref<FitnessScore | null>(null)
const form = reactive(blankForm())

const detailVisible = ref(false)
const detail = ref<FitnessScore | null>(null)

const strengthLabel = computed(() => form.gender === 'FEMALE' ? '1分钟仰卧起坐 (次)' : '引体向上 (次)')
const enduranceLabel = computed(() => form.gender === 'FEMALE' ? '800 米跑 (秒)' : '1000 米跑 (秒)')

function blankForm() {
  return {
    userId: null as number | null,
    testDate: dayjs().format('YYYY-MM-DD'),
    gender: 'MALE' as 'MALE' | 'FEMALE',
    groupType: 'UNIV_1_2' as 'UNIV_1_2' | 'UNIV_3_4',
    academicYear: '',
    heightCm: null as number | null,
    weightKg: null as number | null,
    vitalCapacity: null as number | null,
    run50Seconds: null as number | null,
    sitReachCm: null as number | null,
    jumpCm: null as number | null,
    strengthCount: null as number | null,
    enduranceSeconds: null as number | null
  }
}

function disableFuture(date: Date) {
  return date.getTime() > Date.now()
}

function studentLabel(s: FitnessScoreStudent) {
  return s.studentNo ? `${s.name}（${s.studentNo}）` : s.name
}

function formatScore(value: number | string | null | undefined) {
  if (value === null || value === undefined || value === '') return '-'
  const n = Number(value)
  return Number.isFinite(n) ? n.toFixed(2).replace(/\.00$/, '') + ' 分' : String(value)
}

async function loadPage(target = 1) {
  query.current = target
  loading.value = true
  try {
    const { data } = await pageFitnessScores({
      current: query.current, size: query.size,
      userId: query.studentId || undefined,
      testDate: query.testDate || undefined
    })
    records.value = data.records || []
    total.value = data.total || 0
  } catch {
    records.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.studentId = null
  query.testDate = null
  void loadPage(1)
}

async function searchStudents(keyword = '') {
  const request = ++studentRequest
  studentsLoading.value = true
  try {
    const { data } = await findFitnessScoreStudents(keyword.trim().slice(0, 80))
    if (request === studentRequest) students.value = data || []
  } catch {
    if (request === studentRequest) students.value = []
  } finally {
    if (request === studentRequest) studentsLoading.value = false
  }
}

function onStudentChange(id: number) {
  const student = students.value.find(item => item.id === id)
  if (student?.gender === 2) form.gender = 'FEMALE'
  if (student?.gender === 1) form.gender = 'MALE'
}

function openCreate() {
  Object.assign(form, blankForm())
  editingId.value = null
  preview.value = null
  formVisible.value = true
  void searchStudents()
}

function openEdit(row: FitnessScore) {
  editingId.value = row.id
  preview.value = null
  Object.assign(form, {
    userId: row.userId,
    testDate: row.testDate,
    gender: row.gender,
    groupType: row.groupType,
    academicYear: row.academicYear,
    heightCm: row.heightCm,
    weightKg: row.weightKg,
    vitalCapacity: row.vitalCapacity ?? null,
    run50Seconds: row.run50Seconds ?? null,
    sitReachCm: row.sitReachCm ?? null,
    jumpCm: row.jumpCm ?? null,
    strengthCount: row.strengthCount ?? null,
    enduranceSeconds: row.enduranceSeconds ?? null
  })
  if (!students.value.some(s => s.id === row.userId)) {
    students.value.push({
      id: row.userId,
      name: row.studentName || '学生 #' + row.userId,
      studentNo: row.studentNo
    })
  }
  formVisible.value = true
}

function buildPayload(): FitnessScorePayload | null {
  if (!form.userId) {
    ElMessage.warning('请选择学生')
    return null
  }
  if (!form.testDate) {
    ElMessage.warning('请选择测试日期')
    return null
  }
  if (form.heightCm == null || form.weightKg == null) {
    ElMessage.warning('请填写身高和体重')
    return null
  }
  return {
    userId: form.userId,
    testDate: form.testDate,
    heightCm: form.heightCm,
    weightKg: form.weightKg,
    vitalCapacity: form.vitalCapacity,
    run50Seconds: form.run50Seconds,
    sitReachCm: form.sitReachCm,
    jumpCm: form.jumpCm,
    strengthCount: form.strengthCount,
    enduranceSeconds: form.enduranceSeconds,
    gender: form.gender,
    groupType: form.groupType,
    academicYear: form.academicYear.trim() || null
  }
}

async function doPreview() {
  const payload = buildPayload()
  if (!payload) return
  previewing.value = true
  try {
    const { data } = await previewFitnessScore(payload)
    preview.value = data
  } catch {
    preview.value = null
  } finally {
    previewing.value = false
  }
}

async function save() {
  const payload = buildPayload()
  if (!payload) return
  saving.value = true
  try {
    if (editingId.value === null) {
      await createFitnessScore(payload)
      ElMessage.success('国标评分记录已保存')
    } else {
      await updateFitnessScore(editingId.value, payload)
      ElMessage.success('国标评分记录已更新')
    }
    formVisible.value = false
    await loadPage(1)
  } catch {
    // 统一请求拦截器已展示后端校验信息
  } finally {
    saving.value = false
  }
}

async function openDetail(id: number) {
  try {
    const { data } = await getFitnessScore(id)
    detail.value = data
    detailVisible.value = true
  } catch {
    // 拦截器已提示
  }
}

async function toggleStatus(row: FitnessScore) {
  const invalid = row.status === 'VALID'
  try {
    await ElMessageBox.confirm(
      `确定把 ${row.studentName || '该学生'} 的这条体测成绩判定为${invalid ? '无效' : '有效'}吗？`,
      '审核确认', { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await changeFitnessScoreStatus(row.id, invalid ? 'INVALID' : 'VALID')
    ElMessage.success(invalid ? '已判定为无效' : '已恢复有效')
    detailVisible.value = false
    await loadPage(query.current)
  } catch {
    // 拦截器已提示
  }
}

onMounted(() => {
  void searchStudents()
  void loadPage()
})
</script>

<style scoped>
.score-admin-page { min-width: 0; padding: 2px 0 26px; }
.page-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; margin-bottom: 18px; flex-wrap: wrap; }
.page-head h1 { margin: 0 0 6px; color: var(--text-primary, #18263a); font-size: 26px; font-weight: 700; }
.page-head p { margin: 0; color: var(--text-secondary, #50627a); font-size: 14px; line-height: 1.7; }
.panel :deep(.el-table) { font-variant-numeric: tabular-nums; }
.filters { margin-bottom: 4px; }
.student-name { display: block; font-weight: 600; }
.student-no { display: block; color: var(--text-secondary, #62748c); font-size: 12px; }
.score-value { display: block; color: #1d2b44; font-weight: 600; font-variant-numeric: tabular-nums; }
.score-sub { display: block; color: #8a6a20; font-size: 12px; }
.score-sub.invalid { color: #8a98aa; }
.pager { justify-content: flex-end; margin-top: 14px; }
.form-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); column-gap: 14px; }
.measure-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); column-gap: 14px; }
.form-grid :deep(.el-select), .measure-grid :deep(.el-input-number) { width: 100%; }
.hint { margin: 2px 0 14px; color: #7a889d; font-size: 12px; line-height: 1.7; }
.preview-sheet { margin-top: 10px; }
.entry-dialog :deep(.el-dialog__body) {
  max-height: calc(100vh - 210px);
  overflow-y: auto;
}
@media (max-width: 760px) {
  .form-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .measure-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
</style>
