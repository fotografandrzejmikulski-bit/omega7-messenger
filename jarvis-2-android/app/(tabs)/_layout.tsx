import {Tabs} from 'expo-router';
import {C} from '@/components/ui';
export default function TabsLayout(){return <Tabs screenOptions={{headerShown:false,tabBarStyle:{backgroundColor:C.panel,borderTopColor:C.line},tabBarActiveTintColor:C.cyan,tabBarInactiveTintColor:C.muted}}><Tabs.Screen name="home" options={{title:'Home'}}/><Tabs.Screen name="agents" options={{title:'Agents'}}/><Tabs.Screen name="tasks" options={{title:'Tasks'}}/><Tabs.Screen name="memory" options={{title:'Memory'}}/><Tabs.Screen name="settings" options={{title:'Settings'}}/></Tabs>}
