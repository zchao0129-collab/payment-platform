import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  // ========== Auth ==========
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/pc/LoginPage.vue'),
    meta: { title: '商户登录' },
  },
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: () => import('@/views/pc/admin/AdminLoginPage.vue'),
    meta: { title: '管理员登录' },
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/pc/RegisterPage.vue'),
    meta: { title: '商户入驻' },
  },

  // ========== PC 商户端 ==========
  {
    path: '/merchant',
    component: () => import('@/views/pc/merchant/MerchantLayout.vue'),
    redirect: '/merchant/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'MerchantDashboard',
        component: () => import('@/views/pc/merchant/DashboardPage.vue'),
        meta: { title: '工作台首页' },
      },
      {
        path: 'orders',
        name: 'MerchantOrders',
        component: () => import('@/views/pc/merchant/OrdersPage.vue').catch(err => {
          console.error('[Router] Failed to load OrdersPage:', err)
          // Return a fallback component on load failure
          return { template: '<div style="padding:20px;color:red">订单页面加载失败，请刷新重试</div>' }
        }),
        meta: { title: '我的订单' },
      },
      {
        path: 'commission',
        name: 'MerchantCommission',
        component: () => import('@/views/pc/merchant/CommissionPage.vue'),
        meta: { title: '佣金列表' },
      },
      {
        path: 'withdrawal',
        name: 'MerchantWithdrawal',
        component: () => import('@/views/pc/merchant/WithdrawalPage.vue'),
        meta: { title: '提现明细' },
      },
      {
        path: 'qrcode',
        name: 'MerchantQrcode',
        component: () => import('@/views/pc/merchant/QrcodePage.vue'),
        meta: { title: '码牌管理' },
      },
      {
        path: 'profile',
        name: 'MerchantProfile',
        component: () => import('@/views/pc/merchant/ProfilePage.vue'),
        meta: { title: '商户信息' },
      },
    ],
  },

  // ========== PC 管理端 ==========
  {
    path: '/admin',
    component: () => import('@/views/pc/admin/AdminLayout.vue'),
    redirect: '/admin/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('@/views/pc/admin/DashboardPage.vue'),
        meta: { title: '数据看板' },
      },
      {
        path: 'orders',
        name: 'AdminOrders',
        component: () => import('@/views/pc/admin/OrdersPage.vue'),
        meta: { title: '订单管理' },
      },
      {
        path: 'merchants',
        name: 'AdminMerchants',
        component: () => import('@/views/pc/admin/MerchantsPage.vue'),
        meta: { title: '商户管理' },
      },
      {
        path: 'audit',
        name: 'AdminAudit',
        component: () => import('@/views/pc/admin/AuditPage.vue'),
        meta: { title: '提现审核' },
      },
      {
        path: 'alipay',
        name: 'AdminAlipay',
        component: () => import('@/views/pc/admin/AlipayConfigPage.vue'),
        meta: { title: '支付宝配置' },
      },
      {
        path: 'wechat',
        name: 'AdminWechat',
        component: () => import('@/views/pc/admin/WechatConfigPage.vue'),
        meta: { title: '微信支付配置' },
      },
      {
        path: 'commconfig',
        name: 'AdminCommConfig',
        component: () => import('@/views/pc/admin/CommConfigPage.vue'),
        meta: { title: '返佣配置' },
      },
      {
        path: 'amountfloat',
        name: 'AdminAmountFloat',
        component: () => import('@/views/pc/admin/OrderAmountConfigPage.vue'),
        meta: { title: '金额浮动配置' },
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/views/pc/admin/UsersPage.vue'),
        meta: { title: '用户管理' },
      },
      {
        path: 'stats',
        name: 'AdminStats',
        component: () => import('@/views/pc/admin/StatsPage.vue'),
        meta: { title: '统计分析' },
      },
    ],
  },

  // ========== App 商户端登录 ==========
  {
    path: '/app/login',
    name: 'AppLogin',
    component: () => import('@/views/mobile/AppLoginPage.vue'),
    meta: { title: '商户登录' },
  },

  // ========== App 商户端（5 Tab） ==========
  {
    path: '/app/merchant',
    name: 'AppMerchant',
    component: () => import('@/views/mobile/AppMerchantPage.vue'),
    meta: { title: '商户平台' },
  },

  // ========== App 用户端：收银台 ==========
  {
    path: '/app/cashier',
    name: 'AppCashier',
    component: () => import('@/views/mobile/AppCashierPage.vue'),
    meta: { title: '收银台' },
  },

  // ========== App 用户端：支付结果 ==========
  {
    path: '/app/pay-result',
    name: 'AppPayResult',
    component: () => import('@/views/mobile/AppPayResultPage.vue'),
    meta: { title: '支付结果' },
  },

  // ========== PC 收银台 ==========
  {
    path: '/cashier',
    name: 'Cashier',
    component: () => import('@/views/pc/CashierPage.vue'),
    meta: { title: '收银台' },
  },

  // ========== 默认跳转 ==========
  {
    path: '/',
    redirect: '/login',
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/login',
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

const APP_TITLE = window.__APP_CONFIG__?.appTitle || '支付商户管理平台'

router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - ${APP_TITLE}` : APP_TITLE

  const isAdminRoute = to.path.startsWith('/admin')
  const isMerchantRoute = to.path.startsWith('/merchant')

  // Use role-specific token keys — admin and merchant sessions are independent
  const tokenKey = isAdminRoute ? 'admin_access_token' : 'mch_access_token'
  const userKey = isAdminRoute ? 'admin_user_info' : 'mch_user_info'

  const token = localStorage.getItem(tokenKey)
  const userInfo = (() => {
    try { return JSON.parse(localStorage.getItem(userKey)) } catch { return null }
  })()
  const role = userInfo?.role

  // 1. Logged-in admin visiting merchant login → redirect to admin dashboard
  if (to.path === '/login' && role === 1) {
    next('/admin/dashboard')
    return
  }

  // 2. Logged-in merchant visiting admin login → redirect to merchant dashboard
  if (to.path === '/admin/login' && role === 2) {
    next('/merchant/dashboard')
    return
  }

  // 3. Logged-in admin visiting admin login → redirect to admin dashboard
  if (to.path === '/admin/login' && role === 1) {
    next('/admin/dashboard')
    return
  }

  // 4. Logged-in merchant visiting merchant login → redirect to merchant dashboard
  if (to.path === '/login' && role === 2) {
    next('/merchant/dashboard')
    return
  }

  // 5. No admin token → admin pages (except login) redirect to admin login
  if (isAdminRoute && to.path !== '/admin/login' && !token) {
    next('/admin/login')
    return
  }

  // 6. No merchant token → merchant pages redirect to merchant login
  if (isMerchantRoute && !token) {
    next('/login')
    return
  }

  next()
})

export default router
