<template>
  <div class="phone-frame-container">
    <div class="phone-frame">
      <div class="phone-status-bar">
        <span class="time">9:41</span>
        <span class="icons">●●●●○ &nbsp;WiFi &nbsp;🔋 85%</span>
      </div>

      <!-- Main View -->
      <template v-if="!subPage">
        <div class="phone-app-header">
          <span>{{ currentTabTitle }}</span>
        </div>
        <div class="phone-app-content">
          <!-- Tab 1: Dashboard -->
          <div v-show="activeTab === 'dashboard'">
            <div class="m-welcome-card">
              <div class="w-name">👋 你好, {{ userStore.userInfo.username }}</div>
              <div class="w-merchant">商户号：{{ userStore.userInfo.merchantNo }}</div>
              <div class="w-date">2024-08-09 星期四</div>
            </div>
            <div class="m-stat-row">
              <div class="m-stat-card"><div class="m-label">今日营收</div><div class="m-value">¥3,280</div><div class="m-sub" style="color:var(--success)">↑ 较昨日 12%</div></div>
              <div class="m-stat-card"><div class="m-label">本周营收</div><div class="m-value">¥18,560</div><div class="m-sub">共 42 笔</div></div>
              <div class="m-stat-card"><div class="m-label">本月营收</div><div class="m-value">¥86,420</div><div class="m-sub">共 186 笔</div></div>
              <div class="m-stat-card"><div class="m-label">可提现佣金</div><div class="m-value" style="color:var(--success)">¥4,321</div><div class="m-sub">已提现 ¥2,150</div></div>
            </div>
            <div class="m-section-header">
              <span class="m-section-title">📋 近期订单</span>
              <span class="m-section-link" @click="activeTab='orders'">查看全部 ›</span>
            </div>
            <OrderCard v-for="o in recentOrders" :key="o.orderNo" :order="o" />
          </div>

          <!-- Tab 2: Orders -->
          <div v-show="activeTab === 'orders'">
            <div class="m-search-bar">
              <span class="m-search-icon">🔍</span>
              <input type="text" v-model="orderSearch" placeholder="搜索订单号 / 产品名称" />
            </div>
            <div class="m-filter-chips">
              <span v-for="s in orderStatuses" :key="s.value"
                class="m-chip" :class="{ active: orderFilter === s.value }"
                @click="orderFilter = s.value">{{ s.label }}</span>
            </div>
            <OrderCard v-for="o in filteredOrders" :key="o.orderNo" :order="o" />
            <div class="load-more">加载更多 (共86条) ›</div>
          </div>

          <!-- Tab 3: Commission -->
          <div v-show="activeTab === 'commission'">
            <div class="m-commission-summary">
              <div class="m-cs-label">累计佣金</div>
              <div class="m-cs-total">¥12,580.00</div>
              <div class="m-cs-row">
                <div class="m-cs-item"><div class="m-cs-val" style="color:var(--primary)">¥4,321.00</div><div class="m-cs-lbl">可提现</div></div>
                <div class="m-cs-item"><div class="m-cs-val">¥8,259.00</div><div class="m-cs-lbl">已提现</div></div>
                <div class="m-cs-item"><div class="m-cs-val" style="color:var(--warning)">¥2,000.00</div><div class="m-cs-lbl">审核中</div></div>
              </div>
            </div>
            <button class="m-btn-block m-btn-primary" @click="showWithdrawSheet = true">发起提现</button>
            <div v-for="c in commissions" :key="c.orderNo" class="m-commission-item">
              <div class="m-ci-left">
                <div class="m-ci-order">{{ c.orderNo }}</div>
                <div class="m-ci-meta">比例 {{ c.rate }} · {{ c.createTime }}</div>
              </div>
              <div class="m-ci-right">
                <div class="m-ci-amount">{{ c.commission }}</div>
                <div class="m-ci-status"><span class="tag" :class="commissionTagClass(c.status)">{{ c.statusText }}</span></div>
              </div>
            </div>

            <!-- Withdraw Bottom Sheet -->
            <div class="m-bottom-sheet" :class="{ active: showWithdrawSheet }">
              <div class="m-bs-overlay" @click="showWithdrawSheet = false"></div>
              <div class="m-bs-panel">
                <div class="m-bs-handle"></div>
                <div class="m-bs-title">发起提现</div>
                <div class="m-bs-amount-display">
                  <div class="m-bs-balance-label">可提现金额</div>
                  <div class="m-bs-balance-value">¥4,321.00</div>
                </div>
                <div class="m-form-group"><label>提现金额 *</label><input type="number" placeholder="请输入提现金额" min="0.01" max="4321" /></div>
                <div class="m-form-group"><label>到账支付宝账号</label><div class="m-input-readonly">zhang***@example.com</div></div>
                <div class="warning-note">⚠️ 提现需管理员审核，审核通过后自动打款</div>
                <div class="m-bs-row">
                  <button class="cancel-btn" @click="showWithdrawSheet = false">取消</button>
                  <button class="confirm-btn" @click="showWithdrawSheet = false">确认提现</button>
                </div>
              </div>
            </div>
          </div>

          <!-- Tab 4: QR Code -->
          <div v-show="activeTab === 'qrcode'">
            <div class="m-qr-area">
              <div class="m-qr-code"></div>
              <div class="m-qr-merchant">张三的店铺</div>
              <div class="m-qr-sub">商户号：M20240001 · 码牌永久有效</div>
              <button class="m-btn-outline">📥 保存到相册</button>
              <button class="m-btn-outline">📤 分享码牌</button>
            </div>
            <div class="m-guide" :class="{ open: qrGuideOpen }">
              <div class="m-guide-header" @click="qrGuideOpen = !qrGuideOpen">
                <span>📖 码牌使用说明</span>
                <span class="m-guide-arrow">▼</span>
              </div>
              <div class="m-guide-body">
                1. 用户使用支付宝扫描上方二维码<br />
                2. 进入收银台页面，输入付款金额<br />
                3. 点击支付，调起支付宝完成付款<br />
                4. 支付成功后订单自动关联到本商户<br />
                5. 码牌永久有效，无需更换
              </div>
            </div>
          </div>

          <!-- Tab 5: Profile -->
          <div v-show="activeTab === 'profile'">
            <div class="m-profile-card">
              <div class="m-avatar">👤</div>
              <div class="m-p-name">{{ userStore.userInfo.username }}</div>
              <div class="m-p-phone">{{ userStore.userInfo.phone }}</div>
            </div>
            <div class="m-menu-list">
              <div class="m-menu-item" @click="openSubPage('edit')"><span>商户信息</span><span class="m-chevron">›</span></div>
              <div class="m-menu-item" @click="openSubPage('password')"><span>修改密码</span><span class="m-chevron">›</span></div>
              <div class="m-menu-item"><span>关于平台</span><span class="m-chevron">›</span></div>
            </div>
            <div class="m-menu-list">
              <div class="m-menu-item danger" @click="handleLogout">退出登录</div>
            </div>
          </div>
        </div>

        <!-- Bottom Nav -->
        <div class="phone-bottom-nav">
          <button v-for="t in tabs" :key="t.key" class="tab-item" :class="{ active: activeTab === t.key }" @click="activeTab = t.key">
            <span class="tab-icon">{{ t.icon }}</span>{{ t.label }}
          </button>
        </div>
      </template>

      <!-- Sub Pages -->
      <template v-if="subPage === 'edit'">
        <div class="phone-app-header">
          <button class="back-btn show" @click="subPage = null">←</button>
          <span>商户信息</span>
        </div>
        <div class="phone-app-content">
          <div class="m-form-card">
            <div class="m-form-group"><label>商户名称</label><input type="text" v-model="profileForm.merchantName" /></div>
            <div class="m-form-group"><label>商户号</label><input type="text" :value="userStore.userInfo.merchantNo" disabled /></div>
            <div class="m-form-group"><label>手机号</label><input type="text" v-model="profileForm.phone" /></div>
            <div class="m-form-group"><label>支付宝提现账号</label><input type="text" v-model="profileForm.alipayAccount" /></div>
            <div class="m-form-group"></div>
          </div>
          <button class="m-btn-block m-btn-primary" @click="subPage = null">保存修改</button>
          <button class="m-btn-block m-btn-outline" @click="subPage = 'password'">修改密码</button>
        </div>
      </template>

      <template v-if="subPage === 'password'">
        <div class="phone-app-header">
          <button class="back-btn show" @click="subPage = null">←</button>
          <span>修改密码</span>
        </div>
        <div class="phone-app-content">
          <div class="m-form-card">
            <div class="m-form-group"><label>原密码 *</label><input type="password" placeholder="请输入原密码" /></div>
            <div class="m-form-group"><label>新密码 *</label><input type="password" placeholder="8-20位，含字母+数字" /></div>
            <div class="m-form-group"><label>确认新密码 *</label><input type="password" placeholder="再次输入新密码" /></div>
          </div>
          <button class="m-btn-block m-btn-primary" @click="subPage = null">确认修改</button>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import OrderCard from './OrderCard.vue'

const router = useRouter()
const userStore = useUserStore()
const activeTab = ref('dashboard')
const subPage = ref(null)
const showWithdrawSheet = ref(false)
const qrGuideOpen = ref(false)
const orderSearch = ref('')
const orderFilter = ref('all')

const profileForm = reactive({
  merchantName: '张三的店铺',
  phone: '138****8888',
  alipayAccount: 'zhangsan@example.com',
})

const tabs = [
  { key: 'dashboard', icon: '📊', label: '工作台', title: '工作台' },
  { key: 'orders', icon: '📋', label: '订单', title: '我的订单' },
  { key: 'commission', icon: '💰', label: '佣金', title: '佣金列表' },
  { key: 'qrcode', icon: '📱', label: '码牌', title: '收款码牌' },
  { key: 'profile', icon: '👤', label: '我的', title: '我的' },
]

const currentTabTitle = computed(() => tabs.find(t => t.key === activeTab.value)?.title || '工作台')

const orderStatuses = [
  { label: '全部', value: 'all' }, { label: '已支付', value: 'paid' },
  { label: '已回调', value: 'callback' }, { label: '已退款', value: 'refund' },
  { label: '已失效', value: 'expired' },
]

const recentOrders = ref([
  { orderNo: 'ORD20240809001', amount: '¥128.00', productName: '商品A', time: '2024-08-09 14:32:10', status: 'callback', statusText: '已回调', borderColor: 'var(--success)' },
  { orderNo: 'ORD20240809002', amount: '¥256.00', productName: '商品B', time: '2024-08-09 14:18:05', status: 'paid', statusText: '已支付', borderColor: 'var(--primary)' },
  { orderNo: 'ORD20240809003', amount: '¥89.00', productName: '商品C', time: '2024-08-09 13:45:22', status: 'callback', statusText: '已回调', borderColor: 'var(--success)' },
  { orderNo: 'ORD20240809004', amount: '¥520.00', productName: '商品A', time: '2024-08-09 11:20:00', status: 'refund', statusText: '已退款', borderColor: 'var(--danger)' },
  { orderNo: 'ORD20240809005', amount: '¥66.00', productName: '商品D', time: '2024-08-09 10:05:18', status: 'expired', statusText: '已失效', borderColor: '#ccc' },
])

const allOrders = ref([
  ...recentOrders.value,
  { orderNo: 'ORD20240807001', amount: '¥1,280.00', productName: '商品B', time: '2024-08-07 09:30:00', status: 'fail', statusText: '支付失败', borderColor: 'var(--warning)' },
])

const filteredOrders = computed(() => {
  let list = allOrders.value
  if (orderFilter.value !== 'all') list = list.filter(o => o.status === orderFilter.value)
  if (orderSearch.value) list = list.filter(o => o.orderNo.includes(orderSearch.value) || o.productName.includes(orderSearch.value))
  return list
})

const commissions = ref([
  { orderNo: 'ORD20240809001', rate: '5%', commission: '¥6.40', createTime: '2024-08-10 02:00', status: 'unwithdrawn', statusText: '未提现' },
  { orderNo: 'ORD20240808005', rate: '8%', commission: '¥80.00', createTime: '2024-08-09 02:00', status: 'withdrawn', statusText: '已提现' },
  { orderNo: 'ORD20240807012', rate: '5%', commission: '¥26.00', createTime: '2024-08-08 02:00', status: 'withdrawn', statusText: '已提现' },
  { orderNo: 'ORD20240806003', rate: '10%', commission: '¥250.00', createTime: '2024-08-07 02:00', status: 'auditing', statusText: '审核中' },
])

function commissionTagClass(s) {
  return { unwithdrawn: 'tag-blue', withdrawn: 'tag-green', auditing: 'tag-orange' }[s] || 'tag-gray'
}

function openSubPage(page) { subPage.value = page }
function handleLogout() { userStore.logout(); router.push('/app/login') }
</script>

<style scoped>
/* Mobile component styles — imported from prototype */
.m-welcome-card {
  background: linear-gradient(135deg, var(--primary), #4096ff);
  color: #fff;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 14px;
  box-shadow: 0 4px 12px rgba(22, 119, 255, 0.25);
}
.m-welcome-card .w-name { font-size: 20px; font-weight: 700; }
.m-welcome-card .w-merchant { font-size: 12px; opacity: 0.85; margin-top: 4px; }
.m-welcome-card .w-date { font-size: 11px; opacity: 0.65; margin-top: 2px; }
.m-stat-row { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-bottom: 12px; }
.m-stat-card { background: #fff; border-radius: 10px; padding: 14px 16px; box-shadow: 0 1px 3px rgba(0,0,0,.06); }
.m-stat-card .m-label { font-size: 12px; color: #999; margin-bottom: 6px; }
.m-stat-card .m-value { font-size: 22px; font-weight: 700; color: #333; }
.m-stat-card .m-sub { font-size: 11px; color: #999; margin-top: 2px; }
.m-section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.m-section-header .m-section-title { font-size: 15px; font-weight: 600; }
.m-section-header .m-section-link { font-size: 12px; color: var(--primary); cursor: pointer; }
.m-search-bar { display: flex; align-items: center; background: #fff; border-radius: 20px; padding: 8px 16px; margin-bottom: 10px; gap: 8px; box-shadow: 0 1px 3px rgba(0,0,0,.04); }
.m-search-bar input { border: none; flex: 1; background: transparent; font-size: 14px; outline: none; }
.m-search-icon { font-size: 16px; color: #bbb; }
.m-filter-chips { display: flex; gap: 8px; overflow-x: auto; padding-bottom: 4px; margin-bottom: 12px; }
.m-chip { flex-shrink: 0; padding: 6px 16px; border-radius: 16px; font-size: 13px; background: #fff; color: #999; border: 1px solid #e8e8e8; cursor: pointer; transition: all 0.2s; }
.m-chip.active { background: var(--primary); color: #fff; border-color: var(--primary); }
.load-more { text-align: center; padding: 12px; color: var(--primary); font-size: 13px; cursor: pointer; }
.m-commission-summary { background: #fff; border-radius: 12px; padding: 18px; margin-bottom: 14px; box-shadow: 0 1px 3px rgba(0,0,0,.06); text-align: center; }
.m-cs-label { font-size: 12px; color: #999; margin-bottom: 4px; }
.m-cs-total { font-size: 32px; font-weight: 700; color: var(--success); }
.m-cs-row { display: flex; justify-content: space-around; margin-top: 12px; }
.m-cs-item .m-cs-val { font-size: 16px; font-weight: 600; }
.m-cs-item .m-cs-lbl { font-size: 11px; color: #999; }
.m-commission-item { background: #fff; border-radius: 10px; padding: 12px 14px; margin-bottom: 8px; box-shadow: 0 1px 3px rgba(0,0,0,.06); display: flex; justify-content: space-between; align-items: center; }
.m-ci-left { flex: 1; }
.m-ci-order { font-size: 13px; font-weight: 500; }
.m-ci-meta { font-size: 11px; color: #999; margin-top: 2px; }
.m-ci-right { text-align: right; }
.m-ci-amount { font-size: 17px; font-weight: 700; }
.m-ci-status { margin-top: 2px; }
.m-qr-area { text-align: center; padding: 20px; }
.m-qr-code { width: 180px; height: 180px; margin: 0 auto 12px; background: linear-gradient(45deg, #000 25%, transparent 25%, transparent 75%, #000 75%), linear-gradient(45deg, #000 25%, transparent 25%, transparent 75%, #000 75%); background-size: 16px 16px; background-position: 0 0, 8px 8px; border: 8px solid #333; border-radius: 12px; }
.m-qr-merchant { font-size: 16px; font-weight: 600; margin-bottom: 4px; }
.m-qr-sub { font-size: 12px; color: #999; margin-bottom: 16px; }
.m-btn-outline { background: #fff; color: var(--primary); border: 1px solid var(--primary); display: block; width: 100%; padding: 12px; font-size: 15px; font-weight: 500; border-radius: 24px; text-align: center; margin-bottom: 10px; cursor: pointer; transition: all 0.2s; }
.m-guide { background: #fff; border-radius: 12px; overflow: hidden; box-shadow: 0 1px 3px rgba(0,0,0,.06); }
.m-guide .m-guide-header { padding: 14px 16px; display: flex; justify-content: space-between; align-items: center; cursor: pointer; font-size: 14px; font-weight: 500; }
.m-guide .m-guide-arrow { transition: transform .25s; font-size: 12px; color: #999; }
.m-guide.open .m-guide-arrow { transform: rotate(180deg); }
.m-guide .m-guide-body { display: none; padding: 0 16px 14px; font-size: 13px; color: #999; line-height: 1.8; }
.m-guide.open .m-guide-body { display: block; }
.m-profile-card { background: #fff; border-radius: 12px; padding: 24px 20px; margin-bottom: 14px; box-shadow: 0 1px 3px rgba(0,0,0,.06); text-align: center; }
.m-avatar { width: 64px; height: 64px; border-radius: 50%; background: linear-gradient(135deg, #e6f4ff, #bae0ff); display: flex; align-items: center; justify-content: center; font-size: 28px; margin: 0 auto 12px; color: var(--primary); }
.m-p-name { font-size: 18px; font-weight: 600; }
.m-p-phone { font-size: 13px; color: #999; margin-top: 4px; }
.m-menu-list { background: #fff; border-radius: 12px; overflow: hidden; box-shadow: 0 1px 3px rgba(0,0,0,.06); margin-bottom: 14px; }
.m-menu-item { display: flex; justify-content: space-between; align-items: center; padding: 14px 16px; border-bottom: 1px solid #f5f5f5; cursor: pointer; font-size: 14px; transition: background .15s; }
.m-menu-item:last-child { border-bottom: none; }
.m-menu-item:active { background: #fafafa; }
.m-menu-item .m-chevron { color: #ccc; font-size: 14px; }
.m-menu-item.danger { color: var(--danger); justify-content: center; font-weight: 500; }
.m-btn-block { display: block; width: 100%; padding: 12px; font-size: 15px; font-weight: 600; border-radius: 24px; text-align: center; margin-bottom: 10px; border: none; cursor: pointer; transition: all 0.2s; }
.m-btn-primary { background: var(--primary); color: #fff; }
.m-btn-primary:active { background: var(--primary-hover); }
.m-btn-outline { background: #fff; color: var(--primary); border: 1px solid var(--primary); display: block; width: 100%; padding: 12px; font-size: 15px; font-weight: 500; border-radius: 24px; text-align: center; margin-bottom: 10px; cursor: pointer; transition: all 0.2s; }
.m-form-group { margin-bottom: 14px; }
.m-form-group label { display: block; margin-bottom: 6px; font-size: 13px; font-weight: 500; color: #333; }
.m-form-group input, .m-input-readonly { width: 100%; padding: 10px 14px; border: 1px solid #e8e8e8; border-radius: 8px; font-size: 14px; background: #fff; transition: border-color 0.2s; outline: none; }
.m-form-group input:focus { border-color: var(--primary); }
.m-form-group input:disabled, .m-input-readonly { background: #f5f5f5; color: #999; }
.m-form-card { background: #fff; border-radius: 12px; padding: 20px; margin-bottom: 12px; box-shadow: 0 1px 3px rgba(0,0,0,.06); }
.back-btn { position: absolute; left: 4px; top: 50%; transform: translateY(-50%); font-size: 18px; background: none; border: none; padding: 8px 12px; cursor: pointer; color: var(--primary); }
.back-btn.show { display: block; }
/* Bottom Sheet */
.m-bottom-sheet { position: absolute; inset: 0; z-index: 50; pointer-events: none; }
.m-bottom-sheet.active { pointer-events: auto; }
.m-bs-overlay { position: absolute; inset: 0; background: rgba(0,0,0,.4); opacity: 0; transition: opacity .3s; }
.m-bottom-sheet.active .m-bs-overlay { opacity: 1; }
.m-bs-panel { position: absolute; bottom: 0; left: 0; right: 0; background: #fff; border-radius: 16px 16px 0 0; padding: 20px 20px 30px; transform: translateY(100%); transition: transform .3s ease-out; max-height: 70%; overflow-y: auto; }
.m-bottom-sheet.active .m-bs-panel { transform: translateY(0); }
.m-bs-handle { width: 36px; height: 4px; background: #ddd; border-radius: 2px; margin: 0 auto 16px; }
.m-bs-title { font-size: 16px; font-weight: 600; margin-bottom: 16px; text-align: center; }
.m-bs-amount-display { text-align: center; padding: 12px; background: #f6ffed; border-radius: 8px; margin-bottom: 14px; }
.m-bs-balance-label { font-size: 12px; color: #999; }
.m-bs-balance-value { font-size: 24px; font-weight: 700; color: var(--success); }
.warning-note { font-size: 11px; color: #999; padding: 8px; background: #fffbe6; border-radius: 8px; }
.m-bs-row { display: flex; gap: 10px; margin-top: 16px; }
.m-bs-row button { flex: 1; border-radius: 24px; padding: 12px; font-size: 15px; cursor: pointer; border: none; }
.cancel-btn { background: #f5f5f5; color: #333; }
.confirm-btn { background: var(--primary); color: #fff; }
</style>
