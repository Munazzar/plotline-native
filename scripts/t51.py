import asyncio
from playwright.async_api import async_playwright
async def main():
  async with async_playwright() as p:
    b=await p.chromium.launch();ctx=await b.new_context(viewport={'width':390,'height':860},is_mobile=True,has_touch=True,user_agent='Mozilla/5.0 (Linux; Android 14) Mobile');pg=await ctx.new_page();errs=[];pg.on('pageerror',lambda e:errs.append(str(e)))
    await pg.goto('http://localhost:8765/index.html');await pg.wait_for_timeout(700)
    await pg.fill('input[name=name]','M');await pg.tap('button[value=demo]');await pg.wait_for_timeout(700)
    await pg.evaluate("S.settings.tourOffered=true;S.settings.setupDone=true;document.querySelector('.tprompt')?.remove();go('goals')");await pg.wait_for_timeout(800)
    sel=await pg.query_selector('#view select');print('goals select',bool(sel))
    await sel.tap();await pg.wait_for_timeout(600)
    print('PASS in-app picker opens' if await pg.evaluate("!!document.querySelector('.selp.on')") else 'FAIL picker')
    await pg.screenshot(path='x-selp.png')
    v=await pg.evaluate("document.querySelectorAll('.selp-o')[1].dataset.v");await pg.tap('.selp-o:nth-of-type(2)');await pg.wait_for_timeout(600)
    print('PASS choice applies' if await pg.evaluate(f"document.querySelector('#view select')&&(document.querySelector('#view select').value==={repr(v)}||F.area==={repr(v)})") else 'FAIL choice',v)
    print('errors',errs);await b.close()
asyncio.run(main())
