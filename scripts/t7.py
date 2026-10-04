import asyncio,json,sys
sys.path.insert(0,'.')
from sync import device,setup_sync,FILES
MOCK=open('native2.py').read().split('MOCK="""')[1].split('"""')[0]
from playwright.async_api import async_playwright
from sync import google
import re
DAYS="S.days.map(d=>d.title+(d.done?'✓':'')).sort()"
async def main():
  async with async_playwright() as p:
    FILES.clear();b=await p.chromium.launch()
    A=await device(b,'A');await A.fill('input[name=name]','M');await A.click('button[value=demo]');await A.wait_for_timeout(500)
    await setup_sync(A)
    B=await device(b,'B');await B.click('[data-act=welcomeSync]');await B.wait_for_timeout(500);await setup_sync(B)
    print('B got days',len(await B.evaluate(DAYS)),'==',len(await A.evaluate(DAYS)))
    # A adds inline, B completes one and deletes another concurrently
    await A.evaluate("location.hash='#/today'");await A.wait_for_timeout(500)
    await A.fill('.tg-add input','Stretch for 10 minutes');await A.press('.tg-add input','Enter');await A.wait_for_timeout(400)
    print('inline add focused again',await A.evaluate("document.activeElement===document.querySelector('.tg-add input')"),'| added',await A.evaluate("S.days.some(d=>d.title==='Stretch for 10 minutes'&&d.date===ymd())"))
    await B.evaluate("(()=>{const x=S.days.find(d=>d.title.startsWith('Call mom'));ACT.toggleDay({id:x.id});const y=S.days.find(d=>d.title.startsWith('Prep lunches'));S.days=S.days.filter(d=>d!==y);save()})()");await B.wait_for_timeout(500)
    for pg in (A,B,A):await pg.evaluate("syncNow(false)");await pg.wait_for_timeout(800)
    da,db=await A.evaluate(DAYS),await B.evaluate(DAYS)
    print('days converged',da==db,'| stretch on B','Stretch for 10 minutes' in db,'| call mom done on A','Call mom after work✓' in da,'| lunches gone on A',not any(x.startswith('Prep') for x in da))
    remote=json.loads(list(FILES.values())[0]['body']);print('drive doc has days',len(remote.get('days',[])))
    # carry-over + move
    await A.evaluate("location.hash='#/today'");await A.wait_for_timeout(400)
    await A.click('[data-act=dayMove]');await A.wait_for_timeout(300);print('carry moved to today',await A.evaluate("S.days.find(d=>d.title==='Clear the email inbox').date===ymd()"))
    # calendar interactions
    await A.evaluate("location.hash='#/cal'");await A.wait_for_timeout(500)
    t=await A.evaluate("document.querySelector('.cal-t h2').textContent");await A.click('[data-act=calStep][data-d=\"1\"]');await A.wait_for_timeout(300);t2=await A.evaluate("document.querySelector('.cal-t h2').textContent")
    await A.click('[data-act=calToday]');await A.wait_for_timeout(300);print('month nav',t,'->',t2,'-> back',await A.evaluate("document.querySelector('.cal-t h2').textContent"))
    d3=await A.evaluate("addDays(3)");await A.click(f'.cd[data-d=\"{d3}\"]');await A.wait_for_timeout(300);await A.click(f'.cd[data-d=\"{d3}\"]');await A.wait_for_timeout(300)
    print('double tap opens day view',await A.evaluate("S.settings.layout.cal"),await A.evaluate("CAL.d")==d3)
    await A.click('.ph-r [data-act=addDay]');await A.wait_for_timeout(300);await A.fill('#sheet [name=title]','Book the dentist');await A.fill('#sheet [name=time]','10:30');await A.click('#sheet button.pri');await A.wait_for_timeout(400)
    print('added on picked day',await A.evaluate(f"S.days.some(d=>d.title==='Book the dentist'&&d.date==='{d3}'&&d.time==='10:30')"),'| shows in list',await A.evaluate("document.querySelector('.agenda').textContent.includes('Book the dentist')"))
    # toggle step from agenda
    await A.evaluate("S.settings.layout.cal='month';CAL.d=ymd();render(false)");await A.wait_for_timeout(300)
    st=await A.evaluate("(()=>{const b=[...document.querySelectorAll('.agr.k-step [data-act=toggleStep]')].pop();const g=G(b.dataset.g),s=g.steps.find(x=>x.id===b.dataset.s);b.click();return s.done})()");print('step toggled from calendar',st)
    reps=await A.evaluate("(()=>{const g=S.goals[0];const s=g.steps.find(x=>!x.done);s.repeat='weekly';s.due=ymd();save();const n=dnum(ymd());return Object.values(calItems(n,n+30)).flat().filter(x=>x.k==='rep'&&x.s===s).length})()");print('weekly repeat projected in next 30 days',reps)
    for v in ['day','week','month']:
      await A.click(f'[data-act=calView][data-v={v}]');await A.wait_for_timeout(250)
    await A.evaluate("location.hash='#/road'");await A.wait_for_timeout(600);await A.click('.fl-chip');await A.wait_for_timeout(400)
    print('timeline row opened',await A.evaluate("!!document.querySelector('.fl-row.open .spl')"),'| draw only on opened row',await A.evaluate("!document.querySelector('#road').classList.contains('draw')&&document.querySelectorAll('.fl-row.draw').length===1"))
    print('page errors',A.errs+B.errs)
    # native payload
    ctx=await b.new_context(viewport={'width':390,'height':844});await ctx.route(re.compile(r'https://(accounts\.google\.com|www\.googleapis\.com)/.*'),google)
    N=await ctx.new_page();errs=[];N.on('pageerror',lambda e:errs.append(str(e)));await N.add_init_script(MOCK)
    await N.goto('http://localhost:8765/index.html');await N.wait_for_timeout(600);await N.evaluate('S.settings.setupDone=true');await N.click('button[value=demo]');await N.wait_for_timeout(900)
    w=await N.evaluate("__calls.filter(c=>c[0]=='widget').pop()[1]")
    print('widget keys',sorted(w.keys()),'| today days',[d['t'] for d in w['days'] if d['d']==await N.evaluate('ymd()')],'| up',len(w['up']),w['up'][0],'| goals',[(g['t'],g['p']) for g in w['goals']],'| counts today',w['counts'].get(await N.evaluate('ymd()')))
    sch=await N.evaluate("(()=>{let L;const o=NATIVE.schedule;NATIVE.schedule=j=>{L=JSON.parse(j)};syncNative();NATIVE.schedule=o;return L.filter(x=>x.body==='Goal for today').map(x=>x.title)})()");print('day-goal reminders',sch)
    await N.evaluate("__route('cal')");await N.wait_for_timeout(500);print('route cal',await N.evaluate("cur.p"))
    await N.evaluate("__route('add')");await N.wait_for_timeout(900);print('route add focuses input',await N.evaluate("document.activeElement&&document.activeElement.closest('.tg-add')!=null"))
    print('native errors',errs)
    await b.close()
asyncio.run(main())
