/* ================= WRITING (MQC-style journaling) =================
   Moments are rich entries: title, formatted text (bold/italic/underline/highlight/lists), mood, optional
   photo, goal link and a private flag. They autosave as you type. Stored on the entry as html (sanitised)
   plus plain text, so older code, search and sync keep working. */
const MOODS=[['😊','Calm'],['😄','Energized'],['🙏','Grateful'],['😐','Okay'],['😔','Low'],['😟','Anxious'],['😡','Frustrated'],['😴','Tired']];
const WPROMPTS=['What is one thing you wish you could say out loud but can’t?','What drained your energy today, and what gently filled it back up?','If your mood had a weather forecast right now, what would it be?','Write a letter to yourself five years ago about what you’ve survived.','What are three tiny wins from today, even if they feel silly?','What are you carrying that isn’t yours to carry?','Who made today a little better, and did they know it?','What would make tomorrow feel lighter?','What did you avoid today, and why?','What are you proud of that nobody noticed?'];
function planPrompts(){const out=[];active().forEach(g=>{const n=nextStep(g);if(n)out.push(`What’s really in the way of “${n.title}”?`);out.push(`Why does “${g.title}” still matter to you?`)});
 S.habits.filter(h=>h.status==='active').forEach(h=>{if(h.kind==='quit')out.push(`You’re ${qDays(h)} days free of ${h.title.toLowerCase().replace(/^(quit|no|stop)\s+/,'')}. What’s helping?`);else if(hStreak(h)>2)out.push(`${h.title}: ${hStreak(h)} in a row. What made it stick this time?`)});return out}
const SKIPTAGS=new Set(['SCRIPT','STYLE','IFRAME','OBJECT','EMBED','SVG','MATH','TEMPLATE','NOSCRIPT','LINK','META','IMG','VIDEO','AUDIO','INPUT','BUTTON','SELECT','TEXTAREA','FORM']);
const KEEP={B:'b',STRONG:'b',I:'i',EM:'i',U:'u',MARK:'mark',UL:'ul',OL:'ol',LI:'li',P:'p',DIV:'p',BR:'br',H1:'p',H2:'p',H3:'p',H4:'p',BLOCKQUOTE:'p'};
/* allow-list sanitiser: keeps only simple formatting, drops every attribute */
function cleanHTML(h){const t=document.createElement('template');t.innerHTML=String(h||'');const o=document.createElement('div');
 const walk=(n,out)=>n.childNodes.forEach(c=>{if(c.nodeType===3){out.appendChild(document.createTextNode(c.nodeValue));return}if(c.nodeType!==1||SKIPTAGS.has(c.tagName.toUpperCase()))return;
  let tag=KEEP[c.tagName.toUpperCase()]||'';if(c.tagName==='SPAN'||c.tagName==='FONT'){const st=c.style||{},bg=st.backgroundColor||'';if(bg&&bg!=='transparent'&&!/rgba\(0, 0, 0, 0\)/.test(bg))tag='mark';else if(/bold|[6-9]00/.test(st.fontWeight||''))tag='b';else if(st.fontStyle==='italic')tag='i';else if(/underline/.test(st.textDecoration||st.textDecorationLine||''))tag='u'}
  if(!tag){walk(c,out);return}const el=document.createElement(tag);out.appendChild(el);if(tag!=='br')walk(c,el)});
 walk(t.content,o);return o.innerHTML.length>80000?o.innerHTML.slice(0,80000):o.innerHTML}
function htmlText(h){const d=document.createElement('div');d.innerHTML=String(h||'').replace(/<br\s*\/?>/gi,'\n').replace(/<\/(p|li|div)>/gi,'$&\n');return(d.textContent||'').replace(/\n{3,}/g,'\n\n').trim()}
const richOf=e=>e.html?cleanHTML(e.html):esc(e.text||'').replace(/\n/g,'<br>');
let WR=null,WRT=0;
function writeSheet(e,gid,date){const ed=!!e;PHOTO=ed?(e.img||null):null;WR={id:ed?e.id:null,gid:ed?e.goalId:(gid||null),date:date||(ed?ymd(new Date(e.t)):ymd())};
 const day=WR.date,sib=S.entries.filter(x=>x.type==='note'&&ymd(new Date(x.t))===day).sort((a,b)=>a.t-b.t);
 openSheet(`<div class="wr">
 <div class="wr-top"><span class="data" id="wrStat">${ed?'Saved':'New entry · starts saving as you write'}</span><div class="wr-pills">${sib.map((x,k)=>`<button class="wr-pill${e&&x.id===e.id?' on':''}" data-act="wrOpen" data-id="${x.id}">${x.mood?x.mood+' ':''}${esc(trunc(x.title||'Entry '+(k+1),18))}</button>`).join('')}<button class="wr-pill add" data-act="wrNew" data-d="${day}">${ic('plus')}New</button></div></div>
 <input class="wr-title" id="wrTitle" maxlength="90" placeholder="Title · e.g. Morning gratitude, Late-night vent" value="${esc(ed?e.title||'':'')}">
 <div class="wr-mood" role="radiogroup" aria-label="Mood">${MOODS.map(([m,n])=>`<button type="button" class="wr-m${ed&&e.mood===m?' on':''}" data-act="wrMood" data-m="${m}" aria-pressed="${ed&&e.mood===m}" title="${n}"><span>${m}</span><small>${n}</small></button>`).join('')}</div>
 <div class="wr-tb" role="toolbar" aria-label="Formatting"><button type="button" data-cmd="bold" aria-label="Bold"><b>B</b></button><button type="button" data-cmd="italic" aria-label="Italic"><i>I</i></button><button type="button" data-cmd="underline" aria-label="Underline"><u>U</u></button><button type="button" data-cmd="hilite" aria-label="Highlight"><mark>H</mark></button><button type="button" data-cmd="insertUnorderedList" aria-label="Bullet list">${ic('menu')}</button><button type="button" data-cmd="removeFormat" aria-label="Clear formatting">Tx</button><span style="flex:1"></span><button type="button" class="wr-pr" data-act="wrPrompt">${ic('ai')}Prompt</button></div>
 <div class="wr-ed" id="wrEd" contenteditable="true" role="textbox" aria-multiline="true" data-ph="Write freely. Bold it, underline it, highlight the parts that matter.">${ed?richOf(e):''}</div>
 <div class="wr-meta"><div class="field"><label>Date</label><input type="date" id="wrDate" value="${day}" max="${ymd()}"></div><div class="field"><label>Goal · optional</label><select id="wrGoal"><option value="">None</option>${treeOrder(S.goals).map(({g,d})=>`<option value="${g.id}" ${g.id===WR.gid?'selected':''}>${' '.repeat(d)}${esc(trunc(g.title,40))}</option>`).join('')}</select></div></div>
 <div class="cover-row"><label class="btn sm">${ic('camera')}<span id="phLbl">${PHOTO?'Change photo':'Add a photo'}</span><input type="file" accept="image/*" data-file="photo" hidden></label>${PHOTO?`<button type="button" class="btn ghost danger sm" data-act="rmPhoto">Remove</button>`:''}<label class="sw wr-priv"><span>${ic('lock','ico-s')} Private</span><input type="checkbox" id="wrPriv" ${ed&&e.private?'checked':''}><i></i></label></div><img id="phPrev" class="ph-prev" alt="" ${PHOTO?`src="${PHOTO}"`:'hidden'}>
 <div class="actions">${ed?`<button type="button" class="btn ghost danger" data-act="wrDel">${ic('trash')}Delete</button>`:''}<span style="flex:1"></span><button type="button" class="btn pri" data-act="wrDone">Done</button></div></div>`);
 $('#sheet').classList.add('wsheet');if(!ed)setTimeout(()=>$('#wrEd')?.focus(),380)}
function wrSoon(){clearTimeout(WRT);const s=$('#wrStat');if(s)s.textContent='Saving…';WRT=setTimeout(wrSave,650)}
function wrSave(){clearTimeout(WRT);if(!WR)return;const ed=$('#wrEd');if(!ed)return;const html=cleanHTML(ed.innerHTML),text=htmlText(html),title=($('#wrTitle')?.value||'').trim(),mood=$('.wr-m.on')?.dataset.m||'',gid=$('#wrGoal')?.value||null,priv=!!$('#wrPriv')?.checked,dv=validDate($('#wrDate')?.value)||ymd();
 let e=WR.id&&S.entries.find(x=>x.id===WR.id);if(!e&&!text&&!title&&!PHOTO){const s=$('#wrStat');if(s)s.textContent='New entry · starts saving as you write';return}
 const when=prev=>{const[y,m,d]=dv.split('-').map(Number),b=new Date(prev||Date.now());return new Date(y,m-1,d,b.getHours(),b.getMinutes()).getTime()};
 if(!e){e={id:uid(),t:when(),type:'note',goalId:gid,title,text,html,mood,private:priv,sid:null,img:PHOTO||null};S.entries.push(e);WR.id=e.id}
 else Object.assign(e,{title,text,html,mood,private:priv,goalId:gid,img:PHOTO||null,t:ymd(new Date(e.t))===dv?e.t:when(e.t)});
 save();const s=$('#wrStat');if(s)s.textContent='Saved '+new Date().toLocaleTimeString([],{hour:'numeric',minute:'2-digit'})}
function wrClose(){wrSave();WR=null;$('#sheet').classList.remove('wsheet');closeSheet();render(false)}
Object.assign(ACT,{
 addEntry:d=>writeSheet(null,d&&d.g),editEntry:d=>{const e=S.entries.find(x=>x.id===d.id);if(e)writeSheet(e)},
 wrDone:()=>wrClose(),
 wrOpen:d=>{wrSave();const e=S.entries.find(x=>x.id===d.id);if(e)writeSheet(e)},
 wrNew:d=>{wrSave();writeSheet(null,null,d.d)},
 wrMood:(d,el)=>{const on=!el.classList.contains('on');document.querySelectorAll('.wr-m').forEach(b=>{b.classList.remove('on');b.setAttribute('aria-pressed','false')});if(on){el.classList.add('on');el.setAttribute('aria-pressed','true')}vib();wrSoon()},
 wrPrompt:()=>{const ed=$('#wrEd');if(!ed)return;const P=[...planPrompts(),...WPROMPTS],p=P[Math.floor(Math.random()*P.length)];const html=`<p><b>${esc(p)}</b></p><p><br></p>`;if(!ed.innerText.trim())ed.innerHTML=html;else ed.insertAdjacentHTML('beforeend',html);ed.focus();const r=document.createRange();r.selectNodeContents(ed);r.collapse(false);const sl=getSelection();sl.removeAllRanges();sl.addRange(r);wrSoon()},
 wrDel:()=>{const id=WR&&WR.id;if(!id){wrClose();return}askConfirm('Delete this entry?','It will be removed from your journal.','Delete',()=>{const u=undoPoint();S.entries=S.entries.filter(x=>x.id!==id);WR=null;$('#sheet').classList.remove('wsheet');save();render(false);toast('Entry deleted',u)})},
});
/* toolbar keeps the text selection (pointerdown is cancelled so the editor never loses focus) */
document.addEventListener('pointerdown',e=>{const b=e.target.closest&&e.target.closest('.wr-tb [data-cmd]');if(!b)return;e.preventDefault();const c=b.dataset.cmd;$('#wrEd')?.focus();
 try{if(c==='hilite'){const cur=String(document.queryCommandValue('hiliteColor')||'');const on=cur&&!/transparent|rgba\(0, 0, 0, 0\)|^$/.test(cur)&&!/255, 255, 255/.test(cur);document.execCommand('styleWithCSS',false,true);document.execCommand('hiliteColor',false,on?'transparent':'#FFE36E');document.execCommand('styleWithCSS',false,false)}else document.execCommand(c,false,null)}catch(_){}wrTb();wrSoon()});
function wrTb(){document.querySelectorAll('.wr-tb [data-cmd]').forEach(b=>{let on=false;try{on=['bold','italic','underline','insertUnorderedList'].includes(b.dataset.cmd)&&document.queryCommandState(b.dataset.cmd)}catch(_){}b.classList.toggle('on',on)})}
document.addEventListener('selectionchange',()=>{if(WR&&document.activeElement&&document.activeElement.id==='wrEd')wrTb()});
document.addEventListener('input',e=>{if(WR&&e.target.closest&&e.target.closest('#wrEd,#wrTitle'))wrSoon()});
document.addEventListener('change',e=>{if(WR&&e.target.closest&&e.target.closest('#wrDate,#wrGoal,#wrPriv'))wrSoon()});
document.addEventListener('paste',e=>{if(!WR||!e.target.closest||!e.target.closest('#wrEd'))return;const h=e.clipboardData&&e.clipboardData.getData('text/html');if(h){e.preventDefault();document.execCommand('insertHTML',false,cleanHTML(h))}});
/* mood strip on the Journal page: the last 14 days */
function moodStrip(){const tn=dnum(ymd()),M={};S.entries.forEach(e=>{if(e.type==='note'&&e.mood){const k=dnum(ymd(new Date(e.t)));if(k>tn-14&&(!M[k]||M[k].t<e.t))M[k]=e}});if(!Object.keys(M).length)return'';
 const cnt={};Object.values(M).forEach(e=>cnt[e.mood]=(cnt[e.mood]||0)+1);const top=Object.entries(cnt).sort((a,b)=>b[1]-a[1])[0];const nm=(MOODS.find(m=>m[0]===top[0])||[])[1]||'';
 return`<section class="mstrip rv"><div class="ms-h"><span class="data">Mood · last 14 days</span><span class="data">mostly ${top[0]} ${esc(nm.toLowerCase())}</span></div><div class="ms-row">${[...Array(14)].map((_,i)=>{const k=tn-13+i,e=M[k];return`<button class="ms-d${e?' on':''}${k===tn?' now':''}" ${e?`data-act="viewEntry" data-id="${e.id}"`:`data-act="wrNew" data-d="${fromN(k)}"`} aria-label="${dUTC(k).toLocaleDateString(undefined,{month:'short',day:'numeric',timeZone:'UTC'})}${e?': '+e.mood:''}"><span>${e?e.mood:''}</span><em>${DAYS[dUTC(k).getUTCDay()][0]}</em></button>`}).join('')}</div></section>`}
/* one-time import of an MQC (My Quiet Space) journal.json export */
FILE.mqc=inp=>{const f=inp.files[0];if(!f)return;const r=new FileReader();r.onload=()=>{try{const o=JSON.parse(r.result),days=(o&&o.days)||[];let n=0,skip=0;const have=new Set(S.entries.map(e=>e.id));
  days.forEach(d=>(d.entries||[]).forEach(x=>{const id='mqc-'+String(x.id||uid());if(have.has(id)){skip++;return}const html=cleanHTML(x.html||''),t=Date.parse(x.createdAt||'')||Date.parse((d.date||'')+'T12:00:00')||Date.now();
   S.entries.push({id,t,type:'note',goalId:null,title:String(x.title||'').slice(0,90),text:htmlText(html),html,mood:MOODS.some(m=>m[0]===x.mood)?x.mood:(x.mood||''),private:false,sid:null,img:null});n++}));
  if(!n&&!skip)throw 0;save();render(false);toast(n?`Imported ${n} entr${n===1?'y':'ies'} from MQC${skip?` · ${skip} already here`:''}`:'Everything from that file is already here')}catch(e){toast('That file isn’t an MQC journal export')}};r.readAsText(f);inp.value=''};
