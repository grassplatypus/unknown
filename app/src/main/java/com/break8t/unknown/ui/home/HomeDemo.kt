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
    const val JAM_MEET_COUNT = 14

    /** Figma S-09-b 견본: 1.6 km 남고 막대가 258/560만큼 찼다. */
    val JAM = JamView("서부간선도로", "성산대교", "목동교", remainingM = 1600.0, progress = 258f / 560f, JAM_MEET_COUNT, usualMin = 6)
    const val JAM_LONGEST_M = 1600.0 * 560 / 302

    // 레이드 동안 남은 거리. 정체가 앞에서 늘었다 줄었다 하는 것을 시연에서 보이려고 중간에 세 번 늘린다.
    val FAKE_REMAINING_M = listOf(1600.0, 1500.0, 1420.0, 1480.0, 1560.0, 1450.0, 1300.0, 1180.0, 1240.0, 1100.0, 950.0, 800.0, 860.0, 700.0)

    // 앞의 셋만 화면에 보여서 테두리 단계가 하나씩 다르게 둔다. 나머지 단계는 모두 보기에서 보인다.
    val PARTICIPANTS = listOf(
        Participant("영희", friend = true, level = 23),
        Participant("철수", friend = true, level = 12),
        Participant("길동", friend = true, level = 6),
        Participant("구름나그네", friend = false, level = 15),
        Participant("달리는감자", friend = false, level = 8),
        Participant("초보농부", friend = false, level = 2),
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
