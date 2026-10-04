# 1.11: Home, AI plan habits, start page, insights, ask-about, workout places + Health Connect notes
import asyncio,json
from playwright.async_api import async_playwright
PLAN={"format":"plotline-plan","version":1,"mode":"create","goals":[{"id":"new-1","action":"add","parent":None,"title":"Calmer mornings","why":"","area":"health","horizon":"quarter","priority":1,"startInDays":0,"targetInDays":90,"steps":[{"id":"new","title":"Put the phone outside the bedroom","dueInDays":1,"dueTime":None,"reminder":"none","note":""}],"linksTo":[]}],
 "habits":[{"title":"Morning walk 15 minutes","icon":"🚶","kind":"build","freq":"daily","days":[],"times":0,"target":1,"unit":"","part":"morning","time":"07:00","why":"Fresh air first","goal":"new-1"},{"title":"No phone in bed","icon":"📵","kind":"quit","freq":"daily","days":[],"times":0,"target":1,"unit":"","part":"evening","time":None,"why":"","goal":None}]}
async def main():
  async with async_playwright() as p:
    b=await p.chromium.launch()
    for W in [390,1280]:
      pg=await (await b.new_context(viewport={'width':W,'height':860})).new_page();errs=[];pg.on('pageerror',lambda e:errs.append(str(e)))
      await pg.add_init_script("window.PlotlineNative={openHealthConnect(){window.__hco=1},liveShareOn(){return false},liveShare(v){window.__live=v}}")
      await pg.goto('http://localhost:8765/index.html');await pg.wait_for_timeout(700)
      await pg.fill('input[name=name]','Munazzar');await pg.click('button[value=demo]');await pg.wait_for_timeout(700)
      await pg.evaluate("S.settings.tourOffered=true;S.settings.setupDone=true;S.settings.theme='mocha';applyTheme();document.querySelector('.tprompt')?.remove();go('today')");await pg.wait_for_timeout(800)
      ok=lambda n,v:print(W,('PASS ' if v else 'FAIL ')+n)
      ok('nav says Home',await pg.evaluate("[...document.querySelectorAll('[data-nav=today] span')].some(s=>s.textContent==='Home')"))
      ok('quick actions',await pg.evaluate("document.querySelectorAll('.hmq-b').length===7"))
      ok('your day card',await pg.evaluate("!!document.querySelector('.hday')"))
      ok('Plan with AI card explains itself',await pg.evaluate("!!document.querySelector('.aip p')"))
      await pg.evaluate("document.querySelectorAll('#view .rv').forEach(e=>e.classList.add('in'))");await pg.wait_for_timeout(400);await pg.screenshot(path=f'x-home-{W}.png',full_page=True)
      ins=await pg.evaluate("homeInsights().map(x=>x.e+' '+x.t)");print(W,'insights',ins)
      # AI plan with habits
      await pg.evaluate("ACT.aiStart()");await pg.wait_for_timeout(500)
      await pg.evaluate(f"AI.reply={json.dumps('Here you go\n```json\n'+json.dumps(PLAN)+'\n```')};render(false)");await pg.wait_for_timeout(300)
      await pg.click('[data-act=parseAI]');await pg.wait_for_timeout(500)
      ok('plan with habits validates',await pg.evaluate("!!AI.plan&&!AI.errs.length"))
      ok('preview lists habits',await pg.evaluate("document.querySelectorAll('[data-act=pvHab]').length===2"))
      if W==390:await pg.evaluate("document.querySelectorAll('#view .rv').forEach(e=>e.classList.add('in'))");await pg.screenshot(path='x-aiplan.png',full_page=True)
      n0=await pg.evaluate("S.habits.length");await pg.click('[data-act=doImport]');await pg.wait_for_timeout(700)
      ok('habits created and linked',await pg.evaluate(f"S.habits.length==={n0}+2&&!!S.habits.find(h=>h.title==='Morning walk 15 minutes').goalId&&S.habits.find(h=>h.title==='No phone in bed').kind==='quit'"))
      bad=await pg.evaluate("validatePlan({...JSON.parse('"+json.dumps(PLAN).replace("'","\\'")+"'),habits:[{title:'x'}]},'create').length>0");ok('bad habit rejected',bad)
      # start page
      hid=await pg.evaluate("S.habits[0].id");await pg.evaluate(f"S.settings.landing='habit/{hid}';history.replaceState(null,'','#/');landHere();route()");await pg.wait_for_timeout(500)
      ok('start page opens a habit',await pg.evaluate(f"location.hash==='#/habit/{hid}'"))
      ok('ask about this button',await pg.evaluate("!!document.querySelector('.crumb [data-act=askAbout]')"))
      ok('habit page bar fits',await pg.evaluate("[...document.querySelectorAll('.crumb .ph-r > *')].every(e=>e.getBoundingClientRect().right<=innerWidth+1)"))
      await pg.evaluate("S.settings.landing='';go('settings/look')");await pg.wait_for_timeout(500);ok('start page setting shown',await pg.evaluate("!!document.querySelector('.lp-cur')"))
      # workouts: gym place counts; Health Connect empty note
      await pg.evaluate("S.settings.places=[{id:'p1',name:'Gym',emoji:'🏋️',lat:1,lng:1,r:100},{id:'p2',name:'Home',emoji:'🏠',lat:1,lng:1,r:100}];const L=actLog();L[ymd()]={s:500,p:{p1:45,p2:300}};NATIVE.activityLoad=()=>setTimeout(()=>__act(JSON.stringify({ok:{steps:true,hc:true,hcSupported:true,screen:true,loc:true,bg:true},hc:{steps:3},days:{[ymd()]:{steps:500,pl:{p1:45,p2:300}}},work:[],top:[],inside:{}})),30);go('activity')")
      await pg.wait_for_timeout(700)
      ok('gym time counts as workout',await pg.evaluate("document.querySelector('#act-work .act-big').textContent.trim()==='45m'"))
      ok('home is not a workout',await pg.evaluate("!wkPlace(places()[1])&&wkPlace(places()[0])"))
      ok('Health Connect empty note',await pg.evaluate("!!document.querySelector('#act-work .act-diag')"))
      await pg.evaluate("document.querySelectorAll('#view .rv').forEach(e=>e.classList.add('in'))");await pg.wait_for_timeout(300)
      if W==390:await pg.screenshot(path='x-act2.png',full_page=True)
      print(W,'errors',errs)
    await b.close()
asyncio.run(main())
