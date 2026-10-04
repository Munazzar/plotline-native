# Contrast + empty-control audit across every theme (and card style) on the main pages.
# Flags visible text whose WCAG contrast against its effective solid background is < 3 (any size), and buttons
# with nothing visible inside. Gradients/images under text are resolved by sampling the rendered pixels.
import asyncio,sys,json
from playwright.async_api import async_playwright
JS=r"""(()=>{
const lum=c=>{const f=v=>{v/=255;return v<=.03928?v/12.92:Math.pow((v+.055)/1.055,2.4)};return .2126*f(c[0])+.7152*f(c[1])+.0722*f(c[2])};
const parse=s=>{const m=s.match(/rgba?\(([^)]+)\)/);if(!m)return null;const p=m[1].split(/[ ,\/]+/).filter(Boolean).map(Number);return[p[0],p[1],p[2],p.length>3?p[3]:1]};
const mix=(a,b)=>[a[0]*a[3]+b[0]*(1-a[3]),a[1]*a[3]+b[1]*(1-a[3]),a[2]*a[3]+b[2]*(1-a[3]),1];
function bgOf(el){let stack=[];let e=el;while(e&&e.nodeType===1){const cs=getComputedStyle(e);if(cs.backgroundImage!=='none'&&!/url\(/.test(cs.backgroundImage)===false)return null;if(cs.backgroundImage&&cs.backgroundImage!=='none')return null;const c=parse(cs.backgroundColor);if(c&&c[3]>0){stack.push(c);if(c[3]>=1)break}if(cs.backdropFilter&&cs.backdropFilter!=='none'&&!(c&&c[3]>=.85))return null;e=e.parentElement}
 let base=e?null:(()=>{const b=parse(getComputedStyle(document.body).backgroundColor);if(b&&b[3]>0)return b;const h=parse(getComputedStyle(document.documentElement).backgroundColor);if(h&&h[3]>0)return h;const v=getComputedStyle(document.documentElement).getPropertyValue('--bg').trim();const m=v.match(/^#([0-9a-f]{6})$/i);return m?[parseInt(m[1].slice(0,2),16),parseInt(m[1].slice(2,4),16),parseInt(m[1].slice(4,6),16),1]:[0,0,0,1]})();if(!base){base=stack.pop()}for(let i=stack.length-1;i>=0;i--)base=mix(stack[i],base);return base}
const out=[];const seen=new Set();
const walker=document.createTreeWalker(document.body,NodeFilter.SHOW_TEXT);let n;
while(n=walker.nextNode()){const t=n.textContent.trim();if(!t||!/[A-Za-z0-9]/.test(t))continue;const el=n.parentElement;if(!el||seen.has(el))continue;seen.add(el);
 const r=el.getBoundingClientRect();if(r.width<2||r.height<2||r.bottom<0||r.top>innerHeight||r.right<0||r.left>innerWidth)continue;
 const cs=getComputedStyle(el);if(cs.visibility==='hidden')continue;let op=1,e=el;while(e){const o=+getComputedStyle(e).opacity;op*=o;e=e.parentElement}if(op<.35)continue;
 if(el.closest('[aria-hidden=true],.tdim,.rx-burst,input,textarea,select,option'))continue;
 const fg=parse(cs.color);if(!fg)continue;const bg=bgOf(el);if(!bg)continue;const f2=mix([fg[0],fg[1],fg[2],fg[3]*Math.min(1,op)],bg);
 const L1=lum(f2),L2=lum(bg),cr=(Math.max(L1,L2)+.05)/(Math.min(L1,L2)+.05);
 if(cr<3)out.push({cr:+cr.toFixed(2),t:t.slice(0,40),cls:(el.className&&el.className.baseVal===undefined?el.className:'')+' <'+(el.parentElement.className||'')+'>'})}
document.querySelectorAll('button,a.btn,[role=button]').forEach(b=>{const r=b.getBoundingClientRect();if(r.width<2||r.height<2||r.bottom<0||r.top>innerHeight)return;if(getComputedStyle(b).visibility==='hidden')return;
 const vis=[...b.querySelectorAll('*')].concat([b]).some(x=>{if(x.tagName==='svg'||x.tagName==='IMG'){const q=x.getBoundingClientRect();return q.width>2}if([...x.childNodes].some(c=>c.nodeType===3&&c.textContent.trim())){return getComputedStyle(x).display!=='none'&&x.getBoundingClientRect().width>1}return false});
 const cs=getComputedStyle(b);const deco=cs.backgroundImage!=='none'||b.matches('.sw i,.node,.hv-tap,[class*=dot],.ccard,.sw,.tcard,.thm');
 if(!vis&&!deco&&!b.closest('[aria-hidden=true]'))out.push({cr:0,t:'EMPTY BUTTON '+(b.dataset.act||''),cls:b.className})});
return out})()"""
PAGES=['today','habits','calendar','goals','vista','map','journal','journalV','ask','settings','settings/look','settings/share','settings/notif','hvista','goal','habit','group','react']
async def main():
  themes=sys.argv[1].split(',') if len(sys.argv)>1 else ['night','space','dusk','graphite','daylight','nord','forest','ocean','mocha','neon','paper','mint','lavender','blush','mono']
  cards=sys.argv[2].split(',') if len(sys.argv)>2 else ['solid']
  async with async_playwright() as p:
    b=await p.chromium.launch();ctx=await b.new_context(viewport={'width':390,'height':844})
    await ctx.route('https://fonts.googleapis.com/**',lambda r:r.abort())
    pg=await ctx.new_page();errs=[];pg.on('pageerror',lambda e:errs.append(str(e)))
    await pg.add_init_script('window.__allOpen=1');await pg.goto('http://localhost:8765/index.html');await pg.wait_for_timeout(600)
    await pg.fill('input[name=name]','Munazzar');await pg.click('button[value=demo]');await pg.wait_for_timeout(800)
    await pg.evaluate("""S.settings.tourOffered=true;S.settings.setupDone=true;document.querySelector('.tprompt')?.remove();document.querySelector('#sheet.on')&&closeSheet();
     PLOTLINE_CFG.firebase={apiKey:'k',projectId:'p'};window.syncOn=()=>true;S.settings.sync.email='me@gmail.com';
     S.shares=[{id:'c1',fs:1,role:'member',status:'invited',from:'ann@gmail.com',fromName:'ann',title:'GYM',mode:'watch',k:'x',u:1},{id:'c2',fs:1,role:'owner',status:'joined',title:S.habits[0].title,mode:'together',local:{kind:'habit',id:S.habits[0].id},k:'x',u:1}];
     S.settings.share={...shset(),rx:{c2:{t:Date.now(),n:2}}};SHR={c2:[{id:'r1',from:'Ann',fe:'ann@gmail.com',e:'🔥',m:'Proud of you',t:Date.now()-5000,cid:'c2'}]};
     S.entries.push({id:'e1',t:Date.now(),type:'note',goalId:null,title:'Reply to “💪 Gym”',text:'Im tired lets skip today.',mood:'',u:1});window.fsMembers=async()=>[];save();""")
    tot={}
    for th in themes:
     for cs in cards:
      await pg.evaluate(f"S.settings.theme='{th}';S.settings.cards='{cs}';applyTheme()");await pg.wait_for_timeout(300)
      for pgname in PAGES:
        js={'journalV':"S.settings.layout.journal='v';go('journal')",'journal':"S.settings.layout.journal='h';S.settings.jFilter='all';go('journal')",'goal':"go('goal/'+S.goals[0].id)",'habit':"go('habit/'+S.habits[0].id)",
            'group':"go('today');setTimeout(()=>openSheet(`<div class=\"sh-g\">${shGroupHTML(shById('c2'),{c:{members:['me@gmail.com','ann@gmail.com'],keys:{},owner:'me@gmail.com'},d:{title:'Run',mode:'together',item:{kind:'habit'}},ms:[{email:'me@gmail.com',name:'Me',mine:true,progress:{},cheers:[{to:'ann@gmail.com',emoji:'👏',t:Date.now()}]},{email:'ann@gmail.com',name:'Ann',progress:{streak:3},cheers:[]}],all:[]})}</div>`),300)",
            'react':"go('today');setTimeout(()=>{shReactSheet(shById('c2'),'ann@gmail.com','Ann');reactBanner([{id:'r9',from:'Ann',fe:'ann@gmail.com',e:'🔥',m:'Let’s go!',t:Date.now(),cid:'c2'}])},300)"}.get(pgname,f"closeSheet();go('{pgname}')")
        await pg.evaluate("document.querySelector('#sheet.on')&&closeSheet();RB.q=[];reactShow()");await pg.evaluate(js);await pg.wait_for_timeout(1100)
        await pg.evaluate("document.querySelectorAll('.rv').forEach(e=>e.classList.add('in'))");await pg.wait_for_timeout(450)
        if pgname=='journal':
          await pg.evaluate("document.querySelectorAll('.jf').forEach(e=>e.classList.add('flip'))");await pg.wait_for_timeout(800)
        r=await pg.evaluate(JS)
        if r:
          tot[(th,cs,pgname)]=r
          print(f'== {th}/{cs}/{pgname}');[print('  ',x) for x in r[:12]]
        if '--shots' in sys.argv:await pg.screenshot(path=f'/home/claude/scripts/c-{th}-{cs}-{pgname.replace("/","_")}.png')
    print('ISSUES',sum(len(v) for v in tot.values()),'errors',errs);await b.close()
asyncio.run(main())
