
/* ================= AUTOMATIONS =================
 ⚡ on a habit, step or day goal: check it off by itself when something true happens.
 Sources (each one asks its own permission, only when you first use it):
  steps (phone step counter) · workout and sleep (Health Connect, Android 14+) · screen time (Usage access) · places (location).
 Rules live on the item (h.auto / s.auto / x.auto, synced); places live on this device (settings.places).
 The phone checks the rules (Auto.java) and queues check-offs the same way as widget taps, with an Undo. */
const AUTO_T={
 steps:{n:'Steps',e:'👟',x:'When I walk enough steps',kinds:['habit','step','day']},
 workout:{n:'Workout',e:'🏋️',x:'When I log a workout (Health Connect)',kinds:['habit','step','day']},
 sleep:{n:'Sleep',e:'😴',x:'When I sleep long enough (Health Connect)',kinds:['habit']},
 place:{n:'Place',e:'📍',x:'When I’m at a place, or arrive / leave',kinds:['habit','step','day']},
 screen:{n:'Screen time',e:'📱',x:'When I stay under a limit in chosen apps',kinds:['habit']}};
const EX_TYPES=[['any','Any workout'],['56','Running'],['79','Walking'],['8','Cycling'],['70','Strength'],['83','Yoga'],['74','Swimming'],['64','Soccer'],['10','Boxing'],['36','HIIT']];
const places=()=>S.settings.places||[];
function astat(){if(!NATIVE||!NATIVE.autoStatus)return{};try{return JSON.parse(NATIVE.autoStatus())}catch(e){return{}}}
function autoGuess(t){t=String(t||'').toLowerCase();if(/step|walk/.test(t))return'steps';if(/sleep|bed/.test(t))return'sleep';if(/gym|workout|exercis|run|lift|train|yoga|swim|cycl|soccer|football|cricket/.test(t))return/gym|soccer|football|cricket/.test(t)?'place':'workout';if(/social|screen|phone|instagram|tiktok|youtube|scroll/.test(t))return'screen';if(/office|work|mosque|masjid|church|class|school/.test(t))return'place';return''}
function autoOf(k,o){return o&&o.auto&&o.auto.type?o.auto:null}
function autoLabel(a){if(!a)return'';const p=a.type==='place'?places().find(x=>x.id===a.place):null;
 return a.type==='steps'?`${(+a.n||0).toLocaleString()} steps`:a.type==='workout'?`${a.mins||20} min ${a.kind&&a.kind!=='any'?(EX_TYPES.find(x=>x[0]===a.kind)||[0,'workout'])[1].toLowerCase():'workout'}`:a.type==='sleep'?`${a.hours||7} h sleep`:a.type==='screen'?(a.log?'Logs screen time':`Under ${a.max||60} min`):p?(a.when==='arrive'?'Arrive at ':a.when==='leave'?'Leave ':'At ')+(p.emoji||'')+' '+p.name+(a.mins&&!a.when?` · ${a.mins} min`:''):'Place'}
function autoBtn(k,attrs,o,cls){const a=autoOf(k,o);return`<button class="bell auto ${a?'on':''} ${cls||''}" data-act="autoEdit" data-k="${k}" ${attrs} aria-label="Automatic check-off${a?': '+esc(autoLabel(a)):''}">${ic('flame')}${a&&cls!=='ic'?`<span>${esc(autoLabel(a))}</span>`:''}</button>`}
/* rules for the phone */
function autoRules(){const R=[],td=ymd();
 S.habits.forEach(h=>{const a=h.auto;if(!a||!a.type||h.status!=='active')return;R.push({...a,key:'h:'+h.id,kind:'habit',hid:h.id,hv:hTarget(h),title:`${hIcon(h)} ${h.title}`,m:[0,1,2,3,4,5,6].map(i=>h.freq!=='days'||h.days.includes(i)?'1':'0').join(''),log:a.type==='screen'&&h.kind==='quit'&&h.mode==='limit'})});
 S.goals.forEach(g=>{if(g.status!=='active')return;g.steps.forEach(s=>{const a=s.auto;if(!a||!a.type||s.done)return;R.push({...a,key:'s:'+g.id+'/'+s.id,kind:'step',g:g.id,s:s.id,title:s.title,...(s.due&&s.due>=td?{}:{})})})});
 S.days.forEach(x=>{const a=x.auto;if(!a||!a.type||x.done||x.date<td)return;R.push({...a,key:'d:'+x.id,kind:'day',id:x.id,title:x.title,date:x.date})});
 return R}
function autoSync(){if(!NATIVE||!NATIVE.autoSet)return;const R=autoRules(),k=JSON.stringify([R,places()]);if(autoSync.k===k)return;autoSync.k=k;try{NATIVE.autoSet(JSON.stringify({rules:R,places:places()}))}catch(e){}}

/* ---- the ⚡ sheet ---- */
let AU=null;
function auItem(){if(!AU)return null;if(AU.k==='habit')return H(AU.id);if(AU.k==='day')return S.days.find(x=>x.id===AU.id);const g=G(AU.g);return g&&g.steps.find(x=>x.id===AU.s)}
function autoBody(){const o=auItem();if(!o)return'';const a=o.auto&&o.auto.type?o.auto:null,D=AU.d||{},st=astat(),t=AU.pick||D.type||'';const K=AU.k;
 if(!NATIVE)return`<p class="small muted">Automatic check-offs use your phone’s sensors and apps, so they work in the Android app. Set them up there.</p>`;
 const types=Object.entries(AUTO_T).filter(([k,v])=>v.kinds.includes(K)&&!(k==='screen'&&o.kind==='routine'));
 let b=`<div class="au-types">${types.map(([k,v])=>`<button class="au-t${t===k?' on':''}" data-act="autoPick" data-t="${k}"><span>${v.e}</span><b>${v.n}</b><small>${v.x}</small></button>`).join('')}</div>`;
 if(!t)return b+`<p class="small muted" style="margin-top:12px">Pick what should check it off. You can change or remove it any time.</p>`;
 const cur={...(D.type===t?D:{}),type:t};const need=autoNeed(t,st);
 if(need)b+=`<div class="rm-snz mute">${ic('lock')}<div><b>${need.t}</b><small>${need.x}</small></div><button class="btn sm pri" data-act="autoPerm" data-t="${t}">${need.b}</button></div>`;
 if(t==='steps')b+=`<div class="field"><label>Steps in a day</label>${auChips('n',[[3000,'3,000'],[5000,'5,000'],[8000,'8,000'],[10000,'10,000'],[12000,'12,000']],cur.n||8000)}<div class="rm-inl"><span class="small muted">Or any number</span><input type="number" min="100" step="100" data-au="n" value="${cur.n||8000}"></div>${st.steps&&st.steps.perm?`<p class="small muted">Today so far: ${(+st.steps.today||0).toLocaleString()} steps</p>`:''}</div>`;
 if(t==='workout')b+=`<div class="field"><label>Kind</label><select data-au="kind">${EX_TYPES.map(([k,n])=>`<option value="${k}" ${String(cur.kind||'any')===k?'selected':''}>${n}</option>`).join('')}</select></div><div class="field"><label>At least</label>${auChips('mins',[[10,'10 min'],[20,'20 min'],[30,'30 min'],[45,'45 min'],[60,'1 hour']],cur.mins||20)}</div><p class="small muted">From any app that writes to Health Connect, like Samsung Health, Fitbit or Strava.</p>`;
 if(t==='sleep')b+=`<div class="field"><label>At least</label>${auChips('hours',[[6,'6 h'],[6.5,'6½ h'],[7,'7 h'],[7.5,'7½ h'],[8,'8 h']],cur.hours||7)}</div><p class="small muted">Last night’s sleep, from Health Connect.</p>`;
 if(t==='place'){const P=places();b+=`<div class="field"><label>Place</label>${P.length?`<div class="chips rm-ch">${P.map(p=>`<button type="button" class="chip${cur.place===p.id?' on':''}" data-act="auSet" data-f="place" data-v="${p.id}">${esc(p.emoji||'📍')} ${esc(p.name)}</button>`).join('')}</div>`:''}<button class="btn sm" data-act="placeNew" style="margin-top:8px">${ic('plus')}Add the place I’m at now</button></div>
  <div class="field"><label>Check it off</label>${auChips('when',[['','After I’ve been there a while'],['now','As soon as I arrive']],cur.when==='now'?'now':cur.when==='arrive'||cur.when==='leave'?'':cur.when||'')}</div>
  ${cur.when!=='now'&&cur.when!=='arrive'&&cur.when!=='leave'?`<div class="field"><label>Time there</label>${auChips('mins',[[10,'10 min'],[20,'20 min'],[30,'30 min'],[45,'45 min'],[60,'1 hour']],cur.mins||20)}</div>`:''}
  <div class="field"><label>Or just remind me</label>${auChips('when',[['arrive','When I arrive'],['leave','When I leave']],cur.when)}</div>`}
 if(t==='screen'){const lim=o.kind==='quit'&&o.mode==='limit';b+=`<div class="field"><label>Apps</label><div class="au-apps" id="auApps">${(cur.apps||[]).length?(cur.apps||[]).map(pk=>`<span class="chip on">${esc(appName(pk))}</span>`).join(''):'<span class="small muted">None picked yet</span>'}</div><button class="btn sm" data-act="appsPick" style="margin-top:8px">Choose apps</button></div>
  ${lim?`<p class="small muted">This habit counts minutes: Plotline logs the time you spend in these apps each day, and the limit does the rest.</p>`:`<div class="field"><label>Done if I stay under</label>${auChips('max',[[15,'15 min'],[30,'30 min'],[60,'1 hour'],[90,'1½ h'],[120,'2 h']],cur.max||60)}</div><div class="field"><label>Check at</label><input type="time" data-au="at" value="${cur.at||'21:00'}"></div>`}
  ${cur.apps&&cur.apps.length&&st.screen&&st.screen.perm?`<p class="small muted">Today so far: ${NATIVE.screenNow(cur.apps.join(','))} min</p>`:''}`}
 b+=`<label class="sw"><span>Tell me when it’s done<small>A notification with Undo</small></span><input type="checkbox" data-au="tell" ${cur.tell===false?'':'checked'}><i></i></label>`;
 const ready=autoReady(cur);
 b+=`<div class="actions">${a?`<button class="btn danger" data-act="autoOff">Turn off</button>`:''}<button class="btn pri" data-act="autoSave" ${ready?'':'disabled'}>${a?'Save':'Turn on'}</button></div>`;
 return b}
function autoReady(c){if(c.type==='place')return!!c.place&&places().some(p=>p.id===c.place);if(c.type==='screen')return!!(c.apps&&c.apps.length);return true}
function autoNeed(t,st){if(t==='steps'&&st.steps){if(!st.steps.ok)return{t:'No step counter on this phone',x:'Try Health Connect workouts instead.',b:'OK'};if(!st.steps.perm)return{t:'Allow physical activity',x:'Plotline reads the phone’s step counter. Nothing leaves the phone.',b:'Allow'}}
 if((t==='workout'||t==='sleep')&&st.hc){if(!st.hc.ok)return{t:'Needs Android 14',x:'Health Connect is built into Android 14 and newer.',b:'OK'};if(!st.hc.perm)return{t:'Connect Health Connect',x:'Plotline only reads workouts, sleep and steps, never writes.',b:'Connect'}}
 if(t==='screen'&&st.screen&&!st.screen.perm)return{t:'Allow usage access',x:'Android lists apps by name; turn on Plotline. It only counts minutes in the apps you pick.',b:'Open'};
 if(t==='place'&&st.loc){if(!st.loc.perm)return{t:'Allow location',x:'Used only to know when you’re at your saved places.',b:'Allow'};if(!st.loc.bg)return{t:'Allow location all the time',x:'So places work with the app closed. Choose “Allow all the time”.',b:'Open'}}
 return null}
const auChips=(f,opts,cur)=>`<div class="chips rm-ch">${opts.map(([v,l])=>`<button type="button" class="chip${String(cur)===String(v)?' on':''}" data-act="auSet" data-f="${f}" data-v="${v}">${l}</button>`).join('')}</div>`;
let APPS=null;const appName=pk=>{const a=(APPS||[]).find(x=>x.pk===pk);return a?a.name:pk.split('.').pop()};
function autoPaint(){const b=$('#auBody');if(b)b.innerHTML=autoBody()}
function auDraft(){const o=auItem();AU.d=AU.d||{...(o&&o.auto||{})};return AU.d}
Object.assign(ACT,{
 autoEdit:d=>{AU={k:d.k,id:d.id,g:d.g,s:d.s};const o=auItem();if(!o)return;AU.d={...(o.auto||{})};AU.pick=AU.d.type||autoGuess(o.title);if(AU.pick&&!AU.d.type)AU.d={type:AU.pick};if(NATIVE&&!APPS){try{APPS=JSON.parse(NATIVE.autoApps())}catch(e){APPS=[]}}
  openSheet(`<div class="data">Automatic check-off</div><h2 style="margin-top:6px">${esc(o.title)}</h2><div id="auBody"></div>`);autoPaintD()},
 autoPick:d=>{AU.pick=d.t;AU.d={...(AU.d&&AU.d.type===d.t?AU.d:{}),type:d.t};autoPaintD()},
 auSet:d=>{const D=auDraft();const v=isNaN(+d.v)||d.v===''?d.v:+d.v;D[d.f]=d.f==='when'&&v==='now'?'now':v;if(d.f==='when'&&D.when==='now')D.mins=0;autoPaintD()},
 autoPerm:async d=>{const t=d.t,st=astat();
  if(t==='steps'){if(st.steps&&!st.steps.ok)return;await permAsk('act','android.permission.ACTIVITY_RECOGNITION')}
  else if(t==='workout'||t==='sleep'){if(st.hc&&!st.hc.ok)return;const ok=await permAsk('hc','android.permission.health.READ_EXERCISE,android.permission.health.READ_SLEEP,android.permission.health.READ_STEPS');if(!ok)NATIVE.openHealthConnect()}
  else if(t==='screen'){NATIVE.openUsageAccess();window.addEventListener('focus',autoPaintD,{once:true})}
  else if(t==='place'){if(!st.loc.perm)await permAsk('loc','android.permission.ACCESS_FINE_LOCATION,android.permission.ACCESS_COARSE_LOCATION');else{const ok=await permAsk('bgloc','android.permission.ACCESS_BACKGROUND_LOCATION');if(!ok)NATIVE.openAppSettings()}}
  setTimeout(autoPaintD,400)},
 autoSave:()=>{const o=auItem();if(!o)return;const D={...auDraft(),type:AU.pick};const DEF={steps:{n:8000},workout:{mins:20,kind:'any'},sleep:{hours:7},screen:{max:60,at:'21:00'},place:{mins:20}}[D.type]||{};for(const k in DEF)if(D[k]==null||D[k]==='')D[k]=DEF[k];if(D.when==='now'){D.when='';D.mins=0}D.tell=D.tell!==false;o.auto=D;o.u=Date.now();if(AU.k==='step'){const g=G(AU.g);if(g)g.u=Date.now()}save();syncNative();closeSheet();render(false);toast('⚡ '+esc(autoLabel(D))+' · it checks itself off')},
 autoOff:()=>{const o=auItem();if(!o)return;delete o.auto;o.u=Date.now();save();syncNative();closeSheet();render(false);toast('Automatic check-off off')},
 appsPick:()=>{const D=auDraft(),sel=new Set(D.apps||[]);const L=(APPS||[]).slice().sort((a,b)=>(b.cat===4)-(a.cat===4)||a.name.localeCompare(b.name));
  const d=document.createElement('div');d.className='vdlg';d.innerHTML=`<div class="vdlg-b au-pick"><h3>Choose apps</h3><p class="small muted">Social apps are first.</p><div class="au-list">${L.map(a=>`<label class="au-app"><input type="checkbox" value="${esc(a.pk)}" ${sel.has(a.pk)?'checked':''}><span>${esc(a.name)}${a.cat===4?' <em>social</em>':''}</span></label>`).join('')||'<p class="small muted">No apps found.</p>'}</div><div class="actions"><button class="btn ghost" data-act="appsCancel">Cancel</button><button class="btn pri" data-act="appsOk">Done</button></div></div>`;document.body.appendChild(d)},
 appsOk:()=>{const d=document.querySelector('.vdlg');if(!d)return;auDraft().apps=[...d.querySelectorAll('input:checked')].map(i=>i.value);d.remove();autoPaintD()},
 appsCancel:()=>document.querySelector('.vdlg')?.remove(),
 placeNew:async()=>{const st=astat();if(st.loc&&!st.loc.perm){if(!(await permAsk('loc','android.permission.ACCESS_FINE_LOCATION,android.permission.ACCESS_COARSE_LOCATION')))return toast('Location is needed to save a place')}
  toast('Finding where you are…');const r=await new Promise(res=>{window.__here=j=>{try{res(JSON.parse(j))}catch(e){res({err:'bad'})}};NATIVE.placeHere()});
  if(r.err)return toast(r.err==='perm'?'Allow location to save a place':'Couldn’t find your location. Try again outside or with Wi-Fi on');
  const d=document.createElement('div');d.className='vdlg';d.innerHTML=`<form class="vdlg-b" data-form="placeSave" data-lat="${r.lat}" data-lng="${r.lng}"><h3>Save this place</h3><p class="small muted">Accurate to about ${Math.round(r.acc||50)} m.</p><div class="field"><label>Name</label><input name="name" required maxlength="30" placeholder="Gym, Work, Home, Mosque…"></div><div class="two"><div class="field"><label>Emoji</label><input name="emoji" class="emo-in" maxlength="16" placeholder="📍"></div><div class="field"><label>Size</label><select name="r"><option value="100">Small · 100 m</option><option value="150" selected>Normal · 150 m</option><option value="300">Large · 300 m</option></select></div></div><div class="actions"><button type="button" class="btn ghost" data-act="appsCancel">Cancel</button><button class="btn pri">Save place</button></div></form>`;document.body.appendChild(d);setTimeout(()=>d.querySelector('input').focus(),50)},
 placeDel:d=>askConfirm('Delete this place?','Automations that use it stop working until you pick another place.','Delete',()=>{S.settings.places=places().filter(p=>p.id!==d.id);save();syncNative();closeSheet();render(false)})});
Object.assign(FORM,{placeSave:f=>{const E=f.elements,name=E.name.value.trim();if(!name)return;const p={id:uid(),name,emoji:oneEmoji(E.emoji.value),lat:+f.dataset.lat,lng:+f.dataset.lng,r:+E.r.value||150};S.settings.places=[...places(),p];save();syncNative();document.querySelector('.vdlg')?.remove();if(AU){auDraft().place=p.id;autoPaintD()}else render(false);toast('Place saved')}});
document.addEventListener('change',e=>{const t=e.target;if(!t.dataset||t.dataset.au==null||!AU)return;const D=auDraft();D[t.dataset.au]=t.type==='checkbox'?t.checked:t.type==='number'?+t.value:t.value;if(t.type!=='checkbox')autoPaintD()});
function autoPaintD(){if(AU&&AU.pick){AU.d=AU.d||{};AU.d.type=AU.pick}const b=$('#auBody');if(b)b.innerHTML=autoBody()}

/* ---- Settings → Automations ---- */
function autoSub(){if(!NATIVE)return'In the Android app';const n=autoRules().length;return n?`${n} item${n===1?'':'s'} check${n===1?'s':''} itself off`:'Off · set up with ⚡ on a habit or step'}
function autoHTML(){if(!NATIVE)return`<section class="panel rv"><h3>Automations</h3><p class="small muted">Automatic check-offs use your phone’s step counter, Health Connect, screen time and places, so they live in the Android app.</p></section>`;
 const st=astat(),R=autoRules(),ok=(b,txt)=>`<span class="au-ok ${b?'on':''}">${b?ic('check'):''}${txt}</span>`;
 return`<section class="panel rv"><h3>How it works</h3><p class="small muted">Tap ⚡ on a habit, step or day goal and pick what checks it off: steps, a workout, sleep, time at a place, or staying under a screen-time limit. Everything is read on this phone. You get a notification with Undo each time.</p></section>
 <section class="panel rv"><h3>Sources</h3><div class="au-src">
  <div><b>👟 Steps</b><small>Phone step counter${st.steps&&st.steps.perm?` · ${(+st.steps.today||0).toLocaleString()} today`:''}</small>${st.steps&&!st.steps.ok?ok(false,'Not on this phone'):st.steps&&st.steps.perm?ok(true,'On'):`<button class="btn sm" data-act="autoPerm" data-t="steps">Allow</button>`}</div>
  <div><b>🏋️ Health Connect</b><small>Workouts, sleep and steps from your fitness apps</small>${st.hc&&!st.hc.ok?ok(false,'Android 14+'):st.hc&&st.hc.perm?ok(true,'Connected'):`<button class="btn sm" data-act="autoPerm" data-t="workout">Connect</button>`}</div>
  <div><b>📱 Screen time</b><small>Minutes in apps you choose</small>${st.screen&&st.screen.perm?ok(true,'On'):`<button class="btn sm" data-act="autoPerm" data-t="screen">Allow</button>`}</div>
  <div><b>📍 Location</b><small>Only for your saved places</small>${st.loc&&st.loc.perm&&st.loc.bg?ok(true,'All the time'):st.loc&&st.loc.perm?`<button class="btn sm" data-act="autoPerm" data-t="place">Allow all the time</button>`:`<button class="btn sm" data-act="autoPerm" data-t="place">Allow</button>`}</div></div></section>
 <section class="panel rv"><h3>Places</h3>${places().length?`<div class="chl">${places().map(p=>`<div class="chl-r"><span style="font-size:20px">${esc(p.emoji||'📍')}</span><span><b>${esc(p.name)}</b><small>${p.r} m around it${st.inside&&st.inside[p.id]?' · you’re here now':''}</small></span><button class="ibtn sm" data-act="placeDel" data-id="${p.id}" aria-label="Delete ${esc(p.name)}">${ic('trash')}</button></div>`).join('')}</div>`:'<p class="small muted">No places yet.</p>'}<div class="actions left"><button class="btn sm" data-act="placeNew">${ic('plus')}Add the place I’m at now</button></div></section>
 <section class="panel rv"><h3>In use</h3>${R.length?`<div class="chl">${R.map(r=>`<div class="chl-r"><span style="font-size:18px">${AUTO_T[r.type].e}</span><span><b>${esc(r.title)}</b><small>${esc(autoLabel(r))}</small></span></div>`).join('')}</div>`:'<p class="small muted">Nothing yet. Look for ⚡ on a habit or step.</p>'}</section>`}
