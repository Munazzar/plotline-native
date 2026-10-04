/* ================= INSIGHTS (on-device RAG) =================
 Retrieval runs on this device. Every journal entry, goal, habit and day plan becomes a short passage.
 Keyword search (BM25) works instantly and offline. "Smart search" adds semantic search with the open-source
 all-MiniLM-L6-v2 model via Transformers.js — downloaded once from Hugging Face, cached, then offline; vectors are
 kept in IndexedDB ('ragvec'), never synced. The best passages plus numbers the app computes itself go to one of:
 an on-device open-source model through wllama (llama.cpp in WebAssembly, bundled in lib/wllama; CPU, or WebGPU when
 present), your own OpenAI-compatible server (Ollama, LM Studio…), or a prompt to copy into any AI.
 Every question also carries insOverview(): a compact "about me" of all goals, habits and moods. */
const RAG_CFG=Object.assign({lib:'https://cdn.jsdelivr.net/npm/@huggingface/transformers@4.3.0/dist/transformers.min.js',model:'Xenova/all-MiniLM-L6-v2',dtype:'q8',localModels:'',wasm:'',wllama:'./lib/wllama/index.min.js',wllamaWasm:'lib/wllama/wllama.wasm',nctx:4096},window.PLOTLINE_RAG||{});
const HFR=(r,f)=>`https://huggingface.co/${r}/resolve/main/${f}`;
/* [id, label, url, GB]. Phi-4 mini uses Q3_K_S: the Q4 file is 2.5 GB, over wllama's 2 GB-per-file limit. */
const LLMS=[['llama3.2-1b','Llama 3.2 1B · 0.8 GB · fast, good',HFR('bartowski/Llama-3.2-1B-Instruct-GGUF','Llama-3.2-1B-Instruct-Q4_K_M.gguf'),.8,HFR('bartowski/Llama-3.2-1B-Instruct-GGUF','Llama-3.2-1B-Instruct-Q4_0.gguf'),.77],
 ['qwen2.5-1.5b','Qwen 2.5 1.5B · 1.1 GB · better answers',HFR('Qwen/Qwen2.5-1.5B-Instruct-GGUF','qwen2.5-1.5b-instruct-q4_k_m.gguf'),1.1,HFR('Qwen/Qwen2.5-1.5B-Instruct-GGUF','qwen2.5-1.5b-instruct-q4_0.gguf'),1.07],
 ['phi4-mini','Phi-4 mini 3.8B · smartest, about a minute per answer',HFR('bartowski/microsoft_Phi-4-mini-instruct-GGUF','microsoft_Phi-4-mini-instruct-Q3_K_S.gguf'),1.9,HFR('bartowski/microsoft_Phi-4-mini-instruct-GGUF','microsoft_Phi-4-mini-instruct-Q4_0.gguf'),2.33],
 ['qwen2.5-0.5b','Qwen 2.5 0.5B · 0.5 GB · fastest, basic',HFR('Qwen/Qwen2.5-0.5B-Instruct-GGUF','qwen2.5-0.5b-instruct-q4_k_m.gguf'),.5,HFR('Qwen/Qwen2.5-0.5B-Instruct-GGUF','qwen2.5-0.5b-instruct-q4_0.gguf'),.43]].concat(window.PLOTLINE_RAG&&window.PLOTLINE_RAG.llms||[]);
/* rows: [id, label, wasm url (web, ≤2 GB), GB, native url (Android app, Q4_0: repacked for ARM dot-product → fastest on phones), GB] */
const API_PRE=[['Ollama','http://localhost:11434/v1','llama3.2'],['LM Studio','http://localhost:1234/v1','local-model'],['OpenRouter','https://openrouter.ai/api/v1','meta-llama/llama-3.3-70b-instruct']];
function insSet(){const d={engine:'local',smart:false,priv:false,url:API_PRE[0][1],model:API_PRE[0][2],llm:LLMS[0][0]};const o=S.settings.ins={...d,...(S.settings.ins||{})};if(!o.nat){o.nat=1;o.phi=1;if(NAT_OK()||!o.llm||o.llm==='phi4-mini')o.llm='llama3.2-1b'}return o}
const aiKey=()=>{try{return localStorage.getItem('plotline.aikey')||''}catch(e){return''}};
const MOODN=Object.fromEntries(MOODS.map(([m,n])=>[m,n]));const MOOD_POS=new Set(['Calm','Energized','Grateful']),MOOD_NEG=new Set(['Low','Anxious','Frustrated','Tired']);
let IN={q:'',pre:'',win:0,goal:'',busy:false,prog:'',res:null,ctl:null,paste:'',fq:''},VEC=null,EMB=null,EMB_P=null,LLM=null,LLM_ID='';
const fnv=s=>{let h=2166136261;for(let i=0;i<s.length;i++){h^=s.charCodeAt(i);h=Math.imul(h,16777619)}return(h>>>0).toString(36)+s.length.toString(36)};

/* ---- passages ---- */
function ragDocs(){const priv=insSet().priv,out=[],dstr=t=>ymd(new Date(t));
 const add=(o,body)=>{body=String(body||'').replace(/\s+/g,' ').trim();if(body.length<2)return;const head=o.head;
  if(body.length<=800){out.push({...o,text:head+': '+body});return}
  for(let i=0,n=0;i<body.length;i+=620,n++){let s=body.slice(i,i+800);out.push({...o,id:o.id+'#'+n,text:head+(n?' (cont.)':'')+': '+s});if(i+800>=body.length)break}};
 const gt=id=>{const g=id&&G(id);return g?` · goal “${g.title}”`:''};
 S.entries.forEach(e=>{if(e.type==='token'||(e.private&&!priv))return;const d=dstr(e.t),h=entryHabit(e),mood=MOODN[e.mood];
  const kind={note:'Journal',step:'Step done',focus:'Focus session','goal-new':'New goal','goal-done':'Goal completed',habit:'Habit milestone'}[e.type]||'Note';
  add({id:'e'+e.id,kind:'entry',ref:e.id,gid:e.goalId||'',date:d,label:e.title||trunc(e.text||kind,60),mood:mood||'',head:`${d} · ${kind}${mood?' · mood '+mood:''}${gt(e.goalId)}${h?` · habit “${h.title}”`:''}${e.title?' · '+e.title:''}`},e.text||e.title)});
 S.goals.forEach(g=>{const p=Math.round(prog(g)*100),par=g.parent&&G(g.parent),td=ymd();
  const done=g.steps.filter(s=>s.done),open=g.steps.filter(s=>!s.done);const last=done.reduce((a,s)=>Math.max(a,s.doneAt||0),0);
  const body=[g.why&&'Why: '+g.why,`${areaName(g.area)}, ${hzName(g.horizon)}, ${PRIO[g.priority]||'Medium'} priority, ${g.status==='done'?'completed'+(g.completedAt?' '+dstr(g.completedAt):''):g.status}, ${p}% done`,`started ${g.startDate}${g.targetDate?', target '+g.targetDate:''}`,par&&`part of “${par.title}”`,last&&'last step done '+dstr(last),
   done.length&&'Done steps: '+done.map(s=>s.title).join('; '),open.length&&'Open steps: '+open.map(s=>s.title+(s.due?(s.due<td?' (overdue since '+s.due+')':' (due '+s.due+')'):'')).join('; ')].filter(Boolean).join('. ');
  add({id:'g'+g.id,kind:'goal',ref:g.id,gid:g.id,date:g.startDate,label:g.title,head:`Goal “${g.title}”`},body)});
 const td=dnum(ymd());
 S.habits.forEach(h=>{if(h.status==='archived')return;const K={build:'build',routine:'routine',quit:h.mode==='limit'?'limit':'break'}[h.kind];let body=[h.why&&'Why: '+h.why,h.cue&&'Cue: '+h.cue,h.mini&&'Minimum version: '+h.mini,h.status==='paused'&&'paused'];
  if(h.kind==='quit'){const sl=h.slips.map(Number).filter(Boolean).sort((a,b)=>b-a);body.push(h.mode==='limit'?`daily limit ${h.limit}${h.unit?' '+h.unit:''}`:`clean since ${dstr(sl[0]||h.start)}`,`${sl.length} slip${sl.length===1?'':'s'} logged`+(sl.length?', last '+dstr(sl[0]):''),h.urges.length&&h.urges.length+' urges surfed')}
  else{let due=0,dn=0,miss=[];for(let k=td-29;k<=td;k++){const ds=fromN(k);if(!hCounts(h,ds))continue;due++;if(hDone(h,ds))dn++;else if(k<td)miss.push(DAYS[dowOf(ds)])}
   const mc={};miss.forEach(x=>mc[x]=(mc[x]||0)+1);const worst=Object.entries(mc).sort((a,b)=>b[1]-a[1]).filter(x=>x[1]>1).slice(0,2).map(x=>x[0]);
   body.push(`streak ${hStreak(h)}, best ${hBest(h)}, strength ${hStrength(h)}%`,`last 30 days: done ${dn} of ${due} due days`,worst.length&&'most often missed on '+worst.join(' and '),Object.keys(h.skip).length&&Object.keys(h.skip).length+' rest days')}
  add({id:'h'+h.id,kind:'habit',ref:h.id,gid:h.goalId||'',date:h.startDate,label:h.title,head:`Habit “${h.title}” (${K})${gt(h.goalId)}`},body.filter(Boolean).join('. '))});
 const days={};S.days.forEach(x=>(days[x.date]=days[x.date]||[]).push(x));
 Object.entries(days).forEach(([d,xs])=>add({id:'d'+d,kind:'day',ref:d,gid:(xs.find(x=>x.goalId)||{}).goalId||'',date:d,label:'Day plan · '+fmtDate(d),head:`${d} · Day plan`},xs.map(x=>x.title+(x.done?' (done)':' (not done)')).join('; ')));
 out.forEach(o=>{o.h=fnv(o.text)});return out}

/* ---- keyword search (BM25) ---- */
const STOPW=new Set("a an and are as at be been but by can could do did does for from had has have how i if im in into is it its just me my of on or our so than that the their them then there these they this to too up us was we were what when where which who why will with would you your should shall about any some get got make best good way ways tips give tell want need know like more most much very also all one thing things help please".split(' '));
const stemW=w=>w.length>4?w.replace(/(ings|ing|edly|ed|ies|es|ly|s)$/,''):w;
const toksOf=s=>(String(s).toLowerCase().match(/[\p{L}\p{N}]+/gu)||[]).filter(w=>w.length>1&&!STOPW.has(w)).map(stemW);
function bm25(docs,q){const N=docs.length,qt=[...new Set(toksOf(q))];if(!N||!qt.length)return docs.map(()=>0);const tf=docs.map(d=>toksOf(d.text)),avg=tf.reduce((a,t)=>a+t.length,0)/N||1,df=new Map();
 tf.forEach(t=>new Set(t).forEach(w=>df.set(w,(df.get(w)||0)+1)));
 return tf.map(t=>{const c=new Map();t.forEach(w=>c.set(w,(c.get(w)||0)+1));let s=0;qt.forEach(w=>{const f=c.get(w);if(!f)return;const n=df.get(w);s+=Math.log(1+(N-n+.5)/(n+.5))*f*2.2/(f+1.2*(.25+.75*t.length/avg))});return s})}

/* ---- semantic search (Transformers.js + MiniLM) ---- */
function embedder(onP){if(EMB)return Promise.resolve(EMB);if(EMB_P)return EMB_P;
 EMB_P=(async()=>{const T=await import(RAG_CFG.lib);if(RAG_CFG.localModels){T.env.allowLocalModels=true;T.env.localModelPath=RAG_CFG.localModels;T.env.allowRemoteModels=false}if(RAG_CFG.wasm)T.env.backends.onnx.wasm.wasmPaths=RAG_CFG.wasm;
  const files={};const p=await T.pipeline('feature-extraction',RAG_CFG.model,{dtype:RAG_CFG.dtype,progress_callback:x=>{if(x.status==='progress'&&x.total){files[x.file]=[x.loaded,x.total];const a=Object.values(files).reduce((s,v)=>[s[0]+v[0],s[1]+v[1]],[0,0]);onP&&onP(`Downloading the search model · ${Math.round(a[0]/1048576)} of ${Math.round(a[1]/1048576)} MB`)}}});
  EMB=async arr=>{const o=await p(arr,{pooling:'mean',normalize:true}),n=o.dims[o.dims.length-1],d=o.data;return arr.map((_,i)=>d.slice(i*n,(i+1)*n))};return EMB})();
 EMB_P.catch(()=>{EMB_P=null});return EMB_P}
async function loadVecs(){if(VEC)return VEC;const r=await DB.get('ragvec').catch(()=>null);VEC=new Map(r&&r.m===RAG_CFG.model&&r.v?Object.entries(r.v):[]);return VEC}
async function ensureVecs(docs,onP){await loadVecs();const need=docs.filter(d=>!VEC.has(d.h));if(!need.length)return;const f=await embedder(onP);
 for(let i=0;i<need.length;i+=16){const b=need.slice(i,i+16),vs=await f(b.map(d=>d.text));b.forEach((d,j)=>VEC.set(d.h,vs[j]));onP&&onP(`Reading your notes · ${Math.min(i+16,need.length)} of ${need.length}`);await new Promise(r=>setTimeout(r,0))}
 const live=new Set(docs.map(d=>d.h));for(const k of [...VEC.keys()])if(!live.has(k))VEC.delete(k);await DB.set('ragvec',{m:RAG_CFG.model,v:Object.fromEntries(VEC)}).catch(()=>{})}

/* ---- retrieve: filter → BM25 (+ vectors) → reciprocal rank fusion → fit to budget ---- */
let VSTRONG=null;const vecStrong=i=>VSTRONG&&VSTRONG[i]>=.45;
async function ragRetrieve(q,o,onP){let docs=ragDocs();VSTRONG=null;const since=o.win?fromN(dnum(ymd())-o.win+1):'';
 if(o.goal){const g=G(o.goal);const ids=new Set(g?[g.id,...descendants(g).map(x=>x.id)]:[]);docs=docs.filter(d=>ids.has(d.gid))}
 if(o.kinds)docs=docs.filter(d=>o.kinds.includes(d.kind));
 if(since)docs=docs.filter(d=>d.kind==='goal'||d.kind==='habit'||d.date>=since);
 let mode='keyword',err='',order;
 if(o.all){order=docs.map((d,i)=>i).sort((a,b)=>(docs[b].kind==='entry')-(docs[a].kind==='entry')||(docs[a].date<docs[b].date?1:-1))}
 else{const lists=[];const rank=sc=>sc.map((s,i)=>[i,s]).filter(x=>x[1]>0).sort((a,b)=>b[1]-a[1]).map(x=>x[0]);lists.push(rank(bm25(docs,q)));
  if(insSet().smart&&docs.length){try{await ensureVecs(docs,onP);const[qv]=await(await embedder(onP))([q]);const vsc=docs.map(d=>{const v=VEC.get(d.h);let s=0;for(let j=0;j<v.length;j++)s+=v[j]*qv[j];return s>.32?s:0});VSTRONG=vsc;lists.push(rank(vsc));mode='smart'}catch(e){console.warn('smart search',e);err='Smart search couldn’t load, so keyword search was used. Check your connection and try again.'}}
  const sc=new Map();lists.forEach(l=>l.forEach((i,r)=>sc.set(i,(sc.get(i)||0)+1/(60+r))));order=[...sc.entries()].sort((a,b)=>b[1]-a[1]).map(x=>x[0]);
  /* relevance gate: a note must actually match the question. Keep keyword hits within reach of the best one,
     and only pad with recent entries for broad “how am I doing” questions. */
  const bs=bm25(docs,q),top=Math.max(0,...bs),vs=lists[1]?new Set(lists[1]):null;
  order=order.filter(i=>(top>0&&bs[i]>=top*.35)||vecStrong(i)||(vs&&vs.has(i)&&bs[i]>0));
  if(o.broad){const seen=new Set(order);docs.map((d,i)=>i).filter(i=>!seen.has(i)&&docs[i].kind==='entry').sort((a,b)=>docs[a].date<docs[b].date?1:-1).slice(0,Math.max(0,6-order.length)).forEach(i=>order.push(i))}}
 const budget=insSet().engine==='local'?(IS_PHONE?3200:5200):12000,srcs=[];let used=0;for(const i of order){const d=docs[i];if(used+d.text.length>budget||srcs.length>=(insSet().engine==='local'?(IS_PHONE?6:10):16))break;used+=d.text.length;srcs.push(d)}
 return{srcs,mode,err,total:docs.length}}

/* ---- numbers the app computes itself (no AI) ---- */
function insStats(win,goal){const tn=dnum(ymd()),from=win?tn-win+1:-1e9,inW=t=>dnum(ymd(new Date(t)))>=from,out=[],priv=insSet().priv;
 const gs=goal&&G(goal)?[G(goal),...descendants(G(goal))]:S.goals,gid=new Set(gs.map(g=>g.id));
 const ents=S.entries.filter(e=>e.type!=='token'&&inW(e.t)&&(!goal||gid.has(e.goalId))),notes=ents.filter(e=>e.type==='note'&&(!e.private||priv));
 const moods={};notes.forEach(e=>{const m=MOODN[e.mood];if(m)moods[m]=(moods[m]||0)+1});const mt=Object.entries(moods).sort((a,b)=>b[1]-a[1]);
 out.push({k:'Journal',v:notes.length,t:`${notes.length} journal entr${notes.length===1?'y':'ies'}${mt.length?', moods: '+mt.map(([m,n])=>`${m} ${n}`).join(', '):''}`});
 const steps=ents.filter(e=>e.type==='step').length;out.push({k:'Steps done',v:steps,t:`${steps} steps completed`});
 const gd=gs.filter(g=>g.status==='done'&&g.completedAt&&inW(g.completedAt)).length;if(gd)out.push({k:'Goals done',v:gd,t:`${gd} goals completed`});
 const td=ymd(),od=gs.reduce((a,g)=>a+(g.status==='active'?g.steps.filter(s=>!s.done&&s.due&&s.due<td).length:0),0);out.push({k:'Overdue',v:od,t:`${od} open steps are past their due date`});
 const stale=gs.filter(g=>g.status==='active'&&g.steps.some(s=>!s.done)&&Math.max(g.createdAt||0,...g.steps.map(s=>s.doneAt||0))<Date.now()-21*864e5);
 if(stale.length)out.push({k:'Stalled',v:stale.length,t:`No step done in 3+ weeks on: ${stale.map(g=>'“'+g.title+'”').join(', ')}`});
 const dys=S.days.filter(x=>dnum(x.date)>=from&&x.date<=td&&(!goal||gid.has(x.goalId)));if(dys.length){const dn=dys.filter(x=>x.done).length;out.push({k:'Day goals',v:Math.round(dn/dys.length*100)+'%',t:`${dn} of ${dys.length} day goals done`})}
 const hs=S.habits.filter(h=>h.status!=='archived'&&h.kind!=='quit'&&(!goal||gid.has(h.goalId)));
 if(hs.length){const w=Math.min(win||90,90);let due=0,dn=0;hs.forEach(h=>{for(let k=tn-w+1;k<=tn;k++){const ds=fromN(k);if(hCounts(h,ds)){due++;if(hDone(h,ds))dn++}}});if(due)out.push({k:'Habits kept',v:Math.round(dn/due*100)+'%',t:`habits kept on ${dn} of ${due} due days (last ${w} days)`})}
 /* mood × habit: share of positive moods on days a habit was done vs not */
 const md={};S.entries.forEach(e=>{if(e.type!=='note'||(e.private&&!priv))return;const m=MOODN[e.mood];if(!m||(!MOOD_POS.has(m)&&!MOOD_NEG.has(m)))return;const d=ymd(new Date(e.t));if(dnum(d)<tn-179)return;(md[d]=md[d]||[]).push(MOOD_POS.has(m)?1:0)});
 const mdays=Object.keys(md),links=[];if(mdays.length>=6)hs.forEach(h=>{const a=[],b=[];mdays.forEach(d=>{if(!hDue(h,d))return;const v=md[d].reduce((x,y)=>x+y,0)/md[d].length;(hDone(h,d)?a:b).push(v)});if(a.length<3||b.length<3)return;const pa=Math.round(a.reduce((x,y)=>x+y,0)/a.length*100),pb=Math.round(b.reduce((x,y)=>x+y,0)/b.length*100);
  if(Math.abs(pa-pb)>=20)links.push({k:'Mood link',d:Math.abs(pa-pb),v:(pa>pb?'+':'')+(pa-pb)+'%',t:`On days you did “${h.title}”, ${pa}% of your moods were positive, vs ${pb}% on days you didn’t (${a.length} and ${b.length} days)`,hl:1})});
 links.sort((a,b)=>b.d-a.d).slice(0,2).forEach(x=>out.push(x));
 return out}

/* ---- "about me": a compact overview of the whole plan, sent with every question so the model knows my context ---- */
/* what a question is about: broad check-ins get the whole picture; specific questions only get the goals and habits they name */
const BROAD_RX=/\b(how am i|how have i|how('?s| is) my|am i (on track|doing)|summar|overview|review|recap|check[- ]?in|lately|recently|this (week|month|year)|last (week|month)|patterns?|trends?|progress|overall|everything|my (plan|goals|habits|week|month)|what should i (do|focus|work)|where (do|should) i (start|focus)|priorit|next step)/i;
function insFocus(q){const qs=String(q||''),broad=!qs||BROAD_RX.test(qs),qt=new Set(toksOf(qs).filter(w=>w.length>2));
 const hit=t=>{const w=toksOf(t);return w.some(x=>qt.has(x))};return{broad,hit,mood:/\b(mood|feel|felt|happy|sad|stress|anxi|energy|tired|low)/i.test(qs),today:/\b(today|tonight|now)\b/i.test(qs),hab:/\b(habit|streak|routine)/i.test(qs)}}
function insOverview(max,q){const td=ymd(),tn=dnum(td),dstr=t=>ymd(new Date(t)),L=[],priv=insSet().priv,nm=S.settings.name,F=insFocus(q);
 L.push(`${nm?'Name: '+nm+'. ':''}Today: ${DAYS[new Date().getDay()]} ${td}.`);
 const act=treeOrder(S.goals.filter(g=>g.status!=='archived'));const all=act.filter(t=>t.g.status==='active'),dn=S.goals.filter(g=>g.status==='done');
 const gtx=g=>g.title+' '+(g.why||'')+' '+areaName(g.area)+' '+g.steps.map(x=>x.title).join(' ');
 const on=F.broad?all:all.filter(t=>F.hit(gtx(t.g)));const skip=all.length-on.length;
 if(on.length){L.push(F.broad?`Active goals (${on.length}):`:`Goals related to the question:`);on.forEach(({g,d})=>{const nx=g.steps.find(s=>!s.done),od=g.steps.filter(s=>!s.done&&s.due&&s.due<td).length,last=Math.max(g.createdAt||0,...g.steps.map(s=>s.doneAt||0)),idle=Math.floor((Date.now()-last)/864e5);
  L.push(`${'  '.repeat(Math.min(d,3))}- “${g.title}” · ${areaName(g.area)} · ${Math.round(prog(g)*100)}%${g.targetDate?' · target '+g.targetDate:''}${g.priority===1?' · high priority':''}${nx?` · next: ${nx.title}${nx.due?' ('+nx.due+')':''}`:''}${od?` · ${od} overdue`:''}${idle>=14?` · no progress for ${idle} days`:''}${g.why&&d===0?` · why: ${trunc(g.why,90)}`:''}`)})}
 else L.push(all.length?'None of my goals are about this.':'No active goals yet.');
 if(skip&&on.length)L.push(`(${skip} other goals not related to the question are left out.)`);
 const rd=dn.filter(g=>g.completedAt&&g.completedAt>Date.now()-90*864e5&&(F.broad||F.hit(g.title)));if(rd.length)L.push(`Completed in the last 90 days: ${rd.map(g=>'“'+g.title+'”').join(', ')}.`);
 const hs=S.habits.filter(h=>h.status!=='archived'&&(F.broad||F.hab||F.hit(h.title+' '+(h.cue||''))));if(hs.length){L.push(`Habits (${hs.length}):`);hs.forEach(h=>{let t;
  if(h.kind==='quit'){const sl=h.slips.map(Number).filter(Boolean).sort((a,b)=>b-a);t=h.mode==='limit'?`limit ${h.limit}${h.unit?' '+h.unit:''} a day`:`breaking it · clean ${Math.floor((Date.now()-(sl[0]||h.start))/864e5)} days · ${sl.length} slips`}
  else{let due=0,d2=0;for(let k=tn-29;k<=tn;k++){const ds=fromN(k);if(hCounts(h,ds)){due++;if(hDone(h,ds))d2++}}t=`${h.kind==='routine'?'routine':'building'} · streak ${hStreak(h)} · kept ${d2} of ${due} due days (30d)${h.cue?' · cue: '+trunc(h.cue,50):''}`}
  L.push(`- “${h.title}” · ${t}${h.status==='paused'?' · paused':''}`)})}
 const mw=(a,b)=>{const c={};S.entries.forEach(e=>{if(e.type!=='note'||(e.private&&!priv))return;const k=dnum(dstr(e.t)),m=MOODN[e.mood];if(m&&k>tn-a&&k<=tn-b)c[m]=(c[m]||0)+1});return c};
 const m1=mw(30,0),m0=mw(60,30),pos=c=>{const v=Object.entries(c),n=v.reduce((a,x)=>a+x[1],0);return n?Math.round(v.filter(x=>MOOD_POS.has(x[0])).reduce((a,x)=>a+x[1],0)/n*100):null};
 if((F.broad||F.mood)&&Object.keys(m1).length){const p1=pos(m1),p0=pos(m0);L.push(`Moods, last 30 days: ${Object.entries(m1).sort((a,b)=>b[1]-a[1]).map(([m,n])=>m+' '+n).join(', ')} (${p1}% positive${p0!=null?`, vs ${p0}% the 30 days before`:''}).`)}
 const tg=S.days.filter(x=>x.date===td);if(tg.length&&(F.broad||F.today))L.push(`Today’s plan: ${tg.map(x=>x.title+(x.done?' ✓':'')).join('; ')}.`);
 let s=L.join('\n');if(s.length>max)s=s.slice(0,max-1)+'…';return s}

function insPrompt(q,r,stats,W=IN.win){const loc=insSet().engine==='local',nm=S.settings.name,F=insFocus(q),num=F.broad||F.mood||F.hab;return`You are a thoughtful, practical coach inside Plotline, ${nm?nm+'’s':'my'} personal goals, habits and journal app. ABOUT ME, NUMBERS and NOTES are my own data. Stay on my goals, habits, routines, moods and journal, and general coaching on planning, motivation and wellbeing habits. If I ask about anything else (trivia, news, coding, creative writing, homework and so on), reply with exactly this sentence and nothing more: "${OFF_LINE}"
RULES
1. Answer exactly the QUESTION. Every sentence must be about it.
2. Use a goal, habit or note only if it is clearly about the question. Ignore everything else, even if it looks important. Never bring up unrelated topics from my data.
3. If nothing in my data is about the question, say that in one short line, then give brief, practical general coaching on it.
4. Cite the notes you use like [3]. Don’t invent facts about me.
5. Talk to me directly, warm and specific.

ABOUT ME
${insOverview(loc?(IS_PHONE?1200:1800):4000,q)}
${num?`
NUMBERS (computed by the app${W?`, last ${W} days`:''})
${stats.map(s=>'- '+s.t).join('\n')||'- none'}
`:''}
NOTES (only notes that match the question)
${r.srcs.map((d,i)=>`[${i+1}] ${d.text}`).join('\n')||'(none of my notes are about this)'}

QUESTION
${q}

ANSWER FORMAT
${F.broad?'Two to five short paragraphs or bullets, citing notes like [2]. End with one small next step for this week, starting with "Next step:".':'A short, direct answer to the question (one to three short paragraphs or a few bullets). End with "Next step:" only if it helps.'}`}
function insFollowMsg(q,fresh,base){return`FOLLOW-UP QUESTION
${q}
${fresh.length?`\nMORE NOTES\n${fresh.map((d,i)=>`[${base+i+1}] ${d.text}`).join('\n')}\n`:''}
Answer only this follow-up, following the same RULES: use only data that is about it, and stay on my plan (otherwise reply with the fixed sentence). Cite notes like [${base+1}]. End with "Next step:" only if it helps.`}

/* ---- scope: Ask only talks about the plan (goals, habits, routines, moods, journal) and general coaching on those ----
 1) a gate in code before any model runs, 2) a scope rule in the prompt, 3) a check on the answer. */
const OFF_LINE='I can only help with your goals, habits, routines, moods and journal, plus general coaching on those. Try asking about your plan.';
const COACH_RX=/\b(goal|habit|routine|plan|planning|step|streak|progress|track|achiev|accomplish|motivat|discipline|consisten|procrastinat|productiv|focus|priorit|schedul|time manag|balance|stuck|struggl|overwhelm|burn ?out|quit|miss|skip|slip|relapse|urge|craving|mood|feel|feeling|stress|anxi|calm|happy|sad|low|tired|energy|sleep|rest|grateful|gratitude|reflect|journal|diary|note|moment|wins?|celebrat|week|weekly|month|today|tonight|tomorrow|morning|evening|daily|year|health|fitness|exercis|workout|run|walk|gym|diet|eat|water|weight|meditat|pray|faith|family|kids|wife|husband|friend|relationship|career|job|work|study|learn|read|book|save|saving|budget|money|debt|spend|finance|reminder|calendar|deadline|due|overdue|lazy|willpower|self[- ]?care|wellbeing|well-being|mindset|confidence|lonely|purpose)/i;
const OFF_RX=/\b(write|compose|generate|make)\b.{0,20}\b(poem|story|song|lyrics|essay|joke|code|script|program|function|email|letter|tweet|rap|haiku)\b|\btranslate\b|\bcapital of\b|\bwho (is|was|won)\b|\bweather\b|\bstock(s| price)?\b|\bbitcoin|crypto\b|\brecipe\b|\b(javascript|python|java|sql|html|css|regex|c\+\+)\b|\bsolve\b.{0,20}\b(equation|integral|for x)\b|\bnews\b|\bmovie|netflix|celebrit|\bpresident\b|\belection\b|\bhistory of\b|\bdefine\b|\bmeaning of the word\b|\bspell\b/i;
function dataHits(q){const qt=[...new Set(toksOf(q))].filter(w=>w.length>2);if(!qt.length)return 0;const vocab=new Set();[...S.goals.map(g=>g.title+' '+(g.why||'')+' '+g.steps.map(x=>x.title).join(' ')),...S.habits.map(h=>h.title+' '+(h.cue||'')),...S.entries.slice(-400).map(e=>(e.title||'')+' '+(e.text||''))].forEach(t=>toksOf(t).forEach(w=>vocab.add(w)));return qt.filter(w=>vocab.has(w)).length}
function askGate(q,follow){const t=q.toLowerCase();
 if(OFF_RX.test(t)&&!/\b(my|our)\b.{0,30}\b(goal|habit|plan|journal|week|progress|routine)/.test(t))return false;
 if(follow)return true;                       // “why?”, “and after that?” — follow-ups keep the conversation’s topic
 if(dataHits(q)>=2)return true;               // talks about things that are in the plan or journal
 if(COACH_RX.test(t))return true;             // general coaching on goals, habits, moods, planning
 return false}
function answerOff(a){if(/\[\d+\]/.test(a)||a.includes(OFF_LINE.slice(0,40)))return false;return!COACH_RX.test(a.toLowerCase())&&dataHits(a)<2}
function askBlocked(q){const R=IN.res,t={q,a:OFF_LINE,done:true,err:'',off:1};IN.q='';
 if(R&&!R.pending&&!R.offOnly)R.turns.push(t);else IN.res={srcs:[],turns:[t],engine:insSet().engine,msgs:[],offOnly:1,opt:{win:IN.win,goal:IN.goal},mode:'keyword',total:0};
 render(false);askScroll(true)}
/* ---- answer engines ---- */
async function apiChat(msgs,onTok,signal,opts={}){const s=insSet(),key=aiKey();if(!s.url)throw new Error('Add your AI server address first');
 const r=await fetch(s.url.replace(/\/+$/,'')+'/chat/completions',{method:'POST',signal,headers:{'Content-Type':'application/json',...(key?{Authorization:'Bearer '+key}:{})},body:JSON.stringify({model:s.model,messages:msgs,stream:true,temperature:opts.temperature??.4,...(opts.max_tokens?{max_tokens:opts.max_tokens}:{}),...(opts.schema?{response_format:{type:'json_object'}}:{})})});
 if(!r.ok)throw new Error(`The server said ${r.status}. ${(await r.text().catch(()=>'')).slice(0,160)}`);
 if(!r.body||!(r.headers.get('content-type')||'').includes('event-stream')){const j=await r.json();onTok(j.choices?.[0]?.message?.content||'');return}
 const rd=r.body.getReader(),dec=new TextDecoder();let buf='';for(;;){const{value,done}=await rd.read();if(done)break;buf+=dec.decode(value,{stream:true});let i;
  while((i=buf.indexOf('\n'))>=0){const ln=buf.slice(0,i).trim();buf=buf.slice(i+1);if(!ln.startsWith('data:'))continue;const d=ln.slice(5).trim();if(d==='[DONE]')return;try{const t=JSON.parse(d).choices?.[0]?.delta?.content;if(t)onTok(t)}catch(e){}}}}
/* on-device: wllama (llama.cpp in WebAssembly, bundled with the app) runs an open-source GGUF model on the CPU, or on the GPU through WebGPU when there is one */
const llmOf=id=>LLMS.find(m=>m[0]===id)||LLMS[0];
/* phones: CPU only (mobile WebGPU drivers can freeze the phone or return garbage), leave cores free for the UI, smaller context */
const IS_PHONE=!!NATIVE||/Android|iPhone|iPad|Mobile/i.test(navigator.userAgent);
const useGPU=()=>{const g=insSet().gpu;return g==null?!IS_PHONE:!!g};
async function localLLM(onP,cpuOnly){const m=llmOf(insSet().llm),gpu=useGPU()&&!cpuOnly,key=m[0]+(gpu?':gpu':':cpu');if(LLM&&LLM_ID===key)return LLM;
 const{Wllama}=await import(RAG_CFG.wllama);if(LLM)try{await LLM.exit()}catch(e){}LLM=null;LLM_ID='';
 const w=new Wllama({default:new URL(RAG_CFG.wllamaWasm,location.href).href},{suppressNativeLog:true,allowOffline:true});const hc=navigator.hardwareConcurrency||4;
 await w.loadModelFromUrl(m[2],{n_ctx:RAG_CFG.nctx||(IS_PHONE?(m[3]>1.5?2560:3072):4096),n_gpu_layers:gpu?999:0,...(IS_PHONE?{n_threads:Math.max(1,Math.min(4,hc-2))}:{}),progressCallback:({loaded,total})=>onP&&total&&onP(`Downloading ${m[1].split(' · ')[0]} · ${Math.round(loaded/1048576)} of ${Math.round(total/1048576)} MB · once, then offline`)});
 LLM=w;LLM_ID=key;return w}
/* if the prompt is too long for the model, drop the least relevant notes (they come last) and try again */
const insShrink=msgs=>msgs.map(m=>{if(m.role!=='user'||!/\n\[\d+\] /.test(m.content))return m;const L=m.content.split('\n'),ix=L.map((l,i)=>/^\[\d+\] /.test(l)?i:-1).filter(i=>i>=0);const cut=new Set(ix.slice(Math.ceil(ix.length*.5)));return{...m,content:L.filter((l,i)=>!cut.has(i)).map(l=>l.length>700&&/^\[\d+\] /.test(l)?l.slice(0,700)+'…':l).join('\n')}});
/* garbled output (runs of commas, a token repeating): stop early instead of filling the screen */
const garbled=s=>{if(RAG_CFG.noGuard)return false;if(/([^\p{L}\p{N}\s])\1{9,}/u.test(s)||/(\S{1,6})\1{9,}/u.test(s))return true;if(s.length<80)return false;const l=(s.match(/[\p{L}\p{N}]/gu)||[]).length;return l/s.length<.35};
async function localChat(msgs,onTok,onP,tries=0,reset,cpuOnly,opts={}){const w=await localLLM(onP,cpuOnly);const t0=Date.now(),tick=setInterval(()=>onP&&!got&&onP(`${opts.reading||'Reading your notes on this device…'} ${Math.round((Date.now()-t0)/1000)}s`),1000);let got=false,acc='',bad=false;const ctl=new AbortController();const outer=IN.ctl&&IN.ctl.signal;if(outer)outer.addEventListener('abort',()=>ctl.abort(),{once:true});
 try{await w.createChatCompletion({messages:msgs,stream:true,max_tokens:opts.max_tokens||(IS_PHONE?450:600),temperature:opts.temperature??.3,top_k:40,top_p:.9,repeat_penalty:opts.schema?1:1.15,...(opts.schema?{response_format:{type:'json_schema',json_schema:{name:'plan',schema:opts.schema}}}:{}),abortSignal:ctl.signal,onData:c=>{const t=c.choices?.[0]?.delta?.content;if(!t||bad)return;acc+=t;if(!opts.schema&&garbled(acc)){bad=true;ctl.abort();return}got=true;onTok(t)}})}
 catch(e){if(!(e&&e.name==='AbortError'||bad||(outer&&outer.aborted))){clearInterval(tick);if(!got&&tries<2&&/context/i.test(e&&e.message||''))return localChat(opts.schema?msgs:insShrink(msgs),onTok,onP,tries+1,reset,cpuOnly,opts);try{await w.exit()}catch(_){}LLM=null;LLM_ID='';if(!got)throw e}}
 clearInterval(tick);
 if(bad&&!(outer&&outer.aborted)){try{await w.exit()}catch(_){}LLM=null;LLM_ID='';reset&&reset();
  if(!cpuOnly&&useGPU()){onP&&onP('The GPU gave garbled text. Retrying on the CPU…');return localChat(msgs,onTok,onP,tries,reset,true,opts)}
  throw new Error('The on-device model got confused. Try again, or pick a bigger model under “How answers are made”.')}}
/* ---- Android app: native llama.cpp (llama-server built for arm64, run by LocalLlm.java), 3–5× faster than wasm ---- */
let NAT_ST=null;const natSt=()=>{try{return NAT_ST=JSON.parse(NATIVE.llmStatus())}catch(e){return NAT_ST={ok:false}}};
const NAT_OK=()=>!!(NATIVE&&NATIVE.llmStatus)&&(NAT_ST||natSt()).ok;
const LLMCB={};window.__llm=(id,k,t)=>{const f=LLMCB[id];if(f)f(k,t)};
const natFile=m=>(m[4]||m[2]).split('/').pop();
const natHave=m=>!!(natSt().have||{})[natFile(m)];
const natCtx=m=>(m[5]||m[3])>1.5?4096:8192,natThreads=()=>Math.max(2,Math.min(4,(NAT_ST&&NAT_ST.cores)||4));
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
let NAT_WARM='';
function natWarm(){if(!NAT_OK()||insSet().engine!=='local')return;const m=llmOf(insSet().llm);if(NAT_WARM===m[0]||!natHave(m)||natSt().running===natFile(m))return;NAT_WARM=m[0];const id='w'+uid();LLMCB[id]=()=>{delete LLMCB[id]};NATIVE.llmStart(id,natFile(m),natCtx(m),natThreads())}
async function natEnsure(m,onP){const f=natFile(m),gb=m[5]||m[3];
 if(!natHave(m)){if(!(natSt().dl||{}).busy)NATIVE.llmDownload(m[4]||m[2],f);
  for(;;){await sleep(700);if(IN.ctl&&IN.ctl.signal.aborted)throw Object.assign(new Error('stopped'),{name:'AbortError'});const st=natSt();if(st.have&&st.have[f])break;const d=st.dl||{};
   if(!d.busy&&d.err)throw new Error('The model couldn’t download ('+d.err+'). Check your connection and try again.');
   onP&&onP(`Downloading ${m[1].split(' · ')[0]} · ${Math.round((d.loaded||0)/1048576)} of ${d.total?Math.round(d.total/1048576):Math.round(gb*1024)} MB · once, then offline`)}}
 if(natSt().running===f)return;onP&&onP('Loading the model on this phone…');
 await new Promise((res,rej)=>{const id='s'+uid();LLMCB[id]=(k,t)=>{if(k!=='ready'&&k!=='err')return;delete LLMCB[id];k==='ready'?res():rej(new Error(t))};NATIVE.llmStart(id,f,natCtx(m),natThreads())})}
async function natChat(msgs,onTok,onP,reset,opts={}){const m=llmOf(insSet().llm);await natEnsure(m,onP);const t0=Date.now();let got=false,acc='';
 const tick=setInterval(()=>{if(!got&&onP)onP(`${opts.reading||'Reading your notes on this phone…'} ${Math.round((Date.now()-t0)/1000)}s`)},1000);
 try{await new Promise((res,rej)=>{const id='c'+uid();const stop=()=>NATIVE.llmCancel(id);if(IN.ctl)IN.ctl.signal.addEventListener('abort',stop,{once:true});
  LLMCB[id]=(k,t)=>{if(k==='t'){acc+=t;if(!opts.schema&&garbled(acc)){stop();delete LLMCB[id];reset&&reset();rej(new Error('The model got confused. Try again, or pick a different model under “How answers are made”.'));return}if(!got){got=true;clearInterval(tick);onP&&onP('')}onTok(t);return}
   delete LLMCB[id];k==='done'?res():rej(new Error(/memory|stopped|killed/i.test(t)?t:'On-device answer failed: '+t))};
  NATIVE.llmChat(id,JSON.stringify({messages:msgs,max_tokens:opts.max_tokens||(IS_PHONE?450:600),temperature:opts.temperature??.3,top_k:40,top_p:.9,repeat_penalty:opts.schema?1:1.1,...(opts.schema?{response_format:{type:'json_schema',json_schema:{name:'plan',schema:opts.schema}}}:{})}))})}
 finally{clearInterval(tick)}}
/* follow-ups on a small on-device model: keep the first prompt and the last exchange so it fits the context window */
const insMsgs=R=>{const m=R.msgs;if(R.engine!=='local'||m.length<=4)return m;return[m[0],m[1],...m.slice(-3)]};

/* ---- answer text → safe HTML with clickable citations ---- */
function mdLite(s,n){s=String(s||'').replace(/<think>[\s\S]*?(<\/think>|$)/g,'').trim();let h=esc(s).replace(/\*\*(.+?)\*\*/g,'<b>$1</b>').replace(/(^|[\s(])\*(?!\s)([^*\n]+?)\*(?=[\s).,;:!?]|$)/g,'$1<i>$2</i>');
 h=h.replace(/\[(\d{1,2}(?:\s*[,–-]\s*\d{1,2})*)\]/g,(m,g)=>{const ids=[];g.split(/\s*,\s*/).forEach(p=>{const[a,b]=p.split(/\s*[–-]\s*/).map(Number);for(let k=a;k<=(b||a)&&k<a+8;k++)ids.push(k)});const bs=ids.filter(k=>k>=1&&k<=n).map(k=>`<button class="cite" data-act="insSrc" data-i="${k-1}" aria-label="Source ${k}">${k}</button>`).join('');return bs||m});
 let out='',ul=false;h.split('\n').forEach(l=>{const m=/^\s*(?:[-*•]|\d+[.)])\s+(.*)/.exec(l);if(m){if(!ul){out+='<ul>';ul=true}out+=`<li>${m[1]}</li>`;return}if(ul){out+='</ul>';ul=false}if(/^\s*#{1,4}\s+/.test(l))out+=`<h4>${l.replace(/^\s*#+\s+/,'')}</h4>`;else if(l.trim())out+=`<p>${l.replace(/^(Next step:)/,'<b>$1</b>')}</p>`});if(ul)out+='</ul>';return out}

/* ---- Ask: a private assistant over your own plan ---- */
const INS_PRE=[{k:'week',n:'Weekly reflection',d:'What went well, what slipped',q:'Reflect on my last 7 days: what went well, what slipped, and what pattern do you notice? Suggest one adjustment for next week.',win:7,all:true},
 {k:'mood',n:'What lifts my mood',d:'Patterns in moods and habits',q:'Looking at my moods, journal notes and habits, what seems to lift my mood and what drags it down?',kw:'feel happy calm energized grateful low anxious tired frustrated',win:90,kinds:['entry','habit']},
 {k:'stuck',n:'Where I’m stuck',d:'Stalled goals and a restart step',q:'Which goals have stalled, and what in my notes suggests why? Suggest one small restart step for each.',kw:'stuck overdue delay hard skip',win:0,kinds:['goal','entry']},
 {k:'habits',n:'Habit patterns',d:'When I keep them, when I slip',q:'What patterns do you see in my habits: when do I keep them and when do I slip? What would make them easier?',kw:'habit streak missed skip slip routine',win:60,kinds:['habit','entry','day']},
 {k:'wins',n:'Wins to celebrate',d:'What I’ve achieved lately',q:'What have I accomplished recently that I should be proud of? Be specific.',win:30,all:true}];
const INS_WIN=[[7,'7 days'],[30,'30 days'],[90,'90 days'],[0,'All time']];
const ENG_N={local:'On this device',api:'Your AI server',copy:'Copy to any AI'};
const insPage=()=>cur.p==='ask';
function askEngineLabel(){const s=insSet();if(s.engine==='local')return llmOf(s.llm)[1].split(' · ')[0]+(NAT_OK()?' · on this phone':' · on this device');return s.engine==='api'?'Your AI server'+(s.model?' · '+s.model:''):'Prompt to copy into any AI'}
/* questions written from the plan itself */
function askIdeas(){const out=[],td=ymd();
 const stale=S.goals.filter(g=>g.status==='active'&&g.steps.some(x=>!x.done)&&Math.max(g.createdAt||0,...g.steps.map(x=>x.doneAt||0))<Date.now()-21*864e5)[0];if(stale)out.push(`Why have I stalled on “${trunc(stale.title,40)}”?`);
 const hs=S.habits.filter(h=>h.status==='active'&&h.kind!=='quit').map(h=>{let due=0,dn=0;for(let k=dnum(td)-13;k<=dnum(td);k++){const ds=fromN(k);if(hCounts(h,ds)){due++;if(hDone(h,ds))dn++}}return{h,r:due?dn/due:1}}).sort((a,b)=>a.r-b.r)[0];if(hs&&hs.r<.6)out.push(`How can I keep “${trunc(hs.h.title,36)}” going?`);
 const g=S.goals.filter(x=>x.status==='active'&&x.priority===1)[0];if(g)out.push(`What should I do next for “${trunc(g.title,40)}”?`);
 return out.slice(0,3)}
function vAsk(){setTimeout(natWarm,300);const s=insSet(),R=IN.res,nm=S.settings.name,h=new Date().getHours(),gr=h<5?'Still up':h<12?'Good morning':h<17?'Good afternoon':'Good evening';
 const head=`<header class="ph ask-h"><div><h1>Ask</h1><button class="data ask-eng" data-act="askSettings">${esc(askEngineLabel())} ${ic('next','ico-s')}</button></div><div class="ph-r">${R?`<button class="ibtn" data-act="askNew" aria-label="New chat">${ic('plus')}</button>`:''}<button class="ibtn" data-act="askSettings" aria-label="Assistant settings">${ic('tune')}</button>${gear()}</div></header>`;
 let body;
 if(!R){const stats=insStats(IN.win,IN.goal),ideas=askIdeas();
  body=`<div class="ask-hero rv"><div class="ask-orb">${ic('ai')}</div><h2>${gr}${nm?', '+esc(nm):''}.</h2><p class="muted">Ask anything about your goals, habits and journal. Answers come from your own notes${s.engine==='local'?', on this device':''}.</p></div>
  <div class="ask-sugs rv">${INS_PRE.map(p=>`<button class="ask-sug" data-act="askPre" data-k="${p.k}"><b>${p.n}</b><small>${p.d}</small></button>`).join('')}${ideas.map(q=>`<button class="ask-sug idea" data-act="askIdea" data-q="${esc(q)}"><b>${esc(q)}</b><small>From your plan</small></button>`).join('')}
   <button class="ask-sug plan" data-act="askPlan"><b>${ic('flag')}Plan something new</b><small>Turn hopes into goals and steps with AI</small></button></div>
  <div class="ask-sig rv"><div class="small muted">Signals${IN.win?` · last ${IN.win} days`:''}</div><div class="ins-stats">${stats.map(x=>`<div class="ins-st ${x.hl?'hl':''}" title="${esc(x.t)}"><b>${esc(String(x.v))}</b><span>${esc(x.k)}</span>${x.hl?`<small>${esc(x.t)}</small>`:''}</div>`).join('')}</div></div>`}
 else body=`<div class="ask-thread">${R.turns.map((t,i)=>askTurn(R,t,i)).join('')}</div>`;
 const scope=`${(INS_WIN.find(w=>w[0]===IN.win)||INS_WIN[3])[1]}${IN.goal&&G(IN.goal)?' · '+esc(trunc(G(IN.goal).title,22)):''}`;
 return`<section class="ask ${R?'has':''}">${head}${body}
 <div class="ask-comp"><button class="ask-scope" data-act="askScope" aria-label="Search scope">${ic('search','ico-s')}<span>${scope}</span></button>
  <div class="ask-row"><textarea id="askIn" rows="1" placeholder="${R?'Ask a follow-up…':'Ask about your goals, habits, journal…'}" aria-label="Ask">${esc(IN.q)}</textarea>
  ${IN.busy?`<button class="ask-go stop" data-act="insStop" aria-label="Stop"><i></i></button>`:`<button class="ask-go" data-act="askSend" aria-label="Send">${ic('up')}</button>`}</div></div></section>`}
function askTurn(R,t,i){const last=i===R.turns.length-1,n=R.srcs.length,done=R.turns.every(x=>x.done);
 let a='';
 if(R.pending)a=`<p class="ask-prog" id="insProg">${esc(IN.prog||'Searching your notes…')}</p>`;
 else{if(i===0)a+=insTop(R);
  if(R.engine==='copy'&&!t.a)a+=`<div class="ask-copy"><b>${i?'Follow-up prompt ready':'Your prompt is ready'}</b><p class="small muted">${i?'Paste it into the same chat.':`It holds your question, an overview of your plan and ${n} matching note${n===1?'':'s'}. Nothing is sent by Plotline.`}</p><div class="actions left"><button class="btn pri sm" data-act="insCopy">${ic('copy')}Copy prompt</button>${i?'':`<a class="btn sm" href="https://claude.ai/new" target="_blank" rel="noopener">Claude ${ic('ext','ico-s')}</a><a class="btn sm" href="https://chatgpt.com/" target="_blank" rel="noopener">ChatGPT ${ic('ext','ico-s')}</a><a class="btn sm" href="https://gemini.google.com/app" target="_blank" rel="noopener">Gemini ${ic('ext','ico-s')}</a>`}</div><textarea id="insPaste" rows="3" placeholder="Paste the reply here to keep it with its sources">${esc(IN.paste)}</textarea><div class="actions left"><button class="btn sm" data-act="insPasted">Show reply</button></div></div>`;
  else{a+=`${t.err?`<p class="small err">${esc(t.err)}</p>`:''}<div class="ins-ans" ${last?'id="insAns"':''}>${t.a?mdLite(t.a,n):t.done?'':'<p class="ask-dots"><i></i><i></i><i></i></p>'}</div>`;
   if(last&&!t.done)a+=`<p class="ask-prog" id="insProg">${esc(IN.prog)}</p>`}
  if(last&&done&&R.turns.some(x=>x.a&&!x.off)&&!t.off)a+=`<div class="ask-acts"><button class="btn sm ghost" data-act="insSave">${ic('journal')}Save to journal</button><button class="btn sm ghost" data-act="insCopyAns">${ic('copy')}Copy</button>${n?`<button class="btn sm ghost" data-act="askSources">${n} source${n===1?'':'s'}</button>`:''}</div>`}
 return`<div class="ask-turn"><div class="ask-me">${esc(t.q)}</div><div class="ask-ai"><span class="ask-av">${ic('ai')}</span><div class="ask-body">${a}</div></div></div>`}
function askScroll(force){if(force)ASK_STICK=true;requestAnimationFrame(()=>{const near=innerHeight+scrollY>=document.documentElement.scrollHeight-260;if(force||near)window.scrollTo({top:document.documentElement.scrollHeight,behavior:force&&!reduced()?'smooth':'auto'})})}
function askSetRefresh(){const sh=$('#sheet');if(sh&&sh.classList.contains('on')&&$('#askSet')){const y=sh.scrollTop;ACT.askSettings();sh.scrollTop=y}}
const insProg=t=>{IN.prog=t||'';const p=$('#insProg');if(p)p.textContent=IN.prog};
/* streaming paint: tokens arrive in bursts, so the text is revealed at a steady pace (a few characters a frame,
   faster when far behind). Finished lines are formatted; the line being written is plain text, so the layout
   doesn't reflow on every token. The page follows the text only when it reaches the composer. */
let ASK_ANIM=0,ASK_SHOWN=0,ASK_STICK=true;
function insPaint(){if(!ASK_ANIM)ASK_ANIM=requestAnimationFrame(askTick)}
function askTick(){ASK_ANIM=0;const a=$('#insAns'),R=IN.res;if(!a||!R)return;const t=R.turns[R.turns.length-1],full=(t.a||'').replace(/<think>[\s\S]*?(<\/think>|$)/g,'');
 if(ASK_SHOWN>full.length)ASK_SHOWN=0;const back=full.length-ASK_SHOWN;if(back>0)ASK_SHOWN+=Math.min(back,Math.max(2,Math.ceil(back/12)));
 const s=full.slice(0,ASK_SHOWN);if(!s){if(!a.querySelector('.ask-dots'))a.innerHTML='<p class="ask-dots"><i></i><i></i><i></i></p>';a.dataset.head='';return}
 const nl=s.lastIndexOf('\n'),head=nl>=0?s.slice(0,nl):'',tail=(nl>=0?s.slice(nl+1):s).replace(/\*\*/g,'');
 if(a.dataset.head!==head||!a.querySelector('.ins-tail')){a.innerHTML=(head?mdLite(head,R.srcs.length):'')+'<p class="ins-tail"></p>';a.dataset.head=head}
 a.querySelector('.ins-tail').textContent=/^\s*[-*•]\s/.test(tail)?'• '+tail.replace(/^\s*[-*•]\s+/,''):tail;
 askFollow();if(ASK_SHOWN<full.length)ASK_ANIM=requestAnimationFrame(askTick)}
function askDrain(){return new Promise(res=>{const t0=Date.now();const w=()=>{const R=IN.res,t=R&&R.turns[R.turns.length-1];if(!t||!$('#insAns')||ASK_SHOWN>=(t.a||'').replace(/<think>[\s\S]*?(<\/think>|$)/g,'').length||Date.now()-t0>4000)return res();setTimeout(w,50)};w()})}
function askFollow(){if(!ASK_STICK)return;const a=$('#insAns'),c=$('.ask-comp .ask-row');if(!a||!c)return;const gap=a.getBoundingClientRect().bottom-(c.getBoundingClientRect().top-18);if(gap>1)window.scrollBy(0,Math.min(gap,24))}
['wheel','touchmove'].forEach(ev=>window.addEventListener(ev,()=>{if(IN.busy&&insPage())ASK_STICK=false},{passive:true}));
function insTop(R){const top=R.srcs.slice(0,3);if(!top.length)return'';const q=R.turns[0].q,dated=R.srcs.slice(0,6).filter(d=>d.kind==='entry'),first=/^\s*(when|since when|how long)\b/i.test(q)&&dated.length?dated.reduce((a,b)=>a.date<=b.date?a:b):null;return`<div class="ins-top">${first?`<div class="ins-first"><span class="k">Earliest matching note</span><b>${esc(fmtDate(first.date))}</b><small>${esc(trunc(first.label,80))} · ${Math.max(0,-daysUntil(first.date))} days ago</small></div>`:''}<div class="small muted">Best matches in your notes</div>${top.map((d,i)=>{const body=d.text.slice(d.text.indexOf(': ')+2);return`<button class="ins-tc" data-act="insSrc" data-i="${i}"><span class="k">${esc(({entry:'Journal',goal:'Goal',habit:'Habit',day:'Day plan'})[d.kind])}${d.kind==='entry'||d.kind==='day'?' · '+esc(fmtDate(d.date)):''}${d.mood?' · '+esc(d.mood):''}</span><b>${esc(trunc(d.label,70))}</b><small>${esc(trunc(body,110))}</small></button>`}).join('')}</div>`}
function insEngineHTML(){const s=insSet();return`<div class="ins-eng" id="askSet">
 <p class="small muted">Plotline builds a short overview of your plan and finds the notes that match your question on this device. Only that goes to the engine you pick.</p>
 <div class="field"><label>Answer engine</label><div class="radios sm wrap">${Object.entries(ENG_N).map(([k,n])=>`<label><input type="radio" name="insE" data-ins="engine" value="${k}" ${s.engine===k?'checked':''}><span>${n}</span></label>`).join('')}</div></div>
 ${s.engine==='local'?`<p class="small muted">An open-source model runs ${NAT_OK()?'natively on this phone with llama.cpp':'inside '+(NATIVE?'the app':'this browser')+' with llama.cpp (wllama)'}. It downloads once, then works offline. Nothing leaves this device. Smaller models answer faster; bigger ones understand more.</p><div class="field"><label>Model</label><select data-ins="llm">${LLMS.map(([k,n])=>`<option value="${k}" ${llmOf(s.llm)[0]===k?'selected':''}>${n}${NAT_OK()&&natHave(llmOf(k))?' · on this phone':''}</option>`).join('')}</select></div>${NAT_OK()&&natHave(llmOf(s.llm))?`<div class="actions left"><button class="btn sm ghost" data-act="insDelModel">Delete this model from the phone</button></div>`:''}`:''}
 ${s.engine==='copy'?`<p class="small muted">You copy a ready-made prompt into Claude, ChatGPT, Gemini or any assistant. Plotline itself sends nothing.</p>`:''}
 ${s.engine==='api'?`<p class="small muted">Any OpenAI-compatible server. Open-source models on your own computer with Ollama or LM Studio keep everything at home. The key stays on this device and is never synced or backed up.</p>
  <div class="chips" style="margin:10px 0">${API_PRE.map(([n,u,m])=>`<button class="chip ${s.url===u?'on':''}" data-act="insApiPre" data-u="${u}" data-m="${m}">${n}</button>`).join('')}</div>
  <div class="field"><label for="insUrl">Server address</label><input id="insUrl" data-ins="url" value="${esc(s.url)}" placeholder="http://localhost:11434/v1" autocomplete="off" spellcheck="false"></div>
  <div class="field"><label for="insModel">Model</label><input id="insModel" data-ins="model" value="${esc(s.model)}" placeholder="llama3.2" autocomplete="off" spellcheck="false"></div>
  <div class="field"><label for="insKey">API key <small class="muted">(not needed for Ollama)</small></label><input id="insKey" data-ins="key" type="password" value="${esc(aiKey())}" autocomplete="off"></div>
  <p class="small muted">For Ollama on your computer, start it with <code>OLLAMA_ORIGINS=${esc(NATIVE?'https://appassets.androidplatform.net':location.origin)}</code>. The phone app needs an https address (for example through Tailscale).</p>
  <div class="actions left"><button class="btn sm" data-act="insTest">Test connection</button></div>`:''}
 ${s.engine==='local'&&!NAT_OK()?`<label class="sw"><span>Use the GPU<small>Faster on laptops. Off on phones by default, where it can freeze the device or garble answers.</small></span><input type="checkbox" data-ins="gpu" ${useGPU()?'checked':''}><i></i></label>`:''}
 <label class="sw"><span>Smart search<small>Finds notes by meaning, not just words. Downloads a 23 MB open-source model (all-MiniLM-L6-v2) once.</small></span><input type="checkbox" data-ins="smart" ${s.smart?'checked':''}><i></i></label>
 <label class="sw"><span>Include private entries<small>Off by default. Private journal entries stay out of every search and prompt.</small></span><input type="checkbox" data-ins="priv" ${s.priv?'checked':''}><i></i></label>
 <div class="actions left">${s.smart?`<button class="btn sm ghost" data-act="insReindex">Rebuild search index</button>`:''}<button class="btn sm ghost" data-act="insAbout">See what the AI knows about you</button></div></div>`}
async function insRun(){const R=IN.res,t=R.turns[R.turns.length-1];if(R.engine==='copy'){IN.busy=false;insProg('');render(false);return}
 ASK_SHOWN=0;insProg(R.engine==='local'?(LLM&&LLM_ID.startsWith(llmOf(insSet().llm)[0]+':')?'Thinking on this device…':'Starting the on-device model…'):'Asking your AI server…');render(false);askScroll(true);
 const onTok=x=>{if(!t.a)insProg('');t.a+=x;insPaint()};
 try{if(R.engine==='api')await apiChat(insMsgs(R),onTok,IN.ctl.signal);else if(NAT_OK())await natChat(insMsgs(R),onTok,insProg,()=>{t.a='';insPaint()});else await localChat(insMsgs(R),onTok,insProg,0,()=>{t.a='';insPaint()});if(!t.a.trim()&&!IN.ctl.signal.aborted)t.err='The model returned an empty answer.'}
 catch(e){if(e.name!=='AbortError')t.err=(e&&e.message||'Something went wrong')+(R.engine==='api'&&/fetch|network|load/i.test(e.message||'')?' · Check the address, that the server is running, and that it allows this app (CORS).':R.engine==='local'&&/fetch|network|download/i.test(e.message||'')?' · The model couldn’t download. Check your connection and try again.':R.engine==='local'&&/memory|alloc|oom|abort|range/i.test(e.message||'')?' · This device may not have enough free memory for this model. Close other apps, or pick Llama 3.2 1B in the assistant settings.':'')}
 if(t.a&&!IN.ctl.signal.aborted)await askDrain();
 if(t.a&&answerOff(t.a.replace(/<think>[\s\S]*?<\/think>/g,'')))t.a=OFF_LINE;
 if(t.a)R.msgs.push({role:'assistant',content:t.a.replace(/<think>[\s\S]*?<\/think>/g,'').trim()});t.done=true;IN.busy=false;insProg('');if(insPage())render(false)}
Object.assign(ACT,{
 askSend:()=>{const q=(IN.q||'').trim();if(!q||IN.busy)return;const follow=!!(IN.res&&!IN.res.pending&&!IN.res.offOnly);if(!askGate(q,follow))return askBlocked(q);if(follow){IN.q='';ACT.insFollow(null,null,null,q)}else ACT.insAsk(null,null,null,q)},
 askPre:d=>{const p=INS_PRE.find(x=>x.k===d.k);if(!p)return;IN.pre=p.k;ACT.insAsk(null,null,null,p.q)},
 askIdea:d=>ACT.insAsk(null,null,null,d.q),
 askPlan:()=>{AI.tab='new';go('ai')},
 askOpen:()=>{go('ask');setTimeout(()=>$('#askIn')?.focus(),450)},
 askNew:()=>{IN.ctl&&IN.ctl.abort();IN.res=null;IN.busy=false;IN.q='';IN.prog='';render(false);window.scrollTo(0,0);setTimeout(()=>$('#askIn')?.focus(),60)},
 askSettings:()=>openSheet(`<h2>Assistant settings</h2>${insEngineHTML()}<div class="actions"><button class="btn" data-act="close">Done</button></div>`),
 askScope:()=>openSheet(`<h2>What to search</h2><p class="small muted">Answers use notes from this time range. Goals and habits are always included.</p><div class="field"><label>Time range</label><div class="radios sm wrap">${INS_WIN.map(([v,n])=>`<label><input type="radio" name="insWin" data-ins="win" value="${v}" ${IN.win===v?'checked':''}><span>${n}</span></label>`).join('')}</div></div><div class="field"><label>Focus</label><select data-ins="goal"><option value="">Whole plan</option>${treeOrder(S.goals).filter(t=>t.d<2).map(t=>`<option value="${t.g.id}" ${IN.goal===t.g.id?'selected':''}>${'· '.repeat(t.d||0)}${esc(trunc(t.g.title,48))}</option>`).join('')}</select></div><div class="actions"><button class="btn pri" data-act="close">Done</button></div>`),
 askSources:()=>{const R=IN.res;if(!R)return;openSheet(`<h2>Sources</h2><p class="small muted">${R.mode==='smart'?'Smart + keyword search':'Keyword search'}${R.total?` over ${R.total} passages`:''}, plus an overview of your plan.</p><ol class="ins-src-l">${R.srcs.map((d,i)=>`<li><button class="ins-si" data-act="insSrc" data-i="${i}"><span class="n">${i+1}</span><span class="t"><b>${esc(trunc(d.label,70))}</b><small>${esc(({entry:'Journal',goal:'Goal',habit:'Habit',day:'Day plan'})[d.kind])}${d.kind==='entry'||d.kind==='day'?' · '+fmtDate(d.date):''}${d.mood?' · '+esc(d.mood):''}</small></span></button></li>`).join('')}</ol><div class="actions"><button class="btn" data-act="close">Done</button></div>`)},
 insFollow:async(d,el,ev,qq)=>{const R=IN.res,q=String(qq||'').trim();if(!R||IN.busy||!q)return;IN.busy=true;IN.paste='';IN.ctl=new AbortController();R.turns.push({q,a:'',done:false,err:''});insProg('Searching your notes…');render(false);askScroll(true);
  let fresh=[];try{const r=await ragRetrieve(q,R.opt,insProg);const have=new Set(R.srcs.map(d=>d.id));fresh=r.srcs.filter(d=>!have.has(d.id)).slice(0,R.engine==='local'?4:8)}catch(e){}
  const msg=insFollowMsg(q,fresh,R.srcs.length);R.srcs.push(...fresh);R.msgs.push({role:'user',content:msg});R.prompt=msg;await insRun()},
 insAsk:async(d,el,ev,qq)=>{if(IN.busy)return;const q=String(qq||IN.q||'').trim(),p=INS_PRE.find(x=>x.k===IN.pre&&x.q===q);if(!q)return toast('Ask a question or pick one of the suggestions');
  {const s=insSet(),m=llmOf(s.llm),gb=NAT_OK()?(m[5]||m[3]):m[3];if(s.engine==='local'&&(gb||0)>=1&&!(s.dl||{})[m[0]]&&!(NAT_OK()&&natHave(m)))return askConfirm('Download '+esc(m[1].split(' · ')[0])+'?',`This downloads ${gb} GB once from Hugging Face, then answers work offline. Use Wi-Fi.${gb>1.5?' It needs about 3 GB of free memory, and on a phone each answer can take a minute or more.':''}`,'Download',()=>{s.dl={...(s.dl||{}),[m[0]]:1};save();closeSheet();ACT.insAsk(null,null,null,q)},false)}
  if(!S.entries.length&&!S.goals.length&&!S.habits.length)return toast('Add a goal, habit or journal entry first');
  const s=insSet();IN.busy=true;IN.paste='';IN.q='';IN.pre='';IN.ctl=new AbortController();IN.res={srcs:[],turns:[{q,a:'',done:false,err:''}],engine:s.engine,msgs:[],pending:1};insProg('Searching your notes…');render(false);askScroll(true);
  try{const W=p?p.win:IN.win,r=await ragRetrieve(p?p.q+' '+(p.kw||''):q,{win:W,goal:IN.goal,all:p&&p.all,kinds:p&&p.kinds,broad:!!p||insFocus(q).broad},insProg);const stats=insStats(W,IN.goal),prompt=insPrompt(q,r,stats,W);
   IN.res={srcs:r.srcs,mode:r.mode,err:r.err,total:r.total,engine:s.engine,prompt,msgs:[{role:'system',content:'You are a personal coach inside the Plotline app. You only discuss the user’s own goals, habits, routines, moods and journal, and general coaching on those, using only the data provided. For anything else reply exactly: '+OFF_LINE+' Cite notes like [2].'},{role:'user',content:prompt}],turns:[{q,a:'',done:false,err:''}],opt:{win:W,goal:IN.goal}}}
  catch(e){IN.busy=false;IN.res=null;insProg('');toast('Search failed: '+(e.message||e));render(false);return}
  await insRun()},
 insStop:()=>{IN.ctl&&IN.ctl.abort()},
 insCopy:async()=>toast(IN.res&&await copyText(IN.res.prompt)?'Prompt copied. Paste it into any AI':'Copy failed. Try again'),
 insPasted:()=>{const R=IN.res;if(!R)return;if(!IN.paste.trim())return toast('Paste the reply first');const t=R.turns[R.turns.length-1];t.a=IN.paste.trim();t.done=true;IN.paste='';R.msgs.push({role:'assistant',content:t.a});render(false)},
 insCopyAns:async()=>{const R=IN.res;toast(R&&await copyText(R.turns.map((t,i)=>(i?'Q: '+t.q+'\n':'')+t.a.replace(/<think>[\s\S]*?<\/think>/g,'').trim()).join('\n\n'))?'Copied':'Copy failed')},
 insSave:()=>{const R=IN.res;if(!R||!R.turns.some(t=>t.a))return;const html=cleanHTML(R.turns.filter(t=>!t.off).map((t,i)=>(i?`<p><b>${esc(t.q)}</b></p>`:'')+mdLite(t.a,0)).join(''));const text=htmlText(html);
  S.entries.push({id:uid(),t:Date.now(),type:'note',goalId:R.opt.goal||null,title:'Insight · '+trunc(R.turns[0].q,60),text,html,mood:'',private:false,sid:null,img:null});save();toast('Saved to your journal')},
 insSrc:d=>{const x=IN.res&&IN.res.srcs[+d.i];if(!x)return;closeSheet();if(x.kind==='entry'){if(S.entries.some(e=>e.id===x.ref))ACT.viewEntry({id:x.ref})}else if(x.kind==='goal')go('goal/'+x.ref);else if(x.kind==='habit')go('habit/'+x.ref);else if(x.kind==='day'){CAL.d=x.ref;S.settings.layout.cal='day';go('cal')}},
 insDelModel:()=>{const m=llmOf(insSet().llm);askConfirm('Delete '+esc(m[1].split(' · ')[0])+'?','The model file is removed from this phone. You can download it again any time.','Delete',()=>{NATIVE.llmDelete(natFile(m));NAT_WARM='';const s=insSet();if(s.dl)delete s.dl[m[0]];save();closeSheet();render(false);toast('Model deleted')})},
 insAbout:()=>openSheet(`<h2>What the AI knows about you</h2><p class="small muted">This overview goes with every question, together with the notes that match it. It’s built on this device from your plan.</p><pre class="spec">${esc(insOverview(insSet().engine==='local'?(IS_PHONE?1200:1800):4000))}</pre><div class="actions"><button class="btn" data-act="close">Done</button></div>`),
 insApiPre:d=>{const s=insSet();s.url=d.u;s.model=d.m;save();askSetRefresh()},
 insTest:async()=>{const s=insSet();try{const r=await fetch(s.url.replace(/\/+$/,'')+'/models',{headers:aiKey()?{Authorization:'Bearer '+aiKey()}:{}});if(!r.ok)throw new Error('status '+r.status);const j=await r.json().catch(()=>({}));const ids=(j.data||[]).map(m=>m.id);toast(ids.length?`Connected · ${ids.length} model${ids.length===1?'':'s'}${ids.includes(s.model)?'':' · “'+s.model+'” not found'}`:'Connected')}catch(e){toast('Couldn’t reach the server. Check the address and CORS')}},
 insReindex:async()=>{VEC=new Map();await DB.set('ragvec',{m:RAG_CFG.model,v:{}}).catch(()=>{});try{insProg('Rebuilding…');await ensureVecs(ragDocs(),insProg);insProg('');toast('Search index rebuilt')}catch(e){insProg('');toast('Couldn’t load the search model. Check your connection')}}
});
document.addEventListener('input',e=>{const t=e.target;if(t.id==='askIn'){IN.q=t.value;t.style.height='auto';t.style.height=Math.min(160,t.scrollHeight)+'px'}else if(t.id==='insPaste')IN.paste=t.value;
 else if(t.dataset&&(t.dataset.ins==='url'||t.dataset.ins==='model')){insSet()[t.dataset.ins]=t.value.trim();save()}else if(t.dataset&&t.dataset.ins==='key'){try{t.value?localStorage.setItem('plotline.aikey',t.value.trim()):localStorage.removeItem('plotline.aikey')}catch(_){}}});
document.addEventListener('keydown',e=>{if(e.target&&e.target.id==='askIn'&&e.key==='Enter'&&!e.shiftKey&&(!IS_PHONE||e.ctrlKey||e.metaKey)){e.preventDefault();ACT.askSend()}});
document.addEventListener('change',e=>{const t=e.target,k=t.dataset&&t.dataset.ins;if(!k||k==='url'||k==='model'||k==='key')return;const s=insSet();
 if(k==='win'){IN.win=+t.value;if(insPage())render(false);return}if(k==='goal'){IN.goal=t.value;if(insPage())render(false);return}
 if(k==='engine'||k==='llm')s[k]=t.value;else if(k==='smart'||k==='priv'||k==='gpu')s[k]=t.checked;save();
 if(k==='smart'&&t.checked){toast('Preparing smart search…');ensureVecs(ragDocs(),()=>{}).then(()=>toast('Smart search is ready')).catch(()=>toast('Couldn’t load the search model. Keyword search still works'))}
 if(insPage())render(false);askSetRefresh()});
