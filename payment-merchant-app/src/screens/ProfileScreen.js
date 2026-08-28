import React, { useState, useEffect } from 'react';
import {
  View, Text, StyleSheet, ScrollView, TouchableOpacity,
  ActivityIndicator, Modal, TextInput,
} from 'react-native';
import { showAlert, showConfirm } from '../utils/dialog';
import { useNavigation } from '@react-navigation/native';
import { useAuth } from '../store/AuthContext';
import { getProfile, updateProfile, changePassword } from '../api/merchant';
import { getCommissionSummary } from '../api/commission';
import Colors from '../theme/colors';

export default function ProfileScreen() {
  const { user, logout } = useAuth();
  const navigation = useNavigation();
  const [profile, setProfile] = useState(null);
  const [commission, setCommission] = useState({ total: 0, withdrawable: 0, withdrawn: 0, auditing: 0 });
  const [loading, setLoading] = useState(true);

  // Edit profile modal
  const [editVisible, setEditVisible] = useState(false);
  const [editName, setEditName] = useState('');
  const [editPhone, setEditPhone] = useState('');
  const [editAlipay, setEditAlipay] = useState('');
  const [editRealName, setEditRealName] = useState('');
  const [editIdCard, setEditIdCard] = useState('');
  const [saving, setSaving] = useState(false);

  // Change password modal
  const [pwdVisible, setPwdVisible] = useState(false);
  const [oldPwd, setOldPwd] = useState('');
  const [newPwd, setNewPwd] = useState('');
  const [confirmPwd, setConfirmPwd] = useState('');
  const [changingPwd, setChangingPwd] = useState(false);

  const fmt = (v) => (v != null ? Number(v).toFixed(2) : '0.00');

  const fetchData = async () => {
    try {
      const [prof, comm] = await Promise.all([
        getProfile().catch(() => null),
        getCommissionSummary().catch(() => null),
      ]);
      if (prof) setProfile(prof);
      if (comm) setCommission(comm);
    } catch {} finally { setLoading(false); }
  };

  useEffect(() => { fetchData(); }, []);

  // ── Edit Profile ──
  const openEdit = () => {
    setEditName(profile?.merchantName || user?.username || '');
    setEditPhone(profile?.phone || '');
    setEditAlipay(profile?.alipayAccount || '');
    setEditRealName(profile?.realName || '');
    setEditIdCard(profile?.idCardNo || '');
    setEditVisible(true);
  };

  const handleSaveProfile = async () => {
    if (!editName.trim()) { showAlert('提示', '请输入店铺名称'); return; }
    if (editIdCard && !/^\d{17}[\dXx]$/.test(editIdCard.trim())) {
      showAlert('提示', '请输入正确的身份证号码'); return;
    }
    setSaving(true);
    try {
      await updateProfile({
        merchantName: editName.trim(),
        phone: editPhone.trim(),
        alipayAccount: editAlipay.trim(),
        realName: editRealName.trim(),
        idCardNo: editIdCard.trim(),
      });
      setEditVisible(false);
      fetchData();
      showAlert('成功', '信息已更新');
    } catch (e) {
      showAlert('保存失败', e.message || '请稍后重试');
    } finally { setSaving(false); }
  };

  // ── Change Password ──
  const handleChangePwd = async () => {
    if (!oldPwd) { showAlert('提示', '请输入原密码'); return; }
    if (!newPwd) { showAlert('提示', '请输入新密码'); return; }
    if (newPwd.length < 6) { showAlert('提示', '新密码至少6位'); return; }
    if (newPwd !== confirmPwd) { showAlert('提示', '两次密码不一致'); return; }
    setChangingPwd(true);
    try {
      await changePassword({ oldPassword: oldPwd, newPassword: newPwd });
      showAlert('成功', '密码已修改，请重新登录');
      setPwdVisible(false);
      setOldPwd(''); setNewPwd(''); setConfirmPwd('');
      logout();
    } catch (e) {
      showAlert('修改失败', e.message || '请稍后重试');
    } finally { setChangingPwd(false); }
  };

  // ── Logout ──
  const handleLogout = () => {
    showConfirm('退出登录', '确定要退出登录吗？', { destructive: true, onConfirm: logout });
  };

  // ── Customer Service ──
  const handleContact = () => {
    showAlert('联系客服', '客服电话：400-xxx-xxxx\n工作时间：周一至周五 9:00-18:00');
  };

  if (loading) {
    return <View style={styles.loader}><ActivityIndicator size="large" color={Colors.primary} /></View>;
  }

  return (
    <View style={styles.container}>
      <ScrollView contentContainerStyle={{ paddingBottom: 32 }}>
        {/* ── Header ── */}
        <View style={styles.header}>
          <View style={styles.avatar}>
            <Text style={styles.avatarText}>
              {((profile?.merchantName || user?.username) || '商')[0]}
            </Text>
          </View>
          <Text style={styles.merchantName}>{profile?.merchantName || user?.username || '商户'}</Text>
          <Text style={styles.merchantNo}>商户号：{user?.merchantNo || profile?.merchantNo || '—'}</Text>
        </View>

        {/* ── Commission Cards ── */}
        <View style={styles.commissionRow}>
          <View style={styles.commCard}>
            <Text style={styles.commLabel}>累计佣金</Text>
            <Text style={[styles.commValue, { color: Colors.success }]}>¥{fmt(commission.total)}</Text>
          </View>
          <View style={styles.commCard}>
            <Text style={styles.commLabel}>可提现</Text>
            <Text style={[styles.commValue, { color: Colors.primary }]}>¥{fmt(commission.withdrawable)}</Text>
          </View>
        </View>

        {/* ── Menu ── */}
        <View style={styles.menuCard}>
          <MenuItem icon="📱" label="我的码牌" onPress={() => navigation.navigate('Qrcode')} />
          <MenuItem icon="💰" label="佣金明细" onPress={() => navigation.navigate('Commission')} border />
          <MenuItem icon="💸" label="提现明细" onPress={() => navigation.navigate('Withdrawal')} border />
        </View>

        {/* ── Bottom Actions ── */}
        <View style={styles.menuCard}>
          <MenuItem icon="👤" label="编辑资料" onPress={openEdit} />
          <MenuItem icon="🔒" label="修改密码" onPress={() => setPwdVisible(true)} border />
          <MenuItem icon="📞" label="联系客服" onPress={handleContact} border />
        </View>

        {/* ── Logout ── */}
        <TouchableOpacity style={styles.logoutBtn} onPress={handleLogout}>
          <Text style={styles.logoutBtnText}>退出登录</Text>
        </TouchableOpacity>

        <Text style={styles.version}>支付商户端 v1.0.0</Text>
      </ScrollView>

      {/* ── Edit Profile Modal ── */}
      <Modal visible={editVisible} transparent animationType="slide">
        <View style={styles.modalOverlay}>
          <View style={styles.modalPanel}>
            <View style={styles.modalHandle} />
            <Text style={styles.modalTitle}>编辑资料</Text>
            <View style={styles.inputGroup}>
              <Text style={styles.inputLabel}>店铺名称</Text>
              <TextInput style={styles.input} value={editName} onChangeText={setEditName} placeholder="请输入店铺名称" placeholderTextColor={Colors.textPlaceholder} />
            </View>
            <View style={styles.inputGroup}>
              <Text style={styles.inputLabel}>手机号</Text>
              <TextInput style={styles.input} value={editPhone} onChangeText={setEditPhone} placeholder="请输入手机号" placeholderTextColor={Colors.textPlaceholder} keyboardType="phone-pad" maxLength={11} />
            </View>
            <View style={styles.inputGroup}>
              <Text style={styles.inputLabel}>支付宝账号</Text>
              <TextInput style={styles.input} value={editAlipay} onChangeText={setEditAlipay} placeholder="用于佣金提现打款" placeholderTextColor={Colors.textPlaceholder} autoCapitalize="none" />
            </View>
            <View style={styles.inputGroup}>
              <Text style={styles.inputLabel}>真实姓名</Text>
              <TextInput style={styles.input} value={editRealName} onChangeText={setEditRealName} placeholder="请输入真实姓名" placeholderTextColor={Colors.textPlaceholder} />
            </View>
            <View style={styles.inputGroup}>
              <Text style={styles.inputLabel}>身份证号码</Text>
              <TextInput style={styles.input} value={editIdCard} onChangeText={setEditIdCard} placeholder="请输入身份证号码" placeholderTextColor={Colors.textPlaceholder} autoCapitalize="characters" maxLength={18} />
            </View>
            <View style={styles.modalRow}>
              <TouchableOpacity style={styles.cancelBtn} onPress={() => setEditVisible(false)}>
                <Text style={styles.cancelBtnText}>取消</Text>
              </TouchableOpacity>
              <TouchableOpacity style={[styles.confirmBtn, saving && { opacity: 0.6 }]} onPress={handleSaveProfile} disabled={saving}>
                {saving ? <ActivityIndicator color={Colors.white} /> : <Text style={styles.confirmBtnText}>保存</Text>}
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>

      {/* ── Change Password Modal ── */}
      <Modal visible={pwdVisible} transparent animationType="slide">
        <View style={styles.modalOverlay}>
          <View style={styles.modalPanel}>
            <View style={styles.modalHandle} />
            <Text style={styles.modalTitle}>修改密码</Text>
            <View style={styles.inputGroup}>
              <Text style={styles.inputLabel}>原密码</Text>
              <TextInput style={styles.input} value={oldPwd} onChangeText={setOldPwd} placeholder="请输入原密码" placeholderTextColor={Colors.textPlaceholder} secureTextEntry autoCapitalize="none" />
            </View>
            <View style={styles.inputGroup}>
              <Text style={styles.inputLabel}>新密码</Text>
              <TextInput style={styles.input} value={newPwd} onChangeText={setNewPwd} placeholder="至少6位" placeholderTextColor={Colors.textPlaceholder} secureTextEntry autoCapitalize="none" />
            </View>
            <View style={styles.inputGroup}>
              <Text style={styles.inputLabel}>确认新密码</Text>
              <TextInput style={styles.input} value={confirmPwd} onChangeText={setConfirmPwd} placeholder="再次输入新密码" placeholderTextColor={Colors.textPlaceholder} secureTextEntry autoCapitalize="none" />
            </View>
            <View style={styles.modalRow}>
              <TouchableOpacity style={styles.cancelBtn} onPress={() => { setPwdVisible(false); setOldPwd(''); setNewPwd(''); setConfirmPwd(''); }}>
                <Text style={styles.cancelBtnText}>取消</Text>
              </TouchableOpacity>
              <TouchableOpacity style={[styles.confirmBtn, changingPwd && { opacity: 0.6 }]} onPress={handleChangePwd} disabled={changingPwd}>
                {changingPwd ? <ActivityIndicator color={Colors.white} /> : <Text style={styles.confirmBtnText}>确认修改</Text>}
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>
    </View>
  );
}

// ── Menu Item ──
function MenuItem({ icon, label, onPress, border }) {
  return (
    <TouchableOpacity
      style={[styles.menuItem, border && styles.menuItemBorder]}
      onPress={onPress}
      activeOpacity={0.6}
    >
      <View style={styles.menuLeft}>
        <Text style={styles.menuIcon}>{icon}</Text>
        <Text style={styles.menuLabel}>{label}</Text>
      </View>
      <Text style={styles.menuArrow}>›</Text>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.background },
  loader: { flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: Colors.background },

  // Header
  header: { alignItems: 'center', paddingVertical: 28, backgroundColor: Colors.white },
  avatar: {
    width: 68, height: 68, borderRadius: 34, backgroundColor: Colors.primary,
    justifyContent: 'center', alignItems: 'center', marginBottom: 10,
  },
  avatarText: { fontSize: 28, fontWeight: '700', color: Colors.white },
  merchantName: { fontSize: 19, fontWeight: '600', color: Colors.text, marginBottom: 4 },
  merchantNo: { fontSize: 13, color: Colors.textHint },

  // Commission Cards
  commissionRow: { flexDirection: 'row', marginHorizontal: 16, marginTop: 16, gap: 12 },
  commCard: {
    flex: 1, backgroundColor: Colors.white, borderRadius: 14, padding: 18,
    alignItems: 'center',
    shadowColor: '#000', shadowOpacity: 0.04, shadowRadius: 6, elevation: 2,
  },
  commLabel: { fontSize: 12, color: Colors.textHint, marginBottom: 6 },
  commValue: { fontSize: 22, fontWeight: '700' },

  // Menu
  menuCard: {
    backgroundColor: Colors.white, marginHorizontal: 16, marginTop: 14,
    borderRadius: 14, overflow: 'hidden',
    shadowColor: '#000', shadowOpacity: 0.04, shadowRadius: 6, elevation: 2,
  },
  menuItem: {
    flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center',
    paddingVertical: 15, paddingHorizontal: 18,
  },
  menuItemBorder: { borderTopWidth: 1, borderTopColor: Colors.border },
  menuLeft: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  menuIcon: { fontSize: 20 },
  menuLabel: { fontSize: 15, fontWeight: '500', color: Colors.text },
  menuArrow: { fontSize: 20, color: Colors.textHint, fontWeight: '300' },

  // Logout
  logoutBtn: {
    marginHorizontal: 16, marginTop: 20,
    borderWidth: 1, borderColor: Colors.danger, borderRadius: 24,
    paddingVertical: 13, alignItems: 'center',
  },
  logoutBtnText: { color: Colors.danger, fontSize: 15, fontWeight: '600' },
  version: { textAlign: 'center', color: Colors.textHint, fontSize: 12, marginTop: 14 },

  // Modal
  modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.4)', justifyContent: 'flex-end' },
  modalPanel: { backgroundColor: Colors.white, borderTopLeftRadius: 20, borderTopRightRadius: 20, padding: 24, paddingBottom: 36 },
  modalHandle: { width: 36, height: 4, backgroundColor: '#DDD', borderRadius: 2, alignSelf: 'center', marginBottom: 16 },
  modalTitle: { fontSize: 17, fontWeight: '600', textAlign: 'center', marginBottom: 18, color: Colors.text },
  inputGroup: { marginBottom: 14 },
  inputLabel: { fontSize: 13, fontWeight: '500', color: Colors.text, marginBottom: 6 },
  input: { borderWidth: 1, borderColor: Colors.border, borderRadius: 10, paddingHorizontal: 14, paddingVertical: 12, fontSize: 15, color: Colors.text, backgroundColor: Colors.background },
  modalRow: { flexDirection: 'row', gap: 12, marginTop: 4 },
  cancelBtn: { flex: 1, backgroundColor: Colors.background, borderRadius: 24, paddingVertical: 13, alignItems: 'center' },
  cancelBtnText: { color: Colors.text, fontSize: 15, fontWeight: '500' },
  confirmBtn: { flex: 1, backgroundColor: Colors.primary, borderRadius: 24, paddingVertical: 13, alignItems: 'center' },
  confirmBtnText: { color: Colors.white, fontSize: 15, fontWeight: '600' },
});
