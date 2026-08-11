<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="$emit('update:visible', $event)"
    :title="editData ? '编辑返佣区间' : '新增返佣区间'"
    width="520px"
    destroy-on-close
    @closed="resetForm"
  >
    <el-form :model="form" label-position="top">
      <el-form-item label="最小金额（元）">
        <el-input v-model="form.minAmount" type="number" step="0.01" placeholder="0.00" />
      </el-form-item>
      <el-form-item label="最大金额（元）">
        <el-input v-model="form.maxAmount" type="number" step="0.01" placeholder="100.00" />
      </el-form-item>
      <el-form-item label="返佣比例（%）">
        <el-input v-model="form.rate" type="number" step="0.01" placeholder="3" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="$emit('update:visible', false)">取消</el-button>
      <el-button type="primary" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({ visible: Boolean, editData: Object })
const emit = defineEmits(['update:visible', 'saved'])

const form = reactive({ minAmount: '', maxAmount: '', rate: '' })

watch(() => props.visible, (val) => {
  if (val && props.editData) {
    form.minAmount = props.editData.minAmount || ''
    form.maxAmount = props.editData.maxAmount || ''
    form.rate = props.editData.rate || ''
  }
})

function resetForm() {
  form.minAmount = ''
  form.maxAmount = ''
  form.rate = ''
}

function handleSave() {
  if (!form.minAmount || !form.maxAmount || !form.rate) {
    ElMessage.warning('请填写所有字段')
    return
  }
  ElMessage.success('保存成功')
  emit('saved')
  emit('update:visible', false)
}
</script>
