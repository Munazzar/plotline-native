# Reactions (1.8.3): emoji + quick messages, live banner on any page, chip badge, Today row, 50/day cap on both ends,
# quick-message editing, Android decryption interop (E2E.java), and the new placeholder email.
import asyncio,json,subprocess,os
import t37
from t37 import ok,device,CIRC,MS,arr,sval
from playwright.async_api import async_playwright
async def main():
  async with async_playwright() as p:
    b=await p.chromium.launch()
    A=await device(b,'Ann','a',True)
    hid=await A.evaluate("(()=>{const h=normHabit({id:uid(),title:'Gym',icon:'💪',startDate:addDays(-5)});S.habits.push(h);save();return h.id})()")
    await A.evaluate(f"go('habit/{hid}');ACT.shareHabit({{id:'{hid}'}})");await A.wait_for_timeout(800);await A.click('[data-act=shTold]');await A.wait_for_timeout(300)
    ok(await A.evaluate("document.querySelector('form[data-form=shareGo] input[name=emails]').placeholder")=='address@gmail.com','share form placeholder is address@gmail.com')
    await A.click('.sh-m:has(input[value=watch])');await A.fill('form[data-form=shareGo] input[name=emails]','b@example.com');await A.click('form[data-form=shareGo] .btn.pri');await A.wait_for_timeout(1500)
    sid=await A.evaluate("shares()[0].id");await A.evaluate("closeSheet()")
    B=await device(b,'Ben','b',True)
    await B.evaluate("SHC.t=0;shRefresh(false)");await B.wait_for_timeout(1200);await A.evaluate("SHC.t=0;shRefresh(false)");await A.wait_for_timeout(1200);await B.evaluate("SHC.t=0;shRefresh(false)");await B.wait_for_timeout(1200)
    await B.evaluate(f"shJoin(shById('{sid}'))");await B.wait_for_timeout(1800)
    bh=await B.evaluate(f"shById('{sid}').local.id")
    await B.evaluate("go('goals')");await B.wait_for_timeout(500)
    await B.evaluate("shPollReacts()");await B.wait_for_timeout(1200)  # baseline
    # A reacts with a quick message
    await A.evaluate(f"ACT.shOpen({{id:'{sid}'}})");await A.wait_for_timeout(1500)
    await A.click('#sheet [data-act=shReact]');await A.wait_for_timeout(400)
    t=await A.evaluate("document.querySelector('#sheet').textContent")
    ok(all(e in t for e in ['👏','🔥','💪','❤️'])and 'Proud of you' in t and '50 of 50 left today' in t,'picker: several emoji + quick messages + daily count')
    await A.click('.rx-m:has-text("Proud of you")');await A.wait_for_timeout(1500)
    ok(await A.evaluate("!!document.querySelector('#sheet.on [data-shg]')"),'after sending, back in the group view')
    await A.wait_for_timeout(500);t=await A.evaluate("document.querySelector('#sheet').textContent");ok('You → Ben' in t and 'Proud of you' in t,'sender sees it in the group’s reactions feed')
    # B is on another page: live banner within ~20 s
    bt=''
    for _ in range(60):
      bt=await B.evaluate("(()=>{const e=document.querySelector('#rxb.on');return e?e.textContent:''})()")
      if bt:break
      await B.wait_for_timeout(500)
    ok('Ann' in bt and 'Proud of you' in bt and 'Gym' in bt,'B: banner on another page with who, what and which item: '+bt[:80])
    await B.evaluate(f"go('habits')");await B.wait_for_timeout(500);ok(await B.evaluate("!!document.querySelector('.sh-chip .sh-dot')")or await B.evaluate(f"rxUnread('{sid}')>0"),'B: unread count kept for the item’s Shared chip')
    await B.screenshot(path='/home/claude/scripts/x-rx-banner.png')
    await B.evaluate("go('today')");await B.wait_for_timeout(600)
    ok(await B.evaluate("!!document.querySelector('.sh-today .rx-row')"),'B: unread reaction on Today')
    await B.evaluate(f"ACT.shOpen({{id:'{sid}'}})");await B.wait_for_timeout(1800)
    ok(await B.evaluate(f"rxUnread('{sid}')===0"),'B: opening the group marks them read')
    await B.screenshot(path='/home/claude/scripts/x-rx-group.png')
    # live inside an open group view: A sends an emoji, B's open view updates within ~6 s
    await A.click('#sheet [data-act=shReact]');await A.wait_for_timeout(300);await A.click('.rx-b[data-e="🎉"]');await A.wait_for_timeout(8000)
    t=await B.evaluate("document.querySelector('#sheet').textContent");ok('🎉' in t,'B: open group view shows the new 🎉 by itself')
    # sender cap: 50 a day
    await A.evaluate(f"(()=>{{const x=shById('{sid}');x.myCheers=[...Array(50)].map((_,i)=>({{id:'z'+i,to:'b@example.com',emoji:'👏',m:'',t:Date.now()-i*1000}}));save()}})()")
    await A.evaluate(f"closeSheet();ACT.shReact({{id:'{sid}',to:'b@example.com',nm:'Ben'}})");await A.wait_for_timeout(400)
    t=await A.evaluate("document.querySelector('#sheet').textContent");dis=await A.evaluate("[...document.querySelectorAll('.rx-b,.rx-m')].every(b=>b.disabled)")
    ok('That’s 50 for today' in t and dis,'sender: after 50 today, the picker is closed until tomorrow')
    n0=await A.evaluate(f"(shById('{sid}').cheerOut||[]).length");await A.evaluate("RXT={id:'%s',to:'b@example.com',nm:'Ben'};ACT.shReactGo({e:'🔥'})"%sid);await A.wait_for_timeout(300)
    ok(await A.evaluate(f"(shById('{sid}').cheerOut||[]).length")==n0,'sender: a 51st is refused even if called directly')
    # receiver cap: a modified client writes 70 today → B counts 50
    await A.evaluate(f"(async()=>{{const x=shById('{sid}');await fsSetMe(x,{{status:'joined',progress:{{}},cheers:[...Array(70)].map((_,i)=>({{id:'y'+i,to:'b@example.com',emoji:'🔥',m:'',t:Date.now()-i*500}}))}})}})()");await A.wait_for_timeout(800)
    await B.evaluate("closeSheet();shPollReacts()");await B.wait_for_timeout(1500)
    ok(await B.evaluate("reactsForMe().filter(r=>r.fe==='a@example.com'&&dayOf(r.t)===ymd()).length")==50,'receiver: only 50 a day from one person count')
    # quick messages are editable, no free text elsewhere
    await B.evaluate("go('settings/share')");await B.wait_for_timeout(600)
    ok(await B.evaluate("document.querySelector('#view').textContent.includes('Up to 50 a day')"),'Settings → Sharing explains reactions and the limit')
    await B.click('[data-act=shQuickEdit]');await B.wait_for_timeout(300);await B.fill('form[data-form=shQuick] input[name=q0]','Walk after dinner?');await B.click('form[data-form=shQuick] .btn.pri');await B.wait_for_timeout(400)
    ok(await B.evaluate("quickMsgs()[0]==='Walk after dinner?'"),'quick messages saved')
    # Android hand-off carries joined keys + what was already shown
    cfg=await B.evaluate("""(()=>{const s=fbGet()||{},id=S.ident||{};return{joined:shares().filter(x=>x.fs&&x.status==='joined'&&x.k).map(x=>({cid:x.id,k:x.k,title:String(x.title||'').slice(0,60)})),rx:Object.fromEntries(Object.entries(shset().rx||{}).map(([k,v])=>[k,v.t||0]))}})()""")
    src=open('/home/claude/plotline.html').read();ok('joined:shares().filter' in src[src.index('function fbNative'):src.index('async function shAllow')],'fbNative sends joined keys')
    ok(cfg and any(j['cid']==sid and j['k'] and j['title']=='Gym' for j in cfg['joined']) and sid in cfg['rx'],'Android gets joined item keys and the seen marker')
    # E2E.java decrypts what the page encrypted (reactions in the background notification)
    enc=await A.evaluate(f"encJ(shById('{sid}').k,{{name:'Ann',cheers:[{{to:'b@example.com',emoji:'🔥',m:'Proud of you',t:1}}]}})");k=await A.evaluate(f"shById('{sid}').k")
    os.makedirs('/tmp/claude-0/jt/com/munazzar/plotline',exist_ok=True)
    open('/tmp/claude-0/jt/com/munazzar/plotline/RxT.java','w').write('package com.munazzar.plotline;public class RxT{public static void main(String[] a)throws Exception{System.out.print(new String(E2E.unseal(E2E.b64d(a[0]),a[1]),"UTF-8"));}}')
    subprocess.run(['javac','-cp','/tmp/claude-0/cls','-d','/tmp/claude-0/jt','/tmp/claude-0/jt/com/munazzar/plotline/RxT.java'],check=True,capture_output=True)
    out=subprocess.run(['java','-cp','/tmp/claude-0/cls:/tmp/claude-0/jt','com.munazzar.plotline.RxT',k,enc],capture_output=True,text=True).stdout
    ok('Proud of you' in out,'Android (E2E.java) decrypts a reaction the page encrypted')
    print('errors',A.errs+B.errs);ok(not(A.errs+B.errs),'no page errors')
    print(f'{t37.ok_n[0]} passed, {t37.ok_n[1]} failed');await b.close()
asyncio.run(main())
