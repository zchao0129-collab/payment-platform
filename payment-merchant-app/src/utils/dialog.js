import { Alert, Platform } from 'react-native';

// react-native-web 的 Alert.alert 是空实现（no-op），
// 因此在 Web（H5）端回退到浏览器原生 window.alert / window.confirm，
// 保证「退出登录」等确认/提示弹窗在 H5 上也能正常弹出。

const isWeb = Platform.OS === 'web';

export function showAlert(title, message) {
  if (isWeb) {
    window.alert(message ? `${title}\n\n${message}` : title);
  } else {
    Alert.alert(title, message);
  }
}

export function showConfirm(title, message, {
  confirmText = '确定',
  cancelText = '取消',
  destructive = false,
  onConfirm,
  onCancel,
} = {}) {
  if (isWeb) {
    // 浏览器原生 confirm 只有「确定/取消」，按钮文案由浏览器语言决定
    const ok = window.confirm(message ? `${title}\n\n${message}` : title);
    if (ok) onConfirm && onConfirm();
    else onCancel && onCancel();
  } else {
    Alert.alert(title, message, [
      { text: cancelText, style: 'cancel', onPress: onCancel },
      { text: confirmText, style: destructive ? 'destructive' : 'default', onPress: onConfirm },
    ]);
  }
}
