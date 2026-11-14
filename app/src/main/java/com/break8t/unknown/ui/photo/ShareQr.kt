package com.break8t.unknown.ui.photo

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.break8t.unknown.R
import com.break8t.unknown.ui.components.AppCard
import com.break8t.unknown.ui.components.SecondaryButton
import com.break8t.unknown.ui.house.ChipGap
import com.break8t.unknown.ui.house.PreviewFrame
import com.break8t.unknown.ui.house.ScreenPad
import com.break8t.unknown.ui.house.TopChip
import com.break8t.unknown.ui.house.WindowPreviews
import com.break8t.unknown.ui.theme.AppColors
import kotlinx.coroutines.delay

enum class ShareKind { PHOTO, POSTCARD, JAM_CARD }

/** 휴대폰으로 열 QR을 보이는 공유 화면(S-17). title과 subtitle은 왼쪽 카드 아래에 쓴다. */
@Composable
fun ShareQrScreen(kind: ShareKind, title: String, subtitle: String, onClose: () -> Unit) =
    ShareQrScreen(kind, title, subtitle, image = null, onClose = onClose)

/** 왼쪽 카드에 그림(엽서, 정체 카드)을 함께 보일 때. */
@Composable
fun ShareQrScreen(kind: ShareKind, title: String, subtitle: String, @DrawableRes image: Int?, onClose: () -> Unit) =
    ShareQrLayout(kind, onClose, onRetake = null) {
        val shape = RoundedCornerShape(10.dp)
        val pictureModifier = Modifier.fillMaxWidth().aspectRatio(660f / 420f).clip(shape)
        if (image != null) {
            Image(painterResource(image), null, pictureModifier, contentScale = ContentScale.Crop)
        } else {
            Box(pictureModifier.background(AppColors.Muted))
        }
        Text(title, color = AppColors.Text, fontSize = 47.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 21.dp))
        Text(subtitle, color = AppColors.TextSub, fontSize = 34.sp)
    }

/** 공유 화면의 틀. onRetake가 있으면 QR 아래에 다시 찍기 버튼을 둔다(사진일 때). */
@Composable
internal fun ShareQrLayout(
    kind: ShareKind,
    onClose: () -> Unit,
    onRetake: (() -> Unit)?,
    preview: @Composable ColumnScope.() -> Unit,
) {
    val chip = when (kind) {
        ShareKind.PHOTO -> R.string.photo_share_photo
        ShareKind.POSTCARD -> R.string.photo_share_postcard
        ShareKind.JAM_CARD -> R.string.photo_share_jam_card
    }
    Column(
        Modifier.fillMaxSize().background(AppColors.Background).padding(ScreenPad),
        verticalArrangement = Arrangement.spacedBy(ChipGap),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(ChipGap)) {
            TopChip(stringResource(R.string.photo_close), onClose, icon = R.drawable.house_ic_back)
            TopChip(stringResource(chip), null)
        }
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            if (maxWidth >= 1000.dp) {
                Row(Modifier.fillMaxWidth().height(728.dp), horizontalArrangement = Arrangement.spacedBy(ScreenPad)) {
                    AppCard(Modifier.weight(700f).fillMaxHeight()) { Column(Modifier.padding(23.dp), content = preview) }
                    AppCard(Modifier.weight(363f).fillMaxHeight()) { QrPanel(kind, onRetake) }
                }
            } else {
                // 화면을 나누면 두 카드가 나란히 들어가지 않아서 위아래로 쌓는다.
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(ScreenPad)) {
                    AppCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(23.dp), content = preview) }
                    AppCard(Modifier.fillMaxWidth()) { QrPanel(kind, onRetake) }
                }
            }
        }
    }
}

@Composable
private fun QrPanel(kind: ShareKind, onRetake: (() -> Unit)?) {
    val line = when (kind) {
        ShareKind.PHOTO -> R.string.photo_qr_photo
        ShareKind.POSTCARD -> R.string.photo_qr_postcard
        ShareKind.JAM_CARD -> R.string.photo_qr_jam_card
    }
    // 화면을 나눠 Activity가 다시 만들어져도 남은 시간이 처음으로 돌아가지 않게 마감 시각을 저장한다.
    val deadline = rememberSaveable { System.currentTimeMillis() + SHARE_TTL_SEC * 1000L }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(deadline) {
        while (now < deadline) {
            delay(1_000)
            now = System.currentTimeMillis()
        }
    }
    val left = remainingSec(deadline, now)
    Column(
        Modifier.fillMaxWidth().padding(23.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(23.dp),
    ) {
        Text(
            stringResource(line),
            color = AppColors.Text,
            fontSize = 36.sp,
            lineHeight = 52.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        QrPattern(seed = deadline.hashCode(), modifier = Modifier.size(343.dp).alpha(if (left == 0) 0.2f else 1f))
        Text(
            when {
                left == 0 -> stringResource(R.string.photo_ttl_over)
                left >= 60 -> stringResource(R.string.photo_ttl_min, left / 60, left % 60)
                else -> stringResource(R.string.photo_ttl_sec, left)
            },
            color = AppColors.TextSub,
            fontSize = 31.sp,
            textAlign = TextAlign.Center,
        )
        if (onRetake != null) SecondaryButton(stringResource(R.string.photo_retake), onRetake, Modifier.fillMaxWidth())
    }
}

@Composable
private fun QrPattern(seed: Int, modifier: Modifier = Modifier) {
    val modules = remember(seed) { qrModules(seed) }
    val desc = stringResource(R.string.photo_qr)
    Canvas(modifier.clip(RoundedCornerShape(10.dp)).background(Color.White).padding(16.dp).semantics { contentDescription = desc }) {
        val cell = size.width / modules.size
        modules.forEachIndexed { y, row ->
            row.forEachIndexed { x, on -> if (on) drawRect(AppColors.Text, Offset(x * cell, y * cell), Size(cell, cell)) }
        }
    }
}

@WindowPreviews
@Composable
private fun PostcardSharePreview() = PreviewFrame {
    ShareQrScreen(ShareKind.POSTCARD, "경주 첨성대", "2026.10.12 방문") {}
}

@WindowPreviews
@Composable
private fun JamCardSharePreview() = PreviewFrame {
    ShareQrScreen(ShareKind.JAM_CARD, "성산대교 구간", "서부간선도로 · 성산대교 → 목동교") {}
}
