import React, { useState } from 'react';
import {
  View, Text, TextInput, TouchableOpacity,
  StyleSheet, KeyboardAvoidingView, Platform, ActivityIndicator,
} from 'react-native';
import { showAlert } from '../utils/dialog';
import { useAuth } from '../store/AuthContext';
import { getCaptchaTicket, login as loginApi } from '../api/auth';
import Colors from '../theme/colors';

export default function LoginScreen() {
  const { login } = useAuth();
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);

  const handleLogin = async () => {
    if (!phone.trim()) { showAlert('提示', '请输入手机号'); return; }
    if (!password) { showAlert('提示', '请输入密码'); return; }
    setLoading(true);
    try {
      // 1. 获取验证票据
      const ticket = await getCaptchaTicket(1);
      // 2. 登录
      const resp = await loginApi({ phone: phone.trim(), password, captchaTicket: ticket });
      await login(resp);
    } catch (e) {
      showAlert('登录失败', e.message || '请检查账号密码');
    } finally {
      setLoading(false);
    }
  };

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      <View style={styles.header}>
        <Text style={styles.logo}>💚</Text>
        <Text style={styles.title}>支付商户端</Text>
        <Text style={styles.subtitle}>聚合支付 · 轻松收款</Text>
      </View>

      <View style={styles.form}>
        <View style={styles.inputGroup}>
          <Text style={styles.label}>手机号</Text>
          <TextInput
            style={styles.input}
            value={phone}
            onChangeText={setPhone}
            placeholder="请输入手机号"
            placeholderTextColor={Colors.textPlaceholder}
            keyboardType="phone-pad"
            maxLength={11}
            autoCapitalize="none"
          />
        </View>

        <View style={styles.inputGroup}>
          <Text style={styles.label}>密码</Text>
          <TextInput
            style={styles.input}
            value={password}
            onChangeText={setPassword}
            placeholder="请输入密码"
            placeholderTextColor={Colors.textPlaceholder}
            secureTextEntry
            autoCapitalize="none"
          />
        </View>

        <TouchableOpacity
          style={[styles.button, loading && styles.buttonDisabled]}
          onPress={handleLogin}
          disabled={loading}
          activeOpacity={0.8}
        >
          {loading ? (
            <ActivityIndicator color={Colors.white} />
          ) : (
            <Text style={styles.buttonText}>登录</Text>
          )}
        </TouchableOpacity>
      </View>

      <Text style={styles.footer}>v1.0.0 · 支付商户管理平台</Text>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.white, justifyContent: 'center', paddingHorizontal: 32 },
  header: { alignItems: 'center', marginBottom: 48 },
  logo: { fontSize: 56, marginBottom: 12 },
  title: { fontSize: 26, fontWeight: '700', color: Colors.text, marginBottom: 6 },
  subtitle: { fontSize: 14, color: Colors.textHint },
  form: { width: '100%' },
  inputGroup: { marginBottom: 20 },
  label: { fontSize: 14, fontWeight: '500', color: Colors.text, marginBottom: 8 },
  input: {
    borderWidth: 1, borderColor: Colors.border, borderRadius: 12,
    paddingHorizontal: 16, paddingVertical: 14, fontSize: 16,
    color: Colors.text, backgroundColor: Colors.background,
  },
  button: {
    backgroundColor: Colors.primary, borderRadius: 12, paddingVertical: 16,
    alignItems: 'center', marginTop: 8,
  },
  buttonDisabled: { opacity: 0.6 },
  buttonText: { color: Colors.white, fontSize: 17, fontWeight: '600' },
  footer: { textAlign: 'center', color: Colors.textHint, fontSize: 12, marginTop: 48 },
});
