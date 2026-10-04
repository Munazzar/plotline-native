import asyncio,json
from playwright.async_api import async_playwright
SP='/home/claude/scripts'
def plan(roots):
  gs=[]
  if roots==1:
    gs.append(dict(id='new-1',action='add',parent=None,title='Build a calmer, stronger family life',why='So the next five years feel intentional.',area='family',horizon='multi',priority=1,startInDays=0,targetInDays=720,steps=[],linksTo=[]))
    P='new-1'
  else: P=None
  gs.append(dict(id='new-2',action='add',parent=P,title='Weekend adventures',why='',area='family',horizon='quarter',priority=2,startInDays=0,targetInDays=90,steps=[dict(id='new',title='List ten places nearby',dueInDays=2,dueTime=None,reminder='none',note='')],linksTo=[]))
  gs.append(dict(id='new-3',action='add',parent='new-2',title='Plan the lake trip',why='',area='family',horizon='month',priority=2,startInDays=0,targetInDays=30,steps=[dict(id='new',title='Book the cabin',dueInDays=5,dueTime=None,reminder='1d',note='')],linksTo=[]))
  gs.append(dict(id='new-4',action='add',parent=P,title='Emergency fund',why='',area='Side business',horizon='year',priority=1,startInDays=0,targetInDays=300,steps=[],linksTo=['new-2']))
  return 'Here you go.\n```json\n'+json.dumps(dict(format='plotline-plan',version=1,mode='create',goals=gs))+'\n```'
async def main():
  async with async_playwright() as p:
    b=await p.chromium.launch();pg=await b.new_page(viewport={'width':390,'height':844});errs=[];pg.on('pageerror',lambda e:errs.append(str(e)))
    await pg.goto('http://localhost:8765/index.html');await pg.wait_for_timeout(500);await pg.click('button[value=empty]');await pg.wait_for_timeout(500)
    for roots in (1,2):
      await pg.evaluate("location.hash='#/ai'");await pg.wait_for_timeout(400)
      await pg.evaluate("r=>{document.getElementById('aiReply').value=r;ACT.parseAI()}",plan(roots));await pg.wait_for_timeout(500)
      print('main',await pg.evaluate("JSON.stringify(AI.main)"),'sel',await pg.evaluate("[...AI.sel]"),'errs',await pg.evaluate("AI.errs"))
      await pg.fill('[data-aim=title]','My family plan' if roots==2 else 'Family first');await pg.fill('[data-ait="2"]','Plan the lake trip (July)')
      await pg.evaluate("document.querySelectorAll('.rv').forEach(e=>e.classList.add('in'));document.querySelector('.aimain').scrollIntoView()");await pg.wait_for_timeout(300)
      await pg.screenshot(path=f'{SP}/x-ai{roots}.png',full_page=False)
      await pg.evaluate("ACT.doImport()");await pg.wait_for_timeout(800)
      print(await pg.evaluate("JSON.stringify(S.goals.map(g=>[g.title,G(g.parent)?G(g.parent).title:'',pct(g)]))"))
      print('hash',await pg.evaluate("location.hash"))
    await pg.evaluate("document.querySelectorAll('.rv').forEach(e=>e.classList.add('in'))");await pg.wait_for_timeout(300);await pg.screenshot(path=f'{SP}/x-umb.png',full_page=True)
    # complete sub-sub goal and check roll-up
    await pg.evaluate("const g=S.goals.find(x=>x.title.startsWith('Plan the lake'));ACT.toggleStep({g:g.id,s:g.steps[0].id})");await pg.wait_for_timeout(1500)
    print(await pg.evaluate("JSON.stringify(S.goals.map(g=>[g.title,g.status,pct(g)]))"))
    await pg.evaluate("location.hash='#/goals'");await pg.wait_for_timeout(500);await pg.evaluate("document.querySelectorAll('.rv').forEach(e=>e.classList.add('in'))");await pg.screenshot(path=f'{SP}/x-goals.png',full_page=True)
    print('errors',errs);await b.close()
asyncio.run(main())
