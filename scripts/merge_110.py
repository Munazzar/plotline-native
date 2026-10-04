# Merges scripts/v110.js + v110.css into plotline.html (idempotent; replaces the block between the BEGIN/END markers).
import re
import os as _o;ROOT=_o.environ.get('PLOTLINE_ROOT') or _o.path.dirname(_o.path.dirname(_o.path.abspath(__file__)))
P=ROOT+'/plotline.html';s=open(P).read()
js=open(ROOT+'/scripts/v110.js').read().rstrip('\n');css=open(ROOT+'/scripts/v110.css').read().rstrip('\n')
assert js.endswith('/* ==== END 1.10 MODULES ==== */') and css.endswith('/* ==== END 1.10 CSS ==== */'),'END marker must be the last line of v110.js/v110.css (append new code above it)'
# drop the first-round 1.10 modules if still present
if '\n/* ================= THREADS =================' in s:
    i=s.index('\n/* ================= THREADS =================');j=s.rindex('\n</script>');s=s[:i]+s[j:]
if '/* ================= threads */' in s:
    i=s.index('/* ================= threads */');j=s.index('</style>\n</head>');s=s[:i]+s[j:]
B,E='/* ==== BEGIN 1.10 MODULES ====','/* ==== END 1.10 MODULES ==== */'
if B in s:i=s.index(B);j=s.index(E)+len(E);s=s[:i]+js+s[j:]
else:j=s.rindex('\n</script>');s=s[:j]+'\n'+js+s[j:]
B,E='/* ==== BEGIN 1.10 CSS ==== */','/* ==== END 1.10 CSS ==== */'
if B in s:i=s.index(B);j=s.index(E)+len(E);s=s[:i]+css+s[j:]
else:j=s.index('</style>\n</head>');s=s[:j]+css+'\n'+s[j:]
def once(a,b):
    global s
    if b in s:return
    assert s.count(a)==1,(a[:60],s.count(a));s=s.replace(a,b)
once("o+=todayGoals();o+=habitsStrip();","o+=todayGoals();o+=habitsStrip();if(typeof todayExtras==='function')o+=todayExtras();")
if "[['today','Today'],['history','History'],['vista','Vista']" in s:s=s.replace("[['today','Today'],['history','History'],['vista','Vista']","[['today','Today'],['vista','Vista']")
once("threads:'journal',thread:'journal',activity:'today'})","threads:'journal',thread:'journal',activity:'today',day:'cal'})")
once("bars+=`<div class=\"${k===tn?'now':''}\"><i class=\"${p?'':'z'}\" style=\"height:${Math.max(6,p*100)}%\"></i><span>${DAYS[dUTC(k).getUTCDay()][0]}</span></div>`",
 "bars+=`<button class=\"hb-d${k===tn?' hb-now':''}\" data-act=\"dyOpen\" data-d=\"${fromN(k)}\" aria-label=\"${esc(dayName(fromN(k)))}: ${r.due?r.dn+' of '+r.due+' done':'nothing due'}. Open the day\"><i class=\"${p?'':'z'}\" style=\"height:${Math.max(6,p*100)}%\"></i><span>${DAYS[dUTC(k).getUTCDay()][0]}</span></button>`")
once(".hbars>div{display:flex;flex-direction:column;align-items:center;justify-content:flex-end;gap:6px;height:100%}",
 ".hbars>div,.hbars>.hb-d{display:flex;flex-direction:column;align-items:center;justify-content:flex-end;gap:6px;height:100%;min-width:0;padding:0;margin:0;border:0;background:none;color:inherit;font:inherit;cursor:pointer;border-radius:8px}.hbars>.hb-d:hover i{opacity:.8}")
once(".hbars .now span{color:var(--text)}",".hbars .hb-now span{color:var(--text);font-weight:700}")
once('const top=`<header class="ph"><div><h1>Vista</h1><div class="vsw">${segHTML(\'vista\',\'vistaGo\',[[\'road\',\'Timeline\'],[\'map\',\'Map\']],cur.p===\'map\'?\'map\':\'road\')}</div></div><div class="ph-r"><button class="ibtn" data-act="chapList" aria-label="Chapters" title="Chapters">${ic(\'flag\')}</button><button class="ibtn" data-act="yearOpen" aria-label="Your year in review" title="Your year">${ic(\'ai\')}</button><button class="btn" data-act="rdToday">${ic(\'cal\')}Today</button>${gear()}</div></header>\n <div class="bar">${segHTML(\'rz\',\'rdZoom\',[[\'month\',\'Weeks\'],[\'quarter\',\'Months\'],[\'year\',\'Years\']],RD.zoom)}<div style="display:flex;gap:8px">${segHTML(\'rs\',\'rdShow\',[[\'active\',\'Active\'],[\'all\',\'All\']],RD.show)}<button class="btn" data-act="rdAll">${RD.open.size?\'Collapse\':\'Expand\'}</button></div></div>`;','const top=`<header class="ph"><div><h1>Vista</h1></div><div class="ph-r"><button class="ibtn" data-act="chapList" aria-label="Chapters" title="Chapters">${ic(\'flag\')}</button><button class="ibtn" data-act="yearOpen" aria-label="Your year in review" title="Your year">${ic(\'ai\')}</button>${gear()}</div></header>\n <div class="vtool rv"><div class="vtool-r">${segHTML(\'vista\',\'vistaGo\',[[\'road\',\'Timeline\'],[\'map\',\'Map\']],\'road\')}<span class="vtool-ic"><button class="ibtn" data-act="rdToday" aria-label="Jump to today" title="Jump to today">${ic(\'target\')}</button><button class="ibtn${RD.open.size?\' on\':\'\'}" data-act="rdAll" aria-label="${RD.open.size?\'Collapse all goals\':\'Expand all goals\'}" title="${RD.open.size?\'Collapse all\':\'Expand all\'}">${ic(RD.open.size?\'vcollapse\':\'vexpand\')}</button></span></div><div class="vtool-r">${segHTML(\'rz\',\'rdZoom\',[[\'month\',\'Weeks\'],[\'quarter\',\'Months\'],[\'year\',\'Years\']],RD.zoom)}${segHTML(\'rs\',\'rdShow\',[[\'active\',\'Active\'],[\'all\',\'All\']],RD.show)}</div></div>`;')
once('<div class="data" style="margin-top:16px">Quick messages</div><div class="rx-msgs">','<form class="rx-own" data-form="shReactTxt" autocomplete="off"><input name="m" maxlength="120" placeholder="Type your own message…" aria-label="Your message" ${dis}><button class="btn pri" type="submit" aria-label="Send" ${dis}>${ic(\'send\')}</button></form><div class="data" style="margin-top:16px">Quick messages</div><div class="rx-msgs">')
once("m:String(m||'').slice(0,60),t:Date.now()};if(!r.emoji&&!r.m)return false;","m:String(m||'').slice(0,120),t:Date.now()};if(!r.emoji&&!r.m)return false;")
once("e:String(c.emoji||'').slice(0,16),m:String(c.m||'').slice(0,60),t:+c.t,cid:sh.id","e:String(c.emoji||'').slice(0,16),m:String(c.m||'').slice(0,120),t:+c.t,cid:sh.id")
once("There’s no free chat, so it stays a nudge.","You can also type your own message each time.")
once('  "goals":   array of goal objects, at least 1\n','  "goals":   array of goal objects, at least 1\n  "habits":  array of habit objects, may be [] (optional key)\n')
once('\nSTEP OBJECT — all keys required, in this order\n','\nHABIT OBJECT — all keys required, in this order. Habits are repeating practices that make the goals happen. Always new: never repeat a habit I already have.\n  "title":   string, 3 to 60 characters, the action itself ("Walk 20 minutes", "Read 10 pages")\n  "icon":    one emoji, or ""\n  "kind":    "build" (start doing it) | "quit" (stop doing it)\n  "freq":    "daily" | "days" | "times"\n  "days":    weekday numbers when freq is "days" (0=Sun … 6=Sat), e.g. [1,3,5]; otherwise []\n  "times":   times a week when freq is "times", 1 to 6; otherwise 0\n  "target":  integer 1 to 100, units per day (1 for a simple yes/no habit)\n  "unit":    string, e.g. "pages", "glasses", "minutes"; "" when target is 1\n  "part":    "morning" | "afternoon" | "evening" | "any"\n  "time":    "HH:MM" 24-hour for a daily reminder, or null\n  "why":     string, one sentence, may be ""\n  "goal":    id of the goal it supports ("new-2" or an existing goal id), or null\n\nSTEP OBJECT — all keys required, in this order\n')
once("- Add reminders only where they genuinely help.`;","- Add reminders only where they genuinely help.\n- Add 1 to 4 habits for the practices that repeat (daily reading, three runs a week, no phone after 10 pm). Use steps for one-off actions and habits for repeating ones.`;")
once('      "linksTo": []\n    }\n  ]\n}`;','      "linksTo": []\n    }\n  ],\n  "habits": [\n    { "title": "Run 20 minutes", "icon": "🏃", "kind": "build", "freq": "times", "days": [], "times": 3, "target": 1, "unit": "", "part": "morning", "time": "07:00", "why": "Builds the base for the 10K.", "goal": "new-2" }\n  ]\n}`;')
once("Object.keys(o).forEach(k=>{if(!['format','version','mode','goals'].includes(k))","Object.keys(o).forEach(k=>{if(!['format','version','mode','goals','habits'].includes(k))")
once(" return E.slice(0,30)}\n/* the main (umbrella) goal"," if(typeof planHabitsCheck==='function')planHabitsCheck(o,E);\n return E.slice(0,30)}\n/* the main (umbrella) goal")
once("catch(e){}route();if(S.settings.onboarded","catch(e){}if(typeof landHere==='function')landHere();route();if(S.settings.onboarded")
once('<div class="ask-sugs rv">${INS_PRE.map(','${typeof aiPlanCard===\'function\'?aiPlanCard(\'ask\'):\'\'}<div class="ask-sugs rv">${INS_PRE.map(')
once("\n   <button class=\"ask-sug plan\" data-act=\"askPlan\"><b>${ic('flag')}Plan something new</b><small>Turn hopes into goals and steps with AI</small></button></div>","</div>")
once("placeholder=\"${R?'Ask a follow-up…':'Ask about your goals, habits, journal…'}\"","placeholder=\"${R?'Ask a follow-up…':'Ask anything about your life…'}\"")
once(",goals,...widgetHabits()}))}catch(e){}}",",goals,...widgetHabits(),...(typeof widgetThreads==='function'?widgetThreads():{})}))}catch(e){}}")
once("  else if(a.k==='hskip'){","  else if(a.k==='thr'||a.k==='thrnew'){if(typeof thrFromQueue==='function'&&thrFromQueue(a))n++}\n  else if(a.k==='hskip'){")
_dup='\n   <button class="ask-sug plan" data-act="askPlan"><b>${ic(\'flag\')}Plan something new</b><small>Turn hopes into goals and steps with AI</small></button></div>'
if _dup in s:s=s.replace(_dup,'</div>')
once("if(booted&&document.startViewTransition&&!reduced()&&!TOURING)","if(booted&&document.startViewTransition&&!reduced()&&!TOURING&&!(typeof navFast==='function'&&navFast(p)))")
once("h.kind==='routine'?`${h.steps.length} steps`","h.kind==='routine'?`${h.steps.length} step${h.steps.length===1?'':'s'}`")
once('placeholder="What do you want to achieve today?" aria-label="Add a goal for today"','placeholder="Add a goal for today…" aria-label="Add a goal for today"')
once("document.addEventListener('visibilitychange',()=>{if(!lockActive()||TOURING)return;","document.addEventListener('visibilitychange',()=>{if(!lockActive()||TOURING||window.__NSHELL)return;")
once(" try{const d=await DB.get('state');if(d&&d.goals)S=norm(d)}catch(e){}"," try{await 0;const d=await DB.get('state');if(d&&d.goals)S=norm(d)}catch(e){}")
open(P,'w').write(s);print('merged',len(s))
# guard: the merged app script must parse
import subprocess
r=subprocess.run(['node','-e',"const s=require('fs').readFileSync('"+ROOT+"/plotline.html','utf8');for(const x of s.matchAll(/<script>([\\s\\S]*?)<\\/script>/g)){new Function(x[1])}"],capture_output=True,text=True)
if r.returncode:print('SYNTAX ERROR in plotline.html:',r.stderr.strip().splitlines()[-1] if r.stderr else '');raise SystemExit(1)
print('syntax ok')
