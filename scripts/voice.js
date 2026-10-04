
/* ================= VOICE =================
 A mic in the places you write: today's goals, the journal, Ask, AI plans, and the goal / step / habit / day forms.
 On the phone it uses on-device speech recognition when the phone has it (audio stays on the phone).
 Anything that would send audio to an online speech service (the phone's regular recognizer, or the browser's)
 is used only after you allow it once. Tap the mic to talk, tap again to stop; words appear as you speak. */
const VOICE_DEF={on:true,cloud:null,lang:''};
const vset=()=>({...VOICE_DEF,...(S.settings.voice||{})});
const VOICE_SEL=['#askIn','.tg-add input','#wrEd','#aiText','#aiChange','form[data-form=saveGoal] input[name=title]','form[data-form=saveGoal] textarea[name=why]','form[data-form=saveDay] input[name=title]','form[data-form=saveStep] input[name=title]','form[data-form=saveStep] textarea[name=note]','form[data-form=saveHabit] input[name=title]','form[data-form=saveHabit] textarea[name=why]'];
let VC=null;
function vstate(){if(NATIVE&&NATIVE.voiceState){try{return JSON.parse(NATIVE.voiceState())}catch(e){}return{}}return{web:!!(window.SpeechRecognition||window.webkitSpeechRecognition)}}
function voiceAvail(){const v=vstate();return NATIVE?!!(v.onDevice||v.any):!!v.web}
function voiceSub(){const s=insSet(),v=vset(),st=vstate();const ai=s.engine==='local'?'On-device AI':s.engine==='api'?'Your AI server':'Copy prompt';return ai+' · voice '+(!v.on?'off':NATIVE?(st.onDevice?'on this phone':v.cloud?'online speech':'needs your OK'):(st.web?(v.cloud?'browser speech':'needs your OK'):'not in this browser'))}
function voiceHTML(){const v=vset(),st=vstate();
 return`<section class="panel wide rv"><h3>Assistant</h3>${insEngineHTML()}</section>
 <section class="panel rv" id="voicePanel"><h3>Voice</h3><p class="small muted">${NATIVE?(st.onDevice?'This phone turns speech into text on the device, so your voice never leaves it.':'This phone has no on-device speech recognition. Its regular speech service (usually Google’s) sends audio online to turn it into text.'):'Your browser’s speech service usually sends audio online to turn it into text.'}</p>
 <label class="sw"><span>Mic buttons<small>Talk instead of typing in the journal, Ask, goals and habits</small></span><input type="checkbox" data-voice="on" ${v.on?'checked':''}><i></i></label>
 ${(NATIVE&&!st.onDevice)||!NATIVE?`<label class="sw"><span>Allow online speech<small>${v.cloud?'Allowed':'Asked the first time you tap a mic'}</small></span><input type="checkbox" data-voice="cloud" ${v.cloud?'checked':''}><i></i></label>`:''}
 <div class="field"><label>Language</label><select data-voice="lang">${[['','Same as my phone'],['en-US','English (US)'],['en-GB','English (UK)'],['en-IN','English (India)'],['ur-PK','Urdu'],['hi-IN','Hindi'],['ar-SA','Arabic'],['es-ES','Spanish'],['fr-FR','French']].map(([k,n])=>`<option value="${k}" ${v.lang===k?'selected':''}>${n}</option>`).join('')}</select></div></section>`}
document.addEventListener('change',e=>{const t=e.target;if(!t.dataset||!t.dataset.voice)return;const k=t.dataset.voice;S.settings.voice={...vset(),[k]:t.type==='checkbox'?t.checked:t.value};save();render(false)});
/* put a mic in each place you write (once per element) */
function micDecorate(root){if(!vset().on||!voiceAvail())return;(root||document).querySelectorAll(VOICE_SEL.join(',')).forEach(el=>{if(el.dataset.mic)return;el.dataset.mic=1;const host=el.parentElement;if(!host)return;host.classList.add('has-mic');if(/flex|grid/.test(getComputedStyle(host).display))host.classList.add('mic-flow');
 const b=document.createElement('button');b.type='button';b.className='mic';b.setAttribute('aria-label','Speak instead of typing');b.innerHTML='<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="9" y="3" width="6" height="11" rx="3"/><path d="M5 11a7 7 0 0 0 14 0M12 18v3"/></svg><i></i>';
 b.addEventListener('click',ev=>{ev.preventDefault();ev.stopPropagation();VC&&VC.el===el?voiceStop():voiceStart(el,b)});if(el.nextSibling)host.insertBefore(b,el.nextSibling);else host.appendChild(b)})}
function vInsert(el,base,text){if(el.isContentEditable){el.dataset.vbase=el.dataset.vbase||'';return}const sep=base&&!/\s$/.test(base)&&text?' ':'';el.value=base+sep+text;el.dispatchEvent(new Event('input',{bubbles:true}))}
async function voiceStart(el,btn){const v=vset(),st=vstate();voiceStop(true);
 const needCloud=NATIVE?!st.onDevice:true;
 if(needCloud&&!v.cloud){const ok=await voiceConsent();if(!ok)return}
 if(NATIVE){if(!(await permAsk('mic','android.permission.RECORD_AUDIO')))return toast('Plotline needs the microphone to hear you. You can allow it in Android settings.')}
 const id='v'+uid(),base=el.isContentEditable?'':el.value;VC={el,btn,id,base,txt:''};btn.classList.add('rec');el.classList.add('listening');
 if(el.isContentEditable){el.focus();const s=getSelection();if(!s.rangeCount||!el.contains(s.anchorNode)){const r=document.createRange();r.selectNodeContents(el);r.collapse(false);s.removeAllRanges();s.addRange(r)}VC.mark=document.createElement('span');VC.mark.className='vlive';s.getRangeAt(0).insertNode(VC.mark)}
 const on=(k,t)=>{if(!VC||VC.id!==id)return;if(k==='partial'||k==='final'){VC.txt=t;if(VC.mark)VC.mark.textContent=(VC.mark.previousSibling&&!/\s$/.test(VC.mark.previousSibling.textContent||'')?' ':'')+t;else vInsert(el,base,t);if(k==='final')voiceStop(false,true)}
  else if(k==='level')btn.style.setProperty('--lv',Math.max(0,Math.min(1,(+t+2)/12)));
  else if(k==='err'){const m={perm:'Allow the microphone to use voice',no_on_device:'This phone can’t do on-device speech',7:'Didn’t catch that. Tap the mic and try again',6:'Didn’t hear anything',9:'Allow the microphone to use voice',13:'Speech isn’t available on this phone right now'}[t];voiceStop(false,!!VC.txt);if(m)toast(m)}
  else if(k==='end')voiceStop(false,true)};
 if(NATIVE){window.__voice=(i,k,t)=>on(k,t);NATIVE.voiceStart(id,v.lang||'',!st.onDevice&&!!vset().cloud);return}
 const R=window.SpeechRecognition||window.webkitSpeechRecognition;const r=new R();VC.r=r;r.lang=v.lang||navigator.language||'en-US';r.interimResults=true;r.continuous=false;
 r.onresult=e=>{let t='';for(const x of e.results)t+=x[0].transcript;on(e.results[e.results.length-1].isFinal?'final':'partial',t)};r.onerror=e=>on('err',e.error==='not-allowed'?'perm':e.error==='no-speech'?'6':String(e.error));r.onend=()=>on('end','');try{r.start()}catch(e){voiceStop()}}
function voiceStop(silent,keep){if(!VC)return;const c=VC;VC=null;c.btn.classList.remove('rec');c.el.classList.remove('listening');try{if(NATIVE)NATIVE.voiceStop();else c.r&&c.r.stop()}catch(e){}
 if(c.mark){const t=c.mark.textContent;if(keep&&t){c.mark.replaceWith(document.createTextNode(t));c.el.dispatchEvent(new Event('input',{bubbles:true}))}else c.mark.remove()}else if(!keep&&!silent&&!c.txt)vInsert(c.el,c.base,'')}
function permAsk(key,perms){if(!NATIVE||!NATIVE.askPerm)return Promise.resolve(true);if(perms.split(',').every(p=>NATIVE.hasPerm(p)))return Promise.resolve(true);return new Promise(res=>{const prev=window.__perm;window.__perm=(k,ok)=>{if(k!==key){prev&&prev(k,ok);return}window.__perm=prev;res(!!ok)};NATIVE.askPerm(key,perms)})}
function voiceConsent(){return new Promise(res=>{VCONS=res;const d=document.createElement('div');d.className='vdlg';d.innerHTML=`<div class="vdlg-b" role="dialog" aria-modal="true"><h3>Use online speech?</h3><p class="small muted">${NATIVE?'This phone can’t turn speech into text on the device. Its speech service (usually Google’s) can, but it sends what you say online to do it.':'Your browser turns speech into text with an online service, so what you say is sent to it.'} Plotline itself never stores or sends your audio.</p><div class="actions"><button class="btn ghost" data-act="vcNo">Not now</button><button class="btn pri" data-act="vcYes">Allow</button></div></div>`;document.body.appendChild(d)})}
let VCONS=null;
Object.assign(ACT,{vcYes:()=>{S.settings.voice={...vset(),cloud:true};save();document.querySelector('.vdlg')?.remove();const r=VCONS;VCONS=null;r&&r(true)},vcNo:()=>{document.querySelector('.vdlg')?.remove();const r=VCONS;VCONS=null;r&&r(false)}});
