import * as Notifications from 'expo-notifications';
import Constants from 'expo-constants';
Notifications.setNotificationHandler({handleNotification:async()=>({shouldPlaySound:true,shouldSetBadge:true,shouldShowBanner:true,shouldShowList:true})});
export async function registerPush(){const p=await Notifications.getPermissionsAsync();if(!p.granted){const q=await Notifications.requestPermissionsAsync();if(!q.granted)return null;}const projectId=Constants.easConfig?.projectId||Constants.expoConfig?.extra?.eas?.projectId;try{return(await Notifications.getExpoPushTokenAsync(projectId?{projectId}:undefined)).data}catch{return null}}
