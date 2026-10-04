import asyncio
from playwright.async_api import async_playwright
SP='/home/claude/scripts'
async def main():
  async with async_playwright() as p:
    b=await p.chromium.launch()
    for W,H in ((1280,800),(390,844),(360,640)):
      pg=await b.new_page(viewport={'width':W,'height':H});errs=[];pg.on('pageerror',lambda e:errs.append(str(e)));E=pg.evaluate
      await pg.goto('http://localhost:8765/index.html');await pg.wait_for_timeout(400);await pg.click('button[value=demo]');await pg.wait_for_timeout(500);await E("S.settings.tourOffered=true;document.querySelector('.tprompt')?.remove()")
      await E("location.hash='#/journal'");await pg.wait_for_timeout(500)
      await pg.click('.fsb');await pg.wait_for_timeout(900)
      print(W,'focus',await E("[document.documentElement.dataset.focus,!!$('#fnav'),getComputedStyle($('.rail')).display,getComputedStyle($('.tabbar')).display]"))
      await pg.screenshot(path=f'{SP}/x-f-journal{W}.png')
      await pg.click('.fn-t');await pg.wait_for_timeout(700);await pg.screenshot(path=f'{SP}/x-f-menu{W}.png')
      await pg.click('.fn-i[href="#/map"]');await pg.wait_for_timeout(2600)
      print(W,'map fills',await E("(()=>{const m=$('.mapwrap').getBoundingClientRect();return[Math.round(m.top),Math.round(m.bottom),innerHeight,$('#fnav .fn-t span').textContent,$('#fnav').classList.contains('open')]})()"))
      await pg.screenshot(path=f'{SP}/x-f-map{W}.png')
      await E("location.hash='#/road'");await pg.wait_for_timeout(900);await pg.screenshot(path=f'{SP}/x-f-road{W}.png')
      await E("location.hash='#/goal/'+S.goals[0].id");await pg.wait_for_timeout(900);await pg.screenshot(path=f'{SP}/x-f-goal{W}.png')
      ov=await E("(()=>{const a=$('#fnav .fn-t').getBoundingClientRect(),o=[];document.querySelectorAll('.ph-r .ibtn,.ph-r .btn,.crumb .ibtn').forEach(x=>{const r=x.getBoundingClientRect();if(r.width&&r.left<a.right&&r.right>a.left&&r.top<a.bottom&&r.bottom>a.top)o.push(x.outerHTML.slice(0,40))});return o})()")
      print(W,'overlap on goal page',ov)
      for pgn in ['today','habits','cal','goals','ai','settings','journal','road']:
        await E(f"location.hash='#/{pgn}'");await pg.wait_for_timeout(350)
        ov=await E("(()=>{const a=$('#fnav .fn-t').getBoundingClientRect(),o=[];document.querySelectorAll('#view .ph-r > *,#view .bar > *').forEach(x=>{const r=x.getBoundingClientRect();if(r.width&&r.left<a.right&&r.right>a.left&&r.top<a.bottom&&r.bottom>a.top)o.push(x.className)});return o})()")
        if ov: print(W,pgn,'overlap',ov)
      await pg.keyboard.press('Escape');await pg.wait_for_timeout(500)
      print(W,'esc exits',await E("[document.documentElement.dataset.focus,!!$('#fnav')]"))
      await pg.keyboard.press('f');await pg.wait_for_timeout(500);print(W,'F enters',await E("document.documentElement.dataset.focus"))
      print(W,'errors',errs);await pg.close()
    await b.close()
asyncio.run(main())
