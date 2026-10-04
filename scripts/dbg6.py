import asyncio
from playwright.async_api import async_playwright
async def main():
  async with async_playwright() as p:
    b=await p.chromium.launch();pg=await (await b.new_context(viewport={'width':390,'height':860})).new_page()
    await pg.goto('http://localhost:8765/index.html');await pg.wait_for_timeout(700)
    await pg.fill('input[name=name]','M');await pg.click('button[value=demo]');await pg.wait_for_timeout(700)
    await pg.evaluate("S.settings.tourOffered=true;S.settings.setupDone=true;document.querySelector('.tprompt')?.remove();go('goal/'+S.goals[0].id)");await pg.wait_for_timeout(1500)
    print(await pg.evaluate("[...document.querySelectorAll('.sc.big .sc-t')].slice(1,2).map(el=>{const L=[];let e=el;while(e&&!e.classList.contains('hs-item')){const r=e.getBoundingClientRect(),c=getComputedStyle(e);L.push(e.className.slice(0,22)+' h'+Math.round(r.height)+' '+c.position+' ov:'+c.overflow+' ar:'+c.aspectRatio);e=e.parentElement}return L})"));print(await pg.evaluate("[...document.querySelectorAll('.sc.big .sc-t')].slice(0,3).map(el=>{const f=el.closest('.sc-face');const ft=[...f.children].find(c=>c!==el&&c.compareDocumentPosition(el)&Node.DOCUMENT_POSITION_PRECEDING);const cs=getComputedStyle(el);return [el.textContent.slice(0,30),cs.fontSize,el.style.cssText,el.scrollWidth,el.clientWidth,f.scrollHeight,f.clientHeight,ft&&ft.className,Math.round(el.getBoundingClientRect().bottom),ft&&Math.round(ft.getBoundingClientRect().top),!!f.offsetParent,cs.lineHeight,cs.overflow,cs.display]})"))
    await b.close()
asyncio.run(main())
