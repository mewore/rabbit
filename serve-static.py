import http.server
import socketserver
import os

current_dir = os.path.dirname(__file__)
project_dir = os.path.join(current_dir, 'static')
os.chdir(project_dir)

PORT = 8100

handler = http.server.SimpleHTTPRequestHandler
http_daemon = socketserver.TCPServer(('', PORT), handler)
print('Go to: http://localhost:' + str(PORT))
http_daemon.serve_forever()

