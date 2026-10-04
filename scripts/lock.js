
/* ================= APP LOCK + FIRST-RUN SETUP =================
 Off by default. On the phone it unlocks with fingerprint / face, with the phone's own screen lock as the fallback;
 on the web with the device's built-in fingerprint / face (WebAuthn) when there is one, or the Plotline PIN.
 It locks when the app opens and again after it has been in the background for the chosen time.
 This is a screen lock for privacy on a shared device; data on the device is not encrypted by it. */
const LOCK_DEF={on:false,after:0,secure:false,web:''};
const lockSet=()=>({...LOCK_DEF,...(S.settings.lock||{})});
let BIO=null,LOCKED=false,HID_AT=0;
function bioInfo(){if(!NATIVE||!NATIVE.bioState)return{bio:false,any:false};try{return JSON.parse(NATIVE.bioState())}catch(e){return{bio:false,any:false}}}
const webBioOK=async()=>{try{return!!(window.PublicKeyCredential&&await PublicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable())}catch(e){return false}};
const lockActive=()=>!!(S.settings.pinHash||lockSet().on);
function lockSub(){const l=lockSet();if(!l.on&&!S.settings.pinHash)return'Off · anyone with this device can open Plotline';return(NATIVE?(bioInfo().bio?'Fingerprint / face':'Phone screen lock'):(l.web?'Device fingerprint':'PIN'))+(S.settings.pinHash?' + PIN':'')+' · locks '+(l.after?`after ${l.after} min away`:'every time you leave')}
function lockHTML(){const l=lockSet(),bi=bioInfo();
 const can=NATIVE?bi.any:true;
 return`<section class="panel rv" id="lockPanel"><h3>App lock</h3><p class="small muted">${NATIVE?(bi.bio?'Unlock Plotline with your fingerprint or face. Your phone’s screen lock works as the backup.':bi.any?'Unlock Plotline with your phone’s screen lock (PIN, pattern or password).':'Set up a screen lock or fingerprint in your phone’s settings first, then turn this on.'):'Unlock Plotline with this device’s fingerprint or face if it has one, otherwise with your Plotline PIN.'} It hides your plan from anyone who picks up your device.</p>
 <label class="sw"><span>Lock Plotline<small>${l.on?'On':'Off'}</small></span><input type="checkbox" data-lock="on" ${l.on?'checked':''} ${can?'':'disabled'}><i></i></label>
 ${l.on?`<div class="field"><label>Lock again after</label>${chipsRow2('after',[[0,'Right away'],[1,'1 min'],[5,'5 min'],[15,'15 min'],[60,'1 hour']],l.after)}</div>
 ${NATIVE?`<label class="sw"><span>Hide in recent apps<small>Blank preview in the app switcher, and no screenshots</small></span><input type="checkbox" data-lock="secure" ${l.secure?'checked':''}><i></i></label>`:''}
 <div class="actions left"><button class="btn sm" data-act="lockTest">${ic('lock')}Try it now</button></div>`:''}</section>`}
const chipsRow2=(f,opts,cur)=>`<div class="chips rm-ch">${opts.map(([v,l])=>`<button type="button" class="chip${String(cur)===String(v)?' on':''}" data-act="lockSetv" data-f="${f}" data-v="${v}">${l}</button>`).join('')}</div>`;
async function lockApply(f,v){const l=lockSet();
 if(f==='on'&&v){/* prove it works before turning it on */
  const ok=await bioCheck('Turn on app lock','Confirm it’s you');if(!ok){toast('App lock not turned on');render(false);return}
  if(!NATIVE&&!l.web&&!S.settings.pinHash){toast('Set a PIN first, it unlocks Plotline on this browser');render(false);return ACT.setPin()}}
 S.settings.lock={...l,[f]:v};save();if(f==='secure'&&NATIVE&&NATIVE.setSecure)NATIVE.setSecure(!!v);if(f==='on'&&!v&&NATIVE&&NATIVE.setSecure)NATIVE.setSecure(false);render(false);if(f==='on')toast(v?'App lock on':'App lock off')}
document.addEventListener('change',e=>{const t=e.target;if(!t.dataset||!t.dataset.lock)return;lockApply(t.dataset.lock,t.type==='checkbox'?t.checked:t.value)});
/* one prompt: native biometric / screen lock, or web platform authenticator; resolves true when it's you */
function bioCheck(title,sub){
 if(NATIVE&&NATIVE.bioAuth){const bi=bioInfo();if(!bi.any)return Promise.resolve(false);return new Promise(res=>{BIO=res;window.__bio=(k,m)=>{const r=BIO;BIO=null;if(r)r(k==='ok');if(k!=='ok'&&m&&!/cancel/i.test(m))toast(esc(m))};try{NATIVE.bioAuth(title,sub||'')}catch(e){BIO=null;res(false)}})}
 return webBio()}
async function webBio(){if(!await webBioOK())return!!S.settings.pinHash;const l=lockSet();
 try{if(!l.web){const id=crypto.getRandomValues(new Uint8Array(16));const c=await navigator.credentials.create({publicKey:{challenge:crypto.getRandomValues(new Uint8Array(32)),rp:{name:'Plotline'},user:{id,name:S.settings.name||'Plotline',displayName:S.settings.name||'Plotline'},pubKeyCredParams:[{type:'public-key',alg:-7},{type:'public-key',alg:-257}],authenticatorSelection:{authenticatorAttachment:'platform',userVerification:'required',residentKey:'discouraged'},timeout:60000}});if(!c)return false;S.settings.lock={...lockSet(),web:btoa(String.fromCharCode(...new Uint8Array(c.rawId)))};save();return true}
  const raw=Uint8Array.from(atob(l.web),c=>c.charCodeAt(0));const a=await navigator.credentials.get({publicKey:{challenge:crypto.getRandomValues(new Uint8Array(32)),allowCredentials:[{type:'public-key',id:raw}],userVerification:'required',timeout:60000}});return!!a}catch(e){return false}}
/* the lock screen: fingerprint first when available, PIN as the alternative */
function showLock(){if(document.querySelector('.lock'))return;LOCKED=true;const l=lockSet(),pin=!!S.settings.pinHash,bio=l.on;
 const d=document.createElement('div');d.className='lock';d.innerHTML=`<form class="box" data-form="unlock">${LOGO}<h2>Welcome back</h2>${bio?`<p class="muted small">${NATIVE?'Unlock with your fingerprint, face or screen lock':'Unlock to open Plotline'}</p><button type="button" class="btn pri" style="width:100%;margin-top:6px" data-act="lockBio">${ic('lock')}Unlock</button>`:''}${pin?`${bio?'<p class="small muted" style="margin-top:16px">or enter your PIN</p>':'<p class="muted small">Enter your PIN to open Plotline</p>'}<input class="pin-in" name="p" type="password" inputmode="numeric" autocomplete="off" aria-label="PIN"><p class="err" id="pinErr"></p><button class="btn ${bio?'':'pri'}" style="width:100%;margin-top:10px">Unlock with PIN</button><div style="margin-top:16px"><button type="button" class="link" data-act="forgotPin">Forgot PIN?</button></div>`:''}</form>`;
 document.body.appendChild(d);if(bio)setTimeout(()=>ACT.lockBio(),250);else setTimeout(()=>d.querySelector('input')?.focus(),100)}
function unlockDone(){LOCKED=false;const l=document.querySelector('.lock');if(!l)return;l.style.transition='opacity .45s var(--ease)';l.style.opacity=0;setTimeout(()=>l.remove(),450);if(!S.settings.tourOffered)setTimeout(tourPrompt,1100);try{applyWidgetQueue()}catch(e){}}
document.addEventListener('visibilitychange',()=>{if(!lockActive()||TOURING)return;if(document.hidden){HID_AT=Date.now();return}if(!LOCKED&&HID_AT&&Date.now()-HID_AT>=lockSet().after*60000)showLock()});
Object.assign(ACT,{
 lockSetv:d=>lockApply(d.f,d.f==='after'?+d.v:d.v),
 lockBio:async()=>{if(await bioCheck('Unlock Plotline',''))unlockDone()},
 lockTest:async()=>{toast(await bioCheck('Test app lock','')?'That works':'Not unlocked')}});

/* ---- first-run setup: the permissions and choices that make Plotline work well, each one skippable ---- */
function setupItems(){const L=[];const nGranted=NATIVE?NATIVE.notificationsGranted():('Notification'in window&&Notification.permission==='granted');
 L.push({k:'notif',t:'Reminders',x:'Get reminders for habits, steps and check-ins, and reply to them right from the notification.',done:nGranted,btn:'Allow notifications'});
 if(NATIVE&&NATIVE.exactAlarms)L.push({k:'exact',t:'On-time reminders',x:'Let Android deliver reminders at the exact minute instead of a few minutes late.',done:NATIVE.exactAlarms(),btn:'Allow'});
 L.push({k:'lock',t:'App lock',x:NATIVE?'Open Plotline with your fingerprint or face. Off unless you turn it on.':'Lock Plotline on this browser with your fingerprint or a PIN. Off unless you turn it on.',done:lockActive(),btn:'Turn on',opt:1});
 L.push({k:'sync',t:'Sync',x:'Keep your phone and the web in step through your own Google Drive.',done:syncOn(),btn:'Connect',opt:1});
 return L}
function setupHTML(){const L=setupItems();return`<div class="data">Set up Plotline</div><h2 style="margin-top:8px">A few choices, once</h2><p class="muted small">Each one is optional and you can change it any time in Settings.</p><div class="setup">${L.map(x=>`<div class="su-r${x.done?' done':''}"><span class="su-i">${x.done?ic('check'):ic({notif:'bell',exact:'timer',lock:'lock',sync:'link'}[x.k])}</span><div><b>${x.t}${x.opt&&!x.done?' <em>optional</em>':''}</b><small>${x.x}</small></div>${x.done?'<span class="data">On</span>':`<button class="btn sm${x.opt?'':' pri'}" data-act="setupDo" data-k="${x.k}">${x.btn}</button>`}</div>`).join('')}</div><div class="actions"><button class="btn pri" data-act="setupDone">Done</button></div>`}
let SETUP_NEXT=null;
function setupFlow(next){SETUP_NEXT=next||null;openSheet(setupHTML(),true)}
function setupPaint(){const sh=$('#sheet');if(sh&&sh.querySelector('.setup'))sh.innerHTML='<div class="grab"></div>'+setupHTML()}
Object.assign(ACT,{
 setupDo:async d=>{if(d.k==='notif'){askNotif();setTimeout(setupPaint,1600)}else if(d.k==='exact'){try{NATIVE.openExactAlarms()}catch(e){}window.addEventListener('focus',setupPaint,{once:true});setTimeout(setupPaint,2500)}
  else if(d.k==='lock'){const ok=await bioCheck('Turn on app lock','Confirm it’s you');if(ok){S.settings.lock={...lockSet(),on:true};save();toast('App lock on')}else if(!NATIVE){S.settings.setupDone=true;SHEET_LOCK=false;closeSheet();go('settings/privacy');return}setupPaint()}
  else if(d.k==='sync'){S.settings.setupDone=true;SHEET_LOCK=false;closeSheet();save();go('settings/account')}},
 setupDone:()=>{S.settings.setupDone=true;save();SHEET_LOCK=false;closeSheet();const n=SETUP_NEXT;SETUP_NEXT=null;if(n)setTimeout(n,500)},
 setupOpen:()=>setupFlow()});
