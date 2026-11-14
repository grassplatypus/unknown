"""Move the Pleos emulator navi's own position (no root). Usage: python tools/drive.py where|goto LAT LON|path FILE|keys
Drives the navi's hidden joystick location simulator with DPAD keys; enable it first (docs/emulator-capabilities.md).
Every key is sent only while the navi is the top activity, so keys never land in another app."""
import math
import os
import re
import socket
import subprocess
import sys
import time

NAVI = "ai.umos.maps.android.navigation.app"
STEP_M = 0.8          # simulator moves 0.8 m per 50 ms tick while DPAD_UP is held (16 m/s)
TURN_DEG = 2.0        # and turns 2 degrees per tick while DPAD_LEFT/RIGHT is held
TICK_S = 0.05
MM = re.compile(r"\[mm\] org\(([-\d.]+),([-\d.]+):\d+\).*?head\(([-\d.]+)\), speed\(([-\d.]+)\)")


def adb(*args, timeout=20):
    return subprocess.run(["adb", *args], capture_output=True, text=True, encoding="utf-8",
                          errors="replace", timeout=timeout).stdout


class Console:
    """Emulator console (port 5554); `event send` presses keys at the virtual keyboard."""

    def __init__(self, port=5554):
        self.s = socket.create_connection(("127.0.0.1", port), timeout=5)
        self._read()
        token = open(os.path.expanduser("~/.emulator_console_auth_token")).read().strip()
        self.cmd(f"auth {token}")

    def _read(self):
        out = b""
        while not out.endswith(b"OK\r\n") and b"KO:" not in out:
            out += self.s.recv(4096)
        return out.decode(errors="replace")

    def cmd(self, line):
        self.s.sendall((line + "\r\n").encode())
        return self._read()

    def hold(self, key, seconds):
        self.cmd(f"event send EV_KEY:{key}:1")
        time.sleep(max(seconds, 0.06))
        self.cmd(f"event send EV_KEY:{key}:0")


def navi_on_top():
    top = adb("shell", "dumpsys activity activities | grep -m1 topResumedActivity")
    return NAVI in top


def front():
    """Bring the navi forward and confirm; raise if another app keeps the screen."""
    for _ in range(3):
        if navi_on_top():
            return
        adb("shell", "am", "start", "--user", "10", "-n", f"{NAVI}/.MainActivity")
        time.sleep(1.0)
    raise SystemExit("navi is not the top activity; another app holds the screen")


def where(after=0.0):
    """Latest engine input position (lat, lon, head, speed) from the map-matching log, newer than `after`."""
    deadline = time.time() + 6
    while time.time() < deadline:
        lines = adb("logcat", "-d", "-v", "epoch", "-t", "300", "-s", "navi_engine:I").splitlines()
        for line in reversed(lines):
            m = MM.search(line)
            if m:
                ts = float(line.split()[0])
                if ts > after:
                    lon, lat, head, spd = map(float, m.groups())
                    return lat, lon, head, spd, ts
                break
        time.sleep(0.5)
    raise SystemExit("no fresh [mm] line from navi_engine; is the navi running?")


def dist_bearing(lat1, lon1, lat2, lon2):
    r = 6371000.0
    p1, p2 = math.radians(lat1), math.radians(lat2)
    dl = math.radians(lon2 - lon1)
    d = 2 * r * math.asin(math.sqrt(math.sin((p2 - p1) / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2))
    b = math.degrees(math.atan2(math.sin(dl) * math.cos(p2), math.cos(p1) * math.sin(p2) - math.sin(p1) * math.cos(p2) * math.cos(dl)))
    return d, b % 360


def goto(con, lat, lon, tol=15.0, log=print):
    """Closed loop: measure heading from the last move, turn toward the target, drive, repeat."""
    p = where()
    heading = None
    for _ in range(400):
        d, brg = dist_bearing(p[0], p[1], lat, lon)
        if d <= tol:
            return p
        if heading is not None:
            err = (brg - heading + 540) % 360 - 180
            if abs(err) > TURN_DEG:
                front()
                con.hold("KEY_RIGHT" if err > 0 else "KEY_LEFT", abs(err) / TURN_DEG * TICK_S)
        front()
        burst = min(d / (STEP_M / TICK_S), 3.0)
        con.hold("KEY_UP", burst)
        q = where(after=p[4])
        moved, heading = dist_bearing(p[0], p[1], q[0], q[1])
        if moved < 1.0:
            heading = None
        log(f"{q[0]:.6f},{q[1]:.6f} head={q[2]:.0f} to_target={d:.0f}m")
        p = q
    raise SystemExit("did not converge")


def main(argv):
    if not argv or argv[0] in ("-h", "--help"):
        print(__doc__)
        return
    if argv[0] == "where":
        lat, lon, head, spd, _ = where()
        print(f"{lat:.7f} {lon:.7f} head={head} speed={spd}")
        return
    con = Console()
    if argv[0] == "goto":
        goto(con, float(argv[1]), float(argv[2]))
    elif argv[0] == "path":
        for line in open(argv[1], encoding="utf-8"):
            if line.strip() and not line.startswith("#"):
                lat, lon = map(float, line.replace(",", " ").split()[:2])
                goto(con, lat, lon)
    elif argv[0] == "keys":
        front()
        con.hold(argv[1], float(argv[2]))
    else:
        sys.exit(__doc__)


if __name__ == "__main__":
    main(sys.argv[1:])
