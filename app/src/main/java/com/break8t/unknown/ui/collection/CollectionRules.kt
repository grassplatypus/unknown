package com.break8t.unknown.ui.collection

/** 내 정보 S-20 묶음 썸네일: 가진 것 가운데 가장 최근에 얻은 것(팀 결정). 없으면 null. */
fun newestOwned(items: List<Collectible>): Collectible? = items.filter { it.owned }.maxByOrNull { it.recency }

enum class ListPhase { LOADING, EMPTY, CONTENT }

/** 비어 있음은 다 불러온 뒤에만 정한다. 개수 0은 불러오는 중일 때도 나오기 때문이다. */
fun listPhase(loaded: Boolean, count: Int): ListPhase = when {
    !loaded -> ListPhase.LOADING
    count == 0 -> ListPhase.EMPTY
    else -> ListPhase.CONTENT
}

/** 이름 뒤 조사. 견본 닉네임과 캐릭터 이름이 바뀌어도 "냥이로", "구름이를"처럼 맞게 붙인다. */
object Josa {
    private const val HANGUL_FIRST = 0xAC00
    private const val HANGUL_LAST = 0xD7A3
    private const val FINALS = 28
    private const val FINAL_RIEUL = 8

    // 한글이 아닌 끝 글자는 숫자만 읽는 소리로 판단한다(영, 일, 삼, 육, 칠, 팔).
    private fun finalIndex(word: String): Int {
        val c = word.lastOrNull() ?: return 0
        return when {
            c.code in HANGUL_FIRST..HANGUL_LAST -> (c.code - HANGUL_FIRST) % FINALS
            c in "036" -> 1
            c in "178" -> FINAL_RIEUL
            else -> 0
        }
    }

    fun eulReul(word: String) = word + if (finalIndex(word) != 0) "을" else "를"
    fun waGwa(word: String) = word + if (finalIndex(word) != 0) "과" else "와"
    fun euroRo(word: String) = word + finalIndex(word).let { if (it != 0 && it != FINAL_RIEUL) "으로" else "로" }
}
