import React, { useState, useCallback } from 'react';
import {
  View, Text, StyleSheet, ScrollView, RefreshControl, ActivityIndicator,
} from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { useAuth } from '../store/AuthContext';
import { getRevenueStats } from '../api/statistics';
import { getOrderList } from '../api/order';
import { getCommissionSummary } from '../api/commission';
import Colors from '../theme/colors';

export default function DashboardScreen() {
  const { user } = useAuth();
  const [stats, setStats] = useState({ todayAmount: 0, weekAmount: 0, monthAmount: 0, weekOrders: 0, monthOrders: 0 });
  const [commission, setCommission] = useState({ withdrawable: 0, withdrawn: 0 });
  const [recentOrders, setRecentOrders] = useState([]);
  const [refreshing, setRefreshing] = useState(false);
  const [loading, setLoading] = useState(true);

  const fetchData = async () => {
    try {
      const [revenue, orders, comm] = await Promise.all([
        getRevenueStats().catch(() => null),
        getOrderList({ page: 1, size: 5 }).catch(() => null),
        getCommissionSummary().catch(() => null),
      ]);
      if (revenue) setStats(revenue);
      if (orders?.records) setRecentOrders(orders.records);
      if (comm) setCommission(comm);
    } catch {} finally { setLoading(false); setRefreshing(false); }
  };

  useFocusEffect(useCallback(() => { fetchData(); }, []));

  const onRefresh = () => { setRefreshing(true); fetchData(); };

  const fmt = (v) => (v != null ? Number(v).toFixed(2) : '0.00');

  const statusMap = { 1: '新建', 2: '已支付', 3: '已回调', 4: '已退款', 5: '已失效', 6: '支付失败' };
  const statusColors = {
    1: { bg: Colors.tagBlue, text: Colors.tagBlueText },
    2: { bg: Colors.tagBlue, text: Colors.tagBlueText },
    3: { bg: Colors.tagGreen, text: Colors.tagGreenText },
    4: { bg: Colors.tagRed, text: Colors.tagRedText },
    5: { bg: Colors.tagGray, text: Colors.tagGrayText },
    6: { bg: Colors.tagOrange, text: Colors.tagOrangeText },
  };

  if (loading) {
    return <View style={styles.loader}><ActivityIndicator size="large" color={Colors.primary} /></View>;
  }

  return (
    <ScrollView
      style={styles.container}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} colors={[Colors.primary]} />}
    >
      {/* Welcome Card */}
      <View style={styles.welcomeCard}>
        <Text style={styles.welcomeText}>👋 你好, {user?.username || '商户'}</Text>
        <Text style={styles.welcomeSub}>商户号：{user?.merchantNo || '—'}</Text>
      </View>

      {/* Stat Cards */}
      <View style={styles.statRow}>
        <StatCard label="今日营收" value={fmt(stats.todayAmount)} sub="实时更新" subColor={Colors.success} />
        <StatCard label="本周营收" value={fmt(stats.weekAmount)} sub={`共 ${stats.weekOrders || 0} 笔`} />
      </View>
      <View style={styles.statRow}>
        <StatCard label="本月营收" value={fmt(stats.monthAmount)} sub={`共 ${stats.monthOrders || 0} 笔`} />
        <StatCard label="可提现佣金" value={fmt(commission.withdrawable)} sub={`已提现 ¥${fmt(commission.withdrawn)}`} valueColor={Colors.success} />
      </View>

      {/* Recent Orders */}
      <Text style={styles.sectionTitle}>📋 近期订单</Text>
      {recentOrders.length === 0 ? (
        <View style={styles.emptyCard}><Text style={styles.emptyText}>暂无订单</Text></View>
      ) : (
        recentOrders.map((o) => {
          const sc = statusColors[o.orderStatus] || statusColors[1];
          return (
            <View key={o.orderNo} style={styles.orderCard}>
              <View style={styles.orderLeft}>
                <Text style={styles.orderNo}>{o.orderNo}</Text>
                <Text style={styles.orderTime}>{o.payTime || o.createdAt || ''}</Text>
              </View>
              <View style={styles.orderRight}>
                <Text style={styles.orderAmount}>¥{fmt(o.orderAmount)}</Text>
                <View style={[styles.statusTag, { backgroundColor: sc.bg }]}>
                  <Text style={[styles.statusText, { color: sc.text }]}>{statusMap[o.orderStatus] || '未知'}</Text>
                </View>
              </View>
            </View>
          );
        })
      )}
      <View style={styles.bottomSpacer} />
    </ScrollView>
  );
}

function StatCard({ label, value, sub, subColor, valueColor }) {
  return (
    <View style={styles.statCard}>
      <Text style={styles.statLabel}>{label}</Text>
      <Text style={[styles.statValue, valueColor ? { color: valueColor } : null]}>{`¥${value}`}</Text>
      {sub ? <Text style={[styles.statSub, subColor ? { color: subColor } : null]}>{sub}</Text> : null}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.background },
  loader: { flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: Colors.background },
  welcomeCard: {
    backgroundColor: Colors.primary, marginHorizontal: 16, marginTop: 16,
    borderRadius: 14, padding: 22,
  },
  welcomeText: { fontSize: 20, fontWeight: '700', color: Colors.white },
  welcomeSub: { fontSize: 12, color: 'rgba(255,255,255,0.8)', marginTop: 6 },
  statRow: { flexDirection: 'row', marginHorizontal: 16, marginTop: 12, gap: 10 },
  statCard: {
    flex: 1, backgroundColor: Colors.card, borderRadius: 12,
    padding: 16, shadowColor: '#000', shadowOpacity: 0.04, shadowRadius: 6, elevation: 2,
  },
  statLabel: { fontSize: 12, color: Colors.textHint, marginBottom: 6 },
  statValue: { fontSize: 22, fontWeight: '700', color: Colors.text },
  statSub: { fontSize: 11, color: Colors.textHint, marginTop: 4 },
  sectionTitle: { fontSize: 16, fontWeight: '600', color: Colors.text, marginHorizontal: 16, marginTop: 20, marginBottom: 10 },
  emptyCard: { backgroundColor: Colors.card, marginHorizontal: 16, borderRadius: 10, padding: 32, alignItems: 'center' },
  emptyText: { color: Colors.textHint, fontSize: 14 },
  orderCard: {
    backgroundColor: Colors.card, marginHorizontal: 16, marginBottom: 8,
    borderRadius: 10, padding: 14, flexDirection: 'row', justifyContent: 'space-between',
    shadowColor: '#000', shadowOpacity: 0.03, shadowRadius: 4, elevation: 1,
  },
  orderLeft: { flex: 1 },
  orderNo: { fontSize: 14, fontWeight: '500', color: Colors.text },
  orderTime: { fontSize: 11, color: Colors.textHint, marginTop: 4 },
  orderRight: { alignItems: 'flex-end' },
  orderAmount: { fontSize: 17, fontWeight: '700', color: Colors.text },
  statusTag: { paddingHorizontal: 8, paddingVertical: 3, borderRadius: 10, marginTop: 4 },
  statusText: { fontSize: 11, fontWeight: '500' },
  bottomSpacer: { height: 20 },
});
