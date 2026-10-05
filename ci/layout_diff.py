# Compares where text sits on the 1.13 web page and on the native screen, scenario by scenario.
# Web: ci/out/web/<name>.rects.json (CSS px, page coordinates). Native: ci/out/native/<name>.xml (uiautomator, device px).
# Texts are matched by their words (case-insensitive). Positions are compared after removing the typical vertical
# offset (the status bar and anything above that shifts the whole page), so each line shows a local difference.
# Writes ci/out/layout/<name>.txt and ci/out/layout.md (worst differences first, and web texts missing on native).
import glob, json, os, re, statistics, sys

D = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'out')
DPR = 2.625
SCREEN_H = 915


def norm(t):
    return re.sub(r'[^0-9a-z%/:.+]+', ' ', t.lower().replace('’', "'")).strip()


def native_nodes(path):
    xml = open(path, encoding='utf-8', errors='replace').read()
    out = []
    for m in re.finditer(r'<node [^>]*?text="([^"]*)"[^>]*?content-desc="([^"]*)"[^>]*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml):
        t = m.group(1) or ''
        if not t.strip(): continue
        x1, y1, x2, y2 = (int(v) / DPR for v in m.groups()[2:])
        t = t.replace('&amp;', '&').replace('&quot;', '"').replace('&#10;', ' ').replace('&apos;', "'")
        out.append({'t': t, 'x': x1, 'y': y1, 'w': x2 - x1, 'h': y2 - y1})
    return out


def diff(name):
    wp, np_ = f'{D}/web/{name}.rects.json', f'{D}/native/{name}.xml'
    if not (os.path.exists(wp) and os.path.exists(np_)): return None
    web = [r for r in json.load(open(wp)) if r['y'] < SCREEN_H * 1.0 and norm(r['t'])]
    nat = [n for n in native_nodes(np_) if norm(n['t'])]
    used, pairs, missing = set(), [], []
    for r in web:
        k = norm(r['t']); best = None
        for i, n in enumerate(nat):
            if i in used: continue
            nk = norm(n['t'])
            if nk == k or (len(k) > 12 and (nk.startswith(k) or k.startswith(nk))):
                if best is None or abs(n['y'] - r['y']) < abs(nat[best]['y'] - r['y']): best = i
        if best is None: missing.append(r); continue
        used.add(best); pairs.append((r, nat[best]))
    if not pairs: return {'name': name, 'pairs': [], 'missing': missing, 'extra': nat, 'off': 0, 'score': 0}
    cy = lambda o: o['y'] + o['h'] / 2
    off = statistics.median(cy(n) - cy(r) for r, n in pairs)
    rows = []
    for r, n in pairs:
        # vertical: centres (line boxes differ); horizontal: whichever edge or centre lines up best (text gravity differs)
        dy = (cy(n) - cy(r)) - off; dh = n['h'] - r['h']; dw = n['w'] - r['w']
        dx = min((n['x'] - r['x'], (n['x'] + n['w'] / 2) - (r['x'] + r['w'] / 2), (n['x'] + n['w']) - (r['x'] + r['w'])), key=abs)
        rows.append((max(abs(dy), abs(dx)), r, n, dx, dy, dw, dh))
    rows.sort(key=lambda z: -z[0])
    extra = [n for i, n in enumerate(nat) if i not in used]
    return {'name': name, 'rows': rows, 'missing': missing, 'extra': extra, 'off': off, 'score': sum(z[0] for z in rows) / len(rows)}


def main():
    os.makedirs(D + '/layout', exist_ok=True)
    names = sorted(os.path.basename(p)[:-11] for p in glob.glob(D + '/web/*.rects.json'))
    if len(sys.argv) > 1: names = [n for n in names if any(a in n for a in sys.argv[1:])]
    summary = ['# Layout differences (dp; native minus web, after the page offset)', '']
    for nm in names:
        d = diff(nm)
        if not d or 'rows' not in d: continue
        L = [f'{nm}: page offset {d["off"]:.0f}dp, {len(d["rows"])} texts matched, mean |diff| {d["score"]:.1f}dp', '',
             '  diff   dx    dy    dw    dh   web(x,y,w,h fs)            text']
        for z, r, n, dx, dy, dw, dh in d['rows']:
            L.append(f'{z:6.0f} {dx:5.0f} {dy:5.0f} {dw:5.0f} {dh:5.0f}   ({r["x"]:.0f},{r["y"]:.0f},{r["w"]:.0f},{r["h"]:.0f} {r["fs"]:.1f}{"b" if str(r["fw"]) >= "600" else ""})  {r["t"][:60]}')
        if d['missing']:
            L += ['', 'on the web but not found on native (first screen):'] + [f'   ({r["x"]:.0f},{r["y"]:.0f}) <{r["tag"]}.{r["cls"]}> {r["t"][:70]}' for r in d['missing']]
        if d['extra']:
            L += ['', 'on native only:'] + [f'   ({n["x"]:.0f},{n["y"]:.0f}) {n["t"][:70]}' for n in d['extra']]
        open(f'{D}/layout/{nm}.txt', 'w').write('\n'.join(L) + '\n')
        worst = ', '.join(f'"{r["t"][:24]}" {dy:+.0f}/{dx:+.0f}' for z, r, n, dx, dy, dw, dh in d['rows'][:3] if z >= 6)
        summary.append(f'- **{nm}** mean {d["score"]:.1f}dp · missing {len(d["missing"])} · worst: {worst or "—"}')
    open(D + '/layout.md', 'w').write('\n'.join(summary) + '\n')
    print('\n'.join(summary))


main()
