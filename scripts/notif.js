
/* ================= NOTIFICATIONS =================
 What reminds you, when it stays quiet, and the replies you send from the notification bar.
 The phone reads simple replies itself (done, busy 2h, snooze 20 min, skip today). Anything else waits in the
 queue; when the app opens, the on-device AI (or your AI server) reads it and acts, or it is saved to the journal. */
const NOTIF_DEF={habits:true,steps:true,days:true,checkins:true,miles:true,wrap:true,wrapAt:'21:00',reply:true,mute:120,quiet:false,qFrom:'22:30',qTo:'07:00'};
const nset=()=>({...NOTIF_DEF,...(S.settings.notif||{})});
const hm=t=>{const[h,m]=String(t||'0:0').split(':').map(Number);return(h||0)*60+(m||0)};
function inQuiet(at){const n=nset();if(!n.quiet)return false;const d=new Date(at),m=d.getHours()*60+d.getMinutes(),a=hm(n.qFrom),b=hm(n.qTo);return a<b?m>=a&&m<b:m>=a||m<b}
/* evening wrap-up: one notification listing what's still open, answerable in one reply */
function wrapAlarms(now,L){const n=nset();if(!n.wrap)return;for(let k=0;k<3;k++){const d=new Date();d.setDate(d.getDate()+k);const[hh,mm]=n.wrapAt.split(':').map(Number);d.setHours(hh,mm,0,0);if(d.getTime()<=now)continue;const ds=ymd(d);
 const hs=S.habits.filter(h=>h.status==='active'&&h.kind!=='quit'&&hCounts(h,ds)&&!hDone(h,ds));if(!hs.length)continue;
 L.push({at:d.getTime(),title:hs.length===1?`Still open today: ${hs[0].title}`:`${hs.length} habits still open today`,body:hs.map(h=>hIcon(h)+' '+h.title).join(' · ')+'. Reply with what you did, or tap All done.',k:'wrap',hd:ds,hs:hs.map(h=>({id:h.id,t:h.title,v:hTarget(h)}))})}}
function nlist(L){const n=nset(),ok={habit:n.habits,wrap:n.wrap,step:n.steps,day:n.days,checkin:n.checkins,mile:n.miles};
 return L.filter(x=>ok[x.k]!==false&&!(x.k!=='timer'&&inQuiet(x.at))).map(x=>({...x,rp:x.rp===false?false:n.reply,mute:n.mute})).sort((a,b)=>a.at-b.at)}
function notifState(){try{return JSON.parse(NATIVE&&NATIVE.notifState?NATIVE.notifState():'{}')}catch(e){return{}}}
function notifHTML(){const n=nset(),ns=NATIVE?notifState():{},mu=ns.muteUntil>Date.now();
 const tg=(k,t,sub)=>`<label class="sw"><span>${t}${sub?`<small>${sub}</small>`:''}</span><input type="checkbox" data-nset="${k}" ${n[k]?'checked':''}><i></i></label>`;
 return`<section class="panel rv" id="notifSet"><h3>Notifications</h3><p class="small muted">${NATIVE?'Reply right from the notification bar: “done”, “busy at work for 2 hours”, “snooze 20 min”, “skip today”. Anything else is read by the assistant when you next open the app.':'Notifications with replies and Done buttons need the Android app. Here, reminders show while the app is open.'}</p>
 ${NATIVE?`<div class="nmute${mu?' on':''}"><div><b>${mu?'Muted until '+new Date(ns.muteUntil).toLocaleTimeString([],{hour:'numeric',minute:'2-digit'}):'Focus mode'}</b><small>${mu?'Reminders wait and arrive as one summary when this ends.':'Pause every reminder for a while.'}</small></div>${mu?`<button class="btn sm" data-act="notifMute" data-m="0">Unmute</button>`:`<div class="nmb">${[[60,'1h'],[120,'2h'],[240,'4h']].map(([m,l])=>`<button class="btn sm" data-act="notifMute" data-m="${m}">${l}</button>`).join('')}</div>`}</div>`:''}
 <div class="nlist">${tg('habits','Habit reminders','At the time set on each habit')}${tg('wrap','Evening wrap-up','One list of habits still open')}${n.wrap?`<label class="nrow"><span>Wrap-up time</span><input type="time" data-nset="wrapAt" value="${n.wrapAt}"></label>`:''}${tg('steps','Step reminders','Steps with a reminder')}${tg('days','Day goals','Day goals with a time')}${tg('checkins','Goal check-ins','Daily or weekly, set on each goal')}${tg('miles','Milestones','Clean-time wins on habits you’re breaking')}${NATIVE?tg('reply','Reply and Done buttons','Act on reminders without opening the app'):''}
 ${NATIVE?`<label class="nrow"><span>Mute button length</span><select data-nset="mute">${[[30,'30 min'],[60,'1 hour'],[120,'2 hours'],[180,'3 hours'],[240,'4 hours']].map(([m,l])=>`<option value="${m}" ${+n.mute===m?'selected':''}>${l}</option>`).join('')}</select></label>`:''}
 ${tg('quiet','Quiet hours','No reminders overnight')}${n.quiet?`<label class="nrow"><span>From</span><input type="time" data-nset="qFrom" value="${n.qFrom}"></label><label class="nrow"><span>Until</span><input type="time" data-nset="qTo" value="${n.qTo}"></label>`:''}</div></section>`}
document.addEventListener('change',e=>{const t=e.target;if(!t.dataset||!t.dataset.nset)return;const k=t.dataset.nset;S.settings.notif={...nset(),[k]:t.type==='checkbox'?t.checked:k==='mute'?+t.value:t.value};save();syncNative();if(t.type==='checkbox')render(false)});
Object.assign(ACT,{notifMute:d=>{if(!NATIVE||!NATIVE.notifMute)return;const msg=NATIVE.notifMute(+d.m||0);toast(esc(msg||'Done'));render(false)}});

/* ---- replies the phone couldn't place: ask the assistant what they mean ---- */
const REPLY_RX={done:/\b(done|did|finished|completed?|yes|yep|already|drank|ran|read|prayed|walked|worked out)\b/i,neg:/\b(not|didn'?t|haven'?t|no|forgot)\b/i,skip:/\b(skip|rest|day off|sick|not today)\b/i};
function replyCtx(a){const o=a.o||{};if(o.k==='wrap')return(o.hs||[]).map(x=>({id:x.id,t:x.t}));if(o.k==='habit'){const h=H(o.hid);return h?[{id:h.id,t:h.title}]:[]}return[]}
async function replyAI(a){const o=a.o||{},hs=replyCtx(a),e=typeof planEngine==='function'?planEngine():'';
 const kw=()=>{const t=a.text.toLowerCase();if(REPLY_RX.neg.test(t))return{action:'note',ids:[]};if(REPLY_RX.done.test(t)){const ids=hs.filter(h=>toksOf(h.t).some(w=>toksOf(t).includes(w))).map(h=>h.id);return{action:'done',ids:ids.length||hs.length!==1?ids:[hs[0].id]}}if(REPLY_RX.skip.test(t))return{action:'skip',ids:hs.length===1?[hs[0].id]:[]};return{action:'note',ids:[]}};
 if(!e||e==='native'&&!(typeof natHave==='function'&&natHave(llmOf(insSet().llm))))return kw();
 const schema={type:'object',properties:{action:{enum:['done','skip','note']},ids:{type:'array',items:hs.length?{enum:hs.map(h=>h.id)}:{type:'string'},maxItems:12}},required:['action','ids'],additionalProperties:false};
 const msgs=[{role:'system',content:'You read short replies a person typed to a reminder notification in their habits app. Reply with JSON only.'},{role:'user',content:`Reminder: ${o.title||''}\n${o.body||''}\n${hs.length?'Habits in it:\n'+hs.map(h=>`- id ${h.id}: ${h.t}`).join('\n')+'\n':''}\nTheir reply: "${a.text}"\n\nWhich of these habits do they say they DID? action "done" with those ids. If they say they are skipping or resting today, action "skip". Otherwise action "note" with no ids.`}];
 let out='';const onTok=x=>{out+=x};const opts={schema,max_tokens:120,temperature:0};
 try{if(e==='api')await apiChat(msgs,onTok,new AbortController().signal,opts);else if(e==='native')await natChat(msgs,onTok,()=>{},null,opts);else return kw();const j=JSON.parse(out.slice(out.indexOf('{'),out.lastIndexOf('}')+1));if(!['done','skip','note'].includes(j.action))return kw();j.ids=(j.ids||[]).filter(id=>hs.some(h=>h.id===id));return j}catch(err){return kw()}}
async function applyReplies(list){let n=0,notes=0;for(const a of list){const o=a.o||{},r=await replyAI(a),d=o.hd&&validDate(o.hd)?o.hd:ymd();
  if(r.action==='done'&&r.ids.length){r.ids.forEach(id=>{const h=H(id);if(h&&d<=ymd()){hSetVal(h,d,hTarget(h));n++}})}
  else if(r.action==='skip'&&r.ids.length){r.ids.forEach(id=>{const h=H(id);if(h){h.skip[d]=1;n++}})}
  else{const g=o.g&&G(o.g),h=o.hid&&H(o.hid);S.entries.push({id:uid(),t:a.at||Date.now(),type:'note',goalId:g?g.id:h&&h.goalId||null,title:'Reply to “'+trunc(o.title||'a reminder',60)+'”',text:a.text,mood:'',sid:null,img:null,u:Date.now()});notes++}}
 if(n)checkMilestones();if(n||notes){save();render(false);toast(`${n?`${n} habit${n>1?'s':''} updated from your replies`:''}${n&&notes?' · ':''}${notes?`${notes} repl${notes>1?'ies':'y'} saved to your journal`:''}`)}}
