package com.break8t.unknown.ui.iso

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin

/** Camera presets for the photo views. Pitch 30 on the oblique view gives the 2:1 pixel-art tile. */
enum class ViewPreset(val yaw: Float, val pitch: Float) {
    OBLIQUE(45f, 30f),
    LOW_FRONT(0f, 12f),
    SIDE(90f, 12f),
    TOP(0f, 90f);

    /** Hit testing and editing are only promised in these views. */
    val editable: Boolean get() = this == OBLIQUE || this == TOP
}

/** Orthographic camera. [yaw] turns the world about z (degrees), [pitch] 90 looks straight down. */
@Immutable
data class IsoCamera(val yaw: Float, val pitch: Float) {
    companion object {
        fun of(preset: ViewPreset, step: Int) = IsoCamera(preset.yaw + 90f * step.mod(4), preset.pitch)
    }
}

/**
 * World (x east, y south, z up, in cells) to screen pixels. [unit] is pixels per cell edge and
 * ([originX], [originY]) is where the scene point ([cx], [cy], 0) lands.
 */
class Projection(
    camera: IsoCamera,
    val unit: Float,
    val originX: Float,
    val originY: Float,
    val cx: Float,
    val cy: Float,
) {
    private val cosY = cos(Math.toRadians(camera.yaw.toDouble())).toFloat()
    private val sinY = sin(Math.toRadians(camera.yaw.toDouble())).toFloat()
    private val cosP = cos(Math.toRadians(camera.pitch.toDouble())).toFloat()
    private val sinP = sin(Math.toRadians(camera.pitch.toDouble())).toFloat()

    /** Unit vector from the scene toward the camera; its signs pick the visible faces. */
    val toCamX = sinY * cosP
    val toCamY = cosY * cosP
    val toCamZ = sinP

    /** World direction of screen right; picks which side face reads as the left (lit) one. */
    val rightX = cosY
    val rightY = -sinY

    fun screenX(x: Float, y: Float, @Suppress("UNUSED_PARAMETER") z: Float): Float =
        originX + unit * ((x - cx) * cosY - (y - cy) * sinY)

    fun screenY(x: Float, y: Float, z: Float): Float =
        originY + unit * (((x - cx) * sinY + (y - cy) * cosY) * sinP - z * cosP)

    /** Larger is nearer the camera. */
    fun depth(x: Float, y: Float, z: Float): Float =
        ((x - cx) * sinY + (y - cy) * cosY) * cosP + z * sinP

    /** The world point on the plane at height [z] under a screen point, or null when the view is too flat. */
    fun floorPoint(sx: Float, sy: Float, z: Float = 0f): Offset? {
        if (sinP < MIN_HIT_SIN) return null
        val u = (sx - originX) / unit
        val v = ((sy - originY) / unit + z * cosP) / sinP
        return Offset(cx + u * cosY + v * sinY, cy - u * sinY + v * cosY)
    }

    /** The grid cell under a screen point, or null when it is off the grid. */
    fun cellAt(sx: Float, sy: Float, scene: IsoScene): Pair<Int, Int>? {
        val (x, y) = floorCell(sx, sy) ?: return null
        return if (scene.inside(x, y)) x to y else null
    }

    /** Like [cellAt] but without the grid bounds, for dragging. */
    fun floorCell(sx: Float, sy: Float): Pair<Int, Int>? {
        val p = floorPoint(sx, sy) ?: return null
        return floor(p.x).toInt() to floor(p.y).toInt()
    }

    companion object {
        private const val MIN_HIT_SIN = 0.05f

        /** Fits the scene into [width] x [height], then applies [zoom] and [pan] (pixels). */
        fun framing(scene: IsoScene, camera: IsoCamera, width: Float, height: Float, zoom: Float, pan: Offset): Projection {
            val cx = scene.width / 2f
            val cy = scene.depth / 2f
            val p = Projection(camera, 1f, 0f, 0f, cx, cy)
            val b = scene.frame ?: FrameBox(0f, 0f, scene.width.toFloat(), scene.depth.toFloat(), -scene.floorDepth, maxOf(scene.wallHeight, HEADROOM))
            var minU = Float.MAX_VALUE; var maxU = -Float.MAX_VALUE
            var minV = Float.MAX_VALUE; var maxV = -Float.MAX_VALUE
            for (x in floatArrayOf(b.x0, b.x1)) for (y in floatArrayOf(b.y0, b.y1))
                for (z in floatArrayOf(b.z0, b.z1)) {
                    val u = p.screenX(x, y, z); val v = p.screenY(x, y, z)
                    minU = min(minU, u); maxU = maxOf(maxU, u); minV = min(minV, v); maxV = maxOf(maxV, v)
                }
            val unit = min(width / (maxU - minU), height / (maxV - minV)) * FIT_MARGIN * zoom
            return Projection(
                camera, unit,
                width / 2f + pan.x - unit * (minU + maxU) / 2f,
                height / 2f + pan.y - unit * (minV + maxV) / 2f,
                cx, cy,
            )
        }

        private const val FIT_MARGIN = 0.92f
        private const val HEADROOM = 3f // tallest island object, so roofs are not cut at zoom 1
    }
}
