<template>
  <div>
    <div class="page-title">商户管理</div>
    <div class="card">
      <div class="search-bar">
        <el-input v-model="search.merchantName" placeholder="商户名称" style="width:180px" clearable />
        <el-input v-model="search.phone" placeholder="手机号" style="width:160px" clearable />
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button type="success" @click="openCreate">+ 新增商户</el-button>
      </div>

      <el-table :data="merchants" stripe style="width:100%" v-loading="loading">
        <el-table-column prop="merchantNo" label="商户号" width="150" />
        <el-table-column prop="merchantName" label="商户名称" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="alipayAccount" label="支付宝账号" />
        <el-table-column prop="referralCode" label="推荐码" width="100" />
        <el-table-column prop="createdAt" label="注册时间" width="160" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <span class="tag" :class="row.status === 1 ? 'tag-green' : 'tag-gray'" style="cursor:pointer" @click="toggleStatus(row)">
              {{ row.status === 1 ? '启用' : '停用' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="开放API" width="90">
          <template #default="{ row }">
            <span class="tag" :class="row.apiEnabled === 1 ? 'tag-green' : 'tag-gray'" style="cursor:pointer" @click="toggleApi(row)">
              {{ row.apiEnabled === 1 ? '已开通' : '未开通' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="金额浮动" width="90">
          <template #default="{ row }">
            <span class="tag" :class="row.floatEnabled === 1 ? 'tag-green' : 'tag-gray'" style="cursor:pointer" @click="toggleFloat(row)">
              {{ row.floatEnabled === 1 ? '浮动' : '不浮动' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="270">
          <template #default="{ row }">
            <a href="#" @click.prevent="openEdit(row)">编辑</a>
            <span style="margin:0 4px;color:#ddd">|</span>
            <a href="#" @click.prevent="previewQrcode(row)">码牌预览</a>
            <span style="margin:0 4px;color:#ddd">|</span>
            <a href="#" @click.prevent="resetSecret(row)">重置密钥</a>
            <span style="margin:0 4px;color:#ddd">|</span>
            <a href="#" class="danger-link" @click.prevent="removeMerchant(row)">删除</a>
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
          @current-change="fetchMerchants"
        />
      </div>
    </div>

    <MerchantModal v-model:visible="merchantModalVisible" :merchant="editingMerchant" @saved="fetchMerchants" />

    <!-- 码牌预览弹窗 -->
    <el-dialog v-model="qrcodeVisible" title="码牌预览" width="480px" center>
      <div style="text-align:center;padding:12px">
        <div v-if="qrcodeLoading" style="padding:40px;color:#999">加载中...</div>
        <div v-else-if="!qrcodeImage" style="padding:40px;color:#999">该商户尚无可用码牌</div>
        <div v-else>
          <img :src="qrcodeImage" style="width:320px;height:auto;border-radius:12px;box-shadow:0 2px 12px rgba(0,0,0,0.1)" />
          <div style="margin-top:12px;font-size:13px;color:#666">
            商户：{{ qrcodeMerchantName }}<br/>
            码牌编号：{{ qrcodeNo }}
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 重置密钥弹窗 -->
    <el-dialog v-model="secretVisible" title="重置API密钥" width="520px" center>
      <div style="padding:8px 0">
        <el-alert type="warning" :closable="false" show-icon title="请立即保存，密钥仅展示一次" />
        <p style="margin:16px 0 8px;color:#666;font-size:13px">商户：{{ secretMerchantName }}</p>
        <div class="secret-box">{{ newSecret }}</div>
      </div>
      <template #footer>
        <el-button type="primary" @click="secretVisible = false">我已保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMerchantList, toggleMerchantStatus, getMerchantQrcode, resetApiSecret, updateApiConfig, deleteMerchant } from '@/api/merchant'
import MerchantModal from '@/components/MerchantModal.vue'

const search = reactive({ merchantName: '', phone: '' })
const page = reactive({ current: 1, size: 10, total: 0 })
const merchants = ref([])
const loading = ref(false)
const merchantModalVisible = ref(false)
const editingMerchant = ref(null)

// 码牌预览
const qrcodeVisible = ref(false)
const qrcodeLoading = ref(false)
const qrcodeImage = ref('')
const qrcodeNo = ref('')
const qrcodeMerchantName = ref('')

// 重置API密钥
const secretVisible = ref(false)
const newSecret = ref('')
const secretMerchantName = ref('')

async function previewQrcode(row) {
  qrcodeVisible.value = true
  qrcodeLoading.value = true
  qrcodeImage.value = ''
  qrcodeNo.value = ''
  qrcodeMerchantName.value = row.merchantName
  try {
    const qr = await getMerchantQrcode(row.id)
    if (qr && qr.qrcodeImage) {
      qrcodeImage.value = qr.qrcodeImage
      qrcodeNo.value = qr.qrcodeNo
    }
  } catch (e) {
    console.warn('Failed to load qrcode', e)
  } finally {
    qrcodeLoading.value = false
  }
}

async function fetchMerchants() {
  loading.value = true
  try {
    const params = { page: page.current, size: page.size }
    if (search.merchantName) params.merchantName = search.merchantName
    if (search.phone) params.phone = search.phone
    const result = await getMerchantList(params)
    merchants.value = result?.records || []
    page.total = result?.total || 0
  } catch (e) {
    console.warn('Failed to load merchants', e)
  } finally {
    loading.value = false
  }
}

function handleSearch() { page.current = 1; fetchMerchants() }

async function toggleStatus(row) {
  const newStatus = row.status === 1 ? 2 : 1
  const actionText = newStatus === 2 ? '停用' : '启用'
  try {
    await ElMessageBox.confirm(`确定要${actionText}商户「${row.merchantName}」吗？`, '确认操作', { type: 'warning' })
    await toggleMerchantStatus(row.id, newStatus)
    ElMessage.success(`${actionText}成功`)
    fetchMerchants()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败')
  }
}

function openCreate() {
  editingMerchant.value = null
  merchantModalVisible.value = true
}

function openEdit(row) {
  editingMerchant.value = row
  merchantModalVisible.value = true
}

async function resetSecret(row) {
  try {
    await ElMessageBox.confirm(
      `确定要重置商户「${row.merchantName}」的API签名密钥吗？重置后原密钥将立即失效。`,
      '确认重置',
      { type: 'warning' }
    )
  } catch (e) {
    return
  }
  try {
    const secret = await resetApiSecret(row.id)
    newSecret.value = secret
    secretMerchantName.value = row.merchantName
    secretVisible.value = true
  } catch (e) {
    // 错误已由拦截器提示
  }
}

async function toggleApi(row) {
  const newVal = row.apiEnabled === 1 ? 0 : 1
  try {
    await updateApiConfig(row.id, { apiEnabled: newVal })
    row.apiEnabled = newVal
    ElMessage.success(newVal === 1 ? '已开通开放API' : '已关闭开放API')
  } catch (e) {
    // 错误已由拦截器提示
  }
}

async function toggleFloat(row) {
  const newVal = row.floatEnabled === 1 ? 0 : 1
  try {
    await updateApiConfig(row.id, { floatEnabled: newVal })
    row.floatEnabled = newVal
    ElMessage.success(newVal === 1 ? '已开启金额浮动' : '已关闭金额浮动')
  } catch (e) {
    // 错误已由拦截器提示
  }
}

async function removeMerchant(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除商户「${row.merchantName}」吗？将同时删除其关联的订单、码牌、佣金、提现及登录账号等数据，且不可恢复。`,
      '确认删除',
      { type: 'warning', confirmButtonText: '删除', confirmButtonClass: 'el-button--danger' }
    )
  } catch (e) {
    return
  }
  try {
    await deleteMerchant(row.id)
    ElMessage.success('删除成功')
    fetchMerchants()
  } catch (e) {
    // 错误已由拦截器提示
  }
}

onMounted(() => fetchMerchants())
</script>

<style scoped>
.pagination { display: flex; justify-content: flex-end; align-items: center; gap: 8px; margin-top: 16px; }
.page-info { font-size: 13px; color: #999; }
.secret-box { font-family: monospace; word-break: break-all; background: #f5f7fa; border: 1px dashed #c0c4cc; border-radius: 6px; padding: 12px; font-size: 14px; color: #303133; user-select: all; }
.danger-link { color: #f56c6c; }
</style>
