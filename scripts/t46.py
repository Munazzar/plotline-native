import asyncio
from playwright.async_api import async_playwright
async def main():
  async with async_playwright() as p:
    b=await p.chromium.launch();pg=await (await b.new_context(viewport={'width':390,'height':860})).new_page()
    errs=[];pg.on('pageerror',lambda e:errs.append(str(e)))
    await pg.goto('http://localhost:8765/index.html');await pg.wait_for_timeout(700)
    await pg.fill('input[name=name]','M');await pg.click('button[value=demo]');await pg.wait_for_timeout(700)
    await pg.evaluate("S.settings.tourOffered=true;S.settings.setupDone=true;S.settings.theme='paper';applyTheme();document.querySelector('.tprompt')?.remove();go('habits')");await pg.wait_for_timeout(600)
    await pg.evaluate("document.querySelectorAll('#view .rv').forEach(e=>e.classList.add('in'))");await pg.wait_for_timeout(1200)
    tops=await pg.evaluate("[...document.querySelectorAll('.hbars .hb-d span')].map(s=>Math.round(s.getBoundingClientRect().top))")
    print('labels aligned',len(set(tops))==1,tops)
    el=await pg.query_selector('.hsum');await el.screenshot(path='x-hsum.png')
    await pg.click('.hbars .hb-d:nth-child(3)');await pg.wait_for_timeout(600)
    print('bar opens day',await pg.evaluate("location.hash"),await pg.evaluate("'#/day/'+fromN(dnum(ymd())-4)"))
    print('errors',errs);await b.close()
asyncio.run(main())
