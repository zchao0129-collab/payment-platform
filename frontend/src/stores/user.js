import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as authApi from '@/api/auth'

// Separate storage keys per role to allow simultaneous merchant + admin login in same browser
const STORAGE = {
  1: { token: 'admin_access_token', refresh: 'admin_refresh_token', user: 'admin_user_info' },
  2: { token: 'mch_access_token', refresh: 'mch_refresh_token', user: 'mch_user_info' },
}

/** Get storage keys for a given role */
export function getRoleKeys(role) {
  return STORAGE[role] || STORAGE[2]
}

/** Get the token for the current active context (determined by current URL path) */
export function getActiveToken() {
  const isAdmin = window.location.pathname.startsWith('/admin')
  const role = isAdmin ? 1 : 2
  return localStorage.getItem(STORAGE[role].token)
}

/** Get the user info for the current active context */
export function getActiveUserInfo() {
  const isAdmin = window.location.pathname.startsWith('/admin')
  const role = isAdmin ? 1 : 2
  try { return JSON.parse(localStorage.getItem(STORAGE[role].user)) } catch { return null }
}

export const useUserStore = defineStore('user', () => {
  const userInfo = ref({
    userId: null,
    username: '',
    phone: '',
    role: null,       // 1=admin, 2=merchant
    merchantId: null,
    merchantNo: '',
    merchantName: '',
  })

  const isLoggedIn = computed(() => !!userInfo.value.userId)
  const isAdmin = computed(() => userInfo.value.role === 1)
  const isMerchant = computed(() => userInfo.value.role === 2)

  /** Load user from storage. Pass role to load specific role, or omit to auto-detect from URL. */
  function loadFromStorage(role) {
    if (!role) {
      role = window.location.pathname.startsWith('/admin') ? 1 : 2
    }
    const keys = STORAGE[role]
    const stored = localStorage.getItem(keys.user)
    if (stored) {
      try {
        const parsed = JSON.parse(stored)
        userInfo.value = {
          userId: parsed.userId ?? null,
          username: parsed.username ?? '',
          phone: parsed.phone ?? '',
          role: parsed.role ?? null,
          merchantId: parsed.merchantId ?? null,
          merchantNo: parsed.merchantNo ?? '',
          merchantName: parsed.merchantName ?? '',
        }
      } catch { /* ignore corrupt data */ }
    }
  }

  function saveLogin(resp) {
    const role = resp.role
    const keys = STORAGE[role] || STORAGE[2]

    userInfo.value = {
      userId: resp.userId,
      username: resp.username,
      phone: resp.phone,
      role: resp.role,
      merchantId: resp.merchantId,
      merchantNo: resp.merchantNo,
      merchantName: resp.merchantName,
    }
    localStorage.setItem(keys.token, resp.accessToken)
    localStorage.setItem(keys.refresh, resp.refreshToken)
    localStorage.setItem(keys.user, JSON.stringify(userInfo.value))
  }

  async function loginAction(loginData) {
    const resp = await authApi.login(loginData)
    saveLogin(resp)
    return resp
  }

  function logoutAction() {
    const role = userInfo.value.role || 2
    const keys = STORAGE[role] || STORAGE[2]
    authApi.logout().catch(() => {})
    clearSessionForRole(role)
  }

  function clearSession() {
    // Clear all roles for a full logout
    clearSessionForRole(1)
    clearSessionForRole(2)
    userInfo.value = {
      userId: null, username: '', phone: '', role: null,
      merchantId: null, merchantNo: '', merchantName: '',
    }
  }

  function clearSessionForRole(role) {
    const keys = STORAGE[role]
    if (keys) {
      localStorage.removeItem(keys.token)
      localStorage.removeItem(keys.refresh)
      localStorage.removeItem(keys.user)
    }
  }

  return {
    userInfo, isLoggedIn, isAdmin, isMerchant,
    loadFromStorage, saveLogin, loginAction, logout: logoutAction, logoutAction, clearSession,
  }
})
