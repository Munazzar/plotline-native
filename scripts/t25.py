# AI Plan written on-device: native engine (mock bridge → real arm64 llama-server under qemu) with JSON-schema constrained output.
import asyncio,json,re
from playwright.async_api import async_playwright
MOCK=re.search(r'MOCK=r"""(.*?)"""',open('/home/claude/scripts/t24.py').read(),re.S).group(1)
ok=lambda c,m:print(('PASS ' if c else 'FAIL ')+m)
async def main():
  async with async_playwright() as p:
    b=await p.chromium.launch();ctx=await b.new_context(viewport={'width':390,'height':844});await ctx.add_init_script(MOCK)
    pg=await ctx.new_page();errs=[];pg.on('pageerror',lambda e:errs.append(str(e)))
    await pg.goto('http://localhost:8767/index.html');await pg.wait_for_timeout(600);await pg.fill('input[name=name]','M');await pg.click('button[value=empty]');await pg.wait_for_timeout(600)
    await pg.evaluate("S.settings.tourOffered=true;document.querySelector('.tprompt')?.remove();insSet().llm='tiny';go('ai')");await pg.wait_for_timeout(800)
    ok(await pg.evaluate("!!document.querySelector('[data-act=aiGen]')"),'Create my plan button offered')
    await pg.fill('#aiText','I want to run a 5K and save money for a trip.');await pg.click('[data-act=aiGen]')
    await pg.wait_for_function("!AIG.busy",timeout=400000)
    r=await pg.evaluate("({out:AIG.out.slice(0,160),len:AIG.out.length,err:AIG.err,plan:!!AI.plan,errs:AI.errs.slice(0,3),rf:window.__lastBody&&window.__lastBody.response_format&&window.__lastBody.response_format.type,mt:window.__lastBody.max_tokens,tries:AIG.try})");print(r)
    out=await pg.evaluate("AIG.out");ok(re.match(r'\{\s*"format"\s*:\s*"plotline-plan"\s*,\s*"version"\s*:\s*1\s*,\s*"mode"\s*:\s*"create"\s*,\s*"goals"\s*:\s*\[\s*\{\s*"id"\s*:\s*"new-\d+"',out) is not None,'grammar forces the plan format (random test model)')
    ok(r['rf']=='json_schema' and r['mt']==2200,'request carries the JSON schema')
    ok(r['plan'] or (r['errs'] and r['err']),'result goes to review or shows the checks')
    await pg.screenshot(path='/home/claude/scripts/x-aigen-done.png',full_page=True)
    print('errors',errs);await b.close()
asyncio.run(main())
