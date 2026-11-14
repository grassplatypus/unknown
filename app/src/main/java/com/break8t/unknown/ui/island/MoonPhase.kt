package com.break8t.unknown.ui.island

import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.Instant
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sqrt

/** 평균 삭망월(일). */
const val SYNODIC_DAYS = 29.53059

/** 기준 신월. 이 시각부터 삭망월이 몇 번 지났는지로 위상을 센다. */
val REFERENCE_NEW_MOON: Instant = Instant.parse("2000-01-06T18:14:00Z")

/** 달의 나이를 0 이상 1 미만으로 돌려준다. 0은 신월, 0.25는 상현, 0.5는 보름, 0.75는 하현이다. */
fun moonPhase(at: Instant): Double {
    val days = ChronoUnit.MILLIS.between(REFERENCE_NEW_MOON, at) / 86_400_000.0
    return ((days / SYNODIC_DAYS) % 1.0 + 1.0) % 1.0
}

/** 그날 정오(현지 시각)의 위상. 날짜로 정해서 하루 동안 그림이 바뀌지 않는다. */
fun moonPhase(date: LocalDate, zone: ZoneId): Double = moonPhase(date.atTime(12, 0).atZone(zone).toInstant())

/**
 * 반지름 [r] 픽셀 원판의 픽셀 ([i], [j])가 밝은지. 한국(북반구)에서 보듯 차는 달은 오른쪽부터 밝아진다.
 * 경계를 반 픽셀 넓혀서 신월에는 한 점도, 보름에는 빠지는 점 없이 그려진다.
 */
fun moonLit(i: Int, j: Int, r: Int, phase: Double): Boolean {
    if (i * i + j * j > r * r) return false
    // The waning half mirrors the waxing one, so both quarters leave the same middle column dark.
    val waxing = phase < 0.5
    val edge = cos(2 * PI * (if (waxing) phase else 1 - phase)) * (sqrt((r * r - j * j).toDouble()) + 0.5)
    return (if (waxing) i else -i) > edge
}
