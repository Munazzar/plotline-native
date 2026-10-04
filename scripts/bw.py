# Copies /home/claude/plotline.html into the web build and the APK assets,
# swapping the Google Fonts links for the bundled local @font-face block.
import re,os
import os as _o;ROOT=_o.environ.get('PLOTLINE_ROOT') or _o.path.dirname(_o.path.dirname(_o.path.abspath(__file__)))
D=os.path.dirname(os.path.abspath(__file__))
src=open(ROOT+'/plotline.html').read()
fb=open(D+'/fontblock.html').read().rstrip('\n')
out=re.sub(r'<link rel="preconnect" href="https://fonts.googleapis.com">\n<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>\n<link href="https://fonts.googleapis.com/css2[^\n]*>',lambda m:fb,src)
assert out!=src,'font links not found'
for p in [ROOT+'/apkbuild/web/index.html',ROOT+'/apkbuild/app/assets/www/index.html']:open(p,'w').write(out)
import shutil
shutil.copytree(ROOT+'/apkbuild/web/lib',ROOT+'/apkbuild/app/assets/www/lib',dirs_exist_ok=True)
shutil.copytree(ROOT+'/apkbuild/web/fonts',ROOT+'/apkbuild/app/assets/www/fonts',dirs_exist_ok=True)
for f in os.listdir(D+'/static'):
    for dst in [ROOT+'/apkbuild/web/',ROOT+'/apkbuild/app/assets/www/']:shutil.copy(D+'/static/'+f,dst+f)
print('built',len(out))
