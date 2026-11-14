package com.break8t.unknown.ui.iso

import androidx.compose.ui.graphics.Color
import com.break8t.unknown.ui.theme.AppColors

/**
 * Scene palette sampled from the Figma illustrations (cam_rot, room_rot, item and character art).
 * Ints are ARGB for the rasterizer's floor and wall textures; Colors are for voxel shapes.
 */
object IsoPalette {
    const val Grass = 0xFFA0C740.toInt()
    const val GrassDark = 0xFF7CA938.toInt()
    const val GrassLight = 0xFFB0CD5A.toInt()
    const val Dirt = 0xFFAF5D37.toInt()
    const val DirtDark = 0xFF8A4A2C.toInt()
    const val Stone = 0xFF8F8B8C.toInt()
    const val StoneDark = 0xFF707067.toInt()
    const val Sand = 0xFFE9D39A.toInt()
    const val SandDark = 0xFFD4B979.toInt()
    const val Soil = 0xFF88512E.toInt()
    const val SoilDark = 0xFF5E3820.toInt()
    const val Water = 0xFF34A1E2.toInt()
    const val WaterLight = 0xFF8FD0F5.toInt()
    const val Road = 0xFF6B6A70.toInt()
    const val RoadLight = 0xFF8A8990.toInt()
    const val Vine = 0xFF4E8A2E.toInt()
    const val VineLight = 0xFF7DB443.toInt()
    const val Floor = 0xFFC6743C.toInt()
    const val FloorGrain = 0xFFB86A36.toInt()
    const val FloorSeam = 0xFF8A4A26.toInt()
    const val WallWood = 0xFFDC8239.toInt()
    const val WallGrain = 0xFFC9733A.toInt()
    const val WallSeam = 0xFFA35B34.toInt()
    const val WallTop = 0xFFE68E41.toInt()
    const val Baseboard = 0xFFA6A29C.toInt() // grey stone blocks of S-14
    const val BaseboardDark = 0xFF6F6B66.toInt()
    const val SelectFrame = 0xFFFFFAF0.toInt() // AppColors.Card
    const val GhostOk = 0xFFFFFFFF.toInt()

    /** Behind the scene at night; the scene itself darkens toward this color. */
    val Night = Color(0xFF011043) // Figma S-12-b sky

    internal val T = Color.Transparent
    internal val Ink = AppColors.Text
    internal val Cream = Color(0xFFF4E3C8)
    internal val WoodLight = Color(0xFFD08750)
    internal val Wood = Color(0xFFC07742)
    internal val WoodDark = Color(0xFF8A4F2A)
    internal val WoodDeep = Color(0xFF5D3727)
    internal val Trunk = Color(0xFFAC5D31)
    internal val TrunkDark = Color(0xFF7A3F22)
    internal val GlassLight = Color(0xFFC8E4F6)
    internal val RoadKerb = Color(0xFFB4B2B6)
    internal val GrassC = Color(Grass)
    internal val DirtC = Color(Dirt)
    internal val WindowWarm = Color(0xFFF6C453)
    internal val Leaf = Color(0xFF578730)
    internal val LeafLight = Color(0xFF9EC540)
    internal val LeafDark = Color(0xFF39652B)
    internal val Sofa = Color(0xFF29A343)
    internal val SofaDark = Color(0xFF047344)
    internal val SofaLight = Color(0xFF67B43D)
    internal val PlantLeaf = Color(0xFF32872A)
    internal val PlantLight = Color(0xFF72B922)
    internal val Pot = Color(0xFFAF4429)
    internal val PotRim = Color(0xFFE1743F)
    internal val Roof = Color(0xFFD2473B)
    internal val RoofDark = Color(0xFFBC3A34)
    internal val StoneC = Color(Stone)
    internal val StoneLight = Color(0xFFB9B6A2)
    internal val Glass = Color(0xFF8EC1E9)
    internal val Light = Color(0xFFFFD873)
    internal val Shade = Color(0xFFFFE7A8)
    internal val SoilC = Color(Soil)
    internal val SoilDarkC = Color(SoilDark)
    internal val Sprout = Color(0xFF9BD46B)
    internal val Carrot = Color(0xFFF08A24)
    internal val Purple = Color(0xFF7A3E8E)
    internal val WaterC = Color(Water)
    internal val Tire = Color(0xFF3A3532)
    internal val Red = Color(0xFFD9483B)
    internal val Blue = Color(0xFF3F7FD0)
    internal val Yellow = Color(0xFFF2B33D)
    internal val Pink = Color(0xFFF0A0A0)
    internal val White = Color(0xFFF7F4EC)
    internal val CloudShade = Color(0xFFE0DDD5)
    internal val Orange = Color(0xFFF2A93B)
    internal val Stripe = Color(0xFFD9822B)
}

enum class PlotStage { EMPTY, SPROUT, GROWING, GROWN }

/** The catalog crops (seed_carrot, seed_potato, seed_wheat). */
enum class Crop { CARROT, SWEET_POTATO, WHEAT }

/** Voxel models for the known ids, VPC voxels per cell edge. Fronts face +y. */
object IsoShapes {
    private val P = IsoPalette

    private fun c(x: Int, y: Int, z: Int, w: Int, d: Int, h: Int, color: Color, glow: Color? = null) =
        Cuboid(x, y, z, w, d, h, color, glow)

    /** 16 x 8 x 7. */
    val sofa = IsoShape(2, 1, listOf(
        c(1, 1, 0, 1, 1, 1, P.WoodDeep), c(14, 1, 0, 1, 1, 1, P.WoodDeep),
        c(1, 6, 0, 1, 1, 1, P.WoodDeep), c(14, 6, 0, 1, 1, 1, P.WoodDeep),
        c(0, 0, 1, 16, 8, 2, P.SofaDark),
        c(2, 2, 3, 12, 6, 1, P.Sofa), c(8, 2, 3, 1, 6, 1, P.SofaDark),
        c(0, 0, 3, 16, 2, 4, P.Sofa), c(0, 0, 6, 16, 2, 1, P.SofaLight),
        c(0, 2, 3, 2, 6, 2, P.Sofa), c(14, 2, 3, 2, 6, 2, P.Sofa),
        c(0, 2, 4, 2, 6, 1, P.SofaLight), c(14, 2, 4, 2, 6, 1, P.SofaLight),
    ))

    /** 8 x 8 x 18: a leafy indoor tree in a clay pot, like the S-14 plant. */
    val plant = IsoShape(1, 1, buildList {
        fun lump(x: Int, y: Int, z: Int, w: Int, d: Int, h: Int, color: Color) {
            add(c(x + 1, y, z, w - 2, d, h, color)); add(c(x, y + 1, z, 1, d - 2, h, color)); add(c(x + w - 1, y + 1, z, 1, d - 2, h, color))
        }
        add(c(2, 2, 0, 4, 4, 3, P.Pot)); add(c(2, 2, 3, 4, 4, 1, P.PotRim)); add(c(3, 3, 3, 2, 2, 1, P.SoilDarkC))
        add(c(4, 4, 4, 1, 1, 5, P.Trunk))
        lump(1, 1, 7, 6, 6, 3, P.LeafDark)
        lump(0, 1, 9, 8, 6, 4, P.PlantLeaf); lump(1, 0, 10, 6, 8, 4, P.PlantLeaf)
        lump(1, 1, 13, 6, 6, 3, P.PlantLight); lump(2, 2, 16, 4, 4, 2, P.PlantLeaf)
        for ((x, y, z) in listOf(Triple(0, 3, 11), Triple(7, 4, 10), Triple(3, 7, 12), Triple(5, 0, 12), Triple(2, 6, 15))) {
            add(c(x, y, z, 1, 1, 1, P.PlantLight))
        }
    })

    /** 8 x 1 x 6 on the wall; its height on the wall comes from the object's z. */
    val picture = IsoShape(1, 1, listOf(
        c(1, 0, 0, 6, 1, 6, P.Wood),
        c(2, 0, 1, 4, 1, 2, P.PlantLight), c(2, 0, 3, 4, 1, 2, Color(0xFF74BDF7)), c(4, 0, 4, 1, 1, 1, P.White),
    ), wallMounted = true)

    /** 16 x 16 x 1, an oval with diagonal stripes like the item art. */
    val rug = IsoShape(2, 2, buildList {
        for (j in 0 until 16) for (i in 0 until 16) {
            val e = ((i - 7.5f) / 7.5f).let { it * it } + ((j - 7.5f) / 6.5f).let { it * it }
            if (e > 1f) continue
            val color = when {
                e > 0.72f -> P.Wood
                (i + j) / 3 % 2 == 0 -> P.Red
                else -> P.Cream
            }
            add(c(i, j, 0, 1, 1, 1, color))
        }
    })

    /** 8 x 8 x 14; the shade glows at night. */
    val lamp = IsoShape(1, 1, listOf(
        c(2, 2, 0, 4, 4, 1, P.WoodDark), c(3, 3, 1, 2, 2, 8, P.Wood),
        c(1, 1, 9, 6, 6, 4, P.Shade, P.Light), c(1, 1, 9, 6, 6, 1, P.WoodLight, P.Light),
        c(3, 3, 13, 2, 2, 1, P.WoodDark),
    ))

    /** 16 x 8 x 18, open side to +y. */
    val shelf = IsoShape(2, 1, buildList {
        add(c(0, 0, 0, 1, 6, 18, P.WoodDark)); add(c(15, 0, 0, 1, 6, 18, P.WoodDark))
        add(c(1, 0, 0, 14, 1, 17, P.WoodDeep)); add(c(0, 0, 17, 16, 6, 1, P.Wood))
        for (z in intArrayOf(0, 6, 12)) add(c(1, 1, z, 14, 5, 1, P.Wood))
        val books = listOf(P.Red, P.Blue, P.Leaf, P.Yellow)
        for ((row, z) in intArrayOf(1, 7).withIndex()) for (k in 0 until 6) {
            add(c(2 + k * 2, 1, z, if (k % 3 == 2) 1 else 2, 4, 3 + (k + row) % 3, books[(k + row) % books.size]))
        }
        add(c(11, 2, 13, 2, 2, 1, P.Pot)); add(c(11, 2, 14, 2, 2, 2, P.PlantLeaf))
    })

    /** 24 x 24 x 24: stone footing, plank walls, gable roof running along x, chimney. */
    val house = IsoShape(3, 3, buildList {
        add(c(1, 1, 0, 22, 22, 2, P.StoneC))
        add(c(1, 1, 2, 22, 22, 10, P.Wood))
        for (z in intArrayOf(4, 7, 10)) add(c(1, 1, z, 22, 22, 1, P.WoodDark))
        for ((x, y) in listOf(1 to 1, 22 to 1, 1 to 22, 22 to 22)) add(c(x, y, 2, 1, 1, 10, P.WoodDeep))
        add(c(10, 22, 2, 4, 1, 7, P.WoodDeep)); add(c(9, 23, 0, 6, 1, 1, P.StoneLight))
        add(c(8, 22, 7, 1, 1, 2, P.Light, P.Light)) // porch lantern
        fun window(x: Int, y: Int, alongX: Boolean) {
            if (alongX) {
                add(c(x, y, 6, 4, 1, 3, P.WindowWarm, P.Light)); add(c(x + 2, y, 6, 1, 1, 3, P.WoodDeep))
            } else {
                add(c(x, y, 6, 1, 4, 3, P.WindowWarm, P.Light)); add(c(x, y + 2, 6, 1, 1, 3, P.WoodDeep))
            }
        }
        window(3, 22, true); window(17, 22, true); window(10, 1, true)
        window(22, 10, false); window(1, 10, false)
        for (l in 0 until 12) {
            add(c(1, l, 12 + l, 22, 24 - 2 * l, 1, if (l % 2 == 0) P.Roof else P.RoofDark))
            if (22 - 2 * l > 0) add(c(1, l + 1, 12 + l, 22, 22 - 2 * l, 1, P.Wood))
        }
        add(c(16, 5, 14, 3, 3, 10, P.StoneC)); add(c(16, 5, 23, 3, 3, 1, P.StoneLight))
    })

    /** Soil bed with a plant drawn like the S-13 art: a pixel sprite two voxels deep standing across the cell. Cached. */
    fun plot(stage: PlotStage, crop: Crop = Crop.CARROT): IsoShape = plotCache.getValue(stage to crop)


    private val plotCache: Map<Pair<PlotStage, Crop>, IsoShape> by lazy {
        PlotStage.entries.flatMap { st -> Crop.entries.map { st to it } }.associateWith { (st, cr) -> buildPlot(st, cr) }
    }


    private val spriteColors = mapOf(
        'l' to P.LeafLight, 'L' to P.Leaf, 'd' to P.LeafDark, 'O' to P.Carrot, 'o' to P.Stripe,
        'P' to P.Purple, 'p' to P.Pink, 'Y' to P.Yellow, 's' to P.Shade, 'w' to P.Orange, 'g' to P.Sprout,
    )

    private fun sprite(stage: PlotStage, crop: Crop): List<String> = when {
        stage == PlotStage.EMPTY -> emptyList()
        stage == PlotStage.SPROUT -> listOf("..l..l..", ".lL..Ll.", "..L..L..")
        crop == Crop.CARROT -> listOf(".l....l.", "lLl..lLl", ".Ld..dL.", "lLl..lLl", ".dL..Ld.", "OOO..OOO", "oOO..OOo") +
            if (stage == PlotStage.GROWN) listOf(".OO..OO.", "..O..O..") else emptyList()
        crop == Crop.SWEET_POTATO -> listOf("..lLLl..", ".lLLLLl.", "lLLdLLLl", ".dLLLLd.", "..dLLd..") +
            if (stage == PlotStage.GROWN) listOf(".PP..PP.", "PpPP.PpP", ".PP..PP.") else listOf("..P..P..")
        stage == PlotStage.GROWN -> listOf("YY.YY.YY", "Ys.Ys.Ys", "YY.YY.YY", "Ys.Ys.Ys", ".w..w..w", ".w..w..w", ".w..w..w")
        else -> listOf("ll.ll.ll", "lg.lg.lg", "ll.ll.ll", ".L..L..L", ".L..L..L", ".L..L..L")
    }

    private fun MutableList<Cuboid>.sprite(rows: List<String>, y: Int, rowH: Int) = rows.forEachIndexed { r, line ->
        val z = 1 + (rows.size - 1 - r) * rowH
        line.forEachIndexed { x, ch -> spriteColors[ch]?.let { add(c(x, y, z, 1, 2, rowH, it)) } }
    }

    private fun buildPlot(stage: PlotStage, crop: Crop): IsoShape = IsoShape(1, 1, buildList {
        add(c(0, 0, 0, 8, 8, 1, P.SoilC))
        add(c(0, 2, 0, 8, 1, 1, P.SoilDarkC)); add(c(0, 6, 0, 8, 1, 1, P.SoilDarkC))
        sprite(sprite(stage, crop), 3, 1)
    })

    /** 8 x 8 x 12, a round canopy over a short trunk. */
    val tree = IsoShape(1, 1, listOf(
        c(3, 3, 0, 2, 2, 5, P.Trunk),
        c(0, 0, 4, 8, 8, 1, P.LeafDark), c(0, 0, 5, 8, 8, 4, P.Leaf), c(1, 1, 9, 6, 6, 2, P.LeafLight),
        c(2, 2, 11, 4, 4, 1, P.LeafLight),
        c(0, 0, 4, 1, 1, 5, P.T), c(7, 0, 4, 1, 1, 5, P.T), c(0, 7, 4, 1, 1, 5, P.T), c(7, 7, 4, 1, 1, 5, P.T),
        c(1, 7, 6, 1, 1, 1, P.LeafLight), c(7, 3, 7, 1, 1, 1, P.LeafLight), c(4, 7, 5, 1, 1, 1, P.LeafDark),
    ))

    /** 8 x 8 x 6, runs along x through the middle. Rotate once to run along y. */
    val fence = IsoShape(1, 1, listOf(
        c(0, 3, 0, 2, 2, 6, P.Wood), c(6, 3, 0, 2, 2, 6, P.Wood),
        c(0, 3, 5, 2, 2, 1, P.WoodLight), c(6, 3, 5, 2, 2, 1, P.WoodLight),
        c(2, 4, 2, 4, 1, 1, P.WoodDark), c(2, 4, 4, 4, 1, 1, P.WoodDark),
    ))

    /** Flat stepping stones. */
    val path = IsoShape(1, 1, listOf(
        c(0, 0, 0, 4, 4, 1, P.StoneLight), c(4, 0, 0, 4, 3, 1, P.StoneC),
        c(0, 4, 0, 3, 4, 1, P.StoneC), c(3, 3, 0, 5, 5, 1, P.StoneLight),
    ))

    /** 16 x 16 x 1 pond with a stone rim and a lily pad. */
    val water = IsoShape(2, 2, listOf(
        c(0, 0, 0, 16, 16, 1, P.StoneC), c(2, 2, 0, 12, 12, 1, P.WaterC), c(5, 6, 0, 2, 2, 1, P.Leaf),
        c(0, 0, 0, 1, 1, 1, P.T), c(15, 0, 0, 1, 1, 1, P.T), c(0, 15, 0, 1, 1, 1, P.T), c(15, 15, 0, 1, 1, 1, P.T),
    ))

    /** 16 x 8 x 8, facing +x. */
    fun car(body: Color, lit: Boolean = false): IsoShape = IsoShape(2, 1, listOf(
        c(2, 0, 0, 3, 1, 2, P.Tire), c(11, 0, 0, 3, 1, 2, P.Tire), c(2, 7, 0, 3, 1, 2, P.Tire), c(11, 7, 0, 3, 1, 2, P.Tire),
        c(0, 1, 1, 16, 6, 3, body),
        c(4, 1, 4, 8, 6, 3, P.Glass), c(4, 1, 4, 1, 6, 3, body), c(8, 1, 4, 1, 6, 3, body), c(11, 1, 4, 1, 6, 3, body),
        c(4, 1, 7, 8, 6, 1, body),
        c(15, 1, 2, 1, 1, 1, if (lit) P.Light else P.Cream, P.Light), c(15, 6, 2, 1, 1, 1, if (lit) P.Light else P.Cream, P.Light),
        c(15, 2, 1, 1, 4, 1, P.StoneC), c(0, 1, 2, 1, 1, 1, P.Red), c(0, 6, 2, 1, 1, 1, P.Red),
    ))

    /**
     * The character [id] in [pose] (null or an unknown pose is the plain standing figure), or null for an
     * unknown id. Each character has its own model per pose, see [IsoCharacters]; they are 2 x 2 cells, drawn at half scale.
     */
    fun character(id: String, pose: String?): IsoShape? = IsoCharacters.of(id, pose)

    val sprout: IsoShape get() = character("ch_sprout", null)!!
    val cat: IsoShape get() = character("ch_cat", null)!!
    val mush: IsoShape get() = character("ch_mush", null)!!
    val cloud: IsoShape get() = character("ch_cloud", null)!!
    val firefly: IsoShape get() = character("ch_firefly", null)!!

    private val cars = mapOf("car_red" to car(P.Red), "car_blue" to car(P.Blue), "car_yellow" to car(P.Yellow))
    private val litCars = mapOf("car_red" to car(P.Red, true), "car_blue" to car(P.Blue, true), "car_yellow" to car(P.Yellow, true))

    /** The car [id] with its headlights on (the night glow), for a car that is driving. */
    fun litCar(id: String): IsoShape? = litCars[id] ?: litCars["car_red"].takeIf { id == "car" }

    /** Shape for a catalog or island id, or null for ids this library does not know. */
    fun of(id: String): IsoShape? = when (id) {
        "it_sofa" -> sofa
        "it_plant" -> plant
        "it_picture" -> picture
        "it_rug" -> rug
        "it_lamp" -> lamp
        "it_shelf" -> shelf
        "house" -> house
        "farm_empty" -> plot(PlotStage.EMPTY)
        "farm_sprout" -> plot(PlotStage.SPROUT)
        "farm_growing" -> plot(PlotStage.GROWING)
        "farm_grown" -> plot(PlotStage.GROWN)
        "tree" -> tree
        "fence" -> fence
        "path" -> path
        "water" -> water
        "car" -> cars["car_red"]
        else -> cars[id] ?: character(id, null)
    }
}
