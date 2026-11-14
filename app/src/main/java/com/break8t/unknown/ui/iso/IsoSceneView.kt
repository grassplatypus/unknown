package com.break8t.unknown.ui.iso

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.break8t.unknown.ui.theme.AppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

/** Size of one low-resolution pixel on screen at zoom 1, in dp. Larger reads chunkier. */
const val PIXEL_DP = 3f

/** What a tap landed on. */
sealed interface IsoHit {
    data class Cell(val x: Int, val y: Int) : IsoHit
    data class Item(val id: String) : IsoHit
}

/** A dragged object's candidate spot and whether the validator accepted it. */
data class Ghost(val placement: Placement, val valid: Boolean)

/** View state a screen keeps across recomposition and window changes. Use [rememberIsoViewState]. */
@Stable
class IsoViewState(preset: ViewPreset = ViewPreset.OBLIQUE, step: Int = 0, night: Boolean = false) {
    var preset by mutableStateOf(preset)
    var step by mutableIntStateOf(step.mod(4))
    var night by mutableStateOf(night)
    var zoom by mutableFloatStateOf(1f)
    var pan by mutableStateOf(Offset.Zero)
    var selectedId by mutableStateOf<String?>(null)
    var ghost by mutableStateOf<Ghost?>(null)
        internal set

    /** Wall time of the last render, for checking smoothness on the device. */
    var lastRenderMs by mutableLongStateOf(0L)
        internal set

    val camera: IsoCamera get() = IsoCamera.of(preset, step)

    fun rotateView(by: Int = 1) {
        step = (step + by).mod(4)
    }

    fun resetView() {
        zoom = 1f
        pan = Offset.Zero
    }

    /** Turns the selected object a quarter turn if [canPlace] allows it. Returns whether it turned. */
    fun rotateSelected(scene: IsoScene, canPlace: (Placement) -> Boolean, onPlace: (Placement) -> Unit): Boolean {
        val p = selectedId?.let(scene::turned) ?: return false
        if (!canPlace(p)) return false
        onPlace(p)
        return true
    }

    companion object {
        const val MIN_ZOOM = 0.7f
        const val MAX_ZOOM = 3f

        val Saver = listSaver<IsoViewState, Any?>(
            save = { listOf(it.preset.ordinal, it.step, it.night, it.zoom, it.pan.x, it.pan.y, it.selectedId) },
            restore = {
                IsoViewState(ViewPreset.entries[it[0] as Int], it[1] as Int, it[2] as Boolean).apply {
                    zoom = it[3] as Float
                    pan = Offset(it[4] as Float, it[5] as Float)
                    selectedId = it[6] as String?
                }
            },
        )
    }
}

@Composable
fun rememberIsoViewState(preset: ViewPreset = ViewPreset.OBLIQUE, night: Boolean = false): IsoViewState =
    rememberSaveable(saver = IsoViewState.Saver) { IsoViewState(preset, 0, night) }

/**
 * Renders [scene] off the main thread into a low-resolution buffer and shows it with nearest-neighbour
 * scaling. Drag pans, pinch zooms, tap reports [onTap]. In [editMode] (oblique and top views) tap or
 * long press selects, and dragging the selection moves a ghost that [canPlace] judges before [onPlace].
 */
@Composable
fun IsoSceneView(
    scene: IsoScene,
    state: IsoViewState,
    modifier: Modifier = Modifier,
    editMode: Boolean = false,
    onTap: (IsoHit) -> Unit = {},
    canPlace: (Placement) -> Boolean = scene::isFree,
    onPlace: (Placement) -> Unit = {},
    camera: IsoCamera = state.camera,
) {
    // Whole screen pixels per buffer pixel, so the zoom-1 image matches the viewport at any density.
    val base = max(1, (PIXEL_DP * LocalDensity.current.density).roundToInt())
    BoxWithConstraints(modifier) {
        val bw = constraints.maxWidth / base
        val bh = constraints.maxHeight / base
        SceneCanvas(scene, state, camera, bw, bh, base, editMode, onTap, canPlace, onPlace)
    }
}

@Composable
private fun SceneCanvas(
    scene: IsoScene, state: IsoViewState, camera: IsoCamera, bw: Int, bh: Int, base: Int, editMode: Boolean,
    onTap: (IsoHit) -> Unit, canPlace: (Placement) -> Boolean, onPlace: (Placement) -> Unit,
) {
    val night = state.night
    val selected = state.selectedId
    val ghost = state.ghost
    // Previews do not wait for coroutines, so they render in place.
    val inPreview = LocalInspectionMode.current
    val previewFrame = if (inPreview && bw > 0 && bh > 0) {
        remember(scene, camera, night, selected, ghost, bw, bh) { render(scene, camera, bw, bh, night, selected, ghost) }
    } else null
    // Re-renders only when what is drawn changes; pan and zoom just move and scale the last image.
    val produced by produceState<Rendered?>(null, scene, camera, night, selected, ghost, bw, bh) {
        if (inPreview || bw <= 0 || bh <= 0) return@produceState
        val r = withContext(Dispatchers.Default) { render(scene, camera, bw, bh, night, selected, ghost) }
        value = r
        state.lastRenderMs = r.ms
    }
    val rendered = previewFrame ?: produced
    val live = rememberUpdatedState(Live(scene, rendered, editMode && camera.pitch >= EDIT_MIN_PITCH, onTap, canPlace, onPlace))

    Box(Modifier.fillMaxSize().pointerInput(state, base) { gestures(state, live, base) }) {
        Canvas(Modifier.fillMaxSize()) {
            if (night) drawRect(IsoPalette.Night.copy(alpha = NIGHT_OVERLAY))
            val r = rendered ?: return@Canvas
            val l = layout(r.frame, size, state, base)
            drawImage(
                r.image,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(r.frame.w, r.frame.h),
                dstOffset = IntOffset(l.left, l.top),
                dstSize = IntSize(r.frame.w * l.px, r.frame.h * l.px),
                filterQuality = FilterQuality.None,
            )
        }
        if (rendered == null && bw > 0 && bh > 0) {
            CircularProgressIndicator(Modifier.align(Alignment.Center), color = AppColors.Primary)
        }
    }
}

private class Rendered(val frame: IsoFrame, val image: ImageBitmap, val ms: Long)

private fun render(scene: IsoScene, camera: IsoCamera, bw: Int, bh: Int, night: Boolean, selected: String?, ghost: Ghost?): Rendered {
    val t0 = System.nanoTime()
    val f = renderScene(scene, camera, bw, bh, night, selected, ghost)
    val image = Bitmap.createBitmap(f.pixels, f.w, f.h, Bitmap.Config.ARGB_8888).asImageBitmap()
    return Rendered(f, image, (System.nanoTime() - t0) / 1_000_000)
}

private class Live(
    val scene: IsoScene,
    val rendered: Rendered?,
    val editMode: Boolean,
    val onTap: (IsoHit) -> Unit,
    val canPlace: (Placement) -> Boolean,
    val onPlace: (Placement) -> Unit,
)

private const val NIGHT_OVERLAY = 0.45f
private const val EDIT_MIN_PITCH = 30f // OBLIQUE and steeper; flatter views make floor picking too coarse

/** Where the buffer lands on screen: an integer scale keeps every low-res pixel the same size. */
private class Layout(val left: Int, val top: Int, val px: Int) {
    fun bx(x: Float) = (x - left) / px
    fun by(y: Float) = (y - top) / px
}

private fun layout(f: IsoFrame, size: Size, state: IsoViewState, base: Int): Layout {
    val px = max(1, (base * state.zoom).roundToInt())
    return Layout(
        (size.width / 2f + state.pan.x - f.w * px / 2f).roundToInt(),
        (size.height / 2f + state.pan.y - f.h * px / 2f).roundToInt(),
        px,
    )
}

/** Hit result for buffer point ([bx], [by]): the object under it from the id buffer, else the floor cell. */
internal fun hitTest(scene: IsoScene, f: IsoFrame, bx: Float, by: Float): IsoHit? =
    f.objectAt(floor(bx).toInt(), floor(by).toInt())?.let { IsoHit.Item(it) }
        ?: f.projection.cellAt(bx, by, scene)?.let { (x, y) -> IsoHit.Cell(x, y) }

private const val TAP = 1
private const val MOVE = 2

private suspend fun PointerInputScope.gestures(state: IsoViewState, live: State<Live>, base: Int) =
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val start = down.position
        val l = live.value
        val r = l.rendered
        val view = Size(size.width.toFloat(), size.height.toFloat())
        val lay = r?.let { layout(it.frame, view, state, base) }
        val hit = if (r != null && lay != null) hitTest(l.scene, r.frame, lay.bx(start.x), lay.by(start.y)) else null
        val edit = l.editMode
        var dragId = (hit as? IsoHit.Item)?.id?.takeIf { edit && it == state.selectedId }
        val slop = viewConfiguration.touchSlop
        val kind = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
            var k = 0
            while (k == 0) {
                val e = awaitPointerEvent()
                val c = e.changes.firstOrNull { it.id == down.id }
                k = when {
                    e.changes.count { it.pressed } > 1 -> MOVE
                    c == null || !c.pressed -> TAP
                    (c.position - start).getDistance() > slop -> MOVE
                    else -> 0
                }
            }
            k
        }
        if (kind == TAP) {
            if (edit) state.selectedId = (hit as? IsoHit.Item)?.id
            hit?.let(l.onTap)
            return@awaitEachGesture
        }
        if (kind == null && edit && hit is IsoHit.Item) { // long press picks the object up
            state.selectedId = hit.id
            dragId = hit.id
        }
        val dragged = dragId?.let(l.scene::find)
        val proj = r?.frame?.projection
        val grab = if (dragged != null && proj != null && lay != null) {
            proj.floorCell(lay.bx(start.x), lay.by(start.y))?.let { (gx, gy) -> (gx - dragged.x) to (gy - dragged.y) }
        } else null
        while (true) {
            val e = awaitPointerEvent()
            val pressed = e.changes.count { it.pressed }
            if (pressed == 0) break
            if (pressed > 1) {
                state.ghost = null
                dragId = null
                val centroid = e.calculateCentroid()
                val k = (state.zoom * e.calculateZoom()).coerceIn(IsoViewState.MIN_ZOOM, IsoViewState.MAX_ZOOM) / state.zoom
                val c = Offset(view.width / 2f, view.height / 2f)
                state.zoom *= k
                state.pan = clampPan(centroid - c - (centroid - c - state.pan) * k + e.calculatePan(), state.zoom, view)
            } else if (dragged != null && grab != null && dragId != null && proj != null && lay != null) {
                val pos = e.changes.first { it.pressed }.position
                proj.floorCell(lay.bx(pos.x), lay.by(pos.y))?.let { (cx, cy) ->
                    val p = l.scene.snap(dragged, cx, cy, grab.first, grab.second)
                    if (state.ghost?.placement != p) state.ghost = Ghost(p, live.value.canPlace(p))
                }
            } else {
                val c = e.changes.first { it.pressed }
                state.pan = clampPan(state.pan + c.positionChange(), state.zoom, view)
            }
            e.changes.forEach { it.consume() }
        }
        state.ghost?.let { g ->
            state.ghost = null
            if (g.valid && g.placement != dragged?.placement) live.value.onPlace(g.placement)
        }
    }

private fun clampPan(pan: Offset, zoom: Float, view: Size): Offset {
    val lx = view.width * zoom / 2f
    val ly = view.height * zoom / 2f
    return Offset(pan.x.coerceIn(-lx, lx), pan.y.coerceIn(-ly, ly))
}
