# Snoozes from replies are visible on the item, history is a quiet accordion per item, any emoji can be an icon, no model name on the plan card.
import asyncio,json,re
from playwright.async_api import async_playwright
ok=lambda c,m:print(('PASS ' if c else 'FAIL ')+m)
MOCK=re.search(r'MOCK=r"""(.*?)"""',open('/home/claude/scripts/t30.py').read(),re.S).group(1)
MOCK=MOCK.replace("notifState:()=>JSON.stringify({muteUntil:window.__mute,missed:0}),","notifState:()=>JSON.stringify({muteUntil:window.__mute,missed:0,snoozes:window.__snz||{}}),notifLog:k=>JSON.stringify((window.__log||[]).filter(e=>e.key===k||k.startsWith('g:')&&e.key.startsWith('s:'+k.slice(2)+'/'))),notifCancelSnooze:n=>{delete window.__snz[n];window.__cancelled=n},exactAlarms:()=>false,openExactAlarms:()=>{window.__exact=1},")
async def main():
  async with async_playwright() as p:
    b=await p.chromium.launch();ctx=await b.new_context(viewport={'width':390,'height':844});await ctx.add_init_script(MOCK)
    pg=await ctx.new_page();errs=[];pg.on('pageerror',lambda e:errs.append(str(e)))
    await pg.add_init_script('window.__allOpen=1');await pg.goto('http://localhost:8765/index.html');await pg.wait_for_timeout(600)
    await pg.fill('input[name=name]','M');await pg.click('button[value=demo]');await pg.wait_for_timeout(700)
    hid=await pg.evaluate("S.settings.tourOffered=true;document.querySelector('.tprompt')?.remove();S.habits.find(h=>h.kind==='build').id")
    at=await pg.evaluate("(()=>{const d=new Date();d.setHours(17,0,0,0);if(d<Date.now())d.setDate(d.getDate()+1);return d.getTime()})()")
    now=await pg.evaluate("Date.now()")
    await pg.evaluate(f"""window.__snz={{'1003':{{at:{at},key:'h:{hid}',title:'Water'}}}};window.__log=[
      {{t:{now},key:'h:{hid}',kind:'reply-ai',title:'Water',text:'remind me at 5pm',res:'⏰ I’ll remind you again at 5:00 PM'}},
      {{t:{now-60000},key:'h:{hid}',kind:'sent',title:'Water',text:'Time for this habit'}}];NSC.t=0;go('habit/{hid}')""");await pg.wait_for_timeout(600)
    lab=await pg.evaluate("document.querySelector('.crumb .bell').textContent");print(lab);ok('Again' in lab and '5:00' in lab,'habit bell shows the snooze from the reply')
    ok(await pg.evaluate("!!document.querySelector('details.nhist')&&!document.querySelector('details.nhist').open"),'history is a closed, quiet accordion')
    await pg.click('details.nhist summary');await pg.wait_for_timeout(200)
    t=await pg.evaluate("[...document.querySelectorAll('.nh-list li')].map(l=>l.textContent)");print(t);ok(len(t)==2 and 'remind me at 5pm' in t[0],'history lists newest first with the reply and what was done')
    await pg.evaluate("document.querySelectorAll('.rv').forEach(e=>e.classList.add('in'))");await pg.screenshot(path='/home/claude/scripts/x-hist.png',full_page=True)
    await pg.click('.crumb .bell');await pg.wait_for_timeout(300)
    ok(await pg.evaluate("document.querySelector('#remBody .rm-snz b').textContent.includes('Reminds you again')"),'reminder sheet shows the snooze')
    ok(await pg.evaluate("document.querySelector('.rm-next b').textContent.includes('5:00')"),'next reminder includes the snooze')
    await pg.screenshot(path='/home/claude/scripts/x-snz.png')
    await pg.click('[data-act=snzCancel]');await pg.wait_for_timeout(300)
    ok(await pg.evaluate("window.__cancelled===1003&&!document.querySelector('#remBody .rm-snz:not(.mute) [data-act=snzCancel]')"),'snooze can be cancelled')
    ok(await pg.evaluate("!!document.querySelector('[data-act=exactOn]')"),'asks to allow on-time reminders when Android would delay them')
    await pg.evaluate("closeSheet()")
    # emoji icon
    await pg.evaluate(f"ACT.editHabit({{id:'{hid}'}})");await pg.wait_for_timeout(400)
    await pg.click('.hown-in');await pg.keyboard.type('🦄🦄x');await pg.wait_for_timeout(100)
    v=await pg.evaluate("document.querySelector('.hown-in').value");ok(v=='🦄','typing keeps one emoji ('+v+')')
    await pg.click('form[data-form=saveHabit] .btn.pri');await pg.wait_for_timeout(400)
    ok(await pg.evaluate(f"H('{hid}').icon")=='🦄','typed emoji saved as the habit icon')
    # plan card without model name
    await pg.evaluate("insSet().engine='local';go('ai')");await pg.wait_for_timeout(500)
    card=await pg.evaluate("(document.querySelector('.aig')||{}).textContent||''");print(card[:120]);ok('Llama' not in card and 'Qwen' not in card and 'Phi' not in card,'plan card does not repeat the model name')
    print('errors',errs);await b.close()
asyncio.run(main())
