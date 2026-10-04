s=open('plotline.html').read()
a=s.index('/* ================= INSIGHTS (on-device RAG)');b=s.index('\n/* account-switch choices',a)
js=open('scripts/insights.js').read().strip('\n')
s=s[:a]+js+s[b:]
open('plotline.html','w').write(s);print('ok',len(s))
