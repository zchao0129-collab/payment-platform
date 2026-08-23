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

      <div class="toolbar">
        <el-button type="success" @click="openTestDialog">测试订单</el-button>
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
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <a href="#" @click.prevent="viewDetail(row)">详情</a>
            <a href="#" class="op-gap" @click.prevent="triggerCallback(row)">回调</a>
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

    <!-- 测试订单弹框 -->
    <el-dialog v-model="testDialog.visible" title="测试订单" width="520px" :close-on-click-modal="false">
      <el-form label-width="80px">
        <el-form-item label="金额">
          <el-input v-model="testDialog.amount" placeholder="请输入金额，如 1.00" />
        </el-form-item>
        <el-form-item label="支付通道">
          <el-select v-model="testDialog.payChannel" style="width:100%">
            <el-option label="支付宝" value="ALIPAY" />
            <el-option label="微信" value="WECHAT" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="testDialog.payChannel === 'ALIPAY'" label="交易类型">
          <el-select v-model="testDialog.tradeType" style="width:100%">
            <el-option label="手机网站支付 (WAP)" value="WAP" />
            <el-option label="当面付 (F2F)" value="F2F" />
          </el-select>
        </el-form-item>
      </el-form>
      <div class="test-dialog-footer">
        <el-button @click="testDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="testDialog.loading" @click="runTestOrder">测试</el-button>
      </div>

      <div v-if="testDialog.result" class="test-result">
        <div class="result-item">订单号：{{ testDialog.result.orderNo }}</div>
        <div class="result-item">金额：¥{{ fmt(testDialog.result.amount) }}</div>
        <div class="result-item">通道：{{ testDialog.result.payChannel }}</div>
        <div class="result-item">类型：{{ testDialog.result.tradeType === 'F2F' ? '当面付' : '手机网站支付' }}</div>
        <div v-if="testDialog.result.payUrl" class="result-item">
          支付链接：<a :href="testDialog.result.payUrl" target="_blank" rel="noopener">{{ testDialog.result.payUrl }}</a>
        </div>
        <div v-if="testDialog.result.qrCode" class="result-item">
          二维码内容：<span style="word-break:break-all;color:#666">{{ testDialog.result.qrCode }}</span>
        </div>
        <div v-if="testDialog.qrSrc" class="qr-wrap">
          <img :src="testDialog.qrSrc" alt="支付二维码" />
          <div class="qr-hint">{{ testDialog.result.tradeType === 'F2F' ? '顾客用支付宝扫码支付' : '扫码打开支付链接' }}</div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import * as orderApi from '@/api/order'
import * as qrApi from '@/api/qrcode'

const search = reactive({
  orderNo: '',
  productName: '',
  orderStatus: null,
})

const page = reactive({ current: 1, size: 10, total: 0 })
const orders = ref([])
const loading = ref(false)

const testDialog = reactive({
  visible: false,
  amount: '1.00',
  payChannel: 'ALIPAY',
  tradeType: 'WAP',
  loading: false,
  result: null,
  qrSrc: '',
})

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

// ========== 测试订单 ==========
function openTestDialog() {
  testDialog.result = null
  testDialog.qrSrc = ''
  testDialog.visible = true
}

async function runTestOrder() {
  const amount = String(testDialog.amount || '').trim()
  if (!amount) {
    ElMessage.warning('请输入金额')
    return
  }
  if (Number.isNaN(Number(amount)) || Number(amount) <= 0) {
    ElMessage.warning('请输入有效的正数金额')
    return
  }
  testDialog.loading = true
  try {
    const result = await orderApi.adminTestCreate({ amount, payChannel: testDialog.payChannel, tradeType: testDialog.tradeType })
    testDialog.result = result
    const qrContent = result?.payUrl || result?.qrCode
    if (qrContent) {
      try {
        testDialog.qrSrc = await qrApi.encodeQrcode(qrContent)
      } catch (e) {
        testDialog.qrSrc = ''
      }
    } else {
      testDialog.qrSrc = ''
    }
    ElMessage.success('测试订单创建成功')
  } catch (e) {
    // 错误已由拦截器提示
  } finally {
    testDialog.loading = false
  }
}

// ========== 手动回调 ==========
async function triggerCallback(row) {
  try {
    const res = await orderApi.adminTriggerCallback(row.orderNo)
    const ns = res?.notifyStatus
    if (ns === 1) ElMessage.success('回调推送成功')
    else if (ns === 2) ElMessage.warning('回调推送失败（将自动重试）')
    else ElMessage.info('回调未触发（请检查商户回调地址/开放API配置）')
    fetchOrders()
  } catch (e) {
    // 错误已由拦截器提示
  }
}

onMounted(() => fetchOrders())
</script>

<style scoped>
.pagination { display: flex; justify-content: flex-end; align-items: center; gap: 8px; margin-top: 16px; }
.page-info { font-size: 13px; color: #999; }
.toolbar { display: flex; justify-content: flex-end; margin-bottom: 12px; }
.op-gap { margin-left: 12px; }
.test-dialog-footer { display: flex; justify-content: flex-end; gap: 8px; margin-top: 4px; }
.test-result {
  margin-top: 16px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 6px;
  font-size: 13px;
}
.result-item { margin-bottom: 6px; word-break: break-all; }
.result-item a { color: #409eff; }
.qr-wrap { margin-top: 12px; text-align: center; }
.qr-wrap img { width: 180px; height: 180px; border: 1px solid #ebeef5; border-radius: 4px; }
.qr-hint { margin-top: 6px; font-size: 12px; color: #999; }
</style>
