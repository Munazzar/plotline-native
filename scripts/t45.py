# 1.10 round 2: header, journal weeks, threads views, day page, habits calendar, activity (mock phone)
import asyncio,sys
from playwright.async_api import async_playwright
W=int(sys.argv[1]) if len(sys.argv)>1 else 390
MOCK="""(()=>{const t=dnum(ymd());const days={};for(let i=0;i<30;i++){const ds=fromN(t-29+i);days[ds]={steps:3000+i*310,hsteps:i%3?0:9000,sleep:360+(i*13)%120,ex:i%4?0:35,scr:120+(i*17)%90,pl:i%2?{p1:60+i}:{},ap:[['YouTube',40+i],['Chrome',20]],ss:[[Date.now()-(30-i)*864e5-8*36e5,Date.now()-(30-i)*864e5-1*36e5]],w:i%4?[]:[{t:Date.now()-(29-i)*864e5,m:35,k:56}]}}
NATIVE.activityLoad=n=>{setTimeout(()=>window.__act(JSON.stringify({ok:{steps:true,hc:true,hcSupported:true,screen:true,loc:true,bg:true},days,work:[{t:Date.now()-36e5,m:35,k:56},{t:Date.now()-864e5,m:50,k:70}],top:[{pk:'a',name:'YouTube',min:48},{pk:'b',name:'Chrome',min:22}],inside:{p1:Date.now()-6e5}})),50)};
NATIVE.autoStatus=()=>JSON.stringify({steps:{ok:true,perm:true,today:5000},hc:{ok:true,perm:true},screen:{perm:true},loc:{perm:true,bg:true},inside:{}});
const h=S.habits.find(x=>x.kind!=='quit');const y=Date.now()-864e5;NATIVE.notifLog=k=>JSON.stringify([{t:y,kind:'sent',key:'h:'+h.id,title:h.title,text:'Time for it'},{t:y+6e4,kind:'reply',key:'h:'+h.id,title:h.title,text:'busy, remind me in 2 hours',res:'⏰ I’ll remind you again at 3 PM'},{t:y+72e5,kind:'done',key:'h:'+h.id,title:h.title}].filter(e=>k==='*'||e.key===k));
S.settings.places=[{id:'p1',name:'Gym',emoji:'🏋️',lat:1,lng:1,r:100}];})()"""
async def main():
  async with async_playwright() as p:
    b=await p.chromium.launch();pg=await (await b.new_context(viewport={'width':W,'height':860})).new_page()
    errs=[];pg.on('pageerror',lambda e:errs.append(str(e)));pg.on('console',lambda m:m.type=='error' and 'TUNNEL' not in m.text and 'Failed to load' not in m.text and errs.append(m.text))
    await pg.add_init_script("window.PlotlineNative={}")
    await pg.goto('http://localhost:8765/index.html');await pg.wait_for_timeout(700)
    await pg.fill('input[name=name]','Munazzar');await pg.click('button[value=demo]');await pg.wait_for_timeout(700)
    await pg.evaluate("S.settings.tourOffered=true;S.settings.setupDone=true;S.settings.theme='paper';applyTheme&&applyTheme();document.querySelector('.tprompt')?.remove()")
    await pg.evaluate(MOCK)
    ok=lambda n,v:print(('PASS ' if v else 'FAIL ')+n)
    async def shot(n,full=False):
      await pg.evaluate("document.querySelectorAll('#view .rv').forEach(e=>e.classList.add('in'))");await pg.wait_for_timeout(350);await pg.screenshot(path=f'x-{n}-{W}.png',full_page=full)
    # header: gear on the title row
    for r in ['today','habits','journal','goals','cal','ask']:
      await pg.evaluate(f"go('{r}')");await pg.wait_for_timeout(450)
      g=await pg.evaluate("(()=>{const h=document.querySelector('#view .ph'),t=h&&h.querySelector('h1'),g=h&&h.querySelector('.gearb');if(!g)return 'nogear';const a=g.getBoundingClientRect(),b=(t&&getComputedStyle(t).display!=='none'?t:h.querySelector('.data')).getBoundingClientRect();return (h.classList.contains('ph-stack')?a.top<=b.top:Math.abs((a.top+a.bottom)/2-(b.top+b.bottom)/2)<a.height)&&a.right>innerWidth-40})()")
      ok(f'{r}: gear on the first line, right corner',g is True)
    await pg.evaluate("go('today')");await pg.wait_for_timeout(900);await shot('today',True)
    ok('today: activity card shows 4 metrics',await pg.evaluate("document.querySelectorAll('#actCard .at-t').length===4"))
    # threads
    await pg.evaluate("go('threads')");await pg.wait_for_timeout(400)
    await pg.click('button[data-act=thrNew]');await pg.wait_for_timeout(450)
    await pg.fill('#sheet input[name=title]','Plotline ideas');await pg.fill('#sheet textarea[name=x]','Slow thought to begin with');await pg.click('#sheet button[type=submit]');await pg.wait_for_timeout(700)
    ok('thread: not linked unless chosen',await pg.evaluate("S.threads[0].link===null"))
    await pg.click('.thr-tags.tg-s [data-v=prog]');await pg.fill('.thr-in textarea','Sketched the screens');await pg.click('.thr-in button[type=submit]');await pg.wait_for_timeout(500)
    ok('thread: newest first',await pg.evaluate("document.querySelector('#view .hs-item .thr-utext, #view .thr-uc .jt').textContent.includes('Sketched')"))
    await shot('thread-h')
    await pg.evaluate("ACT.layout({k:'journal',v:'v'})");await pg.wait_for_timeout(400);await shot('thread-v')
    ok('thread: vertical has update cards',await pg.evaluate("document.querySelectorAll('.thr-uc').length===2"))
    await pg.evaluate("go('threads')");await pg.wait_for_timeout(400);await shot('threads-v')
    await pg.evaluate("ACT.layout({k:'journal',v:'h'})");await pg.wait_for_timeout(400);await shot('threads-h')
    # journal week
    await pg.evaluate("go('journal')");await pg.wait_for_timeout(500);await shot('journal')
    ok('journal: newest first',await pg.evaluate("(()=>{const L=[...document.querySelectorAll('.hs-item')].map(x=>[...x.classList].find(c=>c.startsWith('jd-')));return L.length>0&&L.every((v,i)=>!i||L[i-1]>=v)})()"))
    ok('journal: only 7 days',await pg.evaluate("[...document.querySelectorAll('.hs-item')].every(x=>{const d=[...x.classList].find(c=>c.startsWith('jd-')).slice(3);return dnum(d)>dnum(ymd())-7})"))
    await pg.evaluate("ACT.jwGo({d:1})");await pg.wait_for_timeout(400);ok('journal: earlier week',await pg.evaluate("JW===1"))
    await pg.evaluate("ACT.jwGo({d:-1})");await pg.wait_for_timeout(300)
    # habits
    await pg.evaluate("go('habits')");await pg.wait_for_timeout(400)
    ok('habits: no History tab',await pg.evaluate("!document.querySelector('[data-seg=hv] [data-v=history]')"))
    await pg.click('.hcal-t');await pg.wait_for_timeout(600);await shot('habits-cal')
    ok('habits: month opens',await pg.evaluate("document.querySelectorAll('#hcal .hh-c').length>27"))
    yd=await pg.evaluate("fromN(dnum(ymd())-1)")
    await pg.click(f'.hh-d[data-d="{yd}"]');await pg.wait_for_timeout(700)
    ok('day page opens',await pg.evaluate(f"location.hash==='#/day/{yd}'"))
    await shot('day',True)
    hid=await pg.evaluate("S.habits.find(x=>x.kind!=='quit').id")
    before=await pg.evaluate(f"hVal(H('{hid}'),'{yd}')")
    await pg.click(f'.dy-h[data-hid="{hid}"] .hh-k');await pg.wait_for_timeout(500)
    ok('day: habit editable',await pg.evaluate(f"hVal(H('{hid}'),'{yd}')")!=before)
    await pg.click(f'.dy-h[data-hid="{hid}"] .dy-hx');await pg.wait_for_timeout(500)
    ok('day: reminders and reply shown',await pg.evaluate(f"document.querySelectorAll('.dy-h[data-hid=\"{hid}\"] .dy-n').length>=2"))
    await shot('day-open')
    ok('day: activity for the day',await pg.evaluate("!!document.querySelector('#dyAct .ad-grid')"))
    await pg.evaluate("ACT.dyGo({d:-1})");await pg.wait_for_timeout(400);ok('day: previous day',await pg.evaluate(f"location.hash==='#/day/'+fromN(dnum('{yd}')-1)"))
    # activity
    await pg.evaluate("go('activity')");await pg.wait_for_timeout(800);await shot('activity',True)
    ok('activity: bars open a day',await pg.evaluate("document.querySelectorAll('.abg[data-act=dyOpen]').length>=7"))
    ok('activity: day-by-day history',await pg.evaluate("document.querySelectorAll('.ah-r').length>=14"))
    await pg.evaluate("ACT.actMore()");await pg.wait_for_timeout(300);ok('activity: older days',await pg.evaluate("document.querySelectorAll('.ah-r').length>=30"))
    # overflow check
    for r in ['today','habits','journal','threads','activity','day/'+yd,'thread/'+await pg.evaluate("S.threads[0].id")]:
      await pg.evaluate(f"go('{r}')");await pg.wait_for_timeout(500)
      ov=await pg.evaluate("document.documentElement.scrollWidth-innerWidth")
      ok(f'{r}: no sideways overflow ({ov})',ov<=1)
    print('errors',errs);await b.close()
asyncio.run(main())
