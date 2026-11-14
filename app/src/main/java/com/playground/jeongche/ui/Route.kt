package com.playground.jeongche.ui

import androidx.annotation.StringRes
import com.playground.jeongche.R

/** 사이드 메뉴 순서와 같다. 주행 중에는 allowedWhileDriving이 아닌 화면을 흐리게 덮는다. */
enum class Route(@StringRes val label: Int, val allowedWhileDriving: Boolean, @StringRes val parkHint: Int?) {
    HOME(R.string.route_home, true, null),
    ISLAND(R.string.route_island, false, R.string.park_hint_island),
    ENCYCLOPEDIA(R.string.route_encyclopedia, false, R.string.park_hint_encyclopedia),
    FIELD_DROP(R.string.route_field_drop, false, R.string.park_hint_field_drop),
    MY_INFO(R.string.route_my_info, false, R.string.park_hint_my_info),
    SETTINGS(R.string.route_settings, true, null),
}
