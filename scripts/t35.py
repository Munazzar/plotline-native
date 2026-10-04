# 1.8.0 smoke: settings hub + areas, app lock (web PIN), voice mics, chapters, year in review, automations sheet (native mock), sharing consent.
import asyncio,re
from playwright.async_api import async_playwright
ok=lambda c,m:print(('PASS ' if c else 'FAIL ')+m)
MOCK=re.search(r'MOCK=r"""(.*?)"""',open('/home/claude/scripts/t30.py').read(),re.S).group(1)
MOCK=MOCK.replace("llmStatus:()=>'{\"ok\":false}'","llmStatus:()=>'{\"ok\":false}',autoStatus:()=>JSON.stringify({steps:{ok:true,perm:true,today:5214},hc:{ok:true,perm:false},screen:{perm:false},loc:{perm:true,bg:false},inside:{}}),autoApps:()=>JSON.stringify([{pk:'com.instagram.android',name:'Instagram',cat:4},{pk:'com.android.chrome',name:'Chrome',cat:-1}]),autoSet:j=>{window.__auto=JSON.parse(j)},screenNow:()=>12,bioState:()=>'{\"bio\":true,\"any\":true}',bioAuth:(t,s)=>setTimeout(()=>window.__bio('ok',''),50),setSecure:v=>{window.__secure=v},voiceState:()=>'{\"onDevice\":true,\"any\":true,\"perm\":true}',hasPerm:()=>true,askPerm:(k,p)=>setTimeout(()=>window.__perm(k,true),20),voiceStart:(id)=>{setTimeout(()=>window.__voice(id,'partial','hello'),50);setTimeout(()=>window.__voice(id,'final','hello world'),120);setTimeout(()=>window.__voice(id,'end',''),150)},voiceStop:()=>{},placeHere:()=>setTimeout(()=>window.__here(JSON.stringify({lat:41.9,lng:-87.9,acc:12})),30)")
async def main():
  async with async_playwright() as p:
    b=await p.chromium.launch()
    for native in [False,True]:
      ctx=await b.new_context(viewport={'width':390,'height':844})
      if native:await ctx.add_init_script(MOCK)
      pg=await ctx.new_page();errs=[];pg.on('pageerror',lambda e:errs.append(str(e)))
      await pg.add_init_script('window.__allOpen=1');await pg.goto('http://localhost:8765/index.html');await pg.wait_for_timeout(600)
      await pg.fill('input[name=name]','M');await pg.click('button[value=demo]');await pg.wait_for_timeout(700)
      await pg.evaluate("S.settings.tourOffered=true;S.settings.setupDone=true;document.querySelector('.tprompt')?.remove()")
      tag='native' if native else 'web'
      await pg.evaluate("go('settings')");await pg.wait_for_timeout(500)
      n=await pg.evaluate("document.querySelectorAll('.shub-r').length");ok(n==8,f'{tag}: settings hub has 8 areas')
      if not native: await pg.screenshot(path='/home/claude/scripts/x-hub.png')
      for k in ['account','look','notif','privacy','auto','share','voice','data']:
        await pg.evaluate(f"go('settings/{k}')");await pg.wait_for_timeout(250)
      ok(not errs,f'{tag}: every settings area renders')
      # lock
      if native:
        await pg.evaluate("go('settings/privacy')");await pg.wait_for_timeout(300);await pg.click('[data-lock=on] + i');await pg.wait_for_timeout(500)
        ok(await pg.evaluate("lockSet().on"),'native: fingerprint lock turns on after confirming')
        await pg.evaluate("showLock()");await pg.wait_for_timeout(1300);ok(await pg.evaluate("!document.querySelector('.lock')"),'native: lock screen opens with the fingerprint prompt and unlocks')
      # voice
      await pg.evaluate("go('ask')");await pg.wait_for_timeout(500)
      has=await pg.evaluate("!!document.querySelector('.has-mic .mic')");print(tag,'mic',has)
      if native:
        await pg.click('.has-mic .mic');await pg.wait_for_timeout(400);v=await pg.evaluate("document.querySelector('#askIn').value");ok(v.endswith('hello world'),'native: voice fills the Ask box ('+v+')')
      # chapters + year
      await pg.evaluate("ACT.chapNew()");await pg.wait_for_timeout(200);await pg.fill('form[data-form=chapSave] input[name=title]','Training season');await pg.evaluate("document.querySelector('form[data-form=chapSave] input[name=start]').value=addDays(-20)");await pg.click('form[data-form=chapSave] .btn.pri');await pg.wait_for_timeout(300)
      await pg.evaluate("S.settings.layout.vista='road';go('road')");await pg.wait_for_timeout(600);ok(await pg.evaluate("!!document.querySelector('.chap')"),f'{tag}: chapter band on Vista')
      await pg.evaluate("ACT.yearOpen()");await pg.wait_for_timeout(400);n=await pg.evaluate("document.querySelectorAll('.yr-s').length");ok(n>=5,f'{tag}: year in review has {n} slides')
      if not native:await pg.screenshot(path='/home/claude/scripts/x-year.png')
      await pg.evaluate("for(let i=0;i<3;i++)ACT.yearStep({d:1})");await pg.wait_for_timeout(300)
      if not native:await pg.screenshot(path='/home/claude/scripts/x-year3.png')
      print(tag,"yr count",await pg.evaluate("document.querySelectorAll('.yrv').length"));await pg.evaluate("ACT.yearClose()");print(tag,"after close",await pg.evaluate("document.querySelectorAll('.yrv').length"))
      # automations
      if native:
        hid=await pg.evaluate("S.habits.find(h=>/walk|water/i.test(h.title)&&h.kind==='build').id")
        await pg.evaluate(f"go('habit/{hid}')");await pg.wait_for_timeout(500);await pg.click('.crumb .bell.auto');await pg.wait_for_timeout(300)
        await pg.click('[data-act=autoPick][data-t=steps]');await pg.click('#auBody [data-f=n][data-v="10000"]');await pg.wait_for_timeout(100)
        await pg.screenshot(path='/home/claude/scripts/x-auto.png');await pg.click('[data-act=autoSave]');await pg.wait_for_timeout(300)
        r=await pg.evaluate(f"(window.__auto&&window.__auto.rules||[]).find(r=>r.hid==='{hid}')");print(r);ok(r and r['type']=='steps' and r['n']==10000,'native: steps automation reaches the phone')
        await pg.evaluate("ACT.autoEdit({k:'habit',id:'"+hid+"'})");await pg.click('[data-act=autoPick][data-t=place]');await pg.click('[data-act=placeNew]');await pg.wait_for_timeout(300);await pg.fill('form[data-form=placeSave] input[name=name]','Gym');await pg.click('form[data-form=placeSave] .btn.pri');await pg.wait_for_timeout(200);await pg.click('[data-act=autoSave]');await pg.wait_for_timeout(300)
        r=await pg.evaluate(f"window.__auto.rules.find(r=>r.hid==='{hid}')");print(r);ok(r['type']=='place' and r['mins']==20 and len(await pg.evaluate("window.__auto.places"))==1,'native: place automation with a saved place, 20 min default')
      # sharing consent appears and needs sync first
      await pg.evaluate(f"PLOTLINE_CFG.firebase={{apiKey:'k',projectId:'p'}};ACT.shareHabit({{id:S.habits[0].id}})");await pg.wait_for_timeout(300)
      ok(await pg.evaluate("document.querySelector('#sheet').textContent.includes('turn on sync first')"),f'{tag}: sharing explains it needs Google sync first')
      print(tag,'errors',errs);await ctx.close()
    await b.close()
asyncio.run(main())
