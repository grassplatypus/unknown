"""Move the Pleos emulator's built-in navi to a position.
The emulator stamps every NMEA sentence with checksum *47, so the navi drops them; we pick
altitude and a near-zero negative speed so that GGA and RMC really sum to 47 for the next second.
Usage: python tools/navi_gps.py <lat> <lon> [seconds=40]"""
import re
import subprocess
import sys
import time
from datetime import datetime, timezone

TARGET = 0x47


def adb(*args, timeout=15):
    return subprocess.run(["adb", *args], capture_output=True, text=True, encoding="utf-8",
                          errors="replace", timeout=timeout).stdout


def checksum(body):
    x = 0
    for c in body:
        x ^= ord(c)
    return x


def fix(lat, lon, alt, vel):
    adb("emu", "geo", "fix", f"{lon:.6f}", f"{lat:.6f}", f"{alt:.1f}", "10", f"{vel:.2f}")


def latest_sentences():
    """Newest rejected GGA and RMC, and the offset (sentence second - device epoch second)."""
    log = adb("logcat", "-d", "-v", "epoch", "-s", "GpsLocationSource:W")
    gga = re.findall(r"([0-9]+)\.[0-9]+ .*\$(GPGGA,[^*]*)\*47", log)
    rmc = re.findall(r"\$(GPRMC,[^*]*)\*47", log)
    if not gga or not rmc:
        return None, None, None
    epoch, body = gga[-1]
    t = body.split(",")[1]
    day = int(epoch) - int(epoch) % 86400
    sent = day + int(t[:2]) * 3600 + int(t[2:4]) * 60 + int(t[4:6])
    return body, rmc[-1], sent - int(epoch)


def navi_position():
    lines = [l for l in adb("logcat", "-d", "-s", "navi_engine:I").splitlines() if "mapmatching" in l]
    m = re.search(r"org\(([0-9.]+),([0-9.]+)", lines[-1]) if lines else None
    return (float(m.group(2)), float(m.group(1))) if m else None


def with_time(body, hhmmss):
    parts = body.split(",")
    parts[1] = hhmmss
    return ",".join(parts)


def choose(gga, rmc, hhmmss):
    """Altitude and speed that make both sentences sum to 47 at this time, or None."""
    g, r = gga.split(","), rmc.split(",")
    alt = vel = None
    for a in range(100, 200):  # 10.0 .. 19.9: three digits flip the high nibble to 4
        g[9] = f"{a / 10:.1f}"
        if checksum(with_time(",".join(g), hhmmss)) == TARGET:
            alt = a / 10
            break
    for v in range(1, 100):  # -0.01 .. -0.99: the minus sign flips the high nibble to 4
        r[7] = f"{-v / 100:.2f}"
        if checksum(with_time(",".join(r), hhmmss)) == TARGET:
            vel = -v / 100
            break
    return (alt, vel) if alt is not None and vel is not None else None


def main():
    lat, lon = float(sys.argv[1]), float(sys.argv[2])
    seconds = int(sys.argv[3]) if len(sys.argv) > 3 else 40
    fix(lat, lon, 10.0, -0.01)
    time.sleep(2.5)
    gga, rmc, offset = latest_sentences()
    if not gga:
        sys.exit("no NMEA rejections in the log yet; is the navi running?")
    # 문장은 초가 바뀐 직후 나오고, 찍히는 시각은 기기 epoch 초 + offset이다. 초 경계 직후에 그 초용 값을 보낸다.
    deadline = time.time() + seconds
    while time.time() < deadline:
        time.sleep(1 - (time.time() % 1) + 0.01)
        target = datetime.fromtimestamp(int(time.time()) + offset, timezone.utc).strftime("%H%M%S")
        picked = choose(gga, rmc, target)
        if picked:
            fix(lat, lon, *picked)
        pos = navi_position()
        if pos and abs(pos[0] - lat) < 0.001 and abs(pos[1] - lon) < 0.001:
            print(f"navi moved to {pos[0]:.6f},{pos[1]:.6f}")
            return
    print(f"navi still at {navi_position()}; try again or increase seconds")


if __name__ == "__main__":
    main()
