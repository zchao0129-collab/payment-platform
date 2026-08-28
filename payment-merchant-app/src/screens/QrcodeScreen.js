import React, { useState, useCallback } from 'react';
import {
  View, Text, StyleSheet, TouchableOpacity, Image,
  ActivityIndicator, ScrollView, RefreshControl,
  Modal, useWindowDimensions,
} from 'react-native';
import { showAlert, showConfirm } from '../utils/dialog';
import { useFocusEffect } from '@react-navigation/native';
import { getMyQrcode, regenerateQrcode } from '../api/qrcode';
import Colors from '../theme/colors';

export default function QrcodeScreen() {
  const [qrcode, setQrcode] = useState(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [regenerating, setRegenerating] = useState(false);
  const [guideOpen, setGuideOpen] = useState(false);
  const [previewVisible, setPreviewVisible] = useState(false);

  // 码牌图片宽高比 600:780，按手机屏幕自适应，避免预览过大无法完整查看
  const { width: winW, height: winH } = useWindowDimensions();
  const QR_ASPECT = 600 / 780;
  const previewW = Math.min(winW - 40, winH * 0.72 * QR_ASPECT);
  const previewH = previewW / QR_ASPECT;

  const fetchQrcode = async () => {
    try {
      const result = await getMyQrcode();
      setQrcode(result);
    } catch {} finally { setLoading(false); setRefreshing(false); }
  };

  useFocusEffect(useCallback(() => { fetchQrcode(); }, []));

  const handleRegenerate = () => {
    showConfirm('重新生成码牌', '重新生成后旧码牌将立即失效，确定继续吗？', {
      destructive: true,
      onConfirm: async () => {
        setRegenerating(true);
        try {
          const result = await regenerateQrcode();
          setQrcode(result);
          showAlert('成功', '码牌已重新生成');
        } catch (e) {
          showAlert('失败', e.message || '请稍后重试');
        } finally { setRegenerating(false); }
      },
    });
  };

  const handleDownload = () => {
    showAlert('保存码牌', '长按上方码牌图片即可保存到相册，或截图保存。');
  };

  if (loading) {
    return <View style={styles.loader}><ActivityIndicator size="large" color={Colors.primary} /></View>;
  }

  const qrcodeUrl = qrcode?.qrcodeImage || qrcode?.qrcodeData;
  const hasQrcode = !!qrcodeUrl;

  return (
    <ScrollView
      style={styles.container}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => { setRefreshing(true); fetchQrcode(); }} colors={[Colors.primary]} />}
      contentContainerStyle={{ paddingBottom: 32 }}
    >
      {/* QR Code Card */}
      <View style={styles.card}>
        <Text style={styles.cardTitle}>我的收款码牌</Text>
        {hasQrcode ? (
          <>
            <TouchableOpacity
              style={styles.qrWrapper}
              onPress={() => setPreviewVisible(true)}
              activeOpacity={0.8}
            >
              <Image
                source={{ uri: qrcodeUrl }}
                style={styles.qrImage}
                resizeMode="contain"
              />
              <View style={styles.previewHint}>
                <Text style={styles.previewHintText}>🔍 点击预览大图</Text>
              </View>
            </TouchableOpacity>
            <Text style={styles.qrHint}>微信 / 支付宝 扫码均可支付</Text>
            <View style={styles.btnRow}>
              <TouchableOpacity style={styles.outlineBtn} onPress={handleDownload}>
                <Text style={styles.outlineBtnText}>💾 保存到相册</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={[styles.outlineBtn, regenerating && { opacity: 0.6 }]}
                onPress={handleRegenerate}
                disabled={regenerating}
              >
                {regenerating ? (
                  <ActivityIndicator size="small" color={Colors.primary} />
                ) : (
                  <Text style={styles.dangerBtnText}>🔄 重新生成</Text>
                )}
              </TouchableOpacity>
            </View>
          </>
        ) : (
          <View style={styles.emptyQr}>
            <Text style={styles.emptyIcon}>📷</Text>
            <Text style={styles.emptyQrText}>暂无收款码牌</Text>
            <Text style={styles.emptyQrSub}>请联系管理员为您生成码牌</Text>
          </View>
        )}
      </View>

      {/* Usage Guide */}
      <View style={styles.card}>
        <TouchableOpacity
          style={styles.guideHeader}
          onPress={() => setGuideOpen(!guideOpen)}
          activeOpacity={0.7}
        >
          <Text style={styles.guideTitle}>📖 使用指南</Text>
          <Text style={styles.guideArrow}>{guideOpen ? '▲' : '▼'}</Text>
        </TouchableOpacity>
        {guideOpen && (
          <View style={styles.guideBody}>
            <GuideStep num="1" text="将收款码保存到手机相册或打印出来" />
            <GuideStep num="2" text="顾客使用微信或支付宝扫描码牌" />
            <GuideStep num="3" text="顾客输入金额并确认支付" />
            <GuideStep num="4" text="支付完成后资金自动结算到您的账户" />
            <GuideStep num="5" text="在「佣金」页面查看提现" />
            <View style={styles.note}>
              <Text style={styles.noteText}>
                💡 码牌长期有效，请勿泄露给非顾客人员。如发现异常交易，请立即联系管理员冻结码牌。
              </Text>
            </View>
          </View>
        )}
      </View>
      {/* ── Full-screen Preview Modal ── */}
      <Modal
        visible={previewVisible}
        transparent={false}
        animationType="fade"
        onRequestClose={() => setPreviewVisible(false)}
        statusBarTranslucent
      >
        <View style={styles.previewOverlay}>
          {/* Top bar */}
          <View style={styles.previewTopBar}>
            <Text style={styles.previewTitle}>收款码牌</Text>
            <TouchableOpacity
              style={styles.previewCloseBtn}
              onPress={() => setPreviewVisible(false)}
            >
              <Text style={styles.previewCloseText}>✕</Text>
            </TouchableOpacity>
          </View>

          {/* QR Code Image — 自适应屏幕尺寸 */}
          <View style={[styles.previewImageContainer, { width: previewW, height: previewH }]}>
            {hasQrcode && (
              <Image
                source={{ uri: qrcodeUrl }}
                style={styles.previewImage}
                resizeMode="contain"
              />
            )}
          </View>

          {/* Bottom hint */}
          <View style={styles.previewBottom}>
            <Text style={styles.previewBottomHint}>微信 / 支付宝 扫码均可支付</Text>
            <Text style={styles.previewBottomSub}>请顾客使用手机扫码完成付款</Text>
          </View>
        </View>
      </Modal>
    </ScrollView>
  );
}

function GuideStep({ num, text }) {
  return (
    <View style={styles.step}>
      <View style={styles.stepNum}>
        <Text style={styles.stepNumText}>{num}</Text>
      </View>
      <Text style={styles.stepText}>{text}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.background },
  loader: { flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: Colors.background },
  card: {
    backgroundColor: Colors.white, marginHorizontal: 16, marginTop: 16,
    borderRadius: 14, padding: 20,
    shadowColor: '#000', shadowOpacity: 0.04, shadowRadius: 6, elevation: 2,
  },
  cardTitle: { fontSize: 16, fontWeight: '600', color: Colors.text, marginBottom: 16, textAlign: 'center' },
  qrWrapper: {
    backgroundColor: Colors.background, borderRadius: 12, padding: 12,
    alignItems: 'center', marginBottom: 12,
  },
  qrImage: { width: 220, height: 220, borderRadius: 8 },
  qrHint: { fontSize: 13, color: Colors.textHint, textAlign: 'center', marginBottom: 16 },
  btnRow: { flexDirection: 'row', gap: 12 },
  outlineBtn: {
    flex: 1, borderWidth: 1, borderColor: Colors.border, borderRadius: 24,
    paddingVertical: 12, alignItems: 'center',
  },
  outlineBtnText: { fontSize: 14, color: Colors.primary, fontWeight: '500' },
  dangerBtnText: { fontSize: 14, color: Colors.danger, fontWeight: '500' },
  emptyQr: { alignItems: 'center', paddingVertical: 40 },
  emptyIcon: { fontSize: 48, marginBottom: 12 },
  emptyQrText: { fontSize: 16, fontWeight: '600', color: Colors.text, marginBottom: 6 },
  emptyQrSub: { fontSize: 13, color: Colors.textHint },
  // Guide
  guideHeader: {
    flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center',
  },
  guideTitle: { fontSize: 16, fontWeight: '600', color: Colors.text },
  guideArrow: { fontSize: 12, color: Colors.textHint },
  guideBody: { marginTop: 14, paddingTop: 14, borderTopWidth: 1, borderTopColor: Colors.border },
  step: { flexDirection: 'row', alignItems: 'flex-start', marginBottom: 12, gap: 12 },
  stepNum: {
    width: 24, height: 24, borderRadius: 12, backgroundColor: Colors.primary,
    justifyContent: 'center', alignItems: 'center',
  },
  stepNumText: { color: Colors.white, fontSize: 13, fontWeight: '600' },
  stepText: { flex: 1, fontSize: 14, color: Colors.text, lineHeight: 22 },
  note: { backgroundColor: '#FFFBE6', borderRadius: 8, padding: 12, marginTop: 4 },
  noteText: { fontSize: 12, color: Colors.textSecondary, lineHeight: 20 },
  // Preview hint
  previewHint: {
    position: 'absolute', bottom: 8, right: 12,
    backgroundColor: 'rgba(0,0,0,0.45)', borderRadius: 10,
    paddingHorizontal: 8, paddingVertical: 4,
  },
  previewHintText: { color: '#fff', fontSize: 11 },
  // Preview Modal
  previewOverlay: {
    flex: 1, backgroundColor: '#1a1a1a',
    justifyContent: 'center', alignItems: 'center',
  },
  previewTopBar: {
    position: 'absolute', top: 0, left: 0, right: 0,
    flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center',
    paddingTop: 48, paddingBottom: 12, paddingHorizontal: 20,
    backgroundColor: 'rgba(0,0,0,0.3)',
    zIndex: 10,
  },
  previewTitle: { color: '#fff', fontSize: 17, fontWeight: '600' },
  previewCloseBtn: {
    width: 36, height: 36, borderRadius: 18,
    backgroundColor: 'rgba(255,255,255,0.15)',
    justifyContent: 'center', alignItems: 'center',
  },
  previewCloseText: { color: '#fff', fontSize: 18, fontWeight: '600' },
  previewImageContainer: {
    backgroundColor: '#fff', borderRadius: 16,
    justifyContent: 'center', alignItems: 'center',
    overflow: 'hidden',
  },
  previewImage: { width: '100%', height: '100%' },
  previewBottom: {
    position: 'absolute', bottom: 60, alignItems: 'center',
  },
  previewBottomHint: { color: 'rgba(255,255,255,0.8)', fontSize: 15, fontWeight: '500' },
  previewBottomSub: { color: 'rgba(255,255,255,0.45)', fontSize: 12, marginTop: 6 },
});
