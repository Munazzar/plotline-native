import asyncio,sys
from playwright.async_api import async_playwright
async def main():
  async with async_playwright() as p:
    b=await p.chromium.launch()
    for W in [390,1280]:
      pg=await (await b.new_context(viewport={'width':W,'height':860})).new_page();errs=[];pg.on('pageerror',lambda e:errs.append(str(e)))
      await pg.goto('http://localhost:8765/index.html');await pg.wait_for_timeout(700)
      await pg.fill('input[name=name]','M');await pg.click('button[value=demo]');await pg.wait_for_timeout(700)
      await pg.evaluate("S.settings.tourOffered=true;S.settings.setupDone=true;S.settings.theme='night';applyTheme();document.querySelector('.tprompt')?.remove();go('road')");await pg.wait_for_timeout(2200)
      await pg.evaluate("document.querySelectorAll('#view .rv').forEach(e=>e.classList.add('in'))");await pg.wait_for_timeout(500)
      await pg.screenshot(path=f'x-vista-{W}.png')
      # NOW line under chips: element at line x over a chip should be the chip
      r=await pg.evaluate("(()=>{const l=document.querySelector('.today-line').getBoundingClientRect(),c=[...document.querySelectorAll('.fl-chip')].find(c=>{const b=c.getBoundingClientRect();return b.left<l.left&&b.right>l.left});if(!c)return 'no chip crosses';const b=c.getBoundingClientRect();const e=document.elementFromPoint(l.left+1,(b.top+b.bottom)/2);return !!(e&&e.closest('.fl-chip'))})()")
      print(W,'chip above NOW line',r,'overflow',await pg.evaluate("document.documentElement.scrollWidth-innerWidth"))
      await pg.click('[data-act=rdAll]');await pg.wait_for_timeout(600);print(W,'expanded',await pg.evaluate("RD.open.size>0"))
      await pg.click('[data-act=rdToday]');await pg.wait_for_timeout(400)
      await pg.evaluate("ACT.vistaGo({v:'map'})");await pg.wait_for_timeout(1200);await pg.screenshot(path=f'x-vmap-{W}.png')
      print(W,'errors',errs)
    await b.close()
asyncio.run(main())
