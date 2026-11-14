package com.break8t.unknown.ui.iso

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import com.break8t.unknown.ui.theme.AppColors
import kotlin.math.abs

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
 * Draws [scene] in one Canvas. Drag pans, pinch zooms, tap reports [onTap]. With [editMode] in an
 * [ViewPreset.editable] view, tap or long press selects; dragging the selection moves a ghost that
 * [canPlace] judges, and release calls [onPlace] only when it is valid.
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
) {
    val camera = state.camera
    val drawList = remember(scene, camera) { buildDrawList(scene, camera) }
    val ghost = state.ghost
    val ghostBoxes = remember(ghost, scene, camera) {
        ghost?.let { g -> scene.find(g.placement.id)?.moved(g.placement) }
            ?.let { paintOrder(it.worldBoxes(scene.baseZ(it)), camera) }
            .orEmpty()
    }
    val path = remember { Path() }
    val paint = remember { Paint().apply { isAntiAlias = false } }
    val size = remember { floatArrayOf(0f, 0f) }
    val live = rememberUpdatedState(Live(scene, drawList, editMode, onTap, canPlace, onPlace))

    Canvas(modifier.pointerInput(state) { gestures(state, size, live) }) {
        size[0] = this.size.width; size[1] = this.size.height
        val p = Projection.framing(scene, camera, size[0], size[1], state.zoom, state.pan)
        val night = state.night
        if (night) drawRect(IsoPalette.NightOverlay)
        val boxes = drawList.boxes
        for (i in 0 until drawList.firstObject) drawBox(boxes[i], p, night, path, paint)
        for (i in drawList.firstObject until boxes.size) drawBox(boxes[i], p, night, path, paint)
        // Drawn over the objects: a sofa covers its whole footprint, so an outline under it would not show.
        state.selectedId?.let { id -> scene.find(id)?.let { footprint(it, scene, p, path, AppColors.Selected, fill = false) } }
        if (ghost != null) {
            val tint = if (ghost.valid) AppColors.Success else AppColors.Error
            scene.find(ghost.placement.id)?.moved(ghost.placement)?.let { footprint(it, scene, p, path, tint, fill = true) }
            for (b in ghostBoxes) drawBox(b, p, night, path, paint, tint = if (ghost.valid) null else AppColors.Error, alpha = GHOST_ALPHA)
        }
    }
}

private class Live(
    val scene: IsoScene,
    val drawList: DrawList,
    val editMode: Boolean,
    val onTap: (IsoHit) -> Unit,
    val canPlace: (Placement) -> Boolean,
    val onPlace: (Placement) -> Unit,
)

private const val GHOST_ALPHA = 0.75f
private const val E = 1e-4f

/** Front-facing faces only, so a box's faces never overlap each other. */
private fun DrawScope.drawBox(
    b: WorldBox, p: Projection, night: Boolean, path: Path, paint: Paint,
    tint: Color? = null, alpha: Float = 1f,
) {
    val lit = night && b.emissive != null
    val own = when {
        lit -> b.emissive!!
        night -> IsoPalette.night(b.color)
        else -> b.color
    }
    val tinted = if (tint != null) lerp(own, tint, 0.6f) else own
    val base = if (alpha < 1f) tinted.copy(alpha = alpha) else tinted
    fun shade(f: Float) = if (lit) base else IsoPalette.faceShade(base, f)
    if (p.toCamZ > E) quad(p, path, paint, shade(IsoPalette.TOP),
        b.x0, b.y0, b.z1, b.x1, b.y0, b.z1, b.x1, b.y1, b.z1, b.x0, b.y1, b.z1)
    if (p.toCamX > E) quad(p, path, paint, shade(IsoPalette.X_POS),
        b.x1, b.y0, b.z0, b.x1, b.y1, b.z0, b.x1, b.y1, b.z1, b.x1, b.y0, b.z1)
    if (p.toCamX < -E) quad(p, path, paint, shade(IsoPalette.X_NEG),
        b.x0, b.y0, b.z0, b.x0, b.y1, b.z0, b.x0, b.y1, b.z1, b.x0, b.y0, b.z1)
    if (p.toCamY > E) quad(p, path, paint, shade(IsoPalette.Y_POS),
        b.x0, b.y1, b.z0, b.x1, b.y1, b.z0, b.x1, b.y1, b.z1, b.x0, b.y1, b.z1)
    if (p.toCamY < -E) quad(p, path, paint, shade(IsoPalette.Y_NEG),
        b.x0, b.y0, b.z0, b.x1, b.y0, b.z0, b.x1, b.y0, b.z1, b.x0, b.y0, b.z1)
}

private fun DrawScope.quad(
    p: Projection, path: Path, paint: Paint, color: Color,
    ax: Float, ay: Float, az: Float, bx: Float, by: Float, bz: Float,
    cx: Float, cy: Float, cz: Float, dx: Float, dy: Float, dz: Float,
) {
    path.reset()
    path.moveTo(p.screenX(ax, ay, az), p.screenY(ax, ay, az))
    path.lineTo(p.screenX(bx, by, bz), p.screenY(bx, by, bz))
    path.lineTo(p.screenX(cx, cy, cz), p.screenY(cx, cy, cz))
    path.lineTo(p.screenX(dx, dy, dz), p.screenY(dx, dy, dz))
    path.close()
    paint.color = color
    drawIntoCanvas { it.drawPath(path, paint) }
}

private fun DrawScope.footprint(o: IsoObject, scene: IsoScene, p: Projection, path: Path, color: Color, fill: Boolean) {
    val z = scene.baseZ(o) + 0.01f
    val x0 = o.x.toFloat(); val y0 = o.y.toFloat(); val x1 = x0 + o.footW; val y1 = y0 + o.footD
    path.reset()
    path.moveTo(p.screenX(x0, y0, z), p.screenY(x0, y0, z))
    path.lineTo(p.screenX(x1, y0, z), p.screenY(x1, y0, z))
    path.lineTo(p.screenX(x1, y1, z), p.screenY(x1, y1, z))
    path.lineTo(p.screenX(x0, y1, z), p.screenY(x0, y1, z))
    path.close()
    if (fill) drawPath(path, color.copy(alpha = 0.35f)) else drawPath(path, color, style = Stroke(width = 3f * density))
}

/** The topmost object under a screen point; boxes are tested front to back by their visible faces. */
internal fun hitObject(list: DrawList, p: Projection, sx: Float, sy: Float): String? {
    for (i in list.boxes.lastIndex downTo list.firstObject) {
        val b = list.boxes[i]
        if (p.toCamZ > E && inQuad(p, sx, sy, b.x0, b.y0, b.z1, b.x1, b.y0, b.z1, b.x1, b.y1, b.z1, b.x0, b.y1, b.z1)) return b.ownerId
        val fx = if (p.toCamX > 0) b.x1 else b.x0
        if (abs(p.toCamX) > E && inQuad(p, sx, sy, fx, b.y0, b.z0, fx, b.y1, b.z0, fx, b.y1, b.z1, fx, b.y0, b.z1)) return b.ownerId
        val fy = if (p.toCamY > 0) b.y1 else b.y0
        if (abs(p.toCamY) > E && inQuad(p, sx, sy, b.x0, fy, b.z0, b.x1, fy, b.z0, b.x1, fy, b.z1, b.x0, fy, b.z1)) return b.ownerId
    }
    return null
}

/** Hit result for a screen point: an object first, then a floor cell. */
internal fun hitTest(scene: IsoScene, list: DrawList, p: Projection, sx: Float, sy: Float): IsoHit? =
    hitObject(list, p, sx, sy)?.let { IsoHit.Item(it) } ?: p.cellAt(sx, sy, scene)?.let { (x, y) -> IsoHit.Cell(x, y) }

private fun inQuad(
    p: Projection, sx: Float, sy: Float,
    ax: Float, ay: Float, az: Float, bx: Float, by: Float, bz: Float,
    cx: Float, cy: Float, cz: Float, dx: Float, dy: Float, dz: Float,
): Boolean {
    val x1 = p.screenX(ax, ay, az); val y1 = p.screenY(ax, ay, az)
    val x2 = p.screenX(bx, by, bz); val y2 = p.screenY(bx, by, bz)
    val x3 = p.screenX(cx, cy, cz); val y3 = p.screenY(cx, cy, cz)
    val x4 = p.screenX(dx, dy, dz); val y4 = p.screenY(dx, dy, dz)
    val c1 = (x2 - x1) * (sy - y1) - (y2 - y1) * (sx - x1)
    val c2 = (x3 - x2) * (sy - y2) - (y3 - y2) * (sx - x2)
    val c3 = (x4 - x3) * (sy - y3) - (y4 - y3) * (sx - x3)
    val c4 = (x1 - x4) * (sy - y4) - (y1 - y4) * (sx - x4)
    return (c1 >= 0 && c2 >= 0 && c3 >= 0 && c4 >= 0) || (c1 <= 0 && c2 <= 0 && c3 <= 0 && c4 <= 0)
}

private const val TAP = 1
private const val MOVE = 2

private suspend fun PointerInputScope.gestures(state: IsoViewState, size: FloatArray, live: State<Live>) =
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val start = down.position
        val l = live.value
        fun projection() = Projection.framing(l.scene, state.camera, size[0], size[1], state.zoom, state.pan)
        val hit = hitTest(l.scene, l.drawList, projection(), start.x, start.y)
        val edit = l.editMode && state.preset.editable
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
        val grab = dragged?.let { o -> projection().floorCell(start.x, start.y)?.let { (gx, gy) -> (gx - o.x) to (gy - o.y) } }
        while (true) {
            val e = awaitPointerEvent()
            val pressed = e.changes.count { it.pressed }
            if (pressed == 0) break
            if (pressed > 1) {
                state.ghost = null
                dragId = null
                val centroid = e.calculateCentroid()
                val k = (state.zoom * e.calculateZoom()).coerceIn(IsoViewState.MIN_ZOOM, IsoViewState.MAX_ZOOM) / state.zoom
                val c = Offset(size[0] / 2f, size[1] / 2f)
                state.zoom *= k
                state.pan = clampPan(centroid - c - (centroid - c - state.pan) * k + e.calculatePan(), state.zoom)
            } else if (dragged != null && grab != null && dragId != null) {
                val pos = e.changes.first { it.pressed }.position
                projection().floorCell(pos.x, pos.y)?.let { (cx, cy) ->
                    val p = l.scene.snap(dragged, cx, cy, grab.first, grab.second)
                    if (state.ghost?.placement != p) state.ghost = Ghost(p, live.value.canPlace(p))
                }
            } else {
                val c = e.changes.first { it.pressed }
                state.pan = clampPan(state.pan + c.positionChange(), state.zoom)
            }
            e.changes.forEach { it.consume() }
        }
        state.ghost?.let { g ->
            state.ghost = null
            if (g.valid && g.placement != dragged?.placement) live.value.onPlace(g.placement)
        }
    }

private fun PointerInputScope.clampPan(pan: Offset, zoom: Float): Offset {
    val lim = Size(size.width * zoom / 2f, size.height * zoom / 2f)
    return Offset(pan.x.coerceIn(-lim.width, lim.width), pan.y.coerceIn(-lim.height, lim.height))
}
