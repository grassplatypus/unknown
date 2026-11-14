package com.break8t.unknown.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WindowScaleTest {
    @Test fun fullWindowIsUnscaled() = assertEquals(1f, windowScale(1682f), 0f)

    @Test fun splitWindowShrinksButStaysNarrow() {
        val k = windowScale(841f)
        assertEquals(0.75f, k, 0.001f)
        assertTrue(841f / k - 182f < 1000f) // 사이드 메뉴를 뺀 본문은 넓은 배치 기준(1000dp) 아래에 남는다
    }
}
