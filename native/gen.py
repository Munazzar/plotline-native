# Generates apkbuild/src/com/munazzar/plotline/NGen.java from the web app: themes, fonts, areas and icons
# (icons converted to absolute M/L/C/Z paths so Java only needs a tiny parser).
import re,math,json,os
S=open('/home/claude/plotline.html').read()+open('/home/claude/scripts/fontblock.html').read()
out=[]
def col(v,frost=.5):
    v=v.strip()
    v=v.replace('var(--frost,.5)',str(frost));v=re.sub(r'calc\(([^()]*)\)',lambda m:str(eval(m.group(1))),v)
    if v=='transparent':return 0
    m=re.match(r'#([0-9a-fA-F]{3,8})$',v)
    if m:
        h=m.group(1)
        if len(h)==3:h=''.join(c*2 for c in h)
        if len(h)==6:h='ff'+h
        elif len(h)==8:h=h[6:]+h[:6]
        return int(h,16)
    m=re.match(r'rgba?\(([^)]*)\)',v)
    if m:
        p=[float(x) for x in re.split(r'[ ,/]+',m.group(1).strip()) if x]
        a=p[3] if len(p)>3 else 1
        return (round(a*255)<<24)|(int(p[0])<<16)|(int(p[1])<<8)|int(p[2])
    raise Exception('color '+v)
# ---- themes
names=dict(re.findall(r"\['([a-z]+)','([^']+)','#[0-9A-Fa-f]+'",S))
TH=[]
for m in re.finditer(r'\[data-theme=([a-z]+)\]\{color-scheme:(dark|light);([^}]*)\}',S):
    tid,mode,body=m.groups()
    if tid in [t[0] for t in TH]:continue
    v=dict(re.findall(r'--([a-z0-9-]+):([^;]+)',body))
    keys=['bg','bg-2','surface','surface-2','line','line-2','text','muted','dim','accent','on-accent']
    TH.append((tid,names.get(tid,tid.title()),mode=='light',[col(v[k]) for k in keys]))
# ---- fonts
FONTS=json.loads(re.search(r'const FONTS=(\[\[.*?\]\]);',S).group(1))
faces=re.findall(r"@font-face\{font-family:'([^']+)';font-weight:([0-9 ]+);font-display:swap;src:url\(fonts/([^)]+)\.woff2\)",S)
fam={}
for f,w,file in faces:fam.setdefault(f,[]).append((w,file))
PRE=re.findall(r"\['([a-z]+)','([^']+)','([a-z0-9]+)','([a-z0-9]+)','([a-z0-9]+)'\]",re.search(r'const FONT_PRE=(\[.*?\]\]);',S).group(1))
# ---- icons
def arc2c(x1,y1,rx,ry,phi,fa,fs,x2,y2):
    if rx==0 or ry==0:return [('L',x2,y2)]
    phi=math.radians(phi);cp,sp=math.cos(phi),math.sin(phi)
    dx,dy=(x1-x2)/2,(y1-y2)/2;x1p=cp*dx+sp*dy;y1p=-sp*dx+cp*dy
    rx,ry=abs(rx),abs(ry);lam=x1p**2/rx**2+y1p**2/ry**2
    if lam>1:rx*=math.sqrt(lam);ry*=math.sqrt(lam)
    num=rx*rx*ry*ry-rx*rx*y1p*y1p-ry*ry*x1p*x1p;den=rx*rx*y1p*y1p+ry*ry*x1p*x1p
    co=math.sqrt(max(0,num/den)) if den else 0
    if fa==fs:co=-co
    cxp=co*rx*y1p/ry;cyp=-co*ry*x1p/rx
    cx=cp*cxp-sp*cyp+(x1+x2)/2;cy=sp*cxp+cp*cyp+(y1+y2)/2
    def ang(ux,uy,vx,vy):
        a=math.atan2(ux*vy-uy*vx,ux*vx+uy*vy);return a
    t1=ang(1,0,(x1p-cxp)/rx,(y1p-cyp)/ry);dt=ang((x1p-cxp)/rx,(y1p-cyp)/ry,(-x1p-cxp)/rx,(-y1p-cyp)/ry)
    if not fs and dt>0:dt-=2*math.pi
    if fs and dt<0:dt+=2*math.pi
    n=max(1,int(math.ceil(abs(dt)/(math.pi/2)-1e-9)));d=dt/n;res=[]
    k=4/3*math.tan(d/4)
    for i in range(n):
        a1=t1+i*d;a2=a1+d
        def pt(a):return (cx+rx*math.cos(a)*cp-ry*math.sin(a)*sp, cy+rx*math.cos(a)*sp+ry*math.sin(a)*cp)
        def dv(a):return (-rx*math.sin(a)*cp-ry*math.cos(a)*sp, -rx*math.sin(a)*sp+ry*math.cos(a)*cp)
        p1=pt(a1);p2=pt(a2);d1=dv(a1);d2=dv(a2)
        res.append(('C',p1[0]+k*d1[0],p1[1]+k*d1[1],p2[0]-k*d2[0],p2[1]-k*d2[1],p2[0],p2[1]))
    return res
def norm_path(d):
    toks=re.findall(r'[MmLlHhVvCcSsQqTtAaZz]|-?(?:\d*\.\d+|\d+\.?)(?:e-?\d+)?',d)
    i=0;cmd=None;x=y=sx=sy=0;out=[];lc=None;lq=None
    def num():
        nonlocal i;v=float(toks[i]);i+=1;return v
    while i<len(toks):
        t=toks[i]
        if re.match(r'[A-Za-z]',t):cmd=t;i+=1
        elif cmd is None:raise Exception('bad path')
        C=cmd.upper();rel=cmd.islower()
        if C=='Z':out.append(('Z',));x,y=sx,sy;lc=lq=None;continue
        if C=='M':
            nx,ny=num(),num()
            if rel:nx+=x;ny+=y
            out.append(('M',nx,ny));x,y=sx,sy=nx,ny;cmd='l' if rel else 'L';lc=lq=None
        elif C=='L':
            nx,ny=num(),num()
            if rel:nx+=x;ny+=y
            out.append(('L',nx,ny));x,y=nx,ny;lc=lq=None
        elif C=='H':
            nx=num()+(x if rel else 0);out.append(('L',nx,y));x=nx;lc=lq=None
        elif C=='V':
            ny=num()+(y if rel else 0);out.append(('L',x,ny));y=ny;lc=lq=None
        elif C=='C':
            a=[num() for _ in range(6)]
            if rel:a=[a[0]+x,a[1]+y,a[2]+x,a[3]+y,a[4]+x,a[5]+y]
            out.append(('C',*a));lc=(a[2],a[3]);x,y=a[4],a[5];lq=None
        elif C=='S':
            a=[num() for _ in range(4)]
            if rel:a=[a[0]+x,a[1]+y,a[2]+x,a[3]+y]
            c1=(2*x-lc[0],2*y-lc[1]) if lc else (x,y)
            out.append(('C',c1[0],c1[1],*a));lc=(a[0],a[1]);x,y=a[2],a[3];lq=None
        elif C=='Q':
            a=[num() for _ in range(4)]
            if rel:a=[a[0]+x,a[1]+y,a[2]+x,a[3]+y]
            out.append(('C',x+2/3*(a[0]-x),y+2/3*(a[1]-y),a[2]+2/3*(a[0]-a[2]),a[3]+2/3*(a[1]-a[3]),a[2],a[3]));lq=(a[0],a[1]);x,y=a[2],a[3];lc=None
        elif C=='T':
            a=[num(),num()]
            if rel:a=[a[0]+x,a[1]+y]
            q=(2*x-lq[0],2*y-lq[1]) if lq else (x,y)
            out.append(('C',x+2/3*(q[0]-x),y+2/3*(q[1]-y),a[0]+2/3*(q[0]-a[0]),a[1]+2/3*(q[1]-a[1]),a[0],a[1]));lq=q;x,y=a;lc=None
        elif C=='A':
            rx,ry,ph=num(),num(),num();fa=int(num());fs=int(num());nx,ny=num(),num()
            if rel:nx+=x;ny+=y
            out+=arc2c(x,y,rx,ry,ph,fa,fs,nx,ny);x,y=nx,ny;lc=lq=None
    f=lambda v:('%.2f'%v).rstrip('0').rstrip('.')
    return ' '.join(s[0]+(' '+' '.join(f(v) for v in s[1:]) if len(s)>1 else '') for s in out)
def attrs(t):return dict(re.findall(r'([a-z-]+)="([^"]*)"',t))
ICONS={}
for k,v in re.findall(r"([a-zA-Z0-9_]+):'(<(?:path|circle|rect)[^']*)'",S):
    parts=[]
    for el,a in re.findall(r'<(path|circle|rect)([^>]*)/?>',v):
        A=attrs(a);fill='fill' in A and A['fill'] not in ('none',)
        if el=='path':d=A['d']
        elif el=='circle':
            cx,cy,r=float(A['cx']),float(A['cy']),float(A['r']);d=f'M{cx-r} {cy}a{r} {r} 0 1 0 {2*r} 0a{r} {r} 0 1 0 {-2*r} 0z'
        else:
            x,y,w,h=float(A.get('x',0)),float(A.get('y',0)),float(A['width']),float(A['height']);r=float(A.get('rx',0))
            d=f'M{x+r} {y}h{w-2*r}a{r} {r} 0 0 1 {r} {r}v{h-2*r}a{r} {r} 0 0 1 {-r} {r}h{-(w-2*r)}a{r} {r} 0 0 1 {-r} {-r}v{-(h-2*r)}a{r} {r} 0 0 1 {r} {-r}z' if r else f'M{x} {y}h{w}v{h}h{-w}z'
        try:parts.append(('f' if fill else 's')+norm_path(d))
        except Exception as e:print('skip',k,e)
    if parts and k not in ICONS:ICONS[k]='|'.join(parts)
AREAS=re.findall(r"\{id:'([a-z]+)',name:'([^']+)'\}",re.search(r'const AREAS=(\[.*?\]);',S).group(1))
ACOL=dict(re.findall(r"([a-z]+):'(#[0-9A-F]{6})'",re.search(r'const ACOL=\{([^}]*)\}',S).group(1)))
j=lambda s:json.dumps(s,ensure_ascii=False)
L=['package com.munazzar.plotline;','','/* GENERATED by native/gen.py from plotline.html. Do not edit by hand. */','final class NGen {']
L.append('    /* id, name, light, then bg, bg2, surface, surface2, line, line2, text, muted, dim, accent, onAccent */')
L.append('    static final String[] THEME_ID = {'+','.join(j(t[0]) for t in TH)+'};')
L.append('    static final String[] THEME_NAME = {'+','.join(j(t[1]) for t in TH)+'};')
L.append('    static final boolean[] THEME_LIGHT = {'+','.join('true' if t[2] else 'false' for t in TH)+'};')
L.append('    static final int[][] THEME_COL = {'+','.join('{'+','.join('0x%08X'%c for c in t[3])+'}' for t in TH)+'};')
L.append('    /* fonts: id, label, role (d/b/m), files "weight:file;weight:file" (weight "100 900" = variable) */')
FL=[]
for fid,lab,fam_,role in FONTS:
    files=';'.join(f'{w}:{fl}' for w,fl in fam.get(fam_,[]))
    FL.append((fid,lab,role,files))
L.append('    static final String[][] FONTS = {'+','.join('{'+','.join(j(x) for x in f)+'}' for f in FL)+'};')
L.append('    static final String[][] FONT_PRE = {'+','.join('{'+','.join(j(x) for x in p)+'}' for p in PRE)+'};')
L.append('    static final String[] AREA_ID = {'+','.join(j(a[0]) for a in AREAS)+'};')
L.append('    static final String[] AREA_NAME = {'+','.join(j(a[1]) for a in AREAS)+'};')
L.append('    static final int[] AREA_COL = {'+','.join('0xFF'+ACOL[a[0]][1:] for a in AREAS)+'};')
keys=sorted(ICONS)
L.append('    static final String[] ICON_ID = {'+','.join(j(k) for k in keys)+'};')
# icon data can exceed constant-pool string limits if joined; keep one string per icon
L.append('    static final String[] ICON_D = {'+','.join(j(ICONS[k]) for k in keys)+'};')
L.append('}')
open('/home/claude/apkbuild/src/com/munazzar/plotline/NGen.java','w').write('\n'.join(L)+'\n')
print('themes',len(TH),'fonts',len(FL),'icons',len(keys),'missing font files',[f[0] for f in FL if not f[3] and f[0]!='system'])
