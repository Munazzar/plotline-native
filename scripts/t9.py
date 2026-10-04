import asyncio,json
from playwright.async_api import async_playwright
MOCK=open('native2.py').read().split('MOCK="""')[1].split('"""')[0]
MOCK+="window.__q='[]';window.PlotlineNative.widgetQueue=()=>{const q=__q;__q='[]';return q};"
async def main():
  async with async_playwright() as p:
    b=await p.chromium.launch();ctx=await b.new_context(viewport={'width':390,'height':844},has_touch=True)
    pg=await ctx.new_page();errs=[];pg.on('pageerror',lambda e:errs.append(str(e)));await pg.add_init_script(MOCK)
    await pg.goto('http://localhost:8765/index.html');await pg.wait_for_timeout(600);await pg.click('button[value=demo]');await pg.wait_for_timeout(800)
    w=await pg.evaluate("__calls.filter(c=>c[0]=='widget').pop()[1]")
    print('ids in payload', all(d.get('id') for d in w['days']), all(i.get('sid') and i.get('gid') for i in w['items']), all(g.get('id') for g in w['goals']))
    day=[d for d in w['days'] if not d['done']][0];it=w['items'][0]
    q=[{'k':'day','id':day['id'],'done':True,'at':1},{'k':'step','g':it['gid'],'s':it['sid'],'at':2}]
    await pg.evaluate(f"__q={json.dumps(json.dumps(q))};window.dispatchEvent(new Event('app-resume'))");await pg.wait_for_timeout(600)
    print('queue applied',await pg.evaluate(f"[S.days.find(d=>d.id==='{day['id']}').done,G('{it['gid']}').steps.find(s=>s.id==='{it['sid']}').done]"),'| toast',await pg.evaluate("document.querySelector('#toast').textContent"))
    w2=await pg.evaluate("__calls.filter(c=>c[0]=='widget').pop()[1]");print('widget refreshed, new up-next first:',w2['items'][0]['t']!=it['t'])
    await pg.evaluate("__route('cal-'+addDays(2))");await pg.wait_for_timeout(600);print('route to day',await pg.evaluate("[cur.p,S.settings.layout.cal,CAL.d===addDays(2),!!document.querySelector('#dgrid')]"))
    gid=w['goals'][1]['id'];await pg.evaluate(f"__route('goal/{gid}')");await pg.wait_for_timeout(600);print('route to goal',await pg.evaluate("[cur.p,cur.id]")==['goal',gid])
    # day grid
    await pg.evaluate("CAL.d=ymd();S.settings.layout.cal='day';location.hash='#/cal'");await pg.wait_for_timeout(700)
    await pg.evaluate("document.querySelector('#dgrid').scrollIntoView({block:'center'})");await pg.wait_for_timeout(300)
    await pg.evaluate("document.querySelector('#dgrid').scrollTop=9*60");await pg.wait_for_timeout(200)
    box=await pg.evaluate("(()=>{const b=document.querySelector('.tg-body').getBoundingClientRect(),g=document.querySelector('#dgrid').getBoundingClientRect();return{x:b.left+b.width*.7,y:g.top+40}})()")
    await pg.mouse.click(box['x'],box['y']);await pg.wait_for_timeout(400)
    print('slot tap opens form with time',await pg.evaluate("[document.querySelector('#sheet.on [name=time]')?.value]"))
    await pg.fill('#sheet [name=title]','Planning block');await pg.click('#sheet input[name=dur][value="90"]+span');await pg.click('#sheet button.pri');await pg.wait_for_timeout(500)
    print('block rendered',await pg.evaluate("(()=>{const e=[...document.querySelectorAll('.tev')].find(x=>x.textContent.includes('Planning block'));return e&&[Math.round(e.getBoundingClientRect().height),e.querySelector('.tm').textContent]})()"))
    before=await pg.evaluate("Math.round(document.querySelector('.tev').getBoundingClientRect().height)")
    await pg.click('[data-act=calZoom][data-d="1"]');await pg.wait_for_timeout(500)
    after=await pg.evaluate("Math.round(document.querySelector('.tev').getBoundingClientRect().height)")
    print('zoom in grows blocks',before,'->',after,'| level',await pg.evaluate("[S.settings.layout.calPpm,document.querySelector('#tgz').textContent]"))
    await pg.click('[data-act=calZoom][data-d="-1"]');await pg.click('[data-act=calZoom][data-d="-1"]');await pg.wait_for_timeout(700);print('zoom out',await pg.evaluate("S.settings.layout.calPpm"))
    # pinch via CDP touch events
    cdp=await ctx.new_cdp_session(pg);g=await pg.evaluate("(()=>{const r=document.querySelector('#dgrid').getBoundingClientRect();return{x:r.left+r.width/2,y:r.top+r.height/2}})()")
    p0=await pg.evaluate("calPpm()")
    await cdp.send('Input.dispatchTouchEvent',{'type':'touchStart','touchPoints':[{'x':g['x'],'y':g['y']-30,'id':1},{'x':g['x'],'y':g['y']+30,'id':2}]})
    for k in range(1,6):await cdp.send('Input.dispatchTouchEvent',{'type':'touchMove','touchPoints':[{'x':g['x'],'y':g['y']-30-k*12,'id':1},{'x':g['x'],'y':g['y']+30+k*12,'id':2}]})
    await cdp.send('Input.dispatchTouchEvent',{'type':'touchEnd','touchPoints':[]});await pg.wait_for_timeout(400)
    print('pinch zoom',round(p0,2),'->',round(await pg.evaluate("calPpm()"),2),'| no form opened',await pg.evaluate("!document.querySelector('#sheet').classList.contains('on')"))
    await pg.reload();await pg.wait_for_timeout(900);print('zoom persisted',await pg.evaluate("calPpm()"))
    print('errors',errs);await b.close()
asyncio.run(main())
