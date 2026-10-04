import asyncio,json,re,sys
sys.path.insert(0,'/home/claude/scripts')
from sync import google,FILES
from playwright.async_api import async_playwright
MOCK="""window.__calls=[];window.PlotlineNative={schedule:j=>__calls.push(['schedule']),notificationsGranted:()=>true,requestNotifications:()=>{},copy:t=>{},saveFile:()=>true,setBars:()=>{},
googleToken:i=>{__calls.push(['googleToken',i]);setTimeout(()=>__gauth(JSON.stringify({token:'tok-abc',email:'munazzar@example.com',native:true,exp:3000})),60)},
invalidateToken:t=>__calls.push(['invalidate']),googleSignOut:()=>__calls.push(['signout']),openUrl:u=>__calls.push(['openUrl',u]),share:(s,t)=>__calls.push(['share',s,t.length]),widget:j=>__calls.push(['widget',JSON.parse(j)])};"""
async def main():
  async with async_playwright() as p:
    b=await p.chromium.launch();ctx=await b.new_context(viewport={'width':390,'height':844});await ctx.route(re.compile(r'https://(accounts\.google\.com|www\.googleapis\.com)/.*'),google)
    pg=await ctx.new_page();errs=[];pg.on('pageerror',lambda e:errs.append(str(e)));await pg.add_init_script(MOCK)
    await pg.goto('http://localhost:8765/index.html');await pg.wait_for_timeout(600);await pg.click('button[value=demo]');await pg.wait_for_timeout(600)
    await pg.evaluate("location.hash='#/settings/account'");await pg.wait_for_timeout(600)
    await pg.click('[data-act=syncConnect]');await pg.wait_for_timeout(2500)
    r=await pg.evaluate("({on:S.settings.sync.on,native:S.settings.sync.native,email:S.settings.sync.email,msg:SYNC_MSG})");print('native connect',r,'| drive files',len(FILES))
    await pg.evaluate("localStorage.removeItem('plotline.tok');syncNow(false)");await pg.wait_for_timeout(900)
    print('silent native refresh', await pg.evaluate("[SYNC_MSG,__calls.filter(c=>c[0]=='googleToken').map(c=>c[1])]"))
    w=await pg.evaluate("__calls.filter(c=>c[0]=='widget').pop()[1]");print('widget items',len(w['items']),w['items'][0])
    await pg.evaluate("(()=>{const g=S.goals[0];ACT.goalMenu({id:g.id})})()");await pg.wait_for_timeout(400);await pg.click('#sheet [data-act=shareGoal]');await pg.wait_for_timeout(300)
    print('share',await pg.evaluate("__calls.filter(c=>c[0]=='share').pop()"))
    # browser fallback: webUrl set -> openUrl with redirect to web address and state app...
    await pg.evaluate("S.settings.sync.webUrl='https://munazzar.github.io/plotline/';S.settings.sync.clientId='123-test.apps.googleusercontent.com';disconnectDrive()");await pg.wait_for_timeout(400)
    await pg.click('[data-act=syncConnectBrowser]');await pg.wait_for_timeout(400)
    u=await pg.evaluate("__calls.filter(c=>c[0]=='openUrl').pop()[1]");print('browser fallback url ok','redirect_uri=https%3A%2F%2Fmunazzar.github.io%2Fplotline%2F' in u and 'state=app' in u and 'drive.appdata' in u)
    await pg.evaluate("__gauth(JSON.stringify({token:'tok-abc',exp:3600}))");await pg.wait_for_timeout(2000)
    print('after deep-link token',await pg.evaluate("({on:S.settings.sync.on,native:S.settings.sync.native,msg:SYNC_MSG})"))
    # the web page's return screen for app sign-in
    pg2=await ctx.new_page();await pg2.goto('http://localhost:8765/index.html#access_token=tok-abc&expires_in=3599&state=app123');await pg2.wait_for_timeout(500)
    print('return page link',await pg2.evaluate("document.querySelector('a.btn')?.getAttribute('href')"))
    await pg.screenshot(path='/home/claude/scripts/n-settings.png',full_page=True)
    print('errors',errs);await b.close()
asyncio.run(main())
