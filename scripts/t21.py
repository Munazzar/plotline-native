# Insights (RAG): keyword + smart retrieval, stats, copy prompt, API engine streaming, citations, save to journal.
import asyncio,json,sys
from playwright.async_api import async_playwright
SP='/home/claude/scripts'
RAG={'llms':[['tiny','Tiny test · 0.5 MB','http://localhost:8767/models/tiny.gguf']],'nctx':8192,'lib':'/nm/@huggingface/transformers/dist/transformers.min.js','localModels':'/models/','dtype':'fp32','wasm':'/nm/onnxruntime-web/dist/'}
SEED="""(()=>{const run=S.goals.find(g=>/10K/.test(g.title));const day=864e5;const M=['😄','😊','🙏','😔','😴','😟'];
 const hb=normHabit({kind:'build',title:'Morning walk',goalId:run.id,startDate:addDays(-40)});hb.id='hbw';S.habits.push(hb);
 for(let i=1;i<=30;i++){const d=addDays(-i);const walked=i%3!==0;if(walked)hb.log[d]=1;
  S.entries.push({id:'je'+i,t:Date.now()-i*day,type:'note',goalId:i%4?null:run.id,title:walked?'Good morning':'Rough day',text:walked?'Walked before work and felt clear headed and strong all day. Legs are getting fitter.':'Skipped the walk, stayed on my phone, felt sluggish and snapped at the kids.',html:'',mood:walked?M[i%3]:M[3+i%3],private:false,sid:null,img:null})}
 S.entries.push({id:'jpriv',t:Date.now()-2*day,type:'note',goalId:null,title:'Secret',text:'PRIVATEWORD salary worries',html:'',mood:'😟',private:true,sid:null,img:null});save();return S.entries.length})()"""
async def main():
  async with async_playwright() as p:
    b=await p.chromium.launch();ctx=await b.new_context(viewport={'width':390,'height':844})
    await ctx.add_init_script('window.PLOTLINE_RAG='+json.dumps(RAG))
    pg=await ctx.new_page();errs=[];pg.on('pageerror',lambda e:errs.append(str(e)));pg.on('console',lambda m:m.type=='error' and errs.append(m.text))
    await pg.goto('http://localhost:8767/index.html');await pg.wait_for_timeout(600)
    await pg.fill('input[name=name]','Munazzar');await pg.click('button[value=demo]');await pg.wait_for_timeout(700);await pg.evaluate("S.settings.tourOffered=true;document.querySelector('.tprompt')?.remove()")
    print('entries',await pg.evaluate(SEED));await pg.evaluate("insSet().engine='copy'")
    await pg.evaluate("go('ask')");await pg.wait_for_timeout(900)
    ok=lambda c,m:print(('PASS ' if c else 'FAIL ')+m)
    ok(await pg.evaluate("cur.p==='ask'&&!!document.querySelector('.ask')"),'route #/insights opens the Ask assistant')
    st=await pg.evaluate("insStats(30,'').map(s=>s.k+':'+s.v)");print(st);ok(any(s.startswith('Mood link') for s in st),'mood × habit link computed')
    await pg.evaluate("document.querySelectorAll('.rv').forEach(e=>e.classList.add('in'))");await pg.screenshot(path=SP+'/x-ins-390.png',full_page=True)
    # keyword + copy engine
    await pg.evaluate('ACT.askNew()');await pg.wait_for_timeout(200)
    await pg.fill('#askIn','running shoes fitting');await pg.click('[data-act=askSend]');await pg.wait_for_function("IN.res&&!IN.busy")
    r=await pg.evaluate("({mode:IN.res.mode,labels:IN.res.srcs.slice(0,3).map(d=>d.label),priv:IN.res.prompt.includes('PRIVATEWORD'),hasCite:IN.res.prompt.includes('[1] ')})");print(r)
    ov=await pg.evaluate("IN.res.prompt");ab=ov.split('\nABOUT ME\n')[1].split('QUESTION')[0];ok('10K' in ab and 'Moods, last 30 days' not in ab,'about-me carries the related goal, not unrelated moods')
    ok(r['mode']=='keyword' and any('10K' in l for l in r['labels']) and not r['priv'] and r['hasCite'],'keyword retrieval finds the run goal, private excluded')
    ok(await pg.evaluate("!!document.querySelector('[data-act=insCopy]')"),'copy prompt shown')
    # preset
    await pg.evaluate('ACT.askNew()');await pg.wait_for_timeout(200)
    await pg.click('[data-act=askPre][data-k=week]');await pg.wait_for_function("IN.res&&!IN.busy")
    r=await pg.evaluate("({n:IN.res.srcs.length,old:IN.res.srcs.filter(d=>d.kind==='entry'&&d.date<fromN(dnum(ymd())-6)).length})");print('week',r);ok(r['n']>0 and r['old']==0,'weekly preset stays inside 7 days')
    # smart search
    await pg.evaluate('ACT.askNew()');await pg.wait_for_timeout(200)
    await pg.evaluate("insSet().smart=true;IN.pre='';IN.win=0");await pg.fill('#askIn','what gives me energy?');await pg.click('[data-act=askSend]')
    await pg.wait_for_function("IN.res&&!IN.busy",timeout=120000)
    r=await pg.evaluate("({mode:IN.res.mode,err:IN.res.err,top:IN.res.srcs.slice(0,5).map(d=>d.label),vec:VEC&&VEC.size})");print(r)
    ok(r['mode']=='smart' and r['vec']>20,'smart search (MiniLM) indexed and used')
    n=await pg.evaluate("DB.get('ragvec').then(v=>Object.keys(v.v).length)");ok(n>20,f'vectors cached in IndexedDB ({n})')
    # api engine
    await pg.evaluate('ACT.askNew()');await pg.wait_for_timeout(200)
    await pg.evaluate("Object.assign(insSet(),{engine:'api',url:'http://127.0.0.1:8766/v1',model:'llama3.2'})");await pg.fill('#askIn','how is my running going');await pg.click('[data-act=askSend]')
    await pg.wait_for_function("IN.res&&IN.res.turns[0].done",timeout=20000)
    r=await pg.evaluate("({a:IN.res.turns[0].a.length,err:IN.res.turns[0].err,cites:document.querySelectorAll('#insAns .cite').length,b:!!document.querySelector('#insAns b'),li:document.querySelectorAll('#insAns li').length})");print(r)
    ok(r['cites']==3 and r['b'] and r['li']==2 and not r['err'],'streamed answer renders bold, bullets, 3 citations')
    req=json.load(open('/tmp/claude-0/-home-claude/4cd044a0-c38e-5c9e-abda-a471eab1b1b3/scratchpad/lastreq.json'));ok(req['stream'] and 'NOTES' in req['messages'][1]['content'] and 'ABOUT ME' in req['messages'][1]['content'],'server got streaming request with overview + notes')
    await pg.fill('#askIn','which habit should I start with?');await pg.click('[data-act=askSend]');await pg.wait_for_function("IN.res.turns.length==2&&IN.res.turns[1].done",timeout=20000)
    req=json.load(open('/tmp/claude-0/-home-claude/4cd044a0-c38e-5c9e-abda-a471eab1b1b3/scratchpad/lastreq.json'));roles=[m['role'] for m in req['messages']]
    ok(roles==['system','user','assistant','user'] and 'FOLLOW-UP' in req['messages'][3]['content'],'follow-up keeps the conversation '+str(roles))
    ok(await pg.evaluate("document.querySelectorAll('.ask-turn').length")==2,'thread shows both turns')
    await pg.evaluate("document.querySelectorAll('.rv').forEach(e=>e.classList.add('in'))");await pg.wait_for_timeout(1300);await pg.screenshot(path=SP+'/x-ins-answer.png',full_page=True)
    await pg.evaluate("ACT.askSettings()");await pg.wait_for_timeout(300);await pg.click('#sheet [data-act=insTest]');await pg.evaluate('closeSheet()');await pg.wait_for_timeout(500);print('test toast',await pg.evaluate("$('#toast').textContent"))
    before=await pg.evaluate("S.entries.length");await pg.evaluate('closeSheet()');await pg.wait_for_timeout(300);await pg.click('[data-act=insSave]');await pg.wait_for_timeout(300)
    e=await pg.evaluate("S.entries[S.entries.length-1]");ok(await pg.evaluate("S.entries.length")==before+1 and e['title'].startswith('Insight') and '[1]' in e['text'] and 'which habit' in e['text'],'saved whole thread to journal')
    await pg.click('#insAns .cite');await pg.wait_for_timeout(800);print('after cite',await pg.evaluate("[cur.p,$('#sheet').classList.contains('on')]"))
    # on-device engine (wllama + tiny random GGUF: checks download, load, streaming, graceful end)
    await pg.evaluate("closeSheet();go('ask')");await pg.wait_for_timeout(600)
    await pg.evaluate('ACT.askNew()');await pg.wait_for_timeout(200)
    await pg.evaluate("Object.assign(insSet(),{engine:'local',llm:'phi4-mini'});render(false)");await pg.fill('#askIn','how is my running going');await pg.click('[data-act=askSend]');await pg.wait_for_timeout(500)
    ok('Phi-4 mini' in await pg.evaluate("$('#sheet h2').textContent") and not await pg.evaluate("!!IN.busy"),'Phi-4 mini asks before its 1.9 GB download')
    await pg.evaluate("closeSheet()");await pg.wait_for_timeout(300)
    await pg.evaluate('ACT.askNew()');await pg.wait_for_timeout(200)
    await pg.evaluate("Object.assign(insSet(),{engine:'local',llm:'tiny'});render(false)");await pg.fill('#askIn','how is my running going');await pg.click('[data-act=askSend]')
    await pg.wait_for_function("IN.res&&IN.res.turns[0].done",timeout=90000)
    r=await pg.evaluate("({a:IN.res.turns[0].a.length,err:IN.res.turns[0].err,id:LLM_ID,prompt:IN.res.msgs[1].content.length})");print('local',r)
    ok(r['a']>0 and r['id'].startswith('tiny'),'on-device wllama model streamed an answer')
    g=await pg.evaluate("[garbled('alam,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,'),garbled('and,,,,,,,,,,,,,,,,,,,,,,,, ,,,,,,,,'),garbled('You started going to Vasa Fitness on Sep 12 [1]. Next step: keep your Tuesday slot.')]")
    ok(g==[True,True,False],'garbled-output guard '+str(g))
    ok(await pg.evaluate("!!document.querySelector('.ins-top .ins-tc')"),'best matches shown')
    await pg.set_viewport_size({'width':390,'height':844});await pg.evaluate("IN.res.turns[0].a='alam'+','.repeat(400);render(false)");await pg.wait_for_timeout(300)
    ok(await pg.evaluate("document.documentElement.scrollWidth<=innerWidth+1"),'long junk text does not widen the page')
    await pg.set_viewport_size({'width':1280,'height':900});await pg.evaluate("closeSheet();go('ask')");await pg.wait_for_timeout(900)
    await pg.evaluate("document.querySelectorAll('.rv').forEach(e=>e.classList.add('in'))");await pg.screenshot(path=SP+'/x-ins-1280.png',full_page=True)
    errs=[e for e in errs if 'peg' not in e and 'Invalid typed array' not in e and 'Stack trace' not in e];print('errors',errs);await b.close()
asyncio.run(main())
