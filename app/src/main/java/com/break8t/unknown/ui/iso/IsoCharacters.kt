package com.break8t.unknown.ui.iso

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.pow

/** Colors sampled from the encyclopedia art (res/drawable-nodpi/myinfo_ch_*.png); limbs a step darker so they read on the body. */
internal object CharacterPalette {
    val Eye = Color(0xFF2F1408)
    val Ink = Color(0xFF3A1907)
    val Spark = Color(0xFFFFFBF0)
    val Mouth = Color(0xFFE8735E)

    val SproutBody = Color(0xFFC9D734)
    val SproutLimb = Color(0xFFADBF24)
    val SproutDark = Color(0xFF94A818)
    val SproutLight = Color(0xFFDDE66A)
    val SproutCream = Color(0xFFFCF1A5)
    val SproutFeet = Color(0xFFFCEB9C)

    val CatOrange = Color(0xFFFDB102)
    val CatLimb = Color(0xFFEE9A00)
    val CatCream = Color(0xFFFDE8AC)
    val CatStripe = Color(0xFFD86B01)
    val CatCheek = Color(0xFFF59A6B)
    val CatHat = Color(0xFFBA683D)
    val CatHatDark = Color(0xFF8E4F33)
    val CatScarf = Color(0xFFC65C29)

    val MushCap = Color(0xFFE82810)
    val MushCapDark = Color(0xFFBF1C09)
    val MushSpot = Color(0xFFFDECC1)
    val MushBody = Color(0xFFFCE9BD)
    val MushLimb = Color(0xFFF3CF98)
    val MushShade = Color(0xFFE9B97E)
    val MushCheek = Color(0xFFFC9E7B)

    val CloudWhite = Color(0xFFFDF4E8)
    val CloudPuff = Color(0xFFF4E6D2)
    val CloudShade = Color(0xFFF0D8B8)
    val CloudShoe = Color(0xFF81A4D4)
    val CloudShoeDark = Color(0xFF5F86BE)
    val CloudCheek = Color(0xFFF8B98B)

    val FlyYellow = Color(0xFFFDB901)
    val FlyLight = Color(0xFFFDDF3C)
    val FlyCream = Color(0xFFFDE9A1)
    val FlyWing = Color(0xFFF6E3B0)
    val FlyOrange = Color(0xFFF09300)
    val FlyOrangeDark = Color(0xFFD77701)
    val FlyGlow = Color(0xFFFFF5AE)
}

/**
 * Each character in each pose as its own voxel model: 16 x 16 voxels on a 2 x 2 cell footprint, so draw it
 * at half scale. Every model is authored in the orientation the pose shows (side faces +x, back faces -y).
 */
internal object IsoCharacters {
    val ids = listOf("ch_sprout", "ch_cat", "ch_mush", "ch_cloud", "ch_firefly")
    val poses = listOf(null, "pose_wave", "pose_jump", "pose_sit", "pose_side", "pose_back")

    fun of(id: String, pose: String?): IsoShape? = cache[id to pose.takeIf { it in poses }]

    private val cache: Map<Pair<String, String?>, IsoShape> by lazy {
        ids.flatMap { id -> poses.map { id to it } }.associateWith { (id, p) -> build(id, Pose.entries[poses.indexOf(p)]) }
    }

    private fun build(id: String, p: Pose): IsoShape = when (id) {
        "ch_cat" -> cat(p)
        "ch_mush" -> mush(p)
        "ch_cloud" -> cloud(p)
        "ch_firefly" -> firefly(p)
        else -> sprout(p)
    }

    private val C = CharacterPalette
    private val eye = Mat(C.Eye)
    private val spark = Mat(C.Spark)
    private val mouth = Mat(C.Mouth)

    /** Eyes at [xl] and the mirrored column, two voxels wide: round with a highlight, or closed happy arcs. */
    private fun Vox.eyes(xl: Int, z: Int, happy: Boolean) {
        val xr = Vox.N - xl - 2
        if (happy) for (x0 in intArrayOf(xl - 1, xr)) {
            mark(x0, z, eye); mark(x0 + 1, z + 1, eye); mark(x0 + 2, z, eye)
        } else for (x0 in intArrayOf(xl, xr)) {
            mark(x0, z, eye); mark(x0 + 1, z, eye); mark(x0 + 1, z + 1, eye); mark(x0, z + 1, spark)
        }
    }

    private fun Vox.mouth(z: Int, open: Boolean) {
        front(7, z, mouth); front(8, z, mouth)
        if (open) { front(7, z - 1, mouth); front(8, z - 1, mouth) }
    }

    // 새싹이: a round green bud with a cream belly, stubby arms and a two-leaf sprout on top.
    private fun sprout(p: Pose): IsoShape = Vox().run {
        val body = Mat(C.SproutBody); val limb = Mat(C.SproutLimb); val dark = Mat(C.SproutDark)
        val cream = Mat(C.SproutCream); val feet = Mat(C.SproutFeet)
        val sit = p == Pose.SIT
        val b = if (sit) 0 else 1
        when (p) {
            Pose.SIT -> { box(4, 10, 0, 3, 5, 2, feet); box(9, 10, 0, 3, 5, 2, feet) }
            Pose.SIDE -> { box(4, 10, 0, 3, 4, 2, feet); box(9, 3, 0, 3, 4, 2, feet) }
            Pose.JUMP -> { box(2, 7, 0, 3, 3, 2, feet); box(11, 7, 0, 3, 3, 2, feet) }
            else -> { box(4, 7, 0, 3, 3, 2, feet); box(9, 7, 0, 3, 3, 2, feet) }
        }
        if (sit) ball(8f, 8f, 6.4f, 6.6f, 5.4f, 6f, body, e = 2.6f) else ball(8f, 8f, 7.5f, 6f, 5f, 6.5f, body, e = 2.6f)
        frontOval(8f, b + 3.5f, 3f, 2.2f, cream)
        for (x in 5..8) for (y in 6..9) if ((x - 6.5f) * (x - 6.5f) + (y - 7.5f) * (y - 7.5f) < 4f) top(x, y, Mat(C.SproutLight))
        val ez = b + 9
        eyes(5, ez, p == Pose.JUMP)
        mouth(ez - 2, p == Pose.WAVE || p == Pose.JUMP)
        for (x in intArrayOf(3, 4, 11, 12)) front(x, ez - 2, cream)
        when (p) {
            Pose.WAVE -> { box(1, 7, b + 4, 2, 3, 3, limb); box(13, 7, b + 7, 2, 2, 2, limb); box(14, 7, b + 9, 2, 2, 7, limb) }
            Pose.JUMP -> { box(1, 7, b + 8, 2, 2, 2, limb); box(0, 7, b + 10, 2, 2, 5, limb); box(13, 7, b + 8, 2, 2, 2, limb); box(14, 7, b + 10, 2, 2, 5, limb) }
            Pose.SIT -> { box(0, 9, 1, 3, 3, 3, limb); box(13, 9, 1, 3, 3, 3, limb) }
            Pose.SIDE -> { box(1, 10, b + 4, 2, 3, 3, limb); box(13, 3, b + 4, 2, 3, 3, limb) }
            Pose.BACK -> { box(1, 4, b + 4, 2, 3, 2, limb); box(13, 4, b + 4, 2, 3, 2, limb); box(3, 2, b + 3, 10, 2, 2, limb) }
            Pose.STAND -> { box(1, 7, b + 4, 2, 3, 3, limb); box(13, 7, b + 4, 2, 3, 3, limb) }
        }
        val top = b + 13
        box(7, 7, top - 1, 2, 2, 3, dark)
        val lean = when (p) { Pose.JUMP -> 0.7f; Pose.SIT -> -0.1f; else -> 0.25f }
        ball(4.4f, 8f, top + 2.2f, 3.8f, 2.6f, 1.2f, body, lean = -lean)
        ball(11.6f, 8f, top + 2.2f + if (p == Pose.WAVE) 1f else 0f, 3.8f, 2.6f, 1.2f, body, lean = lean)
        for (x in intArrayOf(2, 3, 4, 5, 10, 11, 12, 13)) top(x, 8, dark)
        shape(turns(p), if (p == Pose.JUMP) 4 else 0)
    }

    // 냥이: orange tabby in a brown cap and rust scarf; its striped tail and ears move with the pose.
    private fun cat(p: Pose): IsoShape = Vox().run {
        val fur = Mat(C.CatOrange); val limb = Mat(C.CatLimb); val cream = Mat(C.CatCream); val stripe = Mat(C.CatStripe)
        val hat = Mat(C.CatHat); val hatDark = Mat(C.CatHatDark); val scarf = Mat(C.CatScarf); val pink = Mat(C.CatCheek)
        val b = if (p == Pose.SIT) -1 else 0
        when (p) {
            Pose.SIT -> {
                ball(4.5f, 7.5f, 1.8f, 2.2f, 3.4f, 2.2f, limb); ball(11.5f, 7.5f, 1.8f, 2.2f, 3.4f, 2.2f, limb)
                box(5, 10, 0, 2, 2, 5, limb); box(9, 10, 0, 2, 2, 5, limb); box(5, 11, 0, 2, 1, 1, cream); box(9, 11, 0, 2, 1, 1, cream)
            }
            Pose.SIDE -> { box(5, 9, 0, 2, 3, 3, limb); box(9, 4, 0, 2, 3, 3, limb); box(5, 11, 0, 2, 1, 1, cream); box(9, 6, 0, 2, 1, 1, cream) }
            Pose.JUMP -> { box(4, 7, 0, 2, 3, 3, limb); box(10, 7, 0, 2, 3, 3, limb); box(3, 7, 0, 1, 3, 1, cream); box(12, 7, 0, 1, 3, 1, cream) }
            else -> { box(5, 7, 0, 2, 3, 3, limb); box(9, 7, 0, 2, 3, 3, limb); box(5, 9, 0, 2, 1, 1, cream); box(9, 9, 0, 2, 1, 1, cream) }
        }
        ball(8f, 8f, b + 5.5f, 4f, 3.2f, 3.6f, fur, e = 2.6f)
        frontOval(8f, b + 5f, 1.8f, 2f, cream)
        for (x in 5..10) { back(x, b + 4, stripe); back(x, b + 6, stripe) }
        ball(8f, 8f, b + 8.6f, 4.6f, 3.8f, 1.1f, scarf)
        ball(8f, 8.5f, b + 12.5f, 6f, 4.8f, 4.2f, fur, e = 2.6f)
        frontOval(8f, b + 11f, 2.2f, 1.3f, cream)
        front(7, b + 12, Mat(C.Ink)); front(8, b + 12, Mat(C.Ink))
        if (p == Pose.WAVE || p == Pose.JUMP) { front(7, b + 10, mouth); front(8, b + 10, mouth) }
        eyes(4, b + 13, p == Pose.JUMP)
        front(3, b + 11, pink); front(12, b + 11, pink)
        for (y in 6..11) for (z in intArrayOf(b + 11, b + 13)) { left(y, z, stripe); right(y, z, stripe) }
        for (x in 5..10) { back(x, b + 12, stripe); back(x, b + 14, stripe) }
        // A flat cap with a short brim; the ears come out on either side of it.
        ball(8f, 8.5f, b + 16.2f, 4.6f, 4.4f, 2.2f, hat, zMin = b + 16, e = 2.4f)
        box(5, 12, b + 16, 6, 1, 1, hatDark); box(7, 8, b + 18, 2, 2, 1, hatDark)
        val earUp = if (p == Pose.JUMP) 1 else 0
        for (x0 in intArrayOf(2, 11)) {
            box(x0, 8, b + 15, 3, 2, 3 + earUp, fur); box(if (x0 == 2) 2 else 12, 8, b + 18 + earUp, 2, 2, 1, fur)
            box(if (x0 == 2) 3 else 12, 9, b + 16, 1, 1, 2, pink)
        }
        // The scarf tail hangs at the front; from behind the knot shows instead.
        if (p == Pose.BACK) box(7, 3, b + 6, 2, 1, 3, scarf) else { box(9, 11, b + 5, 2, 1, 3, scarf); box(9, 11, b + 6, 2, 1, 1, cream) }
        when (p) {
            Pose.WAVE -> { box(2, 7, b + 4, 2, 2, 4, limb); box(2, 7, b + 4, 2, 2, 1, cream); box(12, 7, b + 6, 2, 2, 2, limb); box(14, 7, b + 7, 2, 2, 8, limb); box(14, 7, b + 14, 2, 2, 1, cream) }
            Pose.JUMP -> { box(2, 7, b + 7, 2, 2, 2, limb); box(0, 7, b + 9, 2, 2, 5, limb); box(14, 7, b + 7, 2, 2, 2, limb); box(14, 7, b + 9, 2, 2, 5, limb) }
            Pose.SIT -> { box(3, 9, b + 4, 2, 2, 3, limb); box(11, 9, b + 4, 2, 2, 3, limb) }
            Pose.SIDE -> { box(2, 9, b + 4, 2, 2, 4, limb); box(2, 9, b + 4, 2, 2, 1, cream); box(12, 5, b + 4, 2, 2, 4, limb) }
            Pose.BACK -> { box(2, 6, b + 5, 2, 2, 3, limb); box(12, 6, b + 5, 2, 2, 3, limb); box(4, 3, b + 4, 8, 2, 2, limb); box(6, 3, b + 4, 4, 1, 1, cream) }
            Pose.STAND -> { box(2, 7, b + 4, 2, 2, 4, limb); box(2, 7, b + 4, 2, 2, 1, cream); box(12, 7, b + 4, 2, 2, 4, limb); box(12, 7, b + 4, 2, 2, 1, cream) }
        }
        fun tail(cells: List<Triple<Int, Int, Int>>) = cells.forEachIndexed { i, (x, y, z) -> box(x, y, z, 2, 2, 1, if (i % 2 == 0) fur else stripe) }
        when (p) {
            Pose.SIT -> tail((0..5).map { Triple(12, 9 - it, 0) }.reversed() + (0..4).map { Triple(11 - 2 * it, 12, 0) })
            Pose.SIDE -> tail((0..3).map { Triple(7, 3 - it, 3) } + (0..5).map { Triple(7, 0, 4 + it) })
            Pose.JUMP -> tail((0..9).map { Triple(7, 2, 3 + it) })
            Pose.BACK -> tail((0..1).map { Triple(7, 3 - it, 2) } + (0..7).map { Triple(7, 1, 3 + it) } + listOf(Triple(8, 1, 11), Triple(9, 1, 11)))
            Pose.WAVE -> tail((0..1).map { Triple(11, 3 - it, 2) } + (0..7).map { Triple(13, 2, 2 + it) } + listOf(Triple(14, 2, 10), Triple(14, 3, 11)))
            Pose.STAND -> tail((0..1).map { Triple(11, 3 - it, 2) } + (0..5).map { Triple(13, 2, 2 + it) } + listOf(Triple(14, 2, 8)))
        }
        shape(turns(p), if (p == Pose.JUMP) 4 else 0)
    }

    // 버섯이: a cream stalk body under a big spotted red cap that tilts with the pose.
    private fun mush(p: Pose): IsoShape = Vox().run {
        val body = Mat(C.MushBody); val limb = Mat(C.MushLimb); val shade = Mat(C.MushShade)
        val cap = Mat(C.MushCap); val capDark = Mat(C.MushCapDark); val spot = Mat(C.MushSpot)
        when (p) {
            Pose.SIT -> { box(5, 11, 0, 2, 3, 2, shade); box(9, 11, 0, 2, 3, 2, shade) }
            Pose.SIDE -> { box(5, 10, 0, 2, 3, 1, shade); box(9, 3, 0, 2, 3, 1, shade) }
            Pose.JUMP -> { box(3, 7, 0, 2, 2, 2, shade); box(11, 7, 0, 2, 2, 2, shade) }
            else -> { box(5, 7, 0, 2, 3, 1, shade); box(9, 7, 0, 2, 3, 1, shade) }
        }
        val sit = p == Pose.SIT
        if (sit) ball(8f, 8f, 4f, 5.2f, 4.6f, 4f, body, e = 2.6f) else ball(8f, 8f, 5f, 4.8f, 4.4f, 5f, body, e = 2.6f)
        val fz = if (sit) 4 else 5
        eyes(5, fz, p == Pose.JUMP)
        if (p == Pose.WAVE || p == Pose.JUMP) mouth(fz - 1, true) else { front(6, fz - 1, eye); front(7, fz - 2, eye); front(8, fz - 2, eye); front(9, fz - 1, eye) }
        for (x in intArrayOf(3, 4, 11, 12)) front(x, fz - 1, Mat(C.MushCheek))
        when (p) {
            Pose.WAVE -> { box(2, 7, fz - 2, 2, 2, 2, limb); box(12, 7, fz - 1, 2, 2, 2, limb); box(14, 7, fz, 2, 2, 5, limb) }
            Pose.JUMP -> { box(1, 7, fz, 3, 2, 2, limb); box(0, 7, fz + 1, 1, 2, 2, limb); box(12, 7, fz, 3, 2, 2, limb); box(15, 7, fz + 1, 1, 2, 2, limb) }
            Pose.SIT -> { box(1, 8, 0, 3, 3, 2, limb); box(12, 8, 0, 3, 3, 2, limb) }
            Pose.SIDE -> { box(2, 9, fz - 2, 2, 3, 2, limb); box(12, 5, fz - 2, 2, 3, 2, limb) }
            Pose.BACK -> { box(2, 6, fz - 2, 2, 2, 2, limb); box(12, 6, fz - 2, 2, 2, 2, limb); box(4, 3, fz - 2, 8, 2, 2, limb) }
            Pose.STAND -> { box(2, 7, fz - 2, 2, 2, 2, limb); box(12, 7, fz - 2, 2, 2, 2, limb) }
        }
        val capZ = if (sit) 11f else 12.5f
        val lean = when (p) { Pose.WAVE -> 0.35f; Pose.JUMP -> -0.2f; else -> 0f }
        val nod = when (p) { Pose.SIT -> -0.2f; Pose.SIDE -> -0.25f; Pose.JUMP -> 0.25f; Pose.BACK -> -0.2f; else -> 0f }
        val rim = capZ.toInt() - 4
        ball(8f, 8f, capZ, 7.4f, 6.8f, 5.2f, cap, lean, nod, zMin = rim, e = 2.2f)
        tint(0, 0, rim, 16, 16, 1, capDark, only = cap)
        tint(0, 0, rim + 1, 16, 16, 1, capDark, only = cap)
        for ((x, y) in listOf(7 to 7, 8 to 7, 7 to 8, 8 to 8, 6 to 7, 9 to 8, 7 to 6, 8 to 9, 3 to 5, 4 to 5, 3 to 6, 12 to 10, 11 to 10, 12 to 9, 5 to 11, 10 to 4)) top(x, y, spot)
        for ((a, z) in listOf(3 to 0, 4 to 0, 3 to 1, 4 to 1, 11 to 1, 12 to 1, 11 to 2, 7 to 2, 8 to 2)) {
            front(a, rim + 2 + z, spot); back(15 - a, rim + 2 + z, spot); left(15 - a, rim + 2 + z, spot); right(a, rim + 2 + z, spot)
        }
        shape(turns(p), if (p == Pose.JUMP) 4 else 0)
    }

    // 구름이: a puffy cloud in blue shoes; its arms are small cloud puffs, its eyes always closed.
    private fun cloud(p: Pose): IsoShape = Vox().run {
        val white = Mat(C.CloudWhite); val puff = Mat(C.CloudPuff); val shade = Mat(C.CloudShade)
        val shoe = Mat(C.CloudShoe); val shoeDark = Mat(C.CloudShoeDark)
        val sit = p == Pose.SIT
        fun shoes(xs: IntArray, ys: IntArray) = xs.forEachIndexed { i, x -> box(x, ys[i], 0, 3, 4, 2, shoe); box(x, ys[i], 0, 3, 4, 1, shoeDark) }
        when (p) {
            Pose.SIT -> shoes(intArrayOf(4, 9), intArrayOf(11, 11))
            Pose.SIDE -> shoes(intArrayOf(4, 9), intArrayOf(10, 3))
            Pose.JUMP -> shoes(intArrayOf(2, 11), intArrayOf(7, 7))
            else -> shoes(intArrayOf(4, 9), intArrayOf(7, 7))
        }
        val b = if (sit) 1 else 3
        if (!sit) { box(5, 8, 2, 2, 2, 2, shade); box(9, 8, 2, 2, 2, 2, shade) }
        if (p == Pose.JUMP) { box(3, 8, 2, 2, 2, 2, shade); box(11, 8, 2, 2, 2, 2, shade) }
        ball(8f, 8f, b + 4.6f, if (sit) 6.8f else 6.2f, 5f, if (sit) 4f else 4.6f, white, e = 2.4f)
        ball(6.8f, 8f, b + 8.6f, 3.6f, 3.6f, 3f, white)
        ball(10.4f, 8.4f, b + 8f, 3f, 3f, 2.5f, white)
        tint(0, 0, b, 16, 16, 1, shade)
        if (p == Pose.BACK) { ball(5f, 3.2f, b + 5f, 2.4f, 1.6f, 2f, white); ball(11f, 3.2f, b + 6f, 2.2f, 1.6f, 2f, white) }
        val ez = b + 5
        val arc = if (p == Pose.JUMP) intArrayOf(0, 1, 1, 0) else intArrayOf(1, 0, 0, 1)
        for (x0 in intArrayOf(3, 9)) for (i in 0..3) mark(x0 + i, ez + arc[i], eye)
        mouth(ez - 2, p == Pose.WAVE || p == Pose.JUMP)
        for (x in intArrayOf(2, 3, 12, 13)) front(x, ez - 1, Mat(C.CloudCheek))
        fun arm(x: Float, y: Float, z: Float, r: Float = 1.8f) = ball(x, y, z, r, r + 0.2f, r, puff)
        when (p) {
            Pose.WAVE -> { arm(1.6f, 8f, b + 2.5f); arm(14f, 8f, b + 7f, 1.3f); arm(14.2f, 8f, b + 10f) }
            Pose.JUMP -> { arm(1.6f, 8f, b + 7.5f); arm(14.4f, 8f, b + 7.5f); arm(0.9f, 8f, b + 10f, 1.2f); arm(15.1f, 8f, b + 10f, 1.2f) }
            Pose.SIT -> { arm(1.4f, 9f, 1.6f); arm(14.6f, 9f, 1.6f) }
            Pose.SIDE -> { arm(1.6f, 11f, b + 2.5f); arm(14.4f, 4.5f, b + 2.5f) }
            Pose.BACK -> { arm(1.6f, 7f, b + 2.5f); arm(14.4f, 7f, b + 2.5f) }
            Pose.STAND -> { arm(1.6f, 8f, b + 2.5f); arm(14.4f, 8f, b + 2.5f) }
        }
        shape(turns(p), if (p == Pose.JUMP) 4 else 0)
    }

    // 반딧이: yellow head with antennae, cream collar and wings, an orange abdomen that glows at night.
    private fun firefly(p: Pose): IsoShape = Vox().run {
        val yellow = Mat(C.FlyYellow); val cream = Mat(C.FlyCream); val wingM = Mat(C.FlyWing)
        val orange = Mat(C.FlyOrange); val dark = Mat(C.FlyOrangeDark); val ink = Mat(C.Ink)
        val glow = Mat(C.FlyGlow, IsoPalette.Light); val tip = Mat(C.FlyLight, IsoPalette.Light)
        when (p) {
            Pose.SIT -> { box(6, 10, 0, 1, 3, 1, dark); box(9, 10, 0, 1, 3, 1, dark) }
            Pose.SIDE -> { box(6, 9, 0, 1, 2, 2, dark); box(9, 5, 0, 1, 2, 2, dark) }
            Pose.JUMP -> { box(5, 7, 0, 1, 2, 2, dark); box(10, 7, 0, 1, 2, 2, dark); box(4, 7, 0, 1, 1, 1, dark); box(11, 7, 0, 1, 1, 1, dark) }
            else -> { box(6, 7, 0, 1, 2, 2, dark); box(9, 7, 0, 1, 2, 2, dark) }
        }
        val b = if (p == Pose.SIT) -1 else 0
        ball(8f, 8f, b + 4.2f, 3.6f, 3.2f, 3.4f, orange, e = 2.4f)
        for (x in 5..10) { back(x, b + 4, dark); front(x, b + 4, dark) }
        for (x in 5..10) for (z in b + 1..b + 3) { front(x, z, glow); if (p == Pose.BACK || p == Pose.JUMP) back(x, z, glow) }
        ball(8f, 8f, b + 7.6f, 4.6f, 4f, 1.2f, cream)
        ball(8f, 8f, b + 12f, 5.2f, 4.6f, 4.2f, yellow, e = 2.6f)
        for (x in 5..8) for (y in 7..9) top(x, y, Mat(C.FlyLight))
        val ez = b + 11
        eyes(4, ez, p == Pose.JUMP)
        mouth(ez - 1, p == Pose.WAVE || p == Pose.JUMP)
        for (x in intArrayOf(3, 12)) front(x, ez - 1, cream)
        // Antennae: two ink stalks with glowing tips; the waving side bends outward.
        val top = b + 16
        for ((x, out) in listOf(6 to -1, 9 to 1)) {
            val bend = if (p == Pose.WAVE && out > 0 || p == Pose.JUMP) 1 else 0
            box(x, 8, top, 1, 1, 1, ink); box(x + out, 8, top + 1, 1, 1, 2, ink)
            box(if (out < 0) x - 2 - bend else x + 1 + bend, 7, top + 3 - bend, 2, 2, 2, tip)
        }
        fun wing(x: Float, y: Float, z: Float, rx: Float, ry: Float, rz: Float, lean: Float = 0f) = ball(x, y, z, rx, ry, rz, wingM, lean = lean)
        when (p) {
            Pose.JUMP -> { wing(2.4f, 5.4f, b + 12f, 2.8f, 0.9f, 3.4f, -0.6f); wing(13.6f, 5.4f, b + 12f, 2.8f, 0.9f, 3.4f, 0.6f) }
            Pose.SIT -> { wing(4f, 4f, b + 5f, 1.8f, 0.9f, 3.2f); wing(12f, 4f, b + 5f, 1.8f, 0.9f, 3.2f) }
            Pose.SIDE -> { wing(3.4f, 3f, b + 9f, 0.9f, 3f, 3f); wing(12.6f, 3f, b + 9f, 0.9f, 3f, 3f) }
            Pose.BACK -> { wing(2.4f, 3.4f, b + 9f, 3f, 0.9f, 3.6f, -0.3f); wing(13.6f, 3.4f, b + 9f, 3f, 0.9f, 3.6f, 0.3f) }
            else -> { wing(1.8f, 6.4f, b + 9f, 2.8f, 0.9f, 3.2f, -0.3f); wing(14.2f, 6.4f, b + 9f, 2.8f, 0.9f, 3.2f, 0.3f) }
        }
        when (p) {
            Pose.WAVE -> { box(3, 8, b + 5, 2, 2, 2, orange); box(12, 8, b + 7, 2, 2, 2, orange); box(13, 8, b + 9, 2, 2, 5, orange) }
            Pose.JUMP -> { box(3, 8, b + 7, 2, 2, 2, orange); box(2, 8, b + 9, 2, 2, 2, orange); box(11, 8, b + 7, 2, 2, 2, orange); box(12, 8, b + 9, 2, 2, 2, orange) }
            Pose.SIT -> { box(3, 9, b + 4, 2, 2, 2, orange); box(11, 9, b + 4, 2, 2, 2, orange) }
            Pose.SIDE -> { box(3, 9, b + 5, 2, 2, 2, orange); box(11, 5, b + 5, 2, 2, 2, orange) }
            Pose.BACK -> { box(3, 6, b + 4, 2, 2, 2, orange); box(11, 6, b + 4, 2, 2, 2, orange) }
            Pose.STAND -> { box(3, 8, b + 5, 2, 2, 2, orange); box(11, 8, b + 5, 2, 2, 2, orange) }
        }
        shape(turns(p), if (p == Pose.JUMP) 5 else 0)
    }

    /** Side shows the figure walking toward +x, its left flank to the camera; back turns it away. */
    private fun turns(p: Pose) = when (p) { Pose.SIDE -> 3; Pose.BACK -> 2; else -> 0 }
}

private enum class Pose { STAND, WAVE, JUMP, SIT, SIDE, BACK }

private data class Mat(val color: Color, val glow: Color? = null)

/** A 16 x 16 x 32 voxel canvas: solids, rounded blobs, and recoloring the first surface seen from one side. */
private class Vox {
    private val g = arrayOfNulls<Mat>(N * N * H)

    operator fun get(x: Int, y: Int, z: Int): Mat? = if (x in 0 until N && y in 0 until N && z in 0 until H) g[x + N * (y + N * z)] else null
    fun set(x: Int, y: Int, z: Int, m: Mat?) { if (x in 0 until N && y in 0 until N && z in 0 until H) g[x + N * (y + N * z)] = m }

    fun box(x: Int, y: Int, z: Int, w: Int, d: Int, h: Int, m: Mat?) {
        for (k in z until z + h) for (j in y until y + d) for (i in x until x + w) set(i, j, k, m)
    }

    /**
     * Superellipsoid through voxel centres: [e] 2 is round, larger is boxier with flatter faces (cleaner shading).
     * [lean] and [nod] tip it by that much z per voxel of x and of y.
     */
    fun ball(
        cx: Float, cy: Float, cz: Float, rx: Float, ry: Float, rz: Float, m: Mat?,
        lean: Float = 0f, nod: Float = 0f, zMin: Int = 0, e: Float = 2f,
    ) {
        for (z in maxOf(0, zMin) until H) for (y in 0 until N) for (x in 0 until N) {
            val dx = abs((x + .5f - cx) / rx)
            val dy = abs((y + .5f - cy) / ry)
            val dz = abs((z + .5f - cz - lean * (x + .5f - cx) - nod * (y + .5f - cy)) / rz)
            if (dx.pow(e) + dy.pow(e) + dz.pow(e) <= 1f) set(x, y, z, m)
        }
    }

    /** Recolors the filled voxels inside the box, or only those of [only]. */
    fun tint(x: Int, y: Int, z: Int, w: Int, d: Int, h: Int, m: Mat, only: Mat? = null) {
        for (k in z until z + h) for (j in y until y + d) for (i in x until x + w) {
            val v = get(i, j, k) ?: continue
            if (only == null || v == only) set(i, j, k, m)
        }
    }

    fun front(x: Int, z: Int, m: Mat) { for (y in N - 1 downTo 0) if (get(x, y, z) != null) { set(x, y, z, m); return } }

    /** Paints the front like [front] and closes any open side beside it, so an eye never shows from the side or back. */
    fun mark(x: Int, z: Int, m: Mat) {
        val y = (N - 1 downTo 0).firstOrNull { get(x, it, z) != null } ?: return
        set(x, y, z, m)
        for (nx in intArrayOf(x - 1, x + 1)) if (nx in 0 until N && get(nx, y, z) == null) set(nx, y, z, get(x, y - 1, z) ?: m)
        if (get(x, y, z + 1) == null) set(x, y, z + 1, get(x, y - 1, z + 1) ?: get(x, y - 1, z) ?: m)
    }
    fun back(x: Int, z: Int, m: Mat) { for (y in 0 until N) if (get(x, y, z) != null) { set(x, y, z, m); return } }
    fun left(y: Int, z: Int, m: Mat) { for (x in 0 until N) if (get(x, y, z) != null) { set(x, y, z, m); return } }
    fun right(y: Int, z: Int, m: Mat) { for (x in N - 1 downTo 0) if (get(x, y, z) != null) { set(x, y, z, m); return } }
    fun top(x: Int, y: Int, m: Mat) { for (z in H - 1 downTo 0) if (get(x, y, z) != null) { set(x, y, z, m); return } }

    fun frontOval(cx: Float, cz: Float, rx: Float, rz: Float, m: Mat) {
        for (z in 0 until H) for (x in 0 until N) {
            val dx = (x + .5f - cx) / rx
            val dz = (z + .5f - cz) / rz
            if (dx * dx + dz * dz <= 1f) front(x, z, m)
        }
    }

    /** Turns the canvas [turns] quarter turns the way [Cuboid.rotated] does, raises it [lift], and packs x runs. */
    fun shape(turns: Int, lift: Int): IsoShape {
        val out = arrayOfNulls<Mat>(N * N * H)
        for (z in 0 until H) for (y in 0 until N) for (x in 0 until N) {
            val m = get(x, y, z) ?: continue
            val (rx, ry) = when (turns.mod(4)) { 1 -> N - 1 - y to x; 2 -> N - 1 - x to N - 1 - y; 3 -> y to N - 1 - x; else -> x to y }
            require(z + lift < H) { "lift pushes the model out of the canvas" }
            out[rx + N * (ry + N * (z + lift))] = m
        }
        val parts = ArrayList<Cuboid>()
        for (z in 0 until H) for (y in 0 until N) {
            var x = 0
            while (x < N) {
                val m = out[x + N * (y + N * z)]
                var e = x + 1
                while (e < N && out[e + N * (y + N * z)] == m) e++
                if (m != null) parts += Cuboid(x, y, z, e - x, 1, 1, m.color, m.glow)
                x = e
            }
        }
        return IsoShape(N / VPC, N / VPC, parts)
    }

    companion object {
        const val N = 16
        const val H = 32
    }
}
