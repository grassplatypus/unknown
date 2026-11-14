package com.break8t.unknown.ui.iso

import androidx.compose.ui.graphics.toArgb
import com.break8t.unknown.ui.theme.AppColors
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * One low-resolution frame: ARGB pixels, the object id under each pixel, and the projection used.
 * It is the window at ([ox], [oy]) of the [fullW] x [fullH] buffer the scene was framed for.
 */
class IsoFrame(
    val w: Int,
    val h: Int,
    val pixels: IntArray,
    val ids: IntArray,
    val projection: Projection,
    private val names: Array<String?>,
    val ox: Int = 0,
    val oy: Int = 0,
    val fullW: Int = w,
    val fullH: Int = h,
) {
    /** The object under buffer pixel ([bx], [by]); null for floor, walls, the ghost and empty sky. */
    fun objectAt(bx: Int, by: Int): String? {
        if (bx !in 0 until w || by !in 0 until h) return null
        val id = ids[by * w + bx]
        return if (id > 0) names.getOrNull(id - 1) else null
    }
}

/** A pixel rectangle of the zoom-1 buffer, [x0] and [y0] inclusive, [x1] and [y1] exclusive. */
data class Window(val x0: Int, val y0: Int, val x1: Int, val y1: Int)

/**
 * Renders [scene] framed for a [w] x [h] buffer with a depth buffer, so any camera is correct without
 * sorting. [window] draws only (or beyond) part of that buffer, for panned and zoomed views.
 * Pure Kotlin: runs off the main thread and in unit tests.
 */
fun renderScene(
    scene: IsoScene,
    camera: IsoCamera,
    w: Int,
    h: Int,
    night: Boolean = false,
    selectedId: String? = null,
    ghost: Ghost? = null,
    window: Window = Window(0, 0, w, h),
): IsoFrame {
    val full = Projection.framing(scene, camera, w.toFloat(), h.toFloat(), 1f, androidx.compose.ui.geometry.Offset.Zero)
    val p = Projection(camera, full.unit, full.originX - window.x0, full.originY - window.y0, full.cx, full.cy)
    val r = Raster(window.x1 - window.x0, window.y1 - window.y0, p, night)
    val names = arrayOfNulls<String>(scene.objects.size + 1)
    if (scene.wallHeight > 0f) r.walls(scene)
    val ghostObj = ghost?.let { g -> scene.find(g.placement.id)?.moved(g.placement) }
    r.floor(scene, ghostObj, ghost?.valid ?: true)
    for (road in scene.roads) r.road(road, scene)
    scene.objects.forEachIndexed { i, o ->
        names[i] = if (o.pickable) o.id else null
        if (o.wall != null && !(scene.wallHeight > 0f && wallVisible(o.wall, p))) return@forEachIndexed
        if (o.shape === IsoScenery.laneY) r.roadIds += i + 1
        r.model(o, scene.baseZ(o), i + 1)
    }
    if (ghostObj != null) {
        r.tint = if (ghost!!.valid) IsoPalette.GhostOk else AppColors.Error.toArgb()
        r.tintAmount = if (ghost.valid) 0.35f else 0.6f
        r.bias = 0.01f
        r.model(ghostObj, scene.baseZ(ghostObj), names.size)
    }
    val selected = selectedId?.let { id -> scene.objects.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.plus(1) } ?: 0
    return IsoFrame(r.w, r.h, r.finish(selected), r.ids, p, names, window.x0, window.y0, w, h)
}

/** Cells of [road] whose centre lies within one cell of the frame, counted from its start: 0 until this. */
internal fun roadLength(road: RoadRay, p: Projection, w: Int, h: Int): Int {
    val sx = p.screenX(road.x + 0.5f, road.y + 0.5f, 0f); val sy = p.screenY(road.x + 0.5f, road.y + 0.5f, 0f)
    val vx = p.screenX(road.x + 0.5f + road.dx, road.y + 0.5f + road.dy, 0f) - sx
    val vy = p.screenY(road.x + 0.5f + road.dx, road.y + 0.5f + road.dy, 0f) - sy
    // Steps until the centre is a cell's length past the far edge along each axis it moves on.
    val pad = p.unit * 2f
    fun steps(s: Float, v: Float, size: Int) = when {
        v > 1e-3f -> (size + pad - s) / v
        v < -1e-3f -> (-pad - s) / v
        else -> Float.MAX_VALUE
    }
    val k = min(steps(sx, vx, w), steps(sy, vy, h))
    return if (k == Float.MAX_VALUE) MAX_ROAD else ceil(k).toInt().coerceIn(0, MAX_ROAD)
}

private const val MAX_ROAD = 4096

/** Cells of [road] a [w] x [h] view of [scene] shows, counted from its start, plus a cell or two past the edge. */
fun roadCellsInView(road: RoadRay, scene: IsoScene, camera: IsoCamera, w: Float, h: Float): Int =
    roadLength(road, Projection.framing(scene, camera, w, h, 1f, androidx.compose.ui.geometry.Offset.Zero), w.toInt(), h.toInt())

/** Walls are drawn only where they stand behind the room, so they never hide it; all four from the top. */
internal fun wallVisible(side: WallSide, p: Projection): Boolean {
    if (p.toCamZ > 0.99f) return true
    val facing = when (side) {
        WallSide.NORTH -> p.toCamY
        WallSide.SOUTH -> -p.toCamY
        WallSide.WEST -> p.toCamX
        WallSide.EAST -> -p.toCamX
    }
    return facing > E
}

private const val E = 1e-4f
/** Ids of road ray cells count down from here, below every floor cell id. */
internal const val ROAD_ID = -1_000_000
private const val EDGE_DEPTH = 0.2f // depth jump (cells) inside one object that still gets an outline
private const val OUTLINE = 0.4f // the art outlines in near black
private const val WALL_T = 0.25f

/** Faces lit from the upper left of the screen: top, left-facing and right-facing tones. */
private const val TONE_TOP = 1f
private const val TONE_LEFT = 0.82f
private const val TONE_RIGHT = 0.64f

internal fun interface Texture {
    /** Material color of texel ([i], [j]) out of [ni] x [nj]; j counts up from the bottom of side faces. */
    fun at(i: Int, j: Int, ni: Int, nj: Int, seed: Int): Int
}

internal class Raster(val w: Int, val h: Int, val p: Projection, private val night: Boolean) {
    val color = IntArray(w * h)
    val depth = FloatArray(w * h) { Float.NEGATIVE_INFINITY }
    val ids = IntArray(w * h)
    /** Ids of road surfaces (floor cells, bridge cells, lane paint). Outlines between them only mark real steps. */
    val roadIds = HashSet<Int>()
    var tint = 0
    var tintAmount = 0f
    var bias = 0f

    private fun visible(nx: Int, ny: Int, nz: Int) = nx * p.toCamX + ny * p.toCamY + nz * p.toCamZ > E

    private fun tone(nx: Int, ny: Int, nz: Int): Float {
        if (nz > 0) return TONE_TOP
        val sx = nx * p.rightX + ny * p.rightY
        return if (sx > 0.05f) TONE_RIGHT else TONE_LEFT
    }

    /** Night darkening, face tone, then the ghost or footprint tint. */
    private fun finish(c: Int, tone: Float): Int {
        var r = (c shr 16 and 0xFF).toFloat(); var g = (c shr 8 and 0xFF).toFloat(); var b = (c and 0xFF).toFloat()
        if (night) {
            r += (NIGHT_R - r) * NIGHT_AMOUNT; g += (NIGHT_G - g) * NIGHT_AMOUNT; b += (NIGHT_B - b) * NIGHT_AMOUNT
        }
        r *= tone; g *= tone; b *= tone
        if (tintAmount > 0f) {
            r += ((tint shr 16 and 0xFF) - r) * tintAmount
            g += ((tint shr 8 and 0xFF) - g) * tintAmount
            b += ((tint and 0xFF) - b) * tintAmount
        }
        return argb(r, g, b)
    }

    /** Rasterizes the rectangle O, O+A, O+B, O+A+B. With [tex], [mat] is ignored and texels set the color. */
    fun quad(
        ox: Float, oy: Float, oz: Float, ax: Float, ay: Float, az: Float, bx: Float, by: Float, bz: Float,
        mat: Int, tone: Float, id: Int, tex: Texture? = null, ni: Int = 1, nj: Int = 1, seed: Int = 0, lit: Boolean = false,
    ) {
        val x0 = p.screenX(ox, oy, oz); val y0 = p.screenY(ox, oy, oz); val d0 = p.depth(ox, oy, oz) + bias
        val xa = p.screenX(ox + ax, oy + ay, oz + az) - x0; val ya = p.screenY(ox + ax, oy + ay, oz + az) - y0
        val xb = p.screenX(ox + bx, oy + by, oz + bz) - x0; val yb = p.screenY(ox + bx, oy + by, oz + bz) - y0
        val da = p.depth(ox + ax, oy + ay, oz + az) + bias - d0; val db = p.depth(ox + bx, oy + by, oz + bz) + bias - d0
        val det = xa * yb - ya * xb
        if (abs(det) < 1e-6f) return
        val minX = max(0, floor(min(min(x0, x0 + xa), min(x0 + xb, x0 + xa + xb))).toInt())
        val maxX = min(w - 1, ceil(max(max(x0, x0 + xa), max(x0 + xb, x0 + xa + xb))).toInt())
        val minY = max(0, floor(min(min(y0, y0 + ya), min(y0 + yb, y0 + ya + yb))).toInt())
        val maxY = min(h - 1, ceil(max(max(y0, y0 + ya), max(y0 + yb, y0 + ya + yb))).toInt())
        val flat = if (tex == null) (if (lit) mat else finish(mat, tone)) else 0
        for (py in minY..maxY) {
            val cy = py + 0.5f - y0
            for (px in minX..maxX) {
                val cx = px + 0.5f - x0
                val s = (cx * yb - cy * xb) / det
                val t = (xa * cy - ya * cx) / det
                if (s < -1e-3f || s > 1.001f || t < -1e-3f || t > 1.001f) continue
                val i = py * w + px
                val d = d0 + s * da + t * db
                if (d <= depth[i]) continue
                depth[i] = d
                ids[i] = id
                color[i] = if (tex == null) flat else {
                    val ti = (s * ni).toInt().coerceIn(0, ni - 1)
                    val tj = (t * nj).toInt().coerceIn(0, nj - 1)
                    finish(tex.at(ti, tj, ni, nj, seed), tone)
                }
            }
        }
    }

    /** One face of the box [x0..x1] x [y0..y1] x [z0..z1]; side faces run A along the ground and B up. */
    private fun face(
        dir: Int, x0: Float, y0: Float, z0: Float, x1: Float, y1: Float, z1: Float,
        mat: Int, id: Int, tex: Texture?, seed: Int,
    ) {
        val nx = VoxelModel.DX[dir]; val ny = VoxelModel.DY[dir]; val nz = VoxelModel.DZ[dir]
        if (!visible(nx, ny, nz)) return
        val t = tone(nx, ny, nz)
        val lx = x1 - x0; val ly = y1 - y0; val lz = z1 - z0
        fun n(v: Float) = max(1, (v * VPC).roundToInt())
        when (dir) {
            0 -> quad(x1, y0, z0, 0f, ly, 0f, 0f, 0f, lz, mat, t, id, tex, n(ly), n(lz), seed)
            1 -> quad(x0, y0, z0, 0f, ly, 0f, 0f, 0f, lz, mat, t, id, tex, n(ly), n(lz), seed)
            2 -> quad(x0, y1, z0, lx, 0f, 0f, 0f, 0f, lz, mat, t, id, tex, n(lx), n(lz), seed)
            3 -> quad(x0, y0, z0, lx, 0f, 0f, 0f, 0f, lz, mat, t, id, tex, n(lx), n(lz), seed)
            else -> quad(x0, y0, z1, lx, 0f, 0f, 0f, ly, 0f, mat, t, id, tex, n(lx), n(ly), seed)
        }
    }

    fun walls(scene: IsoScene) {
        // Walls stand on the floor top, so the baseboard rows of the plank texture sit at floor level.
        val w = scene.width.toFloat(); val d = scene.depth.toFloat(); val top = scene.wallHeight; val bot = 0f
        val base = -(scene.width * scene.depth) - 1
        fun wall(side: WallSide, x0: Float, y0: Float, x1: Float, y1: Float) {
            if (!wallVisible(side, p)) return
            for (dir in 0 until 4) face(dir, x0, y0, bot, x1, y1, top, IsoPalette.WallWood, base - side.ordinal, Textures.plank, 0)
            face(4, x0, y0, bot, x1, y1, top, IsoPalette.WallTop, base - side.ordinal, null, 0)
        }
        wall(WallSide.NORTH, -WALL_T, -WALL_T, w + WALL_T, 0f)
        wall(WallSide.SOUTH, -WALL_T, d, w + WALL_T, d + WALL_T)
        wall(WallSide.WEST, -WALL_T, 0f, 0f, d)
        wall(WallSide.EAST, w, 0f, w + WALL_T, d)
    }

    /** Floor columns. Sides are only drawn where they are exposed, which also saves fill time. */
    fun floor(scene: IsoScene, ghost: IsoObject?, ghostValid: Boolean) {
        val mark = if (ghostValid) AppColors.Success.toArgb() else AppColors.Error.toArgb()
        for (y in 0 until scene.depth) for (x in 0 until scene.width) {
            val cell = scene.cell(x, y)
            if (cell.floor == FloorType.NONE) continue
            val bot = scene.bottom(x, y)
            val id = -((y / scene.tile) * scene.width + x / scene.tile) - 1
            val seed = x * 7919 + y * 104729
            val inGhost = ghost != null && ghostCovers(ghost, x, y)
            tint = if (inGhost) mark else 0
            tintAmount = if (inGhost) 0.45f else 0f
            if (cell.floor == FloorType.ROAD) roadIds += id
            face(4, x.toFloat(), y.toFloat(), bot, x + 1f, y + 1f, cell.height, 0, id, Textures.top(cell.floor), seed)
            for (dir in 0 until 4) {
                val nx = x + VoxelModel.DX[dir]; val ny = y + VoxelModel.DY[dir]
                // A shallower neighbour leaves the lower part of this side open, which steps the island's underside.
                val exposed = !scene.inside(nx, ny) || scene.cell(nx, ny).floor == FloorType.NONE ||
                    scene.cell(nx, ny).height < cell.height || scene.bottom(nx, ny) > bot
                if (exposed) face(dir, x.toFloat(), y.toFloat(), bot, x + 1f, y + 1f, cell.height, 0, id, Textures.side(cell.floor), seed)
            }
        }
        tint = 0; tintAmount = 0f
    }

    /** The cells of [road] this frame can see: a thin road slab each, with its paint on top. */
    fun road(road: RoadRay, scene: IsoScene) {
        val n = roadLength(road, p, w, h)
        for (k in 0 until n) {
            val x = road.x + k * road.dx; val y = road.y + k * road.dy
            if (scene.inside(x, y)) continue
            val id = ROAD_ID - k
            roadIds += id
            val seed = x * 7919 + y * 104729
            face(4, x.toFloat(), y.toFloat(), -road.depth, x + 1f, y + 1f, 0f, 0, id, Textures.top(FloorType.ROAD), seed)
            for (dir in 0 until 4) {
                if (VoxelModel.DX[dir] == road.dx && VoxelModel.DY[dir] == road.dy) continue
                if (VoxelModel.DX[dir] == -road.dx && VoxelModel.DY[dir] == -road.dy && k > 0) continue
                face(dir, x.toFloat(), y.toFloat(), -road.depth, x + 1f, y + 1f, 0f, 0, id, Textures.side(FloorType.ROAD), seed)
            }
            road.paint?.let { model(IsoObject("road", it, x, y), 0f, id) }
        }
    }

    /** Exposed voxel faces of [o], rotated and placed. Emissive voxels keep their glow color at night. */
    fun model(o: IsoObject, baseZ: Float, id: Int) {
        val m = o.shape.model
        val r = o.rot
        val s = o.scale / VPC
        val rdx = IntArray(5); val rdy = IntArray(5); val vis = BooleanArray(5)
        val cols = IntArray(m.day.size * 5)
        for (dir in 0 until 5) {
            var dx = VoxelModel.DX[dir]; var dy = VoxelModel.DY[dir]
            repeat(r) { val t = dx; dx = -dy; dy = t }
            rdx[dir] = dx; rdy[dir] = dy
            val dz = VoxelModel.DZ[dir]
            vis[dir] = visible(dx, dy, dz)
            val t = tone(dx, dy, dz)
            for (k in m.day.indices) cols[k * 5 + dir] = if (night && m.glow[k] != 0) m.glow[k] else finish(m.day[k], t)
        }
        val f = m.faces
        var q = 0
        while (q < f.size) {
            val vx = f[q]; val vy = f[q + 1]; val vz = f[q + 2]; val dir = f[q + 3]; val k = f[q + 4]
            q += 5
            if (!vis[dir]) continue
            val rx = when (r) { 1 -> m.sy - vy - 1; 2 -> m.sx - vx - 1; 3 -> vy; else -> vx }
            val ry = when (r) { 1 -> vx; 2 -> m.sy - vy - 1; 3 -> m.sx - vx - 1; else -> vy }
            val wx = o.x + o.ox + rx * s; val wy = o.y + o.oy + ry * s; val wz = baseZ + vz * s
            val c = cols[k * 5 + dir]
            when {
                dir == 4 -> quad(wx, wy, wz + s, s, 0f, 0f, 0f, s, 0f, c, 1f, id, lit = true)
                rdx[dir] > 0 -> quad(wx + s, wy, wz, 0f, s, 0f, 0f, 0f, s, c, 1f, id, lit = true)
                rdx[dir] < 0 -> quad(wx, wy, wz, 0f, s, 0f, 0f, 0f, s, c, 1f, id, lit = true)
                rdy[dir] > 0 -> quad(wx, wy + s, wz, s, 0f, 0f, 0f, 0f, s, c, 1f, id, lit = true)
                else -> quad(wx, wy, wz, s, 0f, 0f, 0f, 0f, s, c, 1f, id, lit = true)
            }
        }
    }

    /** Outline pass on id and depth edges, then a pixel frame around the selected object. */
    fun finish(selected: Int): IntArray {
        val out = color.copyOf()
        var l = w; var t = h; var rr = -1; var b = -1
        for (y in 0 until h) for (x in 0 until w) {
            val i = y * w + x
            val id = ids[i]
            if (id == 0) continue
            if (id == selected) { l = min(l, x); t = min(t, y); rr = max(rr, x); b = max(b, y) }
            val d = depth[i]
            if (edge(x - 1, y, id, d) || edge(x + 1, y, id, d) || edge(x, y - 1, id, d) || edge(x, y + 1, id, d)) {
                out[i] = shade(color[i], OUTLINE)
            }
        }
        if (rr >= 0) {
            val white = IsoPalette.SelectFrame
            val x0 = max(0, l - 2); val x1 = min(w - 1, rr + 2); val y0 = max(0, t - 2); val y1 = min(h - 1, b + 2)
            for (x in x0..x1) { out[y0 * w + x] = white; out[y1 * w + x] = white }
            for (y in y0..y1) { out[y * w + x0] = white; out[y * w + x1] = white }
        }
        return out
    }

    private fun edge(x: Int, y: Int, id: Int, d: Float): Boolean {
        if (x < 0 || y < 0 || x >= w || y >= h) return true
        val n = y * w + x
        val nid = ids[n]
        if (nid == 0) return true
        return outlines(id, d, nid, depth[n], id in roadIds && nid in roadIds)
    }
}

/**
 * Whether a pixel of [id] at depth [d] gets the dark outline next to a nearer neighbour [nid] at [nd].
 * Road cells (floor, bridge, lane paint) count as one object: seen along the road (low front view) each next cell is nearer, which
 * otherwise drew a dark line across the road at every cell and made it look bumpy.
 */
internal fun outlines(id: Int, d: Float, nid: Int, nd: Float, bothRoad: Boolean = false): Boolean {
    if (nid == id || bothRoad) return nd < d - EDGE_DEPTH
    // Coplanar neighbours (flat floor seen from the top) outline on one side only.
    return nd < d - 1e-3f || (abs(nd - d) <= 1e-3f && nid < id)
}


internal fun argb(r: Float, g: Float, b: Float): Int =
    (0xFF shl 24) or (r.toInt().coerceIn(0, 255) shl 16) or (g.toInt().coerceIn(0, 255) shl 8) or b.toInt().coerceIn(0, 255)

internal fun shade(c: Int, f: Float): Int =
    argb((c shr 16 and 0xFF) * f, (c shr 8 and 0xFF) * f, (c and 0xFF) * f)

private const val NIGHT_R = 0x01.toFloat()
private const val NIGHT_G = 0x10.toFloat()
private const val NIGHT_B = 0x43.toFloat()
private const val NIGHT_AMOUNT = 0.42f // S-12-b roof: day 205,64,52 to night 110,36,50

private fun hash(i: Int, j: Int, seed: Int): Int {
    var x = seed * 73856093 xor i * 19349663 xor j * 83492791
    x *= 0x27d4eb2d
    return (x xor (x ushr 15)) and 0x7FFFFFFF
}

/** Texel patterns for floors and walls, in the sampled illustration palette. See IsoPalette. */
private object Textures {
    private val P = IsoPalette

    val plank = Texture { i, j, _, _, seed ->
        when {
            j < 3 -> if (j == 2 || i % 6 == 0) P.BaseboardDark else P.Baseboard
            j % 4 == 3 -> P.WallSeam
            hash(i / 3, j, seed) % 13 == 0 -> P.WallGrain
            else -> P.WallWood
        }
    }

    private val grass = Texture { i, j, _, _, seed ->
        val h = hash(i, j, seed)
        when { h % 19 == 0 -> P.GrassDark; h % 23 == 1 -> P.GrassLight; else -> P.Grass }
    }
    // One plank tile per cell with dark seams on its edges, like the S-14 floor.
    private val wood = Texture { i, j, _, nj, seed ->
        if (i == 0 || j == 0) P.FloorSeam else if (j == nj / 2 || hash(i / 2, j, seed) % 9 == 0) P.FloorGrain else P.Floor
    }
    private val soil = Texture { _, j, _, _, _ -> if (j % 3 == 1) P.SoilDark else P.Soil }
    private val sand = Texture { i, j, _, _, seed -> if (hash(i, j, seed) % 11 == 0) P.SandDark else P.Sand }
    private val water = Texture { i, j, _, _, seed -> if (hash(i, j, seed) % 17 == 0) P.WaterLight else P.Water }
    private val road = Texture { i, j, _, _, seed -> if (hash(i, j, seed) % 29 == 0) P.RoadLight else P.Road }

    fun top(t: FloorType): Texture = when (t) {
        FloorType.GRASS, FloorType.NONE -> grass
        FloorType.SAND -> sand
        FloorType.SOIL -> soil
        FloorType.WATER -> water
        FloorType.WOOD -> wood
        FloorType.ROAD -> road
    }

    private val woodSide = Texture { _, j, _, _, _ -> if (j % 3 == 0) P.FloorGrain else P.Floor }

    private val sides = FloorType.entries.associateWith { buildSide(it) }

    fun side(t: FloorType): Texture = sides.getValue(t)

    /** Ground block: a band of the surface material, then dirt, then stone, like the island illustration. */
    private fun buildSide(t: FloorType): Texture {
        if (t == FloorType.WOOD) return woodSide
        val surface = when (t) {
            FloorType.SAND -> P.Sand
            FloorType.SOIL -> P.Soil
            FloorType.WATER -> P.Water
            FloorType.ROAD -> P.Road
            else -> P.Grass
        }
        return Texture { i, j, _, nj, seed ->
            // Vines hang from the grass down the dirt and stone, a few columns per side.
            val vine = t == FloorType.GRASS && hash(i, 1, seed) % 7 == 0 && nj - j < 3 + hash(i, 2, seed) % 14
            when {
                vine && j < nj - 2 -> if ((nj - j) % 3 == 0) P.VineLight else P.Vine
                j >= nj - 2 -> if (t == FloorType.GRASS && j == nj - 2 && hash(i, 0, seed) % 3 == 0) P.GrassDark else surface
                j >= nj - 7 -> if (hash(i, j, seed) % 9 == 0) P.DirtDark else P.Dirt
                j % 3 == 0 || (i + (j / 3) * 2) % 5 == 0 -> P.StoneDark
                else -> P.Stone
            }
        }
    }
}

/**
 * Whether floor cell ([x], [y]) is under [o] where it is drawn: [IsoObject.ox], [IsoObject.oy] and [IsoObject.scale]
 * move and size the drawing, so the photo character (cell 0, 0, placed by ox/oy) gets its mark under itself.
 */
internal fun ghostCovers(o: IsoObject, x: Int, y: Int): Boolean {
    if (o.wall != null) return false
    val x0 = o.x + o.ox; val y0 = o.y + o.oy
    val eps = 1e-3f
    return x + 1 > x0 + eps && x < x0 + o.footW * o.scale - eps && y + 1 > y0 + eps && y < y0 + o.footD * o.scale - eps
}
