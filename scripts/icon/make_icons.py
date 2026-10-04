# Renders the Plotline "map" logo to every icon size (Android adaptive fg/mono + legacy, web 192/512) with headless Chromium.
import asyncio
from playwright.async_api import async_playwright
A='/home/claude/apkbuild'
# constellation in a 40x40 box: centre node + area-coloured goal nodes, like the Map view
def mark(mono=False,text='#F2EEE6',acc='#FFB547'):
    # four timelines, like lanes in Habit Vista: each starts at its own time, runs bold through a streak,
    # and all meet today's line on the right, where one glows as the goal reached
    c=lambda col:'#fff' if mono else col
    T=31   # today
    lanes=[(8.5,13,'#46C99B',[18,26],[13]),(15.5,6,'#6B9BFF',[6,12.5],[18,23]),(22.5,10,'#B48CFF',[15,28.5],[10]),(29.5,4.5,'#FF8C6B',[11,19.5],[4.5,25])]
    s=''
    if not mono: s+=f'<defs><radialGradient id="g"><stop offset="0" stop-color="{acc}" stop-opacity=".6"/><stop offset="1" stop-color="{acc}" stop-opacity="0"/></radialGradient></defs><circle cx="{T}" cy="15.5" r="7.5" fill="url(#g)"/>'
    s+=f'<line x1="{T}" y1="3.5" x2="{T}" y2="34.5" stroke="{c(acc)}" stroke-opacity="{.9 if mono else .55}" stroke-width=".9" stroke-dasharray="1.6 1.6" stroke-linecap="round"/>'
    for y,x0,col,run,dots in lanes:
        s+=f'<line x1="{x0}" y1="{y}" x2="{T}" y2="{y}" stroke="{c(col)}" stroke-opacity="{.55 if mono else .38}" stroke-width="1.2" stroke-linecap="round"/>'
        s+=f'<line x1="{run[0]}" y1="{y}" x2="{run[1]}" y2="{y}" stroke="{c(col)}" stroke-width="2.4" stroke-linecap="round"/>'
        for x in dots: s+=f'<circle cx="{x}" cy="{y}" r="1.4" fill="{c(col)}"/>'
        if y!=15.5:
            s+=f'<circle cx="{T}" cy="{y}" r="1.75" fill="{c(col)}"/>'
    s+=f'<circle cx="{T}" cy="15.5" r="4.7" fill="none" stroke="{c(acc)}" stroke-opacity="{1 if mono else .55}" stroke-width=".85"/><circle cx="{T}" cy="15.5" r="2.9" fill="{c(acc)}"/>'
    return s
def svg(px,kind):
    if kind=='fg':   # 108dp canvas, art kept inside the 66dp safe zone
        return f'<svg xmlns="http://www.w3.org/2000/svg" width="{px}" height="{px}" viewBox="-14 -14 68 68">{mark()}</svg>'
    if kind=='mono':
        return f'<svg xmlns="http://www.w3.org/2000/svg" width="{px}" height="{px}" viewBox="-14 -14 68 68">{mark(True)}</svg>'
    if kind=='legacy':
        return f'<svg xmlns="http://www.w3.org/2000/svg" width="{px}" height="{px}" viewBox="0 0 40 40"><rect width="40" height="40" rx="9" fill="#0A0F1F"/><rect x="1" y="1" width="38" height="38" rx="8.5" fill="#141B33"/><g transform="translate(3 3) scale(.85)">{mark()}</g></svg>'
    if kind=='web':  # maskable: full bleed background, art in the centre 80%
        return f'<svg xmlns="http://www.w3.org/2000/svg" width="{px}" height="{px}" viewBox="0 0 40 40"><rect width="40" height="40" fill="#0A0F1F"/><g transform="translate(6 6) scale(.7)">{mark()}</g></svg>'
JOBS=[]
for d,f,l in [('mdpi',108,48),('hdpi',162,72),('xhdpi',216,96),('xxhdpi',324,144),('xxxhdpi',432,192)]:
    JOBS+= [(f'{A}/app/res/mipmap-{d}/ic_fg.png',f,'fg'),(f'{A}/app/res/mipmap-{d}/ic_mono.png',f,'mono'),(f'{A}/app/res/mipmap-{d}/ic_launcher.png',l,'legacy')]
JOBS+=[(f'{A}/web/icon-192.png',192,'web'),(f'{A}/web/icon-512.png',512,'web'),('/home/claude/scripts/icon/preview-512.png',512,'legacy')]
async def main():
    async with async_playwright() as p:
        b=await p.chromium.launch();pg=await b.new_page()
        for out,px,kind in JOBS:
            await pg.set_viewport_size({'width':px,'height':px})
            await pg.set_content(f'<html><body style="margin:0;background:transparent">{svg(px,kind)}</body></html>')
            await pg.screenshot(path=out,omit_background=True,clip={'x':0,'y':0,'width':px,'height':px})
        await b.close()
    print('icons',len(JOBS))
asyncio.run(main())
open('/home/claude/scripts/icon/mark.svg','w').write(mark())
