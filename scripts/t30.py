# Notifications: alarms carry what they're about, settings filter them, replies from the queue are applied.
import asyncio,json
from playwright.async_api import async_playwright
ok=lambda c,m:print(('PASS ' if c else 'FAIL ')+m)
MOCK=r"""
window.__sched=[];window.__q=[];window.__mute=0;
window.PlotlineNative={schedule:j=>{window.__sched=JSON.parse(j)},notificationsGranted:()=>true,requestNotifications:()=>{},widget:()=>{},widgetQueue:()=>{const q=JSON.stringify(window.__q);window.__q=[];return q},
 notifState:()=>JSON.stringify({muteUntil:window.__mute,missed:0}),notifMute:m=>{window.__mute=m?Date.now()+m*60000:0;return m?'Muted':'Reminders are back on'},llmStatus:()=>'{"ok":false}'};
window.PlotlineNative=new Proxy(window.PlotlineNative,{get:(t,k)=>k in t?t[k]:(()=>'')});
"""
async def main():
  async with async_playwright() as p:
    b=await p.chromium.launch();ctx=await b.new_context(viewport={'width':390,'height':844});await ctx.add_init_script(MOCK)
    pg=await ctx.new_page();errs=[];pg.on('pageerror',lambda e:errs.append(str(e)))
    await pg.add_init_script('window.__allOpen=1');await pg.goto('http://localhost:8765/index.html');await pg.wait_for_timeout(600)
    nat=await pg.evaluate("!!NATIVE");print('native',nat)
    await pg.fill('input[name=name]','M');await pg.click('button[value=demo]');await pg.wait_for_timeout(700)
    await pg.evaluate("S.settings.tourOffered=true;document.querySelector('.tprompt')?.remove();S.habits.forEach(h=>{if(h.kind!=='quit'){h.remind=true;h.time='23:59'}});syncNative()")
    L=await pg.evaluate("window.__sched");ks=sorted(set(x.get('k') for x in L));print(len(L),ks)
    ok('habit' in ks and all('k' in x for x in L),'every reminder says what it is about')
    hb=[x for x in L if x['k']=='habit'][0];ok(hb.get('hid') and hb.get('hd') and hb.get('rp') is True and hb.get('mute')==120,'habit reminder has its habit, day, reply and mute length')
    w=[x for x in L if x['k']=='wrap'];ok(len(w)>0 and len(w[0]['hs'])>0,'evening wrap-up lists open habits')
    await pg.evaluate("S.settings.notif={...nset(),habits:false,mute:60};syncNative()");L2=await pg.evaluate("window.__sched")
    ok(not any(x['k']=='habit' for x in L2) and all(x.get('mute')==60 for x in L2),'turning habit reminders off removes them; mute length passes through')
    await pg.evaluate("S.settings.notif={...nset(),habits:true,quiet:true,qFrom:'23:00',qTo:'07:00'};syncNative()");L3=await pg.evaluate("window.__sched")
    ok(not any(x['k']=='habit' for x in L3),'quiet hours drop the 23:59 reminders')
    # settings UI
    await pg.evaluate("go('settings/notif')");await pg.wait_for_timeout(700)
    ok(await pg.evaluate("!!document.querySelector('#notifSet [data-nset=habits]')&&!!document.querySelector('[data-act=notifMute]')"),'notification settings and focus mute shown')
    await pg.evaluate("document.querySelector('[data-act=notifMute][data-m=\"120\"]').click()");await pg.wait_for_timeout(300)
    ok(await pg.evaluate("document.querySelector('.nmute.on')!=null"),'mute 2h shows the muted state')
    await pg.wait_for_timeout(2600);await pg.evaluate("document.querySelectorAll('.rv').forEach(e=>e.classList.add('in'));document.querySelector('#notifSet').scrollIntoView()");await pg.wait_for_timeout(300);await pg.screenshot(path='/home/claude/scripts/x-notif.png',full_page=False)
    # queued actions from the notification bar
    h=await pg.evaluate("(()=>{const h=S.habits.find(h=>h.kind==='build'&&!hDone(h,ymd()));return{id:h.id,t:h.title,v:hTarget(h)}})()")
    h2=await pg.evaluate("(()=>{const L=S.habits.filter(h=>h.kind==='build'&&!hDone(h,ymd()));return L.length>1?{id:L[1].id,t:L[1].title}:null})()")
    q=[{'k':'habit','id':h['id'],'d':await pg.evaluate('ymd()'),'v':h['v'],'at':1},
       {'k':'reply','text':'did '+h2['t'].lower(),'o':{'k':'wrap','hd':await pg.evaluate('ymd()'),'title':'2 habits','hs':[{'id':h2['id'],'t':h2['t'],'v':1}]},'at':2},
       {'k':'reply','text':'Feeling great about the run, legs are sore','o':{'k':'checkin','g':await pg.evaluate("S.goals[0].id"),'title':'Check in: x'},'at':3}]
    n0=await pg.evaluate("S.entries.length")
    await pg.evaluate(f"window.__q={json.dumps(q)};applyWidgetQueue()");await pg.wait_for_timeout(1500)
    r=await pg.evaluate(f"({{a:hDone(H('{h['id']}'),ymd()),b:hDone(H('{h2['id']}'),ymd()),n:S.entries.length}})");print(r)
    ok(r['a'],'Done from the notification checks the habit off')
    ok(r['b'],'free-text wrap reply ticks the habit it names')
    ok(r['n']==n0+1,'unclear check-in reply is saved to the journal')
    print('errors',errs);await b.close()
asyncio.run(main())
