import asyncio
from playwright.async_api import async_playwright
async def main():
  async with async_playwright() as p:
    b=await p.chromium.launch();pg=await (await b.new_context(viewport={'width':390,'height':860})).new_page()
    errs=[];pg.on('pageerror',lambda e:errs.append(str(e)));bad=[];pg.on('response',lambda r:r.status>=400 and bad.append(r.url))
    await pg.goto('http://localhost:8765/index.html');await pg.wait_for_timeout(700)
    await pg.fill('input[name=name]','Munazzar');await pg.click('button[value=demo]');await pg.wait_for_timeout(700)
    await pg.evaluate("S.settings.tourOffered=true;S.settings.setupDone=true;S.settings.theme='paper';applyTheme();document.querySelector('.tprompt')?.remove()")
    fams=await pg.evaluate("[...new Set(SCRIPT_FONTS.map(x=>x[2]))]")
    loaded=await pg.evaluate("Promise.all(SCRIPT_FONTS.flatMap(x=>x[4].map(w=>document.fonts.load(w+' 20px \"'+x[2]+'\"').then(r=>r.length>0)))).then(a=>a.every(Boolean))")
    print('all script fonts load',loaded,len(fams),'families');print('404s',bad)
    for k in ['handwritten','elegant','notebook']:
      await pg.evaluate(f"ACT.fontPre({{k:'{k}'}});go('today')");await pg.wait_for_timeout(900)
      await pg.evaluate("document.querySelectorAll('#view .rv').forEach(e=>e.classList.add('in'))");await pg.wait_for_timeout(400)
      print(k,'script flag',await pg.evaluate("document.documentElement.hasAttribute('data-fscript')"),'overflow',await pg.evaluate("document.documentElement.scrollWidth-innerWidth"))
      await pg.screenshot(path=f'x-f-{k}.png')
    await pg.evaluate("go('journal')");await pg.wait_for_timeout(700);await pg.evaluate("document.querySelectorAll('#view .rv').forEach(e=>e.classList.add('in'))");await pg.wait_for_timeout(400);await pg.screenshot(path='x-f-journal.png')
    await pg.evaluate("go('settings/look')");await pg.wait_for_timeout(800);await pg.evaluate("document.querySelector('.fmore')&&(document.querySelector('.fmore').open=true);document.querySelectorAll('#view .rv').forEach(e=>e.classList.add('in'))");await pg.wait_for_timeout(500)
    el=await pg.query_selector('.fpre');await el.screenshot(path='x-f-pre.png') if el else print('no fpre')
    print('errors',errs);await b.close()
asyncio.run(main())
