# Native engine path (Android): mock PlotlineNative that forwards llm* calls to a real arm64 llama-server under qemu.
import asyncio,json
from playwright.async_api import async_playwright
MOCK=r"""(()=>{const st={have:{'tiny.gguf':1},running:'',dl:{},ok:true,cores:8};const base={
 llmStatus:()=>JSON.stringify(st),llmDownload:()=>{},llmDelete:()=>{},llmStop:()=>{},
 llmStart:(id,f,ctx,th)=>{window.__starts=(window.__starts||[]).concat([[f,ctx,th]]);fetch('http://localhost:8099/health').then(r=>{st.running=f;setTimeout(()=>window.__llm(id,'ready',''),50)}).catch(e=>window.__llm(id,'err',String(e)))},
 llmCancel:id=>{(window.__ab||{})[id]&&window.__ab[id].abort()},
 llmChat:(id,body)=>{const ab=new AbortController();(window.__ab=window.__ab||{})[id]=ab;const b=JSON.parse(body);b.stream=true;window.__lastBody=b;
  fetch('http://localhost:8099/v1/chat/completions',{method:'POST',signal:ab.signal,headers:{'Content-Type':'application/json',Authorization:'Bearer testkey'},body:JSON.stringify(b)}).then(async r=>{const rd=r.body.getReader(),dec=new TextDecoder();let buf='';for(;;){const{value,done}=await rd.read();if(done)break;buf+=dec.decode(value,{stream:true});let i;while((i=buf.indexOf('\n'))>=0){const l=buf.slice(0,i).trim();buf=buf.slice(i+1);if(!l.startsWith('data:'))continue;const d=l.slice(5).trim();if(d==='[DONE]')continue;try{const t=JSON.parse(d).choices[0].delta.content;if(t)window.__llm(id,'t',t)}catch(e){}}}window.__llm(id,'done','')}).catch(e=>window.__llm(id,ab.signal.aborted?'done':'err',String(e)))}};
 window.PlotlineNative=new Proxy(base,{get:(t,p)=>p in t?t[p]:(p==='notificationsGranted'?()=>true:p==='googleToken'||p==='then'?undefined:()=>'')});
 window.PLOTLINE_RAG={noGuard:1,llms:[['tiny','Tiny test',location.origin+'/models/tiny.gguf',.001,location.origin+'/models/tiny.gguf',.001]]};})()"""
ok=lambda c,m:print(('PASS ' if c else 'FAIL ')+m)
async def main():
  async with async_playwright() as p:
    b=await p.chromium.launch();ctx=await b.new_context(viewport={'width':390,'height':844});await ctx.add_init_script(MOCK)
    pg=await ctx.new_page();errs=[];pg.on('pageerror',lambda e:errs.append(str(e)))
    await pg.goto('http://localhost:8767/index.html');await pg.wait_for_timeout(600);await pg.fill('input[name=name]','M');await pg.click('button[value=demo]');await pg.wait_for_timeout(600)
    await pg.evaluate("S.settings.tourOffered=true;document.querySelector('.tprompt')?.remove();S.entries.push({id:'v1',t:Date.now()-40*864e5,type:'note',goalId:null,title:'Joined Vasa',text:'Signed up at Vasa Fitness today and did my first leg day.',html:'',mood:'😄',private:false});save()")
    r=await pg.evaluate("({nat:NAT_OK(),llm:insSet().llm,engine:insSet().engine})");print(r);ok(r['nat'] and r['llm']=='llama3.2-1b','native detected; default model is Llama 3.2 1B')
    await pg.evaluate("insSet().llm='tiny';go('ask')");await pg.wait_for_timeout(1500)
    ok(await pg.evaluate("(window.__starts||[]).length===1"),'model preloads when Insights opens '+str(await pg.evaluate("window.__starts")))
    await pg.evaluate('ACT.askNew()');await pg.wait_for_timeout(200)
    await pg.fill('#askIn','When did I start going to vasa fitness');await pg.click('[data-act=askSend]');await pg.wait_for_function("IN.res&&IN.res.turns[0].done",timeout=120000)
    r=await pg.evaluate("({a:IN.res.turns[0].a.length,err:IN.res.turns[0].err,first:$('.ins-first b')&&$('.ins-first b').textContent,q:window.__lastBody.messages[1].content.trim().split('\\n').slice(-6,-4).join(' | '),mt:window.__lastBody.max_tokens})");print(r)
    ok(r['a']>0 and not r['err'],'answer streamed from native llama-server')
    ok(bool(r['first']),'earliest matching note shown for a “when” question')
    ok('QUESTION' in r['q'] or 'When did I' in r['q'],'question sits at the end of the prompt')
    await pg.fill('#askIn','and after that?');await pg.click('[data-act=askSend]');await pg.wait_for_function("IN.res.turns.length==2&&IN.res.turns[1].done",timeout=120000)
    ok(await pg.evaluate("IN.res.turns[1].a.length>0&&window.__lastBody.messages.length===4"),'follow-up through native engine')
    await pg.evaluate("document.querySelectorAll('.rv').forEach(e=>e.classList.add('in'))");await pg.wait_for_timeout(1200);await pg.screenshot(path='/home/claude/scripts/x-native.png',full_page=True)
    print('errors',errs);await b.close()
asyncio.run(main())
