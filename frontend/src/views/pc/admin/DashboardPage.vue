<template>
  <div v-loading="loading">
    <div class="page-title">数据看板</div>

    <!-- 营收卡片 -->
    <div class="stat-row">
      <div class="stat-card" v-for="c in revenueCards" :key="c.label">
        <div class="label">{{ c.label }}</div>
        <div class="value">¥{{ fmt(c.totalAmount) }}</div>
        <div class="sub split">
          <span>API ¥{{ fmt(c.apiAmount) }}</span>
          <span>码牌 ¥{{ fmt(c.cashierAmount) }}</span>
        </div>
      </div>
    </div>

    <!-- 成率卡片 -->
    <div class="stat-row">
      <div class="stat-card" v-for="r in rateCards" :key="r.label">
        <div class="label">{{ r.label }}</div>
        <div class="value">{{ fmtRate(r.totalRate) }}</div>
        <div class="sub split">
          <span>API {{ fmtRate(r.apiRate) }}</span>
          <span>码牌 {{ fmtRate(r.cashierRate) }}</span>
        </div>
      </div>
    </div>

    <!-- 曲线图 -->
    <div style="display:flex;gap:16px;flex-wrap:wrap">
      <div class="card" style="flex:1;min-width:min(320px,100%)">
        <div class="card-header">
          本周营收曲线
          <span class="chart-legend">
            <i style="background:#1677ff"></i>API
            <i style="background:#52c41a"></i>码牌
          </span>
        </div>
        <svg v-if="weekChart.points.length" :viewBox="`0 0 ${weekChart.w} ${weekChart.h}`" class="line-chart">
          <g v-for="g in weekChart.gridlines" :key="'g'+g.y">
            <line :x1="PAD.left" :y1="g.y" :x2="weekChart.w - PAD.right" :y2="g.y" stroke="#f0f0f0" />
            <text :x="PAD.left - 6" :y="g.y + 4" text-anchor="end" class="axis-text">{{ g.label }}</text>
          </g>
          <polyline :points="weekChart.apiLine" fill="none" stroke="#1677ff" stroke-width="2" />
          <polyline :points="weekChart.cashierLine" fill="none" stroke="#52c41a" stroke-width="2" />
          <circle v-for="p in weekChart.api" :key="'a'+p.x" :cx="p.x" :cy="p.y" r="3" fill="#1677ff" />
          <circle v-for="p in weekChart.cashier" :key="'c'+p.x" :cx="p.x" :cy="p.y" r="3" fill="#52c41a" />
          <text v-for="l in weekChart.labels" :key="l.text" :x="l.x" :y="weekChart.h - 6" text-anchor="middle" class="axis-text">{{ l.text }}</text>
        </svg>
        <div v-else style="text-align:center;color:#bbb;padding:24px">暂无数据</div>
      </div>

      <div class="card" style="flex:1;min-width:min(320px,100%)">
        <div class="card-header">
          本月营收曲线
          <span class="chart-legend">
            <i style="background:#1677ff"></i>API
            <i style="background:#52c41a"></i>码牌
          </span>
        </div>
        <svg v-if="monthChart.points.length" :viewBox="`0 0 ${monthChart.w} ${monthChart.h}`" class="line-chart">
          <g v-for="g in monthChart.gridlines" :key="'g'+g.y">
            <line :x1="PAD.left" :y1="g.y" :x2="monthChart.w - PAD.right" :y2="g.y" stroke="#f0f0f0" />
            <text :x="PAD.left - 6" :y="g.y + 4" text-anchor="end" class="axis-text">{{ g.label }}</text>
          </g>
          <polyline :points="monthChart.apiLine" fill="none" stroke="#1677ff" stroke-width="2" />
          <polyline :points="monthChart.cashierLine" fill="none" stroke="#52c41a" stroke-width="2" />
          <circle v-for="p in monthChart.api" :key="'a'+p.x" :cx="p.x" :cy="p.y" r="2.5" fill="#1677ff" />
          <circle v-for="p in monthChart.cashier" :key="'c'+p.x" :cx="p.x" :cy="p.y" r="2.5" fill="#52c41a" />
          <text v-for="l in monthChart.labels" :key="l.text" :x="l.x" :y="monthChart.h - 6" text-anchor="middle" class="axis-text">{{ l.text }}</text>
        </svg>
        <div v-else style="text-align:center;color:#bbb;padding:24px">暂无数据</div>
      </div>
    </div>

    <!-- Rankings -->
    <div style="display:flex;gap:16px;flex-wrap:wrap">
      <div class="card" style="flex:1;min-width:min(320px,100%)">
        <div class="card-header">订单排行 TOP5</div>
        <el-table :data="orderRank" stripe>
          <el-table-column label="排名" width="70">
            <template #default="{ $index }">{{ ['🥇','🥈','🥉'][$index] || $index+1 }}</template>
          </el-table-column>
          <el-table-column prop="merchantName" label="商户" />
          <el-table-column prop="orderCount" label="订单数" />
          <el-table-column label="金额">
            <template #default="{ row }">¥{{ fmt(row.totalAmount) }}</template>
          </el-table-column>
        </el-table>
      </div>
      <div class="card" style="flex:1;min-width:min(320px,100%)">
        <div class="card-header">提现排行 TOP5</div>
        <el-table :data="withdrawRank" stripe>
          <el-table-column label="排名" width="70">
            <template #default="{ $index }">{{ ['🥇','🥈','🥉'][$index] || $index+1 }}</template>
          </el-table-column>
          <el-table-column prop="merchantName" label="商户" />
          <el-table-column label="提现金额">
            <template #default="{ row }">¥{{ fmt(row.totalAmount) }}</template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import * as statisticsApi from '@/api/statistics'

const loading = ref(true)
const stats = reactive({
  today: {}, yesterday: {}, week: {}, month: {},
  weekTrend: [], monthTrend: [],
})
const orderRank = ref([])
const withdrawRank = ref([])

function fmt(v) { return v != null ? Number(v).toFixed(2) : '0.00' }
function fmtRate(v) { return v != null ? `${Number(v).toFixed(2)}%` : '—' }

const revenueCards = computed(() => [
  { label: '当日营收', ...stats.today },
  { label: '昨日营收', ...stats.yesterday },
  { label: '本周营收', ...stats.week },
  { label: '本月营收', ...stats.month },
])

const rateCards = computed(() => [
  { label: '当日成率', ...stats.today },
  { label: '昨日成率', ...stats.yesterday },
  { label: '本月成率', ...stats.month },
])

// ===== 轻量 SVG 折线图 =====
const W = 640
const H = 220
const PAD = { top: 16, right: 16, bottom: 30, left: 52 }

function buildChart(trend) {
  const points = Array.isArray(trend) ? trend : []
  const innerW = W - PAD.left - PAD.right
  const innerH = H - PAD.top - PAD.bottom
  const values = points.flatMap(p => [Number(p.apiAmount || 0), Number(p.cashierAmount || 0)])
  const max = Math.max(1, ...values)
  const n = points.length
  const stepX = n > 1 ? innerW / (n - 1) : 0
  const xAt = i => PAD.left + (n > 1 ? stepX * i : innerW / 2)
  const yAt = v => PAD.top + innerH - (v / max) * innerH

  const api = points.map((p, i) => ({ x: xAt(i), y: yAt(Number(p.apiAmount || 0)) }))
  const cashier = points.map((p, i) => ({ x: xAt(i), y: yAt(Number(p.cashierAmount || 0)) }))
  const labels = points.map((p, i) => ({ x: xAt(i), text: shortDate(p.date) }))
  const gridlines = [0, 0.5, 1].map(r => ({
    y: PAD.top + innerH - r * innerH,
    label: fmtAxis(max * r),
  }))

  return {
    points,
    api, cashier,
    apiLine: api.map(p => `${p.x},${p.y}`).join(' '),
    cashierLine: cashier.map(p => `${p.x},${p.y}`).join(' '),
    labels, gridlines,
    w: W, h: H,
  }
}

function shortDate(d) {
  if (!d) return ''
  const parts = String(d).split('-')
  return parts.length === 3 ? `${parts[1]}-${parts[2]}` : d
}

function fmtAxis(v) {
  const n = Number(v || 0)
  if (n >= 10000) return (n / 10000).toFixed(1) + 'w'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return String(Math.round(n))
}

const weekChart = computed(() => buildChart(stats.weekTrend))
const monthChart = computed(() => buildChart(stats.monthTrend))

onMounted(async () => {
  try {
    const [revenue, orders, withdraws] = await Promise.all([
      statisticsApi.getRevenueStats(),
      statisticsApi.getOrderRank(),
      statisticsApi.getWithdrawRank(),
    ])
    if (revenue) {
      stats.today = revenue.today || {}
      stats.yesterday = revenue.yesterday || {}
      stats.week = revenue.week || {}
      stats.month = revenue.month || {}
      stats.weekTrend = revenue.weekTrend || []
      stats.monthTrend = revenue.monthTrend || []
    }
    if (orders) orderRank.value = orders.slice(0, 5)
    if (withdraws) withdrawRank.value = withdraws.slice(0, 5)
  } catch (e) {
    console.warn('Failed to load dashboard', e)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.stat-card .sub.split {
  display: flex;
  gap: 12px;
  margin-top: 8px;
}
.stat-card .sub.split span {
  font-size: 12px;
  color: var(--text-secondary);
}
.chart-legend {
  float: right;
  font-size: 12px;
  font-weight: 400;
  color: var(--text-secondary);
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.chart-legend i {
  display: inline-block;
  width: 12px;
  height: 12px;
  border-radius: 2px;
  margin-left: 8px;
}
.line-chart {
  width: 100%;
  height: auto;
  display: block;
}
.axis-text {
  font-size: 11px;
  fill: var(--text-secondary);
}
</style>
