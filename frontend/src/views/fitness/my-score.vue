<template>
  <main class="score-page">
    <header class="page-head">
      <div>
        <h1>我的体测成绩</h1>
        <p>按《国家学生体质健康标准（2014 年修订）》大学组评分；同一学年以最新一次有效测试为准。</p>
      </div>
      <el-button :loading="loading" @click="load">刷新</el-button>
    </header>

    <el-alert
      type="info" :closable="false" show-icon
      title="这里只显示教师/管理员录入并审核有效的成绩；被判定无效的记录不会出现在学生端。"
    />

    <section v-loading="loading" class="score-list">
      <ScoreSheet v-for="score in scores" :key="score.id" :score="score" />
      <el-empty v-if="!loading && !scores.length" description="暂无已审核有效的体测成绩" />
    </section>
  </main>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { myFitnessScores } from '@/api/fitnessScore'
import type { FitnessScore } from '@/api/fitnessScore'
import ScoreSheet from './components/ScoreSheet.vue'

const scores = ref<FitnessScore[]>([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const { data } = await myFitnessScores()
    scores.value = data || []
  } catch {
    scores.value = []
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.score-page { min-width: 0; padding: 2px 0 28px; }
.page-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; margin-bottom: 16px; flex-wrap: wrap; }
.page-head h1 { margin: 0 0 6px; color: var(--text-primary, #18263a); font-size: 26px; font-weight: 700; }
.page-head p { margin: 0; color: var(--text-secondary, #50627a); font-size: 14px; line-height: 1.7; }
.score-list { display: grid; gap: 18px; margin-top: 18px; }
</style>
