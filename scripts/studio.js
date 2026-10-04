/* ================= LIVE HOME: STUDIO =================
 A cosy, hand-painted-feeling room on the Today page (settings.home='studio'):
  - a cork work board on the wall with everything active pinned to it: today's list, habits due today, active goals;
  - a workbench under it with the journal, a tray of what's "on the bench" (next steps, paused habits), a lamp, a mug and a plant;
  - a window on the right showing the real sky for this time of day: sunrise, day, sunset, night, moon and stars;
  - a cat that strolls in now and then, jumps up on the bench, sits a while, wanders along it and jumps off again.
 Play / pause (settings.studio.play, on by default; off by default with reduced motion). Everything is HTML/SVG/CSS, no images.
 The scene re-renders with the page; the cat's walk and the clouds keep their place (timeline in CAT, negative animation delays). */
const st0=()=>({play:!reduced(),...(S.settings.studio||{})});
const studioOn=()=>S.settings.home==='studio';
const ST_COLORS={nightTop:'#0A1638',nightBot:'#283A6E',dayTop:'#3D8FE2',dayBot:'#C6E8F8',duskTop:'#5D4B8C',duskBot:'#F5A06B',dawnBot:'#F7C48F'};
const hx=h=>[1,3,5].map(i=>parseInt(h.slice(i,i+2),16)),toHex=a=>'#'+a.map(v=>Math.round(Math.max(0,Math.min(255,v))).toString(16).padStart(2,'0')).join('');
const mixC=(a,b,t)=>{const A=hx(a),B=hx(b);return toHex(A.map((v,i)=>v+(B[i]-v)*t))};
const clamp01=v=>Math.max(0,Math.min(1,v));
/* sun times: a gentle seasonal day length (about Chicago's), solar noon ~12:45 local */
function skyNow(d=new Date(window.ST_FAKE||Date.now())){const doy=Math.floor((d-new Date(d.getFullYear(),0,0))/864e5),len=12+3*Math.sin(2*Math.PI*(doy-80)/365),noon=12.75,rise=noon-len/2,set=noon+len/2;
 const h=d.getHours()+d.getMinutes()/60;let alt,sunX=null,moonX=null;
 if(h>=rise&&h<=set){const p=(h-rise)/(set-rise);alt=Math.sin(Math.PI*p);sunX=p}else{const nl=24-(set-rise),p=((h-set+24)%24)/nl;alt=-Math.sin(Math.PI*p);moonX=p}
 const amb=clamp01((alt+.08)/.38),tw=clamp01(1-Math.abs(alt)/.22),morning=h<noon;
 let top=mixC(ST_COLORS.nightTop,ST_COLORS.dayTop,amb),bot=mixC(ST_COLORS.nightBot,ST_COLORS.dayBot,amb);
 top=mixC(top,ST_COLORS.duskTop,tw*.55);bot=mixC(bot,morning?ST_COLORS.dawnBot:ST_COLORS.duskBot,tw*.85);
 return{amb,tw,alt,sunX,moonX,top,bot,night:amb<.15}}
/* ---- what goes on the board and on the bench ---- */
function studioData(narrow){const td=ymd();const list=S.days.filter(x=>x.date===td).sort(byTime);
 const habits=S.habits.filter(h=>h.status==='active'&&hDue(h,td));const goals=active().filter(g=>!g.parent||!G(g.parent)||G(g.parent).status!=='active').sort((a,b)=>(b.pinned?1:0)-(a.pinned?1:0)||a.priority-b.priority);
 const nH=narrow?6:8,nG=12;
 const steps=active().map(g=>({g,s:nextStep(g)})).filter(x=>x.s).sort(urgency).slice(0,narrow?2:3);
 const paused=S.habits.filter(h=>h.status==='paused').slice(0,1);
 return{list,habits:habits.slice(0,nH),goals:goals.slice(0,nG),more:Math.max(0,habits.length-nH)+Math.max(0,goals.length-nG),steps,paused}}
const rot=id=>{let n=0;for(const c of String(id))n=(n*31+c.charCodeAt(0))|0;return((Math.abs(n)%9)-4)*.7};
function studioBoard(D){const td=ymd(),nm=S.settings.name;const hr=new Date().getHours(),gr=hr<5?'Still up':hr<12?'Good morning':hr<17?'Good afternoon':'Good evening';
 const listDone=D.list.filter(x=>x.done).length;
 const paper=`<div class="sn sn-list" style="--r:-1.2deg"><i class="pin"></i><b>Today</b>${D.list.length?D.list.slice(0,5).map(x=>`<button class="sn-li${x.done?' d':''}" data-act="toggleDay" data-id="${x.id}" aria-label="${x.done?'Undo':'Done'}: ${esc(x.title)}"><span></span>${esc(trunc(x.title,34))}</button>`).join('')+(D.list.length>5?`<small>+${D.list.length-5} more</small>`:''):`<small>Nothing planned yet</small>`}<small class="sn-c">${listDone} of ${D.list.length}</small></div>`;
 const hab=D.habits.map(h=>{const dn=hDone(h,td),n=hTarget(h),v=Math.min(n,+hVal(h,td)||0),cnt=n>1&&!dn;return`<div class="sn sn-hab${dn?' d':''}" style="${cvar(h)};--r:${rot(h.id)}deg"><i class="tape"></i><button class="sn-open" data-act="openHabit" data-id="${h.id}"><em>${esc(hIcon(h))}</em><span>${esc(trunc(h.title,28))}${cnt?`<small class="sn-cnt">${v} of ${n}${h.unit?' '+esc(h.unit):''}</small>`:''}</span></button><button class="sn-chk${cnt?' cnt':''}" style="--p:${n>1?Math.round(v/n*100):dn?100:0}" data-act="hTap" data-id="${h.id}" aria-label="${dn?'Undo':cnt?`Add one (${v} of ${n})`:'Mark done'}: ${esc(h.title)}">${cnt?`<b>${v}</b>`:ic('check')}</button></div>`}).join('');
 const gl=D.goals.map(g=>{const p=pct(g),nx=nextStep(g);return`<button class="sn sn-goal" style="${cvar(g)};--r:${rot(g.id)}deg" data-act="open" data-id="${g.id}"><i class="pin"></i><span class="sn-a">${esc(areaName(g.area))}</span><b>${esc(trunc(g.title,44))}</b><span class="sn-bar"><i style="width:${p}%"></i></span><small>${p}%${nx?' · '+esc(trunc(nx.title,30)):''}</small></button>`}).join('');
 const more=D.more?`<a class="sn sn-more" href="#/goals" style="--r:2deg">+${D.more} more</a>`:'';
 return`<div class="st-board" aria-label="Your work board"><div class="st-cork"><div class="st-hello"><span>${esc(gr)}${nm?', '+esc(nm):''}</span><small>${new Date().toLocaleDateString(undefined,{weekday:'long',month:'short',day:'numeric'})}</small></div><div class="st-notes">${paper}${hab}${gl}${more}</div></div></div>`}
function studioBench(D){const today=S.entries.filter(e=>dayOf(e.t)===ymd());const mood=today.find(e=>e.mood);
 const cards=[...D.steps.map(({g,s})=>`<button class="bc" style="${cvar(g)}" data-act="open" data-id="${g.id}"><b>${esc(trunc(s.title,30))}</b><small>${esc(dueShort(s)||'next')}</small></button>`),...D.paused.map(h=>`<button class="bc paused" style="${cvar(h)}" data-act="openHabit" data-id="${h.id}"><b>${esc(hIcon(h))} ${esc(trunc(h.title,24))}</b><small>paused</small></button>`)].join('');
 return`<div class="st-desk"><div class="st-top"></div><div class="st-front"></div><i class="st-leg l"></i><i class="st-leg r"></i></div>
 <button class="st-book" data-act="stJournal" aria-label="Journal · ${today.length} today"><span class="pg l"></span><span class="pg r"><b>${mood?esc(mood.mood):'✎'}</b><small>${today.length?today.length+' today':'write'}</small></span></button>
 <div class="st-tray" aria-label="On the bench">${cards||`<span class="bc empty"><small>Bench is clear</small></span>`}</div>
 <div class="st-cup" aria-hidden="true"><i class="pen a"></i><i class="pen b"></i><i class="pen c"></i></div>
 <div class="st-books" aria-hidden="true"><i class="bk a"></i><i class="bk b"></i><i class="bk c"></i></div>
 <div class="st-mug" aria-hidden="true">${art('cup')}<i class="steam a"></i><i class="steam b"></i></div>
 <div class="st-plant" aria-hidden="true">${art('plant')}</div>
 <div class="st-lamp" aria-hidden="true"><i class="cone"></i><i class="pool"></i><i class="glow"></i>${STUDIO_LAMP}</div>`}
const STUDIO_PLANT=`<svg viewBox="0 0 60 80"><ellipse cx="30" cy="78" rx="18" ry="2.6" fill="rgba(0,0,0,.25)"/><path d="M30 52C28 36 16 30 8 30c4 10 12 18 22 22zM30 52c2-18 14-26 24-26-3 12-12 22-24 26zM30 54c-1-20 4-34 10-42 3 14-1 30-10 42z" fill="#5E9C5B"/><path d="M30 54c-6-14-14-20-22-22M30 54c6-16 16-24 24-26" stroke="#3F7343" stroke-width="1.4" fill="none"/><path d="M14 52h32l-4 26H18z" fill="#C8734B"/><path d="M14 52h10l-2 26h-4z" fill="rgba(255,255,255,.14)"/><path d="M40 52h6l-4 26h-4z" fill="rgba(0,0,0,.14)"/><path d="M12 50h36v6H12z" fill="#D9895E"/></svg>`;
const STUDIO_LAMP=`<svg viewBox="0 0 70 110"><defs><linearGradient id="lsh" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#3F8B85"/><stop offset="1" stop-color="#1F4C4A"/></linearGradient><linearGradient id="lmt" x1="0" x2="1"><stop offset="0" stop-color="#6B6058"/><stop offset=".5" stop-color="#3B332D"/><stop offset="1" stop-color="#211C18"/></linearGradient></defs>
<ellipse cx="35" cy="106" rx="20" ry="3" fill="rgba(0,0,0,.28)"/><path d="M16 104h38" stroke="url(#lmt)" stroke-width="7" stroke-linecap="round"/><path d="M35 102V58l-14-22" stroke="url(#lmt)" stroke-width="4" fill="none" stroke-linecap="round"/><circle cx="35" cy="58" r="4.2" fill="#3B332D"/><circle cx="21" cy="36" r="3.4" fill="#3B332D"/>
<path d="M4 30l27-23 19 21-27 22z" fill="url(#lsh)"/><path d="M8 29l23-19" stroke="rgba(255,255,255,.25)" stroke-width="1.6"/><path d="M23 50l27-22 4 7-25 20z" fill="#1B3F3E"/><ellipse class="bulb" cx="40" cy="42" rx="5" ry="3" transform="rotate(-40 40 42)" fill="#FFE7B0"/></svg>`;
const CAT_LEG_H=(cls,fill)=>`<g class="lg hind ${cls}"><g class="j hip"><path d="M32 50C26 64 36 81 54 82c10 0 13-11 9-24-4-12-23-16-31-8z" fill="${fill}"/>
<g class="j knee"><circle cx="54" cy="76" r="6" fill="${fill}"/><path d="M49 72c-5 6-8 13-8 19h9c1-6 5-11 10-16z" fill="${fill}"/><g class="j hock"><circle cx="45.5" cy="90" r="4.6" fill="${fill}"/><path d="M41 89c1 6 3 10 5 14h9c0-4-3-8-5-14z" fill="${fill}"/>
<path class="toe" d="M44 104.6c0-3.6 3.4-5.6 8-5.6s8.6 2 8.6 5.6z" fill="${cls==='n'?'#FBEBD9':'#E2C9AE'}"/></g></g></g></g>`;
const CAT_LEG_F=(cls,fill)=>`<g class="lg front ${cls}"><g class="j sho"><path d="M96 46c-4 12-3 24 2 36h12c4-12 5-24 2-36-5-4-11-4-16 0z" fill="${fill}"/>
<g class="j elb"><circle cx="104.5" cy="79" r="5.6" fill="${fill}"/><path d="M99.6 78l.8 20h8.2l.8-20z" fill="${fill}"/><g class="j wri"><circle cx="104.5" cy="97.5" r="4.2" fill="${fill}"/>
<path class="toe" d="M98.6 104.6c0-3.8 2.8-6.2 7-6.2 4 0 8 2.4 8 6.2z" fill="${cls==='n'?'#FBEBD9':'#E2C9AE'}"/></g></g></g></g>`;
const STUDIO_CAT=`<svg viewBox="0 0 160 110" class="cat-s"><defs>
<linearGradient id="fur" gradientUnits="userSpaceOnUse" x1="0" y1="34" x2="0" y2="106"><stop offset="0" stop-color="#F6B371"/><stop offset=".55" stop-color="#E8924C"/><stop offset="1" stop-color="#C96F35"/></linearGradient>
<radialGradient id="furh" cx=".38" cy=".32" r=".75"><stop offset="0" stop-color="#FAC991"/><stop offset=".7" stop-color="#EC9A55"/><stop offset="1" stop-color="#D47D3D"/></radialGradient>
<linearGradient id="furd" gradientUnits="userSpaceOnUse" x1="0" y1="34" x2="0" y2="106"><stop offset="0" stop-color="#D88540"/><stop offset="1" stop-color="#A65A2A"/></linearGradient>
<radialGradient id="cream" cx=".5" cy=".3" r=".8"><stop offset="0" stop-color="#FFF6EA"/><stop offset="1" stop-color="#F1DCC4"/></radialGradient></defs>
<ellipse class="c-shadow" cx="78" cy="105" rx="46" ry="3.6" fill="rgba(30,15,5,.3)"/>
<g class="pose-walk">${CAT_LEG_H('f','url(#furd)',0)}${CAT_LEG_F('f','url(#furd)',0)}
<g class="c-tail"><path d="M36 52C20 50 11 39 14 25c2-8 8-12 13-10" stroke="url(#fur)" stroke-width="8.5" fill="none" stroke-linecap="round"/><path d="M15.5 21c2-5 6-7 11-6" stroke="#B2602C" stroke-width="8.5" fill="none" stroke-linecap="round"/></g>
<g class="c-body"><path d="M30 57c-2-15 16-22 40-21 22 0 42-1 50 11 6 10 1 25-14 28-14 2-28 2-42 1-18-1-33-4-34-19z" fill="url(#fur)"/>
<path d="M50 70c14 6 40 6 56 0-2 4-6 6-14 7-12 1-28 1-38-1-3-1-4-3-4-6z" fill="#FBE7D2" opacity=".55"/>
<path d="M50 39q5 8 1 16M62 37q5 9 1 17M74 37q4 8 1 15M86 38q4 7 1 13" stroke="#C9682E" stroke-width="3.4" fill="none" stroke-linecap="round" opacity=".7"/>
<path d="M34 50c4-6 12-8 18-6" stroke="rgba(255,240,220,.35)" stroke-width="3" fill="none" stroke-linecap="round"/></g>
${CAT_LEG_H('n','url(#fur)',0)}${CAT_LEG_F('n','url(#fur)',0)}
<g class="c-head" transform="translate(124 36)"><g class="hd-in"><path class="ear l" d="M-9-12l1-17 11 11z" fill="url(#fur)"/><path class="ear r" d="M5-15l12-11 1 17z" fill="url(#fur)"/><path d="M-6-14l1-9 6 6zM8-16l6-6 1 9z" fill="#F4B3A3"/>
<path d="M-16 2c0-12 8-19 18-19s19 7 19 19c0 10-8 15-19 15s-18-5-18-15z" fill="url(#furh)"/><path d="M-3 6c2-5 13-6 16 0 1 5-4 8-8 8s-9-3-8-8z" fill="url(#cream)"/>
<g class="eyes"><ellipse cx="-4" cy="0" rx="2.6" ry="3.4" fill="#2B2420"/><ellipse cx="8.5" cy="0" rx="2.6" ry="3.4" fill="#2B2420"/><circle cx="-3.2" cy="-1.2" r=".9" fill="#fff"/><circle cx="9.3" cy="-1.2" r=".9" fill="#fff"/></g>
<path d="M5 5.5l-2.6 2-2.6-2z" fill="#E07F7F"/><path class="mouth" d="M.5 9q2 4 4 0" fill="#9C3B3B" opacity="0"/><path d="M11 7l13-2M11 9l13 2M-5 7l-12-2M-5 9l-12 2" stroke="#7A5A48" stroke-width=".7" opacity=".7"/>
<ellipse cx="-9" cy="6" rx="3" ry="1.8" fill="#F7A8A0" opacity=".45"/><ellipse cx="16" cy="6" rx="3" ry="1.8" fill="#F7A8A0" opacity=".45"/></g></g></g>
<g class="pose-sit"><g class="c-tail"><path d="M60 104C40 108 28 101 30 89" stroke="url(#fur)" stroke-width="8.5" fill="none" stroke-linecap="round"/><path d="M31 94C30 91 30 89 31 87" stroke="#B2602C" stroke-width="8.5" fill="none" stroke-linecap="round"/></g>
<g class="sit-body"><path d="M54 105C46 82 52 56 76 52c22-3 32 20 28 53z" fill="url(#fur)"/>
<path d="M50 84c0-14 10-22 22-20 12 2 16 16 14 30-1 8-6 11-14 11H56c-4-4-6-12-6-21z" fill="url(#fur)"/><path d="M54 76q8-8 18-6" stroke="rgba(255,240,220,.35)" stroke-width="3" fill="none" stroke-linecap="round"/>
<path d="M58 80q6 5 6 13M66 72q6 4 7 12" stroke="#C9682E" stroke-width="3.2" fill="none" stroke-linecap="round" opacity=".7"/>
<path class="toe" d="M56 105c0-3 4-4.5 9-4.5s9 1.5 9 4.5z" fill="#FFF1E0"/>
<path d="M80 76c-.4 9 0 18 .6 25h8.6c.4-7 .6-16 .2-25-3-2-6.4-2-9.4 0z" fill="url(#furd)"/><path class="toe" d="M79.5 105c0-3.4 2.8-5.5 6.5-5.5s6.5 2.1 6.5 5.5z" fill="#E2C9AE"/>
<ellipse cx="88" cy="70" rx="9.5" ry="14" fill="url(#cream)"/>
<g class="paw-r"><path d="M92 72c-.5 10 0 21 .5 29h9.5c.5-8 .8-19 .5-29-3-2-7-2-10.5 0z" fill="url(#fur)"/><path class="toe" d="M91.5 105c0-3.6 3-5.8 7-5.8s7 2.2 7 5.8z" fill="#FBEBD9"/></g></g>
<g class="c-head" transform="translate(90 38)"><g class="hd-in"><path class="ear l" d="M-15-10l0-18 12 10z" fill="url(#fur)"/><path class="ear r" d="M4-18l13-9 1 18z" fill="url(#fur)"/><path d="M-12-13l0-9 6 5zM7-18l7-5 1 9z" fill="#F4B3A3"/>
<path d="M-17 2c0-12 8-20 18-20s18 8 18 20c0 11-8 16-18 16s-18-5-18-16z" fill="url(#furh)"/><path d="M-6 6c2-5 12-5 14 0 1 5-4 9-7 9s-8-4-7-9z" fill="url(#cream)"/>
<g class="eyes"><ellipse cx="-6" cy="0" rx="2.8" ry="3.6" fill="#2B2420"/><ellipse cx="7" cy="0" rx="2.8" ry="3.6" fill="#2B2420"/><circle cx="-5.1" cy="-1.3" r="1" fill="#fff"/><circle cx="7.9" cy="-1.3" r="1" fill="#fff"/></g>
<path d="M2.6 5.6l-2.6 2-2.6-2z" fill="#E07F7F"/><path class="mouth" d="M-2 9q2 5 4 0z" fill="#9C3B3B" opacity="0"/><path d="M8 7l13-2M8 9l13 2M-8 7l-13-2M-8 9l-13 2" stroke="#7A5A48" stroke-width=".7" opacity=".7"/>
<ellipse cx="-11" cy="6" rx="3" ry="1.8" fill="#F7A8A0" opacity=".45"/><ellipse cx="13" cy="6" rx="3" ry="1.8" fill="#F7A8A0" opacity=".45"/></g></g></g>
<g class="pose-loaf"><path d="M36 104C26 104 24 94 32 92" stroke="url(#fur)" stroke-width="8.5" fill="none" stroke-linecap="round"/><g class="loaf-body"><path d="M36 106c-4-24 14-38 44-38s48 13 46 38z" fill="url(#fur)"/><path d="M58 78q4 6 2 12M72 74q4 7 2 13M86 74q4 7 2 13" stroke="#C9682E" stroke-width="3" fill="none" stroke-linecap="round" opacity=".7"/><path class="toe" d="M112 106c0-3 3-4.5 7-4.5s7 1.5 7 4.5z" fill="#FFF1E0"/></g>
<g transform="translate(112 82)"><path d="M-12-10l-1-15 11 9zM3-15l12-8 0 15z" fill="url(#fur)"/><path d="M-16 4c0-11 7-18 16-18s16 7 16 18c0 9-7 13-16 13s-16-4-16-13z" fill="url(#furh)"/><path d="M-6 8c2-4 10-4 12 0 0 4-3 6-6 6s-7-2-6-6z" fill="url(#cream)"/>
<path d="M-8 3q3 3 6 0M4 3q3 3 6 0" stroke="#2B2420" stroke-width="1.3" fill="none"/><path d="M2 7l-2 1.6-2-1.6z" fill="#E07F7F"/></g>
<g class="zz"><text x="124" y="58">z</text><text x="132" y="47">z</text><text x="140" y="34">Z</text></g></g>
<g class="bubble" opacity="0"><rect x="104" y="0" width="46" height="20" rx="10" fill="#FFFDF6" stroke="#3A2B1E" stroke-width="1"/><path d="M114 19l-4 7 10-7z" fill="#FFFDF6"/><text x="127" y="14" text-anchor="middle">mrrp?</text></g></svg>`;
/* sunlight: soft rays from the window glass that fan out down and to the left, fading into the room */
function studioRays(narrow,W0){const W=W0||(narrow?{x0:56,x1:94,y0:53.5,y1:68.5}:{x0:71,x1:96,y0:7,y1:57});
 const n=7,rays=[];for(let i=0;i<n;i++){const t=i/(n-1),sx=W.x0+1+(W.x1-W.x0-2)*(.08+.84*t),sy=W.y0+2+(W.y1-W.y0-4)*(.15+.7*(1-t)),w=(narrow?1.6:1.1)+((i*7)%3)*.9;
  const ex=sx-(narrow?44:62)-t*(narrow?6:10),ey=104,spread=(narrow?7:9)+((i*5)%4)*3;rays.push(`<polygon points="${(sx-w).toFixed(1)},${sy.toFixed(1)} ${(sx+w).toFixed(1)},${(sy-w*.4).toFixed(1)} ${(ex+spread).toFixed(1)},${ey} ${(ex-spread).toFixed(1)},${ey}" style="animation-delay:-${(i*1.7).toFixed(1)}s;opacity:${(.55+((i*3)%4)*.12).toFixed(2)}"/>`)}
 /* two broad, faint rays that reach across to the top-left corner */
 [[.2,narrow?30:22,18],[.55,narrow?52:44,22]].forEach(([t,ey,sp],k)=>{const sx=W.x0+2,sy=W.y0+(W.y1-W.y0)*t;rays.push(`<polygon class="wide" points="${sx},${(sy-3).toFixed(1)} ${sx},${(sy+4).toFixed(1)} -4,${ey+sp} -4,${ey-sp}" style="animation-delay:-${k*3}s"/>`)});
 return`<svg class="st-rays" viewBox="0 0 100 100" preserveAspectRatio="none" aria-hidden="true"><defs><linearGradient id="rayw" gradientUnits="userSpaceOnUse" x1="${W.x0}" y1="0" x2="0" y2="0"><stop offset="0" style="stop-color:var(--ray)" stop-opacity=".45"/><stop offset=".6" style="stop-color:var(--ray)" stop-opacity=".12"/><stop offset="1" style="stop-color:var(--ray)" stop-opacity="0"/></linearGradient><linearGradient id="rayg" gradientUnits="userSpaceOnUse" x1="${W.x1-4}" y1="${W.y0}" x2="${narrow?20:18}" y2="104"><stop offset="0" style="stop-color:var(--ray)" stop-opacity=".9"/><stop offset=".45" style="stop-color:var(--ray)" stop-opacity=".38"/><stop offset=".8" style="stop-color:var(--ray)" stop-opacity=".08"/><stop offset="1" style="stop-color:var(--ray)" stop-opacity="0"/></linearGradient></defs><g fill="url(#rayg)">${rays.join('')}</g></svg>`}
function studioLights(){const n=innerWidth<640?11:17;let o='',pts=[];for(let i=0;i<n;i++){const x=2+96*i/(n-1),y=3+Math.sin(Math.PI*((i%((n-1)/2))/((n-1)/2)))*3.2;pts.push([x,y])}
 const d=pts.map((p,i)=>(i?'L':'M')+p[0].toFixed(1)+' '+p[1].toFixed(1)).join(' ');
 o=pts.map((p,i)=>`<g transform="translate(${p[0].toFixed(1)} ${(p[1]+.6).toFixed(1)})"><rect x="-.35" y="-.5" width=".7" height=".8" fill="#4A4038"/><ellipse class="bulb" cy="1" rx=".55" ry=".85" style="animation-delay:-${(i*.83)%4}s"/></g>`).join('');
 return`<svg class="st-lights" viewBox="0 0 100 12" preserveAspectRatio="none" aria-hidden="true"><path d="${d}" stroke="#3E3530" stroke-width=".18" fill="none"/>${o}</svg>`}
function studioWindow(){const n=Date.now()/1000;
 const puff=(x,y,r)=>`<circle cx="${x}" cy="${y}" r="${r}" fill="url(#cl)"/>`;
 const cloud=(k,dur,top,sc,op)=>`<g class="cloud" style="animation-duration:${dur}s;animation-delay:-${((n+k*977)%dur).toFixed(1)}s"><g transform="translate(0 ${top}) scale(${sc})" opacity="${op}"><ellipse cx="22" cy="14" rx="24" ry="5" fill="#B9CBE6" opacity=".55"/>${puff(8,10,7)}${puff(18,6,9)}${puff(30,5,11)}${puff(41,9,8)}${puff(26,12,7)}</g></g>`;
 let sd=7;const rn=()=>(sd=(sd*16807)%2147483647)/2147483647;
 const stars=[...Array(70)].map((_,i)=>{const x=rn()*100,y=rn()*rn()*86+1,r=rn()<.12?.75:.28+rn()*.28,tw=rn()<.3;return`<circle class="star${tw?' tw':''}" cx="${x.toFixed(1)}" cy="${y.toFixed(1)}" r="${r.toFixed(2)}" style="opacity:${(.45+rn()*.55).toFixed(2)};animation-delay:-${(rn()*6).toFixed(1)}s;animation-duration:${(3+rn()*4).toFixed(1)}s"/>`}).join('')+'<g class="shoot"><path d="M0 0 L-14 5" stroke="url(#shootg)" stroke-width=".7" stroke-linecap="round"/></g>';
 const clump=(x,y,r,c)=>`<g class="clump"><circle cx="${x}" cy="${y}" r="${r}" fill="${c}"/><circle cx="${x-r*.5}" cy="${y+r*.3}" r="${r*.75}" fill="${c}"/><circle cx="${x+r*.55}" cy="${y+r*.25}" r="${r*.7}" fill="${c}"/><circle class="hl" cx="${x-r*.25}" cy="${y-r*.35}" r="${r*.45}"/></g>`;
 const grass=[...Array(22)].map((_,i)=>`<path d="M${(i*4.7).toFixed(1)} 140q${i%2?1:-1} -${6+i%4*2} ${i%3-1} -${9+i%5*2}" style="animation-delay:-${(i*.37)%3}s"/>`).join('');
 return`<div class="st-win" aria-label="Window"><div class="st-glass"><svg viewBox="0 0 100 140" preserveAspectRatio="xMidYMid slice"><defs><linearGradient id="stsky" x1="0" y1="0" x2="0" y2="1"><stop offset="0" style="stop-color:var(--sky1)"/><stop offset=".68" style="stop-color:var(--sky2)"/></linearGradient><radialGradient id="stsun"><stop offset="0" stop-color="#FFF8DE"/><stop offset=".35" stop-color="#FFE8A8"/><stop offset="1" stop-color="#FFC56B" stop-opacity="0"/></radialGradient>
 <radialGradient id="cl" cx=".42" cy=".3" r=".75"><stop offset="0" stop-color="#FFFFFF"/><stop offset=".65" stop-color="#F3F6FC"/><stop offset="1" stop-color="#C9D6EC"/></radialGradient><linearGradient id="shootg" x1="0" y1="0" x2="-14" y2="5" gradientUnits="userSpaceOnUse"><stop offset="0" stop-color="#fff"/><stop offset="1" stop-color="#fff" stop-opacity="0"/></linearGradient><radialGradient id="mglow"><stop offset="0" stop-color="#FFF8E0" stop-opacity=".55"/><stop offset="1" stop-color="#FFF8E0" stop-opacity="0"/></radialGradient></defs>
 <rect width="100" height="140" fill="url(#stsky)"/><g class="stars">${stars}</g>
 <g class="sun"><circle r="24" fill="url(#stsun)"/><circle r="7" fill="#FFF6D8"/></g><g class="moon"><circle r="16" fill="url(#mglow)"/><path d="M-1.5-6.3A6.5 6.5 0 1 0 5.6 3.4 5.4 5.4 0 0 1-1.5-6.3z" fill="#F6F2E2"/></g>
 <g class="clouds">${cloud(1,150,8,1,.95)}${cloud(2,210,26,.7,.85)}${cloud(3,260,2,.55,.7)}</g>
 <path class="hill far" d="M0 92C10 84 20 80 32 85s20-12 34-11 22 10 34 7v59H0z"/>
 <g class="far-trees">${clump(30,82,3,'var(--ft)')}${clump(37,83,2.4,'var(--ft)')}${clump(72,78,2.8,'var(--ft)')}</g>
 <path class="hill mid" d="M0 104c16-10 34-12 50-5s30 6 50-4v45H0z"/>
 <path class="path" d="M58 140c-2-10 4-18 14-24 6-4 8-8 6-12" stroke-width="4" fill="none"/>
 <g class="house"><path class="wall" d="M64 100h13v-8l-6.5-5.5-6.5 5.5z"/><path d="M62 93l8.5-7.5 8.5 7.5" fill="none" stroke="#B5523B" stroke-width="2.6" stroke-linecap="round"/><rect class="lit" x="69" y="94.5" width="3.6" height="3.6" fill="#FFD27A"/><path class="smoke" d="M75 87q-2-4 1-7t0-7" stroke="#fff" stroke-width="1.4" fill="none" opacity=".5"/></g>
 <path class="hill near" d="M0 116c20-6 40-6 60 2s28 2 40-2v24H0z"/>
 <g class="tree"><path d="M18 122C19 110 17 102 20 92" stroke="#5B4636" stroke-width="3" fill="none"/><path d="M19 104l-6-5M20 98l6-4" stroke="#5B4636" stroke-width="1.6"/>
 ${clump(19,86,9,'var(--tr)')}${clump(11,94,6,'var(--tr)')}${clump(28,93,6.5,'var(--tr)')}</g>
 <g class="grass">${grass}</g>
 <g class="birds"><g class="bird" style="animation-delay:-${(n%46).toFixed(1)}s"><path d="M0 0q2-2 4 0q2-2 4 0"/></g><g class="bird b2" style="animation-delay:-${((n+17)%46).toFixed(1)}s"><path d="M0 0q1.5-1.5 3 0q1.5-1.5 3 0"/></g></g>
 <g class="flies">${[...Array(9)].map((_,i)=>`<circle cx="${6+i*11}" cy="${110+(i*7)%22}" r=".9" style="animation-delay:-${i*.9}s"/>`).join('')}</g></svg><i class="st-shine"></i></div>
 <i class="st-frame v"></i><i class="st-frame h"></i><i class="st-sill"></i><i class="st-jars">${art('sunflower')}${art('jar')}</i><i class="st-cur l"></i><i class="st-cur r"></i><div class="st-hang" aria-hidden="true">${STUDIO_VINE}</div></div>`}
const STUDIO_VINE=`<svg viewBox="0 0 60 120"><path d="M30 0v14" stroke="#7A6450" stroke-width="1"/><path d="M14 14h32l-4 14H18z" fill="#E7DCC8"/><path d="M14 14h32v3H14z" fill="#CDBFA6"/>
<g class="vine v1"><path d="M20 26c-4 20 2 40-4 62" stroke="#4E7F45" stroke-width="1.4" fill="none"/>${[30,42,54,66,78,88].map((y,i)=>`<path d="M${18-(i%2?3:-1)} ${y}c-6-2-8 4-4 7 3 0 5-3 4-7z" fill="${i%2?'#5E9C5B':'#6FB06A'}"/>`).join('')}</g>
<g class="vine v2"><path d="M40 26c6 18 0 34 6 52" stroke="#4E7F45" stroke-width="1.4" fill="none"/>${[34,46,58,70].map((y,i)=>`<path d="M${42+(i%2?3:-1)} ${y}c6-2 8 4 4 7-3 0-5-3-4-7z" fill="${i%2?'#6FB06A':'#5E9C5B'}"/>`).join('')}</g>
<g class="vine v3"><path d="M30 26c-1 26 2 50-2 78" stroke="#4E7F45" stroke-width="1.4" fill="none"/>${[40,56,72,88,100].map((y,i)=>`<path d="M${29+(i%2?2:-2)} ${y}c${i%2?6:-6}-2 ${i%2?8:-8} 4 ${i%2?4:-4} 7-${i%2?3:-3} 0-5-3-4-7z" fill="#64A55F"/>`).join('')}</g></svg>`;
let ST_LIGHT=0;
function studioHTML(){const narrow=innerWidth<640;const D=studioData(narrow),sky=skyNow(),p=st0().play;

 return`<section class="studio${p?'':' paused'}${narrow?' narrow':''}" id="studio" style="${studioVars(sky)}" aria-label="Your studio">
 ${narrow?'<div class="st-camwrap">':''}<div class="st-cam"><div class="st-wall"></div><div class="st-wains"></div>${studioRays(narrow)}<div class="st-motes" aria-hidden="true">${[...Array(9)].map((_,i)=>`<i style="--x:${(i*37)%90+5}%;--y:${(i*53)%80+10}%;--d:${7+i%4*2}s;animation-delay:-${i*1.3}s"></i>`).join('')}</div>${studioLights()}${studioWindow()}${studioClock()}${studioBoard(D)}<div class="st-floor"></div><div class="st-sunpatch" aria-hidden="true"></div><div class="st-rug" aria-hidden="true"></div><div class="st-basket" aria-hidden="true">${art('basket')}</div><div class="st-yarn" aria-hidden="true">${art('yarn')}</div>${studioBench(D)}
 <div class="st-cat" id="stCat" data-act="stCat" role="img" aria-label="The studio cat">${STUDIO_CAT}</div><div class="st-night"></div></div>${narrow?'</div>':''}<div class="st-fade"></div><div class="st-fade-top"></div>
 <button class="st-play" data-act="stPlay" aria-label="${p?'Pause the scene':'Play the scene'}" title="${p?'Pause':'Play'}">${p?'<svg viewBox="0 0 24 24"><rect x="6" y="5" width="4" height="14" rx="1"/><rect x="14" y="5" width="4" height="14" rx="1"/></svg>':'<svg viewBox="0 0 24 24"><path d="M8 5l11 7-11 7z"/></svg>'}</button></section>`}
function studioClock(){const d=new Date(),m=d.getMinutes(),h=d.getHours()%12+m/60,s=d.getSeconds();
 return`<div class="st-clock" aria-label="${d.toLocaleTimeString([],{hour:'numeric',minute:'2-digit'})}"><svg viewBox="0 0 100 100">${[...Array(12)].map((_,i)=>`<line x1="50" y1="9" x2="50" y2="${i%3?14:17}" stroke="#7A6450" stroke-width="${i%3?2:3.4}" transform="rotate(${i*30} 50 50)"/>`).join('')}
 <line class="hd hh" x1="50" y1="50" x2="50" y2="28" stroke-width="5" transform="rotate(${h*30} 50 50)"/><line class="hd mm" x1="50" y1="50" x2="50" y2="18" stroke-width="3.4" transform="rotate(${m*6} 50 50)"/>
 <g class="sec" style="animation-delay:-${s}s"><line x1="50" y1="56" x2="50" y2="16" stroke="#C7362A" stroke-width="1.6"/></g><circle cx="50" cy="50" r="3.6" fill="#3A2B1E"/></svg></div>`}
function studioVars(k){const sx=k.sunX!=null?10+80*k.sunX:-50,sy=k.sunX!=null?100-78*Math.max(0,k.alt):-50,mx=k.moonX!=null?12+76*k.moonX:-50,my=k.moonX!=null?96-70*Math.max(0,-k.alt):-50;
 return`--sky1:${k.top};--sky2:${k.bot};--amb:${k.amb.toFixed(3)};--tw:${k.tw.toFixed(3)};--sunx:${sx.toFixed(1)};--suny:${sy.toFixed(1)};--moonx:${mx.toFixed(1)};--moony:${my.toFixed(1)};--night:${k.night?1:0}`}
function studioSky(){const el=$('#studio');if(!el)return;const k=skyNow();el.setAttribute('style',studioVars(k));el.classList.toggle('is-night',k.amb<.35);el.classList.toggle('is-day',k.amb>.05);const ck=el.querySelector('.st-clock');if(ck)ck.outerHTML=studioClock();const sv=el.querySelector('.st-glass svg');if(!sv)return;
 const s=sv.querySelector('.sun'),m=sv.querySelector('.moon');s.setAttribute('transform',`translate(${k.sunX!=null?10+80*k.sunX:-50} ${k.sunX!=null?100-78*Math.max(0,k.alt):-50})`);m.setAttribute('transform',`translate(${k.moonX!=null?12+76*k.moonX:-50} ${k.moonX!=null?96-70*Math.max(0,-k.alt):-50})`)}
/* ---- the cat: a timeline of legs (walk, jump, sit), in % of the scene; it survives re-renders ---- */
const CAT={t0:0,el:0,run:false,raf:0,plan:null,cycle:0};
/* behaviours: stroll in → jump on the desk → sit a while (look around, groom, yawn, watch the window, or nap at night)
   → jump off the table and out of view. Each leg {d ms, p pose, a/b [x%,y%], h jump height, act, dir}. */
function catPlan(narrow){const fl=narrow?92.5:91.5,dk=narrow?73.2:71,rnd=(a,b)=>a+Math.random()*(b-a),pick=a=>a.splice(Math.floor(Math.random()*a.length),1)[0];
 const fromL=CAT.cycle%2===0,X=v=>fromL?v:100-v,night=skyNow().amb<.2,legs=[];let x=X(-14),dir=fromL?1:-1;
 const ws=(narrow?20:10.5)*.21,walk=(to,y,sp)=>{const speed=ws*(sp||1);dir=to>=x?1:-1;legs.push({d:Math.abs(to-x)/speed*1000,p:'walk',a:[x,y],b:[to,y],dir});x=to};
 const stay=(p,act,d,y)=>legs.push({d,p,act,a:[x,y],b:[x,y],dir});
 walk(X(narrow?24:26),fl);stay('sit','look',rnd(900,1500),fl);
 const up=X(narrow?34:36);legs.push({d:820,p:'jump',a:[x,fl],b:[up,dk],h:narrow?12:15,dir});x=up;
 stay('sit','',700,dk);
 const acts=night?['sleep','groom','look','yawn']:['look','groom','yawn','window'];
 const n=night?2:3;for(let k=0;k<n;k++){const a=pick(acts);
  if(a==='sleep')stay('loaf','sleep',rnd(9000,14000),dk);else stay('sit',a,a==='groom'?rnd(4500,6500):rnd(3000,4500),dk)}
 stay('stretch','stretch',2600,dk);
 const edge=X(narrow?86:84);walk(edge,dk,.9);
 legs.push({d:950,p:'jump',a:[x,dk],b:[X(narrow?118:112),fl],h:9,dir,off:1});x=X(narrow?118:112);
 legs.push({d:rnd(25000,45000),p:'away',a:[x,fl],b:[x,fl],dir});
 return{legs}}
/* the cat is positioned with a GPU transform (no layout), and only animated frame-by-frame while it moves;
   while it sits, naps or is away the loop sleeps until the next leg */
let CATBOX={w:0,h:0};
function catBox(){const c=document.querySelector('#studio .st-cam');if(c)CATBOX={w:c.clientWidth,h:c.clientHeight}}
addEventListener('resize',()=>{catBox()},{passive:true});
function catDraw(t){const el=$('#stCat'),sc=$('#studio');if(!el||!sc)return null;if(!CAT.plan)CAT.plan=catPlan(sc.classList.contains('narrow'));if(!CATBOX.w)catBox();let i=0;const L=CAT.plan.legs;
 while(i<L.length&&t>L[i].d){t-=L[i].d;i++}
 if(i>=L.length){CAT.cycle++;CAT.plan=catPlan(sc.classList.contains('narrow'));CAT.el=0;CAT.t0=performance.now();return catDraw(0)}
 const g=L[i],k=g.d?t/g.d:1,ez=g.p==='walk'?k-Math.sin(2*Math.PI*k)/(2*Math.PI)*.12:g.p==='jump'?k-Math.sin(2*Math.PI*k)/(2*Math.PI)*.35:k;
 const x=g.a[0]+(g.b[0]-g.a[0])*ez,y=g.a[1]+(g.b[1]-g.a[1])*(g.p==='jump'?k*k*(3-2*k):k)-(g.h?Math.sin(Math.PI*k)*g.h:0);
 if(g.p==='jump'){const j=k<.22?'crouch':k>.86?'land':'air';if(el.dataset.j!==j)el.dataset.j=j;el.dataset.up=g.b[1]<g.a[1]?'1':'0'}else if(el.dataset.j){delete el.dataset.j;delete el.dataset.up}
 el.style.transform=`translate3d(${(x/100*CATBOX.w).toFixed(1)}px,${(y/100*CATBOX.h).toFixed(1)}px,0) translate(-50%,-95%) scaleX(${g.dir||1})`;
 if(el.dataset.p!==g.p)el.dataset.p=g.p;const act=g.act||'';if(el.dataset.a!==act)el.dataset.a=act;if(el.style.getPropertyValue('--dir')!==String(g.dir||1))el.style.setProperty('--dir',g.dir||1);
 return{moving:g.p==='walk'||g.p==='jump',left:g.d-t}}
let CATT=0;
function catFrame(now){if(!CAT.run)return;const r=catDraw(CAT.el+(now-CAT.t0));if(!r){CAT.run=false;return}
 if(r.moving)CAT.raf=requestAnimationFrame(catFrame);else{clearTimeout(CATT);CATT=setTimeout(()=>{if(CAT.run)CAT.raf=requestAnimationFrame(catFrame)},Math.max(16,r.left+4))}}
function catStart(){if(CAT.run)return;CAT.run=true;CAT.t0=performance.now();catBox();CAT.raf=requestAnimationFrame(catFrame)}
function catStop(){if(!CAT.run)return;CAT.run=false;cancelAnimationFrame(CAT.raf);clearTimeout(CATT);CAT.el+=performance.now()-CAT.t0}
/* rays start at the real window, wherever the layout put it */
function raysFit(sc){const cam=sc.querySelector('.st-cam'),w=sc.querySelector('.st-glass'),old=sc.querySelector('.st-rays');if(!cam||!w||!old)return;const a=cam.getBoundingClientRect(),b=w.getBoundingClientRect();if(!a.width)return;
 const W={x0:+((b.left-a.left)/a.width*100).toFixed(1),x1:+((b.right-a.left)/a.width*100).toFixed(1),y0:+((b.top-a.top)/a.height*100).toFixed(1),y1:+((b.bottom-a.top)/a.height*100).toFixed(1)},k=JSON.stringify(W);
 if(sc._rk===k)return;sc._rk=k;old.outerHTML=studioRays(sc.classList.contains('narrow'),W)}
addEventListener('resize',()=>{const sc=document.getElementById('studio');if(sc)raysFit(sc)},{passive:true});
let STSKY=0;
let STIO=null,STVIS=true;
function studioMount(){const sc=$('#studio');if(!sc){catStop();clearInterval(STSKY);STSKY=0;STIO&&STIO.disconnect();return}
 /* rest when the room is scrolled out of sight: the cat stops and every CSS animation pauses */
 if('IntersectionObserver'in window&&!sc._io){STIO&&STIO.disconnect();sc._io=1;STIO=new IntersectionObserver(es=>{STVIS=es[0].isIntersecting;sc.classList.toggle('offscreen',!STVIS);if(STVIS&&st0().play&&document.visibilityState==='visible')catStart();else catStop()});STIO.observe(sc)}
 studioSky();if(!STSKY)STSKY=setInterval(()=>{if(document.visibilityState==='visible')studioSky()},60000);

 raysFit(sc);catBox();if(st0().play&&document.visibilityState==='visible'&&STVIS){catStop();catStart()}else{catStop();catDraw(CAT.el)}}
document.addEventListener('visibilitychange',()=>{if(!$('#studio'))return;if(document.visibilityState==='visible'&&st0().play&&STVIS)catStart();else catStop()});
/* the room lives in #stuHost, outside the part of the page that re-renders (#vin). A re-render only rebuilds the
   board notes, the bench tray and the journal from a light version, so taps keep their target, the board keeps
   its scroll, the cat keeps walking, and nothing heavy is parsed or re-styled. */
viewSet=function(v,h){const want=cur.p==='today'&&studioOn(),R=document.documentElement;
 if(!want){v.innerHTML=h;R.classList.remove('stu-on');return}
 let host=document.getElementById('stuHost'),vin=document.getElementById('vin');
 if(!host||!vin||host.parentNode!==v){v.innerHTML='<div id="stuHost"></div><div id="vin"></div>';host=v.firstElementChild;vin=v.lastElementChild}
 const narrow=innerWidth<640,sc=host.firstElementChild;
 if(!sc||sc.classList.contains('narrow')!==narrow)host.innerHTML=studioHTML();
 else{const D=studioData(narrow);ST_LIGHT=1;const t=document.createElement('div');t.innerHTML=studioBoard(D)+studioBench(D);ST_LIGHT=0;
  const ns=sc.querySelector('.st-notes'),sy=ns?ns.scrollTop:0;
  ['.st-hello','.st-notes','.st-tray','.st-book'].forEach(q=>{const o=sc.querySelector(q),n=t.querySelector(q);if(o&&n&&o.innerHTML!==n.innerHTML)o.innerHTML=n.innerHTML});
  if(ns)ns.scrollTop=sy}
 vin.innerHTML=h;R.classList.add('stu-on')};
{const _r=render;render=function(a){_r(a);studioMount()}}
const _vToday=vToday;vToday=function(){let o=_vToday();const on=studioOn(),i=o.indexOf('<header class="ph">');if(i<0)return o;
 const j=o.indexOf('<div class="ph-r">',i);if(j>0)o=o.slice(0,j+18)+`<button class="ibtn${on?' on':''}" data-act="homeStyle" data-k="${on?'classic':'studio'}" aria-label="${on?'Classic home':'Studio home (live)'}" title="${on?'Classic home':'Studio home (live)'}">${ic('home')}</button>`+o.slice(j+18);
 return o};VIEWS.today=vToday;
function homePanel(){const h=S.settings.home==='studio'?'studio':'classic';return`<section class="panel wide rv"><h3>Home page</h3><p class="small muted">What Today opens with. More live homes are coming. Some studio props are Microsoft Fluent Emoji (MIT licence).</p><div class="hpick">${[['classic','Classic','Your plan as cards and lists'],['studio','Studio · live','A cosy room: your work board, a bench, a window with the real sky, and a cat']].map(([k,n,x])=>`<button class="hpc${h===k?' on':''}" data-act="homeStyle" data-k="${k}" aria-pressed="${h===k}"><span class="hpv hpv-${k}"></span><b>${n}</b><small>${x}</small></button>`).join('')}</div></section>`}
Object.assign(ACT,{
 stPlay:()=>{const p=!st0().play;S.settings.studio={...st0(),play:p};save();const sc=$('#studio');if(sc){sc.classList.toggle('paused',!p);const b=sc.querySelector('.st-play');b.innerHTML=p?'<svg viewBox="0 0 24 24"><rect x="6" y="5" width="4" height="14" rx="1"/><rect x="14" y="5" width="4" height="14" rx="1"/></svg>':'<svg viewBox="0 0 24 24"><path d="M8 5l11 7-11 7z"/></svg>';b.setAttribute('aria-label',p?'Pause the scene':'Play the scene')}if(p)catStart();else catStop()},
 stCat:(d,el)=>{[0,1,2].forEach(i=>setTimeout(()=>{const h=document.createElement('i');h.className='st-heart';h.style.setProperty('--hx',(i-1)*40+'%');h.textContent='♥';el.appendChild(h);setTimeout(()=>h.remove(),1400)},i*180));el.classList.remove('pet');void el.offsetWidth;el.classList.add('pet');setTimeout(()=>el.classList.remove('pet'),1800)},
 stJournal:()=>go('journal'),
 homeStyle:d=>{S.settings.home=d.k;save();render(false);toast(d.k==='studio'?'Studio home is on':'Classic home')}});

/* status-bar scrim: invisible while the room is at the top (the room fades into the page by itself) */
function stTop(){const on=!!document.getElementById('studio')&&scrollY<40;document.documentElement.classList.toggle('stu-attop',on)}
addEventListener('scroll',stTop,{passive:true});{const _r2=render;render=function(a){_r2(a);stTop()}}
/* ================= END STUDIO ================= */
