# Puts each web (1.13) page next to the native page with the same name: ci/out/cmp/<name>-<k>.png
# (left = web 1.13, right = native), half size, and writes ci/out/index.md listing them.
import glob, os
from PIL import Image, ImageDraw

D = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'out')
os.makedirs(D + '/cmp', exist_ok=True)
names = sorted({os.path.basename(p)[:-4] for p in glob.glob(D + '/web/*-[0-9].png') + glob.glob(D + '/native/*-[0-9].png')})
lines = ['# 1.13 web (left) vs native (right)', '']
for n in names:
    ims = []
    for side in ('web', 'native'):
        p = f'{D}/{side}/{n}.png'
        im = Image.open(p).convert('RGB') if os.path.exists(p) else Image.new('RGB', (1080, 2400), (60, 0, 0))
        ims.append(im.resize((540, round(im.height * 540 / im.width))))
    h = max(i.height for i in ims)
    c = Image.new('RGB', (540 * 2 + 12, h + 40), (255, 255, 255))
    d = ImageDraw.Draw(c)
    d.text((8, 12), 'WEB 1.13  ' + n, fill=(0, 0, 0)); d.text((552 + 8, 12), 'NATIVE  ' + n, fill=(0, 0, 0))
    c.paste(ims[0], (0, 40)); c.paste(ims[1], (552, 40))
    c.save(f'{D}/cmp/{n}.png', optimize=True)
    lines.append(f'![{n}](cmp/{n}.png)')
open(D + '/index.md', 'w').write('\n'.join(lines) + '\n')
print('compared', len(names))
