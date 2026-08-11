<template>
  <div>
    <div class="page-title">订单管理</div>
    <div class="card">
      <div class="search-bar">
        <el-input v-model="search.orderNo" placeholder="订单号" style="width:160px" clearable />
        <el-input v-model="search.productName" placeholder="产品名称" style="width:160px" clearable />
        <el-select v-model="search.orderStatus" placeholder="订单状态" style="width:130px" clearable>
          <el-option label="新建" :value="1" />
          <el-option label="已支付" :value="2" />
          <el-option label="已回调" :value="3" />
          <el-option label="已退款" :value="4" />
          <el-option label="已失效" :value="5" />
          <el-option label="支付失败" :value="6" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>

      <el-table :data="orders" stripe style="width:100%" v-loading="loading">
        <el-table-column prop="orderNo" label="订单号" width="180" />
        <el-table-column prop="merchantNo" label="商户号" width="140" />
        <el-table-column prop="productName" label="产品名称" />
        <el-table-column label="订单金额">
          <template #default="{ row }">¥{{ fmt(row.orderAmount) }}</template>
        </el-table-column>
        <el-table-column prop="payTime" label="支付时间" width="160" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <span class="tag" :class="statusClass(row.orderStatus)">{{ statusText(row.orderStatus) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <a href="#" @click.prevent="viewDetail(row)">详情</a>
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
          @current-change="fetchOrders"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import * as orderApi from '@/api/order'

const search = reactive({
  orderNo: '',
  productName: '',
  orderStatus: null,
})

const page = reactive({ current: 1, size: 10, total: 0 })
const orders = ref([])
const loading = ref(false)

const statusMap = { 1: '新建', 2: '已支付', 3: '已回调', 4: '已退款', 5: '已失效', 6: '支付失败' }
const statusClassMap = { 1: 'tag-blue', 2: 'tag-blue', 3: 'tag-green', 4: 'tag-red', 5: 'tag-gray', 6: 'tag-orange' }

function statusText(s) { return statusMap[s] || '未知' }
function statusClass(s) { return statusClassMap[s] || 'tag-gray' }
function fmt(v) { return v != null ? Number(v).toFixed(2) : '0.00' }

async function fetchOrders() {
  loading.value = true
  try {
    const params = { page: page.current, size: page.size }
    if (search.orderNo) params.orderNo = search.orderNo
    if (search.productName) params.productName = search.productName
    if (search.orderStatus) params.orderStatus = search.orderStatus
    const result = await orderApi.getOrderList(params)
    orders.value = result?.records || []
    page.total = result?.total || 0
  } catch (e) {
    console.warn('Failed to load orders', e)
  } finally {
    loading.value = false
  }
}

function handleSearch() { page.current = 1; fetchOrders() }
function handleReset() {
  Object.assign(search, { orderNo: '', productName: '', orderStatus: null })
  page.current = 1
  fetchOrders()
}
function viewDetail(row) {
  ElMessage.info('订单详情: ' + row.orderNo)
}

onMounted(() => fetchOrders())
</script>

<style scoped>
.pagination { display: flex; justify-content: flex-end; align-items: center; gap: 8px; margin-top: 16px; }
.page-info { font-size: 13px; color: #999; }
</style>
