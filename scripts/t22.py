# Account switching: data on this device belongs to one Google account; a different account never gets it silently.
import asyncio,json,re,urllib.parse
from playwright.async_api import async_playwright
DR={'a':{},'b':{}};CUR=['a'];N=[0]
EM={'a':'alice@example.com','b':'bob@example.com'}
def cors(extra=None):
    h={'access-control-allow-origin':'*','access-control-allow-headers':'authorization,content-type','access-control-allow-methods':'GET,POST,PATCH,OPTIONS'}
    if extra:h.update(extra)
    return h
J=lambda b:{'status':200,'headers':cors({'content-type':'application/json'}),'body':json.dumps(b)}
async def google(route,req):
    u=urllib.parse.urlparse(req.url);q=urllib.parse.parse_qs(u.query)
    if req.method=='OPTIONS':return await route.fulfill(status=204,headers=cors())
    if u.netloc=='accounts.google.com':
        return await route.fulfill(status=302,headers={'location':f"{q['redirect_uri'][0]}#access_token=tok-{CUR[0]}&token_type=Bearer&expires_in=3599&state={q['state'][0]}&scope=x"})
    au=req.headers.get('authorization','');acc=au[len('Bearer tok-'):] if au.startswith('Bearer tok-') else None
    if acc not in DR:return await route.fulfill(status=401,headers=cors(),body='{}')
    F=DR[acc];p=u.path
    if p=='/drive/v3/about':return await route.fulfill(**J({'user':{'emailAddress':EM[acc]}}))
    if p=='/drive/v3/files' and req.method=='GET':return await route.fulfill(**J({'files':[{'id':k} for k in F]}))
    m=re.match(r'/drive/v3/files/([^/]+)$',p)
    if m and req.method=='GET':
        f=F.get(m.group(1));return await route.fulfill(status=200 if f else 404,headers=cors({'content-type':'application/json'}),body=f if f else '{}')
    if p=='/upload/drive/v3/files' and req.method=='POST':
        body=req.post_data;b=re.search(r'boundary=(\S+)',req.headers['content-type']).group(1);parts=body.split('--'+b)
        content=parts[2].split('\r\n\r\n',1)[1].rsplit('\r\n',1)[0];N[0]+=1;fid=f'{acc}{N[0]}';F[fid]=content;return await route.fulfill(**J({'id':fid}))
    m=re.match(r'/upload/drive/v3/files/([^/]+)$',p)
    if m and req.method=='PATCH':
        if m.group(1) not in F:return await route.fulfill(status=404,headers=cors(),body='{}')
        F[m.group(1)]=req.post_data;return await route.fulfill(**J({}))
    return await route.fulfill(status=400,headers=cors(),body='unhandled '+p)
def drive(acc):
    if not DR[acc]:return None
    return json.loads(list(DR[acc].values())[-1])
titles=lambda d:sorted(g['title'] for g in d['goals']) if d else []
ok=lambda c,m:print(('PASS ' if c else 'FAIL ')+m)
async def main():
  async with async_playwright() as p:
    b=await p.chromium.launch();ctx=await b.new_context(viewport={'width':1200,'height':850});await ctx.route(re.compile(r'https://(accounts\.google\.com|www\.googleapis\.com)/.*'),google);await ctx.route('https://fonts.googleapis.com/**',lambda r:r.abort())
    pg=await ctx.new_page();errs=[];pg.on('pageerror',lambda e:errs.append(str(e)))
    await pg.goto('http://localhost:8765/index.html');await pg.wait_for_timeout(500);await pg.fill('input[name=name]','Alice');await pg.click('button[value=demo]');await pg.wait_for_timeout(500)
    await pg.evaluate("S.settings.tourOffered=true;document.querySelector('.tprompt')?.remove()")
    S=lambda:pg.evaluate("({goals:S.goals.map(g=>g.title).sort(),on:S.settings.sync.on,email:S.settings.sync.email,owner:S.settings.sync.owner,sheet:$('#sheet').classList.contains('on')?$('#sheet h2').textContent:''})")
    async def connect():
        await pg.evaluate("closeSheet();location.hash='#/settings/account'");await pg.wait_for_timeout(400)
        if await pg.evaluate("!!document.querySelector('#syncPanel details')"):
            await pg.evaluate("document.querySelector('#syncPanel details').open=true");await pg.fill('#syncPanel [name=cid]','123-test.apps.googleusercontent.com');await pg.click('#syncPanel [data-form=syncCfg] button');await pg.wait_for_timeout(300)
        await pg.click('[data-act=syncConnect]');await pg.wait_for_timeout(2500)
    async def off(kind='Keep'):
        await pg.evaluate("location.hash='#/settings/account'");await pg.wait_for_timeout(400);await pg.click('[data-act=syncOff]');await pg.wait_for_timeout(300);await pg.click(f'[data-act=syncOff{kind}]');await pg.wait_for_timeout(1500)
    await connect();s=await S();demo=s['goals']
    ok(s['on'] and s['owner']=='alice@example.com' and titles(drive('a'))==demo,'first connect: Alice owns the data, Drive A has it')
    await off();s=await S();ok(not s['on'] and s['goals']==demo and s['owner']=='alice@example.com','turn off + keep: data stays, still Alice’s')
    CUR[0]='b';await connect();s=await S();print(s['sheet'])
    ok('Alice'.lower() in s['sheet'].lower() or 'alice' in s['sheet'],'connecting Bob shows the account choice')
    ok(drive('b') is None and not s['on'],'nothing written to Bob’s Drive before choosing')
    await pg.click('[data-act=acctCancel]');await pg.wait_for_timeout(500);s=await S()
    ok(drive('b') is None and s['goals']==demo and not s['on'] and s['email']=='alice@example.com','cancel: nothing synced, still Alice')
    await connect();await pg.click('[data-act=acctSwitch]');await pg.wait_for_timeout(2000);s=await S()
    ok(s['on'] and s['owner']=='bob@example.com' and s['goals']==[] ,'switch: device now shows Bob’s (empty) plan')
    ok(titles(drive('b'))==[] ,'Bob’s Drive did not receive Alice’s goals')
    st=await pg.evaluate("DB.get('stash:alice@example.com').then(x=>x&&x.doc.goals.length)");ok(st==len(demo),f'Alice’s data set aside on the device ({st} goals)')
    await pg.evaluate("S.goals.push(newGoal({title:'Bob goal'}));save()");await pg.wait_for_timeout(300);await pg.evaluate("syncNow(false)");await pg.wait_for_timeout(1000)
    ok(titles(drive('b'))==['Bob goal'],'Bob’s own goal syncs to Bob')
    # auto-sync guard: token silently becomes Alice's while device holds Bob's data
    await pg.evaluate("localStorage.setItem('plotline.tok',JSON.stringify({t:'tok-a',exp:Date.now()+3e6}));S.goals[0].title='Bob goal 2';save()");await pg.wait_for_timeout(300);await pg.evaluate("syncNow(false)");await pg.wait_for_timeout(1200);s=await S()
    ok('Bob goal 2' not in titles(drive('a')) and not s['on'] and s['sheet']!='','background sync with the wrong account pauses and asks')
    await pg.click('[data-act=acctSwitch]');await pg.wait_for_timeout(2000);s=await S()
    ok(s['owner']=='alice@example.com' and s['goals']==demo and 'Bob goal 2' not in s['goals'],'switch back to Alice restores Alice’s plan')
    ok(titles(drive('a'))==demo,'Alice’s Drive unchanged by Bob')
    st=await pg.evaluate("DB.get('stash:bob@example.com').then(x=>x&&x.doc.goals.map(g=>g.title))");ok(st==['Bob goal 2'],'Bob’s unsynced change set aside, not lost')
    # deliberate copy
    await off();CUR[0]='b';await connect();await pg.click('[data-act=acctMerge]');await pg.wait_for_timeout(300);await pg.click('#sheet [data-act=confirmOk]');await pg.wait_for_timeout(2000);s=await S()
    ok(set(demo)<=set(titles(drive('b'))) and s['owner']=='bob@example.com','copy on purpose merges Alice’s data into Bob')
    # turn off + remove
    await off('Remove');s=await S();ok(s['goals']==[] and not s['on'] and titles(drive('b'))!=[],'turn off + remove clears the device, Drive keeps it')
    CUR[0]='a';await connect();s=await S();ok(s['sheet']=='' and s['goals']==demo and s['owner']=='alice@example.com','empty device + any account: just pulls that account (no prompt)')
    print('errors',errs);await b.close()
asyncio.run(main())
