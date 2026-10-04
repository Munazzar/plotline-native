# Pixel-based contrast audit: renders each page, records every visible text run's colour + box,
# then re-renders with text made transparent and samples the real background pixels behind it.
# Catches text on gradients, frosted glass, images. Usage: contrast2.py theme1,theme2 [--shots]
import asyncio,sys,io
from PIL import Image
from playwright.async_api import async_playwright
COLLECT=r"""(()=>{const parse=s=>{const m=s.match(/rgba?\(([^)]+)\)/);if(!m)return null;const p=m[1].split(/[ ,\/]+/).filter(Boolean).map(Number);return[p[0],p[1],p[2],p.length>3?p[3]:1]};
const out=[],seen=new Set();const TB=[...document.querySelectorAll('.tabbar,#fnav,.sbar')].map(e=>e.getBoundingClientRect()).filter(r=>r.height>0);const w=document.createTreeWalker(document.body,NodeFilter.SHOW_TEXT);let n;
while(n=w.nextNode()){const t=n.textContent.trim();if(!t||!/[\p{L}\p{N}]/u.test(t))continue;const el=n.parentElement;if(!el||seen.has(el))continue;seen.add(el);
 if(el.closest('[aria-hidden=true],.rx-burst,select,option,svg,.studio,#studio,.sky,.scene'))continue;
 const rg=document.createRange();rg.selectNodeContents(n);const r=rg.getBoundingClientRect();if(r.width<4||r.height<6||r.bottom<2||r.top>innerHeight-2||r.right<2||r.left>innerWidth-2)continue;
 if(!el.closest('.tabbar')&&TB.some(t=>r.bottom>t.top&&r.top<t.bottom&&r.right>t.left&&r.left<t.right))continue;const cs=getComputedStyle(el);if(cs.visibility==='hidden')continue;let op=1,e=el;while(e){op*=+getComputedStyle(e).opacity;e=e.parentElement}if(op<.2)continue;
 // skip text covered by another element (e.g. under tab bar)
 const cx=Math.min(innerWidth-1,Math.max(0,r.left+Math.min(r.width/2,20))),cy=Math.min(innerHeight-1,Math.max(0,r.top+r.height/2));let cov=false;for(const fx of [.15,.5,.85]){const px=Math.min(innerWidth-1,Math.max(0,r.left+r.width*fx));const top=document.elementFromPoint(px,cy);if(!top||(top!==el&&!el.contains(top)&&!top.contains(el))){cov=true;break}}if(cov)continue;
 let fg=parse(cs.webkitTextFillColor&&cs.webkitTextFillColor!==cs.color&&!/rgba\(0, 0, 0, 0\)/.test(cs.webkitTextFillColor)?cs.webkitTextFillColor:cs.color);if(!fg)continue;
 const isPh=el.tagName==='INPUT'||el.tagName==='TEXTAREA';
 out.push({t:t.slice(0,40),fg,op:Math.min(1,op*fg[3]),x:r.left,y:r.top,w:r.width,h:r.height,cls:(typeof el.className==='string'?el.className:'')+' <'+(el.parentElement&&typeof el.parentElement.className==='string'?el.parentElement.className:'')+'>',fs:parseFloat(cs.fontSize),fw:+cs.fontWeight})}
// placeholders
document.querySelectorAll('input[placeholder],textarea[placeholder]').forEach(el=>{if(el.value)return;const r=el.getBoundingClientRect();if(r.width<4||r.bottom<2||r.top>innerHeight-2)return;const c=parse(getComputedStyle(el,'::placeholder').color);if(!c)return;out.push({t:'[ph] '+el.placeholder.slice(0,34),fg:c,op:c[3],x:r.left+8,y:r.top+4,w:Math.min(r.width-16,160),h:r.height-8,cls:'placeholder',fs:16,fw:400,ph:1})});
return out})()"""
HIDE="""(()=>{const s=document.createElement('style');s.id='__hidetx';s.textContent='*,*::before,*::after{color:transparent!important;-webkit-text-fill-color:transparent!important;text-shadow:none!important;caret-color:transparent!important;text-decoration-color:transparent!important}*::placeholder{color:transparent!important}';document.head.appendChild(s)})()"""
def lum(c):
  f=lambda v:(v/255/12.92) if v/255<=.03928 else ((v/255+.055)/1.055)**2.4
  return .2126*f(c[0])+.7152*f(c[1])+.0722*f(c[2])
def cr(a,b):
  A,B=lum(a),lum(b);return (max(A,B)+.05)/(min(A,B)+.05)
PAGES=sys.argv[3].split(',') if len(sys.argv)>3 and not sys.argv[3].startswith('--') else ['today','habits','habit','quit','cal','goals','goal','vista','journal','threads','thread','ask','ai','settings','settings/look','activity','aov']
async def main():
  themes=sys.argv[1].split(',');W=int(sys.argv[2]) if len(sys.argv)>2 else 390
  async with async_playwright() as p:
    b=await p.chromium.launch();ctx=await b.new_context(viewport={'width':W,'height':844},device_scale_factor=1)
    pg=await ctx.new_page();errs=[];pg.on('pageerror',lambda e:errs.append(str(e)))
    await pg.add_init_script('window.__allOpen=1');await pg.goto('http://localhost:8765/index.html');await pg.wait_for_timeout(600)
    await pg.fill('input[name=name]','Munazzar');await pg.click('button[value=demo]');await pg.wait_for_timeout(800)
    await pg.evaluate("S.settings.tourOffered=true;S.settings.setupDone=true;document.querySelector('.tprompt')?.remove();S.threads=[{id:'t1',title:'Plotline ideas',link:null,status:'open',created:Date.now()-5e8,u:1,ups:[{id:'a',t:Date.now()-4e8,k:'idea',x:'Voice logging from a widget',u:1}]}];save()")
    ids=await pg.evaluate("[S.goals[0].id,S.habits.find(h=>h.kind==='build').id,S.habits.find(h=>h.kind==='quit').id]")
    total=0
    for th in themes:
      await pg.evaluate(f"S.settings.theme='{th}';applyTheme()")
      for name in PAGES:
        route={'goal':'goal/'+ids[0],'habit':'habit/'+ids[1],'quit':'habit/'+ids[2],'thread':'thread/t1','aov':'today'}.get(name,name)
        await pg.evaluate(f"typeof aovClose==='function'&&aovClose();document.querySelector('#sheet.on')&&closeSheet();location.hash='#/{route}';render(false)");await pg.wait_for_timeout(700)
        if name=='aov':await pg.evaluate("aovOpen()");await pg.wait_for_timeout(600)
        await pg.evaluate("document.querySelectorAll('.rv').forEach(e=>e.classList.add('in'));document.getAnimations().forEach(a=>{try{a.finish()}catch(e){}})");await pg.wait_for_timeout(250)
        issues=[]
        for sy in ([0,700,1400] if name!='aov' else [0]):
          await pg.evaluate(f"document.documentElement.style.scrollBehavior='auto';window.scrollTo({{top:{sy},behavior:'instant'}})");await pg.wait_for_timeout(250)
          if sy and await pg.evaluate("scrollY")<sy-5:break
          items=await pg.evaluate(COLLECT)
          await pg.evaluate(HIDE);await pg.wait_for_timeout(80)
          img=Image.open(io.BytesIO(await pg.screenshot())).convert('RGB')
          if '--shots' in sys.argv and sy==0:
            await pg.evaluate("document.getElementById('__hidetx').remove()");img2=await pg.screenshot(path=f'/tmp/claude-0/ui/c2-{th}-{name.replace("/","_")}.png')
          else:await pg.evaluate("document.getElementById('__hidetx')?.remove()")
          for it in items:
            x0,y0=int(max(0,it['x'])),int(max(0,it['y']));x1,y1=int(min(img.width-1,it['x']+it['w'])),int(min(img.height-1,it['y']+it['h']))
            if x1-x0<2 or y1-y0<2:continue
            px=list(img.crop((x0,y0,x1,y1)).resize((min(24,x1-x0),min(8,y1-y0))).get_flattened_data() if hasattr(Image.Image,"get_flattened_data") else img.crop((x0,y0,x1,y1)).resize((min(24,x1-x0),min(8,y1-y0))).getdata())
            fg=it['fg'];a=it['op']
            # worst-case over sampled bg pixels (use 20th percentile contrast)
            vals=sorted(cr([fg[0]*a+c[0]*(1-a),fg[1]*a+c[1]*(1-a),fg[2]*a+c[2]*(1-a)],c) for c in px)
            v=vals[len(vals)//5]
            big=it['fs']>=24 or (it['fs']>=18.6 and it['fw']>=700)
            lim=2.6 if it.get('ph') else (3.0 if big else 3.6)
            if v<lim:issues.append((round(v,2),it['t'],it['cls'][:60],sy))
        seen=set();u=[i for i in issues if not (i[1] in seen or seen.add(i[1]))]
        if u:
          total+=len(u);print(f'== {th}/{name}');[print('  ',x) for x in u[:14]]
    print('ISSUES',total,'errors',errs);await b.close()
asyncio.run(main())
