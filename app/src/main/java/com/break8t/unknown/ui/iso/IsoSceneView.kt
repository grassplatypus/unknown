package com.break8t.unknown.ui.iso

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.break8t.unknown.ui.theme.AppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.ceil
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
    overlay: @Composable IsoOverlayScope.() -> Unit = {},
) {
    // Whole screen pixels per buffer pixel, so the zoom-1 image matches the viewport at any density.
    val base = max(1, (PIXEL_DP * LocalDensity.current.density).roundToInt())
    BoxWithConstraints(modifier) {
        val bw = constraints.maxWidth / base
        val bh = constraints.maxHeight / base
        val view = Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
        SceneCanvas(scene, state, camera, bw, bh, base, view, editMode, onTap, canPlace, onPlace, overlay)
    }
}

/**
 * Lets overlay children sit on world points of the scene. Positions are read at layout time, so
 * labels follow pan and zoom without recomposing.
 */
class IsoOverlayScope internal constructor(
    private val box: BoxScope,
    private val projection: Projection,
    private val bw: Int,
    private val bh: Int,
    private val base: Int,
    private val state: IsoViewState,
) : BoxScope by box {
    /** Puts the child's [align] point (bottom centre by default) on the world point ([x], [y], [z]). */
    fun Modifier.isoAnchor(x: Float, y: Float, z: Float, align: Alignment = Alignment.BottomCenter): Modifier = layout { m, c ->
        val p = m.measure(c.copy(minWidth = 0, minHeight = 0))
        layout(p.width, p.height) {
            val s = point(x, y, z, Size(c.maxWidth.toFloat(), c.maxHeight.toFloat()))
            val pin = align.align(IntSize.Zero, IntSize(p.width, p.height), layoutDirection)
            p.place(s.x.roundToInt() - pin.x, s.y.roundToInt() - pin.y)
        }
    }

    /** Sizes and places the child over the screen box of the world rectangle [x0]..[x1], [y0]..[y1] at height [z]. */
    fun Modifier.isoRect(x0: Float, y0: Float, x1: Float, y1: Float, z: Float = 0f): Modifier = layout { m, c ->
        val view = Size(c.maxWidth.toFloat(), c.maxHeight.toFloat())
        val r = rectOnScreen(x0, y0, x1, y1, z, view)
        val p = m.measure(Constraints.fixed(max(0, r.width.roundToInt()), max(0, r.height.roundToInt())))
        layout(p.width, p.height) {
            val now = rectOnScreen(x0, y0, x1, y1, z, view)
            p.place(now.left.roundToInt(), now.top.roundToInt())
        }
    }

    private fun point(x: Float, y: Float, z: Float, view: Size) =
        screenPoint(projection, layoutOf(bw, bh, view, state.zoom, state.pan, base), x, y, z)

    private fun rectOnScreen(x0: Float, y0: Float, x1: Float, y1: Float, z: Float, view: Size): Rect {
        val pts = listOf(point(x0, y0, z, view), point(x1, y0, z, view), point(x0, y1, z, view), point(x1, y1, z, view))
        return Rect(pts.minOf { it.x }, pts.minOf { it.y }, pts.maxOf { it.x }, pts.maxOf { it.y })
    }
}

/** Where world point ([x], [y], [z]) lands on screen for a frame drawn with [l]. */
internal fun screenPoint(p: Projection, l: Layout, x: Float, y: Float, z: Float): Offset =
    Offset(l.left + p.screenX(x, y, z) * l.px, l.top + p.screenY(x, y, z) * l.px)

@Composable
private fun SceneCanvas(
    scene: IsoScene, state: IsoViewState, camera: IsoCamera, bw: Int, bh: Int, base: Int, view: Size, editMode: Boolean,
    onTap: (IsoHit) -> Unit, canPlace: (Placement) -> Boolean, onPlace: (Placement) -> Unit,
    overlay: @Composable IsoOverlayScope.() -> Unit,
) {
    val night = state.night
    val selected = state.selectedId
    val ghost = state.ghost
    // Previews do not wait for coroutines, so they render in place.
    val inPreview = LocalInspectionMode.current
    // Pan and zoom move and scale the last image at once; the part of the buffer on screen (plus a margin) is
    // re-rendered when it changes by a step, so things that run to the screen edge, like the road, keep doing so.
    val window by remember(bw, bh, base, view) {
        derivedStateOf { visibleWindow(bw, bh, view, state.zoom, state.pan, base) }
    }
    val previewFrame = if (inPreview && bw > 0 && bh > 0) {
        remember(scene, camera, night, selected, ghost, bw, bh) { render(scene, camera, bw, bh, night, selected, ghost, window) }
    } else null
    val produced by produceState<Rendered?>(null, scene, camera, night, selected, ghost, bw, bh, window) {
        if (inPreview || bw <= 0 || bh <= 0) return@produceState
        val prev = value
        if (prev != null && prev.window != window && prev.same(scene, camera, night, selected, ghost)) delay(WINDOW_SETTLE_MS)
        val r = withContext(Dispatchers.Default) { render(scene, camera, bw, bh, night, selected, ghost, window) }
        value = r
        state.lastRenderMs = r.ms
        Log.d(LOG_TAG, "render ${r.frame.w}x${r.frame.h} of ${bw}x$bh, ${scene.objects.size} objects, ${r.ms} ms")
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
        } else if (rendered != null) {
            // Same framing renderScene uses, so labels line up with the drawn pixels.
            val proj = remember(scene, camera, bw, bh) { Projection.framing(scene, camera, bw.toFloat(), bh.toFloat(), 1f, Offset.Zero) }
            IsoOverlayScope(this, proj, bw, bh, base, state).overlay()
        }
    }
}

private class Rendered(
    val frame: IsoFrame, val image: ImageBitmap, val ms: Long, val window: Window,
    private val key: List<Any?>,
) {
    fun same(scene: IsoScene, camera: IsoCamera, night: Boolean, selected: String?, ghost: Ghost?) =
        key == listOf(scene, camera, night, selected, ghost)
}

private fun render(
    scene: IsoScene, camera: IsoCamera, bw: Int, bh: Int, night: Boolean, selected: String?, ghost: Ghost?, window: Window,
): Rendered {
    val t0 = System.nanoTime()
    val f = renderScene(scene, camera, bw, bh, night, selected, ghost, window)
    val image = Bitmap.createBitmap(f.pixels, f.w, f.h, Bitmap.Config.ARGB_8888).asImageBitmap()
    return Rendered(f, image, (System.nanoTime() - t0) / 1_000_000, window, listOf(scene, camera, night, selected, ghost))
}

/**
 * The part of the zoom-1 buffer the screen shows at [zoom] and [pan], grown by an eighth on each side and
 * snapped outward to [WINDOW_STEP] pixels so small pans reuse the last render.
 */
internal fun visibleWindow(bw: Int, bh: Int, view: Size, zoom: Float, pan: Offset, base: Int): Window {
    val l = layoutOf(bw, bh, view, zoom, pan, base)
    val x0 = -l.left.toFloat() / l.px; val x1 = (view.width - l.left) / l.px
    val y0 = -l.top.toFloat() / l.px; val y1 = (view.height - l.top) / l.px
    val mx = (x1 - x0) / 8f; val my = (y1 - y0) / 8f
    fun down(v: Float) = Math.floorDiv(floor(v).toInt(), WINDOW_STEP) * WINDOW_STEP
    fun up(v: Float) = -Math.floorDiv(-ceil(v).toInt(), WINDOW_STEP) * WINDOW_STEP
    return Window(down(x0 - mx), down(y0 - my), up(x1 + mx), up(y1 + my))
}

private const val WINDOW_STEP = 32
private const val WINDOW_SETTLE_MS = 120L

private class Live(
    val scene: IsoScene,
    val rendered: Rendered?,
    val editMode: Boolean,
    val onTap: (IsoHit) -> Unit,
    val canPlace: (Placement) -> Boolean,
    val onPlace: (Placement) -> Unit,
)

private const val NIGHT_OVERLAY = 0.45f
private const val LOG_TAG = "IsoScene"
private const val EDIT_MIN_PITCH = 30f // OBLIQUE and steeper; flatter views make floor picking too coarse

/** Where the buffer lands on screen: an integer scale keeps every low-res pixel the same size. */
internal class Layout(val left: Int, val top: Int, val px: Int) {
    fun bx(x: Float) = (x - left) / px
    fun by(y: Float) = (y - top) / px
}

/** Screen placement of [f], which may be a window into its zoom-1 buffer. */
private fun layout(f: IsoFrame, size: Size, state: IsoViewState, base: Int): Layout {
    val l = layoutOf(f.fullW, f.fullH, size, state.zoom, state.pan, base)
    return Layout(l.left + f.ox * l.px, l.top + f.oy * l.px, l.px)
}

internal fun layoutOf(w: Int, h: Int, size: Size, zoom: Float, pan: Offset, base: Int): Layout {
    val px = max(1, (base * zoom).roundToInt())
    return Layout(
        (size.width / 2f + pan.x - w * px / 2f).roundToInt(),
        (size.height / 2f + pan.y - h * px / 2f).roundToInt(),
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
