<template>
  <div>
    <div class="page-title">工作台</div>
    <!-- Stat Cards -->
    <div class="stat-row">
      <div class="stat-card">
        <div class="label">今日营收</div>
        <div class="value">¥{{ stats.todayRevenue.toFixed(2) }}</div>
        <div class="sub" style="color:var(--success)">实时更新</div>
      </div>
      <div class="stat-card">
        <div class="label">本周营收</div>
        <div class="value">¥{{ stats.weekRevenue.toFixed(2) }}</div>
        <div class="sub">共 {{ stats.weekOrders }} 笔订单</div>
      </div>
      <div class="stat-card">
        <div class="label">本月营收</div>
        <div class="value">¥{{ stats.monthRevenue.toFixed(2) }}</div>
        <div class="sub">共 {{ stats.monthOrders }} 笔订单</div>
      </div>
      <div class="stat-card">
        <div class="label">可提现佣金</div>
        <div class="value" style="color:var(--success)">¥{{ commissionStats.withdrawable }}</div>
        <div class="sub">已提现 ¥{{ commissionStats.withdrawn }}</div>
      </div>
    </div>

    <!-- Recent Orders -->
    <div class="card" v-loading="loading">
      <div class="card-header">近期订单</div>
      <el-table :data="recentOrders" stripe style="width:100%">
        <el-table-column prop="orderNo" label="订单号" />
        <el-table-column prop="amount" label="金额" />
        <el-table-column prop="payTime" label="支付时间" />
        <el-table-column label="状态">
          <template #default="{ row }">
            <span class="tag" :class="statusClass(row.orderStatus)">{{ row.statusText }}</span>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import * as statisticsApi from '@/api/statistics'
import * as orderApi from '@/api/order'
import { getCommissionSummary } from '@/api/commission'

const stats = ref({ todayRevenue: 0, weekRevenue: 0, monthRevenue: 0, weekOrders: 0, monthOrders: 0 })
const commissionStats = ref({ withdrawable: '0.00', withdrawn: '0.00' })
const recentOrders = ref([])
const loading = ref(true)

const statusMap = { 1: '新建', 2: '已支付', 3: '已回调', 4: '已退款', 5: '已失效', 6: '支付失败' }
const statusClassMap = { 1: 'tag-blue', 2: 'tag-blue', 3: 'tag-green', 4: 'tag-red', 5: 'tag-gray', 6: 'tag-orange' }

function statusText(status) { return statusMap[status] || '未知' }
function statusClass(status) { return statusClassMap[status] || 'tag-gray' }

onMounted(async () => {
  try {
    const [revenue, orders, commission] = await Promise.all([
      statisticsApi.getRevenueStats(),
      orderApi.getOrderList({ page: 1, size: 5 }),
      getCommissionSummary().catch(() => null),
    ])
    if (revenue) {
      stats.value = {
        todayRevenue: revenue.todayAmount || 0,
        weekRevenue: revenue.weekAmount || 0,
        monthRevenue: revenue.monthAmount || 0,
        weekOrders: revenue.weekOrders || 0,
        monthOrders: revenue.monthOrders || 0,
      }
    }
    if (orders?.records) {
      recentOrders.value = orders.records.map(o => ({
        ...o,
        amount: `¥${Number(o.orderAmount || 0).toFixed(2)}`,
        statusText: statusText(o.orderStatus),
      }))
    }
    if (commission) {
      commissionStats.value = {
        withdrawable: Number(commission.withdrawable || 0).toFixed(2),
        withdrawn: Number(commission.withdrawn || 0).toFixed(2),
      }
    }
  } catch (e) {
    console.warn('Failed to load dashboard data, using defaults', e)
  } finally {
    loading.value = false
  }
})
</script>
