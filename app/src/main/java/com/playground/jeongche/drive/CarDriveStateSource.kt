package com.playground.jeongche.drive

import android.car.Car
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** AAOS GEAR_SELECTION으로 기어를 읽는다. Pleos Vehicle SDK는 서비스 연결이 끊겨도
 *  리스너에 알리지 않아서 지난 P 값이 남을 수 있으므로 쓰지 않는다. */
class CarDriveStateSource(context: Context) : DriveStateSource {
    private val context = context.applicationContext
    private val state = MutableStateFlow(DriveMode.UNKNOWN)
    override val mode: StateFlow<DriveMode> = state

    // 한 줄로 돌려서 start와 stop의 차량 호출이 섞이지 않게 한다. 메인 스레드는 쓰지 않는다.
    private val worker = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))
    private val lock = Any()
    private var session: Session? = null

    override fun start() = replace(Session())

    override fun stop() = replace(null)

    private fun replace(next: Session?) {
        val old = synchronized(lock) {
            session.also { session = next; state.value = DriveMode.UNKNOWN }
        }
        worker.launch {
            old?.close()
            next?.open()
        }
    }

    private inner class Session {
        private var car: Car? = null
        private var latest = Long.MIN_VALUE

        private val callback = object : CarPropertyManager.CarPropertyEventCallback {
            override fun onChangeEvent(value: CarPropertyValue<*>) = report(value)
            override fun onErrorEvent(propertyId: Int, areaId: Int) = fail("error event")
        }

        fun open() {
            try {
                car = Car.createCar(context, null, Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT) { c, ready ->
                    worker.launch { if (ready) listen(c) else fail("car service disconnected") }
                }
            } catch (e: Exception) {
                fail("createCar: $e")
            }
        }

        fun close() {
            runCatching { (car?.getCarManager(Car.PROPERTY_SERVICE) as? CarPropertyManager)?.unregisterCallback(callback) }
            runCatching { car?.disconnect() }
            car = null
        }

        // 등록을 먼저 하고 현재값을 읽는다. 늦게 온 현재값은 timestamp로 걸러낸다.
        private fun listen(c: Car) {
            if (!isCurrent()) return
            try {
                val pm = c.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
                val gear = VehiclePropertyIds.GEAR_SELECTION
                if (!pm.registerCallback(callback, gear, CarPropertyManager.SENSOR_RATE_ONCHANGE)) {
                    return fail("registerCallback returned false")
                }
                pm.getProperty(Int::class.javaObjectType, gear, 0)?.let(::report)
            } catch (e: Exception) {
                fail("listen: $e")
            }
        }

        private fun isCurrent() = synchronized(lock) { session === this }

        private fun report(value: CarPropertyValue<*>) = synchronized(lock) {
            if (session !== this || value.timestamp < latest) return@synchronized
            latest = value.timestamp
            state.value = gearMode(value.value as? Int, value.status == CarPropertyValue.STATUS_AVAILABLE)
        }

        private fun fail(why: String) = synchronized(lock) {
            if (session !== this) return@synchronized
            Log.w(TAG, "gear unreadable: $why")
            state.value = DriveMode.UNKNOWN
        }
    }

    private companion object {
        const val TAG = "DriveState"
    }
}
