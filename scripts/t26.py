# Ask scope: off-topic questions are answered by the app itself; plan + coaching questions go through.
import asyncio,json
from playwright.async_api import async_playwright
ON=['Why do I keep skipping my run?','How is my emergency fund going?','When did I start reading before bed?','How do I build a habit that sticks?','I feel tired and unmotivated this week','What should I focus on tomorrow?','How can I stop procrastinating?','Summarize my week','Am I spending enough time with my family?','Tips for a better morning routine']
OFF=['What is the capital of France?','Write me a poem about cars','Write a python script to sort a list','Who won the world cup in 2022?','Translate hello into Spanish','What is the weather today?','Give me a pasta recipe','Explain quantum entanglement','bitcoin price prediction','Tell me a joke','How do I change a car tire?','How can I make pasta?',"What's my IP address?"]
FOLLOW=['why?','and after that?','what about the second one']
ok=lambda c,m:print(('PASS ' if c else 'FAIL ')+m)
async def main():
  async with async_playwright() as p:
    b=await p.chromium.launch();pg=await b.new_page(viewport={'width':390,'height':844});errs=[];pg.on('pageerror',lambda e:errs.append(str(e)))
    await pg.goto('http://localhost:8765/index.html');await pg.wait_for_timeout(500);await pg.fill('input[name=name]','M');await pg.click('button[value=demo]');await pg.wait_for_timeout(600)
    r=await pg.evaluate("([on,off,fo])=>({on:on.map(q=>[q,askGate(q,false)]),off:off.map(q=>[q,askGate(q,false)]),fo:fo.map(q=>[q,askGate(q,true)]),offFollow:askGate('write me a poem about cars',true)})",[ON,OFF,FOLLOW])
    bad=[q for q,v in r['on'] if not v];ok(not bad,'plan and coaching questions allowed '+str(bad))
    bad=[q for q,v in r['off'] if v];ok(not bad,'off-topic questions blocked '+str(bad))
    ok(all(v for q,v in r['fo']) and not r['offFollow'],'short follow-ups allowed, off-topic follow-up blocked')
    await pg.evaluate("S.settings.tourOffered=true;document.querySelector('.tprompt')?.remove();Object.assign(insSet(),{engine:'copy'});go('ask')");await pg.wait_for_timeout(700)
    await pg.fill('#askIn','What is the capital of France?');await pg.click('[data-act=askSend]');await pg.wait_for_timeout(400)
    r=await pg.evaluate("({a:IN.res&&IN.res.turns[0].a,busy:IN.busy,save:!!document.querySelector('[data-act=insSave]')})");print(r)
    ok(r['a'].startswith('I can only help') and not r['busy'] and not r['save'],'blocked question gets the fixed reply instantly, no model, no save button')
    await pg.fill('#askIn','How is my emergency fund going?');await pg.click('[data-act=askSend]');await pg.wait_for_function("IN.res&&!IN.busy&&!IN.res.offOnly")
    p2=await pg.evaluate("IN.res.prompt");ok('I can only help with your goals' in p2 and 'trivia' in p2,'prompt carries the scope rule')
    ok(await pg.evaluate("answerOff('Paris is the capital of France.')===true&&answerOff('Your run streak is 4 days [2]. Next step: run Tuesday.')===false"),'answer check catches an off-topic reply')
    await pg.screenshot(path='/home/claude/scripts/x-scope.png')
    print('errors',errs);await b.close()
asyncio.run(main())
