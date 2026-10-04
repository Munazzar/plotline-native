# Desktop polish: drag timelines with the mouse, selected day readable in Graphite, no scrollbars.
import asyncio
from playwright.async_api import async_playwright
ok=lambda c,m:print(('PASS ' if c else 'FAIL ')+m)
async def main():
  async with async_playwright() as p:
    b=await p.chromium.launch();pg=await b.new_page(viewport={'width':1280,'height':860});errs=[];pg.on('pageerror',lambda e:errs.append(str(e)))
    await pg.goto('http://localhost:8765/index.html');await pg.wait_for_timeout(500)
    await pg.fill('input[name=name]','M');await pg.click('button[value=demo]');await pg.wait_for_timeout(700)
    await pg.evaluate("S.settings.tourOffered=true;document.querySelector('.tprompt')?.remove()")
    for route,sel in [("S.settings.layout.vista='road';go('road')",'#road'),("HV='vista';go('habits')",'#hvsc')]:
      await pg.evaluate(route);await pg.wait_for_timeout(900)
      bx=await pg.evaluate(f"(()=>{{const r=document.querySelector('{sel}').getBoundingClientRect();return[r.x+r.width/2,r.y+Math.min(r.height-10,120),document.querySelector('{sel}').scrollLeft]}})()")
      await pg.mouse.move(bx[0],bx[1]);await pg.mouse.down();await pg.mouse.move(bx[0]+260,bx[1],steps=8);await pg.mouse.up();await pg.wait_for_timeout(200)
      sl=await pg.evaluate(f"document.querySelector('{sel}').scrollLeft");ok(abs(sl-bx[2])>100,f'{sel} drags sideways with the mouse ({bx[2]:.0f}->{sl:.0f})')
      ok(await pg.evaluate("cur.p")!='habit' and await pg.evaluate("cur.p")!='goal','a drag does not open what is under it')
    await pg.evaluate("document.documentElement.dataset.theme='graphite';S.settings.theme='graphite';S.settings.layout.cal='day';CAL.d=ymd();go('cal')");await pg.wait_for_timeout(800)
    c=await pg.evaluate("(()=>{const b=document.querySelector('.ds.pk b');const s=getComputedStyle(b),p=getComputedStyle(b.closest('.ds'));return[s.color,p.backgroundColor,b.textContent]})()");print(c)
    ok(c[0]!=c[1],'selected day number is readable in Graphite')
    w=await pg.evaluate("(()=>{const g=document.querySelector('#dgrid');return g.offsetWidth-g.clientWidth})()");ok(w==0,'day grid shows no scrollbar')
    await pg.screenshot(path='/home/claude/scripts/x-calday-graphite.png')
    print('errors',errs);await b.close()
asyncio.run(main())
