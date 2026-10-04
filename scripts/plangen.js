
/* ================= AI PLAN on this device =================
 Same prompt and Plotline Plan Format as the copy/paste flow, but sent to the assistant's engine
 (native llama.cpp on the Android app, wllama on the web, or your own AI server). Local engines are
 constrained by a JSON schema of the format, so a small model can only write valid JSON;
 validatePlan() still checks every field and one automatic retry carries the errors back. */
function planEngine(){const s=insSet();if(s.engine==='api'&&s.url)return'api';if(NAT_OK())return'native';if(s.engine==='local')return'wasm';return''}
function planEngineName(){const e=planEngine(),m=llmOf(insSet().llm)[1].split(' · ')[0];return e==='api'?'your AI server':e==='native'?m+' on this phone':m+' on this device'}
function planSchema(mode){const up=mode==='update',N=t=>({type:[t,'null']});
 const step={type:'object',properties:{id:up?{type:'string',maxLength:24}:{const:'new'},title:{type:'string',minLength:3,maxLength:100},dueInDays:{type:['integer','null'],minimum:0},dueTime:{type:['string','null'],pattern:'^([01][0-9]|2[0-3]):[0-5][0-9]$'},reminder:{enum:['none','at-time','15m','1h','1d','1w']},note:{type:'string',maxLength:200}},required:STEP_KEYS,additionalProperties:false};
 const goal={type:'object',properties:{id:up?{type:'string',maxLength:24}:{type:'string',pattern:'^new-[0-9]{1,2}$'},action:{enum:up?['keep','update','add','remove']:['add']},parent:{type:['string','null'],maxLength:24},title:{type:'string',minLength:3,maxLength:80},why:{type:'string',maxLength:200},area:{type:'string',minLength:1,maxLength:24},horizon:{enum:HZ_IDS},priority:{enum:[1,2,3]},startInDays:up?{type:['integer','null'],minimum:0}:{type:'integer',minimum:0},targetInDays:{type:['integer','null'],minimum:1},steps:{type:'array',items:step,maxItems:12},linksTo:{type:'array',items:{type:'string',maxLength:24},maxItems:6}},required:GOAL_KEYS,additionalProperties:false};
 return{type:'object',properties:{format:{const:WP_FMT},version:{const:1},mode:{const:mode},goals:{type:'array',items:goal,minItems:1,maxItems:12}},required:['format','version','mode','goals'],additionalProperties:false}}
let AIG={busy:false,out:'',prog:'',err:'',try:0};
function aiGenHTML(ch){const e=planEngine();if(!e)return`<p class="small muted" style="margin-top:12px">Want it written right here? Pick an on-device model in <button class="link small" data-act="askSettings">assistant settings</button>.</p>`;
 const n=(AIG.out.match(/"title"\s*:/g)||[]).length,g=(AIG.out.match(/"action"\s*:/g)||[]).length;
 return`<div class="aig ${AIG.busy?'on':''}"><div class="aig-h"><span class="ask-av">${ic('ai')}</span><div><b>${AIG.busy?'Writing your plan…':ch?'Rework my plan here':'Write the plan here'}</b><small>${esc(planEngineName())}${e!=='api'?' · nothing leaves this device':''} · <button class="link small" data-act="askSettings">change</button></small></div></div>
 ${AIG.busy?`<p class="small muted aig-p" id="aigProg">${esc(AIG.prog||(g?`${g} goal${g===1?'':'s'}, ${Math.max(0,n-g)} step${n-g===1?'':'s'} so far`:'Starting…'))}</p><div class="actions left"><button class="btn sm" data-act="aiGenStop">Stop</button></div>`
  :`${AIG.err?`<p class="small err">${esc(AIG.err)}</p>`:''}<div class="actions left"><button class="btn pri" data-act="aiGen">${ic('ai')}${ch?'Rework my plan':'Create my plan'}</button></div>`}</div>`}
function aiGenPaint(){const p=$('#aigProg');if(!p)return;const n=(AIG.out.match(/"title"\s*:/g)||[]).length,g=(AIG.out.match(/"action"\s*:/g)||[]).length;p.textContent=AIG.prog||(g?`${g} goal${g===1?'':'s'}, ${Math.max(0,n-g)} step${n-g===1?'':'s'} so far`:'Reading what you wrote…')}
Object.assign(ACT,{
 aiGen:async()=>{if(AIG.busy)return;const ch=AI.tab==='change',mode=ch?'update':'create',t=(ch?AI.change:AI.text)||'';const ta=$(ch?'#aiChange':'#aiText');const txt=(ta?ta.value:t).trim();
  if(!txt)return toast(ch?'Describe what changed first':'Write a few lines about what you want first');if(ch&&!S.goals.length)return toast('No goals yet. Start with New plan');
  const e=planEngine(),s=insSet(),m=llmOf(s.llm);if(e==='native'&&!natHave(m)&&!(s.dl||{})[m[0]])return askConfirm('Download '+esc(m[1].split(' · ')[0])+'?',`This downloads ${m[5]||m[3]} GB once, then plans and answers work offline. Use Wi-Fi.`,'Download',()=>{s.dl={...(s.dl||{}),[m[0]]:1};save();closeSheet();ACT.aiGen()},false);
  if(ch)AI.change=txt;else AI.text=txt;
  AIG={busy:true,out:'',prog:'',err:'',try:0};AI.plan=null;AI.errs=[];AI.reply='';IN.ctl=new AbortController();render(false);setTimeout(()=>$('.aig')?.scrollIntoView({behavior:reduced()?'auto':'smooth',block:'center'}),60);
  const sys={role:'system',content:'You are a life-planning coach inside the Plotline app. Reply with only the JSON plan in the Plotline Plan Format.'};
  const base=(ch?updatePrompt:createPrompt)(txt);let errs=[];
  for(let attempt=0;attempt<2;attempt++){AIG.out='';AIG.try=attempt;AIG.prog=attempt?'The first draft didn’t pass the checks. Fixing it…':'';aiGenPaint();
   const msgs=[sys,{role:'user',content:attempt?base+`\n\nYour previous plan was rejected for these reasons. Avoid them:\n${errs.map(x=>'- '+x).join('\n')}`:base}];
   const opts={schema:planSchema(mode),max_tokens:2200,temperature:.5,reading:'Reading what you wrote…'};
   const onTok=x=>{AIG.out+=x;if(AIG.prog&&!attempt)AIG.prog='';aiGenPaint()};const onP=x=>{AIG.prog=x||'';aiGenPaint()};
   try{if(e==='api')await apiChat(msgs,onTok,IN.ctl.signal,opts);else if(e==='native')await natChat(msgs,onTok,onP,null,opts);else await localChat(msgs,onTok,onP,0,null,false,opts)}
   catch(err){if(err&&err.name==='AbortError'||IN.ctl.signal.aborted){AIG.busy=false;AIG.err='Stopped.';render(false);return}AIG.busy=false;AIG.err=(err&&err.message||'Couldn’t write the plan')+'. You can still copy the prompt into any AI below.';render(false);return}
   if(IN.ctl.signal.aborted){AIG.busy=false;AIG.err='Stopped.';render(false);return}
   try{const o=extractPlan(AIG.out);errs=validatePlan(o,mode)}catch(x){errs=x.errs||[String(x.message||x)]}
   if(!errs.length)break}
  AIG.busy=false;AIG.err=errs.length?'The model’s plan still has problems (listed below). Try again, pick a bigger model, or copy the prompt into any AI.':'';AI.reply=AIG.out;render(false);ACT.parseAI()},
 aiGenStop:()=>{IN.ctl&&IN.ctl.abort()}
});
