package com.break8t.unknown.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import com.break8t.unknown.R
import com.break8t.unknown.drive.DriveMode
import com.break8t.unknown.drive.LocalUxLimits
import com.break8t.unknown.ui.components.DrivingCover
import com.break8t.unknown.ui.components.SideRail
import com.break8t.unknown.ui.components.drivingBlur
import com.break8t.unknown.data.LocalDemoState
import com.break8t.unknown.ui.home.HomeScreen
import com.break8t.unknown.ui.home.HomeStore
import com.break8t.unknown.ui.settings.SettingsScreen
import com.break8t.unknown.ui.theme.AppColors

/** 사이드 메뉴와 본문. 주행 중이면 허용되지 않은 화면 위에 DrivingCover를 덮는다. */
@Composable
fun AppShell(mode: DriveMode) {
    var route by rememberSaveable { mutableStateOf(Route.HOME) }
    val demo = LocalDemoState.current
    // 어느 메뉴에 있든 기어 변화를 홈 상태에 알려서, P로 바꾼 순간 보상이 들어가게 한다.
    LaunchedEffect(mode) { HomeStore.controller.onMode(mode, demo) }
    Row(Modifier.fillMaxSize().background(AppColors.Background)) {
        SideRail(current = route, onSelect = { route = it })
        Box(Modifier.weight(1f).fillMaxHeight()) {
            val limits = LocalUxLimits.current
            val covered = (!route.allowedWhileDriving && !mode.isParked) || limits.blocks(route.blockedBy)
            val focus = LocalFocusManager.current
            // 덮이는 순간 본문 안에 있던 포커스를 빼서 로터리 입력이 가려진 화면에 닿지 않게 한다.
            LaunchedEffect(covered) { if (covered) focus.clearFocus(force = true) }
            Box(Modifier.fillMaxSize().drivingBlur(covered)) { ScreenFor(route, mode) { route = it } }
            if (covered) DrivingCover(stringResource(route.parkHint))
        }
    }
}

@Composable
private fun ScreenFor(route: Route, mode: DriveMode, onNavigate: (Route) -> Unit) {
    when (route) {
        Route.HOME -> HomeScreen(mode, onNavigate)
        // 계정 화면과 상점은 아직 합치지 않았다. 합치면 콜백을 연결한다.
        Route.SETTINGS -> SettingsScreen(onSwitchAccount = {}, onLogout = {}, onOpenShop = {})
        else -> Box(Modifier.fillMaxSize(), Alignment.Center) {
            Text(stringResource(R.string.screen_coming), color = AppColors.TextSub)
        }
    }
}
