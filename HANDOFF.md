# Plotline — handoff for a new session

Plotline is Munazzar's life-goals app: a single-file web app (GitHub Pages) plus an Android app that wraps the same page in a WebView. Data stays on the device and, if sync is on, in the user's own Google Drive hidden app folder. No servers, no database, no analytics.

**Current version: 1.11.0** (versionCode 49) · package `com.munazzar.plotline` · web at `https://munazzar.github.io/plotline/`

## ▶ NEXT SESSION — start here (written 2026-10-01, after 1.9.6 shipped)

**State:** 1.9.6 (versionCode 44) built, signed with the same key (cert SHA-256 7bb13e1a…), delivered. Firebase project `plotline-510215` is live and its rules are **unchanged** in 1.8.3, so nothing to republish. Sharing has been confirmed on two real phones.

**Not yet verified on real phones:** the reactions banner and badge, the Android reaction notification (`ShareCheck.reactions`, every 15 min while you're in a shared item), the hourly invite notification, and the Java owner key hand-off (POST + `X-HTTP-Method-Override: PATCH`). Ask for screenshots.

**Workspace note:** the source zip must include `apkbuild/app/res`, `apkbuild/app/lib` and `apkbuild/app/assets` (1.8.2's zip didn't). If they are missing, decode the last APK: `java -jar apkbuild/tools/apktool.jar d -f -s -o /tmp/dec Plotline-x.apk`, then copy `res`, `lib` and `assets` into `apkbuild/app/`.

**Working rules for this user:** make surgical, minimal changes; verify against the source; production quality. Never commit or zip `apkbuild/plotline-upload.jks` or `plotline-keypass.txt`, and sign every update with that key. Ask before pushing to GitHub (`Munazzar/plotline`). Test tips: run the web server with `setsid nohup sh /tmp/claude-0/srv.sh` (create it if missing: `cd /home/claude/apkbuild/web && exec python3 -m http.server 8765`). Tests that onboard must set `S.settings.setupDone=true` and `tourOffered=true`. The sharing tests override `PLOTLINE_CFG.firebase={apiKey:'k',projectId:'p'}` to hit the fake. The workspace can't reach Firebase or Google directly (allowlist). `bw.py` needs `apkbuild/app/assets/www/` to exist.

## Restore the workspace (do this first in a new session)

The user attaches two files: `plotline-source.zip` and `plotline-signing-key.zip`. Then:

```sh
cd /home/claude
unzip -o plotline-source.zip          # plotline.html, HANDOFF.md, scripts/, apkbuild/{src,app,web,build.sh}
unzip -o plotline-signing-key.zip     # apkbuild/plotline-upload.jks + apkbuild/plotline-keypass.txt
sh scripts/get-tools.sh               # downloads apktool 2.10.0, dex2jar 2.4, uber-apk-signer 1.3.0, android-30.jar from GitHub
sh apkbuild/build.sh                  # should end with "Built out/Plotline-<version>.apk"
```

Needs JDK 17+, python3, zip, curl. For testing: `pip install playwright --break-system-packages` (Chromium is preinstalled at /opt/pw-browsers; do not run `playwright install`). The Java compiles against android-30.jar; anything newer than API 30 must be called reflectively or behind an SDK_INT check (see `Reminders.canExact`). For a Play Store .aab, also download bundletool from github.com/google/bundletool/releases.

## Where things live

- `/home/claude/plotline.html` — **the one source of truth** for the app (HTML + CSS + JS in one file, ~250 KB). Edit this.
- `scripts/bw.py` — copies plotline.html into `apkbuild/web/index.html` and `apkbuild/app/assets/www/index.html`, swapping Google Fonts for the bundled fonts (`scripts/fontblock.html`). Run after every edit.
- `apkbuild/web/` — the GitHub Pages site (index.html, sw.js service worker, manifest, icons, fonts).
- `apkbuild/src/com/munazzar/plotline/` — Android Java: `MainActivity` (WebView, Google sign-in via AccountManager, deep links, widget routes), `Bridge` (JS interface `window.PlotlineNative`), `Reminders`/`AlarmReceiver`/`BootReceiver` (notifications), `W` (widget helpers + tap queue), `UpNextWidget`, `TodayWidget`, `WeekWidget`, `GoalsWidget`, `HabitsWidget`, `HabitGridWidget`, `HabitVistaWidget`, `StreakWidget`, `WidgetActions` (widget check-offs, habit taps), `Notify` (notification Done / Reply / Mute, reply reader, mute + summary, snooze), `LocalLlm` (on-device llama-server).
- `apkbuild/app/` — apktool project: `AndroidManifest.xml`, `apktool.yml`, `res/` (widget layouts in `res/layout/widget_*.xml`, provider info in `res/xml/`), `assets/www/`.
- `apkbuild/build.sh` — one-command build → `apkbuild/out/Plotline-<version>.apk` + `out/plotline-web.zip`.

## Release steps

1. Edit `plotline.html` (prefer small, surgical edits — the user wants minimal changes, not rewrites).
2. Bump `APP_VER` in plotline.html and the version in **both** `apkbuild/app/AndroidManifest.xml` (`versionCode`, `versionName`) and `apkbuild/app/apktool.yml`, and the `CACHE` name in `apkbuild/web/sw.js`.
3. `sh apkbuild/build.sh`
4. Test (below), then send the APK and web zip.

## Testing

`cd apkbuild/web && python3 -m http.server 8765 &` then from `scripts/`:
- `shot.py name W H "js|||click sel|||wait ms" [full]` — screenshots after loading the demo plan.
- `sync.py` — two fake devices + a fake Google Drive; checks merge, deletes, auto-sync, PIN reset. Must print `converged True` and no page errors.
- `t15.py` AI umbrella import + nested goals + progress roll-up · `t16.py` habits end to end (forms, taps, streaks, milestones, pause, rest days, routine player, urge/slip, limits, widget queue, alarms) · `t7.py` day goals / calendar / timeline / widget payload · `t9.py` widget tap queue, deep-link routes, Day time grid + zoom · `t12.py` mouse drag on carousels · `t14.py` pull-to-sync · `audit.py` layout overlap + horizontal overflow on every page at 360/390/820/1280.

## Key facts

- **Google OAuth web client ID** (built into `PLOTLINE_CFG` in plotline.html): `362782126571-p46kstdcbces9oag10bmdo20r767kn4g.apps.googleusercontent.com`. Project "Plotline" in Google Cloud, consent screen External/Testing, scope `drive.appdata` only. Testers must be added under Google Auth platform → Audience → Test users.
- **Android OAuth client**: package `com.munazzar.plotline`, SHA-1 `0F:7E:7D:3D:C3:FF:04:97:CA:19:5E:D3:13:F5:42:74:8C:C3:FD:78`.
- **Signing**: keystore `plotline-upload.jks`, alias `plotline`, password in `plotline-keypass.txt`. Every update must be signed with this key or it won't install over the old app. Keep it out of git.
- **Sync**: Drive appDataFolder file `plotline.json` = `{goals, entries, days, habits, dead}`. Every object has `u` (last-changed ms); deletions become tombstones in `dead`; `mergeDocs()` picks newest per id. Settings, PIN and name are never synced.
- **Local storage**: IndexedDB `waypoints` key `state` (whole app state `S`); localStorage `plotline.tok` (Drive access token). Android SharedPreferences: `plotline_auth`, `plotline_widget` (widget data + tap queue), `plotline_alarms`, `plotline_notif` (muteUntil, missed reminders).
- **AI plan**: copy/paste prompt to any AI (ChatGPT/Claude/Gemini links). Strict JSON "Plotline Plan Format" validated by `validatePlan()`. No AI API calls from the plan flow.

## App features (1.2.3)

Today page (Today's goals with inline add + carry-over, Up next, Coming up, Pinned) · Goals (grid / carousel flip cards) · Goal detail (step cards / path / flowing timeline) · Calendar (Day = Teams-style hour grid with durations, overlap lanes, now-line, tap slot to add, zoom buttons/pinch/Ctrl+wheel; Week; Month; dated list below) · Timeline (flowing animated wave tracks, no separators) · Life map · Journal (moments, flip cards) · AI Plan · Settings (themes, card styles, halo, PIN, sync, backup/restore) · repeating steps, weekly review, goal sharing, focus timer, reminders, PWA offline, pull-down to sync (touch), 4 Android widgets (Up next with Done, Today's goals tap-to-check, This week tap-a-day, Goal progress rings).


## 1.3.0 — habits, routines, nested goals

- **Habits** (`S.habits`, synced like goals). `kind`: `build` (freq daily / days / times-per-week, `target`+`unit`, `cue`, `mini`), `routine` (`steps[{id,title,min}]`, per-day done ids in `rs`), `quit` (`mode` quit = live clock from `start`/`slips`, milestones in `MILES`; `mode` limit = daily `limit`, counts in `log`). Logs: `log{date:n}`, `skip{date:1}` rest days, `pz[{a,b}]` pause ranges (streak kept). `smile`/`mile` = last announced milestone.
- Code lives in the `HABITS & ROUTINES` block (search `/* ================= HABITS`), styles in `/* ================= habits` CSS. Source copies: `scripts/habits.js`, `scripts/habits.css` (already merged — edit plotline.html, not these).
- Pages: `#/habits` (Today/Build/Break/Routines), `#/habit/<id>`. Today page strip, goal page "Habits for this goal", weekly review line, journal `type:'habit'` milestone entries. Routine player + urge-surfing sheet. Templates in `HTPL`.
- Deep links from widgets: `urge-<id>`, `routine-<id>`, `slip-<id>`, `habit/<id>`, `habits`.
- Widget payload adds `hd`, `habits[]` (id,t,e,a,c,n,u,k,m weekday mask,sd,tw,sb streak-before-today,l{date:v}), `quits[]`. Queue item `{k:'habit',id,d,v}` (absolute value, deduped by id+date).
- Habit reminders go to the alarm list with `hid/hd/hv` → notification gets a **Mark done** action (WidgetActions HABIT mode=set).
- Clean streak widget uses a `Chronometer` for the live hh:mm:ss and re-arms an alarm at each day rollover.
- **Nested goals**: `goal.parent` (id or ''). `kids()`, `ancestors()`, `descendants()`, `treeOrder()`. `prog()` rolls up sub-goals; a parent auto-completes when everything under it is done. Deleting a goal moves its sub-goals up a level.
- **AI plan format**: goals take an optional `"parent"` (required in the prompt). Create mode = one main goal (parent null) + nested goals. Preview shows an editable main-goal card (title, why, area, horizon, target, cover image) and editable sub-goal titles; if the AI sends several top-level goals they get wrapped under a new main goal.
- Widgets not yet seen on a real phone (7 now). Ask for screenshots.

## 1.3.1 – 1.3.7 (added after 1.3.0)

- **Custom life areas**: `areaKey()` / `areaOf()` accept any name; custom areas borrow one of the 7 colors (`.k`). Area fields are text inputs with a datalist (`areaInput`). AI format allows own area names.
- **Sub-goals in views**: `subTree(g)` / `subCard()` put sub-goals (any depth) into Cards, Path and Timeline (timeline = one lane per sub-goal, `ppdFitMany`).
- **AI main goal**: prompt makes the AI name the main goal; if it sends several top goals they're wrapped under one named from their titles.
- **Habits**: Templates tab (`HV='templates'`) + templates under each kind tab.
- **Guided tour** (`TOUR_STEPS`, `startTour`, `endTour`): runs on a sandbox copy of demo data (`TOURING` blocks save/sync/widgets; snapshot restored at end). Spotlight = `.tdim` with clip-path hole + `.tspot` ring animated by rAF (`spotTo`); `tSettle` waits for targets to stop moving; `tScroll` eases page scroll; card docks at bottom on phones and fades between steps (`.trcard.away`). `html[data-touring]` disables page animations. Offered once on load (`settings.tourOffered`), replay in Settings. Test: `scripts/t18.py`.
- **Journal**: filters (`JFILT`: Highlights/My moments/Goals/Habits/Steps/All), every entry flips (`entryCard`), `viewEntry` sheet with Open goal/habit, date jump with animation (`jumpJournal`), habit milestone entries store `hid`.
- **MQC-style writing** (`writeSheet`, `wrSave` autosave): title, 8 moods (`MOODS`), rich text toolbar (execCommand), prompts incl. plan-aware (`planPrompts`), several entries per day, private flag, photo, goal link. Entries store sanitised `html` (`cleanHTML` allow-list) + plain `text` + `mood` + `private`. Mood strip (`moodStrip`). Settings → Import MQC journal (`FILE.mqc`, ids `mqc-<id>`, dedupes).
- **Full-screen / focus mode** (`settings.focus`, `applyFocus`, `setFocus`, key F / Esc): hides rail, tabbar and page titles; floating nav pill `#fnav` (auto-hides on scroll); browser Fullscreen API on web; Android `PlotlineNative.immersive(bool)` hides system bars. Map becomes true full screen.
- **Map** grows from the centre on open (`growMap`); map fits above tab bar on small phones.
- **Calendar** fits the viewport (`fitCal`, compact `.ph.cph`), mouse-drag to change month/week/day on web (`CDR`).
- **Privacy policy**: `apkbuild/web/privacy.html` (linked from Settings → Privacy). Contact email still a placeholder `[add your contact email]`; policy discloses Android `allowBackup=true` (offered to switch off).
- Tests added: `t15` AI umbrella/nesting, `t16` habits, `t17` custom areas + sub-goal views, `t18` tour, `t19` journal/map/calendar, `t20` focus mode. `shot.py` suppresses the tour prompt.
- GitHub push to Munazzar/plotline blocked: Claude GitHub app has no access (user must connect it in claude.ai Settings → Connectors). Ask before pushing.

## 1.4.0 — Insights (on-device RAG)

- AI Plan page has a third tab **Insights** (`AI.tab='insights'`, route alias `#/insights`). Code: `/* ================= INSIGHTS` block at the end of the script; CSS `/* ================= insights`. Source copies `scripts/insights.js` / `scripts/insights.css` (already merged — edit plotline.html).
- **Passages** (`ragDocs()`): journal entries (chunked ~800 chars), goals (why, status, %, done/open/overdue steps), habits (streaks, 30-day rate, most-missed weekdays, slips), day plans per date. Private entries excluded unless `settings.ins.priv`.
- **Retrieval** (`ragRetrieve`): filters (time window, goal subtree, kinds) → BM25 (`bm25`) + optional semantic vectors → reciprocal-rank fusion → char budget (12k, or 5.2k for on-device). Presets in `INS_PRE` (`all:true` = take everything in the window, newest first).
- **Smart search**: Transformers.js 4.3.0 from jsDelivr + `Xenova/all-MiniLM-L6-v2` (q8, ~23 MB, from Hugging Face, cached by the library). Vectors in IndexedDB `waypoints` key `ragvec` = `{m:model, v:{hash:Float32Array}}`; not synced. `RAG_CFG` can be overridden with `window.PLOTLINE_RAG` (tests use local lib/model).
- **Numbers** (`insStats`): journal/moods, steps done, overdue, stalled goals, day-goal rate, habit rate, and mood × habit links (share of positive moods on done vs not-done days, top 2 with ≥20-point gap).
- **Answer engines** (`settings.ins.engine`): `copy` (default; prompt with [n] citations for any AI, paste reply back), `api` (any OpenAI-compatible `/chat/completions` with SSE — Ollama/LM Studio/OpenRouter presets; key only in localStorage `plotline.aikey`), `local` (WebLLM 0.2.85, needs WebGPU; models in `LLMS`). Answers render via `mdLite` with clickable citations (`insSrc`), Save to journal as a note titled "Insight · …".
- sw.js: only deletes `plotline-v*` caches (keeps transformers/webllm model caches) and caches jsDelivr libs in `lib-cdn`.
- Android: LAN `http://` Ollama is blocked as mixed content in the WebView — needs an https address. WebGPU in Android WebView not verified.
- Test: `scripts/t21.py` (needs `scripts/fakeai.py` on :8766 and a server on :8767 with `nm/` = node_modules of @huggingface/transformers + a local MiniLM under `models/`; see the script header).

## 1.4.1 — on-device answers, "about me" context, account ownership

- **On this device** (default engine, `engine:'local'`) now uses **wllama 3.6.1** (llama.cpp → WebAssembly, MIT), bundled at `apkbuild/web/lib/wllama/` (index.min.js + wllama.wasm, 8.4 MB; `bw.py` copies `web/lib` into APK assets). Runs on CPU, or WebGPU when present. WebLLM removed. Models in `LLMS` (GGUF Q4_K_M from Hugging Face, cached by wllama, `allowOffline`): Qwen2.5 0.5B (0.5 GB, default), Llama 3.2 1B (0.8 GB), Qwen2.5 1.5B (1.1 GB). `n_ctx` 4096; `insShrink` drops the lower half of notes and retries on a context-overflow error.
- Android `MainActivity`: asset responses carry COOP same-origin / COEP credentialless / CORP same-origin (cross-origin isolation → multi-thread wasm), `.wasm` served as application/wasm. Web (GitHub Pages) stays single-thread because web sign-in uses a popup (COOP would break it).
- **About me** (`insOverview`): name, date, every active goal (tree, %, target, next step, overdue, idle days, why), recent completions, habits (streak, 30-day kept rate, cue, clean days/slips), mood mix last 30 days vs previous 30, today’s plan. Sent with every question (1.8k chars on-device, 4k otherwise). Settings panel → "See what the AI knows about you".
- **Follow-ups**: `IN.res = {srcs, msgs, turns[{q,a,done,err}], opt}`; `insFollow` retrieves new notes (numbered after the existing ones) and continues the chat; on-device keeps first prompt + last exchange (`insMsgs`). Save to journal saves the whole thread.
- **Account ownership** (sync): `settings.sync.owner` = lower-cased email this device's data belongs to (set after each successful sync; migrated from `sync.email` when there's data). `acctEmail()` checks the signed-in account (Drive `about`) once per token. Connecting — or any background sync — with a different account while the device has data pauses sync and opens `acctChoice`: **Switch** (current data stashed in IndexedDB `stash:<email>`, the other account's stash restored if any, then sync), **Copy into** (deliberate merge), **Cancel** (token dropped, nothing written). Turning sync off asks Keep vs Remove (Remove syncs once more, and only clears if that succeeded). Account picker no longer pre-fills the old email. PIN reset with a different account now drops that token.
- Tests: `t21.py` (on-device via a tiny random GGUF with ChatML template, made by the scratch `mkgguf.py` using `pip install gguf`), `t22.py` account switching (two fake Drives).

## 1.4.2 — on-device fixes after the first phone test

- First real-phone run (Qwen 0.5B) froze the phone and printed `alam,,,,,,` — wllama had auto-enabled WebGPU. Now `IS_PHONE` (Android app or mobile UA) → CPU only (`n_gpu_layers:0`), `n_threads` = cores−2 (max 4), `n_ctx` 3072, `max_tokens` 450, smaller budget (notes 3.2k chars / 6 notes, overview 1.2k). Settings toggle "Use the GPU" (`settings.ins.gpu`, default off on phones).
- `garbled()` stops generation on comma runs / repeated tokens / low letter ratio; retries once on CPU if the GPU was on, else shows a clear error. `repeat_penalty` 1.15.
- `.ins-ans` breaks long strings (junk no longer widens the page). "Best matches in your notes" cards (`insTop`) show the top 3 dated notes right away. Time range defaults to All time for typed questions. A seconds counter shows while the phone reads the prompt.

## 1.4.3 — Phi-4 mini

- Default on-device model is **Phi-4 mini 3.8B** (`phi4-mini`, bartowski/microsoft_Phi-4-mini-instruct-GGUF **Q3_K_S**, 1.9 GB). Q4_K_M is 2.5 GB — over wllama's 2 GB-per-file limit; to use it, split with `llama-gguf-split` and pass the shard URLs. Existing installs are switched once (`settings.ins.phi`).
- `LLMS` rows now carry size in GB (4th field). Models ≥1 GB ask before their first download (`settings.ins.dl`). Phone `n_ctx` 2560 for models >1.5 GB. Load/run errors that look like memory get a "pick Llama 3.2 1B" hint.

## 1.4.4 — Vista (Timeline + Map in one tab)

- Nav item `['vista','Vista','map']` replaces Timeline and Map (7 tabs now). `#/vista` opens the last-used view (`settings.layout.vista` = 'road' | 'map'); `#/road` and `#/map` still work (widgets, tour, links) and highlight Vista. Both headers show "Vista" + a Timeline/Map switch (`.vsw`, action `vistaGo`); map height calc adjusted +26px for it. Full-screen floater lists Timeline and Map separately. Tour: road step taps the Vista tab (`navk`), map step has no nav tap.

## 1.4.5 — remove sample data

- `seedDemo()` now wraps `seedDemo0()` and tags every object it creates with `demo:1` (synced like any field). Older installs with untagged samples are recognised by the sample titles in `DEMO_T` (goals, habits, day goals, the first-run note) plus log entries tied to those goals/habits — only while nothing is tagged and `settings.demoGone` isn't set.
- `demoSets()` / `demoCount()`; `ACT.demoRemove` (confirm + Undo) deletes them, moves the user's own sub-goals up a level, unlinks the user's own habits/day goals/notes from sample goals; deletions become tombstones so synced devices drop them too. Today shows a dismissable banner (`.demob`, `settings.demoHide`); Settings → Your data has "Remove sample data" while any remain. Test: `scripts/t23.py`.

## 1.5.0 — native llama.cpp on Android

- Phone test of 1.4.3: Phi-4 mini through wllama (wasm, CPU) ran 1000+ s without output. Fix: the Android app now runs **llama.cpp natively**.
- Binary: llama.cpp `llama-server`, cross-compiled with **Zig** (`pip install ziglang`; no NDK — dl.google.com is blocked here) as a static **aarch64-linux-musl** executable, `-mcpu=generic+v8_2a+dotprod+fullfp16`, stripped (9.7 MB). Shipped as `apkbuild/app/lib/arm64-v8a/libllamaserver.so` (apps may only exec files from their native lib dir); manifest `android:extractNativeLibs="true"`. Copy + llama.cpp commit/licence in `apkbuild/native/`. Rebuild: `scripts/build-llama-android.sh` (zig's linker segfaults on `--dependency-file`, so the script relinks without it). Tested under `qemu-aarch64-static` (`apt-get download qemu-user-static`, `dpkg -x`).
- `LocalLlm.java`: downloads GGUF to `files/models/` (resume via `.part`, follows HF redirects), starts `llama-server --host 127.0.0.1 --port <random> --api-key <random> -c 4096|3072 -t/-tb min(4,cores) -ngl 0 -np 1 --jinja --no-warmup`, waits for `/health`, streams `/v1/chat/completions` (SSE) and pushes tokens to the page with `window.__llm(id,'t'|'done'|'err'|'ready',text)`. Stops after 10 idle minutes and in `onDestroy`. Log: `cacheDir/llama-server.log`. Bridge: `llmStatus/llmDownload/llmDelete/llmStart/llmChat/llmCancel/llmStop`.
- JS (`NAT_OK()`, `natEnsure`, `natChat`, `natWarm`): used whenever the bridge has `llmStatus`; wllama stays for the web. `LLMS` rows gained native Q4_0 URLs + sizes (Q4_0 is repacked for ARM dot-product at load → fastest on phones). Default model is Llama 3.2 1B (one-time switch via `settings.ins.nat`); Phi-4 mini uses the full Q4_0 (2.33 GB, no 2 GB limit natively). Model preloads when Insights opens. "Delete this model from the phone" in the panel.
- Prompt order changed: instructions → ABOUT ME → NUMBERS → NOTES → QUESTION, so llama-server's prompt cache reuses the stable prefix (faster follow-ups). "Earliest matching note" card for when/since-when/how-long questions.
- Test: `scripts/t24.py` (mock bridge forwarding to the real arm64 server under qemu on :8099 with the tiny GGUF; `PLOTLINE_RAG.noGuard` disables the garble guard for random weights).
- Not yet seen on a real phone: exec permission, speed, memory. Ask for `llama-server.log` if it fails.

## 1.6.0 — Ask (assistant tab)

- Insights left AI Plan and became its own tab **Ask** (`#/ask`, `vAsk`; `#/insights` redirects). Nav: Today · Habits · Calendar · Goals · Vista · Journal · **Ask** (AI Plan is reached from Ask's "Plan something new" card and `#/ai`; the AI page highlights Ask). AI Plan's seg is back to New plan / Change of plans.
- Layout: header (engine label button → settings), New chat (+), **tune** icon (assistant settings sheet = `insEngineHTML()` inside `#askSet`, re-rendered by `askSetRefresh` on change), gear. Empty state: greeting, preset cards (`INS_PRE`), up to 3 plan-specific questions (`askIdeas`: stalled goal, weakest habit, top-priority goal), Plan something new, Signals tiles. Thread: user bubble (`.ask-me`) + assistant block (best matches + earliest-note card on the first turn, streamed answer with citations, Save/Copy/Sources sheet). Composer sticky at the bottom (`.ask-comp`, scope chip → time range/goal sheet, auto-growing textarea, send/stop; Enter sends on desktop). Every message after the first is a follow-up; + starts a new chat.
- Presets apply their own time window for that question only (`W`), the scope chip stays as the user set it.
- Tour: Ask step (nav) before Plan with AI (no nav). Tests t21/t24 ported (`ACT.askNew()` between questions); audit covers `ask`.

## 1.6.1 — AI Plan on this device + tour refresh

- AI Plan step 1 has a **Write the plan here** card (`aiGenHTML`, `ACT.aiGen`; code in the `AI PLAN on this device` block, source `scripts/plangen.js`). Engine = `planEngine()`: your AI server if set, else native llama.cpp in the app, else wllama when the assistant engine is "On this device"; otherwise a hint links to assistant settings. Same `createPrompt`/`updatePrompt` as copy/paste; output is constrained by `planSchema(mode)` (JSON schema of the Plotline Plan Format: consts, enums, key order, max lengths, `new-N` ids, HH:MM pattern) sent as `response_format: json_schema` (llama-server and wllama; `json_object` for API servers). Streams with a goals/steps counter, Stop button; then `extractPlan` + `validatePlan`; one automatic retry with the errors; result goes into step 2/3 exactly like a pasted reply (`AI.reply` + `parseAI`).
- Engines take `opts` (`max_tokens`, `temperature`, `schema`, `reading`); the garble guard is off for schema output. Native context is now 8192 for ≤1.5 GB models (4096 for Phi-4 mini) so a full plan fits.
- Grammar note: llama-server feeds the template's generation prompt into the grammar; the tiny test GGUF needs `add_space_prefix=false` for that (real models are fine).
- Tour: Ask (composer), suggestions, settings icon, then Plan with AI with the on-device option. 26 steps; t18 passes.
- Test: `scripts/t25.py`.

## 1.6.2 — Ask stays on topic

- Scope = the user's goals, habits, routines, moods, journal + general coaching on those (user chose "general coaching allowed").
- `askGate(q, follow)` runs in `ACT.askSend` before any model: blocks `OFF_RX` (write a poem/code/email, translate, capital of, who won, weather, stocks/crypto, recipe, programming languages, news, celebrities, define…) unless it's about "my goal/habit/plan/journal/week…"; follow-ups pass unless off-topic; otherwise allowed if ≥2 question words appear in the user's own data (`dataHits`) or `COACH_RX` matches (coaching/wellbeing/planning vocabulary — no generic verbs like change/start/better). Blocked → `askBlocked` adds the fixed `OFF_LINE` reply instantly (turn `off:1`, never sent to a model, not saved to the journal). Presets and plan-ideas skip the gate.
- Prompt and system message carry the scope rule with the exact `OFF_LINE` sentence; follow-up messages restate it.
- `answerOff(a)` after streaming: no citation, no coaching vocabulary and <2 data words → replaced by `OFF_LINE`.
- Test: `scripts/t26.py` (10 on-topic, 13 off-topic incl. "change a car tire", follow-ups).

## 1.6.3 — polish from phone feedback

- **Smooth streaming** in Ask: `insPaint` repaints at most every ~90 ms; `askFollow` keeps the end of the answer just above the composer with an instant `scrollBy` (no repeated smooth scrolls, which made the screen shake); scrolling by hand during an answer stops the follow (`ASK_STICK`).
- **Map**: the header "fit" button (same corner icon as full screen) moved into the map controls as a target icon (`IC.target`); one full-screen button remains.
- **Edge to edge on Android**: `MainActivity` wraps the WebView in a FrameLayout, transparent status/nav bars, `setDecorFitsSystemWindows(false)` (API 30+) / layout flags (29); insets reach CSS as `--sat`/`--sab` (also re-sent on page load). Keyboard: root gets bottom padding = IME height. All `env(safe-area-inset-top/bottom)` now read `var(--sat|--sab, env(...))`; `main` and the rail pad by `--sat`; map height subtracts both. `Bridge.setBars` keeps bars transparent and colours the root; immersive keeps the layout flags.
- (Replaced in 1.6.4 by continuous card zoom.)
- **New logo**: a small map — glowing amber "you" node, four area-coloured goal nodes, dashed links. In-app `LOGO` uses theme variables; icons rendered by `scripts/icon/make_icons.py` (adaptive fg/mono + legacy mipmaps, web 192/512 maskable).

## 1.6.4 — feedback round

- **Answers stream like typing**: `askTick` reveals text at a steady pace from a buffer (`ASK_SHOWN`), finished lines are formatted with `mdLite`, the line being written is plain text (`.ins-tail`), the page follows by ≤24 px per frame; `askDrain` lets the reveal finish before the final render.
- **No white pill at the top**: hidden toasts are now `opacity:0; visibility:hidden` (with system-bar insets the off-screen toast peeked in).
- **The app never zooms**: viewport `maximum-scale=1,user-scalable=no`; WebView `setSupportZoom(false)`.
- **Card zoom** (replaces the 3-step density): pinch on `[data-zk]` areas — goals grid (`goals`), vertical journal (`journal`), goal path (`path`) — sets CSS `zoom` continuously between 0.4 and 1, keeping the point between the fingers in place, with a % hint; Ctrl + wheel on desktop; saved per area in `settings.cz`. Zoomed timelines (`.spine`) keep their original width (`zfix`) so they shrink into a centred column; the grid gains columns. A tap right after a pinch is swallowed.
- **Logo v2**: an uneven constellation — four goal nodes linked into a path that curves to a glowing amber goal (dashed link = what's next). `scripts/icon/make_icons.py` renders all icons; the in-app `LOGO` is generated from the same mark with theme variables.

## 1.6.5 — map-style card zoom, no scrollbars, full-screen cut-out

- Card zoom now behaves like the map: during the pinch the area scales with a GPU transform around the point between the fingers (`preview`), and on release it is laid out at the new CSS `zoom` with the same point kept under the fingers (`finish`; horizontal scroll kept for carousels). Range 35%–180% (zoom in gives bigger cards). Areas (`data-zk`): goals grid, journal vertical timeline, goal path, and every horizontal card carousel (`hs-<page>`: journal timeline, goals carousel, goal step cards). Saved per area in `settings.cz`.
- Scrollbars hidden everywhere (CSS + WebView `setVertical/HorizontalScrollBarEnabled(false)`).
- Focus mode black band: window `layoutInDisplayCutoutMode = SHORT_EDGES`, so the page draws into the camera cut-out when bars are hidden. A blurred scrim (`.sbar`, height `--sat`) sits under the status bar in normal mode so content scrolling under the clock stays readable.

## 1.7.0 — Habit Vista, useful month view, interactive notifications, four-line logo

- **Ask answers stay on the question.** `insFocus(q)` decides broad vs specific (`BROAD_RX`: how am I doing, this week, progress, priorities…). Specific questions get an ABOUT ME with only the goals/habits whose words match, moods/today's plan only when asked, NUMBERS only for broad/mood/habit questions. `ragRetrieve` drops notes that don't really match (BM25 under 35% of the best hit; vector score under .32 unless strong ≥.45) and only pads with recent entries when `o.broad`. Prompt has numbered RULES (answer exactly the question, ignore unrelated data, say so in one line if nothing fits, then brief general coaching). More stopwords. Test: `t27.py`.
- **Calendar month** (`calMonth` + `calPeek`): each day shows titled chips (2 + “+n more”), a habit bar (done of due, glowing when all done), the journal mood, ⚑ for goal targets, red ring on past days with open items. Under the grid, a day peek for the picked day: items with tick boxes, that day's habits as toggles (`hDay`), and journal entries. Tapping a day in Month no longer jumps away; tap it again for the Day view. Phone grid rows are compact (`fitCal`, `--rows`) so the peek is on screen. Test: `t28.py`.
- **Habit Vista** (Habits → Vista; `scripts/hvista.js` + `.css`, merged at the end of the script/style): one lane per habit, done days are stops, streak runs bold (≥7 glow, length labelled), misses small hollow, rest days dashed, pauses dotted; breaking habits are one line broken by red × slips (limit mode: over-limit days). Labels sticky on the left; scroller opens at today. Zoom 2.2–30 px/day with − +, pinch (redraws the SVG only, anchor date kept) or ctrl+wheel; saved in `settings.layout.hvz`. Tap a stop → `hDaySheet`. Tour step added. Route `hvista` opens it. Test: `t29.py`.
- **Habit Vista widget** (`HabitVistaWidget`, `widget_hvista.xml`): Canvas bitmap sized from the widget options, last 14–35 days by width, same visual language. Widget payload `l` now covers 36 days; quits carry `st` (start) and `sl` (slip days).
- **Interactive notifications.** Every alarm from `syncNative` carries `k` (habit, step, day, checkin, wrap, mile, timer) plus its ids, `rp` (buttons on/off) and `mute` (minutes). Java `Notify` adds **Done** (or All done), **Reply** (RemoteInput) and **Mute 2h**. Replies are read on the phone by `Notify.plan()`: done/finished/✅ → done; not yet/haven't → snooze 1h; busy/focused/at work/meeting/driving → mute (durations: “3 hours”, “half an hour”, “for next 2”, “till 5pm”, “until tomorrow”); snooze/later/remind me → re-post after the time; skip/rest/sick → rest day. Anything else is queued as `{k:'reply'}`; on app open `applyReplies` asks the on-device model / AI server (JSON schema: done|skip|note + habit ids), falling back to keywords, and otherwise saves the reply as a journal note. While muted, reminders are held and summed up in one notification when the mute ends. Evening wrap-up (default 21:00) lists open habits. Settings → Notifications: per-type toggles, wrap-up time, Reply/Done buttons, mute length, quiet hours, Focus mode 1h/2h/4h + Unmute (`NATIVE.notifState/notifMute`). Tests: `t30.py`, Java parser checked with a small harness (22 phrasings).
- **Logo v3**: four timelines of different lengths meeting today's dashed line, the goal glowing amber (`scripts/icon/make_icons.py`, in-app `LOGO`, notification icon `ic_stat.xml`).

## 1.7.1 — Habit Vista redesign, a 🔔 on every item

- **Habit Vista redesign** (same layout on phone and desktop): hero with this week's ring, longest live streak, done-in-30-days and perfect-day chips; span switch Week · Month · 3 months · Year (animated zoom; pinch / ctrl+wheel are continuous, stored as days in view `settings.layout.hvd`); visible date range label. Each habit is a full-width lane: name + live streak + 30-day % on top (overlay, fixed while the timeline scrolls), track below in a tinted lane. Streak runs are 6px bold lines (glow ≥7), single done days are dots, finished runs ≥5 get a length pill under the line, today has a dashed pulsing “tap to log” stop (`hTap`). Zoomed out (<5 px/day) isolated done days become bars. Left edge fades.
- **Per-item reminders** (`scripts/remind.js`): `bellBtn` on habit rows and habit detail, step cards (front + back), day goal rows, goal detail. One sheet (`remEdit` → `#remBody`), changes save instantly and re-schedule. Habit: on/off, time chips (defaults to habit time or its part of the day: morning 8, afternoon 1 pm, evening 7 pm, anytime 9), nudge if not done (`h.again` minutes), second time (`h.time2`). Step / day goal: date + time, At the time / 15 / 30 min / 1 h / 1 day / 1 week before / Morning of (`'m'`, 8:00) / Custom (`'c'` + `remDate`/`remTime`). Day goals without `remind` default to at-time when they have a time. Goal: check-in daily/weekly + time/day, target-date reminder (`g.remTarget` days before, 9:00), list of open steps with their own bells, “remind me for every dated step”. Preview of the next reminders and a warning if that type is off in Settings. New steps default to “at due time”; new habits default to reminder on. `reminderAt` → `remAtOf`. Native: a Done from the notification drops that item's later nudges (`Reminders.dropFor`). Test: `t31.py`.

## 1.7.2 — notification replies read by the on-device AI

- Bug from the phone: “mute for 2 days” muted 2 hours (the reader had no day/week units, and the bare-number rule read “for 2” as hours), and a later “remind me again in 5 hrs” was swallowed because snoozed reminders were held by the mute.
- **ReplyService** (foreground service, type dataSync, started from the Reply action): starts llama-server with the model the app picked (`NATIVE.setReplyModel(file,label,ctx,threads)` from `replyModelSync()`; prefers the Ask model, else any downloaded one; prefs `plotline_notif` aiModel/aiLabel/aiCtx/aiThreads) via `LocalLlm.ensure(Context…)`, sends one non-streaming `LocalLlm.complete` with a JSON schema: `{"actions":[…]}`, each action one of done · skip_today · unmute · stop_this · remind_in{amount,unit} · remind_at{day,time} · mute_for{amount,unit} · mute_until{day,time} · move_to{day} · habits_done{ids} (evening list, ids enum) · note{text}. System prompt with 10 examples is fixed so the server can reuse its cache. Grammar checked on the arm64 server (qemu) with the test model.
- The phone's own reader still checks the model: explicit times in the text (“for 2 days”, “in 5 hrs”, “till 4:30pm”, “tomorrow at 8”, weekdays, tonight, rest of the day) win over the model's arithmetic (`Notify.at`), and remind↔mute are swapped when the words clearly say the other (“remind/snooze” with no mute cue, or the reverse). If no model is downloaded, the switch is off, or the model fails, `Notify.rules` handles it. The notification shows “Reading …” while it works, then exactly what was done with the time (“🔕 All reminders muted until Sat, Oct 3 · 12:51 PM”, “⏰ I’ll remind you again at 5:51 PM”), who understood it, and an **Undo** button (30 s; restores the mute, cancels the snooze, removes queued changes by `rid`).
- Reminders you asked to hear again (`force`) arrive even while muted. New queue kinds applied by the app: `jnote` (journal note), `move` (step due / day goal date), `remoff` (turn off that item's reminder).
- Settings → Notifications: “Understand replies with on-device AI” (`notif.ai`, default on) showing which model reads them.
- Tests: `t32.py`; Java harness for the reader and the model path (needs a real org.json on the classpath, e.g. built from github.com/stleary/JSON-java).

## 1.7.3 — snoozes you can see, per-item notification history, own-emoji icons

- **Snoozes are visible.** `Notify.snoozeAt` registers each snooze in prefs `snz` `{nid:{at,key,title,o}}` (removed when it fires, on Undo, or when cancelled). `notifState()` returns `snoozes`; the item's 🔔 shows “Again 5:00 PM”, the reminder sheet shows “Reminds you again …” with **Cancel** (`NATIVE.notifCancelSnooze(nid)`), and the next-reminder preview includes it. Snoozes and an active mute are re-armed after reboot/update (`Notify.rearmSnoozes` from `BootReceiver`).
- **On-time reminders.** Android 12+ needs “Alarms & reminders” for exact alarms; without it alarms can drift. `NATIVE.exactAlarms()` / `openExactAlarms()`; the Notifications settings and every reminder sheet show an **Allow** prompt while it's off.
- **Notification history per item** (device-only, prefs `log`, newest first, capped at 400): sent, held while muted, reminded again, Done, Mute, your reply + what was done, Undo. Keys `h:<habit>`, `s:<goal>/<step>`, `d:<day goal>`, `g:<goal>` (includes its steps), evening-list entries also count for each listed habit. `NATIVE.notifLog(key)`. Shown as a small grey “Notification history · n” accordion on habit and goal pages, and at the bottom of step / day-goal reminder sheets.
- **Undo** now also covers the Done and Mute buttons; it says so when the change was already applied in the app.
- The model name is no longer repeated: not in notification confirmations, not on the AI Plan card (“On-device AI on this phone”); only in Ask settings and Settings → Notifications.
- Reply model gets at most 75 s (incl. first load) before the quick reader answers.
- **Habit icon:** any emoji from the keyboard (the ＋ tile; one grapheme kept via `Intl.Segmenter`).
- Test: `t33.py`.

## 1.7.4 — sign-in that tells you, desktop polish, tour refresh

- **Sign-in.** Sync never turns itself off on an auth error. `authBanner()` shows a fixed banner on every page (“Sync paused · Sign in again”, one button `ACT.reauth`, “–” minimises it) plus a red dot on Settings; it clears as soon as a token arrives. On Android `reauth` prefers device sign-in (AccountManager renews tokens silently, so it stays signed in); browser sign-in is the fallback. Web tokens are Google's 1-hour implicit-flow tokens: without a backend they can't be renewed silently, so on web the banner appears when one expires (one tap; Google usually returns at once). Silent web refresh would need a small token-exchange service (e.g. a Cloudflare Worker holding the client secret) — not built.
- Auth waiters: `AUTHQ` (every caller gets the result) with timeouts, so a sign-in that never answers can no longer freeze sync (previously a single `AUTHWAIT` could be overwritten and hang `SYNCING`).
- **Desktop:** mouse drag pans `.road`, Habit Vista (`.hv-wrap`), `.hstrip`, `.hscroll`; a drag never counts as a click. Habit Vista lane names no longer cover the whole header row. Scrollbars hidden everywhere (`*{scrollbar-width:none}`), including the Day grid. Day strip: the selected day's number used the accent colour, which equals the text colour in Graphite, so it vanished on the white chip; now it uses the background colour.
- **Tour** (32 steps): added Reminders (🔔), Everything on a day (month peek), Goal reminders, Reply to reminders, Notifications settings; refreshed Habit vista, Sync (banner) and Widgets text.
- Tests: `t34.py`; `sync.py` now checks the banner and that sync stays on.

## 1.8.0 — settings hub, app lock, voice, chapters + year in review, automations, sharing

- **Settings hub** (`setSecs()`, routes `#/settings/<area>`): You & sync · Look & feel · Notifications · Privacy & lock · Automations · Sharing · AI & voice · Your data. Each row shows its current state; each area is its own page. Footer: version (`APP_VER`), privacy policy, tour, setup checklist. Tour steps point at `settings/account`, `settings/notif`, `settings/privacy`, `settings/auto`, `settings/share`.
- **App lock** (`scripts/lock.js`): `settings.lock={on,after,secure,web}`, off by default. Android: `Bridge.bioState/bioAuth` (BiometricPrompt, BIOMETRIC_WEAK|DEVICE_CREDENTIAL, so the phone screen lock is the fallback), `setSecure` (FLAG_SECURE: blank in recents, no screenshots). Web: WebAuthn platform authenticator (local UI lock, credential id in `lock.web`) or the PIN. Locks on open and after N minutes in the background. It is a privacy screen lock, not encryption. **First-run setup checklist** (`setupFlow`): notifications, on-time reminders, app lock, sync — each optional; shown before the tour prompt for new users and once for existing users (`settings.setupDone`).
- **Voice** (`scripts/voice.js`, `Voice.java`): mic in Ask, today's goals, journal editor, AI plan boxes and goal/step/habit/day forms (`VOICE_SEL`, `micDecorate`). Android: on-device `SpeechRecognizer` (API 31+, reflection) — the regular recognizer (may be online) only after the person allows “online speech” (`settings.voice.cloud`). Web: browser SpeechRecognition, also only after consent. RECORD_AUDIO asked through `Bridge.askPerm` → `window.__perm`. In flex rows the mic sits before the send button (`.mic-flow`).
- **Chapters + Year in review** (`scripts/chapters.js`): `S.chapters` (synced; sync doc now also carries `chapters` and `shares`, merged by id/u like goals). Bands on Vista timeline (`chapBandsRoad`, `ROAD_CH`) and Habit Vista; manage via the flag button in Vista. Year in review (`yearShow`, sparkle button in Vista and Journal): 6–7 slides from your data; **Share my year** draws a 1080×1920 PNG (`yearCanvas`) → `Bridge.shareImage` (MediaStore Pictures/Plotline + share sheet) or Web Share / download.
- **Automations** (`scripts/auto.js`, `Auto.java`): ⚡ per habit / step / day goal (`item.auto={type,…}`), places in `settings.places` (device only). Types: steps (TYPE_STEP_COUNTER, ACTIVITY_RECOGNITION; daily = now − yesterday's last reading), workout & sleep (Health Connect platform API on Android 14+, through reflection: ExerciseSession/SleepSession/Steps records), screen time (UsageStats, Usage access; limit habits get minutes logged), place (LocationManager proximity alerts, dwell N min / arrive / leave; needs “Allow all the time”). `autoSync()` → `Bridge.autoSet`; a 30-min inexact alarm checks rules; check-offs go through the widget queue with Undo + notification history (`auto`). Boot re-arms. **Play Store note:** background location, usage access and Health Connect each need a declaration/form in Play Console.
- **Sharing** (`scripts/share.js`): Drive-only. Turning it on adds the `drive.file` scope (`scopeSync`, `Bridge.setScopes` for AccountManager) after an explanation sheet. Sharing an item creates a circle file in “Plotline shared” in the owner's Drive (appProperties plotline=circle, cid) shared with invitees with `sendNotificationEmail=false`; each member keeps their own member file (progress: checks/streak/pct/done step ids, cheers) shared back with the group. Invites are found with `files.list` on appProperties (shared with me) and show on Today and in Settings → Sharing: View → Join / Later / Decline. Modes: together, compete (weekly leaderboard), watch (accountability). Goals can assign steps by email. Leave deletes your file; Stop deletes the circle. **Unverified against real Google Drive:** that another user's app (same OAuth client, `drive.file`) can list/read files created by Plotline and shared with them — tested only against a fake Drive (`t36.py`). Add `drive.file` to the OAuth consent screen's scopes in Google Cloud.
- Tests: `t35.py` (hub, lock, voice, chapters, year, automations with a native mock), `t36.py` (two-person sharing on a fake Drive). Tests that onboard without the tour flags set `S.settings.setupDone=true`.

## 1.8.1 — sharing fix: invites via Google's picker, one shared file per item

- Phone test showed invites never arrived: with the `drive.file` scope Google only lets an app open a file shared by someone else after the person **picks it in Google's Picker** (or "Open with"); listing "shared with me" returns nothing. Confirmed by Google's docs (drive.file = files "that you open with an app or that the user shares with an app").
- Fix: **Find invites** first checks what's already accessible, then opens Google's Picker (`scripts/static/picker.html`, copied to web/ and assets by `bw.py`) filtered to "shared with me" JSON files named Plotline (or to the one file id from an invite link). On Android it runs in a full-screen in-app WebView (`Picker.java`, served without COOP/COEP so Google's iframes load); on the web in a pop-up; results via `window.__picked` / postMessage. Picking grants access to that one file.
- **One shared file per item**: the owner grants members **writer**; each person writes only their own section `ms[email]={name,status,progress,cheers,u}` (`shSection`: read → merge → write → read back, retried). Member files are gone (1.8.0 owners: first publish upgrades members to writer and deletes the old member file). Decline/leave are written to the file so the owner sees them.
- After sharing, **Let them know** sends a short note with a link `…/#/join/<fileId>` (route opens the picker for just that file) and the path Settings → Sharing → Find invites. Group view has **Remind them** for people who haven't joined.
- Needs a **Google Picker API key**: `PLOTLINE_CFG.pickerKey` (empty) or Settings → Sharing → Google picker key (`settings.share.key`). App id = project number from the client id. In Google Cloud: enable "Google Picker API", create an API key (API restriction: Google Picker API; no application restriction, because the Android app's origin is appassets.androidplatform.net).
- Test: `t36.py` now hides shared files until picked, like real Google.

## 1.8.2 — sharing on Firebase, end-to-end encrypted: invites just appear

- Why: Drive's `drive.file` can't let the partner's app see a shared file unless they pick it in Google's Picker (confirmed in Google's docs); the user rejected any picking/links/keys. Full `drive` scope = restricted scope (security audit) and a worse privacy story. So **only shared items** go to **Firebase Firestore over REST**, **end-to-end encrypted**; the plan, journal, etc. stay only in the user's Drive appDataFolder. Picker, `picker.html`, `Picker.java`, `Bridge.openPicker`, `drive.file` and the picker key are gone.
- Config: `PLOTLINE_CFG.firebase={apiKey,projectId}` (empty → Settings → Sharing says "not switched on in this version"). Firebase project = the existing "Plotline" Google Cloud project (so the Android and web OAuth clients' tokens are accepted). Authentication → Google enabled. Firestore (production mode) with **`apkbuild/firestore.rules`**.
- **E2E scheme** (`share.js` WebCrypto; `E2E.java` identical, interop-tested JVM↔WebCrypto): each person has an ECDH P-256 key pair `S.ident={pub,priv(pkcs8),fp,c}`, **synced in the Drive doc** (`docOf/mergeDocs/sameDoc/norm`; earliest `c` wins if two devices made one). Public half published at `keys/{email}={pub,fp,u}` (`publishKey`). Each shared item has a random AES-256 key `sh.k` (kept in `S.shares`, i.e. in Drive). Item key locked per member: SHA-256("plotline-wrap-v1"+ECDH(owner priv, member pub)) → AES-GCM, stored in the circle's `locks` map `{email:{fp,w}}`; owner's public key is `opub`. Content = AES-GCM(base64 iv+ct). The server sees only emails (members, owner), timestamps, and statuses joined/declined/left.
- Data: `circles/{cid}={owner,members[],opub,locks(JSON),enc,created,u}` where `enc` = {title,mode,item,ownerName}; `circles/{cid}/ms/{email}={status,enc,u}` where `enc`={name,progress,cheers}; each person writes only their own. Field is `locks`, not `keys`, because rules maps have a `keys()` method.
- Invitee not on Plotline yet: the invite appears at once but **locked** ("unlocking…"); the owner's app locks the key for them on its next refresh (`shWrapMissing` in `shRefresh`, re-checks fingerprints every 30 min) or the owner's Android background check (`ShareCheck` gets `priv` + `owned:[{cid,k}]` via `Bridge.shareCfg`, PATCH via POST + `X-HTTP-Method-Override`). Already on Plotline → unlocked instantly.
- Sign-in: scopes `drive.appdata` + `userinfo.email` (`SCOPE_EMAIL`, `scopeSync`). `fbSignIn` = Identity Toolkit `signInWithIdp` (`access_token=…&providerId=google.com`, requestUri `http://localhost`); session in localStorage `plotline.fb` (per device; cleared on disconnect/account change); `fbRefresh` via securetoken. **Upgrade path:** pre-1.8.2 Android silent token with the new scope → `needs_consent` → `token()` falls back to drive.appdata (`settings.share.lite`) so sync never breaks, and `settings.share.allow` shows an **Allow** card on Today + Settings → Sharing (`shAllow`).
- `settings.share.on` defaults **on** (lazy `v:2` migration in `shset()`). **Awareness:** first share shows "What happens when you share" (`SH_FACTS`, `settings.share.told`); share form, join sheet and Settings say it's E2E and what the service sees; the sync connect text says Google confirms the email for invites.
- **Live:** invites/locks refresh every 60 s while open (and on return to the app); my progress publishes within ~5 s of a change (interval compares `pubKey`); an open group view re-reads members every 6 s (`SHLIVE`, `[data-shg]`). Android: hourly `ShareCheck` posts "x@gmail.com invited you" (channel "Shared with you") → `#/join/<cid>` → `shJoinRoute`.
- 1.8.1 Drive shares: owner shares → "Shared before the update → Share again"; old invites cleared (`shLegacy`).
- Tests: `t37.py` (fake Identity Toolkit/securetoken/Firestore enforcing the rules: encrypted at rest, locked→unlocked invite, join, live update, cheers, denials, invite-more with instant lock, stop, legacy, private key only in Drive), `t38.py` (Android upgrade path, native mock). `t36.py` removed.

## 1.8.3 — reactions instead of one 👏, sharing UI fixes, contrast pass

- **Reactions** (`scripts/react.js` + `react.css`, merged at the end of the script and after `.sh-how`): in the group view, every other member has a **React** button that opens a picker with 6 emoji (`REACT_EMO`) and the user's **quick messages** (`settings.share.quick`, at most 8 × 40 characters, defaults in `REACT_MSG`, editable from the picker or Settings → Sharing → Reactions). There is no free text. Sending appends `{id,to,emoji,m,t}` to `cheerOut`, which publishes into my encrypted ms doc as `cheers` (the last 60 are kept). 1.8.2's 👏 entries still read fine.
- **50 a day:** the sender's app counts `myCheers` and `cheerOut` across all shares for today. Because S.shares syncs, the count holds across devices. At 50 the picker turns off and `shReactGo` refuses. Receivers apply `capReacts` (the first 50 per sender per local day count; the rest are ignored), so a modified client can't flood anyone. Firestore rules can't count inside encrypted data, so the rules are unchanged.
- **Live and visible:** received reactions are collected into `SHR` by `shCollect` from the open group poll (6 s) and from `shPollReacts` (every 20 s while visible, and on return to the app). `shInbox` tracks `settings.share.rx={cid:{t,n}}` per device. New ones show a top banner on any page (`#rxb`: who, emoji or message, which item, **React** back, auto-hides after 12 s) with a floating emoji burst and a short vibration. They also show a count badge on the item's Shared chip (`.sh-dot`) and a row on Today (`rxTodayRows`). Opening the group marks them read. The group view has a "Reactions · live" feed going both ways.
- **Android:** `fbNative` also sends `joined:[{cid,k,title}]` and `rx:{cid:t}`. `ShareCheck.reactions` decrypts members' ms docs with the item key (`E2E.unseal`), applies the same 50-per-day cap, and posts "Ann: 🔥 Proud of you · on “Gym” · tap to react back" on channel **Reactions** (opens `#/join/<cid>` → group). The alarm runs every 15 minutes while you're in any shared item, hourly otherwise. It never re-notifies what the app already showed.
- **Fixes:** the Settings grid (`.set`, `.shub`) used `1fr` tracks that grew to fit nowrap text, which made the Sharing page and hub wider than the phone. They now use `minmax(0,1fr)`, and the invite row wraps its buttons. Page headers (`.ph`) wrap their buttons under the title on phones only when they don't fit (Vista and Journal were cut off). The back faces of journal and step cards had a fixed 55% black overlay (meant for cover images), which made "Reply to …" moments unreadable. It's now `--t-dim`, set only with an image (`IMGV`, `data-bg=image`). The moment's View button got an eye icon (`IC.eye`); it was empty on phones. Daylight moments use a lighter `--mo` instead of the saturated accent. Contrast fixes: habit weekday letters (`.hd` uses `--muted`), the 🔔 label on tinted cards, "No notes yet." on step backs, and in Daylight the danger, overdue and streak-flame colours. Placeholder emails are now `address@gmail.com`.
- Tests: `t39.py` (reactions end to end: picker, live banner on another page, badge, Today row, read on open, live in an open group, sender cap, receiver cap with a modified client, quick-message editing, Android config, JS→`E2E.java` decrypt), `t37.py` updated to use React, `contrast.py` (WCAG check of visible text under 3:1 plus empty buttons; every theme × card style on 18 views). After the fixes: 0 issues in every theme with solid cards. `audit.py` at 360/390: only the scrollable segment controls remain, by design.

## 1.8.4 — shared panel on the item page

- Feedback: the small "👀 Shared" chip was hard to find, and it wasn't clear you could react. Every shared habit or goal page now shows a **Shared** panel right under the hero (`scripts/shpanel.js` / `.css`; `shInject` wraps `vHabit`/`vGoal` and `VIEWS`). It has the mode and a **Group ›** button. For each other member it shows their name, ✓ done today or not yet, check-ins this week (or % for goals), and the latest reaction from them. Next to each person are **one-tap** 👏 🔥 💪 ❤️ buttons (`ACT.shQuick` → `reactSend`) that send without leaving the page, plus **More** (full picker with quick messages; it closes back to the page). Footer: "Tap to send · n left today". If nobody has joined yet, it says so.
- Live: the panel re-reads that item every 8 s while it's on screen (`SHM` cache, `shPanelLoad`, `shPanelPaint`). Reactions for the item you're looking at don't pop a banner, just the emoji burst, and they're marked read. The picker's "Back to the group" is now "Open the group", and the picker only returns to the group if it was opened from there (`RXT.back`).
- Tests: `t40.py`. `t37`/`t39` now click the React button inside the sheet, and t39's receiver waits on Goals.

## 1.9.0 — Studio live home, fonts, 10 new themes, fixes

- **Notification replies stay out of the journal.** `jnote` queue items are ignored, and `applyReplies` no longer writes a "Reply to …" moment. On Android, `Notify` no longer enqueues `jnote` ("📝 Noted in this reminder's history"). `ReplyService.fromModel` treats a skip word on a habit (`Notify.SKIP_RX`) as a rest day even when the model only heard a note. A one-time cleanup (`settings.replyClean`) removes old auto-made reply moments (`type:'note'`, title "Reply to “", no html/img/mood) and tombstones them so synced devices drop them too.
- **Goal card back on phones:** "Open" was cut off. The Priority row is now `.kv.pr` (hidden ≤560px), the title and next step clamp to 2 lines, and the actions are `flex:none`.
- **Themes:** 5 modern dark (Nordic, Forest, Deep sea, Mocha, Neon) and 5 light (Paper, Mint, Lavender, Blush, Mono). The 7th field in `THEMES` means light. `isLightTheme()` sets `html[data-mode=light|dark]`, and light-specific CSS now uses `[data-mode=light]`. Look & feel groups them under Dark and Light. "Match my device" only swaps to Daylight if the chosen theme is dark.
- **Fonts:** `FONTS` registry (10 heading, 15 text, 7 numbers/labels) plus `FONT_PRE` pairings (Plotline, Studio, Editorial, Friendly, Geometric, Clean, Easy to read, Bold, Classic, Tech). `settings.font={d,b,m}` sets `--f-display/--f-body/--f-mono` (`applyFonts`, called from `applyTheme`). Files are bundled woff2 from @fontsource on npm (latin subset, variable where available) in `web/fonts/`; `bw.py` copies them into APK assets (~0.9 MB). `@font-face` rules are injected once from JS (`#fontfaces`), so a file only downloads when used. `html{font-synthesis-weight:none}` prevents fake bold on single-weight display fonts. Registry source: `scripts/fontreg.json`.
- **Studio live home** (`scripts/studio.js` + `studio.css`, merged with `scripts/merge_studio.py`; `settings.home='studio'`, toggled by the 🏠 button on Today's header or Look & feel → Home page):
  - **Room:** plaster wall with wainscoting, fairy lights, a cork board (every active goal, today's habits as sticky notes with a check, today's list on lined paper; it scrolls if full), and a workbench (journal → Journal, a tray with the next steps and paused habits, pencil cup, books, mug with steam, plant, lamp).
  - **Window:** Ghibli-style hills, tree, house with chimney smoke, path, grass, fluffy clouds, birds by day, fireflies, moon and stars at night; curtains, a hanging pothos and jars on the sill.
  - **Light:** follows the real time of day through `skyNow()` (seasonal sunrise/sunset). It drives `--amb`/`--tw`, which set the sky colours, wall/floor tones, sunbeam, dust motes, floor sun patch, lamp cone and glow, and the night tint. There's a wall clock on phones. A slow camera drift (`.st-cam`, about 1.1× zoom) frames it.
  - **Cat:** `catPlan` strolls in → jumps on the desk → sits with 2–3 behaviours (look, groom, yawn, window; naps at night) → stretches → walks to the edge → leaps off the table and out of view. It then waits 25–45 s and comes back from the other side. Poses walk/sit/loaf/stretch/jump use CSS sub-animations (4-beat gait, tail, blink, ear twitch, breathing, crouch/land squash). Tap it for hearts and "mrrp?".
  - **Play/pause:** `settings.studio.play`, on by default, off with reduced motion. It also rests when the tab is hidden or the room is scrolled away (IntersectionObserver). `window.ST_FAKE` overrides the time for tests.
- Tests: `t41.py` (Studio on both widths: items pinned, habit check, goal opens, cat moves/pauses, full behaviour cycle, light follows the clock; 15 themes; font pair applies and loads; light mode flag; reply cleanup; unclear reply not journaled). Contrast audit over all 15 themes: 0 issues.

## 1.9.1 — the cat, the sunlight, the clock

- **Cat rebuilt with jointed legs** (viewBox 160×110). Each leg is nested groups that rotate around their own joint in view-box units (`transform-box:view-box`): hind hip → knee → hock → paw (haunch shape), front shoulder → elbow → wrist → paw. Joints have round caps, and fur gradients are `userSpaceOnUse`, so the parts blend without seams.
- **Walk cycle** follows the walk-cycle reference (rustyanimator.com/cat-walk-cycle): a lateral four-beat footfall (near hind 0, near front .25, far hind .5, far front .75 of `--P` 1.1 s), linear stance so the paws don't slide, with walk speed tied to body size (`ws`). Pelvis and chest rock against each other twice a step, and the head and tail lag a beat. Keyframes are `cHip/cKnee/cHock/cSho/cElb/cWri`.
- **Jump and other poses:** a jump has crouch → air (front legs reach, hind legs push, body tilts nose-up going up and nose-down coming down, `data-up`) → land squash. Stretch is a proper front reach. The sit pose got a haunch, a fur-coloured front leg and a cream chest.
- **Behaviour** (as asked): stroll in → jump on the desk → sit (look, groom, yawn, watch the window; naps at night) → stretch → walk to the edge → leap off the table and out of view.
- **Sunlight:** the window-shaped beam and floor patch are gone. `studioRays()` draws 7 soft rays from the window glass fanning down-left, plus 2 broad faint rays to the top-left. They fade to nothing near the floor, get warmer at golden hour, and shimmer slowly.
- **Wall clock** now also shows on wide screens, between the board (now 53% wide) and the window.

## 1.9.2 — Studio full-bleed

- **Full bleed:** the Studio spans the whole content area (`width:calc(100vw - var(--railw))`, `--railw` 96px on desktop, 0 on phones and in focus mode). It's flush with the top (negative margin = main's top padding, incl. `--sat`), has no rounded corners, and is `min(56.25% of width, 100vh)` tall on wide screens (9:15 on phones). It renders first on Today, above invites and banners.
- **Bottom edge:** melts into the page: `.st-fade` (bottom 13%, gradient to `--bg`) plus inner shadows top and bottom. The cat's floor line moved up (91.5% / 92.5%) so it stays visible above the fade.
- **Smaller tweaks:** thinner clock rim; camera zoomed out (1.02–1.045 wide, 1.01–1.03 phone); bench cards capped in size on very wide screens; play/pause at top-right on desktop and bottom-right on phones.
- **Cat gait:** diagonal pairs now move together (near hind + far front, then near front + far hind), as cats do.

## 1.9.3 — fixes from the live site

- **Settings → Home page preview blew up across the page:** its preview used the class `studio`, which picked up the whole scene's full-bleed rules. Renamed to `.hpv-classic` / `.hpv-studio`. Never use the bare class `studio` outside the scene.
- **Lamp light:** the cone now runs from the bulb down onto the desk only (clip + 1cqw blur, screen blend), with a warm pool where it lands. Night only. Glass shine is dimmer at night.
- **Phone top merge:** the scene runs under the status bar (board and lights offset by `--sat`), with `.st-fade-top` fading from the page colour. The status-bar scrim `.sbar` is now 1.6× taller with a masked soft bottom edge (all pages), so there's no hard line.
- `t41` waits for Settings to render before counting themes (it was flaky).

## 1.9.4 — Studio speed, taps, props

- **Why taps missed and the app felt slow:**
  - Every render (each habit tap, every sync) rebuilt the whole room: about 200 KB of SVG, noise textures and the cat. That reset the board's scroll, so the next circle had moved, and the cat restarted.
  - On top of that, about 60 CSS animations ran all the time, including night-only ones running invisibly by day. There was a full-scene camera drift, blend modes and a blurred status-bar scrim.
- **Now:**
  - `render()` writes through `viewSet(v,html)` (core change: one line plus a default). On Today with Studio on, `#view` holds `#stuHost` (the room, built once) and `#vin` (the page). Re-renders rebuild only `.st-hello/.st-notes/.st-tray/.st-book` from a light version (`ST_LIGHT` skips art). The board keeps its scroll and the cat keeps walking. Render cost went from about 60 ms to about 11 ms (classic Today is about 6.5 ms).
  - The cat is moved with `transform: translate3d` in px (`CATBOX`), so there's no layout per frame. The rAF loop only runs while it walks or jumps; sitting, napping and away legs sleep on a timeout.
  - No camera drift, no note sway. Grass, curtains, vines, rays and bulbs are static. Only some stars twinkle, and at most 4 fireflies. Day-only and night-only elements are `display:none` via `.is-day/.is-night` (set in `studioSky`). No `mix-blend-mode`. The play button has no backdrop blur. CSS animations pause when the room is offscreen (`.offscreen`).
  - The status-bar scrim is transparent while the room is at the top (`html.stu-attop`, toggled on scroll) and back to normal below.
  - The sticky-note check circles are bigger, with an invisible 0.7em hit margin; day-list rows are taller.
- **Props:** plant, cup, books, jar, sunflower, basket and yarn are Microsoft Fluent Emoji colour SVGs (MIT, via npm `@iconify-json/fluent-emoji`), inlined in `scripts/studio_art.js` (ids prefixed `fe<key>`). `merge_studio.py` prepends that file; merging is now idempotent (END marker). Credit line in Settings → Home page.
- **Layout:** the board (54% high) no longer runs into the bench. Bench cards are capped in size; phones show one bench card.
- **Watch out:** never reuse the scene's class names on other elements. `.studio` (1.9.3) and `.st-top` (this round, on `<html>`) both broke layout.

## 1.9.5 — Studio polish from the live site

- **"Circles don't work":** counted habits (Drink water 8 glasses, Read 20 pages…) add one per tap, but the circle only showed a tick at the target. The circle is now a progress ring with the count, and the note says "3 of 8 glasses". Checked with 10 mouse clicks and taps at 2000×750, 1366 and phone: every tap registers.
- **Stars:** a seeded, scattered field of 70; about 30% twinkle with their own speed; a shooting star every ~17 s at night. (The old formula drew them in a diagonal line.)
- **Motion back, cheaply:** curtains sway, fairy-light bulbs twinkle, the sun rays "stir" (a transform animation on the pre-blurred rays layer), and the lamp glow flickers slightly.
- **Window keeps its shape** on very wide screens (`aspect-ratio:.76`, 54% high), so the view outside isn't cropped. `raysFit()` measures the real window and redraws the rays from it.
- **Props stand on the desk:** they're lowered to the surface, the art's bottom padding is compensated (`translateY(6%)`), and they have a contact shadow.

## 1.9.6 — Studio placement fixes

- **Root cause of the cut curtains and hidden jar/sunflower:** `contain:paint` on `.st-win` (added for speed in 1.9.4) clipped everything outside the window box: curtains, sill, jars, the hanging plant. It also clipped the board's shadow. `contain` is now only on wall, wainscot and floor. **Never put `contain:paint` or `overflow:hidden` on `.st-win` or `.st-board`.**
- **Books:** the Fluent "books" art draws them staggered in mid-air, so it's replaced by a CSS stack of three flat books with a contact shadow.
- **Sill:** sunflower and jar are sized in % of the window (they scale with it) and stand at the sill's left end, clear of the lamp.
- **Phones:** the window keeps its shape (`aspect-ratio:.76`, 19% high), so the moon and sun are visible. The plant stands on the desk; the sill items are hidden because they would sit behind the plant.
- Checked in close-up screenshots: 2000×750 focus mode night and day, 1440 night, phone night and day.

## 2.0.0 — Native Android app (2026-10-03)
**What changed:** the Android app's main screens are now real native Android views (Java, framework widgets only — no AndroidX, because this workspace can't reach Google's Maven). The web app is unchanged as the web version and also runs *hidden* inside the Android app as the "engine".

### Architecture
- `MainActivity` still hosts the WebView (`web`), now `INVISIBLE`. `NShell` (native) sits on top: `NPager` (swipeable main pages, all kept alive) + native tab bar + a stack for detail screens + bottom sheets + toasts.
- **One copy of the data**: `NStore` keeps the same JSON document the web app uses (`files/state.json`). Native edits mutate JSON in place, then `NStore.changed()` stamps `u`/tombstones exactly like the web's `stamp()`, writes the file (background thread) and calls `window.__nativeChanged()` so the engine reloads.
- **Engine** (web, hidden): reads via `NATIVE.storeGet()` (DB.get wrapper in v110.js), writes via `NATIVE.storeSet()` (DB.set wrapper). Native merges incoming web writes with `NMerge` (port of `mergeDocs`), compares id→u versions to avoid ping-pong, and keeps unchanged objects identical so open screens keep editing live objects. Google Drive sync, reminders (`syncNative`), widgets (`pushWidget`), widget-queue, AI, sharing, app lock UI all still run in the engine — same code as the web, so Android and web always agree and sync to the same Drive file.
- **Native screens**: Home (`NHome`: greeting, quick actions, your day ring, up next, today's goals, habits, activity, threads, coming up), Habits (`NHabitsPage` + `NHabitScreen` with month calendar, stats, slips), Calendar (`NCalPage`: month + day agenda), Goals (`NGoalsPage` + `NGoalScreen` with steps), Journal (`NJournalPage`: moments + threads, `NThreadScreen`). Forms in `NForms` (goal, step, habit, moment, thread, day goal, + menu).
- **Classic layer (not native yet)**: Vista, Ask/AI, Plan with AI, Settings, Activity details, Day page, sharing/group screens, habit/goal advanced options. `NShell.openClassic(route)` shows the WebView under the native tab bar (web tab bar hidden via `html.nshell`). Back closes web overlays first (`__nback`), then returns to native.
- **Onboarding/lock**: brand-new installs see the web welcome/setup in the classic layer until `onboarded` (+ setupDone or data). App lock: native shows the web lock screen (`__nlock`) and the web calls `NATIVE.unlocked()`.
- **Safety**: `NCrash` keeps errors (copyable from an error card); two crashes in a row fall back to the classic app; `Bridge.nativeOff()/nativeOn()` toggles.
- Logic ports are verified against the web: `/tmp`-style harness compiles NDates/NHabits/NMerge on the JVM with org.json (github stleary/JSON-java) and compares to JS values (streak, best, strength, rate, done, due, counts, freqText, mergeDocs) — 0 diffs. `scripts/t53.py` tests the engine bridge (migration from IndexedDB, native edit → engine, engine save → native, back, lock).
- Generated data: `native/gen.py` → `NGen.java` (17 themes from the CSS, fonts, areas, the web icons as flattened paths). `native/fonts.py` converts the bundled woff2 fonts to TTF in `app/assets/nfonts` (fonttools + brotli).
- Build constraints: **no lambdas / method refs** (dex2jar can't dex invokedynamic), nothing above API 30 without reflection.

### Not verified on a device
Push to GitHub was refused (Claude GitHub App not installed), so the emulator run never happened. Ready-to-go CI: local branch `native-ci` in `/home/claude/plotline` (and `native/ci/`): workflow builds a debuggable copy, seeds demo data via `run-as`, screenshots every native screen and commits them back to the branch. Once access works: `git -C /home/claude/plotline push -u origin native-ci`, wait for the run, `git fetch` and look at `ci/out/*.png` + `errors.txt`.

### Next (port order)
1. Fix anything the emulator run shows. 2. Native Settings (theme/colours/fonts/sync connect). 3. Native Ask (LocalLlm is already native). 4. Vista timeline. 5. Day page + Activity. 6. Move reminders/widgets/sync from the engine into Java (Drive REST via AccountManager token — MainActivity.fetchToken already exists), then the engine is only needed for classic screens.

## 1.13.0 — Readability pass, snappy tabs, Colours & background, themed widgets, card-stack Threads widget (2026-10-03)
- IMPORTANT merge fix: code appended *after* the END markers in v110.js/v110.css was being duplicated on every merge. The END marker must stay the last line; use `python3 scripts/add.py v110.js|v110.css < snippet` to insert above it. merge_110.py now asserts this.
- Readability: new pixel-based audit `scripts/contrast2.py <themes> [width]` (samples the rendered background behind every text run, so it catches text on gradients/frosted glass that contrast.py skipped). Fixes: ink buttons ("Mark done", "I have an urge") lost their fill on glass themes; ring centres (.hh-d, .hday-r, .act-ring) were translucent on glass (pseudo-elements can't go inside :is()); placeholders, out-of-month calendar days, "+N more", agenda kind labels, light-theme "Late"; Vista month label hidden under the NOW pill (roadAxisFix).
- Page titles never break inside a word: fitH1 binary-searches the size; if it would go under 30px the header switches to a large-title layout (buttons row above, class .ph-stack). Re-fits after fonts load.
- Crumb action rows (habit/goal/thread pages) can no longer overlap the back button: they shrink at <=400/<=370px and otherwise scroll with an edge fade (crumbFit). Same edge fade on scrolling tab/chip rows (edgeFade).
- Status bar: the scrim no longer draws a band. It is invisible at the top of a page (html.pg-top) and becomes a soft fading blur once content scrolls under the status bar.
- Ask overlay: pull-to-refresh can no longer start inside the overlay/pickers/banners (it hijacked scrolling). Composer respects the nav-bar inset.
- Home: "Hide" on Plan with AI is now "Minimize" → a compact bar (.aip-min; note the old global .mini class collides) with a chevron to expand.
- Step-card reminder bell was 210x156 because of the global .mini class; now an inline icon chip.
- Colours & background panel (Look & feel): accent (12 swatches + any colour), background wallpaper (None/Aurora/Ocean/Sunset/Forest/Rose/Mono/Accent) on any theme, strength, glass frost, widget opacity, optional slow drift. Settings: accent, wall, wallStr, frost, wAlpha, wallMove. lookApply() sets data-wall/--wop/--frost/--accent/--on-accent.
- Liquid Glass dark is calmer: neutral base (#10141F), no CSS blur filter on the wallpaper, no animation by default.
- Performance ("tabs feel slow"): tab-to-tab navigation skips the full-page view transition (navFast, patched into route()), on-screen sections appear at once with a short fade (html[data-anim=snappy]); below-the-fold reveals are shorter. Look & feel > Animations: Snappy (default) / Expressive. Glass: only big containers blur; chips/buttons on top use a plain translucent fill. fitCards uses a binary search.
- Widgets follow the app theme (Android 12+): pushWidget sends th {bg,text,muted,dim,accent,on,light}; W.rv(c, layout) reads the layout XML once and maps the original palette colours/backgrounds to the theme (backgrounds tinted with RemoteViews.setColorStateList via reflection); W.bgRes for drawables set in code. Android 11 and older keep the original dark look.
- Threads widget is now a 2x2 card stack (StackView + ThreadsCards RemoteViewsService): swipe up/down to pick a thread, + Add to type, mic to speak, tap the card to open it; the last card starts a new thread. Card taps go through QuickLog (route extra → opens MainActivity). QuickLog uses the app theme.
- Small: "1 step" plural; today-goal placeholder shortened; Your-day row wraps on narrow phones; light theme grid is 3-up on phones.
- Tests: t52 (overlay touch scroll, minimize, crumb, colours, widget theme payload, snappy nav, status scrim, ink buttons). t45 accepts the large-title header.
- NOT yet tested on a real phone: themed widgets and the card-stack Threads widget (compiles and is packaged; StackView/RemoteViews tinting needs a device check).

## 1.12.0 — Ask overlay, Liquid Glass, Threads widget, settings folds (2026-10-03)
- Ask overlay: "Ask" buttons open a bottom sheet (#aov) on the current page instead of navigating. Drag the header down >110px to close, up >50px to expand; buttons for new chat, full page, close; Esc/hashchange close. Code: AOV/aovOpen/aovPaint/aovClose/aovDrag in v110.js; askWith(q) opens + sends.
- Reactions wait for app lock: reactShow/reactBurst are deferred while LOCKED or hidden (RB.wait) and replayed after unlockDone / visibilitychange.
- Journal week bar is one compact row (arrows, 7 days with mood, date picker); no entry counts.
- Themes: Liquid Glass (glass) and Liquid Glass Light (glasslight) — 17 themes total. Frosted surfaces + animated wallpaper in v110.css. contrast.py falls back to --bg when body is transparent.
- Android Threads widget (ThreadsWidget.java, widget_threads.xml) + QuickLog activity (type or speak an update into a thread, or create a new thread). Saves go through W.enqueue {k:'thr'|'thrnew'}; applyWidgetQueue/thrFromQueue apply them on open. NOT yet tested on a real phone.
- Settings panels fold (foldSettings; state in S.settings.fold). Tests set window.__allOpen=1 to open all.
- Start page picker is an in-app sheet (landSheet) instead of a native select; all <select>s on touch devices open an in-app picker (selOpen/.selp).
- fitCards shrinks card titles (goal cards, step cards, journal/thread big text) until no clipping/clamp/footer collision; uses setProperty(...,'important').
- versionCode 50. Tests: t50 (overlay/lock/week bar/widget queue/themes/folds), t51 (select picker).

## 1.11.0 — Home, AI plans with habits, insights, start page, instant reactions, workouts (2026-10-03)

Still all in `scripts/v110.js` / `v110.css` + `merge_110.py` (core one-liners). Tests: `t49.py` (this round), `t45.py`, `audit.py` (now also covers threads, a thread, a day, activity, look & data settings).
- **Home** (nav label "Home", route stays `today`): quick actions row (`.hmq`: Plan with AI, Goal, Habit, Write, Thread, Ask, Today in full), "Your day" card (habits + day goals + steps due today, next item with Done), **Insights** (`homeInsights()`: streak at risk after 3 pm, best/worst weekday over 8 weeks, goals idle 14+ days, steps week-on-week ±20%, sleep 7h+ vs habit rate, quiet threads, perfect-day runs; each has Ask AI → `askWith(q)`), explained **Plan with AI** card (`aiPlanCard`, hideable on Home, always on Ask above the suggestions).
- **AI plans create habits**: optional `"habits"` array in the Plotline Plan Format (SPEC, GUIDE, EX_CREATE, `planSchema` wrapper, `planHabitsCheck` in `validatePlan`), preview with tick boxes (`pvHab`), created after the goals and linked by goal title (`applyPlan` wrapper).
- **Start page**: `settings.landing` (any page, a thread, a habit, a goal, or Today in full), Settings → Look & feel → Start page; applied once at launch by `landHere()` (only when the app opens on Home).
- **Ask about this** sparkle on goal, habit and thread pages. Ask now also reads threads (`kind:'thread'`) and 60 days of activity (`kind:'day'`, ids `a<date>`), and ABOUT ME includes a 7-day activity line and open threads.
- **Reactions**: type your own message (`shReactTxt`, up to 120 chars; quick messages still there). **Instant reactions** (Settings → Sharing, Android): `LiveShare` foreground service (dataSync, silent notification) polls the sharing space every 30 s while you're in a shared item, using `ShareCheck.reactions`; starts from `ShareCheck.config` and on boot, stops when off or no shared items. True push needs a server (FCM + Cloud Function); not built.
- **Workouts**: every saved place is now watched (before, only places used by a rule — so Gym never logged time). Places can count as workouts (auto by name/emoji `WK_RX`, toggle per place in Activity); workout minutes = Health Connect sessions + time at workout places (`wkMin`). Native `activity()` returns `hc:{steps,sleep,ex,errSteps,errSleep,errEx}`; the Activity page explains an empty or failing Health Connect read with an "Open Health Connect" button. Note: Android 14 does not allow Health Connect reads in the background, so automations that need workouts/sleep only work while the app is open (Android 15 adds a background permission).
- **UI fixes**: detail-page button bars fit on phones (icon-only pills, fullscreen hidden there), Activity day rows no longer cut values, AI plan review no longer wider than the phone (`.fstep>div{min-width:0}`), empty metrics show "—". Class names now checked for clashes with a script (see the Python snippet in this round's session); `.hq` is the habit-quit card.

## 1.10.0 / 1.10.1 — Threads, Day page, Activity, habit calendar, journal weeks, Studio fixes (2026-10-02)

All new UI lives in **`scripts/v110.js` + `scripts/v110.css`**, merged into plotline.html between `/* ==== BEGIN 1.10 MODULES ==== */` and `/* ==== END 1.10 MODULES ==== */` (CSS: `BEGIN/END 1.10 CSS`) by **`python3 scripts/merge_110.py`** (idempotent, also applies the few core one-liners). Edit v110.*, then merge, then `bw.py`/build. Test: **`scripts/t45.py [390|1280]`** (mock phone via `add_init_script("window.PlotlineNative={}")`; `NATIVE` is a const).

- **Headers (all pages):** `.ph` is a grid; the title and every action share row 1, gear last in the top-right (gear also shown on desktop and appended to every `.crumb .ph-r`). On phones text buttons in headers become icons. `fitH1()` shrinks a title to fit beside the buttons instead of breaking words.
- **Journal:** 7 days at a time (`JW` weeks back, `jwBar`: arrows, swipe, date picker `[data-jwdate]`, day cells with mood that jump to that day), newest first in both views, filter chips under the week, Journal | Threads switch + layout toggle on one row. Old mood strip no longer on Journal.
- **Threads:** `S.threads` (synced, `mergeThreads` merges updates by id). Same look as Journal: list = journal cards (side-by-side `thrBig` / timeline flip cards `thrFlip`), thread page = its updates as journal cards, newest first, quick composer with tag chips (Enter sends on keyboards). Not linked unless the user picks a goal/habit (or starts from one). Today shows the 3 latest as list rows (`thrToday`). Entry points: Today, Journal switch, 🧵 on goal/habit pages, Thread button on journal entries.
- **Day page `#/day/<date>`** (`vDay`): everything on a date. Summary chips jump to sections. Habits: each row editable (done, counts, routine steps, rest day, limit counts, slips add/remove) and expands to show that day's reminders and replies as a chat, cheers sent/received, journal entries and thread updates. Then Activity (from `settings.actLog`), goals for the day, steps done / due, journal cards, thread updates, all reminders/replies/cheers. Prev/next arrows, swipe on the title, date picker. Opened from the habit week strip and month calendar, Activity bars and day list, Today's "Yesterday" line, and an "Everything on this day" button in the small habit day sheets. Native: `Notify.history(c,"*")` returns the whole log (latest 400).
- **Habits:** no History tab. Habits → Today starts with a tappable 7-day strip and "Show the month" (also the calendar icon in the header) that reveals a month calendar (`settings.layout.hcal`). Any day opens the Day page.
- **Activity:** Today card shows steps, sleep, workout and screen time (+ yesterday line). `#/activity`: tiles with 7/14/30-day bars (tap a bar → Day page), "Day by day" list from `settings.actLog` (180 days kept on the device). Native `Auto.activity()` / `Bridge.activityLoad(days)` → `window.__act`: per-day steps (step counter vs Health Connect), sleep minutes + sessions (`ss`), workouts (`w`), screen minutes + top 5 apps per day (`ap`), time at places (`pv:<date>:<id>` logged on leaving). First load asks 30 days. **Not yet run on a real phone.** `ACT_EX` exercise names follow Health Connect constants from memory — verify (existing `EX_TYPES` maps 10→Boxing; HC BOXING is likely 11).
- **Studio (1.10.0):** cup art lost its 3 baked-in steam wisps; live steam = 3 wavy SVG wisps; birds flap (`d:path()` morph); sun rays start at the sun's position in the window (`raysFit` → `studioRays(…,SU)`), refreshed each minute; vine leaf path bug fixed; Today toggle icons `IC.room`/`IC.cards`.
- **Class-name lesson:** `.mini` (pinned-goal card), `.now` (Up next card) and `.ag` (calendar agenda) are global classes; reusing them broke the tag chips, the habit week bar's today column and the chart goal line. Prefix new classes.
- **1.10.3:** 16 script/handwritten fonts (`SCRIPT_FONTS` in v110.js: Dancing Script, Caveat, Pacifico, Great Vibes, Satisfy, Sacramento, Parisienne, Allura, Lobster, Yellowtail, Kaushan Script, Homemade Apple, Kalam, Patrick Hand, Indie Flower, Shadows Into Light; @fontsource, OFL/Apache) in `web/fonts/`, own `@font-face` block `#fontfaces2` with per-font `size-adjust`; presets Handwritten, Notebook, Elegant, Playful, Signature. A script heading sets `html[data-fscript]` (no all-caps, no letter-spacing); script text sets `data-fscriptb`. Vista timeline header: Timeline|Map + Today/Expand icons on one row, zoom + Active/All on the next (`.vtool`, patched in `merge_110.py`); lanes sit above the NOW line.
- **1.10.2:** Habits summary week bars are buttons (`.hb-d`, today `.hb-now`) that open the Day page; patched from `merge_110.py`.
- Source note: the first upload this round had only old zips; the real 1.9.6 source came later and everything was replayed onto it.

## Open items

- Firebase sharing tested only against a fake; confirm on two real phones once the Firebase keys are in. Privacy policy should say: shared items are E2E encrypted in Firebase; Firebase sees emails of people in a share and of everyone signed in for invites; public keys are readable by signed-in users (so an email's use of Plotline is discoverable). X-HTTP-Method-Override PATCH from Java is untested against real Firestore.
- Privacy hardening offered, not yet done: set `android:allowBackup="false"` in the manifest; optional passphrase encryption of `plotline.json` before upload.
- Optional: switch web sign-in to Google Identity Services token client; optional bring-your-own-key AI.
- Widgets (incl. Habit Vista) and the notification actions/replies were compiled and packaged but not yet seen on a real phone — ask the user for feedback/screenshots.
- Ideas not done yet: MQC-style day sidebar view; Health Connect auto-check for habits; Notion via Cloudflare Worker; share-into-Plotline; habits in the AI plan format; habit reminders on the web when the app is closed (needs push).
- Play Store: will need an .aab signed with the same key (bundletool is in tools/) and Play App Signing's SHA-1 added as a second Android OAuth client.


## 2.0.1 (parity pass 1)
See NEXT-SESSION.md section 6 '2.0.1 additions'. Web tests t52/t50/t51/t45/t49/t41/t37/t53 pass; audit.py shows 2 pre-existing minor gap notes. Not device-tested.

## 2.0.1 UI-fidelity pass
See NEXT-SESSION.md "2.0.1 UI-fidelity pass". Unverified on device.

## 2.0.2
Second UI-fidelity pass: see NEXT-SESSION.md "2.0.2". versionCode 54. Unverified on device.

## 2.0.3
See NEXT-SESSION.md "2.0.3". versionCode 55. Not device-tested.

## 2.0.4
See NEXT-SESSION.md '2.0.4'. Not device-tested.

## 2.0.5
See NEXT-SESSION.md '2.0.5'. versionCode 57. Native Settings, Ask, Vista (Timeline+Map), goal Timeline, Chapters. Not device-tested.

## 2.0.6
See NEXT-SESSION.md "2.0.6": covers/halo/map full-screen, goal+step+habit forms, Year in review, Habit Vista, Automations+Sharing bodies, Plan with AI, guided tour, first-run welcome are native. Studio room, weekly review and a few engine sheets remain web. Not device-tested.

## 2.0.7 summary
See NEXT-SESSION.md "2.0.7". Version bumped in plotline.html (APP_VER), AndroidManifest.xml, apktool.yml, sw.js. Signed with plotline-upload.jks, SHA-256 7B:B1:3E:1A:…:9E:48:99 verified.

## 2.0.8 summary
Web layer migration finished: no user path opens the classic web screens any more (safety net remains for anything missed). See NEXT-SESSION.md "2.0.8". Version bumped in plotline.html, AndroidManifest.xml, apktool.yml, sw.js. Signed with plotline-upload.jks, SHA-256 7B:B1:…:9E:48:99 verified.

## 2.0.9 summary
Fixes from the 2.0.8 screenshots (tab swipe off, full screen rebuilt, Studio data: page, NFlow/clipping lines, title fitting) and a page-by-page re-port to 1.13: Journal (week bar, NHs cards / NSpine timeline), Threads, Thread detail, Home order and styling, goal card back. See NEXT-SESSION.md "2.0.9". Version bumped in plotline.html, AndroidManifest.xml, apktool.yml, sw.js. Signed with plotline-upload.jks, SHA-256 7B:B1:…:9E:48:99 verified.
