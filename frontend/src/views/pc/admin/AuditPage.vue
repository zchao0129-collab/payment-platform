<template>
  <div>
    <div class="page-title">提现审核 <span class="badge" v-if="pendingCount">{{ pendingCount }}</span></div>
    <div class="card">
      <div class="search-bar">
        <el-select v-model="search.status" placeholder="审核状态" style="width:140px" clearable @change="handleSearch">
          <el-option label="待审核" :value="1" />
          <el-option label="已通过" :value="2" />
          <el-option label="已驳回" :value="3" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
      </div>

      <el-table :data="audits" stripe style="width:100%" v-loading="loading">
        <el-table-column prop="withdrawalNo" label="申请编号" width="180" />
        <el-table-column prop="merchantNo" label="商户号" width="150" />
        <el-table-column label="提现金额">
          <template #default="{ row }">¥{{ fmt(row.amount) }}</template>
        </el-table-column>
        <el-table-column prop="alipayAccount" label="支付宝账号" />
        <el-table-column prop="createdAt" label="申请时间" width="160" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <span class="tag" :class="auditStatusClass(row.status)">{{ auditStatusText(row.status) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <template v-if="row.status === 1">
              <el-button size="small" type="primary" @click="approve(row)">通过</el-button>
              <el-button size="small" type="danger" @click="openReject(row)">驳回</el-button>
            </template>
            <template v-else>
              <span style="color:#999;font-size:12px">
                {{ row.auditTime }} {{ row.status === 2 ? '已通过' : row.rejectReason ? '驳回: ' + row.rejectReason : '已驳回' }}
              </span>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <span class="page-info">共 {{ page.total }} 条</span>
        <el-pagination
          v-model:current-page="page.current"
          :page-size="page.size"
          :total="page.total"
          layout="prev, pager, next"
          small
          @current-change="fetchAudits"
        />
      </div>
    </div>

    <AuditRejectModal v-model:visible="rejectVisible" :withdrawal="selectedRow" @done="fetchAudits" />
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getWithdrawalList, approveWithdrawal } from '@/api/withdrawal'
import AuditRejectModal from '@/components/AuditRejectModal.vue'

const search = reactive({ status: null })
const page = reactive({ current: 1, size: 10, total: 0 })
const audits = ref([])
const loading = ref(false)
const rejectVisible = ref(false)
const selectedRow = ref(null)

const pendingCount = computed(() => audits.value.filter(a => a.status === 1).length)

const statusMap = { 1: '待审核', 2: '已通过', 3: '已驳回' }
const statusClassMap = { 1: 'tag-orange', 2: 'tag-green', 3: 'tag-red' }

function auditStatusText(s) { return statusMap[s] || '未知' }
function auditStatusClass(s) { return statusClassMap[s] || 'tag-gray' }
function fmt(v) { return v != null ? Number(v).toFixed(2) : '0.00' }

async function fetchAudits() {
  loading.value = true
  try {
    const params = { page: page.current, size: page.size }
    if (search.status) params.status = search.status
    const result = await getWithdrawalList(params)
    audits.value = result?.records || []
    page.total = result?.total || 0
  } catch (e) {
    console.warn('Failed to load audits', e)
  } finally {
    loading.value = false
  }
}

function handleSearch() { page.current = 1; fetchAudits() }

async function approve(row) {
  try {
    await ElMessageBox.confirm(`确定通过「${row.merchantNo}」提现 ¥${fmt(row.amount)} 的申请吗？`, '确认通过', { type: 'warning' })
    await approveWithdrawal(row.id)
    ElMessage.success('审核通过，已打款')
    fetchAudits()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败')
  }
}

function openReject(row) {
  selectedRow.value = row
  rejectVisible.value = true
}

onMounted(() => fetchAudits())
</script>

<style scoped>
.pagination { display: flex; justify-content: flex-end; align-items: center; gap: 8px; margin-top: 16px; }
.page-info { font-size: 13px; color: #999; }
</style>
