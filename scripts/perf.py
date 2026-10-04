import asyncio,sys,json
from playwright.async_api import async_playwright
TH=sys.argv[1] if len(sys.argv)>1 else 'glass'
async def main():
  async with async_playwright() as p:
    b=await p.chromium.launch();ctx=await b.new_context(viewport={'width':390,'height':844},device_scale_factor=2.6)
    pg=await ctx.new_page()
    await pg.add_init_script("window.PlotlineNative=new Proxy({activityLoad(){}},{get:(t,k)=>k in t?t[k]:(k==='then'?undefined:()=>'{}')})")
    await pg.goto('http://localhost:8765/index.html');await pg.wait_for_timeout(600)
    await pg.fill('input[name=name]','M');await pg.click('button[value=demo]');await pg.wait_for_timeout(800)
    await pg.evaluate(f"S.settings.setupDone=true;S.settings.tourOffered=true;closeSheet();S.settings.theme='{TH}';applyTheme();save()")
    cdp=await ctx.new_cdp_session(pg);await cdp.send('Emulation.setCPUThrottlingRate',{'rate':4})
    await pg.wait_for_timeout(500)
    res={}
    for r in ['today','habits','cal','goals','vista','journal','ask','today','habits','goals']:
      t=await pg.evaluate("""(r)=>new Promise(res=>{const t0=performance.now();location.hash='#/'+r;
        // wait for hashchange render + 2 frames (paint)
        let t1;const o=render;requestAnimationFrame(()=>{});setTimeout(()=>{t1=performance.now();requestAnimationFrame(()=>requestAnimationFrame(()=>res([Math.round(t1-t0),Math.round(performance.now()-t0)])))},0)})""",r)
      await pg.wait_for_timeout(700)
      res.setdefault(r,[]).append(t)
    print(TH,res)
    # direct render cost
    out={}
    for r in ['today','habits','goals','journal','cal']:
      await pg.evaluate(f"location.hash='#/{r}'");await pg.wait_for_timeout(600)
      out[r]=await pg.evaluate("(()=>{const a=[];for(let i=0;i<3;i++){const t=performance.now();render(false);a.push(Math.round(performance.now()-t))}return a})()")
    print('render(false) ms x4 cpu',out)
    await b.close()
asyncio.run(main())
