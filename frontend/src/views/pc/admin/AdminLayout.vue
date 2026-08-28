<template>
  <div class="layout">
    <!-- 桌面端侧边栏（移动端由下方抽屉接管） -->
    <aside class="sidebar">
      <div class="logo"><span>⚙️</span>&nbsp;管理后台</div>
      <nav class="menu">
        <template v-for="group in menuGroups" :key="group.title">
          <div class="menu-group-title">{{ group.title }}</div>
          <div
            v-for="item in group.items"
            :key="item.key"
            class="menu-item"
            :class="{ active: currentRoute === item.key }"
            @click="navigate(item.key)"
          >
            <span class="icon">{{ item.icon }}</span>{{ item.label }}
            <span v-if="item.badge" class="badge" style="margin-left:auto">{{ item.badge }}</span>
          </div>
        </template>
      </nav>
    </aside>

    <!-- 移动端抽屉菜单 -->
    <el-drawer
      v-model="menuOpen"
      direction="ltr"
      size="220px"
      :with-header="false"
      class="admin-mobile-drawer"
    >
      <div class="sidebar">
        <div class="logo"><span>⚙️</span>&nbsp;管理后台</div>
        <nav class="menu">
          <template v-for="group in menuGroups" :key="group.title">
            <div class="menu-group-title">{{ group.title }}</div>
            <div
              v-for="item in group.items"
              :key="item.key"
              class="menu-item"
              :class="{ active: currentRoute === item.key }"
              @click="navigate(item.key)"
            >
              <span class="icon">{{ item.icon }}</span>{{ item.label }}
              <span v-if="item.badge" class="badge" style="margin-left:auto">{{ item.badge }}</span>
            </div>
          </template>
        </nav>
      </div>
    </el-drawer>

    <div class="main-area">
      <div class="header">
        <div class="header-left">
          <button class="menu-toggle" aria-label="打开菜单" @click="menuOpen = true">
            <span></span><span></span><span></span>
          </button>
          <span class="header-title">支付商户管理平台 · 管理后台</span>
        </div>
        <div class="user-info">
          <span class="tag tag-red" style="background:#fff2f0;color:#ff4d4f">管理员</span>
          <span>Admin</span>
          <span>|</span>
          <a href="#" @click.prevent="handleLogout">退出</a>
        </div>
      </div>
      <div class="content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const menuOpen = ref(false)

const menuGroups = [
  {
    title: '工作台',
    items: [{ key: 'dashboard', icon: '📊', label: '数据看板' }],
  },
  {
    title: '业务管理',
    items: [
      { key: 'orders', icon: '📋', label: '订单管理' },
      { key: 'merchants', icon: '🏪', label: '商户管理' },
      { key: 'audit', icon: '✅', label: '提现审核', badge: 3 },
    ],
  },
  {
    title: '系统设置',
    items: [
      { key: 'alipay', icon: '💳', label: '支付宝配置' },
      { key: 'wechat', icon: '💚', label: '微信支付配置' },
      { key: 'commconfig', icon: '📐', label: '返佣配置' },
      { key: 'amountfloat', icon: '🔀', label: '金额浮动配置' },
      { key: 'users', icon: '👥', label: '用户管理' },
    ],
  },
  {
    title: '数据',
    items: [{ key: 'stats', icon: '📈', label: '统计分析' }],
  },
]

const currentRoute = computed(() => {
  const segments = route.path.split('/').filter(Boolean)
  return segments[1] || 'dashboard'
})

function navigate(name) {
  menuOpen.value = false
  if (currentRoute.value === name) return
  router.push(`/admin/${name}`).catch(() => {})
}

function handleLogout() {
  userStore.logout()
  router.push('/admin/login')
}
</script>

<style scoped>
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.header-title {
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 汉堡按钮：默认隐藏，移动端显示 */
.menu-toggle {
  display: none;
  width: 36px;
  height: 36px;
  background: transparent;
  border: 1px solid var(--border);
  border-radius: 6px;
  cursor: pointer;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 0;
  flex-shrink: 0;
}

.menu-toggle span {
  display: block;
  width: 18px;
  height: 2px;
  background: var(--text);
  border-radius: 1px;
}

@media (max-width: 768px) {
  .menu-toggle {
    display: flex;
  }
}
</style>
