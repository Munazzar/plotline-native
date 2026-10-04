# Ask relevance: specific questions only carry the goals/notes they're about; broad check-ins keep the whole picture.
import asyncio
from playwright.async_api import async_playwright
ok=lambda c,m:print(('PASS ' if c else 'FAIL ')+m)
async def ask(pg,q):
    await pg.evaluate('ACT.askNew()');await pg.wait_for_timeout(150)
    await pg.fill('#askIn',q);await pg.click('[data-act=askSend]');await pg.wait_for_function("IN.res&&!IN.busy")
    return await pg.evaluate("({p:IN.res.prompt||'',n:IN.res.srcs.length,labels:IN.res.srcs.map(d=>d.label),off:!!IN.res.offOnly})")
async def main():
  async with async_playwright() as p:
    b=await p.chromium.launch();pg=await b.new_page(viewport={'width':390,'height':844});errs=[];pg.on('pageerror',lambda e:errs.append(str(e)))
    await pg.goto('http://localhost:8765/index.html');await pg.wait_for_timeout(600)
    await pg.fill('input[name=name]','M');await pg.click('button[value=demo]');await pg.wait_for_timeout(700)
    await pg.evaluate("S.settings.tourOffered=true;document.querySelector('.tprompt')?.remove();insSet().engine='copy';go('ask')");await pg.wait_for_timeout(600)
    titles=await pg.evaluate("S.goals.filter(g=>g.status==='active').map(g=>g.title)");print(titles)
    r=await ask(pg,'What should I cook for dinner tonight to eat healthier?');print(r['n'],r['labels'][:4])
    body=r['p'].split('\nABOUT ME\n')[1].split('QUESTION')[0];print(body[:700])
    unrelated=[t for t in titles if t in body]
    ok(len(unrelated)<=2,'cooking question does not dump all goals (%d of %d shown)'%(len(unrelated),len(titles)))
    ok('Answer exactly the QUESTION' in r['p'],'prompt tells the model to answer only the question')
    r2=await ask(pg,'How am I doing this week?');b2=r2['p'].split('\nABOUT ME\n')[1]
    ok(sum(t in b2 for t in titles)==len(titles) and r2['n']>0,'broad check-in keeps the whole plan and recent notes')
    g=titles[0];r3=await ask(pg,'Any tips for '+g+'?');ok(g in r3['p'].split('\nABOUT ME\n')[1],'specific goal question includes that goal')
    print('errors',errs);await b.close()
asyncio.run(main())
