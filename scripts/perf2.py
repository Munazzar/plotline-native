import asyncio,sys
from playwright.async_api import async_playwright
async def main():
  async with async_playwright() as p:
    b=await p.chromium.launch();ctx=await b.new_context(viewport={'width':390,'height':844},device_scale_factor=2.6)
    pg=await ctx.new_page()
    await pg.goto('http://localhost:8765/index.html');await pg.wait_for_timeout(600)
    await pg.fill('input[name=name]','M');await pg.click('button[value=demo]');await pg.wait_for_timeout(800)
    await pg.evaluate("S.settings.setupDone=true;S.settings.tourOffered=true;closeSheet();S.settings.theme='glass';applyTheme();save()")
    cdp=await ctx.new_cdp_session(pg);await cdp.send('Emulation.setCPUThrottlingRate',{'rate':4})
    await pg.evaluate("""window.__T={};const W=n=>{const f=window[n];if(typeof f!=='function')return;window[n]=function(...a){const t=performance.now();try{return f.apply(this,a)}finally{__T[n]=(__T[n]||0)+performance.now()-t}}};
     ['imgCSS','zfixAll','hvInit','authBanner','micDecorate','moveInd','placeSegs','initHS','reveal','fillSpine','fitCards','studioMount','fitH1','foldSettings','crumbFit','sbarTop','stTop','aovPaint','buildFloater'].forEach(W);
     for(const k in VIEWS){const f=VIEWS[k];VIEWS[k]=function(...a){const t=performance.now();try{return f.apply(this,a)}finally{__T['VIEW '+k]=(__T['VIEW '+k]||0)+performance.now()-t}}}
     const vs=viewSet;viewSet=function(v,h){const t=performance.now();vs(v,h);__T.viewSet=(__T.viewSet||0)+performance.now()-t};0""")
    for r in ['today','habits','goals','journal','cal']:
      await pg.evaluate(f"location.hash='#/{r}'");await pg.wait_for_timeout(700)
      out=await pg.evaluate("(()=>{__T={};const t=performance.now();for(let i=0;i<3;i++)render(false);const tot=(performance.now()-t)/3;void document.body.offsetHeight;return [Math.round(tot),Object.entries(__T).map(([k,v])=>[k,+(v/3).toFixed(1)]).sort((a,b)=>b[1]-a[1]).slice(0,8)]})()")
      print(r,out)
    await b.close()
asyncio.run(main())
