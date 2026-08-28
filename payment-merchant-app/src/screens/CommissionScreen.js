import React, { useState, useCallback } from 'react';
import {
  View, Text, StyleSheet, FlatList, TouchableOpacity, TextInput,
  ActivityIndicator, RefreshControl, Modal,
} from 'react-native';
import { showAlert } from '../utils/dialog';
import { useFocusEffect } from '@react-navigation/native';
import { getCommissionList, getCommissionSummary, withdrawCommission } from '../api/commission';
import Colors from '../theme/colors';

export default function CommissionScreen() {
  const [commissions, setCommissions] = useState([]);
  const [summary, setSummary] = useState({ total: 0, withdrawable: 0, withdrawn: 0, auditing: 0 });
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [withdrawVisible, setWithdrawVisible] = useState(false);
  const [withdrawAmount, setWithdrawAmount] = useState('');
  const [withdrawing, setWithdrawing] = useState(false);

  const fmt = (v) => (v != null ? Number(v).toFixed(2) : '0.00');
  const fmtRate = (v) => (v != null ? (Number(v) * 100).toFixed(0) + '%' : '—');

  const fetchData = async () => {
    try {
      const [list, sum] = await Promise.all([
        getCommissionList({ page: 1, size: 50 }).catch(() => null),
        getCommissionSummary().catch(() => null),
      ]);
      if (list?.records) setCommissions(list.records);
      if (sum) setSummary(sum);
    } catch {} finally { setLoading(false); setRefreshing(false); }
  };

  useFocusEffect(useCallback(() => { fetchData(); }, []));

  const handleWithdraw = async () => {
    const amt = parseFloat(withdrawAmount);
    if (!amt || amt <= 0) { showAlert('提示', '请输入有效金额'); return; }
    if (amt > summary.withdrawable) { showAlert('提示', '超出可提现金额'); return; }
    setWithdrawing(true);
    try {
      await withdrawCommission(amt);
      showAlert('成功', '提现申请已提交，请等待审核');
      setWithdrawVisible(false);
      setWithdrawAmount('');
      fetchData();
    } catch (e) {
      showAlert('提现失败', e.message || '请稍后重试');
    } finally { setWithdrawing(false); }
  };

  const renderItem = ({ item }) => {
    return (
      <View style={styles.card}>
        <View style={styles.cardLeft}>
          <Text style={styles.orderNo}>{item.orderNo || item.commissionNo}</Text>
          <Text style={styles.meta}>收款金额 ¥{fmt(item.orderAmount)}</Text>
          <Text style={styles.meta}>比例 {fmtRate(item.commRate)} · {item.createdAt || ''}</Text>
        </View>
        <View style={styles.cardRight}>
          <Text style={styles.commLabel}>佣金</Text>
          <Text style={styles.amount}>¥{fmt(item.commAmount)}</Text>
        </View>
      </View>
    );
  };

  if (loading) {
    return <View style={styles.loader}><ActivityIndicator size="large" color={Colors.primary} /></View>;
  }

  return (
    <View style={styles.container}>
      <FlatList
        data={commissions}
        keyExtractor={(item) => item.commissionNo || String(item.id)}
        renderItem={renderItem}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => { setRefreshing(true); fetchData(); }} colors={[Colors.primary]} />}
        ListHeaderComponent={() => (
          <View>
            {/* Summary */}
            <View style={styles.summaryCard}>
              <Text style={styles.summaryLabel}>累计佣金</Text>
              <Text style={styles.summaryTotal}>¥{fmt(summary.total)}</Text>
              <View style={styles.summaryRow}>
                <SumItem label="可提现" value={fmt(summary.withdrawable)} color={Colors.primary} />
                <SumItem label="已提现" value={fmt(summary.withdrawn)} />
                <SumItem label="审核中" value={fmt(summary.auditing)} color={Colors.warning} />
              </View>
            </View>

            <TouchableOpacity style={styles.withdrawBtn} onPress={() => setWithdrawVisible(true)}>
              <Text style={styles.withdrawBtnText}>发起提现</Text>
            </TouchableOpacity>

            <Text style={styles.sectionTitle}>佣金明细</Text>
          </View>
        )}
        ListEmptyComponent={<View style={styles.empty}><Text style={styles.emptyText}>暂无佣金记录</Text></View>}
        contentContainerStyle={{ paddingBottom: 20 }}
      />

      {/* Withdraw Modal */}
      <Modal visible={withdrawVisible} transparent animationType="slide">
        <View style={styles.modalOverlay}>
          <View style={styles.modalPanel}>
            <View style={styles.modalHandle} />
            <Text style={styles.modalTitle}>发起提现</Text>
            <View style={styles.balanceBox}>
              <Text style={styles.balanceLabel}>可提现金额</Text>
              <Text style={styles.balanceValue}>¥{fmt(summary.withdrawable)}</Text>
            </View>
            <View style={styles.inputGroup}>
              <Text style={styles.inputLabel}>提现金额 *</Text>
              <TextInput
                style={styles.input}
                value={withdrawAmount}
                onChangeText={setWithdrawAmount}
                placeholder="请输入提现金额"
                placeholderTextColor={Colors.textPlaceholder}
                keyboardType="decimal-pad"
              />
            </View>
            <View style={styles.warningBox}>
              <Text style={styles.warningText}>⚠️ 提现需管理员审核，审核通过后自动打款</Text>
            </View>
            <View style={styles.modalRow}>
              <TouchableOpacity style={styles.cancelBtn} onPress={() => setWithdrawVisible(false)}>
                <Text style={styles.cancelBtnText}>取消</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={[styles.confirmBtn, withdrawing && { opacity: 0.6 }]}
                onPress={handleWithdraw}
                disabled={withdrawing}
              >
                {withdrawing ? (
                  <ActivityIndicator color={Colors.white} />
                ) : (
                  <Text style={styles.confirmBtnText}>确认提现</Text>
                )}
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>
    </View>
  );
}

function SumItem({ label, value, color }) {
  return (
    <View style={styles.sumItem}>
      <Text style={[styles.sumItemValue, color ? { color } : null]}>{`¥${value}`}</Text>
      <Text style={styles.sumItemLabel}>{label}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.background },
  loader: { flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: Colors.background },
  summaryCard: { backgroundColor: Colors.white, marginHorizontal: 16, marginTop: 16, borderRadius: 14, padding: 20, alignItems: 'center', shadowColor: '#000', shadowOpacity: 0.04, shadowRadius: 6, elevation: 2 },
  summaryLabel: { fontSize: 13, color: Colors.textHint, marginBottom: 4 },
  summaryTotal: { fontSize: 36, fontWeight: '700', color: Colors.success },
  summaryRow: { flexDirection: 'row', justifyContent: 'space-around', marginTop: 16, width: '100%' },
  sumItem: { alignItems: 'center' },
  sumItemValue: { fontSize: 17, fontWeight: '600', color: Colors.text },
  sumItemLabel: { fontSize: 11, color: Colors.textHint, marginTop: 2 },
  withdrawBtn: { backgroundColor: Colors.primary, marginHorizontal: 16, marginTop: 14, borderRadius: 24, paddingVertical: 14, alignItems: 'center' },
  withdrawBtnText: { color: Colors.white, fontSize: 16, fontWeight: '600' },
  sectionTitle: { fontSize: 15, fontWeight: '600', color: Colors.text, marginHorizontal: 16, marginTop: 18, marginBottom: 10 },
  card: {
    backgroundColor: Colors.white, marginHorizontal: 16, marginBottom: 8, borderRadius: 10, padding: 14,
    flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center',
    shadowColor: '#000', shadowOpacity: 0.03, shadowRadius: 4, elevation: 1,
  },
  cardLeft: { flex: 1 },
  orderNo: { fontSize: 14, fontWeight: '500', color: Colors.text },
  meta: { fontSize: 11, color: Colors.textHint, marginTop: 4 },
  cardRight: { alignItems: 'flex-end' },
  commLabel: { fontSize: 11, color: Colors.textHint, marginBottom: 2 },
  amount: { fontSize: 17, fontWeight: '700', color: Colors.text },
  tag: { paddingHorizontal: 8, paddingVertical: 3, borderRadius: 10, marginTop: 4 },
  tagText: { fontSize: 11, fontWeight: '500' },
  empty: { padding: 48, alignItems: 'center' },
  emptyText: { color: Colors.textHint, fontSize: 14 },
  // Modal
  modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.4)', justifyContent: 'flex-end' },
  modalPanel: { backgroundColor: Colors.white, borderTopLeftRadius: 20, borderTopRightRadius: 20, padding: 24, paddingBottom: 36 },
  modalHandle: { width: 36, height: 4, backgroundColor: '#DDD', borderRadius: 2, alignSelf: 'center', marginBottom: 18 },
  modalTitle: { fontSize: 17, fontWeight: '600', textAlign: 'center', marginBottom: 18, color: Colors.text },
  balanceBox: { backgroundColor: '#F6FFED', borderRadius: 10, padding: 14, alignItems: 'center', marginBottom: 16 },
  balanceLabel: { fontSize: 12, color: Colors.textHint },
  balanceValue: { fontSize: 26, fontWeight: '700', color: Colors.success },
  inputGroup: { marginBottom: 14 },
  inputLabel: { fontSize: 13, fontWeight: '500', color: Colors.text, marginBottom: 6 },
  input: { borderWidth: 1, borderColor: Colors.border, borderRadius: 10, paddingHorizontal: 14, paddingVertical: 12, fontSize: 16, color: Colors.text, backgroundColor: Colors.background },
  warningBox: { backgroundColor: '#FFFBE6', borderRadius: 8, padding: 10, marginBottom: 18 },
  warningText: { fontSize: 12, color: Colors.textHint },
  modalRow: { flexDirection: 'row', gap: 12 },
  cancelBtn: { flex: 1, backgroundColor: Colors.background, borderRadius: 24, paddingVertical: 14, alignItems: 'center' },
  cancelBtnText: { color: Colors.text, fontSize: 15, fontWeight: '500' },
  confirmBtn: { flex: 1, backgroundColor: Colors.primary, borderRadius: 24, paddingVertical: 14, alignItems: 'center' },
  confirmBtnText: { color: Colors.white, fontSize: 15, fontWeight: '600' },
});
