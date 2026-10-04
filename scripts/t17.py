import asyncio,json
from playwright.async_api import async_playwright
SP='/home/claude/scripts'
async def main():
  async with async_playwright() as p:
    b=await p.chromium.launch();pg=await b.new_page(viewport={'width':390,'height':844});errs=[];pg.on('pageerror',lambda e:errs.append(str(e)))
    E=pg.evaluate
    await pg.goto('http://localhost:8765/index.html');await pg.wait_for_timeout(500);await pg.click('button[value=demo]');await pg.wait_for_timeout(500)
    await E("""(()=>{const m=newGoal({title:'A healthier, stronger year',area:'health',horizon:'year',targetDate:addDays(300)});S.goals.push(m);
     const a=newGoal({title:'Run a 10K race',area:'health',horizon:'quarter',parent:m.id,targetDate:addDays(80),steps:[{title:'Buy shoes',due:addDays(3)},{title:'Run 5K',due:addDays(30)}]});S.goals.push(a);
     const b=newGoal({title:'Race-day nutrition',area:'Kitchen',horizon:'month',parent:a.id,targetDate:addDays(60),steps:[{title:'Test breakfast before long run',due:addDays(20)}]});S.goals.push(b);
     const c=newGoal({title:'Sleep 7+ hours',area:'health',horizon:'month',parent:m.id,targetDate:addDays(40),steps:[{title:'Phone out of bedroom',due:addDays(1)}]});S.goals.push(c);
     m.steps.push({id:uid(),title:'Book a health check-up',due:addDays(10),time:'',remind:'',reminded:false,note:'',done:false,doneAt:null});save();window.M=m.id})()""")
    for md in ('cards','path','orbit'):
      await E(f"S.settings.layout.goal='{md}';location.hash='#/goal/'+M;render(false)");await pg.wait_for_timeout(900)
      await E("document.querySelectorAll('.rv').forEach(e=>e.classList.add('in'))");await pg.wait_for_timeout(400)
      await pg.screenshot(path=f'{SP}/x-g{md}.png',full_page=True)
      print(md,await E("[document.querySelectorAll('.subc').length,document.querySelectorAll('.fl-row').length]"))
    await E("location.hash='#/goals'");await pg.wait_for_timeout(500)
    print('area filter has Kitchen',await E("[...document.querySelectorAll('[data-f=area] option')].map(o=>o.textContent)"))
    await E("location.hash='#/map'");await pg.wait_for_timeout(1500);print('map areas',await E("ML.areas.map(a=>a.name)"))
    await E("ACT.newGoal({})");await pg.wait_for_timeout(300);await pg.fill('#sheet [name=title]','Fix the fence');await pg.fill('#sheet [name=area]','Home projects');await pg.click('#sheet button.pri');await pg.wait_for_timeout(400)
    print('custom saved',await E("JSON.stringify(S.goals.filter(g=>g.title==='Fix the fence').map(g=>[g.area,areaName(g.area),cvar(g).slice(0,25)]))"))
    await E("ACT.newGoal({})");await pg.wait_for_timeout(300);await pg.fill('#sheet [name=title]','Walk daily');await pg.fill('#sheet [name=area]','health');await pg.click('#sheet button.pri');await pg.wait_for_timeout(400)
    print('builtin by name',await E("S.goals.find(g=>g.title==='Walk daily').area"))
    print('errors',errs);await b.close()
asyncio.run(main())
