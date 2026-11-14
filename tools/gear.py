"""Switch the Pleos emulator's gear through the Virtual Console (port 8088).
Usage: python tools/gear.py P|R|N|D   or   python tools/gear.py   (show current gear)"""
import json
import subprocess
import sys
import urllib.request

BASE = "http://127.0.0.1:8088"
GEARS = {"P": "Park", "R": "Reverse", "N": "Neutral", "D": "Drive"}


def call(method, path):
    req = urllib.request.Request(BASE + path, method=method, data=b"" if method != "GET" else None)
    with urllib.request.urlopen(req, timeout=10) as r:
        return json.loads(r.read() or b"{}")


def current():
    actual = call("GET", "/vehicle/state")["gear"].get("actual") or {}
    return actual.get("value", "unknown")


def main():
    subprocess.run(["adb", "forward", "tcp:8088", "tcp:8088"], check=True, capture_output=True)
    if len(sys.argv) < 2:
        print(current())
        return
    gear = GEARS.get(sys.argv[1].upper())
    if gear is None:
        sys.exit(f"gear must be one of {', '.join(GEARS)}")
    # 콘솔은 브라우저 세션이 살아 있을 때만 쓰기를 받는다. 심박을 한 번 보내 세션을 연다.
    call("POST", "/browser/session?client=gear-cli")
    result = call("PUT", f"/vehicle/gear?gear={gear}")
    if "error" in result:
        sys.exit(f"console refused: {result['error']}")
    print(current())


if __name__ == "__main__":
    main()
