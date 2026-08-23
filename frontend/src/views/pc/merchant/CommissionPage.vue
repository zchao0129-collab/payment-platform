<template>
  <div>
    <div class="page-title">佣金列表</div>
    <div class="card">
      <!-- Commission Summary -->
      <div class="stat-row" v-loading="summaryLoading">
        <div class="stat-card">
          <div class="label">累计佣金</div>
          <div class="value" style="color:var(--success)">¥{{ fmt(summary.total) }}</div>
        </div>
        <div class="stat-card">
          <div class="label">可提现</div>
          <div class="value" style="color:var(--primary)">¥{{ fmt(summary.withdrawable) }}</div>
        </div>
        <div class="stat-card">
          <div class="label">已提现</div>
          <div class="value">¥{{ fmt(summary.withdrawn) }}</div>
        </div>
        <div class="stat-card">
          <div class="label">待审核</div>
          <div class="value" style="color:var(--warning)">¥{{ fmt(summary.auditing) }}</div>
        </div>
      </div>

      <div style="margin-bottom:12px">
        <el-button type="primary" @click="withdrawVisible = true">发起提现</el-button>
      </div>

      <!-- Commission Table -->
      <el-table :data="commissions" stripe style="width:100%" v-loading="loading">
        <el-table-column prop="orderNo" label="来源订单号" />
        <el-table-column label="收款金额">
          <template #default="{ row }">¥{{ fmt(row.orderAmount) }}</template>
        </el-table-column>
        <el-table-column label="返佣比例">
          <template #default="{ row }">{{ fmtRate(row.commRate) }}</template>
        </el-table-column>
        <el-table-column label="佣金金额">
          <template #default="{ row }">¥{{ fmt(row.commAmount) }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="生成时间" />
      </el-table>

      <div class="pagination">
        <span class="page-info">共 {{ page.total }} 条</span>
        <el-pagination
          v-model:current-page="page.current"
          :page-size="page.size"
          :total="page.total"
          layout="prev, pager, next"
          small
          @current-change="fetchCommissions"
        />
      </div>
    </div>

    <!-- Withdraw Modal -->
    <WithdrawModal v-model:visible="withdrawVisible" @done="onWithdrawDone" />
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { getCommissionList, getCommissionSummary } from '@/api/commission'
import WithdrawModal from '@/components/WithdrawModal.vue'

const withdrawVisible = ref(false)
const loading = ref(false)
const summaryLoading = ref(false)

const page = reactive({ current: 1, size: 10, total: 0 })
const commissions = ref([])
const summary = reactive({
  total: 0,
  withdrawable: 0,
  withdrawn: 0,
  auditing: 0,
})

function fmt(v) { return v != null ? Number(v).toFixed(2) : '0.00' }
function fmtRate(v) { return v != null ? (Number(v) * 100).toFixed(0) + '%' : '—' }

async function fetchSummary() {
  summaryLoading.value = true
  try {
    const data = await getCommissionSummary()
    if (data) {
      summary.total = data.total || 0
      summary.withdrawable = data.withdrawable || 0
      summary.withdrawn = data.withdrawn || 0
      summary.auditing = data.auditing || 0
    }
  } catch (e) {
    console.warn('Failed to load commission summary', e)
  } finally {
    summaryLoading.value = false
  }
}

async function fetchCommissions() {
  loading.value = true
  try {
    const result = await getCommissionList({ page: page.current, size: page.size })
    commissions.value = result?.records || []
    page.total = result?.total || 0
  } catch (e) {
    console.warn('Failed to load commissions', e)
  } finally {
    loading.value = false
  }
}

function onWithdrawDone() {
  fetchSummary()
  fetchCommissions()
}

onMounted(() => {
  fetchSummary()
  fetchCommissions()
})
</script>

<style scoped>
.pagination {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
}
.page-info {
  font-size: 13px;
  color: #999;
}
</style>
