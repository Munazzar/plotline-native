# Plotline — start here (new session)

Owner: Munazzar. App: Plotline (life goals, habits, journal, threads). Android + web, one Google Drive file.
Latest build: **2.0.9** (versionCode 61; still never run on a device by me — owner tests and sends screenshots). Previous all-web build: 1.13.0 (versionCode 51).

## 1. Files to attach to the new session
| File | What it is |
|---|---|
| `plotline-source.zip` | Everything except tools and key: `plotline.html` (web app, single file), `scripts/` (module sources + tests), `native/` (generators, CI), `apkbuild/` (apktool-decoded app + Java sources), `HANDOFF.md` (full history), this file |
| `plotline-tools-1-apktool.zip`, `plotline-tools-2-android-jar.zip`, `plotline-tools-3-dex-signer.zip` | together = `apkbuild/tools/` — android-30.jar, apktool.jar, d2j (dex2jar 2.4), uber-signer.jar. Google's Maven is blocked in the sandbox, so these must be supplied |
| `plotline-signing-key.zip` | `plotline-upload.jks` + `plotline-keypass.txt` (store and key password, alias `plotline`). Cert SHA-256 `7B:B1:3E:1A:…:9E:48:99`. **Every update must be signed with this or phones refuse to update.** Never commit it or put it in the source zip |
| (optional) `Plotline-1.13.0.apk` | Only needed to compare old screens; the 1.13 web UI is still inside `plotline.html` |

## 2. Restore the workspace (paste into the new session)
```
cd /home/claude
unzip -o plotline-source.zip            # -> plotline.html, HANDOFF.md, NEXT-SESSION.md, scripts/, native/, apkbuild/
for f in plotline-tools-*.zip; do unzip -o $f -d apkbuild/; done   # -> apkbuild/tools/...
unzip -o plotline-signing-key.zip -d apkbuild/   # -> apkbuild/plotline-upload.jks, plotline-keypass.txt
mkdir -p www && ln -sf ../plotline.html www/index.html && ln -sf /home/claude/apkbuild/web/fonts www/fonts && ln -sf /home/claude/apkbuild/web/lib www/lib
(cd www && setsid nohup python3 -m http.server 8765 >/dev/null 2>&1 &)   # test server for Playwright
pip install --break-system-packages fonttools brotli   # only if regenerating fonts
```
Paths are hard-coded to `/home/claude/...` in scripts.

## 3. Build and release
1. Web/JS changes: edit `scripts/v110.js` / `scripts/v110.css` (insert new code ABOVE the END marker — use `python3 scripts/add.py v110.js < snippet`), then `python3 scripts/merge_110.py` (merges into `plotline.html`, applies `once()` core patches, runs a JS syntax check).
2. Java changes: `apkbuild/src/com/munazzar/plotline/*.java`. Rules: **no lambdas or method references** (dex2jar can't dex invokedynamic), nothing above API 30 unless via reflection, framework views only (no AndroidX).
3. Bump version in 4 places: `const APP_VER=` in plotline.html, `versionCode`/`versionName` in `apkbuild/app/AndroidManifest.xml` and `apkbuild/app/apktool.yml`, `CACHE` in `apkbuild/web/sw.js`.
4. `sh apkbuild/build.sh` → `apkbuild/out/Plotline-<ver>.apk` (signed, check SHA-256 7bb13e1a…) and `apkbuild/out/plotline-web.zip` (GitHub Pages site).
5. Regenerate native theme/font/icon tables after CSS/icon changes: `python3 native/gen.py` (writes NGen.java); fonts: `python3 native/fonts.py`.

## 4. Tests
- Web: `scripts/t52.py t50.py t51.py t45.py t49.py t41.py t37.py` (Playwright, server on :8765). `t53.py` = native-store bridge. `contrast2.py <themes>` = pixel contrast audit. `audit.py` = layout overflow.
- Java logic vs JS: harness in HANDOFF 2.0.0 section (compiles NDates/NHabits/NMerge with org.json from github.com/stleary/JSON-java and compares to values dumped from the web app). Last run: 0 diffs.
- Device: **never run on a phone or emulator yet.** Branch `native-ci` (in a local clone of github.com/Munazzar/plotline) has `.github/workflows/native-ci.yml` + `ci/run-emulator.sh` that installs a debuggable copy, seeds demo data and screenshots every native screen, committing them back. Push was refused because the **Claude GitHub App is not installed on Munazzar/plotline** — install it (github.com/apps/claude/installations/select_target), then push `native-ci`. Files for it are also in `native/ci/`.

## 5. Architecture of 2.0.0 (short)
- `MainActivity` hosts the old WebView (hidden, "engine") + `NShell` (native UI on top).
- Data: `NStore` = one JSON doc in `files/state.json`, same format as the web. Native edits → `changed()` stamps `u`/tombstones → JS `__nativeChanged()`. JS saves → `Bridge.storeSet` → `NStore.fromWeb` (merge by id/u).
- Engine still does: Google Drive sync, reminders, widgets, widget queue, AI, sharing, lock screen, and every screen not yet native ("classic layer", opened with `NShell.openClassic(route)` under the native tab bar).
- Safety: `NCrash` (error card with copy, two crashes in a row → classic app).

## 6. Feature parity — 1.13.0 vs native 2.0.0
Native now: Home (greeting, quick actions, your-day ring, up next, today's goals, habit tiles, activity tiles, threads, coming up), Habits list (today/build/break/all, week strip, week dots), Habit detail (hero, check/±, rest day, stats, month calendar, slips, pause/archive/delete), Goals grid (filters, areas), Goal detail (steps tick/edit/add, sub-goal list, moments, pin/achieve/delete), Calendar (month dots, day agenda incl. habits), Journal (moments by day, threads list), Thread detail (add/edit/delete updates), sheets for goal/step/habit/moment/thread/day goal, + menu, swipe pages, edge-swipe back, toasts.

**Still classic (web) — port these next, in this order:**
1. Settings (all: account/sync connect, look & feel themes/colours/background/fonts/card style/halo/animations, start page, notifications, privacy & lock, automations, sharing, AI & voice, data backup/import) — and native theme picker.
2. Ask + Ask overlay + Plan with AI (wizard, plan preview/apply, rework plan) + insights cards.
3. Home "Studio" live room (selectable home style) + weekly review + check-in flows.
4. Vista (timeline road + map), chapters.
5. Day page (past day, everything editable) + Activity page (steps/sleep/workouts/screen/places, 7/14/30 day charts, tap a bar).
6. Habit extras: routines player, urge timer, limit-mode details, templates, habit vista grid, reminders/bells editing, auto check-off rules, notification history, reactions/cheers, shared habits/groups/live share, habit week chart tap.
7. Goal extras: focus timer, reminders per step, repeat steps editor, goal links/connected goals, sub-goal creation, goal image/card backgrounds, check-ins, target reminders, card flip/pinch views, path/timeline goal views.
8. Journal extras: journal week bar/date picker, highlights/my moments/goals/habits/steps filters, flip cards, photos, voice entries, private entries editing, link threads to goals/habits/entries.
9. Global: search, focus/fullscreen mode, onboarding + tour (currently web), voice/mic input on fields, undo toasts, app lock natively.
10. Move engine jobs into Java: Drive sync (MainActivity.fetchToken already gets the AccountManager token), reminders (port syncNative/habitAlarms), widgets (port pushWidget) — then the WebView is only needed for anything left in classic.

### 2.0.1 additions (native)
Day page, Activity page, full Habits page (week strip, month calendar, summary, routines, templates/Vista chips), Home extras (insights, weekly review, plan card, pinned, quit clocks), Habit detail (weekday bars, 12-week heat map, slip triggers, reminders row, more-sheet actions), Goal detail (sub-goals, reminders, connect, ICS, share, duplicate), Goals page (search, Path/Timeline via web), Journal (filters, search, Year in review via web). Generic bridge `window.__nact` / `NShell.act(route, act, data, one)` opens any 1.13 web action from native; "All options in the full form" links on every native sheet.
Still web: Settings, Ask/Plan with AI, Vista, Studio live room, calendar week/day views, goal cards flip/pinch, onboarding/tour, global search, focus mode; richer native form fields.

## 7. Standing rules from the owner
- Surgical, minimal changes; verify against source; production quality; test before release.
- Ask before pushing to GitHub (Munazzar/plotline). Never commit/zip the signing key with the source.
- Sign every APK with plotline-upload.jks. Deliver APK + web zip + source zip each release, update HANDOFF.md (and the Project copy).

## 2.0.1 UI-fidelity pass (from user screenshots; NOT device-tested)
Done: header ibtn sizes (NUi.Fix, 40dp narrow/46), gear row (full-screen focus mode + settings), Habits page (seg filter, week strip, Monday-first month cal, summary ring/bars, dashed nudge, habit rows w/ flame+bell), new Habit detail (NHabitScreen mirrors web vHabit), goal cards (web .gc), Up-next NFlow, Home "Coming up" via NBits.li, Goals search, Journal filters + Year in review.
Helpers: NUi.Fix/Sq/dashed/mix, NBits.seg/li/bar(h), NFlow.
TODO: Home dayRow/todayGoals -> NBits.li (goal dot, time, bell, Move to today, tg-add field), hpills, Plan-with-AI card, pinned minis; Goals filter bar (seg + area select + grid/carousel seg) and vGoal detail; Journal/Calendar layouts. Need user's web 1.13 screenshots of Home, Goals, Goal detail, Journal, Calendar.
Rule: source zip must exclude signing key + apkbuild/tools.

## 2.0.2 (second UI-fidelity pass; NOT device-tested)
- Calendar (NCalPage rewritten): Day/Week/Month seg + Today btn, subtitle, month cells w/ titled chips + habit bar + mood + target flag, day peek card (items, habit pills, journal rows), week list, day timeline (lanes, zoom, now line), agenda list under the card. Not done: swipe between months, pinch zoom, tap-slot-to-time.
- Goals page: web seg (Active/Short/Long/Achieved), area dropdown (PopupMenu), grid/carousel icon seg (layout.goals), flip cards (NBits.flipCard), carousel mode.
- Goal detail: pin button, ancestor crumb, hero data line + small %, "Add a sub-goal" link, Cards/Path seg (NGoalSteps: big flip cards carousel; zig-zag spine), Timeline -> web layer (gvMode orbit), goal habits, Connected goals, Moments w/ Add.
- NPage body clipToPadding=false.
Still TODO: Home today's goals/hpills/Plan-with-AI/pinned/activity; Journal+Vista layouts; Settings, Ask/Plan with AI, Studio, onboarding; goal halo/card styles (Vista "Card style": Solid/Glow/Neon/Aurora…); web 1.13 screenshots of Home/Journal needed.

## 2.0.3 (native fidelity pass, untested on device)
- Habit rows: NBits.week clamps to <=178dp; NHabitsPage.subLine wraps via NFlow.
- Home: pinned minis 210x156; yourDay ring/Next row/Done button; todayGoals header ring + add row + 14-day carry ("Still open from earlier"); dayRow uses NBits.li with bell + "Move to today".
- Journal: Journal/Threads seg, h/v icon seg (layout.journal), filter seg (settings.jFilter), Year-in-review link, flip-card carousel.
- Calendar rewritten (day/week/month, agenda, timeline); Goals grid/carousel flip cards; Goal detail Cards/Path steps (NGoalSteps).
- Remaining gaps: Home habit tiles/mic/Threads parity; Journal mood strip date row; Calendar swipe/pinch/slot prefill; native Timeline mode; Halo/glow card styles; Vista/Settings/Ask/Plan-with-AI/Studio/onboarding still web layer.

## 2.0.4 (versionCode 56) — untested on device
- NEW native voice mic (NVoice): mic at the end of Goal/Step/Habit/Day title+why/note fields, Journal text, Home add-goal. On-device speech first; online only after one OK (settings.voice.cloud); permission via MainActivity.askPerms("nmic"); Voice.send routes ids starting "n" to NVoice.
- Home habits = horizontal strip of 168dp pills (web habitsStrip) with ring header "N OF M".
- Journal: mood strip (last 14 days), calendar jump button (DatePickerDialog) that scrolls carousel/list.
- Calendar: swipe left/right to change week/month, pinch zoom in day timeline, tap an empty slot prefills the time (NForms.dayGoal(date,x,preTime)).
- Card styles (settings.cards: solid/glow/neon/aurora/glass/index) via NCard.bg + NTheme.INK/INK_MUTED switching; applied to goal cards, hero, step cards, habit cards, Home pinned, journal cards. Approximation: no live blur, neon number outline/outer glow, aurora animation.
- Still web layer: Vista tab, Ask, Settings (+Vista panels), Plan with AI, Studio, Year review, onboarding, goal Timeline (orbit/road).

## 2.0.5 (versionCode 57) — untested on device
- NEW native Settings hub (NSettings): You & sync, Look & feel (themes, match device, reduce motion, home page, fonts, card style/background + image upload, halo prefs), Notifications, Privacy & lock, Automations/Sharing (status + "Open…" in web layer), AI & voice, Your data (export/import/MQC/erase/remove sample). Engine does the work via acts; REQ_NIMP=15 (import file), REQ_NIMG=16 (card image).
- NEW native Ask tab (NAskPage + NMd): intro, thread, sources sheet, scope pill, composer with mic. Polls engine `__nask`; sends via `__nasksend`. Pager order is now Home, Habits, Calendar, Goals, Vista(4), Journal(5), Ask(6).
- NEW native Vista (NVistaPage): Timeline (NRoad canvas: wavy lanes, planets, ship, NOW line, chapter bands, Weeks/Months/Years, Active/All, expand-all, jump-to-today) and Map (NMapView: areas/goals, drag nodes/areas persisted to g.mapPos/settings.areaPos, pinch zoom, spacing slider, fit/zoom, reset, selection card + Open, double-tap opens, halo per settings.halo).
- NEW goal detail Timeline mode native (NRoad single/multi via NRoad.tree/ppdFit).
- NEW native Chapters list/edit/delete (NChapters; S.chapters).
- Engine hooks in v110.js: __nsub, __nask, __nasksend, __naskpaste, ACT.nImport, ACT.nLock; __nativeChanged re-applies theme/fonts. scripts/t54.py tests them.
- Icons expand/collapse approximated with down/up (NGen has no vexpand/vcollapse).
- STILL WEB LAYER: Year in review slides (yearOpen), Plan with AI flow, Studio room, onboarding/tour, Automations & Sharing section bodies, Habit Vista chapter bands, per-goal cover images (g.img), halo glow on cards/timeline tracks, map "full screen" focus mode.
- Next: device-test screenshots from owner → fix; then port remaining web-layer screens in the list above.

## 2.0.6 (versionCode 58) — untested on device
Native now (was the web layer): per-goal cover images (card, hero, home, ink swap via NTheme.pushImg/popImg), halo glow (lane + map + cards), Vista chapter bands + chapter list/form, map full-screen, full goal form (NGoalForm), step form (quick-due chips, repeat, reminder), Year in review (NYear, engine hook __nyear), Habit Vista (NHabitVista, __nhv), habit day sheet (NHabitDay), full habit form + 20 templates (NHabitForm/NHabitTpl), Settings → Automations (sources, places, in-use; hook __nauto) and Sharing (switch, invites, shared, legacy; hook __nshare, ACT nShareOn), Plan with AI (NAiPlan; hooks __nai/__naiset/__naitab/__naitoggle/__naititle/__naimain/__naiprompt; ACT.parseAI guarded for no DOM), guided tour (NTour; sandbox in NStore.sandboxBegin/End; hooks __ntourBegin/__ntourEnd), first-run welcome + setup + tour offer (NWelcome; hooks __nonboard/__nsetup).
Still engine/web-rendered: Studio room (animated SVG scene; Quick action opens it on the web layer), weekly review, assistant settings, PIN set/remove, place save dialog, share-an-item flow sheets, Drive connect. Tour has no spotlight/animated finger (native screens, text cards only).
Tests: web t52 t50 t51 t45 t49 t41 t37 t53 t54 pass. Nothing here is device-tested.
Never push to GitHub without asking; never zip the signing key.

## 2.0.7 (versionCode 59) — more of the web layer made native (NOT device-tested)
New/changed native: NReminders (bell sheets for habit/step/day/goal, hook __nrem), NAutoEdit (⚡ auto check-off sheet), NTimer (focus timer pill, pick sheet, "Time's up", hook __ntimerfin), NEntryForm (journal moment writer: same-day pills, mood, B/I/U/H/•/Tx toolbar stored as web `html`, Prompt via __nprompt, date (max today), goal, photo, private, 650ms autosave), step form (focus-timer chips, Earlier/Later, Add to calendar), day-goal form (Date, How long), NForms.connect (two-way goal links), native calendar export (NSheets.ics + hook __nics for icsGoal/icsCheckin/icsAll/icsStep → Bridge.saveFile).
The `fullLink` "All options in the full form" rows for step, day and moment are gone.
Tests: t52 t50 t51 t45 t49 t41 t37 t53 t54 pass (run individually; t54 now covers __nics, __nprompt, __ntimerfin).
Still on the web layer (engine shown via sh.act(...,true)): threads-from (thrFrom), urge timer, create-a-routine (hBlank), Studio room, syncConnect/syncOff/wipe/lockTest/demoRemove, openClassic("goal/..","habit/..") fallbacks. Silent engine calls still used: dupGoal, hDup, askAbout, routineGo.
Notes: the bullet button inserts "• " text (not an html list); highlight is yellow background stored as <mark>. Per-step ⚡ entry point is only via the goal menu "Auto check-off".

## 2.0.8 (versionCode 60) — web layer migration finished (NOT device-tested)
Every screen and sheet the user can reach is now native; the web engine stays hidden and only does sync, crypto/sharing network, AI, reminders and widgets.
How the engine is driven now (scripts/v110.js, end of file):
- `sh.run(act, data)` → `window.__nrun`: runs a web ACT with the web layer hidden. While hidden: `go()` is forwarded to `NShell.route` (NATIVE.nroute, retries until the store has the new item), `toast()` → native toast (with Undo), `askConfirm()` → native NSheets.confirm (NATIVE.nconfirm → __nconfYes), `burst()` → NFx.burst, `acctChoice` → native sheet, `reactShow/reactBurst` → NShare.banner/burst, `finishTimer` left to NTimer, `welcome/setupFlow/showLock/tourPrompt` off (native has its own).
- Safety net: any web sheet that still opens while hidden calls NATIVE.nneedUi() and the web layer comes up with it (console.warn 'nshell: web sheet …' names it).
- `Bridge.classicOn()` tells the engine whether the web layer is showing.
New native files: NEng (journal viewer, Ask scope, sync off, account switch, erase), NUrge (urge breathing timer, slip log, routine runner), NFx (burst, beep, vibrate, reduced motion), NShare (invites, accept/join, live group view, invite more, reaction picker, quick messages, Settings reactions panel, shared-item panel + chip on goal/habit, Today rows, banner, join route), NLock (lock screen: logo, biometric, PIN, forgot PIN), NStudio (Studio room).
NStudio: the 1.13 SVG/CSS room rendered by its own small scene view inside native Home (engine builds the page via __nstudiopage, updates via __nstudioparts; taps go native; Google fonts allowed, everything else blocked). It is web tech inside the native page, not the web layer.
Also native now: thread link picker + threads-from (NForms.threadsFor), goal/habit Duplicate (engine via run → native route), Ask AI about goal/habit (sh.ask), Day page reminders/replies/cheers + expandable habit rows (NDayScreen), Activity permission/Health Connect panels (__nactv), widget/notification routes urge-/routine-/slip-/cal-/hvista/add/join/ai, backup import confirm, home style toggle.
Hooks added: __nrun, __nconfYes, __nwipe, __nentry, __nscope/__nscopeset, __nsr/__nsrGet + __nsh* (acc, join, gopen, grp, more, rx, react, quick, panel, today, redo, jr, how), __nstudio/__nstudiopage/__nstudioparts, __nactv, __ndyreacts, __nprompt, __nics. Tests in scripts/t54.py (all pass); t52 t50 t51 t45 t49 t41 t37 t53 pass.
Known gaps vs 1.13: Studio is a scene view (not Canvas); bullet button in the moment editor inserts "• " text; Home layout order differs slightly from web Today (share card/demo bar sit under quick actions, not above the greeting); Settings "Home page" uses chips, not the web's preview cards.

## 2.0.9 (versionCode 61) — fixes from the owner's 2.0.8 screenshots + page-by-page 1.13 re-port (NOT device-tested)
Owner said: "lots of UI issues, doesn't feel like the original, full screen doesn't work, after clicking the screen sometimes goes back". Fixed:
- Screen jumping back: NPager swipe between tabs is off (`pager.locked = true`; 1.13 has no swipe between tabs).
- Full screen rewritten like the web: immersive, tab bar hidden, floating pill top-left (icon + page name + menu) with a 2-column page panel and "Exit full screen" (NShell.focusMode/buildFnav). Page titles hidden in full screen, only the buttons stay (NPage.header).
- Studio "Webpage not available": NStudio no longer blocks the page's own data:/about: URLs.
- Grey lines through habit sub-lines (NFlow measured fixed-size children wrong) and the Calendar day grid drawing over its card (NPage.unclip now clips inner ScrollViews to their outline).
- Titles never break inside a word (web fitH1): shrink 40→30sp beside the buttons, else buttons on their own row and the title down to 22sp.
- New NHs = web hsShell (4:5 cards, snapping, neighbours turned/faded, dashed line with stems and nodes, ‹ 01 / 23 ›, tap an off-centre card to centre it). Used by Goals carousel, Journal, Threads, Thread detail.
- New NSpine = web flight-path timeline (dashed centre line, date pills, alternating cards with glowing dots).
- New NJCards = web momentCard/entryCard/journalBig/journalCard, thrBig/thrFlip, updBig/updCard.
- Journal rebuilt to 1.13 vJournalW: header (Year ✦, Write, gear), Journal/Threads + cards/timeline switch, the 7-day week bar (‹ days ›, date jump, swipe for earlier/later weeks, "Back to this week"), filter chips, cards or timeline, 1.13 empty states. Mood strip/search/"Year in review" link removed (not in 1.13). Threads tab = vThreads (Active · N / Done / All, cards or timeline, empty states). NForms.threadAdd = web thrAddSheet.
- Thread detail = web vThread (edit / done-reopen / gear, big title, started line, link pill, emoji tag composer with send button, "Latest first" + layout switch, update cards or timeline).
- Home re-ordered and restyled to the web: share card + demo bar above the greeting; header buttons room/＋New goal/full/settings; "N steps done this week · N late · N active goals"; quick-action chips (no Studio chip); Your day ring counts habits + today's goals + steps due today, "1/10" inline; insights cards (btn.sm + ghost Ask AI); Today's goals; habit pills (flame icon, routine play button → runner, quit clock with big numbers); Plan with AI card (shared with Ask, minimised row); Activity; Threads (New link, + per row); Up next card (ink buttons with icons); Coming up; Pinned.
- Goal card back face: divider above Due, due colours (late/soon), 38dp pin.
- `.data` labels app-wide now 10.5sp mono, .05em (NUi.label). New helpers: NUi.pbtn (header primary icon button), NUi.btnSm, NUi.iconD, NUi.sp; NRing.inline/glyph; NGen icon "send".
Tests: t52 t50 t51 t45 t49 t41 t37 t53 t54 pass. Web layer unchanged except APP_VER.
Next: continue the page-by-page comparison (Habits page sections, Calendar, Vista, Ask thread view, Settings, goal/habit/day detail) using /tmp harness o.py (go(route) → DOM outline + screenshot with demo data) — needs device screenshots from the owner to confirm.
