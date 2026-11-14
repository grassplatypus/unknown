package com.break8t.unknown.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import com.break8t.unknown.R
import com.break8t.unknown.data.LocalDemoState
import com.break8t.unknown.drive.DriveMode
import com.break8t.unknown.drive.LocalUxLimits
import com.break8t.unknown.ui.components.DrivingCover
import com.break8t.unknown.ui.components.SideRail
import com.break8t.unknown.ui.fielddrop.FieldDropScreen
import com.break8t.unknown.ui.home.HomeScreen
import com.break8t.unknown.ui.home.HomeStore
import com.break8t.unknown.ui.inventory.InventoryScreen
import com.break8t.unknown.ui.island.IslandPlace
import com.break8t.unknown.ui.island.IslandScreen
import com.break8t.unknown.ui.settings.SettingsScreen
import com.break8t.unknown.ui.shop.ShopScreen
import com.break8t.unknown.ui.theme.AppColors

/** 섬 안에서 들어가는 화면. 사이드 메뉴에는 없고 섬 메뉴 아래에 쌓인다. */
enum class IslandSub { SHOP, INVENTORY }

/**
 * 사이드 메뉴와 본문. 덮어야 하면 본문을 아예 그리지 않는다. 대화상자는 별도 창이라
 * 덮개로 가릴 수 없어서다. 화면 상태는 SaveableStateHolder가 보관해 P로 돌아오면 이어진다.
 */
@Composable
fun AppShell(mode: DriveMode) {
    var route by rememberSaveable { mutableStateOf(Route.HOME) }
    var sub by rememberSaveable { mutableStateOf<IslandSub?>(null) }
    val demo = LocalDemoState.current
    // 어느 메뉴에 있든 기어 변화를 홈 상태에 알려서, P로 바꾼 순간 보상이 들어가게 한다.
    LaunchedEffect(mode) { HomeStore.controller.onMode(mode, demo) }
    val holder = rememberSaveableStateHolder()
    val nav = Nav(
        go = { route = it; sub = null },
        open = { route = Route.ISLAND; sub = it },
        back = { sub = null },
    )
    Row(Modifier.fillMaxSize().background(AppColors.Background)) {
        SideRail(current = route, onSelect = nav.go)
        Box(Modifier.weight(1f).fillMaxHeight()) {
            val limits = LocalUxLimits.current
            val covered = (!route.allowedWhileDriving && !mode.isParked) || limits.blocks(route.blockedBy)
            val focus = LocalFocusManager.current
            LaunchedEffect(covered) { if (covered) focus.clearFocus(force = true) }
            if (covered) {
                DrivingCover(stringResource(route.parkHint))
            } else {
                holder.SaveableStateProvider("${route.name}/${sub?.name}") { ScreenFor(route, sub, mode, nav) }
            }
        }
    }
}

private class Nav(val go: (Route) -> Unit, val open: (IslandSub) -> Unit, val back: () -> Unit)

@Composable
private fun ScreenFor(route: Route, sub: IslandSub?, mode: DriveMode, nav: Nav) {
    when {
        route == Route.ISLAND && sub == IslandSub.SHOP ->
            ShopScreen(onBack = nav.back, onOpenInventory = { nav.open(IslandSub.INVENTORY) })
        route == Route.ISLAND && sub == IslandSub.INVENTORY ->
            InventoryScreen(onBack = nav.back, onOpenShop = { nav.open(IslandSub.SHOP) }, onPlaceFurniture = nav.back)
        // 집과 사진 화면은 아직 합치지 않았다.
        route == Route.ISLAND -> IslandScreen(onOpen = { place ->
            when (place) {
                IslandPlace.SHOP -> nav.open(IslandSub.SHOP)
                IslandPlace.INVENTORY -> nav.open(IslandSub.INVENTORY)
                IslandPlace.HOUSE, IslandPlace.PHOTO -> Unit
            }
        })
        route == Route.HOME -> HomeScreen(mode, nav.go)
        route == Route.FIELD_DROP -> FieldDropScreen()
        // 계정 화면은 아직 합치지 않았다. 합치면 계정 전환과 로그아웃을 연결한다.
        route == Route.SETTINGS -> SettingsScreen(onSwitchAccount = {}, onLogout = {}, onOpenShop = { nav.open(IslandSub.SHOP) })
        else -> Box(Modifier.fillMaxSize(), Alignment.Center) {
            Text(stringResource(R.string.screen_coming), color = AppColors.TextSub)
        }
    }
}
