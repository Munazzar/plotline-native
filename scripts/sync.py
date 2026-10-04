import asyncio,json,re,urllib.parse
from playwright.async_api import async_playwright
FILES={};N=[0];TOK='tok-abc'
def cors(extra=None):
    h={'access-control-allow-origin':'*','access-control-allow-headers':'authorization,content-type','access-control-allow-methods':'GET,POST,PATCH,OPTIONS'}
    if extra:h.update(extra);return h
    return h
async def google(route,req):
    u=urllib.parse.urlparse(req.url);q=urllib.parse.parse_qs(u.query)
    if req.method=='OPTIONS':return await route.fulfill(status=204,headers=cors())
    if u.netloc=='accounts.google.com':
        red=q['redirect_uri'][0];st=q['state'][0]
        return await route.fulfill(status=302,headers={'location':f"{red}#access_token={TOK}&token_type=Bearer&expires_in=3599&state={st}&scope=x"})
    if req.headers.get('authorization')!='Bearer '+TOK:return await route.fulfill(status=401,headers=cors(),body='{}')
    p=u.path
    if p=='/drive/v3/about':return await route.fulfill(status=200,headers=cors({'content-type':'application/json'}),body=json.dumps({'user':{'emailAddress':'munazzar@example.com'}}))
    if p=='/drive/v3/files' and req.method=='GET':
        assert q.get('spaces')==['appDataFolder']
        return await route.fulfill(status=200,headers=cors({'content-type':'application/json'}),body=json.dumps({'files':[{'id':k} for k,v in FILES.items() if v['name']=='plotline.json']}))
    m=re.match(r'/drive/v3/files/([^/]+)$',p)
    if m and req.method=='GET':
        f=FILES.get(m.group(1));return await route.fulfill(status=200 if f else 404,headers=cors({'content-type':'application/json'}),body=f['body'] if f else '{}')
    if p=='/upload/drive/v3/files' and req.method=='POST':
        body=req.post_data;b=re.search(r'boundary=(\S+)',req.headers['content-type']).group(1);parts=body.split('--'+b)
        meta=json.loads(parts[1].split('\r\n\r\n',1)[1].strip());content=parts[2].split('\r\n\r\n',1)[1].rsplit('\r\n',1)[0]
        assert meta['parents']==['appDataFolder'];json.loads(content)
        N[0]+=1;fid=f'f{N[0]}';FILES[fid]={'name':meta['name'],'body':content}
        return await route.fulfill(status=200,headers=cors({'content-type':'application/json'}),body=json.dumps({'id':fid}))
    m=re.match(r'/upload/drive/v3/files/([^/]+)$',p)
    if m and req.method=='PATCH':
        json.loads(req.post_data);FILES[m.group(1)]['body']=req.post_data
        return await route.fulfill(status=200,headers=cors({'content-type':'application/json'}),body='{}')
    return await route.fulfill(status=400,headers=cors(),body='unhandled '+p)
URL='http://localhost:8765/index.html'
async def device(b,name):
    ctx=await b.new_context(viewport={'width':1200,'height':850});await ctx.route(re.compile(r'https://(accounts\.google\.com|www\.googleapis\.com)/.*'),google);await ctx.route('https://fonts.googleapis.com/**',lambda r:r.abort())
    pg=await ctx.new_page();errs=[];pg.on('pageerror',lambda e:errs.append(f'{name}: {e}'));pg.errs=errs;pg.ctx=ctx
    await pg.goto(URL);await pg.wait_for_timeout(500);await pg.evaluate('S.settings.setupDone=true');return pg
async def setup_sync(pg):
    await pg.evaluate("location.hash='#/settings/account'");await pg.wait_for_timeout(500)
    if await pg.evaluate("!!document.querySelector('#syncPanel details')"):
      await pg.evaluate("document.querySelector('#syncPanel details').open=true")
      await pg.fill('#syncPanel [name=cid]','123-test.apps.googleusercontent.com');await pg.click('#syncPanel [data-form=syncCfg] button');await pg.wait_for_timeout(300)
    await pg.click('[data-act=syncConnect]');await pg.wait_for_timeout(2500)
st=lambda pg:pg.evaluate("({goals:S.goals.map(g=>g.title).sort(),doneSteps:S.goals.flatMap(g=>g.steps.filter(s=>s.done).map(s=>s.title)).length,entries:S.entries.length,email:S.settings.sync.email,on:S.settings.sync.on,msg:SYNC_MSG})")
async def main():
  async with async_playwright() as p:
    b=await p.chromium.launch()
    A=await device(b,'A');await A.fill('input[name=name]','Munazzar');await A.click('button[value=demo]');await A.wait_for_timeout(500)
    await setup_sync(A);a1=await st(A);print('A connected',a1['on'],a1['email'],a1['msg'],'| drive files',len(FILES))
    B=await device(b,'B');await B.click('[data-act=welcomeSync]');await B.wait_for_timeout(500);await setup_sync(B);b1=await st(B);print('B pulled goals',len(b1['goals']),'==',len(a1['goals']),'| entries',b1['entries'],'==',a1['entries'])
    # concurrent edits
    await B.evaluate("(()=>{const g=S.goals.find(x=>x.title.startsWith('Read'));const s=g.steps.find(x=>!x.done);s.done=true;s.doneAt=Date.now();S.goals.push(newGoal({title:'Learn to swim',area:'health'}));save()})()")
    await A.evaluate("(()=>{S.goals=S.goals.filter(x=>!x.title.startsWith('Build a calm'));S.goals.forEach(g=>g.links=g.links.filter(l=>G(l)));save()})()")
    await A.wait_for_timeout(400);await B.wait_for_timeout(400)
    await A.evaluate("syncNow(false)");await A.wait_for_timeout(800);await B.evaluate("syncNow(false)");await B.wait_for_timeout(800);await A.evaluate("syncNow(false)");await A.wait_for_timeout(800)
    a2=await st(A);b2=await st(B)
    print('converged',a2['goals']==b2['goals'],'| swim on A','Learn to swim' in a2['goals'],'| calm gone on B',not any(t.startswith('Build a calm') for t in b2['goals']),'| done steps A/B',a2['doneSteps'],b2['doneSteps'])
    # auto-sync after an edit (debounced) without manual call
    await A.evaluate("(()=>{S.goals[0].title='Run a half marathon';save()})()");await A.wait_for_timeout(4200);await B.evaluate("syncNow(false)");await B.wait_for_timeout(900)
    print('auto-sync reached B','Run a half marathon' in (await st(B))['goals'])
    # expired token -> status asks to sign in, no crash
    await A.evaluate("localStorage.removeItem('plotline.tok');syncNow(false)");await A.wait_for_timeout(600);print('expired token status:',(await st(A))['msg'])
    print('sign-in banner shown on every page:',await A.evaluate("!!document.querySelector('#authb [data-act=reauth]')&&(go('habits'),true)&&!!document.querySelector('#authb')"),'| sync still on:',await A.evaluate('S.settings.sync.on'))
    # PIN reset via Google
    await A.evaluate("(async()=>{S.settings.pinHash=await hash('1234');save()})()");await A.wait_for_timeout(400)
    await A.reload();await A.wait_for_timeout(900);await A.click('.lock [data-act=forgotPin]');await A.wait_for_timeout(400);await A.click('#sheet [data-act=pinGoogle]');await A.wait_for_timeout(2500)
    print('PIN cleared via Google',await A.evaluate("!S.settings.pinHash&&!document.querySelector('.lock')"))
    # erase on A must not delete remote data
    await A.evaluate("location.hash='#/settings/data'");await A.wait_for_timeout(400);await A.click('[data-act=wipe]');await A.click('#sheet [data-act=confirmOk]');await A.wait_for_timeout(800)
    remote=json.loads(list(FILES.values())[0]['body']);print('after erase, Drive still has goals:',len(remote['goals']),'| dead markers:',len(remote['dead']))
    await B.evaluate("syncNow(false)");await B.wait_for_timeout(800);print('B unaffected by A erase:',len((await st(B))['goals']))
    # repeating step + share + weekly
    await B.evaluate("(()=>{const g=S.goals[0];const s=g.steps.find(x=>!x.done);s.repeat='weekly';s.due=ymd();save();ACT.toggleStep({g:g.id,s:s.id})})()");await B.wait_for_timeout(400)
    print('repeat spawned next:',await B.evaluate("(g=>{const r=g.steps.filter(s=>s.repeat==='weekly');return r.map(s=>[s.done,s.due])})(S.goals[0])"))
    txt=await B.evaluate("(()=>{let out='';const oc=copyText;copyText=t=>{out=t;return Promise.resolve(true)};navigator.share=undefined;shareGoal(S.goals[1]);copyText=oc;return out})()")
    ok=await B.evaluate("t=>{try{const o=extractPlan(t);return validatePlan(o,'create')}catch(e){return e.errs||String(e)}}",txt);print('shared plan validates:',ok==[] ,ok[:2])
    await B.evaluate("ACT.weekly()");await B.wait_for_timeout(400);await B.fill('#sheet [name=a]','Ran 3 times');await B.click('#sheet button.pri');await B.wait_for_timeout(400)
    print('weekly review saved:',await B.evaluate("S.entries.some(e=>e.title&&e.title.startsWith('Weekly review'))"))
    print('page errors',A.errs+B.errs)
    await b.close()
if __name__=="__main__":asyncio.run(main())
