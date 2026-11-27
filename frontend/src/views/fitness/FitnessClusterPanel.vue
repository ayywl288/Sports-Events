<template>
  <el-card shadow="never" class="cluster-panel">
    <form class="cluster-controls" @submit.prevent="runAnalysis">
      <label class="cluster-field">
        <span>测试日期</span>
        <el-date-picker
          v-model="date" type="date" value-format="YYYY-MM-DD"
          aria-label="聚类测试日期" placeholder="选择测试日期" :disabled="loading"
        />
      </label>
      <label class="cluster-field cluster-count">
        <span>分组数 K</span>
        <el-input-number v-model="k" :min="1" :max="10" :step="1" :precision="0"
          aria-label="分组数" :disabled="loading" />
      </label>
      <el-button native-type="submit" type="primary" :loading="loading" :disabled="!date">
        开始聚类
      </el-button>
    </form>

    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />

    <div v-if="result" class="cluster-result">
      <div class="cluster-summary">
        <h2>{{ analyzedDate }} · 聚类结果</h2>
        <p>{{ total }} 条有效记录，{{ result.analysis.iterations }} 次迭代，SSE {{ result.analysis.sse.toFixed(3) }}<span v-if="result.excludedCount">，排除 {{ result.excludedCount }} 条无效记录</span></p>
      </div>
      <div class="cluster-layout">
        <section aria-labelledby="cluster-scatter-title">
          <h3 id="cluster-scatter-title">分组散点图</h3>
          <p class="cluster-note cluster-projection">每个点代表一条体测记录。切换坐标轴只改变展示维度，颜色始终对应四项指标的聚类结果。</p>
          <div class="cluster-axes">
            <div class="cluster-axis">
              <label for="cluster-x-axis">横轴</label>
              <el-select id="cluster-x-axis" v-model="xAxis" aria-label="横轴指标">
                <el-option v-for="metric in metrics" :key="metric.key" :label="metric.label"
                  :value="metric.key" :disabled="metric.key === yAxis" />
              </el-select>
            </div>
            <div class="cluster-axis">
              <label for="cluster-y-axis">纵轴</label>
              <el-select id="cluster-y-axis" v-model="yAxis" aria-label="纵轴指标">
                <el-option v-for="metric in metrics" :key="metric.key" :label="metric.label"
                  :value="metric.key" :disabled="metric.key === xAxis" />
              </el-select>
            </div>
          </div>
          <div ref="chartHost" class="cluster-chart" role="img" :aria-label="chartLabel"></div>
        </section>
        <section aria-labelledby="cluster-centers-title">
          <h3 id="cluster-centers-title">聚类中心</h3>
          <el-table :data="centers" stripe row-key="name">
            <el-table-column label="组别" width="83">
              <template #default="{ row }">
                <span class="cluster-legend-key">
                  <i :style="{ backgroundColor: row.color }" aria-hidden="true"></i>{{ row.name }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="count" label="人数" width="60" align="right" />
            <el-table-column
              v-for="(metric, index) in metrics" :key="metric.key"
              :label="metricLabel(metric)" min-width="86" align="right"
            >
              <template #default="{ row }">{{ row.count ? row.values[index].toFixed(metric.decimals) : '—' }}</template>
            </el-table-column>
          </el-table>
          <h3 id="cluster-insights-title" class="cluster-insights-title">各组特点与训练建议</h3>
          <p class="cluster-note cluster-projection">特点按当天样本的肺活量、50 米和立定跳远比较，供集体训练安排参考。</p>
          <ul class="cluster-insights" aria-labelledby="cluster-insights-title">
            <li v-for="group in centers" :key="group.name">
              <h4 class="cluster-legend-key">
                <i :style="{ backgroundColor: group.color }" aria-hidden="true"></i>{{ group.name }}
              </h4>
              <div>
                <p>{{ group.feature }}</p>
                <p v-if="group.advice" class="cluster-advice">{{ group.advice }}</p>
              </div>
            </li>
          </ul>
        </section>
      </div>
    </div>

    <el-empty v-else-if="!loading && !error" :image-size="82"
      description="选择有体测记录的日期，再点「开始聚类」。" />
  </el-card>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, shallowRef, watch } from 'vue'
import { ScatterChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { init, use } from 'echarts/core'
import type { ECharts } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { analyzeFitness } from '@/api/fitness'
import type { FitnessClusterPoint, FitnessClusters } from '@/api/fitness'

use([ScatterChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

const props = defineProps<{ suggestedDate: string }>()
const date = ref(props.suggestedDate)
const k = ref<number | undefined>(3)
const loading = ref(false)
const error = ref('')
const result = shallowRef<FitnessClusters>()
const analyzedDate = ref('')
const chartHost = ref<HTMLDivElement>()
type MetricKey = Exclude<keyof FitnessClusterPoint, 'cluster'>
interface Metric {
  key: MetricKey
  label: string
  unit?: string
  decimals: number
  comparison?: { direction: 1 | -1; above: string; below: string }
}

const metrics: Metric[] = [
  { key: 'bmi', label: 'BMI', decimals: 2 },
  { key: 'vitalCapacity', label: '肺活量', unit: 'mL', decimals: 0,
    comparison: { direction: 1, above: '肺活量较高', below: '肺活量偏低' } },
  { key: 'run50Seconds', label: '50 米用时', unit: '秒', decimals: 2,
    comparison: { direction: -1, above: '50 米跑较快', below: '50 米跑偏慢' } },
  { key: 'jumpCm', label: '立定跳远', unit: 'cm', decimals: 0,
    comparison: { direction: 1, above: '立定跳远成绩较好', below: '立定跳远距离偏短' } }
]
const xAxis = ref<MetricKey>('bmi')
const yAxis = ref<MetricKey>('run50Seconds')
const xMetric = computed(() => metrics.find(metric => metric.key === xAxis.value)!)
const yMetric = computed(() => metrics.find(metric => metric.key === yAxis.value)!)
const palette = ['#4058c7', '#b84926', '#117969', '#8f4384', '#756512', '#116f91', '#646475', '#6451a4', '#a04560', '#316345']
let chart: ECharts | undefined
let resize: ResizeObserver | undefined

function metricLabel(metric: Metric) {
  return metric.unit ? `${metric.label} (${metric.unit})` : metric.label
}

function trainingAdvice(weaknesses: MetricKey[], strengthCount: number) {
  if (weaknesses.length >= 2) return '加强基础体能训练，优先练习相对薄弱的项目。'
  if (weaknesses.includes('jumpCm')) return '加强下肢爆发力与起跳技术练习。'
  if (weaknesses.includes('run50Seconds')) return '加强起跑与短距离加速练习。'
  if (weaknesses.includes('vitalCapacity')) return '可在日常体育课中循序增加有氧耐力练习。'
  if (strengthCount >= 2) return '保持优势，可适当安排进阶训练。'
  return '保持常规体能练习，定期复测。'
}

const centers = computed(() => {
  const analyzed = result.value
  if (!analyzed) return []
  const { analysis, points } = analyzed
  const reference = metrics.map(({ key }) => {
    const mean = points.reduce((sum, point) => sum + point[key], 0) / points.length
    const deviation = Math.sqrt(points.reduce((sum, point) => sum + (point[key] - mean) ** 2, 0) / points.length)
    return { mean, deviation }
  })
  return analysis.centers.map((values, index) => {
    const group = { name: `第${index + 1}组`, color: palette[index], count: analysis.counts[index], values }
    if (!group.count) return { ...group, feature: '当前分组暂无样本。', advice: '' }
    const strengths: string[] = []
    const weaknesses: MetricKey[] = []
    const weakLabels: string[] = []
    metrics.forEach(({ key, comparison }, metricIndex) => {
      const { mean, deviation } = reference[metricIndex]
      if (!comparison || deviation < 1e-9) return
      // Report only differences of at least half a standard deviation within this cohort.
      const relative = comparison.direction * (values[metricIndex] - mean) / deviation
      if (relative >= 0.5) strengths.push(comparison.above)
      if (relative <= -0.5) {
        weaknesses.push(key)
        weakLabels.push(comparison.below)
      }
    })
    const description = [strengths.join('、'), weakLabels.join('、')].filter(Boolean).join('，但')
    return {
      ...group,
      feature: description ? description + '。' : '三项表现接近当日样本平均水平。',
      advice: trainingAdvice(weaknesses, strengths.length)
    }
  })
})
const total = computed(() => centers.value.reduce((sum, item) => sum + item.count, 0))
const chartLabel = computed(() =>
  `${xMetric.value.label}与${yMetric.value.label}散点图。` + centers.value.map(item => item.name + ' ' + item.count + ' 人').join('，')
)

watch(() => props.suggestedDate, value => {
  if (!date.value && value) date.value = value
})

function clearChart() {
  resize?.disconnect()
  chart?.dispose()
  chart = undefined
  resize = undefined
}

watch([chartHost, result, xMetric, yMetric], ([host, analyzed, horizontal, vertical]) => {
  if (!host || !analyzed) {
    clearChart()
    return
  }
  if (chart?.getDom() !== host) {
    clearChart()
    chart = init(host)
    resize = new ResizeObserver(() => chart?.resize())
    resize.observe(host)
  }
  const grouped = centers.value.map(() => [] as number[][])
  for (const point of analyzed.points) {
    grouped[point.cluster].push([point[horizontal.key], point[vertical.key]])
  }
  chart?.setOption({
    grid: { top: 44, bottom: 54, left: 76, right: 26 },
    legend: { type: 'scroll', top: 2, itemGap: 13, textStyle: { color: '#33445d' } },
    tooltip: {
      trigger: 'item',
      renderMode: 'richText',
      formatter: (params: unknown) => {
        const point = params as { seriesName: string; value: [number, number] }
        return [point.seriesName, ...[horizontal, vertical].map((metric, index) =>
          `${metric.label} ${point.value[index].toFixed(metric.decimals)}${metric.unit ? ' ' + metric.unit : ''}`
        )].join('\n')
      }
    },
    xAxis: {
      type: 'value', name: metricLabel(horizontal), nameLocation: 'middle', nameGap: 31, scale: true,
      minInterval: horizontal.decimals === 0 ? 1 : 0.1, splitLine: { lineStyle: { color: '#e5eaf1' } }
    },
    yAxis: {
      type: 'value', name: metricLabel(vertical), nameLocation: 'middle', nameGap: 51, scale: true,
      minInterval: vertical.decimals === 0 ? 1 : 0.1, splitLine: { lineStyle: { color: '#e5eaf1' } }
    },
    series: centers.value.map((group, index) => ({
      name: group.name + ' · ' + group.count + ' 人',
      type: 'scatter' as const,
      data: grouped[index],
      symbolSize: 10,
      itemStyle: { color: group.color, opacity: 0.82 }
    }))
  }, { replaceMerge: ['series'] })
}, { flush: 'post' })

watch([date, k], () => {
  result.value = undefined
  error.value = ''
})
onBeforeUnmount(clearChart)

async function runAnalysis() {
  if (!date.value || loading.value) return
  const groupCount = k.value
  if (!groupCount || !Number.isInteger(groupCount) || groupCount < 1 || groupCount > 10) {
    error.value = '请输入 1 到 10 之间的分组数'
    return
  }
  loading.value = true
  error.value = ''
  result.value = undefined
  try {
    const testDate = date.value
    const { data } = await analyzeFitness(testDate, groupCount)
    analyzedDate.value = testDate
    result.value = data
  } catch (problem) {
    error.value = problem instanceof Error ? problem.message : '聚类失败，请核对当天的体测记录后重试'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.cluster-controls { display: flex; flex-wrap: wrap; align-items: end; gap: 12px; }
.cluster-field { display: grid; gap: 5px; margin: 0; color: var(--text-primary, #18263a); font-size: 13px; font-weight: 600; }
.cluster-field :deep(.el-input) { width: 176px; }
.cluster-count :deep(.el-input-number), .cluster-count :deep(.el-input) { width: 116px; }
.cluster-panel :deep(.el-alert) { margin-top: 16px; }
.cluster-result { margin-top: 20px; border-top: 1px solid var(--border-light, #e5eaf1); padding-top: 18px; }
.cluster-summary h2 { margin: 0 0 6px; font-size: 19px; color: var(--text-primary, #18263a); }
.cluster-summary p, .cluster-note { color: var(--text-secondary, #50627a); font-size: 13px; line-height: 1.6; }
.cluster-summary p { margin: 0; font-variant-numeric: tabular-nums; }
.cluster-layout { display: grid; grid-template-columns: minmax(0, 1.12fr) minmax(415px, 1fr); gap: 24px; margin-top: 18px; }
.cluster-layout section { min-width: 0; }
.cluster-layout h3 { margin: 0 0 11px; color: var(--text-primary, #18263a); font-size: 15px; }
.cluster-chart { width: 100%; height: 340px; }
.cluster-note { margin: 10px 0 0; }
.cluster-projection { margin: -4px 0 8px; }
.cluster-axes { display: flex; flex-wrap: wrap; gap: 8px 16px; margin: 13px 0 8px; }
.cluster-axis { display: flex; align-items: center; gap: 8px; font-size: 13px; font-weight: 600; }
.cluster-axis :deep(.el-select) { width: 130px; }
.cluster-legend-key { display: inline-flex; align-items: center; gap: 5px; white-space: nowrap; }
.cluster-legend-key i { width: 8px; height: 8px; border-radius: 50%; }
.cluster-panel :deep(.el-table) { font-variant-numeric: tabular-nums; }
.cluster-layout .cluster-insights-title { margin-top: 19px; }
.cluster-insights { list-style: none; padding: 0; margin: 0; }
.cluster-insights li { display: flex; align-items: baseline; gap: 12px; border-top: 1px solid var(--border-light, #e5eaf1); padding: 9px 0; font-size: 13px; line-height: 1.55; }
.cluster-insights h4, .cluster-insights p { margin: 0; }
.cluster-insights .cluster-advice { color: var(--text-secondary, #50627a); margin-top: 2px; }
@media (max-width: 1050px) {
  .cluster-layout { grid-template-columns: 1fr; gap: 14px; }
  .cluster-chart { height: 310px; }
}
</style>
