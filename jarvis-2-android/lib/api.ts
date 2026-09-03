import * as SecureStore from 'expo-secure-store';
import * as FileSystem from 'expo-file-system/legacy';
const KEY='jarvis_api_url'; const TOKEN_KEY='jarvis_api_token'; const DEFAULT='http://10.0.2.2:8787';
async function request(path:string, init:RequestInit={}){const base=await getApiUrl();const token=await SecureStore.getItemAsync(TOKEN_KEY);const headers=new Headers(init.headers);headers.set('content-type','application/json');if(token)headers.set('authorization',`Bearer ${token}`);const r=await fetch(`${base}${path}`,{...init,headers});if(!r.ok)throw new Error(`API ${r.status}`);return r.json();}
export async function getApiUrl(){return (await SecureStore.getItemAsync(KEY))||DEFAULT;}
export async function setApiUrl(url:string){await SecureStore.setItemAsync(KEY,url.replace(/\/$/,''));}
export async function setApiToken(token:string){if(token)await SecureStore.setItemAsync(TOKEN_KEY,token);else await SecureStore.deleteItemAsync(TOKEN_KEY);}
export const chat=(text:string)=>request('/api/chat',{method:'POST',body:JSON.stringify({text})});
export const health=()=>request('/api/health');
export const getTasks=()=>request('/api/tasks');
export const getMemory=()=>request('/api/memory');
export const getApprovals=()=>request('/api/approvals');
export const getAgents=()=>request('/api/agents');
export const getAutomations=()=>request('/api/automations');
export const createAutomation=(name:string,agent:string,schedule:string)=>request('/api/automations',{method:'POST',body:JSON.stringify({name,agent,schedule})});
export const updateAutomation=(id:string,patch:any)=>request(`/api/automations/${id}`,{method:'PATCH',body:JSON.stringify(patch)});
export const createTask=(title:string,priority='medium')=>request('/api/tasks',{method:'POST',body:JSON.stringify({title,priority})});
export const remember=(text:string)=>request('/api/memory',{method:'POST',body:JSON.stringify({text})});
export const decideApproval=(id:string,status:'approved'|'rejected')=>request(`/api/approvals/${id}`,{method:'PATCH',body:JSON.stringify({status})});
export async function transcribe(uri:string){const base=await getApiUrl();const token=await SecureStore.getItemAsync(TOKEN_KEY);const r=await FileSystem.uploadAsync(`${base}/api/transcribe`,uri,{fieldName:'file',httpMethod:'POST',uploadType:FileSystem.FileSystemUploadType.BINARY_CONTENT,mimeType:'audio/m4a',headers:token?{authorization:`Bearer ${token}`}:{}});if(r.status<200||r.status>=300)throw new Error(`API ${r.status}`);return JSON.parse(r.body);}
