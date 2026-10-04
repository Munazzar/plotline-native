/* ================= GUIDED TOUR =================
   Runs on a sandbox copy of example data: the real state is snapshotted, saving and syncing are paused,
   and everything is restored when the tour ends (even if the app is closed mid-tour, nothing was saved). */
let TOURING=false,TR=null;
const TOUR_STEPS=[
 {r:'today',c:1,t:'Welcome to Plotline',x:'A two-minute walk through everything the app does. The tour runs on example data, so your own plan stays exactly as it is.'},
 {r:'today',s:'.tg-box',t:'Today’s goals',x:'Type what you want to finish today and press +. Tap a circle to check it off. Anything unfinished from earlier waits below, one tap from moving to today.',d:'type'},
 {r:'today',s:'.now',t:'Up next',x:'The most urgent step across all your goals. Mark it done, or start a focus timer that chimes when the time is up.',d:'tap',tap:'.now .btn.ink'},
 {r:'today',s:'.hstrip',t:'Habits at a glance',x:'Today’s habits sit right on the Today page. Tap a ring to log one, and swipe for the rest.',d:'swipe',sc:'.hstrip'},
 {r:'habits',nav:1,s:'.hsum',t:'Habits',x:'Build good habits, break bad ones and run routines. The ring is today, the bars are the last seven days.'},
 {r:'habits',s:'.hrow',t:'Log a habit',x:'Tap the ring to log it. Counted habits, like 8 glasses of water, fill one tap at a time. The dots are the last seven days: tap one to fix a day you forgot.',d:'tap',tap:'.hrow .hchk',click:1},
 {r:'habits',s:'.hq',t:'Break a habit',x:'A live clock counts your time free, with milestones along the way. When a craving hits, tap “I have an urge” for a guided breathing timer. Slips are logged honestly and the clock starts again.',d:'tap',tap:'.hq .btn.ink'},
 {r:'habits',prep:()=>{HV='routine'},s:'.rcard',t:'Routines',x:'A routine is a short sequence you run the same way each time, like a morning start. Press Start and a timer walks you through each step.',d:'tap',tap:'.rcard .btn.ink'},
 {r:'habits',prep:()=>{HV='templates'},s:'.tplg',t:'Templates',x:'Ready-made habits, habit breakers and routines. Tap one, adjust it, and you’re going. They’re always one tap away under Templates.',d:'tap',tap:'.tpl'},
 {r:'goals',nav:1,prep:()=>{HV='today';S.settings.layout.goals='grid'},s:'.fgrid .fc',t:'Goals',x:'Every goal is a card. Tap one to flip it and see the next step and due date. Sub-goals sit right after the goal they belong to.',d:'flip'},
 {r:'goals',s:'.bar',t:'Filter and switch views',x:'Show active, short-term, long-term or achieved goals, filter by life area, or switch to a swipeable carousel.'},
 {r:'goal',id:()=>(S.goals.find(g=>kids(g).length)||S.goals[0]).id,prep:()=>{S.settings.layout.goal='cards'},s:'.hero',t:'A goal up close',x:'Why it matters, the dates, and progress that includes every sub-goal beneath it. Pin it to Today with the pin, edit it with the pencil.'},
 {r:'goal',s:'.bar .seg-c',t:'Three ways to see the path',x:'Cards, a path, or a flowing timeline. Steps and sub-goals, at any depth, appear in all three.',d:'cycle'},
 {r:'goal',s:'.hs-shell',t:'Swipe through steps',x:'Swipe the cards. Tap a card to flip it for notes and actions, and tap its dot to mark the step done. Sub-goal cards open that sub-goal.',d:'swipe',sc:'.hs-shell .hs'},
 {r:'cal',nav:1,prep:()=>{S.settings.layout.cal='month'},s:'.cal-card',t:'Calendar',x:'Day goals, steps and goal targets together. Switch between Day, Week and Month, and swipe to move through dates.',d:'swipe'},
 {r:'cal',prep:()=>{S.settings.layout.cal='day';CAL.d=ymd()},s:'#dgrid',t:'Plan the day by the hour',x:'Tap an empty slot to add something at that time. Pinch, or use the zoom buttons, to see more or less of the day.',d:'pinch'},
 {r:'road',nav:1,s:'#road',t:'Timeline',x:'Every goal flows on its own path across the months, with steps as points along it. Tap a path to open it up.',d:'swipe',sc:'#road'},
 {r:'map',nav:1,s:'#map',t:'Life map',x:'Goals grouped by life area, with lines where goals support each other. Drag anything, pinch to zoom, tap to focus.',d:'drag'},
 {r:'journal',nav:1,s:'.hs-shell,.spine',t:'Journal',x:'Moments, photos, milestones and achieved goals collect here as the story of your year.'},
 {r:'ai',nav:1,s:'.fstep',t:'Plan with AI',x:'Describe what you want in your own words. Copy the prompt into any AI assistant, paste its reply back, then review one main goal with everything nested under it before anything is created.',d:'tap',tap:'[data-act=copyPrompt]'},
 {r:'settings',nav:1,s:'#syncPanel',t:'Sync and settings',x:'Connect your own Google Drive to keep phone and web in step. Themes, card styles, reminders, a PIN and backups are here too.'},
 {r:'today',c:1,t:'Pull down to sync',x:'On a phone, pull down at the top of any page to sync or refresh.',d:'pull'},
 {r:'today',c:1,native:1,t:'Home-screen widgets',x:'Long-press your home screen, choose Widgets, then Plotline. Check off goals and habits, see your week, or watch a live clean-time clock without opening the app.',d:'widget'},
 {r:'today',c:1,t:'You’re all set',x:'Your own plan is back, exactly as you left it. Replay this tour any time from Settings.'},
].filter(s=>!s.native||NATIVE);
function tourPrompt(){if(TOURING||$('.tprompt'))return;S.settings.tourOffered=true;save();const d=document.createElement('div');d.className='tprompt';d.setAttribute('role','dialog');d.innerHTML=`${LOGO}<div><b>Take a quick tour?</b><span>Two minutes, with examples, through goals, habits, the calendar, timeline and more.</span></div><div class="tp-a"><button class="btn ghost sm" data-act="tourNo">Not now</button><button class="btn pri sm" data-act="tourGo">Start tour</button></div>`;document.body.appendChild(d);requestAnimationFrame(()=>d.classList.add('on'))}
function startTour(){document.querySelector('.tprompt')?.remove();if(TOURING)return;closeSheet();RP=null;URGE=null;const st=S.settings;
 TR={prev:JSON.stringify(S),back:location.hash,hv:HV,i:-1,timers:[],pos:0};TOURING=true;
 S=norm({settings:{...JSON.parse(JSON.stringify(st)),sync:{...st.sync,on:false},timer:null,pinHash:''}});seedDemo();
 const run=S.goals.find(g=>g.title==='Run a 10K race');if(run){const sub=newGoal({title:'Build strength for race day',area:'health',horizon:'month',parent:run.id,targetDate:addDays(40),steps:[{title:'Two leg days a week',due:addDays(4)},{title:'Core routine after each run',due:addDays(12)}]});S.goals.push(sub)}
 const o=document.createElement('div');o.className='tour';o.innerHTML=`<div class="tblock"></div><div class="tspot"></div><div class="tfinger"><i></i><i></i></div><div class="tcard" role="dialog" aria-live="polite"></div><div class="tbadge data">Tour · example data</div>`;document.body.appendChild(o);
 TR.pos=setInterval(tourPlace,220);window.addEventListener('resize',tourPlace);document.addEventListener('scroll',tourPlace,{passive:true});tourStep(0)}
function tourEl(q){if(!q)return null;for(const s of q.split(','))for(const e of document.querySelectorAll(s.trim()))if(e.offsetParent||e.getClientRects().length)return e;return null}
function navEl(r){return[...document.querySelectorAll(`[data-nav="${r}"],a.ibtn[href="#/${r}"]`)].find(e=>e.getClientRects().length&&getComputedStyle(e).visibility!=='hidden'&&e.offsetParent)}
const tWait=ms=>new Promise(r=>{TR.timers.push(setTimeout(r,reduced()?Math.min(ms,120):ms))});
async function tourStep(i){if(!TR)return;TR.timers.forEach(clearTimeout);TR.timers=[];const tok=TR.tok=Math.random();const st=TOUR_STEPS[i];if(!st)return endTour();const back=i<TR.i;TR.i=i;TR.focus=null;tourFinger('');
 const route=st.r==='goal'?'goal/'+(st.id?st.id():TR.gid):st.r;if(st.r==='goal'&&st.id)TR.gid=st.id();
 const here=(cur.p+(cur.id?'/'+cur.id:''))===route;
 if(!here&&st.nav&&!back){const n=navEl(st.r);if(n){TR.focus=n;tourCard(st,i,true);tourPlace();await tWait(450);if(TR.tok!==tok)return;tourFinger('tap',n);await tWait(900);if(TR.tok!==tok)return}}
 if(st.prep)st.prep();
 if(!here)go(route);else render(false);await tWait(here?120:520);if(TR.tok!==tok)return;
 const el=st.c?null:tourEl(st.s);if(el){const r=el.getBoundingClientRect();if(r.top<70||r.bottom>innerHeight-150){el.scrollIntoView({block:r.height>innerHeight*.55?'start':'center',behavior:'auto'});if(r.height>innerHeight*.55)scrollBy(0,-80)}}else if(!st.c)window.scrollTo(0,0);
 TR.focus=el;tourCard(st,i,false);tourPlace();await tWait(650);if(TR.tok!==tok)return;tourDemo(st,el,tok)}
function tourCard(st,i,nav){const c=$('.tour .tcard'),N=TOUR_STEPS.length;c.classList.toggle('mid',!!st.c);
 c.innerHTML=nav?`<div class="data">Step ${i+1} of ${N}</div><h3>${esc(st.t)}</h3><p>Opening it from the menu…</p>`
 :`<div class="tc-bar"><i style="width:${((i+1)/N*100).toFixed(1)}%"></i></div><div class="data">Step ${i+1} of ${N}</div>${st.d==='widget'?tourWidgetMock():''}${st.d==='pull'?`<div class="tpull"><i></i><span>${ic('reset')}</span></div>`:''}<h3>${esc(st.t)}</h3><p>${esc(st.x)}</p><div class="tc-a">${i===N-1?'':`<button class="link" data-act="tourEnd">Skip tour</button>`}<span style="flex:1"></span>${i?`<button class="btn ghost sm" data-act="tourPrev">Back</button>`:''}<button class="btn pri sm" data-act="tourNext">${i===N-1?'Finish':i?'Next':'Start'}</button></div>`;
 c.classList.remove('in');void c.offsetWidth;c.classList.add('in')}
function tourWidgetMock(){return`<div class="twid"><div class="tw-h"><b>HABITS TODAY</b><span>2/4</span></div>${[['💧','Drink water','3/8',0],['📖','Read before bed','✓',1],['💪','Exercise','',0],['🙏','Pray on time','✓',1]].map(([e,t,v,d],k)=>`<div class="tw-r" style="--k:${k}"><span>${e}</span><em>${t}</em><i class="${d?'on':''}">${v}</i></div>`).join('')}</div>`}
function tourPlace(){if(!TR)return;const sp=$('.tour .tspot'),c=$('.tour .tcard');if(!sp||!c)return;const el=TR.focus&&document.contains(TR.focus)?TR.focus:(TOUR_STEPS[TR.i]&&!TOUR_STEPS[TR.i].c?tourEl(TOUR_STEPS[TR.i].s):null);if(el&&el!==TR.focus)TR.focus=el;
 const mob=innerWidth<=820,cw=c.offsetWidth,ch=c.offsetHeight;
 if(!el){sp.style.opacity=0;sp.style.transform='translate(50vw,50vh)';sp.style.width=sp.style.height='0px';c.style.left=Math.max(12,(innerWidth-cw)/2)+'px';c.style.top=Math.max(12,(innerHeight-ch)/2)+'px';return}
 const r=el.getBoundingClientRect(),pad=8,top=Math.max(6,r.top-pad),bot=Math.min(innerHeight-6,r.bottom+pad);sp.style.opacity=1;sp.style.transform=`translate(${r.left-pad}px,${top}px)`;sp.style.width=(r.width+pad*2)+'px';sp.style.height=Math.max(0,bot-top)+'px';
 let x,y;if(mob){x=(innerWidth-cw)/2;const below=innerHeight-bot,above=top;y=below>=ch+24?Math.min(bot+14,innerHeight-ch-100):above>=ch+24?Math.max(12,top-ch-14):innerHeight-ch-96}
 else{x=Math.min(innerWidth-cw-16,Math.max(16,r.left));y=innerHeight-bot>=ch+24?bot+14:top>=ch+24?top-ch-14:Math.max(16,innerHeight-ch-24);if(r.width>innerWidth*.5&&y===Math.max(16,innerHeight-ch-24))x=innerWidth-cw-24}
 c.style.left=Math.round(Math.max(12,x))+'px';c.style.top=Math.round(Math.max(12,y))+'px'}
function tourFinger(kind,el,dx=0,dy=0){const f=$('.tour .tfinger');if(!f)return;f.className='tfinger';if(!kind||reduced())return;const r=el?el.getBoundingClientRect():{left:innerWidth/2,top:innerHeight/2,width:0,height:0};
 f.style.left=(r.left+r.width/2+dx)+'px';f.style.top=(r.top+r.height/2+dy)+'px';void f.offsetWidth;f.classList.add('on',kind)}
async function tourDemo(st,el,tok){const ok=()=>TR&&TR.tok===tok;if(!st.d||!ok())return;
 if(st.d==='tap'){const t=el&&el.querySelector(st.tap)||tourEl(st.tap);if(!t)return;tourFinger('tap',t);await tWait(700);if(!ok())return;if(st.click){t.click();await tWait(500);if(!ok())return;TR.focus=tourEl(st.s)}await tWait(900);if(ok())tourFinger('')}
 else if(st.d==='type'){const inp=el&&el.querySelector('input');if(!inp)return;tourFinger('tap',inp,-60);await tWait(600);const txt='Call the bank about the car loan';for(let k=1;k<=txt.length&&ok();k++){inp.value=txt.slice(0,k);await tWait(38)}tourFinger('tap',el.querySelector('.tg-add button'));await tWait(1400);if(!ok())return;inp.value='';const ck=el.querySelector('.li:not(.done) .chk');if(ck){tourFinger('tap',ck);await tWait(650);if(!ok())return;ck.click();await tWait(400);TR.focus=tourEl(st.s)}await tWait(1000);if(ok())tourFinger('')}
 else if(st.d==='swipe'){const sc=st.sc?tourEl(st.sc):null;tourFinger('swipe',el);if(sc){await tWait(500);sc.scrollBy({left:Math.min(300,sc.clientWidth*.7),behavior:'smooth'});await tWait(1500);if(!ok())return;sc.scrollBy({left:-Math.min(300,sc.clientWidth*.7),behavior:'smooth'})}await tWait(1600);if(ok())tourFinger('')}
 else if(st.d==='flip'){if(!el)return;tourFinger('tap',el);await tWait(650);if(!ok())return;el.classList.add('flip','flipping');await tWait(2400);if(!ok())return;el.classList.remove('flip');tourFinger('')}
 else if(st.d==='cycle'){for(const v of ['path','orbit','cards']){const b=tourEl(`.bar .seg-c [data-v="${v}"]`);if(!b||!ok())return;tourFinger('tap',b);await tWait(600);if(!ok())return;ACT.gvMode({v});S.settings.layout.goal=v;await tWait(1500);TR.focus=tourEl(st.s)}if(ok())tourFinger('')}
 else if(st.d==='pinch'){tourFinger('pinch',el);await tWait(700);if(!ok())return;ACT.calZoom({d:'1'});await tWait(1400);if(!ok())return;ACT.calZoom({d:'-1'});await tWait(900);if(ok())tourFinger('')}
 else if(st.d==='drag'){tourFinger('drag',el,-40,-20);await tWait(2600);if(ok())tourFinger('')}
 else if(st.d==='pull'){const p=$('.tour .tpull');if(p)p.classList.add('go')}}
function endTour(){if(!TR)return;const t=TR;TR=null;t.timers.forEach(clearTimeout);clearInterval(t.pos);window.removeEventListener('resize',tourPlace);document.removeEventListener('scroll',tourPlace);
 document.querySelector('.tour')?.remove();closeSheet();RP=null;URGE=null;TOURING=false;S=norm(JSON.parse(t.prev));HV=t.hv||'today';primeSig();S.settings.toured=true;S.settings.tourOffered=true;save();applyTheme();
 const b=t.back&&t.back.startsWith('#/')?t.back.slice(2):'today';if(location.hash==='#/'+b)route();else go(b);toast('Tour finished. Your own plan is back.')}
Object.assign(ACT,{tourGo:()=>startTour(),tourNo:()=>{const d=$('.tprompt');if(d){d.classList.remove('on');setTimeout(()=>d.remove(),400)}toast('You can take the tour any time from Settings')},
 tourNext:()=>{if(TR)tourStep(TR.i+1)},tourPrev:()=>{if(TR&&TR.i>0)tourStep(TR.i-1)},tourEnd:()=>endTour()});
document.addEventListener('keydown',e=>{if(!TR)return;if(e.key==='ArrowRight'||e.key==='Enter'){e.preventDefault();e.stopPropagation();ACT.tourNext()}else if(e.key==='ArrowLeft'){e.preventDefault();ACT.tourPrev()}else if(e.key==='Escape'){e.preventDefault();endTour()}},true);
