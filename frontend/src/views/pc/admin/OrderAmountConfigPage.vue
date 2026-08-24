<template>
  <div>
    <div class="page-title">金额浮动配置</div>
    <div class="card">
      <div class="card-header">开放API订单金额浮动</div>
      <el-form :model="form" label-width="160px" style="max-width:560px">
        <el-form-item label="启用浮动">
          <el-radio-group v-model="form.enabled">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="2">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="最小浮动金额（元）">
          <el-input-number v-model="form.minFloat" :min="0" :max="100" :precision="2" :step="0.01" />
        </el-form-item>
        <el-form-item label="最大浮动金额（元）">
          <el-input-number v-model="form.maxFloat" :min="0" :max="100" :precision="2" :step="0.01" />
        </el-form-item>
        <el-form-item label="浮动方向">
          <el-radio-group v-model="form.floatDirection">
            <el-radio value="BOTH">上下随机</el-radio>
            <el-radio value="UP">只上浮</el-radio>
            <el-radio value="DOWN">只下浮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="浮动判定主从">
          <el-radio-group v-model="form.judgeMode">
            <el-radio value="MERCHANT">商户为主</el-radio>
            <el-radio value="URL">跳转/回调地址为主</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.judgeMode === 'URL'" label="跳转/回调地址关键字">
          <el-input v-model="form.floatUrlKeywords" type="textarea" :rows="2" placeholder="域名或关键字，逗号分隔；returnUrl/notifyUrl 命中任一即浮动" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
        </el-form-item>
      </el-form>
      <div class="warning-note">
        ⚠️ 仅对「开放API」创建的订单生效：订单金额会在原始金额基础上随机上浮或下浮
        [{{ form.minFloat }} ~ {{ form.maxFloat }}] 元。是否浮动由「判定主从」决定——商户为主时按商户开关判断，
        跳转/回调地址为主时按关键字命中判断。收银台/码牌下单不受影响。
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getOrderAmountConfig, saveOrderAmountConfig } from '@/api/admin'

const form = reactive({
  enabled: 1,
  minFloat: 0.01,
  maxFloat: 0.09,
  floatDirection: 'BOTH',
  judgeMode: 'MERCHANT',
  floatUrlKeywords: '',
})
const saving = ref(false)

async function fetchConfig() {
  try {
    const result = await getOrderAmountConfig()
    if (result) {
      form.enabled = result.enabled ?? 1
      form.minFloat = Number(result.minFloat ?? 0.01)
      form.maxFloat = Number(result.maxFloat ?? 0.09)
      form.floatDirection = result.floatDirection || 'BOTH'
      form.judgeMode = result.judgeMode || 'MERCHANT'
      form.floatUrlKeywords = result.floatUrlKeywords || ''
    }
  } catch (e) {
    console.warn('Failed to load order amount config', e)
  }
}

async function submit() {
  if (Number(form.maxFloat) < Number(form.minFloat)) {
    ElMessage.warning('最大浮动金额不能小于最小浮动金额')
    return
  }
  saving.value = true
  try {
    await saveOrderAmountConfig({
      enabled: form.enabled,
      minFloat: Number(form.minFloat),
      maxFloat: Number(form.maxFloat),
      floatDirection: form.floatDirection,
      judgeMode: form.judgeMode,
      floatUrlKeywords: form.floatUrlKeywords,
    })
    ElMessage.success('保存成功')
  } catch (e) {
    // 错误已由拦截器提示
  } finally {
    saving.value = false
  }
}

onMounted(() => fetchConfig())
</script>

<style scoped>
.warning-note {
  margin-top: 12px;
  padding: 10px;
  background: #fffbe6;
  border-radius: 6px;
  font-size: 12px;
  color: #faad14;
  max-width: 560px;
}
</style>
