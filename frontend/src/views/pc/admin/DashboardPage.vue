<template>
  <div>
    <div class="page-title">数据看板</div>
    <!-- Stat Cards -->
    <div class="stat-row" v-loading="loading">
      <div class="stat-card">
        <div class="label">当日营收</div>
        <div class="value">¥{{ fmt(stats.todayAmount) }}</div>
        <div class="sub">共 {{ stats.todayOrders }} 笔</div>
      </div>
      <div class="stat-card">
        <div class="label">本周营收</div>
        <div class="value">¥{{ fmt(stats.weekAmount) }}</div>
        <div class="sub">共 {{ stats.weekOrders }} 笔</div>
      </div>
      <div class="stat-card">
        <div class="label">本月营收</div>
        <div class="value">¥{{ fmt(stats.monthAmount) }}</div>
        <div class="sub">共 {{ stats.monthOrders }} 笔</div>
      </div>
      <div class="stat-card">
        <div class="label">商户数量</div>
        <div class="value">{{ stats.merchantCount || '—' }}</div>
        <div class="sub">平台总商户</div>
      </div>
    </div>

    <!-- Rankings -->
    <div style="display:flex;gap:16px">
      <div class="card" style="flex:1">
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
      <div class="card" style="flex:1">
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
import { ref, reactive, onMounted } from 'vue'
import * as statisticsApi from '@/api/statistics'

const loading = ref(true)
const stats = reactive({
  todayAmount: 0, todayOrders: 0,
  weekAmount: 0, weekOrders: 0,
  monthAmount: 0, monthOrders: 0,
  merchantCount: 0,
})
const orderRank = ref([])
const withdrawRank = ref([])

function fmt(v) {
  return v != null ? Number(v).toFixed(2) : '0.00'
}

onMounted(async () => {
  try {
    const [revenue, orders, withdraws] = await Promise.all([
      statisticsApi.getRevenueStats(),
      statisticsApi.getOrderRank(),
      statisticsApi.getWithdrawRank(),
    ])
    if (revenue) Object.assign(stats, {
      todayAmount: revenue.todayAmount || 0,
      todayOrders: revenue.todayOrders || 0,
      weekAmount: revenue.weekAmount || 0,
      weekOrders: revenue.weekOrders || 0,
      monthAmount: revenue.monthAmount || 0,
      monthOrders: revenue.monthOrders || 0,
    })
    if (orders) orderRank.value = orders.slice(0, 5)
    if (withdraws) withdrawRank.value = withdraws.slice(0, 5)
  } catch (e) {
    console.warn('Failed to load dashboard', e)
  } finally {
    loading.value = false
  }
})
</script>
