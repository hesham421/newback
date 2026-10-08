"""Sequential HTTP timing of the tenant filter path (ADR-TENANT-004 M2). Keep-alive, one connection, client-side timing."""
import http.client, json, os, sys, time, statistics

base_port = int(sys.argv[1]); label = sys.argv[2]; n = int(sys.argv[3]) if len(sys.argv) > 3 else 3000; warm = 500
pw = os.environ["ERP_BOOTSTRAP_ADMIN_PASSWORD"]
conn = http.client.HTTPConnection("localhost", base_port, timeout=30)

def call(method, path, headers=None, body=None):
    h = {"Accept": "application/json"}; h.update(headers or {})
    data = None
    if body is not None:
        data = json.dumps(body).encode(); h["Content-Type"] = "application/json"
    t0 = time.perf_counter_ns()
    conn.request(method, path, body=data, headers=h)
    r = conn.getresponse(); payload = r.read()
    return r.status, payload, time.perf_counter_ns() - t0

st, payload, _ = call("POST", "/api/v1/sec/auth/login", {"X-Tenant-Code": "PLATFORM"}, {"username": "admin", "password": pw})
assert st == 200, (st, payload[:300])
token = json.loads(payload)["data"]["accessToken"]

cases = {
    "token /tenant/me": ({"Authorization": "Bearer " + token}, 200),
    "header-only /tenant/me": ({"X-Tenant-Code": "PLATFORM"}, 401),
}
out = {"label": label}
for name, (headers, expect) in cases.items():
    for _ in range(warm):
        s, p, _ = call("GET", "/api/v1/tenant/me", headers)
        assert s == expect, (name, s, p[:200])
    samples = []
    for _ in range(n):
        s, p, dt = call("GET", "/api/v1/tenant/me", headers)
        assert s == expect, (name, s)
        samples.append(dt / 1e6)
    samples.sort()
    q = lambda f: samples[min(len(samples) - 1, int(round(f * len(samples))) - 1)]
    out[name] = {"n": n, "p50": round(q(0.50), 3), "p95": round(q(0.95), 3), "p99": round(q(0.99), 3),
                 "mean": round(statistics.fmean(samples), 3)}
print(json.dumps(out))
