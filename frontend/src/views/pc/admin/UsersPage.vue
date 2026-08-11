<template>
  <div>
    <div class="page-title">用户管理</div>
    <div class="card">
      <div class="search-bar">
        <el-input v-model="search.keyword" placeholder="用户名/手机号" style="width:200px" clearable @keyup.enter="handleSearch" />
        <el-select v-model="search.role" placeholder="角色" style="width:140px" clearable @change="handleSearch">
          <el-option label="管理员" :value="1" />
          <el-option label="商户用户" :value="2" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button type="success" @click="openAdd">+ 新增用户</el-button>
      </div>

      <el-table :data="users" stripe style="width:100%" v-loading="loading">
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="角色" width="90">
          <template #default="{ row }">
            <span class="tag" :class="row.role === 1 ? 'tag-red' : 'tag-blue'">
              {{ row.role === 1 ? '管理员' : '商户用户' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="merchantName" label="关联商户" />
        <el-table-column prop="lastLoginTime" label="最后登录" width="160" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <span class="tag" :class="row.status === 1 ? 'tag-green' : 'tag-gray'">
              {{ row.status === 1 ? '正常' : '停用' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <a href="#" @click.prevent="edit(row)">编辑</a>
            <a href="#" @click.prevent="toggle(row)"
              style="margin-left:8px"
              :style="{ color: row.status === 1 ? 'var(--danger)' : 'var(--success)' }">
              {{ row.status === 1 ? '停用' : '启用' }}
            </a>
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
          @current-change="fetchUsers"
        />
      </div>
    </div>

    <!-- Add/Edit Modal -->
    <el-dialog
      v-model="modalVisible"
      :title="editingId ? '编辑用户' : '新增用户'"
      width="480px"
      @closed="resetForm"
    >
      <el-form :model="form" label-width="120px">
        <el-form-item label="用户名 *" v-if="!editingId">
          <el-input v-model="form.username" placeholder="登录用户名" />
        </el-form-item>
        <el-form-item label="手机号 *">
          <el-input v-model="form.phone" placeholder="11 位手机号" />
        </el-form-item>
        <el-form-item label="角色 *">
          <el-radio-group v-model="form.role" :disabled="!!editingId">
            <el-radio :value="1">管理员</el-radio>
            <el-radio :value="2">商户用户</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="关联商户" v-if="form.role === 2">
          <el-select v-model="form.merchantId" placeholder="选择商户" filterable clearable style="width:100%">
            <el-option
              v-for="m in merchants"
              :key="m.id"
              :label="`${m.merchantName} (${m.merchantNo})`"
              :value="m.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="密码" :label-width="editingId ? '新密码' : '120px'">
          <el-input v-model="form.password" type="password" show-password
            :placeholder="editingId ? '留空则不修改密码' : '请输入密码'" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="modalVisible = false">取消</el-button>
        <el-button type="primary" @click="submit" :loading="submitting">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserList, createUser, updateUser, toggleUserStatus } from '@/api/admin'
import { getMerchantList } from '@/api/merchant'

const DEFAULT_FORM = {
  username: '',
  phone: '',
  role: 1,
  merchantId: '',
  password: '',
}

const form = reactive({ ...DEFAULT_FORM })
const search = reactive({ keyword: '', role: null })
const page = reactive({ current: 1, size: 10, total: 0 })
const users = ref([])
const merchants = ref([])
const loading = ref(false)
const submitting = ref(false)
const modalVisible = ref(false)
const editingId = ref(null)

function resetForm() {
  editingId.value = null
  Object.assign(form, DEFAULT_FORM)
}

async function fetchUsers() {
  loading.value = true
  try {
    const params = { page: page.current, size: page.size }
    if (search.keyword) params.keyword = search.keyword
    if (search.role) params.role = search.role
    const result = await getUserList(params)
    users.value = result?.records || []
    page.total = result?.total || 0
  } catch (e) {
    console.warn('Failed to load users', e)
  } finally {
    loading.value = false
  }
}

function handleSearch() { page.current = 1; fetchUsers() }

async function fetchMerchants() {
  try {
    // Load all enabled merchants for the dropdown
    const result = await getMerchantList({ page: 1, size: 9999 })
    merchants.value = result?.records || []
  } catch (e) {
    console.warn('Failed to load merchants for dropdown', e)
  }
}

function openAdd() { resetForm(); modalVisible.value = true }

function edit(row) {
  editingId.value = row.id
  Object.assign(form, {
    username: row.username || '',
    phone: row.phone || '',
    role: row.role ?? 1,
    merchantId: row.merchantId || '',
    password: '',
  })
  modalVisible.value = true
}

async function submit() {
  if (!form.phone) { ElMessage.warning('请输入手机号'); return }
  if (!editingId.value && !form.username) { ElMessage.warning('请输入用户名'); return }
  if (!editingId.value && !form.password) { ElMessage.warning('请输入密码'); return }

  submitting.value = true
  try {
    const data = {
      phone: form.phone,
      role: Number(form.role),
      merchantId: form.merchantId || null,
    }
    if (!editingId.value) data.username = form.username
    if (form.password) data.password = form.password

    if (editingId.value) {
      await updateUser(editingId.value, data)
      ElMessage.success('已更新')
    } else {
      await createUser(data)
      ElMessage.success('用户已创建')
    }
    modalVisible.value = false
    fetchUsers()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

async function toggle(row) {
  const newStatus = row.status === 1 ? 2 : 1
  const actionText = newStatus === 2 ? '停用' : '启用'
  try {
    await ElMessageBox.confirm(`确定要${actionText}用户「${row.username}」吗？`, '确认操作', { type: 'warning' })
    await toggleUserStatus(row.id, newStatus)
    ElMessage.success(`${actionText}成功`)
    fetchUsers()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败')
  }
}

onMounted(() => { fetchUsers(); fetchMerchants() })
</script>

<style scoped>
.pagination { display: flex; justify-content: flex-end; align-items: center; gap: 8px; margin-top: 16px; }
.page-info { font-size: 13px; color: #999; }
</style>
