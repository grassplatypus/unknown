package com.playground.jeongche.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.playground.jeongche.R
import com.playground.jeongche.drive.DriveMode
import com.playground.jeongche.ui.components.DrivingCover
import com.playground.jeongche.ui.components.SideRail
import com.playground.jeongche.ui.theme.AppColors

/** 사이드 메뉴와 본문. 주행 중이면 허용되지 않은 화면 위에 DrivingCover를 덮는다. */
@Composable
fun AppShell(mode: DriveMode) {
    var route by rememberSaveable { mutableStateOf(Route.HOME) }
    Row(Modifier.fillMaxSize().background(AppColors.Background)) {
        SideRail(current = route, onSelect = { route = it })
        Box(Modifier.weight(1f).fillMaxHeight()) {
            ScreenFor(route, mode)
            val hint = route.parkHint
            if (!route.allowedWhileDriving && !mode.isParked && hint != null) {
                DrivingCover(stringResource(hint))
            }
        }
    }
}

@Composable
private fun ScreenFor(route: Route, mode: DriveMode) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Text(stringResource(R.string.screen_coming), color = AppColors.TextSub)
    }
}
