
/* ================= CHAPTERS + YEAR IN REVIEW =================
 Chapters name seasons of your life ("New job", "Baby #2", "Training for Chicago"). They show as soft bands
 behind the Vista timeline and Habit Vista, and frame the year in review. Synced like goals (S.chapters).
 The year in review is a short story built from your own data: goals reached, habits kept, best month,
 moods, your own words and your chapters. It can be shared as one image; nothing is uploaded by Plotline. */
const CH_COLS=['#46C99B','#6B9BFF','#AC93FF','#FF8C6B','#F0C862','#F585B8','#A9D14A','#5ED0E6'];
const chapters=()=>(S.chapters||[]).slice().sort((a,b)=>a.start<b.start?-1:1);
const chEnd=c=>c.end||ymd();
function chapBandsRoad(x0,ppd,W){return chapters().map(c=>{const s=Math.max(x0,dnum(c.start)),e=dnum(chEnd(c));if(e<x0)return'';const l=(s-x0)*ppd,w=Math.max(6,(e-s+1)*ppd);if(l>W)return'';
 return`<button class="chap" style="--cc:${c.color||CH_COLS[0]};left:calc(var(--gutter) + ${l.toFixed(1)}px);width:${w.toFixed(1)}px" data-act="chapEdit" data-id="${c.id}" aria-label="Chapter: ${esc(c.title)}"><span>${esc(c.emoji||'')} ${esc(c.title)}${c.end?'':' · now'}</span></button>`}).join('')}
function chapForm(c){const e=!!c;c=c||{title:'',emoji:'',start:ymd(),end:'',color:CH_COLS[(S.chapters||[]).length%CH_COLS.length]};
 return`<div class="data">Chapters</div><h2 style="margin-top:6px">${e?'Edit chapter':'New chapter'}</h2><p class="small muted">A season of your life, like “New job” or “Training for my first marathon”. It shows as a band on Vista and frames your year in review.</p>
 <form data-form="chapSave" ${e?`data-id="${c.id}"`:''}><div class="field"><label>Name</label><input name="title" required maxlength="40" value="${esc(c.title)}" placeholder="New job"></div>
 <div class="field"><label>Emoji · optional</label><input name="emoji" class="emo-in" maxlength="16" value="${esc(c.emoji||'')}" placeholder="Type any emoji"></div>
 <div class="two"><div class="field"><label>From</label><input type="date" name="start" required value="${c.start}"></div><div class="field"><label>Until · empty if it’s still going</label><input type="date" name="end" value="${c.end||''}"></div></div>
 <div class="field"><label>Colour</label><div class="chips">${CH_COLS.map(k=>`<label class="chsw"><input type="radio" name="color" value="${k}" ${c.color===k?'checked':''}><span style="background:${k}"></span></label>`).join('')}</div></div>
 <div class="actions">${e?`<button type="button" class="btn danger" data-act="chapDel" data-id="${c.id}">Delete</button>`:''}<button type="button" class="btn ghost" data-act="close">Cancel</button><button class="btn pri">${e?'Save':'Add chapter'}</button></div></form>`}
function chapList(){const L=chapters();return`<div class="data">Chapters</div><h2 style="margin-top:6px">Your chapters</h2>${L.length?`<div class="chl">${L.map(c=>`<button class="chl-r" style="--cc:${c.color}" data-act="chapEdit" data-id="${c.id}"><i></i><span><b>${esc(c.emoji||'')} ${esc(c.title)}</b><small>${fmtDate(c.start)} – ${c.end?fmtDate(c.end):'now'}</small></span>${ic('edit','ico-s')}</button>`).join('')}</div>`:'<p class="small muted">No chapters yet. Name a season of your life and it appears on Vista.</p>'}<div class="actions"><button class="btn ghost" data-act="close">Done</button><button class="btn pri" data-act="chapNew">${ic('plus')}New chapter</button></div>`}
Object.assign(ACT,{chapNew:()=>openSheet(chapForm()),chapEdit:d=>{const c=(S.chapters||[]).find(x=>x.id===d.id);if(c)openSheet(chapForm(c))},chapList:()=>openSheet(chapList()),
 chapDel:d=>askConfirm('Delete this chapter?','Your goals, habits and journal stay exactly as they are.','Delete',()=>{S.chapters=(S.chapters||[]).filter(x=>x.id!==d.id);save();closeSheet();render(false);toast('Chapter deleted')}),
 yearOpen:d=>yearShow(+(d&&d.y)||yearDefault())});
Object.assign(FORM,{chapSave:f=>{const E=f.elements,title=E.title.value.trim(),start=validDate(E.start.value),end=validDate(E.end.value);if(!title||!start)return;if(end&&end<start)return toast('The end is before the start');
 const o={title,emoji:oneEmoji(E.emoji.value),start,end:end||'',color:(new FormData(f).get('color'))||CH_COLS[0],u:Date.now()};S.chapters=S.chapters||[];
 const ex=f.dataset.id&&S.chapters.find(x=>x.id===f.dataset.id);if(ex)Object.assign(ex,o);else S.chapters.push({id:uid(),...o});save();closeSheet();render(false);toast(ex?'Chapter saved':'Chapter added')}});

/* ---- year in review ---- */
const yearDefault=()=>{const d=new Date();return d.getMonth()===0&&d.getDate()<=20?d.getFullYear()-1:d.getFullYear()};
function yearData(y){const a=`${y}-01-01`,b=`${y}-12-31`,td=ymd(),end=b<td?b:td,inY=t=>{const s=ymd(new Date(t));return s>=a&&s<=b},M=[...Array(12)].map(()=>({steps:0,checks:0,notes:0}));
 const done=S.goals.filter(g=>g.status==='done'&&g.completedAt&&inY(g.completedAt)).sort((p,q)=>q.completedAt-p.completedAt);
 const started=S.goals.filter(g=>inY(g.createdAt||0)).length;
 const steps=S.entries.filter(e=>e.type==='step'&&inY(e.t));steps.forEach(e=>M[new Date(e.t).getMonth()].steps++);
 let checks=0;const hab=[];S.habits.forEach(h=>{if(h.kind==='quit'){const fr=h.mode==='limit'?lBest(h):qBest(h);hab.push({h,q:1,best:fr});return}
  let n=0,run=0,best=0,due=0;for(let k=dnum(a);k<=dnum(end);k++){const ds=fromN(k);if(ds<h.startDate||!hDue(h,ds)||h.skip[ds])continue;due++;if(hDone(h,ds)){n++;run++;best=Math.max(best,run);M[+ds.slice(5,7)-1].checks++}else if(ds<td)run=0}checks+=n;hab.push({h,n,best,rate:due?Math.round(n/due*100):0})});
 const notes=S.entries.filter(e=>e.type==='note'&&inY(e.t));notes.forEach(e=>M[new Date(e.t).getMonth()].notes++);
 const moods={};notes.forEach(e=>{const m=MOODN[e.mood];if(m)moods[m]=(moods[m]||0)+1});const mt=Object.entries(moods).sort((p,q)=>q[1]-p[1]);const mn=mt.reduce((s,x)=>s+x[1],0);const pos=mn?Math.round(mt.filter(x=>MOOD_POS.has(x[0])).reduce((s,x)=>s+x[1],0)/mn*100):null;
 const words=notes.reduce((s,e)=>s+String(e.text||'').split(/\s+/).filter(Boolean).length,0);
 const score=m=>m.steps*2+m.checks+m.notes;const bestM=M.map((m,i)=>[i,score(m)]).sort((p,q)=>q[1]-p[1])[0];
 const quotes=notes.filter(e=>!e.private&&e.text).map(e=>{const s=String(e.text).replace(/\s+/g,' ').split(/(?<=[.!?])\s/).find(x=>x.length>=30&&x.length<=170);return s?{t:e.t,s,pos:MOOD_POS.has(MOODN[e.mood]||'')}:null}).filter(Boolean);
 const qs=[];[...quotes.filter(x=>x.pos),...quotes].forEach(x=>{if(qs.length<3&&!qs.some(z=>Math.abs(z.t-x.t)<20*864e5))qs.push(x)});qs.sort((p,q)=>p.t-q.t);
 const chs=chapters().filter(c=>c.start<=b&&chEnd(c)>=a);
 const topH=hab.filter(x=>!x.q).sort((p,q)=>q.best-p.best)[0],steady=hab.filter(x=>!x.q&&x.n>=10).sort((p,q)=>q.rate-p.rate)[0],free=hab.filter(x=>x.q).sort((p,q)=>q.best-p.best)[0];
 return{y,done,started,steps:steps.length,checks,M,bestM,topH,steady,free,notes:notes.length,words,mt,pos,qs,chs,active:S.goals.filter(g=>g.status==='active').length}}
const MONTHS=[...Array(12)].map((_,i)=>new Date(2026,i,1).toLocaleDateString(undefined,{month:'long'}));
function yearSlides(D){const nm=S.settings.name,S1=[];const big=(n,l)=>`<div class="yr-big"><b>${n}</b><span>${l}</span></div>`;
 S1.push(`<div class="yr-k">Your ${D.y}</div><h1>${nm?esc(nm)+'’s':'Your'}<br>plotline</h1><p>${D.chs.length?'A year in '+D.chs.length+' chapter'+(D.chs.length>1?'s':'')+'.':'A year of small steps.'} Tap to see it.</p>${D.chs.length?`<div class="yr-chs">${D.chs.map(c=>`<span style="--cc:${c.color}">${esc(c.emoji||'')} ${esc(c.title)}</span>`).join('')}</div>`:''}`);
 S1.push(`<div class="yr-k">Goals</div>${big(D.done.length,D.done.length===1?'goal reached':'goals reached')}${big(D.steps,'steps done')}${D.done.length?`<p>Biggest win: <b>${esc(D.done[0].title)}</b></p>`:`<p>${D.started} goal${D.started===1?'':'s'} started. The finish lines are coming.</p>`}`);
 S1.push(`<div class="yr-k">Habits</div>${big(D.checks.toLocaleString(),'check-ins')}${D.topH&&D.topH.best?`<p>Longest streak: <b>${D.topH.best} days</b> of ${esc(D.topH.h.title)} ${esc(hIcon(D.topH.h))}</p>`:''}${D.steady?`<p>Most steady: <b>${esc(D.steady.h.title)}</b>, kept ${D.steady.rate}% of the days it was due.</p>`:''}${D.free&&D.free.best?`<p>Longest run free of ${esc(D.free.h.title.replace(/^no /i,''))}: <b>${D.free.best} days</b>.</p>`:''}`);
 const mx=Math.max(1,...D.M.map(m=>m.steps*2+m.checks+m.notes));
 S1.push(`<div class="yr-k">Your best month</div><h2>${D.bestM&&D.bestM[1]?MONTHS[D.bestM[0]]:'Still to come'}</h2><div class="yr-bars">${D.M.map((m,i)=>`<i class="${D.bestM&&i===D.bestM[0]?'on':''}" style="--h:${((m.steps*2+m.checks+m.notes)/mx*100).toFixed(0)}%"><em>${MONTHS[i][0]}</em></i>`).join('')}</div><p>Steps, habits and journal entries, month by month.</p>`);
 if(D.notes)S1.push(`<div class="yr-k">Your journal</div>${big(D.notes,D.notes===1?'entry':'entries')}${big(D.words.toLocaleString(),'words')}${D.mt.length?`<p>You felt <b>${esc(D.mt[0][0].toLowerCase())}</b> most often${D.pos!=null?`, and ${D.pos}% of your moods were positive`:''}.</p>`:''}`);
 if(D.qs.length)S1.push(`<div class="yr-k">In your words</div>${D.qs.map(q=>`<blockquote>“${esc(q.s)}”<cite>${new Date(q.t).toLocaleDateString(undefined,{month:'long',day:'numeric'})}</cite></blockquote>`).join('')}`);
 S1.push(`<div class="yr-k">Next</div><h2>${D.active} goal${D.active===1?'':'s'} in motion</h2><p>Keep the plot moving. Small steps, a clear path.</p><div class="yr-act"><button class="btn pri" data-act="yearShare" data-y="${D.y}">Share my year</button><button class="btn" data-act="yearClose">Close</button></div>`);
 return S1}
let YR=null;
function yearShow(y){const D=yearData(y),sl=yearSlides(D);YR={y,i:0,n:sl.length,D};const o=document.createElement('div');o.className='yrv';o.setAttribute('role','dialog');o.setAttribute('aria-label','Your year in review');
 o.innerHTML=`<div class="yr-bar">${sl.map((_,i)=>`<i><b></b></i>`).join('')}</div><button class="ibtn yr-x" data-act="yearClose" aria-label="Close">${ic('minus')}</button><div class="yr-in">${sl.map((h,i)=>`<section class="yr-s" data-i="${i}">${h}</section>`).join('')}</div><button class="yr-nav l" data-act="yearStep" data-d="-1" aria-label="Back"></button><button class="yr-nav r" data-act="yearStep" data-d="1" aria-label="Next"></button>`;
 document.body.appendChild(o);document.documentElement.style.overflow='hidden';yearPaint()}
function yearPaint(){const o=document.querySelector('.yrv');if(!o||!YR)return;o.querySelectorAll('.yr-s').forEach((s,i)=>s.classList.toggle('on',i===YR.i));o.querySelectorAll('.yr-bar i').forEach((b,i)=>b.className=i<YR.i?'d':i===YR.i?'on':'');}
function yearCanvas(D){const W=1080,H=1920,c=document.createElement('canvas');c.width=W;c.height=H;const x=c.getContext('2d');const cs=getComputedStyle(document.documentElement),acc=cs.getPropertyValue('--accent').trim()||'#FFB547';
 const g=x.createLinearGradient(0,0,0,H);g.addColorStop(0,'#0E1430');g.addColorStop(1,'#05070F');x.fillStyle=g;x.fillRect(0,0,W,H);
 x.strokeStyle='rgba(255,255,255,.08)';for(let i=0;i<4;i++){x.beginPath();x.moveTo(120,520+i*70);x.lineTo(W-120,520+i*70);x.stroke()}
 const T=(t,y,sz,col,w)=>{x.fillStyle=col||'#F2EEE6';x.font=`${w||700} ${sz}px "Bebas Neue","Oswald",Impact,sans-serif`;x.fillText(t,96,y)};const B=(t,y,sz,col)=>{x.fillStyle=col||'#C9CBD3';x.font=`500 ${sz}px system-ui,-apple-system,Segoe UI,Roboto,sans-serif`;x.fillText(t,96,y)};
 B(`PLOTLINE · ${D.y}`,170,40,acc);T((S.settings.name?S.settings.name.toUpperCase()+'’S':'MY')+' YEAR',300,130);
 const rows=[[D.done.length,'goals reached'],[D.steps,'steps done'],[D.checks,'habit check-ins'],[D.topH&&D.topH.best||0,'day longest streak'],[D.notes,'journal entries']];
 rows.forEach(([n,l],i)=>{T(String(n),560+i*230,170,i===0?acc:'#F2EEE6');B(l,620+i*230,44)});
 let cx=96;D.chs.slice(0,4).forEach(ch=>{x.font='600 34px system-ui,sans-serif';const t=(ch.emoji?ch.emoji+' ':'')+ch.title,w=x.measureText(t).width+44;if(cx+w>W-96)return;x.fillStyle=ch.color;x.globalAlpha=.25;x.beginPath();x.roundRect?x.roundRect(cx,380,w,62,31):x.rect(cx,380,w,62);x.fill();x.globalAlpha=1;x.fillStyle='#F2EEE6';x.fillText(t,cx+22,423);cx+=w+14});if(D.bestM&&D.bestM[1])B('Best month: '+MONTHS[D.bestM[0]],1760,40,'#C9CBD3');B('Made with Plotline',1840,32,'rgba(255,255,255,.45)');return c}
Object.assign(ACT,{yearStep:d=>{if(!YR)return;const n=YR.i+ +d.d;if(n<0)return;if(n>=YR.n)return ACT.yearClose();YR.i=n;yearPaint()},
 yearClose:()=>{document.querySelector('.yrv')?.remove();document.documentElement.style.overflow='';YR=null},
 yearShare:async d=>{const D=YR?YR.D:yearData(+d.y);const url=yearCanvas(D).toDataURL('image/png');const txt=`My ${D.y} in Plotline: ${D.done.length} goals reached, ${D.checks} habit check-ins.`;
  if(NATIVE&&NATIVE.shareImage){if(!NATIVE.shareImage(`plotline-${D.y}.png`,url,txt))toast('Couldn’t share the image');return}
  try{const blob=await(await fetch(url)).blob(),file=new File([blob],`plotline-${D.y}.png`,{type:'image/png'});if(navigator.canShare&&navigator.canShare({files:[file]})){await navigator.share({files:[file],text:txt});return}}catch(e){}
  const a=document.createElement('a');a.href=url;a.download=`plotline-${D.y}.png`;document.body.appendChild(a);a.click();a.remove();toast('Image saved')}});
document.addEventListener('keydown',e=>{if(!YR)return;if(e.key==='ArrowRight')ACT.yearStep({d:1});else if(e.key==='ArrowLeft')ACT.yearStep({d:-1});else if(e.key==='Escape')ACT.yearClose()});
