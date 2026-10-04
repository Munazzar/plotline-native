# Remove sample data: tagged + legacy (untagged) installs; user's own data survives.
import asyncio
from playwright.async_api import async_playwright
ok=lambda c,m:print(('PASS ' if c else 'FAIL ')+m)
async def run(pg,legacy):
    await pg.goto('http://localhost:8765/index.html');await pg.evaluate("indexedDB.deleteDatabase('waypoints')");await pg.reload();await pg.wait_for_timeout(600)
    await pg.fill('input[name=name]','M');await pg.click('button[value=demo]');await pg.wait_for_timeout(600);await pg.evaluate("S.settings.tourOffered=true;document.querySelector('.tprompt')?.remove()")
    if legacy:await pg.evaluate("[...S.goals,...S.habits,...S.days,...S.entries].forEach(o=>delete o.demo);save()")
    await pg.evaluate("""(()=>{const run=S.goals.find(g=>g.title==='Run a 10K race');const my=newGoal({title:'My own goal',parent:run.id});S.goals.push(my);const sub=newGoal({title:'Half marathon',parent:run.id});S.goals.push(sub);
      S.habits.push(normHabit({id:'myh',title:'Stretch',goalId:run.id}));S.entries.push({id:'mye',t:Date.now(),type:'note',goalId:run.id,title:'Mine',text:'my note',html:'',mood:'',private:false});
      S.days.push({id:'myd',title:'My day goal',date:ymd(),time:'',goalId:null,done:false,doneAt:null,createdAt:Date.now()});save();render(false)})()""");await pg.wait_for_timeout(400)
    n=await pg.evaluate("demoCount()");ok(n>20,f'{"legacy" if legacy else "tagged"}: sample data detected ({n})')
    ok(await pg.evaluate("!!document.querySelector('.demob [data-act=demoRemove]')"),'Today banner offers removal')
    await pg.click('.demob [data-act=demoRemove]');await pg.wait_for_timeout(300);await pg.click('#sheet [data-act=confirmOk]');await pg.wait_for_timeout(600)
    r=await pg.evaluate("({g:S.goals.map(g=>[g.title,g.parent]),h:S.habits.map(h=>[h.title,h.goalId]),d:S.days.map(x=>x.title),e:S.entries.map(e=>[e.title||e.text,e.goalId]),n:demoCount(),dead:Object.keys(S.dead).length,banner:!!document.querySelector('.demob')})")
    print(r)
    ok(sorted(x[0] for x in r['g'])==['Half marathon','My own goal'] and all(x[1]=='' for x in r['g']),'only my goals remain, moved up a level')
    ok(r['h']==[['Stretch','']] and r['d']==['My day goal'] and r['e']==[['Mine',None]],'my habit, day goal and note stay (unlinked)')
    ok(r['n']==0 and not r['banner'] and r['dead']>20,'no sample data left, banner gone, deletions recorded for sync')
async def main():
  async with async_playwright() as p:
    b=await p.chromium.launch();pg=await b.new_page(viewport={'width':390,'height':844});errs=[];pg.on('pageerror',lambda e:errs.append(str(e)))
    await run(pg,False);await pg.screenshot(path='/home/claude/scripts/x-demo-after.png')
    await run(pg,True)
    print('errors',errs);await b.close()
asyncio.run(main())
