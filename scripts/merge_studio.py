# re-merge scripts/studio_art.js + studio.js + studio.css into plotline.html (replaces the previous merged copy)
p='/home/claude/plotline.html';s=open(p).read()
js=open('/home/claude/scripts/studio.js').read();js=js.replace('/* ================= LIVE HOME: STUDIO =================','/* ================= LIVE HOME: STUDIO =================*/\n'+open('/home/claude/scripts/studio_art.js').read()+'/*',1)
css=open('/home/claude/scripts/studio.css').read()
a=s.index('/* ================= LIVE HOME: STUDIO')
END='/* ================= END STUDIO ================= */'
if END in s: b=s.index(END)+len(END)+1
else:
    b=s.index("homeStyle:d=>{");b=s.index('\n',s.index('});',b))+1
s=s[:a]+js+s[b:]
a=s.index('/* ================= live home: studio');b=s.index('/* shared item panel */')
s=s[:a]+css+s[b:]
open(p,'w').write(s)
