package com.break8t.unknown.ui.photo

import com.break8t.unknown.config.Catalog
import com.break8t.unknown.config.ItemKind
import com.break8t.unknown.ui.house.CameraPreset
import com.break8t.unknown.ui.house.SceneCamera
import com.break8t.unknown.ui.house.SceneCharacter
import com.break8t.unknown.ui.iso.IsoObject
import com.break8t.unknown.ui.iso.IsoShapes
import kotlin.random.Random

/** 공유 QR을 열 수 있는 시간. 팀이 아직 정하지 않아서 시연용으로 5분을 쓴다. */
const val SHARE_TTL_SEC = 5 * 60

/** 캐릭터 크기 단계(장면 높이 비율). 둘째 값이 Figma S-16-f 견본(64px / 901px)이다. */
val CharacterSizes = listOf(0.05f, 0.071f, 0.1f, 0.14f)

// ponytail: 실제 시간을 따를 때의 밤 기준(19시~6시)은 임시값이다. 섬 화면과 같은 규칙으로 맞춰야 한다.
private const val NIGHT_FROM_HOUR = 19
private const val NIGHT_UNTIL_HOUR = 6

/** 사진 찍기 화면 상태. 화면을 나눠도 남도록 원시 값만 둔다. poseId가 없으면 캐릭터를 세우지 않은 것이다. */
data class PhotoState(
    val preset: CameraPreset = CameraPreset.OBLIQUE,
    val rotation: Int = 0,
    val poseId: String? = null,
    val x: Float = 0.49f,
    val y: Float = 0.53f,
    val sizeIndex: Int = 1,
    val turns: Int = 0,
    val editing: Boolean = false,
    val shot: Boolean = false,
) {
    val camera get() = SceneCamera(preset, rotation)
    val character get() = poseId?.let { SceneCharacter(it, x, y, CharacterSizes[sizeIndex], turns) }

    fun rotated() = copy(rotation = camera.rotated().rotation)
    fun turned() = copy(turns = (turns + 1) % 4)
    fun resized(delta: Int) = copy(sizeIndex = (sizeIndex + delta).coerceIn(0, CharacterSizes.lastIndex))
    fun moved(dx: Float, dy: Float) = copy(x = (x + dx).coerceIn(0f, 1f), y = (y + dy).coerceIn(0f, 1f))

    /** Stands the character in the middle of cell ([cx], [cy]) of a [grid] x [grid] floor. */
    fun placedAt(cx: Int, cy: Int, grid: Int) = copy(x = ((cx + 0.5f) / grid).coerceIn(0f, 1f), y = ((cy + 0.5f) / grid).coerceIn(0f, 1f))

    /** 캐릭터 버튼: 아직 없으면 첫 포즈로 세우고, 편집 카드를 연다. */
    fun startCharacter(options: List<String>, spawn: Pair<Float, Float>? = null) =
        if (poseId != null || spawn == null) copy(poseId = poseId ?: options.first(), editing = true)
        else copy(poseId = options.first(), editing = true, x = spawn.first, y = spawn.second)

    fun toList(): List<Any> = listOf(preset.name, rotation, poseId ?: "", x, y, sizeIndex, turns, editing, shot)

    companion object {
        fun fromList(l: List<Any>) = PhotoState(
            CameraPreset.valueOf(l[0] as String), l[1] as Int, (l[2] as String).ifEmpty { null },
            l[3] as Float, l[4] as Float, l[5] as Int, l[6] as Int, l[7] as Boolean, l[8] as Boolean,
        )
    }
}

/**
 * 고를 수 있는 포즈. 첫 칸은 고른 캐릭터의 기본 그림이다.
 * 포즈 그림(pose_*)은 새싹이 그림뿐이라 다른 캐릭터에는 붙이지 않는다.
 */
fun poseOptions(characterId: String, owned: Set<String>): List<String> {
    val poses = if (characterId == "ch_sprout") Catalog.ofKind(ItemKind.POSE).map { it.id }.filter { it in owned } else emptyList()
    return listOf(characterId) + poses
}

fun isNight(realTime: Boolean, nightFlag: Boolean, hour: Int): Boolean =
    if (realTime) hour >= NIGHT_FROM_HOUR || hour < NIGHT_UNTIL_HOUR else nightFlag

fun remainingSec(deadlineMs: Long, nowMs: Long): Int = ((deadlineMs - nowMs + 999) / 1000).coerceAtLeast(0).toInt()

/** 스캔되지 않는 QR 모양. 세 모서리의 위치 표시만 실제 QR처럼 그린다. */
fun qrModules(seed: Int, size: Int = 25): List<BooleanArray> {
    val rnd = Random(seed)
    val grid = List(size) { BooleanArray(size) { rnd.nextBoolean() } }
    for ((ox, oy) in listOf(0 to 0, size - 7 to 0, 0 to size - 7)) {
        for (dx in -1..7) for (dy in -1..7) {
            val x = ox + dx
            val y = oy + dy
            if (x !in 0 until size || y !in 0 until size) continue
            val ring = maxOf(kotlin.math.abs(dx - 3), kotlin.math.abs(dy - 3))
            grid[y][x] = ring == 3 || ring <= 1
        }
    }
    return grid
}

/**
 * The character as a voxel model standing in the scene. [SceneCharacter.x] and y are fractions of a
 * [grid] x [grid] floor; size 0.071 (the Figma sample) is the model at its own scale.
 */
fun characterObject(c: SceneCharacter, grid: Int, baseScale: Float = 1f, pickable: Boolean = false): IsoObject {
    val pose = c.imageId.takeIf { it.startsWith("pose_") }
    val base = if (pose != null) IsoShapes.sprout else IsoShapes.of(c.imageId) ?: IsoShapes.sprout
    val scale = c.size / CharacterSizes[1] * baseScale
    val turns = IsoShapes.poseTurns(pose) + c.turns
    return IsoObject(
        "character", IsoShapes.posed(base, pose), 0, 0, turns.mod(4),
        ox = c.x * grid - scale / 2f, oy = c.y * grid - scale / 2f,
        z = if (pose == "pose_jump") JUMP_CELLS else 0f, scale = scale, pickable = pickable,
    )
}

/** A rectangle in dp. */
data class FrameRect(val left: Float, val top: Float, val width: Float, val height: Float)

/**
 * The capture frame of S-16 (760 x 640 at 150, 120 in the 1144 x 901 content), shrunk on the right so it
 * ends [gap] before the angle panel at [panelLeft]. All values in dp.
 */
fun captureFrame(width: Float, height: Float, panelLeft: Float, gap: Float): FrameRect {
    val left = width * 150f / 1144f
    val top = height * 120f / 901f
    val w = minOf(width * 760f / 1144f, panelLeft - gap - left)
    return FrameRect(left, top, w.coerceAtLeast(0f), height * 640f / 901f)
}

private const val JUMP_CELLS = 0.3f
