import asyncio
from playwright.async_api import async_playwright
SP='/home/claude/scripts'
async def main():
  async with async_playwright() as p:
    b=await p.chromium.launch()
    for W,H in ((1280,800),(390,844),(360,600)):
      pg=await b.new_page(viewport={'width':W,'height':H});errs=[];pg.on('pageerror',lambda e:errs.append(str(e)));E=pg.evaluate
      await pg.goto('http://localhost:8765/index.html');await pg.wait_for_timeout(400);await pg.click('button[value=demo]');await pg.wait_for_timeout(500);await E("S.settings.tourOffered=true;document.querySelector('.tprompt')?.remove()")
      # journal filters
      await E("location.hash='#/journal'");await pg.wait_for_timeout(600)
      for f in ['high','mine','goals','habits','steps','all']:
        await E(f"ACT.jFilt({{v:'{f}'}})");await pg.wait_for_timeout(150)
        print(W,f,await E("document.querySelectorAll('.jf').length"))
      await E("ACT.jFilt({v:'high'})");await pg.wait_for_timeout(300)
      # flip an auto entry and view
      await E("document.querySelector('.jf.jk-goal-new')?.click()");await pg.wait_for_timeout(900)
      if W==390: await pg.screenshot(path=f'{SP}/x-jflip.png')
      await E("ACT.viewEntry({id:document.querySelector('.jf.jk-goal-new').dataset.jid})");await pg.wait_for_timeout(700)
      if W==390: await pg.screenshot(path=f'{SP}/x-jview.png')
      await E("document.querySelector('#sheet [data-act=jGo]').click()");await pg.wait_for_timeout(700);print('jumped to',await E("location.hash.split('/')[1]"))
      await E("location.hash='#/journal'");await pg.wait_for_timeout(700)
      await E("const i=document.querySelector('[data-jwdate]');i.value=addDays(-9);i.dispatchEvent(new Event('change',{bubbles:true}))");await pg.wait_for_timeout(350)
      if W==1280: await pg.screenshot(path=f'{SP}/x-jjump.png')
      await pg.wait_for_timeout(1500)
      # map grow + card
      await E("location.hash='#/map'");await pg.wait_for_timeout(500)
      if W==1280: await pg.screenshot(path=f'{SP}/x-mgrow.png')
      await pg.wait_for_timeout(1800);await E("ACT.mapSel({id:S.goals[0].id})");await pg.wait_for_timeout(900)
      r=await E("(()=>{const b=document.querySelector('.mapcard .btn').getBoundingClientRect(),t=document.querySelector('.tabbar');const tb=t&&getComputedStyle(t).display!=='none'?t.getBoundingClientRect().top:innerHeight;return[Math.round(b.bottom),Math.round(tb)]})()")
      print(W,H,'map open btn bottom vs tabbar top',r,r[0]<=r[1])
      if W==360: await pg.screenshot(path=f'{SP}/x-map360.png')
      # calendar fits
      for v in ('month','week','day'):
        await E(f"location.hash='#/cal';S.settings.layout.cal='{v}';render(false)");await pg.wait_for_timeout(700)
        fit=await E("(()=>{const c=document.querySelector('.cal-card').getBoundingClientRect();return[Math.round(c.bottom),innerHeight]})()")
        print(W,v,'card bottom',fit)
        if W in (1280,390): await pg.screenshot(path=f'{SP}/x-cal{v}{W}.png')
      if W==1280:
        for v in ('week','day'):
          await E(f"S.settings.layout.cal='{v}';CAL.d=ymd();render(false)");await pg.wait_for_timeout(600)
          d0=await E("CAL.d");box=await E("(()=>{const r=document.querySelector('.cal-body').getBoundingClientRect();return[r.left+r.width/2,r.top+120]})()")
          await pg.mouse.move(box[0],box[1]);await pg.mouse.down();
          for k in range(12): await pg.mouse.move(box[0]-k*25,box[1]+2);await pg.wait_for_timeout(16)
          await pg.mouse.up();await pg.wait_for_timeout(700);print('drag',v,d0,'->',await E("CAL.d"),'sheet opened?',await E("$('#sheet').classList.contains('on')"))
      print(W,'errors',errs);await pg.close()
    await b.close()
asyncio.run(main())
