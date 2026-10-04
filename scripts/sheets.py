from PIL import Image
import glob,os,sys
pat=sys.argv[1];W=int(sys.argv[2]) if len(sys.argv)>2 else 390
fs=sorted(glob.glob(pat),key=os.path.getmtime)
for k in range(0,len(fs),6):
    ims=[Image.open(f) for f in fs[k:k+6]];H=1500 if W<500 else 900
    o=Image.new('RGB',((W+10)*len(ims),H),'white')
    for j,i in enumerate(ims):o.paste(i.crop((0,0,W,min(H,i.size[1]))),(j*(W+10),0))
    sc=.6 if W<500 else .3
    o.resize((int(o.size[0]*sc),int(H*sc))).save(f'/tmp/claude-0/ui/sheet{k//6}.png');print(k//6,[os.path.basename(f)[:-4] for f in fs[k:k+6]])
