import asyncio,json
from playwright.async_api import async_playwright
MOCK=open('native2.py').read().split('MOCK="""')[1].split('"""')[0]
MOCK+="window.__q='[]';window.PlotlineNative.widgetQueue=()=>{const q=__q;__q='[]';return q};"
async def main():
  async with async_playwright() as p:
    b=await p.chromium.launch();ctx=await b.new_context(viewport={'width':390,'height':844})
    pg=await ctx.new_page();errs=[];pg.on('pageerror',lambda e:errs.append(str(e)));await pg.add_init_script(MOCK)
    E=pg.evaluate
    await pg.goto('http://localhost:8765/index.html');await pg.wait_for_timeout(500);await pg.click('button[value=empty]');await pg.wait_for_timeout(400);await E("S.settings.tourOffered=true;S.settings.setupDone=true;document.querySelector('.tprompt')?.remove();document.querySelector('#sheet.on')&&closeSheet()")
    # create one of each kind through the real forms
    await E("location.hash='#/habits'");await pg.wait_for_timeout(300)
    await E("ACT.newHabit({})");await pg.wait_for_timeout(300);await pg.click('[data-act=hBlank][data-k=build]');await pg.wait_for_timeout(300)
    await pg.fill('#sheet [name=title]','Stretch');await pg.fill('#sheet [name=target]','3');await pg.fill('#sheet [name=unit]','sets');await pg.fill('#sheet [name=time]','07:30');await pg.click('#sheet .sw:has([name=remind])');await pg.click('#sheet button.pri');await pg.wait_for_timeout(400)
    print('build',await E("JSON.stringify((h=>[h.kind,h.target,h.unit,h.time,h.remind,location.hash.startsWith('#/habit/')])(S.habits[0]))"))
    await E("ACT.newHabit({})");await pg.wait_for_timeout(300);await pg.click('[data-act=hBlank][data-k=routine]');await pg.wait_for_timeout(300)
    vis=await E("[...document.querySelectorAll('#sheet [data-k]')].map(x=>x.dataset.k+':'+!x.hidden).join(' ')");print('routine form sections',vis)
    await pg.fill('#sheet [name=title]','Wind down');await pg.fill('#sheet [name=steps]','Tidy up 1\nRead - 2 min\nLights out');await pg.click('#sheet button.pri');await pg.wait_for_timeout(400)
    print('routine steps',await E("JSON.stringify(S.habits[1].steps.map(s=>[s.title,s.min]))"))
    await E("ACT.newHabit({})");await pg.wait_for_timeout(300);await pg.click('[data-act=hBlank][data-k=quit]');await pg.wait_for_timeout(300)
    await pg.fill('#sheet [name=title]','No soda');await E("const i=document.querySelector('#sheet [name=start]');const d=new Date(Date.now()-8*864e5-3600e3);i.value=localDT(d.getTime())");await pg.fill('#sheet [name=cost]','2.5');await pg.click('#sheet button.pri');await pg.wait_for_timeout(400)
    print('quit',await E("JSON.stringify((h=>[h.kind,h.mode,qDays(h),h.mile,Math.round(qSaved(h).money)])(S.habits[2]))"))
    await E("ACT.newHabit({})");await pg.wait_for_timeout(300);await pg.click('[data-act=hBlank][data-k=quit]');await pg.wait_for_timeout(300)
    await pg.click('#sheet label:has([name=mode][value=limit])');await pg.fill('#sheet [name=title]','Coffee');await pg.fill('#sheet [name=limit]','2');await pg.click('#sheet button.pri');await pg.wait_for_timeout(400)
    print('limit',await E("JSON.stringify((h=>[h.kind,h.mode,h.limit])(S.habits[3]))"))
    # taps
    h0="S.habits[0]"
    for i in range(3): await E(f"ACT.hTap({{id:{h0}.id}})")
    print('after 3 taps',await E(f"[hVal({h0},ymd()),hDone({h0},ymd()),hStreak({h0})]"))
    await E(f"ACT.hTap({{id:{h0}.id}})");print('4th tap resets',await E(f"hVal({h0},ymd())"));await E("ACT.undo()");print('undo',await E(f"hVal(S.habits[0],ymd())"))
    # backfill 6 previous days -> 7 day streak milestone
    for k in range(1,7): await E(f"ACT.hDay({{id:S.habits[0].id,d:addDays(-{k})}})")
    print('streak/milestone',await E("[hStreak(S.habits[0]),S.habits[0].smile,S.entries.filter(e=>e.type==='habit').map(e=>e.text)]"))
    await E("S.habits[0].startDate=addDays(-10);save()")
    # pause keeps streak; resume
    await E("ACT.hPause({id:S.habits[0].id})");s1=await E("hStreak(S.habits[0])");await E("ACT.hPause({id:S.habits[0].id})");print('pause keeps streak',s1,await E("[S.habits[0].status,hStreak(S.habits[0]),JSON.stringify(S.habits[0].pz)]"))
    # rest day
    await E("ACT.hSkip({id:S.habits[0].id,d:addDays(-7)})");print('rest day then streak',await E("hStreak(S.habits[0])"))
    # routine player
    await E("ACT.routineGo({id:S.habits[1].id})");await pg.wait_for_timeout(300);print('player',await E("[!!RP,$('#sheet h2').textContent,$('#rpT').textContent]"))
    await E("ACT.rpDone()");await pg.wait_for_timeout(200);await E("ACT.rpSkip()");await pg.wait_for_timeout(200);print('after done+skip',await E("[RP&&RP.i,(S.habits[1].rs[ymd()]||[]).length]"))
    await E("ACT.rpDone()");await pg.wait_for_timeout(200);await E("ACT.rpDone()");await pg.wait_for_timeout(300);print('routine complete',await E("[RP,hDone(S.habits[1],ymd()),$('#toast').textContent]"))
    # quit: urge + slip
    await E("ACT.urge({id:S.habits[2].id})");await pg.wait_for_timeout(1200);print('urge timer',await E("$('#urT').textContent"))
    await E("ACT.urgeWin({id:S.habits[2].id})");await E("ACT.slip({id:S.habits[2].id})");await pg.wait_for_timeout(300);await pg.click('#sheet label:has([name=trig][value=Stress])');await pg.click('#sheet button.pri');await pg.wait_for_timeout(300)
    print('after slip',await E("JSON.stringify((h=>[qDays(h),qBest(h),h.urges.length,h.slips[0].trig,h.mile])(S.habits[2]))"),await E("$('#toast').textContent"))
    await E("ACT.hLimit({id:S.habits[3].id,n:'1'});ACT.hLimit({id:S.habits[3].id,n:'1'});ACT.hLimit({id:S.habits[3].id,n:'1'})");print('limit over',await E("[S.habits[3].log[ymd()],lStreak(S.habits[3])]"))
    # widget payload + queue
    await pg.wait_for_timeout(400)
    w=json.loads(await E("JSON.stringify(widgetHabits())"));print('widget habits',[(x['t'],x['k'],x['n'],x['m'],x['sb'],x['a']) for x in w['habits']],'quits',[(x['t'],x['mode'],x['saved']) for x in w['quits']])
    await E("__q=JSON.stringify([{k:'habit',id:S.habits[0].id,d:addDays(-8),v:3},{k:'habit',id:S.habits[3].id,d:ymd(),v:1}]);applyWidgetQueue()")
    print('queue applied',await E("[S.habits[0].log[addDays(-8)],S.habits[3].log[ymd()],$('#toast').textContent]"))
    sch=await E("(()=>{let L;const o=NATIVE.schedule;NATIVE.schedule=j=>{L=JSON.parse(j)};syncNative();NATIVE.schedule=o;return L.filter(x=>x.hid||x.title.includes('free')).map(x=>[x.title,x.hid?1:0,x.hd,x.hv])})()");print('alarms',sch)
    # detail pages render for each
    for i in range(4):
      await E(f"location.hash='#/habit/'+S.habits[{i}].id");await pg.wait_for_timeout(250)
    await E("location.hash='#/today'");await pg.wait_for_timeout(300);print('today strip',await E("document.querySelectorAll('.hpill').length"))
    # goal link section
    await E("S.goals.push(newGoal({title:'Get fit'}));S.habits[0].goalId=S.goals[0].id;save();location.hash='#/goal/'+S.goals[0].id");await pg.wait_for_timeout(400);print('goal habits section',await E("!!document.querySelector('.hrow')"))
    # weekly review line
    print('weekly',await E("habitWeekLine()"))
    # old backup without habits restores
    print('norm old',await E("JSON.stringify(norm({goals:[],entries:[]}).habits)"))
    print('errors',errs);await b.close()
asyncio.run(main())
