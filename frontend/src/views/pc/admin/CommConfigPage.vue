<template>
  <div>
    <div class="page-title">返佣配置</div>
    <div class="card">
      <div class="card-header">区间列表</div>
      <el-table :data="intervals" stripe v-loading="loading">
        <el-table-column label="序号" width="70">
          <template #default="{ $index }">{{ $index + 1 }}</template>
        </el-table-column>
        <el-table-column label="最小金额（元）">
          <template #default="{ row }">{{ fmt(row.minAmount) }}</template>
        </el-table-column>
        <el-table-column label="最大金额（元）">
          <template #default="{ row }">{{ fmt(row.maxAmount) }}</template>
        </el-table-column>
        <el-table-column label="返佣比例">
          <template #default="{ row }">{{ row.commRate != null ? (row.commRate * 100).toFixed(1) + '%' : '-' }}</template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="70" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <span class="tag" :class="row.status === 1 ? 'tag-green' : 'tag-gray'">
              {{ row.status === 1 ? '启用' : '停用' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button size="small" @click="edit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top:16px">
        <el-button type="primary" @click="openAdd">+ 新增区间</el-button>
      </div>

      <div class="warning-note">
        ⚠️ 修改配置仅对新生成的佣金生效，已有佣金不受影响。区间必须完整覆盖且不可重叠。
      </div>
    </div>

    <!-- Add/Edit Modal -->
    <el-dialog
      v-model="modalVisible"
      :title="editingId ? '编辑返佣区间' : '新增返佣区间'"
      width="480px"
      @closed="resetForm"
    >
      <el-form :model="form" label-width="130px">
        <el-form-item label="最小金额（元）*">
          <el-input v-model="form.minAmount" placeholder="0.00" type="number" />
        </el-form-item>
        <el-form-item label="最大金额（元）*">
          <el-input v-model="form.maxAmount" placeholder="100.00" type="number" />
        </el-form-item>
        <el-form-item label="返佣比例 *">
          <el-input v-model="form.commRate" placeholder="如 0.05 表示 5%">
            <template #suffix><span style="color:#999">= {{ fmtRate(form.commRate) }}</span></template>
          </el-input>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="2">停用</el-radio>
          </el-radio-group>
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
import { getCommConfigList, addCommConfig, updateCommConfig, deleteCommConfig } from '@/api/admin'

const DEFAULT_FORM = {
  minAmount: '0.00',
  maxAmount: '',
  commRate: '',
  sortOrder: 0,
  status: 1,
}

const form = reactive({ ...DEFAULT_FORM })
const intervals = ref([])
const loading = ref(false)
const submitting = ref(false)
const modalVisible = ref(false)
const editingId = ref(null)

function fmt(v) { return v != null ? Number(v).toFixed(2) : '-' }
function fmtRate(v) {
  if (v == null || v === '') return '-'
  return (Number(v) * 100).toFixed(1) + '%'
}

function resetForm() {
  editingId.value = null
  Object.assign(form, DEFAULT_FORM)
}

async function fetchConfigs() {
  loading.value = true
  try {
    const result = await getCommConfigList()
    intervals.value = Array.isArray(result) ? result : result?.records || []
  } catch (e) {
    console.warn('Failed to load commission configs', e)
  } finally {
    loading.value = false
  }
}

function openAdd() { resetForm(); modalVisible.value = true }

function edit(row) {
  editingId.value = row.id
  Object.assign(form, {
    minAmount: row.minAmount ?? '0.00',
    maxAmount: row.maxAmount ?? '',
    commRate: row.commRate ?? '',
    sortOrder: row.sortOrder ?? 0,
    status: row.status ?? 1,
  })
  modalVisible.value = true
}

async function submit() {
  if (form.minAmount == null || form.minAmount === '') { ElMessage.warning('请输入最小金额'); return }
  if (form.maxAmount == null || form.maxAmount === '') { ElMessage.warning('请输入最大金额'); return }
  if (form.commRate == null || form.commRate === '') { ElMessage.warning('请输入返佣比例'); return }

  submitting.value = true
  try {
    const data = {
      minAmount: Number(form.minAmount),
      maxAmount: Number(form.maxAmount),
      commRate: Number(form.commRate),
      sortOrder: form.sortOrder,
      status: form.status,
    }
    if (editingId.value) {
      await updateCommConfig(editingId.value, data)
      ElMessage.success('已更新')
    } else {
      await addCommConfig(data)
      ElMessage.success('已添加')
    }
    modalVisible.value = false
    fetchConfigs()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除 [¥${fmt(row.minAmount)} ~ ¥${fmt(row.maxAmount)}] 的返佣区间吗？`,
      '确认删除',
      { type: 'warning' }
    )
    await deleteCommConfig(row.id)
    ElMessage.success('已删除')
    fetchConfigs()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '删除失败')
  }
}

onMounted(() => fetchConfigs())
</script>

<style scoped>
.warning-note {
  margin-top: 12px;
  padding: 10px;
  background: #fffbe6;
  border-radius: 6px;
  font-size: 12px;
  color: #faad14;
}
</style>
