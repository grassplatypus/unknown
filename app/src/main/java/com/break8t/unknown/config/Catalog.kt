package com.break8t.unknown.config

/**
 * 아이템 이름, 가격, 작물 시간. 여러 화면이 같은 값을 써야 해서 한 곳에 둔다.
 * id는 Figma 그림 이름과 같다. 가격은 Figma 견본, "임시" 표시는 팀이 정하지 않은 값이다.
 */
enum class ItemKind { SEED, FURNITURE, CHARACTER, CAR, VOICE_PACK, POSE }

data class CatalogItem(val id: String, val name: String, val kind: ItemKind, val price: Int?)

data class CropSpec(val seedId: String, val name: String, val growMinutes: Int, val harvestCoins: Int)

object Catalog {
    val items: List<CatalogItem> = listOf(
        CatalogItem("seed_carrot", "당근 씨앗", ItemKind.SEED, 10),
        CatalogItem("seed_potato", "고구마 씨앗", ItemKind.SEED, 20),
        CatalogItem("seed_wheat", "밀 씨앗", ItemKind.SEED, 15),
        CatalogItem("it_sofa", "소파", ItemKind.FURNITURE, 120),
        CatalogItem("it_plant", "화분", ItemKind.FURNITURE, 40),
        CatalogItem("it_picture", "액자", ItemKind.FURNITURE, 60),
        CatalogItem("it_rug", "러그", ItemKind.FURNITURE, 80),
        CatalogItem("it_lamp", "스탠드", ItemKind.FURNITURE, 90),
        CatalogItem("it_shelf", "책장", ItemKind.FURNITURE, 300),
        CatalogItem("ch_sprout", "새싹이", ItemKind.CHARACTER, null),
        CatalogItem("ch_cat", "냥이", ItemKind.CHARACTER, null),
        CatalogItem("ch_mush", "버섯이", ItemKind.CHARACTER, null),
        CatalogItem("ch_cloud", "구름이", ItemKind.CHARACTER, null),
        CatalogItem("ch_firefly", "반딧이", ItemKind.CHARACTER, 200), // 임시
        CatalogItem("car_red", "빨간 차", ItemKind.CAR, null),
        CatalogItem("car_blue", "파란 차", ItemKind.CAR, null),
        CatalogItem("car_yellow", "노란 차", ItemKind.CAR, null),
        CatalogItem("voice_default", "기본 목소리", ItemKind.VOICE_PACK, null),
        CatalogItem("voice_sprout", "새싹이 목소리", ItemKind.VOICE_PACK, 200), // 임시
        CatalogItem("voice_cat", "냥이 목소리", ItemKind.VOICE_PACK, 300), // 임시
        CatalogItem("pose_wave", "인사", ItemKind.POSE, null),
        CatalogItem("pose_jump", "점프", ItemKind.POSE, 50),
        CatalogItem("pose_sit", "앉기", ItemKind.POSE, null),
        CatalogItem("pose_side", "옆모습", ItemKind.POSE, 80),
        CatalogItem("pose_back", "뒷모습", ItemKind.POSE, null),
    )

    // 수확 코인은 Figma T-05(밀 2칸, 코인 40)에서 밀만 나온다. 나머지는 임시.
    val crops: List<CropSpec> = listOf(
        CropSpec("seed_carrot", "당근", growMinutes = 2 * 60, harvestCoins = 15),
        CropSpec("seed_potato", "고구마", growMinutes = 2 * 24 * 60, harvestCoins = 60),
        CropSpec("seed_wheat", "밀", growMinutes = 6 * 60, harvestCoins = 20),
    )

    fun item(id: String): CatalogItem? = items.firstOrNull { it.id == id }
    fun ofKind(kind: ItemKind): List<CatalogItem> = items.filter { it.kind == kind }
}
