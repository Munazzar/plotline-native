# Sharing over a fake Firebase (Identity Toolkit + secure token + Firestore with the real security rules' logic).
# A shares a habit with B. B signed in before 1.8.2 (no email permission): asked once in the app, then the invite just appears.
import asyncio,json,re,urllib.parse,time
from playwright.async_api import async_playwright
ok_n=[0,0]
def ok(c,m):
    ok_n[0 if c else 1]+=1;print(('PASS ' if c else 'FAIL ')+m)
USERS={'tok-a-e':'a@example.com','tok-b':'b@example.com','tok-b-e':'b@example.com'}
APPF={};CIRC={};MS={};KEYS={};DENIED=[]
def H():return{'access-control-allow-origin':'*','access-control-allow-headers':'authorization,content-type','access-control-allow-methods':'GET,POST,PATCH,DELETE,OPTIONS','content-type':'application/json'}
def sval(f,k):v=f.get(k) or {};return v.get('stringValue','')
def arr(f,k):return[x.get('stringValue','') for x in ((f.get(k) or {}).get('arrayValue') or {}).get('values',[])]
def doc(name,f):return{'name':name,'fields':f,'createTime':'x','updateTime':'x'}
BASE='projects/p/databases/(default)/documents'
def mk(person):
    async def route(r,req):
        u=urllib.parse.urlparse(req.url);q=urllib.parse.parse_qs(u.query);p=u.path
        if req.method=='OPTIONS':return await r.fulfill(status=204,headers=H())
        J=lambda o,s=200:r.fulfill(status=s,headers=H(),body=json.dumps(o))
        if u.netloc=='accounts.google.com':
            red=q['redirect_uri'][0];st=q['state'][0];sc=q.get('scope',[''])[0]
            tok=f'tok-{person}'+('-e' if 'userinfo.email' in sc else '')
            return await r.fulfill(status=302,headers={'location':f"{red}#access_token={tok}&token_type=Bearer&expires_in=3599&state={st}&scope=x"})
        if u.netloc=='identitytoolkit.googleapis.com':
            b=json.loads(req.post_data);t=urllib.parse.parse_qs(b['postBody'])['access_token'][0]
            assert q['key']==['k'] and b['requestUri']
            if t not in USERS:return await J({'error':{'message':'INVALID_IDP_RESPONSE'}},400)
            o={'idToken':'id-'+t,'refreshToken':'rt-'+t,'expiresIn':'3600','localId':'u'+t}
            if t.endswith('-e'):o['email']=USERS[t];o['emailVerified']=True
            return await J(o)
        if u.netloc=='securetoken.googleapis.com':
            rt=urllib.parse.parse_qs(req.post_data)['refresh_token'][0]
            return await J({'id_token':'id-'+rt[3:],'refresh_token':rt,'expires_in':'3600'})
        if u.netloc=='firestore.googleapis.com':
            t=(req.headers.get('authorization') or '').replace('Bearer id-','');t2=t if t.endswith('-e') else None
            me=USERS.get(t2) if t2 else None
            if not me:return await J({'error':{'status':'UNAUTHENTICATED'}},401)
            path=urllib.parse.unquote(p.split('/documents',1)[1])
            def deny(why):DENIED.append(why);return J({'error':{'status':'PERMISSION_DENIED','message':why}},403)
            if path==':runQuery':
                sq=json.loads(req.post_data)['structuredQuery'];ff=sq['where']['fieldFilter']
                if not(ff['field']['fieldPath']=='members' and ff['op']=='ARRAY_CONTAINS' and ff['value']['stringValue']==me):return await deny('query not limited to me')
                return await J([{'document':doc(f'{BASE}/circles/{c}',f)} for c,f in CIRC.items() if me in arr(f,'members')] or [{'readTime':'x'}])
            km=re.match(r'^/keys/([^/]+)$',path)
            if km:
                em=km.group(1)
                if req.method=='GET':return await (J(doc(f'{BASE}/keys/{em}',KEYS[em])) if em in KEYS else J({'error':{'status':'NOT_FOUND'}},404))
                f=json.loads(req.post_data)['fields']
                if em!=me or set(f)-{'pub','fp','u'}:return await deny('bad key write')
                KEYS[em]=f;return await J(doc('x',f))
            m=re.match(r'^/circles(?:/([^/]+))?(?:/ms(?:/([^/]+))?)?$',path)
            if not m:return await J({},400)
            cid,em=m.group(1),m.group(2);sub='/ms' in path
            if not cid and req.method=='POST':
                cid=q['documentId'][0];f=json.loads(req.post_data)['fields']
                if cid in CIRC:return await J({'error':{'status':'ALREADY_EXISTS'}},409)
                if sval(f,'owner')!=me or me not in arr(f,'members') or set(f)-{'owner','members','opub','locks','enc','created','u'}:return await deny('bad create')
                CIRC[cid]=f;return await J(doc(f'{BASE}/circles/{cid}',f))
            c=CIRC.get(cid)
            if not sub:
                if req.method=='GET':
                    if not c:return await J({'error':{'status':'NOT_FOUND'}},404)
                    if me not in arr(c,'members'):return await deny('read circle')
                    return await J(doc(f'{BASE}/circles/{cid}',c))
                if req.method=='PATCH':
                    if not c or sval(c,'owner')!=me:return await deny('update circle')
                    f=json.loads(req.post_data)['fields'];mask=q.get('updateMask.fieldPaths',list(f))
                    n=dict(c);[n.__setitem__(k,f[k]) for k in mask];
                    if me not in arr(n,'members'):return await deny('owner must stay')
                    CIRC[cid]=n;return await J(doc('x',n))
                if req.method=='DELETE':
                    if c and sval(c,'owner')!=me:return await deny('delete circle')
                    CIRC.pop(cid,None);return await J({})
            else:
                if not c or me not in arr(c,'members'):return await deny('ms access')
                if not em and req.method=='GET':return await J({'documents':[doc(f'{BASE}/circles/{cid}/ms/{e}',f) for (cc,e),f in MS.items() if cc==cid]} if any(cc==cid for cc,_ in MS) else {})
                if req.method=='PATCH':
                    if em!=me:return await deny('write someone else’s progress')
                    f=json.loads(req.post_data)['fields']
                    if set(f)-{'status','enc','u'} or sval(f,'status') not in('joined','declined','left'):return await deny('bad ms keys')
                    MS[(cid,em)]=f;return await J(doc('x',f))
                if req.method=='DELETE':
                    if em!=me and sval(c,'owner')!=me:return await deny('delete ms')
                    MS.pop((cid,em),None);return await J({})
            return await J({},400)
        # Google Drive (sync only)
        t=(req.headers.get('authorization') or '').replace('Bearer ','');user=USERS.get(t)
        if not user:return await J({},401)
        if p=='/drive/v3/about':return await J({'user':{'emailAddress':user}})
        if p=='/drive/v3/files':return await J({'files':[{'id':k} for k,v in APPF.items() if v['o']==user]})
        if p=='/upload/drive/v3/files' and req.method=='POST':
            k='f'+str(len(APPF));APPF[k]={'o':user,'b':'{}'};return await J({'id':k})
        m=re.match(r'/upload/drive/v3/files/([^/]+)$',p)
        if m:APPF[m.group(1)]['b']=req.post_data;return await J({})
        m=re.match(r'/drive/v3/files/([^/]+)$',p)
        if m:return await r.fulfill(status=200,headers=H(),body=APPF[m.group(1)]['b'])
        return await J({},400)
    return route
async def device(b,name,person,fb_first):
    ctx=await b.new_context(viewport={'width':390,'height':844})
    await ctx.route(re.compile(r'https://(accounts\.google\.com|www\.googleapis\.com|identitytoolkit\.googleapis\.com|securetoken\.googleapis\.com|firestore\.googleapis\.com)/.*'),mk(person))
    await ctx.route('https://fonts.googleapis.com/**',lambda r:r.abort())
    pg=await ctx.new_page();errs=[];pg.on('pageerror',lambda e:errs.append(f'{name}: {e}'));pg.errs=errs
    await pg.add_init_script('window.__allOpen=1');await pg.goto('http://localhost:8765/index.html');await pg.wait_for_timeout(500)
    await pg.fill('input[name=name]',name);await pg.click('button[value=empty]');await pg.wait_for_timeout(600)
    await pg.evaluate("S.settings.tourOffered=true;S.settings.setupDone=true;document.querySelector('.tprompt')?.remove();document.querySelector('#sheet.on')&&closeSheet()")
    await pg.evaluate("PLOTLINE_CFG.firebase={apiKey:'k',projectId:'p'};scopeSync()")
    if not fb_first:await pg.evaluate("S.settings.share={...shset(),lite:true};scopeSync()")  # signed in before 1.8.2: no email permission
    await pg.evaluate("location.hash='#/settings/account'");await pg.wait_for_timeout(400)
    await pg.click('[data-act=syncConnect]');await pg.wait_for_timeout(2500)
    return pg
async def main():
  async with async_playwright() as p:
    b=await p.chromium.launch()
    A=await device(b,'Ann','a',True)
    ok(await A.evaluate("DRIVE_SCOPE.includes('userinfo.email')&&!DRIVE_SCOPE.includes('drive.file')"),'A: asks Google for sync + email only (no Drive file access)')
    hid=await A.evaluate("(()=>{const h=normHabit({id:uid(),title:'Morning run',startDate:addDays(-10)});S.habits.push(h);hSetVal(h,ymd(),1);save();return h.id})()")
    await A.evaluate(f"go('habit/{hid}');ACT.shareHabit({{id:'{hid}'}})");await A.wait_for_timeout(800)
    t=await A.evaluate("document.querySelector('#sheet').textContent");ok('What happens when you share' in t and 'can’t read it' in t and 'email addresses' in t,'A: first share explains exactly what happens (encrypted, what the service sees)')
    await A.click('[data-act=shTold]');await A.wait_for_timeout(300)
    ok(await A.evaluate("!!document.querySelector('form[data-form=shareGo]')"),'A: then the share form (no extra permission screens)')
    await A.click('.sh-m:has(input[value=compete])');await A.fill('form[data-form=shareGo] input[name=emails]','B@Example.com');await A.click('form[data-form=shareGo] .btn.pri');await A.wait_for_timeout(1500)
    ok(len(CIRC)==1 and arr(list(CIRC.values())[0],'members')==['a@example.com','b@example.com'],'A: one shared item, members are A and B (lowercased)')
    ok('Morning run' not in json.dumps(CIRC)+json.dumps(list(MS.values())) and 'Ann' not in json.dumps(CIRC)+json.dumps(list(MS.values())),'what the server holds is encrypted: no title, no names')
    ok(await A.evaluate("(async()=>{const x=shares()[0];const c=await fsCircle(x.id);const d=await decJ(x.k,c.enc);return d.title==='Morning run'&&d.item.kind==='habit'})()"),'A can decrypt it with the item key')
    t=await A.evaluate("document.querySelector('#sheet').textContent");ok('Invite sent' in t,'A: told the invite is sent, nothing else to do')
    await A.evaluate('closeSheet();syncNow(false)');await A.wait_for_timeout(1500)
    ok(any('"ident"' in v['b'] and '"priv"' in v['b'] for v in APPF.values() if v['o']=='a@example.com'),'A’s private key is kept in A’s own Drive (synced with the plan), never in Firebase')
    B=await device(b,'Ben','b',False)
    await B.evaluate("shRefresh(false)");await B.wait_for_timeout(1200)
    ok(await B.evaluate("shset().allow===true"),'B (signed in before): asked once for the email permission, sync keeps working')
    await B.evaluate("go('today')");await B.wait_for_timeout(400)
    ok(await B.evaluate("!!document.querySelector('.sh-today [data-act=shAllow]')"),'B: the Allow card is on Today, not hidden in settings')
    await B.click('.sh-today [data-act=shAllow]');await B.wait_for_timeout(3000)
    ok(await B.evaluate("shset().allow===false&&!!fbGet()&&fbGet().email==='b@example.com'"),'B: one tap → signed in for sharing')
    ok(await B.evaluate("shares().some(x=>x.status==='invited'&&x.locked&&x.from==='a@example.com')"),'B: the invite appeared at once (locked: B wasn’t on Plotline when A shared)')
    ok('b@example.com' in KEYS and 'priv' not in json.dumps(KEYS),'B published only a public key')
    await A.evaluate("SHC.t=0;shRefresh(false)");await A.wait_for_timeout(1500)
    ok('b@example.com' in json.loads(sval(list(CIRC.values())[0],'locks')),'A’s app locked the item key for B')
    await B.evaluate("SHC.t=0;shRefresh(false)");await B.wait_for_timeout(1500)
    ok(await B.evaluate("shares().some(x=>x.status==='invited'&&!x.locked&&x.title==='Morning run'&&x.fromName==='Ann')"),'B: the invite unlocked with its title')
    await B.evaluate("go('today')");await B.wait_for_timeout(400)
    ok(await B.evaluate("!!document.querySelector('.sh-today [data-act=shAccept]')"),'B: invite card on Today')
    await B.click('.sh-today [data-act=shAccept]');await B.wait_for_timeout(1200)
    await B.click('[data-act=shJoin]');await B.wait_for_timeout(1800)
    bh=await B.evaluate("(()=>{const x=shares().find(x=>x.status==='joined');return x&&x.local&&x.local.id})()")
    ok(bool(bh) and await B.evaluate(f"!!H('{bh}')"),'B: joined, the habit is in B’s plan')
    await B.evaluate(f"hSetVal(H('{bh}'),ymd(),1);save();shRefresh(true)");await B.wait_for_timeout(1500)
    sid=await A.evaluate("shares()[0].id");await A.evaluate(f"ACT.shOpen({{id:'{sid}'}})");await A.wait_for_timeout(1800)
    t=await A.evaluate("document.querySelector('#sheet').textContent")
    ok('Ben' in t and '1 this week' in t,'A: sees Ben’s check-in on the leaderboard')
    await B.evaluate(f"hSetVal(H('{bh}'),ymd(),0);save()");await A.wait_for_timeout(15000)
    t=await A.evaluate("document.querySelector('#sheet').textContent");ok('Ben' in t and '0 this week' in t,'live: Ben’s new check-in shows in Ann’s open group view by itself')
    await A.click('#sheet [data-act=shReact]');await A.wait_for_timeout(400);await A.click('.rx-b[data-e="🔥"]');await A.wait_for_timeout(1800)
    await B.evaluate(f"ACT.shOpen({{id:'{sid}'}})");await B.wait_for_timeout(1800);t2=await B.evaluate("document.querySelector('#sheet').textContent")
    ok('Ann → you' in t2 and '🔥' in t2,'B: sees Ann’s reaction in the group view')
    await B.evaluate('closeSheet()')
    # security: B can’t write A’s progress or change the circle
    r=await B.evaluate(f"fs('/circles/{sid}/ms/'+encodeURIComponent('a@example.com'),{{method:'PATCH',body:JSON.stringify(fsEnc({{name:'x',status:'joined',progress:'{{}}',cheers:'[]',u:1}}))}}).then(()=>'ok',e=>String(e.message))")
    ok('403' in r,'rules: B can’t write Ann’s progress')
    r=await B.evaluate(f"fs('/circles/{sid}',{{method:'DELETE'}}).then(()=>'ok',e=>String(e.message))")
    ok('403' in r and sid in CIRC,'rules: B can’t delete Ann’s share')
    # invite more + stop
    pub=await A.evaluate("(async()=>{const kp=await crypto.subtle.generateKey(EC,true,['deriveBits']);const p=b64(await crypto.subtle.exportKey('raw',kp.publicKey));return[p,await fpOf(p)]})()")
    KEYS['c@example.com']={'pub':{'stringValue':pub[0]},'fp':{'stringValue':pub[1]}}
    await A.evaluate(f"closeSheet();ACT.shInvite({{id:'{sid}'}})");await A.wait_for_timeout(300);await A.fill('form[data-form=shMore] input','c@example.com');await A.click('form[data-form=shMore] .btn.pri');await A.wait_for_timeout(1200)
    ok('c@example.com' in arr(CIRC[sid],'members') and 'c@example.com' in json.loads(sval(CIRC[sid],'locks')),'A: invited someone already on Plotline: their key is locked in right away')
    await A.evaluate(f"closeSheet();ACT.shStop({{id:'{sid}'}})");await A.wait_for_timeout(300);await A.click('#sheet [data-act=confirmOk]');await A.wait_for_timeout(1500)
    ok(sid not in CIRC and not any(c==sid for c,_ in MS),'A: stop sharing deletes the item and everyone’s progress')
    await B.evaluate("SHC.t=0;shRefresh(false)");await B.wait_for_timeout(1200)
    ok(await B.evaluate(f"shById('{sid}').status==='ended'"),'B: sees it ended')
    # legacy 1.8.1 Drive shares are moved over gracefully
    await A.evaluate("S.shares.push({id:'old1',fileId:'x9',role:'owner',mode:'together',title:'Gym',status:'joined',local:{kind:'habit',id:'%s'},u:1});S.shares.push({id:'old2',fileId:'x8',role:'member',title:'Read',status:'invited',u:1});go('settings/share')"%hid);await A.wait_for_timeout(500)
    ok(await A.evaluate("shById('old1').status==='legacy'&&shById('old2').status==='ended'&&!!document.querySelector('[data-act=shRedo]')"),'old Drive shares: offered “Share again”, stale invites cleared')
    await A.screenshot(path='/home/claude/scripts/x-share-legacy.png')
    ok(not [d for d in DENIED if d not in('write someone else’s progress','delete circle')],'no unexpected permission denials: '+str(DENIED))
    print('errors',A.errs+B.errs);ok(not(A.errs+B.errs),'no page errors')
    print(f'{ok_n[0]} passed, {ok_n[1]} failed');await b.close()
if __name__=="__main__":asyncio.run(main())
