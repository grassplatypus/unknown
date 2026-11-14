package com.break8t.unknown.ui.island

import com.break8t.unknown.ui.iso.VPC
import kotlin.math.floor

/** How long the car takes to drive in or out. Screen timing, not a game value. */
const val CAR_DRIVE_MS = 1_500L

/** Where the island car is. ARRIVING and LEAVING are the drives; GONE means the leave can navigate now. */
enum class TripPhase { ARRIVING, PARKED, LEAVING, GONE }

/** The car's trip and when its current drive started (epoch ms). */
data class Trip(val phase: TripPhase, val startMs: Long = 0L)

/**
 * The car trip as a state machine. Taps skip a drive to its end, and leaving P ends any drive at once:
 * an arrival becomes parked and a departure becomes gone, so the driving cover is never held back.
 */
object TripRules {
    fun enter(now: Long) = Trip(TripPhase.ARRIVING, now)

    /** A leave request: drives out only while parked in P; otherwise the car is gone at once. */
    fun leave(t: Trip, now: Long, parked: Boolean): Trip =
        if (!parked || t.phase == TripPhase.GONE) Trip(TripPhase.GONE, now) else Trip(TripPhase.LEAVING, now)

    fun tick(t: Trip, now: Long, durationMs: Long = CAR_DRIVE_MS): Trip =
        if (now - t.startMs < durationMs) t else end(t)

    fun skip(t: Trip): Trip = end(t)

    fun gear(t: Trip, parked: Boolean): Trip = if (parked) t else end(t)

    fun driving(t: Trip) = t.phase == TripPhase.ARRIVING || t.phase == TripPhase.LEAVING

    /** 0 at the start of the drive, 1 at its end. */
    fun progress(t: Trip, now: Long, durationMs: Long = CAR_DRIVE_MS): Float = when (t.phase) {
        TripPhase.PARKED, TripPhase.GONE -> 1f
        else -> ((now - t.startMs).toFloat() / durationMs).coerceIn(0f, 1f)
    }

    private fun end(t: Trip) = when (t.phase) {
        TripPhase.ARRIVING -> t.copy(phase = TripPhase.PARKED)
        TripPhase.LEAVING -> t.copy(phase = TripPhase.GONE)
        else -> t
    }
}

/** The car's place on the road in cells and its quarter turns (1 faces down the road, 3 faces the house). */
data class CarPose(val y: Float, val rotation: Int)

/**
 * The car on the road at x = road, from [farY] (off screen) to its parking spot [parkY]. Positions snap to
 * whole voxels, so it moves in pixel steps; leaving starts with two quarter turns in the spot, then drives off.
 */
fun carPose(phase: TripPhase, progress: Float, parkY: Float, farY: Float): CarPose {
    fun snap(y: Float) = floor(y * VPC) / VPC
    val p = progress.coerceIn(0f, 1f)
    return when (phase) {
        TripPhase.ARRIVING -> CarPose(snap(farY + (parkY - farY) * p), 3)
        TripPhase.PARKED -> CarPose(parkY, 3)
        TripPhase.LEAVING, TripPhase.GONE -> when {
            p < TURN / 2 -> CarPose(parkY, 3)
            p < TURN -> CarPose(parkY, 0)
            else -> CarPose(snap(parkY + (farY - parkY) * (p - TURN) / (1f - TURN)), 1)
        }
    }
}

private const val TURN = 0.2f // share of the leave spent turning round in the parking spot
