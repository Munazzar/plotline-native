import asyncio,re,sys
sys.path.insert(0,'.')
from sync import google,FILES
from playwright.async_api import async_playwright
async def pull(cdp,x,y0,dist,steps=12):
  await cdp.send('Input.dispatchTouchEvent',{'type':'touchStart','touchPoints':[{'x':x,'y':y0,'id':1}]})
  for k in range(1,steps+1):await cdp.send('Input.dispatchTouchEvent',{'type':'touchMove','touchPoints':[{'x':x,'y':y0+dist*k/steps,'id':1}]})
async def main():
  async with async_playwright() as p:
    b=await p.chromium.launch()
    # drag must not flip on desktop
    pg=await (await b.new_context(viewport={'width':1400,'height':850})).new_page()
    await pg.goto('http://localhost:8765/index.html');await pg.wait_for_timeout(500);await pg.click('button[value=demo]');await pg.wait_for_timeout(400)
    await pg.evaluate("S.settings.layout.goals='h';location.hash='#/goals'");await pg.wait_for_timeout(900)
    r=await pg.evaluate("(()=>{const r=document.querySelector('.hs-item.center .hs-card').getBoundingClientRect();return{x:r.x+r.width/2,y:r.y+r.height/2}})()")
    await pg.mouse.move(r['x'],r['y']);await pg.mouse.down()
    for k in range(1,9):await pg.mouse.move(r['x']-k*30,r['y'])
    await pg.mouse.up();await pg.wait_for_timeout(600)
    print('desktop drag flipped a card:',await pg.evaluate("!!document.querySelector('.fc.flip')"),'| carousel visible',await pg.evaluate("document.querySelector('.hs-item .hs-card').closest('.hs').clientWidth>0"))
    await pg.mouse.click(r['x'],r['y']);await pg.wait_for_timeout(600);print('plain click still flips:',await pg.evaluate("!!document.querySelector('.fc.flip')"))
    # pull to sync on phone
    ctx=await b.new_context(viewport={'width':390,'height':844},has_touch=True,is_mobile=True);await ctx.route(re.compile(r'https://(accounts\.google\.com|www\.googleapis\.com)/.*'),google)
    m=await ctx.new_page();errs=[];m.on('pageerror',lambda e:errs.append(str(e)))
    await m.goto('http://localhost:8765/index.html');await m.wait_for_timeout(500);await m.click('button[value=demo]');await m.wait_for_timeout(600)
    cdp=await ctx.new_cdp_session(m)
    await pull(cdp,200,300,80);ready=await m.evaluate("[document.querySelector('#ptr').classList.contains('on'),document.querySelector('#ptr').classList.contains('ready')]")
    await cdp.send('Input.dispatchTouchEvent',{'type':'touchEnd','touchPoints':[]});await m.wait_for_timeout(500)
    print('short pull shows but not ready',ready,'| hides after',await m.evaluate("!document.querySelector('#ptr').classList.contains('on')"))
    await pull(cdp,200,300,320);await m.screenshot(path='x-ptr.png')
    await cdp.send('Input.dispatchTouchEvent',{'type':'touchEnd','touchPoints':[]});await m.wait_for_timeout(900)
    print('long pull w/o sync -> toast:',await m.evaluate("document.querySelector('#toast').textContent"))
    # turn on fake sync then pull
    FILES.clear()
    await m.evaluate("location.hash='#/settings'");await m.wait_for_timeout(500)
    await m.evaluate("S.settings.sync.on=true;tokSet('tok-abc',3600);save()");await m.wait_for_timeout(300)
    await m.evaluate("location.hash='#/today';window.scrollTo(0,0)");await m.wait_for_timeout(600)
    await pull(cdp,200,300,320);await cdp.send('Input.dispatchTouchEvent',{'type':'touchEnd','touchPoints':[]});await m.wait_for_timeout(150)
    spin=await m.evaluate("document.querySelector('#ptr').classList.contains('spin')");await m.wait_for_timeout(2000)
    print('spins while syncing',spin,'| synced',await m.evaluate("[SYNC_MSG,document.querySelector('#toast').textContent]"),'| drive files',len(FILES),'| hidden after',await m.evaluate("!document.querySelector('#ptr').classList.contains('on')"))
    # scrolled page must not trigger
    await m.evaluate("window.scrollTo(0,400)");await m.wait_for_timeout(200);await pull(cdp,200,300,300);print('no pull when scrolled',await m.evaluate("!document.querySelector('#ptr').classList.contains('on')"));await cdp.send('Input.dispatchTouchEvent',{'type':'touchEnd','touchPoints':[]})
    print('errors',errs);await b.close()
asyncio.run(main())
