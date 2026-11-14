package com.break8t.unknown.ui

import androidx.annotation.StringRes
import com.break8t.unknown.R
import com.break8t.unknown.drive.UxLimits

/**
 * 사이드 메뉴 순서와 같다. 두 가지로 덮는다: 팀 결정(allowedWhileDriving=false면 P가 아닐 때)과
 * 플랫폼의 지역 규제(blockedBy 플래그가 CarUxRestrictions에 걸려 있을 때).
 */
enum class Route(
    @StringRes val label: Int,
    val allowedWhileDriving: Boolean,
    @StringRes val parkHint: Int,
    val blockedBy: Int = 0,
) {
    HOME(R.string.route_home, true, R.string.park_hint_home),
    ISLAND(R.string.route_island, false, R.string.park_hint_island),
    ENCYCLOPEDIA(R.string.route_encyclopedia, false, R.string.park_hint_encyclopedia),
    FIELD_DROP(R.string.route_field_drop, false, R.string.park_hint_field_drop),
    MY_INFO(R.string.route_my_info, false, R.string.park_hint_my_info),
    SETTINGS(R.string.route_settings, true, R.string.park_hint_settings, blockedBy = UxLimits.NO_SETUP),
}
