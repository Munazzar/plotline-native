/* ================= HABITS & ROUTINES =================
   S.habits[] syncs like goals (id + u, tombstones in dead). Kinds:
   build   — do something on a schedule (daily / chosen weekdays / N times a week), optional daily amount
   routine — an ordered checklist with optional minutes per step, run with a guided player
   quit    — break a habit: "quit" runs a live clean-time clock with milestones; "limit" caps it per day */
Object.assign(IC,{habit:'<path d="M20 12a8 8 0 1 1-2.3-5.6"/><path d="M20 4v4h-4"/><path d="M8.5 12.5l2.5 2.5 4.5-5"/>',
 flame:'<path d="M12 22c4.2 0 7-2.9 7-6.8 0-3.4-2.2-5.6-3.9-8.7-.6 1.6-1.6 2.7-2.9 3.1.2-2.9-1-5.7-3.4-7.6.2 3.6-1.6 5.5-3.2 7.6C4.6 11.3 5 13.6 5 15.2 5 19.1 7.8 22 12 22z"/>',
 play:'<path d="M8 5.5v13l10.5-6.5z"/>',pause:'<path d="M9 5.5v13M15 5.5v13"/>',skip:'<path d="M6 5.5l9 6.5-9 6.5z"/><path d="M18 5.5v13"/>',
 wave:'<path d="M2.5 10c2.4-3 4.8-3 7.2 0s4.8 3 7.2 0 3.6-2.2 4.6-1.4"/><path d="M2.5 16c2.4-3 4.8-3 7.2 0s4.8 3 7.2 0 3.6-2.2 4.6-1.4"/>',
 shield:'<path d="M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6z"/><path d="M8.8 12.2l2.2 2.2 4.2-4.4"/>',archive:'<rect x="3" y="4" width="18" height="5" rx="1.5"/><path d="M5 9v10h14V9M10 13h4"/>'});
const HKIND=[['build','Build'],['quit','Break'],['routine','Routine']];const HKN={build:'Building',quit:'Breaking',routine:'Routine'};
const HPARTS=[['morning','Morning'],['afternoon','Afternoon'],['evening','Evening'],['any','Anytime']];
const HICONS=['💧','📖','🏃','🚶','💪','🧘','🙏','🛏️','🌙','☀️','🥗','🍎','🦷','✍️','🧠','🎧','🎯','💼','💸','🧹','👨‍👩‍👧','🌱','⏰','🚭','🍬','☕','📵','📱','🎮','🍔','🍺','🛒'];
const TRIGS=['Stress','Boredom','Tired','Social','Hungry','A usual cue','Other'];
const MILES=[1,3,7,14,21,30,60,90,180,365,730,1095];
const SMILES=[3,7,14,21,30,50,66,100,150,200,365],WMILES=[2,4,8,12,26,52];
const ACOL={health:'#46C99B',career:'#6B9BFF',family:'#FF8C6B',faith:'#F0C862',finance:'#A9D14A',learning:'#AC93FF',personal:'#F585B8'};
function normHabit(h){const k=['build','quit','routine'].includes(h.kind)?h.kind:'build';
 const o={kind:k,title:'Habit',icon:'',why:'',cue:'',mini:'',plan:'',area:'personal',goalId:'',freq:'daily',days:[0,1,2,3,4,5,6],times:3,target:1,unit:'',part:'any',time:'',remind:false,steps:[],log:{},rs:{},skip:{},pz:[],mode:'quit',limit:1,start:0,slips:[],urges:[],cost:0,cur:'$',mins:0,mile:0,smile:0,status:'active',startDate:'',createdAt:Date.now(),...h};
 o.kind=k;if(!AREA_IDS.includes(o.area))o.area='personal';if(!['daily','days','times'].includes(o.freq))o.freq='daily';
 o.days=Array.isArray(o.days)?o.days.map(Number).filter(x=>x>=0&&x<7):[];if(!o.days.length)o.days=[0,1,2,3,4,5,6];
 o.times=Math.min(7,Math.max(1,+o.times||3));o.target=Math.max(1,Math.min(999,+o.target||1));o.limit=Math.max(0,+o.limit||0);if(o.mode!=='limit')o.mode='quit';
 if(!validDate(o.startDate))o.startDate=ymd(new Date(o.createdAt||Date.now()));if(k==='quit'&&!o.start)o.start=o.createdAt||Date.now();
 for(const key of ['log','rs','skip'])if(!o[key]||typeof o[key]!=='object'||Array.isArray(o[key]))o[key]={};
 for(const key of ['steps','slips','urges','pz'])if(!Array.isArray(o[key]))o[key]=[];
 if(!['active','paused','archived'].includes(o.status))o.status='active';return o}
const H=id=>S.habits.find(h=>h.id===id);
const hIcon=h=>h.icon||(h.kind==='quit'?'🛡️':h.kind==='routine'?'🔁':'🌱');
const dowOf=ds=>dUTC(dnum(ds)).getUTCDay();
const wkStart=n=>n-dUTC(n).getUTCDay();
const hPaused=(h,ds)=>h.pz.some(p=>ds>=p.a&&(!p.b||ds<=p.b));
const hTarget=h=>h.kind==='routine'&&h.steps.length?h.steps.length:h.target;
function hVal(h,ds){if(h.kind==='routine'&&h.steps.length){const ids=new Set(h.steps.map(s=>s.id));return(h.rs[ds]||[]).filter(x=>ids.has(x)).length}return+h.log[ds]||0}
const hDone=(h,ds)=>h.kind!=='quit'&&hVal(h,ds)>=hTarget(h);
function hDue(h,ds){if(h.kind==='quit'||h.status==='archived'||ds<h.startDate||hPaused(h,ds))return false;return h.freq!=='days'||h.days.includes(dowOf(ds))}
function weekCount(h,w){let n=0;for(let k=w;k<w+7;k++)if(hDone(h,fromN(k)))n++;return n}
/* counts toward "today": due, not a rest day, and for N-a-week habits only while the week still needs it (or it's done today) */
function hCounts(h,ds){if(!hDue(h,ds)||h.skip[ds])return false;if(h.freq==='times'&&!hDone(h,ds)&&weekCount(h,wkStart(dnum(ds)))>=h.times)return false;return true}
function hSetVal(h,ds,v){v=Math.max(0,Math.round(+v||0));if(h.kind==='routine'&&h.steps.length){if(v>=h.steps.length)h.rs[ds]=h.steps.map(s=>s.id);else if(!v)delete h.rs[ds];else h.rs[ds]=h.steps.slice(0,v).map(s=>s.id)}else if(v)h.log[ds]=v;else delete h.log[ds];if(v)delete h.skip[ds]}
function hStreak(h){const tn=dnum(ymd()),st=dnum(h.startDate);
 if(h.freq==='times'){let n=0,w=wkStart(tn);if(weekCount(h,w)>=h.times)n++;w-=7;while(w+6>=st&&n<600){if(hPaused(h,fromN(w+6))||weekCount(h,w)>=h.times){if(!hPaused(h,fromN(w+6)))n++}else break;w-=7}return n}
 let n=0;for(let k=tn;k>=st&&tn-k<4000;k--){const ds=fromN(k);if(!hDue(h,ds)||h.skip[ds])continue;if(hDone(h,ds))n++;else if(k===tn)continue;else break}return n}
function hBest(h){const tn=dnum(ymd()),st=dnum(h.startDate);let b=0,r=0;
 if(h.freq==='times'){for(let w=wkStart(st);w<=tn;w+=7){if(weekCount(h,w)>=h.times){r++;b=Math.max(b,r)}else if(w+6<tn)r=0}return b}
 for(let k=st;k<=tn;k++){const ds=fromN(k);if(!hDue(h,ds)||h.skip[ds])continue;if(hDone(h,ds)){r++;b=Math.max(b,r)}else if(k<tn)r=0}return b}
/* habit strength: an exponential average of recent days, so one miss dents it and a comeback repairs it */
function hStrength(h){const tn=dnum(ymd()),st=Math.max(dnum(h.startDate),tn-180);let s=0;
 if(h.freq==='times'){for(let w=wkStart(st);w<=tn;w+=7){const c=weekCount(h,w);if(w+6>=tn&&c<h.times)break;s+=(Math.min(1,c/h.times)-s)*.25}return Math.round(s*100)}
 for(let k=st;k<=tn;k++){const ds=fromN(k);if(!hDue(h,ds)||h.skip[ds])continue;if(k===tn&&!hDone(h,ds))break;s+=(Math.min(1,hVal(h,ds)/hTarget(h))-s)*.08}return Math.round(s*100)}
function hRate(h,days=30){const tn=dnum(ymd()),st=dnum(h.startDate);
 if(h.freq==='times'){let dn=0,w=0;for(let k=Math.max(st,tn-27);k<=tn;k++)if(hDone(h,fromN(k)))dn++;w=Math.max(1,Math.ceil((tn-Math.max(st,tn-27)+1)/7));return Math.min(100,Math.round(dn/(h.times*w)*100))}
 let due=0,dn=0;for(let k=Math.max(st,tn-days+1);k<=tn;k++){const ds=fromN(k);if(!hDue(h,ds)||h.skip[ds])continue;if(k===tn&&!hDone(h,ds))continue;due++;if(hDone(h,ds))dn++}return due?Math.round(dn/due*100):null}
const freqText=h=>h.freq==='times'?`${h.times}× a week`:h.freq==='days'?(h.days.length===7?'Every day':h.days.join()==='1,2,3,4,5'?'Weekdays':h.days.join()==='0,6'?'Weekends':h.days.map(d=>DAYS[d]).join(', ')):'Every day';
const routineMin=h=>h.steps.reduce((a,s)=>a+(+s.min||0),0);
/* ---- breaking a habit ---- */
const qSince=h=>Math.max(h.start||0,...h.slips.map(s=>s.t));
const qDays=h=>Math.max(0,Math.floor((Date.now()-qSince(h))/864e5));
function qBest(h){if(h.mode==='limit')return lBest(h);const ts=[h.start,...h.slips.map(s=>s.t).filter(t=>t>=h.start)].sort((a,b)=>a-b);let b=0;for(let i=0;i<ts.length;i++){const e=i+1<ts.length?ts[i+1]:Date.now();b=Math.max(b,e-ts[i])}return Math.floor(b/864e5)}
const lOk=(h,ds)=>(+h.log[ds]||0)<=h.limit;
function lStreak(h){const tn=dnum(ymd()),st=dnum(h.startDate);let n=0;for(let k=tn;k>=st&&tn-k<4000;k--){if(lOk(h,fromN(k)))n++;else break}return n}
function lBest(h){const tn=dnum(ymd()),st=dnum(h.startDate);let b=0,r=0;for(let k=st;k<=tn;k++){if(lOk(h,fromN(k))){r++;b=Math.max(b,r)}else r=0}return b}
function qSaved(h){if(h.mode==='limit')return{money:0,mins:0};const slipDays=new Set(h.slips.filter(s=>s.t>=h.start).map(s=>ymd(new Date(s.t)))).size;const d=Math.max(0,(Date.now()-h.start)/864e5-slipDays);return{money:d*(+h.cost||0),mins:d*(+h.mins||0)}}
const money=(h,x)=>`${h.cur||'$'}${x>=100?Math.round(x).toLocaleString():x.toFixed(x<10?2:0)}`;
const hrs=m=>m>=60?`${Math.round(m/60)} h`:`${Math.round(m)} min`;
const mileName=m=>m>=365?`${m/365} year${m>365?'s':''}`:m>=30?`${Math.round(m/30)} month${m>=60?'s':''}`:m%7===0&&m>=7?`${m/7} week${m>7?'s':''}`:`${m} day${m>1?'s':''}`;
function clockHTML(ms,short){ms=Math.max(0,ms);const d=Math.floor(ms/864e5),h=Math.floor(ms%864e5/36e5),m=Math.floor(ms%36e5/6e4),s=Math.floor(ms%6e4/1e3);
 return short?`<b>${d}</b>d <b>${h}</b>h`:`<b>${d}</b><small>${d===1?'day':'days'}</small><span class="hms">${pad2(h)}:${pad2(m)}:${pad2(s)}</span>`}
/* ---- milestones: streak and clean-time wins go to the journal ---- */
const floorMile=(L,v)=>[...L].reverse().find(m=>m<=v)||0;
function checkMilestones(announce=true){let ch=false,won=null;
 S.habits.forEach(h=>{if(h.status!=='active')return;
  if(h.kind==='quit'){const v=h.mode==='limit'?lStreak(h)-1:qDays(h),L=h.mode==='limit'?SMILES:MILES,m=floorMile(L,v);if(m>(h.mile||0)){h.mile=m;ch=true;if(announce){won=[h,h.mode==='limit'?`${m} days under your limit`:`${mileName(m)} free`];S.entries.push({id:uid(),t:Date.now(),type:'habit',goalId:h.goalId||null,text:`${won[1]} · ${h.title}`,sid:null,img:null})}}else if(m<(h.mile||0)){h.mile=m;ch=true}return}
  const s=hStreak(h),L=h.freq==='times'?WMILES:SMILES,m=floorMile(L,s);
  if(m>(h.smile||0)){h.smile=m;ch=true;if(announce){const lab=h.freq==='times'?`${m}-week streak`:`${m}-day streak`;won=[h,lab];S.entries.push({id:uid(),t:Date.now(),type:'habit',goalId:h.goalId||null,text:`${lab} · ${h.title}`,sid:null,img:null})}}else if(m<(h.smile||0)){h.smile=m;ch=true}});
 if(won){burst();setTimeout(()=>toast(`${esc(hIcon(won[0]))} Milestone: ${esc(won[1])}`),350)}
 return ch}

/* ================= habit views ================= */
let HV='today',JUST='';
function hRing(f,sz=48,sw=4){const r=(sz-sw)/2-1,C=2*Math.PI*r;return`<svg class="hring" viewBox="0 0 ${sz} ${sz}" aria-hidden="true"><circle cx="${sz/2}" cy="${sz/2}" r="${r}" class="a" stroke-width="${sw}"/><circle cx="${sz/2}" cy="${sz/2}" r="${r}" class="b" stroke-width="${sw}" stroke-dasharray="${C.toFixed(1)}" stroke-dashoffset="${(C*(1-Math.min(1,Math.max(0,f)))).toFixed(1)}"/></svg>`}
function hChk(h){const td=ymd(),v=hVal(h,td),n=hTarget(h),dn=v>=n,rt=h.kind==='routine'&&h.steps.length;
 const inner=dn?ic('check'):rt?ic('play'):n>1?`<b>${v}</b><small>/${n}</small>`:'';
 return`<button class="hchk${dn?' on':''}${JUST===h.id&&dn?' pop':''}" ${rt&&!dn?`data-act="routineGo"`:`data-act="hTap"`} data-id="${h.id}" aria-label="${rt&&!dn?'Start routine':dn?'Done, tap to undo':n>1?`Add one (${v} of ${n})`:'Mark done'}: ${esc(h.title)}">${hRing(v/n)}<span>${inner}</span></button>`}
function hWeek(h){const tn=dnum(ymd());let o='';for(let k=tn-6;k<=tn;k++){const ds=fromN(k),v=hVal(h,ds),n=hTarget(h);let c;
  if(ds<h.startDate)c='n';else if(h.skip[ds])c='s';else if(v>=n)c='d';else if(v>0)c='p';else if(!hDue(h,ds))c='n';else if(k===tn)c='t';else c=h.freq==='times'?'n':'x';
  o+=`<button class="hd ${c}" data-act="hDay" data-id="${h.id}" data-d="${ds}" aria-label="${dayName(ds)}: ${c==='d'?'done':c==='s'?'rest day':'not done'}"><i></i><em>${DAYS[dUTC(k).getUTCDay()][0]}</em></button>`}return`<div class="hweek">${o}</div>`}
function hSub(h){const td=ymd(),s=hStreak(h),out=[];if(h.status==='paused')out.push('Paused');
 if(s)out.push(`<span class="hfl">${ic('flame')}${s}${h.freq==='times'?' wk':''}</span>`);
 if(h.freq==='times')out.push(`${weekCount(h,wkStart(dnum(td)))} of ${h.times} this week`);else if(h.freq==='days'&&h.days.length<7)out.push(freqText(h));
 if(h.kind==='routine')out.push(`${h.steps.length} step${h.steps.length===1?'':'s'}${routineMin(h)?' · '+routineMin(h)+' min':''}`);
 if(h.cue)out.push(`<span class="ell">${esc(h.cue)}</span>`);else if(h.time)out.push(fmtTime(h.time));
 return out.join('<i class="sep"></i>')}
function hrow(h){const td=ymd();
 return`<div class="hrow${hDone(h,td)?' done':''}${h.status!=='active'?' off':''}" style="${cvar(h)}" data-act="openHabit" data-id="${h.id}"><span class="hic">${esc(hIcon(h))}</span><div class="hb"><div class="t ell">${esc(h.title)}${h.kind==='build'&&h.target>1?`<span class="hu">${h.target} ${esc(h.unit)}</span>`:''}</div><div class="s">${hSub(h)}</div>${hWeek(h)}</div>${h.status==='active'&&!h.skip[td]?hChk(h):h.skip[td]?'<span class="data">Rest day</span>':''}</div>`}
function qcard(h){const now=Date.now(),td=ymd();
 if(h.mode==='limit'){const v=+h.log[td]||0,over=v>h.limit,st=lStreak(h);
  return`<article class="hq tint${over?' over':''}" style="${cvar(h)}" data-act="openHabit" data-id="${h.id}"><div class="hq-top"><span class="data">Cutting down · max ${h.limit}${h.unit?' '+esc(h.unit):''} a day</span><span class="hic sm">${esc(hIcon(h))}</span></div><h3>${esc(h.title)}</h3>
  <div class="hq-lim"><b>${v}</b><span>of ${h.limit} today</span></div><div class="pbar"><i style="width:${Math.min(100,h.limit?v/h.limit*100:v?100:0)}%"></i></div><div class="data">${over?'Over the limit today · tomorrow is a fresh start':`${Math.max(0,h.limit-v)} left today`}${st?` · ${st} day${st>1?'s':''} on track`:''}</div>
  <div class="hq-a"><button class="btn ink sm" data-act="hLimit" data-id="${h.id}" data-n="1">${ic('plus')}Log one</button>${v?`<button class="btn inkline sm" data-act="hLimit" data-id="${h.id}" data-n="-1">Undo</button>`:''}<button class="btn inkline sm" data-act="urge" data-id="${h.id}">${ic('wave')}Urge</button></div></article>`}
 const since=qSince(h),d=Math.floor((now-since)/864e5),nx=MILES.find(m=>m>d)||d+365,pv=floorMile(MILES,d),f=(now-since-pv*864e5)/((nx-pv)*864e5),sv=qSaved(h);
 return`<article class="hq tint" style="${cvar(h)}" data-act="openHabit" data-id="${h.id}"><div class="hq-top"><span class="data">${h.slips.some(s=>s.t>=h.start)?'Since your last slip':'Free since '+fmtDate(ymd(new Date(since)))}</span><span class="hic sm">${esc(hIcon(h))}</span></div><h3>${esc(h.title)}</h3>
 <div class="hq-clock" data-since="${since}">${clockHTML(now-since)}</div><div class="pbar"><i style="width:${(f*100).toFixed(1)}%"></i></div><div class="data">Next milestone: ${mileName(nx)}${sv.money>=.5?' · '+money(h,sv.money)+' saved':sv.mins>=30?' · '+hrs(sv.mins)+' back':''}</div>
 <div class="hq-a"><button class="btn ink sm" data-act="urge" data-id="${h.id}">${ic('wave')}I have an urge</button><button class="btn inkline sm" data-act="slip" data-id="${h.id}">Log a slip</button></div></article>`}
function todayHabits(){const td=ymd();return S.habits.filter(h=>h.status==='active'&&h.kind!=='quit'&&hDue(h,td)).sort((a,b)=>(a.time||'99')<(b.time||'99')?-1:(a.time||'99')>(b.time||'99')?1:(a.createdAt||0)-(b.createdAt||0))}
function dayRatio(ds){let due=0,dn=0;S.habits.forEach(h=>{if(h.kind==='quit'||h.status==='archived'||!hDue(h,ds)||h.skip[ds])return;const d=hDone(h,ds);if(h.freq==='times'&&!d)return;due++;if(d)dn++});return{due,dn}}
function perfectRun(){const tn=dnum(ymd()),ms=Math.min(tn,...S.habits.map(h=>dnum(h.startDate)));let n=0;for(let k=tn;k>=ms&&k>tn-800;k--){const r=dayRatio(fromN(k));if(!r.due)continue;if(r.dn>=r.due)n++;else if(k===tn)continue;else break}return n}
function hSummary(){const tn=dnum(ymd()),t=dayRatio(ymd()),f=t.due?t.dn/t.due:0;let bars='';
 for(let k=tn-6;k<=tn;k++){const r=dayRatio(fromN(k)),p=r.due?r.dn/r.due:0;bars+=`<div class="${k===tn?'now':''}"><i class="${p?'':'z'}" style="height:${Math.max(6,p*100)}%"></i><span>${DAYS[dUTC(k).getUTCDay()][0]}</span></div>`}
 const pr=perfectRun(),wk=[...Array(7)].map((_,i)=>dayRatio(fromN(tn-i))).reduce((a,r)=>({due:a.due+r.due,dn:a.dn+r.dn}),{due:0,dn:0});
 return`<section class="hsum rv"><div class="big" style="--c:var(--accent)">${hRing(f,104,9)}<b>${Math.round(f*100)}<small>%</small></b></div><div><div class="data">Today · ${t.dn} of ${t.due} done</div><div class="hbars">${bars}</div><div class="data" style="margin-top:10px">${wk.due?Math.round(wk.dn/wk.due*100)+'% kept this week':'This week'}${pr>1?` · ${pr} perfect days in a row`:''}</div></div></section>`}
function hNudge(){const yd=addDays(-1),td=ymd();const miss=S.habits.filter(h=>h.status==='active'&&h.kind!=='quit'&&h.freq!=='times'&&hDue(h,yd)&&!h.skip[yd]&&!hDone(h,yd)&&!hDone(h,td)&&hDue(h,td)&&yd>=h.startDate);if(!miss.length)return'';
 const m=miss.find(h=>h.mini);return`<div class="hnudge rv">${ic('flag')}<div><b>Missed yesterday: ${miss.slice(0,3).map(h=>esc(h.title)).join(', ')}${miss.length>3?' +'+(miss.length-3):''}</b><div class="small muted">Missing once is normal. Try not to miss twice.${m?` The small version counts: “${esc(m.mini)}”.`:''}</div></div></div>`}
function vHabits(){const all=S.habits.filter(h=>h.status!=='archived'),td=ymd(),t=dayRatio(td);
 const head=`<header class="ph"><div><h1>Habits</h1><div class="data">${all.length?(t.due?`${t.dn} of ${t.due} done today`:'Nothing due today')+` · ${all.filter(h=>h.kind==='quit').length} being broken`:'Build good ones. Break the rest.'}</div></div><div class="ph-r"><button class="ibtn" data-act="newHabit" aria-label="New habit">${ic('plus')}</button>${gear()}</div></header>`;
 if(!S.habits.length)return head+`<div class="empty rv"><span class="hic" style="--c:var(--accent);margin:0 auto 16px;width:64px;height:64px;font-size:30px">🌱</span><h3>Small things, done daily</h3><p>Build a habit, run a routine, or break one that holds you back. Start from a template or from scratch.</p><div class="actions"><button class="btn pri" data-act="newHabit">${ic('plus')}New habit</button></div></div>${tplGrid()}`;
 const bar=`<div class="bar">${segHTML('hv','hView',[['today','Today'],['build','Build'],['quit','Break'],['routine','Routines']],HV)}</div>`;
 let body='';
 if(HV==='today'){const hs=todayHabits();body+=hs.length?hSummary()+hNudge():'';
  HPARTS.forEach(([k,n])=>{const L=hs.filter(h=>(h.part||'any')===k);if(L.length)body+=`<section class="rv"><div class="st-h"><h3>${n}</h3><span class="data">${L.filter(h=>hDone(h,td)).length} of ${L.length}</span></div><div class="list">${L.map(hrow).join('')}</div></section>`});
  const qs=all.filter(h=>h.kind==='quit'&&h.status==='active');if(qs.length)body+=`<section class="rv"><div class="st-h"><h3>Breaking</h3><button class="link" data-act="hView" data-v="quit">All</button></div><div class="hqgrid">${qs.map(qcard).join('')}</div></section>`;
  const later=all.filter(h=>h.kind!=='quit'&&h.status==='active'&&!hDue(h,td));if(later.length)body+=`<section class="rv"><div class="st-h"><h3>Not today</h3></div><div class="chips">${later.map(h=>`<button class="chip" style="${cvar(h)}" data-act="openHabit" data-id="${h.id}">${esc(hIcon(h))} ${esc(h.title)} <span class="data">${esc(freqText(h))}</span></button>`).join('')}</div></section>`;
  if(!hs.length&&!qs.length)body+=`<div class="empty rv"><p>Nothing scheduled for today. Enjoy it, or add something new.</p><div class="actions"><button class="btn pri" data-act="newHabit">${ic('plus')}New habit</button></div></div>`}
 else{const L=all.filter(h=>h.kind===HV);
  if(!L.length)body+=`<div class="empty rv"><p>${HV==='quit'?'Nothing to break yet. Quit something completely, or cut it down to a daily limit.':HV==='routine'?'No routines yet. A routine is a short sequence you run the same way each time, like a morning start.':'No habits to build yet.'}</p><div class="actions"><button class="btn pri" data-act="newHabit" data-k="${HV}">${ic('plus')}${HV==='quit'?'Break a habit':HV==='routine'?'New routine':'New habit'}</button></div></div>${tplGrid(HV)}`;
  else if(HV==='quit')body+=`<div class="hqgrid rv">${L.map(qcard).join('')}</div>`;
  else if(HV==='routine')body+=`<div class="rgrid">${L.map(rcard).join('')}</div>`;
  else body+=`<div class="list rv">${L.map(hrow).join('')}</div>`}
 const arch=S.habits.filter(h=>h.status==='archived'&&(HV==='today'||h.kind===HV));
 if(arch.length)body+=`<details class="rv harch"><summary class="data">Archived · ${arch.length}</summary><div class="list" style="margin-top:10px">${arch.map(hrow).join('')}</div></details>`;
 return head+bar+body}
function rcard(h){const td=ymd(),v=hVal(h,td),n=hTarget(h),s=hStreak(h);
 return`<article class="rcard tint rv" style="${cvar(h)}" data-act="openHabit" data-id="${h.id}"><div class="hq-top"><span class="data">${HPARTS.find(p=>p[0]===h.part)?.[1]||'Anytime'} · ${routineMin(h)?routineMin(h)+' min':h.steps.length+' steps'}</span><span class="hic sm">${esc(hIcon(h))}</span></div><h3>${esc(h.title)}</h3>
 <ol class="rsteps">${h.steps.slice(0,6).map(st=>`<li class="${(h.rs[td]||[]).includes(st.id)?'d':''}"><i>${(h.rs[td]||[]).includes(st.id)?ic('check'):''}</i><span class="ell">${esc(st.title)}</span>${st.min?`<em>${st.min}m</em>`:''}</li>`).join('')}${h.steps.length>6?`<li class="more">+${h.steps.length-6} more</li>`:''}</ol>
 <div class="hq-a"><button class="btn ink sm" data-act="routineGo" data-id="${h.id}">${ic('play')}${v>=n?'Run again':v?'Continue':'Start'}</button><span class="data">${v>=n?'Done today':v?`${v} of ${n} today`:''}${s?` · ${s}${h.freq==='times'?' wk':'-day'} streak`:''}</span></div></article>`}
/* ---- detail ---- */
function hHeat(h){const W=20,tn=dnum(ymd()),a=wkStart(tn)-7*(W-1);let o='';
 for(let k=a;k<a+7*W;k++){const ds=fromN(k);let c,act='';
  if(k>tn)c='f';else if(h.kind==='quit'){if(ds<ymd(new Date(h.start)))c='n';else if(h.mode==='limit'){const v=+h.log[ds]||0;c=v>h.limit?'x':v?'p':'d'}else c=h.slips.some(s=>ymd(new Date(s.t))===ds)?'x':'d'}
  else{act=`data-act="hDaySheet" data-id="${h.id}" data-d="${ds}"`;const v=hVal(h,ds),n=hTarget(h);c=ds<h.startDate?'n':h.skip[ds]?'s':v>=n?'d':v?'p':!hDue(h,ds)||k===tn||h.freq==='times'?'n':'x'}
  o+=`<button class="${c}${k===tn?' now':''}" ${act} aria-label="${dUTC(k).toLocaleDateString(undefined,{month:'short',day:'numeric',timeZone:'UTC'})}" ${act?'':'tabindex="-1"'}></button>`}
 const mo=[...Array(W)].map((_,i)=>{const d=dUTC(a+7*i);return d.getUTCDate()<=7?`<span style="grid-column:${i+1}">${d.toLocaleDateString(undefined,{month:'short',timeZone:'UTC'})}</span>`:''}).join('');
 return`<div class="hheat"><div class="hmo" style="grid-template-columns:repeat(${W},1fr)">${mo}</div><div class="hmap" style="grid-template-columns:repeat(${W},1fr)">${o}</div><div class="hleg data"><span><i class="d"></i>${h.kind==='quit'?(h.mode==='limit'?'Under limit':'Free'):'Done'}</span><span><i class="p"></i>${h.kind==='quit'?'Some':'Partly'}</span><span><i class="x"></i>${h.kind==='quit'?(h.mode==='limit'?'Over':'Slip'):'Missed'}</span>${h.kind!=='quit'?'<span><i class="s"></i>Rest day</span>':''}</div></div>`}
function hWeekdays(h){const tn=dnum(ymd()),st=Math.max(dnum(h.startDate),tn-84),due=Array(7).fill(0),dn=Array(7).fill(0);
 for(let k=st;k<tn;k++){const ds=fromN(k),w=dUTC(k).getUTCDay();if(!hDue(h,ds)||h.skip[ds])continue;if(h.freq==='times'&&!hDone(h,ds))continue;due[w]++;if(hDone(h,ds))dn[w]++}
 if(due.reduce((a,b)=>a+b,0)<10||h.freq==='times')return'';const r=due.map((d,i)=>d?dn[i]/d:null);const idx=r.map((v,i)=>[v,i]).filter(x=>x[0]!=null).sort((a,b)=>b[0]-a[0]);
 const best=idx[0],worst=idx[idx.length-1];
 return`<section class="rv"><div class="st-h"><h3>By weekday</h3><span class="data">last 12 weeks</span></div><div class="panel"><div class="hbars wd">${r.map((v,i)=>`<div><i class="${v?'':'z'}" style="height:${v==null?4:Math.max(6,v*100)}%;${v==null?'opacity:.3':''}"></i><span>${DAYS[i].slice(0,2)}</span></div>`).join('')}</div>${best&&worst&&best[0]-worst[0]>=.2?`<p class="small muted" style="margin-top:12px">Strongest on ${DAYS[best[1]]} (${Math.round(best[0]*100)}%). ${DAYS[worst[1]]} is the weak spot (${Math.round(worst[0]*100)}%). Plan something specific for that day.</p>`:`<p class="small muted" style="margin-top:12px">Steady across the week.</p>`}</div></section>`}
function hTiles(L){return`<div class="hstats rv">${L.map(([v,l,x])=>`<div class="hst"><b ${x||''}>${v}</b><span class="data">${l}</span></div>`).join('')}</div>`}
function slipInsight(h){const sl=h.slips.filter(s=>s.t>=h.start);if(sl.length<2)return'';const c={};sl.forEach(s=>{if(s.trig)c[s.trig]=(c[s.trig]||0)+1});const top=Object.entries(c).sort((a,b)=>b[1]-a[1])[0];
 const part=x=>{const hh=new Date(x).getHours();return hh<5?'late at night':hh<12?'in the morning':hh<17?'in the afternoon':hh<21?'in the evening':'late at night'};const pc={};sl.forEach(s=>{const p=part(s.t);pc[p]=(pc[p]||0)+1});const tp=Object.entries(pc).sort((a,b)=>b[1]-a[1])[0];
 return`<p class="small" style="margin-top:12px">${top?`Most slips come from <b>${esc(top[0].toLowerCase())}</b>`:'Slips'}${tp&&tp[1]>1?` and happen most ${tp[0]}`:''}. ${h.plan?'Keep your plan ready for exactly those moments.':'Write a plan for those moments in Edit.'}</p>`}
function vHabit(id){const h=H(id);if(!h)return`<header class="ph"><div><h1>Not found</h1></div></header><a class="btn" href="#/habits">Back to habits</a>`;
 const td=ymd(),g=h.goalId&&G(h.goalId);let o=`<div class="crumb"><a href="#/habits" class="ibtn" aria-label="Back to habits">${ic('back')}</a><div class="ph-r"><button class="ibtn" data-act="editHabit" data-id="${h.id}" aria-label="Edit habit">${ic('edit')}</button><button class="ibtn" data-act="habitMenu" data-id="${h.id}" aria-label="More">${ic('more')}</button></div></div>`;
 const kindTxt=h.kind==='quit'?(h.mode==='limit'?'Cutting down':'Breaking'):h.kind==='routine'?'Routine':'Building';
 o+=`<section class="hero tint hhero" style="${cvar(h)}"><div class="data">${kindTxt} · ${areaName(h.area)}${h.kind!=='quit'?' · '+freqText(h):''}${h.status!=='active'?' · '+cap(h.status):''}</div><h1><span class="hh-ic">${esc(hIcon(h))}</span>${esc(h.title)}</h1>${h.why?`<p class="why">${esc(h.why)}</p>`:''}${h.cue&&h.kind==='build'?`<p class="hcue">${ic('link','ico-s')} ${esc(h.cue)}, I will ${esc(h.title.toLowerCase())}${h.target>1?` (${h.target} ${esc(h.unit)})`:''}.</p>`:''}`;
 if(h.kind==='quit'&&h.mode!=='limit'){const since=qSince(h);o+=`<div class="hq-clock big" data-since="${since}">${clockHTML(Date.now()-since)}</div><div class="hq-a" style="margin-top:18px"><button class="btn ink" data-act="urge" data-id="${h.id}">${ic('wave')}I have an urge</button><button class="btn inkline" data-act="slip" data-id="${h.id}">Log a slip</button></div></section>`;
  const sv=qSaved(h),ub=h.urges.filter(u=>u.ok).length,d=qDays(h),nx=MILES.find(m=>m>d);
  o+=hTiles([[`${qBest(h)}<small>d</small>`,'Best run'],[h.cost?money(h,sv.money):ub,h.cost?'Money saved':'Urges beaten'],[h.mins?hrs(sv.mins):h.slips.filter(s=>s.t>=h.start).length,h.mins?'Time back':'Slips'],[nx?mileName(nx).replace(' ','<small> ')+'</small>':'—','Next milestone']]);
  o+=`<section class="rv"><div class="st-h"><h3>Milestones</h3><span class="data">current run</span></div><div class="hmiles">${MILES.map(m=>`<div class="hm${d>=m?' ok':''}${m===nx?' nx':''}"><span>${d>=m?ic('check'):m===nx?ic('flag'):''}</span><b>${mileName(m)}</b></div>`).join('')}</div></section>`}
 else if(h.kind==='quit'){const v=+h.log[td]||0;o+=`<div class="hq-lim big"><b>${v}</b><span>of ${h.limit}${h.unit?' '+esc(h.unit):''} today</span></div><div class="hq-a" style="margin-top:18px"><button class="btn ink" data-act="hLimit" data-id="${h.id}" data-n="1">${ic('plus')}Log one</button>${v?`<button class="btn inkline" data-act="hLimit" data-id="${h.id}" data-n="-1">Undo</button>`:''}<button class="btn inkline" data-act="urge" data-id="${h.id}">${ic('wave')}I have an urge</button></div></section>`;
  o+=hTiles([[lStreak(h)+'<small>d</small>','On track'],[lBest(h)+'<small>d</small>','Best run'],[h.urges.filter(u=>u.ok).length,'Urges beaten'],[(()=>{const tn=dnum(ymd());let t=0,n=0;for(let k=tn-7;k<tn;k++){const ds=fromN(k);if(ds<h.startDate)continue;t+=+h.log[ds]||0;n++}return n?(t/n).toFixed(1):'—'})(),'Daily avg · 7d']])}
 else{const v=hVal(h,td),n=hTarget(h),s=hStreak(h),u=h.freq==='times'?'wk':'d';
  o+=`<div class="hero-f"><div><div class="data">Strength ${hStrength(h)}%</div><div class="pbar"><i style="width:${hStrength(h)}%"></i></div></div><div class="hero-pct">${s}<small>${u}</small></div></div>`;
  if(h.status==='active'){if(h.kind==='routine'&&h.steps.length)o+=`<div class="hq-a" style="margin-top:20px"><button class="btn ink" data-act="routineGo" data-id="${h.id}">${ic('play')}${v>=n?'Run again':v?`Continue · ${v} of ${n}`:'Start routine'}</button></div>`;
   else if(n>1)o+=`<div class="hstep ink" style="margin-top:20px"><button class="ibtn" data-act="hAdj" data-id="${h.id}" data-d="${td}" data-n="-1" aria-label="One less">${ic('minus')}</button><div><b>${v}</b><span>of ${n} ${esc(h.unit)} today</span></div><button class="ibtn" data-act="hAdj" data-id="${h.id}" data-d="${td}" data-n="1" aria-label="One more">${ic('plus')}</button></div>`;
   else o+=`<div class="hq-a" style="margin-top:20px"><button class="btn ${v>=n?'inkline':'ink'}" data-act="hTap" data-id="${h.id}">${ic('check')}${v>=n?'Done today · undo':'Mark done for today'}</button>${hDue(h,td)&&v<n?`<button class="btn inkline" data-act="hSkip" data-id="${h.id}" data-d="${td}">${h.skip[td]?'Undo rest day':'Rest day'}</button>`:''}</div>`}
  o+=`</section>`;const r=hRate(h),age=dnum(td)-dnum(h.startDate)+1;
  o+=hTiles([[`${s}<small>${u}</small>`,'Current streak'],[`${hBest(h)}<small>${u}</small>`,'Best streak'],[`${hStrength(h)}<small>%</small>`,'Strength'],[r==null?'—':`${r}<small>%</small>`,h.freq==='times'?'Last 4 weeks':'Last 30 days']]);
  if(h.mini)o+=`<div class="hnudge rv" style="margin-top:14px">${ic('shield')}<div><b>On a hard day</b><div class="small muted">${esc(h.mini)}. It still counts.</div></div></div>`;
  if(h.kind==='routine')o+=`<section class="rv"><div class="st-h"><h3>Steps</h3><button class="link" data-act="editHabit" data-id="${h.id}">Edit</button></div><div class="list">${h.steps.map((st,i)=>`<label class="li"><input type="checkbox" class="rchk" data-hstep="${st.id}" data-id="${h.id}" data-d="${td}" ${(h.rs[td]||[]).includes(st.id)?'checked':''}><span class="nbadge sm" style="position:static">${pad2(i+1)}</span><div style="flex:1;min-width:0"><div class="t">${esc(st.title)}</div></div><span class="data">${st.min?st.min+' min':''}</span></label>`).join('')||'<p class="small muted" style="padding:16px">No steps yet. Add them in Edit.</p>'}</div></section>`;
  o+=`<section class="rv"><div class="st-h"><h3>Becoming automatic</h3><span class="data">day ${age}</span></div><div class="panel"><div class="pbar" style="height:6px;${cvar(h)}"><i style="width:${Math.min(100,age/66*100)}%"></i></div><p class="small muted" style="margin-top:10px">${age<66?`New habits take about two months to feel automatic, on average, and the range is wide. Day ${age} of roughly 66. Keep the bar low and show up.`:'Past the two-month mark. For most people it feels automatic by now. Protect it on busy days.'}</p></div></section>`}
 o+=`<section class="rv"><div class="st-h"><h3>History</h3>${h.kind!=='quit'?'<span class="data">tap a day to change it</span>':''}</div><div class="panel">${hHeat(h)}</div></section>`;
 if(h.kind!=='quit')o+=hWeekdays(h);
 if(h.kind==='quit'){const sl=h.slips.filter(s=>s.t>=h.start).sort((a,b)=>b.t-a.t);
  if(h.plan)o+=`<div class="hnudge rv" style="margin-top:26px">${ic('shield')}<div><b>When the urge hits</b><div class="small muted">${esc(h.plan)}</div></div></div>`;
  if(h.mode!=='limit')o+=`<section class="rv"><div class="st-h"><h3>Slips</h3><span class="data">${sl.length}</span></div>${sl.length?`<div class="list">${sl.slice(0,12).map(s=>`<div class="li" style="cursor:default"><span class="agi">${ic('reset')}</span><div style="flex:1;min-width:0"><div class="t">${new Date(s.t).toLocaleDateString(undefined,{weekday:'short',month:'short',day:'numeric'})} · ${new Date(s.t).toLocaleTimeString([],{hour:'numeric',minute:'2-digit'})}</div><div class="s">${esc([s.trig,s.note].filter(Boolean).join(' · ')||'No note')}</div></div></div>`).join('')}</div>${slipInsight(h)}`:`<p class="small muted">None in this run. If one happens, log it honestly. Patterns in your slips show you where to plan ahead.</p>`}</section>`}
 if(g)o+=`<section class="rv"><div class="st-h"><h3>Supports</h3></div><a class="gchip" style="${cvar(g)}" href="#/goal/${g.id}"><i></i>${esc(g.title)}<span>${pct(g)}%</span></a></section>`;
 return o}
function goalHabits(g){const L=S.habits.filter(h=>h.goalId===g.id&&h.status!=='archived');if(!L.length)return'';return`<section class="rv"><div class="st-h"><h3>Habits for this goal</h3><a class="link" href="#/habits">All habits</a></div>${L.filter(h=>h.kind!=='quit').length?`<div class="list">${L.filter(h=>h.kind!=='quit').map(hrow).join('')}</div>`:''}${L.filter(h=>h.kind==='quit').length?`<div class="hqgrid" style="margin-top:12px">${L.filter(h=>h.kind==='quit').map(qcard).join('')}</div>`:''}</section>`}
/* Today page strip */
function habitsStrip(){const hs=todayHabits(),qs=S.habits.filter(h=>h.status==='active'&&h.kind==='quit');if(!hs.length&&!qs.length)return'';const td=ymd(),t=dayRatio(td),r=8,C=2*Math.PI*r;
 return`<section class="rv"><div class="st-h"><div class="tg-h"><h3>Habits</h3>${t.due?`<svg class="tg-ring" viewBox="0 0 22 22"><circle class="a" cx="11" cy="11" r="${r}"/><circle class="b" cx="11" cy="11" r="${r}" stroke-dasharray="${C.toFixed(2)}" stroke-dashoffset="${(C*(1-t.dn/t.due)).toFixed(2)}"/></svg><span class="data">${t.dn} of ${t.due}</span>`:''}</div><a class="link" href="#/habits">All habits</a></div><div class="hstrip">
 ${hs.map(h=>`<div class="hpill${hDone(h,td)?' on':''}${h.skip[td]?' off':''}" style="${cvar(h)}" data-act="openHabit" data-id="${h.id}"><div class="hp-top"><span class="hic sm">${esc(hIcon(h))}</span>${h.skip[td]?'':hChk(h)}</div><div class="t ell">${esc(h.title)}</div><div class="data">${h.skip[td]?'Rest day':hStreak(h)?`${ic('flame','ico-s')} ${hStreak(h)}${h.freq==='times'?' wk':' d'}`:h.kind==='routine'?`${h.steps.length} steps`:h.target>1?`${hVal(h,td)}/${h.target} ${esc(h.unit)}`:'Start today'}</div></div>`).join('')}
 ${qs.map(h=>`<div class="hpill q" style="${cvar(h)}" data-act="openHabit" data-id="${h.id}"><div class="hp-top"><span class="hic sm">${esc(hIcon(h))}</span></div><div class="t ell">${esc(h.title)}</div><div class="data hp-clock"${h.mode==='limit'?'':` data-since="${qSince(h)}" data-short="1"`}>${h.mode==='limit'?`${+h.log[td]||0} of ${h.limit} today`:clockHTML(Date.now()-qSince(h),1)}</div></div>`).join('')}</div></section>`}
/* ---- templates ---- */
const HTPL=[
 {kind:'build',title:'Drink water',icon:'💧',target:8,unit:'glasses',area:'health',cue:'After each meal and each bathroom break',mini:'One glass'},
 {kind:'build',title:'Read',icon:'📖',target:20,unit:'pages',area:'learning',part:'evening',cue:'After I get into bed',mini:'Read one page'},
 {kind:'build',title:'Walk',icon:'🚶',target:30,unit:'minutes',area:'health',part:'afternoon',cue:'After lunch',mini:'Walk to the end of the street and back'},
 {kind:'build',title:'Exercise',icon:'💪',freq:'times',times:3,area:'health',part:'morning',mini:'Put on workout clothes and do five minutes'},
 {kind:'build',title:'Pray on time',icon:'🙏',target:5,unit:'prayers',area:'faith'},
 {kind:'build',title:'Journal',icon:'✍️',area:'personal',part:'evening',cue:'After dinner',mini:'Write one sentence'},
 {kind:'build',title:'Lights out by 11',icon:'🌙',area:'health',part:'evening',time:'22:30',remind:true,mini:'Phone on the charger outside the bedroom'},
 {kind:'build',title:'Learn a skill',icon:'🧠',target:25,unit:'minutes',area:'career',freq:'days',days:[1,2,3,4,5],mini:'One lesson'},
 {kind:'build',title:'Phone-free family time',icon:'👨‍👩‍👧',area:'family',part:'evening',target:30,unit:'minutes',mini:'Ten minutes, fully present'},
 {kind:'build',title:'Weekly money check',icon:'💸',area:'finance',freq:'times',times:1,mini:'Look at your balance once'},
 {kind:'quit',mode:'quit',title:'Quit smoking',icon:'🚭',area:'health',cost:10,mins:45,plan:'Drink a glass of cold water, then walk for five minutes'},
 {kind:'quit',mode:'quit',title:'No added sugar',icon:'🍬',area:'health',cost:3,plan:'Have fruit or a cup of tea instead'},
 {kind:'quit',mode:'quit',title:'No phone in bed',icon:'📵',area:'personal',plan:'Charge the phone outside the bedroom and keep a book by the bed'},
 {kind:'quit',mode:'quit',title:'No junk food',icon:'🍔',area:'health',cost:8},
 {kind:'quit',mode:'limit',title:'Less social media',icon:'📱',area:'personal',limit:2,unit:'sessions',mins:60,plan:'Open a book or message a friend instead'},
 {kind:'quit',mode:'limit',title:'Cut down on coffee',icon:'☕',area:'health',limit:2,unit:'cups'},
 {kind:'routine',title:'Morning routine',icon:'☀️',area:'personal',part:'morning',steps:'Drink a glass of water 1\nMake the bed 2\nStretch 5\nPlan the top three for today 5'},
 {kind:'routine',title:'Evening wind-down',icon:'🌙',area:'health',part:'evening',steps:'Tidy one room 10\nSet out tomorrow’s clothes 5\nReflect on the day 5\nRead 15\nLights out'},
 {kind:'routine',title:'Workout',icon:'💪',area:'health',part:'morning',freq:'times',times:3,steps:'Warm up 5\nStrength circuit 20\nCardio 10\nStretch 5'},
 {kind:'routine',title:'Deep work start',icon:'🎯',area:'career',part:'morning',freq:'days',days:[1,2,3,4,5],steps:'Clear the desk 2\nPhone on silent 1\nPick one task 2\nFocus block 50\nShort walk 5'},
];
function tplGrid(only){const G3=[['build','Build a habit'],['quit','Break a habit'],['routine','Routines']].filter(x=>!only||x[0]===only);
 return G3.map(([k,n])=>`<section class="rv"><div class="st-h"><h3>${n}</h3><span class="data">templates</span></div><div class="tplg">${HTPL.map((t,i)=>t.kind===k?`<button class="tpl" style="--c:var(--c-${t.area})" data-act="hTpl" data-i="${i}"><span class="hic sm">${t.icon}</span><span><b>${esc(t.title)}</b><small>${t.kind==='quit'?(t.mode==='limit'?`Max ${t.limit} ${t.unit} a day`:'Live clean-time clock'):t.kind==='routine'?t.steps.split('\n').length+' steps':t.freq==='times'?t.times+'× a week':t.target>1?t.target+' '+t.unit+' a day':'Every day'}</small></span></button>`:'').join('')}</div></section>`).join('')}
/* ---- form ---- */
const localDT=ms=>{const d=new Date(ms);return`${ymd(d)}T${pad2(d.getHours())}:${pad2(d.getMinutes())}`};
function habitForm(h,kind,tpl){const e=!!h;h=h||normHabit({kind:kind||'build',...(tpl||{}),steps:[],id:'x'});if(tpl&&tpl.steps)h.steps=parseSteps(tpl.steps,[]);
 const K=h.kind,icons=HICONS.includes(hIcon(h))||!h.icon?HICONS:[h.icon,...HICONS];const gs=active();
 return`<h2>${e?'Edit habit':tpl?'New habit from template':'New habit'}</h2><form data-form="saveHabit" ${e?`data-id="${h.id}"`:''}>
 <div class="field"><label>Type</label><div class="radios">${HKIND.map(([k,n])=>`<label><input type="radio" name="kind" value="${k}" ${K===k?'checked':''} ${e?'disabled':''}><span>${n}</span></label>`).join('')}</div>${e?`<input type="hidden" name="kind" value="${K}">`:''}</div>
 <div class="field"><label>Name</label><input name="title" required maxlength="60" value="${esc(e||tpl?h.title:'')}" placeholder="Read 20 pages"></div>
 <div class="field"><label>Icon</label><div class="hicons">${icons.map(x=>`<label><input type="radio" name="icon" value="${x}" ${hIcon(h)===x?'checked':''}><span>${x}</span></label>`).join('')}</div></div>
 <div data-k="quit"><div class="field"><label>Approach</label><div class="radios">${[['quit','Stop completely'],['limit','Cut down']].map(([k,n])=>`<label><input type="radio" name="mode" value="${k}" ${h.mode===k?'checked':''}><span>${n}</span></label>`).join('')}</div></div>
  <div data-md="quit"><div class="field"><label>${e?'Current run started':'When did you stop?'}</label><input type="datetime-local" name="start" value="${localDT(e?h.start:Date.now())}" max="${localDT(Date.now()+6e4)}"><small class="hint">Already stopped a while ago? Set the real date and the clock starts there.</small></div></div>
  <div data-md="limit"><div class="two"><div class="field"><label>Daily limit</label><input type="number" name="limit" min="0" max="99" value="${h.limit}"></div><div class="field"><label>Unit · optional</label><input name="lunit" maxlength="20" value="${esc(h.unit)}" placeholder="cups, sessions"></div></div></div>
  <div class="field"><label>When the urge hits, I will… · optional</label><input name="plan" maxlength="140" value="${esc(h.plan)}" placeholder="Drink water and walk for five minutes"></div>
  <div class="two"><div class="field"><label>Money per day it costs · optional</label><div class="hcur"><input name="cur" maxlength="3" value="${esc(h.cur||'$')}" aria-label="Currency"><input type="number" name="cost" min="0" step="0.01" value="${h.cost||''}" placeholder="0"></div></div><div class="field"><label>Minutes per day it takes · optional</label><input type="number" name="mins" min="0" max="1440" value="${h.mins||''}" placeholder="0"></div></div></div>
 <div data-k="build"><div class="field"><label>Tie it to something you already do · optional</label><input name="cue" maxlength="80" value="${esc(h.cue)}" placeholder="After I pour my morning coffee"></div>
  <div class="two"><div class="field"><label>Amount per day</label><input type="number" name="target" min="1" max="999" value="${h.target}"></div><div class="field"><label>Unit · optional</label><input name="unit" maxlength="20" value="${esc(h.unit)}" placeholder="glasses, pages, minutes"></div></div>
  <div class="field"><label>Small version for hard days · optional</label><input name="mini" maxlength="80" value="${esc(h.mini)}" placeholder="Just put on running shoes"></div></div>
 <div data-k="routine"><div class="field"><label>Steps · one per line, minutes at the end</label><textarea name="steps" placeholder="Drink a glass of water 1&#10;Stretch 5&#10;Plan the day 5">${esc(h.steps.map(s=>s.title+(s.min?' '+s.min:'')).join('\n'))}</textarea></div></div>
 <div data-k="build routine"><div class="field"><label>How often</label><div class="radios sm">${[['daily','Every day'],['days','Some days'],['times','X a week']].map(([k,n])=>`<label><input type="radio" name="freq" value="${k}" ${h.freq===k?'checked':''}><span>${n}</span></label>`).join('')}</div></div>
  <div class="field" data-fq="days"><label>On</label><div class="radios sm">${DAYS.map((d,i)=>`<label><input type="checkbox" name="days" value="${i}" ${h.freq!=='days'||h.days.includes(i)?'checked':''}><span>${d.slice(0,2)}</span></label>`).join('')}</div></div>
  <div class="field" data-fq="times"><label>Times a week</label><div class="radios sm">${[1,2,3,4,5,6].map(n=>`<label><input type="radio" name="times" value="${n}" ${h.times===n?'checked':''}><span>${n}</span></label>`).join('')}</div></div>
  <div class="field"><label>Time of day</label><div class="radios sm">${HPARTS.map(([k,n])=>`<label><input type="radio" name="part" value="${k}" ${(h.part||'any')===k?'checked':''}><span>${n}</span></label>`).join('')}</div></div>
  <div class="two"><div class="field"><label>Reminder time · optional</label><input type="time" name="time" value="${h.time||''}"></div><div class="field"><label>&nbsp;</label><label class="sw" style="border:0;padding:6px 0"><span>Remind me</span><input type="checkbox" name="remind" ${h.remind?'checked':''}><i></i></label></div></div></div>
 <div class="field"><label>Why it matters · optional</label><textarea name="why" style="min-height:72px" placeholder="Read this on the days you don’t feel like it">${esc(h.why)}</textarea></div>
 <div class="two"><div class="field"><label>Life area</label><select name="area">${AREAS.map(a=>`<option value="${a.id}" ${a.id===h.area?'selected':''}>${a.name}</option>`).join('')}</select></div><div class="field"><label>Supports a goal · optional</label><select name="goal"><option value="">None</option>${treeOrder(gs).map(({g,d})=>`<option value="${g.id}" ${h.goalId===g.id?'selected':''}>${' '.repeat(d)}${esc(trunc(g.title,44))}</option>`).join('')}</select></div></div>
 <div class="actions"><button type="button" class="btn ghost" data-act="close">Cancel</button><button class="btn pri">${e?'Save':'Create'}</button></div></form>`}
function hFormSync(f){const k=(f.querySelector('[name=kind]:checked')||f.querySelector('[name=kind][type=hidden]')||{}).value||'build',fq=(f.querySelector('[name=freq]:checked')||{}).value||'daily',md=(f.querySelector('[name=mode]:checked')||{}).value||'quit';
 f.querySelectorAll('[data-k]').forEach(x=>x.hidden=!x.dataset.k.split(' ').includes(k));f.querySelectorAll('[data-fq]').forEach(x=>x.hidden=x.dataset.fq!==fq);f.querySelectorAll('[data-md]').forEach(x=>x.hidden=x.dataset.md!==md);
 const t=f.elements.title;if(t)t.placeholder=k==='quit'?(md==='limit'?'Less social media':'Quit smoking'):k==='routine'?'Morning routine':'Read 20 pages'}
function parseSteps(txt,old){return String(txt||'').split('\n').map(l=>l.trim()).filter(Boolean).slice(0,30).map(l=>{const m=/^(.*?)[\s·:,\-–—]+(\d{1,3})\s*(m|min|mins|minutes)?\.?$/i.exec(l);const title=(m?m[1]:l).trim()||l,min=m?Math.min(240,+m[2]):0;const o=(old||[]).find(s=>s.title===title);return{id:o?o.id:uid(),title,min}})}
function openHabitForm(h,kind,tpl){openSheet(habitForm(h,kind,tpl));const f=$('#sheet form');if(f)hFormSync(f)}
/* ---- routine player ---- */
let RP=null,URGE=null;
function rpDraw(){const h=H(RP.id);if(!h)return rpEnd();const s=h.steps[RP.i],td=ymd(),done=new Set(h.rs[td]||[]),N=h.steps.length,left=RP.paused?RP.left:Math.max(0,RP.end-Date.now()),tot=(s.min||0)*6e4;const r=88,C=2*Math.PI*r;
 const html=`<div class="grab"></div><div class="data">${esc(hIcon(h))} ${esc(h.title)} · step ${RP.i+1} of ${N}</div><div class="rp" style="${cvar(h)}"><div class="rp-ring"><svg viewBox="0 0 200 200"><circle cx="100" cy="100" r="${r}" class="a"/><circle id="rpArc" cx="100" cy="100" r="${r}" class="b" stroke-dasharray="${C.toFixed(1)}" stroke-dashoffset="${tot?(C*(1-left/tot)).toFixed(1):0}" data-c="${C.toFixed(1)}"/></svg><div class="rp-t"><b id="rpT">${tot?`${Math.floor(left/6e4)}:${pad2(Math.floor(left%6e4/1e3))}`:ic('check')}</b><span class="data">${tot?(RP.paused?'Paused':'Remaining'):'No timer'}</span></div></div><h2>${esc(s.title)}</h2>
 <div class="rp-steps">${h.steps.map((x,i)=>`<span class="${done.has(x.id)?'d':''}${i===RP.i?' c':''}" title="${esc(x.title)}"></span>`).join('')}</div>${RP.i+1<N?`<div class="data" style="text-align:center">Next: ${esc(h.steps[RP.i+1].title)}</div>`:''}</div>
 <div class="actions rp-a"><button class="btn ghost" data-act="rpClose">Close</button>${tot?`<button class="btn" data-act="rpPause">${ic(RP.paused?'play':'pause')}${RP.paused?'Resume':'Pause'}</button>`:''}<button class="btn" data-act="rpSkip">${ic('skip')}Skip</button><button class="btn pri" data-act="rpDone">${ic('check')}${RP.i+1<N||h.steps.some((x,i)=>i!==RP.i&&!done.has(x.id))?'Done':'Finish'}</button></div>`;
 const sh=$('#sheet');if(sh.classList.contains('on')){sh.innerHTML=html}else openSheet(html.replace('<div class="grab"></div>',''),true);SHEET_LOCK=true}
function rpStart(i){const h=H(RP.id);RP.i=i;const s=h.steps[i];RP.left=(s.min||0)*6e4;RP.end=Date.now()+RP.left;RP.paused=false;RP.beeped=false;rpDraw()}
function rpNext(markDone){const h=H(RP.id),td=ymd();if(markDone){const L=new Set(h.rs[td]||[]);L.add(h.steps[RP.i].id);h.rs[td]=h.steps.filter(s=>L.has(s.id)).map(s=>s.id);delete h.skip[td];save()}
 const done=new Set(h.rs[td]||[]);
 if(h.steps.every(s=>done.has(s.id))){rpEnd();burst();const st=hStreak(h);if(!checkMilestones())toast(`${esc(h.title)} complete${st>1?` · ${st}${h.freq==='times'?'-week':'-day'} streak`:''}`);save();return}
 let j=h.steps.findIndex((s,k)=>k>RP.i&&!done.has(s.id));if(j<0)j=h.steps.findIndex(s=>!done.has(s.id));
 if(j===RP.i&&!markDone){const n=done.size;rpEnd();if(n)toast(`Saved · ${n} of ${h.steps.length} steps done`);return}rpStart(j)}
function rpEnd(){RP=null;SHEET_LOCK=false;closeSheet();render(false)}
function rpTick(now){if(!RP||RP.paused)return;const t=$('#rpT'),a=$('#rpArc');const h=H(RP.id);if(!h||!t)return;const tot=(h.steps[RP.i].min||0)*6e4;if(!tot)return;const left=Math.max(0,RP.end-now);
 t.textContent=`${Math.floor(left/6e4)}:${pad2(Math.floor(left%6e4/1e3))}`;if(a)a.setAttribute('stroke-dashoffset',(+a.dataset.c*(1-left/tot)).toFixed(1));
 if(left<=0&&!RP.beeped){RP.beeped=true;beep();vib();setTimeout(()=>{if(RP&&RP.beeped)rpNext(true)},900)}}
/* ---- live bits: clean-time clocks, routine timer, urge timer, day change ---- */
let LASTD=ymd();
function paintLive(){const now=Date.now();document.querySelectorAll('[data-since]').forEach(el=>{el.innerHTML=clockHTML(now-+el.dataset.since,!!el.dataset.short)});
 if(RP)rpTick(now);
 if(URGE){const t=$('#urT');if(!t)URGE=null;else{const left=Math.max(0,URGE.end-now);t.textContent=left?`${Math.floor(left/6e4)}:${pad2(Math.floor(left%6e4/1e3))}`:'You rode it out';const ph=(now-URGE.t0)%10000,b=$('#brTxt');if(b)b.textContent=ph<4000?'Breathe in':ph<5500?'Hold':'Breathe out'}}
 if(LASTD!==ymd()){LASTD=ymd();if(!$('#sheet').classList.contains('on'))render(false);pushWidget();syncNative()}}
setInterval(paintLive,1000);
function hCelebrate(h){const td=ymd(),t=dayRatio(td);if(t.due>1&&t.dn>=t.due){burst();toast('Every habit for today is done');return}const s=hStreak(h);if(s>1)toast(`${esc(hIcon(h))} ${esc(h.title)} · ${s}${h.freq==='times'?'-week':'-day'} streak`)}
/* widget + notification payload */
function widgetHabits(){const td=ymd(),tn=dnum(td),L=d=>{const o={};for(let k=tn-7;k<=tn;k++){const ds=fromN(k),v=hVal(d,ds);if(v)o[ds]=v;if(d.skip[ds])o[ds]=-1}return o};
 const hs=S.habits.filter(h=>h.status==='active'&&h.kind!=='quit').sort((a,b)=>HPARTS.findIndex(p=>p[0]===a.part)-HPARTS.findIndex(p=>p[0]===b.part)||(a.time||'99').localeCompare(b.time||'99')).slice(0,12).map(h=>{const s=hStreak(h);return{id:h.id,t:h.title,e:hIcon(h),c:ACOL[h.area]||'#FFB547',n:hTarget(h),u:h.unit||'',k:h.kind==='routine'&&h.steps.length?'routine':'build',m:[0,1,2,3,4,5,6].map(i=>h.freq!=='days'||h.days.includes(i)?'1':'0').join(''),sd:h.startDate,tw:h.freq==='times'?h.times:0,sb:h.freq==='times'?s:Math.max(0,s-(hDone(h,td)?1:0)),l:L(h)}});
 const qs=S.habits.filter(h=>h.status==='active'&&h.kind==='quit').slice(0,6).map(h=>{const sv=qSaved(h),d=qDays(h),nx=MILES.find(m=>m>d);return{id:h.id,t:h.title,e:hIcon(h),c:ACOL[h.area]||'#FFB547',mode:h.mode,since:qSince(h),best:qBest(h),saved:h.mode==='limit'?'':sv.money>=.5?money(h,sv.money)+' saved':sv.mins>=30?hrs(sv.mins)+' back':'',next:nx?mileName(nx):'',lim:h.limit,u:h.unit||'',l:L(h),ls:lStreak(h)}});
 return{hd:td,habits:hs,quits:qs}}
function habitAlarms(now,L){S.habits.forEach(h=>{if(h.status!=='active')return;
  if(h.kind!=='quit'&&h.remind&&h.time){const[hh,mm]=h.time.split(':').map(Number);for(let k=0;k<4;k++){const d=new Date();d.setDate(d.getDate()+k);d.setHours(hh,mm,0,0);const ds=ymd(d);if(d.getTime()<=now||!hCounts(h,ds)||hDone(h,ds))continue;const s=hStreak(h);
   L.push({at:d.getTime(),title:`${hIcon(h)} ${h.title}`,body:h.cue?h.cue:s>1?`Keep your ${s}${h.freq==='times'?'-week':'-day'} streak going`:h.mini?'Small version: '+h.mini:'Time for this habit',hid:h.kind==='build'?h.id:'',hd:ds,hv:hTarget(h)})}}
  if(h.kind==='quit'&&h.mode!=='limit'){const since=qSince(h),d=qDays(h),nx=MILES.find(m=>m>d);if(nx)L.push({at:since+nx*864e5,title:`🎉 ${mileName(nx)} free · ${h.title}`,body:'A new milestone. Open Plotline to see how far you’ve come.'})}})}
function habitTick(now,td){let ch=false;const d=new Date(),cm=d.getHours()*60+d.getMinutes(),R=S.settings.hRem||(S.settings.hRem={});
 S.habits.forEach(h=>{if(h.status!=='active'||h.kind==='quit'||!h.remind||!h.time)return;const[hh,mm]=h.time.split(':').map(Number);if(cm<hh*60+mm||cm>hh*60+mm+180||R[h.id]===td||!hCounts(h,td)||hDone(h,td))return;R[h.id]=td;ch=true;notify(`${hIcon(h)} ${h.title}`,h.cue||'Time for this habit')});
 if(checkMilestones())ch=true;return ch}

/* ================= habit actions ================= */
Object.assign(ACT,{
 hView:d=>{HV=d.v;render(false)},
 openHabit:d=>go('habit/'+d.id),
 newHabit:d=>openSheet(`<div class="data">New</div><h2 style="margin-top:8px">What do you want to work on?</h2><div class="hnew">${[['build','🌱','Build a habit','Something to do daily or a few times a week'],['quit','🛡️','Break a habit','Stop completely, or cut down to a daily limit'],['routine','🔁','Create a routine','A short sequence with a guided timer']].map(([k,e,t,s])=>`<button class="tpl big" data-act="hBlank" data-k="${k}"><span class="hic">${e}</span><span><b>${t}</b><small>${s}</small></span></button>`).join('')}</div>${tplGrid(d&&d.k)}`),
 hBlank:d=>openHabitForm(null,d.k),
 hTpl:d=>{const t=HTPL[+d.i];if(t)openHabitForm(null,t.kind,t)},
 editHabit:d=>{const h=H(d.id);if(h)openHabitForm(h)},
 hTap:d=>{const h=H(d.id);if(!h)return;const td=ymd(),v=hVal(h,td),n=hTarget(h);
  if(v>=n){const u=undoPoint();hSetVal(h,td,0);JUST='';save();render(false);toast('Unchecked for today',u);return}
  hSetVal(h,td,v+1);JUST=h.id;vib();if(v+1>=n){checkMilestones()||hCelebrate(h)}save();render(false)},
 hDay:d=>{const h=H(d.id);if(!h||d.d>ymd())return;const v=hVal(h,d.d),n=hTarget(h);hSetVal(h,d.d,v>=n?0:n);if(v<n){vib();JUST=d.d===ymd()?h.id:'';checkMilestones()}save();render(false)},
 hAdj:d=>{const h=H(d.id);if(!h)return;const v=hVal(h,d.d);hSetVal(h,d.d,v+(+d.n));if(v+(+d.n)>=hTarget(h)&&v<hTarget(h)){vib();if(d.d===ymd()&&!checkMilestones())hCelebrate(h)}save();render(false);const b=$('#hsv');if(b)b.textContent=hVal(h,d.d)},
 hSkip:d=>{const h=H(d.id);if(!h)return;if(h.skip[d.d])delete h.skip[d.d];else{h.skip[d.d]=1;hSetVal(h,d.d,0);h.skip[d.d]=1}closeSheet();save();render(false);toast(h.skip[d.d]?'Rest day. Your streak is safe.':'Rest day removed')},
 hSetDay:d=>{const h=H(d.id);if(!h)return;hSetVal(h,d.d,+d.v?hTarget(h):0);closeSheet();checkMilestones();save();render(false)},
 hDaySheet:d=>{const h=H(d.id),ds=d.d;if(!h||ds>ymd())return;const v=hVal(h,ds),n=hTarget(h),rt=h.kind==='routine'&&h.steps.length;
  openSheet(`<div class="data">${esc(dayName(ds))}</div><h2 style="margin-top:8px">${esc(hIcon(h))} ${esc(h.title)}</h2>
  ${rt?`<div class="clist" style="margin-top:14px">${h.steps.map(s=>`<label class="citem" style="${cvar(h)}"><input type="checkbox" data-hstep="${s.id}" data-id="${h.id}" data-d="${ds}" ${(h.rs[ds]||[]).includes(s.id)?'checked':''}><i></i><span>${esc(s.title)}</span><span class="data">${s.min?s.min+' min':''}</span></label>`).join('')}</div>`
   :n>1?`<div class="hstep" style="margin-top:14px"><button class="ibtn" data-act="hAdj" data-id="${h.id}" data-d="${ds}" data-n="-1" aria-label="One less">${ic('minus')}</button><div><b id="hsv">${v}</b><span>of ${n} ${esc(h.unit)}</span></div><button class="ibtn" data-act="hAdj" data-id="${h.id}" data-d="${ds}" data-n="1" aria-label="One more">${ic('plus')}</button></div>`:`<p class="muted small">${v?'Marked done.':h.skip[ds]?'Rest day.':'Not done.'}</p>`}
  <div class="actions"><button class="btn ghost" data-act="hSkip" data-id="${h.id}" data-d="${ds}">${h.skip[ds]?'Undo rest day':'Rest day'}</button>${rt||n>1?`<button class="btn pri" data-act="close">Done</button>`:`<button class="btn pri" data-act="hSetDay" data-id="${h.id}" data-d="${ds}" data-v="${v?0:1}">${v?'Mark not done':'Mark done'}</button>`}</div>`)},
 hLimit:d=>{const h=H(d.id);if(!h)return;const td=ymd(),v=Math.max(0,(+h.log[td]||0)+(+d.n));if(v)h.log[td]=v;else delete h.log[td];vib();save();render(false);if(+d.n>0)toast(v>h.limit?`Over today’s limit of ${h.limit}. Tomorrow resets.`:v===h.limit?'That’s today’s limit':`${h.limit-v} left today`)},
 habitMenu:d=>{const h=H(d.id);openSheet(`<div class="data">${HKN[h.kind]}</div><h2 style="margin-top:8px">${esc(hIcon(h))} ${esc(h.title)}</h2><div class="menu">
  ${h.kind==='routine'&&h.steps.length?`<button data-act="routineGo" data-id="${h.id}">${ic('play')}Start routine</button>`:''}
  ${h.kind==='quit'&&h.mode!=='limit'?`<button data-act="slip" data-id="${h.id}">${ic('reset')}Log a slip and restart the clock</button>`:''}
  ${h.status!=='archived'?`<button data-act="hPause" data-id="${h.id}">${ic(h.status==='paused'?'play':'pause')}${h.status==='paused'?'Resume':'Pause · keeps your streak'}</button>`:''}
  <button data-act="hArchive" data-id="${h.id}">${ic('archive')}${h.status==='archived'?'Restore from archive':'Archive'}</button>
  <button data-act="hDup" data-id="${h.id}">${ic('copy')}Duplicate</button>
  <button class="danger" data-act="hDel" data-id="${h.id}">${ic('trash')}Delete habit</button></div>`)},
 hPause:d=>{const h=H(d.id),td=ymd(),yd=addDays(-1);if(h.status==='paused'){const p=h.pz.find(x=>!x.b);if(p){if(p.a>yd)h.pz=h.pz.filter(x=>x!==p);else p.b=yd}h.status='active';toast('Resumed')}else{h.pz.push({a:td,b:''});h.status='paused';toast('Paused. Your streak waits for you.')}closeSheet();save();render(false)},
 hArchive:d=>{const h=H(d.id);if(h.status==='archived'){h.status='active';const p=h.pz.find(x=>!x.b);if(p)p.b=addDays(-1)}else{if(h.status==='active')h.pz.push({a:ymd(),b:''});h.status='archived'}closeSheet();save();if(cur.p==='habit')go('habits');else render(false);toast(h.status==='archived'?'Archived':'Restored')},
 hDup:d=>{const h=H(d.id);const n=normHabit({...JSON.parse(JSON.stringify(h)),id:uid(),title:h.title+' (copy)',log:{},rs:{},skip:{},pz:[],slips:[],urges:[],start:Date.now(),startDate:ymd(),createdAt:Date.now(),mile:0,smile:0,status:'active',u:0});n.steps=n.steps.map(s=>({...s,id:uid()}));S.habits.push(n);closeSheet();save();go('habit/'+n.id);toast('Duplicated with a fresh history')},
 hDel:d=>{const h=H(d.id);askConfirm('Delete this habit?',`“${esc(h.title)}” and its history will be removed. Archive it instead to keep the history.`,'Delete',()=>{const u=undoPoint();S.habits=S.habits.filter(x=>x.id!==h.id);save();go('habits');toast('Habit deleted',u)})},
 routineGo:d=>{const h=H(d.id);if(!h)return;if(!h.steps.length){ACT.hTap(d);return}const td=ymd(),done=new Set(h.rs[td]||[]);let i=h.steps.findIndex(s=>!done.has(s.id));if(i<0){delete h.rs[td];i=0}RP={id:h.id};rpStart(i)},
 rpDone:()=>{if(RP){vib();rpNext(true)}},rpSkip:()=>{if(RP)rpNext(false)},
 rpPause:()=>{if(!RP)return;if(RP.paused){RP.end=Date.now()+RP.left;RP.paused=false}else{RP.left=Math.max(0,RP.end-Date.now());RP.paused=true}rpDraw()},
 rpClose:()=>{const h=RP&&H(RP.id);const n=h?(h.rs[ymd()]||[]).length:0;rpEnd();if(h&&n&&n<h.steps.length)toast(`Saved · ${n} of ${h.steps.length} steps done`)},
 urge:d=>{const h=H(d.id);if(!h)return;URGE={id:h.id,t0:Date.now(),end:Date.now()+5*6e4};const ub=h.urges.filter(u=>u.ok).length;
  openSheet(`<div class="data">${esc(hIcon(h))} ${esc(h.title)}</div><h2 style="margin-top:8px">Ride it out</h2><p class="muted small">An urge rises, peaks and fades, usually within minutes. You don’t have to act on it. Breathe with the circle.</p>
  <div class="breath" style="${cvar(h)}"><i></i><span id="brTxt">Breathe in</span></div><div class="data" style="text-align:center;font-size:13px" id="urT">5:00</div>
  ${h.why?`<blockquote class="hwhy"><span class="data">Why you started</span>${esc(h.why)}</blockquote>`:''}${h.plan?`<blockquote class="hwhy"><span class="data">Your plan</span>${esc(h.plan)}</blockquote>`:''}
  <div class="data" style="text-align:center;margin-top:12px">${h.mode==='limit'?`${+h.log[ymd()]||0} of ${h.limit} today`:`${qDays(h)} day${qDays(h)===1?'':'s'} free`}${ub?` · ${ub} urge${ub>1?'s':''} beaten so far`:''}</div>
  <div class="actions">${h.mode==='limit'?`<button class="btn" data-act="urgeGave" data-id="${h.id}">I gave in</button>`:`<button class="btn" data-act="slip" data-id="${h.id}">I slipped</button>`}<button class="btn pri" data-act="urgeWin" data-id="${h.id}">${ic('check')}It passed</button></div>`)},
 urgeWin:d=>{const h=H(d.id);URGE=null;h.urges.push({t:Date.now(),ok:1});if(h.urges.length>400)h.urges=h.urges.slice(-400);closeSheet();save();burst();render(false);const n=h.urges.filter(u=>u.ok).length;toast(n===1?'First urge beaten. Remember how that felt.':`Urge beaten. That’s ${n} so far.`)},
 urgeGave:d=>{const h=H(d.id);URGE=null;h.urges.push({t:Date.now(),ok:0});closeSheet();ACT.hLimit({id:h.id,n:'1'})},
 slip:d=>{URGE=null;const h=H(d.id);if(!h)return;openSheet(`<div class="data">${esc(hIcon(h))} ${esc(h.title)}</div><h2 style="margin-top:8px">Log a slip</h2><p class="muted small">A slip is information, not failure. Note what set it off, then start again. Your best run stays on record.</p>
  <form data-form="saveSlip" data-id="${h.id}"><div class="field"><label>When</label><input type="datetime-local" name="t" value="${localDT(Date.now())}" max="${localDT(Date.now()+6e4)}"></div>
  <div class="field"><label>What set it off?</label><div class="radios sm wrap">${TRIGS.map(t=>`<label><input type="radio" name="trig" value="${t}"><span>${t}</span></label>`).join('')}</div></div>
  <div class="field"><label>Note · optional</label><textarea name="note" style="min-height:72px" maxlength="300" placeholder="Where were you, how did you feel?"></textarea></div>
  <div class="actions"><button type="button" class="btn ghost" data-act="close">Cancel</button><button class="btn pri">Restart the clock</button></div></form>`)},
});
Object.assign(FORM,{
 saveHabit:f=>{const fd=new FormData(f),kind=fd.get('kind')||'build',title=String(fd.get('title')||'').trim();if(!title)return toast('Give it a name');const ex=f.dataset.id&&H(f.dataset.id);
  const o={title,icon:fd.get('icon')||'',why:String(fd.get('why')||'').trim(),area:fd.get('area')||'personal',goalId:fd.get('goal')||''};
  if(kind!=='quit'){o.freq=fd.get('freq')||'daily';o.days=fd.getAll('days').map(Number);if(o.freq==='days'&&(!o.days.length||o.days.length===7)){o.freq='daily';o.days=[0,1,2,3,4,5,6]}o.times=+fd.get('times')||3;o.part=fd.get('part')||'any';o.time=fd.get('time')||'';o.remind=fd.get('remind')==='on'&&!!o.time}
  if(kind==='build'){o.cue=String(fd.get('cue')||'').trim();o.mini=String(fd.get('mini')||'').trim();o.target=Math.max(1,Math.min(999,+fd.get('target')||1));o.unit=String(fd.get('unit')||'').trim()}
  if(kind==='routine'){o.steps=parseSteps(fd.get('steps'),ex?ex.steps:[]);if(!o.steps.length)return toast('Add at least one step')}
  if(kind==='quit'){o.mode=fd.get('mode')==='limit'?'limit':'quit';o.limit=Math.max(0,+fd.get('limit')||0);o.unit=String(fd.get('lunit')||'').trim();o.plan=String(fd.get('plan')||'').trim();o.cost=Math.max(0,+fd.get('cost')||0);o.cur=String(fd.get('cur')||'$').trim()||'$';o.mins=Math.max(0,+fd.get('mins')||0);
   const st=fd.get('start');const t=st?new Date(st).getTime():NaN;if(o.mode==='quit'&&!isNaN(t)&&(!ex||st!==localDT(ex.start)))o.start=Math.min(Date.now(),t)}
  if(o.remind)askNotif();
  if(ex){if(ex.kind==='quit'&&o.start&&o.start!==ex.start){o.mile=0;if(ymd(new Date(o.start))<ex.startDate)o.startDate=ymd(new Date(o.start))}Object.assign(ex,o);checkMilestones(false);closeSheet();save();render(false);toast('Saved')}
  else{const h=normHabit({...o,kind,id:uid(),createdAt:Date.now(),startDate:o.start?ymd(new Date(o.start)):ymd()});checkMilestones(false);S.habits.push(h);h.smile=0;h.mile=h.kind==='quit'?floorMile(MILES,qDays(h)):0;HV='today';closeSheet();save();go('habit/'+h.id);toast(kind==='quit'?'Clock started. You’ve got this.':kind==='routine'?'Routine created':'Habit created')}},
 saveSlip:f=>{const h=H(f.dataset.id);if(!h)return;const fd=new FormData(f);let t=new Date(fd.get('t')).getTime();if(isNaN(t))t=Date.now();t=Math.min(Date.now(),t);const best=qBest(h);
  h.slips.push({t,trig:fd.get('trig')||'',note:String(fd.get('note')||'').trim()});h.mile=0;closeSheet();save();render(false);toast(`Clock restarted. Your best run is ${best} day${best===1?'':'s'}. You can beat it.`)},
});
FILE.aicover=async inp=>{const f=inp.files[0];if(!f||!AI.main)return;try{AI.main.img=await readImg(f);render(false)}catch(e){toast('Couldn’t read that image')}};
document.addEventListener('change',e=>{const t=e.target;if(t.form&&t.form.dataset.form==='saveHabit')hFormSync(t.form);
 if(t.dataset.hstep){const h=H(t.dataset.id),ds=t.dataset.d;if(!h)return;const L=new Set(h.rs[ds]||[]);t.checked?L.add(t.dataset.hstep):L.delete(t.dataset.hstep);h.rs[ds]=h.steps.filter(s=>L.has(s.id)).map(s=>s.id);if(!h.rs[ds].length)delete h.rs[ds];else delete h.skip[ds];if(t.checked)vib();if(hDone(h,ds))checkMilestones();save();render(false)}
 if(t.dataset.aim&&AI.main&&t.tagName!=='TEXTAREA'&&t.type!=='text'){AI.main[t.dataset.aim]=t.value;render(false)}});
document.addEventListener('input',e=>{const t=e.target;if(t.dataset.aim&&AI.main&&(t.tagName==='TEXTAREA'||t.type==='text'))AI.main[t.dataset.aim]=t.value;else if(t.dataset.ait!=null&&AI.plan){const g=AI.plan.goals[+t.dataset.ait];if(g)g.title=t.value}});
function seedHabits(){const day=864e5,tn=dnum(ymd());const mk=(o,fill)=>{const h=normHabit({id:uid(),createdAt:Date.now()-50*day,startDate:addDays(-49),...o});if(fill)for(let k=tn-49;k<tn;k++){const v=fill(tn-k,dUTC(k).getUTCDay());if(v)hSetVal(h,fromN(k),v)}S.habits.push(h);return h};
 const water=mk({title:'Drink water',icon:'💧',area:'health',target:8,unit:'glasses',cue:'After each meal'},a=>a%9===4?5:8);water.log[ymd()]=3;
 mk({title:'Read before bed',icon:'📖',area:'learning',part:'evening',target:20,unit:'pages',cue:'After I get into bed',mini:'Read one page',time:'21:30',remind:true},a=>a<12||a%6?20:0);
 mk({title:'Exercise',icon:'💪',area:'health',part:'morning',freq:'times',times:3},(a,w)=>[1,3,5].includes(w)&&a%11!==3?1:0);
 mk({title:'Pray on time',icon:'🙏',area:'faith',target:5,unit:'prayers'},a=>a%7===2?4:5);
 const mr=mk({kind:'routine',title:'Morning routine',icon:'☀️',area:'personal',part:'morning',steps:parseSteps('Drink a glass of water 1\nMake the bed 2\nStretch 5\nPlan the top three for today 5',[])});for(let k=tn-30;k<tn;k++)if((tn-k)%8)mr.rs[fromN(k)]=mr.steps.map(s=>s.id);mr.rs[ymd()]=[mr.steps[0].id];
 mk({title:'Family time, phones away',icon:'👨‍👩‍👧',area:'family',part:'evening',freq:'days',days:[0,5,6],target:30,unit:'minutes'},(a,w)=>[0,5,6].includes(w)?30:0);
 mk({kind:'quit',title:'No added sugar',icon:'🍬',area:'health',cost:3,plan:'Have fruit or a cup of tea instead',why:'Steady energy through the afternoon.',start:Date.now()-26*day,slips:[{t:Date.now()-9.6*day,trig:'Stress',note:'Office birthday cake'}]});
 mk({kind:'quit',mode:'limit',title:'Less social media',icon:'📱',area:'personal',limit:2,unit:'sessions',plan:'Message a friend or read instead',start:Date.now()-20*day,startDate:addDays(-20)},a=>a>20?0:a%5===0?3:a%2?1:2);
 S.habits.forEach(h=>{h.smile=h.kind==='quit'?0:floorMile(h.freq==='times'?WMILES:SMILES,hStreak(h));if(h.kind==='quit')h.mile=floorMile(h.mode==='limit'?SMILES:MILES,h.mode==='limit'?lStreak(h)-1:qDays(h))})}
