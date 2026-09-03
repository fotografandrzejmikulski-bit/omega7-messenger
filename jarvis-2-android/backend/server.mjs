import http from 'node:http';
import crypto from 'node:crypto';
const PORT=Number(process.env.PORT||8787), TOKEN=process.env.JARVIS_API_TOKEN||'', CORS=process.env.CORS_ORIGIN||'*';
const STATE_FILE=process.env.JARVIS_STATE_FILE||'./jarvis-state.json';
const agents=[['01','Triage / Router'],['02','Executive'],['03','Research'],['04','Email'],['05','Calendar'],['06','Finance'],['07','Contacts'],['08','Data / Logic'],['09','Memory'],['10','Voice'],['11','Policy']];
let state={tasks:[],memory:[],approvals:[],audit:[],push_tokens:[]};
try{state=JSON.parse(await import('node:fs/promises').then(x=>x.readFile(STATE_FILE,'utf8')))}catch{}
const fs=await import('node:fs/promises');
const id=()=>crypto.randomUUID();
async function save(){await fs.writeFile(STATE_FILE,JSON.stringify(state,null,2))}
function json(res,status,payload,headers={}){res.writeHead(status,{'content-type':'application/json; charset=utf-8','access-control-allow-origin':CORS,'access-control-allow-headers':'content-type,authorization','access-control-allow-methods':'GET,POST,PATCH,OPTIONS',...headers});res.end(JSON.stringify(payload))}
function auth(req){return !TOKEN||req.headers.authorization===`Bearer ${TOKEN}`}
async function read(req,limit=25_000_000){const chunks=[];let n=0;for await(const c of req){n+=c.length;if(n>limit)throw Error('request too large');chunks.push(c)}return Buffer.concat(chunks)}
async function body(req){return JSON.parse((await read(req,2_000_000)).toString()||'{}')}
function modelFor(text){const n=text.length;if(/analiz|strateg|złożon|research|bada|porówn|plan/i.test(text)||n>1400)return process.env.OPENAI_MODEL_COMPLEX||'gpt-5.6-sol';if(/szybko|krótko|notat|przypomnij/i.test(text))return process.env.OPENAI_MODEL_FAST||'gpt-5.6-luna';return process.env.OPENAI_MODEL||'gpt-5.6-terra'}
async function openai(text){const key=process.env.OPENAI_API_KEY;if(!key)return{reply:'Rdzeń JARVIS działa, ale OPENAI_API_KEY nie jest skonfigurowany po stronie backendu.',model:null};const model=modelFor(text);const r=await fetch('https://api.openai.com/v1/responses',{method:'POST',headers:{authorization:`Bearer ${key}`,'content-type':'application/json'},body:JSON.stringify({model,input:[{role:'system',content:[{type:'input_text',text:'JARVIS 2.0 is a precise personal AI executive assistant. Respond in Polish unless asked otherwise. Never claim an external action was completed unless a tool actually completed it. Ask for approval before consequential external actions.'}]},{role:'user',content:[{type:'input_text',text}]}]})});if(!r.ok)throw Error(`OpenAI ${r.status}`);const d=await r.json();return{reply:d.output_text||'Brak odpowiedzi.',model,response_id:d.id}}
async function transcribe(req,res){const key=process.env.OPENAI_API_KEY;if(!key)return json(res,503,{error:'transcription unavailable: OPENAI_API_KEY not configured'});const audio=await read(req,25_000_000);const form=new FormData();form.append('file',new Blob([audio],{type:req.headers['content-type']||'audio/m4a'}),'jarvis.m4a');form.append('model',process.env.OPENAI_TRANSCRIBE_MODEL||'gpt-4o-transcribe');form.append('language','pl');const r=await fetch('https://api.openai.com/v1/audio/transcriptions',{method:'POST',headers:{authorization:`Bearer ${key}`},body:form});if(!r.ok)throw Error(`Transcription ${r.status}`);return json(res,200,await r.json())}
async function sendPush(token,title,bodyText){const r=await fetch('https://exp.host/--/api/v2/push/send',{method:'POST',headers:{'content-type':'application/json'},body:JSON.stringify({to:token,title,body:bodyText,sound:'default'})});return r.ok}
async function route(req,res){const u=new URL(req.url,`http://${req.headers.host}`);if(req.method==='OPTIONS')return json(res,204,{});if(!auth(req))return json(res,401,{error:'unauthorized'});try{
if(req.method==='GET'&&u.pathname==='/api/health')return json(res,200,{ok:true,version:'3.1.1',service:'jarvis-backend',persistence:process.env.DATABASE_URL?'postgres-configured':'json-fallback'});
if(req.method==='GET'&&u.pathname==='/api/agents')return json(res,200,{agents:agents.map(([id,name])=>({id,name,status:'online'}))});
if(req.method==='GET'&&u.pathname==='/api/tasks')return json(res,200,{tasks:state.tasks});
if(req.method==='POST'&&u.pathname==='/api/tasks'){const x=await body(req);const t={id:id(),title:String(x.title||'Untitled task'),status:'planned',priority:['low','medium','high','critical'].includes(x.priority)?x.priority:'medium',created_at:new Date().toISOString()};state.tasks.push(t);await save();return json(res,201,t)}
if(req.method==='PATCH'&&u.pathname.startsWith('/api/tasks/')){const t=state.tasks.find(x=>x.id===u.pathname.split('/').pop());if(!t)return json(res,404,{error:'not found'});Object.assign(t,await body(req));await save();return json(res,200,t)}
if(req.method==='GET'&&u.pathname==='/api/memory')return json(res,200,{memory:state.memory});
if(req.method==='POST'&&u.pathname==='/api/memory'){const x=await body(req);const m={id:id(),text:String(x.text||''),created_at:new Date().toISOString()};state.memory.push(m);await save();return json(res,201,m)}
if(req.method==='GET'&&u.pathname==='/api/approvals')return json(res,200,{approvals:state.approvals});
if(req.method==='POST'&&u.pathname==='/api/approvals'){const x=await body(req);const a={id:id(),action:String(x.action||''),status:'pending',created_at:new Date().toISOString()};state.approvals.push(a);await save();return json(res,201,a)}
if(req.method==='PATCH'&&u.pathname.startsWith('/api/approvals/')){const a=state.approvals.find(x=>x.id===u.pathname.split('/').pop());if(!a)return json(res,404,{error:'not found'});Object.assign(a,await body(req));await save();return json(res,200,a)}
if(req.method==='POST'&&u.pathname==='/api/push/register'){const x=await body(req);if(x.token&&!state.push_tokens.includes(x.token))state.push_tokens.push(String(x.token));await save();return json(res,200,{ok:true})}
if(req.method==='POST'&&u.pathname==='/api/push/test'){const x=await body(req);const title=String(x.title||'JARVIS');const bodyText=String(x.body||'Test notification');let sent=0;for(const token of state.push_tokens){if(await sendPush(token,title,bodyText))sent++}return json(res,200,{sent})}
if(req.method==='POST'&&u.pathname==='/api/transcribe')return transcribe(req,res);
if(req.method==='GET'&&u.pathname==='/api/audit')return json(res,200,{audit:state.audit.slice(-500)});
if(req.method==='GET'&&u.pathname==='/api/integrations')return json(res,200,{google:{configured:Boolean(process.env.GOOGLE_CLIENT_ID&&process.env.GOOGLE_CLIENT_SECRET)},telegram:{configured:Boolean(process.env.TELEGRAM_BOT_TOKEN)},database:{configured:Boolean(process.env.DATABASE_URL)}});
if(req.method==='POST'&&u.pathname==='/api/chat'){const x=await body(req),text=String(x.text||'').trim();if(!text)return json(res,400,{error:'text required'});const trace=id(),result=await openai(text);state.audit.push({trace_id:trace,type:'chat',model:result.model,text,created_at:new Date().toISOString()});await save();return json(res,200,{trace_id:trace,...result})}
return json(res,404,{error:'not found'});
}catch(e){return json(res,500,{error:e?.message||'internal error'})}}
http.createServer(route).listen(PORT,()=>console.log(`JARVIS backend listening on :${PORT}`));
