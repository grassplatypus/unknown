# 구조

## 흐름

`MainActivity`가 `DriveStateSources.create()`로 기어 상태 소스를 만들고, `mode`를 `AppShell`에 넘긴다.
`AppShell`은 사이드 메뉴(`SideRail`)와 본문을 그린다. 주행 중이고 `Route.allowedWhileDriving`이 false면 본문 위에 `DrivingCover`를 덮는다.

## 계약

| 이름 | 위치 | 약속 |
|---|---|---|
| `DriveMode` | `drive/DriveState.kt` | UNKNOWN, PARKED, DRIVING. `isParked`는 PARKED일 때만 참 |
| `DriveStateSource` | 같은 파일 | `mode: StateFlow<DriveMode>`, `start()`, `stop()`. 차량 호출은 메인 스레드 밖에서 한다 |
| `RewardCalculator.calculate` | `reward/Reward.kt` | `(DriveSession, BalanceConfig) -> RewardResult`. 부수 효과 없음 |
| `SideRail(current, onSelect, modifier)` | `ui/components/SideRail.kt` | 시그니처를 바꾸지 않는다 |
| `DrivingCover(hint, modifier)` | `ui/components/DrivingCover.kt` | 아래 화면의 터치를 막는다 |
| `LocalUxLimits` | `drive/UxRestrictions.kt` | 플랫폼 주행 규제. `blocks(flag)`, `noSetup`, `noKeyboard`, `noDialpad`. 모르면 전부 막음 |

## 창 크기

에뮬레이터 실측(`docs/emulator-capabilities.md`)은 다음과 같다.

- 기본 창은 가로 1682×1210dp다.
- 화면을 나누면 세로 841×1210dp가 되고, 이때 Activity가 다시 만들어진다.
- 레이아웃은 두 크기에서 모두 깨지지 않아야 한다. 화면 상태는 `rememberSaveable`이나 ViewModel에 둔다.

## 데이터 (백엔드 없음)

- 공모전 시연용이다. 서버, 로그인 서버, 네트워크 호출을 만들지 않는다.
- 여러 화면이 같이 읽는 사용자 상태(코인, 레벨, 닉네임, 고른 자동차와 캐릭터, 음성 안내, 운전 상태 공개)는 `data/DemoState.kt`의 `LocalDemoState`로 읽고 바꾼다.
- 한 기능에서만 쓰는 견본 데이터(정체 카드 목록, 상점 상품 등)는 그 기능 패키지 안에 둔다. 견본 숫자는 Figma 견본과 맞춘다.

## 화면 진입점

| 화면 | 함수 | 위치 |
|---|---|---|
| 시작과 계정 (사이드 메뉴 밖, 로그인 전) | `AccountFlow(mode, onSignedIn, skipSplash)` | `ui/account/` |
| 홈 | `HomeScreen(mode, onNavigate)` | `ui/home/` |
| 필드 드랍 | `FieldDropScreen()` | `ui/fielddrop/` |
| 섬 | `IslandScreen(onOpen)` | `ui/island/` |
| 상점, 보관함 (섬 아래) | `ShopScreen(onBack, onOpenInventory)`, `InventoryScreen(onBack, onOpenShop, onPlaceFurniture)` | `ui/shop/`, `ui/inventory/` |
| 설정 | `SettingsScreen(onSwitchAccount, onLogout, onOpenShop)` | `ui/settings/` |
| 집 (섬 아래) | `HouseScreen(onBack, onOpenInventory, onTakePhoto)` | `ui/house/` |
| 사진 찍기 (섬 아래) | `PhotoScreen(inHouse, onClose)` | `ui/photo/` |
| 공유 QR (도감, 내 정보에서) | `ShareQrScreen(kind, title, subtitle, onClose)` | `ui/photo/` |
| 내 정보, 친구 | `MyInfoScreen(onShare)` | `ui/myinfo/` |
| 도감 | `EncyclopediaScreen(onShare, onOpenFieldDrop)` | `ui/encyclopedia/` (목록 부품은 `ui/collection/`) |

`AppShell`이 사이드 메뉴 선택에 따라 진입점을 부른다. 기능 담당은 진입점 시그니처를 바꾸지 않는다.

## 화면 상태

- 오래 걸리는 작업(차량, 디스크, 네트워크)은 메인 스레드에서 하지 않는다. 하는 동안에는 진행 표시를 보인다.
- 비어 있는 상태는 `loaded && count == 0`일 때만 보인다. 불러오는 중에 "없어요"를 띄우지 않는다.
