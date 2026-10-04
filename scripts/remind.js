
/* ================= REMINDERS PER ITEM =================
 A bell on every habit, step, day goal and goal opens one small sheet: on/off, when, and a preview of the
 next reminders. Defaults follow the plan: a habit's set time (or its part of the day), a step's or day
 goal's due time, a goal's check-in and target date. Changes save as you tap. */
const PART_T={morning:'08:00',afternoon:'13:00',evening:'19:00',any:'09:00'};
const MORNING_OF='08:00';
const partTime=h=>PART_T[h.part||'any']||'09:00';
/* step and day goal reminder: '' off · minutes before the due time · 'm' morning of · 'c' custom date+time (remDate/remTime) */
function remAtOf(o,date,time){const r=o.remind;if(r===''||r==null)return null;
 if(r==='c'){if(!validDate(o.remDate)||!o.remTime)return null;const[y,m,d]=o.remDate.split('-').map(Number),[hh,mm]=o.remTime.split(':').map(Number);return new Date(y,m-1,d,hh,mm).getTime()}
 if(!date)return null;const[y,m,d]=date.split('-').map(Number);
 if(r==='m'){const[hh,mm]=MORNING_OF.split(':').map(Number);return new Date(y,m-1,d,hh,mm).getTime()}
 const[hh,mm]=(time||'09:00').split(':').map(Number);return new Date(y,m-1,d,hh,mm).getTime()-(+r)*6e4}
const dayRem=x=>x.remind==null?(x.time?'0':''):x.remind;
const dayRemAt=x=>remAtOf({...x,remind:dayRem(x)},x.date,x.time);
const remLabel=(r,o)=>r===''||r==null?'Off':r==='m'?'Morning of':r==='c'?(o&&o.remDate&&o.remTime?fmtDate(o.remDate)+' · '+fmtTime(o.remTime):'Custom'):r==='0'?'At the time':({5:'5 min',15:'15 min',30:'30 min',60:'1 hour',120:'2 hours',1440:'1 day',10080:'1 week'}[r]||r+' min')+' before';
const fmtAt=t=>{const d=new Date(t),td=ymd(),ds=ymd(d);return(ds===td?'Today':ds===addDays(1)?'Tomorrow':d.toLocaleDateString(undefined,{weekday:'short',month:'short',day:'numeric'}))+' · '+d.toLocaleTimeString([],{hour:'numeric',minute:'2-digit'})};
/* habit reminder times on a day: main time, an optional second time, and a nudge if still not done */
function habitRemTimes(h){if(!h.remind)return[];const t=[h.time||partTime(h)];if(h.time2)t.push(h.time2);return t}
/* the bell on an item: shows the next reminder at a glance */
function bellBtn(k,attrs,on,label,cls=''){return`<button class="bell ${on?'on':''} ${cls}" data-act="remEdit" data-k="${k}" ${attrs} aria-label="Reminder: ${esc(label)}">${ic('bell')}${label&&cls!=='ic'?`<span>${esc(label)}</span>`:''}</button>`}
const habitBell=(h,cls)=>h.kind==='quit'?'':bellBtn('habit',`data-id="${h.id}"`,h.remind,h.remind?fmtTime(h.time||partTime(h))+(h.time2?' +1':''):'Off',cls);
const stepBell=(g,s,cls)=>s.done?'':bellBtn('step',`data-g="${g.id}" data-s="${s.id}"`,!!remAtOf(s,s.due,s.time),remAtOf(s,s.due,s.time)?remLabel(s.remind,s):'Off',cls);
const dayBell=(x,cls)=>x.done?'':bellBtn('day',`data-id="${x.id}"`,!!dayRemAt(x),dayRemAt(x)?(x.time&&dayRem(x)==='0'?fmtTime(x.time):remLabel(dayRem(x),x)):'Off',cls);
const goalBell=(g,cls)=>bellBtn('goal',`data-id="${g.id}"`,!!(g.checkin||g.remTarget),g.checkin?cap(g.checkin)+' check-in':g.remTarget?'Target reminder':'Reminders',cls);

let REM=null;
function remItem(){if(!REM)return null;if(REM.k==='habit')return H(REM.id);if(REM.k==='day')return S.days.find(x=>x.id===REM.id);if(REM.k==='goal')return G(REM.id);const g=G(REM.g);return g&&g.steps.find(x=>x.id===REM.s)}
function remNext(){const now=Date.now(),L=[],o=remItem();if(!o)return[];
 if(REM.k==='habit'){for(let k=0;k<8&&L.length<3;k++){const d=new Date();d.setDate(d.getDate()+k);const ds=ymd(d);if(!hCounts(o,ds)||hDone(o,ds)&&k===0)continue;habitRemTimes(o).forEach(t=>{const[hh,mm]=t.split(':').map(Number);d.setHours(hh,mm,0,0);if(d.getTime()>now)L.push(d.getTime())});if(o.again&&o.remind){const[hh,mm]=(o.time||partTime(o)).split(':').map(Number);d.setHours(hh,mm,0,0);const t=d.getTime()+o.again*6e4;if(t>now)L.push(t)}}return L.sort((a,b)=>a-b).slice(0,3)}
 if(REM.k==='step'){const t=remAtOf(o,o.due,o.time);return t&&t>now?[t]:[]}
 if(REM.k==='day'){const t=dayRemAt(o);return t&&t>now?[t]:[]}
 const g=o;if(g.checkin){const[hh,mm]=(g.checkinTime||'19:00').split(':').map(Number);for(let k=0;k<14&&L.length<2;k++){const d=new Date();d.setDate(d.getDate()+k);d.setHours(hh,mm,0,0);if(d.getTime()<=now)continue;if(g.checkin==='weekly'&&d.getDay()!==+g.checkinDay)continue;L.push(d.getTime())}}
 const tt=goalTargetAt(g);if(tt&&tt>now)L.push(tt);return L.sort((a,b)=>a-b)}
function goalTargetAt(g){if(!g.targetDate||g.remTarget===''||g.remTarget==null||g.status!=='active')return null;const[y,m,d]=g.targetDate.split('-').map(Number);return new Date(y,m-1,d,9,0).getTime()-(+g.remTarget)*864e5}
const chipsRow=(f,opts,cur)=>`<div class="chips rm-ch">${opts.map(([v,l])=>`<button type="button" class="chip${String(cur)===String(v)?' on':''}" data-act="remSet" data-f="${f}" data-v="${v}">${l}</button>`).join('')}</div>`;
function remBody(){const o=remItem();if(!o)return'<p class="small muted">This item is gone.</p>';const nx=remNext(),K=REM.k;let b='';
 const sw=(on,f)=>`<label class="sw rm-sw"><span>${on?'Reminder on':'Reminder off'}<small>${on?'You can change anything below':'Turn it on to get a nudge'}</small></span><input type="checkbox" data-rm="${f}" ${on?'checked':''}><i></i></label>`;
 if(K==='habit'){const t=o.time||partTime(o),sug=[...new Set([o.time,partTime(o),'07:00','08:00','12:30','18:00','21:00'].filter(Boolean))].sort();
  b+=sw(o.remind,'remind');
  if(o.remind){b+=`<div class="field"><label>Remind me at</label>${chipsRow('time',sug.map(v=>[v,fmtTime(v)]),t)}<div class="rm-inl"><span class="small muted">Or pick a time</span><input type="time" data-rm="time" value="${t}"></div></div>
   <div class="field"><label>If it’s still not done</label>${chipsRow('again',[[0,'No nudge'],[30,'30 min later'],[60,'1 h later'],[120,'2 h later'],[180,'3 h later']],o.again||0)}</div>
   <div class="field"><label>Second reminder</label>${chipsRow('time2',[['','None'],['12:30',fmtTime('12:30')],['18:00',fmtTime('18:00')],['21:00',fmtTime('21:00')]],o.time2||'')}<div class="rm-inl"><span class="small muted">Or pick a time</span><input type="time" data-rm="time2" value="${o.time2||''}"></div></div>
   <p class="small muted">On the days this habit is due · ${esc(freqText(o))}. Change the schedule in Edit.</p>`}}
 else if(K==='step'||K==='day'){const r=K==='day'?dayRem(o):String(o.remind??''),date=K==='day'?o.date:o.due,time=o.time;
  b+=sw(r!=='','remind');
  b+=`<div class="two"><div class="field"><label>${K==='day'?'Day':'Due'}</label><input type="date" data-rm="${K==='day'?'date':'due'}" value="${date||''}"></div><div class="field"><label>Time</label><input type="time" data-rm="ptime" value="${time||''}"></div></div>`;
  if(r!==''){b+=`<div class="field"><label>When</label>${chipsRow('remind',[['0',time?'At '+fmtTime(time):'At 9:00 AM'],['15','15 min before'],['30','30 min before'],['60','1 h before'],['1440','1 day before'],...(K==='step'?[['10080','1 week before']]:[]),['m','Morning of'],['c','Custom']],r)}</div>`;
   if(r==='c')b+=`<div class="two"><div class="field"><label>Remind on</label><input type="date" data-rm="remDate" value="${o.remDate||date||ymd()}"></div><div class="field"><label>At</label><input type="time" data-rm="remTime" value="${o.remTime||time||'09:00'}"></div></div>`;
   if(!date&&r!=='c')b+=`<p class="small warn">Add a ${K==='day'?'day':'due date'} so the reminder knows when.</p>`}}
 else{const g=o;b+=`<div class="field"><label>Check-in</label>${chipsRow('checkin',[['','Off'],['daily','Every day'],['weekly','Every week']],g.checkin||'')}</div>`;
  if(g.checkin){b+=`<div class="two"><div class="field"><label>At</label><input type="time" data-rm="checkinTime" value="${g.checkinTime||'19:00'}"></div>${g.checkin==='weekly'?`<div class="field"><label>On</label><select data-rm="checkinDay">${DAYS.map((d,i)=>`<option value="${i}" ${+g.checkinDay===i?'selected':''}>${d}</option>`).join('')}</select></div>`:''}</div>`}
  b+=`<div class="field"><label>Target date${g.targetDate?' · '+fmtDate(g.targetDate):''}</label>${g.targetDate?chipsRow('remTarget',[['','Off'],['0','On the day'],['1','1 day before'],['7','1 week before'],['30','1 month before']],g.remTarget??''):'<p class="small muted">Set a target date in Edit to get reminded before it.</p>'}</div>`;
  const open=g.steps.filter(s=>!s.done);if(open.length)b+=`<div class="field"><label>Steps</label><div class="rm-steps">${open.slice(0,12).map(s=>`<div class="rm-st"><span class="ell">${esc(s.title)}<small>${s.due?fmtDate(s.due)+(s.time?' · '+fmtTime(s.time):''):'No date'}</small></span>${stepBell(g,s)}</div>`).join('')}</div>${open.some(s=>s.due&&!remAtOf(s,s.due,s.time))?`<button class="btn sm" data-act="remAllSteps" data-id="${g.id}">${ic('bell')}Remind me for every dated step</button>`:''}</div>`}
 b+=`<div class="rm-next">${ic('bell')}<div><b>${nx.length?'Next: '+fmtAt(nx[0]):'No reminder coming up'}</b>${nx.length>1?`<small>Then ${nx.slice(1).map(fmtAt).join(' · ')}</small>`:''}${NATIVE&&nset&&!nset()[{habit:'habits',step:'steps',day:'days',goal:'checkins'}[K]]?`<small class="warn">This type is turned off in Settings → Notifications.</small>`:''}</div></div>`;
 return b}
function remTitle(){const o=remItem();return o?o.title:''}
function remOpen(){openSheet(`<div class="data">Reminders</div><h2 style="margin-top:6px">${esc(remTitle())}</h2><div id="remBody">${remBody()}</div><div class="actions">${REM.back?`<button class="btn ghost" data-act="remBack">Back</button>`:''}<button class="btn pri" data-act="close">Done</button></div>`)}
function remApply(f,v){const o=remItem();if(!o)return;const K=REM.k;
 if(K==='habit'){if(f==='remind'){o.remind=!!v;if(v&&!o.time)o.time=partTime(o)}else if(f==='again')o.again=+v||0;else if(f==='time'){if(v)o.time=v}else if(f==='time2')o.time2=v||''}
 else if(K==='step'||K==='day'){if(f==='remind'){o.remind=v===true?(o.time?'0':'m'):v===false?'':v;if(o.remind==='c'){o.remDate=o.remDate||(K==='day'?o.date:o.due)||ymd();o.remTime=o.remTime||o.time||'09:00'}}else if(f==='ptime')o.time=v||'';else if(f==='due')o.due=validDate(v);else if(f==='date'){if(validDate(v))o.date=v}else o[f]=v;if(K==='step')o.reminded=false}
 else{if(f==='checkinDay')o.checkinDay=+v;else if(f==='remTarget')o.remTarget=v;else o[f]=v;if(f==='checkin'&&v&&!o.checkinTime)o.checkinTime='19:00'}
 o.u=Date.now();if(K==='step'){const g=G(REM.g);if(g)g.u=Date.now()}save();syncNative();render(false);const bd=$('#remBody');if(bd)bd.innerHTML=remBody();if(v&&v!=='')askNotif()}
document.addEventListener('change',e=>{const t=e.target;if(!t.dataset||t.dataset.rm==null||!REM)return;remApply(t.dataset.rm,t.type==='checkbox'?t.checked:t.value)});
Object.assign(ACT,{
 remEdit:d=>{const back=REM&&REM.k==='goal'&&d.k==='step'&&$('#remBody')?{...REM}:null;REM={k:d.k,id:d.id,g:d.g,s:d.s,back};remOpen()},
 remBack:()=>{if(REM&&REM.back){REM=REM.back;remOpen()}},
 remSet:d=>remApply(d.f,d.v),
 remAllSteps:d=>{const g=G(d.id);if(!g)return;let n=0;g.steps.forEach(s=>{if(!s.done&&s.due&&!remAtOf(s,s.due,s.time)){s.remind='0';s.reminded=false;n++}});g.u=Date.now();save();syncNative();askNotif();const bd=$('#remBody');if(bd)bd.innerHTML=remBody();toast(`Reminders on for ${n} step${n===1?'':'s'}`)}});
