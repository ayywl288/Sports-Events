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
          <p class="cluster-note cluster-projection">每个点代表一条记录。图中显示 BMI 和 50 米用时，颜色来自四项指标的聚类结果。</p>
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
              v-for="(feature, index) in centerHeaders" :key="feature"
              :label="feature" min-width="86" align="right"
            >
              <template #default="{ row }">{{ row.values[index]?.toFixed(index === 1 || index === 3 ? 0 : 2) }}</template>
            </el-table-column>
          </el-table>
          <p class="cluster-note">中心按原单位显示。组号用于区分样本，国家体测等级由标准评分确定。</p>
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
import type { FitnessClusters } from '@/api/fitness'

use([ScatterChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

const props = defineProps<{ suggestedDate: string }>()
const date = ref(props.suggestedDate)
const k = ref<number | undefined>(3)
const loading = ref(false)
const error = ref('')
const result = shallowRef<FitnessClusters>()
const analyzedDate = ref('')
const chartHost = ref<HTMLDivElement>()
const centerHeaders = ['BMI', '肺活量 (mL)', '50 米 (秒)', '立定跳远 (cm)']
const palette = ['#4058c7', '#b84926', '#117969', '#8f4384', '#756512', '#116f91', '#646475', '#6451a4', '#a04560', '#316345']
let chart: ECharts | undefined
let resize: ResizeObserver | undefined

const centers = computed(() => result.value?.analysis.centers.map((values, index) => ({
  name: '第' + (index + 1) + '组',
  color: palette[index],
  count: result.value!.analysis.counts[index],
  values
})) ?? [])
const total = computed(() => centers.value.reduce((sum, item) => sum + item.count, 0))
const chartLabel = computed(() =>
  'BMI 与 50 米用时散点图。' + centers.value.map(item => item.name + ' ' + item.count + ' 人').join('，')
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

watch([chartHost, result], ([host, analyzed]) => {
  clearChart()
  if (!host || !analyzed) return
  const grouped = centers.value.map(() => [] as number[][])
  for (const point of analyzed.points) {
    grouped[point.cluster].push([point.bmi, point.run50Seconds])
  }
  chart = init(host)
  chart.setOption({
    grid: { top: 44, bottom: 54, left: 58, right: 26 },
    legend: { type: 'scroll', top: 2, itemGap: 13, textStyle: { color: '#33445d' } },
    tooltip: {
      trigger: 'item',
      renderMode: 'richText',
      formatter: (params: unknown) => {
        const point = params as { seriesName: string; value: [number, number] }
        return point.seriesName + '\nBMI ' + point.value[0].toFixed(2)
          + '\n50 米 ' + point.value[1].toFixed(2) + ' 秒'
      }
    },
    xAxis: {
      type: 'value', name: 'BMI', nameLocation: 'middle', nameGap: 31, scale: true,
      minInterval: 1, splitLine: { lineStyle: { color: '#e5eaf1' } }
    },
    yAxis: {
      type: 'value', name: '50 米用时 (秒)', nameLocation: 'middle', nameGap: 39, scale: true,
      minInterval: 0.5, splitLine: { lineStyle: { color: '#e5eaf1' } }
    },
    series: centers.value.map((group, index) => ({
      name: group.name + ' · ' + group.count + ' 人',
      type: 'scatter' as const,
      data: grouped[index],
      symbolSize: 10,
      itemStyle: { color: group.color, opacity: 0.82 }
    }))
  })
  resize = new ResizeObserver(() => chart?.resize())
  resize.observe(host)
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
.cluster-legend-key { display: inline-flex; align-items: center; gap: 5px; white-space: nowrap; }
.cluster-legend-key i { width: 8px; height: 8px; border-radius: 50%; }
.cluster-panel :deep(.el-table) { font-variant-numeric: tabular-nums; }
@media (max-width: 1050px) {
  .cluster-layout { grid-template-columns: 1fr; gap: 14px; }
  .cluster-chart { height: 310px; }
}
</style>
