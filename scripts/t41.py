# 1.9.0: Studio home, fonts, new themes, replies kept out of the journal, goal card back fits.
import asyncio,json
from playwright.async_api import async_playwright
n=[0,0]
def ok(c,m):n[0 if c else 1]+=1;print(('PASS ' if c else 'FAIL ')+m)
async def main():
  async with async_playwright() as p:
    b=await p.chromium.launch()
    for W,H in [(1280,860),(390,844)]:
      ctx=await b.new_context(viewport={'width':W,'height':H});await ctx.route('https://fonts.googleapis.com/**',lambda r:r.abort())
      pg=await ctx.new_page();errs=[];pg.on('pageerror',lambda e:errs.append(str(e)))
      await pg.add_init_script('window.__allOpen=1');await pg.goto('http://localhost:8765/index.html');await pg.wait_for_timeout(500)
      await pg.fill('input[name=name]','Ann');await pg.click('button[value=demo]');await pg.wait_for_timeout(700)
      await pg.evaluate("S.settings.tourOffered=true;S.settings.setupDone=true;document.querySelector('.tprompt')?.remove();document.querySelector('#sheet.on')&&closeSheet();go('today')");await pg.wait_for_timeout(500)
      ok(not await pg.evaluate("!!document.querySelector('#studio')"),f'{W}: classic home by default')
      await pg.click('[data-act=homeStyle]');await pg.wait_for_timeout(800)
      ok(await pg.evaluate("!!document.querySelector('#studio')&&S.settings.home==='studio'"),f'{W}: one tap on the home button turns the Studio on')
      c=await pg.evaluate("[document.querySelectorAll('#studio .sn-goal').length,active().length,document.querySelectorAll('#studio .sn-hab').length,S.habits.filter(h=>h.status==='active'&&hDue(h,ymd())).length,document.querySelectorAll('#studio .sn-li').length]")
      ok(c[0]==min(c[1],12) and c[2]==min(c[3],6 if W<640 else 8) and c[4]>0,f'{W}: every active goal and today’s habits are pinned, plus the day list {c}')
      ok(await pg.evaluate("document.querySelectorAll('#studio .st-tray .bc').length>0"),f'{W}: next steps sit on the bench')
      ok(await pg.evaluate("document.documentElement.scrollWidth")<=W,f'{W}: no sideways scroll')
      hid=await pg.evaluate("document.querySelector('#studio .sn-hab .sn-chk').dataset.id");v0=await pg.evaluate(f"hVal(H('{hid}'),ymd())")
      await pg.click('#studio .sn-hab .sn-chk',force=True);await pg.wait_for_timeout(500)
      ok(await pg.evaluate(f"hVal(H('{hid}'),ymd())")!=v0,f'{W}: tapping a sticky note’s check marks the habit')
      await pg.evaluate("document.querySelector('#studio .sn-goal').click()");await pg.wait_for_timeout(500)
      ok((await pg.evaluate("location.hash")).startswith('#/goal/'),f'{W}: tapping a goal card opens it')
      await pg.evaluate("go('today')");await pg.wait_for_timeout(600)
      # cat moves while playing, stays put when paused
      await pg.evaluate("catStop();CAT.el=1000;CAT.plan=null;CAT.cycle=0;catStart()");await pg.wait_for_timeout(1500)
      x1=await pg.evaluate("parseFloat((document.querySelector('#stCat').style.transform.match(/translate3d\(([-\d.]+)px/)||[0,'NaN'])[1])");await pg.wait_for_timeout(1200);x2=await pg.evaluate("parseFloat((document.querySelector('#stCat').style.transform.match(/translate3d\(([-\d.]+)px/)||[0,'NaN'])[1])")
      ok(x2>x1,f'{W}: the cat strolls in ({x1:.1f}% → {x2:.1f}%)')
      await pg.click('#studio .st-play');await pg.wait_for_timeout(300);x3=await pg.evaluate("parseFloat((document.querySelector('#stCat').style.transform.match(/translate3d\(([-\d.]+)px/)||[0,'NaN'])[1])");await pg.wait_for_timeout(1200);x4=await pg.evaluate("parseFloat((document.querySelector('#stCat').style.transform.match(/translate3d\(([-\d.]+)px/)||[0,'NaN'])[1])")
      ok(x3==x4 and await pg.evaluate("document.querySelector('#studio').classList.contains('paused')&&S.settings.studio.play===false"),f'{W}: pause stops the scene and is remembered')
      await pg.click('#studio .st-play');await pg.wait_for_timeout(300)
      seq=await pg.evaluate("(()=>{catStop();CAT.plan=null;CAT.cycle=0;const o=[];for(let t=0;t<150000;t+=500){catDraw(t);o.push(document.querySelector('#stCat').dataset.p)}return[...new Set(o)].join(',')})()")
      ok(seq.startswith('walk') and 'jump' in seq and 'sit' in seq and 'away' in seq,f'{W}: walk → jump up → sit → wander → jump down → away ({seq})')
      for t,lab in [('2026-10-01T12:30:00','day'),('2026-10-01T23:00:00','night')]:
        a=await pg.evaluate(f"(()=>{{window.ST_FAKE=new Date('{t}').getTime();studioSky();return+getComputedStyle(document.querySelector('#studio')).getPropertyValue('--amb')}})()")
        ok(a>.8 if lab=='day' else a<.05,f'{W}: window light follows the time of day ({lab}: {a})')
      print('errors',errs);ok(not errs,f'{W}: no page errors')
      if W==1280:
        await pg.evaluate("go('settings/look')");await pg.wait_for_selector('.tcard[data-act=theme]',timeout=8000)
        tc=await pg.evaluate("document.querySelectorAll('.tcard[data-act=theme]').length");ok(tc==17,f'17 themes (dark and light groups): {tc}')
        await pg.click('.fpcard[data-k=editorial]');await pg.wait_for_timeout(400)
        f=await pg.evaluate("[getComputedStyle(document.documentElement).getPropertyValue('--f-display'),getComputedStyle(document.body).fontFamily]")
        ok('Instrument Serif' in f[0] and 'Source Serif 4' in f[1],f'font pair applies: {f}')
        await pg.wait_for_timeout(800);ok(await pg.evaluate("document.fonts.check(\"16px 'Source Serif 4'\")"),'the bundled font file loads')
        await pg.click('.tcard[data-k=mint]');await pg.wait_for_timeout(500)
        ok(await pg.evaluate("document.documentElement.dataset.mode==='light'&&document.documentElement.dataset.theme==='mint'"),'light themes switch light mode on')
        # replies stay out of the journal; old reply moments are tidied away
        await pg.evaluate("S.entries.push({id:'rp1',t:Date.now(),type:'note',title:'Reply to “💪 Gym”',text:'tired',mood:'',u:1});S.settings.replyClean=0;save()");await pg.wait_for_timeout(1500)
        await pg.reload();await pg.wait_for_timeout(1200)
        ok(await pg.evaluate("!S.entries.some(e=>e.id==='rp1')&&!!S.dead.rp1"),'old “Reply to …” moments removed (and tombstoned for other devices)')
        n0=await pg.evaluate("S.entries.length");await pg.evaluate("applyReplies([{text:'something unclear',o:{k:'habit',hid:S.habits[0].id,title:'Gym'}}])");await pg.wait_for_timeout(800)
        ok(await pg.evaluate("S.entries.length")==n0,'an unclear reply is not saved as a journal moment')
      await ctx.close()
    print(f'{n[0]} passed, {n[1]} failed');await b.close()
asyncio.run(main())
