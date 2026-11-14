package com.break8t.unknown.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.break8t.unknown.data.Profile
import com.break8t.unknown.ui.Route
import com.break8t.unknown.ui.components.SideRail
import com.break8t.unknown.ui.theme.AppColors
import com.break8t.unknown.ui.theme.AppTheme

@Preview(name = "기본 창", widthDp = 1682, heightDp = 1210)
@Preview(name = "화면 나눔", widthDp = 841, heightDp = 1210)
internal annotation class HomeSizes

@Composable
private fun Frame(content: @Composable () -> Unit) = AppTheme {
    Row(Modifier.fillMaxSize().background(AppColors.Background)) {
        SideRail(current = Route.HOME, onSelect = {})
        Box(Modifier.weight(1f).fillMaxHeight()) { content() }
    }
}

@HomeSizes
@Composable
private fun S08Parked() = Frame { ParkedHome(Profile(), onVoiceChange = {}) }

@HomeSizes
@Composable
private fun S09aNoRaid() = Frame { DrivingHome(RaidState.None, onEnd = {}) }

@HomeSizes
@Composable
private fun S09bSolo() = Frame { DrivingHome(RaidState.Solo, onEnd = {}) }

@HomeSizes
@Composable
private fun S09cGroup() = Frame { DrivingHome(RaidState.Group(HomeDemo.PARTICIPANTS), onEnd = {}) }

@HomeSizes
@Composable
private fun S09dPassed() = Frame {
    DrivingHome(RaidState.Passed(HomeDemo.RAID_JAM_SEC, coins = 10, HomeDemo.PARTICIPANTS), onEnd = {})
}
