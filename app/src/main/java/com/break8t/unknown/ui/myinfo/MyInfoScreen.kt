package com.break8t.unknown.ui.myinfo

import com.break8t.unknown.ui.components.ScreenTitle
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.break8t.unknown.ui.photo.ShareHandler
import com.break8t.unknown.R
import com.break8t.unknown.config.BalanceConfig
import com.break8t.unknown.data.LocalDemoState
import com.break8t.unknown.data.Profile
import com.break8t.unknown.ui.Route
import com.break8t.unknown.ui.collection.Art
import com.break8t.unknown.ui.collection.ArtShape
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.break8t.unknown.ui.collection.CollectionKind
import com.break8t.unknown.ui.collection.CollectionPane
import com.break8t.unknown.ui.collection.CollectionScreen
import com.break8t.unknown.ui.collection.label
import com.break8t.unknown.ui.collection.Collectible
import com.break8t.unknown.ui.collection.HeaderChip
import com.break8t.unknown.ui.collection.Overlay
import com.break8t.unknown.ui.collection.PreviewShell
import com.break8t.unknown.ui.collection.ScreenPadding
import com.break8t.unknown.ui.collection.ScreenPreviews
import com.break8t.unknown.ui.collection.Tile
import com.break8t.unknown.ui.collection.artFor
import com.break8t.unknown.ui.collection.collections
import com.break8t.unknown.ui.collection.newestOwned
import com.break8t.unknown.ui.components.SecondaryButton
import com.break8t.unknown.ui.theme.AppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update

private const val TICK_MS = 1_000L

private enum class Page { HOME, LIST, FRIENDS, ADD }

/** 내 정보 진입점(Figma S-20 ~ S-23). 공유는 콜백으로만 알린다. */
@Composable
fun MyInfoScreen(onShare: ShareHandler) {
    val demo = LocalDemoState.current
    val profile by demo.profile.collectAsState()
    val friends by FriendStore.friends.collectAsState()
    val config = BalanceConfig.DEFAULT
    var page by rememberSaveable { mutableStateOf(Page.HOME) }
    var listKind by rememberSaveable { mutableStateOf(CollectionKind.CARD) }
    var friendId by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var askPublic by rememberSaveable { mutableStateOf(false) }
    var addedId by rememberSaveable { mutableStateOf<String?>(null) }
    var input by rememberSaveable { mutableStateOf("") }
    var showWrong by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = page != Page.HOME) { page = if (page == Page.ADD) Page.FRIENDS else Page.HOME }

    when (page) {
        Page.HOME -> MyInfoHome(profile, friends.size, collections(profile), { page = Page.FRIENDS }) {
            listKind = it
            page = Page.LIST
        }
        Page.LIST -> CollectionScreen(listKind, { listKind = it }, loaded = true, showLocked = false, onShare, null) {
            HeaderChip(stringResource(R.string.myinfo_title), selected = false) { page = Page.HOME }
        }
        Page.FRIENDS -> Box(Modifier.fillMaxSize()) {
            val selected = friends.firstOrNull { it.id == friendId } ?: friends.firstOrNull()
            FriendsPane(
                friends, selected?.id, profile.drivingStatusPublic,
                onBack = { page = Page.HOME },
                onSelect = { friendId = it.id },
                onTogglePublic = { on -> if (on) askPublic = true else demo.update { it.copy(drivingStatusPublic = false) } },
                onAdd = { page = Page.ADD },
                onDelete = { confirmDelete = true },
            )
            if (confirmDelete && selected != null) {
                Overlay({ confirmDelete = false }) {
                    DeleteFriendDialog(selected, { confirmDelete = false }) {
                        FriendStore.friends.update { list -> list.filterNot { it.id == selected.id } }
                        friendId = null
                        confirmDelete = false
                    }
                }
            }
            if (askPublic) {
                Overlay({ askPublic = false }) {
                    PublicStatusDialog({ askPublic = false }) {
                        demo.update { it.copy(drivingStatusPublic = true) }
                        askPublic = false
                    }
                }
            }
        }
        Page.ADD -> Box(Modifier.fillMaxSize()) {
            var now by remember { mutableLongStateOf(System.currentTimeMillis() / 1000) }
            LaunchedEffect(Unit) { while (true) { delay(TICK_MS); now = System.currentTimeMillis() / 1000 } }
            val check = checkFriendCode(input, friends, config)
            FriendAddPane(
                input, check, showWrong, FriendCode.mine(now, config), FriendCode.secondsLeft(now, config), friends.size, config,
                onClose = { input = ""; showWrong = false; page = Page.FRIENDS },
                onDigit = { d -> if (input.length < FriendCode.LENGTH) input += d; showWrong = false },
                onBackspace = { input = input.dropLast(1); showWrong = false },
                onAdd = {
                    when (check) {
                        AddCheck.OK -> {
                            val f = demoNewFriend()
                            FriendStore.friends.update { it + f }
                            addedId = f.id
                            input = ""
                        }
                        AddCheck.WRONG -> showWrong = true
                        AddCheck.FULL, AddCheck.INCOMPLETE -> Unit
                    }
                },
            )
            friends.firstOrNull { it.id == addedId }?.let { f ->
                val done = { friendId = f.id; addedId = null; page = Page.FRIENDS }
                Overlay(done) { FriendAddedDialog(f, done) }
            }
        }
    }
}

/** S-20. 묶음 칸의 썸네일은 가장 최근에 얻은 것이다(팀 결정). */
@Composable
private fun MyInfoHome(
    profile: Profile,
    friendCount: Int,
    all: Map<CollectionKind, List<Collectible>>,
    onFriends: () -> Unit,
    onKind: (CollectionKind) -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(35.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ScreenTitle(stringResource(R.string.myinfo_title))
            Spacer(Modifier.weight(1f))
            SecondaryButton(stringResource(R.string.myinfo_friends_button, friendCount), onFriends)
        }
        ProfileCard(profile)
        BoxWithConstraints {
            // 넓은 창은 Figma처럼 다섯 칸 한 줄. 화면을 나누면 "정체 카드"가 한 줄에 들어가도록 두 칸씩.
            val cols = if (maxWidth >= 1100.dp) CollectionKind.entries.size else 2
            Column(verticalArrangement = Arrangement.spacedBy(26.dp)) {
                CollectionKind.entries.chunked(cols).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                        row.forEach { k -> KindTile(k, all[k].orEmpty(), Modifier.weight(1f)) { onKind(k) } }
                        repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileCard(profile: Profile) = BoxWithConstraints(
    Modifier.fillMaxWidth().clip(RoundedCornerShape(21.dp)).background(AppColors.Card)
        .border(3.dp, AppColors.Line, RoundedCornerShape(21.dp)).padding(36.dp),
) {
    val art = if (maxWidth >= 900.dp) 325.dp else 180.dp
    Row(horizontalArrangement = Arrangement.spacedBy(52.dp), verticalAlignment = Alignment.CenterVertically) {
        Art(artFor(profile.selectedCharacterId), profile.nickname, Modifier.size(art))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(profile.nickname, Modifier.weight(1f), color = AppColors.Text, fontSize = 62.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Box(
                    Modifier.clip(RoundedCornerShape(13.dp)).background(AppColors.Selected)
                        .border(3.dp, AppColors.Line, RoundedCornerShape(13.dp)).padding(horizontal = 21.dp, vertical = 8.dp),
                ) { Text(stringResource(R.string.myinfo_level, profile.level), color = AppColors.Text, fontSize = 39.sp, fontWeight = FontWeight.Bold) }
            }
            Text(profile.statusMessage, color = AppColors.TextSub, fontSize = 36.sp)
            XpBox(profile)
        }
    }
}

@Composable
private fun XpBox(profile: Profile) = Column(
    Modifier.padding(top = 20.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(AppColors.Background).padding(26.dp),
    verticalArrangement = Arrangement.spacedBy(26.dp),
) {
    Row {
        Text(stringResource(R.string.myinfo_xp), Modifier.weight(1f), color = AppColors.Text, fontSize = 36.sp, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.myinfo_xp_value, profile.xp, profile.xpToNext), color = AppColors.TextSub, fontSize = 36.sp)
    }
    val fraction = if (profile.xpToNext > 0) (profile.xp.toFloat() / profile.xpToNext).coerceIn(0f, 1f) else 0f
    Box(Modifier.fillMaxWidth().height(31.dp).clip(RoundedCornerShape(16.dp)).background(AppColors.Muted)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(fraction).background(AppColors.Success))
    }
}

@Composable
private fun KindTile(kind: CollectionKind, items: List<Collectible>, modifier: Modifier, onClick: () -> Unit) =
    Tile(onClick, modifier) {
        val newest = newestOwned(items)
        val crop = kind == CollectionKind.CARD || kind == CollectionKind.POSTCARD
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(ArtShape).background(if (newest == null) AppColors.Muted else AppColors.Card)) {
            if (newest != null) Art(newest.art, newest.name, Modifier.fillMaxSize(), if (crop) ContentScale.Crop else ContentScale.Fit)
        }
        Text(
            stringResource(kind.label()), Modifier.fillMaxWidth().padding(top = 26.dp),
            color = AppColors.Text, fontSize = 44.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1,
        )
        Text(
            stringResource(R.string.myinfo_count, items.count { it.owned }), Modifier.fillMaxWidth().heightIn(min = 48.dp),
            color = AppColors.TextSub, fontSize = 36.sp, textAlign = TextAlign.Center,
        )
    }

// Previews: Figma S-20 and S-21-a..e.

@ScreenPreviews
@Composable
private fun MyInfoHomePreview() = PreviewShell(Route.MY_INFO) {
    MyInfoHome(Profile(), demoFriends.size, collections(Profile()), {}) {}
}

internal class MyInfoLists : PreviewParameterProvider<CollectionKind> {
    override val values = CollectionKind.entries.asSequence()
}

@ScreenPreviews
@Composable
private fun MyInfoListPreview(@PreviewParameter(MyInfoLists::class) kind: CollectionKind) = PreviewShell(Route.MY_INFO) {
    CollectionPane(collections(Profile()), kind, {}, true, false, Profile(), {
        HeaderChip(stringResource(R.string.myinfo_title), selected = false) {}
    }, null) {}
}
