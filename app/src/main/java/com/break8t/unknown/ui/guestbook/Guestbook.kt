package com.break8t.unknown.ui.guestbook

import com.break8t.unknown.config.BalanceConfig
import com.break8t.unknown.config.Catalog
import com.break8t.unknown.config.ItemKind
import com.break8t.unknown.data.Profile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.max

/** 방명록 한 개. [authorId]는 "me" 또는 Friend.id라서 닉네임이 바뀌어도 누가 남겼는지 안다. */
data class GuestbookEntry(
    val id: String,
    val authorId: String,
    val nickname: String,
    val characterId: String,
    val atMs: Long,
    val text: String,
)

/** 섬 id마다 방명록 목록. 백엔드가 없어서 프로세스 안에만 있다(DemoStore와 같은 이유). */
object GuestbookStore {
    /** 내 섬의 id. 친구 섬은 Friend.id다. */
    const val MY_ISLAND = "me"

    /** 내가 남긴 항목의 [GuestbookEntry.authorId]. */
    const val ME = "me"

    private val seededAt = System.currentTimeMillis()

    /** Profile.guestbookLastVisitMs가 없을 때 쓰는 지난 방문 시각. 견본 중 두 개가 이보다 뒤에 온다. */
    val sampleLastVisitMs: Long = seededAt - DAY_MS

    private val state = MutableStateFlow(demoGuestbooks(seededAt))
    val entries: StateFlow<Map<String, List<GuestbookEntry>>> = state

    fun add(islandId: String, entry: GuestbookEntry) =
        state.update { it + (islandId to (listOf(entry) + it[islandId].orEmpty())) }

    fun remove(islandId: String, entryId: String) =
        state.update { it + (islandId to it[islandId].orEmpty().filterNot { e -> e.id == entryId }) }
}

/** 게시판이 섬에 있을 때만 방명록이 켜진다. 넣어 두면 친구도 못 남기고 알림도 없다. */
fun guestbookOn(profile: Profile): Boolean = profile.guestbookBoardPlaced

/** 게시판을 보관함에 넣거나 섬에 놓는다. 개수는 늘 1개라서 이 깃발 하나만 바꾼다. */
fun withBoard(profile: Profile, placed: Boolean): Profile = profile.copy(guestbookBoardPlaced = placed)

/** 야외 장식은 버릴 수 없다(게시판은 넣고 빼기만 한다). */
fun isDiscardable(id: String): Boolean = Catalog.item(id)?.kind != ItemKind.OUTDOOR

/** 내 섬에 지난번 들어온 뒤 새로 온 항목. 내가 남긴 것은 세지 않는다. */
fun newSince(entries: List<GuestbookEntry>, lastVisitMs: Long): List<GuestbookEntry> =
    entries.filter { it.atMs > lastVisitMs && it.authorId != GuestbookStore.ME }

/** 섬에 들어올 때 알림 창에 띄울 개수. 게시판을 넣어 두었으면 0이다. */
fun noticeCount(profile: Profile, entries: List<GuestbookEntry>, sampleLastVisitMs: Long): Int =
    if (!guestbookOn(profile)) 0 else newSince(entries, profile.guestbookLastVisitMs ?: sampleLastVisitMs).size

/**
 * 지울 수 있는지. 섬 주인은 자기 섬의 모든 항목을, 다른 사람은 자기가 남긴 것만 지운다.
 * [viewerId]는 보는 사람(나는 "me"), [islandId]는 섬 주인의 id다.
 */
fun canDelete(islandId: String, entry: GuestbookEntry, viewerId: String = GuestbookStore.ME): Boolean =
    islandId == viewerId || entry.authorId == viewerId

/** 친구 섬에 게시판이 있는지. 견본으로 한 명은 게시판을 넣어 두었다. */
fun friendBoardShown(friendId: String): Boolean = friendId !in friendsWithBoardStored

internal val friendsWithBoardStored = setOf("f_turtle")

/** 남길 글. 앞뒤 공백을 지우고 최대 글자 수로 자른다. 비어 있으면 남길 수 없어 null이다. */
fun guestbookText(raw: String, config: BalanceConfig = BalanceConfig.DEFAULT): String? =
    raw.trim().take(config.guestbookMaxChars).takeIf { it.isNotEmpty() }

/** 목록에 보일 "얼마 전". 화면 문구는 부르는 쪽이 고른다. */
sealed interface Ago {
    data object JustNow : Ago
    data class Minutes(val n: Long) : Ago
    data class Hours(val n: Long) : Ago
    data class Days(val n: Long) : Ago
}

fun ago(nowMs: Long, atMs: Long): Ago {
    val m = max(0, nowMs - atMs) / 60_000
    return when {
        m < 1 -> Ago.JustNow
        m < 60 -> Ago.Minutes(m)
        m < 24 * 60 -> Ago.Hours(m / 60)
        else -> Ago.Days(m / (24 * 60))
    }
}

internal const val DAY_MS = 24 * 60 * 60 * 1000L
private const val HOUR_MS = 60 * 60 * 1000L

// 남긴 사람은 Friends.kt의 견본 친구와 같은 id, 닉네임, 캐릭터다.
private data class Author(val id: String, val nickname: String, val characterId: String)

private val acorn = Author("f_acorn", "도토리길", "ch_cat")
private val turtle = Author("f_turtle", "느린거북", "ch_mush")
private val dawn = Author("f_dawn", "새벽출근", "ch_cloud")
private val camper = Author("f_camper", "주말캠퍼", "ch_sprout")
private val dusk = Author("f_dusk", "퇴근요정", "ch_firefly")

private fun sample(id: String, a: Author, atMs: Long, text: String) =
    GuestbookEntry(id, a.id, a.nickname, a.characterId, atMs, text)

internal fun demoGuestbooks(now: Long): Map<String, List<GuestbookEntry>> = mapOf(
    GuestbookStore.MY_ISLAND to listOf(
        sample("g_me_1", dawn, now - 2 * HOUR_MS, "집이 너무 못생겼어요"),
        sample("g_me_2", acorn, now - 9 * HOUR_MS, "일요일 간술 ㄱ"),
        sample("g_me_3", camper, now - 3 * DAY_MS, "오늘 할일 돼지 갈비 1kg 사기 사과 1박스 사기"),
        sample("g_me_4", dusk, now - 6 * DAY_MS, "가나다라마바사아자차카타파하"),
    ),
    "f_acorn" to listOf(
        sample("g_acorn_1", dawn, now - 5 * HOUR_MS, "도토리길님 섬에 사과나무가 예뻐요."),
        sample("g_acorn_2", dusk, now - 2 * DAY_MS, "오늘도 고생 많았어요. 푹 쉬어요."),
    ),
    "f_dawn" to listOf(
        sample("g_dawn_1", acorn, now - 30 * 60 * 1000L, "새벽출근님, 오늘은 좀 일찍 나섰어요?"),
    ),
    "f_camper" to listOf(
        sample("g_camper_1", turtle, now - DAY_MS, "천천히 가도 괜찮아요. 주말 캠핑 사진 보여 주세요."),
        sample("g_camper_2", dawn, now - 3 * DAY_MS, "금요일이 금방 올 거예요."),
        sample("g_camper_3", acorn, now - 4 * DAY_MS, "텃밭에 고구마 심었더라고요. 수확하면 나눠 줘요."),
    ),
    "f_dusk" to listOf(
        sample("g_dusk_1", camper, now - 7 * HOUR_MS, "퇴근요정님 섬은 밤에 더 예뻐요."),
    ),
)
