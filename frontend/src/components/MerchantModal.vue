<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="$emit('update:visible', $event)"
    :title="isEdit ? '编辑商户' : '新增商户'"
    width="520px"
    destroy-on-close
  >
    <el-form :model="form" label-position="top">
      <el-form-item label="商户名称 *">
        <el-input v-model="form.merchantName" placeholder="请输入商户名称" />
      </el-form-item>
      <el-form-item label="手机号 *">
        <el-input v-model="form.phone" placeholder="请输入手机号（作为登录账号）" />
      </el-form-item>
      <el-form-item label="支付宝账号 *">
        <el-input v-model="form.alipayAccount" placeholder="请输入支付宝提现账号" />
      </el-form-item>
      <el-form-item label="真实姓名">
        <el-input v-model="form.realName" placeholder="请输入真实姓名" />
      </el-form-item>
      <el-form-item label="身份证号">
        <el-input v-model="form.idCardNo" placeholder="请输入身份证号" />
      </el-form-item>
      <template v-if="isEdit">
        <el-form-item label="支付回调地址">
          <el-input v-model="form.notifyUrl" placeholder="支付成功后回调通知地址(URL)" />
        </el-form-item>
        <el-form-item label="开通开放API">
          <el-switch v-model="form.apiEnabled" :active-value="1" :inactive-value="0" active-text="开通" inactive-text="未开通" />
        </el-form-item>
        <el-form-item label="调用IP白名单">
          <el-input v-model="form.ipWhitelist" type="textarea" :rows="2" placeholder="逗号分隔，留空表示不限制" />
        </el-form-item>
      </template>
      <el-form-item v-if="!isEdit" label="初始密码 *">
        <el-input v-model="form.password" type="password" placeholder="设置初始登录密码" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="$emit('update:visible', false)">取消</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">
        {{ isEdit ? '保存' : '确认创建' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { createMerchant, updateMerchant } from '@/api/merchant'

const props = defineProps({
  visible: Boolean,
  merchant: { type: Object, default: null },
})
const emit = defineEmits(['update:visible', 'saved'])

const loading = ref(false)
const isEdit = computed(() => !!props.merchant)

const emptyForm = () => ({ merchantName: '', phone: '', alipayAccount: '', realName: '', idCardNo: '', password: '', notifyUrl: '', apiEnabled: 0, ipWhitelist: '' })
const form = reactive(emptyForm())

watch(
  () => props.visible,
  (v) => {
    if (!v) return
    Object.assign(form, emptyForm())
    if (props.merchant) {
      form.merchantName = props.merchant.merchantName || ''
      form.phone = props.merchant.phone || ''
      form.alipayAccount = props.merchant.alipayAccount || ''
      form.realName = props.merchant.realName || ''
      form.idCardNo = props.merchant.idCardNo || ''
      form.notifyUrl = props.merchant.notifyUrl || ''
      form.apiEnabled = props.merchant.apiEnabled ?? 0
      form.ipWhitelist = props.merchant.ipWhitelist || ''
    }
  }
)

async function handleConfirm() {
  if (!form.merchantName || !form.phone || !form.alipayAccount) {
    ElMessage.warning('请填写商户名称、手机号、支付宝账号')
    return
  }
  if (!isEdit.value && !form.password) {
    ElMessage.warning('请设置初始密码')
    return
  }
  loading.value = true
  try {
    if (isEdit.value) {
      await updateMerchant(props.merchant.id, {
        merchantName: form.merchantName,
        phone: form.phone,
        alipayAccount: form.alipayAccount,
        realName: form.realName,
        idCardNo: form.idCardNo,
        notifyUrl: form.notifyUrl,
        apiEnabled: form.apiEnabled,
        ipWhitelist: form.ipWhitelist,
      })
      ElMessage.success('保存成功')
    } else {
      await createMerchant({
        merchantName: form.merchantName,
        phone: form.phone,
        alipayAccount: form.alipayAccount,
        realName: form.realName,
        idCardNo: form.idCardNo,
        password: form.password,
      })
      ElMessage.success('商户创建成功，已自动创建登录用户')
    }
    emit('saved')
    emit('update:visible', false)
  } catch (e) {
    // 错误已由拦截器提示
  } finally {
    loading.value = false
  }
}
</script>
