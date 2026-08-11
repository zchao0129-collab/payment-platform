<template>
  <div>
    <div class="page-title">码牌管理</div>

    <!-- QR Code Card -->
    <div class="card" style="text-align:center" v-loading="loading">
      <div class="card-header" style="text-align:left">我的收款码牌</div>

      <!-- Real QR code image (Alipay-style composite) -->
      <div v-if="qrcodeImage" class="qr-wrapper">
        <img :src="qrcodeImage" alt="支付宝商家收款码" class="qr-image" @click="previewVisible = true" title="点击查看大图" />
      </div>

      <!-- Placeholder when no QR code yet -->
      <div v-else class="qr-placeholder">
        <div class="qr-empty-icon">📱</div>
        <div style="color:#999;font-size:14px">暂无码牌，请点击下方按钮生成</div>
      </div>

      <div style="display:flex;gap:10px;justify-content:center;margin-top:12px">
        <el-button type="primary" @click="downloadQrcode" :disabled="!qrcodeImage">
          下载二维码
        </el-button>
        <el-button @click="handleRegenerate" :loading="regenerating">
          重新生成
        </el-button>
      </div>
    </div>

    <!-- Usage Guide -->
    <div class="card">
      <div class="card-header">码牌使用说明</div>
      <div style="font-size:13px;color:#999;line-height:2">
        1. 用户使用支付宝扫描上方二维码<br />
        2. 进入收银台页面，输入付款金额<br />
        3. 点击支付，调起支付宝完成付款<br />
        4. 支付成功后订单自动关联到本商户<br />
        5. 码牌永久有效，无需更换
      </div>
    </div>

    <!-- Image Preview Overlay -->
    <teleport to="body">
      <div v-if="previewVisible" class="preview-overlay" @click.self="previewVisible = false">
        <div class="preview-close" @click="previewVisible = false">✕</div>
        <img :src="qrcodeImage" alt="收款码牌预览" class="preview-image" />
        <div class="preview-hint">点击空白处关闭</div>
      </div>
    </teleport>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import * as qrcodeApi from '@/api/qrcode'

const userStore = useUserStore()
const qrcodeImage = ref('')
const qrcodeNo = ref('')
const loading = ref(false)
const regenerating = ref(false)
const previewVisible = ref(false)

onMounted(async () => {
  await fetchQrcode()
})

async function fetchQrcode() {
  loading.value = true
  try {
    const data = await qrcodeApi.getMyQrcode()
    if (data && data.qrcodeImage) {
      qrcodeImage.value = data.qrcodeImage
      qrcodeNo.value = data.qrcodeNo || ''
    }
  } catch (e) {
    console.warn('Failed to load QR code:', e)
  } finally {
    loading.value = false
  }
}

async function handleRegenerate() {
  try {
    await ElMessageBox.confirm(
      '重新生成后旧码牌将失效，确认继续？',
      '提示',
      { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }

  regenerating.value = true
  try {
    const data = await qrcodeApi.regenerateQrcode()
    if (data && data.qrcodeImage) {
      qrcodeImage.value = data.qrcodeImage
      qrcodeNo.value = data.qrcodeNo || ''
      ElMessage.success('码牌已重新生成')
    }
  } catch (e) {
    ElMessage.error(e.message || '重新生成失败')
  } finally {
    regenerating.value = false
  }
}

function downloadQrcode() {
  if (!qrcodeImage.value) return
  const link = document.createElement('a')
  link.href = qrcodeImage.value
  link.download = `码牌_${userStore.userInfo.merchantNo || '收款'}.png`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}
</script>

<style scoped>
.qr-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 8px;
}

.qr-image {
  width: 260px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  object-fit: contain;
  background: #fff;
  cursor: pointer;
  transition: transform 0.2s;
}
.qr-image:hover {
  transform: scale(1.05);
}

/* Preview overlay */
.preview-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.85);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
}
.preview-close {
  position: absolute;
  top: 20px;
  right: 30px;
  font-size: 32px;
  color: #fff;
  cursor: pointer;
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.15);
  transition: background 0.2s;
}
.preview-close:hover {
  background: rgba(255, 255, 255, 0.3);
}
.preview-image {
  max-width: 90vw;
  max-height: 80vh;
  border-radius: 16px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.5);
  background: #fff;
  object-fit: contain;
}
.preview-hint {
  color: rgba(255, 255, 255, 0.5);
  font-size: 13px;
  margin-top: 16px;
}

.qr-placeholder {
  width: 260px;
  height: 338px;
  margin: 20px auto;
  border: 2px dashed #d9d9d9;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  background: #fafafa;
}

.qr-empty-icon {
  font-size: 48px;
  opacity: 0.5;
}
</style>
