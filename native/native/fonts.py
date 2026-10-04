# Converts the web fonts (woff2) to TTF for the native app: assets/nfonts/<file>.ttf
import os,glob
from fontTools.ttLib import TTFont
src='/home/claude/apkbuild/web/fonts';dst='/home/claude/apkbuild/app/assets/nfonts';os.makedirs(dst,exist_ok=True)
n=0
for f in sorted(glob.glob(src+'/*.woff2')):
    out=os.path.join(dst,os.path.basename(f)[:-6]+'.ttf')
    if os.path.exists(out):continue
    t=TTFont(f);t.flavor=None;t.save(out);n+=1
print('converted',n,'total',len(os.listdir(dst)))
