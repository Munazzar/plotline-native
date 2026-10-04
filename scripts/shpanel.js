/* ---- shared item panel (1.8.4): right under the habit / goal hero, who it's shared with, how they're doing,
 and one-tap reactions. No need to find the small "Shared" chip and open the group first. */
const SHM={};// cid -> {ms, t}  last members read (also filled by the reaction poll)
const PANEL_EMO=['👏','🔥','💪','❤️'];
function shPanelFor(kind,id){return shares().find(x=>x.fs&&x.k&&x.status==='joined'&&x.local&&x.local.kind===kind&&x.local.id===id)}
function shPanelBody(x){const md=MODES[x.mode]||MODES.together,c=SHM[x.id],left=Math.max(0,REACT_MAX-reactsSentToday()),td=ymd();
 const head=`<div class="shp-h"><span class="shp-t">${md.e} <b>Shared</b> · ${esc(md.n)}</span><button class="btn sm ghost" data-act="shOpen" data-id="${x.id}">Group${ic('next','ico-s')}</button></div>`;
 if(!c)return head+`<p class="small muted">Loading who’s in it…</p>`;
 const others=c.ms.filter(m=>m.email!==fme()&&m.status==='joined');
 if(!others.length)return head+`<p class="small muted">${x.role==='owner'?'Nobody has joined yet. Once they do, you’ll see their progress here and can react with one tap.':'Waiting for the others.'}</p>`;
 const mine=reactsForMe(x.id);
 return head+others.map(m=>{const p=m.progress||{},kind=(x.local||{}).kind,last=mine.filter(r=>r.fe===m.email).slice(-1)[0],nm=m.name||m.email.split('@')[0];
  const st=kind==='habit'?`${p.checks&&p.checks[td]?'✓ done today':'not yet today'} · ${weekChecks(p)} this week`:`${p.pct||0}% done`;
  return`<div class="shp-m"><div class="shp-who"><span class="sh-ava">${esc(nm[0].toUpperCase())}</span><div><b>${esc(nm)}</b><small>${st}</small>${last?`<small class="shp-last">${esc(last.e||'💬')} ${last.m?esc(last.m)+' · ':''}${esc(dayLabel(last.t).toLowerCase())}</small>`:''}</div></div>
  <div class="shp-r" role="group" aria-label="React to ${esc(nm)}">${PANEL_EMO.map(e=>`<button class="shp-e" data-act="shQuick" data-id="${x.id}" data-to="${esc(m.email)}" data-nm="${esc(nm)}" data-e="${e}" ${left?'':'disabled'} aria-label="Send ${e} to ${esc(nm)}">${e}</button>`).join('')}<button class="shp-e more" data-act="shReact" data-id="${x.id}" data-to="${esc(m.email)}" data-nm="${esc(nm)}" ${left?'':'disabled'} aria-label="More reactions and messages for ${esc(nm)}">💬<span>More</span></button></div></div>`}).join('')
  +`<p class="shp-f small muted">${left?`Tap to send · ${left} left today`:`That’s ${REACT_MAX} for today`}</p>`}
function shPanel(kind,id){const x=shPanelFor(kind,id);if(!x)return'';const c=SHM[x.id];if(!c||Date.now()-c.t>15000)setTimeout(()=>shPanelLoad(x),0);
 return`<section class="shp rv" data-shp="${x.id}">${shPanelBody(x)}</section>`}
async function shPanelLoad(x){if(SHM[x.id]&&SHM[x.id].busy)return;SHM[x.id]={...(SHM[x.id]||{}),busy:1};try{shCollect(x,await fsMembers(x))}catch(e){if(SHM[x.id])SHM[x.id].busy=0}}
function shPanelPaint(cid){const x=shById(cid);document.querySelectorAll(`[data-shp="${cid}"]`).forEach(el=>{el.innerHTML=shPanelBody(x)})}
const _shCollect=shCollect;shCollect=function(sh,ms){SHM[sh.id]={ms:ms||[],t:Date.now()};_shCollect(sh,ms);if(document.querySelector(`[data-shp="${sh.id}"]`)){shPanelPaint(sh.id);const rx=shset().rx||{};if(rx[sh.id]&&rx[sh.id].n){rx[sh.id]={...rx[sh.id],n:0};shPatch({rx:{...rx}})}}};
function shInject(html,kind,id){const p=shPanel(kind,id);if(!p)return html;const i=html.indexOf('class="hero');if(i<0)return html;const j=html.indexOf('</section>',i);return j<0?html:html.slice(0,j+10)+p+html.slice(j+10)}
const _vHabit=vHabit;vHabit=id=>shInject(_vHabit(id),'habit',id);
const _vGoal=vGoal;vGoal=id=>shInject(_vGoal(id),'goal',id);
async function reactSend(x,to,emoji,m){if(reactsSentToday()>=REACT_MAX){toast(`That’s ${REACT_MAX} reactions for today`);return false}
 const r={id:uid(),to,emoji:emoji||'',m:String(m||'').slice(0,60),t:Date.now()};if(!r.emoji&&!r.m)return false;
 x.cheerOut=[...(x.cheerOut||[]),r];x.u=Date.now();save();reactBurst(r.emoji||'💬');await shPublish(x);return r}
Object.assign(ACT,{shQuick:(d,el)=>{const x=shById(d.id);if(!x)return;if(el){el.classList.remove('sent');void el.offsetWidth;el.classList.add('sent')}
 reactSend(x,d.to,d.e,'').then(r=>{if(r){toast(`Sent ${r.emoji} to ${esc(d.nm||'')}`);shPanelPaint(x.id)}}).catch(e=>toast(shErr(e)))}});
