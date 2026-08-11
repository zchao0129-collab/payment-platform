<template>
  <div v-loading="loading">
    <div class="page-title">统计分析</div>

    <!-- Stat Cards -->
    <div class="stat-row">
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
        <div class="label">当日提现</div>
        <div class="value">¥{{ fmt(stats.todayWithdraw) }}</div>
        <div class="sub">平台总提现</div>
      </div>
    </div>

    <!-- Rankings -->
    <div style="display:flex;gap:16px">
      <div class="card" style="flex:1">
        <div class="card-header">商户订单排行 TOP10</div>
        <el-table :data="orderRank" stripe>
          <el-table-column label="排名" width="80">
            <template #default="{ $index }">
              <span v-if="$index < 3" style="font-size:18px">{{ ['🥇','🥈','🥉'][$index] }}</span>
              <span v-else>{{ $index + 1 }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="merchantName" label="商户" />
          <el-table-column prop="orderCount" label="订单数" width="100" />
          <el-table-column label="总金额">
            <template #default="{ row }">¥{{ fmt(row.totalAmount) }}</template>
          </el-table-column>
        </el-table>
        <div v-if="!orderRank.length" style="text-align:center;color:#bbb;padding:8px">暂无数据</div>
      </div>

      <div class="card" style="flex:1">
        <div class="card-header">商户提现排行 TOP10</div>
        <el-table :data="withdrawRank" stripe>
          <el-table-column label="排名" width="80">
            <template #default="{ $index }">
              <span v-if="$index < 3" style="font-size:18px">{{ ['🥇','🥈','🥉'][$index] }}</span>
              <span v-else>{{ $index + 1 }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="merchantName" label="商户" />
          <el-table-column label="提现金额">
            <template #default="{ row }">¥{{ fmt(row.totalAmount) }}</template>
          </el-table-column>
        </el-table>
        <div v-if="!withdrawRank.length" style="text-align:center;color:#bbb;padding:8px">暂无数据</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getRevenueStats, getOrderRank, getWithdrawRank } from '@/api/statistics'

const loading = ref(true)
const stats = reactive({
  todayAmount: 0,
  todayOrders: 0,
  weekAmount: 0,
  weekOrders: 0,
  monthAmount: 0,
  monthOrders: 0,
  todayWithdraw: 0,
})
const orderRank = ref([])
const withdrawRank = ref([])

function fmt(v) { return v != null ? Number(v).toFixed(2) : '0.00' }

onMounted(async () => {
  try {
    const [revenue, orders, withdraws] = await Promise.all([
      getRevenueStats(),
      getOrderRank(),
      getWithdrawRank(),
    ])
    if (revenue) Object.assign(stats, {
      todayAmount: revenue.todayAmount || 0,
      todayOrders: revenue.todayOrders || 0,
      weekAmount: revenue.weekAmount || 0,
      weekOrders: revenue.weekOrders || 0,
      monthAmount: revenue.monthAmount || 0,
      monthOrders: revenue.monthOrders || 0,
      todayWithdraw: revenue.todayWithdraw || 0,
    })
    orderRank.value = Array.isArray(orders) ? orders.slice(0, 10) : []
    withdrawRank.value = Array.isArray(withdraws) ? withdraws.slice(0, 10) : []
  } catch (e) {
    console.warn('Failed to load statistics', e)
  } finally {
    loading.value = false
  }
})
</script>
