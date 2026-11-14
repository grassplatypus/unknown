package com.break8t.unknown.ui.myinfo

import com.break8t.unknown.config.BalanceConfig
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.random.Random

/** 친구 한 명. 백엔드가 없어서 견본이다. 그림은 ch_* 캐릭터 id다. */
data class Friend(
    val id: String,
    val nickname: String,
    val status: String,
    val characterId: String,
    val driving: Boolean,
    val jamsThisWeek: Int,
    val lastPostcard: String,
    val since: String,
    // 섬 놀러 가기와 컬렉션 견본. 엽서와 뱃지 id는 도감 견본(ui/collection/Collectible.kt)의 id다.
    val carId: String = "car_red",
    val islandLevel: Int = 0,
    val postcardIds: Set<String> = emptySet(),
    val badgeIds: Set<String> = emptySet(),
)

// Figma 견본의 이름은 실제 사람 이름이 섞여 있어서 쓰지 않고 새로 지었다.
internal val demoFriends = listOf(
    Friend("f_acorn", "도토리길", "출근길 같이 버텨요", "ch_cat", driving = true, 6, "벚꽃길", "2026.10.02",
        "car_blue", 1, setOf("post_blossom", "post_coast", "post_cheomseongdae_xmas"), setOf("badge_regular", "badge_postcard_3")),
    Friend("f_turtle", "느린거북", "서두르지 않아요", "ch_mush", driving = true, 3, "등대 해안도로", "2026.09.18",
        "car_yellow", 0, setOf("post_coast"), setOf("badge_safe")),
    Friend("f_dawn", "새벽출근", "오늘도 서부간선", "ch_cloud", driving = false, 9, "경주 첨성대", "2026.08.30",
        "car_red", 2, setOf("post_cheomseongdae", "post_cheomseongdae_gold", "post_coast_gold", "post_blossom"),
        setOf("badge_regular", "badge_holiday", "badge_night", "badge_postcard_3")),
    Friend("f_camper", "주말캠퍼", "금요일만 기다려요", "ch_sprout", driving = false, 1, "등대 해안도로", "2026.08.11",
        "car_red", 0, setOf("post_coast"), emptySet()),
    Friend("f_dusk", "퇴근요정", "정시 퇴근이 목표", "ch_firefly", driving = false, 4, "벚꽃길", "2026.07.05",
        "car_yellow", 1, setOf("post_blossom", "post_cheomseongdae"), setOf("badge_night")),
)

/** 화면을 나누면 Activity가 다시 만들어지므로 친구 목록은 프로세스에 하나만 둔다(DemoStore와 같은 이유). */
object FriendStore {
    val friends = MutableStateFlow(demoFriends)
}

/** 방명록 프로필에서 고른 친구 화면. 내 정보가 열리면 그 화면으로 가고 비운다. */
object FriendNav {
    data class Request(val friendId: String, val collection: Boolean)
    val request = MutableStateFlow<Request?>(null)
}

object FriendCode {
    // ponytail: 번호 길이는 BalanceConfig에 없어서 여기 둔다. 팀이 바꿀 값이면 BalanceConfig로 옮긴다.
    const val LENGTH = 6

    /** 시연할 때 이 번호를 넣으면 [demoNewFriend]가 친구가 된다(D-23). */
    const val DEMO = "123456"

    private val modulus = (1..LENGTH).fold(1) { acc, _ -> acc * 10 }

    /** 내 번호. 같은 [BalanceConfig.friendCodeTtlSec] 구간 안에서는 같고, 구간이 바뀌면 새 번호가 된다. */
    fun mine(epochSec: Long, config: BalanceConfig): String {
        val n = Random(epochSec / config.friendCodeTtlSec).nextInt(modulus)
        val code = n.toString().padStart(LENGTH, '0')
        return if (code == DEMO) ((n + 1) % modulus).toString().padStart(LENGTH, '0') else code
    }

    fun secondsLeft(epochSec: Long, config: BalanceConfig): Int =
        (config.friendCodeTtlSec - epochSec % config.friendCodeTtlSec).toInt()

    /** "482913" -> "482 913" */
    fun spaced(code: String) = code.chunked(LENGTH / 2).joinToString(" ")
}

enum class AddCheck { FULL, INCOMPLETE, WRONG, OK }

/** 친구 번호 확인. 가득 찼으면 번호와 상관없이 FULL(S-23-c), 틀리면 WRONG(S-23-b). */
fun checkFriendCode(input: String, friends: List<Friend>, config: BalanceConfig): AddCheck = when {
    friends.size >= config.maxFriends -> AddCheck.FULL
    input.length < FriendCode.LENGTH -> AddCheck.INCOMPLETE
    input != FriendCode.DEMO || friends.any { it.id == demoNewFriend().id } -> AddCheck.WRONG
    else -> AddCheck.OK
}

fun demoNewFriend(today: LocalDate = LocalDate.now()) = Friend(
    "f_star", "별빛운전", "밤길이 좋아요", "ch_firefly", driving = false, 0, "벚꽃길",
    today.format(DateTimeFormatter.ofPattern("yyyy.MM.dd")),
)
