/* ---- reactions: a few emoji and short quick messages instead of a single 👏; live; at most 50 a day ----
 Sent ones live in my own encrypted progress doc as cheers:[{id,to,emoji,m,t}] (same field as 1.8.2's 👏, so
 older apps still show them). The sender's app stops at 50 a day (counted across devices, since S.shares syncs).
 The sharing rules can't count inside encrypted data, so every receiver also ignores anything past 50 a day
 from one person. Quick messages are a short list each person edits (settings.share.quick); there is no free chat.
 settings.share.rx = {cid:{t,n}} per device: newest reaction already shown, and how many are unread. */
const REACT_EMO=['👏','🔥','💪','❤️','🎉','🙌'];
const REACT_MSG=['You got this','Gym time?','Proud of you','Don’t break the streak','Let’s go!','Missed you today'];
const REACT_MAX=50;
const quickMsgs=()=>{const q=shset().quick;return Array.isArray(q)&&q.length?q:REACT_MSG};
const dayOf=t=>ymd(new Date(t));
function reactsSentToday(){const d=ymd(),ids=new Set();shares().forEach(x=>(x.myCheers||[]).concat(x.cheerOut||[]).forEach(c=>{if(c&&dayOf(c.t)===d)ids.add(c.id||x.id+c.t)}));return ids.size}
/* only the first 50 a day from each person count, whatever their app sent */
function capReacts(L){const n={};return L.slice().sort((a,b)=>a.t-b.t).filter(r=>{const k=r.fe+'|'+dayOf(r.t);n[k]=(n[k]||0)+1;return n[k]<=REACT_MAX})}
let SHR={};
function shCollect(sh,ms){SHR[sh.id]=(ms||[]).filter(m=>m.email!==fme()&&m.status!=='left').flatMap(m=>(m.cheers||[]).filter(c=>c&&c.to===fme()&&+c.t>0&&+c.t<Date.now()+6e5).map(c=>({id:c.id||m.email+c.t,from:m.name,fe:m.email,e:String(c.emoji||'').slice(0,16),m:String(c.m||'').slice(0,60),t:+c.t,cid:sh.id})));shInbox()}
const reactsForMe=cid=>{const all=capReacts(Object.values(SHR).flat());return cid?all.filter(r=>r.cid===cid):all};
const rxUnread=cid=>((shset().rx||{})[cid]||{}).n||0;
function rxRead(cid){const rx={...(shset().rx||{})};if(rx[cid]&&rx[cid].n){rx[cid]={...rx[cid],n:0};shPatch({rx});render(false)}}
const openGroup=()=>{const el=document.querySelector('#sheet.on [data-shg]');return el&&el.dataset.shg};
function shInbox(){const rx={...(shset().rx||{})},fresh=[],og=openGroup();let ch=false;
 for(const cid of Object.keys(SHR)){const o={...(rx[cid]||{t:Date.now()-6*36e5,n:0})},nw=reactsForMe(cid).filter(r=>r.t>o.t);if(!rx[cid])ch=true;
  if(nw.length){o.t=Math.max(...nw.map(r=>r.t));if(cid!==og)o.n=(o.n||0)+nw.length;fresh.push(...nw);ch=true}rx[cid]=o}
 if(!ch)return;shPatch({rx});fbNative();if(!fresh.length)return;
 const bg=fresh.filter(r=>r.cid!==og);fresh.filter(r=>r.cid===og).slice(-3).forEach((r,i)=>setTimeout(()=>reactBurst(r.e||'💬'),i*350));
 if(bg.length){reactBanner(bg);render(false)}}
/* the banner: shows on any page while the app is open */
let RB={q:[],tm:0};
function reactBanner(L){RB.q=[...RB.q,...L.sort((a,b)=>a.t-b.t)].slice(-12);reactShow();reactBurst(L[L.length-1].e||'💬');try{navigator.vibrate&&navigator.vibrate([20,60,20])}catch(e){}}
function reactShow(){let el=$('#rxb');if(!el){el=document.createElement('div');el.id='rxb';el.className='rxb';el.setAttribute('role','status');el.setAttribute('aria-live','polite');document.body.appendChild(el)}
 clearTimeout(RB.tm);const r=RB.q[RB.q.length-1];if(!r){el.classList.remove('on');return}const more=RB.q.length-1,sh=shById(r.cid);
 el.innerHTML=`<button class="rxb-main" data-act="rxOpen" data-id="${r.cid}"><span class="rxb-e">${esc(r.e||'💬')}</span><span class="rxb-t"><b>${esc(r.from)}</b><span>${r.m?esc(r.m):'sent you '+esc(r.e)}</span><small>${sh?'on “'+esc(sh.title)+'”':''}${more?` · +${more} more`:''}</small></span></button><button class="btn sm pri" data-act="shReact" data-id="${r.cid}" data-to="${esc(r.fe)}" data-nm="${esc(r.from)}">React</button><button class="ibtn sm rxb-x" data-act="rxClose" aria-label="Dismiss">✕</button>`;
 el.classList.remove('on');void el.offsetWidth;el.classList.add('on');RB.tm=setTimeout(()=>{RB.q=[];reactShow()},9000)}
function reactBurst(e){if(reduced&&reduced())return;const w=document.createElement('div');w.className='rx-burst';w.setAttribute('aria-hidden','true');
 w.innerHTML=[...Array(7)].map((_,i)=>`<span style="--x:${Math.round((Math.random()-.5)*220)}px;--d:${i*70}ms;--s:${(.8+Math.random()*.7).toFixed(2)}">${esc(e)}</span>`).join('');document.body.appendChild(w);setTimeout(()=>w.remove(),2200)}
/* in the group view: recent reactions both ways, newest first */
function rxFeed(sh,ms){const mine=(ms.find(m=>m.mine)||{}).cheers||[],nm=e=>{const m=ms.find(x=>x.email===e);return m?m.name:(e||'').split('@')[0]};
 shCollect(sh,ms);const L=[...reactsForMe(sh.id).map(r=>({...r,dir:'in'})),...mine.filter(c=>c&&c.t).map(c=>({e:c.emoji,m:c.m,t:+c.t,to:nm(c.to),dir:'out'}))].sort((a,b)=>b.t-a.t).slice(0,6);
 if(!L.length)return`<p class="small muted rx-none">No reactions yet. Tap <b>React</b> next to someone to cheer them on.</p>`;
 return`<div class="rx-feed"><div class="data">Reactions · live</div>${L.map(r=>`<div class="rx-i ${r.dir}"><span class="rx-ie">${esc(r.e||'💬')}</span><span class="rx-it"><b>${r.dir==='in'?esc(r.from)+' → you':'You → '+esc(r.to)}</b>${r.m?`<span>${esc(r.m)}</span>`:''}</span><small>${esc(dayLabel(r.t))} ${new Date(r.t).toLocaleTimeString([],{hour:'numeric',minute:'2-digit'})}</small></div>`).join('')}</div>`}
/* Today: unread reactions get a row next to invites */
function rxTodayRows(){const rx=shset().rx||{};return shares().filter(x=>x.status==='joined'&&(rx[x.id]||{}).n).slice(0,2).map(x=>{const r=reactsForMe(x.id).slice(-1)[0],n=rx[x.id].n;
 return`<div class="sh-inv rx-row"><span class="sh-ava rx-ava">${esc(r&&r.e||'💬')}</span><div><b>${r?esc(r.from)+(r.m?': '+esc(r.m):' sent you '+esc(r.e)):n+' new reaction'+(n===1?'':'s')}</b><small>${n>1?n+' new · ':''}on “${esc(x.title)}”</small></div><div class="sh-inv-a"><button class="btn sm pri" data-act="shOpen" data-id="${x.id}">Open</button></div></div>`}).join('')}
/* the picker */
let RXT=null;
function shReactSheet(sh,to,name){RXT={id:sh.id,to,nm:name};const left=Math.max(0,REACT_MAX-reactsSentToday()),dis=left?'':'disabled';
 openSheet(`<div class="data">${MODES[sh.mode]?.e||''} ${esc(sh.title)}</div><h2 style="margin-top:6px">React to ${esc(name)}</h2>
 <div class="rx-emo">${REACT_EMO.map(e=>`<button class="rx-b" data-act="shReactGo" data-e="${e}" ${dis} aria-label="Send ${e}">${e}</button>`).join('')}</div>
 <div class="data" style="margin-top:16px">Quick messages</div><div class="rx-msgs">${quickMsgs().map((m,i)=>`<button class="rx-m" data-act="shReactGo" data-i="${i}" ${dis}>${esc(m)}</button>`).join('')}</div>
 <p class="small muted rx-left">${left?`${left} of ${REACT_MAX} left today`:`That’s ${REACT_MAX} for today. Reactions open again tomorrow.`}</p>
 <div class="actions"><button class="btn ghost" data-act="shQuickEdit" data-back="1">Edit messages</button><button class="btn pri" data-act="shOpen" data-id="${sh.id}">Back to the group</button></div>`)}
function shQuickForm(back){const q=quickMsgs();openSheet(`<h2>Quick messages</h2><p class="small muted">Short messages you can send with one tap in a shared item. Up to 8, 40 characters each. There’s no free chat, so it stays a nudge.</p>
 <form data-form="shQuick" data-back="${back?1:''}"><div class="rx-qf">${[...Array(8)].map((_,i)=>`<input name="q${i}" maxlength="40" value="${esc(q[i]||'')}" placeholder="${i<REACT_MSG.length?esc(REACT_MSG[i]):'Add one'}" autocomplete="off" aria-label="Quick message ${i+1}">`).join('')}</div>
 <div class="actions"><button type="button" class="btn ghost" data-act="shQuickReset">Reset to defaults</button><button class="btn pri">Save</button></div></form>`)}
async function shPollReacts(){if(!shOn()||SHC.busy||SHC.rb||TOURING)return;const L=shares().filter(x=>x.fs&&x.k&&x.status==='joined');if(!L.length)return;SHC.rb=true;
 try{for(const sh of L){if(openGroup()===sh.id)continue;shCollect(sh,await fsMembers(sh))}}catch(e){}finally{SHC.rb=false}}
setInterval(()=>{if(document.visibilityState==='visible')shPollReacts()},20000);
document.addEventListener('visibilitychange',()=>{if(document.visibilityState==='visible')setTimeout(shPollReacts,1500)});
Object.assign(ACT,{
 shReact:d=>{const x=shById(d.id);if(!x||x.status!=='joined')return;if(RB.q.length){RB.q=[];reactShow()}shReactSheet(x,d.to,d.nm||(d.to||'').split('@')[0])},
 shReactGo:d=>{const x=RXT&&shById(RXT.id);if(!x)return;if(reactsSentToday()>=REACT_MAX)return toast(`That’s ${REACT_MAX} reactions for today`);
  const m=d.i!=null?String(quickMsgs()[+d.i]||'').slice(0,60):'',r={id:uid(),to:RXT.to,emoji:d.e||'',m,t:Date.now()};if(!r.emoji&&!r.m)return;
  x.cheerOut=[...(x.cheerOut||[]),r];x.u=Date.now();save();reactBurst(r.emoji||'💬');
  shPublish(x).then(()=>{toast(`Sent to ${esc(RXT.nm)} ${r.emoji?r.emoji:'· “'+esc(r.m)+'”'}`);shOpen(x)}).catch(e=>toast(shErr(e)))},
 shQuickEdit:d=>shQuickForm(!!(d.back&&RXT)),
 shQuickReset:()=>{shPatch({quick:null});const f=document.querySelector('form[data-form=shQuick]');shQuickForm(f&&!!f.dataset.back);toast('Back to the defaults')},
 rxOpen:d=>{RB.q=[];reactShow();const x=shById(d.id);if(x)shOpen(x)},
 rxClose:()=>{RB.q=[];reactShow()}});
Object.assign(FORM,{shQuick:f=>{const fd=new FormData(f),q=[...new Set([...Array(8)].map((_,i)=>String(fd.get('q'+i)||'').replace(/\s+/g,' ').trim().slice(0,40)).filter(Boolean))];
 shPatch({quick:q.length?q:null});toast('Quick messages saved');const x=f.dataset.back&&RXT&&shById(RXT.id);if(x)shReactSheet(x,RXT.to,RXT.nm);else{closeSheet();render(false)}}});
