package com.break8t.unknown.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import com.break8t.unknown.ui.photo.ShareHandler
import com.break8t.unknown.data.LocalDemoState
import com.break8t.unknown.drive.DriveMode
import com.break8t.unknown.drive.LocalUxLimits
import com.break8t.unknown.ui.components.DrivingCover
import com.break8t.unknown.ui.components.SideRail
import com.break8t.unknown.ui.fielddrop.FieldDropScreen
import com.break8t.unknown.ui.home.HomeScreen
import com.break8t.unknown.ui.home.HomeStore
import com.break8t.unknown.ui.encyclopedia.EncyclopediaScreen
import com.break8t.unknown.ui.house.HouseScreen
import com.break8t.unknown.ui.inventory.InventoryScreen
import com.break8t.unknown.ui.island.IslandPlace
import com.break8t.unknown.ui.island.IslandScreen
import com.break8t.unknown.ui.myinfo.MyInfoScreen
import com.break8t.unknown.ui.photo.PhotoScreen
import com.break8t.unknown.ui.photo.ShareKind
import com.break8t.unknown.ui.photo.ShareQrScreen
import com.break8t.unknown.ui.settings.SettingsScreen
import com.break8t.unknown.ui.shop.ShopScreen
import com.break8t.unknown.ui.theme.AppColors

/** 섬 안에서 들어가는 화면. 사이드 메뉴에는 없고 섬 메뉴 아래에 쌓인다. */
enum class IslandSub { SHOP, INVENTORY, HOUSE, PHOTO_ISLAND, PHOTO_HOUSE }

/**
 * 사이드 메뉴와 본문. 덮어야 하면 본문을 아예 그리지 않는다. 대화상자는 별도 창이라
 * 덮개로 가릴 수 없어서다. 화면 상태는 SaveableStateHolder가 보관해 P로 돌아오면 이어진다.
 */
@Composable
fun AppShell(mode: DriveMode, onSignOut: () -> Unit = {}) {
    var route by rememberSaveable { mutableStateOf(Route.HOME) }
    var sub by rememberSaveable { mutableStateOf<IslandSub?>(null) }
    val demo = LocalDemoState.current
    // 어느 메뉴에 있든 기어 변화를 홈 상태에 알려서, P로 바꾼 순간 보상이 들어가게 한다.
    LaunchedEffect(mode) { HomeStore.controller.onMode(mode, demo) }
    // 홈을 열지 않아도 정체를 받도록 내비 연결은 앱 틀에서 한다. 두 번 불러도 한 번만 연결된다.
    val context = LocalContext.current
    LaunchedEffect(Unit) { HomeStore.controller.attachNavi(context) }
    val holder = rememberSaveableStateHolder()
    // 공유 화면은 도감, 내 정보, 홈 알림이 함께 쓴다. 열려 있으면 본문 대신 보인다.
    var shareKind by rememberSaveable { mutableStateOf<ShareKind?>(null) }
    var shareTitle by rememberSaveable { mutableStateOf("") }
    var shareSubtitle by rememberSaveable { mutableStateOf("") }
    var shareImage by rememberSaveable { mutableStateOf<Int?>(null) }
    val nav = Nav(
        go = { route = it; sub = null; shareKind = null },
        open = { route = Route.ISLAND; sub = it },
        back = { sub = null },
        signOut = onSignOut,
        share = { isPostcard, title, subtitle, image ->
            shareKind = if (isPostcard) ShareKind.POSTCARD else ShareKind.JAM_CARD
            shareTitle = title
            shareSubtitle = subtitle
            shareImage = image
        },
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
                val kind = shareKind
                if (kind != null) ShareQrScreen(kind, shareTitle, shareSubtitle, shareImage, onClose = { shareKind = null })
                else holder.SaveableStateProvider("${route.name}/${sub?.name}") { ScreenFor(route, sub, mode, nav) }
            }
        }
    }
}

private class Nav(
    val go: (Route) -> Unit,
    val open: (IslandSub) -> Unit,
    val back: () -> Unit,
    val signOut: () -> Unit,
    val share: ShareHandler,
)

@Composable
private fun ScreenFor(route: Route, sub: IslandSub?, mode: DriveMode, nav: Nav) {
    when {
        route == Route.ISLAND && sub == IslandSub.SHOP ->
            ShopScreen(onBack = nav.back, onOpenInventory = { nav.open(IslandSub.INVENTORY) })
        route == Route.ISLAND && sub == IslandSub.INVENTORY ->
            InventoryScreen(onBack = nav.back, onOpenShop = { nav.open(IslandSub.SHOP) }, onPlaceFurniture = { nav.open(IslandSub.HOUSE) })
        route == Route.ISLAND && sub == IslandSub.HOUSE ->
            HouseScreen(onBack = nav.back, onOpenInventory = { nav.open(IslandSub.INVENTORY) }, onTakePhoto = { nav.open(IslandSub.PHOTO_HOUSE) })
        route == Route.ISLAND && sub == IslandSub.PHOTO_ISLAND -> PhotoScreen(inHouse = false, onClose = nav.back)
        route == Route.ISLAND && sub == IslandSub.PHOTO_HOUSE -> PhotoScreen(inHouse = true, onClose = { nav.open(IslandSub.HOUSE) })
        route == Route.ISLAND -> IslandScreen(onOpen = { place ->
            when (place) {
                IslandPlace.SHOP -> nav.open(IslandSub.SHOP)
                IslandPlace.INVENTORY -> nav.open(IslandSub.INVENTORY)
                IslandPlace.HOUSE -> nav.open(IslandSub.HOUSE)
                IslandPlace.PHOTO -> nav.open(IslandSub.PHOTO_ISLAND)
            }
        })
        route == Route.MY_INFO -> MyInfoScreen(onShare = nav.share)
        route == Route.ENCYCLOPEDIA -> EncyclopediaScreen(onShare = nav.share, onOpenFieldDrop = { nav.go(Route.FIELD_DROP) })
        route == Route.HOME -> HomeScreen(mode, nav.go)
        route == Route.FIELD_DROP -> FieldDropScreen()
        route == Route.SETTINGS ->
            SettingsScreen(onSwitchAccount = nav.signOut, onLogout = nav.signOut, onOpenShop = { nav.open(IslandSub.SHOP) })
    }
}
