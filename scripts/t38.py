# Native (Android) path for someone who signed in before 1.8.2: silent sign-in with the new email permission needs consent,
# so sync quietly keeps the old permission, the app asks once, and the hourly invite check gets its settings.
import asyncio,json,re
from playwright.async_api import async_playwright
import t37
from t37 import ok,CIRC
MOCK=r"""
window.__granted=false;window.__scopes='';window.__cfg=null;window.__calls=[];
window.PlotlineNative={setScopes:s=>{window.__scopes=s},googleToken:i=>{window.__calls.push([i,window.__scopes]);setTimeout(()=>{const em=window.__scopes.includes('userinfo.email');
  if(em&&!i&&!window.__granted)return window.__gauth(JSON.stringify({err:'needs_consent'}));if(em&&i)window.__granted=true;
  window.__gauth(JSON.stringify({token:em?'tok-b-e':'tok-b',exp:3600,email:'b@example.com',native:true}))},50)},
 shareCfg:j=>{window.__cfg=JSON.parse(j)},invalidateToken:()=>{},llmStatus:()=>'{"ok":false}',notificationsGranted:()=>true};
window.PlotlineNative=new Proxy(window.PlotlineNative,{get:(t,k)=>k in t?t[k]:(()=>'')});
"""
async def main():
  async with async_playwright() as p:
    b=await p.chromium.launch();ctx=await b.new_context(viewport={'width':390,'height':844});await ctx.add_init_script(MOCK)
    await ctx.route(re.compile(r'https://(accounts\.google\.com|www\.googleapis\.com|identitytoolkit\.googleapis\.com|securetoken\.googleapis\.com|firestore\.googleapis\.com)/.*'),t37.mk('b'))
    await ctx.route('https://fonts.googleapis.com/**',lambda r:r.abort())
    # an invite from Ann already waiting
    CIRC['cx1']={'owner':{'stringValue':'a@example.com'},'ownerName':{'stringValue':'Ann'},'title':{'stringValue':'Gym'},'mode':{'stringValue':'together'},'item':{'stringValue':json.dumps({'kind':'habit','title':'Gym'})},'members':{'arrayValue':{'values':[{'stringValue':'a@example.com'},{'stringValue':'b@example.com'}]}}}
    pg=await ctx.new_page();errs=[];pg.on('pageerror',lambda e:errs.append(str(e)))
    await pg.goto('http://localhost:8765/index.html');await pg.wait_for_timeout(500);await pg.evaluate("PLOTLINE_CFG.firebase={apiKey:'k',projectId:'p'}")
    await pg.fill('input[name=name]','Ben');await pg.click('button[value=empty]');await pg.wait_for_timeout(600)
    await pg.evaluate("S.settings.tourOffered=true;S.settings.setupDone=true;document.querySelector('.tprompt')?.remove();document.querySelector('#sheet.on')&&closeSheet();S.settings.share={on:false};S.settings.sync={...S.settings.sync,on:true,native:true,email:'b@example.com',owner:'b@example.com'};PLOTLINE_CFG.firebase={apiKey:'k',projectId:'p'};scopeSync();save()")
    ok(await pg.evaluate("shOn()&&DRIVE_SCOPE.includes('userinfo.email')"),'1.8.1 users with sharing off get invites on by default after the update')
    await pg.evaluate("syncNow(false)");await pg.wait_for_timeout(2500)
    ok(await pg.evaluate("S.settings.sync.err!=='auth'"),'sync keeps working (no sign-out banner)')
    ok(await pg.evaluate("shset().lite===true&&shset().allow===true&&DRIVE_SCOPE.indexOf('userinfo')<0"),'fell back to the old permission and flagged one tap')
    await pg.evaluate("go('today')");await pg.wait_for_timeout(400)
    await pg.click('.sh-today [data-act=shAllow]');await pg.wait_for_timeout(2500)
    calls=await pg.evaluate("__calls");ok(any(c[0] and 'userinfo.email' in c[1] for c in calls),'Allow asks the phone account for the email permission interactively')
    ok(await pg.evaluate("shares().some(x=>x.id==='cx1'&&x.status==='invited')"),'Ann’s invite appears right after')
    cfg=await pg.evaluate("__cfg");ok(cfg and cfg['on'] and cfg['rt'] and cfg['pid']=='p' and cfg['email']=='b@example.com' and 'cx1' in cfg['seen'],'hourly background check gets its settings (and skips invites already shown)')
    print('errors',errs);ok(not errs,'no page errors');await b.close()
asyncio.run(main())
