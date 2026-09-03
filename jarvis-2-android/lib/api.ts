import * as SecureStore from 'expo-secure-store';
const KEY='jarvis_api_url';
const DEFAULT='http://10.0.2.2:8787';
export async function getApiUrl(){return (await SecureStore.getItemAsync(KEY)) || DEFAULT;}
export async function setApiUrl(url:string){await SecureStore.setItemAsync(KEY,url.replace(/\/$/,''));}
export async function chat(text:string){const base=await getApiUrl();const r=await fetch(`${base}/api/chat`,{method:'POST',headers:{'content-type':'application/json'},body:JSON.stringify({text})});if(!r.ok) throw new Error(`API ${r.status}`);return r.json();}
export async function health(){const base=await getApiUrl();const r=await fetch(`${base}/api/health`);if(!r.ok) throw new Error(`API ${r.status}`);return r.json();}
