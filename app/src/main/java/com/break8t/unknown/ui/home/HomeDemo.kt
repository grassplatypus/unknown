package com.break8t.unknown.ui.home

import com.break8t.unknown.reward.DriveSession
import kotlin.time.Duration.Companion.seconds

/** 홈 시연용 견본 값과 레이드 진행 속도. 게임 밸런스가 아니라서 BalanceConfig에 두지 않는다. */
object HomeDemo {
    // 주행 중 FakeRaidSource가 각 상태에 머무는 시간. 시연에서 모든 상태를 보이려고 짧게 둔다.
    val RAID_NONE = 15.seconds
    val RAID_SOLO = 15.seconds
    val RAID_GROUP = 20.seconds
    val RAID_PASSED = 15.seconds

    /** Figma S-09-d 견본: 8분 40초. 이 구간 하나로 받을 코인을 계산한다. */
    const val RAID_JAM_SEC = 520L
    const val RAID_PROGRESS = 258f / 560f
    const val RAID_REMAINING_KM = 1.6
    const val RAID_USUAL_MIN = 6
    const val JAM_MEET_COUNT = 14

    val PARTICIPANTS = listOf(
        Participant("영희", friend = true),
        Participant("철수", friend = true),
        Participant("길동", friend = true),
        Participant("구름나그네", friend = false),
        Participant("달리는감자", friend = false),
        Participant("초보농부", friend = false),
    )

    /** Figma D-02 견본: 70분, 정체 8분 40초와 5분 20초, 안전점수 92점. */
    val SESSION = DriveSession(totalDriveSec = 4200, jamSegmentsSec = listOf(520, 320), safetyScore = 92)
    const val TODAY_KM_PARKED = 42

    // S-08 "이번 주" 카드
    const val WEEK_JAMS = 6
    const val WEEK_SAFETY = 92
    const val WEEK_COINS = 180

    // S-09-a 주행 중
    const val TODAY_KM_DRIVING = 12
    const val TODAY_MIN_DRIVING = 24
    const val DRIVING_JAMS = 0
    const val DRIVING_SAFETY = 100
}
