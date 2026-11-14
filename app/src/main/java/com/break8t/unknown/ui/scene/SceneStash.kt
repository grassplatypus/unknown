package com.break8t.unknown.ui.scene

import com.break8t.unknown.config.Catalog
import com.break8t.unknown.config.ItemKind
import com.break8t.unknown.ui.house.SceneCharacter
import com.break8t.unknown.ui.iso.IsoObject
import com.break8t.unknown.ui.iso.IsoShapes
import com.break8t.unknown.ui.photo.CharacterSizes
import com.break8t.unknown.ui.photo.characterObject

/** 섬과 집 장면의 보관함에 드는 것의 종류. */
enum class StashKind { CHARACTER, FURNITURE, OBJECT }

/** 보관함의 한 칸. [id]는 Catalog id이고, 장면에서도 이 id로 찾는다. */
data class StashItem(val id: String, val kind: StashKind)

/** 그 장면에서 이 항목을 꺼낼 수 있는지. 친구 섬과 섬 꾸미기가 서로 다른 규칙을 넘긴다. */
typealias StashRule = (StashItem) -> Boolean

/** 친구 섬: 섬 주인의 캐릭터만 꺼내고, 가구와 사물은 놓을 수 없다. */
val FriendVisitRule: StashRule = { it.kind == StashKind.CHARACTER }

/** 장면에 꺼내 놓은 것. x, y는 바닥에 대한 비율이고, [sizeIndex]와 [poseId]는 캐릭터만 쓴다. */
data class StagePiece(
    val itemId: String,
    val kind: StashKind,
    val x: Float,
    val y: Float,
    val turns: Int = 0,
    val sizeIndex: Int = DEFAULT_SIZE,
    val poseId: String? = null,
) {
    /** The look characterObject draws: the plain character, or the pose put on it. */
    val look get() = poseId ?: itemId
}

/** How far a piece may go: a character's whole footprint stays on the [grid] x [grid] floor. */
data class StageBounds(val grid: Int, val baseScale: Float = 1f) {
    /** Half the footprint as a fraction of the floor; characterObject spans size / CharacterSizes[1] * baseScale cells. */
    fun margin(p: StagePiece): Float =
        if (p.kind != StashKind.CHARACTER) 0f
        else (CharacterSizes[p.sizeIndex] / CharacterSizes[DEFAULT_SIZE] * baseScale / grid / 2f).coerceAtMost(0.5f)

    fun clamp(p: StagePiece): StagePiece {
        val m = margin(p)
        return p.copy(x = p.x.coerceIn(m, 1f - m), y = p.y.coerceIn(m, 1f - m))
    }
}

/**
 * 장면 보관함 상태: 꺼낸 것, 조종 중인 것([selected]), 패널이 열렸는지. 화면은 이 함수들로만 바꾼다.
 * 화면을 나눠도 남도록 [toList]와 [fromList]로 원시 값만 저장한다.
 */
data class SceneStage(
    val pieces: List<StagePiece> = emptyList(),
    val selected: String? = null,
    val panelOpen: Boolean = false,
) {
    fun piece(id: String?) = pieces.firstOrNull { it.itemId == id }
    fun isOut(id: String) = piece(id) != null

    /** What the panel lists: everything not already in the scene. */
    fun inStash(items: List<StashItem>) = items.filterNot { isOut(it.id) }

    fun togglePanel() = copy(panelOpen = !panelOpen, selected = null)

    /** 꺼내기: 규칙이 막거나 이미 나와 있으면 그대로. 꺼내면 패널을 닫고 바로 조종한다. */
    fun takeOut(item: StashItem, rule: StashRule, spot: Pair<Float, Float>, bounds: StageBounds): SceneStage {
        if (!rule(item) || isOut(item.id)) return this
        val p = bounds.clamp(StagePiece(item.id, item.kind, spot.first, spot.second))
        return copy(pieces = pieces + p, selected = item.id, panelOpen = false)
    }

    fun putAway(id: String) = copy(pieces = pieces.filterNot { it.itemId == id }, selected = selected.takeIf { it != id })

    fun select(id: String?) = copy(selected = id?.takeIf(::isOut), panelOpen = false)

    fun moveBy(id: String, dx: Float, dy: Float, bounds: StageBounds) = edit(id) { bounds.clamp(it.copy(x = it.x + dx, y = it.y + dy)) }

    fun moveTo(id: String, x: Float, y: Float, bounds: StageBounds) = edit(id) { bounds.clamp(it.copy(x = x, y = y)) }

    /** Stands piece [id] in the middle of floor cell ([cx], [cy]). */
    fun placeAtCell(id: String, cx: Int, cy: Int, bounds: StageBounds) =
        moveTo(id, (cx + 0.5f) / bounds.grid, (cy + 0.5f) / bounds.grid, bounds)

    fun turn(id: String) = edit(id) { it.copy(turns = (it.turns + 1) % 4) }

    /** 크기는 캐릭터만. 커지면 가장자리에서 안쪽으로 밀어서 장면 밖으로 나가지 않게 한다. */
    fun resize(id: String, delta: Int, bounds: StageBounds) = edit(id) {
        if (it.kind != StashKind.CHARACTER) it
        else bounds.clamp(it.copy(sizeIndex = (it.sizeIndex + delta).coerceIn(0, CharacterSizes.lastIndex)))
    }

    /** 포즈는 캐릭터만. 캐릭터 자신의 id를 고르면 기본 포즈다. */
    fun pose(id: String, look: String) = edit(id) {
        if (it.kind != StashKind.CHARACTER) it else it.copy(poseId = look.takeIf { l -> l != it.itemId })
    }

    /** 뒤로: 열린 패널, 조종 카드 순서로 닫는다. 닫을 것이 없으면 null(화면을 떠난다). */
    fun back(): SceneStage? = when {
        panelOpen -> copy(panelOpen = false)
        selected != null -> copy(selected = null)
        else -> null
    }

    private fun edit(id: String, f: (StagePiece) -> StagePiece) = copy(pieces = pieces.map { if (it.itemId == id) f(it) else it })

    fun toList(): List<Any> = listOf<Any>(selected.orEmpty(), panelOpen) + pieces.flatMap {
        listOf(it.itemId, it.kind.name, it.x, it.y, it.turns, it.sizeIndex, it.poseId.orEmpty())
    }

    companion object {
        private const val FIELDS = 7

        fun fromList(l: List<Any>) = SceneStage(
            l.drop(2).chunked(FIELDS).map {
                StagePiece(
                    it[0] as String, StashKind.valueOf(it[1] as String), it[2] as Float, it[3] as Float,
                    it[4] as Int, it[5] as Int, (it[6] as String).ifEmpty { null },
                )
            },
            (l[0] as String).ifEmpty { null },
            l[1] as Boolean,
        )
    }
}

/** The Figma S-16-f size step, the one a piece starts at. */
const val DEFAULT_SIZE = 1

/** 친구 섬의 포즈: 기본과 모든 포즈. 친구 캐릭터라 내 보유 목록과 상관없다. */
fun allPoseOptions(characterId: String): List<String> = listOf(characterId) + Catalog.ofKind(ItemKind.POSE).map { it.id }

/** The scene objects for the pieces. Characters go through characterObject; the id stays the item id. */
fun stageObjects(stage: SceneStage, bounds: StageBounds): List<IsoObject> = stage.pieces.mapNotNull { p ->
    when (p.kind) {
        StashKind.CHARACTER -> characterObject(
            SceneCharacter(p.look, p.x, p.y, CharacterSizes[p.sizeIndex], p.turns), bounds.grid, bounds.baseScale,
            pickable = true, characterId = p.itemId,
        ).copy(id = p.itemId)
        else -> IsoShapes.of(p.itemId)?.let {
            IsoObject(p.itemId, it, (p.x * bounds.grid).toInt(), (p.y * bounds.grid).toInt(), p.turns)
        }
    }
}

/** 사진 찍기를 열 때 꺼내 둔 캐릭터를 그 자리, 크기, 포즈 그대로 넘긴다. 꺼내지 않았으면 null. */
fun SceneStage.photoCharacter(characterId: String): SceneCharacter? =
    piece(characterId)?.takeIf { it.kind == StashKind.CHARACTER }?.let {
        SceneCharacter(it.look, it.x, it.y, CharacterSizes[it.sizeIndex], it.turns)
    }
