# 1.13: overlay touch scroll (no pull-to-refresh), minimized AI plan, crumb row never overlaps, colours & background,
# snappy tab switches, seamless status bar, widget theme payload, glass ink buttons readable
import asyncio
from playwright.async_api import async_playwright
async def touch_drag(cdp,x,y0,y1,steps=8):
  await cdp.send('Input.dispatchTouchEvent',{'type':'touchStart','touchPoints':[{'x':x,'y':y0}]})
  for i in range(1,steps+1):
    await cdp.send('Input.dispatchTouchEvent',{'type':'touchMove','touchPoints':[{'x':x,'y':y0+(y1-y0)*i/steps}]});await asyncio.sleep(0.02)
  await cdp.send('Input.dispatchTouchEvent',{'type':'touchEnd','touchPoints':[]})
async def main():
  fails=0
  async with async_playwright() as p:
    b=await p.chromium.launch()
    ctx=await b.new_context(viewport={'width':360,'height':760},has_touch=True,is_mobile=True,device_scale_factor=2)
    pg=await ctx.new_page();errs=[];pg.on('pageerror',lambda e:errs.append(str(e)))
    await pg.add_init_script("window.__W=null;window.PlotlineNative={widget(j){window.__W=JSON.parse(j)},widgetQueue(){return '[]'}}")
    await pg.goto('http://localhost:8765/index.html');await pg.wait_for_timeout(700)
    await pg.fill('input[name=name]','Munazzar');await pg.click('button[value=demo]');await pg.wait_for_timeout(700)
    await pg.evaluate("S.settings.tourOffered=true;S.settings.setupDone=true;closeSheet();document.querySelector('.tprompt')?.remove();S.settings.ins={...(S.settings.ins||{}),engine:'copy'};save();go('today')");await pg.wait_for_timeout(600)
    def ok(n,v):
      nonlocal fails
      print(('PASS ' if v else 'FAIL ')+n);fails+=0 if v else 1
    cdp=await ctx.new_cdp_session(pg)
    # --- overlay scroll
    await pg.evaluate("""aovOpen();IN.hist=Array.from({length:8},(_,i)=>({q:'Question '+i,a:'Answer '+i+' '+'lorem ipsum '.repeat(40)}));aovPaint(true)""");await pg.wait_for_timeout(700)
    st=await pg.evaluate("(()=>{const b=document.querySelector('.aov-b');return [b.scrollHeight,b.clientHeight,b.scrollTop]})()")
    if st[0]<=st[1]:
      await pg.evaluate("document.querySelector('.aov-b').insertAdjacentHTML('beforeend','<div style=height:2000px></div>');document.querySelector('.aov-b').scrollTop=999999")
    top0=await pg.evaluate("document.querySelector('.aov-b').scrollTop")
    r=await pg.evaluate("(()=>{const r=document.querySelector('.aov-b').getBoundingClientRect();return[r.left+r.width/2,r.top+40,r.bottom-40]})()")
    await touch_drag(cdp,r[0],r[1],r[2]);await pg.wait_for_timeout(500)
    top1=await pg.evaluate("document.querySelector('.aov-b').scrollTop")
    ok('dragging down inside the overlay scrolls it (no refresh)',top1<top0 and await pg.evaluate("!document.querySelector('#ptr.on,#ptr.spin')&&AOV.open"))
    await pg.evaluate("aovClose()");await pg.wait_for_timeout(400)
    # --- AI plan minimize
    await pg.evaluate("S.settings.aipHide=false;S.settings.demoHide=true;save();render(false)");await pg.wait_for_timeout(300)
    has=await pg.evaluate("!!document.querySelector('#view .aip:not(.aip-min) [data-act=aipHide]')")
    if has:
      await pg.click('#view [data-act=aipHide]');await pg.wait_for_timeout(400)
      ok('Minimize leaves a compact Plan with AI bar',await pg.evaluate("!!document.querySelector('#view .aip.aip-min [data-act=aiStart]')&&document.querySelector('#view .aip.aip-min').getBoundingClientRect().height<80"))
      await pg.click('#view .aip.aip-min [data-act=aipShow]');await pg.wait_for_timeout(400)
      ok('expand brings the full card back',await pg.evaluate("!!document.querySelector('#view .aip:not(.aip-min)')"))
    else: ok('plan card present on home',False)
    # --- crumb at 340/360
    for W in (340,360):
      await pg.set_viewport_size({'width':W,'height':760});await pg.evaluate("go('habit/'+S.habits[0].id)");await pg.wait_for_timeout(600)
      ok(f'{W}: crumb buttons never overlap the back button',await pg.evaluate("(()=>{const bk=document.querySelector('#view .crumb>a').getBoundingClientRect();const r=document.querySelector('#view .crumb>.ph-r');const R=r.getBoundingClientRect();return R.left>=bk.right-1&&getComputedStyle(r).overflowX!=='visible'&&document.querySelector('#view .crumb .gearb').getBoundingClientRect().right<=innerWidth})()"))
    await pg.set_viewport_size({'width':390,'height':760})
    # --- colours & background
    await pg.evaluate("S.settings.theme='glass';applyTheme();go('settings/look')");await pg.wait_for_timeout(600)
    ok('colours panel present',await pg.evaluate("!!document.querySelector('.lk-p')"))
    ok('glass defaults to the Aurora background',await pg.evaluate("document.documentElement.dataset.wall==='aurora'"))
    await pg.evaluate("window.__allOpen=1;render(false)");await pg.wait_for_timeout(300)
    await pg.click('.lk-c[data-v="#FF5C8A"]');await pg.wait_for_timeout(300)
    ok('accent applies',await pg.evaluate("getComputedStyle(document.documentElement).getPropertyValue('--accent').trim().toUpperCase()==='#FF5C8A'"))
    await pg.click('.lk-w[data-v=ocean]');await pg.wait_for_timeout(300)
    ok('background choice applies',await pg.evaluate("document.documentElement.dataset.wall==='ocean'"))
    await pg.evaluate("const r=document.querySelector('[data-lk=frost]');r.value=90;r.dispatchEvent(new Event('input',{bubbles:true}))")
    ok('frost slider updates',await pg.evaluate("document.documentElement.style.getPropertyValue('--frost')==='0.90'"))
    await pg.evaluate("S.settings.theme='night';applyTheme();render(false)");await pg.wait_for_timeout(200)
    ok('a background can be used on any theme',await pg.evaluate("document.documentElement.dataset.wall==='ocean'&&getComputedStyle(document.body,'::before').content!=='none'"))
    await pg.click('[data-act=lkReset]');await pg.wait_for_timeout(300)
    ok('reset returns to theme defaults',await pg.evaluate("document.documentElement.dataset.wall==='off'&&!document.documentElement.style.getPropertyValue('--accent')"))
    # --- widget theme payload
    await pg.evaluate("S.settings.theme='glasslight';applyTheme();save()");await pg.wait_for_timeout(900)
    ok('widgets get the theme colours',await pg.evaluate("!!(__W&&__W.th&&/^#[0-9a-f]{8}$/.test(__W.th.bg)&&/^#[0-9a-f]{8}$/.test(__W.th.text)&&__W.th.light===true)"))
    # --- snappy nav: no view transition between tabs
    await pg.evaluate("window.__vt=0;const o=document.startViewTransition&&document.startViewTransition.bind(document);if(o)document.startViewTransition=f=>{__vt++;return o(f)};0")
    for r in ['habits','goals','journal','today']:
      await pg.evaluate(f"location.hash='#/{r}'");await pg.wait_for_timeout(350)
    ok('tab switches skip the slow page transition',await pg.evaluate("__vt===0"))
    ok('on-screen sections are visible right away',await pg.evaluate("[...document.querySelectorAll('#view .rv')].filter(e=>e.getBoundingClientRect().top<innerHeight).every(e=>e.classList.contains('in'))"))
    # --- status bar scrim hidden at top
    ok('status bar scrim invisible at top of page',await pg.evaluate("document.documentElement.classList.contains('pg-top')&&getComputedStyle(document.querySelector('.sbar')).opacity==='0'"))
    # --- glass ink button readable
    await pg.evaluate("S.settings.theme='glasslight';applyTheme();go('today')");await pg.wait_for_timeout(500)
    ok('ink buttons keep their solid fill on glass',await pg.evaluate("(()=>{const b=document.querySelector('#view .btn.ink');if(!b)return true;const c=getComputedStyle(b);return c.backdropFilter==='none'&&!/rgba\\(255, 255, 255, 0\\.[0-9]/.test(c.backgroundColor)})()"))
    print('errors',errs);await b.close()
  print('FAILS',fails)
asyncio.run(main())
