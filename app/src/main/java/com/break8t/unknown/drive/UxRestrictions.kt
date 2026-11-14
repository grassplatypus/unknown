package com.break8t.unknown.drive

import android.car.Car
import android.car.drivingstate.CarUxRestrictions
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.Context
import android.util.Log
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 플랫폼이 지역 규제에 맞춰 내려주는 주행 중 UX 제한. 앱은 이 값만 따르고 지역을 직접 판단하지 않는다.
 * 값을 아직 못 받았거나 읽지 못하면 UNKNOWN이고, 모든 제한이 걸린 것으로 본다.
 */
data class UxLimits(val requiresDistractionOptimization: Boolean, val activeRestrictions: Int) {
    fun blocks(flags: Int): Boolean = flags != 0 && requiresDistractionOptimization && (activeRestrictions and flags) != 0

    val noSetup get() = blocks(NO_SETUP)
    val noKeyboard get() = blocks(NO_KEYBOARD)
    val noDialpad get() = blocks(NO_DIALPAD)
    val noVideo get() = blocks(NO_VIDEO)

    companion object {
        // android.car.drivingstate.CarUxRestrictions 값. 테스트가 android 스텁 없이 돌도록 숫자로 적는다.
        const val NO_DIALPAD = 0x1
        const val NO_FILTERING = 0x2
        const val LIMIT_STRING_LENGTH = 0x4
        const val NO_KEYBOARD = 0x8
        const val NO_VIDEO = 0x10
        const val LIMIT_CONTENT = 0x20
        const val NO_SETUP = 0x40
        const val NO_TEXT_MESSAGE = 0x80
        const val NO_VOICE_TRANSCRIPTION = 0x100
        const val ALL = 0x1ff

        val NONE = UxLimits(false, 0)
        val UNKNOWN = UxLimits(true, ALL)
    }
}

interface UxLimitsSource {
    val limits: StateFlow<UxLimits>
    fun start()
    fun stop()
}

class FakeUxLimitsSource(initial: UxLimits = UxLimits.NONE) : UxLimitsSource {
    private val state = MutableStateFlow(initial)
    override val limits: StateFlow<UxLimits> = state
    fun set(value: UxLimits) { state.value = value }
    override fun start() = Unit
    override fun stop() = Unit
}

/** CarUxRestrictionsManager로 받는다. 차량 호출은 메인 스레드 밖에서 한 줄로 처리한다. */
class CarUxLimitsSource(context: Context) : UxLimitsSource {
    private val context = context.applicationContext
    private val state = MutableStateFlow(UxLimits.UNKNOWN)
    override val limits: StateFlow<UxLimits> = state
    private val worker = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))
    private var car: Car? = null
    private var manager: CarUxRestrictionsManager? = null

    private fun report(r: CarUxRestrictions?) {
        state.value = r?.let { UxLimits(it.isRequiresDistractionOptimization, it.activeRestrictions) } ?: UxLimits.UNKNOWN
    }

    override fun start() {
        worker.launch {
            try {
                car = Car.createCar(context, null, Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT) { c, ready ->
                    worker.launch {
                        if (!ready) return@launch report(null)
                        try {
                            val m = c.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as CarUxRestrictionsManager
                            manager = m
                            m.registerListener { report(it) }
                            report(m.currentCarUxRestrictions)
                        } catch (e: Exception) {
                            Log.w(TAG, "ux restrictions unreadable: $e")
                            report(null)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "createCar: $e")
                report(null)
            }
        }
    }

    override fun stop() {
        worker.launch {
            runCatching { manager?.unregisterListener() }
            runCatching { car?.disconnect() }
            manager = null
            car = null
            report(null)
        }
    }

    private companion object {
        const val TAG = "UxLimits"
    }
}

val LocalUxLimits = staticCompositionLocalOf { UxLimits.UNKNOWN }
