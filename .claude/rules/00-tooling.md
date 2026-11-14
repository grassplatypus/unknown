# 도구

## 빌드와 테스트

```
./gradlew assembleDebug testDebugUnitTest --console=plain
```

- 새 worktree에는 `local.properties`(SDK 경로)가 없다. 메인 체크아웃의 것을 복사한다.
- worktree를 지우기 전에 `./gradlew --stop`을 먼저 한다. 안 하면 Windows 파일 잠금 때문에 지워지지 않는다.

## 에뮬레이터

- 반드시 `D:\playground\hyundai\tools\run-emulator.cmd`로 띄운다. 다른 방법으로 띄우면 멈추거나 종료 코드 139로 죽는다.
- 에뮬레이터에서 쓸 수 있는 것과 측정값은 `D:\playground\hyundai\docs\emulator-capabilities.md`에 있다.

## Pleos SDK 찾기

웹 검색보다 로컬 인덱스를 먼저 본다.

```
D:/playground/hyundai/.venv/Scripts/python D:/playground/hyundai/tools/search.py "질의" --kind sdk
```

`sdk` 결과가 공식 정의다. `repo` 결과는 다른 팀의 코드라서 근거로 쓸 때 저장소 이름을 밝힌다.

## 여러 에이전트가 함께 일할 때

- 에이전트마다 브랜치 하나와 worktree 하나(`.claude/worktrees/<이름>`)를 쓴다.
- 맡은 경로만 고친다. 공용 파일(Gradle, 매니페스트, `Route.kt`, `AppShell.kt`, `BalanceConfig.kt`, `theme/`, `strings_common.xml`)은 고치지 않고, 필요한 변경을 보고서에 적는다.
- 커밋할 때는 `git add <자기 경로>`만 쓴다. `git add -A`와 `git add .`는 훅이 막는다.
- push하지 않는다. 합치는 일은 리드가 한 번에 하나씩 한다.
- `docs/trajectory.jsonl`은 리드만 커밋한다.
