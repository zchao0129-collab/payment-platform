<template>
  <div class="layout">
    <div class="sidebar">
      <div class="logo"><span>💰</span>&nbsp;商户平台</div>
      <div class="menu">
        <div class="menu-group-title">工作台</div>
        <div
          class="menu-item"
          :class="{ active: currentRoute === 'dashboard' }"
          @click="navigate('dashboard')"
        >
          <span class="icon">📊</span>工作台首页
        </div>
        <div class="menu-group-title">业务管理</div>
        <div
          class="menu-item"
          :class="{ active: currentRoute === 'orders' }"
          @click="navigate('orders')"
        >
          <span class="icon">📋</span>我的订单
        </div>
        <div
          class="menu-item"
          :class="{ active: currentRoute === 'commission' }"
          @click="navigate('commission')"
        >
          <span class="icon">💰</span>佣金列表
        </div>
        <div
          class="menu-item"
          :class="{ active: currentRoute === 'qrcode' }"
          @click="navigate('qrcode')"
        >
          <span class="icon">📱</span>码牌管理
        </div>
        <div class="menu-group-title">账户</div>
        <div
          class="menu-item"
          :class="{ active: currentRoute === 'profile' }"
          @click="navigate('profile')"
        >
          <span class="icon">👤</span>商户信息
        </div>
      </div>
      <div class="sidebar-footer">
        商户号：{{ userStore.userInfo.merchantNo || '—' }}
      </div>
    </div>
    <div class="main-area">
      <div class="header">
        <span style="font-weight:600">商户名称：{{ userStore.userInfo.merchantName }}</span>
        <div class="user-info">
          <span class="tag tag-green">商户</span>
          <span>{{ userStore.userInfo.username }}</span>
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
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const currentRoute = computed(() => {
  const segments = route.path.split('/').filter(Boolean)
  return segments[1] || 'dashboard'
})

function navigate(name) {
  if (currentRoute.value === name) return
  router.push(`/merchant/${name}`).catch(() => {})
}

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.sidebar-footer {
  padding: 16px 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  font-size: 12px;
  color: rgba(255, 255, 255, 0.45);
}
</style>
