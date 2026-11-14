package com.playground.jeongche.drive

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** 기어가 P일 때만 PARKED다. 값을 아직 못 읽었거나 읽기에 실패하면 UNKNOWN이고, 주행 중처럼 막는다. */
enum class DriveMode {
    UNKNOWN, PARKED, DRIVING;

    val isParked: Boolean get() = this == PARKED
}

interface DriveStateSource {
    val mode: StateFlow<DriveMode>
    fun start()
    fun stop()
}

/** 기기 없이 화면을 볼 때와 테스트에서 쓴다. */
class FakeDriveStateSource(initial: DriveMode = DriveMode.PARKED) : DriveStateSource {
    private val state = MutableStateFlow(initial)
    override val mode: StateFlow<DriveMode> = state
    fun set(mode: DriveMode) { state.value = mode }
    override fun start() = Unit
    override fun stop() = Unit
}
