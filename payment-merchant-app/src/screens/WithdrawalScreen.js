import React, { useState, useCallback } from 'react';
import {
  View, Text, StyleSheet, FlatList, ActivityIndicator, RefreshControl,
} from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { getWithdrawalList } from '../api/withdrawal';
import Colors from '../theme/colors';

const WITHDRAW_STATUS = { 1: '待审核', 2: '已打款', 3: '已驳回' };
const STATUS_COLORS = {
  1: { bg: Colors.tagOrange, text: Colors.tagOrangeText },
  2: { bg: Colors.tagGreen, text: Colors.tagGreenText },
  3: { bg: Colors.tagRed, text: Colors.tagRedText },
};

export default function WithdrawalScreen() {
  const [withdrawals, setWithdrawals] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  const fmt = (v) => (v != null ? Number(v).toFixed(2) : '0.00');

  const fetchData = async () => {
    try {
      const result = await getWithdrawalList({ page: 1, size: 50 });
      setWithdrawals(result?.records || []);
    } catch {} finally { setLoading(false); setRefreshing(false); }
  };

  useFocusEffect(useCallback(() => { fetchData(); }, []));

  const renderItem = ({ item }) => {
    const sc = STATUS_COLORS[item.status] || STATUS_COLORS[1];
    return (
      <View style={styles.card}>
        <View style={styles.cardTop}>
          <Text style={styles.withdrawalNo}>{item.withdrawalNo}</Text>
          <View style={[styles.tag, { backgroundColor: sc.bg }]}>
            <Text style={[styles.tagText, { color: sc.text }]}>{WITHDRAW_STATUS[item.status] || '未知'}</Text>
          </View>
        </View>
        <View style={styles.cardBottom}>
          <View style={styles.cardLeft}>
            <Text style={styles.meta}>支付宝账号：{item.alipayAccount || '—'}</Text>
            <Text style={styles.meta}>申请时间：{item.createdAt || '—'}</Text>
            {item.rejectReason ? <Text style={styles.reject}>驳回原因：{item.rejectReason}</Text> : null}
          </View>
          <Text style={styles.amount}>¥{fmt(item.amount)}</Text>
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
        data={withdrawals}
        keyExtractor={(item) => item.withdrawalNo || String(item.id)}
        renderItem={renderItem}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => { setRefreshing(true); fetchData(); }} colors={[Colors.primary]} />}
        ListEmptyComponent={<View style={styles.empty}><Text style={styles.emptyText}>暂无提现记录</Text></View>}
        contentContainerStyle={{ paddingTop: 12, paddingBottom: 20 }}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.background },
  loader: { flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: Colors.background },
  card: {
    backgroundColor: Colors.white, marginHorizontal: 16, marginBottom: 8, borderRadius: 10, padding: 14,
    shadowColor: '#000', shadowOpacity: 0.03, shadowRadius: 4, elevation: 1,
  },
  cardTop: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 },
  withdrawalNo: { fontSize: 14, fontWeight: '500', color: Colors.text },
  tag: { paddingHorizontal: 8, paddingVertical: 3, borderRadius: 10 },
  tagText: { fontSize: 11, fontWeight: '500' },
  cardBottom: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  cardLeft: { flex: 1 },
  meta: { fontSize: 11, color: Colors.textHint, marginTop: 3 },
  reject: { fontSize: 11, color: Colors.danger, marginTop: 3 },
  amount: { fontSize: 18, fontWeight: '700', color: Colors.text },
  empty: { padding: 48, alignItems: 'center' },
  emptyText: { color: Colors.textHint, fontSize: 14 },
});
