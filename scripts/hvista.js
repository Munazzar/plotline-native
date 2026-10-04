
/* ================= HABIT VISTA =================
 Every habit is a lane: its name and live streak on top, its line through time underneath.
 Done days are stops, streaks are bold glowing stretches, misses are faint, rest days hollow, pauses dotted,
 and habits you're breaking run as one clean line broken by slips (×). Today sits at the right edge.
 Zoom by days in view: Week · Month · 3 months · Year buttons, pinch, or ctrl+wheel. Same layout on phone and desktop. */
const HV_TOP=34,HV_LANE=70,HV_TRACK=48,HVD_MIN=7,HVD_MAX=400;
const HV_SPANS=[[7,'Week'],[30,'Month'],[90,'3 months'],[365,'Year']];
const hvDays=()=>Math.max(HVD_MIN,Math.min(HVD_MAX,+S.settings.layout.hvd||30));
function hvHabits(){return S.habits.filter(h=>h.status!=='archived').sort((a,b)=>(a.kind==='quit')-(b.kind==='quit')||(b.status==='active')-(a.status==='active')||(a.createdAt||0)-(b.createdAt||0))}
const hvCol=i=>`var(--c-${AREA_IDS[i%7]})`;
function hvStart(h){return h.kind==='quit'?dnum(ymd(new Date(h.start||Date.now()))):dnum(h.startDate)}
/* per-day state: d done · p partly · x missed · s rest · z paused · n not due / before start · q clean (quit) · k slip / over limit */
function hvState(h,k,tn){const ds=fromN(k);
 if(h.kind==='quit'){if(k<hvStart(h))return'n';if(h.mode==='limit'){const v=+h.log[ds]||0;return v>h.limit?'k':'q'}return h.slips.some(s=>s.t>=h.start&&ymd(new Date(s.t))===ds)?'k':'q'}
 if(ds<h.startDate)return'n';if(hPaused(h,ds))return'z';if(h.skip[ds])return's';const v=hVal(h,ds),n=hTarget(h);if(v>=n)return'd';if(v>0)return'p';if(!hDue(h,ds))return'n';if(k===tn||h.freq==='times')return'n';return'x'}
function hvLaneStats(h){if(h.kind==='quit'){const d=h.mode==='limit'?lStreak(h):qDays(h);return{big:d,unit:h.mode==='limit'?'days on track':'days free',kept:null}}
 const s=hStreak(h);return{big:s,unit:h.freq==='times'?(s===1?'week':'weeks'):(s===1?'day':'days'),kept:hRate(h)}}
/* the whole drawing; x() maps a day number to pixels */
function hvSvg(L,ppd){const tn=dnum(ymd()),vw=hvVW(),st=Math.min(tn-6,...L.map(hvStart));const a=Math.max(tn-730,Math.min(st-1,tn-Math.ceil(vw/ppd)+1)),pad=Math.max(30,ppd*.6);
 const W=Math.round((tn-a)*ppd+pad*2),H=HV_TOP+L.length*HV_LANE+6,x=k=>+((k-a)*ppd+pad).toFixed(1);let g='';
 /* axis */
 let lastLbl=-1e9;for(let k=a;k<=tn;k++){const d=dUTC(k),dm=d.getUTCDate(),dw=d.getUTCDay();
  if(dm===1){g+=`<line x1="${x(k)}" y1="${HV_TOP-6}" x2="${x(k)}" y2="${H}" class="hv-mo"/>`;if(x(k)-lastLbl>46&&x(tn)-x(k)>58&&x(k)>40){g+=`<text x="${x(k)+5}" y="15" class="hv-t">${d.toLocaleDateString(undefined,{month:'short',...(d.getUTCMonth()===0?{year:'numeric'}:{}),timeZone:'UTC'})}</text>`;lastLbl=x(k)}}
  else if(ppd>=9&&dw===1){g+=`<line x1="${x(k)}" y1="${HV_TOP-2}" x2="${x(k)}" y2="${H}" class="hv-wk"/>`;g+=`<text x="${x(k)}" y="28" class="hv-t s" text-anchor="middle">${dm}</text>`}
  else if(ppd>=30)g+=`<text x="${x(k)}" y="28" class="hv-t s" text-anchor="middle">${DAYS[dw][0]}${dm}</text>`}
 (S.chapters||[]).forEach(c=>{const s=Math.max(a,dnum(c.start)),e=Math.min(tn,dnum(c.end||ymd()));if(e<a||s>tn)return;g+=`<rect x="${x(s)-ppd/2}" y="${HV_TOP-6}" width="${Math.max(3,(e-s+1)*ppd)}" height="${H-HV_TOP+6}" class="hv-chap" style="--cc:${c.color||'#6B9BFF'}"/>`+(ppd<9&&(e-s+1)*ppd>60?`<text x="${x(s)+2}" y="28" class="hv-t s hv-chapt" style="--cc:${c.color||'#6B9BFF'}">${esc((c.emoji?c.emoji+' ':'')+c.title)}</text>`:'')});
 g+=`<line x1="${x(tn)}" y1="${HV_TOP-4}" x2="${x(tn)}" y2="${H}" class="hv-now"/><g transform="translate(${x(tn)},${HV_TOP-14})"><rect x="-21" y="-9" width="42" height="17" rx="8.5" class="hv-nowp"/><text y="3.5" text-anchor="middle" class="hv-nowt">Today</text></g>`;
 const r=Math.max(2.4,Math.min(6,ppd*.32)),dense=ppd<5;
 L.forEach((h,i)=>{const y=HV_TOP+i*HV_LANE+HV_TRACK,c=hvCol(i),S0=[];for(let k=a;k<=tn;k++)S0.push(hvState(h,k,tn));
  let s=`<g class="hv-l${h.status==='paused'?' off':''}" style="--c:${c}">`;const first=S0.findIndex(v=>v!=='n');
  if(first<0){g+=s+`<line x1="${x(a)}" y1="${y}" x2="${x(tn)}" y2="${y}" class="hv-ghost"/></g>`;return}
  s+=`<line x1="${x(a+first)}" y1="${y}" x2="${x(tn)}" y2="${y}" class="hv-base"/>`;
  if(h.kind==='quit'){let s0=first;for(let j=first;j<=S0.length;j++){if(j===S0.length||S0[j]==='k'){if(j-1>=s0)s+=`<line x1="${x(a+s0)}" y1="${y}" x2="${x(a+j-1)}" y2="${y}" class="hv-run${j-s0>=7?' long':''}"/>`;if(j<S0.length){const q=Math.max(3,r*.9);s+=`<g class="hv-slip" transform="translate(${x(a+j)},${y})"><circle r="${q+3}"/><line x1="-${q}" y1="-${q}" x2="${q}" y2="${q}"/><line x1="${q}" y1="-${q}" x2="-${q}" y2="${q}"/></g>`}s0=j+1}}
   s+=`<circle cx="${x(tn)}" cy="${y}" r="${r+1}" class="hv-head"/>`}
  else{const runs=[];let run=[];for(let j=first;j<S0.length;j++){const v=S0[j];if(v==='d')run.push(j);else if(v==='x'||v==='p'){if(run.length)runs.push(run);run=[]}}if(run.length)runs.push(run);
   const inRun=new Set();runs.forEach(R=>{if(R.length>1){R.forEach(j=>inRun.add(j));s+=`<line x1="${x(a+R[0])}" y1="${y}" x2="${x(a+R[R.length-1])}" y2="${y}" class="hv-run${R.length>=7?' long':''}"/>`}const live=a+R[R.length-1]>=tn-1;if(R.length>=5&&!live&&(x(a+R[R.length-1])-x(a+R[0]))>30){const cx=(x(a+R[0])+x(a+R[R.length-1]))/2;s+=`<g transform="translate(${cx},${y+r+12})"><rect x="-13" y="-8" width="26" height="14" rx="7" class="hv-np"/><text y="2.8" text-anchor="middle" class="hv-n">${R.length}</text></g>`}});
   for(let j=first;j<S0.length;j++){const v=S0[j],k=a+j,cx=x(k);if(v==='n')continue;const act=` data-act="hDaySheet" data-id="${h.id}" data-d="${fromN(k)}"`;
    if(dense){if(inRun.has(j))continue;if(v==='d'||v==='p')s+=`<rect x="${cx-ppd/2}" y="${y-5}" width="${Math.max(1,ppd-.4)}" height="10" rx="1" class="hv-bar${v==='p'?' p':''}"/>`;else if(v==='s')s+=`<rect x="${cx-ppd/2}" y="${y-2}" width="${Math.max(1,ppd-.4)}" height="4" class="hv-sbar"/>`;continue}
    if(v==='d'){if(!inRun.has(j)||ppd>=16||k===tn)s+=`<circle cx="${cx}" cy="${y}" r="${r}" class="hv-d${k===tn?' now':''}"${act}/>`;else if(ppd>=6)s+=`<circle cx="${cx}" cy="${y}" r="1.3" class="hv-tick"/>`}
    else if(v==='p')s+=`<circle cx="${cx}" cy="${y}" r="${r}" class="hv-p"${act}/>`;
    else if(v==='x')s+=`<circle cx="${cx}" cy="${y}" r="${Math.max(1.6,r*.45)}" class="hv-x"${act}/>`;
    else if(v==='s')s+=`<circle cx="${cx}" cy="${y}" r="${r*.78}" class="hv-s"${act}/>`;
    else if(v==='z')s+=`<line x1="${cx-ppd/2}" y1="${y}" x2="${cx+ppd/2}" y2="${y}" class="hv-z"/>`;
    if(ppd>=6)s+=`<rect x="${cx-ppd/2}" y="${y-14}" width="${ppd}" height="28" fill="transparent"${act}/>`}
   if(S0[S0.length-1]!=='d'&&hCounts(h,ymd()))s+=`<circle cx="${x(tn)}" cy="${y}" r="${r+1.5}" class="hv-todo" data-act="hTap" data-id="${h.id}"/>`}
  g+=s+'</g>'});
 return{svg:`<svg class="hv-svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}">${g}</svg>`,a,ppd,pad}}
function hvVW(){const sc=document.getElementById('hvsc');return sc&&sc.clientWidth?sc.clientWidth:Math.min(innerWidth,1100)-(innerWidth<=820?32:300)}
const hvPpd=()=>Math.max(.9,Math.min(64,hvVW()/hvDays()));
function hvSpanBtns(){const d=hvDays();const near=HV_SPANS.reduce((b,s)=>Math.abs(Math.log(s[0]/d))<Math.abs(Math.log(b[0]/d))?s:b);return HV_SPANS.map(([v,l])=>`<button class="${near[0]===v?'on':''}" data-act="hvSpan" data-v="${v}">${l}</button>`).join('')}
function hVista(){const L=hvHabits();if(!L.length)return'';const tn=dnum(ymd()),{svg}=hvSvg(L,hvPpd());
 const wk=[...Array(7)].map((_,i)=>dayRatio(fromN(tn-i))).reduce((o,r)=>({due:o.due+r.due,dn:o.dn+r.dn}),{due:0,dn:0}),f=wk.due?wk.dn/wk.due:0;
 const best=L.filter(h=>h.kind!=='quit'&&h.status==='active').map(h=>[h,hStreak(h)]).sort((x,y)=>y[1]-x[1])[0];
 let m30=0;L.forEach(h=>{if(h.kind!=='quit')for(let k=tn-29;k<=tn;k++)if(hDone(h,fromN(k)))m30++});const pr=perfectRun();
 const labs=L.map((h,i)=>{const st=hvLaneStats(h),q=h.kind==='quit';return`<div class="hv-lane" style="--c:${hvCol(i)};top:${HV_TOP+i*HV_LANE}px;height:${HV_LANE}px"><button class="hv-name" data-act="openHabit" data-id="${h.id}"><span class="hv-ic">${esc(hIcon(h))}</span><b>${esc(h.title)}</b>${h.status==='paused'?'<em>Paused</em>':''}</button><span class="hv-st${st.big?'':' z'}">${q?'':st.big?ic('flame'):''}<b>${st.big}</b><small>${st.unit}</small>${st.kept!=null?`<i>${st.kept}%</i>`:''}</span></div>`}).join('');
 return`<section class="hvista rv"><div class="hv-hero"><div class="hv-ring">${hRing(f,76,7)}<b>${Math.round(f*100)}<small>%</small></b></div><div class="hv-hs"><span class="data">Kept this week · ${wk.dn} of ${wk.due}</span><div class="hv-chips">${best&&best[1]?`<span class="hv-chip">${ic('flame')}<b>${best[1]}</b> ${esc(trunc(best[0].title,22))}</span>`:''}<span class="hv-chip">${ic('check')}<b>${m30}</b> done in 30 days</span>${pr>1?`<span class="hv-chip">★ <b>${pr}</b> perfect days</span>`:''}</div></div></div>
 <div class="hv-bar2"><div class="hv-span" role="group" aria-label="Time span">${hvSpanBtns()}</div><span class="data" id="hvrng"></span></div>
 <div class="hv-wrap" style="height:${HV_TOP+L.length*HV_LANE+6}px"><div class="hv-bg">${L.map((h,i)=>`<i style="--c:${hvCol(i)};top:${HV_TOP+i*HV_LANE+4}px;height:${HV_LANE-8}px"></i>`).join('')}</div><div class="hv-sc" id="hvsc">${svg}</div><div class="hv-labs">${labs}</div></div>
 <div class="hv-leg data"><span><i class="d"></i>Done</span><span><b class="ln"></b>Streak</span><span><i class="x"></i>Missed</span><span><i class="s"></i>Rest</span><span><b class="sl">×</b>Slip</span><span><i class="t"></i>Due today, tap to log</span></div></section>`}
/* keep the date under the fingers (or the right edge) still while zooming; only the SVG is redrawn */
let HVK=null,HVA=0;
function hvRange(){const sc=$('#hvsc'),o=$('#hvrng');if(!sc||!o||!HVA)return;const p=hvPpd(),pad=Math.max(30,p*.6),k0=Math.round(HVA+(sc.scrollLeft-pad)/p),k1=Math.min(dnum(ymd()),Math.round(HVA+(sc.scrollLeft+sc.clientWidth-pad)/p)),f=k=>dUTC(k).toLocaleDateString(undefined,{month:'short',day:'numeric',timeZone:'UTC'});o.textContent=f(Math.max(HVA,k0))+' – '+(k1>=dnum(ymd())?'today':f(k1))}
function hvSet(days,ax){const sc=$('#hvsc');if(!sc)return;days=Math.max(HVD_MIN,Math.min(HVD_MAX,days));const oldP=hvPpd(),oldPad=Math.max(30,oldP*.6),atEnd=sc.scrollLeft+sc.clientWidth>=sc.scrollWidth-3,anchor=ax==null?sc.clientWidth:ax,day=HVA+(sc.scrollLeft+anchor-oldPad)/oldP;
 S.settings.layout.hvd=+days.toFixed(2);const R=hvSvg(hvHabits(),hvPpd());sc.innerHTML=R.svg;HVA=R.a;sc.scrollLeft=atEnd&&ax==null?sc.scrollWidth:(day-R.a)*R.ppd+R.pad-anchor;const g=$('.hv-span');if(g)g.innerHTML=hvSpanBtns();hvRange()}
function hvInit(){const sc=$('#hvsc');if(!sc||sc.dataset.on)return;sc.dataset.on=1;
 const R=hvSvg(hvHabits(),hvPpd());sc.innerHTML=R.svg;HVA=R.a;sc.scrollLeft=sc.scrollWidth;hvRange();
 sc.addEventListener('scroll',()=>{if(!hvInit.q){hvInit.q=1;requestAnimationFrame(()=>{hvInit.q=0;hvRange()})}},{passive:true});
 sc.addEventListener('touchstart',e=>{if(e.touches.length!==2)return;const[p,q]=e.touches,r=sc.getBoundingClientRect();HVK={d:Math.abs(p.clientX-q.clientX)||1,v:hvDays(),x:(p.clientX+q.clientX)/2-r.left}},{passive:true});
 sc.addEventListener('touchmove',e=>{if(!HVK||e.touches.length!==2)return;e.preventDefault();const[p,q]=e.touches,d=Math.abs(p.clientX-q.clientX)||1,t=HVK.v*HVK.d/d;if(!HVK.raf)HVK.raf=requestAnimationFrame(()=>{if(HVK){HVK.raf=0;hvSet(t,HVK.x)}})},{passive:false});
 sc.addEventListener('touchend',e=>{if(HVK&&e.touches.length<2){HVK=null;HSDRAG=Date.now();save()}},{passive:true});
 sc.addEventListener('wheel',e=>{if(!e.ctrlKey)return;e.preventDefault();const r=sc.getBoundingClientRect();hvSet(hvDays()*Math.exp(e.deltaY*.004),e.clientX-r.left);clearTimeout(hvInit.t);hvInit.t=setTimeout(save,300)},{passive:false})}
/* animate between spans so the change reads as a zoom */
Object.assign(ACT,{hvSpan:d=>{const to=+d.v,from=hvDays();if(reduced()||!$('#hvsc')){hvSet(to);save();return}const t0=performance.now(),f=t=>{const k=Math.min(1,(t-t0)/320),e=1-Math.pow(1-k,3);hvSet(Math.exp(Math.log(from)+(Math.log(to)-Math.log(from))*e));if(k<1)requestAnimationFrame(f);else save()};requestAnimationFrame(f)}});
