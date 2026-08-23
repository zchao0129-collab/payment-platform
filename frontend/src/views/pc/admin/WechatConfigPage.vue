<template>
  <div v-loading="loading">
    <div class="page-title">微信支付配置</div>

    <!-- Existing configs list -->
    <div class="card" v-if="configs.length">
      <div class="card-header">已保存的配置 ({{ configs.length }})</div>
      <el-table :data="configs" stripe>
        <el-table-column prop="configName" label="配置名称" />
        <el-table-column prop="appId" label="AppID" />
        <el-table-column prop="mchId" label="商户号" />
        <el-table-column prop="serialNo" label="证书序列号" width="160" />
        <el-table-column prop="weight" label="权重" width="80" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <span class="tag" :class="row.status === 1 ? 'tag-green' : 'tag-gray'" style="cursor:pointer" @click="toggle(row)">
              {{ row.status === 1 ? '启用' : '停用' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="lastTestTime" label="最后测试" width="150" />
        <el-table-column label="测试结果" width="90">
          <template #default="{ row }">
            <span :style="{ color: row.lastTestResult === 1 ? 'var(--success)' : row.lastTestResult ? 'var(--danger)' : '#999' }">
              {{ row.lastTestResult === 1 ? '成功' : row.lastTestResult === 2 ? '失败' : '未测试' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button size="small" @click="testConnection(row)">测试</el-button>
            <el-button size="small" @click="editConfig(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- Add/Edit form -->
    <div class="card" style="margin-top:16px">
      <div class="card-header">{{ editingId ? '编辑配置' : '新增配置' }}</div>
      <el-form :model="form" label-width="140px" class="config-form">
        <el-form-item label="配置名称 *">
          <el-input v-model="form.configName" placeholder="用于区分不同微信支付配置" />
        </el-form-item>
        <el-form-item label="AppID *">
          <el-input v-model="form.appId" placeholder="微信公众号/小程序 AppID" />
        </el-form-item>
        <el-form-item label="商户号 *">
          <el-input v-model="form.mchId" placeholder="微信支付商户号" />
        </el-form-item>
        <el-form-item label="APIv3 密钥 *">
          <el-input v-model="form.apiV3Key" placeholder="微信支付 APIv3 密钥（32位）" show-password />
        </el-form-item>
        <el-form-item label="证书序列号 *">
          <el-input v-model="form.serialNo" placeholder="商户API证书序列号" />
        </el-form-item>
        <el-form-item label="商户私钥 *">
          <el-input v-model="form.privateKey" type="textarea" :rows="5"
            placeholder="商户私钥 PEM 内容（含 -----BEGIN PRIVATE KEY----- 头尾标记）" />
        </el-form-item>

        <el-form-item label="权重">
          <el-input-number v-model="form.weight" :min="0" :max="1000" :step="10" />
          <span class="form-tip">权重越高被选中概率越大，0=不使用，多个启用配置间按权重比例分流</span>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="save" :loading="saving">
            {{ editingId ? '更新配置' : '保存配置' }}
          </el-button>
          <el-button v-if="editingId" @click="cancelEdit">取消编辑</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listWechatConfigs,
  saveWechatConfig,
  deleteWechatConfig,
  toggleWechatConfigStatus,
  testWechatConnection,
} from '@/api/wechat'

const DEFAULT_FORM = {
  configName: '',
  appId: '',
  mchId: '',
  apiV3Key: '',
  serialNo: '',
  privateKey: '',
  weight: 100,
}

const form = reactive({ ...DEFAULT_FORM })
const configs = ref([])
const loading = ref(false)
const saving = ref(false)
const editingId = ref(null)

async function fetchConfigs() {
  loading.value = true
  try {
    const result = await listWechatConfigs()
    configs.value = Array.isArray(result) ? result : result?.records || []
  } catch (e) {
    console.warn('Failed to load wechat configs', e)
  } finally {
    loading.value = false
  }
}

function editConfig(row) {
  editingId.value = row.id
  Object.assign(form, {
    configName: row.configName || '',
    appId: row.appId || '',
    mchId: row.mchId || '',
    apiV3Key: row.apiV3Key || '',
    serialNo: row.serialNo || '',
    privateKey: row.privateKey || '',
    weight: row.weight != null ? row.weight : 100,
  })
}

function cancelEdit() {
  editingId.value = null
  Object.assign(form, DEFAULT_FORM)
}

async function save() {
  if (!form.configName || !form.appId || !form.mchId || !form.apiV3Key || !form.serialNo) {
    ElMessage.warning('请填写配置名称、AppID、商户号、APIv3密钥、证书序列号')
    return
  }
  saving.value = true
  try {
    const data = { ...form }
    if (editingId.value) data.id = editingId.value
    await saveWechatConfig(data)
    ElMessage.success(editingId.value ? '配置已更新' : '配置已保存')
    cancelEdit()
    fetchConfigs()
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function toggle(row) {
  const newStatus = row.status === 1 ? 2 : 1
  const actionText = newStatus === 1 ? '启用' : '停用'
  const tip = newStatus === 1 ? `启用「${row.configName}」后，其他配置将被停用。` : `确定停用配置「${row.configName}」吗？`
  try {
    await ElMessageBox.confirm(tip, `确认${actionText}`, { type: 'warning' })
    await toggleWechatConfigStatus(row.id, newStatus)
    ElMessage.success(`已${actionText}`)
    fetchConfigs()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败')
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确定删除配置「${row.configName}」吗？此操作不可恢复。`, '确认删除', { type: 'warning' })
    await deleteWechatConfig(row.id)
    ElMessage.success('已删除')
    fetchConfigs()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败')
  }
}

async function testConnection(row) {
  try {
    await testWechatConnection(row.id)
    ElMessage.success('连通性测试通过')
    fetchConfigs()
  } catch (e) {
    ElMessage.error(e.message || '连通性测试失败')
  }
}

onMounted(() => fetchConfigs())
</script>

<style scoped>
.config-form { max-width: 700px; }
</style>
