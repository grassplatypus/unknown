package com.break8t.unknown.ui.island

import com.break8t.unknown.ui.iso.VPC
import kotlin.math.floor

/** How long the car takes to drive in. Screen timing, not a game value. */
const val CAR_DRIVE_MS = 1_500L

/** Where the island car is: driving in from the road, or parked in its spot. */
enum class TripPhase { ARRIVING, PARKED }

/** The car's trip and when its drive in started (epoch ms). */
data class Trip(val phase: TripPhase, val startMs: Long = 0L)

/** The drive in as a state machine. A tap skips it to the end; an interruption (leaving P) parks the car at once. */
object TripRules {
    fun enter(now: Long) = Trip(TripPhase.ARRIVING, now)

    fun tick(t: Trip, now: Long, durationMs: Long = CAR_DRIVE_MS): Trip =
        if (t.phase == TripPhase.ARRIVING && now - t.startMs >= durationMs) t.copy(phase = TripPhase.PARKED) else t

    fun skip(t: Trip): Trip = t.copy(phase = TripPhase.PARKED)

    /** What a trip becomes when the island screen is dropped mid-drive (gear out of P, window change). */
    fun interrupted(t: Trip): Trip = t.copy(phase = TripPhase.PARKED)

    fun driving(t: Trip) = t.phase == TripPhase.ARRIVING

    /** 0 at the start of the drive, 1 at its end. */
    fun progress(t: Trip, now: Long, durationMs: Long = CAR_DRIVE_MS): Float =
        if (t.phase == TripPhase.PARKED) 1f else ((now - t.startMs).toFloat() / durationMs).coerceIn(0f, 1f)
}

/** The car's place on the road in cells and its quarter turns (3 faces up the road toward the house). */
data class CarPose(val y: Float, val rotation: Int)

/** The car on the road from [farY] (off screen) to its spot [parkY], snapped to whole voxels so it moves in pixel steps. */
fun carPose(phase: TripPhase, progress: Float, parkY: Float, farY: Float): CarPose = when (phase) {
    TripPhase.PARKED -> CarPose(parkY, 3)
    TripPhase.ARRIVING -> {
        val p = progress.coerceIn(0f, 1f)
        CarPose(floor((farY + (parkY - farY) * p) * VPC) / VPC, 3)
    }
}
