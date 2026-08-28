import React, { useState, useCallback } from 'react';
import {
  View, Text, StyleSheet, FlatList, TextInput, TouchableOpacity,
  ActivityIndicator, RefreshControl,
} from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { getOrderList } from '../api/order';
import Colors from '../theme/colors';

const PAGE_SIZE = 15;

const STATUSES = [
  { label: '全部', value: null },
  { label: '已支付', value: 2 },
  { label: '已回调', value: 3 },
  { label: '已退款', value: 4 },
  { label: '已失效', value: 5 },
  { label: '支付失败', value: 6 },
];

const STATUS_MAP = { 1: '新建', 2: '已支付', 3: '已回调', 4: '已退款', 5: '已失效', 6: '支付失败' };
const STATUS_COLORS = {
  1: { bg: Colors.tagBlue, text: Colors.tagBlueText },
  2: { bg: Colors.tagBlue, text: Colors.tagBlueText },
  3: { bg: Colors.tagGreen, text: Colors.tagGreenText },
  4: { bg: Colors.tagRed, text: Colors.tagRedText },
  5: { bg: Colors.tagGray, text: Colors.tagGrayText },
  6: { bg: Colors.tagOrange, text: Colors.tagOrangeText },
};

export default function OrdersScreen() {
  const [orders, setOrders] = useState([]);
  const [page, setPage] = useState(1);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [searchText, setSearchText] = useState('');
  const [statusFilter, setStatusFilter] = useState(null);

  const fetchOrders = async (p = 1, append = false) => {
    try {
      const params = { page: p, size: PAGE_SIZE };
      if (searchText) params.orderNo = searchText;
      if (statusFilter) params.orderStatus = statusFilter;
      const result = await getOrderList(params);
      setOrders(append ? (prev) => [...prev, ...(result?.records || [])] : (result?.records || []));
      setTotal(result?.total || 0);
      setPage(p);
    } catch {} finally {
      setLoading(false); setRefreshing(false); setLoadingMore(false);
    }
  };

  useFocusEffect(useCallback(() => { fetchOrders(1); }, [statusFilter]));

  const onRefresh = () => { setRefreshing(true); fetchOrders(1); };
  const onEndReached = () => {
    if (loadingMore || orders.length >= total) return;
    setLoadingMore(true);
    fetchOrders(page + 1, true);
  };

  const handleSearch = () => fetchOrders(1);

  const fmt = (v) => (v != null ? Number(v).toFixed(2) : '0.00');

  const renderItem = ({ item }) => {
    const sc = STATUS_COLORS[item.orderStatus] || STATUS_COLORS[1];
    return (
      <View style={styles.card}>
        <View style={styles.cardTop}>
          <Text style={styles.orderNo}>{item.orderNo}</Text>
          <View style={[styles.tag, { backgroundColor: sc.bg }]}>
            <Text style={[styles.tagText, { color: sc.text }]}>{STATUS_MAP[item.orderStatus] || '未知'}</Text>
          </View>
        </View>
        <View style={styles.cardBottom}>
          <Text style={styles.product}>{item.productName || '扫码支付'}</Text>
          <Text style={styles.amount}>¥{fmt(item.orderAmount)}</Text>
        </View>
        {item.payTime ? <Text style={styles.time}>{item.payTime}</Text> : null}
      </View>
    );
  };

  const renderFooter = () => {
    if (!loadingMore) return null;
    return <ActivityIndicator style={{ padding: 16 }} color={Colors.primary} />;
  };

  if (loading) {
    return <View style={styles.loader}><ActivityIndicator size="large" color={Colors.primary} /></View>;
  }

  return (
    <View style={styles.container}>
      {/* ── Fixed Header ── */}
      <View style={styles.headerSection}>
        {/* Search */}
        <View style={styles.searchRow}>
          <TextInput
            style={styles.searchInput}
            value={searchText}
            onChangeText={setSearchText}
            placeholder="搜索订单号"
            placeholderTextColor={Colors.textPlaceholder}
            onSubmitEditing={handleSearch}
            returnKeyType="search"
          />
          <TouchableOpacity style={styles.searchBtn} onPress={handleSearch}>
            <Text style={styles.searchBtnText}>查询</Text>
          </TouchableOpacity>
        </View>

        {/* Status filter chips */}
        <FlatList
          horizontal
          data={STATUSES}
          showsHorizontalScrollIndicator={false}
          style={styles.chipList}
          contentContainerStyle={styles.chipListContent}
          keyExtractor={(item) => String(item.value)}
          renderItem={({ item }) => (
            <TouchableOpacity
              style={[styles.chip, statusFilter === item.value && styles.chipActive]}
              onPress={() => setStatusFilter(item.value)}
            >
              <Text style={[styles.chipText, statusFilter === item.value && styles.chipTextActive]}>{item.label}</Text>
            </TouchableOpacity>
          )}
        />
      </View>

      {/* ── Order list ── */}
      <FlatList
        data={orders}
        keyExtractor={(item) => item.orderNo || String(item.id)}
        renderItem={renderItem}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} colors={[Colors.primary]} />}
        onEndReached={onEndReached}
        onEndReachedThreshold={0.3}
        ListFooterComponent={renderFooter}
        ListEmptyComponent={<View style={styles.empty}><Text style={styles.emptyText}>暂无订单</Text></View>}
        contentContainerStyle={styles.listContent}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.background },
  loader: { flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: Colors.background },
  // Fixed header
  headerSection: { backgroundColor: Colors.background, paddingBottom: 4 },
  searchRow: { flexDirection: 'row', paddingHorizontal: 16, paddingTop: 12, gap: 10 },
  searchInput: {
    flex: 1, borderWidth: 1, borderColor: Colors.border, borderRadius: 10,
    paddingHorizontal: 14, paddingVertical: 10, fontSize: 14, color: Colors.text,
    backgroundColor: Colors.white,
  },
  searchBtn: {
    backgroundColor: Colors.primary, borderRadius: 10, paddingHorizontal: 20,
    justifyContent: 'center', alignItems: 'center',
  },
  searchBtnText: { color: Colors.white, fontSize: 14, fontWeight: '600' },
  chipList: { flexGrow: 0 },
  chipListContent: { paddingHorizontal: 16, paddingVertical: 10 },
  chip: {
    paddingHorizontal: 14, paddingVertical: 6, borderRadius: 16,
    backgroundColor: Colors.white, borderWidth: 1, borderColor: Colors.border, marginRight: 8,
  },
  chipActive: { backgroundColor: Colors.primary, borderColor: Colors.primary },
  chipText: { fontSize: 13, color: Colors.textHint },
  chipTextActive: { color: Colors.white },
  // List
  listContent: { paddingTop: 4, paddingBottom: 20 },
  card: {
    backgroundColor: Colors.white, marginHorizontal: 16, marginBottom: 8,
    borderRadius: 10, padding: 14, shadowColor: '#000', shadowOpacity: 0.03, shadowRadius: 4, elevation: 1,
  },
  cardTop: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 },
  orderNo: { fontSize: 14, fontWeight: '500', color: Colors.text },
  tag: { paddingHorizontal: 8, paddingVertical: 3, borderRadius: 10 },
  tagText: { fontSize: 11, fontWeight: '500' },
  cardBottom: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  product: { fontSize: 13, color: Colors.textSecondary },
  amount: { fontSize: 18, fontWeight: '700', color: Colors.text },
  time: { fontSize: 11, color: Colors.textHint, marginTop: 6 },
  empty: { padding: 48, alignItems: 'center' },
  emptyText: { color: Colors.textHint, fontSize: 14 },
});
