import React, { useRef, useEffect } from 'react';
import { ActivityIndicator, View, Text } from 'react-native';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { Ionicons } from '@expo/vector-icons';
import { useAuth } from '../store/AuthContext';
import Colors from '../theme/colors';

import LoginScreen from '../screens/LoginScreen';
import DashboardScreen from '../screens/DashboardScreen';
import OrdersScreen from '../screens/OrdersScreen';
import QrcodeScreen from '../screens/QrcodeScreen';
import CommissionScreen from '../screens/CommissionScreen';
import WithdrawalScreen from '../screens/WithdrawalScreen';
import ProfileScreen from '../screens/ProfileScreen';

const Stack = createNativeStackNavigator();
const Tab = createBottomTabNavigator();

const TAB_ICONS = {
  'DashboardTab': { active: 'home', inactive: 'home-outline' },
  'OrdersTab':   { active: 'receipt', inactive: 'receipt-outline' },
  'ProfileTab':  { active: 'person', inactive: 'person-outline' },
};

function TabIcon({ routeName, focused, color }) {
  const icon = TAB_ICONS[routeName] || { active: 'ellipse', inactive: 'ellipse-outline' };
  return <Ionicons name={focused ? icon.active : icon.inactive} size={24} color={color} />;
}

function MainTabs() {
  return (
    <Tab.Navigator
      screenOptions={{
        headerStyle: { backgroundColor: Colors.white },
        headerTitleStyle: { color: Colors.text, fontSize: 17, fontWeight: '600' },
        headerShadowVisible: false,
        tabBarActiveTintColor: Colors.primary,
        tabBarInactiveTintColor: Colors.textHint,
        tabBarStyle: {
          backgroundColor: Colors.white,
          borderTopColor: Colors.border,
          borderTopWidth: 1,
          paddingBottom: 8,
          paddingTop: 8,
          height: 66,
        },
        tabBarLabelStyle: { fontSize: 12, fontWeight: '500' },
      }}
    >
      <Tab.Screen name="DashboardTab" component={DashboardScreen}
        options={{ title: '首页', tabBarLabel: '首页',
          tabBarIcon: ({ focused, color }) => <TabIcon routeName="DashboardTab" focused={focused} color={color} />, }} />
      <Tab.Screen name="OrdersTab" component={OrdersScreen}
        options={{ title: '订单', tabBarLabel: '订单',
          tabBarIcon: ({ focused, color }) => <TabIcon routeName="OrdersTab" focused={focused} color={color} />, }} />
      <Tab.Screen name="ProfileTab" component={ProfileScreen}
        options={{ title: '我的', tabBarLabel: '我的',
          tabBarIcon: ({ focused, color }) => <TabIcon routeName="ProfileTab" focused={focused} color={color} />, }} />
    </Tab.Navigator>
  );
}

export default function AppNavigator() {
  const { user, loading } = useAuth();
  const navigationRef = useRef(null);

  // When auth state changes, navigate to the appropriate screen
  useEffect(() => {
    if (loading) return;
    if (navigationRef.current) {
      if (user) {
        navigationRef.current.reset({ index: 0, routes: [{ name: 'Main' }] });
      } else {
        navigationRef.current.reset({ index: 0, routes: [{ name: 'Login' }] });
      }
    }
  }, [user, loading]);

  if (loading) {
    return (
      <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: Colors.background }}>
        <ActivityIndicator size="large" color={Colors.primary} />
      </View>
    );
  }

  return (
    <NavigationContainer ref={navigationRef}>
      <Stack.Navigator screenOptions={{ headerShown: false }}>
        <Stack.Screen name="Login" component={LoginScreen} />
        <Stack.Screen name="Main" component={MainTabs} />
        <Stack.Screen name="Qrcode" component={QrcodeScreen}
          options={{ title: '我的码牌', headerShown: true, headerTintColor: Colors.primary }} />
        <Stack.Screen name="Commission" component={CommissionScreen}
          options={{ title: '佣金明细', headerShown: true, headerTintColor: Colors.primary }} />
        <Stack.Screen name="Withdrawal" component={WithdrawalScreen}
          options={{ title: '提现明细', headerShown: true, headerTintColor: Colors.primary }} />
      </Stack.Navigator>
    </NavigationContainer>
  );
}
