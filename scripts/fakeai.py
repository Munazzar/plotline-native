# Fake OpenAI-compatible server with CORS + SSE streaming, for testing the Insights "Your AI server" engine.
import http.server,json,time,sys
class H(http.server.BaseHTTPRequestHandler):
  def cors(self):
    self.send_header('Access-Control-Allow-Origin','*');self.send_header('Access-Control-Allow-Headers','*');self.send_header('Access-Control-Allow-Methods','GET,POST,OPTIONS')
  def do_OPTIONS(self):self.send_response(204);self.cors();self.end_headers()
  def do_GET(self):
    b=json.dumps({'data':[{'id':'llama3.2'}]}).encode();self.send_response(200);self.cors();self.send_header('Content-Type','application/json');self.end_headers();self.wfile.write(b)
  def do_POST(self):
    n=int(self.headers.get('Content-Length',0));body=json.loads(self.rfile.read(n));open('/tmp/claude-0/-home-claude/4cd044a0-c38e-5c9e-abda-a471eab1b1b3/scratchpad/lastreq.json','w').write(json.dumps(body))
    self.send_response(200);self.cors();self.send_header('Content-Type','text/event-stream');self.end_headers()
    for t in ['**Momentum is real.** ','You finished the shoe fitting [1] ','and your notes show energy after runs [2, 3].\n','- Mornings work best\n','- Weekends slip\n','Next step: book the 10K this week.']:
      self.wfile.write(('data: '+json.dumps({'choices':[{'delta':{'content':t}}]})+'\n\n').encode());self.wfile.flush();time.sleep(.05)
    self.wfile.write(b'data: [DONE]\n\n')
  def log_message(self,*a):pass
http.server.ThreadingHTTPServer(('127.0.0.1',8766),H).serve_forever()
