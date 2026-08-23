<template>
  <div class="layout">
    <div class="sidebar">
      <div class="logo"><span>⚙️</span>&nbsp;管理后台</div>
      <div class="menu">
        <div class="menu-group-title">工作台</div>
        <div
          class="menu-item"
          :class="{ active: currentRoute === 'dashboard' }"
          @click="navigate('dashboard')"
        >
          <span class="icon">📊</span>数据看板
        </div>
        <div class="menu-group-title">业务管理</div>
        <div
          class="menu-item"
          :class="{ active: currentRoute === 'orders' }"
          @click="navigate('orders')"
        >
          <span class="icon">📋</span>订单管理
        </div>
        <div
          class="menu-item"
          :class="{ active: currentRoute === 'merchants' }"
          @click="navigate('merchants')"
        >
          <span class="icon">🏪</span>商户管理
        </div>
        <div
          class="menu-item"
          :class="{ active: currentRoute === 'audit' }"
          @click="navigate('audit')"
        >
          <span class="icon">✅</span>提现审核
          <span class="badge" style="margin-left:auto">3</span>
        </div>
        <div class="menu-group-title">系统设置</div>
        <div
          class="menu-item"
          :class="{ active: currentRoute === 'alipay' }"
          @click="navigate('alipay')"
        >
          <span class="icon">💳</span>支付宝配置
        </div>
        <div
          class="menu-item"
          :class="{ active: currentRoute === 'wechat' }"
          @click="navigate('wechat')"
        >
          <span class="icon">💚</span>微信支付配置
        </div>
        <div
          class="menu-item"
          :class="{ active: currentRoute === 'commconfig' }"
          @click="navigate('commconfig')"
        >
          <span class="icon">📐</span>返佣配置
        </div>
        <div
          class="menu-item"
          :class="{ active: currentRoute === 'users' }"
          @click="navigate('users')"
        >
          <span class="icon">👥</span>用户管理
        </div>
        <div class="menu-group-title">数据</div>
        <div
          class="menu-item"
          :class="{ active: currentRoute === 'stats' }"
          @click="navigate('stats')"
        >
          <span class="icon">📈</span>统计分析
        </div>
      </div>
    </div>
    <div class="main-area">
      <div class="header">
        <span style="font-weight:600">支付商户管理平台 · 管理后台</span>
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
  router.push(`/admin/${name}`).catch(() => {})
}

function handleLogout() {
  userStore.logout()
  router.push('/admin/login')
}
</script>
