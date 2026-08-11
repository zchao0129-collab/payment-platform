<template>
  <div v-loading="loading">
    <div class="page-title">支付宝配置</div>

    <!-- Existing configs list -->
    <div class="card" v-if="configs.length">
      <div class="card-header">已保存的配置 ({{ configs.length }})</div>
      <el-table :data="configs" stripe>
        <el-table-column prop="configName" label="配置名称" />
        <el-table-column prop="configType" label="类型" width="90">
          <template #default="{ row }">{{ row.configType === 2 ? '秘钥' : '证书' }}</template>
        </el-table-column>
        <el-table-column prop="appId" label="AppID" />
        <el-table-column prop="uid" label="商户UID" />
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
            <span :style="{ color: row.lastTestResult === 'SUCCESS' ? 'var(--success)' : row.lastTestResult ? 'var(--danger)' : '#999' }">
              {{ row.lastTestResult || '未测试' }}
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
          <el-input v-model="form.configName" placeholder="用于区分不同支付宝配置" />
        </el-form-item>
        <el-form-item label="配置类型">
          <el-radio-group v-model="form.configType" :disabled="!!editingId">
            <el-radio value="key">秘钥模式</el-radio>
            <el-radio value="cert">证书模式</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="AppID *">
          <el-input v-model="form.appId" placeholder="支付宝开放平台应用ID" />
        </el-form-item>
        <el-form-item label="商户UID *">
          <el-input v-model="form.uid" placeholder="支付宝商户UID/合作伙伴ID" />
        </el-form-item>

        <template v-if="form.configType === 'key'">
          <el-form-item label="应用公钥">
            <el-input v-model="form.appPublicKey" type="textarea" :rows="3" placeholder="请输入应用公钥" />
          </el-form-item>
		  <el-form-item label="应用私钥">
		    <el-input v-model="form.privateKey" type="textarea" :rows="3" placeholder="请输入应用私钥" />
		  </el-form-item>
		  <el-form-item label="支付宝公钥">
		    <el-input v-model="form.alipayPublicKey" type="textarea" :rows="3" placeholder="请输入支付宝公钥" />
		  </el-form-item>
        </template>

        <template v-if="form.configType === 'cert'">
			<el-form-item label="支付宝私钥">
			  <el-input v-model="form.privateKey" type="textarea" :rows="3" placeholder="请输入支付宝商户私钥" />
			</el-form-item>
          <el-form-item label="应用证书">
            <div class="cert-field">
              <el-input v-model="form.appCertPath" placeholder="应用公钥证书文件路径 (.crt/.cer/.pem)" readonly />
              <el-upload
                :show-file-list="false"
                :http-request="(opts) => handleUpload(opts, 'appCertPath')"
                accept=".crt,.cer,.pem"
              >
                <el-button :loading="uploading === 'appCertPath'">📎 上传</el-button>
              </el-upload>
            </div>
          </el-form-item>
          <el-form-item label="支付宝根证书">
            <div class="cert-field">
              <el-input v-model="form.rootCertPath" placeholder="支付宝根证书文件路径 (.crt/.cer/.pem)" readonly />
              <el-upload
                :show-file-list="false"
                :http-request="(opts) => handleUpload(opts, 'rootCertPath')"
                accept=".crt,.cer,.pem"
              >
                <el-button :loading="uploading === 'rootCertPath'">📎 上传</el-button>
              </el-upload>
            </div>
          </el-form-item>
          <el-form-item label="支付宝公钥证书">
            <div class="cert-field">
              <el-input v-model="form.publicCertPath" placeholder="支付宝公钥证书文件路径 (.crt/.cer/.pem)" readonly />
              <el-upload
                :show-file-list="false"
                :http-request="(opts) => handleUpload(opts, 'publicCertPath')"
                accept=".crt,.cer,.pem"
              >
                <el-button :loading="uploading === 'publicCertPath'">📎 上传</el-button>
              </el-upload>
            </div>
          </el-form-item>
        </template>

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
  getAlipayConfigList,
  saveAlipayConfig,
  deleteAlipayConfig,
  toggleAlipayConfigStatus,
  testAlipayConnection,
  uploadCertFile,
} from '@/api/admin'

const DEFAULT_FORM = {
  configName: '',
  configType: 'key',
  appId: '',
  uid: '',
  privateKey: '',
  alipayPublicKey: '',
  appPublicKey: '',
  appCertPath: '',
  rootCertPath: '',
  publicCertPath: '',
  weight: 100,
}

const form = reactive({ ...DEFAULT_FORM })
const configs = ref([])
const loading = ref(false)
const saving = ref(false)
const uploading = ref(null) // track which cert field is uploading
const editingId = ref(null)

async function fetchConfigs() {
  loading.value = true
  try {
    const result = await getAlipayConfigList()
    configs.value = Array.isArray(result) ? result : result?.records || []
  } catch (e) {
    console.warn('Failed to load alipay configs', e)
  } finally {
    loading.value = false
  }
}

function editConfig(row) {
  editingId.value = row.id
  Object.assign(form, {
    configName: row.configName || '',
    configType: row.configType === 1 ? 'cert' : 'key',
    appId: row.appId || '',
    uid: row.uid || '',
    privateKey: row.privateKey || '',
    alipayPublicKey: row.alipayPublicKey || '',
    appPublicKey: row.appPublicKey || '',
    appCertPath: row.appCertPath || '',
    rootCertPath: row.rootCertPath || '',
    publicCertPath: row.publicCertPath || '',
    weight: row.weight != null ? row.weight : 100,
  })
}

function cancelEdit() {
  editingId.value = null
  Object.assign(form, DEFAULT_FORM)
}

async function handleUpload({ file }, fieldKey) {
  uploading.value = fieldKey
  try {
    const uploadedPath = await uploadCertFile(file)
    // Server returns the stored file path
    form[fieldKey] = typeof uploadedPath === 'string' ? uploadedPath : (uploadedPath?.path || uploadedPath?.filePath || '')
    ElMessage.success(`${file.name} 上传成功`)
  } catch (e) {
    ElMessage.error(e.message || '证书上传失败')
  } finally {
    uploading.value = null
  }
}

async function save() {
  if (!form.configName || !form.appId || !form.uid) {
    ElMessage.warning('请填写配置名称、AppID、商户UID')
    return
  }
  saving.value = true
  try {
    const data = { ...form }
    // convert configType string to integer: cert→1, key→2
    data.configType = data.configType === 'cert' ? 1 : 2
    if (editingId.value) data.id = editingId.value
    await saveAlipayConfig(data)
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
    await toggleAlipayConfigStatus(row.id, newStatus)
    ElMessage.success(`已${actionText}`)
    fetchConfigs()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败')
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确定删除配置「${row.configName}」吗？此操作不可恢复。`, '确认删除', { type: 'warning' })
    await deleteAlipayConfig(row.id)
    ElMessage.success('已删除')
    fetchConfigs()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败')
  }
}

async function testConnection(row) {
  try {
    await testAlipayConnection(row.id)
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
.cert-field {
  display: flex;
  gap: 10px;
  align-items: center;
}
.cert-field :deep(.el-input) {
  flex: 1;
}
</style>
