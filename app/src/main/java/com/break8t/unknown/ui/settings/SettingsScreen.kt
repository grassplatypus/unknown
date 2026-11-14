package com.break8t.unknown.ui.settings

import com.break8t.unknown.ui.components.ScreenTitle
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.break8t.unknown.R
import com.break8t.unknown.config.BalanceConfig
import com.break8t.unknown.config.Catalog
import com.break8t.unknown.config.CatalogItem
import com.break8t.unknown.data.DemoState
import com.break8t.unknown.data.LocalDemoState
import com.break8t.unknown.data.Profile
import com.break8t.unknown.drive.LocalUxLimits
import com.break8t.unknown.drive.UxLimits
import com.break8t.unknown.ui.Route
import com.break8t.unknown.ui.components.AppCard
import com.break8t.unknown.ui.components.AppChip
import com.break8t.unknown.ui.components.AppDialog
import com.break8t.unknown.ui.components.AppToast
import com.break8t.unknown.ui.components.BorderWidth
import com.break8t.unknown.ui.components.ControlShape
import com.break8t.unknown.ui.components.DialogPanel
import com.break8t.unknown.ui.components.PrimaryButton
import com.break8t.unknown.ui.components.SecondaryButton
import com.break8t.unknown.ui.components.SideRail
import com.break8t.unknown.ui.components.StateButton
import com.break8t.unknown.ui.theme.AppColors
import com.break8t.unknown.ui.theme.AppTheme
import com.break8t.unknown.voice.AssetVoicePlayer
import com.break8t.unknown.voice.VoiceLine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

// Figma "8. 설정" S-25 ~ S-28, D-27, T-11 ~ T-13. Sizes are Figma px x 1.3.
private val RowShape = RoundedCornerShape(21.dp)
private val ArtShape = RoundedCornerShape(16.dp)
private const val TOAST_MS = 2_500L

private enum class Tab(@StringRes val label: Int) {
    SOUND(R.string.settings_tab_sound),
    DISPLAY(R.string.settings_tab_display),
    ACCOUNT(R.string.settings_tab_account),
}

private enum class Page { MAIN, STATUS, NICKNAME, VOICE }

private enum class Ask { NONE, LOGOUT, DRIVING_PUBLIC }

/** 설정 진입점. 다른 묶음으로 가는 일은 콜백으로만 알린다. */
@Composable
fun SettingsScreen(onSwitchAccount: () -> Unit, onLogout: () -> Unit, onOpenShop: () -> Unit) =
    SettingsContent(onSwitchAccount, onLogout, onOpenShop)

@Composable
private fun SettingsContent(
    onSwitchAccount: () -> Unit,
    onLogout: () -> Unit,
    onOpenShop: () -> Unit,
    initialTab: Tab = Tab.SOUND,
    initialPage: Page = Page.MAIN,
    initialToast: String? = null,
) {
    val demo = LocalDemoState.current
    val p by demo.profile.collectAsState()
    val cfg = BalanceConfig.DEFAULT
    var tab by rememberSaveable { mutableStateOf(initialTab) }
    var page by rememberSaveable { mutableStateOf(initialPage) }
    var ask by rememberSaveable { mutableStateOf(Ask.NONE) }
    var toast by rememberSaveable { mutableStateOf(initialToast) }
    LaunchedEffect(toast) {
        if (toast != null) {
            delay(TOAST_MS)
            toast = null
        }
    }
    BackHandler(page != Page.MAIN) { page = Page.MAIN }
    val back = { page = Page.MAIN }
    // 플랫폼이 주행 중 설정을 막으면(NO_SETUP) 열어 둔 하위 화면과 확인 창을 그 자리에서 닫는다.
    val locked = LocalUxLimits.current.noSetup
    LaunchedEffect(locked) {
        if (locked) {
            page = Page.MAIN
            ask = Ask.NONE
        }
    }

    val statusSaved = stringResource(R.string.settings_status_saved)
    val nicknameSaved = stringResource(R.string.settings_nickname_saved)
    val voiceChanged = stringResource(R.string.settings_voice_changed)

    // imePadding here, not per page, so the save toast also stays above the keyboard.
    Box(Modifier.fillMaxSize().imePadding()) {
        when (page) {
            Page.MAIN -> MainPage(
                p,
                tab,
                onTab = { tab = it },
                onOpen = { page = it },
                onDrivingPublic = { on ->
                    if (on) ask = Ask.DRIVING_PUBLIC else demo.update { it.copy(drivingStatusPublic = false) }
                },
                onSwitchAccount = onSwitchAccount,
                onAskLogout = { ask = Ask.LOGOUT },
            )
            Page.STATUS -> EditPage(
                title = R.string.settings_status_message,
                initial = p.statusMessage,
                max = cfg.statusMessageMax,
                characterId = p.selectedCharacterId,
                preview = { draft -> p.nickname to draft },
                toSave = { statusMessageToSave(it, cfg.statusMessageMax) },
                onSave = { v ->
                    demo.update { it.copy(statusMessage = v) }
                    toast = statusSaved
                },
                onBack = back,
            )
            Page.NICKNAME -> EditPage(
                title = R.string.settings_nickname,
                initial = p.nickname,
                max = cfg.nicknameMax,
                characterId = p.selectedCharacterId,
                preview = { draft -> draft to p.statusMessage },
                toSave = { nicknameToSave(it, cfg.nicknameMax) },
                onSave = { v ->
                    demo.update { it.copy(nickname = v) }
                    toast = nicknameSaved
                },
                onBack = back,
            )
            Page.VOICE -> VoicePage(
                packs = ownedVoicePacks(p.owned),
                selectedId = p.voicePackId,
                onSelect = { pack ->
                    if (pack.id != p.voicePackId) {
                        demo.update { it.copy(voicePackId = pack.id) }
                        toast = voiceChanged.format(withRo(pack.name))
                    }
                },
                onOpenShop = onOpenShop,
                onBack = back,
            )
        }
        toast?.let { AppToast(it, Modifier.align(Alignment.BottomCenter).padding(bottom = 35.dp)) }
    }

    when (ask) {
        Ask.NONE -> Unit
        Ask.LOGOUT -> AppDialog(
            title = stringResource(R.string.settings_logout_title),
            body = stringResource(R.string.settings_logout_body),
            confirmText = stringResource(R.string.settings_logout),
            onConfirm = {
                ask = Ask.NONE
                onLogout()
            },
            onDismiss = { ask = Ask.NONE },
        )
        Ask.DRIVING_PUBLIC -> AppDialog(
            title = stringResource(R.string.settings_driving_public_title),
            body = stringResource(R.string.settings_driving_public_body),
            confirmText = stringResource(R.string.settings_driving_public_confirm),
            onConfirm = {
                ask = Ask.NONE
                demo.update { it.copy(drivingStatusPublic = true) }
            },
            onDismiss = { ask = Ask.NONE },
        )
    }
}

@Composable
private fun PageColumn(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(35.dp),
        verticalArrangement = Arrangement.spacedBy(21.dp),
    ) { content() }
}

@Composable
private fun MainPage(
    p: Profile,
    tab: Tab,
    onTab: (Tab) -> Unit,
    onOpen: (Page) -> Unit,
    onDrivingPublic: (Boolean) -> Unit,
    onSwitchAccount: () -> Unit,
    onAskLogout: () -> Unit,
) = PageColumn {
    val demo = LocalDemoState.current
    val locked = LocalUxLimits.current.noSetup
    FlowRow(horizontalArrangement = Arrangement.spacedBy(21.dp), verticalArrangement = Arrangement.spacedBy(21.dp)) {
        ScreenTitle(stringResource(R.string.route_settings), Modifier.padding(end = 31.dp))
        Tab.entries.forEach { t -> AppChip(stringResource(t.label), t == tab, onClick = { onTab(t) }) }
    }
    if (locked) Text(stringResource(R.string.settings_driving_locked), color = AppColors.TextSub, fontSize = 34.sp)
    val change = stringResource(R.string.settings_change)
    val wide = Modifier.widthIn(min = 286.dp)
    // 주행 중에는 음성 안내 켜기와 끄기만 남기고, 나머지 버튼은 같은 글자의 꺼진 버튼으로 바꾼다.
    val button = @Composable { text: String, onClick: () -> Unit ->
        if (locked) DisabledPill(text, wide) else SecondaryButton(text, onClick, wide)
    }
    val toggle = @Composable { on: Boolean, onChange: (Boolean) -> Unit ->
        if (locked) DisabledPill(stringResource(if (on) R.string.components_on else R.string.components_off), wide)
        else StateButton(on, onChange)
    }
    when (tab) {
        Tab.SOUND -> {
            SettingRow(stringResource(R.string.settings_voice_guide)) {
                StateButton(p.voiceGuideOn, { on -> demo.update { it.copy(voiceGuideOn = on) } })
            }
            SettingRow(stringResource(R.string.settings_voice_pack), Catalog.item(p.voicePackId)?.name) {
                button(change) { onOpen(Page.VOICE) }
            }
        }
        Tab.DISPLAY -> {
            SettingRow(stringResource(R.string.settings_island_real_time)) {
                toggle(p.islandRealTime) { on -> demo.update { it.copy(islandRealTime = on) } }
            }
            SettingRow(stringResource(R.string.settings_island_time)) {
                Row(horizontalArrangement = Arrangement.spacedBy(21.dp)) {
                    TimeChip(R.string.settings_day, selected = !p.islandNight, enabled = !locked && islandTimeChoiceEnabled(p.islandRealTime)) {
                        demo.update { it.copy(islandNight = false) }
                    }
                    TimeChip(R.string.settings_night, selected = p.islandNight, enabled = !locked && islandTimeChoiceEnabled(p.islandRealTime)) {
                        demo.update { it.copy(islandNight = true) }
                    }
                }
            }
        }
        Tab.ACCOUNT -> {
            SettingRow(
                p.nickname,
                stringResource(R.string.settings_no_pin),
                lead = { Art(p.selectedCharacterId, 99.dp, bordered = true) },
            ) {
                button(stringResource(R.string.settings_switch_account), onSwitchAccount)
            }
            SettingRow(stringResource(R.string.settings_nickname), p.nickname) {
                button(change) { onOpen(Page.NICKNAME) }
            }
            SettingRow(stringResource(R.string.settings_status_message), p.statusMessage) {
                button(change) { onOpen(Page.STATUS) }
            }
            SettingRow(stringResource(R.string.settings_driving_public)) {
                toggle(p.drivingStatusPublic, onDrivingPublic)
            }
            SettingRow(stringResource(R.string.settings_logout_row)) {
                if (locked) DisabledPill(stringResource(R.string.settings_logout), wide)
                else Pill(
                    stringResource(R.string.settings_logout),
                    wide,
                    textColor = AppColors.Error,
                    line = AppColors.Error,
                    onClick = onAskLogout,
                )
            }
        }
    }
}

@Composable
private fun TimeChip(@StringRes label: Int, selected: Boolean, enabled: Boolean, onClick: () -> Unit) =
    if (enabled) {
        AppChip(stringResource(label), selected, onClick)
    } else {
        Pill(stringResource(label), fill = AppColors.Disabled, textColor = AppColors.DisabledText, line = null, fontSize = 39.sp, disabled = true)
    }

/** 카드 한 줄. 화면을 나눠 좁으면 버튼을 글 아래로 내린다. */
@Composable
private fun SettingRow(
    title: String,
    subtitle: String? = null,
    lead: (@Composable () -> Unit)? = null,
    trailing: @Composable () -> Unit,
) {
    AppCard(Modifier.fillMaxWidth()) {
        BoxWithConstraints(Modifier.heightIn(min = 138.dp).padding(horizontal = 29.dp, vertical = 17.dp), contentAlignment = Alignment.CenterStart) {
            val label = @Composable {
                Row(horizontalArrangement = Arrangement.spacedBy(26.dp), verticalAlignment = Alignment.CenterVertically) {
                    lead?.invoke()
                    Column {
                        Text(title, color = AppColors.Text, fontSize = 44.sp, fontWeight = FontWeight.Bold)
                        subtitle?.let { Text(it, color = AppColors.TextSub, fontSize = 31.sp) }
                    }
                }
            }
            if (maxWidth < 760.dp) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(17.dp)) {
                    label()
                    Box(Modifier.align(Alignment.End)) { trailing() }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                    Box(Modifier.weight(1f)) { label() }
                    trailing()
                }
            }
        }
    }
}

/** 상태 메시지(S-26)와 닉네임(S-27). 위 카드는 저장 전 입력을 그대로 보여 준다. */
@Composable
private fun EditPage(
    @StringRes title: Int,
    initial: String,
    max: Int,
    characterId: String,
    preview: (draft: String) -> Pair<String, String>,
    toSave: (draft: String) -> String?,
    onSave: (String) -> Unit,
    onBack: () -> Unit,
) = PageColumn {
    val state = rememberTextFieldState(initial)
    val noKeyboard = LocalUxLimits.current.noKeyboard
    val focus = LocalFocusManager.current
    // 주행 규제가 걸리는 순간 열려 있던 키보드를 닫는다.
    LaunchedEffect(noKeyboard) { if (noKeyboard) focus.clearFocus(force = true) }
    val draft = state.text.toString()

    SubHeader(title, onBack)
    val (name, sub) = preview(draft)
    AppCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.heightIn(min = 151.dp).padding(horizontal = 23.dp, vertical = 17.dp),
            horizontalArrangement = Arrangement.spacedBy(26.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Art(characterId, 99.dp, bordered = true)
            Column {
                Text(name, color = AppColors.Text, fontSize = 44.sp, fontWeight = FontWeight.Bold)
                Text(sub, color = AppColors.TextSub, fontSize = 31.sp)
            }
        }
    }
    Spacer(Modifier.height(18.dp))
    BasicTextField(
        state = state,
        modifier = Modifier.widthIn(max = 1273.dp).fillMaxWidth(),
        readOnly = noKeyboard,
        inputTransformation = InputTransformation.maxLength(max),
        textStyle = TextStyle(color = AppColors.Text, fontSize = 47.sp, fontWeight = FontWeight.Medium),
        lineLimits = TextFieldLineLimits.SingleLine,
        cursorBrush = SolidColor(AppColors.Text),
        decorator = { inner ->
            Row(
                Modifier
                    .heightIn(min = 125.dp)
                    .clip(ArtShape)
                    .background(AppColors.Card)
                    .border(7.dp, if (noKeyboard) AppColors.Disabled else AppColors.Selected, ArtShape)
                    .padding(horizontal = 25.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(26.dp),
            ) {
                Box(Modifier.weight(1f)) { inner() }
                Text(stringResource(R.string.settings_counter, draft.length, max), color = AppColors.TextSub, fontSize = 31.sp)
            }
        },
    )
    if (noKeyboard) Text(stringResource(R.string.settings_park_to_type), color = AppColors.TextSub, fontSize = 31.sp)
    Spacer(Modifier.height(41.dp))
    Row(Modifier.widthIn(max = 815.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(35.dp)) {
        val save = toSave(draft)
        if (save != null) {
            PrimaryButton(stringResource(R.string.settings_save), {
                state.setTextAndPlaceCursorAtEnd(save)
                onSave(save)
            }, Modifier.weight(1f))
        } else {
            DisabledPill(stringResource(R.string.settings_save), Modifier.weight(1f))
        }
        if (noKeyboard) {
            DisabledPill(stringResource(R.string.settings_clear), Modifier.weight(1f))
        } else {
            SecondaryButton(stringResource(R.string.settings_clear), { state.clearText() }, Modifier.weight(1f))
        }
    }
}

/** 음성팩 고르기(S-28). 줄을 누르면 고르고, 재생 버튼은 미리 듣기만 한다. */
@Composable
private fun VoicePage(
    packs: List<CatalogItem>,
    selectedId: String,
    onSelect: (CatalogItem) -> Unit,
    onOpenShop: () -> Unit,
    onBack: () -> Unit,
) = PageColumn {
    val context = LocalContext.current
    val inPreview = LocalInspectionMode.current
    val packFolder = remember { MutableStateFlow("") }
    val player = remember { if (inPreview) null else AssetVoicePlayer(context, packFolder, MutableStateFlow(true)) }
    DisposableEffect(player) { onDispose { player?.release() } }

    SubHeader(R.string.settings_voice_pack, onBack)
    Spacer(Modifier.height(92.dp))
    packs.forEach { pack ->
        VoicePackRow(pack, pack.id == selectedId, onSelect = { onSelect(pack) }) {
            packFolder.value = voiceAssetFolder(pack.id)
            // 미리 듣기용 대사는 아무 줄이나 괜찮다. 보상 안내가 가장 자주 듣는 줄이다.
            player?.play(VoiceLine.REWARD_READY)
        }
    }
    Spacer(Modifier.height(10.dp))
    SecondaryButton(stringResource(R.string.settings_more_in_shop), onOpenShop, Modifier.widthIn(min = 468.dp))
}

@Composable
private fun VoicePackRow(pack: CatalogItem, selected: Boolean, onSelect: () -> Unit, onPreview: () -> Unit) = BoxWithConstraints {
    // 화면을 나눠 좁으면 "사용 중"을 이름 아래로 내려서 이름이 한 글자씩 꺾이지 않게 한다.
    val narrow = maxWidth < 760.dp
    val inUse = @Composable { Text(stringResource(R.string.settings_in_use), color = AppColors.Success, fontSize = 34.sp, fontWeight = FontWeight.Bold) }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 161.dp)
            .clip(RowShape)
            .background(AppColors.Card)
            .border(if (selected) 7.dp else BorderWidth, if (selected) AppColors.Selected else AppColors.Line, RowShape)
            .selectable(selected, role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(26.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Art(pack.id, 109.dp, bordered = false)
        Box(
            Modifier
                .size(109.dp)
                .clip(ControlShape)
                .background(AppColors.Card)
                .border(BorderWidth, AppColors.Line, ControlShape)
                .clickable(role = Role.Button, onClickLabel = stringResource(R.string.settings_preview_voice), onClick = onPreview),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.settings_ic_play), stringResource(R.string.settings_preview_voice), Modifier.size(52.dp), tint = AppColors.Text)
        }
        Column(Modifier.weight(1f)) {
            Text(pack.name, color = AppColors.Text, fontSize = 42.sp, fontWeight = FontWeight.Bold)
            if (pack.id == "voice_default") {
                Text(stringResource(R.string.settings_voice_default_desc), color = AppColors.TextSub, fontSize = 31.sp)
            }
            if (selected && narrow) inUse()
        }
        if (selected && !narrow) inUse()
        Box(
            Modifier.size(57.dp).clip(CircleShape).background(AppColors.Card).border(4.dp, AppColors.Primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Box(Modifier.size(31.dp).clip(CircleShape).background(AppColors.Primary))
        }
    }
}

@Composable
private fun SubHeader(@StringRes title: Int, onBack: () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(35.dp), verticalArrangement = Arrangement.spacedBy(21.dp)) {
        Pill(stringResource(R.string.route_settings), icon = R.drawable.settings_ic_back, onClickLabel = stringResource(R.string.settings_back), onClick = onBack)
        Pill(stringResource(title), fill = AppColors.Selected)
    }
}

// ch_sprout 그림만 내보냈다. 나머지는 단색 상자와 이름 첫 글자로 자리를 잡는다.
@DrawableRes
private fun artFor(id: String): Int? = when (id) {
    "ch_sprout", "voice_default", "voice_sprout" -> R.drawable.settings_ch_sprout
    // 다른 캐릭터와 그 목소리는 도감의 캐릭터 그림을 쓴다. 없으면 첫 글자만 나왔다
    else -> com.break8t.unknown.ui.collection.artFor(Catalog.characterOf(id)?.id ?: id)
}

@Composable
private fun Art(id: String, size: Dp, bordered: Boolean) {
    val res = artFor(id)
    Box(
        Modifier
            .size(size)
            .clip(ArtShape)
            .background(AppColors.Accent)
            .then(if (bordered) Modifier.border(BorderWidth, AppColors.Line, ArtShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (res != null) {
            Image(painterResource(res), contentDescription = null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Text(Catalog.item(id)?.name?.take(1).orEmpty(), color = AppColors.Text, fontSize = 44.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** 공용 부품에 없는 모양(제목 칩, 아이콘 칩, 빨간 버튼, 꺼진 칩)을 한 곳에서 그린다. */
@Composable
private fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    fill: Color = AppColors.Card,
    textColor: Color = AppColors.Text,
    line: Color? = AppColors.Line,
    fontSize: TextUnit = 44.sp,
    @DrawableRes icon: Int? = null,
    disabled: Boolean = false,
    onClickLabel: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier
            .heightIn(min = 100.dp)
            .clip(ControlShape)
            .background(fill)
            .then(if (line != null) Modifier.border(BorderWidth, line, ControlShape) else Modifier)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClickLabel = onClickLabel, onClick = onClick) else Modifier)
            .then(if (disabled) Modifier.semantics { disabled() } else Modifier)
            .padding(horizontal = 31.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(13.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let { Icon(painterResource(it), contentDescription = null, Modifier.size(44.dp), tint = textColor) }
        Text(text, color = textColor, fontSize = fontSize, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DisabledPill(text: String, modifier: Modifier = Modifier) =
    Pill(text, modifier, AppColors.Disabled, AppColors.DisabledText, line = null, disabled = true)

// ---- Previews. Sample values below are preview-only. ----

@Preview(name = "기본 창", widthDp = 1682, heightDp = 1210)
@Preview(name = "화면 나눔", widthDp = 841, heightDp = 1210)
private annotation class SizePreviews

@Composable
private fun Frame(
    profile: Profile = Profile(),
    limits: UxLimits = UxLimits.NONE,
    dialog: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) = AppTheme {
    val demo = remember { DemoState(profile) }
    CompositionLocalProvider(LocalDemoState provides demo, LocalUxLimits provides limits) {
        Row(Modifier.fillMaxSize().background(AppColors.Background)) {
            SideRail(current = Route.SETTINGS, onSelect = {})
            Box(Modifier.weight(1f)) {
                content()
                dialog?.let {
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), Alignment.Center) { it() }
                }
            }
        }
    }
}

@Composable
private fun Screen(
    tab: Tab = Tab.SOUND,
    page: Page = Page.MAIN,
    toast: String? = null,
) = SettingsContent({}, {}, {}, tab, page, toast)

@SizePreviews @Composable private fun SoundTab() = Frame { Screen() }

@SizePreviews @Composable private fun DisplayTabRealTime() = Frame { Screen(Tab.DISPLAY) }

@SizePreviews @Composable
private fun DisplayTabManual() = Frame(Profile(islandRealTime = false, islandNight = true)) { Screen(Tab.DISPLAY) }

@SizePreviews @Composable private fun AccountTab() = Frame { Screen(Tab.ACCOUNT) }

@SizePreviews @Composable
private fun AccountTabDriving() = Frame(limits = UxLimits(true, UxLimits.NO_SETUP)) { Screen(Tab.ACCOUNT) }

@SizePreviews @Composable
private fun LogoutDialog() = Frame(dialog = {
    DialogPanel(
        stringResource(R.string.settings_logout_title),
        stringResource(R.string.settings_logout_body),
        stringResource(R.string.settings_logout),
        onConfirm = {},
        onDismiss = {},
    )
}) { Screen(Tab.ACCOUNT) }

@SizePreviews @Composable
private fun DrivingPublicDialog() = Frame(Profile(drivingStatusPublic = false), dialog = {
    DialogPanel(
        stringResource(R.string.settings_driving_public_title),
        stringResource(R.string.settings_driving_public_body),
        stringResource(R.string.settings_driving_public_confirm),
        onConfirm = {},
        onDismiss = {},
    )
}) { Screen(Tab.ACCOUNT) }

@SizePreviews @Composable
private fun StatusEdit() = Frame(Profile(statusMessage = "주말엔 강릉 가요")) { Screen(page = Page.STATUS) }

@SizePreviews @Composable
private fun StatusSaved() = Frame(Profile(statusMessage = "주말엔 강릉 가요")) {
    Screen(page = Page.STATUS, toast = stringResource(R.string.settings_status_saved))
}

@SizePreviews @Composable
private fun StatusNoKeyboard() = Frame(limits = UxLimits(true, UxLimits.NO_KEYBOARD)) { Screen(page = Page.STATUS) }

@SizePreviews @Composable private fun NicknameEdit() = Frame { Screen(page = Page.NICKNAME) }

@SizePreviews @Composable
private fun NicknameBlank() = Frame(Profile(nickname = " ")) { Screen(page = Page.NICKNAME) }

@SizePreviews @Composable
private fun NicknameSaved() = Frame { Screen(page = Page.NICKNAME, toast = stringResource(R.string.settings_nickname_saved)) }

// 가진 캐릭터(새싹이, 냥이)의 목소리가 함께 보이고, 상점에서 반딧이를 사면 반딧이 목소리가 더해진다.
@SizePreviews @Composable private fun VoicePacks() = Frame { Screen(page = Page.VOICE) }

@SizePreviews @Composable
private fun VoicePacksWithFirefly() = Frame(Profile(owned = Profile().owned + "ch_firefly")) { Screen(page = Page.VOICE) }

@SizePreviews @Composable
private fun VoicePackChanged() = Frame(Profile(voicePackId = "voice_sprout")) {
    Screen(page = Page.VOICE, toast = stringResource(R.string.settings_voice_changed, withRo("새싹이 목소리")))
}
