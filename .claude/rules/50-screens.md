# 화면 만들기

화면 묶음을 맡은 에이전트가 따르는 규칙이다. 묶음별 지시문에는 맡은 화면과 진입점만 적는다.

## Figma에서 읽기

- 파일 키 `LrJbNYraGylKrTpgQ0WsJk`, 페이지 "초안 v5". Figma MCP 도구는 ToolSearch로 한 번에 불러온다.
  `create_session`을 먼저 부르고, `get_design_context` 전에 `/figma-design-to-code` 스킬을 불러온다.
- 값(간격, 글자 크기, 색, 구조)은 `get_design_context`에서 가져오고, `get_screenshot`은 비교 기준으로 쓴다.
- 화면 프레임 안의 "App/정체불명"만 앱이다. 차량 패널, 상단 상태 표시줄, 하단 독은 차량 시스템 화면이라 만들지 않는다.
- 빨간 테두리와 빨간 글씨, 이름이 "이전 ·"이나 "변경 표시 ·"로 시작하는 레이어는 검토 메모라서 디자인이 아니다.
- 섹션 "10. 프로토타입 전용 장면"은 보지 않는다.
- 크기는 Figma 1px = 1.3dp. 창 1682×1210dp와 화면을 나눈 841×1210dp 두 크기 모두에서 깨지지 않아야 한다.

## 디자인 패턴은 어디서나 같다

이 앱의 얼굴은 **따뜻한 색의 픽셀 아트**다. 새로 그리는 것, 만들어 내는 것, 외부에서 가져오는 것 모두 같은 패턴을 따른다.

- 색은 `AppColors`와 Figma 일러스트에서 뽑은 팔레트만 쓴다. 매끈한 그라데이션과 흐린 그림자를 쓰지 않는다.
- 그림을 키우거나 줄일 때는 보간 없이 한다(`FilterQuality.None`). 픽셀 그림이 뭉개지면 안 된다.
- 섬과 집은 3D 복셀 오브젝트를 저해상도로 그린 뒤 그대로 확대한다. 평평한 단색 도형이나 고정 그림으로 대신하지 않는다.
- 지도처럼 외부 화면을 들이면 앱 팔레트에 맞게 다시 칠한다.
- Higgsfield 같은 생성 도구로 만든 것도 이 패턴에 맞춰 다듬은 뒤에만 넣는다.
- 화면의 틀(칩, 카드, 버튼, 대화상자, 토스트)은 Figma와 공용 부품을 따른다. 기능마다 다른 모양을 새로 만들지 않는다.
- 화면을 만든 뒤에는 에뮬레이터 캡처와 Figma 스크린샷을 나란히 놓고 비교한다. 비교하지 않은 화면은 끝난 것이 아니다.
- 가로 창과 세로 창(화면 나누기) 둘 다 에뮬레이터에서 확인한다. 세로 창은 `adb shell input swipe 2546 720 1700 720 800`으로 만들고, `adb shell input tap 1706 712`로 좌우를 바꾼다(2560×1440 기준, `docs/emulator-capabilities.md`). 세로로 바뀌면 Activity가 다시 만들어지므로 상태가 이어지는지도 본다.

## 막히면 다른 길로

도구가 막혀도 멈추거나 결과를 줄이지 말고 다른 길을 찾아 계속한다. 보고서에는 어느 길을 썼고 그 길의 한계가 무엇인지 적는다.

- Figma MCP가 호출 한도에 걸리면 Chrome(claude-in-chrome)에서 Figma 플러그인 API로 값을 **읽기만** 한다. Figma 파일은 고치지 않는다.
- Figma에서 그림을 내보낼 수 없으면 `D:\playground\hyundai\assets\farm-demo`의 원본을 쓴다. 가능하면 Figma 것과 같은 파일인지 비교한다.
- adb가 멈추면 버전이 다른 adb가 둘 있는지 본다. SDK의 `platform-tools/adb`를 쓴다.
- 에뮬레이터에서 차량 값을 바꿀 때 `car_service` 명령이 막히면 Virtual Console(8088 포트, `tools/gear.py` 참고)을 쓴다.

## 그림

- 일러스트는 Figma에서 내보내 `res/drawable-nodpi/<묶음>_<이름>.png`로 둔다(예: `farm_isl.png`). 접두어가 같으면 다른 묶음과 이름이 겹치지 않는다.
- 이름이 `ip_`로 시작하는 그림은 절대 내보내지 않는다. 그 자리는 `ch_*` 그림이나 단색 원과 글자로 대신한다.
- 화면 전체 캡처를 그림으로 넣지 않는다. 화면은 Compose로 그린다.

## 코드

- 공용 부품은 `ui/components`에서 가져다 쓴다(PrimaryButton, SecondaryButton, StateButton, AppChip, AppCard, AppDialog, AppToast). 색은 `AppColors`다.
- 사용자 상태(코인, 레벨, 아이템, 보유 목록, 설정 값)는 `LocalDemoState`로 읽고 바꾼다. 그 기능에서만 쓰는 견본 데이터는 자기 패키지 안에 둔다.
- 다른 묶음의 화면으로 가야 하면 진입점의 콜백으로 알리고 직접 부르지 않는다. 연결은 리드가 한다.
- 화면 문구는 `res/values/strings_<묶음>.xml`에 둔다. Figma 문구를 쓰고, 해요체로 쓴다.
- 순수 로직(상태 전이, 계산, 규칙 판단)은 Android 없이 테스트할 수 있게 함수로 빼고 `app/src/test/`에 JUnit 4 테스트를 둔다.
- 각 화면 상태마다 1682×1210dp와 841×1210dp `@Preview`를 둔다.

## 작업 방식

- 자기 worktree에서 일하고, 고칠 수 있는 경로는 지시문에 적힌 것뿐이다. 공용 파일이 필요하면 고치지 않고 보고서에 적는다.
- 에뮬레이터는 쓰지 않는다. 화면 확인은 리드가 합친 뒤에 한다.
- 확인은 worktree 루트에서 `./gradlew assembleDebug testDebugUnitTest --console=plain`(Git Bash)로 한다. 테스트 수는 `app/build/test-results/testDebugUnitTest/*.xml`에서 읽는다.
- 커밋은 `git -C <worktree> add <정확한 경로>`와 `git -C <worktree> commit -m "..."`로 한다. 끝내기 전에 `./gradlew --stop`을 한다.

## 보고서 (마지막 메시지)

1. 바꾼 파일 2. 화면 상태마다 시연에서 어떻게 들어가는지 3. 내보낸 그림 이름과 `ip_*` 대신 쓴 것
4. 빌드와 테스트 명령, 마지막 출력, 테스트 수 5. 커밋 해시 6. 눈으로 확인하지 못한 것 7. 필요한 공용 변경
