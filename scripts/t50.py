# 1.12: Ask overlay, reactions behind the lock, compact week bar, Liquid Glass, Threads widget queue
import asyncio
from playwright.async_api import async_playwright
async def main():
  async with async_playwright() as p:
    b=await p.chromium.launch()
    for W in [390,1280]:
      pg=await (await b.new_context(viewport={'width':W,'height':860})).new_page();errs=[];pg.on('pageerror',lambda e:errs.append(str(e)))
      await pg.add_init_script("window.__Q=[];window.__W=null;window.PlotlineNative={widget(j){window.__W=JSON.parse(j)},widgetQueue(){const q=JSON.stringify(window.__Q);window.__Q=[];return q}}")
      await pg.goto('http://localhost:8765/index.html');await pg.wait_for_timeout(700)
      await pg.fill('input[name=name]','Munazzar');await pg.click('button[value=demo]');await pg.wait_for_timeout(700)
      await pg.evaluate("S.settings.tourOffered=true;S.settings.setupDone=true;document.querySelector('.tprompt')?.remove();S.settings.ins={...(S.settings.ins||{}),engine:'copy'};go('habit/'+S.habits[0].id)");await pg.wait_for_timeout(700)
      ok=lambda n,v:print(W,('PASS ' if v else 'FAIL ')+n)
      await pg.click('.crumb [data-act=askAbout]');await pg.wait_for_timeout(1500)
      ok('Ask AI opens an overlay on the same page',await pg.evaluate("AOV.open&&location.hash.startsWith('#/habit/')&&!!document.querySelector('#aov.on .ask-thread')"))
      await pg.screenshot(path=f'x-aov-{W}.png')
      # drag down to close
      box=await pg.evaluate("(()=>{const r=document.querySelector('.aov-grab').getBoundingClientRect();return[r.left+r.width/2-60,r.top+4]})()")
      await pg.mouse.move(box[0],box[1]);await pg.mouse.down();await pg.mouse.move(box[0],box[1]+80,steps=5);await pg.mouse.move(box[0],box[1]+200,steps=5);await pg.mouse.up();await pg.wait_for_timeout(600)
      ok('drag down closes it',await pg.evaluate("!AOV.open&&!document.querySelector('#aov')"))
      await pg.evaluate("go('today')");await pg.wait_for_timeout(500)
      await pg.click('.hmq-b[data-act=askGo]');await pg.wait_for_timeout(600)
      ok('quick action Ask opens overlay',await pg.evaluate("AOV.open"))
      await pg.click('#aov [data-act=aovFull]');await pg.wait_for_timeout(700)
      ok('open full page',await pg.evaluate("location.hash==='#/ask'&&!AOV.open"))
      # reactions wait for the lock
      await pg.evaluate("go('today')");await pg.wait_for_timeout(400)
      await pg.evaluate("LOCKED=true;RB.q=[{cid:'x',e:'🔥',from:'Zoya',fe:'z@x.com',m:'Go!',t:Date.now()}];reactShow()")
      ok('banner held while locked',await pg.evaluate("!document.querySelector('#rxb.on')&&RB.wait===1"))
      await pg.evaluate("document.body.insertAdjacentHTML('beforeend','<div class=lock></div>');unlockDone()");await pg.wait_for_timeout(900)
      ok('banner shows after unlock',await pg.evaluate("!!document.querySelector('#rxb.on')"))
      await pg.evaluate("RB.q=[];reactShow()")
      # journal week bar is one row
      await pg.evaluate("go('journal')");await pg.wait_for_timeout(500)
      ok('week bar one compact row',await pg.evaluate("(()=>{const r=document.querySelector('#jwBar').getBoundingClientRect();return r.height<80&&!document.querySelector('#jwBar').textContent.includes('entr')})()"))
      # widget payload + queue
      await pg.evaluate("S.threads=[{id:'t1',title:'App idea',link:null,status:'open',created:Date.now()-9e5,u:1,ups:[{id:'u1',t:Date.now()-9e5,k:'idea',x:'Voice logging',u:1}]}];save()");await pg.wait_for_timeout(600)
      ok('widget gets threads',await pg.evaluate("!!(__W&&__W.thr&&__W.thr[0].id==='t1')"))
      await pg.evaluate("__Q=[{k:'thr',id:'t1',x:'Said from the widget',kind:'prog',t:Date.now(),qid:'wq1'},{k:'thrnew',title:'Garden plans',nid:'wq2n',x:'Tomatoes by the fence',kind:'note',t:Date.now(),qid:'wq2'},{k:'thr',id:'wq2n',x:'Bought seeds',kind:'done',t:Date.now()+1,qid:'wq3'}];applyWidgetQueue()");await pg.wait_for_timeout(500)
      ok('widget update lands in the thread',await pg.evaluate("TH('t1').ups.some(u=>u.id==='wq1'&&u.k==='prog')"))
      ok('widget can start a thread and add to it',await pg.evaluate("!!TH('wq2n')&&TH('wq2n').ups.length===2"))
      await pg.evaluate("__Q=[{k:'thr',id:'t1',x:'Said from the widget',kind:'prog',t:Date.now(),qid:'wq1'}];applyWidgetQueue()")
      ok('no duplicates if applied twice',await pg.evaluate("TH('t1').ups.filter(u=>u.id==='wq1').length===1"))
      # glass themes
      for th in ['glass','glasslight']:
        await pg.evaluate(f"S.settings.theme='{th}';applyTheme();go('today')");await pg.wait_for_timeout(700)
        ok(th+' applies',await pg.evaluate(f"document.documentElement.dataset.theme==='{th}'"))
        await pg.evaluate("document.querySelectorAll('#view .rv').forEach(e=>e.classList.add('in'))");await pg.wait_for_timeout(400)
        await pg.screenshot(path=f'x-{th}-{W}.png')
      print(W,'errors',errs)
    await b.close()
asyncio.run(main())
async def folds():
  async with async_playwright() as p:
    b=await p.chromium.launch()
    for W in [390,1280]:
      pg=await (await b.new_context(viewport={'width':W,'height':860})).new_page();errs=[];pg.on('pageerror',lambda e:errs.append(str(e)))
      await pg.goto('http://localhost:8765/index.html');await pg.wait_for_timeout(700)
      await pg.fill('input[name=name]','M');await pg.click('button[value=demo]');await pg.wait_for_timeout(700)
      await pg.evaluate("S.settings.tourOffered=true;S.settings.setupDone=true;document.querySelector('.tprompt')?.remove();go('settings/look')");await pg.wait_for_timeout(700)
      ok=lambda n,v:print(W,('PASS ' if v else 'FAIL ')+n)
      n=await pg.evaluate("document.querySelectorAll('.set > .fold').length");o=await pg.evaluate("document.querySelectorAll('.set > .fold.open').length")
      ok(f'look & feel folds ({n} sections, {o} open)',n>=4 and o==1)
      await pg.click('.set > .fold:nth-child(3) > h3');await pg.wait_for_timeout(500)
      ok('tap opens a section',await pg.evaluate("document.querySelector('.set > .fold:nth-child(3)').classList.contains('open')"))
      await pg.evaluate("render(false)");await pg.wait_for_timeout(300)
      ok('stays open after re-render',await pg.evaluate("document.querySelector('.set > .fold:nth-child(3)').classList.contains('open')"))
      await pg.evaluate("document.querySelectorAll('#view .rv').forEach(e=>e.classList.add('in'))");await pg.screenshot(path=f'x-fold-{W}.png')
      await pg.evaluate("(()=>{const h=[...document.querySelectorAll('.set > .fold > h3')].find(x=>x.textContent.includes('Start page'));if(!h.parentElement.classList.contains('open'))h.click()})()");await pg.wait_for_timeout(500);await pg.click('.lp-cur');await pg.wait_for_timeout(600);await pg.screenshot(path=f'x-land-{W}.png')
      await pg.click('#sheet .lp-o[data-v=journal]');await pg.wait_for_timeout(500)
      ok('picker sets start page',await pg.evaluate("S.settings.landing==='journal'&&document.querySelector('.lp-cur b').textContent==='Journal'"))
      print(W,'errors',errs)
    await b.close()
asyncio.run(folds())
