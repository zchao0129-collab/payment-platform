<template>
  <div>
    <div class="page-title">提现明细</div>
    <div class="card">
      <el-table :data="withdrawals" stripe style="width:100%" v-loading="loading">
        <el-table-column prop="withdrawalNo" label="提现单号" min-width="180" />
        <el-table-column label="提现金额">
          <template #default="{ row }">¥{{ fmt(row.amount) }}</template>
        </el-table-column>
        <el-table-column prop="alipayAccount" label="支付宝账号" min-width="160" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <span class="tag" :class="statusClass(row.status)">{{ statusText(row.status) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="申请时间" min-width="160" />
        <el-table-column prop="auditTime" label="审核时间" min-width="160" />
        <el-table-column prop="rejectReason" label="驳回原因" min-width="160" />
      </el-table>

      <div class="pagination">
        <span class="page-info">共 {{ page.total }} 条</span>
        <el-pagination
          v-model:current-page="page.current"
          :page-size="page.size"
          :total="page.total"
          layout="prev, pager, next"
          small
          @current-change="fetchWithdrawals"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { getWithdrawalList } from '@/api/withdrawal'

const loading = ref(false)

const page = reactive({ current: 1, size: 10, total: 0 })
const withdrawals = ref([])

const statusMap = { 1: '待审核', 2: '已打款', 3: '已驳回' }
const statusClassMap = { 1: 'tag-orange', 2: 'tag-green', 3: 'tag-red' }

function statusText(s) { return statusMap[s] || '未知' }
function statusClass(s) { return statusClassMap[s] || 'tag-gray' }
function fmt(v) { return v != null ? Number(v).toFixed(2) : '0.00' }

async function fetchWithdrawals() {
  loading.value = true
  try {
    const result = await getWithdrawalList({ page: page.current, size: page.size })
    withdrawals.value = result?.records || []
    page.total = result?.total || 0
  } catch (e) {
    console.warn('Failed to load withdrawals', e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchWithdrawals()
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
