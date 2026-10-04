import asyncio
from playwright.async_api import async_playwright
async def main():
  async with async_playwright() as p:
    b=await p.chromium.launch();pg=await (await b.new_context(viewport={'width':390,'height':800})).new_page();errs=[]
    pg.on('pageerror',lambda e:errs.append(str(e)));pg.on('console',lambda m:m.type=='error' and errs.append(m.text))
    await pg.goto('http://localhost:8765/index.html');await pg.wait_for_timeout(1500);print(errs[:5]);print(await pg.evaluate("document.body.innerText.slice(0,200)"))
    await b.close()
asyncio.run(main())
