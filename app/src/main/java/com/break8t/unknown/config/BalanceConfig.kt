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

    // 도감 뱃지: 모은 엽서가 이 수에 닿을 때마다 뱃지 하나가 열린다. 작은 수부터 적는다.
    val postcardBadgeSteps: List<Int> = listOf(3, 5, 10),
    // 레이드 아바타 테두리: 레벨이 이 수에 닿을 때마다 기본, 은, 금, 금과 보석 순으로 한 단계 오른다.
    val avatarFrameLevels: List<Int> = listOf(5, 10, 20),
) {
    companion object {
        val DEFAULT = BalanceConfig()
    }
}
