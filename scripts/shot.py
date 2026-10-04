import asyncio,sys,json
from playwright.async_api import async_playwright
SP='/home/claude/scripts'
# usage: shot.py name w h "js;js" [full] [theme]
async def main():
  name,w,h,js=sys.argv[1],int(sys.argv[2]),int(sys.argv[3]),sys.argv[4]
  full=len(sys.argv)>5 and sys.argv[5]=='full'
  async with async_playwright() as p:
    b=await p.chromium.launch();ctx=await b.new_context(viewport={'width':w,'height':h})
    pg=await ctx.new_page();errs=[];pg.on('pageerror',lambda e:errs.append(str(e)));pg.on('console',lambda m:m.type=='error' and errs.append(m.text))
    await pg.goto('http://localhost:8765/index.html');await pg.wait_for_timeout(600)
    await pg.fill('input[name=name]','Munazzar');await pg.click('button[value=demo]');await pg.wait_for_timeout(700);await pg.evaluate("S.settings.tourOffered=true;document.querySelector('.tprompt')?.remove()")
    for part in js.split('|||'):
      part=part.strip()
      if not part:continue
      if part.startswith('wait '):await pg.wait_for_timeout(int(part[5:]));continue
      if part.startswith('click '):await pg.click(part[6:]);await pg.wait_for_timeout(400);continue
      if part.startswith('snap '):await pg.screenshot(path=f'{SP}/x-{part[5:]}.png',full_page=full);continue
      r=await pg.evaluate(part)
      if r is not None:print(json.dumps(r)[:3000])
      await pg.wait_for_timeout(500)
    await pg.evaluate("document.querySelectorAll('.rv').forEach(e=>e.classList.add('in'))");await pg.wait_for_timeout(1200);await pg.screenshot(path=f'{SP}/x-{name}.png',full_page=full)
    print('errors',errs);await b.close()
asyncio.run(main())
