"""Append one JSON line per Claude Code hook event to docs/trajectory.jsonl.
Long strings are cut; errors are swallowed so logging never breaks a session."""
import datetime
import json
import pathlib
import sys

REPO = pathlib.Path(__file__).resolve().parents[2]
OUT = REPO / "docs" / "trajectory.jsonl"
LIMIT = 600


def cut(value, limit=LIMIT):
    if isinstance(value, str):
        return value if len(value) <= limit else value[:limit] + f"...(+{len(value) - limit})"
    if isinstance(value, dict):
        return {k: cut(v, limit) for k, v in value.items()}
    if isinstance(value, list):
        return [cut(v, limit) for v in value[:20]]
    return value


def tool_input(name, data):
    # Keep what identifies the action, drop bulky payloads (file contents, edit strings).
    keep = ("file_path", "command", "description", "pattern", "path", "subagent_type",
            "prompt", "url", "skill", "args", "query", "action", "nodeId", "id", "page_id")
    return {k: data[k] for k in keep if k in data}


def tool_result(resp):
    if isinstance(resp, dict):
        out = {k: resp[k] for k in ("success", "error", "exitCode", "interrupted", "filePath") if k in resp}
        if "stderr" in resp and resp["stderr"]:
            out["stderr"] = resp["stderr"]
        return out or {"keys": sorted(resp)[:10]}
    if isinstance(resp, str):
        return {"text": resp}
    return {"type": type(resp).__name__}


def record(p):
    event = p.get("hook_event_name", "?")
    rec = {
        "ts": datetime.datetime.now().astimezone().isoformat(timespec="seconds"),
        "event": event,
        "session": p.get("session_id", "")[:8],
    }
    if p.get("agent_id"):
        rec["agent"] = p.get("agent_type") or p.get("agent_id")
    if event in ("PostToolUse", "PostToolUseFailure"):
        rec["tool"] = p.get("tool_name")
        rec["input"] = tool_input(rec["tool"], p.get("tool_input") or {})
        if event == "PostToolUse":
            rec["result"] = tool_result(p.get("tool_response"))
        else:
            rec["error"] = p.get("error")
    elif event == "UserPromptSubmit":
        rec["prompt"] = p.get("prompt")
    elif event in ("Stop", "SubagentStop"):
        rec["message"] = p.get("last_assistant_message")
        if event == "SubagentStop":
            rec["agent_type"] = p.get("agent_type")
    elif event == "SubagentStart":
        rec["agent_type"] = p.get("agent_type")
    return cut(rec)


def main():
    try:
        payload = json.loads(sys.stdin.buffer.read().decode("utf-8") or "{}")
        OUT.parent.mkdir(parents=True, exist_ok=True)
        with OUT.open("a", encoding="utf-8") as f:
            f.write(json.dumps(record(payload), ensure_ascii=False) + "\n")
    except Exception as e:  # logging must never break the session
        print(f"trajectory hook: {e}", file=sys.stderr)


if __name__ == "__main__":
    main()
