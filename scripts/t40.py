# Shared panel on the habit / goal page (1.8.4): who it's shared with, their progress today, one-tap reactions.
import asyncio
import t37
from t37 import ok,device
from playwright.async_api import async_playwright
async def main():
  async with async_playwright() as p:
    b=await p.chromium.launch()
    A=await device(b,'Ann','a',True)
    hid=await A.evaluate("(()=>{const h=normHabit({id:uid(),title:'Gym',icon:'💪',startDate:addDays(-5)});S.habits.push(h);save();return h.id})()")
    await A.evaluate(f"go('habit/{hid}');ACT.shareHabit({{id:'{hid}'}})");await A.wait_for_timeout(800);await A.click('[data-act=shTold]');await A.wait_for_timeout(300)
    await A.click('.sh-m:has(input[value=watch])');await A.fill('form[data-form=shareGo] input[name=emails]','b@example.com');await A.click('form[data-form=shareGo] .btn.pri');await A.wait_for_timeout(1500)
    sid=await A.evaluate("shares()[0].id");await A.evaluate("closeSheet()");await A.wait_for_timeout(1200)
    t=await A.evaluate("(document.querySelector('[data-shp]')||{}).textContent||''")
    ok('Shared' in t and 'Nobody has joined yet' in t,'owner: panel under the hero says nobody joined yet')
    B=await device(b,'Ben','b',True)
    await B.evaluate("SHC.t=0;shRefresh(false)");await B.wait_for_timeout(1200);await A.evaluate("SHC.t=0;shRefresh(false)");await A.wait_for_timeout(1200);await B.evaluate("SHC.t=0;shRefresh(false)");await B.wait_for_timeout(1200)
    await B.evaluate(f"shJoin(shById('{sid}'))");await B.wait_for_timeout(1800);bh=await B.evaluate(f"shById('{sid}').local.id")
    await A.evaluate(f"hSetVal(H('{hid}'),ymd(),1);save()");await A.wait_for_timeout(6500)
    await B.evaluate("S.settings.theme='daylight';applyTheme();go('habit/%s')"%bh);await B.wait_for_timeout(2000)
    t=await B.evaluate("(document.querySelector('[data-shp]')||{}).textContent||''")
    ok('Ann' in t and 'done today' in t,'member: panel shows Ann and that she did it today: '+t[:90])
    n=await B.evaluate("document.querySelectorAll('[data-shp] .shp-e').length");ok(n==5,'one-tap emoji + More right on the page')
    await A.evaluate("go('today')")
    await B.click('[data-shp] .shp-e[data-e="💪"]');await B.wait_for_timeout(1500)
    ok((await B.evaluate("location.hash")).endswith(bh) and not await B.evaluate("!!document.querySelector('#sheet.on')"),'sending stays on the page (no sheets)')
    await B.screenshot(path='/home/claude/scripts/x-shp-day.png')
    bt=''
    for _ in range(60):
      bt=await A.evaluate("(()=>{const e=document.querySelector('#rxb.on');return e?e.textContent:''})()")
      if bt:break
      await A.wait_for_timeout(500)
    ok('Ben' in bt and '💪' in bt,'Ann gets it live on Today')
    await B.click('[data-shp] .shp-e.more');await B.wait_for_timeout(500)
    await B.click('.rx-m:has-text("Proud of you")');await B.wait_for_timeout(1500)
    ok(not await B.evaluate("!!document.querySelector('#sheet.on')") and (await B.evaluate("location.hash")).endswith(bh),'More → message → back on the page')
    await A.evaluate(f"go('habit/{hid}')");await A.wait_for_timeout(9000)
    t=await A.evaluate("(document.querySelector('[data-shp]')||{}).textContent||''");ok('Ben' in t and 'Proud of you' in t,'owner panel shows Ben and his latest reaction: '+t[:100])
    await A.screenshot(path='/home/claude/scripts/x-shp-night.png')
    print('errors',A.errs+B.errs);ok(not(A.errs+B.errs),'no page errors')
    print(f'{t37.ok_n[0]} passed, {t37.ok_n[1]} failed');await b.close()
asyncio.run(main())
