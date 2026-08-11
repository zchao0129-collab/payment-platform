import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import Vant from 'vant'
import 'vant/lib/index.css'
import App from './App.vue'
import router from './router'
import { useUserStore } from './stores/user'
import './styles/global.css'

// Fix: dayjs plugins required by Element Plus (el-date-picker, etc.)
// dayjs plugins — MUST be configured before Element Plus because
// el-date-picker uses dayjs internally and needs these plugins.
// vite.config.js has resolve.dedupe:['dayjs'] to force a single instance.
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import isToday from 'dayjs/plugin/isToday'
import isYesterday from 'dayjs/plugin/isYesterday'
import weekOfYear from 'dayjs/plugin/weekOfYear'
import localeData from 'dayjs/plugin/localeData'
import utc from 'dayjs/plugin/utc'
import customParseFormat from 'dayjs/plugin/customParseFormat'
dayjs.extend(isToday)
dayjs.extend(isYesterday)
dayjs.extend(weekOfYear)
dayjs.extend(localeData)
dayjs.extend(utc)
dayjs.extend(customParseFormat)
dayjs.locale('zh-cn')

const app = createApp(App)

// Global error handler — catch rendering errors
app.config.errorHandler = (err, instance, info) => {
  console.error('[Vue Error]', err, info)
  // Show error overlay on page
  const el = document.createElement('div')
  el.style.cssText = 'position:fixed;top:0;left:0;right:0;background:#ff4d4f;color:#fff;padding:16px 24px;z-index:99999;font-size:14px;line-height:1.6;max-height:50vh;overflow-y:auto;white-space:pre-wrap;font-family:monospace;'
  el.textContent = 'Vue Error: ' + (err?.message || err) + '\n\n' + (err?.stack || '')
  document.body.appendChild(el)
}

// 1. Pinia
const pinia = createPinia()
app.use(pinia)

// 2. Restore login state from localStorage (before router guards run)
useUserStore().loadFromStorage()
// 3. Router
app.use(router)

// 4. UI libs
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
app.use(ElementPlus, { locale: zhCn })
app.use(Vant)

app.mount('#app')
