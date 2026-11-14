package com.break8t.unknown.ui.myinfo

import com.break8t.unknown.ui.components.ScreenTitle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.break8t.unknown.R
import com.break8t.unknown.config.BalanceConfig
import com.break8t.unknown.ui.Route
import com.break8t.unknown.ui.collection.Art
import com.break8t.unknown.ui.collection.ArtShape
import com.break8t.unknown.ui.collection.DialogArt
import com.break8t.unknown.ui.collection.DialogBody
import com.break8t.unknown.ui.collection.DialogFrame
import com.break8t.unknown.ui.collection.DialogTitle
import com.break8t.unknown.ui.collection.HeaderChip
import com.break8t.unknown.ui.collection.Josa
import com.break8t.unknown.ui.collection.Overlay
import com.break8t.unknown.ui.collection.PreviewShell
import com.break8t.unknown.ui.collection.ScreenPadding
import com.break8t.unknown.ui.collection.ScreenPreviews
import com.break8t.unknown.ui.collection.TileShape
import com.break8t.unknown.ui.collection.artFor
import com.break8t.unknown.ui.components.PrimaryButton
import com.break8t.unknown.ui.components.SecondaryButton
import com.break8t.unknown.ui.components.StateButton
import com.break8t.unknown.ui.theme.AppColors

// Figma S-22, S-23, D-21, D-22, D-23. Sizes are Figma px x 1.3, see SideRail.kt.
private val ControlShape = RoundedCornerShape(16.dp)
private val PillShape = RoundedCornerShape(percent = 50)
private val WideFrom = 1100.dp

/** S-22. 넓은 창은 목록과 자세히를 나란히, 좁은 창은 자세히를 위에 둔다. */
@Composable
internal fun FriendsPane(
    friends: List<Friend>,
    selectedId: String?,
    drivingPublic: Boolean,
    onBack: () -> Unit,
    onSelect: (Friend) -> Unit,
    onTogglePublic: (Boolean) -> Unit,
    onAdd: () -> Unit,
    onDelete: () -> Unit,
    onVisit: (Friend) -> Unit = {},
    onCollection: (Friend) -> Unit = {},
) {
    val selected = friends.firstOrNull { it.id == selectedId } ?: friends.firstOrNull()
    Column(Modifier.fillMaxSize().padding(start = ScreenPadding, top = ScreenPadding, end = ScreenPadding)) {
        FlowRow(
            Modifier.fillMaxWidth().padding(bottom = 35.dp),
            horizontalArrangement = Arrangement.spacedBy(35.dp),
            verticalArrangement = Arrangement.spacedBy(21.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            HeaderChip(stringResource(R.string.myinfo_title), selected = false, onBack = onBack)
            Text(stringResource(R.string.myinfo_friends_title, friends.size), color = AppColors.Text, fontSize = 49.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                Text(stringResource(R.string.myinfo_driving_public), color = AppColors.Text, fontSize = 36.sp, fontWeight = FontWeight.Medium)
                StateButton(drivingPublic, onTogglePublic)
            }
            PrimaryButton(stringResource(R.string.myinfo_add_friend), onAdd)
        }
        if (selected == null) {
            Column(
                Modifier.fillMaxSize().padding(bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(48.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(R.string.myinfo_friends_empty), color = AppColors.Text, fontSize = 52.sp, fontWeight = FontWeight.Bold)
                PrimaryButton(stringResource(R.string.myinfo_add_friend), onAdd, Modifier.width(364.dp))
            }
        } else BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth >= WideFrom
            Row(horizontalArrangement = Arrangement.spacedBy(35.dp)) {
                LazyColumn(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(26.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = ScreenPadding),
                ) {
                    if (!wide) item { FriendDetail(selected, onDelete, onVisit, onCollection) }
                    items(friends, key = { it.id }) { f -> FriendRow(f, f.id == selected.id, chipBelow = !wide) { onSelect(f) } }
                }
                if (wide) FriendDetail(selected, onDelete, onVisit, onCollection, Modifier.width(550.dp))
            }
        }
    }
}

@Composable
private fun Avatar(friend: Friend, size: androidx.compose.ui.unit.Dp) =
    Box(Modifier.size(size).clip(ArtShape).background(AppColors.Accent).border(3.dp, AppColors.Line, ArtShape), Alignment.Center) {
        Art(artFor(friend.characterId), friend.nickname, Modifier.size(size - 8.dp))
    }

@Composable
private fun DrivingChip() = Row(
    Modifier.clip(PillShape).background(AppColors.SuccessBg).padding(horizontal = 21.dp, vertical = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    Icon(painterResource(R.drawable.myinfo_ic_wheel), null, Modifier.size(34.dp), tint = AppColors.Success)
    Text(stringResource(R.string.myinfo_driving), color = AppColors.Success, fontSize = 31.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun FriendRow(friend: Friend, selected: Boolean, chipBelow: Boolean, onClick: () -> Unit) = Row(
    Modifier
        .fillMaxWidth()
        .clip(TileShape)
        .background(AppColors.Card)
        .border(if (selected) 6.5.dp else 3.dp, if (selected) AppColors.Selected else AppColors.Line, TileShape)
        .clickable(role = Role.Button, onClick = onClick)
        .padding(26.dp),
    horizontalArrangement = Arrangement.spacedBy(26.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    Avatar(friend, 99.dp)
    // 좁은 창에서는 칩을 이름 아래로 내려야 이름이 잘리지 않는다.
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(friend.nickname, color = AppColors.Text, fontSize = 44.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(friend.status, color = AppColors.TextSub, fontSize = 31.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (friend.driving && chipBelow) DrivingChip()
    }
    if (friend.driving && !chipBelow) DrivingChip()
    Icon(painterResource(R.drawable.myinfo_ic_next), null, Modifier.size(47.dp), tint = AppColors.Text)
}

@Composable
private fun FriendDetail(
    friend: Friend,
    onDelete: () -> Unit,
    onVisit: (Friend) -> Unit,
    onCollection: (Friend) -> Unit,
    modifier: Modifier = Modifier,
) = Column(
    modifier
        .clip(TileShape)
        .background(AppColors.Card)
        .border(3.dp, AppColors.Line, TileShape)
        .padding(31.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
) {
    Row(horizontalArrangement = Arrangement.spacedBy(26.dp)) {
        Avatar(friend, 174.dp)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(friend.nickname, color = AppColors.Text, fontSize = 52.sp, fontWeight = FontWeight.Bold)
            Text(friend.status, color = AppColors.TextSub, fontSize = 31.sp)
            if (friend.driving) DrivingChip()
        }
    }
    Box(Modifier.fillMaxWidth().padding(vertical = 10.dp).height(3.dp).background(AppColors.Muted))
    Stat(R.string.myinfo_friend_jams, stringResource(R.string.myinfo_friend_jams_value, friend.jamsThisWeek))
    Stat(R.string.myinfo_friend_postcard, friend.lastPostcard)
    Stat(R.string.myinfo_friend_since, friend.since)
    Spacer(Modifier.height(16.dp))
    PrimaryButton(stringResource(R.string.myinfo_visit_island), { onVisit(friend) }, Modifier.fillMaxWidth())
    SecondaryButton(stringResource(R.string.myinfo_friend_collection), { onCollection(friend) }, Modifier.fillMaxWidth())
    DangerButton(stringResource(R.string.myinfo_delete_friend), onDelete, Modifier.fillMaxWidth())
}

@Composable
private fun ColumnScope.Stat(label: Int, value: String) {
    Text(stringResource(label), color = AppColors.TextSub, fontSize = 31.sp)
    Text(value, color = AppColors.Text, fontSize = 42.sp, fontWeight = FontWeight.Bold)
}

/** Figma "Button/친구 삭제": 빨간 테두리와 글자. 공용 버튼에는 이 모양이 없다. */
@Composable
private fun DangerButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) = Box(
    modifier
        .heightIn(min = 100.dp)
        .clip(ControlShape)
        .background(AppColors.Card)
        .border(3.dp, AppColors.Error, ControlShape)
        .clickable(role = Role.Button, onClick = onClick)
        .padding(horizontal = 26.dp, vertical = 12.dp),
    contentAlignment = Alignment.Center,
) { Text(text, color = AppColors.Error, fontSize = 44.sp, fontWeight = FontWeight.Bold) }

/** S-23. [check]가 WRONG이면 칸을 빨갛게(S-23-b), FULL이면 숫자판을 막는다(S-23-c). */
@Composable
internal fun FriendAddPane(
    input: String,
    check: AddCheck,
    showWrong: Boolean,
    myCode: String,
    secondsLeft: Int,
    friendCount: Int,
    config: BalanceConfig,
    onClose: () -> Unit,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onAdd: () -> Unit,
) {
    val full = check == AddCheck.FULL
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(ScreenPadding)) {
        FlowRow(
            Modifier.fillMaxWidth().padding(bottom = 35.dp),
            horizontalArrangement = Arrangement.spacedBy(35.dp),
            verticalArrangement = Arrangement.spacedBy(21.dp),
        ) {
            HeaderChip(stringResource(R.string.myinfo_close), selected = false, onBack = onClose)
            ScreenTitle(stringResource(R.string.myinfo_add_friend))
            HeaderChip(stringResource(R.string.myinfo_friend_limit, friendCount, config.maxFriends), selected = false)
        }
        BoxWithConstraints {
            val wide = maxWidth >= WideFrom
            val myCard = @Composable { m: Modifier -> MyCodeCard(myCode, secondsLeft, m) }
            val entry = @Composable { m: Modifier -> CodeEntry(input, showWrong, full, config, onDigit, onBackspace, onAdd, m) }
            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(35.dp)) {
                    myCard(Modifier.width(624.dp))
                    entry(Modifier.weight(1f))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(35.dp)) {
                    myCard(Modifier.fillMaxWidth())
                    entry(Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun CardColumn(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) = Column(
    modifier.clip(TileShape).background(AppColors.Card).border(3.dp, AppColors.Line, TileShape).padding(horizontal = 36.dp, vertical = 31.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(31.dp),
    content = content,
)

@Composable
private fun MyCodeCard(code: String, secondsLeft: Int, modifier: Modifier) = CardColumn(modifier) {
    Text(stringResource(R.string.myinfo_my_code), color = AppColors.Text, fontSize = 39.sp, fontWeight = FontWeight.Bold)
    Box(Modifier.fillMaxWidth().heightIn(min = 169.dp).clip(ArtShape).background(AppColors.Accent), Alignment.Center) {
        Text(FriendCode.spaced(code), color = AppColors.Text, fontSize = 109.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
    Text(
        stringResource(R.string.myinfo_code_expires, secondsLeft / 60, secondsLeft % 60),
        color = AppColors.TextSub, fontSize = 31.sp, textAlign = TextAlign.Center,
    )
}

@Composable
private fun CodeEntry(
    input: String,
    wrong: Boolean,
    full: Boolean,
    config: BalanceConfig,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier,
) = CardColumn(modifier) {
    Text(stringResource(R.string.myinfo_enter_code), color = AppColors.Text, fontSize = 39.sp, fontWeight = FontWeight.Bold)
    // 좁은 창(화면 나눔)에서도 여섯 칸이 들어가도록 칸 너비를 나눠 갖는다.
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(FriendCode.LENGTH) { i ->
            if (i == FriendCode.LENGTH / 2) Spacer(Modifier.width(31.dp))
            val active = !full && !wrong && i == input.length
            val border = when {
                wrong -> AppColors.Error
                active -> AppColors.Selected
                else -> AppColors.Line
            }
            Box(
                Modifier.weight(1f).aspectRatio(91f / 114f).clip(RoundedCornerShape(13.dp)).background(AppColors.Card)
                    .border(if (active) 6.5.dp else 3.dp, border, RoundedCornerShape(13.dp)),
                Alignment.Center,
            ) { Text(input.getOrNull(i)?.toString().orEmpty(), color = AppColors.Text, fontSize = 62.sp, fontWeight = FontWeight.Bold) }
        }
    }
    // 문구가 없을 때도 같은 글꼴의 한 줄을 투명하게 그려 둔다. 빈 줄은 한글보다 낮아서 오류가 뜰 때 숫자판이 밀린다.
    val message = when {
        full -> stringResource(R.string.myinfo_code_full, config.maxFriends)
        else -> stringResource(R.string.myinfo_code_wrong)
    }
    Text(
        message,
        Modifier.fillMaxWidth().alpha(if (full || wrong) 1f else 0f),
        color = AppColors.Error,
        fontSize = 29.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        softWrap = false,
    )
    val keys = listOf("123", "456", "789", " 0<")
    Column(verticalArrangement = Arrangement.spacedBy(21.dp)) {
        keys.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(21.dp)) {
                row.forEach { k ->
                    val m = Modifier.weight(1f)
                    when (k) {
                        ' ' -> Spacer(m)
                        '<' -> Key(m, !full, onBackspace) {
                            Icon(painterResource(R.drawable.myinfo_ic_del), stringResource(R.string.myinfo_backspace), Modifier.size(52.dp), tint = AppColors.Text)
                        }
                        else -> Key(m, !full, { onDigit(k) }) {
                            Text(k.toString(), color = AppColors.Text, fontSize = 52.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
    val ready = !full && input.length == FriendCode.LENGTH
    Box(
        Modifier.fillMaxWidth().heightIn(min = 100.dp).clip(ControlShape)
            .background(if (ready) AppColors.Primary else AppColors.Disabled)
            .clickable(enabled = ready, role = Role.Button, onClick = onAdd),
        Alignment.Center,
    ) {
        Text(
            stringResource(R.string.myinfo_add),
            color = if (ready) AppColors.Card else AppColors.DisabledText, fontSize = 44.sp, fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun Key(modifier: Modifier, enabled: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) = Box(
    modifier
        .heightIn(min = 109.dp)
        .alpha(if (enabled) 1f else 0.4f)
        .clip(ControlShape)
        .background(AppColors.Card)
        .border(3.dp, AppColors.Line, ControlShape)
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
    contentAlignment = Alignment.Center,
) { content() }

/** D-21 */
@Composable
internal fun DeleteFriendDialog(friend: Friend, onCancel: () -> Unit, onDelete: () -> Unit) = DialogFrame(936.dp) {
    DialogTitle(stringResource(R.string.myinfo_delete_title, Josa.eulReul(friend.nickname)))
    DialogBody(stringResource(R.string.myinfo_delete_body))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(35.dp)) {
        SecondaryButton(stringResource(R.string.components_cancel), onCancel, Modifier.weight(1f))
        DangerButton(stringResource(R.string.myinfo_delete_confirm), onDelete, Modifier.weight(1f))
    }
}

/** D-22. 켤 때만 묻고, 끌 때는 묻지 않는다. */
@Composable
internal fun PublicStatusDialog(onCancel: () -> Unit, onConfirm: () -> Unit) = DialogFrame(1040.dp) {
    DialogTitle(stringResource(R.string.myinfo_public_title))
    DialogBody(stringResource(R.string.myinfo_public_body))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(35.dp)) {
        SecondaryButton(stringResource(R.string.components_cancel), onCancel, Modifier.weight(1f))
        PrimaryButton(stringResource(R.string.myinfo_public_confirm), onConfirm, Modifier.weight(1f))
    }
}

/** D-23 */
@Composable
internal fun FriendAddedDialog(friend: Friend, onOk: () -> Unit) = DialogFrame(832.dp) {
    DialogArt(artFor(friend.characterId), friend.nickname)
    DialogTitle(stringResource(R.string.myinfo_friend_added, Josa.waGwa(friend.nickname)))
    PrimaryButton(stringResource(R.string.myinfo_ok), onOk, Modifier.fillMaxWidth())
}

// Previews: Figma S-22-a/b, D-21, D-22, S-23-a/b/c, D-23.

@ScreenPreviews
@Composable
private fun FriendsPreview() = PreviewShell(Route.MY_INFO) {
    FriendsPane(demoFriends, null, true, {}, {}, {}, {}, {})
}

@ScreenPreviews
@Composable
private fun FriendsEmptyPreview() = PreviewShell(Route.MY_INFO) {
    FriendsPane(emptyList(), null, true, {}, {}, {}, {}, {})
}

@ScreenPreviews
@Composable
private fun DeleteFriendPreview() = PreviewShell(Route.MY_INFO) {
    FriendsPane(demoFriends, null, true, {}, {}, {}, {}, {})
    Overlay({}) { DeleteFriendDialog(demoFriends.first(), {}, {}) }
}

@ScreenPreviews
@Composable
private fun PublicStatusPreview() = PreviewShell(Route.MY_INFO) {
    FriendsPane(demoFriends, null, false, {}, {}, {}, {}, {})
    Overlay({}) { PublicStatusDialog({}, {}) }
}

@ScreenPreviews
@Composable
private fun FriendAddPreview() = PreviewShell(Route.MY_INFO) {
    FriendAddPane("482", AddCheck.INCOMPLETE, false, "482913", 252, 5, BalanceConfig.DEFAULT, {}, {}, {}, {})
}

@ScreenPreviews
@Composable
private fun FriendAddWrongPreview() = PreviewShell(Route.MY_INFO) {
    FriendAddPane("111111", AddCheck.WRONG, true, "482913", 252, 5, BalanceConfig.DEFAULT, {}, {}, {}, {})
}

@ScreenPreviews
@Composable
private fun FriendAddFullPreview() = PreviewShell(Route.MY_INFO) {
    FriendAddPane("", AddCheck.FULL, false, "482913", 252, 30, BalanceConfig.DEFAULT, {}, {}, {}, {})
}

@ScreenPreviews
@Composable
private fun FriendAddedPreview() = PreviewShell(Route.MY_INFO) {
    FriendAddPane(FriendCode.DEMO, AddCheck.OK, false, "482913", 252, 6, BalanceConfig.DEFAULT, {}, {}, {}, {})
    Overlay({}) { FriendAddedDialog(demoNewFriend(), {}) }
}
