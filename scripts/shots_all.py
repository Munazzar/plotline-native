import asyncio,sys
from playwright.async_api import async_playwright
TH=sys.argv[1] if len(sys.argv)>1 else 'mocha';W=int(sys.argv[2]) if len(sys.argv)>2 else 390
async def main():
  async with async_playwright() as p:
    b=await p.chromium.launch();pg=await (await b.new_context(viewport={'width':W,'height':860})).new_page();errs=[]
    pg.on('pageerror',lambda e:errs.append(str(e)))
    MOCK=len(sys.argv)>3
    if MOCK:await pg.add_init_script("window.PlotlineNative=new Proxy({activityLoad(){}},{get:(t,k)=>k in t?t[k]:(k==='then'?undefined:()=>'{}')})")
    await pg.goto('http://localhost:8765/index.html');await pg.wait_for_timeout(700)
    await pg.fill('input[name=name]','Munazzar');await pg.click('button[value=demo]');await pg.wait_for_timeout(800)
    await pg.evaluate(f"S.settings.tourOffered=true;S.settings.setupDone=true;S.settings.theme='{TH}';applyTheme();document.querySelector('.tprompt')?.remove();S.settings.demoHide=true;"
      "S.threads=[{id:'t1',title:'Plotline ideas',link:null,status:'open',created:Date.now()-5e8,u:1,ups:[{id:'a',t:Date.now()-4e8,k:'idea',x:'Voice logging from a widget',u:1},{id:'b',t:Date.now()-2e7,k:'prog',x:'Built the quick log box',u:1}]}];"
      "const L=actLog();for(let i=0;i<10;i++)L[fromN(dnum(ymd())-i)]={s:3000+i*700,z:380+i*9,x:i%3?0:40,m:120+i*11,p:{}};S.settings.places=[{id:'p1',name:'Gym',emoji:'🏋️',lat:1,lng:1,r:100}];save()")
    q=await pg.evaluate("[S.goals[0].id,S.habits.find(h=>h.kind==='build').id,S.habits.find(h=>h.kind==='quit').id,(S.habits.find(h=>h.kind==='routine')||S.habits[0]).id,fromN(dnum(ymd())-1)]")
    R=[('home','today',''),('habits','habits',"HV='today'"),('hvista','habits',"HV='vista'"),('hbreak','habits',"HV='quit'"),('hab','habit/'+q[1],''),('quit','habit/'+q[2],''),('routine','habit/'+q[3],''),
       ('calm','cal',"S.settings.layout.cal='month'"),('calw','cal',"S.settings.layout.cal='week'"),('cald','cal',"S.settings.layout.cal='day'"),('goals','goals',''),('goal','goal/'+q[0],''),
       ('road','road',''),('map','map',''),('journal','journal',''),('threads','threads',''),('thread','thread/t1',''),('ask','ask',''),('ai','ai',''),('settings','settings',''),
       ('s-look','settings/look',''),('s-acc','settings/account',''),('s-notif','settings/notif',''),('s-priv','settings/privacy',''),('s-auto','settings/auto',''),('s-share','settings/share',''),('s-voice','settings/voice',''),('s-data','settings/data',''),
       ('activity','activity',''),('day','day/'+q[4],'')]
    if MOCK:R=[x for x in R if x[0] in ('home','activity','day','hab')]
    for n,r,pre in R:
      if pre:await pg.evaluate(pre)
      await pg.evaluate(f"location.hash='#/{r}';render(false)");await pg.wait_for_timeout(900)
      await pg.evaluate("document.querySelectorAll('#view .rv').forEach(e=>e.classList.add('in'));window.scrollTo(0,0)");await pg.wait_for_timeout(350)
      await pg.screenshot(path=f'/tmp/claude-0/ui/{TH}-{W}-{n}{"-app" if MOCK else ""}.png',full_page=True)
    print('errors',errs);await b.close()
asyncio.run(main())
