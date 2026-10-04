# Calendar month: titled chips, habit bars, moods, day peek with habit toggles.
import asyncio,sys
from playwright.async_api import async_playwright
SP='/home/claude/scripts'
ok=lambda c,m:print(('PASS ' if c else 'FAIL ')+m)
async def main():
  async with async_playwright() as p:
    b=await p.chromium.launch()
    for W,H in [(390,844),(1280,860)]:
      pg=await b.new_page(viewport={'width':W,'height':H});errs=[];pg.on('pageerror',lambda e:errs.append(str(e)))
      await pg.goto('http://localhost:8765/index.html');await pg.wait_for_timeout(500)
      await pg.fill('input[name=name]','M');await pg.click('button[value=demo]');await pg.wait_for_timeout(700)
      await pg.evaluate("S.settings.tourOffered=true;document.querySelector('.tprompt')?.remove();S.settings.layout.cal='month';go('cal')");await pg.wait_for_timeout(900)
      await pg.evaluate("document.querySelectorAll('.rv').forEach(e=>e.classList.add('in'))")
      await pg.screenshot(path=f'{SP}/x-calm2-{W}.png')
      r=await pg.evaluate("({chips:[...document.querySelectorAll('.cal-m .cd-chips em')].filter(e=>getComputedStyle(e).display!=='none').length,hb:document.querySelectorAll('.cd-hb').length,peek:!!document.querySelector('#cpeek'),ph:document.querySelectorAll('.cpk-h').length,peekTop:document.querySelector('#cpeek').getBoundingClientRect().top,ow:document.documentElement.scrollWidth>innerWidth})");print(W,r)
      ok(r['chips']>0 and r['hb']>0 and r['peek'] and not r['ow'],f'{W}: chips, habit bars and peek shown, no overflow')
      if W==390: ok(r['peekTop']<H,'day peek starts on screen on a phone')
      if r['ph']:
        before=await pg.evaluate("document.querySelector('.cpk-h').classList.contains('on')");await pg.click('.cpk-h');await pg.wait_for_timeout(300)
        after=await pg.evaluate("document.querySelector('.cpk-h').classList.contains('on')");ok(before!=after,'habit toggles from the peek')
      d=await pg.evaluate("[...document.querySelectorAll('.cal-m .cd')][3].dataset.d");await pg.click(f'.cal-m .cd[data-d="{d}"]');await pg.wait_for_timeout(300)
      ok(await pg.evaluate(f"S.settings.layout.cal==='month'&&CAL.d==='{d}'&&document.querySelector('#cpeek b').textContent.length>3"),'tapping a day updates the peek, stays in month')
      print('errors',errs);await pg.close()
    await b.close()
asyncio.run(main())
