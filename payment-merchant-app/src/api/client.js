import axios from 'axios';
import AsyncStorage from '@react-native-async-storage/async-storage';

const TOKEN_KEY = 'mch_access_token';
const USER_KEY = 'mch_user_info';

const client = axios.create({
  // 使用 EXPO_PUBLIC_API_URL 环境变量，未设置时回退到本地调试地址
  baseURL: process.env.EXPO_PUBLIC_API_URL || 'http://localhost:8080/api',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
});

// Request interceptor — attach JWT token
client.interceptors.request.use(async (config) => {
  const token = await AsyncStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor — unwrap Result<T>
client.interceptors.response.use(
  (response) => {
    const res = response.data;
    if (res.code === 200) {
      return res.data;
    }
    if (res.code === 401) {
      clearSession();
      throw new Error(res.msg || '登录已过期，请重新登录');
    }
    throw new Error(res.msg || '请求失败');
  },
  (error) => {
    if (error.response?.status === 401) {
      clearSession();
      throw new Error('登录已过期，请重新登录');
    }
    throw new Error(error.message || '网络错误');
  }
);

async function clearSession() {
  await AsyncStorage.multiRemove([TOKEN_KEY, 'mch_refresh_token', USER_KEY]);
}

export async function saveLogin(resp) {
  await AsyncStorage.setItem(TOKEN_KEY, resp.accessToken);
  await AsyncStorage.setItem('mch_refresh_token', resp.refreshToken);
  await AsyncStorage.setItem(USER_KEY, JSON.stringify({
    userId: resp.userId,
    username: resp.username,
    phone: resp.phone,
    role: resp.role,
    merchantId: resp.merchantId,
    merchantNo: resp.merchantNo,
    merchantName: resp.merchantName,
  }));
}

export async function getUserInfo() {
  const raw = await AsyncStorage.getItem(USER_KEY);
  return raw ? JSON.parse(raw) : null;
}

export async function getToken() {
  return AsyncStorage.getItem(TOKEN_KEY);
}

export async function logout() {
  await AsyncStorage.multiRemove([TOKEN_KEY, 'mch_refresh_token', USER_KEY]);
}

export default client;
