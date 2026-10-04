import asyncio,sys
from playwright.async_api import async_playwright
async def main():
  async with async_playwright() as p:
    b=await p.chromium.launch()
    for w in (390,1400):
      pg=await (await b.new_context(viewport={'width':w,'height':850})).new_page()
      await pg.goto('http://localhost:8765/index.html');await pg.wait_for_timeout(500);await pg.click('button[value=demo]');await pg.wait_for_timeout(500)
      for h,prep in [('goals',"S.settings.layout.goals='h'"),('journal',"S.settings.layout.journal='h'"),('goal/'+await pg.evaluate('S.goals[0].id'),"S.settings.layout.goal='cards'")]:
        await pg.evaluate(prep+f";location.hash='#/{h}'");await pg.wait_for_timeout(800)
        await pg.evaluate("(document.querySelector('.hs-item.center .hs-card')||document.querySelector('.hs-card')).scrollIntoView({block:'center'})");await pg.wait_for_timeout(900)
        r=await pg.evaluate("(()=>{const c=document.querySelector('.hs-item.center .hs-card')||document.querySelector('.hs-card');const r=c.getBoundingClientRect();const el=document.elementFromPoint(r.x+r.width/2,r.y+r.height/2);return{x:r.x+r.width/2,y:r.y+r.height/2,hit:el&&el.className,sl:document.querySelector('.hs-item .hs-card').closest('.hs').scrollLeft}})()")
        await pg.mouse.move(r['x'],r['y']);await pg.mouse.down()
        for k in range(1,11):await pg.mouse.move(r['x']+(k*25 if 'journal' in h else -k*25),r['y'])
        await pg.mouse.up();await pg.wait_for_timeout(700)
        sl=await pg.evaluate("document.querySelector('.hs-item .hs-card').closest('.hs').scrollLeft")
        print(w,h.split('/')[0],'hit:',str(r['hit'])[:40],'scroll',r['sl'],'->',sl)
    await b.close()
asyncio.run(main())
