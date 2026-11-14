package com.break8t.unknown.ui.photo

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.break8t.unknown.R
import com.break8t.unknown.config.Catalog
import com.break8t.unknown.data.LocalDemoState
import com.break8t.unknown.ui.components.AppCard
import com.break8t.unknown.ui.components.BorderWidth
import com.break8t.unknown.ui.components.ControlShape
import com.break8t.unknown.ui.components.PrimaryButton
import com.break8t.unknown.ui.house.CameraPreset
import com.break8t.unknown.ui.house.ChipGap
import com.break8t.unknown.ui.house.HouseStore
import com.break8t.unknown.ui.house.PreviewFrame
import com.break8t.unknown.ui.house.PrimaryIconButton
import com.break8t.unknown.ui.house.RoomScene
import com.break8t.unknown.ui.house.RoomSceneState
import com.break8t.unknown.ui.house.ScenePlace
import com.break8t.unknown.ui.house.ScreenPad
import com.break8t.unknown.ui.house.SquareIconButton
import com.break8t.unknown.ui.house.TopChip
import com.break8t.unknown.ui.house.WindowPreviews
import com.break8t.unknown.ui.house.characterImage
import com.break8t.unknown.ui.house.presetLabel
import com.break8t.unknown.ui.house.trimToOwned
import com.break8t.unknown.ui.theme.AppColors
import java.time.LocalTime

private val PhotoStateSaver = listSaver<PhotoState, Any>({ it.toList() }, { PhotoState.fromList(it) })

/** 사진 찍기(S-16). 촬영하면 같은 장면으로 공유 화면(S-17-a)을 보이고, 다시 찍기로 돌아온다. */
@Composable
fun PhotoScreen(inHouse: Boolean, onClose: () -> Unit) = PhotoScreenImpl(inHouse, onClose)

@Composable
internal fun PhotoScreenImpl(inHouse: Boolean, onClose: () -> Unit, start: PhotoState = PhotoState(), night: Boolean? = null) {
    val profile by LocalDemoState.current.profile.collectAsState()
    val layout by HouseStore.layout.collectAsState()
    var s by rememberSaveable(stateSaver = PhotoStateSaver) { mutableStateOf(start) }
    val isNight = night ?: remember { isNight(profile.islandRealTime, profile.islandNight, LocalTime.now().hour) }
    val scene = RoomSceneState(
        place = if (inHouse) ScenePlace.HOUSE else ScenePlace.ISLAND,
        layout = if (inHouse) trimToOwned(layout, profile.items) else emptyList(),
        camera = s.camera,
        night = isNight,
        character = s.character,
    )
    BackHandler {
        when {
            s.shot -> s = s.copy(shot = false)
            s.editing -> s = s.copy(editing = false)
            else -> onClose()
        }
    }
    if (s.shot) {
        ShareQrLayout(ShareKind.PHOTO, onClose, onRetake = { s = s.copy(shot = false) }) {
            // 카메라 화면과 같은 비율(1144x901)이라 찍을 때 본 구도가 그대로 보인다.
            RoomScene(scene, Modifier.aspectRatio(1144f / 901f, matchHeightConstraintsFirst = true).clip(RoundedCornerShape(10.dp)))
        }
        return
    }
    val options = poseOptions(profile.selectedCharacterId, profile.owned)
    Box(Modifier.fillMaxSize()) {
        RoomScene(
            scene,
            Modifier.fillMaxSize().then(
                if (!s.editing) Modifier else Modifier.pointerInput(Unit) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        s = s.moved(drag.x / size.width, drag.y / size.height)
                    }
                },
            ),
        )
        // 찍히는 범위. Figma S-16의 흰 테두리(760x640 / 1144x901)와 같은 비율이다.
        Box(Modifier.align(Alignment.Center).fillMaxWidth(0.66f).fillMaxHeight(0.71f).border(5.dp, Color.White))
        Row(Modifier.padding(ScreenPad), horizontalArrangement = Arrangement.spacedBy(ChipGap)) {
            TopChip(stringResource(R.string.photo_cancel), onClose, icon = R.drawable.house_ic_back)
            TopChip(stringResource(R.string.photo_title), null)
        }
        PresetCard(s, { s = it }, Modifier.align(Alignment.TopEnd).padding(top = 195.dp, end = ScreenPad))
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(ScreenPad)) {
            if (s.editing) {
                CharacterCard(s, options, { s = it })
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TopChip(stringResource(R.string.photo_character), { s = s.startCharacter(options) })
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        PrimaryIconButton(
                            stringResource(R.string.photo_shoot),
                            R.drawable.house_ic_cam,
                            { s = s.copy(shot = true, editing = false) },
                            Modifier.widthIn(min = 364.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetCard(s: PhotoState, onChange: (PhotoState) -> Unit, modifier: Modifier = Modifier) {
    AppCard(modifier.width(243.dp)) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            for (p in CameraPreset.entries) {
                val on = s.preset == p
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 99.dp)
                        .clip(ControlShape)
                        .background(if (on) AppColors.Selected else AppColors.Card)
                        .border(BorderWidth, AppColors.Line, ControlShape)
                        .clickable(role = Role.RadioButton) { onChange(s.copy(preset = p)) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(presetLabel(p)), color = AppColors.Text, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                }
            }
            SquareIconButton(
                R.drawable.photo_ic_rotate,
                stringResource(R.string.photo_rotate),
                { onChange(s.rotated()) },
                Modifier.fillMaxWidth(),
                size = 99.dp,
                iconSize = 57.dp,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CharacterCard(s: PhotoState, options: List<String>, onChange: (PhotoState) -> Unit) {
    AppCard(Modifier.fillMaxWidth()) {
        FlowRow(
            Modifier.padding(23.dp),
            horizontalArrangement = Arrangement.spacedBy(31.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            itemVerticalAlignment = Alignment.Bottom,
        ) {
            Labeled(stringResource(R.string.photo_pose)) {
                Row(Modifier.widthIn(max = 620.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    for (id in options) {
                        val on = s.poseId == id
                        Image(
                            painterResource(characterImage(id)),
                            Catalog.item(id)?.name,
                            Modifier
                                .size(109.dp)
                                .clip(ControlShape)
                                .background(AppColors.Accent)
                                .border(if (on) 6.5.dp else BorderWidth, if (on) AppColors.Selected else AppColors.Line, ControlShape)
                                .clickable(role = Role.RadioButton) { onChange(s.copy(poseId = id)) }
                                .padding(8.dp),
                        )
                    }
                }
            }
            Labeled(stringResource(R.string.photo_size)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SquareIconButton(R.drawable.photo_ic_minus, stringResource(R.string.photo_smaller), { onChange(s.resized(-1)) })
                    SquareIconButton(R.drawable.photo_ic_plus, stringResource(R.string.photo_bigger), { onChange(s.resized(+1)) })
                }
            }
            SquareIconButton(R.drawable.photo_ic_flip, stringResource(R.string.photo_flip), { onChange(s.copy(flipped = !s.flipped)) })
            PrimaryButton(stringResource(R.string.photo_done), { onChange(s.copy(editing = false)) })
        }
    }
}

@Composable
private fun Labeled(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, color = AppColors.TextSub, fontSize = 29.sp)
        content()
    }
}

// S-16 상태마다 하나씩. 그림은 RoomScene 자리 표시용 렌더러로 그려진다.
@WindowPreviews @Composable private fun ObliquePreview() = PreviewFrame { PhotoScreenImpl(false, {}, night = false) }
@WindowPreviews @Composable private fun RotatedPreview() = PreviewFrame { PhotoScreenImpl(false, {}, PhotoState(rotation = 1), night = false) }
@WindowPreviews @Composable private fun TopPreview() = PreviewFrame { PhotoScreenImpl(false, {}, PhotoState(preset = CameraPreset.TOP), night = false) }
@WindowPreviews @Composable private fun LowPreview() = PreviewFrame { PhotoScreenImpl(false, {}, PhotoState(preset = CameraPreset.LOW_FRONT), night = false) }
@WindowPreviews @Composable private fun SidePreview() = PreviewFrame { PhotoScreenImpl(false, {}, PhotoState(preset = CameraPreset.SIDE), night = false) }
@WindowPreviews @Composable private fun NightPreview() = PreviewFrame { PhotoScreenImpl(false, {}, night = true) }
@WindowPreviews @Composable private fun CharacterPreview() =
    PreviewFrame { PhotoScreenImpl(false, {}, PhotoState(poseId = "pose_wave", editing = true), night = false) }
@WindowPreviews @Composable private fun InHousePreview() = PreviewFrame { PhotoScreenImpl(true, {}, PhotoState(poseId = "pose_wave"), night = false) }
@WindowPreviews @Composable private fun InHouseRotatedPreview() =
    PreviewFrame { PhotoScreenImpl(true, {}, PhotoState(rotation = 1, poseId = "pose_wave"), night = false) }
@WindowPreviews @Composable private fun SharePhotoPreview() =
    PreviewFrame { PhotoScreenImpl(true, {}, PhotoState(poseId = "pose_wave", shot = true), night = false) }
