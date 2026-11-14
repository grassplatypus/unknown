package com.break8t.unknown.config

/**
 * 배율, 기준, 상한 같은 숫자는 전부 여기 있다. 코드에 숫자를 직접 쓰지 않는다.
 * Notion "개발 지침" 1장의 상수 표와 같은 값이어야 하며, 원본은 이 파일이다.
 */
data class BalanceConfig(
    // 보상 (임시 표시가 붙은 값은 팀이 아직 정하지 않았다)
    val driveCoinPerMin: Double = 0.5, // 임시
    val raidCoinPerMin: Double = 1.2,
    val safeScoreThreshold: Int = 80, // 임시
    val safeBonusCoin: Int = 10, // 임시
    val xpPerMin: Double = 1.0, // 임시
    val xpJamWeight: Double = 1.5, // 임시

    // 계정
    val maxAccounts: Int = 5,
    val autoLoginSec: Int = 10,
    val splashHoldSec: Int = 5, // 시작 안전 안내를 읽을 수 있게 최소 이만큼 보인다(팀 피드백)
    val pinLength: Int = 4,
    val pinFailAlertCount: Int = 3,
    val loginQrTtlSec: Int = 5 * 60,
    val nicknameMax: Int = 10,
    val statusMessageMax: Int = 20,

    // 친구, 레이드
    val maxFriends: Int = 30,
    val friendCodeTtlSec: Int = 5 * 60,
    val drivingStatusPublicDefault: Boolean = true,
    val raidVisibleAvatars: Int = 3,

    // 필드 드랍, 섬, 상점
    val mushroomDailyLimit: Int = 10,
    val mushroomRadiusM: Int = 20,
    // 필드 드랍 지도를 처음 열 때와 현재 위치 버튼을 누를 때 카메라 높이(위에서 내려다봄, 기울이지 않음).
    val fieldDropAltitudeM: Int = 200,
    val farmGridSize: Int = 3,
    val houseGridSize: Int = 5,
    val houseWallHeight: Int = 4,
    val shopMaxQty: Int = 10,

    // 도감 뱃지: 엽서를 모은 장소 수가 이 수에 닿을 때마다 뱃지 하나가 열린다(같은 장소의 다른 판은 한 곳). 작은 수부터 적는다.
    val postcardBadgeSteps: List<Int> = listOf(3, 5, 10),
    // 레이드 아바타 테두리: 레벨이 이 수에 닿을 때마다 기본, 은, 금, 금과 보석 순으로 한 단계 오른다.
    val avatarFrameLevels: List<Int> = listOf(5, 10, 20),
    // 도감 NEW 뱃지: 얻은 뒤 이 시간이 지나기 전에 아직 눌러 보지 않은 항목에 붙는다.
    val newBadgeHours: Int = 36,
    // 정체 카드 방문 뱃지: 만난 횟수가 이 수에 닿을 때만 그 단계 숫자를 그림 오른쪽 위에 보인다.
    val visitBadgeSteps: List<Int> = listOf(5, 10, 15, 20),
    // 섬 넓히기: 단계별 섬 한 변의 칸 수, 그리고 i단계에서 i+1단계로 넓히는 코인(임시).
    val islandSizes: List<Int> = listOf(10, 12, 14),
    val islandExpandCoins: List<Int> = listOf(200, 400), // 임시
    // 집 넓히기: houseGridSize 다음 단계의 칸 수와 그 단계로 가는 코인(같은 순서, 임시)
    val houseGridSteps: List<Int> = listOf(6, 7),
    val houseGridStepPrices: List<Int> = listOf(150, 300),
) {
    companion object {
        val DEFAULT = BalanceConfig()
    }
}
