"""Mechanical checks for this repo, run as PreToolUse(Bash) and PostToolUse(Write|Edit).
Rules come from .claude/rules/00-tooling.md and 10-code-style.md."""
import json
import re
import sys

EM_DASH = "—"
REPO_MARK = re.compile(r"[\\/]playground[\\/]hyundai[\\/]app[\\/]", re.I)
BASH_RULES = [
    (re.compile(r"\bgit\b[^;&|]*\bpush\b"), "Do not push; the lead merges locally."),
    (re.compile(r"\bgit\b[^;&|]*\badd\s+(-A|--all|\.)(\s|$)"), "Stage only your own paths: git add <path>."),
    (re.compile(r"co-authored-by", re.I), "No Co-Authored-By trailer in commits."),
    (re.compile(r"--no-verify"), "Do not skip hooks."),
]


def check_bash(cmd):
    for pattern, reason in BASH_RULES:
        if pattern.search(cmd):
            return reason
    return None


def written_text(tool, data):
    if tool == "Write":
        return data.get("content", "")
    if tool == "Edit":
        return data.get("new_string", "")
    return ""


def check_write(tool, data):
    path = data.get("file_path", "")
    if not REPO_MARK.search(path.replace("/", "\\") + "\\") or not path.endswith((".kt", ".xml", ".md")):
        return None
    bad = [line for line in written_text(tool, data).splitlines() if EM_DASH in line]
    if bad:
        return f"Em dash in {path}; use a comma, colon or parentheses:\n" + "\n".join(bad[:5])
    return None


def main():
    sys.stdout.reconfigure(encoding="utf-8")
    sys.stderr.reconfigure(encoding="utf-8")
    p = json.loads(sys.stdin.buffer.read().decode("utf-8") or "{}")
    event, tool, data = p.get("hook_event_name"), p.get("tool_name"), p.get("tool_input") or {}
    if event == "PreToolUse" and tool == "Bash":
        reason = check_bash(data.get("command", ""))
        if reason:
            print(json.dumps({"hookSpecificOutput": {"hookEventName": "PreToolUse",
                  "permissionDecision": "deny", "permissionDecisionReason": reason}}))
    elif event == "PostToolUse" and tool in ("Write", "Edit"):
        msg = check_write(tool, data)
        if msg:
            print(msg, file=sys.stderr)
            sys.exit(2)


if __name__ == "__main__":
    main()
