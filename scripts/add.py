# usage: python3 add.py v110.js|v110.css  < snippet   (inserts the snippet just above the END marker)
import sys
p=sys.argv[1];m={'v110.js':'/* ==== END 1.10 MODULES ==== */','v110.css':'/* ==== END 1.10 CSS ==== */'}[p]
s=open(p).read();add=sys.stdin.read().rstrip('\n')+'\n';i=s.rindex(m);open(p,'w').write(s[:i]+add+s[i:])
