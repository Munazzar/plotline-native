/* ==== BEGIN 1.10 MODULES ====
 Journal week pages · Threads (journal-style) · Day page (everything on a date, editable) · Habits calendar · Activity.
 Source: scripts/v110.js + v110.css, merged by scripts/merge_110.py (idempotent). Edit here, then merge. */
IC.thread='<circle cx="6" cy="5" r="2"/><circle cx="18" cy="12" r="2"/><circle cx="6" cy="19" r="2"/><path d="M6 7c0 5 12 1 12 3M18 14c0 5-12 1-12 3"/>';
IC.send='<path d="M22 2 11 13"/><path d="M22 2l-7 20-4-9-9-4z"/>';
IC.chev='<path d="m6 9 6 6 6-6"/>';
IC.vexpand='<path d="M7 9l5-5 5 5"/><path d="M7 15l5 5 5-5"/>';IC.vcollapse='<path d="M7 4l5 5 5-5"/><path d="M7 20l5-5 5 5"/>';
const DEFC='--c:var(--mo,var(--accent));--c2:var(--c-learning);--c3:var(--c-career)';
const dOf=t=>ymd(new Date(t));
const fmtMin=m=>{m=Math.round(m||0);if(m<60)return m+'m';const h=Math.floor(m/60),r=m%60;return r?`${h}h ${String(r).padStart(2,'0')}m`:h+'h'};
const tShort=t=>new Date(t).toLocaleTimeString([],{hour:'numeric',minute:'2-digit'});
const dLong=ds=>new Date(ds+'T12:00').toLocaleDateString(undefined,{weekday:'long',month:'long',day:'numeric',year:'numeric'});
function swipe(el,fn){if(!el||el._sw)return;el._sw=1;let x0=null,y0=0;el.addEventListener('pointerdown',e=>{x0=e.clientX;y0=e.clientY},{passive:true});
 el.addEventListener('pointerup',e=>{if(x0==null)return;const dx=e.clientX-x0,dy=e.clientY-y0;x0=null;if(Math.abs(dx)>55&&Math.abs(dx)>Math.abs(dy)*1.4)fn(dx<0?1:-1)},{passive:true})}

/* ================= JOURNAL: 7 days at a time, newest first ================= */
let JW=0;const JW_SLIDE={d:0};
const jwRange=()=>{const b=dnum(ymd())-7*JW;return{a:b-6,b}};
const jwIn=(t,R)=>{const k=dnum(dOf(t));return k>=R.a&&k<=R.b};
function jwBar(R,es){const td=ymd(),cells=[];for(let k=R.a;k<=R.b;k++){const ds=fromN(k),dayE=S.entries.filter(e=>dOf(e.t)===ds),n=es.filter(e=>dOf(e.t)===ds).length,md=dayE.filter(e=>e.mood).sort((a,b)=>b.t-a.t)[0];
  cells.push(`<button class="jw-d${n?' has':''}${ds===td?' today':''}" data-act="jwDay" data-d="${ds}" ${n?'':'disabled'} aria-label="${esc(dayName(ds))}"><span>${new Date(ds+'T12:00').toLocaleDateString(undefined,{weekday:'narrow'})}</span><b>${+ds.slice(8)}</b><i>${md?md.mood:n?'<em></em>':''}</i></button>`)}
 const f=d=>new Date(fromN(d)+'T12:00').toLocaleDateString(undefined,{month:'short',day:'numeric'});
 return`<div class="jw rv" id="jwBar"><button class="ibtn sm" data-act="jwGo" data-d="1" aria-label="Earlier 7 days">${ic('back')}</button><div class="jw-days">${cells.join('')}</div><button class="ibtn sm" data-act="jwGo" data-d="-1" aria-label="Later 7 days" ${JW?'':'disabled'}>${ic('next')}</button><label class="ibtn sm jdate" aria-label="Jump to a date" title="Jump to a date">${ic('cal')}<input type="date" data-jwdate max="${td}"></label></div>${JW?`<div class="jw-cap data">${f(R.a)} – ${f(R.b)} · <button class="link small" data-act="jwGo" data-d="${-JW}">Back to this week</button></div>`:''}`}
function jwCards(es){if(S.settings.layout.journal==='h')return hsShell('journal-w',es.map(e=>{const g=G(e.goalId),hb=!g&&entryHabit(e);return hsItem(g?cvar(g):hb?cvar(hb):DEFC,journalBig(e),'done jd-'+dOf(e.t),`<span class="node static" aria-hidden="true"></span>`,dayLabel(e.t))}).join(''),0);
 let last='',html='',i=0;es.forEach(e=>{const d=dOf(e.t);if(d!==last){html+=`<div class="sp-date rv" data-jday="${d}"><span class="data">${dayLabel(e.t)}</span></div>`;last=d}const g=G(e.goalId),h=entryHabit(e);html+=`<div class="sp-item ${i++%2?'r':'l'} rv" data-jt="${e.id}" style="${g?cvar(g):h?cvar(h):DEFC}"><div class="sp-card">${journalCard(e)}</div><span class="jdot"></span></div>`});
 return`<div class="spine" id="jspine" data-zk="journal" style="${zoomCss('journal')}">${html}</div>`}
function jSwitchRow(on){return`<div class="jbar rv">${segHTML('jv','jvGo',[['journal','Journal'],['threads','Threads']],on)}${iconSeg('journal',[['h','horz','Cards side by side'],['v','vert','Timeline']],S.settings.layout.journal)}</div>`}
function vJournalW(){const jf=jFilter(),R=jwRange(),es=S.entries.filter(e=>jPass(e,jf)&&jwIn(e.t,R)).sort((a,b)=>b.t-a.t);
 const head=`<header class="ph"><div><h1>Journal</h1></div><div class="ph-r"><button class="ibtn" data-act="yearOpen" aria-label="Your year in review" title="Your year">${ic('ai')}</button><button class="btn pri" data-act="addEntry" aria-label="Write an entry">${ic('edit')}<span>Write</span></button>${gear()}</div></header>`;
 const chips=`<div class="jchips rv">${segHTML('jf','jFilt',JFILT,jf)}</div>`;
 let body;if(es.length)body=`<div class="jw-cards${JW_SLIDE.d?' in-'+(JW_SLIDE.d>0?'r':'l'):''}">${jwCards(es)}</div>`;
 else{const before=S.entries.filter(e=>jPass(e,jf)&&dnum(dOf(e.t))<R.a).sort((a,b)=>b.t-a.t)[0];
  body=S.entries.length?`<div class="empty rv"><p>Nothing in these 7 days${jf!=='all'?' for this filter':''}.</p><div class="actions">${before?`<button class="btn" data-act="jwTo" data-d="${dOf(before.t)}">Go to ${esc(dayName(dOf(before.t)))}</button>`:''}${JW?`<button class="btn" data-act="jwGo" data-d="${-JW}">Back to this week</button>`:`<button class="btn pri" data-act="addEntry">${ic('edit')}Write</button>`}</div></div>`
  :`<div class="empty rv"><h3>Your story starts here</h3><p>Goals you set and reach, notes and photos land on this line.</p><div class="actions"><button class="btn pri" data-act="addEntry">${ic('camera')}Add a moment</button></div></div>`}
 JW_SLIDE.d=0;return head+jSwitchRow('journal')+jwBar(R,es)+chips+body}
function jwScrollTo(ds){const el=document.querySelector(`[data-jday="${ds}"]`);if(el){document.querySelectorAll('#view .rv').forEach(x=>x.classList.add('in'));el.scrollIntoView({behavior:reduced()?'auto':'smooth',block:'start'});return}
 const it=document.querySelector('.hs-item.jd-'+ds),hs=it&&it.closest('.hs');if(!it)return;hs.scrollTo({left:it.offsetLeft+it.offsetWidth/2-hs.clientWidth/2,behavior:reduced()?'auto':'smooth'});it.classList.remove('jhit');void it.offsetWidth;it.classList.add('jhit');setTimeout(()=>it.classList.remove('jhit'),1800)}
Object.assign(ACT,{
 jwGo:d=>{const n=Math.max(0,JW+ +d.d);if(n===JW)return;JW_SLIDE.d=+d.d;JW=n;render(false)},
 jwDay:d=>jwScrollTo(d.d),
 jwTo:d=>{JW=Math.max(0,Math.floor((dnum(ymd())-dnum(d.d))/7));render(false);setTimeout(()=>jwScrollTo(d.d),120)},
 jvGo:d=>go(d.v==='threads'?'threads':'journal')});
document.addEventListener('change',e=>{const t=e.target;if(!t.matches)return;if(t.matches('[data-jwdate]')&&t.value){ACT.jwTo({d:t.value});t.value=''}if(t.matches('[data-dydate]')&&t.value&&t.value<=ymd())go('day/'+t.value)});

/* ================= THREADS: running logs, shown like the journal ================= */
const THR_TAGS=[['note','💭','Thought'],['idea','💡','Idea'],['prog','🚧','Progress'],['block','⛔','Blocked'],['done','✅','Done']];
const thrTag=k=>THR_TAGS.find(x=>x[0]===k)||THR_TAGS[0];
const threads=()=>S.threads||(S.threads=[]);
const TH=id=>threads().find(t=>t.id===id);
const thrUps=t=>(t.ups||[]).filter(x=>!x.del).sort((a,b)=>b.t-a.t);
const thrLast=t=>{const u=thrUps(t)[0];return u?u.t:t.created||0};
function thrAgo(t){const m=Math.round((Date.now()-t)/6e4);if(m<1)return'just now';if(m<60)return m+' min ago';const h=Math.round(m/60);if(h<24)return h+' h ago';const d=Math.round(h/24);if(d<7)return d===1?'yesterday':d+' days ago';return new Date(t).toLocaleDateString(undefined,{month:'short',day:'numeric'})}
function thrLink(t){const l=t.link;if(!l)return null;
 if(l.k==='goal'){const g=G(l.id);return g?{e:'🎯',n:g.title,go:'goal/'+g.id,c:cvar(g)}:{e:'🎯',n:'Goal removed',gone:1}}
 if(l.k==='habit'){const h=H(l.id);return h?{e:hIcon(h),n:h.title,go:'habit/'+h.id,c:cvar(h)}:{e:'✅',n:'Habit removed',gone:1}}
 if(l.k==='entry'){const e=S.entries.find(x=>x.id===l.id);return e?{e:'📓',n:e.title||trunc((e.text||'Journal entry').split('\n')[0],40),entry:e.id}:{e:'📓',n:'Entry removed',gone:1}}
 return null}
const thrFor=(k,id)=>threads().filter(t=>t.link&&t.link.k===k&&t.link.id===id);
const thrCol=t=>{const l=thrLink(t);return l&&l.c?l.c:DEFC};
function thrBtn(k,id){const n=thrFor(k,id).filter(t=>t.status!=='done').length;return`<button class="ibtn thr-b${n?' on':''}" data-act="thrFrom" data-k="${k}" data-id="${id}" aria-label="Threads${n?' ('+n+')':''}" title="Threads">${ic('thread')}${n?`<sup>${n}</sup>`:''}</button>`}
const thrText=x=>esc(x).replace(/\n/g,'<br>');
function thrTags(sel,mini){return`<div class="thr-tags${mini?' tg-s':''}" role="radiogroup" aria-label="Kind of update"><input type="hidden" name="k" value="${sel}">${THR_TAGS.map(([k,e,n])=>`<button type="button" class="chip${k===sel?' on':''}" role="radio" aria-checked="${k===sel}" data-act="thrTag" data-v="${k}" title="${n}">${e}${mini?'':' '+n}</button>`).join('')}</div>`}
function thrLinkSel(cur){const gs=active(),hs=S.habits.filter(h=>h.status!=='archived');const v=cur?cur.k+':'+cur.id:'';
 return`<select name="link" aria-label="Link to"><option value="">Not linked · stands on its own</option>${gs.length?`<optgroup label="Goals">${gs.map(g=>`<option value="goal:${g.id}" ${v==='goal:'+g.id?'selected':''}>🎯 ${esc(trunc(g.title,48))}</option>`).join('')}</optgroup>`:''}${hs.length?`<optgroup label="Habits">${hs.map(h=>`<option value="habit:${h.id}" ${v==='habit:'+h.id?'selected':''}>${esc(hIcon(h))} ${esc(trunc(h.title,48))}</option>`).join('')}</optgroup>`:''}${cur&&cur.k==='entry'?`<option value="entry:${cur.id}" selected>📓 This journal entry</option>`:''}</select>`}
const thrPick=v=>{if(!v)return null;const i=v.indexOf(':');return{k:v.slice(0,i),id:v.slice(i+1)}};
/* cards, built from the journal's own card styles */
function thrBig(t){const u=thrUps(t)[0],tg=u?thrTag(u.k):null,n=thrUps(t).length,lk=thrLink(t);
 return`<div class="jb tint thr-jb${t.status==='done'?' fin':''}" data-act="thrOpen" data-id="${t.id}" role="button" tabindex="0" aria-label="Open thread ${esc(t.title)}"><div class="jh"><span class="data">${t.status==='done'?'✅ Done':tg?tg[1]+' '+tg[2]:'🧵 New'} · ${n} update${n===1?'':'s'}</span>${t.status==='done'?'':`<button class="x" data-act="thrAddSheet" data-id="${t.id}" aria-label="Add an update">${ic('plus')}</button>`}</div><div class="data" style="margin-top:4px">${thrAgo(thrLast(t))}</div><div class="jb-big">${esc(t.title)}</div>${u?`<div class="jb-q thr-clamp">${esc(trunc(u.x,180))}</div>`:'<div class="jb-q" style="opacity:.65">Nothing logged yet</div>'}${lk?`<span class="jg">${esc(lk.e)} ${esc(trunc(lk.n,34))}</span>`:''}</div>`}
function thrFlip(t){const U=thrUps(t),u=U[0],tg=u?thrTag(u.k):null,lk=thrLink(t),when=thrAgo(thrLast(t));
 return`<div class="jf" style="${thrCol(t)}" data-act="flip" tabindex="0" role="button" aria-label="${esc(t.title)}. Show latest updates"><div class="jf-in"><div class="jf-face jf-front tint"><div class="sc-top"><span class="data">${tg?tg[1]+' ':''}Thread · ${when}</span><span class="flip-hint">${ic('flipi')}</span></div><div class="jt-big">${esc(t.title)}</div><div class="data" style="margin-top:10px">${U.length} update${U.length===1?'':'s'}${lk?' · '+esc(trunc(lk.n,28)):''}</div></div>
 <div class="jf-face jf-back tint"><div class="data">${when}</div><h4>${esc(t.title)}</h4>${U.slice(0,3).map(x=>`<p class="note thr-ln">${thrTag(x.k)[1]} ${esc(trunc(x.x,90))}</p>`).join('')||'<p class="note" style="opacity:.7">Nothing logged yet.</p>'}<div class="sc-act"><button class="btn inkline sm" data-act="thrOpen" data-id="${t.id}">${ic('eye')}<span>Open</span></button>${t.status==='done'?'':`<button class="btn inkline sm" data-act="thrAddSheet" data-id="${t.id}">${ic('plus')}<span>Update</span></button>`}</div></div></div></div>`}
function thrSpine(L,dateOf,card,colOf){let last='',h='',i=0;L.forEach(x=>{const d=dOf(dateOf(x));if(d!==last){h+=`<div class="sp-date rv" data-jday="${d}"><span class="data">${dayLabel(dateOf(x))}</span></div>`;last=d}h+=`<div class="sp-item ${i++%2?'r':'l'} rv" style="${colOf(x)}"><div class="sp-card">${card(x)}</div><span class="jdot"></span></div>`});return`<div class="spine" data-zk="journal" style="${zoomCss('journal')}">${h}</div>`}
let THF='open';
function vThreads(){const all=threads().slice().sort((a,b)=>thrLast(b)-thrLast(a)),open=all.filter(t=>t.status!=='done'),L=THF==='done'?all.filter(t=>t.status==='done'):THF==='all'?all:open;
 const head=`<header class="ph"><div><h1>Journal</h1></div><div class="ph-r"><button class="btn pri" data-act="thrNew" aria-label="New thread">${ic('plus')}<span>New thread</span></button>${gear()}</div></header>`;
 const chips=all.length?`<div class="jchips rv">${segHTML('thf','thrFilt',[['open',`Active · ${open.length}`],['done','Done'],['all','All']],THF)}</div>`:'';
 const H0=S.settings.layout.journal==='h';
 const body=L.length?(H0?hsShell('threads-'+THF,L.map(t=>hsItem(thrCol(t),thrBig(t),'done',`<span class="node static" aria-hidden="true"></span>`,dayLabel(thrLast(t)))).join(''),0):thrSpine(L,thrLast,thrFlip,thrCol))
  :all.length?`<div class="empty rv"><p>Nothing here.</p><div class="actions"><button class="btn" data-act="thrFilt" data-v="all">Show all threads</button></div></div>`
  :`<div class="empty rv"><h3>Think out loud, one step at a time</h3><p>A thread is a running log for an idea, a project or anything you’re working through. Add a line whenever something changes. Link it to a goal or habit only if you want to.</p><div class="actions"><button class="btn pri" data-act="thrNew">${ic('plus')}Start a thread</button></div></div>`;
 return head+jSwitchRow('threads')+chips+body}
function updBig(t,u){const g=thrTag(u.k);return`<div class="jb tint thr-ub" data-k="${g[0]}"><div class="jh"><span class="data">${g[1]} ${g[2]} · ${tShort(u.t)}</span><button class="x" data-act="thrUpEdit" data-id="${t.id}" data-u="${u.id}" aria-label="Edit update">${ic('edit')}</button></div><div class="data" style="margin-top:4px">${dayLabel(u.t)}</div><div class="thr-utext">${thrText(u.x)}</div></div>`}
function updCard(t,u){const g=thrTag(u.k);return`<div class="jc thr-uc" data-k="${g[0]}"><div class="jh"><span class="data">${g[1]} ${g[2]} · ${tShort(u.t)}</span><button class="x" data-act="thrUpEdit" data-id="${t.id}" data-u="${u.id}" aria-label="Edit update">${ic('edit')}</button></div><div class="jt">${thrText(u.x)}</div></div>`}
function vThread(id){const t=TH(id);if(!t)return`<header class="ph"><div><h1>Not found</h1></div><div class="ph-r">${gear()}</div></header><a class="btn" href="#/threads">Back to threads</a>`;
 const lk=thrLink(t),U=thrUps(t),col=thrCol(t),H0=S.settings.layout.journal==='h';
 const top=`<div class="crumb"><a href="#/threads" class="ibtn" aria-label="Back to threads">${ic('back')}</a><div class="ph-r"><button class="ibtn" data-act="thrEdit" data-id="${t.id}" aria-label="Edit thread" title="Edit">${ic('edit')}</button>${t.status==='done'?`<button class="ibtn" data-act="thrReopen" data-id="${t.id}" aria-label="Reopen" title="Reopen">${ic('rep')}</button>`:`<button class="ibtn" data-act="thrDone" data-id="${t.id}" aria-label="Mark done" title="Mark done">${ic('check')}</button>`}${gear()}</div></div>
 <header class="ph thr-ph" style="${col}"><div><h1>${esc(t.title)}</h1><div class="data">Thread · started ${new Date(t.created).toLocaleDateString(undefined,{month:'short',day:'numeric',year:'numeric'})} · ${U.length} update${U.length===1?'':'s'}${t.status==='done'?' · done':''}</div>${lk?`<button class="thr-lk" data-act="thrLinkGo" data-id="${t.id}" ${lk.gone?'disabled':''}>${esc(lk.e)} ${esc(trunc(lk.n,40))}${lk.gone?'':ic('next','ico-s')}</button>`:''}</div></header>`;
 const comp=t.status==='done'?'':`<form class="thr-comp rv" data-form="thrAdd" data-id="${t.id}" autocomplete="off">${thrTags('note',true)}<div class="thr-in"><textarea name="x" rows="1" maxlength="2000" placeholder="Add an update…" required aria-label="Add an update"></textarea><button class="btn pri" type="submit" aria-label="Add update">${ic('send')}</button></div></form>`;
 const bar=`<div class="jbar rv"><span class="data">Latest first</span>${iconSeg('journal',[['h','horz','Cards side by side'],['v','vert','Timeline']],S.settings.layout.journal)}</div>`;
 const body=U.length?(H0?hsShell('thread-'+t.id,U.map(u=>hsItem(col,updBig(t,u),'done jd-'+dOf(u.t),`<span class="node static" aria-hidden="true"></span>`,dayLabel(u.t))).join(''),0):thrSpine(U,u=>u.t,u=>updCard(t,u),()=>col))
  :`<div class="empty rv"><p>Add the first line above. Come back any time and keep building on it.</p></div>`;
 return top+comp+bar+body}
function thrNewSheet(link,ttl){openSheet(`<div class="data">Thread</div><h2 style="margin-top:6px">Start a thread</h2><p class="small muted">A running log. Add a first thought now and keep adding as it grows.</p><form data-form="thrNew" autocomplete="off"><div class="field"><label>Name it</label><input name="title" maxlength="80" required placeholder="App idea, Moving plans, Learning guitar…" value="${esc(ttl||'')}"></div><div class="field"><label>First update <span class="muted">(optional)</span></label>${thrTags('note')}<textarea name="x" rows="3" maxlength="2000" placeholder="What’s on your mind?" style="margin-top:10px"></textarea></div><div class="field"><label>Link it to</label>${thrLinkSel(link)}<p class="small muted" style="margin-top:6px">Optional. A linked thread also shows on that goal or habit.</p></div><div class="actions"><button class="btn ghost" type="button" data-act="close">Cancel</button><button class="btn pri" type="submit">Start thread</button></div></form>`);
 setTimeout(()=>{const i=$('#sheet input[name=title]');if(i&&!i.value)i.focus()},350)}
function thrAddSheet(id){const t=TH(id);if(!t)return;openSheet(`<div class="data">${esc(t.title)}</div><h2 style="margin-top:6px">Add an update</h2><form data-form="thrAdd" data-id="${t.id}" data-sheet="1" autocomplete="off">${thrTags('note')}<div class="field" style="margin-top:12px"><textarea name="x" rows="4" maxlength="2000" required placeholder="What’s new?"></textarea></div><div class="actions"><button class="btn ghost" type="button" data-act="close">Cancel</button><button class="btn pri" type="submit">Add update</button></div></form>`);setTimeout(()=>$('#sheet textarea')?.focus(),350)}
Object.assign(FORM,{
 thrNew:f=>{const E=f.elements,title=E.title.value.trim();if(!title)return;const now=Date.now(),t={id:uid(),title,link:thrPick(E.link&&E.link.value),status:'open',created:now,u:now,ups:[]};const x=E.x.value.trim();if(x)t.ups.push({id:uid(),t:now,k:E.k.value||'note',x,u:now});threads().push(t);save();closeSheet();go('thread/'+t.id);toast('Thread started')},
 thrAdd:f=>{const t=TH(f.dataset.id);if(!t)return;const x=f.elements.x.value.trim();if(!x)return;const now=Date.now();t.ups.push({id:uid(),t:now,k:f.elements.k.value||'note',x,u:now});if(t.status==='done')t.status='open';t.u=now;save();if(f.dataset.sheet)closeSheet();render(false);toast('Added')},
 thrEdit:f=>{const t=TH(f.dataset.id);if(!t)return;const E=f.elements,title=E.title.value.trim();if(!title)return;t.title=title;t.link=thrPick(E.link.value);t.u=Date.now();save();closeSheet();render(false);toast('Saved')},
 thrUpEdit:f=>{const t=TH(f.dataset.id),u=t&&t.ups.find(x=>x.id===f.dataset.u);if(!u)return;const x=f.elements.x.value.trim();if(!x)return;u.x=x;u.k=f.elements.k.value||u.k;u.u=Date.now();t.u=u.u;save();closeSheet();render(false);toast('Saved')}});
Object.assign(ACT,{
 thrFilt:d=>{THF=d.v;render(false)},
 thrOpen:d=>{closeSheet();go('thread/'+d.id)},
 thrNew:()=>thrNewSheet(null),
 thrAddSheet:d=>thrAddSheet(d.id),
 thrTag:(d,el)=>{const w=el.closest('.thr-tags');if(!w)return;w.querySelector('input').value=d.v;w.querySelectorAll('.chip').forEach(c=>{const on=c===el;c.classList.toggle('on',on);c.setAttribute('aria-checked',on)})},
 thrFrom:d=>{const L=thrFor(d.k,d.id),it=d.k==='goal'?G(d.id):d.k==='habit'?H(d.id):S.entries.find(x=>x.id===d.id);if(!it)return;
  const ttl=d.k==='entry'?trunc((it.title||(it.text||'').split('\n')[0]||'').trim(),60):it.title;
  if(!L.length)return thrNewSheet({k:d.k,id:d.id},ttl);
  openSheet(`<div class="data">Threads</div><h2 style="margin-top:6px">${esc(trunc(ttl||'This item',40))}</h2><div class="list" style="margin:14px 0">${L.sort((a,b)=>thrLast(b)-thrLast(a)).map(t=>{const u=thrUps(t)[0];return`<div class="li" data-act="thrOpen" data-id="${t.id}"><span class="thr-em">${u?thrTag(u.k)[1]:'🧵'}</span><div style="flex:1;min-width:0"><div class="t ell">${esc(t.title)}</div><div class="s"><span class="ell">${u?esc(u.x):'Nothing logged yet'}</span></div></div><div class="data">${thrAgo(thrLast(t))}</div></div>`}).join('')}</div><div class="actions"><button class="btn ghost" data-act="close">Close</button><button class="btn pri" data-act="thrNewFrom" data-k="${d.k}" data-id="${d.id}">${ic('plus')}New thread</button></div>`)},
 thrNewFrom:d=>{const it=d.k==='goal'?G(d.id):d.k==='habit'?H(d.id):S.entries.find(x=>x.id===d.id);closeSheet();setTimeout(()=>thrNewSheet({k:d.k,id:d.id},it?(it.title||trunc((it.text||'').split('\n')[0],60)):''),380)},
 thrLinkGo:d=>{const t=TH(d.id),l=t&&thrLink(t);if(!l||l.gone)return;if(l.entry)ACT.viewEntry({id:l.entry});else go(l.go)},
 thrEdit:d=>{const t=TH(d.id);if(!t)return;openSheet(`<div class="data">Thread</div><h2 style="margin-top:6px">Edit thread</h2><form data-form="thrEdit" data-id="${t.id}" autocomplete="off"><div class="field"><label>Name</label><input name="title" maxlength="80" required value="${esc(t.title)}"></div><div class="field"><label>Linked to</label>${thrLinkSel(t.link)}</div><div class="actions"><button class="btn danger" type="button" data-act="thrDel" data-id="${t.id}">Delete</button><button class="btn pri" type="submit">Save</button></div></form>`)},
 thrDel:d=>askConfirm('Delete this thread?','Its updates go with it. Your goals, habits and journal stay as they are.','Delete',()=>{S.threads=threads().filter(x=>x.id!==d.id);save();closeSheet();go('threads');toast('Thread deleted')}),
 thrDone:d=>{const t=TH(d.id);if(!t)return;t.status='done';t.u=Date.now();save();render(false);toast('Thread marked done')},
 thrReopen:d=>{const t=TH(d.id);if(!t)return;t.status='open';t.u=Date.now();save();render(false)},
 thrUpEdit:d=>{const t=TH(d.id),u=t&&t.ups.find(x=>x.id===d.u);if(!u)return;openSheet(`<div class="data">${new Date(u.t).toLocaleString(undefined,{weekday:'short',month:'short',day:'numeric',hour:'numeric',minute:'2-digit'})}</div><h2 style="margin-top:6px">Edit update</h2><form data-form="thrUpEdit" data-id="${t.id}" data-u="${u.id}" autocomplete="off">${thrTags(u.k)}<div class="field" style="margin-top:12px"><textarea name="x" rows="5" maxlength="2000" required>${esc(u.x)}</textarea></div><div class="actions"><button class="btn danger" type="button" data-act="thrUpDel" data-id="${t.id}" data-u="${u.id}">Delete</button><button class="btn pri" type="submit">Save</button></div></form>`)},
 thrUpDel:d=>{const t=TH(d.id),u=t&&t.ups.find(x=>x.id===d.u);if(!u)return;u.del=1;u.u=Date.now();t.u=u.u;save();closeSheet();render(false);toast('Update removed')}});
/* the quick box grows as you type; Enter sends on a keyboard, Shift+Enter is a new line */
document.addEventListener('input',e=>{const t=e.target;if(t.matches&&t.matches('.thr-in textarea')){t.style.height='auto';t.style.height=Math.min(160,t.scrollHeight)+'px'}});
document.addEventListener('keydown',e=>{const t=e.target;if(e.key==='Enter'&&!e.shiftKey&&t.matches&&t.matches('.thr-in textarea')&&matchMedia('(hover:hover)').matches){e.preventDefault();t.form.requestSubmit()}});
function thrToday(){const L=threads().filter(t=>t.status!=='done').sort((a,b)=>thrLast(b)-thrLast(a)).slice(0,3);
 const rows=L.length?L.map(t=>{const u=thrUps(t)[0];return`<div class="li thr-li" style="${thrCol(t)}" data-act="thrOpen" data-id="${t.id}"><span class="thr-em">${u?thrTag(u.k)[1]:'🧵'}</span><div style="flex:1;min-width:0"><div class="t ell">${esc(t.title)}</div><div class="s"><span class="ell">${u?esc(u.x):'Nothing logged yet'}</span></div></div><div class="data">${thrAgo(thrLast(t))}</div><button class="ibtn sm" data-act="thrAddSheet" data-id="${t.id}" aria-label="Add an update to ${esc(t.title)}">${ic('plus')}</button></div>`}).join('')
  :`<div class="li thr-li" data-act="thrNew"><span class="thr-em">🧵</span><div style="flex:1;min-width:0"><div class="t">Start a thread</div><div class="s"><span class="ell">Log a thought or a status, then build on it</span></div></div>${ic('plus','ico-s')}</div>`;
 return`<section class="rv"><div class="st-h"><h3>Threads</h3><span style="display:flex;gap:16px;align-items:center">${L.length?`<button class="link" data-act="thrNew">New</button>`:''}<a class="link" href="#/threads">All threads</a></span></div><div class="list">${rows}</div></section>`}

/* ================= DAY PAGE: everything on one date, and what can be changed is changeable ================= */
const DAYX=new Set();
const slipT=s=>typeof s==='object'&&s?+s.t:+s;
function dyHabits(ds){const k=dnum(ds);return S.habits.filter(h=>h.status!=='archived'&&(h.kind==='quit'?dnum(dOf(h.start||Date.now()))<=k:hDue(h,ds)||hVal(h,ds)>0||(h.skip&&h.skip[ds])))}
function dyNotes(ds){if(!NATIVE||!NATIVE.notifLog)return[];return nhist('*').filter(e=>e&&e.t&&dOf(e.t)===ds).sort((a,b)=>a.t-b.t)}
function dyReacts(ds,hid){const out=[];shares().forEach(sh=>{const L=sh.local||{};if(hid&&!(L.kind==='habit'&&L.id===hid))return;const it=typeof shItem==='function'?shItem(sh):null,nm=it?it.title:'shared item';
  [...(sh.myCheers||[]),...(sh.cheerOut||[])].forEach(c=>{if(c&&c.t&&dOf(c.t)===ds)out.push({t:c.t,me:1,e:c.emoji,m:c.m,who:c.to,item:nm})});
  (SHR[sh.id]||[]).forEach(c=>{if(c.t&&dOf(c.t)===ds)out.push({t:c.t,me:0,e:c.e,m:c.m,who:c.from,item:nm})})});return out.sort((a,b)=>a.t-b.t)}
const dyReactRow=r=>`<li class="dy-r${r.me?' me':''}"><span class="dy-re">${esc(r.e||'💬')}</span><span><b>${r.me?'You → '+esc(String(r.who||'').split('@')[0]):esc(r.who||'Someone')}</b>${r.m?` “${esc(r.m)}”`:''}<small>${esc(trunc(r.item,30))} · ${tShort(r.t)}</small></span></li>`;
function dyNoteRow(e){const kind=e.kind||'',txt=NH_TXT[kind]||kind,ttl=e.title||'';
 if(/^reply/.test(kind))return`<li class="dy-n me"><div class="dy-bub">${esc(e.text||'')}</div><small>${tShort(e.t)}${e.res?' · '+esc(e.res):''}</small></li>`;
 if(kind==='sent'||kind==='again'||kind==='held')return`<li class="dy-n"><div class="dy-bub"><b>${esc(ttl||txt)}</b>${e.text?'<br>'+esc(e.text):''}</div><small>${esc(txt)} · ${tShort(e.t)}</small></li>`;
 return`<li class="dy-n sys"><small>${esc(txt)}${ttl?' · '+esc(ttl):''}${e.text?' · '+esc(e.text):''}${e.res?' · '+esc(e.res):''} · ${tShort(e.t)}</small></li>`}
function dyHabitRow(h,ds,fut){const v=hVal(h,ds),n=hTarget(h),dn=hDone(h,ds),sk=h.skip&&h.skip[ds],pz=typeof hPaused==='function'&&hPaused(h,ds),open=DAYX.has(h.id);
 let st,ctl='';
 if(h.kind==='quit'&&h.mode==='limit'){const c=+h.log[ds]||0;st=`${c} of ${h.limit}${h.unit?' '+esc(h.unit):''}${c>h.limit?' · over':''}`;ctl=fut?'':`<button class="ibtn sm" data-act="dyLim" data-id="${h.id}" data-d="${ds}" data-n="-1" aria-label="One less">${ic('minus')}</button><button class="ibtn sm" data-act="dyLim" data-id="${h.id}" data-d="${ds}" data-n="1" aria-label="One more">${ic('plus')}</button>`}
 else if(h.kind==='quit'){const sl=(h.slips||[]).filter(s=>dOf(slipT(s))===ds);st=sl.length?`${sl.length} slip${sl.length>1?'s':''}`:'Clean';ctl=`<span class="dy-pill ${sl.length?'bad':'ok'}">${sl.length?'Slip':'Clean'}</span>`}
 else{st=pz?'Paused':sk?'Rest day':n>1?`${v} of ${n}${h.unit?' '+esc(h.unit):''}`:dn?'Done':'Not done';
  if(!fut&&!pz)ctl=(n>1&&h.kind!=='routine'?`<button class="ibtn sm" data-act="dyAdj" data-id="${h.id}" data-d="${ds}" data-n="-1" aria-label="One less">${ic('minus')}</button><button class="ibtn sm" data-act="dyAdj" data-id="${h.id}" data-d="${ds}" data-n="1" aria-label="One more">${ic('plus')}</button>`:'')+`<button class="hh-k${dn?' on':''}" data-act="dyTog" data-id="${h.id}" data-d="${ds}" aria-label="${dn?'Undo':'Mark done'}: ${esc(h.title)}">${ic('check')}</button>`}
 const notes=dyNotes(ds).filter(e=>e.key==='h:'+h.id||(e.also||[]).includes('h:'+h.id)),reacts=dyReacts(ds,h.id),ents=S.entries.filter(e=>dOf(e.t)===ds&&entryHabit(e)===h),tu=threads().filter(t=>t.link&&t.link.k==='habit'&&t.link.id===h.id).flatMap(t=>thrUps(t).filter(u=>dOf(u.t)===ds).map(u=>({t,u})));
  let body='';
 if(h.kind==='routine'&&h.steps.length)body+=`<div class="clist">${h.steps.map(s=>`<label class="citem" style="${cvar(h)}"><input type="checkbox" data-hstep="${s.id}" data-id="${h.id}" data-d="${ds}" ${(h.rs[ds]||[]).includes(s.id)?'checked':''} ${fut?'disabled':''}><i></i><span>${esc(s.title)}</span><span class="data">${s.min?s.min+' min':''}</span></label>`).join('')}</div>`;
 if(h.kind==='quit'&&h.mode!=='limit'){const sl=(h.slips||[]).filter(s=>dOf(slipT(s))===ds),ur=(h.urges||[]).filter(u=>u&&u.t&&dOf(u.t)===ds);
  body+=`<div class="dy-sub">Slips</div>${sl.length?`<ul class="dy-ul">${sl.map(s=>`<li><span><b>${tShort(slipT(s))}</b>${s.trig?' · '+esc(s.trig):''}${s.note?` · ${esc(s.note)}`:''}</span><button class="ibtn sm" data-act="dySlipDel" data-id="${h.id}" data-t="${slipT(s)}" aria-label="Remove this slip">${ic('trash')}</button></li>`).join('')}</ul>`:'<p class="small muted">No slips. A clean day.</p>'}${fut?'':`<form class="dy-add" data-form="dySlip" data-id="${h.id}" data-d="${ds}"><input type="time" name="tm" value="12:00" aria-label="Time"><input name="note" maxlength="80" placeholder="What set it off? (optional)"><button class="btn sm" type="submit">Log a slip</button></form>`}
  ${ur.length?`<div class="dy-sub">Urges</div><ul class="dy-ul">${ur.map(u=>`<li><span><b>${tShort(u.t)}</b> · ${u.ok?'rode it out':'gave in'}</span></li>`).join('')}</ul>`:''}`}
 if(h.kind!=='quit'&&!fut&&!pz)body+=`<div class="dy-acts"><button class="btn sm" data-act="hSkip" data-id="${h.id}" data-d="${ds}">${sk?'Remove rest day':'Make it a rest day'}</button><a class="btn sm" href="#/habit/${h.id}">Open habit</a></div>`;
 else body+=`<div class="dy-acts"><a class="btn sm" href="#/habit/${h.id}">Open habit</a></div>`;
 if(notes.length)body+=`<div class="dy-sub">Reminders and replies</div><ol class="dy-chat">${notes.map(dyNoteRow).join('')}</ol>`;
 if(reacts.length)body+=`<div class="dy-sub">Cheers</div><ul class="dy-rl">${reacts.map(dyReactRow).join('')}</ul>`;
 if(ents.length)body+=`<div class="dy-sub">Journal</div>${ents.map(e=>`<button class="dy-ent" data-act="viewEntry" data-id="${e.id}">${e.mood?e.mood+' ':''}${esc(trunc(e.title||e.text||'Entry',80))}<small>${tShort(e.t)}</small></button>`).join('')}`;
 if(tu.length)body+=`<div class="dy-sub">Thread updates</div>${tu.map(({t,u})=>`<button class="dy-ent" data-act="thrOpen" data-id="${t.id}">${thrTag(u.k)[1]} ${esc(trunc(u.x,80))}<small>${esc(trunc(t.title,30))} · ${tShort(u.t)}</small></button>`).join('')}`;
 return`<div class="dy-h${open?' open':''}${dn?' d':''}" style="${cvar(h)}" data-hid="${h.id}"><div class="dy-hr"><button class="dy-hx" data-act="dyX" data-id="${h.id}" aria-expanded="${open}"><span class="hh-i">${esc(hIcon(h))}</span><span class="hh-t"><b>${esc(h.title)}</b><small>${st}${notes.length?` · 🔔 ${notes.length}`:''}${reacts.length?` · 👏 ${reacts.length}`:''}${ents.length?` · 📓 ${ents.length}`:''}${tu.length?` · 🧵 ${tu.length}`:''}</small></span>${ic('chev','dy-chev')}</button>${ctl}</div><div class="dy-hb"><div>${body}</div></div></div>`}
function vDay(ds){ds=validDate(ds)||ymd();const td=ymd(),fut=ds>td,k=dnum(ds),rel=k-dnum(td);
 const top=`<div class="crumb dy-crumb"><button class="ibtn" data-act="dyBack" aria-label="Back">${ic('back')}</button><div class="ph-r"><button class="ibtn" data-act="dyGo" data-d="-1" aria-label="Day before">${ic('back')}</button><label class="ibtn jdate" aria-label="Pick a date" title="Pick a date">${ic('cal')}<input type="date" data-dydate max="${td}" value="${ds}"></label><button class="ibtn" data-act="dyGo" data-d="1" aria-label="Day after" ${ds>=td?'disabled':''}>${ic('next')}</button>${gear()}</div></div>`;
 const head=`<header class="ph dy-head"><div><h1>${rel===0?'Today':rel===-1?'Yesterday':new Date(ds+'T12:00').toLocaleDateString(undefined,{weekday:'long'})}</h1><div class="data">${esc(dLong(ds))}${rel<-1?` · ${-rel} days ago`:''}</div></div></header>`;
 const HS=dyHabits(ds),r=dayRatio(ds),A=(S.settings.actLog||{})[ds]||null,ents=S.entries.filter(e=>dOf(e.t)===ds).sort((a,b)=>a.t-b.t),moods=ents.filter(e=>e.mood),dg=S.days.filter(x=>x.date===ds).sort(byTime),notes=dyNotes(ds),reacts=dyReacts(ds),tu=threads().flatMap(t=>thrUps(t).filter(u=>dOf(u.t)===ds).map(u=>({t,u})));
 const stepsDone=ents.filter(e=>e.type==='step'),due=active().flatMap(g=>g.steps.filter(s=>s.due===ds&&!s.done).map(s=>({g,s})));
 const chip=(e,v,l,to)=>`<button class="dy-c" data-act="dyJump" data-to="${to.slice(1)}"><span>${e}</span><b>${v}</b><small>${l}</small></button>`;
 const sum=`<div class="dy-sum rv">${r.due?chip('✅',`${r.dn}/${r.due}`,'habits','#dyHab'):''}${A&&A.s?chip('👟',(+A.s).toLocaleString(),'steps','#dyAct'):''}${A&&A.z?chip('😴',fmtMin(A.z),'sleep','#dyAct'):''}${A&&wkMin(A)?chip('🏋️',fmtMin(wkMin(A)),'workout','#dyAct'):''}${A&&A.m?chip('📱',fmtMin(A.m),'screen','#dyAct'):''}${moods.length?chip(moods[moods.length-1].mood,moods.length,'mood','#dyJ'):''}${ents.length?chip('📓',ents.length,'entries','#dyJ'):''}${notes.length?chip('🔔',notes.length,'reminders','#dyN'):''}${reacts.length?chip('👏',reacts.length,'cheers','#dyN'):''}${dg.length?chip('🎯',`${dg.filter(x=>x.done).length}/${dg.length}`,'day goals','#dyG'):''}</div>`;
 let o=top+head+sum;
 o+=`<section class="rv dy-sec" id="dyHab"><div class="st-h"><h3>Habits</h3><span class="data">${r.due?`${r.dn} of ${r.due} kept`:''}</span></div>${HS.length?`<div class="dy-hl">${HS.map(h=>dyHabitRow(h,ds,fut)).join('')}</div>`:'<p class="small muted">No habits were due on this day.</p>'}${fut?'<p class="small muted" style="margin-top:8px">This day hasn’t happened yet.</p>':''}</section>`;
 if(NATIVE&&NATIVE.activityLoad||A)o+=`<section class="rv dy-sec" id="dyAct"><div class="st-h"><h3>Activity</h3><a class="link" href="#/activity">All activity</a></div>${A?actDayHTML(ds,A):'<p class="small muted">Nothing was recorded on the phone for this day.</p>'}</section>`;
 if(dg.length||!fut)o+=`<section class="rv dy-sec" id="dyG"><div class="st-h"><h3>Goals for the day</h3></div>${dg.length?`<div class="list">${dg.map(x=>`<div class="li"><button class="chk${x.done?' on':''}" data-act="toggleDay" data-id="${x.id}" aria-label="${x.done?'Undo':'Done'}: ${esc(x.title)}">${ic('check')}</button><div style="flex:1;min-width:0"><div class="t${x.done?' done':''}">${esc(x.title)}</div>${x.time?`<div class="s">${fmtTime(x.time)}</div>`:''}</div><button class="ibtn sm" data-act="editDay" data-id="${x.id}" aria-label="Edit">${ic('edit')}</button></div>`).join('')}</div>`:'<p class="small muted">No goals were set for this day.</p>'}</section>`;
 if(stepsDone.length||due.length)o+=`<section class="rv dy-sec"><div class="st-h"><h3>Steps</h3></div><div class="list">${stepsDone.map(e=>{const g=G(e.goalId);return`<div class="li" style="${g?cvar(g):''}"><span class="chk on">${ic('check')}</span><div style="flex:1;min-width:0"><div class="t">${esc(e.text||'Step')}</div><div class="s"><i></i><span class="ell">${g?esc(g.title):''} · ${tShort(e.t)}</span></div></div></div>`}).join('')}${due.map(({g,s})=>`<div class="li" style="${cvar(g)}"><button class="chk" data-act="toggleStep" data-g="${g.id}" data-s="${s.id}" aria-label="Done: ${esc(s.title)}">${ic('check')}</button><div style="flex:1;min-width:0"><div class="t">${esc(s.title)}</div><div class="s"><i></i><span class="ell">${esc(g.title)} · was due this day</span></div></div></div>`).join('')}</div></section>`;
 const jl=ents.filter(e=>e.type!=='step'&&e.type!=='focus');
 o+=`<section class="rv dy-sec" id="dyJ"><div class="st-h"><h3>Journal</h3>${fut?'':`<button class="link" data-act="addEntry">Write</button>`}</div>${jl.length?`<div class="dy-jg">${jl.map(e=>journalCard(e)).join('')}</div>`:'<p class="small muted">No journal entries on this day.</p>'}</section>`;
 if(tu.length)o+=`<section class="rv dy-sec"><div class="st-h"><h3>Threads</h3></div><div class="list">${tu.map(({t,u})=>`<div class="li" data-act="thrOpen" data-id="${t.id}"><span class="thr-em">${thrTag(u.k)[1]}</span><div style="flex:1;min-width:0"><div class="t">${esc(u.x)}</div><div class="s"><span class="ell">${esc(t.title)} · ${tShort(u.t)}</span></div></div></div>`).join('')}</div></section>`;
 if(notes.length||reacts.length)o+=`<section class="rv dy-sec" id="dyN"><div class="st-h"><h3>Reminders, replies and cheers</h3></div>${notes.length?`<ol class="dy-chat">${notes.map(e=>{return dyNoteRow(e)}).join('')}</ol>`:''}${reacts.length?`<ul class="dy-rl">${reacts.map(dyReactRow).join('')}</ul>`:''}</section>`;
 else if(NATIVE&&NATIVE.notifLog)o+=`<section class="rv dy-sec" id="dyN"><div class="st-h"><h3>Reminders and replies</h3></div><p class="small muted">No reminders on this day. The phone keeps the latest 400.</p></section>`;
 return o}
const dyAfter=()=>{checkMilestones();save();render(false)};
Object.assign(ACT,{
 dyOpen:d=>{closeSheet();go('day/'+d.d)},
 dyJump:d=>{const el=document.getElementById(d.to);if(!el)return;document.querySelectorAll('#view .rv').forEach(x=>x.classList.add('in'));el.scrollIntoView({behavior:reduced()?'auto':'smooth',block:'start'})},
 dyBack:()=>{if(history.length>1)history.back();else go('habits')},
 dyGo:d=>{const n=fromN(dnum(cur.id||ymd())+ +d.d);if(n>ymd())return;go('day/'+n)},
 dyX:(d,el)=>{const row=el.closest('.dy-h');if(!row)return;const o=!row.classList.contains('open');row.classList.toggle('open',o);el.setAttribute('aria-expanded',o);o?DAYX.add(d.id):DAYX.delete(d.id)},
 dyTog:d=>{const h=H(d.id);if(!h||d.d>ymd())return;const v=hVal(h,d.d),n=hTarget(h);hSetVal(h,d.d,v>=n?0:n);if(h.skip)delete h.skip[d.d];if(v<n)vib();dyAfter()},
 dyAdj:d=>{const h=H(d.id);if(!h||d.d>ymd())return;hSetVal(h,d.d,hVal(h,d.d)+(+d.n));dyAfter()},
 dyLim:d=>{const h=H(d.id);if(!h||d.d>ymd())return;const v=Math.max(0,(+h.log[d.d]||0)+(+d.n));if(v)h.log[d.d]=v;else delete h.log[d.d];save();render(false)},
 dySlipDel:d=>askConfirm('Remove this slip?','Your clean-day count is worked out again without it.','Remove',()=>{const h=H(d.id);if(!h)return;h.slips=(h.slips||[]).filter(s=>slipT(s)!==+d.t);h.mile=0;closeSheet();save();render(false);toast('Slip removed')},false)});
Object.assign(FORM,{dySlip:f=>{const h=H(f.dataset.id);if(!h)return;const[hh,mm]=(f.elements.tm.value||'12:00').split(':').map(Number),t=new Date(f.dataset.d+'T00:00');t.setHours(hh||0,mm||0,0,0);if(t.getTime()>Date.now())return toast('That time hasn’t happened yet');h.slips=[...(h.slips||[]),{t:t.getTime(),trig:'',note:f.elements.note.value.trim()}];h.mile=0;save();render(false);toast('Slip logged')}});
/* swipe the date heading to move a day */
{const _r=render;render=function(a){_r(a);if(cur.p==='day'){swipe(document.querySelector('.dy-head'),d=>ACT.dyGo({d}))}if(cur.p==='journal'){swipe(document.getElementById('jwBar'),d=>ACT.jwGo({d:-d}))}
 /* every page gets the settings gear in the top-right corner, including detail pages */
 document.querySelectorAll('#view .crumb>.ph-r').forEach(r=>{if(!r.querySelector('.gearb'))r.insertAdjacentHTML('beforeend',gear())})}}
/* page titles shrink to fit next to the buttons instead of breaking words */
/* Page titles never break inside a word: shrink to fit; if the title would get too small beside the action
   buttons, the buttons move to their own row above it (large-title layout) and the title gets the full width. */
function fitH1(){document.querySelectorAll('#view .ph h1').forEach(h=>{const ph=h.closest('.ph');if(ph)ph.classList.remove('ph-stack');h.style.fontSize='';if(!h.offsetParent)return;h.style.overflowWrap='normal';
 const fit=min=>{let s=parseFloat(getComputedStyle(h).fontSize);if(h.scrollWidth<=h.clientWidth+1)return true;let lo=min,hi=s;for(let n=0;n<6;n++){const m=(lo+hi)/2;h.style.fontSize=m+'px';if(h.scrollWidth>h.clientWidth+1)hi=m;else lo=m}h.style.fontSize=Math.floor(lo)+'px';return h.scrollWidth<=h.clientWidth+1};
 if(!fit(30)&&ph&&ph.querySelector(':scope>.ph-r')){h.style.fontSize='';ph.classList.add('ph-stack');fit(22)}h.style.overflowWrap=''})}
if(document.fonts)document.fonts.ready.then(()=>{try{fitH1()}catch(e){}});
{const _r=render;render=function(a){_r(a);fitH1()}}addEventListener('resize',()=>fitH1(),{passive:true});
{const _nh=nhist;nhist=function(k){let r;try{r=_nh(k)}catch(e){r=[]}return Array.isArray(r)?r:[]}}
/* card titles shrink to fit their card (long words, long titles) instead of spilling over the edge */
function fitCards(root){(root||document).querySelectorAll('#view .fc-t, #view .sc-t, #view .jb-big, #view .jt-big').forEach(el=>{const face=el.closest('.fc-face,.sc-face,.jb,.jf-face');if(!face||!face.offsetParent)return;el.style.removeProperty('font-size');el.style.overflowWrap='normal';
 let s=parseFloat(getComputedStyle(el).fontSize),n=0;const ft=[...face.children].find(c=>c!==el&&c.compareDocumentPosition(el)&Node.DOCUMENT_POSITION_PRECEDING),box=el.closest('.hs-card')||face,over=()=>el.scrollWidth>el.clientWidth+1||el.scrollHeight>el.clientHeight+s*.35||face.scrollHeight>face.clientHeight+1||(ft&&el.getBoundingClientRect().bottom>ft.getBoundingClientRect().top-2);if(over()){let lo=14,hi=s;for(n=0;n<5;n++){const m=(lo+hi)/2;el.style.setProperty('font-size',m+'px','important');if(over())hi=m;else lo=m}el.style.setProperty('font-size',Math.floor(lo)+'px','important')}el.style.overflowWrap=''})}
{const _r=render;render=function(a){_r(a);fitCards();if(document.fonts&&document.fonts.status!=='loaded')document.fonts.ready.then(()=>fitCards())}}addEventListener('resize',()=>fitCards(),{passive:true});
/* the small habit sheets (Habit Vista, habit grid) can open the whole day */
{const _h=ACT.hDaySheet;ACT.hDaySheet=d=>{_h(d);const a=$('#sheet .actions');const b=`<button class="btn" data-act="dyOpen" data-d="${d.d}">Everything on this day</button>`;if(a)a.insertAdjacentHTML('afterbegin',b);else $('#sheet').insertAdjacentHTML('beforeend',`<div class="actions">${b}</div>`)}}

/* ================= HABITS: a week you can tap, and a month calendar one tap away ================= */
let HH={m:ymd().slice(0,7)};
const hcalOn=()=>!!S.settings.layout.hcal;
function hhCell(ds){const td=ymd(),fut=ds>td,r=dayRatio(ds),p=r.due?r.dn/r.due:0,k=fut||!r.due?'':p>=1?' full':p>0?' part':' miss';
 return`<button class="hh-c${k}${ds===td?' today':''}${fut?' fut':''}" ${fut?'disabled':''} data-act="dyOpen" data-d="${ds}" style="--p:${Math.round(p*100)}" aria-label="${esc(dayName(ds))}: ${r.due?r.dn+' of '+r.due+' habits':'nothing due'}"><b>${+ds.slice(8)}</b>${!fut&&r.due?`<small>${r.dn}/${r.due}</small>`:''}</button>`}
function hCal(){const[y,m]=HH.m.split('-').map(Number),first=new Date(y,m-1,1),n=new Date(y,m,0).getDate(),lead=(first.getDay()+6)%7,cells=[],P=s=>String(s).padStart(2,'0');
 for(let i=0;i<lead;i++)cells.push('<span class="hh-e"></span>');for(let d=1;d<=n;d++)cells.push(hhCell(`${y}-${P(m)}-${P(d)}`));
 let done=0,due=0;for(let d=1;d<=n;d++){const ds=`${y}-${P(m)}-${P(d)}`;if(ds>ymd())break;const r=dayRatio(ds);due+=r.due;done+=r.dn}const cm=ymd().slice(0,7);
 return`<div class="hcal-in"><div class="hh-h"><button class="ibtn sm" data-act="hhMon" data-d="-1" aria-label="Previous month">${ic('back')}</button><div><b>${first.toLocaleDateString(undefined,{month:'long',year:'numeric'})}</b><span class="data">${due?Math.round(done/due*100)+'% kept · '+done+' of '+due:'Nothing due yet'}</span></div><button class="ibtn sm" data-act="hhMon" data-d="1" aria-label="Next month" ${HH.m>=cm?'disabled':''}>${ic('next')}</button></div>
 <div class="hh-w">${['M','T','W','T','F','S','S'].map(x=>`<span>${x}</span>`).join('')}</div><div class="hh-g">${cells.join('')}</div><div class="hh-leg"><span><i class="full"></i>All done</span><span><i class="part"></i>Some</span><span><i class="miss"></i>Missed</span><span class="muted">Tap a day to see and change everything</span></div></div>`}
function hWeekStrip(){const t=dnum(ymd()),days=[];for(let k=t-6;k<=t;k++)days.push(fromN(k));const on=hcalOn();
 return`<section class="rv hh-wk"><div class="hh-row">${days.map(ds=>{const r=dayRatio(ds),p=r.due?r.dn/r.due:0;return`<button class="hh-d${p>=1?' full':p>0?' part':r.due&&ds<ymd()?' miss':''}${ds===ymd()?' today':''}" data-act="dyOpen" data-d="${ds}" style="--p:${Math.round(p*100)}" aria-label="${esc(dayName(ds))}: open the day"><span>${new Date(ds+'T12:00').toLocaleDateString(undefined,{weekday:'narrow'})}</span><i><b>${+ds.slice(8)}</b></i><small>${r.due?r.dn+'/'+r.due:'–'}</small></button>`}).join('')}</div>
 <button class="hcal-t" data-act="hcalT" aria-expanded="${on}">${ic('cal','ico-s')}<span>${on?'Hide calendar':'Show the month'}</span>${ic('chev','ico-s hcal-chev')}</button><div class="hcal${on?' on':''}" id="hcal">${on?hCal():''}</div></section>`}
Object.assign(ACT,{
 hcalT:()=>{const on=!hcalOn();S.settings.layout.hcal=on;save();if(HV!=='today'){HV='today';render(false);return}const c=$('#hcal'),b=document.querySelector('.hcal-t');if(!c){render(false);return}
  if(on){c.innerHTML=hCal();requestAnimationFrame(()=>c.classList.add('on'))}else{c.classList.remove('on');setTimeout(()=>{if(!hcalOn())c.innerHTML=''},380)}
  if(b){b.setAttribute('aria-expanded',on);b.querySelector('span').textContent=on?'Hide calendar':'Show the month'}document.querySelectorAll('[data-act=hcalT].ibtn').forEach(x=>x.classList.toggle('on',on))},
 hhMon:d=>{const[y,m]=HH.m.split('-').map(Number),x=new Date(y,m-1+ +d.d,1),k=x.getFullYear()+'-'+String(x.getMonth()+1).padStart(2,'0');if(k>ymd().slice(0,7))return;HH.m=k;const c=$('#hcal');if(c)c.innerHTML=hCal()}});
{const _vH=vHabits;vHabits=function(){let o=_vH();const i=o.indexOf('<div class="ph-r">');if(i>0&&S.habits.length)o=o.slice(0,i+18)+`<button class="ibtn${hcalOn()?' on':''}" data-act="hcalT" aria-label="Calendar" title="Calendar">${ic('cal')}</button>`+o.slice(i+18);return o};VIEWS.habits=vHabits}

/* ================= ACTIVITY ================= */
const ACT_EX={2:'Badminton',5:'Basketball',8:'Cycling',9:'Cycling',10:'Boot camp',11:'Boxing',14:'Cricket',16:'Dancing',25:'Elliptical',26:'Class',32:'Golf',34:'Gymnastics',36:'HIIT',37:'Hiking',44:'Martial arts',48:'Pilates',51:'Climbing',53:'Rowing',54:'Rowing',56:'Running',57:'Treadmill run',64:'Soccer',66:'Squash',70:'Strength',71:'Stretching',73:'Swimming',74:'Swimming',75:'Table tennis',76:'Tennis',78:'Volleyball',79:'Walking',81:'Weightlifting',83:'Yoga'};
const ACT_EI={8:'🚴',9:'🚴',11:'🥊',14:'🏏',16:'💃',37:'🥾',44:'🥋',48:'🧘',51:'🧗',53:'🚣',54:'🚣',56:'🏃',57:'🏃',64:'⚽',70:'🏋️',81:'🏋️',73:'🏊',74:'🏊',76:'🎾',78:'🏐',79:'🚶',83:'🧘',5:'🏀',2:'🏸'};
let ACTV={data:null,at:0,busy:false,days:7,more:14,first:true};
const actSet=()=>({goal:8000,sleep:7.5,scr:180,...(S.settings.act||{})});
const actLog=()=>S.settings.actLog||(S.settings.actLog={});
const nat=()=>NATIVE&&NATIVE.activityLoad;
const WK_RX=/gym|fitness|work ?out|pool|swim|yoga|pilates|crossfit|dojo|boxing|martial|climb|sport|court|field|track|club|studio|🏋|💪|🏊|🧘|🥊|🧗|⚽|🏀|🎾/i;
const wkPlace=p=>p&&(p.wk!=null?!!p.wk:WK_RX.test((p.name||'')+' '+(p.emoji||'')));
const plWk=pm=>{let m=0;places().forEach(p=>{if(wkPlace(p))m+=+((pm||{})[p.id])||0});return m};
const wkMin=o=>o?((+o.x||0)+plWk(o.p))||(o.x!=null?0:undefined):undefined;
const fmtMinC=m=>fmtMin(m).replace(' ','');
function actLoad(force){if(!nat()||ACTV.busy)return;if(!force&&ACTV.data&&Date.now()-ACTV.at<45000)return;ACTV.busy=true;const n=ACTV.first?30:Math.max(ACTV.days,14);ACTV.first=false;setTimeout(()=>{try{NATIVE.activityLoad(n)}catch(e){ACTV.busy=false}},0);setTimeout(()=>{ACTV.busy=false},25000)}
window.__act=j=>{ACTV.busy=false;let d;try{d=JSON.parse(j)}catch(e){return}if(!d||d.err)return;ACTV.data=d;ACTV.at=Date.now();const L=actLog();
 Object.entries(d.days||{}).forEach(([k,v])=>{const o=L[k]||{};const st=Math.max(+v.steps||0,+v.hsteps||0);if(st||o.s==null)o.s=Math.max(st,o.s||0);if(v.sleep!=null)o.z=v.sleep;if(v.ex!=null)o.x=v.ex;if(v.scr!=null)o.m=v.scr;if(v.pl)o.p=v.pl;if(v.ap)o.a=v.ap;if(v.ss)o.ss=v.ss;if(v.w)o.w=v.w;L[k]=o});
 const keys=Object.keys(L).sort();while(keys.length>180)delete L[keys.shift()];save();
 if((cur.p==='activity'||cur.p==='day')&&!$('#sheet').classList.contains('on'))render(false);else actCardPaint()};
document.addEventListener('visibilitychange',()=>{if(document.visibilityState==='visible'&&['activity','today','day'].includes(cur.p))actLoad(false)});
function actDays(n){const t=dnum(ymd()),L=actLog(),out=[];for(let k=t-n+1;k<=t;k++){const ds=fromN(k),o=L[ds]||{};out.push({d:ds,s:o.s,z:o.z,x:wkMin(o),m:o.m,p:o.p||{}})}return out}
function actBars(days,key,goal,fmt){const n=days.length,W=n*30,H=86,vals=days.map(d=>+d[key]||0),mx=Math.max(goal||0,...vals,1),bw=n>14?14:20;
 const bars=days.map((d,i)=>{const v=vals[i],h=v?Math.max(3,v/mx*58):0,x=i*30+(30-bw)/2,t=d.d===ymd(),lab=n>14?(i%5===0||t?+d.d.slice(8):''):new Date(d.d+'T12:00').toLocaleDateString(undefined,{weekday:'narrow'});
  return`<g class="abg" data-act="dyOpen" data-d="${d.d}" role="button" tabindex="0" aria-label="${esc(dayName(d.d))}: ${v?fmt(v):'no data'}"><title>${esc(dayName(d.d))}: ${v?fmt(v):'no data'}</title><rect x="${i*30}" y="0" width="30" height="${H}" fill="transparent"/><rect x="${x}" y="${68-h}" width="${bw}" height="${Math.max(h,2)}" rx="5" class="ab${t?' t':''}${goal&&v>=goal?' ok':''}${v?'':' z'}"/><text x="${i*30+15}" y="82" text-anchor="middle" class="al${t?' t':''}">${lab}</text></g>`}).join('');
 const gl=goal?`<line x1="0" x2="${W}" y1="${68-goal/mx*58}" y2="${68-goal/mx*58}" class="a-gl"/>`:'';
 return`<svg class="abars" viewBox="0 0 ${W} ${H}" role="group" aria-label="Last ${n} days. Tap a day to open it.">${gl}${bars}</svg>`}
function actTile(id,ic0,title,big,sub,chart,extra,need){return`<section class="panel act-t rv" id="act-${id}"><div class="act-h"><span class="act-i">${ic0}</span><b>${title}</b></div>${need||`<div class="act-big">${big}</div><div class="small muted">${sub}</div>${chart||''}${extra||''}`}</section>`}
const actNeed=(t,x,b)=>`<div class="act-need"><p class="small muted">${x}</p><button class="btn sm pri" data-act="autoPerm" data-t="${t}">${b}</button></div>`;
const wkName=w=>`${ACT_EI[w.k]||'💪'} ${ACT_EX[w.k]||'Workout'}`;
function actDayHTML(ds,A){const P=places(),A0=actSet();const tile=(e,v,l,p)=>`<div class="ad-t"><span>${e}</span><b>${v}</b><small>${l}</small>${p!=null?`<i style="--p:${Math.min(100,p)}"></i>`:''}</div>`;
 let o=`<div class="ad-grid">${tile('👟',A.s!=null?(+A.s).toLocaleString():'—','steps',A.s!=null?Math.round(A.s/A0.goal*100):null)}${tile('😴',A.z?fmtMin(A.z):'—','sleep',A.z?Math.round(A.z/(A0.sleep*60)*100):null)}${tile('🏋️',wkMin(A)?fmtMin(wkMin(A)):'—','workouts')}${tile('📱',A.m!=null?fmtMin(A.m):'—','screen time')}</div>`;
 const rows=[];
 (A.ss||[]).forEach(([s,e])=>rows.push(`<li><span>😴</span><b>Asleep ${tShort(s)} → ${tShort(e)}</b><small>${fmtMin((e-s)/6e4)}</small></li>`));
 (A.w||[]).forEach(w=>rows.push(`<li><span>${(ACT_EI[w.k]||'💪')}</span><b>${esc(ACT_EX[w.k]||'Workout')}</b><small>${fmtMin(w.m)} · ${tShort(w.t)}</small></li>`));
 (A.a||[]).forEach(([n,m])=>rows.push(`<li><span>📱</span><b>${esc(n)}</b><small>${fmtMin(m)}</small></li>`));
 Object.entries(A.p||{}).forEach(([id,m])=>{const p=P.find(x=>x.id===id);if(p&&m)rows.push(`<li><span>${esc(p.emoji||'📍')}</span><b>${esc(p.name)}${wkPlace(p)?' · workout':''}</b><small>${fmtMin(m)} there</small></li>`)});
 return o+(rows.length?`<ul class="ad-list">${rows.join('')}</ul>`:'')}
function vActivity(){actLoad(false);const A=actSet(),d=ACTV.data||{},ok=d.ok||{},N=ACTV.days,days=actDays(N),td=days[days.length-1],L=actLog(),avg=k=>{const v=days.map(x=>x[k]).filter(x=>x>0);return v.length?Math.round(v.reduce((a,b)=>a+b,0)/v.length):0};
 const head=`<header class="ph"><div><h1>Activity</h1><div class="data">${ACTV.at?'Updated '+tShort(ACTV.at)+' · ':''}read on this phone, never uploaded</div></div><div class="ph-r"><button class="ibtn" data-act="actRefresh" aria-label="Refresh" title="Refresh">${ic('rep')}</button><button class="ibtn" data-act="actGoals" aria-label="Daily goals" title="Daily goals">${ic('target')}</button>${gear()}</div></header>`;
 if(!NATIVE)return head+`<div class="empty rv"><h3>Activity lives in the Android app</h3><p>Steps, sleep, workouts, screen time and places are read from your phone, so there’s nothing to show on the web.</p></div>`;
 if(!nat())return head+`<div class="empty rv"><p>Update the app to see your activity.</p></div>`;
 if(!ACTV.data&&!Object.keys(L).length)return head+`<div class="empty rv"><p>Reading your phone…</p></div>`;
 const bar=`<div class="bar">${segHTML('actd','actRange',[['7','7 days'],['14','14 days'],['30','30 days']],String(N))}<span class="data">Tap a bar to open that day</span></div>`;
 const st=td.s||0,pct0=Math.min(100,Math.round(st/A.goal*100)),hit=days.filter(x=>x.s>=A.goal).length,hasS=ok.steps||days.some(x=>x.s);
 const ring=`<div class="act-ring" style="--p:${pct0}"><b>${st.toLocaleString()}</b><small>steps today</small></div>`;
 const T=[];
 const hci=d.hc||{},hcNote=(what,n,err)=>ok.hc&&ACTV.data?(err?`<p class="small muted act-diag">Couldn’t read ${what} from Health Connect: ${esc(trunc(err,120))}</p>`:!n?`<div class="act-diag"><p class="small muted">Health Connect is connected but has no ${what} from the last 30 days. If you use Samsung Health, Fitbit, Google Fit or a watch app, turn on syncing to Health Connect in that app.</p><button class="btn sm" data-act="hcOpen">Open Health Connect</button></div>`:''):'';
 T.push(actTile('steps','👟','Steps',hasS?ring:'—',hasS?`${pct0}% of ${A.goal.toLocaleString()} · avg ${avg('s').toLocaleString()} · goal met ${hit} of ${N} days`:'',hasS?actBars(days,'s',A.goal,v=>v.toLocaleString()+' steps'):'','',hasS?'':actNeed('steps','Allow physical activity so Plotline can read your phone’s step counter.','Allow')));
 const zl=days.slice().reverse().find(x=>x.z>0),hasZ=ok.hc||zl;
 T.push(actTile('sleep','😴','Sleep',hasZ&&(td.z||zl)?fmtMin(td.z||zl.z):'—',hasZ?(td.z?'last night':zl?'last recorded · '+dayName(zl.d):'no sleep recorded yet')+` · avg ${fmtMin(avg('z'))}`:'',hasZ?actBars(days,'z',A.sleep*60,fmtMin):'',hcNote('sleep',hci.sleep,hci.errSleep),hasZ?'':actNeed('sleep',ok.hcSupported===false?'Sleep and workouts come from Health Connect, built into Android 14 and newer.':'Connect Health Connect to see sleep and workouts from your fitness apps.',ok.hcSupported===false?'OK':'Connect')));
 const W=((d.work)||[]).slice().sort((a,b)=>b.t-a.t),wk=days.filter(x=>x.x>0).length,WP=places().filter(wkPlace),hasW=ok.hc||W.length||days.some(x=>x.x)||WP.length;
 T.push(actTile('work','🏋️','Workouts',hasW&&td.x?fmtMin(td.x):'—',hasW?`today · active ${wk} of ${N} days`:'',hasW?actBars(days,'x',0,fmtMin):'',(W.length||WP.length?`<div class="act-list">${W.slice(0,6).map(w=>`<button data-act="dyOpen" data-d="${dOf(w.t)}"><span>${ACT_EI[w.k]||'💪'}</span><b>${esc(ACT_EX[w.k]||'Workout')}</b><small>${fmtMin(w.m)} · ${esc(dayName(dOf(w.t)))}</small></button>`).join('')}${WP.map(p=>{const m=days.reduce((a,x)=>a+(+(x.p||{})[p.id]||0),0);return`<div><span>${esc(p.emoji||'📍')}</span><b>${esc(p.name)}</b><small>${m?fmtMin(m)+' in '+N+' days':'counted when you leave'}</small></div>`}).join('')}</div>`:(hasW?'<p class="small muted" style="margin-top:10px">No workouts in this period.</p>':''))+hcNote('workouts',hci.ex,hci.errEx),hasW?'':actNeed('workout','Workouts come from any app that writes to Health Connect, like Samsung Health, Fitbit or Strava.','Connect')));
 const top=d.top||((L[ymd()]||{}).a||[]).map(([name,min])=>({name,min})),hasM=ok.screen||days.some(x=>x.m);
 T.push(actTile('screen','📱','Screen time',hasM&&td.m!=null?fmtMin(td.m):'—',hasM?`today · avg ${fmtMin(avg('m'))} a day`+(A.scr?` · limit ${fmtMin(A.scr)}`:''):'',hasM?actBars(days,'m',A.scr,fmtMin):'',top.length?`<div class="act-list">${top.map(a=>`<div><span>📱</span><b>${esc(a.name)}</b><small>${fmtMin(a.min)} today</small></div>`).join('')}</div>`:'',hasM?'':actNeed('screen','Turn on Usage access for Plotline to see how long you spend in apps. Only minutes are read.','Allow')));
 const P=places(),inside=d.inside||{};
 T.push(actTile('places','📍','Places',P.length?(P.some(p=>inside[p.id])?'At '+esc(P.filter(p=>inside[p.id]).map(p=>p.name).join(', ')):'Away'):'—',P.length?'time is counted when you leave a saved place · places you mark count as workouts':'',P.length?`<div class="act-list">${P.map(p=>{const wm=days.reduce((a,x)=>a+(+(x.p||{})[p.id]||0),0),tm=+(td.p||{})[p.id]||0;return`<div><span>${esc(p.emoji||'📍')}</span><b>${esc(p.name)}</b><small>${inside[p.id]?'here now · ':''}${tm?fmtMin(tm)+' today · ':''}${wm?fmtMin(wm)+' in '+N+' days':'no time yet'}</small><button class="chip act-wk${wkPlace(p)?' on':''}" data-act="actWk" data-id="${p.id}" aria-pressed="${wkPlace(p)}">${wkPlace(p)?'✓ Counts as workout':'Count as workout'}</button></div>`}).join('')}</div>`:'',P.length&&!ok.bg&&ACTV.data?actNeed('place','Allow location “all the time” so visits count with the app closed.','Allow'):'',P.length?'':`<div class="act-need"><p class="small muted">Save the places you go often (home, work, the gym) and Plotline counts the time you spend at each. Location never leaves the phone.</p><button class="btn sm pri" data-act="placeNew">${ic('plus')}Add the place I’m at now</button></div>`));
 const keys=Object.keys(L).filter(k=>k<=ymd()).sort().reverse(),shown=keys.slice(0,ACTV.more);
 const hist=`<section class="rv"><div class="st-h"><h3>Day by day</h3><span class="data">${keys.length} day${keys.length===1?'':'s'} kept on this phone</span></div><div class="ah-list">${shown.map(k=>{const o=L[k]||{};return`<button class="ah-r" data-act="dyOpen" data-d="${k}"><span class="ah-d"><b>${esc(dayName(k))}</b><small>${new Date(k+'T12:00').toLocaleDateString(undefined,{month:'short',day:'numeric'})}</small></span><span class="ah-v${o.s>=A.goal?' ok':''}"><i>👟</i>${o.s!=null?(+o.s).toLocaleString():'—'}</span><span class="ah-v"><i>😴</i>${o.z?fmtMinC(o.z):'—'}</span><span class="ah-v"><i>🏋️</i>${wkMin(o)?fmtMinC(wkMin(o)):'—'}</span><span class="ah-v"><i>📱</i>${o.m!=null?fmtMinC(o.m):'—'}</span>${ic('next','ico-s')}</button>`}).join('')}</div>${keys.length>shown.length?`<div class="actions left"><button class="btn sm" data-act="actMore">Show older days</button></div>`:''}</section>`;
 return head+bar+`<div class="act-grid">${T.join('')}</div>`+hist+`<p class="small muted" style="margin:18px 4px 0">Habits can use these too: tap ⚡ on a habit to check it off by itself.</p>`}
function actCardHTML(){if(!nat())return'';const L=actLog(),t=L[ymd()]||{},A=actSet(),y=L[fromN(dnum(ymd())-1)]||{};
 const z=t.z||0,tile=(k,e,v,l,p)=>`<button class="at-t" data-act="actGo" data-k="${k}"><span class="at-e">${e}</span><b>${v}</b><small>${l}</small>${p!=null?`<i style="--p:${Math.max(2,Math.min(100,p))}"></i>`:''}</button>`;
 return`<section class="rv at" id="actCard"><div class="st-h"><h3>Activity</h3><a class="link" href="#/activity">Details</a></div><div class="at-g">${tile('steps','👟',(t.s||0).toLocaleString(),`steps · goal ${A.goal>=1000?A.goal/1000+'k':A.goal}`,Math.round((t.s||0)/A.goal*100))}${tile('sleep','😴',z?fmtMin(z):'—','sleep'+(z?'':' · not recorded'),z?Math.round(z/(A.sleep*60)*100):null)}${tile('work','🏋️',wkMin(t)?fmtMin(wkMin(t)):'—','workout'+(wkMin(t)?'':' today'))}${tile('screen','📱',t.m!=null?fmtMin(t.m):'—','screen time',A.scr&&t.m!=null?Math.round(t.m/A.scr*100):null)}</div>${y.s!=null?`<button class="at-y" data-act="dyOpen" data-d="${fromN(dnum(ymd())-1)}">Yesterday: ${(+y.s).toLocaleString()} steps${y.z?' · '+fmtMin(y.z)+' sleep':''}${y.m!=null?' · '+fmtMin(y.m)+' screen':''} ${ic('next','ico-s')}</button>`:''}</section>`}
function actCardPaint(){const e=document.getElementById('actCard');if(!e)return;const t=document.createElement('div');t.innerHTML=actCardHTML();const n=t.firstElementChild;if(n){n.classList.add('in');e.replaceWith(n)}}
Object.assign(ACT,{
 actRefresh:()=>{actLoad(true);toast('Refreshing…')},
 hcOpen:()=>{try{NATIVE.openHealthConnect()}catch(e){}},
 actWk:d=>{const L=places(),p=L.find(x=>x.id===d.id);if(!p)return;p.wk=!wkPlace(p);S.settings.places=[...L];save();render(false);toast(p.wk?`Time at ${p.name} counts as a workout`:`${p.name} no longer counts as a workout`)},
 actRange:d=>{ACTV.days=+d.v;render(false)},
 actMore:()=>{ACTV.more+=30;render(false)},
 actGo:d=>{go('activity');setTimeout(()=>{const el=document.getElementById('act-'+d.k);if(el){document.querySelectorAll('#view .rv').forEach(x=>x.classList.add('in'));el.scrollIntoView({behavior:reduced()?'auto':'smooth',block:'start'})}},120)},
 actGoals:()=>{const A=actSet();openSheet(`<div class="data">Activity</div><h2 style="margin-top:6px">Daily goals</h2><form data-form="actGoals"><div class="field"><label>Steps a day</label><input type="number" name="goal" min="500" step="500" value="${A.goal}"></div><div class="field"><label>Sleep, hours a night</label><input type="number" name="sleep" min="3" max="12" step="0.5" value="${A.sleep}"></div><div class="field"><label>Screen time limit, minutes a day <span class="muted">(0 = none)</span></label><input type="number" name="scr" min="0" step="15" value="${A.scr}"></div><div class="actions"><button class="btn ghost" type="button" data-act="close">Cancel</button><button class="btn pri" type="submit">Save</button></div></form>`)}});
Object.assign(FORM,{actGoals:f=>{const E=f.elements;S.settings.act={goal:Math.max(500,+E.goal.value||8000),sleep:Math.min(12,Math.max(3,+E.sleep.value||7.5)),scr:Math.max(0,+E.scr.value||0)};save();closeSheet();render(false);toast('Saved')}});

/* ================= SCRIPT & HANDWRITTEN FONTS =================
 Bundled from @fontsource (OFL / Apache). size-adjust in each @font-face evens out how small most scripts draw,
 and a script heading turns off the all-caps headings (capital script letters don't join). */
const SCRIPT_FONTS=[
 ['dancing','Dancing Script','Dancing Script','d',[400,700],112],['caveat','Caveat','Caveat','d',[400,700],125],['pacifico','Pacifico','Pacifico','d',[400],95],
 ['greatvibes','Great Vibes','Great Vibes','d',[400],135],['satisfy','Satisfy','Satisfy','d',[400],115],['sacramento','Sacramento','Sacramento','d',[400],145],
 ['parisienne','Parisienne','Parisienne','d',[400],125],['allura','Allura','Allura','d',[400],135],['lobster','Lobster','Lobster','d',[400],104],
 ['yellowtail','Yellowtail','Yellowtail','d',[400],118],['kaushan','Kaushan Script','Kaushan Script','d',[400],104],['homemade','Homemade Apple','Homemade Apple','d',[400],88],
 ['kalam','Kalam','Kalam','b',[400,700],100],['patrick','Patrick Hand','Patrick Hand','b',[400],112],['indie','Indie Flower','Indie Flower','b',[400],106],
 ['caveatb','Caveat','Caveat','b',[400,700],125],['shadows','Shadows Into Light','Shadows Into Light','b',[400],115],['dancingb','Dancing Script','Dancing Script','b',[400,700],112]];
const scriptFont=id=>SCRIPT_FONTS.some(x=>x[0]===id);
SCRIPT_FONTS.forEach(x=>{if(!fontOf(x[0]))FONTS.push([x[0],x[1],x[2],x[3]])});
[['handwritten','Handwritten','dancing','kalam','dmmono'],['notebook','Notebook','caveat','patrick','spacemono'],['elegant','Elegant','greatvibes','lora','plexmono'],['playful','Playful','pacifico','nunito','dmmono'],['signature','Signature','sacramento','indie','dmmono']].forEach(p=>{if(!FONT_PRE.some(x=>x[0]===p[0]))FONT_PRE.push(p)});
{const st=document.createElement('style');st.id='fontfaces2';const fam=new Map();SCRIPT_FONTS.forEach(x=>fam.set(x[2],x));
 st.textContent=[...fam.values()].map(([,,f,,ws,adj])=>ws.map(w=>`@font-face{font-family:'${f}';font-weight:${w};font-display:swap;size-adjust:${adj}%;src:url(fonts/${f.toLowerCase().replace(/ /g,'-')}-latin-${w}-normal.woff2) format('woff2')}`).join('')).join('');
 document.head.appendChild(st)}
{const _af=applyFonts;applyFonts=function(){_af();const f=fontSet(),r=document.documentElement;r.toggleAttribute('data-fscript',scriptFont(f.d));r.toggleAttribute('data-fscriptb',scriptFont(f.b));
 /* script fonts fall back to the system's handwriting face while they load, not Impact */
 [['d','--f-display'],['b','--f-body']].forEach(([k,v])=>{if(scriptFont(f[k]))r.style.setProperty(v,`'${fontOf(f[k])[2]}',cursive`)})}}
applyFonts();

/* ================= REACTIONS: your own words, and instant mode on Android ================= */
Object.assign(FORM,{shReactTxt:f=>{const x=RXT&&shById(RXT.id),m=String(f.elements.m.value||'').replace(/\s+/g,' ').trim();if(!x||!m)return;if(reactsSentToday()>=REACT_MAX)return toast(`That’s ${REACT_MAX} reactions for today`);
 const nm=RXT.nm,back=RXT.back;reactSend(x,RXT.to,'',m).then(r=>{if(!r)return;toast(`Sent to ${esc(nm)} · “${esc(trunc(r.m,40))}”`);if(back)shOpen(x);else{closeSheet();shPanelPaint(x.id)}}).catch(e=>toast(shErr(e)))}});
const liveOK=()=>NATIVE&&NATIVE.liveShare;
const liveOn=()=>{try{return liveOK()&&NATIVE.liveShareOn()}catch(e){return false}};
function livePanel(){if(!liveOK())return'';const on=liveOn();return`<section class="panel rv"><h3>Instant reactions</h3><p class="small muted">Get reactions within about 30 seconds, even with Plotline closed. While you’re in a shared item, a small silent notification stays in your notification shade (Android needs it), and it checks the sharing space every 30 seconds. Off, reactions arrive within about 15 minutes.</p><label class="sw"><span>Instant reactions<small>${on?'On':'Off'} · uses a little more battery</small></span><input type="checkbox" data-act="liveT" ${on?'checked':''}><i></i></label></section>`}
Object.assign(ACT,{liveT:(d,el)=>{if(!liveOK())return;const v=!!(el&&el.checked);try{NATIVE.liveShare(v)}catch(e){}toast(v?'Instant reactions on':'Instant reactions off')}});

{const _sh=shareHTML;shareHTML=function(){return _sh()+livePanel()}}
/* ================= AI PLAN: habits too =================
 The plan format has an optional "habits" array (see SPEC). Each habit is checked, previewed with a tick box, and created
 after the goals, linked to the goal it supports. */
const HAB_KEYS=['title','icon','kind','freq','days','times','target','unit','part','time','why','goal'];
function planHabitsCheck(o,E){if(!('habits' in o))return;if(!Array.isArray(o.habits)){E.push('"habits" must be an array (may be []).');return}
 const gids=new Set([...(o.goals||[]).map(g=>g&&g.id),...S.goals.map(g=>g.id)]),have=new Set(S.habits.map(h=>lc(h.title)));
 o.habits.slice(0,12).forEach((h,i)=>{const p=`habits[${i}]`+(h&&typeof h.title==='string'?` ("${trunc(h.title,30)}")`:'');if(!h||typeof h!=='object'||Array.isArray(h)){E.push(p+' must be an object.');return}
  Object.keys(h).forEach(k=>{if(!HAB_KEYS.includes(k))E.push(`${p}: unknown key "${k}".`)});HAB_KEYS.forEach(k=>{if(!(k in h))E.push(`${p}: missing key "${k}".`)});
  if(typeof h.title!=='string'||h.title.trim().length<3||h.title.length>60)E.push(`${p}: "title" must be 3 to 60 characters.`);else if(have.has(lc(h.title)))E.push(`${p}: I already have this habit. Leave it out.`);
  if(typeof h.icon!=='string'||[...h.icon].length>8)E.push(`${p}: "icon" must be one emoji or "".`);
  if(!['build','quit'].includes(h.kind))E.push(`${p}: "kind" must be "build" or "quit".`);
  if(!['daily','days','times'].includes(h.freq))E.push(`${p}: "freq" must be "daily", "days" or "times".`);
  if(!Array.isArray(h.days)||h.days.some(d=>!Number.isInteger(d)||d<0||d>6))E.push(`${p}: "days" must be an array of weekday numbers 0 to 6.`);else if(h.freq==='days'&&!h.days.length)E.push(`${p}: freq "days" needs at least one weekday in "days".`);
  if(!Number.isInteger(h.times)||h.times<0||h.times>6)E.push(`${p}: "times" must be an integer 0 to 6.`);else if(h.freq==='times'&&h.times<1)E.push(`${p}: freq "times" needs "times" from 1 to 6.`);
  if(!Number.isInteger(h.target)||h.target<1||h.target>100)E.push(`${p}: "target" must be an integer 1 to 100.`);
  if(typeof h.unit!=='string'||h.unit.length>20)E.push(`${p}: "unit" must be a short string (may be "").`);
  if(!['morning','afternoon','evening','any'].includes(h.part))E.push(`${p}: "part" must be "morning", "afternoon", "evening" or "any".`);
  if(!(h.time===null||(typeof h.time==='string'&&/^([01]\d|2[0-3]):[0-5]\d$/.test(h.time))))E.push(`${p}: "time" must be "HH:MM" (24-hour) or null.`);
  if(typeof h.why!=='string')E.push(`${p}: "why" must be a string (may be "").`);
  if(!(h.goal===null||(typeof h.goal==='string'&&gids.has(h.goal))))E.push(`${p}: "goal" must be null or the id of a goal in the plan.`)})}
{const _ps=planSchema;planSchema=function(m){const s=_ps(m);s.properties.habits={type:'array',maxItems:6,items:{type:'object',properties:{title:{type:'string',minLength:3,maxLength:60},icon:{type:'string',maxLength:8},kind:{enum:['build','quit']},freq:{enum:['daily','days','times']},days:{type:'array',items:{type:'integer',minimum:0,maximum:6},maxItems:7},times:{type:'integer',minimum:0,maximum:6},target:{type:'integer',minimum:1,maximum:100},unit:{type:'string',maxLength:20},part:{enum:['morning','afternoon','evening','any']},time:{type:['string','null'],pattern:'^([01][0-9]|2[0-3]):[0-5][0-9]$'},why:{type:'string',maxLength:200},goal:{type:['string','null'],maxLength:24}},required:HAB_KEYS,additionalProperties:false}};return s}}
const planHabits=()=>AI.plan&&Array.isArray(AI.plan.habits)?AI.plan.habits:[];
function aiHabSel(){if(AI.hfor!==AI.plan){AI.hfor=AI.plan;AI.hsel=new Set(planHabits().map((_,i)=>i))}return AI.hsel}
const habFreqTxt=h=>h.freq==='days'?h.days.map(d=>DAYS[d].slice(0,3)).join(' '):h.freq==='times'?`${h.times}× a week`:'Every day';
{const _pv=previewHTML;previewHTML=function(){let o=_pv();const L=planHabits();if(!L.length)return o;const sel=aiHabSel(),plan=AI.plan;
 const gt=id=>{if(!id)return'';const g=plan.goals.find(x=>x.id===id)||G(id);return g?g.title:''};
 const hs=`<div class="pv-hh"><div class="data">Habits · ${sel.size} of ${L.length} chosen</div>${L.map((h,i)=>`<label class="pv ${sel.has(i)?'on':''}" style="--c:var(--accent);--dp:0"><input type="checkbox" data-act="pvHab" data-i="${i}" ${sel.has(i)?'checked':''}><div style="min-width:0;flex:1"><div class="t">${esc(h.icon||'✅')} ${esc(h.title)}<span class="badge add">${h.kind==='quit'?'Break':'New habit'}</span></div><div class="m"><span class="data">${esc(habFreqTxt(h))}${h.target>1?` · ${h.target} ${esc(h.unit)}`:''}</span><span class="data">${esc(HPARTS.find(p=>p[0]===h.part)?.[1]||'Anytime')}${h.time?' · '+fmtTime(h.time):''}</span>${gt(h.goal)?`<span class="data">for ${esc(trunc(gt(h.goal),36))}</span>`:''}</div>${h.why?`<p class="small muted" style="margin-top:4px">${esc(h.why)}</p>`:''}</div></label>`).join('')}</div>`;
 const k=o.lastIndexOf('<div class="actions left"><button class="btn pri" data-act="doImport"');return k<0?o+hs:o.slice(0,k)+hs+o.slice(k)}}
Object.assign(ACT,{pvHab:d=>{const s=aiHabSel(),i=+d.i;s.has(i)?s.delete(i):s.add(i);render(false)}});
{const _ap=applyPlan;applyPlan=function(){const o=AI.plan;if(!o)return _ap();const L=planHabits(),sel=aiHabSel(),M=AI.main;
 const titleOf=id=>{if(!id)return'';if(M&&id===M.rid)return(M.title||'').trim();const g=o.goals.find(x=>x.id===id);return g?g.title.trim():(G(id)?G(id).title:'')};
 const want=L.filter((_,i)=>sel.has(i)).map(h=>({h,gt:titleOf(h.goal)}));const before=new Set(S.goals.map(g=>g.id));_ap();if(!want.length)return;
 const fresh=S.goals.filter(g=>!before.has(g.id));const gidFor=(gt,id)=>{const f=fresh.find(g=>g.title===gt)||fresh.find(g=>lc(g.title)===lc(gt));return f?f.id:(G(id)?id:'')};
 want.forEach(({h,gt})=>{const g=gidFor(gt,h.goal),gg=g&&G(g);S.habits.push(normHabit({id:uid(),title:h.title.trim(),icon:oneEmoji(h.icon||''),kind:h.kind,mode:'quit',freq:h.freq,days:h.freq==='days'?h.days:[0,1,2,3,4,5,6],times:h.times||3,target:h.kind==='build'?h.target:1,unit:h.unit||'',part:h.part,time:h.time||'',remind:!!h.time,why:h.why||'',goalId:g||'',area:gg?gg.area:'personal',start:Date.now(),startDate:ymd(),createdAt:Date.now(),u:Date.now()}))});
 save();syncNative();toast(`${want.length} habit${want.length===1?'':'s'} added too`)}}

/* ================= HOME (was Today): quick actions, your day, insights, Plan with AI ================= */
NAV[0]=['today','Home','home'];
function aiPlanCard(where){if(where==='home'&&S.settings.aipHide)return`<section class="aip aip-min rv"><button class="aip-mb" data-act="aiStart"><span class="aip-i">${ic('ai')}</span><span class="aip-mt"><b>Plan with AI</b><small>Describe a goal, get steps and habits</small></span></button><button class="ibtn aip-x" data-act="aipShow" aria-label="Expand Plan with AI">${ic('chev')}</button></section>`;
 return`<section class="aip rv${where==='ask'?' in-ask':''}"><span class="aip-i">${ic('ai')}</span><div class="aip-b"><b>Plan with AI</b><p>Say what you want in your own words, like “get fit by summer”, “save for a house” or “a calmer morning”. The AI drafts a goal with steps, dates and the habits to get there. You check and edit everything before anything is saved.</p><div class="aip-a"><button class="btn pri sm" data-act="aiStart">${ic('ai')}Start a plan</button><button class="btn sm" data-act="aiChangeStart">Rework my plan</button>${where==='home'?`<button class="btn ghost sm" data-act="aipHide">Minimize</button>`:''}</div></div></section>`}
function homeQuick(){const Q=[['aiStart','✨','Plan with AI'],['newGoal','🎯','Goal'],['newHabit','✅','Habit'],['addEntry','✍️','Write'],['thrNew','🧵','Thread'],['askGo','💬','Ask'],['dayToday','📅','Today in full']];
 return`<div class="hmq rv" role="toolbar" aria-label="Quick actions">${Q.map(([a,e,l])=>`<button class="hmq-b" data-act="${a}"><span>${e}</span>${l}</button>`).join('')}</div>`}
function homeDay(){const td=ymd(),hs=S.habits.filter(h=>h.status==='active'&&h.kind!=='quit'&&hDue(h,td)&&!(h.skip&&h.skip[td])),dg=S.days.filter(x=>x.date===td),st=active().flatMap(g=>g.steps.filter(s=>s.due===td).map(s=>({g,s})));
 const items=[...hs.map(h=>({k:'h',done:hDone(h,td),t:`${hIcon(h)} ${h.title}`,h})),...dg.map(x=>({k:'d',done:!!x.done,t:x.title,x})),...st.map(o=>({k:'s',done:!!o.s.done,t:o.s.title,o}))];
 if(!items.length)return'';const dn=items.filter(i=>i.done).length,p=dn/items.length,nx=items.find(i=>!i.done);
 const act=nx?(nx.k==='h'?`data-act="hTap" data-id="${nx.h.id}"`:nx.k==='d'?`data-act="toggleDay" data-id="${nx.x.id}"`:`data-act="toggleStep" data-g="${nx.o.g.id}" data-s="${nx.o.s.id}"`):'';
 return`<section class="hday rv"><div class="hday-r" style="--p:${Math.round(p*100)}"><b>${dn}<small>/${items.length}</small></b></div><div class="hday-b"><div class="data">Your day · ${p>=1?'all done':Math.round(p*100)+'% done'}</div>${nx?`<div class="hday-n"><span class="ell">Next: ${esc(nx.t)}</span><button class="btn sm pri" ${act}>${ic('check')}Done</button></div>`:`<div class="hday-n"><span>Everything for today is done. Well played.</span></div>`}<button class="link small" data-act="dayToday">See everything for today ›</button></div></section>`}
function homeInsights(){const out=[],td=ymd(),tn=dnum(td),hr=new Date().getHours(),A=S.settings.actLog||{};
 S.habits.filter(h=>h.status==='active'&&h.kind!=='quit'&&hDue(h,td)&&!hDone(h,td)&&!(h.skip&&h.skip[td])).forEach(h=>{const n=hStreak(h);if(n>=3&&hr>=15)out.push({p:9,e:'🔥',t:`Your ${n}-${h.freq==='times'?'week':'day'} streak on “${h.title}” is on the line today.`,b:`<button class="btn sm" data-act="openHabit" data-id="${h.id}">Open habit</button>`,q:`My habit “${h.title}” has a ${n}-day streak and isn’t done yet today. What small version could I do right now to keep it going?`})});
 {const W=[0,1,2,3,4,5,6].map(()=>({d:0,n:0}));for(let k=tn-56;k<tn;k++){const ds=fromN(k),r=dayRatio(ds),w=new Date(ds+'T12:00').getDay();W[w].d+=r.due;W[w].n+=r.dn}
  const ok=W.map((x,i)=>({i,p:x.d>=6?x.n/x.d:null})).filter(x=>x.p!=null);if(ok.length>=5){const b=ok.reduce((a,x)=>x.p>a.p?x:a),w=ok.reduce((a,x)=>x.p<a.p?x:a);if(b.p-w.p>=.25){const nm=i=>new Date(2024,0,7+i).toLocaleDateString(undefined,{weekday:'long'});out.push({p:6,e:'📆',t:`You keep habits best on ${nm(b.i)}s (${Math.round(b.p*100)}%) and slip most on ${nm(w.i)}s (${Math.round(w.p*100)}%).`,q:`I keep my habits best on ${nm(b.i)}s and slip most on ${nm(w.i)}s. Looking at my habits and journal, why might that be, and what would make ${nm(w.i)}s easier?`})}}}
 active().forEach(g=>{const open=g.steps.filter(s=>!s.done);if(!open.length)return;const last=Math.max(dnum(g.startDate||td),...g.steps.filter(s=>s.doneAt).map(s=>dnum(ymd(new Date(s.doneAt)))));const idle=tn-last;if(idle>=14)out.push({p:5+(g.priority===1?2:0),e:'🧭',t:`No progress on “${g.title}” for ${idle} days. Next step: ${nextStep(g)?nextStep(g).title:open[0].title}.`,b:`<button class="btn sm" data-act="open" data-id="${g.id}">Open goal</button>`,q:`I haven’t made progress on my goal “${g.title}” for ${idle} days. What in my notes suggests why, and what is one small step I can take today?`})});
 {const S7=k=>{let a=0,n=0;for(let i=k;i<k+7;i++){const o=A[fromN(i)];if(o&&o.s!=null){a+=+o.s;n++}}return n>=4?a/n:null};const now=S7(tn-6),prev=S7(tn-13);if(now!=null&&prev&&Math.abs(now-prev)/prev>=.2)out.push({p:4,e:now>prev?'📈':'📉',t:`You’re averaging ${Math.round(now).toLocaleString()} steps a day this week, ${Math.round(Math.abs(now-prev)/prev*100)}% ${now>prev?'more':'less'} than last week.`,b:`<a class="btn sm" href="#/activity">Activity</a>`,q:`My steps went ${now>prev?'up':'down'} ${Math.round(Math.abs(now-prev)/prev*100)}% this week compared with last week. How does that line up with my habits, sleep and moods?`})}
 {let g=[],b=[];for(let k=tn-30;k<tn;k++){const ds=fromN(k),o=A[ds],r=dayRatio(ds);if(!o||!o.z||!r.due)continue;(o.z>=420?g:b).push(r.dn/r.due)}if(g.length>=3&&b.length>=3){const av=x=>x.reduce((a,v)=>a+v,0)/x.length,d=av(g)-av(b);if(Math.abs(d)>=.15)out.push({p:7,e:'😴',t:`After 7+ hours of sleep you keep ${Math.round(av(g)*100)}% of your habits, against ${Math.round(av(b)*100)}% on shorter nights.`,q:`I keep more of my habits after 7+ hours of sleep. Based on my data, what could help me sleep longer more often?`})}}
 threads().filter(t=>t.status!=='done'&&thrUps(t).length).forEach(t=>{const d=Math.floor((Date.now()-thrLast(t))/864e5);if(d>=7)out.push({p:3,e:'🧵',t:`Your thread “${t.title}” has been quiet for ${d} days.`,b:`<button class="btn sm" data-act="thrAddSheet" data-id="${t.id}">Add an update</button>`,q:`Summarize my thread “${t.title}” and suggest what I could do next with it.`})});
 const pr=typeof perfectRun==='function'?perfectRun():0;if(pr>=3)out.push({p:8,e:'🏆',t:`${pr} perfect days in a row: every habit kept. Keep it rolling.`,q:`I’ve kept every habit ${pr} days in a row. What’s working, and what should I watch out for?`});
 return out.sort((a,b)=>b.p-a.p).slice(0,3)}
function homeInsightsHTML(){const L=homeInsights();if(!L.length)return'';return`<section class="rv"><div class="st-h"><h3>Insights</h3><button class="link" data-act="askGo">Ask AI</button></div><div class="hins">${L.map(x=>`<div class="hin"><span class="hin-e">${x.e}</span><div class="hin-b"><p>${esc(x.t)}</p><div class="hin-a">${x.b||''}<button class="btn sm ghost" data-act="askQ" data-q="${esc(x.q)}">${ic('ai')}Ask AI</button></div></div></div>`).join('')}</div></section>`}
function homeTop(){return homeQuick()+homeDay()+homeInsightsHTML()}
{const _vT3=vToday;vToday=function(){let o=_vT3();const i=o.indexOf('</header>');return i<0?o:o.slice(0,i+9)+homeTop()+o.slice(i+9)};VIEWS.today=vToday}
{const _tx=todayExtras;todayExtras=function(){return aiPlanCard('home')+_tx()}}
function askWith(q){if(IN.busy)return toast('Still answering the last question');if(IN.res){IN.ctl&&IN.ctl.abort();IN.res=null}IN.q=q;if(!aovOpen()){go('ask');setTimeout(()=>{const t=$('#askIn');if(t)t.value=q;ACT.askSend()},250);return}setTimeout(()=>ACT.askSend(),280)}
Object.assign(ACT,{
 aiStart:()=>{AI.tab='new';go('ai')},aiChangeStart:()=>{AI.tab=S.goals.length?'change':'new';go('ai')},
 aipHide:()=>{S.settings.aipHide=true;save();render(false)},aipShow:()=>{S.settings.aipHide=false;save();render(false)},
 askGo:()=>{if(!aovOpen())go('ask');setTimeout(()=>$('#aov #askIn')?.focus(),380)},askQ:d=>askWith(d.q),dayToday:()=>go('day/'+ymd()),
 askAbout:d=>{const k=d.k;if(k==='goal'){const g=G(d.id);if(g)askWith(`How am I doing with my goal “${g.title}”? What’s working, what’s stuck, and what should I do next?`)}
  else if(k==='habit'){const h=H(d.id);if(h)askWith(`How is my habit “${h.title}” going? When do I keep it, when do I slip, and how can I make it easier?`)}
  else if(k==='thread'){const t=TH(d.id);if(t)askWith(`Look at my thread “${t.title}”. Summarize where it stands and suggest the next few steps.`)}}});
/* threads and activity feed Ask, so answers can use them */
{const _rd=ragDocs;ragDocs=function(){const out=_rd();
 threads().forEach(t=>{const U=thrUps(t).slice().reverse(),lk=thrLink(t);if(!U.length)return;let body=U.map(u=>`${ymd(new Date(u.t))} ${thrTag(u.k)[2]}: ${u.x}`).join(' · ');const head=`Thread “${t.title}”${lk?` (about ${lk.n})`:''}${t.status==='done'?', done':''}`;
  for(let i=0,n=0;i<body.length;i+=620,n++){out.push({id:'t'+t.id+'#'+n,kind:'thread',ref:t.id,gid:t.link&&t.link.k==='goal'?t.link.id:'',date:ymd(new Date(thrLast(t))),label:t.title,head,text:head+(n?' (cont.)':'')+': '+body.slice(i,i+800)});if(i+800>=body.length)break}});
 const A=S.settings.actLog||{};Object.keys(A).sort().slice(-60).forEach(ds=>{const o=A[ds]||{},w=wkMin(o),b=[o.s!=null&&`${(+o.s).toLocaleString()} steps`,o.z&&`slept ${fmtMin(o.z)}`,w&&`worked out ${fmtMin(w)}`,o.m!=null&&`${fmtMin(o.m)} screen time`].filter(Boolean).join(', ');if(b)out.push({id:'a'+ds,kind:'day',ref:ds,gid:'',date:ds,label:'Activity '+ds,head:`${ds} · Activity`,text:`${ds} · Activity: ${b}`})});
 return out}}
{const _is=ACT.insSrc;ACT.insSrc=d=>{const x=IN.res&&IN.res.srcs[+d.i];if(x&&x.kind==='thread'){closeSheet();go('thread/'+x.ref);return}if(x&&x.kind==='day'&&/^a\d/.test(x.id||'')){closeSheet();go('day/'+x.ref);return}_is(d)}}
if(typeof insOverview==='function'){const _io=insOverview;insOverview=function(max,q){let s=_io(max,q);const L=actDays(7).filter(x=>x.s!=null||x.z||x.m!=null);if(L.length){const av=k=>{const v=L.map(x=>x[k]).filter(x=>x>0);return v.length?v.reduce((a,b)=>a+b,0)/v.length:0};s+=`\nActivity, last 7 days: about ${Math.round(av('s')).toLocaleString()} steps a day${av('z')?`, ${fmtMin(av('z'))} sleep`:''}${av('x')?`, ${fmtMin(av('x'))} workouts on active days`:''}${av('m')?`, ${fmtMin(av('m'))} screen time`:''}.`}
 const T=threads().filter(t=>t.status!=='done').slice(0,5);if(T.length)s+=`\nOpen threads: ${T.map(t=>`“${t.title}”${thrUps(t)[0]?` (latest: ${trunc(thrUps(t)[0].x,80)})`:''}`).join('; ')}.`;return max?s.slice(0,max+400):s}}
/* where the app opens: Settings → Look & feel → Start page */
function landOpts(){const pg=[['','Home'],['habits','Habits'],['cal','Calendar'],['goals','Goals'],['vista','Vista'],['journal','Journal'],['threads','Threads'],['ask','Ask'],['activity','Activity'],['day/__today','Today in full']];
 return{pg,th:threads().filter(t=>t.status!=='done').map(t=>['thread/'+t.id,'🧵 '+t.title]),hb:S.habits.filter(h=>h.status!=='archived').map(h=>['habit/'+h.id,hIcon(h)+' '+h.title]),gl:active().map(g=>['goal/'+g.id,'🎯 '+g.title])}}
function landName(v){if(!v)return'Home';const O=landOpts(),f=[...O.pg,...O.th,...O.hb,...O.gl].find(x=>x[0]===v);return f?f[1]:'Home'}
function landPanel(){return`<section class="panel wide rv"><h3>Start page</h3><p class="small muted">The page Plotline opens on. Pick any page, or one thread, habit or goal you check every day.</p><button class="lp-cur" data-act="landPick"><span><small>Opens on</small><b>${esc(trunc(landName(S.settings.landing||''),46))}</b></span>${ic('next','ico-s')}</button></section>`}
function landSheet(){const v=S.settings.landing||'',O=landOpts(),row=([k,n])=>`<button class="lp-o${k===v?' on':''}" data-act="landSet" data-v="${esc(k)}" aria-pressed="${k===v}"><span>${esc(n)}</span>${k===v?ic('check','ico-s'):''}</button>`;
 const grp=(t,L,grid)=>L.length?`<div class="lp-g"><div class="data">${t}</div><div class="${grid?'lp-grid':'lp-list'}">${L.map(row).join('')}</div></div>`:'';
 openSheet(`<div class="data">Start page</div><h2 style="margin-top:6px">Open Plotline on…</h2>${grp('Pages',O.pg,1)}${grp('A thread',O.th)}${grp('A habit',O.hb)}${grp('A goal',O.gl)}`)}
Object.assign(ACT,{landPick:()=>landSheet(),landSet:d=>{S.settings.landing=d.v||'';save();closeSheet();render(false);toast(d.v?`Plotline will open on ${landName(d.v)}`:'Plotline will open on Home')}});
{const _hp=homePanel;homePanel=function(){return landPanel()+_hp()}}
document.addEventListener('change',e=>{if(e.target.matches&&e.target.matches('[data-land]')){S.settings.landing=e.target.value;save();toast(e.target.value?'Plotline will open there':'Plotline will open on Home')}});
function landHere(){const L=S.settings.landing;if(!L)return;const h=location.hash;if(h&&h!=='#'&&h!=='#/'&&h!=='#/today')return;let r=L==='day/__today'?'day/'+ymd():L;const[p,id]=r.split('/');
 if((p==='thread'&&!TH(id))||(p==='habit'&&!H(id))||(p==='goal'&&!G(id)))return;history.replaceState(null,'','#/'+r)}
/* "Ask about this" and "Make this my start page" on goal, habit and thread pages */
{const _r=render;render=function(a){_r(a);const k={goal:'goal',habit:'habit',thread:'thread'}[cur.p];if(!k||!cur.id)return;const bar=document.querySelector('#view .crumb>.ph-r');if(!bar||bar.querySelector('[data-act=askAbout]'))return;
 const g=bar.querySelector('.gearb'),html=`<button class="ibtn" data-act="askAbout" data-k="${k}" data-id="${cur.id}" aria-label="Ask AI about this" title="Ask AI about this">${ic('ai')}</button>`;if(g)g.insertAdjacentHTML('beforebegin',html);else bar.insertAdjacentHTML('beforeend',html)}}

/* ================= reactions wait for the app lock ================= */
{const _rs=reactShow;reactShow=function(){if((LOCKED||document.hidden)&&RB.q.length){clearTimeout(RB.tm);RB.wait=1;const el=$('#rxb');if(el)el.classList.remove('on');return}RB.wait=0;_rs()}}
{const _rb=reactBurst;reactBurst=function(e){if(LOCKED||document.hidden)return;_rb(e)}}
{const _ud=unlockDone;unlockDone=function(){_ud();if(RB.q.length)setTimeout(reactShow,650)}}
document.addEventListener('visibilitychange',()=>{if(!document.hidden&&RB.wait)setTimeout(()=>{if(!LOCKED)reactShow()},700)});

/* ================= ASK OVERLAY: answers slide up over the page you're on ================= */
const AOV={open:false,full:false};
function aovBody(){const R=IN.res;return R?`<div class="ask-thread">${R.turns.map((t,i)=>askTurn(R,t,i)).join('')}</div>`:`<div class="aov-empty"><p class="muted">Ask about your goals, habits, journal, threads and activity. Answers use your own data.</p><div class="aov-sugs">${INS_PRE.slice(0,4).map(p=>`<button class="ask-sug" data-act="askPre" data-k="${p.k}"><b>${p.n}</b><small>${p.d}</small></button>`).join('')}</div></div>`}
function aovGo(){return IN.busy?`<button class="ask-go stop" data-act="insStop" aria-label="Stop"><i></i></button>`:`<button class="ask-go" data-act="askSend" aria-label="Send">${ic('up')}</button>`}
function aovOpen(){if(cur.p==='ask')return false;let el=$('#aov');if(!el){el=document.createElement('div');el.id='aov';el.className='aov';el.innerHTML=`<div class="aov-dim" data-act="aovClose"></div><section class="aov-s" role="dialog" aria-modal="true" aria-label="Ask"><div class="aov-h"><span class="aov-grab" aria-hidden="true"></span><div class="aov-t"><span class="ask-av">${ic('ai')}</span><b>Ask</b><small>${esc(askEngineLabel())}</small></div><button class="ibtn sm" data-act="askNew" aria-label="New chat" title="New chat">${ic('plus')}</button><button class="ibtn sm" data-act="aovFull" aria-label="Open full page" title="Open full page">${ic('full')}</button><button class="ibtn sm" data-act="aovClose" aria-label="Close" title="Close">✕</button></div><div class="aov-b"></div><div class="ask-comp aov-comp"><div class="ask-row"><textarea id="askIn" rows="1" placeholder="Ask anything about your life…" aria-label="Ask"></textarea><span class="aov-go"></span></div></div></section>`;document.body.appendChild(el);aovDrag(el)}
 AOV.open=true;document.documentElement.classList.add('aov-on');aovPaint(true);requestAnimationFrame(()=>el.classList.add('on'));setTimeout(()=>{if(typeof micDecorate==='function')micDecorate(el)},0);return true}
function aovPaint(scroll){const el=$('#aov');if(!el||!AOV.open)return;const b=el.querySelector('.aov-b'),near=b.scrollHeight-b.scrollTop-b.clientHeight<120;b.innerHTML=aovBody();el.querySelector('.aov-go').innerHTML=aovGo();const t=el.querySelector('#askIn');if(t&&document.activeElement!==t)t.value=IN.q||'';if(t)t.placeholder=IN.res?'Ask a follow-up…':'Ask anything about your life…';if(scroll||near||ASK_STICK)b.scrollTop=b.scrollHeight}
function aovClose(){const el=$('#aov');AOV.open=false;document.documentElement.classList.remove('aov-on');if(!el)return;el.classList.remove('on','full');el.querySelector('.aov-s').style.transform='';setTimeout(()=>{if(!AOV.open)el.remove()},360)}
function aovDrag(el){const s=el.querySelector('.aov-s'),h=el.querySelector('.aov-h');let y0=null,dy=0;
 h.addEventListener('pointerdown',e=>{if(e.target.closest('button'))return;y0=e.clientY;dy=0;s.style.transition='none';h.setPointerCapture(e.pointerId)});
 h.addEventListener('pointermove',e=>{if(y0==null)return;dy=e.clientY-y0;s.style.transform=`translateY(${Math.max(dy,-40)}px)`});
 const end=()=>{if(y0==null)return;y0=null;s.style.transition='';s.style.transform='';if(dy>110)aovClose();else if(dy<-50)el.classList.add('full');else if(dy>40&&el.classList.contains('full'))el.classList.remove('full')};
 h.addEventListener('pointerup',end);h.addEventListener('pointercancel',end)}
{const _r=render;render=function(a){_r(a);if(AOV.open){if(cur.p==='ask')aovClose();else aovPaint()}}}
{const _f=askFollow;askFollow=function(){if(AOV.open){if(!ASK_STICK)return;const b=$('#aov .aov-b');if(b)b.scrollTop=b.scrollHeight;return}_f()}}
{const _sc=askScroll;askScroll=function(f){if(AOV.open){if(f)ASK_STICK=true;const b=$('#aov .aov-b');if(b)requestAnimationFrame(()=>{b.scrollTop=b.scrollHeight});return}_sc(f)}}
document.addEventListener('keydown',e=>{if(e.key==='Escape'&&AOV.open&&!$('#sheet').classList.contains('on'))aovClose()});
window.addEventListener('hashchange',()=>{if(AOV.open)aovClose()});
document.addEventListener('scroll',e=>{const b=e.target;if(AOV.open&&b&&b.classList&&b.classList.contains('aov-b')&&IN.busy){if(b.scrollHeight-b.scrollTop-b.clientHeight>80)ASK_STICK=false}},true);
Object.assign(ACT,{aovClose:()=>aovClose(),aovFull:()=>{aovClose();go('ask')}});

{const _an=ACT.askNew;ACT.askNew=d=>{if(AOV.open){IN.ctl&&IN.ctl.abort();IN.res=null;IN.busy=false;IN.q='';IN.prog='';aovPaint(true);setTimeout(()=>$('#aov #askIn')?.focus(),60);return}_an(d)}}

/* ================= LIQUID GLASS themes ================= */
THEMES.push(['glass','Liquid Glass','#10141F','#2A3042','#8FD8FF',0],['glasslight','Liquid Glass Light','#E8EEF9','#FFFFFF','#0A84FF',0,1]);
THEME_IDS.push('glass','glasslight');

/* ================= THREADS WIDGET: data for the home screen, and what it typed or said ================= */
function widgetThreads(){return{th:widgetTheme(),thr:threads().filter(t=>t.status!=='done').sort((a,b)=>thrLast(b)-thrLast(a)).slice(0,12).map(t=>{const u=thrUps(t)[0];return{id:t.id,t:t.title,x:u?trunc(u.x,90):'',e:u?thrTag(u.k)[1]:'🧵',at:thrLast(t)}})}}
function thrFromQueue(a){const k=['note','idea','prog','block','done'].includes(a.kind)?a.kind:'note',x=String(a.x||'').trim().slice(0,2000),t0=+a.t||Date.now(),qid=String(a.qid||'');
 if(a.k==='thrnew'){const title=String(a.title||'').trim().slice(0,80)||trunc(x,60);if(!title)return false;if(TH(a.nid))return false;const t={id:a.nid||uid(),title,link:null,status:'open',created:t0,u:Date.now(),ups:[]};if(x)t.ups.push({id:qid||uid(),t:t0,k,x,u:Date.now()});threads().push(t);return true}
 const t=TH(a.id);if(!t||!x)return false;if(qid&&t.ups.some(u=>u.id===qid))return false;t.ups.push({id:qid||uid(),t:t0,k,x,u:Date.now()});if(t.status==='done')t.status='open';t.u=Date.now();return true}

/* ================= SETTINGS: every area's sections fold to their titles ================= */
const foldSet=()=>S.settings.fold||(S.settings.fold={});
function foldSettings(){if(cur.p!=='settings'||!cur.id)return;const P=[...document.querySelectorAll('#view .set > section.panel')].filter(x=>x.firstElementChild&&x.firstElementChild.tagName==='H3');if(P.length<2)return;const F=foldSet();
 P.forEach((sec,i)=>{if(sec.classList.contains('fold'))return;const h=sec.firstElementChild,key=cur.id+':'+h.textContent.trim().slice(0,40);const open=window.__allOpen||(key in F?F[key]:i===0);
  const body=document.createElement('div');body.className='fold-b';const inner=document.createElement('div');while(h.nextSibling)inner.appendChild(h.nextSibling);body.appendChild(inner);sec.appendChild(body);
  sec.classList.add('fold');sec.classList.toggle('open',open);sec.dataset.fk=key;h.setAttribute('role','button');h.setAttribute('tabindex','0');h.setAttribute('aria-expanded',open);h.insertAdjacentHTML('beforeend',ic('chev','fold-c'))})}
function foldToggle(h){const sec=h.closest('.fold');if(!sec)return;const o=!sec.classList.contains('open');sec.classList.toggle('open',o);h.setAttribute('aria-expanded',o);foldSet()[sec.dataset.fk]=o;save()}
document.addEventListener('click',e=>{const h=e.target.closest&&e.target.closest('.fold > h3');if(h)foldToggle(h)});
document.addEventListener('keydown',e=>{if((e.key==='Enter'||e.key===' ')&&e.target.matches&&e.target.matches('.fold > h3')){e.preventDefault();foldToggle(e.target)}});
{const _r=render;render=function(a){_r(a);foldSettings()}}

/* ================= dropdowns on phones open an in-app picker, not Android's system list ================= */
const SELP_ON=()=>IS_PHONE||matchMedia('(pointer:coarse)').matches;
function selLabel(sel){const id=sel.id&&document.querySelector(`label[for="${sel.id}"]`);const f=sel.closest('.field');const l=id||(f&&f.querySelector('label'));return(sel.getAttribute('aria-label')||(l&&l.textContent)||'Choose').trim()}
function selOpen(sel){if(sel.disabled)return;const opt=o=>`<button class="selp-o${o.value===sel.value?' on':''}" data-v="${esc(o.value)}" ${o.disabled?'disabled':''}><span>${esc(o.textContent.trim())}</span>${o.value===sel.value?ic('check','ico-s'):''}</button>`;
 let body='';[...sel.children].forEach(c=>{if(c.tagName==='OPTGROUP')body+=`<div class="selp-g data">${esc(c.label)}</div>`+[...c.children].map(opt).join('');else if(c.tagName==='OPTION')body+=opt(c)});
 const el=document.createElement('div');el.className='selp';el.innerHTML=`<div class="selp-dim"></div><section class="selp-s" role="dialog" aria-modal="true" aria-label="${esc(selLabel(sel))}"><span class="selp-grab" aria-hidden="true"></span><div class="selp-h"><b>${esc(selLabel(sel))}</b><button class="ibtn sm" aria-label="Close">✕</button></div><div class="selp-l">${body}</div></section>`;
 document.body.appendChild(el);requestAnimationFrame(()=>{el.classList.add('on');const on=el.querySelector('.selp-o.on');if(on)on.scrollIntoView({block:'center'})});
 const close=()=>{el.classList.remove('on');setTimeout(()=>el.remove(),300)};
 el.addEventListener('click',e=>{const b=e.target.closest('.selp-o');if(b){if(sel.value!==b.dataset.v){sel.value=b.dataset.v;sel.dispatchEvent(new Event('input',{bubbles:true}));sel.dispatchEvent(new Event('change',{bubbles:true}))}close();return}if(e.target.closest('.selp-dim,.selp-h .ibtn'))close()})}
{let T=null;document.addEventListener('touchstart',e=>{const s=e.target.closest&&e.target.closest('select');if(!s||s.multiple||s.disabled||!SELP_ON())return;e.preventDefault();T={s,x:e.touches[0].clientX,y:e.touches[0].clientY}},{capture:true,passive:false});
 document.addEventListener('touchend',e=>{if(!T)return;const t=e.changedTouches[0],s=T.s,m=Math.hypot(t.clientX-T.x,t.clientY-T.y);T=null;if(m<12){e.preventDefault();selOpen(s)}},{capture:true,passive:false})}
document.addEventListener('mousedown',e=>{const s=e.target.closest&&e.target.closest('select');if(!s||s.multiple||!SELP_ON())return;e.preventDefault();s.blur();selOpen(s)},true);
document.addEventListener('keydown',e=>{if(e.key==='Escape'){const p=document.querySelector('.selp.on');if(p){p.classList.remove('on');setTimeout(()=>p.remove(),300)}}});

/* ================= wiring ================= */
function todayExtras(){if(nat())actLoad(false);return actCardHTML()+thrToday()}
VIEWS.journal=vJournalW;VIEWS.threads=vThreads;VIEWS.thread=vThread;VIEWS.day=vDay;VIEWS.activity=vActivity;
/* 1.13: crumb action rows that don't fit scroll, starting at the right (gear visible) */
function crumbFit(){document.querySelectorAll('#view .crumb>.ph-r').forEach(r=>{const o=r.scrollWidth>r.clientWidth+2;r.classList.toggle('ovf',o);if(o)r.scrollLeft=r.scrollWidth})}
{const _r=render;render=function(a){_r(a);crumbFit()}}addEventListener('resize',()=>crumbFit(),{passive:true});
function sbarTop(){document.documentElement.classList.toggle('pg-top',scrollY<6)}
addEventListener('scroll',sbarTop,{passive:true});{const _r=render;render=function(a){_r(a);sbarTop()}}
/* 1.13: pull-to-refresh must never start inside the Ask overlay, pickers or banners (it hijacked their scrolling) */
document.addEventListener('touchstart',e=>{const t=e.target;if(document.documentElement.classList.contains('aov-on')||(t&&t.closest&&t.closest('#aov,.selp,.rxb,#sheet,.ovl,[data-noptr]')))PTR=null},{passive:true});
/* ===== 1.13: Colours & background (accent, wallpaper, strength, frost, motion) ===== */
const LK_WALLS=[['off','None'],['aurora','Aurora'],['ocean','Ocean'],['sunset','Sunset'],['forest','Forest'],['rose','Rose'],['mono','Mono'],['accent','Accent']];
const LK_ACCENTS=['#FFB547','#FF7A59','#FF5C8A','#C77DFF','#8B7CFF','#2E6BFF','#0A84FF','#38BDF8','#2DD4BF','#46C99B','#84CC16','#E8E9EB'];
function wallOf(){const st=S.settings,t=st.theme;if(st.wall&&LK_WALLS.some(w=>w[0]===st.wall))return st.wall;return(t==='glass'||t==='glasslight')?'aurora':'off'}
function hexLum(h){const m=/^#?([0-9a-f]{6})$/i.exec(h||'');if(!m)return .5;const n=parseInt(m[1],16),f=v=>{v/=255;return v<=.03928?v/12.92:Math.pow((v+.055)/1.055,2.4)};return .2126*f(n>>16&255)+.7152*f(n>>8&255)+.0722*f(n&255)}
function lookApply(){const st=S.settings,r=document.documentElement;r.dataset.wall=wallOf();r.dataset.wallmove=st.wallMove?'on':'off';
 r.style.setProperty('--wop',((st.wallStr==null?55:+st.wallStr)/100).toFixed(2));r.style.setProperty('--frost',((st.frost==null?50:+st.frost)/100).toFixed(2));
 if(st.accent&&/^#[0-9a-f]{6}$/i.test(st.accent)){r.style.setProperty('--accent',st.accent);r.style.setProperty('--on-accent',hexLum(st.accent)>.4?'#0B0F1A':'#FFFFFF')}else{r.style.removeProperty('--accent');r.style.removeProperty('--on-accent')}
 if(typeof pushWidget==='function')clearTimeout(lookApply.t),lookApply.t=setTimeout(()=>{try{pushWidget()}catch(e){}},400)}
{const _at=applyTheme;applyTheme=function(a){_at(a);lookApply();if(a&&document.startViewTransition)setTimeout(lookApply,30)}}
function colorPanel(){const st=S.settings,w=wallOf(),glass=st.theme==='glass'||st.theme==='glasslight',ws=st.wallStr==null?55:+st.wallStr,fr=st.frost==null?50:+st.frost;
 return`<section class="panel wide rv lk-p"><h3>Colours &amp; background</h3><p class="small muted">Make Plotline yours. These sit on top of any theme, and the widgets on your home screen follow them.</p>
 <div class="data tgl">Accent</div><div class="lk-sw"><button class="lk-c auto${st.accent?'':' on'}" data-act="lkAccent" data-v="" aria-pressed="${!st.accent}" title="Theme colour"><span>Auto</span></button>${LK_ACCENTS.map(c=>`<button class="lk-c${st.accent===c?' on':''}" data-act="lkAccent" data-v="${c}" style="--sw:${c}" aria-label="Accent ${c}" aria-pressed="${st.accent===c}"></button>`).join('')}<label class="lk-c pick${st.accent&&!LK_ACCENTS.includes(st.accent)?' on':''}" style="--sw:${st.accent||'#888'}" title="Pick any colour"><input type="color" data-lk="accent" value="${st.accent||'#8FD8FF'}" aria-label="Custom accent">${ic('edit')}</label></div>
 <div class="data tgl">Background</div><div class="lk-walls">${LK_WALLS.map(([k,n])=>`<button class="lk-w${w===k?' on':''}" data-act="lkWall" data-v="${k}" data-wp="${k}" aria-pressed="${w===k}"><i></i><span>${n}</span></button>`).join('')}</div>
 <label class="lk-r${w==='off'?' dis':''}"><span>Background strength<small>${ws<30?'Subtle':ws<70?'Balanced':'Vivid'}</small></span><input type="range" min="10" max="100" step="5" value="${ws}" data-lk="wallStr" ${w==='off'?'disabled':''}></label>
 ${glass?`<label class="lk-r"><span>Glass frost<small>${fr<35?'Clear':fr<70?'Frosted':'Milky'}</small></span><input type="range" min="0" max="100" step="5" value="${fr}" data-lk="frost"></label>`:''}
 <label class="sw"><span>Move the background<small>A slow drift. Off saves battery and keeps scrolling smooth</small></span><input type="checkbox" data-lk="wallMove" ${st.wallMove?'checked':''} ${w==='off'?'disabled':''}><i></i></label>
 ${st.accent||st.wall||st.wallStr!=null||st.frost!=null||st.wallMove?`<button class="btn ghost sm" data-act="lkReset">Reset to theme defaults</button>`:''}</section>`}
{const _hp=homePanel;homePanel=function(){return colorPanel()+_hp()}}
Object.assign(ACT,{lkAccent:(d)=>{S.settings.accent=d.v||'';save();applyTheme();render(false)},lkWall:(d)=>{S.settings.wall=d.v;save();applyTheme();render(false)},
 lkReset:()=>{['accent','wall','wallStr','frost','wallMove'].forEach(k=>delete S.settings[k]);save();applyTheme();render(false)}});
document.addEventListener('input',e=>{const t=e.target;if(!t||!t.dataset||!t.dataset.lk)return;const k=t.dataset.lk;
 if(k==='accent'){S.settings.accent=t.value;lookApply();const l=t.closest('.lk-c');if(l)l.style.setProperty('--sw',t.value)}
 else if(k==='wallMove'){S.settings.wallMove=t.checked;lookApply()}
 else{S.settings[k]=+t.value;lookApply();const sm=t.parentElement.querySelector('small');if(sm)sm.textContent=k==='wAlpha'?(t.value>=95?'Solid':t.value>=75?'Slightly see-through':'See-through'):k==='frost'?(t.value<35?'Clear':t.value<70?'Frosted':'Milky'):(t.value<30?'Subtle':t.value<70?'Balanced':'Vivid')}
 clearTimeout(lookApply.s);lookApply.s=setTimeout(save,300)});
document.addEventListener('change',e=>{const t=e.target;if(t&&t.dataset&&t.dataset.lk==='accent'){save();render(false)}});
lookApply();
/* ===== 1.13: snappy navigation =====
   Tab switches used to run a full-page view transition (0.55s) and then fade every section in with a stagger
   (0.8–1s each, up to 0.54s delay), so a page looked "slow" even though it rendered in ~20ms. Now, between top-level
   pages, the swap is instant and what's on screen appears with one short fade. Below-the-fold sections still
   reveal on scroll, just quicker. The hero morph (goal card -> goal page) keeps its view transition.
   Settings > Look & feel > Animations can switch back to the expressive style. */
const NAV_TOP=new Set(['today','habits','cal','goals','road','map','vista','journal','threads','ask','settings','activity']);
function animRich(){return S.settings.anim==='rich'}
function navFast(p){return !animRich()&&NAV_TOP.has(p)&&NAV_TOP.has(cur.p)}
{const _rv=reveal;reveal=function(anim){_rv(anim);if(animRich()||!anim)return;const H=innerHeight;document.querySelectorAll('#view .rv:not(.in)').forEach(e=>{if(e.getBoundingClientRect().top<H){e.style.transitionDelay='0ms';e.classList.add('in')}})}}
function animApply(){document.documentElement.dataset.anim=animRich()?'rich':'snappy'}
{const _at=applyTheme;applyTheme=function(a){_at(a);animApply()}}animApply();
{const _hp=homePanel;homePanel=function(){const st=S.settings;return _hp()+`<section class="panel wide rv"><h3>Animations</h3><p class="small muted">How pages appear when you move around.</p>${segHTML('anim','animSet',[['snappy','Snappy'],['rich','Expressive']],animRich()?'rich':'snappy')}<p class="small muted" style="margin-top:10px">${animRich()?'Pages slide and fade in, section by section.':'Pages switch instantly with a light fade. Best on older phones.'}</p></section>`}}
ACT.animSet=(d)=>{S.settings.anim=d.v;save();animApply();render(false)};
/* ===== 1.13: home-screen widgets follow the app's theme, accent and chosen widget opacity ===== */
function cssRGBA(v){const pr=document.createElement('i');pr.style.cssText='position:absolute;visibility:hidden;color:'+v;document.body.appendChild(pr);const c=getComputedStyle(pr).color;pr.remove();
 let m=c.match(/rgba?\(([^)]+)\)/);if(m){const p=m[1].split(/[ ,\/]+/).filter(Boolean).map(Number);return[p[0],p[1],p[2],p.length>3?p[3]:1]}
 m=c.match(/color\(srgb ([^)]+)\)/);if(m){const p=m[1].split(/[ \/]+/).filter(Boolean).map(Number);return[p[0]*255,p[1]*255,p[2]*255,p.length>3?p[3]:1]}return[128,128,128,1]}
function wArgb(c,a){const h=x=>Math.max(0,Math.min(255,Math.round(x))).toString(16).padStart(2,'0');return'#'+h((a==null?c[3]:a)*255)+h(c[0])+h(c[1])+h(c[2])}
function wMix(a,b,t){return[a[0]+(b[0]-a[0])*t,a[1]+(b[1]-a[1])*t,a[2]+(b[2]-a[2])*t,1]}
function widgetTheme(){const bg=cssRGBA('var(--bg-2)'),bg0=cssRGBA('var(--bg)'),tx=cssRGBA('var(--text)'),mu=cssRGBA('var(--muted)'),ac=cssRGBA('var(--accent)'),on=cssRGBA('var(--on-accent)');
 const light=document.documentElement.dataset.mode==='light',op=(S.settings.wAlpha==null?94:+S.settings.wAlpha)/100,muO=wMix(bg,mu,mu[3]),dim=wMix(bg,tx,.32);
 return{light,bg:wArgb(wMix(bg0,bg,.6),op),card:wArgb(wMix(bg,tx,light?.05:.07),1),row:wArgb(tx,light?.06:.07),chip:wArgb(tx,light?.09:.12),text:wArgb(tx,1),muted:wArgb(muO,1),dim:wArgb(dim,1),accent:wArgb(ac,1),on:wArgb(on,1)}}
{const _cp=colorPanel;colorPanel=function(){const h=_cp(),wa=S.settings.wAlpha==null?94:+S.settings.wAlpha;return h.replace('<label class="sw"><span>Move the background',`<label class="lk-r"><span>Widget background<small>${wa>=95?'Solid':wa>=75?'Slightly see-through':'See-through'}</small></span><input type="range" min="40" max="100" step="2" value="${wa}" data-lk="wAlpha"></label><label class="sw"><span>Move the background`)}}
/* Vista: an axis label sitting under the NOW pill is unreadable; nudge it to the right of the pill */
function roadAxisFix(){const n=document.querySelector('#view .today-line>span');if(!n)return;const nr=n.getBoundingClientRect();document.querySelectorAll('#view .fl-ax .fl-tk').forEach(t=>{t.style.removeProperty('translate');const r=t.getBoundingClientRect();if(r.right>nr.left-4&&r.left<nr.right+4&&r.bottom>nr.top&&r.top<nr.bottom)t.style.translate=(nr.right+8-r.left)+'px 0'})}
{const _r=render;render=function(a){_r(a);if(cur.p==='road')requestAnimationFrame(roadAxisFix)}}
/* horizontal chip/tab rows that scroll: fade the edge that has more, so a cut-off tab reads as "scroll for more" */
function edgeFade(){document.querySelectorAll('#view .jchips,#view .seg-c,#view .hmq').forEach(el=>{const sc=el.scrollWidth>el.clientWidth+2;if(!sc){el.classList.remove('fade-r','fade-l');return}const f=()=>{el.classList.toggle('fade-r',el.scrollLeft+el.clientWidth<el.scrollWidth-4);el.classList.toggle('fade-l',el.scrollLeft>4)};f();if(!el.__ef){el.__ef=1;el.addEventListener('scroll',f,{passive:true})}})}
{const _r=render;render=function(a){_r(a);edgeFade()}}addEventListener('resize',()=>edgeFade(),{passive:true});
/* ===== 1.14: native app shell =====
   On Android the screens are native; this page runs hidden underneath as the engine (Drive sync, reminders,
   widgets, AI, sharing) and shows "classic" screens that aren't native yet. Both read and write one copy of
   the data that lives in the app (NATIVE.storeGet / storeSet). */
const NSHELL=!!(NATIVE&&NATIVE.shellMode&&(()=>{try{return NATIVE.shellMode()}catch(e){return false}})());
if(NSHELL){window.__NSHELL=true;document.documentElement.classList.add('nshell');S.settings.tourOffered=true}
{const _g=DB.get.bind(DB),_s=DB.set.bind(DB);
 DB.get=async k=>{if(k==='state'&&NSHELL){try{const t=NATIVE.storeGet();if(t){const d=JSON.parse(t);if(d&&d.goals){if(d.settings)d.settings.tourOffered=true;return d}}}catch(e){}const d=await _g(k);if(d&&d.goals){if(d.settings)d.settings.tourOffered=true;try{NATIVE.storeSet(JSON.stringify(d))}catch(e){}}return d}return _g(k)};
 DB.set=(k,v)=>{if(k==='state'&&NSHELL){try{NATIVE.storeSet(JSON.stringify(v))}catch(e){}}return _s(k,v)}}
window.__nativeChanged=()=>{if(!NSHELL)return;if(SYNCING){SYNCING.finally(()=>setTimeout(window.__nativeChanged,80));return}
 try{const t=NATIVE.storeGet();if(!t)return;const d=JSON.parse(t);if(!d||!d.goals)return;S=norm(d);if(NSHELL)S.settings.tourOffered=true;primeSig();DIRTY=true;
  _s_idb(S);try{applyTheme(false);applyFonts()}catch(e){}syncNative();pushWidget();syncSoon(1200);if(!$('#sheet').classList.contains('on'))render(false)}catch(e){console.warn('nativeChanged',e)}};
/* keep the browser copy in step too (without bouncing back to the app) */
function _s_idb(v){try{indexedDB&&DB.open().then(db=>{const tx=db.transaction('kv','readwrite');tx.objectStore('kv').put(v,'state')}).catch(()=>{})}catch(e){}}
window.__nback=()=>{try{if(typeof AOV!=='undefined'&&AOV.open){aovClose();return true}if($('#sheet').classList.contains('on')){if(!SHEET_LOCK)closeSheet();return true}if(LOCKED)return true}catch(e){}return false};
window.__nlock=()=>{if(lockActive()){if(!LOCKED)showLock()}else{try{NATIVE.unlocked()}catch(e){}}};
{const _ud=unlockDone;unlockDone=function(){_ud();if(NSHELL)try{NATIVE.unlocked()}catch(e){}}}
/* ===== 2.0.1: native screens run any web action (reminders, auto check-off, focus timer, routines, weekly review…) ===== */
window.__nact=(route,act,data,one)=>{try{closeSheet();RP=null;SHEET_LOCK=false;if(route&&location.hash!=='#/'+route)go(route);
 window.__nOne=!!one;setTimeout(()=>{const f=ACT[act];if(!f){console.warn('nact?',act);if(one)try{NATIVE.closeClassic()}catch(e){}return}const el=document.createElement('button');Object.assign(el.dataset,data||{});f(el.dataset,el,{})},route?420:30);return true}catch(e){console.warn('nact',e);return false}};
{const _cs=closeSheet;closeSheet=function(){_cs.apply(this,arguments);if(window.__nOne&&NSHELL)setTimeout(()=>{if(window.__nOne&&!$('#sheet').classList.contains('on')&&!(typeof AOV!=='undefined'&&AOV.open)){window.__nOne=false;try{NATIVE.closeClassic()}catch(e){}}},320)}}
/* 2.0.1: data the native Home asks the engine for (insights use the web's own rules) */
window.__nhome=()=>{try{const ins=homeInsights().map(x=>{const b=x.b||'',m=/data-act="([A-Za-z]+)"(?: data-id="([^"]*)")?/.exec(b),hr=/href="#\/([a-z]+)"/.exec(b);return{e:x.e,t:x.t,q:x.q,a:m?m[1]:hr?'go:'+hr[1]:'',i:(m&&m[2])||'',l:b.replace(/<[^>]*>/g,'').trim()}});
 return JSON.stringify({ins,demo:demoCount(),sh:(typeof shTodayCard==='function'&&shTodayCard())?1:0})}catch(e){return JSON.stringify({err:String(e)})}};
/* 2.0.5: native Settings asks the engine for the strings only it knows, and hands it files / lock changes */
window.__nsub=()=>{try{return JSON.stringify({auto:autoSub(),share:shareSub(),voice:voiceSub(),lock:lockSub(),msg:SYNC_MSG||'',on:!!syncOn(),demo:demoCount()})}catch(e){return JSON.stringify({err:String(e)})}};
Object.assign(ACT,{nImport:d=>{try{const t=NATIVE.importText();FILE[d.k]({files:[new Blob([t],{type:'application/json'})],value:''})}catch(e){toast('Couldn’t read that file')}},
 nLock:d=>lockApply(d.f,d.v==='1'?true:d.v==='0'?false:d.v)});
/* 2.0.5: the native Ask page runs on the engine: it reads the chat state and sends questions through the web's own flow */
window.__nask=w=>{try{if(w)setTimeout(natWarm,300);const R=IN.res,s=insSet(),n=R?R.srcs.length:0,sc=((INS_WIN.find(x=>x[0]===IN.win)||INS_WIN[3])[1])+(IN.goal&&G(IN.goal)?' · '+trunc(G(IN.goal).title,22):'');
 return JSON.stringify({lab:askEngineLabel(),busy:!!IN.busy,prog:IN.prog||'',has:!!R,pending:!!(R&&R.pending),eng:R?R.engine:s.engine,n,scope:sc,win:IN.win,name:S.settings.name||'',sheet:$('#sheet').classList.contains('on'),
  turns:R?R.turns.map(t=>({q:t.q,a:t.a||'',done:!!t.done,err:t.err||'',off:!!t.off})):[],
  srcs:R?R.srcs.map(d=>({kind:d.kind,ref:d.ref,label:d.label,date:d.date||'',mood:d.mood||'',text:d.text||''})):[],
  sugs:INS_PRE.map(p=>({k:p.k,n:p.n,d:p.d})),ideas:askIdeas(),stats:insStats(IN.win,IN.goal).map(x=>({v:String(x.v),k:x.k,hl:!!x.hl,t:x.t||''}))})}catch(e){return JSON.stringify({err:String(e)})}};
window.__nasksend=q=>{try{window.__nOne=true;if(location.hash!=='#/ask')go('ask');IN.q=String(q||'');ACT.askSend()}catch(e){console.warn(e)}};
window.__naskpaste=t=>{try{IN.paste=String(t||'');ACT.insPasted()}catch(e){}};
/* native year in review: the data behind the story slides */
window.__nyear=y=>{try{const D=yearData(+y||yearDefault());return JSON.stringify({y:D.y,name:S.settings.name||'',chs:D.chs.map(c=>({e:c.emoji||'',t:c.title,c:c.color})),done:D.done.length,doneTop:D.done[0]?D.done[0].title:'',started:D.started,steps:D.steps,checks:D.checks,topH:D.topH&&D.topH.best?{best:D.topH.best,t:D.topH.h.title,i:hIcon(D.topH.h)}:null,steady:D.steady?{t:D.steady.h.title,r:D.steady.rate}:null,free:D.free&&D.free.best?{t:D.free.h.title.replace(/^no /i,''),b:D.free.best}:null,M:D.M,best:D.bestM&&D.bestM[1]?D.bestM[0]:-1,notes:D.notes,words:D.words,mood:D.mt.length?D.mt[0][0]:'',pos:D.pos,qs:D.qs.map(q=>({s:q.s,t:q.t})),active:D.active})}catch(e){return ''}};
window.__nyearDef=()=>yearDefault();
/* native habit vista: per-day state strings for every lane (see hvState) + the hero numbers */
window.__nhv=()=>{try{const L=hvHabits(),tn=dnum(ymd());if(!L.length)return JSON.stringify({lanes:[]});const st=Math.min(tn-6,...L.map(hvStart)),a=Math.max(tn-730,st-1);
 const lanes=L.map((h,i)=>{const sx=hvLaneStats(h);let s='';for(let k=a;k<=tn;k++)s+=hvState(h,k,tn);return{id:h.id,t:h.title,i:hIcon(h),paused:h.status==='paused',quit:h.kind==='quit',big:sx.big,unit:sx.unit,kept:sx.kept,ci:i%7,st:s,todo:s[s.length-1]!=='d'&&!!hCounts(h,ymd())}});
 const wk=[...Array(7)].map((_,i)=>dayRatio(fromN(tn-i))).reduce((o,r)=>({due:o.due+r.due,dn:o.dn+r.dn}),{due:0,dn:0}),best=L.filter(h=>h.kind!=='quit'&&h.status==='active').map(h=>[h,hStreak(h)]).sort((x,y)=>y[1]-x[1])[0];
 let m30=0;L.forEach(h=>{if(h.kind!=='quit')for(let k=tn-29;k<=tn;k++)if(hDone(h,fromN(k)))m30++});
 return JSON.stringify({a,tn,hvd:hvDays(),lanes,due:wk.due,dn:wk.dn,best:best&&best[1]?{t:best[0].title,n:best[1]}:null,m30,pr:perfectRun()})}catch(e){return ''}};
/* native automations + sharing settings bodies */
window.__nauto=()=>{try{const st=astat();return JSON.stringify({native:!!NATIVE,st,places:places().map(p=>({id:p.id,name:p.name,e:p.emoji||'📍',r:p.r})),rules:autoRules().map(r=>({e:AUTO_T[r.type].e,t:r.title,l:autoLabel(r)}))})}catch(e){return ''}};
window.__nshare=()=>{try{const s=shset(),ok=fsOK(),sy=!!syncOn(),L=shares(),M=x=>(MODES[x.mode]||{});return JSON.stringify({ok,sync:sy,on:!!s.on,allow:!!s.allow,allowHide:!!s.allowHide,me:fme()||'',
 inv:L.filter(x=>x.status==='invited'||x.status==='later').map(x=>({id:x.id,who:shWho(x),locked:!!x.locked,from:x.from||'',e:M(x).e||'',m:M(x).n||'Shared',t:x.title,later:x.status==='later'})),
 on_:L.filter(x=>x.status==='joined').map(x=>({id:x.id,e:M(x).e||'🤝',t:x.title,m:M(x).n||'',own:x.role==='owner',from:x.fromName||x.from||''})),
 old:L.filter(x=>x.status==='legacy').map(x=>({id:x.id,e:M(x).e||'🤝',t:x.title,m:M(x).n||''}))})}catch(e){return ''}};
Object.assign(ACT,{nShareOn:d=>shareOn(d.v==='1')});
/* native Plan with AI: state + edits, the planner itself is the web's (plangen.js) */
const _tx=h=>String(h||'').replace(/<[^>]+>/g,' ').replace(/\s+/g,' ').trim();
window.__nai=()=>{try{const e=planEngine(),ch=AI.tab==='change',o=AI.plan,up=!!(o&&o.mode==='update'),M=!up&&AI.main;
 const n=(AIG.out.match(/"title"\s*:/g)||[]).length,g=(AIG.out.match(/"action"\s*:/g)||[]).length;
 const B={add:'New',update:'Update',remove:'Remove',keep:'No change'};let rows=[],main=null;
 if(o){const pm=new Map(o.goals.map(x=>[x.id,x.parent||null])),depth=x=>{let d=0,p=pm.get(x.id);while(p&&pm.has(p)&&d<12){d++;p=pm.get(p)}return d};
  const order=up?o.goals.map((x,i)=>i):treeOrder(o.goals.map((x,i)=>({id:x.id,parent:x.parent||'',i}))).map(t=>t.g.i).filter(i=>!(M&&o.goals[i].id===M.rid));
  rows=order.map(i=>{const x=o.goals[i],dp=M?depth(x)-(M.wrap?0:1)+1:0,ex=G(x.id),edit=x.action==='add'||x.action==='update',area=areaKey(x.area)||(ex&&ex.area)||'personal';
   const par=up&&x.parent&&(G(x.parent)||o.goals.find(y=>y.id===x.parent));
   return{i,t:edit?x.title:(ex?ex.title:x.id),act:x.action,badge:B[x.action],area,az:areaName(area),hz:edit?hzName(x.horizon):'',
    dt:edit?((x.startInDays!=null?fmtDate(addDays(x.startInDays)):'')+(x.targetInDays!=null?' → '+fmtDate(addDays(x.targetInDays)):'')):'',
    det:x.action==='update'&&ex?_tx(diffGoal(ex,x)):'',rm:x.action==='remove',par:par?trunc(par.title,40):'',dp:Math.max(0,dp),keep:x.action==='keep',sel:AI.sel.has(i),
    steps:edit?x.steps.map(s=>({t:s.title,d:s.dueInDays!=null?fmtDate(addDays(s.dueInDays))+(s.dueTime?' '+fmtTime(s.dueTime):''):'',b:s.reminder!=='none',nw:up&&s.id==='new'})):[]}});
  if(M){const r=M.rid&&o.goals.find(x=>x.id===M.rid);main={title:M.title,why:M.why,area:areaName(M.area),horizon:M.horizon,hzn:hzName(M.horizon),target:M.target||'',img:M.img||'',wrap:!!M.wrap,steps:r?r.steps.map(s=>({t:s.title,d:s.dueInDays!=null?fmtDate(addDays(s.dueInDays)):''})):[]}}}
 return JSON.stringify({sheet:$('#sheet').classList.contains('on'),tab:AI.tab,text:AI.text,change:AI.change,reply:AI.reply,eng:e,engName:e?planEngineName():'',busy:!!AIG.busy,prog0:0,prog:AIG.prog||(g?`${g} goal${g===1?'':'s'}, ${Math.max(0,n-g)} step${n-g===1?'':'s'} so far`:'Reading what you wrote…'),aerr:AIG.err||'',errs:AI.errs||[],has:!!o,up,hasGoals:S.goals.length>0,main,rows,nsel:AI.sel.size,spec:SPEC(ch?'update':'create'),
  areas:[...new Set([...AREAS.map(a=>a.name),...S.goals.map(g=>areaName(g.area))])],hzs:HORIZONS})}catch(e){return JSON.stringify({err:String(e)})}};
window.__naiset=(a,b,c)=>{AI.text=String(a||'');AI.change=String(b||'');AI.reply=String(c||'')};
window.__naitab=v=>{ACT.aiTab({v})};
window.__naitoggle=i=>{AI.sel.has(+i)?AI.sel.delete(+i):AI.sel.add(+i)};
window.__naititle=(i,t)=>{if(AI.plan){const g=AI.plan.goals[+i];if(g)g.title=String(t||'')}};
window.__naimain=(f,v)=>{if(!AI.main)return;AI.main[f]=f==='area'?(areaKey(v)||'personal'):v};
window.__naiprompt=k=>{const ch=AI.tab==='change',t=ch?AI.change:AI.text;if(k==='spec')return SPEC(ch?'update':'create');if(k==='fix')return fixPrompt();if(!t.trim())return'';return ch?updatePrompt(t):createPrompt(t)};
Object.assign(ACT,{parseAI:()=>{const r=$('#aiReply');if(r)AI.reply=r.value;const mode=AI.tab==='change'?'update':'create';AI.plan=null;AI.errs=[];try{const o=extractPlan(AI.reply);const errs=validatePlan(o,mode);if(errs.length)AI.errs=errs;else{AI.plan=o;AI.sel=new Set(o.goals.map((x,i)=>x.action==='keep'?-1:i).filter(i=>i>=0));AI.main=null;if(mode==='create')aiMain(o)}}catch(e){AI.errs=e.errs||[String(e.message||e)]}render(false);setTimeout(()=>document.querySelector(AI.plan?'.pv':'.errbox')?.scrollIntoView({behavior:'smooth',block:'center'}),60)}});
/* native guided tour: the engine pauses saving, syncing and reminders (TOURING) and hands back the example data */
window.__ntourBegin=()=>{try{if(TOURING)return'';closeSheet();RP=null;URGE=null;const st=S.settings;TR={prev:JSON.stringify(S),back:'today',hv:HV,i:-1,timers:[],pos:0};TOURING=true;
 S=norm({settings:{...JSON.parse(JSON.stringify(st)),sync:{...st.sync,on:false},timer:null,pinHash:''}});seedDemo();
 const run=S.goals.find(g=>g.title==='Run a 10K race');if(run){const sub=newGoal({title:'Build strength for race day',area:'health',horizon:'month',parent:run.id,targetDate:addDays(40),steps:[{title:'Two leg days a week',due:addDays(4)},{title:'Core routine after each run',due:addDays(12)}]});S.goals.push(sub)}
 return JSON.stringify(S)}catch(e){TOURING=false;TR=null;return''}};
window.__ntourEnd=()=>{try{if(!TR)return;const t=TR;TR=null;TOURING=false;S=norm(JSON.parse(t.prev));HV=t.hv||'today';primeSig();S.settings.toured=true;S.settings.tourOffered=true;save();applyTheme()}catch(e){TOURING=false;TR=null}};
/* native first run: welcome choice + the one-time setup list */
window.__nonboard=(name,kind)=>{try{S.settings.name=String(name||'').trim();S.settings.onboarded=true;if(kind==='demo')seedDemo();SHEET_LOCK=false;closeSheet();save();render(false)}catch(e){console.warn(e)}};
window.__nsetup=()=>{try{return JSON.stringify(setupItems().map(x=>({k:x.k,t:x.t,x:x.x,done:!!x.done,btn:x.btn,opt:!!x.opt})))}catch(e){return'[]'}};
/* native weekly review */
window.__nweek=()=>{try{const done=weekDone(),late=active().flatMap(g=>g.steps.filter(s=>!s.done&&s.due&&daysUntil(s.due)<0)),ahead=active().map(g=>({g,s:nextStep(g)})).filter(x=>x.s&&x.s.due&&daysUntil(x.s.due)>=0&&daysUntil(x.s.due)<=7);const hw=habitWeekLine();
 return JSON.stringify({a:new Date(Date.now()-6*864e5).toLocaleDateString(undefined,{month:'short',day:'numeric'}),b:new Date().toLocaleDateString(undefined,{month:'short',day:'numeric'}),done:done.length,top:done.slice(-5).map(e=>e.text),late:late.length,ahead:ahead.length,habits:hw?hw.replace(/^Habits: /,'').replace(/\.$/,''):''})}catch(e){return''}};
window.__nweeksave=(a,b,c)=>{try{const parts=[['What went well',a],['What got in the way',b],['Focus for next week',c]].filter(p=>String(p[1]||'').trim()).map(p=>`${p[0]}:\n${String(p[1]).trim()}`);const done=weekDone();
 S.entries.push({id:uid(),t:Date.now(),type:'note',goalId:null,title:'Weekly review · '+new Date().toLocaleDateString(undefined,{month:'short',day:'numeric'}),text:(done.length?`Finished ${done.length} step${done.length===1?'':'s'} this week.\n\n`:'')+(habitWeekLine()?habitWeekLine()+'\n\n':'')+parts.join('\n\n'),sid:null,img:null});S.settings.lastReview=ymd();save();render(false)}catch(e){console.warn(e)}};
/* native assistant settings */
window.__nins=()=>{try{const s=insSet();return JSON.stringify({s:{engine:s.engine,smart:!!s.smart,priv:!!s.priv,url:s.url||'',model:s.model||'',llm:llmOf(s.llm)[0]},key:aiKey(),eng:ENG_N,nat:!!NAT_OK(),gpu:!!useGPU(),
 llms:LLMS.map(l=>({k:l[0],n:l[1],have:!!(NAT_OK()&&natHave(l))})),api:API_PRE.map(a=>({n:a[0],u:a[1],m:a[2]})),origin:NATIVE?'https://appassets.androidplatform.net':location.origin,curHave:!!(NAT_OK()&&natHave(llmOf(s.llm))),curName:llmOf(s.llm)[1].split(' · ')[0]})}catch(e){return''}};
window.__ninsset=(k,v)=>{try{const s=insSet();if(k==='key'){try{v?localStorage.setItem('plotline.aikey',String(v).trim()):localStorage.removeItem('plotline.aikey')}catch(_){}return}
 if(k==='url'||k==='model'){s[k]=String(v).trim();save();return}if(k==='engine'||k==='llm')s[k]=v;else if(k==='smart'||k==='priv'||k==='gpu')s[k]=(v===true||v==='1'||v==='true');save();
 if(k==='smart'&&s.smart)ensureVecs(ragDocs(),()=>{}).then(()=>{window.__nit='Smart search is ready'}).catch(()=>{window.__nit='Couldn’t load the search model. Keyword search still works'})}catch(e){console.warn(e)}};
window.__ninstest=()=>{window.__nit='';const s=insSet();fetch(s.url.replace(/\/+$/,'')+'/models',{headers:aiKey()?{Authorization:'Bearer '+aiKey()}:{}}).then(r=>{if(!r.ok)throw new Error('status '+r.status);return r.json().catch(()=>({}))}).then(j=>{const ids=(j.data||[]).map(m=>m.id);window.__nit=ids.length?`Connected · ${ids.length} model${ids.length===1?'':'s'}${ids.includes(s.model)?'':' · “'+s.model+'” not found'}`:'Connected'}).catch(()=>{window.__nit='Couldn’t reach the server. Check the address and CORS'});return 1};
window.__nit='';window.__nitGet=()=>{const t=window.__nit||'';window.__nit='';return t};
window.__nmdel=()=>{try{const m=llmOf(insSet().llm);NATIVE.llmDelete(natFile(m));NAT_WARM='';const s=insSet();if(s.dl)delete s.dl[m[0]];save()}catch(e){}};
window.__nabout=()=>{try{const s=insSet();return insOverview(s.engine==='local'?(IS_PHONE?1200:1800):4000)}catch(e){return''}};
/* native "save this place" */
window.__nph='';window.__nphGet=()=>{const t=window.__nph||'';if(t)window.__nph='';return t};
window.__nplace=async()=>{window.__nph='';try{const st=astat();if(st.loc&&!st.loc.perm){if(!(await permAsk('loc','android.permission.ACCESS_FINE_LOCATION,android.permission.ACCESS_COARSE_LOCATION'))){window.__nph=JSON.stringify({err:'perm'});return}}
 const r=await new Promise(res=>{window.__here=j=>{try{res(JSON.parse(j))}catch(e){res({err:'bad'})}};NATIVE.placeHere()});window.__nph=JSON.stringify(r)}catch(e){window.__nph=JSON.stringify({err:'bad'})}};
window.__nplacedel=id=>{try{S.settings.places=places().filter(p=>p.id!==id);save();syncNative()}catch(e){}};
window.__nplacesave=(name,emoji,r,lat,lng)=>{try{const n=String(name||'').trim();if(!n)return;const p={id:uid(),name:n,emoji:oneEmoji(emoji||''),lat:+lat,lng:+lng,r:+r||150};S.settings.places=[...places(),p];save();syncNative();return p.id}catch(e){}};
/* native share-an-item: prep (checks), then go (creates the encrypted share); results arrive in window.__nsh */
window.__nsh='';window.__nshGet=()=>{const t=window.__nsh||'';if(t)window.__nsh='';return t};
window.__nshprep=async(kind,id,g)=>{window.__nsh='';try{const ref=kind==='step'?{g,id}:{id};const o=kind==='habit'?H(id):kind==='goal'?G(id):(G(g)||{steps:[]}).steps.find(s=>s.id===id);if(!o)return void(window.__nsh=JSON.stringify({err:'This item is gone'}));
 if(!fsOK())return void(window.__nsh=JSON.stringify({err:'Sharing isn’t available in this version yet'}));
 if(!syncOn())return void(window.__nsh=JSON.stringify({sync:1}));
 if(!shset().on){shPatch({on:true});scopeSync()}
 try{await fbSession();await publishKey()}catch(e){if(e&&e.fbAllow)return void(window.__nsh=JSON.stringify({allow:1}));if(e&&e.fbAuth)return void(window.__nsh=JSON.stringify({err:'Sign in again to share'}));return void(window.__nsh=JSON.stringify({err:shErr(e)}))}
 SHN={kind,ref,mode:'together'};
 window.__nsh=JSON.stringify({ok:1,title:o.title,kind,told:!!shset().told,modes:Object.entries(MODES).filter(([k])=>kind!=='step'||k!=='compete').map(([k,v])=>({k,e:v.e,n:v.n,x:v.x})),steps:kind==='goal'?o.steps.filter(s=>!s.done).map(s=>({id:s.id,t:s.title})):[],facts:SH_FACTS(o.title).replace(/<li>/g,'\n• ').replace(/<[^>]+>/g,'').replace(/&[a-z]+;/g,' ').trim(),me:fme()||''})}catch(e){window.__nsh=JSON.stringify({err:String(e&&e.message||e)})}};
window.__nshtold=()=>{shPatch({told:true})};
window.__nshgo=async(mode,emailsRaw,assigns)=>{window.__nsh='';try{const emails=emailsOf(emailsRaw);if(!emails.length)return void(window.__nsh=JSON.stringify({err:'Add the Google email of who you’re sharing with'}));const K=SHN.kind,ref=SHN.ref;const o=K==='habit'?H(ref.id):K==='goal'?G(ref.id):(G(ref.g)||{steps:[]}).steps.find(s=>s.id===ref.id);if(!o)return;
 const id=await ensureIdent(),cid='c'+uid()+uid(),k=newKey(),item={kind:K,title:o.title};if(K==='habit'){item.icon=hIcon(o);item.unit=o.unit||'';item.target=hTarget(o);item.hkind=o.kind;item.freq=o.freq;item.days=o.days;item.times=o.times}
 if(K==='step'){item.sid=ref.id;item.due=o.due||''}
 if(K==='goal'){item.steps=o.steps.map(s=>({id:s.id,title:s.title,due:s.due||'',to:lc((assigns&&assigns[s.id])||'')}));item.why=o.why||'';item.area=o.area}
 const sh={id:cid,fs:1,k,role:'owner',mode,title:o.title,status:'joined',local:K==='step'?{kind:'step',g:ref.g,id:ref.id}:{kind:K,id:ref.id},u:Date.now()};
 const members=[fme(),...emails],keys=await shWrapMissing({members,keys:{}},sh,true)||{};
 await fs(`/circles?documentId=${cid}`,{method:'POST',body:JSON.stringify(fsEnc({owner:fme(),members,opub:id.pub,locks:JSON.stringify(keys),enc:await encJ(k,{title:o.title,mode,item,ownerName:myName().slice(0,80)}),created:Date.now(),u:Date.now()}))});
 shares().push(sh);save();await shPublish(sh).catch(()=>{});render(false);
 const later=emails.filter(e=>!keys[e]);window.__nsh=JSON.stringify({sent:1,who:emails.length===1?emails[0].split('@')[0]:emails.length+' people',title:sh.title,later:later.length,laterWho:later.length===1?later[0]:later.length+' people',msg:`I’m using Plotline for my goals and habits and shared one with you. Get it here and sign in with your Google account: ${(cfg().webUrl||PLOTLINE_CFG.webUrl)}`})}
 catch(e){window.__nsh=JSON.stringify({err:shErr(e)})}};
/* native reminders sheet: the "next reminders" preview and the habit schedule text */
window.__nrem=(k,id,g,sid)=>{try{REM={k,id,g,s:sid};const nx=remNext(),o=remItem();return JSON.stringify({next:nx.map(fmtAt),warn:!!(NATIVE&&nset&&!nset()[{habit:'habits',step:'steps',day:'days',goal:'checkins'}[k]]),ft:k==='habit'&&o?freqText(o):''})}catch(e){return''}};
/* native focus timer finish: log the minutes on the step (no web sheet) */
window.__ntimerfin=()=>{try{const t=S.settings.timer;if(!t)return'';S.settings.timer=null;const g=G(t.g),s=g&&g.steps.find(x=>x.id===t.s);if(s){s.focus=(s.focus||0)+t.m;log('focus',g.id,`${t.m} min focused on “${s.title}”`,Date.now(),s.id)}save();return'1'}catch(e){return''}};
/* 2.0.7: calendar export for the native app: runs the web ACT but captures the file instead of downloading it */
window.__nics=(k,gid,sid)=>{let out=null,msg='';const od=download,ot=toast,oc=closeSheet;try{download=(n,t)=>{out={name:n,text:t}};toast=m=>{msg=String(m||'')};closeSheet=()=>{};ACT[k]({id:gid,g:gid,s:sid})}catch(e){return JSON.stringify({err:String(e)})}finally{download=od;toast=ot;closeSheet=oc}return JSON.stringify({name:out?out.name:'',text:out?out.text:'',msg})};
window.__nprompt=()=>{const P=[...planPrompts(),...WPROMPTS];return P[Math.floor(Math.random()*P.length)]||''};
/* 2.0.8: the native shell runs engine actions without showing the web layer. While the web layer is hidden,
   a page change the action asks for goes to the native screens, toasts show natively, and a sheet the action
   opens (not ported yet) brings the web layer up so nothing is ever lost. */
const nHidden=()=>{try{return NSHELL&&!NATIVE.classicOn()}catch(e){return false}};
window.__nRunT=0;
{const _go=go;go=function(h){if(nHidden()&&Date.now()-window.__nRunT<4000){try{NATIVE.nroute(String(h||''))}catch(e){}return}return _go.apply(this,arguments)}}
{const _t=toast;toast=function(m,u){_t.apply(this,arguments);if(nHidden()){try{NATIVE.ntoast(String(m||''),!!u)}catch(e){}}}}
window.__nrun=(act,data)=>{try{window.__nRunT=Date.now();RP=null;SHEET_LOCK=false;const f=ACT[act];if(!f)return'0';const el=document.createElement('button');Object.assign(el.dataset,data||{});
 const r=f(el.dataset,el,{});setTimeout(()=>{if(nHidden()&&($('#sheet').classList.contains('on')||(typeof AOV!=='undefined'&&AOV.open))){window.__nOne=true;try{NATIVE.nneedUi()}catch(e){}}},450);return'1'}catch(e){console.warn('nrun',e);return'0'}};
{const _ac=askConfirm;askConfirm=function(t,msg,ok,fn,danger=true){if(nHidden()){window.__nconf=fn;try{NATIVE.nconfirm(String(t),String(msg),String(ok),danger!==false);return}catch(e){}}return _ac.apply(this,arguments)}}
window.__nconfYes=()=>{const f=window.__nconf;window.__nconf=null;window.__nRunT=Date.now();if(f){try{const r=f();if(r&&r.catch)r.catch(e=>console.warn(e))}catch(e){console.warn(e)}}setTimeout(()=>{if(nHidden()&&$('#sheet').classList.contains('on')){window.__nOne=true;try{NATIVE.nneedUi()}catch(e){}}},450)};
/* erase everything, without the web welcome (the native one runs instead) */
window.__nwipe=()=>{try{tokDrop();S=norm({});SIG.clear();DIRTY=false;save();applyTheme();return'1'}catch(e){return''}};
/* 2.0.8: everything the native journal viewer shows for one entry (web viewEntry) */
window.__nentry=id=>{try{const e=S.entries.find(x=>x.id===id);if(!e)return'';const g=G(e.goalId),h=entryHabit(e),dt=new Date(e.t),kind=JK[e.type]||'Moment',s=e.sid&&g&&g.steps.find(x=>x.id===e.sid);const title=e.title||(e.type==='note'?trunc((e.text||'').split('\n')[0],80):e.text)||kind;const nx=g&&nextStep(g);
 return JSON.stringify({id:e.id,note:e.type==='note',img:e.img||'',head:kind+' · '+dt.toLocaleDateString(undefined,{weekday:'long',month:'long',day:'numeric',year:'numeric'})+' · '+dt.toLocaleTimeString([],{hour:'numeric',minute:'2-digit'}),title,
  mood:e.type==='note'&&e.mood?e.mood+' '+((MOODS.find(m=>m[0]===e.mood)||[])[1]||''):'',html:e.type==='note'&&(e.html||e.text)?richOf(e):'',
  g:g?{id:g.id,area:g.area,title:g.title,lbl:e.type==='goal-new'||e.type==='goal-done'?'The goal':'Part of',pct:pct(g),sub:g.status==='done'?'Achieved':pct(g)+'% · '+(nx?'next: '+trunc(nx.title,50):'no open steps'),step:s?'Step: '+s.title+' · '+(s.done?'done':'open'):''}:null,
  h:h?{id:h.id,area:h.area,title:hIcon(h)+' '+h.title,sub:h.kind==='quit'?qDays(h)+' days free now':hStreak(h)+(h.freq==='times'?'-week':'-day')+' streak now'}:null})}catch(e){return JSON.stringify({err:String(e)})}};
window.__nscope=()=>{try{return JSON.stringify({win:IN.win,goal:IN.goal||'',goals:treeOrder(S.goals).filter(t=>t.d<2).map(t=>({id:t.g.id,t:'· '.repeat(t.d||0)+trunc(t.g.title,48)}))})}catch(e){return JSON.stringify({err:String(e)})}};
window.__nscopeset=(w,g)=>{IN.win=+w;IN.goal=String(g||'');return'1'};
{const _a=acctChoice;acctChoice=function(em){if(nHidden()){ACCT_NEW=em;const old=S.settings.sync.owner||'another account';try{NATIVE.nsheet('acct',JSON.stringify({old,em,n:S.goals.length,h:S.habits.length,j:S.entries.filter(e=>e.type==='note').length}));return}catch(e){}}return _a.apply(this,arguments)}}
/* 2.0.8: sharing, drawn natively. The engine keeps doing the encryption and the network; async results land in
   window.__nsr[key] and the native screens poll __nsrGet(key). */
window.__nsr={};window.__nsrGet=k=>{const v=window.__nsr[k];if(v===undefined)return'';delete window.__nsr[k];return v};
const nsrSet=(k,o)=>{window.__nsr[k]=JSON.stringify(o)};
const nTxt=h=>String(h||'').replace(/<li>/g,'\n• ').replace(/<[^>]+>/g,'').replace(/&amp;/g,'&').replace(/&lt;/g,'<').replace(/&gt;/g,'>').replace(/&quot;/g,'"').replace(/&#39;/g,"'").trim();
window.__nshhow=()=>nTxt(SH_FACTS(''));
window.__nshacc=async id=>{const k='acc:'+id;try{const sh=shById(id);if(!sh)return nsrSet(k,{err:'That invite isn’t here any more'});
 if(sh.locked)return nsrSet(k,{locked:1,who:shWho(sh),from:sh.from||''});let c,d;
 try{c=await fsCircle(sh.id);if(!c)throw new Error('404');d=await decJ(sh.k,c.enc)}catch(e){if(/404/.test(String(e&&e.message))){sh.status='ended';sh.u=Date.now();save()}return nsrSet(k,{err:nTxt(shErr(e))})}
 const md=MODES[d.mode]||MODES.together,it=d.item||{},mine=(it.steps||[]).filter(s=>s.to===fme());
 nsrSet(k,{ok:1,e:md.e,n:md.n,x:md.x,title:d.title,ownerName:d.ownerName||c.owner,owner:c.owner,steps:it.kind==='goal'&&it.steps?it.steps.map(s=>({t:s.title,me:s.to===fme(),to:s.to?(s.to===fme()?'you':s.to.split('@')[0]):''})):[],mine:mine.length})}catch(e){nsrSet(k,{err:nTxt(shErr(e))})}};
window.__nshjoin=async id=>{const sh=shById(id);if(!sh)return nsrSet('join:'+id,{ok:0});await shJoin(sh);nsrSet('join:'+id,{ok:sh.status==='joined'?1:0})};
window.__nshgopen=id=>{SHG_OPENING=id||null;if(id)rxRead(id)};
window.__nshgrp=async id=>{const k='grp:'+id,sh=shById(id);if(!sh)return nsrSet(k,{err:'That shared item isn’t here any more'});let G2;
 try{await shPublish(sh);G2=await shGroup(sh)}catch(e){if(/404/.test(String(e&&e.message))){sh.status='ended';sh.u=Date.now();save()}return nsrSet(k,{err:nTxt(shErr(e)),title:sh.title})}
 shCollect(sh,G2.all);rxRead(sh.id);const{c,d,ms,all}=G2,it=d.item||{},kind=it.kind,score=m=>kind==='habit'?weekChecks(m.progress):(m.progress&&m.progress.pct)||0;
 const rows=ms.slice().sort((a,b)=>score(b)-score(a)),tot=kind==='habit'?rows.reduce((s,m)=>s+weekChecks(m.progress),0):Math.round(rows.reduce((s,m)=>s+((m.progress&&m.progress.pct)||0),0)/Math.max(1,rows.length));
 const st=e=>(all.find(m=>m.email===e)||{}).status,pending=c.members.filter(e=>!ms.some(m=>m.email===e)),tn=dnum(ymd());
 const mineC=(ms.find(m=>m.mine)||{}).cheers||[],nm=e=>{const m=ms.find(x=>x.email===e);return m?m.name:(e||'').split('@')[0]};
 const feed=[...reactsForMe(sh.id).map(r=>({...r,dir:'in'})),...mineC.filter(x=>x&&x.t).map(x=>({e:x.emoji,m:x.m,t:+x.t,to:nm(x.to),dir:'out'}))].sort((a,b)=>b.t-a.t).slice(0,6).map(r=>({e:r.e||'💬',dir:r.dir,who:r.dir==='in'?r.from+' → you':'You → '+r.to,m:r.m||'',when:dayLabel(r.t)+' '+new Date(r.t).toLocaleTimeString([],{hour:'numeric',minute:'2-digit'})}));
 nsrSet(k,{ok:1,id:sh.id,role:sh.role,e:(MODES[d.mode]||{}).e||'',n:(MODES[d.mode]||{}).n||'',mode:d.mode,title:d.title,kind,tot,
  rows:rows.map((m,i)=>({mine:!!m.mine,ava:d.mode==='compete'?(['🥇','🥈','🥉'][i]||String(i+1)):String(m.name||m.email||'?')[0].toUpperCase(),name:m.mine?'You':(m.name||m.email),email:m.email,nm:m.name||m.email.split('@')[0],
   sub:(kind==='habit'?`${weekChecks(m.progress)} this week · ${(m.progress&&m.progress.streak)||0} day streak`:`${(m.progress&&m.progress.pct)||0}% done`)+(m.progress&&m.progress.u?' · '+dayLabel(m.progress.u):''),
   wk:kind==='habit'?[...Array(7)].map((_,j)=>!!(m.progress&&m.progress.checks&&m.progress.checks[fromN(tn-6+j)])):null,pct:(m.progress&&m.progress.pct)||0})),
  pending:pending.map(e=>({ava:e[0].toUpperCase(),e,sub:st(e)==='declined'?'Declined':st(e)==='left'?'Left':c.keys[e]||c.owner===e?'Invited · hasn’t joined yet':'Invited · not on Plotline yet'})),
  feed,steps:kind==='goal'&&it.steps?it.steps.map(s=>{const by=ms.find(m=>m.progress&&(m.progress.done||[]).includes(s.id));return{t:s.title,d:!!by,me:s.to===fme(),who:by?'✓ '+(by.mine?'you':by.name):s.to?(s.to===fme()?'you':s.to.split('@')[0]):'anyone'}}):[]})};
window.__nshmore=async(id,raw)=>{const k='more:'+id,x=shById(id);if(!x||!x.k)return nsrSet(k,{err:'That shared item isn’t here any more'});const emails=emailsOf(raw);if(!emails.length)return nsrSet(k,{err:'Add the Google email of who you’re inviting'});
 try{const c=await fsCircle(x.id);if(!c)throw new Error('404');const members=[...new Set([...c.members,...emails])].slice(0,21);const keys=await shWrapMissing({...c,members},x,true)||c.keys;
  await fs(`/circles/${x.id}?updateMask.fieldPaths=members&updateMask.fieldPaths=locks&updateMask.fieldPaths=u`,{method:'PATCH',body:JSON.stringify(fsEnc({members,locks:JSON.stringify(keys),u:Date.now()}))});
  const later=emails.filter(e=>!keys[e]);nsrSet(k,{sent:1,id:x.id,who:emails.length===1?emails[0].split('@')[0]:emails.length+' people',title:x.title,later:later.length,laterWho:later.length===1?later[0]:later.length+' people',msg:`I’m using Plotline for my goals and habits and shared one with you. Get it here and sign in with your Google account: ${(cfg().webUrl||PLOTLINE_CFG.webUrl)}`})}catch(e){nsrSet(k,{err:nTxt(shErr(e))})}};
window.__nshrx=()=>JSON.stringify({left:Math.max(0,REACT_MAX-reactsSentToday()),max:REACT_MAX,emo:REACT_EMO,quick:quickMsgs(),defs:REACT_MSG,panel:PANEL_EMO});
window.__nshreact=async(id,to,e,i,m)=>{const x=shById(id);if(!x)return nsrSet('rx',{err:'That shared item isn’t here any more'});if(reactsSentToday()>=REACT_MAX)return nsrSet('rx',{err:`That’s ${REACT_MAX} reactions for today`});
 const msg=i!==''&&i!=null?String(quickMsgs()[+i]||'').slice(0,60):String(m||'');try{const r=await reactSend(x,to,e||'',msg);nsrSet('rx',r?{ok:1,emoji:r.emoji||'',m:r.m||''}:{ok:0})}catch(err){nsrSet('rx',{err:nTxt(shErr(err))})}};
window.__nshquick=j=>{try{const q=j?JSON.parse(j):null;shPatch({quick:q&&q.length?q:null});return'1'}catch(e){return''}};
window.__nshpanel=(kind,id)=>{try{const x=shPanelFor(kind,id),any=shares().find(y=>y.status==='joined'&&y.local&&y.local.kind===kind&&y.local.id===id);if(!any)return'';const md=MODES[any.mode]||MODES.together,n=rxUnread(any.id);
 if(!x)return JSON.stringify({id:any.id,e:md.e||'🤝',n:md.n,unread:n,chip:1});
 const c=SHM[x.id];if(!c||Date.now()-c.t>4000)setTimeout(()=>shPanelLoad(x),0);const left=Math.max(0,REACT_MAX-reactsSentToday()),td=ymd();
 if(c){const rx=shset().rx||{};if(rx[x.id]&&rx[x.id].n){rx[x.id]={...rx[x.id],n:0};shPatch({rx:{...rx}})}}
 const others=c?c.ms.filter(m=>m.email!==fme()&&m.status==='joined'):[],mine=reactsForMe(x.id);
 return JSON.stringify({id:x.id,e:md.e,n:md.n,unread:n,chip:1,panel:1,loaded:!!c,owner:x.role==='owner',left,max:REACT_MAX,emo:PANEL_EMO,others:others.map(m=>{const p=m.progress||{},last=mine.filter(r=>r.fe===m.email).slice(-1)[0],nm=m.name||m.email.split('@')[0];
  return{email:m.email,nm,ava:nm[0].toUpperCase(),st:kind==='habit'?`${p.checks&&p.checks[td]?'✓ done today':'not yet today'} · ${weekChecks(p)} this week`:`${p.pct||0}% done`,last:last?`${last.e||'💬'} ${last.m?last.m+' · ':''}${dayLabel(last.t).toLowerCase()}`:''}})})}catch(e){return''}};
window.__nshtoday=()=>{try{if(!shOn())return'';const s=shset(),inv=shares().filter(x=>x.status==='invited').slice(0,2),rx=s.rx||{};
 const rr=shares().filter(x=>x.status==='joined'&&(rx[x.id]||{}).n).slice(0,2).map(x=>{const r=reactsForMe(x.id).slice(-1)[0],n=rx[x.id].n;return{id:x.id,e:r&&r.e||'💬',t:r?r.from+(r.m?': '+r.m:' sent you '+r.e):n+' new reaction'+(n===1?'':'s'),sub:(n>1?n+' new · ':'')+'on “'+x.title+'”'}});
 const o={inv:inv.map(x=>({id:x.id,ava:shWho(x)[0].toUpperCase(),who:shWho(x),locked:!!x.locked,from:x.from||'',e:(MODES[x.mode]||{}).e||'',m:(MODES[x.mode]||{}).n||'Shared',t:x.title,later:x.status==='later'})),rx:rr,allow:!!(s.allow&&!s.allowHide),allowHide:!!s.allowHide};
 return o.inv.length||o.rx.length||o.allow?JSON.stringify(o):''}catch(e){return''}};
window.__nshredo=id=>{const x=shById(id);if(!x||!x.local)return'';const L=x.local;x.status='ended';x.u=Date.now();save();return JSON.stringify(L)};
window.__nshjr=async id=>{const k='jr:'+id;try{if(!shOn())return nsrSet(k,{k:'off'});SHC.t=0;await shRefresh(false);const x=shById(id);if(x&&['invited','later'].includes(x.status))return nsrSet(k,{k:'acc'});if(x&&x.status==='joined')return nsrSet(k,{k:'open'});nsrSet(k,{k:'none'})}catch(e){nsrSet(k,{k:'none'})}};
/* reactions that arrive while the native screens are up: banner + floating emoji drawn natively */
{const _rs=reactShow;reactShow=function(){if(nHidden()){clearTimeout(RB.tm);const r=RB.q[RB.q.length-1];if(!r){try{NATIVE.nsheet('rxb','{}')}catch(e){}return}const sh=shById(r.cid);
 try{NATIVE.nsheet('rxb',JSON.stringify({cid:r.cid,e:r.e||'💬',from:r.from,fe:r.fe,m:r.m?r.m:'sent you '+(r.e||''),sub:(sh?'on “'+sh.title+'”':'')+(RB.q.length>1?' · +'+(RB.q.length-1)+' more':'')}))}catch(e){}RB.tm=setTimeout(()=>{RB.q=[];reactShow()},12000);return}return _rs.apply(this,arguments)}}
{const _rb=reactBurst;reactBurst=function(e){if(nHidden()){try{NATIVE.nsheet('rxburst',JSON.stringify({e:String(e||'💬')}))}catch(_){}return}return _rb.apply(this,arguments)}}
{const _ft=finishTimer;finishTimer=function(){if(nHidden())return;return _ft.apply(this,arguments)}}
/* the native app has its own welcome, setup, tour offer and lock screen */
{const w=(f)=>function(){if(nHidden())return;return f.apply(this,arguments)};welcome=w(welcome);setupFlow=w(setupFlow);showLock=w(showLock);if(typeof tourPrompt==='function')tourPrompt=w(tourPrompt)}
/* safety net: any web sheet that still opens while the native screens are up brings the web layer up with it */
{const _os=openSheet;openSheet=function(){const r=_os.apply(this,arguments);if(nHidden()){window.__nOne=true;setTimeout(()=>{if(nHidden()&&$('#sheet').classList.contains('on')){console.warn('nshell: web sheet',String(arguments[0]||'').slice(0,80));try{NATIVE.nneedUi()}catch(e){}}},60)}return r}}
{const _b=burst;burst=function(){if(nHidden()){try{NATIVE.nsheet('burst','{}')}catch(e){}return}return _b.apply(this,arguments)}}
/* the hidden engine never animates its own copy of the room */
{const _cs=catStart;window.__catStart0=_cs;catStart=function(){if(nHidden())return;return _cs.apply(this,arguments)}}
/* 2.0.8: the Studio room, drawn by its own scene view inside the native Home (same SVG/CSS, taps go native) */
window.__nstudio=()=>{try{const R=document.documentElement,F=(n,f)=>`var ${n}=${f.toString()};`,D=f=>f.toString()+';';
 const js=[`var ST_COLORS=${JSON.stringify(ST_COLORS)};`,F('hx',hx),F('toHex',toHex),F('mixC',mixC),F('clamp01',clamp01),
  D(skyNow),D(studioVars),D(studioClock),D(studioRays),D(raysFit),D(studioSky),'var CAT={t0:0,el:0,run:false,raf:0,plan:null,cycle:0},CATBOX={w:0,h:0},CATT=0;',
  D(catPlan),D(catBox),D(catDraw),D(catFrame),D(window.__catStart0||catStart),D(catStop)].join('\n');
 return JSON.stringify({css:[...document.querySelectorAll('style')].map(x=>x.textContent).join('\n'),links:[...document.querySelectorAll('link[rel=stylesheet]')].map(l=>l.getAttribute('href')),
  attrs:[...R.attributes].map(a=>[a.name,a.value]),html:studioHTML(),js,play:!!st0().play,reduced:reduced()})}catch(e){return JSON.stringify({err:String(e)})}};
window.__nstudioparts=()=>{try{const D=studioData(innerWidth<640);ST_LIGHT=1;const t=document.createElement('div');t.innerHTML=studioBoard(D)+studioBench(D);ST_LIGHT=0;const o={};
 [['hello','.st-hello'],['notes','.st-notes'],['tray','.st-tray'],['book','.st-book']].forEach(([k,q])=>{const n=t.querySelector(q);o[k]=n?n.innerHTML:null});return JSON.stringify(o)}catch(e){ST_LIGHT=0;return JSON.stringify({err:String(e)})}};
window.__nstudiopage=()=>{try{const o=JSON.parse(window.__nstudio());if(o.err)return'';const at=o.attrs.map(([k,v])=>k==='class'?['class',(v+' stu-on stu-attop').trim()]:[k,v]);if(!at.some(a=>a[0]==='class'))at.push(['class','stu-on stu-attop']);
 const boot=`var NS=window.PlotStudio||{act:function(){},size:function(){},play:function(){}};var PLAY=${o.play},RED=${o.reduced};var $=function(s){return document.querySelector(s)};var reduced=function(){return RED};var st0=function(){return{play:PLAY}};
${o.js}
function fit(){var sc=$('#studio');if(!sc)return;var r=sc.getBoundingClientRect();NS.size(r.top+scrollY,r.height);var n=sc.querySelector('.st-notes');if(n&&NS.notes){var q=n.getBoundingClientRect();NS.notes(q.left,q.top-r.top,q.right,q.bottom-r.top)}}
function mount(){var sc=$('#studio');if(!sc)return;studioSky();setInterval(studioSky,60000);raysFit(sc);catBox();if(PLAY&&!RED)catStart();else catDraw(CAT.el);fit()}
var PL='<svg viewBox="0 0 24 24"><rect x="6" y="5" width="4" height="14" rx="1"/><rect x="14" y="5" width="4" height="14" rx="1"/></svg>',PY='<svg viewBox="0 0 24 24"><path d="M8 5l11 7-11 7z"/></svg>';
document.addEventListener('click',function(e){var a=e.target.closest('a[href^="#/"]');if(a){e.preventDefault();NS.act('go',JSON.stringify({to:a.getAttribute('href').slice(2)}));return}
 var b=e.target.closest('[data-act]');if(!b)return;e.preventDefault();var k=b.dataset.act;
 if(k==='stCat'){[0,1,2].forEach(function(i){setTimeout(function(){var h=document.createElement('i');h.className='st-heart';h.style.setProperty('--hx',(i-1)*40+'%');h.textContent='♥';b.appendChild(h);setTimeout(function(){h.remove()},1400)},i*180)});b.classList.remove('pet');void b.offsetWidth;b.classList.add('pet');setTimeout(function(){b.classList.remove('pet')},1800);return}
 if(k==='stPlay'){PLAY=!PLAY;var sc=$('#studio');sc.classList.toggle('paused',!PLAY);var p=sc.querySelector('.st-play');p.innerHTML=PLAY?PL:PY;p.setAttribute('aria-label',PLAY?'Pause the scene':'Play the scene');if(PLAY)catStart();else catStop();NS.play(PLAY);return}
 NS.act(k,JSON.stringify(Object.assign({},b.dataset)))});
window.__parts=function(o){var sc=$('#studio');if(!sc||!o)return;var ns=sc.querySelector('.st-notes'),sy=ns?ns.scrollTop:0;[['hello','.st-hello'],['notes','.st-notes'],['tray','.st-tray'],['book','.st-book']].forEach(function(q){var el=sc.querySelector(q[1]);if(el&&o[q[0]]!=null&&el.innerHTML!==o[q[0]])el.innerHTML=o[q[0]]});if(ns)ns.scrollTop=sy;studioSky();fit()};
window.__vis=function(v){if(v&&PLAY&&!RED)catStart();else catStop()};
window.__sat=function(px){document.documentElement.style.setProperty('--sat',px+'px');setTimeout(function(){catBox();raysFit($('#studio'));fit()},50)};
addEventListener('load',function(){mount();setTimeout(fit,300);setTimeout(fit,1200)});addEventListener('resize',function(){catBox();var sc=$('#studio');if(sc)raysFit(sc);fit()});`;
 return`<!doctype html><html ${at.map(([k,v])=>`${k}="${String(v).replace(/"/g,'&quot;')}"`).join(' ')}><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover">${o.links.map(h=>`<link rel="stylesheet" href="${h}">`).join('')}<style>${o.css}</style><style>html,body{overflow:hidden!important}</style></head><body><main id="view"><div id="stuHost">${o.html}</div><div id="vin"></div></main><script>${boot.replace(/<\/script/g,'<\\/script')}<\/script></body></html>`}catch(e){return''}};
window.__ndyreacts=ds=>{try{return JSON.stringify(dyReacts(ds).map(r=>({t:r.t,me:r.me,e:r.e||'💬',m:r.m||'',who:r.me?String(r.who||'').split('@')[0]:(r.who||'Someone'),item:r.item,hid:''})).concat(...shares().filter(sh=>sh.local&&sh.local.kind==='habit').map(sh=>dyReacts(ds,sh.local.id).map(r=>({t:r.t,me:r.me,e:r.e||'💬',m:r.m||'',who:r.me?String(r.who||'').split('@')[0]:(r.who||'Someone'),item:r.item,hid:sh.local.id})))))}catch(e){return'[]'}};
/* 2.0.8: what the native Activity page needs beyond settings.actLog (permissions, Health Connect, live places) */
window.__nactv=()=>{try{const d=ACTV.data||{};return JSON.stringify({at:ACTV.at?tShort(ACTV.at):'',loaded:!!ACTV.data,ok:d.ok||{},hc:d.hc||{},inside:d.inside||{},work:((d.work)||[]).slice().sort((a,b)=>b.t-a.t).slice(0,6).map(w=>({e:ACT_EI[w.k]||'💪',n:ACT_EX[w.k]||'Workout',m:w.m,d:dOf(w.t)})),top:(d.top||[]).map(a=>({n:a.name,m:a.min})),wk:places().map(p=>wkPlace(p))})}catch(e){return JSON.stringify({err:String(e)})}};
/* ==== END 1.10 MODULES ==== */
