import React from 'react';
import { StatusBar } from 'react-native';
import { AuthProvider } from './src/store/AuthContext';
import AppNavigator from './src/navigation/AppNavigator';
import PaymentVoiceNotifier from './src/components/PaymentVoiceNotifier';
import Colors from './src/theme/colors';

export default function App() {
  return (
    <AuthProvider>
      <StatusBar
        barStyle="dark-content"
        backgroundColor={Colors.white}
        translucent={false}
      />
      <PaymentVoiceNotifier />
      <AppNavigator />
    </AuthProvider>
  );
}
