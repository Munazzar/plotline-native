import asyncio,json,sys
from playwright.async_api import async_playwright
JS="""(()=>{const out=[];const kids=[...document.querySelectorAll('#view > *')].filter(e=>getComputedStyle(e).position!=='fixed');
for(let i=1;i<kids.length;i++){const a=kids[i-1].getBoundingClientRect(),b=kids[i].getBoundingClientRect();if(b.top-a.bottom<10&&a.height&&b.height)out.push(['gap',Math.round(b.top-a.bottom),kids[i-1].className.slice(0,30),kids[i].className.slice(0,30)])}
if(document.documentElement.scrollWidth>innerWidth+1)out.push(['hscroll',document.documentElement.scrollWidth]);
document.querySelectorAll('#view *').forEach(e=>{const r=e.getBoundingClientRect();if(r.width&&r.right>innerWidth+1&&!e.closest('.hs,.hscroll,.hstrip,.road,.fl-row,#map,.hs-shell,svg,.jchips,.sc-back,.jf-back'))out.push(['off',e.className&&e.className.baseVal===undefined?e.className.slice(0,30):e.tagName,Math.round(r.right)])});
return out.slice(0,12)})()"""
async def main():
  async with async_playwright() as p:
    b=await p.chromium.launch()
    for w in (360,390,820,1280):
      pg=await (await b.new_context(viewport={'width':w,'height':800})).new_page();errs=[];pg.on('pageerror',lambda e:errs.append(str(e)))
      await pg.goto('http://localhost:8765/index.html');await pg.wait_for_timeout(500);await pg.click('button[value=demo]');await pg.wait_for_timeout(600);await pg.evaluate("S.settings.tourOffered=true;S.settings.setupDone=true;document.querySelector('.tprompt')?.remove();S.threads=[{id:'t1',title:'A fairly long thread title for testing the layout',link:null,status:'open',created:Date.now(),u:1,ups:[{id:'u1',t:Date.now(),k:'idea',x:'First idea',u:1}]}]")
      gid=await pg.evaluate('S.goals[0].id');hid=await pg.evaluate('S.habits[0].id');qid=await pg.evaluate("S.habits.find(h=>h.kind==='quit').id")
      for h in ['today','habits','habit/'+hid,'habit/'+qid,'cal','goals','road','map','journal','ai','ask','settings','settings/account','settings/notif','settings/privacy','settings/auto','settings/share','settings/voice','settings/look','settings/data','goal/'+gid,'threads','thread/t1','day/'+(await pg.evaluate('fromN(dnum(ymd())-1)')),'activity']:
        await pg.evaluate(f"location.hash='#/{h}'");await pg.wait_for_timeout(700)
        r=await pg.evaluate(JS)
        if r:print(w,h,r)
      for v in ['day','week']:
        await pg.evaluate(f"location.hash='#/cal';S.settings.layout.cal='{v}';render(false)");await pg.wait_for_timeout(400);r=await pg.evaluate(JS)
        if r:print(w,'cal-'+v,r)
      print(w,'errors',errs)
    await b.close()
asyncio.run(main())
