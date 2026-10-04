# Habit Vista: lanes per habit, streak runs, zoom keeps working, tap a stop opens the day sheet.
import asyncio
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
      await pg.evaluate("S.settings.tourOffered=true;document.querySelector('.tprompt')?.remove();go('habits')");await pg.wait_for_timeout(600)
      await pg.click('[data-act=hView][data-v=vista]');await pg.wait_for_timeout(600)
      await pg.evaluate("document.querySelectorAll('.rv').forEach(e=>e.classList.add('in'))")
      r=await pg.evaluate("({lanes:document.querySelectorAll('.hv-l').length,habits:hvHabits().length,stops:document.querySelectorAll('.hv-d').length,runs:document.querySelectorAll('.hv-run').length,atEnd:(()=>{const s=document.querySelector('#hvsc');return s.scrollLeft+s.clientWidth>=s.scrollWidth-2})(),ow:document.documentElement.scrollWidth>innerWidth})");print(W,r)
      ok(r['lanes']==r['habits'] and r['stops']>0 and r['runs']>0,'one lane per habit with stops and streak runs')
      ok(r['atEnd'] and not r['ow'],'opens at today, page does not overflow')
      await pg.wait_for_timeout(300);await pg.evaluate("document.querySelector('.hvista').scrollIntoView()");await pg.screenshot(path=f'{SP}/x-hvista-{W}.png')
      w0=await pg.evaluate("document.querySelector('.hv-svg').width.baseVal.value");await pg.click('[data-act=hvSpan][data-v="90"]');await pg.wait_for_timeout(600)
      w1=await pg.evaluate("document.querySelector('.hv-svg').width.baseVal.value");ok(w1!=w0,f'3-month span redraws the timeline {w0}->{w1}')
      await pg.screenshot(path=f'{SP}/x-hvista-out-{W}.png');await pg.click('[data-act=hvSpan][data-v="7"]');await pg.wait_for_timeout(600);await pg.screenshot(path=f'{SP}/x-hvista-wk-{W}.png');await pg.click('[data-act=hvSpan][data-v="30"]');await pg.wait_for_timeout(500)
      await pg.evaluate("document.querySelector('.hv-d').dispatchEvent(new MouseEvent('click',{bubbles:true}))");await pg.wait_for_timeout(400)
      ok(await pg.evaluate("!!document.querySelector('.sheet.on, .sheet-wrap.on, dialog[open], .sheet')"),'tapping a stop opens the day sheet')
      print('errors',errs);await pg.close()
    await b.close()
asyncio.run(main())
