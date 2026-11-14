# 정체불명 (Pleos 차량 앱)

현대차 공모전용 Android Automotive 앱이다. Compose로 만들고 Pleos Connect 에뮬레이터(API 34)에서 돌린다.
기획 자료는 Figma "정체불명 시나리오 이미지 / 초안 v5"와 Notion "개발 지침"이다. 숫자의 원본은 Notion이 아니라 `BalanceConfig.kt`다.

꼭 지킬 것:

- 기어가 P가 아니거나 모르면 주행 중으로 본다. 모르는 상태에서 잠금을 풀지 않는다.
- 배율, 기준, 상한은 `config/BalanceConfig.kt`에만 쓴다.
- 화면 문구에 긴 대시를 쓰지 않는다. 커밋에 `Co-Authored-By`를 넣지 않는다.
- 다른 회사 캐릭터 그림(`ip_*`)을 저장소에 넣지 않는다.

```
app/src/main/java/com/break8t/unknown/
  config/      BalanceConfig (상수 전부)
  drive/       기어 상태 (DriveMode, DriveStateSource)
  reward/      보상 계산
  ui/          AppShell, Route, theme/, components/, 화면별 패키지
docs/trajectory.jsonl   훅이 남기는 작업 기록 (로컬 전용, 커밋하지 않음)
```

@.claude/rules/00-tooling.md
@.claude/rules/10-code-style.md
@.claude/rules/20-architecture.md
@.claude/rules/30-domain.md
@.claude/rules/40-safety.md
@.claude/rules/50-screens.md
