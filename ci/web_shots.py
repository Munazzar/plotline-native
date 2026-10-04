# Opens the 1.13 web build (default https://munazzar.github.io/plotline/) at phone size, loads the sample data,
# saves the resulting state to ci/out/state.json (the emulator gets the same data), then screenshots every
# scenario page by page (one phone screen at a time) into ci/out/web/<name>-<k>.png, plus a text outline.
import asyncio, json, os, sys
from playwright.async_api import async_playwright
sys.path.insert(0, os.path.dirname(__file__))
from scenarios import scenarios, THREAD_JS

URL = os.environ.get('WEB_URL', 'https://munazzar.github.io/plotline/')
OUT = os.path.join(os.path.dirname(__file__), 'out')
W, H = 412, 915          # Pixel 6 in CSS px (1080x2400 @ 2.625)
STEP = 560               # scroll per page, matches the emulator swipe (1470 px / 2.625)

OUTLINE = """(()=>{const out=[];const skip=new Set(['SVG','PATH','CIRCLE','LINE','RECT','G','POLYGON','ELLIPSE','DEFS','STOP','LINEARGRADIENT','RADIALGRADIENT','TEXT','TITLE','STYLE']);
function w(n,d){if(d>14)return;for(const c of n.children){if(skip.has(c.tagName.toUpperCase()))continue;const cs=getComputedStyle(c);if(cs.display==='none'||cs.visibility==='hidden')continue;
 const own=[...c.childNodes].filter(x=>x.nodeType===3).map(x=>x.textContent.trim()).join(' ').trim();
 const cls=(c.getAttribute('class')||'').split(/\\s+/).slice(0,3).join('.');const act=c.dataset&&c.dataset.act?(' @'+c.dataset.act):'';const aria=c.getAttribute('aria-label');
 if(own||act||/^(H1|H2|H3|H4|BUTTON|A|INPUT|SELECT|SECTION|HEADER|TEXTAREA)$/.test(c.tagName)||/(card|li|chip|seg|panel|data|st-h|ph)/.test(cls))out.push('  '.repeat(d)+c.tagName.toLowerCase()+(cls?'.'+cls:'')+act+(aria&&!own?' ['+aria+']':'')+(own?' "'+own.slice(0,70)+'"':''));
 w(c,d+1)}}
w(document.querySelector('#view')||document.body,0);return out.join('\\n')})()"""


async def main():
    os.makedirs(OUT + '/web', exist_ok=True)
    async with async_playwright() as p:
        b = await p.chromium.launch()
        ctx = await b.new_context(viewport={'width': W, 'height': H}, device_scale_factor=2.625, is_mobile=True, has_touch=True)
        pg = await ctx.new_page()
        errs = []
        pg.on('pageerror', lambda e: errs.append(str(e)))
        await pg.goto(URL); await pg.wait_for_timeout(2500)
        # first run: name + "try with sample data"
        if await pg.locator('input[name=name]').count():
            await pg.fill('input[name=name]', 'M')
            if await pg.locator('button[value=demo]').count():
                await pg.click('button[value=demo]')
            else:
                await pg.get_by_text('sample', exact=False).first.click()
            await pg.wait_for_timeout(2000)
        await pg.evaluate("typeof closeSheet==='function'&&closeSheet()")
        await pg.wait_for_timeout(600)
        await pg.evaluate(THREAD_JS); await pg.wait_for_timeout(300)
        open(OUT + '/web/version.txt', 'w').write(URL + '  APP_VER=' + str(await pg.evaluate("typeof APP_VER!=='undefined'?APP_VER:'?'")) + '\n')
        state = json.loads(await pg.evaluate("JSON.stringify(S)"))
        json.dump(state, open(OUT + '/state.json', 'w'))
        for name, route, steps in scenarios(state):
            await pg.evaluate(f"closeSheet&&closeSheet();go('{route}')"); await pg.wait_for_timeout(1400)
            for web, _nat in steps:
                try:
                    kind, arg = web.split(':', 1)
                    if kind == 'text': await pg.locator('#view').get_by_text(arg, exact=True).first.click(timeout=4000)
                    elif kind == 'css': await pg.locator(arg).first.click(timeout=4000)
                    else: await pg.evaluate(arg)
                except Exception as e: print('web step failed', name, web, str(e)[:120])
                await pg.wait_for_timeout(900)
            # reveal-on-scroll content: mark everything shown so screenshots match a scrolled-through page
            await pg.evaluate("document.querySelectorAll('.rv').forEach(x=>x.classList.add('in'))")
            open(f'{OUT}/web/{name}.txt', 'w').write(await pg.evaluate(OUTLINE))
            total = await pg.evaluate("Math.max(document.documentElement.scrollHeight,document.body.scrollHeight)")
            k = 0; y = 0
            while k < 8:
                await pg.evaluate(f"window.scrollTo(0,{y})"); await pg.wait_for_timeout(500)
                await pg.screenshot(path=f'{OUT}/web/{name}-{k}.png')
                k += 1; y += STEP
                if y >= total - H + STEP: break
            await pg.evaluate("window.scrollTo(0,0)")
        open(OUT + '/web/errors.txt', 'w').write('\n'.join(errs))
        await b.close()

asyncio.run(main())
