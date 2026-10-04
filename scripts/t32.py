# Replies read on the phone: the app tells the phone which downloaded model to use, and applies notes, moves and "stop this" from the queue.
import asyncio,json,re
from playwright.async_api import async_playwright
ok=lambda c,m:print(('PASS ' if c else 'FAIL ')+m)
MOCK=re.search(r'MOCK=r"""(.*?)"""',open('/home/claude/scripts/t30.py').read(),re.S).group(1)
MOCK=MOCK.replace("""llmStatus:()=>'{"ok":false}'""","""llmStatus:()=>JSON.stringify({ok:true,have:{'Llama-3.2-1B-Instruct-Q4_0.gguf':1},dl:{},running:'',cores:8}),setReplyModel:(f,l,c,t)=>{window.__rm=[f,l,c,t]}""")
async def main():
  async with async_playwright() as p:
    b=await p.chromium.launch();ctx=await b.new_context(viewport={'width':390,'height':844});await ctx.add_init_script(MOCK)
    pg=await ctx.new_page();errs=[];pg.on('pageerror',lambda e:errs.append(str(e)))
    await pg.goto('http://localhost:8765/index.html');await pg.wait_for_timeout(600)
    await pg.fill('input[name=name]','M');await pg.click('button[value=demo]');await pg.wait_for_timeout(700)
    await pg.evaluate("S.settings.tourOffered=true;document.querySelector('.tprompt')?.remove();insSet().llm=LLMS.find(m=>natFile(m)==='Llama-3.2-1B-Instruct-Q4_0.gguf')?.[0]||insSet().llm;replyModelSync.k=null;syncNative()")
    print(await pg.evaluate("[LLMS.map(natFile),natHave(llmOf(insSet().llm))]"))
    rm=await pg.evaluate("window.__rm");print(rm);ok(rm and rm[0].endswith('.gguf') and rm[1],'phone told which downloaded model reads replies')
    await pg.evaluate("S.settings.notif={...nset(),ai:false};replyModelSync()");ok(await pg.evaluate("window.__rm[0]===''"),'switching AI replies off falls back to the quick reader')
    ids=await pg.evaluate("(()=>{const g=S.goals.find(g=>g.steps.some(s=>!s.done));const s=g.steps.find(s=>!s.done);s.remind='0';const h=S.habits.find(h=>h.kind==='build');h.remind=true;return{g:g.id,s:s.id,h:h.id}})()")
    n0=await pg.evaluate("S.entries.length")
    q=[{'k':'jnote','text':'Felt amazing after the walk','o':{'k':'habit','hid':ids['h'],'title':'Walk'},'at':1},
       {'k':'move','to':'2026-10-09','o':{'k':'step','g':ids['g'],'s':ids['s']},'at':2},
       {'k':'remoff','o':{'k':'habit','hid':ids['h']},'at':3}]
    await pg.evaluate(f"window.__q={json.dumps(q)};applyWidgetQueue()");await pg.wait_for_timeout(500)
    r=await pg.evaluate(f"({{n:S.entries.length,due:G('{ids['g']}').steps.find(s=>s.id==='{ids['s']}').due,rem:H('{ids['h']}').remind}})");print(r)
    ok(r['n']==n0+1,'note from a reply saved to the journal');ok(r['due']=='2026-10-09','“push it to …” moved the step');ok(r['rem'] is False,'“stop reminding me about this” turned that habit’s reminder off')
    await pg.evaluate("go('settings/notif')");await pg.wait_for_timeout(500)
    ok(await pg.evaluate("!!document.querySelector('[data-nset=ai]')"),'settings show the on-device AI replies switch')
    print('errors',errs);await b.close()
asyncio.run(main())
