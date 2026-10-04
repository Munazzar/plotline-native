# 2.0: web engine <-> native store bridge (NATIVE.storeGet/storeSet, __nativeChanged, __nback, nshell CSS)
import asyncio,json
from playwright.async_api import async_playwright
MOCK="""window.__store='';window.__sets=0;window.__unl=0;
window.PlotlineNative={shellMode(){return true},storeGet(){return window.__store},storeSet(j){window.__store=j;window.__sets++},unlocked(){window.__unl++},
 widget(){},schedule(){},widgetQueue(){return '[]'}};"""
async def main():
  fails=0
  def ok(n,v):
    nonlocal fails;print(('PASS ' if v else 'FAIL ')+n);fails+=0 if v else 1
  async with async_playwright() as p:
    b=await p.chromium.launch();ctx=await b.new_context(viewport={'width':390,'height':800})
    # 1) a normal web profile with demo data in IndexedDB
    pg=await ctx.new_page();errs=[];pg.on('pageerror',lambda e:errs.append(str(e)))
    await pg.goto('http://localhost:8765/index.html');await pg.wait_for_timeout(700)
    await pg.fill('input[name=name]','M');await pg.click('button[value=demo]');await pg.wait_for_timeout(1200)
    n=await pg.evaluate("S.goals.length");await pg.close()
    # 2) same profile opened by the native app: the engine must move the IndexedDB copy into the native store
    pg=await ctx.new_page();pg.on('pageerror',lambda e:errs.append(str(e)))
    await pg.add_init_script(MOCK)
    await pg.goto('http://localhost:8765/index.html');await pg.wait_for_timeout(1500)
    st=await pg.evaluate("window.__store?JSON.parse(window.__store):null")
    ok('first launch moves the data into the native store',bool(st) and len(st['goals'])==n)
    ok('web tab bar hidden in native shell',await pg.evaluate("getComputedStyle(document.querySelector('.tabbar')).display==='none'&&document.documentElement.classList.contains('nshell')"))
    ok('web tour never offered in native shell',await pg.evaluate("S.settings.tourOffered===true"))
    # 3) a native edit reaches the engine
    await pg.evaluate("""(()=>{const d=JSON.parse(window.__store);d.days.push({id:'nat1',title:'From native',date:ymd(),time:'',goalId:null,done:false,doneAt:null,createdAt:Date.now(),u:Date.now()});window.__store=JSON.stringify(d);window.__nativeChanged()})()""")
    await pg.wait_for_timeout(400)
    ok('native edit appears in the engine',await pg.evaluate("S.days.some(x=>x.id==='nat1')&&DIRTY===true"))
    # 4) engine save writes back through storeSet
    s0=await pg.evaluate("window.__sets")
    await pg.evaluate("S.days.find(x=>x.id==='nat1').done=true;save()");await pg.wait_for_timeout(600)
    ok('engine saves go to the native store',await pg.evaluate(f"window.__sets>{s0}&&JSON.parse(window.__store).days.find(x=>x.id==='nat1').done===true"))
    # 5) back handling for the classic layer
    await pg.evaluate("openSheet('<p>x</p>')");await pg.wait_for_timeout(300)
    ok('__nback closes an open sheet',await pg.evaluate("window.__nback()===true")); await pg.wait_for_timeout(400)
    ok('__nback returns false when nothing is open',await pg.evaluate("window.__nback()===false"))
    # 6) lock: no lock set -> native is told it's unlocked
    await pg.evaluate("window.__nlock()")
    ok('__nlock without a lock reports unlocked',await pg.evaluate("window.__unl>=1"))
    ok('no page errors',not errs)
    if errs:print(errs[:3])
    await b.close()
  print('FAILS',fails)
asyncio.run(main())
