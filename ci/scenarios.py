# The screens compared between the 1.13 web build and the native app.
# Each scenario: (name, route, steps). Routes are the web hash routes; NShell.route understands the same ones.
# A step is (web, native): what to tap on each side after opening the route.
#   web:    'text:Week' (exact visible text), 'css:<selector>', 'js:<code>'
#   native: 'text:Week' (a view showing that text), 'desc:horz' (a view's content description), None (skip)
import datetime

# a thread with three updates is added to the sample data so thread screens have content (both sides get it)
THREAD_JS = """(()=>{S.threads=S.threads||[];if(S.threads.some(t=>t.id==='thrdemo1'))return;const now=Date.now(),D=864e5;
S.threads.push({id:'thrdemo1',title:'Garage gym idea',link:{k:'goal',id:(S.goals[0]||{}).id||''},status:'open',created:now-5*D,u:now,
ups:[{id:'tu1',t:now-5*D,k:'idea',x:'Turn the garage corner into a small gym',u:now},{id:'tu2',t:now-2*D,k:'prog',x:'Bought two kettlebells and a mat',u:now},
{id:'tu3',t:now-36e5,k:'note',x:'First workout there this morning, felt great',u:now}]});save()})()"""


def scenarios(state):
    today = datetime.date.today().isoformat()
    g = next((x for x in state.get('goals', []) if x.get('status', 'active') == 'active'), None)
    h = next((x for x in state.get('habits', []) if x.get('status', 'active') == 'active' and x.get('kind') != 'quit'), None)
    q = next((x for x in state.get('habits', []) if x.get('kind') == 'quit'), None)
    t = next(iter(state.get('threads', []) or []), None)
    L = [
        ('01-today', 'today', []),
        ('02-habits', 'habits', []),
        ('03-cal', 'cal', []),
        ('04-goals', 'goals', []),
        ('05-vista', 'vista', []),
        ('06-map', 'map', []),
        ('07-journal', 'journal', []),
        ('08-threads', 'threads', []),
        ('09-ask', 'ask', []),
        ('10-day', 'day/' + today, []),
        ('11-activity', 'activity', []),
        ('12-settings', 'settings', []),
    ]
    if g: L.append(('20-goal', 'goal/' + g['id'], []))
    if h: L.append(('21-habit', 'habit/' + h['id'], []))
    if q: L.append(('22-habit-quit', 'habit/' + q['id'], []))
    if t: L.append(('23-thread', 'thread/' + t['id'], []))
    # views reached by a tap
    L += [
        ('30-cal-week', 'cal', [('text:Week', 'text:Week')]),
        ('31-cal-day', 'cal', [('text:Day', 'text:Day')]),
        ('32-cal-month', 'cal', [('text:Month', 'text:Month')]),
        ('33-habits-build', 'habits', [('text:Build', 'text:Build')]),
        ('34-habits-break', 'habits', [('text:Break', 'text:Break')]),
        ('35-habits-routines', 'habits', [('text:Routines', 'text:Routines')]),
        ('36-habits-vista', 'habits', [('text:Vista', 'text:Vista')]),
        ('37-habits-today', 'habits', [('text:Today', 'text:Today')]),
        ('38-goals-carousel', 'goals', [('css:[aria-label="Carousel"]', 'desc:horz')]),
        ('39-goals-grid', 'goals', [('css:[aria-label="Grid"]', 'desc:grid')]),
        ('40-settings-look', 'settings/look', []),
        ('41-settings-notif', 'settings/notif', []),
        ('42-settings-data', 'settings/data', []),
        ('43-settings-account', 'settings/account', []),
    ]
    if g:
        L += [('44-goal-path', 'goal/' + g['id'], [('text:Path', 'text:Path')]),
              ('45-goal-timeline', 'goal/' + g['id'], [('text:Timeline', 'text:Timeline')]),
              ('46-goal-cards', 'goal/' + g['id'], [('text:Cards', 'text:Cards')])]
    # sheets
    L += [
        ('50-sheet-newgoal', 'goals', [('css:[aria-label="New goal"]', 'desc:plus')]),
        ('51-sheet-newhabit', 'habits', [('css:[aria-label="New habit"]', 'desc:plus')]),
        ('52-sheet-write', 'journal', [('css:[aria-label="Write an entry"]', 'desc:Write')]),
        ('54-journal-timeline', 'journal', [('css:[aria-label="Timeline"]', 'desc:vert')]),
        ('55-threads-timeline', 'threads', []),
        ('56-journal-cards', 'journal', [('css:[aria-label="Cards side by side"]', 'desc:horz')]),
        ('59-fullscreen', 'today', [('css:#view .fsb', 'desc:full')]),   # last: full screen stays on
    ]
    return L
