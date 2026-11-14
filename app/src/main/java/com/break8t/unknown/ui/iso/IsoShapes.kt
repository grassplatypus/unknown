package com.break8t.unknown.ui.iso

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.break8t.unknown.ui.theme.AppColors

/** Scene colors. Side faces are these colors darkened by [faceShade], never a gradient. */
object IsoPalette {
    val Grass = Color(0xFF86C25B)
    val Sand = Color(0xFFE9D39A)
    val Soil = Color(0xFF8A5A36)
    val SoilDark = Color(0xFF6E4527)
    val Water = Color(0xFF4F9FDF)
    val WaterLight = Color(0xFF7FC0F0)
    val Wood = Color(0xFFB07A4A)
    val WoodDark = Color(0xFF7A4E2D)
    val WoodFloor = Color(0xFFD9B27C)
    val Wall = Color(0xFFF3E6C8)
    val Roof = Color(0xFFC0533A)
    val Leaf = Color(0xFF4F9A3A)
    val LeafLight = Color(0xFF73B95A)
    val Sprout = Color(0xFF9BD46B)
    val Crop = AppColors.Selected
    val Stone = Color(0xFFC9BFA8)
    val Window = Color(0xFFA9D6F5)
    val Light = Color(0xFFFFD873)
    val Fabric = Color(0xFF6C8EBF)
    val FabricLight = Color(0xFF8FAED6)
    val Rug = Color(0xFFC56B5A)
    val Cream = AppColors.Card
    val Pot = Color(0xFFC4683F)
    val Metal = Color(0xFF5B5550)
    val Tire = Color(0xFF3A3532)
    val Night = Color(0xFF1B2440)
    val NightOverlay = Night.copy(alpha = 0.45f)

    fun floor(t: FloorType): Color = when (t) {
        FloorType.GRASS -> Grass
        FloorType.SAND -> Sand
        FloorType.SOIL -> Soil
        FloorType.WATER -> Water
        FloorType.WOOD -> WoodFloor
        FloorType.NONE -> Color.Transparent
    }

    /** World-fixed light, so faces keep their shade as the view turns. */
    internal const val TOP = 1f
    internal const val X_POS = 0.68f
    internal const val X_NEG = 0.86f
    internal const val Y_POS = 0.80f
    internal const val Y_NEG = 0.74f

    internal fun faceShade(c: Color, f: Float) = Color(c.red * f, c.green * f, c.blue * f, c.alpha)
    internal fun night(c: Color) = lerp(c, Night, 0.55f)
}

enum class PlotStage { EMPTY, SPROUT, GROWING, GROWN }

/** Procedural shapes for the known item ids. Parts touch but never intersect, which the depth sort relies on. */
object IsoShapes {
    private fun c(x: Float, y: Float, z: Float, w: Float, d: Float, h: Float, color: Color, emissive: Color? = null) =
        Cuboid(x, y, z, w, d, h, color, emissive)

    val sofa = IsoShape(2, 1, listOf(
        c(0f, 0f, 0f, 2f, 0.25f, 0.9f, IsoPalette.Fabric),
        c(0f, 0.25f, 0f, 2f, 0.75f, 0.45f, IsoPalette.Fabric),
        c(0f, 0.25f, 0.45f, 0.2f, 0.75f, 0.25f, IsoPalette.Fabric),
        c(1.8f, 0.25f, 0.45f, 0.2f, 0.75f, 0.25f, IsoPalette.Fabric),
        c(0.2f, 0.3f, 0.45f, 1.6f, 0.65f, 0.1f, IsoPalette.FabricLight),
    ))

    val plant = IsoShape(1, 1, listOf(
        c(0.3f, 0.3f, 0f, 0.4f, 0.4f, 0.4f, IsoPalette.Pot),
        c(0.2f, 0.2f, 0.4f, 0.6f, 0.6f, 0.5f, IsoPalette.Leaf),
        c(0.35f, 0.35f, 0.9f, 0.3f, 0.3f, 0.25f, IsoPalette.LeafLight),
    ))

    val picture = IsoShape(1, 1, listOf(
        c(0.1f, 0f, 1.6f, 0.8f, 0.08f, 0.7f, IsoPalette.Wood),
        c(0.2f, 0.08f, 1.7f, 0.6f, 0.02f, 0.25f, IsoPalette.LeafLight),
        c(0.2f, 0.08f, 1.95f, 0.6f, 0.02f, 0.25f, IsoPalette.WaterLight),
    ), wallMounted = true)

    val rug = IsoShape(2, 2, listOf(
        c(0.1f, 0.1f, 0f, 1.8f, 1.8f, 0.04f, IsoPalette.Rug),
        c(0.4f, 0.4f, 0.04f, 1.2f, 1.2f, 0.02f, IsoPalette.Cream),
    ))

    val lamp = IsoShape(1, 1, listOf(
        c(0.35f, 0.35f, 0f, 0.3f, 0.3f, 0.08f, IsoPalette.Metal),
        c(0.46f, 0.46f, 0.08f, 0.08f, 0.08f, 1.3f, IsoPalette.Metal),
        c(0.25f, 0.25f, 1.38f, 0.5f, 0.5f, 0.4f, IsoPalette.Cream, IsoPalette.Light),
    ))

    val shelf = IsoShape(2, 1, buildList {
        add(c(0f, 0f, 0f, 0.12f, 0.5f, 2.2f, IsoPalette.WoodDark))
        add(c(1.88f, 0f, 0f, 0.12f, 0.5f, 2.2f, IsoPalette.WoodDark))
        add(c(0.12f, 0f, 2.08f, 1.76f, 0.5f, 0.12f, IsoPalette.WoodDark))
        for (z in floatArrayOf(0f, 0.7f, 1.4f)) add(c(0.12f, 0f, z, 1.76f, 0.5f, 0.1f, IsoPalette.Wood))
        val books = listOf(IsoPalette.Roof, IsoPalette.Fabric, IsoPalette.Leaf, IsoPalette.Crop)
        for ((row, z) in floatArrayOf(0.1f, 0.8f).withIndex()) for (k in 0 until 3) {
            add(c(0.2f + k * 0.5f, 0.05f, z, 0.35f, 0.4f, 0.45f + 0.05f * k, books[(row + k) % books.size]))
        }
    })

    val house = IsoShape(3, 3, listOf(
        c(0.1f, 0.1f, 0f, 2.8f, 2.8f, 1.6f, IsoPalette.Wall),
        c(1.2f, 2.9f, 0f, 0.6f, 0.06f, 0.95f, IsoPalette.WoodDark),
        c(0.35f, 2.9f, 0.7f, 0.55f, 0.05f, 0.45f, IsoPalette.Window, IsoPalette.Light),
        c(2.1f, 2.9f, 0.7f, 0.55f, 0.05f, 0.45f, IsoPalette.Window, IsoPalette.Light),
        c(2.9f, 1.2f, 0.7f, 0.05f, 0.6f, 0.45f, IsoPalette.Window, IsoPalette.Light),
        c(1.2f, 0.05f, 0.7f, 0.6f, 0.05f, 0.45f, IsoPalette.Window, IsoPalette.Light),
        c(0.05f, 1.2f, 0.7f, 0.05f, 0.6f, 0.45f, IsoPalette.Window, IsoPalette.Light),
        c(0f, 0f, 1.6f, 3f, 3f, 0.35f, IsoPalette.Roof),
        c(0.4f, 0.4f, 1.95f, 2.2f, 2.2f, 0.35f, IsoPalette.Roof),
        c(0.9f, 0.9f, 2.3f, 1.2f, 1.2f, 0.3f, IsoPalette.Roof),
        c(2.15f, 0.5f, 2.3f, 0.35f, 0.35f, 0.5f, IsoPalette.Stone),
    ))

    fun plot(stage: PlotStage): IsoShape = IsoShape(1, 1, buildList {
        add(c(0.05f, 0.05f, 0f, 0.9f, 0.9f, 0.12f, IsoPalette.Soil))
        val spots = listOf(0.2f to 0.2f, 0.6f to 0.2f, 0.2f to 0.6f, 0.6f to 0.6f)
        for ((x, y) in spots) when (stage) {
            PlotStage.EMPTY -> add(c(x, y, 0.12f, 0.2f, 0.2f, 0.02f, IsoPalette.SoilDark))
            PlotStage.SPROUT -> add(c(x + 0.06f, y + 0.06f, 0.12f, 0.08f, 0.08f, 0.15f, IsoPalette.Sprout))
            PlotStage.GROWING -> add(c(x + 0.02f, y + 0.02f, 0.12f, 0.16f, 0.16f, 0.4f, IsoPalette.LeafLight))
            PlotStage.GROWN -> {
                add(c(x - 0.02f, y - 0.02f, 0.12f, 0.24f, 0.24f, 0.5f, IsoPalette.Leaf))
                add(c(x + 0.03f, y + 0.03f, 0.62f, 0.14f, 0.14f, 0.12f, IsoPalette.Crop))
            }
        }
    })

    val tree = IsoShape(1, 1, listOf(
        c(0.4f, 0.4f, 0f, 0.2f, 0.2f, 0.7f, IsoPalette.WoodDark),
        c(0.1f, 0.1f, 0.7f, 0.8f, 0.8f, 0.7f, IsoPalette.Leaf),
        c(0.25f, 0.25f, 1.4f, 0.5f, 0.5f, 0.4f, IsoPalette.LeafLight),
    ))

    /** Runs along x; rotate once to run along y. */
    val fence = IsoShape(1, 1, listOf(
        c(0f, 0.44f, 0f, 0.12f, 0.12f, 0.6f, IsoPalette.Wood),
        c(0.88f, 0.44f, 0f, 0.12f, 0.12f, 0.6f, IsoPalette.Wood),
        c(0.12f, 0.46f, 0.2f, 0.76f, 0.08f, 0.08f, IsoPalette.Wood),
        c(0.12f, 0.46f, 0.42f, 0.76f, 0.08f, 0.08f, IsoPalette.Wood),
    ))

    val path = IsoShape(1, 1, listOf(c(0.05f, 0.05f, 0f, 0.9f, 0.9f, 0.05f, IsoPalette.Stone)))

    val water = IsoShape(2, 2, listOf(
        c(0f, 0f, 0f, 2f, 2f, 0.03f, IsoPalette.Sand),
        c(0.2f, 0.2f, 0.03f, 1.6f, 1.6f, 0.03f, IsoPalette.Water),
    ))

    /** Faces +x. */
    fun car(body: Color): IsoShape = IsoShape(2, 1, listOf(
        c(0.3f, 0.05f, 0f, 0.3f, 0.1f, 0.22f, IsoPalette.Tire),
        c(1.4f, 0.05f, 0f, 0.3f, 0.1f, 0.22f, IsoPalette.Tire),
        c(0.3f, 0.85f, 0f, 0.3f, 0.1f, 0.22f, IsoPalette.Tire),
        c(1.4f, 0.85f, 0f, 0.3f, 0.1f, 0.22f, IsoPalette.Tire),
        c(0.1f, 0.15f, 0.1f, 1.8f, 0.7f, 0.35f, body),
        c(1.9f, 0.22f, 0.25f, 0.04f, 0.14f, 0.08f, IsoPalette.Cream, IsoPalette.Light),
        c(1.9f, 0.64f, 0.25f, 0.04f, 0.14f, 0.08f, IsoPalette.Cream, IsoPalette.Light),
        c(0.55f, 0.2f, 0.45f, 0.85f, 0.6f, 0.32f, IsoPalette.Window),
        c(0.6f, 0.25f, 0.77f, 0.75f, 0.5f, 0.06f, body),
    ))

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
        "car", "car_red" -> car(Color(0xFFD9483B))
        "car_blue" -> car(Color(0xFF3F7FD0))
        "car_yellow" -> car(Color(0xFFF0C23A))
        else -> null
    }
}
