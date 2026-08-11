<template>
  <div>
    <div class="page-title">我的订单</div>
    <div class="card">
      <!-- Search Bar -->
      <div class="search-bar">
        <el-input v-model="search.orderNo" placeholder="订单号" style="width:160px" clearable />
        <el-input v-model="search.amount" placeholder="订单金额" style="width:120px" clearable />
        <el-input v-model="search.productName" placeholder="产品名称" style="width:160px" clearable />
        <el-select v-model="search.status" placeholder="订单状态" style="width:130px" clearable>
          <el-option label="新建" :value="1" />
          <el-option label="已支付" :value="2" />
          <el-option label="已回调" :value="3" />
          <el-option label="已退款" :value="4" />
          <el-option label="已失效" :value="5" />
          <el-option label="支付失败" :value="6" />
        </el-select>
        <el-date-picker v-model="search.dateRange" type="daterange" range-separator="-" start-placeholder="开始日期" end-placeholder="结束日期" style="width:240px" />
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </div>

    <!-- Table -->
    <div class="card">
      <el-table :data="orders" stripe style="width:100%">
        <el-table-column prop="orderNo" label="订单号" />
        <el-table-column prop="productName" label="产品名称" />
        <el-table-column prop="amount" label="订单金额" />
        <el-table-column prop="payTime" label="支付时间" />
        <el-table-column label="订单状态">
          <template #default="{ row }">
            <span class="tag" :class="statusClass(row.status)">{{ row.statusText }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default>
            <a href="#">详情</a>
          </template>
        </el-table-column>
      </el-table>

      <!-- Pagination -->
      <div class="pagination">
        <span class="page-info">共 {{ page.total }} 条</span>
        <el-pagination
          v-model:current-page="page.current"
          :page-size="10"
          :total="page.total"
          layout="prev, pager, next"
          small
          @current-change="fetchOrders"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import * as orderApi from '@/api/order'

const search = reactive({
  orderNo: '',
  amount: '',
  productName: '',
  status: null,
  dateRange: null,
})

const page = reactive({ current: 1, total: 0 })
const orders = ref([])
const loading = ref(false)

const statusMap = { 1: '新建', 2: '已支付', 3: '已回调', 4: '已退款', 5: '已失效', 6: '支付失败' }
const statusClassMap = { 1: 'tag-blue', 2: 'tag-blue', 3: 'tag-green', 4: 'tag-red', 5: 'tag-gray', 6: 'tag-orange' }

function statusClass(status) { return statusClassMap[status] || 'tag-gray' }

async function fetchOrders() {
  loading.value = true
  try {
    const params = { page: page.current, size: 10 }
    if (search.orderNo) params.orderNo = search.orderNo
    if (search.amount) params.orderAmount = search.amount
    if (search.status) params.orderStatus = search.status
    if (search.productName) params.productName = search.productName
    if (search.dateRange) {
      // Format Date objects to ISO strings for backend LocalDateTime parsing
      const fmt = d => d.toISOString().replace('T', ' ').substring(0, 19)
      params.startTime = fmt(search.dateRange[0])
      params.endTime = fmt(search.dateRange[1])
    }
    const result = await orderApi.getOrderList(params)
    orders.value = (result?.records || []).map(o => ({
      ...o,
      amount: `¥${Number(o.orderAmount || 0).toFixed(2)}`,
      statusText: statusMap[o.orderStatus] || '未知',
    }))
    page.total = result?.total || 0
  } catch (e) {
    console.warn('Failed to load orders', e)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.current = 1
  fetchOrders()
}
function handleReset() {
  Object.assign(search, { orderNo: '', amount: '', productName: '', status: null, dateRange: null })
  page.current = 1
  fetchOrders()
}

onMounted(() => fetchOrders())
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
