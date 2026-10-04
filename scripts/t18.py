import asyncio,json,sys
from playwright.async_api import async_playwright
SP='/home/claude/scripts'
W=int(sys.argv[1]) if len(sys.argv)>1 else 390
SNAP=set(map(int,sys.argv[2].split(','))) if len(sys.argv)>2 else set()
async def main():
  async with async_playwright() as p:
    b=await p.chromium.launch();pg=await b.new_page(viewport={'width':W,'height':844});errs=[];pg.on('pageerror',lambda e:errs.append(str(e)))
    E=pg.evaluate
    await pg.goto('http://localhost:8765/index.html');await pg.wait_for_timeout(500)
    await pg.fill('input[name=name]','Munazzar');await pg.click('button[value=empty]');await pg.wait_for_timeout(700)
    # make a tiny real plan
    await E("S.goals.push(newGoal({title:'My real goal',area:'Home projects'}));S.habits.push(normHabit({id:uid(),title:'My real habit'}));save()");await pg.wait_for_timeout(600)
    su=await E("!!document.querySelector('#sheet.on .setup')");print('setup checklist shown first',su)
    if su:
      await pg.click('[data-act=setupDone]');await pg.wait_for_timeout(1200)
    print('prompt shown',await E("!!document.querySelector('.tprompt.on')"))
    before=await E("JSON.stringify([S.goals.map(g=>g.title),S.habits.map(h=>h.title),S.days.length,S.entries.length])")
    db0=await E("DB.get('state').then(d=>JSON.stringify([d.goals.length,d.habits.length]))")
    await pg.click('.tprompt [data-act=tourGo]');await pg.wait_for_timeout(1500)
    n=await E("TOUR_STEPS.length");print('steps',n)
    for i in range(n):
      await pg.wait_for_timeout(4200)
      st=await E("[TR&&TR.i,cur.p,TOUR_STEPS[TR.i].t,!!TR.focus||!!TOUR_STEPS[TR.i].c,$('.tour .trcard').getBoundingClientRect().bottom<=innerHeight+1,$('.tour .trcard').getBoundingClientRect().top>=0]")
      print(st)
      if i in SNAP: await pg.screenshot(path=f'{SP}/x-tour{W}-{i}.png')
      if i<n-1: await E("ACT.tourNext()")
    db1=await E("DB.get('state').then(d=>JSON.stringify([d.goals.length,d.habits.length]))")
    await E("ACT.tourNext()");await pg.wait_for_timeout(1200)
    after=await E("JSON.stringify([S.goals.map(g=>g.title),S.habits.map(h=>h.title),S.days.length,S.entries.length])")
    print('restored',before==after,after,'db untouched during tour',db0==db1,db1,'toured',await E("S.settings.toured"),'overlay gone',await E("!document.querySelector('.tour')"))
    print('errors',errs);await b.close()
asyncio.run(main())
