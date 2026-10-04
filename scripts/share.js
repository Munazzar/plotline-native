
/* ================= SHARING (end-to-end encrypted; only shared items leave your Drive) =================
 Share one goal, habit or step with someone: do it together, compete, or keep each other accountable.
 In plain terms (also shown to the person before anything is shared):
  - Until you share, everything stays only in your own Google Drive.
  - Sharing an item puts an ENCRYPTED copy of just that item in Plotline's sharing space (Google Firebase).
    Each shared item has its own secret key. It is locked on your device before it leaves, and only the
    people you invited can unlock it: not Plotline's developer, not Google, not anyone with the database.
  - Each person has a key pair. The private half is kept in their own Drive (synced with their plan); only the
    public half is published, so others can lock an item's key for them.
  - What the service can see: the email addresses in a share and when something changed. Never what it is.
  - Progress (check-ins, streak, percent) and cheers sync live while the app is open; on Android a background
    check posts invites even when the app is closed.
  - Stopping a share deletes it for everyone; leaving removes your progress.
 S.shares (synced) remembers your circles and their keys; S.ident (synced) is your key pair; settings.share is per device. */
const SHARE_DEF={on:true};
const shset=()=>{const s={...SHARE_DEF,...(S.settings.share||{})};if(s.v!==2){s.on=true;s.v=2}return s};
const shPatch=o=>{S.settings.share={...shset(),...o};save()};
const shares=()=>S.shares||(S.shares=[]);
const MODES={together:{n:'Do it together',e:'🤝',x:'One shared goal. Everyone’s progress adds up.'},compete:{n:'Friendly competition',e:'🏁',x:'Same goal, a leaderboard for the week.'},watch:{n:'Accountability',e:'👀',x:'They see your progress and can cheer you on.'}};
let SHC={t:0,busy:false,kc:{}};
const me=()=>lc(S.settings.sync.email||'');
const myName=()=>S.settings.name||(S.settings.sync.email||'').split('@')[0];
const fbc=()=>{const c=PLOTLINE_CFG.firebase||{};return{key:(c.apiKey||'').trim(),pid:(c.projectId||'').trim()}};
const fsOK=()=>!!(fbc().key&&fbc().pid&&window.crypto&&crypto.subtle);
const shOn=()=>fsOK()&&syncOn()&&shset().on;

/* ---- end-to-end encryption (WebCrypto: ECDH P-256 + AES-GCM; same scheme in ShareCheck.java) ---- */
const b64=u=>{let s='';const a=u instanceof Uint8Array?u:new Uint8Array(u);for(let i=0;i<a.length;i++)s+=String.fromCharCode(a[i]);return btoa(s)};
const unb64=s=>Uint8Array.from(atob(s),c=>c.charCodeAt(0));
const TE=new TextEncoder(),TD=new TextDecoder(),EC={name:'ECDH',namedCurve:'P-256'};
const fpOf=async pub=>[...new Uint8Array(await crypto.subtle.digest('SHA-256',unb64(pub)))].slice(0,8).map(x=>x.toString(16).padStart(2,'0')).join('');
async function ensureIdent(){if(S.ident&&S.ident.pub&&S.ident.priv)return S.ident;const kp=await crypto.subtle.generateKey(EC,true,['deriveBits']);
 const pub=b64(await crypto.subtle.exportKey('raw',kp.publicKey)),priv=b64(await crypto.subtle.exportKey('pkcs8',kp.privateKey));S.ident={pub,priv,fp:await fpOf(pub),c:Date.now()};DIRTY=true;save();return S.ident}
let PRIVK=null;async function privKey(){const id=S.ident;if(PRIVK&&PRIVK.fp===id.fp)return PRIVK.k;const k=await crypto.subtle.importKey('pkcs8',unb64(id.priv),EC,false,['deriveBits']);PRIVK={fp:id.fp,k};return k}
async function pairKey(theirPub){const pub=await crypto.subtle.importKey('raw',unb64(theirPub),EC,false,[]);const bits=new Uint8Array(await crypto.subtle.deriveBits({name:'ECDH',public:pub},await privKey(),256));
 const pre=TE.encode('plotline-wrap-v1'),m=new Uint8Array(pre.length+32);m.set(pre);m.set(bits,pre.length);return crypto.subtle.importKey('raw',await crypto.subtle.digest('SHA-256',m),'AES-GCM',false,['encrypt','decrypt'])}
async function seal(key,bytes){const iv=crypto.getRandomValues(new Uint8Array(12));const ct=new Uint8Array(await crypto.subtle.encrypt({name:'AES-GCM',iv},key,bytes));const o=new Uint8Array(12+ct.length);o.set(iv);o.set(ct,12);return b64(o)}
async function unseal(key,s){const a=unb64(s);return new Uint8Array(await crypto.subtle.decrypt({name:'AES-GCM',iv:a.slice(0,12)},key,a.slice(12)))}
const ckey=k=>crypto.subtle.importKey('raw',unb64(k),'AES-GCM',false,['encrypt','decrypt']);
const encJ=async(k,o)=>seal(await ckey(k),TE.encode(JSON.stringify(o)));
const decJ=async(k,s)=>JSON.parse(TD.decode(await unseal(await ckey(k),s)));
const wrapFor=async(k,theirPub)=>seal(await pairKey(theirPub),unb64(k));
const unwrapK=async(w,ownerPub)=>b64(await unseal(await pairKey(ownerPub),w));
const newKey=()=>b64(crypto.getRandomValues(new Uint8Array(32)));

/* ---- Firebase sign-in with the Google account you already use for sync ---- */
const FBK='plotline.fb';
const fbGet=()=>{try{return JSON.parse(localStorage.getItem(FBK)||'null')}catch(e){return null}};
function fbPut(o){try{o?localStorage.setItem(FBK,JSON.stringify(o)):localStorage.removeItem(FBK)}catch(e){}fbNative()}
const fme=()=>lc((fbGet()||{}).email||me());
async function fbSignIn(gtok){const r=await fetch(`https://identitytoolkit.googleapis.com/v1/accounts:signInWithIdp?key=${fbc().key}`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({postBody:`access_token=${gtok}&providerId=google.com`,requestUri:'http://localhost',returnSecureToken:true,returnIdpCredential:false})});
 const j=await r.json().catch(()=>({}));if(!r.ok||!j.idToken)throw new Error('Sign-in for sharing failed'+(j.error&&j.error.message?': '+j.error.message:' ('+r.status+')'));
 if(!j.email)throw{noEmail:true};
 const s={id:j.idToken,exp:Date.now()+(+j.expiresIn||3600)*1e3,rt:j.refreshToken,email:lc(j.email),uid:j.localId};fbPut(s);return s}
async function fbRefresh(s){let r;try{r=await fetch(`https://securetoken.googleapis.com/v1/token?key=${fbc().key}`,{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:'grant_type=refresh_token&refresh_token='+encodeURIComponent(s.rt)})}catch(e){throw new Error('You’re offline')}
 const j=await r.json().catch(()=>({}));if(!r.ok||!j.id_token){if(r.status===400){fbPut(null);return fbFromGoogle(false)}throw new Error('Sharing sign-in '+r.status)}
 const n={...s,id:j.id_token,rt:j.refresh_token||s.rt,exp:Date.now()+(+j.expires_in||3600)*1e3};fbPut(n);return n}
/* a Google token that includes your email address → a sharing session. If Google hasn’t been told it may share
   your email yet (people who signed in before 1.8.2), you’re asked once, in the app. */
async function fbFromGoogle(interactive){for(let i=0;i<2;i++){let t;try{t=await token(interactive)}catch(e){throw{fbAuth:true}}
  try{const s=await fbSignIn(t);if(shset().allow||shset().lite)shPatch({allow:false,lite:false});return s}
  catch(e){const miss=e&&(e.noEmail||/INVALID_IDP/.test(String(e.message)));if(!miss)throw e;if(i===0&&!interactive&&NATIVE&&S.settings.sync.native&&!shset().lite){tokDrop();continue}shPatch({allow:true});render(false);throw{fbAllow:true}}}}
let FBP=null;
async function fbSession(){let s=fbGet();if(s&&s.email&&me()&&s.email!==me()){fbPut(null);s=null}
 if(s&&s.exp>Date.now()+60e3)return s;
 if(FBP)return FBP;FBP=(s&&s.rt?fbRefresh(s):fbFromGoogle(false)).finally(()=>{FBP=null});return FBP}
/* the Android background check: finds new invites, and (for items you share) locks the item key for people who
   install Plotline after you invited them, so their invite unlocks without you opening the app */
function fbNative(){try{if(!NATIVE||!NATIVE.shareCfg)return;const s=fbGet()||{},c=fbc(),id=S.ident||{};
 NATIVE.shareCfg(JSON.stringify({on:!!(shOn()&&s.rt),key:c.key,pid:c.pid,rt:s.rt||'',email:s.email||'',seen:shares().map(x=>x.id),priv:id.priv||'',owned:shares().filter(x=>x.fs&&x.role==='owner'&&x.status==='joined'&&x.k).map(x=>({cid:x.id,k:x.k}))}))}catch(e){}}
async function shAllow(){shPatch({lite:false});scopeSync();tokDrop();
 try{const t=NATIVE&&S.settings.sync.native?await nativeAuth(true):await(NATIVE?browserAuthFromApp():webAuth('consent'));await fbSignIn(t);shPatch({allow:false});render(false);toast('Done. Invites will show up here');await shRefresh(true)}
 catch(e){toast(e&&e.noEmail?'Google didn’t confirm your email, so invites can’t reach you yet':shErr(e))}render(false)}

/* ---- Firestore over REST ---- */
const FSB=()=>`https://firestore.googleapis.com/v1/projects/${fbc().pid}/databases/(default)/documents`;
async function fs(path,opt={},retry=true){const s=await fbSession();let r;try{r=await fetch(FSB()+path,{...opt,headers:{'Content-Type':'application/json',...(opt.headers||{}),Authorization:'Bearer '+s.id}})}catch(e){throw new Error('You’re offline')}
 if(r.status===401&&retry){const x=fbGet();if(x)fbPut({...x,exp:0});return fs(path,opt,false)}
 if(r.status===404&&opt.ok404)return null;
 if(!r.ok){let m='';try{const j=await r.json();m=j.error&&(j.error.status||j.error.message)||''}catch(e){}throw new Error(`Sharing ${r.status}${m?': '+m:''}`)}
 const t=await r.text();return t?JSON.parse(t):null}
const fsv=v=>v==null?{nullValue:null}:typeof v==='string'?{stringValue:v}:typeof v==='number'?{integerValue:String(Math.round(v))}:typeof v==='boolean'?{booleanValue:v}:Array.isArray(v)?{arrayValue:{values:v.map(fsv)}}:{stringValue:JSON.stringify(v)};
const fsx=v=>!v?null:'stringValue' in v?v.stringValue:'integerValue' in v?+v.integerValue:'booleanValue' in v?v.booleanValue:'arrayValue' in v?(v.arrayValue.values||[]).map(fsx):'doubleValue' in v?v.doubleValue:null;
const fsEnc=o=>({fields:Object.fromEntries(Object.entries(o).map(([k,v])=>[k,fsv(v)]))});
const fsDec=d=>{const o={_id:decodeURIComponent(d.name.split('/').pop())};for(const k in d.fields||{})o[k]=fsx(d.fields[k]);return o};
const jp=(s,d)=>{try{return s?JSON.parse(s):d}catch(e){return d}};
const circDec=d=>{const c=fsDec(d);c.cid=c._id;c.keys=jp(c.locks,{});c.members=(c.members||[]).map(lc);c.owner=lc(c.owner||'');return c};
const fsCircle=async cid=>{const d=await fs(`/circles/${cid}`,{ok404:true});return d&&circDec(d)};
async function fsMine(){const j=await fs(':runQuery',{method:'POST',body:JSON.stringify({structuredQuery:{from:[{collectionId:'circles'}],where:{fieldFilter:{field:{fieldPath:'members'},op:'ARRAY_CONTAINS',value:{stringValue:fme()}}},limit:200}})});return(j||[]).filter(x=>x.document).map(x=>circDec(x.document))}
async function fsMembers(sh){const j=await fs(`/circles/${sh.id}/ms?pageSize=100`);const out=[];
 for(const d of j&&j.documents||[]){const m=fsDec(d);m.email=lc(m._id);let p={};if(m.enc&&sh.k){try{p=await decJ(sh.k,m.enc)}catch(e){}}m.name=p.name||m.email.split('@')[0];m.progress=p.progress||{};m.cheers=p.cheers||[];out.push(m)}return out}
async function fsSetMe(sh,o){const enc=sh.k&&o.status==='joined'?await encJ(sh.k,{name:myName().slice(0,80),progress:o.progress||{},cheers:o.cheers||[]}):'';
 return fs(`/circles/${sh.id}/ms/${encodeURIComponent(fme())}`,{method:'PATCH',body:JSON.stringify(fsEnc({status:o.status,enc,u:Date.now()}))})}
/* public keys: keys/{email} = {pub, fp}. Only the public half; the private half never leaves your Drive. */
async function publishKey(){const id=await ensureIdent();if(shset().pubfp===id.fp&&shset().pubme===fme())return;
 await fs(`/keys/${encodeURIComponent(fme())}`,{method:'PATCH',body:JSON.stringify(fsEnc({pub:id.pub,fp:id.fp,u:Date.now()}))});shPatch({pubfp:id.fp,pubme:fme()})}
async function keyOf(email,fresh){const c=SHC.kc[email];if(c&&!fresh&&Date.now()-c.t<10*60000)return c.k;const d=await fs(`/keys/${encodeURIComponent(email)}`,{ok404:true});const k=d?fsDec(d):null;SHC.kc[email]={t:Date.now(),k};return k}
/* lock the item key for every member who has a public key and doesn’t have it yet (or got a new key pair) */
async function shWrapMissing(c,sh,fresh){const keys={...c.keys};let ch=0;
 for(const m of c.members){if(m===fme())continue;const pk=await keyOf(m,fresh).catch(()=>null);if(!pk||!pk.pub)continue;if(keys[m]&&keys[m].fp===pk.fp)continue;keys[m]={fp:pk.fp,w:await wrapFor(sh.k,pk.pub)};ch++}
 return ch?keys:null}

/* ---- what my progress looks like for a shared item (only this) ---- */
function shItem(sh){const L=sh.local||{};if(L.kind==='habit')return H(L.id);if(L.kind==='goal')return G(L.id);if(L.kind==='step'){const g=G(L.g);return g&&g.steps.find(s=>s.id===L.id)}return null}
function shProgress(sh){const o=shItem(sh),L=sh.local||{},tn=dnum(ymd()),out={u:Date.now()};if(!o)return out;
 if(L.kind==='habit'){const ch={};for(let k=tn-59;k<=tn;k++){const ds=fromN(k);if(hDone(o,ds))ch[ds]=1}out.checks=ch;out.streak=o.kind==='quit'?qDays(o):hStreak(o);out.rate=hRate(o)}
 else if(L.kind==='goal'){out.pct=pct(o);out.done=o.steps.filter(s=>s.done).map(s=>s.sid||s.id);out.total=o.steps.length}
 else if(L.kind==='step'){out.pct=o.done?100:0;out.done=o.done?[L.id]:[]}
 return out}
const pubKey=p=>JSON.stringify([p.checks,p.streak,p.pct,p.done]);
const weekChecks=p=>{if(!p||!p.checks)return 0;const tn=dnum(ymd());let n=0;for(let k=tn-6;k<=tn;k++)if(p.checks[fromN(k)])n++;return n};

/* ---- settings page ---- */
function shareSub(){if(!fsOK())return'Not available in this version';if(!syncOn())return'Needs Google sync first';const s=shset();if(!s.on)return'Off';if(s.allow)return'One tap needed to get invites';const n=shares().filter(x=>x.status==='joined').length,inv=shares().filter(x=>x.status==='invited').length;return(n?`${n} shared item${n===1?'':'s'}`:'On')+(inv?` · ${inv} invite${inv===1?'':'s'} waiting`:'')}
function shAllowCard(){return`<div class="sh-what">${ic('lock')}<div><b>Get invites from people you know</b><small>Google needs to confirm your email address to Plotline once, so invites sent to it reach you. Plotline sees nothing else.</small><div class="actions left" style="margin-top:8px"><button class="btn sm pri" data-act="shAllow">Allow</button>${shset().allowHide?'':`<button class="btn sm ghost" data-act="shAllowHide">Not now</button>`}</div></div></div>`}
function shareHTML(){shLegacy();const s=shset();
 if(!fsOK())return`<section class="panel rv"><h3>Sharing</h3><p class="small muted">Sharing isn’t switched on in this version of Plotline yet.</p></section>`;
 if(!syncOn())return`<section class="panel rv"><h3>Sharing</h3><p class="small muted">Sharing uses your Google account, so connect sync first.</p><div class="actions left"><a class="btn pri" href="#/settings/account">Set up sync</a></div></section>`;
 const L=shares();const inv=L.filter(x=>x.status==='invited'||x.status==='later'),on=L.filter(x=>x.status==='joined'),old=L.filter(x=>x.status==='legacy');
 return`<section class="panel rv"><h3>Sharing</h3><p class="small muted">Everything in Plotline stays in your own Google Drive. When you share one goal, habit or step, only that item goes to Plotline’s sharing space, end-to-end encrypted: only the people in it can read it.</p>
 <label class="sw"><span>Sharing and invites<small>${s.on?'Invites sent to '+esc(fme()||'your email')+' show up here':'Off · you won’t get invites'}</small></span><input type="checkbox" data-share="on" ${s.on?'checked':''}><i></i></label>
 ${s.on&&s.allow?shAllowCard():''}
 <div class="actions left">${s.on?`<button class="btn sm" data-act="shRefresh">Check for invites</button>`:''}<button class="btn sm ghost" data-act="shHow">How it works</button></div></section>
 ${inv.length?`<section class="panel rv"><h3>Invites</h3>${inv.map(shInviteRow).join('')}</section>`:''}
 ${on.length?`<section class="panel rv"><h3>Shared with others</h3><div class="chl">${on.map(x=>`<button class="chl-r" style="--cc:var(--accent)" data-act="shOpen" data-id="${x.id}"><span style="font-size:18px">${MODES[x.mode]?.e||'🤝'}</span><span><b>${esc(x.title)}</b><small>${MODES[x.mode]?.n||''} · ${x.role==='owner'?'you shared it':'from '+esc(x.fromName||x.from||'')}</small></span>${ic('next','ico-s')}</button>`).join('')}</div></section>`:''}
 ${old.length?`<section class="panel rv"><h3>Shared before the update</h3><p class="small muted">Sharing works a new, simpler way now: invites just appear for the other person, end-to-end encrypted. Share these again to move them over.</p>${old.map(x=>`<div class="sh-inv"><span class="sh-ava">${MODES[x.mode]?.e||'🤝'}</span><div><b>${esc(x.title)}</b><small>${MODES[x.mode]?.n||''}</small></div><div class="sh-inv-a"><button class="btn sm pri" data-act="shRedo" data-id="${x.id}">Share again</button><button class="btn sm ghost" data-act="shDrop" data-id="${x.id}">Remove</button></div></div>`).join('')}</section>`:''}`}
const shWho=x=>x.fromName||(x.from||'').split('@')[0]||'Someone';
function shInviteRow(x){return`<div class="sh-inv"><span class="sh-ava">${esc(shWho(x)[0].toUpperCase())}</span><div><b>${esc(shWho(x))} invited you</b><small>${x.locked?`🔒 ${esc(x.from||'')} · unlocking…`:`${MODES[x.mode]?.e||''} ${MODES[x.mode]?.n||'Shared'} · “${esc(x.title)}”`}</small></div><div class="sh-inv-a"><button class="btn sm pri" data-act="shAccept" data-id="${x.id}">View</button>${x.status==='later'?'':`<button class="btn sm ghost" data-act="shLater" data-id="${x.id}">Later</button>`}</div></div>`}
const SH_FACTS=(item)=>`<ol class="sh-how"><li><b>Until you share, it’s only yours.</b> Your goals, habits, journal and moods live only in your own Google Drive.</li><li><b>Sharing sends one item${item?`, “${esc(item)}”`:''}.</b> An encrypted copy of just that item goes to Plotline’s sharing space (Google Firebase): its title, steps and mode, and each person’s name, check-in days, streak, percent and cheers.</li><li><b>Only the people in it can read it.</b> It’s locked on your device with a key only they get. Plotline’s developer, Google and Firebase can’t read it.</li><li><b>What the service does see:</b> the email addresses of the people in a share and when it changed. Never what it’s about.</li><li><b>It stays in step.</b> Your progress on this item syncs to them within seconds while Plotline is open, and theirs to you.</li><li><b>You can stop any time.</b> Stopping deletes it for everyone. Leaving removes your progress.</li></ol>`;
const SH_DET=()=>`<details class="sh-asg"><summary class="small">How it works</summary>${SH_FACTS('')}</details>`;
const SH_HOW=`<h2>How sharing works</h2>${SH_FACTS('')}<p class="small muted">To get invites, Google confirms your email to Plotline’s sharing service. You can turn invites off here any time.</p><div class="actions"><button class="btn pri" data-act="close">Got it</button></div>`;
function shareOn(v){shPatch({on:!!v});scopeSync();fbNative();render(false);if(v){toast('Sharing is on');shRefresh(true)}}
document.addEventListener('change',e=>{const t=e.target;if(t.dataset&&t.dataset.share==='on')shareOn(t.checked)});

/* ---- share an item ---- */
async function shReady(){if(!fsOK()){toast('Sharing isn’t available in this version yet');return false}
 if(!syncOn()){askConfirm('Connect Google first','Sharing uses your Google account, so turn on sync first.','Set up sync',()=>{closeSheet();go('settings/account')},false);return false}
 if(!shset().on){shPatch({on:true});scopeSync()}
 try{await fbSession();await publishKey();return true}catch(e){
  if(e&&e.fbAllow){askConfirm('One quick permission','Google needs to confirm your email address to Plotline, so the people you invite know it’s you. Plotline sees nothing else.','Allow',()=>{closeSheet();shAllow()},false);return false}
  if(e&&e.fbAuth){toast('Sign in again to share');ACT.reauth&&ACT.reauth();return false}
  toast(shErr(e));return false}}
let SHN=null;
async function shareStart(kind,ref){const o=kind==='habit'?H(ref.id):kind==='goal'?G(ref.id):(G(ref.g)||{steps:[]}).steps.find(s=>s.id===ref.id);if(!o)return;
 if(!(await shReady()))return;SHN={kind,ref,mode:'together'};
 if(!shset().told)return openSheet(`<div class="data">Before you share</div><h2 style="margin-top:6px">What happens when you share</h2>${SH_FACTS(o.title)}<div class="actions"><button class="btn ghost" data-act="close">Cancel</button><button class="btn pri" data-act="shTold">I understand, continue</button></div>`);
 openSheet(shareForm(o))}
function shareForm(o){const K=SHN.kind,g=K==='goal'?o:null;return`<div class="data">Share</div><h2 style="margin-top:6px">${esc(o.title)}</h2><form data-form="shareGo">
 <div class="field"><label>How</label><div class="sh-modes">${Object.entries(MODES).filter(([k])=>K!=='step'||k!=='compete').map(([k,v])=>`<label class="sh-m"><input type="radio" name="mode" value="${k}" ${SHN.mode===k?'checked':''}><span><em>${v.e}</em><b>${v.n}</b><small>${v.x}</small></span></label>`).join('')}</div></div>
 <div class="field"><label>With · their Google email</label><input name="emails" type="email" multiple required placeholder="sara@gmail.com" autocomplete="off"><small class="muted small">The email they use in Plotline. Add more than one with commas.</small></div>
 ${g&&g.steps.length?`<details class="sh-asg"><summary class="small">Assign steps · optional</summary><p class="small muted">Assigned steps show up for that person to do. Leave a step unassigned to do it yourself.</p>${g.steps.filter(s=>!s.done).map(s=>`<div class="sh-st"><span class="ell">${esc(s.title)}</span><input name="as_${s.id}" type="email" placeholder="me" autocomplete="off"></div>`).join('')}</details>`:''}
 <div class="sh-what">${ic('lock')}<div><b>End-to-end encrypted</b><small>They’ll see the title${g?', the steps':''}, the mode, and your name, progress, streak and check-in days for this one item, live. Nothing else from your plan. Only the people in it can read it.</small></div></div>${SH_DET()}
 <div class="actions"><button type="button" class="btn ghost" data-act="close">Cancel</button><button class="btn pri">Share</button></div></form>`}
const emailsOf=v=>[...new Set(String(v||'').split(/[,\s;]+/).map(lc).filter(x=>/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(x)&&x!==fme()))].slice(0,20);
Object.assign(FORM,{shareGo:async f=>{const fd=new FormData(f),mode=fd.get('mode')||'together',emails=emailsOf(fd.get('emails'));
 if(!emails.length)return toast('Add the Google email of who you’re sharing with');const K=SHN.kind,ref=SHN.ref;const o=K==='habit'?H(ref.id):K==='goal'?G(ref.id):(G(ref.g)||{steps:[]}).steps.find(s=>s.id===ref.id);if(!o)return;
 const btn=f.querySelector('.btn.pri');btn.disabled=true;btn.textContent='Sharing…';
 try{const id=await ensureIdent(),cid='c'+uid()+uid(),k=newKey(),item={kind:K,title:o.title};if(K==='habit'){item.icon=hIcon(o);item.unit=o.unit||'';item.target=hTarget(o);item.hkind=o.kind;item.freq=o.freq;item.days=o.days;item.times=o.times}
  if(K==='step'){item.sid=ref.id;item.due=o.due||''}
  if(K==='goal'){item.steps=o.steps.map(s=>({id:s.id,title:s.title,due:s.due||'',to:lc(fd.get('as_'+s.id)||'')}));item.why=o.why||'';item.area=o.area}
  const sh={id:cid,fs:1,k,role:'owner',mode,title:o.title,status:'joined',local:K==='step'?{kind:'step',g:ref.g,id:ref.id}:{kind:K,id:ref.id},u:Date.now()};
  const members=[fme(),...emails],keys=await shWrapMissing({members,keys:{}},sh,true)||{};
  await fs(`/circles?documentId=${cid}`,{method:'POST',body:JSON.stringify(fsEnc({owner:fme(),members,opub:id.pub,locks:JSON.stringify(keys),enc:await encJ(k,{title:o.title,mode,item,ownerName:myName().slice(0,80)}),created:Date.now(),u:Date.now()}))});
  shares().push(sh);save();await shPublish(sh).catch(()=>{});closeSheet();render(false);shSent(sh,emails,emails.filter(e=>!keys[e]))}
 catch(e){btn.disabled=false;btn.textContent='Share';toast(shErr(e))}}});
function shErr(e){if(e&&(e.auth||e.fbAuth))return'Sign in again to share';if(e&&e.fbAllow)return'Allow Plotline to confirm your email first (Settings → Sharing)';const m=String(e&&e.message||e);if(/403|PERMISSION_DENIED/.test(m))return'You don’t have access to that shared item';if(/404|NOT_FOUND/.test(m))return'That shared item isn’t available any more';return esc(m)}
function shSent(sh,emails,later){const who=emails.length===1?emails[0].split('@')[0]:emails.length+' people';
 openSheet(`<div class="data">Shared</div><h2 style="margin-top:6px">Invite sent to ${esc(who)}</h2><p class="muted">“${esc(sh.title)}” shows up in their Plotline, with a notification on their Android phone. Nothing else to do.</p>
 ${later.length?`<div class="sh-what">${ic('link')}<div><b>${esc(later.length===1?later[0]:later.length+' people')} ${later.length===1?'isn’t':'aren’t'} on Plotline yet</b><small>Send them the app. Once they sign in, your Plotline unlocks the invite for them automatically (usually within the hour).</small></div></div>`:''}
 <div class="actions">${later.length?`<button class="btn ghost" data-act="shSendNote" data-id="${sh.id}">Send them Plotline</button>`:''}<button class="btn pri" data-act="close">Done</button></div>`)}

/* ---- refresh: find and unlock invites, lock keys for new members, publish my progress ---- */
function shLegacy(){let n=0;shares().forEach(x=>{if(x.fs||x.status==='ended'||x.status==='declined'||x.status==='left'||x.status==='legacy')return;x.status=x.role==='owner'&&x.status==='joined'&&x.local?'legacy':'ended';x.u=Date.now();n++});if(n)save()}
async function shUnlock(c,x){let k=x&&x.k;if(!k){const w=c.keys[fme()];if(!w||!S.ident||w.fp!==S.ident.fp||!c.opub)return null;try{k=await unwrapK(w.w,c.opub)}catch(e){return null}}
 try{return{k,d:await decJ(k,c.enc)}}catch(e){return null}}
async function shRefresh(manual){if(!shOn()||SHC.busy||TOURING)return;SHC.busy=true;let news=0;shLegacy();
 try{await publishKey();const L=await fsMine(),seen=new Set(L.map(c=>c.cid)),known=new Map(shares().map(x=>[x.id,x]));
  for(const c of L){const x=known.get(c.cid);
   if(c.owner!==fme()){if(x&&['declined','left','ended'].includes(x.status))continue;const u=await shUnlock(c,x);
    if(!x){shares().push({id:c.cid,fs:1,role:'member',status:'invited',from:c.owner,fromName:u?u.d.ownerName:'',title:u?u.d.title:'',mode:u?u.d.mode:'',k:u?u.k:'',locked:!u,u:Date.now()});news++}
    else if(u&&(x.locked||!x.k||x.title!==u.d.title)){Object.assign(x,{k:u.k,locked:false,title:u.d.title,mode:u.d.mode,fromName:u.d.ownerName,u:Date.now()});news++}}
   else if(x&&x.k&&x.status==='joined'){const keys=await shWrapMissing(c,x,Date.now()-(x.kt||0)>30*60000).catch(()=>null);x.kt=Date.now();
    if(keys)await fs(`/circles/${c.cid}?updateMask.fieldPaths=locks&updateMask.fieldPaths=u`,{method:'PATCH',body:JSON.stringify(fsEnc({locks:JSON.stringify(keys),u:Date.now()}))}).catch(()=>{})}}
  shares().forEach(x=>{if(x.fs&&['joined','invited','later'].includes(x.status)&&!seen.has(x.id)){x.status='ended';x.u=Date.now();news++}});
  for(const sh of shares().filter(x=>x.fs&&x.status==='joined'))await shPublish(sh).catch(()=>{});
  if(news){save();render(false);const inv=shares().filter(x=>x.status==='invited'),y=inv[inv.length-1];if(y)toast(y.locked?`${esc(shWho(y))} invited you · unlocking…`:`${esc(shWho(y))} invited you to “${esc(y.title)}”`)}else if(manual)toast('No new invites')}
 catch(e){if(manual&&!(e&&e.fbAllow))toast(shErr(e))}finally{SHC.busy=false;SHC.t=Date.now();fbNative()}}
async function shPublish(sh){if(!sh.fs||!sh.k)return;const p=shProgress(sh),key=pubKey(p);if(sh.pub===key&&!sh.cheerOut)return;
 const out=sh.cheerOut||[],cheers=[...(sh.myCheers||[]),...out].slice(-40);await fsSetMe(sh,{status:'joined',progress:p,cheers});
 sh.myCheers=cheers;sh.pub=key;if(out.length){sh.cheerOut=null;sh.u=Date.now()}save()}
async function shGroup(sh){const c=await fsCircle(sh.id);if(!c)throw new Error('404');const d=await decJ(sh.k,c.enc);const all=await fsMembers(sh);const ms=all.filter(m=>m.status==='joined').map(m=>({...m,mine:m.email===fme()}));return{c,d,ms,all}}
/* live: invites every minute while open; my progress goes out seconds after a check-in; an open group view refreshes itself */
function shPoll(){if(shOn()&&Date.now()-SHC.t>55000)shRefresh(false)}
document.addEventListener('visibilitychange',()=>{if(document.visibilityState==='visible')setTimeout(shPoll,800)});
setInterval(()=>{if(document.visibilityState==='visible')shPoll()},60000);
setInterval(()=>{if(document.visibilityState!=='visible'||!shOn()||SHC.busy)return;for(const sh of shares())if(sh.fs&&sh.k&&sh.status==='joined'&&pubKey(shProgress(sh))!==sh.pub)shPublish(sh).catch(()=>{})},5000);

/* ---- accept / later / decline ---- */
const SH_LOCKED=x=>`<div class="data">Invite</div><h2 style="margin-top:6px">${esc(shWho(x))} invited you</h2><p class="muted">It’s end-to-end encrypted, so it unlocks once ${esc(x.from||'their')} Plotline is online and hands your copy of the key over (usually within the hour, nothing for you to do). You’ll see what it is before you join.</p><div class="actions"><button class="btn danger ghost" data-act="shDecline" data-id="${x.id}">Decline</button><button class="btn pri" data-act="close">OK</button></div>`;
async function shAcceptView(sh){if(sh.locked)return openSheet(SH_LOCKED(sh));openSheet(`<div class="data">Invite</div><h2 style="margin-top:6px">${esc(sh.title)}</h2><p class="muted small">Loading…</p>`);let c,d;
 try{c=await fsCircle(sh.id);if(!c)throw new Error('404');d=await decJ(sh.k,c.enc)}catch(e){if(/404/.test(String(e&&e.message))){sh.status='ended';sh.u=Date.now();save();render(false)}openSheet(`<h2>Invite not available</h2><p class="muted">${shErr(e)}</p><div class="actions"><button class="btn" data-act="close">Close</button></div>`);return}
 const md=MODES[d.mode]||MODES.together,it=d.item||{},mine=(it.steps||[]).filter(s=>s.to===fme());
 openSheet(`<div class="data">${md.e} ${md.n}</div><h2 style="margin-top:6px">${esc(d.title)}</h2><p class="muted">${esc(d.ownerName||c.owner)} (${esc(c.owner)}) invited you. ${md.x}</p>
 ${it.kind==='goal'&&it.steps&&it.steps.length?`<div class="sh-steps">${it.steps.map(s=>`<div class="${s.to===fme()?'me':''}"><i></i><span>${esc(s.title)}</span>${s.to?`<em>${s.to===fme()?'you':esc(s.to.split('@')[0])}</em>`:''}</div>`).join('')}</div>`:''}
 <div class="sh-what">${ic('lock')}<div><b>If you join</b><small>Plotline adds “${esc(d.title)}” to your plan${mine.length?` with the ${mine.length} step${mine.length===1?'':'s'} assigned to you`:''}. Your name, progress, streak and check-in days for this one item sync live to the people in it, end-to-end encrypted. Nothing else from your plan is shared, and you can leave any time.</small></div></div>${SH_DET()}
 <div class="actions"><button class="btn danger ghost" data-act="shDecline" data-id="${sh.id}">Decline</button><button class="btn ghost" data-act="shLater" data-id="${sh.id}">Later</button><button class="btn pri" data-act="shJoin" data-id="${sh.id}">Join</button></div>`)}
async function shJoin(sh){const btn=document.querySelector('[data-act=shJoin]');if(btn){btn.disabled=true;btn.textContent='Joining…'}
 try{const c=await fsCircle(sh.id);if(!c)throw new Error('404');const d=await decJ(sh.k,c.enc),it=d.item||{};let local;
  if(it.kind==='habit'){const h=normHabit({kind:it.hkind||'build',title:it.title,icon:it.icon||'',target:it.target||1,unit:it.unit||'',freq:it.freq||'daily',days:it.days||[0,1,2,3,4,5,6],times:it.times||3,id:uid(),startDate:ymd(),createdAt:Date.now()});S.habits.push(h);local={kind:'habit',id:h.id}}
  else{const picked=it.kind==='step'?[{id:it.sid||'',title:it.title,due:it.due||''}]:(it.steps||[]).filter(s=>!s.to||s.to===fme());
   const g=newGoal({title:it.title,why:it.why||'',area:it.area||'personal',horizon:'month',steps:picked.map(s=>({title:s.title,due:s.due||'',remind:'0'}))});g.steps.forEach((st,i)=>{st.sid=picked[i].id});S.goals.push(g);local={kind:'goal',id:g.id}}
  sh.local=local;sh.mode=d.mode;sh.title=d.title;sh.status='joined';sh.u=Date.now();sh.pub='';
  await shPublish(sh);save();closeSheet();render(false);toast(`Joined “${esc(d.title)}”`)}
 catch(e){if(btn){btn.disabled=false;btn.textContent='Join'}toast(shErr(e))}}

/* ---- the group view (refreshes itself every few seconds while open) ---- */
let SHLIVE=null;
async function shOpen(sh){openSheet(`<div class="data">${MODES[sh.mode]?.e||''} ${MODES[sh.mode]?.n||''}</div><h2 style="margin-top:6px">${esc(sh.title)}</h2><p class="small muted">Loading the group…</p>`);
 let G2;try{await shPublish(sh);G2=await shGroup(sh)}catch(e){if(/404/.test(String(e&&e.message))){sh.status='ended';sh.u=Date.now();save();render(false)}openSheet(`<h2>${esc(sh.title)}</h2><p class="muted">${shErr(e)}</p><div class="actions"><button class="btn" data-act="close">Close</button></div>`);return}
 openSheet(`<div class="sh-g" data-shg="${sh.id}">${shGroupHTML(sh,G2)}</div>`);
 clearInterval(SHLIVE);let last=JSON.stringify(G2.all);
 SHLIVE=setInterval(async()=>{const el=document.querySelector(`#sheet.on [data-shg="${sh.id}"]`);if(!el){clearInterval(SHLIVE);return}if(document.visibilityState!=='visible')return;
  try{await shPublish(sh);const g=await shGroup(sh),j=JSON.stringify(g.all);if(j!==last){last=j;const e2=document.querySelector(`#sheet.on [data-shg="${sh.id}"]`);if(e2)e2.innerHTML=shGroupHTML(sh,g)}}catch(e){}},6000)}
function shGroupHTML(sh,{c,d,ms,all}){const it=d.item||{},kind=it.kind,score=m=>kind==='habit'?weekChecks(m.progress):(m.progress&&m.progress.pct)||0;
 const rows=ms.slice().sort((a,b)=>score(b)-score(a)),tot=kind==='habit'?rows.reduce((s,m)=>s+weekChecks(m.progress),0):Math.round(rows.reduce((s,m)=>s+((m.progress&&m.progress.pct)||0),0)/Math.max(1,rows.length));
 const cheersForMe=ms.flatMap(m=>(m.cheers||[]).filter(x=>x.to===fme()).map(x=>({...x,from:m.name}))).sort((a,b)=>b.t-a.t).slice(0,3);
 const st=e=>(all.find(m=>m.email===e)||{}).status;const pending=c.members.filter(e=>!ms.some(m=>m.email===e));
 return`<div class="data">${MODES[d.mode]?.e||''} ${MODES[d.mode]?.n||''} · <span title="End-to-end encrypted">🔒 live</span></div><h2 style="margin-top:6px">${esc(d.title)}</h2>
 ${d.mode==='together'?`<div class="sh-tot"><b>${tot}${kind==='habit'?'':'%'}</b><span>${kind==='habit'?'check-ins together this week':'done together'}</span></div>`:''}
 <div class="sh-ms">${rows.map((m,i)=>`<div class="sh-mr${m.mine?' me':''}"><span class="sh-ava">${d.mode==='compete'?['🥇','🥈','🥉'][i]||i+1:esc((m.name||m.email||'?')[0].toUpperCase())}</span><div><b>${m.mine?'You':esc(m.name||m.email)}</b><small>${kind==='habit'?`${weekChecks(m.progress)} this week · ${(m.progress&&m.progress.streak)||0} day streak`:`${(m.progress&&m.progress.pct)||0}% done`}${m.progress&&m.progress.u?' · '+esc(dayLabel(m.progress.u)):''}</small>${kind==='habit'?`<div class="sh-wk">${[...Array(7)].map((_,k)=>{const ds=fromN(dnum(ymd())-6+k);return`<i class="${m.progress&&m.progress.checks&&m.progress.checks[ds]?'on':''}"></i>`}).join('')}</div>`:`<div class="pbar"><i style="width:${(m.progress&&m.progress.pct)||0}%"></i></div>`}</div>${m.mine?'':`<button class="ibtn sm sh-ch" data-act="shCheer" data-id="${sh.id}" data-to="${esc(m.email)}" aria-label="Cheer ${esc(m.name||'')}">👏</button>`}</div>`).join('')}
 ${pending.map(e=>`<div class="sh-mr pend"><span class="sh-ava">${esc(e[0].toUpperCase())}</span><div><b>${esc(e)}</b><small>${st(e)==='declined'?'Declined':st(e)==='left'?'Left':c.keys[e]||c.owner===e?'Invited · hasn’t joined yet':'Invited · not on Plotline yet'}</small></div></div>`).join('')}</div>
 ${cheersForMe.length?`<p class="small" style="margin-top:12px">${cheersForMe.map(x=>`${esc(x.emoji)} ${esc(x.from)} cheered you ${esc(dayLabel(x.t).toLowerCase())}`).join('<br>')}</p>`:''}
 ${kind==='goal'&&it.steps?`<details class="sh-asg"><summary class="small">Steps and who’s on them</summary><div class="sh-steps">${it.steps.map(s=>{const by=ms.find(m=>m.progress&&(m.progress.done||[]).includes(s.id));return`<div class="${by?'d':''} ${s.to===fme()?'me':''}"><i></i><span>${esc(s.title)}</span><em>${by?'✓ '+esc(by.mine?'you':by.name):s.to?esc(s.to===fme()?'you':s.to.split('@')[0]):'anyone'}</em></div>`}).join('')}</div></details>`:''}
 <div class="actions">${sh.role==='owner'?`<button class="btn danger ghost" data-act="shStop" data-id="${sh.id}">Stop sharing</button><button class="btn ghost" data-act="shInvite" data-id="${sh.id}">Invite more</button>`:`<button class="btn danger ghost" data-act="shLeave" data-id="${sh.id}">Leave</button>`}<button class="btn pri" data-act="close">Done</button></div>`}
const shById=id=>shares().find(x=>x.id===id);
function shareChip(kind,id){const L=shares().filter(x=>x.status==='joined'&&x.local&&x.local.kind===kind&&x.local.id===id);if(!L.length)return'';const x=L[0];return`<button class="sh-chip" data-act="shOpen" data-id="${x.id}" aria-label="Shared: ${esc(MODES[x.mode]?.n||'')}">${MODES[x.mode]?.e||'🤝'}<span>Shared</span></button>`}
function shTodayCard(){if(!shOn())return'';const s=shset(),inv=shares().filter(x=>x.status==='invited');let o='';if(inv.length)o+=inv.slice(0,2).map(shInviteRow).join('');if(s.allow&&!s.allowHide)o+=shAllowCard();return o?`<section class="sh-today rv">${o}</section>`:''}
/* opening an invite from its notification (or an old link) */
function shJoinRoute(id){setTimeout(async()=>{if(!shOn())return go('settings/share');SHC.t=0;await shRefresh(false);const x=shById(id);if(x&&['invited','later'].includes(x.status))return shAcceptView(x);if(x&&x.status==='joined')return shOpen(x);
 openSheet(`<div class="data">Invite</div><h2 style="margin-top:6px">Invite not found</h2><p class="muted">It may have been stopped, or it’s from an older version of Plotline. Ask them to share it again: it will show up here by itself.</p><div class="actions"><button class="btn pri" data-act="close">OK</button></div>`)},600)}
Object.assign(ACT,{
 shHow:()=>openSheet(SH_HOW),shRefresh:()=>shRefresh(true),shAllow:()=>shAllow(),shAllowHide:()=>{shPatch({allowHide:true});render(false);toast('You can allow it later in Settings → Sharing')},
 shTold:()=>{shPatch({told:true});const K=SHN&&SHN.kind,ref=SHN&&SHN.ref;if(!K)return closeSheet();const o=K==='habit'?H(ref.id):K==='goal'?G(ref.id):(G(ref.g)||{steps:[]}).steps.find(s=>s.id===ref.id);if(o)openSheet(shareForm(o))},
 shSendNote:async()=>{const msg=`I’m using Plotline for my goals and habits and shared one with you. Get it here and sign in with your Google account: ${(cfg().webUrl||PLOTLINE_CFG.webUrl)}`;closeSheet();if(NATIVE){try{NATIVE.share('Plotline',msg);return}catch(e){}}try{if(navigator.share){await navigator.share({text:msg});return}}catch(e){}if(await copyText(msg))toast('Copied. Paste it in a message')},
 shareGoal:d=>shareStart('goal',{id:d.id}),shareHabit:d=>shareStart('habit',{id:d.id}),shareStep:d=>shareStart('step',{g:d.g,id:d.s}),
 shAccept:d=>{const x=shById(d.id);if(x)shAcceptView(x)},
 shLater:d=>{const x=shById(d.id);if(!x)return;x.status='later';x.u=Date.now();save();closeSheet();render(false);toast('It waits under Settings → Sharing')},
 shDecline:d=>{const x=shById(d.id);if(!x)return;fsSetMe(x,{status:'declined'}).catch(()=>{});x.status='declined';x.u=Date.now();save();closeSheet();render(false);toast('Invite declined')},
 shJoin:d=>{const x=shById(d.id);if(x)shJoin(x)},
 shOpen:d=>{const x=shById(d.id);if(x)shOpen(x)},
 shCheer:d=>{const x=shById(d.id);if(!x)return;x.cheerOut=[...(x.cheerOut||[]),{to:d.to,emoji:'👏',t:Date.now()}];shPublish(x).then(()=>toast('Cheer sent 👏')).catch(e=>toast(shErr(e)))},
 shLeave:d=>{const x=shById(d.id);if(!x)return;askConfirm('Leave this shared item?','Your progress is removed from it and the group stops seeing it. Your own copy in Plotline stays.','Leave',async()=>{closeSheet();try{await fsSetMe(x,{status:'left'})}catch(e){}x.status='left';x.k='';x.u=Date.now();save();render(false);toast('You left')})},
 shStop:d=>{const x=shById(d.id);if(!x)return;askConfirm('Stop sharing?','The shared item is deleted, so everyone stops seeing it. Your goal stays in your plan.','Stop sharing',async()=>{closeSheet();try{const j=await fs(`/circles/${x.id}/ms?pageSize=100`);for(const dd of j&&j.documents||[])await fs(`/circles/${x.id}/ms/${encodeURIComponent(fsDec(dd)._id)}`,{method:'DELETE',ok404:true});await fs(`/circles/${x.id}`,{method:'DELETE',ok404:true})}catch(e){return toast(shErr(e))}x.status='ended';x.k='';x.u=Date.now();save();render(false);toast('Sharing stopped')})},
 shInvite:d=>{const x=shById(d.id);if(!x)return;openSheet(`<h2>Invite more people</h2><form data-form="shMore" data-id="${x.id}"><div class="field"><label>Their Google email</label><input name="emails" type="email" multiple required placeholder="name@gmail.com"></div><div class="actions"><button type="button" class="btn ghost" data-act="close">Cancel</button><button class="btn pri">Invite</button></div></form>`)},
 shRedo:d=>{const x=shById(d.id);if(!x||!x.local)return;const L=x.local;x.status='ended';x.u=Date.now();save();render(false);shareStart(L.kind,L.kind==='step'?{g:L.g,id:L.id}:{id:L.id})},
 shDrop:d=>{const x=shById(d.id);if(!x)return;x.status='ended';x.u=Date.now();save();render(false)}});
Object.assign(FORM,{shMore:async f=>{const x=shById(f.dataset.id);if(!x||!x.k)return;const emails=emailsOf(new FormData(f).get('emails'));if(!emails.length)return;
 try{const c=await fsCircle(x.id);if(!c)throw new Error('404');const members=[...new Set([...c.members,...emails])].slice(0,21);const keys=await shWrapMissing({...c,members},x,true)||c.keys;
  await fs(`/circles/${x.id}?updateMask.fieldPaths=members&updateMask.fieldPaths=locks&updateMask.fieldPaths=u`,{method:'PATCH',body:JSON.stringify(fsEnc({members,locks:JSON.stringify(keys),u:Date.now()}))});closeSheet();shSent(x,emails,emails.filter(e=>!keys[e]))}catch(e){toast(shErr(e))}}});
