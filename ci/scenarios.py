# The screens compared between the 1.13 web build and the native app.
# Each scenario: (name, route). Routes are the web hash routes; NShell.route understands the same ones.
# {goal}, {habit}, {thread}, {today} are filled from the demo data both sides share.
import datetime


def scenarios(state):
    today = datetime.date.today().isoformat()
    g = next((x for x in state.get('goals', []) if x.get('status', 'active') == 'active'), None)
    h = next((x for x in state.get('habits', []) if x.get('status', 'active') == 'active' and x.get('kind') != 'quit'), None)
    q = next((x for x in state.get('habits', []) if x.get('kind') == 'quit'), None)
    t = next(iter(state.get('threads', []) or []), None)
    L = [
        ('01-today', 'today'),
        ('02-habits', 'habits'),
        ('03-cal', 'cal'),
        ('04-goals', 'goals'),
        ('05-vista', 'vista'),
        ('06-map', 'map'),
        ('07-journal', 'journal'),
        ('08-threads', 'threads'),
        ('09-ask', 'ask'),
        ('10-day', 'day/' + today),
        ('11-activity', 'activity'),
        ('12-settings', 'settings'),
    ]
    if g: L.append(('20-goal', 'goal/' + g['id']))
    if h: L.append(('21-habit', 'habit/' + h['id']))
    if q: L.append(('22-habit-quit', 'habit/' + q['id']))
    if t: L.append(('23-thread', 'thread/' + t['id']))
    return L
