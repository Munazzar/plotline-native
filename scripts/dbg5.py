import asyncio
from playwright.async_api import async_playwright
async def main():
  async with async_playwright() as p:
    b=await p.chromium.launch();pg=await (await b.new_context(viewport={'width':390,'height':860})).new_page()
    await pg.goto('http://localhost:8765/index.html');await pg.wait_for_timeout(700)
    await pg.fill('input[name=name]','M');await pg.click('button[value=demo]');await pg.wait_for_timeout(700)
    await pg.evaluate("S.settings.tourOffered=true;S.settings.setupDone=true;document.querySelector('.tprompt')?.remove();go('goals')");await pg.wait_for_timeout(1200)
    print(await pg.evaluate("[...document.querySelectorAll('.fc-t')].slice(0,4).map(el=>{const f=el.closest('.fc-face');const cs=getComputedStyle(el);return [el.textContent,el.scrollWidth,el.clientWidth,f.scrollHeight,f.clientHeight,cs.fontSize,cs.overflowWrap,cs.wordBreak,cs.overflow,el.style.fontSize,!!f.offsetParent]})"))
    await b.close()
asyncio.run(main())
