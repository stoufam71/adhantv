"""fc.py tools | fc.py call <tool> '<json args>' | fc.py batch <file.json>  (list of [tool, args])"""
import json, os, subprocess, sys
CLI = os.environ.get("FIGCRAFT_CLI", "figcraft-cli")  # path to figcraft-cli; default looks it up on PATH
p = subprocess.Popen([CLI, "mcp", "--attach", "47800"], stdin=subprocess.PIPE, stdout=subprocess.PIPE, text=True)
n = 0
def rpc(method, params=None, notify=False):
    global n
    m = {"jsonrpc": "2.0", "method": method}
    if params is not None: m["params"] = params
    if not notify:
        n += 1; m["id"] = n
    p.stdin.write(json.dumps(m) + "\n"); p.stdin.flush()
    if notify: return
    while True:
        r = json.loads(p.stdout.readline())
        if r.get("id") == n: return r
rpc("initialize", {"protocolVersion": "2024-11-05", "capabilities": {}, "clientInfo": {"name": "cli", "version": "0"}})
rpc("notifications/initialized", notify=True)
def call(tool, args):
    r = rpc("tools/call", {"name": tool, "arguments": args})
    res = r.get("result", r)
    txt = "".join(c.get("text", "") for c in res.get("content", [])) if isinstance(res, dict) else str(res)
    return ("ERR " if res.get("isError") or "error" in r else "") + txt
if sys.argv[1] == "tools":
    for t in rpc("tools/list")["result"]["tools"]:
        print(t["name"], "-", t.get("description", "")[:110].replace("\n", " "), "|", ",".join(t.get("inputSchema", {}).get("properties", {}).keys()))
elif sys.argv[1] == "call":
    print(call(sys.argv[2], json.loads(sys.argv[3]))[:int(sys.argv[4]) if len(sys.argv) > 4 else 3000])
else:
    for tool, args in json.load(open(sys.argv[2])):
        out = call(tool, args)
        print(out[:200] if out.startswith("ERR") else out[:80])
p.stdin.close()
