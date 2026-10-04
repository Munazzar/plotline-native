# Runs on the GitHub runner while the Android emulator is up (inside android-emulator-runner's script).
# Installs the debuggable APK, gives it the same sample data the web build made (ci/out/state.json),
# opens every scenario route and screenshots it page by page into ci/out/native/<name>-<k>.png.
# Also saves the native view tree (uiautomator) per scenario and the app's logcat.
import json, os, re, subprocess, sys, time
sys.path.insert(0, os.path.dirname(__file__))
from scenarios import scenarios

D = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(D, 'out')
PKG = 'com.munazzar.plotline'
APK = sys.argv[1] if len(sys.argv) > 1 else os.path.join(D, '..', 'apkbuild', 'out', 'Plotline-debug.apk')


def sh(*a, inp=None, check=False):
    r = subprocess.run(['adb'] + list(a), input=inp, capture_output=True)
    if check and r.returncode: print('adb failed', a, r.stderr.decode()[:400])
    return r.stdout


def shot(path):
    open(path, 'wb').write(sh('exec-out', 'screencap', '-p'))


def tree():
    sh('shell', 'uiautomator', 'dump', '/sdcard/ui.xml')
    return sh('shell', 'cat', '/sdcard/ui.xml').decode('utf-8', 'replace')


def tap(spec):
    """tap the first on-screen view whose text or content-description matches"""
    kind, arg = spec.split(':', 1)
    xml = tree()
    attr = 'text' if kind == 'text' else 'content-desc'
    for m in re.finditer(r'<node [^>]*?' + attr + r'="([^"]*)"[^>]*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml):
        x1, y1, x2, y2 = map(int, m.groups()[1:])
        if m.group(1) == arg and y2 < 2400 - 330:   # not the bottom tab bar
            sh('shell', 'input', 'tap', str((x1 + x2) // 2), str((y1 + y2) // 2)); return True
    print('native tap: nothing matches', spec); return False


def main():
    os.makedirs(OUT + '/native', exist_ok=True)
    state = json.load(open(OUT + '/state.json'))
    sh('install', '-r', '-g', APK, check=True)
    for s in ['window_animation_scale', 'transition_animation_scale', 'animator_duration_scale']:
        sh('shell', 'settings', 'put', 'global', s, '1')
    # first launch creates the app's folders, then the shared sample data goes in
    sh('shell', 'am', 'start', '-W', '-n', PKG + '/.MainActivity'); time.sleep(10)
    shot(OUT + '/native/00-first-run.png')
    sh('shell', 'am', 'force-stop', PKG)
    sh('shell', f"run-as {PKG} sh -c 'mkdir -p files; cat > files/state.json'", inp=json.dumps(state).encode(), check=True)
    print('seeded', sh('shell', f"run-as {PKG} ls -la files").decode())
    sh('logcat', '-c')
    sh('shell', 'am', 'start', '-W', '-n', PKG + '/.MainActivity'); time.sleep(14)
    shot(OUT + '/native/00-launch.png')
    for name, route, steps in scenarios(state):
        sh('shell', 'am', 'start', '-n', PKG + '/.MainActivity', '--es', 'route', route); time.sleep(3.5)
        for _web, nat in steps:
            if nat: tap(nat); time.sleep(2)
        open(f'{OUT}/native/{name}.xml', 'w').write(tree())
        prev = None
        for k in range(8):
            shot(f'{OUT}/native/{name}-{k}.png')
            # slow drag (no fling): 1470 px = 560 dp, the same step the web pages use
            sh('shell', 'input', 'swipe', '540', '1900', '540', '430', '1800'); time.sleep(1.2)
            cur = tree()
            if cur == prev: break
            prev = cur
        # back to the top for the next route
        for _ in range(3): sh('shell', 'input', 'swipe', '540', '500', '540', '2000', '150')
    open(OUT + '/native/logcat.txt', 'wb').write(sh('logcat', '-d'))
    pid = sh('shell', 'pidof', PKG).decode().strip()
    errs = [l for l in open(OUT + '/native/logcat.txt', encoding='utf-8', errors='replace')
            if ('FATAL' in l or ' E AndroidRuntime' in l or (pid and f' {pid} ' in l and (' E ' in l or ' W ' in l and 'Exception' in l)) or 'PlotlineNative' in l or 'nshell' in l or ('CONSOLE' in l and ('Error' in l or 'Uncaught' in l))) and 'uiautomator' not in l]
    open(OUT + '/native/errors.txt', 'w').writelines(errs)


main()
