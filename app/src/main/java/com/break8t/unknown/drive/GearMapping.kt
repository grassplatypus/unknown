package com.break8t.unknown.drive

// android.car.VehicleGear 값이다. 테스트가 android 스텁 없이 돌도록 숫자로 적는다.
private const val GEAR_PARK = 0x4
private val NON_PARK_GEARS = setOf(0x1, 0x2, 0x8, 0x10, 0x20, 0x40, 0x80, 0x100, 0x200, 0x400, 0x800, 0x1000)

/** P일 때만 PARKED다. 값이 없거나, 읽을 수 없는 상태거나, 모르는 값이면 UNKNOWN이다. */
internal fun gearMode(raw: Int?, available: Boolean): DriveMode = when {
    !available || raw == null -> DriveMode.UNKNOWN
    raw == GEAR_PARK -> DriveMode.PARKED
    raw in NON_PARK_GEARS -> DriveMode.DRIVING
    else -> DriveMode.UNKNOWN
}
