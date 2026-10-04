# Plotline native (2.x)

Native Android rewrite of Plotline 1.13 (the all-web app at https://munazzar.github.io/plotline/).
Goal: every screen, feature and behaviour matches 1.13 exactly, improved only where the owner agrees.

## Layout
- `plotline.html` — the web app (1.13 UI + engine hooks). Runs hidden inside the APK as the "engine" (sync, AI, sharing crypto, reminders, widgets).
- `scripts/v110.js`, `scripts/v110.css` — module sources; `python3 scripts/merge_110.py` merges them into plotline.html. `apkbuild/build.sh` does NOT merge.
- `apkbuild/src/com/munazzar/plotline/*.java` — the native UI (NShell, NPage subclasses, N* views/sheets).
- `apkbuild/app/` — apktool-decoded app (manifest, res, assets, native libs).
- `ci/` — screenshot comparison: `web_shots.py` (1.13 web, makes the shared sample data), `emu.py` (emulator), `compare.py` (side-by-side), `scenarios.py` (screens).

## Rules
- Java: no lambdas or method references (dex2jar can't dex invokedynamic), framework views only (no AndroidX), compile against API 30, minSdk 29.
- Never commit `*.jks` / `*keypass*` (release signing happens off CI with plotline-upload.jks; cert SHA-256 7B:B1:3E:1A:…:9E:48:99).
- Surgical, minimal changes; verify against the 1.13 source/screens before calling something matched.

## Build
`sh scripts/get-tools.sh` once, then `sh apkbuild/build.sh` (release-signed if the key is present, else debug-signed; `DEBUGGABLE=1` for the emulator build).
Release: bump APP_VER (plotline.html), versionCode/versionName (apkbuild/app/AndroidManifest.xml + apktool.yml), CACHE (apkbuild/web/sw.js).

## Compare loop
Every push to `main` runs `.github/workflows/compare.yml`: builds a debuggable APK, screenshots 1.13 web and the native app with the same sample data, and force-pushes `ci/out` to the `shots` branch (`cmp/<screen>-<page>.png`: left web, right native; `native/*.xml` view trees; `native/errors.txt`).
